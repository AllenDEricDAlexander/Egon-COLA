package ${package}.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.integrations.redis.enabled", havingValue = "true")
public class RedisConfig {
    @Bean("integrationRedisTtl")
    Duration integrationRedisTtl(@Value("${symbol_dollar}{app.integrations.redis.ttl:10m}") Duration ttl) {
        return ttl;
    }
}
