package top.egon.cola.platform.rbac3.admin.session.application;

import top.egon.cola.platform.rbac3.admin.application.port.AuditPort;
import top.egon.cola.platform.rbac3.admin.application.port.AuthorizationEventPort;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * 类型 `SessionSecurityEventRecorder` 位于当前包内，是类型，用于承载 `Session Security Event Recorder` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `SessionSecurityEventRecorder` is a type in its package and carries the responsibility, state, or contract for `Session Security Event Recorder`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Records one append-only audit entry and one reliable runtime event for a terminal session.
 */
public final class SessionSecurityEventRecorder {

    /**
     * 字段 `auditPort` 表示 `SessionSecurityEventRecorder` 中与 `audit Port` 相关的状态、依赖、配置或结果（声明类型 `AuditPort`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `auditPort` stores the `audit Port`-related state, dependency, configuration, or result of `SessionSecurityEventRecorder` (declared type `AuditPort`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `auditPort` 时应保持 `SessionSecurityEventRecorder` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `auditPort`, preserve `SessionSecurityEventRecorder`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AuditPort auditPort;
    /**
     * 字段 `eventPort` 表示 `SessionSecurityEventRecorder` 中与 `event Port` 相关的状态、依赖、配置或结果（声明类型 `AuthorizationEventPort`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `eventPort` stores the `event Port`-related state, dependency, configuration, or result of `SessionSecurityEventRecorder` (declared type `AuthorizationEventPort`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `eventPort` 时应保持 `SessionSecurityEventRecorder` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `eventPort`, preserve `SessionSecurityEventRecorder`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AuthorizationEventPort eventPort;

    /**
     * 构造器 `SessionSecurityEventRecorder` 用于创建并初始化 `SessionSecurityEventRecorder` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `SessionSecurityEventRecorder` creates and initializes `SessionSecurityEventRecorder`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `SessionSecurityEventRecorder` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `SessionSecurityEventRecorder`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param auditPort 输入参数 `auditPort`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param eventPort 输入参数 `eventPort`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public SessionSecurityEventRecorder(
            AuditPort auditPort,
            AuthorizationEventPort eventPort) {
        this.auditPort = Objects.requireNonNull(auditPort, "auditPort");
        this.eventPort = Objects.requireNonNull(eventPort, "eventPort");
    }

    /**
     * 方法 `record` 按照 `SessionSecurityEventRecorder` 的职责处理输入，完成 `record` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `record` processes its inputs according to `SessionSecurityEventRecorder`'s responsibility, performs the `record` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `record` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `record`, then continue the business flow using its result, exception, or side effect.
     *
     * @param termination 输入参数 `termination`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void record(Termination termination) {
        Objects.requireNonNull(termination, "termination");
        String correlationId = "session:" + termination.sessionId()
                + ':' + termination.sessionVersion();
        Map<String, String> evidence = Map.of(
                "userId", termination.userId(),
                "status", termination.status(),
                "reason", termination.reason(),
                "sessionVersion", Long.toString(termination.sessionVersion()));
        boolean replayDetected = "REFRESH_TOKEN_REUSED".equals(termination.reason());
        auditPort.append(new AuditPort.AuditEvent(
                termination.tenantId(), auditEventType(termination), termination.actorId(),
                "SESSION", termination.sessionId(), correlationId, correlationId,
                evidence, termination.occurredAt(),
                replayDetected ? "DENIED" : "SUCCESS",
                replayDetected ? "CRITICAL" : "INFO",
                termination.reason()));
        eventPort.enqueue(new AuthorizationEventPort.AuthorizationEvent(
                termination.tenantId(), "SESSION", termination.sessionId(),
                "SESSION_REVOKED", evidence, correlationId));
    }

    /**
     * 方法 `auditEventType` 按照 `SessionSecurityEventRecorder` 的职责处理输入，完成 `audit Event Type` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `auditEventType` processes its inputs according to `SessionSecurityEventRecorder`'s responsibility, performs the `audit Event Type` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `auditEventType` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `auditEventType`, then continue the business flow using its result, exception, or side effect.
     *
     * @param termination 输入参数 `termination`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private String auditEventType(Termination termination) {
        if ("REFRESH_TOKEN_REUSED".equals(termination.reason())) {
            return "REFRESH_TOKEN_REPLAY_DETECTED";
        }
        if ("LOGGED_OUT".equals(termination.status())) {
            return "SESSION_LOGGED_OUT";
        }
        return "SESSION_REVOKED";
    }

    /**
     * 类型 `Termination` 位于 `SessionSecurityEventRecorder` 内，是记录类型，用于承载 `Termination` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Termination` is a record inside `SessionSecurityEventRecorder` and carries the responsibility, state, or contract for `Termination`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Termination` 作为 `SessionSecurityEventRecorder` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Termination` as the responsibility boundary of `SessionSecurityEventRecorder`, following its existing construction, interface, or Spring-assembly mechanism.
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
    public record Termination(
            /**
             * 字段 `tenantId` 表示 `Termination` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `Termination` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `Termination` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `Termination`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `userId` 表示 `Termination` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `Termination` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `userId` 时应保持 `Termination` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `userId`, preserve `Termination`'s lifecycle, immutability, and thread-safety constraints.
             */
            String userId,
            /**
             * 字段 `sessionId` 表示 `Termination` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `Termination` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `Termination` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `Termination`'s lifecycle, immutability, and thread-safety constraints.
             */
            String sessionId,
            /**
             * 字段 `sessionVersion` 表示 `Termination` 中与 `session Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionVersion` stores the `session Version`-related state, dependency, configuration, or result of `Termination` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionVersion` 时应保持 `Termination` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionVersion`, preserve `Termination`'s lifecycle, immutability, and thread-safety constraints.
             */
            long sessionVersion,
            /**
             * 字段 `status` 表示 `Termination` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `Termination` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `Termination` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `Termination`'s lifecycle, immutability, and thread-safety constraints.
             */
            String status,
            /**
             * 字段 `reason` 表示 `Termination` 中与 `reason` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `reason` stores the `reason`-related state, dependency, configuration, or result of `Termination` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `reason` 时应保持 `Termination` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `reason`, preserve `Termination`'s lifecycle, immutability, and thread-safety constraints.
             */
            String reason,
            /**
             * 字段 `actorId` 表示 `Termination` 中与 `actor Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorId` stores the `actor Id`-related state, dependency, configuration, or result of `Termination` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorId` 时应保持 `Termination` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorId`, preserve `Termination`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorId,
            /**
             * 字段 `occurredAt` 表示 `Termination` 中与 `occurred At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `occurredAt` stores the `occurred At`-related state, dependency, configuration, or result of `Termination` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `occurredAt` 时应保持 `Termination` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `occurredAt`, preserve `Termination`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant occurredAt
    ) {

        /**
         * 构造器 `Termination` 用于创建并初始化 `Termination` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `Termination` creates and initializes `Termination`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `Termination` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `Termination`'s constructor entry point and do not bypass the validation and initialization constraints established there.
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
        public Termination {
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
         * 方法 `required` 按照 `Termination` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `required` processes its inputs according to `Termination`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
