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

def assertPortableDockerfile = { jarFile, exposedPorts, readinessPort ->
    assertMissing("Dockerfile")
    [
        "Dockerfile.containerd",
        "Dockerfile.nerdctl",
        "Dockerfile.podman",
        "Containerfile",
        "Containerfile.podman",
        "deploy/container/Dockerfile.containerd",
        "deploy/container/Dockerfile.nerdctl",
        "deploy/container/Dockerfile.podman",
        "deploy/container/Containerfile",
        "deploy/container/Containerfile.podman"
    ].each { assertMissing(it) }

    def text = assertFile("deploy/container/Dockerfile").text
    assert text.contains("ARG BUILD_IMAGE=eclipse-temurin:21-jdk-jammy")
    assert text.contains('FROM ${BUILD_IMAGE} AS builder')
    assert text.contains("chmod +x mvnw")
    assert text.contains("./mvnw -B -ntp -DskipTests package")
    assert text.contains("ARG RUNTIME_IMAGE=eclipse-temurin:21-jre-jammy")
    assert text.contains('FROM ${RUNTIME_IMAGE} AS extractor')
    assert text.contains("ARG JAR_FILE=${jarFile}")
    assert text.contains('COPY --from=builder /workspace/${JAR_FILE} app.jar')
    assert text.contains("java -Djarmode=tools -jar app.jar extract --layers --destination extracted")
    assert text.contains('FROM ${RUNTIME_IMAGE} AS runtime')
    assert text.contains("ARG CONTAINER_ENGINE=oci")
    assert text.contains("ARG APP_UID=10001")
    assert text.contains("ARG APP_GID=10001")
    assert text.contains("org.opencontainers.image.build.engine")
    assert text.contains("USER app")
    assert text.contains("EXPOSE ${exposedPorts}")
    assert text.contains("http://127.0.0.1:${readinessPort}/actuator/health/readiness")
    assert text.contains("JarLauncher")
    assert !text.contains("--mount=type=cache")
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
    "facade/course",
    "facade/course/dto",
    "facade/dto",
    "facade/exam",
    "facade/exam/dto",
    "facade/enums",
    "facade/exceptions",
    "facade/utils",
    "domain",
    "domain/common",
    "domain/client",
    "domain/client/organization",
    "application",
    "application/result",
    "application/exceptions",
    "application/config",
    "infrastructure",
    "infrastructure/validators",
    "infrastructure/aop",
    "infrastructure/config",
    "infrastructure/client",
    "infrastructure/client/organization",
    "adapter",
    "adapter/course/facade/impl",
    "adapter/course/converter",
    "adapter/course/validators",
    "adapter/exam/facade/impl",
    "adapter/exam/dto",
    "adapter/exam/converter",
    "adapter/exam/mq",
    "adapter/exam/validators",
    "adapter/handler",
    "starter",
    "starter/config",
    "starter/config/async",
    "starter/config/encryption",
]
["course", "exam"].each { businessDomain ->
    ["aggregates", "entities", "enums", "event", "repos", "service", "validators", "vos"].each { role ->
        requiredPackagePaths << "domain/${businessDomain}/${role}"
    }
}
["course", "exam"].each { businessDomain ->
    ["command", "converter", "manage", "query", "result", "validators"].each { role ->
        requiredPackagePaths << "application/${businessDomain}/${role}"
    }
    requiredPackagePaths << "application/${businessDomain}/manage/impl"
}
["course", "exam"].each { businessDomain ->
    requiredPackagePaths.addAll([
        "infrastructure/${businessDomain}/repo",
        "infrastructure/${businessDomain}/repo/impl",
        "infrastructure/${businessDomain}/repo/po",
        "infrastructure/${businessDomain}/repo/jpa",
        "infrastructure/${businessDomain}/repo/converter",
        "infrastructure/${businessDomain}/mq",
        "infrastructure/${businessDomain}/mq/message"
    ])
}
requiredPackagePaths.each { packagePath ->
    def separator = packagePath.indexOf('/')
    def module = separator < 0 ? packagePath : packagePath.substring(0, separator)
    assertFile("student-management-evaluation-${module}/src/main/java/it/pkg/${packagePath}/package-info.java")
}
["aggregates", "entities", "enums", "event", "repos", "service", "validators", "vos"].each { role ->
    ["course", "exam"].each { businessDomain ->
        assertMissing("student-management-evaluation-domain/src/main/java/it/pkg/domain/${role}/${businessDomain}")
    }
}
["command", "converter", "manage", "query", "result", "validators"].each { role ->
    ["course", "exam"].each { businessDomain ->
        assertMissing("student-management-evaluation-application/src/main/java/it/pkg/application/${role}/${businessDomain}")
    }
}
assertMissing("student-management-evaluation-application/src/main/java/it/pkg/application/assemblers")
["course", "exam"].each { businessDomain ->
    assertMissing("student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/repo/${businessDomain}")
    assertMissing("student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/mq/${businessDomain}")
}
assertMissing("student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/mq/message")
[
    "facade/impl/course", "facade/impl/exam",
    "converter/course", "converter/exam",
    "validators/course", "validators/exam",
    "dto/course", "dto/exam",
    "mq/course", "mq/exam"
].each { oldPath ->
    assertMissing("student-management-evaluation-adapter/src/main/java/it/pkg/adapter/${oldPath}")
}

