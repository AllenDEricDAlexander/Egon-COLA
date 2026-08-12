package top.egon.cola.platform.idp.starter.autoconfigure;

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
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import top.egon.cola.component.ddc.api.extension.DdcAdmissionTicketSupplier;
import top.egon.cola.platform.idp.starter.security.IdpBearerAuthenticationFilter;
import top.egon.cola.platform.idp.starter.admission.RpcResourceServerAdmissionClient;
import top.egon.cola.platform.idp.starter.security.IdpJwtVerifier;
import top.egon.cola.platform.idp.starter.security.ServiceScopeAuthorization;
import top.egon.cola.platform.idp.starter.state.IdentityOAuthClientStateReader;
import top.egon.cola.platform.idp.starter.state.IdentityResourceServerStateReader;
import top.egon.cola.platform.idp.starter.state.IdentityUserStateReader;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;

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
                    .withBean(DdcAdmissionTicketSupplier.class,
                            () -> (biz, app, env, instance) -> {
                                throw new IllegalStateException("not invoked");
                            })
                    .withPropertyValues(
                            "egon.cola.platform.idp.enabled=true",
                            "egon.cola.platform.idp.issuer=https://idp.local",
                            "egon.cola.platform.idp.jwk-set-uri=https://idp.local/oauth2/jwks",
                            "egon.cola.platform.idp.resource-server-id=resource-rbac3-prod",
                            "egon.cola.platform.idp.resource-uri=https://api.example/prod/permission/rbac3");

    @Test
    void providesIdentityOnlyFilterBeforeRbac3Filter() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(IdentityUserStateReader.class);
            assertThat(context).hasSingleBean(
                    IdentityResourceServerStateReader.class);
            assertThat(context).hasSingleBean(
                    IdentityOAuthClientStateReader.class);
            assertThat(context).hasSingleBean(IdpJwtVerifier.class);
            assertThat(context).hasSingleBean(IdpBearerAuthenticationFilter.class);
            assertThat(context).hasSingleBean(ServiceScopeAuthorization.class);
            FilterRegistrationBean<?> registration = context.getBean(
                    "idpBearerFilterRegistration",
                    FilterRegistrationBean.class);
            assertThat(registration.getOrder()).isEqualTo(-102);
        });
    }

    @Test
    void remainsDisabledByDefault() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        IdpStarterAutoConfiguration.class))
                .run(context -> assertThat(context)
                        .doesNotHaveBean(IdpJwtVerifier.class));
    }

    @Test
    void doesNotRequireAdmissionCredentialsWhenDdcConsumersAreDisabled() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        IdpStarterAutoConfiguration.class))
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withBean(RedissonClient.class,
                        () -> mock(RedissonClient.class))
                .withPropertyValues(
                        "egon.cola.platform.idp.enabled=true",
                        "egon.cola.platform.idp.issuer=https://idp.local",
                        "egon.cola.platform.idp.jwk-set-uri=https://idp.local/oauth2/jwks",
                        "egon.cola.platform.idp.resource-server-id=resource-ddc-local",
                        "egon.cola.platform.idp.resource-uri=https://api.example/local/platform/ddc",
                        "egon.cola.component.ddc.enabled=false",
                        "egon.cola.component.ddc.registry.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(IdpJwtVerifier.class);
                    assertThat(context).doesNotHaveBean(
                            DdcAdmissionTicketSupplier.class);
                });
    }

    @Test
    void providesAdmissionTicketSupplierWhenAllMachineIdentityIsConfigured()
            throws Exception {
        Path privateKey = ownerOnlyPrivateKey();

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        IdpStarterAutoConfiguration.class))
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withBean(RedissonClient.class,
                        () -> mock(RedissonClient.class))
                .withPropertyValues(
                        "egon.cola.platform.idp.enabled=true",
                        "egon.cola.platform.idp.issuer=https://idp.local",
                        "egon.cola.platform.idp.jwk-set-uri=https://idp.local/oauth2/jwks",
                        "egon.cola.platform.idp.resource-server-id=rs-idp-prod",
                        "egon.cola.platform.idp.resource-uri=https://api.example/idp",
                        "egon.cola.component.ddc.enabled=true",
                        "egon.cola.platform.idp.admission.biz-code=platform",
                        "egon.cola.platform.idp.admission.app-code=idp",
                        "egon.cola.platform.idp.admission.environment=prod",
                        "egon.cola.platform.idp.admission.instance-id=idp-local-8080",
                        "egon.cola.platform.idp.admission.management-client-id=idp-service",
                        "egon.cola.platform.idp.admission.kid=idp-service-2026-08",
                        "egon.cola.platform.idp.admission.private-key-path="
                                + privateKey,
                        "egon.cola.platform.idp.admission.rpc-target=dns:///127.0.0.1:18122",
                        "egon.cola.platform.idp.admission.rpc-timeout=3s",
                        "egon.cola.component.rpc.tls.development-plaintext=true",
                        "egon.cola.platform.idp.admission.renewal-skew=30s")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(
                            RpcResourceServerAdmissionClient.class
                    );
                    assertThat(context).hasSingleBean(
                            DdcAdmissionTicketSupplier.class
                    );
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
            String issuer = "http://127.0.0.1:" + server.getAddress().getPort();
            String resource = "https://api.example/local/permission/rbac3";
            IdpStarterProperties properties = new IdpStarterProperties();
            properties.setEnabled(true);
            properties.setIssuer(issuer);
            properties.setJwkSetUri(issuer + "/oauth2/jwks");
            properties.setResourceServerId("permission-rbac3-local");
            properties.setResourceUri(URI.create(resource));
            JwtDecoder decoder = new IdpStarterAutoConfiguration()
                    .idpJwtDecoder(properties);
            Instant now = Instant.now();
            SignedJWT token = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                            .type(new JOSEObjectType("at+jwt"))
                            .keyID("idp-local")
                            .build(),
                    new JWTClaimsSet.Builder()
                            .issuer(issuer)
                            .subject("idp-service")
                            .audience(List.of(resource))
                            .issueTime(Date.from(now))
                            .notBeforeTime(Date.from(now))
                            .expirationTime(Date.from(now.plusSeconds(60)))
                            .jwtID("token-1")
                            .build()
            );
            token.sign(new RSASSASigner(key));

            assertThat(decoder.decode(token.serialize()).getSubject())
                    .isEqualTo("idp-service");
        } finally {
            server.stop(0);
        }
    }

    private static Path ownerOnlyPrivateKey() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        byte[] privateKey = generator.generateKeyPair()
                .getPrivate().getEncoded();
        String encoded = Base64.getMimeEncoder(64, new byte[]{'\n'})
                .encodeToString(privateKey);
        Path path = Files.createTempFile(
                "idp-admission-private-",
                ".pem"
        );
        Files.writeString(
                path,
                "-----BEGIN PRIVATE KEY-----\n"
                        + encoded
                        + "\n-----END PRIVATE KEY-----\n",
                StandardCharsets.US_ASCII
        );
        Files.setPosixFilePermissions(path, Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE
        ));
        path.toFile().deleteOnExit();
        return path;
    }
}
