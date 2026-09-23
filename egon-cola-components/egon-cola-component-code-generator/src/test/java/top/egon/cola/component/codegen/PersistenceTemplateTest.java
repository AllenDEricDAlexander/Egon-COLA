package top.egon.cola.component.codegen;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.egon.cola.component.codegen.ddl.DdlSchemaService;
import top.egon.cola.component.codegen.ddl.PostgreDdlAdapter;
import top.egon.cola.component.codegen.model.CodegenConfigBO;
import top.egon.cola.component.codegen.model.CodegenPlanBO;
import top.egon.cola.component.codegen.model.CodegenProfileEnum;
import top.egon.cola.component.codegen.model.CodegenSchemaBO;
import top.egon.cola.component.codegen.profile.ProjectLayoutStrategy;
import top.egon.cola.component.codegen.template.FreeMarkerTemplateService;
import top.egon.cola.component.codegen.validation.GenerationScopeValidator;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistenceTemplateTest {

    private static CodegenSchemaBO orders;

    private final GenerationScopeValidator scope = new GenerationScopeValidator();

    private final FreeMarkerTemplateService templates = new FreeMarkerTemplateService();

    @BeforeAll
    static void loadOrders() {
        orders = new DdlSchemaService(new PostgreDdlAdapter()).read(CodegenConfigBO.builder()
                .input(CodegenConfigBO.InputBO.builder()
                        .mode("schema")
                        .schemaFiles(List.of("src/test/resources/ddl/schema.sql"))
                        .build())
                .build());
    }

    @Test
    void threeProfilesRenderPersistenceAndJavaCompiles(@TempDir Path output) throws Exception {
        for (CodegenProfileEnum profile : List.of(CodegenProfileEnum.LIGHT, CodegenProfileEnum.WEB, CodegenProfileEnum.SERVICE)) {
            CodegenConfigBO config = config(profile, List.of("po", "dao", "mapper-xml", "repo"));
            assertTrue(scope.validate(config, orders).isEmpty(), profile.code());
            Map<String, String> rendered = render(config);
            assertEquals(4, rendered.size());
            String xml = rendered.get("mapper-xml");
            assertTrue(xml.contains("id=\"selectActiveById\""));
            assertTrue(xml.contains("id=\"selectActiveByIds\""));
            assertTrue(xml.contains("id=\"deleteVersionedById\""));
            assertTrue(xml.contains("#{id}"));
            assertTrue(xml.contains("#{et.id}"));
            assertTrue(xml.contains("#{MP_OPTLOCK_VERSION_ORIGINAL}"));
            assertTrue(xml.contains("deleted_at IS NULL"));
            assertTrue(xml.contains("ids.size()"));
            assertFalse(xml.contains("SELECT *"));
            String repo = rendered.get("repo");
            assertFalse(repo.contains("boolean save("));
            assertFalse(repo.contains("removeById"));
            assertFalse(repo.contains("updateById"));
            assertTrue(repo.contains("@Repository(\"ordersRepository\")"));
            assertTrue(rendered.get("po").contains("@SuperBuilder"));
            assertTrue(rendered.get("po").contains("extends EgonModel<OrdersPO>"));
            for (String inherited : List.of("id", "tenantId", "createUserId", "createTime",
                    "updateUserId", "updateTime", "deletedAt", "version")) {
                assertFalse(rendered.get("po").matches("(?s).*private\\s+\\w+\\s+" + inherited + "\\s*;.*"), inherited);
            }
            assertTrue(rendered.get("po").contains("extends EgonModel<OrdersPO>"));
            assertFalse(rendered.get("po").contains("private Long id"));
            if (profile == CodegenProfileEnum.LIGHT) {
                assertTrue(path(config, "po").startsWith("src/main/java/"));
            } else {
                assertTrue(path(config, "po").startsWith("order-service-infrastructure/"));
            }
            assertFalse(path(config, "po").contains(".."));
        }
        assertFalse(ProjectLayoutStrategy.resolve(CodegenProfileEnum.SERVICE).allows("controller"));
        assertTrue(ProjectLayoutStrategy.resolve(CodegenProfileEnum.WEB).allows("controller"));
        compile(render(config(CodegenProfileEnum.LIGHT, List.of("po", "dao", "repo"))), output);
        assertFalse(Files.exists(Path.of("target", "codegen-persistence-no-write")));
    }

    @Test
    void repositoryOnlyKeepsExistingTypesAndDoesNotAddArtifacts() {
        CodegenConfigBO config = config(CodegenProfileEnum.WEB, List.of("repo"));
        config.setExistingTypes(new LinkedHashMap<>(Map.of(
                "po", "com.example.order.infrastructure.order.po.OrdersPO",
                "dao", "com.example.order.infrastructure.order.dao.OrdersDAO")));
        assertTrue(scope.validate(config, orders).isEmpty());
        assertEquals(List.of("repo"), config.getArtifacts());
        Map<String, String> rendered = render(config);
        assertEquals(1, rendered.size());
        assertTrue(rendered.get("repo").contains("OrdersDAO"));
        assertTrue(rendered.get("repo").contains("OrdersPO"));
    }

    @Test
    void compositeIdentityAndLegacyUniqueKeyBlockGeneration() {
        CodegenSchemaBO.TableBO original = orders.getTables().get(0);
        List<CodegenSchemaBO.ConstraintBO> constraints = new ArrayList<>(original.getConstraints());
        constraints.add(CodegenSchemaBO.ConstraintBO.builder()
                .kind("PRIMARY_KEY").columns(List.of("id", "tenant_id")).build());
        CodegenSchemaBO.TableBO composite = CodegenSchemaBO.TableBO.builder()
                .logicalName(original.getLogicalName())
                .columns(original.getColumns())
                .constraints(constraints)
                .indexes(original.getIndexes())
                .build();
        CodegenSchemaBO compositeSchema = CodegenSchemaBO.builder().tables(List.of(composite)).build();
        assertTrue(codes(scope.validate(config(CodegenProfileEnum.LIGHT, List.of("po")), compositeSchema))
                .contains(GenerationScopeValidator.UNSUPPORTED_ID));

        List<CodegenSchemaBO.IndexBO> indexes = new ArrayList<>(original.getIndexes());
        indexes.add(CodegenSchemaBO.IndexBO.builder()
                .name("orders_code_only").unique(true).columns(List.of("tenant_id", "code")).build());
        CodegenSchemaBO.TableBO legacy = CodegenSchemaBO.TableBO.builder()
                .logicalName(original.getLogicalName())
                .columns(original.getColumns())
                .constraints(original.getConstraints())
                .indexes(indexes)
                .build();
        CodegenSchemaBO legacySchema = CodegenSchemaBO.builder().tables(List.of(legacy)).build();
        assertTrue(codes(scope.validate(config(CodegenProfileEnum.SERVICE, List.of("dao")), legacySchema))
                .contains(GenerationScopeValidator.LEGACY_UNIQUE_KEY));
    }

    @Test
    void inheritedAuditColumnsRemainInDdlButNotInThePojo() {
        CodegenSchemaBO withoutAudit = orders.snapshot();
        withoutAudit.getTables().get(0).getColumns().removeIf(column -> "create_time".equals(column.getName()));
        assertTrue(codes(scope.validate(config(CodegenProfileEnum.LIGHT, List.of("po")), withoutAudit))
                .contains(GenerationScopeValidator.BASE_FIELD_TYPE));
    }

    private Map<String, String> render(CodegenConfigBO config) {
        CodegenSchemaBO.TableBO table = orders.getTables().get(0);
        Map<String, String> rendered = new LinkedHashMap<>();
        for (String artifact : config.getArtifacts()) {
            Map<String, Object> model = scope.renderModel(config, table, artifact);
            rendered.put(artifact, new String(templates.render(artifact, model), StandardCharsets.UTF_8));
        }
        return rendered;
    }

    private String path(CodegenConfigBO config, String artifact) {
        return ProjectLayoutStrategy.resolve(config.getProfile()).relativePath(config, artifact, "OrdersPO.java");
    }

    private static List<String> codes(List<CodegenPlanBO.DiagnosticBO> diagnostics) {
        return diagnostics.stream().map(CodegenPlanBO.DiagnosticBO::getCode).toList();
    }

    private static CodegenConfigBO config(CodegenProfileEnum profile, List<String> artifacts) {
        CodegenConfigBO.CodegenConfigBOBuilder builder = CodegenConfigBO.builder()
                .configVersion(1)
                .profile(profile)
                .basePackage("com.example.order")
                .domain("order")
                .outputRoot("target/codegen-persistence-no-write")
                .artifacts(new ArrayList<>(artifacts))
                .tables(new ArrayList<>(List.of("orders")));
        if (profile != CodegenProfileEnum.LIGHT) {
            builder.roots(new LinkedHashMap<>(Map.of(
                    "infrastructure", "order-service-infrastructure",
                    "domain", "order-service-domain",
                    "application", "order-service-application",
                    "adapter", "order-service-adapter")));
        }
        return builder.build();
    }

    private static void compile(Map<String, String> sources, Path output) throws Exception {
        Path sourceRoot = output.resolve("src");
        Files.createDirectories(sourceRoot);
        List<Path> files = new ArrayList<>();
        for (Map.Entry<String, String> source : sources.entrySet()) {
            String simple = switch (source.getKey()) {
                case "po" -> "OrdersPO.java";
                case "dao" -> "OrdersDAO.java";
                case "repo" -> "OrdersRepository.java";
                default -> throw new IllegalArgumentException(source.getKey());
            };
            Path file = sourceRoot.resolve(simple);
            Files.writeString(file, source.getValue());
            files.add(file);
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Path classes = output.resolve("classes");
        Files.createDirectories(classes);
        List<String> arguments = new ArrayList<>(List.of(
                "-classpath", System.getProperty("java.class.path") + java.io.File.pathSeparator + lombokJar(),
                "-processorpath", lombokJar().toString(),
                "-d", classes.toString()));
        arguments.addAll(files.stream().map(Path::toString).toList());
        ByteArrayOutputStream writer = new ByteArrayOutputStream();
        int result = compiler.run(null, writer, writer, arguments.toArray(String[]::new));
        assertEquals(0, result, writer.toString(StandardCharsets.UTF_8));
        assertTrue(Files.exists(classes.resolve("com/example/order/infrastructure/order/po/OrdersPO.class")));
    }

    private static Path lombokJar() {
        List<Path> candidates = List.of(
                Path.of(System.getProperty("user.home"), "maven/repository/org/projectlombok/lombok/1.18.46/lombok-1.18.46.jar"),
                Path.of(System.getProperty("user.home"), ".m2/repository/org/projectlombok/lombok/1.18.46/lombok-1.18.46.jar"));
        return candidates.stream().filter(Files::exists).findFirst().orElseThrow();
    }
}
