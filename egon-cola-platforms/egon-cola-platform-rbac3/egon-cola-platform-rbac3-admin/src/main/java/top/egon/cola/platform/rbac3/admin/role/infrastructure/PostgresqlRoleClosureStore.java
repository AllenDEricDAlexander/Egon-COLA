package top.egon.cola.platform.rbac3.admin.role.infrastructure;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.stereotype.Repository;

/**
 * Uses a transaction-scoped PostgreSQL advisory lock and deterministic closure rebuild.
 */
@Repository
public class PostgresqlRoleClosureStore {

    private final JdbcTemplate jdbcTemplate;

    public PostgresqlRoleClosureStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void lockGraph(long tenantId, long applicationId) {
        jdbcTemplate.execute(
                "select pg_advisory_xact_lock(hashtextextended(?, 0))",
                (PreparedStatementCallback<Void>) statement -> {
                    statement.setString(1, tenantId + ":" + applicationId);
                    statement.execute();
                    return null;
                });
    }

    public void rebuild(long tenantId, long applicationId) {
        jdbcTemplate.update(
                "delete from rbac3_role_closure where tenant_id = ? and application_id = ?",
                tenantId,
                applicationId);
        jdbcTemplate.update("""
                with recursive paths(ancestor_role_id, descendant_role_id, depth) as (
                    select senior_role_id, junior_role_id, 1
                      from rbac3_role_inheritance
                     where tenant_id = ? and application_id = ?
                    union
                    select paths.ancestor_role_id, edges.junior_role_id, paths.depth + 1
                      from paths
                      join rbac3_role_inheritance edges
                        on edges.tenant_id = ?
                       and edges.application_id = ?
                       and edges.senior_role_id = paths.descendant_role_id
                     where paths.depth < 10
                ), closure_rows as (
                    select id as ancestor_role_id, id as descendant_role_id, 0 as depth
                      from rbac3_role
                     where tenant_id = ? and application_id = ?
                    union all
                    select ancestor_role_id, descendant_role_id, min(depth)
                      from paths
                     group by ancestor_role_id, descendant_role_id
                )
                insert into rbac3_role_closure (
                    tenant_id, application_id, ancestor_role_id, descendant_role_id, depth
                )
                select ?, ?, ancestor_role_id, descendant_role_id, min(depth)
                  from closure_rows
                 group by ancestor_role_id, descendant_role_id
                """,
                tenantId,
                applicationId,
                tenantId,
                applicationId,
                tenantId,
                applicationId,
                tenantId,
                applicationId);
    }
}
