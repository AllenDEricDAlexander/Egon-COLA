package top.egon.cola.platform.rbac3.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.shared.domain.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.authorization.policy.service.ConstraintFacade;
import top.egon.cola.platform.rbac3.admin.iam.role.service.RoleFacade;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import top.egon.cola.platform.rbac3.admin.authorization.resource.controller.ApplicationResourceController;
import top.egon.cola.platform.rbac3.admin.authorization.resource.service.GlobalResourceCatalogService;
import top.egon.cola.platform.rbac3.admin.iam.role.controller.RoleController;
import top.egon.cola.platform.rbac3.admin.authorization.policy.controller.ConstraintController;

@WebMvcTest(
        controllers = {
                ApplicationResourceController.class,
                RoleController.class,
                ConstraintController.class
        },
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
class Rbac3ControlPlaneGatewayDiscoveryTest {

    @Autowired
    private RequestMappingHandlerMapping handlerMappings;

    @MockitoBean
    private RoleFacade roleFacade;

    @MockitoBean
    private ConstraintFacade constraintFacade;

    @MockitoBean
    private GlobalResourceCatalogService globalResourceCatalogService;

    @MockitoBean
    private DatabaseClock databaseClock;

    @MockitoBean
    private LongIdGenerator idGenerator;

    @Test
    void openApiAnnotationsExposeAllResourceRoleAndTypedConstraintOperations() {
        org.assertj.core.api.Assertions.assertThat(operationIds(ApplicationResourceController.class))
                .contains("rbac3-application-list-v1", "rbac3-application-resource-list-v1");
        org.assertj.core.api.Assertions.assertThat(operationIds(RoleController.class))
                .contains("rbac3-role-create-v1", "rbac3-role-inheritance-add-v1");
        org.assertj.core.api.Assertions.assertThat(operationIds(ConstraintController.class))
                .contains(
                        "rbac3-sod-set-create-v1",
                        "rbac3-data-rule-create-v1",
                        "rbac3-field-rule-create-v1",
                        "rbac3-operation-sod-create-v1");
    }

    private Set<String> operationIds(Class<?> controllerType) {
        return handlerMappings.getHandlerMethods().values().stream()
                .filter(handler -> controllerType.equals(handler.getBeanType()))
                .map(handler -> handler.getMethod().getAnnotation(Operation.class))
                .filter(Objects::nonNull)
                .map(Operation::operationId)
                .collect(Collectors.toSet());
    }
}
