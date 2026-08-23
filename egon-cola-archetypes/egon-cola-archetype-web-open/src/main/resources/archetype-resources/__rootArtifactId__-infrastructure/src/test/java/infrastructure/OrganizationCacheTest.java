package ${package}.infrastructure;

import ${package}.domain.user.entities.User;
import ${package}.domain.user.enums.UserStatus;
import ${package}.domain.user.vos.UserId;
import ${package}.infrastructure.cache.RedisCommandIdempotencyAdapter;
import ${package}.infrastructure.user.cache.RedisUserCache;
import ${package}.infrastructure.config.OrganizationIntegrationProperties;
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
        cache.put(new User(new UserId("u-1"), "Mario", "mario@example.com", UserStatus.ACTIVE));
        verify(valueOperations).set(eq("student-management-organization:user:u-1"),
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
