package architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
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

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Verifies the generated Service Open dependency direction without the internal bytecode plugin.
 */
class OpenArchitectureTest {

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .importPackages("top.egon.cola.archetype.source.serviceopen");

    @Test
    void keeps_facade_and_domain_independent_from_business_and_runtime_layers() {
        noClasses()
                .that().resideInAnyPackage("top.egon.cola.archetype.source.serviceopen.facade..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "top.egon.cola.archetype.source.serviceopen.domain..",
                        "top.egon.cola.archetype.source.serviceopen.application..",
                        "top.egon.cola.archetype.source.serviceopen.infrastructure..",
                        "top.egon.cola.archetype.source.serviceopen.adapter..",
                        "top.egon.cola.archetype.source.serviceopen.starter..")
                .check(PRODUCTION_CLASSES);

        noClasses()
                .that().resideInAnyPackage("top.egon.cola.archetype.source.serviceopen.domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "top.egon.cola.archetype.source.serviceopen.application..",
                        "top.egon.cola.archetype.source.serviceopen.adapter..",
                        "top.egon.cola.archetype.source.serviceopen.infrastructure..",
                        "top.egon.cola.archetype.source.serviceopen.starter..",
                        "top.egon.cola.archetype.source.serviceopen.facade..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void keeps_transport_and_persistence_frameworks_at_the_edge() {
        noClasses()
                .that().resideInAnyPackage("top.egon.cola.archetype.source.serviceopen.domain..", "top.egon.cola.archetype.source.serviceopen.application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "com.baomidou.mybatisplus..",
                        "org.apache.shardingsphere..",
                        "org.apache.dubbo..",
                        "io.grpc..")
                .check(PRODUCTION_CLASSES);

        noClasses()
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springdoc..",
                        "org.springframework.cloud.gateway..",
                        "org.flywaydb..",
                        "jakarta.persistence..",
                        "org.springframework.data.jpa..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void keeps_service_adapter_http_free() {
        noClasses()
                .that().resideInAnyPackage("top.egon.cola.archetype.source.serviceopen.adapter..")
                .should().beAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void uses_common_persistence_contract_in_infrastructure() throws IOException {
        Path infrastructureRoot;
        try (Stream<Path> siblings = Files.list(Path.of(".."))) {
            infrastructureRoot = siblings
                    .filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().endsWith("-infrastructure"))
                    .map(path -> path.resolve("src/main/java"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "generated infrastructure source root is missing"));
        }
        List<Path> javaFiles;
        try (Stream<Path> paths = Files.walk(infrastructureRoot)) {
            javaFiles = paths.filter(path -> path.toString().endsWith(".java")).toList();
        }
        String source = javaFiles.stream()
                .map(OpenArchitectureTest::read)
                .reduce("", String::concat);
        assertTrue(source.contains("extends EgonModel<"));
        assertTrue(source.contains("extends EgonColaMapper<"));
        assertTrue(source.contains("extends EgonColaServiceImpl<"));
        assertFalse(source.contains("extends BaseMapper<"));
        assertFalse(source.contains("repo.mapper"));
        assertEquals(5, javaFiles.stream()
                .filter(path -> path.getFileName().toString().endsWith("PO.java"))
                .count());
        assertEquals(5, javaFiles.stream()
                .filter(path -> path.getFileName().toString().endsWith("DAO.java"))
                .count());
        assertEquals(3, javaFiles.stream()
                .filter(path -> path.getFileName().toString().endsWith("DomainServiceImpl.java"))
                .count());
    }

    private static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException("cannot read generated source " + path, failure);
        }
    }
}
