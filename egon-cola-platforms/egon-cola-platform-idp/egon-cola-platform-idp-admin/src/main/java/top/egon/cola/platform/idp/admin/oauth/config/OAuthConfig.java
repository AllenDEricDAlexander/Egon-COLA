package top.egon.cola.platform.idp.admin.oauth.config;

import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.idp.admin.oauth.repo.IdentityClientRedirectUriRepository;
import top.egon.cola.platform.idp.admin.oauth.repo.IdentityClientRepository;
import top.egon.cola.platform.idp.admin.oauth.repo.JpaOAuthClientStore;
import top.egon.cola.platform.idp.admin.oauth.repo.RedisClientAssertionReplayStore;
import top.egon.cola.platform.idp.admin.oauth.service.impl.PrivateKeyJwtAuthenticator;
import top.egon.cola.platform.idp.admin.resource.repo.IdentityClientJwkRepository;
import top.egon.cola.platform.idp.admin.resource.repo.JpaClientCredentialStore;
import top.egon.cola.platform.idp.admin.support.oauth.LocalServiceAccessTokenSupplier;
import top.egon.cola.platform.idp.admin.support.rbac3.HttpTenantMembershipAdapter;
import top.egon.cola.platform.idp.admin.token.service.impl.ClientCredentialsTokenService;
import top.egon.cola.platform.idp.admin.token.service.impl.Rs256TokenService;
import top.egon.cola.platform.idp.core.port.ClientAssertionReplayStore;
import top.egon.cola.platform.idp.core.port.ClientCredentialStore;
import top.egon.cola.platform.idp.core.port.OAuthClientStore;
import top.egon.cola.platform.idp.core.port.ResourceServerStore;
import top.egon.cola.platform.idp.core.port.TenantMembershipPort;
import top.egon.cola.platform.idp.core.resource.ClientCredentialsAccessPolicy;

import java.net.URI;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * OAuth Client、SERVICE token and USER membership wiring.
 *
 * <p>Authorization-code and server-side SSO wiring is intentionally absent.</p>
 */
@Configuration(proxyBeanMethods = false)
public class OAuthConfig {

    /**
     * 提供可替换的 UTC 业务时钟。
     *
     * <p>Provides an overridable UTC business clock.</p>
     *
     * @return UTC 时钟；UTC clock
     */
    @Bean
    @ConditionalOnMissingBean
    Clock idpClock() {
        return Clock.systemUTC();
    }

    /**
     * 提供密码学安全随机源。
     *
     * <p>Provides a cryptographically secure random source.</p>
     *
     * @return 安全随机源；secure random source
     */
    @Bean
    @ConditionalOnMissingBean
    SecureRandom idpSecureRandom() {
        return new SecureRandom();
    }

    /**
     * 创建 OAuth Client 查询端口。
     *
     * <p>Creates the OAuth Client lookup port.</p>
     *
     * @param clients Client 仓储；Client repository
     * @param redirects 回调地址仓储；redirect-URI repository
     * @return OAuth Client 查询端口；OAuth Client lookup port
     */
    @Bean
    OAuthClientStore oauthClientStore(
            IdentityClientRepository clients,
            IdentityClientRedirectUriRepository redirects
    ) {
        return new JpaOAuthClientStore(clients, redirects);
    }

    /**
     * 创建 OAuth Client 公开 JWK 查询端口。
     *
     * <p>Creates the OAuth Client public-JWK lookup port.</p>
     *
     * @param credentials Client JWK 仓储；Client JWK repository
     * @return Client 公开凭证查询端口；Client public-credential lookup port
     */
    @Bean
    ClientCredentialStore clientCredentialStore(
            IdentityClientJwkRepository credentials
    ) {
        return new JpaClientCredentialStore(credentials);
    }

