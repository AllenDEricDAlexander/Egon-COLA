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
            .importPackages("top.egon.cola.archetype.source.webopen");

    @Test
    void keeps_facade_and_domain_independent_from_runtime_layers() {
        noClasses()
                .that().resideInAnyPackage("top.egon.cola.archetype.source.webopen.facade..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "top.egon.cola.archetype.source.webopen.domain..",
                        "top.egon.cola.archetype.source.webopen.application..",
                        "top.egon.cola.archetype.source.webopen.infrastructure..",
                        "top.egon.cola.archetype.source.webopen.adapter..",
                        "top.egon.cola.archetype.source.webopen.starter..")
                .check(PRODUCTION_CLASSES);

        noClasses()
                .that().resideInAnyPackage("top.egon.cola.archetype.source.webopen.domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "top.egon.cola.archetype.source.webopen.application..",
                        "top.egon.cola.archetype.source.webopen.infrastructure..",
                        "top.egon.cola.archetype.source.webopen.adapter..",
                        "top.egon.cola.archetype.source.webopen.starter..",
                        "top.egon.cola.archetype.source.webopen.facade..")
                .check(PRODUCTION_CLASSES);

        noClasses()
                .that().resideInAnyPackage("top.egon.cola.archetype.source.webopen.application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "top.egon.cola.archetype.source.webopen.infrastructure..",
                        "top.egon.cola.archetype.source.webopen.adapter..",
                        "top.egon.cola.archetype.source.webopen.starter..",
                        "top.egon.cola.archetype.source.webopen.facade..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void keeps_infrastructure_and_adapter_edges_directional() {
        noClasses()
                .that().resideInAnyPackage("top.egon.cola.archetype.source.webopen.infrastructure..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("top.egon.cola.archetype.source.webopen.adapter..", "top.egon.cola.archetype.source.webopen.starter..")
                .check(PRODUCTION_CLASSES);

        noClasses()
                .that().resideInAnyPackage("top.egon.cola.archetype.source.webopen.adapter..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("top.egon.cola.archetype.source.webopen.infrastructure..", "top.egon.cola.archetype.source.webopen.starter..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void keeps_transport_persistence_and_gateway_frameworks_at_the_edge() {
        noClasses()
                .that().resideInAnyPackage("top.egon.cola.archetype.source.webopen.domain..", "top.egon.cola.archetype.source.webopen.application..")
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
                .should().resideInAnyPackage("top.egon.cola.archetype.source.webopen.adapter..")
                .check(PRODUCTION_CLASSES);

        classes()
                .that().areAnnotatedWith("org.springframework.stereotype.Controller")
                .should().resideInAnyPackage("top.egon.cola.archetype.source.webopen.adapter..")
                .check(PRODUCTION_CLASSES);
    }
}
