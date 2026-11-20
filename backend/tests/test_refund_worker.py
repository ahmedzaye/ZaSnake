"""Private refund worker authorization and safety checks (offline only)."""
import unittest
from unittest.mock import MagicMock, patch

import refund_worker


class RefundWorkerTests(unittest.TestCase):
    def test_apply_rejected_before_any_google_api_or_firestore_access(self):
        auth = MagicMock()
        db = MagicMock()
        for project, flag, apply in [
            ("za-snake", "true", True),
            ("zasnake", "false", True),
            ("zasnake", "", True),
        ]:
            with self.subTest(project=project, flag=flag, apply=apply):
                with self.assertRaises(ValueError):
                    refund_worker.run(
                        hours=24, apply=apply, project=project, allow_apply=flag,
                        auth_default=auth, firestore_factory=db)
        auth.assert_not_called()
        db.assert_not_called()

    def test_apply_invokes_authoritative_reconciliation_only_with_both_gates(self):
        auth = MagicMock(return_value=("credential", "zasnake"))
        session = MagicMock(return_value="play_session")
        db = MagicMock(return_value="firestore_client")
        with patch.object(refund_worker, "reconcile_window", return_value={
            "dry_run": False, "applied": 1
        }) as reconcile:
            result = refund_worker.run(
                hours=24, apply=True, now_ms=1_800_000_000_000,
                project="zasnake", allow_apply="true",
                auth_default=auth, session_factory=session,
                firestore_factory=db)
        self.assertEqual(result["applied"], 1)
        self.assertEqual(reconcile.call_args.kwargs["dry_run"], False)
        self.assertEqual(db.call_args.kwargs["project"], "zasnake")

    def test_read_only_is_default_even_without_apply_permission(self):
        with patch.object(refund_worker, "reconcile_window", return_value={
            "dry_run": True, "applied": 0
        }) as reconcile:
            refund_worker.run(
                hours=24, apply=False, now_ms=1_800_000_000_000,
                project="zasnake", allow_apply="false",
                auth_default=MagicMock(return_value=("credential", "zasnake")),
                session_factory=MagicMock(return_value="play_session"),
                firestore_factory=MagicMock(return_value="firestore_client"))
        self.assertEqual(reconcile.call_args.kwargs["dry_run"], True)

    def test_rejects_invalid_hours_or_type_before_api_access(self):
        auth = MagicMock()
        for hours in (0, 169, True, 1.5, "24"):
            with self.subTest(hours=hours), self.assertRaises(ValueError):
                refund_worker.run(
                    hours=hours, apply=False, project="zasnake",
                    auth_default=auth)
        auth.assert_not_called()


if __name__ == "__main__":
    unittest.main()
