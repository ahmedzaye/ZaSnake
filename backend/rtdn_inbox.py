"""Emulator-only, immutable Google Play RTDN inbox prototype.

This module is wired to an OPTIONAL, disabled-by-default receiver preview
which only permits writes to the local Firestore emulator. No live Pub/Sub
push subscription, Cloud Run service, billing or automatic reconciler is
wired. The only write target is demo-zasnake-billing at 127.0.0.1:8085.

Store only vetted event metadata and SHA-256 digests, never raw notification
payloads, OAuth tokens, purchase tokens, user identities, or order IDs.
An incoming RTDN notification is only a *hint*, never proof of payment/refund.
"""
import base64
import binascii
import hashlib
import os

from google.api_core.exceptions import AlreadyExists, Conflict
from google.cloud import firestore
from play_refund_events import decode_rtdn_envelope, RefundEventError

EMULATOR_HOST = "127.0.0.1:8085"
EMULATOR_PROJECT = "demo-zasnake-billing"
COLLECTION = "rtdn_inbox_staging"


class InboxError(ValueError):
    """The inbox refused an unsafe configuration, envelope or duplicate."""


def _require_local_emulator(db) -> None:
    """Strong defense in depth: fail before ANY Firestore operation."""
    if (os.environ.get("FIRESTORE_EMULATOR_HOST") != EMULATOR_HOST
            or os.environ.get("GCLOUD_PROJECT") != EMULATOR_PROJECT
            or os.environ.get("GOOGLE_CLOUD_PROJECT") != EMULATOR_PROJECT
            or getattr(db, "project", None) != EMULATOR_PROJECT):
        raise InboxError("emulator_required")


def _validate_pubsub(envelope: dict, expected_subscription: str) -> tuple[str, str, str]:
    if (not isinstance(expected_subscription, str)
            or not expected_subscription.startswith("projects/zasnake/subscriptions/")
            or len(expected_subscription) > 200
            or envelope.get("subscription") != expected_subscription):
        raise InboxError("unexpected_subscription")
    message = envelope.get("message")
    if not isinstance(message, dict):
        raise InboxError("invalid_message")
    message_id = message.get("messageId")
    if (not isinstance(message_id, str) or not message_id.isascii()
            or not message_id.isdigit() or len(message_id) > 128):
        raise InboxError("invalid_message_id")
    encoded = message.get("data")
    # Full envelope validation (size, base64, JSON, package, event type) is
    # separately performed by decode_rtdn_envelope before touching Firestore.
    if not isinstance(encoded, str):
        raise InboxError("invalid_data")
    try:
        payload = base64.b64decode(encoded, validate=True)
    except (binascii.Error, ValueError):
        raise InboxError("invalid_data") from None
    payload_digest = hashlib.sha256(payload).hexdigest()
    return expected_subscription, message_id, payload_digest


def record_rtdn_hint(db, envelope: dict, *, expected_subscription: str) -> dict:
    """Atomically create a unique event or validate an exact duplicate.

    Idempotency scope: subscription + Pub/Sub messageId. A repeated messageId
    with different bytes must FAIL, not be silently acknowledged.

    This does not ACK a message or authorize any wallet activity. Callers
    must independently authenticate Pub/Sub before using it in any live path.
    """
    _require_local_emulator(db)
    if not isinstance(envelope, dict):
        raise InboxError("invalid_envelope")
    try:
        event = decode_rtdn_envelope(envelope)
    except (RefundEventError, ValueError, TypeError):
        raise InboxError("invalid_rtdn") from None
    subscription, message_id, payload_digest = _validate_pubsub(
        envelope, expected_subscription
    )
    doc_id = hashlib.sha256((subscription + "\x00" + message_id).encode()).hexdigest()
    record = {
        "schemaVersion": 1,
        "source": "google_play_rtdn",
        "subscription": subscription,
        "pubsubMessageId": message_id,
        "payloadSha256": payload_digest,
        "kind": event["kind"],
        "status": "test_only" if event["kind"] == "test" else "pending_manual_review",
        "createdAt": firestore.SERVER_TIMESTAMP,
    }
    # Decoded details allow a future review worker to look up a known purchase
    # by digest. Never save any field containing raw purchase token or SKU.
    for field in ("purchaseDigest", "notificationType", "refundType"):
        if field in event:
            record[field] = event[field]
    reference = db.collection(COLLECTION).document(doc_id)
    try:
        reference.create(record)
        return {"outcome": "created", "kind": event["kind"]}
    except (Conflict, AlreadyExists):
        # There is no upsert. Race with another delivery is safe. A changed
        # message with the same Pub/Sub ID cannot overwrite an existing record.
        snap = reference.get()
        if not snap.exists:
            raise InboxError("duplicate_unreadable")
        stored = snap.to_dict()
        immutable_keys = (
            "schemaVersion", "source", "subscription", "pubsubMessageId",
            "payloadSha256", "kind", "purchaseDigest",
            "notificationType", "refundType"
        )
        if (not isinstance(stored, dict)
                or any(stored.get(k) != record.get(k) for k in immutable_keys)):
            raise InboxError("conflicting_duplicate")
        return {"outcome": "duplicate", "kind": event["kind"]}
