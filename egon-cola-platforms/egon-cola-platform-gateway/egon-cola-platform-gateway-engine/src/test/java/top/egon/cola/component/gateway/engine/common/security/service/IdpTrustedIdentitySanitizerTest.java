package top.egon.cola.component.gateway.engine.common.security.service;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.core.security.TrustedIdentity;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IdpTrustedIdentitySanitizerTest {

    @Test
    void replacesSpoofedIdpHeadersWithVerifiedValues() {
        Map<String, List<String>> result = new TrustedIdentitySanitizer()
                .sanitizeHttp(
                        Map.of(
                                "X-Egon-Identity-Sub", List.of("mallory"),
                                "X-Egon-Tenant-Id", List.of("wrong-tenant"),
                                "X-Egon-Service-Scopes", List.of("admin:*")),
                        Set.of(
                                "x-egon-identity-sub",
                                "x-egon-tenant-id",
                                "x-egon-service-scopes"),
                        new TrustedIdentity(Map.of(
                                "X-Egon-Identity-Sub", "alice",
                                "X-Egon-Tenant-Id", "tenant-a",
                                "X-Egon-Service-Scopes", "resource:read"), Map.of()));

        assertEquals(List.of("alice"), result.get("x-egon-identity-sub"));
        assertEquals(List.of("tenant-a"), result.get("x-egon-tenant-id"));
        assertEquals(
                List.of("resource:read"),
                result.get("x-egon-service-scopes")
        );
    }
}
