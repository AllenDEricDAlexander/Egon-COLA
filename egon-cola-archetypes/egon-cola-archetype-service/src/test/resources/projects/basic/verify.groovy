def projectDir = [
    new File(basedir, "project/student-management-evaluation"),
    new File(basedir, "student-management-evaluation"),
    basedir
].find {
    new File(it, "pom.xml").isFile() && new File(it, "student-management-evaluation-starter").isDirectory()
}

assert projectDir != null: "Expected generated project directory"

def assertFile = { path ->
    def file = new File(projectDir, path)
    assert file.isFile(): "Expected file ${path}"
    file
}

def assertDir = { path ->
    def file = new File(projectDir, path)
    assert file.isDirectory(): "Expected directory ${path}"
    file
}

def assertRuntimeConfigFiles = { resourcesDir ->
    [
        "bootstrap.yml",
        "bootstrap-local.yml",
        "bootstrap-dev.yml",
        "bootstrap-test.yml",
        "bootstrap-prod.yml",
        "application.yml",
        "application-local.yml",
        "application-dev.yml",
        "application-test.yml",
        "application-prod.yml"
    ].each {
        assertFile("${resourcesDir}/${it}")
    }
}

def javaFileTexts = { path ->
    def dir = new File(projectDir, path)
    assert dir.isDirectory(): "Expected directory ${path}"
    def files = []
    dir.traverse(type: groovy.io.FileType.FILES) { file ->
        if (file.name.endsWith(".java")) {
            files << file
        }
    }
    files.collect { it.text }
}

def assertNoGenericMapStructConverterInjection = { path ->
    javaFileTexts(path).each { text ->
        assert !text.contains("import io.github.linpeilie.Converter;")
        assert !text.contains("private final Converter converter;")
        assert !text.contains('@Qualifier("converter")')
    }
}

def assertNoJavaText = { path, token ->
    def matches = []
    def dir = new File(projectDir, path)
    assert dir.isDirectory(): "Expected directory ${path}"
    dir.traverse(type: groovy.io.FileType.FILES) { file ->
        if (file.name.endsWith(".java") && file.getText("UTF-8").contains(token)) {
            matches << projectDir.toPath().relativize(file.toPath()).toString().replace(File.separator, "/")
        }
    }
    assert matches.isEmpty(): "Unexpected token '${token}' in ${matches.join(', ')}"
}

assertFile("pom.xml")
def mvnw = assertFile("mvnw")
assertFile("mvnw.cmd")
if (!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows")) {
    assert mvnw.canExecute(): "Expected mvnw to be executable"
}
assertFile(".mvn/wrapper/maven-wrapper.properties")
assertFile(".gitignore")
assertFile(".gitattributes")
assertFile("README.md")
assertFile("Dockerfile")
assertFile(".dockerignore")
assert assertFile("README.md").text.contains("Docker")
assert assertFile("README.md").text.contains("docker build -t student-management-evaluation:local .")
assert assertFile("README.md").text.contains("The starter module includes the Spring Boot Web starter only for Actuator management HTTP.")
assert assertFile("README.md").text.contains("to adapter, application, domain, or infrastructure modules")
def dockerfileText = assertFile("Dockerfile").text
assert dockerfileText.contains("FROM eclipse-temurin:21-jre-jammy AS extractor")
assert dockerfileText.contains("FROM eclipse-temurin:21-jre-jammy AS runtime")
assert !dockerfileText.contains(" AS builder")
assert !dockerfileText.contains("dependency:go-offline")
assert !dockerfileText.contains("./mvnw")
assert dockerfileText.contains("ARG STARTER_MODULE=student-management-evaluation-starter")
assert dockerfileText.contains('ARG JAR_FILE=${STARTER_MODULE}/target/*.jar')
assert dockerfileText.contains('COPY ${JAR_FILE} app.jar')
assert dockerfileText.contains("java -Djarmode=tools -jar app.jar extract --layers --destination extracted")
assert dockerfileText.contains("USER app")
assert dockerfileText.contains("EXPOSE 8081 50051")
assert dockerfileText.contains("JarLauncher")
def dockerignoreLines = assertFile(".dockerignore").readLines("UTF-8")
[
    ".git",
    ".gitignore",
    ".github",
    ".idea",
    ".vscode",
    "*.iml",
    ".DS_Store",
    "",
    "**/target/*",
    "!target/*.jar",
    "!*/target/*.jar",
    "**/build",
    "**/.mvn/wrapper/maven-wrapper.jar",
    "",
    "logs",
    "*.log",
    "",
    ".env",
    ".env.*",
    "config/*secret*",
    "secrets",
    "*.pem",
    "*.key"
].each {
    assert dockerignoreLines.contains(it): "Expected .dockerignore to contain line ${it}"
}
def gitignoreLines = assertFile(".gitignore").readLines("UTF-8")
[
    ".env",
    ".env.*",
    "config/application-secrets.yml",
    "secrets/",
    "*.pem",
    "*.key"
].each {
    assert gitignoreLines.contains(it): "Expected .gitignore to contain line ${it}"
}

