package top.egon.cola.platform.rbac3.admin.runtime.service;

import top.egon.cola.platform.rbac3.admin.runtime.repository.GatewayAdminSnapshotRepository;
import top.egon.cola.platform.rbac3.admin.runtime.repository.GatewayDefinitionStatusPort;
import top.egon.cola.platform.rbac3.admin.runtime.repository.ProviderLeaseStatusPort;
import top.egon.cola.platform.rbac3.admin.runtime.service.ControlPlaneRuntimeStatusPort;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.ServiceIdentityVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.DdcProviderLeaseStatusVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.GatewayAdminSnapshotVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.GatewayDefinitionStatusVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.DefinitionStatusVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.GatewayReleaseStatusVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.ProviderLeaseStatusVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.RuntimeStatusVO;

/**
 * 类型 `GatewayDdcRuntimeStatusService` 位于当前包内，是类型，用于承载 `Gateway Ddc Runtime Status Service` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `GatewayDdcRuntimeStatusService` is a type in its package and carries the responsibility, state, or contract for `Gateway Ddc Runtime Status Service`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Aggregates, but never collapses, definition, provider lease and release state.
 */
public final class GatewayDdcRuntimeStatusService
        implements ControlPlaneRuntimeStatusPort {

    /**
     * 字段 `definition` 表示 `GatewayDdcRuntimeStatusService` 中与 `definition` 相关的状态、依赖、配置或结果（声明类型 `Supplier&lt;GatewayDefinitionStatusVO&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `definition` stores the `definition`-related state, dependency, configuration, or result of `GatewayDdcRuntimeStatusService` (declared type `Supplier&lt;GatewayDefinitionStatusVO&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `definition` 时应保持 `GatewayDdcRuntimeStatusService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `definition`, preserve `GatewayDdcRuntimeStatusService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final GatewayDefinitionStatusPort definition;
    /**
     * 字段 `lease` 表示 `GatewayDdcRuntimeStatusService` 中与 `lease` 相关的状态、依赖、配置或结果（声明类型 `Supplier&lt;DdcProviderLeaseStatusVO&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `lease` stores the `lease`-related state, dependency, configuration, or result of `GatewayDdcRuntimeStatusService` (declared type `Supplier&lt;DdcProviderLeaseStatusVO&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `lease` 时应保持 `GatewayDdcRuntimeStatusService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `lease`, preserve `GatewayDdcRuntimeStatusService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ProviderLeaseStatusPort lease;
    /**
     * 字段 `gatewayAdmin` 表示 `GatewayDdcRuntimeStatusService` 中与 `gateway Admin` 相关的状态、依赖、配置或结果（声明类型 `GatewayAdminControlPlaneStatusClient`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `gatewayAdmin` stores the `gateway Admin`-related state, dependency, configuration, or result of `GatewayDdcRuntimeStatusService` (declared type `GatewayAdminControlPlaneStatusClient`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `gatewayAdmin` 时应保持 `GatewayDdcRuntimeStatusService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `gatewayAdmin`, preserve `GatewayDdcRuntimeStatusService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final GatewayAdminSnapshotRepository gatewayAdmin;
    /**
     * 字段 `expectedIdentity` 表示 `GatewayDdcRuntimeStatusService` 中与 `expected Identity` 相关的状态、依赖、配置或结果（声明类型 `ServiceIdentityVO`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `expectedIdentity` stores the `expected Identity`-related state, dependency, configuration, or result of `GatewayDdcRuntimeStatusService` (declared type `ServiceIdentityVO`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `expectedIdentity` 时应保持 `GatewayDdcRuntimeStatusService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `expectedIdentity`, preserve `GatewayDdcRuntimeStatusService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ServiceIdentityVO expectedIdentity;
    /**
     * 字段 `clock` 表示 `GatewayDdcRuntimeStatusService` 中与 `clock` 相关的状态、依赖、配置或结果（声明类型 `Clock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `clock` stores the `clock`-related state, dependency, configuration, or result of `GatewayDdcRuntimeStatusService` (declared type `Clock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `clock` 时应保持 `GatewayDdcRuntimeStatusService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `clock`, preserve `GatewayDdcRuntimeStatusService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Clock clock;

    /**
     * 构造器 `GatewayDdcRuntimeStatusService` 用于创建并初始化 `GatewayDdcRuntimeStatusService` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `GatewayDdcRuntimeStatusService` creates and initializes `GatewayDdcRuntimeStatusService`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `GatewayDdcRuntimeStatusService` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `GatewayDdcRuntimeStatusService`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param definition 输入参数 `definition`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param lease 输入参数 `lease`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param gatewayAdmin 输入参数 `gatewayAdmin`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param expectedIdentity 输入参数 `expectedIdentity`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public GatewayDdcRuntimeStatusService(
            GatewayDefinitionStatusPort definition,
            ProviderLeaseStatusPort lease,
            GatewayAdminSnapshotRepository gatewayAdmin,
            ServiceIdentityVO expectedIdentity,
            Clock clock) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.lease = Objects.requireNonNull(lease, "lease");
        this.gatewayAdmin = Objects.requireNonNull(gatewayAdmin, "gatewayAdmin");
        this.expectedIdentity = Objects.requireNonNull(expectedIdentity, "expectedIdentity");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * 方法 `status` 按照 `GatewayDdcRuntimeStatusService` 的职责处理输入，完成 `status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `status` processes its inputs according to `GatewayDdcRuntimeStatusService`'s responsibility, performs the `status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `status` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `status`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    public RuntimeStatusVO status() {
        var definitionStatus = definition.status();
        var leaseStatus = lease.status();
        var gateway = gatewayAdmin.snapshot();
        Instant checkedAt = clock.instant();
        String routeability = routeability(
                definitionStatus, leaseStatus, gateway, checkedAt);
        return new RuntimeStatusVO(
                new DefinitionStatusVO(
                        definitionStatus.status(), definitionStatus.definitionSetId(),
                        definitionStatus.warnings()),
                new ProviderLeaseStatusVO(
                        leaseStatus.state(), leaseStatus.instanceId(),
                        leaseStatus.leaseExpireAt()),
                new GatewayReleaseStatusVO(
                        gateway.release().releaseId(), routeability,
                        gateway.consistency().observedVersion()),
                checkedAt);
    }

    /**
     * 方法 `routeability` 按照 `GatewayDdcRuntimeStatusService` 的职责处理输入，完成 `routeability` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `routeability` processes its inputs according to `GatewayDdcRuntimeStatusService`'s responsibility, performs the `routeability` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `routeability` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `routeability`, then continue the business flow using its result, exception, or side effect.
     *
     * @param definitionStatus 输入参数 `definitionStatus`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param leaseStatus 输入参数 `leaseStatus`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param gateway 输入参数 `gateway`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param checkedAt 输入参数 `checkedAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private String routeability(
            GatewayDefinitionStatusVO definitionStatus,
            DdcProviderLeaseStatusVO leaseStatus,
            GatewayAdminSnapshotVO gateway,
            Instant checkedAt) {
        if (unknown(gateway)) {
            return "UNKNOWN";
        }
        if (!definitionStatus.accepted()
                || !"REGISTERED".equals(leaseStatus.state())
                || leaseStatus.leaseExpireAt() == null
                || !leaseStatus.leaseExpireAt().isAfter(checkedAt)
                || !expectedIdentity.equals(definitionStatus.identity())
                || !expectedIdentity.equals(leaseStatus.identity())
                || !"SUCCESS".equals(gateway.release().releaseStatus())
                || !Objects.equals(
                        definitionStatus.definitionSetId(),
                        gateway.release().definitionSetId())
                || !Objects.equals(
                        expectedIdentity.version(),
                        gateway.release().publishedVersion())
                || !gateway.consistency().consistent()
                || !Objects.equals(
                        gateway.release().releaseId(), gateway.consistency().releaseId())
                || !"SUCCESS".equals(gateway.consistency().releaseStatus())) {
            return "NOT_ROUTABLE";
        }
        boolean providerMatches = gateway.providers().instances().stream()
                .filter(instance -> "UP".equals(instance.status())
                        || "ONLINE".equals(instance.status())
                        || "ACTIVE".equals(instance.status()))
                .anyMatch(instance -> expectedIdentity.matches(instance.serviceKey())
                        && Objects.equals(
                        definitionStatus.definitionSetId(), instance.definitionSetId()));
        return providerMatches ? "ROUTABLE" : "NOT_ROUTABLE";
    }

    /**
     * 方法 `unknown` 按照 `GatewayDdcRuntimeStatusService` 的职责处理输入，完成 `unknown` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `unknown` processes its inputs according to `GatewayDdcRuntimeStatusService`'s responsibility, performs the `unknown` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `unknown` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `unknown`, then continue the business flow using its result, exception, or side effect.
     *
     * @param gateway 输入参数 `gateway`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private boolean unknown(
            GatewayAdminSnapshotVO gateway) {
        return "UNKNOWN".equals(gateway.release().state())
                || "UNKNOWN".equals(gateway.providers().state())
                || "UNKNOWN".equals(gateway.consistency().state());
    }

    }
