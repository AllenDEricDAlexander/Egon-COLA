package top.egon.cola.component.accessguard.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import top.egon.cola.component.accessguard.core.GuardEngine;
import top.egon.cola.component.accessguard.execution.reactive.ReactiveGuardExecutor;
import top.egon.cola.component.accessguard.execution.reactive.ReactorGuardExecutor;

@AutoConfiguration(
        after = AccessGuardCoreAutoConfiguration.class,
        before = AccessGuardAopAutoConfiguration.class)
@ConditionalOnClass(name = {
        "reactor.core.publisher.Mono",
        "reactor.core.publisher.Flux"
})
@ConditionalOnProperty(
        prefix = AccessGuardProperties.PREFIX,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AccessGuardReactiveAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ReactiveGuardExecutor.class)
    ReactiveGuardExecutor accessGuardReactiveExecutor(GuardEngine engine) {
        return new ReactorGuardExecutor(engine);
    }
}
