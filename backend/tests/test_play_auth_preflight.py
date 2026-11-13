"""Offline tests for silent read-only OAuth preflight."""
import unittest
from unittest.mock import MagicMock

from play_auth_preflight import probe


class PlayAuthPreflightTests(unittest.TestCase):
    def test_adc_failure(self):
        def fail(**kwargs):
            raise RuntimeError("secret=never-log")
        self.assertEqual(probe(auth_default=fail), 21)

    def test_token_refresh_failure_makes_no_google_play_call(self):
        credentials = MagicMock()
        credentials.refresh.side_effect = RuntimeError("credential secret")
        session = MagicMock()
        self.assertEqual(probe(
            auth_default=lambda **_: (credentials, "zasnake"),
            request_factory=MagicMock(),
            session_factory=session), 22)
        session.assert_not_called()

    def test_status_codes_and_bounded_read_only_request(self):
        for status, expected in [(200, 0), (401, 23), (403, 24),
                                 (400, 25), (429, 25), (500, 25)]:
            with self.subTest(status=status):
                credential = MagicMock()
                session = MagicMock()
                session.get.return_value.status_code = status
                result = probe(
                    now_ms=1_800_000_000_000,
                    auth_default=lambda **_: (credential, "zasnake"),
                    request_factory=MagicMock(),
                    session_factory=lambda _: session)
                self.assertEqual(result, expected)
                credential.refresh.assert_called_once()
                args, kwargs = session.get.call_args
                self.assertTrue(args[0].startswith("https://androidpublisher.googleapis.com/"))
                self.assertEqual(kwargs["params"]["pageSelection.maxResults"], "1")
                self.assertEqual(kwargs["params"]["type"], "0")
                self.assertEqual(kwargs["timeout"], 15)
                self.assertFalse(kwargs["allow_redirects"])

    def test_network_failure(self):
        credential = MagicMock()
        session = MagicMock()
        session.get.side_effect = RuntimeError("private purchase token")
        self.assertEqual(probe(
            auth_default=lambda **_: (credential, "zasnake"),
            request_factory=MagicMock(), session_factory=lambda _: session), 26)

    def test_invalid_clock_does_not_call_google_play(self):
        credential = MagicMock()
        session = MagicMock()
        self.assertEqual(probe(
            now_ms=0, auth_default=lambda **_: (credential, "zasnake"),
            request_factory=MagicMock(), session_factory=session), 27)
        session.assert_not_called()


if __name__ == "__main__":
    unittest.main()
