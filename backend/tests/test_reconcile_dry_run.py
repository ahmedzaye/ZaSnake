"""Pure offline checks for explicit read-only reconciliation CLI."""
import io
import unittest
from contextlib import redirect_stdout, redirect_stderr
from unittest.mock import MagicMock, patch

from reconcile_dry_run import main, perform_read_only_review


class ReadOnlyRunnerTests(unittest.TestCase):
    def test_no_run_flag_has_no_side_effects(self):
        with (patch("reconcile_dry_run.perform_read_only_review") as review,
              redirect_stderr(io.StringIO()),
              self.assertRaises(SystemExit) as failure):
            main(["--hours", "12"])
        self.assertEqual(failure.exception.code, 2)
        review.assert_not_called()

    def test_run_invokes_dry_run_only(self):
        fake_auth = MagicMock(return_value=("credentials", "project"))
        fake_client = MagicMock(return_value="firestore-read")
        fake_session = MagicMock(return_value="play-read")
        with patch("reconcile_dry_run.reconcile_window",
                   return_value={"matched_purchases": 0, "applied": 0,
                                 "dry_run": True}) as reconcile:
            result = perform_read_only_review(
                hours=24, now_ms=1_800_000_000_000,
                auth_default=fake_auth, client_factory=fake_client,
                session_factory=fake_session,
            )
        self.assertEqual(result["dry_run"], True)
        fake_auth.assert_called_once_with(scopes=["https://www.googleapis.com/auth/androidpublisher"])
        fake_client.assert_called_once_with(project="zasnake")
        reconcile.assert_called_once()
        self.assertEqual(reconcile.call_args.kwargs, {"dry_run": True})

    def test_invalid_window_does_not_fetch(self):
        for hours in (0, -1, 169, True, "24"):
            with self.subTest(hours=hours):
                with patch("reconcile_dry_run.reconcile_window") as reconcile:
                    with self.assertRaises(ValueError):
                        perform_read_only_review(hours=hours)
                    reconcile.assert_not_called()

    def test_cli_prints_only_non_sensitive_counters(self):
        counts = {"matched_purchases": 2, "unknown_purchases": 1,
                  "verified_full_refunds": 3, "applied": 0,
                  "dry_run": True}
        with (patch("reconcile_dry_run.perform_read_only_review", return_value=counts),
              redirect_stdout(io.StringIO()) as output):
            result = main(["--run", "--hours", "24"])
        self.assertEqual(result, 0)
        self.assertIn('"mode": "DRY_RUN"', output.getvalue())
        self.assertIn('"applied": 0', output.getvalue())

    def test_cli_failure_does_not_log_credentials(self):
        with (patch("reconcile_dry_run.perform_read_only_review",
                    side_effect=RuntimeError("Secret token=bad")),
              redirect_stderr(io.StringIO()) as output):
            result = main(["--run"])
        self.assertEqual(result, 1)
        self.assertNotIn("Secret", output.getvalue())
        self.assertIn("READ_ONLY_REVIEW_FAILED", output.getvalue())


if __name__ == "__main__":
    unittest.main()
