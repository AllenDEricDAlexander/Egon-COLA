package top.egon.cola.platform.rbac3.admin.runtime;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.idp.starter.security.ServiceScopeAuthorization;
import top.egon.cola.platform.rbac3.contract.authorization.Decision;
import top.egon.cola.platform.rbac3.contract.authorization.PermissionRequest;
import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationService;
import top.egon.cola.platform.rbac3.starter.authorization.DefaultAuthorizationService;
import top.egon.cola.platform.rbac3.starter.security.Rbac3MethodAuthorizationAspect;
import top.egon.cola.platform.rbac3.starter.security.RequiresPermission;
import top.egon.cola.platform.rbac3.starter.web.Rbac3AuthorizationExceptionHandler;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验收 RBAC3 只处理 USER 业务权限，而 SERVICE 操作仅消费 IdP Scope。
 * Accepts that RBAC3 handles USER business permissions while SERVICE operations consume only IdP scopes.
 */
class Rbac3UserResourceAuthorizationIT {

    private static final Instant NOW = Instant.parse("2026-08-10T00:00:00Z");

    @Test
    void userWithoutTheInterfacePermissionIsDeniedAndMappedToForbidden()
            throws Throwable {
        DefaultAuthorizationService authorization = authorization(Set.of(
                "idp:resource-server:read"));
        assertThat(authorization.requirePermission(PermissionRequest.of(
                "idp:resource-server:update")).decision())
                .isEqualTo(Decision.DENY);
        Rbac3MethodAuthorizationAspect aspect =
                new Rbac3MethodAuthorizationAspect(authorization);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        RequiresPermission annotation = mock(RequiresPermission.class);
        when(annotation.value()).thenReturn("idp:resource-server:update");

        assertThatThrownBy(() -> aspect.authorize(joinPoint, annotation))
                .isInstanceOfSatisfying(
                        DefaultAuthorizationService.AuthorizationDeniedException.class,
                        denied -> assertThat(
                                new Rbac3AuthorizationExceptionHandler()
                                        .denied(denied).getStatusCode())
                                .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void serviceWithoutTheOperationScopeIsDeniedLocallyWithoutRbac3Lookup() {
        ServiceIdentityPrincipal service = new ServiceIdentityPrincipal(
                "idp-service", "tenant-1", "idp-service", "service-token-1",
                URI.create("https://api.egon.internal/prod/permission/rbac3"),
                8L, Set.of("service:authorization:snapshot"),
                "permission", "idp", "prod", "idp-key-1",
                NOW, NOW.plusSeconds(300));
        var authentication = new TestingAuthenticationToken(
                service, "", "ROLE_SERVICE");

        assertThat(new ServiceScopeAuthorization().hasScope(
                authentication, "service:authorization:decide")).isFalse();
    }

    private static DefaultAuthorizationService authorization(
            Set<String> permissions
    ) {
        IdentityPrincipal identity = new IdentityPrincipal(
                "alice", "tenant-1", "session-1", "idp-admin-web", "token-1",
                3L, Set.of("https://api.egon.internal/prod/permission/idp"),
                NOW, NOW.plusSeconds(300));
        SystemAuthorizationSnapshot snapshot = new SystemAuthorizationSnapshot(
                "tenant-1", "alice", "rbac-user-1", "session-1", "idp",
                11L, 12L, 13L, List.of(), permissions, Map.of(), Map.of(),
                "sha256:matrix", NOW, NOW.plusSeconds(300));
        return new DefaultAuthorizationService(
                () -> new AuthorizationService.RuntimeAuthorizationContext(
                        identity, snapshot, false),
                request -> AuthorizationService.OperationSodResult.allowed(),
                request -> AuthorizationService.FenceResult.allowed(NOW),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }
}
