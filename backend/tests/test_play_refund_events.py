import base64
import json
import unittest

from play_refund_events import (
    RefundEventError, decode_rtdn_envelope, purchase_digest, validated_voided_digest,
)


def envelope(section, name="voidedPurchaseNotification"):
    data = {"packageName": "com.zayedart.zasnake", name: section}
    return {"message": {"data": base64.b64encode(json.dumps(data).encode()).decode()}}


class RefundEventTests(unittest.TestCase):
    def test_voided_notification_is_only_a_hint(self):
        result = decode_rtdn_envelope(envelope({
            "purchaseToken": "test-token-123", "productType": 2, "refundType": 1
        }))
        self.assertEqual(result["kind"], "recheck_voided_purchases")
        self.assertEqual(result["purchaseDigest"], purchase_digest("test-token-123"))
        self.assertNotIn("purchaseToken", result)

    def test_duplicate_event_is_stable(self):
        message = envelope({
            "purchaseToken": "test-token-123", "productType": 2, "refundType": 1
        })
        self.assertEqual(decode_rtdn_envelope(message), decode_rtdn_envelope(message))

    def test_untrusted_package_rejected(self):
        msg = envelope({"purchaseToken": "test-token-123", "productType": 2, "refundType": 1})
        data = json.loads(base64.b64decode(msg["message"]["data"]))
        data["packageName"] = "other.package"
        msg["message"]["data"] = base64.b64encode(json.dumps(data).encode()).decode()
        with self.assertRaises(RefundEventError):
            decode_rtdn_envelope(msg)

    def test_test_notification(self):
        self.assertEqual(decode_rtdn_envelope(envelope({"version": "1.0"}, "testNotification")),
                         {"kind": "test"})

    def test_subscription_event_ignored(self):
        self.assertEqual(
            decode_rtdn_envelope(envelope({"notificationType": 1}, "subscriptionNotification"))["kind"],
            "unsupported_notification",
        )

    def test_malformed_rtdn_rejected(self):
        for case in ({}, None, {"message": {"data": "@@@?"}},
                     {"message": {"data": ""}}, {"message": {"data": "a" * 20000}}):
            with self.subTest(case=case), self.assertRaises(RefundEventError):
                decode_rtdn_envelope(case)

    def test_only_full_voided_purchases(self):
        digest = validated_voided_digest({
            "purchaseToken": "test-token-123",
            "voidedTimeMillis": "1784033300000",
        })
        self.assertEqual(digest, purchase_digest("test-token-123"))
        with self.assertRaises(RefundEventError):
            validated_voided_digest({
                "purchaseToken": "test-token-123", "voidedTimeMillis": "1784033300000",
                "voidedQuantity": 1,
            })

    def test_reject_bad_voided_fields(self):
        for value in (
            {}, {"purchaseToken": "x", "voidedTimeMillis": "1"},
            {"purchaseToken": "test-token-123", "voidedTimeMillis": "tomorrow"},
        ):
            with self.subTest(value=value), self.assertRaises(RefundEventError):
                validated_voided_digest(value)


if __name__ == "__main__":
    unittest.main()
