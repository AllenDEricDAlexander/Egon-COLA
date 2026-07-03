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
assertFile("mvnw")
assertFile("mvnw.cmd")
assertFile(".mvn/wrapper/maven-wrapper.properties")
assertFile(".gitignore")
assertFile(".gitattributes")
assertFile("README.md")

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
assert webApplicationYaml.contains("dubbo:")
assert webApplicationYaml.contains('name: ${spring.application.name}')
assert webApplicationYaml.contains('${DUBBO_REGISTRY_ADDRESS:N/A}')
assert webApplicationYaml.contains("name: tri")
assert webApplicationYaml.contains('${DUBBO_PORT:50051}')
assert webApplicationYaml.contains("timeout: 3000")
assert webApplicationYaml.contains("retries: 0")

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
assertModuleDependencies(infrastructureDependencies, ["student-management-organization-application"])
assertModuleDependencies(adapterDependencies, [
    "student-management-organization-application",
    "student-management-organization-facade"
])
assertModuleDependencies(starterDependencies, [
    "student-management-organization-adapter",
    "student-management-organization-infrastructure"
])

assertDependency(facadeDependencies, "spring-boot-starter-validation")
assertScopedDependency(facadeDependencies, "lombok", "provided")

assertDependency(domainDependencies, "student-management-organization-common")

assertDependency(applicationDependencies, "student-management-organization-domain")
assertDependency(applicationDependencies, "spring-context")
assertDependency(applicationDependencies, "spring-tx")
assertScopedDependency(applicationDependencies, "lombok", "provided")
assertNoDependency(applicationDependencies, "student-management-organization-common")
assertNoDependency(applicationDependencies, "student-management-organization-infrastructure")

assertDependency(infrastructureDependencies, "student-management-organization-application")
assertNoDependency(infrastructureDependencies, "student-management-organization-domain")
assertNoDependency(infrastructureDependencies, "student-management-organization-common")
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
assertFile("student-management-organization-application/src/main/java/it/pkg/application/manage/user/UserManage.java")
assertFile("student-management-organization-application/src/main/java/it/pkg/application/manage/teaching/SchoolClassManage.java")
assertFile("student-management-organization-infrastructure/src/main/resources/db/migration/V1__init_student_management_organization.sql")
assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/controller/user/UserController.java")
assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/controller/teaching/SchoolClassController.java")
assertFile("student-management-organization-starter/src/main/java/it/pkg/starter/OrganizationApplication.java")
assertFile("student-management-organization-starter/src/test/java/it/pkg/starter/OrganizationFlowTest.java")
assertFile("student-management-organization-starter/src/test/java/it/pkg/starter/ArchitectureDependencyTest.java")

def organizationApplicationText = assertFile("student-management-organization-starter/src/main/java/it/pkg/starter/OrganizationApplication.java").text
assert organizationApplicationText.contains("@EnableDubbo")
assert organizationApplicationText.contains('scanBasePackages = "it.pkg.adapter.facade"')

assertFile("student-management-organization-application/src/main/java/it/pkg/application/config/DomainServiceConfiguration.java")
assertMissing("student-management-organization-application/src/main/java/it/pkg/application/manage/user/UserView.java")
assertMissing("student-management-organization-application/src/main/java/it/pkg/application/manage/teaching/SchoolClassView.java")

def userManageText = assertFile("student-management-organization-application/src/main/java/it/pkg/application/manage/user/UserManage.java").text
assert userManageText.contains("User create(String name, String email)")
assert userManageText.contains("User getById(String userId)")
assert !userManageText.contains("UserView")

def schoolClassManageText = assertFile("student-management-organization-application/src/main/java/it/pkg/application/manage/teaching/SchoolClassManage.java").text
assert schoolClassManageText.contains("SchoolClass create(String name, String gradeName)")
assert !schoolClassManageText.contains("SchoolClassView")

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
assertFile("student-management-organization-domain/src/main/java/it/pkg/domain/common/Page.java")
assertFile("student-management-organization-facade/src/main/java/it/pkg/facade/dto/PageResponse.java")

assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/convertor/UserAdapterMapper.java").text.contains("BaseMapper<User, UserDTO>")
assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/convertor/SchoolClassAdapterMapper.java").text.contains("BaseMapper<SchoolClass, SchoolClassDTO>")
assertFile("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/repo/user/converter/UserPoMapper.java").text.contains("BaseMapper<User, UserPo>")
assertFile("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/repo/teaching/converter/SchoolClassPoMapper.java").text.contains("BaseMapper<SchoolClass, SchoolClassPo>")

def userManageTextAfterPage = assertFile("student-management-organization-application/src/main/java/it/pkg/application/manage/user/UserManage.java").text
assert userManageTextAfterPage.contains("Page<User> getPage(int currentPage, int pageSize)")
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
