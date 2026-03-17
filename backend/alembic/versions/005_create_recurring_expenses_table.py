"""create recurring expenses table

Revision ID: 005
Revises: 004
Create Date: 2026-03-17
"""
from alembic import op
import sqlalchemy as sa

revision = "005"
down_revision = "004"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "recurring_expenses",
        sa.Column("id", sa.UUID(), nullable=False, server_default=sa.text("gen_random_uuid()")),
        sa.Column("group_id", sa.UUID(), nullable=False),
        sa.Column("title", sa.String(200), nullable=False),
        sa.Column("amount", sa.Numeric(12, 2), nullable=False),
        sa.Column("category", sa.String(50), nullable=False),
        sa.Column("paid_by_user_id", sa.UUID(), nullable=True),
        sa.Column("paid_by_guest_id", sa.UUID(), nullable=True),
        sa.Column("split_type", sa.String(20), nullable=False, server_default="EQUAL"),
        sa.Column("splits_json", sa.Text(), nullable=False, server_default="[]"),
        sa.Column("frequency", sa.String(20), nullable=False),
        sa.Column("next_due_date", sa.Date(), nullable=False),
        sa.Column("is_active", sa.Boolean(), nullable=False, server_default="true"),
        sa.Column("created_by", sa.UUID(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False,
                  server_default=sa.text("now()")),
        sa.PrimaryKeyConstraint("id"),
        sa.ForeignKeyConstraint(["group_id"], ["groups.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["paid_by_user_id"], ["users.id"]),
        sa.ForeignKeyConstraint(["paid_by_guest_id"], ["guests.id"]),
        sa.ForeignKeyConstraint(["created_by"], ["users.id"]),
    )
    op.create_index("ix_recurring_expenses_group_id", "recurring_expenses", ["group_id"])
    op.create_index("ix_recurring_expenses_next_due_date", "recurring_expenses", ["next_due_date"])


def downgrade() -> None:
    op.drop_table("recurring_expenses")
