def projectDir = new File(basedir, "pom.xml").isFile() ? basedir : context.projectDir

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

def assertMissing = { path ->
    assert !new File(projectDir, path).exists(): "Unexpected stale path ${path}"
}

def relativePath = { file ->
    projectDir.toPath().relativize(file.toPath()).toString().replace(File.separator, "/")
}

def isGeneratedOrVcsPath = { path ->
    path.tokenize("/").any { it == "target" || it == ".git" }
}

def sourceConfigDocNames = ["README.md", "pom.xml", ".gitignore", ".gitattributes"] as Set
def sourceConfigDocExtensions = [".java", ".xml", ".yml", ".yaml", ".properties", ".md", ".sql"]
def isSourceConfigDocFile = { file ->
    def path = relativePath(file)
    if (isGeneratedOrVcsPath(path)) {
        return false
    }
    sourceConfigDocNames.contains(file.name) || sourceConfigDocExtensions.any { file.name.endsWith(it) }
}

def collectSourceConfigDocFiles
collectSourceConfigDocFiles = { dir, files ->
    dir.listFiles()?.each { file ->
        def path = relativePath(file)
        if (file.isDirectory()) {
            if (!isGeneratedOrVcsPath(path)) {
                collectSourceConfigDocFiles(file, files)
            }
        } else if (isSourceConfigDocFile(file)) {
            files << file
        }
    }
}

def assertNoStaleText = { files, token ->
    def matches = []
    files.each { file ->
        if (matches.size() < 5 && file.getText("UTF-8").contains(token)) {
            matches << relativePath(file)
        }
    }
    assert matches.isEmpty(): "Unexpected stale token '${token}' in first matching relative paths: ${matches.join(', ')}"
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
assert assertFile("README.md").text.contains("## Modules")
assert assertFile("README.md").text.contains("## Domains")
assert assertFile("README.md").text.contains("## Dependency Direction")
assert assertFile("README.md").text.contains("## Clean Architecture Boundary Rules")
assert assertFile("README.md").text.contains("## Commands")
assert assertFile("README.md").text.contains("## Runtime Baseline")
assert assertFile("README.md").text.contains("docker build -t student-management-organization:local .")
def dockerfileText = assertFile("Dockerfile").text
assert dockerfileText.contains("FROM eclipse-temurin:21-jre-jammy AS extractor")
assert dockerfileText.contains("FROM eclipse-temurin:21-jre-jammy AS runtime")
assert !dockerfileText.contains(" AS builder")
assert !dockerfileText.contains("dependency:go-offline")
assert !dockerfileText.contains("./mvnw")
assert dockerfileText.contains("ARG STARTER_MODULE=student-management-organization-starter")
assert dockerfileText.contains('ARG JAR_FILE=${STARTER_MODULE}/target/*.jar')
assert dockerfileText.contains('COPY ${JAR_FILE} app.jar')
assert dockerfileText.contains("java -Djarmode=tools -jar app.jar extract --layers --destination extracted")
assert dockerfileText.contains("USER app")
assert dockerfileText.contains("EXPOSE 8080 50051")
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
    assertDir("student-management-organization-${it}")
}

[
    "student-management-organization-client",
    "student-management-organization-app",
    "start",
    "student-management-evaluation"
].each {
    assertMissing(it)
}

[
    "student-management-organization-adapter/src/main/java/mobile",
    "student-management-organization-adapter/src/main/java/wap",
    "student-management-organization-adapter/src/main/java/web",
    "student-management-organization-domain/src/test/java/domain",
    "student-management-organization-infrastructure/src/main/java/config",
    "student-management-organization-infrastructure/src/main/java/customer",
    "student-management-organization-infrastructure/src/main/java/order",
    "student-management-organization-infrastructure/src/main/resources/mybatis",
    "student-management-organization-infrastructure/src/test/java/repository",
    "student-management-organization-infrastructure/src/test/resources/sample.properties"
].each {
    assertMissing(it)
}

