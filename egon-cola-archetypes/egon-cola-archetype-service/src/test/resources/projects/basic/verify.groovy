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

assertFile("pom.xml")
assertFile("mvnw")
assertFile("mvnw.cmd")
assertFile(".mvn/wrapper/maven-wrapper.properties")
assertFile(".gitignore")
assertFile(".gitattributes")
assertFile("README.md")

["common", "facade", "domain", "application", "infrastructure", "adapter", "starter"].each {
    assertDir("student-management-evaluation-${it}")
}

["utils", "enums", "exceptions", "dto"].each {
    assertDir("student-management-evaluation-facade/src/main/java/it/pkg/facade/${it}")
}

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

def pom = assertFile("pom.xml").text
assert pom.contains("<artifactId>spring-boot-starter-parent</artifactId>")
assert pom.contains("<version>3.5.16</version>")
assert pom.contains("<java.version>21</java.version>")
assert pom.contains("<lombok.version>1.18.38</lombok.version>")
assert pom.contains("<mapstruct-plus.version>1.5.1</mapstruct-plus.version>")
assert pom.contains("<dubbo.version>3.3.6</dubbo.version>")
assert pom.contains("<spring-cloud.version>2025.0.3</spring-cloud.version>")
assert pom.contains("<spring-cloud-alibaba.version>2025.0.0.0</spring-cloud-alibaba.version>")
assert pom.contains("<artifactId>spring-cloud-dependencies</artifactId>")
assert pom.contains("<artifactId>spring-cloud-alibaba-dependencies</artifactId>")
assert pom.contains("<artifactId>dubbo-bom</artifactId>")
assert pom.contains("<artifactId>mapstruct-plus-spring-boot-starter</artifactId>")
assert pom.contains("<artifactId>mapstruct-plus-processor</artifactId>")
assert pom.contains("<module>student-management-evaluation-common</module>")
assert pom.contains("<module>student-management-evaluation-facade</module>")
assert pom.contains("<module>student-management-evaluation-domain</module>")
assert pom.contains("<module>student-management-evaluation-application</module>")
assert pom.contains("<module>student-management-evaluation-infrastructure</module>")
assert pom.contains("<module>student-management-evaluation-adapter</module>")
assert pom.contains("<module>student-management-evaluation-starter</module>")
def webStarter = "spring-boot-starter-" + "web"
assert !pom.contains(webStarter)
assert !pom.contains(webStarter + "flux")

assertFile("lombok.config").text.contains("lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier")

assertRuntimeConfigFiles("student-management-evaluation-starter/src/main/resources")

def adapterPomText = assertFile("student-management-evaluation-adapter/pom.xml").text
assert adapterPomText.contains("<artifactId>dubbo-spring-boot-starter</artifactId>")
assert adapterPomText.contains("<artifactId>mapstruct-plus-spring-boot-starter</artifactId>")
assert !adapterPomText.contains(webStarter)

def applicationPomText = assertFile("student-management-evaluation-application/pom.xml").text
assert applicationPomText.contains("<artifactId>lombok</artifactId>")

def infrastructurePomText = assertFile("student-management-evaluation-infrastructure/pom.xml").text
assert infrastructurePomText.contains("<artifactId>mapstruct-plus-spring-boot-starter</artifactId>")

def starterPomText = assertFile("student-management-evaluation-starter/pom.xml").text
assert starterPomText.contains("<artifactId>spring-cloud-starter-bootstrap</artifactId>")
assert starterPomText.contains("<artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>")
assert starterPomText.contains("<artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>")
assert starterPomText.contains("<artifactId>micrometer-registry-prometheus</artifactId>")
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

def serviceApplicationYaml = assertFile("student-management-evaluation-starter/src/main/resources/application.yml").text
assert serviceApplicationYaml.contains("dubbo:")
assert serviceApplicationYaml.contains('name: ${spring.application.name}')
assert serviceApplicationYaml.contains('${DUBBO_REGISTRY_ADDRESS:N/A}')
assert serviceApplicationYaml.contains("name: tri")
assert serviceApplicationYaml.contains('${DUBBO_PORT:50051}')
assert serviceApplicationYaml.contains("timeout: 3000")
assert serviceApplicationYaml.contains("retries: 0")

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

assertDependency(facadeDependencies, "jakarta.validation-api")
assertNoDependency(facadeDependencies, "spring-boot-starter-validation")
assertDependency(domainDependencies, "spring-boot-starter-validation")
assertDependency(applicationDependencies, "spring-boot-starter-validation")
assertDependency(infrastructureDependencies, "spring-boot-starter-validation")
assertDependency(adapterDependencies, "spring-boot-starter-validation")
assertNoDependency(adapterDependencies, webStarter)

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
        "Course create(String name, int credit);",
        "Course getById(String courseId);"
], "CourseManage")
assert !courseManageText.contains("CourseView")

def examManageText = assertFile("student-management-evaluation-application/src/main/java/it/pkg/application/manage/examing/ExamManage.java").text
assertContainsAll(examManageText, [
        "import it.pkg.domain.entities.examing.ExamResult;",
        "ExamResult record(String courseId, String studentId, int score);",
        "ExamResult getById(String examResultId);"
], "ExamManage")
assert !examManageText.contains("ExamResultView")

def courseManageImplText = assertFile("student-management-evaluation-application/src/main/java/it/pkg/application/manage/course/impl/CourseManageImpl.java").text
assertContainsAll(courseManageImplText, [
        '@Service("courseManage")',
        "@RequiredArgsConstructor",
        '@Qualifier("courseRepositoryImpl")',
        '@Qualifier("courseDomainService")'
], "CourseManageImpl")

def examManageImplText = assertFile("student-management-evaluation-application/src/main/java/it/pkg/application/manage/examing/impl/ExamManageImpl.java").text
assertContainsAll(examManageImplText, [
        '@Service("examManage")',
        "@RequiredArgsConstructor",
        '@Qualifier("examResultRepositoryImpl")',
        '@Qualifier("examDomainService")'
], "ExamManageImpl")

def domainServiceConfigurationText = assertFile("student-management-evaluation-application/src/main/java/it/pkg/application/config/DomainServiceConfiguration.java").text
assertContainsAll(domainServiceConfigurationText, [
        '@Bean("courseDomainService")',
        '@Bean("examDomainService")',
        '@Qualifier("courseRepositoryImpl") CourseRepository courseRepository',
        "new CourseDomainService(courseRepository)",
        "new ExamDomainService(courseRepository)"
], "DomainServiceConfiguration")

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
assertFile("student-management-evaluation-domain/src/main/java/it/pkg/domain/common/Page.java")
assertFile("student-management-evaluation-facade/src/main/java/it/pkg/facade/dto/PageResponse.java")

assertFile("student-management-evaluation-adapter/src/main/java/it/pkg/adapter/convertor/CourseAdapterMapper.java").text.contains("BaseMapper<Course, CourseDTO>")
assertFile("student-management-evaluation-adapter/src/main/java/it/pkg/adapter/convertor/ExamResultAdapterMapper.java").text.contains("BaseMapper<ExamResult, ExamResultDTO>")
assertFile("student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/repo/course/converter/CoursePoMapper.java").text.contains("BaseMapper<Course, CoursePo>")
assertFile("student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/repo/examing/converter/ExamResultPoMapper.java").text.contains("BaseMapper<ExamResult, ExamResultPo>")

def courseManageTextAfterPage = assertFile("student-management-evaluation-application/src/main/java/it/pkg/application/manage/course/CourseManage.java").text
assert courseManageTextAfterPage.contains("Page<Course> getPage(int currentPage, int pageSize)")
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
