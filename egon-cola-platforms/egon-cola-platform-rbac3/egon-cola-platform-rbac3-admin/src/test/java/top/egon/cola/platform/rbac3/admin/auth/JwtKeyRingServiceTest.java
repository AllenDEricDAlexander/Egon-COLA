package top.egon.cola.platform.rbac3.admin.auth;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.auth.service.JwtKeyRingService;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.KeyDescriptorVO;
import top.egon.cola.platform.rbac3.admin.auth.domain.enums.JwtKeyRingKeyStateEnum;

class JwtKeyRingServiceTest {

    @Test
    void rotatesThroughPreparedSigningVerifyOnlyAndRetiredWithoutExposingPrivateJwk() {
        Instant now = Instant.parse("2026-07-30T10:00:00Z");
        var current = key("old", JwtKeyRingKeyStateEnum.SIGNING, now, null);
        var service = new JwtKeyRingService(List.of(current), Duration.ofMinutes(32));
        service.publishPrepared(key("new", JwtKeyRingKeyStateEnum.PREPARED, null, null));

        service.promoteToSigning("new", now.plusSeconds(10));
        assertEquals("new", service.signingKey().kid());
        assertEquals(JwtKeyRingKeyStateEnum.VERIFY_ONLY,
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

    private KeyDescriptorVO key(
            String kid,
            JwtKeyRingKeyStateEnum state,
            Instant signingSince,
            Instant retireNotBefore) {
        return new KeyDescriptorVO(
                kid,
                "RS256",
                Map.of("kty", "RSA", "n", "modulus", "e", "AQAB", "d", "private"),
                state,
                signingSince,
                retireNotBefore);
    }
}
