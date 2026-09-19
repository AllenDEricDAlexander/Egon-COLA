package top.egon.cola.component.common.mybatis.extension;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import top.egon.cola.component.common.core.cache.EgonColaCachePort;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.handler.EgonColaMetaObjectHandler;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationUtils;
import top.egon.cola.component.common.mybatis.support.TestBusinessMapper;
import top.egon.cola.component.common.mybatis.support.TestBusinessModel;
import top.egon.cola.component.common.mybatis.support.TestBusinessRepository;
import top.egon.cola.component.common.mybatis.support.TestTenantIdProvider;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Cached-read and transparent-eviction enhancement contract of {@link EgonColaRepository}
 * against the {@link EgonColaCachePort} SPI, plus the zero-impact baseline when the port is absent.
 */
class EgonColaRepositoryCacheEnhancementTest {

    private static final String REGION = "TestBusinessModel";
    private static final long TENANT = 41L;

    private ValidatorFactory validators;
    private TestBusinessMapper mapper;
    private TestTenantIdProvider tenant;
    private RecordingCachePort port;
    private TestBusinessCachedRepository repository;

    @BeforeEach
    void setUp() {
        validators = Validation.buildDefaultValidatorFactory();
        mapper = mock(TestBusinessMapper.class);
        tenant = new TestTenantIdProvider();
        tenant.set(TENANT);
        port = new RecordingCachePort();
        repository = cachedRepository(port);
    }

    @AfterEach
    void close() {
        validators.close();
    }

    @Test
    void getByCacheHitReturnsCachedValueWithoutSql() {
        TestBusinessModel cached = persisted(7L);
        port.cached.put("41:7", cached);
        assertThat(repository.getByCache(7L)).isSameAs(cached);
        assertThat(port.getCalls).containsExactly(REGION + "|41:7");
        verifyNoInteractions(mapper);
    }

    @Test
    void getByCacheMissLoadsExactlyOnceThroughGetById() {
        TestBusinessModel row = persisted(7L);
        when(mapper.selectActiveById(7L)).thenReturn(row);
        assertThat(repository.getByCache(7L)).isSameAs(row);
        assertThat(port.loaderCalls).isEqualTo(1);
        verify(mapper, times(1)).selectActiveById(7L);
    }

