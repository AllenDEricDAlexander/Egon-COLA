package ${package}.infrastructure.user.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ${package}.domain.user.service.UserCacheService;
import ${package}.domain.user.vos.UserSnapshot;
import ${package}.infrastructure.config.TransactionCompletionExecutor;
import ${package}.infrastructure.user.validators.UserInfrastructureValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component("userCacheService")
@ConditionalOnProperty(name = "app.integrations.redis.enabled", havingValue = "true")
@RequiredArgsConstructor
public class RedisUserCacheService implements UserCacheService {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final UserInfrastructureValidator validator;
    private final TransactionCompletionExecutor transactionCompletionExecutor;
    @Value("${symbol_dollar}{spring.application.name}")
    private final String applicationName;
    @Value("${symbol_dollar}{app.integrations.redis.ttl:10m}")
    private final Duration ttl;

    @Override
    public Optional<UserSnapshot> getUser(String userId) {
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
    public void evictUser(String userId) {
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

    private String userKey(String userId) {
        return applicationName + ":user:" + userId;
    }

    private String idempotencyKey(String key) {
        return applicationName + ":idempotency:user:" + key;
    }
}
