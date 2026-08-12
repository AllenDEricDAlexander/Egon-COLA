package top.egon.cola.component.gateway.engine.transport;

/**
 * Immutable names for externally observable transport commit facts.
 * 补充说明 / Supplementary summary: {@code GatewayCommitPoint} 是枚举类型，位于当前 Gateway 模块的相关包中，负责网关CommitPoint相关的职责与边界。
 * English supplement: {@code GatewayCommitPoint} is an enumeration in the current Gateway module; it owns the gateway commit point-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public enum GatewayCommitPoint {
    /**
     * 中文说明：表示 NEW 这一固定值；它属于 {@code GatewayCommitPoint} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value new; it is a state, type, or protocol value of {@code GatewayCommitPoint} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCommitPoint} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCommitPoint}; do not couple callers to its representation when the owning type exposes an API.
     */
    NEW(Flow.BOTH, 0),
    /**
     * 中文说明：表示 请求STREAMING 这一固定值；它属于 {@code GatewayCommitPoint} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value request streaming; it is a state, type, or protocol value of {@code GatewayCommitPoint} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCommitPoint} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCommitPoint}; do not couple callers to its representation when the owning type exposes an API.
     */
    REQUEST_STREAMING(Flow.HTTP, 1),
    /**
     * 中文说明：表示 UPSTREAMHEADERSRECEIVED 这一固定值；它属于 {@code GatewayCommitPoint} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value upstream headers received; it is a state, type, or protocol value of {@code GatewayCommitPoint} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCommitPoint} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCommitPoint}; do not couple callers to its representation when the owning type exposes an API.
     */
    UPSTREAM_HEADERS_RECEIVED(Flow.HTTP, 2),
    /**
     * 中文说明：表示 DOWNSTREAMHEADERSCOMMITTED 这一固定值；它属于 {@code GatewayCommitPoint} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value downstream headers committed; it is a state, type, or protocol value of {@code GatewayCommitPoint} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCommitPoint} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCommitPoint}; do not couple callers to its representation when the owning type exposes an API.
     */
    DOWNSTREAM_HEADERS_COMMITTED(Flow.HTTP, 3),
    /**
     * 中文说明：表示 FIRSTBODY缓冲区SENT 这一固定值；它属于 {@code GatewayCommitPoint} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value first body buffer sent; it is a state, type, or protocol value of {@code GatewayCommitPoint} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCommitPoint} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCommitPoint}; do not couple callers to its representation when the owning type exposes an API.
     */
    FIRST_BODY_BUFFER_SENT(Flow.HTTP, 4),
    /**
     * 中文说明：表示 UPSTREAMHANDSHAKERECEIVED 这一固定值；它属于 {@code GatewayCommitPoint} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value upstream handshake received; it is a state, type, or protocol value of {@code GatewayCommitPoint} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCommitPoint} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCommitPoint}; do not couple callers to its representation when the owning type exposes an API.
     */
    UPSTREAM_HANDSHAKE_RECEIVED(Flow.WEBSOCKET, 1),
    /**
     * 中文说明：表示 客户端HANDSHAKECOMMITTED 这一固定值；它属于 {@code GatewayCommitPoint} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value client handshake committed; it is a state, type, or protocol value of {@code GatewayCommitPoint} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCommitPoint} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCommitPoint}; do not couple callers to its representation when the owning type exposes an API.
     */
    CLIENT_HANDSHAKE_COMMITTED(Flow.WEBSOCKET, 2),
    /**
     * 中文说明：表示 FIRSTFRAMEFORWARDED 这一固定值；它属于 {@code GatewayCommitPoint} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value first frame forwarded; it is a state, type, or protocol value of {@code GatewayCommitPoint} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCommitPoint} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCommitPoint}; do not couple callers to its representation when the owning type exposes an API.
     */
    FIRST_FRAME_FORWARDED(Flow.WEBSOCKET, 3),
    /**
     * 中文说明：表示 TERMINATED 这一固定值；它属于 {@code GatewayCommitPoint} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value terminated; it is a state, type, or protocol value of {@code GatewayCommitPoint} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCommitPoint} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCommitPoint}; do not couple callers to its representation when the owning type exposes an API.
     */
    TERMINATED(Flow.BOTH, Integer.MAX_VALUE);

    /**
     * 中文说明：保存 flow 对应的状态、依赖或配置值；字段类型为 {@code Flow}，由 {@code GatewayCommitPoint} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by flow; its type is {@code Flow}, and {@code GatewayCommitPoint} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCommitPoint} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCommitPoint}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Flow flow;

    /**
     * 中文说明：保存 rank 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayCommitPoint} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by rank; its type is {@code int}, and {@code GatewayCommitPoint} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCommitPoint} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCommitPoint}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final int rank;

    /**
     * 中文说明：创建 {@code GatewayCommitPoint} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayCommitPoint} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param flow 参数 flow；parameter flow。
     * @param rank 参数 rank；parameter rank。
     */
    GatewayCommitPoint(Flow flow, int rank) {
        this.flow = flow;
        this.rank = rank;
    }

    /**
     * 中文说明：执行 supports 操作；该方法是 {@code GatewayCommitPoint} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the supports operation; this method is the invocation entry point on {@code GatewayCommitPoint} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCommitPoint.supports(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param expected 参数 expected；parameter expected。
     * @return 返回 supports 的处理结果；returns the result of the operation.
     */
    boolean supports(Flow expected) {
        return flow == Flow.BOTH || flow == expected;
    }

    /**
     * 中文说明：执行 rank 操作；该方法是 {@code GatewayCommitPoint} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the rank operation; this method is the invocation entry point on {@code GatewayCommitPoint} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCommitPoint.rank(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 rank 的处理结果；returns the result of the operation.
     */
    int rank() {
        return rank;
    }

    /**
     * 中文说明：{@code Flow} 是枚举类型，位于当前 Gateway 模块的相关包中，负责Flow相关的职责与边界。
     * English summary: {@code Flow} is an enumeration in the current Gateway module; it owns the flow-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    enum Flow {
        /**
         * 中文说明：表示 BOTH 这一固定值；它属于 {@code GatewayCommitPoint.Flow} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value both; it is a state, type, or protocol value of {@code GatewayCommitPoint.Flow} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayCommitPoint.Flow} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCommitPoint.Flow}; do not couple callers to its representation when the owning type exposes an API.
         */
        BOTH,
        /**
         * 中文说明：表示 HTTP 这一固定值；它属于 {@code GatewayCommitPoint.Flow} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value http; it is a state, type, or protocol value of {@code GatewayCommitPoint.Flow} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayCommitPoint.Flow} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCommitPoint.Flow}; do not couple callers to its representation when the owning type exposes an API.
         */
        HTTP,
        /**
         * 中文说明：表示 WebSocket 这一固定值；它属于 {@code GatewayCommitPoint.Flow} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value websocket; it is a state, type, or protocol value of {@code GatewayCommitPoint.Flow} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayCommitPoint.Flow} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCommitPoint.Flow}; do not couple callers to its representation when the owning type exposes an API.
         */
        WEBSOCKET
    }
}
