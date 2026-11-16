# RTDN receiver ↔ durable inbox, local-emulator integration

**Draft, not deployed, not activated on any real Play purchase.**

Builds on PR #32 (already merged to main), with **an explicitly opt-in
development-only integration**. The receiver remains disabled unless the
existing `RTDN_RECEIVER_ENABLED=true` flag is set, and the inbox integration
remains disabled unless a second flag `RTDN_INBOX_EMULATOR_ENABLED=true` is set.

The opt-in integration:

- Verifies signed Google Pub/Sub push OIDC identity and expected audience/email.
- Validates subscription, Play package, Pub/Sub message ID and event kind.
- Requires *all* of `FIRESTORE_EMULATOR_HOST=127.0.0.1:8085`,
  `GCLOUD_PROJECT=demo-zasnake-billing` and
  `GOOGLE_CLOUD_PROJECT=demo-zasnake-billing`.
- Creates an immutable, minimum-data inbox hint in the local emulator.
- Replayed identical Pub/Sub message IDs return safe duplicate, not write.
- An authenticated Play test event returns HTTP 204 ONLY if emulator
  persistence returned successfully. Failure returns HTTP 503.
- **All real payment and refund notifications always return HTTP 503**,
  even if their hint was persisted. This avoids falsely acknowledging a
  financially relevant hint without Google Play verification/reconciliation.
- Never grants/charges/revokes wallet funds; does not modify Cloud Run,
  Cloud Pub/Sub subscriptions, project IAM or billing flags.

**Important:** This is not a production-ready receiver. Real event messages
will retry and require a durable reconciliation worker + dead-letter strategy,
then a separate security review before live push subscriptions are connected.

## Tests in Cloud Shell

From repo root:

```sh
PYTHONPATH=rtdn-receiver:backend python -m unittest discover \
  -s rtdn-receiver/tests -p 'test_*.py' -v
```

For the backend suite plus emulator:
```sh
FIRESTORE_EMULATOR_HOST=127.0.0.1:8085 \
GCLOUD_PROJECT=demo-zasnake-billing \
GOOGLE_CLOUD_PROJECT=demo-zasnake-billing \
PYTHONPATH=backend python -m unittest discover \
  -s backend/tests -p 'test_*.py' -v
```

No one should deploy this yet. In particular, `RTDN_INBOX_EMULATOR_ENABLED`
must never be set for production services; its exclusive target is the local
development emulator.
