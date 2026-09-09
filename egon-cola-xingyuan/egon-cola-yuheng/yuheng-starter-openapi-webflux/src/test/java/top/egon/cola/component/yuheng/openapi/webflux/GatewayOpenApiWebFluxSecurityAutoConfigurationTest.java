package top.egon.cola.component.yuheng.openapi.webflux;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springdoc.webflux.api.OpenApiResource;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayOpenApiWebFluxSecurityAutoConfigurationTest {

    @Test
    void isConditionalReactiveAutoConfigurationForSpringdocAndSecurity() {
        Class<?> configuration =
                GatewayOpenApiWebFluxSecurityAutoConfiguration.class;

        assertThat(configuration.getAnnotation(AutoConfiguration.class))
                .isNotNull();
        assertThat(configuration.getAnnotation(ConditionalOnClass.class))
                .isNotNull();
        assertThat(configuration.getAnnotation(ConditionalOnWebApplication.class))
                .extracting(ConditionalOnWebApplication::type)
                .isEqualTo(ConditionalOnWebApplication.Type.REACTIVE);
        assertThat(configuration.getAnnotation(ConditionalOnClass.class)
                .value())
                .contains(ServerHttpSecurity.class,
                        SecurityWebFilterChain.class, OpenApiResource.class);
    }
}
