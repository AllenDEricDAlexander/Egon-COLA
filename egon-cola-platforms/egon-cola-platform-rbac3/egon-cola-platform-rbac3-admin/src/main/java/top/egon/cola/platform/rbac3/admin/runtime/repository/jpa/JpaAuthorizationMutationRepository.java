package top.egon.cola.platform.rbac3.admin.runtime.repository.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.runtime.domain.po.AuthorizationMutationPO;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import top.egon.cola.platform.rbac3.admin.runtime.repository.AuthorizationMutationRepository;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.MutationRecordVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.enums.AuthorizationMutationResultStatusEnum;
import top.egon.cola.platform.rbac3.admin.runtime.repository.MutationQueryPort;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.MutationVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.AuthorizationMutationPageVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.enums.AuthorizationMutationScopeTypeEnum;
import top.egon.cola.platform.rbac3.admin.runtime.domain.enums.AuthorizationMutationStatusEnum;
import top.egon.cola.platform.rbac3.admin.runtime.repository.AuthorizationMutationRecoveryRepository;
import top.egon.cola.platform.rbac3.admin.runtime.domain.dto.MutationWorkDTO;

/**
 * 类型 `JpaAuthorizationMutationRepository` 位于当前包内，是类型，用于承载 `Authorization Mutation Repository` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `JpaAuthorizationMutationRepository` is a type in its package and carries the responsibility, state, or contract for `Authorization Mutation Repository`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `JpaAuthorizationMutationRepository` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `JpaAuthorizationMutationRepository` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Repository
public class JpaAuthorizationMutationRepository implements
        AuthorizationMutationRepository,
        MutationQueryPort,
        AuthorizationMutationRecoveryRepository {

    /**
     * 字段 `entityManager` 表示 `JpaAuthorizationMutationRepository` 中与 `entity Manager` 相关的状态、依赖、配置或结果（声明类型 `EntityManager`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `entityManager` stores the `entity Manager`-related state, dependency, configuration, or result of `JpaAuthorizationMutationRepository` (declared type `EntityManager`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `entityManager` 时应保持 `JpaAuthorizationMutationRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `entityManager`, preserve `JpaAuthorizationMutationRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final EntityManager entityManager;

    /**
     * 构造器 `JpaAuthorizationMutationRepository` 用于创建并初始化 `JpaAuthorizationMutationRepository` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `JpaAuthorizationMutationRepository` creates and initializes `JpaAuthorizationMutationRepository`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `JpaAuthorizationMutationRepository` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `JpaAuthorizationMutationRepository`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param entityManager 输入参数 `entityManager`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public JpaAuthorizationMutationRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * 方法 `prepare` 按照 `JpaAuthorizationMutationRepository` 的职责处理输入，完成 `prepare` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `prepare` processes its inputs according to `JpaAuthorizationMutationRepository`'s responsibility, performs the `prepare` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `prepare` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `prepare`, then continue the business flow using its result, exception, or side effect.
     *
     * @param record 输入参数 `record`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    @Override
    @Transactional
    public void prepare(MutationRecordVO record) {
        var scope = record.scope();
        var versions = record.versions();
        entityManager.persist(new AuthorizationMutationPO(
                Long.valueOf(record.mutationId()),
                Long.valueOf(scope.tenantId()),
                "USER".equals(scope.scopeType())
                        ? Long.valueOf(scope.scopeId()) : null,
                AuthorizationMutationScopeTypeEnum.valueOf(scope.scopeType()),
                scope.commandId(), versions.oldAuthVersion(),
                versions.newAuthVersion(), versions.oldPolicyVersion(),
                versions.newPolicyVersion(), scope.actorId(), record.createdAt()));
    }

    /**
     * 方法 `transition` 按照 `JpaAuthorizationMutationRepository` 的职责处理输入，完成 `transition` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `transition` processes its inputs according to `JpaAuthorizationMutationRepository`'s responsibility, performs the `transition` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `transition` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `transition`, then continue the business flow using its result, exception, or side effect.
     *
     * @param mutationId 输入参数 `mutationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param errorCode 输入参数 `errorCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    @Override
    @Transactional
    public void transition(
            String mutationId,
            AuthorizationMutationResultStatusEnum status,
            String errorCode,
            Instant now
    ) {
        AuthorizationMutationPO mutation = entityManager.find(
                AuthorizationMutationPO.class, Long.valueOf(mutationId),
                LockModeType.PESSIMISTIC_WRITE);
        if (mutation == null) {
            throw new IllegalStateException("authorization mutation is missing");
        }
        String actorId = mutation.getUpdatedBy();
        switch (status) {
            case COMMITTED -> mutation.committed(now, actorId);
            case FENCED -> mutation.fenced(now, actorId);
            case PROJECTED -> mutation.projected(now, actorId);
            case COMPLETED -> mutation.completed(now, actorId);
            case RECOVERY_REQUIRED -> mutation.recoveryRequired(
                    errorCode, now, actorId);
        }
    }

    /**
     * 方法 `query` 按照 `JpaAuthorizationMutationRepository` 的职责处理输入，完成 `query` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `query` processes its inputs according to `JpaAuthorizationMutationRepository`'s responsibility, performs the `query` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `query` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `query`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param cursor 输入参数 `cursor`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param pageSize 输入参数 `pageSize`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional(readOnly = true)
    public AuthorizationMutationPageVO query(
            String tenantId,
            String status,
            String cursor,
            int pageSize) {
        StringBuilder hql = new StringBuilder("""
                select m from AuthorizationMutationEntity m
                 where m.tenantId = :tenantId
                """);
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("tenantId", Long.valueOf(tenantId));
        if (status != null && !status.isBlank()) {
            hql.append(" and m.status = :status");
            parameters.put("status", AuthorizationMutationStatusEnum.valueOf(
                    status.trim()));
        }
        if (cursor != null && !cursor.isBlank()) {
            hql.append(" and m.mutationId < :cursorId");
            parameters.put("cursorId", parseCursor(cursor));
        }
        hql.append(" order by m.mutationId desc");
        var query = entityManager.createQuery(hql.toString(), AuthorizationMutationPO.class);
        parameters.forEach(query::setParameter);
        var rows = query.setMaxResults(pageSize + 1).getResultList();
        boolean more = rows.size() > pageSize;
        var pageRows = more ? rows.subList(0, pageSize) : rows;
        String nextCursor = more
                ? pageRows.getLast().getMutationId().toString()
                : null;
        return new AuthorizationMutationPageVO(
                pageRows.stream().map(this::toView).toList(), nextCursor);
    }

    /**
     * 方法 `claimById` 按照 `JpaAuthorizationMutationRepository` 的职责处理输入，完成 `claim By Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `claimById` processes its inputs according to `JpaAuthorizationMutationRepository`'s responsibility, performs the `claim By Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `claimById` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `claimById`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param mutationId 输入参数 `mutationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional
    public Optional<MutationWorkDTO> claimById(
            String tenantId,
            String mutationId) {
        List<AuthorizationMutationPO> rows = entityManager.createQuery("""
                        select m from AuthorizationMutationEntity m
                         where m.tenantId = :tenantId
                           and m.mutationId = :mutationId
                        """, AuthorizationMutationPO.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("mutationId", Long.valueOf(mutationId))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .setMaxResults(1)
                .getResultList();
        return rows.stream().findFirst().map(this::toWork);
    }

    /**
     * 方法 `claimRecoverable` 按照 `JpaAuthorizationMutationRepository` 的职责处理输入，完成 `claim Recoverable` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `claimRecoverable` processes its inputs according to `JpaAuthorizationMutationRepository`'s responsibility, performs the `claim Recoverable` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `claimRecoverable` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `claimRecoverable`, then continue the business flow using its result, exception, or side effect.
     *
     * @param batchSize 输入参数 `batchSize`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional
    public List<MutationWorkDTO> claimRecoverable(
            int batchSize) {
        @SuppressWarnings("unchecked")
        List<Number> mutationIds = entityManager.createNativeQuery("""
                        select mutation_id
                          from rbac3_authorization_mutation
                         where status in ('COMMITTED', 'PROJECTED', 'RECOVERY_REQUIRED')
                         order by updated_at, mutation_id
                         for update skip locked
                         limit :batchSize
                        """)
                .setParameter("batchSize", batchSize)
                .getResultList();
        return mutationIds.stream()
                .map(Number::longValue)
                .map(id -> entityManager.find(AuthorizationMutationPO.class, id))
                .filter(java.util.Objects::nonNull)
                .map(this::toWork)
                .toList();
    }

    /**
     * 方法 `completed` 按照 `JpaAuthorizationMutationRepository` 的职责处理输入，完成 `completed` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `completed` processes its inputs according to `JpaAuthorizationMutationRepository`'s responsibility, performs the `completed` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `completed` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `completed`, then continue the business flow using its result, exception, or side effect.
     *
     * @param mutationId 输入参数 `mutationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    @Override
    @Transactional
    public void completed(String mutationId, Instant now, String actorId) {
        AuthorizationMutationPO mutation = locked(mutationId);
        if (mutation.getStatus() == AuthorizationMutationStatusEnum.COMPLETED) {
            return;
        }
        if (mutation.getStatus() != AuthorizationMutationStatusEnum.PROJECTED) {
            mutation.projected(now, actorId);
        }
        mutation.completed(now, actorId);
    }

    /**
     * 方法 `failed` 按照 `JpaAuthorizationMutationRepository` 的职责处理输入，完成 `failed` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `failed` processes its inputs according to `JpaAuthorizationMutationRepository`'s responsibility, performs the `failed` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `failed` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `failed`, then continue the business flow using its result, exception, or side effect.
     *
     * @param mutationId 输入参数 `mutationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    @Override
    @Transactional
    public void failed(
            String mutationId,
            String reasonCode,
            Instant now,
            String actorId) {
        AuthorizationMutationPO mutation = locked(mutationId);
        if (mutation.getStatus() != AuthorizationMutationStatusEnum.COMPLETED) {
            mutation.recoveryRequired(reasonCode, now, actorId);
        }
    }

    /**
     * 方法 `toView` 按照 `JpaAuthorizationMutationRepository` 的职责处理输入，完成 `to View` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `toView` processes its inputs according to `JpaAuthorizationMutationRepository`'s responsibility, performs the `to View` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `toView` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `toView`, then continue the business flow using its result, exception, or side effect.
     *
     * @param mutation 输入参数 `mutation`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private MutationVO toView(
            AuthorizationMutationPO mutation) {
        String scopeId = switch (mutation.getScopeType()) {
            case USER -> String.valueOf(mutation.getUserId());
            case TENANT -> String.valueOf(mutation.getTenantId());
        };
        return new MutationVO(
                mutation.getMutationId().toString(),
                mutation.getScopeType().name(), scopeId,
                mutation.getCommandId(), mutation.getStatus().name(),
                mutation.getAttempt(), mutation.getLastErrorCode(),
                mutation.getUpdatedAt());
    }

    /**
     * 方法 `toWork` 按照 `JpaAuthorizationMutationRepository` 的职责处理输入，完成 `to Work` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `toWork` processes its inputs according to `JpaAuthorizationMutationRepository`'s responsibility, performs the `to Work` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `toWork` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `toWork`, then continue the business flow using its result, exception, or side effect.
     *
     * @param mutation 输入参数 `mutation`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private MutationWorkDTO toWork(
            AuthorizationMutationPO mutation) {
        String scopeId = switch (mutation.getScopeType()) {
            case USER -> String.valueOf(mutation.getUserId());
            case TENANT -> String.valueOf(mutation.getTenantId());
        };
        return new MutationWorkDTO(
                mutation.getMutationId().toString(),
                mutation.getTenantId().toString(),
                mutation.getScopeType().name(),
                scopeId,
                mutation.getStatus().name());
    }

    /**
     * 方法 `locked` 按照 `JpaAuthorizationMutationRepository` 的职责处理输入，完成 `locked` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `locked` processes its inputs according to `JpaAuthorizationMutationRepository`'s responsibility, performs the `locked` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `locked` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `locked`, then continue the business flow using its result, exception, or side effect.
     *
     * @param mutationId 输入参数 `mutationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private AuthorizationMutationPO locked(String mutationId) {
        AuthorizationMutationPO mutation = entityManager.find(
                AuthorizationMutationPO.class, Long.valueOf(mutationId),
                LockModeType.PESSIMISTIC_WRITE);
        if (mutation == null) {
            throw new IllegalStateException("authorization mutation is missing");
        }
        return mutation;
    }

    /**
     * 方法 `parseCursor` 按照 `JpaAuthorizationMutationRepository` 的职责处理输入，完成 `parse Cursor` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `parseCursor` processes its inputs according to `JpaAuthorizationMutationRepository`'s responsibility, performs the `parse Cursor` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `parseCursor` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `parseCursor`, then continue the business flow using its result, exception, or side effect.
     *
     * @param cursor 输入参数 `cursor`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static Long parseCursor(String cursor) {
        try {
            long value = Long.parseLong(cursor.trim());
            if (value <= 0) {
                throw new NumberFormatException("cursor must be positive");
            }
            return value;
        } catch (NumberFormatException invalid) {
            throw new IllegalArgumentException("mutation cursor is invalid", invalid);
        }
    }
}
