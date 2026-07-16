package top.egon.cola.component.bytecode.agent;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AgentJarManifestTest {

    @Test
    void declaresPremainOnlyManifest() throws Exception {
        Manifest manifest = Collections.list(getClass().getClassLoader()
                        .getResources("META-INF/MANIFEST.MF"))
                .stream()
                .map(resource -> {
                    try (var stream = resource.openStream()) {
                        return new Manifest(stream);
                    } catch (Exception exception) {
                        throw new IllegalStateException(exception);
                    }
                })
                .filter(candidate -> BytecodeAgent.class.getName().equals(
                        candidate.getMainAttributes().getValue("Premain-Class")))
                .findFirst()
                .orElse(null);
        assertNotNull(manifest);
        assertEquals("false", manifest.getMainAttributes().getValue("Can-Redefine-Classes"));
        assertEquals("false", manifest.getMainAttributes().getValue("Can-Retransform-Classes"));
        assertFalse(manifest.getMainAttributes().containsKey("Agent-Class"));
        assertEquals(1, Arrays.stream(BytecodeAgent.class.getDeclaredMethods())
                .map(Method::getName)
                .filter("premain"::equals)
                .count());
        assertFalse(Arrays.stream(BytecodeAgent.class.getDeclaredMethods())
                .map(Method::getName)
                .anyMatch("agentmain"::equals));
    }
}
