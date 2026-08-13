package top.egon.cola.platform.rbac3.admin.activation.service;

import top.egon.cola.platform.rbac3.admin.application.port.Rbac3RuntimePolicy;
import top.egon.cola.platform.rbac3.admin.snapshot.application.SessionSnapshotProjector;
import top.egon.cola.platform.rbac3.contract.activation.ActiveRoleSetView;
import top.egon.cola.platform.rbac3.contract.activation.ReplaceActiveRolesResult;
import top.egon.cola.platform.rbac3.core.activation.DefaultRoleActivationResolver;
import top.egon.cola.platform.rbac3.core.activation.RoleActivationInput;
import top.egon.cola.platform.rbac3.core.activation.RoleActivationResolution;
import top.egon.cola.platform.rbac3.core.activation.RoleActivationResolver;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import top.egon.cola.platform.rbac3.admin.activation.repository.ActivationTransaction;
import top.egon.cola.platform.rbac3.admin.activation.repository.RoleActivationRuntimeRepository;
import top.egon.cola.platform.rbac3.admin.activation.domain.dto.ReplaceCommandDTO;
import top.egon.cola.platform.rbac3.admin.activation.domain.vo.SessionStateVO;
import top.egon.cola.platform.rbac3.admin.activation.domain.vo.ResolvedActivationVO;
import top.egon.cola.platform.rbac3.admin.activation.domain.vo.TransactionResultVO;
import top.egon.cola.platform.rbac3.admin.activation.domain.vo.CurrentStateVO;
import top.egon.cola.platform.rbac3.admin.activation.domain.vo.RuntimePublicationVO;
import top.egon.cola.platform.rbac3.admin.activation.repository.RoleActivationFactRepository;
import top.egon.cola.platform.rbac3.admin.activation.domain.vo.ActivationFactsVO;
import top.egon.cola.platform.rbac3.admin.activation.domain.vo.ApplicationFactVO;

/**
 * 类型 `RoleActivationFacade` 位于当前包内，是类型，用于承载 `Role Activation Facade` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `RoleActivationFacade` is a type in its package and carries the responsibility, state, or contract for `Role Activation Facade`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Replaces the current session's canonical activation roots and publishes one snapshot.
 */
public final class RoleActivationFacade {

