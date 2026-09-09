package top.egon.cola.platform.tianquan.jianshen.admin.controller;

import org.junit.jupiter.api.Test;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.models.media.Schema;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.platform.tianquan.jianshen.admin.audit.controller.AuditController;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.business.controller.UserBusinessAccessController;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.controller.RoleResourceGrantController;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.domain.dto.ReplaceRoleResourcesRequestDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.userrole.controller.AssignmentController;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.permission.controller.PermissionController;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.permission.controller.ResourcePermissionMappingController;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.policy.controller.ConstraintController;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.policy.management.controller.ManagementPolicyController;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.policy.participation.controller.ParticipationController;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.controller.ApplicationResourceController;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.activation.controller.RoleActivationController;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.controller.RuntimeController;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.decision.controller.InternalAuthorizationController;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.decision.controller.Rbac3AboutController;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.simulation.controller.AuthorizationSimulationController;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.application.controller.ApplicationController;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.business.controller.BusinessCatalogController;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.organization.controller.DirectoryController;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.organization.controller.OrganizationController;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.organization.controller.UserOrganizationAssignmentController;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.position.controller.PositionController;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.position.controller.UserPositionAssignmentController;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.role.controller.RoleController;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.user.controller.UserController;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.user.controller.UserDirectoryController;

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

    @Test
    void roleResourceSchemasExposeFieldDescriptionsAndUnambiguousComponentNames() throws Exception {
        var method = RoleResourceGrantController.class.getMethod("tree", String.class);
        var resolved = ModelConverters.getInstance(true).resolveAsResolvedSchema(
                new AnnotatedType(method.getGenericReturnType()).resolveAsRef(true));
        String wrapperName = resolved.schema.get$ref().substring("#/components/schemas/".length());
        Schema<?> wrapper = resolved.referencedSchemas.get(wrapperName);
        assertThat(wrapper.getProperties().get("data").getDescription()).isNotBlank();
        Schema<?> tree = resolved.referencedSchemas.get("RoleResourceGrantTreeVO");
        assertThat(tree.getProperties().get("roleId").getDescription()).isEqualTo("角色 ID");
        assertThat(tree.getProperties().get("nodes").getDescription()).isNotBlank();
        assertThat(resolved.referencedSchemas).containsKeys(
                "RoleResourceGrantNode", "RoleResourceGrantSummary", "RoleResourceLinkedApi");
        for (String component : List.of(wrapperName, "RoleResourceGrantTreeVO",
                "RoleResourceGrantNode", "RoleResourceGrantSummary", "RoleResourceLinkedApi")) {
            Schema<?> componentSchema = resolved.referencedSchemas.get(component);
            assertThat(componentSchema.getProperties().values())
                    .allSatisfy(property -> assertThat(property.getDescription()).isNotBlank());
        }
        assertThat(method.getParameters()[0].getAnnotation(Parameter.class).description()).isNotBlank();
        Schema<?> node = resolved.referencedSchemas.get("RoleResourceGrantNode");
        assertThat(node.getProperties().get("parentCode").getTypes())
                .containsExactlyInAnyOrder("string", "null");
        var request = ModelConverters.getInstance(true).resolveAsResolvedSchema(
                new AnnotatedType(ReplaceRoleResourcesRequestDTO.class).resolveAsRef(true));
        Schema<?> requestSchema = request.referencedSchemas.get("ReplaceRoleResourcesRequestDTO");
        assertThat(requestSchema.getProperties().get("resourceIds").getMaxItems()).isEqualTo(2000);
        assertThat(requestSchema.getProperties().get("resourceIds").getUniqueItems()).isTrue();
        assertThat(requestSchema.getProperties().get("validFrom").getTypes())
                .containsExactlyInAnyOrder("string", "null");
    }

    private EgonApiCatalog catalog(Class<?> controller) {
        return controller.getAnnotation(EgonApiCatalog.class);
    }
}