modules.each { module ->
    ["src/main/java", "src/main/resources", "src/test/java", "src/test/resources"].each { path ->
        assert new File(projectDir, "student-management-evaluation-${module}/${path}").isDirectory()
    }
}

def serviceApplication = assertFile(
        "student-management-evaluation-starter/src/main/java/it/pkg/starter/EvaluationServiceApplication.java").text
assert serviceApplication.contains('"it.pkg.adapter.course.facade.impl"')
assert serviceApplication.contains('"it.pkg.adapter.exam.facade.impl"')
assert !serviceApplication.contains('"it.pkg.adapter.facade"')


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

def externalFacadeDependencies = { module ->
    def pom = new groovy.xml.XmlSlurper(false, false)
            .parse(assertFile("student-management-evaluation-${module}/pom.xml"))
    pom.dependencies.dependency.findAll {
        it.groupId.text() == '${organization-facade.group-id}'
                || it.artifactId.text() == '${organization-facade.artifact-id}'
    }.collect { [groupId: it.groupId.text(), artifactId: it.artifactId.text()] }
}
assert externalFacadeDependencies("infrastructure") == [[
    groupId: '${organization-facade.group-id}',
    artifactId: '${organization-facade.artifact-id}'
]]
modules.findAll { it != "infrastructure" }.each { module ->
    assert externalFacadeDependencies(module).isEmpty():
            "Unexpected Organization Facade dependency in ${module}"
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
        assert '${organization-facade.artifact-id}' in artifacts
        assert "dubbo-spring-boot-starter" in artifacts
    } else {
        assert !("flyway-database-postgresql" in artifacts)
        assert !('${organization-facade.artifact-id}' in artifacts)
    }
}

