package top.egon.cola.component.gateway.admin.application.observability;

import top.egon.cola.component.gateway.contract.observability.GatewayCallEventV1;

import java.time.Instant;
import java.util.List;

/**
 * 中文说明：{@code GatewayObservabilityStore} 是接口契约，位于当前 Gateway 模块的相关包中，负责网关可观测性存储相关的职责与边界。
 * English summary: {@code GatewayObservabilityStore} is an interface contract in the current Gateway module; it owns the gateway observability store-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public interface GatewayObservabilityStore {

    /**
     * 中文说明：执行 project 操作；该方法是 {@code GatewayObservabilityStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the project operation; this method is the invocation entry point on {@code GatewayObservabilityStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayObservabilityStore.project(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param event 参数 事件；parameter event。
     * @param expiresAt 参数 expiresAt；parameter expires at。
     * @return 返回 project 的处理结果；returns the result of the operation.
     */
    boolean project(GatewayCallEventV1 event, Instant expiresAt);

    /**
     * 中文说明：执行 recordFailure 操作；该方法是 {@code GatewayObservabilityStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the record failure operation; this method is the invocation entry point on {@code GatewayObservabilityStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayObservabilityStore.recordFailure(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param failure 参数 failure；parameter failure。
     */
    void recordFailure(ConsumeFailure failure);

    /**
     * 中文说明：执行 traces 操作；该方法是 {@code GatewayObservabilityStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the traces operation; this method is the invocation entry point on {@code GatewayObservabilityStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayObservabilityStore.traces(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param query 参数 query；parameter query。
     * @return 返回 traces 的处理结果；returns the result of the operation.
     */
    Page<TraceSummary> traces(TraceQuery query);

    /**
     * 中文说明：执行 dashboard 操作；该方法是 {@code GatewayObservabilityStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the dashboard operation; this method is the invocation entry point on {@code GatewayObservabilityStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayObservabilityStore.dashboard(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param env 参数 env；parameter env。
     * @param namespace 参数 命名空间；parameter namespace。
     * @param since 参数 since；parameter since。
     * @return 返回 dashboard 的处理结果；returns the result of the operation.
     */
    DashboardSummary dashboard(String env, String namespace, Instant since);

    /**
     * 中文说明：执行 audits 操作；该方法是 {@code GatewayObservabilityStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the audits operation; this method is the invocation entry point on {@code GatewayObservabilityStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayObservabilityStore.audits(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param query 参数 query；parameter query。
     * @return 返回 audits 的处理结果；returns the result of the operation.
     */
    Page<AuditSummary> audits(AuditQuery query);

    /**
     * 中文说明：执行 deleteExpired 操作；该方法是 {@code GatewayObservabilityStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete expired operation; this method is the invocation entry point on {@code GatewayObservabilityStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayObservabilityStore.deleteExpired(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param now 参数 now；parameter now。
     * @return 返回 deleteExpired 的处理结果；returns the result of the operation.
     */
    int deleteExpired(Instant now);

    /**
     * 中文说明：{@code ConsumeFailure} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责ConsumeFailure相关的职责与边界。
     * English summary: {@code ConsumeFailure} is an immutable data carrier in the current Gateway module; it owns the consume failure-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param id 参数 id；parameter id。
     * @param topic 参数 topic；parameter topic。
     * @param partition 参数 partition；parameter partition。
     * @param offset 参数 offset；parameter offset。
     * @param eventId 参数 事件Id；parameter event id。
     * @param failureCode 参数 failureCode；parameter failure code。
     * @param failureMessage 参数 failure消息；parameter failure message。
     * @param payloadSha256 参数 payloadSha256；parameter payload sha256。
     * @param payloadSize 参数 payloadSize；parameter payload size。
     * @param occurredAt 参数 occurredAt；parameter occurred at。
     */
    record ConsumeFailure(
            /**
             * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayObservabilityStore.ConsumeFailure} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code GatewayObservabilityStore.ConsumeFailure} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.ConsumeFailure} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.ConsumeFailure}; do not couple callers to its representation when the owning type exposes an API.
             */
            String id,
            /**
             * 中文说明：保存 topic 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayObservabilityStore.ConsumeFailure} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by topic; its type is {@code String}, and {@code GatewayObservabilityStore.ConsumeFailure} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.ConsumeFailure} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.ConsumeFailure}; do not couple callers to its representation when the owning type exposes an API.
             */
            String topic,
            /**
             * 中文说明：保存 partition 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayObservabilityStore.ConsumeFailure} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by partition; its type is {@code int}, and {@code GatewayObservabilityStore.ConsumeFailure} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.ConsumeFailure} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.ConsumeFailure}; do not couple callers to its representation when the owning type exposes an API.
             */
            int partition,
            /**
             * 中文说明：保存 offset 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayObservabilityStore.ConsumeFailure} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by offset; its type is {@code long}, and {@code GatewayObservabilityStore.ConsumeFailure} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.ConsumeFailure} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.ConsumeFailure}; do not couple callers to its representation when the owning type exposes an API.
             */
            long offset,
            /**
             * 中文说明：保存 事件Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayObservabilityStore.ConsumeFailure} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by event id; its type is {@code String}, and {@code GatewayObservabilityStore.ConsumeFailure} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.ConsumeFailure} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.ConsumeFailure}; do not couple callers to its representation when the owning type exposes an API.
             */
            String eventId,
            /**
             * 中文说明：保存 failureCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayObservabilityStore.ConsumeFailure} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by failure code; its type is {@code String}, and {@code GatewayObservabilityStore.ConsumeFailure} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.ConsumeFailure} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.ConsumeFailure}; do not couple callers to its representation when the owning type exposes an API.
             */
            String failureCode,
            /**
             * 中文说明：保存 failure消息 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayObservabilityStore.ConsumeFailure} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by failure message; its type is {@code String}, and {@code GatewayObservabilityStore.ConsumeFailure} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.ConsumeFailure} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.ConsumeFailure}; do not couple callers to its representation when the owning type exposes an API.
             */
            String failureMessage,
            /**
             * 中文说明：保存 payloadSha256 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayObservabilityStore.ConsumeFailure} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by payload sha256; its type is {@code String}, and {@code GatewayObservabilityStore.ConsumeFailure} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.ConsumeFailure} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.ConsumeFailure}; do not couple callers to its representation when the owning type exposes an API.
             */
            String payloadSha256,
            /**
             * 中文说明：保存 payloadSize 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayObservabilityStore.ConsumeFailure} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by payload size; its type is {@code int}, and {@code GatewayObservabilityStore.ConsumeFailure} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.ConsumeFailure} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.ConsumeFailure}; do not couple callers to its representation when the owning type exposes an API.
             */
            int payloadSize,
            /**
             * 中文说明：保存 occurredAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayObservabilityStore.ConsumeFailure} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by occurred at; its type is {@code Instant}, and {@code GatewayObservabilityStore.ConsumeFailure} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.ConsumeFailure} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.ConsumeFailure}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant occurredAt
    ) {
    }

    /**
     * 中文说明：{@code TraceQuery} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责TraceQuery相关的职责与边界。
     * English summary: {@code TraceQuery} is an immutable data carrier in the current Gateway module; it owns the trace query-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param env 参数 env；parameter env。
     * @param namespace 参数 命名空间；parameter namespace。
     * @param traceId 参数 traceId；parameter trace id。
     * @param protocol 参数 protocol；parameter protocol。
     * @param statusCategory 参数 statusCategory；parameter status category。
     * @param page 参数 page；parameter page。
     * @param size 参数 size；parameter size。
     */
    record TraceQuery(
            /**
             * 中文说明：保存 env 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayObservabilityStore.TraceQuery} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by env; its type is {@code String}, and {@code GatewayObservabilityStore.TraceQuery} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.TraceQuery} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.TraceQuery}; do not couple callers to its representation when the owning type exposes an API.
             */
            String env,
            /**
             * 中文说明：保存 命名空间 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayObservabilityStore.TraceQuery} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by namespace; its type is {@code String}, and {@code GatewayObservabilityStore.TraceQuery} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.TraceQuery} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.TraceQuery}; do not couple callers to its representation when the owning type exposes an API.
             */
            String namespace,
            /**
             * 中文说明：保存 traceId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayObservabilityStore.TraceQuery} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by trace id; its type is {@code String}, and {@code GatewayObservabilityStore.TraceQuery} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.TraceQuery} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.TraceQuery}; do not couple callers to its representation when the owning type exposes an API.
             */
            String traceId,
            /**
             * 中文说明：保存 protocol 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayObservabilityStore.TraceQuery} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by protocol; its type is {@code String}, and {@code GatewayObservabilityStore.TraceQuery} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.TraceQuery} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.TraceQuery}; do not couple callers to its representation when the owning type exposes an API.
             */
            String protocol,
            /**
             * 中文说明：保存 statusCategory 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayObservabilityStore.TraceQuery} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by status category; its type is {@code String}, and {@code GatewayObservabilityStore.TraceQuery} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.TraceQuery} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.TraceQuery}; do not couple callers to its representation when the owning type exposes an API.
             */
            String statusCategory,
            /**
             * 中文说明：保存 page 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayObservabilityStore.TraceQuery} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by page; its type is {@code int}, and {@code GatewayObservabilityStore.TraceQuery} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.TraceQuery} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.TraceQuery}; do not couple callers to its representation when the owning type exposes an API.
             */
            int page,
            /**
             * 中文说明：保存 size 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayObservabilityStore.TraceQuery} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by size; its type is {@code int}, and {@code GatewayObservabilityStore.TraceQuery} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.TraceQuery} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.TraceQuery}; do not couple callers to its representation when the owning type exposes an API.
             */
            int size
    ) {

        /**
         * 中文说明：创建 {@code GatewayObservabilityStore.TraceQuery} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code GatewayObservabilityStore.TraceQuery} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param env 参数 env；parameter env。
         * @param namespace 参数 命名空间；parameter namespace。
         * @param traceId 参数 traceId；parameter trace id。
         * @param protocol 参数 protocol；parameter protocol。
         * @param statusCategory 参数 statusCategory；parameter status category。
         * @param page 参数 page；parameter page。
         * @param size 参数 size；parameter size。
         */
        public TraceQuery {
            if (page < 1 || size < 1 || size > 200) {
                throw new IllegalArgumentException(
                        "invalid trace page request"
                );
            }
        }
    }

    /**
     * 中文说明：{@code AuditQuery} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责审计Query相关的职责与边界。
     * English summary: {@code AuditQuery} is an immutable data carrier in the current Gateway module; it owns the audit query-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param env 参数 env；parameter env。
     * @param namespace 参数 命名空间；parameter namespace。
     * @param actorId 参数 actorId；parameter actor id。
     * @param resourceId 参数 资源Id；parameter resource id。
     * @param traceId 参数 traceId；parameter trace id。
     * @param successful 参数 successful；parameter successful。
     * @param page 参数 page；parameter page。
     * @param size 参数 size；parameter size。
     */
    record AuditQuery(
            /**
             * 中文说明：保存 env 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayObservabilityStore.AuditQuery} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by env; its type is {@code String}, and {@code GatewayObservabilityStore.AuditQuery} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.AuditQuery} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.AuditQuery}; do not couple callers to its representation when the owning type exposes an API.
             */
            String env,
            /**
             * 中文说明：保存 命名空间 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayObservabilityStore.AuditQuery} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by namespace; its type is {@code String}, and {@code GatewayObservabilityStore.AuditQuery} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.AuditQuery} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.AuditQuery}; do not couple callers to its representation when the owning type exposes an API.
             */
            String namespace,
            /**
             * 中文说明：保存 actorId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayObservabilityStore.AuditQuery} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by actor id; its type is {@code String}, and {@code GatewayObservabilityStore.AuditQuery} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.AuditQuery} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.AuditQuery}; do not couple callers to its representation when the owning type exposes an API.
             */
            String actorId,
            /**
             * 中文说明：保存 资源Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayObservabilityStore.AuditQuery} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by resource id; its type is {@code String}, and {@code GatewayObservabilityStore.AuditQuery} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.AuditQuery} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.AuditQuery}; do not couple callers to its representation when the owning type exposes an API.
             */
            String resourceId,
            /**
             * 中文说明：保存 traceId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayObservabilityStore.AuditQuery} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by trace id; its type is {@code String}, and {@code GatewayObservabilityStore.AuditQuery} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.AuditQuery} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.AuditQuery}; do not couple callers to its representation when the owning type exposes an API.
             */
            String traceId,
            /**
             * 中文说明：保存 successful 对应的状态、依赖或配置值；字段类型为 {@code Boolean}，由 {@code GatewayObservabilityStore.AuditQuery} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by successful; its type is {@code Boolean}, and {@code GatewayObservabilityStore.AuditQuery} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.AuditQuery} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.AuditQuery}; do not couple callers to its representation when the owning type exposes an API.
             */
            Boolean successful,
            /**
             * 中文说明：保存 page 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayObservabilityStore.AuditQuery} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by page; its type is {@code int}, and {@code GatewayObservabilityStore.AuditQuery} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.AuditQuery} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.AuditQuery}; do not couple callers to its representation when the owning type exposes an API.
             */
            int page,
            /**
             * 中文说明：保存 size 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayObservabilityStore.AuditQuery} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by size; its type is {@code int}, and {@code GatewayObservabilityStore.AuditQuery} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.AuditQuery} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.AuditQuery}; do not couple callers to its representation when the owning type exposes an API.
             */
            int size
    ) {

        /**
         * 中文说明：创建 {@code GatewayObservabilityStore.AuditQuery} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code GatewayObservabilityStore.AuditQuery} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param env 参数 env；parameter env。
         * @param namespace 参数 命名空间；parameter namespace。
         * @param actorId 参数 actorId；parameter actor id。
         * @param resourceId 参数 资源Id；parameter resource id。
         * @param traceId 参数 traceId；parameter trace id。
         * @param successful 参数 successful；parameter successful。
         * @param page 参数 page；parameter page。
         * @param size 参数 size；parameter size。
         */
        public AuditQuery {
            if (page < 1 || size < 1 || size > 200) {
                throw new IllegalArgumentException(
                        "invalid audit page request"
                );
            }
        }
    }

    /**
     * 中文说明：{@code TraceSummary} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责TraceSummary相关的职责与边界。
     * English summary: {@code TraceSummary} is an immutable data carrier in the current Gateway module; it owns the trace summary-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param eventId 参数 事件Id；parameter event id。
     * @param traceId 参数 traceId；parameter trace id。
     * @param startedAt 参数 startedAt；parameter started at。
     * @param durationMs 参数 durationMs；parameter duration ms。
     * @param protocol 参数 protocol；parameter protocol。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param operationKey 参数 操作键；parameter operation key。
     * @param statusCategory 参数 statusCategory；parameter status category。
     * @param engineInstanceId 参数 引擎InstanceId；parameter engine instance id。
     * @param providerService 参数 提供方服务；parameter provider service。
     */
    record TraceSummary(
            /**
             * 中文说明：保存 事件Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayObservabilityStore.TraceSummary} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by event id; its type is {@code String}, and {@code GatewayObservabilityStore.TraceSummary} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.TraceSummary} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.TraceSummary}; do not couple callers to its representation when the owning type exposes an API.
             */
            String eventId,
            /**
             * 中文说明：保存 traceId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayObservabilityStore.TraceSummary} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by trace id; its type is {@code String}, and {@code GatewayObservabilityStore.TraceSummary} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.TraceSummary} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.TraceSummary}; do not couple callers to its representation when the owning type exposes an API.
             */
            String traceId,
            /**
             * 中文说明：保存 startedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayObservabilityStore.TraceSummary} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by started at; its type is {@code Instant}, and {@code GatewayObservabilityStore.TraceSummary} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.TraceSummary} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.TraceSummary}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant startedAt,
            /**
             * 中文说明：保存 durationMs 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayObservabilityStore.TraceSummary} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by duration ms; its type is {@code long}, and {@code GatewayObservabilityStore.TraceSummary} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.TraceSummary} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.TraceSummary}; do not couple callers to its representation when the owning type exposes an API.
             */
            long durationMs,
            /**
             * 中文说明：保存 protocol 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayObservabilityStore.TraceSummary} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by protocol; its type is {@code String}, and {@code GatewayObservabilityStore.TraceSummary} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.TraceSummary} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.TraceSummary}; do not couple callers to its representation when the owning type exposes an API.
             */
            String protocol,
            /**
             * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayObservabilityStore.TraceSummary} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code GatewayObservabilityStore.TraceSummary} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.TraceSummary} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.TraceSummary}; do not couple callers to its representation when the owning type exposes an API.
             */
            String gatewayGroupId,
            /**
             * 中文说明：保存 操作键 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayObservabilityStore.TraceSummary} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by operation key; its type is {@code String}, and {@code GatewayObservabilityStore.TraceSummary} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.TraceSummary} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.TraceSummary}; do not couple callers to its representation when the owning type exposes an API.
             */
            String operationKey,
            /**
             * 中文说明：保存 statusCategory 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayObservabilityStore.TraceSummary} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by status category; its type is {@code String}, and {@code GatewayObservabilityStore.TraceSummary} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.TraceSummary} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.TraceSummary}; do not couple callers to its representation when the owning type exposes an API.
             */
            String statusCategory,
            /**
             * 中文说明：保存 引擎InstanceId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayObservabilityStore.TraceSummary} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by engine instance id; its type is {@code String}, and {@code GatewayObservabilityStore.TraceSummary} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.TraceSummary} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.TraceSummary}; do not couple callers to its representation when the owning type exposes an API.
             */
            String engineInstanceId,
            /**
             * 中文说明：保存 提供方服务 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayObservabilityStore.TraceSummary} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by provider service; its type is {@code String}, and {@code GatewayObservabilityStore.TraceSummary} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.TraceSummary} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.TraceSummary}; do not couple callers to its representation when the owning type exposes an API.
             */
            String providerService
    ) {
    }

    /**
     * 中文说明：{@code AuditSummary} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责审计Summary相关的职责与边界。
     * English summary: {@code AuditSummary} is an immutable data carrier in the current Gateway module; it owns the audit summary-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param id 参数 id；parameter id。
     * @param actorId 参数 actorId；parameter actor id。
     * @param actorType 参数 actorType；parameter actor type。
     * @param source 参数 source；parameter source。
     * @param traceId 参数 traceId；parameter trace id。
     * @param resourceType 参数 资源Type；parameter resource type。
     * @param resourceId 参数 资源Id；parameter resource id。
     * @param action 参数 action；parameter action。
     * @param beforeSummary 参数 beforeSummary；parameter before summary。
     * @param afterSummary 参数 afterSummary；parameter after summary。
     * @param draftRevision 参数 草稿Revision；parameter draft revision。
     * @param releaseId 参数 发布Id；parameter release id。
     * @param successful 参数 successful；parameter successful。
     * @param errorCode 参数 errorCode；parameter error code。
     * @param occurredAt 参数 occurredAt；parameter occurred at。
     */
    record AuditSummary(
            /**
             * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayObservabilityStore.AuditSummary} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code GatewayObservabilityStore.AuditSummary} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.AuditSummary} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.AuditSummary}; do not couple callers to its representation when the owning type exposes an API.
             */
            String id,
            /**
             * 中文说明：保存 actorId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayObservabilityStore.AuditSummary} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by actor id; its type is {@code String}, and {@code GatewayObservabilityStore.AuditSummary} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.AuditSummary} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.AuditSummary}; do not couple callers to its representation when the owning type exposes an API.
             */
            String actorId,
            /**
             * 中文说明：保存 actorType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayObservabilityStore.AuditSummary} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by actor type; its type is {@code String}, and {@code GatewayObservabilityStore.AuditSummary} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.AuditSummary} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.AuditSummary}; do not couple callers to its representation when the owning type exposes an API.
             */
            String actorType,
            /**
             * 中文说明：保存 source 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayObservabilityStore.AuditSummary} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by source; its type is {@code String}, and {@code GatewayObservabilityStore.AuditSummary} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.AuditSummary} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.AuditSummary}; do not couple callers to its representation when the owning type exposes an API.
             */
            String source,
            /**
             * 中文说明：保存 traceId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayObservabilityStore.AuditSummary} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by trace id; its type is {@code String}, and {@code GatewayObservabilityStore.AuditSummary} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.AuditSummary} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.AuditSummary}; do not couple callers to its representation when the owning type exposes an API.
             */
            String traceId,
            /**
             * 中文说明：保存 资源Type 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayObservabilityStore.AuditSummary} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by resource type; its type is {@code String}, and {@code GatewayObservabilityStore.AuditSummary} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.AuditSummary} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.AuditSummary}; do not couple callers to its representation when the owning type exposes an API.
             */
            String resourceType,
            /**
             * 中文说明：保存 资源Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayObservabilityStore.AuditSummary} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by resource id; its type is {@code String}, and {@code GatewayObservabilityStore.AuditSummary} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.AuditSummary} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.AuditSummary}; do not couple callers to its representation when the owning type exposes an API.
             */
            String resourceId,
            /**
             * 中文说明：保存 action 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayObservabilityStore.AuditSummary} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by action; its type is {@code String}, and {@code GatewayObservabilityStore.AuditSummary} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.AuditSummary} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.AuditSummary}; do not couple callers to its representation when the owning type exposes an API.
             */
            String action,
            /**
             * 中文说明：保存 beforeSummary 对应的状态、依赖或配置值；字段类型为 {@code Object}，由 {@code GatewayObservabilityStore.AuditSummary} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by before summary; its type is {@code Object}, and {@code GatewayObservabilityStore.AuditSummary} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.AuditSummary} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.AuditSummary}; do not couple callers to its representation when the owning type exposes an API.
             */
            Object beforeSummary,
            /**
             * 中文说明：保存 afterSummary 对应的状态、依赖或配置值；字段类型为 {@code Object}，由 {@code GatewayObservabilityStore.AuditSummary} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by after summary; its type is {@code Object}, and {@code GatewayObservabilityStore.AuditSummary} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.AuditSummary} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.AuditSummary}; do not couple callers to its representation when the owning type exposes an API.
             */
            Object afterSummary,
            /**
             * 中文说明：保存 草稿Revision 对应的状态、依赖或配置值；字段类型为 {@code Long}，由 {@code GatewayObservabilityStore.AuditSummary} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by draft revision; its type is {@code Long}, and {@code GatewayObservabilityStore.AuditSummary} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.AuditSummary} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.AuditSummary}; do not couple callers to its representation when the owning type exposes an API.
             */
            Long draftRevision,
            /**
             * 中文说明：保存 发布Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayObservabilityStore.AuditSummary} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by release id; its type is {@code String}, and {@code GatewayObservabilityStore.AuditSummary} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.AuditSummary} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.AuditSummary}; do not couple callers to its representation when the owning type exposes an API.
             */
            String releaseId,
            /**
             * 中文说明：保存 successful 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayObservabilityStore.AuditSummary} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by successful; its type is {@code boolean}, and {@code GatewayObservabilityStore.AuditSummary} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.AuditSummary} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.AuditSummary}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean successful,
            /**
             * 中文说明：保存 errorCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayObservabilityStore.AuditSummary} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by error code; its type is {@code String}, and {@code GatewayObservabilityStore.AuditSummary} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.AuditSummary} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.AuditSummary}; do not couple callers to its representation when the owning type exposes an API.
             */
            String errorCode,
            /**
             * 中文说明：保存 occurredAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayObservabilityStore.AuditSummary} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by occurred at; its type is {@code Instant}, and {@code GatewayObservabilityStore.AuditSummary} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.AuditSummary} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.AuditSummary}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant occurredAt
    ) {
    }

    /**
     * 中文说明：{@code RequestPoint} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责请求Point相关的职责与边界。
     * English summary: {@code RequestPoint} is an immutable data carrier in the current Gateway module; it owns the request point-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param time 参数 time；parameter time。
     * @param requests 参数 requests；parameter requests。
     * @param errors 参数 errors；parameter errors。
     * @param p50 参数 p50；parameter p50。
     * @param p95 参数 p95；parameter p95。
     * @param p99 参数 p99；parameter p99。
     */
    record RequestPoint(
            /**
             * 中文说明：保存 time 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayObservabilityStore.RequestPoint} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by time; its type is {@code Instant}, and {@code GatewayObservabilityStore.RequestPoint} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.RequestPoint} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.RequestPoint}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant time,
            /**
             * 中文说明：保存 requests 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayObservabilityStore.RequestPoint} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by requests; its type is {@code long}, and {@code GatewayObservabilityStore.RequestPoint} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.RequestPoint} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.RequestPoint}; do not couple callers to its representation when the owning type exposes an API.
             */
            long requests,
            /**
             * 中文说明：保存 errors 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayObservabilityStore.RequestPoint} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by errors; its type is {@code long}, and {@code GatewayObservabilityStore.RequestPoint} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.RequestPoint} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.RequestPoint}; do not couple callers to its representation when the owning type exposes an API.
             */
            long errors,
            /**
             * 中文说明：保存 p50 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayObservabilityStore.RequestPoint} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by p50; its type is {@code long}, and {@code GatewayObservabilityStore.RequestPoint} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.RequestPoint} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.RequestPoint}; do not couple callers to its representation when the owning type exposes an API.
             */
            long p50,
            /**
             * 中文说明：保存 p95 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayObservabilityStore.RequestPoint} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by p95; its type is {@code long}, and {@code GatewayObservabilityStore.RequestPoint} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.RequestPoint} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.RequestPoint}; do not couple callers to its representation when the owning type exposes an API.
             */
            long p95,
            /**
             * 中文说明：保存 p99 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayObservabilityStore.RequestPoint} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by p99; its type is {@code long}, and {@code GatewayObservabilityStore.RequestPoint} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.RequestPoint} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.RequestPoint}; do not couple callers to its representation when the owning type exposes an API.
             */
            long p99
    ) {
    }

    /**
     * 中文说明：{@code ProtocolCall} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Protocol调用相关的职责与边界。
     * English summary: {@code ProtocolCall} is an immutable data carrier in the current Gateway module; it owns the protocol call-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param protocol 参数 protocol；parameter protocol。
     * @param value 参数 值；parameter value。
     */
    record ProtocolCall(
    /**
     * 中文说明：保存 protocol 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayObservabilityStore.ProtocolCall} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by protocol; its type is {@code String}, and {@code GatewayObservabilityStore.ProtocolCall} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.ProtocolCall} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.ProtocolCall}; do not couple callers to its representation when the owning type exposes an API.
     */
    String protocol,
    /**
     * 中文说明：保存 值 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayObservabilityStore.ProtocolCall} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by value; its type is {@code long}, and {@code GatewayObservabilityStore.ProtocolCall} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.ProtocolCall} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.ProtocolCall}; do not couple callers to its representation when the owning type exposes an API.
     */
    long value) {
    }

    /**
     * 中文说明：{@code DashboardSummary} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责DashboardSummary相关的职责与边界。
     * English summary: {@code DashboardSummary} is an immutable data carrier in the current Gateway module; it owns the dashboard summary-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param gatewayGroups 参数 网关Groups；parameter gateway groups。
     * @param readyEngines 参数 readyEngines；parameter ready engines。
     * @param totalEngines 参数 totalEngines；parameter total engines。
     * @param inconsistentGroups 参数 inconsistentGroups；parameter inconsistent groups。
     * @param activeProviders 参数 activeProviders；parameter active providers。
     * @param abnormalProviders 参数 abnormalProviders；parameter abnormal providers。
     * @param releaseSuccessRate 参数 发布SuccessRate；parameter release success rate。
     * @param requestSeries 参数 请求Series；parameter request series。
     * @param protocolCalls 参数 protocolCalls；parameter protocol calls。
     * @param observabilityState 参数 可观测性State；parameter observability state。
     */
    record DashboardSummary(
            /**
             * 中文说明：保存 网关Groups 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayObservabilityStore.DashboardSummary} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway groups; its type is {@code long}, and {@code GatewayObservabilityStore.DashboardSummary} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.DashboardSummary} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.DashboardSummary}; do not couple callers to its representation when the owning type exposes an API.
             */
            long gatewayGroups,
            /**
             * 中文说明：保存 readyEngines 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayObservabilityStore.DashboardSummary} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by ready engines; its type is {@code long}, and {@code GatewayObservabilityStore.DashboardSummary} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.DashboardSummary} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.DashboardSummary}; do not couple callers to its representation when the owning type exposes an API.
             */
            long readyEngines,
            /**
             * 中文说明：保存 totalEngines 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayObservabilityStore.DashboardSummary} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by total engines; its type is {@code long}, and {@code GatewayObservabilityStore.DashboardSummary} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.DashboardSummary} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.DashboardSummary}; do not couple callers to its representation when the owning type exposes an API.
             */
            long totalEngines,
            /**
             * 中文说明：保存 inconsistentGroups 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayObservabilityStore.DashboardSummary} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by inconsistent groups; its type is {@code long}, and {@code GatewayObservabilityStore.DashboardSummary} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.DashboardSummary} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.DashboardSummary}; do not couple callers to its representation when the owning type exposes an API.
             */
            long inconsistentGroups,
            /**
             * 中文说明：保存 activeProviders 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayObservabilityStore.DashboardSummary} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by active providers; its type is {@code long}, and {@code GatewayObservabilityStore.DashboardSummary} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.DashboardSummary} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.DashboardSummary}; do not couple callers to its representation when the owning type exposes an API.
             */
            long activeProviders,
            /**
             * 中文说明：保存 abnormalProviders 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayObservabilityStore.DashboardSummary} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by abnormal providers; its type is {@code long}, and {@code GatewayObservabilityStore.DashboardSummary} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.DashboardSummary} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.DashboardSummary}; do not couple callers to its representation when the owning type exposes an API.
             */
            long abnormalProviders,
            /**
             * 中文说明：保存 发布SuccessRate 对应的状态、依赖或配置值；字段类型为 {@code double}，由 {@code GatewayObservabilityStore.DashboardSummary} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by release success rate; its type is {@code double}, and {@code GatewayObservabilityStore.DashboardSummary} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.DashboardSummary} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.DashboardSummary}; do not couple callers to its representation when the owning type exposes an API.
             */
            double releaseSuccessRate,
            /**
             * 中文说明：保存 请求Series 对应的状态、依赖或配置值；字段类型为 {@code List<RequestPoint>}，由 {@code GatewayObservabilityStore.DashboardSummary} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by request series; its type is {@code List<RequestPoint>}, and {@code GatewayObservabilityStore.DashboardSummary} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.DashboardSummary} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.DashboardSummary}; do not couple callers to its representation when the owning type exposes an API.
             */
            List<RequestPoint> requestSeries,
            /**
             * 中文说明：保存 protocolCalls 对应的状态、依赖或配置值；字段类型为 {@code List<ProtocolCall>}，由 {@code GatewayObservabilityStore.DashboardSummary} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by protocol calls; its type is {@code List<ProtocolCall>}, and {@code GatewayObservabilityStore.DashboardSummary} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.DashboardSummary} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.DashboardSummary}; do not couple callers to its representation when the owning type exposes an API.
             */
            List<ProtocolCall> protocolCalls,
            /**
             * 中文说明：保存 可观测性State 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayObservabilityStore.DashboardSummary} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by observability state; its type is {@code String}, and {@code GatewayObservabilityStore.DashboardSummary} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.DashboardSummary} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.DashboardSummary}; do not couple callers to its representation when the owning type exposes an API.
             */
            String observabilityState
    ) {

        /**
         * 中文说明：创建 {@code GatewayObservabilityStore.DashboardSummary} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code GatewayObservabilityStore.DashboardSummary} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param gatewayGroups 参数 网关Groups；parameter gateway groups。
         * @param readyEngines 参数 readyEngines；parameter ready engines。
         * @param totalEngines 参数 totalEngines；parameter total engines。
         * @param inconsistentGroups 参数 inconsistentGroups；parameter inconsistent groups。
         * @param activeProviders 参数 activeProviders；parameter active providers。
         * @param abnormalProviders 参数 abnormalProviders；parameter abnormal providers。
         * @param releaseSuccessRate 参数 发布SuccessRate；parameter release success rate。
         * @param requestSeries 参数 请求Series；parameter request series。
         * @param protocolCalls 参数 protocolCalls；parameter protocol calls。
         * @param observabilityState 参数 可观测性State；parameter observability state。
         */
        public DashboardSummary {
            requestSeries = List.copyOf(requestSeries);
            protocolCalls = List.copyOf(protocolCalls);
        }
    }

    /**
     * 中文说明：{@code Page} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Page相关的职责与边界。
     * English summary: {@code Page} is an immutable data carrier in the current Gateway module; it owns the page-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param items 参数 items；parameter items。
     * @param page 参数 page；parameter page。
     * @param size 参数 size；parameter size。
     * @param total 参数 total；parameter total。
     */
    record Page<T>(
    /**
     * 中文说明：保存 items 对应的状态、依赖或配置值；字段类型为 {@code List<T>}，由 {@code GatewayObservabilityStore.Page} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by items; its type is {@code List<T>}, and {@code GatewayObservabilityStore.Page} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.Page} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.Page}; do not couple callers to its representation when the owning type exposes an API.
     */
    List<T> items,
    /**
     * 中文说明：保存 page 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayObservabilityStore.Page} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by page; its type is {@code int}, and {@code GatewayObservabilityStore.Page} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.Page} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.Page}; do not couple callers to its representation when the owning type exposes an API.
     */
    int page,
    /**
     * 中文说明：保存 size 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayObservabilityStore.Page} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by size; its type is {@code int}, and {@code GatewayObservabilityStore.Page} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.Page} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.Page}; do not couple callers to its representation when the owning type exposes an API.
     */
    int size,
    /**
     * 中文说明：保存 total 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayObservabilityStore.Page} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by total; its type is {@code long}, and {@code GatewayObservabilityStore.Page} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayObservabilityStore.Page} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityStore.Page}; do not couple callers to its representation when the owning type exposes an API.
     */
    long total) {

        /**
         * 中文说明：创建 {@code GatewayObservabilityStore.Page} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code GatewayObservabilityStore.Page} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param items 参数 items；parameter items。
         * @param page 参数 page；parameter page。
         * @param size 参数 size；parameter size。
         * @param total 参数 total；parameter total。
         */
        public Page {
            items = List.copyOf(items);
        }
    }
}
