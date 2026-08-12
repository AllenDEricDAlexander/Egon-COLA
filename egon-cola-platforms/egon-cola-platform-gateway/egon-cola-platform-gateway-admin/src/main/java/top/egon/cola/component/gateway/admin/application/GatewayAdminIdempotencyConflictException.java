package top.egon.cola.component.gateway.admin.application;

/**
 * 中文说明：{@code GatewayAdminIdempotencyConflictException} 是异常类型，位于当前 Gateway 模块的相关包中，负责网关管理端IdempotencyConflictException相关的职责与边界。
 * English summary: {@code GatewayAdminIdempotencyConflictException} is a gateway admin idempotency conflict exception exception in the current Gateway module; it owns the gateway admin idempotency conflict exception-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public class GatewayAdminIdempotencyConflictException
        extends RuntimeException {

    /**
     * 中文说明：创建 {@code GatewayAdminIdempotencyConflictException} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayAdminIdempotencyConflictException} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     */
    public GatewayAdminIdempotencyConflictException() {
        super("idempotency key was reused with a different payload");
    }
}
