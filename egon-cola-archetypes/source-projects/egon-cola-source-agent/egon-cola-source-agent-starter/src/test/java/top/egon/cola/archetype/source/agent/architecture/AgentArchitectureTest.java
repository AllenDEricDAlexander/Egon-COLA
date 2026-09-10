package top.egon.cola.archetype.source.agent.architecture;

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

/** Verifies the Agent source reactor keeps the exact Web six-module boundary. */
class AgentArchitectureTest {

    private static final Path SOURCE_ROOT = Path.of("..").toAbsolutePath().normalize();
    private static final String ROOT_ARTIFACT_ID = artifactId(SOURCE_ROOT.resolve("pom.xml"));
    private static final List<String> MODULES = List.of(
            ROOT_ARTIFACT_ID + "-common",
            ROOT_ARTIFACT_ID + "-domain",
            ROOT_ARTIFACT_ID + "-application",
            ROOT_ARTIFACT_ID + "-infrastructure",
            ROOT_ARTIFACT_ID + "-adapter",
            ROOT_ARTIFACT_ID + "-starter");

    @Test
    void uses_exact_web_non_open_modules() throws IOException {
        String rootPom = read(SOURCE_ROOT.resolve("pom.xml"));
        assertEquals(6, MODULES.stream().filter(module -> rootPom.contains("<module>" + module + "</module>"))
                .count());
        assertTrue(rootPom.indexOf("<module>" + MODULES.getFirst() + "</module>")
                < rootPom.indexOf("<module>" + MODULES.getLast() + "</module>"));
        assertFalse(rootPom.contains("facade"));
    }

    @Test
    void preserves_domain_first_dependencies() throws IOException {
        assertDirectInternalDependency(modulePom("domain"), moduleName("common"));
        assertDirectInternalDependency(modulePom("application"), moduleName("domain"));
        assertDirectInternalDependency(modulePom("infrastructure"), moduleName("domain"));
        assertDirectInternalDependency(modulePom("adapter"), moduleName("application"));
        assertDirectInternalDependency(modulePom("starter"), moduleName("adapter"));
        assertDirectInternalDependency(modulePom("starter"), moduleName("infrastructure"));
        assertFalse(read(modulePom("adapter")).contains(moduleName("infrastructure")));
    }

    @Test
    void keeps_agent_technology_in_infrastructure_and_starter() throws IOException {
        String domain = readJavaSources(SOURCE_ROOT.resolve(moduleName("domain") + "/src/main/java"));
        String application = readJavaSources(SOURCE_ROOT.resolve(moduleName("application") + "/src/main/java"));
        for (String forbidden : List.of("com.google.adk", "io.reactivex", "org.springframework.ai",
                "org.springframework.web", "org.springframework.http", "io.modelcontextprotocol")) {
            assertFalse(domain.contains(forbidden), forbidden);
            assertFalse(application.contains(forbidden), forbidden);
        }
    }

    @Test
    void has_no_forbidden_integrations_or_source_web_business() throws IOException {
        try (Stream<Path> files = Files.walk(SOURCE_ROOT)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> !path.toString().contains("/src/test/"))
                    .filter(path -> !path.toString().contains("/target/"))
                    .filter(path -> path.toString().endsWith(".xml") || path.toString().endsWith(".java"))
                    .forEach(path -> {
                        String content = read(path);
                        // flyway 与 mybatis 是 knowledge 域的持久化依赖，已由 Spec 2026-09-10-11-51 的 Amends 放开；
                        // 其余禁项保持不变。
                        for (String forbidden : List.of("spring-boot-starter-jdbc", "spring-boot-starter-data-redis",
                                "spring-boot-starter-amqp", "spring-boot-starter-graphql", "dubbo-spring-boot-starter",
                                "shardingsphere", "egon-cola-source-web")) {
                            assertFalse(content.toLowerCase().contains(forbidden), path + " contains " + forbidden);
                        }
                    });
        }
    }

    @Test
    void keeps_knowledge_dependency_direction() throws IOException {
        assertFalse(read(modulePom("adapter")).contains(moduleName("infrastructure")),
                "adapter must not depend on infrastructure");
        assertFalse(read(modulePom("application")).contains(moduleName("infrastructure")),
                "application must not depend on infrastructure");
    }

    private static void assertDirectInternalDependency(Path pom, String artifactId) throws IOException {
        assertTrue(read(pom).contains("<artifactId>" + artifactId + "</artifactId>"),
                pom + " missing " + artifactId);
    }

    private static String moduleName(String suffix) {
        return ROOT_ARTIFACT_ID + "-" + suffix;
    }

    private static Path modulePom(String suffix) {
        return SOURCE_ROOT.resolve(moduleName(suffix) + "/pom.xml");
    }

    private static String artifactId(Path pom) {
        try {
            String content = Files.readString(pom, StandardCharsets.UTF_8);
            String start = "<artifactId>";
            int begin = content.indexOf(start, content.indexOf("</parent>")) + start.length();
            return content.substring(begin, content.indexOf("</artifactId>", begin));
        } catch (IOException failure) {
            throw new IllegalStateException("cannot read root artifactId from " + pom, failure);
        }
    }

    private static String readJavaSources(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            return "";
        }
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(path -> path.toString().endsWith(".java"))
                    .map(AgentArchitectureTest::read)
                    .reduce("", String::concat);
        }
    }

    private static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException("cannot read " + path, failure);
        }
    }
}
