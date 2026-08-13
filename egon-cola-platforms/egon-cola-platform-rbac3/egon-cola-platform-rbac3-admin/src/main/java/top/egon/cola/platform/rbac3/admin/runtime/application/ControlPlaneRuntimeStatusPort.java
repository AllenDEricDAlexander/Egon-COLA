package top.egon.cola.platform.rbac3.admin.runtime.application;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 类型 `ControlPlaneRuntimeStatusPort` 位于当前包内，是接口，用于承载 `Control Plane Runtime Status Port` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `ControlPlaneRuntimeStatusPort` is an interface in its package and carries the responsibility, state, or contract for `Control Plane Runtime Status Port`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Read-only boundary for Gateway definition, DDC lease and release observations.
 */
@FunctionalInterface
public interface ControlPlaneRuntimeStatusPort {

    /**
     * 方法 `status` 按照 `ControlPlaneRuntimeStatusPort` 的职责处理输入，完成 `status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `status` processes its inputs according to `ControlPlaneRuntimeStatusPort`'s responsibility, performs the `status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `status` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `status`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    RuntimeStatus status();

    /**
     * 类型 `RuntimeStatus` 位于 `ControlPlaneRuntimeStatusPort` 内，是记录类型，用于承载 `Runtime Status` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RuntimeStatus` is a record inside `ControlPlaneRuntimeStatusPort` and carries the responsibility, state, or contract for `Runtime Status`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RuntimeStatus` 作为 `ControlPlaneRuntimeStatusPort` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RuntimeStatus` as the responsibility boundary of `ControlPlaneRuntimeStatusPort`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param ddcConfigClient 记录组件 `ddcConfigClient` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `ddcConfigClient` carries constructor data whose meaning is defined by the record contract.
     * @param definition 记录组件 `definition` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `definition` carries constructor data whose meaning is defined by the record contract.
     * @param providerLease 记录组件 `providerLease` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `providerLease` carries constructor data whose meaning is defined by the record contract.
     * @param gatewayRelease 记录组件 `gatewayRelease` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `gatewayRelease` carries constructor data whose meaning is defined by the record contract.
     * @param flyway 记录组件 `flyway` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `flyway` carries constructor data whose meaning is defined by the record contract.
     * @param redisProjection 记录组件 `redisProjection` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `redisProjection` carries constructor data whose meaning is defined by the record contract.
     * @param fence 记录组件 `fence` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `fence` carries constructor data whose meaning is defined by the record contract.
     * @param outbox 记录组件 `outbox` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `outbox` carries constructor data whose meaning is defined by the record contract.
     * @param checkedAt 记录组件 `checkedAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `checkedAt` carries constructor data whose meaning is defined by the record contract.
     */
    record RuntimeStatus(
            /**
             * 字段 `ddcConfigClient` 表示 `RuntimeStatus` 中与 `ddc Config Client` 相关的状态、依赖、配置或结果（声明类型 `DdcConfigClientStatus`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `ddcConfigClient` stores the `ddc Config Client`-related state, dependency, configuration, or result of `RuntimeStatus` (declared type `DdcConfigClientStatus`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `ddcConfigClient` 时应保持 `RuntimeStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `ddcConfigClient`, preserve `RuntimeStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            DdcConfigClientStatus ddcConfigClient,
            /**
             * 字段 `definition` 表示 `RuntimeStatus` 中与 `definition` 相关的状态、依赖、配置或结果（声明类型 `DefinitionStatus`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `definition` stores the `definition`-related state, dependency, configuration, or result of `RuntimeStatus` (declared type `DefinitionStatus`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `definition` 时应保持 `RuntimeStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `definition`, preserve `RuntimeStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            DefinitionStatus definition,
            /**
             * 字段 `providerLease` 表示 `RuntimeStatus` 中与 `provider Lease` 相关的状态、依赖、配置或结果（声明类型 `ProviderLeaseStatus`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `providerLease` stores the `provider Lease`-related state, dependency, configuration, or result of `RuntimeStatus` (declared type `ProviderLeaseStatus`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `providerLease` 时应保持 `RuntimeStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `providerLease`, preserve `RuntimeStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            ProviderLeaseStatus providerLease,
            /**
             * 字段 `gatewayRelease` 表示 `RuntimeStatus` 中与 `gateway Release` 相关的状态、依赖、配置或结果（声明类型 `GatewayReleaseStatus`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `gatewayRelease` stores the `gateway Release`-related state, dependency, configuration, or result of `RuntimeStatus` (declared type `GatewayReleaseStatus`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `gatewayRelease` 时应保持 `RuntimeStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `gatewayRelease`, preserve `RuntimeStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            GatewayReleaseStatus gatewayRelease,
            /**
             * 字段 `flyway` 表示 `RuntimeStatus` 中与 `flyway` 相关的状态、依赖、配置或结果（声明类型 `FlywayStatus`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `flyway` stores the `flyway`-related state, dependency, configuration, or result of `RuntimeStatus` (declared type `FlywayStatus`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `flyway` 时应保持 `RuntimeStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `flyway`, preserve `RuntimeStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            FlywayStatus flyway,
            /**
             * 字段 `redisProjection` 表示 `RuntimeStatus` 中与 `redis Projection` 相关的状态、依赖、配置或结果（声明类型 `RedisProjectionStatus`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `redisProjection` stores the `redis Projection`-related state, dependency, configuration, or result of `RuntimeStatus` (declared type `RedisProjectionStatus`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `redisProjection` 时应保持 `RuntimeStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `redisProjection`, preserve `RuntimeStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            RedisProjectionStatus redisProjection,
            /**
             * 字段 `fence` 表示 `RuntimeStatus` 中与 `fence` 相关的状态、依赖、配置或结果（声明类型 `FenceMutationStatus`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `fence` stores the `fence`-related state, dependency, configuration, or result of `RuntimeStatus` (declared type `FenceMutationStatus`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `fence` 时应保持 `RuntimeStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `fence`, preserve `RuntimeStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            FenceMutationStatus fence,
            /**
             * 字段 `outbox` 表示 `RuntimeStatus` 中与 `outbox` 相关的状态、依赖、配置或结果（声明类型 `OutboxStatus`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `outbox` stores the `outbox`-related state, dependency, configuration, or result of `RuntimeStatus` (declared type `OutboxStatus`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `outbox` 时应保持 `RuntimeStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `outbox`, preserve `RuntimeStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            OutboxStatus outbox,
            /**
             * 字段 `checkedAt` 表示 `RuntimeStatus` 中与 `checked At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `checkedAt` stores the `checked At`-related state, dependency, configuration, or result of `RuntimeStatus` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `checkedAt` 时应保持 `RuntimeStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `checkedAt`, preserve `RuntimeStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant checkedAt) {

        /**
         * 构造器 `RuntimeStatus` 用于创建并初始化 `RuntimeStatus` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `RuntimeStatus` creates and initializes `RuntimeStatus`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `RuntimeStatus` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `RuntimeStatus`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param ddcConfigClient 输入参数 `ddcConfigClient`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param definition 输入参数 `definition`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param providerLease 输入参数 `providerLease`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param gatewayRelease 输入参数 `gatewayRelease`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param flyway 输入参数 `flyway`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param redisProjection 输入参数 `redisProjection`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param fence 输入参数 `fence`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param outbox 输入参数 `outbox`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param checkedAt 输入参数 `checkedAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public RuntimeStatus {
            ddcConfigClient = ddcConfigClient == null
                    ? DdcConfigClientStatus.unknown()
                    : ddcConfigClient;
        }

        /**
         * 构造器 `RuntimeStatus` 用于创建并初始化 `RuntimeStatus` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `RuntimeStatus` creates and initializes `RuntimeStatus`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `RuntimeStatus` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `RuntimeStatus`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param definition 输入参数 `definition`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param providerLease 输入参数 `providerLease`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param gatewayRelease 输入参数 `gatewayRelease`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param checkedAt 输入参数 `checkedAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public RuntimeStatus(
                DefinitionStatus definition,
                ProviderLeaseStatus providerLease,
                GatewayReleaseStatus gatewayRelease,
                Instant checkedAt) {
            this(DdcConfigClientStatus.unknown(), definition, providerLease,
                    gatewayRelease, checkedAt);
        }

        /**
         * 构造器 `RuntimeStatus` 用于创建并初始化 `RuntimeStatus` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `RuntimeStatus` creates and initializes `RuntimeStatus`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `RuntimeStatus` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `RuntimeStatus`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param ddcConfigClient 输入参数 `ddcConfigClient`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param definition 输入参数 `definition`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param providerLease 输入参数 `providerLease`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param gatewayRelease 输入参数 `gatewayRelease`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param checkedAt 输入参数 `checkedAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public RuntimeStatus(
                DdcConfigClientStatus ddcConfigClient,
                DefinitionStatus definition,
                ProviderLeaseStatus providerLease,
                GatewayReleaseStatus gatewayRelease,
                Instant checkedAt) {
            this(ddcConfigClient, definition, providerLease, gatewayRelease,
                    new FlywayStatus("UNKNOWN", "UNKNOWN"),
                    new RedisProjectionStatus("UNKNOWN", 0L),
                    new FenceMutationStatus("UNKNOWN", 0L, 0L, 0L),
                    new OutboxStatus("UNKNOWN", 0L, 0L),
                    checkedAt);
        }
    }

    /**
     * 类型 `DdcConfigClientStatus` 位于 `ControlPlaneRuntimeStatusPort` 内，是记录类型，用于承载 `Ddc Config Client Status` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `DdcConfigClientStatus` is a record inside `ControlPlaneRuntimeStatusPort` and carries the responsibility, state, or contract for `Ddc Config Client Status`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `DdcConfigClientStatus` 作为 `ControlPlaneRuntimeStatusPort` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `DdcConfigClientStatus` as the responsibility boundary of `ControlPlaneRuntimeStatusPort`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param state 记录组件 `state` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `state` carries constructor data whose meaning is defined by the record contract.
     * @param instanceId 记录组件 `instanceId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `instanceId` carries constructor data whose meaning is defined by the record contract.
     * @param leaseIdFingerprint 记录组件 `leaseIdFingerprint` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `leaseIdFingerprint` carries constructor data whose meaning is defined by the record contract.
     * @param leaseExpireAt 记录组件 `leaseExpireAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `leaseExpireAt` carries constructor data whose meaning is defined by the record contract.
     * @param configVersions 记录组件 `configVersions` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `configVersions` carries constructor data whose meaning is defined by the record contract.
     * @param lastApplyFailureKey 记录组件 `lastApplyFailureKey` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `lastApplyFailureKey` carries constructor data whose meaning is defined by the record contract.
     * @param lastApplyFailureVersion 记录组件 `lastApplyFailureVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `lastApplyFailureVersion` carries constructor data whose meaning is defined by the record contract.
     * @param lastApplyFailureCode 记录组件 `lastApplyFailureCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `lastApplyFailureCode` carries constructor data whose meaning is defined by the record contract.
     */
    record DdcConfigClientStatus(
            /**
             * 字段 `state` 表示 `DdcConfigClientStatus` 中与 `state` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `state` stores the `state`-related state, dependency, configuration, or result of `DdcConfigClientStatus` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `state` 时应保持 `DdcConfigClientStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `state`, preserve `DdcConfigClientStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            String state,
            /**
             * 字段 `instanceId` 表示 `DdcConfigClientStatus` 中与 `instance Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `instanceId` stores the `instance Id`-related state, dependency, configuration, or result of `DdcConfigClientStatus` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `instanceId` 时应保持 `DdcConfigClientStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `instanceId`, preserve `DdcConfigClientStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            String instanceId,
            /**
             * 字段 `leaseIdFingerprint` 表示 `DdcConfigClientStatus` 中与 `lease Id Fingerprint` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `leaseIdFingerprint` stores the `lease Id Fingerprint`-related state, dependency, configuration, or result of `DdcConfigClientStatus` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `leaseIdFingerprint` 时应保持 `DdcConfigClientStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `leaseIdFingerprint`, preserve `DdcConfigClientStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            String leaseIdFingerprint,
            /**
             * 字段 `leaseExpireAt` 表示 `DdcConfigClientStatus` 中与 `lease Expire At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `leaseExpireAt` stores the `lease Expire At`-related state, dependency, configuration, or result of `DdcConfigClientStatus` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `leaseExpireAt` 时应保持 `DdcConfigClientStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `leaseExpireAt`, preserve `DdcConfigClientStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant leaseExpireAt,
            /**
             * 字段 `configVersions` 表示 `DdcConfigClientStatus` 中与 `config Versions` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, Long&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `configVersions` stores the `config Versions`-related state, dependency, configuration, or result of `DdcConfigClientStatus` (declared type `Map&lt;String, Long&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `configVersions` 时应保持 `DdcConfigClientStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `configVersions`, preserve `DdcConfigClientStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            Map<String, Long> configVersions,
            /**
             * 字段 `lastApplyFailureKey` 表示 `DdcConfigClientStatus` 中与 `last Apply Failure Key` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `lastApplyFailureKey` stores the `last Apply Failure Key`-related state, dependency, configuration, or result of `DdcConfigClientStatus` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `lastApplyFailureKey` 时应保持 `DdcConfigClientStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `lastApplyFailureKey`, preserve `DdcConfigClientStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            String lastApplyFailureKey,
            /**
             * 字段 `lastApplyFailureVersion` 表示 `DdcConfigClientStatus` 中与 `last Apply Failure Version` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `lastApplyFailureVersion` stores the `last Apply Failure Version`-related state, dependency, configuration, or result of `DdcConfigClientStatus` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `lastApplyFailureVersion` 时应保持 `DdcConfigClientStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `lastApplyFailureVersion`, preserve `DdcConfigClientStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long lastApplyFailureVersion,
            /**
             * 字段 `lastApplyFailureCode` 表示 `DdcConfigClientStatus` 中与 `last Apply Failure Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `lastApplyFailureCode` stores the `last Apply Failure Code`-related state, dependency, configuration, or result of `DdcConfigClientStatus` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `lastApplyFailureCode` 时应保持 `DdcConfigClientStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `lastApplyFailureCode`, preserve `DdcConfigClientStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            String lastApplyFailureCode) {

        /**
         * 构造器 `DdcConfigClientStatus` 用于创建并初始化 `DdcConfigClientStatus` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `DdcConfigClientStatus` creates and initializes `DdcConfigClientStatus`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `DdcConfigClientStatus` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `DdcConfigClientStatus`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param state 输入参数 `state`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param instanceId 输入参数 `instanceId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param leaseIdFingerprint 输入参数 `leaseIdFingerprint`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param leaseExpireAt 输入参数 `leaseExpireAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param configVersions 输入参数 `configVersions`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param lastApplyFailureKey 输入参数 `lastApplyFailureKey`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param lastApplyFailureVersion 输入参数 `lastApplyFailureVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param lastApplyFailureCode 输入参数 `lastApplyFailureCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public DdcConfigClientStatus {
            configVersions = configVersions == null ? Map.of() : Map.copyOf(configVersions);
        }

        /**
         * 方法 `unknown` 按照 `DdcConfigClientStatus` 的职责处理输入，完成 `unknown` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `unknown` processes its inputs according to `DdcConfigClientStatus`'s responsibility, performs the `unknown` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `unknown` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `unknown`, then continue the business flow using its result, exception, or side effect.
         *
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        public static DdcConfigClientStatus unknown() {
            return new DdcConfigClientStatus(
                    "UNKNOWN", null, null, null, Map.of(), null, null, null);
        }
    }

    /**
     * 类型 `DefinitionStatus` 位于 `ControlPlaneRuntimeStatusPort` 内，是记录类型，用于承载 `Definition Status` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `DefinitionStatus` is a record inside `ControlPlaneRuntimeStatusPort` and carries the responsibility, state, or contract for `Definition Status`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `DefinitionStatus` 作为 `ControlPlaneRuntimeStatusPort` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `DefinitionStatus` as the responsibility boundary of `ControlPlaneRuntimeStatusPort`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     * @param definitionSetId 记录组件 `definitionSetId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `definitionSetId` carries constructor data whose meaning is defined by the record contract.
     * @param warnings 记录组件 `warnings` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `warnings` carries constructor data whose meaning is defined by the record contract.
     */
    record DefinitionStatus(
            /**
             * 字段 `status` 表示 `DefinitionStatus` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `DefinitionStatus` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `DefinitionStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `DefinitionStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            String status,
            /**
             * 字段 `definitionSetId` 表示 `DefinitionStatus` 中与 `definition Set Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `definitionSetId` stores the `definition Set Id`-related state, dependency, configuration, or result of `DefinitionStatus` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `definitionSetId` 时应保持 `DefinitionStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `definitionSetId`, preserve `DefinitionStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            String definitionSetId,
            /**
             * 字段 `warnings` 表示 `DefinitionStatus` 中与 `warnings` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `warnings` stores the `warnings`-related state, dependency, configuration, or result of `DefinitionStatus` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `warnings` 时应保持 `DefinitionStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `warnings`, preserve `DefinitionStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<String> warnings) {
        /**
         * 构造器 `DefinitionStatus` 用于创建并初始化 `DefinitionStatus` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `DefinitionStatus` creates and initializes `DefinitionStatus`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `DefinitionStatus` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `DefinitionStatus`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param definitionSetId 输入参数 `definitionSetId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param warnings 输入参数 `warnings`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public DefinitionStatus {
            warnings = List.copyOf(warnings);
        }
    }

    /**
     * 类型 `ProviderLeaseStatus` 位于 `ControlPlaneRuntimeStatusPort` 内，是记录类型，用于承载 `Provider Lease Status` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ProviderLeaseStatus` is a record inside `ControlPlaneRuntimeStatusPort` and carries the responsibility, state, or contract for `Provider Lease Status`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ProviderLeaseStatus` 作为 `ControlPlaneRuntimeStatusPort` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ProviderLeaseStatus` as the responsibility boundary of `ControlPlaneRuntimeStatusPort`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param state 记录组件 `state` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `state` carries constructor data whose meaning is defined by the record contract.
     * @param instanceId 记录组件 `instanceId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `instanceId` carries constructor data whose meaning is defined by the record contract.
     * @param leaseExpireAt 记录组件 `leaseExpireAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `leaseExpireAt` carries constructor data whose meaning is defined by the record contract.
     */
    record ProviderLeaseStatus(
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
            Instant leaseExpireAt) {
    }

    /**
     * 类型 `GatewayReleaseStatus` 位于 `ControlPlaneRuntimeStatusPort` 内，是记录类型，用于承载 `Gateway Release Status` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `GatewayReleaseStatus` is a record inside `ControlPlaneRuntimeStatusPort` and carries the responsibility, state, or contract for `Gateway Release Status`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `GatewayReleaseStatus` 作为 `ControlPlaneRuntimeStatusPort` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `GatewayReleaseStatus` as the responsibility boundary of `ControlPlaneRuntimeStatusPort`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param releaseId 记录组件 `releaseId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `releaseId` carries constructor data whose meaning is defined by the record contract.
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     * @param observedByEngineVersion 记录组件 `observedByEngineVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `observedByEngineVersion` carries constructor data whose meaning is defined by the record contract.
     */
    record GatewayReleaseStatus(
            /**
             * 字段 `releaseId` 表示 `GatewayReleaseStatus` 中与 `release Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `releaseId` stores the `release Id`-related state, dependency, configuration, or result of `GatewayReleaseStatus` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `releaseId` 时应保持 `GatewayReleaseStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `releaseId`, preserve `GatewayReleaseStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            String releaseId,
            /**
             * 字段 `status` 表示 `GatewayReleaseStatus` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `GatewayReleaseStatus` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `GatewayReleaseStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `GatewayReleaseStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            String status,
            /**
             * 字段 `observedByEngineVersion` 表示 `GatewayReleaseStatus` 中与 `observed By Engine Version` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `observedByEngineVersion` stores the `observed By Engine Version`-related state, dependency, configuration, or result of `GatewayReleaseStatus` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `observedByEngineVersion` 时应保持 `GatewayReleaseStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `observedByEngineVersion`, preserve `GatewayReleaseStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            String observedByEngineVersion) {
    }

    /**
     * 类型 `FlywayStatus` 位于 `ControlPlaneRuntimeStatusPort` 内，是记录类型，用于承载 `Flyway Status` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `FlywayStatus` is a record inside `ControlPlaneRuntimeStatusPort` and carries the responsibility, state, or contract for `Flyway Status`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `FlywayStatus` 作为 `ControlPlaneRuntimeStatusPort` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `FlywayStatus` as the responsibility boundary of `ControlPlaneRuntimeStatusPort`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param rbac3History 记录组件 `rbac3History` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `rbac3History` carries constructor data whose meaning is defined by the record contract.
     * @param outboxHistory 记录组件 `outboxHistory` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `outboxHistory` carries constructor data whose meaning is defined by the record contract.
     */
    record FlywayStatus(/**
 * 字段 `rbac3History` 表示 `FlywayStatus` 中与 `rbac3 History` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `rbac3History` stores the `rbac3 History`-related state, dependency, configuration, or result of `FlywayStatus` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `rbac3History` 时应保持 `FlywayStatus` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `rbac3History`, preserve `FlywayStatus`'s lifecycle, immutability, and thread-safety constraints.
 */ String rbac3History, /**
 * 字段 `outboxHistory` 表示 `FlywayStatus` 中与 `outbox History` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `outboxHistory` stores the `outbox History`-related state, dependency, configuration, or result of `FlywayStatus` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `outboxHistory` 时应保持 `FlywayStatus` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `outboxHistory`, preserve `FlywayStatus`'s lifecycle, immutability, and thread-safety constraints.
 */ String outboxHistory) {
    }

    /**
     * 类型 `RedisProjectionStatus` 位于 `ControlPlaneRuntimeStatusPort` 内，是记录类型，用于承载 `Redis Projection Status` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RedisProjectionStatus` is a record inside `ControlPlaneRuntimeStatusPort` and carries the responsibility, state, or contract for `Redis Projection Status`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RedisProjectionStatus` 作为 `ControlPlaneRuntimeStatusPort` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RedisProjectionStatus` as the responsibility boundary of `ControlPlaneRuntimeStatusPort`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param state 记录组件 `state` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `state` carries constructor data whose meaning is defined by the record contract.
     * @param checkpointLag 记录组件 `checkpointLag` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `checkpointLag` carries constructor data whose meaning is defined by the record contract.
     */
    record RedisProjectionStatus(/**
 * 字段 `state` 表示 `RedisProjectionStatus` 中与 `state` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `state` stores the `state`-related state, dependency, configuration, or result of `RedisProjectionStatus` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `state` 时应保持 `RedisProjectionStatus` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `state`, preserve `RedisProjectionStatus`'s lifecycle, immutability, and thread-safety constraints.
 */ String state, /**
 * 字段 `checkpointLag` 表示 `RedisProjectionStatus` 中与 `checkpoint Lag` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `checkpointLag` stores the `checkpoint Lag`-related state, dependency, configuration, or result of `RedisProjectionStatus` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `checkpointLag` 时应保持 `RedisProjectionStatus` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `checkpointLag`, preserve `RedisProjectionStatus`'s lifecycle, immutability, and thread-safety constraints.
 */ long checkpointLag) {
    }

    /**
     * 类型 `FenceMutationStatus` 位于 `ControlPlaneRuntimeStatusPort` 内，是记录类型，用于承载 `Fence Mutation Status` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `FenceMutationStatus` is a record inside `ControlPlaneRuntimeStatusPort` and carries the responsibility, state, or contract for `Fence Mutation Status`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `FenceMutationStatus` 作为 `ControlPlaneRuntimeStatusPort` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `FenceMutationStatus` as the responsibility boundary of `ControlPlaneRuntimeStatusPort`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param state 记录组件 `state` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `state` carries constructor data whose meaning is defined by the record contract.
     * @param pendingCount 记录组件 `pendingCount` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `pendingCount` carries constructor data whose meaning is defined by the record contract.
     * @param recoveryRequiredCount 记录组件 `recoveryRequiredCount` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `recoveryRequiredCount` carries constructor data whose meaning is defined by the record contract.
     * @param oldestAgeSeconds 记录组件 `oldestAgeSeconds` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `oldestAgeSeconds` carries constructor data whose meaning is defined by the record contract.
     */
    record FenceMutationStatus(
            /**
             * 字段 `state` 表示 `FenceMutationStatus` 中与 `state` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `state` stores the `state`-related state, dependency, configuration, or result of `FenceMutationStatus` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `state` 时应保持 `FenceMutationStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `state`, preserve `FenceMutationStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            String state,
            /**
             * 字段 `pendingCount` 表示 `FenceMutationStatus` 中与 `pending Count` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `pendingCount` stores the `pending Count`-related state, dependency, configuration, or result of `FenceMutationStatus` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `pendingCount` 时应保持 `FenceMutationStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `pendingCount`, preserve `FenceMutationStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            long pendingCount,
            /**
             * 字段 `recoveryRequiredCount` 表示 `FenceMutationStatus` 中与 `recovery Required Count` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `recoveryRequiredCount` stores the `recovery Required Count`-related state, dependency, configuration, or result of `FenceMutationStatus` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `recoveryRequiredCount` 时应保持 `FenceMutationStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `recoveryRequiredCount`, preserve `FenceMutationStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            long recoveryRequiredCount,
            /**
             * 字段 `oldestAgeSeconds` 表示 `FenceMutationStatus` 中与 `oldest Age Seconds` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `oldestAgeSeconds` stores the `oldest Age Seconds`-related state, dependency, configuration, or result of `FenceMutationStatus` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `oldestAgeSeconds` 时应保持 `FenceMutationStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `oldestAgeSeconds`, preserve `FenceMutationStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            long oldestAgeSeconds) {
    }

    /**
     * 类型 `OutboxStatus` 位于 `ControlPlaneRuntimeStatusPort` 内，是记录类型，用于承载 `Outbox Status` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `OutboxStatus` is a record inside `ControlPlaneRuntimeStatusPort` and carries the responsibility, state, or contract for `Outbox Status`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `OutboxStatus` 作为 `ControlPlaneRuntimeStatusPort` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `OutboxStatus` as the responsibility boundary of `ControlPlaneRuntimeStatusPort`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param state 记录组件 `state` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `state` carries constructor data whose meaning is defined by the record contract.
     * @param pendingCount 记录组件 `pendingCount` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `pendingCount` carries constructor data whose meaning is defined by the record contract.
     * @param oldestAgeSeconds 记录组件 `oldestAgeSeconds` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `oldestAgeSeconds` carries constructor data whose meaning is defined by the record contract.
     */
    record OutboxStatus(
            /**
             * 字段 `state` 表示 `OutboxStatus` 中与 `state` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `state` stores the `state`-related state, dependency, configuration, or result of `OutboxStatus` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `state` 时应保持 `OutboxStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `state`, preserve `OutboxStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            String state,
            /**
             * 字段 `pendingCount` 表示 `OutboxStatus` 中与 `pending Count` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `pendingCount` stores the `pending Count`-related state, dependency, configuration, or result of `OutboxStatus` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `pendingCount` 时应保持 `OutboxStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `pendingCount`, preserve `OutboxStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            long pendingCount,
            /**
             * 字段 `oldestAgeSeconds` 表示 `OutboxStatus` 中与 `oldest Age Seconds` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `oldestAgeSeconds` stores the `oldest Age Seconds`-related state, dependency, configuration, or result of `OutboxStatus` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `oldestAgeSeconds` 时应保持 `OutboxStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `oldestAgeSeconds`, preserve `OutboxStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            long oldestAgeSeconds) {
    }
}
