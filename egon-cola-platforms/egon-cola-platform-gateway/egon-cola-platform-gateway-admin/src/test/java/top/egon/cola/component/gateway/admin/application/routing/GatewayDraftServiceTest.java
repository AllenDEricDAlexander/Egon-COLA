package top.egon.cola.component.gateway.admin.routing.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import top.egon.cola.component.gateway.admin.shared.domain.exception.GatewayAdminIdempotencyConflictException;
import top.egon.cola.component.gateway.admin.shared.repository.IdempotencyRepository;
import top.egon.cola.component.gateway.admin.shared.domain.RequestAuditContext;
import top.egon.cola.component.gateway.admin.catalog.repository.GatewayCatalogRepository;
import top.egon.cola.component.gateway.admin.shared.domain.AdminActor;
import top.egon.cola.component.gateway.admin.observability.repository.GatewayAuditLogRepository;
import top.egon.cola.component.gateway.admin.routing.domain.po.GatewayDraftPO;
import top.egon.cola.component.gateway.admin.routing.repository.GatewayDraftRepository;
import top.egon.cola.component.gateway.admin.routing.repository.GatewayDraftJpaRepository;
import top.egon.cola.component.gateway.admin.rule.service.GatewayRuleCanonicalizer;

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
        AtomicReference<top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO> saved =
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
        top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftMutationResultVO replay = fixture.service.putRoute(
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

        ArgumentCaptor<top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO> route =
                ArgumentCaptor.forClass(top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO.class);
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
        top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayRouteMutationDTO command = mutation(legacy);
        when(fixture.idempotency.find(
                "GATEWAY_DRAFT",
                "group-1",
                "idem-1"
        )).thenReturn(Optional.of(legacyRecord(
                legacyDigest("route-1", command)
        )));

        top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftMutationResultVO result = fixture.service.putRoute(
                "group-1",
                "route-1",
                command,
                actor(),
                request()
        );

        assertThat(result).isEqualTo(
                new top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftMutationResultVO(4L, "route-1", true)
        );
        verify(fixture.store, never()).upsertRoute(any());
    }

    @Test
    void conflictsWhenRawCommandDiffersFromThePreUpgradeDigest() {
        Fixture fixture = fixture();
        top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayRouteMutationDTO original = mutation(
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
                new top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO(
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

        top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationReportVO report =
                fixture.service.validate("group-1");

        assertThat(report.valid()).isFalse();
        assertThat(report.errors()).contains(
                new top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationIssueVO(
                        "routes.route-1.host",
                        "ROUTE_HOST_REQUIRED",
                        "Host is required"
                ),
                new top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationIssueVO(
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
                new top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO(
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

        top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationReportVO report =
                fixture.service.validate("group-1");

        assertThat(report.valid()).isFalse();
        assertThat(report.errors()).contains(
                new top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationIssueVO(
                        "routes.route-1.host",
                        "ROUTE_HOST_INVALID",
                        "Host must be a string"
                ),
                new top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationIssueVO(
                        "routes.route-1.httpMethod",
                        "ROUTE_METHOD_INVALID",
                        "HTTP Method must be a string"
                )
        );
    }

    private Fixture fixture() {
        GatewayDraftJpaRepository drafts = mock(GatewayDraftJpaRepository.class);
        GatewayDraftRepository store = mock(GatewayDraftRepository.class);
        GatewayCatalogRepository catalog = mock(GatewayCatalogRepository.class);
        IdempotencyRepository idempotency = mock(IdempotencyRepository.class);
        GatewayAuditLogRepository audits =
                mock(GatewayAuditLogRepository.class);
        GatewayDraftPO draft = new GatewayDraftPO(
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

    private top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayRouteMutationDTO mutation(
            Map<String, Object> content) {
        return new top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayRouteMutationDTO(
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
            top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayRouteMutationDTO command) {
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

    private top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO legacyRecord(String digest) {
        return new top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO(
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

    private top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO operation() {
        return new top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO(
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

    private top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO definition() {
        return new top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO(
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
                top.egon.cola.component.gateway.admin.shared.domain.enums.AdminActorTypeEnum.USER,
                Set.of(),
                Set.of()
        );
    }

    private RequestAuditContext request() {
        return new RequestAuditContext("request-1", "trace-1");
    }

    private record Fixture(
            GatewayDraftService service,
            GatewayDraftRepository store,
            IdempotencyRepository idempotency
    ) {
    }
}
