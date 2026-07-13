"""Canonical expense categories, shared by parser + analytics + both buckets."""

CATEGORIES = [
    "FOOD",
    "GROCERIES",
    "TRANSPORT",
    "SHOPPING",
    "BILLS",
    "ENTERTAINMENT",
    "HEALTH",
    "TRAVEL",
    "OTHER",
]

DEFAULT_CATEGORY = "OTHER"


def normalize(category: str | None) -> str:
    """Map free text to a canonical category, falling back to OTHER."""
    if not category:
        return DEFAULT_CATEGORY
    upper = category.strip().upper()
    return upper if upper in CATEGORIES else DEFAULT_CATEGORY
