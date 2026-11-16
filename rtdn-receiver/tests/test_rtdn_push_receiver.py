"""Offline security tests for the *disabled by default* RTDN receiver."""
import base64
import json
import unittest
from unittest.mock import patch

import rtdn_push_receiver as receiver


SUBSCRIPTION = "projects/zasnake/subscriptions/zasnake-rtdn-staging-push"
AUDIENCE = "https://zasnake-rtdn-receiver-123-ew.a.run.app"
EMAIL = "zasnake-rtdn-push@zasnake.iam.gserviceaccount.com"
CLAIMS = {"email": EMAIL, "email_verified": True,
          "iss": "https://accounts.google.com"}
PLAY_TEST = {"version": "1.0", "packageName": "com.zayedart.zasnake",
             "eventTimeMillis": "1794775475471", "testNotification": {"version": "1.0"}}
PLAY_REFUND = {"version": "1.0", "packageName": "com.zayedart.zasnake",
               "voidedPurchaseNotification": {
                   "productType": 2, "refundType": 1,
                   "purchaseToken": "fake-test-token-123456"}}


def envelope(event):
    return {"subscription": SUBSCRIPTION,
            "message": {
                "messageId": "23478792722578822",
                "data": base64.b64encode(json.dumps(event).encode()).decode(),
            }}


class AuthenticatedRtdnPushReceiverTests(unittest.TestCase):
    def setUp(self):
        self.old = (receiver.ENABLED, receiver.PUSH_AUDIENCE,
                    receiver.PUSH_EMAIL, receiver.PUSH_SUBSCRIPTION)
        receiver.ENABLED = True
        receiver.PUSH_AUDIENCE = AUDIENCE
        receiver.PUSH_EMAIL = EMAIL
        receiver.PUSH_SUBSCRIPTION = SUBSCRIPTION
        self.client = receiver.app.test_client()
        self.auth = {"Authorization": "Bearer " + ("fake-jwt-" * 8)}

    def tearDown(self):
        (receiver.ENABLED, receiver.PUSH_AUDIENCE,
         receiver.PUSH_EMAIL, receiver.PUSH_SUBSCRIPTION) = self.old

    def test_disabled_refuses_before_auth(self):
        receiver.ENABLED = False
        with patch.object(receiver.id_token, "verify_oauth2_token") as verify:
            result = self.client.post("/pubsub/rtdn", json=envelope(PLAY_TEST))
        self.assertEqual(result.status_code, 503)
        verify.assert_not_called()

    def test_unconfigured_refuses_before_auth(self):
        for field, bad in (("PUSH_AUDIENCE", "http://localhost:1234"),
                           ("PUSH_EMAIL", "other@example.com"),
                           ("PUSH_SUBSCRIPTION", "")):
            with self.subTest(field=field):
                original = getattr(receiver, field)
                setattr(receiver, field, bad)
                try:
                    with patch.object(receiver.id_token, "verify_oauth2_token") as verify:
                        result = self.client.post("/pubsub/rtdn",
                                                  json=envelope(PLAY_TEST))
                    self.assertEqual(result.status_code, 503)
                    verify.assert_not_called()
                finally:
                    setattr(receiver, field, original)

    def test_missing_or_bad_signature_cannot_ack(self):
        self.assertEqual(self.client.post("/pubsub/rtdn",
                         json=envelope(PLAY_TEST)).status_code, 401)
        with patch.object(receiver.id_token, "verify_oauth2_token",
                          side_effect=ValueError("bad signature")):
            result = self.client.post("/pubsub/rtdn", headers=self.auth,
                                      json=envelope(PLAY_TEST))
        self.assertEqual(result.status_code, 401)

    def test_signed_email_must_match_and_be_verified(self):
        for claim in ({"email": "bad@example.com", "email_verified": True,
                       "iss": "https://accounts.google.com"},
                      {"email": EMAIL, "email_verified": False,
                       "iss": "https://accounts.google.com"},
                      {"email": EMAIL, "email_verified": True,
                       "iss": "other-issuer"}):
            with self.subTest(claim=claim):
                with patch.object(receiver.id_token, "verify_oauth2_token",
                                  return_value=claim):
                    response = self.client.post("/pubsub/rtdn", headers=self.auth,
                                                json=envelope(PLAY_TEST))
                self.assertEqual(response.status_code, 401)

    def test_authenticated_play_test_acks_and_checks_audience(self):
        with patch.object(receiver.id_token, "verify_oauth2_token",
                          return_value=CLAIMS) as verify:
            response = self.client.post("/pubsub/rtdn", headers=self.auth,
                                        json=envelope(PLAY_TEST))
        self.assertEqual(response.status_code, 204)
        self.assertEqual(verify.call_args.kwargs["audience"], AUDIENCE)

    def test_bad_subscription_or_notification_never_acks(self):
        with patch.object(receiver.id_token, "verify_oauth2_token",
                          return_value=CLAIMS):
            bad = envelope(PLAY_TEST)
            bad["subscription"] = "projects/other/subscriptions/other"
            self.assertEqual(self.client.post("/pubsub/rtdn",
                             headers=self.auth, json=bad).status_code, 400)
            bad = envelope({"packageName": "com.other", "testNotification": {}})
            self.assertEqual(self.client.post("/pubsub/rtdn",
                             headers=self.auth, json=bad).status_code, 400)
            bad = envelope(PLAY_TEST)
            bad["message"]["messageId"] = ""
            self.assertEqual(self.client.post("/pubsub/rtdn",
                             headers=self.auth, json=bad).status_code, 400)

    def test_real_refunds_are_not_acked_without_durable_reconciliation(self):
        with patch.object(receiver.id_token, "verify_oauth2_token",
                          return_value=CLAIMS):
            response = self.client.post("/pubsub/rtdn", headers=self.auth,
                                        json=envelope(PLAY_REFUND))
        self.assertEqual(response.status_code, 503)
        self.assertEqual(response.get_json()["error"],
                         "processing_not_configured")


if __name__ == "__main__":
    unittest.main()
