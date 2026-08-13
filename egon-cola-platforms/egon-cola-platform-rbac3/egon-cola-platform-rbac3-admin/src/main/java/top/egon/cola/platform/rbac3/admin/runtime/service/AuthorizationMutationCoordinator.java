package top.egon.cola.platform.rbac3.admin.runtime.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;
import top.egon.cola.platform.rbac3.admin.runtime.repository.AuthorizationMutationRepository;
import top.egon.cola.platform.rbac3.admin.runtime.repository.RuntimeProjector;
import top.egon.cola.platform.rbac3.admin.runtime.service.internal.TransactionExecutor;
import top.egon.cola.platform.rbac3.admin.runtime.service.internal.MutationIdGenerator;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.MutationScopeVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.ExpectedVersionsVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.MutationRecordVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.MutationResultVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.enums.AuthorizationMutationResultStatusEnum;

/**
 * 类型 `AuthorizationMutationCoordinator` 位于当前包内，是类型，用于承载 `Authorization Mutation Coordinator` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `AuthorizationMutationCoordinator` is a type in its package and carries the responsibility, state, or contract for `Authorization Mutation Coordinator`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Commits authorization facts first, then projects them under a fail-closed fence.
 */
public final class AuthorizationMutationCoordinator {

    /**
     * 字段 `mutationStore` 表示 `AuthorizationMutationCoordinator` 中与 `mutation Store` 相关的状态、依赖、配置或结果（声明类型 `AuthorizationMutationRepository`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `mutationStore` stores the `mutation Store`-related state, dependency, configuration, or result of `AuthorizationMutationCoordinator` (declared type `AuthorizationMutationRepository`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `mutationStore` 时应保持 `AuthorizationMutationCoordinator` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `mutationStore`, preserve `AuthorizationMutationCoordinator`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AuthorizationMutationRepository mutationStore;
    /**
     * 字段 `fenceService` 表示 `AuthorizationMutationCoordinator` 中与 `fence Service` 相关的状态、依赖、配置或结果（声明类型 `AuthorizationFenceService`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `fenceService` stores the `fence Service`-related state, dependency, configuration, or result of `AuthorizationMutationCoordinator` (declared type `AuthorizationFenceService`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `fenceService` 时应保持 `AuthorizationMutationCoordinator` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `fenceService`, preserve `AuthorizationMutationCoordinator`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AuthorizationFenceService fenceService;
    /**
     * 字段 `projector` 表示 `AuthorizationMutationCoordinator` 中与 `projector` 相关的状态、依赖、配置或结果（声明类型 `RuntimeProjector`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `projector` stores the `projector`-related state, dependency, configuration, or result of `AuthorizationMutationCoordinator` (declared type `RuntimeProjector`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `projector` 时应保持 `AuthorizationMutationCoordinator` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `projector`, preserve `AuthorizationMutationCoordinator`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RuntimeProjector projector;
    /**
     * 字段 `transactionExecutor` 表示 `AuthorizationMutationCoordinator` 中与 `transaction Executor` 相关的状态、依赖、配置或结果（声明类型 `TransactionExecutor`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `transactionExecutor` stores the `transaction Executor`-related state, dependency, configuration, or result of `AuthorizationMutationCoordinator` (declared type `TransactionExecutor`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `transactionExecutor` 时应保持 `AuthorizationMutationCoordinator` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `transactionExecutor`, preserve `AuthorizationMutationCoordinator`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final TransactionExecutor transactionExecutor;
    /**
     * 字段 `idGenerator` 表示 `AuthorizationMutationCoordinator` 中与 `id Generator` 相关的状态、依赖、配置或结果（声明类型 `MutationIdGenerator`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `idGenerator` stores the `id Generator`-related state, dependency, configuration, or result of `AuthorizationMutationCoordinator` (declared type `MutationIdGenerator`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `idGenerator` 时应保持 `AuthorizationMutationCoordinator` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `idGenerator`, preserve `AuthorizationMutationCoordinator`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final MutationIdGenerator idGenerator;
    /**
     * 字段 `clock` 表示 `AuthorizationMutationCoordinator` 中与 `clock` 相关的状态、依赖、配置或结果（声明类型 `Clock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `clock` stores the `clock`-related state, dependency, configuration, or result of `AuthorizationMutationCoordinator` (declared type `Clock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `clock` 时应保持 `AuthorizationMutationCoordinator` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `clock`, preserve `AuthorizationMutationCoordinator`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Clock clock;

    /**
     * 构造器 `AuthorizationMutationCoordinator` 用于创建并初始化 `AuthorizationMutationCoordinator` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `AuthorizationMutationCoordinator` creates and initializes `AuthorizationMutationCoordinator`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `AuthorizationMutationCoordinator` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `AuthorizationMutationCoordinator`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param mutationStore 输入参数 `mutationStore`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param fenceService 输入参数 `fenceService`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param projector 输入参数 `projector`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param transactionExecutor 输入参数 `transactionExecutor`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idGenerator 输入参数 `idGenerator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public AuthorizationMutationCoordinator(
            AuthorizationMutationRepository mutationStore,
            AuthorizationFenceService fenceService,
            RuntimeProjector projector,
            TransactionExecutor transactionExecutor,
            MutationIdGenerator idGenerator,
            Clock clock
    ) {
        this.mutationStore = Objects.requireNonNull(mutationStore, "mutationStore");
        this.fenceService = Objects.requireNonNull(fenceService, "fenceService");
        this.projector = Objects.requireNonNull(projector, "projector");
        this.transactionExecutor = Objects.requireNonNull(
                transactionExecutor, "transactionExecutor");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * 方法 `execute` 按照 `AuthorizationMutationCoordinator` 的职责处理输入，完成 `execute` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `execute` processes its inputs according to `AuthorizationMutationCoordinator`'s responsibility, performs the `execute` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `execute` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `execute`, then continue the business flow using its result, exception, or side effect.
     *
     * @param <T> 类型参数表示数据库变更结果的具体类型；type parameter representing the database mutation result type.
     * @param scope 输入参数 `scope`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param subjectId 输入参数 `subjectId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param versions 输入参数 `versions`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param databaseMutation 输入参数 `databaseMutation`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public <T> MutationResultVO<T> execute(
            MutationScopeVO scope,
            String subjectId,
            ExpectedVersionsVO versions,
            Supplier<T> databaseMutation
    ) {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(versions, "versions");
        Objects.requireNonNull(databaseMutation, "databaseMutation");
        String mutationId = idGenerator.next();
        MutationRecordVO record = new MutationRecordVO(
                mutationId, scope, subjectId, versions, clock.instant());

        @SuppressWarnings("unchecked")
        T value = (T) transactionExecutor.execute(() -> {
            mutationStore.prepare(record);
            T changed = databaseMutation.get();
            mutationStore.transition(
                    mutationId, AuthorizationMutationResultStatusEnum.COMMITTED, null, clock.instant());
            return changed;
        });

        try {
            fenceService.create(
                    scope.tenantId(), scope.scopeType(), scope.scopeId(), mutationId);
            mutationStore.transition(
                    mutationId, AuthorizationMutationResultStatusEnum.FENCED, null, clock.instant());
            projector.project(record);
            mutationStore.transition(
                    mutationId, AuthorizationMutationResultStatusEnum.PROJECTED, null, clock.instant());
            fenceService.release(
                    scope.tenantId(), scope.scopeType(), scope.scopeId());
            mutationStore.transition(
                    mutationId, AuthorizationMutationResultStatusEnum.COMPLETED, null, clock.instant());
            return new MutationResultVO<>(
                    mutationId, true, "ALLOW", value, versions);
        } catch (RuntimeException exception) {
            mutationStore.transition(
                    mutationId, AuthorizationMutationResultStatusEnum.RECOVERY_REQUIRED,
                    "AUTH_PROPAGATION_PENDING", clock.instant());
            return new MutationResultVO<>(
                    mutationId, false, "AUTH_PROPAGATION_PENDING", value, versions);
        }
    }









    }
