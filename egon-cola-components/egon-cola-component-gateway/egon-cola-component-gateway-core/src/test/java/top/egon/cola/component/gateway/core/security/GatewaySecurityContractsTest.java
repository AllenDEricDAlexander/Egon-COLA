package top.egon.cola.component.gateway.core.security;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.core.context.GatewayPrincipal;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GatewaySecurityContractsTest {

    @Test
    void credentialNeverRendersItsSecret() {
        GatewayCredential credential = new GatewayCredential(
                "bearer",
                "super-secret",
                Map.of("issuer", "accounts")
        );

        assertFalse(credential.toString().contains("super-secret"));
        assertEquals("super-secret", credential.tokenReference());
    }

    @Test
    void anonymousPrincipalIsExplicitAndUnauthenticated() {
        GatewayPrincipal principal = GatewayPrincipal.anonymous();

        assertEquals("ANONYMOUS", principal.principalId());
        assertFalse(principal.authenticated());
    }

    @Test
    void policyRejectsUnsupportedFailOpenAndInvalidCombinations() {
        assertThrows(IllegalArgumentException.class, () ->
                new GatewaySecurityPolicy(
                        "security",
                        AuthenticationMode.REQUIRED,
                        List.of(),
                        List.of(),
                        List.of(),
                        AuthorizationDecisionMode.ALL_ALLOW,
                        null,
                        Duration.ofMillis(100),
                        SecurityFailureMode.FAIL_OPEN
                )
        );
    }

    @Test
    void attributesAreBounded() {
        assertThrows(IllegalArgumentException.class, () ->
                new GatewayCredential(
                        "bearer",
                        "token",
                        java.util.stream.IntStream.range(0, 17)
                                .boxed()
                                .collect(java.util.stream.Collectors.toMap(
                                        Object::toString,
                                        Object::toString
                                ))
                )
        );
    }
}
