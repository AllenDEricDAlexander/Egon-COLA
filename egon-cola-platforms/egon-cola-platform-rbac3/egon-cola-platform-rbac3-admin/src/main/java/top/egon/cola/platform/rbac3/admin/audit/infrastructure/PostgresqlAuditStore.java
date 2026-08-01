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
 * PostgreSQL append/query adapter for already-redacted audit records.
 */
@Repository
public class PostgresqlAuditStore implements AuditPort, AuditQueryService.AuditStore {

    private final EntityManager entityManager;
    private final LongIdGenerator idGenerator;
    private final AuditCursorCodec cursorCodec;

    public PostgresqlAuditStore(
            EntityManager entityManager,
            LongIdGenerator idGenerator,
            AuditCursorCodec cursorCodec) {
        this.entityManager = entityManager;
        this.idGenerator = idGenerator;
        this.cursorCodec = cursorCodec;
    }

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

    private String safe(String value) {
        return value == null ? "" : value;
    }

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
