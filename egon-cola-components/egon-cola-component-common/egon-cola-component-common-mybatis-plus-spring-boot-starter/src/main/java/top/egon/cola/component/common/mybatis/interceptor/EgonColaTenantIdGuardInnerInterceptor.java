package top.egon.cola.component.common.mybatis.interceptor;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.LongValue;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static top.egon.cola.component.common.mybatis.interceptor.EgonColaOriginalSqlGuardInterceptor.*;

/** Validates explicit scope before rewriting and proves complete scope at final prepare. */
@Slf4j
@RequiredArgsConstructor
public final class EgonColaTenantIdGuardInnerInterceptor implements InnerInterceptor {

    @Qualifier("egonColaMdcTenantIdProvider")
    private final EgonColaTenantIdProvider tenantIdProvider;
    @Qualifier("egonColaMdcUserIdProvider")
    private final EgonColaUserIdProvider userIdProvider;
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    @Override
    public void beforeQuery(Executor executor, MappedStatement statement, Object parameter, RowBounds bounds,
                            ResultHandler handler, BoundSql sql) {
        SqlShapeBO shape = shape(sql.getSql());
        com.baomidou.mybatisplus.core.toolkit.ParameterUtils.findPage(parameter).ifPresent(page -> {
            if (page.getSize() < 1 || page.getSize() > properties.getPagination().getMaxPageSize()) {
                throw new IllegalStateException("PAGE_SIZE_INVALID");
            }
        });
        if (allIgnored(shape, properties)) { return; }
        Long tenant = requireTenant();
        validateExplicitTenants(shape, sql, statement.getConfiguration(), tenant);
        for (TableScopeBO scope : shape.scopes()) {
            if (!ignored(scope) && !active(scope.isolation(), scope)) { throw new IllegalStateException("ACTIVE_PREDICATE_REQUIRED"); }
        }
    }

    @Override
    public void beforePrepare(StatementHandler handler, Connection connection, Integer timeout) {
        var wrapped = PluginUtils.mpStatementHandler(handler);
        BoundSql sql = handler.getBoundSql();
        SqlShapeBO shape = shape(sql.getSql());
        if (allIgnored(shape, properties)) { return; }
        Long tenant = requireTenant();
        validateExplicitTenants(shape, sql, wrapped.configuration(), tenant);
        if (shape.statement() instanceof Update update) {
            removeAuthoritativeTenantAssignment(update, wrapped.mappedStatement(), sql, wrapped.mPBoundSql());
            validateProtectedAssignments(update, wrapped.mappedStatement(), sql, wrapped.configuration());
        }
    }

