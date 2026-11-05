"""Small pure validation helpers for the mobile Billing gateway."""
from urllib.parse import urlparse

ALLOWED_ROUTES = {
    ("POST", "/session"),
    ("POST", "/claim"),
    ("GET", "/grants"),
}
MAX_REQUEST_BYTES = 8192


def private_service_url(value: str) -> str:
    """Accept only a concrete HTTPS Cloud Run service root (never client supplied)."""
    if not isinstance(value, str):
        raise ValueError("private_url_unset")
    parsed = urlparse(value.strip())
    if (parsed.scheme != "https" or not parsed.hostname or
            not parsed.hostname.endswith(".run.app") or
            parsed.username or parsed.password or parsed.port or
            parsed.path not in ("", "/") or parsed.query or parsed.fragment):
        raise ValueError("invalid_private_service_url")
    return f"https://{parsed.hostname}"


def route_allowed(method: str, path: str) -> bool:
    return (method, path) in ALLOWED_ROUTES


def valid_app_id(claims: dict, expected: str) -> bool:
    """Firebase App Check's 'sub' is the registered Firebase application ID."""
    return (isinstance(claims, dict) and isinstance(expected, str) and
            expected.startswith("1:") and claims.get("sub") == expected)
