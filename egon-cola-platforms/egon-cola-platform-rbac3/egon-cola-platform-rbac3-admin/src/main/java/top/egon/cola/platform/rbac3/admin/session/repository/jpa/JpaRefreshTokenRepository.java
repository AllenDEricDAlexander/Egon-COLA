package top.egon.cola.platform.rbac3.admin.session.repository.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.session.service.RefreshTokenService;
import top.egon.cola.platform.rbac3.admin.session.service.SessionRuntimeSynchronizer;
import top.egon.cola.platform.rbac3.admin.session.service.SessionSecurityEventRecorder;
import top.egon.cola.platform.rbac3.admin.session.domain.po.RefreshTokenPO;
import top.egon.cola.platform.rbac3.admin.tenant.domain.po.TenantPO;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;
import top.egon.cola.platform.rbac3.admin.tenant.domain.enums.TenantStatusEnum;
import top.egon.cola.platform.rbac3.admin.session.repository.RefreshTokenRepository;
import top.egon.cola.platform.rbac3.admin.session.domain.vo.TokenRecordVO;
import top.egon.cola.platform.rbac3.admin.session.domain.enums.RefreshTokenTokenStatusEnum;
import top.egon.cola.platform.rbac3.admin.session.domain.vo.TerminationVO;
import top.egon.cola.platform.rbac3.admin.session.domain.enums.RefreshTokenStatusEnum;

/**
 * 类型 `JpaRefreshTokenRepository` 位于当前包内，是类型，用于承载 `Refresh Token Repository` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `JpaRefreshTokenRepository` is a type in its package and carries the responsibility, state, or contract for `Refresh Token Repository`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * PostgreSQL implementation of the refresh-token atomic lock boundary.
 */
@Repository
public class JpaRefreshTokenRepository implements RefreshTokenRepository {

