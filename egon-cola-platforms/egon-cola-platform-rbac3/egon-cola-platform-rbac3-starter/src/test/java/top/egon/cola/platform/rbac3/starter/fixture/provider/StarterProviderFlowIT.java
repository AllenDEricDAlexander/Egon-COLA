package top.egon.cola.platform.rbac3.starter.fixture.provider;

import org.junit.jupiter.api.Test;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.authentication.TestingAuthenticationToken;
import top.egon.cola.platform.rbac3.contract.authorization.AuthorizationDecision;
import top.egon.cola.platform.rbac3.contract.authorization.Decision;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationService;
import top.egon.cola.platform.rbac3.starter.security.Rbac3MethodAuthorizationManager;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
        PaymentFixtureController controller = new PaymentFixtureController();
        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getMethod()).thenReturn(
                PaymentFixtureController.class.getDeclaredMethod(
                        "payment", String.class));
        when(invocation.getThis()).thenReturn(controller);

        assertThat(new Rbac3MethodAuthorizationManager(authorization)
                .check(() -> new TestingAuthenticationToken("subject", ""), invocation)
                .isGranted()).isFalse();
    }
}
