package top.egon.cola.platform.rbac3.admin.security;

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import top.egon.cola.platform.rbac3.admin.auth.application.JwtKeyRingService;
import top.egon.cola.platform.rbac3.admin.config.Rbac3SecurityProperties;
import top.egon.cola.platform.rbac3.admin.snapshot.infrastructure.RedisAuthorizationRuntimeStore;

import java.io.IOException;
import java.io.InputStream;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Loads deployment-owned RSA material and enforces issuer, Resource URI and RS256.
 */
@Configuration(proxyBeanMethods = false)
public class Rbac3JwtConfiguration {

    @Bean
    Rbac3RsaKeyMaterial rbac3RsaKeyMaterial(Rbac3SecurityProperties properties) {
        try (InputStream privateInput = new FileSystemResource(
                properties.requirePrivateKeyFile()).getInputStream();
             InputStream publicInput = new FileSystemResource(
                     properties.requirePublicKeyFile()).getInputStream()) {
            return new Rbac3RsaKeyMaterial(
                    (RSAPublicKey) RsaKeyConverters.x509().convert(publicInput),
                    (RSAPrivateKey) RsaKeyConverters.pkcs8().convert(privateInput),
                    properties.requireKid());
        } catch (IOException error) {
            throw new IllegalStateException("cannot read RBAC3 RSA key material", error);
        }
    }

    @Bean
    JwtEncoder jwtEncoder(Rbac3RsaKeyMaterial material) {
        RSAKey key = material.rsaKey();
        JWKSource<SecurityContext> source = new ImmutableJWKSet<>(
                new com.nimbusds.jose.jwk.JWKSet(key));
        return new NimbusJwtEncoder(source);
    }

    @Bean
    JwtDecoder jwtDecoder(
            Rbac3RsaKeyMaterial material,
            Rbac3SecurityProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(material.publicKey())
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        OAuth2TokenValidator<Jwt> resource = token -> token.getAudience().stream()
                .anyMatch(properties.requireResourceUris()::contains)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                        "invalid_token", "JWT audience is not accepted", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.requireIssuer()),
                resource));
        return decoder;
    }

    @Bean
    JwtKeyRingService jwtKeyRingService(
            Rbac3RsaKeyMaterial material,
            Rbac3SecurityProperties properties,
            Clock clock) {
        Map<String, Object> publicJwk = material.rsaKey().toPublicJWK().toJSONObject();
        return new JwtKeyRingService(List.of(new JwtKeyRingService.KeyDescriptor(
                material.kid(), "RS256", publicJwk,
                JwtKeyRingService.KeyState.SIGNING, clock.instant(), null)),
                properties.requireVerificationKeyRetention());
    }

    @Bean
    Rbac3JwtAuthenticationConverter rbac3JwtAuthenticationConverter(
            RedisAuthorizationRuntimeStore runtimeStore) {
        return new Rbac3JwtAuthenticationConverter(runtimeStore);
    }

    public record Rbac3RsaKeyMaterial(
            RSAPublicKey publicKey,
            RSAPrivateKey privateKey,
            String kid) {

        RSAKey rsaKey() {
            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(kid)
                    .algorithm(com.nimbusds.jose.JWSAlgorithm.RS256)
                    .build();
        }
    }
}