["common", "facade", "domain", "application", "infrastructure", "adapter", "starter"].each {
    assertDir("student-management-evaluation-${it}")
}

["utils", "enums", "exceptions", "dto"].each {
    assertDir("student-management-evaluation-facade/src/main/java/it/pkg/facade/${it}")
}

[
    "student-management-evaluation-common/src/main/java/it/pkg/common/constants/EvaluationConstants.java",
    "student-management-evaluation-common/src/main/java/it/pkg/common/enums/YesNoEnum.java",
    "student-management-evaluation-common/src/main/java/it/pkg/common/exceptions/EvaluationError.java",
    "student-management-evaluation-common/src/main/java/it/pkg/common/exceptions/EvaluationBizException.java",
    "student-management-evaluation-common/src/main/java/it/pkg/common/utils/EvaluationIdUtils.java",
    "student-management-evaluation-facade/src/main/java/it/pkg/facade/api/ExamFacade.java",
    "student-management-evaluation-facade/src/main/java/it/pkg/facade/api/ScoreFacade.java",
    "student-management-evaluation-facade/src/main/java/it/pkg/facade/dto/course/ScheduleCourseRequest.java",
    "student-management-evaluation-facade/src/main/java/it/pkg/facade/dto/exam/RecordScoreRequest.java",
    "student-management-evaluation-facade/src/main/java/it/pkg/facade/enums/EvaluationFacadeErrorCode.java",
    "student-management-evaluation-facade/src/main/java/it/pkg/facade/exceptions/EvaluationFacadeException.java",
    "student-management-evaluation-facade/src/main/java/it/pkg/facade/utils/EvaluationFacadeAssert.java",
    "student-management-evaluation-facade/src/test/java/it/pkg/facade/EvaluationFacadeContractTest.java"
].each { assertFile(it) }

[
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/entities/exam/Exam.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/entities/exam/ExamPaper.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/entities/exam/Score.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/aggregates/exam/ExamAggregate.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/aggregates/exam/ScoreAggregate.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/vos/exam/ExamId.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/vos/exam/ScoreValue.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/service/exam/ExamDomainService.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/service/exam/ScoreDomainService.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/repos/exam/ExamRepository.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/repos/exam/ExamPaperRepository.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/repos/exam/ScoreRepository.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/event/exam/ExamEventPublisher.java",
    "student-management-evaluation-domain/src/test/java/it/pkg/domain/exam/ExamDomainServiceTest.java",
    "student-management-evaluation-domain/src/test/java/it/pkg/domain/exam/ScoreDomainServiceTest.java"
].each { assertFile(it) }

assert !new File(projectDir, "student-management-evaluation-client").exists()
assert !new File(projectDir, "student-management-evaluation-app").exists()
assert !new File(projectDir, "start").exists()
assert !new File(projectDir, "student-management-" + "organization").exists()

def forbiddenPaths = ["controller", "web", "filter", "graphql", "vo"].collect { "/${it}/" }
def generatedFiles = projectDir.traverse(type: groovy.io.FileType.FILES).collect {
    it.absolutePath.replace(File.separatorChar, '/' as char)
}
forbiddenPaths.each { forbidden ->
    assert !generatedFiles.any { it.contains(forbidden) }: "Forbidden path segment ${forbidden}"
}

