package top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure;

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
import io.grpc.ServerInterceptor;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import top.egon.cola.component.rpc.consumer.interceptor.RpcClientInterceptorFactory;
import top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceOAuth2Client;
import top.egon.cola.platform.tianquan.shoubing.starter.security.CurrentIdentity;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpBearerAuthenticationFilter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpJwtVerifier;
import top.egon.cola.platform.tianquan.shoubing.starter.security.ServiceScopeAuthorization;
import top.egon.cola.platform.tianquan.shoubing.starter.security.UserAccessTokenVerifier;
import top.egon.cola.platform.tianquan.shoubing.starter.security.rpc.IdpRpcBearerServerInterceptor;
import top.egon.cola.platform.tianquan.shoubing.starter.security.rpc.IdpRpcClientCredentialInterceptorFactory;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityOAuthClientStateReader;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityResourceServerStateReader;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class IdpStarterAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            IdpStarterAutoConfiguration.class))
                    .withBean(ObjectMapper.class, ObjectMapper::new)
                    .withBean(RedissonClient.class,
                            () -> mock(RedissonClient.class))
                    .withPropertyValues(
                            "egon.cola.platform.tianquan.shoubing.enabled=true",
                            "egon.cola.platform.tianquan.shoubing.issuer=https://tianquan-shoubing.local",
                            "egon.cola.platform.tianquan.shoubing.jwk-set-uri=https://tianquan-shoubing.local/oauth2/jwks",
                            "egon.cola.platform.tianquan.shoubing.resource-server-id=resource-tianquan-jianshen-prod",
                            "egon.cola.platform.tianquan.shoubing.resource-uri=https://api.example/prod/permission/tianquan-jianshen");

    @Test
    void providesIdentityOnlyFilterBeforeRbac3Filter() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(
                    IdentityResourceServerStateReader.class);
            assertThat(context).hasSingleBean(
                    IdentityOAuthClientStateReader.class);
            assertThat(context).hasSingleBean(IdpJwtVerifier.class);
            assertThat(context).hasSingleBean(CurrentIdentity.class);
            assertThat(context).hasSingleBean(IdpBearerAuthenticationFilter.class);
            assertThat(context).hasSingleBean(ServiceScopeAuthorization.class);
            assertThat(context).hasSingleBean(
                    IdpRpcClientCredentialInterceptorFactory.class
            );
            assertThat(context).hasSingleBean(
                    IdpRpcBearerServerInterceptor.class
            );
            assertThat(context.getBeansOfType(
                    RpcClientInterceptorFactory.class
            ).values()).anyMatch(
                    IdpRpcClientCredentialInterceptorFactory.class::isInstance
            );
            assertThat(context.getBeansOfType(
                    ServerInterceptor.class
            ).values()).anyMatch(
                    IdpRpcBearerServerInterceptor.class::isInstance
            );
            FilterRegistrationBean<?> registration = context.getBean(
                    "idpBearerFilterRegistration",
                    FilterRegistrationBean.class);
            assertThat(registration.getOrder()).isEqualTo(-102);
        });
    }

    @Test
    void honorsRpcSecurityAdapterOverrides() {
        IdpRpcClientCredentialInterceptorFactory client =
                new IdpRpcClientCredentialInterceptorFactory();
        IdpRpcBearerServerInterceptor server =
                new IdpRpcBearerServerInterceptor(
                        mock(UserAccessTokenVerifier.class)
                );

        contextRunner
                .withBean(
                        IdpRpcClientCredentialInterceptorFactory.class,
                        () -> client
                )
                .withBean(
                        IdpRpcBearerServerInterceptor.class,
                        () -> server
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(
                            IdpRpcClientCredentialInterceptorFactory.class
                    );
                    assertThat(context).hasSingleBean(
                            IdpRpcBearerServerInterceptor.class
                    );
                    assertThat(context.getBean(
                            IdpRpcClientCredentialInterceptorFactory.class
                    )).isSameAs(client);
                    assertThat(context.getBean(
                            IdpRpcBearerServerInterceptor.class
                    )).isSameAs(server);
                });
    }

    @Test
    void remainsDisabledByDefault() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        IdpStarterAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(IdpJwtVerifier.class);
                    assertThat(context).doesNotHaveBean(
                            IdpRpcClientCredentialInterceptorFactory.class
                    );
                    assertThat(context).doesNotHaveBean(
                            IdpRpcBearerServerInterceptor.class
                    );
                });
    }

    @Test
    void doesNotRequireServiceClientWhenOnlyResourceServerIsEnabled() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        IdpStarterAutoConfiguration.class))
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withBean(RedissonClient.class,
                        () -> mock(RedissonClient.class))
                .withPropertyValues(
                        "egon.cola.platform.tianquan.shoubing.enabled=true",
                        "egon.cola.platform.tianquan.shoubing.issuer=https://tianquan-shoubing.local",
                        "egon.cola.platform.tianquan.shoubing.jwk-set-uri=https://tianquan-shoubing.local/oauth2/jwks",
                        "egon.cola.platform.tianquan.shoubing.resource-server-id=resource-tianshu-local",
                        "egon.cola.platform.tianquan.shoubing.resource-uri=https://api.example/local/platform/tianshu",
                        "egon.cola.component.tianshu.enabled=false",
                        "egon.cola.component.tianshu.registry.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(IdpJwtVerifier.class);
                    assertThat(context).doesNotHaveBean(
                            IdpServiceOAuth2Client.class);
                });
    }

    @Test
    void providesServiceClientWhenRegistrationIsConfigured() {
        ClientRegistration registration = ClientRegistration
                .withRegistrationId("egon-tianquan-shoubing")
                .clientId("orders-key")
                .clientSecret("orders-secret")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri("https://tianquan-shoubing.local/oauth2/token")
                .build();
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        IdpStarterAutoConfiguration.class))
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withBean(RedissonClient.class,
                        () -> mock(RedissonClient.class))
                .withBean(ClientRegistrationRepository.class,
                        () -> new InMemoryClientRegistrationRepository(
                                registration))
                .withPropertyValues(
                        "egon.cola.platform.tianquan.shoubing.enabled=true",
                        "egon.cola.platform.tianquan.shoubing.issuer=https://tianquan-shoubing.local",
                        "egon.cola.platform.tianquan.shoubing.jwk-set-uri=https://tianquan-shoubing.local/oauth2/jwks",
                        "egon.cola.platform.tianquan.shoubing.resource-server-id=rs-tianquan-shoubing-prod",
                        "egon.cola.platform.tianquan.shoubing.resource-uri=https://api.example/tianquan-shoubing",
                        "egon.cola.platform.tianquan.shoubing.service-client.app-id=orders-app",
                        "egon.cola.platform.tianquan.shoubing.service-client.registration-id=egon-tianquan-shoubing")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(
                            OAuth2AuthorizedClientManager.class);
                    assertThat(context).hasSingleBean(IdpServiceOAuth2Client.class);
                });
    }

    @Test
    void rejectsLegacyPrivateKeyAndAdmissionProperties() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        IdpStarterAutoConfiguration.class))
                .withPropertyValues(
                        "egon.cola.platform.tianquan.shoubing.enabled=true",
                        "egon.cola.platform.tianquan.shoubing.admission.private-key-path=/run/secrets/tianquan-shoubing.pem")
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .hasMessageContaining("migration")
                        .hasMessageContaining("admission.private-key-path")
                        .hasMessageNotContaining("/run/secrets/tianquan-shoubing.pem"));
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
            String issuer = "http://127.0.0.1:" + server.getAddress().getPort();
            String resource = "https://api.example/local/permission/tianquan-jianshen";
            IdpStarterProperties properties = new IdpStarterProperties();
            properties.setEnabled(true);
            properties.setIssuer(issuer);
            properties.setJwkSetUri(issuer + "/oauth2/jwks");
            properties.setResourceServerId("permission-tianquan-jianshen-local");
            properties.setResourceUri(URI.create(resource));
            JwtDecoder decoder = new IdpStarterAutoConfiguration()
                    .idpJwtDecoder(properties);
            Instant now = Instant.now();
            SignedJWT token = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                            .type(new JOSEObjectType("at+jwt"))
                            .keyID("tianquan-shoubing-local")
                            .build(),
                    new JWTClaimsSet.Builder()
                            .issuer(issuer)
                            .subject("tianquan-shoubing-service")
                            .audience(List.of(resource))
                            .issueTime(Date.from(now))
                            .notBeforeTime(Date.from(now))
                            .expirationTime(Date.from(now.plusSeconds(60)))
                            .jwtID("token-1")
                            .build()
            );
            token.sign(new RSASSASigner(key));

            assertThat(decoder.decode(token.serialize()).getSubject())
                    .isEqualTo("tianquan-shoubing-service");
        } finally {
            server.stop(0);
        }
    }

}
