package top.egon.cola.component.gateway.admin.routing.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import top.egon.cola.component.gateway.admin.shared.repository.IdempotencyRepository;
import top.egon.cola.component.gateway.admin.catalog.repository.GatewayCatalogRepository;
import top.egon.cola.component.gateway.admin.shared.domain.AdminActor;
import top.egon.cola.component.gateway.admin.observability.repository.GatewayAuditLogRepository;
import top.egon.cola.component.gateway.admin.routing.domain.po.GatewayDraftPO;
import top.egon.cola.component.gateway.admin.routing.repository.GatewayDraftJpaRepository;
import top.egon.cola.component.gateway.admin.routing.repository.GatewayDraftRepository;
import top.egon.cola.component.gateway.admin.shared.controller.GatewayAdminExceptionHandler;
import top.egon.cola.component.gateway.admin.routing.controller.GatewayDraftController;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GatewayDraftTransportWorkflowTest {

    private static final Instant NOW =
            Instant.parse("2026-07-30T08:00:00Z");

    @Test
    void savesReadsAndValidatesCanonicalOpenAiTransportPolicy()
            throws Exception {
        Fixture fixture = fixture();

        fixture.mockMvc.perform(put(
                        "/api/v1/gateway/admin/gateway-groups/group-1"
                                + "/draft/routes/route-1"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operationId": "operation-1",
                                  "content": {
                                    "host": "ai.example.com",
                                    "listener": "PUBLIC",
                                    "method": "POST",
                                    "path": "/v1/**",
                                    "transportPolicy": {
                                      "profile": "OPENAI_HTTP",
                                      "transportProtocol": "HTTP",
                                      "requestBodyMode": "STREAMING",
                                      "responseMode": "AUTO_STREAM",
                                      "maxRequestBodyBytes": 536870912,
                                      "connectTimeoutMs": 10000,
                                      "responseHeaderTimeoutMs": 120000,
                                      "streamIdleTimeoutMs": 90000,
                                      "totalTimeoutMs": 1800000,
                                      "bodyLogEnabled": false,
                                      "retryEnabled": false,
                                      "futureOption": false
                                    }
                                  },
                                  "enabled": true,
                                  "expectedRevision": 0,
                                  "idempotencyKey": "idem-1",
                                  "changeReason": "configure OpenAI route"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceId").value("route-1"));

        fixture.mockMvc.perform(get(
                        "/api/v1/gateway/admin/gateway-groups/group-1/draft"
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routes[0].content.httpMethod")
                        .value("POST"))
                .andExpect(jsonPath("$.routes[0].content.pathPattern")
                        .value("/v1/**"))
                .andExpect(jsonPath("$.routes[0].content.accessZones[0]")
                        .value("PUBLIC"))
                .andExpect(jsonPath(
                        "$.routes[0].content.transportPolicy.profile"
                ).value("OPENAI_HTTP"))
                .andExpect(jsonPath(
                        "$.routes[0].content.transportPolicy.bodyLogEnabled"
                ).value(false))
                .andExpect(jsonPath(
                        "$.routes[0].content.transportPolicy.retryEnabled"
                ).value(false))
                .andExpect(jsonPath(
                        "$.routes[0].content.transportPolicy.futureOption"
                ).value(false))
                .andExpect(jsonPath("$.routes[0].content.listener")
                        .doesNotExist())
                .andExpect(jsonPath("$.routes[0].content.model")
                        .doesNotExist())
                .andExpect(jsonPath("$.routes[0].content.tokenLimit")
                        .doesNotExist());

        fixture.mockMvc.perform(post(
                        "/api/v1/gateway/admin/gateway-groups/group-1"
                                + "/draft/validate"
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void reportsMissingHostThroughTheManagementWorkflow()
            throws Exception {
        Fixture fixture = fixture();

        fixture.mockMvc.perform(put(
                        "/api/v1/gateway/admin/gateway-groups/group-1"
                                + "/draft/routes/route-1"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operationId": "operation-1",
                                  "content": {
                                    "listener": "PUBLIC",
                                    "method": "POST",
                                    "path": "/v1/**"
                                  },
                                  "enabled": true,
                                  "expectedRevision": 0,
                                  "idempotencyKey": "idem-1",
                                  "changeReason": "import legacy route"
                                }
                                """))
                .andExpect(status().isOk());

        fixture.mockMvc.perform(post(
                        "/api/v1/gateway/admin/gateway-groups/group-1"
                                + "/draft/validate"
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errors[0].path")
                        .value("routes.route-1.host"))
                .andExpect(jsonPath("$.errors[0].code")
                        .value("ROUTE_HOST_REQUIRED"));
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
        AtomicReference<top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO> route =
                new AtomicReference<>();
        when(drafts.findById("group-1")).thenReturn(Optional.of(draft));
        when(store.routes("group-1")).thenAnswer(ignored ->
                route.get() == null ? List.of() : List.of(route.get()));
        when(store.policies("group-1")).thenReturn(List.of());
        doAnswer(invocation -> {
            route.set(invocation.getArgument(0));
            return null;
        }).when(store).upsertRoute(any());
        when(idempotency.find("GATEWAY_DRAFT", "group-1", "idem-1"))
                .thenReturn(Optional.empty());
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
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                        new GatewayDraftController(service)
                )
                .setCustomArgumentResolvers(new FixedActorResolver())
                .setControllerAdvice(new GatewayAdminExceptionHandler())
                .build();
        return new Fixture(mockMvc);
    }

    private top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO operation() {
        return new top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO(
                "operation-1",
                "application-1",
                "interface-1",
                "openai",
                "HTTP",
                "POST /v1/**",
                true,
                Map.of(
                        "env", "local",
                        "namespace", "default",
                        "serviceName", "openai-compatible-provider",
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
                "openai",
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

    private record Fixture(MockMvc mockMvc) {
    }

    private static final class FixedActorResolver
            implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return AdminActor.class.equals(parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(
                MethodParameter parameter,
                ModelAndViewContainer mavContainer,
                NativeWebRequest webRequest,
                WebDataBinderFactory binderFactory) {
            return new AdminActor(
                    "admin",
                    top.egon.cola.component.gateway.admin.shared.domain.enums.AdminActorTypeEnum.USER,
                    Set.of("gateway:drafts:write"),
                    Set.of()
            );
        }
    }
}
