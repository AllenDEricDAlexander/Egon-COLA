package top.egon.cola.platform.rbac3.admin.controller;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.openapi.annotation.EgonApiCatalog;
import top.egon.cola.platform.rbac3.admin.audit.controller.AuditController;
import top.egon.cola.platform.rbac3.admin.authorization.grant.business.controller.UserBusinessAccessController;
import top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.controller.RoleResourceGrantController;
import top.egon.cola.platform.rbac3.admin.authorization.grant.userrole.controller.AssignmentController;
import top.egon.cola.platform.rbac3.admin.authorization.permission.controller.PermissionController;
import top.egon.cola.platform.rbac3.admin.authorization.permission.controller.ResourcePermissionMappingController;
import top.egon.cola.platform.rbac3.admin.authorization.policy.controller.ConstraintController;
import top.egon.cola.platform.rbac3.admin.authorization.policy.management.controller.ManagementPolicyController;
import top.egon.cola.platform.rbac3.admin.authorization.policy.participation.controller.ParticipationController;
import top.egon.cola.platform.rbac3.admin.authorization.resource.controller.ApplicationResourceController;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.activation.controller.RoleActivationController;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.controller.RuntimeController;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.decision.controller.InternalAuthorizationController;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.decision.controller.Rbac3AboutController;
import top.egon.cola.platform.rbac3.admin.authorization.simulation.controller.AuthorizationSimulationController;
import top.egon.cola.platform.rbac3.admin.iam.application.controller.ApplicationController;
import top.egon.cola.platform.rbac3.admin.iam.business.controller.BusinessCatalogController;
import top.egon.cola.platform.rbac3.admin.iam.organization.controller.DirectoryController;
import top.egon.cola.platform.rbac3.admin.iam.organization.controller.OrganizationController;
import top.egon.cola.platform.rbac3.admin.iam.organization.controller.UserOrganizationAssignmentController;
import top.egon.cola.platform.rbac3.admin.iam.position.controller.PositionController;
import top.egon.cola.platform.rbac3.admin.iam.position.controller.UserPositionAssignmentController;
import top.egon.cola.platform.rbac3.admin.iam.role.controller.RoleController;
import top.egon.cola.platform.rbac3.admin.iam.user.controller.UserController;
import top.egon.cola.platform.rbac3.admin.iam.user.controller.UserDirectoryController;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Rbac3OpenApiGroupContractTest {

    private static final String IAM_GROUP = "iam";

    private static final List<Class<?>> PUBLISHED_IAM_CONTROLLERS = List.of(
            AuditController.class,
            UserBusinessAccessController.class,
            RoleResourceGrantController.class,
            AssignmentController.class,
            ConstraintController.class,
            ManagementPolicyController.class,
            ApplicationResourceController.class,
            RoleActivationController.class,
            RuntimeController.class,
            Rbac3AboutController.class,
            AuthorizationSimulationController.class,
            ApplicationController.class,
            BusinessCatalogController.class,
            DirectoryController.class,
            OrganizationController.class,
            UserOrganizationAssignmentController.class,
            PositionController.class,
            UserPositionAssignmentController.class,
            RoleController.class,
            UserController.class,
            UserDirectoryController.class,
            PermissionController.class,
            ResourcePermissionMappingController.class
    );

    @Test
    void allPublishedRbac3AdminControllersUseTheIamGroup() {
        assertThat(PUBLISHED_IAM_CONTROLLERS).allSatisfy(controller -> {
            EgonApiCatalog catalog = controller.getAnnotation(EgonApiCatalog.class);
            assertThat(catalog)
                    .as("catalog for %s", controller.getSimpleName())
                    .isNotNull();
            assertThat(catalog.interfaceGroupCode()).isEqualTo(IAM_GROUP);
        });
    }

    @Test
    void internalControllersRemainOutsideThePublishedIamGroup() {
        assertThat(catalog(ParticipationController.class).interfaceGroupCode())
                .isEqualTo("business-participation");
        assertThat(catalog(InternalAuthorizationController.class).interfaceGroupCode())
                .isEqualTo("internal-authorization");
    }

    private EgonApiCatalog catalog(Class<?> controller) {
        return controller.getAnnotation(EgonApiCatalog.class);
    }
}
