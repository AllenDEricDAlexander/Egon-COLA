package top.egon.cola.component.gateway.admin.routing.service;


import top.egon.cola.component.gateway.admin.application.controller.*;
import top.egon.cola.component.gateway.admin.application.domain.dto.*;
import top.egon.cola.component.gateway.admin.application.domain.exception.*;
import top.egon.cola.component.gateway.admin.application.domain.po.*;
import top.egon.cola.component.gateway.admin.application.domain.vo.*;
import top.egon.cola.component.gateway.admin.application.repository.*;
import top.egon.cola.component.gateway.admin.application.service.*;
import top.egon.cola.component.gateway.admin.auth.controller.*;
import top.egon.cola.component.gateway.admin.auth.domain.vo.*;
import top.egon.cola.component.gateway.admin.auth.service.*;
import top.egon.cola.component.gateway.admin.bootstrap.*;
import top.egon.cola.component.gateway.admin.catalog.controller.*;
import top.egon.cola.component.gateway.admin.catalog.domain.dto.*;
import top.egon.cola.component.gateway.admin.catalog.domain.enums.*;
import top.egon.cola.component.gateway.admin.catalog.domain.po.*;
import top.egon.cola.component.gateway.admin.catalog.domain.vo.*;
import top.egon.cola.component.gateway.admin.catalog.repository.*;
import top.egon.cola.component.gateway.admin.catalog.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.catalog.service.*;
import top.egon.cola.component.gateway.admin.config.*;
import top.egon.cola.component.gateway.admin.config.properties.*;
import top.egon.cola.component.gateway.admin.credential.controller.*;
import top.egon.cola.component.gateway.admin.credential.domain.dto.*;
import top.egon.cola.component.gateway.admin.credential.domain.po.*;
import top.egon.cola.component.gateway.admin.credential.domain.vo.*;
import top.egon.cola.component.gateway.admin.credential.repository.*;
import top.egon.cola.component.gateway.admin.credential.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.credential.service.*;
import top.egon.cola.component.gateway.admin.group.controller.*;
import top.egon.cola.component.gateway.admin.group.domain.dto.*;
import top.egon.cola.component.gateway.admin.group.domain.po.*;
import top.egon.cola.component.gateway.admin.group.domain.vo.*;
import top.egon.cola.component.gateway.admin.group.repository.*;
import top.egon.cola.component.gateway.admin.group.service.*;
import top.egon.cola.component.gateway.admin.mcp.controller.*;
import top.egon.cola.component.gateway.admin.mcp.domain.dto.*;
import top.egon.cola.component.gateway.admin.mcp.domain.enums.*;
import top.egon.cola.component.gateway.admin.mcp.domain.exception.*;
import top.egon.cola.component.gateway.admin.mcp.domain.po.*;
import top.egon.cola.component.gateway.admin.mcp.domain.vo.*;
import top.egon.cola.component.gateway.admin.mcp.repository.*;
import top.egon.cola.component.gateway.admin.mcp.repository.filesystem.*;
import top.egon.cola.component.gateway.admin.mcp.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.mcp.service.*;
import top.egon.cola.component.gateway.admin.observability.controller.*;
import top.egon.cola.component.gateway.admin.observability.controller.message.*;
import top.egon.cola.component.gateway.admin.observability.controller.scheduled.*;
import top.egon.cola.component.gateway.admin.observability.domain.dto.*;
import top.egon.cola.component.gateway.admin.observability.domain.enums.*;
import top.egon.cola.component.gateway.admin.observability.domain.po.*;
import top.egon.cola.component.gateway.admin.observability.domain.vo.*;
import top.egon.cola.component.gateway.admin.observability.repository.*;
import top.egon.cola.component.gateway.admin.observability.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.observability.service.*;
import top.egon.cola.component.gateway.admin.release.controller.*;
import top.egon.cola.component.gateway.admin.release.controller.scheduled.*;
import top.egon.cola.component.gateway.admin.release.domain.*;
import top.egon.cola.component.gateway.admin.release.domain.dto.*;
import top.egon.cola.component.gateway.admin.release.domain.enums.*;
import top.egon.cola.component.gateway.admin.release.domain.po.*;
import top.egon.cola.component.gateway.admin.release.domain.vo.*;
import top.egon.cola.component.gateway.admin.release.repository.*;
import top.egon.cola.component.gateway.admin.release.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.release.service.*;
import top.egon.cola.component.gateway.admin.reporting.controller.openapi.*;
import top.egon.cola.component.gateway.admin.reporting.controller.scheduled.*;
import top.egon.cola.component.gateway.admin.reporting.domain.dto.*;
import top.egon.cola.component.gateway.admin.reporting.domain.po.*;
import top.egon.cola.component.gateway.admin.reporting.domain.vo.*;
import top.egon.cola.component.gateway.admin.reporting.repository.*;
import top.egon.cola.component.gateway.admin.reporting.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.reporting.service.*;
import top.egon.cola.component.gateway.admin.routing.controller.*;
import top.egon.cola.component.gateway.admin.routing.domain.*;
import top.egon.cola.component.gateway.admin.routing.domain.dto.*;
import top.egon.cola.component.gateway.admin.routing.domain.po.*;
import top.egon.cola.component.gateway.admin.routing.domain.vo.*;
import top.egon.cola.component.gateway.admin.routing.repository.*;
import top.egon.cola.component.gateway.admin.routing.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.routing.service.*;
import top.egon.cola.component.gateway.admin.rule.domain.dto.*;
import top.egon.cola.component.gateway.admin.rule.domain.vo.*;
import top.egon.cola.component.gateway.admin.rule.service.*;
import top.egon.cola.component.gateway.admin.runtime.controller.*;
import top.egon.cola.component.gateway.admin.runtime.domain.dto.*;
import top.egon.cola.component.gateway.admin.runtime.domain.vo.*;
import top.egon.cola.component.gateway.admin.runtime.service.*;
import top.egon.cola.component.gateway.admin.scope.controller.*;
import top.egon.cola.component.gateway.admin.scope.domain.*;
import top.egon.cola.component.gateway.admin.scope.domain.dto.*;
import top.egon.cola.component.gateway.admin.scope.domain.vo.*;
import top.egon.cola.component.gateway.admin.scope.service.*;
import top.egon.cola.component.gateway.admin.shared.controller.*;
import top.egon.cola.component.gateway.admin.shared.domain.*;
import top.egon.cola.component.gateway.admin.shared.domain.enums.*;
import top.egon.cola.component.gateway.admin.shared.domain.exception.*;
import top.egon.cola.component.gateway.admin.shared.domain.po.*;
import top.egon.cola.component.gateway.admin.shared.domain.vo.*;
import top.egon.cola.component.gateway.admin.shared.repository.*;
import top.egon.cola.component.gateway.admin.shared.repository.jdbc.*;
import top.egon.cola.component.gateway.contract.rule.GatewayRequestBodyMode;
import top.egon.cola.component.gateway.contract.rule.GatewayRouteProfile;
import top.egon.cola.component.gateway.contract.rule.GatewayRouteTransportPolicy;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportResponseMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Keeps the route draft JSON boundary compatible while emitting one canonical
 * shape for new writes and release compilation.
 * 补充说明 / Supplementary summary: {@code GatewayRouteDraftMapper} 是映射器，位于当前 Gateway 模块的相关包中，负责网关路由草稿映射器相关的职责与边界。
 * English supplement: {@code GatewayRouteDraftMapper} is a gateway route draft mapper mapper in the current Gateway module; it owns the gateway route draft mapper-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayRouteDraftMapper {

    /**
     * 中文说明：表示 LEGACYKEYS 这一固定值；它属于 {@code GatewayRouteDraftMapper} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value legacy keys; it is a state, type, or protocol value of {@code GatewayRouteDraftMapper} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayRouteDraftMapper} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRouteDraftMapper}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final Set<String> LEGACY_KEYS = Set.of(
            "listener",
            "method",
            "path",
            "protocol",
            "fullMethodName",
            "providerServiceName",
            "operationExternalAccessible"
    );

    /**
     * 中文说明：执行 canonicalize 操作；该方法是 {@code GatewayRouteDraftMapper} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the canonicalize operation; this method is the invocation entry point on {@code GatewayRouteDraftMapper} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRouteDraftMapper.canonicalize(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param content 参数 content；parameter content。
     * @return 返回 canonicalize 的处理结果；returns the result of the operation.
     */
    public Map<String, Object> canonicalize(Map<String, Object> content) {
        Objects.requireNonNull(content, "content");
        Map<String, Object> canonical = new LinkedHashMap<>(content);
        LEGACY_KEYS.forEach(canonical::remove);

        putText(canonical, "host", content.get("host"), false);
        putText(
                canonical,
                "httpMethod",
                canonicalOrLegacy(content, "httpMethod", "method"),
                true
        );
        putText(
                canonical,
                "pathPattern",
                canonicalOrLegacy(content, "pathPattern", "path"),
                false
        );
        putAccessZones(canonical, content);
        canonical.put(
                "priority",
                content.get("priority") == null ? 0 : content.get("priority")
        );
        copyTransportPolicy(canonical, content.get("transportPolicy"));
        return Collections.unmodifiableMap(canonical);
    }

    /**
     * 中文说明：执行 传输策略 操作；该方法是 {@code GatewayRouteDraftMapper} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the transport policy operation; this method is the invocation entry point on {@code GatewayRouteDraftMapper} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRouteDraftMapper.transportPolicy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param canonicalContent 参数 canonicalContent；parameter canonical content。
     * @return 返回 传输策略 的处理结果；returns the result of the operation.
     */
    public GatewayRouteTransportPolicy transportPolicy(
            Map<String, Object> canonicalContent) {
        Object raw = canonicalContent.get("transportPolicy");
        if (raw == null) {
            return null;
        }
        if (!(raw instanceof Map<?, ?> policy)) {
            throw new IllegalArgumentException(
                    "transportPolicy must be an object"
            );
        }
        return new GatewayRouteTransportPolicy(
                enumeration(policy, "profile", GatewayRouteProfile.class),
                enumeration(
                        policy,
                        "transportProtocol",
                        GatewayTransportProtocol.class
                ),
                enumeration(
                        policy,
                        "requestBodyMode",
                        GatewayRequestBodyMode.class
                ),
                enumeration(
                        policy,
                        "responseMode",
                        GatewayTransportResponseMode.class
                ),
                number(policy, "maxRequestBodyBytes"),
                number(policy, "connectTimeoutMs"),
                number(policy, "responseHeaderTimeoutMs"),
                number(policy, "streamIdleTimeoutMs"),
                number(policy, "totalTimeoutMs"),
                number(policy, "websocketIdleTimeoutMs"),
                number(policy, "websocketMaxFrameBytes"),
                flag(policy, "bodyLogEnabled"),
                flag(policy, "retryEnabled")
        );
    }

    /**
     * 中文说明：执行 putAccessZones 操作；该方法是 {@code GatewayRouteDraftMapper} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the put access zones operation; this method is the invocation entry point on {@code GatewayRouteDraftMapper} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRouteDraftMapper.putAccessZones(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param canonical 参数 canonical；parameter canonical。
     * @param content 参数 content；parameter content。
     */
    private void putAccessZones(
            Map<String, Object> canonical,
            Map<String, Object> content) {
        if (content.containsKey("accessZones")) {
            Object zones = content.get("accessZones");
            if (zones instanceof Iterable<?> values) {
                canonical.put("accessZones", normalizedZones(values));
            }
            return;
        }
        Object listener = content.get("listener");
        if (listener == null) {
            canonical.remove("accessZones");
            return;
        }
        if (!(listener instanceof String)) {
            canonical.put("accessZones", List.of(listener));
            return;
        }
        String normalized = text(listener);
        if (normalized == null) {
            canonical.remove("accessZones");
            return;
        }
        canonical.put(
                "accessZones",
                List.of(normalized.toUpperCase(Locale.ROOT))
        );
    }

    /**
     * 中文说明：执行 normalizedZones 操作；该方法是 {@code GatewayRouteDraftMapper} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the normalized zones operation; this method is the invocation entry point on {@code GatewayRouteDraftMapper} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRouteDraftMapper.normalizedZones(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param values 参数 values；parameter values。
     * @return 返回 normalizedZones 的处理结果；returns the result of the operation.
     */
    private List<Object> normalizedZones(Iterable<?> values) {
        LinkedHashSet<Object> zones = new LinkedHashSet<>();
        for (Object value : values) {
            String zone = text(value);
            zones.add(zone == null
                    ? value
                    : zone.toUpperCase(Locale.ROOT));
        }
        return Collections.unmodifiableList(new ArrayList<>(zones));
    }

    /**
     * 中文说明：执行 copy传输策略 操作；该方法是 {@code GatewayRouteDraftMapper} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the copy transport policy operation; this method is the invocation entry point on {@code GatewayRouteDraftMapper} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRouteDraftMapper.copyTransportPolicy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param canonical 参数 canonical；parameter canonical。
     * @param raw 参数 raw；parameter raw。
     */
    private void copyTransportPolicy(
            Map<String, Object> canonical,
            Object raw) {
        if (raw == null) {
            canonical.remove("transportPolicy");
            return;
        }
        if (raw instanceof Map<?, ?> policy) {
            Map<String, Object> copy = new LinkedHashMap<>();
            policy.forEach((key, value) -> {
                if (key instanceof String field) {
                    copy.put(field, value);
                }
            });
            canonical.put(
                    "transportPolicy",
                    Collections.unmodifiableMap(copy)
            );
        }
    }

    /**
     * 中文说明：执行 canonicalOrLegacy 操作；该方法是 {@code GatewayRouteDraftMapper} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the canonical or legacy operation; this method is the invocation entry point on {@code GatewayRouteDraftMapper} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRouteDraftMapper.canonicalOrLegacy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param content 参数 content；parameter content。
     * @param canonical 参数 canonical；parameter canonical。
     * @param legacy 参数 legacy；parameter legacy。
     * @return 返回 canonicalOrLegacy 的处理结果；returns the result of the operation.
     */
    private Object canonicalOrLegacy(
            Map<String, Object> content,
            String canonical,
            String legacy) {
        return content.containsKey(canonical)
                ? content.get(canonical)
                : content.get(legacy);
    }

    /**
     * 中文说明：执行 putText 操作；该方法是 {@code GatewayRouteDraftMapper} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the put text operation; this method is the invocation entry point on {@code GatewayRouteDraftMapper} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRouteDraftMapper.putText(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param canonical 参数 canonical；parameter canonical。
     * @param key 参数 键；parameter key。
     * @param value 参数 值；parameter value。
     * @param uppercase 参数 uppercase；parameter uppercase。
     */
    private void putText(
            Map<String, Object> canonical,
            String key,
            Object value,
            boolean uppercase) {
        if (value == null) {
            canonical.remove(key);
            return;
        }
        if (!(value instanceof String)) {
            canonical.put(key, value);
            return;
        }
        String normalized = text(value);
        if (normalized == null) {
            canonical.remove(key);
            return;
        }
        canonical.put(
                key,
                uppercase
                        ? normalized.toUpperCase(Locale.ROOT)
                        : normalized
        );
    }

    /**
     * 中文说明：执行 text 操作；该方法是 {@code GatewayRouteDraftMapper} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the text operation; this method is the invocation entry point on {@code GatewayRouteDraftMapper} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRouteDraftMapper.text(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 text 的处理结果；returns the result of the operation.
     */
    private String text(Object value) {
        if (!(value instanceof String text) || text.isBlank()) {
            return null;
        }
        return text.trim();
    }

    /**
     * 中文说明：执行 enumeration 操作；该方法是 {@code GatewayRouteDraftMapper} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the enumeration operation; this method is the invocation entry point on {@code GatewayRouteDraftMapper} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRouteDraftMapper.enumeration(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policy 参数 策略；parameter policy。
     * @param field 参数 field；parameter field。
     * @param type 参数 type；parameter type。
     * @return 返回 enumeration 的处理结果；returns the result of the operation.
     */
    private <E extends Enum<E>> E enumeration(
            Map<?, ?> policy,
            String field,
            Class<E> type) {
        Object value = policy.get(field);
        if (value == null) {
            return null;
        }
        if (!(value instanceof String text)) {
            throw new IllegalArgumentException(
                    "transportPolicy." + field + " must be a string"
            );
        }
        try {
            return Enum.valueOf(type, text);
        } catch (IllegalArgumentException unknown) {
            throw new IllegalArgumentException(
                    "transportPolicy." + field + " contains an unknown value",
                    unknown
            );
        }
    }

    /**
     * 中文说明：执行 number 操作；该方法是 {@code GatewayRouteDraftMapper} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the number operation; this method is the invocation entry point on {@code GatewayRouteDraftMapper} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRouteDraftMapper.number(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policy 参数 策略；parameter policy。
     * @param field 参数 field；parameter field。
     * @return 返回 number 的处理结果；returns the result of the operation.
     */
    private Long number(Map<?, ?> policy, String field) {
        Object value = policy.get(field);
        if (value == null) {
            return null;
        }
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(
                    "transportPolicy." + field + " must be an integer"
            );
        }
        return number.longValue();
    }

    /**
     * 中文说明：执行 flag 操作；该方法是 {@code GatewayRouteDraftMapper} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the flag operation; this method is the invocation entry point on {@code GatewayRouteDraftMapper} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRouteDraftMapper.flag(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policy 参数 策略；parameter policy。
     * @param field 参数 field；parameter field。
     * @return 返回 flag 的处理结果；returns the result of the operation.
     */
    private Boolean flag(Map<?, ?> policy, String field) {
        Object value = policy.get(field);
        if (value == null) {
            return null;
        }
        if (!(value instanceof Boolean flag)) {
            throw new IllegalArgumentException(
                    "transportPolicy." + field + " must be a boolean"
            );
        }
        return flag;
    }
}
