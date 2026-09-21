package top.egon.cola.archetype.source.lightopen.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.archetype.source.lightopen.domain.teaching.service.CourseIdempotencyService;
import top.egon.cola.archetype.source.lightopen.domain.user.service.UserIdempotencyService;
import top.egon.cola.archetype.source.lightopen.infrastructure.mq.MqMessageService;
import top.egon.cola.archetype.source.lightopen.infrastructure.mq.MqRouteEnum;
import top.egon.cola.archetype.source.lightopen.infrastructure.teaching.client.TeachingQueryClient;
import top.egon.cola.archetype.source.lightopen.infrastructure.teaching.client.impl.LocalTeachingQueryClientImpl;
import top.egon.cola.archetype.source.lightopen.infrastructure.user.client.UserQueryClient;
import top.egon.cola.archetype.source.lightopen.infrastructure.user.client.impl.LocalUserQueryClientImpl;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Fallback wiring for a profile without the matching integration: the application still boots and
 * keeps the same claim/release and after-commit semantics, only the external system is absent.
 */
@Configuration(proxyBeanMethods = false)
public class LocalAdapterConfiguration {
    @Bean("teachingQueryClient")
    @ConditionalOnProperty(
            name = "app.integrations.external-http.enabled", havingValue = "false", matchIfMissing = true)
    TeachingQueryClient teachingQueryClient() {
        return new LocalTeachingQueryClientImpl();
    }

    @Bean("userQueryClient")
    @ConditionalOnProperty(
            name = "app.integrations.external-http.enabled", havingValue = "false", matchIfMissing = true)
    UserQueryClient userQueryClient() {
        return new LocalUserQueryClientImpl();
    }

    @Bean("mqMessageService")
    @ConditionalOnProperty(
            name = "app.integrations.rabbitmq.enabled", havingValue = "false", matchIfMissing = true)
    MqMessageService mqMessageService(
            @Qualifier("transactionCompletionExecutor") TransactionCompletionExecutor executor) {
        return new InMemoryMqMessageService(executor);
    }

    @Bean("courseIdempotencyService")
    @ConditionalOnProperty(
            name = "app.integrations.redis.enabled", havingValue = "false", matchIfMissing = true)
    CourseIdempotencyService courseIdempotencyService(
            @Qualifier("transactionCompletionExecutor") TransactionCompletionExecutor executor) {
        return new InMemoryIdempotencyService(Duration.ofMinutes(5), executor);
    }

    @Bean("userIdempotencyService")
    @ConditionalOnProperty(
            name = "app.integrations.redis.enabled", havingValue = "false", matchIfMissing = true)
    UserIdempotencyService userIdempotencyService(
            @Qualifier("transactionCompletionExecutor") TransactionCompletionExecutor executor) {
        return new InMemoryIdempotencyService(Duration.ofMinutes(5), executor);
    }

    /** Records instead of publishing so a profile without a broker keeps the after-commit ordering. */
    @Slf4j
    static final class InMemoryMqMessageService implements MqMessageService {
        private final TransactionCompletionExecutor transactionCompletionExecutor;

        InMemoryMqMessageService(TransactionCompletionExecutor transactionCompletionExecutor) {
            this.transactionCompletionExecutor = transactionCompletionExecutor;
        }

        @Override
        public void publish(MqRouteEnum route, Object payload) {
            if (!MqRouteEnum.SCHEMA_VERSION.equals(route.getSchemaVersion()) || !route.accepts(payload)) {
                throw new IllegalStateException("MQ_ROUTE_REJECTED: " + route.name());
            }
            transactionCompletionExecutor.executeAfterCommit(
                    () -> log.info("Local message {} on route {}", payload.getClass().getSimpleName(), route));
        }
    }

    /** Same expiry-and-release contract as the Redis claim, without a Redis deployment. */
    static final class InMemoryIdempotencyService implements CourseIdempotencyService, UserIdempotencyService {
        private final ConcurrentMap<String, Instant> claims = new ConcurrentHashMap<>();
        private final Duration claimTtl;
        private final TransactionCompletionExecutor transactionCompletionExecutor;

        InMemoryIdempotencyService(Duration claimTtl, TransactionCompletionExecutor transactionCompletionExecutor) {
            this.claimTtl = claimTtl;
            this.transactionCompletionExecutor = transactionCompletionExecutor;
        }

        @Override
        public boolean claim(String key) {
            Instant expiresAt = Instant.now().plus(claimTtl);
            boolean[] claimed = new boolean[1];
            claims.compute(key, (ignored, current) -> {
                if (current == null || current.isBefore(Instant.now())) {
                    claimed[0] = true;
                    return expiresAt;
                }
                return current;
            });
            if (claimed[0]) {
                transactionCompletionExecutor.executeAfterRollback(() -> claims.remove(key, expiresAt));
            }
            return claimed[0];
        }
    }
}
