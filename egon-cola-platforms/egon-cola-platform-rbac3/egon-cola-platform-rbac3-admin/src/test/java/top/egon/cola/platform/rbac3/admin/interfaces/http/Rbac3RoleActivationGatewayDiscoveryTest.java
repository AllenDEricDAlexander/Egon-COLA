package top.egon.cola.platform.rbac3.admin.interfaces.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import top.egon.cola.component.gateway.starter.GatewayReportingProperties;
import top.egon.cola.component.gateway.starter.discovery.MvcGatewayDefinitionContributor;
import top.egon.cola.platform.rbac3.admin.activation.application.RoleActivationCandidateService;
import top.egon.cola.platform.rbac3.admin.activation.application.RoleActivationFacade;
import top.egon.cola.platform.rbac3.admin.application.port.DatabaseClock;

import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(
        controllers = RoleActivationController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
class Rbac3RoleActivationGatewayDiscoveryTest {

    @Autowired
    private RequestMappingHandlerMapping handlerMappings;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RoleActivationCandidateService candidateService;

    @MockitoBean
    private RoleActivationFacade roleActivationFacade;

    @MockitoBean
    private DatabaseClock databaseClock;

    @Test
    void reportsEveryRoleActivationOperationWithStableNames() {
        GatewayReportingProperties properties = new GatewayReportingProperties();
        properties.setApplicationCode("rbac3-admin");
        properties.setEnv("test");
        properties.setNamespace("default");
        properties.setArtifactVersion("5.3.2");

        var group = new MvcGatewayDefinitionContributor(
                handlerMappings, properties, objectMapper).discover().getFirst();
        Map<String, String> namesByMethod = group.interfaceGroup().operations().stream()
                .collect(Collectors.toMap(
                        operation -> operation.methodIdentity(),
                        operation -> operation.name()));

        assertThat(namesByMethod).containsExactlyInAnyOrderEntriesOf(Map.of(
                "GET /api/rbac3/v1/role-activation/candidates",
                "rbac3-role-activation-candidates-v1",
                "GET /api/rbac3/v1/role-activation/current",
                "rbac3-role-activation-current-v1",
                "PUT /api/rbac3/v1/role-activation/current",
                "rbac3-role-activation-replace-v1"));
    }
}
