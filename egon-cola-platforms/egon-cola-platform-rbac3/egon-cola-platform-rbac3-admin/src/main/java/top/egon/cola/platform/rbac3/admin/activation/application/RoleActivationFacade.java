package top.egon.cola.platform.rbac3.admin.activation.application;

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
     * 字段 `factSource` 表示 `RoleActivationFacade` 中与 `fact Source` 相关的状态、依赖、配置或结果（声明类型 `RoleActivationCandidateService.ActivationFactSource`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `factSource` stores the `fact Source`-related state, dependency, configuration, or result of `RoleActivationFacade` (declared type `RoleActivationCandidateService.ActivationFactSource`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `factSource` 时应保持 `RoleActivationFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `factSource`, preserve `RoleActivationFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RoleActivationCandidateService.ActivationFactSource factSource;
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
     * 字段 `runtimeStore` 表示 `RoleActivationFacade` 中与 `runtime Store` 相关的状态、依赖、配置或结果（声明类型 `RuntimeStore`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `runtimeStore` stores the `runtime Store`-related state, dependency, configuration, or result of `RoleActivationFacade` (declared type `RuntimeStore`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `runtimeStore` 时应保持 `RoleActivationFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `runtimeStore`, preserve `RoleActivationFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RuntimeStore runtimeStore;
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
            RoleActivationCandidateService.ActivationFactSource factSource,
            ActivationTransaction transaction,
            SessionSnapshotProjector snapshotProjector,
            RuntimeStore runtimeStore,
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
            RoleActivationCandidateService.ActivationFactSource factSource,
            ActivationTransaction transaction,
            RoleActivationResolver resolver,
            SessionSnapshotProjector snapshotProjector,
            RuntimeStore runtimeStore,
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
    public ReplaceActiveRolesResult replace(ReplaceCommand command) {
        Instant now = clock.instant();
        TransactionResult result = transaction.replace(command, now, session -> {
            RoleActivationCandidateService.ActivationFacts facts = factSource.load(
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
            return new ResolvedActivation(resolution, facts);
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
                runtimeStore.publish(new RuntimePublication(
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
        RoleActivationCandidateService.ActivationFacts facts = factSource.load(
                tenantId, userId, now);
        CurrentState state = transaction.current(
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
            RoleActivationCandidateService.ActivationFacts facts
    ) {
        var result = new ArrayList<ActiveRoleSetView.ApplicationActiveRoles>();
        new TreeMap<>(rootsByApplication).forEach((applicationId, roots) -> {
            RoleActivationCandidateService.ApplicationFact application =
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
            SessionState session,
            RoleActivationResolution resolution,
            RoleActivationCandidateService.ActivationFacts facts,
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

    /**
     * 类型 `ActivationTransaction` 位于 `RoleActivationFacade` 内，是接口，用于承载 `Activation Transaction` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ActivationTransaction` is an interface inside `RoleActivationFacade` and carries the responsibility, state, or contract for `Activation Transaction`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ActivationTransaction` 作为 `RoleActivationFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ActivationTransaction` as the responsibility boundary of `RoleActivationFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public interface ActivationTransaction {

        /**
         * 方法 `replace` 按照 `ActivationTransaction` 的职责处理输入，完成 `replace` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `replace` processes its inputs according to `ActivationTransaction`'s responsibility, performs the `replace` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `replace` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `replace`, then continue the business flow using its result, exception, or side effect.
         *
         * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param resolutionFactory 输入参数 `resolutionFactory`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        TransactionResult replace(
                ReplaceCommand command,
                Instant now,
                Function<SessionState, ResolvedActivation> resolutionFactory);

        /**
         * 方法 `current` 按照 `ActivationTransaction` 的职责处理输入，完成 `current` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `current` processes its inputs according to `ActivationTransaction`'s responsibility, performs the `current` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `current` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `current`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        CurrentState current(
                String tenantId,
                String identitySub,
                String userId,
                String sessionId,
                Instant now);

        /**
         * 方法 `markFenced` 按照 `ActivationTransaction` 的职责处理输入，完成 `mark Fenced` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `markFenced` processes its inputs according to `ActivationTransaction`'s responsibility, performs the `mark Fenced` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `markFenced` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `markFenced`, then continue the business flow using its result, exception, or side effect.
         *
         * @param mutationId 输入参数 `mutationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        default void markFenced(String mutationId, Instant now) {
        }

        /**
         * 方法 `markCompleted` 按照 `ActivationTransaction` 的职责处理输入，完成 `mark Completed` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `markCompleted` processes its inputs according to `ActivationTransaction`'s responsibility, performs the `mark Completed` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `markCompleted` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `markCompleted`, then continue the business flow using its result, exception, or side effect.
         *
         * @param mutationId 输入参数 `mutationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        default void markCompleted(String mutationId, Instant now) {
        }

        /**
         * 方法 `markRecoveryRequired` 按照 `ActivationTransaction` 的职责处理输入，完成 `mark Recovery Required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `markRecoveryRequired` processes its inputs according to `ActivationTransaction`'s responsibility, performs the `mark Recovery Required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `markRecoveryRequired` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `markRecoveryRequired`, then continue the business flow using its result, exception, or side effect.
         *
         * @param mutationId 输入参数 `mutationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        default void markRecoveryRequired(
                String mutationId,
                String reasonCode,
                Instant now
        ) {
        }
    }

    /**
     * 类型 `RuntimeStore` 位于 `RoleActivationFacade` 内，是接口，用于承载 `Runtime Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RuntimeStore` is an interface inside `RoleActivationFacade` and carries the responsibility, state, or contract for `Runtime Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RuntimeStore` 作为 `RoleActivationFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RuntimeStore` as the responsibility boundary of `RoleActivationFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public interface RuntimeStore {

        /**
         * 方法 `createFence` 按照 `RuntimeStore` 的职责处理输入，完成 `create Fence` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `createFence` processes its inputs according to `RuntimeStore`'s responsibility, performs the `create Fence` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `createFence` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `createFence`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param mutationId 输入参数 `mutationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param ttl 输入参数 `ttl`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void createFence(
                String tenantId,
                String sessionId,
                String mutationId,
                Duration ttl);

        /**
         * 方法 `publish` 按照 `RuntimeStore` 的职责处理输入，完成 `publish` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `publish` processes its inputs according to `RuntimeStore`'s responsibility, performs the `publish` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `publish` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `publish`, then continue the business flow using its result, exception, or side effect.
         *
         * @param publication 输入参数 `publication`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void publish(RuntimePublication publication);
    }

    /**
     * 类型 `ReplaceCommand` 位于 `RoleActivationFacade` 内，是记录类型，用于承载 `Replace Command` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ReplaceCommand` is a record inside `RoleActivationFacade` and carries the responsibility, state, or contract for `Replace Command`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ReplaceCommand` 作为 `RoleActivationFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ReplaceCommand` as the responsibility boundary of `RoleActivationFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param identitySub 记录组件 `identitySub` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `identitySub` carries constructor data whose meaning is defined by the record contract.
     * @param userId 记录组件 `userId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `userId` carries constructor data whose meaning is defined by the record contract.
     * @param sessionId 记录组件 `sessionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionId` carries constructor data whose meaning is defined by the record contract.
     * @param requestedRoleIds 记录组件 `requestedRoleIds` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `requestedRoleIds` carries constructor data whose meaning is defined by the record contract.
     * @param expectedContextVersion 记录组件 `expectedContextVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedContextVersion` carries constructor data whose meaning is defined by the record contract.
     * @param actorId 记录组件 `actorId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorId` carries constructor data whose meaning is defined by the record contract.
     * @param commandId 记录组件 `commandId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `commandId` carries constructor data whose meaning is defined by the record contract.
     */
    public record ReplaceCommand(
            /**
             * 字段 `tenantId` 表示 `ReplaceCommand` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `ReplaceCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `ReplaceCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `ReplaceCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `identitySub` 表示 `ReplaceCommand` 中与 `identity Sub` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `identitySub` stores the `identity Sub`-related state, dependency, configuration, or result of `ReplaceCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `identitySub` 时应保持 `ReplaceCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `identitySub`, preserve `ReplaceCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String identitySub,
            /**
             * 字段 `userId` 表示 `ReplaceCommand` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `ReplaceCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `userId` 时应保持 `ReplaceCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `userId`, preserve `ReplaceCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String userId,
            /**
             * 字段 `sessionId` 表示 `ReplaceCommand` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `ReplaceCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `ReplaceCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `ReplaceCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String sessionId,
            /**
             * 字段 `requestedRoleIds` 表示 `ReplaceCommand` 中与 `requested Role Ids` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `requestedRoleIds` stores the `requested Role Ids`-related state, dependency, configuration, or result of `ReplaceCommand` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `requestedRoleIds` 时应保持 `ReplaceCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `requestedRoleIds`, preserve `ReplaceCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<String> requestedRoleIds,
            /**
             * 字段 `expectedContextVersion` 表示 `ReplaceCommand` 中与 `expected Context Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedContextVersion` stores the `expected Context Version`-related state, dependency, configuration, or result of `ReplaceCommand` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedContextVersion` 时应保持 `ReplaceCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedContextVersion`, preserve `ReplaceCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            long expectedContextVersion,
            /**
             * 字段 `actorId` 表示 `ReplaceCommand` 中与 `actor Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorId` stores the `actor Id`-related state, dependency, configuration, or result of `ReplaceCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorId` 时应保持 `ReplaceCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorId`, preserve `ReplaceCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorId,
            /**
             * 字段 `commandId` 表示 `ReplaceCommand` 中与 `command Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `commandId` stores the `command Id`-related state, dependency, configuration, or result of `ReplaceCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `commandId` 时应保持 `ReplaceCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `commandId`, preserve `ReplaceCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String commandId
    ) {

        /**
         * 构造器 `ReplaceCommand` 用于创建并初始化 `ReplaceCommand` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `ReplaceCommand` creates and initializes `ReplaceCommand`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `ReplaceCommand` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `ReplaceCommand`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param requestedRoleIds 输入参数 `requestedRoleIds`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param expectedContextVersion 输入参数 `expectedContextVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param commandId 输入参数 `commandId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public ReplaceCommand {
            Objects.requireNonNull(identitySub, "identitySub");
            requestedRoleIds = List.copyOf(requestedRoleIds);
            if (expectedContextVersion < 0) {
                throw new IllegalArgumentException(
                        "expectedContextVersion must not be negative");
            }
            Objects.requireNonNull(commandId, "commandId");
        }
    }

    /**
     * 类型 `SessionState` 位于 `RoleActivationFacade` 内，是记录类型，用于承载 `Session State` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `SessionState` is a record inside `RoleActivationFacade` and carries the responsibility, state, or contract for `Session State`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `SessionState` 作为 `RoleActivationFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SessionState` as the responsibility boundary of `RoleActivationFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param userId 记录组件 `userId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `userId` carries constructor data whose meaning is defined by the record contract.
     * @param sessionId 记录组件 `sessionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionId` carries constructor data whose meaning is defined by the record contract.
     * @param rootsByApplication 记录组件 `rootsByApplication` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `rootsByApplication` carries constructor data whose meaning is defined by the record contract.
     * @param authVersion 记录组件 `authVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authVersion` carries constructor data whose meaning is defined by the record contract.
     * @param sessionVersion 记录组件 `sessionVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionVersion` carries constructor data whose meaning is defined by the record contract.
     * @param policyVersion 记录组件 `policyVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyVersion` carries constructor data whose meaning is defined by the record contract.
     * @param snapshotChecksum 记录组件 `snapshotChecksum` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `snapshotChecksum` carries constructor data whose meaning is defined by the record contract.
     * @param activationRequired 记录组件 `activationRequired` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `activationRequired` carries constructor data whose meaning is defined by the record contract.
     * @param expiresAt 记录组件 `expiresAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expiresAt` carries constructor data whose meaning is defined by the record contract.
     * @param authenticationStrength 记录组件 `authenticationStrength` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authenticationStrength` carries constructor data whose meaning is defined by the record contract.
     * @param strongAuthenticatedAt 记录组件 `strongAuthenticatedAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `strongAuthenticatedAt` carries constructor data whose meaning is defined by the record contract.
     */
    public record SessionState(
            /**
             * 字段 `tenantId` 表示 `SessionState` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `SessionState` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `SessionState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `SessionState`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `userId` 表示 `SessionState` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `SessionState` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `userId` 时应保持 `SessionState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `userId`, preserve `SessionState`'s lifecycle, immutability, and thread-safety constraints.
             */
            String userId,
            /**
             * 字段 `sessionId` 表示 `SessionState` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `SessionState` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `SessionState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `SessionState`'s lifecycle, immutability, and thread-safety constraints.
             */
            String sessionId,
            /**
             * 字段 `rootsByApplication` 表示 `SessionState` 中与 `roots By Application` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, Set&lt;String&gt;&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `rootsByApplication` stores the `roots By Application`-related state, dependency, configuration, or result of `SessionState` (declared type `Map&lt;String, Set&lt;String&gt;&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `rootsByApplication` 时应保持 `SessionState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `rootsByApplication`, preserve `SessionState`'s lifecycle, immutability, and thread-safety constraints.
             */
            Map<String, Set<String>> rootsByApplication,
            /**
             * 字段 `authVersion` 表示 `SessionState` 中与 `auth Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authVersion` stores the `auth Version`-related state, dependency, configuration, or result of `SessionState` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authVersion` 时应保持 `SessionState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authVersion`, preserve `SessionState`'s lifecycle, immutability, and thread-safety constraints.
             */
            long authVersion,
            /**
             * 字段 `sessionVersion` 表示 `SessionState` 中与 `session Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionVersion` stores the `session Version`-related state, dependency, configuration, or result of `SessionState` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionVersion` 时应保持 `SessionState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionVersion`, preserve `SessionState`'s lifecycle, immutability, and thread-safety constraints.
             */
            long sessionVersion,
            /**
             * 字段 `policyVersion` 表示 `SessionState` 中与 `policy Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `SessionState` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `SessionState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `SessionState`'s lifecycle, immutability, and thread-safety constraints.
             */
            long policyVersion,
            /**
             * 字段 `snapshotChecksum` 表示 `SessionState` 中与 `snapshot Checksum` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `snapshotChecksum` stores the `snapshot Checksum`-related state, dependency, configuration, or result of `SessionState` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `snapshotChecksum` 时应保持 `SessionState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `snapshotChecksum`, preserve `SessionState`'s lifecycle, immutability, and thread-safety constraints.
             */
            String snapshotChecksum,
            /**
             * 字段 `activationRequired` 表示 `SessionState` 中与 `activation Required` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `activationRequired` stores the `activation Required`-related state, dependency, configuration, or result of `SessionState` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `activationRequired` 时应保持 `SessionState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `activationRequired`, preserve `SessionState`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean activationRequired,
            /**
             * 字段 `expiresAt` 表示 `SessionState` 中与 `expires At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expiresAt` stores the `expires At`-related state, dependency, configuration, or result of `SessionState` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expiresAt` 时应保持 `SessionState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expiresAt`, preserve `SessionState`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant expiresAt,
            /**
             * 字段 `authenticationStrength` 表示 `SessionState` 中与 `authentication Strength` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authenticationStrength` stores the `authentication Strength`-related state, dependency, configuration, or result of `SessionState` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authenticationStrength` 时应保持 `SessionState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authenticationStrength`, preserve `SessionState`'s lifecycle, immutability, and thread-safety constraints.
             */
            String authenticationStrength,
            /**
             * 字段 `strongAuthenticatedAt` 表示 `SessionState` 中与 `strong Authenticated At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `strongAuthenticatedAt` stores the `strong Authenticated At`-related state, dependency, configuration, or result of `SessionState` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `strongAuthenticatedAt` 时应保持 `SessionState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `strongAuthenticatedAt`, preserve `SessionState`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant strongAuthenticatedAt
    ) {

        /**
         * 构造器 `SessionState` 用于创建并初始化 `SessionState` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `SessionState` creates and initializes `SessionState`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `SessionState` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `SessionState`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param rootsByApplication 输入参数 `rootsByApplication`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param authVersion 输入参数 `authVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param sessionVersion 输入参数 `sessionVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param policyVersion 输入参数 `policyVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param snapshotChecksum 输入参数 `snapshotChecksum`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param activationRequired 输入参数 `activationRequired`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param expiresAt 输入参数 `expiresAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public SessionState(
                String tenantId,
                String userId,
                String sessionId,
                Map<String, Set<String>> rootsByApplication,
                long authVersion,
                long sessionVersion,
                long policyVersion,
                String snapshotChecksum,
                boolean activationRequired,
                Instant expiresAt) {
            this(tenantId, userId, sessionId, rootsByApplication, authVersion,
                    sessionVersion, policyVersion, snapshotChecksum,
                    activationRequired, expiresAt, "PASSWORD", null);
        }
    }

    /**
     * 类型 `ResolvedActivation` 位于 `RoleActivationFacade` 内，是记录类型，用于承载 `Resolved Activation` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ResolvedActivation` is a record inside `RoleActivationFacade` and carries the responsibility, state, or contract for `Resolved Activation`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ResolvedActivation` 作为 `RoleActivationFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ResolvedActivation` as the responsibility boundary of `RoleActivationFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param resolution 记录组件 `resolution` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `resolution` carries constructor data whose meaning is defined by the record contract.
     * @param facts 记录组件 `facts` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `facts` carries constructor data whose meaning is defined by the record contract.
     */
    public record ResolvedActivation(
            /**
             * 字段 `resolution` 表示 `ResolvedActivation` 中与 `resolution` 相关的状态、依赖、配置或结果（声明类型 `RoleActivationResolution`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `resolution` stores the `resolution`-related state, dependency, configuration, or result of `ResolvedActivation` (declared type `RoleActivationResolution`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `resolution` 时应保持 `ResolvedActivation` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `resolution`, preserve `ResolvedActivation`'s lifecycle, immutability, and thread-safety constraints.
             */
            RoleActivationResolution resolution,
            /**
             * 字段 `facts` 表示 `ResolvedActivation` 中与 `facts` 相关的状态、依赖、配置或结果（声明类型 `RoleActivationCandidateService.ActivationFacts`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `facts` stores the `facts`-related state, dependency, configuration, or result of `ResolvedActivation` (declared type `RoleActivationCandidateService.ActivationFacts`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `facts` 时应保持 `ResolvedActivation` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `facts`, preserve `ResolvedActivation`'s lifecycle, immutability, and thread-safety constraints.
             */
            RoleActivationCandidateService.ActivationFacts facts
    ) {
    }

    /**
     * 类型 `TransactionResult` 位于 `RoleActivationFacade` 内，是记录类型，用于承载 `Transaction Result` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `TransactionResult` is a record inside `RoleActivationFacade` and carries the responsibility, state, or contract for `Transaction Result`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `TransactionResult` 作为 `RoleActivationFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `TransactionResult` as the responsibility boundary of `RoleActivationFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param resolved 记录组件 `resolved` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `resolved` carries constructor data whose meaning is defined by the record contract.
     * @param changed 记录组件 `changed` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `changed` carries constructor data whose meaning is defined by the record contract.
     * @param mutationId 记录组件 `mutationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `mutationId` carries constructor data whose meaning is defined by the record contract.
     * @param rootsByApplication 记录组件 `rootsByApplication` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `rootsByApplication` carries constructor data whose meaning is defined by the record contract.
     * @param authVersion 记录组件 `authVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authVersion` carries constructor data whose meaning is defined by the record contract.
     * @param sessionVersion 记录组件 `sessionVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionVersion` carries constructor data whose meaning is defined by the record contract.
     * @param policyVersion 记录组件 `policyVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyVersion` carries constructor data whose meaning is defined by the record contract.
     * @param snapshotChecksum 记录组件 `snapshotChecksum` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `snapshotChecksum` carries constructor data whose meaning is defined by the record contract.
     * @param expiresAt 记录组件 `expiresAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expiresAt` carries constructor data whose meaning is defined by the record contract.
     */
    public record TransactionResult(
            /**
             * 字段 `resolved` 表示 `TransactionResult` 中与 `resolved` 相关的状态、依赖、配置或结果（声明类型 `ResolvedActivation`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `resolved` stores the `resolved`-related state, dependency, configuration, or result of `TransactionResult` (declared type `ResolvedActivation`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `resolved` 时应保持 `TransactionResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `resolved`, preserve `TransactionResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            ResolvedActivation resolved,
            /**
             * 字段 `changed` 表示 `TransactionResult` 中与 `changed` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `changed` stores the `changed`-related state, dependency, configuration, or result of `TransactionResult` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `changed` 时应保持 `TransactionResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `changed`, preserve `TransactionResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean changed,
            /**
             * 字段 `mutationId` 表示 `TransactionResult` 中与 `mutation Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `mutationId` stores the `mutation Id`-related state, dependency, configuration, or result of `TransactionResult` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `mutationId` 时应保持 `TransactionResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `mutationId`, preserve `TransactionResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            String mutationId,
            /**
             * 字段 `rootsByApplication` 表示 `TransactionResult` 中与 `roots By Application` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, Set&lt;String&gt;&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `rootsByApplication` stores the `roots By Application`-related state, dependency, configuration, or result of `TransactionResult` (declared type `Map&lt;String, Set&lt;String&gt;&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `rootsByApplication` 时应保持 `TransactionResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `rootsByApplication`, preserve `TransactionResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            Map<String, Set<String>> rootsByApplication,
            /**
             * 字段 `authVersion` 表示 `TransactionResult` 中与 `auth Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authVersion` stores the `auth Version`-related state, dependency, configuration, or result of `TransactionResult` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authVersion` 时应保持 `TransactionResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authVersion`, preserve `TransactionResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            long authVersion,
            /**
             * 字段 `sessionVersion` 表示 `TransactionResult` 中与 `session Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionVersion` stores the `session Version`-related state, dependency, configuration, or result of `TransactionResult` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionVersion` 时应保持 `TransactionResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionVersion`, preserve `TransactionResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            long sessionVersion,
            /**
             * 字段 `policyVersion` 表示 `TransactionResult` 中与 `policy Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `TransactionResult` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `TransactionResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `TransactionResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            long policyVersion,
            /**
             * 字段 `snapshotChecksum` 表示 `TransactionResult` 中与 `snapshot Checksum` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `snapshotChecksum` stores the `snapshot Checksum`-related state, dependency, configuration, or result of `TransactionResult` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `snapshotChecksum` 时应保持 `TransactionResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `snapshotChecksum`, preserve `TransactionResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            String snapshotChecksum,
            /**
             * 字段 `expiresAt` 表示 `TransactionResult` 中与 `expires At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expiresAt` stores the `expires At`-related state, dependency, configuration, or result of `TransactionResult` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expiresAt` 时应保持 `TransactionResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expiresAt`, preserve `TransactionResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant expiresAt
    ) {
    }

    /**
     * 类型 `CurrentState` 位于 `RoleActivationFacade` 内，是记录类型，用于承载 `Current State` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `CurrentState` is a record inside `RoleActivationFacade` and carries the responsibility, state, or contract for `Current State`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `CurrentState` 作为 `RoleActivationFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `CurrentState` as the responsibility boundary of `RoleActivationFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param rootsByApplication 记录组件 `rootsByApplication` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `rootsByApplication` carries constructor data whose meaning is defined by the record contract.
     * @param authVersion 记录组件 `authVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authVersion` carries constructor data whose meaning is defined by the record contract.
     * @param sessionVersion 记录组件 `sessionVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionVersion` carries constructor data whose meaning is defined by the record contract.
     * @param policyVersion 记录组件 `policyVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyVersion` carries constructor data whose meaning is defined by the record contract.
     * @param snapshotChecksum 记录组件 `snapshotChecksum` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `snapshotChecksum` carries constructor data whose meaning is defined by the record contract.
     * @param activationRequired 记录组件 `activationRequired` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `activationRequired` carries constructor data whose meaning is defined by the record contract.
     */
    public record CurrentState(
            /**
             * 字段 `rootsByApplication` 表示 `CurrentState` 中与 `roots By Application` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, Set&lt;String&gt;&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `rootsByApplication` stores the `roots By Application`-related state, dependency, configuration, or result of `CurrentState` (declared type `Map&lt;String, Set&lt;String&gt;&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `rootsByApplication` 时应保持 `CurrentState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `rootsByApplication`, preserve `CurrentState`'s lifecycle, immutability, and thread-safety constraints.
             */
            Map<String, Set<String>> rootsByApplication,
            /**
             * 字段 `authVersion` 表示 `CurrentState` 中与 `auth Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authVersion` stores the `auth Version`-related state, dependency, configuration, or result of `CurrentState` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authVersion` 时应保持 `CurrentState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authVersion`, preserve `CurrentState`'s lifecycle, immutability, and thread-safety constraints.
             */
            long authVersion,
            /**
             * 字段 `sessionVersion` 表示 `CurrentState` 中与 `session Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionVersion` stores the `session Version`-related state, dependency, configuration, or result of `CurrentState` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionVersion` 时应保持 `CurrentState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionVersion`, preserve `CurrentState`'s lifecycle, immutability, and thread-safety constraints.
             */
            long sessionVersion,
            /**
             * 字段 `policyVersion` 表示 `CurrentState` 中与 `policy Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `CurrentState` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `CurrentState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `CurrentState`'s lifecycle, immutability, and thread-safety constraints.
             */
            long policyVersion,
            /**
             * 字段 `snapshotChecksum` 表示 `CurrentState` 中与 `snapshot Checksum` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `snapshotChecksum` stores the `snapshot Checksum`-related state, dependency, configuration, or result of `CurrentState` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `snapshotChecksum` 时应保持 `CurrentState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `snapshotChecksum`, preserve `CurrentState`'s lifecycle, immutability, and thread-safety constraints.
             */
            String snapshotChecksum,
            /**
             * 字段 `activationRequired` 表示 `CurrentState` 中与 `activation Required` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `activationRequired` stores the `activation Required`-related state, dependency, configuration, or result of `CurrentState` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `activationRequired` 时应保持 `CurrentState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `activationRequired`, preserve `CurrentState`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean activationRequired
    ) {
    }

    /**
     * 类型 `RuntimePublication` 位于 `RoleActivationFacade` 内，是记录类型，用于承载 `Runtime Publication` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RuntimePublication` is a record inside `RoleActivationFacade` and carries the responsibility, state, or contract for `Runtime Publication`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RuntimePublication` 作为 `RoleActivationFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RuntimePublication` as the responsibility boundary of `RoleActivationFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param userId 记录组件 `userId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `userId` carries constructor data whose meaning is defined by the record contract.
     * @param sessionId 记录组件 `sessionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionId` carries constructor data whose meaning is defined by the record contract.
     * @param authVersion 记录组件 `authVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authVersion` carries constructor data whose meaning is defined by the record contract.
     * @param sessionVersion 记录组件 `sessionVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionVersion` carries constructor data whose meaning is defined by the record contract.
     * @param policyVersion 记录组件 `policyVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyVersion` carries constructor data whose meaning is defined by the record contract.
     * @param projection 记录组件 `projection` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `projection` carries constructor data whose meaning is defined by the record contract.
     */
    public record RuntimePublication(
            /**
             * 字段 `tenantId` 表示 `RuntimePublication` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `RuntimePublication` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `RuntimePublication` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `RuntimePublication`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `userId` 表示 `RuntimePublication` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `RuntimePublication` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `userId` 时应保持 `RuntimePublication` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `userId`, preserve `RuntimePublication`'s lifecycle, immutability, and thread-safety constraints.
             */
            String userId,
            /**
             * 字段 `sessionId` 表示 `RuntimePublication` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `RuntimePublication` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `RuntimePublication` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `RuntimePublication`'s lifecycle, immutability, and thread-safety constraints.
             */
            String sessionId,
            /**
             * 字段 `authVersion` 表示 `RuntimePublication` 中与 `auth Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authVersion` stores the `auth Version`-related state, dependency, configuration, or result of `RuntimePublication` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authVersion` 时应保持 `RuntimePublication` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authVersion`, preserve `RuntimePublication`'s lifecycle, immutability, and thread-safety constraints.
             */
            long authVersion,
            /**
             * 字段 `sessionVersion` 表示 `RuntimePublication` 中与 `session Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionVersion` stores the `session Version`-related state, dependency, configuration, or result of `RuntimePublication` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionVersion` 时应保持 `RuntimePublication` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionVersion`, preserve `RuntimePublication`'s lifecycle, immutability, and thread-safety constraints.
             */
            long sessionVersion,
            /**
             * 字段 `policyVersion` 表示 `RuntimePublication` 中与 `policy Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `RuntimePublication` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `RuntimePublication` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `RuntimePublication`'s lifecycle, immutability, and thread-safety constraints.
             */
            long policyVersion,
            /**
             * 字段 `projection` 表示 `RuntimePublication` 中与 `projection` 相关的状态、依赖、配置或结果（声明类型 `SessionSnapshotProjector.Projection`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `projection` stores the `projection`-related state, dependency, configuration, or result of `RuntimePublication` (declared type `SessionSnapshotProjector.Projection`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `projection` 时应保持 `RuntimePublication` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `projection`, preserve `RuntimePublication`'s lifecycle, immutability, and thread-safety constraints.
             */
            SessionSnapshotProjector.Projection projection
    ) {
    }

}
