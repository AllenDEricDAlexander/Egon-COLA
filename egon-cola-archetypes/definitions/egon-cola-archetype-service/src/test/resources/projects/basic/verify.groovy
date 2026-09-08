import groovy.io.FileType
import groovy.xml.XmlSlurper

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

def assertLoggingContract = { mainConfigPath, testConfigPath, applicationConfigPath ->
    def mainConfig = assertFile(mainConfigPath)
    def mainText = mainConfig.getText("UTF-8")
    def mainXml = new XmlSlurper(false, false).parse(mainConfig)
    assert mainXml.name() == "configuration"
    assert !mainXml.attributes().containsKey("scan")

    [
        "org/springframework/boot/logging/logback/defaults.xml",
        'source="spring.application.name"',
        'source="spring.profiles.active"',
        'source="logging.file.path"',
        'source="app.logging.instance-name"',
        'source="logging.level.root"',
        'traceId=%X{traceId:-}',
        'spanId=%X{spanId:-}',
        'requestId=%X{requestId:-}',
        'tenantId=%X{tenantId:-}',
        'userId=%X{userId:-}',
        'entId=%X{entId:-}',
        "%kvp",
        '${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}',
        'name="(local | dev | test) &amp; !k8s"',
        'name="k8s"',
        'name="!local &amp; !dev &amp; !test &amp; !k8s"',
        "StructuredLogEncoder",
        'name="ASYNC_APP_FILE"',
        '<discardingThreshold>${ASYNC_DISCARDING_THRESHOLD}</discardingThreshold>',
        '<neverBlock>${ASYNC_NEVER_BLOCK}</neverBlock>',
        "<includeCallerData>false</includeCallerData>",
        'name="ERROR_FILE"',
        "LevelFilter",
        "<level>ERROR</level>",
        "SizeAndTimeBasedRollingPolicy",
        ".%i.log.gz"
    ].each { token ->
        assert mainText.contains(token): "Expected ${mainConfigPath} to contain ${token}"
    }
    assert !mainText.contains("%class")
    assert !mainText.contains("%method")
    assert !mainText.contains("%line")
    assert !mainText.contains("%caller")
    assert !mainText.contains("<contextName>")

    def appenderRefsForProfile = { profileName ->
        def profile = mainXml.springProfile.find { it.@name.text() == profileName }
        assert profile: "Expected ${mainConfigPath} to define profile ${profileName}"
        profile.root.'appender-ref'.collect { it.@ref.text() } as Set
    }
    assert appenderRefsForProfile("(local | dev | test) & !k8s") == (["CONSOLE"] as Set)
    assert appenderRefsForProfile("k8s") == (["JSON_CONSOLE"] as Set)
    assert appenderRefsForProfile("!local & !dev & !test & !k8s") ==
            (["CONSOLE", "ASYNC_APP_FILE", "ERROR_FILE"] as Set)

    mainXml.depthFirst().findAll { it.name() == "root" }.each { rootLogger ->
        def appenderRefs = rootLogger.'appender-ref'.collect { it.@ref.text() }
        assert !(appenderRefs.contains("APP_FILE") && appenderRefs.contains("ASYNC_APP_FILE")):
                "ROOT must not reference synchronous and asynchronous main-file appenders together"
    }

    def testConfig = assertFile(testConfigPath)
    def testText = testConfig.getText("UTF-8")
    def testXml = new XmlSlurper(false, false).parse(testConfig)
    [
        "UTF-8",
        'traceId=%X{traceId:-}',
        'spanId=%X{spanId:-}',
        'requestId=%X{requestId:-}',
        "%kvp",
        '${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}'
    ].each { token ->
        assert testText.contains(token): "Expected ${testConfigPath} to contain ${token}"
    }
    assert testXml.root.@level.text() == "WARN"

    def applicationText = assertFile(applicationConfigPath).getText("UTF-8")
    [
        "logging:",
        "instance-name:",
        "error-total-size-cap:",
        "queue-size:",
        "discarding-threshold:",
        "never-block:",
        "max-flush-time:",
        "web-stack:",
        "persistence:",
        "rpc:",
        "messaging:",
        "max-file-size:",
        "max-history:",
        "total-size-cap:",
        "clean-history-on-start:"
    ].each { token ->
        assert applicationText.contains(token): "Expected ${applicationConfigPath} to contain ${token}"
    }
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

def modules = ["common", "domain", "application", "infrastructure", "adapter", "starter"]
modules.each { module ->
    assert new File(projectDir, "student-management-evaluation-${module}").isDirectory()
}
assertMissing("student-management-evaluation-facade")

def rootPomFile = assertFile("pom.xml")
def rootPomText = rootPomFile.text
def rootPom = new XmlSlurper(false, false).parse(rootPomFile)
def assertVersionProperty = { pomModel, propertyName ->
    assert pomModel.properties."${propertyName}".text().trim():
            "Expected non-empty ${propertyName}"
}
def assertEgonColaBom = { pomModel ->
    assertVersionProperty(pomModel, "egon-cola.version")
    assert !pomModel.dependencyManagement.dependencies.dependency.any {
        it.artifactId.text() == "egon-cola-components-bom"
    }: "Components BOM is inherited from the published archetypes parent"
}

assert rootPom.properties.'java.version'.text() == "21"
assert assertFile("student-management-evaluation-infrastructure/pom.xml").text.contains("<artifactId>shardingsphere-jdbc</artifactId>")
assert assertFile("student-management-evaluation-infrastructure/pom.xml").text.contains("<artifactId>shardingsphere-sharding-core</artifactId>")
assert assertFile("student-management-evaluation-common/pom.xml").text
        .contains("<artifactId>egon-cola-component-common-id-starter</artifactId>")
assert assertFile("student-management-evaluation-domain/pom.xml").text
        .contains("<artifactId>egon-cola-component-common-mybatis-plus-spring-boot-starter</artifactId>")
assert !rootPomText.contains("<artifactId>mybatis-plus-spring-boot3-starter</artifactId>")
[
    "lombok.version",
    "lombok.mapstruct.binding.version",
    "mapstruct-plus.version"
].each { assertVersionProperty(rootPom, it) }
assert rootPom.properties.'lombok.mapstruct.binding.version'.text() == "0.2.0"
assertEgonColaBom(rootPom)
assert rootPomText.contains("<artifactId>lombok-mapstruct-binding</artifactId>")
def compilerPlugin = rootPom.build.plugins.plugin.find {
    it.artifactId.text() == "maven-compiler-plugin"
}
def bindingProcessor = compilerPlugin.configuration.annotationProcessorPaths.path.find {
    it.groupId.text() == "org.projectlombok" &&
            it.artifactId.text() == "lombok-mapstruct-binding"
}
assert bindingProcessor: "Expected lombok-mapstruct-binding annotation processor"
assert bindingProcessor.version.text() == '${lombok.mapstruct.binding.version}'
def lombokConfig = assertFile("lombok.config").text
[
    "config.stopBubbling = true",
    "lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier",
    "lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Value",
    "lombok.addLombokGeneratedAnnotation = true",
    "lombok.anyConstructor.addConstructorProperties = true",
    "lombok.data.flagUsage = warning",
    "lombok.val.flagUsage = warning"
].each { expected -> assert lombokConfig.contains(expected) }
assert rootPom.modules.module*.text() == modules.collect { "student-management-evaluation-${it}" }
assert rootPom.properties.'organization-facade.group-id'.text() == "top.egon"
assert rootPom.properties.'organization-facade.artifact-id'.text() == "egon-cola-organization-facade"
assert rootPom.properties.'organization-facade.version'.text() == '${egon-cola.version}'
assert rootPom.properties.'organization-facade.package'.text() == "top.egon.cola.organization"
assert rootPom.properties.'evaluation-facade.group-id'.text() == "top.egon"
assert rootPom.properties.'evaluation-facade.artifact-id'.text() == "egon-cola-evaluation-facade"
assert rootPom.properties.'evaluation-facade.version'.text() == '${egon-cola.version}'
assert rootPom.properties.'evaluation-facade.package'.text() == "top.egon.cola.evaluation"
def requiredPackagePaths = [
    "common",
    "common/constants",
    "common/enums",
    "common/exceptions",
    "common/utils",
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
    ["aggregates", "entities", "enums", "event", "service", "validators", "vos"].each { role ->
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
        "infrastructure/${businessDomain}/repo/po",
        "infrastructure/${businessDomain}/repo/dao",
        "infrastructure/${businessDomain}/repo/converter",
        "infrastructure/${businessDomain}/service",
        "infrastructure/${businessDomain}/service/impl",
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
assert !serviceApplication.contains("@EnableDubbo")
assert serviceApplication.contains('scanBasePackages = "it.pkg"')
assert !serviceApplication.contains("enableDefaultTransactions")
assert serviceApplication.contains("LongIdGenerator")
assert serviceApplication.contains("@MapperScan")
assert serviceApplication.contains("infrastructure.course.repo.dao")
assert serviceApplication.contains("infrastructure.exam.repo.dao")
assert serviceApplication.contains("exclude = FlywayAutoConfiguration.class")
assert !serviceApplication.contains('"it.pkg.adapter.facade"')


def internalDependencies = { module ->
    def pom = new XmlSlurper(false, false)
            .parse(assertFile("student-management-evaluation-${module}/pom.xml"))
    pom.dependencies.dependency.findAll {
        it.groupId.text() == "archetype.it" &&
                it.artifactId.text().startsWith("student-management-evaluation-")
    }*.artifactId*.text().collect { it - "student-management-evaluation-" }
}

assert internalDependencies("common") == []
assert internalDependencies("domain") == ["common"]
assert internalDependencies("application") == ["domain"]
assert internalDependencies("infrastructure") == ["domain"]
assert internalDependencies("adapter") == ["application"]
assert internalDependencies("starter") == ["adapter", "infrastructure"]

def dependencyArtifacts = { module ->
    def pom = new XmlSlurper(false, false)
            .parse(assertFile("student-management-evaluation-${module}/pom.xml"))
    pom.dependencies.dependency*.artifactId*.text()
}

def externalFacadeDependencies = { module ->
    def pom = new XmlSlurper(false, false)
            .parse(assertFile("student-management-evaluation-${module}/pom.xml"))
    pom.dependencies.dependency.findAll {
        it.artifactId.text() in [
            '${organization-facade.artifact-id}',
            '${evaluation-facade.artifact-id}'
        ]
    }.collect { [groupId: it.groupId.text(), artifactId: it.artifactId.text()] }
}
assert externalFacadeDependencies("infrastructure") == [[
    groupId: '${organization-facade.group-id}',
    artifactId: '${organization-facade.artifact-id}'
]]
assert externalFacadeDependencies("adapter") == [[
    groupId: '${evaluation-facade.group-id}',
    artifactId: '${evaluation-facade.artifact-id}'
]]
modules.findAll { !(it in ["infrastructure", "adapter"]) }.each { module ->
    assert externalFacadeDependencies(module).isEmpty():
            "Unexpected canonical Facade dependency in ${module}"
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
        assert !('${evaluation-facade.artifact-id}' in artifacts)
        assert "egon-cola-component-rpc-starter" in artifacts
    } else if (module == "adapter") {
        assert '${evaluation-facade.artifact-id}' in artifacts
        assert !('${organization-facade.artifact-id}' in artifacts)
        assert !("flyway-database-postgresql" in artifacts)
    } else {
        assert !("flyway-database-postgresql" in artifacts)
        assert !('${organization-facade.artifact-id}' in artifacts)
        assert !('${evaluation-facade.artifact-id}' in artifacts)
    }
}

[
    "student-management-evaluation-common/src/main/java/it/pkg/common/exceptions/EvaluationBizException.java",
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
    "student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/client/organization/NativeOrganizationDirectoryClient.java",
    "student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/client/organization/LocalOrganizationDirectoryStub.java",
    "student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/client/organization/OrganizationClientFailureMapper.java",
    "student-management-evaluation-adapter/src/main/java/it/pkg/adapter/course/facade/impl/CourseFacadeImpl.java",
    "student-management-evaluation-adapter/src/main/java/it/pkg/adapter/exam/facade/impl/ExamFacadeImpl.java",
    "student-management-evaluation-adapter/src/main/java/it/pkg/adapter/exam/mq/RecordScoreConsumer.java",
    "student-management-evaluation-adapter/src/test/java/it/pkg/adapter/course/facade/impl/CourseFacadeImplTest.java",
    "student-management-evaluation-adapter/src/test/java/it/pkg/adapter/exam/facade/impl/ExamFacadeImplTest.java",
    "student-management-evaluation-adapter/src/test/java/it/pkg/adapter/exam/facade/impl/ScoreFacadeImplTest.java",
    "student-management-evaluation-adapter/src/test/java/it/pkg/adapter/exam/mq/RecordScoreConsumerTest.java",
    "student-management-evaluation-adapter/src/test/java/it/pkg/adapter/NativeServiceRpcProviderTest.java",
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
    "student-management-evaluation-infrastructure/src/test/java/it/pkg/infrastructure/client/organization/NativeOrganizationDirectoryClientTest.java",
    "student-management-evaluation-infrastructure/src/test/java/it/pkg/infrastructure/client/organization/LocalOrganizationDirectoryStubTest.java",
    "student-management-evaluation-starter/src/test/java/it/pkg/starter/EvaluationExternalFreeContextTest.java",
    "student-management-evaluation-starter/src/test/java/it/pkg/starter/EvaluationDataSourceModeTest.java"
].each { assertFile(it) }

assertMissing("student-management-evaluation-starter/src/test/java/it/pkg/starter/ServiceArchitectureDependencyTest.java")
def serviceStarterPom = new XmlSlurper(false, false)
        .parse(assertFile("student-management-evaluation-starter/pom.xml"))
def architecturePlugin = serviceStarterPom.build.plugins.plugin.find {
    it.groupId.text() == "top.egon" &&
            it.artifactId.text() == "egon-cola-component-bytecode-architecture-maven-plugin"
}
assert architecturePlugin
assert architecturePlugin.version.text() == '${egon-cola.version}'
assert architecturePlugin.executions.execution.goals.goal*.text().contains("check-reactor")
assertFile("student-management-evaluation-starter/target/egon-cola-architecture/architecture-report.json")
def allPomText = []
projectDir.eachFileRecurse(FileType.FILES) { file ->
    if (file.name == "pom.xml") allPomText << file.getText("UTF-8")
}
assert allPomText.every { !it.contains("archunit-junit5") && !it.contains("archunit.version") }

[
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
    it.getText("UTF-8").contains("import top.egon.cola.organization.facade.")
}
assert providerImports.every {
    def path = javaPath(it)
    (path.startsWith("student-management-evaluation-infrastructure/src/")
            || path == "student-management-evaluation-infrastructure/target/generated-sources/annotations/it/pkg/infrastructure/client/organization/OrganizationDirectoryConverterImpl.java")
            && path.contains("/infrastructure/client/organization/")
}: "Organization Facade imports escaped Infrastructure client: ${providerImports.collect(javaPath)}"

assert javaFiles.every { !it.text.contains("import org.apache.dubbo.") }:
        "Native source must not import Dubbo"

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
assert !localOrganizationStub.contains("top.egon.cola.organization.facade")
assert !localOrganizationStub.contains("org.apache.dubbo")
assert localOrganizationStub.contains('@Profile("test")')
assert !localOrganizationStub.contains('"local"')

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
def migrations = []
migrationDir.eachFileRecurse(FileType.FILES) { file ->
    if (file.name.endsWith(".sql")) {
        migrations << migrationDir.toPath().relativize(file.toPath()).toString()
                .replace(File.separator, "/")
    }
}
assert migrations.sort() == [
    "sharding/master-data/B20260825_001__baseline_evaluation_master_data_schema.sql",
    "sharding/master-data/V20260726_001__init_evaluation_master_data_schema.sql",
    "sharding/master-data/V20260825_001__migrate_evaluation_master_data_to_egon_model.sql",
    "sharding/shard/B20260825_002__baseline_evaluation_sharded_schema.sql",
    "sharding/shard/V20260726_002__init_evaluation_sharded_schema.sql",
    "sharding/shard/V20260825_002__migrate_evaluation_sharded_to_tenant_model.sql"
]
assertMissing(
        "student-management-evaluation-infrastructure/src/main/resources/db/migration/V1__init_student_management_evaluation.sql")
assertMissing(
        "student-management-evaluation-infrastructure/src/main/resources/db/migration/V2__align_evaluation_course_exam_domain.sql")
assertMissing("student-management-evaluation-common/src/main/java/it/pkg/common/utils/EvaluationIdUtils.java")
assertMissing(
        "student-management-evaluation-infrastructure/src/main/resources/db/migration/default")
assertMissing(
        "student-management-evaluation-infrastructure/src/main/resources/db/migration/sharding/single")
def masterDataMigration = assertFile(
        "student-management-evaluation-infrastructure/src/main/resources/db/migration/sharding/master-data/V20260726_001__init_evaluation_master_data_schema.sql").text
def shardMigration = assertFile(
        "student-management-evaluation-infrastructure/src/main/resources/db/migration/sharding/shard/V20260726_002__init_evaluation_sharded_schema.sql").text
def masterDataMigrationV2 = assertFile(
        "student-management-evaluation-infrastructure/src/main/resources/db/migration/sharding/master-data/V20260825_001__migrate_evaluation_master_data_to_egon_model.sql").text
def shardMigrationV2 = assertFile(
        "student-management-evaluation-infrastructure/src/main/resources/db/migration/sharding/shard/V20260825_002__migrate_evaluation_sharded_to_tenant_model.sql").text
[
    masterDataMigration,
    shardMigration,
    masterDataMigrationV2,
    shardMigrationV2
].each { migration ->
    assert migration.startsWith("-- 变更内容：")
    assert migration.contains("\n-- 影响范围：")
    assert migration.contains("\n-- 兼容性说明：")
}
assert masterDataMigration.contains("CREATE TABLE course")
assert !masterDataMigration.contains("CREATE TABLE exam")
assert shardMigration.contains("CREATE TABLE course_schedule_0")
assert shardMigration.contains("CREATE TABLE exam_0")
assert shardMigration.contains("CREATE TABLE exam_paper_0")
assert shardMigration.contains("CREATE TABLE score_0")
assert shardMigration.contains("REFERENCES exam_0(id)")
assert shardMigration.contains("UNIQUE (exam_id, student_id)")
assert masterDataMigrationV2.contains("evaluation_course")
assert masterDataMigrationV2.contains("tenant_id")
assert shardMigrationV2.contains("evaluation_course_schedule_0")
assert shardMigrationV2.contains("evaluation_exam_0")
assert shardMigrationV2.contains("evaluation_exam_paper_0")
assert shardMigrationV2.contains("evaluation_score_0")
assert shardMigrationV2.contains("tenant_id")

def applicationYaml = assertFile(
        "student-management-evaluation-starter/src/main/resources/application.yml").text
assertLoggingContract(
        "student-management-evaluation-starter/src/main/resources/logback-spring.xml",
        "student-management-evaluation-starter/src/test/resources/logback-test.xml",
        "student-management-evaluation-starter/src/main/resources/application.yml")
def testYaml = assertFile(
        "student-management-evaluation-starter/src/main/resources/application-test.yml").text
assert testYaml.contains("database-name: PUBLIC")
assert !testYaml.contains("DATABASE_TO_LOWER")
assert applicationYaml.contains("default: dev")
assert applicationYaml.contains("shutdown: graceful")
assert applicationYaml.contains("scheduling:")
assert applicationYaml.contains('${SCHEDULING_AWAIT_TERMINATION:30s}')
assert applicationYaml.contains("rabbitmq:")
assert applicationYaml.contains("organization:")
assert applicationYaml.contains("ORGANIZATION_FACADE_TIMEOUT_MS:3000")
assert !applicationYaml.contains("tomcat:")
assert testYaml.contains("rabbitmq:\n      enabled: false")
assert testYaml.contains("organization:\n      enabled: false")
assert testYaml.contains("listener-auto-startup: false")
def nativeProviderTest = assertFile(
        "student-management-evaluation-adapter/src/test/java/it/pkg/adapter/NativeServiceRpcProviderTest.java").text
assert nativeProviderTest.contains("CourseRpcProvider")
assert nativeProviderTest.contains("ExamRpcProvider")
assert nativeProviderTest.contains("ScoreRpcProvider")

[
    "application.yml", "application-dev.yml", "application-test.yml", "application-prod.yml",
    "datasource/sharding.yml", "datasource/sharding-readwrite.yml",
    "sharding/shardingsphere-sharding.yml",
    "sharding/shardingsphere-sharding-readwrite.yml"
].each { name ->
    assertFile("student-management-evaluation-starter/src/main/resources/${name}")
}
def serviceProfileConfigNames = new File(
        projectDir,
        "student-management-evaluation-starter/src/main/resources").listFiles()
        .findAll { it.isFile() && it.name ==~ /(?:application|bootstrap)-.+\.yml/ }
        .collect { it.name }
        .sort()
assert serviceProfileConfigNames == [
    "application-dev.yml",
    "application-prod.yml",
    "application-test.yml"
]: "Only dev, test and prod profile configuration files are allowed"
def serviceShardingApplication = assertFile(
        "student-management-evaluation-starter/src/main/resources/datasource/sharding.yml").text
def serviceApplicationYaml = assertFile(
        "student-management-evaluation-starter/src/main/resources/application.yml").text
assert serviceApplicationYaml.contains('mode: ${APP_DATASOURCE_MODE:SHARDING}')
assert !serviceShardingApplication.contains("mapping-version")
assert serviceShardingApplication.contains('node-count: ${EVALUATION_SHARDING_NODE_COUNT:4}')
assert serviceShardingApplication.contains(
        'node-map: ${EVALUATION_SHARDING_NODE_MAP:0=shard_0:0,1=shard_0:1,2=shard_1:0,3=shard_1:1}')
assert serviceShardingApplication.contains("EVALUATION_SHARDING_MASTER_DATA_URL")
assert serviceShardingApplication.contains("classpath:db/migration/sharding/master-data")
assert serviceShardingApplication.contains("classpath:db/migration/sharding/shard")
def serviceShardingRule = assertFile(
        "student-management-evaluation-starter/src/main/resources/sharding/shardingsphere-sharding.yml").text
assert serviceShardingRule.contains(
        '${app.sharding.database-name:${EVALUATION_SHARDING_DATABASE_NAME:evaluation}}')
assert serviceShardingRule.contains("shardingColumn: tenant_id")
assert serviceShardingRule.contains("evaluation_exam,evaluation_exam_paper,evaluation_score")
assert serviceShardingRule.contains("actualDataNodes: master_data.evaluation_course")
assert serviceShardingRule.contains("LongTenantShardingAlgorithm")
assert !serviceShardingRule.contains("UuidV7BucketShardingAlgorithm")
assert !serviceShardingRule.contains(".public.")
assert !serviceShardingRule.contains("proxy-frontend-database-protocol-type")
assert serviceShardingRule.count("none:") == 2
assert serviceShardingRule.count("auditStrategy:") == 4
assert serviceShardingRule.count("allowHintDisable: false") == 4
assert serviceShardingRule.contains("DML_SHARDING_CONDITIONS")
assert !serviceShardingRule.contains("!SINGLE")
def serviceReadwriteRule = assertFile(
        "student-management-evaluation-starter/src/main/resources/sharding/shardingsphere-sharding-readwrite.yml").text
assert serviceReadwriteRule.contains(
        '${app.sharding.database-name:${EVALUATION_SHARDING_DATABASE_NAME:evaluation}}')
assert serviceReadwriteRule.contains("transactionalReadQueryStrategy: PRIMARY")
assert serviceReadwriteRule.contains("evaluation_exam,evaluation_exam_paper,evaluation_score")
assert serviceReadwriteRule.contains("shardingColumn: tenant_id")
assert serviceReadwriteRule.contains("LongTenantShardingAlgorithm")
assert serviceReadwriteRule.contains("master_data_primary")
assert serviceReadwriteRule.contains("master_data_replica_0")
assert serviceReadwriteRule.count("auditStrategy:") == 4
assert !serviceReadwriteRule.contains(".public.")
assert !serviceReadwriteRule.contains("proxy-frontend-database-protocol-type")
assert !serviceReadwriteRule.contains("!SINGLE")
[
    "student-management-evaluation-application/src/test/java/it/pkg/application/transaction/LocalTransactionBoundaryTest.java",
    "student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/config/datasource/DataSourceModeProperties.java",
    "student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/config/datasource/PhysicalDataSourceFlywayMigrator.java",
    "student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/config/datasource/ShardingDataSourceBootstrapper.java",
    "student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/config/datasource/ShardingDataSourcePropertiesLoader.java",
    "student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/config/datasource/ShardingNodeMap.java",
    "student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/config/datasource/LongTenantShardingAlgorithm.java",
    "student-management-evaluation-infrastructure/src/test/java/it/pkg/infrastructure/config/datasource/DataSourceModePropertiesTest.java",
    "student-management-evaluation-infrastructure/src/test/java/it/pkg/infrastructure/config/datasource/PhysicalDataSourceFlywayMigratorTest.java",
    "student-management-evaluation-infrastructure/src/test/java/it/pkg/infrastructure/config/datasource/ReadwriteRoutingIntegrationTest.java",
    "student-management-evaluation-infrastructure/src/test/java/it/pkg/infrastructure/config/datasource/ShardingDataSourcePropertiesLoaderTest.java",
    "student-management-evaluation-infrastructure/src/test/java/it/pkg/infrastructure/config/datasource/LongTenantShardingAlgorithmTest.java",
    "student-management-evaluation-infrastructure/src/test/java/it/pkg/infrastructure/migration/FlywayMigrationConventionTest.java"
].each { assertFile(it) }
[
    "student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/config/datasource/LogicalDataSourceFlywayMigrationStrategy.java",
    "student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/config/datasource/ShardingDataSourceModeCondition.java",
    "student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/config/datasource/ShardingNodeMapCompatibilityValidator.java",
    "student-management-evaluation-infrastructure/src/test/java/it/pkg/infrastructure/config/datasource/LogicalDataSourceFlywayMigrationStrategyTest.java",
    "student-management-evaluation-infrastructure/src/test/java/it/pkg/infrastructure/config/datasource/ShardingNodeMapCompatibilityValidatorTest.java"
].each { assertMissing(it) }
assertMissing("student-management-evaluation-starter/src/main/resources/bootstrap-local.yml")
assertMissing("student-management-evaluation-starter/src/main/resources/application-local.yml")
assertMissing("student-management-evaluation-starter/src/main/resources/application-sharding.yml")
assertMissing("student-management-evaluation-starter/src/main/resources/application-readwrite.yml")
assert assertFile("pom.xml").text.contains("<spring.profiles.active>test</spring.profiles.active>")

def generatedWorkflow = assertFile(".github/workflows/ci.yml").text
def normalizedGeneratedWorkflow = generatedWorkflow.replaceAll(/\s+/, " ")
assert normalizedGeneratedWorkflow.contains("SPRING_PROFILES_ACTIVE=test bash ./mvnw -B -ntp clean verify")
assert normalizedGeneratedWorkflow.contains("SPRING_PROFILES_ACTIVE=test bash ./mvnw -B -ntp -DskipTests package")
assert normalizedGeneratedWorkflow.contains("docker build --build-arg CONTAINER_ENGINE=docker")
assert normalizedGeneratedWorkflow.contains("--file deploy/container/Dockerfile")
assert normalizedGeneratedWorkflow.contains("--tag student-management-evaluation:ci")
assert !normalizedGeneratedWorkflow.contains("docker build -t student-management-evaluation:ci .")

def readme = assertFile("README.md").text
assert readme.contains("`dev` is the default profile")
assert readme.contains("`feature/*`")
assert readme.contains("`dev`, `release/*`, and `hotfix/*`")
assert readme.contains("`main`")
assert readme.contains("RabbitMQ support is intentionally basic transport")
assert readme.contains("Organization Facade client is an unused infrastructure foundation")
[
    "top.egon:egon-cola-evaluation-facade",
    "top.egon:egon-cola-organization-facade",
    "domain/exam/entities",
    "application/course/manage",
    "infrastructure/exam/repo/dao",
    "infrastructure/exam/service/impl",
    "adapter/exam/mq"
].each { assert readme.contains(it) }
[
    "SPRING_PROFILES_ACTIVE=dev APP_DATASOURCE_MODE=SHARDING",
    "APP_DATASOURCE_MODE=SHARDING_READWRITE",
    "evaluation_course_schedule",
    "tenant_id",
    "physical primary",
    "positive `Long`",
    "VyyyyMMdd_NNN__description.sql",
    "DML_SHARDING_CONDITIONS",
    "from `N` to `2N`",
    "local to one physical database",
    "no online migration, dual-write,"
].each { assert readme.contains(it) }
def serviceReadmeZh = assertFile("README.zh-CN.md").text
[
    "SPRING_PROFILES_ACTIVE=dev APP_DATASOURCE_MODE=SHARDING",
    "tenant_id",
    "正数 `Long`",
    "VyyyyMMdd_NNN__description.sql",
    "DML_SHARDING_CONDITIONS",
    "调整为 `2N`",
    "事务只允许覆盖一个物理库",
    "不提供在线迁移、双写、CDC 或自动搬数"
].each { assert serviceReadmeZh.contains(it) }
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
    "POSTGRES_PASSWORD=",
    "REDIS_PASSWORD=",
    "RABBITMQ_PASSWORD="
].each { expected ->
    assert productionEnv.readLines().contains(expected):
            "Expected production env example line ${expected}"
}
[
    "POSTGRES_PASSWORD=local-postgres",
    "REDIS_PASSWORD=local-redis",
    "RABBITMQ_PASSWORD=local-rabbitmq"
].each { forbidden ->
    assert !productionEnv.contains(forbidden):
            "Production env example must not contain development credential ${forbidden}"
}
assert developmentEnv.contains("IMAGE_TAG=local")
assert developmentEnv.contains("DDC_RPC_TARGET=")
assert productionEnv.contains("REGISTRY=")
assert productionEnv.contains("REGISTRY_NAMESPACE=")
assert productionEnv.contains("IMAGE_TAG=")
assert developmentEnv.contains("IMAGE_NAME=student-management-evaluation")
assert developmentEnv.contains("POSTGRES_DB=student_management_evaluation")
assert developmentEnv.contains("APPLICATION_PORT=8081")
assert developmentEnv.contains("POSTGRES_MASTER_DATA_PORT=5432")
assert developmentEnv.contains("POSTGRES_SHARD_0_PORT=5433")
assert developmentEnv.contains("POSTGRES_SHARD_1_PORT=5434")
assert developmentEnv.contains("ORGANIZATION_FACADE_ENABLED=false")
assert productionEnv.readLines().contains("ORGANIZATION_FACADE_ENABLED=")
def assertDevelopmentCompose = { fileName, engine, requiredApplicationLines ->
    def text = assertFile("deploy/compose/${fileName}").text
    ["application:", "postgres-master-data:", "postgres-shard-0:",
     "postgres-shard-1:", "redis:", "rabbitmq:",
     "healthcheck:", "networks:", "volumes:", "application_logs:",
     "postgres_master_data:", "postgres_shard_0:", "postgres_shard_1:"].each { token ->
        assert text.contains(token): "Expected ${fileName} to contain ${token}"
    }
    assert text.contains("CONTAINER_ENGINE: ${engine}")
    assert text.contains("dockerfile: deploy/container/Dockerfile")
    assert text.contains("context: ../..")
    assert text.contains('stop_grace_period: ${STOP_GRACE_PERIOD:-40s}')
    assert text.contains('SPRING_PROFILES_ACTIVE: dev')
    assert text.contains('APP_DATASOURCE_MODE: "SHARDING"')
    assert !text.contains('APP_DATASOURCE_MODE: ${APP_DATASOURCE_MODE')
    assert text.contains('EVALUATION_SHARDING_MASTER_DATA_URL: jdbc:postgresql://postgres-master-data:5432/${POSTGRES_DB}')
    assert text.contains('EVALUATION_SHARDING_SHARD_0_URL: jdbc:postgresql://postgres-shard-0:5432/${POSTGRES_DB}')
    assert text.contains('EVALUATION_SHARDING_SHARD_1_URL: jdbc:postgresql://postgres-shard-1:5432/${POSTGRES_DB}')
    assert text.contains('EVALUATION_SHARDING_USERNAME: ${POSTGRES_USER}')
    assert text.contains('EVALUATION_SHARDING_PASSWORD: ${POSTGRES_PASSWORD}')
    assert !text.contains("replica")
    assert text.contains("DDC_RPC_TARGET:")
    assert text.contains("DDC_REGISTRY_ACCESS_KEY:")
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
    ["application:", "postgres-master-data:", "postgres-shard-0:",
     "postgres-shard-1:", "redis:", "rabbitmq:",
     "healthcheck:", "networks:", "volumes:", "application_logs:",
     "postgres_master_data:", "postgres_shard_0:", "postgres_shard_1:",
     "read_only: true", "tmpfs:", "mem_limit:", "cpus:",
     "restart: unless-stopped"].each { token ->
        assert text.contains(token): "Expected ${fileName} to contain ${token}"
    }
    assert text.contains('${REGISTRY:?Set REGISTRY}/${REGISTRY_NAMESPACE:?Set REGISTRY_NAMESPACE}/${IMAGE_NAME:?Set IMAGE_NAME}:${IMAGE_TAG:?Set IMAGE_TAG}')
    assert text.contains('stop_grace_period: ${STOP_GRACE_PERIOD:-40s}')
    assert text.contains("SPRING_PROFILES_ACTIVE: prod")
    assert text.contains('APP_DATASOURCE_MODE: "SHARDING"')
    assert !text.contains('APP_DATASOURCE_MODE: ${APP_DATASOURCE_MODE')
    assert text.contains('${POSTGRES_USER:?Set POSTGRES_USER}')
    assert text.contains('${POSTGRES_PASSWORD:?Set POSTGRES_PASSWORD}')
    assert text.contains('EVALUATION_SHARDING_MASTER_DATA_URL: jdbc:postgresql://postgres-master-data:5432/${POSTGRES_DB}')
    assert text.contains('EVALUATION_SHARDING_SHARD_0_URL: jdbc:postgresql://postgres-shard-0:5432/${POSTGRES_DB}')
    assert text.contains('EVALUATION_SHARDING_SHARD_1_URL: jdbc:postgresql://postgres-shard-1:5432/${POSTGRES_DB}')
    assert !text.contains("replica")
    assert text.contains('${REDIS_PASSWORD:?Set REDIS_PASSWORD}')
    assert text.contains('${RABBITMQ_PASSWORD:?Set RABBITMQ_PASSWORD}')
    assert text.contains('DDC_REGISTRY_SECRET_KEY: ${DDC_REGISTRY_SECRET_KEY}')
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
def deliveryReadme = assertFile("deploy/container/README.md").text
def normalizedDeliveryReadme = deliveryReadme.replaceAll(/\s+/, " ")
[
    "One Portable Dockerfile",
    "Docker",
    "Podman",
    "nerdctl",
    "Rootless And Rootful",
    "Development Compose",
    "Production Compose",
    "Persistent Data",
    "Jenkins",
    "does not provide high availability"
].each { token ->
    assert normalizedDeliveryReadme.contains(token): "Expected deployment README to contain ${token}"
}

def generatedReadme = assertFile("README.md").text
[
    "deploy/container/Dockerfile",
    "compose.docker.yaml",
    "compose.podman.yaml",
    "compose.nerdctl.yaml",
    "Jenkinsfile",
    "PUBLISH_IMAGE"
].each { token ->
    assert generatedReadme.contains(token): "Expected generated README to contain ${token}"
}
assert !generatedReadme.contains("docker build -t")
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

[
    "adapter/course/converter/CourseFacadeConverter.java",
    "adapter/exam/converter/ExamFacadeConverter.java",
    "adapter/exam/converter/ScoreFacadeConverter.java"
].each { relativePath ->
    def mapper = assertFile(
            "student-management-evaluation-adapter/src/main/java/it/pkg/${relativePath}").text
    assert mapper.contains("@Mapper(")
    assert mapper.contains("ReportingPolicy.ERROR")
    assert mapper.contains("@BeforeMapping")
}

def coursePo = assertFile(
        "student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/course/repo/po/CoursePO.java").text
assert coursePo.contains("@Data")
assert coursePo.contains("@NoArgsConstructor")
assert coursePo.contains("@AllArgsConstructor")
assert coursePo.contains("@Builder")
assert coursePo.contains("@Accessors(chain = true)")
assert coursePo.contains('@TableName("evaluation_course")')
assert coursePo.contains("extends EgonModel<CoursePO>")
assert !coursePo.contains("@RequiredArgsConstructor")
assert !coursePo.contains("@SuperBuilder")

def persistencePoSources = javaFiles.findAll { file ->
    def path = javaPath(file)
    path.contains("/infrastructure/") && path.contains("/repo/po/")
            && file.name.endsWith("PO.java")
}
assert persistencePoSources.size() == 5
persistencePoSources.each { file ->
    def source = file.text
    ["@Data", "@NoArgsConstructor", "@AllArgsConstructor", "@Builder",
     "@Accessors(chain = true)", "@TableName", "extends EgonModel<"].each { token ->
        assert source.contains(token): "Expected ${file.name} to contain ${token}"
    }
    assert !source.contains("@RequiredArgsConstructor")
    assert !source.contains("@SuperBuilder")
}
def domainServiceSources = javaFiles.findAll { file ->
    def path = javaPath(file)
    path.contains("/domain/") && path.contains("/service/")
            && file.name.endsWith("DomainService.java")
}
assert domainServiceSources.size() == 3
domainServiceSources.each { file ->
    assert file.text.contains("extends EgonColaIService<"):
            "Domain service must extend EgonColaIService: ${file.name}"
}
def infrastructureServiceSources = javaFiles.findAll { file ->
    def path = javaPath(file)
    path.contains("/infrastructure/") && path.contains("/service/impl/")
            && file.name.endsWith("DomainServiceImpl.java")
}
assert infrastructureServiceSources.size() == 3
infrastructureServiceSources.each { file ->
    assert file.text.contains("extends EgonColaServiceImpl<"):
            "Infrastructure service must extend EgonColaServiceImpl: ${file.name}"
}

[
    "student-management-evaluation-adapter/src/main/java/it/pkg/adapter/exam/mq/RecordScoreConsumer.java",
    "student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/course/mq/RabbitCourseEventPublisher.java",
    "student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/exam/mq/RabbitExamEventPublisher.java"
].each { path ->
    def source = assertFile(path).text
    assert source.contains("@RequiredArgsConstructor")
}

def genericConverterSources = []
projectDir.traverse(type: FileType.FILES) { file ->
    def path = projectDir.toPath().relativize(file.toPath()).toString().replace(File.separator, "/")
    if (path.contains("/src/main/java/") && file.name.endsWith(".java")) {
        genericConverterSources << file.text
    }
}
genericConverterSources.each { source ->
    assert !source.contains("import io.github.linpeilie.Converter;")
    assert !source.contains("private final Converter converter;")
    assert !source.contains('@Qualifier("converter")')
}
def sourceBoundaryFiles = []
projectDir.eachFileRecurse { candidate ->
    def candidatePath = projectDir.toPath().relativize(candidate.toPath()).toString().replace(File.separator, '/')
    if (candidate.isFile() && !candidatePath.startsWith('target/')) {
        sourceBoundaryFiles << candidate
    }
}
assert sourceBoundaryFiles.every { candidate ->
    def relativePath = projectDir.toPath().relativize(candidate.toPath()).toString().replace(File.separator, '/')
    !relativePath.contains('.generated')
}
[
    'top.egon.internal.archetype.source',
    'egon-cola-source-service',
    '0.1.0-SNAPSHOT'
].each { forbiddenToken ->
    sourceBoundaryFiles.each { candidate ->
        assert !candidate.getText('UTF-8').contains(forbiddenToken):
                "Generated project leaked source sentinel ${forbiddenToken} in ${candidate}"
    }
}
// Verify Maven filtering and Java argument parsing without starting the application.
def launchModule = new File(projectDir, "student-management-evaluation-starter")
def launchTemplate = new File(launchModule, "src/main/launch/launch.args")
assert launchTemplate.isFile(): "Expected external launch argument template"
assert launchTemplate.text.contains('@run.jvm-args@')
assert launchTemplate.text.contains('@run.config-location@')
def launchArgs = new File(launchModule, "target/launch.args")
assert launchArgs.isFile(): "Maven must generate target/launch.args"
assert !launchArgs.text.contains('@run.')
assert !new File(launchModule, "target/classes/launch.args").exists()
def launchJava = new File(System.getProperty("java.home"),
        System.getProperty("os.name").toLowerCase().contains("windows") ? "bin/java.exe" : "bin/java")
def launchCheck = new ProcessBuilder(launchJava.absolutePath, "@${launchArgs.absolutePath}",
        "-XshowSettings:properties", "-version").redirectErrorStream(true).start()
def launchOutput = launchCheck.inputStream.getText("UTF-8")
assert launchCheck.waitFor() == 0: launchOutput
assert launchOutput.contains("spring.profiles.active = dev"): launchOutput
assert launchOutput.contains("server.port = 8081"): launchOutput
assert launchOutput.contains("spring.config.additional-location = optional:file:./config/override.yml"): launchOutput

// Verify the released parent and the actual packaged runtime, separately from BOM management.
def releasedParent = new XmlSlurper(false, false).parse(new File(projectDir, 'pom.xml'))
assert releasedParent.parent.groupId.text() == 'top.egon'
assert releasedParent.parent.artifactId.text() == 'egon-cola-archetypes-parent'
assert releasedParent.parent.version.text() == releasedParent.properties.'egon-cola.version'.text()
assert releasedParent.parent.version.text() ==~ /[0-9]+(?:\.[0-9]+)+(?:[-.][A-Za-z0-9]+)*/
assert releasedParent.parent.relativePath.size() == 1 && !releasedParent.parent.relativePath.text()
['commons-lang3.version', 'commons.lang3.version', 'shardingsphere.version', 'dubbo.version',
 'grpc.version', 'protobuf.version', 'spring-cloud.version', 'spring-cloud-alibaba.version', 'springdoc.version'].each { name ->
    assert !releasedParent.properties."${name}".text(): "Version must be inherited: ${name}"
}
def releasedArchive = new File(projectDir, 'student-management-evaluation-starter/target').listFiles()?.find {
    it.name.endsWith('.jar') && !it.name.endsWith('-sources.jar') && !it.name.endsWith('-javadoc.jar')
}
assert releasedArchive: 'Expected packaged consumer runtime'
def releasedLibraries = [] as Set
new java.util.jar.JarFile(releasedArchive).withCloseable { archive ->
    archive.entries().each { entry ->
        if (entry.name.startsWith('BOOT-INF/lib/')) releasedLibraries << entry.name.substring('BOOT-INF/lib/'.length())
    }
}
assert releasedLibraries.contains('spring-boot-3.5.16.jar')
assert releasedLibraries.contains('commons-lang3-3.20.0.jar')
assert releasedLibraries.any { it.startsWith('egon-cola-component-common-core-') }
assert releasedLibraries.contains('shardingsphere-jdbc-5.5.3.jar')
assert releasedLibraries.contains('grpc-core-1.75.0.jar')
assert releasedLibraries.contains('protobuf-java-4.32.0.jar')
['egon-cola-component-rpc-starter-', 'egon-cola-component-rpc-ddc-adapter-',
 'egon-cola-platform-dynamic-config-center-starter-',
 'egon-cola-platform-dynamic-config-center-http-registration-starter-',
 'egon-cola-platform-gateway-starter-openapi-webmvc-'].each { required ->
    assert releasedLibraries.any { it.startsWith(required) }: "Missing native runtime ${required}"
}
assert !releasedLibraries.any { it.startsWith('dubbo-') || it.startsWith('nacos-') || it.startsWith('spring-cloud-starter-alibaba-nacos-') }
['bootstrap.yml', 'bootstrap-dev.yml', 'bootstrap-test.yml', 'bootstrap-prod.yml'].each {
    assert !new File(projectDir, 'student-management-evaluation-starter/src/main/resources/' + it).exists()
}
['application.yml', 'application-dev.yml', 'application-test.yml', 'application-prod.yml'].each { profile ->
    def config = new File(projectDir, 'student-management-evaluation-starter/src/main/resources/' + profile).text
    ['rpc:', 'ddc:', 'provider:', 'consumer:', 'registry:', 'gateway:', 'openapi:', 'idp:'].each { token ->
        assert config.contains(token): "Missing native configuration ${token} in ${profile}"
    }
    assert !config.contains('DUBBO_') && !config.contains('NACOS_')
}
def nativeJava = []
projectDir.traverse(type: FileType.FILES) { candidate ->
    def path = '/' + projectDir.toPath().relativize(candidate.toPath()).toString().replace('\\', '/')
    if (path.contains('/src/main/java/') && !path.contains('/target/') && candidate.name.endsWith('.java')) nativeJava << candidate
}
assert nativeJava.every { !it.text.contains('org.apache.dubbo') }
def nativeProviders = nativeJava.findAll { it.name.endsWith('RpcProvider.java') }
assert nativeProviders.every { it.text.contains('@EgonRpcProvider') && it.text.contains('@RequiredArgsConstructor') }
def nativeOperations = nativeProviders.collectMany { provider ->
    (provider.text =~ /public\s+\w*Response\s+(\w+)\(/).collect { it[1] }
}.sort()
assert nativeOperations == ["createCourse", "scheduleCourse", "getCourse", "pageCourses", "createExam", "attachPaper", "publishExam", "getExam", "recordScore", "getScore", "pageScores"].sort(): "Native operation inventory changed: ${nativeOperations}"
['docker', 'podman', 'nerdctl'].each { engine ->
    ['', '.prod'].each { profile ->
        def compose = new File(projectDir, "deploy/compose/compose.${engine}${profile}.yaml").text
        assert compose.contains('DDC_APP_CODE: ${DDC_APP_CODE:?Set DDC_APP_CODE}')
        ['${artifactId}', '${rootArtifactId}', '${parentArtifactId}'].each { marker ->
            assert !compose.contains(marker): "Unexpanded archetype variable in Compose: ${marker}"
        }
    }
}
def nativeReports = []
projectDir.traverse(type: FileType.FILES) { candidate ->
    if (candidate.path.replace('\\', '/').contains('/target/surefire-reports/') && candidate.name.endsWith('.xml')) nativeReports << candidate
}
["NativeServiceRpcProviderTest", "NativeOrganizationDirectoryClientTest", "NativeServiceConfigurationTest"].each { name ->
    def reportsForTest = nativeReports.findAll {
        it.name.endsWith('.' + name + '.xml') || it.name.contains('.' + name + '$')
    }
    assert reportsForTest: "Missing generated native test ${name}"
    def results = reportsForTest.collect { new XmlSlurper(false, false).parse(it) }
    assert results.sum { it.@tests.text().toInteger() } > 0: "No tests ran for ${name}"
    assert results.every { it.@failures.text() == '0' && it.@errors.text() == '0' }:
            "Generated native test failed: ${name}"
}

println 'Published parent and service runtime boundaries passed'
return true
