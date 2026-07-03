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

def adapterPomText = assertFile("student-management-evaluation-adapter/pom.xml").text
assert adapterPomText.contains("<artifactId>dubbo-spring-boot-starter</artifactId>")
assert adapterPomText.contains("<artifactId>mapstruct-plus-spring-boot-starter</artifactId>")
assert !adapterPomText.contains(webStarter)

def applicationPomText = assertFile("student-management-evaluation-application/pom.xml").text
assert applicationPomText.contains("<artifactId>lombok</artifactId>")

def infrastructurePomText = assertFile("student-management-evaluation-infrastructure/pom.xml").text
assert infrastructurePomText.contains("<artifactId>mapstruct-plus-spring-boot-starter</artifactId>")

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
