"""Conservative Google Play Voided Purchases reconciliation (not deployed).

Only an internal service account with Android Publisher API rights may run this.
Default is DRY RUN. No HTTP endpoint or scheduler invokes this module.

The voided-purchases API covers the most recent 30 days. A production poller
needs frequent overlapping windows, checkpointing/monitoring and RTDN fallback.
"""
import hashlib
from urllib.parse import quote

from atomic_wallet_settlement import (
    CANONICAL_PRODUCTS,
    revoke_authoritatively_verified_purchase,
)
from play_refund_events import RefundEventError, validated_voided_digest

PACKAGE_NAME = "com.zayedart.zasnake"
PLAY_URL = (
    "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/"
    + quote(PACKAGE_NAME, safe="")
    + "/purchases/voidedpurchases"
)
MILLIS_PER_DAY = 24 * 60 * 60 * 1000
MAX_PAGES = 50  # refuse incomplete excessive histories instead of infinite requests


class ReconciliationError(ValueError):
    pass


def _validate_window(start_ms: int, end_ms: int, now_ms: int) -> None:
    if (any(type(x) is not int for x in (start_ms, end_ms, now_ms))
            or start_ms < 0 or start_ms >= end_ms or end_ms > now_ms
            or now_ms - start_ms >= 30 * MILLIS_PER_DAY):
        raise ReconciliationError("invalid_time_window")


def fetch_voided_purchases(session, start_ms: int, end_ms: int,
                           now_ms: int, max_pages: int = MAX_PAGES) -> list[dict]:
    """Return all pages before any Firestore writes are possible.

    Use type=0 (in-app products only), quantity-based refunds included so
    unsupported events are surfaced and cannot be silently missed.
    """
    _validate_window(start_ms, end_ms, now_ms)
    if type(max_pages) is not int or not (1 <= max_pages <= MAX_PAGES):
        raise ReconciliationError("invalid_max_pages")
    result = []
    continuation = None
    seen_tokens = set()
    for _ in range(max_pages):
        params = {
            "type": "0",
            "includeQuantityBasedPartialRefund": "true",
            # REST API PageSelection fields are flattened as dotted names.
            # Top-level maxResults/token are NOT the current REST query names.
            "pageSelection.maxResults": "1000",
        }
        if continuation:
            params["pageSelection.token"] = continuation
        else:
            params["startTime"] = str(start_ms)
            params["endTime"] = str(end_ms)
        # Only call a fixed Google endpoint; no user-provided hostname/redirect.
        response = session.get(PLAY_URL, params=params, timeout=15,
                               allow_redirects=False)
        response.raise_for_status()
        page = response.json()
        if not isinstance(page, dict):
            raise ReconciliationError("invalid_play_response")
        rows = page.get("voidedPurchases", [])
        if not isinstance(rows, list) or len(rows) > 1000:
            raise ReconciliationError("invalid_play_response")
        # Validate every record before any future apply step.
        for row in rows:
            validated_voided_digest(row)
        result.extend(rows)
        pagination = page.get("tokenPagination") or {}
        if not isinstance(pagination, dict):
            raise ReconciliationError("invalid_pagination")
        new_token = pagination.get("nextPageToken")
        if not new_token:
            return result
        if (not isinstance(new_token, str) or len(new_token) > 2048
                or new_token in seen_tokens):
            raise ReconciliationError("invalid_pagination")
        seen_tokens.add(new_token)
        continuation = new_token
    raise ReconciliationError("page_limit_exceeded")


def _match_existing_purchase(db, purchase_digest: str) -> dict | None:
    """Never create credits, players, or purchase records from void events."""
    snapshot = db.collection("purchases").document(purchase_digest).get()
    if not snapshot.exists:
        return None
    row = snapshot.to_dict()
    if not isinstance(row, dict):
        raise ReconciliationError("corrupt_purchase")
    player = row.get("playerId")
    product = row.get("productId")
    account = row.get("accountId")
    if (not isinstance(player, str) or not player or len(player) > 256
            or product not in CANONICAL_PRODUCTS
            or account != hashlib.sha256(player.encode("utf-8")).hexdigest()
            or row.get("coins") != CANONICAL_PRODUCTS[product]
            or row.get("status") not in ("reserved", "consumed", "revoked")):
        raise ReconciliationError("corrupt_purchase")
    return row


def reconcile_window(db, session, start_ms: int, end_ms: int, now_ms: int,
                     *, dry_run: bool = True) -> dict[str, int]:
    """Fetch all Google-verified full voids and apply only when explicitly enabled.

    A Google API request failure or malformed/unsupported partial refund raises,
    rather than silently considering a polling window processed. No plaintext
    purchase tokens are returned. Duplicate events are deduplicated by digest.
    """
    records = fetch_voided_purchases(session, start_ms, end_ms, now_ms)
    matches = []
    unmatched = 0
    deduped = set()
    for record in records:
        digest = validated_voided_digest(record)
        if digest in deduped:
            continue
        deduped.add(digest)
        purchase = _match_existing_purchase(db, digest)
        if purchase is None:
            unmatched += 1
            continue
        matches.append((digest, purchase))
    # All reads and validation must pass before mutating anything.
    if type(dry_run) is not bool:
        raise ReconciliationError("invalid_dry_run")
    if not dry_run:
        for digest, purchase in matches:
            revoke_authoritatively_verified_purchase(
                db, digest, purchase["playerId"], purchase["productId"],
                purchase["accountId"], CANONICAL_PRODUCTS[purchase["productId"]]
            )
    # Counters only: no tokens, order IDs, player IDs, or personal data.
    return {
        "verified_full_refunds": len(deduped),
        "matched_purchases": len(matches),
        "unknown_purchases": unmatched,
        "applied": len(matches) if not dry_run else 0,
        "dry_run": dry_run,
    }
