package top.egon.cola.component.gateway.admin.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.model.management.DdcManagementScopeBinding;
import top.egon.cola.component.gateway.admin.application.scope.GatewayScopeService;
import top.egon.cola.component.gateway.admin.domain.AdminActor;
import top.egon.cola.component.gateway.admin.domain.GatewayAdminRevisionConflictException;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayApplicationEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayApplicationRepository;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GatewayApplicationService {

    private final GatewayApplicationRepository applications;

    private final GatewayAuditLogRepository audits;

    private final GatewayScopeService scopes;

    private final Clock clock;

    @Autowired
    public GatewayApplicationService(
            GatewayApplicationRepository applications,
            GatewayAuditLogRepository audits,
            GatewayScopeService scopes) {
        this(applications, audits, scopes, Clock.systemUTC());
    }

    GatewayApplicationService(
            GatewayApplicationRepository applications,
            GatewayAuditLogRepository audits,
            GatewayScopeService scopes,
            Clock clock) {
        this.applications = applications;
        this.audits = audits;
        this.scopes = scopes;
        this.clock = clock;
    }

    @Transactional
    public GatewayApplicationView create(
            CreateGatewayApplication command,
            AdminActor actor,
            RequestAuditContext request) {
        GatewayScopeService.ScopeQuery scope =
                new GatewayScopeService.ScopeQuery(
                        required(command.bizCode(), "bizCode"),
                        required(command.namespace(), "namespace"),
                        required(command.env(), "env"),
                        required(command.applicationCode(), "applicationCode")
                );
        DdcManagementScopeBinding binding = scopes.requireEnabled(scope);
        GatewayApplicationEntity existing = applications
                .findByBizCodeAndApplicationCodeAndEnvAndDeletedFalse(
                        scope.bizCode(),
                        scope.appCode(),
                        scope.env()
                )
                .orElse(null);
        if (existing != null) {
            throw new GatewayApplicationAlreadyExistsException(
                    existing.getId()
            );
        }
        Instant now = clock.instant();
        GatewayApplicationEntity application = new GatewayApplicationEntity(
                UuidV7.simpleString(),
                scope.bizCode(),
                scope.appCode(),
                required(command.displayName(), "displayName"),
                scope.env(),
                scope.namespace(),
                command.description(),
                actor.actorId(),
                now
        );
        applications.saveAndFlush(application);
        audit(actor, request, application.getId(), "CREATE", Map.of(
                "bindingId", binding.bindingId(),
                "bizCode", scope.bizCode(),
                "applicationCode", scope.appCode(),
                "env", scope.env(),
                "namespace", scope.namespace()
        ));
        return view(application, scope.namespace(), true);
    }

    @Transactional(readOnly = true)
    public List<GatewayApplicationView> list() {
        return list(new GatewayScopeService.ScopeQuery(
                null,
                null,
                null,
                null
        ));
    }

    @Transactional(readOnly = true)
    public List<GatewayApplicationView> list(
            GatewayScopeService.ScopeQuery query) {
        List<DdcManagementScopeBinding> bindings = scopes.bindings(query);
        Map<GatewayScopeService.PhysicalApplicationKey,
                List<DdcManagementScopeBinding>> bindingsByApplication =
                bindings.stream().collect(Collectors.groupingBy(
                        GatewayApplicationService::physicalKey,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        return applications.findAllByDeletedFalseOrderByCreatedAtDesc()
                .stream()
                .filter(application -> query.empty()
                        || bindingsByApplication.containsKey(
                        physicalKey(application)))
                .map(application -> scopedView(
                        application,
                        query,
                        bindingsByApplication.getOrDefault(
                                physicalKey(application),
                                List.of()
                        )
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public GatewayApplicationView get(String id) {
        return view(required(id), null, false);
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
        return view(application, null, false);
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

    private GatewayApplicationView scopedView(
            GatewayApplicationEntity application,
            GatewayScopeService.ScopeQuery query,
            List<DdcManagementScopeBinding> bindings) {
        String namespace = application.getNamespace();
        if (!query.empty()) {
            namespace = query.namespace() == null
                    || query.namespace().isBlank()
                    ? matchedNamespace(application, bindings)
                    : query.namespace().trim();
        }
        return view(application, namespace, !bindings.isEmpty());
    }

    private String matchedNamespace(
            GatewayApplicationEntity application,
            List<DdcManagementScopeBinding> bindings) {
        return bindings.stream()
                .map(DdcManagementScopeBinding::namespaceCode)
                .filter(application.getNamespace()::equals)
                .findFirst()
                .orElseGet(() -> bindings.isEmpty()
                        ? application.getNamespace()
                        : bindings.getFirst().namespaceCode());
    }

    private GatewayApplicationView view(
            GatewayApplicationEntity application,
            String namespace,
            boolean ddcMatched) {
        return new GatewayApplicationView(
                application.getId(),
                application.getBizCode(),
                application.getApplicationCode(),
                application.getDisplayName(),
                application.getEnv(),
                namespace == null ? application.getNamespace() : namespace,
                application.getDescription(),
                ddcMatched,
                application.getRevision(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }

    private static GatewayScopeService.PhysicalApplicationKey physicalKey(
            DdcManagementScopeBinding binding) {
        return new GatewayScopeService.PhysicalApplicationKey(
                binding.bizCode(),
                binding.env(),
                binding.appCode()
        );
    }

    private static GatewayScopeService.PhysicalApplicationKey physicalKey(
            GatewayApplicationEntity application) {
        return new GatewayScopeService.PhysicalApplicationKey(
                application.getBizCode(),
                application.getEnv(),
                application.getApplicationCode()
        );
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    public record CreateGatewayApplication(
            String bizCode,
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
            String bizCode,
            String applicationCode,
            String displayName,
            String env,
            String namespace,
            String description,
            boolean ddcMatched,
            long revision,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
