"""RuleParser — the stable parse interface. Phase 2 swaps in an LLMParser behind the
same `parse(text) -> ParsedExpense` signature; nothing downstream changes.

    parse("Coffee 200")            -> personal, title=Coffee, amount=200, category=FOOD
    parse("Dinner 900 split flat") -> split, target="flat"
    parse("Cab 300 with Rahul Aman") -> split, target="Rahul Aman"
"""
from __future__ import annotations

import re
from dataclasses import dataclass
from datetime import date, timedelta
from decimal import Decimal, InvalidOperation
from typing import Optional

# Canonical list mirrors backend app/core/categories.py. Kept as a local copy because
# the bot is a separate container and does not import the backend package.
CATEGORIES = [
    "FOOD", "GROCERIES", "TRANSPORT", "SHOPPING", "BILLS",
    "ENTERTAINMENT", "HEALTH", "TRAVEL", "OTHER",
]

# keyword substring -> category. First hit wins.
_KEYWORDS = {
    "FOOD": ["coffee", "lunch", "dinner", "breakfast", "food", "snack", "pizza",
             "restaurant", "cafe", "tea", "meal", "swiggy", "zomato"],
    "GROCERIES": ["grocery", "groceries", "vegetable", "milk", "supermarket", "bigbasket"],
    "TRANSPORT": ["uber", "ola", "taxi", "cab", "fuel", "petrol", "metro", "bus",
                  "train", "auto", "rickshaw"],
    "SHOPPING": ["amazon", "flipkart", "clothes", "shopping", "shoes", "myntra"],
    "BILLS": ["bill", "electricity", "rent", "wifi", "internet", "recharge",
              "subscription", "water"],
    "ENTERTAINMENT": ["movie", "netflix", "game", "concert", "spotify", "cinema"],
    "HEALTH": ["medicine", "doctor", "pharmacy", "gym", "hospital", "clinic"],
    "TRAVEL": ["flight", "hotel", "trip", "travel", "airbnb", "booking"],
}

_SPLIT_WORDS = ("split", "with")
_AMOUNT_RE = re.compile(r"\d+(?:\.\d{1,2})?")


@dataclass
class ParsedExpense:
    kind: str                      # "personal" | "split"
    title: str
    amount: Optional[Decimal]      # None if no number found -> caller asks a follow-up
    category: str
    target: Optional[str] = None   # group name or people, only for kind="split"
    spent_at: Optional[date] = None


def _category_for(text: str) -> str:
    low = text.lower()
    for cat, words in _KEYWORDS.items():
        if any(w in low for w in words):
            return cat
    return "OTHER"


def _date_for(text: str) -> date:
    low = text.lower()
    if "yesterday" in low:
        return date.today() - timedelta(days=1)
    return date.today()


def parse(text: str) -> ParsedExpense:
    raw = text.strip()
    low = raw.lower()

    # amount: take the last number so "2 coffees 200" -> 200 not 2. ponytail: last-number
    # heuristic, upgrade to LLMParser when it misreads real messages.
    matches = list(_AMOUNT_RE.finditer(raw))
    amount: Optional[Decimal] = None
    if matches:
        try:
            amount = Decimal(matches[-1].group())
        except InvalidOperation:
            amount = None

    # split intent + target
    kind = "personal"
    target: Optional[str] = None
    split_pos = None
    for w in _SPLIT_WORDS:
        idx = low.find(w)
        if idx != -1:
            split_pos = idx if split_pos is None else min(split_pos, idx)
            kind = "split"
            after = raw[idx + len(w):].strip()
            # drop a trailing amount that lands after the split word
            after = _AMOUNT_RE.sub("", after).strip()
            if after:
                target = after
            break

    # title: text before the split word, minus the amount and date words
    head = raw[:split_pos] if split_pos is not None else raw
    if matches and split_pos is None:
        head = head[: matches[-1].start()] + head[matches[-1].end():]
    else:
        head = _AMOUNT_RE.sub("", head)
    for w in ("yesterday", "today"):
        head = re.sub(w, "", head, flags=re.IGNORECASE)
    title = " ".join(head.split()).strip(" -:") or "Expense"

    return ParsedExpense(
        kind=kind,
        title=title.capitalize(),
        amount=amount,
        category=_category_for(raw),
        target=target,
        spent_at=_date_for(raw),
    )


def _selfcheck() -> None:
    p = parse("Coffee 200")
    assert p.kind == "personal" and p.amount == Decimal("200") and p.category == "FOOD", p
    assert p.title == "Coffee", p

    p = parse("Dinner 900 split flat")
    assert p.kind == "split" and p.amount == Decimal("900") and p.target == "flat", p
    assert p.title == "Dinner", p

    p = parse("Cab 300 with Rahul Aman")
    assert p.kind == "split" and p.target == "Rahul Aman" and p.category == "TRANSPORT", p

    p = parse("Groceries yesterday 540.50")
    assert p.amount == Decimal("540.50") and p.category == "GROCERIES", p
    assert p.spent_at == date.today() - timedelta(days=1), p
    assert p.title == "Groceries", p

    p = parse("random note no number")
    assert p.amount is None and p.kind == "personal", p

    print("parser selfcheck: OK")


if __name__ == "__main__":
    _selfcheck()
