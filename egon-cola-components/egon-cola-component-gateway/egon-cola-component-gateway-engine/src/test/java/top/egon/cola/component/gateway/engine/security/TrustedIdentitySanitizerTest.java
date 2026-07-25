package top.egon.cola.component.gateway.engine.security;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.core.security.TrustedIdentity;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TrustedIdentitySanitizerTest {

    @Test
    void removesCredentialsSpoofedIdentityAndHopByHopHeaders() {
        Map<String, List<String>> sanitized =
                new TrustedIdentitySanitizer().sanitizeHttp(
                        Map.of(
                                "authorization", List.of("Bearer secret"),
                                "cookie", List.of("SESSION=secret"),
                                "x-gateway-principal-id", List.of("spoof"),
                                "x-egon-gateway-principal-id",
                                List.of("spoof"),
                                "connection", List.of("close"),
                                "x-request-value", List.of("safe")
                        ),
                        Set.of(),
                        new TrustedIdentity(
                                Map.of(
                                        "X-Egon-Gateway-Principal-Id",
                                        "trusted"
                                ),
                                Map.of()
                        ),
                        "trace-123456789012"
                );

        assertFalse(sanitized.containsKey("authorization"));
        assertFalse(sanitized.containsKey("cookie"));
        assertFalse(sanitized.containsKey("connection"));
        assertEquals(
                List.of("trusted"),
                sanitized.get("x-egon-gateway-principal-id")
        );
        assertEquals(List.of("safe"), sanitized.get("x-request-value"));
    }

    @Test
    void rejectsIdentityMapperOutputOutsideReservedNamespace() {
        assertThrows(IllegalArgumentException.class, () ->
                new TrustedIdentitySanitizer().sanitizeHttp(
                        Map.of(),
                        Set.of(),
                        new TrustedIdentity(
                                Map.of("Authorization", "secret"),
                                Map.of()
                        ),
                        "trace-123456789012"
                )
        );
    }

    @Test
    void rpcMetadataAlsoDropsRawCredentials() {
        Map<String, String> metadata =
                new TrustedIdentitySanitizer().sanitizeRpc(
                        Map.of(
                                "authorization", "secret",
                                "cookie", "secret",
                                "egon-gateway-principal-id", "spoof",
                                "business-key", "safe"
                        ),
                        Set.of(),
                        new TrustedIdentity(
                                Map.of(),
                                Map.of(
                                        "egon-gateway-principal-id",
                                        "trusted"
                                )
                        )
                );

        assertEquals("trusted", metadata.get(
                "egon-gateway-principal-id"
        ));
        assertEquals("safe", metadata.get("business-key"));
        assertFalse(metadata.containsKey("authorization"));
    }
}
