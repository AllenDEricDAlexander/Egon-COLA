package top.egon.cola.component.gateway.engine.rpc;

import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.core.route.GatewayResponseMode;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;

public record RuntimeRpcRoute(
        String routeId,
        String operationId,
        String fullMethodName,
        ProviderServiceKey targetService,
        String requestType,
        String responseType,
        String descriptorSha256,
        Set<String> policyRefs,
        GatewayResponseMode responseMode,
        Duration timeout
) {

    public RuntimeRpcRoute {
        routeId = required(routeId, "routeId");
        operationId = required(operationId, "operationId");
        fullMethodName = required(fullMethodName, "fullMethodName");
        if (!fullMethodName.contains("/")) {
            throw new IllegalArgumentException(
                    "fullMethodName must be service/method"
            );
        }
        targetService = Objects.requireNonNull(
                targetService,
                "targetService"
        );
        if (targetService.protocolType() != ProviderProtocolType.RPC) {
            throw new IllegalArgumentException(
                    "RPC route target must use RPC provider protocol"
            );
        }
        requestType = required(requestType, "requestType");
        responseType = required(responseType, "responseType");
        descriptorSha256 = required(descriptorSha256, "descriptorSha256");
        policyRefs = Set.copyOf(Objects.requireNonNull(policyRefs, "policyRefs"));
        responseMode = Objects.requireNonNull(responseMode, "responseMode");
        timeout = Objects.requireNonNull(timeout, "timeout");
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
