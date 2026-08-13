package top.egon.cola.platform.rbac3.admin.simulation.service;

import top.egon.cola.platform.rbac3.admin.audit.repository.AuditPort;
import top.egon.cola.platform.rbac3.admin.authorization.service.AuthorizationDecisionService;
import top.egon.cola.platform.rbac3.admin.config.security.CurrentRbac3Principal;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import top.egon.cola.platform.rbac3.admin.simulation.domain.dto.SimulationRequestDTO;
import top.egon.cola.platform.rbac3.admin.simulation.domain.vo.SimulationResultVO;
import top.egon.cola.platform.rbac3.admin.simulation.domain.dto.RoleChangeImpactRequestDTO;
import top.egon.cola.platform.rbac3.admin.simulation.domain.vo.RoleImpactSnapshotVO;
import top.egon.cola.platform.rbac3.admin.simulation.domain.vo.RoleChangeImpactResultVO;
import top.egon.cola.platform.rbac3.admin.simulation.repository.RoleImpactRepository;
import top.egon.cola.platform.rbac3.admin.audit.domain.vo.AuditEventVO;
import top.egon.cola.platform.rbac3.admin.authorization.domain.vo.SnapshotRecordVO;

/**
 * 类型 `AuthorizationSimulationService` 位于当前包内，是类型，用于承载 `Authorization Simulation Service` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `AuthorizationSimulationService` is a type in its package and carries the responsibility, state, or contract for `Authorization Simulation Service`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Read-only authorization what-if analysis over one consistent snapshot.
 */
public final class AuthorizationSimulationService {

