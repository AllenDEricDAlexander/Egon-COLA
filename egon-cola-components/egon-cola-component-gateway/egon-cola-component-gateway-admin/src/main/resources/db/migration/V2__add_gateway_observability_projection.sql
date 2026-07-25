CREATE TABLE gateway_call_event_summary (
    event_id VARCHAR(64) PRIMARY KEY,
    trace_id VARCHAR(32) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ NOT NULL,
    duration_ms BIGINT NOT NULL,
    protocol VARCHAR(16) NOT NULL,
    access_zone VARCHAR(16) NOT NULL,
    env VARCHAR(64) NOT NULL,
    namespace VARCHAR(128) NOT NULL,
    gateway_group_id VARCHAR(64),
    operation_id VARCHAR(128),
    route_id VARCHAR(128),
    result_category VARCHAR(32) NOT NULL,
    gateway_error_code VARCHAR(128),
    http_status INTEGER,
    grpc_status VARCHAR(64),
    engine_node_id VARCHAR(256) NOT NULL,
    provider_service VARCHAR(512),
    attempt_count INTEGER NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_gateway_call_trace
    ON gateway_call_event_summary (trace_id, occurred_at DESC);
CREATE INDEX idx_gateway_call_scope_time
    ON gateway_call_event_summary
        (env, namespace, occurred_at DESC);
CREATE INDEX idx_gateway_call_group_time
    ON gateway_call_event_summary
        (gateway_group_id, occurred_at DESC);

CREATE TABLE gateway_call_metric_minute (
    bucket_at TIMESTAMPTZ NOT NULL,
    env VARCHAR(64) NOT NULL,
    namespace VARCHAR(128) NOT NULL,
    protocol VARCHAR(16) NOT NULL,
    gateway_group_id VARCHAR(64) NOT NULL DEFAULT '',
    request_count BIGINT NOT NULL,
    error_count BIGINT NOT NULL,
    duration_total_ms BIGINT NOT NULL,
    duration_max_ms BIGINT NOT NULL,
    PRIMARY KEY (
        bucket_at,
        env,
        namespace,
        protocol,
        gateway_group_id
    )
);

CREATE INDEX idx_gateway_call_metric_scope_time
    ON gateway_call_metric_minute
        (env, namespace, bucket_at DESC);

CREATE TABLE gateway_call_event_consume_failure (
    id VARCHAR(64) PRIMARY KEY,
    topic VARCHAR(256) NOT NULL,
    partition_no INTEGER NOT NULL,
    offset_no BIGINT NOT NULL,
    event_id VARCHAR(64),
    failure_code VARCHAR(128) NOT NULL,
    failure_message VARCHAR(1024),
    payload_sha256 VARCHAR(64) NOT NULL,
    payload_size INTEGER NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    UNIQUE (topic, partition_no, offset_no)
);

CREATE INDEX idx_gateway_call_failure_time
    ON gateway_call_event_consume_failure (occurred_at DESC);
