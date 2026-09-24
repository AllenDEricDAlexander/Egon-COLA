-- ===========================================================================
-- 20260922_001 yuheng 空库基线 / empty-database baseline (db/egon-mp)
-- 49 张逻辑表由 ShardingSphere STANDARD_TENANT_ID 策略路由：
--   datasource yuheng_0 -> schema public -> 物理表 <logical_table>_t0（1 库 x 1 表）。
-- 仅建表与索引/约束；不含任何历史业务数据行。
-- 49 logical tables, each created as public.<logical_table>_t0 (1 x 1 topology).
-- DDL only: no legacy business rows are written by this script.
-- 数据回填、修复与历史迁移重放不在本脚本范围内 / backfill, repair and history
-- adoption are out of scope; legacy Flyway V1..V13 files stay untouched.
-- 受管角色分支 / managed migration role: the whole 49-table body runs only when the
--   starter runner sets egon_migration.role = SHARD (yuheng 采用 1 库 x 1 表，PRIMARY 以
--   RoleEnum.SHARD 登记)。其它角色一律 fail closed，绝不静默跳过建表。
--   Any other role raises instead of silently creating nothing.
-- pgvector 是部署侧前置条件，本脚本不再 CREATE EXTENSION：缺失时首个 vector 列会以
--   "type vector does not exist" 失败，这是一条可运维定位的 fail-closed 错误。
--   pgvector stays an operator-installed prerequisite, never created by the app role.
-- ===========================================================================

DO $egon$
BEGIN
    IF current_setting('egon_migration.role') = 'SHARD' THEN


-- ---------------------------------------------------------------------------
-- 01 gateway_group -> public.gateway_group_t0
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_group_t0 (
    id                  bigint          NOT NULL,
    tenant_id           bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id      varchar(128)    NOT NULL,
    create_time         timestamptz(6)  NOT NULL,
    update_user_id      varchar(128)    NOT NULL,
    update_time         timestamptz(6)  NOT NULL,
    deleted_at          timestamp(6),
    version             bigint          NOT NULL DEFAULT 0,
    gateway_group_code  varchar(128)    NOT NULL,
    display_name        varchar(256)    NOT NULL,
    env                 varchar(64)     NOT NULL,
    namespace           varchar(128)    NOT NULL,
    description         varchar(1024),
    enabled             boolean         NOT NULL DEFAULT TRUE,
    revision            bigint          NOT NULL DEFAULT 0,
    CONSTRAINT pk_group PRIMARY KEY (id),
    CONSTRAINT uq_group_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_group_life_1 UNIQUE (tenant_id, gateway_group_code, env, namespace, deleted_at)
);

CREATE UNIQUE INDEX uq_group_active_1
    ON public.gateway_group_t0 (tenant_id, gateway_group_code, env, namespace)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 02 gateway_application -> public.gateway_application_t0
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_application_t0 (
    id                  bigint          NOT NULL,
    tenant_id           bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id      varchar(128)    NOT NULL,
    create_time         timestamptz(6)  NOT NULL,
    update_user_id      varchar(128)    NOT NULL,
    update_time         timestamptz(6)  NOT NULL,
    deleted_at          timestamp(6),
    version             bigint          NOT NULL DEFAULT 0,
    application_code    varchar(128)    NOT NULL,
    display_name        varchar(256)    NOT NULL,
    env                 varchar(64)     NOT NULL,
    namespace           varchar(128)    NOT NULL,
    description         varchar(1024),
    revision            bigint          NOT NULL DEFAULT 0,
    biz_code            varchar(128)    NOT NULL,
    CONSTRAINT pk_application PRIMARY KEY (id),
    CONSTRAINT uq_application_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_application_life_1 UNIQUE (tenant_id, biz_code, application_code, env, deleted_at)
);

