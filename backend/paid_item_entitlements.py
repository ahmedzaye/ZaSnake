"""Staging-only SERVER-OWNED cosmetic entitlements funded by paid coins.

Not exposed through Flask/Cloud Run/Android. A future handler MUST use the
authenticated session's player ID, not a player ID supplied in request JSON.
Existing local/cloud free-coin purchases are NOT imported, replaced, or charged.

The paid wallet's idempotent 'item:<catalog ID>' spend itself is the proof of
paid ownership: one atomic Firestore wallet transaction performs the spend and
persists that proof. No split ownership document or client-submitted coin cost.
"""
from google.cloud import firestore

from wallet_repository import (
    PREMIUM_ITEM_PRICES, _doc_for, _player_key, _state_from_document,
)
from wallet_rules import WalletError, WalletState, spend


def _item_price(item_id: str) -> int:
    if not isinstance(item_id, str) or item_id not in PREMIUM_ITEM_PRICES:
        raise WalletError("unknown_item")
    return PREMIUM_ITEM_PRICES[item_id]


def _purchased_paid_item_ids(state: WalletState) -> set[str]:
    owned = set()
    for operation, cost in state.spends.items():
        if not operation.startswith("item:"):
            continue
        item_id = operation[len("item:"):]
        # Fail closed on inconsistent/malformed ownership records. Existing
        # spends may retain an older catalog price following legitimate changes.
        if item_id not in PREMIUM_ITEM_PRICES or type(cost) is not int or cost <= 0:
            raise WalletError("invalid_paid_item_entitlement")
        owned.add(item_id)
    return owned


def decide_paid_item_purchase(state: WalletState, item_id: str):
    """Pure function for transaction retries; prices always come from server.

    Returns (updated_state, newly_charged_coins, already_owned). The idempotent
    item key deliberately prevents double spending across devices/retries.
    """
    price = _item_price(item_id)
    operation = "item:" + item_id
    if operation in state.spends:
        # A previous successful server purchase, possibly at an old price.
        if type(state.spends[operation]) is not int or state.spends[operation] <= 0:
            raise WalletError("invalid_paid_item_entitlement")
        return state, 0, True
    updated = spend(state, operation, price)
    return updated, price, False


def _purchase_impl(transaction, wallet_ref, item_id):
    """Transaction body separated from the library decorator for offline tests."""
    # Every read comes before writes; no nested transaction or external API.
    snapshot = wallet_ref.get(transaction=transaction)
    state = _state_from_document(snapshot.to_dict() if snapshot.exists else None)
    _purchased_paid_item_ids(state)  # fail closed on corrupt item evidence
    updated, charged, already_owned = decide_paid_item_purchase(state, item_id)
    if updated != state:
        transaction.set(wallet_ref, _doc_for(updated))
    return {
        "itemId": item_id,
        "chargedPaidCoins": charged,
        "alreadyOwnedInPaidWallet": already_owned,
        "remainingPaidBalance": updated.balance,
    }


@firestore.transactional
def _purchase(transaction, wallet_ref, item_id):
    # The production entrypoint stays decorated; Firestore manages retries.
    return _purchase_impl(transaction, wallet_ref, item_id)


def purchase_paid_item_staging(db, authenticated_player_id: str, item_id: str):
    """Internal experimental function: DO NOT call from an open HTTP endpoint.

    Caller must validate Play Games session and resolve any prior/local ownership
    and mixed free/paid coin rules first. Neither is implemented here.
    """
    _item_price(item_id)
    wallet_ref = db.collection("paid_wallets_v1").document(
        _player_key(authenticated_player_id)
    )
    return _purchase(db.transaction(), wallet_ref, item_id)


def read_paid_item_ids_staging(db, authenticated_player_id: str) -> set[str]:
    """Read paid-only ownership. Never union with client-provided unlock lists."""
    wallet_ref = db.collection("paid_wallets_v1").document(
        _player_key(authenticated_player_id)
    )
    snapshot = wallet_ref.get()
    state = _state_from_document(snapshot.to_dict() if snapshot.exists else None)
    return _purchased_paid_item_ids(state)
