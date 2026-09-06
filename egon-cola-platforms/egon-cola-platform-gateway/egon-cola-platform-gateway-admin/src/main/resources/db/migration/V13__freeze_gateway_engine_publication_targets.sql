-- 冻结每个发布阶段的角色与 DDC scope，恢复和清理不能读取漂移后的配置。
-- Freeze role and DDC scope per phase; recovery and cleanup use the persisted target.
ALTER TABLE gateway_release_publication
    ADD COLUMN target_role VARCHAR(32),
    ADD COLUMN target_biz_code VARCHAR(128),
    ADD COLUMN target_env VARCHAR(64),
    ADD COLUMN target_app_code VARCHAR(128),
    ADD CONSTRAINT ck_gateway_publication_target CHECK (
        (target_role IS NULL AND target_biz_code IS NULL
            AND target_env IS NULL AND target_app_code IS NULL)
        OR (target_role IN ('API_RPC', 'MCP')
            AND target_role IS NOT NULL
            AND target_biz_code IS NOT NULL AND length(trim(target_biz_code)) > 0
            AND target_env IS NOT NULL AND length(trim(target_env)) > 0
            AND target_app_code IS NOT NULL AND length(trim(target_app_code)) > 0)
    );

-- 历史记录没有目标证据，保留 NULL，不推断或补造旧 scope。
-- Historical rows remain unclassified; no target scope is inferred or fabricated.
ALTER TABLE gateway_release_target
    ADD COLUMN engine_role VARCHAR(32),
    ADD CONSTRAINT ck_gateway_release_target_role CHECK (
        engine_role IS NULL OR engine_role IN ('API_RPC', 'MCP')
    );

CREATE UNIQUE INDEX uq_gateway_publication_role_artifact
    ON gateway_release_publication (
        release_id, attempt_no, target_role, config_key
    ) WHERE target_role IS NOT NULL;