    List<ScopedRouteQuery> validateFinal(MappedStatement statement, BoundSql sql, Map<String, EgonColaRoutingProfileBO> profiles) {
        SqlShapeBO shape = shape(sql.getSql());
        if (allIgnored(shape, properties)) { return List.of(); }
        Long tenant = requireTenant();
        boolean query = statement.getSqlCommandType() == SqlCommandType.SELECT;
        if (shape.statement() instanceof net.sf.jsqlparser.statement.delete.Delete) {
            throw new IllegalStateException("PHYSICAL_DELETE_FORBIDDEN");
        }
        boolean insert = shape.statement() instanceof Insert;
        if (insert != (statement.getSqlCommandType() == SqlCommandType.INSERT)
                || query != (shape.statement() instanceof net.sf.jsqlparser.statement.select.Select)) {
            throw new IllegalStateException("SQL_COMMAND_TYPE_MISMATCH");
        }
        if (insert) { return insertRoutes((Insert) shape.statement(), shape, statement, sql, tenant, profiles); }
        List<ScopedRouteQuery> routes = new ArrayList<>();
        for (TableScopeBO scope : shape.scopes()) {
            if (ignored(scope)) { continue; }
            Set<Object> tenants = values(scope.isolation(), scope, "tenant_id", shape, sql, statement.getConfiguration(), false, new HashSet<>());
            if (tenants == null || tenants.size() != 1 || !tenants.contains(tenant)) { throw new IllegalStateException("TENANT_CONTEXT_MISMATCH"); }
            if (!active(scope.isolation(), scope)) { throw new IllegalStateException("ACTIVE_PREDICATE_REQUIRED"); }
            String table = logicalTable(scope.table().getName());
            EgonColaRoutingProfileBO profile = profiles.get(table);
            List<Long> roots = List.of();
            boolean range = false;
            if (profile != null && profile.secondaryColumn() != null) {
                Set<Object> keys = values(scope.predicate(), scope, profile.secondaryColumn(), shape, sql, statement.getConfiguration(), false, new HashSet<>());
                if (keys != null) { roots = longKeys(keys); }
                else { range = mentions(scope.predicate(), scope, profile.secondaryColumn()); }
            }
            boolean commandRead = query && profile != null && profile.secondaryColumn() != null && technicalRead(statement.getId());
            EgonColaRouteQuery.OperationEnum operation = query && !commandRead
                    ? EgonColaRouteQuery.OperationEnum.QUERY : EgonColaRouteQuery.OperationEnum.COMMAND;
            if (query && profile != null && profile.secondaryColumn() != null && roots.isEmpty()
                    && (statement.getResource() == null || !statement.getResource().contains(".xml"))) {
                throw new IllegalStateException("REGISTERED_QUERY_SQL_REQUIRED");
            }
            // Only the actual DML target is a write; subqueries remain independently scoped reads.
            if (query || scope == shape.scopes().getFirst()) {
                routes.add(new ScopedRouteQuery(new EgonColaRouteQuery(table, tenant, roots, operation, range),
                        scope.table().getSchemaName() == null ? null : identifier(scope.table().getSchemaName())));
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

    private List<ScopedRouteQuery> insertRoutes(Insert insert, SqlShapeBO shape, MappedStatement statement,
                                                 BoundSql sql, Long tenant, Map<String, EgonColaRoutingProfileBO> profiles) {
        if (insert.getColumns() == null) { throw new IllegalStateException("INSERT_COLUMNS_REQUIRED"); }
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
                if (!(expression instanceof ExpressionList<?> row)) { throw new IllegalStateException("SQL_SHAPE_UNSUPPORTED"); }
                rows.add(row);
            }
        } else { rows.add(expressions); }
        if (rows.isEmpty() || rows.size() > properties.getBatch().getMaxCollectionSize()) { throw new IllegalStateException("BATCH_COLLECTION_SIZE_INVALID"); }
        String user = requireUser();
        Set<Long> roots = new LinkedHashSet<>();
        for (ExpressionList<?> row : rows) {
            if (row.size() != columns.size()) { throw new IllegalStateException("INSERT_COLUMNS_REQUIRED"); }
            Map<String, Object> values = new HashMap<>();
            for (int i = 0; i < columns.size(); i++) {
                values.put(columns.get(i), value((Expression) row.get(i), sql, statement.getConfiguration()));
            }
            if (!tenant.equals(values.get("tenant_id"))) { throw new IllegalStateException("TENANT_CONTEXT_MISMATCH"); }
            if (!(values.get("id") instanceof Long id) || id <= 0 || !Objects.equals(0L, values.get("version"))
                    || values.get("deleted_at") != null) { throw new IllegalStateException("INSERT_MODEL_STATE_INVALID"); }
            if (!user.equals(values.get("create_user_id")) || !user.equals(values.get("update_user_id"))
                    || !(values.get("create_time") instanceof Instant) || !(values.get("update_time") instanceof Instant)) {
                throw new IllegalStateException("AUDIT_FIELDS_INVALID");
            }
            if (profile != null && profile.secondaryColumn() != null) {
                Object key = values.get(profile.secondaryColumn());
                if (!(key instanceof Long root) || root <= 0) { throw new IllegalStateException("SHARDING_KEY_REQUIRED"); }
                roots.add(root);
            }
        }
        return List.of(new ScopedRouteQuery(new EgonColaRouteQuery(table, tenant, List.copyOf(roots), EgonColaRouteQuery.OperationEnum.COMMAND, false),
                insert.getTable().getSchemaName() == null ? null : identifier(insert.getTable().getSchemaName())));
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
        if (!increment) { throw new IllegalStateException("VERSION_INCREMENT_REQUIRED"); }
        String user = requireUser();
        if (!user.equals(value(assigned(update, "update_user_id"), sql, statement.getConfiguration()))
                || !(value(assigned(update, "update_time"), sql, statement.getConfiguration()) instanceof Instant)) {
            throw new IllegalStateException("AUDIT_FIELDS_INVALID");
        }
        if (bulkRoot != null) {
            Set<String> permitted = Set.of("deleted_at", "version", "update_user_id", "update_time");
            if (assigned(update, "deleted_at") == null || update.getUpdateSets().stream().flatMap(set -> set.getColumns().stream())
                    .anyMatch(column -> !permitted.contains(identifier(column.getColumnName())))) {
                throw new IllegalStateException("BULK_DELETE_SHAPE_REQUIRED");
            }
            Set<Object> roots = values(update.getWhere(), scope, bulkRoot, shape, sql, statement.getConfiguration(), true, new HashSet<>());
            if (roots == null || roots.isEmpty()) { throw new IllegalStateException("BUSINESS_PREDICATE_REQUIRED"); }
            longKeys(roots);
        }
    }

    private static boolean increment(Expression expression, TableScopeBO scope) {
        return unwrap(expression) instanceof Addition addition && matches(addition.getLeftExpression(), scope, "version")
                && unwrap(addition.getRightExpression()) instanceof LongValue value && value.getValue() == 1;
    }

    private void validateProtectedAssignments(Update update, MappedStatement statement, BoundSql sql, Configuration configuration) {
        if (assigned(update, "tenant_id") != null) { throw new IllegalStateException("TENANT_COLUMN_MUTATION_FORBIDDEN"); }
        for (String field : List.of("id", "create_user_id", "create_time")) {
            if (assigned(update, field) != null) { throw new IllegalStateException("TECHNICAL_COLUMN_MUTATION_FORBIDDEN"); }
        }
        Expression deletedAt = assigned(update, "deleted_at");
        if (deletedAt != null) {
            if (!statement.getId().endsWith(".deleteVersionedById")
                    && !properties.getLocalWriteGuard().getAllowedRootStatements().containsKey(statement.getId())) {
                throw new IllegalStateException("LOGIC_DELETE_COLUMN_MUTATION_FORBIDDEN");
            }
            String expression = deletedAt.toString().replace("(", "").replace(")", "").replaceAll("\\s+", " ").trim();
            if (!"CURRENT_TIMESTAMP AT TIME ZONE 'UTC'".equalsIgnoreCase(expression)) {
                throw new IllegalStateException("LOGIC_DELETE_TIMESTAMP_REQUIRED");
            }
        }
    }

    private static Expression assigned(Update update, String name) {
        Expression found = null;
        for (UpdateSet set : update.getUpdateSets()) {
            for (int i = 0; i < set.getColumns().size(); i++) {
                if (identifier(set.getColumn(i).getColumnName()).equals(name)) {
                    if (found != null) { throw new IllegalStateException("DUPLICATE_COLUMN_ASSIGNMENT"); }
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
                if (set.getColumns().size() != 1 || !"et.tenantId".equals(property(set.getValue(0), sql))
                        || !(sql.getParameterObject() instanceof Map<?, ?> parameters)
                        || !parameters.containsKey("et") || !(parameters.get("et") instanceof top.egon.cola.component.common.mybatis.model.EgonModel<?>)
                        || !(statement.getId().endsWith(".updateById") || statement.getId().endsWith(".update"))
                        || !removed.isEmpty()) { throw new IllegalStateException("TENANT_COLUMN_MUTATION_FORBIDDEN"); }
                removed.add(((JdbcParameter) unwrap(set.getValue(0))).getIndex() - 1);
            } else { sets.add(set); }
        }
        if (!removed.isEmpty()) {
            update.setUpdateSets(sets);
            List<ParameterMapping> mappings = new ArrayList<>();
            for (int i = 0; i < sql.getParameterMappings().size(); i++) {
                if (!removed.contains(i)) { mappings.add(sql.getParameterMappings().get(i)); }
            }
            wrapped.sql(update.toString());
            wrapped.parameterMappings(mappings);
        }
    }

    private void validateExplicitTenants(SqlShapeBO shape, BoundSql sql, Configuration configuration, Long tenant) {
        for (TableScopeBO scope : shape.scopes()) {
            if (ignored(scope)) { continue; }
            Expression predicate = scope.predicate();
            if (predicate == null) { continue; }
            predicate.accept(new ExpressionVisitorAdapter<Void>() {
                @Override
                public <S> Void visit(EqualsTo equal, S context) {
                    Expression other = matches(equal.getLeftExpression(), scope, "tenant_id") ? equal.getRightExpression()
                            : matches(equal.getRightExpression(), scope, "tenant_id") ? equal.getLeftExpression() : null;
                    if (other != null && !(other instanceof Column && identifier(((Column) other).getColumnName()).equals("tenant_id"))
                            && !tenant.equals(value(other, sql, configuration))) { throw new IllegalStateException("TENANT_CONTEXT_MISMATCH"); }
                    return super.visit(equal, context);
                }
                @Override
                public <S> Void visit(InExpression in, S context) {
                    if (matches(in.getLeftExpression(), scope, "tenant_id")) {
                        Set<Object> found = values(in, scope, "tenant_id", shape, sql, configuration, false, new HashSet<>());
                        if (found == null || found.size() != 1 || !found.contains(tenant)) { throw new IllegalStateException("TENANT_CONTEXT_MISMATCH"); }
                    }
                    return super.visit(in, context);
                }
                @Override
                public <S> Void visit(Between between, S context) {
                    if (matches(between.getLeftExpression(), scope, "tenant_id")) { throw new IllegalStateException("TENANT_CONTEXT_MISMATCH"); }
                    return super.visit(between, context);
                }
                @Override
                public <S> Void visit(IsNullExpression isNull, S context) {
                    if (matches(isNull.getLeftExpression(), scope, "tenant_id")) { throw new IllegalStateException("TENANT_CONTEXT_MISMATCH"); }
                    return super.visit(isNull, context);
                }
                @Override
                protected <S> Void visitBinaryExpression(BinaryExpression expression, S context) {
                    if (!(expression instanceof EqualsTo)
                            && (matches(expression.getLeftExpression(), scope, "tenant_id") || matches(expression.getRightExpression(), scope, "tenant_id"))) {
                        throw new IllegalStateException("TENANT_CONTEXT_MISMATCH");
                    }
                    return super.visitBinaryExpression(expression, context);
                }
            }, null);
        }
    }

    record ScopedRouteQuery(EgonColaRouteQuery query, String schema) {}

    private boolean ignored(TableScopeBO scope) { return properties.getTenantId().ignores(identifier(scope.table().getName())); }

    private String logicalTable(String physical) {
        String table = identifier(physical);
        if (!properties.getDynamicTableName().isEnabled()) { return table; }
        for (Map.Entry<String, String> mapping : properties.getDynamicTableName().getTables().entrySet()) {
            if (mapping.getValue().equals(table)) { return mapping.getKey(); }
        }
        return table;
    }

    private static boolean technicalRead(String id) {
        return id.endsWith(".selectActiveById") || id.endsWith(".selectActiveByIds");
    }

    private static List<Long> longKeys(Set<Object> keys) {
        List<Long> values = new ArrayList<>();
        for (Object key : keys) {
            if (!(key instanceof Long value) || value <= 0) { throw new IllegalStateException("SHARDING_KEY_REQUIRED"); }
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
                    if (matches(candidate, scope, column)) { found[0] = true; }
                    return null;
                }
            }, null);
        }
        return found[0];
    }

    private Long requireTenant() {
        Long tenant = tenantIdProvider.currentTenantId();
        if (tenant == null) { throw new IllegalStateException("TENANT_CONTEXT_MISSING"); }
        return tenant;
    }

    private String requireUser() {
        String user = userIdProvider.currentUserId();
        if (user == null || user.isBlank()) { throw new IllegalStateException("USER_CONTEXT_MISSING"); }
        return user;
    }
}
