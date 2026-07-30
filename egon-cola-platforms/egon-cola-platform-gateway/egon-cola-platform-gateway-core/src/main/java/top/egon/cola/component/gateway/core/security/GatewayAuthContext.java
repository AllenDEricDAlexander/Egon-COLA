package top.egon.cola.component.gateway.core.security;

import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.core.context.GatewayPrincipal;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record GatewayAuthContext(
        AccessZone accessZone,
        GatewayProtocol protocol,
        String operationId,
        String routeId,
        String policyId,
        String requestTarget,
        String method,
        Set<String> credentialTypes,
        GatewayPrincipal principal,
        String remoteAddress,
        String traceId,
        String requestId,
        Instant deadline,
        String releaseId,
        Map<String, String> attributes
) {

    public GatewayAuthContext {
        accessZone = Objects.requireNonNull(accessZone, "accessZone");
        protocol = Objects.requireNonNull(protocol, "protocol");
        operationId = required(operationId, "operationId");
        routeId = optional(routeId);
        policyId = required(policyId, "policyId");
        requestTarget = required(requestTarget, "requestTarget");
        method = optional(method);
        credentialTypes = Set.copyOf(Objects.requireNonNull(
                credentialTypes,
                "credentialTypes"
        ));
        principal = Objects.requireNonNull(principal, "principal");
        remoteAddress = required(remoteAddress, "remoteAddress");
        traceId = required(traceId, "traceId");
        requestId = required(requestId, "requestId");
        deadline = Objects.requireNonNull(deadline, "deadline");
        releaseId = required(releaseId, "releaseId");
        attributes = Map.copyOf(Objects.requireNonNull(attributes, "attributes"));
        if (attributes.size() > 16) {
            throw new IllegalArgumentException("attribute count exceeds 16");
        }
    }

    public GatewayAuthContext withPrincipal(
            GatewayPrincipal authenticatedPrincipal) {
        return new GatewayAuthContext(
                accessZone,
                protocol,
                operationId,
                routeId,
                policyId,
                requestTarget,
                method,
                credentialTypes,
                authenticatedPrincipal,
                remoteAddress,
                traceId,
                requestId,
                deadline,
                releaseId,
                attributes
        );
    }

    /**
     * Compatibility constructor for callers that do not supply trusted route metadata.
     */
    public GatewayAuthContext(
            AccessZone accessZone,
            GatewayProtocol protocol,
            String operationId,
            String routeId,
            String policyId,
            String requestTarget,
            String method,
            Set<String> credentialTypes,
            GatewayPrincipal principal,
            String remoteAddress,
            String traceId,
            String requestId,
            Instant deadline,
            String releaseId
    ) {
        this(accessZone, protocol, operationId, routeId, policyId,
                requestTarget, method, credentialTypes, principal,
                remoteAddress, traceId, requestId, deadline, releaseId,
                Map.of());
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        String result = value.trim();
        if (result.length() > 1024) {
            throw new IllegalArgumentException(field + " exceeds 1024");
        }
        return result;
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
