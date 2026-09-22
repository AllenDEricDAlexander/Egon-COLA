package top.egon.cola.component.codegen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
