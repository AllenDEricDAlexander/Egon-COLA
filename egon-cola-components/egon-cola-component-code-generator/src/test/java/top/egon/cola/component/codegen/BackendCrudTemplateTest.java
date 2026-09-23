package top.egon.cola.component.codegen;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.egon.cola.component.codegen.ddl.DdlSchemaService;
import top.egon.cola.component.codegen.ddl.PostgreDdlAdapter;
import top.egon.cola.component.codegen.model.CodegenConfigBO;
import top.egon.cola.component.codegen.model.CodegenProfileEnum;
import top.egon.cola.component.codegen.model.CodegenSchemaBO;
import top.egon.cola.component.codegen.profile.ProjectLayoutStrategy;
import top.egon.cola.component.codegen.template.FreeMarkerTemplateService;
import top.egon.cola.component.codegen.validation.GenerationScopeValidator;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
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

class BackendCrudTemplateTest {

    private static final List<String> BACKEND = List.of(
            "domain-model", "domain-query", "command", "query", "result", "converter",
            "domain-service", "domain-impl", "manage", "manage-impl", "po", "dao", "mapper-xml", "repo");

    private static CodegenSchemaBO orders;

    private final GenerationScopeValidator scope = new GenerationScopeValidator();

    private final FreeMarkerTemplateService templates = new FreeMarkerTemplateService();

    @BeforeAll
    static void loadOrders() {
        orders = new DdlSchemaService(new PostgreDdlAdapter()).read(CodegenConfigBO.builder()
                .input(CodegenConfigBO.InputBO.builder().mode("schema")
                        .schemaFiles(List.of("src/test/resources/ddl/schema.sql")).build())
                .build());
    }

    @Test
    void layeredCrudKeepsBoundariesAndVersion(@TempDir Path output) throws Exception {
        Map<String, String> light = render(config(CodegenProfileEnum.LIGHT, withController(true)));
        Map<String, String> service = render(config(CodegenProfileEnum.SERVICE, withController(false)));
        assertFalse(service.containsKey("controller"));
        assertTrue(light.containsKey("controller"));
        assertFalse(ProjectLayoutStrategy.resolve(CodegenProfileEnum.SERVICE).allows("controller"));

        assertFalse(light.get("domain-service").contains(".po."));
        assertFalse(light.get("domain-service").contains("mybatis"));
        assertFalse(light.get("domain-model").contains("EgonModel"));
        assertFalse(light.get("domain-impl").contains(".application."));
        assertTrue(light.get("repo").contains("selectByQuery"));
        assertTrue(light.get("domain-impl").contains("repository.selectByQuery"));
        assertFalse(light.get("domain-impl").contains("getBaseMapper()"));
        assertFalse(light.get("controller").contains("Repository"));
        assertFalse(light.get("controller").contains("DomainService"));
        assertTrue(light.get("controller").contains("manage.detail"));
        assertTrue(light.get("controller").contains("manage.page"));
        assertFalse(light.get("controller").contains("repository."));
        assertFalse(light.get("create-command").contains("tenantId"));
        assertFalse(light.get("create-command").contains("createTime"));
        assertFalse(light.get("result").contains("tenantId"));
        assertTrue(light.get("update-converter").contains("expectedVersion"));
        assertTrue(light.get("domain-impl").contains("po.setVersion(value.getVersion())"));
        assertTrue(light.get("domain-impl").contains("repository.removeById(po)"));
        assertFalse(light.get("domain-impl").contains("removeById(id)"));
        assertFalse(light.get("domain-impl").contains("removeById(value.getId())"));
        assertTrue(light.get("domain-impl").contains("zeroRows"));
        assertTrue(light.get("manage-impl").contains("domain.getVersion()"));

        exerciseManage(light, output);
    }

