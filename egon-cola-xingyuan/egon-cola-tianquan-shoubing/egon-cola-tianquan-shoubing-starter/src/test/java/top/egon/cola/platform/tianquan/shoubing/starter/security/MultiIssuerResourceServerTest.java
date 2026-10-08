package top.egon.cola.platform.tianquan.shoubing.starter.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;
import org.redisson.config.Config;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.platform.tianquan.shoubing.contract.AuthenticationContext;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.contract.IdpPrincipal;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties.TrustedIssuerSettings;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.VerifiedJwtBO;
import top.egon.cola.platform.tianquan.shoubing.starter.security.impl.RedisOAuthRevocationReader;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityOAuthClientStateReader;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityResourceServerStateReader;

import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MultiIssuerResourceServerTest {

    private static final String SECURITY_PACKAGE =
            "top.egon.cola.platform.tianquan.shoubing.starter.security.";

    @AfterEach
    void clearThreadState() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void unavailableIsDistinctFromAnInvalidCredential() {
        boolean unavailablePermitted = Arrays.stream(
                        AccessTokenVerification.class.getPermittedSubclasses())
                .anyMatch(type -> type.getSimpleName().equals("Unavailable"));

        assertThat(unavailablePermitted)
                .as("shared revocation failure needs a typed UNAVAILABLE outcome")
                .isTrue();
    }

    @Test
    void sharedRevocationOutageMapsTo503WithoutExposingDependencyDetails()
            throws Exception {
        UserAccessTokenVerifier userVerifier = mock(UserAccessTokenVerifier.class);
        ServiceAccessTokenVerifier serviceVerifier = mock(ServiceAccessTokenVerifier.class);
        when(userVerifier.verify("synthetic-bearer"))
                .thenReturn(unavailableOrCurrentInvalidResult());
        IdpBearerAuthenticationFilter filter = new IdpBearerAuthenticationFilter(
                userVerifier,
                serviceVerifier,
                new IdpEndpointAuthenticationPolicy(),
                new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/business/orders");
        request.addHeader("Authorization", "Bearer synthetic-bearer");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            throw new AssertionError("unavailable authentication must not reach the endpoint");
        });

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString())
                .contains("OAUTH_SECURITY_STATE_UNAVAILABLE")
                .doesNotContain("redis.internal", "connection refused", "synthetic-bearer");
    }

    @Test
    void trustedIssuerSettingsAndRegistryHaveExplicitExtensionPoints() {
        boolean configuredIssuerMap = Arrays.stream(
                        top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure
                                .IdpStarterProperties.class.getMethods())
                .anyMatch(method -> method.getName().equals("getTrustedIssuers"));

        assertThat(configuredIssuerMap)
                .as("a Resource Server needs an explicit finite trusted issuer map")
                .isTrue();
        assertThat(typeExists(SECURITY_PACKAGE + "TrustedIssuerRegistry"))
                .as("decoder selection must be backed by the explicit registry")
                .isTrue();
        assertThat(typeExists(SECURITY_PACKAGE + "TenantIssuerAuthenticationManagerComponent"))
                .isTrue();
    }

    @Test
    void autoConfigurationBindsMultiIssuerMapAndStrictRevocationReader() {
        Config redisConfig = new Config();
        redisConfig.useSingleServer().setAddress("rediss://redis.example:6379");
        RedissonClient redisson = mock(RedissonClient.class);
        when(redisson.getConfig()).thenReturn(redisConfig);
        String issuer = "https://idp.example/tenants/1001";

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure
                                .IdpStarterAutoConfiguration.class))
                .withBean("rbac3RuntimeRedissonClient", RedissonClient.class, () -> redisson)
                .withBean("jacksonObjectMapper", ObjectMapper.class, ObjectMapper::new)
                .withPropertyValues(
                        "egon.cola.platform.tianquan.shoubing.enabled=true",
                        "egon.cola.platform.tianquan.shoubing.resource-server-id=orders-resource",
                        "egon.cola.platform.tianquan.shoubing.resource-uri=https://api.example/orders",
                        "egon.cola.platform.tianquan.shoubing.revocation.primary-endpoint=rediss://redis.example:6379",
                        "egon.cola.platform.tianquan.shoubing.revocation.epoch=epoch-1",
                        "egon.cola.platform.tianquan.shoubing.trusted-issuers.[tenant-a].issuer=" + issuer,
                        "egon.cola.platform.tianquan.shoubing.trusted-issuers.[tenant-a].jwks-uri=" + issuer + "/.well-known/jwks.json",
                        "egon.cola.platform.tianquan.shoubing.trusted-issuers.[tenant-a].realm-key=tenant:1001",
                        "egon.cola.platform.tianquan.shoubing.trusted-issuers.[tenant-a].realm-type=TENANT",
                        "egon.cola.platform.tianquan.shoubing.trusted-issuers.[tenant-a].tenant-id=1001",
                        "egon.cola.platform.tianquan.shoubing.trusted-issuers.[tenant-a].status=ACTIVE",
                        "egon.cola.platform.tianquan.shoubing.trusted-issuers.[tenant-a].trust-version=1")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(IdpJwtVerifier.class);
                    assertThat(context).hasSingleBean(IdpBearerAuthenticationFilter.class);
                    assertThat(context).hasSingleBean(OAuthRevocationReader.class);
                    assertThat(context).hasBean("trustedIssuerRegistry");
                    assertThat(context).hasBean("verifiedPrincipalConverter");
                });
    }

    @Test
    void trustedIssuerMapRejectsExternalJwksAndExcessiveIssuerCount() {
        String issuerA = "https://idp.example/tenants/1001";
        String issuerB = "https://idp.example/tenants/1002";
        URI jwksA = URI.create(issuerA + "/.well-known/jwks.json");
        URI jwksB = URI.create(issuerB + "/.well-known/jwks.json");
        URI resource = URI.create("https://api.example/orders");
        IdpStarterProperties properties = multiIssuerProperties(issuerA, jwksA, issuerB, jwksB, resource);

        TrustedIssuerSettings unsafe = trustedTenantSettings(
                issuerA, URI.create("https://keys.attacker.example/jwks"), "tenant:1001", "1001");
        properties.setTrustedIssuers(Map.of("tenant-1001", unsafe));
        assertThatThrownBy(() -> new TrustedIssuerRegistry(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("issuer origin");

        IdpStarterProperties bounded = multiIssuerProperties(
                issuerA, jwksA, issuerB, jwksB, resource);
        bounded.getJwksCache().setMaxIssuers(1);
        assertThatThrownBy(() -> new TrustedIssuerRegistry(bounded))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("max-issuers");
    }

    @Test
    void localIssuerAndRedisPlaintextIsRestrictedToExactLoopbackHosts() {
        String localIssuer = "http://127.0.0.1:18120/tenants/1001";
        URI localJwks = URI.create(localIssuer + "/.well-known/jwks.json");
        IdpStarterProperties local = new IdpStarterProperties();
        local.setResourceServerId("orders-resource");
        local.setResourceUri(URI.create("https://api.example/orders"));
        local.getRevocation().setPrimaryEndpoint("redis://127.0.0.1:6379");
        local.getRevocation().setEpoch("epoch-local");
        local.setTrustedIssuers(Map.of("tenant-1001",
                trustedTenantSettings(localIssuer, localJwks, "tenant:1001", "1001")));

        assertThat(new TrustedIssuerRegistry(local)
                .findByIssuer(localIssuer)).isPresent();

        IdpStarterProperties remotePlaintext = new IdpStarterProperties();
        remotePlaintext.setResourceServerId("orders-resource");
        remotePlaintext.setResourceUri(URI.create("https://api.example/orders"));
        remotePlaintext.getRevocation().setPrimaryEndpoint("rediss://redis.example:6379");
        remotePlaintext.getRevocation().setEpoch("epoch-1");
        String remoteIssuer = "http://auth.example/tenants/1001";
        remotePlaintext.setTrustedIssuers(Map.of("tenant-1001",
                trustedTenantSettings(remoteIssuer,
                        URI.create(remoteIssuer + "/.well-known/jwks.json"),
                        "tenant:1001", "1001")));
        assertThatThrownBy(() -> new TrustedIssuerRegistry(remotePlaintext))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("loopback HTTP");
    }

    @Test
    void legacyCutoverPropertiesMustBePairedInBothSingleAndMultiIssuerModes() {
        IdpStarterProperties legacy = new IdpStarterProperties();
        legacy.setIssuer("https://idp.example");
        legacy.setJwkSetUri("https://idp.example/.well-known/jwks.json");
        legacy.setResourceServerId("orders-resource");
        legacy.setResourceUri(URI.create("https://api.example/orders"));
        legacy.validate();

        legacy.getMigration().setLegacyIssuer("https://idp.example");
        assertThatThrownBy(legacy::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be configured together");

        legacy.getMigration().setLegacyIssuer(null);
        legacy.getMigration().setLegacyTenantAllowlistKey("migration:tenant:allowlist");
        assertThatThrownBy(legacy::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be configured together");

        legacy.getMigration().setLegacyIssuer("https://idp.example");
        legacy.validate();

        IdpStarterProperties multiIssuer = multiIssuerProperties(
                "https://idp.example/tenants/1001",
                URI.create("https://idp.example/tenants/1001/.well-known/jwks.json"),
                "https://idp.example/tenants/1002",
                URI.create("https://idp.example/tenants/1002/.well-known/jwks.json"),
                URI.create("https://api.example/orders"));
        multiIssuer.getMigration().setLegacyTenantAllowlistKey("migration:tenant:allowlist");
        assertThatThrownBy(multiIssuer::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be configured together");

        multiIssuer.getMigration().setLegacyIssuer("https://idp.example");
        multiIssuer.validate();
    }

    @Test
    void verifiedMdcIsAvailableDownstreamAndRestoredAfterAnException()
            throws Exception {
        UserAccessTokenVerifier userVerifier = mock(UserAccessTokenVerifier.class);
        ServiceAccessTokenVerifier serviceVerifier = mock(ServiceAccessTokenVerifier.class);
        IdentityPrincipal principal = new IdentityPrincipal(
                "subject-42",
                "tenant-42",
                "jti-42",
                java.util.Set.of("https://api.example/orders"),
                Instant.parse("2026-10-09T00:00:00Z"),
                Instant.parse("2026-10-09T00:05:00Z"),
                AuthenticationContext.of("PASSWORD", Instant.parse("2026-10-09T00:00:00Z")));
        when(userVerifier.verify("synthetic-bearer"))
                .thenReturn(new AccessTokenVerification.Valid<>(principal));
        IdpBearerAuthenticationFilter filter = new IdpBearerAuthenticationFilter(
                userVerifier,
                serviceVerifier,
                new IdpEndpointAuthenticationPolicy(),
                new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/business/orders");
        request.addHeader("Authorization", "Bearer synthetic-bearer");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MDC.put("tenantId", "outer-tenant");
        MDC.put("userId", "outer-user");
        String[] observed = new String[2];

        try {
            filter.doFilter(request, response, (servletRequest, servletResponse) -> {
                observed[0] = MDC.get("tenantId");
                observed[1] = MDC.get("userId");
                throw new ServletException("synthetic downstream failure");
            });
        } catch (ServletException expected) {
            assertThat(expected).hasMessage("synthetic downstream failure");
        }

        assertThat(observed).containsExactly("tenant-42", "subject-42");
        assertThat(MDC.get("tenantId")).isEqualTo("outer-tenant");
        assertThat(MDC.get("userId")).isEqualTo("outer-user");
        assertThat(typeExists(SECURITY_PACKAGE + "VerifiedTenantContextFilter")).isTrue();
        assertThat(typeExists(
                "top.egon.cola.platform.tianquan.jianshen.admin.shared.tenant.domain.TenantContext"))
                .as("starter implementation must not depend on an admin-layer ThreadLocal")
                .isFalse();
    }

    @Test
    void validatedOauthScopesBecomeAuthoritiesWithoutReplacingIdentity() throws Exception {
        UserAccessTokenVerifier userVerifier = mock(UserAccessTokenVerifier.class);
        ServiceAccessTokenVerifier serviceVerifier = mock(ServiceAccessTokenVerifier.class);
        IdentityPrincipal principal = new IdentityPrincipal(
                "subject-42",
                "tenant-42",
                "jti-42",
                Set.of("https://api.example/orders"),
                Instant.parse("2026-10-09T00:00:00Z"),
                Instant.parse("2026-10-09T00:05:00Z"),
                AuthenticationContext.of("PASSWORD", Instant.parse("2026-10-09T00:00:00Z")));
        when(userVerifier.verify("synthetic-bearer"))
                .thenReturn(validResultWithScope(principal, Set.of("orders:read")));
        IdpBearerAuthenticationFilter filter = new IdpBearerAuthenticationFilter(
                userVerifier,
                serviceVerifier,
                new IdpEndpointAuthenticationPolicy(),
                new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/business/orders");
        request.addHeader("Authorization", "Bearer synthetic-bearer");
        List<String> authorities = new ArrayList<>();

        filter.doFilter(request, new MockHttpServletResponse(),
                (servletRequest, servletResponse) -> authorities.addAll(
                        SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                                .stream().map(value -> value.getAuthority()).toList()));

        assertThat(authorities).containsExactly("SCOPE_orders:read");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void verifiesTwoExactIssuerKeysAndRejectsTenantAudienceAndUnknownIssuer()
            throws Exception {
        Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        URI resource = URI.create("https://api.example/orders");
        String issuerA = "https://idp.example/tenants/1001";
        String issuerB = "https://idp.example/tenants/1002";
        URI jwksA = URI.create(issuerA + "/.well-known/jwks.json");
        URI jwksB = URI.create(issuerB + "/.well-known/jwks.json");
        RSAKey keyA = new RSAKeyGenerator(2048).keyID("same-kid").algorithm(JWSAlgorithm.RS256).generate();
        RSAKey keyB = new RSAKeyGenerator(2048).keyID("same-kid").algorithm(JWSAlgorithm.RS256).generate();
        IdpStarterProperties properties = multiIssuerProperties(issuerA, jwksA, issuerB, jwksB, resource);
        TrustedIssuerRegistry registry = new TrustedIssuerRegistry(properties);
        TenantIssuerAuthenticationManagerComponent manager =
                new TenantIssuerAuthenticationManagerComponent(
                        registry, properties, java.time.Clock.fixed(now, java.time.ZoneOffset.UTC));
        manager.initialize();
        RestTemplate restTemplate = restOperations(manager);
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate)
                .ignoreExpectOrder(true)
                .build();
        server.expect(ExpectedCount.once(), MockRestRequestMatchers.requestTo(jwksA.toASCIIString()))
                .andRespond(MockRestResponseCreators.withSuccess(
                        new com.nimbusds.jose.jwk.JWKSet(keyA.toPublicJWK()).toString(),
                        MediaType.APPLICATION_JSON));
        server.expect(ExpectedCount.once(), MockRestRequestMatchers.requestTo(jwksB.toASCIIString()))
                .andRespond(MockRestResponseCreators.withSuccess(
                        new com.nimbusds.jose.jwk.JWKSet(keyB.toPublicJWK()).toString(),
                        MediaType.APPLICATION_JSON));
        String tokenA = userToken(keyA, issuerA, "1001", resource, now);
        String tokenB = userToken(keyB, issuerB, "1002", resource, now);
        String wrongAudience = userToken(keyA, issuerA, "1001",
                URI.create("https://other.example/orders"), now);
        String wrongTenant = userToken(keyA, issuerA, "1002", resource, now);
        String wrongKeyForB = userToken(keyA, issuerB, "1002", resource, now);
        String unknownIssuer = userToken(keyA, "https://evil.example/tenants/1001", "1001", resource, now);
        OAuthRevocationReader revocations = mock(OAuthRevocationReader.class);
        when(revocations.check(org.mockito.ArgumentMatchers.any(VerifiedJwtBO.class)))
                .thenReturn(OAuthRevocationReader.Result.valid());
        IdpJwtVerifier verifier = IdpJwtVerifier.createConfigured(
                null,
                mock(IdentityResourceServerStateReader.class),
                mock(IdentityOAuthClientStateReader.class),
                "orders-resource",
                resource,
                "platform-audience",
                java.time.Clock.fixed(now, java.time.ZoneOffset.UTC),
                manager,
                revocations,
                generatedConverter(),
                null);

        assertThat(verifier.verifyUser(tokenA))
                .isInstanceOf(AccessTokenVerification.Valid.class);
        VerifiedJwtBO issuerBToken = manager.authenticate(tokenB);
        assertThat(issuerBToken.getTrustedIssuer().issuer()).isEqualTo(URI.create(issuerB));
        String issuerHash = TrustedIssuerRegistry.issuerHash(URI.create(issuerB));
        String redisSlotPrefix = "egon:oauth:security:v2:{" + issuerHash + "}:";
        List<Object> redisKeys = RedisOAuthRevocationReader.redisKeys(issuerBToken, issuerHash);
        assertThat(redisKeys).hasSize(7).allSatisfy(key ->
                assertThat(key.toString()).startsWith(redisSlotPrefix));
        assertThat(redisKeys).contains(
                redisSlotPrefix + "ready",
                redisSlotPrefix + "generation:realm",
                redisSlotPrefix + "generation:client:501");
        assertThat(RedisOAuthRevocationReader.READ_SECURITY_SNAPSHOT_SCRIPT)
                .contains("EXISTS", "generation", "lastRevision", "enabled ~= '1'");
        assertThat(RedisOAuthRevocationReader.interpret(
                List.of("U", "OAUTH_SECURITY_READY_NOT_AVAILABLE")).status())
                .isEqualTo(OAuthRevocationReader.Status.UNAVAILABLE);
        assertThat(RedisOAuthRevocationReader.interpret(
                List.of("R", "JWT_SECURITY_VERSION_STALE")).status())
                .isEqualTo(OAuthRevocationReader.Status.REVOKED);
        assertThatThrownBy(() -> manager.authenticate(wrongKeyForB))
                .isInstanceOf(RuntimeException.class);
        assertThat(verifier.verifyUser(wrongAudience))
                .isInstanceOf(AccessTokenVerification.Invalid.class);
        assertThat(verifier.verifyUser(wrongTenant))
                .isInstanceOf(AccessTokenVerification.Invalid.class);
        assertThatThrownBy(() -> manager.authenticate(unknownIssuer))
                .isInstanceOf(TenantIssuerAuthenticationManagerComponent.InvalidIssuerException.class);
        registry.replaceConfiguredEntries(Map.of(
                "tenant-1002", trustedTenantSettings(issuerB, jwksB, "tenant:1002", "1002")));
        assertThatThrownBy(() -> manager.authenticate(tokenA))
                .isInstanceOf(TenantIssuerAuthenticationManagerComponent.InvalidIssuerException.class);
        TrustedIssuerSettings rolledBack =
                trustedTenantSettings(issuerB, jwksB, "tenant:1002", "1002");
        rolledBack.setTrustVersion(0L);
        assertThatThrownBy(() -> registry.replaceConfiguredEntries(Map.of("tenant-1002", rolledBack)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("trustVersion must not decrease");
        verify(revocations, times(1)).check(org.mockito.ArgumentMatchers.any(VerifiedJwtBO.class));
        server.verify();
    }

    @Test
    void sharedRevocationClientMustMatchTheConfiguredSinglePrimary() {
        Config config = new Config();
        config.useSingleServer().setAddress("rediss://redis.example:6379");

        RedisOAuthRevocationReader.validatePrimaryEndpoint(
                config, "rediss://redis.example:6379", new ObjectMapper());
        assertThatThrownBy(() -> RedisOAuthRevocationReader.validatePrimaryEndpoint(
                config, "rediss://other.example:6379", new ObjectMapper()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not match");

        Config cluster = new Config();
        cluster.useClusterServers().addNodeAddress("rediss://redis.example:6379");
        assertThatThrownBy(() -> RedisOAuthRevocationReader.validatePrimaryEndpoint(
                cluster, "rediss://redis.example:6379", new ObjectMapper()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("single PRIMARY");
    }

    private AccessTokenVerification<top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal>
            unavailableOrCurrentInvalidResult() {
        String unavailableName = SECURITY_PACKAGE + "AccessTokenVerification$Unavailable";
        Optional<Class<?>> unavailableType = loadType(unavailableName);
        if (unavailableType.isPresent()) {
            try {
                Constructor<?> constructor = unavailableType.orElseThrow()
                        .getConstructor(String.class);
                return (AccessTokenVerification<IdentityPrincipal>) constructor
                        .newInstance("OAUTH_SECURITY_STATE_UNAVAILABLE");
            } catch (ReflectiveOperationException failure) {
                throw new AssertionError("typed UNAVAILABLE result must have a stable constructor", failure);
            }
        }
        return new AccessTokenVerification.Invalid<>("OAUTH_SECURITY_STATE_UNAVAILABLE");
    }

    @SuppressWarnings("unchecked")
    private AccessTokenVerification<IdentityPrincipal> validResultWithScope(
            IdentityPrincipal principal,
            Set<String> scopes
    ) {
        try {
            Constructor<?> constructor = AccessTokenVerification.Valid.class
                    .getConstructor(IdpPrincipal.class, Set.class);
            return (AccessTokenVerification<IdentityPrincipal>) constructor
                    .newInstance(principal, scopes);
        } catch (NoSuchMethodException compatibilityBaseline) {
            return new AccessTokenVerification.Valid<>(principal);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("verified scopes must be preserved by the authentication result", failure);
        }
    }

    private boolean typeExists(String name) {
        return loadType(name).isPresent();
    }

    private Optional<Class<?>> loadType(String name) {
        try {
            return Optional.of(Class.forName(name));
        } catch (ClassNotFoundException missing) {
            return Optional.empty();
        }
    }

    private IdpStarterProperties multiIssuerProperties(
            String issuerA,
            URI jwksA,
            String issuerB,
            URI jwksB,
            URI resource) {
        IdpStarterProperties properties = new IdpStarterProperties();
        properties.setResourceServerId("orders-resource");
        properties.setResourceUri(resource);
        properties.getRevocation().setPrimaryEndpoint("rediss://redis.example:6379");
        properties.getRevocation().setEpoch("epoch-1");
        Map<String, TrustedIssuerSettings> issuers = new LinkedHashMap<>();
        issuers.put("tenant-1001", trustedTenantSettings(issuerA, jwksA, "tenant:1001", "1001"));
        issuers.put("tenant-1002", trustedTenantSettings(issuerB, jwksB, "tenant:1002", "1002"));
        properties.setTrustedIssuers(issuers);
        return properties;
    }

    private TrustedIssuerSettings trustedTenantSettings(
            String issuer,
            URI jwksUri,
            String realmKey,
            String tenantId) {
        TrustedIssuerSettings settings = new TrustedIssuerSettings();
        settings.setIssuer(issuer);
        settings.setJwksUri(jwksUri.toASCIIString());
        settings.setRealmKey(realmKey);
        settings.setRealmType("TENANT");
        settings.setTenantId(tenantId);
        settings.setStatus("ACTIVE");
        settings.setTrustVersion(1L);
        return settings;
    }

    private RestTemplate restOperations(TenantIssuerAuthenticationManagerComponent manager)
            throws ReflectiveOperationException {
        Field field = TenantIssuerAuthenticationManagerComponent.class
                .getDeclaredField("jwksRestOperations");
        field.setAccessible(true);
        Object restOperations = field.get(manager);
        assertThat(restOperations).isInstanceOf(RestTemplate.class);
        return (RestTemplate) restOperations;
    }

    private String userToken(
            RSAKey signingKey,
            String issuer,
            String tenantId,
            URI audience,
            Instant now) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("subject-" + tenantId)
                .audience(List.of(audience.toASCIIString()))
                .issueTime(Date.from(now.minusSeconds(1)))
                .notBeforeTime(Date.from(now.minusSeconds(1)))
                .expirationTime(Date.from(now.plusSeconds(300)))
                .claim("jti", "100000000000000" + tenantId)
                .claim("tenant_id", tenantId)
                .claim("client_id", "browser-app")
                .claim("principal_type", "USER")
                .claim("scope_context", "TENANT")
                .claim("authorization_id", "801")
                .claim("client_registration_id", "501")
                .claim("security_version", Map.of("realm", 1L, "client", 2L, "subject", 3L))
                .claim("scope", List.of("orders:read"))
                .claim("acr", "PASSWORD")
                .claim("auth_time", Date.from(now.minusSeconds(1)))
                .build();
        SignedJWT token = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID("same-kid")
                .type(new JOSEObjectType("at+jwt"))
                .build(), claims);
        token.sign(new RSASSASigner(signingKey));
        return token.serialize();
    }

    private VerifiedPrincipalConverter generatedConverter() throws ReflectiveOperationException {
        return (VerifiedPrincipalConverter) Class.forName(
                SECURITY_PACKAGE + "VerifiedPrincipalConverterImpl")
                .getConstructor().newInstance();
    }
}
