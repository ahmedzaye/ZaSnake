import unittest
from wallet_rules import WalletError, WalletState, credit, spend, revoke


class WalletRulesTests(unittest.TestCase):
    def test_idempotent_credit(self):
        one = credit(WalletState(), "purchase1", 1000)
        self.assertIs(one, credit(one, "purchase1", 1000))
        self.assertEqual(one.balance, 1000)
        with self.assertRaises(WalletError):
            credit(one, "purchase1", 4000)

    def test_idempotent_spending(self):
        state = credit(WalletState(), "purchase1", 1000)
        state = spend(state, "skin_egypt", 180)
        self.assertIs(state, spend(state, "skin_egypt", 180))
        self.assertEqual(state.balance, 820)
        with self.assertRaises(WalletError):
            spend(state, "skin_egypt", 250)

    def test_no_overdraft(self):
        with self.assertRaises(WalletError):
            spend(WalletState(), "item", 80)
        state = credit(WalletState(), "p1", 1000)
        with self.assertRaises(WalletError):
            spend(state, "expensive", 1200)

    def test_late_full_refund_preserves_debt(self):
        state = spend(credit(WalletState(), "p1", 1000), "item", 200)
        state = revoke(state, "p1", 1000)
        self.assertEqual(state.balance, -200)
        self.assertIs(state, revoke(state, "p1", 1000))
        with self.assertRaises(WalletError):
            spend(state, "other", 10)

    def test_refund_monotonic_and_bounded(self):
        state = credit(WalletState(), "p1", 1000)
        state = revoke(state, "p1", 400)
        self.assertEqual(state.balance, 600)
        with self.assertRaises(WalletError):
            revoke(state, "p1", 100)
        with self.assertRaises(WalletError):
            revoke(state, "p1", 1001)
        with self.assertRaises(WalletError):
            revoke(state, "unknown", 100)

    def test_reject_bool_amount(self):
        with self.assertRaises(WalletError):
            credit(WalletState(), "p1", True)
        with self.assertRaises(WalletError):
            spend(WalletState(), "item", True)


if __name__ == "__main__":
    unittest.main()
