CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    full_name VARCHAR(160) NOT NULL,
    email VARCHAR(254) NOT NULL,
    username VARCHAR(40) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    pin_hash VARCHAR(100),
    pin_failed_attempts INTEGER NOT NULL DEFAULT 0 CHECK (pin_failed_attempts BETWEEN 0 AND 5),
    pin_blocked_until TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'BLOCKED', 'CLOSED')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_user_email UNIQUE (email),
    CONSTRAINT uk_user_username UNIQUE (username)
);

CREATE TABLE user_role (
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    role VARCHAR(12) NOT NULL CHECK (role IN ('USER', 'ADMIN', 'SYSTEM')),
    PRIMARY KEY (user_id, role)
);

CREATE TABLE account (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES app_user(id),
    agency VARCHAR(8) NOT NULL,
    account_number VARCHAR(16) NOT NULL,
    account_digit VARCHAR(2) NOT NULL,
    balance NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK (balance >= 0),
    daily_limit NUMERIC(19,2) NOT NULL CHECK (daily_limit >= 0),
    transferred_today NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK (transferred_today >= 0),
    limit_reference_date DATE NOT NULL,
    last_sandbox_funding_at TIMESTAMPTZ,
    status VARCHAR(24) NOT NULL CHECK (status IN ('ACTIVE', 'TEMPORARILY_BLOCKED', 'CLOSED', 'SYSTEM')),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_account_number UNIQUE (agency, account_number, account_digit)
);

CREATE TABLE pix_key (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES account(id),
    type VARCHAR(12) NOT NULL CHECK (type IN ('EMAIL', 'PHONE', 'USERNAME', 'RANDOM')),
    display_value VARCHAR(254) NOT NULL,
    normalized_value VARCHAR(254) NOT NULL,
    status VARCHAR(12) NOT NULL CHECK (status IN ('ACTIVE', 'DELETED')),
    created_at TIMESTAMPTZ NOT NULL
);
CREATE UNIQUE INDEX uk_pix_key_active ON pix_key(normalized_value) WHERE status = 'ACTIVE';
CREATE INDEX ix_pix_key_account ON pix_key(account_id, status);

CREATE TABLE bank_transfer (
    id UUID PRIMARY KEY,
    public_id VARCHAR(32) NOT NULL UNIQUE,
    end_to_end_id VARCHAR(64) NOT NULL UNIQUE,
    source_account_id UUID NOT NULL REFERENCES account(id),
    destination_account_id UUID NOT NULL REFERENCES account(id),
    amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    description VARCHAR(140),
    status VARCHAR(12) NOT NULL CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'REVERSED')),
    idempotency_key VARCHAR(100) NOT NULL,
    key_used VARCHAR(254) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    failure_reason VARCHAR(255),
    CONSTRAINT ck_transfer_accounts CHECK (source_account_id <> destination_account_id),
    CONSTRAINT uk_transfer_idempotency UNIQUE (source_account_id, idempotency_key)
);
CREATE INDEX ix_transfer_source_created ON bank_transfer(source_account_id, created_at DESC);
CREATE INDEX ix_transfer_destination_created ON bank_transfer(destination_account_id, created_at DESC);
CREATE INDEX ix_transfer_status_created ON bank_transfer(status, created_at DESC);

CREATE TABLE ledger_entry (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES account(id),
    transfer_id UUID REFERENCES bank_transfer(id),
    type VARCHAR(8) NOT NULL CHECK (type IN ('CREDIT', 'DEBIT')),
    category VARCHAR(24) NOT NULL CHECK (category IN ('OPENING_BALANCE', 'PIX_TRANSFER', 'SANDBOX_FUNDING', 'ADMIN_ADJUSTMENT', 'REVERSAL')),
    amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    resulting_balance NUMERIC(19,2) NOT NULL CHECK (resulting_balance >= 0),
    description VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_ledger_account_created ON ledger_entry(account_id, created_at DESC, id DESC);
CREATE INDEX ix_ledger_transfer ON ledger_entry(transfer_id);

CREATE TABLE refresh_token (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    device_summary VARCHAR(160) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_refresh_user_active ON refresh_token(user_id, expires_at) WHERE revoked_at IS NULL;

CREATE TABLE audit_log (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES app_user(id),
    action VARCHAR(48) NOT NULL,
    outcome VARCHAR(16) NOT NULL,
    actor_label VARCHAR(120),
    target_type VARCHAR(48),
    target_id VARCHAR(64),
    metadata VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_audit_created ON audit_log(created_at DESC);
CREATE INDEX ix_audit_user_created ON audit_log(user_id, created_at DESC);
CREATE INDEX ix_audit_action_created ON audit_log(action, created_at DESC);

CREATE TABLE notification (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    title VARCHAR(120) NOT NULL,
    message VARCHAR(300) NOT NULL,
    type VARCHAR(24) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_notification_user_created ON notification(user_id, created_at DESC);

CREATE TABLE rate_limit_bucket (
    bucket_key VARCHAR(128) PRIMARY KEY,
    window_started_at TIMESTAMPTZ NOT NULL,
    request_count INTEGER NOT NULL CHECK (request_count > 0)
);

INSERT INTO app_user (id, full_name, email, username, password_hash, status, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001', 'Conta interna do sistema', 'system@vbank.invalid', 'vbank-system', '{SYSTEM}', 'BLOCKED', NOW(), NOW());

INSERT INTO user_role (user_id, role)
VALUES ('00000000-0000-0000-0000-000000000001', 'SYSTEM');

INSERT INTO account (id, user_id, agency, account_number, account_digit, balance, daily_limit,
                     transferred_today, limit_reference_date, status, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001',
        '0000', '0000000000', '0', 999999999999.00, 999999999999.00, 0, CURRENT_DATE, 'SYSTEM', NOW(), NOW());

INSERT INTO ledger_entry (id, account_id, type, category, amount, resulting_balance, description, created_at)
VALUES ('00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000002',
        'CREDIT', 'OPENING_BALANCE', 999999999999.00, 999999999999.00, 'Reserva fictícia da conta interna do sistema', NOW());
