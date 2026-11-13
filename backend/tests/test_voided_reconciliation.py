"""Offline contract tests; never call Google or live Firestore."""
import hashlib
import unittest
from unittest.mock import MagicMock, patch

from play_refund_events import RefundEventError
from voided_reconciliation import (
    MILLIS_PER_DAY, PLAY_URL, ReconciliationError,
    fetch_voided_purchases, reconcile_window,
)

NOW = 1_800_000_000_000
START = NOW - MILLIS_PER_DAY
TOKEN = "fake-test-token123"
DIGEST = hashlib.sha256(TOKEN.encode()).hexdigest()
PLAYER = "player-test-1"
PRODUCT = "zasnake_coins_1000"
ACCOUNT = hashlib.sha256(PLAYER.encode()).hexdigest()
VOIDED = {"purchaseToken": TOKEN, "voidedTimeMillis": str(NOW - 20_000)}
PURCHASE = {"playerId": PLAYER, "productId": PRODUCT,
            "accountId": ACCOUNT, "coins": 1000, "status": "consumed"}


def play_session(*pages):
    session = MagicMock()
    session.get.return_value.raise_for_status.return_value = None
    session.get.side_effect = []
    for page in pages:
        reply = MagicMock()
        reply.json.return_value = page
        session.get.side_effect.append(reply)
    return session


def firestore_with_purchase(present=True, value=None):
    db = MagicMock()
    snapshot = MagicMock()
    snapshot.exists = present
    snapshot.to_dict.return_value = value if value is not None else PURCHASE
    db.collection.return_value.document.return_value.get.return_value = snapshot
    return db


class VoidedReconciliationTests(unittest.TestCase):
    def test_pagination_and_fixed_google_host(self):
        session = play_session(
            {"voidedPurchases": [VOIDED], "tokenPagination": {"nextPageToken": "next"}},
            {"voidedPurchases": [VOIDED]},
        )
        result = fetch_voided_purchases(session, START, NOW, NOW)
        self.assertEqual(len(result), 2)
        self.assertEqual(session.get.call_count, 2)
        for args in session.get.call_args_list:
            self.assertEqual(args.args[0], PLAY_URL)
            self.assertEqual(args.kwargs["allow_redirects"], False)
            self.assertEqual(args.kwargs["params"]["type"], "0")
        self.assertEqual(session.get.call_args_list[1].kwargs["params"]["token"], "next")

    def test_read_only_default(self):
        db = firestore_with_purchase()
        session = play_session({"voidedPurchases": [VOIDED, VOIDED]})
        with patch("voided_reconciliation.revoke_authoritatively_verified_purchase") as revoke:
            result = reconcile_window(db, session, START, NOW, NOW)
        self.assertEqual(result["matched_purchases"], 1)
        self.assertEqual(result["verified_full_refunds"], 1)
        self.assertTrue(result["dry_run"])
        self.assertEqual(result["applied"], 0)
        revoke.assert_not_called()

    def test_only_explicit_apply_calls_verified_revoke(self):
        db = firestore_with_purchase()
        session = play_session({"voidedPurchases": [VOIDED]})
        with patch("voided_reconciliation.revoke_authoritatively_verified_purchase") as revoke:
            result = reconcile_window(db, session, START, NOW, NOW, dry_run=False)
        self.assertEqual(result["applied"], 1)
        revoke.assert_called_once_with(db, DIGEST, PLAYER, PRODUCT, ACCOUNT, 1000)

    def test_unmatched_receipt_does_not_create_wallet(self):
        db = firestore_with_purchase(present=False)
        session = play_session({"voidedPurchases": [VOIDED]})
        with patch("voided_reconciliation.revoke_authoritatively_verified_purchase") as revoke:
            result = reconcile_window(db, session, START, NOW, NOW, dry_run=False)
        self.assertEqual(result["unknown_purchases"], 1)
        self.assertEqual(result["applied"], 0)
        revoke.assert_not_called()

    def test_rejects_partial_and_does_not_write(self):
        partial = {**VOIDED, "voidedQuantity": 1}
        session = play_session({"voidedPurchases": [partial]})
        db = firestore_with_purchase()
        with patch("voided_reconciliation.revoke_authoritatively_verified_purchase") as revoke:
            with self.assertRaises(RefundEventError):
                reconcile_window(db, session, START, NOW, NOW, dry_run=False)
        revoke.assert_not_called()

    def test_no_write_on_corrupt_purchase(self):
        db = firestore_with_purchase(value={**PURCHASE, "accountId": "wrong"})
        session = play_session({"voidedPurchases": [VOIDED]})
        with patch("voided_reconciliation.revoke_authoritatively_verified_purchase") as revoke:
            with self.assertRaises(ReconciliationError):
                reconcile_window(db, session, START, NOW, NOW, dry_run=False)
        revoke.assert_not_called()

    def test_reject_stale_window_and_page_loop(self):
        with self.assertRaises(ReconciliationError):
            fetch_voided_purchases(MagicMock(), NOW - 31 * MILLIS_PER_DAY, NOW, NOW)
        session = play_session({"tokenPagination": {"nextPageToken": "next"}},
                               {"tokenPagination": {"nextPageToken": "next"}})
        with self.assertRaises(ReconciliationError):
            fetch_voided_purchases(session, START, NOW, NOW)

    def test_no_write_if_later_page_invalid(self):
        session = play_session({"voidedPurchases": [VOIDED],
                                "tokenPagination": {"nextPageToken": "next"}},
                               {"voidedPurchases": [{"purchaseToken": "x"}]})
        with patch("voided_reconciliation.revoke_authoritatively_verified_purchase") as revoke:
            with self.assertRaises(RefundEventError):
                reconcile_window(firestore_with_purchase(), session,
                                 START, NOW, NOW, dry_run=False)
        revoke.assert_not_called()


if __name__ == "__main__":
    unittest.main()
