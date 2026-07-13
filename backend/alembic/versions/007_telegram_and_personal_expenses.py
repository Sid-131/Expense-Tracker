"""add telegram columns to users and create personal_expenses table

Revision ID: 007
Revises: 006
Create Date: 2026-07-13
"""
from alembic import op
import sqlalchemy as sa

revision = "007"
down_revision = "006"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.add_column("users", sa.Column("telegram_id", sa.BigInteger(), nullable=True))
    op.add_column("users", sa.Column("telegram_username", sa.String(64), nullable=True))
    op.create_unique_constraint("uq_users_telegram_id", "users", ["telegram_id"])

    op.create_table(
        "personal_expenses",
        sa.Column("id", sa.UUID(), nullable=False, server_default=sa.text("gen_random_uuid()")),
        sa.Column("user_id", sa.UUID(), nullable=False),
        sa.Column("title", sa.String(200), nullable=False),
        sa.Column("amount", sa.Numeric(10, 2), nullable=False),
        sa.Column("category", sa.String(50), nullable=False, server_default="OTHER"),
        sa.Column("spent_at", sa.Date(), nullable=False, server_default=sa.text("CURRENT_DATE")),
        sa.Column("note", sa.String(500), nullable=True),
        sa.Column("source", sa.String(20), nullable=False, server_default="telegram"),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False,
                  server_default=sa.text("now()")),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False,
                  server_default=sa.text("now()")),
        sa.PrimaryKeyConstraint("id"),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
    )
    op.create_index("ix_personal_expenses_user_id", "personal_expenses", ["user_id"])
    op.create_index("ix_personal_expenses_spent_at", "personal_expenses", ["spent_at"])


def downgrade() -> None:
    op.drop_table("personal_expenses")
    op.drop_constraint("uq_users_telegram_id", "users", type_="unique")
    op.drop_column("users", "telegram_username")
    op.drop_column("users", "telegram_id")
