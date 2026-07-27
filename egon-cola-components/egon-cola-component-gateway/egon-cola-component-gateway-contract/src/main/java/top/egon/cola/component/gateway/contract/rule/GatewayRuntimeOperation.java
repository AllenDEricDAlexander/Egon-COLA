package top.egon.cola.component.gateway.contract.rule;

import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
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
        // Must serialize to nothing when empty, or an operation that has no
        // parameters no longer matches the ruleContentSha256 published for it.
        // This module stays serializer-free, so the rule is declared by the
        // mappers: GatewayRuleJsonCodec and GatewayRuleCanonicalizer.
        List<GatewayRuntimeParameter> parameters,
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
        // A snapshot published before parameters existed has no such field,
        // so an engine on this code must still be able to read it.
        parameters = parameters == null
                ? List.of()
                : parameters.stream()
                        .sorted(Comparator
                                .comparing(GatewayRuntimeParameter::location)
                                .thenComparing(GatewayRuntimeParameter::name))
                        .toList();
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

    /**
     * Compatibility overload matching the signature that preceded
     * {@code parameters}; the operation is built without parameter metadata.
     */
    public GatewayRuntimeOperation(
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
            boolean deprecated) {
        this(
                operationId,
                operationKey,
                protocol,
                methodIdentity,
                requestSchema,
                responseSchema,
                List.of(),
                externalAccessible,
                providerService,
                responseMode,
                policyRefs,
                attributes,
                deprecated
        );
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
