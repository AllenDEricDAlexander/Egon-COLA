package top.egon.cola.platform.rbac3.admin.runtime.service;

import jakarta.persistence.EntityManager;
import org.flywaydb.core.Flyway;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.config.flyway.Rbac3FlywayConfiguration;
import top.egon.cola.platform.rbac3.admin.runtime.service.ControlPlaneRuntimeStatusPort;
import top.egon.cola.platform.rbac3.admin.runtime.domain.po.AuthorizationMutationPO;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.OperationalStatusVO;
import top.egon.cola.platform.rbac3.admin.runtime.repository.internal.MutationFacts;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.FlywayStatusVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.RedisProjectionStatusVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.FenceMutationStatusVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.OutboxStatusVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.enums.AuthorizationMutationStatusEnum;

/**
 * 类型 `Rbac3OperationalRuntimeStatusService` 位于当前包内，是类型，用于承载 `Rbac3 Operational Runtime Status Service` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3OperationalRuntimeStatusService` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Operational Runtime Status Service`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * Produces independent health facts for RBAC3 persistence and projection subsystems.
 */
@Repository
public class Rbac3OperationalRuntimeStatusService {

    /**
     * 字段 `PENDING_MUTATIONS` 表示 `Rbac3OperationalRuntimeStatusService` 中与 `PENDING MUTATIONS` 相关的状态、依赖、配置或结果（声明类型 `List&lt;AuthorizationMutationStatusEnum&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `PENDING_MUTATIONS` stores the `PENDING MUTATIONS`-related state, dependency, configuration, or result of `Rbac3OperationalRuntimeStatusService` (declared type `List&lt;AuthorizationMutationStatusEnum&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `PENDING_MUTATIONS` 时应保持 `Rbac3OperationalRuntimeStatusService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `PENDING_MUTATIONS`, preserve `Rbac3OperationalRuntimeStatusService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final List<AuthorizationMutationStatusEnum> PENDING_MUTATIONS = List.of(
            AuthorizationMutationStatusEnum.PREPARING,
            AuthorizationMutationStatusEnum.COMMITTED,
            AuthorizationMutationStatusEnum.PROJECTED,
            AuthorizationMutationStatusEnum.RECOVERY_REQUIRED);

    /**
     * 字段 `entityManager` 表示 `Rbac3OperationalRuntimeStatusService` 中与 `entity Manager` 相关的状态、依赖、配置或结果（声明类型 `EntityManager`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `entityManager` stores the `entity Manager`-related state, dependency, configuration, or result of `Rbac3OperationalRuntimeStatusService` (declared type `EntityManager`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `entityManager` 时应保持 `Rbac3OperationalRuntimeStatusService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `entityManager`, preserve `Rbac3OperationalRuntimeStatusService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final EntityManager entityManager;
    /**
     * 字段 `rbac3Flyway` 表示 `Rbac3OperationalRuntimeStatusService` 中与 `rbac3 Flyway` 相关的状态、依赖、配置或结果（声明类型 `Flyway`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `rbac3Flyway` stores the `rbac3 Flyway`-related state, dependency, configuration, or result of `Rbac3OperationalRuntimeStatusService` (declared type `Flyway`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `rbac3Flyway` 时应保持 `Rbac3OperationalRuntimeStatusService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `rbac3Flyway`, preserve `Rbac3OperationalRuntimeStatusService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Flyway rbac3Flyway;
    /**
     * 字段 `outboxFlyway` 表示 `Rbac3OperationalRuntimeStatusService` 中与 `outbox Flyway` 相关的状态、依赖、配置或结果（声明类型 `Flyway`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `outboxFlyway` stores the `outbox Flyway`-related state, dependency, configuration, or result of `Rbac3OperationalRuntimeStatusService` (declared type `Flyway`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `outboxFlyway` 时应保持 `Rbac3OperationalRuntimeStatusService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `outboxFlyway`, preserve `Rbac3OperationalRuntimeStatusService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Flyway outboxFlyway;
    /**
     * 字段 `redisson` 表示 `Rbac3OperationalRuntimeStatusService` 中与 `redisson` 相关的状态、依赖、配置或结果（声明类型 `RedissonClient`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `redisson` stores the `redisson`-related state, dependency, configuration, or result of `Rbac3OperationalRuntimeStatusService` (declared type `RedissonClient`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `redisson` 时应保持 `Rbac3OperationalRuntimeStatusService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `redisson`, preserve `Rbac3OperationalRuntimeStatusService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RedissonClient redisson;
    /**
     * 字段 `clock` 表示 `Rbac3OperationalRuntimeStatusService` 中与 `clock` 相关的状态、依赖、配置或结果（声明类型 `Clock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `clock` stores the `clock`-related state, dependency, configuration, or result of `Rbac3OperationalRuntimeStatusService` (declared type `Clock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `clock` 时应保持 `Rbac3OperationalRuntimeStatusService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `clock`, preserve `Rbac3OperationalRuntimeStatusService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Clock clock;

    /**
     * 构造器 `Rbac3OperationalRuntimeStatusService` 用于创建并初始化 `Rbac3OperationalRuntimeStatusService` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `Rbac3OperationalRuntimeStatusService` creates and initializes `Rbac3OperationalRuntimeStatusService`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `Rbac3OperationalRuntimeStatusService` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `Rbac3OperationalRuntimeStatusService`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param entityManager 输入参数 `entityManager`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param rbac3Flyway 输入参数 `rbac3Flyway`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param outboxFlyway 输入参数 `outboxFlyway`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param redisson 输入参数 `redisson`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public Rbac3OperationalRuntimeStatusService(
            EntityManager entityManager,
            @Qualifier(Rbac3FlywayConfiguration.RBAC3_FLYWAY) Flyway rbac3Flyway,
            @Qualifier(Rbac3FlywayConfiguration.OUTBOX_FLYWAY) Flyway outboxFlyway,
            @Qualifier("rbac3RuntimeRedissonClient") RedissonClient redisson,
            Clock clock) {
        this.entityManager = entityManager;
        this.rbac3Flyway = rbac3Flyway;
        this.outboxFlyway = outboxFlyway;
        this.redisson = redisson;
        this.clock = clock;
    }

    /**
     * 方法 `status` 按照 `Rbac3OperationalRuntimeStatusService` 的职责处理输入，完成 `status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `status` processes its inputs according to `Rbac3OperationalRuntimeStatusService`'s responsibility, performs the `status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `status` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `status`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Transactional(readOnly = true)
    public OperationalStatusVO status() {
        MutationFacts mutations = mutationFacts();
        return new OperationalStatusVO(
                new FlywayStatusVO(
                        flywayState(rbac3Flyway), flywayState(outboxFlyway)),
                redisStatus(mutations.projectionLag()),
                new FenceMutationStatusVO(
                        mutationState(mutations), mutations.pendingCount(),
                        mutations.recoveryRequiredCount(), mutations.oldestAgeSeconds()),
                outboxStatus());
    }

    /**
     * 方法 `flywayState` 按照 `Rbac3OperationalRuntimeStatusService` 的职责处理输入，完成 `flyway State` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `flywayState` processes its inputs according to `Rbac3OperationalRuntimeStatusService`'s responsibility, performs the `flyway State` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `flywayState` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `flywayState`, then continue the business flow using its result, exception, or side effect.
     *
     * @param flyway 输入参数 `flyway`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private String flywayState(Flyway flyway) {
        try {
            return flyway.info().pending().length == 0 && flyway.info().current() != null
                    ? "UP_TO_DATE" : "PENDING";
        } catch (RuntimeException unavailable) {
            return "UNAVAILABLE";
        }
    }

    /**
     * 方法 `redisStatus` 按照 `Rbac3OperationalRuntimeStatusService` 的职责处理输入，完成 `redis Status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `redisStatus` processes its inputs according to `Rbac3OperationalRuntimeStatusService`'s responsibility, performs the `redis Status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `redisStatus` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `redisStatus`, then continue the business flow using its result, exception, or side effect.
     *
     * @param lag 输入参数 `lag`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private RedisProjectionStatusVO redisStatus(long lag) {
        try {
            redisson.getKeys().count();
            return new RedisProjectionStatusVO(
                    lag == 0 ? "HEALTHY" : "LAGGING", lag);
        } catch (RuntimeException unavailable) {
            return new RedisProjectionStatusVO("UNAVAILABLE", lag);
        }
    }

    /**
     * 方法 `mutationFacts` 按照 `Rbac3OperationalRuntimeStatusService` 的职责处理输入，完成 `mutation Facts` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `mutationFacts` processes its inputs according to `Rbac3OperationalRuntimeStatusService`'s responsibility, performs the `mutation Facts` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `mutationFacts` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `mutationFacts`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private MutationFacts mutationFacts() {
        try {
            long pending = entityManager.createQuery("""
                            select count(m) from AuthorizationMutationEntity m
                             where m.status in :statuses
                            """, Long.class)
                    .setParameter("statuses", PENDING_MUTATIONS)
                    .getSingleResult();
            long recovery = entityManager.createQuery("""
                            select count(m) from AuthorizationMutationEntity m
                             where m.status = :status
                            """, Long.class)
                    .setParameter("status", AuthorizationMutationStatusEnum.RECOVERY_REQUIRED)
                    .getSingleResult();
            Instant oldest = entityManager.createQuery("""
                            select min(m.updatedAt) from AuthorizationMutationEntity m
                             where m.status in :statuses
                            """, Instant.class)
                    .setParameter("statuses", PENDING_MUTATIONS)
                    .getSingleResult();
            long projectionLag = entityManager.createQuery("""
                            select count(m) from AuthorizationMutationEntity m
                             where m.status in :statuses
                            """, Long.class)
                    .setParameter("statuses", List.of(
                            AuthorizationMutationStatusEnum.COMMITTED,
                            AuthorizationMutationStatusEnum.RECOVERY_REQUIRED))
                    .getSingleResult();
            return new MutationFacts(pending, recovery, ageSeconds(oldest), projectionLag, true);
        } catch (RuntimeException unavailable) {
            return new MutationFacts(0L, 0L, 0L, 0L, false);
        }
    }

    /**
     * 方法 `mutationState` 按照 `Rbac3OperationalRuntimeStatusService` 的职责处理输入，完成 `mutation State` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `mutationState` processes its inputs according to `Rbac3OperationalRuntimeStatusService`'s responsibility, performs the `mutation State` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `mutationState` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `mutationState`, then continue the business flow using its result, exception, or side effect.
     *
     * @param facts 输入参数 `facts`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private String mutationState(MutationFacts facts) {
        if (!facts.available()) {
            return "UNAVAILABLE";
        }
        if (facts.recoveryRequiredCount() > 0) {
            return "DEGRADED";
        }
        return facts.pendingCount() == 0 ? "HEALTHY" : "ACTIVE";
    }

    /**
     * 方法 `outboxStatus` 按照 `Rbac3OperationalRuntimeStatusService` 的职责处理输入，完成 `outbox Status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `outboxStatus` processes its inputs according to `Rbac3OperationalRuntimeStatusService`'s responsibility, performs the `outbox Status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `outboxStatus` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `outboxStatus`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private OutboxStatusVO outboxStatus() {
        try {
            long pending = ((Number) entityManager.createNativeQuery("""
                            select count(*) from egon_cola_outbox_message
                             where status in ('PENDING', 'PROCESSING', 'RETRY_WAIT')
                            """)
                    .getSingleResult()).longValue();
            long dead = ((Number) entityManager.createNativeQuery("""
                            select count(*) from egon_cola_outbox_message where status = 'DEAD'
                            """)
                    .getSingleResult()).longValue();
            Object oldestValue = entityManager.createNativeQuery("""
                            select min(created_at) from egon_cola_outbox_message
                             where status in ('PENDING', 'PROCESSING', 'RETRY_WAIT')
                            """)
                    .getSingleResult();
            Instant oldest = oldestValue instanceof Instant instant
                    ? instant
                    : oldestValue instanceof OffsetDateTime value ? value.toInstant() : null;
            String state = dead > 0 ? "DEGRADED" : pending > 0 ? "LAGGING" : "HEALTHY";
            return new OutboxStatusVO(
                    state, pending, ageSeconds(oldest));
        } catch (RuntimeException unavailable) {
            return new OutboxStatusVO("UNAVAILABLE", 0L, 0L);
        }
    }

    /**
     * 方法 `ageSeconds` 按照 `Rbac3OperationalRuntimeStatusService` 的职责处理输入，完成 `age Seconds` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `ageSeconds` processes its inputs according to `Rbac3OperationalRuntimeStatusService`'s responsibility, performs the `age Seconds` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `ageSeconds` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `ageSeconds`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private long ageSeconds(Instant value) {
        if (value == null) {
            return 0L;
        }
        return Math.max(0L, Duration.between(value, clock.instant()).toSeconds());
    }


    }
