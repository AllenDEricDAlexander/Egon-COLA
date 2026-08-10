package top.egon.cola.platform.idp.admin.support.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.idp.admin.identity.repo.IdentityUserDirectory;
import top.egon.cola.platform.idp.admin.identity.service.impl.IdentityUserServiceImpl;
import top.egon.cola.platform.idp.admin.oauth.repo.IdentityClientAudienceRepository;
import top.egon.cola.platform.idp.admin.oauth.repo.IdentityClientRedirectUriRepository;
import top.egon.cola.platform.idp.admin.oauth.repo.IdentityClientRepository;
import top.egon.cola.platform.idp.admin.oauth.repo.JpaOAuthClientStore;
import top.egon.cola.platform.idp.admin.oauth.service.impl.OAuthClientServiceImpl;
import top.egon.cola.platform.idp.admin.token.repo.IdentitySigningKeyRepository;
import top.egon.cola.platform.idp.admin.token.service.SigningKeyRuntime;
import top.egon.cola.platform.idp.admin.token.service.impl.SigningKeyServiceImpl;
import top.egon.cola.platform.idp.core.audit.IdentitySecurityEventPort;
import top.egon.cola.platform.idp.core.port.IdentityUserStatePort;
import top.egon.cola.platform.idp.core.port.IdentityUserStore;
import top.egon.cola.platform.idp.core.port.OAuthClientStore;
import top.egon.cola.platform.idp.core.port.PasswordCredentialStore;
import top.egon.cola.platform.idp.core.port.PasswordHashPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SpringConstructorResolutionTest {

    @Test
    void createsClassBasedTransactionProxyForOAuthClientStore() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(
                             TransactionProxyConfiguration.class
                     )) {
            OAuthClientStore store = context.getBean(OAuthClientStore.class);

            assertThat(AopUtils.isAopProxy(store)).isTrue();
        }
    }

    @Test
    void createsOAuthClientServiceUsingItsProductionConstructor() {
        try (AnnotationConfigApplicationContext context = contextWith(
                IdentityClientRepository.class,
                IdentityClientRedirectUriRepository.class,
                IdentityClientAudienceRepository.class,
                LongIdGenerator.class
        )) {
            context.registerBean(OAuthClientServiceImpl.class);
            context.refresh();

            assertThat(context.getBean(OAuthClientServiceImpl.class))
                    .isNotNull();
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
                LongIdGenerator.class
        )) {
            context.registerBean(IdentityUserServiceImpl.class);
            context.refresh();

            assertThat(context.getBean(IdentityUserServiceImpl.class))
                    .isNotNull();
        }
    }

    @Test
    void createsSigningKeyServiceUsingItsProductionConstructor() {
        try (AnnotationConfigApplicationContext context = contextWith(
                IdentitySigningKeyRepository.class,
                SigningKeyRuntime.class,
                ObjectMapper.class
        )) {
            context.registerBean(SigningKeyServiceImpl.class);
            context.refresh();

            assertThat(context.getBean(SigningKeyServiceImpl.class))
                    .isNotNull();
        }
    }

    @SafeVarargs
    private static AnnotationConfigApplicationContext contextWith(
            Class<?>... dependencyTypes
    ) {
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext();
        for (Class<?> dependencyType : dependencyTypes) {
            registerMock(context, dependencyType);
        }
        return context;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void registerMock(
            AnnotationConfigApplicationContext context,
            Class<?> dependencyType
    ) {
        context.registerBean(
                (Class) dependencyType,
                () -> mock(dependencyType)
        );
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement(proxyTargetClass = true)
    static class TransactionProxyConfiguration {

        @Bean
        PlatformTransactionManager transactionManager() {
            return mock(PlatformTransactionManager.class);
        }

        @Bean
        IdentityClientRepository identityClientRepository() {
            return mock(IdentityClientRepository.class);
        }

        @Bean
        IdentityClientRedirectUriRepository
                identityClientRedirectUriRepository() {
            return mock(IdentityClientRedirectUriRepository.class);
        }

        @Bean
        IdentityClientAudienceRepository identityClientAudienceRepository() {
            return mock(IdentityClientAudienceRepository.class);
        }

        @Bean
        OAuthClientStore oauthClientStore(
                IdentityClientRepository clients,
                IdentityClientRedirectUriRepository redirects,
                IdentityClientAudienceRepository audiences
        ) {
            return new JpaOAuthClientStore(clients, redirects, audiences);
        }
    }
}
