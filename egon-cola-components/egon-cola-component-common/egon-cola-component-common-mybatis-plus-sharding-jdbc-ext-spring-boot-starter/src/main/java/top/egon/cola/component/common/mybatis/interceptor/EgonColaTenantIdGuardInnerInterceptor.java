package top.egon.cola.component.common.mybatis.interceptor;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.operators.arithmetic.Addition;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Values;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.update.UpdateSet;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.business.EgonColaUserIdProvider;
import top.egon.cola.component.common.mybatis.routing.EgonColaRouteQuery;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO;

import java.sql.Connection;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.function.BiConsumer;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties.LifecycleScopeEnum;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static top.egon.cola.component.common.mybatis.interceptor.EgonColaOriginalSqlGuardInterceptor.SqlShapeBO;
import static top.egon.cola.component.common.mybatis.interceptor.EgonColaOriginalSqlGuardInterceptor.TableScopeBO;
import static top.egon.cola.component.common.mybatis.interceptor.EgonColaOriginalSqlGuardInterceptor.active;
import static top.egon.cola.component.common.mybatis.interceptor.EgonColaOriginalSqlGuardInterceptor.allIgnored;
import static top.egon.cola.component.common.mybatis.interceptor.EgonColaOriginalSqlGuardInterceptor.identifier;
import static top.egon.cola.component.common.mybatis.interceptor.EgonColaOriginalSqlGuardInterceptor.matches;
import static top.egon.cola.component.common.mybatis.interceptor.EgonColaOriginalSqlGuardInterceptor.property;
import static top.egon.cola.component.common.mybatis.interceptor.EgonColaOriginalSqlGuardInterceptor.shape;
import static top.egon.cola.component.common.mybatis.interceptor.EgonColaOriginalSqlGuardInterceptor.unwrap;
import static top.egon.cola.component.common.mybatis.interceptor.EgonColaOriginalSqlGuardInterceptor.value;
import static top.egon.cola.component.common.mybatis.interceptor.EgonColaOriginalSqlGuardInterceptor.values;

/**
 * Validates explicit scope before rewriting and proves complete scope at final prepare.
 */
@Slf4j
@RequiredArgsConstructor
public final class EgonColaTenantIdGuardInnerInterceptor implements InnerInterceptor {

    @Qualifier("egonColaMdcUserIdProvider")
    private final EgonColaUserIdProvider userIdProvider;
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;
    private Set<String> historyReadStatements = Set.of();
    private Set<String> restoreStatements = Set.of();

    private Map<String, LifecyclePolicySnapshotBO> lifecyclePolicies = Map.of();

    // Strategy dispatch keeps each SQL capability independent of the default active-row policy.
    private final Map<LifecycleScopeEnum, BiConsumer<LifecyclePolicySnapshotBO, LifecycleSqlContextBO>> policyChecks = Map.of(
            LifecycleScopeEnum.ID, this::validateHistoryPolicy,
            LifecycleScopeEnum.PARENT, this::validateHistoryPolicy,
            LifecycleScopeEnum.ALL_PARENT, this::validateHistoryPolicy,
            LifecycleScopeEnum.PAGE, this::validatePagePolicy,
            LifecycleScopeEnum.COUNT, this::validatePagePolicy,
            LifecycleScopeEnum.MONOTONE_DELETE, (policy, context) -> { },
            LifecycleScopeEnum.RESTORE, (policy, context) -> { },
            LifecycleScopeEnum.PURGE_ID, this::validatePurgePolicy,
            LifecycleScopeEnum.PURGE_PARENT, this::validatePurgePolicy);

    @PostConstruct
    void snapshotLifecycleStatements() {
        historyReadStatements = Set.copyOf(properties.getLifecycle().getHistoryReadStatements());
        restoreStatements = Set.copyOf(properties.getLifecycle().getRestoreStatements());
        Map<String, LifecyclePolicySnapshotBO> policies = new HashMap<>();
        properties.getLifecycle().getStatementPolicies().forEach((id, value) -> policies.put(id,
                new LifecyclePolicySnapshotBO(value.getScope(), value.getTable(), value.getKeyColumn(),
                        value.getParentTable(), value.getBusinessKeyColumn(), value.getPageSizeParameter(),
                        Set.copyOf(value.getRestoreNullColumns()))));
        lifecyclePolicies = Map.copyOf(policies);
    }

    @Override
    public void beforeQuery(Executor executor, MappedStatement statement, Object parameter, RowBounds bounds, ResultHandler handler, BoundSql sql) {
        SqlShapeBO shape = shape(sql.getSql());
        com.baomidou.mybatisplus.core.toolkit.ParameterUtils.findPage(parameter).ifPresent(page -> {
            if (page.getSize() < 1 || page.getSize() > properties.getPagination().getMaxPageSize()) {
                throw new IllegalStateException("PAGE_SIZE_INVALID");
            }
        });
        if (allIgnored(shape, properties)) {
            return;
        }
        Long tenant = requireTenant();
        validateExplicitTenants(shape, sql, statement.getConfiguration(), tenant);
        boolean historyRead = historyRead(statement);
        LifecyclePolicySnapshotBO policy = validatePolicy(statement, shape, sql);
        for (TableScopeBO scope : shape.scopes()) {
            if (!ignored(scope)) {
                if (policy != null && historyPolicy(policy)) {
                    // Registered scope has already proved tenant, key and lifecycle bounds.
                } else if (historyRead) {
                    validateHistoricalRead(statement, shape, scope, sql);
                } else if (!active(scope.isolation(), scope)) {
                    throw new IllegalStateException("ACTIVE_PREDICATE_REQUIRED");
                }
            }
        }
    }

