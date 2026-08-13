package top.egon.cola.platform.rbac3.admin.runtime.repository.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.runtime.domain.po.IdempotencyRecordPO;

import java.time.Instant;
import java.util.List;
import top.egon.cola.platform.rbac3.admin.runtime.repository.IdempotencyRepository;
import top.egon.cola.platform.rbac3.admin.runtime.domain.dto.StoredCommandDTO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.IdempotencyClaimVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.enums.IdempotencyOutcomeEnum;
import top.egon.cola.platform.rbac3.admin.runtime.domain.enums.IdempotencyRecordActorTypeEnum;
import top.egon.cola.platform.rbac3.admin.runtime.domain.enums.IdempotencyRecordStatusEnum;

/**
 * 类型 `JpaIdempotencyRepository` 位于当前包内，是类型，用于承载 `Idempotency Repository` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `JpaIdempotencyRepository` is a type in its package and carries the responsibility, state, or contract for `Idempotency Repository`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `JpaIdempotencyRepository` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `JpaIdempotencyRepository` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Repository
public class JpaIdempotencyRepository implements IdempotencyRepository {

    /**
     * 字段 `entityManager` 表示 `JpaIdempotencyRepository` 中与 `entity Manager` 相关的状态、依赖、配置或结果（声明类型 `EntityManager`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `entityManager` stores the `entity Manager`-related state, dependency, configuration, or result of `JpaIdempotencyRepository` (declared type `EntityManager`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `entityManager` 时应保持 `JpaIdempotencyRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `entityManager`, preserve `JpaIdempotencyRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final EntityManager entityManager;
    /**
     * 字段 `idGenerator` 表示 `JpaIdempotencyRepository` 中与 `id Generator` 相关的状态、依赖、配置或结果（声明类型 `LongIdGenerator`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `idGenerator` stores the `id Generator`-related state, dependency, configuration, or result of `JpaIdempotencyRepository` (declared type `LongIdGenerator`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `idGenerator` 时应保持 `JpaIdempotencyRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `idGenerator`, preserve `JpaIdempotencyRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final LongIdGenerator idGenerator;

    /**
     * 构造器 `JpaIdempotencyRepository` 用于创建并初始化 `JpaIdempotencyRepository` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `JpaIdempotencyRepository` creates and initializes `JpaIdempotencyRepository`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `JpaIdempotencyRepository` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `JpaIdempotencyRepository`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param entityManager 输入参数 `entityManager`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idGenerator 输入参数 `idGenerator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public JpaIdempotencyRepository(
            EntityManager entityManager,
            LongIdGenerator idGenerator
    ) {
        this.entityManager = entityManager;
        this.idGenerator = idGenerator;
    }

    /**
     * 方法 `claim` 按照 `JpaIdempotencyRepository` 的职责处理输入，完成 `claim` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `claim` processes its inputs according to `JpaIdempotencyRepository`'s responsibility, performs the `claim` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `claim` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `claim`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional
    public IdempotencyClaimVO claim(StoredCommandDTO command) {
        List<IdempotencyRecordPO> records = entityManager.createQuery("""
                        select r from IdempotencyRecordEntity r
                         where r.tenantId = :tenantId and r.actorType = :actorType
                           and r.actorId = :actorId
                           and r.operationCode = :operationCode
                           and r.keyHash = :keyHash
                        """, IdempotencyRecordPO.class)
                .setParameter("tenantId", Long.valueOf(command.tenantId()))
                .setParameter("actorType",
                        IdempotencyRecordActorTypeEnum.valueOf(command.actorType()))
                .setParameter("actorId", command.actorId())
                .setParameter("operationCode", command.operationCode())
                .setParameter("keyHash", command.keyHash())
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        if (!records.isEmpty()) {
            IdempotencyRecordPO record = records.getFirst();
            if (!record.getRequestHash().equals(command.requestHash())) {
                return claim(record, IdempotencyOutcomeEnum.CONFLICT);
            }
            return claim(record,
                    record.getStatus() == IdempotencyRecordStatusEnum.COMPLETED
                            ? IdempotencyOutcomeEnum.REPLAY
                            : IdempotencyOutcomeEnum.IN_PROGRESS);
        }
        Long id = idGenerator.nextLongId();
        entityManager.persist(new IdempotencyRecordPO(
                id, Long.valueOf(command.tenantId()),
                IdempotencyRecordActorTypeEnum.valueOf(command.actorType()),
                command.actorId(), command.operationCode(), command.keyHash(),
                command.requestHash(), command.expiresAt(), command.now()));
        return new IdempotencyClaimVO(
                id.toString(), IdempotencyOutcomeEnum.CLAIMED,
                null, null, null);
    }

    /**
     * 方法 `complete` 按照 `JpaIdempotencyRepository` 的职责处理输入，完成 `complete` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `complete` processes its inputs according to `JpaIdempotencyRepository`'s responsibility, performs the `complete` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `complete` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `complete`, then continue the business flow using its result, exception, or side effect.
     *
     * @param recordId 输入参数 `recordId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param resourceType 输入参数 `resourceType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param resourceId 输入参数 `resourceId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param responseStatus 输入参数 `responseStatus`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param responseDigest 输入参数 `responseDigest`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    @Override
    @Transactional
    public void complete(
            String recordId,
            String resourceType,
            String resourceId,
            int responseStatus,
            String responseDigest,
            Instant now
    ) {
        IdempotencyRecordPO record = entityManager.find(
                IdempotencyRecordPO.class, Long.valueOf(recordId),
                LockModeType.PESSIMISTIC_WRITE);
        if (record == null) {
            throw new IllegalStateException("idempotency record is missing");
        }
        record.complete(
                resourceType, resourceId, responseStatus, responseDigest, now);
    }

    /**
     * 方法 `claim` 按照 `JpaIdempotencyRepository` 的职责处理输入，完成 `claim` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `claim` processes its inputs according to `JpaIdempotencyRepository`'s responsibility, performs the `claim` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `claim` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `claim`, then continue the business flow using its result, exception, or side effect.
     *
     * @param record 输入参数 `record`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param outcome 输入参数 `outcome`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private IdempotencyClaimVO claim(
            IdempotencyRecordPO record,
            IdempotencyOutcomeEnum outcome
    ) {
        return new IdempotencyClaimVO(
                Long.toString((Long) entityManager.getEntityManagerFactory()
                        .getPersistenceUnitUtil().getIdentifier(record)),
                outcome, record.getResourceId(), record.getResponseStatus(),
                record.getResponseDigest());
    }
}
