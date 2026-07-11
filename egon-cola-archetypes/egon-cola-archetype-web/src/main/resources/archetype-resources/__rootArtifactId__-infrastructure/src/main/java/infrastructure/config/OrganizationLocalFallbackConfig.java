package ${package}.infrastructure.config;

import ${package}.domain.client.CommandIdempotencyPort;
import ${package}.domain.client.teaching.GradeCachePort;
import ${package}.domain.client.teaching.SchoolClassCachePort;
import ${package}.domain.client.user.UserCachePort;
import ${package}.infrastructure.cache.InMemoryCommandIdempotencyAdapter;
import ${package}.infrastructure.cache.InMemoryGradeCache;
import ${package}.infrastructure.cache.InMemorySchoolClassCache;
import ${package}.infrastructure.cache.InMemoryUserCache;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "organization.integrations.redis", name = "enabled",
    havingValue = "false", matchIfMissing = true)
public class OrganizationLocalFallbackConfig {
    @Bean InMemoryUserCache inMemoryUserCache() { return new InMemoryUserCache(); }
    @Bean InMemoryGradeCache inMemoryGradeCache() { return new InMemoryGradeCache(); }
    @Bean InMemorySchoolClassCache inMemorySchoolClassCache() { return new InMemorySchoolClassCache(); }
    @Bean InMemoryCommandIdempotencyAdapter inMemoryCommandIdempotencyAdapter() {
        return new InMemoryCommandIdempotencyAdapter();
    }
}
