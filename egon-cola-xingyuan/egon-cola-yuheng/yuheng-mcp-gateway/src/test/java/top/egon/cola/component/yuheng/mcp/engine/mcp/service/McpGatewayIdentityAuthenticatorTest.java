package top.egon.cola.component.yuheng.mcp.engine.mcp.service;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import top.egon.cola.component.yuheng.contract.mcp.protocol.McpProtocolDialect;
import top.egon.cola.component.yuheng.contract.mcp.rule.McpRuntimeServer;
import top.egon.cola.component.yuheng.contract.rule.GatewayRuntimePolicy;
import top.egon.cola.component.yuheng.core.context.GatewayPrincipal;
import top.egon.cola.component.yuheng.core.security.GatewayAuthContext;
import top.egon.cola.component.yuheng.core.security.GatewaySecurityPolicy;
import top.egon.cola.component.yuheng.mcp.common.transport.McpHttpRequest;
import top.egon.cola.component.yuheng.mcp.engine.bootstrap.config.McpGatewayEngineConfiguration;
import top.egon.cola.component.yuheng.mcp.engine.rule.domain.McpGatewayCompiledRulesDTO;
import top.egon.cola.component.yuheng.runtime.rule.service.GatewayRuleActivationApplier;
import top.egon.cola.component.yuheng.runtime.security.domain.GatewaySecurityException;
import top.egon.cola.component.yuheng.runtime.security.domain.GatewaySecurityResult;
import top.egon.cola.component.yuheng.runtime.security.service.GatewaySecurityCapabilityRegistry;
import top.egon.cola.component.yuheng.runtime.security.service.GatewaySecurityChain;

