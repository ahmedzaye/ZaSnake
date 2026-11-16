# Za Snake: RTDN Durable Inbox — emulator-only draft

This PR layers on **PR #31** and only adds a storage library and tests.
The receiver in PR #31 is **not** wired to this library. There is NO new Cloud Run
deployment, service account, push subscription, live Firestore write, billing,
purchase grant, purchase revocation, scheduler, or automatic ACK for real events.

## Purpose

- Record Google Play RTDN *hints* in a local Firestore emulator, never a real DB.
- Create immutable inbox documents atomically (not `set` / upsert).
- Key each document by SHA-256(subscription + NUL + Pub/Sub messageId).
- Repeated identical delivery returns `duplicate` without writing again.
- Same messageId with a different payload gets `conflicting_duplicate`.
- Persist only event category, vetted enums, purchase-token **digest**,
  payload SHA-256, message ID, subscription, processing status and timestamp.
  Never persist raw purchase tokens, order IDs, user/player IDs, receipt JSON,
  credentials, or arbitrary user-supplied SKU.
- Test events get `test_only`, all other types get `pending_manual_review`.
  Nothing is reconciled, credited, debited, or deleted by this module.

## Fail-closed guard

Every write requires ALL of the following:

- `FIRESTORE_EMULATOR_HOST=127.0.0.1:8085`
- `GCLOUD_PROJECT=demo-zasnake-billing`
- `GOOGLE_CLOUD_PROJECT=demo-zasnake-billing`
- Firestore client's `project` is also `demo-zasnake-billing`

A live Google Cloud project or unconfigured emulator is refused **before any
Firestore operation**. These are development-only safety constraints,
not a production IAM solution.

## Tests (Cloud Shell)

With the emulator running, from repo root:

```sh
FIRESTORE_EMULATOR_HOST=127.0.0.1:8085 \
GCLOUD_PROJECT=demo-zasnake-billing \
GOOGLE_CLOUD_PROJECT=demo-zasnake-billing \
PYTHONPATH=backend python -m unittest discover \
  -s backend/tests -p 'test_*.py' -v
```

The existing tests plus 11 new offline inbox unit tests and one new emulator
integration test should run. **No deployment or enabling flags.**

## Future (separate reviewed changes)

Before shipping any real listener: design a durable replay/reconciliation worker
that re-checks Google Play via verified Play API, idempotent wallet updates, scoped
IAM, bounded push retries / dead-letter storage and monitoring. Build an
authenticated push subscriber only after those controls exist.

Google Cloud Pub/Sub push ACK semantics:
https://docs.cloud.google.com/pubsub/docs/push

Firestore atomic document create:
https://docs.cloud.google.com/python/docs/reference/firestore/latest/google.cloud.firestore_v1.document.DocumentReference