    /**
     * 创建 Client Assertion Redis 防重放端口。
     *
     * <p>Creates the Redis Client Assertion replay-prevention port.</p>
     *
     * @param redisson 身份运行态 Redis；identity-runtime Redis
     * @param idpClock UTC 业务时钟；UTC business clock
     * @param keyPrefix Redis Key 前缀；Redis key prefix
     * @return Assertion 防重放端口；assertion replay-prevention port
     */
    @Bean
    ClientAssertionReplayStore clientAssertionReplayStore(
            @Qualifier("rbac3RuntimeRedissonClient") RedissonClient redisson,
            @Qualifier("idpClock") Clock idpClock,
            @Value("${egon.idp.oauth.client-assertion-key-prefix:"
                    + "identity:v1:client-assertion-replay:}")
            String keyPrefix
    ) {
        return new RedisClientAssertionReplayStore(
                redisson,
                keyPrefix,
                idpClock
        );
    }

    /**
     * 创建仅绑定 Token Endpoint 的 {@code private_key_jwt} 认证器。
     *
     * <p>Creates a {@code private_key_jwt} authenticator bound only to the Token Endpoint.</p>
     *
     * @param clients OAuth Client 查询端口；OAuth Client lookup port
     * @param credentials Client JWK 查询端口；Client JWK lookup port
     * @param replays Assertion 防重放端口；assertion replay-prevention port
     * @param issuer IdP Issuer；IdP issuer
     * @param idpClock UTC 业务时钟；UTC business clock
     * @return Token Endpoint Client 认证器；Token Endpoint Client authenticator
     */
    @Bean
    @Primary
    PrivateKeyJwtAuthenticator privateKeyJwtAuthenticator(
            OAuthClientStore clients,
            ClientCredentialStore credentials,
            ClientAssertionReplayStore replays,
            @Value("${egon.idp.oauth.issuer}") String issuer,
            @Qualifier("idpClock") Clock idpClock
    ) {
        return new PrivateKeyJwtAuthenticator(
                clients,
                credentials,
                replays,
                endpoint(issuer),
                idpClock
        );
    }

    /**
     * 创建 IdP 自有的 Client Credentials 授权策略。
     *
     * <p>Creates the IdP-owned Client Credentials authorization policy.</p>
     *
     * @param resources Resource 与 Grant 查询端口；Resource and Grant lookup port
     * @return Client Credentials 授权策略；Client Credentials authorization policy
     */
    @Bean
    ClientCredentialsAccessPolicy clientCredentialsAccessPolicy(
            ResourceServerStore resources
    ) {
        return new ClientCredentialsAccessPolicy(resources);
    }

    /**
     * 创建 SERVICE Token 签发服务。
     *
     * <p>Creates the SERVICE token issuance service.</p>
     *
     * @param clients OAuth Client 查询端口；OAuth Client lookup port
     * @param resources Resource 与 Grant 查询端口；Resource and Grant lookup port
     * @param accessPolicy IdP Service Grant 策略；IdP Service Grant policy
     * @param signer RS256 Token 服务；RS256 token service
     * @param idpClock UTC 业务时钟；UTC business clock
     * @param ids 全局 ID 生成器；global ID generator
     * @return SERVICE Token 签发服务；SERVICE token issuance service
     */
    @Bean
    ClientCredentialsTokenService clientCredentialsTokenService(
            OAuthClientStore clients,
            ResourceServerStore resources,
            ClientCredentialsAccessPolicy accessPolicy,
            Rs256TokenService signer,
            @Qualifier("idpClock") Clock idpClock,
            LongIdGenerator ids
    ) {
        return new ClientCredentialsTokenService(
                clients,
                resources,
                accessPolicy,
                signer,
                idpClock,
                ids::nextId
        );
    }

