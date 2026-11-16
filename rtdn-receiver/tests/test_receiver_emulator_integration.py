"""Real HTTP handler -> Firestore emulator bridge integration (no Google calls).

Uses Flask's local HTTP test client and mock-verified Pub/Sub OIDC claims;
all Firestore reads/writes use only the demo emulator at 127.0.0.1:8085.
No Cloud Run, Pub/Sub, Android Publisher API, or live project access.
"""
import base64
import hashlib
import json
import os
import uuid
import unittest
from unittest.mock import patch

from google.cloud import firestore

import rtdn_push_receiver as receiver
from rtdn_inbox import COLLECTION

EMULATOR_PROJECT = "demo-zasnake-billing"
SUB = "projects/zasnake/subscriptions/zasnake-rtdn-staging-push"
EMAIL = "zasnake-rtdn-push@zasnake.iam.gserviceaccount.com"
AUDIENCE = "https://zasnake-rtdn-receiver-123-ew.a.run.app"
AUTH = {"Authorization": "Bearer " + "fake-jwt-" * 8}
CLAIMS = {
    "email": EMAIL, "email_verified": True,
    "iss": "https://accounts.google.com",
}
TOKEN = "fake-refund-token-test-only"
PLAY_TEST = {
    "version": "1.0",
    "packageName": "com.zayedart.zasnake",
    "testNotification": {"version": "1.0"},
}
PLAY_REFUND = {
    "version": "1.0",
    "packageName": "com.zayedart.zasnake",
    "voidedPurchaseNotification": {
        "productType": 2,
        "refundType": 1,
        "purchaseToken": TOKEN,
    },
}


def envelope(event, message_id):
    return {
        "subscription": SUB,
        "message": {
            "messageId": message_id,
            "data": base64.b64encode(json.dumps(event).encode()).decode("ascii"),
        },
    }


def doc_id(message_id):
    return hashlib.sha256((SUB + "\x00" + message_id).encode()).hexdigest()


@unittest.skipUnless(
    os.environ.get("FIRESTORE_EMULATOR_HOST") == "127.0.0.1:8085"
    and os.environ.get("GCLOUD_PROJECT") == EMULATOR_PROJECT
    and os.environ.get("GOOGLE_CLOUD_PROJECT") == EMULATOR_PROJECT,
    "local demo Firestore emulator is required",
)
class RtdnReceiverEmulatorIntegrationTests(unittest.TestCase):
    def setUp(self):
        self.old_config = (
            receiver.ENABLED,
            receiver.INBOX_EMULATOR_ENABLED,
            receiver.PUSH_AUDIENCE,
            receiver.PUSH_EMAIL,
            receiver.PUSH_SUBSCRIPTION,
        )
        receiver.ENABLED = True
        receiver.INBOX_EMULATOR_ENABLED = True
        receiver.PUSH_AUDIENCE = AUDIENCE
        receiver.PUSH_EMAIL = EMAIL
        receiver.PUSH_SUBSCRIPTION = SUB
        self.db = firestore.Client(project=EMULATOR_PROJECT)
        self.client = receiver.app.test_client()
        self.valid_id_token = patch.object(
            receiver.id_token, "verify_oauth2_token", return_value=CLAIMS
        )
        self.valid_id_token.start()
        self.addCleanup(self.valid_id_token.stop)

    def tearDown(self):
        (
            receiver.ENABLED,
            receiver.INBOX_EMULATOR_ENABLED,
            receiver.PUSH_AUDIENCE,
            receiver.PUSH_EMAIL,
            receiver.PUSH_SUBSCRIPTION,
        ) = self.old_config

    def new_message(self, event):
        message_id = str(uuid.uuid4().int)
        ref = self.db.collection(COLLECTION).document(doc_id(message_id))
        self.addCleanup(ref.delete)
        return envelope(event, message_id), ref

    def post(self, body, headers=AUTH):
        return self.client.post("/pubsub/rtdn", json=body, headers=headers)

    def test_play_test_http_to_firestore_create_then_replay(self):
        body, ref = self.new_message(PLAY_TEST)
        first = self.post(body)
        self.assertEqual(first.status_code, 204, first.get_data(as_text=True))
        row = ref.get()
        self.assertTrue(row.exists)
        first_row = row.to_dict()
        self.assertEqual(first_row["kind"], "test")
        self.assertEqual(first_row["status"], "test_only")
        self.assertEqual(first_row["pubsubMessageId"], body["message"]["messageId"])
        self.assertEqual(self.post(body).status_code, 204)
        second_row = ref.get().to_dict()
        self.assertEqual(first_row, second_row)  # Duplicate did not mutate record.

    def test_conflicting_duplicate_http_fails_closed(self):
        body, ref = self.new_message(PLAY_TEST)
        self.assertEqual(self.post(body).status_code, 204)
        before = ref.get().to_dict()
        changed = envelope(
            dict(PLAY_TEST, eventTimeMillis="1111111111111"),
            body["message"]["messageId"],
        )
        self.assertEqual(self.post(changed).status_code, 503)
        self.assertEqual(ref.get().to_dict(), before)

    def test_real_refund_hint_is_persisted_but_never_acked(self):
        body, ref = self.new_message(PLAY_REFUND)
        response = self.post(body)
        self.assertEqual(response.status_code, 503)
        self.assertEqual(response.get_json()["error"], "processing_not_configured")
        row = ref.get()
        self.assertTrue(row.exists)
        data = row.to_dict()
        self.assertEqual(data["kind"], "recheck_voided_purchases")
        self.assertEqual(data["status"], "pending_manual_review")
        self.assertEqual(data["purchaseDigest"], hashlib.sha256(
            TOKEN.encode("utf-8")
        ).hexdigest())
        self.assertNotIn(TOKEN, str(data))
        self.assertNotIn("data", data)
        self.assertEqual(self.post(body).status_code, 503)

    def test_invalid_auth_is_rejected_before_any_inbox_write(self):
        body, ref = self.new_message(PLAY_TEST)
        with patch.object(receiver.id_token, "verify_oauth2_token",
                          side_effect=ValueError("invalid signature")):
            self.assertEqual(self.post(body).status_code, 401)
        self.assertFalse(ref.get().exists)


if __name__ == "__main__":
    unittest.main()
