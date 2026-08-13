package top.egon.cola.platform.rbac3.admin.assignment.repository.jdbc;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.assignment.domain.po.UserRoleAssignmentPO;
import top.egon.cola.platform.rbac3.admin.identity.domain.po.UserPO;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import top.egon.cola.platform.rbac3.admin.assignment.repository.internal.DueAssignment;
import top.egon.cola.platform.rbac3.admin.runtime.controller.scheduled.AssignmentLifecycleWorker;
import top.egon.cola.platform.rbac3.admin.runtime.repository.AssignmentLifecycleRepository;
import top.egon.cola.platform.rbac3.admin.runtime.repository.ChangePublisher;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.LifecycleChangeVO;

/**
 * 类型 `PostgresqlAssignmentLifecycleRepository` 位于当前包内，是类型，用于承载 `Postgresql Assignment Lifecycle Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `PostgresqlAssignmentLifecycleRepository` is a type in its package and carries the responsibility, state, or contract for `Postgresql Assignment Lifecycle Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Owns due assignment rows until their state change and Outbox enqueue commit.
 */
@Repository
public class PostgresqlAssignmentLifecycleRepository
        implements AssignmentLifecycleRepository {

    /**
     * 字段 `ACTOR` 表示 `PostgresqlAssignmentLifecycleRepository` 中与 `ACTOR` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `ACTOR` stores the `ACTOR`-related state, dependency, configuration, or result of `PostgresqlAssignmentLifecycleRepository` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `ACTOR` 时应保持 `PostgresqlAssignmentLifecycleRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `ACTOR`, preserve `PostgresqlAssignmentLifecycleRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final String ACTOR = "rbac3-assignment-lifecycle-worker";

    /**
     * 字段 `entityManager` 表示 `PostgresqlAssignmentLifecycleRepository` 中与 `entity Manager` 相关的状态、依赖、配置或结果（声明类型 `EntityManager`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `entityManager` stores the `entity Manager`-related state, dependency, configuration, or result of `PostgresqlAssignmentLifecycleRepository` (declared type `EntityManager`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `entityManager` 时应保持 `PostgresqlAssignmentLifecycleRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `entityManager`, preserve `PostgresqlAssignmentLifecycleRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final EntityManager entityManager;

    /**
     * 构造器 `PostgresqlAssignmentLifecycleRepository` 用于创建并初始化 `PostgresqlAssignmentLifecycleRepository` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `PostgresqlAssignmentLifecycleRepository` creates and initializes `PostgresqlAssignmentLifecycleRepository`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `PostgresqlAssignmentLifecycleRepository` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `PostgresqlAssignmentLifecycleRepository`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param entityManager 输入参数 `entityManager`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public PostgresqlAssignmentLifecycleRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * 方法 `processDue` 按照 `PostgresqlAssignmentLifecycleRepository` 的职责处理输入，完成 `process Due` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `processDue` processes its inputs according to `PostgresqlAssignmentLifecycleRepository`'s responsibility, performs the `process Due` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `processDue` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `processDue`, then continue the business flow using its result, exception, or side effect.
     *
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param batchSize 输入参数 `batchSize`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param publisher 输入参数 `publisher`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional
    public int processDue(
            Instant now,
            int batchSize,
            ChangePublisher publisher) {
        List<DueAssignment> due = new ArrayList<>(batchSize);
        due.addAll(lockDue("PENDING", "valid_from <= :now", "ACTIVATED",
                now, batchSize));
        if (due.size() < batchSize) {
            due.addAll(lockDue("ACTIVE", "valid_to is not null and valid_to <= :now",
                    "EXPIRED", now, batchSize - due.size()));
        }
        if (due.size() < batchSize) {
            due.addAll(lockDue("SUSPENDED", "valid_to is not null and valid_to <= :now",
                    "EXPIRED", now, batchSize - due.size()));
        }
        for (DueAssignment candidate : due) {
            UserRoleAssignmentPO assignment = entityManager.find(
                    UserRoleAssignmentPO.class, candidate.assignmentId(),
                    LockModeType.PESSIMISTIC_WRITE);
            UserPO user = entityManager.find(
                    UserPO.class, assignment.getUserId(),
                    LockModeType.PESSIMISTIC_WRITE);
            long authVersion = user.advanceAuthorizationVersion(
                    user.getAuthVersion(), ACTOR, now);
            if ("ACTIVATED".equals(candidate.changeType())) {
                assignment.activate(ACTOR, now);
            } else {
                assignment.expire(ACTOR, now);
            }
            publisher.publish(new LifecycleChangeVO(
                    assignment.getTenantId().toString(),
                    assignment.getId().toString(),
                    assignment.getUserId().toString(),
                    candidate.changeType(),
                    authVersion));
        }
        entityManager.flush();
        return due.size();
    }

    /**
     * 方法 `lockDue` 按照 `PostgresqlAssignmentLifecycleRepository` 的职责处理输入，完成 `lock Due` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `lockDue` processes its inputs according to `PostgresqlAssignmentLifecycleRepository`'s responsibility, performs the `lock Due` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `lockDue` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `lockDue`, then continue the business flow using its result, exception, or side effect.
     *
     * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param timePredicate 输入参数 `timePredicate`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param changeType 输入参数 `changeType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param limit 输入参数 `limit`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @SuppressWarnings("unchecked")
    private List<DueAssignment> lockDue(
            String status,
            String timePredicate,
            String changeType,
            Instant now,
            int limit) {
        if (limit == 0) {
            return List.of();
        }
        String sql = """
                select id
                  from rbac3_user_role_assignment
                 where status = :status and %s
                 order by coalesce(valid_to, valid_from), id
                 for update skip locked
                 limit :batchSize
                """.formatted(timePredicate);
        List<Number> ids = entityManager.createNativeQuery(sql)
                .setParameter("status", status)
                .setParameter("now", now)
                .setParameter("batchSize", limit)
                .getResultList();
        return ids.stream()
                .map(Number::longValue)
                .map(id -> new DueAssignment(id, changeType))
                .toList();
    }

    }
