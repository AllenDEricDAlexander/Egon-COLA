package top.egon.cola.platform.idp.admin.token.config;

import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.idp.admin.token.repo.RedisRefreshTokenStore;
import top.egon.cola.platform.idp.admin.token.service.SigningKeyRuntime;
import top.egon.cola.platform.idp.admin.token.service.impl.ExternalPemSigningKeyRuntime;
import top.egon.cola.platform.idp.admin.token.service.impl.Rs256TokenService;
import top.egon.cola.platform.idp.admin.token.service.impl.RsaPemKeyLoader;
import top.egon.cola.platform.idp.core.audit.IdentitySecurityEventPort;
import top.egon.cola.platform.idp.core.port.IdentityUserStatePort;
import top.egon.cola.platform.idp.core.port.IdentityUserStore;
import top.egon.cola.platform.idp.core.port.RefreshTokenStore;
import top.egon.cola.platform.idp.core.token.TokenFacade;

import java.nio.file.Path;
import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class TokenConfig {

    @Bean
    JwtDecoder idpJwtDecoder(Rs256TokenService tokens) {
        return tokens.jwtDecoder();
    }

    @Bean
    SigningKeyRuntime signingKeyRuntime(
            @Value("${egon.idp.oauth.signing-key.kid}") String configuredKid
    ) {
        return new ExternalPemSigningKeyRuntime(configuredKid);
    }

    @Bean
    Rs256TokenService rs256TokenService(
            @Value("${egon.idp.oauth.signing-key.public-key-file}")
            String publicKeyFile,
            @Value("${egon.idp.oauth.signing-key.private-key-file}")
            String privateKeyFile,
            @Value("${egon.idp.oauth.signing-key.kid}") String kid,
            @Value("${egon.idp.oauth.issuer}") String issuer
    ) {
        RsaPemKeyLoader.KeyMaterial keyMaterial = new RsaPemKeyLoader().load(
                Path.of(publicKeyFile),
                Path.of(privateKeyFile)
        );
        return new Rs256TokenService(
                keyMaterial.publicKey(),
                keyMaterial.privateKey(),
                kid,
                issuer
        );
    }

    @Bean
    RefreshTokenStore refreshTokenStore(
            @Qualifier("rbac3RuntimeRedissonClient") RedissonClient redisson,
            @Value("${egon.idp.oauth.refresh-key-prefix:identity:v1:}")
            String keyPrefix
    ) {
        return new RedisRefreshTokenStore(redisson, keyPrefix);
    }

    @Bean
    TokenFacade tokenFacade(
            Rs256TokenService signer,
            RefreshTokenStore refreshTokens,
            IdentityUserStore users,
            IdentityUserStatePort userStates,
            IdentitySecurityEventPort securityEvents,
            @Qualifier("idpClock") Clock idpClock,
            LongIdGenerator idGenerator
    ) {
        return new TokenFacade(
                signer,
                refreshTokens,
                users,
                userStates,
                securityEvents,
                idpClock,
                idGenerator::nextId
        );
    }
}
