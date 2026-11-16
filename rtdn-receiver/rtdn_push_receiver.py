"""Staging-only Google Play RTDN push receiver.

Isolated from the billing backend: NO live Firestore, Google Play API, purchase
claims or grants. Defaults disabled. Optional integration persists a vetted
hint in the LOCAL Firestore emulator only. ACK only authenticated Google Play
test messages after an enabled emulator write succeeds; NACK all real
purchase/refund hints until authoritative reconciliation is separately built.

Deployment must additionally require Cloud Run IAM authentication. A Pub/Sub
authenticated push subscription must use the expected service account and
audience, and that SA must have run.invoker on this service ONLY.
"""
import os
from urllib.parse import urlparse

from flask import Flask, jsonify, request
from google.auth.transport.requests import Request as GoogleRequest
from google.oauth2 import id_token

from play_refund_events import RefundEventError, decode_rtdn_envelope

app = Flask(__name__)
app.config["MAX_CONTENT_LENGTH"] = 20_000

ENABLED = os.getenv("RTDN_RECEIVER_ENABLED", "false").lower() == "true"
PUSH_AUDIENCE = os.getenv("RTDN_PUSH_AUDIENCE", "")
PUSH_EMAIL = os.getenv("RTDN_PUSH_SERVICE_ACCOUNT", "")
PUSH_SUBSCRIPTION = os.getenv("RTDN_PUSH_SUBSCRIPTION", "")
INBOX_EMULATOR_ENABLED = (
    os.getenv("RTDN_INBOX_EMULATOR_ENABLED", "false").lower() == "true"
)


def configured() -> bool:
    """Require one concrete HTTPS Cloud Run audience and exact subscriber ID."""
    url = urlparse(PUSH_AUDIENCE)
    return (
        url.scheme == "https" and bool(url.hostname)
        and url.hostname.endswith(".run.app")
        and url.username is None and url.password is None and url.port is None
        and url.path in ("", "/") and not url.query and not url.fragment
        and PUSH_EMAIL.endswith("@zasnake.iam.gserviceaccount.com")
        and PUSH_EMAIL.startswith("zasnake-rtdn-push@")
        and PUSH_SUBSCRIPTION.startswith("projects/zasnake/subscriptions/")
        and len(PUSH_SUBSCRIPTION) <= 200
    )


def signed_push_identity_valid() -> bool:
    bearer = request.headers.get("Authorization", "")
    if not bearer.startswith("Bearer ") or not (20 <= len(bearer) <= 8192):
        return False
    try:
        claims = id_token.verify_oauth2_token(
            bearer[len("Bearer "):], GoogleRequest(), audience=PUSH_AUDIENCE
        )
    except (Exception):
        # Do not log or echo bearer tokens, verification errors or claims.
        return False
    return (
        isinstance(claims, dict)
        and claims.get("email") == PUSH_EMAIL
        and claims.get("email_verified") is True
        and claims.get("iss") in ("accounts.google.com", "https://accounts.google.com")
    )


def _persist_emulator_notification(envelope: dict) -> dict:
    """Persist only in the local demo emulator. Never instantiate a real DB.

    This mode is for controlled offline integration tests. The library also
    enforces the same target before any Firestore operation.
    """
    from google.cloud import firestore
    from rtdn_inbox import (
        EMULATOR_HOST, EMULATOR_PROJECT, InboxError, record_rtdn_hint,
    )

    if (os.environ.get("FIRESTORE_EMULATOR_HOST") != EMULATOR_HOST
            or os.environ.get("GCLOUD_PROJECT") != EMULATOR_PROJECT
            or os.environ.get("GOOGLE_CLOUD_PROJECT") != EMULATOR_PROJECT):
        raise InboxError("emulator_required")
    db = firestore.Client(project=EMULATOR_PROJECT)
    return record_rtdn_hint(
        db, envelope, expected_subscription=PUSH_SUBSCRIPTION
    )


@app.post("/pubsub/rtdn")
def receive():
    if not ENABLED:
        return jsonify({"error": "receiver_disabled"}), 503
    if not configured():
        return jsonify({"error": "receiver_unconfigured"}), 503
    if not signed_push_identity_valid():
        return jsonify({"error": "unauthenticated"}), 401
    if not request.is_json:
        return jsonify({"error": "invalid_envelope"}), 400
    envelope = request.get_json(silent=True)
    if not isinstance(envelope, dict) or envelope.get("subscription") != PUSH_SUBSCRIPTION:
        return jsonify({"error": "invalid_envelope"}), 400
    message = envelope.get("message")
    if not isinstance(message, dict):
        return jsonify({"error": "invalid_envelope"}), 400
    # A stable Pub/Sub ID is required for eventual de-duplication.
    message_id = message.get("messageId")
    if not isinstance(message_id, str) or not message_id.isdigit() or len(message_id) > 128:
        return jsonify({"error": "invalid_envelope"}), 400
    try:
        event = decode_rtdn_envelope(envelope)
    except (RefundEventError, ValueError, TypeError):
        return jsonify({"error": "invalid_notification"}), 400

    if INBOX_EMULATOR_ENABLED:
        try:
            # Only after authenticating the push identity and validating Play.
            # New record and exact duplicate are both safe. Any failure NACKs.
            _persist_emulator_notification(envelope)
        except Exception:
            # Never echo/log OAuth tokens, raw payloads or Firestore errors.
            return jsonify({"error": "inbox_unavailable"}), 503

    if event["kind"] == "test":
        # In emulator mode ACK only if durable create / exact replay passed.
        # Existing disabled-by-default preview behavior remains unchanged.
        return "", 204
    # Any real purchase/refund event MUST be stored durably + reconciled via
    # Google Play API BEFORE returning 2xx. Not implemented in this PR.
    # This intentional 503 makes Pub/Sub retry (and eventually dead-letter,
    # if configured) rather than silently losing the real event.
    return jsonify({"error": "processing_not_configured"}), 503


@app.errorhandler(413)
def too_large(_):
    return jsonify({"error": "request_too_large"}), 413


if __name__ == "__main__":
    # Staging dev server only. Production requires private Cloud Run + Gunicorn.
    app.run(host="127.0.0.1", port=8080)
