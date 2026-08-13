package top.egon.cola.platform.rbac3.admin.runtime.repository.ddc;

import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;
import top.egon.cola.component.ddc.http.registration.DdcHttpRegistrationRuntime;
import top.egon.cola.platform.rbac3.admin.runtime.service.GatewayDdcRuntimeStatusService;

import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.DdcProviderLeaseStatusVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.ServiceIdentityVO;

/**
 * 类型 `DdcProviderLeaseStatusRepository` 位于当前包内，是类型，用于承载 `Ddc Provider Lease Status Service` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `DdcProviderLeaseStatusRepository` is a type in its package and carries the responsibility, state, or contract for `Ddc Provider Lease Status Service`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Maps the existing provider lease state machine to the RBAC3 status contract.
 */
public final class DdcProviderLeaseStatusRepository {

    /**
     * 字段 `status` 表示 `DdcProviderLeaseStatusRepository` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `Supplier&lt;DdcProviderLeaseStatusVO&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `status` stores the `status`-related state, dependency, configuration, or result of `DdcProviderLeaseStatusRepository` (declared type `Supplier&lt;DdcProviderLeaseStatusVO&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `status` 时应保持 `DdcProviderLeaseStatusRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `status`, preserve `DdcProviderLeaseStatusRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Supplier<DdcProviderLeaseStatusVO> status;

    /**
     * 构造器 `DdcProviderLeaseStatusRepository` 用于创建并初始化 `DdcProviderLeaseStatusRepository` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `DdcProviderLeaseStatusRepository` creates and initializes `DdcProviderLeaseStatusRepository`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `DdcProviderLeaseStatusRepository` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `DdcProviderLeaseStatusRepository`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param runtime 输入参数 `runtime`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param identity 输入参数 `identity`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public DdcProviderLeaseStatusRepository(
            DdcHttpRegistrationRuntime runtime,
            ServiceIdentityVO identity) {
        Objects.requireNonNull(runtime, "runtime");
        Objects.requireNonNull(identity, "identity");
        this.status = () -> {
            DdcLeaseSession lease = runtime.lease().orElse(null);
            return new DdcProviderLeaseStatusVO(
                    runtime.state().name(), runtime.instanceId(),
                    lease == null ? null : lease.leaseExpireAt(), identity);
        };
    }

    /**
     * 构造器 `DdcProviderLeaseStatusRepository` 用于创建并初始化 `DdcProviderLeaseStatusRepository` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `DdcProviderLeaseStatusRepository` creates and initializes `DdcProviderLeaseStatusRepository`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `DdcProviderLeaseStatusRepository` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `DdcProviderLeaseStatusRepository`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public DdcProviderLeaseStatusRepository(Supplier<DdcProviderLeaseStatusVO> status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    /**
     * 方法 `status` 按照 `DdcProviderLeaseStatusRepository` 的职责处理输入，完成 `status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `status` processes its inputs according to `DdcProviderLeaseStatusRepository`'s responsibility, performs the `status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `status` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `status`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public DdcProviderLeaseStatusVO status() {
        return status.get();
    }

    }
