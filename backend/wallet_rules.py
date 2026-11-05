"""Pure wallet transition rules for a FUTURE transactional, server-authoritative ledger.

This module is not wired to the deployed billing endpoints or Android yet.
Callers must run reads/writes in a Firestore transaction, authenticate the player,
and use only amounts verified through Google Play. Never accept client coin amounts.
"""
from dataclasses import dataclass, field


class WalletError(ValueError):
    pass


@dataclass(frozen=True)
class WalletState:
    """Immutable value; stable purchase IDs and spending IDs survive retries."""
    grants: dict[str, int] = field(default_factory=dict)
    spends: dict[str, int] = field(default_factory=dict)
    revoked: dict[str, int] = field(default_factory=dict)

    @property
    def balance(self) -> int:
        # Negative balances represent debt after late refunds; never erase them.
        return sum(self.grants.values()) - sum(self.spends.values()) - sum(self.revoked.values())


def credit(state: WalletState, purchase_id: str, amount: int) -> WalletState:
    """ONLY invoke after Play verifies ownership, PURCHASED, and quantity=1."""
    if not purchase_id or len(purchase_id) > 128 or type(amount) is not int or amount not in (1000, 2000, 4000):
        raise WalletError("invalid_grant")
    existing = state.grants.get(purchase_id)
    if existing is not None:
        if existing != amount:
            raise WalletError("grant_conflict")
        return state
    return WalletState({**state.grants, purchase_id: amount}, dict(state.spends), dict(state.revoked))


def spend(state: WalletState, operation_id: str, cost: int) -> WalletState:
    """Only use for server-validated catalog operations with stable unique IDs."""
    if not operation_id or len(operation_id) > 128 or type(cost) is not int or cost <= 0:
        raise WalletError("invalid_spend")
    existing = state.spends.get(operation_id)
    if existing is not None:
        if existing != cost:
            raise WalletError("spend_conflict")
        return state
    if state.balance < cost:
        raise WalletError("insufficient_funds")
    return WalletState(dict(state.grants), {**state.spends, operation_id: cost}, dict(state.revoked))


def revoke(state: WalletState, purchase_id: str, amount: int) -> WalletState:
    """Refund events can be retried and partially refunded; amount is TOTAL voided quantity.
    
    For partial quantity/partial monetary refunds, caller must interpret Play receipt
    correctly before deriving amount. No assumption is made here about refunds.
    """
    original = state.grants.get(purchase_id)
    if original is None or type(amount) is not int or amount < 0 or amount > original:
        raise WalletError("invalid_revocation")
    previous = state.revoked.get(purchase_id, 0)
    if amount < previous:
        raise WalletError("revocation_regression")
    if amount == previous:
        return state
    return WalletState(dict(state.grants), dict(state.spends), {**state.revoked, purchase_id: amount})
