package top.egon.cola.component.gateway.admin.interfaces.scheduled;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.ddc.management.DdcManagementClient;
import top.egon.cola.component.gateway.admin.application.projection.GatewayProjectionService;
import top.egon.cola.component.gateway.admin.application.reporting.GatewayDefinitionLifecycleStore;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayApplicationEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayApplicationRepository;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GatewayDefinitionLifecycleReconcilerTest {

    @Test
    void reconcilesOnlyOnlineProviderDefinitionSetsAndAuditsChanges() {
        Instant now = Instant.parse("2026-07-25T08:00:00Z");
        GatewayApplicationRepository applications =
                mock(GatewayApplicationRepository.class);
        GatewayProjectionService projections =
                mock(GatewayProjectionService.class);
        GatewayDefinitionLifecycleStore lifecycle =
                mock(GatewayDefinitionLifecycleStore.class);
        GatewayAuditLogRepository audits =
                mock(GatewayAuditLogRepository.class);
        TransactionTemplate transactions = mock(TransactionTemplate.class);
        doAnswer(invocation -> {
            java.util.function.Consumer<org.springframework.transaction
                    .TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(null);
            return null;
        }).when(transactions).executeWithoutResult(any());
        when(applications.findAllByDeletedFalseOrderByCreatedAtDesc())
                .thenReturn(List.of(new GatewayApplicationEntity(
                        "application-1",
                        "orders",
                        "Orders",
                        "test",
                        "gateway",
                        null,
                        "admin",
                        now
                )));
        when(projections.instances("test", "gateway")).thenReturn(
                new GatewayProjectionService.ProjectionEnvelope<>(
                        List.of(provider(
                                "provider-1",
                                "definition-1",
                                "ONLINE",
                                now.plusSeconds(30)
                        )),
                        now,
                        "DDC_SERVICE_REGISTRY",
                        false,
                        null
                )
        );
        when(lifecycle.reconcile(eq(java.util.Set.of("definition-1")), eq(now)))
                .thenReturn(new GatewayDefinitionLifecycleStore
                        .ReconcileResult(1, 1, 2, 1));
        GatewayDefinitionLifecycleReconciler reconciler =
                new GatewayDefinitionLifecycleReconciler(
                        mock(DdcManagementClient.class),
                        applications,
                        projections,
                        lifecycle,
                        audits,
                        transactions,
                        Clock.fixed(now, ZoneOffset.UTC)
                );

        reconciler.reconcile();

        verify(lifecycle).reconcile(
                java.util.Set.of("definition-1"),
                now
        );
        verify(audits).save(any());
    }

    @Test
    void staleProviderProjectionCannotRetireDefinitions() {
        Instant now = Instant.parse("2026-07-25T08:00:00Z");
        GatewayApplicationRepository applications =
                mock(GatewayApplicationRepository.class);
        GatewayProjectionService projections =
                mock(GatewayProjectionService.class);
        GatewayDefinitionLifecycleStore lifecycle =
                mock(GatewayDefinitionLifecycleStore.class);
        when(applications.findAllByDeletedFalseOrderByCreatedAtDesc())
                .thenReturn(List.of(new GatewayApplicationEntity(
                        "application-1",
                        "orders",
                        "Orders",
                        "test",
                        "gateway",
                        null,
                        "admin",
                        now
                )));
        when(projections.instances("test", "gateway")).thenReturn(
                new GatewayProjectionService.ProjectionEnvelope<>(
                        List.of(),
                        now.minusSeconds(30),
                        "DDC_SERVICE_REGISTRY",
                        true,
                        "unavailable"
                )
        );
        GatewayDefinitionLifecycleReconciler reconciler =
                new GatewayDefinitionLifecycleReconciler(
                        mock(DdcManagementClient.class),
                        applications,
                        projections,
                        lifecycle,
                        mock(GatewayAuditLogRepository.class),
                        mock(TransactionTemplate.class),
                        Clock.fixed(now, ZoneOffset.UTC)
                );

        reconciler.reconcile();

        verify(lifecycle, never()).reconcile(any(), any());
    }

    private GatewayProjectionService.ProviderInstanceProjection provider(
            String instanceId,
            String definitionSetId,
            String status,
            Instant expireAt) {
        return new GatewayProjectionService.ProviderInstanceProjection(
                "HTTP_PROVIDER:HTTP:orders:default:1.0.0",
                "HTTP",
                "orders",
                "default",
                "1.0.0",
                instanceId,
                "lease-" + instanceId,
                "127.0.0.1",
                18080,
                null,
                "zone-a",
                100,
                Map.of("gateway.definition-set-id", definitionSetId),
                definitionSetId,
                status,
                expireAt,
                expireAt.minusSeconds(30)
        );
    }
}
