package top.egon.cola.platform.rbac3.admin.integration;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.admin.interfaces.http.ApplicationResourceController;
import top.egon.cola.platform.rbac3.admin.interfaces.http.AssignmentController;
import top.egon.cola.platform.rbac3.admin.interfaces.http.AuditSimulationController;
import top.egon.cola.platform.rbac3.admin.interfaces.http.AuthController;
import top.egon.cola.platform.rbac3.admin.interfaces.http.ConstraintController;
import top.egon.cola.platform.rbac3.admin.interfaces.http.InternalAuthorizationController;
import top.egon.cola.platform.rbac3.admin.interfaces.http.ManagementPolicyController;
import top.egon.cola.platform.rbac3.admin.interfaces.http.ManifestController;
import top.egon.cola.platform.rbac3.admin.interfaces.http.ParticipationController;
import top.egon.cola.platform.rbac3.admin.interfaces.http.RoleActivationController;
import top.egon.cola.platform.rbac3.admin.interfaces.http.RolePermissionController;
import top.egon.cola.platform.rbac3.admin.interfaces.http.RuntimeController;
import top.egon.cola.platform.rbac3.admin.interfaces.http.SessionController;
import top.egon.cola.platform.rbac3.admin.interfaces.http.TenantUserDirectoryController;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Rbac3GatewayDefinitionDiscoveryTest {

    @Test
    void everyHttpControllerCarriesDiscoverableGatewayMetadataAndUniqueOperations() {
        List<Class<?>> controllers = List.of(
                ApplicationResourceController.class,
                AssignmentController.class,
                AuditSimulationController.class,
                AuthController.class,
                ConstraintController.class,
                InternalAuthorizationController.class,
                ManagementPolicyController.class,
                ManifestController.class,
                ParticipationController.class,
                RoleActivationController.class,
                RolePermissionController.class,
                RuntimeController.class,
                SessionController.class,
                TenantUserDirectoryController.class);
        Set<String> operationNames = new HashSet<>();

        for (Class<?> controller : controllers) {
            assertTrue(controller.isAnnotationPresent(RestController.class));
            assertTrue(controller.isAnnotationPresent(EgonHttpService.class));
            assertTrue(controller.isAnnotationPresent(GatewayInterfaceGroup.class));
            for (var method : controller.getDeclaredMethods()) {
                GatewayOperation operation = method.getAnnotation(GatewayOperation.class);
                if (operation != null) {
                    assertTrue(operationNames.add(operation.name()),
                            () -> "duplicate gateway operation: " + operation.name());
                }
            }
        }

        assertTrue(operationNames.containsAll(Set.of(
                "rbac3-auth-login-v1",
                "rbac3-role-activation-replace-v1",
                "rbac3-internal-authorization-decision-v1",
                "rbac3-runtime-status-v1")));
        assertEquals(operationNames.size(), operationNames.stream().distinct().count());
    }
}
