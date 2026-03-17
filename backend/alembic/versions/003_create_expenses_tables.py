"""create expenses tables

Revision ID: 003
Revises: 002
Create Date: 2026-03-17
"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

revision = '003'
down_revision = '002'
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        'expenses',
        sa.Column('id', postgresql.UUID(as_uuid=True), nullable=False,
                  server_default=sa.text('gen_random_uuid()')),
        sa.Column('group_id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('title', sa.String(200), nullable=False),
        sa.Column('amount', sa.Numeric(10, 2), nullable=False),
        sa.Column('category', sa.String(50), nullable=False, server_default='OTHER'),
        sa.Column('paid_by_user_id', postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column('paid_by_guest_id', postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column('split_type', sa.String(20), nullable=False, server_default='EQUAL'),
        sa.Column('created_by', postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True),
                  server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True),
                  server_default=sa.text('now()'), nullable=False),
        sa.ForeignKeyConstraint(['group_id'], ['groups.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['paid_by_user_id'], ['users.id'], ondelete='SET NULL'),
        sa.ForeignKeyConstraint(['paid_by_guest_id'], ['guests.id'], ondelete='SET NULL'),
        sa.ForeignKeyConstraint(['created_by'], ['users.id'], ondelete='SET NULL'),
        sa.PrimaryKeyConstraint('id'),
    )
    op.create_index('ix_expenses_group_id', 'expenses', ['group_id'])

    op.create_table(
        'expense_splits',
        sa.Column('id', postgresql.UUID(as_uuid=True), nullable=False,
                  server_default=sa.text('gen_random_uuid()')),
        sa.Column('expense_id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('user_id', postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column('guest_id', postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column('amount', sa.Numeric(10, 2), nullable=False),
        sa.Column('percentage', sa.Numeric(5, 2), nullable=True),
        sa.ForeignKeyConstraint(['expense_id'], ['expenses.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['user_id'], ['users.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['guest_id'], ['guests.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id'),
    )
    op.create_index('ix_expense_splits_expense_id', 'expense_splits', ['expense_id'])

    op.create_table(
        'balances',
        sa.Column('id', postgresql.UUID(as_uuid=True), nullable=False,
                  server_default=sa.text('gen_random_uuid()')),
        sa.Column('group_id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('user_id', postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column('guest_id', postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column('net_amount', sa.Numeric(10, 2), nullable=False,
                  server_default=sa.text('0')),
        sa.ForeignKeyConstraint(['group_id'], ['groups.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['user_id'], ['users.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['guest_id'], ['guests.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id'),
    )
    op.create_index('ix_balances_group_id', 'balances', ['group_id'])
    op.create_index('uq_balance_user', 'balances', ['group_id', 'user_id'], unique=True,
                    postgresql_where=sa.text('user_id IS NOT NULL'))
    op.create_index('uq_balance_guest', 'balances', ['group_id', 'guest_id'], unique=True,
                    postgresql_where=sa.text('guest_id IS NOT NULL'))


def downgrade() -> None:
    op.drop_table('balances')
    op.drop_table('expense_splits')
    op.drop_table('expenses')
