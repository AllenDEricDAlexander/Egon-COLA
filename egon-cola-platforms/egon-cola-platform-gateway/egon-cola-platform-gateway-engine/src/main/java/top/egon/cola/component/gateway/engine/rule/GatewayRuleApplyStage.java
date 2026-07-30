package top.egon.cola.component.gateway.engine.rule;

public enum GatewayRuleApplyStage {

    NEVER_APPLIED,
    RECEIVED,
    CHECKSUM_VERIFIED,
    SCHEMA_VALIDATED,
    COMPILED,
    RESOURCE_PREPARED,
    DURABLE_STAGED,
    ACTIVE_POINTER_WRITTEN,
    MEMORY_ACTIVATED,
    ACK_SUCCESS,
    FAILED
}
