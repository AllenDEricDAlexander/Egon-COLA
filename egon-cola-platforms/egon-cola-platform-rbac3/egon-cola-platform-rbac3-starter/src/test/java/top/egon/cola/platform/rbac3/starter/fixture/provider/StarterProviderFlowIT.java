package top.egon.cola.platform.rbac3.starter.fixture.provider;

import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import top.egon.cola.platform.rbac3.contract.authorization.AuthorizationDecision;
import top.egon.cola.platform.rbac3.contract.authorization.Decision;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationService;
import top.egon.cola.platform.rbac3.starter.authorization.DefaultAuthorizationService.AuthorizationDeniedException;
import top.egon.cola.platform.rbac3.starter.security.Rbac3MethodAuthorizationAspect;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StarterProviderFlowIT {

    @Test
    void businessControllerIsFinallyDeniedByAuthorizationService() throws Exception {
        AuthorizationService authorization = mock(AuthorizationService.class);
        when(authorization.requirePermission(any())).thenReturn(new AuthorizationDecision(
                Decision.DENY, "PERMISSION_DENIED", "10001", "20001",
                "finance:payment:read", 1L, 2L, List.of(), Instant.now()));
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(
                new PaymentFixtureController());
        proxyFactory.addAspect(new Rbac3MethodAuthorizationAspect(authorization));
        PaymentFixtureController controller = proxyFactory.getProxy();

        assertThatThrownBy(() -> controller.payment("90001"))
                .isInstanceOf(AuthorizationDeniedException.class)
                .hasMessageContaining("PERMISSION_DENIED");
    }
}
