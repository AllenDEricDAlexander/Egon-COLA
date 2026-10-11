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
import org.junit.jupiter.api.io.TempDir;
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
    @TempDir java.nio.file.Path temporary;

    @Test
    void managedFactoryVerifiesBothTenantsAndServiceThroughTheSharedPorts() throws Exception {
        Method factory = Arrays.stream(IdpGatewayJwtVerifier.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("createConfigured") && method.getParameterCount() == 10)
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

    @Test
    @SuppressWarnings("unchecked")
    void configuredSingleRs256VerificationCapturesExactScopesAndDurableProof() throws Exception {
        URI issuer = URI.create("https://idp.example/tenants/1001");
        var properties = new top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties();
        properties.setResourceServerId("orders"); properties.setResourceUri(RESOURCE);
        properties.getRevocation().setPrimaryEndpoint("rediss://redis.example:6379"); properties.getRevocation().setEpoch("epoch-1");
        var trusted = new top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties.TrustedIssuerSettings();
        trusted.setIssuer(issuer.toASCIIString()); trusted.setJwksUri(issuer + "/.well-known/jwks.json");
        trusted.setRealmKey("tenant:1001"); trusted.setRealmType("TENANT"); trusted.setTenantId("1001");
        trusted.setStatus("ACTIVE"); trusted.setTrustVersion(1);
        properties.setTrustedIssuers(Map.of("tenant-1001", trusted));
        var manager = org.mockito.Mockito.spy(new TenantIssuerAuthenticationManagerComponent(
                new top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry(properties), properties,
                Clock.fixed(NOW, ZoneOffset.UTC)));
        manager.initialize();
        var rest = TenantIssuerAuthenticationManagerComponent.class.getDeclaredField("jwksRestOperations"); rest.setAccessible(true);
        var jwks = org.springframework.test.web.client.MockRestServiceServer.bindTo((org.springframework.web.client.RestTemplate) rest.get(manager)).build();
        var key = new com.nimbusds.jose.jwk.gen.RSAKeyGenerator(2048).keyID("key-1")
                .algorithm(com.nimbusds.jose.JWSAlgorithm.RS256).generate();
        jwks.expect(org.springframework.test.web.client.ExpectedCount.once(),
                        org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(issuer + "/.well-known/jwks.json"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                        new com.nimbusds.jose.jwk.JWKSet(key.toPublicJWK()).toString(), org.springframework.http.MediaType.APPLICATION_JSON));
        var state = new IdentityResourceServerState("orders", RESOURCE, "commerce", "orders", "test", ResourceServerStatus.ACTIVE, 1);
        var resources = mock(GatewayResourceServerResolver.class); when(resources.resolve(any())).thenReturn(state);
        var resourceStates = mock(IdentityResourceServerStateReader.class); when(resourceStates.read("orders")).thenReturn(Optional.of(state));
        var revocations = mock(OAuthRevocationReader.class); when(revocations.check(any())).thenReturn(OAuthRevocationReader.Result.valid());
        var redis = mock(org.redisson.api.RedissonClient.class);
        org.redisson.api.RStream<String, String> stream = mock(org.redisson.api.RStream.class);
        org.mockito.Mockito.doReturn(stream).when(redis).getStream(org.mockito.ArgumentMatchers.eq("egon:oauth:audit:v1"),
                org.mockito.ArgumentMatchers.eq(org.redisson.client.codec.StringCodec.INSTANCE));
        java.util.List<Map<String, String>> events = new java.util.ArrayList<>();
        when(stream.add(any(org.redisson.api.stream.StreamAddArgs.class))).thenAnswer(call -> {
            org.redisson.api.stream.StreamAddParams<String, String> fields = call.getArgument(0);
            events.add(Map.copyOf(fields.getEntries())); return new org.redisson.api.StreamMessageId(events.size(), 1);
        });
        var keyFile = temporary.resolve("audit.key"); java.nio.file.Files.writeString(keyFile, "0123456789abcdef0123456789abcdef");
        java.nio.file.Files.setPosixFilePermissions(keyFile, Set.of(java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                java.nio.file.attribute.PosixFilePermission.OWNER_WRITE));
        var validator = new org.springframework.validation.beanvalidation.LocalValidatorFactoryBean(); validator.afterPropertiesSet();
        try {
            var producer = new top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent(redis,
                    new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules(),
                    new top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthAuditProperties("egon:oauth:audit:v1",
                            temporary.resolve("spool"), 1_048_576, keyFile),
                    new top.egon.cola.platform.tianquan.shoubing.starter.audit.AuditDeliveryProperties(), Clock.fixed(NOW, ZoneOffset.UTC), validator);
            var converter = (top.egon.cola.platform.tianquan.shoubing.starter.security.domain.converter.TokenUsageAuditEventConverter)
                    Class.forName("top.egon.cola.platform.tianquan.shoubing.starter.security.domain.converter.TokenUsageAuditEventConverterImpl")
                            .getConstructor().newInstance();
            var usage = new top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthUsageObservationProperties(true, issuer, false,
                    top.egon.cola.platform.tianquan.shoubing.starter.observability.IdpSecurityObservationReceiverEnum.BIZ);
            var observations = new top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent(
                    producer, converter, usage, sample -> { }, Clock.fixed(NOW, ZoneOffset.UTC), validator, RESOURCE);
            var verifier = IdpGatewayJwtVerifier.createConfigured(null, resourceStates, mock(IdentityOAuthClientStateReader.class), resources,
                    "platform", Clock.fixed(NOW, ZoneOffset.UTC), manager, revocations, generatedConverter(), null, observations);
            String lastToken = null;
            for (boolean service : List.of(false, true)) {
                Map<String, Object> claims = new java.util.LinkedHashMap<>();
                claims.put("iss", issuer.toASCIIString()); claims.put("sub", service ? "orders-service" : "operator");
                claims.put("aud", List.of(RESOURCE.toASCIIString())); claims.put("iat", NOW.minusSeconds(1).getEpochSecond());
                claims.put("nbf", NOW.minusSeconds(1).getEpochSecond()); claims.put("exp", NOW.plusSeconds(300).getEpochSecond());
                claims.put("jti", service ? "902" : "901"); claims.put("tenant_id", "1001");
                claims.put("client_id", service ? "orders-service" : "browser-client"); claims.put("principal_type", service ? "SERVICE" : "USER");
                claims.put("scope_context", "TENANT"); claims.put("authorization_id", "801"); claims.put("client_registration_id", "501");
                claims.put("security_version", service ? Map.of("realm", 1L, "client", 2L) : Map.of("realm", 1L, "client", 2L, "subject", 3L));
                claims.put("scope", "orders:read orders:write");
                if (service) {
                    claims.put("app_id", "orders"); claims.put("resource_version", 1L); claims.put("credential_id", "501:0");
                    claims.put("source_biz", "commerce"); claims.put("source_app", "orders"); claims.put("source_env", "test");
                } else { claims.put("acr", "PASSWORD"); claims.put("auth_time", NOW.minusSeconds(5).getEpochSecond()); }
                var signed = new com.nimbusds.jwt.SignedJWT(new com.nimbusds.jose.JWSHeader.Builder(com.nimbusds.jose.JWSAlgorithm.RS256)
                        .keyID("key-1").type(new com.nimbusds.jose.JOSEObjectType("at+jwt")).build(), com.nimbusds.jwt.JWTClaimsSet.parse(claims));
                signed.sign(new com.nimbusds.jose.crypto.RSASSASigner(key));
                lastToken = signed.serialize();
                var result = verifier.verify(context(), signed.serialize());
                assertThat(result.failure()).isEqualTo(top.egon.cola.component.yuheng.core.security.AuthenticationFailure.NONE);
                assertThat(result.proof()).isNotNull();
                assertThat(result.proof().resource()).isEqualTo(RESOURCE);
                assertThat(result.scopes()).containsExactlyInAnyOrder("orders:read", "orders:write");
                verify(manager, times(1)).authenticate(signed.serialize());
                assertThat(events).hasSize(service ? 2 : 1);
                assertThat(events.getLast()).containsEntry("resource", RESOURCE.toASCIIString()).containsEntry("traceId", "trace")
                        .containsEntry("reasonCode", "TOKEN_USE_VERIFIED").doesNotContainValue(signed.serialize());
            }
            verify(revocations, times(2)).check(any());
            var wrongClaims = new java.util.LinkedHashMap<>(com.nimbusds.jwt.SignedJWT.parse(lastToken).getJWTClaimsSet().toJSONObject());
            wrongClaims.put("aud", List.of("https://api.example/other"));
            var wrong = new com.nimbusds.jwt.SignedJWT(new com.nimbusds.jose.JWSHeader.Builder(com.nimbusds.jose.JWSAlgorithm.RS256)
                    .keyID("key-1").type(new com.nimbusds.jose.JOSEObjectType("at+jwt")).build(), com.nimbusds.jwt.JWTClaimsSet.parse(wrongClaims));
            wrong.sign(new com.nimbusds.jose.crypto.RSASSASigner(key));
            var rejected = verifier.verify(context(), wrong.serialize());
            assertThat(rejected.failure()).isEqualTo(top.egon.cola.component.yuheng.core.security.AuthenticationFailure.INVALID);
            assertThat(rejected.scopes()).isEmpty(); assertThat(rejected.proof()).isNull();
            assertThat(events).hasSize(3);
            assertThat(events.getLast()).containsEntry("reasonCode", "AUTHENTICATION_FAILED").doesNotContainKeys("subjectRef", "clientRef", "tokenRef");
            verify(revocations, times(2)).check(any());
            when(revocations.check(any())).thenReturn(OAuthRevocationReader.Result.unavailable("OAUTH_SECURITY_UNAVAILABLE"));
            var provider = new IdpIdentityAuthenticationProvider(verifier);
            StepVerifier.create(Mono.from(provider.authenticate(context(), new GatewayCredential("bearer", lastToken, Map.of()))))
                    .assertNext(decision -> assertThat(decision.decision()).isEqualTo(SecurityDecision.ERROR)).verifyComplete();
            assertThat(events).hasSize(4);
            assertThat(events.getLast()).doesNotContainKeys("subjectRef", "clientRef", "tokenRef");
            when(revocations.check(any())).thenReturn(OAuthRevocationReader.Result.valid());
            when(stream.add(any(org.redisson.api.stream.StreamAddArgs.class))).thenThrow(new org.redisson.client.RedisException("controlled audit outage"));
            StepVerifier.create(Mono.from(provider.authenticate(context(), new GatewayCredential("bearer", lastToken, Map.of()))))
                    .assertNext(decision -> assertThat(decision.decision()).isEqualTo(SecurityDecision.ERROR)).verifyComplete();
            assertThat(events).hasSize(4);
            try (var frames = java.nio.file.Files.list(temporary.resolve("spool"))) {
                assertThat(frames.filter(path -> path.toString().endsWith(".evt")).count()).isEqualTo(5);
            }
            jwks.verify();
        } finally { validator.close(); }
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
