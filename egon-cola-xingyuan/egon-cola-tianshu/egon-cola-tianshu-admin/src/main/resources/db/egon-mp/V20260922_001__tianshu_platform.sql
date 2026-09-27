CREATE SCHEMA IF NOT EXISTS egon_outbox;

CREATE TABLE ddc_biz_t0 (
    id bigint NOT NULL CHECK (id > 0),
    tenant_id bigint NOT NULL CHECK (tenant_id > 0),
    create_user_id varchar(512) NOT NULL,
    create_time timestamptz(6) NOT NULL,
    update_user_id varchar(512) NOT NULL,
    update_time timestamptz(6) NOT NULL,
    deleted_at timestamp(6) without time zone,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    public_id varchar(64) NOT NULL,
    biz_code varchar(128) NOT NULL,
    biz_name varchar(128) NOT NULL,
    description varchar(512),
    enabled boolean NOT NULL DEFAULT true,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    CONSTRAINT pk_biz PRIMARY KEY (id),
    CONSTRAINT uk_biz_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_biz_public UNIQUE (tenant_id, public_id)
);

CREATE TABLE ddc_env_t0 (
    id bigint NOT NULL CHECK (id > 0),
    tenant_id bigint NOT NULL CHECK (tenant_id > 0),
    create_user_id varchar(512) NOT NULL,
    create_time timestamptz(6) NOT NULL,
    update_user_id varchar(512) NOT NULL,
    update_time timestamptz(6) NOT NULL,
    deleted_at timestamp(6) without time zone,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    public_id varchar(64) NOT NULL,
    env_code varchar(32) NOT NULL,
    description varchar(256),
    sort_order integer NOT NULL DEFAULT 0,
    enabled boolean NOT NULL DEFAULT true,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    CONSTRAINT pk_env PRIMARY KEY (id),
    CONSTRAINT uk_env_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_env_public UNIQUE (tenant_id, public_id)
);

CREATE TABLE ddc_app_t0 (
    id bigint NOT NULL CHECK (id > 0),
    tenant_id bigint NOT NULL CHECK (tenant_id > 0),
    create_user_id varchar(512) NOT NULL,
    create_time timestamptz(6) NOT NULL,
    update_user_id varchar(512) NOT NULL,
    update_time timestamptz(6) NOT NULL,
    deleted_at timestamp(6) without time zone,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    public_id varchar(64) NOT NULL,
    app_code varchar(128) NOT NULL,
    app_name varchar(128) NOT NULL,
    owner varchar(128),
    description varchar(512),
    enabled boolean NOT NULL DEFAULT true,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    biz_code varchar(128) NOT NULL,
    CONSTRAINT pk_app PRIMARY KEY (id),
    CONSTRAINT uk_app_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_app_public UNIQUE (tenant_id, public_id)
);

CREATE TABLE ddc_namespace_t0 (
    id bigint NOT NULL CHECK (id > 0),
    tenant_id bigint NOT NULL CHECK (tenant_id > 0),
    create_user_id varchar(512) NOT NULL,
    create_time timestamptz(6) NOT NULL,
    update_user_id varchar(512) NOT NULL,
    update_time timestamptz(6) NOT NULL,
    deleted_at timestamp(6) without time zone,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    public_id varchar(64) NOT NULL,
    namespace varchar(128) NOT NULL,
    description varchar(512),
    enabled boolean NOT NULL DEFAULT true,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    namespace_code varchar(128) NOT NULL,
    biz_code varchar(128) NOT NULL,
    CONSTRAINT pk_namespace PRIMARY KEY (id),
    CONSTRAINT uk_namespace_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_namespace_public UNIQUE (tenant_id, public_id)
);