    private void exerciseManage(Map<String, String> sources, Path output) throws Exception {
        Path sourceRoot = output.resolve("src");
        write(sourceRoot, "com/example/order/support/OrdersErrorMapper.java", """
                package com.example.order.support;
                public final class OrdersErrorMapper {
                    private OrdersErrorMapper() {}
                    public static RuntimeException zeroRows(String operation, Long id, Long version) {
                        return new IllegalStateException(operation + ":" + id + ":" + version);
                    }
                    public static RuntimeException missing(Long id) {
                        return new IllegalStateException("missing:" + id);
                    }
                }
                """);
        write(sourceRoot, "com/example/order/domain/order/FakeOrdersDomainService.java", """
                package com.example.order.domain.order;
                import top.egon.cola.component.common.core.pojo.PageSlice;
                public class FakeOrdersDomainService implements OrdersDomainService {
                    public OrdersBO updated;
                    public Long deletedVersion;
                    public OrdersBO create(OrdersBO value) { return value; }
                    public OrdersBO update(OrdersBO value) { this.updated = value; return value; }
                    public void delete(Long id, Long expectedVersion) { this.deletedVersion = expectedVersion; }
                    public OrdersBO detail(Long id) { return new OrdersBO().setId(id); }
                    public PageSlice<OrdersBO> query(OrdersDomainQuery query) { return PageSlice.of(java.util.List.of(), false); }
                }
                """);
        for (Map.Entry<String, String> source : sources.entrySet()) {
            if (source.getKey().endsWith("-xml") || "mapper-xml".equals(source.getKey()) || "controller".equals(source.getKey())) {
                continue;
            }
            String fileName = fileName(source.getKey(), source.getValue());
            String packageName = packageName(source.getValue());
            write(sourceRoot, packageName.replace('.', '/') + "/" + fileName, source.getValue());
        }
        Path classes = output.resolve("classes");
        Files.createDirectories(classes);
        List<Path> files = Files.walk(sourceRoot).filter(path -> path.toString().endsWith(".java")).toList();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        String classpath = System.getProperty("java.class.path") + java.io.File.pathSeparator + lombokJar()
                + java.io.File.pathSeparator + mapstructJar();
        List<String> arguments = new ArrayList<>();
        arguments.add("-classpath");
        arguments.add(classpath);
        arguments.add("-processorpath");
        arguments.add(lombokJar() + java.io.File.pathSeparator + mapstructProcessor());
        arguments.add("-d");
        arguments.add(classes.toString());
        files.forEach(file -> arguments.add(file.toString()));
        ByteArrayOutputStream log = new ByteArrayOutputStream();
        int compiled = compiler.run(null, log, log, arguments.toArray(String[]::new));
        assertEquals(0, compiled, log.toString(StandardCharsets.UTF_8));

        try (URLClassLoader loader = new URLClassLoader(new URL[]{classes.toUri().toURL()}, getClass().getClassLoader())) {
            Class<?> manageClass = loader.loadClass("com.example.order.application.order.OrdersManageImpl");
            Object fake = loader.loadClass("com.example.order.domain.order.FakeOrdersDomainService").getConstructor().newInstance();
            Object createConverter = loader.loadClass("com.example.order.application.order.converter.CreateOrdersCommandConverterImpl").getConstructor().newInstance();
            Object updateConverter = loader.loadClass("com.example.order.application.order.converter.UpdateOrdersCommandConverterImpl").getConstructor().newInstance();
            Object deleteConverter = loader.loadClass("com.example.order.application.order.converter.DeleteOrdersCommandConverterImpl").getConstructor().newInstance();
            Object resultConverter = loader.loadClass("com.example.order.application.order.converter.OrdersResultConverterImpl").getConstructor().newInstance();
            Constructor<?> constructor = manageClass.getDeclaredConstructors()[0];
            Object manage = constructor.newInstance(fake, createConverter, updateConverter, deleteConverter, resultConverter);
            Class<?> updateCommand = loader.loadClass("com.example.order.application.order.UpdateOrdersCommand");
            Object command = updateCommand.getConstructor().newInstance();
            updateCommand.getMethod("setId", Long.class).invoke(command, 5L);
            updateCommand.getMethod("setExpectedVersion", Long.class).invoke(command, 7L);
            updateCommand.getMethod("setCode", String.class).invoke(command, "A-1");
            manageClass.getMethod("update", updateCommand).invoke(manage, command);
            Object updated = fake.getClass().getField("updated").get(fake);
            assertEquals(7L, updated.getClass().getMethod("getVersion").invoke(updated));
            Class<?> deleteCommand = loader.loadClass("com.example.order.application.order.DeleteOrdersCommand");
            Object deletion = deleteCommand.getConstructor().newInstance();
            deleteCommand.getMethod("setId", Long.class).invoke(deletion, 5L);
            deleteCommand.getMethod("setExpectedVersion", Long.class).invoke(deletion, 9L);
            manageClass.getMethod("delete", deleteCommand).invoke(manage, deletion);
            assertEquals(9L, fake.getClass().getField("deletedVersion").get(fake));
        }
    }

