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
