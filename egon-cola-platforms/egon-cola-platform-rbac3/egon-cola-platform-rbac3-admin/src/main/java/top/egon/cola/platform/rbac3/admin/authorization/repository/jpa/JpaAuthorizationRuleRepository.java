package top.egon.cola.platform.rbac3.admin.authorization.repository.jpa;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.constraint.domain.po.OperationSodRulePO;
import top.egon.cola.platform.rbac3.admin.participation.service.ParticipationFacade;

import java.time.Instant;
import java.util.List;
import top.egon.cola.platform.rbac3.admin.constraint.domain.enums.OperationSodRuleStatusEnum;
import top.egon.cola.platform.rbac3.admin.participation.repository.OperationSodRuleRepository;
import top.egon.cola.platform.rbac3.admin.participation.domain.vo.PriorActionRuleVO;

/**
 * 类型 `JpaAuthorizationRuleRepository` 位于当前包内，是类型，用于承载 `Authorization Rule Repository` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `JpaAuthorizationRuleRepository` is a type in its package and carries the responsibility, state, or contract for `Authorization Rule Repository`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Reads active typed authorization constraints without evaluating arbitrary expressions.
 */
@Repository
public class JpaAuthorizationRuleRepository
        implements OperationSodRuleRepository {

    /**
     * 字段 `entityManager` 表示 `JpaAuthorizationRuleRepository` 中与 `entity Manager` 相关的状态、依赖、配置或结果（声明类型 `EntityManager`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `entityManager` stores the `entity Manager`-related state, dependency, configuration, or result of `JpaAuthorizationRuleRepository` (declared type `EntityManager`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `entityManager` 时应保持 `JpaAuthorizationRuleRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `entityManager`, preserve `JpaAuthorizationRuleRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final EntityManager entityManager;

    /**
     * 构造器 `JpaAuthorizationRuleRepository` 用于创建并初始化 `JpaAuthorizationRuleRepository` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `JpaAuthorizationRuleRepository` creates and initializes `JpaAuthorizationRuleRepository`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `JpaAuthorizationRuleRepository` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `JpaAuthorizationRuleRepository`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param entityManager 输入参数 `entityManager`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public JpaAuthorizationRuleRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * 方法 `rules` 按照 `JpaAuthorizationRuleRepository` 的职责处理输入，完成 `rules` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rules` processes its inputs according to `JpaAuthorizationRuleRepository`'s responsibility, performs the `rules` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rules` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rules`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationCode 输入参数 `applicationCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param businessResource 输入参数 `businessResource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param laterAction 输入参数 `laterAction`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param at 输入参数 `at`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional(readOnly = true)
    public List<PriorActionRuleVO> rules(
            String tenantId,
            String applicationCode,
            String businessResource,
            String laterAction,
            Instant at) {
        return entityManager.createQuery("""
                        select r.id, r.priorActionCode, r.lookbackFrom
                          from OperationSodRuleEntity r
                         where r.tenantId = :tenantId
                           and r.applicationCode = :applicationCode
                           and r.businessResource = :businessResource
                           and r.forbiddenLaterActionCode = :laterAction
                           and r.status = :status
                           and r.validFrom <= :at
                           and (r.validTo is null or r.validTo > :at)
                         order by r.id
                        """, Object[].class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("applicationCode", applicationCode)
                .setParameter("businessResource", businessResource)
                .setParameter("laterAction", laterAction)
                .setParameter("status", OperationSodRuleStatusEnum.ACTIVE)
                .setParameter("at", at)
                .getResultList().stream()
                .map(row -> new PriorActionRuleVO(
                        row[0].toString(), row[1].toString(),
                        row[2] == null ? Instant.EPOCH : (Instant) row[2]))
                .toList();
    }
}
