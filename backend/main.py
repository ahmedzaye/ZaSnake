"""Cloud Run purchase verifier for Za Snake (adult players signed in with PGS).

Deploy with Cloud Run ADC service account that has Google Play Developer API access
for this app and Firestore permissions. NEVER put OAuth client secret in Android.
"""
import hashlib
import os
import time
from datetime import datetime, timedelta, timezone
from urllib.parse import quote

import google.auth
from google.auth.transport.requests import AuthorizedSession
from google.cloud import firestore
from flask import Flask, jsonify, request
import requests

app = Flask(__name__)
db = firestore.Client()
PACKAGE = "com.zayedart.zasnake"
PRODUCTS = {"zasnake_coins_1000": 1000, "zasnake_coins_2000": 2000, "zasnake_coins_4000": 4000}
SCOPES = ["https://www.googleapis.com/auth/androidpublisher"]


def failure(message, status):
    return jsonify({"error": message}), status


def android_publisher():
    creds, _ = google.auth.default(scopes=SCOPES)
    return AuthorizedSession(creds)


def play_url(product, token):
    return (
        "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/"
        + quote(PACKAGE, safe="") + "/purchases/products/"
        + quote(product, safe="") + "/tokens/" + quote(token, safe="")
    )


def session_player():
    header = request.headers.get("Authorization", "")
    if not header.startswith("Bearer "):
        return None
    digest = hashlib.sha256(header[7:].encode("utf-8")).hexdigest()
    snap = db.collection("sessions").document(digest).get()
    if not snap.exists:
        return None
    info = snap.to_dict()
    if info.get("expiresAt", 0) < time.time():
        return None
    return info.get("playerId")


@app.post("/session")
def create_session():
    payload = request.get_json(silent=True) or {}
    code = payload.get("serverAuthCode", "")
    if not isinstance(code, str) or len(code) < 12 or len(code) > 4096:
        return failure("invalid_code", 400)
    client_id = os.environ["PGS_WEB_CLIENT_ID"]
    client_secret = os.environ["PGS_WEB_CLIENT_SECRET"]
    try:
        token_resp = requests.post(
            "https://oauth2.googleapis.com/token",
            data={"code": code, "client_id": client_id,
                  "client_secret": client_secret, "grant_type": "authorization_code",
                  "redirect_uri": ""},
            timeout=12,
        )
        token_resp.raise_for_status()
        access = token_resp.json()["access_token"]
        player_resp = requests.get(
            "https://games.googleapis.com/games/v1/players/me",
            headers={"Authorization": "Bearer " + access}, timeout=12,
        )
        player_resp.raise_for_status()
        player_id = player_resp.json()["playerId"]
    except (requests.RequestException, KeyError, ValueError):
        return failure("games_auth_failed", 401)
    if not isinstance(player_id, str) or not player_id:
        return failure("invalid_player", 401)
    import secrets
    secret = secrets.token_urlsafe(48)
    token_id = hashlib.sha256(secret.encode("utf-8")).hexdigest()
    # Keep sessions short-lived; only the randomized bearer reaches the Android client.
    db.collection("sessions").document(token_id).set({
        "playerId": player_id,
        "expiresAt": int(time.time()) + 3600,
        "createdAt": firestore.SERVER_TIMESTAMP,
    })
    return jsonify({
        "sessionToken": secret,
        "obfuscatedAccountId": hashlib.sha256(player_id.encode("utf-8")).hexdigest(),
        "expiresInSeconds": 3600,
    })


@app.get("/grants")
def list_grants():
    player = session_player()
    if not player:
        return failure("not_authenticated", 401)
    records = db.collection("purchases").where("playerId", "==", player).stream()
    grants = {
        record.id: int(data["coins"])
        for record in records
        if (data := record.to_dict()).get("status") == "consumed"
    }
    return jsonify({"grants": grants})


@firestore.transactional
def reserve(transaction, reference, player, product, coins, account_id):
    snapshot = reference.get(transaction=transaction)
    if snapshot.exists:
        info = snapshot.to_dict()
        if (info.get("playerId") != player or info.get("productId") != product
                or info.get("coins") != coins):
            return "ownership_conflict"
        return info.get("status", "reserved")
    transaction.create(reference, {
        "playerId": player, "productId": product, "coins": coins,
        "accountId": account_id, "status": "reserved",
        "createdAt": firestore.SERVER_TIMESTAMP,
    })
    return "reserved"


@app.post("/claim")
def claim():
    player = session_player()
    if not player:
        return failure("not_authenticated", 401)
    payload = request.get_json(silent=True) or {}
    product, token = payload.get("productId"), payload.get("purchaseToken")
    if product not in PRODUCTS or not isinstance(token, str) or not (8 <= len(token) <= 4096):
        return failure("invalid_purchase", 400)
    digest = hashlib.sha256(token.encode("utf-8")).hexdigest()
    ref = db.collection("purchases").document(digest)
    expected_account = hashlib.sha256(player.encode("utf-8")).hexdigest()
    # Global token uniqueness is enforced even when two different PGS accounts race.
    existing = ref.get()
    if existing.exists:
        info = existing.to_dict()
        if info.get("playerId") != player or info.get("productId") != product:
            return failure("already_claimed_by_other_account", 409)
        if info.get("status") == "consumed":
            return jsonify({"grants": {digest: PRODUCTS[product]}})
    try:
        session = android_publisher()
        url = play_url(product, token)
        check = session.get(url, timeout=12)
        check.raise_for_status()
        purchase = check.json()
    except (requests.RequestException, ValueError):
        return failure("play_verification_unavailable", 503)
    if (purchase.get("purchaseState") != 0 or purchase.get("productId", product) != product
            or purchase.get("obfuscatedExternalAccountId") != expected_account
            or int(purchase.get("quantity", 1)) != 1):
        return failure("purchase_not_verified_or_pending", 409)
    if purchase.get("consumptionState") == 1 and not existing.exists:
        return failure("already_consumed_without_ledger", 409)

    try:
        status = reserve(db.transaction(), ref, player, product, PRODUCTS[product], expected_account)
    except Exception:
        return failure("purchase_ledger_unavailable", 503)
    if status == "ownership_conflict":
        return failure("already_claimed_by_other_account", 409)
    if status != "consumed" and purchase.get("consumptionState") != 1:
        try:
            # Server consumption serves as Play acknowledgement for this consumable.
            consumed = session.post(url + ":consume", timeout=12)
            consumed.raise_for_status()
        except requests.RequestException:
            return failure("consume_pending_retry", 503)
    ref.update({"status": "consumed", "consumedAt": firestore.SERVER_TIMESTAMP})
    return jsonify({"grants": {digest: PRODUCTS[product]}})


@app.get("/health")
def health():
    return jsonify({"ok": True})


if __name__ == "__main__":
    app.run(port=int(os.environ.get("PORT", "8080")), host="0.0.0.0")
