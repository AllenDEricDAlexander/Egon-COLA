package top.egon.cola.component.codegen.validation;

import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.codegen.model.CodegenConfigBO;
import top.egon.cola.component.codegen.model.CodegenPlanBO;
import top.egon.cola.component.codegen.model.CodegenSchemaBO;
import top.egon.cola.component.codegen.profile.ProjectLayoutStrategy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Preflight for persistence generation. A failure returns diagnostics and does not create directories.
 */
@Slf4j
public class GenerationScopeValidator {

    public static final String BASE_FIELD_TYPE = "BASE_FIELD_TYPE";

    public static final String MISSING_LIFECYCLE_KEY = "MISSING_LIFECYCLE_KEY";

    public static final String MISSING_ACTIVE_GUARD = "MISSING_ACTIVE_GUARD";

    public static final String LEGACY_UNIQUE_KEY = "LEGACY_UNIQUE_KEY";

    public static final String UNSUPPORTED_ID = "UNSUPPORTED_ID";

    public static final String UNKNOWN_SQL_TYPE = "UNKNOWN_SQL_TYPE";

    public static final String MISSING_TYPE = "MISSING_TYPE";

    public static final String BROADCAST_WRITE = "BROADCAST_WRITE";

    public static final String UNSUPPORTED_ARTIFACT = "UNSUPPORTED_ARTIFACT";

    private static final Set<String> BASE_COLUMNS = Set.of(
            "id", "tenant_id", "create_user_id", "create_time", "update_user_id", "update_time", "deleted_at", "version");

    private static final List<String> XML_COLUMNS = List.of(
            "id", "tenant_id", "create_user_id", "create_time", "update_user_id", "update_time", "deleted_at", "version");

    private static final String PROPERTIES_BEAN =
            "egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties";

    public List<CodegenPlanBO.DiagnosticBO> validate(CodegenConfigBO config, CodegenSchemaBO schema) {
        List<CodegenPlanBO.DiagnosticBO> diagnostics = new ArrayList<>();
        if (config == null || config.getProfile() == null || config.getArtifacts() == null || config.getArtifacts().isEmpty()) {
            diagnostics.add(diagnostic("CONFIG_REQUIRED", "/artifacts", "generation scope is incomplete"));
            return diagnostics;
        }
        ProjectLayoutStrategy layout = ProjectLayoutStrategy.resolve(config.getProfile());
        for (String artifact : config.getArtifacts()) {
            if (!layout.allows(artifact)) {
                diagnostics.add(diagnostic(UNSUPPORTED_ARTIFACT, "/artifacts", "artifact is not allowed for " + config.getProfile().code()));
            }
        }
        if (schema == null || schema.getTables() == null || schema.getTables().isEmpty()) {
            diagnostics.add(diagnostic("CONFIG_REQUIRED", "/logicalTables", "schema has no tables"));
            return diagnostics;
        }
        for (CodegenSchemaBO.TableBO table : schema.getTables()) {
            diagnostics.addAll(validateTable(config, table));
        }
        diagnostics.addAll(existingTypes(config));
        if (!diagnostics.isEmpty()) {
            log.warn("generation scope rejected: {}", diagnostics.stream().map(CodegenPlanBO.DiagnosticBO::getCode).distinct().toList());
        }
        return List.copyOf(diagnostics);
    }

    public Map<String, Object> renderModel(CodegenConfigBO config, CodegenSchemaBO.TableBO table, String artifact) {
        ProjectLayoutStrategy layout = ProjectLayoutStrategy.resolve(config.getProfile());
        String poType = simpleType(config, "po", typeName(table.getLogicalName(), "PO"));
        String daoType = simpleType(config, "dao", typeName(table.getLogicalName(), "DAO"));
        String repoType = typeName(table.getLogicalName(), "Repository");
        String poFqn = qualified(config, "po", layout.packageName(config, "po"), poType);
        String daoFqn = qualified(config, "dao", layout.packageName(config, "dao"), daoType);
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("artifact", artifact);
        model.put("poPackage", packageOf(poFqn));
        model.put("daoPackage", packageOf(daoFqn));
        model.put("repoPackage", layout.packageName(config, "repo"));
        model.put("poType", poType);
        model.put("daoType", daoType);
        model.put("repoType", repoType);
        model.put("poFqn", poFqn);
        model.put("daoFqn", daoFqn);
        model.put("tableName", table.getLogicalName());
        model.put("businessFields", businessFields(table));
        model.put("xmlColumns", xmlColumns(table));
        model.put("daoBean", decapitalize(daoType));
        model.put("repoBean", decapitalize(repoType));
        model.put("propertiesBean", PROPERTIES_BEAN);
        return Map.copyOf(model);
    }

