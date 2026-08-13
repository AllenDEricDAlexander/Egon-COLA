package top.egon.cola.platform.rbac3.admin.interfaces.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.component.gateway.starter.GatewayReportingProperties;
import top.egon.cola.component.gateway.starter.discovery.http.MvcGatewayDefinitionContributor;
import top.egon.cola.platform.rbac3.admin.shared.repository.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.constraint.service.ConstraintFacade;
import top.egon.cola.platform.rbac3.admin.resource.service.ApplicationResourceFacade;
import top.egon.cola.platform.rbac3.admin.resource.service.ManifestFacade;
import top.egon.cola.platform.rbac3.admin.role.service.RoleFacade;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import top.egon.cola.platform.rbac3.admin.resource.controller.ApplicationResourceController;
import top.egon.cola.platform.rbac3.admin.resource.controller.ManifestController;
import top.egon.cola.platform.rbac3.admin.role.controller.RolePermissionController;
import top.egon.cola.platform.rbac3.admin.constraint.controller.ConstraintController;

@WebMvcTest(
        controllers = {
                ApplicationResourceController.class,
                ManifestController.class,
                RolePermissionController.class,
                ConstraintController.class
        },
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
class Rbac3ControlPlaneGatewayDiscoveryTest {

    @Autowired
    private RequestMappingHandlerMapping handlerMappings;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ApplicationResourceFacade applicationResourceFacade;

    @MockitoBean
    private ManifestFacade manifestFacade;

    @MockitoBean
    private RoleFacade roleFacade;

    @MockitoBean
    private ConstraintFacade constraintFacade;

    @MockitoBean
    private DatabaseClock databaseClock;

    @MockitoBean
    private LongIdGenerator idGenerator;

    @Test
    void gatewayScannerDiscoversAllResourceRoleAndTypedConstraintRoutes() {
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
                        ApplicationResourceController.class.getName(),
                        ManifestController.class.getName(),
                        RolePermissionController.class.getName(),
                        ConstraintController.class.getName()),
                methodsByController.keySet());
        assertTrue(methodsByController.get(ApplicationResourceController.class.getName())
                .contains("GET /api/rbac3/v1/applications"));
        assertTrue(methodsByController.get(ManifestController.class.getName())
                .contains("POST /api/rbac3/v1/internal/resource-manifests"));
        assertTrue(methodsByController.get(RolePermissionController.class.getName())
                .contains("POST /api/rbac3/v1/roles/{roleId}/permissions"));
        assertTrue(methodsByController.get(ConstraintController.class.getName())
                .contains("POST /api/rbac3/v1/sod-sets"));
        assertTrue(methodsByController.get(ConstraintController.class.getName())
                .contains("POST /api/rbac3/v1/data-rules"));
        assertTrue(methodsByController.get(ConstraintController.class.getName())
                .contains("POST /api/rbac3/v1/field-rules"));
        assertTrue(methodsByController.get(ConstraintController.class.getName())
                .contains("POST /api/rbac3/v1/operation-sod-rules"));
    }
}
