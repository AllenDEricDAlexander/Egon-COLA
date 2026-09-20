package top.egon.cola.component.common.mybatis.integration;

import org.apache.ibatis.mapping.SqlCommandType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.interceptor.EgonColaLocalWriteGuardInnerInterceptor;
import top.egon.cola.component.common.mybatis.interceptor.EgonColaTenantIdGuardInnerInterceptor;
import top.egon.cola.component.common.mybatis.routing.EgonColaPhysicalTargetBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaRouteResult;
import top.egon.cola.component.common.mybatis.support.TestTenantIdProvider;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EgonColaQuerySqlTest {

    private final TestTenantIdProvider tenant = new TestTenantIdProvider();

    @AfterEach
    void clearTenantContext() {
        tenant.clear();
    }

    @Test
    void finalQueriesRequireTenantAndActiveConditionsEvenForScalarResults() {
        for (String sql : List.of("SELECT count(*) FROM records WHERE tenant_id=#{tenantId}",
                "SELECT id FROM records WHERE deleted_at IS NULL", "SELECT id FROM records WHERE tenant_id=#{tenantId} OR 1=1",
                "SELECT id FROM records WHERE tenant_id=42 AND deleted_at IS NULL")) {
            assertThatThrownBy(() -> check(sql, SqlCommandType.SELECT)).isInstanceOf(IllegalStateException.class);
        }
        assertThatCode(() -> check("SELECT count(*) FROM records WHERE tenant_id=#{tenantId} AND deleted_at IS NULL", SqlCommandType.SELECT))
                .doesNotThrowAnyException();
    }

    @Test
    void leftJoinRightTableConditionsBelongInOnAndEveryAliasMustRemainScoped() {
        String base = "SELECT a.id FROM records a LEFT JOIN children b ON a.id=b.parent_id AND a.tenant_id=b.tenant_id ";
        assertThatThrownBy(() -> check(base + "WHERE a.tenant_id=#{tenantId} AND a.deleted_at IS NULL AND b.deleted_at IS NULL AND b.tenant_id=#{tenantId}", SqlCommandType.SELECT))
                .isInstanceOf(IllegalStateException.class);
        String safe = "SELECT a.id FROM records a LEFT JOIN children b ON a.id=b.parent_id AND a.tenant_id=b.tenant_id "
                + "AND b.tenant_id=#{tenantId} AND b.deleted_at IS NULL WHERE a.tenant_id=#{tenantId} AND a.deleted_at IS NULL";
        assertThatCode(() -> check(safe, SqlCommandType.SELECT)).doesNotThrowAnyException();
    }

    @Test
    void customXmlCannotMutateProtectedFieldsOrOmitVersionChecks() {
        for (String sql : List.of(
                "UPDATE records SET tenant_id=42 WHERE id=#{id} AND tenant_id=#{tenantId} AND deleted_at IS NULL AND version=#{version}",
                "UPDATE records SET deleted_at=NULL WHERE id=#{id} AND tenant_id=#{tenantId} AND deleted_at IS NULL AND version=#{version}",
                "UPDATE records SET title='changed',version=version+1,update_user_id=#{user},update_time=#{now} WHERE id=#{id} AND tenant_id=#{tenantId} AND deleted_at IS NULL",
                "DELETE FROM records WHERE id=#{id} AND tenant_id=#{tenantId} AND deleted_at IS NULL AND version=#{version}")) {
            assertThatThrownBy(() -> check(sql, sql.startsWith("DELETE") ? SqlCommandType.DELETE : SqlCommandType.UPDATE))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void plainCommonTenantPolicyRetainsTheEntireSignedLongDomain() {
        for (long tenantId : new long[]{0L, -7L, Long.MIN_VALUE}) {
            tenant.set(tenantId);
            var properties = new EgonColaMybatisPlusProperties();
            var tenantGuard = new EgonColaTenantIdGuardInnerInterceptor(() -> "tester", properties);
            var guard = new EgonColaLocalWriteGuardInnerInterceptor(query -> new EgonColaRouteResult(
                    List.of(new EgonColaPhysicalTargetBO("primary", "public", query.logicalTable())), "a".repeat(64)), tenantGuard,
                    mock(top.egon.cola.component.common.mybatis.routing.EgonColaTwoLevelRouteStrategy.class), Map.of());
            var configuration = EgonColaLocalWriteGuardTest.configuration(mock(DataSource.class));
            assertThatCode(() -> guard.beforePrepare(EgonColaLocalWriteGuardTest.handler(configuration, "test.Mapper.query", SqlCommandType.SELECT,
                    "SELECT count(*) FROM records WHERE tenant_id=" + tenantId + " AND deleted_at IS NULL", Map.of()), mock(Connection.class), null))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void everyUnionAndNestedQueryScopeMustBeIsolated() {
        String scoped = "SELECT id FROM records WHERE tenant_id=#{tenantId} AND deleted_at IS NULL";
        assertThatCode(() -> check(scoped + " UNION ALL " + scoped, SqlCommandType.SELECT)).doesNotThrowAnyException();
        assertThatCode(() -> check("SELECT count(*) FROM (" + scoped + ") scoped_rows", SqlCommandType.SELECT)).doesNotThrowAnyException();
        assertThatThrownBy(() -> check(scoped + " UNION ALL SELECT id FROM records", SqlCommandType.SELECT)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void explicitSchemasMustAgreeWithTheResolvedPhysicalTarget() {
        assertThatCode(() -> check("SELECT id FROM public.records WHERE tenant_id=#{tenantId} AND deleted_at IS NULL", SqlCommandType.SELECT))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> check("SELECT id FROM other.records WHERE tenant_id=#{tenantId} AND deleted_at IS NULL", SqlCommandType.SELECT))
                .hasMessage("SQL_SCHEMA_ROUTE_MISMATCH");
    }

    @Test
    void registeredBulkDeleteCannotBecomeAnUnversionedBusinessUpdate() {
        var properties = new EgonColaMybatisPlusProperties();
        properties.getLocalWriteGuard().getAllowedRootStatements().put("test.Mapper.deleteRoot", "parent_id");
        tenant.set(41L);
        var guard = new EgonColaLocalWriteGuardInnerInterceptor(query -> new EgonColaRouteResult(
                List.of(new EgonColaPhysicalTargetBO("primary", "public", "records")), "a".repeat(64)),
                new EgonColaTenantIdGuardInnerInterceptor(() -> "tester", properties),
                mock(top.egon.cola.component.common.mybatis.routing.EgonColaTwoLevelRouteStrategy.class), Map.of());
        var parameters = new java.util.HashMap<>(EgonColaLocalWriteGuardTest.parameters());
        parameters.put("root", 9L);
        var configuration = EgonColaLocalWriteGuardTest.configuration(mock(DataSource.class));
        String safe = "UPDATE records SET deleted_at=(CURRENT_TIMESTAMP AT TIME ZONE 'UTC'), version=version+1, "
                + "update_user_id=#{user}, update_time=#{now} WHERE parent_id=#{root} AND tenant_id=#{tenantId} AND deleted_at IS NULL";
        assertThatCode(() -> guard.beforePrepare(EgonColaLocalWriteGuardTest.handler(configuration, "test.Mapper.deleteRoot", SqlCommandType.UPDATE,
                safe, parameters), mock(Connection.class), null)).doesNotThrowAnyException();
        assertThatThrownBy(() -> guard.beforePrepare(EgonColaLocalWriteGuardTest.handler(configuration, "test.Mapper.deleteRoot", SqlCommandType.UPDATE,
                safe.replace("deleted_at=(CURRENT_TIMESTAMP AT TIME ZONE 'UTC')", "title='changed'"), parameters), mock(Connection.class), null))
                .hasMessage("BULK_DELETE_SHAPE_REQUIRED");
    }

    private void check(String sql, SqlCommandType command) {
        var properties = new EgonColaMybatisPlusProperties();
        tenant.set(41L);
        var guard = new EgonColaLocalWriteGuardInnerInterceptor(query -> new EgonColaRouteResult(
                List.of(new EgonColaPhysicalTargetBO("primary", "public", query.logicalTable())), "a".repeat(64)),
                new EgonColaTenantIdGuardInnerInterceptor(() -> "tester", properties),
                mock(top.egon.cola.component.common.mybatis.routing.EgonColaTwoLevelRouteStrategy.class), Map.of());
        var configuration = EgonColaLocalWriteGuardTest.configuration(mock(DataSource.class));
        guard.beforePrepare(EgonColaLocalWriteGuardTest.handler(configuration, "test.Mapper.custom", command, sql,
                EgonColaLocalWriteGuardTest.parameters()), mock(Connection.class), null);
    }
}
