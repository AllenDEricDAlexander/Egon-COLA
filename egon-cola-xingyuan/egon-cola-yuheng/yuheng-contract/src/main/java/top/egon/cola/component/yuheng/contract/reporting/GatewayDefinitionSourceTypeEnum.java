package top.egon.cola.component.yuheng.contract.reporting;

/**
 * Identifies the authoritative source that produced a Gateway definition.
 * Gateway definition source is intentionally independent from transport
 * protocol so HTTP OpenAPI and RPC Descriptor reports share one ingestion
 * contract without pretending to have the same schema truth.
 */
public enum GatewayDefinitionSourceTypeEnum {

    /** Definition supplied through the existing manual management contract. */
    MANUAL,

    /** Definition derived from an RPC Protobuf Descriptor. */
    RPC_DESCRIPTOR,

    /** Definition derived from a Provider OpenAPI 3.1 document. */
    OPENAPI31
}
