#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.starter;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class ServiceArchitectureDependencyTest {

    private final JavaClasses importedClasses = new ClassFileImporter().importPackages("${package}");

    @Test
    void shouldNotContainForbiddenInboundPackages() {
        for (JavaClass javaClass : importedClasses) {
            assertThat(hasForbiddenInboundPackageSegment(javaClass.getPackageName()))
                    .as(javaClass.getName())
                    .isFalse();
        }
    }

    @Test
    void forbiddenPackageCheckShouldMatchPackageSegmentBoundaries() {
        assertThat(hasForbiddenInboundPackageSegment("${package}.adapter.web")).isTrue();
        assertThat(hasForbiddenInboundPackageSegment("${package}.adapter.web.internal")).isTrue();
        assertThat(hasForbiddenInboundPackageSegment("${package}.adapter.webhook")).isFalse();
    }

    @Test
    void domainShouldNotDependOnOuterLayersOrFrameworks() {
        noClasses().that().resideInAPackage("${package}.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "${package}.adapter..",
                        "${package}.application..",
                        "${package}.infrastructure..",
                        "${package}.facade..",
                        "${package}.starter..",
                        "org.springframework.data.jpa..",
                        "org.springframework.web..",
                        "org.apache.rocketmq..",
                        "org.apache.kafka..",
                        "org.springframework.amqp..",
                        "com.rabbitmq..",
                        "org.apache.dubbo..",
                        "io.grpc..",
                        "${package}..grpc..")
                .check(importedClasses);
    }

    @Test
    void applicationShouldNotDependOnAdapterInfrastructureFacadeOrStarter() {
        noClasses().that().resideInAPackage("${package}.application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "${package}.adapter..",
                        "${package}.infrastructure..",
                        "${package}.facade..",
                        "${package}.common..",
                        "org.springframework.web..",
                        "${package}.starter..")
                .check(importedClasses);
    }

    @Test
    void projectShouldNotDependOnNativeGrpc() {
        noClasses().should().dependOnClassesThat().resideInAnyPackage("io.grpc..", "${package}..grpc..")
                .check(importedClasses);
    }

    @Test
    void facadeShouldNotDependOnImplementationLayers() {
        noClasses().that().resideInAPackage("${package}.facade..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "${package}.domain..",
                        "${package}.application..",
                        "${package}.adapter..",
                        "${package}.infrastructure..",
                        "${package}.starter..")
                .check(importedClasses);
    }

    @Test
    void adapterShouldDependOnlyOnApplicationAndFacadeInsideTheService() {
        noClasses().that().resideInAPackage("${package}.adapter..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "${package}.common..",
                        "${package}.domain..",
                        "${package}.infrastructure..",
                        "${package}.starter..",
                        "org.springframework.web..")
                .check(importedClasses);
    }

    @Test
    void infrastructureShouldDependOnlyOnDomainInsideTheService() {
        noClasses().that().resideInAPackage("${package}.infrastructure..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "${package}.common..",
                        "${package}.facade..",
                        "${package}.application..",
                        "${package}.adapter..",
                        "${package}.starter..")
                .check(importedClasses);
    }

    @Test
    void providerFacadeTypesStayOutOfInnerAndInboundLayers() {
        noClasses().that().resideInAnyPackage(
                        "${package}.common..",
                        "${package}.domain..",
                        "${package}.application..",
                        "${package}.facade..",
                        "${package}.adapter..",
                        "${package}.starter..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "${organizationFacadePackage}..")
                .check(importedClasses);
    }

    @Test
    void commonShouldNotDependOnOtherServiceModules() {
        noClasses().that().resideInAPackage("${package}.common..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "${package}.facade..",
                        "${package}.domain..",
                        "${package}.application..",
                        "${package}.infrastructure..",
                        "${package}.adapter..",
                        "${package}.starter..")
                .check(importedClasses);
    }

    private boolean hasForbiddenInboundPackageSegment(String packageName) {
        for (String segment : packageName.split("\\.")) {
            if ("controller".equals(segment)
                    || "web".equals(segment)
                    || "filter".equals(segment)
                    || "graphql".equals(segment)
                    || "vo".equals(segment)) {
                return true;
            }
        }
        return false;
    }
}
