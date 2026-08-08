package top.egon.cola.platform.rbac3.admin.interfaces.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import top.egon.cola.component.gateway.starter.GatewayReportingProperties;
import top.egon.cola.component.gateway.starter.discovery.http.MvcGatewayDefinitionContributor;
import top.egon.cola.platform.rbac3.admin.audit.application.AuditQueryService;
import top.egon.cola.platform.rbac3.admin.authorization.application.AuthorizationDecisionService;
import top.egon.cola.platform.rbac3.admin.participation.application.ParticipationFacade;
import top.egon.cola.platform.rbac3.admin.runtime.application.RuntimeQueryService;
import top.egon.cola.platform.rbac3.admin.simulation.application.AuthorizationSimulationService;
import top.egon.cola.platform.rbac3.admin.snapshot.application.SystemAuthorizationSnapshotService;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(
        controllers = {
                InternalAuthorizationController.class,
                ParticipationController.class,
                AuditSimulationController.class,
                RuntimeController.class
        },
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
class Rbac3DecisionRuntimeGatewayDiscoveryTest {

    @Autowired
    private RequestMappingHandlerMapping handlerMappings;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthorizationDecisionService decisionService;

    @MockitoBean
    private SystemAuthorizationSnapshotService systemAuthorizationSnapshotService;

    @MockitoBean
    private ParticipationFacade participationFacade;

    @MockitoBean
    private AuditQueryService auditQueryService;

    @MockitoBean
    private AuthorizationSimulationService simulationService;

    @MockitoBean
    private RuntimeQueryService runtimeQueryService;

    @Test
    void discoversInternalDecisionParticipationAuditSimulationAndRuntimeRoutes() {
        GatewayReportingProperties properties = new GatewayReportingProperties();
        properties.setBizCode("rbac3");
        properties.setApplicationCode("rbac3-admin");
        properties.setEnv("test");
        properties.setNamespace("default");
        properties.setArtifactVersion("5.3.2");

        Map<String, Set<String>> methods = new MvcGatewayDefinitionContributor(
                handlerMappings, properties, objectMapper).discover().stream()
                .collect(Collectors.toMap(
                        group -> group.interfaceGroup().className(),
                        group -> group.interfaceGroup().operations().stream()
                                .map(operation -> operation.methodIdentity())
                                .collect(Collectors.toSet())));

        assertThat(methods.get(InternalAuthorizationController.class.getName()))
                .contains(
                        "GET /internal/v1/authorization/contexts/{tenantId}/{sessionId}",
                        "GET /internal/v1/authorization/sessions/{sessionId}/snapshot",
                        "POST /internal/v1/authorization/decisions",
                        "POST /internal/v1/authorization/fences/verify");
        assertThat(methods.get(ParticipationController.class.getName()))
                .contains(
                        "POST /api/rbac3/v1/internal/business-participations",
                        "GET /api/rbac3/v1/internal/business-participations/conflicts");
        assertThat(methods.get(AuditSimulationController.class.getName()))
                .contains(
                        "GET /api/rbac3/v1/audit-logs",
                        "POST /api/rbac3/v1/simulations/authorization",
                        "POST /api/rbac3/v1/simulations/role-change-impact");
        assertThat(methods.get(RuntimeController.class.getName()))
                .contains(
                        "GET /api/rbac3/v1/runtime/status",
                        "GET /api/rbac3/v1/runtime/mutations",
                        "POST /api/rbac3/v1/runtime/mutations/{mutationId}/retry",
                        "GET /api/rbac3/v1/runtime/gateway-ddc-status");
    }
}