[
    "student-management-evaluation-common/src/main/java/it/pkg/common/exceptions/EvaluationBizException.java",
    "student-management-evaluation-facade/src/main/java/it/pkg/facade/course/CourseFacade.java",
    "student-management-evaluation-facade/src/main/java/it/pkg/facade/course/dto/CourseResponse.java",
    "student-management-evaluation-facade/src/main/java/it/pkg/facade/exam/ExamFacade.java",
    "student-management-evaluation-facade/src/main/java/it/pkg/facade/exam/ScoreFacade.java",
    "student-management-evaluation-facade/src/main/java/it/pkg/facade/exam/dto/ScoreResponse.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/course/entities/Course.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/exam/entities/Exam.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/course/event/CourseEventPublisher.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/exam/event/ExamEventPublisher.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/client/ExternalDependencyFailure.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/client/ExternalDependencyException.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/client/organization/OrganizationDirectoryPort.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/client/organization/OrganizationUser.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/client/organization/OrganizationSchoolClass.java",
    "student-management-evaluation-application/src/main/java/it/pkg/application/course/manage/impl/CourseManageImpl.java",
    "student-management-evaluation-application/src/main/java/it/pkg/application/exam/manage/impl/ExamManageImpl.java",
    "student-management-evaluation-application/src/main/java/it/pkg/application/result/PageResult.java",
    "student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/config/RabbitMqConfiguration.java",
    "student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/course/mq/RabbitCourseEventPublisher.java",
    "student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/course/mq/message/CourseScheduledMessage.java",
    "student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/exam/mq/RabbitExamEventPublisher.java",
    "student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/exam/mq/message/ExamPublishedMessage.java",
    "student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/exam/mq/message/ScoreRecordedMessage.java",
    "student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/client/organization/DubboOrganizationDirectoryClient.java",
    "student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/client/organization/LocalOrganizationDirectoryStub.java",
    "student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/client/organization/OrganizationClientFailureMapper.java",
    "student-management-evaluation-adapter/src/main/java/it/pkg/adapter/course/facade/impl/CourseFacadeImpl.java",
    "student-management-evaluation-adapter/src/main/java/it/pkg/adapter/exam/facade/impl/ExamFacadeImpl.java",
    "student-management-evaluation-adapter/src/main/java/it/pkg/adapter/exam/mq/RecordScoreConsumer.java",
    "student-management-evaluation-adapter/src/test/java/it/pkg/adapter/course/facade/impl/CourseFacadeImplTest.java",
    "student-management-evaluation-adapter/src/test/java/it/pkg/adapter/exam/facade/impl/ExamFacadeImplTest.java",
    "student-management-evaluation-adapter/src/test/java/it/pkg/adapter/exam/facade/impl/ScoreFacadeImplTest.java",
    "student-management-evaluation-adapter/src/test/java/it/pkg/adapter/exam/mq/RecordScoreConsumerTest.java",
    "student-management-evaluation-adapter/src/test/java/it/pkg/adapter/rpc/EvaluationDubboTripleIntegrationTest.java",
    "student-management-evaluation-domain/src/test/java/it/pkg/domain/course/CourseDomainServiceTest.java",
    "student-management-evaluation-domain/src/test/java/it/pkg/domain/exam/ExamDomainServiceTest.java",
    "student-management-evaluation-domain/src/test/java/it/pkg/domain/exam/ScoreDomainServiceTest.java",
    "student-management-evaluation-infrastructure/src/test/java/it/pkg/infrastructure/course/repo/CourseRepositoryTest.java",
    "student-management-evaluation-infrastructure/src/test/java/it/pkg/infrastructure/course/repo/CourseScheduleRepositoryTest.java",
    "student-management-evaluation-infrastructure/src/test/java/it/pkg/infrastructure/exam/repo/ExamRepositoryTest.java",
    "student-management-evaluation-infrastructure/src/test/java/it/pkg/infrastructure/exam/repo/ExamPaperRepositoryTest.java",
    "student-management-evaluation-infrastructure/src/test/java/it/pkg/infrastructure/exam/repo/ScoreRepositoryTest.java",
    "student-management-evaluation-infrastructure/src/test/java/it/pkg/infrastructure/migration/EvaluationMigrationTest.java",
    "student-management-evaluation-infrastructure/src/test/java/it/pkg/infrastructure/course/mq/RabbitCourseEventPublisherTest.java",
    "student-management-evaluation-infrastructure/src/test/java/it/pkg/infrastructure/exam/mq/RabbitExamEventPublisherTest.java",
    "student-management-evaluation-infrastructure/src/test/java/it/pkg/infrastructure/mq/RabbitMqConfigurationTest.java",
    "student-management-evaluation-infrastructure/src/test/java/it/pkg/infrastructure/client/organization/DubboOrganizationDirectoryClientTest.java",
    "student-management-evaluation-infrastructure/src/test/java/it/pkg/infrastructure/client/organization/LocalOrganizationDirectoryStubTest.java",
    "student-management-evaluation-starter/src/test/java/it/pkg/starter/EvaluationExternalFreeContextTest.java",
    "student-management-evaluation-starter/src/test/java/it/pkg/starter/ServiceArchitectureDependencyTest.java"
].each { assertFile(it) }

