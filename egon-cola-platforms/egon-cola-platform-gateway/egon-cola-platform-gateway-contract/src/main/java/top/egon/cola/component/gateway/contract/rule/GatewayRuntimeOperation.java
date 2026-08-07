package top.egon.cola.component.gateway.contract.rule;

import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record GatewayRuntimeOperation(
        String operationId,
        String operationKey,
        GatewayProtocol protocol,
        String methodIdentity,
        String requestSchema,
        String responseSchema,
        boolean externalAccessible,
        GatewayProviderServiceRef providerService,
        String responseMode,
        Set<String> policyRefs,
        Map<String, String> attributes,
        boolean deprecated
) {

    public GatewayRuntimeOperation {
        operationId = required(operationId, "operationId");
        operationKey = required(operationKey, "operationKey");
        protocol = Objects.requireNonNull(protocol, "protocol");
        methodIdentity = required(methodIdentity, "methodIdentity");
        providerService = Objects.requireNonNull(
                providerService,
                "providerService"
        );
        responseMode = required(responseMode, "responseMode");
        LinkedHashSet<String> sortedPolicyRefs = Objects.requireNonNull(
                        policyRefs,
                        "policyRefs"
                ).stream()
                .sorted()
                .collect(java.util.stream.Collectors.toCollection(
                        LinkedHashSet::new
                ));
        policyRefs = Collections.unmodifiableSet(sortedPolicyRefs);
        attributes = Map.copyOf(Objects.requireNonNull(
                attributes,
                "attributes"
        ));
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
