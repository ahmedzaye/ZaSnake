"""Schema/guard tests only; live Firestore integration must be tested separately."""
import unittest
from unittest.mock import patch
from wallet_rules import WalletError, WalletState
from wallet_repository import (
    _player_key, _state_from_document, _doc_for, MAX_EVENTS_PER_WALLET,
    PREMIUM_ITEM_PRICES, credit_verified_purchase, spend_verified_item,
    revoke_purchase,
)


class WalletRepositoryTests(unittest.TestCase):
    def test_player_scoped_key(self):
        a = _player_key("player-one")
        self.assertEqual(len(a), 64)
        self.assertNotEqual(a, _player_key("player-two"))
        self.assertEqual(a, _player_key("player-one"))

    def test_reject_invalid_players(self):
        for p in ("", None, "x" * 257):
            with self.subTest(p=p), self.assertRaises(WalletError):
                _player_key(p)

    def test_schema_balance_and_revocation(self):
        receipt = {"grants": {"p": 1000}, "spends": {"item:skin_egypt": 180},
                   "revoked": {"p": 1000}}
        state = _state_from_document(receipt)
        self.assertEqual(state.balance, -180)
        document = _doc_for(state)
        self.assertEqual(document["schemaVersion"], 1)
        self.assertEqual(document["grants"]["p"], 1000)

    def test_reject_malformed_documents(self):
        invalid = [
            {"grants": []},
            {"grants": {"p": -1000}},
            {"grants": {"p": 1.5}},
            {"grants": {"p": True}},
            {"grants": {"p": 1001}},
            {"grants": {"p": 1000}, "revoked": {"unknown": 100}},
            {"grants": {"p": 1000}, "revoked": {"p": 1001}},
            {"grants": {str(i): 1000 for i in range(MAX_EVENTS_PER_WALLET + 1)}},
        ]
        for doc in invalid:
            with self.subTest(doc=str(doc)[:40]), self.assertRaises(WalletError):
                _state_from_document(doc)

    def test_untrusted_amounts_rejected_early(self):
        with self.assertRaises(WalletError):
            credit_verified_purchase(None, "player", "invalid", 1000)
        with self.assertRaises(WalletError):
            revoke_purchase(None, "player", "not-a-digest", 1000)
        with self.assertRaises(WalletError):
            spend_verified_item(None, "player", "unknown_product")

    def test_server_catalog(self):
        self.assertEqual(PREMIUM_ITEM_PRICES["skin_egypt"], 180)
        self.assertEqual(PREMIUM_ITEM_PRICES["body_crystal"], 350)


if __name__ == "__main__":
    unittest.main()
