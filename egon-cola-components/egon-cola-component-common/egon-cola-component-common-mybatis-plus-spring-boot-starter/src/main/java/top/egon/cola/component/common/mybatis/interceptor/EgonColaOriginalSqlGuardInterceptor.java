package top.egon.cola.component.common.mybatis.interceptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.SignedExpression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.Values;
import net.sf.jsqlparser.statement.select.WithItem;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.model.EgonModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Proves the original business key bound before MP adds tenant or optimistic-lock predicates.
 */
@Slf4j
@RequiredArgsConstructor
@Intercepts(@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}))
public final class EgonColaOriginalSqlGuardInterceptor implements Interceptor {

    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement statement = (MappedStatement) invocation.getArgs()[0];
        if (statement.getSqlCommandType() == SqlCommandType.UPDATE || statement.getSqlCommandType() == SqlCommandType.DELETE) {
            BoundSql sql = statement.getBoundSql(invocation.getArgs()[1]);
            SqlShapeBO shape = shape(sql.getSql());
            if (!allIgnored(shape, properties)) {
                TableScopeBO target = shape.scopes().getFirst();
                String root = properties.getLocalWriteGuard().getAllowedRootStatements().get(statement.getId());
                Set<Object> ids = values(target.predicate(), target, root == null ? "id" : root, shape, sql, statement.getConfiguration(), true, new HashSet<>());
                if (ids == null || ids.isEmpty() || ids.size() > properties.getBatch().getMaxCollectionSize() || ids.stream().anyMatch(value -> !(value instanceof Long id) || id <= 0)) {
                    throw new IllegalStateException("BUSINESS_PREDICATE_REQUIRED");
                }
                Object parameter = invocation.getArgs()[1];
                if (parameter instanceof Map<?, ?> parameters && parameters.containsKey("ew") && parameters.get("ew") instanceof com.baomidou.mybatisplus.core.conditions.AbstractWrapper<?, ?, ?> wrapper) {
                    synchronized (wrapper) {
                        if (wrapper.getParamNameValuePairs().putIfAbsent("EGON_WRITE_WRAPPER_USED", Boolean.TRUE) != null) {
                            throw new IllegalStateException("WRITE_WRAPPER_REUSED");
                        }
                    }
                }
                Object entity = parameter instanceof Map<?, ?> map ? (map.containsKey("et") ? map.get("et") : null) : parameter;
                if (root == null && entity instanceof EgonModel<?> model && (model.getId() == null || ids.size() != 1 || !ids.contains(model.getId()))) {
                    throw new IllegalStateException("WRITE_ID_MISMATCH");
                }
            }
        }
        return invocation.proceed();
    }

    static boolean allIgnored(SqlShapeBO shape, EgonColaMybatisPlusProperties properties) {
        return !shape.scopes().isEmpty() && shape.scopes().stream().allMatch(scope -> properties.getTenantId().ignores(identifier(scope.table().getName())));
    }

    static SqlShapeBO shape(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            List<TableScopeBO> scopes = new ArrayList<>();
            if (statement instanceof Update update) {
                if (update.getFromItem() != null || update.getJoins() != null && !update.getJoins().isEmpty() || update.getWithItemsList() != null && !update.getWithItemsList().isEmpty()) {
                    throw new IllegalStateException("SQL_SHAPE_UNSUPPORTED");
                }
                scopes.add(new TableScopeBO(update.getTable(), update.getWhere(), update.getWhere(), update, 1));
                nested(update.getWhere(), scopes, new HashSet<>());
            } else if (statement instanceof Delete delete) {
                scopes.add(new TableScopeBO(delete.getTable(), delete.getWhere(), delete.getWhere(), delete, 1));
                nested(delete.getWhere(), scopes, new HashSet<>());
            } else if (statement instanceof Insert insert) {
                if (!(insert.getSelect() instanceof Values)) {
                    throw new IllegalStateException("SQL_SHAPE_UNSUPPORTED");
                }
                scopes.add(new TableScopeBO(insert.getTable(), null, null, insert, 1));
            } else if (statement instanceof Select select) {
                collect(select, scopes, new HashSet<>());
            } else {
                throw new IllegalStateException("SQL_SHAPE_UNSUPPORTED");
            }
            if (scopes.isEmpty()) {
                throw new IllegalStateException("SQL_SHAPE_UNSUPPORTED");
            }
            scopes.forEach(scope -> identifier(scope.table().getName()));
            return new SqlShapeBO(statement, List.copyOf(scopes));
        } catch (JSQLParserException exception) {
            throw new IllegalStateException("SQL_SHAPE_UNSUPPORTED", exception);
        }
    }

    private static void collect(Select select, List<TableScopeBO> scopes, Set<String> inheritedCtes) {
        Set<String> ctes = new HashSet<>(inheritedCtes);
        if (select.getWithItemsList() != null) {
            for (WithItem<?> item : select.getWithItemsList()) {
                ctes.add(identifier(item.getAliasName()));
                if (item.getSelect() == null) {
                    throw new IllegalStateException("SQL_SHAPE_UNSUPPORTED");
                }
                collect(item.getSelect(), scopes, ctes);
            }
        }
        if (select instanceof ParenthesedSelect nested) {
            collect(nested.getSelect(), scopes, ctes);
        } else if (select instanceof SetOperationList set) {
            set.getSelects().forEach(part -> collect(part, scopes, ctes));
        } else if (select instanceof PlainSelect plain) {
            Statement scopeId = plain;
            int tableCount = 1 + (plain.getJoins() == null ? 0 : plain.getJoins().size());
            Expression mandatory = plain.getWhere();
            if (plain.getJoins() != null) {
                for (Join join : plain.getJoins()) {
                    if (join.isRight() || join.isFull() || join.isNatural() || (join.getUsingColumns() != null && !join.getUsingColumns().isEmpty())) {
                        throw new IllegalStateException("SQL_SHAPE_UNSUPPORTED");
                    }
                    if (!join.isLeft()) {
                        for (Expression on : join.getOnExpressions()) {
                            mandatory = and(mandatory, on);
                        }
                    }
                }
            }
            from(plain.getFromItem(), mandatory, mandatory, scopeId, tableCount, scopes, ctes);
            if (plain.getJoins() != null) {
                for (Join join : plain.getJoins()) {
                    Expression on = null;
                    for (Expression expression : join.getOnExpressions()) {
                        on = and(on, expression);
                    }
                    from(join.getRightItem(), and(mandatory, on), join.isLeft() ? on : and(mandatory, on), scopeId, tableCount, scopes, ctes);
                    nested(on, scopes, ctes);
                }
            }
            Set<String> aliases = new HashSet<>();
            for (TableScopeBO scope : scopes) {
                if (scope.scopeId() == scopeId && !aliases.add(scope.alias())) {
                    throw new IllegalStateException("SQL_ALIAS_AMBIGUOUS");
                }
            }
            nested(plain.getWhere(), scopes, ctes);
            plain.getSelectItems().forEach(item -> nested(item.getExpression(), scopes, ctes));
        } else {
            throw new IllegalStateException("SQL_SHAPE_UNSUPPORTED");
        }
    }

    private static void from(FromItem from, Expression predicate, Expression isolation, Statement scopeId, int count, List<TableScopeBO> scopes, Set<String> ctes) {
        if (from instanceof Table table) {
            if (!ctes.contains(identifier(table.getName()))) {
                scopes.add(new TableScopeBO(table, predicate, isolation, scopeId, count));
            }
        } else if (from instanceof ParenthesedSelect select) {
            collect(select.getSelect(), scopes, ctes);
        } else {
            throw new IllegalStateException("SQL_SHAPE_UNSUPPORTED");
        }
    }

    private static void nested(Expression expression, List<TableScopeBO> scopes, Set<String> ctes) {
        if (expression == null) {
            return;
        }
        expression.accept(new ExpressionVisitorAdapter<Void>() {
            @Override
            public <S> Void visit(ParenthesedSelect select, S context) {
                collect(select.getSelect(), scopes, ctes);
                return null;
            }
        }, null);
    }

    static Expression and(Expression left, Expression right) {
        return left == null ? right : right == null ? left : new AndExpression(left, right);
    }

    static Set<Object> values(Expression expression, TableScopeBO scope, String column, SqlShapeBO shape, BoundSql sql, Configuration configuration, boolean boundOnly, Set<String> visited) {
        Expression checked = unwrap(expression);
        if (checked instanceof AndExpression and) {
            Set<Object> left = values(and.getLeftExpression(), scope, column, shape, sql, configuration, boundOnly, visited);
            Set<Object> right = values(and.getRightExpression(), scope, column, shape, sql, configuration, boundOnly, visited);
            if (left == null) {
                return right;
            }
            if (right == null) {
                return left;
            }
            Set<Object> intersection = new LinkedHashSet<>(left);
            intersection.retainAll(right);
            return intersection;
        }
        if (checked instanceof OrExpression or) {
            Set<Object> left = values(or.getLeftExpression(), scope, column, shape, sql, configuration, boundOnly, visited);
            Set<Object> right = values(or.getRightExpression(), scope, column, shape, sql, configuration, boundOnly, visited);
            if (left == null || right == null) {
                return null;
            }
            Set<Object> union = new LinkedHashSet<>(left);
            union.addAll(right);
            return union;
        }
        if (checked instanceof EqualsTo equal) {
            Expression other = matches(equal.getLeftExpression(), scope, column) ? equal.getRightExpression() : matches(equal.getRightExpression(), scope, column) ? equal.getLeftExpression() : null;
            if (other == null) {
                return null;
            }
            if (other instanceof Column reference && !boundOnly) {
                String referenceKey = reference.toString();
                if (visited.contains(referenceKey)) {
                    return null;
                }
                Set<String> next = new HashSet<>(visited);
                next.add(referenceKey);
                for (TableScopeBO candidate : shape.scopes()) {
                    if (candidate.scopeId() == scope.scopeId() && matches(reference, candidate, identifier(reference.getColumnName()))) {
                        return values(candidate.predicate(), candidate, identifier(reference.getColumnName()), shape, sql, configuration, false, next);
                    }
                }
                return null;
            }
            if (boundOnly && !(unwrap(other) instanceof JdbcParameter)) {
                return null;
            }
            Object value = value(other, sql, configuration);
            return value == null ? null : Set.of(value);
        }
        if (checked instanceof InExpression in && !in.isNot() && matches(in.getLeftExpression(), scope, column) && in.getRightExpression() instanceof ExpressionList<?> expressions) {
            Set<Object> result = new LinkedHashSet<>();
            for (Object item : expressions) {
                if (!(item instanceof Expression member) || boundOnly && !(unwrap(member) instanceof JdbcParameter)) {
                    return null;
                }
                Object value = value(member, sql, configuration);
                if (value == null) {
                    return null;
                }
                result.add(value);
            }
            return result;
        }
        return null;
    }

    static boolean active(Expression expression, TableScopeBO scope) {
        Expression checked = unwrap(expression);
        if (checked instanceof AndExpression and) {
            return active(and.getLeftExpression(), scope) || active(and.getRightExpression(), scope);
        }
        if (checked instanceof OrExpression or) {
            return active(or.getLeftExpression(), scope) && active(or.getRightExpression(), scope);
        }
        return checked instanceof IsNullExpression isNull && !isNull.isNot() && matches(isNull.getLeftExpression(), scope, "deleted_at");
    }

    static boolean matches(Expression expression, TableScopeBO scope, String column) {
        if (!(unwrap(expression) instanceof Column candidate) || !identifier(candidate.getColumnName()).equals(column)) {
            return false;
        }
        String qualifier = candidate.getTable() == null ? null : candidate.getTable().getName();
        return qualifier == null || qualifier.isBlank() ? scope.tableCount() == 1 : identifier(qualifier).equals(scope.alias());
    }

    static Expression unwrap(Expression expression) {
        while (expression instanceof ParenthesedExpressionList<?> list && list.size() == 1 && list.getFirst() instanceof Expression member) {
            expression = member;
        }
        return expression;
    }

    static Object value(Expression expression, BoundSql sql, Configuration configuration) {
        Expression checked = unwrap(expression);
        if (checked instanceof JdbcParameter parameter) {
            int index = parameter.getIndex() == null ? -1 : parameter.getIndex() - 1;
            if (index < 0 || index >= sql.getParameterMappings().size()) {
                throw new IllegalStateException("SQL_BINDING_UNRESOLVED");
            }
            String property = sql.getParameterMappings().get(index).getProperty();
            if (sql.hasAdditionalParameter(property)) {
                return sql.getAdditionalParameter(property);
            }
            Object argument = sql.getParameterObject();
            if (argument == null) {
                return null;
            }
            if (configuration.getTypeHandlerRegistry().hasTypeHandler(argument.getClass())) {
                return argument;
            }
            if (argument instanceof Map<?, ?> map && map.containsKey(property)) {
                return map.get(property);
            }
            try {
                return configuration.newMetaObject(argument).getValue(property);
            } catch (RuntimeException failure) {
                throw new IllegalStateException("SQL_BINDING_UNRESOLVED", failure);
            }
        }
        if (checked instanceof SignedExpression signed && signed.getExpression() instanceof LongValue number) {
            java.math.BigInteger value = number.getBigIntegerValue();
            return (signed.getSign() == '-' ? value.negate() : value).longValueExact();
        }
        if (checked instanceof LongValue number) {
            return number.getBigIntegerValue().longValueExact();
        }
        if (checked instanceof StringValue string) {
            return string.getValue();
        }
        if (checked == null || checked instanceof NullValue) {
            return null;
        }
        return null;
    }

    static String property(Expression expression, BoundSql sql) {
        if (unwrap(expression) instanceof JdbcParameter parameter && parameter.getIndex() != null && parameter.getIndex() > 0 && parameter.getIndex() <= sql.getParameterMappings().size()) {
            return sql.getParameterMappings().get(parameter.getIndex() - 1).getProperty();
        }
        return "";
    }

    static String identifier(String name) {
        if (name == null || name.isBlank() || name.indexOf('`') >= 0) {
            throw new IllegalStateException("SQL_IDENTIFIER_UNSUPPORTED");
        }
        String value = name;
        if (name.startsWith("\"") && name.endsWith("\"")) {
            value = name.substring(1, name.length() - 1);
            if (!value.equals(value.toLowerCase(Locale.ROOT))) {
                throw new IllegalStateException("SQL_IDENTIFIER_UNSUPPORTED");
            }
        }
        if (!value.matches("[a-zA-Z_][a-zA-Z0-9_]{0,62}")) {
            throw new IllegalStateException("SQL_IDENTIFIER_UNSUPPORTED");
        }
        return value.toLowerCase(Locale.ROOT);
    }

    record SqlShapeBO(Statement statement, List<TableScopeBO> scopes) {
    }

    record TableScopeBO(Table table, Expression predicate, Expression isolation, Statement scopeId, int tableCount) {
        String alias() {
            return identifier(table.getAlias() == null ? table.getName() : table.getAlias().getName());
        }
    }
}
