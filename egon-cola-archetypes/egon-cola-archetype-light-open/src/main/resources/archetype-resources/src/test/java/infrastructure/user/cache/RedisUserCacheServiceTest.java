package ${package}.infrastructure.user.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import ${package}.domain.user.enums.UserStatus;
import ${package}.domain.user.vos.UserSnapshot;
import ${package}.infrastructure.config.TransactionCompletionExecutor;
import ${package}.infrastructure.user.validators.UserInfrastructureValidator;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisUserCacheServiceTest {
    @Test
    void uses_namespaced_json_ttl_and_atomic_claims() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        RedisUserCacheService cache = new RedisUserCacheService(
                redis, objectMapper, new UserInfrastructureValidator(),
                new TransactionCompletionExecutor(), "sample", Duration.ofMinutes(10));
        UserSnapshot snapshot = new UserSnapshot("u-1", "Mario", "mario@example.com", UserStatus.ACTIVE);
        String json = objectMapper.writeValueAsString(snapshot);
        when(values.get("sample:user:u-1")).thenReturn(json);
        when(values.setIfAbsent("sample:idempotency:user:req-1", "1", Duration.ofMinutes(5)))
                .thenReturn(true);

        cache.putUser(snapshot);
        assertEquals(snapshot, cache.getUser("u-1").orElseThrow());
        assertTrue(cache.claimIdempotency("req-1", Duration.ofMinutes(5)));

        verify(values).set("sample:user:u-1", json, Duration.ofMinutes(10));
        verify(values).setIfAbsent("sample:idempotency:user:req-1", "1", Duration.ofMinutes(5));
    }

    @Test
    void evicts_only_after_commit() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        RedisUserCacheService cache = new RedisUserCacheService(
                redis, new ObjectMapper().findAndRegisterModules(), new UserInfrastructureValidator(),
                new TransactionCompletionExecutor(), "sample", Duration.ofMinutes(10));
        TransactionTemplate transaction = new TransactionTemplate(new DataSourceTransactionManager(
                new DriverManagerDataSource("jdbc:h2:mem:redis-user;DB_CLOSE_DELAY=-1", "sa", "")));

        transaction.executeWithoutResult(status -> cache.evictUser("u-1"));
        verify(redis).delete("sample:user:u-1");

        transaction.executeWithoutResult(status -> {
            cache.evictUser("u-2");
            status.setRollbackOnly();
        });
        verify(redis, org.mockito.Mockito.never()).delete("sample:user:u-2");
    }
}
