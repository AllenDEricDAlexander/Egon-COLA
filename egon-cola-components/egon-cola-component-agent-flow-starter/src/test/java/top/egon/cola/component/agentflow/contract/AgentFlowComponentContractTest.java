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
    private static final Path BOM_POM = COMPONENTS_ROOT.resolve("egon-cola-components-bom/pom.xml");
    private static final Path BOM_README = COMPONENTS_ROOT.resolve("egon-cola-components-bom/README.md");
    private static final Path BOM_README_ZH = COMPONENTS_ROOT.resolve("egon-cola-components-bom/README.zh-CN.md");
    private static final Path COMPONENTS_README = COMPONENTS_ROOT.resolve("README.md");
    private static final Path COMPONENTS_README_ZH = COMPONENTS_ROOT.resolve("README.zh-CN.md");
    private static final Path ARCHITECTURE = COMPONENTS_ROOT.resolve("egon-cola-components-architecture.md");
    private static final Path MODULE_POM = Path.of("pom.xml").toAbsolutePath().normalize();
    private static final Path SOURCE_ROOT = Path.of("src/main/java").toAbsolutePath().normalize();
    private static final Path MODULE_README = Path.of("README.md").toAbsolutePath().normalize();
    private static final Path MODULE_README_ZH = Path.of("README.zh-CN.md").toAbsolutePath().normalize();

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
    void is_exported_once_by_components_bom() throws IOException {
        String bom = read(BOM_POM);

        assertEquals(1, count(bom, "<artifactId>egon-cola-component-agent-flow-starter</artifactId>"));
        assertTrue(bom.contains("<version>${project.version}</version>"));
        assertFalse(bom.contains("<artifactId>spring-ai-model</artifactId>"));
        assertFalse(bom.contains("<artifactId>google-adk</artifactId>"));
        assertFalse(bom.contains("<artifactId>google-adk-spring-ai</artifactId>"));
    }

    @Test
    void documents_exact_configuration_api_and_boundaries() throws IOException {
        String english = read(MODULE_README);
        String chinese = read(MODULE_README_ZH);
        String architecture = read(ARCHITECTURE);

        String[] contract = {
                "egon.cola.component.agent-flow",
                "execution-timeout",
                "shutdown-timeout",
                "flows",
                "listFlows",
                "createSession",
                "deleteSession",
                "execute",
                "executeStream",
                "Spring AI 1.1.8",
                "Google ADK 0.7.0",
                "Flowable<Event>",
                "AgentFlowSessionBusyException",
                "AgentFlowExecutionTimeoutException",
                "in-memory",
                "cancel",
                "close",
                "MCP",
                "HTTP",
                "database"
        };
        assertContainsAll(english, contract);
        assertContainsAll(chinese, contract);
        assertContainsAll(architecture,
                "egon-cola-component-agent-flow-starter",
                "Agent Flow",
                "HTTP",
                "MCP",
                "database",
                "ChatModel");
    }

    @Test
    void keeps_chinese_english_contracts_in_sync() throws IOException {
        String english = read(MODULE_README);
        String chinese = read(MODULE_README_ZH);
        String componentsEnglish = read(COMPONENTS_README);
        String componentsChinese = read(COMPONENTS_README_ZH);
        String bomEnglish = read(BOM_README);
        String bomChinese = read(BOM_README_ZH);

        String[] moduleTokens = {
                "Agent Flow",
                "agent-flow-starter",
                "enabled",
                "execution-timeout",
                "shutdown-timeout",
                "executeStream",
                "Spring AI 1.1.8",
                "Google ADK 0.7.0",
                "no retry",
                "in-memory"
        };
        assertContainsAll(english, moduleTokens);
        assertContainsAll(chinese, moduleTokens);
        assertContainsAll(componentsEnglish, "egon-cola-component-agent-flow-starter", "Agent Flow");
        assertContainsAll(componentsChinese, "egon-cola-component-agent-flow-starter", "Agent Flow");
        assertContainsAll(bomEnglish, "egon-cola-component-agent-flow-starter", "Agent Flow");
        assertContainsAll(bomChinese, "egon-cola-component-agent-flow-starter", "Agent Flow");
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

    private static void assertContainsAll(String source, String... tokens) {
        for (String token : tokens) {
            assertTrue(source.contains(token), "missing contract token: " + token);
        }
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