CREATE UNIQUE INDEX uq_application_active_1
    ON public.gateway_application_t0 (tenant_id, biz_code, application_code, env)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 03 gateway_application_credential -> public.gateway_application_credential_t0
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_application_credential_t0 (
    id                  bigint          NOT NULL,
    tenant_id           bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id      varchar(128)    NOT NULL,
    create_time         timestamptz(6)  NOT NULL,
    update_user_id      varchar(128)    NOT NULL,
    update_time         timestamptz(6)  NOT NULL,
    deleted_at          timestamp(6),
    version             bigint          NOT NULL DEFAULT 0,
    application_id      bigint          NOT NULL,
    access_key          varchar(128)    NOT NULL,
    secret_ciphertext   text,
    secret_reference    varchar(512),
    key_version         varchar(64)     NOT NULL,
    status              varchar(32)     NOT NULL,
    valid_from          timestamptz     NOT NULL,
    valid_until         timestamptz,
    CONSTRAINT pk_application_credential PRIMARY KEY (id),
    CONSTRAINT uq_application_credential_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_application_credential_intent_1 UNIQUE (tenant_id, access_key),
    CONSTRAINT ck_gateway_credential_secret
        CHECK ((secret_ciphertext IS NOT NULL) <> (secret_reference IS NOT NULL)),
    CONSTRAINT fk_application_credential_application FOREIGN KEY (tenant_id, application_id)
        REFERENCES public.gateway_application_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

-- ---------------------------------------------------------------------------
-- 04 gateway_hmac_nonce -> public.gateway_hmac_nonce_t0
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_hmac_nonce_t0 (
    id                  bigint          NOT NULL,
    tenant_id           bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id      varchar(128)    NOT NULL,
    create_time         timestamptz(6)  NOT NULL,
    update_user_id      varchar(128)    NOT NULL,
    update_time         timestamptz(6)  NOT NULL,
    deleted_at          timestamp(6),
    version             bigint          NOT NULL DEFAULT 0,
    access_key          varchar(128)    NOT NULL,
    nonce               varchar(256)    NOT NULL,
    expires_at          timestamptz     NOT NULL,
    CONSTRAINT pk_hmac_nonce PRIMARY KEY (id),
    CONSTRAINT uq_hmac_nonce_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_hmac_nonce_life_1 UNIQUE (tenant_id, access_key, nonce, deleted_at)
);

CREATE UNIQUE INDEX uq_hmac_nonce_active_1
    ON public.gateway_hmac_nonce_t0 (tenant_id, access_key, nonce)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_gateway_hmac_nonce_expiry
    ON public.gateway_hmac_nonce_t0 (tenant_id, expires_at)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 05 gateway_business_domain -> public.gateway_business_domain_t0
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_business_domain_t0 (
    id                  bigint          NOT NULL,
    tenant_id           bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id      varchar(128)    NOT NULL,
    create_time         timestamptz(6)  NOT NULL,
    update_user_id      varchar(128)    NOT NULL,
    update_time         timestamptz(6)  NOT NULL,
    deleted_at          timestamp(6),
    version             bigint          NOT NULL DEFAULT 0,
    application_id      bigint          NOT NULL,
    code                varchar(128)    NOT NULL,
    display_name        varchar(256)    NOT NULL,
    description         varchar(1024),
    CONSTRAINT pk_business_domain PRIMARY KEY (id),
    CONSTRAINT uq_business_domain_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_business_domain_life_1 UNIQUE (tenant_id, application_id, code, deleted_at),
    CONSTRAINT fk_business_domain_application FOREIGN KEY (tenant_id, application_id)
        REFERENCES public.gateway_application_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uq_business_domain_active_1
    ON public.gateway_business_domain_t0 (tenant_id, application_id, code)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 06 gateway_entity_domain -> public.gateway_entity_domain_t0
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_entity_domain_t0 (
    id                  bigint          NOT NULL,
    tenant_id           bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id      varchar(128)    NOT NULL,
    create_time         timestamptz(6)  NOT NULL,
    update_user_id      varchar(128)    NOT NULL,
    update_time         timestamptz(6)  NOT NULL,
    deleted_at          timestamp(6),
    version             bigint          NOT NULL DEFAULT 0,
    business_domain_id  bigint          NOT NULL,
    code                varchar(128)    NOT NULL,
    display_name        varchar(256)    NOT NULL,
    description         varchar(1024),
    CONSTRAINT pk_entity_domain PRIMARY KEY (id),
    CONSTRAINT uq_entity_domain_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_entity_domain_life_1 UNIQUE (tenant_id, business_domain_id, code, deleted_at),
    CONSTRAINT fk_entity_domain_business_domain FOREIGN KEY (tenant_id, business_domain_id)
        REFERENCES public.gateway_business_domain_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uq_entity_domain_active_1
    ON public.gateway_entity_domain_t0 (tenant_id, business_domain_id, code)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 07 gateway_interface_group -> public.gateway_interface_group_t0
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_interface_group_t0 (
    id                  bigint          NOT NULL,
    tenant_id           bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id      varchar(128)    NOT NULL,
    create_time         timestamptz(6)  NOT NULL,
    update_user_id      varchar(128)    NOT NULL,
    update_time         timestamptz(6)  NOT NULL,
    deleted_at          timestamp(6),
    version             bigint          NOT NULL DEFAULT 0,
    entity_domain_id    bigint          NOT NULL,
    code                varchar(256)    NOT NULL,
    display_name        varchar(256)    NOT NULL,
    source_type         varchar(32)     NOT NULL,
    class_name          varchar(512),
    description         varchar(1024),
    CONSTRAINT pk_interface_group PRIMARY KEY (id),
    CONSTRAINT uq_interface_group_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_interface_group_life_1 UNIQUE (tenant_id, entity_domain_id, code, deleted_at),
    CONSTRAINT fk_interface_group_entity_domain FOREIGN KEY (tenant_id, entity_domain_id)
        REFERENCES public.gateway_entity_domain_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uq_interface_group_active_1
    ON public.gateway_interface_group_t0 (tenant_id, entity_domain_id, code)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 08 gateway_operation -> public.gateway_operation_t0
-- current_definition_id 的反向指针在文件末尾的延期外键块中建立。
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_operation_t0 (
    id                       bigint          NOT NULL,
    tenant_id                bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id           varchar(128)    NOT NULL,
    create_time              timestamptz(6)  NOT NULL,
    update_user_id           varchar(128)    NOT NULL,
    update_time              timestamptz(6)  NOT NULL,
    deleted_at               timestamp(6),
    version                  bigint          NOT NULL DEFAULT 0,
    application_id           bigint          NOT NULL,
    interface_group_id       bigint          NOT NULL,
    operation_key            varchar(512)    NOT NULL,
    protocol                 varchar(32)     NOT NULL,
    method_identity          varchar(1024)   NOT NULL,
    external_accessible      boolean         NOT NULL DEFAULT FALSE,
    provider_service_identity jsonb          NOT NULL,
    source_type              varchar(32)     NOT NULL,
    lifecycle_status         varchar(32)     NOT NULL,
    current_definition_id    bigint,
    deprecated_at            timestamptz,
    revision                 bigint          NOT NULL DEFAULT 0,
    CONSTRAINT pk_operation PRIMARY KEY (id),
    CONSTRAINT uq_operation_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_operation_life_1 UNIQUE (tenant_id, application_id, operation_key, deleted_at),
    CONSTRAINT fk_operation_application FOREIGN KEY (tenant_id, application_id)
        REFERENCES public.gateway_application_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_operation_interface_group FOREIGN KEY (tenant_id, interface_group_id)
        REFERENCES public.gateway_interface_group_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uq_operation_active_1
    ON public.gateway_operation_t0 (tenant_id, application_id, operation_key)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 09 gateway_definition_set -> public.gateway_definition_set_t0
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_definition_set_t0 (
    id                  bigint          NOT NULL,
    tenant_id           bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id      varchar(128)    NOT NULL,
    create_time         timestamptz(6)  NOT NULL,
    update_user_id      varchar(128)    NOT NULL,
    update_time         timestamptz(6)  NOT NULL,
    deleted_at          timestamp(6),
    version             bigint          NOT NULL DEFAULT 0,
    application_id      bigint          NOT NULL,
    report_id           varchar(128)    NOT NULL,
    build_id            varchar(256)    NOT NULL,
    protocol            varchar(32)     NOT NULL,
    fingerprint         varchar(64)     NOT NULL,
    complete_set        boolean         NOT NULL,
    status              varchar(32)     NOT NULL,
    operation_count     integer         NOT NULL,
    accepted_count      integer         NOT NULL DEFAULT 0,
    conflict_count      integer         NOT NULL DEFAULT 0,
    received_at         timestamptz     NOT NULL,
    completed_at        timestamptz,
    activated_at        timestamptz,
    retired_at          timestamptz,
    CONSTRAINT pk_definition_set PRIMARY KEY (id),
    CONSTRAINT uq_definition_set_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_definition_set_intent_1
        UNIQUE (tenant_id, application_id, build_id, protocol, fingerprint),
    CONSTRAINT uq_definition_set_intent_2 UNIQUE (tenant_id, application_id, report_id),
    CONSTRAINT fk_definition_set_application FOREIGN KEY (tenant_id, application_id)
        REFERENCES public.gateway_application_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

-- ---------------------------------------------------------------------------
-- 10 gateway_operation_definition -> public.gateway_operation_definition_t0
-- payload JSON 列保存规范化 OpenAPI/RPC 结构快照，不参与业务查询条件。
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_operation_definition_t0 (
    id                  bigint          NOT NULL,
    tenant_id           bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id      varchar(128)    NOT NULL,
    create_time         timestamptz(6)  NOT NULL,
    update_user_id      varchar(128)    NOT NULL,
    update_time         timestamptz(6)  NOT NULL,
    deleted_at          timestamp(6),
    version             bigint          NOT NULL DEFAULT 0,
    operation_id        bigint          NOT NULL,
    definition_set_id   bigint,
    definition_version  bigint          NOT NULL,
    definition_sha256   varchar(64)     NOT NULL,
    summary             varchar(1024),
    tags                jsonb           NOT NULL DEFAULT '[]'::jsonb,
    request_schema      jsonb           NOT NULL,
    response_schema     jsonb           NOT NULL,
    error_schema        jsonb           NOT NULL DEFAULT '[]'::jsonb,
    descriptor_snapshot jsonb,
    attributes          jsonb           NOT NULL DEFAULT '{}'::jsonb,
    external_accessible boolean         NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_operation_definition PRIMARY KEY (id),
    CONSTRAINT uq_operation_definition_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_operation_definition_intent_1 UNIQUE (tenant_id, operation_id, definition_version),
    CONSTRAINT uq_operation_definition_intent_2 UNIQUE (tenant_id, operation_id, definition_sha256),
    CONSTRAINT fk_operation_definition_operation FOREIGN KEY (tenant_id, operation_id)
        REFERENCES public.gateway_operation_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_operation_definition_definition_set FOREIGN KEY (tenant_id, definition_set_id)
        REFERENCES public.gateway_definition_set_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

-- ---------------------------------------------------------------------------
-- 11 gateway_definition_set_operation -> public.gateway_definition_set_operation_t0
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_definition_set_operation_t0 (
    id                       bigint          NOT NULL,
    tenant_id                bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id           varchar(128)    NOT NULL,
    create_time              timestamptz(6)  NOT NULL,
    update_user_id           varchar(128)    NOT NULL,
    update_time              timestamptz(6)  NOT NULL,
    deleted_at               timestamp(6),
    version                  bigint          NOT NULL DEFAULT 0,
    definition_set_id        bigint          NOT NULL,
    operation_id             bigint          NOT NULL,
    definition_id            bigint          NOT NULL,
    method_identity          varchar(1024)   NOT NULL,
    provider_service_identity jsonb          NOT NULL,
    external_accessible      boolean         NOT NULL,
    deprecated               boolean         NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_definition_set_operation PRIMARY KEY (id),
    CONSTRAINT uq_definition_set_operation_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_definition_set_operation_intent_1
        UNIQUE (tenant_id, definition_set_id, operation_id),
    CONSTRAINT fk_definition_set_operation_definition_set
        FOREIGN KEY (tenant_id, definition_set_id)
        REFERENCES public.gateway_definition_set_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_definition_set_operation_operation FOREIGN KEY (tenant_id, operation_id)
        REFERENCES public.gateway_operation_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_definition_set_operation_operation_definition
        FOREIGN KEY (tenant_id, definition_id)
        REFERENCES public.gateway_operation_definition_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE INDEX idx_gateway_definition_membership_operation
    ON public.gateway_definition_set_operation_t0 (tenant_id, operation_id, definition_set_id)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 12 gateway_draft -> public.gateway_draft_t0
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_draft_t0 (
    id                  bigint          NOT NULL,
    tenant_id           bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id      varchar(128)    NOT NULL,
    create_time         timestamptz(6)  NOT NULL,
    update_user_id      varchar(128)    NOT NULL,
    update_time         timestamptz(6)  NOT NULL,
    deleted_at          timestamp(6),
    version             bigint          NOT NULL DEFAULT 0,
    gateway_group_id    bigint          NOT NULL,
    revision            bigint          NOT NULL DEFAULT 0,
    based_on_release_id varchar(64),
    status              varchar(32)     NOT NULL,
    change_summary      varchar(1024),
    CONSTRAINT pk_draft PRIMARY KEY (id),
    CONSTRAINT uq_draft_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_draft_life_1 UNIQUE (tenant_id, gateway_group_id, deleted_at),
    CONSTRAINT fk_draft_group FOREIGN KEY (tenant_id, gateway_group_id)
        REFERENCES public.gateway_group_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uq_draft_active_1
    ON public.gateway_draft_t0 (tenant_id, gateway_group_id)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 13 gateway_route_draft -> public.gateway_route_draft_t0
-- route_content 是发布态路由配置的 JSON 载荷，按原 Service 结构校验。
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_route_draft_t0 (
    id                  bigint          NOT NULL,
    tenant_id           bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id      varchar(128)    NOT NULL,
    create_time         timestamptz(6)  NOT NULL,
    update_user_id      varchar(128)    NOT NULL,
    update_time         timestamptz(6)  NOT NULL,
    deleted_at          timestamp(6),
    version             bigint          NOT NULL DEFAULT 0,
    gateway_group_id    bigint          NOT NULL,
    route_id            varchar(128)    NOT NULL,
    operation_id        bigint          NOT NULL,
    route_content       jsonb           NOT NULL,
    enabled             boolean         NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_route_draft PRIMARY KEY (id),
    CONSTRAINT uq_route_draft_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_route_draft_life_1
        UNIQUE (tenant_id, gateway_group_id, route_id, deleted_at),
    CONSTRAINT fk_route_draft_group FOREIGN KEY (tenant_id, gateway_group_id)
        REFERENCES public.gateway_group_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_route_draft_operation FOREIGN KEY (tenant_id, operation_id)
        REFERENCES public.gateway_operation_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uq_route_draft_active_1
    ON public.gateway_route_draft_t0 (tenant_id, gateway_group_id, route_id)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 14 gateway_policy_draft -> public.gateway_policy_draft_t0
-- policy_content 是策略实例的 JSON 载荷，按原 Service 结构校验。
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_policy_draft_t0 (
    id                  bigint          NOT NULL,
    tenant_id           bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id      varchar(128)    NOT NULL,
    create_time         timestamptz(6)  NOT NULL,
    update_user_id      varchar(128)    NOT NULL,
    update_time         timestamptz(6)  NOT NULL,
    deleted_at          timestamp(6),
    version             bigint          NOT NULL DEFAULT 0,
    gateway_group_id    bigint          NOT NULL,
    policy_id           varchar(128)    NOT NULL,
    policy_type         varchar(64)     NOT NULL,
    policy_scope        varchar(64)     NOT NULL,
    policy_content      jsonb           NOT NULL,
    enabled             boolean         NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_policy_draft PRIMARY KEY (id),
    CONSTRAINT uq_policy_draft_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_policy_draft_life_1
        UNIQUE (tenant_id, gateway_group_id, policy_id, deleted_at),
    CONSTRAINT fk_policy_draft_group FOREIGN KEY (tenant_id, gateway_group_id)
        REFERENCES public.gateway_group_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uq_policy_draft_active_1
    ON public.gateway_policy_draft_t0 (tenant_id, gateway_group_id, policy_id)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 15 gateway_release -> public.gateway_release_t0
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_release_t0 (
    id                     bigint          NOT NULL,
    tenant_id              bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id         varchar(128)    NOT NULL,
    create_time            timestamptz(6)  NOT NULL,
    update_user_id         varchar(128)    NOT NULL,
    update_time            timestamptz(6)  NOT NULL,
    deleted_at             timestamp(6),
    version                bigint          NOT NULL DEFAULT 0,
    gateway_group_id       bigint          NOT NULL,
    draft_revision         bigint          NOT NULL,
    based_on_release_id    varchar(64),
    rollback_of_release_id varchar(64),
    status                 varchar(32)     NOT NULL,
    partial_applied        boolean         NOT NULL DEFAULT FALSE,
    change_id              varchar(128),
    validation_report      jsonb           NOT NULL,
    structured_diff        jsonb           NOT NULL,
    change_reason          varchar(1024)   NOT NULL,
    CONSTRAINT pk_release PRIMARY KEY (id),
    CONSTRAINT uq_release_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_release_group FOREIGN KEY (tenant_id, gateway_group_id)
        REFERENCES public.gateway_group_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE INDEX idx_gateway_release_group_created
    ON public.gateway_release_t0 (tenant_id, gateway_group_id, create_time DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_gateway_release_status
    ON public.gateway_release_t0 (tenant_id, status)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 16 gateway_release_content -> public.gateway_release_content_t0
-- canonical_snapshot/activation_content/chunk_manifest 为大块 JSON 载荷。
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_release_content_t0 (
    id                  bigint          NOT NULL,
    tenant_id           bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id      varchar(128)    NOT NULL,
    create_time         timestamptz(6)  NOT NULL,
    update_user_id      varchar(128)    NOT NULL,
    update_time         timestamptz(6)  NOT NULL,
    deleted_at          timestamp(6),
    version             bigint          NOT NULL DEFAULT 0,
    release_id          bigint          NOT NULL,
    rule_content_sha256 varchar(64)     NOT NULL,
    artifact_sha256     varchar(64)     NOT NULL,
    canonical_snapshot  jsonb           NOT NULL,
    activation_content  jsonb           NOT NULL,
    chunk_manifest      jsonb           NOT NULL,
    snapshot_size       bigint          NOT NULL,
    CONSTRAINT pk_release_content PRIMARY KEY (id),
    CONSTRAINT uq_release_content_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_release_content_intent_1 UNIQUE (tenant_id, release_id),
    CONSTRAINT fk_release_content_release FOREIGN KEY (tenant_id, release_id)
        REFERENCES public.gateway_release_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE INDEX idx_gateway_release_content_rule_sha
    ON public.gateway_release_content_t0 (tenant_id, rule_content_sha256)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_gateway_release_content_artifact_sha
    ON public.gateway_release_content_t0 (tenant_id, artifact_sha256)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 17 gateway_release_attempt -> public.gateway_release_attempt_t0
-- attempt_no 放宽为 bigint：gateway_release_target/publication 的复合外键
-- 指向本列，而它们的 PO 类型是 Long；不放宽则基线无法建立。
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_release_attempt_t0 (
    id                  bigint          NOT NULL,
    tenant_id           bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id      varchar(128)    NOT NULL,
    create_time         timestamptz(6)  NOT NULL,
    update_user_id      varchar(128)    NOT NULL,
    update_time         timestamptz(6)  NOT NULL,
    deleted_at          timestamp(6),
    version             bigint          NOT NULL DEFAULT 0,
    release_id          bigint          NOT NULL,
    attempt_no          bigint          NOT NULL,
    status              varchar(32)     NOT NULL,
    change_id           varchar(128),
    lease_owner         varchar(128),
    lease_until         timestamptz,
    started_at          timestamptz     NOT NULL,
    completed_at        timestamptz,
    error_code          varchar(128),
    error_message       varchar(1024),
    CONSTRAINT pk_release_attempt PRIMARY KEY (id),
    CONSTRAINT uq_release_attempt_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_release_attempt_intent_1 UNIQUE (tenant_id, release_id, attempt_no),
    CONSTRAINT fk_release_attempt_release FOREIGN KEY (tenant_id, release_id)
        REFERENCES public.gateway_release_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

-- ---------------------------------------------------------------------------
-- 18 gateway_release_target -> public.gateway_release_target_t0
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_release_target_t0 (
    id                      bigint          NOT NULL,
    tenant_id               bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id          varchar(128)    NOT NULL,
    create_time             timestamptz(6)  NOT NULL,
    update_user_id          varchar(128)    NOT NULL,
    update_time             timestamptz(6)  NOT NULL,
    deleted_at              timestamp(6),
    version                 bigint          NOT NULL DEFAULT 0,
    release_id              bigint          NOT NULL,
    attempt_no              bigint          NOT NULL,
    instance_id             varchar(256)    NOT NULL,
    lease_id                varchar(256)    NOT NULL,
    status                  varchar(32)     NOT NULL,
    applied_version         bigint,
    applied_artifact_sha256 varchar(64),
    error_code              varchar(128),
    observed_at             timestamptz     NOT NULL,
    engine_role             varchar(32),
    CONSTRAINT pk_release_target PRIMARY KEY (id),
    CONSTRAINT uq_release_target_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_release_target_intent_1
        UNIQUE (tenant_id, release_id, attempt_no, instance_id, lease_id),
    CONSTRAINT ck_gateway_release_target_role
        CHECK (engine_role IS NULL OR engine_role IN ('API_RPC', 'MCP')),
    CONSTRAINT fk_release_target_release_attempt
        FOREIGN KEY (tenant_id, release_id, attempt_no)
        REFERENCES public.gateway_release_attempt_t0 (tenant_id, release_id, attempt_no)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

-- ---------------------------------------------------------------------------
-- 19 gateway_release_publication -> public.gateway_release_publication_t0
-- content_value 是下发给引擎的单个配置键原文，长度按原合同不设上限。
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_release_publication_t0 (
    id                  bigint          NOT NULL,
    tenant_id           bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id      varchar(128)    NOT NULL,
    create_time         timestamptz(6)  NOT NULL,
    update_user_id      varchar(128)    NOT NULL,
    update_time         timestamptz(6)  NOT NULL,
    deleted_at          timestamp(6),
    version             bigint          NOT NULL DEFAULT 0,
    release_id          bigint          NOT NULL,
    attempt_no          bigint          NOT NULL CHECK (attempt_no > 0),
    phase_order         integer         NOT NULL CHECK (phase_order >= 0),
    phase_type          varchar(32)     NOT NULL
                        CHECK (phase_type IN ('CHUNK', 'ACTIVATION')),
    config_key          varchar(512)    NOT NULL,
    content_value       text            NOT NULL,
    content_sha256      varchar(64)     NOT NULL CHECK (length(content_sha256) = 64),
    expected_version    bigint,
    change_id           varchar(128)    NOT NULL,
    ddc_target_version  bigint,
    ddc_status          varchar(32)     NOT NULL
                        CHECK (ddc_status IN (
                            'PLANNED', 'RESOLVED', 'SUBMITTED', 'SUCCESS', 'FAILED',
                            'PARTIAL_SUCCESS', 'TIMEOUT', 'UNKNOWN'
                        )),
    error_code          varchar(128),
    error_message       varchar(1024),
    target_role         varchar(32),
    target_biz_code     varchar(128),
    target_env          varchar(64),
    target_app_code     varchar(128),
    CONSTRAINT pk_release_publication PRIMARY KEY (id),
    CONSTRAINT uq_release_publication_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_release_publication_intent_1
        UNIQUE (tenant_id, release_id, attempt_no, phase_order),
    CONSTRAINT uq_release_publication_intent_2 UNIQUE (tenant_id, change_id),
    CONSTRAINT uq_release_publication_intent_3
        UNIQUE (tenant_id, release_id, attempt_no, target_role, config_key),
    CONSTRAINT ck_gateway_publication_ddc_expected
        CHECK (ddc_status = 'PLANNED' OR expected_version IS NOT NULL),
    CONSTRAINT ck_gateway_publication_ddc_target_version
        CHECK (ddc_status <> 'SUCCESS' OR ddc_target_version IS NOT NULL),
    CONSTRAINT ck_gateway_publication_target
        CHECK ((target_role IS NULL AND target_biz_code IS NULL
                    AND target_env IS NULL AND target_app_code IS NULL)
                OR (target_role IN ('API_RPC', 'MCP')
                    AND target_role IS NOT NULL
                    AND target_biz_code IS NOT NULL AND length(trim(target_biz_code)) > 0
                    AND target_env IS NOT NULL AND length(trim(target_env)) > 0
                    AND target_app_code IS NOT NULL AND length(trim(target_app_code)) > 0)),
    CONSTRAINT fk_release_publication_release_attempt
        FOREIGN KEY (tenant_id, release_id, attempt_no)
        REFERENCES public.gateway_release_attempt_t0 (tenant_id, release_id, attempt_no)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE INDEX idx_gateway_release_publication_incomplete
    ON public.gateway_release_publication_t0 (tenant_id, release_id, attempt_no, phase_order)
    WHERE ddc_status <> 'SUCCESS' AND deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 20 gateway_idempotency_record -> public.gateway_idempotency_record_t0
-- response_content 缓存首次成功响应体，用于同键重放返回一致结果。
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_idempotency_record_t0 (
    id                  bigint          NOT NULL,
    tenant_id           bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id      varchar(128)    NOT NULL,
    create_time         timestamptz(6)  NOT NULL,
    update_user_id      varchar(128)    NOT NULL,
    update_time         timestamptz(6)  NOT NULL,
    deleted_at          timestamp(6),
    version             bigint          NOT NULL DEFAULT 0,
    scope_type          varchar(64)     NOT NULL,
    scope_id            varchar(128)    NOT NULL,
    idempotency_key     varchar(256)    NOT NULL,
    payload_sha256      varchar(64)     NOT NULL,
    resource_id         varchar(128)    NOT NULL,
    response_content    jsonb           NOT NULL,
    expires_at          timestamptz,
    CONSTRAINT pk_idempotency_record PRIMARY KEY (id),
    CONSTRAINT uq_idempotency_record_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_idempotency_record_intent_1
        UNIQUE (tenant_id, scope_type, scope_id, idempotency_key)
);

-- ---------------------------------------------------------------------------
-- 21 gateway_audit_log -> public.gateway_audit_log_t0
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_audit_log_t0 (
    id                  bigint          NOT NULL,
    tenant_id           bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id      varchar(128)    NOT NULL,
    create_time         timestamptz(6)  NOT NULL,
    update_user_id      varchar(128)    NOT NULL,
    update_time         timestamptz(6)  NOT NULL,
    deleted_at          timestamp(6),
    version             bigint          NOT NULL DEFAULT 0,
    actor_id            varchar(128)    NOT NULL,
    actor_type          varchar(32)     NOT NULL,
    source              varchar(64)     NOT NULL,
    request_id          varchar(128),
    trace_id            varchar(128),
    resource_type       varchar(64)     NOT NULL,
    resource_id         varchar(128)    NOT NULL,
    action              varchar(128)    NOT NULL,
    before_summary      jsonb,
    after_summary       jsonb,
    draft_revision      bigint,
    release_id          varchar(64),
    successful          boolean         NOT NULL,
    error_code          varchar(128),
    occurred_at         timestamptz     NOT NULL,
    CONSTRAINT pk_audit_log PRIMARY KEY (id),
    CONSTRAINT uq_audit_log_tenant_id UNIQUE (tenant_id, id)
);

CREATE INDEX idx_gateway_audit_resource
    ON public.gateway_audit_log_t0 (tenant_id, resource_type, resource_id, occurred_at DESC)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 22 gateway_call_event_summary -> public.gateway_call_event_summary_t0
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_call_event_summary_t0 (
    id                  bigint          NOT NULL,
    tenant_id           bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id      varchar(128)    NOT NULL,
    create_time         timestamptz(6)  NOT NULL,
    update_user_id      varchar(128)    NOT NULL,
    update_time         timestamptz(6)  NOT NULL,
    deleted_at          timestamp(6),
    version             bigint          NOT NULL DEFAULT 0,
    event_id            varchar(64)     NOT NULL,
    trace_id            varchar(32)     NOT NULL,
    occurred_at         timestamptz     NOT NULL,
    completed_at        timestamptz     NOT NULL,
    duration_ms         bigint          NOT NULL,
    protocol            varchar(16)     NOT NULL,
    access_zone         varchar(16)     NOT NULL,
    env                 varchar(64)     NOT NULL,
    namespace           varchar(128)    NOT NULL,
    gateway_group_id    varchar(64),
    operation_id        varchar(128),
    route_id            varchar(128),
    result_category     varchar(32)     NOT NULL,
    gateway_error_code  varchar(128),
    http_status         integer,
    grpc_status         varchar(64),
    engine_node_id      varchar(256)    NOT NULL,
    provider_service    varchar(512),
    attempt_count       integer         NOT NULL,
    expires_at          timestamptz     NOT NULL,
    CONSTRAINT pk_call_event_summary PRIMARY KEY (id),
    CONSTRAINT uq_call_event_summary_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_call_event_summary_intent_1 UNIQUE (tenant_id, event_id)
);

CREATE INDEX idx_gateway_call_trace
    ON public.gateway_call_event_summary_t0 (tenant_id, trace_id, occurred_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_gateway_call_scope_time
    ON public.gateway_call_event_summary_t0 (tenant_id, env, namespace, occurred_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_gateway_call_group_time
    ON public.gateway_call_event_summary_t0 (tenant_id, gateway_group_id, occurred_at DESC)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 23 gateway_call_metric_minute -> public.gateway_call_metric_minute_t0
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_call_metric_minute_t0 (
    id                  bigint          NOT NULL,
    tenant_id           bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id      varchar(128)    NOT NULL,
    create_time         timestamptz(6)  NOT NULL,
    update_user_id      varchar(128)    NOT NULL,
    update_time         timestamptz(6)  NOT NULL,
    deleted_at          timestamp(6),
    version             bigint          NOT NULL DEFAULT 0,
    bucket_at           timestamptz     NOT NULL,
    env                 varchar(64)     NOT NULL,
    namespace           varchar(128)    NOT NULL,
    protocol            varchar(16)     NOT NULL,
    gateway_group_id    varchar(64)     NOT NULL DEFAULT '',
    request_count       bigint          NOT NULL,
    error_count         bigint          NOT NULL,
    duration_total_ms   bigint          NOT NULL,
    duration_max_ms     bigint          NOT NULL,
    CONSTRAINT pk_call_metric_minute PRIMARY KEY (id),
    CONSTRAINT uq_call_metric_minute_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_call_metric_minute_intent_1
        UNIQUE (tenant_id, bucket_at, env, namespace, protocol, gateway_group_id)
);

CREATE INDEX idx_gateway_call_metric_scope_time
    ON public.gateway_call_metric_minute_t0 (tenant_id, env, namespace, bucket_at DESC)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 24 gateway_call_event_consume_failure -> public.gateway_call_event_consume_failure_t0
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_call_event_consume_failure_t0 (
    id                  bigint          NOT NULL,
    tenant_id           bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id      varchar(128)    NOT NULL,
    create_time         timestamptz(6)  NOT NULL,
    update_user_id      varchar(128)    NOT NULL,
    update_time         timestamptz(6)  NOT NULL,
    deleted_at          timestamp(6),
    version             bigint          NOT NULL DEFAULT 0,
    topic               varchar(256)    NOT NULL,
    partition_no        integer         NOT NULL,
    offset_no           bigint          NOT NULL,
    event_id            varchar(64),
    failure_code        varchar(128)    NOT NULL,
    failure_message     varchar(1024),
    payload_sha256      varchar(64)     NOT NULL,
    payload_size        integer         NOT NULL,
    occurred_at         timestamptz     NOT NULL,
    CONSTRAINT pk_call_event_consume_failure PRIMARY KEY (id),
    CONSTRAINT uq_call_event_consume_failure_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_call_event_consume_failure_intent_1
        UNIQUE (tenant_id, topic, partition_no, offset_no)
);

CREATE INDEX idx_gateway_call_failure_time
    ON public.gateway_call_event_consume_failure_t0 (tenant_id, occurred_at DESC)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 25 gateway_mcp_server -> public.gateway_mcp_server_t0
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_mcp_server_t0 (
    id                      bigint          NOT NULL,
    tenant_id               bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id          varchar(128)    NOT NULL,
    create_time             timestamptz(6)  NOT NULL,
    update_user_id          varchar(128)    NOT NULL,
    update_time             timestamptz(6)  NOT NULL,
    deleted_at              timestamp(6),
    version                 bigint          NOT NULL DEFAULT 0,
    gateway_group_id        bigint          NOT NULL,
    server_code             varchar(128)    NOT NULL,
    display_name            varchar(256)    NOT NULL,
    description             varchar(2048),
    instructions            text,
    dialects                jsonb           NOT NULL DEFAULT '[]'::jsonb,
    resource_uri            varchar(256)    NOT NULL,
    list_cache_ttl_seconds  bigint          NOT NULL DEFAULT 30,
    enabled                 boolean         NOT NULL DEFAULT TRUE,
    revision                bigint          NOT NULL DEFAULT 0,
    CONSTRAINT pk_mcp_server PRIMARY KEY (id),
    CONSTRAINT uq_mcp_server_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_mcp_server_life_1
        UNIQUE (tenant_id, gateway_group_id, server_code, deleted_at),
    CONSTRAINT ck_gateway_mcp_server_dialects CHECK (jsonb_typeof(dialects) = 'array'),
    CONSTRAINT ck_gateway_mcp_server_cache_ttl CHECK (list_cache_ttl_seconds >= 0),
    CONSTRAINT ck_gateway_mcp_server_revision CHECK (revision >= 0),
    CONSTRAINT fk_mcp_server_group FOREIGN KEY (tenant_id, gateway_group_id)
        REFERENCES public.gateway_group_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uq_mcp_server_active_1
    ON public.gateway_mcp_server_t0 (tenant_id, gateway_group_id, server_code)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_gateway_mcp_server_group
    ON public.gateway_mcp_server_t0 (tenant_id, gateway_group_id, enabled, server_code)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 26 gateway_mcp_app_artifact -> public.gateway_mcp_app_artifact_t0
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_mcp_app_artifact_t0 (
    id                      bigint          NOT NULL,
    tenant_id               bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id          varchar(128)    NOT NULL,
    create_time             timestamptz(6)  NOT NULL,
    update_user_id          varchar(128)    NOT NULL,
    update_time             timestamptz(6)  NOT NULL,
    deleted_at              timestamp(6),
    version                 bigint          NOT NULL DEFAULT 0,
    gateway_group_id        bigint          NOT NULL,
    app_code                varchar(128)    NOT NULL,
    app_version             varchar(64)     NOT NULL,
    display_name            varchar(256)    NOT NULL,
    resource_uri            varchar(1024)   NOT NULL,
    artifact_reference      varchar(1024)   NOT NULL,
    artifact_sha256         varchar(64)     NOT NULL,
    size_bytes              bigint          NOT NULL,
    mime_type               varchar(128)    NOT NULL,
    content_security_policy text            NOT NULL,
    permission_manifest     jsonb           NOT NULL DEFAULT '[]'::jsonb,
    allowed_origins         jsonb           NOT NULL DEFAULT '[]'::jsonb,
    status                  varchar(32)     NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT pk_mcp_app_artifact PRIMARY KEY (id),
    CONSTRAINT uq_mcp_app_artifact_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_mcp_app_artifact_intent_1 UNIQUE (tenant_id, app_code, app_version),
    CONSTRAINT uq_mcp_app_artifact_intent_2 UNIQUE (tenant_id, resource_uri),
    CONSTRAINT ck_gateway_mcp_app_sha CHECK (length(artifact_sha256) = 64),
    CONSTRAINT ck_gateway_mcp_app_size
        CHECK (size_bytes >= 0 AND size_bytes <= 16777216),
    CONSTRAINT ck_gateway_mcp_app_mime CHECK (mime_type = 'text/html;profile=mcp-app'),
    CONSTRAINT ck_gateway_mcp_app_status CHECK (status IN ('ACTIVE', 'REVOKED')),
    CONSTRAINT ck_gateway_mcp_app_permissions
        CHECK (jsonb_typeof(permission_manifest) = 'array'),
    CONSTRAINT ck_gateway_mcp_app_origins CHECK (jsonb_typeof(allowed_origins) = 'array'),
    CONSTRAINT fk_mcp_app_artifact_group FOREIGN KEY (tenant_id, gateway_group_id)
        REFERENCES public.gateway_group_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE INDEX idx_gateway_mcp_app_group_status
    ON public.gateway_mcp_app_artifact_t0 (tenant_id, gateway_group_id, status, create_time DESC)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 27 gateway_mcp_remote_provider -> public.gateway_mcp_remote_provider_t0
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_mcp_remote_provider_t0 (
    id                      bigint          NOT NULL,
    tenant_id               bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id          varchar(128)    NOT NULL,
    create_time             timestamptz(6)  NOT NULL,
    update_user_id          varchar(128)    NOT NULL,
    update_time             timestamptz(6)  NOT NULL,
    deleted_at              timestamp(6),
    version                 bigint          NOT NULL DEFAULT 0,
    gateway_group_id        bigint          NOT NULL,
    provider_code           varchar(128)    NOT NULL,
    display_name            varchar(256)    NOT NULL,
    dialect                 varchar(32)     NOT NULL,
    transport_type          varchar(32)     NOT NULL,
    endpoint_reference      varchar(1024)   NOT NULL,
    auth_profile_reference  varchar(512),
    tls_profile_reference   varchar(512),
    capability_fingerprint  varchar(128),
    status                  varchar(32)     NOT NULL DEFAULT 'CONFIGURED',
    enabled                 boolean         NOT NULL DEFAULT TRUE,
    revision                bigint          NOT NULL DEFAULT 0,
    CONSTRAINT pk_mcp_remote_provider PRIMARY KEY (id),
    CONSTRAINT uq_mcp_remote_provider_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_mcp_remote_provider_life_1
        UNIQUE (tenant_id, gateway_group_id, provider_code, deleted_at),
    CONSTRAINT ck_gateway_mcp_provider_dialect
        CHECK (dialect IN ('STABLE_2025_11_25', 'RC_2026_07_28', 'LEGACY_2024_SSE')),
    CONSTRAINT ck_gateway_mcp_provider_transport
        CHECK (transport_type IN ('STREAMABLE_HTTP', 'LEGACY_SSE', 'STDIO_MANAGED')),
    CONSTRAINT ck_gateway_mcp_provider_status
        CHECK (status IN ('CONFIGURED', 'SYNCED', 'DEGRADED', 'DISABLED')),
    CONSTRAINT ck_gateway_mcp_provider_revision CHECK (revision >= 0),
    CONSTRAINT fk_mcp_remote_provider_group FOREIGN KEY (tenant_id, gateway_group_id)
        REFERENCES public.gateway_group_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uq_mcp_remote_provider_active_1
    ON public.gateway_mcp_remote_provider_t0 (tenant_id, gateway_group_id, provider_code)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_gateway_mcp_provider_group_status
    ON public.gateway_mcp_remote_provider_t0 (tenant_id, gateway_group_id, status, enabled)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 28 gateway_mcp_remote_capability -> public.gateway_mcp_remote_capability_t0
-- descriptor 保存上游原始能力声明，只用于展示与差异比对。
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_mcp_remote_capability_t0 (
    id                      bigint          NOT NULL,
    tenant_id               bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id          varchar(128)    NOT NULL,
    create_time             timestamptz(6)  NOT NULL,
    update_user_id          varchar(128)    NOT NULL,
    update_time             timestamptz(6)  NOT NULL,
    deleted_at              timestamp(6),
    version                 bigint          NOT NULL DEFAULT 0,
    provider_id             bigint          NOT NULL,
    primitive_type          varchar(32)     NOT NULL,
    remote_name             varchar(512)    NOT NULL,
    descriptor              jsonb           NOT NULL DEFAULT '{}'::jsonb,
    capability_fingerprint  varchar(128)    NOT NULL,
    synced_at               timestamptz     NOT NULL,
    CONSTRAINT pk_mcp_remote_capability PRIMARY KEY (id),
    CONSTRAINT uq_mcp_remote_capability_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_mcp_remote_capability_life_1
        UNIQUE (tenant_id, provider_id, primitive_type, remote_name, deleted_at),
    CONSTRAINT ck_gateway_mcp_remote_primitive
        CHECK (primitive_type IN ('TOOL', 'RESOURCE', 'RESOURCE_TEMPLATE', 'PROMPT', 'APP')),
    CONSTRAINT ck_gateway_mcp_remote_descriptor
        CHECK (jsonb_typeof(descriptor) = 'object'),
    CONSTRAINT fk_mcp_remote_capability_provider FOREIGN KEY (tenant_id, provider_id)
        REFERENCES public.gateway_mcp_remote_provider_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uq_mcp_remote_capability_active_1
    ON public.gateway_mcp_remote_capability_t0
        (tenant_id, provider_id, primitive_type, remote_name)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_gateway_mcp_remote_capability_fingerprint
    ON public.gateway_mcp_remote_capability_t0 (tenant_id, provider_id, capability_fingerprint)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 29 gateway_mcp_remote_mount_draft -> public.gateway_mcp_remote_mount_draft_t0
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_mcp_remote_mount_draft_t0 (
    id                      bigint          NOT NULL,
    tenant_id               bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id          varchar(128)    NOT NULL,
    create_time             timestamptz(6)  NOT NULL,
    update_user_id          varchar(128)    NOT NULL,
    update_time             timestamptz(6)  NOT NULL,
    deleted_at              timestamp(6),
    version                 bigint          NOT NULL DEFAULT 0,
    gateway_group_id        bigint          NOT NULL,
    server_id               bigint          NOT NULL,
    provider_id             bigint          NOT NULL,
    namespace               varchar(256)    NOT NULL,
    capability_fingerprint  varchar(128)    NOT NULL,
    content                 jsonb           NOT NULL DEFAULT '{}'::jsonb,
    enabled                 boolean         NOT NULL DEFAULT TRUE,
    revision                bigint          NOT NULL DEFAULT 0,
    CONSTRAINT pk_mcp_remote_mount_draft PRIMARY KEY (id),
    CONSTRAINT uq_mcp_remote_mount_draft_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_mcp_remote_mount_draft_life_1
        UNIQUE (tenant_id, server_id, namespace, deleted_at),
    CONSTRAINT ck_gateway_mcp_mount_content CHECK (jsonb_typeof(content) = 'object'),
    CONSTRAINT ck_gateway_mcp_mount_revision CHECK (revision >= 0),
    CONSTRAINT fk_mcp_remote_mount_draft_group FOREIGN KEY (tenant_id, gateway_group_id)
        REFERENCES public.gateway_group_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_mcp_remote_mount_draft_server FOREIGN KEY (tenant_id, server_id)
        REFERENCES public.gateway_mcp_server_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_mcp_remote_mount_draft_provider FOREIGN KEY (tenant_id, provider_id)
        REFERENCES public.gateway_mcp_remote_provider_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uq_mcp_remote_mount_draft_active_1
    ON public.gateway_mcp_remote_mount_draft_t0 (tenant_id, server_id, namespace)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_gateway_mcp_mount_group_provider
    ON public.gateway_mcp_remote_mount_draft_t0 (tenant_id, gateway_group_id, provider_id, enabled)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 30 gateway_mcp_resource_draft -> public.gateway_mcp_resource_draft_t0
-- content 是驱动无关的草稿载荷，driver_type 决定其内部结构。
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_mcp_resource_draft_t0 (
    id                  bigint          NOT NULL,
    tenant_id           bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id      varchar(128)    NOT NULL,
    create_time         timestamptz(6)  NOT NULL,
    update_user_id      varchar(128)    NOT NULL,
    update_time         timestamptz(6)  NOT NULL,
    deleted_at          timestamp(6),
    version             bigint          NOT NULL DEFAULT 0,
    gateway_group_id    bigint          NOT NULL,
    server_id           bigint          NOT NULL,
    resource_name       varchar(256)    NOT NULL,
    resource_uri        varchar(1024)   NOT NULL,
    driver_type         varchar(32)     NOT NULL,
    operation_id        bigint,
    remote_mount_id     bigint,
    content             jsonb           NOT NULL DEFAULT '{}'::jsonb,
    enabled             boolean         NOT NULL DEFAULT TRUE,
    revision            bigint          NOT NULL DEFAULT 0,
    CONSTRAINT pk_mcp_resource_draft PRIMARY KEY (id),
    CONSTRAINT uq_mcp_resource_draft_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_mcp_resource_draft_life_1
        UNIQUE (tenant_id, server_id, resource_name, deleted_at),
    CONSTRAINT uq_mcp_resource_draft_life_2
        UNIQUE (tenant_id, server_id, resource_uri, deleted_at),
    CONSTRAINT ck_gateway_mcp_resource_driver
        CHECK (driver_type IN ('STATIC_TEXT', 'STATIC_BLOB', 'LOCAL_OPERATION',
                               'OBJECT_STORAGE', 'DATABASE_SCHEMA', 'APP_UI', 'REMOTE_MCP')),
    CONSTRAINT ck_gateway_mcp_resource_content CHECK (jsonb_typeof(content) = 'object'),
    CONSTRAINT ck_gateway_mcp_resource_revision CHECK (revision >= 0),
    CONSTRAINT fk_mcp_resource_draft_group FOREIGN KEY (tenant_id, gateway_group_id)
        REFERENCES public.gateway_group_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_mcp_resource_draft_server FOREIGN KEY (tenant_id, server_id)
        REFERENCES public.gateway_mcp_server_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_mcp_resource_draft_operation FOREIGN KEY (tenant_id, operation_id)
        REFERENCES public.gateway_operation_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_mcp_resource_draft_remote_mount FOREIGN KEY (tenant_id, remote_mount_id)
        REFERENCES public.gateway_mcp_remote_mount_draft_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uq_mcp_resource_draft_active_1
    ON public.gateway_mcp_resource_draft_t0 (tenant_id, server_id, resource_name)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_mcp_resource_draft_active_2
    ON public.gateway_mcp_resource_draft_t0 (tenant_id, server_id, resource_uri)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_gateway_mcp_resource_group_server
    ON public.gateway_mcp_resource_draft_t0 (tenant_id, gateway_group_id, server_id, enabled)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 31 gateway_mcp_resource_template_draft
--    -> public.gateway_mcp_resource_template_draft_t0
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_mcp_resource_template_draft_t0 (
    id                  bigint          NOT NULL,
    tenant_id           bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id      varchar(128)    NOT NULL,
    create_time         timestamptz(6)  NOT NULL,
    update_user_id      varchar(128)    NOT NULL,
    update_time         timestamptz(6)  NOT NULL,
    deleted_at          timestamp(6),
    version             bigint          NOT NULL DEFAULT 0,
    gateway_group_id    bigint          NOT NULL,
    server_id           bigint          NOT NULL,
    template_name       varchar(256)    NOT NULL,
    uri_template        varchar(2048)   NOT NULL,
    driver_type         varchar(32)     NOT NULL,
    operation_id        bigint,
    remote_mount_id     bigint,
    content             jsonb           NOT NULL DEFAULT '{}'::jsonb,
    enabled             boolean         NOT NULL DEFAULT TRUE,
    revision            bigint          NOT NULL DEFAULT 0,
    CONSTRAINT pk_mcp_resource_template_draft PRIMARY KEY (id),
    CONSTRAINT uq_mcp_resource_template_draft_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_mcp_resource_template_draft_life_1
        UNIQUE (tenant_id, server_id, template_name, deleted_at),
    CONSTRAINT uq_mcp_resource_template_draft_life_2
        UNIQUE (tenant_id, server_id, uri_template, deleted_at),
    CONSTRAINT ck_gateway_mcp_template_driver
        CHECK (driver_type IN ('STATIC_TEXT', 'STATIC_BLOB', 'LOCAL_OPERATION',
                               'OBJECT_STORAGE', 'DATABASE_SCHEMA', 'APP_UI', 'REMOTE_MCP')),
    CONSTRAINT ck_gateway_mcp_template_content CHECK (jsonb_typeof(content) = 'object'),
    CONSTRAINT ck_gateway_mcp_template_revision CHECK (revision >= 0),
    CONSTRAINT fk_mcp_resource_template_draft_group FOREIGN KEY (tenant_id, gateway_group_id)
        REFERENCES public.gateway_group_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_mcp_resource_template_draft_server FOREIGN KEY (tenant_id, server_id)
        REFERENCES public.gateway_mcp_server_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_mcp_resource_template_draft_operation FOREIGN KEY (tenant_id, operation_id)
        REFERENCES public.gateway_operation_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_mcp_resource_template_draft_remote_mount
        FOREIGN KEY (tenant_id, remote_mount_id)
        REFERENCES public.gateway_mcp_remote_mount_draft_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uq_mcp_resource_template_draft_active_1
    ON public.gateway_mcp_resource_template_draft_t0 (tenant_id, server_id, template_name)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_mcp_resource_template_draft_active_2
    ON public.gateway_mcp_resource_template_draft_t0 (tenant_id, server_id, uri_template)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_gateway_mcp_template_group_server
    ON public.gateway_mcp_resource_template_draft_t0
        (tenant_id, gateway_group_id, server_id, enabled)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 32 gateway_mcp_prompt_draft -> public.gateway_mcp_prompt_draft_t0
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_mcp_prompt_draft_t0 (
    id                  bigint          NOT NULL,
    tenant_id           bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id      varchar(128)    NOT NULL,
    create_time         timestamptz(6)  NOT NULL,
    update_user_id      varchar(128)    NOT NULL,
    update_time         timestamptz(6)  NOT NULL,
    deleted_at          timestamp(6),
    version             bigint          NOT NULL DEFAULT 0,
    gateway_group_id    bigint          NOT NULL,
    server_id           bigint          NOT NULL,
    prompt_name         varchar(256)    NOT NULL,
    source_type         varchar(32)     NOT NULL,
    operation_id        bigint,
    remote_mount_id     bigint,
    content             jsonb           NOT NULL DEFAULT '{}'::jsonb,
    enabled             boolean         NOT NULL DEFAULT TRUE,
    revision            bigint          NOT NULL DEFAULT 0,
    CONSTRAINT pk_mcp_prompt_draft PRIMARY KEY (id),
    CONSTRAINT uq_mcp_prompt_draft_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_mcp_prompt_draft_life_1
        UNIQUE (tenant_id, server_id, prompt_name, deleted_at),
    CONSTRAINT ck_gateway_mcp_prompt_source
        CHECK (source_type IN ('LOCAL_TEMPLATE', 'STATIC_TEMPLATE', 'STRICT_TEMPLATE',
                               'LOCAL_OPERATION', 'REMOTE_MCP')),
    CONSTRAINT ck_gateway_mcp_prompt_content CHECK (jsonb_typeof(content) = 'object'),
    CONSTRAINT ck_gateway_mcp_prompt_revision CHECK (revision >= 0),
    CONSTRAINT fk_mcp_prompt_draft_group FOREIGN KEY (tenant_id, gateway_group_id)
        REFERENCES public.gateway_group_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_mcp_prompt_draft_server FOREIGN KEY (tenant_id, server_id)
        REFERENCES public.gateway_mcp_server_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_mcp_prompt_draft_operation FOREIGN KEY (tenant_id, operation_id)
        REFERENCES public.gateway_operation_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_mcp_prompt_draft_remote_mount FOREIGN KEY (tenant_id, remote_mount_id)
        REFERENCES public.gateway_mcp_remote_mount_draft_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uq_mcp_prompt_draft_active_1
    ON public.gateway_mcp_prompt_draft_t0 (tenant_id, server_id, prompt_name)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_gateway_mcp_prompt_group_server
    ON public.gateway_mcp_prompt_draft_t0 (tenant_id, gateway_group_id, server_id, enabled)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 33 gateway_mcp_task_policy_draft -> public.gateway_mcp_task_policy_draft_t0
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_mcp_task_policy_draft_t0 (
    id                  bigint          NOT NULL,
    tenant_id           bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id      varchar(128)    NOT NULL,
    create_time         timestamptz(6)  NOT NULL,
    update_user_id      varchar(128)    NOT NULL,
    update_time         timestamptz(6)  NOT NULL,
    deleted_at          timestamp(6),
    version             bigint          NOT NULL DEFAULT 0,
    gateway_group_id    bigint          NOT NULL,
    server_id           bigint          NOT NULL,
    tool_name           varchar(256)    NOT NULL,
    content             jsonb           NOT NULL DEFAULT '{}'::jsonb,
    enabled             boolean         NOT NULL DEFAULT TRUE,
    revision            bigint          NOT NULL DEFAULT 0,
    CONSTRAINT pk_mcp_task_policy_draft PRIMARY KEY (id),
    CONSTRAINT uq_mcp_task_policy_draft_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_mcp_task_policy_draft_life_1
        UNIQUE (tenant_id, server_id, tool_name, deleted_at),
    CONSTRAINT ck_gateway_mcp_task_policy_content CHECK (jsonb_typeof(content) = 'object'),
    CONSTRAINT ck_gateway_mcp_task_policy_revision CHECK (revision >= 0),
    CONSTRAINT fk_mcp_task_policy_draft_group FOREIGN KEY (tenant_id, gateway_group_id)
        REFERENCES public.gateway_group_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_mcp_task_policy_draft_server FOREIGN KEY (tenant_id, server_id)
        REFERENCES public.gateway_mcp_server_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uq_mcp_task_policy_draft_active_1
    ON public.gateway_mcp_task_policy_draft_t0 (tenant_id, server_id, tool_name)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_gateway_mcp_task_policy_group
    ON public.gateway_mcp_task_policy_draft_t0 (tenant_id, gateway_group_id, server_id)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 34 gateway_mcp_app_binding_draft -> public.gateway_mcp_app_binding_draft_t0
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_mcp_app_binding_draft_t0 (
    id                  bigint          NOT NULL,
    tenant_id           bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id      varchar(128)    NOT NULL,
    create_time         timestamptz(6)  NOT NULL,
    update_user_id      varchar(128)    NOT NULL,
    update_time         timestamptz(6)  NOT NULL,
    deleted_at          timestamp(6),
    version             bigint          NOT NULL DEFAULT 0,
    gateway_group_id    bigint          NOT NULL,
    server_id           bigint          NOT NULL,
    tool_name           varchar(256)    NOT NULL,
    app_artifact_id     bigint          NOT NULL,
    content             jsonb           NOT NULL DEFAULT '{}'::jsonb,
    enabled             boolean         NOT NULL DEFAULT TRUE,
    revision            bigint          NOT NULL DEFAULT 0,
    CONSTRAINT pk_mcp_app_binding_draft PRIMARY KEY (id),
    CONSTRAINT uq_mcp_app_binding_draft_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_mcp_app_binding_draft_life_1
        UNIQUE (tenant_id, server_id, tool_name, app_artifact_id, deleted_at),
    CONSTRAINT ck_gateway_mcp_app_binding_content CHECK (jsonb_typeof(content) = 'object'),
    CONSTRAINT ck_gateway_mcp_app_binding_revision CHECK (revision >= 0),
    CONSTRAINT fk_mcp_app_binding_draft_group FOREIGN KEY (tenant_id, gateway_group_id)
        REFERENCES public.gateway_group_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_mcp_app_binding_draft_server FOREIGN KEY (tenant_id, server_id)
        REFERENCES public.gateway_mcp_server_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_mcp_app_binding_draft_app_artifact FOREIGN KEY (tenant_id, app_artifact_id)
        REFERENCES public.gateway_mcp_app_artifact_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uq_mcp_app_binding_draft_active_1
    ON public.gateway_mcp_app_binding_draft_t0 (tenant_id, server_id, tool_name, app_artifact_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_gateway_mcp_app_binding_group
    ON public.gateway_mcp_app_binding_draft_t0 (tenant_id, gateway_group_id, server_id)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 35 gateway_mcp_remote_tool_draft -> public.gateway_mcp_remote_tool_draft_t0
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_mcp_remote_tool_draft_t0 (
    id                  bigint          NOT NULL,
    tenant_id           bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id      varchar(128)    NOT NULL,
    create_time         timestamptz(6)  NOT NULL,
    update_user_id      varchar(128)    NOT NULL,
    update_time         timestamptz(6)  NOT NULL,
    deleted_at          timestamp(6),
    version             bigint          NOT NULL DEFAULT 0,
    gateway_group_id    bigint          NOT NULL,
    server_id           bigint          NOT NULL,
    tool_name           varchar(256)    NOT NULL,
    remote_mount_id     bigint          NOT NULL,
    content             jsonb           NOT NULL DEFAULT '{}'::jsonb,
    enabled             boolean         NOT NULL DEFAULT TRUE,
    revision            bigint          NOT NULL DEFAULT 0,
    CONSTRAINT pk_mcp_remote_tool_draft PRIMARY KEY (id),
    CONSTRAINT uq_mcp_remote_tool_draft_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_mcp_remote_tool_draft_life_1
        UNIQUE (tenant_id, server_id, tool_name, deleted_at),
    CONSTRAINT ck_gateway_mcp_remote_tool_content CHECK (jsonb_typeof(content) = 'object'),
    CONSTRAINT ck_gateway_mcp_remote_tool_revision CHECK (revision >= 0),
    CONSTRAINT fk_mcp_remote_tool_draft_group FOREIGN KEY (tenant_id, gateway_group_id)
        REFERENCES public.gateway_group_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_mcp_remote_tool_draft_server FOREIGN KEY (tenant_id, server_id)
        REFERENCES public.gateway_mcp_server_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_mcp_remote_tool_draft_remote_mount FOREIGN KEY (tenant_id, remote_mount_id)
        REFERENCES public.gateway_mcp_remote_mount_draft_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uq_mcp_remote_tool_draft_active_1
    ON public.gateway_mcp_remote_tool_draft_t0 (tenant_id, server_id, tool_name)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_gateway_mcp_remote_tool_group_server
    ON public.gateway_mcp_remote_tool_draft_t0
        (tenant_id, gateway_group_id, server_id, enabled)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_gateway_mcp_remote_tool_mount
    ON public.gateway_mcp_remote_tool_draft_t0 (tenant_id, remote_mount_id)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 36 gateway_mcp_approval -> public.gateway_mcp_approval_t0
-- subject_tenant_id 沿用原 MCP 协议自报租户字符串，与受信任 tenant_id 无关。
-- approval_key 保留 UUID 协议身份，技术 id 由 Snowflake 分配。
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_mcp_approval_t0 (
    id                  bigint          NOT NULL,
    tenant_id           bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id      varchar(128)    NOT NULL,
    create_time         timestamptz(6)  NOT NULL,
    update_user_id      varchar(128)    NOT NULL,
    update_time         timestamptz(6)  NOT NULL,
    deleted_at          timestamp(6),
    version             bigint          NOT NULL DEFAULT 0,
    token_digest        varchar(64)     NOT NULL,
    subject_id          varchar(128)    NOT NULL,
    client_id           varchar(128)    NOT NULL,
    server_code         varchar(128)    NOT NULL,
    tool_name           varchar(256)    NOT NULL,
    argument_digest     varchar(64)     NOT NULL,
    status              varchar(32)     NOT NULL DEFAULT 'PENDING',
    revision            bigint          NOT NULL DEFAULT 0,
    issued_at           timestamptz     NOT NULL,
    expires_at          timestamptz     NOT NULL,
    consumed_at         timestamptz,
    approval_key        varchar(64)     NOT NULL,
    subject_tenant_id   varchar(128)    NOT NULL,
    CONSTRAINT pk_mcp_approval PRIMARY KEY (id),
    CONSTRAINT uq_mcp_approval_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_mcp_approval_intent_1 UNIQUE (tenant_id, approval_key),
    CONSTRAINT uq_mcp_approval_intent_2 UNIQUE (tenant_id, token_digest),
    CONSTRAINT ck_gateway_mcp_approval_token_digest CHECK (length(token_digest) = 64),
    CONSTRAINT ck_gateway_mcp_approval_argument_digest CHECK (length(argument_digest) = 64),
    CONSTRAINT ck_gateway_mcp_approval_status
        CHECK (status IN ('PENDING', 'CONSUMED', 'EXPIRED', 'REVOKED')),
    CONSTRAINT ck_gateway_mcp_approval_expiry CHECK (expires_at > issued_at),
    CONSTRAINT ck_gateway_mcp_approval_consumed
        CHECK ((status = 'CONSUMED' AND consumed_at IS NOT NULL) OR (status <> 'CONSUMED')),
    CONSTRAINT ck_gateway_mcp_approval_revision CHECK (revision >= 0)
);

CREATE INDEX idx_gateway_mcp_approval_owner
    ON public.gateway_mcp_approval_t0
        (tenant_id, subject_id, subject_tenant_id, client_id, server_code, tool_name)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_gateway_mcp_approval_pending_expiry
    ON public.gateway_mcp_approval_t0 (tenant_id, expires_at)
    WHERE status = 'PENDING' AND deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 37 gateway_mcp_task_instance -> public.gateway_mcp_task_instance_t0
-- input/result/error_payload 为协议 JSON，opaque task_key 保留 base64 身份。
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_mcp_task_instance_t0 (
    id                      bigint          NOT NULL,
    tenant_id               bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id          varchar(128)    NOT NULL,
    create_time             timestamptz(6)  NOT NULL,
    update_user_id          varchar(128)    NOT NULL,
    update_time             timestamptz(6)  NOT NULL,
    deleted_at              timestamp(6),
    version                 bigint          NOT NULL DEFAULT 0,
    principal_fingerprint   varchar(128)    NOT NULL,
    subject_id              varchar(128)    NOT NULL,
    client_id               varchar(128)    NOT NULL,
    server_code             varchar(128)    NOT NULL,
    tool_name               varchar(256)    NOT NULL,
    request_digest          varchar(64)     NOT NULL,
    state                   varchar(32)     NOT NULL,
    input_payload           jsonb,
    result_payload          jsonb,
    error_payload           jsonb,
    worker_owner            varchar(256),
    lease_until             timestamptz,
    execution_deadline      timestamptz     NOT NULL,
    expires_at              timestamptz     NOT NULL,
    attempt_count           integer         NOT NULL DEFAULT 0,
    max_attempts            integer         NOT NULL,
    revision                bigint          NOT NULL DEFAULT 0,
    task_key                varchar(64)     NOT NULL,
    subject_tenant_id       varchar(128)    NOT NULL,
    CONSTRAINT pk_mcp_task_instance PRIMARY KEY (id),
    CONSTRAINT uq_mcp_task_instance_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_mcp_task_instance_intent_1 UNIQUE (tenant_id, task_key),
    CONSTRAINT ck_gateway_mcp_task_request_digest CHECK (length(request_digest) = 64),
    CONSTRAINT ck_gateway_mcp_task_state
        CHECK (state IN ('WORKING', 'INPUT_REQUIRED', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT ck_gateway_mcp_task_attempts
        CHECK (attempt_count >= 0 AND max_attempts > 0 AND attempt_count <= max_attempts),
    CONSTRAINT ck_gateway_mcp_task_expiry
        CHECK (expires_at > create_time AND execution_deadline > create_time),
    CONSTRAINT ck_gateway_mcp_task_lease
        CHECK ((worker_owner IS NULL AND lease_until IS NULL)
               OR (worker_owner IS NOT NULL AND lease_until IS NOT NULL)),
    CONSTRAINT ck_gateway_mcp_task_revision CHECK (revision >= 0)
);

CREATE INDEX idx_gateway_mcp_task_owner
    ON public.gateway_mcp_task_instance_t0
        (tenant_id, principal_fingerprint, subject_tenant_id, client_id, create_time DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_gateway_mcp_task_status_expiry
    ON public.gateway_mcp_task_instance_t0 (tenant_id, state, expires_at)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_gateway_mcp_task_pending_worker
    ON public.gateway_mcp_task_instance_t0 (tenant_id, lease_until, create_time)
    WHERE state = 'WORKING' AND deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 38 gateway_mcp_managed_tool_override -> public.gateway_mcp_managed_tool_override_t0
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_mcp_managed_tool_override_t0 (
    id                      bigint          NOT NULL,
    tenant_id               bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id          varchar(128)    NOT NULL,
    create_time             timestamptz(6)  NOT NULL,
    update_user_id          varchar(128)    NOT NULL,
    update_time             timestamptz(6)  NOT NULL,
    deleted_at              timestamp(6),
    version                 bigint          NOT NULL DEFAULT 0,
    tool_id                 varchar(64)     NOT NULL,
    gateway_group_id        bigint          NOT NULL,
    operation_id            bigint          NOT NULL,
    server_id               bigint,
    additional_permissions  jsonb           NOT NULL DEFAULT '[]'::jsonb,
    minimum_risk_level      varchar(16),
    enabled                 boolean,
    revision                bigint          NOT NULL DEFAULT 0,
    CONSTRAINT pk_mcp_managed_tool_override PRIMARY KEY (id),
    CONSTRAINT uq_mcp_managed_tool_override_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_mcp_managed_tool_override_life_1
        UNIQUE (tenant_id, gateway_group_id, operation_id, deleted_at),
    CONSTRAINT uq_mcp_managed_tool_override_life_2 UNIQUE (tenant_id, tool_id, deleted_at),
    CONSTRAINT ck_gateway_mcp_managed_tool_permissions
        CHECK (jsonb_typeof(additional_permissions) = 'array'),
    CONSTRAINT ck_gateway_mcp_managed_tool_risk
        CHECK (minimum_risk_level IS NULL
               OR minimum_risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_gateway_mcp_managed_tool_enabled
        CHECK (enabled IS NULL OR enabled = FALSE),
    CONSTRAINT ck_gateway_mcp_managed_tool_override
        CHECK (server_id IS NOT NULL
               OR jsonb_array_length(additional_permissions) > 0
               OR minimum_risk_level IS NOT NULL
               OR enabled = FALSE),
    CONSTRAINT ck_gateway_mcp_managed_tool_revision CHECK (revision >= 0),
    CONSTRAINT fk_mcp_managed_tool_override_group FOREIGN KEY (tenant_id, gateway_group_id)
        REFERENCES public.gateway_group_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_mcp_managed_tool_override_operation FOREIGN KEY (tenant_id, operation_id)
        REFERENCES public.gateway_operation_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_mcp_managed_tool_override_server FOREIGN KEY (tenant_id, server_id)
        REFERENCES public.gateway_mcp_server_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uq_mcp_managed_tool_override_active_1
    ON public.gateway_mcp_managed_tool_override_t0 (tenant_id, gateway_group_id, operation_id)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_mcp_managed_tool_override_active_2
    ON public.gateway_mcp_managed_tool_override_t0 (tenant_id, tool_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_gateway_mcp_managed_tool_group
    ON public.gateway_mcp_managed_tool_override_t0 (tenant_id, gateway_group_id, operation_id)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 39 gateway_openapi_snapshot -> public.gateway_openapi_snapshot_t0
-- document_json 保存规范化后的 OpenAPI 3.1 文档正文。
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_openapi_snapshot_t0 (
    id                      bigint          NOT NULL,
    tenant_id               bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id          varchar(128)    NOT NULL,
    create_time             timestamptz(6)  NOT NULL,
    update_user_id          varchar(128)    NOT NULL,
    update_time             timestamptz(6)  NOT NULL,
    deleted_at              timestamp(6),
    version                 bigint          NOT NULL DEFAULT 0,
    application_id          bigint          NOT NULL,
    definition_set_id       bigint,
    build_id                varchar(256)    NOT NULL,
    artifact_version        varchar(128)    NOT NULL,
    openapi_group           varchar(128)    NOT NULL DEFAULT 'default',
    openapi_version         varchar(32)     NOT NULL,
    document_sha256         char(64)        NOT NULL,
    canonical_sha256        char(64)        NOT NULL,
    document_json           jsonb           NOT NULL,
    validation_status       varchar(32)     NOT NULL,
    validation_messages     jsonb           NOT NULL DEFAULT '[]'::jsonb,
    operation_count         integer         NOT NULL DEFAULT 0,
    schema_count            integer         NOT NULL DEFAULT 0,
    fetched_from_instance_id varchar(256)   NOT NULL,
    fetched_at              timestamptz     NOT NULL,
    validated_at            timestamptz     NOT NULL,
    CONSTRAINT pk_openapi_snapshot PRIMARY KEY (id),
    CONSTRAINT uq_openapi_snapshot_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_openapi_snapshot_intent_1
        UNIQUE (tenant_id, application_id, build_id, openapi_group, canonical_sha256),
    CONSTRAINT ck_gateway_openapi_snapshot_group
        CHECK (openapi_group ~ '^[a-z][a-z0-9-]{0,63}$'),
    CONSTRAINT ck_gateway_openapi_snapshot_version CHECK (openapi_version LIKE '3.1.%'),
    CONSTRAINT ck_gateway_openapi_snapshot_document_sha
        CHECK (document_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_gateway_openapi_snapshot_canonical_sha
        CHECK (canonical_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_gateway_openapi_snapshot_document_json
        CHECK (jsonb_typeof(document_json) = 'object'),
    CONSTRAINT ck_gateway_openapi_snapshot_validation_status
        CHECK (validation_status IN ('VALID', 'INVALID')),
    CONSTRAINT ck_gateway_openapi_snapshot_validation_messages
        CHECK (jsonb_typeof(validation_messages) = 'array'),
    CONSTRAINT ck_gateway_openapi_snapshot_operation_count CHECK (operation_count >= 0),
    CONSTRAINT ck_gateway_openapi_snapshot_schema_count CHECK (schema_count >= 0),
    CONSTRAINT fk_openapi_snapshot_application FOREIGN KEY (tenant_id, application_id)
        REFERENCES public.gateway_application_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_openapi_snapshot_definition_set FOREIGN KEY (tenant_id, definition_set_id)
        REFERENCES public.gateway_definition_set_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE INDEX idx_gateway_openapi_snapshot_definition
    ON public.gateway_openapi_snapshot_t0
        (tenant_id, definition_set_id, openapi_group, id)
    WHERE definition_set_id IS NOT NULL AND deleted_at IS NULL;

CREATE INDEX idx_gateway_openapi_snapshot_app_time
    ON public.gateway_openapi_snapshot_t0 (tenant_id, application_id, fetched_at DESC, id DESC)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 40 gateway_openapi_sync_state -> public.gateway_openapi_sync_state_t0
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_openapi_sync_state_t0 (
    id                      bigint          NOT NULL,
    tenant_id               bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id          varchar(128)    NOT NULL,
    create_time             timestamptz(6)  NOT NULL,
    update_user_id          varchar(128)    NOT NULL,
    update_time             timestamptz(6)  NOT NULL,
    deleted_at              timestamp(6),
    version                 bigint          NOT NULL DEFAULT 0,
    application_id          bigint          NOT NULL,
    build_id                varchar(256)    NOT NULL,
    artifact_version        varchar(128)    NOT NULL,
    openapi_group           varchar(128)    NOT NULL DEFAULT 'default',
    provider_service_name   varchar(256)    NOT NULL,
    provider_group          varchar(128)    NOT NULL DEFAULT 'default',
    provider_version        varchar(128)    NOT NULL,
    status                  varchar(32)     NOT NULL DEFAULT 'DISCOVERED',
    latest_snapshot_id      bigint,
    definition_set_id       bigint,
    last_instance_id        varchar(256),
    attempt_count           integer         NOT NULL DEFAULT 0,
    last_error_code         varchar(128),
    last_error_message      varchar(1024),
    first_discovered_at     timestamptz     NOT NULL,
    last_attempt_at         timestamptz,
    last_success_at         timestamptz,
    next_retry_at           timestamptz,
    revision                bigint          NOT NULL DEFAULT 0,
    CONSTRAINT pk_openapi_sync_state PRIMARY KEY (id),
    CONSTRAINT uq_openapi_sync_state_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_openapi_sync_state_intent_1
        UNIQUE (tenant_id, application_id, build_id, openapi_group),
    CONSTRAINT ck_gateway_openapi_sync_group
        CHECK (openapi_group ~ '^[a-z][a-z0-9-]{0,63}$'),
    CONSTRAINT ck_gateway_openapi_sync_status
        CHECK (status IN (
            'DISCOVERED', 'FETCHING', 'VALIDATING', 'INVALID', 'INCONSISTENT_BUILD',
            'INGESTING', 'VALID', 'INGEST_FAILED', 'FETCH_FAILED', 'STALE'
        )),
    CONSTRAINT ck_gateway_openapi_sync_attempt_count CHECK (attempt_count >= 0),
    CONSTRAINT ck_gateway_openapi_sync_revision CHECK (revision >= 0),
    CONSTRAINT fk_openapi_sync_state_application FOREIGN KEY (tenant_id, application_id)
        REFERENCES public.gateway_application_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_openapi_sync_state_latest_snapshot FOREIGN KEY (tenant_id, latest_snapshot_id)
        REFERENCES public.gateway_openapi_snapshot_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_openapi_sync_state_definition_set FOREIGN KEY (tenant_id, definition_set_id)
        REFERENCES public.gateway_definition_set_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE INDEX idx_gateway_openapi_sync_due
    ON public.gateway_openapi_sync_state_t0 (tenant_id, next_retry_at, id)
    WHERE status IN ('DISCOVERED', 'FETCH_FAILED', 'INGEST_FAILED', 'STALE') AND deleted_at IS NULL;

CREATE INDEX idx_gateway_openapi_sync_app
    ON public.gateway_openapi_sync_state_t0 (tenant_id, application_id, update_time DESC, id DESC)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 41 gateway_llm_channel -> public.gateway_llm_channel_t0
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_llm_channel_t0 (
    id                  bigint          NOT NULL,
    tenant_id           bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id      varchar(128)    NOT NULL,
    create_time         timestamptz(6)  NOT NULL,
    update_user_id      varchar(128)    NOT NULL,
    update_time         timestamptz(6)  NOT NULL,
    deleted_at          timestamp(6),
    version             bigint          NOT NULL DEFAULT 0,
    channel_key         varchar(64)     NOT NULL,
    name                varchar(128)    NOT NULL,
    deployment          varchar(8)      NOT NULL
                        CHECK (deployment IN ('LOCAL', 'CLOUD')),
    protocol            varchar(32)     NOT NULL
                        CHECK (protocol IN ('OPENAI_CHAT', 'OPENAI_EMBEDDING',
                                            'OPENAI_RESPONSES', 'ANTHROPIC_MESSAGES')),
    base_url            varchar(2048)   NOT NULL,
    secret_ref          varchar(128),
    enabled             boolean         NOT NULL DEFAULT false,
    connect_timeout_ms  integer         NOT NULL DEFAULT 3000
                        CHECK (connect_timeout_ms BETWEEN 1 AND 10000),
    header_timeout_ms   integer         NOT NULL DEFAULT 30000
                        CHECK (header_timeout_ms BETWEEN 1 AND 120000),
    idle_timeout_ms     integer         NOT NULL DEFAULT 30000
                        CHECK (idle_timeout_ms BETWEEN 1 AND 120000),
    total_timeout_ms    integer         NOT NULL DEFAULT 120000
                        CHECK (total_timeout_ms BETWEEN 1 AND 600000
                               AND total_timeout_ms >= connect_timeout_ms
                               AND total_timeout_ms >= header_timeout_ms
                               AND total_timeout_ms >= idle_timeout_ms),
    max_concurrent      integer         NOT NULL DEFAULT 16
                        CHECK (max_concurrent BETWEEN 1 AND 256),
    revision            bigint          NOT NULL DEFAULT 1 CHECK (revision > 0),
    CONSTRAINT pk_llm_channel PRIMARY KEY (id),
    CONSTRAINT uq_llm_channel_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_llm_channel_life_1 UNIQUE (tenant_id, channel_key, deleted_at)
);

CREATE UNIQUE INDEX uq_llm_channel_active_1
    ON public.gateway_llm_channel_t0 (tenant_id, channel_key)
    WHERE deleted_at IS NULL;

CREATE INDEX ix_llm_channel_page
    ON public.gateway_llm_channel_t0 (tenant_id, create_time DESC, id DESC)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 42 gateway_llm_model -> public.gateway_llm_model_t0
-- protocols/allowed_subjects/routes 为 JSON 载荷，元素约束由 Service 校验。
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_llm_model_t0 (
    id                  bigint          NOT NULL,
    tenant_id           bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id      varchar(128)    NOT NULL,
    create_time         timestamptz(6)  NOT NULL,
    update_user_id      varchar(128)    NOT NULL,
    update_time         timestamptz(6)  NOT NULL,
    deleted_at          timestamp(6),
    version             bigint          NOT NULL DEFAULT 0,
    model_key           varchar(64)     NOT NULL,
    name                varchar(128)    NOT NULL,
    kind                varchar(16)     NOT NULL CHECK (kind IN ('CHAT', 'EMBEDDING')),
    protocols           jsonb           NOT NULL,
    enabled             boolean         NOT NULL DEFAULT false,
    dimensions          integer,
    embedding_space_id  varchar(128),
    allowed_subjects    jsonb           NOT NULL,
    routes              jsonb           NOT NULL,
    revision            bigint          NOT NULL DEFAULT 1 CHECK (revision > 0),
    CONSTRAINT pk_llm_model PRIMARY KEY (id),
    CONSTRAINT uq_llm_model_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_llm_model_life_1 UNIQUE (tenant_id, model_key, deleted_at)
);

CREATE UNIQUE INDEX uq_llm_model_active_1
    ON public.gateway_llm_model_t0 (tenant_id, model_key)
    WHERE deleted_at IS NULL;

CREATE INDEX ix_llm_model_page
    ON public.gateway_llm_model_t0 (tenant_id, create_time DESC, id DESC)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 43 gateway_knowledge_base -> public.gateway_knowledge_base_t0
-- members 为 actorId/role 数组，最多 100，成员匹配走 GIN。
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_knowledge_base_t0 (
    id                  bigint          NOT NULL,
    tenant_id           bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id      varchar(128)    NOT NULL,
    create_time         timestamptz(6)  NOT NULL,
    update_user_id      varchar(128)    NOT NULL,
    update_time         timestamptz(6)  NOT NULL,
    deleted_at          timestamp(6),
    version             bigint          NOT NULL DEFAULT 0,
    name                varchar(128)    NOT NULL,
    description         text            NOT NULL DEFAULT '',
    owner_actor_id      varchar(128)    NOT NULL,
    members             jsonb           NOT NULL,
    egress_policy       varchar(32)     NOT NULL DEFAULT 'LOCAL_ONLY'
                        CHECK (egress_policy IN ('LOCAL_ONLY', 'CLOUD_ALLOWED')),
    chat_model          varchar(64)     NOT NULL,
    embedding_model     varchar(64)     NOT NULL,
    embedding_space_id  varchar(128)    NOT NULL,
    dimensions          integer         NOT NULL CHECK (dimensions > 0),
    revision            bigint          NOT NULL DEFAULT 1 CHECK (revision > 0),
    CONSTRAINT pk_knowledge_base PRIMARY KEY (id),
    CONSTRAINT uq_knowledge_base_tenant_id UNIQUE (tenant_id, id)
);

CREATE INDEX ix_knowledge_base_members
    ON public.gateway_knowledge_base_t0 USING gin (members jsonb_path_ops);

CREATE INDEX ix_knowledge_base_page
    ON public.gateway_knowledge_base_t0 (tenant_id, create_time DESC, id DESC)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 44 gateway_knowledge_document -> public.gateway_knowledge_document_t0
-- active_revision_id 的反向指针在文件末尾的延期外键块中建立。
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_knowledge_document_t0 (
    id                  bigint          NOT NULL,
    tenant_id           bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id      varchar(128)    NOT NULL,
    create_time         timestamptz(6)  NOT NULL,
    update_user_id      varchar(128)    NOT NULL,
    update_time         timestamptz(6)  NOT NULL,
    deleted_at          timestamp(6),
    version             bigint          NOT NULL DEFAULT 0,
    kb_id               bigint          NOT NULL,
    file_name           varchar(255)    NOT NULL,
    active_revision_id  bigint,
    latest_job_id       bigint,
    revision            bigint          NOT NULL DEFAULT 1 CHECK (revision > 0),
    CONSTRAINT pk_knowledge_document PRIMARY KEY (id),
    CONSTRAINT uq_knowledge_document_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_knowledge_document_tenant_kb_id UNIQUE (tenant_id, kb_id, id),
    CONSTRAINT fk_knowledge_document_knowledge_base FOREIGN KEY (tenant_id, kb_id)
        REFERENCES public.gateway_knowledge_base_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE INDEX ix_knowledge_document_page
    ON public.gateway_knowledge_document_t0 (tenant_id, kb_id, create_time DESC, id DESC)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 45 gateway_knowledge_revision -> public.gateway_knowledge_revision_t0
-- raw_bytes 保存原件（1..20MiB），content_hash 是其 SHA256 小写 hex。
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_knowledge_revision_t0 (
    id                  bigint          NOT NULL,
    tenant_id           bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id      varchar(128)    NOT NULL,
    create_time         timestamptz(6)  NOT NULL,
    update_user_id      varchar(128)    NOT NULL,
    update_time         timestamptz(6)  NOT NULL,
    deleted_at          timestamp(6),
    version             bigint          NOT NULL DEFAULT 0,
    kb_id               bigint          NOT NULL,
    document_id         bigint          NOT NULL,
    file_name           varchar(255)    NOT NULL,
    media_type          varchar(128)    NOT NULL,
    raw_bytes           bytea           NOT NULL,
    byte_count          bigint          NOT NULL
                        CHECK (byte_count > 0 AND byte_count <= 20971520),
    content_hash        char(64)        NOT NULL,
    extracted_text      text,
    embedding_space_id  varchar(128)    NOT NULL,
    dimensions          integer         NOT NULL CHECK (dimensions > 0),
    chunking_config     jsonb           NOT NULL,
    status              varchar(24)     NOT NULL DEFAULT 'STAGING'
                        CHECK (status IN ('STAGING', 'READY', 'FAILED')),
    chunk_count         integer         NOT NULL DEFAULT 0,
    revision            bigint          NOT NULL DEFAULT 1 CHECK (revision > 0),
    CONSTRAINT pk_knowledge_revision PRIMARY KEY (id),
    CONSTRAINT uq_knowledge_revision_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_knowledge_revision_tenant_kb_id UNIQUE (tenant_id, kb_id, id),
    CONSTRAINT uq_knowledge_revision_tenant_kb_document_id
        UNIQUE (tenant_id, kb_id, document_id, id),
    CONSTRAINT fk_knowledge_revision_document FOREIGN KEY (tenant_id, document_id)
        REFERENCES public.gateway_knowledge_document_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_knowledge_revision_knowledge_base FOREIGN KEY (tenant_id, kb_id)
        REFERENCES public.gateway_knowledge_base_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE INDEX ix_knowledge_revision_page
    ON public.gateway_knowledge_revision_t0 (tenant_id, kb_id, create_time DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX ix_knowledge_revision_history
    ON public.gateway_knowledge_revision_t0 (tenant_id, document_id, create_time DESC, id DESC)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 46 gateway_knowledge_chunk -> public.gateway_knowledge_chunk_t0
-- embedding 是无 typmod 的 pgvector 列：维度不写进列定义，由
-- vector_dims(embedding) = dimensions 校验，与 KB/revision 冻结的 D 一致。
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_knowledge_chunk_t0 (
    id                  bigint          NOT NULL,
    tenant_id           bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id      varchar(128)    NOT NULL,
    create_time         timestamptz(6)  NOT NULL,
    update_user_id      varchar(128)    NOT NULL,
    update_time         timestamptz(6)  NOT NULL,
    deleted_at          timestamp(6),
    version             bigint          NOT NULL DEFAULT 0,
    kb_id               bigint          NOT NULL,
    revision_id         bigint          NOT NULL,
    chunk_index         integer         NOT NULL
                        CHECK (chunk_index >= 0 AND chunk_index <= 9999),
    content             text            NOT NULL,
    metadata            jsonb           NOT NULL DEFAULT '{}',
    content_hash        char(64)        NOT NULL,
    embedding_space_id  varchar(128)    NOT NULL,
    dimensions          integer         NOT NULL CHECK (dimensions > 0),
    embedding           vector          NOT NULL
                        CHECK (vector_dims(embedding) = dimensions),
    revision            bigint          NOT NULL DEFAULT 1 CHECK (revision > 0),
    CONSTRAINT pk_knowledge_chunk PRIMARY KEY (id),
    CONSTRAINT uq_knowledge_chunk_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_knowledge_chunk_intent_1 UNIQUE (tenant_id, revision_id, chunk_index),
    CONSTRAINT fk_knowledge_chunk_revision FOREIGN KEY (tenant_id, revision_id)
        REFERENCES public.gateway_knowledge_revision_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_knowledge_chunk_revision_scope
        FOREIGN KEY (tenant_id, kb_id, revision_id)
        REFERENCES public.gateway_knowledge_revision_t0 (tenant_id, kb_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_knowledge_chunk_knowledge_base FOREIGN KEY (tenant_id, kb_id)
        REFERENCES public.gateway_knowledge_base_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE INDEX ix_knowledge_chunk_page
    ON public.gateway_knowledge_chunk_t0 (tenant_id, kb_id, create_time DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX ix_knowledge_chunk_scope
    ON public.gateway_knowledge_chunk_t0 (tenant_id, kb_id, embedding_space_id, revision_id)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 47 gateway_knowledge_job -> public.gateway_knowledge_job_t0
-- payload/result 为持久命令与成功输出 JSON；不存密钥或上游正文。
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_knowledge_job_t0 (
    id                  bigint          NOT NULL,
    tenant_id           bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id      varchar(128)    NOT NULL,
    create_time         timestamptz(6)  NOT NULL,
    update_user_id      varchar(128)    NOT NULL,
    update_time         timestamptz(6)  NOT NULL,
    deleted_at          timestamp(6),
    version             bigint          NOT NULL DEFAULT 0,
    kb_id               bigint          NOT NULL,
    type                varchar(24)     NOT NULL
                        CHECK (type IN ('DOCUMENT_INGEST', 'WIKI_GENERATE')),
    resource_id         varchar(64)     NOT NULL,
    actor_id            varchar(128)    NOT NULL,
    payload             jsonb           NOT NULL,
    idempotency_key     varchar(64)     NOT NULL,
    request_hash        char(64)        NOT NULL,
    status              varchar(24)     NOT NULL DEFAULT 'QUEUED'
                        CHECK (status IN ('QUEUED', 'RUNNING', 'RETRY_WAIT', 'SUCCEEDED',
                                          'FAILED', 'STALE', 'CANCELLED')),
    stage               varchar(24)     NOT NULL DEFAULT 'QUEUED'
                        CHECK (stage IN ('QUEUED', 'PARSE', 'EMBED', 'GENERATE',
                                         'PUBLISH', 'DONE')),
    attempt             integer         NOT NULL DEFAULT 0
                        CHECK (attempt >= 0 AND attempt <= 3),
    next_attempt_at     timestamptz(6)  NOT NULL,
    lease_owner         varchar(128),
    lease_token         bigint          NOT NULL DEFAULT 0,
    lease_expires_at    timestamptz(6),
    error_code          varchar(128),
    result              jsonb,
    retry_of_job_id     bigint,
    revision            bigint          NOT NULL DEFAULT 1 CHECK (revision > 0),
    CONSTRAINT pk_knowledge_job PRIMARY KEY (id),
    CONSTRAINT uq_knowledge_job_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_knowledge_job_intent_1
        UNIQUE (tenant_id, kb_id, actor_id, type, idempotency_key),
    CONSTRAINT fk_knowledge_job_knowledge_base FOREIGN KEY (tenant_id, kb_id)
        REFERENCES public.gateway_knowledge_base_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE INDEX ix_knowledge_job_page
    ON public.gateway_knowledge_job_t0 (tenant_id, kb_id, create_time DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX ix_knowledge_job_claim
    ON public.gateway_knowledge_job_t0 (tenant_id, next_attempt_at, id)
    WHERE deleted_at IS NULL AND status IN ('QUEUED', 'RETRY_WAIT');

CREATE INDEX ix_knowledge_job_lease
    ON public.gateway_knowledge_job_t0 (tenant_id, lease_expires_at, id)
    WHERE deleted_at IS NULL AND status = 'RUNNING';

-- ---------------------------------------------------------------------------
-- 48 gateway_wiki_page -> public.gateway_wiki_page_t0
-- draft/published 两个反向指针在文件末尾的延期外键块中建立。
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_wiki_page_t0 (
    id                      bigint          NOT NULL,
    tenant_id               bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id          varchar(128)    NOT NULL,
    create_time             timestamptz(6)  NOT NULL,
    update_user_id          varchar(128)    NOT NULL,
    update_time             timestamptz(6)  NOT NULL,
    deleted_at              timestamp(6),
    version                 bigint          NOT NULL DEFAULT 0,
    kb_id                   bigint          NOT NULL,
    slug                    varchar(64)     NOT NULL,
    draft_revision_id       bigint,
    published_revision_id   bigint,
    revision                bigint          NOT NULL DEFAULT 1 CHECK (revision > 0),
    CONSTRAINT pk_wiki_page PRIMARY KEY (id),
    CONSTRAINT uq_wiki_page_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_wiki_page_tenant_kb_id UNIQUE (tenant_id, kb_id, id),
    CONSTRAINT uq_wiki_page_life_1 UNIQUE (tenant_id, kb_id, slug, deleted_at),
    CONSTRAINT fk_wiki_page_knowledge_base FOREIGN KEY (tenant_id, kb_id)
        REFERENCES public.gateway_knowledge_base_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uq_wiki_page_active_1
    ON public.gateway_wiki_page_t0 (tenant_id, kb_id, slug)
    WHERE deleted_at IS NULL;

CREATE INDEX ix_wiki_page_page
    ON public.gateway_wiki_page_t0 (tenant_id, kb_id, create_time DESC, id DESC)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 49 gateway_wiki_revision -> public.gateway_wiki_revision_t0
-- sources 为 {documentRevisionId,chunkId,sourceHash} 数组，检索用 GIN 命中。
-- ---------------------------------------------------------------------------
CREATE TABLE public.gateway_wiki_revision_t0 (
    id                              bigint          NOT NULL,
    tenant_id                       bigint          NOT NULL CHECK (tenant_id > 0),
    create_user_id                  varchar(128)    NOT NULL,
    create_time                     timestamptz(6)  NOT NULL,
    update_user_id                  varchar(128)    NOT NULL,
    update_time                     timestamptz(6)  NOT NULL,
    deleted_at                      timestamp(6),
    version                         bigint          NOT NULL DEFAULT 0,
    kb_id                           bigint          NOT NULL,
    page_id                         bigint          NOT NULL,
    title                           varchar(128)    NOT NULL,
    markdown                        text            NOT NULL
                                    CHECK (octet_length(markdown) BETWEEN 1 AND 131072),
    tags                            jsonb           NOT NULL DEFAULT '[]',
    links                           jsonb           NOT NULL DEFAULT '[]',
    sources                         jsonb           NOT NULL,
    content_hash                    char(64)        NOT NULL,
    author_actor_id                 varchar(128)    NOT NULL,
    generation_job_id               bigint,
    publication_status              varchar(16)     NOT NULL DEFAULT 'DRAFT'
                                    CHECK (publication_status IN ('DRAFT', 'PUBLISHING',
                                                                  'PUBLISHED', 'SUPERSEDED',
                                                                  'ARCHIVED')),
    review_status                   varchar(16)     NOT NULL DEFAULT 'NOT_REQUIRED'
                                    CHECK (review_status IN ('NOT_REQUIRED', 'NOT_SUBMITTED',
                                                             'PENDING', 'APPROVED', 'REJECTED',
                                                             'CANCELLED')),
    publication_policy_snapshot     varchar(24)     NOT NULL DEFAULT 'DIRECT'
                                    CHECK (publication_policy_snapshot IN ('DIRECT',
                                                                           'REVIEW_REQUIRED')),
    publication_version             bigint          NOT NULL DEFAULT 1
                                    CHECK (publication_version > 0),
    review_instance_id              varchar(128),
    reviewer_actor_id               varchar(128),
    reviewed_at                     timestamptz(6),
    review_decision_code            varchar(64)     NOT NULL DEFAULT 'NOT_REQUIRED',
    published_at                    timestamptz(6),
    published_by_actor_id           varchar(128),
    archived_at                     timestamptz(6),
    archived_by_actor_id            varchar(128),
    publication_error_code          varchar(128),
    ever_published                  boolean         NOT NULL DEFAULT false,
    revision                        bigint          NOT NULL DEFAULT 1 CHECK (revision > 0),
    CONSTRAINT pk_wiki_revision PRIMARY KEY (id),
    CONSTRAINT uq_wiki_revision_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_wiki_revision_tenant_kb_page_id
        UNIQUE (tenant_id, kb_id, page_id, id),
    CONSTRAINT fk_wiki_revision_page FOREIGN KEY (tenant_id, page_id)
        REFERENCES public.gateway_wiki_page_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_wiki_revision_knowledge_base FOREIGN KEY (tenant_id, kb_id)
        REFERENCES public.gateway_knowledge_base_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uq_wiki_revision_published
    ON public.gateway_wiki_revision_t0 (tenant_id, page_id)
    WHERE deleted_at IS NULL AND publication_status = 'PUBLISHED';

CREATE INDEX ix_wiki_revision_page
    ON public.gateway_wiki_revision_t0 (tenant_id, kb_id, create_time DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX ix_wiki_revision_sources
    ON public.gateway_wiki_revision_t0 USING gin (sources jsonb_path_ops);

CREATE INDEX ix_wiki_revision_history
    ON public.gateway_wiki_revision_t0 (tenant_id, page_id, create_time DESC, id DESC)
    WHERE deleted_at IS NULL;

-- ===========================================================================
-- 同父资源指针环：父行与子行在同一事务内互相引用，初态允许 NULL，
-- 因此这些外键必须是 DEFERRABLE INITIALLY DEFERRED。
-- Cycle FKs for same-parent resource pointers: a row may be created before
-- the revision/definition it points at, so each pointer is deferrable.
-- ===========================================================================

ALTER TABLE public.gateway_operation_t0
    ADD CONSTRAINT fk_operation_current_definition
    FOREIGN KEY (tenant_id, current_definition_id)
    REFERENCES public.gateway_operation_definition_t0 (tenant_id, id)
    ON UPDATE RESTRICT ON DELETE RESTRICT
    DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE public.gateway_knowledge_document_t0
    ADD CONSTRAINT fk_knowledge_document_active_revision
    FOREIGN KEY (tenant_id, kb_id, id, active_revision_id)
    REFERENCES public.gateway_knowledge_revision_t0 (tenant_id, kb_id, document_id, id)
    ON UPDATE RESTRICT ON DELETE RESTRICT
    DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE public.gateway_wiki_page_t0
    ADD CONSTRAINT fk_wiki_page_draft_revision
    FOREIGN KEY (tenant_id, kb_id, id, draft_revision_id)
    REFERENCES public.gateway_wiki_revision_t0 (tenant_id, kb_id, page_id, id)
    ON UPDATE RESTRICT ON DELETE RESTRICT
    DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE public.gateway_wiki_page_t0
    ADD CONSTRAINT fk_wiki_page_published_revision
    FOREIGN KEY (tenant_id, kb_id, id, published_revision_id)
    REFERENCES public.gateway_wiki_revision_t0 (tenant_id, kb_id, page_id, id)
    ON UPDATE RESTRICT ON DELETE RESTRICT
    DEFERRABLE INITIALLY DEFERRED;

    ELSE
        RAISE EXCEPTION 'Unsupported managed DDL role % for the yuheng 1x1 topology', current_setting('egon_migration.role');
    END IF;
END
$egon$;

-- 受管DDL历史 / managed DDL history: the starter runner inserts into this table inside the
--   same transaction as the script, so the baseline owns its creation. The column set matches
--   EgonColaPostgreDdlRunner#insertHistory and #installedPrefix exactly.
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
