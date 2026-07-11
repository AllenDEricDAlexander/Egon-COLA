import groovy.io.FileType
import java.security.MessageDigest

def projectDir = [
    new File(basedir, "project/student-management-evaluation"),
    new File(basedir, "student-management-evaluation"),
    basedir
].find { new File(it, "student-management-evaluation-starter").isDirectory() }
assert projectDir != null: "Expected generated project directory"

def assertFile = { path ->
    def file = new File(projectDir, path)
    assert file.isFile(): "Expected file ${path}"
    file
}

def assertMissing = { path ->
    assert !new File(projectDir, path).exists(): "Unexpected path ${path}"
}

def modules = ["common", "facade", "domain", "application", "infrastructure", "adapter", "starter"]
modules.each { module ->
    assert new File(projectDir, "student-management-evaluation-${module}").isDirectory()
}

def rootPom = new groovy.xml.XmlSlurper(false, false).parse(assertFile("pom.xml"))
assert rootPom.modules.module*.text() == modules.collect { "student-management-evaluation-${it}" }
assert rootPom.properties.'organization-facade.group-id'.text() == "fixture.organization"
assert rootPom.properties.'organization-facade.artifact-id'.text() ==
        "student-management-organization-facade"
assert rootPom.properties.'organization-facade.version'.text() == "1.0.0-fixture"
assert rootPom.properties.'organization-facade.package'.text() == "fixture.organization"
def requiredPackagePaths = [
    "common",
    "common/constants",
    "common/enums",
    "common/exceptions",
    "common/utils",
    "facade",
    "facade/api",
    "facade/dto",
    "facade/dto/course",
    "facade/dto/exam",
    "facade/enums",
    "facade/exceptions",
    "facade/utils",
    "domain",
    "domain/common",
    "domain/entities",
    "domain/entities/course",
    "domain/entities/exam",
    "domain/aggregates",
    "domain/aggregates/course",
    "domain/aggregates/exam",
    "domain/vos",
    "domain/vos/course",
    "domain/vos/exam",
    "domain/service",
    "domain/service/course",
    "domain/service/course/impl",
    "domain/service/exam",
    "domain/service/exam/impl",
    "domain/repos",
    "domain/repos/course",
    "domain/repos/exam",
    "domain/event",
    "domain/event/course",
    "domain/event/exam",
    "domain/validators",
    "domain/validators/course",
    "domain/validators/exam",
    "domain/enums",
    "domain/enums/course",
    "domain/enums/exam",
    "application",
    "application/manage",
    "application/manage/course",
    "application/manage/course/impl",
    "application/manage/exam",
    "application/manage/exam/impl",
    "application/command",
    "application/command/course",
    "application/command/exam",
    "application/query",
    "application/query/course",
    "application/query/exam",
    "application/result",
    "application/result/course",
    "application/result/exam",
    "application/converter",
    "application/converter/course",
    "application/converter/exam",
    "application/validators",
    "application/validators/course",
    "application/validators/exam",
    "application/assemblers",
    "application/assemblers/course",
    "application/assemblers/exam",
    "application/exceptions",
    "application/config",
    "infrastructure",
    "infrastructure/repo",
    "infrastructure/repo/course",
    "infrastructure/repo/course/impl",
    "infrastructure/repo/course/po",
    "infrastructure/repo/course/jpa",
    "infrastructure/repo/course/converter",
    "infrastructure/repo/exam",
    "infrastructure/repo/exam/impl",
    "infrastructure/repo/exam/po",
    "infrastructure/repo/exam/jpa",
    "infrastructure/repo/exam/converter",
    "infrastructure/mq",
    "infrastructure/mq/course",
    "infrastructure/mq/exam",
    "infrastructure/mq/message",
    "infrastructure/validators",
    "infrastructure/aop",
    "infrastructure/config",
    "adapter",
    "adapter/facade",
    "adapter/facade/impl",
    "adapter/facade/impl/course",
    "adapter/facade/impl/exam",
    "adapter/mq",
    "adapter/mq/course",
    "adapter/mq/exam",
    "adapter/dto",
    "adapter/dto/course",
    "adapter/dto/exam",
    "adapter/converter",
    "adapter/converter/course",
    "adapter/converter/exam",
    "adapter/validators",
    "adapter/validators/course",
    "adapter/validators/exam",
    "adapter/handler",
    "starter",
    "starter/config",
    "starter/config/async",
    "starter/config/encryption",
]
requiredPackagePaths.each { packagePath ->
    def separator = packagePath.indexOf('/')
    def module = separator < 0 ? packagePath : packagePath.substring(0, separator)
    assertFile("student-management-evaluation-${module}/src/main/java/it/pkg/${packagePath}/package-info.java")
}

