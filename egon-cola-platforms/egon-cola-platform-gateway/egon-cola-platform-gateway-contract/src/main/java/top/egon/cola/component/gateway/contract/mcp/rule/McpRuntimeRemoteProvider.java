package top.egon.cola.component.gateway.contract.mcp.rule;

import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;

import java.util.Objects;

public record McpRuntimeRemoteProvider(
        String providerId,
        String providerCode,
        String displayName,
        McpProtocolDialect dialect,
        String transportType,
        String endpointReference,
        String authProfileReference,
        String tlsProfileReference,
        String capabilityFingerprint,
        boolean enabled
) {

    public McpRuntimeRemoteProvider {
        providerId = McpContractSupport.required(providerId, "providerId");
        providerCode = McpContractSupport.required(
                providerCode,
                "providerCode"
        );
        displayName = McpContractSupport.required(
                displayName,
                "displayName"
        );
        dialect = Objects.requireNonNull(dialect, "dialect");
        transportType = McpContractSupport.required(
                transportType,
                "transportType"
        );
        endpointReference = McpContractSupport.required(
                endpointReference,
                "endpointReference"
        );
        authProfileReference = McpContractSupport.optional(
                authProfileReference
        );
        tlsProfileReference = McpContractSupport.optional(tlsProfileReference);
        capabilityFingerprint = McpContractSupport.required(
                capabilityFingerprint,
                "capabilityFingerprint"
        );
    }
}
