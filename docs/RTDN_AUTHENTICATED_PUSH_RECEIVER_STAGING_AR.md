# Za Snake — RTDN Authenticated Push Receiver (STAGING DESIGN ONLY)

**Current state:** Draft code and tests only. No Cloud Run deployment, Pub/Sub push subscription, Firestore/Play API connection, purchase activation, or automated reconciliation.

Existing `zasnake-google-play-rtdn-staging-pull` is a manually inspected **pull subscription**, not a push subscriber. Keep it separate.

## Behavior

- `POST /pubsub/rtdn` defaults to **503 disabled**.
- Enabling requires `RTDN_RECEIVER_ENABLED=true`, a concrete Cloud Run `RTDN_PUSH_AUDIENCE`, exact `RTDN_PUSH_SERVICE_ACCOUNT` value `zasnake-rtdn-push@zasnake.iam.gserviceaccount.com`, and the exact `RTDN_PUSH_SUBSCRIPTION`.
- Requires an Authorization bearer token signed by Google. Verifies JWT signature, expiry, audience, issuer, email and `email_verified`. Fails closed otherwise.
- Only well-formed, correctly scoped Google Play **test notifications** get HTTP 204 (ACK).
- All **real** purchase/refund hints get HTTP 503 (NACK) until **durable idempotent persistence plus reconciliation** is built. This avoids silently dropping real paid events. Never point a live push subscription to this receiver while real events might be delivered without a bounded retry/dead-letter plan.
- No purchase token, player ID, JWT, message content, or identifiable receipt data is logged or returned; no wallet writes or reads.
- The receiver has no Firestore permission and no Play API permission by design.

## Authentication/deployment prerequisites (NOT performed)

1. Deploy to a **separate private Cloud Run service** with no allUsers invoker, no Firestore/Play API permissions, and `RTDN_RECEIVER_ENABLED=false`.
2. Create a **dedicated Pub/Sub push auth service account** separate from the receiver runtime identity.
3. Grant that dedicated push-auth identity `roles/run.invoker` on the single receiver service only, and Pub/Sub service agent the minimal `iam.serviceAccountTokenCreator` permission needed to mint the OIDC token.
4. Later, create a NEW authenticated push subscription (do not convert the existing pull subscription) with the exact URL audience and service account. Ensure `RTDN_PUSH_SUBSCRIPTION` matches.
5. Complete a durable inbox, duplicate event detection, authenticated Voided Purchases API reconciliation, periodic overlap/checkpoints, and bounded retries + DLQ **before allowing production purchase/refund messages to receive 2xx responses**.
6. Keep `GATEWAY_ENABLED=false`, `ATOMIC_BILLING_ENABLED=false`, and Android billing disabled.

## Offline test

From repository root with backend requirements installed:

```sh
PYTHONPATH=rtdn-receiver:backend python -m unittest discover \
  -s rtdn-receiver/tests -p 'test_*.py' -v
```

These tests mock Google JWT validation and never contact Google, Cloud Run, Firestore, or Pub/Sub.

## Official docs
- https://docs.cloud.google.com/pubsub/docs/authenticate-push-subscriptions
- https://docs.cloud.google.com/pubsub/docs/push
- https://developer.android.com/google/play/billing/rtdn-reference
