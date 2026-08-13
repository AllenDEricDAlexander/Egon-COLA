package top.egon.cola.platform.rbac3.admin.auth;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import top.egon.cola.platform.rbac3.admin.runtime.repository.Rbac3RuntimePolicy;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.Rbac3RuntimePolicySnapshotVO;
import top.egon.cola.platform.rbac3.admin.auth.service.JwtKeyRingService;
import top.egon.cola.platform.rbac3.admin.auth.service.JwtTokenService;
import top.egon.cola.platform.rbac3.admin.config.properties.Rbac3AdminProperties;
import top.egon.cola.platform.rbac3.admin.runtime.repository.ddc.AtomicRbac3RuntimePolicy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.KeyDescriptorVO;
import top.egon.cola.platform.rbac3.admin.auth.domain.enums.JwtKeyRingKeyStateEnum;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.AccessTokenSubjectVO;

class JwtTokenServiceTest {

    @Test
    void issuesRs256ReferenceTokenWithKidAudienceTimesAndThreeVersions() {
        Instant now = Instant.parse("2026-07-30T10:00:00Z");
        JwtEncoder encoder = mock(JwtEncoder.class);
        when(encoder.encode(any())).thenAnswer(invocation -> {
            JwtEncoderParameters parameters = invocation.getArgument(0);
            return new Jwt(
                    "encoded-access-token",
                    parameters.getClaims().getIssuedAt(),
                    parameters.getClaims().getExpiresAt(),
                    parameters.getJwsHeader().getHeaders(),
                    parameters.getClaims().getClaims());
        });
        var key = new KeyDescriptorVO(
                "key-1", "RS256", Map.of("kty", "RSA", "n", "n", "e", "AQAB"),
                JwtKeyRingKeyStateEnum.SIGNING, now.minusSeconds(60), null);
        var keyRing = new JwtKeyRingService(List.of(key), Duration.ofMinutes(32));
        AtomicRbac3RuntimePolicy policy = new AtomicRbac3RuntimePolicy(
                new Rbac3AdminProperties());
        var service = new JwtTokenService(
                encoder, keyRing, () -> 9001L, "https://rbac3.example",
                List.of("internal-gateway"), policy);

        var result = service.issue(new AccessTokenSubjectVO(
                "200", "100", "300", 4, 5, 6), now);

        assertEquals("encoded-access-token", result.token());
        assertEquals(now.plus(Duration.ofMinutes(15)), result.expiresAt());
        ArgumentCaptor<JwtEncoderParameters> captor =
                ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(encoder).encode(captor.capture());
        var parameters = captor.getValue();
        assertEquals("RS256", parameters.getJwsHeader().getAlgorithm().getName());
        assertEquals("key-1", parameters.getJwsHeader().getKeyId());
        assertEquals("200", parameters.getClaims().getClaim("tid"));
        assertEquals(4L, ((Number) parameters.getClaims().getClaim("av")).longValue());
        assertEquals(5L, ((Number) parameters.getClaims().getClaim("sv")).longValue());
        assertEquals(6L, ((Number) parameters.getClaims().getClaim("pv")).longValue());
        assertFalse(parameters.getClaims().hasClaim("roles"));
        assertFalse(parameters.getClaims().hasClaim("permissions"));
    }

    @Test
    void usesOneCurrentPolicySnapshotForEachNewToken() {
        Instant now = Instant.parse("2026-07-30T10:00:00Z");
        JwtEncoder encoder = mock(JwtEncoder.class);
        when(encoder.encode(any())).thenAnswer(invocation -> {
            JwtEncoderParameters parameters = invocation.getArgument(0);
            return new Jwt(
                    "encoded-access-token",
                    parameters.getClaims().getIssuedAt(),
                    parameters.getClaims().getExpiresAt(),
                    parameters.getJwsHeader().getHeaders(),
                    parameters.getClaims().getClaims());
        });
        var key = new KeyDescriptorVO(
                "key-1", "RS256", Map.of("kty", "RSA", "n", "n", "e", "AQAB"),
                JwtKeyRingKeyStateEnum.SIGNING, now.minusSeconds(60), null);
        var keyRing = new JwtKeyRingService(List.of(key), Duration.ofMinutes(32));
        AtomicRbac3RuntimePolicy mutablePolicy = new AtomicRbac3RuntimePolicy(
                new Rbac3AdminProperties());
        CountingPolicy policy = new CountingPolicy(mutablePolicy);
        var service = new JwtTokenService(
                encoder, keyRing, () -> 9001L, "https://rbac3.example",
                List.of("internal-gateway"), policy);
        var subject = new AccessTokenSubjectVO(
                "200", "100", "300", 4, 5, 6);

        var first = service.issue(subject, now);
        mutablePolicy.apply(AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY, "1200", 1L);
        var second = service.issue(subject, now);

        assertEquals(now.plusSeconds(900), first.expiresAt());
        assertEquals(now.plusSeconds(900), first.claims().exp());
        assertEquals(now.plusSeconds(1200), second.expiresAt());
        assertEquals(now.plusSeconds(1200), second.claims().exp());
        assertEquals(2, policy.currentCalls);
    }

    private static final class CountingPolicy implements Rbac3RuntimePolicy {

        private final Rbac3RuntimePolicy delegate;
        private int currentCalls;

        private CountingPolicy(Rbac3RuntimePolicy delegate) {
            this.delegate = delegate;
        }

        @Override
        public Rbac3RuntimePolicySnapshotVO current() {
            currentCalls++;
            return delegate.current();
        }
    }
}
