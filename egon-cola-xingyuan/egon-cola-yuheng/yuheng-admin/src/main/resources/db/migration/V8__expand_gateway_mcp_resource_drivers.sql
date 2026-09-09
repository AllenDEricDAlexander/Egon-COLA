ALTER TABLE gateway_mcp_resource_draft
    DROP CONSTRAINT ck_gateway_mcp_resource_driver;

ALTER TABLE gateway_mcp_resource_draft
    ADD CONSTRAINT ck_gateway_mcp_resource_driver CHECK (
        driver_type IN (
            'STATIC_TEXT', 'STATIC_BLOB', 'LOCAL_OPERATION',
            'OBJECT_STORAGE', 'DATABASE_SCHEMA', 'APP_UI', 'REMOTE_MCP'
        )
    );

ALTER TABLE gateway_mcp_resource_template_draft
    DROP CONSTRAINT ck_gateway_mcp_template_driver;

ALTER TABLE gateway_mcp_resource_template_draft
    ADD CONSTRAINT ck_gateway_mcp_template_driver CHECK (
        driver_type IN (
            'STATIC_TEXT', 'STATIC_BLOB', 'LOCAL_OPERATION',
            'OBJECT_STORAGE', 'DATABASE_SCHEMA', 'APP_UI', 'REMOTE_MCP'
        )
    );
