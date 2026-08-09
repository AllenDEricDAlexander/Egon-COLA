package top.egon.cola.component.gateway.admin.interfaces.scheduled;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.api.client.DdcManagementClient;
import top.egon.cola.component.gateway.admin.application.projection.GatewayProjectionService;
import top.egon.cola.component.gateway.admin.application.reporting.GatewayDefinitionLifecycleStore;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayApplicationEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayApplicationRepository;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class GatewayDefinitionLifecycleReconciler {

    private final DdcManagementClient client;

    private final GatewayApplicationRepository applications;

    private final GatewayProjectionService projections;

    private final GatewayDefinitionLifecycleStore lifecycle;

    private final GatewayAuditLogRepository audits;

    private final TransactionTemplate transactions;

    private final Clock clock;

    @Autowired
    public GatewayDefinitionLifecycleReconciler(
            ObjectProvider<DdcManagementClient> client,
            GatewayApplicationRepository applications,
            GatewayProjectionService projections,
            GatewayDefinitionLifecycleStore lifecycle,
            GatewayAuditLogRepository audits,
            TransactionTemplate transactions) {
        this(
                client.getIfAvailable(),
                applications,
                projections,
                lifecycle,
                audits,
                transactions,
                Clock.systemUTC()
        );
    }

    GatewayDefinitionLifecycleReconciler(
            DdcManagementClient client,
            GatewayApplicationRepository applications,
            GatewayProjectionService projections,
            GatewayDefinitionLifecycleStore lifecycle,
            GatewayAuditLogRepository audits,
            TransactionTemplate transactions,
            Clock clock) {
        this.client = client;
        this.applications = applications;
        this.projections = projections;
        this.lifecycle = lifecycle;
        this.audits = audits;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Scheduled(
            fixedDelayString =
                    "${gateway.admin.definition-reconcile-delay:30000}"
    )
    public void reconcile() {
        if (client == null) {
            return;
        }
        Instant now = clock.instant();
        Set<Scope> scopes = new LinkedHashSet<>();
        for (GatewayApplicationEntity application
                : applications.findAllByDeletedFalseOrderByCreatedAtDesc()) {
            scopes.add(new Scope(
                    application.getBizCode(),
                    application.getApplicationCode(),
                    application.getEnv(),
                    application.getNamespace()
            ));
        }
        Set<String> activeDefinitionSets = new LinkedHashSet<>();
        for (Scope scope : scopes) {
            GatewayProjectionService.ProjectionEnvelope<
                    List<GatewayProjectionService.ProviderInstanceProjection>>
                    providers;
            try {
                providers = projections.instances(
                        scope.bizCode(),
                        scope.appCode(),
                        scope.env(),
                        scope.namespace()
                );
            } catch (RuntimeException unavailable) {
                return;
            }
            if (providers.stale()) {
                return;
            }
            providers.value().stream()
                    .filter(provider -> online(provider, now))
                    .map(GatewayProjectionService
                            .ProviderInstanceProjection::definitionSetId)
                    .filter(value -> value != null && !value.isBlank())
                    .forEach(activeDefinitionSets::add);
        }
        transactions.executeWithoutResult(status -> {
            GatewayDefinitionLifecycleStore.ReconcileResult result =
                    lifecycle.reconcile(activeDefinitionSets, now);
            if (result.changed()) {
                audits.save(audit(activeDefinitionSets, result, now));
            }
        });
    }

    private boolean online(
            GatewayProjectionService.ProviderInstanceProjection provider,
            Instant now) {
        return "ONLINE".equals(provider.status())
                && provider.expireAt() != null
                && provider.expireAt().isAfter(now);
    }

    private GatewayAuditLogEntity audit(
            Set<String> activeDefinitionSets,
            GatewayDefinitionLifecycleStore.ReconcileResult result,
            Instant now) {
        return new GatewayAuditLogEntity(
                UuidV7.simpleString(),
                "gateway-definition-reconciler",
                "SYSTEM",
                "SCHEDULED_RECONCILER",
                null,
                null,
                "DEFINITION_SET",
                "provider-active-sets",
                "RECONCILE_PROVIDER_LIFECYCLE",
                Map.of(),
                Map.of(
                        "activeDefinitionSetIds",
                        List.copyOf(activeDefinitionSets),
                        "activatedDefinitionSets",
                        result.activatedDefinitionSets(),
                        "retiredDefinitionSets",
                        result.retiredDefinitionSets(),
                        "activatedOperations",
                        result.activatedOperations(),
                        "offlinedOperations",
                        result.offlinedOperations()
                ),
                null,
                null,
                true,
                null,
                now
        );
    }

    private record Scope(
            String bizCode,
            String appCode,
            String env,
            String namespace) {
    }
}
