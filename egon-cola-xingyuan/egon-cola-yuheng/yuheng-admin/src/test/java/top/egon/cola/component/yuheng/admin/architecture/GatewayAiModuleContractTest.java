package top.egon.cola.component.yuheng.admin.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fixes the approved Yuheng AI delivery boundary: the standalone LLM gateway
 * module and the MyBatis-Plus coordinates every executable consumer needs must
 * exist, while the pure contract/core/runtime modules stay free of persistence
 * inheritance so the AI capability cannot leak into the shared data planes.
 */
class GatewayAiModuleContractTest {

    private static final String MYBATIS_PLUS_COORDINATE =
            "egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter";

    private static final List<String> MP_INHERITANCE_MARKERS = List.of(
            "com.baomidou",
            "top.egon.cola.component.common.mybatis",
            "EgonModel",
            "@TableName"
    );

    private static final List<String> PURE_MODULE_DIRECTORIES = List.of(
            "yuheng-contract",
            "yuheng-core",
            "yuheng-runtime-core"
    );

    @Test
    void approvedLlmGatewayModuleAndManagedCoordinatesArePresent()
            throws IOException {
        Path reactor = llmGatewayReactor();
        List<String> missing = new ArrayList<>();

        require(missing, reactor.resolve("yuheng-llm-gateway/pom.xml"));
        require(missing, reactor.resolve(
                "yuheng-llm-gateway/src/main/java/top/egon/cola/component/"
                        + "yuheng/llm/bootstrap/LlmGatewayApplication.java"));

        String aggregator = read(reactor.resolve("pom.xml"));
        assertTrue(aggregator.contains("<module>yuheng-llm-gateway</module>"),
                "Aggregator POM does not register the yuheng-llm-gateway module");
        assertTrue(
                aggregator.contains(
                        "<artifactId>yuheng-llm-gateway</artifactId>"),
                "Aggregator POM does not manage the yuheng-llm-gateway version");

        for (String consumer : List.of(
                "yuheng-admin/pom.xml",
                "yuheng-mcp-gateway/pom.xml",
                "yuheng-llm-gateway/pom.xml")) {
            Path consumerPom = reactor.resolve(consumer);
            require(missing, consumerPom);
            if (Files.exists(consumerPom)
                    && !read(consumerPom).contains(MYBATIS_PLUS_COORDINATE)) {
                missing.add(consumer + " (missing managed MP starter)");
            }
        }

        assertTrue(missing.isEmpty(),
                "Approved AI module boundary is incomplete: " + missing);
    }

    @Test
    void pureModulesRemainFreeOfMybatisPlusInheritance() throws IOException {
        Path reactor = llmGatewayReactor();
        List<String> offenders = new ArrayList<>();
        for (String module : PURE_MODULE_DIRECTORIES) {
            Path sourceRoot = reactor.resolve(module + "/src/main/java");
            assertTrue(Files.isDirectory(sourceRoot),
                    "Pure module source root disappeared: " + module);
            try (Stream<Path> paths = Files.walk(sourceRoot)) {
                paths.filter(path -> path.toString().endsWith(".java"))
                        .forEach(path -> {
                            String source = read(path);
                            if (MP_INHERITANCE_MARKERS.stream()
                                    .anyMatch(source::contains)) {
                                offenders.add(path.toString());
                            }
                        });
            }
            String pom = read(reactor.resolve(module + "/pom.xml"));
            if (pom.contains(MYBATIS_PLUS_COORDINATE)) {
                offenders.add(module + "/pom.xml");
            }
        }
        assertTrue(offenders.isEmpty(),
                "MyBatis-Plus leaked into a pure module: " + offenders);
    }

    /** Surefire runs from the {@code yuheng-admin} module directory. */
    private static Path llmGatewayReactor() {
        return Path.of("..").toAbsolutePath().normalize();
    }

    private static void require(List<String> missing, Path path) {
        if (!Files.exists(path)) {
            missing.add(path.toString());
        }
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "Cannot inspect Yuheng AI boundary file " + path,
                    failure
            );
        }
    }
}