[
    "student-management-evaluation-facade/src/main/java/it/pkg/facade/api",
    "student-management-evaluation-facade/src/main/java/it/pkg/facade/dto/course",
    "student-management-evaluation-facade/src/main/java/it/pkg/facade/dto/exam",
    "student-management-evaluation-application/src/main/java/it/pkg/application/examing/manage",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/examing/entities",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/examing/repos",
    "student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/examing/repo",
    "student-management-evaluation-adapter/src/main/java/it/pkg/adapter/exam/convertor",
    "student-management-evaluation-adapter/src/main/java/it/pkg/adapter/exam/facade/impl/ExamResultFacadeImpl.java",
    "student-management-evaluation-adapter/src/main/java/it/pkg/adapter/exam/mq/ExamResultMessageConsumer.java",
    "student-management-evaluation-starter/src/test/java/it/pkg/starter/EvaluationFlowTest.java"
].each { assertMissing(it) }

def javaFiles = []
projectDir.eachFileRecurse(FileType.FILES) { file ->
    if (file.name.endsWith(".java")) javaFiles << file
}
def javaPath = { file ->
    projectDir.toPath().relativize(file.toPath()).toString().replace(File.separator, "/")
}
def assertPackageDocs = { String sourceRoot ->
    def root = new File(projectDir, sourceRoot)
    assert root.isDirectory(): "Expected directory ${sourceRoot}"
    def javaDirs = [] as Set
    root.traverse(type: FileType.FILES) { file ->
        if (file.name.endsWith(".java") && file.name != "package-info.java") {
            javaDirs << file.parentFile
        }
    }
    javaDirs.each { dir ->
        assert new File(dir, "package-info.java").isFile():
                "Missing package-info.java in ${projectDir.toPath().relativize(dir.toPath())}"
    }
}
modules.each { module ->
    assertPackageDocs("student-management-evaluation-${module}/src/main/java")
    assertPackageDocs("student-management-evaluation-${module}/src/test/java")
}
def forbiddenServicePathFragments = [
    "/facade/api/", "/facade/dto/course/", "/facade/dto/exam/",
    "/domain/aggregates/course/", "/domain/aggregates/exam/",
    "/domain/entities/course/", "/domain/entities/exam/",
    "/domain/enums/course/", "/domain/enums/exam/",
    "/domain/event/course/", "/domain/event/exam/",
    "/domain/repos/course/", "/domain/repos/exam/",
    "/domain/service/course/", "/domain/service/exam/",
    "/domain/validators/course/", "/domain/validators/exam/",
    "/domain/vos/course/", "/domain/vos/exam/",
    "/application/command/course/", "/application/command/exam/",
    "/application/converter/course/", "/application/converter/exam/",
    "/application/manage/course/", "/application/manage/exam/",
    "/application/query/course/", "/application/query/exam/",
    "/application/result/course/", "/application/result/exam/",
    "/application/validators/course/", "/application/validators/exam/",
    "/infrastructure/repo/course/", "/infrastructure/repo/exam/",
    "/infrastructure/mq/course/", "/infrastructure/mq/exam/",
    "/adapter/facade/impl/course/", "/adapter/facade/impl/exam/",
    "/adapter/converter/course/", "/adapter/converter/exam/",
    "/adapter/validators/course/", "/adapter/validators/exam/",
    "/adapter/dto/exam/", "/adapter/mq/exam/"
]
def staleServicePaths = javaFiles.collect(javaPath).findAll { path ->
    forbiddenServicePathFragments.any { path.contains(it) }
}
assert staleServicePaths.isEmpty():
        "Unexpected technical-first Service paths: ${staleServicePaths.join(', ')}"
