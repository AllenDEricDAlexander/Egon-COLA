package top.egon.cola.archetype.source.agent.starter;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentSourceContractTest {

    private static final Path SOURCE = Path.of(".").toAbsolutePath().normalize().getParent();
    private static final List<String> MODULES = List.of(
            "egon-cola-source-agent-common", "egon-cola-source-agent-domain",
            "egon-cola-source-agent-application", "egon-cola-source-agent-infrastructure",
            "egon-cola-source-agent-adapter", "egon-cola-source-agent-starter");
    private static final Set<String> PROFILE_KEYS = Set.of(
            "max-concurrent-runs", "max-duration", "max-sources", "heartbeat-interval",
            "base-url", "api-key", "model-name", "enabled", "base-uri",
            "sse-endpoint", "request-timeout", "chat.options.model");

    @Test
    void keeps_exact_web_module_scope_and_only_research_business_roots() throws IOException {
        assertEquals(MODULES, moduleNames());
        assertTrue(Files.exists(SOURCE.resolve("README.md")));
        assertTrue(Files.exists(SOURCE.resolve("README.zh-CN.md")));
        assertTrue(Files.exists(SOURCE.resolve(
                "egon-cola-source-agent-starter/src/main/resources/agent/deep-research-flow.yml")));
        assertFalse(allSourceText().contains("egon-cola-source-web"));
        assertFalse(allSourceText().matches("(?s).*\\b(fastjson|flyway|mybatis|redis|graphql|dubbo)\\b.*"));
        assertFalse(allSourceText().contains("BEGIN GENERATED"));
    }

    @Test
    void documents_every_populated_java_package_and_keeps_profile_key_sets_equal() throws IOException {
        try (Stream<Path> files = Files.walk(SOURCE)) {
            files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.toString().contains("/src/main/java/")
                            || path.toString().contains("/src/test/java/"))
                    .map(Path::getParent)
                    .distinct()
                    .forEach(directory -> assertTrue(Files.exists(directory.resolve("package-info.java")),
                            () -> "missing package-info.java: " + directory));
        }
        Set<String> baseline = yamlKeys(SOURCE.resolve(
                "egon-cola-source-agent-starter/src/main/resources/application.yml"));
        for (String profile : List.of("dev", "test", "prod")) {
            assertEquals(baseline, yamlKeys(SOURCE.resolve(
                    "egon-cola-source-agent-starter/src/main/resources/application-" + profile + ".yml")));
        }
    }

    private static List<String> moduleNames() throws IOException {
        List<String> actual = new ArrayList<>();
        String pom = Files.readString(SOURCE.resolve("pom.xml"));
        for (String module : MODULES) {
            if (pom.contains("<module>" + module + "</module>")) {
                actual.add(module);
            }
        }
        return actual;
    }

    private static String allSourceText() throws IOException {
        StringBuilder content = new StringBuilder();
        try (Stream<Path> files = Files.walk(SOURCE)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                if ((file.toString().contains("/src/main/") || file.getFileName().toString().equals("pom.xml"))
                        && (file.toString().endsWith(".java") || file.toString().endsWith(".xml")
                        || file.toString().endsWith(".yml"))) {
                    content.append(Files.readString(file));
                }
            }
        }
        return content.toString().toLowerCase();
    }

    private static Set<String> yamlKeys(Path file) throws IOException {
        return Files.readAllLines(file).stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#") && line.contains(":"))
                .map(line -> line.substring(0, line.indexOf(':')).trim())
                .filter(key -> !key.startsWith("-") && !key.startsWith("${"))
                .filter(PROFILE_KEYS::contains)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
