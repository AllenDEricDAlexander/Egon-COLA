package top.egon.cola.component.common.mybatis.interceptor;

import static org.assertj.core.api.Assertions.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties.LifecyclePolicyBO;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties.LifecycleScopeEnum;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;

/** SQL scope proof only; database execution is covered separately by lifecycle contract tests. */
class EgonColaLifecyclePolicyTest {
    private static final String ID = "test.LifecycleMapper.operation";
    private EgonColaMybatisPlusProperties properties;

    @BeforeEach
    void tenant() {
        EgonColaTenantIdProvider.initialize("tenantId");
        MDC.put("tenantId", "41");
        properties = new EgonColaMybatisPlusProperties();
        properties.getPagination().setMaxPageSize(100);
    }

    @AfterEach
    void clear() {
        MDC.clear();
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    private EgonColaTenantIdGuardInnerInterceptor guard(LifecycleScopeEnum mode) {
        return guard(mode, ID);
    }

    private EgonColaTenantIdGuardInnerInterceptor guard(LifecycleScopeEnum mode, String id) {
        properties.getLifecycle().getStatementPolicies().put(id, LifecyclePolicyBO.builder().scope(mode).table("item")
                .keyColumn(mode == LifecycleScopeEnum.PARENT || mode == LifecycleScopeEnum.ALL_PARENT
                        || mode == LifecycleScopeEnum.PURGE_PARENT ? "parent_id" : "id")
                .parentTable("parent_item").build());
        EgonColaTenantIdGuardInnerInterceptor guard = new EgonColaTenantIdGuardInnerInterceptor(() -> "actor", properties);
        guard.snapshotLifecycleStatements();
        return guard;
    }

    @Test
    void historyRequiresExactPolicyAndBoundKeyAndDoesNotEnableRuntimeMutations() {
        var guard = guard(LifecycleScopeEnum.ID);
        assertThat(prove(guard, SqlCommandType.SELECT, "SELECT * FROM item WHERE tenant_id=? AND id=? AND deleted_at IS NOT NULL", "tenant", 41L, "id", 1L)).hasSize(1);
        assertThat(prove(guard, SqlCommandType.SELECT, "SELECT * FROM item WHERE tenant_id=? AND id=? AND deleted_at IS NULL", "tenant", 41L, "id", 1L)).hasSize(1);
        assertThatThrownBy(() -> prove(guard, SqlCommandType.SELECT, "SELECT * FROM item WHERE tenant_id=? AND deleted_at IS NOT NULL", "tenant", 41L)).hasMessage("LIFECYCLE_KEY_REQUIRED");
        assertThatThrownBy(() -> prove(guard, SqlCommandType.SELECT, "SELECT * FROM item WHERE tenant_id=? AND id=? AND deleted_at IS NOT NULL", "tenant", 42L, "id", 1L)).hasMessage("TENANT_CONTEXT_MISMATCH");
        var closed = new EgonColaTenantIdGuardInnerInterceptor(() -> "actor", properties);
        properties.getLifecycle().getStatementPolicies().clear();
        closed.snapshotLifecycleStatements();
        properties.getLifecycle().getStatementPolicies().put(ID, LifecyclePolicyBO.builder().scope(LifecycleScopeEnum.ID).table("item").build());
        assertThatThrownBy(() -> prove(closed, SqlCommandType.SELECT, "SELECT * FROM item WHERE tenant_id=? AND id=? AND deleted_at IS NOT NULL", "tenant", 41L, "id", 1L)).hasMessage("ACTIVE_PREDICATE_REQUIRED");
    }

    @Test
    void parentHistoryIsBoundToOnePositiveParentAndTable() {
        var guard = guard(LifecycleScopeEnum.ALL_PARENT);
        assertThat(prove(guard, SqlCommandType.SELECT, "SELECT * FROM item WHERE tenant_id=? AND parent_id=?", "tenant", 41L, "parent", 9L)).hasSize(1);
        assertThatThrownBy(() -> prove(guard, SqlCommandType.SELECT, "SELECT * FROM item WHERE tenant_id=? AND parent_id=?", "tenant", 41L, "parent", 0L)).hasMessage("LIFECYCLE_KEY_REQUIRED");
        assertThatThrownBy(() -> prove(guard, SqlCommandType.SELECT, "SELECT * FROM other_item WHERE tenant_id=? AND parent_id=?", "tenant", 41L, "parent", 9L)).hasMessage("LIFECYCLE_TABLE_MISMATCH");
    }

    @Test
    void pagesRequireBoundedSqlLimitAndCountRequiresBoundedCallerPage() {
        var guard = guard(LifecycleScopeEnum.PAGE);
        assertThat(prove(guard, SqlCommandType.SELECT, "SELECT * FROM item WHERE tenant_id=? AND deleted_at IS NOT NULL ORDER BY id DESC LIMIT ?", "tenant", 41L, "limit", 10L)).hasSize(1);
        assertThatThrownBy(() -> prove(guard, SqlCommandType.SELECT, "SELECT * FROM item WHERE tenant_id=? AND deleted_at IS NOT NULL", "tenant", 41L)).hasMessage("PAGE_SIZE_INVALID");
        assertThatThrownBy(() -> prove(guard, SqlCommandType.SELECT, "SELECT * FROM item WHERE tenant_id=? AND deleted_at IS NOT NULL LIMIT ?", "tenant", 41L, "limit", 101L)).hasMessage("PAGE_SIZE_INVALID");
        var countGuard = guard(LifecycleScopeEnum.COUNT);
        assertThat(prove(countGuard, SqlCommandType.SELECT, "SELECT COUNT(*) FROM item WHERE tenant_id=? AND deleted_at IS NOT NULL", "tenant", 41L, "query", Map.of("page", Map.of("pageSize", 10)))).hasSize(1);
    }

    @Test
    void purgeRequiresTransactionTombstoneAndVersion() {
        var guard = guard(LifecycleScopeEnum.PURGE_ID);
        String sql = "DELETE FROM item WHERE tenant_id=? AND id=? AND deleted_at=? AND version=?";
        Object[] params = {"tenant", 41L, "id", 1L, "tombstone", LocalDateTime.of(2026, 1, 1, 0, 0), "version", 3L};
        assertThatThrownBy(() -> prove(guard, SqlCommandType.DELETE, sql, params)).hasMessage("PURGE_TRANSACTION_REQUIRED");
        TransactionSynchronizationManager.setActualTransactionActive(true);
        assertThat(prove(guard, SqlCommandType.DELETE, sql, params)).hasSize(1);
        assertThatThrownBy(() -> prove(guard, SqlCommandType.DELETE, "DELETE FROM item WHERE tenant_id=? AND id=? AND version=?", "tenant", 41L, "id", 1L, "version", 3L)).hasMessage("LIFECYCLE_RESTORE_PREDICATE_REQUIRED");
    }

    @Test
    void childPurgeRequiresAnExistingRecycledParentWithSameBoundKey() {
        var guard = guard(LifecycleScopeEnum.PURGE_PARENT);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        String sql = "DELETE FROM item WHERE tenant_id=? AND parent_id=? AND EXISTS (SELECT 1 FROM parent_item p WHERE p.tenant_id=? AND p.id=? AND p.deleted_at=? AND p.version=? FOR UPDATE)";
        assertThat(prove(guard, SqlCommandType.DELETE, sql, "tenant", 41L, "parent", 9L, "parentTenant", 41L, "parentId", 9L, "tombstone", LocalDateTime.of(2026, 1, 1, 0, 0), "version", 3L)).hasSize(2);
        assertThatThrownBy(() -> prove(guard, SqlCommandType.DELETE, sql, "tenant", 41L, "parent", 9L, "parentTenant", 41L, "parentId", 10L, "tombstone", LocalDateTime.of(2026, 1, 1, 0, 0), "version", 3L)).hasMessage("PURGE_PARENT_MISMATCH");
    }

    @Test
    void monotoneTimestampOnlyAcceptsTheExactSelfScopedUtcFormula() {
        String id = "test.LifecycleMapper.deleteVersionedById";
        var guard = guard(LifecycleScopeEnum.MONOTONE_DELETE, id);
        String formula = "GREATEST((CURRENT_TIMESTAMP AT TIME ZONE 'UTC'), COALESCE((SELECT MAX(history.deleted_at) + INTERVAL '0.000001' SECOND FROM item history WHERE history.tenant_id = ? AND history.sn = ?), (CURRENT_TIMESTAMP AT TIME ZONE 'UTC')))";
        String sql = "UPDATE item SET deleted_at=" + formula + ", version=version+1, update_user_id=?, update_time=? WHERE tenant_id=? AND id=? AND version=? AND sn=? AND deleted_at IS NULL";
        Object[] params = {"historyTenant", 41L, "historySn", "sn", "actor", "actor", "time", Instant.parse("2026-01-01T00:00:00Z"), "tenant", 41L, "id", 1L, "version", 3L, "sn", "sn"};
        assertThat(proveWithId(id, guard, SqlCommandType.UPDATE, sql, params)).hasSize(1);
        assertThatThrownBy(() -> proveWithId(id, guard, SqlCommandType.UPDATE, sql.replace("0.000001", "1"), params)).hasMessage("LOGIC_DELETE_TIMESTAMP_REQUIRED");
        Object[] wrongTenant = params.clone();
        wrongTenant[1] = 42L;
        assertThatThrownBy(() -> proveWithId(id, guard, SqlCommandType.UPDATE, sql, wrongTenant)).hasMessage("LOGIC_DELETE_TIMESTAMP_REQUIRED");
    }

    @Test
    void restorationOnlyClearsTheDeclaredFlagAndRequiresOriginalTombstone() {
        String id = "test.LifecycleMapper.restoreVersionedById";
        properties.getLifecycle().setRestoreStatements(Set.of(id));
        var initial = guard(LifecycleScopeEnum.RESTORE, id);
        properties.getLifecycle().getStatementPolicies().get(id).setRestoreNullColumns(Set.of("deletion_kind"));
        var guard = new EgonColaTenantIdGuardInnerInterceptor(() -> "actor", properties);
        guard.snapshotLifecycleStatements();
        String sql = "UPDATE item SET deleted_at=NULL, deletion_kind=NULL, version=version+1, update_user_id=?, update_time=? WHERE tenant_id=? AND id=? AND version=? AND deleted_at=?";
        Object[] params = {"actor", "actor", "time", Instant.parse("2026-01-01T00:00:00Z"), "tenant", 41L, "id", 1L, "version", 3L, "tombstone", LocalDateTime.of(2026, 1, 1, 0, 0)};
        assertThat(proveWithId(id, guard, SqlCommandType.UPDATE, sql, params)).hasSize(1);
        assertThatThrownBy(() -> proveWithId(id, initial, SqlCommandType.UPDATE, sql, params)).hasMessage("LIFECYCLE_RESTORE_SHAPE_REQUIRED");
        assertThatThrownBy(() -> proveWithId(id, guard, SqlCommandType.UPDATE, sql.replace("deletion_kind=NULL", "deletion_kind='P'"), params)).hasMessage("LIFECYCLE_RESTORE_SHAPE_REQUIRED");
        assertThatThrownBy(() -> proveWithId(id, guard, SqlCommandType.UPDATE, sql.replace("deletion_kind=NULL", "name=NULL"), params)).hasMessage("LIFECYCLE_RESTORE_SHAPE_REQUIRED");
    }

    private static java.util.List<?> prove(EgonColaTenantIdGuardInnerInterceptor guard, SqlCommandType command, String text, Object... parameters) {
        return proveWithId(ID, guard, command, text, parameters);
    }

    private static java.util.List<?> proveWithId(String id, EgonColaTenantIdGuardInnerInterceptor guard, SqlCommandType command, String text, Object... parameters) {
        Configuration configuration = new Configuration();
        java.util.List<ParameterMapping> mappings = new ArrayList<>();
        Map<String, Object> values = new LinkedHashMap<>();
        int bindCount = (int) text.chars().filter(value -> value == '?').count();
        for (int i = 0; i < parameters.length; i += 2) {
            String name = (String) parameters[i];
            Object value = parameters[i + 1];
            values.put(name, value);
            if (mappings.size() < bindCount) {
                mappings.add(new ParameterMapping.Builder(configuration, name, value.getClass()).build());
            }
        }
        var source = new StaticSqlSource(configuration, text, mappings);
        var statement = new MappedStatement.Builder(configuration, id, source, command).resource("policy.xml").build();
        return guard.validateFinal(statement, source.getBoundSql(values), Map.of());
    }
}
