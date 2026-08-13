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
import top.egon.cola.platform.rbac3.admin.shared.repository.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.bootstrap.service.BootstrapQueryService;
import top.egon.cola.platform.rbac3.admin.session.service.SessionFacade;
import top.egon.cola.platform.rbac3.admin.directory.controller.DirectoryController;
import top.egon.cola.platform.rbac3.admin.identity.controller.UserDirectoryController;
import top.egon.cola.platform.rbac3.admin.tenant.controller.TenantController;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import top.egon.cola.platform.rbac3.admin.directory.service.DirectoryCommandService;
import top.egon.cola.platform.rbac3.admin.directory.service.DirectoryQueryService;
import top.egon.cola.platform.rbac3.admin.session.service.SessionManagementService;
import top.egon.cola.platform.rbac3.admin.auth.controller.AuthController;
import top.egon.cola.platform.rbac3.admin.session.controller.SessionController;

@WebMvcTest(
        controllers = {
                AuthController.class,
                TenantController.class,
                UserDirectoryController.class,
                DirectoryController.class,
                SessionController.class
        },
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
class Rbac3GatewayDefinitionDiscoveryTest {

    @Autowired
    private RequestMappingHandlerMapping handlerMappings;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BootstrapQueryService bootstrapQueryService;

    @MockitoBean
    private SessionFacade sessionFacade;

    @MockitoBean
    private DatabaseClock databaseClock;

    @MockitoBean
    private DirectoryCommandService directoryCommandPort;

    @MockitoBean
    private DirectoryQueryService directoryQueryPort;

    @MockitoBean
    private SessionManagementService sessionManagementPort;

    @Test
    void gatewayScannerDiscoversEveryTaskSevenControllerAndStableOperationPath() {
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
                        AuthController.class.getName(),
                        TenantController.class.getName(),
                        UserDirectoryController.class.getName(),
                        DirectoryController.class.getName(),
                        SessionController.class.getName()),
                methodsByController.keySet());
        assertTrue(methodsByController.get(AuthController.class.getName())
                .contains("POST /api/rbac3/v1/auth/logout"));
        assertTrue(methodsByController.get(AuthController.class.getName())
                .contains("GET /api/rbac3/v1/auth/bootstrap"));
        assertTrue(methodsByController.get(DirectoryController.class.getName())
                .contains("POST /api/rbac3/v1/internal/directory-snapshots"));
        assertTrue(methodsByController.get(SessionController.class.getName())
                .contains("POST /api/rbac3/v1/sessions/{sessionId}/revoke"));
    }
}
