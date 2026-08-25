package ${package}.infrastructure.user.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ${package}.domain.user.client.UserCachePort;
import ${package}.domain.user.vos.UserSnapshot;
import ${package}.infrastructure.config.TransactionCompletionExecutor;
import ${package}.infrastructure.user.validators.UserInfrastructureValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component("userCachePort")
@ConditionalOnProperty(name = "app.integrations.redis.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class RedisUserCacheService implements UserCachePort {
    @Qualifier("stringRedisTemplate")
    private final StringRedisTemplate redisTemplate;
    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;
    @Qualifier("userInfrastructureValidator")
    private final UserInfrastructureValidator validator;
    @Qualifier("transactionCompletionExecutor")
    private final TransactionCompletionExecutor transactionCompletionExecutor;
    @Value("${symbol_dollar}{spring.application.name}")
    private final String applicationName;
    @Value("${symbol_dollar}{app.integrations.redis.ttl:10m}")
    private final Duration ttl;

    @Override
    public Optional<UserSnapshot> getUser(Long userId) {
        String payload = redisTemplate.opsForValue().get(userKey(userId));
        if (payload == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(payload, UserSnapshot.class));
        } catch (JsonProcessingException exception) {
            throw validator.invalidCachePayload(payload, exception);
        }
    }

    @Override
    public void putUser(UserSnapshot user) {
        try {
            redisTemplate.opsForValue().set(
                    userKey(user.id()), objectMapper.writeValueAsString(user), ttl);
        } catch (JsonProcessingException exception) {
            throw validator.invalidCachePayload(user.id(), exception);
        }
    }

    @Override
    public void evictUser(Long userId) {
        transactionCompletionExecutor.executeAfterCommit(() -> redisTemplate.delete(userKey(userId)));
    }

    @Override
    public boolean claimIdempotency(String key, Duration claimTtl) {
        String redisKey = idempotencyKey(key);
        boolean claimed = Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(redisKey, "1", claimTtl));
        if (claimed) {
            transactionCompletionExecutor.executeAfterRollback(() -> redisTemplate.delete(redisKey));
        }
        return claimed;
    }

    private String userKey(Long userId) {
        return applicationName + ":user:" + userId;
    }

    private String idempotencyKey(String key) {
        return applicationName + ":idempotency:user:" + key;
    }
}
