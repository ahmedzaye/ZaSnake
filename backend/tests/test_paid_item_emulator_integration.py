"""Firestore Emulator ONLY: atomic paid-item spending and refund debt tests.

Never execute with real Firestore. Example:
FIRESTORE_EMULATOR_HOST=127.0.0.1:8085 python3 -m unittest discover -s tests -v
"""
import os
import unittest
import uuid
from concurrent.futures import ThreadPoolExecutor

from google.cloud import firestore

from paid_item_entitlements import purchase_paid_item_staging, read_paid_item_ids_staging
from wallet_repository import _player_key
from wallet_rules import WalletError


@unittest.skipUnless(os.environ.get("FIRESTORE_EMULATOR_HOST"),
                     "Requires local Firestore emulator; no live DB access allowed")
class PaidItemEmulatorTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        endpoint = os.environ["FIRESTORE_EMULATOR_HOST"]
        if not (endpoint.startswith("127.0.0.1:") or endpoint.startswith("localhost:")):
            raise RuntimeError("Refusing to access nonlocal Firestore")
        cls.db = firestore.Client(project="zasnake-paid-item-emulator-test")

    def setUp(self):
        self.player = "paid-item-test-" + uuid.uuid4().hex
        self.ref = self.db.collection("paid_wallets_v1").document(_player_key(self.player))
        self.ref.set({"grants": {"a" * 64: 1000}, "spends": {}, "revoked": {}})
        self.addCleanup(self.ref.delete)

    def test_paid_spend_atomic_duplicate_and_ownership(self):
        first = purchase_paid_item_staging(self.db, self.player, "skin_egypt")
        self.assertEqual(first["chargedPaidCoins"], 180)
        self.assertEqual(first["remainingPaidBalance"], 820)
        replay = purchase_paid_item_staging(self.db, self.player, "skin_egypt")
        self.assertEqual(replay["chargedPaidCoins"], 0)
        self.assertTrue(replay["alreadyOwnedInPaidWallet"])
        self.assertEqual(read_paid_item_ids_staging(self.db, self.player), {"skin_egypt"})
        self.assertEqual(self.ref.get().to_dict()["spends"], {"item:skin_egypt": 180})

    def test_parallel_spends_do_not_double_charge(self):
        with ThreadPoolExecutor(max_workers=4) as pool:
            results = list(pool.map(
                lambda _: purchase_paid_item_staging(self.db, self.player, "skin_egypt"),
                range(4)))
        self.assertEqual(sum(r["chargedPaidCoins"] for r in results), 180)
        self.assertEqual(self.ref.get().to_dict()["spends"], {"item:skin_egypt": 180})

    def test_insufficient_funds_leaves_entitlements_unchanged(self):
        self.ref.update({"revoked": {"a" * 64: 1000}})
        with self.assertRaises(WalletError):
            purchase_paid_item_staging(self.db, self.player, "skin_egypt")
        self.assertEqual(self.ref.get().to_dict()["spends"], {})


if __name__ == "__main__":
    unittest.main()