import java.lang.reflect.InvocationTargetException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class McpGatewayIdentityAuthenticatorTest {

    private static final McpRuntimeServer SERVER = new McpRuntimeServer(
            "server-1", "billing", "Billing", null, null,
            Set.of(McpProtocolDialect.STABLE_2025_11_25),
            "https://resource.egon.top/mcp/billing", 30, true);
    private static final McpHttpRequest REQUEST = new McpHttpRequest("POST", "/mcp/billing",
            Map.of("x-egon-identity-issuer", "https://attacker.example"), "{}", Map.of());

    @Test
    void exposesManagedPolicyConstructorWithoutRemovingTheLegacyAbi() throws Exception {
        McpGatewayIdentityAuthenticator.class.getConstructor(GatewaySecurityChain.class,
                Function.class, String.class, Clock.class);
        McpGatewayIdentityAuthenticator.class.getConstructor(GatewaySecurityChain.class,
                String.class, String.class, Clock.class);
    }

    @Test
    void bindsIdpResourceToConfiguredMcpResourceUri() {
        assertEquals(Map.of("tianquan-shoubing.resource-uri", SERVER.resourceUri()),
                McpGatewayIdentityAuthenticator.securityAttributes(SERVER));
    }

    @Test
    void handsOffTheSameContextReferenceOnlyForTheManagedConstructor() {
        var chain = mock(GatewaySecurityChain.class);
        var request = McpRequestObservationComponentTest.request();
        var context = McpRequestObservationComponentTest.context(request);
        var result = mock(GatewaySecurityResult.class);
        when(result.context()).thenReturn(context);
        when(chain.execute(any(), any(), any(), any())).thenReturn(Mono.just(result));
        var policy = policy(List.of(source("policy-1", List.of("yuheng.mcp.billing"), List.of("billing:read"))));
        var managed = new McpGatewayIdentityAuthenticator(chain, selected -> policy, "node-1", Clock.systemUTC())
                .authenticate(REQUEST, SERVER).block();
        assertSame(context, managed.get(McpGatewayIdentityAuthenticator.VERIFIED_CONTEXT_ATTRIBUTE));
        var legacy = new McpGatewayIdentityAuthenticator(chain, "https://legacy.example", "node-1", Clock.systemUTC())
                .authenticate(REQUEST, SERVER).block();
        assertFalse(legacy.containsKey(McpGatewayIdentityAuthenticator.VERIFIED_CONTEXT_ATTRIBUTE));
    }

    @Test
    void usesTheSameVerifiedIssuerForBothRealmsAndForwardsThePublishedPolicy() {
        var policy = policy(List.of(source("policy-1", List.of("yuheng.mcp.billing"), List.of("billing:read"))));
        for (String issuer : List.of("https://idp.example/tenants/1001", "https://idp.example/tenants/1002")) {
            var chain = mock(GatewaySecurityChain.class);
            when(chain.execute(any(), any(), same(policy), any())).thenAnswer(call -> {
                GatewayAuthContext context = call.getArgument(1);
                assertEquals(policy.policyId(), context.policyId());
                assertEquals("yuheng.mcp.billing", context.operationId());
                var result = mock(GatewaySecurityResult.class);
                when(result.context()).thenReturn(context.withPrincipal(new GatewayPrincipal(
                        "subject-1", "USER", "1001", null, true,
                        Map.of("tianquan-shoubing.issuer", issuer))));
                return Mono.just(result);
            });
            var identity = new McpGatewayIdentityAuthenticator(chain, selected -> policy, "node-1", Clock.systemUTC())
                    .authenticate(REQUEST, SERVER).block(Duration.ofSeconds(2));
            assertEquals(issuer, identity.get("identity.issuer"));
            assertEquals(SERVER.resourceUri(), identity.get("identity.resource-uri"));
            verify(chain).execute(any(), any(), same(policy), any());
        }
    }

    @Test
    void retains401403503InsteadOfConvertingFailuresToEmpty() {
        var policy = policy(List.of(source("policy-1", List.of("yuheng.mcp.billing"), List.of("billing:read"))));
        for (int status : List.of(401, 403, 503)) {
            var chain = mock(GatewaySecurityChain.class);
            var failure = new GatewaySecurityException("test-" + status, status, "UNAVAILABLE");
            when(chain.execute(any(), any(), any(), any())).thenReturn(Mono.error(failure));
            var authenticator = new McpGatewayIdentityAuthenticator(chain, selected -> policy, "node-1", Clock.systemUTC());
            assertSame(failure, assertThrows(GatewaySecurityException.class,
                    () -> authenticator.authenticate(REQUEST, SERVER).block(Duration.ofSeconds(2))));
            var legacy = new McpGatewayIdentityAuthenticator(chain, "https://legacy.example", "node-1", Clock.systemUTC());
            assertNull(legacy.authenticate(REQUEST, SERVER).block(Duration.ofSeconds(2)));
        }
    }

    @Test
    void missingVerifiedIssuerFailsClosedInManagedMode() {
        var policy = policy(List.of(source("policy-1", List.of("yuheng.mcp.billing"), List.of("billing:read"))));
        var chain = mock(GatewaySecurityChain.class);
        when(chain.execute(any(), any(), any(), any())).thenAnswer(call -> {
            GatewayAuthContext context = call.getArgument(1);
            var result = mock(GatewaySecurityResult.class);
            when(result.context()).thenReturn(context.withPrincipal(new GatewayPrincipal(
                    "subject-1", "USER", "1001", null, true, Map.of())));
            return Mono.just(result);
        });
        var authenticator = new McpGatewayIdentityAuthenticator(chain, selected -> policy, "node-1", Clock.systemUTC());
        assertEquals(503, assertThrows(GatewaySecurityException.class,
                () -> authenticator.authenticate(REQUEST, SERVER).block(Duration.ofSeconds(2))).httpStatus());
    }

    @Test
    void missingOrEmptyPublishedPolicyNeverCallsTheSecurityProvider() {
        var chain = mock(GatewaySecurityChain.class);
        var empty = policy(List.of(source("policy-1", List.of("yuheng.mcp.billing"), List.of())));
        for (Function<McpRuntimeServer, GatewaySecurityPolicy> resolver : List.<Function<McpRuntimeServer, GatewaySecurityPolicy>>of(
                selected -> null, selected -> empty, selected -> { throw new IllegalArgumentException("invalid policy"); })) {
            var authenticator = new McpGatewayIdentityAuthenticator(chain, resolver, "node-1", Clock.systemUTC());
            assertEquals(503, assertThrows(GatewaySecurityException.class,
                    () -> authenticator.authenticate(REQUEST, SERVER).block(Duration.ofSeconds(2))).httpStatus());
        }
        verifyNoInteractions(chain);
    }

    @Test
    void factoryUsesOneActiveSnapshotAndRejectsMissingAmbiguousStaleOrUnreadyBindings() {
        var source = source("policy-1", List.of("yuheng.mcp.billing"), List.of("billing:read"));
        var activation = activation(List.of(source));
        var compiled = resolve(activation, SERVER);
        assertEquals(Set.of("billing:read"), compiled.requiredOAuthScopes());
        verify(activation, times(1)).active();
        assertUnavailable(activation(List.of()), SERVER);
        assertUnavailable(activation(List.of(source, source("policy-2", List.of("yuheng.mcp.billing"), List.of("billing:write")))), SERVER);
        assertUnavailable(activation(List.of(source("wrong", List.of("yuheng.mcp.other"), List.of("billing:read")))), SERVER);
        assertUnavailable(activation, new McpRuntimeServer("different-id", SERVER.serverCode(), "Billing",
                null, null, SERVER.dialects(), SERVER.resourceUri(), 30, true));
        var unready = activation(List.of(source));
        when(unready.status().ready()).thenReturn(false);
        assertUnavailable(unready, SERVER);
        var unavailable = activation(List.of(source));
        when(unavailable.active()).thenReturn(null);
        assertUnavailable(unavailable, SERVER);
    }

    private static GatewayRuntimePolicy source(String id, List<String> operations, List<String> scopes) {
        return new GatewayRuntimePolicy(id, "SECURITY", "PUBLIC", Map.of(
                "operationIds", operations, "requiredOAuthScopes", scopes,
                "authenticationMode", "REQUIRED", "credentialExtractorIds", List.of("bearer"),
                "authenticationProviderIds", List.of("jwt"), "routeSecurityType", "IDENTITY_PROTECTED",
                "credentialForwardingMode", "ORIGINAL_BEARER"));
    }

    private static GatewaySecurityPolicy policy(List<GatewayRuntimePolicy> sources) {
        return resolve(activation(sources), SERVER);
    }

    @SuppressWarnings("unchecked")
    private static GatewayRuleActivationApplier<McpGatewayCompiledRulesDTO> activation(List<GatewayRuntimePolicy> sources) {
        GatewayRuleActivationApplier<McpGatewayCompiledRulesDTO> activation = mock(GatewayRuleActivationApplier.class, RETURNS_DEEP_STUBS);
        var active = mock(McpGatewayCompiledRulesDTO.class, RETURNS_DEEP_STUBS);
        doReturn(active).when(activation).active();
        when(activation.status().ready()).thenReturn(true);
        when(active.mcpRules().serversByCode()).thenReturn(Map.of(SERVER.serverCode(), SERVER));
        when(active.snapshot().content().securityPolicies()).thenReturn(sources);
        return activation;
    }

    private static GatewaySecurityPolicy resolve(GatewayRuleActivationApplier<McpGatewayCompiledRulesDTO> activation,
            McpRuntimeServer server) {
        try {
            var method = McpGatewayEngineConfiguration.class.getDeclaredMethod("managedSecurityPolicy",
                    GatewayRuleActivationApplier.class, GatewaySecurityCapabilityRegistry.class, McpRuntimeServer.class);
            method.setAccessible(true);
            return (GatewaySecurityPolicy) method.invoke(null, activation, mock(GatewaySecurityCapabilityRegistry.class), server);
        } catch (InvocationTargetException failure) {
            throw (RuntimeException) failure.getCause();
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    private static void assertUnavailable(GatewayRuleActivationApplier<McpGatewayCompiledRulesDTO> activation,
            McpRuntimeServer server) {
        assertEquals(503, assertThrows(GatewaySecurityException.class, () -> resolve(activation, server)).httpStatus());
    }
}
