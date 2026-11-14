"""Pure tests of paid-only cosmetic ownership; no live Firestore access."""
import unittest
from unittest.mock import MagicMock, patch

from paid_item_entitlements import (
    _purchased_paid_item_ids, decide_paid_item_purchase,
    purchase_paid_item_staging, read_paid_item_ids_staging,
)
from wallet_rules import WalletError, WalletState, credit, revoke


class PaidItemEntitlementTests(unittest.TestCase):
    def test_server_price_and_idempotent_replay(self):
        initial = credit(WalletState(), "p" * 64, 1000)
        after, charged, owned = decide_paid_item_purchase(initial, "skin_egypt")
        self.assertEqual((charged, owned, after.balance), (180, False, 820))
        self.assertEqual(after.spends["item:skin_egypt"], 180)
        replay, charged, owned = decide_paid_item_purchase(after, "skin_egypt")
        self.assertIs(after, replay)
        self.assertEqual((charged, owned), (0, True))
        self.assertEqual(_purchased_paid_item_ids(after), {"skin_egypt"})

    def test_insufficient_and_revoked_balance_fail_closed(self):
        with self.assertRaisesRegex(WalletError, "insufficient_funds"):
            decide_paid_item_purchase(WalletState(), "theme_ocean")
        funded = credit(WalletState(), "p" * 64, 1000)
        refunded = revoke(funded, "p" * 64, 1000)
        with self.assertRaisesRegex(WalletError, "insufficient_funds"):
            decide_paid_item_purchase(refunded, "skin_egypt")

    def test_never_trust_client_price_or_unknown_item(self):
        for item in ("", "unknown", None, False, "skin_green", "coins_1000"):
            with self.subTest(item=item), self.assertRaises(WalletError):
                decide_paid_item_purchase(WalletState(), item)

    def test_existing_entitlement_retains_old_price_after_catalog_update(self):
        state = WalletState(grants={"p" * 64: 1000},
                            spends={"item:skin_egypt": 120})
        replay, charged, owned = decide_paid_item_purchase(state, "skin_egypt")
        self.assertIs(state, replay)
        self.assertEqual((charged, owned), (0, True))

    def test_unknown_or_corrupt_ownership_fails_closed(self):
        for spends in ({"item:fake": 50}, {"item:skin_egypt": 0}):
            with self.subTest(spends=spends), self.assertRaises(WalletError):
                _purchased_paid_item_ids(WalletState(spends=spends))

    def test_purchase_validates_before_database_access(self):
        db = MagicMock()
        with self.assertRaises(WalletError):
            purchase_paid_item_staging(db, "player", "unknown")
        db.collection.assert_not_called()

    def test_read_owned_from_server_only(self):
        db = MagicMock()
        snap = db.collection.return_value.document.return_value.get.return_value
        snap.exists = True
        snap.to_dict.return_value = {
            "grants": {"p" * 64: 1000},
            "spends": {"item:theme_ocean": 80},
            "revoked": {}
        }
        self.assertEqual(read_paid_item_ids_staging(db, "player"), {"theme_ocean"})

    def test_atomic_duplicate_does_not_write_again(self):
        # Call decorated transaction's underlying function with a fake
        # transaction to verify write count without any GCP dependency.
        from paid_item_entitlements import _purchase
        transaction = MagicMock()
        ref = MagicMock()
        ref.get.return_value.exists = True
        ref.get.return_value.to_dict.return_value = {
            "grants": {"p" * 64: 1000},
            "spends": {"item:skin_egypt": 180},
            "revoked": {}
        }
        underlying = _purchase.__wrapped__
        result = underlying(transaction, ref, "skin_egypt")
        self.assertEqual(result["chargedPaidCoins"], 0)
        self.assertTrue(result["alreadyOwnedInPaidWallet"])
        transaction.set.assert_not_called()


if __name__ == "__main__":
    unittest.main()
