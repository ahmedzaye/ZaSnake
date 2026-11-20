"""Authenticated legacy free-coin ownership as a PAID-SPEND VETO only.

The old local/Play Games save is client-managed and is not cryptographic proof
of purchase. Reports from it MUST NEVER award paid coins or paid entitlements.
They can only permanently block a player from paying real-money coins for an
item that the player says they already own. Items are merged monotonically, so
an older device cannot erase an already reported ownership veto.
"""
from google.cloud import firestore

from wallet_repository import PREMIUM_ITEM_PRICES, _player_key
from wallet_rules import WalletError

COLLECTION = "legacy_paid_spend_veto_v1"


def validate_legacy_items(value) -> list[str]:
    if (not isinstance(value, list) or len(value) > len(PREMIUM_ITEM_PRICES)
            or any(type(item) is not str or item not in PREMIUM_ITEM_PRICES
                   for item in value) or len(set(value)) != len(value)):
        raise WalletError("invalid_legacy_ownership")
    return sorted(value)


def read_veto_snapshot(snapshot) -> set[str]:
    if not snapshot.exists:
        raise WalletError("legacy_sync_required")
    data = snapshot.to_dict()
    if not isinstance(data, dict) or data.get("schemaVersion") != 1:
        raise WalletError("invalid_legacy_ownership")
    ids = validate_legacy_items(data.get("ownedItemIds"))
    return set(ids)


def legacy_veto_ref(db, player_id: str):
    return db.collection(COLLECTION).document(_player_key(player_id))


def legacy_registered(db, player_id: str) -> bool:
    snapshot = legacy_veto_ref(db, player_id).get()
    if not snapshot.exists:
        return False
    read_veto_snapshot(snapshot)
    return True


def _record_impl(transaction, ref, new_ids: list[str]) -> dict:
    snapshot = ref.get(transaction=transaction)
    existing = read_veto_snapshot(snapshot) if snapshot.exists else set()
    combined = sorted(existing | set(new_ids))
    if not snapshot.exists or combined != sorted(existing):
        transaction.set(ref, {
            "schemaVersion": 1,
            "ownedItemIds": combined,
            "updatedAt": firestore.SERVER_TIMESTAMP,
        })
    return {"legacyOwnershipReady": True, "blockedPaidItemCount": len(combined)}


@firestore.transactional
def _record(transaction, ref, new_ids: list[str]) -> dict:
    return _record_impl(transaction, ref, new_ids)


def register_legacy_veto(db, authenticated_player_id: str, reported_ids) -> dict:
    """The authenticated player can only VETO spending, never create ownership."""
    ids = validate_legacy_items(reported_ids)
    return _record(db.transaction(), legacy_veto_ref(db, authenticated_player_id), ids)
