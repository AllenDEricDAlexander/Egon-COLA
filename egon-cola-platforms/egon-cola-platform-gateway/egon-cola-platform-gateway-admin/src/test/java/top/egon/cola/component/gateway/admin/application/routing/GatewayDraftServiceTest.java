package top.egon.cola.component.gateway.admin.application.routing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import top.egon.cola.component.gateway.admin.application.GatewayAdminIdempotencyConflictException;
import top.egon.cola.component.gateway.admin.application.IdempotencyStore;
import top.egon.cola.component.gateway.admin.application.RequestAuditContext;
import top.egon.cola.component.gateway.admin.application.catalog.GatewayCatalogStore;
import top.egon.cola.component.gateway.admin.domain.AdminActor;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogRepository;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayDraftEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayDraftRepository;
import top.egon.cola.component.gateway.admin.rule.GatewayRuleCanonicalizer;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GatewayDraftServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-07-30T08:00:00Z");

    @Test
    void normalizesBeforeDigestAndStorage() {
        Fixture fixture = fixture();
        AtomicReference<IdempotencyStore.Record> saved =
                new AtomicReference<>();
        when(fixture.idempotency.find(
                "GATEWAY_DRAFT",
                "group-1",
                "idem-1"
        )).thenAnswer(ignored -> Optional.ofNullable(saved.get()));
        doAnswer(invocation -> {
            saved.set(invocation.getArgument(0));
            return null;
        }).when(fixture.idempotency).save(any());
        Map<String, Object> legacy = new LinkedHashMap<>();
        legacy.put("host", "ai.example.com");
        legacy.put("listener", "PUBLIC");
        legacy.put("method", "POST");
        legacy.put("path", "/v1/**");
        legacy.put("customExtension", Map.of("enabled", false));

        fixture.service.putRoute(
                "group-1",
                "route-1",
                mutation(legacy),
                actor(),
                request()
        );
        GatewayDraftService.MutationResult replay = fixture.service.putRoute(
                "group-1",
                "route-1",
                mutation(Map.of(
                        "host", "ai.example.com",
                        "httpMethod", "POST",
                        "pathPattern", "/v1/**",
                        "accessZones", List.of("PUBLIC"),
                        "priority", 0,
                        "customExtension", Map.of("enabled", false)
                )),
                actor(),
                request()
        );

        ArgumentCaptor<GatewayDraftStore.RouteDraft> route =
                ArgumentCaptor.forClass(GatewayDraftStore.RouteDraft.class);
        verify(fixture.store).upsertRoute(route.capture());
        assertThat(route.getValue().content()).isEqualTo(Map.of(
                "host", "ai.example.com",
                "httpMethod", "POST",
                "pathPattern", "/v1/**",
                "accessZones", List.of("PUBLIC"),
                "priority", 0,
                "customExtension", Map.of("enabled", false)
        ));
        assertThat(replay.replayed()).isTrue();
    }

    @Test
    void replaysARequestStoredWithThePreUpgradeRawCommandDigest() {
        Fixture fixture = fixture();
        Map<String, Object> legacy = legacyRoute("/v1/**");
        GatewayDraftService.RouteMutation command = mutation(legacy);
        when(fixture.idempotency.find(
                "GATEWAY_DRAFT",
                "group-1",
                "idem-1"
        )).thenReturn(Optional.of(legacyRecord(
                legacyDigest("route-1", command)
        )));

        GatewayDraftService.MutationResult result = fixture.service.putRoute(
                "group-1",
                "route-1",
                command,
                actor(),
                request()
        );

        assertThat(result).isEqualTo(
                new GatewayDraftService.MutationResult(4L, "route-1", true)
        );
        verify(fixture.store, never()).upsertRoute(any());
    }

    @Test
    void conflictsWhenRawCommandDiffersFromThePreUpgradeDigest() {
        Fixture fixture = fixture();
        GatewayDraftService.RouteMutation original = mutation(
                legacyRoute("/v1/**")
        );
        when(fixture.idempotency.find(
                "GATEWAY_DRAFT",
                "group-1",
                "idem-1"
        )).thenReturn(Optional.of(legacyRecord(
                legacyDigest("route-1", original)
        )));

        assertThatThrownBy(() -> fixture.service.putRoute(
                "group-1",
                "route-1",
                mutation(legacyRoute("/v2/**")),
                actor(),
                request()
        )).isInstanceOf(GatewayAdminIdempotencyConflictException.class);

        verify(fixture.store, never()).upsertRoute(any());
    }

    @Test
    void reportsRouteTransportErrorsAtDraftFieldPaths() {
        Fixture fixture = fixture();
        when(fixture.store.routes("group-1")).thenReturn(List.of(
                new GatewayDraftStore.RouteDraft(
                        "group-1",
                        "route-1",
                        "operation-1",
                        Map.of(
                                "listener", "PUBLIC",
                                "method", "POST",
                                "path", "/v1/**",
                                "transportPolicy", Map.of(
                                        "connectTimeoutMs", 99
                                )
                        ),
                        true,
                        NOW,
                        "admin"
                )
        ));
        when(fixture.store.policies("group-1")).thenReturn(List.of());

        GatewayDraftService.ValidationReport report =
                fixture.service.validate("group-1");

        assertThat(report.valid()).isFalse();
        assertThat(report.errors()).contains(
                new GatewayDraftService.ValidationIssue(
                        "routes.route-1.host",
                        "ROUTE_HOST_REQUIRED",
                        "Host is required"
                ),
                new GatewayDraftService.ValidationIssue(
                        "routes.route-1.transportPolicy.connectTimeoutMs",
                        "TRANSPORT_VALUE_OUT_OF_RANGE",
                        "connectTimeoutMs must be an integer from 100 to 60000"
                )
        );
    }

    @Test
    void reportsNonStringTextFieldsFromJacksonBoundContent() throws Exception {
        Fixture fixture = fixture();
        Map<String, Object> content = new ObjectMapper().readValue(
                """
                        {
                          "host": {"tenant": "x"},
                          "listener": "PUBLIC",
                          "method": false,
                          "path": "/v1/**",
                          "futureFalse": false,
                          "futureNumber": 7,
                          "futureNull": null
                        }
                        """,
                new TypeReference<>() {
                }
        );
        when(fixture.store.routes("group-1")).thenReturn(List.of(
                new GatewayDraftStore.RouteDraft(
                        "group-1",
                        "route-1",
                        "operation-1",
                        content,
                        true,
                        NOW,
                        "admin"
                )
        ));
        when(fixture.store.policies("group-1")).thenReturn(List.of());

        GatewayDraftService.ValidationReport report =
                fixture.service.validate("group-1");

        assertThat(report.valid()).isFalse();
        assertThat(report.errors()).contains(
                new GatewayDraftService.ValidationIssue(
                        "routes.route-1.host",
                        "ROUTE_HOST_INVALID",
                        "Host must be a string"
                ),
                new GatewayDraftService.ValidationIssue(
                        "routes.route-1.httpMethod",
                        "ROUTE_METHOD_INVALID",
                        "HTTP Method must be a string"
                )
        );
    }

    private Fixture fixture() {
        GatewayDraftRepository drafts = mock(GatewayDraftRepository.class);
        GatewayDraftStore store = mock(GatewayDraftStore.class);
        GatewayCatalogStore catalog = mock(GatewayCatalogStore.class);
        IdempotencyStore idempotency = mock(IdempotencyStore.class);
        GatewayAuditLogRepository audits =
                mock(GatewayAuditLogRepository.class);
        GatewayDraftEntity draft = new GatewayDraftEntity(
                "group-1",
                "admin",
                NOW
        );
        when(drafts.findById("group-1")).thenReturn(Optional.of(draft));
        when(catalog.findOperation("operation-1"))
                .thenReturn(Optional.of(operation()));
        when(catalog.loadDefinitions("operation-1"))
                .thenReturn(List.of(definition()));
        GatewayDraftService service = new GatewayDraftService(
                drafts,
                store,
                catalog,
                idempotency,
                audits,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        return new Fixture(service, store, idempotency);
    }

    private GatewayDraftService.RouteMutation mutation(
            Map<String, Object> content) {
        return new GatewayDraftService.RouteMutation(
                "operation-1",
                content,
                true,
                0L,
                "idem-1",
                "configure route"
        );
    }

    private Map<String, Object> legacyRoute(String path) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("host", "ai.example.com");
        content.put("listener", "PUBLIC");
        content.put("method", "POST");
        content.put("path", path);
        return content;
    }

    private String legacyDigest(
            String routeId,
            GatewayDraftService.RouteMutation command) {
        GatewayRuleCanonicalizer canonicalizer =
                new GatewayRuleCanonicalizer();
        return GatewayRuleCanonicalizer.sha256(
                canonicalizer.canonicalBytes(Map.of(
                        "action", "PUT_ROUTE",
                        "routeId", routeId,
                        "command", command
                ))
        );
    }

    private IdempotencyStore.Record legacyRecord(String digest) {
        return new IdempotencyStore.Record(
                "GATEWAY_DRAFT",
                "group-1",
                "idem-1",
                digest,
                "route-1",
                Map.of("revision", 4L, "resourceId", "route-1"),
                NOW,
                NOW.plusSeconds(604_800)
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
                Map.of("type", "object"),
                Map.of("type", "object"),
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

    private record Fixture(
            GatewayDraftService service,
            GatewayDraftStore store,
            IdempotencyStore idempotency
    ) {
    }
}
