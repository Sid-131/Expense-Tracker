"""Add performance indexes

Revision ID: 006
Revises: 005
Create Date: 2026-03-18
"""
from alembic import op

revision = '006'
down_revision = '005'
branch_labels = None
depends_on = None


def _create_if_not_exists(name, table, columns, **kw):
    from sqlalchemy import text
    conn = op.get_bind()
    exists = conn.execute(
        text("SELECT 1 FROM pg_indexes WHERE indexname = :n"), {"n": name}
    ).scalar()
    if not exists:
        op.create_index(name, table, columns, **kw)


def upgrade():
    _create_if_not_exists('ix_expenses_created_at', 'expenses', ['created_at'])
    _create_if_not_exists('ix_expenses_paid_by_user_id', 'expenses', ['paid_by_user_id'])
    _create_if_not_exists('ix_expense_splits_user_id', 'expense_splits', ['user_id'])
    _create_if_not_exists('ix_users_email', 'users', ['email'])
    _create_if_not_exists('ix_settlements_status', 'settlements', ['status'])
    _create_if_not_exists('ix_recurring_expenses_next_due_date', 'recurring_expenses', ['next_due_date'])
    _create_if_not_exists('ix_recurring_expenses_is_active', 'recurring_expenses', ['is_active'])


def downgrade():
    op.drop_index('ix_expenses_created_at', 'expenses')
    op.drop_index('ix_expenses_paid_by_user_id', 'expenses')
    op.drop_index('ix_expense_splits_user_id', 'expense_splits')
    op.drop_index('ix_users_email', 'users')
    op.drop_index('ix_settlements_status', 'settlements')
    op.drop_index('ix_recurring_expenses_next_due_date', 'recurring_expenses')
    op.drop_index('ix_recurring_expenses_is_active', 'recurring_expenses')
