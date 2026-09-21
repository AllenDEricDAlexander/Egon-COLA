package top.egon.cola.archetype.source.web.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import top.egon.cola.archetype.source.web.infrastructure.service.impl.CommandIdempotencyServiceImpl;
import top.egon.cola.archetype.source.web.infrastructure.config.OrganizationIntegrationProperties;
import top.egon.cola.archetype.source.web.infrastructure.teaching.repo.GradeRepository;
import top.egon.cola.archetype.source.web.infrastructure.teaching.repo.SchoolClassRepository;
import top.egon.cola.archetype.source.web.infrastructure.user.repo.UserRepository;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Caching stays namespaced per tenant and command claims stay atomic: the entity regions belong to
 * the repository annotations, the claim keys to the idempotency Domain Service implementation.
 */
@ExtendWith(MockitoExtension.class)
class OrganizationCacheTest {
    @Mock RedisTemplate<String, Object> redisTemplate;
    @Mock ValueOperations<String, Object> valueOperations;

    @Test
    void usesAtomicCommandClaimsOnNamespacedKeysWithConfiguredTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        OrganizationIntegrationProperties properties = new OrganizationIntegrationProperties();
        CommandIdempotencyServiceImpl service = new CommandIdempotencyServiceImpl(redisTemplate, properties);

        when(valueOperations.setIfAbsent(
                "student-management-organization:command:create-user:req-1", "1", Duration.ofHours(24)))
                .thenReturn(true);
        assertTrue(service.claim("create-user", "req-1"));
        service.release("create-user", "req-1");
        verify(redisTemplate).delete("student-management-organization:command:create-user:req-1");
    }

    @Test
    void entityRegionsAreOwnedByTheRepositoryWithTheTenantScopedKey() {
        for (Class<?> repository : List.of(UserRepository.class, GradeRepository.class)) {
            CacheConfig config = repository.getAnnotation(CacheConfig.class);
            assertNotNull(config, "repository must own one cache region: " + repository);
            assertEquals(1, config.cacheNames().length, "one region per repository: " + repository);
            assertTrue(config.cacheNames()[0].endsWith("PO"),
                    "region is named after the persistence model: " + repository);
        }
        Cacheable cachedRead = annotated(UserRepository.class, "findCachedById", Cacheable.class);
        assertTrue(cachedRead.sync(), "a cached read must not stampede the database");
        assertTrue(cachedRead.key().contains("tenantId"), "region key is tenant scoped: " + cachedRead.key());
        CacheEvict cachedWrite = annotated(UserRepository.class, "updateCachedById", CacheEvict.class);
        assertTrue(cachedWrite.key().contains("tenantId"), "region key is tenant scoped: " + cachedWrite.key());
        assertEquals("#result", cachedWrite.condition(), "a failed write must not evict the region");
        // The school class lookup is keyed by grade plus class, so it stays an explicit SQL read.
        assertNull(SchoolClassRepository.class.getAnnotation(CacheConfig.class),
                "a compound key has no single-id region");
    }

    private static <A extends java.lang.annotation.Annotation> A annotated(
            Class<?> type, String methodName, Class<A> annotation) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(methodName)) {
                A value = method.getAnnotation(annotation);
                assertNotNull(value, methodName + " must carry @" + annotation.getSimpleName());
                return value;
            }
        }
        throw new AssertionError(methodName + " missing on " + type);
    }
}
