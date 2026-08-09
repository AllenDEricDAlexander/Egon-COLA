package top.egon.cola.component.gateway.admin.application.release;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishResult;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishStatus;
import top.egon.cola.component.gateway.admin.application.RequestAuditContext;
import top.egon.cola.component.gateway.admin.application.catalog.GatewayCatalogStore;
import top.egon.cola.component.gateway.admin.application.routing.GatewayDraftService;
import top.egon.cola.component.gateway.admin.application.routing.GatewayDraftStore;
import top.egon.cola.component.gateway.admin.domain.AdminActor;
import top.egon.cola.component.gateway.admin.domain.GatewayReleaseStatus;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogRepository;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayDraftEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayDraftRepository;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayGroupEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayGroupRepository;
import top.egon.cola.component.gateway.admin.mcp.application.McpReleaseContentFactory;
import top.egon.cola.component.gateway.admin.rule.CompiledGatewayRelease;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuleContent;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeServer;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTool;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.rule.GatewayRequestBodyMode;
import top.egon.cola.component.gateway.contract.rule.GatewayRouteProfile;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportResponseMode;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static top.egon.cola.component.gateway.admin.application.release
        .GatewayReleasePublicationStore.PublicationStatus.FAILED;
import static top.egon.cola.component.gateway.admin.application.release
        .GatewayReleasePublicationStore.PublicationStatus.SUCCESS;

class GatewayReleaseServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-07-26T08:00:00Z");

    @Test
    void activationSuccessCompletesReleaseAndAdvancesDraft() {
        Fixture fixture = fixture(outcome(SUCCESS));

        fixture.service.retry("release-1", actor(), request());

        verify(fixture.releases).completeAttempt(
                eq("release-1"),
                eq(2),
                eq(GatewayReleaseStatus.SUCCESS),
                eq(false),
                eq("018f22d8155d70008000000000000001"),
                isNull(),
                isNull(),
                eq(List.of()),
                eq(NOW)
        );
        assertThat(fixture.draft.getBasedOnReleaseId())
                .isEqualTo("release-1");
        verify(fixture.drafts).flush();
    }

    @Test
    void failedPublicationDoesNotAdvanceDraft() {
        Fixture fixture = fixture(outcome(FAILED));

        fixture.service.retry("release-1", actor(), request());

        verify(fixture.releases).completeAttempt(
                eq("release-1"),
                eq(2),
                eq(GatewayReleaseStatus.FAILED),
                eq(false),
                eq("018f22d8155d70008000000000000001"),
                eq("DDC_PUBLISH_FAILED"),
                eq("failed"),
                eq(List.of()),
                eq(NOW)
        );
        assertThat(fixture.draft.getBasedOnReleaseId()).isNull();
        verify(fixture.drafts, never()).flush();
    }

    @Test
    void createMapsLegacyDraftOntoCanonicalTypedTransportPolicy() {
        Map<String, Object> routeContent = Map.of(
                "host", "ai.example.com",
                "listener", "PUBLIC",
                "method", "POST",
                "path", "/v1/**",
                "transportPolicy", Map.of(
                        "profile", "OPENAI_HTTP",
                        "requestBodyMode", "STREAMING",
                        "responseMode", "AUTO_STREAM",
                        "connectTimeoutMs", 10_000,
                        "retryEnabled", false
                )
        );
        CreateFixture fixture = createFixture(routeContent);

        fixture.service.create(
                "group-1",
                new GatewayReleaseService.CreateRelease(
                        0L,
                        "publish transport route"
                ),
                actor(),
                request()
        );

        ArgumentCaptor<CompiledGatewayRelease> compiled =
                ArgumentCaptor.forClass(CompiledGatewayRelease.class);
        verify(fixture.releases).insert(
                any(GatewayReleaseStore.ReleaseRecord.class),
                compiled.capture(),
                eq(1)
        );
        var publishedRoute = compiled.getValue()
                .snapshot()
                .content()
                .routes()
                .getFirst();
        assertThat(publishedRoute.host()).isEqualTo("ai.example.com");
        assertThat(publishedRoute.httpMethod()).isEqualTo("POST");
        assertThat(publishedRoute.pathPattern()).isEqualTo("/v1/**");
        assertThat(publishedRoute.accessZones())
                .containsExactly(AccessZone.PUBLIC);
        assertThat(publishedRoute.transportPolicy().profile())
                .isEqualTo(GatewayRouteProfile.OPENAI_HTTP);
        assertThat(publishedRoute.transportPolicy().requestBodyMode())
                .isEqualTo(GatewayRequestBodyMode.STREAMING);
        assertThat(publishedRoute.transportPolicy().responseMode())
                .isEqualTo(GatewayTransportResponseMode.AUTO_STREAM);
        assertThat(publishedRoute.transportPolicy().connectTimeoutMs())
                .isEqualTo(10_000L);
        assertThat(publishedRoute.transportPolicy().retryEnabled()).isFalse();
    }

    @Test
    void createRejectsNonStringRouteTextBeforeBuildingASnapshot() {
        CreateFixture fixture = createFixture(Map.of(
                "host", Map.of("tenant", "x"),
                "listener", "PUBLIC",
                "method", false,
                "path", "/v1/**"
        ));

        assertThatThrownBy(() -> fixture.service.create(
                "group-1",
                new GatewayReleaseService.CreateRelease(
                        0L,
                        "publish invalid route"
                ),
                actor(),
                request()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ROUTE_HOST_INVALID")
                .hasMessageContaining("host");

        verify(fixture.releases, never()).insert(
                any(),
                any(),
                anyInt()
        );
    }

    @Test
    void createIncludesMcpDraftInTheCanonicalReleaseSnapshot() {
        McpReleaseContentFactory mcp = mock(McpReleaseContentFactory.class);
        McpRuleContent mcpContent = new McpRuleContent(
                List.of(new McpRuntimeServer(
                        "server-1",
                        "billing",
                        "Billing",
                        null,
                        null,
                        Set.of(McpProtocolDialect.STABLE_2025_11_25),
                        "gateway-mcp",
                        30,
                        true
                )),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        when(mcp.compileForRelease("group-1", 0L)).thenReturn(mcpContent);
        CreateFixture fixture = createFixture(Map.of(
                "host", "ai.example.com",
                "listener", "PUBLIC",
                "method", "POST",
                "path", "/v1/**"
        ), mcp);

        fixture.service.create(
                "group-1",
                new GatewayReleaseService.CreateRelease(
                        0L,
                        "publish Gateway and MCP"
                ),
                actor(),
                request()
        );

        ArgumentCaptor<CompiledGatewayRelease> compiled =
                ArgumentCaptor.forClass(CompiledGatewayRelease.class);
        verify(fixture.releases).insert(
                any(GatewayReleaseStore.ReleaseRecord.class),
                compiled.capture(),
                eq(1)
        );
        assertThat(compiled.getValue().snapshot().content().mcp())
                .isEqualTo(mcpContent);
        verify(mcp).compileForRelease("group-1", 0L);
    }

    @Test
    void managedToolPublishesItsOperationWithoutARoute() {
        McpReleaseContentFactory mcp = mock(McpReleaseContentFactory.class);
        McpRuleContent mcpContent = new McpRuleContent(
                List.of(new McpRuntimeServer(
                        "server-1",
                        "billing",
                        "Billing",
                        null,
                        null,
                        Set.of(McpProtocolDialect.STABLE_2025_11_25),
                        "gateway-mcp",
                        30,
                        true
                )),
                List.of(new McpRuntimeTool(
                        "tool-1",
                        "billing",
                        "orders.get",
                        "Get an order",
                        "LOCAL_OPERATION",
                        "operation-1",
                        "HTTP",
                        null,
                        "{\"type\":\"object\"}",
                        "{\"type\":\"object\"}",
                        Map.of(),
                        Set.of(),
                        "LOW",
                        true,
                        true
                )),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        when(mcp.compileForRelease("group-1", 0L)).thenReturn(mcpContent);
        CreateFixture fixture = createFixture(Map.of(
                "host", "ai.example.com",
                "listener", "PUBLIC",
                "method", "GET",
                "path", "/orders/{id}"
        ), mcp, false);

        fixture.service.create(
                "group-1",
                new GatewayReleaseService.CreateRelease(
                        0L,
                        "publish managed Tool without Route"
                ),
                actor(),
                request()
        );

        ArgumentCaptor<CompiledGatewayRelease> compiled =
                ArgumentCaptor.forClass(CompiledGatewayRelease.class);
        verify(fixture.releases).insert(
                any(GatewayReleaseStore.ReleaseRecord.class),
                compiled.capture(),
                eq(1)
        );
        assertThat(compiled.getValue().snapshot().content().routes()).isEmpty();
        assertThat(compiled.getValue().snapshot().content().operations())
                .extracting(operation -> operation.operationId())
                .containsExactly("operation-1");
    }

    private CreateFixture createFixture(Map<String, Object> routeContent) {
        return createFixture(routeContent, null);
    }

    private CreateFixture createFixture(
            Map<String, Object> routeContent,
            McpReleaseContentFactory mcpContentFactory) {
        return createFixture(routeContent, mcpContentFactory, true);
    }

    private CreateFixture createFixture(
            Map<String, Object> routeContent,
            McpReleaseContentFactory mcpContentFactory,
            boolean routeEnabled) {
        GatewayGroupRepository groups = mock(GatewayGroupRepository.class);
        GatewayDraftRepository drafts = mock(GatewayDraftRepository.class);
        GatewayDraftService draftService = mock(GatewayDraftService.class);
        GatewayCatalogStore catalog = mock(GatewayCatalogStore.class);
        GatewayReleaseStore releases = mock(GatewayReleaseStore.class);
        GatewayAuditLogRepository audits =
                mock(GatewayAuditLogRepository.class);
        GatewayDraftEntity draft = new GatewayDraftEntity(
                "group-1",
                "admin",
                NOW
        );
        GatewayGroupEntity group = new GatewayGroupEntity(
                "group-1",
                "orders",
                "Orders",
                "local",
                "default",
                null,
                "admin",
                NOW
        );
        List<GatewayDraftStore.RouteDraft> routes = routeEnabled
                ? List.of(new GatewayDraftStore.RouteDraft(
                        "group-1",
                        "route-1",
                        "operation-1",
                        routeContent,
                        true,
                        NOW,
                        "admin"
                ))
                : List.of();
        GatewayDraftService.DraftView view =
                new GatewayDraftService.DraftView(
                        "group-1",
                        0L,
                        null,
                        "EDITABLE",
                        null,
                        routes,
                        List.of(),
                        NOW
                );
        when(groups.findByIdAndDeletedFalse("group-1"))
                .thenReturn(Optional.of(group));
        when(drafts.findById("group-1")).thenReturn(Optional.of(draft));
        when(draftService.validate("group-1")).thenReturn(
                new GatewayDraftService.ValidationReport(
                        true,
                        0L,
                        List.of(),
                        List.of(),
                        "draft-sha"
                )
        );
        when(draftService.get("group-1")).thenReturn(view);
        when(catalog.findOperation("operation-1"))
                .thenReturn(Optional.of(operation()));
        when(catalog.loadDefinitions("operation-1"))
                .thenReturn(List.of(definition()));
        when(releases.find(anyString())).thenAnswer(invocation ->
                Optional.of(release(invocation.getArgument(0)))
        );
        when(releases.attempts(anyString())).thenReturn(List.of());
        GatewayReleaseService service = new GatewayReleaseService(
                groups,
                drafts,
                draftService,
                catalog,
                releases,
                audits,
                transactions(),
                null,
                mcpContentFactory,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        return new CreateFixture(service, releases);
    }

    private Fixture fixture(
            GatewayReleasePublicationCoordinator.PublicationOutcome outcome) {
        GatewayGroupRepository groups = mock(GatewayGroupRepository.class);
        GatewayDraftRepository drafts = mock(GatewayDraftRepository.class);
        GatewayDraftService draftService = mock(GatewayDraftService.class);
        GatewayCatalogStore catalog = mock(GatewayCatalogStore.class);
        GatewayReleaseStore releases = mock(GatewayReleaseStore.class);
        GatewayAuditLogRepository audits =
                mock(GatewayAuditLogRepository.class);
        GatewayReleasePublicationCoordinator publications =
                mock(GatewayReleasePublicationCoordinator.class);
        CompiledGatewayRelease compiled = mock(CompiledGatewayRelease.class);
        GatewayDraftEntity draft = new GatewayDraftEntity(
                "group-1",
                "admin",
                NOW
        );
        GatewayReleaseStore.ReleaseRecord release = release();
        when(releases.find("release-1"))
                .thenReturn(Optional.of(release));
        when(releases.nextAttempt("release-1", NOW)).thenReturn(2);
        when(releases.loadCompiled("release-1")).thenReturn(compiled);
        when(releases.attempts("release-1")).thenReturn(List.of());
        when(drafts.findById("group-1")).thenReturn(Optional.of(draft));
        when(publications.execute(
                "release-1",
                2,
                compiled,
                "admin"
        )).thenReturn(outcome);
        GatewayReleaseService service = new GatewayReleaseService(
                groups,
                drafts,
                draftService,
                catalog,
                releases,
                audits,
                transactions(),
                publications,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        return new Fixture(service, releases, drafts, draft);
    }

    private GatewayReleasePublicationCoordinator.PublicationOutcome outcome(
            GatewayReleasePublicationStore.PublicationStatus status) {
        return new GatewayReleasePublicationCoordinator.PublicationOutcome(
                status,
                "018f22d8155d70008000000000000001",
                new DdcManagementPublishResult(
                        "018f22d8155d70008000000000000001",
                        DdcManagementPublishStatus.valueOf(status.name()),
                        2L,
                        "checksum",
                        0,
                        List.of(),
                        status == SUCCESS ? null : "failed",
                        NOW,
                        NOW,
                        NOW
                ),
                false
        );
    }

    private GatewayReleaseStore.ReleaseRecord release() {
        return new GatewayReleaseStore.ReleaseRecord(
                "release-1",
                "group-1",
                1L,
                null,
                null,
                GatewayReleaseStatus.UNKNOWN,
                false,
                null,
                Map.of(),
                Map.of(),
                "retry",
                NOW,
                "admin",
                NOW
        );
    }

    private GatewayReleaseStore.ReleaseRecord release(String releaseId) {
        return new GatewayReleaseStore.ReleaseRecord(
                releaseId,
                "group-1",
                0L,
                null,
                null,
                GatewayReleaseStatus.FAILED,
                false,
                null,
                Map.of(),
                Map.of(),
                "publish transport route",
                NOW,
                "admin",
                NOW
        );
    }

    private GatewayCatalogStore.OperationRecord operation() {
        return new GatewayCatalogStore.OperationRecord(
                "operation-1",
                "application-1",
                "interface-1",
                "orders",
                "HTTP",
                "POST /v1/orders",
                true,
                Map.of(
                        "bizCode", "test-biz",
                        "appCode", "orders",
                        "env", "local",
                        "namespace", "default",
                        "serviceName", "orders",
                        "group", "default",
                        "version", "v1",
                        "transport", "http"
                ),
                "MANUAL",
                "ACTIVE",
                "definition-1",
                1L,
                NOW,
                NOW
        );
    }

    private GatewayCatalogStore.OperationDefinition definition() {
        return new GatewayCatalogStore.OperationDefinition(
                "definition-1",
                "operation-1",
                1L,
                "sha256",
                "orders",
                List.of(),
                Map.of(
                        "$schema", "https://json-schema.org/draft/2020-12/schema",
                        "x-egon-schema-model", "gateway-operation-request/v2",
                        "type", "object",
                        "properties", Map.of(),
                        "additionalProperties", false
                ),
                Map.of(
                        "$schema", "https://json-schema.org/draft/2020-12/schema",
                        "x-egon-schema-model", "gateway-operation-response/v2",
                        "type", "object",
                        "properties", Map.of(),
                        "additionalProperties", false
                ),
                List.of(),
                null,
                Map.of("responseMode", "TRANSPARENT"),
                true,
                NOW,
                "admin"
        );
    }

    private AdminActor actor() {
        return new AdminActor(
                "admin",
                AdminActor.ActorType.USER,
                Set.of(),
                Set.of()
        );
    }

    private RequestAuditContext request() {
        return new RequestAuditContext("request-1", "trace-1");
    }

    private TransactionTemplate transactions() {
        return new TransactionTemplate(new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(
                    TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
            }

            @Override
            public void rollback(TransactionStatus status) {
            }
        });
    }

    private record Fixture(
            GatewayReleaseService service,
            GatewayReleaseStore releases,
            GatewayDraftRepository drafts,
            GatewayDraftEntity draft
    ) {
    }

    private record CreateFixture(
            GatewayReleaseService service,
            GatewayReleaseStore releases
    ) {
    }
}
