package top.egon.cola.component.gateway.engine.http;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.core.security.GatewayCredential;
import top.egon.cola.component.gateway.core.security.TrustedIdentity;
import top.egon.cola.component.gateway.engine.security.TrustedIdentitySanitizer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DefaultGatewayHttpDataPlaneHandlerCredentialForwardingTest {

    private static final String TOKEN = "exact.header.payload.signature";

    @Test
    void restoresTheVerifiedBearerAfterSanitizationAndAcrossRetries() {
        Map<String, List<String>> sanitized = new LinkedHashMap<>(
                new TrustedIdentitySanitizer().sanitizeHttp(
                        Map.of(
                                "authorization", List.of("Bearer forged"),
                                "x-egon-gateway-user-id", List.of("forged-user"),
                                "accept", List.of("application/json")),
                        Set.of("authorization"),
                        new TrustedIdentity(Map.of(
                                "x-egon-gateway-user-id", "9"), Map.of())));
        GatewayHttpSecurityProcessor.Outcome outcome =
                new GatewayHttpSecurityProcessor.Outcome(
                        TrustedIdentity.empty(), Set.of("authorization"),
                        new GatewayCredential("bearer", TOKEN, Map.of()));

        DefaultGatewayHttpDataPlaneHandler.restoreOriginalBearer(
                sanitized, outcome, true);
        Map<String, List<String>> retry = new LinkedHashMap<>(sanitized);
        DefaultGatewayHttpDataPlaneHandler.restoreOriginalBearer(
                retry, outcome, true);

        assertEquals(List.of("Bearer " + TOKEN),
                sanitized.get("authorization"));
        assertEquals(List.of("Bearer " + TOKEN),
                retry.get("authorization"));
        assertEquals(List.of("9"),
                sanitized.get("x-egon-gateway-user-id"));
    }

    @Test
    void neverForwardsToRpcOrWithoutAnAuthorizedCredential() {
        Map<String, List<String>> rpc = new LinkedHashMap<>();
        DefaultGatewayHttpDataPlaneHandler.restoreOriginalBearer(
                rpc,
                new GatewayHttpSecurityProcessor.Outcome(
                        TrustedIdentity.empty(), Set.of(),
                        new GatewayCredential("bearer", TOKEN, Map.of())),
                false);
        Map<String, List<String>> none = new LinkedHashMap<>();
        DefaultGatewayHttpDataPlaneHandler.restoreOriginalBearer(
                none, GatewayHttpSecurityProcessor.Outcome.anonymous(), true);

        assertFalse(rpc.containsKey("authorization"));
        assertFalse(none.containsKey("authorization"));
    }
}
