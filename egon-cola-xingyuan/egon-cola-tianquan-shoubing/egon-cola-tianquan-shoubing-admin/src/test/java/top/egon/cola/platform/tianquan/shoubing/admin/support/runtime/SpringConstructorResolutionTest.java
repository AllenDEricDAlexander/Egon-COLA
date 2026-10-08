package top.egon.cola.platform.tianquan.shoubing.admin.support.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.SecureRandom;
import java.net.URI;
import java.time.Clock;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.repo.IdentityUserDirectory;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.service.impl.IdentityUserServiceImpl;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.OAuthClientViewConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.TenantIssuerService;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthManagementRealmResolver;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthClientRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthClientServiceImpl;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.OAuthGrantRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.OAuthResourceRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.converter.ClientResourceGrantViewConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.converter.ResourceServerViewConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.service.impl.ResourceServerServiceImpl;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.service.ResourceServerProjectionService;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.support.outbox.ResourceServerProjectionDeliveryHandler;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.support.outbox.TransactionalOutboxResourceServerEventAdapter;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.converter.SigningKeyViewConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthSigningKeyRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthRealmRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthKeyCipherComponent;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.SigningKeyServiceImpl;
import top.egon.cola.platform.tianquan.shoubing.core.audit.IdentitySecurityEventPort;
import top.egon.cola.platform.tianquan.shoubing.core.port.IdentityUserStatePort;
import top.egon.cola.platform.tianquan.shoubing.core.port.IdentityUserStore;
import top.egon.cola.platform.tianquan.shoubing.core.port.PasswordCredentialStore;
import top.egon.cola.platform.tianquan.shoubing.core.port.PasswordHashPort;
import top.egon.cola.platform.tianquan.shoubing.core.port.RefreshTokenStore;
import top.egon.cola.component.outbox.api.TransactionalOutbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SpringConstructorResolutionTest {

    @Test
    void createsClassBasedTransactionProxyForOAuthClientStore() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                TransactionProxyConfiguration.class)) {
            Object store = context.getBean("oauthClientStore");
            assertThat(AopUtils.isAopProxy(store)).isTrue();
        }
    }

    @Test
    void createsOAuthClientServiceUsingItsProductionConstructor() {
        assertThat(generatedSpringBeanName("top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter"
                + ".OAuthClientViewConverterImpl")).isEqualTo("OAuthClientViewConverterImpl");
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            registerNamedMock(context, "oAuthClientRepository", OAuthClientRepository.class);
            registerNamedMock(context, "oAuthResourceRepository", OAuthResourceRepository.class);
            registerNamedMock(context, "oAuthGrantRepository", OAuthGrantRepository.class);
            registerNamedMock(context, "OAuthClientViewConverterImpl", OAuthClientViewConverter.class);
            registerNamedMock(context, "identityOutboxPublisher", IdentitySecurityEventPort.class);
            context.registerBean("idpSecureRandom", SecureRandom.class, () -> new SecureRandom());
            context.registerBean("idpClock", Clock.class, Clock::systemUTC);
            context.registerBean(OAuthClientServiceImpl.class);
            context.refresh();

            assertThat(context.getBean(OAuthClientServiceImpl.class)).isNotNull();
        }
    }

    @Test
    void createsIdentityUserServiceUsingItsProductionConstructor() {
        try (AnnotationConfigApplicationContext context = contextWith(
                IdentityUserStore.class,
                PasswordCredentialStore.class,
                IdentityUserDirectory.class,
                PasswordHashPort.class,
                IdentityUserStatePort.class,
                IdentitySecurityEventPort.class,
                RefreshTokenStore.class)) {
            context.registerBean(IdentityUserServiceImpl.class);
            context.refresh();

            assertThat(context.getBean(IdentityUserServiceImpl.class)).isNotNull();
        }
    }

    @Test
    void createsSigningKeyServiceUsingItsProductionConstructor() {
        assertThat(generatedSpringBeanName("top.egon.cola.platform.tianquan.shoubing.admin.token.domain.converter"
                + ".SigningKeyViewConverterImpl")).isEqualTo("signingKeyViewConverterImpl");
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            registerNamedMock(context, "oAuthSigningKeyRepository", OAuthSigningKeyRepository.class);
            registerNamedMock(context, "oAuthRealmRepository", OAuthRealmRepository.class);
            registerNamedMock(context, "oauthKeyCipherComponent", OAuthKeyCipherComponent.class);
            registerNamedMock(context, "signingKeyViewConverterImpl", SigningKeyViewConverter.class);
            registerNamedMock(context, "jacksonObjectMapper", ObjectMapper.class);
            context.registerBean("idpClock", Clock.class, Clock::systemUTC);
            context.registerBean(SigningKeyServiceImpl.class);
            context.refresh();

            assertThat(context.getBean(SigningKeyServiceImpl.class)).isNotNull();
        }
    }

    @Test
    void createsResourceServerServiceWithGeneratedConverterBeanNames() {
        assertThat(generatedSpringBeanName("top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.converter"
                + ".ResourceServerViewConverterImpl")).isEqualTo("resourceServerViewConverterImpl");
        assertThat(generatedSpringBeanName("top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.converter"
                + ".ClientResourceGrantViewConverterImpl")).isEqualTo("clientResourceGrantViewConverterImpl");
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            registerNamedMock(context, "oAuthResourceRepository", OAuthResourceRepository.class);
            registerNamedMock(context, "oAuthGrantRepository", OAuthGrantRepository.class);
            registerNamedMock(context, "oAuthClientRepository", OAuthClientRepository.class);
            registerNamedMock(context, "resourceServerViewConverterImpl", ResourceServerViewConverter.class);
            registerNamedMock(context, "clientResourceGrantViewConverterImpl", ClientResourceGrantViewConverter.class);
            registerNamedMock(context, "transactionalOutbox", TransactionalOutbox.class);
            context.registerBean("idpClock", Clock.class, Clock::systemUTC);
            context.registerBean("resourceServerEventAdapter", TransactionalOutboxResourceServerEventAdapter.class);
            context.registerBean(ResourceServerServiceImpl.class);
            context.refresh();

            assertThat(context.getBean(ResourceServerServiceImpl.class)).isNotNull();
        }
    }

    @Test
    void createsOAuthManagementRealmResolverUsingItsProductionConstructor() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test-properties",
                    Map.of("egon.tianquan-shoubing.oauth.platform-management-tenant-id", "1001")));
            context.registerBean("oauthPublicOrigin", URI.class, () -> URI.create("https://idp.example.test"));
            registerNamedMock(context, "tenantIssuerService", TenantIssuerService.class);
            context.registerBean("idpClock", Clock.class, Clock::systemUTC);
            context.registerBean("oauthManagementRealmResolver", OAuthManagementRealmResolver.class);
            context.refresh();

            assertThat(context.getBean(OAuthManagementRealmResolver.class)).isNotNull();
        }
    }

    @Test
    void createsResourceProjectionAdapterAndHandlerThroughNamedQualifiedBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test-properties",
                    Map.of("egon.tianquan-shoubing.oauth.platform-management-tenant-id", "1001")));
            context.registerBean("oauthPublicOrigin", URI.class, () -> URI.create("https://idp.example.test"));
            registerNamedMock(context, "tenantIssuerService", TenantIssuerService.class);
            context.registerBean("idpClock", Clock.class, Clock::systemUTC);
            context.registerBean("oauthManagementRealmResolver", OAuthManagementRealmResolver.class);
            registerNamedMock(context, "transactionalOutbox", TransactionalOutbox.class);
            context.registerBean("resourceServerEventAdapter", TransactionalOutboxResourceServerEventAdapter.class);
            registerNamedMock(context, "oAuthResourceRepository", OAuthResourceRepository.class);
            registerNamedMock(context, "oAuthClientRepository", OAuthClientRepository.class);
            registerNamedMock(context, "oAuthGrantRepository", OAuthGrantRepository.class);
            registerNamedMock(context, "resourceServerProjectionService", ResourceServerProjectionService.class);
            registerNamedMock(context, "jacksonObjectMapper", ObjectMapper.class);
            context.registerBean(ResourceServerProjectionDeliveryHandler.class);
            context.refresh();

            assertThat(context.getBean(TransactionalOutboxResourceServerEventAdapter.class)).isNotNull();
            assertThat(context.getBean(ResourceServerProjectionDeliveryHandler.class)).isNotNull();
        }
    }

    @SafeVarargs
    private static AnnotationConfigApplicationContext contextWith(Class<?>... dependencyTypes) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        for (Class<?> dependencyType : dependencyTypes) {
            registerMock(context, dependencyType);
        }
        return context;
    }

    private static <T> void registerNamedMock(AnnotationConfigApplicationContext context, String name, Class<T> type) {
        context.registerBean(name, type, () -> mock(type));
    }

    private static <T> void registerMock(AnnotationConfigApplicationContext context, Class<T> type) {
        context.registerBean(type, () -> mock(type));
    }

    private static String generatedSpringBeanName(String className) {
        try {
            return new AnnotationBeanNameGenerator().generateBeanName(
                    new AnnotatedGenericBeanDefinition(Class.forName(className)), new DefaultListableBeanFactory());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("MapStruct did not generate the expected converter", failure);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement(proxyTargetClass = true)
    static class TransactionProxyConfiguration {

        @Bean
        PlatformTransactionManager transactionManager() {
            return mock(PlatformTransactionManager.class);
        }

        @Bean
        top.egon.cola.platform.tianquan.shoubing.admin.oauth.repo.IdentityClientRepository
                identityClientRepository() {
            return mock(top.egon.cola.platform.tianquan.shoubing.admin.oauth.repo.IdentityClientRepository.class);
        }

        @Bean
        top.egon.cola.platform.tianquan.shoubing.admin.oauth.repo.IdentityClientRedirectUriRepository
                identityClientRedirectUriRepository() {
            return mock(top.egon.cola.platform.tianquan.shoubing.admin.oauth.repo.IdentityClientRedirectUriRepository.class);
        }

        @Bean
        top.egon.cola.platform.tianquan.shoubing.core.port.OAuthClientStore oauthClientStore(
                top.egon.cola.platform.tianquan.shoubing.admin.oauth.repo.IdentityClientRepository clients,
                top.egon.cola.platform.tianquan.shoubing.admin.oauth.repo.IdentityClientRedirectUriRepository redirects) {
            return new top.egon.cola.platform.tianquan.shoubing.admin.oauth.repo.JpaOAuthClientStore(clients, redirects);
        }
    }
}
