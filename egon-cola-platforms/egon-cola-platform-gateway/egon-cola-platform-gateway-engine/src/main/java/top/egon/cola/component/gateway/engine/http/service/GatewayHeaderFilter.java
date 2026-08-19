package top.egon.cola.component.gateway.engine.http.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Removes fixed and Connection-declared hop-by-hop fields without changing
 * end-to-end payload metadata.
 * 补充说明 / Supplementary summary: {@code GatewayHeaderFilter} 是过滤器，位于当前 Gateway 模块的相关包中，负责网关Header过滤器相关的职责与边界。
 * English supplement: {@code GatewayHeaderFilter} is a gateway header filter filter in the current Gateway module; it owns the gateway header filter-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayHeaderFilter {

    /**
     * 中文说明：表示 HOPBYHOP 这一固定值；它属于 {@code GatewayHeaderFilter} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value hop by hop; it is a state, type, or protocol value of {@code GatewayHeaderFilter} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayHeaderFilter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHeaderFilter}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final Set<String> HOP_BY_HOP = Set.of(
            "connection",
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "te",
            "trailer",
            "transfer-encoding",
            "upgrade",
            "proxy-connection"
    );

    /**
     * 中文说明：执行 请求Headers 操作；该方法是 {@code GatewayHeaderFilter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the request headers operation; this method is the invocation entry point on {@code GatewayHeaderFilter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHeaderFilter.requestHeaders(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @return 返回 请求Headers 的处理结果；returns the result of the operation.
     */
    public Map<String, List<String>> requestHeaders(
            Map<String, List<String>> source) {
        return filter(source);
    }

    /**
     * 中文说明：执行 响应Headers 操作；该方法是 {@code GatewayHeaderFilter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the response headers operation; this method is the invocation entry point on {@code GatewayHeaderFilter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHeaderFilter.responseHeaders(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @return 返回 响应Headers 的处理结果；returns the result of the operation.
     */
    public Map<String, List<String>> responseHeaders(
            Map<String, List<String>> source) {
        return filter(source);
    }

    /**
     * 中文说明：执行 过滤器 操作；该方法是 {@code GatewayHeaderFilter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the filter operation; this method is the invocation entry point on {@code GatewayHeaderFilter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHeaderFilter.filter(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @return 返回 过滤器 的处理结果；returns the result of the operation.
     */
    private Map<String, List<String>> filter(
            Map<String, List<String>> source) {
        Objects.requireNonNull(source, "source");
        Set<String> removals = new LinkedHashSet<>(HOP_BY_HOP);
        source.forEach((name, values) -> {
            if ("connection".equals(normalizedName(name))) {
                values.forEach(value -> connectionTokens(value, removals));
            }
        });
        Map<String, List<String>> result = new LinkedHashMap<>();
        source.forEach((name, values) -> {
            String normalized = normalizedName(name);
            if (!removals.contains(normalized)) {
                result.computeIfAbsent(
                        normalized,
                        ignored -> new ArrayList<>()
                ).addAll(List.copyOf(values));
            }
        });
        result.replaceAll((ignored, values) -> List.copyOf(values));
        return Map.copyOf(result);
    }

    /**
     * 中文说明：执行 connectionTokens 操作；该方法是 {@code GatewayHeaderFilter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the connection tokens operation; this method is the invocation entry point on {@code GatewayHeaderFilter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHeaderFilter.connectionTokens(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param target 参数 target；parameter target。
     */
    private void connectionTokens(String value, Set<String> target) {
        if (value == null) {
            return;
        }
        for (String token : value.split(",")) {
            String normalized = token.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty()) {
                target.add(normalized);
            }
        }
    }

    /**
     * 中文说明：执行 normalizedName 操作；该方法是 {@code GatewayHeaderFilter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the normalized name operation; this method is the invocation entry point on {@code GatewayHeaderFilter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHeaderFilter.normalizedName(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 normalizedName 的处理结果；returns the result of the operation.
     */
    private String normalizedName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("invalid HTTP header name");
        }
        return value.toLowerCase(Locale.ROOT);
    }
}
