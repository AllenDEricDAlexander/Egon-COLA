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
import top.egon.cola.platform.idp.core.port.IdentityUserStore;
import top.egon.cola.platform.idp.core.port.RefreshTokenStore;
import top.egon.cola.platform.idp.core.port.TenantMembershipPort;
import top.egon.cola.platform.idp.core.token.TokenFacade;

import java.nio.file.Path;
import java.time.Clock;

/**
 * IdP JWT 签名、Refresh Token 存储与 Token 生命周期的 Spring 装配。
 *
 * <p>Spring wiring for IdP JWT signing, refresh-token storage, and token lifecycle.</p>
 */
@Configuration(proxyBeanMethods = false)
public class TokenConfig {

    /**
     * 暴露 IdP 公钥 JWT 验签器。
     *
     * <p>Exposes the IdP public-key JWT decoder.</p>
     *
     * @param tokens RS256 Token 服务；RS256 token service
     * @return JWT 验签器；JWT decoder
     */
    @Bean
    JwtDecoder idpJwtDecoder(Rs256TokenService tokens) {
        return tokens.jwtDecoder();
    }

    /**
     * 创建签名密钥运行态状态提供器。
     *
     * <p>Creates the signing-key runtime-state provider.</p>
     *
     * @param configuredKid 当前密钥标识；current key identifier
     * @return 签名密钥运行态；signing-key runtime
     */
    @Bean
    SigningKeyRuntime signingKeyRuntime(
            @Value("${egon.idp.oauth.signing-key.kid}") String configuredKid
    ) {
        return new ExternalPemSigningKeyRuntime(configuredKid);
    }

    /**
     * 从外部 PEM 文件装载 RS256 Token 服务。
     *
     * <p>Loads the RS256 token service from external PEM files.</p>
     *
     * @param publicKeyFile 公钥文件；public-key file
     * @param privateKeyFile 私钥文件；private-key file
     * @param kid 密钥标识；key identifier
     * @param issuer IdP Issuer；IdP issuer
     * @return RS256 Token 服务；RS256 token service
     */
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

    /**
     * 创建稳定 Refresh Token 的 Redis 元数据存储。
     *
     * <p>Creates the Redis refresh-token store with rotation and replay detection.</p>
     *
     * @param redisson 身份运行态 Redis；identity-runtime Redis
     * @param keyPrefix Redis Key 前缀；Redis key prefix
     * @return Refresh Token 存储；refresh-token store
     */
    @Bean
    RefreshTokenStore refreshTokenStore(
            @Qualifier("rbac3RuntimeRedissonClient") RedissonClient redisson,
            @Value("${egon.idp.oauth.refresh-key-prefix:identity:v1:}")
            String keyPrefix
    ) {
        return new RedisRefreshTokenStore(redisson, keyPrefix);
    }

    /**
     * 创建 USER Token 签发、刷新、撤销和退出门面。
     *
     * <p>Creates the USER token issuance, refresh, revocation, and logout facade.</p>
     *
     * @param signer Token 签名器；token signer
     * @param refreshTokens Refresh Token 存储；refresh-token store
     * @param users 身份用户存储；identity-user store
     * @param memberships 租户成员关系端口；tenant-membership port
     * @param idpClock UTC 业务时钟；UTC business clock
     * @param idGenerator 全局 ID 生成器；global ID generator
     * @return Token 生命周期门面；token-lifecycle facade
     */
    @Bean
    TokenFacade tokenFacade(
            Rs256TokenService signer,
            RefreshTokenStore refreshTokens,
            IdentityUserStore users,
            TenantMembershipPort memberships,
            @Qualifier("idpClock") Clock idpClock,
            LongIdGenerator idGenerator,
            @Value("${egon.idp.oauth.user-audience:platform}") String userAudience
    ) {
        return new TokenFacade(
                signer,
                refreshTokens,
                users,
                memberships,
                idpClock,
                idGenerator::nextId,
                userAudience
        );
    }
}
