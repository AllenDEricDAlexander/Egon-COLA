package ${package}.infrastructure.config;

import ${package}.domain.teaching.client.CourseCachePort;
import ${package}.domain.teaching.event.TeachingEventPublisher;
import ${package}.domain.teaching.gateway.TeachingQueryGateway;
import ${package}.domain.user.client.UserCachePort;
import ${package}.domain.user.event.UserEventPublisher;
import ${package}.domain.user.gateway.UserQueryGateway;
import ${package}.infrastructure.teaching.cache.InMemoryCourseCacheService;
import ${package}.infrastructure.teaching.client.impl.LocalTeachingQueryService;
import ${package}.infrastructure.teaching.mq.LocalTeachingEventPublisher;
import ${package}.infrastructure.user.cache.InMemoryUserCacheService;
import ${package}.infrastructure.user.client.impl.LocalUserQueryService;
import ${package}.infrastructure.user.mq.LocalUserEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration(proxyBeanMethods = false)
public class LocalAdapterConfiguration {
    @Bean("userQueryGateway")
    @ConditionalOnProperty(
            name = "app.integrations.external-http.enabled", havingValue = "false", matchIfMissing = true)
    UserQueryGateway userQueryGateway() {
        return new LocalUserQueryService();
    }

    @Bean("teachingQueryGateway")
    @ConditionalOnProperty(
            name = "app.integrations.external-http.enabled", havingValue = "false", matchIfMissing = true)
    TeachingQueryGateway teachingQueryGateway() {
        return new LocalTeachingQueryService();
    }

    @Bean("userCachePort")
    @ConditionalOnProperty(
            name = "app.integrations.redis.enabled", havingValue = "false", matchIfMissing = true)
    UserCachePort userCachePort(
            @Qualifier("transactionCompletionExecutor") TransactionCompletionExecutor executor) {
        return new InMemoryUserCacheService(executor);
    }

    @Bean("courseCachePort")
    @ConditionalOnProperty(
            name = "app.integrations.redis.enabled", havingValue = "false", matchIfMissing = true)
    CourseCachePort courseCachePort(
            @Qualifier("transactionCompletionExecutor") TransactionCompletionExecutor executor) {
        return new InMemoryCourseCacheService(executor);
    }

    @Bean("userEventPublisher")
    @ConditionalOnProperty(
            name = "app.integrations.rabbitmq.enabled", havingValue = "false", matchIfMissing = true)
    UserEventPublisher userEventPublisher(
            @Qualifier("transactionCompletionExecutor") TransactionCompletionExecutor executor) {
        return new LocalUserEventPublisher(executor);
    }

    @Bean("teachingEventPublisher")
    @ConditionalOnProperty(
            name = "app.integrations.rabbitmq.enabled", havingValue = "false", matchIfMissing = true)
    TeachingEventPublisher teachingEventPublisher(
            @Qualifier("transactionCompletionExecutor") TransactionCompletionExecutor executor) {
        return new LocalTeachingEventPublisher(executor);
    }
}
