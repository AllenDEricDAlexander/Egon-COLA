package top.egon.cola.component.accessguard.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import top.egon.cola.component.accessguard.aop.AccessGuardAop;
import top.egon.cola.component.accessguard.config.AccessGuardConfigProvider;
import top.egon.cola.component.accessguard.config.DefaultAccessGuardConfigProvider;
import top.egon.cola.component.accessguard.event.AccessGuardEventListener;
import top.egon.cola.component.accessguard.event.AccessGuardEventPublisher;
import top.egon.cola.component.accessguard.event.LoggingAccessGuardEventListener;
import top.egon.cola.component.accessguard.event.NoopAccessGuardEventPublisher;

import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties(AccessGuardProperties.class)
@ConditionalOnProperty(prefix = "egon.cola.component.access-guard", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AccessGuardAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AccessGuardConfigProvider accessGuardConfigProvider() {
        return new DefaultAccessGuardConfigProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public LoggingAccessGuardEventListener loggingAccessGuardEventListener() {
        return new LoggingAccessGuardEventListener();
    }

    @Bean
    @ConditionalOnMissingBean
    public AccessGuardEventPublisher accessGuardEventPublisher(List<AccessGuardEventListener> listeners) {
        return new NoopAccessGuardEventPublisher(listeners);
    }

    @Bean
    @ConditionalOnMissingBean
    public AccessGuardAop accessGuardAop() {
        return new AccessGuardAop();
    }
}
