package top.egon.cola.component.accessguard.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import top.egon.cola.component.accessguard.execution.CallerThreadTimeLimiter;
import top.egon.cola.component.accessguard.execution.RoutingTimeLimiter;
import top.egon.cola.component.accessguard.execution.ThreadPoolTimeLimiter;
import top.egon.cola.component.accessguard.execution.TimeLimiter;
import top.egon.cola.component.accessguard.execution.TimeLimiterType;
import top.egon.cola.component.accessguard.execution.VirtualThreadTimeLimiter;

import java.util.Map;

@AutoConfiguration
@ConditionalOnProperty(
        prefix = AccessGuardProperties.PREFIX,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AccessGuardTimeLimitAutoConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(TimeLimiter.class)
    public RoutingTimeLimiter accessGuardTimeLimiter(AccessGuardProperties properties) {
        AccessGuardProperties.ThreadPool pool = properties.getThreadPool();
        return new RoutingTimeLimiter(Map.of(
                TimeLimiterType.CALLER_THREAD,
                new CallerThreadTimeLimiter(),
                TimeLimiterType.THREAD_POOL,
                new ThreadPoolTimeLimiter(
                        pool.getName(),
                        pool.getCorePoolSize(),
                        pool.getMaxPoolSize(),
                        pool.getKeepAlive(),
                        pool.getQueueCapacity()),
                TimeLimiterType.VIRTUAL_THREAD,
                new VirtualThreadTimeLimiter()));
    }
}
