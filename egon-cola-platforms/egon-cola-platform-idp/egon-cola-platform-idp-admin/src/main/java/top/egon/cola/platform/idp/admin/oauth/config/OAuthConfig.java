package top.egon.cola.platform.idp.admin.oauth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import top.egon.cola.platform.idp.admin.oauth.repo.IdentityClientRedirectUriRepository;
import top.egon.cola.platform.idp.admin.oauth.repo.IdentityClientRepository;
import top.egon.cola.platform.idp.admin.oauth.repo.IdpSsoSessionStore;
import top.egon.cola.platform.idp.admin.oauth.repo.JpaOAuthClientStore;
import top.egon.cola.platform.idp.admin.oauth.repo.RedisAuthorizationCodeStore;
import top.egon.cola.platform.idp.admin.support.ddc.IdpRuntimePolicy;
import top.egon.cola.platform.idp.admin.support.rbac3.FileServiceAuthorizationSupplier;
import top.egon.cola.platform.idp.admin.support.rbac3.HttpTenantMembershipAdapter;
import top.egon.cola.platform.idp.admin.support.rbac3.HttpUserResourceAccessAuthorizationAdapter;
import top.egon.cola.platform.idp.admin.support.security.IdpAuthorizationAuthenticationEntryPoint;
import top.egon.cola.platform.idp.admin.support.security.IdpSsoAuthenticationFilter;
import top.egon.cola.platform.idp.core.oauth.AuthorizationFacade;
import top.egon.cola.platform.idp.core.port.AuthorizationCodeStore;
import top.egon.cola.platform.idp.core.port.OAuthClientStore;
import top.egon.cola.platform.idp.core.port.ResourceServerStore;
import top.egon.cola.platform.idp.core.port.TenantMembershipPort;
import top.egon.cola.platform.idp.core.port.UserResourceAccessAuthorizationPort;
import top.egon.cola.platform.idp.core.resource.UserResourceAccessPolicy;

import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Clock;

