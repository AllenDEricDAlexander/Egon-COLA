package top.egon.cola.platform.rbac3.admin.simulation.application;

import top.egon.cola.platform.rbac3.admin.application.port.AuditPort;
import top.egon.cola.platform.rbac3.admin.authorization.application.AuthorizationDecisionService;
import top.egon.cola.platform.rbac3.admin.role.service.RoleFacade;
import top.egon.cola.platform.rbac3.admin.config.security.CurrentRbac3Principal;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import top.egon.cola.platform.rbac3.admin.role.domain.vo.RoleImpactVO;

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
     * 字段 `roleImpactSource` 表示 `AuthorizationSimulationService` 中与 `role Impact Source` 相关的状态、依赖、配置或结果（声明类型 `RoleImpactSource`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `roleImpactSource` stores the `role Impact Source`-related state, dependency, configuration, or result of `AuthorizationSimulationService` (declared type `RoleImpactSource`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `roleImpactSource` 时应保持 `AuthorizationSimulationService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `roleImpactSource`, preserve `AuthorizationSimulationService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RoleImpactSource roleImpactSource;
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
            RoleImpactSource roleImpactSource,
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
    public SimulationResult simulate(
            CurrentRbac3Principal caller,
            SimulationRequest request) {
        Objects.requireNonNull(caller, "caller");
        Objects.requireNonNull(request, "request");
        AuthorizationDecisionService.SnapshotRecord snapshot =
                decisionService.consistentSnapshot(caller, request.decisionRequest());
        var current = decisionService.evaluateConsistentSnapshot(
                snapshot, request.decisionRequest(), Set.of(), Set.of());
        var hypothetical = decisionService.evaluateConsistentSnapshot(
                snapshot, request.decisionRequest(),
                request.hypothesis().addedPermissions(),
                request.hypothesis().removedPermissions());
        Instant expiresAt = clock.instant().plusSeconds(RESULT_TTL_SECONDS);
        auditPort.append(new AuditPort.AuditEvent(
                caller.tenantId(), "AUTHORIZATION_SIMULATED", caller.userId(),
                "SESSION", request.decisionRequest().subject().sessionId(),
                request.requestId(), request.traceId(),
                Map.of(
                        "applicationCode",
                        request.decisionRequest().resource().applicationCode(),
                        "permissionCode", request.decisionRequest().permissionCode(),
                        "snapshotChecksum", snapshot.snapshot().checksum()),
                clock.instant()));
        return new SimulationResult(
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
    public RoleChangeImpactResult simulateRoleChangeImpact(
            CurrentRbac3Principal caller,
            RoleChangeImpactRequest request) {
        Objects.requireNonNull(caller, "caller");
        Objects.requireNonNull(request, "request");
        RoleImpactSnapshot snapshot = roleImpactSource.load(
                caller.tenantId(), request.roleId());
        Instant now = clock.instant();
        auditPort.append(new AuditPort.AuditEvent(
                caller.tenantId(), "ROLE_CHANGE_IMPACT_SIMULATED", caller.userId(),
                "ROLE", request.roleId(), request.requestId(), request.traceId(),
                Map.of(
                        "policyVersion", Long.toString(snapshot.policyVersion()),
                        "evidenceChecksum", snapshot.evidenceChecksum()),
                now));
        return new RoleChangeImpactResult(
                snapshot.impact(), snapshot.policyVersion(),
                snapshot.evidenceChecksum(), now.plusSeconds(RESULT_TTL_SECONDS));
    }

    /**
     * 类型 `SimulationRequest` 位于 `AuthorizationSimulationService` 内，是记录类型，用于承载 `Simulation Request` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `SimulationRequest` is a record inside `AuthorizationSimulationService` and carries the responsibility, state, or contract for `Simulation Request`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `SimulationRequest` 作为 `AuthorizationSimulationService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SimulationRequest` as the responsibility boundary of `AuthorizationSimulationService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param decisionRequest 记录组件 `decisionRequest` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `decisionRequest` carries constructor data whose meaning is defined by the record contract.
     * @param hypothesis 记录组件 `hypothesis` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `hypothesis` carries constructor data whose meaning is defined by the record contract.
     * @param at 记录组件 `at` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `at` carries constructor data whose meaning is defined by the record contract.
     * @param requestId 记录组件 `requestId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `requestId` carries constructor data whose meaning is defined by the record contract.
     * @param traceId 记录组件 `traceId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `traceId` carries constructor data whose meaning is defined by the record contract.
     */
    public record SimulationRequest(
            /**
             * 字段 `decisionRequest` 表示 `SimulationRequest` 中与 `decision Request` 相关的状态、依赖、配置或结果（声明类型 `AuthorizationDecisionService.DecisionRequest`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `decisionRequest` stores the `decision Request`-related state, dependency, configuration, or result of `SimulationRequest` (declared type `AuthorizationDecisionService.DecisionRequest`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `decisionRequest` 时应保持 `SimulationRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `decisionRequest`, preserve `SimulationRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            AuthorizationDecisionService.DecisionRequest decisionRequest,
            /**
             * 字段 `hypothesis` 表示 `SimulationRequest` 中与 `hypothesis` 相关的状态、依赖、配置或结果（声明类型 `Hypothesis`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `hypothesis` stores the `hypothesis`-related state, dependency, configuration, or result of `SimulationRequest` (declared type `Hypothesis`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `hypothesis` 时应保持 `SimulationRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `hypothesis`, preserve `SimulationRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            Hypothesis hypothesis,
            /**
             * 字段 `at` 表示 `SimulationRequest` 中与 `at` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `at` stores the `at`-related state, dependency, configuration, or result of `SimulationRequest` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `at` 时应保持 `SimulationRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `at`, preserve `SimulationRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant at,
            /**
             * 字段 `requestId` 表示 `SimulationRequest` 中与 `request Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `requestId` stores the `request Id`-related state, dependency, configuration, or result of `SimulationRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `requestId` 时应保持 `SimulationRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `requestId`, preserve `SimulationRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String requestId,
            /**
             * 字段 `traceId` 表示 `SimulationRequest` 中与 `trace Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `traceId` stores the `trace Id`-related state, dependency, configuration, or result of `SimulationRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `traceId` 时应保持 `SimulationRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `traceId`, preserve `SimulationRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String traceId) {
        /**
         * 构造器 `SimulationRequest` 用于创建并初始化 `SimulationRequest` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `SimulationRequest` creates and initializes `SimulationRequest`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `SimulationRequest` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `SimulationRequest`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param decisionRequest 输入参数 `decisionRequest`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param hypothesis 输入参数 `hypothesis`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param at 输入参数 `at`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param requestId 输入参数 `requestId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param traceId 输入参数 `traceId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public SimulationRequest {
            decisionRequest = Objects.requireNonNull(decisionRequest, "decisionRequest");
            hypothesis = Objects.requireNonNull(hypothesis, "hypothesis");
            at = Objects.requireNonNull(at, "at");
            requestId = required(requestId, "requestId");
            traceId = required(traceId, "traceId");
        }
    }

    /**
     * 类型 `Hypothesis` 位于 `AuthorizationSimulationService` 内，是记录类型，用于承载 `Hypothesis` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Hypothesis` is a record inside `AuthorizationSimulationService` and carries the responsibility, state, or contract for `Hypothesis`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Hypothesis` 作为 `AuthorizationSimulationService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Hypothesis` as the responsibility boundary of `AuthorizationSimulationService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param addedPermissions 记录组件 `addedPermissions` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `addedPermissions` carries constructor data whose meaning is defined by the record contract.
     * @param removedPermissions 记录组件 `removedPermissions` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `removedPermissions` carries constructor data whose meaning is defined by the record contract.
     */
    public record Hypothesis(
            /**
             * 字段 `addedPermissions` 表示 `Hypothesis` 中与 `added Permissions` 相关的状态、依赖、配置或结果（声明类型 `Set&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `addedPermissions` stores the `added Permissions`-related state, dependency, configuration, or result of `Hypothesis` (declared type `Set&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `addedPermissions` 时应保持 `Hypothesis` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `addedPermissions`, preserve `Hypothesis`'s lifecycle, immutability, and thread-safety constraints.
             */
            Set<String> addedPermissions,
            /**
             * 字段 `removedPermissions` 表示 `Hypothesis` 中与 `removed Permissions` 相关的状态、依赖、配置或结果（声明类型 `Set&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `removedPermissions` stores the `removed Permissions`-related state, dependency, configuration, or result of `Hypothesis` (declared type `Set&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `removedPermissions` 时应保持 `Hypothesis` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `removedPermissions`, preserve `Hypothesis`'s lifecycle, immutability, and thread-safety constraints.
             */
            Set<String> removedPermissions) {
        /**
         * 构造器 `Hypothesis` 用于创建并初始化 `Hypothesis` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `Hypothesis` creates and initializes `Hypothesis`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `Hypothesis` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `Hypothesis`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param addedPermissions 输入参数 `addedPermissions`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param removedPermissions 输入参数 `removedPermissions`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public Hypothesis {
            addedPermissions = Set.copyOf(addedPermissions);
            removedPermissions = Set.copyOf(removedPermissions);
        }
    }

    /**
     * 类型 `SimulationResult` 位于 `AuthorizationSimulationService` 内，是记录类型，用于承载 `Simulation Result` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `SimulationResult` is a record inside `AuthorizationSimulationService` and carries the responsibility, state, or contract for `Simulation Result`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `SimulationResult` 作为 `AuthorizationSimulationService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SimulationResult` as the responsibility boundary of `AuthorizationSimulationService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param current 记录组件 `current` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `current` carries constructor data whose meaning is defined by the record contract.
     * @param hypothetical 记录组件 `hypothetical` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `hypothetical` carries constructor data whose meaning is defined by the record contract.
     * @param authVersion 记录组件 `authVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authVersion` carries constructor data whose meaning is defined by the record contract.
     * @param sessionVersion 记录组件 `sessionVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionVersion` carries constructor data whose meaning is defined by the record contract.
     * @param policyVersion 记录组件 `policyVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyVersion` carries constructor data whose meaning is defined by the record contract.
     * @param snapshotChecksum 记录组件 `snapshotChecksum` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `snapshotChecksum` carries constructor data whose meaning is defined by the record contract.
     * @param expiresAt 记录组件 `expiresAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expiresAt` carries constructor data whose meaning is defined by the record contract.
     */
    public record SimulationResult(
            /**
             * 字段 `current` 表示 `SimulationResult` 中与 `current` 相关的状态、依赖、配置或结果（声明类型 `AuthorizationDecisionService.DecisionBundle`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `current` stores the `current`-related state, dependency, configuration, or result of `SimulationResult` (declared type `AuthorizationDecisionService.DecisionBundle`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `current` 时应保持 `SimulationResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `current`, preserve `SimulationResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            AuthorizationDecisionService.DecisionBundle current,
            /**
             * 字段 `hypothetical` 表示 `SimulationResult` 中与 `hypothetical` 相关的状态、依赖、配置或结果（声明类型 `AuthorizationDecisionService.DecisionBundle`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `hypothetical` stores the `hypothetical`-related state, dependency, configuration, or result of `SimulationResult` (declared type `AuthorizationDecisionService.DecisionBundle`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `hypothetical` 时应保持 `SimulationResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `hypothetical`, preserve `SimulationResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            AuthorizationDecisionService.DecisionBundle hypothetical,
            /**
             * 字段 `authVersion` 表示 `SimulationResult` 中与 `auth Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authVersion` stores the `auth Version`-related state, dependency, configuration, or result of `SimulationResult` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authVersion` 时应保持 `SimulationResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authVersion`, preserve `SimulationResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            long authVersion,
            /**
             * 字段 `sessionVersion` 表示 `SimulationResult` 中与 `session Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionVersion` stores the `session Version`-related state, dependency, configuration, or result of `SimulationResult` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionVersion` 时应保持 `SimulationResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionVersion`, preserve `SimulationResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            long sessionVersion,
            /**
             * 字段 `policyVersion` 表示 `SimulationResult` 中与 `policy Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `SimulationResult` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `SimulationResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `SimulationResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            long policyVersion,
            /**
             * 字段 `snapshotChecksum` 表示 `SimulationResult` 中与 `snapshot Checksum` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `snapshotChecksum` stores the `snapshot Checksum`-related state, dependency, configuration, or result of `SimulationResult` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `snapshotChecksum` 时应保持 `SimulationResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `snapshotChecksum`, preserve `SimulationResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            String snapshotChecksum,
            /**
             * 字段 `expiresAt` 表示 `SimulationResult` 中与 `expires At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expiresAt` stores the `expires At`-related state, dependency, configuration, or result of `SimulationResult` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expiresAt` 时应保持 `SimulationResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expiresAt`, preserve `SimulationResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant expiresAt) {
    }

    /**
     * 类型 `RoleChangeImpactRequest` 位于 `AuthorizationSimulationService` 内，是记录类型，用于承载 `Role Change Impact Request` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RoleChangeImpactRequest` is a record inside `AuthorizationSimulationService` and carries the responsibility, state, or contract for `Role Change Impact Request`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RoleChangeImpactRequest` 作为 `AuthorizationSimulationService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RoleChangeImpactRequest` as the responsibility boundary of `AuthorizationSimulationService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param roleId 记录组件 `roleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleId` carries constructor data whose meaning is defined by the record contract.
     * @param at 记录组件 `at` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `at` carries constructor data whose meaning is defined by the record contract.
     * @param requestId 记录组件 `requestId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `requestId` carries constructor data whose meaning is defined by the record contract.
     * @param traceId 记录组件 `traceId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `traceId` carries constructor data whose meaning is defined by the record contract.
     */
    public record RoleChangeImpactRequest(
            /**
             * 字段 `roleId` 表示 `RoleChangeImpactRequest` 中与 `role Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleId` stores the `role Id`-related state, dependency, configuration, or result of `RoleChangeImpactRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleId` 时应保持 `RoleChangeImpactRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleId`, preserve `RoleChangeImpactRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String roleId,
            /**
             * 字段 `at` 表示 `RoleChangeImpactRequest` 中与 `at` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `at` stores the `at`-related state, dependency, configuration, or result of `RoleChangeImpactRequest` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `at` 时应保持 `RoleChangeImpactRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `at`, preserve `RoleChangeImpactRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant at,
            /**
             * 字段 `requestId` 表示 `RoleChangeImpactRequest` 中与 `request Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `requestId` stores the `request Id`-related state, dependency, configuration, or result of `RoleChangeImpactRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `requestId` 时应保持 `RoleChangeImpactRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `requestId`, preserve `RoleChangeImpactRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String requestId,
            /**
             * 字段 `traceId` 表示 `RoleChangeImpactRequest` 中与 `trace Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `traceId` stores the `trace Id`-related state, dependency, configuration, or result of `RoleChangeImpactRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `traceId` 时应保持 `RoleChangeImpactRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `traceId`, preserve `RoleChangeImpactRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String traceId) {
        /**
         * 构造器 `RoleChangeImpactRequest` 用于创建并初始化 `RoleChangeImpactRequest` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `RoleChangeImpactRequest` creates and initializes `RoleChangeImpactRequest`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `RoleChangeImpactRequest` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `RoleChangeImpactRequest`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param at 输入参数 `at`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param requestId 输入参数 `requestId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param traceId 输入参数 `traceId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public RoleChangeImpactRequest {
            roleId = required(roleId, "roleId");
            at = Objects.requireNonNull(at, "at");
            requestId = required(requestId, "requestId");
            traceId = required(traceId, "traceId");
        }
    }

    /**
     * 类型 `RoleImpactSnapshot` 位于 `AuthorizationSimulationService` 内，是记录类型，用于承载 `Role Impact Snapshot` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RoleImpactSnapshot` is a record inside `AuthorizationSimulationService` and carries the responsibility, state, or contract for `Role Impact Snapshot`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RoleImpactSnapshot` 作为 `AuthorizationSimulationService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RoleImpactSnapshot` as the responsibility boundary of `AuthorizationSimulationService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param impact 记录组件 `impact` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `impact` carries constructor data whose meaning is defined by the record contract.
     * @param policyVersion 记录组件 `policyVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyVersion` carries constructor data whose meaning is defined by the record contract.
     * @param evidenceChecksum 记录组件 `evidenceChecksum` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `evidenceChecksum` carries constructor data whose meaning is defined by the record contract.
     */
    public record RoleImpactSnapshot(
            /**
             * 字段 `impact` 表示 `RoleImpactSnapshot` 中与 `impact` 相关的状态、依赖、配置或结果（声明类型 `RoleImpactVO`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `impact` stores the `impact`-related state, dependency, configuration, or result of `RoleImpactSnapshot` (declared type `RoleImpactVO`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `impact` 时应保持 `RoleImpactSnapshot` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `impact`, preserve `RoleImpactSnapshot`'s lifecycle, immutability, and thread-safety constraints.
             */
            RoleImpactVO impact,
            /**
             * 字段 `policyVersion` 表示 `RoleImpactSnapshot` 中与 `policy Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `RoleImpactSnapshot` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `RoleImpactSnapshot` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `RoleImpactSnapshot`'s lifecycle, immutability, and thread-safety constraints.
             */
            long policyVersion,
            /**
             * 字段 `evidenceChecksum` 表示 `RoleImpactSnapshot` 中与 `evidence Checksum` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `evidenceChecksum` stores the `evidence Checksum`-related state, dependency, configuration, or result of `RoleImpactSnapshot` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `evidenceChecksum` 时应保持 `RoleImpactSnapshot` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `evidenceChecksum`, preserve `RoleImpactSnapshot`'s lifecycle, immutability, and thread-safety constraints.
             */
            String evidenceChecksum) {
        /**
         * 构造器 `RoleImpactSnapshot` 用于创建并初始化 `RoleImpactSnapshot` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `RoleImpactSnapshot` creates and initializes `RoleImpactSnapshot`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `RoleImpactSnapshot` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `RoleImpactSnapshot`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param impact 输入参数 `impact`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param policyVersion 输入参数 `policyVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param evidenceChecksum 输入参数 `evidenceChecksum`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public RoleImpactSnapshot {
            impact = Objects.requireNonNull(impact, "impact");
            if (policyVersion < 0) {
                throw new IllegalArgumentException("policyVersion must not be negative");
            }
            evidenceChecksum = required(evidenceChecksum, "evidenceChecksum");
        }
    }

    /**
     * 类型 `RoleChangeImpactResult` 位于 `AuthorizationSimulationService` 内，是记录类型，用于承载 `Role Change Impact Result` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RoleChangeImpactResult` is a record inside `AuthorizationSimulationService` and carries the responsibility, state, or contract for `Role Change Impact Result`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RoleChangeImpactResult` 作为 `AuthorizationSimulationService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RoleChangeImpactResult` as the responsibility boundary of `AuthorizationSimulationService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param impact 记录组件 `impact` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `impact` carries constructor data whose meaning is defined by the record contract.
     * @param policyVersion 记录组件 `policyVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyVersion` carries constructor data whose meaning is defined by the record contract.
     * @param evidenceChecksum 记录组件 `evidenceChecksum` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `evidenceChecksum` carries constructor data whose meaning is defined by the record contract.
     * @param expiresAt 记录组件 `expiresAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expiresAt` carries constructor data whose meaning is defined by the record contract.
     */
    public record RoleChangeImpactResult(
            /**
             * 字段 `impact` 表示 `RoleChangeImpactResult` 中与 `impact` 相关的状态、依赖、配置或结果（声明类型 `RoleImpactVO`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `impact` stores the `impact`-related state, dependency, configuration, or result of `RoleChangeImpactResult` (declared type `RoleImpactVO`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `impact` 时应保持 `RoleChangeImpactResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `impact`, preserve `RoleChangeImpactResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            RoleImpactVO impact,
            /**
             * 字段 `policyVersion` 表示 `RoleChangeImpactResult` 中与 `policy Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `RoleChangeImpactResult` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `RoleChangeImpactResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `RoleChangeImpactResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            long policyVersion,
            /**
             * 字段 `evidenceChecksum` 表示 `RoleChangeImpactResult` 中与 `evidence Checksum` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `evidenceChecksum` stores the `evidence Checksum`-related state, dependency, configuration, or result of `RoleChangeImpactResult` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `evidenceChecksum` 时应保持 `RoleChangeImpactResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `evidenceChecksum`, preserve `RoleChangeImpactResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            String evidenceChecksum,
            /**
             * 字段 `expiresAt` 表示 `RoleChangeImpactResult` 中与 `expires At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expiresAt` stores the `expires At`-related state, dependency, configuration, or result of `RoleChangeImpactResult` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expiresAt` 时应保持 `RoleChangeImpactResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expiresAt`, preserve `RoleChangeImpactResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant expiresAt) {
    }

    /**
     * 类型 `RoleImpactSource` 位于 `AuthorizationSimulationService` 内，是接口，用于承载 `Role Impact Source` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RoleImpactSource` is an interface inside `AuthorizationSimulationService` and carries the responsibility, state, or contract for `Role Impact Source`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RoleImpactSource` 作为 `AuthorizationSimulationService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RoleImpactSource` as the responsibility boundary of `AuthorizationSimulationService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface RoleImpactSource {
        /**
         * 方法 `load` 按照 `RoleImpactSource` 的职责处理输入，完成 `load` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `load` processes its inputs according to `RoleImpactSource`'s responsibility, performs the `load` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `load` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `load`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        RoleImpactSnapshot load(String tenantId, String roleId);
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
