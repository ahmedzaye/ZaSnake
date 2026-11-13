"""Validate Play RTDN payloads and Voided Purchases API records.

No HTTP route, Cloud Run deployment or background job is installed here.
Pub/Sub push authentication (IAM + authenticated OIDC push subscription) is a
separate precondition. RTDN alone is NOT proof of a refund: before revocation,
the service must consult the authenticated Google Play Voided Purchases API.
Never log raw purchase tokens, Pub/Sub envelopes or OAuth credentials.
"""
import base64
import binascii
import hashlib
import json

PACKAGE_NAME = "com.zayedart.zasnake"


class RefundEventError(ValueError):
    pass


def purchase_digest(token: str) -> str:
    if not isinstance(token, str) or not (8 <= len(token) <= 4096):
        raise RefundEventError("invalid_purchase_token")
    return hashlib.sha256(token.encode("utf-8")).hexdigest()


def decode_rtdn_envelope(envelope: dict) -> dict:
    """Return a *hint* to re-check with Play; never grant/revoke on this alone.

    Only call after verifying the Pub/Sub push identity at the HTTP boundary.
    Unknown notifications are explicitly classified as ignored, not accepted.
    """
    if not isinstance(envelope, dict):
        raise RefundEventError("invalid_envelope")
    message = envelope.get("message")
    if not isinstance(message, dict):
        raise RefundEventError("missing_message")
    encoded = message.get("data")
    if not isinstance(encoded, str) or len(encoded) > 16384:
        raise RefundEventError("invalid_data")
    try:
        raw = base64.b64decode(encoded, validate=True)
        if len(raw) > 8192:
            raise RefundEventError("oversized_data")
        data = json.loads(raw)
    except (binascii.Error, ValueError, UnicodeDecodeError, TypeError):
        raise RefundEventError("invalid_payload") from None
    if not isinstance(data, dict) or data.get("packageName") != PACKAGE_NAME:
        raise RefundEventError("unexpected_package")
    sections = [
        field for field in (
            "testNotification", "oneTimeProductNotification",
            "voidedPurchaseNotification", "subscriptionNotification",
            "pendingRefundReviewNotification",
        ) if field in data
    ]
    if len(sections) != 1:
        raise RefundEventError("ambiguous_notification")
    section_name = sections[0]
    section = data[section_name]
    if not isinstance(section, dict):
        raise RefundEventError("invalid_notification")
    if section_name == "testNotification":
        return {"kind": "test"}
    if section_name == "oneTimeProductNotification":
        notification_type = section.get("notificationType")
        if type(notification_type) is not int or notification_type not in (1, 2):
            raise RefundEventError("unknown_one_time_event")
        digest = purchase_digest(section.get("purchaseToken"))
        return {
            "kind": "purchase_changed",
            "purchaseDigest": digest,
            "notificationType": notification_type,
            "sku": section.get("sku"),
        }
    if section_name == "voidedPurchaseNotification":
        if section.get("productType") != 2:  # PRODUCT_TYPE_ONE_TIME
            return {"kind": "other_product_type"}
        if type(section.get("refundType")) is not int or section.get("refundType") not in (1, 2):
            raise RefundEventError("unknown_refund_type")
        digest = purchase_digest(section.get("purchaseToken"))
        return {
            "kind": "recheck_voided_purchases",
            "purchaseDigest": digest,
            "refundType": section["refundType"],
        }
    return {"kind": "unsupported_notification"}


def validated_voided_digest(item: dict) -> str:
    """Only after item was obtained via authenticated Play Voided Purchases API.

    We validate and reject quantity-based partial refunds: Za Snake sells only
    quantity=1 coins packs. A partial refund must trigger investigation, not a
    guessed credit/debit. Absence of voidedQuantity represents full void.
    """
    if not isinstance(item, dict):
        raise RefundEventError("invalid_voided_record")
    digest = purchase_digest(item.get("purchaseToken"))
    millis = item.get("voidedTimeMillis")
    if not isinstance(millis, str) or not millis.isascii() or not millis.isdigit() or int(millis) <= 0:
        raise RefundEventError("invalid_voided_time")
    if "voidedQuantity" in item:
        raise RefundEventError("unsupported_quantity_refund")
    return digest
