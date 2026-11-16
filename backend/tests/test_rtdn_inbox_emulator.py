"""Integration test against the LOCAL demo Firestore emulator only."""
import base64
import json
import os
import uuid
import unittest

from google.cloud import firestore
from rtdn_inbox import InboxError, record_rtdn_hint

SUB = "projects/zasnake/subscriptions/zasnake-google-play-rtdn-staging-pull"


@unittest.skipUnless(
    os.environ.get("FIRESTORE_EMULATOR_HOST") == "127.0.0.1:8085"
    and os.environ.get("GCLOUD_PROJECT") == "demo-zasnake-billing"
    and os.environ.get("GOOGLE_CLOUD_PROJECT") == "demo-zasnake-billing",
    "local Firestore emulator is required",
)
class InboxEmulatorTests(unittest.TestCase):
    def test_atomic_create_then_duplicate_read_and_collision_rejection(self):
        db = firestore.Client(project="demo-zasnake-billing")
        msg_id = str(uuid.uuid4().int)
        notification = {
            "version": "1.0", "packageName": "com.zayedart.zasnake",
            "testNotification": {"version": "1.0"},
        }
        payload = {
            "subscription": SUB, "message": {
                "messageId": msg_id,
                "data": base64.b64encode(json.dumps(notification).encode()).decode()
            }
        }
        first = record_rtdn_hint(db, payload, expected_subscription=SUB)
        second = record_rtdn_hint(db, payload, expected_subscription=SUB)
        self.assertEqual(first, {"outcome": "created", "kind": "test"})
        self.assertEqual(second, {"outcome": "duplicate", "kind": "test"})
        notification["eventTimeMillis"] = "1"
        payload["message"]["data"] = base64.b64encode(
            json.dumps(notification).encode()
        ).decode()
        with self.assertRaisesRegex(InboxError, "conflicting_duplicate"):
            record_rtdn_hint(db, payload, expected_subscription=SUB)


if __name__ == "__main__":
    unittest.main()
