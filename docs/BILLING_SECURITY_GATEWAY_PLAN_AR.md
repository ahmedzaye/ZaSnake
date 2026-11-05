# Za Snake — Gateway + Wallet staging plan

**Status: proposed, not deployed, payments remain disabled.**

## Reconfirmed project resources
- Cloud project: `zasnake` / `914975720790`
- Private service: `zasnake-billing-staging`, region `europe-west1`
- Firestore: default database `europe-west1`
- Billing runtime identity: `zasnake-billing-server@zasnake.iam.gserviceaccount.com`
- Web OAuth secret: `pgs-web-client-secret` in Secret Manager
- Android application ID: `com.zayedart.zasnake`
- Firebase is now attached to `zasnake`, but its Android App ID must be copied from Firebase settings.

## Public gateway design (new `billing-gateway/`)
- Expose ONLY `POST /session`, `POST /claim`, `GET /grants` when explicitly enabled.
- Firebase App Check `X-Firebase-AppCheck` verified on EVERY request (using Admin SDK), matched against the expected Firebase Android App ID.
- Limit request size, upstream response size, allowed endpoints, methods, URL, and redirect behavior.
- Gateway service account is separate and should have ONLY `roles/run.invoker` on the private Billing service.
- Authentication to the private Cloud Run service via `X-Serverless-Authorization`; the player's session token stays in `Authorization`.
- Billing service remains `--no-allow-unauthenticated` with Firestore/Play API permissions.
- The gateway MUST NOT possess the Billing OAuth secret, Play Developer API access or Firestore roles.
- Firebase App Check only attests a registered app; it is NOT player authentication, nor a complete replay/abuse defense.
- Public service still needs rate limiting and abuse monitoring (for example, API gateway or Cloud Armor where supported), strict OAuth/PGS tests, and consistent error handling.

## Next prerequisites
1. Configure **Firebase App Check → Android → Play Integrity** for package `com.zayedart.zasnake`, using the appropriate Play signing SHA-256. Debug testing requires a separately managed App Check debug provider/token; do not hardcode debug tokens or disable App Check for production.
2. Add Firebase App Check SDK initialization AFTER age gate for allowed adults; debug/release providers need different configuration. Preserve existing age-based rules. Use `FirebaseAppCheck.getInstance().getAppCheckToken(false)`, supply `X-Firebase-AppCheck` on calls to the gateway, and re-fetch if expired. Do not expose service-account keys or OAuth Client Secret.
3. Deploy gateway **disabled** first, with runtime `GATEWAY_ENABLED=false` and a dedicated service account. Only enable/publicize after verifying App Check plus rate limits and protected routes.
4. Route Android to the gateway (never directly to the current private Cloud Run service).
5. Wallet: `backend/wallet_rules.py` provides **pure proposed transition logic only**, NOT a deployed ledger. Implement Firestore transactions; bind purchase player ID, product and token digest; authorize all spends and entitlements on the server. Existing Android save-game statistics can be user-edited; their balances/spends MUST NOT be used as authoritative paid currency or as proof of purchase.
6. Verify Play purchase V2 and Google Play purchase ownership/quantity. Maintain idempotent purchase claims, pending/retry pathways, and Google Play consume/acknowledge deadlines.
7. Add dedicated RTDN Pub/Sub handler + scheduled Voided Purchases API reconciliation, handling full and partial refunds as well as chargebacks. Revoke coins appropriately (including debt when already spent). Log only hashes/non-sensitive IDs and record outcomes.
8. Test unit/integration and Play License Testers: signed-in sessions, App Check, expired sessions, retries, client tampering, repeated tokens, simultaneous claims, PENDING, cancel, consume ambiguity, refunds, cloud merge, offline, cross-device, reinstallation. Provide migrations for existing users.
9. Make a signed release test (AAB versionCode 4) only once all blockers close. Ensure payments do NOT go live accidentally by build flag or hidden debug paths.

## Rollout
```bash
# Deliberate staging smoke test ONLY, not public:
gcloud run deploy zasnake-billing-gateway-staging \
  --project=zasnake \
  --region=europe-west1 \
  --source=./billing-gateway \
  --service-account=zasnake-gateway@zasnake.iam.gserviceaccount.com \
  --set-env-vars=GATEWAY_ENABLED=false,GOOGLE_CLOUD_PROJECT=zasnake \
  --no-allow-unauthenticated \
  --min-instances=0 \
  --max-instances=1
```
The service account above is NOT created automatically. Create and grant it only `run.invoker` on the private service after ensuring no broader roles were inherited. Before enabling, configure `BILLING_PRIVATE_URL` and `EXPECTED_FIREBASE_APP_ID`, App Check and rate limiting.

## Kotlin build setting
The local `gradle.properties` entry `kotlin.compiler.execution.strategy=in-process` is correct. Only write comments starting with `#`, not Markdown `*# ...*`. `org.gradle.tooling.parallel` is optional and not needed for the current build.
