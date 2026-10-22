# Za Snake — production services integration status

## What is in this branch
- A Cloud Run Flask skeleton for Play receipt verification via Google Play Developer API, Firestore token uniqueness, and server consumption.
- Externalized AdMob app ID and separate rewarded ad-unit IDs.
- Existing Play Games Saved Games, sign-in and achievements adapter remains, pending real Console IDs.

## CRITICAL: this is NOT a completed production payment rollout
- The Android client has NOT been connected to the server purchase API in this branch.
- The purchase button remains disabled; no money can be charged through the game.
- Backend code has not been deployed or integration-tested.
- Before deploying or accepting payments, security-review server OAuth code exchange, tokens, the purchase-token/obfuscated-account-ID checks, concurrency and Firestore rules/permissions. Set authenticated request quotas, error handling, monitoring, and secrets.
- Do NOT configure live purchase controls or production endpoint until both client and backend are tested together.

## Console information required from account owner
1. AdMob: production app ID plus Rewarded IDs for REVIVE and DOUBLE_COINS. Existing resource values are Google's SAMPLE IDs, not real ad placements.
2. Play Games Services: numeric game project ID and achievement IDs (see strings.xml), configured Saved Games feature, linked Android application and signing certificate.
3. Google Cloud: Cloud project for Firestore + Cloud Run, OAuth PGS Web client ID/secret; grant a narrowly scoped Cloud Run service account access to Google Play Android Publisher API for this app.
4. Play Console: activate product IDs `zasnake_coins_1000`, `zasnake_coins_2000`, `zasnake_coins_4000`, configure license testers and internal track.

## Age gate
This mixed-audience game currently disables AdMob + PGS initialization for CHILD and Billing for CHILD/TEEN. Preserve those restrictions unless separately reviewed for Google Play Families compliance.

## Android Studio
- Review build and resources after checkout; do NOT deploy just by merging.
- Keep using Google's test ad units until own AdMob setup is complete; use registered test devices when real ad units are introduced.
- Once the actual PGS project ID is entered, verify account sign-in, manual snapshot conflicts, achievements and cross-device restoration using Play internal testing accounts.
- To publish: end-to-end test paid purchase success, cancel, pending, refund, repeat-purchase, backend outage, restore, sign-out, and multi-device merge.
