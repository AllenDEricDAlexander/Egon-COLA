package top.egon.cola.component.bytecode.maven.scan;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureLayer;
import top.egon.cola.component.bytecode.core.architecture.DefaultLayerResolver;
import top.egon.cola.component.bytecode.core.architecture.LayerMapping;

import javax.tools.ToolProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureScannerTest {

    @TempDir
    Path directory;

    @Test
    void scansClassDirectoriesWithoutLoadingApplicationClassesAndUsesContentCache() throws Exception {
        Path source = directory.resolve("src/sample/domain/Order.java");
        Path classes = directory.resolve("classes");
        Files.createDirectories(source.getParent());
        Files.createDirectories(classes);
        Files.writeString(source, """
                package sample.domain;
                public final class Order { java.util.List<String> names; }
                """);
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(
                null, null, null, "-d", classes.toString(), source.toString()));
        ArchitectureScanner scanner = new ArchitectureScanner(new ObjectMapper(), ignored -> { });
        DefaultLayerResolver resolver = new DefaultLayerResolver(new LayerMapping(
                Map.of(), Map.of(ArchitectureLayer.DOMAIN, Set.of("sample.domain.."))));
        ScanRequest request = new ScanRequest(
                List.of(new ScanInput("sample-domain", classes)), resolver,
                true, directory.resolve("cache"), "config");

        ArchitectureScanResult first = scanner.scan(request);
        ArchitectureScanResult second = scanner.scan(request);

        assertEquals(1, first.graph().types().size());
        assertEquals(ArchitectureLayer.DOMAIN, first.graph().types().getFirst().layer());
        assertEquals(0, first.cacheHits());
        assertEquals(1, second.cacheHits());
        assertTrue(first.graph().dependencies().stream()
                .anyMatch(dependency -> dependency.targetClass().equals("java.util.List")));
    }
}
