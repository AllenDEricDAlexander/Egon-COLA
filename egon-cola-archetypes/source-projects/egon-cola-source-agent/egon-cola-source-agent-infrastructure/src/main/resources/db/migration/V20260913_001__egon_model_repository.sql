-- Empty knowledge tables only; Flyway, Outbox and vector ownership remain unchanged.
DO $egon$
BEGIN
    IF EXISTS (SELECT 1 FROM knowledge_base) OR EXISTS (SELECT 1 FROM knowledge_document) THEN
        RAISE EXCEPTION 'REBUILD_REQUIRED: knowledge tables must be empty';
    END IF;
END
$egon$;

DROP INDEX uk_knowledge_base_tenant_code;
DROP INDEX idx_knowledge_base_tenant_created;
DROP INDEX idx_knowledge_document_tenant_base_created;

ALTER TABLE knowledge_base
    ADD COLUMN deleted_at timestamp(6) without time zone,
    ADD COLUMN version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    ALTER COLUMN id DROP IDENTITY IF EXISTS,
    ALTER COLUMN create_user_id SET NOT NULL,
    ALTER COLUMN update_user_id SET NOT NULL,
    DROP COLUMN is_deleted RESTRICT;
ALTER TABLE knowledge_document
    ADD COLUMN deleted_at timestamp(6) without time zone,
    ADD COLUMN version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    ALTER COLUMN id DROP IDENTITY IF EXISTS,
    ALTER COLUMN create_user_id SET NOT NULL,
    ALTER COLUMN update_user_id SET NOT NULL,
    DROP COLUMN is_deleted RESTRICT;

CREATE UNIQUE INDEX uk_knowledge_base_tenant_code ON knowledge_base(tenant_id, lower(code)) WHERE deleted_at IS NULL;
CREATE INDEX idx_knowledge_base_tenant_created ON knowledge_base(tenant_id, create_time DESC, id DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_knowledge_document_tenant_base_created ON knowledge_document(tenant_id, knowledge_base_id, create_time DESC, id DESC) WHERE deleted_at IS NULL;
