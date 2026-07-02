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

def assertMissing = { path ->
    assert !new File(projectDir, path).exists(): "Unexpected stale path ${path}"
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

assert !new File(projectDir, "student-management-organization-client").exists()
assert !new File(projectDir, "student-management-organization-app").exists()
assert !new File(projectDir, "start").exists()
assert !new File(projectDir, "student-management-evaluation").exists()

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
assert rootPomText.contains("<module>student-management-organization-common</module>")
assert rootPomText.contains("<module>student-management-organization-starter</module>")
assert !rootPomText.contains("spring-ai")
assert !rootPomText.contains("drools")
assert !rootPomText.contains("mcp")

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

def commonPom = modulePom("common")
def facadePom = modulePom("facade")
def domainPom = modulePom("domain")
def applicationPom = modulePom("application")
def infrastructurePom = modulePom("infrastructure")
def adapterPom = modulePom("adapter")
def starterPom = modulePom("starter")

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

assertDependency(facadeDependencies, "spring-boot-starter-validation")

assertDependency(domainDependencies, "student-management-organization-common")

assertDependency(applicationDependencies, "student-management-organization-domain")
assertDependency(applicationDependencies, "student-management-organization-common")
assertDependency(applicationDependencies, "spring-context")
assertDependency(applicationDependencies, "spring-tx")
assertNoDependency(applicationDependencies, "student-management-organization-infrastructure")

assertDependency(infrastructureDependencies, "student-management-organization-domain")
assertDependency(infrastructureDependencies, "student-management-organization-application")
assertDependency(infrastructureDependencies, "student-management-organization-common")
assertDependency(infrastructureDependencies, "spring-boot-starter-data-jpa")
assertDependency(infrastructureDependencies, "flyway-core")
assertScopedDependency(infrastructureDependencies, "h2", "runtime")
assertScopedDependency(infrastructureDependencies, "postgresql", "runtime")

assertDependency(adapterDependencies, "student-management-organization-application")
assertDependency(adapterDependencies, "student-management-organization-facade")
assertDependency(adapterDependencies, "student-management-organization-common")
assertDependency(adapterDependencies, "spring-boot-starter-web")
assertDependency(adapterDependencies, "spring-boot-starter-validation")
assertNoDependency(adapterDependencies, "student-management-organization-infrastructure")

assertDependency(starterDependencies, "student-management-organization-adapter")
assertDependency(starterDependencies, "student-management-organization-infrastructure")
assertDependency(starterDependencies, "spring-boot-starter")
assertDependency(starterDependencies, "spring-boot-starter-actuator")
assertScopedDependency(starterDependencies, "spring-boot-starter-test", "test")
assertScopedDependency(starterDependencies, "archunit-junit5", "test")

assertFile("student-management-organization-common/src/main/java/it/pkg/common/response/Response.java")
assertFile("student-management-organization-facade/src/main/java/it/pkg/facade/user/UserFacade.java")
assertFile("student-management-organization-facade/src/main/java/it/pkg/facade/teaching/SchoolClassFacade.java")
assertFile("student-management-organization-domain/src/main/java/it/pkg/domain/entities/user/User.java")
assertFile("student-management-organization-domain/src/main/java/it/pkg/domain/entities/teaching/SchoolClass.java")
assertFile("student-management-organization-application/src/main/java/it/pkg/application/manage/user/UserManage.java")
assertFile("student-management-organization-application/src/main/java/it/pkg/application/manage/teaching/SchoolClassManage.java")

null
