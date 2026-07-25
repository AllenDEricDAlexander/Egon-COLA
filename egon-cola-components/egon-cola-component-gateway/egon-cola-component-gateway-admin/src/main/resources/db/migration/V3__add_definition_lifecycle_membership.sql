ALTER TABLE gateway_definition_set
    ADD COLUMN activated_at TIMESTAMPTZ,
    ADD COLUMN retired_at TIMESTAMPTZ;

CREATE TABLE gateway_definition_set_operation (
    definition_set_id VARCHAR(64) NOT NULL
        REFERENCES gateway_definition_set(id),
    operation_id VARCHAR(64) NOT NULL
        REFERENCES gateway_operation(id),
    definition_id VARCHAR(64) NOT NULL
        REFERENCES gateway_operation_definition(id),
    method_identity VARCHAR(1024) NOT NULL,
    provider_service_identity JSONB NOT NULL,
    external_accessible BOOLEAN NOT NULL,
    deprecated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (definition_set_id, operation_id)
);

CREATE INDEX idx_gateway_definition_membership_operation
    ON gateway_definition_set_operation (operation_id, definition_set_id);

INSERT INTO gateway_definition_set_operation (
    definition_set_id,
    operation_id,
    definition_id,
    method_identity,
    provider_service_identity,
    external_accessible,
    deprecated,
    created_at
)
SELECT d.definition_set_id,
       d.operation_id,
       d.id,
       o.method_identity,
       o.provider_service_identity,
       d.external_accessible,
       o.lifecycle_status = 'DEPRECATED',
       d.created_at
  FROM gateway_operation_definition d
  JOIN gateway_operation o ON o.id = d.operation_id
 WHERE d.definition_set_id IS NOT NULL
ON CONFLICT (definition_set_id, operation_id) DO NOTHING;