    private Map<String, String> render(CodegenConfigBO config) {
        assertTrue(scope.validate(config, orders).isEmpty());
        CodegenSchemaBO.TableBO table = orders.getTables().get(0);
        Map<String, String> rendered = new LinkedHashMap<>();
        for (String artifact : config.getArtifacts()) {
            Map<String, Object> model = new LinkedHashMap<>(scope.renderModel(config, table, artifact));
            switch (artifact) {
                case "command" -> {
                    rendered.put("create-command", text("command", model, "commandKind", "create"));
                    rendered.put("update-command", text("command", model, "commandKind", "update"));
                    rendered.put("delete-command", text("command", model, "commandKind", "delete"));
                }
                case "query" -> {
                    rendered.put("detail-query", text("query", model, "queryKind", "detail"));
                    rendered.put("page-query", text("query", model, "queryKind", "page"));
                }
                case "converter" -> {
                    rendered.put("persistence-converter", text("converter", model, "converterKind", "persistence"));
                    rendered.put("create-converter", text("converter", model, "converterKind", "create"));
                    rendered.put("update-converter", text("converter", model, "converterKind", "update"));
                    rendered.put("delete-converter", text("converter", model, "converterKind", "delete"));
                    rendered.put("result-converter", text("converter", model, "converterKind", "result"));
                }
                default -> rendered.put(artifact, new String(templates.render(artifact, model), StandardCharsets.UTF_8));
            }
        }
        return rendered;
    }

    private String text(String templateId, Map<String, Object> model, String key, String value) {
        Map<String, Object> copy = new LinkedHashMap<>(model);
        copy.put(key, value);
        return new String(templates.render(templateId, copy), StandardCharsets.UTF_8);
    }

    private static CodegenConfigBO config(CodegenProfileEnum profile, List<String> artifacts) {
        return CodegenConfigBO.builder()
                .configVersion(1)
                .profile(profile)
                .basePackage("com.example.order")
                .domain("order")
                .outputRoot("target/codegen-backend-no-write")
                .artifacts(new ArrayList<>(artifacts))
                .tables(new ArrayList<>(List.of("orders")))
                .fieldPolicies(CodegenConfigBO.FieldPoliciesBO.builder()
                        .create(List.of("code")).update(List.of("code")).result(List.of("code"))
                        .filter(List.of("code")).sort(List.of("id")).build())
                .apiContract(CodegenConfigBO.ApiContractBO.builder()
                        .existingErrorMapper("com.example.order.support.OrdersErrorMapper")
                        .contextSymbol("com.example.order.support.OrdersContext")
                        .basePath("/api/orders").build())
                .roots(profile == CodegenProfileEnum.LIGHT ? null : new LinkedHashMap<>(Map.of(
                        "infrastructure", "order-service-infrastructure",
                        "domain", "order-service-domain",
                        "application", "order-service-application",
                        "adapter", "order-service-adapter")))
                .build();
    }

    private static List<String> withController(boolean controller) {
        List<String> artifacts = new ArrayList<>(BACKEND);
        if (controller) {
            artifacts.add("controller");
        }
        return artifacts;
    }

    private static void write(Path root, String relative, String content) throws Exception {
        Path path = root.resolve(relative);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }

    private static String packageName(String source) {
        int start = source.indexOf("package ") + "package ".length();
        return source.substring(start, source.indexOf(';', start)).trim();
    }

    private static String fileName(String key, String source) {
        int classAt = source.indexOf("public class ");
        int interfaceAt = source.indexOf("public interface ");
        int start = classAt >= 0 ? classAt + "public class ".length() : interfaceAt + "public interface ".length();
        String name = source.substring(start, source.indexOf(' ', start)).trim();
        return name + ".java";
    }

    private static Path lombokJar() {
        return jar("org/projectlombok/lombok/1.18.46/lombok-1.18.46.jar");
    }

    private static Path mapstructJar() {
        return jar("org/mapstruct/mapstruct/1.6.3/mapstruct-1.6.3.jar");
    }

    private static Path mapstructProcessor() {
        return jar("org/mapstruct/mapstruct-processor/1.6.3/mapstruct-processor-1.6.3.jar");
    }

    private static Path jar(String relative) {
        List<Path> candidates = List.of(
                Path.of(System.getProperty("user.home"), "maven/repository").resolve(relative),
                Path.of(System.getProperty("user.home"), ".m2/repository").resolve(relative));
        return candidates.stream().filter(Files::exists).findFirst().orElseThrow(() -> new IllegalStateException(relative));
    }
}
