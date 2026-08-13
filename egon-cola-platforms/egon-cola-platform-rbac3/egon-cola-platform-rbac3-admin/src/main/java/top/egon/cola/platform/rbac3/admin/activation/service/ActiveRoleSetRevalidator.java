package top.egon.cola.platform.rbac3.admin.activation.service;

import top.egon.cola.platform.rbac3.core.activation.DefaultRoleActivationResolver;
import top.egon.cola.platform.rbac3.core.activation.RoleActivationInput;
import top.egon.cola.platform.rbac3.core.activation.RoleActivationResolver;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.activation.repository.CurrentActivationRepository;
import top.egon.cola.platform.rbac3.admin.activation.repository.ReselectionRepository;
import top.egon.cola.platform.rbac3.admin.activation.domain.dto.RevalidationCommandDTO;
import top.egon.cola.platform.rbac3.admin.activation.domain.vo.CurrentActivationVO;
import top.egon.cola.platform.rbac3.admin.activation.domain.vo.RevalidationResultVO;
import top.egon.cola.platform.rbac3.admin.activation.repository.RoleActivationFactRepository;

/**
 * 类型 `ActiveRoleSetRevalidator` 位于当前包内，是类型，用于承载 `Active Role Set Revalidator` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `ActiveRoleSetRevalidator` is a type in its package and carries the responsibility, state, or contract for `Active Role Set Revalidator`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Revalidates persisted roots after policy or assignment facts change.
 */
public final class ActiveRoleSetRevalidator {

    /**
     * 字段 `factSource` 表示 `ActiveRoleSetRevalidator` 中与 `fact Source` 相关的状态、依赖、配置或结果（声明类型 `RoleActivationFactRepository`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `factSource` stores the `fact Source`-related state, dependency, configuration, or result of `ActiveRoleSetRevalidator` (declared type `RoleActivationFactRepository`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `factSource` 时应保持 `ActiveRoleSetRevalidator` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `factSource`, preserve `ActiveRoleSetRevalidator`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RoleActivationFactRepository factSource;
    /**
     * 字段 `currentSource` 表示 `ActiveRoleSetRevalidator` 中与 `current Source` 相关的状态、依赖、配置或结果（声明类型 `CurrentActivationRepository`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `currentSource` stores the `current Source`-related state, dependency, configuration, or result of `ActiveRoleSetRevalidator` (declared type `CurrentActivationRepository`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `currentSource` 时应保持 `ActiveRoleSetRevalidator` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `currentSource`, preserve `ActiveRoleSetRevalidator`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final CurrentActivationRepository currentSource;
    /**
     * 字段 `reselectionStore` 表示 `ActiveRoleSetRevalidator` 中与 `reselection Store` 相关的状态、依赖、配置或结果（声明类型 `ReselectionRepository`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `reselectionStore` stores the `reselection Store`-related state, dependency, configuration, or result of `ActiveRoleSetRevalidator` (declared type `ReselectionRepository`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `reselectionStore` 时应保持 `ActiveRoleSetRevalidator` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `reselectionStore`, preserve `ActiveRoleSetRevalidator`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ReselectionRepository reselectionStore;
    /**
     * 字段 `resolver` 表示 `ActiveRoleSetRevalidator` 中与 `resolver` 相关的状态、依赖、配置或结果（声明类型 `RoleActivationResolver`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `resolver` stores the `resolver`-related state, dependency, configuration, or result of `ActiveRoleSetRevalidator` (declared type `RoleActivationResolver`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `resolver` 时应保持 `ActiveRoleSetRevalidator` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `resolver`, preserve `ActiveRoleSetRevalidator`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RoleActivationResolver resolver;

    /**
     * 构造器 `ActiveRoleSetRevalidator` 用于创建并初始化 `ActiveRoleSetRevalidator` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ActiveRoleSetRevalidator` creates and initializes `ActiveRoleSetRevalidator`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ActiveRoleSetRevalidator` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ActiveRoleSetRevalidator`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param factSource 输入参数 `factSource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param currentSource 输入参数 `currentSource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param reselectionStore 输入参数 `reselectionStore`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public ActiveRoleSetRevalidator(
            RoleActivationFactRepository factSource,
            CurrentActivationRepository currentSource,
            ReselectionRepository reselectionStore
    ) {
        this(factSource, currentSource, reselectionStore,
                new DefaultRoleActivationResolver());
    }

    /**
     * 构造器 `ActiveRoleSetRevalidator` 用于创建并初始化 `ActiveRoleSetRevalidator` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ActiveRoleSetRevalidator` creates and initializes `ActiveRoleSetRevalidator`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ActiveRoleSetRevalidator` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ActiveRoleSetRevalidator`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param factSource 输入参数 `factSource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param currentSource 输入参数 `currentSource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param reselectionStore 输入参数 `reselectionStore`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param resolver 输入参数 `resolver`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    ActiveRoleSetRevalidator(
            RoleActivationFactRepository factSource,
            CurrentActivationRepository currentSource,
            ReselectionRepository reselectionStore,
            RoleActivationResolver resolver
    ) {
        this.factSource = Objects.requireNonNull(factSource, "factSource");
        this.currentSource = Objects.requireNonNull(currentSource, "currentSource");
        this.reselectionStore = Objects.requireNonNull(reselectionStore, "reselectionStore");
        this.resolver = Objects.requireNonNull(resolver, "resolver");
    }

    /**
     * 方法 `revalidate` 按照 `ActiveRoleSetRevalidator` 的职责处理输入，完成 `revalidate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `revalidate` processes its inputs according to `ActiveRoleSetRevalidator`'s responsibility, performs the `revalidate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `revalidate` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `revalidate`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public RevalidationResultVO revalidate(RevalidationCommandDTO command) {
        CurrentActivationVO current = currentSource.current(
                command.tenantId(), command.userId(), command.sessionId());
        if (current.rootRoleIds().isEmpty()) {
            return new RevalidationResultVO(false, true, "ROLE_ACTIVATION_REQUIRED");
        }
        var facts = factSource.load(
                command.tenantId(), command.userId(), command.databaseNow());
        try {
            resolver.resolve(new RoleActivationInput(
                    command.tenantId(), command.userId(), command.sessionId(),
                    current.rootRoleIds(), facts.assignments(), facts.hierarchy(),
                    facts.dsdSets(), facts.authorizationFacts(), facts.authVersion(),
                    current.sessionVersion(), facts.policyVersion(), command.databaseNow()));
            return new RevalidationResultVO(true, false, "ALLOW");
        } catch (Rbac3RuleViolation violation) {
            reselectionStore.requireReselection(
                    command.tenantId(), command.sessionId(),
                    current.sessionVersion(), command.databaseNow(), command.actorId());
            return new RevalidationResultVO(false, true, "ROLE_RESELECTION_REQUIRED");
        }
    }





    }
