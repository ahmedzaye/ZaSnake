"""Offline end-to-end receiver policy tests for opt-in emulator inbox.

No network or Firestore connection. Google ID token verification and persistence
are patched; actual Firestore integration is separately covered by backend tests.
"""
import base64
import json
import unittest
from unittest.mock import patch

import rtdn_push_receiver as receiver

EMAIL = "zasnake-rtdn-push@zasnake.iam.gserviceaccount.com"
SUB = "projects/zasnake/subscriptions/zasnake-rtdn-staging-push"
AUTH = {"Authorization": "Bearer " + "fake-jwt-" * 8}
CLAIMS = {
    "email": EMAIL, "email_verified": True,
    "iss": "https://accounts.google.com",
}
TEST = {"version": "1.0", "packageName": "com.zayedart.zasnake",
        "testNotification": {"version": "1.0"}}
VOID = {"version": "1.0", "packageName": "com.zayedart.zasnake",
        "voidedPurchaseNotification": {
            "productType": 2, "refundType": 1,
            "purchaseToken": "fake-purchase-token-only",
        }}


def envelope(event):
    return {
        "subscription": SUB,
        "message": {
            "messageId": "123456789",
            "data": base64.b64encode(json.dumps(event).encode()).decode(),
        },
    }


class ReceiverInboxBridgeTests(unittest.TestCase):
    def setUp(self):
        self.old = (
            receiver.ENABLED, receiver.INBOX_EMULATOR_ENABLED,
            receiver.PUSH_AUDIENCE, receiver.PUSH_EMAIL,
            receiver.PUSH_SUBSCRIPTION,
        )
        receiver.ENABLED = True
        receiver.INBOX_EMULATOR_ENABLED = True
        receiver.PUSH_AUDIENCE = "https://zasnake-rtdn-receiver-123-ew.a.run.app"
        receiver.PUSH_EMAIL = EMAIL
        receiver.PUSH_SUBSCRIPTION = SUB
        self.client = receiver.app.test_client()

    def tearDown(self):
        (
            receiver.ENABLED, receiver.INBOX_EMULATOR_ENABLED,
            receiver.PUSH_AUDIENCE, receiver.PUSH_EMAIL,
            receiver.PUSH_SUBSCRIPTION,
        ) = self.old

    def test_test_notification_acked_after_durable_create(self):
        with patch.object(receiver.id_token, "verify_oauth2_token",
                          return_value=CLAIMS), patch.object(
                          receiver, "_persist_emulator_notification",
                          return_value={"outcome": "created", "kind": "test"}) as save:
            response = self.client.post("/pubsub/rtdn",
                                        json=envelope(TEST), headers=AUTH)
        self.assertEqual(response.status_code, 204)
        save.assert_called_once()

    def test_exact_duplicate_acked_after_safe_duplicate_read(self):
        with patch.object(receiver.id_token, "verify_oauth2_token",
                          return_value=CLAIMS), patch.object(
                          receiver, "_persist_emulator_notification",
                          return_value={"outcome": "duplicate", "kind": "test"}):
            response = self.client.post("/pubsub/rtdn",
                                        json=envelope(TEST), headers=AUTH)
        self.assertEqual(response.status_code, 204)

    def test_emulator_unavailable_fails_closed_without_ack(self):
        with patch.object(receiver.id_token, "verify_oauth2_token",
                          return_value=CLAIMS), patch.object(
                          receiver, "_persist_emulator_notification",
                          side_effect=RuntimeError("raw secret must not leak")):
            response = self.client.post("/pubsub/rtdn",
                                        json=envelope(TEST), headers=AUTH)
        self.assertEqual(response.status_code, 503)
        self.assertNotIn("raw secret", response.get_data(as_text=True))

    def test_conflicting_duplicate_fails_closed(self):
        with patch.object(receiver.id_token, "verify_oauth2_token",
                          return_value=CLAIMS), patch.object(
                          receiver, "_persist_emulator_notification",
                          side_effect=ValueError("conflicting_duplicate")):
            response = self.client.post("/pubsub/rtdn",
                                        json=envelope(TEST), headers=AUTH)
        self.assertEqual(response.status_code, 503)

    def test_real_refund_persists_hint_but_never_acks(self):
        with patch.object(receiver.id_token, "verify_oauth2_token",
                          return_value=CLAIMS), patch.object(
                          receiver, "_persist_emulator_notification",
                          return_value={"outcome": "created",
                                        "kind": "recheck_voided_purchases"}) as save:
            response = self.client.post("/pubsub/rtdn",
                                        json=envelope(VOID), headers=AUTH)
        self.assertEqual(response.status_code, 503)
        self.assertEqual(response.get_json()["error"], "processing_not_configured")
        save.assert_called_once()

    def test_invalid_auth_cannot_write(self):
        with patch.object(receiver.id_token, "verify_oauth2_token",
                          side_effect=ValueError("invalid")), patch.object(
                          receiver, "_persist_emulator_notification") as save:
            response = self.client.post("/pubsub/rtdn",
                                        json=envelope(TEST), headers=AUTH)
        self.assertEqual(response.status_code, 401)
        save.assert_not_called()

    def test_invalid_notification_cannot_write(self):
        wrong = dict(TEST, packageName="com.not.the.app")
        with patch.object(receiver.id_token, "verify_oauth2_token",
                          return_value=CLAIMS), patch.object(
                          receiver, "_persist_emulator_notification") as save:
            response = self.client.post("/pubsub/rtdn",
                                        json=envelope(wrong), headers=AUTH)
        self.assertEqual(response.status_code, 400)
        save.assert_not_called()

    def test_flag_off_retains_preview_behavior_without_writes(self):
        receiver.INBOX_EMULATOR_ENABLED = False
        with patch.object(receiver.id_token, "verify_oauth2_token",
                          return_value=CLAIMS), patch.object(
                          receiver, "_persist_emulator_notification") as save:
            response = self.client.post("/pubsub/rtdn",
                                        json=envelope(TEST), headers=AUTH)
        self.assertEqual(response.status_code, 204)
        save.assert_not_called()

    def test_receiver_disabled_never_writes(self):
        receiver.ENABLED = False
        with patch.object(receiver, "_persist_emulator_notification") as save:
            response = self.client.post("/pubsub/rtdn",
                                        json=envelope(TEST), headers=AUTH)
        self.assertEqual(response.status_code, 503)
        save.assert_not_called()

    def test_missing_emulator_env_guard_precedes_firestore_client(self):
        import os
        with patch.dict(os.environ, {
            "FIRESTORE_EMULATOR_HOST": "",
            "GCLOUD_PROJECT": "demo-zasnake-billing",
            "GOOGLE_CLOUD_PROJECT": "demo-zasnake-billing",
        }):
            with self.assertRaisesRegex(ValueError, "emulator_required"):
                receiver._persist_emulator_notification(envelope(TEST))


if __name__ == "__main__":
    unittest.main()