    /**
     * 字段 `entityManager` 表示 `JpaRefreshTokenRepository` 中与 `entity Manager` 相关的状态、依赖、配置或结果（声明类型 `EntityManager`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `entityManager` stores the `entity Manager`-related state, dependency, configuration, or result of `JpaRefreshTokenRepository` (declared type `EntityManager`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `entityManager` 时应保持 `JpaRefreshTokenRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `entityManager`, preserve `JpaRefreshTokenRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final EntityManager entityManager;
    /**
     * 字段 `sessionRepository` 表示 `JpaRefreshTokenRepository` 中与 `session Repository` 相关的状态、依赖、配置或结果（声明类型 `SessionRepository`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `sessionRepository` stores the `session Repository`-related state, dependency, configuration, or result of `JpaRefreshTokenRepository` (declared type `SessionRepository`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `sessionRepository` 时应保持 `JpaRefreshTokenRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `sessionRepository`, preserve `JpaRefreshTokenRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final JpaSessionEntityRepository sessionRepository;
    /**
     * 字段 `idGenerator` 表示 `JpaRefreshTokenRepository` 中与 `id Generator` 相关的状态、依赖、配置或结果（声明类型 `LongIdGenerator`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `idGenerator` stores the `id Generator`-related state, dependency, configuration, or result of `JpaRefreshTokenRepository` (declared type `LongIdGenerator`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `idGenerator` 时应保持 `JpaRefreshTokenRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `idGenerator`, preserve `JpaRefreshTokenRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final LongIdGenerator idGenerator;
    /**
     * 字段 `runtimeSynchronizer` 表示 `JpaRefreshTokenRepository` 中与 `runtime Synchronizer` 相关的状态、依赖、配置或结果（声明类型 `SessionRuntimeSynchronizer`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `runtimeSynchronizer` stores the `runtime Synchronizer`-related state, dependency, configuration, or result of `JpaRefreshTokenRepository` (declared type `SessionRuntimeSynchronizer`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `runtimeSynchronizer` 时应保持 `JpaRefreshTokenRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `runtimeSynchronizer`, preserve `JpaRefreshTokenRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final SessionRuntimeSynchronizer runtimeSynchronizer;
    /**
     * 字段 `securityEventRecorder` 表示 `JpaRefreshTokenRepository` 中与 `security Event Recorder` 相关的状态、依赖、配置或结果（声明类型 `SessionSecurityEventRecorder`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `securityEventRecorder` stores the `security Event Recorder`-related state, dependency, configuration, or result of `JpaRefreshTokenRepository` (declared type `SessionSecurityEventRecorder`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `securityEventRecorder` 时应保持 `JpaRefreshTokenRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `securityEventRecorder`, preserve `JpaRefreshTokenRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final SessionSecurityEventRecorder securityEventRecorder;

    /**
     * 构造器 `JpaRefreshTokenRepository` 用于创建并初始化 `JpaRefreshTokenRepository` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `JpaRefreshTokenRepository` creates and initializes `JpaRefreshTokenRepository`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `JpaRefreshTokenRepository` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `JpaRefreshTokenRepository`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param entityManager 输入参数 `entityManager`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionRepository 输入参数 `sessionRepository`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idGenerator 输入参数 `idGenerator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param runtimeSynchronizer 输入参数 `runtimeSynchronizer`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param securityEventRecorder 输入参数 `securityEventRecorder`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public JpaRefreshTokenRepository(
            EntityManager entityManager,
            JpaSessionEntityRepository sessionRepository,
            LongIdGenerator idGenerator,
            SessionRuntimeSynchronizer runtimeSynchronizer,
            SessionSecurityEventRecorder securityEventRecorder) {
        this.entityManager = entityManager;
        this.sessionRepository = sessionRepository;
        this.idGenerator = idGenerator;
        this.runtimeSynchronizer = runtimeSynchronizer;
        this.securityEventRecorder = securityEventRecorder;
    }

    /**
     * 方法 `withLockedToken` 按照 `JpaRefreshTokenRepository` 的职责处理输入，完成 `with Locked Token` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `withLockedToken` processes its inputs according to `JpaRefreshTokenRepository`'s responsibility, performs the `with Locked Token` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `withLockedToken` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `withLockedToken`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tokenHash 输入参数 `tokenHash`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param action 输入参数 `action`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional
    public <T> T withLockedToken(
            String tokenHash,
            Function<TokenRecordVO, T> action) {
        RefreshTokenPO entity = findByHash(tokenHash, LockModeType.PESSIMISTIC_WRITE);
        return action.apply(entity == null ? null : toRecord(entity));
    }

    /**
     * 方法 `rotate` 按照 `JpaRefreshTokenRepository` 的职责处理输入，完成 `rotate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rotate` processes its inputs according to `JpaRefreshTokenRepository`'s responsibility, performs the `rotate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rotate` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rotate`, then continue the business flow using its result, exception, or side effect.
     *
     * @param oldToken 输入参数 `oldToken`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param newToken 输入参数 `newToken`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    @Override
    public void rotate(
            TokenRecordVO oldToken,
            TokenRecordVO newToken) {
        RefreshTokenPO current = findByHash(
                oldToken.tokenHash(), LockModeType.PESSIMISTIC_WRITE);
        if (current == null) {
            throw new IllegalStateException("locked refresh token disappeared");
        }
        var session = sessionRepository.lockByTenantIdAndSessionId(
                        current.getTenantId(), current.getSessionId())
                .orElseThrow(() -> new IllegalStateException("refresh session is missing"));
        TenantPO tenant = entityManager.find(
                TenantPO.class, current.getTenantId(), LockModeType.PESSIMISTIC_READ);
        if (tenant == null || tenant.getStatus() != TenantStatusEnum.ACTIVE) {
            throw new IllegalStateException("refresh tenant is unavailable");
        }
        session.refresh(
                tenant.getPolicyVersion(),
                oldToken.rotatedAt(),
                oldToken.rotatedAt().plusSeconds(30 * 60L),
                "refresh");
        long nextId = idGenerator.nextLongId();
        RefreshTokenPO replacement = new RefreshTokenPO(
                nextId,
                Long.valueOf(newToken.tenantId()),
                Long.valueOf(newToken.sessionId()),
                newToken.familyId(),
                newToken.generation(),
                newToken.tokenHash(),
                oldToken.rotatedAt(),
                newToken.expiresAt(),
                "refresh");
        current.rotate(nextId, oldToken.rotatedAt(), "refresh");
        entityManager.persist(replacement);
    }

    /**
     * 方法 `compromiseFamily` 按照 `JpaRefreshTokenRepository` 的职责处理输入，完成 `compromise Family` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `compromiseFamily` processes its inputs according to `JpaRefreshTokenRepository`'s responsibility, performs the `compromise Family` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `compromiseFamily` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `compromiseFamily`, then continue the business flow using its result, exception, or side effect.
     *
     * @param familyId 输入参数 `familyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param detectedAt 输入参数 `detectedAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    @Override
    public void compromiseFamily(String familyId, Instant detectedAt) {
        List<RefreshTokenPO> family = entityManager.createQuery("""
                        select t from RefreshTokenEntity t where t.familyId = :familyId
                        """, RefreshTokenPO.class)
                .setParameter("familyId", familyId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        if (family.isEmpty()) {
            return;
        }
        for (RefreshTokenPO token : family) {
            if (token.getStatus() == RefreshTokenStatusEnum.ROTATED) {
                token.markReused(detectedAt, "refresh-replay");
            } else {
                token.revoke(detectedAt, "refresh-replay");
            }
        }
        RefreshTokenPO evidence = family.getFirst();
        sessionRepository.lockByTenantIdAndSessionId(
                        evidence.getTenantId(), evidence.getSessionId())
                .ifPresent(session -> {
                    if (session.compromise(detectedAt, "refresh-replay")) {
                        securityEventRecorder.record(
                                new TerminationVO(
                                        evidence.getTenantId().toString(),
                                        session.getUserId().toString(),
                                        evidence.getSessionId().toString(),
                                        session.getSessionVersion(), session.getStatus().name(),
                                        session.getRevokeReason(), "refresh-replay", detectedAt));
                        runtimeSynchronizer.synchronize(
                                evidence.getTenantId().toString(),
                                session.getUserId().toString(),
                                evidence.getSessionId().toString(), detectedAt);
                    }
                });
    }

    /**
     * 方法 `findByHash` 按照 `JpaRefreshTokenRepository` 的职责处理输入，完成 `find By Hash` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `findByHash` processes its inputs according to `JpaRefreshTokenRepository`'s responsibility, performs the `find By Hash` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `findByHash` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `findByHash`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tokenHash 输入参数 `tokenHash`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param lockMode 输入参数 `lockMode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private RefreshTokenPO findByHash(String tokenHash, LockModeType lockMode) {
        return entityManager.createQuery(
                        "select t from RefreshTokenEntity t where t.tokenHash = :tokenHash",
                        RefreshTokenPO.class)
                .setParameter("tokenHash", tokenHash)
                .setLockMode(lockMode)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    /**
     * 方法 `toRecord` 按照 `JpaRefreshTokenRepository` 的职责处理输入，完成 `to Record` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `toRecord` processes its inputs according to `JpaRefreshTokenRepository`'s responsibility, performs the `to Record` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `toRecord` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `toRecord`, then continue the business flow using its result, exception, or side effect.
     *
     * @param entity 输入参数 `entity`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static TokenRecordVO toRecord(RefreshTokenPO entity) {
        return new TokenRecordVO(
                entity.getId().toString(),
                entity.getTenantId().toString(),
                entity.getSessionId().toString(),
                entity.getFamilyId(),
                entity.getGeneration(),
                entity.getTokenHash(),
                RefreshTokenTokenStatusEnum.valueOf(entity.getStatus().name()),
                entity.getExpiresAt(),
                null);
    }
}
