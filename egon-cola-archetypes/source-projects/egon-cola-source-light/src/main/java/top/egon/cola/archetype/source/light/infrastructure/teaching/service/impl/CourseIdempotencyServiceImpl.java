package top.egon.cola.archetype.source.light.infrastructure.teaching.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.archetype.source.light.domain.teaching.service.CourseIdempotencyService;
import top.egon.cola.archetype.source.light.infrastructure.config.TransactionCompletionExecutor;

import java.time.Duration;

/** SET-if-absent request claim, released again when the caller's transaction does not commit. */
@Validated
@Service("courseIdempotencyService")
@ConditionalOnProperty(name = "app.integrations.redis.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class CourseIdempotencyServiceImpl implements CourseIdempotencyService {
    private static final Duration CLAIM_TTL = Duration.ofMinutes(5);

    @Qualifier("stringRedisTemplate")
    private final StringRedisTemplate redisTemplate;
    @Qualifier("transactionCompletionExecutor")
    private final TransactionCompletionExecutor transactionCompletionExecutor;
    @Value("${spring.application.name}")
    private final String applicationName;

    @Override
    public boolean claim(String key) {
        String claimKey = applicationName + ":idempotency:course:" + key;
        boolean claimed = Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(claimKey, "1", CLAIM_TTL));
        if (claimed) {
            transactionCompletionExecutor.executeAfterRollback(() -> redisTemplate.delete(claimKey));
        }
        return claimed;
    }
}
