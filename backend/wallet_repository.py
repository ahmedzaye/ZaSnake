"""Staging-only Firestore persistence for server-verified paid-coin ledger.

NOT linked to live endpoints yet. `credit_verified_purchase` must be called only
AFTER Play purchase validation and successful consumption are confirmed, and in
the same transaction that finalizes the verified purchase document.

Never accept a wallet balance or coin amount directly from the Android client.
This wallet is deliberately isolated from the existing local/free-coin economy.
"""
import hashlib
from collections.abc import Callable

from google.cloud import firestore

from wallet_rules import WalletError, WalletState, credit, spend, revoke

# Bounds doc size while a sharded/event-sourced wallet is designed.
MAX_EVENTS_PER_WALLET = 150

# Item IDs and costs are defined by trusted server catalog, never client prices.
# Do not enable this catalog until migration/ownership of legacy local items is
# resolved and all premium purchases are authorized by the server.
PREMIUM_ITEM_PRICES = {
    "skin_egypt": 180, "skin_cyber": 120, "skin_ice": 180, "skin_fire": 250,
    "skin_uae": 180, "skin_jordan": 180, "skin_palestine": 180,
    "skin_kuwait": 180, "skin_saudi": 180,
    "theme_ocean": 80, "theme_sunset": 100, "theme_royal": 120,
    "theme_desert": 140, "theme_rose": 160,
    "head_sharp": 120, "head_cute": 120, "head_royal": 180, "head_cyber": 220,
    "body_smooth": 250, "body_retro_pixel": 200,
    "body_armored": 300, "body_crystal": 350,
}


def _player_key(player_id: str) -> str:
    if not isinstance(player_id, str) or not player_id or len(player_id) > 256:
        raise WalletError("invalid_player")
    return hashlib.sha256(player_id.encode("utf-8")).hexdigest()


def _state_from_document(data: dict | None) -> WalletState:
    if data is None:
        return WalletState()
    if not isinstance(data, dict):
        raise WalletError("invalid_wallet")
    maps = []
    for field in ("grants", "spends", "revoked"):
        values = data.get(field, {})
        if not isinstance(values, dict) or len(values) > MAX_EVENTS_PER_WALLET:
            raise WalletError("invalid_wallet")
        if not all(isinstance(k, str) and 0 < len(k) <= 128 and
                   type(v) is int and v >= 0 for k, v in values.items()):
            raise WalletError("invalid_wallet")
        maps.append(values)
    state = WalletState(*maps)
    if any(value not in (1000, 2000, 4000) for value in state.grants.values()):
        raise WalletError("invalid_wallet")
    if any(k not in state.grants or v > state.grants[k]
           for k, v in state.revoked.items()):
        raise WalletError("invalid_wallet")
    return state


def _doc_for(state: WalletState) -> dict:
    if (len(state.grants) > MAX_EVENTS_PER_WALLET or
        len(state.spends) > MAX_EVENTS_PER_WALLET or
        len(state.revoked) > MAX_EVENTS_PER_WALLET):
        raise WalletError("wallet_requires_compaction")
    return {
        "schemaVersion": 1,
        "grants": state.grants,
        "spends": state.spends,
        "revoked": state.revoked,
        "updatedAt": firestore.SERVER_TIMESTAMP,
    }


@firestore.transactional
def _apply(transaction, wallet_ref, change: Callable[[WalletState], WalletState]):
    snapshot = wallet_ref.get(transaction=transaction)
    state = _state_from_document(snapshot.to_dict() if snapshot.exists else None)
    result = change(state)
    # A transaction retries safely because the changes are deterministic and
    # each operation is idempotent by a stable purchase or item identifier.
    if result != state:
        transaction.set(wallet_ref, _doc_for(result))
    return result


def _run(db, player_id: str, change: Callable[[WalletState], WalletState]):
    wallet_ref = db.collection("paid_wallets_v1").document(_player_key(player_id))
    return _apply(db.transaction(), wallet_ref, change)


def credit_verified_purchase(db, player_id: str, purchase_digest: str, coins: int):
    """Called only from trusted Play validation/consumption; not a public HTTP API.

    IMPORTANT: When integrated, update the purchase ledger's consumed state in
    this SAME Firestore transaction, or a crash may leave inconsistent ledgers.
    This standalone function is for schema review/isolated testing only.
    """
    if (not isinstance(purchase_digest, str) or len(purchase_digest) != 64 or
            any(ch not in "0123456789abcdef" for ch in purchase_digest)):
        raise WalletError("invalid_purchase_digest")
    return _run(db, player_id, lambda s: credit(s, purchase_digest, coins))


def spend_verified_item(db, player_id: str, item_id: str):
    """Future only: require authenticated player, item ownership migration,
    and exclusive server-authoritative coin spending across ALL game coins.
    Do not expose this until the existing free/local currency is reconciled.
    """
    if item_id not in PREMIUM_ITEM_PRICES:
        raise WalletError("unknown_item")
    return _run(db, player_id,
                lambda s: spend(s, "item:" + item_id, PREMIUM_ITEM_PRICES[item_id]))


def revoke_purchase(db, player_id: str, purchase_digest: str, total_revoked_coins: int):
    """Future reconciliation: TOTAL revoked amount from authoritative Google
    Play events; Firestore transaction remains idempotent on repeated events.
    """
    if (not isinstance(purchase_digest, str) or len(purchase_digest) != 64 or
            any(ch not in "0123456789abcdef" for ch in purchase_digest)):
        raise WalletError("invalid_purchase_digest")
    return _run(db, player_id, lambda s: revoke(s, purchase_digest, total_revoked_coins))
