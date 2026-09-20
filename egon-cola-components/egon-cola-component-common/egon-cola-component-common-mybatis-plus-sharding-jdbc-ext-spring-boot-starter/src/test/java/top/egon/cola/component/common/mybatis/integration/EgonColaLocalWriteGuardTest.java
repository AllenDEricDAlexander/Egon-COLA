package top.egon.cola.component.common.mybatis.integration;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.scripting.defaults.RawSqlSource;
import org.apache.ibatis.session.RowBounds;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.interceptor.EgonColaLocalWriteGuardInnerInterceptor;
import top.egon.cola.component.common.mybatis.interceptor.EgonColaTenantIdGuardInnerInterceptor;
import top.egon.cola.component.common.mybatis.routing.EgonColaPhysicalTargetBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaRouteResult;
import top.egon.cola.component.common.mybatis.routing.EgonColaWriteTargetResolver;
import top.egon.cola.component.common.mybatis.support.TestTenantIdProvider;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EgonColaLocalWriteGuardTest {

    @BeforeAll
    static void publishTenantContext() {
        new TestTenantIdProvider().set(41L);
    }

    @AfterAll
    static void clearTenantContext() {
        new TestTenantIdProvider().clear();
    }

    @Test
    void anotherWriteGroupMarksRollbackOnlyBeforeAnySecondSql() {
        DataSource source = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        MybatisConfiguration configuration = configuration(source);
        ConnectionHolder holder = begin(source, connection);
        try {
            var guard = guard(query -> result(query.logicalTable().equals("a") ? "primary_a" : "primary_b", query.logicalTable()));
            guard.beforePrepare(handler(configuration, "a", SqlCommandType.UPDATE), connection, null);
            assertThatThrownBy(() -> guard.beforePrepare(handler(configuration, "b", SqlCommandType.UPDATE), connection, null))
                    .hasMessageContaining("LOCAL_WRITE_TARGET_MISMATCH");
            assertThat(holder.isRollbackOnly()).isTrue();
            verifyNoInteractions(connection);
        } finally {
            end(source);
        }
        assertThat(TransactionSynchronizationManager.getResourceMap()).isEmpty();
    }

    @Test
    void twoTablesInOneGroupAreAllowedButAnotherFactoryIsNot() {
        DataSource source = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        MybatisConfiguration configuration = configuration(source);
        ConnectionHolder holder = begin(source, connection);
        try {
            var guard = guard(query -> result("primary_a", query.logicalTable()));
            guard.beforePrepare(handler(configuration, "a", SqlCommandType.UPDATE), connection, null);
            guard.beforePrepare(handler(configuration, "b", SqlCommandType.UPDATE), connection, null);
            assertThat(holder.isRollbackOnly()).isFalse();
            assertThatThrownBy(() -> guard.beforePrepare(handler(configuration(source), "b", SqlCommandType.UPDATE), connection, null))
                    .hasMessageContaining("LOCAL_WRITE_TARGET_MISMATCH");
            assertThat(holder.isRollbackOnly()).isTrue();
        } finally {
            end(source);
        }
    }

    @Test
    void multipleTargetsAreRejectedOutsideATransaction() {
        DataSource source = mock(DataSource.class);
        var guard = guard(query -> new EgonColaRouteResult(List.of(
                new EgonColaPhysicalTargetBO("primary_a", "public", "a_0"),
                new EgonColaPhysicalTargetBO("primary_a", "public", "a_1")), "a".repeat(64)));
        assertThatThrownBy(() -> guard.beforePrepare(handler(configuration(source), "a", SqlCommandType.UPDATE), mock(Connection.class), null))
                .hasMessageContaining("LOCAL_TRANSACTION_REQUIRED");
    }

    @Test
    void aSuspendedTransactionDoesNotLeakItsWriteTargetIntoRequiresNew() {
        DataSource source = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        MybatisConfiguration configuration = configuration(source);
        begin(source, connection);
        try {
            var guard = guard(query -> result(query.logicalTable().equals("a") ? "primary_a" : "primary_b", query.logicalTable()));
            guard.beforePrepare(handler(configuration, "a", SqlCommandType.UPDATE), connection, null);
            List<TransactionSynchronization> outer = TransactionSynchronizationManager.getSynchronizations();
            outer.forEach(TransactionSynchronization::suspend);
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.initSynchronization();
            guard.beforePrepare(handler(configuration, "b", SqlCommandType.UPDATE), connection, null);
            TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.initSynchronization();
            outer.forEach(sync -> { sync.resume(); TransactionSynchronizationManager.registerSynchronization(sync); });
            guard.beforePrepare(handler(configuration, "a", SqlCommandType.UPDATE), connection, null);
        } finally {
            end(source);
        }
    }

    @Test
    void configuredSecondaryKeysAreRequiredForWritesAndOnlyXmlQueriesMayFanOut() {
        try (var validators = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var strategy = new top.egon.cola.component.common.mybatis.routing.EgonColaTwoLevelRouteStrategy(
                    new top.egon.cola.component.common.core.validation.ValidationUtils(validators.getValidator()));
            var profile = new top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO("records",
                    top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO.TableKindEnum.TENANT_ID_TWO_LEVEL,
                    "mix64-v1", 1, 2, Map.of(0, "primary_a"), "order_id", "order", 0x9e3779b97f4a7c15L,
                    Map.of(new top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO.PartitionKeyBO(0, 0),
                            List.of(new EgonColaPhysicalTargetBO("primary_a", "public", "records_t0_b0")),
                            new top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO.PartitionKeyBO(0, 1),
                            List.of(new EgonColaPhysicalTargetBO("primary_a", "public", "records_t0_b1"))), 2, null);
            var properties = new EgonColaMybatisPlusProperties();
            var sql = new EgonColaTenantIdGuardInnerInterceptor(() -> "tester", properties);
            var guard = new EgonColaLocalWriteGuardInnerInterceptor(query -> { throw new AssertionError("Must use the shared two-level strategy"); },
                    sql, strategy, Map.of("records", profile));
            var configuration = configuration(mock(DataSource.class));
            assertThatThrownBy(() -> guard.beforePrepare(handler(configuration, "records", SqlCommandType.UPDATE), mock(Connection.class), null))
                    .hasMessageContaining("SHARDING_KEY");
            String read = "SELECT id FROM records WHERE tenant_id=#{tenantId} AND deleted_at IS NULL";
            assertThatCode(() -> guard.beforePrepare(handler(configuration, "test.Mapper.query", SqlCommandType.SELECT, read, parameters()),
                    mock(Connection.class), null)).doesNotThrowAnyException();
            assertThatThrownBy(() -> guard.beforePrepare(handler(configuration, "test.Mapper.selectActiveById", SqlCommandType.SELECT, read, parameters()),
                    mock(Connection.class), null)).hasMessageContaining("SHARDING_KEY");
        }
    }

    private static EgonColaLocalWriteGuardInnerInterceptor guard(EgonColaWriteTargetResolver resolver) {
        var properties = new EgonColaMybatisPlusProperties();
        var sqlGuard = new EgonColaTenantIdGuardInnerInterceptor(() -> "tester", properties);
        return new EgonColaLocalWriteGuardInnerInterceptor(resolver, sqlGuard, mock(top.egon.cola.component.common.mybatis.routing.EgonColaTwoLevelRouteStrategy.class), Map.of());
    }

    private static EgonColaRouteResult result(String group, String table) {
        return new EgonColaRouteResult(List.of(new EgonColaPhysicalTargetBO(group, "public", table)), "a".repeat(64));
    }

    static MybatisConfiguration configuration(DataSource source) {
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setEnvironment(new Environment("test", new SpringManagedTransactionFactory(), source));
        return configuration;
    }

    static StatementHandler handler(MybatisConfiguration configuration, String table, SqlCommandType command) {
        return handler(configuration, "test.Mapper.update", command,
                "UPDATE " + table + " SET title='changed', update_user_id=#{user}, update_time=#{now}, version=version+1 "
                        + "WHERE id=#{id} AND tenant_id=#{tenantId} AND deleted_at IS NULL AND version=#{version}", parameters());
    }

    static StatementHandler handler(MybatisConfiguration configuration, String id, SqlCommandType command, String sql, Object parameters) {
        MappedStatement statement = new MappedStatement.Builder(configuration, id, new RawSqlSource(configuration, sql, Map.class), command)
                .resource("test-mapper.xml").build();
        return configuration.newStatementHandler(mock(Executor.class), statement, parameters, RowBounds.DEFAULT, null,
                statement.getBoundSql(parameters));
    }

    static Map<String, Object> parameters() {
        return Map.of("id", 1L, "tenantId", 41L, "version", 0L, "user", "tester", "now", Instant.parse("2026-01-01T00:00:00Z"));
    }

    private static ConnectionHolder begin(DataSource source, Connection connection) {
        ConnectionHolder holder = new ConnectionHolder(connection);
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.bindResource(source, holder);
        return holder;
    }

    private static void end(DataSource source) {
        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        TransactionSynchronizationManager.unbindResource(source);
        TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }
}
