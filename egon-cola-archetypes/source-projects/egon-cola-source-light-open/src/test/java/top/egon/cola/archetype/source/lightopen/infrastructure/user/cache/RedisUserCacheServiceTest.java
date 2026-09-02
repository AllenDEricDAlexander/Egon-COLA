package top.egon.cola.archetype.source.lightopen.infrastructure.user.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import top.egon.cola.archetype.source.lightopen.domain.user.enums.UserStatus;
import top.egon.cola.archetype.source.lightopen.domain.user.vos.UserSnapshot;
import top.egon.cola.archetype.source.lightopen.infrastructure.config.TransactionCompletionExecutor;
import top.egon.cola.archetype.source.lightopen.infrastructure.user.validators.UserInfrastructureValidator;
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
        UserSnapshot snapshot = new UserSnapshot(1001L, "Mario", "mario@example.com", UserStatus.ACTIVE);
        String json = objectMapper.writeValueAsString(snapshot);
        when(values.get("sample:user:1001")).thenReturn(json);
        when(values.setIfAbsent("sample:idempotency:user:req-1", "1", Duration.ofMinutes(5)))
                .thenReturn(true);

        cache.putUser(snapshot);
        assertEquals(snapshot, cache.getUser(1001L).orElseThrow());
        assertTrue(cache.claimIdempotency("req-1", Duration.ofMinutes(5)));

        verify(values).set("sample:user:1001", json, Duration.ofMinutes(10));
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

        transaction.executeWithoutResult(status -> cache.evictUser(1001L));
        verify(redis).delete("sample:user:1001");

        transaction.executeWithoutResult(status -> {
            cache.evictUser(1002L);
            status.setRollbackOnly();
        });
        verify(redis, org.mockito.Mockito.never()).delete("sample:user:1002");
    }
}
