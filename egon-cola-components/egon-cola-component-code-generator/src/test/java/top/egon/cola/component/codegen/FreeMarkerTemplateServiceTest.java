package top.egon.cola.component.codegen;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.codegen.model.CodegenConfigBO;
import top.egon.cola.component.codegen.model.CodegenProfileEnum;
import top.egon.cola.component.codegen.model.CodegenSchemaBO;
import top.egon.cola.component.codegen.template.FreeMarkerTemplateService;
import top.egon.cola.component.codegen.template.FreeMarkerTemplateService.TemplateRenderException;
import top.egon.cola.component.codegen.template.TemplateContextService;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FreeMarkerTemplateServiceTest {

    private final FreeMarkerTemplateService service = new FreeMarkerTemplateService();

    @Test
    void squareBracketTemplateKeepsLiteralsAndDoesNotDoubleEscape() {
        byte[] first = service.render("probe", Map.of("name", "订单Order", "xml", "a&lt;b&amp;c"));
        byte[] second = service.render("probe", Map.of("name", "订单Order", "xml", "a&lt;b&amp;c"));
        assertArrayEquals(first, second);
        String text = new String(first, StandardCharsets.UTF_8);
        assertTrue(text.contains("#{et.id}"));
        assertTrue(text.contains("${existing.property}"));
        assertTrue(text.contains("a&lt;b&amp;c"));
        assertFalse(text.contains("&amp;lt;"));
        assertTrue(text.contains("订单Order"));
        assertFalse(Files.exists(Path.of("target", "codegen-template-candidate")));
    }

    @Test
    void missingRequiredValueAndUnknownTemplateProduceNoCandidate() {
        TemplateRenderException missing = assertThrows(TemplateRenderException.class,
                () -> service.render("probe", Map.of("xml", "ok")));
        assertEquals(FreeMarkerTemplateService.TEMPLATE_ERROR, missing.getCode());
        TemplateRenderException unknown = assertThrows(TemplateRenderException.class,
                () -> service.render("not-a-template", Map.of("name", "x", "xml", "y")));
        assertEquals(FreeMarkerTemplateService.UNKNOWN_TEMPLATE, unknown.getCode());
        assertThrows(TemplateRenderException.class, () -> service.render("probe", null));
    }

    @Test
    void newAndApiBuiltinsAreBlocked() {
        TemplateRenderException exception = assertThrows(TemplateRenderException.class,
                () -> service.render("unsafe", Map.of()));
        assertEquals(FreeMarkerTemplateService.TEMPLATE_ERROR, exception.getCode());
    }

    @Test
    void contextIsAReadOnlyScalarModel() {
        CodegenSchemaBO.TableBO table = CodegenSchemaBO.TableBO.builder()
                .logicalName("orders")
                .role("MASTER_DATA")
                .columns(List.of(
                        CodegenSchemaBO.ColumnBO.builder().name("code").sqlType("varchar").length(64).build(),
                        CodegenSchemaBO.ColumnBO.builder().name("created_at").sqlType("timestamp").build()))
                .build();
        Map<String, Object> context = new TemplateContextService().context(
                CodegenSchemaBO.builder().tables(List.of(table)).build(),
                table,
                CodegenProfileEnum.WEB,
                "result",
                CodegenConfigBO.FieldPoliciesBO.builder().create(List.of("code")).update(List.of("code"))
                        .result(List.of("code")).filter(List.of("code")).sort(List.of("code")).build());
        assertEquals("web", context.get("profile"));
        @SuppressWarnings("unchecked")
        List<Map<String, String>> fields = (List<Map<String, String>>) context.get("fields");
        assertEquals(1, fields.size());
        assertEquals("code", fields.get(0).get("javaName"));
        assertThrows(UnsupportedOperationException.class, () -> context.remove("profile"));
    }
}
