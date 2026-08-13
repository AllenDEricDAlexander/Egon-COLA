package top.egon.cola.platform.rbac3.admin.session.domain.vo;

import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.runtime.repository.Rbac3RuntimePolicy;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.session.domain.enums.SessionLifecycleStatusEnum;
import top.egon.cola.platform.rbac3.admin.session.service.SessionFacade;

/**
     * 类型 `SessionRecordVO` 位于 `SessionFacade` 内，是记录类型，用于承载 `Session Record` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `SessionRecordVO` is a record inside `SessionFacade` and carries the responsibility, state, or contract for `Session Record`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `SessionRecordVO` 作为 `SessionFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SessionRecordVO` as the responsibility boundary of `SessionFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param entityId 记录组件 `entityId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `entityId` carries constructor data whose meaning is defined by the record contract.
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param userId 记录组件 `userId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `userId` carries constructor data whose meaning is defined by the record contract.
     * @param sessionId 记录组件 `sessionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionId` carries constructor data whose meaning is defined by the record contract.
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     * @param sessionVersion 记录组件 `sessionVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionVersion` carries constructor data whose meaning is defined by the record contract.
     * @param authVersion 记录组件 `authVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authVersion` carries constructor data whose meaning is defined by the record contract.
     * @param policyVersion 记录组件 `policyVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyVersion` carries constructor data whose meaning is defined by the record contract.
     * @param activationRequired 记录组件 `activationRequired` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `activationRequired` carries constructor data whose meaning is defined by the record contract.
     * @param tokenFamilyId 记录组件 `tokenFamilyId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tokenFamilyId` carries constructor data whose meaning is defined by the record contract.
     * @param deviceIdHash 记录组件 `deviceIdHash` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `deviceIdHash` carries constructor data whose meaning is defined by the record contract.
     * @param authenticatedAt 记录组件 `authenticatedAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authenticatedAt` carries constructor data whose meaning is defined by the record contract.
     * @param idleExpiresAt 记录组件 `idleExpiresAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `idleExpiresAt` carries constructor data whose meaning is defined by the record contract.
     * @param absoluteExpiresAt 记录组件 `absoluteExpiresAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `absoluteExpiresAt` carries constructor data whose meaning is defined by the record contract.
     */
    public record SessionRecordVO(
            /**
             * 字段 `entityId` 表示 `SessionRecordVO` 中与 `entity Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `entityId` stores the `entity Id`-related state, dependency, configuration, or result of `SessionRecordVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `entityId` 时应保持 `SessionRecordVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `entityId`, preserve `SessionRecordVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String entityId,
            /**
             * 字段 `tenantId` 表示 `SessionRecordVO` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `SessionRecordVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `SessionRecordVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `SessionRecordVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `userId` 表示 `SessionRecordVO` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `SessionRecordVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `userId` 时应保持 `SessionRecordVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `userId`, preserve `SessionRecordVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String userId,
            /**
             * 字段 `sessionId` 表示 `SessionRecordVO` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `SessionRecordVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `SessionRecordVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `SessionRecordVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String sessionId,
            /**
             * 字段 `status` 表示 `SessionRecordVO` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `SessionLifecycleStatusEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `SessionRecordVO` (declared type `SessionLifecycleStatusEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `SessionRecordVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `SessionRecordVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            SessionLifecycleStatusEnum status,
            /**
             * 字段 `sessionVersion` 表示 `SessionRecordVO` 中与 `session Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionVersion` stores the `session Version`-related state, dependency, configuration, or result of `SessionRecordVO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionVersion` 时应保持 `SessionRecordVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionVersion`, preserve `SessionRecordVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            long sessionVersion,
            /**
             * 字段 `authVersion` 表示 `SessionRecordVO` 中与 `auth Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authVersion` stores the `auth Version`-related state, dependency, configuration, or result of `SessionRecordVO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authVersion` 时应保持 `SessionRecordVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authVersion`, preserve `SessionRecordVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            long authVersion,
            /**
             * 字段 `policyVersion` 表示 `SessionRecordVO` 中与 `policy Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `SessionRecordVO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `SessionRecordVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `SessionRecordVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            long policyVersion,
            /**
             * 字段 `activationRequired` 表示 `SessionRecordVO` 中与 `activation Required` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `activationRequired` stores the `activation Required`-related state, dependency, configuration, or result of `SessionRecordVO` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `activationRequired` 时应保持 `SessionRecordVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `activationRequired`, preserve `SessionRecordVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean activationRequired,
            /**
             * 字段 `tokenFamilyId` 表示 `SessionRecordVO` 中与 `token Family Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tokenFamilyId` stores the `token Family Id`-related state, dependency, configuration, or result of `SessionRecordVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tokenFamilyId` 时应保持 `SessionRecordVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tokenFamilyId`, preserve `SessionRecordVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tokenFamilyId,
            /**
             * 字段 `deviceIdHash` 表示 `SessionRecordVO` 中与 `device Id Hash` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `deviceIdHash` stores the `device Id Hash`-related state, dependency, configuration, or result of `SessionRecordVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `deviceIdHash` 时应保持 `SessionRecordVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `deviceIdHash`, preserve `SessionRecordVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String deviceIdHash,
            /**
             * 字段 `authenticatedAt` 表示 `SessionRecordVO` 中与 `authenticated At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authenticatedAt` stores the `authenticated At`-related state, dependency, configuration, or result of `SessionRecordVO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authenticatedAt` 时应保持 `SessionRecordVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authenticatedAt`, preserve `SessionRecordVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant authenticatedAt,
            /**
             * 字段 `idleExpiresAt` 表示 `SessionRecordVO` 中与 `idle Expires At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `idleExpiresAt` stores the `idle Expires At`-related state, dependency, configuration, or result of `SessionRecordVO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `idleExpiresAt` 时应保持 `SessionRecordVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `idleExpiresAt`, preserve `SessionRecordVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant idleExpiresAt,
            /**
             * 字段 `absoluteExpiresAt` 表示 `SessionRecordVO` 中与 `absolute Expires At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `absoluteExpiresAt` stores the `absolute Expires At`-related state, dependency, configuration, or result of `SessionRecordVO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `absoluteExpiresAt` 时应保持 `SessionRecordVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `absoluteExpiresAt`, preserve `SessionRecordVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant absoluteExpiresAt
    ) {
    }
