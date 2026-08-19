package top.egon.cola.component.gateway.engine.mcp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.egon.cola.component.gateway.mcp.common.security.McpSecurityDigests;
import top.egon.cola.component.gateway.mcp.common.telemetry.McpTelemetry;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Emits sanitized runtime audit JSON without bodies, arguments or credentials.
 * 补充说明 / Supplementary summary: {@code McpAuditPublisher} 是类型，位于当前 Gateway 模块的相关包中，负责MCP审计发布器相关的职责与边界。
 * English supplement: {@code McpAuditPublisher} is a type in the current Gateway module; it owns the mcp audit publisher-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpAuditPublisher implements McpTelemetry {

    /**
     * 中文说明：表示 LOGGER 这一固定值；它属于 {@code McpAuditPublisher} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value logger; it is a state, type, or protocol value of {@code McpAuditPublisher} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code McpAuditPublisher} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAuditPublisher}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            McpAuditPublisher.class
    );

    /**
     * 中文说明：保存 object映射器 对应的状态、依赖或配置值；字段类型为 {@code ObjectMapper}，由 {@code McpAuditPublisher} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by object mapper; its type is {@code ObjectMapper}, and {@code McpAuditPublisher} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpAuditPublisher} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAuditPublisher}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：保存 clock 对应的状态、依赖或配置值；字段类型为 {@code Clock}，由 {@code McpAuditPublisher} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clock; its type is {@code Clock}, and {@code McpAuditPublisher} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpAuditPublisher} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAuditPublisher}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Clock clock;

    /**
     * 中文说明：保存 sink 对应的状态、依赖或配置值；字段类型为 {@code AuditSink}，由 {@code McpAuditPublisher} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by sink; its type is {@code AuditSink}, and {@code McpAuditPublisher} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpAuditPublisher} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAuditPublisher}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final AuditSink sink;

    /**
     * 中文说明：创建 {@code McpAuditPublisher} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpAuditPublisher} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @param clock 参数 clock；parameter clock。
     * @param sink 参数 sink；parameter sink。
     */
    public McpAuditPublisher(
            ObjectMapper objectMapper,
            Clock clock,
            AuditSink sink) {
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        ).copy();
        this.clock = Objects.requireNonNull(clock, "clock");
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    /**
     * 中文说明：执行 start 操作；该方法是 {@code McpAuditPublisher} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the start operation; this method is the invocation entry point on {@code McpAuditPublisher} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpAuditPublisher.start(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 start 的处理结果；returns the result of the operation.
     */
    @Override
    public Scope start(Request request) {
        Objects.requireNonNull(request, "request");
        Instant startedAt = clock.instant();
        return new Scope() {
            /**
             * 中文说明：保存 completed 对应的状态、依赖或配置值；字段类型为 {@code AtomicBoolean}，由 {@code McpAuditPublisher} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by completed; its type is {@code AtomicBoolean}, and {@code McpAuditPublisher} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpAuditPublisher} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAuditPublisher}; do not couple callers to its representation when the owning type exposes an API.
             */
            private final AtomicBoolean completed = new AtomicBoolean();

            /**
             * 中文说明：保存 远程提供方 对应的状态、依赖或配置值；字段类型为 {@code AtomicReference<String>}，由 {@code McpAuditPublisher} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by remote provider; its type is {@code AtomicReference<String>}, and {@code McpAuditPublisher} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpAuditPublisher} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAuditPublisher}; do not couple callers to its representation when the owning type exposes an API.
             */
            private final AtomicReference<String> remoteProvider =
                    new AtomicReference<>(request.remoteProviderCode());

            /**
             * 中文说明：执行 远程提供方 操作；该方法是 {@code McpAuditPublisher} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
             * English summary: Executes the remote provider operation; this method is the invocation entry point on {@code McpAuditPublisher} and performs the corresponding runtime, management, or protocol work.
             *
             * 用法 / Usage: 调用方式 / Usage: {@code McpAuditPublisher.remoteProvider(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
             * @param providerCode 参数 提供方Code；parameter provider code。
             */
            @Override
            public void remoteProvider(String providerCode) {
                String normalized = code(providerCode);
                if (normalized != null) {
                    remoteProvider.set(normalized);
                }
            }

            /**
             * 中文说明：执行 startChild 操作；该方法是 {@code McpAuditPublisher} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
             * English summary: Executes the start child operation; this method is the invocation entry point on {@code McpAuditPublisher} and performs the corresponding runtime, management, or protocol work.
             *
             * 用法 / Usage: 调用方式 / Usage: {@code McpAuditPublisher.startChild(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
             * @param kind 参数 kind；parameter kind。
             * @return 返回 startChild 的处理结果；returns the result of the operation.
             */
            @Override
            public Child startChild(ChildKind kind) {
                return Child.noop();
            }

            /**
             * 中文说明：执行 success 操作；该方法是 {@code McpAuditPublisher} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
             * English summary: Executes the success operation; this method is the invocation entry point on {@code McpAuditPublisher} and performs the corresponding runtime, management, or protocol work.
             *
             * 用法 / Usage: 调用方式 / Usage: {@code McpAuditPublisher.success(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
             */
            @Override
            public void success() {
                publish("SUCCESS");
            }

            /**
             * 中文说明：执行 failure 操作；该方法是 {@code McpAuditPublisher} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
             * English summary: Executes the failure operation; this method is the invocation entry point on {@code McpAuditPublisher} and performs the corresponding runtime, management, or protocol work.
             *
             * 用法 / Usage: 调用方式 / Usage: {@code McpAuditPublisher.failure(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
             * @param errorCode 参数 errorCode；parameter error code。
             */
            @Override
            public void failure(String errorCode) {
                publish(safeStatus(errorCode));
            }

            /**
             * 中文说明：执行 publish 操作；该方法是 {@code McpAuditPublisher} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
             * English summary: Executes the publish operation; this method is the invocation entry point on {@code McpAuditPublisher} and performs the corresponding runtime, management, or protocol work.
             *
             * 用法 / Usage: 调用方式 / Usage: {@code McpAuditPublisher.publish(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
             * @param status 参数 status；parameter status。
             */
            private void publish(String status) {
                if (!completed.compareAndSet(false, true)) {
                    return;
                }
                try {
                    sink.publish(objectMapper.writeValueAsString(event(
                            request,
                            startedAt,
                            status,
                            remoteProvider.get()
                    )));
                } catch (Exception failure) {
                    LOGGER.warn("MCP runtime audit publication failed");
                }
            }
        };
    }

    /**
     * 中文说明：执行 事件 操作；该方法是 {@code McpAuditPublisher} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the event operation; this method is the invocation entry point on {@code McpAuditPublisher} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpAuditPublisher.event(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param startedAt 参数 startedAt；parameter started at。
     * @param status 参数 status；parameter status。
     * @param remoteProviderCode 参数 远程提供方Code；parameter remote provider code。
     * @return 返回 事件 的处理结果；returns the result of the operation.
     */
    private Map<String, Object> event(
            Request request,
            Instant startedAt,
            String status,
            String remoteProviderCode) {
        LinkedHashMap<String, Object> event = new LinkedHashMap<>();
        event.put("eventType", "mcp.runtime.request");
        event.put("occurredAt", startedAt.toString());
        event.put("method", request.method());
        event.put("primitive", request.primitive());
        event.put("serverCode", request.serverCode());
        String remoteProvider = code(remoteProviderCode);
        if (remoteProvider != null) {
            event.put("remoteProviderCode", remoteProvider);
        }
        event.put("status", status);
        String actor = attribute(
                request.attributes(),
                "callerId",
                "identity.subject"
        );
        if (actor != null) {
            event.put(
                    "actorFingerprint",
                    McpSecurityDigests.token(actor)
            );
        }
        putIfPresent(
                event,
                "tenantId",
                codeAttribute(
                        request.attributes(),
                        "tenantId",
                        "identity.tenant-id"
                )
        );
        putIfPresent(
                event,
                "clientId",
                codeAttribute(
                        request.attributes(),
                        "idp.client-id",
                        "identity.client-id"
                )
        );
        putIfPresent(
                event,
                "traceId",
                trace(request.attributes())
        );
        return Map.copyOf(event);
    }

    /**
     * 中文说明：执行 codeAttribute 操作；该方法是 {@code McpAuditPublisher} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the code attribute operation; this method is the invocation entry point on {@code McpAuditPublisher} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpAuditPublisher.codeAttribute(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param attributes 参数 attributes；parameter attributes。
     * @param names 参数 names；parameter names。
     * @return 返回 codeAttribute 的处理结果；returns the result of the operation.
     */
    private String codeAttribute(
            Map<String, Object> attributes,
            String... names) {
        return code(attribute(attributes, names));
    }

    /**
     * 中文说明：执行 code 操作；该方法是 {@code McpAuditPublisher} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the code operation; this method is the invocation entry point on {@code McpAuditPublisher} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpAuditPublisher.code(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 code 的处理结果；returns the result of the operation.
     */
    private String code(String value) {
        if (value == null || !value.matches(
                "[A-Za-z0-9][A-Za-z0-9_.:-]{0,127}"
        )) {
            return null;
        }
        return value;
    }

    /**
     * 中文说明：执行 trace 操作；该方法是 {@code McpAuditPublisher} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the trace operation; this method is the invocation entry point on {@code McpAuditPublisher} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpAuditPublisher.trace(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param attributes 参数 attributes；parameter attributes。
     * @return 返回 trace 的处理结果；returns the result of the operation.
     */
    private String trace(Map<String, Object> attributes) {
        String requestId = codeAttribute(attributes, "x-egon-request-id");
        if (requestId != null) {
            return requestId;
        }
        String traceparent = attribute(attributes, "traceparent");
        return traceparent != null && traceparent.matches(
                "[0-9a-fA-F]{2}-[0-9a-fA-F]{32}-"
                        + "[0-9a-fA-F]{16}-[0-9a-fA-F]{2}"
        ) ? traceparent : null;
    }

    /**
     * 中文说明：执行 putIfPresent 操作；该方法是 {@code McpAuditPublisher} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the put if present operation; this method is the invocation entry point on {@code McpAuditPublisher} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpAuditPublisher.putIfPresent(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param target 参数 target；parameter target。
     * @param name 参数 name；parameter name。
     * @param value 参数 值；parameter value。
     */
    private void putIfPresent(
            Map<String, Object> target,
            String name,
            String value) {
        if (value != null) {
            target.put(name, value);
        }
    }

    /**
     * 中文说明：执行 attribute 操作；该方法是 {@code McpAuditPublisher} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the attribute operation; this method is the invocation entry point on {@code McpAuditPublisher} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpAuditPublisher.attribute(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param attributes 参数 attributes；parameter attributes。
     * @param names 参数 names；parameter names。
     * @return 返回 attribute 的处理结果；returns the result of the operation.
     */
    private String attribute(
            Map<String, Object> attributes,
            String... names) {
        for (String name : names) {
            Object value = attributes.get(name);
            if (value instanceof String text && !text.isBlank()) {
                String normalized = text.trim();
                return normalized.length() <= 256
                        ? normalized
                        : normalized.substring(0, 256);
            }
        }
        return null;
    }

    /**
     * 中文说明：执行 safeStatus 操作；该方法是 {@code McpAuditPublisher} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the safe status operation; this method is the invocation entry point on {@code McpAuditPublisher} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpAuditPublisher.safeStatus(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 safeStatus 的处理结果；returns the result of the operation.
     */
    private String safeStatus(String value) {
        if (value == null || !value.matches("[A-Z][A-Z0-9_]{0,63}")) {
            return "ERROR";
        }
        return value;
    }

    /**
     * 中文说明：{@code AuditSink} 是接口契约，位于当前 Gateway 模块的相关包中，负责审计Sink相关的职责与边界。
     * English summary: {@code AuditSink} is an interface contract in the current Gateway module; it owns the audit sink-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    @FunctionalInterface
    public interface AuditSink {

        /**
         * 中文说明：执行 publish 操作；该方法是 {@code McpAuditPublisher.AuditSink} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the publish operation; this method is the invocation entry point on {@code McpAuditPublisher.AuditSink} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpAuditPublisher.AuditSink.publish(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param json 参数 json；parameter json。
         */
        void publish(String json);
    }
}
