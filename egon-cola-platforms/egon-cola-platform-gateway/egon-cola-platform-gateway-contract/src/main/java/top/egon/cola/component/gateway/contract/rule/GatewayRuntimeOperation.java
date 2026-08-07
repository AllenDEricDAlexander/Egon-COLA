package top.egon.cola.component.gateway.contract.rule;

import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 发布到 Engine 的协议无关操作定义。
 *
 * <p>操作是路由和 MCP Tool 共同引用的核心资源；其中 attributes 承载扩展元数据，例如注解
 * 生成的 MCP exposure 信息。
 */
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
