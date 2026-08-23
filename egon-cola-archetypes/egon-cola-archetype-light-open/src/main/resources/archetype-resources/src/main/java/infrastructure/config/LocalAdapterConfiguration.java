package ${package}.infrastructure.config;

import ${package}.domain.teaching.service.CourseCacheService;
import ${package}.domain.teaching.service.TeachingEventPublisher;
import ${package}.domain.teaching.service.TeachingQueryService;
import ${package}.domain.user.service.UserCacheService;
import ${package}.domain.user.service.UserEventPublisher;
import ${package}.domain.user.service.UserQueryService;
import ${package}.infrastructure.teaching.cache.InMemoryCourseCacheService;
import ${package}.infrastructure.teaching.client.impl.LocalTeachingQueryService;
import ${package}.infrastructure.teaching.mq.LocalTeachingEventPublisher;
import ${package}.infrastructure.user.cache.InMemoryUserCacheService;
import ${package}.infrastructure.user.client.impl.LocalUserQueryService;
import ${package}.infrastructure.user.mq.LocalUserEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class LocalAdapterConfiguration {
    @Bean("userQueryService")
    @ConditionalOnProperty(
            name = "app.integrations.external-http.enabled", havingValue = "false", matchIfMissing = true)
    UserQueryService userQueryService() {
        return new LocalUserQueryService();
    }

    @Bean("teachingQueryService")
    @ConditionalOnProperty(
            name = "app.integrations.external-http.enabled", havingValue = "false", matchIfMissing = true)
    TeachingQueryService teachingQueryService() {
        return new LocalTeachingQueryService();
    }

    @Bean("userCacheService")
    @ConditionalOnProperty(
            name = "app.integrations.redis.enabled", havingValue = "false", matchIfMissing = true)
    UserCacheService userCacheService(TransactionCompletionExecutor executor) {
        return new InMemoryUserCacheService(executor);
    }

    @Bean("courseCacheService")
    @ConditionalOnProperty(
            name = "app.integrations.redis.enabled", havingValue = "false", matchIfMissing = true)
    CourseCacheService courseCacheService(TransactionCompletionExecutor executor) {
        return new InMemoryCourseCacheService(executor);
    }

    @Bean("userEventPublisher")
    @ConditionalOnProperty(
            name = "app.integrations.rabbitmq.enabled", havingValue = "false", matchIfMissing = true)
    UserEventPublisher userEventPublisher(TransactionCompletionExecutor executor) {
        return new LocalUserEventPublisher(executor);
    }

    @Bean("teachingEventPublisher")
    @ConditionalOnProperty(
            name = "app.integrations.rabbitmq.enabled", havingValue = "false", matchIfMissing = true)
    TeachingEventPublisher teachingEventPublisher(TransactionCompletionExecutor executor) {
        return new LocalTeachingEventPublisher(executor);
    }
}
