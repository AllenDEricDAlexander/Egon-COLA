package top.egon.cola.platform.rbac3.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.activation.service.RoleActivationCandidateService;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.activation.service.RoleActivationFacade;
import top.egon.cola.platform.rbac3.admin.shared.domain.DatabaseClock;

import static org.assertj.core.api.Assertions.assertThat;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.activation.controller.RoleActivationController;

@WebMvcTest(
        controllers = RoleActivationController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
class Rbac3RoleActivationGatewayDiscoveryTest {

    @Autowired
    private RequestMappingHandlerMapping handlerMappings;

    @MockitoBean
    private RoleActivationCandidateService candidateService;

    @MockitoBean
    private RoleActivationFacade roleActivationFacade;

    @MockitoBean
    private DatabaseClock databaseClock;

    @Test
    void exposesEveryRoleActivationOperationWithStableIds() {
        Map<String, String> idsByMethod = handlerMappings.getHandlerMethods().values().stream()
                .filter(handler -> RoleActivationController.class.equals(handler.getBeanType()))
                .map(handler -> handler.getMethod())
                .filter(method -> method.isAnnotationPresent(Operation.class))
                .collect(Collectors.toMap(
                        Method::getName,
                        method -> method.getAnnotation(Operation.class).operationId()));

        assertThat(idsByMethod).containsExactlyInAnyOrderEntriesOf(Map.of(
                "candidates",
                "rbac3-role-activation-candidates-v1",
                "current",
                "rbac3-role-activation-current-v1",
                "replace",
                "rbac3-role-activation-replace-v1"));
    }
}