def rootPomText = assertFile("pom.xml").text
assert rootPomText.contains("<artifactId>spring-boot-starter-parent</artifactId>")
assert rootPomText.contains("<version>3.5.16</version>")
assert rootPomText.contains("<java.version>21</java.version>")
assert rootPomText.contains("<lombok.version>1.18.38</lombok.version>")
assert rootPomText.contains("<mapstruct-plus.version>1.5.1</mapstruct-plus.version>")
assert rootPomText.contains("<dubbo.version>3.3.6</dubbo.version>")
assert rootPomText.contains("<spring-cloud.version>2025.0.3</spring-cloud.version>")
assert rootPomText.contains("<spring-cloud-alibaba.version>2025.0.0.0</spring-cloud-alibaba.version>")
assert rootPomText.contains("<artifactId>egon-cola-components-bom</artifactId>")
assert rootPomText.contains("<egon-cola.version>5.2.1</egon-cola.version>")
assert !rootPomText.contains("<artifactId>egon-cola-component-common</artifactId>")
assert !rootPomText.contains("<artifactId>egon-cola-component-dynamic-thread-pool-starter</artifactId>")
assert !rootPomText.contains("<artifactId>egon-cola-component-dynamic-thread-pool-admin</artifactId>")
assert !rootPomText.contains("<artifactId>egon-cola-component-dynamic-thread-pool-test</artifactId>")
def commonPomText = assertFile("student-management-evaluation-common/pom.xml").text
assert !commonPomText.contains("<artifactId>egon-cola-component-common-core</artifactId>")
def generatedPomTexts = []
projectDir.traverse(type: groovy.io.FileType.FILES) { file ->
    def path = file.absolutePath.replace(File.separatorChar, '/' as char)
    if (file.name == "pom.xml" && !path.contains("/target/") && !path.contains("/.git/")) {
        generatedPomTexts << file.getText("UTF-8")
    }
}
[
    "egon-cola-component-dynamic-thread-pool-starter",
    "egon-cola-component-dynamic-thread-pool-admin",
    "egon-cola-component-dynamic-thread-pool-test"
].each { artifactId ->
    assert !generatedPomTexts.any { it.contains("<artifactId>${artifactId}</artifactId>") }
}
assert rootPomText.contains("<artifactId>spring-cloud-dependencies</artifactId>")
assert rootPomText.contains("<artifactId>spring-cloud-alibaba-dependencies</artifactId>")
assert rootPomText.contains("<artifactId>dubbo-bom</artifactId>")
assert rootPomText.contains("<artifactId>mapstruct-plus-spring-boot-starter</artifactId>")
assert rootPomText.contains("<artifactId>mapstruct-plus-processor</artifactId>")
assert rootPomText.contains("<module>student-management-evaluation-common</module>")
assert rootPomText.contains("<module>student-management-evaluation-facade</module>")
assert rootPomText.contains("<module>student-management-evaluation-domain</module>")
assert rootPomText.contains("<module>student-management-evaluation-application</module>")
assert rootPomText.contains("<module>student-management-evaluation-infrastructure</module>")
assert rootPomText.contains("<module>student-management-evaluation-adapter</module>")
assert rootPomText.contains("<module>student-management-evaluation-starter</module>")
def webStarter = "spring-boot-starter-" + "web"
assert !rootPomText.contains(webStarter)
assert !rootPomText.contains(webStarter + "flux")

assertFile("lombok.config").text.contains("lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier")

assertRuntimeConfigFiles("student-management-evaluation-starter/src/main/resources")

def adapterPomText = assertFile("student-management-evaluation-adapter/pom.xml").text
assert adapterPomText.contains("<artifactId>dubbo-spring-boot-starter</artifactId>")
assert adapterPomText.contains("<artifactId>mapstruct-plus-spring-boot-starter</artifactId>")
assert !adapterPomText.contains(webStarter)
assert !adapterPomText.contains("spring-boot-starter-webflux")

def serviceStarterPomText = assertFile("student-management-evaluation-starter/pom.xml").text

def allServiceJavaPaths = []
new File(projectDir, "student-management-evaluation-adapter/src/main/java").eachFileRecurse { file ->
    if (file.isFile() && file.name.endsWith(".java")) {
        allServiceJavaPaths << projectDir.toPath().relativize(file.toPath()).toString().replace(File.separator, "/")
    }
}
assert allServiceJavaPaths.every { !it.contains("/controller/") }: "Service adapter must not contain controller package"
assert allServiceJavaPaths.every { !it.contains("/web/") }: "Service adapter must not contain web package"
assert allServiceJavaPaths.every { !it.contains("/filter/") }: "Service adapter must not contain filter package"

def applicationPomText = assertFile("student-management-evaluation-application/pom.xml").text
assert applicationPomText.contains("<artifactId>lombok</artifactId>")
[
    "student-management-evaluation-application/src/main/java/it/pkg/application/command/course/CreateCourseCommand.java",
    "student-management-evaluation-application/src/main/java/it/pkg/application/command/exam/RecordScoreCommand.java",
    "student-management-evaluation-application/src/main/java/it/pkg/application/query/course/GetCourseQuery.java",
    "student-management-evaluation-application/src/main/java/it/pkg/application/query/exam/PageScoreQuery.java",
    "student-management-evaluation-application/src/main/java/it/pkg/application/result/course/CourseResult.java",
    "student-management-evaluation-application/src/main/java/it/pkg/application/result/exam/ScoreResult.java",
    "student-management-evaluation-application/src/main/java/it/pkg/application/manage/exam/ExamManage.java",
    "student-management-evaluation-application/src/main/java/it/pkg/application/manage/exam/ScoreManage.java",
    "student-management-evaluation-application/src/test/java/it/pkg/application/course/CourseManageTest.java",
    "student-management-evaluation-application/src/test/java/it/pkg/application/exam/ExamManageTest.java",
    "student-management-evaluation-application/src/test/java/it/pkg/application/exam/ScoreManageTest.java"
].each { assertFile(it) }

def infrastructurePomText = assertFile("student-management-evaluation-infrastructure/pom.xml").text
assert infrastructurePomText.contains("<artifactId>mapstruct-plus-spring-boot-starter</artifactId>")
assertFile("student-management-evaluation-infrastructure/src/main/resources/db/migration/V1__init_student_management_evaluation.sql")
assertFile("student-management-evaluation-infrastructure/src/main/resources/db/migration/V2__align_evaluation_course_exam_domain.sql")
assertFile("student-management-evaluation-infrastructure/src/test/java/it/pkg/infrastructure/migration/EvaluationMigrationTest.java")
assertFile("student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/repo/course/po/CourseSchedulePo.java")
assertFile("student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/repo/course/impl/CourseScheduleRepositoryImpl.java")

