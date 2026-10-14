package top.egon.cola.platform.tianquan.shoubing.admin.oauth.config;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class OAuthConfigTest {

    @Test
    void keepsSharedSasAndIdentityDependenciesWithoutAnyLegacyClientOrSignerBean() {
        List<String> beans = Arrays.stream(OAuthConfig.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Bean.class))
                .map(java.lang.reflect.Method::getName).toList();
        assertThat(beans).containsExactlyInAnyOrder("idpClock", "idpSecureRandom",
                "tenantMembershipPort", "clientCredentialsAccessPolicy", "localExplicitServiceClientRegistrations");
        assertThat(Arrays.stream(OAuthConfig.class.getDeclaredMethods())
                .flatMap(method -> Arrays.stream(method.getParameterTypes())).map(Class::getName))
                .noneMatch(name -> name.contains(".oauth.repo.Identity") || name.contains("Jpa")
                        || name.endsWith("Rs256TokenService"));
        assertThat(new OAuthConfig().idpClock().getZone()).isEqualTo(java.time.ZoneOffset.UTC);
        assertThat(new OAuthConfig().idpSecureRandom()).isNotNull();
    }
    @Test
    void explicitLocalServiceRegistrationDoesNotDiscoverItsOwnUnstartedIssuer() {
        localRegistrationRunner().run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(
                    org.springframework.security.oauth2.client.registration.ClientRegistrationRepository.class);
            var registration = context.getBean(
                    org.springframework.security.oauth2.client.registration.ClientRegistrationRepository.class)
                    .findByRegistrationId("local-service");
            assertThat(registration.getProviderDetails().getIssuerUri())
                    .isEqualTo("http://127.0.0.1:1/platform");
            assertThat(registration.getProviderDetails().getTokenUri())
                    .isEqualTo("http://127.0.0.1:1/platform/oauth2/token");
            assertThat(registration.getClientAuthenticationMethod()).isEqualTo(
                    org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
        });
    }

    @Test
    void defaultProfileDoesNotTakeOverBootClientRegistrationsAndInvalidLocalInputsReject() {
        localRegistrationRunner().withPropertyValues("spring.profiles.active=production").run(context ->
                assertThat(context).hasNotFailed().doesNotHaveBean(
                        org.springframework.security.oauth2.client.registration.ClientRegistrationRepository.class));
        localRegistrationRunner().withPropertyValues(
                "spring.security.oauth2.client.provider.local-service.token-uri=http://127.0.0.1:1/oauth2/token")
                .run(context -> assertThat(context).hasFailed());
        localRegistrationRunner().withPropertyValues(
                "spring.security.oauth2.client.registration.local-service.client-authentication-method=client_secret_post")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void sasOwnersResolveTheActualGeneratedConverterBeanNames() {
        try (var context = new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
            context.registerBean("tenantIssuerService", top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.TenantIssuerService.class,
                    () -> org.mockito.Mockito.mock(top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.TenantIssuerService.class));
            context.registerBean("oAuthRealmRepository", top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthRealmRepository.class,
                    () -> org.mockito.Mockito.mock(top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthRealmRepository.class));
            context.registerBean("oAuthAuthorizationRepository", top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthAuthorizationRepository.class,
                    () -> org.mockito.Mockito.mock(top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthAuthorizationRepository.class));
            context.registerBean("oAuthTokenRepository", top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthTokenRepository.class,
                    () -> org.mockito.Mockito.mock(top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthTokenRepository.class));
            context.registerBean("oAuthClientRepository", top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthClientRepository.class,
                    () -> org.mockito.Mockito.mock(top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthClientRepository.class));
            context.registerBean("jacksonObjectMapper", com.fasterxml.jackson.databind.ObjectMapper.class, () -> new com.fasterxml.jackson.databind.ObjectMapper());
            context.registerBean("egonColaMybatisPlusClock", java.time.Clock.class, java.time.Clock::systemUTC);
            context.registerBean("oauthSecurityProjectionComponent", top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthSecurityProjectionComponent.class,
                    () -> org.mockito.Mockito.mock(top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthSecurityProjectionComponent.class));
            context.registerBean("oauthTokenIssueService", top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.OAuthTokenIssueService.class,
                    () -> org.mockito.Mockito.mock(top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.OAuthTokenIssueService.class));
            context.register(top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.SasAuthorizationConverterImpl.class,
                    top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.SasRegisteredClientConverterImpl.class,
                    top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.TenantRegisteredClientRepository.class,
                    top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.TenantAuthorizationServiceImpl.class);
            assertThatCode(context::refresh).doesNotThrowAnyException();
            assertThat(context.getBean("tenantRegisteredClientRepository"))
                    .isInstanceOf(top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.TenantRegisteredClientRepository.class);
            assertThat(context.getBean("tenantAuthorizationService"))
                    .isInstanceOf(top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.TenantAuthorizationServiceImpl.class);
        }
    }

    private org.springframework.boot.test.context.runner.ApplicationContextRunner localRegistrationRunner() {
        return new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withUserConfiguration(OAuthConfig.class)
                .withPropertyValues("spring.profiles.active=local",
                        "egon.tianquan-shoubing.oauth.local-explicit-service-registrations=true",
                        "spring.security.oauth2.client.registration.local-service.client-id=service-client",
                        "spring.security.oauth2.client.registration.local-service.client-secret=local-test-secret",
                        "spring.security.oauth2.client.registration.local-service.authorization-grant-type=client_credentials",
                        "spring.security.oauth2.client.registration.local-service.client-authentication-method=client_secret_basic",
                        "spring.security.oauth2.client.provider.local-service.issuer-uri=http://127.0.0.1:1/platform",
                        "spring.security.oauth2.client.provider.local-service.token-uri=http://127.0.0.1:1/platform/oauth2/token")
                .withBean("resourceServerStore", top.egon.cola.platform.tianquan.shoubing.core.port.ResourceServerStore.class,
                        () -> org.mockito.Mockito.mock(top.egon.cola.platform.tianquan.shoubing.core.port.ResourceServerStore.class))
                .withBean("tenantMembershipService", top.egon.cola.platform.tianquan.shoubing.admin.tenant.service.TenantMembershipService.class,
                        () -> org.mockito.Mockito.mock(top.egon.cola.platform.tianquan.shoubing.admin.tenant.service.TenantMembershipService.class))
                .withBean("idpStarterProperties", top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties.class,
                        () -> {
                            var settings = new top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties();
                            settings.getServiceClient().setAppId("stable-app");
                            settings.getServiceClient().setRegistrationId("local-service");
                            settings.getServiceClient().setCredentialVersions(java.util.Map.of("local-service", 1L));
                            return settings;
                        })
                .withBean("trustedIssuerRegistry", top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry.class,
                        () -> {
                            var registry = org.mockito.Mockito.mock(
                                    top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry.class);
                            org.mockito.Mockito.when(registry.findByIssuer(org.mockito.ArgumentMatchers.anyString()))
                                    .thenReturn(java.util.Optional.empty());
                            org.mockito.Mockito.when(registry.findByIssuer("http://127.0.0.1:1/platform"))
                                    .thenReturn(java.util.Optional.of(new top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry.TrustedIssuerVO(
                                            java.net.URI.create("http://127.0.0.1:1/platform"),
                                            java.net.URI.create("http://127.0.0.1:1/platform/oauth2/jwks"), "platform",
                                            top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry.RealmType.PLATFORM,
                                            null, top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry.Status.ACTIVE, 0L)));
                            return registry;
                        });
    }

}
