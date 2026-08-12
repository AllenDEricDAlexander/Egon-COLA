package top.egon.cola.component.gateway.admin.application;

/**
 * 中文说明：{@code GatewayAdminNotFoundException} 是异常类型，位于当前 Gateway 模块的相关包中，负责网关管理端NotFoundException相关的职责与边界。
 * English summary: {@code GatewayAdminNotFoundException} is a gateway admin not found exception exception in the current Gateway module; it owns the gateway admin not found exception-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayAdminNotFoundException extends RuntimeException {

    /**
     * 中文说明：创建 {@code GatewayAdminNotFoundException} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayAdminNotFoundException} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param message 参数 消息；parameter message。
     */
    public GatewayAdminNotFoundException(String message) {
        super(message);
    }
}
