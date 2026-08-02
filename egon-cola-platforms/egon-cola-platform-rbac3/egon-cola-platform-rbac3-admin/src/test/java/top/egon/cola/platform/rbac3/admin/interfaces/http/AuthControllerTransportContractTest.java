package top.egon.cola.platform.rbac3.admin.interfaces.http;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.application.port.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.bootstrap.application.BootstrapQueryService;
import top.egon.cola.platform.rbac3.admin.session.application.SessionFacade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AuthControllerTransportContractTest {

    @Test
    void controllerRetainsOnlySessionBootstrapResponsibilities() {
        AuthController controller = new AuthController(
                mock(BootstrapQueryService.class), mock(SessionFacade.class),
                mock(DatabaseClock.class));

        assertThat(controller).isNotNull();
        assertThat(AuthController.class.getDeclaredMethods())
                .extracting(java.lang.reflect.Method::getName)
                .contains("logout", "bootstrap")
                .doesNotContain("login", "refresh", "jwks", "stepUp");
    }
}
