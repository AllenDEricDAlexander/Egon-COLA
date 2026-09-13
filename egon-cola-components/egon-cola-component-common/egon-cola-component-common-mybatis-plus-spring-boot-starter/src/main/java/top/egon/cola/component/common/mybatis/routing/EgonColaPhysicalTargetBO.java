package top.egon.cola.component.common.mybatis.routing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Comparator;

/**
 * Exact physical table in a canonical primary group; never a replica alias.
 */
public record EgonColaPhysicalTargetBO(
        @NotBlank @Pattern(regexp = "[a-zA-Z_][a-zA-Z0-9_-]{0,62}") String group,
        @NotBlank @Pattern(regexp = "[a-z_][a-z0-9_]{0,62}") String schema,
        @NotBlank @Pattern(regexp = "[a-z_][a-z0-9_]{0,62}") String table) {

    static final Comparator<EgonColaPhysicalTargetBO> ORDER = Comparator
            .comparing(EgonColaPhysicalTargetBO::group)
            .thenComparing(EgonColaPhysicalTargetBO::schema)
            .thenComparing(EgonColaPhysicalTargetBO::table);

    public EgonColaPhysicalTargetBO {
        if (group == null || !group.matches("[a-zA-Z_][a-zA-Z0-9_-]{0,62}")) {
            throw new IllegalArgumentException("Invalid primary group identifier");
        }
        requireIdentifier(schema, "schema");
        requireIdentifier(table, "table");
    }

    static void requireIdentifier(String value, String name) {
        if (value == null || !value.matches("[a-z_][a-z0-9_]{0,62}")) {
            throw new IllegalArgumentException("Invalid PostgreSQL " + name + " identifier");
        }
    }
}
