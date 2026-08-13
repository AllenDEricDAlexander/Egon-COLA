package top.egon.cola.platform.rbac3.admin.participation.infrastructure;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.application.port.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.participation.application.ParticipationFacade;
import top.egon.cola.platform.rbac3.admin.participation.domain.BusinessParticipationEntity;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.List;

/**
 * 类型 `PostgresqlParticipationStore` 位于当前包内，是类型，用于承载 `Postgresql Participation Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `PostgresqlParticipationStore` is a type in its package and carries the responsibility, state, or contract for `Postgresql Participation Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Serializes same-object checks and append through a PostgreSQL transaction lock.
 */
@Repository
public class PostgresqlParticipationStore
        implements ParticipationFacade.ParticipationStore {

    /**
     * 字段 `entityManager` 表示 `PostgresqlParticipationStore` 中与 `entity Manager` 相关的状态、依赖、配置或结果（声明类型 `EntityManager`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `entityManager` stores the `entity Manager`-related state, dependency, configuration, or result of `PostgresqlParticipationStore` (declared type `EntityManager`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `entityManager` 时应保持 `PostgresqlParticipationStore` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `entityManager`, preserve `PostgresqlParticipationStore`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final EntityManager entityManager;
    /**
     * 字段 `idGenerator` 表示 `PostgresqlParticipationStore` 中与 `id Generator` 相关的状态、依赖、配置或结果（声明类型 `LongIdGenerator`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `idGenerator` stores the `id Generator`-related state, dependency, configuration, or result of `PostgresqlParticipationStore` (declared type `LongIdGenerator`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `idGenerator` 时应保持 `PostgresqlParticipationStore` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `idGenerator`, preserve `PostgresqlParticipationStore`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final LongIdGenerator idGenerator;
    /**
     * 字段 `databaseClock` 表示 `PostgresqlParticipationStore` 中与 `database Clock` 相关的状态、依赖、配置或结果（声明类型 `DatabaseClock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `databaseClock` stores the `database Clock`-related state, dependency, configuration, or result of `PostgresqlParticipationStore` (declared type `DatabaseClock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `databaseClock` 时应保持 `PostgresqlParticipationStore` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `databaseClock`, preserve `PostgresqlParticipationStore`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final DatabaseClock databaseClock;

    /**
     * 构造器 `PostgresqlParticipationStore` 用于创建并初始化 `PostgresqlParticipationStore` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `PostgresqlParticipationStore` creates and initializes `PostgresqlParticipationStore`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `PostgresqlParticipationStore` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `PostgresqlParticipationStore`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param entityManager 输入参数 `entityManager`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idGenerator 输入参数 `idGenerator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param databaseClock 输入参数 `databaseClock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public PostgresqlParticipationStore(
            EntityManager entityManager,
            LongIdGenerator idGenerator,
            DatabaseClock databaseClock) {
        this.entityManager = entityManager;
        this.idGenerator = idGenerator;
        this.databaseClock = databaseClock;
    }

    /**
     * 方法 `appendAtomically` 按照 `PostgresqlParticipationStore` 的职责处理输入，完成 `append Atomically` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `appendAtomically` processes its inputs according to `PostgresqlParticipationStore`'s responsibility, performs the `append Atomically` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `appendAtomically` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `appendAtomically`, then continue the business flow using its result, exception, or side effect.
     *
     * @param record 输入参数 `record`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param rules 输入参数 `rules`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional
    public ParticipationFacade.AppendResult appendAtomically(
            ParticipationFacade.ParticipationRecord record,
            List<ParticipationFacade.PriorActionRule> rules) {
        lock(record);
        BusinessParticipationEntity existing = existing(record);
        if (existing != null) {
            if (!existing.getPayloadDigest().equals(record.payloadDigest())) {
                throw new Rbac3RuleViolation("IDEMPOTENCY_CONFLICT");
            }
            return new ParticipationFacade.AppendResult(
                    false, existing.getId().toString(), List.of());
        }
        List<String> conflicts = objectFacts(record).stream()
                .filter(fact -> rules.stream().anyMatch(rule ->
                        rule.actionCode().equals(fact.getActionCode())
                                && !fact.getOccurredAt().isBefore(rule.lookbackFrom())))
                .map(fact -> fact.getId().toString())
                .toList();
        if (!conflicts.isEmpty()) {
            return new ParticipationFacade.AppendResult(false, null, conflicts);
        }
        Long id = idGenerator.nextLongId();
        entityManager.persist(new BusinessParticipationEntity(
                id, Long.valueOf(record.tenantId()), record.applicationCode(),
                record.businessResource(), record.businessId(),
                Long.valueOf(record.actorUserId()), record.actionCode(),
                record.businessEventId(), record.occurredAt(), record.traceId(),
                record.payloadDigest(), databaseClock.transactionNow(),
                record.applicationCode()));
        entityManager.flush();
        return new ParticipationFacade.AppendResult(true, id.toString(), List.of());
    }

    /**
     * 方法 `find` 按照 `PostgresqlParticipationStore` 的职责处理输入，完成 `find` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `find` processes its inputs according to `PostgresqlParticipationStore`'s responsibility, performs the `find` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `find` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `find`, then continue the business flow using its result, exception, or side effect.
     *
     * @param query 输入参数 `query`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param lookbackFrom 输入参数 `lookbackFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ParticipationFacade.ParticipationFact> find(
            ParticipationFacade.ConflictQuery query,
            String tenantId,
            Instant lookbackFrom) {
        return entityManager.createQuery("""
                        select p from BusinessParticipationEntity p
                         where p.tenantId = :tenantId
                           and p.applicationCode = :applicationCode
                           and p.businessResource = :businessResource
                           and p.businessId = :businessId
                           and p.actorUserId = :actorUserId
                           and p.occurredAt >= :lookbackFrom
                         order by p.occurredAt, p.id
                        """, BusinessParticipationEntity.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("applicationCode", query.applicationCode())
                .setParameter("businessResource", query.businessResource())
                .setParameter("businessId", query.businessId())
                .setParameter("actorUserId", Long.valueOf(query.actorUserId()))
                .setParameter("lookbackFrom", lookbackFrom)
                .getResultList().stream()
                .map(this::toFact)
                .toList();
    }

    /**
     * 方法 `lock` 按照 `PostgresqlParticipationStore` 的职责处理输入，完成 `lock` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `lock` processes its inputs according to `PostgresqlParticipationStore`'s responsibility, performs the `lock` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `lock` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `lock`, then continue the business flow using its result, exception, or side effect.
     *
     * @param record 输入参数 `record`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void lock(ParticipationFacade.ParticipationRecord record) {
        String lockKey = String.join("\u001f",
                record.tenantId(), record.applicationCode(), record.businessResource(),
                record.businessId(), record.actorUserId());
        entityManager.createNativeQuery(
                        "select pg_advisory_xact_lock(hashtextextended(cast(?1 as text), 0))")
                .setParameter(1, lockKey)
                .getSingleResult();
    }

    /**
     * 方法 `existing` 按照 `PostgresqlParticipationStore` 的职责处理输入，完成 `existing` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `existing` processes its inputs according to `PostgresqlParticipationStore`'s responsibility, performs the `existing` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `existing` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `existing`, then continue the business flow using its result, exception, or side effect.
     *
     * @param record 输入参数 `record`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private BusinessParticipationEntity existing(
            ParticipationFacade.ParticipationRecord record) {
        return entityManager.createQuery("""
                        select p from BusinessParticipationEntity p
                         where p.tenantId = :tenantId
                           and p.applicationCode = :applicationCode
                           and p.businessEventId = :eventId
                        """, BusinessParticipationEntity.class)
                .setParameter("tenantId", Long.valueOf(record.tenantId()))
                .setParameter("applicationCode", record.applicationCode())
                .setParameter("eventId", record.businessEventId())
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    /**
     * 方法 `objectFacts` 按照 `PostgresqlParticipationStore` 的职责处理输入，完成 `object Facts` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `objectFacts` processes its inputs according to `PostgresqlParticipationStore`'s responsibility, performs the `object Facts` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `objectFacts` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `objectFacts`, then continue the business flow using its result, exception, or side effect.
     *
     * @param record 输入参数 `record`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private List<BusinessParticipationEntity> objectFacts(
            ParticipationFacade.ParticipationRecord record) {
        return entityManager.createQuery("""
                        select p from BusinessParticipationEntity p
                         where p.tenantId = :tenantId
                           and p.applicationCode = :applicationCode
                           and p.businessResource = :businessResource
                           and p.businessId = :businessId
                           and p.actorUserId = :actorUserId
                        """, BusinessParticipationEntity.class)
                .setParameter("tenantId", Long.valueOf(record.tenantId()))
                .setParameter("applicationCode", record.applicationCode())
                .setParameter("businessResource", record.businessResource())
                .setParameter("businessId", record.businessId())
                .setParameter("actorUserId", Long.valueOf(record.actorUserId()))
                .getResultList();
    }

    /**
     * 方法 `toFact` 按照 `PostgresqlParticipationStore` 的职责处理输入，完成 `to Fact` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `toFact` processes its inputs according to `PostgresqlParticipationStore`'s responsibility, performs the `to Fact` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `toFact` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `toFact`, then continue the business flow using its result, exception, or side effect.
     *
     * @param entity 输入参数 `entity`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private ParticipationFacade.ParticipationFact toFact(
            BusinessParticipationEntity entity) {
        return new ParticipationFacade.ParticipationFact(
                entity.getId().toString(), entity.getTenantId().toString(),
                entity.getApplicationCode(), entity.getBusinessResource(),
                entity.getBusinessId(), entity.getActorUserId().toString(),
                entity.getActionCode(), entity.getBusinessEventId(),
                entity.getOccurredAt());
    }
}
