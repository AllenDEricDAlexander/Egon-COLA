package top.egon.cola.platform.tianquan.shoubing.admin.support.web;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class IdpHttpExceptionHandlerTest {

    @Test
    void mapsSecurityProjectionUnavailableToServiceUnavailable() {
        IdpHttpExceptionHandler handler = new IdpHttpExceptionHandler();
        var response = handler.temporarilyUnavailable(new OAuth2AuthenticationException(
                new OAuth2Error("temporarily_unavailable", "secret failure detail", null)));

        assertThat(Arrays.stream(IdpHttpExceptionHandler.class.getDeclaredMethods())
                .map(java.lang.reflect.Method::getName)).contains("temporarilyUnavailable");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().code()).isEqualTo("TEMPORARILY_UNAVAILABLE");
        assertThat(response.getBody().message()).doesNotContain("secret failure detail");
    }

    @Test
    void adviceKeepsItsExplicitBeanName() {
        new ApplicationContextRunner()
                .withUserConfiguration(IdpHttpExceptionHandler.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(IdpHttpExceptionHandler.class);
                    assertThat(context.getBean("idpHttpExceptionHandler"))
                            .isSameAs(context.getBean(IdpHttpExceptionHandler.class));
                });
    }
}
