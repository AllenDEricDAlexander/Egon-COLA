package top.egon.cola.component.gateway.openapi.webmvc;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayOpenApiWebMvcDependencyContractTest {

    @Test
    void exposesOnlyTheServletSpringdocApiSurface() throws Exception {
        assertThat(Class.forName("org.springframework.web.servlet.DispatcherServlet"))
                .isNotNull();
        assertThat(Class.forName(
                "org.springdoc.webmvc.api.MultipleOpenApiWebMvcResource"))
                .isNotNull();
        assertThatThrownBy(() -> Class.forName(
                "org.springdoc.webflux.api.MultipleOpenApiWebFluxResource"))
                .isInstanceOf(ClassNotFoundException.class);
        assertThatThrownBy(() -> Class.forName(
                "org.springdoc.webmvc.ui.SwaggerWelcome"))
                .isInstanceOf(ClassNotFoundException.class);
    }
}
