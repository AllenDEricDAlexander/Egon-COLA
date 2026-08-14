package top.egon.cola.component.gateway.admin.routing.domain;


import top.egon.cola.component.gateway.admin.shared.domain.exception.GatewayAdminRevisionConflictException;

/**
 * 中文说明：{@code GatewayDraftRevision} 是类型，位于当前 Gateway 模块的相关包中，负责网关草稿Revision相关的职责与边界。
 * English summary: {@code GatewayDraftRevision} is a type in the current Gateway module; it owns the gateway draft revision-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayDraftRevision {

    /**
     * 中文说明：保存 值 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayDraftRevision} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by value; its type is {@code long}, and {@code GatewayDraftRevision} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDraftRevision} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftRevision}; do not couple callers to its representation when the owning type exposes an API.
     */
    private long value;

    /**
     * 中文说明：创建 {@code GatewayDraftRevision} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayDraftRevision} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param value 参数 值；parameter value。
     */
    public GatewayDraftRevision(long value) {
        if (value < 0) {
            throw new IllegalArgumentException(
                    "revision must not be negative"
            );
        }
        this.value = value;
    }

    /**
     * 中文说明：执行 值 操作；该方法是 {@code GatewayDraftRevision} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the value operation; this method is the invocation entry point on {@code GatewayDraftRevision} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftRevision.value(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 值 的处理结果；returns the result of the operation.
     */
    public long value() {
        return value;
    }

    /**
     * 中文说明：执行 advance 操作；该方法是 {@code GatewayDraftRevision} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the advance operation; this method is the invocation entry point on {@code GatewayDraftRevision} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftRevision.advance(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @return 返回 advance 的处理结果；returns the result of the operation.
     */
    public long advance(long expectedRevision) {
        if (expectedRevision != value) {
            throw new GatewayAdminRevisionConflictException(value);
        }
        return ++value;
    }
}
