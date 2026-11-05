"""Firebase App Check verified mobile facade for a *private* Cloud Run Billing API.

The gateway has NO Firestore permissions, NO Play Developer API permissions, and
NO OAuth client secret. Google IAM is used exclusively gateway -> private API.

NEVER enable this gateway before Firebase App Check Play Integrity is configured.
"""
import os
import logging

import firebase_admin
from firebase_admin import app_check
from flask import Flask, jsonify, request
from google.auth.transport.requests import Request as GoogleRequest
from google.oauth2 import id_token
import requests

from policy import (
    ALLOWED_ROUTES, MAX_REQUEST_BYTES, private_service_url,
    route_allowed, valid_app_id,
)

app = Flask(__name__)
app.config["MAX_CONTENT_LENGTH"] = MAX_REQUEST_BYTES
firebase_admin.initialize_app(options={"projectId": os.environ.get("GOOGLE_CLOUD_PROJECT", "zasnake")})
log = logging.getLogger(__name__)

# Reject all requests until the operator intentionally configures this service.
ENABLED = os.environ.get("GATEWAY_ENABLED", "false").lower() == "true"
EXPECTED_APP_ID = os.environ.get("EXPECTED_FIREBASE_APP_ID", "")
PRIVATE_URL = os.environ.get("BILLING_PRIVATE_URL", "")


@app.get("/health")
def health():
    # Reports service liveness only, NOT whether billing or the wallet is ready.
    return jsonify({"ok": True, "gatewayEnabled": ENABLED})


@app.route("/<path:route>", methods=["GET", "POST"])
def forward(route):
    path = "/" + route
    if not ENABLED:
        return jsonify({"error": "gateway_disabled"}), 503
    if not route_allowed(request.method, path):
        return jsonify({"error": "not_found"}), 404
    if not EXPECTED_APP_ID.startswith("1:"):
        return jsonify({"error": "gateway_unconfigured"}), 503
    app_check_token = request.headers.get("X-Firebase-AppCheck", "")
    if not (32 <= len(app_check_token) <= 8192):
        return jsonify({"error": "app_attestation_required"}), 401
    try:
        claims = app_check.verify_token(app_check_token)
    except Exception:
        # Never expose validation internals, tokens, or Google credentials.
        return jsonify({"error": "app_attestation_rejected"}), 401
    if not valid_app_id(claims, EXPECTED_APP_ID):
        return jsonify({"error": "app_identity_mismatch"}), 401

    bearer = request.headers.get("Authorization", "")
    if path != "/session" and (not bearer.startswith("Bearer ") or len(bearer) > 320):
        return jsonify({"error": "session_required"}), 401
    if path == "/session" and bearer:
        return jsonify({"error": "unexpected_authorization"}), 400
    if request.method == "POST" and not request.is_json:
        return jsonify({"error": "json_required"}), 415
    body = request.get_data(cache=False) if request.method == "POST" else None
    if body is not None and len(body) > MAX_REQUEST_BYTES:
        return jsonify({"error": "body_too_large"}), 413

    try:
        base = private_service_url(PRIVATE_URL)
        # IAM token's audience must be the *private service's root URL*.
        cloud_token = id_token.fetch_id_token(GoogleRequest(), base)
        headers = {
            "X-Serverless-Authorization": "Bearer " + cloud_token,
            "Accept": "application/json",
        }
        if bearer:
            # Forward app session token; IAM uses X-Serverless-Authorization.
            headers["Authorization"] = bearer
        if body is not None:
            headers["Content-Type"] = "application/json"
        upstream = requests.request(request.method, base + path,
                                    headers=headers, data=body,
                                    timeout=(4, 15), allow_redirects=False)
        if (upstream.status_code not in range(200, 300) and
                upstream.status_code not in (400, 401, 403, 404, 409, 429, 503)):
            return jsonify({"error": "billing_unavailable"}), 503
        if len(upstream.content) > 65536 or "application/json" not in upstream.headers.get("Content-Type", ""):
            return jsonify({"error": "billing_unavailable"}), 503
        # Never reflect arbitrary headers, redirects, cookies, or credentials.
        return app.response_class(upstream.content, status=upstream.status_code,
                                  mimetype="application/json",
                                  headers={"Cache-Control": "no-store"})
    except (requests.RequestException, ValueError):
        return jsonify({"error": "billing_unavailable"}), 503
    except Exception:
        log.exception("Billing gateway internal error (no request data logged)")
        return jsonify({"error": "billing_unavailable"}), 503


@app.errorhandler(413)
def payload_too_large(_):
    return jsonify({"error": "body_too_large"}), 413
