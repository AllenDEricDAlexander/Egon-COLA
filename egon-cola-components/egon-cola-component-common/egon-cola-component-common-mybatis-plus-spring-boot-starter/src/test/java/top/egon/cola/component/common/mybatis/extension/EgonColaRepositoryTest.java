package top.egon.cola.component.common.mybatis.extension;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationUtils;
import top.egon.cola.component.common.mybatis.support.TestBusinessMapper;
import top.egon.cola.component.common.mybatis.support.TestBusinessModel;
import top.egon.cola.component.common.mybatis.support.TestBusinessRepository;
import top.egon.cola.component.common.mybatis.support.TestTenantIdProvider;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EgonColaRepositoryTest {

    private ValidatorFactory validators;
    private TestBusinessMapper mapper;
    private TestBusinessRepository repository;
    private TestTenantIdProvider tenant;

    @BeforeEach
    void setUp() {
        validators = Validation.buildDefaultValidatorFactory();
        mapper = mock(TestBusinessMapper.class);
        tenant = new TestTenantIdProvider();
        tenant.set(41L);
        repository = new TestBusinessRepository(mapper,
                new EgonColaModelValidationUtils(new ValidationUtils(validators.getValidator()), tenant),
                tenant, new EgonColaMybatisPlusProperties());
    }

    @AfterEach
    void close() { validators.close(); }

    @Test
    void saveAndUpdateValidateBeforeMapperCalls() {
        when(mapper.insert(any(TestBusinessModel.class))).thenReturn(1);
        assertThat(repository.save(new TestBusinessModel().businessValues("valid", null))).isTrue();
        assertThatThrownBy(() -> repository.save(new TestBusinessModel().businessValues("", null)))
                .isInstanceOf(ConstraintViolationException.class);
        TestBusinessModel noVersion = new TestBusinessModel().businessValues("valid", null);
        noVersion.setId(1L);
        assertThatThrownBy(() -> repository.updateById(noVersion)).isInstanceOf(ConstraintViolationException.class);
        verify(mapper, never()).updateById(any(TestBusinessModel.class));
        tenant.set(null);
        assertThatThrownBy(() -> repository.getById(1L)).hasMessage("TENANT_CONTEXT_MISSING");
    }

    @Test
    void byIdReadsUseExplicitActiveSqlAndMissingDeleteIsFalse() {
        when(mapper.selectActiveById(1L)).thenReturn(persisted(1L));
        assertThat(repository.getById(1L)).isNotNull();
        when(mapper.selectActiveByIds(List.of(1L))).thenReturn(List.of(persisted(1L)));
        assertThat(repository.listByIds(List.of(1L))).hasSize(1);
        assertThat(repository.removeById(2L)).isFalse();
        verify(mapper, never()).selectById(any());
        verify(mapper, never()).deleteById(any(TestBusinessModel.class));
    }

    @Test
    void allBatchesValidateBoundsNullsAndDuplicateIdsBeforeSql() {
        assertThat(repository.saveBatch(List.of())).isFalse();
        assertThat(repository.updateBatchById(List.of())).isFalse();
        assertThat(repository.removeByIds(List.of())).isFalse();
        assertThatThrownBy(() -> repository.saveBatch(Collections.singletonList(null))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> repository.saveBatch(List.of(persisted(1L)), 0)).hasMessage("BATCH_SIZE_INVALID");
        assertThatThrownBy(() -> repository.saveBatch(List.of(persisted(1L), persisted(1L)))).hasMessage("BATCH_DUPLICATE_ID");
        assertThatThrownBy(() -> repository.saveBatch(Collections.nCopies(10_001, persisted(1L)))).hasMessage("BATCH_COLLECTION_SIZE_INVALID");
        verifyNoInteractions(mapper);
    }

    @Test
    void loadedTenantMismatchCannotEscapeTheRepositoryBoundary() {
        TestBusinessModel foreign = persisted(1L);
        foreign.setTenantId(99L);
        when(mapper.selectActiveById(1L)).thenReturn(foreign);
        assertThatThrownBy(() -> repository.getById(1L)).hasMessage("TENANT_CONTEXT_MISMATCH");
    }

    @Test
    void unknownJdbcCountsAreAllowedOnlyForInsertsAndConditionedFailuresMarkRollbackOnly() {
        for (var command : List.of(org.apache.ibatis.mapping.SqlCommandType.INSERT, org.apache.ibatis.mapping.SqlCommandType.UPDATE)) {
            javax.sql.DataSource source = mock(javax.sql.DataSource.class);
            var holder = new org.springframework.jdbc.datasource.ConnectionHolder(mock(java.sql.Connection.class));
            var configuration = new com.baomidou.mybatisplus.core.MybatisConfiguration();
            configuration.setEnvironment(new org.apache.ibatis.mapping.Environment("counts",
                    new org.mybatis.spring.transaction.SpringManagedTransactionFactory(), source));
            String id = TestBusinessMapper.class.getName() + (command == org.apache.ibatis.mapping.SqlCommandType.INSERT ? ".insert" : ".updateById");
            var statement = new org.apache.ibatis.mapping.MappedStatement.Builder(configuration, id,
                    new org.apache.ibatis.builder.StaticSqlSource(configuration, "test"), command).build();
            configuration.addMappedStatement(statement);
            var global = new com.baomidou.mybatisplus.core.config.GlobalConfig();
            global.setMetaObjectHandler(new top.egon.cola.component.common.mybatis.handler.EgonColaMetaObjectHandler(
                    tenant, () -> "user", java.time.Clock.systemUTC()));
            com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils.setGlobalConfig(configuration, global);
            var factory = mock(org.apache.ibatis.session.SqlSessionFactory.class);
            var session = mock(org.apache.ibatis.session.SqlSession.class);
            when(factory.getConfiguration()).thenReturn(configuration);
            when(factory.openSession(org.apache.ibatis.session.ExecutorType.BATCH, false)).thenReturn(session);
            var candidate = new TestBusinessRepository(mapper,
                    new EgonColaModelValidationUtils(new ValidationUtils(validators.getValidator()), tenant),
                    tenant, new EgonColaMybatisPlusProperties()) {
                @Override
                protected org.apache.ibatis.session.SqlSessionFactory getSqlSessionFactory() { return factory; }
                @Override
                public Class<TestBusinessMapper> getMapperClass() { return TestBusinessMapper.class; }
                List<org.apache.ibatis.executor.BatchResult> execute(TestBusinessModel entity) {
                    return executeMybatisBatch(List.of(entity), 1, new com.baomidou.mybatisplus.core.batch.BatchMethod<>(id));
                }
            };
            org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
            org.springframework.transaction.support.TransactionSynchronizationManager.setActualTransactionActive(true);
            org.springframework.transaction.support.TransactionSynchronizationManager.bindResource(source, holder);
            try {
                TestBusinessModel row = persisted(1L);
                var result = new org.apache.ibatis.executor.BatchResult(statement, "test", row);
                result.setUpdateCounts(new int[]{java.sql.Statement.SUCCESS_NO_INFO});
                when(session.flushStatements()).thenReturn(List.of(result));
                if (command == org.apache.ibatis.mapping.SqlCommandType.INSERT) {
                    assertThat(candidate.execute(row)).containsExactly(result);
                    assertThat(holder.isRollbackOnly()).isFalse();
                } else {
                    assertThatThrownBy(() -> candidate.execute(row)).hasMessage("BATCH_VERSION_CONFLICT_OR_WRITE_FAILED");
                    assertThat(holder.isRollbackOnly()).isTrue();
                }
            } finally {
                org.springframework.transaction.support.TransactionSynchronizationManager.unbindResource(source);
                org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
                org.springframework.transaction.support.TransactionSynchronizationManager.setActualTransactionActive(false);
            }
        }
    }

    @Test
    void invalidIdsAndVersionsFailBeforeMapperWrites() {
        TestBusinessModel invalidId = persisted(-1L);
        assertThatThrownBy(() -> repository.save(invalidId)).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> repository.updateById(invalidId)).isInstanceOf(ConstraintViolationException.class);
        TestBusinessModel invalidVersion = persisted(1L);
        invalidVersion.setVersion(-1L);
        assertThatThrownBy(() -> repository.updateById(invalidVersion)).isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(mapper);
    }

    @Test
    void businessFieldsCannotShadowCommonPropertiesOrPhysicalColumns() {
        EgonColaModelValidationUtils validation = new EgonColaModelValidationUtils(new ValidationUtils(validators.getValidator()), tenant);
        assertThatThrownBy(() -> validation.validateBusiness(new ShadowModelPO(),
                top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups.Operation.INSERT))
                .hasMessageContaining("MODEL_TECHNICAL_FIELD_SHADOWED");
        assertThatThrownBy(() -> validation.validateBusiness(new ShadowColumnPO(),
                top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups.Operation.INSERT))
                .hasMessageContaining("MODEL_TECHNICAL_FIELD_SHADOWED");
    }

    // Deliberately malformed PO fixtures verify rejection before persistence.
    static class ShadowModelPO extends top.egon.cola.component.common.mybatis.model.EgonModel<ShadowModelPO> {
        private Long version;
    }

    static class ShadowColumnPO extends top.egon.cola.component.common.mybatis.model.EgonModel<ShadowColumnPO> {
        @com.baomidou.mybatisplus.annotation.TableField("tenant_id")
        private Long otherTenant;
    }

    private static TestBusinessModel persisted(long id) {
        TestBusinessModel model = new TestBusinessModel().businessValues("valid", null);
        model.setId(id);
        model.setTenantId(41L);
        model.setVersion(0L);
        model.setCreateUserId("user");
        model.setUpdateUserId("user");
        model.setCreateTime(Instant.parse("2026-01-01T00:00:00Z"));
        model.setUpdateTime(Instant.parse("2026-01-01T00:00:00Z"));
        return model;
    }
}
