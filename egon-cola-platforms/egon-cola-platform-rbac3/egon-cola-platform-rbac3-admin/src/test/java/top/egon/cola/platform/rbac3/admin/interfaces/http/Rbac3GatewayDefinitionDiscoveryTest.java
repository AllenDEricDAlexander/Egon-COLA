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
import top.egon.cola.platform.rbac3.admin.application.port.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.auth.application.AuthenticationFacade;
import top.egon.cola.platform.rbac3.admin.auth.application.JwtKeyRingService;
import top.egon.cola.platform.rbac3.admin.auth.application.RefreshFacade;
import top.egon.cola.platform.rbac3.admin.bootstrap.application.BootstrapQueryService;
import top.egon.cola.platform.rbac3.admin.session.application.SessionFacade;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@WebMvcTest(
        controllers = {
                AuthController.class,
                TenantUserDirectoryController.class,
                SessionController.class
        },
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
class Rbac3GatewayDefinitionDiscoveryTest {

    @Autowired
    private RequestMappingHandlerMapping handlerMappings;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationFacade authenticationFacade;

    @MockitoBean
    private RefreshFacade refreshFacade;

    @MockitoBean
    private BootstrapQueryService bootstrapQueryService;

    @MockitoBean
    private SessionFacade sessionFacade;

    @MockitoBean
    private JwtKeyRingService jwtKeyRingService;

    @MockitoBean
    private DatabaseClock databaseClock;

    @MockitoBean
    private TenantUserDirectoryController.DirectoryCommandPort directoryCommandPort;

    @MockitoBean
    private TenantUserDirectoryController.DirectoryQueryPort directoryQueryPort;

    @MockitoBean
    private SessionController.SessionManagementPort sessionManagementPort;

    @Test
    void gatewayScannerDiscoversEveryTaskSevenControllerAndStableOperationPath() {
        GatewayReportingProperties properties = new GatewayReportingProperties();
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
                        TenantUserDirectoryController.class.getName(),
                        SessionController.class.getName()),
                methodsByController.keySet());
        assertTrue(methodsByController.get(AuthController.class.getName())
                .contains("POST /api/rbac3/v1/auth/login"));
        assertTrue(methodsByController.get(AuthController.class.getName())
                .contains("GET /api/rbac3/v1/auth/jwks"));
        assertTrue(methodsByController.get(TenantUserDirectoryController.class.getName())
                .contains("POST /api/rbac3/v1/directory/snapshots"));
        assertTrue(methodsByController.get(SessionController.class.getName())
                .contains("DELETE /api/rbac3/v1/sessions/{sessionId}"));
    }
}
