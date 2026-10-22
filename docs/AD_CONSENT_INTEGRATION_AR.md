# Za Snake — Privacy consent integration (Android Studio test)

This change does **not** enable live ads, billing, or Play Games.

## Implemented
- Request Google UMP consent information after the age gate for TEEN/ADULT categories only.
- Do not initialize Google Mobile Ads or load a rewarded ad until UMP reports `canRequestAds()`.
- Keep CHILD mode free of ad SDK initialization and Play Games startup.
- Clear loaded ads and invalidate outstanding loading callbacks when UMP no longer permits ad requests.
- Show the UMP privacy-options action in Settings → Account when UMP requires it.
- Retain Google sample AdMob application/ad unit IDs while testing.

## Android Studio verification
1. Pull this branch and run Build → Make Project. Confirm Kotlin compiles.
2. Start with a fresh app-data state. Test birth dates in CHILD, TEEN, ADULT bands separately (clearing app data between tests).
3. Verify child mode neither requests rewarded ads nor shows UMP consent forms; gameplay and rewards remain accessible via the existing free options.
4. Configure a privacy message in AdMob Privacy & messaging for your app, then test UMP with a registered test device and applicable debug geography; verify the privacy options entry appears when UMP requires it.
5. If UMP cannot allow ad requests, no rewarded ad should load or show; gameplay should still work.
6. Test changing consent from Settings → Account and app relaunch / loss of connection.
7. Verify all existing menus, themes, purchases with in-game coins, daily rewards, and saved games still work.

## Pending before production
- Enter your own production AdMob app ID and ad unit IDs. Do not commit secrets, and use test ads while developing.
- Finish AdMob privacy messages and any required consent choices.
- Verify Play Families / ad SDK / age policies using the real merged manifest and device tests.
- Google Play Billing still shows a disabled preview. Do not enable charging until transaction verification and fulfillment are implemented and tested.
- Google Play Games project ID and achievement IDs must come from your Play Console account.
- Complete store disclosures, privacy policy, and final release-device QA.
