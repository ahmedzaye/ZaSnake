"""Za Snake billing staging server; requires Cloud Run/Firestore/Play API credentials.

NOT production ready until voided-purchase/RTDN reconciliation, abuse limits and
end-to-end tests are implemented. Never put a service account key or OAuth secret
in the Android app, repository, or logs.
"""
import hashlib
import os
import secrets
import time
from urllib.parse import quote

import google.auth
from google.auth.transport.requests import AuthorizedSession
from google.cloud import firestore
from flask import Flask, jsonify, request
import requests

from purchase_validation import validate_product_purchase_v2, PurchaseValidationError

app = Flask(__name__)
db = firestore.Client()
PACKAGE = "com.zayedart.zasnake"
PRODUCTS = {"zasnake_coins_1000": 1000, "zasnake_coins_2000": 2000,
            "zasnake_coins_4000": 4000}
SCOPES = ["https://www.googleapis.com/auth/androidpublisher"]


def failure(code, status):
    return jsonify({"error": code}), status


def play_session():
    credentials, _ = google.auth.default(scopes=SCOPES)
    return AuthorizedSession(credentials)


def product_v2_url(token):
    return ("https://androidpublisher.googleapis.com/androidpublisher/v3/applications/"
            + quote(PACKAGE, safe="") + "/purchases/productsv2/tokens/"
            + quote(token, safe=""))


def consume_url(product, token):
    return ("https://androidpublisher.googleapis.com/androidpublisher/v3/applications/"
            + quote(PACKAGE, safe="") + "/purchases/products/"
            + quote(product, safe="") + "/tokens/" + quote(token, safe="") + ":consume")


def session_player():
    value = request.headers.get("Authorization", "")
    if not value.startswith("Bearer "):
        return None
    bearer = value[len("Bearer "):]
    if len(bearer) < 32 or len(bearer) > 256:
        return None
    digest = hashlib.sha256(bearer.encode("utf-8")).hexdigest()
    snapshot = db.collection("sessions").document(digest).get()
    if not snapshot.exists:
        return None
    info = snapshot.to_dict() or {}
    if info.get("expiresAt", 0) <= time.time():
        return None
    player_id = info.get("playerId")
    return player_id if isinstance(player_id, str) and player_id else None


@app.post("/session")
def create_session():
    payload = request.get_json(silent=True) or {}
    code = payload.get("serverAuthCode", "") if isinstance(payload, dict) else ""
    if not isinstance(code, str) or not (12 <= len(code) <= 4096):
        return failure("invalid_code", 400)
    client_id = os.environ["PGS_WEB_CLIENT_ID"]
    client_secret = os.environ["PGS_WEB_CLIENT_SECRET"]
    try:
        response = requests.post("https://oauth2.googleapis.com/token", data={
            "code": code, "client_id": client_id, "client_secret": client_secret,
            "grant_type": "authorization_code", "redirect_uri": "",
        }, timeout=12)
        response.raise_for_status()
        access_token = response.json()["access_token"]
        player_response = requests.get("https://games.googleapis.com/games/v1/players/me",
            headers={"Authorization": "Bearer " + access_token}, timeout=12)
        player_response.raise_for_status()
        player_id = player_response.json()["playerId"]
    except (requests.RequestException, KeyError, ValueError, TypeError):
        return failure("games_auth_failed", 401)
    if not isinstance(player_id, str) or not player_id:
        return failure("invalid_player", 401)
    random_token = secrets.token_urlsafe(48)
    token_digest = hashlib.sha256(random_token.encode("utf-8")).hexdigest()
    db.collection("sessions").document(token_digest).set({
        "playerId": player_id, "expiresAt": int(time.time()) + 3600,
        "createdAt": firestore.SERVER_TIMESTAMP,
    })
    return jsonify({"sessionToken": random_token,
                    "obfuscatedAccountId": hashlib.sha256(player_id.encode("utf-8")).hexdigest(),
                    "expiresInSeconds": 3600})


