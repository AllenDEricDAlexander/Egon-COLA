package top.egon.cola.component.gateway.core.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GatewaySecurityPolicyTest {

    @Test
    void legacyConstructionDoesNotForwardCredentials() {
        GatewaySecurityPolicy policy = new GatewaySecurityPolicy(
                "security", AuthenticationMode.REQUIRED,
                List.of("extractor"), List.of("authentication"),
                List.of("authorization"), AuthorizationDecisionMode.ALL_ALLOW,
                "mapper", Duration.ofSeconds(1),
                SecurityFailureMode.FAIL_CLOSED);

        assertEquals(CredentialForwardingMode.NONE,
                policy.credentialForwardingMode());
    }

    @Test
    void protectedHttpPolicyCanExplicitlyForwardTheVerifiedBearer() {
        GatewaySecurityPolicy policy = new GatewaySecurityPolicy(
                "security", AuthenticationMode.REQUIRED,
                List.of("extractor"), List.of("authentication"),
                List.of("authorization"), AuthorizationDecisionMode.ALL_ALLOW,
                "mapper", Duration.ofSeconds(1),
                SecurityFailureMode.FAIL_CLOSED,
                CredentialForwardingMode.ORIGINAL_BEARER);

        assertEquals(CredentialForwardingMode.ORIGINAL_BEARER,
                policy.credentialForwardingMode());
    }
}
