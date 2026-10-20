package top.egon.cola.platform.tianquan.shoubing.admin.oauth.config;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScannerRegistrar;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.type.AnnotationMetadata;
import top.egon.cola.component.outbox.autoconfigure.OutboxMybatisPlusAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthMapperRegistrationTest {

    @Test
    void applicationMappersRemainRegisteredAlongsideTheOutboxScanner() {
        var registry = new DefaultListableBeanFactory();
        var registrar = new MapperScannerRegistrar();
        registrar.registerBeanDefinitions(AnnotationMetadata.introspect(OAuthPersistenceConfiguration.class), registry);
        registrar.registerBeanDefinitions(AnnotationMetadata.introspect(OutboxMybatisPlusAutoConfiguration.class), registry);
        try (var context = new GenericApplicationContext()) {
            context.refresh();
            for (String name : registry.getBeanNamesForType(MapperScannerConfigurer.class)) {
                var scanner = registry.getBean(name, MapperScannerConfigurer.class);
                scanner.setApplicationContext(context);
                scanner.postProcessBeanDefinitionRegistry(registry);
            }
            for (String mapper : new String[]{"OAuthRealmMapper", "OAuthClientMapper", "identityUserDAO",
                    "identityTenantDAO", "OAuthSigningKeyMapper", "outboxMessageDAO"}) {
                assertThat(registry.getBeanDefinition(mapper).getBeanClassName()).isEqualTo(MapperFactoryBean.class.getName());
            }
        }
    }
}
