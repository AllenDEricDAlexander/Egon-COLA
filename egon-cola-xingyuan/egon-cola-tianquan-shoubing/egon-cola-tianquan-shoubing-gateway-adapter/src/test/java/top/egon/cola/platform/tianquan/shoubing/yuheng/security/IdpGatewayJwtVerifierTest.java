package top.egon.cola.platform.tianquan.shoubing.yuheng.security;

import java.lang.reflect.Method;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import top.egon.cola.component.yuheng.contract.protocol.AccessZone;
import top.egon.cola.component.yuheng.contract.protocol.GatewayProtocol;
import top.egon.cola.component.yuheng.core.context.GatewayPrincipal;
import top.egon.cola.component.yuheng.core.security.GatewayAuthContext;
import top.egon.cola.component.yuheng.core.security.GatewayCredential;
import top.egon.cola.component.yuheng.core.security.SecurityDecision;
import top.egon.cola.platform.tianquan.shoubing.contract.PrincipalType;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServerStatus;
import top.egon.cola.platform.tianquan.shoubing.starter.security.OAuthRevocationReader;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TenantIssuerAuthenticationManagerComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.security.VerifiedPrincipalConverter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.VerifiedJwtBO;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityOAuthClientStateReader;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityResourceServerState;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityResourceServerStateReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdpGatewayJwtVerifierTest {

    private static final Instant NOW = Instant.parse("2026-10-09T00:00:00Z");
    private static final URI RESOURCE = URI.create("https://api.example/orders");

    @Test
    void managedFactoryVerifiesBothTenantsAndServiceThroughTheSharedPorts() throws Exception {
        Method factory = Arrays.stream(IdpGatewayJwtVerifier.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("createConfigured"))
                .findFirst().orElse(null);
        assertThat(factory).as("managed Gateway must select the new shared verifier factory").isNotNull();
        JwtDecoder legacyDecoder = mock(JwtDecoder.class);
        when(legacyDecoder.decode(anyString())).thenThrow(new AssertionError("legacy decoder must not receive new tokens"));
        IdentityResourceServerStateReader resourceStates = mock(IdentityResourceServerStateReader.class);
        IdentityOAuthClientStateReader clientStates = mock(IdentityOAuthClientStateReader.class);
        GatewayResourceServerResolver resources = mock(GatewayResourceServerResolver.class);
        IdentityResourceServerState resource = new IdentityResourceServerState("orders", RESOURCE,
                "commerce", "orders", "test", ResourceServerStatus.ACTIVE, 1);
        when(resources.resolve(any())).thenReturn(resource);
        when(resourceStates.read("orders")).thenReturn(Optional.of(resource));
        TenantIssuerAuthenticationManagerComponent manager = mock(TenantIssuerAuthenticationManagerComponent.class);
        when(manager.isConfigured()).thenReturn(true);
        OAuthRevocationReader revocations = mock(OAuthRevocationReader.class);
        when(revocations.check(any())).thenReturn(OAuthRevocationReader.Result.valid());
        VerifiedPrincipalConverter converter = generatedConverter();
        IdpGatewayJwtVerifier verifier = (IdpGatewayJwtVerifier) factory.invoke(null,
                legacyDecoder, resourceStates, clientStates, resources, "platform", Clock.fixed(NOW, ZoneOffset.UTC),
                manager, revocations, converter, null);
        for (String tenant : List.of("1001", "1002")) {
            Jwt jwt = token(tenant, PrincipalType.USER, RESOURCE);
            when(manager.authenticate("user-" + tenant)).thenReturn(VerifiedJwtBO.builder()
                    .jwt(jwt).principalType(PrincipalType.USER).build());
            var result = verifier.verify(context(), "user-" + tenant);
            assertThat(result.failure()).isEqualTo(top.egon.cola.component.yuheng.core.security.AuthenticationFailure.NONE);
            assertThat(result.principal().tenantId()).isEqualTo(tenant);
            verify(manager).authenticate("user-" + tenant);
        }
        when(manager.authenticate("service")).thenReturn(VerifiedJwtBO.builder()
                .jwt(token("1001", PrincipalType.SERVICE, RESOURCE)).principalType(PrincipalType.SERVICE).build());
        var service = verifier.verify(context(), "service");
        assertThat(service.failure()).as("SERVICE verification: %s", service.reason())
                .isEqualTo(top.egon.cola.component.yuheng.core.security.AuthenticationFailure.NONE);
        assertThat(service.principal().principalType()).isEqualTo(PrincipalType.SERVICE);
        verify(revocations, times(3)).check(any());
        verify(legacyDecoder, never()).decode(anyString());
        when(manager.authenticate("wrong-audience")).thenReturn(VerifiedJwtBO.builder()
                .jwt(token("1001", PrincipalType.USER, URI.create("https://api.example/other")))
                .principalType(PrincipalType.USER).build());
        assertThat(verifier.verify(context(), "wrong-audience").failure())
                .isEqualTo(top.egon.cola.component.yuheng.core.security.AuthenticationFailure.INVALID);
        verify(revocations, times(3)).check(any());
        when(revocations.check(any())).thenReturn(OAuthRevocationReader.Result.revoked("OAUTH_TOKEN_REVOKED"));
        assertThat(verifier.verify(context(), "user-1001").failure())
                .isEqualTo(top.egon.cola.component.yuheng.core.security.AuthenticationFailure.INVALID);
        when(revocations.check(any())).thenReturn(OAuthRevocationReader.Result.unavailable("OAUTH_SECURITY_UNAVAILABLE"));
        StepVerifier.create(Mono.from(new IdpIdentityAuthenticationProvider(verifier).authenticate(context(),
                        new GatewayCredential("bearer", "user-1001", Map.of()))))
                .assertNext(decision -> assertThat(decision.decision()).isEqualTo(SecurityDecision.ERROR))
                .verifyComplete();
    }

    private static VerifiedPrincipalConverter generatedConverter() throws Exception {
        return (VerifiedPrincipalConverter) Class.forName(
                "top.egon.cola.platform.tianquan.shoubing.starter.security.VerifiedPrincipalConverterImpl")
                .getDeclaredConstructor().newInstance();
    }

    private static Jwt token(String tenant, PrincipalType type, URI audience) {
        var builder = Jwt.withTokenValue("verified-fixture")
                .headers(headers -> { headers.put("alg", "RS256"); headers.put("kid", "new-key"); headers.put("typ", "at+jwt"); })
                .issuer("https://idp.example/tenants/" + tenant)
                .subject(type == PrincipalType.USER ? "operator" : "orders-service")
                .audience(List.of(audience.toASCIIString())).issuedAt(NOW).notBefore(NOW).expiresAt(NOW.plusSeconds(300))
                .jti("token-" + tenant).claim("principal_type", type.name()).claim("tenant_id", tenant)
                .claim("client_id", type == PrincipalType.USER ? "edge-ui" : "orders-service").claim("scope", "read")
                .claim("acr", "PASSWORD").claim("auth_time", NOW);
        if (type == PrincipalType.SERVICE) {
            builder.claim("app_id", "orders")
                    .claim(top.egon.cola.platform.tianquan.shoubing.contract.IdpClaimNames.SCOPE_CONTEXT, "TENANT")
                    .claim("resource_version", 1L)
                    .claim("credential_id", "501:0").claim("source_biz", "commerce").claim("source_app", "orders")
                    .claim("source_env", "test");
        }
        return builder.build();
    }

    private static GatewayAuthContext context() {
        return new GatewayAuthContext(AccessZone.PUBLIC, GatewayProtocol.HTTP, "operation", "route", "policy",
                "/orders", "GET", Set.of("bearer"), GatewayPrincipal.anonymous(), "127.0.0.1", "trace", "request",
                NOW.plusSeconds(5), "release", Map.of("tianquan-shoubing.resource-uri", RESOURCE.toASCIIString()));
    }
}
