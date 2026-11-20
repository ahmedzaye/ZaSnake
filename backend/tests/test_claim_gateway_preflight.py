"""Staging endpoint tests. Mocks Play/Firestore; NOT emulator or live Play tests."""
import importlib
import os
import unittest
from unittest.mock import MagicMock, patch

from wallet_rules import WalletError


class ClaimGatewayTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        # No GCP credentials are needed for these tests; db is replaced.
        with patch("google.cloud.firestore.Client", return_value=MagicMock()):
            cls.server = importlib.import_module("main")
        cls.client = cls.server.app.test_client()

    def setUp(self):
        self.flags = (
            self.server.ATOMIC_BILLING_ENABLED,
            self.server.CHECKOUT_VERIFIED_ENABLED,
            self.server.PAID_SPENDING_ENABLED,
            self.server.LEGACY_OWNERSHIP_RECONCILED,
        )

    def tearDown(self):
        (self.server.ATOMIC_BILLING_ENABLED,
         self.server.CHECKOUT_VERIFIED_ENABLED,
         self.server.PAID_SPENDING_ENABLED,
         self.server.LEGACY_OWNERSHIP_RECONCILED) = self.flags

    def test_checkout_readiness_fails_closed_and_requires_player(self):
        self.server.ATOMIC_BILLING_ENABLED = False
        with patch.object(self.server, "session_player") as auth:
            disabled = self.client.get("/checkout-ready")
        self.assertEqual(disabled.status_code, 503)
        auth.assert_not_called()

        # Atomic billing alone MUST NOT authorize a new checkout.
        self.server.ATOMIC_BILLING_ENABLED = True
        with patch.object(self.server, "session_player") as auth:
            still_disabled = self.client.get("/checkout-ready")
        self.assertEqual(still_disabled.status_code, 503)
        auth.assert_not_called()

        self.server.CHECKOUT_VERIFIED_ENABLED = True
        with patch.object(self.server, "session_player") as auth:
            spending_disabled = self.client.get("/checkout-ready")
        self.assertEqual(spending_disabled.status_code, 503)
        auth.assert_not_called()

        self.server.PAID_SPENDING_ENABLED = True
        self.server.LEGACY_OWNERSHIP_RECONCILED = True
        with patch.object(self.server, "session_player", return_value=None):
            unauthenticated = self.client.get("/checkout-ready")
        self.assertEqual(unauthenticated.status_code, 401)

        with (patch.object(self.server, "session_player", return_value="player1"),
              patch.object(self.server, "legacy_registered", return_value=True)):
            allowed = self.client.get("/checkout-ready")
        self.assertEqual(allowed.status_code, 200)
        self.assertEqual(allowed.get_json(), {"checkoutReady": True})

    def test_claim_disabled_refuses_before_session(self):
        self.server.ATOMIC_BILLING_ENABLED = False
        with patch.object(self.server, "session_player") as session:
            result = self.client.post("/claim", json={
                "productId": "zasnake_coins_1000", "purchaseToken": "notreal"
            })
        self.assertEqual(result.status_code, 503)
        self.assertEqual(result.get_json()["error"], "billing_disabled")
        session.assert_not_called()

    def test_grants_disabled_refuses_before_session(self):
        self.server.ATOMIC_BILLING_ENABLED = False
        with patch.object(self.server, "session_player") as session:
            result = self.client.get("/grants")
        self.assertEqual(result.status_code, 503)
        session.assert_not_called()

    def test_claim_enabled_without_player(self):
        self.server.ATOMIC_BILLING_ENABLED = True
        with patch.object(self.server, "session_player", return_value=None):
            result = self.client.post("/claim", json={
                "productId": "zasnake_coins_1000", "purchaseToken": "notreal"
            })
        self.assertEqual(result.status_code, 401)

    def test_grants_authorized_returns_server_snapshot(self):
        self.server.ATOMIC_BILLING_ENABLED = True
        with (patch.object(self.server, "session_player", return_value="player1"),
              patch.object(self.server, "current_net_grants",
                           return_value={"a" * 64: 1000}) as wallet):
            response = self.client.get("/grants")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.get_json()["grants"], {"a" * 64: 1000})
        wallet.assert_called_once()

    def test_claim_rejects_unrecognized_consumed_token(self):
        self.server.ATOMIC_BILLING_ENABLED = True
        receipt = {
            "purchaseStateContext": {"purchaseState": "PURCHASED"},
            "obfuscatedExternalAccountId": None,
            "productLineItem": [{
                "productId": "zasnake_coins_1000",
                "productOfferDetails": {"quantity": 1, "refundableQuantity": 1,
                    "consumptionState": "CONSUMPTION_STATE_CONSUMED"}
            }]
        }
        from hashlib import sha256
        receipt["obfuscatedExternalAccountId"] = sha256(b"player1").hexdigest()
        play = MagicMock()
        play.get.return_value.json.return_value = receipt
        absent = MagicMock()
        absent.exists = False
        db = MagicMock()
        db.collection.return_value.document.return_value.get.return_value = absent
        with (patch.object(self.server, "session_player", return_value="player1"),
              patch.object(self.server, "db", db),
              patch.object(self.server, "play_session", return_value=play),
              patch.object(self.server, "finalize_consumed_purchase") as finalize):
            response = self.client.post("/claim", json={
                "productId": "zasnake_coins_1000", "purchaseToken": "testtoken"
            })
        self.assertEqual(response.status_code, 409)
        self.assertEqual(response.get_json()["error"], "unrecognized_consumed_token")
        finalize.assert_not_called()


if __name__ == "__main__":
    unittest.main()