def starterPomText = serviceStarterPomText
def starterPom = new groovy.xml.XmlSlurper(false, false).parse(assertFile("student-management-evaluation-starter/pom.xml"))
def assertSpringBootLayeredJarPlugin = { pomModel ->
    def bootPlugin = pomModel.build.plugins.plugin.find {
        it.artifactId.text() == "spring-boot-maven-plugin"
    }
    assert bootPlugin.artifactId.text() == "spring-boot-maven-plugin": "Expected spring-boot-maven-plugin"
    assert bootPlugin.configuration.layers.enabled.text() == "true"
    assert bootPlugin.configuration.excludes.exclude.any {
        it.groupId.text() == "org.projectlombok" && it.artifactId.text() == "lombok"
    }: "Expected spring-boot-maven-plugin to exclude org.projectlombok:lombok"
}
assert starterPomText.contains("<artifactId>spring-boot-maven-plugin</artifactId>")
assert starterPomText.contains("<layers>")
assert starterPomText.contains("<enabled>true</enabled>")
assertSpringBootLayeredJarPlugin(starterPom)
assert starterPomText.contains("<artifactId>spring-cloud-starter-bootstrap</artifactId>")
assert starterPomText.contains("<artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>")
assert starterPomText.contains("<artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>")
assert starterPomText.contains("<artifactId>micrometer-registry-prometheus</artifactId>")
def asyncConfigurationText = assertFile("student-management-evaluation-starter/src/main/java/it/pkg/starter/config/async/AsyncConfiguration.java").text
assert asyncConfigurationText.contains("implements AsyncConfigurer")
assert asyncConfigurationText.contains("getAsyncUncaughtExceptionHandler")
assert !asyncConfigurationText.contains("ThreadPoolTaskExecutorBuilder")
assert !asyncConfigurationText.contains("applicationTaskExecutor(")
assertFile("student-management-evaluation-starter/src/main/java/it/pkg/starter/config/encryption/ConfigDecryptor.java")
assertFile("student-management-evaluation-starter/src/main/java/it/pkg/starter/config/encryption/ConfigDecryptException.java")
assertFile("student-management-evaluation-starter/src/main/java/it/pkg/starter/config/encryption/ConfigDecryptKeyProvider.java")
assertFile("student-management-evaluation-starter/src/main/java/it/pkg/starter/config/encryption/AesGcmConfigDecryptor.java")
assertFile("student-management-evaluation-starter/src/main/java/it/pkg/starter/config/encryption/ConfigDecryptEnvironmentPostProcessor.java")
assertFile("student-management-evaluation-starter/src/main/java/it/pkg/starter/config/encryption/ConfigCipherCli.java")
def springFactories = assertFile("student-management-evaluation-starter/src/main/resources/META-INF/spring.factories").text
assert springFactories.contains("org.springframework.boot.env.EnvironmentPostProcessor")
assert springFactories.contains("it.pkg.starter.config.encryption.ConfigDecryptEnvironmentPostProcessor")
assertFile("student-management-evaluation-starter/src/test/java/it/pkg/starter/config/encryption/AesGcmConfigDecryptorTest.java")
assertFile("student-management-evaluation-starter/src/test/java/it/pkg/starter/config/encryption/ConfigDecryptEnvironmentPostProcessorTest.java")
assertFile("student-management-evaluation-starter/src/test/java/it/pkg/starter/EvaluationFlowTest.java").text.contains('properties = "dubbo.protocol.port=-1"')

def assertPomContainsProvidedLombok = { pomPath ->
    def pomXml = new groovy.xml.XmlSlurper(false, false).parse(assertFile(pomPath))
    assert pomXml.dependencies.dependency.any {
        it.groupId.text() == "org.projectlombok" &&
                it.artifactId.text() == "lombok" &&
                it.scope.text() == "provided"
    }: "Expected ${pomPath} to declare org.projectlombok:lombok with provided scope"
}

assertPomContainsProvidedLombok("student-management-evaluation-adapter/pom.xml")
assertPomContainsProvidedLombok("student-management-evaluation-application/pom.xml")
assertPomContainsProvidedLombok("student-management-evaluation-facade/pom.xml")
assertPomContainsProvidedLombok("student-management-evaluation-infrastructure/pom.xml")
assertPomContainsProvidedLombok("student-management-evaluation-starter/pom.xml")

