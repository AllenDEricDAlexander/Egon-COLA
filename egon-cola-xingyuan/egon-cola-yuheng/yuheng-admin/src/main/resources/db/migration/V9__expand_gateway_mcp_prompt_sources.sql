ALTER TABLE gateway_mcp_prompt_draft
    DROP CONSTRAINT ck_gateway_mcp_prompt_source;

ALTER TABLE gateway_mcp_prompt_draft
    ADD CONSTRAINT ck_gateway_mcp_prompt_source CHECK (
        source_type IN (
            'LOCAL_TEMPLATE', 'STATIC_TEMPLATE', 'STRICT_TEMPLATE',
            'LOCAL_OPERATION', 'REMOTE_MCP'
        )
    );
