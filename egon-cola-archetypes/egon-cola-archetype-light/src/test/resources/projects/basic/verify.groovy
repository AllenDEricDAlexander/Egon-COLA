def generatedProjectDir = new File(basedir, "project/basic")

def assertFile = { path ->
    def file = new File(generatedProjectDir, path)
    assert file.isFile(): "Expected file ${path}"
    file
}

assertFile("pom.xml")
def mvnw = assertFile("mvnw")
assert mvnw.canExecute(): "Expected mvnw to be executable"
assertFile("mvnw.cmd")
assertFile(".mvn/wrapper/maven-wrapper.properties")
assertFile(".gitignore")
assertFile(".gitattributes")
assertFile("README.md")

def pom = assertFile("pom.xml").text
assert pom.contains("<artifactId>spring-boot-starter-parent</artifactId>")
assert pom.contains("<version>3.5.16</version>")
assert pom.contains("<java.version>21</java.version>")
assert !pom.contains("spring-boot-dependencies")
assert !pom.contains("spring-ai")
assert !pom.contains("drools")
assert !pom.contains("mcp")

def wrapper = assertFile(".mvn/wrapper/maven-wrapper.properties").text
assert wrapper.contains("apache-maven/3.9.14/apache-maven-3.9.14-bin.zip")