def serviceApplicationYaml = assertFile("student-management-evaluation-starter/src/main/resources/application.yml").text
assert serviceApplicationYaml.contains("threads:")
assert serviceApplicationYaml.contains("virtual:")
assert serviceApplicationYaml.contains('${SPRING_THREADS_VIRTUAL_ENABLED:true}')
assert serviceApplicationYaml.contains("timeout-per-shutdown-phase")
assert serviceApplicationYaml.contains("write-dates-as-timestamps: false")
assert serviceApplicationYaml.contains("prometheus")
assert serviceApplicationYaml.contains('${MANAGEMENT_SERVER_PORT:8081}')
assert !serviceApplicationYaml.contains("tomcat:")
assert serviceApplicationYaml.contains("dubbo:")
assert serviceApplicationYaml.contains('name: ${spring.application.name}')
assert serviceApplicationYaml.contains('${DUBBO_REGISTRY_ADDRESS:N/A}')
assert serviceApplicationYaml.contains("name: tri")
assert serviceApplicationYaml.contains('${DUBBO_PORT:50051}')
assert serviceApplicationYaml.contains("timeout: 3000")
assert serviceApplicationYaml.contains("retries: 0")

def serviceApplicationDevYaml = assertFile("student-management-evaluation-starter/src/main/resources/application-dev.yml").text
def serviceApplicationProdYaml = assertFile("student-management-evaluation-starter/src/main/resources/application-prod.yml").text
def serviceBootstrapDevYaml = assertFile("student-management-evaluation-starter/src/main/resources/bootstrap-dev.yml").text
def serviceBootstrapProdYaml = assertFile("student-management-evaluation-starter/src/main/resources/bootstrap-prod.yml").text
def serviceApplicationLocalYaml = assertFile("student-management-evaluation-starter/src/main/resources/application-local.yml").text
def serviceApplicationTestYaml = assertFile("student-management-evaluation-starter/src/main/resources/application-test.yml").text
def serviceBootstrapLocalYaml = assertFile("student-management-evaluation-starter/src/main/resources/bootstrap-local.yml").text
def serviceBootstrapTestYaml = assertFile("student-management-evaluation-starter/src/main/resources/bootstrap-test.yml").text
assert serviceApplicationDevYaml.contains('password: ${DB_PASSWORD:ENC(')
assert serviceApplicationProdYaml.contains('password: ${DB_PASSWORD:ENC(')
assert serviceBootstrapDevYaml.contains('password: ${NACOS_PASSWORD:ENC(')
assert serviceBootstrapProdYaml.contains('password: ${NACOS_PASSWORD:ENC(')
assert serviceApplicationLocalYaml.contains('password: ${DB_PASSWORD:}')
assert serviceApplicationTestYaml.contains('password: ${DB_PASSWORD:}')
assert !serviceBootstrapLocalYaml.contains('ENC(')
assert !serviceBootstrapTestYaml.contains('ENC(')

def wrapper = assertFile(".mvn/wrapper/maven-wrapper.properties").text
assert wrapper.contains("apache-maven/3.9.14/apache-maven-3.9.14-bin.zip")

def modulePom = { module ->
    new groovy.xml.XmlSlurper(false, false).parse(assertFile("student-management-evaluation-${module}/pom.xml"))
}

def dependencies = { moduleXml ->
    moduleXml.dependencies.dependency.collect {
        [
            artifactId: it.artifactId.text(),
            scope: it.scope.text() ?: "compile"
        ]
    }
}

def assertDependency = { deps, artifactId ->
    assert deps.any { it.artifactId == artifactId }: "Expected dependency ${artifactId}"
}

def assertNoDependency = { deps, artifactId ->
    assert !deps.any { it.artifactId == artifactId }: "Unexpected dependency ${artifactId}"
}

def modulePomDependencies = { module ->
    def pomText = assertFile("student-management-evaluation-${module}/pom.xml").text
    def dependenciesText = (pomText =~ /(?s)<dependencies>(.*)<\/dependencies>/)
    if (!dependenciesText.find()) {
        return []
    }
    (dependenciesText.group(1) =~ /<artifactId>student-management-evaluation-([^<]+)<\/artifactId>/).collect { it[1] }
}

assert modulePomDependencies("facade") == []
assert modulePomDependencies("domain") == ["common"]
assert modulePomDependencies("application") == ["domain"]
assert modulePomDependencies("adapter") == ["application", "facade"]
assert modulePomDependencies("infrastructure") == ["domain"]
assert modulePomDependencies("starter") == ["adapter", "infrastructure"]

def facadeDependencies = dependencies(modulePom("facade"))
def domainDependencies = dependencies(modulePom("domain"))
def applicationDependencies = dependencies(modulePom("application"))
def infrastructureDependencies = dependencies(modulePom("infrastructure"))
def adapterDependencies = dependencies(modulePom("adapter"))
def starterDependencies = dependencies(modulePom("starter"))

