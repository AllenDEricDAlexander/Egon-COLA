package top.egon.cola.component.gateway.admin.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.gateway.admin.domain.AdminActor;
import top.egon.cola.component.gateway.admin.domain.GatewayAdminRevisionConflictException;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayApplicationEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayApplicationRepository;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class GatewayApplicationService {

    private final GatewayApplicationRepository applications;

    private final GatewayAuditLogRepository audits;

    private final Clock clock;

    @Autowired
    public GatewayApplicationService(
            GatewayApplicationRepository applications,
            GatewayAuditLogRepository audits) {
        this(applications, audits, Clock.systemUTC());
    }

    GatewayApplicationService(
            GatewayApplicationRepository applications,
            GatewayAuditLogRepository audits,
            Clock clock) {
        this.applications = applications;
        this.audits = audits;
        this.clock = clock;
    }

    @Transactional
    public GatewayApplicationView create(
            CreateGatewayApplication command,
            AdminActor actor,
            RequestAuditContext request) {
        Instant now = clock.instant();
        GatewayApplicationEntity application = new GatewayApplicationEntity(
                UuidV7.simpleString(),
                required(command.applicationCode(), "applicationCode"),
                required(command.displayName(), "displayName"),
                required(command.env(), "env"),
                required(command.namespace(), "namespace"),
                command.description(),
                actor.actorId(),
                now
        );
        applications.save(application);
        audit(actor, request, application.getId(), "CREATE", Map.of(
                "applicationCode", application.getApplicationCode(),
                "env", application.getEnv(),
                "namespace", application.getNamespace()
        ));
        return view(application);
    }

    @Transactional(readOnly = true)
    public List<GatewayApplicationView> list() {
        return applications.findAllByDeletedFalseOrderByCreatedAtDesc()
                .stream()
                .map(this::view)
                .toList();
    }

    @Transactional(readOnly = true)
    public GatewayApplicationView get(String id) {
        return view(required(id));
    }

    @Transactional
    public GatewayApplicationView update(
            String id,
            UpdateGatewayApplication command,
            AdminActor actor,
            RequestAuditContext request) {
        GatewayApplicationEntity application = required(id);
        if (application.getRevision() != command.expectedRevision()) {
            throw new GatewayAdminRevisionConflictException(
                    application.getRevision()
            );
        }
        application.update(
                required(command.displayName(), "displayName"),
                command.description(),
                actor.actorId(),
                clock.instant()
        );
        applications.flush();
        audit(actor, request, id, "UPDATE", Map.of(
                "displayName", application.getDisplayName(),
                "revision", application.getRevision()
        ));
        return view(application);
    }

    private GatewayApplicationEntity required(String id) {
        return applications.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new GatewayAdminNotFoundException(
                        "gateway application " + id + " was not found"
                ));
    }

    private void audit(
            AdminActor actor,
            RequestAuditContext request,
            String id,
            String action,
            Map<String, Object> after) {
        audits.save(new GatewayAuditLogEntity(
                UuidV7.simpleString(),
                actor.actorId(),
                actor.actorType().name(),
                "MANAGEMENT_API",
                request.requestId(),
                request.traceId(),
                "GATEWAY_APPLICATION",
                id,
                action,
                null,
                after,
                null,
                null,
                true,
                null,
                clock.instant()
        ));
    }

    private GatewayApplicationView view(
            GatewayApplicationEntity application) {
        return new GatewayApplicationView(
                application.getId(),
                application.getApplicationCode(),
                application.getDisplayName(),
                application.getEnv(),
                application.getNamespace(),
                application.getDescription(),
                application.getRevision(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    public record CreateGatewayApplication(
            String applicationCode,
            String displayName,
            String env,
            String namespace,
            String description
    ) {
    }

    public record UpdateGatewayApplication(
            String displayName,
            String description,
            long expectedRevision
    ) {
    }

    public record GatewayApplicationView(
            String id,
            String applicationCode,
            String displayName,
            String env,
            String namespace,
            String description,
            long revision,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
