package ${package}.infrastructure.config;

import ${package}.domain.client.CommandIdempotencyPort;
import ${package}.domain.teaching.client.GradeCachePort;
import ${package}.domain.teaching.client.SchoolClassCachePort;
import ${package}.domain.user.client.UserCachePort;
import ${package}.infrastructure.cache.InMemoryCommandIdempotencyAdapter;
import ${package}.infrastructure.teaching.cache.InMemoryGradeCache;
import ${package}.infrastructure.teaching.cache.InMemorySchoolClassCache;
import ${package}.infrastructure.user.cache.InMemoryUserCache;
import ${package}.infrastructure.mq.LocalOrganizationEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OrganizationLocalFallbackConfig {
    @Bean @ConditionalOnProperty(prefix = "organization.integrations.redis", name = "enabled",
        havingValue = "false", matchIfMissing = true)
    InMemoryUserCache inMemoryUserCache() { return new InMemoryUserCache(); }
    @Bean @ConditionalOnProperty(prefix = "organization.integrations.redis", name = "enabled",
        havingValue = "false", matchIfMissing = true)
    InMemoryGradeCache inMemoryGradeCache() { return new InMemoryGradeCache(); }
    @Bean @ConditionalOnProperty(prefix = "organization.integrations.redis", name = "enabled",
        havingValue = "false", matchIfMissing = true)
    InMemorySchoolClassCache inMemorySchoolClassCache() { return new InMemorySchoolClassCache(); }
    @Bean @ConditionalOnProperty(prefix = "organization.integrations.redis", name = "enabled",
        havingValue = "false", matchIfMissing = true)
    InMemoryCommandIdempotencyAdapter inMemoryCommandIdempotencyAdapter() {
        return new InMemoryCommandIdempotencyAdapter();
    }

    @Bean @ConditionalOnProperty(prefix = "organization.integrations.rabbit", name = "enabled",
        havingValue = "false", matchIfMissing = true)
    LocalOrganizationEventPublisher localOrganizationEventPublisher() {
        return new LocalOrganizationEventPublisher();
    }
}
