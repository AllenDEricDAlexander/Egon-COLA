package top.egon.cola.component.gateway.engine.http.cors;

import top.egon.cola.component.gateway.engine.http.service.GatewayOutboundHttpResponse;

import top.egon.cola.component.gateway.engine.http.domain.GatewayInboundHttpRequest;

import top.egon.cola.component.gateway.engine.http.cors.RuntimeCorsPolicy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 中文说明：{@code GatewayCorsProcessor} 是类型，位于当前 Gateway 模块的相关包中，负责网关CorsProcessor相关的职责与边界。
 * English summary: {@code GatewayCorsProcessor} is a type in the current Gateway module; it owns the gateway cors processor-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayCorsProcessor {

    /**
     * 中文说明：保存 policies 对应的状态、依赖或配置值；字段类型为 {@code Supplier<Map<String, RuntimeCorsPolicy>>}，由 {@code GatewayCorsProcessor} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by policies; its type is {@code Supplier<Map<String, RuntimeCorsPolicy>>}, and {@code GatewayCorsProcessor} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCorsProcessor} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCorsProcessor}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Supplier<Map<String, RuntimeCorsPolicy>> policies;

    /**
     * 中文说明：创建 {@code GatewayCorsProcessor} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayCorsProcessor} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param policies 参数 policies；parameter policies。
     */
    public GatewayCorsProcessor(
            Supplier<Map<String, RuntimeCorsPolicy>> policies) {
        this.policies = policies;
    }

    /**
     * 中文说明：执行 evaluate 操作；该方法是 {@code GatewayCorsProcessor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the evaluate operation; this method is the invocation entry point on {@code GatewayCorsProcessor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCorsProcessor.evaluate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policyRefs 参数 策略Refs；parameter policy refs。
     * @param request 参数 请求；parameter request。
     * @param routeMethod 参数 路由方法；parameter route method。
     * @param traceId 参数 traceId；parameter trace id。
     * @return 返回 evaluate 的处理结果；returns the result of the operation.
     */
    public Decision evaluate(
            Set<String> policyRefs,
            GatewayInboundHttpRequest request,
            String routeMethod,
            String traceId) {
        String origin = firstHeader(request.headers(), "origin");
        if (origin == null) {
            return Decision.none();
        }
        RuntimeCorsPolicy policy = referencedPolicy(policyRefs);
        if (policy == null || !policy.enabled()) {
            throw rejected("CORS policy is not configured");
        }
        if (!allowedOrigin(policy, origin)) {
            throw rejected("request origin is not allowed");
        }
        String method = routeMethod.toUpperCase(Locale.ROOT);
        if (!policy.allowedMethods().contains(method)) {
            throw rejected("request method is not allowed");
        }
        boolean preflight = "OPTIONS".equalsIgnoreCase(request.method())
                && firstHeader(
                request.headers(),
                "access-control-request-method"
        ) != null;
        Set<String> requestedHeaders = preflight
                ? requestedHeaders(request.headers())
                : Set.of();
        if (!headersAllowed(policy, requestedHeaders)) {
            throw rejected("request headers are not allowed");
        }
        Map<String, List<String>> corsHeaders = headers(
                policy,
                origin,
                preflight,
                requestedHeaders
        );
        if (!preflight) {
            return new Decision(null, corsHeaders);
        }
        return new Decision(
                new GatewayOutboundHttpResponse(
                        204,
                        corsHeaders,
                        reactor.core.publisher.Flux.empty()
                ),
                corsHeaders
        );
    }

    /**
     * 中文说明：执行 referenced策略 操作；该方法是 {@code GatewayCorsProcessor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the referenced policy operation; this method is the invocation entry point on {@code GatewayCorsProcessor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCorsProcessor.referencedPolicy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policyRefs 参数 策略Refs；parameter policy refs。
     * @return 返回 referenced策略 的处理结果；returns the result of the operation.
     */
    private RuntimeCorsPolicy referencedPolicy(Set<String> policyRefs) {
        List<RuntimeCorsPolicy> referenced = policyRefs.stream()
                .map(policies.get()::get)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (referenced.size() > 1) {
            throw rejected("multiple CORS policies are referenced");
        }
        return referenced.isEmpty() ? null : referenced.getFirst();
    }

    /**
     * 中文说明：执行 allowedOrigin 操作；该方法是 {@code GatewayCorsProcessor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the allowed origin operation; this method is the invocation entry point on {@code GatewayCorsProcessor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCorsProcessor.allowedOrigin(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policy 参数 策略；parameter policy。
     * @param origin 参数 origin；parameter origin。
     * @return 返回 allowedOrigin 的处理结果；returns the result of the operation.
     */
    private boolean allowedOrigin(
            RuntimeCorsPolicy policy,
            String origin) {
        return policy.allowedOrigins().contains("*")
                || policy.allowedOrigins().contains(origin);
    }

    /**
     * 中文说明：执行 headersAllowed 操作；该方法是 {@code GatewayCorsProcessor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the headers allowed operation; this method is the invocation entry point on {@code GatewayCorsProcessor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCorsProcessor.headersAllowed(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policy 参数 策略；parameter policy。
     * @param requestedHeaders 参数 requestedHeaders；parameter requested headers。
     * @return 返回 headersAllowed 的处理结果；returns the result of the operation.
     */
    private boolean headersAllowed(
            RuntimeCorsPolicy policy,
            Set<String> requestedHeaders) {
        if (requestedHeaders.isEmpty()) {
            return true;
        }
        Set<String> allowed = lowerCase(policy.allowedHeaders());
        return allowed.contains("*") || allowed.containsAll(requestedHeaders);
    }

    /**
     * 中文说明：执行 headers 操作；该方法是 {@code GatewayCorsProcessor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the headers operation; this method is the invocation entry point on {@code GatewayCorsProcessor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCorsProcessor.headers(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policy 参数 策略；parameter policy。
     * @param origin 参数 origin；parameter origin。
     * @param preflight 参数 preflight；parameter preflight。
     * @param requestedHeaders 参数 requestedHeaders；parameter requested headers。
     * @return 返回 headers 的处理结果；returns the result of the operation.
     */
    private Map<String, List<String>> headers(
            RuntimeCorsPolicy policy,
            String origin,
            boolean preflight,
            Set<String> requestedHeaders) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        result.put(
                "access-control-allow-origin",
                List.of(policy.allowedOrigins().contains("*") ? "*" : origin)
        );
        if (policy.allowCredentials()) {
            result.put(
                    "access-control-allow-credentials",
                    List.of("true")
            );
        }
        if (!policy.exposedHeaders().isEmpty()) {
            result.put(
                    "access-control-expose-headers",
                    List.of(String.join(", ", policy.exposedHeaders()))
            );
        }
        if (preflight) {
            result.put(
                    "access-control-allow-methods",
                    List.of(String.join(", ", policy.allowedMethods()))
            );
            if (!requestedHeaders.isEmpty()) {
                result.put(
                        "access-control-allow-headers",
                        List.of(String.join(", ", requestedHeaders))
                );
            }
            result.put(
                    "access-control-max-age",
                    List.of(Long.toString(policy.maxAgeSeconds()))
            );
            result.put(
                    "vary",
                    List.of(
                            "Origin",
                            "Access-Control-Request-Method",
                            "Access-Control-Request-Headers"
                    )
            );
        } else {
            result.put("vary", List.of("Origin"));
        }
        return Map.copyOf(result);
    }

    /**
     * 中文说明：执行 requestedHeaders 操作；该方法是 {@code GatewayCorsProcessor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the requested headers operation; this method is the invocation entry point on {@code GatewayCorsProcessor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCorsProcessor.requestedHeaders(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param headers 参数 headers；parameter headers。
     * @return 返回 requestedHeaders 的处理结果；returns the result of the operation.
     */
    private Set<String> requestedHeaders(
            Map<String, List<String>> headers) {
        String value = firstHeader(
                headers,
                "access-control-request-headers"
        );
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(header -> !header.isEmpty())
                .map(header -> header.toLowerCase(Locale.ROOT))
                .forEach(result::add);
        return Set.copyOf(result);
    }

    /**
     * 中文说明：执行 lowerCase 操作；该方法是 {@code GatewayCorsProcessor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the lower case operation; this method is the invocation entry point on {@code GatewayCorsProcessor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCorsProcessor.lowerCase(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param values 参数 values；parameter values。
     * @return 返回 lowerCase 的处理结果；returns the result of the operation.
     */
    private Set<String> lowerCase(Set<String> values) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        values.stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .forEach(result::add);
        return result;
    }

    /**
     * 中文说明：执行 firstHeader 操作；该方法是 {@code GatewayCorsProcessor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the first header operation; this method is the invocation entry point on {@code GatewayCorsProcessor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCorsProcessor.firstHeader(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param headers 参数 headers；parameter headers。
     * @param name 参数 name；parameter name。
     * @return 返回 firstHeader 的处理结果；returns the result of the operation.
     */
    private String firstHeader(
            Map<String, List<String>> headers,
            String name) {
        return headers.entrySet().stream()
                .filter(entry -> name.equalsIgnoreCase(entry.getKey()))
                .flatMap(entry -> entry.getValue().stream())
                .findFirst()
                .orElse(null);
    }

    /**
     * 中文说明：执行 rejected 操作；该方法是 {@code GatewayCorsProcessor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the rejected operation; this method is the invocation entry point on {@code GatewayCorsProcessor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCorsProcessor.rejected(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param message 参数 消息；parameter message。
     * @return 返回 rejected 的处理结果；returns the result of the operation.
     */
    private GatewayCorsException rejected(String message) {
        return new GatewayCorsException(message);
    }

    /**
     * 中文说明：{@code Decision} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Decision相关的职责与边界。
     * English summary: {@code Decision} is an immutable data carrier in the current Gateway module; it owns the decision-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param preflight 参数 preflight；parameter preflight。
     * @param responseHeaders 参数 响应Headers；parameter response headers。
     */
    public record Decision(
            /**
             * 中文说明：保存 preflight 对应的状态、依赖或配置值；字段类型为 {@code GatewayOutboundHttpResponse}，由 {@code GatewayCorsProcessor.Decision} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by preflight; its type is {@code GatewayOutboundHttpResponse}, and {@code GatewayCorsProcessor.Decision} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCorsProcessor.Decision} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCorsProcessor.Decision}; do not couple callers to its representation when the owning type exposes an API.
             */
            GatewayOutboundHttpResponse preflight,
            /**
             * 中文说明：保存 响应Headers 对应的状态、依赖或配置值；字段类型为 {@code Map<String, List<String>>}，由 {@code GatewayCorsProcessor.Decision} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by response headers; its type is {@code Map<String, List<String>>}, and {@code GatewayCorsProcessor.Decision} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCorsProcessor.Decision} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCorsProcessor.Decision}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, List<String>> responseHeaders
    ) {

        /**
         * 中文说明：执行 none 操作；该方法是 {@code GatewayCorsProcessor.Decision} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the none operation; this method is the invocation entry point on {@code GatewayCorsProcessor.Decision} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayCorsProcessor.Decision.none(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 none 的处理结果；returns the result of the operation.
         */
        private static Decision none() {
            return new Decision(null, Map.of());
        }

        /**
         * 中文说明：执行 preflight响应 操作；该方法是 {@code GatewayCorsProcessor.Decision} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the preflight response operation; this method is the invocation entry point on {@code GatewayCorsProcessor.Decision} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayCorsProcessor.Decision.preflightResponse(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 preflight响应 的处理结果；returns the result of the operation.
         */
        public Optional<GatewayOutboundHttpResponse> preflightResponse() {
            return Optional.ofNullable(preflight);
        }

        /**
         * 中文说明：执行 decorate 操作；该方法是 {@code GatewayCorsProcessor.Decision} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the decorate operation; this method is the invocation entry point on {@code GatewayCorsProcessor.Decision} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayCorsProcessor.Decision.decorate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param response 参数 响应；parameter response。
         * @return 返回 decorate 的处理结果；returns the result of the operation.
         */
        public GatewayOutboundHttpResponse decorate(
                GatewayOutboundHttpResponse response) {
            if (responseHeaders.isEmpty()) {
                return response;
            }
            Map<String, List<String>> headers = new LinkedHashMap<>(
                    response.headers()
            );
            headers.putAll(responseHeaders);
            return response.withHeadersAndBody(
                    headers,
                    response.body()
            );
        }
    }
}
