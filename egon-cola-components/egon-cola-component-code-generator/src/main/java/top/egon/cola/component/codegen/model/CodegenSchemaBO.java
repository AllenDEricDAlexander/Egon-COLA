package top.egon.cola.component.codegen.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * Normalized offline schema. Physical shard targets stay attached to one logical table.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder(alphabetic = true)
public class CodegenSchemaBO {

    private String inputFingerprint;

    private String versionChecksumPrefix;

    @Builder.Default
    private List<TableBO> tables = new ArrayList<>();

    public CodegenSchemaBO snapshot() {
        List<TableBO> copied = new ArrayList<>();
        if (tables != null) {
            for (TableBO table : tables) {
                copied.add(table == null ? null : table.snapshot());
            }
        }
        return CodegenSchemaBO.builder()
                .inputFingerprint(inputFingerprint)
                .versionChecksumPrefix(versionChecksumPrefix)
                .tables(copied)
                .build();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonPropertyOrder(alphabetic = true)
    public static class TableBO {

        private String schema;

        private String logicalName;

        private String role;

        @Builder.Default
        private List<String> physicalNames = new ArrayList<>();

        @Builder.Default
        private List<String> routeKeys = new ArrayList<>();

        @Builder.Default
        private List<ColumnBO> columns = new ArrayList<>();

        @Builder.Default
        private List<ConstraintBO> constraints = new ArrayList<>();

        @Builder.Default
        private List<IndexBO> indexes = new ArrayList<>();

        TableBO snapshot() {
            List<ColumnBO> copiedColumns = new ArrayList<>();
            if (columns != null) {
                for (ColumnBO column : columns) {
                    copiedColumns.add(column == null ? null : column.snapshot());
                }
            }
            List<ConstraintBO> copiedConstraints = new ArrayList<>();
            if (constraints != null) {
                for (ConstraintBO constraint : constraints) {
                    copiedConstraints.add(constraint == null ? null : constraint.snapshot());
                }
            }
            List<IndexBO> copiedIndexes = new ArrayList<>();
            if (indexes != null) {
                for (IndexBO index : indexes) {
                    copiedIndexes.add(index == null ? null : index.snapshot());
                }
            }
            return TableBO.builder()
                    .schema(schema)
                    .logicalName(logicalName)
                    .role(role)
                    .physicalNames(CodegenConfigBO.copyList(physicalNames))
                    .routeKeys(CodegenConfigBO.copyList(routeKeys))
                    .columns(copiedColumns)
                    .constraints(copiedConstraints)
                    .indexes(copiedIndexes)
                    .build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonPropertyOrder(alphabetic = true)
    public static class ColumnBO {

        private String name;

        private Integer ordinal;

        private String sqlType;

        private Integer length;

        private Integer precision;

        private Integer scale;

        private Boolean nullable;

        private String defaultExpression;

        private String comment;

        private SourcePositionBO sourcePosition;

        ColumnBO snapshot() {
            return ColumnBO.builder()
                    .name(name)
                    .ordinal(ordinal)
                    .sqlType(sqlType)
                    .length(length)
                    .precision(precision)
                    .scale(scale)
                    .nullable(nullable)
                    .defaultExpression(defaultExpression)
                    .comment(comment)
                    .sourcePosition(sourcePosition == null ? null : sourcePosition.snapshot())
                    .build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonPropertyOrder(alphabetic = true)
    public static class ConstraintBO {

        private String name;

        private String kind;

        @Builder.Default
        private List<String> columns = new ArrayList<>();

        private String predicate;

        private SourcePositionBO sourcePosition;

        ConstraintBO snapshot() {
            return ConstraintBO.builder()
                    .name(name)
                    .kind(kind)
                    .columns(CodegenConfigBO.copyList(columns))
                    .predicate(predicate)
                    .sourcePosition(sourcePosition == null ? null : sourcePosition.snapshot())
                    .build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonPropertyOrder(alphabetic = true)
    public static class IndexBO {

        private String name;

        private Boolean unique;

        @Builder.Default
        private List<String> columns = new ArrayList<>();

        private String predicate;

        private SourcePositionBO sourcePosition;

        IndexBO snapshot() {
            return IndexBO.builder()
                    .name(name)
                    .unique(unique)
                    .columns(CodegenConfigBO.copyList(columns))
                    .predicate(predicate)
                    .sourcePosition(sourcePosition == null ? null : sourcePosition.snapshot())
                    .build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonPropertyOrder(alphabetic = true)
    public static class SourcePositionBO {

        private String file;

        private Integer line;

        private Integer column;

        private Integer offset;

        SourcePositionBO snapshot() {
            return SourcePositionBO.builder()
                    .file(file)
                    .line(line)
                    .column(column)
                    .offset(offset)
                    .build();
        }
    }
}