    @Override
    public void beforePrepare(StatementHandler handler, Connection connection, Integer timeout) {
        var wrapped = PluginUtils.mpStatementHandler(handler);
        BoundSql sql = handler.getBoundSql();
        SqlShapeBO shape = shape(sql.getSql());
        if (allIgnored(shape, properties)) {
            return;
        }
        Long tenant = requireTenant();
        validateExplicitTenants(shape, sql, wrapped.configuration(), tenant);
        if (wrapped.mappedStatement().getId() != null
                && wrapped.mappedStatement().getId().endsWith(".restoreVersionedById")
                && !lifecycleRestore(wrapped.mappedStatement())) {
            throw new IllegalStateException("LIFECYCLE_RESTORE_NOT_ALLOWED");
        }
        if (shape.statement() instanceof Update update) {
            removeAuthoritativeTenantAssignment(update, wrapped.mappedStatement(), sql, wrapped.mPBoundSql());
            validateProtectedAssignments(update, wrapped.mappedStatement(), sql, wrapped.configuration());
        }
    }

    List<ScopedRouteQuery> validateFinal(MappedStatement statement, BoundSql sql, Map<String, EgonColaRoutingProfileBO> profiles) {
        SqlShapeBO shape = shape(sql.getSql());
        if (allIgnored(shape, properties)) {
            return List.of();
        }
        Long tenant = requireTenant();
        boolean query = statement.getSqlCommandType() == SqlCommandType.SELECT;
        LifecyclePolicySnapshotBO policy = validatePolicy(statement, shape, sql);
        if (shape.statement() instanceof net.sf.jsqlparser.statement.delete.Delete && (policy == null || !purgePolicy(policy))) {
            throw new IllegalStateException("PHYSICAL_DELETE_FORBIDDEN");
        }
        boolean historyRead = historyRead(statement);
        boolean lifecycleRestore = lifecycleRestore(statement);
        if (statement.getId() != null && statement.getId().endsWith(".restoreVersionedById") && !lifecycleRestore) {
            throw new IllegalStateException("LIFECYCLE_RESTORE_NOT_ALLOWED");
        }
        if (historyRead && statement.getSqlCommandType() != SqlCommandType.SELECT) {
            throw new IllegalStateException("LIFECYCLE_HISTORY_STATEMENT_INVALID");
        }
        boolean insert = shape.statement() instanceof Insert;
        if (insert != (statement.getSqlCommandType() == SqlCommandType.INSERT) || query != (shape.statement() instanceof net.sf.jsqlparser.statement.select.Select)) {
            throw new IllegalStateException("SQL_COMMAND_TYPE_MISMATCH");
        }
        if (insert) {
            return insertRoutes((Insert) shape.statement(), shape, statement, sql, tenant, profiles);
        }
        List<ScopedRouteQuery> routes = new ArrayList<>();
        for (TableScopeBO scope : shape.scopes()) {
            if (ignored(scope)) {
                continue;
            }
            Set<Object> tenants = values(scope.isolation(), scope, "tenant_id", shape, sql, statement.getConfiguration(), false, new HashSet<>());
            if (tenants == null || tenants.size() != 1 || !tenants.contains(tenant)) {
                throw new IllegalStateException("TENANT_CONTEXT_MISMATCH");
            }
            if (policy != null && (historyPolicy(policy) || purgePolicy(policy))) {
                // The exact policy validates every scope, including the recycled parent proof.
            } else if (historyRead) {
                validateHistoricalRead(statement, shape, scope, sql);
            } else if (lifecycleRestore) {
                validateRestorePredicate(shape, scope, sql, statement.getConfiguration());
            } else if (!active(scope.isolation(), scope)) {
                throw new IllegalStateException("ACTIVE_PREDICATE_REQUIRED");
            }
            String table = logicalTable(scope.table().getName());
            EgonColaRoutingProfileBO profile = profiles.get(table);
            List<Long> roots = List.of();
            boolean range = false;
            if (profile != null && profile.secondaryColumn() != null) {
                Set<Object> keys = values(scope.predicate(), scope, profile.secondaryColumn(), shape, sql, statement.getConfiguration(), false, new HashSet<>());
                if (keys != null) {
                    roots = longKeys(keys);
                } else {
                    range = mentions(scope.predicate(), scope, profile.secondaryColumn());
                }
            }
            boolean commandRead = query && profile != null && profile.secondaryColumn() != null
                    && (technicalRead(statement.getId()) || historyRead);
            EgonColaRouteQuery.OperationEnum operation = query && !commandRead ? EgonColaRouteQuery.OperationEnum.QUERY : EgonColaRouteQuery.OperationEnum.COMMAND;
            if (query && profile != null && profile.secondaryColumn() != null && roots.isEmpty() && (statement.getResource() == null || !statement.getResource().contains(".xml"))) {
                throw new IllegalStateException("REGISTERED_QUERY_SQL_REQUIRED");
            }
            // Only the actual DML target is a write; subqueries remain independently scoped reads.
            if (query || scope == shape.scopes().getFirst() || policy != null && policy.scope() == LifecycleScopeEnum.PURGE_PARENT) {
                routes.add(new ScopedRouteQuery(new EgonColaRouteQuery(table, tenant, roots, operation, range), scope.table().getSchemaName() == null ? null : identifier(scope.table().getSchemaName())));
            }
        }
        if (shape.statement() instanceof Update update) {
            validateProtectedAssignments(update, statement, sql, statement.getConfiguration());
            validateVersionedUpdate(update, shape, statement, sql);
            EgonColaRoutingProfileBO profile = profiles.get(logicalTable(update.getTable().getName()));
            if (profile != null && profile.secondaryColumn() != null && assigned(update, profile.secondaryColumn()) != null) {
                throw new IllegalStateException("SHARDING_KEY_MUTATION_FORBIDDEN");
            }
        }
        return List.copyOf(routes);
    }

