package top.egon.cola.component.common.mybatis.extension;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationUtils;
import top.egon.cola.component.common.mybatis.support.TestBusinessMapper;
import top.egon.cola.component.common.mybatis.support.TestBusinessModel;
import top.egon.cola.component.common.mybatis.support.TestBusinessRepository;
import top.egon.cola.component.common.mybatis.support.TestTenantIdProvider;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 持久化基类无缓存依赖，缓存行为仅来自具体 Repository 的 Spring 代理。
 */
class EgonColaRepositoryCacheEnhancementTest {

    @Test
    void concreteRepositoryAnnotationsCacheReadsAndEvictSuccessfulWrites() {
        try (ValidatorFactory validators = Validation.buildDefaultValidatorFactory();
             AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            TestTenantIdProvider tenant = new TestTenantIdProvider();
            tenant.set(41L);
            TestBusinessMapper mapper = mock(TestBusinessMapper.class);
            TestBusinessModel row = persisted();
            when(mapper.selectActiveById(7L)).thenReturn(row);
            when(mapper.updateById(row)).thenReturn(1);
            EgonColaModelValidationUtils validation = new EgonColaModelValidationUtils(
                    new ValidationUtils(validators.getValidator()), tenant);
            context.register(CacheConfiguration.class);
            context.registerBean(TestBusinessMapper.class, () -> mapper);
            context.registerBean(CachedRepository.class,
                    () -> new CachedRepository(mapper, validation, tenant));
            context.refresh();
            CachedRepository repository = context.getBean(CachedRepository.class);

            assertThat(repository.find(7L)).isSameAs(row);
            assertThat(repository.find(7L)).isSameAs(row);
            verify(mapper, times(1)).selectActiveById(7L);
            assertThat(repository.change(row)).isTrue();
            assertThat(repository.find(7L)).isSameAs(row);
            verify(mapper, times(2)).selectActiveById(7L);
            // 原生 CRUD 不隐式缓存；同类 this 调用也不经过缓存代理。
            repository.getById(7L);
            repository.selfFind(7L);
            verify(mapper, times(4)).selectActiveById(7L);

            when(mapper.updateById(row)).thenReturn(0);
            assertThat(repository.change(row)).isFalse();
            repository.find(7L);
            verify(mapper, times(4)).selectActiveById(7L);
        }
    }

    private static TestBusinessModel persisted() {
        TestBusinessModel model = new TestBusinessModel().businessValues("valid", null);
        model.setId(7L);
        model.setTenantId(41L);
        model.setVersion(0L);
        model.setCreateUserId("user");
        model.setUpdateUserId("user");
        model.setCreateTime(Instant.parse("2026-01-01T00:00:00Z"));
        model.setUpdateTime(Instant.parse("2026-01-01T00:00:00Z"));
        return model;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableCaching(proxyTargetClass = true)
    static class CacheConfiguration {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }
    }

    @CacheConfig(cacheNames = "TestBusinessModel")
    static class CachedRepository extends TestBusinessRepository {

        CachedRepository(TestBusinessMapper mapper, EgonColaModelValidationUtils validation,
                         TestTenantIdProvider tenant) {
            super(mapper, validation, tenant, new EgonColaMybatisPlusProperties());
        }

        @Cacheable(key = "#p0", sync = true)
        public TestBusinessModel find(Long id) {
            return getById(id);
        }

        @CacheEvict(key = "#p0.id", condition = "#result")
        public boolean change(TestBusinessModel entity) {
            return updateById(entity);
        }

        public TestBusinessModel selfFind(Long id) {
            return find(id);
        }
    }
}
