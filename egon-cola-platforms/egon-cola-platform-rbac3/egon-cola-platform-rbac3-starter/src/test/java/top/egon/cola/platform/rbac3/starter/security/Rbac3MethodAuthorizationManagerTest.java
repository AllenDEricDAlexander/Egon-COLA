package top.egon.cola.platform.rbac3.starter.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.aopalliance.intercept.MethodInvocation;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationService;
import top.egon.cola.platform.rbac3.contract.authorization.Decision;
import top.egon.cola.platform.rbac3.contract.authorization.PermissionRequest;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Rbac3MethodAuthorizationManagerTest {

    @Test
    void requiresApiAndGenericPermissionsWithAndSemantics() throws Exception {
        AuthorizationService authorization = mock(AuthorizationService.class);
        when(authorization.requirePermission(any(PermissionRequest.class)))
                .thenReturn(decision(Decision.ALLOW, "ALLOW"));
        Rbac3MethodAuthorizationManager manager =
                new Rbac3MethodAuthorizationManager(authorization);

        AuthorizationDecision decision = manager.check(
                authentication(), invocation("secured"));

        assertThat(decision).isNotNull();
        assertThat(decision.isGranted()).isTrue();
    }

    @Test
    void deniesWhenAnyDeclaredPermissionIsMissing() throws Exception {
        AuthorizationService authorization = mock(AuthorizationService.class);
        when(authorization.requirePermission(any(PermissionRequest.class)))
                .thenAnswer(invocation -> {
                    PermissionRequest request = invocation.getArgument(0);
                    return decision(
                            request.permissionCode().equals("api:read")
                                    ? Decision.ALLOW : Decision.DENY,
                            request.permissionCode().equals("api:read")
                                    ? "ALLOW" : "PERMISSION_DENIED");
                });
        Rbac3MethodAuthorizationManager manager =
                new Rbac3MethodAuthorizationManager(authorization);

        assertThat(manager.check(authentication(), invocation("secured"))
                .isGranted()).isFalse();
    }

    @Test
    void rejectsBlankPermissionAtResolutionTime() throws Exception {
        AuthorizationService authorization = mock(AuthorizationService.class);
        Rbac3MethodAuthorizationManager manager =
                new Rbac3MethodAuthorizationManager(authorization);

        assertThatThrownBy(() -> manager.check(
                authentication(), invocation("invalid")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Supplier<Authentication> authentication() {
        return () -> new TestingAuthenticationToken("subject", "");
    }

    private top.egon.cola.platform.rbac3.contract.authorization.AuthorizationDecision
    decision(Decision decision, String reason) {
        return new top.egon.cola.platform.rbac3.contract.authorization.AuthorizationDecision(
                decision,
                reason,
                "tenant-a",
                "subject",
                "permission",
                1L,
                1L,
                java.util.List.of(),
                Instant.EPOCH);
    }

    private MethodInvocation invocation(String methodName) throws Exception {
        Method method = Fixture.class.getDeclaredMethod(methodName);
        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getMethod()).thenReturn(method);
        when(invocation.getThis()).thenReturn(new Fixture());
        return invocation;
    }

    private static final class Fixture {

        @RBACAPIResource(
                code = "fixture.api",
                permission = "api:read",
                name = "Fixture API")
        @RequiresPermission("method:read")
        private void secured() {
        }

        @RequiresPermission(" ")
        private void invalid() {
        }
    }
}
