package top.egon.cola.platform.rbac3.admin.session.domain.vo;

import top.egon.cola.platform.rbac3.admin.audit.repository.AuditPort;
import top.egon.cola.platform.rbac3.admin.runtime.repository.AuthorizationEventPublisher;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.session.service.SessionSecurityEventRecorder;

/**
     * 类型 `TerminationVO` 位于 `SessionSecurityEventRecorder` 内，是记录类型，用于承载 `TerminationVO` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `TerminationVO` is a record inside `SessionSecurityEventRecorder` and carries the responsibility, state, or contract for `TerminationVO`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `TerminationVO` 作为 `SessionSecurityEventRecorder` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `TerminationVO` as the responsibility boundary of `SessionSecurityEventRecorder`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param userId 记录组件 `userId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `userId` carries constructor data whose meaning is defined by the record contract.
     * @param sessionId 记录组件 `sessionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionId` carries constructor data whose meaning is defined by the record contract.
     * @param sessionVersion 记录组件 `sessionVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionVersion` carries constructor data whose meaning is defined by the record contract.
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     * @param reason 记录组件 `reason` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `reason` carries constructor data whose meaning is defined by the record contract.
     * @param actorId 记录组件 `actorId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorId` carries constructor data whose meaning is defined by the record contract.
     * @param occurredAt 记录组件 `occurredAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `occurredAt` carries constructor data whose meaning is defined by the record contract.
     */
    public record TerminationVO(
            /**
             * 字段 `tenantId` 表示 `TerminationVO` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `TerminationVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `TerminationVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `TerminationVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `userId` 表示 `TerminationVO` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `TerminationVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `userId` 时应保持 `TerminationVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `userId`, preserve `TerminationVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String userId,
            /**
             * 字段 `sessionId` 表示 `TerminationVO` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `TerminationVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `TerminationVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `TerminationVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String sessionId,
            /**
             * 字段 `sessionVersion` 表示 `TerminationVO` 中与 `session Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionVersion` stores the `session Version`-related state, dependency, configuration, or result of `TerminationVO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionVersion` 时应保持 `TerminationVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionVersion`, preserve `TerminationVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            long sessionVersion,
            /**
             * 字段 `status` 表示 `TerminationVO` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `TerminationVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `TerminationVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `TerminationVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String status,
            /**
             * 字段 `reason` 表示 `TerminationVO` 中与 `reason` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `reason` stores the `reason`-related state, dependency, configuration, or result of `TerminationVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `reason` 时应保持 `TerminationVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `reason`, preserve `TerminationVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String reason,
            /**
             * 字段 `actorId` 表示 `TerminationVO` 中与 `actor Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorId` stores the `actor Id`-related state, dependency, configuration, or result of `TerminationVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorId` 时应保持 `TerminationVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorId`, preserve `TerminationVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorId,
            /**
             * 字段 `occurredAt` 表示 `TerminationVO` 中与 `occurred At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `occurredAt` stores the `occurred At`-related state, dependency, configuration, or result of `TerminationVO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `occurredAt` 时应保持 `TerminationVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `occurredAt`, preserve `TerminationVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant occurredAt
    ) {

        /**
         * 构造器 `TerminationVO` 用于创建并初始化 `TerminationVO` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `TerminationVO` creates and initializes `TerminationVO`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `TerminationVO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `TerminationVO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param sessionVersion 输入参数 `sessionVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param reason 输入参数 `reason`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param occurredAt 输入参数 `occurredAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public TerminationVO {
            tenantId = required(tenantId, "tenantId");
            userId = required(userId, "userId");
            sessionId = required(sessionId, "sessionId");
            status = required(status, "status");
            reason = required(reason, "reason");
            actorId = required(actorId, "actorId");
            Objects.requireNonNull(occurredAt, "occurredAt");
            if (sessionVersion < 1) {
                throw new IllegalArgumentException("sessionVersion must be positive");
            }
        }

        /**
         * 方法 `required` 按照 `TerminationVO` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `required` processes its inputs according to `TerminationVO`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
