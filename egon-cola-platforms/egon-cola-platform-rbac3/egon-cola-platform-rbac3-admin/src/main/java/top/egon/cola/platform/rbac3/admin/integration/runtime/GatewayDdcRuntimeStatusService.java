package top.egon.cola.platform.rbac3.admin.integration.runtime;

import top.egon.cola.platform.rbac3.admin.integration.ddc.DdcProviderLeaseStatusService;
import top.egon.cola.platform.rbac3.admin.integration.gateway.GatewayAdminControlPlaneStatusClient;
import top.egon.cola.platform.rbac3.admin.integration.gateway.GatewayDefinitionStatusService;
import top.egon.cola.platform.rbac3.admin.runtime.application.ControlPlaneRuntimeStatusPort;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 类型 `GatewayDdcRuntimeStatusService` 位于当前包内，是类型，用于承载 `Gateway Ddc Runtime Status Service` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `GatewayDdcRuntimeStatusService` is a type in its package and carries the responsibility, state, or contract for `Gateway Ddc Runtime Status Service`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Aggregates, but never collapses, definition, provider lease and release state.
 */
public final class GatewayDdcRuntimeStatusService
        implements ControlPlaneRuntimeStatusPort {

    /**
     * 字段 `definition` 表示 `GatewayDdcRuntimeStatusService` 中与 `definition` 相关的状态、依赖、配置或结果（声明类型 `Supplier&lt;GatewayDefinitionStatusService.DefinitionStatus&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `definition` stores the `definition`-related state, dependency, configuration, or result of `GatewayDdcRuntimeStatusService` (declared type `Supplier&lt;GatewayDefinitionStatusService.DefinitionStatus&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `definition` 时应保持 `GatewayDdcRuntimeStatusService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `definition`, preserve `GatewayDdcRuntimeStatusService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Supplier<GatewayDefinitionStatusService.DefinitionStatus> definition;
    /**
     * 字段 `lease` 表示 `GatewayDdcRuntimeStatusService` 中与 `lease` 相关的状态、依赖、配置或结果（声明类型 `Supplier&lt;DdcProviderLeaseStatusService.ProviderLeaseStatus&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `lease` stores the `lease`-related state, dependency, configuration, or result of `GatewayDdcRuntimeStatusService` (declared type `Supplier&lt;DdcProviderLeaseStatusService.ProviderLeaseStatus&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `lease` 时应保持 `GatewayDdcRuntimeStatusService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `lease`, preserve `GatewayDdcRuntimeStatusService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Supplier<DdcProviderLeaseStatusService.ProviderLeaseStatus> lease;
    /**
     * 字段 `gatewayAdmin` 表示 `GatewayDdcRuntimeStatusService` 中与 `gateway Admin` 相关的状态、依赖、配置或结果（声明类型 `GatewayAdminControlPlaneStatusClient`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `gatewayAdmin` stores the `gateway Admin`-related state, dependency, configuration, or result of `GatewayDdcRuntimeStatusService` (declared type `GatewayAdminControlPlaneStatusClient`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `gatewayAdmin` 时应保持 `GatewayDdcRuntimeStatusService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `gatewayAdmin`, preserve `GatewayDdcRuntimeStatusService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final GatewayAdminControlPlaneStatusClient gatewayAdmin;
    /**
     * 字段 `expectedIdentity` 表示 `GatewayDdcRuntimeStatusService` 中与 `expected Identity` 相关的状态、依赖、配置或结果（声明类型 `ServiceIdentity`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `expectedIdentity` stores the `expected Identity`-related state, dependency, configuration, or result of `GatewayDdcRuntimeStatusService` (declared type `ServiceIdentity`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `expectedIdentity` 时应保持 `GatewayDdcRuntimeStatusService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `expectedIdentity`, preserve `GatewayDdcRuntimeStatusService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ServiceIdentity expectedIdentity;
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
            GatewayDefinitionStatusService definition,
            DdcProviderLeaseStatusService lease,
            GatewayAdminControlPlaneStatusClient gatewayAdmin,
            ServiceIdentity expectedIdentity,
            Clock clock) {
        this(definition::status, lease::status, gatewayAdmin, expectedIdentity, clock);
    }

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
            Supplier<GatewayDefinitionStatusService.DefinitionStatus> definition,
            Supplier<DdcProviderLeaseStatusService.ProviderLeaseStatus> lease,
            GatewayAdminControlPlaneStatusClient gatewayAdmin,
            ServiceIdentity expectedIdentity,
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
    public RuntimeStatus status() {
        var definitionStatus = definition.get();
        var leaseStatus = lease.get();
        var gateway = gatewayAdmin.snapshot();
        Instant checkedAt = clock.instant();
        String routeability = routeability(
                definitionStatus, leaseStatus, gateway, checkedAt);
        return new RuntimeStatus(
                new DefinitionStatus(
                        definitionStatus.status(), definitionStatus.definitionSetId(),
                        definitionStatus.warnings()),
                new ProviderLeaseStatus(
                        leaseStatus.state(), leaseStatus.instanceId(),
                        leaseStatus.leaseExpireAt()),
                new GatewayReleaseStatus(
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
            GatewayDefinitionStatusService.DefinitionStatus definitionStatus,
            DdcProviderLeaseStatusService.ProviderLeaseStatus leaseStatus,
            GatewayAdminControlPlaneStatusClient.GatewayAdminSnapshot gateway,
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
            GatewayAdminControlPlaneStatusClient.GatewayAdminSnapshot gateway) {
        return "UNKNOWN".equals(gateway.release().state())
                || "UNKNOWN".equals(gateway.providers().state())
                || "UNKNOWN".equals(gateway.consistency().state());
    }

    /**
     * 类型 `ServiceIdentity` 位于 `GatewayDdcRuntimeStatusService` 内，是记录类型，用于承载 `Service Identity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ServiceIdentity` is a record inside `GatewayDdcRuntimeStatusService` and carries the responsibility, state, or contract for `Service Identity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ServiceIdentity` 作为 `GatewayDdcRuntimeStatusService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ServiceIdentity` as the responsibility boundary of `GatewayDdcRuntimeStatusService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param bizCode 记录组件 `bizCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `bizCode` carries constructor data whose meaning is defined by the record contract.
     * @param appCode 记录组件 `appCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `appCode` carries constructor data whose meaning is defined by the record contract.
     * @param env 记录组件 `env` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `env` carries constructor data whose meaning is defined by the record contract.
     * @param namespace 记录组件 `namespace` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `namespace` carries constructor data whose meaning is defined by the record contract.
     * @param serviceKind 记录组件 `serviceKind` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `serviceKind` carries constructor data whose meaning is defined by the record contract.
     * @param protocol 记录组件 `protocol` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `protocol` carries constructor data whose meaning is defined by the record contract.
     * @param serviceName 记录组件 `serviceName` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `serviceName` carries constructor data whose meaning is defined by the record contract.
     * @param group 记录组件 `group` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `group` carries constructor data whose meaning is defined by the record contract.
     * @param version 记录组件 `version` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `version` carries constructor data whose meaning is defined by the record contract.
     */
    public record ServiceIdentity(
            /**
             * 字段 `bizCode` 表示 `ServiceIdentity` 中与 `biz Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `bizCode` stores the `biz Code`-related state, dependency, configuration, or result of `ServiceIdentity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `bizCode` 时应保持 `ServiceIdentity` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `bizCode`, preserve `ServiceIdentity`'s lifecycle, immutability, and thread-safety constraints.
             */
            String bizCode,
            /**
             * 字段 `appCode` 表示 `ServiceIdentity` 中与 `app Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `appCode` stores the `app Code`-related state, dependency, configuration, or result of `ServiceIdentity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `appCode` 时应保持 `ServiceIdentity` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `appCode`, preserve `ServiceIdentity`'s lifecycle, immutability, and thread-safety constraints.
             */
            String appCode,
            /**
             * 字段 `env` 表示 `ServiceIdentity` 中与 `env` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `env` stores the `env`-related state, dependency, configuration, or result of `ServiceIdentity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `env` 时应保持 `ServiceIdentity` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `env`, preserve `ServiceIdentity`'s lifecycle, immutability, and thread-safety constraints.
             */
            String env,
            /**
             * 字段 `namespace` 表示 `ServiceIdentity` 中与 `namespace` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `namespace` stores the `namespace`-related state, dependency, configuration, or result of `ServiceIdentity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `namespace` 时应保持 `ServiceIdentity` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `namespace`, preserve `ServiceIdentity`'s lifecycle, immutability, and thread-safety constraints.
             */
            String namespace,
            /**
             * 字段 `serviceKind` 表示 `ServiceIdentity` 中与 `service Kind` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `serviceKind` stores the `service Kind`-related state, dependency, configuration, or result of `ServiceIdentity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `serviceKind` 时应保持 `ServiceIdentity` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `serviceKind`, preserve `ServiceIdentity`'s lifecycle, immutability, and thread-safety constraints.
             */
            String serviceKind,
            /**
             * 字段 `protocol` 表示 `ServiceIdentity` 中与 `protocol` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `protocol` stores the `protocol`-related state, dependency, configuration, or result of `ServiceIdentity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `protocol` 时应保持 `ServiceIdentity` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `protocol`, preserve `ServiceIdentity`'s lifecycle, immutability, and thread-safety constraints.
             */
            String protocol,
            /**
             * 字段 `serviceName` 表示 `ServiceIdentity` 中与 `service Name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `serviceName` stores the `service Name`-related state, dependency, configuration, or result of `ServiceIdentity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `serviceName` 时应保持 `ServiceIdentity` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `serviceName`, preserve `ServiceIdentity`'s lifecycle, immutability, and thread-safety constraints.
             */
            String serviceName,
            /**
             * 字段 `group` 表示 `ServiceIdentity` 中与 `group` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `group` stores the `group`-related state, dependency, configuration, or result of `ServiceIdentity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `group` 时应保持 `ServiceIdentity` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `group`, preserve `ServiceIdentity`'s lifecycle, immutability, and thread-safety constraints.
             */
            String group,
            /**
             * 字段 `version` 表示 `ServiceIdentity` 中与 `version` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `version` stores the `version`-related state, dependency, configuration, or result of `ServiceIdentity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `version` 时应保持 `ServiceIdentity` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `version`, preserve `ServiceIdentity`'s lifecycle, immutability, and thread-safety constraints.
             */
            String version) {

        /**
         * 构造器 `ServiceIdentity` 用于创建并初始化 `ServiceIdentity` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `ServiceIdentity` creates and initializes `ServiceIdentity`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `ServiceIdentity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `ServiceIdentity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param bizCode 输入参数 `bizCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param appCode 输入参数 `appCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param env 输入参数 `env`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param namespace 输入参数 `namespace`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param serviceKind 输入参数 `serviceKind`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param protocol 输入参数 `protocol`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param serviceName 输入参数 `serviceName`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param group 输入参数 `group`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param version 输入参数 `version`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public ServiceIdentity {
            bizCode = required(bizCode, "bizCode");
            appCode = required(appCode, "appCode");
            env = required(env, "env");
            namespace = required(namespace, "namespace");
            serviceKind = required(serviceKind, "serviceKind");
            protocol = required(protocol, "protocol");
            serviceName = required(serviceName, "serviceName");
            group = required(group, "group");
            version = required(version, "version");
        }

        /**
         * 方法 `matches` 按照 `ServiceIdentity` 的职责处理输入，完成 `matches` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `matches` processes its inputs according to `ServiceIdentity`'s responsibility, performs the `matches` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `matches` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `matches`, then continue the business flow using its result, exception, or side effect.
         *
         * @param key 输入参数 `key`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        boolean matches(GatewayAdminControlPlaneStatusClient.ServiceKey key) {
            return key != null
                    && bizCode.equals(key.bizCode())
                    && appCode.equals(key.appCode())
                    && env.equals(key.env())
                    && namespace.equals(key.namespace())
                    && serviceKind.equals(key.serviceKind())
                    && protocol.equals(key.protocol())
                    && serviceName.equals(key.serviceName())
                    && group.equals(key.group())
                    && version.equals(key.version());
        }

        /**
         * 方法 `required` 按照 `ServiceIdentity` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `required` processes its inputs according to `ServiceIdentity`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `required` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `required`, then continue the business flow using its result, exception, or side effect.
         *
         * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param field 输入参数 `field`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        private static String required(String value, String field) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(field + " is required");
            }
            return value.trim();
        }
    }
}
