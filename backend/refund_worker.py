"""Controlled Google Play voided-purchase reconciliation for a private Cloud Run Job.

Default mode is DRY RUN. Writes require BOTH --apply and the private Job's
ALLOW_REFUND_APPLY=true environment setting. Never expose as an HTTP endpoint
or put this entrypoint in the mobile-facing gateway.
The caller is responsible for scheduling overlapping windows, monitoring
failures and granting Play Developer API + Firestore permissions.
"""
import argparse
import json
import os
import sys
import time

import google.auth
from google.auth.transport.requests import AuthorizedSession
from google.cloud import firestore

from reconcile_dry_run import safe_failure_category
from voided_reconciliation import reconcile_window


PROJECT = "zasnake"
SCOPES = ["https://www.googleapis.com/auth/androidpublisher"]


def run(*, hours: int, apply: bool, now_ms: int | None = None,
        project: str | None = None, allow_apply: str | None = None,
        auth_default=google.auth.default,
        session_factory=AuthorizedSession,
        firestore_factory=firestore.Client):
    if type(hours) is not int or not 1 <= hours <= 168:
        raise ValueError("hours_out_of_range")
    if type(apply) is not bool:
        raise ValueError("invalid_apply")
    effective_project = project if project is not None else (
        os.environ.get("GOOGLE_CLOUD_PROJECT") or os.environ.get("GCLOUD_PROJECT")
    )
    if effective_project != PROJECT:
        raise ValueError("wrong_project")
    permission = allow_apply if allow_apply is not None else os.environ.get(
        "ALLOW_REFUND_APPLY", "false")
    if apply and permission != "true":
        raise ValueError("refund_apply_not_authorized")
    now = int(time.time() * 1000) if now_ms is None else now_ms
    if type(now) is not int or now <= hours * 3_600_000:
        raise ValueError("invalid_time")

    # All validation happens before credentials or database access.
    credentials, _ = auth_default(scopes=SCOPES)
    play = session_factory(credentials)
    db = firestore_factory(project=PROJECT)
    return reconcile_window(
        db, play, now - hours * 3_600_000, now, now,
        dry_run=not apply,
    )


def main(argv=None):
    parser = argparse.ArgumentParser(
        description="Private refund reconciler: safe dry-run by default")
    parser.add_argument("--hours", type=int, default=24)
    parser.add_argument("--apply", action="store_true")
    options = parser.parse_args(argv)
    try:
        result = run(hours=options.hours, apply=options.apply)
    except Exception as exc:
        print("REFUND_WORKER_FAILED category=" + safe_failure_category(exc),
              file=sys.stderr)
        return 1
    # Never log purchase tokens, order IDs, session tokens, or player IDs.
    print(json.dumps({
        "mode": "APPLY" if options.apply else "DRY_RUN",
        "windowHours": options.hours,
        **result,
    }, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
