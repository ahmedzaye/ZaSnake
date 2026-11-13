"""Preflight validation tests for experimental atomic settlement.

These tests do not simulate Firestore transactions or Google Play receipts.
Do not deploy based on them alone.
"""
import hashlib
import unittest

from atomic_wallet_settlement import (
    CANONICAL_PRODUCTS, _check_digest, _expected_purchase,
    finalize_consumed_purchase, revoke_authoritatively_verified_purchase,
)
from wallet_rules import WalletError

PLAYER = "pgs-player-1"
ACCOUNT = hashlib.sha256(PLAYER.encode("utf-8")).hexdigest()
PRODUCT = "zasnake_coins_1000"
DIGEST = "a" * 64


def row(**overrides):
    data = {"playerId": PLAYER, "productId": PRODUCT,
            "accountId": ACCOUNT, "coins": 1000, "status": "reserved"}
    data.update(overrides)
    return data


class AtomicSettlementPreflightTests(unittest.TestCase):
    def test_expected_catalog_products(self):
        self.assertEqual(CANONICAL_PRODUCTS, {
            "zasnake_coins_1000": 1000,
            "zasnake_coins_2000": 2000,
            "zasnake_coins_4000": 4000,
        })

    def test_expected_purchase(self):
        self.assertEqual(_expected_purchase(row(), PLAYER, PRODUCT, ACCOUNT), 1000)

    def test_reject_wrong_receipt_binding(self):
        for broken in (
            row(playerId="other"), row(productId="zasnake_coins_4000"),
            row(accountId="0" * 64), row(coins=4000),
            {}, None,
        ):
            with self.subTest(data=broken), self.assertRaises(WalletError):
                _expected_purchase(broken, PLAYER, PRODUCT, ACCOUNT)

    def test_reject_unknown_product(self):
        with self.assertRaises(WalletError):
            _expected_purchase(row(), PLAYER, "unknown", ACCOUNT)

    def test_digest_guard(self):
        _check_digest(DIGEST)
        for bad in (None, "a", "g" * 64, "A" * 64, "a" * 65, ""):
            with self.subTest(digest=bad), self.assertRaises(WalletError):
                _check_digest(bad)

    def test_finalize_rejects_wrong_hash_before_firestore(self):
        with self.assertRaises(WalletError):
            finalize_consumed_purchase(None, DIGEST, PLAYER, PRODUCT, "0" * 64)

    def test_revoke_rejects_wrong_hash_before_firestore(self):
        with self.assertRaises(WalletError):
            revoke_authoritatively_verified_purchase(None, DIGEST, PLAYER, PRODUCT, "0" * 64, 1000)


if __name__ == "__main__":
    unittest.main()