assertDependency(facadeDependencies, "jakarta.validation-api")
assertDependency(facadeDependencies, "junit-jupiter")
assertNoDependency(facadeDependencies, "spring-boot-starter-validation")
assertDependency(domainDependencies, "spring-boot-starter-validation")
assertDependency(domainDependencies, "junit-jupiter")
assertDependency(applicationDependencies, "spring-boot-starter-validation")
assertDependency(applicationDependencies, "spring-boot-starter-test")
assertDependency(infrastructureDependencies, "spring-boot-starter-validation")
assertDependency(infrastructureDependencies, "spring-boot-starter-amqp")
assertDependency(infrastructureDependencies, "flyway-database-postgresql")
assertDependency(infrastructureDependencies, "spring-boot-starter-test")
assertDependency(adapterDependencies, "spring-boot-starter-validation")
assertDependency(adapterDependencies, "spring-boot-starter-amqp")
assertDependency(adapterDependencies, "spring-boot-starter-test")
assertNoDependency(adapterDependencies, webStarter)
assertNoDependency(adapterDependencies, "spring-boot-starter-webflux")
assertDependency(starterDependencies, webStarter)
assertDependency(starterDependencies, "spring-boot-starter-actuator")

def assertNoMatch = { text, pattern, message ->
    assert !(text =~ pattern).find(): message
}

def assertContainsAll = { text, values, label ->
    values.each {
        assert text.contains(it): "Expected ${label} to contain ${it}"
    }
}

def javaTextUnder = { path ->
    def dir = new File(projectDir, path)
    assert dir.isDirectory(): "Expected directory ${path}"
    dir.traverse(type: groovy.io.FileType.FILES)
            .findAll { it.name.endsWith(".java") }
            .collect { it.text }
            .join("\n")
}

assertFile("student-management-evaluation-application/src/main/java/it/pkg/application/config/DomainServiceConfiguration.java")
assert !new File(projectDir, "student-management-evaluation-application/src/main/java/it/pkg/application/view/course/CourseView.java").exists()
assert !new File(projectDir, "student-management-evaluation-application/src/main/java/it/pkg/application/view/examing/ExamResultView.java").exists()

def courseManageText = assertFile("student-management-evaluation-application/src/main/java/it/pkg/application/manage/course/CourseManage.java").text
assertContainsAll(courseManageText, [
        "import it.pkg.domain.entities.course.Course;",
        "Course create(",
        "String name",
        "int credit",
        "Course getById(",
        "String courseId",
        "@NotBlank",
        "@Positive"
], "CourseManage")
assert !courseManageText.contains("CourseView")

def examManageText = assertFile("student-management-evaluation-application/src/main/java/it/pkg/application/manage/examing/ExamManage.java").text
assertContainsAll(examManageText, [
        "import it.pkg.domain.entities.examing.ExamResult;",
        "ExamResult record(",
        "String courseId",
        "String studentId",
        "int score",
        "ExamResult getById(",
        "String examResultId",
        "@NotBlank",
        "@Min",
        "@Max"
], "ExamManage")
assert !examManageText.contains("ExamResultView")

def courseManageImplText = assertFile("student-management-evaluation-application/src/main/java/it/pkg/application/manage/course/impl/CourseManageImpl.java").text
assertContainsAll(courseManageImplText, [
        '@Service("courseManage")',
        "@RequiredArgsConstructor",
        "@Validated",
        '@Qualifier("courseClientImpl")',
        "courseClient.existsByName(name)",
        "courseClient.save(course)"
], "CourseManageImpl")
assertContainsAll(courseManageImplText, [
        "CourseRepository courseRepository",
        "CourseDomainService courseDomainService",
        "CourseResult create(CreateCourseCommand command)",
        "CourseScheduleResult schedule(ScheduleCourseCommand command)"
], "CourseManageImpl evaluation flow")

def examManageImplText = assertFile("student-management-evaluation-application/src/main/java/it/pkg/application/manage/examing/impl/ExamManageImpl.java").text
assertContainsAll(examManageImplText, [
        '@Service("examManage")',
        "@RequiredArgsConstructor",
        "@Validated",
        '@Qualifier("examResultClientImpl")',
        '@Qualifier("legacyExamDomainService")',
        "examResultClient.save(examResult)"
], "ExamManageImpl")
assert !examManageImplText.contains("ExamResultRepository examResultRepository")
assert !examManageImplText.contains("validateRecordRequest")

def domainServiceConfigurationText = assertFile("student-management-evaluation-application/src/main/java/it/pkg/application/config/DomainServiceConfiguration.java").text
assertContainsAll(domainServiceConfigurationText, [
        '@Bean("examDomainService")',
        '@Bean("courseDomainService")',
        '@Bean("scoreDomainService")',
        '@Bean("legacyExamDomainService")',
        '@Qualifier("courseClientImpl") CourseClient courseClient',
        "new CourseDomainServiceImpl()",
        "new ExamDomainServiceImpl()",
        "new ScoreDomainServiceImpl()"
], "DomainServiceConfiguration")
[
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/common/EvaluationDomainException.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/common/EvaluationPortException.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/entities/course/CourseSchedule.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/aggregates/course/CourseAggregate.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/vos/course/CourseId.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/vos/course/CourseCode.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/service/course/CourseDomainService.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/service/course/impl/CourseDomainServiceImpl.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/repos/course/CourseScheduleRepository.java",
    "student-management-evaluation-domain/src/main/java/it/pkg/domain/event/course/CourseEventPublisher.java",
    "student-management-evaluation-domain/src/test/java/it/pkg/domain/course/CourseDomainServiceTest.java",
    "student-management-evaluation-domain/src/test/java/it/pkg/domain/course/CourseAggregateTest.java"
].each { assertFile(it) }

