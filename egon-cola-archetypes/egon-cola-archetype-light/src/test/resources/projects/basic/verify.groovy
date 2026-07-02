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

assertFile("src/main/java/it/pkg/start/StudentManagementApplication.java")
assertFile("src/main/java/it/pkg/adapter/controller/student/StudentController.java")
assertFile("src/main/java/it/pkg/adapter/controller/teaching/CourseController.java")
assertFile("src/main/java/it/pkg/facade/api/StudentManagementFacade.java")
assertFile("src/main/java/it/pkg/application/manage/student/StudentManage.java")
assertFile("src/main/java/it/pkg/application/manage/teaching/CourseManage.java")
assertFile("src/main/java/it/pkg/domain/student/model/Student.java")
assertFile("src/main/java/it/pkg/domain/teaching/model/Course.java")
assertFile("src/main/java/it/pkg/infrastructure/repo/student/impl/StudentRepositoryImpl.java")
assertFile("src/main/java/it/pkg/infrastructure/repo/teaching/impl/CourseRepositoryImpl.java")
assertFile("src/main/resources/application.yml")
assertFile("src/main/resources/db/migration/V1__init_student_management.sql")

assert !new File(generatedProjectDir, "src/main/java/it/pkg/adapter/ChargeController.java").exists()
assert !new File(generatedProjectDir, "src/main/java/it/pkg/domain/charge").exists()
assert !new File(generatedProjectDir, "src/test/charge.http").exists()

def migrationDir = new File(generatedProjectDir, "src/main/resources/db/migration")
assert migrationDir.listFiles({ dir, name -> name.endsWith(".sql") } as FilenameFilter).size() == 1

assertFile("src/test/java/it/pkg/ArchitectureDependencyTest.java")
assertFile("src/test/java/it/pkg/application/StudentManagementFlowTest.java")

def readme = assertFile("README.md").text
assert readme.contains("Student Management")
assert readme.contains("single Maven module")
assert readme.contains("start / adapter / facade / application / infrastructure / common / domain")
assert !readme.contains("计费")
assert !readme.contains("Charge")

def allFiles = []
new File(generatedProjectDir, "src").eachFileRecurse { file ->
    if (file.isFile()) {
        allFiles << file
    }
}
assert allFiles.every { !it.name.contains("Charge") }
assert !new File(generatedProjectDir, "img.png").exists()
assert !new File(generatedProjectDir, "img_1.png").exists()
true
