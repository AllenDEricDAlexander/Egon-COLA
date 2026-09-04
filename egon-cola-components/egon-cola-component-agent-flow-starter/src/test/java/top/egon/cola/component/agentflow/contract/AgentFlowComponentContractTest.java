package top.egon.cola.component.agentflow.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Protects the flat Agent Flow starter boundary before its runtime implementation is added.
 */
class AgentFlowComponentContractTest {

    private static final Path COMPONENTS_ROOT = Path.of("..").toAbsolutePath().normalize();
    private static final Path PARENT_POM = COMPONENTS_ROOT.resolve("pom.xml");
    private static final Path MODULE_POM = Path.of("pom.xml").toAbsolutePath().normalize();
    private static final Path SOURCE_ROOT = Path.of("src/main/java").toAbsolutePath().normalize();

    @Test
    void is_registered_by_parent() throws IOException {
        String parent = read(PARENT_POM);

        assertEquals(1, count(parent, "<module>egon-cola-component-agent-flow-starter</module>"));
        assertTrue(parent.contains("<spring-ai.version>1.1.8</spring-ai.version>"));
        assertTrue(parent.contains("<google-adk.version>0.7.0</google-adk.version>"));
        assertTrue(parent.contains("<artifactId>spring-ai-bom</artifactId>"));
        assertTrue(parent.contains("<artifactId>google-adk-spring-ai</artifactId>"));
    }

    @Test
    void uses_exact_dependency_boundary() throws IOException {
        String module = read(MODULE_POM);

        assertTrue(module.contains("<artifactId>egon-cola-component-common-core</artifactId>"));
        assertTrue(module.contains("<artifactId>spring-ai-model</artifactId>"));
        assertTrue(module.contains("<artifactId>google-adk</artifactId>"));
        assertTrue(module.contains("<artifactId>google-adk-spring-ai</artifactId>"));
        assertTrue(module.contains("<exclusions>"));
        assertTrue(module.contains("<artifactId>google-adk-dev</artifactId>"));
        assertFalse(module.contains("spring-boot-starter-web"));
        assertFalse(module.contains("spring-ai-starter-mcp"));
        assertFalse(module.contains("flyway"));
        assertFalse(module.contains("jdbc"));
    }

    @Test
    void does_not_expose_provider_or_secret_properties() throws IOException {
        String module = read(MODULE_POM);

        assertFalse(module.contains("api-key"));
        assertFalse(module.contains("base-url"));
        assertFalse(module.contains("spring-ai-openai"));
    }

    @Test
    void uses_semantic_java_names_and_safe_logging() throws IOException {
        if (!Files.isDirectory(SOURCE_ROOT)) {
            return;
        }

        try (Stream<Path> files = Files.walk(SOURCE_ROOT)) {
            List<Path> javaFiles = files.filter(path -> path.toString().endsWith(".java")).toList();
            for (Path javaFile : javaFiles) {
                String source = read(javaFile);
                assertFalse(source.contains("java.util.Date"), javaFile.toString());
                assertFalse(source.contains("java.util.Calendar"), javaFile.toString());
                assertFalse(source.contains("java.text.SimpleDateFormat"), javaFile.toString());
                assertFalse(source.contains("LoggerFactory.getLogger"), javaFile.toString());
                assertFalse(source.contains("BeanUtils.copyProperties"), javaFile.toString());
                assertFalse(source.contains("com.alibaba.fastjson"), javaFile.toString());
                assertFalse(source.contains("userId"), javaFile.toString());
                assertFalse(source.contains("sessionId"), javaFile.toString());
                assertFalse(source.contains("prompt"), javaFile.toString());
            }
        }
    }

    @Test
    void uses_flat_functional_packages() throws IOException {
        if (!Files.isDirectory(SOURCE_ROOT)) {
            return;
        }

        try (Stream<Path> directories = Files.walk(SOURCE_ROOT)) {
            directories.filter(Files::isDirectory).forEach(directory -> {
                String normalized = directory.toString().replace('\\', '/');
                assertFalse(normalized.contains("/domain/"), normalized);
                assertFalse(normalized.contains("/application/"), normalized);
                assertFalse(normalized.contains("/infrastructure/"), normalized);
                assertFalse(normalized.contains("/adapter/"), normalized);
                assertFalse(normalized.contains("/aggregate/"), normalized);
                assertFalse(normalized.contains("/repository/"), normalized);
                assertFalse(normalized.contains("/biz/"), normalized);
            });
        }
    }

    private static int count(String source, String token) {
        return source.split(java.util.regex.Pattern.quote(token), -1).length - 1;
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
