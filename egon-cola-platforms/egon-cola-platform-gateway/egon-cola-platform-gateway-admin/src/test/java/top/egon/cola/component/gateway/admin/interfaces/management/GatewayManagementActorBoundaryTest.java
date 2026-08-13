package top.egon.cola.component.gateway.admin;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestHeader;
import top.egon.cola.component.gateway.admin.application.controller.GatewayApplicationController;
import top.egon.cola.component.gateway.admin.catalog.controller.GatewayCatalogController;
import top.egon.cola.component.gateway.admin.credential.controller.GatewayCredentialController;
import top.egon.cola.component.gateway.admin.group.controller.GatewayGroupController;
import top.egon.cola.component.gateway.admin.release.controller.GatewayReleaseController;
import top.egon.cola.component.gateway.admin.routing.controller.GatewayDraftController;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayManagementActorBoundaryTest {

    @Test
    void managementControllersMustNotTrustActorIdentityHeaders() {
        List<Class<?>> controllers = List.of(
                GatewayApplicationController.class,
                GatewayCatalogController.class,
                GatewayCredentialController.class,
                GatewayDraftController.class,
                GatewayGroupController.class,
                GatewayReleaseController.class
        );

        List<String> actorHeaders = controllers.stream()
                .flatMap(controller ->
                        List.of(controller.getDeclaredMethods()).stream())
                .flatMap(method ->
                        List.of(method.getParameters()).stream())
                .filter(this::isAdminActorHeader)
                .map(Parameter::getDeclaringExecutable)
                .map(Method.class::cast)
                .map(Method::toGenericString)
                .toList();

        assertThat(actorHeaders)
                .as("authenticated principal must be the only actor source")
                .isEmpty();
    }

    private boolean isAdminActorHeader(Parameter parameter) {
        RequestHeader header = parameter.getAnnotation(RequestHeader.class);
        return header != null
                && "X-Admin-Actor-Id".equalsIgnoreCase(header.value());
    }
}