def providerImports = javaFiles.findAll {
    it.getText("UTF-8").contains("import fixture.organization.facade.")
}
assert providerImports.every {
    def path = javaPath(it)
    path.startsWith("student-management-evaluation-infrastructure/src/")
            && path.contains("/infrastructure/client/organization/")
}: "Organization Facade imports escaped Infrastructure client: ${providerImports.collect(javaPath)}"

def dubboReferenceImports = javaFiles.findAll {
    it.getText("UTF-8").contains("import org.apache.dubbo.config.annotation.DubboReference;")
}
assert dubboReferenceImports.every {
    javaPath(it).contains("/infrastructure/client/organization/")
}: "Dubbo references escaped Infrastructure client: ${dubboReferenceImports.collect(javaPath)}"

def applicationManageFiles = javaFiles.findAll {
    def path = javaPath(it)
    path.startsWith("student-management-evaluation-application/src/main/java/")
            && (path.contains("/application/course/manage/")
            || path.contains("/application/exam/manage/"))
}
assert applicationManageFiles.every {
    !it.getText("UTF-8").contains("OrganizationDirectoryPort")
}: "OrganizationDirectoryPort must remain unused by current Application use cases"

def localOrganizationStub = assertFile(
        "student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/client/organization/LocalOrganizationDirectoryStub.java").text
assert !localOrganizationStub.contains("fixture.organization")
assert !localOrganizationStub.contains("org.apache.dubbo")

def forbiddenSegments = ["controller", "web", "filter", "graphql", "vo"]
javaFiles.each { file ->
    def relative = projectDir.toPath().relativize(file.toPath()).toString().replace(File.separator, "/")
    def segments = relative.split("/") as List
    assert forbiddenSegments.intersect(segments).isEmpty(): "Forbidden inbound path ${relative}"
}

