"""Expensio Telegram bot — long-polling thin client.

Phase 1 handles personal expense logging end to end. Split intent is detected by the
parser but group splitting is the next slice, so for now the bot tells the user it's
coming rather than mislogging it.
"""
import logging

from telegram import InlineKeyboardButton, InlineKeyboardMarkup, ReplyKeyboardMarkup, Update
from telegram.ext import (
    Application,
    CallbackQueryHandler,
    CommandHandler,
    ContextTypes,
    MessageHandler,
    filters,
)

from bot import api_client
from bot.config import config
from bot.parser import parse

logging.basicConfig(
    format="%(asctime)s %(levelname)s %(name)s %(message)s", level=logging.INFO
)
logger = logging.getLogger("expensio-bot")

MENU = ReplyKeyboardMarkup(
    [["💰 Personal", "👥 Groups"]], resize_keyboard=True
)


def _sender(update: Update) -> tuple[int, str, str | None]:
    u = update.effective_user
    return u.id, (u.full_name or "User"), u.username


async def start(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    tid, name, username = _sender(update)
    await api_client.ensure_jwt(tid, name, username)  # register + cache JWT
    await update.message.reply_text(
        f"Hey {name}! I'm Expensio.\n\n"
        "Log an expense by typing it, e.g.:\n"
        "• `Coffee 200`\n"
        "• `Groceries yesterday 540`\n"
        "• `Dinner 900 split flat`\n\n"
        "Use the menu below anytime.",
        reply_markup=MENU,
        parse_mode="Markdown",
    )


async def on_text(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    text = update.message.text
    if text in ("💰 Personal", "👥 Groups"):
        hint = ("Type an expense like `Coffee 200`." if text == "💰 Personal"
                else "Group splitting is coming in the next update.")
        await update.message.reply_text(hint, parse_mode="Markdown")
        return

    parsed = parse(text)

    if parsed.amount is None:
        await update.message.reply_text(
            f"How much was *{parsed.title}*? Send it like `{parsed.title} 200`.",
            parse_mode="Markdown",
        )
        return

    if parsed.kind == "split":
        await update.message.reply_text(
            f"Splitting *{parsed.title}* (₹{parsed.amount}) with _{parsed.target}_ "
            "is coming in the next update — group splitting isn't wired yet.",
            parse_mode="Markdown",
        )
        return

    tid, name, username = _sender(update)
    try:
        expense = await api_client.create_personal_expense(
            tid, name, username,
            parsed.title, parsed.amount, parsed.category, parsed.spent_at,
        )
    except Exception:
        logger.exception("failed to create personal expense")
        await update.message.reply_text("Couldn't save that — try again in a moment.")
        return

    kb = InlineKeyboardMarkup(
        [[InlineKeyboardButton("🗑 Delete", callback_data=f"del:{expense['id']}")]]
    )
    await update.message.reply_text(
        f"✅ Logged: *{expense['title']}* — ₹{expense['amount']}  ·  {expense['category']}",
        reply_markup=kb,
        parse_mode="Markdown",
    )


async def on_delete(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    query = update.callback_query
    expense_id = query.data.split(":", 1)[1]
    tid, name, username = query.from_user.id, query.from_user.full_name, query.from_user.username
    ok = await api_client.delete_personal_expense(tid, name, username, expense_id)
    await query.answer("Deleted" if ok else "Already gone")
    await query.edit_message_text("🗑 Deleted." if ok else "Nothing to delete.")


async def _on_shutdown(app: Application) -> None:
    await api_client.aclose()


def main() -> None:
    app = Application.builder().token(config.bot_token).post_shutdown(_on_shutdown).build()
    app.add_handler(CommandHandler("start", start))
    app.add_handler(CallbackQueryHandler(on_delete, pattern=r"^del:"))
    app.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, on_text))
    logger.info("Expensio bot starting (long-polling)")
    app.run_polling(allowed_updates=Update.ALL_TYPES)


if __name__ == "__main__":
    main()