    private List<CodegenPlanBO.DiagnosticBO> validateTable(CodegenConfigBO config, CodegenSchemaBO.TableBO table) {
        List<CodegenPlanBO.DiagnosticBO> diagnostics = new ArrayList<>();
        if (table.getLogicalName() == null || !isIdentifier(table.getLogicalName())) {
            diagnostics.add(diagnostic("CONFIG_REQUIRED", "/logicalTables", "logical table name is not a Java identifier"));
            return diagnostics;
        }
        if ("BROADCAST".equals(table.getRole()) && containsWrite(config.getArtifacts())) {
            diagnostics.add(diagnostic(BROADCAST_WRITE, "/artifacts", "broadcast tables do not generate writes"));
        }
        List<String> primaryColumns = primaryColumns(table);
        CodegenSchemaBO.ColumnBO id = column(table, "id");
        if (primaryColumns.size() != 1 || !"id".equals(primaryColumns.get(0)) || id == null
                || !isBigint(id.getSqlType()) || Boolean.TRUE.equals(id.getNullable())) {
            diagnostics.add(diagnostic(UNSUPPORTED_ID, "/" + table.getLogicalName() + "/id",
                    "CRUD requires one non-null BIGINT id primary key"));
        }
        requireBase(diagnostics, table, "tenant_id", "bigint", false);
        requireBase(diagnostics, table, "deleted_at", "timestamp", true);
        requireBase(diagnostics, table, "version", "bigint", false);
        if (!hasLifecycleKey(table)) {
            diagnostics.add(diagnostic(MISSING_LIFECYCLE_KEY, "/" + table.getLogicalName(),
                    "business uniqueness must include deleted_at"));
        }
        if (!hasActiveGuard(table)) {
            diagnostics.add(diagnostic(MISSING_ACTIVE_GUARD, "/" + table.getLogicalName(),
                    "active rows need a NULL deleted_at unique guard"));
        }
        if (hasLegacyBusinessKey(table)) {
            diagnostics.add(diagnostic(LEGACY_UNIQUE_KEY, "/" + table.getLogicalName(),
                    "a business-only unique key still blocks recreate after delete"));
        }
        for (CodegenSchemaBO.ColumnBO column : table.getColumns()) {
            if (column.getName() == null || !isIdentifier(column.getName())) {
                diagnostics.add(diagnostic("CONFIG_REQUIRED", "/" + table.getLogicalName(), "column name is not an identifier"));
                continue;
            }
            if (!BASE_COLUMNS.contains(column.getName()) && javaType(column) == null) {
                diagnostics.add(diagnostic(UNKNOWN_SQL_TYPE, "/" + table.getLogicalName() + "/" + column.getName(),
                        "SQL type is not mapped"));
            }
        }
        return diagnostics;
    }

    private static List<CodegenPlanBO.DiagnosticBO> existingTypes(CodegenConfigBO config) {
        List<CodegenPlanBO.DiagnosticBO> diagnostics = new ArrayList<>();
        Set<String> artifacts = Set.copyOf(config.getArtifacts());
        if (artifacts.contains("repo") && !artifacts.contains("po") && blank(mapping(config, "po"))) {
            diagnostics.add(missing("po"));
        }
        if ((artifacts.contains("repo") || artifacts.contains("mapper-xml")) && !artifacts.contains("dao")
                && blank(mapping(config, "dao"))) {
            diagnostics.add(missing("dao"));
        }
        return diagnostics;
    }

