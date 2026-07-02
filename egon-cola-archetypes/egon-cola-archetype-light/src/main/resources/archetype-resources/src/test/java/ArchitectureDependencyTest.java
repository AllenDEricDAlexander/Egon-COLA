#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureDependencyTest {
    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(location -> ImportOption.Predefined.DO_NOT_INCLUDE_TESTS.includes(location)
                    || location.asURI().getPath().contains("/target/classes/"))
            .importPackages("${package}");

    @Test
    void domain_does_not_depend_on_outer_layers() {
        noClasses().that().resideInAPackage("${package}.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "${package}.adapter..",
                        "${package}.application..",
                        "${package}.facade..",
                        "${package}.infrastructure..",
                        "${package}.start..")
                .check(classes);
    }

    @Test
    void facade_does_not_depend_on_internal_layers() {
        noClasses().that().resideInAPackage("${package}.facade..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "${package}.adapter..",
                        "${package}.application..",
                        "${package}.common..",
                        "${package}.domain..",
                        "${package}.infrastructure..",
                        "${package}.start..")
                .check(classes);
    }

    @Test
    void application_does_not_depend_on_adapter_or_infrastructure() {
        noClasses().that().resideInAPackage("${package}.application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "${package}.adapter..",
                        "${package}.facade..",
                        "${package}.infrastructure..",
                        "${package}.start..")
                .check(classes);
    }

    @Test
    void adapter_only_depends_on_application_facade_and_common() {
        noClasses().that().resideInAPackage("${package}.adapter..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "${package}.domain..",
                        "${package}.infrastructure..",
                        "${package}.start..")
                .check(classes);
    }

    @Test
    void infrastructure_does_not_depend_on_inbound_or_application_layers() {
        noClasses().that().resideInAPackage("${package}.infrastructure..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "${package}.adapter..",
                        "${package}.application..",
                        "${package}.facade..",
                        "${package}.start..")
                .check(classes);
    }

    @Test
    void common_does_not_depend_on_other_project_layers() {
        noClasses().that().resideInAPackage("${package}.common..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "${package}.adapter..",
                        "${package}.application..",
                        "${package}.domain..",
                        "${package}.facade..",
                        "${package}.infrastructure..",
                        "${package}.start..")
                .check(classes);
    }
}