/**
 * OAuth 浏览器授权、授权码存储和 USER Resource 入口策略的 Spring 装配。
 *
 * <p>Spring wiring for OAuth browser authorization, authorization-code storage, and the USER
 * Resource entry policy.</p>
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
     * 创建 SSO 会话 Redis 存储。
     *
     * <p>Creates the Redis SSO-session store.</p>
     *
     * @param redisson 身份运行态 Redis；identity-runtime Redis
     * @param idpSecureRandom 安全随机源；secure random source
     * @param keyPrefix Redis Key 前缀；Redis key prefix
     * @return SSO 会话存储；SSO-session store
     */
    @Bean
    IdpSsoSessionStore idpSsoSessionStore(
            @Qualifier("rbac3RuntimeRedissonClient") RedissonClient redisson,
            SecureRandom idpSecureRandom,
            @Value("${egon.idp.oauth.sso-session-key-prefix:"
                    + "identity:v1:sso-session:}") String keyPrefix
    ) {
        return new IdpSsoSessionStore(redisson, idpSecureRandom, keyPrefix);
    }

    /**
     * 创建浏览器 SSO 身份恢复过滤器。
     *
     * <p>Creates the browser SSO identity-restoration filter.</p>
     *
     * @param sessions SSO 会话存储；SSO-session store
     * @return SSO 身份过滤器；SSO identity filter
     */
    @Bean
    IdpSsoAuthenticationFilter idpSsoAuthenticationFilter(
            IdpSsoSessionStore sessions
    ) {
        return new IdpSsoAuthenticationFilter(sessions);
    }

    /**
     * 创建未登录授权请求的入口处理器。
     *
     * <p>Creates the entry point for unauthenticated authorization requests.</p>
     *
     * @param issuer IdP Issuer；IdP issuer
     * @param loginUri 登录页面地址；login-page URI
     * @return OAuth 登录入口处理器；OAuth login entry point
     */
    @Bean
    IdpAuthorizationAuthenticationEntryPoint
            idpAuthorizationAuthenticationEntryPoint(
            @Value("${egon.idp.oauth.issuer}") String issuer,
            @Value("${egon.idp.oauth.login-uri}") String loginUri
    ) {
        return new IdpAuthorizationAuthenticationEntryPoint(issuer, loginUri);
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
     * 创建一次性授权码 Redis 存储。
     *
     * <p>Creates the one-time authorization-code Redis store.</p>
     *
     * @param redisson 身份运行态 Redis；identity-runtime Redis
     * @param objectMapper JSON 编解码器；JSON codec
     * @param keyPrefix Redis Key 前缀；Redis key prefix
     * @return 授权码存储；authorization-code store
     */
    @Bean
    AuthorizationCodeStore authorizationCodeStore(
            @Qualifier("rbac3RuntimeRedissonClient") RedissonClient redisson,
            ObjectMapper objectMapper,
            @Value("${egon.idp.oauth.authorization-code-key-prefix:"
                    + "identity:v1:auth-code:}") String keyPrefix
    ) {
        return new RedisAuthorizationCodeStore(redisson, objectMapper, keyPrefix);
    }

    /**
     * 创建 Task 6 替换前共享的 IdP 服务身份请求头来源。
     *
     * <p>Creates the shared IdP service-identity header source until Task 6 replaces it.</p>
     *
     * @param authorizationHeaderFile 仅所有者可读的请求头文件；owner-only header file
     * @return 服务身份请求头提供器；service-identity header supplier
     */
    @Bean
    FileServiceAuthorizationSupplier fileServiceAuthorizationSupplier(
            @Value("${egon.idp.rbac3.authorization-header-file}")
            String authorizationHeaderFile
    ) {
        return new FileServiceAuthorizationSupplier(
                Path.of(authorizationHeaderFile)
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
            FileServiceAuthorizationSupplier authorizationHeader
    ) {
        return new HttpTenantMembershipAdapter(
                restClientBuilder.build(),
                baseUrl,
                authorizationHeader
        );
    }

    /**
     * 创建 RBAC3 USER Resource 入口决策适配器。
     *
     * <p>Creates the RBAC3 USER Resource entry-decision adapter.</p>
     *
     * @param restClientBuilder HTTP 客户端构建器；HTTP client builder
     * @param baseUrl RBAC3 基础地址；RBAC3 base URL
     * @param authorizationHeader IdP 服务身份请求头来源；IdP service header source
     * @return USER Resource 入口决策端口；USER Resource entry-decision port
     */
    @Bean
    UserResourceAccessAuthorizationPort userResourceAccessAuthorizationPort(
            RestClient.Builder restClientBuilder,
            @Value("${egon.idp.rbac3.base-url}") String baseUrl,
            FileServiceAuthorizationSupplier authorizationHeader
    ) {
        return new HttpUserResourceAccessAuthorizationAdapter(
                restClientBuilder.build(),
                baseUrl,
                authorizationHeader
        );
    }

    /**
     * 创建授权、换码和刷新共用的 USER Resource 入口策略。
     *
     * <p>Creates the USER Resource entry policy shared by authorization, exchange, and refresh.</p>
     *
     * @param resources Resource Server 查询端口；Resource Server lookup port
     * @param memberships 租户成员关系端口；tenant-membership port
     * @param authorization RBAC3 入口决策端口；RBAC3 entry-decision port
     * @return USER Resource 入口策略；USER Resource entry policy
     */
    @Bean
    UserResourceAccessPolicy userResourceAccessPolicy(
            ResourceServerStore resources,
            TenantMembershipPort memberships,
            UserResourceAccessAuthorizationPort authorization
    ) {
        return new UserResourceAccessPolicy(
                resources,
                memberships,
                authorization
        );
    }

    /**
     * 创建 OAuth 授权门面。
     *
     * <p>Creates the OAuth authorization facade.</p>
     *
     * @param clients OAuth Client 查询端口；OAuth Client lookup port
     * @param codes 授权码存储；authorization-code store
     * @param resourceAccess USER Resource 入口策略；USER Resource entry policy
     * @param idpClock UTC 业务时钟；UTC business clock
     * @param runtimePolicy 动态 OAuth 策略；dynamic OAuth policy
     * @return OAuth 授权门面；OAuth authorization facade
     */
    @Bean
    AuthorizationFacade authorizationFacade(
            OAuthClientStore clients,
            AuthorizationCodeStore codes,
            UserResourceAccessPolicy resourceAccess,
            @Qualifier("idpClock") Clock idpClock,
            IdpRuntimePolicy runtimePolicy
    ) {
        return AuthorizationFacade.dynamicTtl(
                clients,
                codes,
                resourceAccess,
                idpClock,
                () -> runtimePolicy.current().authorizationCodeTtl()
        );
    }
}
