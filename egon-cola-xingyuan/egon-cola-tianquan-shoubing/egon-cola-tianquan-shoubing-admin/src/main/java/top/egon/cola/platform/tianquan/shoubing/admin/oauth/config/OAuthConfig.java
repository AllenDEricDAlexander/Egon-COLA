package top.egon.cola.platform.tianquan.shoubing.admin.oauth.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import top.egon.cola.platform.tianquan.shoubing.admin.tenant.service.LocalTenantMembershipPort;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.service.TenantMembershipService;
import top.egon.cola.platform.tianquan.shoubing.core.port.ResourceServerStore;
import top.egon.cola.platform.tianquan.shoubing.core.port.TenantMembershipPort;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ClientCredentialsAccessPolicy;

import java.security.SecureRandom;
import java.time.Clock;

/**
 * Shared UTC/random, SERVICE grant-policy and USER membership dependencies for SAS and identity.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OAuth2ClientProperties.class)
public class OAuthConfig {

    /**
     * 提供可替换的 UTC 业务时钟。
     *
     * <p>Provides an overridable UTC business clock.</p>
     *
     * @return UTC 时钟；UTC clock
     */
    @Bean
    @Primary
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
     * 创建 Tianquan-Shoubing 自有的 Client Credentials 授权策略。
     *
     * <p>Creates the Tianquan-Shoubing-owned Client Credentials authorization policy.</p>
     *
     * @param resources Resource 与 Grant 查询端口；Resource and Grant lookup port
     * @return Client Credentials 授权策略；Client Credentials authorization policy
     */
    @Bean("clientCredentialsAccessPolicy")
    ClientCredentialsAccessPolicy clientCredentialsAccessPolicy(
            ResourceServerStore resources
    ) {
        return new ClientCredentialsAccessPolicy(resources);
    }

    /**
     * 使用 Tianquan-Shoubing 本地 tenant/membership 权威服务完成 USER 登录成员关系解析。
     *
     * <p>Uses the Tianquan-Shoubing-local tenant/membership authority for USER-login membership resolution.</p>
     */
    @Bean
    TenantMembershipPort tenantMembershipPort(
            TenantMembershipService memberships
    ) {
        return new LocalTenantMembershipPort(memberships);
    }

    /** Uses configured, trusted SERVICE endpoints without self-discovery during explicit local AS bootstrap. */
    @Bean("clientRegistrationRepository")
    @Profile("local")
    @ConditionalOnProperty(prefix = "egon.tianquan-shoubing.oauth", name = "local-explicit-service-registrations",
            havingValue = "true")
    @ConditionalOnMissingBean(ClientRegistrationRepository.class)
    ClientRegistrationRepository localExplicitServiceClientRegistrations(
            OAuth2ClientProperties properties,
            @Qualifier("trustedIssuerRegistry") TrustedIssuerRegistry registry,
            @Qualifier("idpStarterProperties") IdpStarterProperties idpProperties) {
        idpProperties.getServiceClient().validate();
        if (properties.getRegistration().isEmpty() || properties.getRegistration().size() > 1000) {
            throw new IllegalStateException("OAUTH_LOCAL_SERVICE_REGISTRATIONS_REQUIRED");
        }
        java.util.List<ClientRegistration> registrations = new java.util.ArrayList<>();
        for (var entry : properties.getRegistration().entrySet()) {
            String registrationId = entry.getKey();
            var registration = entry.getValue();
            String providerId = registration.getProvider() == null ? registrationId : registration.getProvider();
            var provider = properties.getProvider().get(providerId);
            if (!registrationId.matches("[A-Za-z0-9._-]{1,128}") || provider == null
                    || !"client_credentials".equals(registration.getAuthorizationGrantType())
                    || !"client_secret_basic".equals(registration.getClientAuthenticationMethod())
                    || registration.getClientId() == null
                    || !registration.getClientId().matches("[A-Za-z0-9._-]{1,128}")
                    || registration.getClientSecret() == null || registration.getClientSecret().isBlank()) {
                throw new IllegalStateException("OAUTH_LOCAL_SERVICE_REGISTRATION_INVALID");
            }
            var trusted = registry.findByIssuer(provider.getIssuerUri())
                    .orElseThrow(() -> new IllegalStateException("OAUTH_LOCAL_SERVICE_ISSUER_UNTRUSTED"));
            if (!(trusted.issuer().toASCIIString() + "/oauth2/token").equals(provider.getTokenUri())) {
                throw new IllegalStateException("OAUTH_LOCAL_SERVICE_TOKEN_ENDPOINT_INVALID");
            }
            idpProperties.getServiceClient().credentialVersion(registrationId);
            ClientRegistration.Builder builder = ClientRegistration.withRegistrationId(registrationId)
                    .clientId(registration.getClientId()).clientSecret(registration.getClientSecret())
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .issuerUri(trusted.issuer().toASCIIString()).tokenUri(provider.getTokenUri())
                    .jwkSetUri(trusted.jwksUri().toASCIIString());
            if (registration.getScope() != null) {
                builder.scope(registration.getScope());
            }
            registrations.add(builder.build());
        }
        return new InMemoryClientRegistrationRepository(registrations);
    }

}
