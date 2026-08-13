package top.egon.cola.platform.rbac3.admin.role.repository.jdbc;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.stereotype.Repository;

/**
 * 类型 `PostgresqlRoleClosureRepository` 位于当前包内，是类型，用于承载 `Postgresql Role Closure Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `PostgresqlRoleClosureRepository` is a type in its package and carries the responsibility, state, or contract for `Postgresql Role Closure Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Uses a transaction-scoped PostgreSQL advisory lock and deterministic closure rebuild.
 */
@Repository
public class PostgresqlRoleClosureRepository {

    /**
     * 字段 `jdbcTemplate` 表示 `PostgresqlRoleClosureRepository` 中与 `jdbc Template` 相关的状态、依赖、配置或结果（声明类型 `JdbcTemplate`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `jdbcTemplate` stores the `jdbc Template`-related state, dependency, configuration, or result of `PostgresqlRoleClosureRepository` (declared type `JdbcTemplate`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `jdbcTemplate` 时应保持 `PostgresqlRoleClosureRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `jdbcTemplate`, preserve `PostgresqlRoleClosureRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final JdbcTemplate jdbcTemplate;

    /**
     * 构造器 `PostgresqlRoleClosureRepository` 用于创建并初始化 `PostgresqlRoleClosureRepository` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `PostgresqlRoleClosureRepository` creates and initializes `PostgresqlRoleClosureRepository`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `PostgresqlRoleClosureRepository` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `PostgresqlRoleClosureRepository`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param jdbcTemplate 输入参数 `jdbcTemplate`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public PostgresqlRoleClosureRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 方法 `lockGraph` 按照 `PostgresqlRoleClosureRepository` 的职责处理输入，完成 `lock Graph` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `lockGraph` processes its inputs according to `PostgresqlRoleClosureRepository`'s responsibility, performs the `lock Graph` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `lockGraph` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `lockGraph`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void lockGraph(long tenantId, long applicationId) {
        jdbcTemplate.execute(
                "select pg_advisory_xact_lock(hashtextextended(?, 0))",
                (PreparedStatementCallback<Void>) statement -> {
                    statement.setString(1, tenantId + ":" + applicationId);
                    statement.execute();
                    return null;
                });
    }

    /**
     * 方法 `rebuild` 按照 `PostgresqlRoleClosureRepository` 的职责处理输入，完成 `rebuild` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rebuild` processes its inputs according to `PostgresqlRoleClosureRepository`'s responsibility, performs the `rebuild` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rebuild` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rebuild`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
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
