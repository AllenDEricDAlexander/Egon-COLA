package top.egon.cola.platform.tianquan.jianshen.starter.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure.Rbac3StarterAutoConfiguration;
import top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure.Rbac3StarterProperties.UserScopePolicyProperties;
import top.egon.cola.platform.tianquan.jianshen.starter.authorization.DefaultAuthorizationService;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.contract.AuthenticationContext;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpAuthenticationToken;
import top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.aopalliance.intercept.MethodInvocation;
import org.mockito.ArgumentCaptor;
import top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.Decision;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.PermissionRequest;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.never;

class Rbac3MethodAuthorizationManagerTest {
    private static final Instant NOW = Instant.parse("2026-10-11T00:00:00Z");

    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    @Test
    void realProxyScopeBeforePdpAndOnlyPermissionCompatibility() {
        var authorization = spy(actualAuthorization(Set.of("fixture:read")));
        try (var context = new AnnotationConfigApplicationContext()) {
            context.register(ProxyConfiguration.class);
            context.registerBean("rbac3MethodAuthorizationInterceptor", org.springframework.aop.Advisor.class,
                    () -> new Rbac3StarterAutoConfiguration().rbac3MethodAuthorizationInterceptor(authorization),
                    definition -> definition.setRole(org.springframework.beans.factory.config.BeanDefinition.ROLE_INFRASTRUCTURE));
            context.refresh();
            var business = context.getBean(ProxiedFixture.class);
            SecurityContextHolder.getContext().setAuthentication(new IdpAuthenticationToken(identity()));
            assertThatThrownBy(business::scoped).isInstanceOf(AccessDeniedException.class);
            verify(authorization, never()).requirePermission(any());
            assertThat(business.entryCount()).isZero(); assertThat(business.fieldCount()).isZero();
            SecurityContextHolder.getContext().setAuthentication(new IdpAuthenticationToken(identity(), List.of(new SimpleGrantedAuthority("SCOPE_fixture:read"))));
            assertThat(business.scoped()).isEqualTo("original-result");
            assertThat(business.permissionOnly()).isEqualTo("original-result");
            assertThat(business.entryCount()).isEqualTo(2); assertThat(business.fieldCount()).isEqualTo(2);
            verify(authorization, atLeastOnce()).requirePermission(any());
        }
    }

    @Test
    void configuredScopeMustBePresentBeforeTheRealPdp() throws Exception {
        try (var validator = new LocalValidatorFactoryBean()) {
            validator.afterPropertiesSet();
            var original = spy(actualAuthorization(Set.of("fixture:read")));
            var policy = new UserScopePolicyProperties().setEnabled(true).setPermissionScopes(Map.of("fixture:read", Set.of("fixture:read")));
            var manager = Rbac3MethodAuthorizationManager.createConfigured(original, policy, null, IdpSecurityMetricsRecorder.NOOP, validator);
            var invocation = proxyInvocation("permissionOnly");
            assertThat(manager.check(() -> new IdpAuthenticationToken(identity()), invocation).isGranted()).isFalse();
            verify(original, never()).requirePermission(any());
            var scoped = new IdpAuthenticationToken(identity(), List.of(new SimpleGrantedAuthority("SCOPE_fixture:read")));
            assertThat(manager.check(() -> scoped, invocation).isGranted()).isTrue();
        }
    }

    private AuthorizationService actualAuthorization(Set<String> permissions) {
        var snapshot = new SystemAuthorizationSnapshot("1001", "10001", "20001", "fixture", 1, 2, List.of("50001"), null, null,
                permissions, Set.of("fixture.api"), Map.of(), Map.of(), "sha256:fixture-snapshot", NOW, NOW.plusSeconds(300));
        return new DefaultAuthorizationService(() -> new AuthorizationService.RuntimeAuthorizationContext(identity(), snapshot, false),
                request -> AuthorizationService.OperationSodResult.allowed(), request -> AuthorizationService.FenceResult.allowed(NOW), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private IdentityPrincipal identity() {
        return new IdentityPrincipal("10001", "1001", "jti-1", Set.of("https://api.example/fixture"), NOW,
                NOW.plusSeconds(300), AuthenticationContext.password());
    }

    private MethodInvocation proxyInvocation(String name) throws Exception {
        var invocation = mock(MethodInvocation.class);
        when(invocation.getMethod()).thenReturn(ProxiedFixture.class.getMethod(name));
        when(invocation.getThis()).thenReturn(new ProxiedFixture());
        return invocation;
    }

    @Configuration(value = "methodSecurityProxyFixture", proxyBeanMethods = false)
    @EnableMethodSecurity
    static class ProxyConfiguration {
        @Bean("proxiedFixture") ProxiedFixture proxiedFixture() { return new ProxiedFixture(); }
    }

    public static class ProxiedFixture {
        private final AtomicInteger entries = new AtomicInteger();
        private final AtomicInteger fields = new AtomicInteger();
        @PreAuthorize("hasAuthority('SCOPE_fixture:read')")
        @RequiresPermission("fixture:read")
        @RBACAPIResource(code = "fixture.api", permission = "fixture:read", name = "Fixture API")
        public String scoped() { fields.incrementAndGet(); entries.incrementAndGet(); return "original-result"; }
        @RequiresPermission("fixture:read")
        public String permissionOnly() { fields.incrementAndGet(); entries.incrementAndGet(); return "original-result"; }
        public int entryCount() { return entries.get(); }
        public int fieldCount() { return fields.get(); }
    }

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
        ArgumentCaptor<PermissionRequest> requests = ArgumentCaptor.forClass(
                PermissionRequest.class);
        verify(authorization, atLeastOnce()).requirePermission(requests.capture());
        PermissionRequest apiRequest = requests.getAllValues().stream()
                .filter(request -> "api:read".equals(request.permissionCode()))
                .findFirst()
                .orElseThrow();
        assertThat(apiRequest.resourceCode()).isEqualTo("fixture.api");
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

    private top.egon.cola.platform.tianquan.jianshen.contract.authorization.AuthorizationDecision
    decision(Decision decision, String reason) {
        return new top.egon.cola.platform.tianquan.jianshen.contract.authorization.AuthorizationDecision(
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
