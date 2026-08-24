#set( $symbol_pound = '#' )
package architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Verifies the generated Web Open dependency direction without the internal bytecode plugin.
 */
class OpenArchitectureTest {

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .importPackages("${package}");

    @Test
    void keeps_facade_and_domain_independent_from_runtime_layers() {
        noClasses()
                .that().resideInAnyPackage("${package}.facade..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "${package}.domain..",
                        "${package}.application..",
                        "${package}.infrastructure..",
                        "${package}.adapter..",
                        "${package}.starter..")
                .check(PRODUCTION_CLASSES);

        noClasses()
                .that().resideInAnyPackage("${package}.domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "${package}.application..",
                        "${package}.infrastructure..",
                        "${package}.adapter..",
                        "${package}.starter..",
                        "${package}.facade..")
                .check(PRODUCTION_CLASSES);

        noClasses()
                .that().resideInAnyPackage("${package}.application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "${package}.infrastructure..",
                        "${package}.adapter..",
                        "${package}.starter..",
                        "${package}.facade..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void keeps_infrastructure_and_adapter_edges_directional() {
        noClasses()
                .that().resideInAnyPackage("${package}.infrastructure..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("${package}.adapter..", "${package}.starter..")
                .check(PRODUCTION_CLASSES);

        noClasses()
                .that().resideInAnyPackage("${package}.adapter..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("${package}.infrastructure..", "${package}.starter..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void keeps_transport_persistence_and_gateway_frameworks_at_the_edge() {
        noClasses()
                .that().resideInAnyPackage("${package}.domain..", "${package}.application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "com.baomidou.mybatisplus..",
                        "org.apache.shardingsphere..",
                        "org.apache.dubbo..",
                        "io.grpc..",
                        "org.springdoc..")
                .check(PRODUCTION_CLASSES);

        noClasses()
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework.cloud." + "gateway..",
                        "org." + "fly" + "waydb..",
                        "org.liqui" + "base..",
                        "jakarta." + "persistence..",
                        "org.springframework.data." + "jpa..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void keeps_http_and_graphql_adapters_at_the_edge() {
        classes()
                .that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                .should().resideInAnyPackage("${package}.adapter..")
                .check(PRODUCTION_CLASSES);

        classes()
                .that().areAnnotatedWith("org.springframework.stereotype.Controller")
                .should().resideInAnyPackage("${package}.adapter..")
                .check(PRODUCTION_CLASSES);
    }
}
