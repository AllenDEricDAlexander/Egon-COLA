package top.egon.light;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureDependencyTest {
    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(location -> ImportOption.Predefined.DO_NOT_INCLUDE_TESTS.includes(location)
                    || location.asURI().getPath().contains("/target/classes/"))
            .importPackages("top.egon.light");

    @Test
    void domain_does_not_depend_on_outer_layers() {
        noClasses().that().resideInAPackage("top.egon.light.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "top.egon.light.adapter..",
                        "top.egon.light.application..",
                        "top.egon.light.facade..",
                        "top.egon.light.infrastructure..",
                        "top.egon.light.start..")
                .check(classes);
    }

    @Test
    void facade_does_not_depend_on_internal_layers() {
        noClasses().that().resideInAPackage("top.egon.light.facade..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "top.egon.light.adapter..",
                        "top.egon.light.application..",
                        "top.egon.light.common..",
                        "top.egon.light.domain..",
                        "top.egon.light.infrastructure..",
                        "top.egon.light.start..")
                .check(classes);
    }

    @Test
    void application_does_not_depend_on_adapter_or_infrastructure() {
        noClasses().that().resideInAPackage("top.egon.light.application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "top.egon.light.adapter..",
                        "top.egon.light.facade..",
                        "top.egon.light.common.response..",
                        "top.egon.light.infrastructure..",
                        "top.egon.light.start..")
                .check(classes);
    }

    @Test
    void application_should_not_depend_on_external_models() {
        noClasses().that().resideInAPackage("top.egon.light.application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "top.egon.light.adapter..",
                        "top.egon.light.facade.dto..",
                        "top.egon.light.common.response..",
                        "org.springframework.web..")
                .check(classes);
    }

    @Test
    void adapter_only_depends_on_application_facade_and_common() {
        noClasses().that().resideInAPackage("top.egon.light.adapter..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "top.egon.light.infrastructure..",
                        "top.egon.light.start..")
                .check(classes);
    }

    @Test
    void infrastructure_does_not_depend_on_inbound_or_application_layers() {
        noClasses().that().resideInAPackage("top.egon.light.infrastructure..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "top.egon.light.adapter..",
                        "top.egon.light.application..",
                        "top.egon.light.facade..",
                        "top.egon.light.start..")
                .check(classes);
    }

    @Test
    void common_does_not_depend_on_other_project_layers() {
        noClasses().that().resideInAPackage("top.egon.light.common..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "top.egon.light.adapter..",
                        "top.egon.light.application..",
                        "top.egon.light.domain..",
                        "top.egon.light.facade..",
                        "top.egon.light.infrastructure..",
                        "top.egon.light.start..")
                .check(classes);
    }
}
