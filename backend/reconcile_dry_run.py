"""Manually invoked, READ-ONLY Google Play voided-purchase review.

No HTTP endpoint, timer, scheduler, deployment, mutation or purchase activation.
No secrets/tokens/players/orders in output. Must be invoked with --run explicitly.
Uses the existing Google Play Android Publisher service account credentials.

Example (from backend/, service-account ADC in private Cloud Run context):
    python reconcile_dry_run.py --run --hours 24
"""
import argparse
import json
import sys
import time

import google.auth
from google.auth.transport.requests import AuthorizedSession
from google.cloud import firestore

from voided_reconciliation import reconcile_window, MILLIS_PER_DAY


EXPECTED_PROJECT = "zasnake"
SCOPES = ["https://www.googleapis.com/auth/androidpublisher"]


def perform_read_only_review(*, hours: int, now_ms: int | None = None,
                             client_factory=firestore.Client,
                             auth_default=google.auth.default,
                             session_factory=AuthorizedSession):
    if type(hours) is not int or not 1 <= hours <= 24 * 7:
        raise ValueError("hours_out_of_range")
    now = int(time.time() * 1000) if now_ms is None else now_ms
    if type(now) is not int or now <= hours * 60 * 60 * 1000:
        raise ValueError("invalid_time")
    credentials, _ = auth_default(scopes=SCOPES)
    play = session_factory(credentials)
    db = client_factory(project=EXPECTED_PROJECT)
    # The underlying method deliberately defaults to dry_run=True; be
    # explicit so this script never writes wallet or purchases collections.
    return reconcile_window(db, play, now - hours * 60 * 60 * 1000, now, now,
                            dry_run=True)


def main(argv=None):
    parser = argparse.ArgumentParser(
        description="READ ONLY: review recent Google Play refunded purchases"
    )
    parser.add_argument("--run", action="store_true",
                        help="explicitly authorize Google Play and Firestore read-only requests")
    parser.add_argument("--hours", type=int, default=24,
                        help="lookback hours (1 to 168; default 24)")
    opts = parser.parse_args(argv)
    if not opts.run:
        parser.error("safe default: add --run to explicitly make read-only API requests")
    if not 1 <= opts.hours <= 168:
        parser.error("--hours must be between 1 and 168")
    try:
        counts = perform_read_only_review(hours=opts.hours)
    except Exception:
        # Never print sensitive API payloads or exception messages that might
        # contain purchase tokens/order IDs. Google Cloud logs should use
        # sanitized operational counters only.
        print("READ_ONLY_REVIEW_FAILED (see protected service logs)", file=sys.stderr)
        return 1
    print(json.dumps({"mode": "DRY_RUN", "windowHours": opts.hours, **counts},
                     sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
