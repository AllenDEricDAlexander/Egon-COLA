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

[
    "student-management-evaluation-common/src/main/java/it/pkg/common/exceptions/EvaluationBizException.java",
    "student-management-evaluation-facade/src/main/java/it/pkg/facade/api/CourseFacade.java",
    "student-management-evaluation-facade/src/main/java/it/pkg/facade/api/ExamFacade.java",
    "student-management-evaluation-facade/src/main/java/it/pkg/facade/api/ScoreFacade.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/entities/course/Course.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/entities/exam/Exam.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/event/course/CourseEventPublisher.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/event/exam/ExamEventPublisher.java",
    "student-management-evaluation-application/src/main/java/it/pkg/application/manage/course/impl/CourseManageImpl.java",
    "student-management-evaluation-application/src/main/java/it/pkg/application/manage/exam/impl/ExamManageImpl.java",
    "student-management-evaluation-application/src/main/java/it/pkg/application/result/PageResult.java",
    "student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/config/RabbitMqConfiguration.java",
    "student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/mq/course/RabbitCourseEventPublisher.java",
    "student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/mq/exam/RabbitExamEventPublisher.java",
    "student-management-evaluation-adapter/src/main/java/it/pkg/adapter/facade/impl/course/CourseFacadeImpl.java",
    "student-management-evaluation-adapter/src/main/java/it/pkg/adapter/facade/impl/exam/ExamFacadeImpl.java",
    "student-management-evaluation-adapter/src/main/java/it/pkg/adapter/mq/exam/RecordScoreConsumer.java",
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

assert assertFile("mvnw").canExecute() || System.getProperty("os.name").toLowerCase().contains("windows")
assertFile("mvnw.cmd")
assertFile("README.md")
assertFile("Dockerfile")
assertFile(".dockerignore")
return true