modules.each { module ->
    ["src/main/java", "src/main/resources", "src/test/java", "src/test/resources"].each { path ->
        assert new File(projectDir, "student-management-evaluation-${module}/${path}").isDirectory()
    }
}


def internalDependencies = { module ->
    def pom = new groovy.xml.XmlSlurper(false, false)
            .parse(assertFile("student-management-evaluation-${module}/pom.xml"))
    pom.dependencies.dependency.findAll {
        it.groupId.text() == "archetype.it" &&
                it.artifactId.text().startsWith("student-management-evaluation-")
    }*.artifactId*.text().collect { it - "student-management-evaluation-" }
}

assert internalDependencies("common") == []
assert internalDependencies("facade") == []
assert internalDependencies("domain") == ["common"]
assert internalDependencies("application") == ["domain"]
assert internalDependencies("infrastructure") == ["domain"]
assert internalDependencies("adapter") == ["application", "facade"]
assert internalDependencies("starter") == ["adapter", "infrastructure"]

def dependencyArtifacts = { module ->
    def pom = new groovy.xml.XmlSlurper(false, false)
            .parse(assertFile("student-management-evaluation-${module}/pom.xml"))
    pom.dependencies.dependency*.artifactId*.text()
}
modules.each { module ->
    def artifacts = dependencyArtifacts(module)
    if (module in ["infrastructure", "adapter"]) {
        assert "spring-boot-starter-amqp" in artifacts
    } else {
        assert !("spring-boot-starter-amqp" in artifacts)
    }
    if (module == "infrastructure") {
        assert "flyway-database-postgresql" in artifacts
    } else {
        assert !("flyway-database-postgresql" in artifacts)
    }
}

