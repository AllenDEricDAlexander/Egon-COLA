package top.egon.cola.component.common.mybatis.contract;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusInnerInterceptorAutoConfiguration;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusAutoConfiguration;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.common.mybatis.routing.EgonColaPhysicalTargetBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaRouteResult;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaWriteTargetResolver;
import top.egon.cola.component.common.mybatis.support.TestBusinessMapper;
import top.egon.cola.component.common.mybatis.support.TestBusinessModel;
import top.egon.cola.component.common.mybatis.support.TestBusinessRepository;
import top.egon.cola.component.common.mybatis.support.TestTenantIdProvider;
import top.egon.cola.component.common.mybatis.support.TestUserIdProvider;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Real MyBatis lifecycle allowlist contract using the assembled tenant and validation plugins. */
class EgonColaLifecycleContractTest {

    private static final ValidatorFactory VALIDATORS = Validation.buildDefaultValidatorFactory();
    private static final String MAPPER = TestBusinessMapper.class.getName();
    private static final String HISTORY = MAPPER + ".selectIncludingDeletedById";
    private static final String HISTORY_LOOKALIKE = MAPPER + ".selectIncludingDeletedByIdLookalike";
    private static final String RESTORE = MAPPER + ".restoreVersionedById";
    private static final String PHYSICAL_DELETE = MAPPER + ".physicalDelete";
    private static final String CREATE_MUTATION = MAPPER + ".mutateCreateTime";
    private static Path lifecycleMapperXml;

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ValidationAutoConfiguration.class,
                    EgonColaMybatisPlusAutoConfiguration.class,
                    MybatisPlusInnerInterceptorAutoConfiguration.class,
                    MybatisPlusAutoConfiguration.class))
            .withUserConfiguration(H2Configuration.class)
            .withBean(TestTenantIdProvider.class, TestTenantIdProvider::new)
            .withBean(TestUserIdProvider.class, TestUserIdProvider::new)
            .withBean(Validator.class, VALIDATORS::getValidator)
            .withBean(Clock.class, () -> Clock.fixed(
                    Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));

    @BeforeAll
    static void initializeIdentifierGenerator() throws IOException {
        SnowflakeIdGenerator.initialize(0L, Duration.ofMillis(5));
        lifecycleMapperXml = Files.createTempFile("egon-cola-lifecycle-", ".xml");
        Files.writeString(lifecycleMapperXml, lifecycleMapperXml());
    }

    @AfterAll
    static void closeValidatorFactory() throws IOException {
        VALIDATORS.close();
        if (lifecycleMapperXml != null) {
            Files.deleteIfExists(lifecycleMapperXml);
        }
    }

    @Test
    void historyAndRestoreRequireExactMapperAllowlistAndPreserveAllGuards() {
        contextRunner.withPropertyValues(
                        EgonColaMybatisPlusProperties.PREFIX + ".lifecycle.history-read-statements[0]=" + HISTORY,
                        EgonColaMybatisPlusProperties.PREFIX + ".lifecycle.restore-statements[0]=" + RESTORE)
                .run(context -> {
            TestTenantIdProvider tenant = context.getBean(TestTenantIdProvider.class);
            TestUserIdProvider user = context.getBean(TestUserIdProvider.class);
            TestBusinessRepository repository = context.getBean(TestBusinessRepository.class);
            SqlSessionTemplate sql = context.getBean(SqlSessionTemplate.class);
            JdbcTemplate jdbc = new JdbcTemplate(context.getBean(DataSource.class));
            TransactionTemplate transaction = transaction(context.getBean(DataSource.class));
            tenant.set(41L);
            user.set("lifecycle-user");

            TestBusinessModel versioned = createDeleted(repository, "versioned");
            TestBusinessModel conflicting = createDeleted(repository, "unique-live-key");
            jdbc.execute("ALTER TABLE test_business_record ADD active_title VARCHAR "
                    + "GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN title ELSE NULL END)");
            jdbc.execute("CREATE UNIQUE INDEX uq_test_business_live_title "
                    + "ON test_business_record (tenant_id, active_title)");
            TestBusinessModel liveCollision = new TestBusinessModel()
                    .businessValues("unique-live-key", "live");
            assertThat(repository.save(liveCollision)).isTrue();

            EgonColaMybatisPlusProperties.LifecycleProperties lifecycle =
                    context.getBean(EgonColaMybatisPlusProperties.class).getLifecycle();
            assertThat(lifecycle.getHistoryReadStatements()).containsExactly(HISTORY);
            assertThat(lifecycle.getRestoreStatements()).containsExactly(RESTORE);
            assertThat(EgonColaModelValidationGroups.Operation.RESTORE.getCode()).isEqualTo(5);

            TestBusinessModel loaded = history(sql, versioned.getId(), 41L);
            assertThat(loaded.getDeletedAt()).isNotNull();
            assertThat(loaded.getVersion()).isEqualTo(1L);
            assertThatThrownBy(() -> history(sql, versioned.getId(), 42L))
                    .hasRootCauseMessage("TENANT_CONTEXT_MISMATCH");
            assertThatThrownBy(() -> sql.selectOne(HISTORY_LOOKALIKE,
                    Map.of("id", versioned.getId(), "tenantId", 41L)))
                    .hasRootCauseMessage("ACTIVE_PREDICATE_REQUIRED");

            TestBusinessModel crossTenant = history(sql, versioned.getId(), 41L);
            tenant.set(42L);
            assertThatThrownBy(() -> restore(transaction, repository, crossTenant))
                    .hasRootCauseMessage("TENANT_CONTEXT_MISMATCH");
            tenant.set(41L);

            LocalDateTime deletedAt = loaded.getDeletedAt();
            loaded.setDeletedAt(deletedAt.minusSeconds(1));
            assertThat(restore(transaction, repository, loaded)).isFalse();
            assertThat(loaded.getDeletedAt()).isEqualTo(deletedAt.minusSeconds(1));
            assertThat(loaded.getVersion()).isEqualTo(1L);
            assertThat(jdbc.queryForObject(
                    "SELECT deleted_at FROM test_business_record WHERE id=?", LocalDateTime.class, versioned.getId()))
                    .isEqualTo(deletedAt);

            TestBusinessModel stale = history(sql, versioned.getId(), 41L);
            stale.setVersion(0L);
            assertThat(restore(transaction, repository, stale)).isFalse();
            assertThat(stale.getVersion()).isZero();

            TestBusinessModel collision = history(sql, conflicting.getId(), 41L);
            assertThatThrownBy(() -> restore(transaction, repository, collision))
                    .hasRootCauseInstanceOf(SQLException.class);
            assertThat(jdbc.queryForObject(
                    "SELECT deleted_at FROM test_business_record WHERE id=?", LocalDateTime.class, conflicting.getId()))
                    .isNotNull();
            assertThat(jdbc.queryForObject(
                    "SELECT version FROM test_business_record WHERE id=?", Long.class, conflicting.getId()))
                    .isEqualTo(1L);

            TestBusinessModel restored = history(sql, versioned.getId(), 41L);
            user.set("restore-user");
            assertThat(restore(transaction, repository, restored)).isTrue();
            assertThat(restored.getDeletedAt()).isNull();
            assertThat(restored.getVersion()).isEqualTo(2L);
            TestBusinessModel active = repository.getById(versioned.getId());
            assertThat(active.getTitle()).isEqualTo("versioned");
            assertThat(active.getUpdateUserId()).isEqualTo("restore-user");

            assertThatThrownBy(() -> sql.delete(PHYSICAL_DELETE,
                    Map.of("id", liveCollision.getId(), "tenantId", 41L)))
                    .hasRootCauseMessage("PHYSICAL_DELETE_FORBIDDEN");
            assertThatThrownBy(() -> sql.update(CREATE_MUTATION,
                    Map.of("id", liveCollision.getId(), "tenantId", 41L,
                            "createTime", Instant.parse("2026-01-02T00:00:00Z"))))
                    .hasRootCauseMessage("TECHNICAL_COLUMN_MUTATION_FORBIDDEN");
        });
    }

    @Test
    void historyAndRestoreStayDisabledByDefaultAndRuntimeMutationCannotEnableThem() {
        contextRunner.run(context -> {
            TestTenantIdProvider tenant = context.getBean(TestTenantIdProvider.class);
            TestUserIdProvider user = context.getBean(TestUserIdProvider.class);
            TestBusinessRepository repository = context.getBean(TestBusinessRepository.class);
            SqlSessionTemplate sql = context.getBean(SqlSessionTemplate.class);
            tenant.set(41L);
            user.set("lifecycle-user");
            TestBusinessModel deleted = createDeleted(repository, "default-closed");
            JdbcTemplate jdbc = new JdbcTemplate(context.getBean(DataSource.class));
            LocalDateTime tombstoneTime = jdbc.queryForObject(
                    "SELECT deleted_at FROM test_business_record WHERE id=?", LocalDateTime.class, deleted.getId());
            Long tombstoneVersion = jdbc.queryForObject(
                    "SELECT version FROM test_business_record WHERE id=?", Long.class, deleted.getId());
            assertThat(tombstoneVersion).isEqualTo(1L);
            deleted.setDeletedAt(tombstoneTime).setVersion(tombstoneVersion);

            var configuration = context.getBean(SqlSessionFactory.class).getConfiguration();
            assertThat(configuration.hasStatement(HISTORY, false)).isTrue();
            assertThat(configuration.getMappedStatement(HISTORY, false).getSqlCommandType())
                    .isEqualTo(SqlCommandType.SELECT);
            assertThat(configuration.getMappedStatement(HISTORY, false).getResource()).contains(".xml");
            EgonColaMybatisPlusProperties.LifecycleProperties lifecycle =
                    context.getBean(EgonColaMybatisPlusProperties.class).getLifecycle();
            assertThat(lifecycle.getHistoryReadStatements()).isEmpty();
            assertThat(lifecycle.getRestoreStatements()).isEmpty();
            assertThat(EgonColaMybatisPlusProperties.builder().build().getLifecycle().getHistoryReadStatements()).isEmpty();
            assertThat(EgonColaMybatisPlusProperties.builder().build().getLifecycle().getRestoreStatements()).isEmpty();

            assertThatThrownBy(() -> history(sql, deleted.getId(), 41L))
                    .hasRootCauseMessage("ACTIVE_PREDICATE_REQUIRED");
            assertThatThrownBy(() -> restore(repository, deleted))
                    .hasMessage("LIFECYCLE_RESTORE_NOT_ALLOWED");

            lifecycle.setHistoryReadStatements(Set.of(HISTORY));
            lifecycle.setRestoreStatements(Set.of(RESTORE));
            TransactionTemplate transaction = transaction(context.getBean(DataSource.class));
            assertThatThrownBy(() -> history(sql, deleted.getId(), 41L))
                    .hasRootCauseMessage("ACTIVE_PREDICATE_REQUIRED");
            assertThatThrownBy(() -> restore(transaction, repository, deleted))
                    .hasRootCauseMessage("LIFECYCLE_RESTORE_NOT_ALLOWED");
        });
    }

    @Test
    void wildcardHistoryStatementIsRejectedDuringConfigurationBinding() {
        contextRunner.withPropertyValues(
                        EgonColaMybatisPlusProperties.PREFIX
                                + ".lifecycle.history-read-statements[0]=*")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining("historyReadStatements");
                });
    }

    @Test
    void restoreAllowlistRejectsUnversionedDeleteStatementId() {
        contextRunner.withPropertyValues(
                        EgonColaMybatisPlusProperties.PREFIX
                                + ".lifecycle.restore-statements[0]=" + MAPPER + ".deleteVersionedById")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining("LIFECYCLE_STATEMENT_POLICY_INVALID");
                });
    }

    @Test
    void historyAllowlistRejectsNonSelectMapperStatement() {
        contextRunner.withPropertyValues(
                        EgonColaMybatisPlusProperties.PREFIX
                                + ".lifecycle.history-read-statements[0]=" + MAPPER + ".deleteVersionedById")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining("LIFECYCLE_HISTORY_STATEMENT_INVALID");
                });
    }

    private static TestBusinessModel createDeleted(TestBusinessRepository repository, String title) {
        TestBusinessModel row = new TestBusinessModel().businessValues(title, "payload");
        assertThat(repository.save(row)).isTrue();
        assertThat(repository.removeById(row.getId())).isTrue();
        return row;
    }

    private static TestBusinessModel history(SqlSessionTemplate sql, Long id, Long tenantId) {
        return sql.selectOne(HISTORY, Map.of("id", id, "tenantId", tenantId));
    }

    private static boolean restore(TestBusinessRepository repository, TestBusinessModel model) {
        return repository.restoreById(model);
    }

    private static boolean restore(TransactionTemplate transaction,
                                   TestBusinessRepository repository,
                                   TestBusinessModel model) {
        return transaction.execute(status -> restore(repository, model));
    }

    private static TransactionTemplate transaction(DataSource dataSource) {
        return new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    private static String lifecycleMapperXml() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
                <mapper namespace="%s">
                  <select id="selectIncludingDeletedById" resultType="%s">
                    SELECT id, tenant_id, create_user_id, create_time, update_user_id, update_time,
                           deleted_at, version, title, payload
                    FROM test_business_record
                    WHERE id=#{id} AND tenant_id=#{tenantId} AND deleted_at IS NOT NULL
                  </select>
                  <select id="selectIncludingDeletedByIdLookalike" resultType="%s">
                    SELECT id, tenant_id, create_user_id, create_time, update_user_id, update_time,
                           deleted_at, version, title, payload
                    FROM test_business_record
                    WHERE id=#{id} AND tenant_id=#{tenantId} AND deleted_at IS NOT NULL
                  </select>
                  <update id="restoreVersionedById">
                    UPDATE test_business_record
                    SET deleted_at=NULL, version=version+1,
                        update_user_id=#{et.updateUserId}, update_time=#{et.updateTime}
                    WHERE id=#{et.id} AND tenant_id=#{et.tenantId}
                      AND deleted_at=#{et.deletedAt} AND version=#{MP_OPTLOCK_VERSION_ORIGINAL}
                  </update>
                  <update id="mutateCreateTime">
                    UPDATE test_business_record SET create_time=#{createTime}
                    WHERE id=#{id} AND tenant_id=#{tenantId} AND deleted_at IS NULL
                  </update>
                  <delete id="physicalDelete">
                    DELETE FROM test_business_record WHERE id=#{id} AND tenant_id=#{tenantId}
                  </delete>
                </mapper>
                """.formatted(TestBusinessMapper.class.getName(), TestBusinessModel.class.getName(),
                TestBusinessModel.class.getName());
    }

    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    @MapperScan(basePackageClasses = TestBusinessMapper.class)
    static class H2Configuration {
        @Bean
        DataSource dataSource() {
            JdbcDataSource dataSource = new JdbcDataSource();
            dataSource.setURL("jdbc:h2:mem:lifecycle_" + System.nanoTime()
                    + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false");
            dataSource.setUser("sa");
            new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(dataSource);
            return dataSource;
        }

        @Bean("egonColaRoutingProfiles")
        Map<String, EgonColaRoutingProfileBO> routingProfiles() {
            return Map.of();
        }

        @Bean("egonColaWriteTargetResolver")
        EgonColaWriteTargetResolver writeTargetResolver() {
            return query -> new EgonColaRouteResult(
                    java.util.List.of(new EgonColaPhysicalTargetBO("primary", "public", query.logicalTable())),
                    "a".repeat(64));
        }

        @Bean("lifecycleMapperXmlCustomizer")
        com.baomidou.mybatisplus.autoconfigure.MybatisPlusPropertiesCustomizer mapperXmlCustomizer() {
            return properties -> properties.setMapperLocations(new String[]{
                    "classpath*:mybatis/*.xml", lifecycleMapperXml.toUri().toString()});
        }

        @Bean("testBusinessRepository")
        TestBusinessRepository testBusinessRepository(
                TestBusinessMapper mapper,
                EgonColaMybatisPlusProperties properties) {
            return new TestBusinessRepository(mapper, properties);
        }
    }
}
