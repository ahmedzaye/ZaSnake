"""Offline, fully isolated unit tests for the emulator-only RTDN inbox."""
import base64
import json
import os
import unittest
from unittest.mock import patch

from google.api_core.exceptions import Conflict

from rtdn_inbox import InboxError, record_rtdn_hint


SUB = "projects/zasnake/subscriptions/zasnake-google-play-rtdn-staging-pull"
ENV = {
    "FIRESTORE_EMULATOR_HOST": "127.0.0.1:8085",
    "GCLOUD_PROJECT": "demo-zasnake-billing",
    "GOOGLE_CLOUD_PROJECT": "demo-zasnake-billing",
}
TOKEN = "fake-not-a-real-purchase-token"
BASE = {
    "version": "1.0", "packageName": "com.zayedart.zasnake",
    "eventTimeMillis": "1794775475471",
    "testNotification": {"version": "1.0"}
}
VOIDED = {
    "version": "1.0", "packageName": "com.zayedart.zasnake",
    "voidedPurchaseNotification": {
        "productType": 2, "refundType": 1, "purchaseToken": TOKEN
    }
}


def make_envelope(payload, msg_id="1234567890"):
    return {
        "subscription": SUB,
        "message": {
            "messageId": msg_id,
            "data": base64.b64encode(json.dumps(payload).encode()).decode()
        }
    }


class Snapshot:
    def __init__(self, row):
        self.exists = row is not None
        self.row = row

    def to_dict(self):
        return self.row


class FakeDocument:
    def __init__(self, records, key):
        self.records, self.key = records, key

    def create(self, row):
        if self.key in self.records:
            raise Conflict("already exists")
        self.records[self.key] = dict(row)

    def get(self):
        return Snapshot(self.records.get(self.key))


class FakeCollection:
    def __init__(self, records):
        self.records = records

    def document(self, key):
        return FakeDocument(self.records, key)


class FakeDB:
    project = "demo-zasnake-billing"

    def __init__(self):
        self.records = {}
        self.collections_called = []

    def collection(self, name):
        self.collections_called.append(name)
        return FakeCollection(self.records)


class InboxUnitTests(unittest.TestCase):
    def setUp(self):
        self.db = FakeDB()
        self.env_patch = patch.dict(os.environ, ENV)
        self.env_patch.start()

    def tearDown(self):
        self.env_patch.stop()

    def record(self, envelope, db=None):
        return record_rtdn_hint(db or self.db, envelope,
                                expected_subscription=SUB)

    def test_creates_test_once_and_returns_duplicate_on_redelivery(self):
        event = make_envelope(BASE)
        self.assertEqual(self.record(event), {"outcome": "created", "kind": "test"})
        self.assertEqual(self.record(event), {"outcome": "duplicate", "kind": "test"})
        self.assertEqual(len(self.db.records), 1)

    def test_raw_payload_and_purchase_tokens_never_stored(self):
        result = self.record(make_envelope(VOIDED))
        self.assertEqual(result["kind"], "recheck_voided_purchases")
        data = list(self.db.records.values())[0]
        self.assertEqual(data["status"], "pending_manual_review")
        self.assertEqual(len(data["purchaseDigest"]), 64)
        self.assertNotIn(TOKEN, str(data))
        self.assertNotIn("voidedPurchaseNotification", str(data))
        self.assertNotIn("data", data)
        self.assertNotIn("sku", data)

    def test_same_message_id_changed_payload_fails_closed(self):
        self.record(make_envelope(BASE))
        with self.assertRaisesRegex(InboxError, "conflicting_duplicate"):
            self.record(make_envelope(VOIDED))
        self.assertEqual(len(self.db.records), 1)

    def test_invalid_rtdn_rejected_without_writes(self):
        malformed = make_envelope({"packageName": "com.example.invalid",
                                   "testNotification": {}})
        with self.assertRaisesRegex(InboxError, "invalid_rtdn"):
            self.record(malformed)
        self.assertEqual(self.db.collections_called, [])

    def test_wrong_subscription_rejected_without_writes(self):
        wrong = make_envelope(BASE)
        wrong["subscription"] = "projects/other/subscriptions/not-ours"
        with self.assertRaisesRegex(InboxError, "unexpected_subscription"):
            self.record(wrong)
        self.assertEqual(self.db.collections_called, [])

    def test_missing_message_id_rejected(self):
        invalid = make_envelope(BASE)
        invalid["message"]["messageId"] = ""
        with self.assertRaisesRegex(InboxError, "invalid_message_id"):
            self.record(invalid)
        self.assertEqual(self.db.records, {})

    def test_no_emulator_env_fails_before_db(self):
        with patch.dict(os.environ, {"FIRESTORE_EMULATOR_HOST": ""},
                        clear=False):
            with self.assertRaisesRegex(InboxError, "emulator_required"):
                self.record(make_envelope(BASE))
        self.assertEqual(self.db.collections_called, [])

    def test_production_project_fails_before_db(self):
        self.db.project = "zasnake"
        with self.assertRaisesRegex(InboxError, "emulator_required"):
            self.record(make_envelope(BASE))
        self.assertEqual(self.db.collections_called, [])

    def test_incorrect_emulator_target_fails_before_db(self):
        with patch.dict(os.environ, {"FIRESTORE_EMULATOR_HOST": "remote:8085"}):
            with self.assertRaisesRegex(InboxError, "emulator_required"):
                self.record(make_envelope(BASE))
        self.assertEqual(self.db.collections_called, [])

    def test_firestore_write_error_propagates_without_false_success(self):
        self.db.collection = lambda _: (_ for _ in ()).throw(RuntimeError("offline"))
        with self.assertRaises(RuntimeError):
            self.record(make_envelope(BASE))

    def test_different_message_ids_are_distinct_events(self):
        self.record(make_envelope(BASE, "111"))
        self.record(make_envelope(BASE, "222"))
        self.assertEqual(len(self.db.records), 2)


if __name__ == "__main__":
    unittest.main()
