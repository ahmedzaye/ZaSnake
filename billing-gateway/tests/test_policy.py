import unittest

from policy import private_service_url, route_allowed, valid_app_id


class PolicyTests(unittest.TestCase):
    def test_valid_private_service(self):
        self.assertEqual(
            private_service_url("https://zasnake-billing-staging-914975720790.europe-west1.run.app"),
            "https://zasnake-billing-staging-914975720790.europe-west1.run.app",
        )

    def test_reject_unsafe_urls(self):
        values = [
            "http://example.run.app", "https://run.app.evil.test",
            "https://example.run.app/private", "https://example.run.app?foo=1",
            "https://example.run.app#fragment", "https://u:p@example.run.app",
            "https://example.run.app:8443", "file:///etc/passwd",
            "https://example.com", "",
        ]
        for value in values:
            with self.subTest(value=value), self.assertRaises(ValueError):
                private_service_url(value)

    def test_allowed_routes(self):
        for method, path in (("POST", "/session"), ("POST", "/claim"), ("GET", "/grants")):
            self.assertTrue(route_allowed(method, path))
        for method, path in (("GET", "/session"), ("GET", "/claim"),
                             ("POST", "/grants"), ("GET", "/admin"),
                             ("POST", "/claim/extra"), ("DELETE", "/session")):
            self.assertFalse(route_allowed(method, path))

    def test_app_identity(self):
        appid = "1:914975720790:android:abcd1234"
        self.assertTrue(valid_app_id({"sub": appid}, appid))
        self.assertFalse(valid_app_id({"sub": "1:686286211310:android:wrong"}, appid))
        self.assertFalse(valid_app_id({}, appid))
        self.assertFalse(valid_app_id({"sub": appid}, ""))
        self.assertFalse(valid_app_id({"sub": appid}, "wrong-id"))


if __name__ == "__main__":
    unittest.main()
