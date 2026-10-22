# Google Play Billing — price preview (not checkout)

This change adds a **read-only** Play Billing 8 integration for **ADULT** users after the age gate.
The coin store shows Play's localized currency prices if the 3 products exist and are eligible:
- `zasnake_coins_1000`
- `zasnake_coins_2000`
- `zasnake_coins_4000`

This deliberately does **not** open purchase flows, grant coins, acknowledge/consume purchases, or collect payment.
The shop buttons remain disabled. Users under 18 never instantiate the Billing client in this stage.

## Test in Android Studio
1. Checkout this branch; build. Test child, teen, and adult age-gate paths. No crashes.
2. In adult mode, open Shop > Coins; ensure loading/unavailable messages make sense.
3. Without Play products configured, the catalog can report unavailable; this is expected.
4. Create product IDs in Play Console, distribute an internal test build, and use a licensed tester's Play account to verify localized prices. Never hardcode currency values.
5. Confirm free rewards and the existing coins economy are unchanged.

## Required before enabling purchases
- Decide product eligibility and review Families policy for this mixed-age game.
- Implement purchase flow using verified, current Play ProductDetails and eligible offers.
- Verify purchase tokens on trusted server, ensure each token awards coins exactly once, and handle pending purchases and refunds/revocations.
- Acknowledge/consume correctly **only after** verified fulfillment, and recover outstanding purchases.
- Test cancellation, retry, offline, repeat purchase, and lifecycle changes.
- Never ship an enabled payment button before the verification flow works.
