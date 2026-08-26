CREATE TABLE gateway_openapi_snapshot (
    id VARCHAR(64) PRIMARY KEY,
    application_id VARCHAR(64) NOT NULL
        REFERENCES gateway_application(id) ON DELETE RESTRICT,
    definition_set_id VARCHAR(64)
        REFERENCES gateway_definition_set(id) ON DELETE RESTRICT,
    build_id VARCHAR(256) NOT NULL,
    artifact_version VARCHAR(128) NOT NULL,
    openapi_group VARCHAR(128) NOT NULL DEFAULT 'default',
    openapi_version VARCHAR(32) NOT NULL,
    document_sha256 CHAR(64) NOT NULL,
    canonical_sha256 CHAR(64) NOT NULL,
    document_json JSONB NOT NULL,
    validation_status VARCHAR(32) NOT NULL,
    validation_messages JSONB NOT NULL DEFAULT '[]'::jsonb,
    operation_count INTEGER NOT NULL DEFAULT 0,
    schema_count INTEGER NOT NULL DEFAULT 0,
    fetched_from_instance_id VARCHAR(256) NOT NULL,
    fetched_at TIMESTAMPTZ NOT NULL,
    validated_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_gateway_openapi_snapshot_group CHECK (
        openapi_group ~ '^[a-z][a-z0-9-]{0,63}$'
    ),
    CONSTRAINT ck_gateway_openapi_snapshot_version CHECK (
        openapi_version LIKE '3.1.%'
    ),
    CONSTRAINT ck_gateway_openapi_snapshot_document_sha CHECK (
        document_sha256 ~ '^[0-9a-f]{64}$'
    ),
    CONSTRAINT ck_gateway_openapi_snapshot_canonical_sha CHECK (
        canonical_sha256 ~ '^[0-9a-f]{64}$'
    ),
    CONSTRAINT ck_gateway_openapi_snapshot_document_json CHECK (
        jsonb_typeof(document_json) = 'object'
    ),
    CONSTRAINT ck_gateway_openapi_snapshot_validation_status CHECK (
        validation_status IN ('VALID', 'INVALID')
    ),
    CONSTRAINT ck_gateway_openapi_snapshot_validation_messages CHECK (
        jsonb_typeof(validation_messages) = 'array'
    ),
    CONSTRAINT ck_gateway_openapi_snapshot_operation_count CHECK (
        operation_count >= 0
    ),
    CONSTRAINT ck_gateway_openapi_snapshot_schema_count CHECK (
        schema_count >= 0
    )
);

CREATE UNIQUE INDEX uk_gateway_openapi_snapshot_contract
    ON gateway_openapi_snapshot(
        application_id,
        build_id,
        openapi_group,
        canonical_sha256
    );

CREATE INDEX idx_gateway_openapi_snapshot_definition
    ON gateway_openapi_snapshot(
        definition_set_id,
        openapi_group,
        id
    )
    WHERE definition_set_id IS NOT NULL;

CREATE INDEX idx_gateway_openapi_snapshot_app_time
    ON gateway_openapi_snapshot(
        application_id,
        fetched_at DESC,
        id DESC
    );

CREATE TABLE gateway_openapi_sync_state (
    id VARCHAR(64) PRIMARY KEY,
    application_id VARCHAR(64) NOT NULL
        REFERENCES gateway_application(id) ON DELETE RESTRICT,
    build_id VARCHAR(256) NOT NULL,
    artifact_version VARCHAR(128) NOT NULL,
    openapi_group VARCHAR(128) NOT NULL DEFAULT 'default',
    provider_service_name VARCHAR(256) NOT NULL,
    provider_group VARCHAR(128) NOT NULL DEFAULT 'default',
    provider_version VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DISCOVERED',
    latest_snapshot_id VARCHAR(64)
        REFERENCES gateway_openapi_snapshot(id) ON DELETE RESTRICT,
    definition_set_id VARCHAR(64)
        REFERENCES gateway_definition_set(id) ON DELETE RESTRICT,
    last_instance_id VARCHAR(256),
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_error_code VARCHAR(128),
    last_error_message VARCHAR(1024),
    first_discovered_at TIMESTAMPTZ NOT NULL,
    last_attempt_at TIMESTAMPTZ,
    last_success_at TIMESTAMPTZ,
    next_retry_at TIMESTAMPTZ,
    revision BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_gateway_openapi_sync_group CHECK (
        openapi_group ~ '^[a-z][a-z0-9-]{0,63}$'
    ),
    CONSTRAINT ck_gateway_openapi_sync_status CHECK (
        status IN (
            'DISCOVERED',
            'FETCHING',
            'VALIDATING',
            'INVALID',
            'INCONSISTENT_BUILD',
            'INGESTING',
            'VALID',
            'INGEST_FAILED',
            'FETCH_FAILED',
            'STALE'
        )
    ),
    CONSTRAINT ck_gateway_openapi_sync_attempt_count CHECK (
        attempt_count >= 0
    ),
    CONSTRAINT ck_gateway_openapi_sync_revision CHECK (
        revision >= 0
    )
);

CREATE UNIQUE INDEX uk_gateway_openapi_sync_key
    ON gateway_openapi_sync_state(
        application_id,
        build_id,
        openapi_group
    );

CREATE INDEX idx_gateway_openapi_sync_due
    ON gateway_openapi_sync_state(next_retry_at, id)
    WHERE status IN (
        'DISCOVERED',
        'FETCH_FAILED',
        'INGEST_FAILED',
        'STALE'
    );

CREATE INDEX idx_gateway_openapi_sync_app
    ON gateway_openapi_sync_state(
        application_id,
        updated_at DESC,
        id DESC
    );
