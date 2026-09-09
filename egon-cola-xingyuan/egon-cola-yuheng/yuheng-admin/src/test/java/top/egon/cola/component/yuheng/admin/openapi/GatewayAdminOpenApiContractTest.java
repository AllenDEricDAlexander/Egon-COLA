package top.egon.cola.component.yuheng.admin.openapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.junit.jupiter.api.Test;
import org.springdoc.core.service.AbstractRequestService;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import top.egon.cola.component.yuheng.admin.application.controller.GatewayApplicationController;
import top.egon.cola.component.yuheng.admin.auth.controller.GatewayAuthBootstrapController;
import top.egon.cola.component.yuheng.admin.catalog.controller.GatewayCatalogController;
import top.egon.cola.component.yuheng.admin.credential.controller.GatewayCredentialController;
import top.egon.cola.component.yuheng.admin.group.controller.GatewayGroupController;
import top.egon.cola.component.yuheng.admin.mcp.controller.McpAppAdminController;
import top.egon.cola.component.yuheng.admin.mcp.controller.McpApprovalController;
import top.egon.cola.component.yuheng.admin.mcp.controller.McpCapabilityController;
import top.egon.cola.component.yuheng.admin.mcp.controller.McpProtocolInspectorController;
import top.egon.cola.component.yuheng.admin.mcp.controller.McpRemoteProviderController;
import top.egon.cola.component.yuheng.admin.mcp.controller.McpServerController;
import top.egon.cola.component.yuheng.admin.mcp.controller.McpTaskAdminController;
import top.egon.cola.component.yuheng.admin.mcp.controller.McpToolAdminController;
import top.egon.cola.component.yuheng.admin.observability.controller.GatewayObservabilityController;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.admin.release.controller.GatewayReleaseController;
import top.egon.cola.component.yuheng.admin.reporting.controller.openapi.GatewayDefinitionReportController;
import top.egon.cola.component.yuheng.admin.openapi.controller.GatewayOpenApiController;
import top.egon.cola.component.yuheng.admin.routing.controller.GatewayDraftController;
import top.egon.cola.component.yuheng.admin.runtime.controller.GatewayProjectionController;
import top.egon.cola.component.yuheng.admin.scope.controller.GatewayScopeController;
import top.egon.cola.component.yuheng.admin.shared.controller.GatewayAdminActorArgumentResolver;
import top.egon.cola.component.yuheng.admin.shared.controller.GatewayAdminWebMvcConfiguration;
import top.egon.cola.component.yuheng.admin.shared.domain.AdminActor;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayAdminOpenApiContractTest {

    private static final Set<Class<?>> HTTP_CONTROLLERS = Set.of(
            GatewayApplicationController.class,
            GatewayAuthBootstrapController.class,
            GatewayCatalogController.class,
            GatewayCredentialController.class,
            GatewayGroupController.class,
            McpAppAdminController.class,
            McpApprovalController.class,
            McpCapabilityController.class,
            McpProtocolInspectorController.class,
            McpRemoteProviderController.class,
            McpServerController.class,
            McpTaskAdminController.class,
            McpToolAdminController.class,
            GatewayObservabilityController.class,
            GatewayReleaseController.class,
            GatewayDefinitionReportController.class,
            GatewayDraftController.class,
            GatewayProjectionController.class,
            GatewayScopeController.class,
            GatewayOpenApiController.class
    );

    @Test
    void authenticatedActorRemainsServerResolvedAndIsNotAnOpenApiRequestParameter() {
        boolean wasIgnored = AbstractRequestService.isRequestTypeToIgnore(AdminActor.class);
        try {
            var resolvers = new java.util.ArrayList<org.springframework.web.method.support.HandlerMethodArgumentResolver>();
            new GatewayAdminWebMvcConfiguration().addArgumentResolvers(resolvers);
            assertThat(resolvers).anySatisfy(resolver ->
                    assertThat(resolver).isInstanceOf(GatewayAdminActorArgumentResolver.class));
            assertThat(AbstractRequestService.isRequestTypeToIgnore(AdminActor.class)).isTrue();
            assertThat(AbstractRequestService.isRequestTypeToIgnore(BusinessInput.class)).isFalse();
        } finally {
            if (!wasIgnored) {
                SpringDocUtils.getConfig().removeRequestWrapperToIgnore(AdminActor.class);
            }
        }
    }

    private record BusinessInput(String value) { }

    @Test
    void everyAdminControllerPublishesTheGatewayAdminGroup() {
        Set<String> operationIds = new HashSet<>();
        for (Class<?> controller : HTTP_CONTROLLERS) {
            EgonApiCatalog catalog = controller.getAnnotation(
                    EgonApiCatalog.class);
            assertThat(catalog)
                    .as("catalog for %s", controller.getName())
                    .isNotNull();
            assertThat(catalog.interfaceGroupCode())
                    .as("group for %s", controller.getName())
                    .isEqualTo("gateway-admin");
            assertThat(controller.getAnnotation(Tag.class))
                    .as("tag for %s", controller.getName())
                    .isNotNull();

            for (Method method : controller.getDeclaredMethods()) {
                if (AnnotatedElementUtils.findMergedAnnotation(
                        method, RequestMapping.class) == null) {
                    continue;
                }
                Operation operation = method.getAnnotation(Operation.class);
                assertThat(operation)
                        .as("operation for %s#%s", controller.getName(),
                                method.getName())
                        .isNotNull();
                assertThat(operation.operationId())
                        .as("operationId for %s#%s", controller.getName(),
                                method.getName())
                        .isNotBlank();
                assertThat(operationIds.add(operation.operationId()))
                        .as("unique operationId %s", operation.operationId())
                        .isTrue();
                assertThat(method.getAnnotation(EgonGatewayPolicy.class))
                        .as("policy for %s#%s", controller.getName(),
                                method.getName())
                        .isNotNull();
            }
        }
        assertThat(operationIds).isNotEmpty();
    }
}
