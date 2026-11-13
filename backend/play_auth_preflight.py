"""Manual, read-only Google Play auth probe. No Firestore and no output.

Cloud Run records the process exit code even when stdout/stderr are unavailable.
Codes: 0=HTTP200, 21=ADC acquisition failed, 22=OAuth token refresh failed,
23=Play HTTP401, 24=Play HTTP403, 25=other Play HTTP response,
26=network/request failed, 27=unexpected internal error.

Does not display response bodies, credentials, tokens, player IDs or orders.
"""
import sys
import time

import google.auth
from google.auth.transport.requests import AuthorizedSession, Request

from voided_reconciliation import PLAY_URL

SCOPES = ["https://www.googleapis.com/auth/androidpublisher"]


def probe(*, now_ms=None, auth_default=google.auth.default,
          request_factory=Request, session_factory=AuthorizedSession):
    try:
        credentials, _ = auth_default(scopes=SCOPES)
    except Exception:
        return 21
    try:
        credentials.refresh(request_factory())
    except Exception:
        return 22
    now = int(time.time() * 1000) if now_ms is None else now_ms
    if type(now) is not int or now < 3600000:
        return 27
    params = {
        "type": "0",
        "includeQuantityBasedPartialRefund": "true",
        "pageSelection.maxResults": "1",
        "startTime": str(now - 3600000),
        "endTime": str(now),
    }
    try:
        response = session_factory(credentials).get(
            PLAY_URL, params=params, timeout=15, allow_redirects=False
        )
        code = response.status_code
        if code == 200:
            return 0
        if code == 401:
            return 23
        if code == 403:
            return 24
        return 25
    except Exception:
        return 26


if __name__ == "__main__":
    # stdout/stderr intentionally unused; inspect task exit code in Cloud Run.
    sys.exit(probe())
