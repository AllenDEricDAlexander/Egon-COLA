package top.egon.cola.platform.rbac3.admin.controller;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.shared.domain.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.bootstrap.service.BootstrapQueryService;
import top.egon.cola.platform.rbac3.admin.session.service.SessionFacade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import top.egon.cola.platform.rbac3.admin.auth.controller.AuthController;

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
