package top.egon.cola.component.yuheng.runtime.security.service;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.yuheng.contract.rule.GatewayRuntimePolicy;
import top.egon.cola.component.yuheng.core.security.CredentialForwardingMode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GatewaySecurityPolicyCompilerTest {

    private final GatewaySecurityPolicyCompiler compiler =
            new GatewaySecurityPolicyCompiler(
                    GatewaySecurityCapabilityRegistry.empty());

    @Test
    void defaultsCredentialForwardingToNoneAndAcceptsAnExplicitMode() {
        var defaultPolicy = compiler.compile(
                List.of(policy(Map.of())), Map.of()).get("security");
        var forwardingPolicy = compiler.compile(
                List.of(policy(Map.of(
                        "credentialForwardingMode", "ORIGINAL_BEARER"))),
                Map.of()).get("security");

        assertEquals(CredentialForwardingMode.NONE,
                defaultPolicy.credentialForwardingMode());
        assertEquals(CredentialForwardingMode.ORIGINAL_BEARER,
                forwardingPolicy.credentialForwardingMode());
    }

    @Test
    void rejectsUnknownCredentialForwardingModes() {
        assertThrows(IllegalArgumentException.class, () -> compiler.compile(
                List.of(policy(Map.of(
                        "credentialForwardingMode", "FORWARD_ANYTHING"))),
                Map.of()));
    }

    @Test
    void rejectsBusinessPolicyWithoutAuthenticationAndAuthorization() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> compiler.compile(List.of(policy(Map.of(
                        "routeSecurityType", "BUSINESS_PROTECTED"))), Map.of()));

        assertEquals("BUSINESS_PROTECTED requires authentication and authorization",
                failure.getMessage());
    }

    private GatewayRuntimePolicy policy(Map<String, Object> configuration) {
        Map<String, Object> effective = new LinkedHashMap<>();
        effective.put("routeSecurityType", "PUBLIC_PROTOCOL");
        effective.putAll(configuration);
        return new GatewayRuntimePolicy(
                "security", "SECURITY", "ROUTE", effective);
    }
}
