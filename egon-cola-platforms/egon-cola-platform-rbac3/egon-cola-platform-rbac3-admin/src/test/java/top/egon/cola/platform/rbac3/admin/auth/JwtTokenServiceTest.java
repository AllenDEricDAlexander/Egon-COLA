package top.egon.cola.platform.rbac3.admin.auth;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import top.egon.cola.platform.rbac3.admin.auth.application.JwtKeyRingService;
import top.egon.cola.platform.rbac3.admin.auth.application.JwtTokenService;

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
        var key = new JwtKeyRingService.KeyDescriptor(
                "key-1", "RS256", Map.of("kty", "RSA", "n", "n", "e", "AQAB"),
                JwtKeyRingService.KeyState.SIGNING, now.minusSeconds(60), null);
        var keyRing = new JwtKeyRingService(List.of(key), Duration.ofMinutes(32));
        var service = new JwtTokenService(
                encoder, keyRing, () -> 9001L, "https://rbac3.example",
                List.of("internal-gateway"), Duration.ofMinutes(15));

        var result = service.issue(new JwtTokenService.AccessTokenSubject(
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
}
