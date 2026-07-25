package top.egon.cola.component.gateway.admin.application.routing;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.gateway.admin.application.GatewayAdminIdempotencyConflictException;
import top.egon.cola.component.gateway.admin.application.GatewayAdminNotFoundException;
import top.egon.cola.component.gateway.admin.application.IdempotencyStore;
import top.egon.cola.component.gateway.admin.application.RequestAuditContext;
import top.egon.cola.component.gateway.admin.application.catalog.GatewayCatalogStore;
import top.egon.cola.component.gateway.admin.domain.AdminActor;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogRepository;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayDraftEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayDraftRepository;
import top.egon.cola.component.gateway.admin.rule.GatewayRuleCanonicalizer;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GatewayDraftService {

    private static final String SCOPE = "GATEWAY_DRAFT";

    private final GatewayDraftRepository drafts;

    private final GatewayDraftStore store;

    private final GatewayCatalogStore catalog;

    private final IdempotencyStore idempotency;

    private final GatewayAuditLogRepository audits;

    private final GatewayRuleCanonicalizer canonicalizer =
            new GatewayRuleCanonicalizer();

    private final Clock clock;

    public GatewayDraftService(
            GatewayDraftRepository drafts,
            GatewayDraftStore store,
            GatewayCatalogStore catalog,
            IdempotencyStore idempotency,
            GatewayAuditLogRepository audits) {
        this(
                drafts,
                store,
                catalog,
                idempotency,
                audits,
                Clock.systemUTC()
        );
    }

    GatewayDraftService(
            GatewayDraftRepository drafts,
            GatewayDraftStore store,
            GatewayCatalogStore catalog,
            IdempotencyStore idempotency,
            GatewayAuditLogRepository audits,
            Clock clock) {
        this.drafts = drafts;
        this.store = store;
        this.catalog = catalog;
        this.idempotency = idempotency;
        this.audits = audits;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DraftView get(String gatewayGroupId) {
        GatewayDraftEntity draft = required(gatewayGroupId);
        return view(draft);
    }

    @Transactional
    public MutationResult putRoute(
            String gatewayGroupId,
            String routeId,
            RouteMutation command,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest(Map.of(
                "action", "PUT_ROUTE",
                "routeId", routeId,
                "command", command
        ));
        MutationResult replay = replay(
                gatewayGroupId,
                command.idempotencyKey(),
                digest
        );
        if (replay != null) {
            return replay;
        }
        GatewayCatalogStore.OperationRecord operation =
                catalog.findOperation(command.operationId())
                        .orElseThrow(() -> new GatewayAdminNotFoundException(
                                "gateway operation "
                                        + command.operationId()
                                        + " was not found"
                        ));
        if (isPublic(command.content())
                && !operation.externalAccessible()) {
            throw new IllegalArgumentException(
                    "PUBLIC route references an internal-only operation"
            );
        }
        GatewayDraftEntity draft = editable(
                gatewayGroupId,
                command.expectedRevision()
        );
        Instant now = clock.instant();
        store.upsertRoute(new GatewayDraftStore.RouteDraft(
                gatewayGroupId,
                required(routeId, "routeId"),
                command.operationId(),
                Map.copyOf(command.content()),
                command.enabled(),
                now,
                actor.actorId()
        ));
        return finish(
                draft,
                "ROUTE",
                routeId,
                "UPSERT",
                command.changeReason(),
                command.idempotencyKey(),
                digest,
                actor,
                request,
                now
        );
    }

    @Transactional
    public MutationResult deleteRoute(
            String gatewayGroupId,
            String routeId,
            MutationControl control,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest(Map.of(
                "action", "DELETE_ROUTE",
                "routeId", routeId,
                "expectedRevision", control.expectedRevision()
        ));
        MutationResult replay = replay(
                gatewayGroupId,
                control.idempotencyKey(),
                digest
        );
        if (replay != null) {
            return replay;
        }
        GatewayDraftEntity draft = editable(
                gatewayGroupId,
                control.expectedRevision()
        );
        store.deleteRoute(gatewayGroupId, routeId);
        return finish(
                draft,
                "ROUTE",
                routeId,
                "DELETE",
                control.changeReason(),
                control.idempotencyKey(),
                digest,
                actor,
                request,
                clock.instant()
        );
    }

    @Transactional
    public MutationResult putPolicy(
            String gatewayGroupId,
            String policyId,
            PolicyMutation command,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest(Map.of(
                "action", "PUT_POLICY",
                "policyId", policyId,
                "command", command
        ));
        MutationResult replay = replay(
                gatewayGroupId,
                command.idempotencyKey(),
                digest
        );
        if (replay != null) {
            return replay;
        }
        GatewayDraftEntity draft = editable(
                gatewayGroupId,
                command.expectedRevision()
        );
        Instant now = clock.instant();
        store.upsertPolicy(new GatewayDraftStore.PolicyDraft(
                gatewayGroupId,
                required(policyId, "policyId"),
                required(command.policyType(), "policyType")
                        .toUpperCase(),
                required(command.policyScope(), "policyScope")
                        .toUpperCase(),
                Map.copyOf(command.content()),
                command.enabled(),
                now,
                actor.actorId()
        ));
        return finish(
                draft,
                "POLICY",
                policyId,
                "UPSERT",
                command.changeReason(),
                command.idempotencyKey(),
                digest,
                actor,
                request,
                now
        );
    }

    @Transactional
    public MutationResult deletePolicy(
            String gatewayGroupId,
            String policyId,
            MutationControl control,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest(Map.of(
                "action", "DELETE_POLICY",
                "policyId", policyId,
                "expectedRevision", control.expectedRevision()
        ));
        MutationResult replay = replay(
                gatewayGroupId,
                control.idempotencyKey(),
                digest
        );
        if (replay != null) {
            return replay;
        }
        GatewayDraftEntity draft = editable(
                gatewayGroupId,
                control.expectedRevision()
        );
        store.deletePolicy(gatewayGroupId, policyId);
        return finish(
                draft,
                "POLICY",
                policyId,
                "DELETE",
                control.changeReason(),
                control.idempotencyKey(),
                digest,
                actor,
                request,
                clock.instant()
        );
    }

    @Transactional(readOnly = true)
    public ValidationReport validate(String gatewayGroupId) {
        DraftView draft = get(gatewayGroupId);
        List<ValidationIssue> errors = new ArrayList<>();
        List<ValidationIssue> warnings = new ArrayList<>();
        for (GatewayDraftStore.RouteDraft route : draft.routes()) {
            GatewayCatalogStore.OperationRecord operation =
                    catalog.findOperation(route.operationId()).orElse(null);
            if (operation == null) {
                errors.add(new ValidationIssue(
                        "routes." + route.routeId() + ".operationId",
                        "OPERATION_NOT_FOUND",
                        "referenced operation does not exist"
                ));
                continue;
            }
            if ("OFFLINE".equals(operation.lifecycleStatus())) {
                errors.add(new ValidationIssue(
                        "routes." + route.routeId() + ".operationId",
                        "OPERATION_OFFLINE",
                        "offline operation cannot be published"
                ));
            } else if ("DISCOVERED".equals(
                    operation.lifecycleStatus())) {
                errors.add(new ValidationIssue(
                        "routes." + route.routeId() + ".operationId",
                        "OPERATION_NOT_ACTIVE",
                        "operation is not active on any provider"
                ));
            } else if ("DEPRECATED".equals(operation.lifecycleStatus())) {
                warnings.add(new ValidationIssue(
                        "routes." + route.routeId() + ".operationId",
                        "OPERATION_DEPRECATED",
                        "deprecated operation remains routable"
                ));
            }
            if (isPublic(route.content())
                    && !operation.externalAccessible()) {
                errors.add(new ValidationIssue(
                        "routes." + route.routeId() + ".accessZones",
                        "EXTERNAL_ACCESS_DENIED",
                        "operation is not externally accessible"
                ));
            }
        }
        return new ValidationReport(
                errors.isEmpty(),
                draft.revision(),
                List.copyOf(errors),
                List.copyOf(warnings),
                digest(Map.of(
                        "routes", draft.routes(),
                        "policies", draft.policies()
                ))
        );
    }

    @Transactional(readOnly = true)
    public DraftDiff diff(String gatewayGroupId) {
        DraftView draft = get(gatewayGroupId);
        return new DraftDiff(
                draft.basedOnReleaseId(),
                draft.revision(),
                draft.routes().size(),
                draft.policies().size(),
                digest(Map.of(
                        "routes", draft.routes(),
                        "policies", draft.policies()
                ))
        );
    }

    private MutationResult finish(
            GatewayDraftEntity draft,
            String resourceType,
            String resourceId,
            String action,
            String reason,
            String key,
            String payloadSha,
            AdminActor actor,
            RequestAuditContext request,
            Instant now) {
        draft.touch(required(reason, "changeReason"), actor.actorId(), now);
        drafts.flush();
        MutationResult result = new MutationResult(
                draft.getRevision(),
                resourceId,
                false
        );
        idempotency.save(new IdempotencyStore.Record(
                SCOPE,
                draft.getGatewayGroupId(),
                required(key, "idempotencyKey"),
                payloadSha,
                resourceId,
                Map.of(
                        "revision", result.revision(),
                        "resourceId", resourceId
                ),
                now,
                now.plus(Duration.ofDays(7))
        ));
        audits.save(new GatewayAuditLogEntity(
                UuidV7.simpleString(),
                actor.actorId(),
                actor.actorType().name(),
                "MANAGEMENT_API",
                request.requestId(),
                request.traceId(),
                resourceType,
                resourceId,
                action,
                null,
                Map.of("changeReason", reason),
                draft.getRevision(),
                null,
                true,
                null,
                now
        ));
        return result;
    }

    private MutationResult replay(
            String gatewayGroupId,
            String key,
            String digest) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException(
                    "idempotencyKey is required"
            );
        }
        IdempotencyStore.Record existing = idempotency.find(
                SCOPE,
                gatewayGroupId,
                key
        ).orElse(null);
        if (existing == null) {
            return null;
        }
        if (!existing.payloadSha256().equals(digest)) {
            throw new GatewayAdminIdempotencyConflictException();
        }
        Object revision = existing.response().get("revision");
        long value = revision instanceof Number number
                ? number.longValue()
                : Long.parseLong(revision.toString());
        return new MutationResult(value, existing.resourceId(), true);
    }

    private GatewayDraftEntity editable(String id, long expectedRevision) {
        GatewayDraftEntity draft = required(id);
        draft.assertEditable(expectedRevision);
        return draft;
    }

    private GatewayDraftEntity required(String id) {
        return drafts.findById(id)
                .orElseThrow(() -> new GatewayAdminNotFoundException(
                        "gateway draft " + id + " was not found"
                ));
    }

    private DraftView view(GatewayDraftEntity draft) {
        return new DraftView(
                draft.getGatewayGroupId(),
                draft.getRevision(),
                draft.getBasedOnReleaseId(),
                draft.getStatus(),
                draft.getChangeSummary(),
                store.routes(draft.getGatewayGroupId()),
                store.policies(draft.getGatewayGroupId()),
                draft.getUpdatedAt()
        );
    }

    private boolean isPublic(Map<String, Object> content) {
        Object zones = content.get("accessZones");
        return zones instanceof Iterable<?> values
                && java.util.stream.StreamSupport.stream(
                values.spliterator(),
                false
        ).anyMatch(value -> "PUBLIC".equalsIgnoreCase(value.toString()));
    }

    private String digest(Object value) {
        return GatewayRuleCanonicalizer.sha256(
                canonicalizer.canonicalBytes(value)
        );
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    public record DraftView(
            String gatewayGroupId,
            long revision,
            String basedOnReleaseId,
            String status,
            String changeSummary,
            List<GatewayDraftStore.RouteDraft> routes,
            List<GatewayDraftStore.PolicyDraft> policies,
            Instant updatedAt
    ) {
    }

    public record MutationControl(
            long expectedRevision,
            String idempotencyKey,
            String changeReason
    ) {
    }

    public record RouteMutation(
            String operationId,
            Map<String, Object> content,
            boolean enabled,
            long expectedRevision,
            String idempotencyKey,
            String changeReason
    ) {
    }

    public record PolicyMutation(
            String policyType,
            String policyScope,
            Map<String, Object> content,
            boolean enabled,
            long expectedRevision,
            String idempotencyKey,
            String changeReason
    ) {
    }

    public record MutationResult(
            long revision,
            String resourceId,
            boolean replayed
    ) {
    }

    public record ValidationIssue(
            String path,
            String code,
            String message
    ) {
    }

    public record ValidationReport(
            boolean valid,
            long revision,
            List<ValidationIssue> errors,
            List<ValidationIssue> warnings,
            String draftSha256
    ) {
    }

    public record DraftDiff(
            String basedOnReleaseId,
            long revision,
            int routeCount,
            int policyCount,
            String draftSha256
    ) {
    }
}