    /**
     * 创建 IdP 内部调用 RBAC3 的短期 SERVICE Token 提供器。
     *
     * <p>Creates the short-lived SERVICE token supplier used by IdP to call RBAC3.</p>
     *
     * @param authenticator {@code private_key_jwt} 认证器；authenticator
     * @param tokens SERVICE Token 签发服务；SERVICE token issuance service
     * @param clientId IdP Source Client；IdP Source Client
     * @param keyId IdP Client JWK kid；IdP Client JWK kid
     * @param privateKeyFile owner-only 私钥绝对路径；absolute owner-only private-key path
     * @param issuer IdP Issuer；IdP issuer
     * @param resourceUri RBAC3 Resource URI；RBAC3 Resource URI
     * @param tenantId 内部调用的精确租户；exact tenant for internal calls
     * @param scopeText IdP 授权的 Service Scope；IdP-authorized Service scopes
     * @param ttlSeconds Token 有效秒数；token lifetime in seconds
     * @param renewalSkewSeconds 提前续签秒数；renewal skew in seconds
     * @param idpClock UTC 业务时钟；UTC business clock
     * @param idpSecureRandom 密码学随机源；cryptographic random source
     * @return SERVICE Token Header 提供器；SERVICE token header supplier
     */
    @Bean
    LocalServiceAccessTokenSupplier localServiceAccessTokenSupplier(
            PrivateKeyJwtAuthenticator authenticator,
            ClientCredentialsTokenService tokens,
            @Value("${egon.idp.rbac3.service-token.client-id}")
            String clientId,
            @Value("${egon.idp.rbac3.service-token.key-id}") String keyId,
            @Value("${egon.idp.rbac3.service-token.private-key-file}")
            String privateKeyFile,
            @Value("${egon.idp.oauth.issuer}") String issuer,
            @Value("${egon.idp.rbac3.service-token.resource-uri}")
            String resourceUri,
            @Value("${egon.idp.rbac3.service-token.tenant-id}")
            String tenantId,
            @Value("${egon.idp.rbac3.service-token.scopes}")
            String scopeText,
            @Value("${egon.idp.rbac3.service-token.ttl-seconds:300}")
            long ttlSeconds,
            @Value("${egon.idp.rbac3.service-token.renewal-skew-seconds:30}")
            long renewalSkewSeconds,
            @Qualifier("idpClock") Clock idpClock,
            SecureRandom idpSecureRandom
    ) {
        return new LocalServiceAccessTokenSupplier(
                authenticator,
                tokens,
                clientId,
                keyId,
                Path.of(privateKeyFile),
                endpoint(issuer),
                URI.create(resourceUri),
                tenantId,
                scopes(scopeText),
                Duration.ofSeconds(ttlSeconds),
                Duration.ofSeconds(renewalSkewSeconds),
                idpClock,
                idpSecureRandom
        );
    }

    /**
     * 创建 RBAC3 租户成员关系适配器。
     *
     * <p>Creates the RBAC3 tenant-membership adapter.</p>
     *
     * @param restClientBuilder HTTP 客户端构建器；HTTP client builder
     * @param baseUrl RBAC3 基础地址；RBAC3 base URL
     * @param authorizationHeader IdP 服务身份请求头来源；IdP service header source
     * @return 租户成员关系端口；tenant-membership port
     */
    @Bean
    TenantMembershipPort tenantMembershipPort(
            RestClient.Builder restClientBuilder,
            @Value("${egon.idp.rbac3.base-url}") String baseUrl,
            LocalServiceAccessTokenSupplier authorizationHeader
    ) {
        return new HttpTenantMembershipAdapter(
                restClientBuilder.build(),
                baseUrl,
                authorizationHeader::get,
                authorizationHeader
        );
    }

    /**
     * 从 Issuer 构建精确 Token Endpoint。
     *
     * <p>Builds the exact Token Endpoint from the issuer.</p>
     *
     * @param issuer IdP Issuer；IdP issuer
     * @return Token Endpoint；Token Endpoint
     */
    private static URI endpoint(String issuer) {
        String value = issuer.endsWith("/")
                ? issuer.substring(0, issuer.length() - 1)
                : issuer;
        return URI.create(value + "/oauth2/token");
    }

    /**
     * 解析空格分隔且不重复的 Service Scope。
     *
     * <p>Parses distinct, space-delimited Service scopes.</p>
     *
     * @param value Scope 文本；scope text
     * @return 排序后的不可变 Scope；sorted immutable scopes
     */
    private static Set<String> scopes(String value) {
        if (value == null || value.isBlank()
                || !value.equals(value.trim())
                || value.contains("  ")) {
            throw new IllegalArgumentException("service-token scopes invalid");
        }
        String[] values = value.split(" ");
        TreeSet<String> result = new TreeSet<>(Arrays.asList(values));
        if (result.size() != values.length) {
            throw new IllegalArgumentException(
                    "service-token scopes contain duplicates"
            );
        }
        return Collections.unmodifiableSet(result);
    }
}
