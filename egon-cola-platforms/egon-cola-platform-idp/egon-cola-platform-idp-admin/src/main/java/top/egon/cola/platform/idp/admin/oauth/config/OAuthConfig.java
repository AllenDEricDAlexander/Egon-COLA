package top.egon.cola.platform.idp.admin.oauth.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.idp.admin.oauth.repo.IdentityClientRedirectUriRepository;
import top.egon.cola.platform.idp.admin.oauth.repo.IdentityClientRepository;
import top.egon.cola.platform.idp.admin.oauth.repo.IdentityClientSecretRepository;
import top.egon.cola.platform.idp.admin.oauth.repo.JpaOAuthClientStore;
import top.egon.cola.platform.idp.admin.oauth.service.impl.ClientSecretBasicAuthenticator;
import top.egon.cola.platform.idp.admin.token.service.impl.ClientCredentialsTokenService;
import top.egon.cola.platform.idp.admin.token.service.impl.Rs256TokenService;
import top.egon.cola.platform.idp.core.port.OAuthClientStore;
import top.egon.cola.platform.idp.core.port.PasswordHashPort;
import top.egon.cola.platform.idp.core.port.ResourceServerStore;
import top.egon.cola.platform.idp.core.resource.ClientCredentialsAccessPolicy;

import java.security.SecureRandom;
import java.time.Clock;

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
     * 创建唯一的 Basic Client Secret 认证器。
     *
     * <p>Creates the single Basic Client Secret authenticator.</p>
     */
    @Bean
    @Primary
    ClientSecretBasicAuthenticator clientSecretBasicAuthenticator(
            IdentityClientRepository clients,
            IdentityClientSecretRepository secrets,
            PasswordHashPort passwordHashes
    ) {
        return new ClientSecretBasicAuthenticator(
                clients,
                secrets,
                passwordHashes
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

}
