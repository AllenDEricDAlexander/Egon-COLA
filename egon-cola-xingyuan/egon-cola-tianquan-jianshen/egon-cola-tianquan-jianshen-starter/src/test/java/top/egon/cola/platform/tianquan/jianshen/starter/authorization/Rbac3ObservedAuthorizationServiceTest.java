package top.egon.cola.platform.tianquan.jianshen.starter.authorization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.Decision;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.PermissionRequest;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.tianquan.shoubing.contract.AuthenticationContext;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpEndpointAuthenticationPolicy;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpSecurityObservationFilter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.TokenAuditProofBO;

class Rbac3ObservedAuthorizationServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-11T00:00:00Z");
    private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    private final IdpObservedVerificationComponent observations = mock(IdpObservedVerificationComponent.class);
    private final TokenAuditProofBO proof = new TokenAuditProofBO(URI.create("https://idp.example/tenants/10001"),
            URI.create("https://api.example/finance"), "a".repeat(64), null, "b".repeat(64));
    @TempDir java.nio.file.Path temporary;

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
        org.slf4j.MDC.clear();
        validator.close();
    }

    @Test
    void twoHttpPdpDenialsShareOneStrictHandoff() throws Exception {
        var service = service();
        when(observations.enabled()).thenReturn(true);
        when(observations.authorizationDenied(proof)).thenReturn(true);
        var request = new MockHttpServletRequest("GET", "/api/finance");
        var response = new MockHttpServletResponse();
        new IdpSecurityObservationFilter(observations, new IdpEndpointAuthenticationPolicy())
                .doFilter(request, response, (incoming, outgoing) -> {
                    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
                    authenticate();
                    assertEquals(Decision.DENY, service.requirePermission(PermissionRequest.of("finance:payment:approve")).decision());
                    assertEquals(Decision.DENY, service.requirePermission(PermissionRequest.of("finance:payment:approve")).decision());
                });
        verify(observations, times(1)).authorizationDenied(proof);
    }

    @Test
    void nonServletCallsPreservePerInvocationProofHandoff() {
        var service = service();
        when(observations.authorizationDenied(proof)).thenReturn(true);
        authenticate();
        assertEquals(Decision.DENY, service.requirePermission(PermissionRequest.of("finance:payment:approve")).decision());
        assertEquals(Decision.DENY, service.requirePermission(PermissionRequest.of("finance:payment:approve")).decision());
        verify(observations, times(2)).authorizationDenied(proof);
    }

    @Test
    void failedHttpHandoffIsStickyAndMissingActiveMarkerIsUnavailable() throws Exception {
        var service = service();
        when(observations.enabled()).thenReturn(true);
        when(observations.authorizationDenied(proof)).thenReturn(false);
        var request = new MockHttpServletRequest("GET", "/api/finance");
        new IdpSecurityObservationFilter(observations, new IdpEndpointAuthenticationPolicy())
                .doFilter(request, new MockHttpServletResponse(), (incoming, outgoing) -> {
                    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
                    authenticate();
                    for (int call = 0; call < 2; call++) {
                        var failure = assertThrows(AuthorizationService.RuntimeUnavailableException.class,
                                () -> service.requirePermission(PermissionRequest.of("finance:payment:approve")));
                        assertEquals("OAUTH_AUDIT_DELIVERY_UNAVAILABLE", failure.reasonCode());
                    }
                });
        verify(observations, times(1)).authorizationDenied(proof);
        assertThrows(AuthorizationService.RuntimeUnavailableException.class,
                () -> service.requirePermission(PermissionRequest.of("finance:payment:approve")));
        verify(observations, times(1)).authorizationDenied(proof);
    }

    @Test
    @SuppressWarnings("unchecked")
    void twoRealWorkersWaitForOneActualDurableProducerConfirmation() throws Exception {
        validator.afterPropertiesSet();
        var key = temporary.resolve("audit.key");
        java.nio.file.Files.writeString(key, "0123456789abcdef0123456789abcdef");
        java.nio.file.Files.setPosixFilePermissions(key, Set.of(java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                java.nio.file.attribute.PosixFilePermission.OWNER_WRITE));
        var redis = mock(org.redisson.api.RedissonClient.class);
        org.redisson.api.RStream<String, String> stream = mock(org.redisson.api.RStream.class);
        org.mockito.Mockito.doReturn(stream).when(redis).getStream(org.mockito.ArgumentMatchers.eq("egon:oauth:audit:v1"),
                org.mockito.ArgumentMatchers.eq(org.redisson.client.codec.StringCodec.INSTANCE));
        var firstEntered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        when(stream.add(org.mockito.ArgumentMatchers.any(org.redisson.api.stream.StreamAddArgs.class))).thenAnswer(call -> {
            firstEntered.countDown();
            assertTrue(release.await(5, java.util.concurrent.TimeUnit.SECONDS));
            return new org.redisson.api.StreamMessageId(1, 1);
        });
        var producer = new top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent(redis,
                new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules(),
                new top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthAuditProperties("egon:oauth:audit:v1",
                        temporary.resolve("spool"), 1_048_576, key),
                new top.egon.cola.platform.tianquan.shoubing.starter.audit.AuditDeliveryProperties(),
                Clock.fixed(NOW, ZoneOffset.UTC), validator);
        var converter = (top.egon.cola.platform.tianquan.shoubing.starter.security.domain.converter.TokenUsageAuditEventConverter)
                Class.forName("top.egon.cola.platform.tianquan.shoubing.starter.security.domain.converter.TokenUsageAuditEventConverterImpl")
                        .getConstructor().newInstance();
        var usage = new top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthUsageObservationProperties(true, proof.issuer(), false,
                top.egon.cola.platform.tianquan.shoubing.starter.observability.IdpSecurityObservationReceiverEnum.JIANSHEN);
        var actual = new IdpObservedVerificationComponent(producer, converter, usage, IdpSecurityMetricsRecorder.NOOP,
                Clock.fixed(NOW, ZoneOffset.UTC), validator, proof.resource());
        var service = service(actual);
        var request = new MockHttpServletRequest("GET", "/api/finance");
        try (var workers = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            new IdpSecurityObservationFilter(actual, new IdpEndpointAuthenticationPolicy())
                    .doFilter(request, new MockHttpServletResponse(), (incoming, outgoing) -> {
                        java.util.concurrent.Callable<Decision> denied = () -> {
                            try {
                                RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
                                authenticate();
                                return service.requirePermission(PermissionRequest.of("finance:payment:approve")).decision();
                            } finally { SecurityContextHolder.clearContext(); RequestContextHolder.resetRequestAttributes(); }
                        };
                        var first = workers.submit(denied);
                        try {
                            assertTrue(firstEntered.await(5, java.util.concurrent.TimeUnit.SECONDS));
                            var second = workers.submit(denied);
                            assertFalse(first.isDone());
                            assertFalse(second.isDone());
                            release.countDown();
                            assertEquals(Decision.DENY, first.get(5, java.util.concurrent.TimeUnit.SECONDS));
                            assertEquals(Decision.DENY, second.get(5, java.util.concurrent.TimeUnit.SECONDS));
                        } catch (InterruptedException interrupted) {
                            Thread.currentThread().interrupt(); throw new jakarta.servlet.ServletException(interrupted);
                        } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException failure) {
                            throw new jakarta.servlet.ServletException(failure);
                        } finally { release.countDown(); }
                    });
        } finally { release.countDown(); }
        verify(stream, times(1)).add(org.mockito.ArgumentMatchers.any(org.redisson.api.stream.StreamAddArgs.class));
        try (var files = java.nio.file.Files.list(temporary.resolve("spool"))) {
            assertEquals(1, files.filter(path -> path.toString().endsWith(".evt")).count());
        }
    }

    private Rbac3ObservedAuthorizationService service() {
        return service(observations);
    }

    private Rbac3ObservedAuthorizationService service(IdpObservedVerificationComponent boundary) {
        validator.afterPropertiesSet();
        var snapshot = new SystemAuthorizationSnapshot("10001", "identity-1", "20001", "finance", 1, 2,
                List.of("50001"), null, null, Set.of(), Set.of(), Map.of(), Map.of(),
                "sha256:controlled-snapshot", NOW, NOW.plusSeconds(300));
        var delegate = new DefaultAuthorizationService(
                () -> new AuthorizationService.RuntimeAuthorizationContext(identity(), snapshot, false),
                request -> AuthorizationService.OperationSodResult.allowed(),
                request -> AuthorizationService.FenceResult.allowed(NOW), Clock.fixed(NOW, ZoneOffset.UTC));
        return new Rbac3ObservedAuthorizationService(delegate, IdpSecurityMetricsRecorder.NOOP, boundary, validator);
    }

    private void authenticate() {
        var authentication = UsernamePasswordAuthenticationToken.authenticated(identity(), "", List.of());
        authentication.setDetails(proof);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private IdentityPrincipal identity() {
        return new IdentityPrincipal("identity-1", "10001", "jti-1", Set.of("business"),
                NOW.minusSeconds(30), NOW.plusSeconds(300), AuthenticationContext.password());
    }
}
