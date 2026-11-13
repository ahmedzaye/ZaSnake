"""Atomically settle Play-verified consumable purchases into a paid wallet.

EXPERIMENTAL, opt-in only. Never call from unauthenticated/mobile input.
Before finalize:
  * Play Developer API ProductPurchaseV2 is freshly checked for PURCHASED,
    the exact product, quantity 1 and obfuscated player ownership;
  * consumption was confirmed successful, or Play reports already consumed
    for an existing, correctly owned reserved/consumed purchase.
No external API call is made from Firestore transaction retries.
"""
import hashlib

from google.cloud import firestore

from wallet_repository import _doc_for, _state_from_document, _player_key
from wallet_rules import WalletError, credit, revoke

CANONICAL_PRODUCTS = {
    "zasnake_coins_1000": 1000,
    "zasnake_coins_2000": 2000,
    "zasnake_coins_4000": 4000,
}


def _check_digest(digest: str) -> None:
    if (not isinstance(digest, str) or len(digest) != 64 or
            any(c not in "0123456789abcdef" for c in digest)):
        raise WalletError("invalid_purchase_digest")


def _expected_purchase(purchase: dict, player_id: str,
                       product_id: str, account_hash: str) -> int:
    if product_id not in CANONICAL_PRODUCTS:
        raise WalletError("unknown_product")
    amount = CANONICAL_PRODUCTS[product_id]
    if (not isinstance(purchase, dict) or
            purchase.get("playerId") != player_id or
            purchase.get("productId") != product_id or
            purchase.get("accountId") != account_hash or
            purchase.get("coins") != amount):
        raise WalletError("ownership_conflict")
    return amount


@firestore.transactional
def _finalize(transaction, purchase_ref, wallet_ref, purchase_digest,
              player_id, product_id, account_hash):
    # Firestore requires ALL reads before ANY writes.
    p_snap = purchase_ref.get(transaction=transaction)
    w_snap = wallet_ref.get(transaction=transaction)
    if not p_snap.exists:
        raise WalletError("purchase_not_reserved")
    purchase = p_snap.to_dict() or {}
    amount = _expected_purchase(purchase, player_id, product_id, account_hash)
    if purchase.get("status") not in ("reserved", "consumed"):
        raise WalletError("purchase_not_entitled")
    if purchase.get("revokedCoins", 0):
        raise WalletError("purchase_revoked")
    prior = _state_from_document(w_snap.to_dict() if w_snap.exists else None)
    if prior.revoked.get(purchase_digest, 0):
        raise WalletError("purchase_revoked")
    updated = credit(prior, purchase_digest, amount)
    if updated != prior:
        transaction.set(wallet_ref, _doc_for(updated))
    if purchase.get("status") != "consumed" or not purchase.get("walletCreditedAt"):
        transaction.update(purchase_ref, {
            "status": "consumed",
            "walletCreditedAt": firestore.SERVER_TIMESTAMP,
        })
    return {purchase_digest: amount}


def finalize_consumed_purchase(db, purchase_digest: str, player_id: str,
                               product_id: str, account_hash: str) -> dict[str, int]:
    """Called only after fresh Play verification+confirmed consumption.

    Existing consumed rows may be backfilled, but only after fresh verification.
    Both writes are in ONE transaction. Safe on simultaneous retries.
    """
    _check_digest(purchase_digest)
    _expected_purchase({
        "playerId": player_id, "productId": product_id,
        "accountId": account_hash, "coins": CANONICAL_PRODUCTS.get(product_id)
    }, player_id, product_id, account_hash)
    if account_hash != hashlib.sha256(player_id.encode("utf-8")).hexdigest():
        raise WalletError("invalid_account_hash")
    purchase_ref = db.collection("purchases").document(purchase_digest)
    wallet_ref = db.collection("paid_wallets_v1").document(_player_key(player_id))
    return _finalize(db.transaction(), purchase_ref, wallet_ref, purchase_digest,
                     player_id, product_id, account_hash)


@firestore.transactional
def _revoke(transaction, purchase_ref, wallet_ref, digest,
            player_id, product_id, account_hash, total_revoked):
    p_snap = purchase_ref.get(transaction=transaction)
    w_snap = wallet_ref.get(transaction=transaction)
    if not p_snap.exists:
        raise WalletError("purchase_unknown")
    purchase = p_snap.to_dict() or {}
    amount = _expected_purchase(purchase, player_id, product_id, account_hash)
    if (type(total_revoked) is not int or
            total_revoked not in (0, amount)):
        # Quantity 1. Multi-quantity/partial-money refunds need separate logic.
        raise WalletError("unsupported_refund")
    prior = _state_from_document(w_snap.to_dict() if w_snap.exists else None)
    previous = purchase.get("revokedCoins", 0)
    if type(previous) is not int or previous < 0 or previous > total_revoked:
        raise WalletError("revocation_regression")
    # A Google-verified refund may arrive BEFORE a consumed purchase has been
    # credited to the wallet. Record the refund in the purchase ledger even
    # when there is no wallet grant; later finalize will reject revoked rows.
    # If credit exists, revoke against that grant atomically.
    if digest in prior.grants:
        updated = revoke(prior, digest, total_revoked)
    else:
        if purchase.get("status") not in ("reserved", "revoked"):
            raise WalletError("credit_missing")
        updated = prior
    if updated != prior:
        transaction.set(wallet_ref, _doc_for(updated))
    if previous < total_revoked:
        transaction.update(purchase_ref, {
            "revokedCoins": total_revoked,
            "status": "revoked" if total_revoked == amount else purchase.get("status"),
            "revokedAt": firestore.SERVER_TIMESTAMP,
        })
    return updated.balance


def revoke_authoritatively_verified_purchase(db, digest: str, player_id: str,
                                             product_id: str, account_hash: str,
                                             total_revoked: int) -> int:
    """Only an authenticated reconciliation worker may call this after verifying
    a Google Play voided purchase/refund. Not exposed as HTTP endpoint.
    """
    _check_digest(digest)
    if account_hash != hashlib.sha256(player_id.encode("utf-8")).hexdigest():
        raise WalletError("invalid_account_hash")
    purchase_ref = db.collection("purchases").document(digest)
    wallet_ref = db.collection("paid_wallets_v1").document(_player_key(player_id))
    return _revoke(db.transaction(), purchase_ref, wallet_ref, digest,
                   player_id, product_id, account_hash, total_revoked)


def current_net_grants(db, player_id: str) -> dict[str, int]:
    """For an authenticated player only. Return net grants after revocation.

    Conservative: hides any partially revoked purchase until a quantity-aware
    settlement model is implemented. Never trust a client-supplied wallet.
    """
    wallet = db.collection("paid_wallets_v1").document(_player_key(player_id)).get()
    state = _state_from_document(wallet.to_dict() if wallet.exists else None)
    return {digest: amount for digest, amount in state.grants.items()
            if state.revoked.get(digest, 0) == 0}
