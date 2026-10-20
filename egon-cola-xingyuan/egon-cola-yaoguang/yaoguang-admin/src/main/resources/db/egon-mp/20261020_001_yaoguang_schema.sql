-- 20261020_001 Yaoguang knowledge and Wiki empty-database baseline.
-- Nine tenant-routed business tables in yaoguang_0/public; pgvector is operator installed.
-- No historical data is imported. The script is owned only by yaoguang-admin.
DO $egon$
BEGIN
    IF current_setting('egon_migration.role') = 'SHARD' THEN

-- ---------------------------------------------------------------------------
-- 20 yaoguang_idempotency_record -> public.yaoguang_idempotency_record_t0
-- response_content 缓存首次成功响应体，用于同键重放返回一致结果。
-- ---------------------------------------------------------------------------
CREATE TABLE public.yaoguang_idempotency_record_t0 (
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
-- 21 yaoguang_audit_log -> public.yaoguang_audit_log_t0
-- ---------------------------------------------------------------------------
CREATE TABLE public.yaoguang_audit_log_t0 (
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
    successful          boolean         NOT NULL,
    error_code          varchar(128),
    occurred_at         timestamptz     NOT NULL,
    CONSTRAINT pk_audit_log PRIMARY KEY (id),
    CONSTRAINT uq_audit_log_tenant_id UNIQUE (tenant_id, id)
);

CREATE INDEX idx_gateway_audit_resource
    ON public.yaoguang_audit_log_t0 (tenant_id, resource_type, resource_id, occurred_at DESC)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 43 yaoguang_knowledge_base -> public.yaoguang_knowledge_base_t0
-- members 为 actorId/role 数组，最多 100，成员匹配走 GIN。
-- ---------------------------------------------------------------------------
CREATE TABLE public.yaoguang_knowledge_base_t0 (
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
    ON public.yaoguang_knowledge_base_t0 USING gin (members jsonb_path_ops);

CREATE INDEX ix_knowledge_base_page
    ON public.yaoguang_knowledge_base_t0 (tenant_id, create_time DESC, id DESC)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 44 yaoguang_knowledge_document -> public.yaoguang_knowledge_document_t0
-- active_revision_id 的反向指针在文件末尾的延期外键块中建立。
-- ---------------------------------------------------------------------------
CREATE TABLE public.yaoguang_knowledge_document_t0 (
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
        REFERENCES public.yaoguang_knowledge_base_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE INDEX ix_knowledge_document_page
    ON public.yaoguang_knowledge_document_t0 (tenant_id, kb_id, create_time DESC, id DESC)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 45 yaoguang_knowledge_revision -> public.yaoguang_knowledge_revision_t0
-- raw_bytes 保存原件（1..20MiB），content_hash 是其 SHA256 小写 hex。
-- ---------------------------------------------------------------------------
CREATE TABLE public.yaoguang_knowledge_revision_t0 (
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
        REFERENCES public.yaoguang_knowledge_document_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_knowledge_revision_knowledge_base FOREIGN KEY (tenant_id, kb_id)
        REFERENCES public.yaoguang_knowledge_base_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE INDEX ix_knowledge_revision_page
    ON public.yaoguang_knowledge_revision_t0 (tenant_id, kb_id, create_time DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX ix_knowledge_revision_history
    ON public.yaoguang_knowledge_revision_t0 (tenant_id, document_id, create_time DESC, id DESC)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 46 yaoguang_knowledge_chunk -> public.yaoguang_knowledge_chunk_t0
-- embedding 是无 typmod 的 pgvector 列：维度不写进列定义，由
-- vector_dims(embedding) = dimensions 校验，与 KB/revision 冻结的 D 一致。
-- ---------------------------------------------------------------------------
CREATE TABLE public.yaoguang_knowledge_chunk_t0 (
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
        REFERENCES public.yaoguang_knowledge_revision_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_knowledge_chunk_revision_scope
        FOREIGN KEY (tenant_id, kb_id, revision_id)
        REFERENCES public.yaoguang_knowledge_revision_t0 (tenant_id, kb_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_knowledge_chunk_knowledge_base FOREIGN KEY (tenant_id, kb_id)
        REFERENCES public.yaoguang_knowledge_base_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE INDEX ix_knowledge_chunk_page
    ON public.yaoguang_knowledge_chunk_t0 (tenant_id, kb_id, create_time DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX ix_knowledge_chunk_scope
    ON public.yaoguang_knowledge_chunk_t0 (tenant_id, kb_id, embedding_space_id, revision_id)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 47 yaoguang_knowledge_job -> public.yaoguang_knowledge_job_t0
-- payload/result 为持久命令与成功输出 JSON；不存密钥或上游正文。
-- ---------------------------------------------------------------------------
CREATE TABLE public.yaoguang_knowledge_job_t0 (
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
        REFERENCES public.yaoguang_knowledge_base_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE INDEX ix_knowledge_job_page
    ON public.yaoguang_knowledge_job_t0 (tenant_id, kb_id, create_time DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX ix_knowledge_job_claim
    ON public.yaoguang_knowledge_job_t0 (tenant_id, next_attempt_at, id)
    WHERE deleted_at IS NULL AND status IN ('QUEUED', 'RETRY_WAIT');

CREATE INDEX ix_knowledge_job_lease
    ON public.yaoguang_knowledge_job_t0 (tenant_id, lease_expires_at, id)
    WHERE deleted_at IS NULL AND status = 'RUNNING';

-- ---------------------------------------------------------------------------
-- 48 yaoguang_wiki_page -> public.yaoguang_wiki_page_t0
-- draft/published 两个反向指针在文件末尾的延期外键块中建立。
-- ---------------------------------------------------------------------------
CREATE TABLE public.yaoguang_wiki_page_t0 (
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
        REFERENCES public.yaoguang_knowledge_base_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uq_wiki_page_active_1
    ON public.yaoguang_wiki_page_t0 (tenant_id, kb_id, slug)
    WHERE deleted_at IS NULL;

CREATE INDEX ix_wiki_page_page
    ON public.yaoguang_wiki_page_t0 (tenant_id, kb_id, create_time DESC, id DESC)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 49 yaoguang_wiki_revision -> public.yaoguang_wiki_revision_t0
-- sources 为 {documentRevisionId,chunkId,sourceHash} 数组，检索用 GIN 命中。
-- ---------------------------------------------------------------------------
CREATE TABLE public.yaoguang_wiki_revision_t0 (
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
        REFERENCES public.yaoguang_wiki_page_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_wiki_revision_knowledge_base FOREIGN KEY (tenant_id, kb_id)
        REFERENCES public.yaoguang_knowledge_base_t0 (tenant_id, id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uq_wiki_revision_published
    ON public.yaoguang_wiki_revision_t0 (tenant_id, page_id)
    WHERE deleted_at IS NULL AND publication_status = 'PUBLISHED';

CREATE INDEX ix_wiki_revision_page
    ON public.yaoguang_wiki_revision_t0 (tenant_id, kb_id, create_time DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX ix_wiki_revision_sources
    ON public.yaoguang_wiki_revision_t0 USING gin (sources jsonb_path_ops);

CREATE INDEX ix_wiki_revision_history
    ON public.yaoguang_wiki_revision_t0 (tenant_id, page_id, create_time DESC, id DESC)
    WHERE deleted_at IS NULL;

ALTER TABLE public.yaoguang_knowledge_document_t0
    ADD CONSTRAINT fk_knowledge_document_active_revision
    FOREIGN KEY (tenant_id, kb_id, id, active_revision_id)
    REFERENCES public.yaoguang_knowledge_revision_t0 (tenant_id, kb_id, document_id, id)
    ON UPDATE RESTRICT ON DELETE RESTRICT
    DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE public.yaoguang_wiki_page_t0
    ADD CONSTRAINT fk_wiki_page_draft_revision
    FOREIGN KEY (tenant_id, kb_id, id, draft_revision_id)
    REFERENCES public.yaoguang_wiki_revision_t0 (tenant_id, kb_id, page_id, id)
    ON UPDATE RESTRICT ON DELETE RESTRICT
    DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE public.yaoguang_wiki_page_t0
    ADD CONSTRAINT fk_wiki_page_published_revision
    FOREIGN KEY (tenant_id, kb_id, id, published_revision_id)
    REFERENCES public.yaoguang_wiki_revision_t0 (tenant_id, kb_id, page_id, id)
    ON UPDATE RESTRICT ON DELETE RESTRICT
    DEFERRABLE INITIALLY DEFERRED;

    ELSE
        RAISE EXCEPTION 'Unsupported managed DDL role % for the yaoguang 1x1 topology', current_setting('egon_migration.role');
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
