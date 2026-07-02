#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.starter;

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
        noClasses().that().resideInAPackage("${package}.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "${package}.application..",
                        "${package}.adapter..",
                        "${package}.facade..",
                        "${package}.infrastructure..",
                        "${package}.starter..")
                .check(classes);
    }

    @Test
    void facade_does_not_depend_on_internal_layers() {
        noClasses().that().resideInAPackage("${package}.facade..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "${package}.common..",
                        "${package}.domain..",
                        "${package}.application..",
                        "${package}.adapter..",
                        "${package}.infrastructure..",
                        "${package}.starter..")
                .check(classes);
    }

    @Test
    void adapter_does_not_depend_on_infrastructure() {
        noClasses().that().resideInAPackage("${package}.adapter..")
                .should().dependOnClassesThat().resideInAPackage("${package}.infrastructure..")
                .check(classes);
    }

    private static List<Path> targetClassPaths() {
        Path root = Path.of("..").toAbsolutePath().normalize();
        return List.of(
                root.resolve("${rootArtifactId}-common/target/classes"),
                root.resolve("${rootArtifactId}-facade/target/classes"),
                root.resolve("${rootArtifactId}-domain/target/classes"),
                root.resolve("${rootArtifactId}-application/target/classes"),
                root.resolve("${rootArtifactId}-adapter/target/classes"),
                root.resolve("${rootArtifactId}-infrastructure/target/classes"),
                root.resolve("${rootArtifactId}-starter/target/classes"));
    }
}
