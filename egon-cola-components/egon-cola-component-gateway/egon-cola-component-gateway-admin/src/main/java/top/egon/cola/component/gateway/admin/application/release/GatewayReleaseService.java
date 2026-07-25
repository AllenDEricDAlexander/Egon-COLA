package top.egon.cola.component.gateway.admin.application.release;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishTarget;
import top.egon.cola.component.gateway.admin.application.GatewayAdminNotFoundException;
import top.egon.cola.component.gateway.admin.application.RequestAuditContext;
import top.egon.cola.component.gateway.admin.application.catalog.GatewayCatalogStore;
import top.egon.cola.component.gateway.admin.application.routing.GatewayDraftService;
import top.egon.cola.component.gateway.admin.application.routing.GatewayDraftStore;
import top.egon.cola.component.gateway.admin.domain.AdminActor;
import top.egon.cola.component.gateway.admin.domain.GatewayReleaseStatus;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogRepository;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayDraftEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayDraftRepository;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayGroupEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayGroupRepository;
import top.egon.cola.component.gateway.admin.rule.CompiledGatewayRelease;
import top.egon.cola.component.gateway.admin.rule.GatewayDdcRulePublisher;
import top.egon.cola.component.gateway.admin.rule.GatewayRuleCanonicalizer;
import top.egon.cola.component.gateway.admin.rule.GatewayRuleCompiler;
import top.egon.cola.component.gateway.admin.rule.GatewayRulePublishResult;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayProviderServiceRef;
import top.egon.cola.component.gateway.contract.rule.GatewayRpcDescriptor;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleContent;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeOperation;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimePolicy;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeRoute;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class GatewayReleaseService {

    private final GatewayGroupRepository groups;

    private final GatewayDraftRepository drafts;

    private final GatewayDraftService draftService;

    private final GatewayCatalogStore catalog;

    private final GatewayReleaseStore releases;

    private final GatewayAuditLogRepository audits;

    private final TransactionTemplate transactions;

    private final GatewayDdcRulePublisher publisher;

    private final GatewayRuleCanonicalizer canonicalizer =
            new GatewayRuleCanonicalizer();

    private final GatewayRuleCompiler compiler =
            new GatewayRuleCompiler(canonicalizer);

    private final Clock clock;

    public GatewayReleaseService(
            GatewayGroupRepository groups,
            GatewayDraftRepository drafts,
            GatewayDraftService draftService,
            GatewayCatalogStore catalog,
            GatewayReleaseStore releases,
            GatewayAuditLogRepository audits,
            TransactionTemplate transactions,
            ObjectProvider<GatewayDdcRulePublisher> publisher) {
        this(
                groups,
                drafts,
                draftService,
                catalog,
                releases,
                audits,
                transactions,
                publisher.getIfAvailable(),
                Clock.systemUTC()
        );
    }

    GatewayReleaseService(
            GatewayGroupRepository groups,
            GatewayDraftRepository drafts,
            GatewayDraftService draftService,
            GatewayCatalogStore catalog,
            GatewayReleaseStore releases,
            GatewayAuditLogRepository audits,
            TransactionTemplate transactions,
            GatewayDdcRulePublisher publisher,
            Clock clock) {
        this.groups = groups;
        this.drafts = drafts;
        this.draftService = draftService;
        this.catalog = catalog;
        this.releases = releases;
        this.audits = audits;
        this.transactions = transactions;
        this.publisher = publisher;
        this.clock = clock;
    }

    public ReleaseView create(
            String gatewayGroupId,
            CreateRelease command,
            AdminActor actor,
            RequestAuditContext request) {
        PreparedRelease prepared = transactions.execute(status -> prepare(
                gatewayGroupId,
                command.expectedDraftRevision(),
                command.changeReason(),
                null,
                null,
                actor,
                request
        ));
        return publish(prepared, actor);
    }

    public ReleaseView retry(
            String releaseId,
            AdminActor actor,
            RequestAuditContext request) {
        PreparedRelease prepared = transactions.execute(status -> {
            GatewayReleaseStore.ReleaseRecord release = required(releaseId);
            if (!Set.of(
                    GatewayReleaseStatus.FAILED,
                    GatewayReleaseStatus.TIMEOUT,
                    GatewayReleaseStatus.UNKNOWN
            ).contains(release.status())) {
                throw new IllegalStateException(
                        "GATEWAY_ADMIN_RELEASE_NOT_RETRYABLE"
                );
            }
            int attempt = releases.nextAttempt(
                    releaseId,
                    clock.instant()
            );
            audit(
                    actor,
                    request,
                    release.gatewayGroupId(),
                    releaseId,
                    "RETRY",
                    release.draftRevision()
            );
            return new PreparedRelease(
                    release,
                    releases.loadCompiled(releaseId),
                    attempt
            );
        });
        return publish(prepared, actor);
    }

    public ReleaseView rollback(
            String gatewayGroupId,
            RollbackRelease command,
            AdminActor actor,
            RequestAuditContext request) {
        GatewayReleaseStore.ReleaseRecord source =
                required(command.sourceReleaseId());
        if (!source.gatewayGroupId().equals(gatewayGroupId)
                || source.status() != GatewayReleaseStatus.SUCCESS) {
            throw new IllegalArgumentException(
                    "rollback source must be a successful release "
                            + "from the same gateway group"
            );
        }
        GatewayRuleContent content = releases.loadCompiled(source.id())
                .snapshot()
                .content();
        PreparedRelease prepared = transactions.execute(status -> prepare(
                gatewayGroupId,
                command.expectedDraftRevision(),
                command.changeReason(),
                source.id(),
                content,
                actor,
                request
        ));
        return publish(prepared, actor);
    }

    public ReleaseView get(String releaseId) {
        return view(required(releaseId));
    }

    public List<ReleaseView> history(String gatewayGroupId) {
        return releases.history(gatewayGroupId)
                .stream()
                .map(this::view)
                .toList();
    }

    public Map<String, Object> diff(String releaseId) {
        return required(releaseId).structuredDiff();
    }

    private PreparedRelease prepare(
            String gatewayGroupId,
            long expectedRevision,
            String changeReason,
            String rollbackOfReleaseId,
            GatewayRuleContent rollbackContent,
            AdminActor actor,
            RequestAuditContext request) {
        GatewayGroupEntity group = groups.findByIdAndDeletedFalse(
                gatewayGroupId
        ).orElseThrow(() -> new GatewayAdminNotFoundException(
                "gateway group " + gatewayGroupId + " was not found"
        ));
        if (!group.isEnabled()) {
            throw new IllegalStateException(
                    "GATEWAY_ADMIN_GATEWAY_GROUP_DISABLED"
            );
        }
        if (releases.hasReleaseInProgress(gatewayGroupId)) {
            throw new IllegalStateException(
                    "GATEWAY_ADMIN_RELEASE_IN_PROGRESS"
            );
        }
        GatewayDraftEntity draft = drafts.findById(gatewayGroupId)
                .orElseThrow(() -> new GatewayAdminNotFoundException(
                        "gateway draft " + gatewayGroupId + " was not found"
                ));
        draft.assertEditable(expectedRevision);
        GatewayDraftService.ValidationReport validation =
                draftService.validate(gatewayGroupId);
        if (!validation.valid()) {
            throw new IllegalArgumentException(
                    "draft validation failed: " + validation.errors()
            );
        }
        String releaseId = UuidV7.simpleString();
        GatewayRuleContent content = rollbackContent == null
                ? content(group, draftService.get(gatewayGroupId))
                : rollbackContent;
        CompiledGatewayRelease compiled = compiler.compile(
                releaseId,
                clock.instant(),
                content
        );
        Instant now = clock.instant();
        Map<String, Object> structuredDiff = Map.of(
                "basedOnReleaseId",
                value(draft.getBasedOnReleaseId()),
                "rollbackOfReleaseId",
                value(rollbackOfReleaseId),
                "routeCount", content.routes().size(),
                "operationCount", content.operations().size(),
                "policyCount", policyCount(content),
                "ruleContentSha256",
                compiled.snapshot().ruleContentSha256()
        );
        GatewayReleaseStore.ReleaseRecord release =
                new GatewayReleaseStore.ReleaseRecord(
                        releaseId,
                        gatewayGroupId,
                        draft.getRevision(),
                        draft.getBasedOnReleaseId(),
                        rollbackOfReleaseId,
                        GatewayReleaseStatus.READY,
                        false,
                        null,
                        Map.of(
                                "valid", true,
                                "warnings", validation.warnings(),
                                "draftSha256", validation.draftSha256()
                        ),
                        structuredDiff,
                        required(changeReason, "changeReason"),
                        now,
                        actor.actorId(),
                        now
                );
        releases.insert(release, compiled, 1);
        audit(
                actor,
                request,
                gatewayGroupId,
                releaseId,
                rollbackOfReleaseId == null ? "CREATE_RELEASE" : "ROLLBACK",
                draft.getRevision()
        );
        return new PreparedRelease(release, compiled, 1);
    }

    private ReleaseView publish(
            PreparedRelease prepared,
            AdminActor actor) {
        transactions.executeWithoutResult(status -> releases.beginAttempt(
                prepared.release().id(),
                prepared.attemptNo(),
                clock.instant()
        ));
        if (publisher == null) {
            transactions.executeWithoutResult(status -> releases
                    .completeAttempt(
                            prepared.release().id(),
                            prepared.attemptNo(),
                            GatewayReleaseStatus.FAILED,
                            false,
                            null,
                            "GATEWAY_ADMIN_DDC_UNAVAILABLE",
                            "DDC management client is not configured",
                            List.of(),
                            clock.instant()
                    ));
            return get(prepared.release().id());
        }
        String changeId = "gateway-release-" + prepared.release().id()
                + "-attempt-" + prepared.attemptNo();
        try {
            GatewayRulePublishResult result = publisher.publish(
                    prepared.compiled(),
                    null,
                    changeId,
                    actor.actorId()
            );
            List<GatewayReleaseStore.TargetRecord> targets =
                    result.activationResult()
                            .targets()
                            .stream()
                            .map(this::target)
                            .toList();
            transactions.executeWithoutResult(status -> {
                releases.completeAttempt(
                        prepared.release().id(),
                        prepared.attemptNo(),
                        GatewayReleaseStatus.SUCCESS,
                        false,
                        result.activationResult().changeId(),
                        null,
                        null,
                        targets,
                        clock.instant()
                );
                GatewayDraftEntity draft = drafts.findById(
                        prepared.release().gatewayGroupId()
                ).orElseThrow();
                draft.baseOn(
                        prepared.release().id(),
                        actor.actorId(),
                        clock.instant()
                );
                drafts.flush();
            });
        } catch (RuntimeException failure) {
            transactions.executeWithoutResult(status -> releases
                    .completeAttempt(
                            prepared.release().id(),
                            prepared.attemptNo(),
                            GatewayReleaseStatus.UNKNOWN,
                            false,
                            changeId,
                            "GATEWAY_ADMIN_DDC_UNAVAILABLE",
                            bounded(failure.getMessage()),
                            List.of(),
                            clock.instant()
                    ));
        }
        return get(prepared.release().id());
    }

    private GatewayRuleContent content(
            GatewayGroupEntity group,
            GatewayDraftService.DraftView draft) {
        Map<String, GatewayCatalogStore.OperationRecord> operations =
                new LinkedHashMap<>();
        draft.routes().forEach(route -> operations.put(
                route.operationId(),
                catalog.findOperation(route.operationId())
                        .orElseThrow(() -> new GatewayAdminNotFoundException(
                                "gateway operation "
                                        + route.operationId()
                                        + " was not found"
                        ))
        ));
        List<GatewayRuntimePolicy> policies = draft.policies()
                .stream()
                .filter(GatewayDraftStore.PolicyDraft::enabled)
                .map(policy -> new GatewayRuntimePolicy(
                        policy.policyId(),
                        policy.policyType(),
                        policy.policyScope(),
                        policy.content()
                ))
                .toList();
        List<GatewayRuntimeOperation> runtimeOperations = operations.values()
                .stream()
                .map(operation -> operation(
                        operation,
                        policyRefs(operation.id(), policies)
                ))
                .toList();
        List<GatewayRuntimeRoute> runtimeRoutes = draft.routes()
                .stream()
                .filter(GatewayDraftStore.RouteDraft::enabled)
                .map(this::route)
                .toList();
        List<GatewayRuntimePolicy> provider = policies.stream()
                .filter(policy -> Set.of(
                        "LOAD_BALANCE",
                        "PROVIDER_OVERRIDE"
                ).contains(policy.type()))
                .toList();
        List<GatewayRuntimePolicy> security = policies.stream()
                .filter(policy -> "SECURITY".equals(policy.type()))
                .toList();
        List<GatewayRuntimePolicy> cors = policies.stream()
                .filter(policy -> "CORS".equals(policy.type()))
                .toList();
        Set<String> separated = new LinkedHashSet<>();
        provider.forEach(policy -> separated.add(policy.policyId()));
        security.forEach(policy -> separated.add(policy.policyId()));
        cors.forEach(policy -> separated.add(policy.policyId()));
        List<GatewayRuntimePolicy> traffic = policies.stream()
                .filter(policy -> !separated.contains(policy.policyId()))
                .toList();
        List<GatewayRpcDescriptor> descriptors = runtimeOperations.stream()
                .filter(operation -> operation.protocol()
                        == GatewayProtocol.RPC)
                .map(operation -> descriptor(operations.get(
                        operation.operationId()
                )))
                .distinct()
                .toList();
        return new GatewayRuleContent(
                group.getId(),
                group.getGatewayGroupCode(),
                group.getEnv(),
                group.getNamespace(),
                runtimeOperations,
                runtimeRoutes,
                provider,
                traffic,
                security,
                cors,
                descriptors
        );
    }

    private GatewayRuntimeOperation operation(
            GatewayCatalogStore.OperationRecord operation,
            Set<String> policyRefs) {
        GatewayCatalogStore.OperationDefinition definition =
                catalog.loadDefinitions(operation.id())
                        .stream()
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "operation has no definition: "
                                        + operation.id()
                        ));
        Map<String, String> attributes = new LinkedHashMap<>();
        definition.attributes().forEach((key, value) -> {
            if (value != null) {
                attributes.put(key, value.toString());
            }
        });
        if (operation.protocol().equals("RPC")
                && definition.descriptorSnapshot() != null) {
            attributes.put(
                    "descriptorSha256",
                    definition.descriptorSnapshot()
                            .get("sha256")
                            .toString()
            );
        }
        Map<String, Object> provider = operation.providerServiceIdentity();
        GatewayProtocol protocol = GatewayProtocol.valueOf(
                operation.protocol()
        );
        String requestSchema = protocol == GatewayProtocol.RPC
                ? text(definition.requestSchema(), "messageType")
                : canonicalizer.json(definition.requestSchema());
        String responseSchema = protocol == GatewayProtocol.RPC
                ? text(definition.responseSchema(), "messageType")
                : canonicalizer.json(definition.responseSchema());
        return new GatewayRuntimeOperation(
                operation.id(),
                operation.operationKey(),
                protocol,
                operation.methodIdentity(),
                requestSchema,
                responseSchema,
                operation.externalAccessible(),
                new GatewayProviderServiceRef(
                        text(provider, "env"),
                        text(provider, "namespace"),
                        protocol,
                        text(provider, "serviceName"),
                        text(provider, "group"),
                        text(provider, "version"),
                        text(provider, "transport")
                ),
                attributes.getOrDefault(
                        "responseMode",
                        "TRANSPARENT"
                ),
                policyRefs,
                attributes,
                "DEPRECATED".equals(operation.lifecycleStatus())
        );
    }

    private GatewayRuntimeRoute route(GatewayDraftStore.RouteDraft route) {
        Map<String, Object> content = route.content();
        return new GatewayRuntimeRoute(
                route.routeId(),
                route.operationId(),
                text(content, "host"),
                text(content, "httpMethod"),
                text(content, "pathPattern"),
                values(content.get("accessZones")).stream()
                        .map(value -> AccessZone.valueOf(
                                value.toUpperCase()
                        ))
                        .collect(java.util.stream.Collectors.toUnmodifiableSet()),
                number(content.get("priority"), 0),
                route.enabled()
        );
    }

    private GatewayRpcDescriptor descriptor(
            GatewayCatalogStore.OperationRecord operation) {
        Map<String, Object> descriptor = catalog.loadDefinitions(
                operation.id()
        ).getFirst().descriptorSnapshot();
        if (descriptor == null) {
            throw new IllegalArgumentException(
                    "RPC operation has no descriptor snapshot"
            );
        }
        return new GatewayRpcDescriptor(
                text(descriptor, "descriptorId"),
                text(descriptor, "sha256"),
                text(descriptor, "base64DescriptorSet")
        );
    }

    private Set<String> policyRefs(
            String operationId,
            List<GatewayRuntimePolicy> policies) {
        return policies.stream()
                .filter(policy -> applies(policy, operationId))
                .map(GatewayRuntimePolicy::policyId)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private boolean applies(
            GatewayRuntimePolicy policy,
            String operationId) {
        if ("GLOBAL".equals(policy.scope())) {
            return true;
        }
        Object operationIds = policy.configuration().get("operationIds");
        return values(operationIds).contains(operationId);
    }

    private GatewayReleaseStore.TargetRecord target(
            DdcManagementPublishTarget target) {
        return new GatewayReleaseStore.TargetRecord(
                target.instanceId(),
                target.leaseId(),
                target.status(),
                target.currentVersion(),
                null,
                target.errorMessage() == null ? null : "DDC_TARGET_ERROR",
                target.ackAt() == null ? clock.instant() : target.ackAt()
        );
    }

    private GatewayReleaseStore.ReleaseRecord required(String releaseId) {
        return releases.find(releaseId)
                .orElseThrow(() -> new GatewayAdminNotFoundException(
                        "gateway release " + releaseId + " was not found"
                ));
    }

    private void audit(
            AdminActor actor,
            RequestAuditContext request,
            String gatewayGroupId,
            String releaseId,
            String action,
            long revision) {
        audits.save(new GatewayAuditLogEntity(
                UuidV7.simpleString(),
                actor.actorId(),
                actor.actorType().name(),
                "MANAGEMENT_API",
                request.requestId(),
                request.traceId(),
                "GATEWAY_RELEASE",
                releaseId,
                action,
                null,
                Map.of("gatewayGroupId", gatewayGroupId),
                revision,
                releaseId,
                true,
                null,
                clock.instant()
        ));
    }

    private ReleaseView view(GatewayReleaseStore.ReleaseRecord release) {
        return new ReleaseView(
                release.id(),
                release.gatewayGroupId(),
                release.draftRevision(),
                release.basedOnReleaseId(),
                release.rollbackOfReleaseId(),
                release.status(),
                release.partialApplied(),
                release.changeId(),
                release.validationReport(),
                release.structuredDiff(),
                release.changeReason(),
                release.createdAt(),
                release.updatedAt(),
                releases.attempts(release.id())
        );
    }

    private int policyCount(GatewayRuleContent content) {
        return content.providerPolicies().size()
                + content.trafficPolicies().size()
                + content.securityPolicies().size()
                + content.corsPolicies().size();
    }

    private List<String> values(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(Object::toString).toList();
        }
        return List.of(value.toString());
    }

    private int number(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        return value instanceof Number number
                ? number.intValue()
                : Integer.parseInt(value.toString());
    }

    private String text(Map<String, ?> values, String key) {
        Object value = values.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value.toString().trim();
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private String bounded(String value) {
        if (value == null) {
            return "DDC publish failed";
        }
        return value.length() <= 1024 ? value : value.substring(0, 1024);
    }

    private record PreparedRelease(
            GatewayReleaseStore.ReleaseRecord release,
            CompiledGatewayRelease compiled,
            int attemptNo
    ) {
    }

    public record CreateRelease(
            long expectedDraftRevision,
            String changeReason
    ) {
    }

    public record RollbackRelease(
            String sourceReleaseId,
            long expectedDraftRevision,
            String changeReason
    ) {
    }

    public record ReleaseView(
            String releaseId,
            String gatewayGroupId,
            long draftRevision,
            String basedOnReleaseId,
            String rollbackOfReleaseId,
            GatewayReleaseStatus status,
            boolean partialApplied,
            String changeId,
            Map<String, Object> validationReport,
            Map<String, Object> structuredDiff,
            String changeReason,
            Instant createdAt,
            Instant updatedAt,
            List<GatewayReleaseStore.AttemptRecord> attempts
    ) {
    }
}
