package top.egon.cola.component.bytecode.test.release;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class BytecodeReleaseShapeTest {

    private static final List<String> PUBLISHED_MODULES = List.of(
            "egon-cola-component-bytecode-api",
            "egon-cola-component-bytecode-bridge",
            "egon-cola-component-bytecode-core",
            "egon-cola-component-bytecode-runtime",
            "egon-cola-component-bytecode-agent",
            "egon-cola-component-bytecode-starter",
            "egon-cola-component-bytecode-architecture-maven-plugin"
    );

    private final Path agentJar = Path.of(System.getProperty("egon.bytecode.agent.jar"));
    private final Path componentDirectory = agentJar.getParent().getParent().getParent();
    private final String version = System.getProperty("egon.bytecode.project.version");

    @Test
    void publishesOnePremainOnlyShadedAgent() throws Exception {
        Path target = agentJar.getParent();
        assertTrue(Files.isRegularFile(agentJar), "main Agent JAR is missing");
        try (var files = Files.list(target)) {
            assertEquals(1, files.filter(path -> path.getFileName().toString()
                            .equals(agentJar.getFileName().toString()))
                    .count(), "exactly one main Agent artifact must be published");
        }

        try (JarFile jar = new JarFile(agentJar.toFile())) {
            Manifest manifest = jar.getManifest();
            assertNotNull(manifest);
            assertEquals("top.egon.cola.component.bytecode.agent.BytecodeAgent",
                    manifest.getMainAttributes().getValue("Premain-Class"));
            assertEquals(version,
                    manifest.getMainAttributes().getValue("Implementation-Version"));
            assertEquals("1.0",
                    manifest.getMainAttributes().getValue("Egon-Bridge-Protocol"));
            assertEquals("false",
                    manifest.getMainAttributes().getValue("Can-Redefine-Classes"));
            assertEquals("false",
                    manifest.getMainAttributes().getValue("Can-Retransform-Classes"));
            assertFalse(manifest.getMainAttributes().containsKey("Agent-Class"));
            assertNotNull(jar.getEntry(
                    "top/egon/cola/component/bytecode/agent/shaded/asm/ClassReader.class"));
            assertNotNull(jar.getEntry(
                    "top/egon/cola/component/bytecode/agent/shaded/snakeyaml/v2/api/Load.class"));
            assertTrue(jar.getEntry("org/objectweb/asm/ClassReader.class") == null);
            assertTrue(jar.getEntry("org/snakeyaml/engine/v2/api/Load.class") == null);
        }
    }

    @Test
    void bomExportsOnlyConsumerFacingBytecodeArtifacts() throws IOException {
        String bom = Files.readString(componentDirectory.resolve(
                "../egon-cola-components-bom/pom.xml").normalize());
        for (String artifact : List.of(
                "egon-cola-component-bytecode-api",
                "egon-cola-component-bytecode-bridge",
                "egon-cola-component-bytecode-runtime",
                "egon-cola-component-bytecode-agent",
                "egon-cola-component-bytecode-starter")) {
            assertTrue(bom.contains("<artifactId>" + artifact + "</artifactId>"), artifact);
        }
        for (String internalArtifact : List.of(
                "egon-cola-component-bytecode-core",
                "egon-cola-component-bytecode-architecture-maven-plugin",
                "egon-cola-component-bytecode-test",
                "egon-cola-component-bytecode-benchmark")) {
            assertFalse(bom.contains("<artifactId>" + internalArtifact + "</artifactId>"),
                    internalArtifact);
        }
    }

    @Test
    void releaseProfileAttachesSourcesAndJavadocsToPublishedModules() {
        assumeTrue(Boolean.getBoolean("egon.release.shape"),
                "run with -Prelease -Degon.release.shape=true");
        for (String module : PUBLISHED_MODULES) {
            Path target = componentDirectory.resolve(module).resolve("target");
            assertTrue(Files.isRegularFile(target.resolve(
                    module + "-" + version + "-sources.jar")), module + " sources");
            assertTrue(Files.isRegularFile(target.resolve(
                    module + "-" + version + "-javadoc.jar")), module + " javadocs");
        }
    }
}
