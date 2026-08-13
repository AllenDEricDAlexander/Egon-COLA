package top.egon.cola.platform.rbac3.admin.session.service;

import top.egon.cola.platform.rbac3.admin.audit.repository.AuditPort;
import top.egon.cola.platform.rbac3.admin.runtime.repository.AuthorizationEventPublisher;
import top.egon.cola.platform.rbac3.admin.session.repository.SessionSecurityEventPort;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.session.domain.vo.TerminationVO;
import top.egon.cola.platform.rbac3.admin.audit.domain.vo.AuditEventVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.AuthorizationEventVO;

/**
 * 类型 `SessionSecurityEventRecorder` 位于当前包内，是类型，用于承载 `Session Security Event Recorder` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `SessionSecurityEventRecorder` is a type in its package and carries the responsibility, state, or contract for `Session Security Event Recorder`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Records one append-only audit entry and one reliable runtime event for a terminal session.
 */
public final class SessionSecurityEventRecorder implements SessionSecurityEventPort {

    /**
     * 字段 `auditPort` 表示 `SessionSecurityEventRecorder` 中与 `audit Port` 相关的状态、依赖、配置或结果（声明类型 `AuditPort`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `auditPort` stores the `audit Port`-related state, dependency, configuration, or result of `SessionSecurityEventRecorder` (declared type `AuditPort`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `auditPort` 时应保持 `SessionSecurityEventRecorder` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `auditPort`, preserve `SessionSecurityEventRecorder`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AuditPort auditPort;
    /**
     * 字段 `eventPort` 表示 `SessionSecurityEventRecorder` 中与 `event Port` 相关的状态、依赖、配置或结果（声明类型 `AuthorizationEventPublisher`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `eventPort` stores the `event Port`-related state, dependency, configuration, or result of `SessionSecurityEventRecorder` (declared type `AuthorizationEventPublisher`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `eventPort` 时应保持 `SessionSecurityEventRecorder` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `eventPort`, preserve `SessionSecurityEventRecorder`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AuthorizationEventPublisher eventPort;

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
            AuthorizationEventPublisher eventPort) {
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
    @Override
    public void record(TerminationVO termination) {
        Objects.requireNonNull(termination, "termination");
        String correlationId = "session:" + termination.sessionId()
                + ':' + termination.sessionVersion();
        Map<String, String> evidence = Map.of(
                "userId", termination.userId(),
                "status", termination.status(),
                "reason", termination.reason(),
                "sessionVersion", Long.toString(termination.sessionVersion()));
        boolean replayDetected = "REFRESH_TOKEN_REUSED".equals(termination.reason());
        auditPort.append(new AuditEventVO(
                termination.tenantId(), auditEventType(termination), termination.actorId(),
                "SESSION", termination.sessionId(), correlationId, correlationId,
                evidence, termination.occurredAt(),
                replayDetected ? "DENIED" : "SUCCESS",
                replayDetected ? "CRITICAL" : "INFO",
                termination.reason()));
        eventPort.enqueue(new AuthorizationEventVO(
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
    private String auditEventType(TerminationVO termination) {
        if ("REFRESH_TOKEN_REUSED".equals(termination.reason())) {
            return "REFRESH_TOKEN_REPLAY_DETECTED";
        }
        if ("LOGGED_OUT".equals(termination.status())) {
            return "SESSION_LOGGED_OUT";
        }
        return "SESSION_REVOKED";
    }

    }
