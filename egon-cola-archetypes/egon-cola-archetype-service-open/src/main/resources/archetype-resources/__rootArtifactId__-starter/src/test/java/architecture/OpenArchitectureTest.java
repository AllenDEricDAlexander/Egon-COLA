#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
package architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Verifies the generated Service Open dependency direction without the internal bytecode plugin.
 */
class OpenArchitectureTest {

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .importPackages("${package}");

    @Test
    void keeps_facade_and_domain_independent_from_business_and_runtime_layers() {
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
                        "${package}.adapter..",
                        "${package}.infrastructure..",
                        "${package}.starter..",
                        "${package}.facade..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void keeps_transport_and_persistence_frameworks_at_the_edge() {
        noClasses()
                .that().resideInAnyPackage("${package}.domain..", "${package}.application..")
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
                .that().resideInAnyPackage("${package}.adapter..")
                .should().beAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                .check(PRODUCTION_CLASSES);
    }
}
