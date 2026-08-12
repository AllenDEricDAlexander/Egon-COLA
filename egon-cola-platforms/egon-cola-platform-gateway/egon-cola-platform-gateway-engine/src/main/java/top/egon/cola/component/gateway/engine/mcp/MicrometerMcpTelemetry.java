package top.egon.cola.component.gateway.engine.mcp;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import top.egon.cola.component.gateway.mcp.telemetry.McpTelemetry;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Micrometer Adapter with a fixed low-cardinality MCP tag vocabulary.
 * 补充说明 / Supplementary summary: {@code MicrometerMcpTelemetry} 是类型，位于当前 Gateway 模块的相关包中，负责MicrometerMCP遥测相关的职责与边界。
 * English supplement: {@code MicrometerMcpTelemetry} is a type in the current Gateway module; it owns the micrometer mcp telemetry-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class MicrometerMcpTelemetry implements McpTelemetry {

    /**
     * 中文说明：表示 METHODS 这一固定值；它属于 {@code MicrometerMcpTelemetry} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value methods; it is a state, type, or protocol value of {@code MicrometerMcpTelemetry} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code MicrometerMcpTelemetry} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code MicrometerMcpTelemetry}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final Set<String> METHODS = Set.of(
            "initialize",
            "notifications/initialized",
            "ping",
            "server/discover",
            "tools/list",
            "tools/call",
            "resources/list",
            "resources/templates/list",
            "resources/read",
            "resources/subscribe",
            "resources/unsubscribe",
            "subscriptions/listen",
            "prompts/list",
            "prompts/get",
            "completion/complete",
            "tasks/get",
            "tasks/update",
            "tasks/cancel"
    );

    /**
     * 中文说明：表示 PRIMITIVES 这一固定值；它属于 {@code MicrometerMcpTelemetry} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value primitives; it is a state, type, or protocol value of {@code MicrometerMcpTelemetry} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code MicrometerMcpTelemetry} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code MicrometerMcpTelemetry}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final Set<String> PRIMITIVES = Set.of(
            "LIFECYCLE",
            "TOOL",
            "RESOURCE",
            "SUBSCRIPTION",
            "PROMPT",
            "COMPLETION",
            "TASK",
            "APP",
            "UNKNOWN"
    );

    /**
     * 中文说明：保存 meters 对应的状态、依赖或配置值；字段类型为 {@code MeterRegistry}，由 {@code MicrometerMcpTelemetry} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by meters; its type is {@code MeterRegistry}, and {@code MicrometerMcpTelemetry} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code MicrometerMcpTelemetry} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code MicrometerMcpTelemetry}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final MeterRegistry meters;

    /**
     * 中文说明：保存 observations 对应的状态、依赖或配置值；字段类型为 {@code ObservationRegistry}，由 {@code MicrometerMcpTelemetry} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by observations; its type is {@code ObservationRegistry}, and {@code MicrometerMcpTelemetry} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code MicrometerMcpTelemetry} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code MicrometerMcpTelemetry}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ObservationRegistry observations;

    /**
     * 中文说明：创建 {@code MicrometerMcpTelemetry} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code MicrometerMcpTelemetry} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param meters 参数 meters；parameter meters。
     * @param observations 参数 observations；parameter observations。
     */
    public MicrometerMcpTelemetry(
            MeterRegistry meters,
            ObservationRegistry observations) {
        this.meters = Objects.requireNonNull(meters, "meters");
        this.observations = Objects.requireNonNull(
                observations,
                "observations"
        );
    }

    /**
     * 中文说明：执行 start 操作；该方法是 {@code MicrometerMcpTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the start operation; this method is the invocation entry point on {@code MicrometerMcpTelemetry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MicrometerMcpTelemetry.start(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 start 的处理结果；returns the result of the operation.
     */
    @Override
    public Scope start(Request request) {
        Objects.requireNonNull(request, "request");
        Tags tags = tags(request);
        Observation observation = Observation.createNotStarted(
                        "mcp.server.request",
                        observations
                )
                .contextualName("mcp " + tags.method())
                .lowCardinalityKeyValue("mcp.method", tags.method())
                .lowCardinalityKeyValue("mcp.primitive", tags.primitive())
                .lowCardinalityKeyValue("mcp.server", tags.server())
                .lowCardinalityKeyValue(
                        "mcp.remote.provider",
                        tags.remoteProvider()
                )
                .start();
        Timer.Sample sample = Timer.start(meters);
        return new MeteredScope(tags, observation, sample);
    }

    /**
     * 中文说明：执行 tags 操作；该方法是 {@code MicrometerMcpTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the tags operation; this method is the invocation entry point on {@code MicrometerMcpTelemetry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MicrometerMcpTelemetry.tags(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 tags 的处理结果；returns the result of the operation.
     */
    private Tags tags(Request request) {
        String method = METHODS.contains(request.method())
                ? request.method()
                : "unknown";
        String primitive = request.primitive().toUpperCase(Locale.ROOT);
        if (!PRIMITIVES.contains(primitive)) {
            primitive = "UNKNOWN";
        }
        return new Tags(
                method,
                primitive,
                code(request.serverCode()),
                request.remoteProviderCode() == null
                        ? "none"
                        : code(request.remoteProviderCode())
        );
    }

    /**
     * 中文说明：执行 code 操作；该方法是 {@code MicrometerMcpTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the code operation; this method is the invocation entry point on {@code MicrometerMcpTelemetry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MicrometerMcpTelemetry.code(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 code 的处理结果；returns the result of the operation.
     */
    private String code(String value) {
        return value.matches("[A-Za-z0-9][A-Za-z0-9_.-]{0,63}")
                ? value
                : "other";
    }

    /**
     * 中文说明：执行 status 操作；该方法是 {@code MicrometerMcpTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the status operation; this method is the invocation entry point on {@code MicrometerMcpTelemetry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MicrometerMcpTelemetry.status(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 status 的处理结果；returns the result of the operation.
     */
    private String status(String value) {
        if (value == null || value.isBlank()) {
            return "ERROR";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.matches("[A-Z][A-Z0-9_]{0,63}")
                ? normalized
                : "ERROR";
    }

    /**
     * 中文说明：{@code MeteredScope} 是类型，位于当前 Gateway 模块的相关包中，负责MeteredScope相关的职责与边界。
     * English summary: {@code MeteredScope} is a type in the current Gateway module; it owns the metered scope-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    private final class MeteredScope implements Scope {

        /**
         * 中文说明：保存 tags 对应的状态、依赖或配置值；字段类型为 {@code Tags}，由 {@code MicrometerMcpTelemetry.MeteredScope} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by tags; its type is {@code Tags}, and {@code MicrometerMcpTelemetry.MeteredScope} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code MicrometerMcpTelemetry.MeteredScope} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code MicrometerMcpTelemetry.MeteredScope}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final Tags tags;

        /**
         * 中文说明：保存 观测 对应的状态、依赖或配置值；字段类型为 {@code Observation}，由 {@code MicrometerMcpTelemetry.MeteredScope} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by observation; its type is {@code Observation}, and {@code MicrometerMcpTelemetry.MeteredScope} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code MicrometerMcpTelemetry.MeteredScope} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code MicrometerMcpTelemetry.MeteredScope}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final Observation observation;

        /**
         * 中文说明：保存 sample 对应的状态、依赖或配置值；字段类型为 {@code Timer.Sample}，由 {@code MicrometerMcpTelemetry.MeteredScope} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by sample; its type is {@code Timer.Sample}, and {@code MicrometerMcpTelemetry.MeteredScope} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code MicrometerMcpTelemetry.MeteredScope} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code MicrometerMcpTelemetry.MeteredScope}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final Timer.Sample sample;

        /**
         * 中文说明：保存 completed 对应的状态、依赖或配置值；字段类型为 {@code AtomicBoolean}，由 {@code MicrometerMcpTelemetry.MeteredScope} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by completed; its type is {@code AtomicBoolean}, and {@code MicrometerMcpTelemetry.MeteredScope} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code MicrometerMcpTelemetry.MeteredScope} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code MicrometerMcpTelemetry.MeteredScope}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final AtomicBoolean completed = new AtomicBoolean();

        /**
         * 中文说明：保存 远程提供方 对应的状态、依赖或配置值；字段类型为 {@code AtomicReference<String>}，由 {@code MicrometerMcpTelemetry.MeteredScope} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by remote provider; its type is {@code AtomicReference<String>}, and {@code MicrometerMcpTelemetry.MeteredScope} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code MicrometerMcpTelemetry.MeteredScope} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code MicrometerMcpTelemetry.MeteredScope}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final AtomicReference<String> remoteProvider;

        /**
         * 中文说明：创建 {@code MicrometerMcpTelemetry.MeteredScope} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code MicrometerMcpTelemetry.MeteredScope} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param tags 参数 tags；parameter tags。
         * @param observation 参数 观测；parameter observation。
         * @param sample 参数 sample；parameter sample。
         */
        private MeteredScope(
                Tags tags,
                Observation observation,
                Timer.Sample sample) {
            this.tags = tags;
            this.observation = observation;
            this.sample = sample;
            this.remoteProvider = new AtomicReference<>(
                    tags.remoteProvider()
            );
        }

        /**
         * 中文说明：执行 远程提供方 操作；该方法是 {@code MicrometerMcpTelemetry.MeteredScope} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the remote provider operation; this method is the invocation entry point on {@code MicrometerMcpTelemetry.MeteredScope} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code MicrometerMcpTelemetry.MeteredScope.remoteProvider(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param providerCode 参数 提供方Code；parameter provider code。
         */
        @Override
        public void remoteProvider(String providerCode) {
            String value = code(Objects.requireNonNull(
                    providerCode,
                    "providerCode"
            ));
            remoteProvider.set(value);
            observation.lowCardinalityKeyValue(
                    "mcp.remote.provider",
                    value
            );
        }

        /**
         * 中文说明：执行 startChild 操作；该方法是 {@code MicrometerMcpTelemetry.MeteredScope} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the start child operation; this method is the invocation entry point on {@code MicrometerMcpTelemetry.MeteredScope} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code MicrometerMcpTelemetry.MeteredScope.startChild(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param kind 参数 kind；parameter kind。
         * @return 返回 startChild 的处理结果；returns the result of the operation.
         */
        @Override
        public Child startChild(ChildKind kind) {
            Objects.requireNonNull(kind, "kind");
            if (completed.get()) {
                return Child.noop();
            }
            Observation child = Observation.createNotStarted(
                            "mcp.server." + kind.name()
                                    .toLowerCase(Locale.ROOT),
                            observations
                    )
                    .parentObservation(observation)
                    .lowCardinalityKeyValue("mcp.method", tags.method())
                    .lowCardinalityKeyValue(
                            "mcp.child.kind",
                            kind.name()
                    )
                    .start();
            Timer.Sample childSample = Timer.start(meters);
            return new MeteredChild(
                    kind,
                    tags,
                    remoteProvider.get(),
                    child,
                    childSample
            );
        }

        /**
         * 中文说明：执行 success 操作；该方法是 {@code MicrometerMcpTelemetry.MeteredScope} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the success operation; this method is the invocation entry point on {@code MicrometerMcpTelemetry.MeteredScope} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code MicrometerMcpTelemetry.MeteredScope.success(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         */
        @Override
        public void success() {
            complete("SUCCESS", null);
        }

        /**
         * 中文说明：执行 failure 操作；该方法是 {@code MicrometerMcpTelemetry.MeteredScope} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the failure operation; this method is the invocation entry point on {@code MicrometerMcpTelemetry.MeteredScope} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code MicrometerMcpTelemetry.MeteredScope.failure(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param errorCode 参数 errorCode；parameter error code。
         */
        @Override
        public void failure(String errorCode) {
            String failureStatus = status(errorCode);
            complete(
                    failureStatus,
                    new TelemetryFailure(failureStatus)
            );
        }

        /**
         * 中文说明：执行 complete 操作；该方法是 {@code MicrometerMcpTelemetry.MeteredScope} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the complete operation; this method is the invocation entry point on {@code MicrometerMcpTelemetry.MeteredScope} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code MicrometerMcpTelemetry.MeteredScope.complete(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param status 参数 status；parameter status。
         * @param failure 参数 failure；parameter failure。
         */
        private void complete(String status, RuntimeException failure) {
            if (!completed.compareAndSet(false, true)) {
                return;
            }
            observation.lowCardinalityKeyValue("mcp.status", status);
            if (failure != null) {
                observation.error(failure);
            }
            observation.stop();
            sample.stop(Timer.builder("gateway.mcp.requests")
                    .tags(tags.values(status, remoteProvider.get()))
                    .register(meters));
        }
    }

    /**
     * 中文说明：{@code MeteredChild} 是类型，位于当前 Gateway 模块的相关包中，负责MeteredChild相关的职责与边界。
     * English summary: {@code MeteredChild} is a type in the current Gateway module; it owns the metered child-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    private final class MeteredChild implements Child {

        /**
         * 中文说明：保存 kind 对应的状态、依赖或配置值；字段类型为 {@code ChildKind}，由 {@code MicrometerMcpTelemetry.MeteredChild} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by kind; its type is {@code ChildKind}, and {@code MicrometerMcpTelemetry.MeteredChild} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code MicrometerMcpTelemetry.MeteredChild} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code MicrometerMcpTelemetry.MeteredChild}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final ChildKind kind;

        /**
         * 中文说明：保存 tags 对应的状态、依赖或配置值；字段类型为 {@code Tags}，由 {@code MicrometerMcpTelemetry.MeteredChild} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by tags; its type is {@code Tags}, and {@code MicrometerMcpTelemetry.MeteredChild} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code MicrometerMcpTelemetry.MeteredChild} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code MicrometerMcpTelemetry.MeteredChild}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final Tags tags;

        /**
         * 中文说明：保存 远程提供方 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code MicrometerMcpTelemetry.MeteredChild} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by remote provider; its type is {@code String}, and {@code MicrometerMcpTelemetry.MeteredChild} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code MicrometerMcpTelemetry.MeteredChild} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code MicrometerMcpTelemetry.MeteredChild}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final String remoteProvider;

        /**
         * 中文说明：保存 观测 对应的状态、依赖或配置值；字段类型为 {@code Observation}，由 {@code MicrometerMcpTelemetry.MeteredChild} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by observation; its type is {@code Observation}, and {@code MicrometerMcpTelemetry.MeteredChild} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code MicrometerMcpTelemetry.MeteredChild} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code MicrometerMcpTelemetry.MeteredChild}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final Observation observation;

        /**
         * 中文说明：保存 sample 对应的状态、依赖或配置值；字段类型为 {@code Timer.Sample}，由 {@code MicrometerMcpTelemetry.MeteredChild} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by sample; its type is {@code Timer.Sample}, and {@code MicrometerMcpTelemetry.MeteredChild} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code MicrometerMcpTelemetry.MeteredChild} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code MicrometerMcpTelemetry.MeteredChild}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final Timer.Sample sample;

        /**
         * 中文说明：保存 completed 对应的状态、依赖或配置值；字段类型为 {@code AtomicBoolean}，由 {@code MicrometerMcpTelemetry.MeteredChild} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by completed; its type is {@code AtomicBoolean}, and {@code MicrometerMcpTelemetry.MeteredChild} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code MicrometerMcpTelemetry.MeteredChild} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code MicrometerMcpTelemetry.MeteredChild}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final AtomicBoolean completed = new AtomicBoolean();

        /**
         * 中文说明：创建 {@code MicrometerMcpTelemetry.MeteredChild} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code MicrometerMcpTelemetry.MeteredChild} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param kind 参数 kind；parameter kind。
         * @param tags 参数 tags；parameter tags。
         * @param remoteProvider 参数 远程提供方；parameter remote provider。
         * @param observation 参数 观测；parameter observation。
         * @param sample 参数 sample；parameter sample。
         */
        private MeteredChild(
                ChildKind kind,
                Tags tags,
                String remoteProvider,
                Observation observation,
                Timer.Sample sample) {
            this.kind = kind;
            this.tags = tags;
            this.remoteProvider = remoteProvider;
            this.observation = observation;
            this.sample = sample;
        }

        /**
         * 中文说明：执行 success 操作；该方法是 {@code MicrometerMcpTelemetry.MeteredChild} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the success operation; this method is the invocation entry point on {@code MicrometerMcpTelemetry.MeteredChild} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code MicrometerMcpTelemetry.MeteredChild.success(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         */
        @Override
        public void success() {
            complete("SUCCESS", null);
        }

        /**
         * 中文说明：执行 failure 操作；该方法是 {@code MicrometerMcpTelemetry.MeteredChild} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the failure operation; this method is the invocation entry point on {@code MicrometerMcpTelemetry.MeteredChild} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code MicrometerMcpTelemetry.MeteredChild.failure(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param errorCode 参数 errorCode；parameter error code。
         */
        @Override
        public void failure(String errorCode) {
            String failureStatus = status(errorCode);
            complete(
                    failureStatus,
                    new TelemetryFailure(failureStatus)
            );
        }

        /**
         * 中文说明：执行 complete 操作；该方法是 {@code MicrometerMcpTelemetry.MeteredChild} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the complete operation; this method is the invocation entry point on {@code MicrometerMcpTelemetry.MeteredChild} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code MicrometerMcpTelemetry.MeteredChild.complete(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param status 参数 status；parameter status。
         * @param failure 参数 failure；parameter failure。
         */
        private void complete(String status, RuntimeException failure) {
            if (!completed.compareAndSet(false, true)) {
                return;
            }
            observation.lowCardinalityKeyValue("mcp.status", status);
            if (failure != null) {
                observation.error(failure);
            }
            observation.stop();
            sample.stop(Timer.builder("gateway.mcp.children")
                    .tags(tags.values(status, remoteProvider))
                    .tag("mcp.child.kind", kind.name())
                    .register(meters));
        }
    }

    /**
     * 中文说明：{@code Tags} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Tags相关的职责与边界。
     * English summary: {@code Tags} is an immutable data carrier in the current Gateway module; it owns the tags-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param method 参数 方法；parameter method。
     * @param primitive 参数 primitive；parameter primitive。
     * @param server 参数 服务器；parameter server。
     * @param remoteProvider 参数 远程提供方；parameter remote provider。
     */
    private record Tags(
            /**
             * 中文说明：保存 方法 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code MicrometerMcpTelemetry.Tags} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by method; its type is {@code String}, and {@code MicrometerMcpTelemetry.Tags} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code MicrometerMcpTelemetry.Tags} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code MicrometerMcpTelemetry.Tags}; do not couple callers to its representation when the owning type exposes an API.
             */
            String method,
            /**
             * 中文说明：保存 primitive 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code MicrometerMcpTelemetry.Tags} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by primitive; its type is {@code String}, and {@code MicrometerMcpTelemetry.Tags} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code MicrometerMcpTelemetry.Tags} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code MicrometerMcpTelemetry.Tags}; do not couple callers to its representation when the owning type exposes an API.
             */
            String primitive,
            /**
             * 中文说明：保存 服务器 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code MicrometerMcpTelemetry.Tags} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by server; its type is {@code String}, and {@code MicrometerMcpTelemetry.Tags} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code MicrometerMcpTelemetry.Tags} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code MicrometerMcpTelemetry.Tags}; do not couple callers to its representation when the owning type exposes an API.
             */
            String server,
            /**
             * 中文说明：保存 远程提供方 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code MicrometerMcpTelemetry.Tags} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by remote provider; its type is {@code String}, and {@code MicrometerMcpTelemetry.Tags} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code MicrometerMcpTelemetry.Tags} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code MicrometerMcpTelemetry.Tags}; do not couple callers to its representation when the owning type exposes an API.
             */
            String remoteProvider
    ) {

        /**
         * 中文说明：执行 values 操作；该方法是 {@code MicrometerMcpTelemetry.Tags} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the values operation; this method is the invocation entry point on {@code MicrometerMcpTelemetry.Tags} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code MicrometerMcpTelemetry.Tags.values(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param status 参数 status；parameter status。
         * @param remoteProvider 参数 远程提供方；parameter remote provider。
         * @return 返回 values 的处理结果；returns the result of the operation.
         */
        private List<Tag> values(String status, String remoteProvider) {
            return List.of(
                    Tag.of("mcp.method", method),
                    Tag.of("mcp.primitive", primitive),
                    Tag.of("mcp.server", server),
                    Tag.of("mcp.remote.provider", remoteProvider),
                    Tag.of("mcp.status", status)
            );
        }
    }

    /**
     * 中文说明：{@code TelemetryFailure} 是类型，位于当前 Gateway 模块的相关包中，负责遥测Failure相关的职责与边界。
     * English summary: {@code TelemetryFailure} is a type in the current Gateway module; it owns the telemetry failure-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    private static final class TelemetryFailure
            extends RuntimeException {

        /**
         * 中文说明：创建 {@code MicrometerMcpTelemetry.TelemetryFailure} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code MicrometerMcpTelemetry.TelemetryFailure} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param status 参数 status；parameter status。
         */
        private TelemetryFailure(String status) {
            super(status, null, false, false);
        }
    }
}