    private LifecyclePolicySnapshotBO validatePolicy(MappedStatement statement, SqlShapeBO shape, BoundSql sql) {
        LifecyclePolicySnapshotBO policy = lifecyclePolicies.get(statement.getId());
        if (policy == null) {
            return null;
        }
        TableScopeBO target = shape.scopes().getFirst();
        if (!logicalTable(target.table().getName()).equals(policy.table())) {
            throw new IllegalStateException("LIFECYCLE_TABLE_MISMATCH");
        }
        SqlCommandType expected = historyPolicy(policy) ? SqlCommandType.SELECT
                : purgePolicy(policy) ? SqlCommandType.DELETE : SqlCommandType.UPDATE;
        if (statement.getSqlCommandType() != expected
                || statement.getResource() == null || !statement.getResource().contains(".xml")) {
            throw new IllegalStateException("LIFECYCLE_STATEMENT_POLICY_INVALID");
        }
        for (TableScopeBO scope : shape.scopes()) {
            Set<Object> tenants = values(scope.isolation(), scope, "tenant_id", shape, sql,
                    statement.getConfiguration(), false, new HashSet<>());
            if (tenants == null || tenants.size() != 1 || !tenants.contains(requireTenant())) {
                throw new IllegalStateException("TENANT_CONTEXT_MISMATCH");
            }
        }
        if (policy.scope() != LifecycleScopeEnum.PURGE_PARENT && shape.scopes().size() != 1) {
            throw new IllegalStateException("LIFECYCLE_SCOPE_REQUIRED");
        }
        policyChecks.get(policy.scope()).accept(policy, new LifecycleSqlContextBO(statement, shape, sql));
        return policy;
    }

    private void validateHistoryPolicy(LifecyclePolicySnapshotBO policy, LifecycleSqlContextBO context) {
        TableScopeBO scope = context.shape().scopes().getFirst();
        requireKey(policy.keyColumn(), scope, context, "LIFECYCLE_KEY_REQUIRED");
        if (policy.scope() != LifecycleScopeEnum.ALL_PARENT && !active(scope.isolation(), scope)
                && !deletedAtNotNull(scope.isolation(), scope)) {
            throw new IllegalStateException("LIFECYCLE_HISTORY_PREDICATE_REQUIRED");
        }
    }

    private void validatePagePolicy(LifecyclePolicySnapshotBO policy, LifecycleSqlContextBO context) {
        TableScopeBO scope = context.shape().scopes().getFirst();
        if (!active(scope.isolation(), scope) && !deletedAtNotNull(scope.isolation(), scope)) {
            throw new IllegalStateException("LIFECYCLE_HISTORY_PREDICATE_REQUIRED");
        }
        if (!(scope.scopeId() instanceof net.sf.jsqlparser.statement.select.PlainSelect select)) {
            throw new IllegalStateException("LIFECYCLE_SCOPE_REQUIRED");
        }
        Object size;
        if (policy.scope() == LifecycleScopeEnum.PAGE) {
            size = select.getLimit() == null ? null
                    : value(select.getLimit().getRowCount(), context.sql(), context.statement().getConfiguration());
        } else {
            if (select.getSelectItems().size() != 1
                    || !select.getSelectItems().getFirst().getExpression().toString().equalsIgnoreCase("COUNT(*)")) {
                throw new IllegalStateException("LIFECYCLE_COUNT_SHAPE_REQUIRED");
            }
            var parameter = context.sql().getParameterObject();
            var meta = context.statement().getConfiguration().newMetaObject(parameter);
            size = meta.hasGetter(policy.pageSizeParameter()) ? meta.getValue(policy.pageSizeParameter()) : null;
        }
        if (!(size instanceof Integer || size instanceof Long) || ((Number) size).longValue() < 1
                || ((Number) size).longValue() > properties.getPagination().getMaxPageSize()) {
            throw new IllegalStateException("PAGE_SIZE_INVALID");
        }
    }

