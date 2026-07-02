package top.egon.fable-web.starter;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureDependencyTest {
    private final JavaClasses classes = new ClassFileImporter().importPaths(targetClassPaths());

    @Test
    void domain_does_not_depend_on_outer_layers() {
        noClasses().that().resideInAPackage("top.egon.fable-web.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "top.egon.fable-web.application..",
                        "top.egon.fable-web.adapter..",
                        "top.egon.fable-web.facade..",
                        "top.egon.fable-web.infrastructure..",
                        "top.egon.fable-web.starter..")
                .check(classes);
    }

    @Test
    void facade_does_not_depend_on_internal_layers() {
        noClasses().that().resideInAPackage("top.egon.fable-web.facade..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "top.egon.fable-web.common..",
                        "top.egon.fable-web.domain..",
                        "top.egon.fable-web.application..",
                        "top.egon.fable-web.adapter..",
                        "top.egon.fable-web.infrastructure..",
                        "top.egon.fable-web.starter..")
                .check(classes);
    }

    @Test
    void adapter_does_not_depend_on_infrastructure() {
        noClasses().that().resideInAPackage("top.egon.fable-web.adapter..")
                .should().dependOnClassesThat().resideInAPackage("top.egon.fable-web.infrastructure..")
                .check(classes);
    }

    private static List<Path> targetClassPaths() {
        Path root = Path.of("..").toAbsolutePath().normalize();
        return List.of(
                root.resolve("fable-web-common/target/classes"),
                root.resolve("fable-web-facade/target/classes"),
                root.resolve("fable-web-domain/target/classes"),
                root.resolve("fable-web-application/target/classes"),
                root.resolve("fable-web-adapter/target/classes"),
                root.resolve("fable-web-infrastructure/target/classes"),
                root.resolve("fable-web-starter/target/classes"));
    }
}
