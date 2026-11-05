"""Strict, dependency-free validation of Google Play ProductPurchaseV2 responses.

This module deliberately does not award coins or treat a device-side purchase as proof.
"""
from collections.abc import Mapping


class PurchaseValidationError(ValueError):
    pass


def validate_product_purchase_v2(receipt, product_id: str, account_id: str) -> str:
    """Return the consumption state only for one completed, owned, nonrefunded purchase.

    The product's buy purchase option must not allow multi-quantity transactions.
    """
    if not isinstance(receipt, Mapping):
        raise PurchaseValidationError("bad_receipt")
    state = receipt.get("purchaseStateContext") or {}
    if not isinstance(state, Mapping) or state.get("purchaseState") != "PURCHASED":
        raise PurchaseValidationError("not_purchased")
    if receipt.get("obfuscatedExternalAccountId") != account_id:
        raise PurchaseValidationError("account_mismatch")
    lines = receipt.get("productLineItem")
    if not isinstance(lines, list) or len(lines) != 1 or not isinstance(lines[0], Mapping):
        raise PurchaseValidationError("invalid_items")
    line = lines[0]
    if line.get("productId") != product_id:
        raise PurchaseValidationError("product_mismatch")
    offer = line.get("productOfferDetails")
    if not isinstance(offer, Mapping):
        raise PurchaseValidationError("invalid_offer")
    qty = offer.get("quantity")
    if type(qty) is not int or qty != 1:
        raise PurchaseValidationError("unsupported_quantity")
    refundable = offer.get("refundableQuantity")
    if refundable is not None and (type(refundable) is not int or refundable != 1):
        raise PurchaseValidationError("fully_or_partially_refunded")
    if offer.get("rentOfferDetails") or offer.get("preorderOfferDetails"):
        raise PurchaseValidationError("unsupported_offer")
    consumption = offer.get("consumptionState")
    if consumption not in ("CONSUMPTION_STATE_YET_TO_BE_CONSUMED", "CONSUMPTION_STATE_CONSUMED"):
        raise PurchaseValidationError("unknown_consumption_state")
    return consumption