def staleTokens = [
    ".adapter.exam.convertor.", ".application.examing.manage.",
    ".domain.examing.entities.", ".domain.examing.repos.", ".domain.examing.service.",
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
assert applicationYaml.contains("organization:")
assert applicationYaml.contains("DUBBO_CONSUMER_TIMEOUT:3000")
assert localYaml.contains("console:\n      enabled: false")
assert localYaml.contains("organization:\n      enabled: false")
assert testYaml.contains("rabbitmq:\n      enabled: false")
assert testYaml.contains("organization:\n      enabled: false")
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
assert readme.contains("Organization Facade client is an unused infrastructure foundation")
[
    "facade/course/dto",
    "domain/exam/entities",
    "application/course/manage",
    "infrastructure/exam/repo",
    "adapter/exam/mq"
].each { assert readme.contains(it) }
assert readme.contains("service-only")
assert !readme.contains("facade/api")
assert !readme.contains("application/manage/course")

assert assertFile("mvnw").canExecute() || System.getProperty("os.name").toLowerCase().contains("windows")
assertFile("mvnw.cmd")
assertFile("README.md")
assertFile(".dockerignore")
assertPortableDockerfile(
        "student-management-evaluation-starter/target/*.jar", "8081 50051", "8081")
def developmentEnv = assertFile("deploy/env/.env.example").text
def productionEnv = assertFile("deploy/env/.env.prod.example").text
[
    "POSTGRES_IMAGE=postgres:17-alpine",
    "REDIS_IMAGE=redis:7.4-alpine",
    "RABBITMQ_IMAGE=rabbitmq:4-management",
    "NACOS_IMAGE=nacos/nacos-server:v2.5.1",
    "POSTGRES_PASSWORD=",
    "REDIS_PASSWORD=",
    "RABBITMQ_PASSWORD=",
    "NACOS_PASSWORD=",
    "NACOS_AUTH_TOKEN="
].each { expected ->
    assert productionEnv.readLines().contains(expected):
            "Expected production env example line ${expected}"
}
[
    "POSTGRES_PASSWORD=local-postgres",
    "REDIS_PASSWORD=local-redis",
    "RABBITMQ_PASSWORD=local-rabbitmq",
    "NACOS_PASSWORD=nacos"
].each { forbidden ->
    assert !productionEnv.contains(forbidden):
            "Production env example must not contain development credential ${forbidden}"
}
assert developmentEnv.contains("IMAGE_TAG=local")
assert developmentEnv.contains("NACOS_AUTH_ENABLE=true")
assert productionEnv.contains("REGISTRY=")
assert productionEnv.contains("REGISTRY_NAMESPACE=")
assert productionEnv.contains("IMAGE_TAG=")
assert developmentEnv.contains("IMAGE_NAME=student-management-evaluation")
assert developmentEnv.contains("POSTGRES_DB=student_management_evaluation")
assert developmentEnv.contains("APPLICATION_PORT=8081")
assert developmentEnv.contains("ORGANIZATION_FACADE_ENABLED=false")
assert productionEnv.readLines().contains("ORGANIZATION_FACADE_ENABLED=")
def assertDevelopmentCompose = { fileName, engine, requiredApplicationLines ->
    def text = assertFile("deploy/compose/${fileName}").text
    ["application:", "postgres:", "redis:", "rabbitmq:", "nacos:",
     "healthcheck:", "networks:", "volumes:", "application_logs:"].each { token ->
        assert text.contains(token): "Expected ${fileName} to contain ${token}"
    }
    assert text.contains("CONTAINER_ENGINE: ${engine}")
    assert text.contains("dockerfile: deploy/container/Dockerfile")
    assert text.contains("context: ../..")
    assert text.contains('SPRING_PROFILES_ACTIVE: dev')
    assert text.contains('jdbc:postgresql://postgres:5432/${POSTGRES_DB}')
    assert text.contains("NACOS_SERVER_ADDR: nacos:8848")
    assert text.contains("DUBBO_REGISTRY_ADDRESS: nacos://nacos:8848")
    assert text.contains('pg_isready -U "$${POSTGRES_USER}" -d "$${POSTGRES_DB}"')
    assert text.contains('redis-cli --no-auth-warning -a "$${REDIS_PASSWORD}" ping')
    assert text.contains('["CMD", "rabbitmq-diagnostics", "-q", "ping"]')
    requiredApplicationLines.each { required ->
        assert text.contains(required): "Expected ${fileName} to contain ${required}"
    }
}
def developmentComposeFiles = [
    "compose.docker.yaml" : "docker",
    "compose.podman.yaml" : "podman",
    "compose.nerdctl.yaml": "nerdctl"
]
developmentComposeFiles.each { fileName, engine ->
    assertDevelopmentCompose(fileName, engine, [
        'MANAGEMENT_SERVER_PORT: ${APPLICATION_PORT:-8081}',
        'ORGANIZATION_FACADE_ENABLED: "false"',
        '"${APPLICATION_PORT:-8081}:${APPLICATION_PORT:-8081}"'
    ])
}
def assertProductionCompose = { fileName, requiredApplicationLines ->
    def text = assertFile("deploy/compose/${fileName}").text
    ["application:", "postgres:", "redis:", "rabbitmq:", "nacos:",
     "healthcheck:", "networks:", "volumes:", "application_logs:",
     "read_only: true", "tmpfs:", "mem_limit:", "cpus:",
     "restart: unless-stopped"].each { token ->
        assert text.contains(token): "Expected ${fileName} to contain ${token}"
    }
    assert text.contains('${REGISTRY:?Set REGISTRY}/${REGISTRY_NAMESPACE:?Set REGISTRY_NAMESPACE}/${IMAGE_NAME:?Set IMAGE_NAME}:${IMAGE_TAG:?Set IMAGE_TAG}')
    assert text.contains("SPRING_PROFILES_ACTIVE: prod")
    assert text.contains('${POSTGRES_USER:?Set POSTGRES_USER}')
    assert text.contains('${POSTGRES_PASSWORD:?Set POSTGRES_PASSWORD}')
    assert text.contains('${REDIS_PASSWORD:?Set REDIS_PASSWORD}')
    assert text.contains('${RABBITMQ_PASSWORD:?Set RABBITMQ_PASSWORD}')
    assert text.contains('${NACOS_AUTH_TOKEN:?Set NACOS_AUTH_TOKEN}')
    assert !text.contains("build:")
    assert !text.contains("local-postgres")
    assert !text.contains("local-redis")
    assert !text.contains("local-rabbitmq")
    assert !text.contains('${POSTGRES_PORT:-5432}:5432')
    assert !text.contains('${RABBITMQ_MANAGEMENT_PORT:-15672}:15672')
    requiredApplicationLines.each { required ->
        assert text.contains(required): "Expected ${fileName} to contain ${required}"
    }
}
def productionComposeFiles = [
    "compose.docker.prod.yaml",
    "compose.podman.prod.yaml",
    "compose.nerdctl.prod.yaml"
]
productionComposeFiles.each { fileName ->
    assertProductionCompose(fileName, [
        'MANAGEMENT_SERVER_PORT: ${APPLICATION_PORT:-8081}',
        'ORGANIZATION_FACADE_ENABLED: "${ORGANIZATION_FACADE_ENABLED:?Set ORGANIZATION_FACADE_ENABLED to true or false}"'
    ])
}
def jenkinsfile = assertFile("Jenkinsfile").text
[
    "pipeline {",
    "choice(name: 'CONTAINER_ENGINE', choices: ['docker', 'podman', 'nerdctl']",
    "string(name: 'CONTAINERD_NAMESPACE', defaultValue: 'default'",
    "string(name: 'REGISTRY', defaultValue: ''",
    "string(name: 'REGISTRY_NAMESPACE', defaultValue: ''",
    "string(name: 'REGISTRY_CREDENTIALS_ID', defaultValue: ''",
    "string(name: 'IMAGE_NAME', defaultValue: ''",
    "string(name: 'IMAGE_TAG', defaultValue: ''",
    "booleanParam(name: 'PUBLISH_IMAGE', defaultValue: false",
    "booleanParam(name: 'PUBLISH_LATEST', defaultValue: false",
    "stage('Preflight')",
    "stage('Test')",
    "stage('Build Image')",
    "stage('Publish Image')",
    "deploy/container/Dockerfile",
    'CONTAINER_ENGINE=${CONTAINER_ENGINE}',
    "credentialsId: params.REGISTRY_CREDENTIALS_ID",
    "--password-stdin",
    "SPRING_PROFILES_ACTIVE=test bash ./mvnw -B -ntp clean verify",
    "allowEmptyResults: true"
].each { token ->
    assert jenkinsfile.contains(token): "Expected Jenkinsfile to contain ${token}"
}
assert !jenkinsfile.contains("docker compose")
assert !jenkinsfile.contains("podman compose")
assert !jenkinsfile.contains("nerdctl compose")
assert !jenkinsfile.contains("withRegistry")
def dockerignoreLines = assertFile(".dockerignore").readLines("UTF-8")
[
    ".git", ".gitignore", ".github", ".idea", ".vscode", "*.iml", ".DS_Store", "",
    "**/target", "**/build", "**/.mvn/wrapper/maven-wrapper.jar",
    "logs", "*.log", ".env", ".env.*", "config/*secret*", "secrets", "*.pem", "*.key"
].each {
    assert dockerignoreLines.contains(it): "Expected .dockerignore to contain line ${it}"
}
def gitignoreLines = assertFile(".gitignore").readLines("UTF-8")
[
    ".env", ".env.*", "!deploy/env/.env.example", "!deploy/env/.env.prod.example",
    "config/application-secrets.yml", "secrets/", "*.pem", "*.key"
].each {
    assert gitignoreLines.contains(it): "Expected .gitignore to contain line ${it}"
}
return true
