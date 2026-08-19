package top.egon.cola.component.gateway.engine.common.security.service;

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
                        )
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
                        )
                )
        );
    }

    @Test
    void forwardsAuthorizationOnlyForTransparentProfileUnlessVetoed() {
        TrustedIdentitySanitizer sanitizer = new TrustedIdentitySanitizer();
        TrustedIdentity identity = new TrustedIdentity(Map.of(), Map.of());

        Map<String, List<String>> forwarded = sanitizer.sanitizeHttp(
                Map.of(
                        "Authorization", List.of("Bearer upstream"),
                        "OpenAI-Project", List.of("project")
                ),
                Set.of(),
                identity,
                true
        );
        Map<String, List<String>> vetoed = sanitizer.sanitizeHttp(
                Map.of("Authorization", List.of("Bearer upstream")),
                Set.of("AUTHORIZATION"),
                identity,
                true
        );

        assertEquals(
                List.of("Bearer upstream"),
                forwarded.get("authorization")
        );
        assertEquals(List.of("project"), forwarded.get("openai-project"));
        assertFalse(vetoed.containsKey("authorization"));
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
