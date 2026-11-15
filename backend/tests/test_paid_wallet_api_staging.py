"""Paid-wallet HTTP contract tests. All Play and Firestore calls are mocked."""
import importlib
import unittest
from unittest.mock import MagicMock, patch


class PaidWalletApiTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        with patch("google.cloud.firestore.Client", return_value=MagicMock()):
            cls.server = importlib.import_module("main")
        cls.client = cls.server.app.test_client()

    def setUp(self):
        self.saved = (
            self.server.ATOMIC_BILLING_ENABLED,
            self.server.PAID_SPENDING_ENABLED,
            self.server.LEGACY_OWNERSHIP_RECONCILED
        )

    def tearDown(self):
        (self.server.ATOMIC_BILLING_ENABLED,
         self.server.PAID_SPENDING_ENABLED,
         self.server.LEGACY_OWNERSHIP_RECONCILED) = self.saved

    def ready(self):
        self.server.ATOMIC_BILLING_ENABLED = True
        self.server.PAID_SPENDING_ENABLED = True
        self.server.LEGACY_OWNERSHIP_RECONCILED = True

    def test_both_endpoints_fail_closed_for_every_missing_flag(self):
        flags = [
            (False, False, False), (True, False, False),
            (True, True, False), (False, True, True),
            (True, False, True), (False, False, True)
        ]
        for state in flags:
            with self.subTest(flags=state):
                (self.server.ATOMIC_BILLING_ENABLED,
                 self.server.PAID_SPENDING_ENABLED,
                 self.server.LEGACY_OWNERSHIP_RECONCILED) = state
                with patch.object(self.server, "session_player") as auth:
                    get = self.client.get("/paid-wallet")
                    post = self.client.post("/paid-wallet/items",
                                            json={"itemId": "theme_ocean"})
                self.assertEqual(get.status_code, 503)
                self.assertEqual(post.status_code, 503)
                auth.assert_not_called()

    def test_session_required_before_any_firestore_call(self):
        self.ready()
        with (patch.object(self.server, "session_player", return_value=None),
              patch.object(self.server, "paid_item_wallet_snapshot_staging") as read,
              patch.object(self.server, "purchase_paid_item_staging") as spend):
            self.assertEqual(self.client.get("/paid-wallet").status_code, 401)
            self.assertEqual(self.client.post("/paid-wallet/items",
                             json={"itemId": "theme_ocean"}).status_code, 401)
        read.assert_not_called()
        spend.assert_not_called()

    def test_rejects_client_supplied_price_and_identity(self):
        self.ready()
        payloads = [
            {}, {"itemId": "theme_ocean", "price": 0},
            {"itemId": "theme_ocean", "playerId": "someone-else"},
            {"itemId": "theme_ocean", "paidCoins": 10000},
            {"itemId": 1}, {"itemId": "skin_green"},
            {"itemId": "not-a-real-item"},
        ]
        with (patch.object(self.server, "session_player", return_value="playerA"),
              patch.object(self.server, "purchase_paid_item_staging") as spend):
            for payload in payloads:
                with self.subTest(payload=payload):
                    self.assertEqual(self.client.post("/paid-wallet/items",
                                     json=payload).status_code, 400)
            self.assertEqual(self.client.post("/paid-wallet/items",
                             data="hello", content_type="text/plain").status_code, 415)
        spend.assert_not_called()

    def test_spend_calls_server_only_catalog_for_authenticated_player(self):
        self.ready()
        expected = {"itemId": "theme_ocean", "chargedPaidCoins": 80,
                    "alreadyOwnedInPaidWallet": False,
                    "remainingPaidBalance": 920}
        with (patch.object(self.server, "session_player", return_value="verified-player"),
              patch.object(self.server, "purchase_paid_item_staging",
                           return_value=expected) as spend):
            result = self.client.post("/paid-wallet/items", json={"itemId": "theme_ocean"})
        self.assertEqual(result.status_code, 200)
        self.assertEqual(result.get_json(), expected)
        spend.assert_called_once_with(self.server.db, "verified-player", "theme_ocean")

    def test_insufficient_paid_balance_never_claims_success(self):
        from wallet_rules import WalletError
        self.ready()
        with (patch.object(self.server, "session_player", return_value="verified-player"),
              patch.object(self.server, "purchase_paid_item_staging",
                           side_effect=WalletError("insufficient_funds"))):
            response = self.client.post("/paid-wallet/items",
                                        json={"itemId": "theme_ocean"})
        self.assertEqual(response.status_code, 409)
        self.assertEqual(response.get_json()["error"], "insufficient_paid_balance")

    def test_wallet_read_is_server_bound_and_ignores_client_payload(self):
        self.ready()
        data = {"spendablePaidCoins": 920, "ownedPaidItemIds": ["theme_ocean"]}
        with (patch.object(self.server, "session_player", return_value="verified-player"),
              patch.object(self.server, "paid_item_wallet_snapshot_staging",
                           return_value=data) as read):
            response = self.client.get("/paid-wallet")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.get_json(), data)
        read.assert_called_once_with(self.server.db, "verified-player")


if __name__ == "__main__":
    unittest.main()
