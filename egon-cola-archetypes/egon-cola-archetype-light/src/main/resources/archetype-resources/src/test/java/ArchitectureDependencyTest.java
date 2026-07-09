#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

class ArchitectureDependencyTest {
    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(location -> ImportOption.Predefined.DO_NOT_INCLUDE_TESTS.includes(location)
                    || location.asURI().getPath().contains("/target/classes/"))
            .importPackages("${package}");

    @Test
    void start_depends_only_on_adapter_and_infrastructure() {
        assertNoDependency("${package}.start..",
                "${package}.application..", "${package}.common..", "${package}.domain..", "${package}.facade..");
    }

    @Test
    void adapter_depends_only_on_application_and_facade() {
        assertNoDependency("${package}.adapter..",
                "${package}.common..", "${package}.domain..", "${package}.infrastructure..", "${package}.start..");
    }

    @Test
    void application_depends_only_on_domain() {
        assertNoDependency("${package}.application..",
                "${package}.adapter..", "${package}.common..", "${package}.facade..",
                "${package}.infrastructure..", "${package}.start..");
    }

    @Test
    void domain_depends_only_on_common() {
        assertNoDependency("${package}.domain..",
                "${package}.adapter..", "${package}.application..", "${package}.facade..",
                "${package}.infrastructure..", "${package}.start..");
    }

    @Test
    void infrastructure_depends_only_on_domain() {
        assertNoDependency("${package}.infrastructure..",
                "${package}.adapter..", "${package}.application..", "${package}.common..",
                "${package}.facade..", "${package}.start..");
    }

    @Test
    void facade_has_no_internal_layer_dependencies() {
        assertNoDependency("${package}.facade..",
                "${package}.adapter..", "${package}.application..", "${package}.common..",
                "${package}.domain..", "${package}.infrastructure..", "${package}.start..");
    }

    @Test
    void common_has_no_business_layer_dependencies() {
        assertNoDependency("${package}.common..",
                "${package}.adapter..", "${package}.application..", "${package}.domain..",
                "${package}.facade..", "${package}.infrastructure..", "${package}.start..");
    }

    @Test
    void business_packages_are_domain_first() {
        Set<String> packageNames = classes.stream()
                .map(JavaClass::getPackageName)
                .collect(Collectors.toSet());
        List.of(
                "${package}.adapter.controller",
                "${package}.adapter.mq",
                "${package}.adapter.rpc",
                "${package}.adapter.graphql",
                "${package}.adapter.facade",
                "${package}.adapter.dto",
                "${package}.adapter.vo",
                "${package}.adapter.convertor",
                "${package}.adapter.validation",
                "${package}.adapter.validators",
                "${package}.application.manage",
                "${package}.application.command",
                "${package}.application.query",
                "${package}.application.result",
                "${package}.application.convertor",
                "${package}.application.validators",
                "${package}.application.assemblers",
                "${package}.application.config",
                "${package}.facade.api",
                "${package}.facade.dto",
                "${package}.facade.enums",
                "${package}.facade.exceptions",
                "${package}.facade.utils",
                "${package}.infrastructure.repo",
                "${package}.infrastructure.service",
                "${package}.infrastructure.validators",
                "${package}.infrastructure.client",
                "${package}.infrastructure.mq",
                "${package}.infrastructure.cache",
                "${package}.domain.common",
                "${package}.domain.student",
                "${package}.domain.teaching.model")
                .forEach(forbidden -> assertThat(packageNames)
                        .noneMatch(name -> name.startsWith(forbidden)));
    }

    @Test
    void outbound_ports_have_no_reversed_implementation_packages() {
        Set<String> packageNames = classes.stream()
                .map(JavaClass::getPackageName)
                .collect(Collectors.toSet());
        assertThat(packageNames).noneMatch(name -> name.startsWith("${package}.application.client"));
        assertThat(packageNames).noneMatch(name -> name.startsWith("${package}.domain.user.service.impl"));
        assertThat(packageNames).noneMatch(name -> name.startsWith("${package}.domain.teaching.service.impl"));
    }

    private void assertNoDependency(String originPackage, String... forbiddenPackages) {
        noClasses().that().resideInAPackage(originPackage)
                .should().dependOnClassesThat().resideInAnyPackage(forbiddenPackages)
                .check(classes);
    }
}
