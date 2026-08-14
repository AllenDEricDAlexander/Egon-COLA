package top.egon.cola.platform.idp.gateway.autoconfigure;

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
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import top.egon.cola.component.gateway.core.security.GatewayAuthenticationProvider;
import top.egon.cola.component.gateway.core.security.GatewayAuthorizationProvider;
import top.egon.cola.component.gateway.core.security.GatewayCredentialExtractor;
import top.egon.cola.component.gateway.core.security.GatewayIdentityMapper;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class IdpGatewayAdapterAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    IdpGatewayAdapterAutoConfiguration.class))
            .withBean(ObjectMapper.class,
                    () -> new ObjectMapper().findAndRegisterModules());

    @Test
    void staysDisabledByDefault() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(GatewayCredentialExtractor.class);
            assertThat(context).doesNotHaveBean(GatewayIdentityMapper.class);
        });
    }

    @Test
    void registersOnlyIdentityCapabilitiesWhenRedisIsAvailable() {
        runner.withPropertyValues(
                        "egon.cola.platform.idp.gateway.enabled=true",
                        "egon.cola.platform.idp.gateway.issuer=https://idp.local",
                        "egon.cola.platform.idp.gateway.jwk-set-uri=https://idp.local/oauth2/jwks",
                        "egon.cola.platform.idp.gateway.idp-refresh-uri=https://idp.local/oauth2/token",
                        "egon.cola.platform.idp.gateway.runtime.redis-enabled=false")
                .withBean("idpGatewayRedissonClient", RedissonClient.class,
                        () -> mock(RedissonClient.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(GatewayCredentialExtractor.class);
                    assertThat(context).hasSingleBean(GatewayAuthenticationProvider.class);
                    assertThat(context).hasSingleBean(GatewayIdentityMapper.class);
                    assertThat(context).doesNotHaveBean(
                            GatewayAuthorizationProvider.class);
                    assertThat(context.getBean(GatewayCredentialExtractor.class)
                            .extractorId()).isEqualTo("idp-user-cookie");
                    assertThat(context.getBean(GatewayAuthenticationProvider.class)
                            .providerId()).isEqualTo("idp-jwt");
                    assertThat(context.getBean(GatewayIdentityMapper.class)
                            .mapperId()).isEqualTo("idp-identity");
                });
    }

    @Test
    void decoderAcceptsAccessTokenTypeValidatedByIdpVerifier()
            throws Exception {
        RSAKey key = new RSAKeyGenerator(2048)
                .keyID("idp-local")
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
            properties.setIdpRefreshUri(issuer + "/oauth2/token");
            JwtDecoder decoder = new IdpGatewayAdapterAutoConfiguration()
                    .idpGatewayJwtDecoder(properties);
            Instant now = Instant.now();
            SignedJWT token = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                            .type(new JOSEObjectType("at+jwt"))
                            .keyID("idp-local")
                            .build(),
                    new JWTClaimsSet.Builder()
                            .issuer(issuer)
                            .subject("user-1")
                            .audience(List.of("https://api.example/resource"))
                            .issueTime(Date.from(now))
                            .notBeforeTime(Date.from(now))
                            .expirationTime(Date.from(now.plusSeconds(60)))
                            .jwtID("token-1")
                            .build()
            );
            token.sign(new RSASSASigner(key));

            assertThat(decoder.decode(token.serialize()).getSubject())
                    .isEqualTo("user-1");
        } finally {
            server.stop(0);
        }
    }
}
