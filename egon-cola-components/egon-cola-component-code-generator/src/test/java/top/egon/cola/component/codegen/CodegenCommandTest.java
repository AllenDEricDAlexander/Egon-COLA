package top.egon.cola.component.codegen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.egon.cola.component.codegen.cli.CodegenCommand;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodegenCommandTest {

    private final CodegenCommand command = new CodegenCommand();

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void commandsReturnStableCodesWithoutStartingServices(@TempDir Path root) throws Exception {
        Invocation templates = invoke("templates");
        assertEquals(0, templates.code());
        JsonNode catalog = mapper.readTree(templates.out());
        assertEquals("freemarker", catalog.get("engine").asText());
        assertEquals("2.3.35", catalog.get("engineVersion").asText());
        assertEquals(2, catalog.get("planFormatVersion").asInt());
        assertTrue(catalog.path("artifacts").toString().contains("manage-impl"));
        assertTrue(templates.err().isBlank() || !templates.err().contains("SpringApplication"));

        assertEquals(2, invoke("nope").code());
        assertEquals(2, invoke("apply", "--config", "missing.json").code());

        Path config = writeConfig(root);
        Invocation check = invoke("check", "--config", config.toString());
        assertEquals(8, check.code());
        assertFalse(Files.exists(root.resolve("order-service-infrastructure")));

        Invocation plan = invoke("plan", "--config", config.toString());
        assertEquals(0, plan.code(), plan.err());
        String planId = mapper.readTree(plan.out()).get("planId").asText();
        Invocation apply = invoke("apply", "--config", config.toString(), "--plan", planId);
        assertEquals(0, apply.code(), apply.err());
        assertTrue(Files.exists(root.resolve("order-service-infrastructure/src/main/java/com/example/order/infrastructure/order/po/OrdersPO.java")));

        Invocation second = invoke("check", "--config", config.toString());
        assertEquals(0, second.code(), second.err());

        String source = Files.readString(Path.of("src/main/java/top/egon/cola/component/codegen/cli/CodegenCommand.java"));
        assertFalse(source.contains("SpringApplication"));
        assertFalse(source.contains("DriverManager"));
        assertFalse(source.contains("EgonColaPostgreDdlRunner"));
    }

    @Test
    void launcherBlocksMissingClasspath() throws Exception {
        Path script = Path.of("../../scripts/egon-codegen.sh").toAbsolutePath().normalize();
        if (!Files.exists(script)) {
            script = Path.of("scripts/egon-codegen.sh").toAbsolutePath().normalize();
        }
        Process process = new ProcessBuilder("bash", script.toString(), "templates")
                .redirectErrorStream(false)
                .start();
        String error = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        int code = process.waitFor();
        assertEquals(7, code);
        assertTrue(error.contains("BLOCKED_TOOLING"));
    }

    @Test
    void backendCrudFlowsFromRepositoryThroughServicesToController(@TempDir Path root) throws Exception {
        Path config = writeConfig(root);
        String json = Files.readString(config).replace(
                "[\"po\", \"dao\", \"mapper-xml\", \"repo\"]", "[\"backend-crud\"]");
        Files.writeString(config, json);
        Invocation planned = invoke("plan", "--config", config.toString());
        assertEquals(0, planned.code(), planned.err());
        String planId = mapper.readTree(planned.out()).path("planId").asText();
        assertEquals(22, mapper.readTree(planned.out()).path("fileCount").asInt());
        assertTrue(mapper.readTree(planned.out()).path("files").toString().contains("OrdersManageImpl.java"));
        assertEquals(0, invoke("apply", "--config", config.toString(), "--plan", planId).code());

        Path infrastructure = root.resolve("order-service-infrastructure/src/main/java/com/example/order/infrastructure/order");
        Path application = root.resolve("order-service-application/src/main/java/com/example/order/application/order");
        Path adapter = root.resolve("order-service-adapter/src/main/java/com/example/order/adapter/order");
        assertTrue(Files.readString(infrastructure.resolve("repo/OrdersRepository.java")).contains("EgonColaRepository"));
        assertTrue(Files.readString(infrastructure.resolve("repo/OrdersRepository.java")).contains("selectByQuery"));
        assertTrue(Files.readString(infrastructure.resolve("service/impl/OrdersDomainServiceImpl.java"))
                .contains("repository.removeById(po)"));
        assertTrue(Files.readString(infrastructure.resolve("service/impl/OrdersDomainServiceImpl.java"))
                .contains("repository.selectByQuery"));
        assertTrue(Files.readString(application.resolve("OrdersManageImpl.java")).contains("domainService.update(domain)"));
        assertTrue(Files.readString(adapter.resolve("OrdersController.java")).contains("manage.update(command)"));

        String po = Files.readString(infrastructure.resolve("po/OrdersPO.java"));
        assertTrue(po.contains("extends EgonModel<OrdersPO>"));
        for (String inherited : new String[]{"id", "tenantId", "createUserId", "createTime",
                "updateUserId", "updateTime", "deletedAt", "version"}) {
            assertFalse(po.matches("(?s).*private\\s+\\w+\\s+" + inherited + "\\s*;.*"), inherited);
        }
        assertEquals(0, invoke("check", "--config", config.toString()).code());
    }

    @Test
    void serviceBackendCrudHasNoHttpController(@TempDir Path root) throws Exception {
        Path config = writeConfig(root);
        String json = Files.readString(config)
                .replace("\"projectType\": \"web\"", "\"projectType\": \"service\"")
                .replace("[\"po\", \"dao\", \"mapper-xml\", \"repo\"]", "[\"backend-crud\"]");
        Files.writeString(config, json);
        Invocation planned = invoke("plan", "--config", config.toString());
        assertEquals(0, planned.code(), planned.err());
        assertEquals(21, mapper.readTree(planned.out()).path("fileCount").asInt());
        assertFalse(mapper.readTree(planned.out()).path("files").toString().contains("Controller.java"));
        String planId = mapper.readTree(planned.out()).path("planId").asText();
        assertEquals(0, invoke("apply", "--config", config.toString(), "--plan", planId).code());
        assertFalse(Files.exists(root.resolve("order-service-adapter/src/main/java/com/example/order/adapter/order/OrdersController.java")));
    }

    @Test
    void oldPlanRejectsChangedConfigurationAndDdl(@TempDir Path root) throws Exception {
        Path config = writeConfig(root);
        Path schema = root.resolve("schema.sql");
        Files.copy(Path.of("src/test/resources/ddl/schema.sql"), schema);
        String original = Files.readString(config).replace(
                Path.of("src/test/resources/ddl/schema.sql").toAbsolutePath().toString(), schema.toString());
        Files.writeString(config, original);
        Invocation planned = invoke("plan", "--config", config.toString());
        assertEquals(0, planned.code(), planned.err());
        String planId = mapper.readTree(planned.out()).path("planId").asText();

        Files.writeString(config, original.replace("\"result\": [\"code\"]", "\"result\": []"));
        Invocation changedConfig = invoke("apply", "--config", config.toString(), "--plan", planId);
        assertEquals(6, changedConfig.code(), changedConfig.err());
        Files.writeString(config, original);

        Files.writeString(schema, Files.readString(schema) + "\nCOMMENT ON TABLE orders IS 'changed';\n");
        Invocation changedDdl = invoke("apply", "--config", config.toString(), "--plan", planId);
        assertEquals(6, changedDdl.code(), changedDdl.err());
    }

    @Test
    void oldFormatAndEditedPlanCannotApply(@TempDir Path root) throws Exception {
        Path config = writeConfig(root);
        Invocation planned = invoke("plan", "--config", config.toString());
        assertEquals(0, planned.code(), planned.err());
        String planId = mapper.readTree(planned.out()).path("planId").asText();
        Path planFile = root.resolve(".egon/codegen/plans/" + planId + ".json");
        ObjectNode document = (ObjectNode) mapper.readTree(planFile.toFile());
        document.put("formatVersion", 1);
        mapper.writeValue(planFile.toFile(), document);
        assertEquals(6, invoke("apply", "--config", config.toString(), "--plan", planId).code());
        document.put("formatVersion", 2);
        ((ObjectNode) document.path("files").get(0)).put("operation", "UPDATE");
        mapper.writeValue(planFile.toFile(), document);
        assertEquals(6, invoke("apply", "--config", config.toString(), "--plan", planId).code());
        assertFalse(Files.exists(root.resolve("order-service-infrastructure")));
    }

    private Path writeConfig(Path root) throws Exception {
        String json = Files.readString(Path.of("src/test/resources/codegen-web.json"))
                .replace("target/codegen-web-example", root.toString())
                .replace("src/test/resources/ddl/schema.sql",
                        Path.of("src/test/resources/ddl/schema.sql").toAbsolutePath().toString());
        Path config = root.resolve("codegen.json");
        Files.writeString(config, json);
        return config;
    }

    private Invocation invoke(String... args) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream error = new ByteArrayOutputStream();
        int code = command.run(args, new PrintStream(output), new PrintStream(error));
        return new Invocation(code, output.toString(StandardCharsets.UTF_8), error.toString(StandardCharsets.UTF_8));
    }

    private record Invocation(int code, String out, String err) {
    }
}
