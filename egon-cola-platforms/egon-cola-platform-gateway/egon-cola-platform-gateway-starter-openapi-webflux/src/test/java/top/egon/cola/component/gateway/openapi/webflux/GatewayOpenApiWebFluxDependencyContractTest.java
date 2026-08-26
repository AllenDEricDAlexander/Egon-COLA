package top.egon.cola.component.gateway.openapi.webflux;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayOpenApiWebFluxDependencyContractTest {

    @Test
    void exposesOnlyTheReactiveSpringdocApiSurface() throws Exception {
        assertThat(Class.forName(
                "org.springframework.web.reactive.DispatcherHandler"))
                .isNotNull();
        assertThat(Class.forName(
                "org.springdoc.webflux.api.MultipleOpenApiWebFluxResource"))
                .isNotNull();
        assertThatThrownBy(() -> Class.forName(
                "org.springframework.web.servlet.DispatcherServlet"))
                .isInstanceOf(ClassNotFoundException.class);
        assertThatThrownBy(() -> Class.forName(
                "org.springdoc.webflux.ui.SwaggerWelcome"))
                .isInstanceOf(ClassNotFoundException.class);
    }
}