    @Test
    void getByCacheLoaderFailureKeepsGetByIdExceptionSemantics() {
        when(mapper.selectActiveById(7L)).thenThrow(new IllegalStateException("DB_DOWN"));
        assertThatThrownBy(() -> repository.getByCache(7L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("DB_DOWN");
    }

    @Test
    void listByCachePartialHitAssemblesInInputOrderAndDropsMissing() {
        TestBusinessModel eight = persisted(8L);
        port.cached.put("41:8", eight);
        // key 41:7 stays a miss and the mapper stub returns null for it
        assertThat(repository.listByCache(List.of(7L, 8L))).containsExactly(eight);
        assertThat(port.getAllCalls).containsExactly(List.of("41:7", "41:8"));
        verify(mapper, times(1)).selectActiveById(7L);
        verify(mapper, times(0)).selectActiveById(8L);
    }

    @Test
    void listByCacheDeduplicatesKeysKeepingFirstSeenOrder() {
        TestBusinessModel eight = persisted(8L);
        TestBusinessModel seven = persisted(7L);
        port.cached.put("41:8", eight);
        port.cached.put("41:7", seven);
        assertThat(repository.listByCache(List.of(8L, 7L, 8L))).containsExactly(eight, seven);
        assertThat(port.getAllCalls).containsExactly(List.of("41:8", "41:7"));
        verifyNoInteractions(mapper);
    }

    @Test
    void listByCacheEmptyInputShortCircuitsWithoutPortInteraction() {
        assertThat(repository.listByCache(List.of())).isEmpty();
        assertThat(port.getAllCalls).isEmpty();
        verifyNoInteractions(mapper);
    }

    @Test
    void cachedReadGuardsRunBeforeAnyPortInteraction() {
        assertThatThrownBy(() -> repository.getByCache(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ID_MUST_BE_POSITIVE_LONG");
        assertThatThrownBy(() -> repository.listByCache(List.of(0L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ID_MUST_BE_POSITIVE_LONG");
        tenant.set(null);
        assertThatThrownBy(() -> repository.getByCache(7L)).hasMessage("TENANT_CONTEXT_MISSING");
        assertThat(port.getCalls).isEmpty();
        assertThat(port.getAllCalls).isEmpty();
        assertThat(port.registrations).isEmpty();
    }

    @Test
    void absentPortMakesCachedReadsByteEquivalentToDirectReads() {
        TestBusinessCachedRepository withoutPort = cachedRepository(null);
        TestBusinessModel row = persisted(7L);
        when(mapper.selectActiveById(7L)).thenReturn(row);
        assertThat(withoutPort.getByCache(7L)).isSameAs(withoutPort.getById(7L)).isSameAs(row);
        verify(mapper, times(2)).selectActiveById(7L);
        List<TestBusinessModel> rows = List.of(persisted(7L), persisted(8L));
        when(mapper.selectActiveByIds(any())).thenReturn(new ArrayList<>(rows));
        assertThat(withoutPort.listByCache(List.of(7L, 8L))).isEqualTo(withoutPort.listByIds(List.of(7L, 8L)));
        verify(mapper, times(2)).selectActiveByIds(any());
    }

    @Test
    void absentPortWritesKeepBaselineBehavior() {
        TestBusinessCachedRepository withoutPort = cachedRepository(null);
        when(mapper.insert(any(TestBusinessModel.class))).thenAnswer(invocation -> {
            invocation.<TestBusinessModel>getArgument(0).setId(9L);
            return 1;
        });
        assertThat(withoutPort.save(new TestBusinessModel().businessValues("valid", null))).isTrue();
        verify(mapper, times(1)).insert(any(TestBusinessModel.class));
    }

    @Test
    void saveRegistersGeneratedIdEvictionOnce() {
        when(mapper.insert(any(TestBusinessModel.class))).thenAnswer(invocation -> {
            invocation.<TestBusinessModel>getArgument(0).setId(9L);
            return 1;
        });
        assertThat(repository.save(new TestBusinessModel().businessValues("valid", null))).isTrue();
        assertThat(port.registrations).singleElement()
                .extracting(RecordingCachePort.Registration::cacheName,
                        RecordingCachePort.Registration::exactKeys,
                        RecordingCachePort.Registration::globPatterns)
                .containsExactly(REGION, List.of("41:9"), List.of());
    }

    @Test
    void updateByIdAndRemoveByIdEntityRegisterExactKeys() {
        when(mapper.updateById(any(TestBusinessModel.class))).thenReturn(1);
        when(mapper.deleteVersionedById(any(TestBusinessModel.class))).thenReturn(1);
        assertThat(repository.updateById(persisted(7L))).isTrue();
        assertThat(repository.removeById(persisted(8L))).isTrue();
        assertThat(port.registrations)
                .extracting(RecordingCachePort.Registration::exactKeys)
                .containsExactly(List.of("41:7"), List.of("41:8"));
    }

    @Test
    void delegatedRemoveByIdChainRegistersExactlyOnce() {
        when(mapper.selectActiveById(7L)).thenReturn(persisted(7L));
        when(mapper.deleteVersionedById(any(TestBusinessModel.class))).thenReturn(1);
        assertThat(repository.removeById(7L, true)).isTrue();
        assertThat(port.registrations).singleElement()
                .extracting(RecordingCachePort.Registration::exactKeys)
                .isEqualTo(List.of("41:7"));
    }

    @Test
    void failedWritesRegisterNothing() {
        when(mapper.insert(any(TestBusinessModel.class))).thenReturn(0);
        when(mapper.updateById(any(TestBusinessModel.class))).thenReturn(0);
        when(mapper.deleteVersionedById(any(TestBusinessModel.class))).thenReturn(0);
        assertThat(repository.save(persisted(7L))).isFalse();
        assertThat(repository.updateById(persisted(8L))).isFalse();
        assertThat(repository.removeById(persisted(9L))).isFalse();
        assertThat(port.registrations).isEmpty();
    }

    @Test
    void wrapperUpdateRegistersTenantPrefixEviction() {
        when(mapper.update(any(TestBusinessModel.class), any())).thenReturn(1);
        assertThat(repository.update(persisted(7L),
                Wrappers.<TestBusinessModel>lambdaUpdate().eq(TestBusinessModel::getPayload, "x"))).isTrue();
        assertThat(port.registrations).singleElement()
                .extracting(RecordingCachePort.Registration::exactKeys,
                        RecordingCachePort.Registration::globPatterns)
                .containsExactly(List.of(), List.of("41:*"));
    }

    @Test
    void saveBatchRegistersEntityIdsOnceInOrder() {
        BatchHarness harness = batchHarness(Map.of("insert", SqlCommandType.INSERT));
        List<BatchResult> flushed = List.of(
                batchCount(harness, "insert", 5L, 1),
                batchCount(harness, "insert", 6L, 1));
        when(harness.session.flushStatements()).thenReturn(flushed);
        bindTransaction(harness);
        try {
            assertThat(batchRepository(harness).saveBatch(List.of(persisted(5L), persisted(6L)), 100)).isTrue();
        } finally {
            unbindTransaction(harness);
        }
        assertThat(port.registrations)
                .isEqualTo(List.of(new RecordingCachePort.Registration(REGION, List.of("41:5", "41:6"), List.of())));
    }

    @Test
    void removeByIdsDedupesRegistersInFirstSeenOrderViaUseFillOverload() {
        BatchHarness harness = batchHarness(Map.of("deleteVersionedById", SqlCommandType.DELETE));
        when(mapper.selectActiveByIds(any())).thenReturn(new ArrayList<>(List.of(persisted(8L), persisted(7L))));
        List<BatchResult> flushed = List.of(
                batchCount(harness, "deleteVersionedById", 8L, 1),
                batchCount(harness, "deleteVersionedById", 7L, 1));
        when(harness.session.flushStatements()).thenReturn(flushed);
        bindTransaction(harness);
        try {
            assertThat(batchRepository(harness).removeByIds(List.of(8L, 7L, 8L), true)).isTrue();
        } finally {
            unbindTransaction(harness);
        }
        assertThat(port.registrations)
                .isEqualTo(List.of(new RecordingCachePort.Registration(REGION, List.of("41:8", "41:7"), List.of())));
    }

    @Test
    void portGuardFailuresPropagateOutOfTheWriteAfterItSucceeded() {
        when(mapper.insert(any(TestBusinessModel.class))).thenAnswer(invocation -> {
            invocation.<TestBusinessModel>getArgument(0).setId(9L);
            return 1;
        });
        port.failure = new IllegalStateException("CACHE_KEY_TENANT_MISMATCH: registered");
        assertThatThrownBy(() -> repository.save(new TestBusinessModel().businessValues("valid", null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("CACHE_KEY_TENANT_MISMATCH: registered");
        verify(mapper, times(1)).insert(any(TestBusinessModel.class));
    }

    private EgonColaModelValidationUtils validationUtils() {
        return new EgonColaModelValidationUtils(new ValidationUtils(validators.getValidator()), tenant);
    }

    private TestBusinessCachedRepository cachedRepository(EgonColaCachePort cachePort) {
        return new TestBusinessCachedRepository(mapper, validationUtils(), tenant,
                new EgonColaMybatisPlusProperties(), cachePort);
    }

    private record BatchHarness(ConnectionHolder holder, SqlSessionFactory factory, SqlSession session) {
    }

    private BatchHarness batchHarness(Map<String, SqlCommandType> statements) {
        javax.sql.DataSource source = mock(javax.sql.DataSource.class);
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setEnvironment(new Environment("cache-enhancement", new SpringManagedTransactionFactory(), source));
        statements.forEach((name, command) -> configuration.addMappedStatement(new MappedStatement.Builder(
                configuration, TestBusinessMapper.class.getName() + "." + name,
                new StaticSqlSource(configuration, "test"), command).build()));
        GlobalConfig global = new GlobalConfig();
        global.setMetaObjectHandler(new EgonColaMetaObjectHandler(tenant, () -> "user", Clock.systemUTC()));
        GlobalConfigUtils.setGlobalConfig(configuration, global);
        SqlSessionFactory factory = mock(SqlSessionFactory.class);
        SqlSession session = mock(SqlSession.class);
        when(factory.getConfiguration()).thenReturn(configuration);
        when(factory.openSession(ExecutorType.BATCH, false)).thenReturn(session);
        return new BatchHarness(new ConnectionHolder(mock(java.sql.Connection.class)), factory, session);
    }

    private BatchResult batchCount(BatchHarness harness, String statementName, Long id, int count) {
        MappedStatement statement = harness.factory().getConfiguration()
                .getMappedStatement(TestBusinessMapper.class.getName() + "." + statementName);
        BatchResult result = new BatchResult(statement, "test", persisted(id));
        result.setUpdateCounts(new int[]{count});
        return result;
    }

    private void bindTransaction(BatchHarness harness) {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        javax.sql.DataSource source = harness.factory().getConfiguration().getEnvironment().getDataSource();
        TransactionSynchronizationManager.bindResource(source, harness.holder());
    }

    private void unbindTransaction(BatchHarness harness) {
        javax.sql.DataSource source = harness.factory().getConfiguration().getEnvironment().getDataSource();
        TransactionSynchronizationManager.unbindResource(source);
        TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    private TestBusinessCachedRepository batchRepository(BatchHarness harness) {
        return new TestBusinessCachedRepository(mapper, validationUtils(), tenant,
                new EgonColaMybatisPlusProperties(), port) {
            @Override
            protected SqlSessionFactory getSqlSessionFactory() {
                return harness.factory();
            }

            @Override
            public Class<TestBusinessMapper> getMapperClass() {
                return TestBusinessMapper.class;
            }
        };
    }

    private static TestBusinessModel persisted(long id) {
        TestBusinessModel model = new TestBusinessModel().businessValues("valid", null);
        model.setId(id);
        model.setTenantId(TENANT);
        model.setVersion(0L);
        model.setCreateUserId("user");
        model.setUpdateUserId("user");
        model.setCreateTime(Instant.parse("2026-01-01T00:00:00Z"));
        model.setUpdateTime(Instant.parse("2026-01-01T00:00:00Z"));
        return model;
    }

    /** Template repository exposing the cache-port seam; a null port simulates an unassembled starter. */
    static class TestBusinessCachedRepository extends TestBusinessRepository {

        final EgonColaCachePort port;

        TestBusinessCachedRepository(TestBusinessMapper baseMapper, EgonColaModelValidationUtils modelValidationUtils,
                EgonColaTenantIdProvider tenantIdProvider, EgonColaMybatisPlusProperties properties,
                EgonColaCachePort port) {
            super(baseMapper, modelValidationUtils, tenantIdProvider, properties);
            this.port = port;
        }

        @Override
        protected ObjectProvider<EgonColaCachePort> getCachePortProvider() {
            return port == null ? null : new SingletonProvider<>(port);
        }
    }

    private record SingletonProvider<E>(E value) implements ObjectProvider<E> {

        @Override
        public E getObject() {
            return value;
        }

        @Override
        public E getIfAvailable() {
            return value;
        }
    }

    /** Handoff recorder: keys and collections cross the layer boundary already validated by the repository. */
    static final class RecordingCachePort implements EgonColaCachePort {

        record Registration(String cacheName, List<String> exactKeys, List<String> globPatterns) {
        }

        final List<Registration> registrations = new ArrayList<>();
        final List<String> getCalls = new ArrayList<>();
        final List<List<String>> getAllCalls = new ArrayList<>();
        final Map<String, Object> cached = new LinkedHashMap<>();
        int loaderCalls;
        RuntimeException failure;

        @Override
        public Object get(String cacheName, String key, Supplier<Object> loader) {
            guard();
            getCalls.add(cacheName + "|" + key);
            if (cached.containsKey(key)) {
                return cached.get(key);
            }
            loaderCalls++;
            return loader.get();
        }

        @Override
        public List<Object> getAll(String cacheName, List<String> keys, Function<String, Object> loader) {
            guard();
            getAllCalls.add(List.copyOf(keys));
            List<Object> values = new ArrayList<>();
            for (String key : keys) {
                if (cached.containsKey(key)) {
                    values.add(cached.get(key));
                } else {
                    loaderCalls++;
                    values.add(loader.apply(key));
                }
            }
            return values;
        }

        @Override
        public void registerEvictionAfterCommit(String cacheName, Collection<String> exactKeys,
                Collection<String> globPatterns) {
            guard();
            registrations.add(new Registration(cacheName, List.copyOf(exactKeys), List.copyOf(globPatterns)));
        }

        private void guard() {
            if (failure != null) {
                throw failure;
            }
        }
    }
}
