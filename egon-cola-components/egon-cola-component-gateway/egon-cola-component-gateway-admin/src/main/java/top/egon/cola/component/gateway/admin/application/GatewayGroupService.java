package top.egon.cola.component.gateway.admin.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.gateway.admin.domain.AdminActor;
import top.egon.cola.component.gateway.admin.domain.GatewayAdminRevisionConflictException;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogRepository;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayDraftEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayDraftRepository;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayGroupEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayGroupRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class GatewayGroupService {

    private final GatewayGroupRepository groups;

    private final GatewayDraftRepository drafts;

    private final GatewayAuditLogRepository audits;

    private final Clock clock;

    public GatewayGroupService(
            GatewayGroupRepository groups,
            GatewayDraftRepository drafts,
            GatewayAuditLogRepository audits) {
        this(groups, drafts, audits, Clock.systemUTC());
    }

    GatewayGroupService(
            GatewayGroupRepository groups,
            GatewayDraftRepository drafts,
            GatewayAuditLogRepository audits,
            Clock clock) {
        this.groups = groups;
        this.drafts = drafts;
        this.audits = audits;
        this.clock = clock;
    }

    @Transactional
    public GatewayGroupView create(
            CreateGatewayGroup command,
            AdminActor actor,
            RequestAuditContext request) {
        Instant now = clock.instant();
        String id = UuidV7.simpleString();
        GatewayGroupEntity group = new GatewayGroupEntity(
                id,
                command.gatewayGroupCode(),
                command.displayName(),
                command.env(),
                command.namespace(),
                command.description(),
                actor.actorId(),
                now
        );
        groups.save(group);
        drafts.save(new GatewayDraftEntity(id, actor.actorId(), now));
        audit(
                actor,
                request,
                "GATEWAY_GROUP",
                id,
                "CREATE",
                null,
                Map.of(
                        "gatewayGroupCode",
                        command.gatewayGroupCode(),
                        "env",
                        command.env(),
                        "namespace",
                        command.namespace()
                )
        );
        return view(group);
    }

    @Transactional(readOnly = true)
    public List<GatewayGroupView> list() {
        return groups.findAllByDeletedFalseOrderByCreatedAtDesc()
                .stream()
                .map(this::view)
                .toList();
    }

    @Transactional(readOnly = true)
    public GatewayGroupView get(String id) {
        return view(required(id));
    }

    @Transactional
    public GatewayGroupView update(
            String id,
            UpdateGatewayGroup command,
            AdminActor actor,
            RequestAuditContext request) {
        GatewayGroupEntity group = required(id);
        assertRevision(command.expectedRevision(), group.getRevision());
        Map<String, Object> before = Map.of(
                "displayName",
                group.getDisplayName(),
                "enabled",
                group.isEnabled()
        );
        group.update(
                command.displayName(),
                command.description(),
                actor.actorId(),
                clock.instant()
        );
        groups.flush();
        audit(
                actor,
                request,
                "GATEWAY_GROUP",
                id,
                "UPDATE",
                before,
                Map.of(
                        "displayName",
                        group.getDisplayName(),
                        "enabled",
                        group.isEnabled()
                )
        );
        return view(group);
    }

    @Transactional
    public GatewayGroupView setEnabled(
            String id,
            boolean enabled,
            AdminActor actor,
            RequestAuditContext request) {
        GatewayGroupEntity group = required(id);
        group.setEnabled(enabled, actor.actorId(), clock.instant());
        groups.flush();
        audit(
                actor,
                request,
                "GATEWAY_GROUP",
                id,
                enabled ? "ENABLE" : "DISABLE",
                Map.of("enabled", !enabled),
                Map.of("enabled", enabled)
        );
        return view(group);
    }

    private GatewayGroupEntity required(String id) {
        return groups.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new GatewayAdminNotFoundException(
                        "gateway group " + id + " was not found"
                ));
    }

    private void assertRevision(long expected, long current) {
        if (expected != current) {
            throw new GatewayAdminRevisionConflictException(current);
        }
    }

    private void audit(
            AdminActor actor,
            RequestAuditContext request,
            String resourceType,
            String resourceId,
            String action,
            Map<String, Object> before,
            Map<String, Object> after) {
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
                before,
                after,
                null,
                null,
                true,
                null,
                clock.instant()
        ));
    }

    private GatewayGroupView view(GatewayGroupEntity group) {
        return new GatewayGroupView(
                group.getId(),
                group.getGatewayGroupCode(),
                group.getDisplayName(),
                group.getEnv(),
                group.getNamespace(),
                group.getDescription(),
                group.isEnabled(),
                group.getRevision(),
                group.getCreatedAt(),
                group.getUpdatedAt()
        );
    }

    public record CreateGatewayGroup(
            String gatewayGroupCode,
            String displayName,
            String env,
            String namespace,
            String description
    ) {
    }

    public record UpdateGatewayGroup(
            String displayName,
            String description,
            long expectedRevision
    ) {
    }

    public record GatewayGroupView(
            String id,
            String gatewayGroupCode,
            String displayName,
            String env,
            String namespace,
            String description,
            boolean enabled,
            long revision,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
