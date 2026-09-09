package top.egon.cola.component.yuheng.contract.rule;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 绑定到操作或路由的运行时治理策略。
 *
 * <p>{@code configuration} 保留具体策略的键值，由 Engine 或类型化策略 Codec 解释。
 */
public record GatewayRuntimePolicy(
        String policyId,
        String type,
        String scope,
        Map<String, Object> configuration
) {

    public GatewayRuntimePolicy {
        policyId = required(policyId, "policyId");
        type = required(type, "type");
        scope = required(scope, "scope");
        configuration = Collections.unmodifiableMap(new LinkedHashMap<>(
                Objects.requireNonNull(configuration, "configuration")
        ));
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