def courseFacadeImplText = assertFile("student-management-evaluation-adapter/src/main/java/it/pkg/adapter/facade/impl/CourseFacadeImpl.java").text
assertContainsAll(courseFacadeImplText, [
        "@DubboService(",
        "interfaceClass = CourseFacade.class",
        'version = "1.0.0"',
        'group = "course"',
        "@RequiredArgsConstructor",
        '@Qualifier("courseManage")',
        '@Qualifier("courseAdapterConvertor")',
        '@Qualifier("serviceExceptionHandler")'
], "CourseFacadeImpl")

def examFacadeImplText = assertFile("student-management-evaluation-adapter/src/main/java/it/pkg/adapter/facade/impl/ExamResultFacadeImpl.java").text
assertContainsAll(examFacadeImplText, [
        "@DubboService(",
        "interfaceClass = ExamResultFacade.class",
        'version = "1.0.0"',
        'group = "exam-result"',
        "@RequiredArgsConstructor",
        '@Qualifier("examManage")',
        '@Qualifier("examResultAdapterConvertor")',
        '@Qualifier("serviceExceptionHandler")'
], "ExamResultFacadeImpl")

assertNoGenericMapStructConverterInjection("student-management-evaluation-adapter/src/main/java/it/pkg/adapter/convertor")
assertNoGenericMapStructConverterInjection("student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/repo")

assertFile("student-management-evaluation-adapter/src/main/java/it/pkg/adapter/convertor/CourseAdapterMapper.java")
assertFile("student-management-evaluation-adapter/src/main/java/it/pkg/adapter/convertor/ExamResultAdapterMapper.java")
assertFile("student-management-evaluation-adapter/src/main/java/it/pkg/adapter/validation/ValidatorUtils.java")
assertFile("student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/repo/course/converter/CoursePoMapper.java")
assertFile("student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/repo/course/converter/CourseDomainMapper.java")
assertFile("student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/repo/examing/converter/ExamResultPoMapper.java")
assertFile("student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/repo/examing/converter/ExamResultDomainMapper.java")
assertFile("student-management-evaluation-domain/src/main/java/it/pkg/domain/client/course/CourseClient.java")
assertFile("student-management-evaluation-domain/src/main/java/it/pkg/domain/client/examing/ExamResultClient.java")
assertFile("student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/client/impl/course/CourseClientImpl.java")
assertFile("student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/client/impl/examing/ExamResultClientImpl.java")
def courseClientImplText = assertFile("student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/client/impl/course/CourseClientImpl.java").text
assertContainsAll(courseClientImplText, [
        '@Component("courseClientImpl")',
        "@Validated",
        "implements CourseClient",
        '@Qualifier("courseRepositoryImpl")'
], "CourseClientImpl")
assert !courseClientImplText.contains("jakarta.validation.constraints")

def examResultClientImplText = assertFile("student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/client/impl/examing/ExamResultClientImpl.java").text
assertContainsAll(examResultClientImplText, [
        '@Component("examResultClientImpl")',
        "@Validated",
        "implements ExamResultClient",
        '@Qualifier("examResultRepositoryImpl")'
], "ExamResultClientImpl")
assert !examResultClientImplText.contains("jakarta.validation.constraints")
assert !examResultClientImplText.contains("validateRecordRequest")
def courseClientText = assertFile("student-management-evaluation-domain/src/main/java/it/pkg/domain/client/course/CourseClient.java").text
assertContainsAll(courseClientText, [
        "@NotNull",
        "@NotBlank",
        "@Positive"
], "CourseClient")
assert !courseClientText.contains("default ")
def examResultClientText = assertFile("student-management-evaluation-domain/src/main/java/it/pkg/domain/client/examing/ExamResultClient.java").text
assertContainsAll(examResultClientText, [
        "@NotNull",
        "@NotBlank"
], "ExamResultClient")
assert !examResultClientText.contains("@Min")
assert !examResultClientText.contains("@Max")
assert !examResultClientText.contains("default ")
assert !examResultClientText.contains("validateRecordRequest")
assertFile("student-management-evaluation-domain/src/main/java/it/pkg/domain/common/Page.java")
assertFile("student-management-evaluation-facade/src/main/java/it/pkg/facade/dto/PageResponse.java")

assertFile("student-management-evaluation-adapter/src/main/java/it/pkg/adapter/convertor/CourseAdapterMapper.java").text.contains("BaseMapper<Course, CourseDTO>")
assertFile("student-management-evaluation-adapter/src/main/java/it/pkg/adapter/convertor/ExamResultAdapterMapper.java").text.contains("BaseMapper<ExamResult, ExamResultDTO>")
assertFile("student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/repo/course/converter/CoursePoMapper.java").text.contains("BaseMapper<Course, CoursePo>")
assertFile("student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/repo/examing/converter/ExamResultPoMapper.java").text.contains("BaseMapper<ExamResult, ExamResultPo>")

