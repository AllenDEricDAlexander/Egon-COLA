package top.egon.cola.archetype.source.webopen.infrastructure;

import top.egon.cola.archetype.source.webopen.domain.user.entities.User;
import top.egon.cola.archetype.source.webopen.domain.user.enums.UserStatus;
import top.egon.cola.archetype.source.webopen.domain.user.vos.UserId;
import top.egon.cola.archetype.source.webopen.infrastructure.cache.RedisCommandIdempotencyAdapter;
import top.egon.cola.archetype.source.webopen.infrastructure.user.cache.RedisUserCache;
import top.egon.cola.archetype.source.webopen.infrastructure.config.OrganizationIntegrationProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationCacheTest {
    @Mock RedisTemplate<String, Object> redisTemplate;
    @Mock ValueOperations<String, Object> valueOperations;

    @Test
    void usesNamespacedKeysTtlAndAtomicCommandClaims() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        OrganizationIntegrationProperties properties = new OrganizationIntegrationProperties();
        RedisUserCache cache = new RedisUserCache(redisTemplate, properties);
        cache.put(new User(new UserId(1001L), "Mario", "mario@example.com", UserStatus.ACTIVE));
        verify(valueOperations).set(eq("student-management-organization:user:1001"),
            any(), eq(Duration.ofMinutes(10)));

        when(valueOperations.setIfAbsent(
            "student-management-organization:command:create-user:req-1", "1", Duration.ofHours(24)))
            .thenReturn(true);
        RedisCommandIdempotencyAdapter adapter = new RedisCommandIdempotencyAdapter(redisTemplate, properties);
        assertTrue(adapter.claim("create-user", "req-1"));
        adapter.release("create-user", "req-1");
        verify(redisTemplate).delete("student-management-organization:command:create-user:req-1");
    }
}