CREATE TABLE ddc_namespace_env_app_t0 (
    id bigint NOT NULL CHECK (id > 0),
    tenant_id bigint NOT NULL CHECK (tenant_id > 0),
    create_user_id varchar(512) NOT NULL,
    create_time timestamptz(6) NOT NULL,
    update_user_id varchar(512) NOT NULL,
    update_time timestamptz(6) NOT NULL,
    deleted_at timestamp(6) without time zone,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    public_id varchar(64) NOT NULL,
    namespace_id bigint NOT NULL,
    env_code varchar(32) NOT NULL,
    app_id bigint NOT NULL,
    enabled boolean NOT NULL DEFAULT true,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    CONSTRAINT pk_namespace_env_app PRIMARY KEY (id),
    CONSTRAINT uk_namespace_env_app_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_namespace_env_app_public UNIQUE (tenant_id, public_id),
    CONSTRAINT fk_namespace_env_app_namespace FOREIGN KEY (tenant_id, namespace_id)
        REFERENCES ddc_namespace_t0 (tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_namespace_env_app_app FOREIGN KEY (tenant_id, app_id)
        REFERENCES ddc_app_t0 (tenant_id, id) ON DELETE RESTRICT
);

CREATE TABLE ddc_config_item_t0 (
    id bigint NOT NULL CHECK (id > 0),
    tenant_id bigint NOT NULL CHECK (tenant_id > 0),
    create_user_id varchar(512) NOT NULL,
    create_time timestamptz(6) NOT NULL,
    update_user_id varchar(512) NOT NULL,
    update_time timestamptz(6) NOT NULL,
    deleted_at timestamp(6) without time zone,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    public_id varchar(64) NOT NULL,
    app_code varchar(128) NOT NULL,
    env varchar(32) NOT NULL,
    namespace varchar(128),
    config_key varchar(256) NOT NULL,
    config_value text,
    default_value text,
    value_type varchar(32) NOT NULL,
    current_version bigint NOT NULL,
    description varchar(512),
    enabled boolean NOT NULL DEFAULT true,
    deleted boolean NOT NULL DEFAULT false,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    published_version bigint,
    biz_code varchar(128) NOT NULL,
    CONSTRAINT pk_config_item PRIMARY KEY (id),
    CONSTRAINT uk_config_item_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_config_item_public UNIQUE (tenant_id, public_id)
);

CREATE TABLE ddc_config_version_t0 (
    id bigint NOT NULL CHECK (id > 0),
    tenant_id bigint NOT NULL CHECK (tenant_id > 0),
    create_user_id varchar(512) NOT NULL,
    create_time timestamptz(6) NOT NULL,
    update_user_id varchar(512) NOT NULL,
    update_time timestamptz(6) NOT NULL,
    deleted_at timestamp(6) without time zone,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    public_id varchar(64) NOT NULL,
    config_id bigint NOT NULL,
    app_code varchar(128) NOT NULL,
    env varchar(32) NOT NULL,
    namespace varchar(128),
    config_key varchar(256) NOT NULL,
    config_revision bigint NOT NULL CHECK (config_revision > 0),
    old_value text,
    new_value text,
    value_type varchar(32),
    change_type varchar(32),
    change_reason varchar(512),
    operator varchar(128),
    operator_ip varchar(64),
    created_at timestamp(6) without time zone NOT NULL,
    biz_code varchar(128),
    CONSTRAINT pk_config_version PRIMARY KEY (id),
    CONSTRAINT uk_config_version_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_config_version_public UNIQUE (tenant_id, public_id),
    CONSTRAINT uk_config_version_business UNIQUE (tenant_id, config_id, config_revision),
    CONSTRAINT fk_config_version_item FOREIGN KEY (tenant_id, config_id)
        REFERENCES ddc_config_item_t0 (tenant_id, id) ON DELETE RESTRICT
);

CREATE TABLE ddc_publish_task_t0 (
    id bigint NOT NULL CHECK (id > 0),
    tenant_id bigint NOT NULL CHECK (tenant_id > 0),
    create_user_id varchar(512) NOT NULL,
    create_time timestamptz(6) NOT NULL,
    update_user_id varchar(512) NOT NULL,
    update_time timestamptz(6) NOT NULL,
    deleted_at timestamp(6) without time zone,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    public_id varchar(64) NOT NULL,
    change_id varchar(64) NOT NULL,
    config_id bigint,
    app_code varchar(128),
    env varchar(32),
    namespace varchar(128),
    config_key varchar(256),
    target_version bigint,
    publish_mode varchar(32),
    status varchar(32),
    target_count integer DEFAULT 0,
    ack_count integer DEFAULT 0,
    failed_count integer DEFAULT 0,
    ignored_count integer DEFAULT 0,
    timeout_count integer DEFAULT 0,
    timeout_ms bigint,
    operator varchar(128),
    error_message text,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    content_checksum varchar(64),
    attempt_count integer NOT NULL DEFAULT 0,
    dispatched_at timestamp(6) without time zone,
    completed_at timestamp(6) without time zone,
    failure_stage varchar(64),
    biz_code varchar(128),
    CONSTRAINT pk_publish_task PRIMARY KEY (id),
    CONSTRAINT uk_publish_task_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_publish_task_public UNIQUE (tenant_id, public_id),
    CONSTRAINT uk_publish_task_business UNIQUE (tenant_id, change_id),
    CONSTRAINT fk_publish_task_config FOREIGN KEY (tenant_id, config_id)
        REFERENCES ddc_config_item_t0 (tenant_id, id) ON DELETE RESTRICT
);

CREATE TABLE ddc_publish_ack_t0 (
    id bigint NOT NULL CHECK (id > 0),
    tenant_id bigint NOT NULL CHECK (tenant_id > 0),
    create_user_id varchar(512) NOT NULL,
    create_time timestamptz(6) NOT NULL,
    update_user_id varchar(512) NOT NULL,
    update_time timestamptz(6) NOT NULL,
    deleted_at timestamp(6) without time zone,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    public_id varchar(64) NOT NULL,
    change_id varchar(64) NOT NULL,
    instance_id varchar(256) NOT NULL,
    app_code varchar(128),
    env varchar(32),
    namespace varchar(128),
    config_key varchar(256),
    target_version bigint,
    current_version bigint,
    ack_status varchar(32),
    error_message text,
    ack_at timestamp(6) without time zone,
    lease_id varchar(64),
    content_checksum varchar(64),
    biz_code varchar(128),
    CONSTRAINT pk_publish_ack PRIMARY KEY (id),
    CONSTRAINT uk_publish_ack_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_publish_ack_public UNIQUE (tenant_id, public_id),
    CONSTRAINT uk_publish_ack_target UNIQUE (tenant_id, change_id, instance_id, lease_id)
);

CREATE TABLE ddc_instance_t0 (
    id bigint NOT NULL CHECK (id > 0),
    tenant_id bigint NOT NULL CHECK (tenant_id > 0),
    create_user_id varchar(512) NOT NULL,
    create_time timestamptz(6) NOT NULL,
    update_user_id varchar(512) NOT NULL,
    update_time timestamptz(6) NOT NULL,
    deleted_at timestamp(6) without time zone,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    public_id varchar(64) NOT NULL,
    instance_id varchar(256) NOT NULL,
    app_code varchar(128),
    env varchar(32),
    namespace varchar(128),
    host varchar(128),
    port integer,
    pid varchar(128),
    sdk_version varchar(64),
    status varchar(32),
    last_heartbeat_at timestamp(6) without time zone,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    lease_id varchar(64),
    lease_expire_at timestamp(6) without time zone,
    runtime_metadata text NOT NULL DEFAULT '{}',
    biz_code varchar(128),
    resource_server_id varchar(128),
    resource_version bigint,
    credential_id varchar(128),
    admission_expires_at timestamp(6) without time zone,
    CONSTRAINT pk_instance PRIMARY KEY (id),
    CONSTRAINT uk_instance_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_instance_public UNIQUE (tenant_id, public_id)
);

CREATE TABLE ddc_operation_log_t0 (
    id bigint NOT NULL CHECK (id > 0),
    tenant_id bigint NOT NULL CHECK (tenant_id > 0),
    create_user_id varchar(512) NOT NULL,
    create_time timestamptz(6) NOT NULL,
    update_user_id varchar(512) NOT NULL,
    update_time timestamptz(6) NOT NULL,
    deleted_at timestamp(6) without time zone,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    public_id varchar(64) NOT NULL,
    app_code varchar(128),
    env varchar(32),
    namespace varchar(128),
    config_key varchar(256),
    operation_type varchar(64),
    operator varchar(128),
    operator_ip varchar(64),
    operation_content text,
    created_at timestamp(6) without time zone NOT NULL,
    biz_code varchar(128),
    CONSTRAINT pk_operation_log PRIMARY KEY (id),
    CONSTRAINT uk_operation_log_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_operation_log_public UNIQUE (tenant_id, public_id)
);

CREATE TABLE ddc_ai_resource_t0 (
    id bigint NOT NULL CHECK (id > 0),
    tenant_id bigint NOT NULL CHECK (tenant_id > 0),
    create_user_id varchar(512) NOT NULL,
    create_time timestamptz(6) NOT NULL,
    update_user_id varchar(512) NOT NULL,
    update_time timestamptz(6) NOT NULL,
    deleted_at timestamp(6) without time zone,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    biz_code varchar(128) NOT NULL,
    namespace_id bigint NOT NULL,
    env_code varchar(32) NOT NULL,
    app_code varchar(128) NOT NULL,
    kind varchar(16) NOT NULL CHECK (kind IN ('PROMPT', 'SKILL')),
    resource_key varchar(128) COLLATE "C" NOT NULL,
    display_name varchar(128) NOT NULL,
    description varchar(2048) NOT NULL DEFAULT '',
    draft_prompt text,
    last_version_no bigint NOT NULL DEFAULT 0 CHECK (last_version_no >= 0),
    current_version_id bigint,
    CONSTRAINT pk_ai_resource PRIMARY KEY (id),
    CONSTRAINT uk_ai_resource_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_ai_resource_kind UNIQUE (tenant_id, id, kind),
    CONSTRAINT fk_ai_resource_namespace FOREIGN KEY (tenant_id, namespace_id)
        REFERENCES ddc_namespace_t0 (tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT ck_ai_resource_key CHECK (
        octet_length(resource_key) BETWEEN 1 AND 128
        AND resource_key ~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$'),
    CONSTRAINT ck_ai_resource_draft_prompt CHECK (
        (kind = 'PROMPT' AND draft_prompt IS NOT NULL)
        OR (kind = 'SKILL' AND draft_prompt IS NULL))
);

CREATE TABLE ddc_ai_version_t0 (
    id bigint NOT NULL CHECK (id > 0),
    tenant_id bigint NOT NULL CHECK (tenant_id > 0),
    create_user_id varchar(512) NOT NULL,
    create_time timestamptz(6) NOT NULL,
    update_user_id varchar(512) NOT NULL,
    update_time timestamptz(6) NOT NULL,
    deleted_at timestamp(6) without time zone,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    resource_id bigint NOT NULL,
    version_no bigint NOT NULL CHECK (version_no > 0),
    kind varchar(16) NOT NULL CHECK (kind IN ('PROMPT', 'SKILL')),
    display_name varchar(128) NOT NULL,
    description varchar(2048) NOT NULL DEFAULT '',
    prompt_text text,
    content_sha256 char(64) NOT NULL CHECK (content_sha256 ~ '^[0-9a-f]{64}$'),
    content_bytes bigint NOT NULL CHECK (content_bytes >= 0),
    node_count integer NOT NULL DEFAULT 0,
    source_version_id bigint,
    change_note varchar(512) NOT NULL DEFAULT '',
    CONSTRAINT pk_ai_version PRIMARY KEY (id),
    CONSTRAINT uk_ai_version_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_ai_version_no UNIQUE (tenant_id, resource_id, version_no),
    CONSTRAINT uk_ai_version_owner UNIQUE (tenant_id, resource_id, id),
    CONSTRAINT uk_ai_version_kind UNIQUE (tenant_id, id, kind),
    CONSTRAINT fk_ai_version_resource FOREIGN KEY (tenant_id, resource_id, kind)
        REFERENCES ddc_ai_resource_t0 (tenant_id, id, kind) ON DELETE RESTRICT,
    CONSTRAINT fk_ai_version_source FOREIGN KEY (tenant_id, source_version_id, kind)
        REFERENCES ddc_ai_version_t0 (tenant_id, id, kind) ON DELETE RESTRICT,
    CONSTRAINT ck_ai_version_content CHECK (
        (kind = 'PROMPT' AND prompt_text IS NOT NULL AND node_count = 0)
        OR (kind = 'SKILL' AND prompt_text IS NULL AND node_count BETWEEN 1 AND 256))
);

CREATE TABLE ddc_ai_draft_node_t0 (
    id bigint NOT NULL CHECK (id > 0),
    tenant_id bigint NOT NULL CHECK (tenant_id > 0),
    create_user_id varchar(512) NOT NULL,
    create_time timestamptz(6) NOT NULL,
    update_user_id varchar(512) NOT NULL,
    update_time timestamptz(6) NOT NULL,
    deleted_at timestamp(6) without time zone,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    resource_id bigint NOT NULL,
    parent_path varchar(1024) COLLATE "C",
    path varchar(1024) COLLATE "C" NOT NULL,
    node_type varchar(16) NOT NULL CHECK (node_type IN ('DIRECTORY', 'FILE')),
    content text,
    content_sha256 char(64),
    content_bytes bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_ai_draft_node PRIMARY KEY (id),
    CONSTRAINT uk_ai_draft_node_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_ai_draft_node_resource FOREIGN KEY (tenant_id, resource_id)
        REFERENCES ddc_ai_resource_t0 (tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT ck_ai_draft_node_path CHECK (
        octet_length(path) <= 1024
        AND (parent_path IS NULL OR octet_length(parent_path) <= 1024)
        AND ((path = '' AND parent_path IS NULL AND node_type = 'DIRECTORY')
            OR (path <> '' AND parent_path IS NOT NULL
                AND parent_path = regexp_replace(path, '(^|/)[^/]+$', '')))),
    CONSTRAINT ck_ai_draft_node_content CHECK (
        (node_type = 'DIRECTORY' AND content IS NULL AND content_sha256 IS NULL AND content_bytes = 0)
        OR (node_type = 'FILE' AND content IS NOT NULL AND content_sha256 IS NOT NULL
            AND content_sha256 ~ '^[0-9a-f]{64}$'
            AND content_bytes = octet_length(content) AND content_bytes BETWEEN 0 AND 262144))
);

CREATE TABLE ddc_ai_version_node_t0 (
    id bigint NOT NULL CHECK (id > 0),
    tenant_id bigint NOT NULL CHECK (tenant_id > 0),
    create_user_id varchar(512) NOT NULL,
    create_time timestamptz(6) NOT NULL,
    update_user_id varchar(512) NOT NULL,
    update_time timestamptz(6) NOT NULL,
    deleted_at timestamp(6) without time zone,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    version_id bigint NOT NULL,
    parent_path varchar(1024) COLLATE "C",
    path varchar(1024) COLLATE "C" NOT NULL,
    node_type varchar(16) NOT NULL CHECK (node_type IN ('DIRECTORY', 'FILE')),
    content text,
    content_sha256 char(64),
    content_bytes bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_ai_version_node PRIMARY KEY (id),
    CONSTRAINT uk_ai_version_node_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_ai_version_node_version FOREIGN KEY (tenant_id, version_id)
        REFERENCES ddc_ai_version_t0 (tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_ai_version_node_parent FOREIGN KEY (tenant_id, version_id, parent_path)
        REFERENCES ddc_ai_version_node_t0 (tenant_id, version_id, path)
        DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT ck_ai_version_node_path CHECK (
        octet_length(path) <= 1024
        AND (parent_path IS NULL OR octet_length(parent_path) <= 1024)
        AND ((path = '' AND parent_path IS NULL AND node_type = 'DIRECTORY')
            OR (path <> '' AND parent_path IS NOT NULL
                AND parent_path = regexp_replace(path, '(^|/)[^/]+$', '')))),
    CONSTRAINT ck_ai_version_node_content CHECK (
        (node_type = 'DIRECTORY' AND content IS NULL AND content_sha256 IS NULL AND content_bytes = 0)
        OR (node_type = 'FILE' AND content IS NOT NULL AND content_sha256 IS NOT NULL
            AND content_sha256 ~ '^[0-9a-f]{64}$'
            AND content_bytes = octet_length(content) AND content_bytes BETWEEN 0 AND 262144))
);

CREATE TABLE ddc_ai_publication_t0 (
    id bigint NOT NULL CHECK (id > 0),
    tenant_id bigint NOT NULL CHECK (tenant_id > 0),
    create_user_id varchar(512) NOT NULL,
    create_time timestamptz(6) NOT NULL,
    update_user_id varchar(512) NOT NULL,
    update_time timestamptz(6) NOT NULL,
    deleted_at timestamp(6) without time zone,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    resource_id bigint NOT NULL,
    operation varchar(16) NOT NULL CHECK (operation IN ('PUBLISH', 'ROLLBACK', 'PROMOTE')),
    request_id varchar(64) NOT NULL CHECK (request_id ~ '^[A-Za-z0-9_-]{16,64}$'),
    request_sha256 char(64) NOT NULL CHECK (request_sha256 ~ '^[0-9a-f]{64}$'),
    previous_version_id bigint,
    target_version_id bigint NOT NULL,
    source_version_id bigint,
    result_revision bigint NOT NULL CHECK (result_revision >= 0),
    target_created boolean NOT NULL DEFAULT false,
    CONSTRAINT pk_ai_publication PRIMARY KEY (id),
    CONSTRAINT uk_ai_publication_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_ai_publication_request UNIQUE (tenant_id, request_id),
    CONSTRAINT fk_ai_publication_resource FOREIGN KEY (tenant_id, resource_id)
        REFERENCES ddc_ai_resource_t0 (tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_ai_publication_previous FOREIGN KEY (tenant_id, resource_id, previous_version_id)
        REFERENCES ddc_ai_version_t0 (tenant_id, resource_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_ai_publication_target FOREIGN KEY (tenant_id, resource_id, target_version_id)
        REFERENCES ddc_ai_version_t0 (tenant_id, resource_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_ai_publication_source FOREIGN KEY (tenant_id, source_version_id)
        REFERENCES ddc_ai_version_t0 (tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT ck_ai_publication_source CHECK (
        (operation = 'PROMOTE' AND source_version_id IS NOT NULL)
        OR (operation IN ('PUBLISH', 'ROLLBACK') AND source_version_id IS NULL))
);

ALTER TABLE ddc_ai_resource_t0
    ADD CONSTRAINT fk_ai_resource_current_version
    FOREIGN KEY (tenant_id, id, current_version_id)
    REFERENCES ddc_ai_version_t0 (tenant_id, resource_id, id)
    DEFERRABLE INITIALLY DEFERRED;

CREATE UNIQUE INDEX uk_biz_lifecycle
    ON ddc_biz_t0 (tenant_id, biz_code, deleted_at);
CREATE UNIQUE INDEX uk_biz_live
    ON ddc_biz_t0 (tenant_id, biz_code) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uk_env_lifecycle
    ON ddc_env_t0 (tenant_id, env_code, deleted_at);
CREATE UNIQUE INDEX uk_env_live
    ON ddc_env_t0 (tenant_id, env_code) WHERE deleted_at IS NULL;
CREATE INDEX ix_env_page
    ON ddc_env_t0 (tenant_id, sort_order, env_code, id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uk_app_lifecycle
    ON ddc_app_t0 (tenant_id, app_code, deleted_at);
CREATE UNIQUE INDEX uk_app_live
    ON ddc_app_t0 (tenant_id, app_code) WHERE deleted_at IS NULL;
CREATE INDEX ix_app_page
    ON ddc_app_t0 (tenant_id, biz_code, app_code, id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uk_namespace_lifecycle
    ON ddc_namespace_t0 (tenant_id, biz_code, namespace_code, deleted_at);
CREATE UNIQUE INDEX uk_namespace_live
    ON ddc_namespace_t0 (tenant_id, biz_code, namespace_code) WHERE deleted_at IS NULL;
CREATE INDEX ix_namespace_page
    ON ddc_namespace_t0 (tenant_id, biz_code, namespace_code, id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uk_namespace_env_app_lifecycle
    ON ddc_namespace_env_app_t0 (tenant_id, namespace_id, env_code, app_id, deleted_at);
CREATE UNIQUE INDEX uk_namespace_env_app_live
    ON ddc_namespace_env_app_t0 (tenant_id, namespace_id, env_code, app_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_namespace_env_app_page
    ON ddc_namespace_env_app_t0 (tenant_id, namespace_id, env_code, app_id, id) WHERE deleted_at IS NULL;
CREATE INDEX ix_binding_app
    ON ddc_namespace_env_app_t0 (tenant_id, app_id, env_code, namespace_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uk_config_item_lifecycle
    ON ddc_config_item_t0 (tenant_id, biz_code, env, app_code, config_key, deleted_at);
CREATE UNIQUE INDEX uk_config_item_live
    ON ddc_config_item_t0 (tenant_id, biz_code, env, app_code, config_key) WHERE deleted_at IS NULL;
CREATE INDEX ix_config_item_page
    ON ddc_config_item_t0 (tenant_id, biz_code, env, app_code, config_key, id) WHERE deleted_at IS NULL;
CREATE INDEX ix_publish_task_page
    ON ddc_publish_task_t0 (tenant_id, created_at DESC, id DESC) WHERE deleted_at IS NULL;
CREATE INDEX ix_publish_recover
    ON ddc_publish_task_t0 (tenant_id, status, updated_at, id) WHERE deleted_at IS NULL;
CREATE INDEX ix_publish_ack_page
    ON ddc_publish_ack_t0 (tenant_id, change_id, instance_id, id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uk_instance_business
    ON ddc_instance_t0 (tenant_id, instance_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_instance_page
    ON ddc_instance_t0 (tenant_id, biz_code, env, app_code, instance_id, id) WHERE deleted_at IS NULL;
CREATE INDEX ix_instance_admission
    ON ddc_instance_t0 (tenant_id, resource_server_id, resource_version, admission_expires_at);
CREATE INDEX ix_operation_log_page
    ON ddc_operation_log_t0 (tenant_id, created_at DESC, id DESC) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uk_ai_resource_lifecycle
    ON ddc_ai_resource_t0 (tenant_id, biz_code, namespace_id, env_code, app_code, kind, resource_key, deleted_at);
CREATE UNIQUE INDEX uk_ai_resource_live
    ON ddc_ai_resource_t0 (tenant_id, biz_code, namespace_id, env_code, app_code, kind, resource_key)
    WHERE deleted_at IS NULL;
CREATE INDEX ix_ai_resource_page
    ON ddc_ai_resource_t0 (tenant_id, biz_code, namespace_id, env_code, app_code, kind, update_time DESC, id DESC)
    WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uk_ai_draft_node_lifecycle
    ON ddc_ai_draft_node_t0 (tenant_id, resource_id, path, deleted_at);
CREATE UNIQUE INDEX uk_ai_draft_node_live
    ON ddc_ai_draft_node_t0 (tenant_id, resource_id, path) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uk_ai_version_path
    ON ddc_ai_version_node_t0 (tenant_id, version_id, path);
CREATE INDEX ix_ai_publication_history
    ON ddc_ai_publication_t0 (tenant_id, resource_id, id DESC);

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