def courseManageTextAfterPage = assertFile("student-management-evaluation-application/src/main/java/it/pkg/application/manage/course/CourseManage.java").text
assert courseManageTextAfterPage.contains("Page<Course> getPage(")
assert courseManageTextAfterPage.contains("int currentPage")
assert courseManageTextAfterPage.contains("int pageSize")
assert courseManageTextAfterPage.contains("import it.pkg.domain.common.Page;")

def courseRepositoryText = assertFile("student-management-evaluation-domain/src/main/java/it/pkg/domain/repos/course/CourseRepository.java").text
assert courseRepositoryText.contains("Page<Course> findPage(int currentPage, int pageSize)")
assert courseRepositoryText.contains("import it.pkg.domain.common.Page;")

def courseFacadeTextAfterPage = assertFile("student-management-evaluation-facade/src/main/java/it/pkg/facade/api/CourseFacade.java").text
assert courseFacadeTextAfterPage.contains("SingleResponse<PageResponse<CourseDTO>> getCourses(int currentPage, int pageSize)")

def courseFacadeImplTextAfterPage = assertFile("student-management-evaluation-adapter/src/main/java/it/pkg/adapter/facade/impl/CourseFacadeImpl.java").text
assert courseFacadeImplTextAfterPage.contains("SingleResponse<PageResponse<CourseDTO>> getCourses")
assert courseFacadeImplTextAfterPage.contains("SingleResponse.of(courseAdapterConvertor.toPageResponse(courseManage.getPage(currentPage, pageSize)))")

def courseRepositoryImplText = assertFile("student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/repo/course/impl/CourseRepositoryImpl.java").text
assertContainsAll(courseRepositoryImplText, [
        '@Repository("courseRepositoryImpl")',
        "@RequiredArgsConstructor",
        '@Qualifier("courseJpaRepository")',
        '@Qualifier("courseConverter")'
], "CourseRepositoryImpl")

def examRepositoryImplText = assertFile("student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/repo/examing/impl/ExamResultRepositoryImpl.java").text
assertContainsAll(examRepositoryImplText, [
        '@Repository("examResultRepositoryImpl")',
        "@RequiredArgsConstructor",
        '@Qualifier("examResultJpaRepository")',
        '@Qualifier("examResultConverter")'
], "ExamResultRepositoryImpl")

def mqConsumerText = assertFile("student-management-evaluation-adapter/src/main/java/it/pkg/adapter/mq/ExamResultMessageConsumer.java").text
assertContainsAll(mqConsumerText, [
        '@Component("examResultMessageConsumer")',
        "@RequiredArgsConstructor",
        '@Qualifier("examManage")',
        '@Qualifier("examResultAdapterConvertor")',
        '@Qualifier("serviceExceptionHandler")',
        "examResultAdapterConvertor.toDTO(examResult)"
], "ExamResultMessageConsumer")

def handlerText = assertFile("student-management-evaluation-adapter/src/main/java/it/pkg/adapter/handler/ServiceExceptionHandler.java").text
assert handlerText.contains('@Component("serviceExceptionHandler")')

def courseDtoText = assertFile("student-management-evaluation-facade/src/main/java/it/pkg/facade/dto/course/CourseDTO.java").text
assertContainsAll(courseDtoText, [
        "implements Serializable",
        "@Data",
        "@Builder",
        "@NoArgsConstructor",
        "@AllArgsConstructor",
        "private String id;",
        "private String name;",
        "private int credit;",
        "private String status;"
], "CourseDTO")

def examDtoText = assertFile("student-management-evaluation-facade/src/main/java/it/pkg/facade/dto/examing/ExamResultDTO.java").text
assertContainsAll(examDtoText, [
        "implements Serializable",
        "@Data",
        "@Builder",
        "@NoArgsConstructor",
        "@AllArgsConstructor",
        "private String id;",
        "private String courseId;",
        "private String studentId;",
        "private int score;",
        "private String status;"
], "ExamResultDTO")

def applicationJavaText = javaTextUnder("student-management-evaluation-application/src/main/java")
assertNoMatch(applicationJavaText, /\bView\b/, "Application layer must not contain View contracts")
assert !applicationJavaText.contains("facade.dto")
assert !applicationJavaText.contains("common.response")

def facadeJavaText = javaTextUnder("student-management-evaluation-facade/src/main/java/it/pkg/facade")
assert !facadeJavaText.contains("import it.pkg.domain.")
assert !facadeJavaText.contains("@AutoMapper")
assert !facadeJavaText.contains("ObjectFactory")
assert !facadeJavaText.contains("@Component")

def allGeneratedPathText = generatedFiles.join("\n")
assert !allGeneratedPathText.contains(".proto")
assert !allGeneratedPathText.contains("/grpc/")
assert !allGeneratedPathText.contains("/grpcjava/")
