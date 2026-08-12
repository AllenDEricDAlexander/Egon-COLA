package top.egon.cola.component.gateway.engine.transport;

/**
 * Base failure for a specifically identified transport timeout boundary.
 * 补充说明 / Supplementary summary: {@code GatewayTransportTimeoutException} 是异常类型，位于当前 Gateway 模块的相关包中，负责网关传输超时Exception相关的职责与边界。
 * English supplement: {@code GatewayTransportTimeoutException} is a gateway transport timeout exception exception in the current Gateway module; it owns the gateway transport timeout exception-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public abstract class GatewayTransportTimeoutException
        extends RuntimeException {

    /**
     * 中文说明：保存 errorCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayTransportTimeoutException} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by error code; its type is {@code String}, and {@code GatewayTransportTimeoutException} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayTransportTimeoutException} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTransportTimeoutException}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final String errorCode;

    /**
     * 中文说明：创建 {@code GatewayTransportTimeoutException} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayTransportTimeoutException} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param errorCode 参数 errorCode；parameter error code。
     * @param message 参数 消息；parameter message。
     */
    protected GatewayTransportTimeoutException(
            String errorCode,
            String message) {
        super(message, null, false, false);
        this.errorCode = errorCode;
    }

    /**
     * 中文说明：执行 errorCode 操作；该方法是 {@code GatewayTransportTimeoutException} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the error code operation; this method is the invocation entry point on {@code GatewayTransportTimeoutException} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTransportTimeoutException.errorCode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 errorCode 的处理结果；returns the result of the operation.
     */
    public String errorCode() {
        return errorCode;
    }
}
