package top.egon.cola.component.codegen.template;

import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.codegen.model.CodegenConfigBO;
import top.egon.cola.component.codegen.model.CodegenProfileEnum;
import top.egon.cola.component.codegen.model.CodegenSchemaBO;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Turns schema and field policy into a read-only render model. It does not expose the parser or a class loader.
 */
@Slf4j
public class TemplateContextService {

    private static final Set<String> SERVER_FIELDS = Set.of(
            "tenant_id", "created_at", "created_by", "updated_at", "updated_by", "deleted_at",
            "create_time", "create_user_id", "update_time", "update_user_id");

    public Map<String, Object> context(
            CodegenSchemaBO schema,
            CodegenSchemaBO.TableBO table,
            CodegenProfileEnum profile,
            String artifact,
            CodegenConfigBO.FieldPoliciesBO policies) {
        if (schema == null || table == null || profile == null || artifact == null || artifact.isBlank()) {
            throw new IllegalArgumentException("schema, table, profile and artifact are required");
        }
        List<Map<String, String>> fields = new ArrayList<>();
        Set<String> imports = new LinkedHashSet<>();
        for (CodegenSchemaBO.ColumnBO column : table.getColumns()) {
            if (column.getName() == null || isHidden(column.getName(), policies, artifact)) {
                continue;
            }
            String javaType = javaType(column);
            if (javaType.contains(".")) {
                imports.add(javaType);
            }
            fields.add(Map.of(
                    "column", column.getName(),
                    "javaName", javaName(column.getName()),
                    "javaType", simpleName(javaType),
                    "sqlType", column.getSqlType() == null ? "" : column.getSqlType()));
        }
        List<String> sortedImports = new ArrayList<>(imports);
        sortedImports.sort(String::compareTo);
        log.debug("prepared {} render fields for {}", fields.size(), artifact);
        return Map.of(
                "profile", profile.code(),
                "artifact", artifact,
                "logicalTable", table.getLogicalName() == null ? "" : table.getLogicalName(),
                "role", table.getRole() == null ? "" : table.getRole(),
                "imports", List.copyOf(sortedImports),
                "fields", List.copyOf(fields));
    }

    private static boolean isHidden(String column, CodegenConfigBO.FieldPoliciesBO policies, String artifact) {
        if (policies == null || "po".equals(artifact) || "dao".equals(artifact) || "mapper-xml".equals(artifact)
                || "repo".equals(artifact)) {
            return false;
        }
        Set<String> allowed = new LinkedHashSet<>();
        addAll(allowed, policies.getCreate());
        addAll(allowed, policies.getUpdate());
        addAll(allowed, policies.getResult());
        addAll(allowed, policies.getFilter());
        addAll(allowed, policies.getSort());
        return !allowed.contains(column) || SERVER_FIELDS.contains(column);
    }

    private static void addAll(Set<String> target, List<String> values) {
        if (values != null) {
            target.addAll(values);
        }
    }

    static String javaName(String column) {
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

    private static String javaType(CodegenSchemaBO.ColumnBO column) {
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
        return "String";
    }

    private static String simpleName(String javaType) {
        int dot = javaType.lastIndexOf('.');
        return dot < 0 ? javaType : javaType.substring(dot + 1);
    }
}