assertMissing("student-management-organization-adapter/src/main/java/package-info.java")
assertMissing("student-management-organization-adapter/src/main/java/it/pkg/package-info.java")
assertMissing("student-management-organization-domain/src/main/java/domain")
assertMissing("student-management-organization-domain/src/main/java/it/pkg/domain/package-info.java")
assertMissing("student-management-organization-infrastructure/src/main/java/package-info.java")
assertMissing("student-management-organization-infrastructure/src/main/java/it/pkg/package-info.java")

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
def commonPomText = assertFile("student-management-organization-common/pom.xml").text
assert commonPomText.contains("<artifactId>egon-cola-component-common-core</artifactId>")
def generatedPomTexts = []
projectDir.traverse(type: groovy.io.FileType.FILES) { file ->
    def path = relativePath(file)
    if (file.name == "pom.xml" && !isGeneratedOrVcsPath(path)) {
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
assert rootPomText.contains("<module>student-management-organization-common</module>")
assert rootPomText.contains("<module>student-management-organization-starter</module>")
assert !rootPomText.contains("spring-ai")
assert !rootPomText.contains("drools")
assert !rootPomText.contains("mcp")

assertFile("lombok.config").text.contains("lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier")

assertRuntimeConfigFiles("student-management-organization-starter/src/main/resources")

def webApplicationYaml = assertFile("student-management-organization-starter/src/main/resources/application.yml").text
assert webApplicationYaml.contains("threads:")
assert webApplicationYaml.contains("virtual:")
assert webApplicationYaml.contains('${SPRING_THREADS_VIRTUAL_ENABLED:true}')
assert webApplicationYaml.contains("timeout-per-shutdown-phase")
assert webApplicationYaml.contains("write-dates-as-timestamps: false")
assert webApplicationYaml.contains("prometheus")
assert webApplicationYaml.contains("tomcat:")
assert webApplicationYaml.contains('${TOMCAT_MAX_CONNECTIONS:8192}')
assert webApplicationYaml.contains("dubbo:")
assert webApplicationYaml.contains('name: ${spring.application.name}')
assert webApplicationYaml.contains('${DUBBO_REGISTRY_ADDRESS:N/A}')
assert webApplicationYaml.contains("name: tri")
assert webApplicationYaml.contains('${DUBBO_PORT:50051}')
assert webApplicationYaml.contains("timeout: 3000")
assert webApplicationYaml.contains("retries: 0")

def webApplicationDevYaml = assertFile("student-management-organization-starter/src/main/resources/application-dev.yml").text
def webApplicationProdYaml = assertFile("student-management-organization-starter/src/main/resources/application-prod.yml").text
def webBootstrapDevYaml = assertFile("student-management-organization-starter/src/main/resources/bootstrap-dev.yml").text
def webBootstrapProdYaml = assertFile("student-management-organization-starter/src/main/resources/bootstrap-prod.yml").text
def webApplicationLocalYaml = assertFile("student-management-organization-starter/src/main/resources/application-local.yml").text
def webApplicationTestYaml = assertFile("student-management-organization-starter/src/main/resources/application-test.yml").text
def webBootstrapLocalYaml = assertFile("student-management-organization-starter/src/main/resources/bootstrap-local.yml").text
def webBootstrapTestYaml = assertFile("student-management-organization-starter/src/main/resources/bootstrap-test.yml").text
assert webApplicationDevYaml.contains('password: ${DB_PASSWORD:ENC(')
assert webApplicationProdYaml.contains('password: ${DB_PASSWORD:ENC(')
assert webBootstrapDevYaml.contains('password: ${NACOS_PASSWORD:ENC(')
assert webBootstrapProdYaml.contains('password: ${NACOS_PASSWORD:ENC(')
assert webApplicationLocalYaml.contains('password: ${DB_PASSWORD:}')
assert webApplicationTestYaml.contains('password: ${DB_PASSWORD:}')
assert !webBootstrapLocalYaml.contains('ENC(')
assert !webBootstrapTestYaml.contains('ENC(')

def wrapper = assertFile(".mvn/wrapper/maven-wrapper.properties").text
assert wrapper.contains("apache-maven/3.9.14/apache-maven-3.9.14-bin.zip")

def modulePom = { module ->
    new groovy.xml.XmlSlurper(false, false).parse(assertFile("student-management-organization-${module}/pom.xml"))
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

def assertScopedDependency = { deps, artifactId, scope ->
    assert deps.any { it.artifactId == artifactId && it.scope == scope }: "Expected dependency ${artifactId} with scope ${scope}"
}

def assertNoDependency = { deps, artifactId ->
    assert !deps.any { it.artifactId == artifactId }: "Unexpected dependency ${artifactId}"
}

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

def moduleArtifactIds = [
    "student-management-organization-common",
    "student-management-organization-facade",
    "student-management-organization-domain",
    "student-management-organization-application",
    "student-management-organization-infrastructure",
    "student-management-organization-adapter",
    "student-management-organization-starter"
] as Set

def moduleDependencies = { deps ->
    deps.findAll { moduleArtifactIds.contains(it.artifactId) }
            .collect { it.artifactId } as Set
}

def assertModuleDependencies = { deps, expected ->
    def actual = moduleDependencies(deps)
    assert actual == (expected as Set): "Expected module dependencies ${expected}, but got ${actual}"
}

def commonPom = modulePom("common")
def facadePom = modulePom("facade")
def domainPom = modulePom("domain")
def applicationPom = modulePom("application")
def infrastructurePom = modulePom("infrastructure")
def adapterPom = modulePom("adapter")
def starterPom = modulePom("starter")
def starterPomText = assertFile("student-management-organization-starter/pom.xml").text
assert starterPomText.contains("<artifactId>spring-boot-maven-plugin</artifactId>")
assert starterPomText.contains("<layers>")
assert starterPomText.contains("<enabled>true</enabled>")
assertSpringBootLayeredJarPlugin(starterPom)

assert commonPom.artifactId.text() == "student-management-organization-common"
assert facadePom.artifactId.text() == "student-management-organization-facade"
assert domainPom.artifactId.text() == "student-management-organization-domain"
assert applicationPom.artifactId.text() == "student-management-organization-application"
assert infrastructurePom.artifactId.text() == "student-management-organization-infrastructure"
assert adapterPom.artifactId.text() == "student-management-organization-adapter"
assert starterPom.artifactId.text() == "student-management-organization-starter"

def facadeDependencies = dependencies(facadePom)
def domainDependencies = dependencies(domainPom)
def applicationDependencies = dependencies(applicationPom)
def infrastructureDependencies = dependencies(infrastructurePom)
def adapterDependencies = dependencies(adapterPom)
def starterDependencies = dependencies(starterPom)

assertModuleDependencies(dependencies(commonPom), [])
assertModuleDependencies(facadeDependencies, [])
assertModuleDependencies(domainDependencies, ["student-management-organization-common"])
assertModuleDependencies(applicationDependencies, ["student-management-organization-domain"])
assertModuleDependencies(infrastructureDependencies, ["student-management-organization-domain"])
assertModuleDependencies(adapterDependencies, [
    "student-management-organization-application",
    "student-management-organization-facade"
])
assertModuleDependencies(starterDependencies, [
    "student-management-organization-adapter",
    "student-management-organization-infrastructure"
])

assertDependency(facadeDependencies, "jakarta.validation-api")
assertNoDependency(facadeDependencies, "spring-boot-starter-validation")
assertScopedDependency(facadeDependencies, "lombok", "provided")

assertDependency(domainDependencies, "student-management-organization-common")
assertDependency(domainDependencies, "spring-boot-starter-validation")

assertDependency(applicationDependencies, "student-management-organization-domain")
assertDependency(applicationDependencies, "spring-context")
assertDependency(applicationDependencies, "spring-tx")
assertDependency(applicationDependencies, "spring-boot-starter-validation")
assertScopedDependency(applicationDependencies, "lombok", "provided")
assertNoDependency(applicationDependencies, "student-management-organization-common")
assertNoDependency(applicationDependencies, "student-management-organization-infrastructure")

assertDependency(infrastructureDependencies, "student-management-organization-domain")
assertNoDependency(infrastructureDependencies, "student-management-organization-common")
assertDependency(infrastructureDependencies, "spring-boot-starter-validation")
assertDependency(infrastructureDependencies, "mapstruct-plus-spring-boot-starter")
assertDependency(infrastructureDependencies, "spring-boot-starter-data-jpa")
assertDependency(infrastructureDependencies, "flyway-core")
assertScopedDependency(infrastructureDependencies, "h2", "runtime")
assertScopedDependency(infrastructureDependencies, "postgresql", "runtime")
assertScopedDependency(infrastructureDependencies, "lombok", "provided")

assertDependency(adapterDependencies, "student-management-organization-application")
assertDependency(adapterDependencies, "student-management-organization-facade")
assertNoDependency(adapterDependencies, "student-management-organization-common")
assertDependency(adapterDependencies, "spring-boot-starter-web")
assertDependency(adapterDependencies, "spring-boot-starter-validation")
assertDependency(adapterDependencies, "dubbo-spring-boot-starter")
assertDependency(adapterDependencies, "mapstruct-plus-spring-boot-starter")
assertScopedDependency(adapterDependencies, "lombok", "provided")
assertNoDependency(adapterDependencies, "student-management-organization-infrastructure")

assertDependency(starterDependencies, "student-management-organization-adapter")
assertDependency(starterDependencies, "student-management-organization-infrastructure")
assertDependency(starterDependencies, "spring-boot-starter")
assertDependency(starterDependencies, "spring-boot-starter-actuator")
assertScopedDependency(starterDependencies, "lombok", "provided")
assertScopedDependency(starterDependencies, "spring-boot-starter-test", "test")
assertScopedDependency(starterDependencies, "archunit-junit5", "test")
assert starterPomText.contains("<artifactId>spring-cloud-starter-bootstrap</artifactId>")
assert starterPomText.contains("<artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>")
assert starterPomText.contains("<artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>")
assert starterPomText.contains("<artifactId>micrometer-registry-prometheus</artifactId>")

assertFile("student-management-organization-common/src/main/java/it/pkg/common/response/Response.java")
assertFile("student-management-organization-facade/src/main/java/it/pkg/facade/user/UserFacade.java")
assertFile("student-management-organization-facade/src/main/java/it/pkg/facade/teaching/SchoolClassFacade.java")
assertFile("student-management-organization-domain/src/main/java/it/pkg/domain/entities/user/User.java")
assertFile("student-management-organization-domain/src/main/java/it/pkg/domain/entities/teaching/SchoolClass.java")
assertFile("student-management-organization-domain/src/main/java/it/pkg/domain/client/user/UserClient.java")
assertFile("student-management-organization-domain/src/main/java/it/pkg/domain/client/teaching/SchoolClassClient.java")
assertFile("student-management-organization-application/src/main/java/it/pkg/application/manage/user/UserManage.java")
assertFile("student-management-organization-application/src/main/java/it/pkg/application/manage/teaching/SchoolClassManage.java")
assertFile("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/client/impl/user/UserClientImpl.java")
assertFile("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/client/impl/teaching/SchoolClassClientImpl.java")
assertFile("student-management-organization-infrastructure/src/main/resources/db/migration/V1__init_student_management_organization.sql")
assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/controller/user/UserController.java")
assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/controller/teaching/SchoolClassController.java")
assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/validation/ValidatorUtils.java")
assertFile("student-management-organization-starter/src/main/java/it/pkg/starter/OrganizationApplication.java")
def asyncConfigurationText = assertFile("student-management-organization-starter/src/main/java/it/pkg/starter/config/async/AsyncConfiguration.java").text
assert asyncConfigurationText.contains("implements AsyncConfigurer")
assert asyncConfigurationText.contains("getAsyncUncaughtExceptionHandler")
assert !asyncConfigurationText.contains("ThreadPoolTaskExecutorBuilder")
assert !asyncConfigurationText.contains("applicationTaskExecutor(")
assertFile("student-management-organization-starter/src/main/java/it/pkg/starter/config/encryption/ConfigDecryptor.java")
assertFile("student-management-organization-starter/src/main/java/it/pkg/starter/config/encryption/ConfigDecryptException.java")
assertFile("student-management-organization-starter/src/main/java/it/pkg/starter/config/encryption/ConfigDecryptKeyProvider.java")
assertFile("student-management-organization-starter/src/main/java/it/pkg/starter/config/encryption/AesGcmConfigDecryptor.java")
assertFile("student-management-organization-starter/src/main/java/it/pkg/starter/config/encryption/ConfigDecryptEnvironmentPostProcessor.java")
assertFile("student-management-organization-starter/src/main/java/it/pkg/starter/config/encryption/ConfigCipherCli.java")
def springFactories = assertFile("student-management-organization-starter/src/main/resources/META-INF/spring.factories").text
assert springFactories.contains("org.springframework.boot.env.EnvironmentPostProcessor")
assert springFactories.contains("it.pkg.starter.config.encryption.ConfigDecryptEnvironmentPostProcessor")
assertFile("student-management-organization-starter/src/test/java/it/pkg/starter/config/encryption/AesGcmConfigDecryptorTest.java")
assertFile("student-management-organization-starter/src/test/java/it/pkg/starter/config/encryption/ConfigDecryptEnvironmentPostProcessorTest.java")
assertFile("student-management-organization-starter/src/test/java/it/pkg/starter/OrganizationFlowTest.java").text.contains('properties = "dubbo.protocol.port=-1"')
assertFile("student-management-organization-starter/src/test/java/it/pkg/starter/ArchitectureDependencyTest.java")

def organizationApplicationText = assertFile("student-management-organization-starter/src/main/java/it/pkg/starter/OrganizationApplication.java").text
assert organizationApplicationText.contains("@EnableDubbo")
assert organizationApplicationText.contains('scanBasePackages = "it.pkg.adapter.facade"')

assertFile("student-management-organization-application/src/main/java/it/pkg/application/config/DomainServiceConfiguration.java")
assertMissing("student-management-organization-application/src/main/java/it/pkg/application/manage/user/UserView.java")
assertMissing("student-management-organization-application/src/main/java/it/pkg/application/manage/teaching/SchoolClassView.java")

def userManageText = assertFile("student-management-organization-application/src/main/java/it/pkg/application/manage/user/UserManage.java").text
assert userManageText.contains("User create(")
assert userManageText.contains("String name")
assert userManageText.contains("String email")
assert userManageText.contains("User getById(")
assert userManageText.contains("String userId")
assert userManageText.contains("@NotBlank")
assert userManageText.contains("@Email")
assert userManageText.contains("@Positive")
assert !userManageText.contains("UserView")

def schoolClassManageText = assertFile("student-management-organization-application/src/main/java/it/pkg/application/manage/teaching/SchoolClassManage.java").text
assert schoolClassManageText.contains("SchoolClass create(")
assert schoolClassManageText.contains("String name")
assert schoolClassManageText.contains("String gradeName")
assert schoolClassManageText.contains("@NotBlank")
assert !schoolClassManageText.contains("SchoolClassView")

def userManageImplText = assertFile("student-management-organization-application/src/main/java/it/pkg/application/manage/user/impl/UserManageImpl.java").text
assert userManageImplText.contains("@Validated")
assert userManageImplText.contains('@Qualifier("userClientImpl")')
assert userManageImplText.contains("userClient.existsByEmail(email)")
assert !userManageImplText.contains("UserRepository userRepository")

def schoolClassManageImplText = assertFile("student-management-organization-application/src/main/java/it/pkg/application/manage/teaching/impl/SchoolClassManageImpl.java").text
assert schoolClassManageImplText.contains("@Validated")
assert schoolClassManageImplText.contains('@Qualifier("userClientImpl")')
assert schoolClassManageImplText.contains('@Qualifier("schoolClassClientImpl")')
assert schoolClassManageImplText.contains("userClient.findById(userId)")
assert schoolClassManageImplText.contains("schoolClassClient.findById(schoolClassId)")
assert !schoolClassManageImplText.contains("UserRepository userRepository")
assert !schoolClassManageImplText.contains("SchoolClassRepository schoolClassRepository")
assertNoJavaText("student-management-organization-application/src/main/java/it/pkg/application", "domain.repos")

def userFacadeText = assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/facade/user/UserFacadeImpl.java").text
assert userFacadeText.contains("@DubboService")
assert userFacadeText.contains("interfaceClass = UserFacade.class")
assert userFacadeText.contains('version = "1.0.0"')
assert userFacadeText.contains('group = "user"')
assert userFacadeText.contains("@Qualifier(\"userManage\")")
assert userFacadeText.contains("@Qualifier(\"userAdapterConverter\")")

def schoolClassFacadeText = assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/facade/teaching/SchoolClassFacadeImpl.java").text
assert schoolClassFacadeText.contains("@DubboService")
assert schoolClassFacadeText.contains("interfaceClass = SchoolClassFacade.class")
assert schoolClassFacadeText.contains('version = "1.0.0"')
assert schoolClassFacadeText.contains('group = "school-class"')
assert schoolClassFacadeText.contains("@Qualifier(\"schoolClassManage\")")
assert schoolClassFacadeText.contains("@Qualifier(\"schoolClassAdapterConverter\")")

assertNoGenericMapStructConverterInjection("student-management-organization-adapter/src/main/java/it/pkg/adapter/convertor")
assertNoGenericMapStructConverterInjection("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/repo")

assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/convertor/UserAdapterMapper.java")
assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/convertor/SchoolClassAdapterMapper.java")
assertFile("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/repo/user/converter/UserPoMapper.java")
assertFile("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/repo/user/converter/UserDomainMapper.java")
assertFile("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/repo/user/converter/UserDomainFactory.java")
assertFile("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/repo/teaching/converter/SchoolClassPoMapper.java")
assertFile("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/repo/teaching/converter/SchoolClassDomainMapper.java")
assertFile("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/repo/teaching/converter/SchoolClassDomainFactory.java")
def userClientImplText = assertFile("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/client/impl/user/UserClientImpl.java").text
assert userClientImplText.contains('@Component("userClientImpl")')
assert userClientImplText.contains("@Validated")
assert userClientImplText.contains("implements UserClient")
assert userClientImplText.contains('@Qualifier("userRepositoryImpl")')
assert !userClientImplText.contains("jakarta.validation.constraints")

def schoolClassClientImplText = assertFile("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/client/impl/teaching/SchoolClassClientImpl.java").text
assert schoolClassClientImplText.contains('@Component("schoolClassClientImpl")')
assert schoolClassClientImplText.contains("@Validated")
assert schoolClassClientImplText.contains("implements SchoolClassClient")
assert schoolClassClientImplText.contains('@Qualifier("schoolClassRepositoryImpl")')
assert !schoolClassClientImplText.contains("jakarta.validation.constraints")
def userClientText = assertFile("student-management-organization-domain/src/main/java/it/pkg/domain/client/user/UserClient.java").text
assert userClientText.contains("@NotNull")
assert userClientText.contains("@NotBlank")
assert userClientText.contains("@Email")
assert userClientText.contains("@Positive")
assert !userClientText.contains("default ")
def schoolClassClientText = assertFile("student-management-organization-domain/src/main/java/it/pkg/domain/client/teaching/SchoolClassClient.java").text
assert schoolClassClientText.contains("@NotNull")
assert schoolClassClientText.contains("@NotBlank")
assert !schoolClassClientText.contains("default ")
assertFile("student-management-organization-domain/src/main/java/it/pkg/domain/common/Page.java")
assertFile("student-management-organization-facade/src/main/java/it/pkg/facade/dto/PageResponse.java")

assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/convertor/UserAdapterMapper.java").text.contains("BaseMapper<User, UserDTO>")
assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/convertor/SchoolClassAdapterMapper.java").text.contains("BaseMapper<SchoolClass, SchoolClassDTO>")
assertFile("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/repo/user/converter/UserPoMapper.java").text.contains("BaseMapper<User, UserPo>")
assertFile("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/repo/teaching/converter/SchoolClassPoMapper.java").text.contains("BaseMapper<SchoolClass, SchoolClassPo>")

def userManageTextAfterPage = assertFile("student-management-organization-application/src/main/java/it/pkg/application/manage/user/UserManage.java").text
assert userManageTextAfterPage.contains("Page<User> getPage(")
assert userManageTextAfterPage.contains("int currentPage")
assert userManageTextAfterPage.contains("int pageSize")
assert userManageTextAfterPage.contains("import it.pkg.domain.common.Page;")

def userRepositoryText = assertFile("student-management-organization-domain/src/main/java/it/pkg/domain/repos/user/UserRepository.java").text
assert userRepositoryText.contains("Page<User> findPage(int currentPage, int pageSize)")
assert userRepositoryText.contains("import it.pkg.domain.common.Page;")

def userControllerText = assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/controller/user/UserController.java").text
assert userControllerText.contains("SingleResponse<PageResponse<UserDTO>> getPage")
assert userControllerText.contains("userAdapterConverter.toPageResponse(userManage.getPage(currentPage, pageSize))")

def userFacadeTextAfterPage = assertFile("student-management-organization-facade/src/main/java/it/pkg/facade/user/UserFacade.java").text
assert userFacadeTextAfterPage.contains("PageResponse<UserDTO> getUsers(int currentPage, int pageSize)")

def userFacadeImplTextAfterPage = assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/facade/user/UserFacadeImpl.java").text
assert userFacadeImplTextAfterPage.contains("PageResponse<UserDTO> getUsers")
assert userFacadeImplTextAfterPage.contains("userAdapterConverter.toPageResponse(userManage.getPage(currentPage, pageSize))")

def applicationJava = []
new File(projectDir, "student-management-organization-application/src/main/java").eachFileRecurse { file ->
    if (file.isFile() && file.name.endsWith(".java")) {
        applicationJava << file
    }
}
assert applicationJava.every { !it.text.contains("View") }
assert applicationJava.every { !it.text.contains("facade.dto") }
assert applicationJava.every { !it.text.contains("common.response") }

def facadeJava = []
new File(projectDir, "student-management-organization-facade/src/main/java").eachFileRecurse { file ->
    if (file.isFile() && file.name.endsWith(".java")) {
        facadeJava << file
    }
}
assert facadeJava.every { !it.text.contains("import it.pkg.domain.") }
assert facadeJava.every { !it.text.contains("@AutoMapper") }
assert facadeJava.every { !it.text.contains("@Component") }

def migrationDir = new File(projectDir, "student-management-organization-infrastructure/src/main/resources/db/migration")
assert migrationDir.listFiles({ dir, name -> name.endsWith(".sql") } as FilenameFilter).size() == 1

def readme = assertFile("README.md").text
assert readme.contains("Student Management Organization")
assert readme.contains("one independent Project")
assert readme.contains("user")
assert readme.contains("teaching")
assert readme.contains("bash ./mvnw test")
assert !readme.contains("\n./mvnw ")
assert !readme.contains("student-management-evaluation")

def scannedFiles = []
collectSourceConfigDocFiles(projectDir, scannedFiles)
[
    "__rootArtifactId__-client",
    'dir="__rootArtifactId__-app"',
    'name="${rootArtifactId}-app"',
    "student-management-organization-client",
    "<module>student-management-organization-app</module>",
    "<artifactId>student-management-organization-app</artifactId>",
    "<name>student-management-organization-app</name>",
    "<module>start</module>",
    "app1",
    "app2",
    "examing",
    "student-management-evaluation",
    "package it.pkg.customer",
    "package it.pkg.order",
    "import it.pkg.customer",
    "import it.pkg.order",
    "/customer/",
    "/order/"
].each { token ->
    assertNoStaleText(scannedFiles, token)
}

null
