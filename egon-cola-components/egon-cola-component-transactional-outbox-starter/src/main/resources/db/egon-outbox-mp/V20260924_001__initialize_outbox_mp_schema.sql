CREATE TABLE egon_cola_outbox_message (
    id bigint NOT NULL,
    message_id varchar(64) NOT NULL,
    idempotency_key varchar(256),
    message_fingerprint char(64) NOT NULL,
    channel varchar(64) NOT NULL,
    destination varchar(256) NOT NULL,
    payload text NOT NULL,
    content_type varchar(128) NOT NULL,
    schema_version varchar(32),
    headers_json text NOT NULL DEFAULT '{}',
    trace_id varchar(128),
    status varchar(32) NOT NULL,
    attempt_count integer NOT NULL DEFAULT 0,
    max_attempts integer NOT NULL,
    next_attempt_at timestamptz(6) NOT NULL,
    locked_by varchar(128),
    locked_until timestamptz(6),
    last_error_code varchar(64),
    last_error_message text,
    created_at timestamptz(6) NOT NULL,
    updated_at timestamptz(6) NOT NULL,
    completed_at timestamptz(6),
    tenant_id bigint NOT NULL,
    create_user_id varchar(128) NOT NULL,
    create_time timestamptz(6) NOT NULL,
    update_user_id varchar(128) NOT NULL,
    update_time timestamptz(6) NOT NULL,
    deleted_at timestamp(6) without time zone,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_outbox_message PRIMARY KEY (id),
    CONSTRAINT ck_outbox_status CHECK (status IN ('PENDING', 'PROCESSING', 'RETRY_WAIT', 'SUCCEEDED', 'DEAD')),
    CONSTRAINT ck_outbox_attempt_count CHECK (attempt_count >= 0),
    CONSTRAINT ck_outbox_max_attempts CHECK (max_attempts >= 1),
    CONSTRAINT ck_outbox_technical_tenant CHECK (tenant_id = 0),
    CONSTRAINT ck_outbox_not_soft_deleted CHECK (deleted_at IS NULL),
    CONSTRAINT ck_outbox_version CHECK (version >= 0)
);

CREATE UNIQUE INDEX uk_outbox_message_id
    ON egon_cola_outbox_message (message_id);

CREATE UNIQUE INDEX uk_outbox_idempotency_key
    ON egon_cola_outbox_message (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX idx_outbox_claim
    ON egon_cola_outbox_message (next_attempt_at, id)
    WHERE status IN ('PENDING', 'RETRY_WAIT');

CREATE INDEX idx_outbox_reclaim
    ON egon_cola_outbox_message (locked_until, id)
    WHERE status = 'PROCESSING';

CREATE INDEX idx_outbox_cleanup
    ON egon_cola_outbox_message (completed_at, id)
    WHERE status = 'SUCCEEDED';

CREATE TABLE ddl_history (
    tenant_id bigint NOT NULL DEFAULT 0 CHECK (tenant_id = 0),
    script varchar(500) NOT NULL,
    type varchar(30) NOT NULL DEFAULT 'SQL' CHECK (type = 'SQL'),
    version varchar(30) NOT NULL,
    checksum char(64) NOT NULL CHECK (checksum ~ '^[0-9a-f]{64}$'),
    installed_on timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    execution_ms bigint NOT NULL CHECK (execution_ms >= 0),
    route_fingerprint char(64) NOT NULL CHECK (route_fingerprint ~ '^[0-9a-f]{64}$'),
    CONSTRAINT pk_ddl_history PRIMARY KEY (script, type),
    CONSTRAINT uk_ddl_history_type_version UNIQUE (type, version)
);
