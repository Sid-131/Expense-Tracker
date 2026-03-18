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


def upgrade():
    # expenses: sort by created_at DESC frequently
    op.create_index('ix_expenses_created_at', 'expenses', ['created_at'])
    # expenses: analytics queries filter by paid_by_user_id
    op.create_index('ix_expenses_paid_by_user_id', 'expenses', ['paid_by_user_id'])
    # expense_splits: analytics queries filter by user_id
    op.create_index('ix_expense_splits_user_id', 'expense_splits', ['user_id'])
    # users: login lookup by email
    op.create_index('ix_users_email', 'users', ['email'])
    # settlements: filter by status
    op.create_index('ix_settlements_status', 'settlements', ['status'])
    # recurring_expenses: scheduler queries next_due_date + is_active
    op.create_index('ix_recurring_expenses_next_due_date', 'recurring_expenses', ['next_due_date'])
    op.create_index('ix_recurring_expenses_is_active', 'recurring_expenses', ['is_active'])


def downgrade():
    op.drop_index('ix_expenses_created_at', 'expenses')
    op.drop_index('ix_expenses_paid_by_user_id', 'expenses')
    op.drop_index('ix_expense_splits_user_id', 'expense_splits')
    op.drop_index('ix_users_email', 'users')
    op.drop_index('ix_settlements_status', 'settlements')
    op.drop_index('ix_recurring_expenses_next_due_date', 'recurring_expenses')
    op.drop_index('ix_recurring_expenses_is_active', 'recurring_expenses')
