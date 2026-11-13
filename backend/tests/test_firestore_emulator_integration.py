"""End-to-end Firestore transaction tests, run ONLY against the local emulator.

Start the emulator separately:
    gcloud emulators firestore start --host-port=127.0.0.1:8085
Then run:
    FIRESTORE_EMULATOR_HOST=127.0.0.1:8085 python3 -m unittest tests.test_firestore_emulator_integration -v
Or:
    FIRESTORE_EMULATOR_HOST=127.0.0.1:8085 python3 -m unittest discover -s tests -v

No real Google Cloud Firestore database is ever used by this test module.
"""
import hashlib
import os
import unittest
import uuid
from concurrent.futures import ThreadPoolExecutor

from google.cloud import firestore

from atomic_wallet_settlement import (
    finalize_consumed_purchase,
    revoke_authoritatively_verified_purchase,
    current_net_grants,
)
from wallet_repository import _player_key
from wallet_rules import WalletError

PACKAGE = "zasnake_coins_1000"


@unittest.skipUnless(os.environ.get("FIRESTORE_EMULATOR_HOST"),
                     "Requires local Firestore emulator; no live DB access allowed")
class FirestoreSettlementIntegrationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        # Double-check emulator host before any Firestore client is created.
        endpoint = os.environ["FIRESTORE_EMULATOR_HOST"]
        if not (endpoint.startswith("127.0.0.1:") or endpoint.startswith("localhost:")):
            raise RuntimeError("Refusing to run integration tests against a nonlocal endpoint")
        cls.db = firestore.Client(project="zasnake-billing-emulator-test")

    def setUp(self):
        suffix = uuid.uuid4().hex
        self.player = "emulator-test-" + suffix
        self.account = hashlib.sha256(self.player.encode()).hexdigest()
        self.digest = hashlib.sha256(suffix.encode()).hexdigest()
        self.ref = self.db.collection("purchases").document(self.digest)
        self.wallet = self.db.collection("paid_wallets_v1").document(_player_key(self.player))
        self.addCleanup(lambda: self.ref.delete())
        self.addCleanup(lambda: self.wallet.delete())

    def reserve(self):
        self.ref.set({
            "playerId": self.player,
            "productId": PACKAGE,
            "coins": 1000,
            "accountId": self.account,
            "status": "reserved",
        })

    def settle(self):
        return finalize_consumed_purchase(self.db, self.digest, self.player, PACKAGE, self.account)

    def revoke(self):
        return revoke_authoritatively_verified_purchase(
            self.db, self.digest, self.player, PACKAGE, self.account, 1000
        )

    def test_credit_atomic_and_repeat_safe(self):
        self.reserve()
        self.assertEqual(self.settle(), {self.digest: 1000})
        self.assertEqual(self.settle(), {self.digest: 1000})
        self.assertEqual(self.wallet.get().to_dict()["grants"], {self.digest: 1000})
        self.assertEqual(self.ref.get().to_dict()["status"], "consumed")
        self.assertEqual(current_net_grants(self.db, self.player), {self.digest: 1000})

    def test_rejected_unreserved_purchase_no_credit(self):
        with self.assertRaises(WalletError):
            self.settle()
        self.assertFalse(self.wallet.get().exists)

    def test_parallel_claims_exactly_once(self):
        self.reserve()
        # Real Firestore transactions should resolve competing writes without double credit.
        with ThreadPoolExecutor(max_workers=4) as workers:
            futures = [workers.submit(self.settle) for _ in range(4)]
            for future in futures:
                self.assertEqual(future.result(), {self.digest: 1000})
        self.assertEqual(self.wallet.get().to_dict()["grants"], {self.digest: 1000})

    def test_revocation_idempotent_after_credit(self):
        self.reserve()
        self.settle()
        self.assertEqual(self.revoke(), 0)
        self.assertEqual(self.revoke(), 0)
        self.assertEqual(current_net_grants(self.db, self.player), {})
        self.assertEqual(self.ref.get().to_dict()["status"], "revoked")
        with self.assertRaises(WalletError):
            self.settle()

    def test_refund_before_wallet_credit_blocks_late_claim(self):
        self.reserve()
        self.assertEqual(self.revoke(), 0)
        self.assertFalse(self.wallet.get().exists)
        self.assertEqual(self.ref.get().to_dict()["revokedCoins"], 1000)
        with self.assertRaises(WalletError):
            self.settle()

    def test_wrong_player_cannot_claim(self):
        self.reserve()
        other = "imposter"
        other_hash = hashlib.sha256(other.encode()).hexdigest()
        with self.assertRaises(WalletError):
            finalize_consumed_purchase(self.db, self.digest, other, PACKAGE, other_hash)
        self.assertFalse(self.wallet.get().exists)

if __name__ == "__main__":
    unittest.main()
