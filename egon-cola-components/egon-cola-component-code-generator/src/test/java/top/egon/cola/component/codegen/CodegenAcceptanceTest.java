package top.egon.cola.component.codegen;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.egon.cola.component.codegen.cli.CodegenCommand;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodegenAcceptanceTest {

    private final CodegenCommand command = new CodegenCommand();

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void threeLayoutsCompileAndDoNotEmbedTheTemplateEngine(@TempDir Path root) throws Exception {
        for (String profile : List.of("light", "web", "service")) {
            Path project = root.resolve(profile);
            Files.createDirectories(project);
            Path config = project.resolve("codegen.json");
            Files.writeString(config, config(profile, project));
            assertEquals(0, run("plan", "--config", config.toString()));
            String planId = mapper.readTree(lastOut).get("planId").asText();
            assertEquals(0, run("apply", "--config", config.toString(), "--plan", planId), lastErr);
            Path javaRoot = project.resolve(profile.equals("light") ? "" : "egon-cola-source-" + profile + "-infrastructure");
            Path po = javaRoot.resolve("src/main/java/com/example/codegenfixture/infrastructure/codegenfixture/po/OrdersPO.java");
            Path repository = javaRoot.resolve("src/main/java/com/example/codegenfixture/infrastructure/codegenfixture/repo/OrdersRepository.java");
            assertTrue(Files.exists(po));
            String poSource = Files.readString(po);
            String repositorySource = Files.readString(repository);
            assertFalse(poSource.contains("freemarker"));
            assertFalse(repositorySource.contains("freemarker"));
            assertTrue(poSource.contains("package com.example.codegenfixture.infrastructure.codegenfixture.po"));
            assertTrue(repositorySource.contains("extends EgonColaRepository"));
            compile(javaRoot.resolve("src/main/java"), project.resolve("classes"));
        }
        String launcher = Files.readString(Path.of("src/main/java/top/egon/cola/component/codegen/cli/CodegenCommand.java"));
        assertFalse(launcher.contains("SpringApplication"));
        assertFalse(launcher.contains("DriverManager"));
    }

    private String lastOut = "";

    private String lastErr = "";

    private int run(String... args) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream error = new ByteArrayOutputStream();
        int code = command.run(args, new PrintStream(output), new PrintStream(error));
        lastOut = output.toString(StandardCharsets.UTF_8);
        lastErr = error.toString(StandardCharsets.UTF_8);
        return code;
    }

    private static void compile(Path sourceRoot, Path classes) throws Exception {
        Files.createDirectories(classes);
        List<Path> files = Files.walk(sourceRoot).filter(path -> path.toString().endsWith(".java")).toList();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Path lombok = jar("org/projectlombok/lombok/1.18.46/lombok-1.18.46.jar");
        List<String> arguments = new ArrayList<>();
        arguments.add("-classpath");
        arguments.add(System.getProperty("java.class.path") + java.io.File.pathSeparator + lombok);
        arguments.add("-processorpath");
        arguments.add(lombok.toString());
        arguments.add("-d");
        arguments.add(classes.toString());
        files.forEach(file -> arguments.add(file.toString()));
        ByteArrayOutputStream log = new ByteArrayOutputStream();
        assertEquals(0, compiler.run(null, log, log, arguments.toArray(String[]::new)), log.toString(StandardCharsets.UTF_8));
    }

    private static String config(String profile, Path output) {
        String modules = profile.equals("light") ? "" : """
                  "modulePaths": {
                    "domain": "egon-cola-source-%s-domain",
                    "application": "egon-cola-source-%s-application",
                    "infrastructure": "egon-cola-source-%s-infrastructure",
                    "adapter": "egon-cola-source-%s-adapter"
                  },
                """.formatted(profile, profile, profile, profile);
        return """
                {
                  "configVersion": 1,
                  "projectType": "%s",
                  "basePackage": "com.example.codegenfixture",
                  "domain": "codegenfixture",
                  "outputRoot": "%s",
                %s
                  "input": {"mode": "schema", "schemaFiles": ["%s"]},
                  "logicalTables": ["orders"],
                  "artifacts": ["po", "dao", "mapper-xml", "repo"],
                  "existingTypeMappings": {},
                  "fieldPolicies": {"create": ["code"], "update": ["code"], "result": ["code"], "filter": ["code"], "sort": ["id"]},
                  "apiContract": {
                    "existingErrorMapper": "com.example.codegenfixture.support.FixtureErrorMapper",
                    "contextSymbol": "com.example.codegenfixture.support.FixtureContext",
                    "basePath": "/api/codegenfixture"
                  },
                  "events": {"enabled": false}
                }
                """.formatted(profile, output, modules,
                Path.of("src/test/resources/ddl/schema.sql").toAbsolutePath());
    }

    private static Path jar(String relative) {
        List<Path> candidates = List.of(
                Path.of(System.getProperty("user.home"), "maven/repository").resolve(relative),
                Path.of(System.getProperty("user.home"), ".m2/repository").resolve(relative));
        return candidates.stream().filter(Files::exists).findFirst().orElseThrow();
    }
}
