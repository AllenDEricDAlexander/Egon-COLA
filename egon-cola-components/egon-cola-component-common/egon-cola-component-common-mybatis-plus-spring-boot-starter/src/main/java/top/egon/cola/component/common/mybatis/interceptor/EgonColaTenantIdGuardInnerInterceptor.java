package top.egon.cola.component.common.mybatis.interceptor;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.statement.Statement;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.apache.ibatis.executor.statement.StatementHandler;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fail-closed SQL guard that runs before MyBatis-Plus TenantLine rewriting.
 *
 * <p>TenantLine remains the single SQL rewriting mechanism. This interceptor
 * only validates explicit tenant predicates and protected-column mutations so
 * a caller cannot spoof or widen the shared scope.</p>
 */
public final class EgonColaTenantIdGuardInnerInterceptor implements InnerInterceptor {

    private static final String TENANT_COLUMN = "tenant_id";
    private static final String LOGIC_COLUMN = "is_deleted";
    private static final Pattern TABLE_PATTERN = Pattern.compile(
            "\\b(?:from|join|update|into|delete\\s+from)\\s+([a-zA-Z0-9_.$`\"]+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TENANT_EQUAL_PATTERN = Pattern.compile(
            "\\btenant_id\\s*=\\s*(\\?|[-+]?\\d+|null)\\b?", Pattern.CASE_INSENSITIVE);
    private static final Pattern UPDATE_SET_PATTERN = Pattern.compile(
            "\\bset\\b(.*?)(?:\\bwhere\\b|$)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final EgonColaTenantIdProvider tenantIdProvider;
    private final Set<String> ignoredTables;

    public EgonColaTenantIdGuardInnerInterceptor(
            EgonColaTenantIdProvider tenantIdProvider,
            EgonColaMybatisPlusProperties properties) {
        this.tenantIdProvider = Objects.requireNonNull(tenantIdProvider,
                "tenantIdProvider must not be null");
        Objects.requireNonNull(properties, "properties must not be null");
        this.ignoredTables = properties.getTenantId().getIgnoredTables().stream()
                .filter(Objects::nonNull)
                .map(EgonColaTenantIdGuardInnerInterceptor::normalizeIdentifier)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Override
    public void beforePrepare(StatementHandler sh, Connection connection, Integer transactionTimeout) {
        PluginUtils.MPStatementHandler statementHandler = PluginUtils.mpStatementHandler(sh);
        PluginUtils.MPBoundSql boundSql = statementHandler.mPBoundSql();
        String sql = boundSql.sql();
        if (sql == null || sql.isBlank()) {
            throw new IllegalStateException("SQL_SHAPE_UNSUPPORTED");
        }
        Statement parsed;
        try {
            parsed = CCJSqlParserUtil.parse(sql);
        } catch (JSQLParserException | RuntimeException exception) {
            throw new IllegalStateException("SQL_SHAPE_UNSUPPORTED", exception);
        }
        List<String> tables = referencedTables(sql);
        if (tables.isEmpty()) {
            throw new IllegalStateException("SQL_SHAPE_UNSUPPORTED");
        }
        if (tables.stream().noneMatch(table -> !ignoredTables.contains(table))) {
            return;
        }

        Long currentTenantId = tenantIdProvider.currentTenantId();
        if (currentTenantId == null) {
            throw new IllegalStateException("TENANT_CONTEXT_MISSING");
        }

        String normalizedSql = normalizeSql(sql);
        MappedStatement mappedStatement = statementHandler.mappedStatement();
        if (isUpdate(mappedStatement, normalizedSql)) {
            validateProtectedAssignments(normalizedSql, mappedStatement, boundSql);
            normalizedSql = normalizeSql(boundSql.sql());
        }
        validateExplicitTenantPredicates(normalizedSql, boundSql, statementHandler.configuration(),
                currentTenantId);
        // Keep the parsed statement in the guard's execution path: a parser
        // success is the minimum supported-shape contract before TenantLine.
        if (parsed == null) {
            throw new IllegalStateException("SQL_SHAPE_UNSUPPORTED");
        }
    }

    private void validateProtectedAssignments(String sql,
                                              MappedStatement mappedStatement,
                                              PluginUtils.MPBoundSql boundSql) {
        Matcher matcher = UPDATE_SET_PATTERN.matcher(sql);
        if (!matcher.find()) {
            return;
        }
        String assignments = matcher.group(1);
        if (containsAssignment(assignments, TENANT_COLUMN)) {
            if (!removeAuthoritativeTenantAssignment(mappedStatement, boundSql)) {
                throw new IllegalStateException("TENANT_COLUMN_MUTATION_FORBIDDEN");
            }
            sql = normalizeSql(boundSql.sql());
            matcher = UPDATE_SET_PATTERN.matcher(sql);
            if (!matcher.find()) {
                return;
            }
            assignments = matcher.group(1);
        }
        if (containsAssignment(assignments, LOGIC_COLUMN)
                && !isOfficialLogicDelete(mappedStatement)) {
            throw new IllegalStateException("LOGIC_DELETE_COLUMN_MUTATION_FORBIDDEN");
        }
    }

    private static boolean removeAuthoritativeTenantAssignment(
            MappedStatement mappedStatement, PluginUtils.MPBoundSql boundSql) {
        if (!isOfficialTenantFillStatement(mappedStatement)) {
            return false;
        }
        String originalSql = boundSql.sql();
        Matcher assignment = Pattern.compile("\\btenant_id\\s*=\\s*\\?",
                        Pattern.CASE_INSENSITIVE).matcher(originalSql);
        if (!assignment.find()) {
            return false;
        }
        int questionMarkIndex = countQuestionMarks(originalSql, 0, assignment.start());
        List<ParameterMapping> mappings = boundSql.parameterMappings();
        if (questionMarkIndex >= mappings.size()
                || !isAuthoritativeTenantProperty(mappings.get(questionMarkIndex).getProperty())) {
            return false;
        }
        int start = assignment.start();
        int end = assignment.end();
        int before = start - 1;
        while (before >= 0 && Character.isWhitespace(originalSql.charAt(before))) {
            before--;
        }
        if (before >= 0 && originalSql.charAt(before) == ',') {
            start = before;
        } else {
            while (end < originalSql.length() && Character.isWhitespace(originalSql.charAt(end))) {
                end++;
            }
            if (end < originalSql.length() && originalSql.charAt(end) == ',') {
                end++;
            }
        }
        boundSql.sql(originalSql.substring(0, start) + originalSql.substring(end));
        List<ParameterMapping> remaining = new ArrayList<>(mappings);
        remaining.remove(questionMarkIndex);
        boundSql.parameterMappings(remaining);
        return true;
    }

    private static boolean isOfficialTenantFillStatement(MappedStatement mappedStatement) {
        if (mappedStatement == null || mappedStatement.getId() == null) {
            return false;
        }
        String id = mappedStatement.getId().toLowerCase(Locale.ROOT);
        return id.contains(".updatebyid")
                || id.endsWith(".update")
                || id.contains(".deletebyid");
    }

    private static boolean isAuthoritativeTenantProperty(String property) {
        if (property == null) {
            return false;
        }
        String normalized = property.toLowerCase(Locale.ROOT);
        return normalized.contains("tenantid") && !normalized.contains("paramnamevaluepairs");
    }

    private void validateExplicitTenantPredicates(String sql,
                                                  PluginUtils.MPBoundSql boundSql,
                                                  Configuration configuration,
                                                  Long currentTenantId) {
        Matcher matcher = TENANT_EQUAL_PATTERN.matcher(sql);
        int lastMatchEnd = -1;
        int questionMarksBefore = 0;
        boolean explicitPredicate = false;
        while (matcher.find()) {
            explicitPredicate = true;
            questionMarksBefore += countQuestionMarks(sql, lastMatchEnd + 1, matcher.start());
            String valueToken = matcher.group(1);
            Object value = "?".equals(valueToken)
                    ? resolveParameter(boundSql, configuration, questionMarksBefore)
                    : parseLiteral(valueToken);
            if (!(value instanceof Number number)
                    || number.longValue() != currentTenantId) {
                throw new IllegalStateException("TENANT_CONTEXT_MISMATCH");
            }
            lastMatchEnd = matcher.end() - 1;
        }
        if (!explicitPredicate && containsTenantPredicate(sql)) {
            throw new IllegalStateException("TENANT_CONTEXT_MISMATCH");
        }
    }

    private Object resolveParameter(PluginUtils.MPBoundSql boundSql,
                                    Configuration configuration,
                                    int questionMarkIndex) {
        List<ParameterMapping> mappings = boundSql.parameterMappings();
        if (questionMarkIndex < 0 || questionMarkIndex >= mappings.size()) {
            return null;
        }
        String property = mappings.get(questionMarkIndex).getProperty();
        Map<String, Object> additional = boundSql.additionalParameters();
        if (additional.containsKey(property)) {
            return additional.get(property);
        }
        Object parameterObject = boundSql.parameterObject();
        if (parameterObject == null) {
            return null;
        }
        if (parameterObject instanceof Map<?, ?> values && values.containsKey(property)) {
            return values.get(property);
        }
        TypeHandlerRegistry registry = configuration == null
                ? null : configuration.getTypeHandlerRegistry();
        if (registry != null && registry.hasTypeHandler(parameterObject.getClass())) {
            return parameterObject;
        }
        if (configuration != null) {
            MetaObject metaObject = configuration.newMetaObject(parameterObject);
            if (metaObject.hasGetter(property)) {
                return metaObject.getValue(property);
            }
        }
        return null;
    }

    private static int countQuestionMarks(String sql, int start, int end) {
        int count = 0;
        for (int index = Math.max(0, start); index < Math.min(sql.length(), end); index++) {
            if (sql.charAt(index) == '?') {
                count++;
            }
        }
        return count;
    }

    private static Object parseLiteral(String token) {
        if (token == null || token.equalsIgnoreCase("null")) {
            return null;
        }
        try {
            return Long.valueOf(token);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static boolean containsTenantPredicate(String sql) {
        int where = sql.indexOf(" where ");
        if (where < 0) {
            return false;
        }
        return sql.substring(where).contains(TENANT_COLUMN);
    }

    private static boolean containsAssignment(String assignments, String column) {
        return Pattern.compile("\\b" + Pattern.quote(column) + "\\s*=", Pattern.CASE_INSENSITIVE)
                .matcher(assignments).find();
    }

    private static boolean isUpdate(MappedStatement mappedStatement, String sql) {
        return (mappedStatement != null
                && mappedStatement.getSqlCommandType() == org.apache.ibatis.mapping.SqlCommandType.UPDATE)
                || sql.startsWith("update ");
    }

    private static boolean isOfficialLogicDelete(MappedStatement mappedStatement) {
        if (mappedStatement == null || mappedStatement.getId() == null) {
            return false;
        }
        String id = mappedStatement.getId().toLowerCase(Locale.ROOT);
        return id.contains("deletebyid") || id.endsWith(".delete");
    }

    private static List<String> referencedTables(String sql) {
        List<String> tables = new ArrayList<>();
        Matcher matcher = TABLE_PATTERN.matcher(sql);
        while (matcher.find()) {
            tables.add(normalizeIdentifier(matcher.group(1)));
        }
        return tables;
    }

    private static String normalizeSql(String sql) {
        return sql.replace('`', ' ').replace('"', ' ')
                .replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeIdentifier(String identifier) {
        String normalized = identifier == null ? "" : identifier.trim()
                .replace("`", "").replace("\"", "");
        int separator = normalized.lastIndexOf('.');
        return (separator >= 0 ? normalized.substring(separator + 1) : normalized)
                .toLowerCase(Locale.ROOT);
    }
}
