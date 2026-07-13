package ${package}.starter;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureDependencyTest {
    private final JavaClasses classes = new ClassFileImporter().importPaths(targetClassPaths());

    @Test
    void enforcesProjectModuleDirections() {
        noClasses().that().resideInAPackage("${package}.common..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "${package}.facade..", "${package}.domain..", "${package}.application..",
                        "${package}.infrastructure..", "${package}.adapter..", "${package}.starter..")
                .check(classes);
        noClasses().that().resideInAPackage("${package}.facade..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "${package}.common..", "${package}.domain..", "${package}.application..",
                        "${package}.infrastructure..", "${package}.adapter..", "${package}.starter..")
                .check(classes);
        noClasses().that().resideInAPackage("${package}.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "${package}.common..", "${package}.facade..", "${package}.application..",
                        "${package}.infrastructure..", "${package}.adapter..", "${package}.starter..")
                .check(classes);
        noClasses().that().resideInAPackage("${package}.application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "${package}.common..", "${package}.facade..", "${package}.infrastructure..",
                        "${package}.adapter..", "${package}.starter..")
                .check(classes);
        noClasses().that().resideInAPackage("${package}.infrastructure..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "${package}.common..", "${package}.facade..", "${package}.application..",
                        "${package}.adapter..", "${package}.starter..")
                .check(classes);
        noClasses().that().resideInAPackage("${package}.adapter..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "${package}.common..", "${package}.domain..", "${package}.infrastructure..",
                        "${package}.starter..")
                .check(classes);
    }

    @Test
    void domainRemainsFrameworkIndependent() {
        noClasses().that().resideInAPackage("${package}.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta.persistence..", "org.apache.dubbo..",
                        "org.springframework.data.redis..", "org.springframework.amqp..")
                .check(classes);
    }

    @Test
    void facadeRemainsAPlainContractModule() {
        noClasses().that().resideInAPackage("${package}.facade..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta.persistence..", "org.apache.dubbo..")
                .check(classes);
    }

    @Test
    void applicationDoesNotOwnTransportOrPersistenceApis() {
        noClasses().that().resideInAPackage("${package}.application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.web..", "jakarta.persistence..", "org.apache.dubbo..",
                        "org.springframework.data.redis..", "org.springframework.amqp..")
                .check(classes);
    }

    @Test
    void externalEvaluationFacadeStaysBehindInfrastructure() {
        noClasses().that().resideInAnyPackage(
                        "${package}.common..",
                        "${package}.facade..",
                        "${package}.domain..",
                        "${package}.application..",
                        "${package}.adapter..",
                        "${package}.starter..")
                .should().dependOnClassesThat().resideInAPackage("${evaluationFacadePackage}..")
                .check(classes);
    }

    @Test
    void adapterDoesNotReachTechnicalImplementations() {
        noClasses().that().resideInAPackage("${package}.adapter..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "${package}.domain..", "${package}.infrastructure..", "jakarta.persistence..",
                        "org.springframework.data.redis..")
                .check(classes);
    }

    @Test
    void facadeImplementationsStayInAdapterDomainPackages() {
        classes().that().haveSimpleNameEndingWith("FacadeImpl")
                .should().resideInAnyPackage(
                        "${package}.adapter.facade.impl.user..",
                        "${package}.adapter.facade.impl.teaching..")
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
