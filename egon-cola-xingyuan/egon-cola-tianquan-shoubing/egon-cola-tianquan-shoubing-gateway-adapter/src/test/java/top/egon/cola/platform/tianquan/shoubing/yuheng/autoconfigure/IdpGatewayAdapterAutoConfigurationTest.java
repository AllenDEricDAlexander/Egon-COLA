package top.egon.cola.platform.tianquan.shoubing.yuheng.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import top.egon.cola.component.yuheng.core.security.GatewayAuthenticationProvider;
import top.egon.cola.component.yuheng.core.security.GatewayAuthorizationProvider;
import top.egon.cola.component.yuheng.core.security.GatewayCredentialExtractor;
import top.egon.cola.component.yuheng.core.security.GatewayIdentityMapper;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterAutoConfiguration;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.Clock;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TenantIssuerAuthenticationManagerComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.security.OAuthRevocationReader;
import top.egon.cola.platform.tianquan.shoubing.starter.security.VerifiedPrincipalConverter;
import top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpRefreshClient;
import top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpRefreshTokenStatusClient;
import top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpUserOnlineStateProvider;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdpGatewayAdapterAutoConfigurationTest {

    private final top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent observer = mock(top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent.class);

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    IdpGatewayAdapterAutoConfiguration.class))
            .withBean(ObjectMapper.class,
                    () -> new ObjectMapper().findAndRegisterModules())
            .withBean("tenantIssuerAuthenticationManagerComponent", TenantIssuerAuthenticationManagerComponent.class, () -> {
                var manager = mock(TenantIssuerAuthenticationManagerComponent.class);
                when(manager.isConfigured()).thenReturn(true);
                return manager;
            })
            .withBean("oauthRevocationReader", OAuthRevocationReader.class, () -> verified -> OAuthRevocationReader.Result.valid())
            .withBean("verifiedPrincipalConverter", VerifiedPrincipalConverter.class, () -> {
                try {
                    return (VerifiedPrincipalConverter) Class.forName(
                            "top.egon.cola.platform.tianquan.shoubing.starter.security.VerifiedPrincipalConverterImpl")
                            .getDeclaredConstructor().newInstance();
                } catch (ReflectiveOperationException failure) {
                    throw new AssertionError("Generated shared principal converter is required", failure);
                }
            })
            .withBean("idpObservedVerificationComponent", top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent.class, () -> observer)
            .withBean("idpSecurityMetricsRecorder", top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder.class, () -> sample -> { })
            .withBean("idpClock", Clock.class, Clock::systemUTC)
            .withBean("defaultValidator", jakarta.validation.Validator.class, () -> {
                var validator = new org.springframework.validation.beanvalidation.LocalValidatorFactoryBean();
                validator.afterPropertiesSet(); return validator;
            });

    @Test
    void runsBeforeTheServletStarterCreatesGenericStateReaders() {
        AutoConfiguration configuration =
                IdpGatewayAdapterAutoConfiguration.class.getAnnotation(
                        AutoConfiguration.class
                );

        assertThat(configuration.before())
                .contains(IdpStarterAutoConfiguration.class);
    }

    @Test
    void staysDisabledByDefault() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(GatewayCredentialExtractor.class);
            assertThat(context).doesNotHaveBean(GatewayIdentityMapper.class);
        });
    }

    @Test
    void registersMandatoryScopeSeparatelyFromOrdinaryAuthorization() {
        runner.withPropertyValues(
                        "egon.cola.platform.tianquan.shoubing.yuheng.enabled=true",
                        "egon.cola.platform.tianquan.shoubing.yuheng.issuer=https://tianquan-shoubing.local",
                        "egon.cola.platform.tianquan.shoubing.yuheng.jwk-set-uri=https://tianquan-shoubing.local/oauth2/jwks",
                        "egon.cola.platform.tianquan.shoubing.yuheng.tianquan-shoubing-refresh-uri=https://tianquan-shoubing.local/oauth2/token",
                        "egon.cola.platform.tianquan.shoubing.yuheng.refresh-status-resource-uri=https://api.tianquan-shoubing.local",
                        "egon.cola.platform.tianquan.shoubing.yuheng.runtime.redis-enabled=false")
                .withBean("idpGatewayRedissonClient", RedissonClient.class,
                        () -> mock(RedissonClient.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(GatewayCredentialExtractor.class);
                    assertThat(context).hasSingleBean(GatewayAuthenticationProvider.class);
                    assertThat(context).hasSingleBean(GatewayIdentityMapper.class);
                    assertThat(context).hasSingleBean(GatewayAuthorizationProvider.class);
                    assertThat(context.getBean(GatewayAuthorizationProvider.class).mandatory()).isTrue();
                    assertThat(context.getBean(GatewayAuthenticationProvider.class).requiredAuthorizationProviderIds())
                            .containsExactly(top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpOAuthScopeAuthorizationProvider.PROVIDER_ID);
                    assertThat(context.getBean(GatewayCredentialExtractor.class)
                            .extractorId()).isEqualTo("tianquan-shoubing-user-cookie");
                    assertThat(context.getBean(GatewayAuthenticationProvider.class)
                            .providerId()).isEqualTo("tianquan-shoubing-jwt");
                    assertThat(context.getBean(GatewayIdentityMapper.class)
                            .mapperId()).isEqualTo("tianquan-shoubing-identity");
                });
    }

    @Test
    void doesNotRegisterRetiredRefreshClientsOrRequireTheirUri() {
        runner.withPropertyValues(
                        "egon.cola.platform.tianquan.shoubing.yuheng.enabled=true",
                        "egon.cola.platform.tianquan.shoubing.yuheng.issuer=https://idp.example/platform",
                        "egon.cola.platform.tianquan.shoubing.yuheng.jwk-set-uri=https://idp.example/platform/.well-known/jwks.json",
                        "egon.cola.platform.tianquan.shoubing.yuheng.runtime.redis-enabled=false")
                .withBean("idpGatewayRedissonClient", RedissonClient.class, () -> mock(RedissonClient.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(IdpRefreshClient.class);
                    assertThat(context).doesNotHaveBean(IdpRefreshTokenStatusClient.class);
                    assertThat(context).doesNotHaveBean(IdpUserOnlineStateProvider.class);
                    assertThat(context).hasSingleBean(GatewayAuthenticationProvider.class);
                });
    }

    @Test
    void observerIsInjectedAndOldFactoriesAndProviderConstructorStayAvailable() throws Exception {
        runner.withPropertyValues("egon.cola.platform.tianquan.shoubing.yuheng.enabled=true",
                        "egon.cola.platform.tianquan.shoubing.yuheng.issuer=https://idp.example/platform",
                        "egon.cola.platform.tianquan.shoubing.yuheng.jwk-set-uri=https://idp.example/platform/.well-known/jwks.json",
                        "egon.cola.platform.tianquan.shoubing.yuheng.runtime.redis-enabled=false")
                .withBean("idpGatewayRedissonClient", RedissonClient.class, () -> mock(RedissonClient.class))
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(GatewayAuthenticationProvider.class);
                    var verifier = context.getBean("idpGatewayJwtVerifier", top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpGatewayJwtVerifier.class);
                    try {
                        var field = verifier.getClass().getDeclaredField("observations"); field.setAccessible(true);
                        assertThat(field.get(verifier)).isSameAs(observer);
                    } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
                });
        assertThat(java.util.Arrays.stream(top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpGatewayJwtVerifier.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("createConfigured")).map(java.lang.reflect.Method::getParameterCount))
                .containsExactlyInAnyOrder(10, 11);
        top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpIdentityAuthenticationProvider.class
                .getConstructor(top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpIdentityAuthenticationProvider.TokenVerifier.class);
    }

    @Test
    void strictAuditAndStateBindingsUseOAuthPrimaryRatherThanRbacProjectionRedis() {
        var oauthPrimary = mock(RedissonClient.class);
        var rbacProjection = mock(RedissonClient.class);
        runner.withPropertyValues("egon.cola.platform.tianquan.shoubing.yuheng.enabled=true",
                        "egon.cola.platform.tianquan.shoubing.yuheng.issuer=https://idp.example/tenants/1001",
                        "egon.cola.platform.tianquan.shoubing.yuheng.jwk-set-uri=https://idp.example/tenants/1001/.well-known/jwks.json",
                        "egon.cola.platform.tianquan.shoubing.yuheng.runtime.redis-enabled=false",
                        "egon.cola.platform.tianquan.shoubing.usage-observation.enabled=true")
                .withBean("idpGatewayRedissonClient", RedissonClient.class, () -> oauthPrimary)
                .withBean("rbac3RuntimeRedissonClient", RedissonClient.class, () -> rbacProjection)
                .withBean("idpStarterProperties", top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties.class,
                        top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties::new)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean("idpAuditRedissonClient")).isSameAs(oauthPrimary).isNotSameAs(rbacProjection);
                    assertThat(context.getBean("identityResourceServerStateReader")).isSameAs(context.getBean("idpGatewayResourceServerStateReader"));
                    assertThat(context.getBean("identityOAuthClientStateReader")).isSameAs(context.getBean("idpGatewayOAuthClientStateReader"));
                });
    }

    @Test
    void decoderAcceptsAccessTokenTypeValidatedByIdpVerifier()
            throws Exception {
        RSAKey key = new RSAKeyGenerator(2048)
                .keyID("tianquan-shoubing-local")
                .algorithm(JWSAlgorithm.RS256)
                .generate();
        byte[] jwkSet = ("{\"keys\":["
                + key.toPublicJWK().toJSONString()
                + "]}").getBytes(StandardCharsets.UTF_8);
        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0
        );
        server.createContext("/oauth2/jwks", exchange -> {
            exchange.getResponseHeaders().add(
                    "Content-Type",
                    "application/json"
            );
            exchange.sendResponseHeaders(200, jwkSet.length);
            exchange.getResponseBody().write(jwkSet);
            exchange.close();
        });
        server.start();
        try {
            String issuer = "http://127.0.0.1:"
                    + server.getAddress().getPort();
            IdpGatewayAdapterProperties properties =
                    new IdpGatewayAdapterProperties();
            properties.setEnabled(true);
            properties.setIssuer(issuer);
            properties.setJwkSetUri(issuer + "/oauth2/jwks");
            properties.setTianquanShoubingRefreshUri(issuer + "/oauth2/token");
            properties.setRefreshStatusResourceUri(
                    java.net.URI.create("https://api.tianquan-shoubing.local"));
            JwtDecoder decoder = new IdpGatewayAdapterAutoConfiguration()
                    .idpGatewayJwtDecoder(properties);
            Instant now = Instant.now();
            SignedJWT token = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                            .type(new JOSEObjectType("at+jwt"))
                            .keyID("tianquan-shoubing-local")
                            .build(),
                    JWTClaimsSet.parse(java.util.Map.of("iss", issuer, "sub", "user-1",
                            "aud", List.of("https://api.example/resource"), "iat", now.getEpochSecond(), "nbf", now.getEpochSecond(),
                            "exp", now.plusSeconds(60).getEpochSecond(), "jti", "token-1"))
            );
            token.sign(new RSASSASigner(key));

            assertThat(decoder.decode(token.serialize()).getSubject())
                    .isEqualTo("user-1");
        } finally {
            server.stop(0);
        }
    }
}
