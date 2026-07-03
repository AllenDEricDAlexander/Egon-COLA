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
assert pom.contains("<lombok.version>1.18.38</lombok.version>")
assert pom.contains("<mapstruct-plus.version>1.5.1</mapstruct-plus.version>")
assert pom.contains("<dubbo.version>3.3.6</dubbo.version>")
assert pom.contains("<artifactId>mapstruct-plus-spring-boot-starter</artifactId>")
assert pom.contains("<artifactId>dubbo-spring-boot-starter</artifactId>")
assert pom.contains("<artifactId>mapstruct-plus-processor</artifactId>")
assert !pom.contains("spring-boot-dependencies")
assert !pom.contains("spring-ai")
assert !pom.contains("drools")
assert !pom.contains("mcp")

assertFile("lombok.config").text.contains("lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier")

def applicationYaml = assertFile("src/main/resources/application.yml").text
assert applicationYaml.contains("dubbo:")
assert applicationYaml.contains('name: ${spring.application.name}')
assert applicationYaml.contains('${DUBBO_REGISTRY_ADDRESS:N/A}')
assert applicationYaml.contains("name: tri")
assert applicationYaml.contains('${DUBBO_PORT:50051}')
assert applicationYaml.contains("timeout: 3000")
assert applicationYaml.contains("retries: 0")

def wrapper = assertFile(".mvn/wrapper/maven-wrapper.properties").text
assert wrapper.contains("apache-maven/3.9.14/apache-maven-3.9.14-bin.zip")

assertFile("src/main/java/it/pkg/start/StudentManagementApplication.java")
def starterText = assertFile("src/main/java/it/pkg/start/StudentManagementApplication.java").text
assert starterText.contains("@EnableDubbo")
assert starterText.contains('scanBasePackages = "it.pkg.adapter.facade"')
assertFile("src/main/java/it/pkg/adapter/controller/student/StudentController.java")
assertFile("src/main/java/it/pkg/adapter/controller/teaching/CourseController.java")
assertFile("src/main/java/it/pkg/facade/api/StudentManagementFacade.java")
assertFile("src/main/java/it/pkg/application/manage/student/StudentManage.java")
assertFile("src/main/java/it/pkg/application/manage/teaching/CourseManage.java")
assertFile("src/main/java/it/pkg/application/config/DomainServiceConfiguration.java")
assertFile("src/main/java/it/pkg/domain/student/model/Student.java")
assertFile("src/main/java/it/pkg/domain/teaching/model/Course.java")
assertFile("src/main/java/it/pkg/infrastructure/repo/student/impl/StudentRepositoryImpl.java")
assertFile("src/main/java/it/pkg/infrastructure/repo/teaching/impl/CourseRepositoryImpl.java")
assertFile("src/main/resources/application.yml")
assertFile("src/main/resources/db/migration/V1__init_student_management.sql")

assert !new File(generatedProjectDir, "src/main/java/it/pkg/application/manage/student/StudentView.java").exists()
assert !new File(generatedProjectDir, "src/main/java/it/pkg/application/manage/teaching/CourseView.java").exists()

def studentManageText = assertFile("src/main/java/it/pkg/application/manage/student/StudentManage.java").text
assert studentManageText.contains("Student register(String name, String email)")
assert studentManageText.contains("Student getById(String studentId)")
assert !studentManageText.contains("StudentView")

def courseManageText = assertFile("src/main/java/it/pkg/application/manage/teaching/CourseManage.java").text
assert courseManageText.contains("Course create(String name, String description)")
assert courseManageText.contains("Course getById(String courseId)")
assert !courseManageText.contains("CourseView")

def lightFacadeText = assertFile("src/main/java/it/pkg/adapter/facade/impl/StudentManagementFacadeImpl.java").text
assert lightFacadeText.contains("@DubboService")
assert lightFacadeText.contains("interfaceClass = StudentManagementFacade.class")
assert lightFacadeText.contains('version = "1.0.0"')
assert lightFacadeText.contains('group = "student-management"')
assert lightFacadeText.contains("@RequiredArgsConstructor")
assert lightFacadeText.contains("@Qualifier(\"studentManage\")")
assert lightFacadeText.contains("@Qualifier(\"studentAdapterConverter\")")

def lightConverterText = assertFile("src/main/java/it/pkg/adapter/convertor/StudentAdapterConverter.java").text
assert lightConverterText.contains("io.github.linpeilie.Converter")
assert lightConverterText.contains("@Component(\"studentAdapterConverter\")")
assert lightConverterText.contains("BaseMapper<Student, StudentDTO>")
assert !lightConverterText.contains("static StudentDTO")

def courseConverterText = assertFile("src/main/java/it/pkg/adapter/convertor/CourseAdapterConverter.java").text
assert courseConverterText.contains("io.github.linpeilie.Converter")
assert courseConverterText.contains("@Component(\"courseAdapterConverter\")")
assert courseConverterText.contains("BaseMapper<Course, CourseDTO>")

def facadeJavaFiles = []
new File(generatedProjectDir, "src/main/java/it/pkg/facade").eachFileRecurse { file ->
    if (file.isFile() && file.name.endsWith(".java")) {
        facadeJavaFiles << file
    }
}
assert !facadeJavaFiles.isEmpty()
assert facadeJavaFiles.every { !it.text.contains("import it.pkg.domain.") }

def studentDtoText = assertFile("src/main/java/it/pkg/facade/dto/StudentDTO.java").text
assert !studentDtoText.contains("@AutoMapper")
assert !studentDtoText.contains("org.springframework.stereotype.Component")
assert !studentDtoText.contains("org.mapstruct.ObjectFactory")

def courseDtoText = assertFile("src/main/java/it/pkg/facade/dto/CourseDTO.java").text
assert !courseDtoText.contains("@AutoMapper")
assert !courseDtoText.contains("org.springframework.stereotype.Component")
assert !courseDtoText.contains("org.mapstruct.ObjectFactory")

def allApplicationJava = []
new File(generatedProjectDir, "src/main/java/it/pkg/application").eachFileRecurse { file ->
    if (file.isFile() && file.name.endsWith(".java")) {
        allApplicationJava << file
    }
}
assert allApplicationJava.every { !it.text.contains("View") }
assert allApplicationJava.every { !it.text.contains("facade.dto") }
assert allApplicationJava.every { !it.text.contains("common.response") }

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
