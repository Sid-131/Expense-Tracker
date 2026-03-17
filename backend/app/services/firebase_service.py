"""
Firebase Cloud Messaging helper.
Initialises firebase-admin once using a service-account JSON file.
If the file is missing (e.g. during local dev without Firebase), all
send_* calls are silently skipped so the rest of the app keeps working.
"""
import logging
import os
from pathlib import Path

logger = logging.getLogger(__name__)

_SA_PATH = Path(os.getenv("FIREBASE_SA_PATH", "/app/firebase-service-account.json"))
_app = None


def _get_app():
    global _app
    if _app is not None:
        return _app
    if not _SA_PATH.exists():
        return None
    try:
        import firebase_admin
        from firebase_admin import credentials
        cred = credentials.Certificate(str(_SA_PATH))
        _app = firebase_admin.initialize_app(cred)
        logger.info("Firebase Admin initialised from %s", _SA_PATH)
    except Exception as exc:
        logger.warning("Firebase Admin init failed: %s", exc)
        _app = None
    return _app


async def send_notification(token: str, title: str, body: str, data: dict | None = None) -> None:
    """Send a single FCM notification. Fails silently if Firebase is not configured."""
    if not token:
        return
    app = _get_app()
    if app is None:
        logger.debug("Firebase not configured — skipping push to token %s…", token[:20])
        return
    try:
        from firebase_admin import messaging
        msg = messaging.Message(
            notification=messaging.Notification(title=title, body=body),
            data={k: str(v) for k, v in (data or {}).items()},
            token=token,
        )
        messaging.send(msg)
    except Exception as exc:
        logger.warning("FCM send failed: %s", exc)


async def send_to_users(tokens: list[str], title: str, body: str, data: dict | None = None) -> None:
    """Send the same notification to multiple FCM tokens."""
    for token in tokens:
        await send_notification(token, title, body, data)