    /**
     * 字段 `FENCE_TTL` 表示 `RoleActivationFacade` 中与 `FENCE TTL` 相关的状态、依赖、配置或结果（声明类型 `Duration`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `FENCE_TTL` stores the `FENCE TTL`-related state, dependency, configuration, or result of `RoleActivationFacade` (declared type `Duration`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `FENCE_TTL` 时应保持 `RoleActivationFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `FENCE_TTL`, preserve `RoleActivationFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final Duration FENCE_TTL = Duration.ofMinutes(5);
    /**
     * 字段 `STRONG_AUTHENTICATION_MAX_AGE` 表示 `RoleActivationFacade` 中与 `STRONG AUTHENTICATION MAX AGE` 相关的状态、依赖、配置或结果（声明类型 `Duration`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `STRONG_AUTHENTICATION_MAX_AGE` stores the `STRONG AUTHENTICATION MAX AGE`-related state, dependency, configuration, or result of `RoleActivationFacade` (declared type `Duration`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `STRONG_AUTHENTICATION_MAX_AGE` 时应保持 `RoleActivationFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `STRONG_AUTHENTICATION_MAX_AGE`, preserve `RoleActivationFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final Duration STRONG_AUTHENTICATION_MAX_AGE = Duration.ofMinutes(10);

    /**
     * 字段 `factSource` 表示 `RoleActivationFacade` 中与 `fact Source` 相关的状态、依赖、配置或结果（声明类型 `RoleActivationFactRepository`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `factSource` stores the `fact Source`-related state, dependency, configuration, or result of `RoleActivationFacade` (declared type `RoleActivationFactRepository`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `factSource` 时应保持 `RoleActivationFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `factSource`, preserve `RoleActivationFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RoleActivationFactRepository factSource;
    /**
     * 字段 `transaction` 表示 `RoleActivationFacade` 中与 `transaction` 相关的状态、依赖、配置或结果（声明类型 `ActivationTransaction`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `transaction` stores the `transaction`-related state, dependency, configuration, or result of `RoleActivationFacade` (declared type `ActivationTransaction`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `transaction` 时应保持 `RoleActivationFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `transaction`, preserve `RoleActivationFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ActivationTransaction transaction;
    /**
     * 字段 `resolver` 表示 `RoleActivationFacade` 中与 `resolver` 相关的状态、依赖、配置或结果（声明类型 `RoleActivationResolver`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `resolver` stores the `resolver`-related state, dependency, configuration, or result of `RoleActivationFacade` (declared type `RoleActivationResolver`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `resolver` 时应保持 `RoleActivationFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `resolver`, preserve `RoleActivationFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RoleActivationResolver resolver;
    /**
     * 字段 `snapshotProjector` 表示 `RoleActivationFacade` 中与 `snapshot Projector` 相关的状态、依赖、配置或结果（声明类型 `SessionSnapshotProjector`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `snapshotProjector` stores the `snapshot Projector`-related state, dependency, configuration, or result of `RoleActivationFacade` (declared type `SessionSnapshotProjector`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `snapshotProjector` 时应保持 `RoleActivationFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `snapshotProjector`, preserve `RoleActivationFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final SessionSnapshotProjector snapshotProjector;
    /**
     * 字段 `runtimeStore` 表示 `RoleActivationFacade` 中与 `runtime Store` 相关的状态、依赖、配置或结果（声明类型 `RoleActivationRuntimeRepository`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `runtimeStore` stores the `runtime Store`-related state, dependency, configuration, or result of `RoleActivationFacade` (declared type `RoleActivationRuntimeRepository`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `runtimeStore` 时应保持 `RoleActivationFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `runtimeStore`, preserve `RoleActivationFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RoleActivationRuntimeRepository runtimeStore;
    /**
     * 字段 `runtimePolicy` 表示 `RoleActivationFacade` 中与 `runtime Policy` 相关的状态、依赖、配置或结果（声明类型 `Rbac3RuntimePolicy`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `runtimePolicy` stores the `runtime Policy`-related state, dependency, configuration, or result of `RoleActivationFacade` (declared type `Rbac3RuntimePolicy`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `runtimePolicy` 时应保持 `RoleActivationFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `runtimePolicy`, preserve `RoleActivationFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Rbac3RuntimePolicy runtimePolicy;
    /**
     * 字段 `clock` 表示 `RoleActivationFacade` 中与 `clock` 相关的状态、依赖、配置或结果（声明类型 `Clock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `clock` stores the `clock`-related state, dependency, configuration, or result of `RoleActivationFacade` (declared type `Clock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `clock` 时应保持 `RoleActivationFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `clock`, preserve `RoleActivationFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Clock clock;

    /**
     * 构造器 `RoleActivationFacade` 用于创建并初始化 `RoleActivationFacade` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `RoleActivationFacade` creates and initializes `RoleActivationFacade`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `RoleActivationFacade` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `RoleActivationFacade`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param factSource 输入参数 `factSource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param transaction 输入参数 `transaction`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param snapshotProjector 输入参数 `snapshotProjector`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param runtimeStore 输入参数 `runtimeStore`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param runtimePolicy 输入参数 `runtimePolicy`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public RoleActivationFacade(
            RoleActivationFactRepository factSource,
            ActivationTransaction transaction,
            SessionSnapshotProjector snapshotProjector,
            RoleActivationRuntimeRepository runtimeStore,
            Rbac3RuntimePolicy runtimePolicy,
            Clock clock
    ) {
        this(factSource, transaction, new DefaultRoleActivationResolver(),
                snapshotProjector, runtimeStore, runtimePolicy, clock);
    }

    /**
     * 构造器 `RoleActivationFacade` 用于创建并初始化 `RoleActivationFacade` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `RoleActivationFacade` creates and initializes `RoleActivationFacade`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `RoleActivationFacade` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `RoleActivationFacade`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param factSource 输入参数 `factSource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param transaction 输入参数 `transaction`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param resolver 输入参数 `resolver`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param snapshotProjector 输入参数 `snapshotProjector`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param runtimeStore 输入参数 `runtimeStore`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param runtimePolicy 输入参数 `runtimePolicy`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    RoleActivationFacade(
            RoleActivationFactRepository factSource,
            ActivationTransaction transaction,
            RoleActivationResolver resolver,
            SessionSnapshotProjector snapshotProjector,
            RoleActivationRuntimeRepository runtimeStore,
            Rbac3RuntimePolicy runtimePolicy,
            Clock clock
    ) {
        this.factSource = Objects.requireNonNull(factSource, "factSource");
        this.transaction = Objects.requireNonNull(transaction, "transaction");
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.snapshotProjector = Objects.requireNonNull(
                snapshotProjector, "snapshotProjector");
        this.runtimeStore = Objects.requireNonNull(runtimeStore, "runtimeStore");
        this.runtimePolicy = Objects.requireNonNull(runtimePolicy, "runtimePolicy");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * 方法 `replace` 按照 `RoleActivationFacade` 的职责处理输入，完成 `replace` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `replace` processes its inputs according to `RoleActivationFacade`'s responsibility, performs the `replace` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `replace` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `replace`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public ReplaceActiveRolesResult replace(ReplaceCommandDTO command) {
        Instant now = clock.instant();
        TransactionResultVO result = transaction.replace(command, now, session -> {
            ActivationFactsVO facts = factSource.load(
                    command.tenantId(), command.userId(), now);
            RoleActivationResolution resolution = resolver.resolve(new RoleActivationInput(
                    command.tenantId(),
                    command.userId(),
                    command.sessionId(),
                    command.requestedRoleIds(),
                    facts.assignments(),
                    facts.hierarchy(),
                    facts.dsdSets(),
                    facts.authorizationFacts(),
                    facts.authVersion(),
                    session.sessionVersion(),
                    facts.policyVersion(),
                    now));
            requireWithinRootLimit(resolution);
            requireAuthenticationStrength(session, resolution, facts, now);
            return new ResolvedActivationVO(resolution, facts);
        });
        if (result.changed()) {
            try {
                runtimeStore.createFence(
                        command.tenantId(), command.sessionId(),
                        result.mutationId(), FENCE_TTL);
                transaction.markFenced(result.mutationId(), now);
                SessionSnapshotProjector.Projection projection = snapshotProjector.project(
                        new SessionSnapshotProjector.ProjectionCommand(
                                command.tenantId(), command.identitySub(), command.userId(),
                                command.sessionId(),
                                result.authVersion(), result.sessionVersion(),
                                result.policyVersion(), result.expiresAt(),
                                result.resolved().resolution(),
                                result.resolved().facts(), now));
                runtimeStore.publish(new RuntimePublicationVO(
                        command.tenantId(), command.userId(), command.sessionId(),
                        result.authVersion(), result.sessionVersion(),
                        result.policyVersion(), projection));
                transaction.markCompleted(result.mutationId(), now);
            } catch (RuntimeException exception) {
                try {
                    transaction.markRecoveryRequired(
                            result.mutationId(), "AUTH_PROPAGATION_PENDING", now);
                } catch (RuntimeException recoveryFailure) {
                    exception.addSuppressed(recoveryFailure);
                }
                throw new Rbac3RuleViolation(
                        "AUTH_PROPAGATION_PENDING", List.of(result.mutationId()));
            }
        }
        return new ReplaceActiveRolesResult(
                activeRoles(result.rootsByApplication(), result.resolved().facts()),
                result.changed(),
                result.sessionVersion(),
                result.authVersion(),
                result.policyVersion(),
                false,
                result.snapshotChecksum());
    }

    /**
     * 方法 `current` 按照 `RoleActivationFacade` 的职责处理输入，完成 `current` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `current` processes its inputs according to `RoleActivationFacade`'s responsibility, performs the `current` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `current` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `current`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public ActiveRoleSetView current(
            String tenantId,
            String identitySub,
            String userId,
            String sessionId
    ) {
        Instant now = clock.instant();
        ActivationFactsVO facts = factSource.load(
                tenantId, userId, now);
        CurrentStateVO state = transaction.current(
                tenantId, identitySub, userId, sessionId, now);
        return new ActiveRoleSetView(
                sessionId,
                activeRoles(state.rootsByApplication(), facts),
                state.activationRequired(),
                state.authVersion(),
                state.sessionVersion(),
                state.policyVersion(),
                state.snapshotChecksum() == null ? "unavailable" : state.snapshotChecksum());
    }

    /**
     * 方法 `activeRoles` 按照 `RoleActivationFacade` 的职责处理输入，完成 `active Roles` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `activeRoles` processes its inputs according to `RoleActivationFacade`'s responsibility, performs the `active Roles` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `activeRoles` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `activeRoles`, then continue the business flow using its result, exception, or side effect.
     *
     * @param rootsByApplication 输入参数 `rootsByApplication`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param facts 输入参数 `facts`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private List<ActiveRoleSetView.ApplicationActiveRoles> activeRoles(
            Map<String, Set<String>> rootsByApplication,
            ActivationFactsVO facts
    ) {
        var result = new ArrayList<ActiveRoleSetView.ApplicationActiveRoles>();
        new TreeMap<>(rootsByApplication).forEach((applicationId, roots) -> {
            ApplicationFactVO application =
                    facts.applications().get(applicationId);
            if (application == null) {
                throw new IllegalStateException("missing application fact: " + applicationId);
            }
            result.add(new ActiveRoleSetView.ApplicationActiveRoles(
                    application.code(), new ArrayList<>(new TreeSet<>(roots))));
        });
        return result;
    }

    /**
     * 方法 `requireAuthenticationStrength` 按照 `RoleActivationFacade` 的职责处理输入，完成 `require Authentication Strength` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requireAuthenticationStrength` processes its inputs according to `RoleActivationFacade`'s responsibility, performs the `require Authentication Strength` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requireAuthenticationStrength` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireAuthenticationStrength`, then continue the business flow using its result, exception, or side effect.
     *
     * @param session 输入参数 `session`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param resolution 输入参数 `resolution`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param facts 输入参数 `facts`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void requireAuthenticationStrength(
            SessionStateVO session,
            RoleActivationResolution resolution,
            ActivationFactsVO facts,
            Instant now) {
        int required = resolution.snapshot().effectiveRoleIds().stream()
                .map(facts.hierarchy()::requireNode)
                .map(node -> node.riskLevel())
                .mapToInt(risk -> switch (risk) {
                    case LOW, MEDIUM -> 0;
                    case HIGH -> 1;
                    case CRITICAL -> 2;
                })
                .max()
                .orElse(0);
        int actual = switch (session.authenticationStrength()) {
            case "PASSWORD" -> 0;
            case "MFA" -> 1;
            case "STRONG" -> session.strongAuthenticatedAt() != null
                    && session.strongAuthenticatedAt()
                    .plus(STRONG_AUTHENTICATION_MAX_AGE).isAfter(now) ? 2 : 0;
            default -> 0;
        };
        if (actual < required) {
            throw new Rbac3RuleViolation("STEP_UP_REQUIRED");
        }
    }

    /**
     * 方法 `requireWithinRootLimit` 按照 `RoleActivationFacade` 的职责处理输入，完成 `require Within Root Limit` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requireWithinRootLimit` processes its inputs according to `RoleActivationFacade`'s responsibility, performs the `require Within Root Limit` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requireWithinRootLimit` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireWithinRootLimit`, then continue the business flow using its result, exception, or side effect.
     *
     * @param resolution 输入参数 `resolution`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void requireWithinRootLimit(RoleActivationResolution resolution) {
        int actual = resolution.activeRoleSet().rootIds().size();
        int maximum = runtimePolicy.current().maximumActiveRoots();
        if (actual > maximum) {
            throw new Rbac3RuleViolation(
                    "ACTIVE_ROLE_ROOT_LIMIT_EXCEEDED",
                    List.of(Integer.toString(actual), Integer.toString(maximum)));
        }
    }









}
