package architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Verifies the generated Light Open package direction without relying on a custom Maven bytecode plugin.
 */
class OpenArchitectureTest {

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .importPath(Path.of("target/classes"));

    @Test
    void keeps_inward_layer_dependencies() {
        noClasses()
                .that().resideInAnyPackage("${package}.domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "${package}.application..",
                        "${package}.adapter..",
                        "${package}.infrastructure..",
                        "${package}.start..")
                .check(PRODUCTION_CLASSES);

        noClasses()
                .that().resideInAnyPackage("${package}.application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "${package}.adapter..",
                        "${package}.infrastructure..",
                        "${package}.start..")
                .check(PRODUCTION_CLASSES);

        noClasses()
                .that().resideInAnyPackage("${package}.infrastructure..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "${package}.application..",
                        "${package}.adapter..",
                        "${package}.start..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void keeps_transport_and_persistence_frameworks_at_the_edge() {
        noClasses()
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.apache." + "dubbo..",
                        "org.springframework.cloud." + "gateway..",
                        "org." + "flywaydb..",
                        "jakarta." + "persistence..",
                        "org.springframework.data." + "jpa..")
                .check(PRODUCTION_CLASSES);

        noClasses()
                .that().resideInAnyPackage("${package}.domain..", "${package}.application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "com.baomidou.mybatisplus..",
                        "org.apache.shardingsphere..")
                .check(PRODUCTION_CLASSES);
    }
}