[
    "student-management-evaluation-common/src/main/java/it/pkg/common/exceptions/EvaluationBizException.java",
    "student-management-evaluation-facade/src/main/java/it/pkg/facade/api/CourseFacade.java",
    "student-management-evaluation-facade/src/main/java/it/pkg/facade/api/ExamFacade.java",
    "student-management-evaluation-facade/src/main/java/it/pkg/facade/api/ScoreFacade.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/entities/course/Course.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/entities/exam/Exam.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/event/course/CourseEventPublisher.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/event/exam/ExamEventPublisher.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/client/organization/OrganizationDirectoryPort.java",
    "student-management-evaluation-application/src/main/java/it/pkg/application/manage/course/impl/CourseManageImpl.java",
    "student-management-evaluation-application/src/main/java/it/pkg/application/manage/exam/impl/ExamManageImpl.java",
    "student-management-evaluation-application/src/main/java/it/pkg/application/result/PageResult.java",
    "student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/config/RabbitMqConfiguration.java",
    "student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/mq/course/RabbitCourseEventPublisher.java",
    "student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/mq/exam/RabbitExamEventPublisher.java",
    "student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/client/organization/DubboOrganizationDirectoryClient.java",
    "student-management-evaluation-adapter/src/main/java/it/pkg/adapter/facade/impl/course/CourseFacadeImpl.java",
    "student-management-evaluation-adapter/src/main/java/it/pkg/adapter/facade/impl/exam/ExamFacadeImpl.java",
    "student-management-evaluation-adapter/src/main/java/it/pkg/adapter/mq/exam/RecordScoreConsumer.java",
    "student-management-evaluation-adapter/src/test/java/it/pkg/adapter/facade/impl/CourseFacadeImplTest.java",
    "student-management-evaluation-adapter/src/test/java/it/pkg/adapter/facade/impl/ExamFacadeImplTest.java",
    "student-management-evaluation-adapter/src/test/java/it/pkg/adapter/facade/impl/ScoreFacadeImplTest.java",
    "student-management-evaluation-adapter/src/test/java/it/pkg/adapter/mq/exam/RecordScoreConsumerTest.java",
    "student-management-evaluation-adapter/src/test/java/it/pkg/adapter/rpc/EvaluationDubboTripleIntegrationTest.java",
    "student-management-evaluation-domain/src/test/java/it/pkg/domain/course/CourseDomainServiceTest.java",
    "student-management-evaluation-domain/src/test/java/it/pkg/domain/exam/ExamDomainServiceTest.java",
    "student-management-evaluation-domain/src/test/java/it/pkg/domain/exam/ScoreDomainServiceTest.java",
    "student-management-evaluation-infrastructure/src/test/java/it/pkg/infrastructure/repo/course/CourseRepositoryTest.java",
    "student-management-evaluation-infrastructure/src/test/java/it/pkg/infrastructure/repo/course/CourseScheduleRepositoryTest.java",
    "student-management-evaluation-infrastructure/src/test/java/it/pkg/infrastructure/repo/exam/ExamRepositoryTest.java",
    "student-management-evaluation-infrastructure/src/test/java/it/pkg/infrastructure/repo/exam/ExamPaperRepositoryTest.java",
    "student-management-evaluation-infrastructure/src/test/java/it/pkg/infrastructure/repo/exam/ScoreRepositoryTest.java",
    "student-management-evaluation-infrastructure/src/test/java/it/pkg/infrastructure/migration/EvaluationMigrationTest.java",
    "student-management-evaluation-infrastructure/src/test/java/it/pkg/infrastructure/mq/RabbitCourseEventPublisherTest.java",
    "student-management-evaluation-infrastructure/src/test/java/it/pkg/infrastructure/mq/RabbitExamEventPublisherTest.java",
    "student-management-evaluation-infrastructure/src/test/java/it/pkg/infrastructure/mq/RabbitMqConfigurationTest.java",
    "student-management-evaluation-starter/src/test/java/it/pkg/starter/EvaluationExternalFreeContextTest.java",
    "student-management-evaluation-starter/src/test/java/it/pkg/starter/ServiceArchitectureDependencyTest.java"
].each { assertFile(it) }

[
    "student-management-evaluation-facade/src/main/java/it/pkg/facade/api/ExamResultFacade.java",
    "student-management-evaluation-facade/src/main/java/it/pkg/facade/dto/course/CourseDTO.java",
    "student-management-evaluation-application/src/main/java/it/pkg/application/manage/examing",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/client",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/entities/examing",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/repos/examing",
    "student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/client",
    "student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/repo/examing",
    "student-management-evaluation-adapter/src/main/java/it/pkg/adapter/convertor",
    "student-management-evaluation-adapter/src/main/java/it/pkg/adapter/facade/impl/ExamResultFacadeImpl.java",
    "student-management-evaluation-adapter/src/main/java/it/pkg/adapter/mq/ExamResultMessageConsumer.java",
    "student-management-evaluation-starter/src/test/java/it/pkg/starter/EvaluationFlowTest.java"
].each { assertMissing(it) }

def javaFiles = []
projectDir.eachFileRecurse(FileType.FILES) { file ->
    if (file.name.endsWith(".java")) javaFiles << file
}
def forbiddenSegments = ["controller", "web", "filter", "graphql", "vo"]
javaFiles.each { file ->
    def relative = projectDir.toPath().relativize(file.toPath()).toString().replace(File.separator, "/")
    def segments = relative.split("/") as List
    assert forbiddenSegments.intersect(segments).isEmpty(): "Forbidden inbound path ${relative}"
}

