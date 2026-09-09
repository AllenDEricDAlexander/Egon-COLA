package top.egon.cola.component.tianshu.autoconfigure;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import top.egon.cola.component.tianshu.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.tianshu.service.registry.DdcServiceKeyFactory;
import top.egon.cola.component.tianshu.api.client.DdcServiceRegistryClient;

/**
 * 在显式启用服务注册时装配服务键工厂和注册客户端。 Configures the service-key factory and registry client when service registry is explicitly enabled.
 */
@AutoConfiguration(after = DdcAutoConfiguration.class)
@EnableConfigurationProperties(DdcProperties.class)
@ConditionalOnProperty(
        prefix = "egon.cola.component.tianshu.registry",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class DdcRegistryAutoConfiguration {

    /**
     * 创建从 Tianshu 作用域属性生成服务键的工厂。 Creates the factory that derives service keys from Tianshu scope properties.
     *
     * @param properties Tianshu 属性。 Tianshu properties
     * @return 服务键工厂。 service-key factory
     */
    @Bean
    @ConditionalOnMissingBean(DdcServiceKeyFactory.class)
    public DdcServiceKeyFactory ddcServiceKeyFactory(
            DdcProperties properties) {
        return new DdcServiceKeyFactory(properties);
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(DdcServiceRegistryClient.class)
    public static BeanFactoryPostProcessor
            ddcServiceRegistryClientRequirement() {
        return beanFactory -> {
            throw new IllegalStateException(
                    "Required DdcServiceRegistryClient Port is missing; add "
                            + "top.egon:egon-cola-component-rpc-tianshu-adapter"
            );
        };
    }
}
