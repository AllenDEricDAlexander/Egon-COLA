package top.egon.cola.platform.rbac3.admin.interfaces.http;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.CookieValue;
import top.egon.cola.platform.rbac3.admin.application.port.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.auth.application.AuthenticationFacade;
import top.egon.cola.platform.rbac3.admin.auth.application.JwtKeyRingService;
import top.egon.cola.platform.rbac3.admin.auth.application.RefreshFacade;
import top.egon.cola.platform.rbac3.admin.auth.application.StepUpFacade;
import top.egon.cola.platform.rbac3.admin.bootstrap.application.BootstrapQueryService;
import top.egon.cola.platform.rbac3.admin.session.application.SessionFacade;
import top.egon.cola.platform.rbac3.contract.auth.RefreshResult;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTransportContractTest {

    @Test
    void refreshAcceptsExactlyOneBodyOrHttpOnlyCookieSource() {
        Method refresh = Arrays.stream(AuthController.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("refresh"))
                .findFirst()
                .orElseThrow();

        assertThat(refresh.getParameterCount()).isEqualTo(2);
        assertThat(refresh.getParameters()[1].getAnnotation(CookieValue.class))
                .isNotNull()
                .extracting(CookieValue::name)
                .isEqualTo("rbac3_refresh_token");
    }

    @Test
    void jwksUsesTheStandardTopLevelJsonShape() throws Exception {
        assertThat(AuthController.class.getMethod("jwks").getReturnType())
                .isEqualTo(Map.class);
    }

    @Test
    void cookieRefreshRotatesTheSecureHttpOnlyCookie() {
        Instant now = Instant.parse("2026-08-01T03:00:00Z");
        RefreshFacade refreshFacade = mock(RefreshFacade.class);
        DatabaseClock clock = mock(DatabaseClock.class);
        RefreshResult result = new RefreshResult(
                "Bearer", "access", 300, "rotated", 600, "7",
                1, 2, 3, false, null, true);
        when(clock.transactionNow()).thenReturn(now);
        when(refreshFacade.refresh("cookie-token", now)).thenReturn(result);

        var response = controller(refreshFacade, clock).refresh(null, "cookie-token");

        verify(refreshFacade).refresh("cookie-token", now);
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .contains("rbac3_refresh_token=rotated")
                .contains("Path=/api/rbac3/v1/auth")
                .contains("Secure")
                .contains("HttpOnly")
                .contains("SameSite=Strict");
    }

    @Test
    void refreshRejectsAmbiguousOrMissingCredentialSources() {
        AuthController controller = controller(
                mock(RefreshFacade.class), mock(DatabaseClock.class));

        assertThatThrownBy(() -> controller.refresh(
                new AuthController.RefreshRequest("body-token"), "cookie-token"))
                .isInstanceOf(Rbac3RuleViolation.class)
                .hasMessageContaining("REQUEST_INVALID");
        assertThatThrownBy(() -> controller.refresh(null, null))
                .isInstanceOf(Rbac3RuleViolation.class)
                .hasMessageContaining("REQUEST_INVALID");
    }

    private AuthController controller(
            RefreshFacade refreshFacade,
            DatabaseClock clock) {
        return new AuthController(
                mock(AuthenticationFacade.class), refreshFacade,
                mock(BootstrapQueryService.class), mock(SessionFacade.class),
                mock(JwtKeyRingService.class), mock(StepUpFacade.class), clock);
    }
}
