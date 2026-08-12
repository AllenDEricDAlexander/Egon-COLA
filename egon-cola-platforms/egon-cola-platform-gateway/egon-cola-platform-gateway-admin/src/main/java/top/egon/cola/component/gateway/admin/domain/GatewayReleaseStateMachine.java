package top.egon.cola.component.gateway.admin.domain;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 中文说明：{@code GatewayReleaseStateMachine} 是类型，位于当前 Gateway 模块的相关包中，负责网关发布StateMachine相关的职责与边界。
 * English summary: {@code GatewayReleaseStateMachine} is a type in the current Gateway module; it owns the gateway release state machine-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayReleaseStateMachine {

    /**
     * 中文说明：表示 TRANSITIONS 这一固定值；它属于 {@code GatewayReleaseStateMachine} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value transitions; it is a state, type, or protocol value of {@code GatewayReleaseStateMachine} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReleaseStateMachine} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseStateMachine}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final Map<GatewayReleaseStatus, Set<GatewayReleaseStatus>>
            TRANSITIONS = Map.of(
            GatewayReleaseStatus.CREATED,
            Set.of(GatewayReleaseStatus.VALIDATING),
            GatewayReleaseStatus.VALIDATING,
            Set.of(
                    GatewayReleaseStatus.READY,
                    GatewayReleaseStatus.FAILED
            ),
            GatewayReleaseStatus.READY,
            Set.of(GatewayReleaseStatus.PUBLISHING),
            GatewayReleaseStatus.PUBLISHING,
            Set.of(
                    GatewayReleaseStatus.SUCCESS,
                    GatewayReleaseStatus.FAILED,
                    GatewayReleaseStatus.TIMEOUT,
                    GatewayReleaseStatus.UNKNOWN
            ),
            GatewayReleaseStatus.SUCCESS,
            Set.of(GatewayReleaseStatus.SUPERSEDED),
            GatewayReleaseStatus.FAILED,
            Set.of(GatewayReleaseStatus.PUBLISHING),
            GatewayReleaseStatus.TIMEOUT,
            Set.of(GatewayReleaseStatus.PUBLISHING),
            GatewayReleaseStatus.UNKNOWN,
            Set.of(GatewayReleaseStatus.PUBLISHING),
            GatewayReleaseStatus.SUPERSEDED,
            Set.of()
    );

    /**
     * 中文说明：保存 status 对应的状态、依赖或配置值；字段类型为 {@code GatewayReleaseStatus}，由 {@code GatewayReleaseStateMachine} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by status; its type is {@code GatewayReleaseStatus}, and {@code GatewayReleaseStateMachine} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReleaseStateMachine} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseStateMachine}; do not couple callers to its representation when the owning type exposes an API.
     */
    private GatewayReleaseStatus status;

    /**
     * 中文说明：创建 {@code GatewayReleaseStateMachine} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayReleaseStateMachine} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param status 参数 status；parameter status。
     */
    public GatewayReleaseStateMachine(GatewayReleaseStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    /**
     * 中文说明：执行 status 操作；该方法是 {@code GatewayReleaseStateMachine} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the status operation; this method is the invocation entry point on {@code GatewayReleaseStateMachine} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseStateMachine.status(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 status 的处理结果；returns the result of the operation.
     */
    public GatewayReleaseStatus status() {
        return status;
    }

    /**
     * 中文说明：执行 transitionTo 操作；该方法是 {@code GatewayReleaseStateMachine} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the transition to operation; this method is the invocation entry point on {@code GatewayReleaseStateMachine} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseStateMachine.transitionTo(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param target 参数 target；parameter target。
     */
    public void transitionTo(GatewayReleaseStatus target) {
        Objects.requireNonNull(target, "target");
        if (!TRANSITIONS.getOrDefault(status, Set.of()).contains(target)) {
            throw new IllegalStateException(
                    "illegal release transition " + status + " -> " + target
            );
        }
        status = target;
    }
}
