package top.egon.cola.platform.rbac3.admin.integration.ddc;

import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;
import top.egon.cola.component.ddc.http.registration.DdcHttpRegistrationRuntime;
import top.egon.cola.platform.rbac3.admin.integration.runtime.GatewayDdcRuntimeStatusService;

import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 类型 `DdcProviderLeaseStatusService` 位于当前包内，是类型，用于承载 `Ddc Provider Lease Status Service` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `DdcProviderLeaseStatusService` is a type in its package and carries the responsibility, state, or contract for `Ddc Provider Lease Status Service`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Maps the existing provider lease state machine to the RBAC3 status contract.
 */
public final class DdcProviderLeaseStatusService {

    /**
     * 字段 `status` 表示 `DdcProviderLeaseStatusService` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `Supplier&lt;ProviderLeaseStatus&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `status` stores the `status`-related state, dependency, configuration, or result of `DdcProviderLeaseStatusService` (declared type `Supplier&lt;ProviderLeaseStatus&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `status` 时应保持 `DdcProviderLeaseStatusService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `status`, preserve `DdcProviderLeaseStatusService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Supplier<ProviderLeaseStatus> status;

    /**
     * 构造器 `DdcProviderLeaseStatusService` 用于创建并初始化 `DdcProviderLeaseStatusService` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `DdcProviderLeaseStatusService` creates and initializes `DdcProviderLeaseStatusService`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `DdcProviderLeaseStatusService` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `DdcProviderLeaseStatusService`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param runtime 输入参数 `runtime`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param identity 输入参数 `identity`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public DdcProviderLeaseStatusService(
            DdcHttpRegistrationRuntime runtime,
            GatewayDdcRuntimeStatusService.ServiceIdentity identity) {
        Objects.requireNonNull(runtime, "runtime");
        Objects.requireNonNull(identity, "identity");
        this.status = () -> {
            DdcLeaseSession lease = runtime.lease().orElse(null);
            return new ProviderLeaseStatus(
                    runtime.state().name(), runtime.instanceId(),
                    lease == null ? null : lease.leaseExpireAt(), identity);
        };
    }

    /**
     * 构造器 `DdcProviderLeaseStatusService` 用于创建并初始化 `DdcProviderLeaseStatusService` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `DdcProviderLeaseStatusService` creates and initializes `DdcProviderLeaseStatusService`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `DdcProviderLeaseStatusService` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `DdcProviderLeaseStatusService`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public DdcProviderLeaseStatusService(Supplier<ProviderLeaseStatus> status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    /**
     * 方法 `status` 按照 `DdcProviderLeaseStatusService` 的职责处理输入，完成 `status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `status` processes its inputs according to `DdcProviderLeaseStatusService`'s responsibility, performs the `status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `status` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `status`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public ProviderLeaseStatus status() {
        return status.get();
    }

    /**
     * 类型 `ProviderLeaseStatus` 位于 `DdcProviderLeaseStatusService` 内，是记录类型，用于承载 `Provider Lease Status` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ProviderLeaseStatus` is a record inside `DdcProviderLeaseStatusService` and carries the responsibility, state, or contract for `Provider Lease Status`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ProviderLeaseStatus` 作为 `DdcProviderLeaseStatusService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ProviderLeaseStatus` as the responsibility boundary of `DdcProviderLeaseStatusService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param state 记录组件 `state` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `state` carries constructor data whose meaning is defined by the record contract.
     * @param instanceId 记录组件 `instanceId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `instanceId` carries constructor data whose meaning is defined by the record contract.
     * @param leaseExpireAt 记录组件 `leaseExpireAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `leaseExpireAt` carries constructor data whose meaning is defined by the record contract.
     * @param identity 记录组件 `identity` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `identity` carries constructor data whose meaning is defined by the record contract.
     */
    public record ProviderLeaseStatus(
            /**
             * 字段 `state` 表示 `ProviderLeaseStatus` 中与 `state` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `state` stores the `state`-related state, dependency, configuration, or result of `ProviderLeaseStatus` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `state` 时应保持 `ProviderLeaseStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `state`, preserve `ProviderLeaseStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            String state,
            /**
             * 字段 `instanceId` 表示 `ProviderLeaseStatus` 中与 `instance Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `instanceId` stores the `instance Id`-related state, dependency, configuration, or result of `ProviderLeaseStatus` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `instanceId` 时应保持 `ProviderLeaseStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `instanceId`, preserve `ProviderLeaseStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            String instanceId,
            /**
             * 字段 `leaseExpireAt` 表示 `ProviderLeaseStatus` 中与 `lease Expire At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `leaseExpireAt` stores the `lease Expire At`-related state, dependency, configuration, or result of `ProviderLeaseStatus` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `leaseExpireAt` 时应保持 `ProviderLeaseStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `leaseExpireAt`, preserve `ProviderLeaseStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant leaseExpireAt,
            /**
             * 字段 `identity` 表示 `ProviderLeaseStatus` 中与 `identity` 相关的状态、依赖、配置或结果（声明类型 `GatewayDdcRuntimeStatusService.ServiceIdentity`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `identity` stores the `identity`-related state, dependency, configuration, or result of `ProviderLeaseStatus` (declared type `GatewayDdcRuntimeStatusService.ServiceIdentity`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `identity` 时应保持 `ProviderLeaseStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `identity`, preserve `ProviderLeaseStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            GatewayDdcRuntimeStatusService.ServiceIdentity identity) {
    }
}
