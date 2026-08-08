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
import top.egon.cola.platform.rbac3.admin.application.port.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.assignment.application.AssignmentFacade;
import top.egon.cola.platform.rbac3.admin.management.application.ManagementPolicyFacade;
import top.egon.cola.platform.rbac3.admin.runtime.application.IdempotencyService;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@WebMvcTest(
        controllers = {AssignmentController.class, ManagementPolicyController.class},
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
class Rbac3AssignmentManagementGatewayDiscoveryTest {

    @Autowired
    private RequestMappingHandlerMapping handlerMappings;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AssignmentFacade assignmentFacade;

    @MockitoBean
    private ManagementPolicyFacade managementPolicyFacade;

    @MockitoBean
    private IdempotencyService idempotencyService;

    @MockitoBean
    private AssignmentController.SessionStrengthPort sessionStrengthPort;

    @MockitoBean
    private DatabaseClock databaseClock;

    @Test
    void gatewayScannerDiscoversAssignmentAndManagementPolicyRoutes() {
        GatewayReportingProperties properties = new GatewayReportingProperties();
        properties.setBizCode("rbac3");
        properties.setApplicationCode("rbac3-admin");
        properties.setEnv("test");
        properties.setNamespace("default");
        properties.setArtifactVersion("5.3.2");

        var groups = new MvcGatewayDefinitionContributor(
                handlerMappings, properties, objectMapper).discover();
        Map<String, Set<String>> methodsByController = groups.stream()
                .collect(Collectors.toMap(
                        group -> group.interfaceGroup().className(),
                        group -> group.interfaceGroup().operations().stream()
                                .map(operation -> operation.methodIdentity())
                                .collect(Collectors.toSet())));

        assertEquals(Set.of(
                        AssignmentController.class.getName(),
                        ManagementPolicyController.class.getName()),
                methodsByController.keySet());
        assertTrue(methodsByController.get(AssignmentController.class.getName())
                .contains("POST /api/rbac3/v1/users/{userId}/role-assignments"));
        assertTrue(methodsByController.get(AssignmentController.class.getName())
                .contains("POST /api/rbac3/v1/users/{userId}/role-assignments/{assignmentId}/revoke"));
        assertTrue(methodsByController.get(ManagementPolicyController.class.getName())
                .contains("PUT /api/rbac3/v1/management-policies/{policyId}"));
        assertTrue(methodsByController.get(ManagementPolicyController.class.getName())
                .contains("POST /api/rbac3/v1/management-policies/{policyId}/disable"));
        assertTrue(methodsByController.get(ManagementPolicyController.class.getName())
                .contains("GET /api/rbac3/v1/management-capabilities/me"));
        assertTrue(methodsByController.get(ManagementPolicyController.class.getName())
                .contains("GET /api/rbac3/v1/manageable-users"));
        assertTrue(methodsByController.get(ManagementPolicyController.class.getName())
                .contains("GET /api/rbac3/v1/manageable-roles"));
    }
}
