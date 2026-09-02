package top.egon.cola.archetype.source.webopen.infrastructure.config;

import top.egon.cola.archetype.source.webopen.domain.client.CommandIdempotencyPort;
import top.egon.cola.archetype.source.webopen.domain.teaching.client.GradeCachePort;
import top.egon.cola.archetype.source.webopen.domain.teaching.client.SchoolClassCachePort;
import top.egon.cola.archetype.source.webopen.domain.user.client.UserCachePort;
import top.egon.cola.archetype.source.webopen.infrastructure.cache.InMemoryCommandIdempotencyAdapter;
import top.egon.cola.archetype.source.webopen.infrastructure.teaching.cache.InMemoryGradeCache;
import top.egon.cola.archetype.source.webopen.infrastructure.teaching.cache.InMemorySchoolClassCache;
import top.egon.cola.archetype.source.webopen.infrastructure.user.cache.InMemoryUserCache;
import top.egon.cola.archetype.source.webopen.infrastructure.mq.LocalOrganizationEventPublisher;
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
