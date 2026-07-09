def generatedProjectDir = new File(basedir, "project/basic")

def assertFile = { path ->
    def file = new File(generatedProjectDir, path)
    assert file.isFile(): "Expected file ${path}"
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
    def dir = new File(generatedProjectDir, path)
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
assert assertFile("README.md").text.contains("docker build -t basic:local .")
def dockerfileText = assertFile("Dockerfile").text
assert dockerfileText.contains("FROM eclipse-temurin:21-jre-jammy AS extractor")
assert dockerfileText.contains("FROM eclipse-temurin:21-jre-jammy AS runtime")
assert !dockerfileText.contains(" AS builder")
assert !dockerfileText.contains("dependency:go-offline")
assert !dockerfileText.contains("./mvnw")
assert dockerfileText.contains("ARG JAR_FILE=target/*.jar")
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

def pom = assertFile("pom.xml").text
def pomXml = new groovy.xml.XmlSlurper(false, false).parse(assertFile("pom.xml"))
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
assert pom.contains("<artifactId>spring-boot-starter-parent</artifactId>")
assert pom.contains("<version>3.5.16</version>")
assert pom.contains("<java.version>21</java.version>")
assert pom.contains("<lombok.version>1.18.38</lombok.version>")
assert pom.contains("<mapstruct-plus.version>1.5.1</mapstruct-plus.version>")
assert pom.contains("<dubbo.version>3.3.6</dubbo.version>")
assert pom.contains("<spring-cloud.version>2025.0.3</spring-cloud.version>")
assert pom.contains("<spring-cloud-alibaba.version>2025.0.0.0</spring-cloud-alibaba.version>")
assert pom.contains("<springdoc.version>2.8.17</springdoc.version>")
[
    "spring-boot-starter-graphql",
    "spring-boot-starter-amqp",
    "spring-boot-starter-data-redis",
    "spring-boot-starter-aop",
    "springdoc-openapi-starter-webmvc-ui",
    "flyway-database-postgresql",
    "spring-boot-starter-test",
    "spring-graphql-test"
].each { artifactId ->
    assert pom.contains("<artifactId>${artifactId}</artifactId>")
}
assert !pom.contains("mybatis-plus")
assert !pom.contains("mybatis-spring")
assert pom.contains("<artifactId>egon-cola-components-bom</artifactId>")
assert pom.contains("<egon-cola.version>5.2.1</egon-cola.version>")
assert pom.contains("<artifactId>egon-cola-component-common-core</artifactId>")
assert !pom.contains("<artifactId>egon-cola-component-dynamic-thread-pool-starter</artifactId>")
assert !pom.contains("<artifactId>egon-cola-component-dynamic-thread-pool-admin</artifactId>")
assert !pom.contains("<artifactId>egon-cola-component-dynamic-thread-pool-test</artifactId>")
assert pom.contains("<artifactId>spring-cloud-dependencies</artifactId>")
assert pom.contains("<artifactId>spring-cloud-alibaba-dependencies</artifactId>")
assert pom.contains("<artifactId>mapstruct-plus-spring-boot-starter</artifactId>")
assert pom.contains("<artifactId>dubbo-spring-boot-starter</artifactId>")
assert pom.contains("<artifactId>mapstruct-plus-processor</artifactId>")
assert pom.contains("<artifactId>spring-boot-dependencies</artifactId>")
assert !pom.contains("spring-ai")
assert !pom.contains("drools")
assert !pom.contains("mcp")
assert pom.contains("<artifactId>spring-boot-maven-plugin</artifactId>")
assert pom.contains("<layers>")
assert pom.contains("<enabled>true</enabled>")
assertSpringBootLayeredJarPlugin(pomXml)

def starterPomText = pom
assert starterPomText.contains("<artifactId>spring-cloud-starter-bootstrap</artifactId>")
assert starterPomText.contains("<artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>")
assert starterPomText.contains("<artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>")
assert starterPomText.contains("<artifactId>micrometer-registry-prometheus</artifactId>")

assertFile("lombok.config").text.contains("lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier")

assertRuntimeConfigFiles("src/main/resources")

def assertEncryptedPasswordDefault = { path, envName ->
    def text = assertFile(path).text
    assert text.contains('password: ${' + envName + ':ENC(v1:'): "Expected ${path} to use encrypted ${envName} default"
}
assertEncryptedPasswordDefault("src/main/resources/application-dev.yml", "DB_PASSWORD")
assertEncryptedPasswordDefault("src/main/resources/application-prod.yml", "DB_PASSWORD")
assertEncryptedPasswordDefault("src/main/resources/bootstrap-dev.yml", "NACOS_PASSWORD")
assertEncryptedPasswordDefault("src/main/resources/bootstrap-prod.yml", "NACOS_PASSWORD")
assert assertFile("src/main/resources/application-local.yml").text.contains('password: ${DB_PASSWORD:}')
assert assertFile("src/main/resources/application-test.yml").text.contains('password: ${DB_PASSWORD:}')
assert !assertFile("src/main/resources/bootstrap-local.yml").text.contains('ENC(')
assert !assertFile("src/main/resources/bootstrap-test.yml").text.contains('ENC(')

def applicationYaml = assertFile("src/main/resources/application.yml").text
assert applicationYaml.contains("threads:")
assert applicationYaml.contains("virtual:")
assert applicationYaml.contains('${SPRING_THREADS_VIRTUAL_ENABLED:true}')
assert applicationYaml.contains("timeout-per-shutdown-phase")
assert applicationYaml.contains("write-dates-as-timestamps: false")
assert applicationYaml.contains("prometheus")
assert applicationYaml.contains("tomcat:")
assert applicationYaml.contains('${TOMCAT_MAX_CONNECTIONS:8192}')
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
def asyncConfigurationText = assertFile("src/main/java/it/pkg/start/config/async/AsyncConfiguration.java").text
assert asyncConfigurationText.contains("implements AsyncConfigurer")
assert asyncConfigurationText.contains("getAsyncUncaughtExceptionHandler")
assert !asyncConfigurationText.contains("ThreadPoolTaskExecutorBuilder")
assert !asyncConfigurationText.contains("applicationTaskExecutor(")
assertFile("src/main/java/it/pkg/start/config/encryption/ConfigDecryptor.java")
assertFile("src/main/java/it/pkg/start/config/encryption/ConfigDecryptException.java")
assertFile("src/main/java/it/pkg/start/config/encryption/ConfigDecryptKeyProvider.java")
assertFile("src/main/java/it/pkg/start/config/encryption/AesGcmConfigDecryptor.java")
assertFile("src/main/java/it/pkg/start/config/encryption/ConfigDecryptEnvironmentPostProcessor.java")
assertFile("src/main/java/it/pkg/start/config/encryption/ConfigCipherCli.java")
def springFactories = assertFile("src/main/resources/META-INF/spring.factories").text
assert springFactories.contains("org.springframework.boot.env.EnvironmentPostProcessor")
assert springFactories.contains("it.pkg.start.config.encryption.ConfigDecryptEnvironmentPostProcessor")
assertFile("src/test/java/it/pkg/start/config/encryption/AesGcmConfigDecryptorTest.java")
assertFile("src/test/java/it/pkg/start/config/encryption/ConfigDecryptEnvironmentPostProcessorTest.java")
assertFile("src/main/java/it/pkg/start/config/JacksonConfig.java")
assertFile("src/main/java/it/pkg/start/config/OpenApiConfig.java")
assertFile("src/main/java/it/pkg/start/config/ActuatorConfig.java")
assertFile("src/test/java/it/pkg/start/StudentManagementApplicationTest.java")
assertFile("src/test/java/it/pkg/start/config/RuntimeConfigurationTest.java")
def starterText = assertFile("src/main/java/it/pkg/start/StudentManagementApplication.java").text
assert starterText.contains("@EnableDubbo")
assert starterText.contains('"it.pkg.adapter.user.rpc"')
assert starterText.contains('"it.pkg.adapter.teaching.rpc"')
assert starterText.contains('"it.pkg.infrastructure.user.repo.jpa"')
assert starterText.contains('"it.pkg.infrastructure.teaching.repo.jpa"')
assertFile("src/main/resources/application.yml")
assertFile("src/main/resources/db/migration/V1__init_student_management.sql")

assertFile("src/main/java/it/pkg/domain/user/aggregates/UserAggregate.java")
[
    "UserDomainService",
    "RoleDomainService",
    "PermissionDomainService",
    "UserQueryService",
    "UserCacheService",
    "UserEventPublisher"
].each { serviceName ->
    assertFile("src/main/java/it/pkg/domain/user/service/${serviceName}.java")
}
["UserRepository", "RoleRepository", "PermissionRepository"].each { repositoryName ->
    assertFile("src/main/java/it/pkg/domain/user/repos/${repositoryName}.java")
}
assertFile("src/test/java/it/pkg/domain/user/aggregates/UserAggregateTest.java")
assertFile("src/test/java/it/pkg/domain/user/aggregates/RolePermissionAggregateTest.java")

[
    "entities/SchoolClass",
    "entities/Course",
    "aggregates/SchoolClassAggregate",
    "aggregates/CourseAggregate",
    "vos/SchoolClassId",
    "vos/CourseCode",
    "vos/Semester",
    "vos/CourseSchedule",
    "vos/ExternalCourse",
    "vos/CourseSnapshot",
    "vos/TeachingEvent",
    "enums/SchoolClassStatus",
    "enums/CourseStatus",
    "exceptions/TeachingDomainException",
    "validators/TeachingDomainValidator",
    "service/SchoolClassDomainService",
    "service/CourseDomainService",
    "service/TeachingQueryService",
    "service/CourseCacheService",
    "service/TeachingEventPublisher",
    "repos/SchoolClassRepository",
    "repos/CourseRepository"
].each { typePath ->
    assertFile("src/main/java/it/pkg/domain/teaching/${typePath}.java")
}
assertFile("src/test/java/it/pkg/domain/teaching/aggregates/SchoolClassAggregateTest.java")

[
    "command/CreateUserCommand",
    "command/AssignRoleCommand",
    "command/GrantPermissionCommand",
    "query/GetUserQuery",
    "result/UserResult",
    "result/PermissionResult",
    "manage/UserManage",
    "manage/RoleManage",
    "manage/PermissionManage",
    "manage/UserUseCaseException",
    "manage/impl/UserManageImpl",
    "manage/impl/RoleManageImpl",
    "manage/impl/PermissionManageImpl",
    "convertor/UserApplicationConvertor",
    "validators/UserApplicationValidator",
    "assemblers/UserAssembler"
].each { typePath ->
    assertFile("src/main/java/it/pkg/application/user/${typePath}.java")
}
[
    "manage/UserManageTest",
    "manage/RoleManageTest",
    "manage/PermissionManageTest",
    "validators/UserApplicationValidatorTest"
].each { testPath ->
    assertFile("src/test/java/it/pkg/application/user/${testPath}.java")
}

[
    "command/CreateSchoolClassCommand",
    "command/CreateCourseCommand",
    "command/ScheduleCourseCommand",
    "query/GetCourseQuery",
    "result/SchoolClassResult",
    "result/CourseResult",
    "manage/SchoolClassManage",
    "manage/CourseManage",
    "manage/TeachingUseCaseException",
    "manage/impl/SchoolClassManageImpl",
    "manage/impl/CourseManageImpl",
    "convertor/TeachingApplicationConvertor",
    "validators/TeachingApplicationValidator",
    "assemblers/TeachingAssembler"
].each { typePath ->
    assertFile("src/main/java/it/pkg/application/teaching/${typePath}.java")
}
[
    "manage/SchoolClassManageTest",
    "manage/CourseManageTest",
    "validators/TeachingApplicationValidatorTest"
].each { testPath ->
    assertFile("src/test/java/it/pkg/application/teaching/${testPath}.java")
}

assertFile("src/main/resources/db/migration/V1__init_student_management.sql")
assertFile("src/main/resources/db/migration/V2__align_large_monolith_domain.sql")
def migrationFiles = new File(generatedProjectDir, "src/main/resources/db/migration")
        .listFiles()
        .findAll { it.name.endsWith(".sql") }
assert migrationFiles.size() == 2: "Expected exactly two migration SQL files"
[
    "user/repo/po/UserPO",
    "user/repo/jpa/UserJpaRepository",
    "user/repo/impl/UserRepositoryImpl",
    "teaching/repo/po/SchoolClassPO",
    "teaching/repo/jpa/SchoolClassJpaRepository",
    "teaching/repo/impl/SchoolClassRepositoryImpl"
].each { typePath ->
    assertFile("src/main/java/it/pkg/infrastructure/${typePath}.java")
}
assertFile("src/test/java/it/pkg/infrastructure/user/repo/UserRepositoryImplTest.java")
assertFile("src/test/java/it/pkg/infrastructure/teaching/repo/SchoolClassRepositoryImplTest.java")
assert pom.contains("<id>postgres-flyway-verify</id>")
assert pom.contains("<artifactId>flyway-maven-plugin</artifactId>")
assert pom.contains('${env.POSTGRES_VERIFY_URL}')
assert pom.contains('${env.POSTGRES_VERIFY_USER}')
assert pom.contains('${env.POSTGRES_VERIFY_PASSWORD}')

[
    "user/service/impl/UserDomainServiceImpl",
    "user/service/impl/RoleDomainServiceImpl",
    "user/service/impl/PermissionDomainServiceImpl",
    "teaching/service/impl/SchoolClassDomainServiceImpl",
    "teaching/service/impl/CourseDomainServiceImpl",
    "user/client/impl/LocalUserQueryService",
    "teaching/client/impl/LocalTeachingQueryService",
    "user/cache/InMemoryUserCacheService",
    "teaching/cache/InMemoryCourseCacheService",
    "user/mq/LocalUserEventPublisher",
    "teaching/mq/LocalTeachingEventPublisher",
    "config/TransactionCompletionExecutor",
    "config/LocalAdapterConfiguration"
].each { typePath ->
    assertFile("src/main/java/it/pkg/infrastructure/${typePath}.java")
}
assertFile("src/test/java/it/pkg/infrastructure/config/LocalAdapterConfigurationTest.java")
assertFile("src/test/java/it/pkg/infrastructure/config/TransactionCompletionExecutorTest.java")

[
    "user/client/impl/RestUserQueryService",
    "teaching/client/impl/RestTeachingQueryService",
    "user/cache/RedisUserCacheService",
    "teaching/cache/RedisCourseCacheService",
    "user/mq/RabbitUserEventPublisher",
    "teaching/mq/RabbitTeachingEventPublisher",
    "user/validators/UserInfrastructureValidator",
    "teaching/validators/TeachingInfrastructureValidator",
    "aop/RepositoryMonitorAspect",
    "aop/InfrastructureLogAspect",
    "config/ExternalClientConfig",
    "config/RabbitMqConfig",
    "config/RedisConfig"
].each { typePath ->
    assertFile("src/main/java/it/pkg/infrastructure/${typePath}.java")
}
[
    "user/client/RestUserQueryServiceTest",
    "teaching/client/RestTeachingQueryServiceTest",
    "user/mq/RabbitUserEventPublisherTest",
    "teaching/mq/RabbitTeachingEventPublisherTest",
    "user/cache/RedisUserCacheServiceTest",
    "teaching/cache/RedisCourseCacheServiceTest",
    "user/validators/UserInfrastructureValidatorTest",
    "teaching/validators/TeachingInfrastructureValidatorTest",
    "aop/InfrastructureAspectTest",
    "config/RabbitMqConfigTest"
].each { testPath ->
    assertFile("src/test/java/it/pkg/infrastructure/${testPath}.java")
}

[
    "controller/UserController",
    "controller/RoleController",
    "controller/PermissionController",
    "dto/CreateUserRequest",
    "dto/AssignRoleRequest",
    "dto/GrantPermissionRequest",
    "vo/UserDetailVO",
    "vo/PermissionTreeVO",
    "convertor/UserAdapterConvertor",
    "validators/UserRequestValidator",
    "graphql/UserResolver",
    "facade/impl/UserFacadeImpl",
    "facade/impl/PermissionFacadeImpl",
    "rpc/UserRpcProvider",
    "mq/UserImportedConsumer"
].each { typePath ->
    assertFile("src/main/java/it/pkg/adapter/user/${typePath}.java")
}
[
    "UserFacade",
    "PermissionFacade",
    "dto/CreateUserDTO",
    "dto/AssignRoleDTO",
    "dto/GrantPermissionDTO",
    "dto/UserDetailDTO",
    "dto/PermissionDTO",
    "enums/UserFacadeStatus",
    "exceptions/UserFacadeException",
    "utils/UserFacadeAssert"
].each { typePath ->
    assertFile("src/main/java/it/pkg/facade/user/${typePath}.java")
}
[
    "RequestContext",
    "RequestContextHolder",
    "TraceIdFilter",
    "RequestContextFilter"
].each { typeName ->
    assertFile("src/main/java/it/pkg/adapter/filter/${typeName}.java")
}
assertFile("src/main/resources/graphql/user.graphqls")
["controller", "mq", "rpc"].each { packageName ->
    assert new File(generatedProjectDir, "src/main/java/it/pkg/adapter/user/${packageName}").isDirectory()
    assert !new File(generatedProjectDir, "src/main/java/it/pkg/adapter/${packageName}/user").exists()
}
[
    "controller/SchoolClassController",
    "controller/CourseController",
    "dto/CreateSchoolClassRequest",
    "dto/CreateCourseRequest",
    "dto/ScheduleCourseRequest",
    "vo/SchoolClassDetailVO",
    "vo/CourseDetailVO",
    "convertor/TeachingAdapterConvertor",
    "validators/TeachingRequestValidator",
    "graphql/CourseResolver",
    "facade/impl/SchoolClassFacadeImpl",
    "facade/impl/CourseFacadeImpl",
    "rpc/CourseRpcProvider",
    "mq/CourseImportedConsumer"
].each { typePath ->
    assertFile("src/main/java/it/pkg/adapter/teaching/${typePath}.java")
}
[
    "SchoolClassFacade",
    "CourseFacade",
    "dto/CreateSchoolClassDTO",
    "dto/CreateCourseDTO",
    "dto/ScheduleCourseDTO",
    "dto/SchoolClassDetailDTO",
    "dto/CourseDTO",
    "enums/CourseFacadeStatus",
    "exceptions/TeachingFacadeException",
    "utils/TeachingFacadeAssert"
].each { typePath ->
    assertFile("src/main/java/it/pkg/facade/teaching/${typePath}.java")
}
[
    "ApiResponse",
    "GlobalExceptionHandler",
    "ResponseWrapperHandler",
    "GraphQlExceptionResolver",
    "RabbitConsumerErrorHandler"
].each { typeName ->
    assertFile("src/main/java/it/pkg/adapter/handler/${typeName}.java")
}
assertFile("src/main/resources/graphql/teaching.graphqls")
["controller", "mq", "rpc"].each { packageName ->
    assert new File(generatedProjectDir, "src/main/java/it/pkg/adapter/teaching/${packageName}").isDirectory()
}

def facadeJavaFiles = []
new File(generatedProjectDir, "src/main/java/it/pkg/facade").eachFileRecurse { file ->
    if (file.isFile() && file.name.endsWith(".java")) {
        facadeJavaFiles << file
    }
}
assert !facadeJavaFiles.isEmpty()
assert facadeJavaFiles.every { !it.text.contains("import it.pkg.domain.") }

def allApplicationJava = []
new File(generatedProjectDir, "src/main/java/it/pkg/application").eachFileRecurse { file ->
    if (file.isFile() && file.name.endsWith(".java")) {
        allApplicationJava << file
    }
}
assert allApplicationJava.every { !it.text.contains("View") }
assert allApplicationJava.every { !it.text.contains("facade.dto") }
assert allApplicationJava.every { !it.text.contains("common.response") }

[
    "adapter/controller",
    "adapter/convertor",
    "adapter/facade",
    "adapter/validation",
    "adapter/vo",
    "application/config",
    "application/convertor",
    "application/manage",
    "application/validators",
    "domain/common",
    "domain/student",
    "domain/teaching/model",
    "facade/api",
    "facade/dto",
    "facade/enums",
    "infrastructure/repo"
].each { reversedPath ->
    assert !new File(generatedProjectDir, "src/main/java/it/pkg/${reversedPath}").exists()
}

assert !new File(generatedProjectDir, "src/main/java/it/pkg/adapter/ChargeController.java").exists()
assert !new File(generatedProjectDir, "src/main/java/it/pkg/domain/charge").exists()
assert !new File(generatedProjectDir, "src/test/charge.http").exists()

def migrationDir = new File(generatedProjectDir, "src/main/resources/db/migration")
assert migrationDir.listFiles({ dir, name -> name.endsWith(".sql") } as FilenameFilter).size() == 2

assertFile("src/test/java/it/pkg/ArchitectureDependencyTest.java")

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
