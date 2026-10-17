CREATE TABLE ddc_ai_usage_feedback_t0 (
    id bigint NOT NULL CHECK (id > 0),
    tenant_id bigint NOT NULL CHECK (tenant_id > 0),
    create_user_id varchar(512) NOT NULL,
    create_time timestamptz(6) NOT NULL,
    update_user_id varchar(512) NOT NULL,
    update_time timestamptz(6) NOT NULL,
    deleted_at timestamp(6) without time zone,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    resource_id bigint NOT NULL,
    version_id bigint NOT NULL,
    usage_id varchar(64) NOT NULL CHECK (usage_id ~ '^[A-Za-z0-9_-]{16,64}$'),
    reporter_credential_id varchar(128) NOT NULL,
    biz_code varchar(128) NOT NULL,
    env_code varchar(32) NOT NULL,
    app_code varchar(128) NOT NULL,
    outcome varchar(16) NOT NULL CHECK (outcome IN ('SUCCESS', 'FAILURE', 'UNKNOWN')),
    explicit_score smallint CHECK (explicit_score BETWEEN 0 AND 100),
    judge_score smallint CHECK (judge_score BETWEEN 0 AND 100),
    judge_state varchar(16) NOT NULL
        CHECK (judge_state IN ('NOT_REQUIRED', 'PENDING', 'JUDGING', 'DONE', 'FAILED')),
    judge_attempts smallint NOT NULL DEFAULT 0 CHECK (judge_attempts BETWEEN 0 AND 10),
    judge_lease_until timestamptz(6),
    judged_at timestamptz(6),
    judge_error varchar(64),
    input_excerpt text CHECK (input_excerpt IS NULL OR octet_length(input_excerpt) <= 8192),
    output_excerpt text CHECK (output_excerpt IS NULL OR octet_length(output_excerpt) <= 32768),
    latency_ms integer CHECK (latency_ms >= 0),
    occurred_at timestamptz(6) NOT NULL,
    trace_id varchar(64),
    CONSTRAINT pk_ai_usage_feedback PRIMARY KEY (id),
    CONSTRAINT uk_ai_usage_feedback_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_ai_usage_feedback_usage UNIQUE (tenant_id, usage_id),
    CONSTRAINT fk_ai_usage_feedback_resource FOREIGN KEY (tenant_id, resource_id)
        REFERENCES ddc_ai_resource_t0 (tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_ai_usage_feedback_version FOREIGN KEY (tenant_id, resource_id, version_id)
        REFERENCES ddc_ai_version_t0 (tenant_id, resource_id, id) ON DELETE RESTRICT,
    CONSTRAINT ck_ai_usage_feedback_judge_done CHECK ((judge_state = 'DONE') = (judge_score IS NOT NULL)),
    CONSTRAINT ck_ai_usage_feedback_judge_lease CHECK ((judge_state = 'JUDGING') = (judge_lease_until IS NOT NULL))
);

CREATE TABLE ddc_ai_optimization_policy_t0 (
    id bigint NOT NULL CHECK (id > 0),
    tenant_id bigint NOT NULL CHECK (tenant_id > 0),
    create_user_id varchar(512) NOT NULL,
    create_time timestamptz(6) NOT NULL,
    update_user_id varchar(512) NOT NULL,
    update_time timestamptz(6) NOT NULL,
    deleted_at timestamp(6) without time zone,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    resource_id bigint NOT NULL,
    enabled boolean NOT NULL DEFAULT false,
    score_threshold smallint NOT NULL DEFAULT 80 CHECK (score_threshold BETWEEN 1 AND 100),
    min_samples integer NOT NULL DEFAULT 20 CHECK (min_samples BETWEEN 1 AND 100000),
    evaluation_window_seconds integer NOT NULL DEFAULT 86400
        CHECK (evaluation_window_seconds BETWEEN 300 AND 2592000),
    max_iterations smallint NOT NULL DEFAULT 5 CHECK (max_iterations BETWEEN 1 AND 50),
    cooldown_seconds integer NOT NULL DEFAULT 3600 CHECK (cooldown_seconds BETWEEN 60 AND 604800),
    auto_publish boolean NOT NULL DEFAULT false,
    judge_enabled boolean NOT NULL DEFAULT true,
    optimizer_model_alias varchar(64) NOT NULL CHECK (optimizer_model_alias ~ '^[a-z0-9][a-z0-9-]{0,63}$'),
    judge_model_alias varchar(64) NOT NULL CHECK (judge_model_alias ~ '^[a-z0-9][a-z0-9-]{0,63}$'),
    optimizer_instruction varchar(4096) NOT NULL DEFAULT '',
    state varchar(24) NOT NULL DEFAULT 'ARMED'
        CHECK (state IN ('ARMED', 'AWAITING_PUBLISH', 'CONVERGED', 'EXHAUSTED', 'PAUSED')),
    iterations_done smallint NOT NULL DEFAULT 0 CHECK (iterations_done >= 0),
    consecutive_failures smallint NOT NULL DEFAULT 0 CHECK (consecutive_failures >= 0),
    last_evaluated_at timestamptz(6),
    last_evaluated_version_id bigint,
    last_average_score numeric(5, 2) CHECK (last_average_score BETWEEN 0 AND 100),
    last_sample_count integer CHECK (last_sample_count >= 0),
    active_run_id bigint,
    converged_at timestamptz(6),
    CONSTRAINT pk_ai_optimization_policy PRIMARY KEY (id),
    CONSTRAINT uk_ai_optimization_policy_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_ai_optimization_policy_resource FOREIGN KEY (tenant_id, resource_id)
        REFERENCES ddc_ai_resource_t0 (tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT ck_ai_optimization_policy_converged CHECK ((state = 'CONVERGED') = (converged_at IS NOT NULL))
);

CREATE TABLE ddc_ai_optimization_run_t0 (
    id bigint NOT NULL CHECK (id > 0),
    tenant_id bigint NOT NULL CHECK (tenant_id > 0),
    create_user_id varchar(512) NOT NULL,
    create_time timestamptz(6) NOT NULL,
    update_user_id varchar(512) NOT NULL,
    update_time timestamptz(6) NOT NULL,
    deleted_at timestamp(6) without time zone,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    resource_id bigint NOT NULL,
    policy_id bigint NOT NULL,
    run_no bigint NOT NULL CHECK (run_no > 0),
    iteration_no smallint NOT NULL CHECK (iteration_no > 0),
    "trigger" varchar(16) NOT NULL CHECK ("trigger" IN ('SCHEDULED', 'MANUAL')),
    request_id varchar(64) CHECK (request_id ~ '^[A-Za-z0-9_-]{16,64}$'),
    request_sha256 char(64) CHECK (request_sha256 ~ '^[0-9a-f]{64}$'),
    state varchar(16) NOT NULL DEFAULT 'PENDING'
        CHECK (state IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'SKIPPED')),
    base_version_id bigint NOT NULL,
    sample_count integer NOT NULL CHECK (sample_count >= 0),
    average_score numeric(5, 2) CHECK (average_score BETWEEN 0 AND 100),
    threshold_snapshot smallint NOT NULL CHECK (threshold_snapshot BETWEEN 1 AND 100),
    window_start timestamptz(6) NOT NULL,
    window_end timestamptz(6) NOT NULL,
    model_alias varchar(64) NOT NULL CHECK (model_alias ~ '^[a-z0-9][a-z0-9-]{0,63}$'),
    prompt_sha256 char(64) CHECK (prompt_sha256 ~ '^[0-9a-f]{64}$'),
    output_sha256 char(64) CHECK (output_sha256 ~ '^[0-9a-f]{64}$'),
    prompt_tokens integer CHECK (prompt_tokens >= 0),
    completion_tokens integer CHECK (completion_tokens >= 0),
    produced_version_id bigint,
    publication_id bigint,
    auto_published boolean NOT NULL DEFAULT false,
    error_code varchar(32) CHECK (error_code IS NULL OR error_code IN (
        'MODEL_AUTH', 'MODEL_TIMEOUT', 'MODEL_ERROR', 'MODEL_RATE_LIMITED', 'INVALID_OUTPUT',
        'BASE_CHANGED', 'DRAFT_DIRTY', 'PUBLISH_CONFLICT', 'LEASE_EXPIRED', 'NOT_PUBLISHED')),
    error_message varchar(512),
    note varchar(512) NOT NULL DEFAULT '',
    lease_owner varchar(128),
    lease_until timestamptz(6),
    started_at timestamptz(6),
    finished_at timestamptz(6),
    CONSTRAINT pk_ai_optimization_run PRIMARY KEY (id),
    CONSTRAINT uk_ai_optimization_run_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_ai_optimization_run_no UNIQUE (tenant_id, resource_id, run_no),
    CONSTRAINT fk_ai_optimization_run_resource FOREIGN KEY (tenant_id, resource_id)
        REFERENCES ddc_ai_resource_t0 (tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_ai_optimization_run_policy FOREIGN KEY (tenant_id, policy_id)
        REFERENCES ddc_ai_optimization_policy_t0 (tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_ai_optimization_run_base FOREIGN KEY (tenant_id, resource_id, base_version_id)
        REFERENCES ddc_ai_version_t0 (tenant_id, resource_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_ai_optimization_run_produced FOREIGN KEY (tenant_id, resource_id, produced_version_id)
        REFERENCES ddc_ai_version_t0 (tenant_id, resource_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_ai_optimization_run_publication FOREIGN KEY (tenant_id, publication_id)
        REFERENCES ddc_ai_publication_t0 (tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT ck_ai_optimization_run_request CHECK ((request_id IS NULL) = (request_sha256 IS NULL)),
    CONSTRAINT ck_ai_optimization_run_window CHECK (window_start < window_end),
    CONSTRAINT ck_ai_optimization_run_auto_published CHECK (NOT auto_published OR publication_id IS NOT NULL),
    CONSTRAINT ck_ai_optimization_run_error CHECK ((state IN ('FAILED', 'SKIPPED')) = (error_code IS NOT NULL)),
    CONSTRAINT ck_ai_optimization_run_lease CHECK ((state = 'RUNNING') = (lease_until IS NOT NULL)),
    CONSTRAINT ck_ai_optimization_run_finished CHECK (
        (state IN ('SUCCEEDED', 'FAILED', 'SKIPPED')) = (finished_at IS NOT NULL))
);

CREATE INDEX ix_ai_usage_feedback_version_time
    ON ddc_ai_usage_feedback_t0 (tenant_id, resource_id, version_id, occurred_at DESC, id DESC)
    WHERE deleted_at IS NULL;
CREATE INDEX ix_ai_usage_feedback_judge_pending
    ON ddc_ai_usage_feedback_t0 (tenant_id, resource_id, occurred_at)
    WHERE deleted_at IS NULL AND judge_state IN ('PENDING', 'JUDGING');
CREATE INDEX ix_ai_usage_feedback_retention
    ON ddc_ai_usage_feedback_t0 (tenant_id, occurred_at, id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uk_ai_optimization_policy_lifecycle
    ON ddc_ai_optimization_policy_t0 (tenant_id, resource_id, deleted_at);
CREATE UNIQUE INDEX uk_ai_optimization_policy_live
    ON ddc_ai_optimization_policy_t0 (tenant_id, resource_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_ai_optimization_policy_scan
    ON ddc_ai_optimization_policy_t0 (tenant_id, state, last_evaluated_at)
    WHERE deleted_at IS NULL AND enabled;
CREATE UNIQUE INDEX uk_ai_optimization_run_request
    ON ddc_ai_optimization_run_t0 (tenant_id, request_id) WHERE request_id IS NOT NULL;
CREATE UNIQUE INDEX uk_ai_optimization_run_active
    ON ddc_ai_optimization_run_t0 (tenant_id, resource_id)
    WHERE state IN ('PENDING', 'RUNNING') AND deleted_at IS NULL;
CREATE INDEX ix_ai_optimization_run_claim
    ON ddc_ai_optimization_run_t0 (tenant_id, state, lease_until)
    WHERE state IN ('PENDING', 'RUNNING') AND deleted_at IS NULL;
CREATE INDEX ix_ai_optimization_run_page
    ON ddc_ai_optimization_run_t0 (tenant_id, resource_id, id DESC) WHERE deleted_at IS NULL;