    private static void requireBase(
            List<CodegenPlanBO.DiagnosticBO> diagnostics, CodegenSchemaBO.TableBO table, String name, String typePrefix, boolean nullable) {
        CodegenSchemaBO.ColumnBO column = column(table, name);
        if (column == null || column.getSqlType() == null || !column.getSqlType().toLowerCase(Locale.ROOT).startsWith(typePrefix)
                || (nullable && Boolean.FALSE.equals(column.getNullable()))
                || (!nullable && Boolean.TRUE.equals(column.getNullable()))) {
            diagnostics.add(diagnostic(BASE_FIELD_TYPE, "/" + table.getLogicalName() + "/" + name,
                    "base column does not match EgonModel"));
        }
    }

    private static boolean hasLifecycleKey(CodegenSchemaBO.TableBO table) {
        for (CodegenSchemaBO.IndexBO index : table.getIndexes()) {
            if (Boolean.TRUE.equals(index.getUnique()) && index.getColumns() != null
                    && index.getColumns().contains("deleted_at") && containsBusiness(table, index.getColumns())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasActiveGuard(CodegenSchemaBO.TableBO table) {
        for (CodegenSchemaBO.IndexBO index : table.getIndexes()) {
            String predicate = index.getPredicate() == null ? "" : index.getPredicate().toLowerCase(Locale.ROOT);
            if (Boolean.TRUE.equals(index.getUnique()) && predicate.contains("deleted_at is null")
                    && containsBusiness(table, index.getColumns())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasLegacyBusinessKey(CodegenSchemaBO.TableBO table) {
        for (CodegenSchemaBO.IndexBO index : table.getIndexes()) {
            if (!Boolean.TRUE.equals(index.getUnique()) || index.getColumns() == null) {
                continue;
            }
            boolean predicate = index.getPredicate() != null && !index.getPredicate().isBlank();
            boolean onlyBusiness = !index.getColumns().contains("deleted_at") && !index.getColumns().contains("id")
                    && containsBusiness(table, index.getColumns()) && !predicate;
            if (onlyBusiness) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsBusiness(CodegenSchemaBO.TableBO table, List<String> columns) {
        if (columns == null) {
            return false;
        }
        for (String column : columns) {
            if (!BASE_COLUMNS.contains(column) && column(table, column) != null) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsWrite(List<String> artifacts) {
        for (String artifact : artifacts) {
            if (ProjectLayoutStrategy.PERSISTENCE.contains(artifact)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> primaryColumns(CodegenSchemaBO.TableBO table) {
        java.util.LinkedHashSet<String> columns = new java.util.LinkedHashSet<>();
        if (table.getConstraints() != null) {
            for (CodegenSchemaBO.ConstraintBO constraint : table.getConstraints()) {
                if ("PRIMARY_KEY".equals(constraint.getKind()) && constraint.getColumns() != null) {
                    columns.addAll(constraint.getColumns());
                }
            }
        }
        return new ArrayList<>(columns);
    }

    private static List<Map<String, String>> businessFields(CodegenSchemaBO.TableBO table) {
        List<Map<String, String>> fields = new ArrayList<>();
        for (CodegenSchemaBO.ColumnBO column : table.getColumns()) {
            if (column.getName() == null || BASE_COLUMNS.contains(column.getName())) {
                continue;
            }
            String javaType = javaType(column);
            fields.add(Map.of(
                    "column", column.getName(),
                    "javaName", javaName(column.getName()),
                    "javaType", javaType == null ? "String" : javaType));
        }
        return List.copyOf(fields);
    }

    private static List<String> xmlColumns(CodegenSchemaBO.TableBO table) {
        List<String> columns = new ArrayList<>(XML_COLUMNS);
        for (CodegenSchemaBO.ColumnBO column : table.getColumns()) {
            if (column.getName() != null && !columns.contains(column.getName())) {
                columns.add(column.getName());
            }
        }
        return List.copyOf(columns);
    }

    static String javaType(CodegenSchemaBO.ColumnBO column) {
        String type = column.getSqlType() == null ? "" : column.getSqlType().toLowerCase(Locale.ROOT);
        if (type.startsWith("bigint") || "int8".equals(type)) {
            return "Long";
        }
        if (type.startsWith("int") || type.startsWith("integer") || "int4".equals(type)) {
            return "Integer";
        }
        if (type.startsWith("bool")) {
            return "Boolean";
        }
        if (type.contains("timestamp") || type.startsWith("date") || type.startsWith("time")) {
            return "java.time.LocalDateTime";
        }
        if (type.startsWith("numeric") || type.startsWith("decimal")) {
            return "java.math.BigDecimal";
        }
        if (type.startsWith("varchar") || type.startsWith("char") || type.startsWith("text") || "bpchar".equals(type)) {
            return "String";
        }
        return null;
    }

    private static String javaName(String column) {
        StringBuilder name = new StringBuilder();
        boolean upper = false;
        for (int index = 0; index < column.length(); index++) {
            char current = column.charAt(index);
            if (current == '_') {
                upper = true;
                continue;
            }
            name.append(upper ? Character.toUpperCase(current) : Character.toLowerCase(current));
            upper = false;
        }
        return name.toString();
    }

    static String typeName(String logicalTable, String suffix) {
        StringBuilder name = new StringBuilder();
        boolean upper = true;
        for (int index = 0; index < logicalTable.length(); index++) {
            char current = logicalTable.charAt(index);
            if (current == '_') {
                upper = true;
                continue;
            }
            name.append(upper ? Character.toUpperCase(current) : Character.toLowerCase(current));
            upper = false;
        }
        return name + suffix;
    }

    private static String simpleType(CodegenConfigBO config, String artifact, String generated) {
        if (config.getArtifacts().contains(artifact)) {
            return generated;
        }
        String existing = mapping(config, artifact);
        if (existing == null) {
            return generated;
        }
        int dot = existing.lastIndexOf('.');
        return dot < 0 ? existing : existing.substring(dot + 1);
    }

    private static String qualified(CodegenConfigBO config, String artifact, String generatedPackage, String simple) {
        if (config.getArtifacts().contains(artifact)) {
            return generatedPackage + "." + simple;
        }
        String existing = mapping(config, artifact);
        return existing == null ? generatedPackage + "." + simple : existing;
    }

    private static String packageOf(String qualified) {
        int dot = qualified.lastIndexOf('.');
        return dot < 0 ? "" : qualified.substring(0, dot);
    }

    private static String decapitalize(String typeName) {
        if (typeName == null || typeName.isEmpty()) {
            return typeName;
        }
        return Character.toLowerCase(typeName.charAt(0)) + typeName.substring(1);
    }

    private static String mapping(CodegenConfigBO config, String key) {
        return config.getExistingTypes() == null ? null : config.getExistingTypes().get(key);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean isBigint(String sqlType) {
        return sqlType != null && sqlType.toLowerCase(Locale.ROOT).startsWith("bigint");
    }

    private static boolean isIdentifier(String value) {
        return value.matches("[A-Za-z_][A-Za-z0-9_]*");
    }

    private static CodegenSchemaBO.ColumnBO column(CodegenSchemaBO.TableBO table, String name) {
        for (CodegenSchemaBO.ColumnBO column : table.getColumns()) {
            if (name.equals(column.getName())) {
                return column;
            }
        }
        return null;
    }

    private static CodegenPlanBO.DiagnosticBO missing(String typeName) {
        CodegenPlanBO.DiagnosticBO diagnostic = diagnostic(MISSING_TYPE, "/existingTypeMappings/" + typeName,
                "missing existing type " + typeName);
        diagnostic.setArtifact(typeName);
        return diagnostic;
    }

    private static CodegenPlanBO.DiagnosticBO diagnostic(String code, String pointer, String message) {
        return CodegenPlanBO.DiagnosticBO.of(code, pointer, message);
    }
}
