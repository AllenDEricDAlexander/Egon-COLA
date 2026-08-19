package top.egon.cola.component.gateway.mcp.remote.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeRemoteProvider;
import top.egon.cola.component.gateway.core.mcp.remote.RemoteAuthProvider;

import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Discovers canonical remote descriptors and persists one immutable snapshot.
 * 补充说明 / Supplementary summary: {@code McpCapabilitySynchronizer} 是类型，位于当前 Gateway 模块的相关包中，负责MCPCapabilitySynchronizer相关的职责与边界。
 * English supplement: {@code McpCapabilitySynchronizer} is a type in the current Gateway module; it owns the mcp capability synchronizer-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpCapabilitySynchronizer {

    /**
     * 中文说明：表示 METHODS 这一固定值；它属于 {@code McpCapabilitySynchronizer} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value methods; it is a state, type, or protocol value of {@code McpCapabilitySynchronizer} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code McpCapabilitySynchronizer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCapabilitySynchronizer}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final List<DiscoveryMethod> METHODS = List.of(
            new DiscoveryMethod("TOOL", "tools/list", "tools"),
            new DiscoveryMethod("RESOURCE", "resources/list", "resources"),
            new DiscoveryMethod(
                    "RESOURCE_TEMPLATE",
                    "resources/templates/list",
                    "resourceTemplates"
            ),
            new DiscoveryMethod("PROMPT", "prompts/list", "prompts")
    );

    /**
     * 中文说明：保存 clients 对应的状态、依赖或配置值；字段类型为 {@code McpRemoteClientPool}，由 {@code McpCapabilitySynchronizer} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clients; its type is {@code McpRemoteClientPool}, and {@code McpCapabilitySynchronizer} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpCapabilitySynchronizer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCapabilitySynchronizer}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpRemoteClientPool clients;

    /**
     * 中文说明：保存 translator 对应的状态、依赖或配置值；字段类型为 {@code McpDialectTranslator}，由 {@code McpCapabilitySynchronizer} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by translator; its type is {@code McpDialectTranslator}, and {@code McpCapabilitySynchronizer} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpCapabilitySynchronizer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCapabilitySynchronizer}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpDialectTranslator translator;

    /**
     * 中文说明：保存 object映射器 对应的状态、依赖或配置值；字段类型为 {@code ObjectMapper}，由 {@code McpCapabilitySynchronizer} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by object mapper; its type is {@code ObjectMapper}, and {@code McpCapabilitySynchronizer} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpCapabilitySynchronizer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCapabilitySynchronizer}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：保存 clock 对应的状态、依赖或配置值；字段类型为 {@code Clock}，由 {@code McpCapabilitySynchronizer} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clock; its type is {@code Clock}, and {@code McpCapabilitySynchronizer} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpCapabilitySynchronizer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCapabilitySynchronizer}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Clock clock;

    /**
     * 中文说明：保存 sink 对应的状态、依赖或配置值；字段类型为 {@code SnapshotSink}，由 {@code McpCapabilitySynchronizer} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by sink; its type is {@code SnapshotSink}, and {@code McpCapabilitySynchronizer} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpCapabilitySynchronizer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCapabilitySynchronizer}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final SnapshotSink sink;

    /**
     * 中文说明：创建 {@code McpCapabilitySynchronizer} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpCapabilitySynchronizer} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param clients 参数 clients；parameter clients。
     * @param translator 参数 translator；parameter translator。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @param clock 参数 clock；parameter clock。
     * @param sink 参数 sink；parameter sink。
     */
    public McpCapabilitySynchronizer(
            McpRemoteClientPool clients,
            McpDialectTranslator translator,
            ObjectMapper objectMapper,
            Clock clock,
            SnapshotSink sink) {
        this.clients = Objects.requireNonNull(clients, "clients");
        this.translator = Objects.requireNonNull(translator, "translator");
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        ).copy()
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        this.clock = Objects.requireNonNull(clock, "clock");
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    /**
     * 中文说明：执行 synchronize 操作；该方法是 {@code McpCapabilitySynchronizer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the synchronize operation; this method is the invocation entry point on {@code McpCapabilitySynchronizer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpCapabilitySynchronizer.synchronize(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param provider 参数 提供方；parameter provider。
     * @return 返回 synchronize 的处理结果；returns the result of the operation.
     */
    public Publisher<CapabilitySnapshot> synchronize(
            McpRuntimeRemoteProvider provider) {
        Objects.requireNonNull(provider, "provider");
        return initialize(provider)
                .thenMany(Flux.fromIterable(METHODS)
                        .concatMap(method -> discover(provider, method)))
                .collectList()
                .map(capabilities -> snapshot(provider, capabilities))
                .doOnNext(sink::persist);
    }

    /**
     * 中文说明：执行 requireReviewed 操作；该方法是 {@code McpCapabilitySynchronizer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the require reviewed operation; this method is the invocation entry point on {@code McpCapabilitySynchronizer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpCapabilitySynchronizer.requireReviewed(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param provider 参数 提供方；parameter provider。
     * @param snapshot 参数 snapshot；parameter snapshot。
     */
    public void requireReviewed(
            McpRuntimeRemoteProvider provider,
            CapabilitySnapshot snapshot) {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(snapshot, "snapshot");
        if (!provider.providerId().equals(snapshot.providerId())
                || !snapshot.matchesReviewedFingerprint()) {
            throw new IllegalStateException(
                    "remote MCP capabilities changed; preview and publish "
                            + "a new Release"
            );
        }
    }

    /**
     * 中文说明：执行 initialize 操作；该方法是 {@code McpCapabilitySynchronizer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the initialize operation; this method is the invocation entry point on {@code McpCapabilitySynchronizer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpCapabilitySynchronizer.initialize(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param provider 参数 提供方；parameter provider。
     * @return 返回 initialize 的处理结果；returns the result of the operation.
     */
    private Mono<Void> initialize(McpRuntimeRemoteProvider provider) {
        Map<String, Object> params = Map.of(
                "protocolVersion", provider.dialect().protocolVersion(),
                "capabilities", Map.of(),
                "clientInfo", Map.of(
                        "name", "egon-cola-gateway",
                        "version", "5.3.2"
                )
        );
        return call(provider, "initialize", params)
                .then();
    }

    /**
     * 中文说明：执行 discover 操作；该方法是 {@code McpCapabilitySynchronizer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the discover operation; this method is the invocation entry point on {@code McpCapabilitySynchronizer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpCapabilitySynchronizer.discover(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param provider 参数 提供方；parameter provider。
     * @param method 参数 方法；parameter method。
     * @return 返回 discover 的处理结果；returns the result of the operation.
     */
    private Flux<Capability> discover(
            McpRuntimeRemoteProvider provider,
            DiscoveryMethod method) {
        return call(provider, method.method(), Map.of())
                .flatMapMany(result -> Flux.fromIterable(
                        descriptors(result.get(method.resultField()))
                ))
                .map(descriptor -> new Capability(
                        method.primitiveType(),
                        name(descriptor),
                        descriptor
                ));
    }

    /**
     * 中文说明：执行 调用 操作；该方法是 {@code McpCapabilitySynchronizer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the call operation; this method is the invocation entry point on {@code McpCapabilitySynchronizer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpCapabilitySynchronizer.call(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param provider 参数 提供方；parameter provider。
     * @param method 参数 方法；parameter method。
     * @param params 参数 params；parameter params。
     * @return 返回 调用 的处理结果；returns the result of the operation.
     */
    private Mono<Map<String, Object>> call(
            McpRuntimeRemoteProvider provider,
            String method,
            Map<String, Object> params) {
        McpDialectTranslator.OutboundCall call = translator.outbound(
                provider.dialect(),
                provider.dialect(),
                method,
                params,
                Map.of("purpose", "capability-sync"),
                Map.of()
        );
        return Mono.from(clients.exchange(
                        provider,
                        call,
                        RemoteAuthProvider.AuthContext.system()
                ))
                .map(translator::result);
    }

    /**
     * 中文说明：执行 snapshot 操作；该方法是 {@code McpCapabilitySynchronizer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the snapshot operation; this method is the invocation entry point on {@code McpCapabilitySynchronizer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpCapabilitySynchronizer.snapshot(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param provider 参数 提供方；parameter provider。
     * @param values 参数 values；parameter values。
     * @return 返回 snapshot 的处理结果；returns the result of the operation.
     */
    private CapabilitySnapshot snapshot(
            McpRuntimeRemoteProvider provider,
            List<Capability> values) {
        List<Capability> sorted = values.stream()
                .sorted(Comparator.comparing(Capability::primitiveType)
                        .thenComparing(Capability::remoteName))
                .toList();
        String fingerprint = fingerprint(sorted);
        return new CapabilitySnapshot(
                provider.providerId(),
                fingerprint,
                fingerprint.equals(provider.capabilityFingerprint()),
                sorted,
                clock.instant()
        );
    }

    /**
     * 中文说明：执行 fingerprint 操作；该方法是 {@code McpCapabilitySynchronizer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the fingerprint operation; this method is the invocation entry point on {@code McpCapabilitySynchronizer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpCapabilitySynchronizer.fingerprint(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param capabilities 参数 capabilities；parameter capabilities。
     * @return 返回 fingerprint 的处理结果；returns the result of the operation.
     */
    private String fingerprint(List<Capability> capabilities) {
        try {
            ArrayList<Map<String, Object>> canonical = new ArrayList<>();
            capabilities.forEach(capability -> {
                LinkedHashMap<String, Object> item = new LinkedHashMap<>();
                item.put("primitiveType", capability.primitiveType());
                item.put("remoteName", capability.remoteName());
                item.put("descriptor", capability.descriptor());
                canonical.add(item);
            });
            return HexFormat.of().formatHex(MessageDigest.getInstance(
                    "SHA-256"
            ).digest(objectMapper.writeValueAsBytes(canonical)));
        } catch (Exception failure) {
            throw new IllegalStateException(
                    "remote MCP capability fingerprint failed",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 descriptors 操作；该方法是 {@code McpCapabilitySynchronizer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the descriptors operation; this method is the invocation entry point on {@code McpCapabilitySynchronizer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpCapabilitySynchronizer.descriptors(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 descriptors 的处理结果；returns the result of the operation.
     */
    private List<Map<String, Object>> descriptors(Object value) {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> source)) {
            throw new IllegalStateException(
                    "remote MCP capability list is invalid"
            );
        }
        ArrayList<Map<String, Object>> result = new ArrayList<>();
        source.forEach(item -> {
            if (!(item instanceof Map<?, ?> map)) {
                throw new IllegalStateException(
                        "remote MCP capability descriptor is invalid"
                );
            }
            LinkedHashMap<String, Object> descriptor = new LinkedHashMap<>();
            map.forEach((key, content) -> {
                if (!(key instanceof String name)) {
                    throw new IllegalStateException(
                            "remote MCP capability field is invalid"
                    );
                }
                descriptor.put(name, content);
            });
            result.add(Map.copyOf(descriptor));
        });
        return List.copyOf(result);
    }

    /**
     * 中文说明：执行 name 操作；该方法是 {@code McpCapabilitySynchronizer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the name operation; this method is the invocation entry point on {@code McpCapabilitySynchronizer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpCapabilitySynchronizer.name(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param descriptor 参数 descriptor；parameter descriptor。
     * @return 返回 name 的处理结果；returns the result of the operation.
     */
    private String name(Map<String, Object> descriptor) {
        Object value = descriptor.get("name");
        if (!(value instanceof String text) || text.isBlank()) {
            value = descriptor.get("uri");
        }
        if (!(value instanceof String text) || text.isBlank()) {
            value = descriptor.get("uriTemplate");
        }
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalStateException(
                    "remote MCP capability name is missing"
            );
        }
        return text.trim();
    }

    /**
     * 中文说明：{@code SnapshotSink} 是接口契约，位于当前 Gateway 模块的相关包中，负责SnapshotSink相关的职责与边界。
     * English summary: {@code SnapshotSink} is an interface contract in the current Gateway module; it owns the snapshot sink-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    @FunctionalInterface
    public interface SnapshotSink {

        /**
         * 中文说明：执行 persist 操作；该方法是 {@code McpCapabilitySynchronizer.SnapshotSink} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the persist operation; this method is the invocation entry point on {@code McpCapabilitySynchronizer.SnapshotSink} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpCapabilitySynchronizer.SnapshotSink.persist(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param snapshot 参数 snapshot；parameter snapshot。
         */
        void persist(CapabilitySnapshot snapshot);
    }

    /**
     * 中文说明：{@code Capability} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Capability相关的职责与边界。
     * English summary: {@code Capability} is an immutable data carrier in the current Gateway module; it owns the capability-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param primitiveType 参数 primitiveType；parameter primitive type。
     * @param remoteName 参数 远程Name；parameter remote name。
     * @param descriptor 参数 descriptor；parameter descriptor。
     */
    public record Capability(
            /**
             * 中文说明：保存 primitiveType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpCapabilitySynchronizer.Capability} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by primitive type; its type is {@code String}, and {@code McpCapabilitySynchronizer.Capability} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpCapabilitySynchronizer.Capability} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCapabilitySynchronizer.Capability}; do not couple callers to its representation when the owning type exposes an API.
             */
            String primitiveType,
            /**
             * 中文说明：保存 远程Name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpCapabilitySynchronizer.Capability} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by remote name; its type is {@code String}, and {@code McpCapabilitySynchronizer.Capability} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpCapabilitySynchronizer.Capability} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCapabilitySynchronizer.Capability}; do not couple callers to its representation when the owning type exposes an API.
             */
            String remoteName,
            /**
             * 中文说明：保存 descriptor 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code McpCapabilitySynchronizer.Capability} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by descriptor; its type is {@code Map<String, Object>}, and {@code McpCapabilitySynchronizer.Capability} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpCapabilitySynchronizer.Capability} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCapabilitySynchronizer.Capability}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> descriptor
    ) {

        /**
         * 中文说明：创建 {@code McpCapabilitySynchronizer.Capability} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code McpCapabilitySynchronizer.Capability} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param primitiveType 参数 primitiveType；parameter primitive type。
         * @param remoteName 参数 远程Name；parameter remote name。
         * @param descriptor 参数 descriptor；parameter descriptor。
         */
        public Capability {
            primitiveType = required(primitiveType, "primitiveType");
            remoteName = required(remoteName, "remoteName");
            descriptor = Map.copyOf(Objects.requireNonNull(
                    descriptor,
                    "descriptor"
            ));
        }
    }

    /**
     * 中文说明：{@code CapabilitySnapshot} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责CapabilitySnapshot相关的职责与边界。
     * English summary: {@code CapabilitySnapshot} is an immutable data carrier in the current Gateway module; it owns the capability snapshot-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param providerId 参数 提供方Id；parameter provider id。
     * @param fingerprint 参数 fingerprint；parameter fingerprint。
     * @param matchesReviewedFingerprint 参数 matchesReviewedFingerprint；parameter matches reviewed fingerprint。
     * @param capabilities 参数 capabilities；parameter capabilities。
     * @param synchronizedAt 参数 synchronizedAt；parameter synchronized at。
     */
    public record CapabilitySnapshot(
            /**
             * 中文说明：保存 提供方Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpCapabilitySynchronizer.CapabilitySnapshot} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by provider id; its type is {@code String}, and {@code McpCapabilitySynchronizer.CapabilitySnapshot} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpCapabilitySynchronizer.CapabilitySnapshot} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCapabilitySynchronizer.CapabilitySnapshot}; do not couple callers to its representation when the owning type exposes an API.
             */
            String providerId,
            /**
             * 中文说明：保存 fingerprint 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpCapabilitySynchronizer.CapabilitySnapshot} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by fingerprint; its type is {@code String}, and {@code McpCapabilitySynchronizer.CapabilitySnapshot} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpCapabilitySynchronizer.CapabilitySnapshot} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCapabilitySynchronizer.CapabilitySnapshot}; do not couple callers to its representation when the owning type exposes an API.
             */
            String fingerprint,
            /**
             * 中文说明：保存 matchesReviewedFingerprint 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code McpCapabilitySynchronizer.CapabilitySnapshot} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by matches reviewed fingerprint; its type is {@code boolean}, and {@code McpCapabilitySynchronizer.CapabilitySnapshot} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpCapabilitySynchronizer.CapabilitySnapshot} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCapabilitySynchronizer.CapabilitySnapshot}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean matchesReviewedFingerprint,
            /**
             * 中文说明：保存 capabilities 对应的状态、依赖或配置值；字段类型为 {@code List<Capability>}，由 {@code McpCapabilitySynchronizer.CapabilitySnapshot} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by capabilities; its type is {@code List<Capability>}, and {@code McpCapabilitySynchronizer.CapabilitySnapshot} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpCapabilitySynchronizer.CapabilitySnapshot} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCapabilitySynchronizer.CapabilitySnapshot}; do not couple callers to its representation when the owning type exposes an API.
             */
            List<Capability> capabilities,
            /**
             * 中文说明：保存 synchronizedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code McpCapabilitySynchronizer.CapabilitySnapshot} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by synchronized at; its type is {@code Instant}, and {@code McpCapabilitySynchronizer.CapabilitySnapshot} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpCapabilitySynchronizer.CapabilitySnapshot} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCapabilitySynchronizer.CapabilitySnapshot}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant synchronizedAt
    ) {

        /**
         * 中文说明：创建 {@code McpCapabilitySynchronizer.CapabilitySnapshot} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code McpCapabilitySynchronizer.CapabilitySnapshot} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param providerId 参数 提供方Id；parameter provider id。
         * @param fingerprint 参数 fingerprint；parameter fingerprint。
         * @param matchesReviewedFingerprint 参数 matchesReviewedFingerprint；parameter matches reviewed fingerprint。
         * @param capabilities 参数 capabilities；parameter capabilities。
         * @param synchronizedAt 参数 synchronizedAt；parameter synchronized at。
         */
        public CapabilitySnapshot {
            providerId = required(providerId, "providerId");
            fingerprint = required(fingerprint, "fingerprint");
            capabilities = List.copyOf(Objects.requireNonNull(
                    capabilities,
                    "capabilities"
            ));
            synchronizedAt = Objects.requireNonNull(
                    synchronizedAt,
                    "synchronizedAt"
            );
        }
    }

    /**
     * 中文说明：{@code DiscoveryMethod} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责发现方法相关的职责与边界。
     * English summary: {@code DiscoveryMethod} is an immutable data carrier in the current Gateway module; it owns the discovery method-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param primitiveType 参数 primitiveType；parameter primitive type。
     * @param method 参数 方法；parameter method。
     * @param resultField 参数 resultField；parameter result field。
     */
    private record DiscoveryMethod(
            /**
             * 中文说明：保存 primitiveType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpCapabilitySynchronizer.DiscoveryMethod} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by primitive type; its type is {@code String}, and {@code McpCapabilitySynchronizer.DiscoveryMethod} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpCapabilitySynchronizer.DiscoveryMethod} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCapabilitySynchronizer.DiscoveryMethod}; do not couple callers to its representation when the owning type exposes an API.
             */
            String primitiveType,
            /**
             * 中文说明：保存 方法 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpCapabilitySynchronizer.DiscoveryMethod} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by method; its type is {@code String}, and {@code McpCapabilitySynchronizer.DiscoveryMethod} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpCapabilitySynchronizer.DiscoveryMethod} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCapabilitySynchronizer.DiscoveryMethod}; do not couple callers to its representation when the owning type exposes an API.
             */
            String method,
            /**
             * 中文说明：保存 resultField 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpCapabilitySynchronizer.DiscoveryMethod} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by result field; its type is {@code String}, and {@code McpCapabilitySynchronizer.DiscoveryMethod} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpCapabilitySynchronizer.DiscoveryMethod} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCapabilitySynchronizer.DiscoveryMethod}; do not couple callers to its representation when the owning type exposes an API.
             */
            String resultField
    ) {
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code McpCapabilitySynchronizer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code McpCapabilitySynchronizer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpCapabilitySynchronizer.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "remote MCP " + field + " is required"
            );
        }
        return value.trim();
    }
}
