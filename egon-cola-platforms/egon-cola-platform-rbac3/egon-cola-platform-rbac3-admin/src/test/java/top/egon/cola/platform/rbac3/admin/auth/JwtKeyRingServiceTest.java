package top.egon.cola.platform.rbac3.admin.auth;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.auth.application.JwtKeyRingService;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtKeyRingServiceTest {

    @Test
    void rotatesThroughPreparedSigningVerifyOnlyAndRetiredWithoutExposingPrivateJwk() {
        Instant now = Instant.parse("2026-07-30T10:00:00Z");
        var current = key("old", JwtKeyRingService.KeyState.SIGNING, now, null);
        var service = new JwtKeyRingService(List.of(current), Duration.ofMinutes(32));
        service.publishPrepared(key("new", JwtKeyRingService.KeyState.PREPARED, null, null));

        service.promoteToSigning("new", now.plusSeconds(10));
        assertEquals("new", service.signingKey().kid());
        assertEquals(JwtKeyRingService.KeyState.VERIFY_ONLY,
                service.snapshot().stream().filter(value -> value.kid().equals("old"))
                        .findFirst().orElseThrow().state());
        assertThrows(IllegalStateException.class,
                () -> service.retire("old", now.plus(Duration.ofMinutes(31))));
        service.retire("old", now.plus(Duration.ofMinutes(33)));

        @SuppressWarnings("unchecked")
        var jwks = (List<Map<String, Object>>) service.publicJwks().get("keys");
        assertEquals(List.of("new"), jwks.stream().map(value -> value.get("kid")).toList());
        assertFalse(jwks.getFirst().containsKey("d"));
    }

    private JwtKeyRingService.KeyDescriptor key(
            String kid,
            JwtKeyRingService.KeyState state,
            Instant signingSince,
            Instant retireNotBefore) {
        return new JwtKeyRingService.KeyDescriptor(
                kid,
                "RS256",
                Map.of("kty", "RSA", "n", "modulus", "e", "AQAB", "d", "private"),
                state,
                signingSince,
                retireNotBefore);
    }
}
