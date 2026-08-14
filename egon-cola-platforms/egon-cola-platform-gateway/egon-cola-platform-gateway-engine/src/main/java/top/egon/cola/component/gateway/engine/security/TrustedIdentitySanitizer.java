package top.egon.cola.component.gateway.engine.security;

import top.egon.cola.component.gateway.core.security.TrustedIdentity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 中文说明：{@code TrustedIdentitySanitizer} 是类型，位于当前 Gateway 模块的相关包中，负责Trusted身份Sanitizer相关的职责与边界。
 * English summary: {@code TrustedIdentitySanitizer} is a type in the current Gateway module; it owns the trusted identity sanitizer-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class TrustedIdentitySanitizer {

    /**
     * 中文说明：表示 FIXEDSENSITIVE 这一固定值；它属于 {@code TrustedIdentitySanitizer} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value fixed sensitive; it is a state, type, or protocol value of {@code TrustedIdentitySanitizer} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code TrustedIdentitySanitizer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code TrustedIdentitySanitizer}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final Set<String> FIXED_SENSITIVE = Set.of(
            "authorization",
            "proxy-authorization",
            "cookie",
            "set-cookie",
            "x-gateway-principal-id",
            "x-gateway-tenant-id",
            "x-gateway-authenticated",
            "x-gateway-auth-provider",
            "x-gateway-access-zone",
            "x-internal-request",
            "x-forwarded-internal",
            "gateway-access-zone",
            "gateway-principal-id",
            "gateway-tenant-id"
    );

    /**
     * 中文说明：表示 IDPTRUSTEDHTTP 这一固定值；它属于 {@code TrustedIdentitySanitizer} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value idp trusted http; it is a state, type, or protocol value of {@code TrustedIdentitySanitizer} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code TrustedIdentitySanitizer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code TrustedIdentitySanitizer}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final Set<String> IDP_TRUSTED_HTTP = Set.of(
            "x-egon-principal-type",
            "x-egon-identity-sub",
            "x-egon-tenant-id",
            "x-egon-client-id",
            "x-egon-token-id",
            "x-egon-resource-uri",
            "x-egon-resource-version",
            "x-egon-source-biz",
            "x-egon-source-app",
            "x-egon-source-env",
            "x-egon-service-scopes",
            "x-egon-credential-id"
    );

    /**
     * 中文说明：表示 HOPBYHOP 这一固定值；它属于 {@code TrustedIdentitySanitizer} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value hop by hop; it is a state, type, or protocol value of {@code TrustedIdentitySanitizer} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code TrustedIdentitySanitizer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code TrustedIdentitySanitizer}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final Set<String> HOP_BY_HOP = Set.of(
            "connection",
            "keep-alive",
            "proxy-authenticate",
            "te",
            "trailer",
            "transfer-encoding",
            "upgrade"
    );

    /**
     * 中文说明：执行 sanitizeHttp 操作；该方法是 {@code TrustedIdentitySanitizer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the sanitize http operation; this method is the invocation entry point on {@code TrustedIdentitySanitizer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code TrustedIdentitySanitizer.sanitizeHttp(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param fieldsToRemove 参数 fieldsToRemove；parameter fields to remove。
     * @param identity 参数 身份；parameter identity。
     * @return 返回 sanitizeHttp 的处理结果；returns the result of the operation.
     */
    public Map<String, List<String>> sanitizeHttp(
            Map<String, List<String>> source,
            Set<String> fieldsToRemove,
            TrustedIdentity identity) {
        return sanitizeHttp(source, fieldsToRemove, identity, false);
    }

    /**
     * 中文说明：执行 sanitizeHttp 操作；该方法是 {@code TrustedIdentitySanitizer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the sanitize http operation; this method is the invocation entry point on {@code TrustedIdentitySanitizer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code TrustedIdentitySanitizer.sanitizeHttp(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param fieldsToRemove 参数 fieldsToRemove；parameter fields to remove。
     * @param identity 参数 身份；parameter identity。
     * @param authorizationForwardingAllowed 参数 授权ForwardingAllowed；parameter authorization forwarding allowed。
     * @return 返回 sanitizeHttp 的处理结果；returns the result of the operation.
     */
    public Map<String, List<String>> sanitizeHttp(
            Map<String, List<String>> source,
            Set<String> fieldsToRemove,
            TrustedIdentity identity,
            boolean authorizationForwardingAllowed) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(identity, "identity");
        Set<String> removals = normalized(fieldsToRemove);
        Map<String, List<String>> result = new LinkedHashMap<>();
        source.forEach((name, values) -> {
            String lower = normalizedName(name);
            if (safeInbound(
                    lower,
                    removals,
                    authorizationForwardingAllowed
            )) {
                result.put(lower, List.copyOf(values));
            }
        });
        if (identity.httpHeaders().size() > 16) {
            throw new IllegalArgumentException(
                    "trusted HTTP identity field count exceeds 16"
            );
        }
        identity.httpHeaders().forEach((name, value) -> {
            String lower = normalizedName(name);
            if (!lower.startsWith("x-egon-gateway-")
                    && !IDP_TRUSTED_HTTP.contains(lower)) {
                throw new IllegalArgumentException(
                        "untrusted HTTP identity field " + name
                );
            }
            result.put(lower, List.of(safeValue(value)));
        });
        return Map.copyOf(result);
    }

    /**
     * 中文说明：执行 sanitizeRpc 操作；该方法是 {@code TrustedIdentitySanitizer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the sanitize rpc operation; this method is the invocation entry point on {@code TrustedIdentitySanitizer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code TrustedIdentitySanitizer.sanitizeRpc(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param fieldsToRemove 参数 fieldsToRemove；parameter fields to remove。
     * @param identity 参数 身份；parameter identity。
     * @return 返回 sanitizeRpc 的处理结果；returns the result of the operation.
     */
    public Map<String, String> sanitizeRpc(
            Map<String, String> source,
            Set<String> fieldsToRemove,
            TrustedIdentity identity) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(identity, "identity");
        Set<String> removals = normalized(fieldsToRemove);
        Map<String, String> result = new LinkedHashMap<>();
        source.forEach((name, value) -> {
            String lower = normalizedName(name);
            if (safeInbound(lower, removals, false)
                    && !lower.startsWith("egon-gateway-")) {
                result.put(lower, safeValue(value));
            }
        });
        if (identity.rpcMetadata().size() > 16) {
            throw new IllegalArgumentException(
                    "trusted RPC identity field count exceeds 16"
            );
        }
        identity.rpcMetadata().forEach((name, value) -> {
            String lower = normalizedName(name);
            if (!lower.startsWith("egon-gateway-")) {
                throw new IllegalArgumentException(
                        "untrusted RPC identity field " + name
                );
            }
            result.put(lower, safeValue(value));
        });
        return Map.copyOf(result);
    }

    /**
     * 中文说明：执行 safeInbound 操作；该方法是 {@code TrustedIdentitySanitizer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the safe inbound operation; this method is the invocation entry point on {@code TrustedIdentitySanitizer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code TrustedIdentitySanitizer.safeInbound(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param name 参数 name；parameter name。
     * @param removals 参数 removals；parameter removals。
     * @param authorizationForwardingAllowed 参数 授权ForwardingAllowed；parameter authorization forwarding allowed。
     * @return 返回 safeInbound 的处理结果；returns the result of the operation.
     */
    private boolean safeInbound(
            String name,
            Set<String> removals,
            boolean authorizationForwardingAllowed) {
        boolean fixedSensitive = FIXED_SENSITIVE.contains(name)
                || IDP_TRUSTED_HTTP.contains(name);
        fixedSensitive = fixedSensitive
                && !(authorizationForwardingAllowed
                && "authorization".equals(name));
        return !fixedSensitive
                && !HOP_BY_HOP.contains(name)
                && !removals.contains(name)
                && !name.startsWith("x-egon-gateway-")
                && !name.startsWith("x-forwarded-");
    }

    /**
     * 中文说明：执行 normalized 操作；该方法是 {@code TrustedIdentitySanitizer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the normalized operation; this method is the invocation entry point on {@code TrustedIdentitySanitizer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code TrustedIdentitySanitizer.normalized(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param names 参数 names；parameter names。
     * @return 返回 normalized 的处理结果；returns the result of the operation.
     */
    private Set<String> normalized(Set<String> names) {
        if (names == null || names.isEmpty()) {
            return Set.of();
        }
        return names.stream()
                .map(this::normalizedName)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * 中文说明：执行 normalizedName 操作；该方法是 {@code TrustedIdentitySanitizer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the normalized name operation; this method is the invocation entry point on {@code TrustedIdentitySanitizer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code TrustedIdentitySanitizer.normalizedName(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 normalizedName 的处理结果；returns the result of the operation.
     */
    private String normalizedName(String value) {
        if (value == null || value.isBlank() || value.length() > 128) {
            throw new IllegalArgumentException("invalid metadata name");
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9!#$%&'*+.^_`|~-]+")) {
            throw new IllegalArgumentException("invalid metadata name");
        }
        return normalized;
    }

    /**
     * 中文说明：执行 safe值 操作；该方法是 {@code TrustedIdentitySanitizer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the safe value operation; this method is the invocation entry point on {@code TrustedIdentitySanitizer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code TrustedIdentitySanitizer.safeValue(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 safe值 的处理结果；returns the result of the operation.
     */
    private String safeValue(String value) {
        if (value == null
                || value.length() > 1024
                || value.indexOf('\r') >= 0
                || value.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("invalid metadata value");
        }
        return value;
    }
}