    /**
     * 字段 `RESULT_TTL_SECONDS` 表示 `AuthorizationSimulationService` 中与 `RESULT TTL SECONDS` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `RESULT_TTL_SECONDS` stores the `RESULT TTL SECONDS`-related state, dependency, configuration, or result of `AuthorizationSimulationService` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `RESULT_TTL_SECONDS` 时应保持 `AuthorizationSimulationService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `RESULT_TTL_SECONDS`, preserve `AuthorizationSimulationService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final long RESULT_TTL_SECONDS = 300;

    /**
     * 字段 `decisionService` 表示 `AuthorizationSimulationService` 中与 `decision Service` 相关的状态、依赖、配置或结果（声明类型 `AuthorizationDecisionService`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `decisionService` stores the `decision Service`-related state, dependency, configuration, or result of `AuthorizationSimulationService` (declared type `AuthorizationDecisionService`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `decisionService` 时应保持 `AuthorizationSimulationService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `decisionService`, preserve `AuthorizationSimulationService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AuthorizationDecisionService decisionService;
    /**
     * 字段 `roleImpactSource` 表示 `AuthorizationSimulationService` 中与 `role Impact Source` 相关的状态、依赖、配置或结果（声明类型 `RoleImpactRepository`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `roleImpactSource` stores the `role Impact Source`-related state, dependency, configuration, or result of `AuthorizationSimulationService` (declared type `RoleImpactRepository`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `roleImpactSource` 时应保持 `AuthorizationSimulationService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `roleImpactSource`, preserve `AuthorizationSimulationService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RoleImpactRepository roleImpactSource;
    /**
     * 字段 `auditPort` 表示 `AuthorizationSimulationService` 中与 `audit Port` 相关的状态、依赖、配置或结果（声明类型 `AuditPort`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `auditPort` stores the `audit Port`-related state, dependency, configuration, or result of `AuthorizationSimulationService` (declared type `AuditPort`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `auditPort` 时应保持 `AuthorizationSimulationService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `auditPort`, preserve `AuthorizationSimulationService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AuditPort auditPort;
    /**
     * 字段 `clock` 表示 `AuthorizationSimulationService` 中与 `clock` 相关的状态、依赖、配置或结果（声明类型 `Clock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `clock` stores the `clock`-related state, dependency, configuration, or result of `AuthorizationSimulationService` (declared type `Clock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `clock` 时应保持 `AuthorizationSimulationService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `clock`, preserve `AuthorizationSimulationService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Clock clock;

    /**
     * 构造器 `AuthorizationSimulationService` 用于创建并初始化 `AuthorizationSimulationService` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `AuthorizationSimulationService` creates and initializes `AuthorizationSimulationService`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `AuthorizationSimulationService` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `AuthorizationSimulationService`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param decisionService 输入参数 `decisionService`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param roleImpactSource 输入参数 `roleImpactSource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param auditPort 输入参数 `auditPort`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public AuthorizationSimulationService(
            AuthorizationDecisionService decisionService,
            RoleImpactRepository roleImpactSource,
            AuditPort auditPort,
            Clock clock) {
        this.decisionService = Objects.requireNonNull(decisionService, "decisionService");
        this.roleImpactSource = Objects.requireNonNull(roleImpactSource, "roleImpactSource");
        this.auditPort = Objects.requireNonNull(auditPort, "auditPort");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * 方法 `simulate` 按照 `AuthorizationSimulationService` 的职责处理输入，完成 `simulate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `simulate` processes its inputs according to `AuthorizationSimulationService`'s responsibility, performs the `simulate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `simulate` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `simulate`, then continue the business flow using its result, exception, or side effect.
     *
     * @param caller 输入参数 `caller`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public SimulationResultVO simulate(
            CurrentRbac3Principal caller,
            SimulationRequestDTO request) {
        Objects.requireNonNull(caller, "caller");
        Objects.requireNonNull(request, "request");
        SnapshotRecordVO snapshot =
                decisionService.consistentSnapshot(caller, request.decisionRequest());
        var current = decisionService.evaluateConsistentSnapshot(
                snapshot, request.decisionRequest(), Set.of(), Set.of());
        var hypothetical = decisionService.evaluateConsistentSnapshot(
                snapshot, request.decisionRequest(),
                request.hypothesis().addedPermissions(),
                request.hypothesis().removedPermissions());
        Instant expiresAt = clock.instant().plusSeconds(RESULT_TTL_SECONDS);
        auditPort.append(new AuditEventVO(
                caller.tenantId(), "AUTHORIZATION_SIMULATED", caller.userId(),
                "SESSION", request.decisionRequest().subject().sessionId(),
                request.requestId(), request.traceId(),
                Map.of(
                        "applicationCode",
                        request.decisionRequest().resource().applicationCode(),
                        "permissionCode", request.decisionRequest().permissionCode(),
                        "snapshotChecksum", snapshot.snapshot().checksum()),
                clock.instant()));
        return new SimulationResultVO(
                current, hypothetical, snapshot.snapshot().authVersion(),
                snapshot.snapshot().sessionVersion(), snapshot.snapshot().policyVersion(),
                snapshot.snapshot().checksum(), expiresAt);
    }

    /**
     * 方法 `simulateRoleChangeImpact` 按照 `AuthorizationSimulationService` 的职责处理输入，完成 `simulate Role Change Impact` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `simulateRoleChangeImpact` processes its inputs according to `AuthorizationSimulationService`'s responsibility, performs the `simulate Role Change Impact` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `simulateRoleChangeImpact` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `simulateRoleChangeImpact`, then continue the business flow using its result, exception, or side effect.
     *
     * @param caller 输入参数 `caller`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public RoleChangeImpactResultVO simulateRoleChangeImpact(
            CurrentRbac3Principal caller,
            RoleChangeImpactRequestDTO request) {
        Objects.requireNonNull(caller, "caller");
        Objects.requireNonNull(request, "request");
        RoleImpactSnapshotVO snapshot = roleImpactSource.load(
                caller.tenantId(), request.roleId());
        Instant now = clock.instant();
        auditPort.append(new AuditEventVO(
                caller.tenantId(), "ROLE_CHANGE_IMPACT_SIMULATED", caller.userId(),
                "ROLE", request.roleId(), request.requestId(), request.traceId(),
                Map.of(
                        "policyVersion", Long.toString(snapshot.policyVersion()),
                        "evidenceChecksum", snapshot.evidenceChecksum()),
                now));
        return new RoleChangeImpactResultVO(
                snapshot.impact(), snapshot.policyVersion(),
                snapshot.evidenceChecksum(), now.plusSeconds(RESULT_TTL_SECONDS));
    }








    /**
     * 方法 `required` 按照 `AuthorizationSimulationService` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `AuthorizationSimulationService`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `required` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `required`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param fieldName 输入参数 `fieldName`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
