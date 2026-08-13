package top.egon.cola.platform.rbac3.admin.audit.infrastructure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.application.port.AuditPort;
import top.egon.cola.platform.rbac3.admin.audit.application.AuditQueryService;
import top.egon.cola.platform.rbac3.admin.audit.domain.AuditLogEntity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 类型 `PostgresqlAuditStore` 位于当前包内，是类型，用于承载 `Postgresql Audit Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `PostgresqlAuditStore` is a type in its package and carries the responsibility, state, or contract for `Postgresql Audit Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * PostgreSQL append/query adapter for already-redacted audit records.
 */
@Repository
public class PostgresqlAuditStore implements AuditPort, AuditQueryService.AuditStore {

    /**
     * 字段 `entityManager` 表示 `PostgresqlAuditStore` 中与 `entity Manager` 相关的状态、依赖、配置或结果（声明类型 `EntityManager`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `entityManager` stores the `entity Manager`-related state, dependency, configuration, or result of `PostgresqlAuditStore` (declared type `EntityManager`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `entityManager` 时应保持 `PostgresqlAuditStore` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `entityManager`, preserve `PostgresqlAuditStore`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final EntityManager entityManager;
    /**
     * 字段 `idGenerator` 表示 `PostgresqlAuditStore` 中与 `id Generator` 相关的状态、依赖、配置或结果（声明类型 `LongIdGenerator`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `idGenerator` stores the `id Generator`-related state, dependency, configuration, or result of `PostgresqlAuditStore` (declared type `LongIdGenerator`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `idGenerator` 时应保持 `PostgresqlAuditStore` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `idGenerator`, preserve `PostgresqlAuditStore`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final LongIdGenerator idGenerator;
    /**
     * 字段 `cursorCodec` 表示 `PostgresqlAuditStore` 中与 `cursor Codec` 相关的状态、依赖、配置或结果（声明类型 `AuditCursorCodec`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `cursorCodec` stores the `cursor Codec`-related state, dependency, configuration, or result of `PostgresqlAuditStore` (declared type `AuditCursorCodec`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `cursorCodec` 时应保持 `PostgresqlAuditStore` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `cursorCodec`, preserve `PostgresqlAuditStore`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AuditCursorCodec cursorCodec;

    /**
     * 构造器 `PostgresqlAuditStore` 用于创建并初始化 `PostgresqlAuditStore` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `PostgresqlAuditStore` creates and initializes `PostgresqlAuditStore`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `PostgresqlAuditStore` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `PostgresqlAuditStore`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param entityManager 输入参数 `entityManager`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idGenerator 输入参数 `idGenerator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param cursorCodec 输入参数 `cursorCodec`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public PostgresqlAuditStore(
            EntityManager entityManager,
            LongIdGenerator idGenerator,
            AuditCursorCodec cursorCodec) {
        this.entityManager = entityManager;
        this.idGenerator = idGenerator;
        this.cursorCodec = cursorCodec;
    }

    /**
     * 方法 `append` 按照 `PostgresqlAuditStore` 的职责处理输入，完成 `append` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `append` processes its inputs according to `PostgresqlAuditStore`'s responsibility, performs the `append` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `append` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `append`, then continue the business flow using its result, exception, or side effect.
     *
     * @param event 输入参数 `event`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    @Override
    @Transactional
    public void append(AuditEvent event) {
        new AuditQueryService(
                this, Clock.fixed(event.occurredAt(), ZoneOffset.UTC))
                .record(new AuditQueryService.AuditCommand(
                        event.tenantId(), event.eventType(), event.outcome(), event.severity(), "USER",
                        event.actorId(), event.targetType(), event.targetId(), null,
                        event.reasonCode(), event.requestId(), event.traceId(), Map.of(),
                        event.safeEvidence(), event.occurredAt()));
    }

    /**
     * 方法 `append` 按照 `PostgresqlAuditStore` 的职责处理输入，完成 `append` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `append` processes its inputs according to `PostgresqlAuditStore`'s responsibility, performs the `append` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `append` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `append`, then continue the business flow using its result, exception, or side effect.
     *
     * @param record 输入参数 `record`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional
    public AuditQueryService.AuditView append(AuditQueryService.AuditView record) {
        AuditLogEntity entity = new AuditLogEntity(
                idGenerator.nextLongId(), Long.valueOf(record.tenantId()),
                record.eventType(), record.outcome(), record.severity(),
                record.actorType(), record.actorId(), record.targetType(), record.targetId(),
                record.managementPolicyId() == null
                        ? null : Long.valueOf(record.managementPolicyId()),
                record.reasonCode(), record.requestId(), record.traceId(), null, null,
                record.beforeSnapshot(), record.afterSnapshot(), record.payloadChecksum(),
                record.createdAt());
        entityManager.persist(entity);
        return toView(entity);
    }

    /**
     * 方法 `query` 按照 `PostgresqlAuditStore` 的职责处理输入，完成 `query` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `query` processes its inputs according to `PostgresqlAuditStore`'s responsibility, performs the `query` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `query` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `query`, then continue the business flow using its result, exception, or side effect.
     *
     * @param query 输入参数 `query`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional(readOnly = true)
    public AuditQueryService.Page query(AuditQueryService.Query query) {
        String filterDigest = filterDigest(query);
        var jpql = new StringBuilder("""
                select a from AuditLogEntity a
                 where a.tenantId = :tenantId
                   and a.createdAt >= :from
                   and a.createdAt <= :to
                """);
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("tenantId", Long.valueOf(query.tenantId()));
        parameters.put("from", query.from());
        parameters.put("to", query.to());
        exact(jpql, parameters, "actorId", query.actorId());
        exact(jpql, parameters, "targetId", query.targetId());
        exact(jpql, parameters, "eventType", query.eventType());
        exact(jpql, parameters, "outcome", query.outcome());
        exact(jpql, parameters, "reasonCode", query.reasonCode());
        exact(jpql, parameters, "requestId", query.requestId());
        exact(jpql, parameters, "traceId", query.traceId());
        exact(jpql, parameters, "targetType", query.targetType());
        if (query.cursor() != null) {
            AuditCursorCodec.CursorPosition cursor = cursorCodec.decode(
                    query.cursor(), query.tenantId(), filterDigest);
            jpql.append(" and (a.createdAt < :cursorTime")
                    .append(" or (a.createdAt = :cursorTime and a.id < :cursorId))");
            parameters.put("cursorTime", cursor.createdAt());
            parameters.put("cursorId", cursor.id());
        }
        jpql.append(" order by a.createdAt desc, a.id desc");
        TypedQuery<AuditLogEntity> typedQuery = entityManager.createQuery(
                jpql.toString(), AuditLogEntity.class);
        parameters.forEach(typedQuery::setParameter);
        List<AuditLogEntity> rows = typedQuery
                .setMaxResults(query.pageSize() + 1)
                .getResultList();
        boolean more = rows.size() > query.pageSize();
        List<AuditLogEntity> pageRows = more
                ? rows.subList(0, query.pageSize()) : rows;
        String nextCursor = more
                ? cursorCodec.encode(new AuditCursorCodec.CursorPosition(
                        pageRows.getLast().getCreatedAt(), pageRows.getLast().getId()),
                        query.tenantId(), filterDigest)
                : null;
        return new AuditQueryService.Page(
                pageRows.stream().map(this::toView).toList(), nextCursor);
    }

    /**
     * 方法 `exact` 按照 `PostgresqlAuditStore` 的职责处理输入，完成 `exact` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `exact` processes its inputs according to `PostgresqlAuditStore`'s responsibility, performs the `exact` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `exact` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `exact`, then continue the business flow using its result, exception, or side effect.
     *
     * @param jpql 输入参数 `jpql`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param parameters 输入参数 `parameters`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param property 输入参数 `property`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void exact(
            StringBuilder jpql,
            Map<String, Object> parameters,
            String property,
            String value) {
        if (value != null && !value.isBlank()) {
            jpql.append(" and a.").append(property).append(" = :").append(property);
            parameters.put(property, value.trim());
        }
    }

    /**
     * 方法 `filterDigest` 按照 `PostgresqlAuditStore` 的职责处理输入，完成 `filter Digest` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `filterDigest` processes its inputs according to `PostgresqlAuditStore`'s responsibility, performs the `filter Digest` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `filterDigest` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `filterDigest`, then continue the business flow using its result, exception, or side effect.
     *
     * @param query 输入参数 `query`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private String filterDigest(AuditQueryService.Query query) {
        String value = String.join("\u001f",
                query.from().toString(), query.to().toString(),
                safe(query.actorId()), safe(query.targetId()), safe(query.eventType()),
                safe(query.outcome()), safe(query.reasonCode()), safe(query.requestId()),
                safe(query.traceId()), safe(query.targetType()));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }

    /**
     * 方法 `safe` 按照 `PostgresqlAuditStore` 的职责处理输入，完成 `safe` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `safe` processes its inputs according to `PostgresqlAuditStore`'s responsibility, performs the `safe` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `safe` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `safe`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private String safe(String value) {
        return value == null ? "" : value;
    }

    /**
     * 方法 `toView` 按照 `PostgresqlAuditStore` 的职责处理输入，完成 `to View` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `toView` processes its inputs according to `PostgresqlAuditStore`'s responsibility, performs the `to View` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `toView` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `toView`, then continue the business flow using its result, exception, or side effect.
     *
     * @param entity 输入参数 `entity`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private AuditQueryService.AuditView toView(AuditLogEntity entity) {
        return new AuditQueryService.AuditView(
                entity.getId().toString(), entity.getTenantId().toString(),
                entity.getEventType(), entity.getOutcome(), entity.getSeverity(),
                entity.getActorType(), entity.getActorId(), entity.getTargetType(),
                entity.getTargetId(), entity.getManagementPolicyId() == null
                        ? null : entity.getManagementPolicyId().toString(),
                entity.getReasonCode(), entity.getRequestId(), entity.getTraceId(),
                entity.getBeforeSnapshot(), entity.getAfterSnapshot(),
                entity.getPayloadChecksum(), entity.getCreatedAt());
    }
}
