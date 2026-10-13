package top.egon.cola.component.codegen;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.codegen.ddl.DdlSchemaService;
import top.egon.cola.component.codegen.ddl.PostgreDdlAdapter;
import top.egon.cola.component.codegen.model.CodegenConfigBO;
import top.egon.cola.component.codegen.model.CodegenProfileEnum;
import top.egon.cola.component.codegen.model.CodegenSchemaBO;
import top.egon.cola.component.codegen.template.TemplateContextService;
import top.egon.cola.component.codegen.validation.GenerationScopeValidator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeTypeMappingTest {

    static final List<String> TIME_FIELDS = List.of("code", "aware_at", "alias_at", "local_at", "on_date", "at_time");

    private final GenerationScopeValidator scope = new GenerationScopeValidator();

    private final TemplateContextService contexts = new TemplateContextService();

    @Test
    void bothRenderEntrypointsKeepDistinctTimeSemantics() throws Exception {
        CodegenSchemaBO schema = temporalSchema();
        CodegenSchemaBO.TableBO table = schema.getTables().get(0);
        CodegenConfigBO config = config();
        assertTrue(scope.validate(config, schema).isEmpty());
        Map<String, Object> context = contexts.context(schema, table, CodegenProfileEnum.LIGHT, "po", null);
        Map<String, Object> model = scope.renderModel(config, table, "po");
        Map<String, String> expected = Map.of(
                "aware_at", "java.time.Instant", "alias_at", "java.time.Instant",
                "local_at", "java.time.LocalDateTime", "on_date", "java.time.LocalDate", "at_time", "java.time.LocalTime");
        expected.forEach((name, type) -> {
            assertEquals(type.substring(type.lastIndexOf('.') + 1), field(context, "fields", name).get("javaType"), name);
            assertEquals(type, field(model, "businessFields", name).get("javaType"), name);
        });
        assertEquals(List.of("java.time.Instant", "java.time.LocalDate", "java.time.LocalDateTime", "java.time.LocalTime"),
                context.get("imports"));
    }

    @Test
    void aliasesCaseWhitespaceAndPrecisionKeepTheSameMapping() throws Exception {
        for (String type : List.of("TIMESTAMPTZ", " timestamptz(6) ", "timestamp (6)  WITH   TIME ZONE")) {
            CodegenSchemaBO schema = temporalSchema();
            column(schema, "aware_at").setSqlType(type);
            assertEquals("Instant", field(contexts.context(schema, schema.getTables().get(0),
                    CodegenProfileEnum.LIGHT, "po", null), "fields", "aware_at").get("javaType"));
            assertEquals("java.time.Instant", field(scope.renderModel(config(), schema.getTables().get(0), "po"),
                    "businessFields", "aware_at").get("javaType"));
        }
    }

    @Test
    void unsupportedTemporalTypesFailBeforeRendering() throws Exception {
        for (String type : List.of("timetz", "time with time zone", "interval", "timestamp[]",
                "timestamptz[]", "date[]", "time[]", "timestamp(7)", "time(-1)")) {
            CodegenSchemaBO schema = temporalSchema();
            column(schema, "aware_at").setSqlType(type);
            assertTrue(scope.validate(config(), schema).stream().anyMatch(diagnostic ->
                    GenerationScopeValidator.UNKNOWN_SQL_TYPE.equals(diagnostic.getCode())), type);
            assertThrows(IllegalArgumentException.class, () -> contexts.context(schema,
                    schema.getTables().get(0), CodegenProfileEnum.LIGHT, "po", null), type);
        }
    }

    @Test
    void unsupportedSqlTemporalShapesRemainVisibleToTheScope() throws Exception {
        String baseline = Files.readString(Path.of("src/test/resources/ddl/schema.sql"));
        for (String type : List.of("INTERVAL", "TIMETZ", "TIME WITH TIME ZONE", "TIMESTAMP[]", "DATE[]", "TIME[]", "TIMESTAMPTZ[]")) {
            CodegenSchemaBO schema = readSql(baseline.replace("code VARCHAR(64)", "code VARCHAR(64), unsupported_at " + type));
            assertTrue(scope.validate(config(), schema).stream().anyMatch(diagnostic ->
                    GenerationScopeValidator.UNKNOWN_SQL_TYPE.equals(diagnostic.getCode())), type);
            assertThrows(IllegalArgumentException.class, () -> contexts.context(schema,
                    schema.getTables().get(0), CodegenProfileEnum.LIGHT, "po", null), type);
        }
    }

    @Test
    void baseAuditAliasesRetainTheLegacyDeletionBoundary() throws Exception {
        CodegenSchemaBO schema = temporalSchema();
        column(schema, "create_time").setSqlType("timestamptz");
        column(schema, "update_time").setSqlType("timestamptz");
        assertTrue(scope.validate(config(), schema).isEmpty());
        column(schema, "deleted_at").setSqlType("timestamptz");
        assertTrue(scope.validate(config(), schema).stream().anyMatch(diagnostic ->
                GenerationScopeValidator.BASE_FIELD_TYPE.equals(diagnostic.getCode())));
        schema = temporalSchema();
        column(schema, "create_time").setSqlType("timestamp");
        assertTrue(scope.validate(config(), schema).stream().anyMatch(diagnostic ->
                GenerationScopeValidator.BASE_FIELD_TYPE.equals(diagnostic.getCode())));
    }

    static CodegenSchemaBO temporalSchema() throws IOException {
        String sql = Files.readString(Path.of("src/test/resources/ddl/schema.sql"))
                .replace("code VARCHAR(64)\n", """
                        code VARCHAR(64),
                        aware_at TIMESTAMP(6) WITH TIME ZONE,
                        alias_at TIMESTAMPTZ(6),
                        local_at TIMESTAMP(6) WITHOUT TIME ZONE,
                        on_date DATE,
                        at_time TIME(6) WITHOUT TIME ZONE
                        """);
        return readSql(sql);
    }

    static CodegenSchemaBO readSql(String sql) throws IOException {
        Path script = Files.createTempFile("egon-time-types-", ".sql");
        try {
            Files.writeString(script, sql);
            return new DdlSchemaService(new PostgreDdlAdapter()).read(CodegenConfigBO.builder()
                    .input(CodegenConfigBO.InputBO.builder().mode("schema").schemaFiles(List.of(script.toString())).build())
                    .build());
        } finally {
            Files.deleteIfExists(script);
        }
    }

    private static CodegenConfigBO config() {
        return CodegenConfigBO.builder().profile(CodegenProfileEnum.LIGHT).artifacts(List.of("po"))
                .basePackage("com.example.order").domain("order").build();
    }

    private static CodegenSchemaBO.ColumnBO column(CodegenSchemaBO schema, String name) {
        return schema.getTables().get(0).getColumns().stream().filter(column -> name.equals(column.getName()))
                .findFirst().orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> field(Map<String, Object> model, String key, String name) {
        return ((List<Map<String, Object>>) model.get(key)).stream()
                .filter(field -> name.equals(field.get("column"))).findFirst().orElseThrow();
    }
}
