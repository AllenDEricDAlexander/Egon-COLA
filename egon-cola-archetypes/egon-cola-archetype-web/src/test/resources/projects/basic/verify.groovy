def assertFile = { path ->
    def file = new File(basedir, path)
    assert file.isFile(): "Expected file ${path}"
    file
}

def assertDir = { path ->
    def file = new File(basedir, path)
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
    assertDir("student-management-organization-${it}")
}

assert !new File(basedir, "student-management-organization-client").exists()
assert !new File(basedir, "student-management-organization-app").exists()
assert !new File(basedir, "start").exists()
assert !new File(basedir, "student-management-evaluation").exists()

def pom = assertFile("pom.xml").text
assert pom.contains("<artifactId>spring-boot-starter-parent</artifactId>")
assert pom.contains("<version>3.5.16</version>")
assert pom.contains("<java.version>21</java.version>")
assert pom.contains("<module>student-management-organization-common</module>")
assert pom.contains("<module>student-management-organization-starter</module>")
assert !pom.contains("spring-ai")
assert !pom.contains("drools")
assert !pom.contains("mcp")

def wrapper = assertFile(".mvn/wrapper/maven-wrapper.properties").text
assert wrapper.contains("apache-maven/3.9.14/apache-maven-3.9.14-bin.zip")