    private void validatePurgePolicy(LifecyclePolicySnapshotBO policy, LifecycleSqlContextBO context) {
        requireUser();
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("PURGE_TRANSACTION_REQUIRED");
        }
        TableScopeBO target = context.shape().scopes().getFirst();
        Long key = requireKey(policy.keyColumn(), target, context, "LIFECYCLE_KEY_REQUIRED");
        if (policy.scope() == LifecycleScopeEnum.PURGE_ID) {
            validateRestorePredicate(context.shape(), target, context.sql(), context.statement().getConfiguration());
        } else {
            if (context.shape().scopes().size() != 2) {
                throw new IllegalStateException("PURGE_PARENT_PROOF_REQUIRED");
            }
            TableScopeBO parent = context.shape().scopes().get(1);
            if (!(parent.scopeId() instanceof net.sf.jsqlparser.statement.select.PlainSelect parentSelect)
                    || parentSelect.getForMode() != net.sf.jsqlparser.statement.select.ForMode.UPDATE
                    || !logicalTable(parent.table().getName()).equals(policy.parentTable())
                    || !mandatoryExists(target.predicate(), parent.scopeId())) {
                throw new IllegalStateException("PURGE_PARENT_PROOF_REQUIRED");
            }
            validateRestorePredicate(context.shape(), parent, context.sql(), context.statement().getConfiguration());
            if (!key.equals(requireKey("id", parent, context, "PURGE_PARENT_MISMATCH"))) {
                throw new IllegalStateException("PURGE_PARENT_MISMATCH");
            }
        }
    }

    private static boolean mandatoryExists(Expression expression, net.sf.jsqlparser.statement.Statement parent) {
        Expression checked = unwrap(expression);
        if (checked instanceof net.sf.jsqlparser.expression.operators.conditional.AndExpression and) {
            return mandatoryExists(and.getLeftExpression(), parent) || mandatoryExists(and.getRightExpression(), parent);
        }
        if (checked instanceof net.sf.jsqlparser.expression.operators.conditional.OrExpression or) {
            return mandatoryExists(or.getLeftExpression(), parent) && mandatoryExists(or.getRightExpression(), parent);
        }
        if (!(checked instanceof net.sf.jsqlparser.expression.operators.relational.ExistsExpression exists) || exists.isNot()) {
            return false;
        }
        Expression right = exists.getRightExpression();
        return right instanceof net.sf.jsqlparser.statement.select.ParenthesedSelect nested && nested.getSelect() == parent;
    }

    private static Long requireKey(String column, TableScopeBO scope, LifecycleSqlContextBO context, String failure) {
        Set<Object> keys = values(scope.predicate(), scope, column, context.shape(), context.sql(),
                context.statement().getConfiguration(), false, new HashSet<>());
        if (keys == null || keys.size() != 1 || !(keys.iterator().next() instanceof Long id) || id <= 0
                || !hasEquality(scope.predicate(), scope, column, context.sql(), context.statement().getConfiguration())) {
            throw new IllegalStateException(failure);
        }
        return (Long) keys.iterator().next();
    }

    private boolean monotoneDeletion(Update update, MappedStatement statement, BoundSql sql, Expression expression) {
        LifecyclePolicySnapshotBO policy = lifecyclePolicies.get(statement.getId());
        if (policy == null || policy.scope() != LifecycleScopeEnum.MONOTONE_DELETE
                || !logicalTable(update.getTable().getName()).equals(policy.table())
                || !statement.getId().endsWith(".deleteVersionedById")) {
            return false;
        }
        String expected = "GREATEST((CURRENT_TIMESTAMP AT TIME ZONE 'UTC'), COALESCE((SELECT MAX(history.deleted_at) + INTERVAL '0.000001' SECOND FROM "
                + policy.table() + " history WHERE history.tenant_id = ? AND history." + policy.businessKeyColumn()
                + " = ?), (CURRENT_TIMESTAMP AT TIME ZONE 'UTC')))";
        try {
            String canonical = net.sf.jsqlparser.parser.CCJSqlParserUtil.parseExpression(expected).toString();
            if (!canonical.equalsIgnoreCase(expression.toString())) {
                return false;
            }
        } catch (net.sf.jsqlparser.JSQLParserException invalidPolicy) {
            throw new IllegalStateException("LIFECYCLE_STATEMENT_POLICY_INVALID", invalidPolicy);
        }
        // Check the two JDBC bindings independently; SQL shape alone is not tenant authority.
        List<Object> bindings = new ArrayList<>();
        expression.accept(new ExpressionVisitorAdapter<Void>() {
            @Override public <S> Void visit(net.sf.jsqlparser.statement.select.Select select, S context) {
                select.getPlainSelect().getWhere().accept(this, context);
                return null;
            }
            @Override public <S> Void visit(JdbcParameter parameter, S context) {
                bindings.add(value(parameter, sql, statement.getConfiguration()));
                return null;
            }
        });
        SqlShapeBO shape = shape(sql.getSql());
        TableScopeBO scope = shape.scopes().getFirst();
        Set<Object> keys = values(scope.predicate(), scope, policy.businessKeyColumn(), shape, sql,
                statement.getConfiguration(), false, new HashSet<>());
        return bindings.size() == 2 && requireTenant().equals(bindings.getFirst())
                && bindings.get(1) instanceof String key && !key.isBlank()
                && keys != null && keys.size() == 1 && keys.contains(key);
    }

    boolean requiresLifecycleTransaction(MappedStatement statement) {
        LifecyclePolicySnapshotBO policy = lifecyclePolicies.get(statement.getId());
        return policy != null && purgePolicy(policy);
    }

    private static boolean historyPolicy(LifecyclePolicySnapshotBO policy) {
        return Set.of(LifecycleScopeEnum.ID, LifecycleScopeEnum.PARENT, LifecycleScopeEnum.PAGE,
                LifecycleScopeEnum.COUNT, LifecycleScopeEnum.ALL_PARENT).contains(policy.scope());
    }

    private static boolean purgePolicy(LifecyclePolicySnapshotBO policy) {
        return policy.scope() == LifecycleScopeEnum.PURGE_ID || policy.scope() == LifecycleScopeEnum.PURGE_PARENT;
    }

    private record LifecyclePolicySnapshotBO(LifecycleScopeEnum scope, String table, String keyColumn,
            String parentTable, String businessKeyColumn, String pageSizeParameter, Set<String> restoreNullColumns) { }
    private record LifecycleSqlContextBO(MappedStatement statement, SqlShapeBO shape, BoundSql sql) { }

    private boolean historyRead(MappedStatement statement) {
        return statement.getSqlCommandType() == SqlCommandType.SELECT
                && historyReadStatements.contains(statement.getId());
    }

    private boolean lifecycleRestore(MappedStatement statement) {
        return statement.getSqlCommandType() == SqlCommandType.UPDATE
                && restoreStatements.contains(statement.getId());
    }

    private static void validateHistoricalRead(MappedStatement statement, SqlShapeBO shape,
                                               TableScopeBO scope, BoundSql sql) {
        if (statement.getSqlCommandType() != SqlCommandType.SELECT
                || !deletedAtNotNull(scope.isolation(), scope)) {
            throw new IllegalStateException("LIFECYCLE_HISTORY_PREDICATE_REQUIRED");
        }
        Set<Object> ids = values(scope.predicate(), scope, "id", shape, sql,
                statement.getConfiguration(), false, new HashSet<>());
        if (ids == null || ids.size() != 1 || !(ids.iterator().next() instanceof Long id) || id <= 0
                || !hasEquality(scope.predicate(), scope, "id", sql, statement.getConfiguration())) {
            throw new IllegalStateException("LIFECYCLE_HISTORY_ID_REQUIRED");
        }
    }

    private static boolean deletedAtNotNull(Expression expression, TableScopeBO scope) {
        Expression checked = unwrap(expression);
        if (checked instanceof net.sf.jsqlparser.expression.operators.conditional.AndExpression and) {
            return deletedAtNotNull(and.getLeftExpression(), scope)
                    || deletedAtNotNull(and.getRightExpression(), scope);
        }
        if (checked instanceof net.sf.jsqlparser.expression.operators.conditional.OrExpression or) {
            return deletedAtNotNull(or.getLeftExpression(), scope)
                    && deletedAtNotNull(or.getRightExpression(), scope);
        }
        return checked instanceof net.sf.jsqlparser.expression.operators.relational.IsNullExpression isNull
                && isNull.isNot() && matches(isNull.getLeftExpression(), scope, "deleted_at");
    }

    private static void validateRestorePredicate(SqlShapeBO shape, TableScopeBO scope,
                                                 BoundSql sql, Configuration configuration) {
        Set<Object> ids = values(scope.predicate(), scope, "id", shape, sql, configuration, false, new HashSet<>());
        Set<Object> versions = values(scope.predicate(), scope, "version", shape, sql, configuration, true, new HashSet<>());
        Set<Object> deletedAtValues = values(scope.predicate(), scope, "deleted_at", shape, sql, configuration, false, new HashSet<>());
        if (ids == null || ids.size() != 1 || !(ids.iterator().next() instanceof Long id) || id <= 0
                || versions == null || versions.size() != 1 || !(versions.iterator().next() instanceof Long version) || version < 0
                || deletedAtValues == null || deletedAtValues.size() != 1 || !(deletedAtValues.iterator().next() instanceof LocalDateTime)
                || !hasEquality(scope.predicate(), scope, "id", sql, configuration)
                || !hasEquality(scope.predicate(), scope, "version", sql, configuration)
                || !hasEquality(scope.predicate(), scope, "deleted_at", sql, configuration)) {
            throw new IllegalStateException("LIFECYCLE_RESTORE_PREDICATE_REQUIRED");
        }
    }

    private static boolean hasEquality(Expression expression, TableScopeBO scope, String column,
                                       BoundSql sql, Configuration configuration) {
        Expression checked = unwrap(expression);
        if (checked instanceof net.sf.jsqlparser.expression.operators.conditional.AndExpression and) {
            return hasEquality(and.getLeftExpression(), scope, column, sql, configuration)
                    || hasEquality(and.getRightExpression(), scope, column, sql, configuration);
        }
        if (checked instanceof net.sf.jsqlparser.expression.operators.conditional.OrExpression or) {
            return hasEquality(or.getLeftExpression(), scope, column, sql, configuration)
                    && hasEquality(or.getRightExpression(), scope, column, sql, configuration);
        }
        if (!(checked instanceof net.sf.jsqlparser.expression.operators.relational.EqualsTo equals)) {
            return false;
        }
        Expression valueExpression = matches(equals.getLeftExpression(), scope, column)
                ? equals.getRightExpression()
                : matches(equals.getRightExpression(), scope, column) ? equals.getLeftExpression() : null;
        return valueExpression != null && value(valueExpression, sql, configuration) != null;
    }

    private List<ScopedRouteQuery> insertRoutes(Insert insert, SqlShapeBO shape, MappedStatement statement, BoundSql sql, Long tenant, Map<String, EgonColaRoutingProfileBO> profiles) {
        if (insert.getColumns() == null) {
            throw new IllegalStateException("INSERT_COLUMNS_REQUIRED");
        }
        String table = logicalTable(insert.getTable().getName());
        EgonColaRoutingProfileBO profile = profiles.get(table);
        List<String> columns = insert.getColumns().stream().map(column -> identifier(column.getColumnName())).toList();
        Set<String> required = Set.of("id", "tenant_id", "create_user_id", "create_time", "update_user_id", "update_time", "deleted_at", "version");
        if (!new HashSet<>(columns).containsAll(required) || new HashSet<>(columns).size() != columns.size()) {
            throw new IllegalStateException("INSERT_TECHNICAL_COLUMNS_REQUIRED");
        }
        ExpressionList<?> expressions = ((Values) insert.getSelect()).getExpressions();
        List<ExpressionList<?>> rows = new ArrayList<>();
        if (!expressions.isEmpty() && expressions.getFirst() instanceof ParenthesedExpressionList<?>) {
            for (Object expression : expressions) {
                if (!(expression instanceof ExpressionList<?> row)) {
                    throw new IllegalStateException("SQL_SHAPE_UNSUPPORTED");
                }
                rows.add(row);
            }
        } else {
            rows.add(expressions);
        }
        if (rows.isEmpty() || rows.size() > properties.getBatch().getMaxCollectionSize()) {
            throw new IllegalStateException("BATCH_COLLECTION_SIZE_INVALID");
        }
        String user = requireUser();
        Set<Long> roots = new LinkedHashSet<>();
        for (ExpressionList<?> row : rows) {
            if (row.size() != columns.size()) {
                throw new IllegalStateException("INSERT_COLUMNS_REQUIRED");
            }
            Map<String, Object> values = new HashMap<>();
            for (int i = 0; i < columns.size(); i++) {
                values.put(columns.get(i), value((Expression) row.get(i), sql, statement.getConfiguration()));
            }
            if (!tenant.equals(values.get("tenant_id"))) {
                throw new IllegalStateException("TENANT_CONTEXT_MISMATCH");
            }
            if (!(values.get("id") instanceof Long id) || id <= 0 || !Objects.equals(0L, values.get("version")) || values.get("deleted_at") != null) {
                throw new IllegalStateException("INSERT_MODEL_STATE_INVALID");
            }
            if (!user.equals(values.get("create_user_id")) || !user.equals(values.get("update_user_id")) || !(values.get("create_time") instanceof Instant) || !(values.get("update_time") instanceof Instant)) {
                throw new IllegalStateException("AUDIT_FIELDS_INVALID");
            }
            if (profile != null && profile.secondaryColumn() != null) {
                Object key = values.get(profile.secondaryColumn());
                if (!(key instanceof Long root) || root <= 0) {
                    throw new IllegalStateException("SHARDING_KEY_REQUIRED");
                }
                roots.add(root);
            }
        }
        return List.of(new ScopedRouteQuery(new EgonColaRouteQuery(table, tenant, List.copyOf(roots), EgonColaRouteQuery.OperationEnum.COMMAND, false), insert.getTable().getSchemaName() == null ? null : identifier(insert.getTable().getSchemaName())));
    }

    private void validateVersionedUpdate(Update update, SqlShapeBO shape, MappedStatement statement, BoundSql sql) {
        TableScopeBO scope = shape.scopes().getFirst();
        String bulkRoot = properties.getLocalWriteGuard().getAllowedRootStatements().get(statement.getId());
        Set<Object> expected = values(update.getWhere(), scope, "version", shape, sql, statement.getConfiguration(), true, new HashSet<>());
        if (bulkRoot == null && (expected == null || expected.size() != 1 || !(expected.iterator().next() instanceof Long version) || version < 0)) {
            throw new IllegalStateException("EXPECTED_VERSION_REQUIRED");
        }
        Expression changedVersion = assigned(update, "version");
        boolean increment = increment(changedVersion, scope);
        if (!increment && expected != null && expected.size() == 1 && expected.iterator().next() instanceof Long version) {
            increment = version < Long.MAX_VALUE && Objects.equals(value(changedVersion, sql, statement.getConfiguration()), version + 1);
        }
        if (!increment) {
            throw new IllegalStateException("VERSION_INCREMENT_REQUIRED");
        }
        String user = requireUser();
        if (!user.equals(value(assigned(update, "update_user_id"), sql, statement.getConfiguration())) || !(value(assigned(update, "update_time"), sql, statement.getConfiguration()) instanceof Instant)) {
            throw new IllegalStateException("AUDIT_FIELDS_INVALID");
        }
        if (bulkRoot != null) {
            Set<String> permitted = Set.of("deleted_at", "version", "update_user_id", "update_time");
            if (assigned(update, "deleted_at") == null || update.getUpdateSets().stream().flatMap(set -> set.getColumns().stream()).anyMatch(column -> !permitted.contains(identifier(column.getColumnName())))) {
                throw new IllegalStateException("BULK_DELETE_SHAPE_REQUIRED");
            }
            Set<Object> roots = values(update.getWhere(), scope, bulkRoot, shape, sql, statement.getConfiguration(), true, new HashSet<>());
            if (roots == null || roots.isEmpty()) {
                throw new IllegalStateException("BUSINESS_PREDICATE_REQUIRED");
            }
            longKeys(roots);
        }
    }

    private static boolean increment(Expression expression, TableScopeBO scope) {
        return unwrap(expression) instanceof Addition addition && matches(addition.getLeftExpression(), scope, "version") && unwrap(addition.getRightExpression()) instanceof LongValue value && value.getValue() == 1;
    }

    private void validateProtectedAssignments(Update update, MappedStatement statement, BoundSql sql, Configuration configuration) {
        if (assigned(update, "tenant_id") != null) {
            throw new IllegalStateException("TENANT_COLUMN_MUTATION_FORBIDDEN");
        }
        for (String field : List.of("id", "create_user_id", "create_time")) {
            if (assigned(update, field) != null) {
                throw new IllegalStateException("TECHNICAL_COLUMN_MUTATION_FORBIDDEN");
            }
        }
        Expression deletedAt = assigned(update, "deleted_at");
        if (lifecycleRestore(statement)) {
            Set<String> permitted = new HashSet<>(Set.of("deleted_at", "version", "update_user_id", "update_time"));
            LifecyclePolicySnapshotBO policy = lifecyclePolicies.get(statement.getId());
            if (policy != null && policy.scope() == LifecycleScopeEnum.RESTORE) {
                if (!logicalTable(update.getTable().getName()).equals(policy.table())) {
                    throw new IllegalStateException("LIFECYCLE_TABLE_MISMATCH");
                }
                for (String column : policy.restoreNullColumns()) {
                    Expression assigned = assigned(update, column);
                    if (!(unwrap(assigned) instanceof NullValue)) {
                        throw new IllegalStateException("LIFECYCLE_RESTORE_SHAPE_REQUIRED");
                    }
                }
                permitted.addAll(policy.restoreNullColumns());
            }
            if (deletedAt == null || !(unwrap(deletedAt) instanceof NullValue)
                    || update.getUpdateSets().stream().flatMap(set -> set.getColumns().stream())
                    .anyMatch(column -> !permitted.contains(identifier(column.getColumnName())))) {
                throw new IllegalStateException("LIFECYCLE_RESTORE_SHAPE_REQUIRED");
            }
            return;
        }
        if (deletedAt != null) {
            if (!statement.getId().endsWith(".deleteVersionedById") && !properties.getLocalWriteGuard().getAllowedRootStatements().containsKey(statement.getId())) {
                throw new IllegalStateException("LOGIC_DELETE_COLUMN_MUTATION_FORBIDDEN");
            }
            String expression = deletedAt.toString().replace("(", "").replace(")", "").replaceAll("\\s+", " ").trim();
            if (!"CURRENT_TIMESTAMP AT TIME ZONE 'UTC'".equalsIgnoreCase(expression)
                    && !monotoneDeletion(update, statement, sql, deletedAt)) {
                throw new IllegalStateException("LOGIC_DELETE_TIMESTAMP_REQUIRED");
            }
        }
    }

    private static Expression assigned(Update update, String name) {
        Expression found = null;
        for (UpdateSet set : update.getUpdateSets()) {
            for (int i = 0; i < set.getColumns().size(); i++) {
                if (identifier(set.getColumn(i).getColumnName()).equals(name)) {
                    if (found != null) {
                        throw new IllegalStateException("DUPLICATE_COLUMN_ASSIGNMENT");
                    }
                    found = set.getValue(i);
                }
            }
        }
        return found;
    }

    private static void removeAuthoritativeTenantAssignment(Update update, MappedStatement statement, BoundSql sql, PluginUtils.MPBoundSql wrapped) {
        Set<Integer> removed = new HashSet<>();
        List<UpdateSet> sets = new ArrayList<>();
        for (UpdateSet set : update.getUpdateSets()) {
            if (set.getColumns().stream().anyMatch(column -> identifier(column.getColumnName()).equals("tenant_id"))) {
                if (set.getColumns().size() != 1 || !"et.tenantId".equals(property(set.getValue(0), sql)) || !(sql.getParameterObject() instanceof Map<?, ?> parameters) || !parameters.containsKey("et") || !(parameters.get("et") instanceof top.egon.cola.component.common.mybatis.model.EgonModel<?>) || !(statement.getId().endsWith(".updateById") || statement.getId().endsWith(".update")) || !removed.isEmpty()) {
                    throw new IllegalStateException("TENANT_COLUMN_MUTATION_FORBIDDEN");
                }
                removed.add(((JdbcParameter) unwrap(set.getValue(0))).getIndex() - 1);
            } else {
                sets.add(set);
            }
        }
        if (!removed.isEmpty()) {
            update.setUpdateSets(sets);
            List<ParameterMapping> mappings = new ArrayList<>();
            for (int i = 0; i < sql.getParameterMappings().size(); i++) {
                if (!removed.contains(i)) {
                    mappings.add(sql.getParameterMappings().get(i));
                }
            }
            wrapped.sql(update.toString());
            wrapped.parameterMappings(mappings);
        }
    }

    private void validateExplicitTenants(SqlShapeBO shape, BoundSql sql, Configuration configuration, Long tenant) {
        for (TableScopeBO scope : shape.scopes()) {
            if (ignored(scope)) {
                continue;
            }
            Expression predicate = scope.predicate();
            if (predicate == null) {
                continue;
            }
            predicate.accept(new ExpressionVisitorAdapter<Void>() {
                @Override
                public <S> Void visit(EqualsTo equal, S context) {
                    Expression other = matches(equal.getLeftExpression(), scope, "tenant_id") ? equal.getRightExpression() : matches(equal.getRightExpression(), scope, "tenant_id") ? equal.getLeftExpression() : null;
                    if (other != null && !(other instanceof Column && identifier(((Column) other).getColumnName()).equals("tenant_id")) && !tenant.equals(value(other, sql, configuration))) {
                        throw new IllegalStateException("TENANT_CONTEXT_MISMATCH");
                    }
                    return super.visit(equal, context);
                }

                @Override
                public <S> Void visit(InExpression in, S context) {
                    if (matches(in.getLeftExpression(), scope, "tenant_id")) {
                        Set<Object> found = values(in, scope, "tenant_id", shape, sql, configuration, false, new HashSet<>());
                        if (found == null || found.size() != 1 || !found.contains(tenant)) {
                            throw new IllegalStateException("TENANT_CONTEXT_MISMATCH");
                        }
                    }
                    return super.visit(in, context);
                }

                @Override
                public <S> Void visit(Between between, S context) {
                    if (matches(between.getLeftExpression(), scope, "tenant_id")) {
                        throw new IllegalStateException("TENANT_CONTEXT_MISMATCH");
                    }
                    return super.visit(between, context);
                }

                @Override
                public <S> Void visit(IsNullExpression isNull, S context) {
                    if (matches(isNull.getLeftExpression(), scope, "tenant_id")) {
                        throw new IllegalStateException("TENANT_CONTEXT_MISMATCH");
                    }
                    return super.visit(isNull, context);
                }

                @Override
                protected <S> Void visitBinaryExpression(BinaryExpression expression, S context) {
                    if (!(expression instanceof EqualsTo) && (matches(expression.getLeftExpression(), scope, "tenant_id") || matches(expression.getRightExpression(), scope, "tenant_id"))) {
                        throw new IllegalStateException("TENANT_CONTEXT_MISMATCH");
                    }
                    return super.visitBinaryExpression(expression, context);
                }
            }, null);
        }
    }

    record ScopedRouteQuery(EgonColaRouteQuery query, String schema) {
    }

    private boolean ignored(TableScopeBO scope) {
        return properties.getTenantId().ignores(identifier(scope.table().getName()));
    }

    private String logicalTable(String physical) {
        String table = identifier(physical);
        if (!properties.getDynamicTableName().isEnabled()) {
            return table;
        }
        for (Map.Entry<String, String> mapping : properties.getDynamicTableName().getTables().entrySet()) {
            if (mapping.getValue().equals(table)) {
                return mapping.getKey();
            }
        }
        return table;
    }

    private static boolean technicalRead(String id) {
        return id.endsWith(".selectActiveById") || id.endsWith(".selectActiveByIds");
    }

    private static List<Long> longKeys(Set<Object> keys) {
        List<Long> values = new ArrayList<>();
        for (Object key : keys) {
            if (!(key instanceof Long value) || value <= 0) {
                throw new IllegalStateException("SHARDING_KEY_REQUIRED");
            }
            values.add(value);
        }
        return values.stream().distinct().sorted().toList();
    }

    private static boolean mentions(Expression expression, TableScopeBO scope, String column) {
        boolean[] found = {false};
        if (expression != null) {
            expression.accept(new ExpressionVisitorAdapter<Void>() {
                @Override
                public <S> Void visit(Column candidate, S context) {
                    if (matches(candidate, scope, column)) {
                        found[0] = true;
                    }
                    return null;
                }
            }, null);
        }
        return found[0];
    }

    private Long requireTenant() {
        return EgonColaTenantIdProvider.currentTenantId();
    }

    private String requireUser() {
        String user = userIdProvider.currentUserId();
        if (user == null || user.isBlank()) {
            throw new IllegalStateException("USER_CONTEXT_MISSING");
        }
        return user;
    }
}
