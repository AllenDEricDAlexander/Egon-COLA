package top.egon.cola.platform.rbac3.admin.bootstrap.service;

import top.egon.cola.platform.rbac3.admin.activation.service.RoleActivationCandidateService;
import top.egon.cola.platform.rbac3.admin.activation.service.RoleActivationFacade;
import top.egon.cola.platform.rbac3.admin.session.service.AuthorizationContextFacade;
import top.egon.cola.platform.rbac3.admin.snapshot.application.SystemAuthorizationSnapshotService;
import top.egon.cola.platform.rbac3.contract.activation.RoleActivationCandidateView;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import top.egon.cola.platform.rbac3.admin.bootstrap.service.internal.CandidateRepository;
import top.egon.cola.platform.rbac3.admin.bootstrap.service.internal.RoleActivator;
import top.egon.cola.platform.rbac3.admin.bootstrap.domain.vo.ApplicationDefinitionVO;
import top.egon.cola.platform.rbac3.admin.session.domain.vo.AuthorizationContextVO;
import top.egon.cola.platform.rbac3.admin.activation.domain.dto.ReplaceCommandDTO;

/**
 * 类型 `Rbac3DevelopmentAuthorizationContextInitializer` 位于当前包内，是类型，用于承载 `Rbac3 Development Authorization Context Initializer` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3DevelopmentAuthorizationContextInitializer` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Development Authorization Context Initializer`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * Activates the generated local administrator roles for an opted-in development stack.
 */
