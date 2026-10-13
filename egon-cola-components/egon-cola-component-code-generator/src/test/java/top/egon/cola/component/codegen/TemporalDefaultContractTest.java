package top.egon.cola.component.codegen;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.codegen.model.CodegenConfigBO;
import top.egon.cola.component.codegen.model.CodegenProfileEnum;
import top.egon.cola.component.codegen.model.CodegenSchemaBO;
import top.egon.cola.component.codegen.template.TemplateContextService;
import top.egon.cola.component.codegen.validation.GenerationScopeValidator;
import top.egon.cola.component.codegen.validation.TemporalDefaultValidator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemporalDefaultContractTest {

    @Test
    void utcFunctionsAndTypedLiteralsKeepTheirColumnSemantics() {
        for (String type : List.of("timestamptz", "timestamp(6) with time zone")) {
            for (String expression : List.of("CURRENT_TIMESTAMP", "CURRENT_TIMESTAMP(6)", "now()", "clock_timestamp()",
                    "'2026-10-13T01:02:03.123456Z'", "TIMESTAMPTZ '2026-10-13T01:02:03+00:00'",
                    "'2026-10-13T01:02:03Z'::timestamptz")) {
                assertTrue(supported(type, expression), type + "/" + expression);
            }
        }
        assertTrue(supported("timestamp without time zone", "(CURRENT_TIMESTAMP AT TIME ZONE 'UTC')"));
        assertTrue(supported("timestamp", "TIMESTAMP '2026-10-13 01:02:03'"));
        assertTrue(supported("date", "((CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::date)"));
        assertTrue(supported("date", "DATE '2026-10-13'"));
        assertTrue(supported("time(6) without time zone", "(now() AT TIME ZONE 'UTC')::time(6)"));
        assertTrue(supported("time", "TIME '09:00:00'"));
        assertTrue(supported("timestamp", "NULL"));
        assertTrue(supported("integer", "42"));
    }

    @Test
    void sessionDependentDefaultsWrongTypesAndFrozenNowAreRejected() {
        for (String expression : List.of("CURRENT_TIMESTAMP", "LOCALTIMESTAMP", "now()", "TIMESTAMP 'now'")) {
            assertFalse(supported("timestamp", expression), expression);
        }
        for (String expression : List.of("CURRENT_DATE", "CURRENT_TIMESTAMP::date", "DATE 'now'", "DATE '2026-02-30'")) {
            assertFalse(supported("date", expression), expression);
        }
        for (String expression : List.of("CURRENT_TIME", "LOCALTIME", "CURRENT_TIMESTAMP::time")) {
            assertFalse(supported("time", expression), expression);
        }
        for (String expression : List.of("timezone('UTC', now())", "now() AT TIME ZONE 'UTC'",
                "'2026-10-13T01:02:03+08:00'", "TIMESTAMP '2026-10-13T01:02:03Z'",
                "'2026-10-13T01:02:03Z'::timestamp", "CURRENT_TIMESTAMP(9)", "custom_now()")) {
            assertFalse(supported("timestamptz", expression), expression);
        }
    }

    @Test
    void actualCreateDefaultsAreCheckedByBothRenderingEntrypointsIncludingHiddenAuditFields() throws Exception {
        String baseline = Files.readString(Path.of("src/test/resources/ddl/schema.sql"));
        CodegenConfigBO config = CodegenConfigBO.builder().profile(CodegenProfileEnum.LIGHT).artifacts(List.of("po"))
                .basePackage("com.example.order").domain("order").build();
        for (String definition : List.of("due_on DATE DEFAULT CURRENT_DATE", "occurred_at TIMESTAMPTZ DEFAULT '2026-10-13T01:02:03+08:00'")) {
            CodegenSchemaBO schema = TimeTypeMappingTest.readSql(baseline.replace("code VARCHAR(64)", "code VARCHAR(64), " + definition));
            assertTrue(new GenerationScopeValidator().validate(config, schema).stream().anyMatch(diagnostic ->
                    TemporalDefaultValidator.TIME_DEFAULT_UNSUPPORTED.equals(diagnostic.getCode())));
            assertThrows(IllegalArgumentException.class, () -> new TemplateContextService().context(schema,
                    schema.getTables().get(0), CodegenProfileEnum.LIGHT, "po", null));
            assertThrows(IllegalArgumentException.class, () -> new GenerationScopeValidator().renderModel(config,
                    schema.getTables().get(0), "po"));
        }
        CodegenSchemaBO schema = TimeTypeMappingTest.temporalSchema();
        schema.getTables().get(0).getColumns().stream().filter(column -> "create_time".equals(column.getName()))
                .findFirst().orElseThrow().setDefaultExpression("timezone('UTC', now())");
        assertThrows(IllegalArgumentException.class, () -> new TemplateContextService().context(schema,
                schema.getTables().get(0), CodegenProfileEnum.LIGHT, "dto", null));
        CodegenSchemaBO safe = TimeTypeMappingTest.readSql(baseline.replace("code VARCHAR(64)",
                "code VARCHAR(64), due_on DATE DEFAULT ((CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::date)"));
        assertTrue(new GenerationScopeValidator().validate(config, safe).isEmpty());
    }

    private static boolean supported(String type, String expression) {
        return TemporalDefaultValidator.supports(CodegenSchemaBO.ColumnBO.builder()
                .name("time_value").sqlType(type).defaultExpression(expression).build());
    }
}