@app.get("/grants")
def grants():
    player_id = session_player()
    if not player_id:
        return failure("not_authenticated", 401)
    # Only grants from confirmed, consumed records; no client-submitted coin amounts.
    docs = db.collection("purchases").where("playerId", "==", player_id).stream()
    values = {record.id: int(info["coins"]) for record in docs
              if (info := record.to_dict()).get("status") == "consumed"}
    return jsonify({"grants": values})


@firestore.transactional
def reserve_once(transaction, ref, player_id, product_id, coins, account_id):
    snapshot = ref.get(transaction=transaction)
    if snapshot.exists:
        row = snapshot.to_dict() or {}
        if (row.get("playerId") != player_id or row.get("productId") != product_id
                or row.get("coins") != coins or row.get("accountId") != account_id):
            return "ownership_conflict"
        return row.get("status", "reserved")
    transaction.create(ref, {"playerId": player_id, "productId": product_id,
        "coins": coins, "accountId": account_id, "status": "reserved",
        "createdAt": firestore.SERVER_TIMESTAMP})
    return "reserved"


@app.post("/claim")
def claim():
    player_id = session_player()
    if not player_id:
        return failure("not_authenticated", 401)
    payload = request.get_json(silent=True) or {}
    if not isinstance(payload, dict):
        return failure("invalid_purchase", 400)
    product_id, token = payload.get("productId"), payload.get("purchaseToken")
    if product_id not in PRODUCTS or not isinstance(token, str) or not (8 <= len(token) <= 4096):
        return failure("invalid_purchase", 400)
    digest = hashlib.sha256(token.encode("utf-8")).hexdigest()
    reference = db.collection("purchases").document(digest)
    account_id = hashlib.sha256(player_id.encode("utf-8")).hexdigest()

    # Check account ownership even for previously processed tokens.
    existing = reference.get()
    if existing.exists:
        row = existing.to_dict() or {}
        if (row.get("playerId") != player_id or row.get("productId") != product_id
                or row.get("accountId") != account_id):
            return failure("ownership_conflict", 409)
        if row.get("status") == "consumed":
            return jsonify({"grants": {digest: PRODUCTS[product_id]}})
        if row.get("status") != "reserved":
            return failure("not_entitled", 409)

    try:
        session = play_session()
        resp = session.get(product_v2_url(token), timeout=12)
        resp.raise_for_status()
        receipt = resp.json()
    except (requests.RequestException, ValueError, TypeError):
        return failure("play_verification_unavailable", 503)
    try:
        consumption_state = validate_product_purchase_v2(receipt, product_id, account_id)
    except PurchaseValidationError:
        return failure("purchase_not_verified_or_pending", 409)

    # Never create a new entitlement for a token that was consumed by someone else.
    if consumption_state == "CONSUMPTION_STATE_CONSUMED" and not existing.exists:
        return failure("unrecognized_consumed_token", 409)

    try:
        status = reserve_once(db.transaction(), reference, player_id,
                              product_id, PRODUCTS[product_id], account_id)
    except Exception:
        return failure("purchase_ledger_unavailable", 503)
    if status == "ownership_conflict":
        return failure("ownership_conflict", 409)
    if status not in ("reserved", "consumed"):
        return failure("not_entitled", 409)
    if status == "reserved" and consumption_state == "CONSUMPTION_STATE_YET_TO_BE_CONSUMED":
        try:
            response = session.post(consume_url(product_id, token), timeout=12)
            response.raise_for_status()
        except requests.RequestException:
            # If Google completed consumption but response was lost, retry sees
            # a consumed token and recovers the existing Firestore reservation.
            return failure("consume_pending_retry", 503)
    try:
        reference.update({"status": "consumed", "consumedAt": firestore.SERVER_TIMESTAMP})
    except Exception:
        return failure("purchase_ledger_unavailable", 503)
    return jsonify({"grants": {digest: PRODUCTS[product_id]}})


@app.get("/health")
def health():
    return jsonify({"ok": True, "stage": "staging"})


if __name__ == "__main__":
    app.run(port=int(os.environ.get("PORT", "8080")), host="0.0.0.0")
