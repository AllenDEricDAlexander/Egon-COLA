package top.egon.cola.platform.rbac3.admin.integration;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.admin.resource.controller.ApplicationResourceController;
import top.egon.cola.platform.rbac3.admin.assignment.controller.AssignmentController;
import top.egon.cola.platform.rbac3.admin.audit.controller.AuditController;
import top.egon.cola.platform.rbac3.admin.auth.controller.AuthController;
import top.egon.cola.platform.rbac3.admin.constraint.controller.ConstraintController;
import top.egon.cola.platform.rbac3.admin.authorization.controller.InternalAuthorizationController;
import top.egon.cola.platform.rbac3.admin.identity.controller.InternalIdentityController;
import top.egon.cola.platform.rbac3.admin.management.controller.ManagementPolicyController;
import top.egon.cola.platform.rbac3.admin.resource.controller.ManifestController;
import top.egon.cola.platform.rbac3.admin.participation.controller.ParticipationController;
import top.egon.cola.platform.rbac3.admin.activation.controller.RoleActivationController;
import top.egon.cola.platform.rbac3.admin.role.controller.RolePermissionController;
import top.egon.cola.platform.rbac3.admin.runtime.controller.RuntimeController;
import top.egon.cola.platform.rbac3.admin.session.controller.SessionController;
import top.egon.cola.platform.rbac3.admin.simulation.controller.AuthorizationSimulationController;
import top.egon.cola.platform.rbac3.admin.directory.controller.DirectoryController;
import top.egon.cola.platform.rbac3.admin.identity.controller.UserDirectoryController;
import top.egon.cola.platform.rbac3.admin.tenant.controller.TenantController;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Rbac3GatewayDefinitionDiscoveryTest {

    @Test
    void everyHttpControllerCarriesDiscoverableGatewayMetadataAndUniqueOperations() {
        List<Class<?>> controllers = List.of(
                ApplicationResourceController.class,
                AssignmentController.class,
                AuditController.class,
                AuthorizationSimulationController.class,
                AuthController.class,
                ConstraintController.class,
                InternalAuthorizationController.class,
                InternalIdentityController.class,
                ManagementPolicyController.class,
                ManifestController.class,
                ParticipationController.class,
                RoleActivationController.class,
                RolePermissionController.class,
                RuntimeController.class,
                SessionController.class,
                TenantController.class,
                UserDirectoryController.class,
                DirectoryController.class);
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
                    assertTrue(operation.name().matches(
                            "rbac3(?:-[a-z0-9]+)+-v\\d+"));
                    assertFalse(operation.summary().isBlank());
                    assertTrue(Arrays.asList(operation.tags()).contains("rbac3"));
                    assertTrue(operation.tags().length >= 2);
                }
            }
        }

        assertTrue(operationNames.containsAll(Set.of(
                "rbac3-internal-identity-resolve-v1",
                "rbac3-role-activation-replace-v1",
                "rbac3-internal-authorization-decision-v1",
                "rbac3-runtime-status-v1")));
        assertEquals(operationNames.size(), operationNames.stream().distinct().count());
    }
}
