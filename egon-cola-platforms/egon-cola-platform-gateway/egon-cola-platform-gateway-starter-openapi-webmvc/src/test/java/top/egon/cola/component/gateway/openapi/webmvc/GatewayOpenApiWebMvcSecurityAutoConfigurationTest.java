package top.egon.cola.component.gateway.openapi.webmvc;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springdoc.webmvc.api.OpenApiResource;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayOpenApiWebMvcSecurityAutoConfigurationTest {

    @Test
    void isConditionalServletAutoConfigurationForSpringdocAndSecurity() {
        Class<?> configuration =
                GatewayOpenApiWebMvcSecurityAutoConfiguration.class;

        assertThat(configuration.getAnnotation(AutoConfiguration.class))
                .isNotNull();
        assertThat(configuration.getAnnotation(ConditionalOnClass.class))
                .isNotNull();
        assertThat(configuration.getAnnotation(ConditionalOnWebApplication.class))
                .extracting(ConditionalOnWebApplication::type)
                .isEqualTo(ConditionalOnWebApplication.Type.SERVLET);
        assertThat(configuration.getAnnotation(ConditionalOnClass.class)
                .value())
                .contains(HttpSecurity.class, SecurityFilterChain.class,
                        OpenApiResource.class);
    }
}