public final class Rbac3DevelopmentAuthorizationContextInitializer
        implements SystemAuthorizationSnapshotService.ContextInitializer {

    /**
     * 字段 `DEVELOPMENT_ACTOR` 表示 `Rbac3DevelopmentAuthorizationContextInitializer` 中与 `DEVELOPMENT ACTOR` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `DEVELOPMENT_ACTOR` stores the `DEVELOPMENT ACTOR`-related state, dependency, configuration, or result of `Rbac3DevelopmentAuthorizationContextInitializer` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `DEVELOPMENT_ACTOR` 时应保持 `Rbac3DevelopmentAuthorizationContextInitializer` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `DEVELOPMENT_ACTOR`, preserve `Rbac3DevelopmentAuthorizationContextInitializer`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final String DEVELOPMENT_ACTOR = "development-bootstrap";
    /** 开发拓扑中按应用分组的角色编码；development role codes grouped by application.
     * 含义与用法：读取、传递或更新 `DEVELOPMENT_ROLES_BY_APPLICATION` 时应保持 `Rbac3DevelopmentAuthorizationContextInitializer` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `DEVELOPMENT_ROLES_BY_APPLICATION`, preserve `Rbac3DevelopmentAuthorizationContextInitializer`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final Map<String, Set<String>> DEVELOPMENT_ROLES_BY_APPLICATION =
            Rbac3DevelopmentTopology.applications().stream()
                    .collect(Collectors.groupingBy(
                            ApplicationDefinitionVO::applicationCode,
                            Collectors.mapping(
                                    ApplicationDefinitionVO::roleCode,
                                    Collectors.toUnmodifiableSet()
                            )
                    ));

    /**
     * 字段 `enabled` 表示 `Rbac3DevelopmentAuthorizationContextInitializer` 中与 `enabled` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `enabled` stores the `enabled`-related state, dependency, configuration, or result of `Rbac3DevelopmentAuthorizationContextInitializer` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `enabled` 时应保持 `Rbac3DevelopmentAuthorizationContextInitializer` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `enabled`, preserve `Rbac3DevelopmentAuthorizationContextInitializer`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final boolean enabled;
    /**
     * 字段 `candidates` 表示 `Rbac3DevelopmentAuthorizationContextInitializer` 中与 `candidates` 相关的状态、依赖、配置或结果（声明类型 `CandidateRepository`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `candidates` stores the `candidates`-related state, dependency, configuration, or result of `Rbac3DevelopmentAuthorizationContextInitializer` (declared type `CandidateRepository`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `candidates` 时应保持 `Rbac3DevelopmentAuthorizationContextInitializer` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `candidates`, preserve `Rbac3DevelopmentAuthorizationContextInitializer`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final CandidateRepository candidates;
    /**
     * 字段 `activator` 表示 `Rbac3DevelopmentAuthorizationContextInitializer` 中与 `activator` 相关的状态、依赖、配置或结果（声明类型 `RoleActivator`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `activator` stores the `activator`-related state, dependency, configuration, or result of `Rbac3DevelopmentAuthorizationContextInitializer` (declared type `RoleActivator`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `activator` 时应保持 `Rbac3DevelopmentAuthorizationContextInitializer` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `activator`, preserve `Rbac3DevelopmentAuthorizationContextInitializer`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RoleActivator activator;

    /**
     * 构造器 `Rbac3DevelopmentAuthorizationContextInitializer` 用于创建并初始化 `Rbac3DevelopmentAuthorizationContextInitializer` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `Rbac3DevelopmentAuthorizationContextInitializer` creates and initializes `Rbac3DevelopmentAuthorizationContextInitializer`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `Rbac3DevelopmentAuthorizationContextInitializer` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `Rbac3DevelopmentAuthorizationContextInitializer`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param enabled 输入参数 `enabled`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param candidates 输入参数 `candidates`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param activator 输入参数 `activator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public Rbac3DevelopmentAuthorizationContextInitializer(
            boolean enabled,
            RoleActivationCandidateService candidates,
            RoleActivationFacade activator) {
        this(enabled, candidates::candidates, activator::replace);
    }

    /**
     * 构造器 `Rbac3DevelopmentAuthorizationContextInitializer` 用于创建并初始化 `Rbac3DevelopmentAuthorizationContextInitializer` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `Rbac3DevelopmentAuthorizationContextInitializer` creates and initializes `Rbac3DevelopmentAuthorizationContextInitializer`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `Rbac3DevelopmentAuthorizationContextInitializer` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `Rbac3DevelopmentAuthorizationContextInitializer`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param enabled 输入参数 `enabled`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param candidates 输入参数 `candidates`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param activator 输入参数 `activator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    Rbac3DevelopmentAuthorizationContextInitializer(
            boolean enabled,
            CandidateRepository candidates,
            RoleActivator activator) {
        this.enabled = enabled;
        this.candidates = Objects.requireNonNull(candidates, "candidates");
        this.activator = Objects.requireNonNull(activator, "activator");
    }

    /**
     * 方法 `initialize` 按照 `Rbac3DevelopmentAuthorizationContextInitializer` 的职责处理输入，完成 `initialize` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `initialize` processes its inputs according to `Rbac3DevelopmentAuthorizationContextInitializer`'s responsibility, performs the `initialize` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `initialize` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `initialize`, then continue the business flow using its result, exception, or side effect.
     *
     * @param context 输入参数 `context`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    public SystemAuthorizationSnapshotService.ContextInitialization initialize(
            AuthorizationContextVO context,
            Instant now) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(now, "now");
        if (!enabled || !context.activationRequired()) {
            return SystemAuthorizationSnapshotService.ContextInitialization.UNCHANGED;
        }
        RoleActivationCandidateView view = candidates.load(
                context.tenantId(), context.rbac3UserId(), now);
        List<String> roleIds = view.applications().stream()
                .flatMap(application -> application.candidates().stream()
                        .filter(candidate -> DEVELOPMENT_ROLES_BY_APPLICATION
                                .getOrDefault(
                                        application.applicationCode(),
                                        Set.of()
                                )
                                .contains(candidate.rootRoleCode())))
                .filter(candidate -> "PASSWORD".equals(candidate.requiredAuthStrength()))
                .map(candidate -> candidate.rootRoleId())
                .sorted()
                .toList();
        if (roleIds.isEmpty()) {
            return SystemAuthorizationSnapshotService.ContextInitialization.UNCHANGED;
        }
        try {
            activator.replace(new ReplaceCommandDTO(
                    context.tenantId(), context.identitySub(),
                    context.rbac3UserId(), context.sessionId(), roleIds,
                    context.contextVersion(), DEVELOPMENT_ACTOR,
                    "development-bootstrap:auto-activate-local-admin:"
                            + context.tenantId() + ':' + context.sessionId()
                            + ':' + context.contextVersion()));
        } catch (Rbac3RuleViolation violation) {
            if (!"ROLE_ACTIVATION_VERSION_CONFLICT".equals(
                    violation.reasonCode())) {
                throw violation;
            }
            return SystemAuthorizationSnapshotService.ContextInitialization.CONCURRENT;
        }
        return SystemAuthorizationSnapshotService.ContextInitialization.COMPLETED;
    }
}
