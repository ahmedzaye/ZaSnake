# Publisher-provided service configuration (23 Oct 2026)

The publisher supplied the following public **identifiers**. They are now applied to the Android resources on this branch:
- AdMob App ID: `ca-app-pub-6495262421269253~4884698925`
- Rewarded revive: `ca-app-pub-6495262421269253/5814637214`
- Rewarded double coins: `ca-app-pub-6495262421269253/4581446596`
- Play Games project ID: `914975720790`
- UMP message: publisher reports published.
- One-time products: publisher has shown all three in Play Console.

Debug builds request Google's sample rewarded unit; release builds use the publisher's IDs after age gate and UMP consent.

## Remaining account configuration (cannot be established from public IDs)
1. **Play Games**: verify the linked Android app for `com.zayedart.zasnake`, correct Google Play **app-signing SHA-1**, configured tester accounts, the Saved Games feature, and publish the Games Services configuration. Replace the 11 placeholder achievement IDs with real Play Games achievement IDs. An App ID alone is not proof of successful sign-in or cloud sync.
2. **Server OAuth**: create a **Web application** OAuth Client ID for the Play Games server authorization flow. Put its **public client ID** in `pgs_web_client_id` in resources. Store the **client secret** only in Google Cloud Secret Manager, never on Android or GitHub. Confirm the server code exchange configuration works in actual testing.
3. **Cloud Run**: configure a GCP project and billing, Firestore, Secret Manager, Google Play Android Developer API and a least-privilege service account linked to Play Console. Security-review the backend skeleton before deploying. Set the deployed HTTPS URL in `billing_backend_url`. Without these values the payment button fails closed.
4. **AdMob**: verify both real rewarded units are approved/ready and UMP published for the correct app, register test devices and test adult/teen/child scenarios. Do **not** click live ads when testing.
5. **Play purchases**: confirm each one-time product has an active purchase option and tester eligibility. Verify real Play internal-track test buys, failures, pending states, refunds, retries, consumption, ledger recovery, cross-device grants.
6. **Age compliance**: Child stays ad-free and PGS-disabled; under-18 Billing stays disabled in this build pending Families policy review.

**Unverified**: Android Studio build, real Play Games authentication, actual AdMob ad fills, UMP message appearance, cloud backend, paid purchase end-to-end. Do not call this production-ready.