def staleTokens = [
    ".adapter.convertor.", ".application.manage.examing.", ".domain.client.",
    ".domain.entities.examing.", ".domain.repos.examing.", ".domain.service.examing.",
    ".facade.api.ExamResultFacade", ".facade.dto.examing.",
    ".common.constants.ErrorCodes", ".common.exception."
]
javaFiles.each { file ->
    staleTokens.each { token ->
        assert !file.getText("UTF-8").contains(token): "Unexpected ${token} in ${file.name}"
    }
}

def javaKeepFiles = []
projectDir.eachFileRecurse(FileType.FILES) { file ->
    def path = file.absolutePath.replace(File.separator, "/")
    if (file.name == ".gitkeep" && path.contains("/src/") && path.contains("/java/")) {
        javaKeepFiles << path
    }
}
assert javaKeepFiles.isEmpty(): "Unexpected Java .gitkeep files"

def migrationDir = new File(projectDir,
        "student-management-evaluation-infrastructure/src/main/resources/db/migration")
def migrations = migrationDir.listFiles().findAll { it.name.endsWith(".sql") }*.name.sort()
assert migrations == [
    "V1__init_student_management_evaluation.sql",
    "V2__align_evaluation_course_exam_domain.sql"
]
def v1 = assertFile("student-management-evaluation-infrastructure/src/main/resources/db/migration/V1__init_student_management_evaluation.sql")
def digest = MessageDigest.getInstance("SHA-256").digest(v1.bytes).encodeHex().toString()
assert digest == "ed5d26a47aef8337b204ab3e77b8d4583fcfc22c3f30cb46fc2055a4429b5df0"

def applicationYaml = assertFile(
        "student-management-evaluation-starter/src/main/resources/application.yml").text
def localYaml = assertFile(
        "student-management-evaluation-starter/src/main/resources/application-local.yml").text
def testYaml = assertFile(
        "student-management-evaluation-starter/src/main/resources/application-test.yml").text
assert applicationYaml.contains("rabbitmq:")
assert localYaml.contains("console:\n      enabled: false")
assert testYaml.contains("rabbitmq:\n      enabled: false")
assert testYaml.contains("listener-auto-startup: false")

def tripleTest = assertFile(
        "student-management-evaluation-adapter/src/test/java/it/pkg/adapter/rpc/EvaluationDubboTripleIntegrationTest.java").text
assert tripleTest.contains("new ServerSocket(0)")
assert tripleTest.contains("tri://127.0.0.1:")
assert tripleTest.contains("reference.get().create")
assert tripleTest.contains("examReference.get().createExam")

[
    "bootstrap.yml", "bootstrap-local.yml", "bootstrap-dev.yml", "bootstrap-test.yml", "bootstrap-prod.yml",
    "application.yml", "application-local.yml", "application-dev.yml", "application-test.yml", "application-prod.yml"
].each { name ->
    assertFile("student-management-evaluation-starter/src/main/resources/${name}")
}

def generatedWorkflow = assertFile(".github/workflows/ci.yml").text
assert generatedWorkflow.contains("SPRING_PROFILES_ACTIVE=test bash ./mvnw -B -ntp clean test")
assert generatedWorkflow.contains("SPRING_PROFILES_ACTIVE=test bash ./mvnw -B -ntp -DskipTests package")
assert generatedWorkflow.contains("docker build -t student-management-evaluation:ci .")

def readme = assertFile("README.md").text
assert readme.contains("require no Nacos, RabbitMQ, or PostgreSQL")
assert readme.contains("V1__init_student_management_evaluation.sql` is immutable")
assert readme.contains("RabbitMQ support is intentionally basic transport")
assert readme.contains("Organization dual-domain Facade integration is deferred")

assert assertFile("mvnw").canExecute() || System.getProperty("os.name").toLowerCase().contains("windows")
assertFile("mvnw.cmd")
assertFile("README.md")
assertFile("Dockerfile")
assertFile(".dockerignore")
return true
