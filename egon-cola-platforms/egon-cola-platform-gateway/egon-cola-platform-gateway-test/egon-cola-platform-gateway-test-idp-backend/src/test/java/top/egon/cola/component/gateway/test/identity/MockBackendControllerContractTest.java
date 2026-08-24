package top.egon.cola.component.gateway.test.identity;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.rbac3.starter.security.RequiresPermission;

import static org.assertj.core.api.Assertions.assertThat;

class MockBackendControllerContractTest {

    @Test
    void declaresReadAndAdminPermissionsAtTheDownstreamBoundary()
            throws NoSuchMethodException {
        assertThat(MockBackendController.class.getMethod("read", IdentityPrincipal.class).getAnnotation(
                RequiresPermission.class).value()).isEqualTo("mock:read");
        assertThat(MockBackendController.class.getMethod("admin", IdentityPrincipal.class).getAnnotation(
                RequiresPermission.class).value()).isEqualTo("mock:admin");
    }
}
