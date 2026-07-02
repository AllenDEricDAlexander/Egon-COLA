package top.egon.fable.starter;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class ServiceArchitectureDependencyTest {

    private final JavaClasses importedClasses = new ClassFileImporter().importPackages("top.egon.fable");

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
        assertThat(hasForbiddenInboundPackageSegment("top.egon.fable.adapter.web")).isTrue();
        assertThat(hasForbiddenInboundPackageSegment("top.egon.fable.adapter.web.internal")).isTrue();
        assertThat(hasForbiddenInboundPackageSegment("top.egon.fable.adapter.webhook")).isFalse();
    }

    @Test
    void domainShouldNotDependOnOuterLayersOrFrameworks() {
        noClasses().that().resideInAPackage("top.egon.fable.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "top.egon.fable.adapter..",
                        "top.egon.fable.application..",
                        "top.egon.fable.infrastructure..",
                        "top.egon.fable.facade..",
                        "top.egon.fable.starter..",
                        "org.springframework.data.jpa..",
                        "org.springframework.web..",
                        "org.apache.rocketmq..",
                        "org.apache.kafka..",
                        "org.springframework.amqp..",
                        "com.rabbitmq..",
                        "org.apache.dubbo..",
                        "io.grpc..")
                .check(importedClasses);
    }

    @Test
    void applicationShouldNotDependOnAdapterInfrastructureFacadeOrStarter() {
        noClasses().that().resideInAPackage("top.egon.fable.application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "top.egon.fable.adapter..",
                        "top.egon.fable.infrastructure..",
                        "top.egon.fable.facade..",
                        "top.egon.fable.starter..")
                .check(importedClasses);
    }

    @Test
    void adapterShouldNotDependOnSpringMvc() {
        noClasses().that().resideInAPackage("top.egon.fable.adapter..")
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework.web..")
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
