package top.egon.cola.platform.idp.admin.oauth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import top.egon.cola.platform.idp.admin.support.rbac3.FileServiceAuthorizationSupplier;
import top.egon.cola.platform.idp.admin.support.rbac3.HttpTenantMembershipAdapter;
import top.egon.cola.platform.idp.admin.support.ddc.IdpRuntimePolicy;
import top.egon.cola.platform.idp.admin.oauth.repo.IdentityClientRedirectUriRepository;
import top.egon.cola.platform.idp.admin.oauth.repo.IdentityClientRepository;
import top.egon.cola.platform.idp.admin.oauth.repo.IdpSsoSessionStore;
import top.egon.cola.platform.idp.admin.oauth.repo.JpaOAuthClientStore;
import top.egon.cola.platform.idp.admin.oauth.repo.RedisAuthorizationCodeStore;
import top.egon.cola.platform.idp.admin.resource.repo.IdentityClientResourceGrantRepository;
import top.egon.cola.platform.idp.admin.resource.repo.IdentityResourceServerRepository;
import top.egon.cola.platform.idp.admin.support.security.IdpSsoAuthenticationFilter;
import top.egon.cola.platform.idp.admin.support.security.IdpAuthorizationAuthenticationEntryPoint;
import top.egon.cola.platform.idp.core.oauth.AuthorizationFacade;
import top.egon.cola.platform.idp.core.port.AuthorizationCodeStore;
import top.egon.cola.platform.idp.core.port.OAuthClientStore;
import top.egon.cola.platform.idp.core.port.TenantMembershipPort;

import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class OAuthConfig {

    @Bean
    @ConditionalOnMissingBean
    Clock idpClock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean
    SecureRandom idpSecureRandom() {
        return new SecureRandom();
    }

    @Bean
    IdpSsoSessionStore idpSsoSessionStore(
            @Qualifier("rbac3RuntimeRedissonClient") RedissonClient redisson,
            SecureRandom idpSecureRandom,
            @Value("${egon.idp.oauth.sso-session-key-prefix:"
                    + "identity:v1:sso-session:}") String keyPrefix
    ) {
        return new IdpSsoSessionStore(redisson, idpSecureRandom, keyPrefix);
    }

    @Bean
    IdpSsoAuthenticationFilter idpSsoAuthenticationFilter(
            IdpSsoSessionStore sessions
    ) {
        return new IdpSsoAuthenticationFilter(sessions);
    }

    @Bean
    IdpAuthorizationAuthenticationEntryPoint
            idpAuthorizationAuthenticationEntryPoint(
            @Value("${egon.idp.oauth.issuer}") String issuer,
            @Value("${egon.idp.oauth.login-uri}") String loginUri
    ) {
        return new IdpAuthorizationAuthenticationEntryPoint(issuer, loginUri);
    }

    @Bean
    OAuthClientStore oauthClientStore(
            IdentityClientRepository clients,
            IdentityClientRedirectUriRepository redirects,
            IdentityResourceServerRepository resources,
            IdentityClientResourceGrantRepository grants
    ) {
        return new JpaOAuthClientStore(clients, redirects, resources, grants);
    }

    @Bean
    AuthorizationCodeStore authorizationCodeStore(
            @Qualifier("rbac3RuntimeRedissonClient") RedissonClient redisson,
            ObjectMapper objectMapper,
            @Value("${egon.idp.oauth.authorization-code-key-prefix:"
                    + "identity:v1:auth-code:}") String keyPrefix
    ) {
        return new RedisAuthorizationCodeStore(
                redisson,
                objectMapper,
                keyPrefix
        );
    }

    @Bean
    TenantMembershipPort tenantMembershipPort(
            RestClient.Builder restClientBuilder,
            @Value("${egon.idp.rbac3.base-url}") String baseUrl,
            @Value("${egon.idp.rbac3.authorization-header-file}")
            String authorizationHeaderFile
    ) {
        return new HttpTenantMembershipAdapter(
                restClientBuilder.build(),
                baseUrl,
                new FileServiceAuthorizationSupplier(
                        Path.of(authorizationHeaderFile)
                )
        );
    }

    @Bean
    AuthorizationFacade authorizationFacade(
            OAuthClientStore clients,
            AuthorizationCodeStore codes,
            TenantMembershipPort memberships,
            @Qualifier("idpClock") Clock idpClock,
            IdpRuntimePolicy runtimePolicy
    ) {
        return AuthorizationFacade.dynamicTtl(
                clients,
                codes,
                memberships,
                idpClock,
                () -> runtimePolicy.current().authorizationCodeTtl()
        );
    }
}
