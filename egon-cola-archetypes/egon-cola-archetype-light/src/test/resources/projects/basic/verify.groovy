def generatedProjectDir = new File(basedir, "project/basic")

def assertFile = { path ->
    def file = new File(generatedProjectDir, path)
    assert file.isFile(): "Expected file ${path}"
    file
}

def assertMissing = { path ->
    assert !new File(generatedProjectDir, path).exists(): "Unexpected path ${path}"
}

def assertPortableDockerfile = { jarFile, exposedPorts, readinessPort ->
    assertMissing("Dockerfile")
    [
        "Dockerfile.containerd",
        "Dockerfile.nerdctl",
        "Dockerfile.podman",
        "Containerfile",
        "Containerfile.podman",
        "deploy/container/Dockerfile.containerd",
        "deploy/container/Dockerfile.nerdctl",
        "deploy/container/Dockerfile.podman",
        "deploy/container/Containerfile",
        "deploy/container/Containerfile.podman"
    ].each { assertMissing(it) }

    def text = assertFile("deploy/container/Dockerfile").text
    assert text.contains("ARG BUILD_IMAGE=eclipse-temurin:21-jdk-jammy")
    assert text.contains('FROM ${BUILD_IMAGE} AS builder')
    assert text.contains("chmod +x mvnw")
    assert text.contains("./mvnw -B -ntp -DskipTests package")
    assert text.contains("ARG RUNTIME_IMAGE=eclipse-temurin:21-jre-jammy")
    assert text.contains('FROM ${RUNTIME_IMAGE} AS extractor')
    assert text.contains("ARG JAR_FILE=${jarFile}")
    assert text.contains('COPY --from=builder /workspace/${JAR_FILE} app.jar')
    assert text.contains("java -Djarmode=tools -jar app.jar extract --layers --destination extracted")
    assert text.contains('FROM ${RUNTIME_IMAGE} AS runtime')
    assert text.contains("ARG CONTAINER_ENGINE=oci")
    assert text.contains("ARG APP_UID=10001")
    assert text.contains("ARG APP_GID=10001")
    assert text.contains("org.opencontainers.image.build.engine")
    assert text.contains("USER app")
    assert text.contains("EXPOSE ${exposedPorts}")
    assert text.contains("http://127.0.0.1:${readinessPort}/actuator/health/readiness")
    assert text.contains("JarLauncher")
    assert !text.contains("--mount=type=cache")
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

def assertNoInternalImports = { path, forbiddenLayers ->
    javaFileTexts(path).each { text ->
        forbiddenLayers.each { layer ->
            assert !text.contains("import it.pkg.${layer}."):
                    "Unexpected ${layer} import under ${path}"
        }
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
assertFile(".dockerignore")
assert assertFile("README.md").text.contains("Docker")
assert assertFile("README.md").text.contains("docker build -t basic:local .")
assertPortableDockerfile("target/*.jar", "8080 50051", "8080")
def developmentEnv = assertFile("deploy/env/.env.example").text
def productionEnv = assertFile("deploy/env/.env.prod.example").text
[
    "POSTGRES_IMAGE=postgres:17-alpine",
    "REDIS_IMAGE=redis:7.4-alpine",
    "RABBITMQ_IMAGE=rabbitmq:4-management",
    "NACOS_IMAGE=nacos/nacos-server:v2.5.1",
    "POSTGRES_PASSWORD=",
    "REDIS_PASSWORD=",
    "RABBITMQ_PASSWORD=",
    "NACOS_PASSWORD=",
    "NACOS_AUTH_TOKEN="
].each { expected ->
    assert productionEnv.readLines().contains(expected):
            "Expected production env example line ${expected}"
}
[
    "POSTGRES_PASSWORD=local-postgres",
    "REDIS_PASSWORD=local-redis",
    "RABBITMQ_PASSWORD=local-rabbitmq",
    "NACOS_PASSWORD=nacos"
].each { forbidden ->
    assert !productionEnv.contains(forbidden):
            "Production env example must not contain development credential ${forbidden}"
}
assert developmentEnv.contains("IMAGE_TAG=local")
assert developmentEnv.contains("NACOS_AUTH_ENABLE=true")
assert productionEnv.contains("REGISTRY=")
assert productionEnv.contains("REGISTRY_NAMESPACE=")
assert productionEnv.contains("IMAGE_TAG=")
assert developmentEnv.contains("IMAGE_NAME=basic")
assert developmentEnv.contains("POSTGRES_DB=student_management")
assert developmentEnv.contains("EXTERNAL_HTTP_ENABLED=false")
def assertDevelopmentCompose = { fileName, engine, requiredApplicationLines ->
    def text = assertFile("deploy/compose/${fileName}").text
    ["application:", "postgres:", "redis:", "rabbitmq:", "nacos:",
     "healthcheck:", "networks:", "volumes:", "application_logs:"].each { token ->
        assert text.contains(token): "Expected ${fileName} to contain ${token}"
    }
    assert text.contains("CONTAINER_ENGINE: ${engine}")
    assert text.contains("dockerfile: deploy/container/Dockerfile")
    assert text.contains("context: ../..")
    assert text.contains('SPRING_PROFILES_ACTIVE: dev')
    assert text.contains('jdbc:postgresql://postgres:5432/${POSTGRES_DB}')
    assert text.contains("NACOS_SERVER_ADDR: nacos:8848")
    assert text.contains("DUBBO_REGISTRY_ADDRESS: nacos://nacos:8848")
    assert text.contains('pg_isready -U "$${POSTGRES_USER}" -d "$${POSTGRES_DB}"')
    assert text.contains('redis-cli --no-auth-warning -a "$${REDIS_PASSWORD}" ping')
    assert text.contains('["CMD", "rabbitmq-diagnostics", "-q", "ping"]')
    requiredApplicationLines.each { required ->
        assert text.contains(required): "Expected ${fileName} to contain ${required}"
    }
}
def developmentComposeFiles = [
    "compose.docker.yaml" : "docker",
    "compose.podman.yaml" : "podman",
    "compose.nerdctl.yaml": "nerdctl"
]
developmentComposeFiles.each { fileName, engine ->
    assertDevelopmentCompose(fileName, engine, [
        'SERVER_PORT: ${APPLICATION_PORT:-8080}',
        'EXTERNAL_HTTP_ENABLED: "false"',
        '"${APPLICATION_PORT:-8080}:${APPLICATION_PORT:-8080}"'
    ])
}
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
    "**/target",
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
    "!deploy/env/.env.example",
    "!deploy/env/.env.prod.example",
    "config/application-secrets.yml",
    "secrets/",
    "*.pem",
    "*.key"
].each {
    assert gitignoreLines.contains(it): "Expected .gitignore to contain line ${it}"
}

def pom = assertFile("pom.xml").text
def pomXml = new groovy.xml.XmlSlurper(false, false).parse(assertFile("pom.xml"))
def generatedPoms = []
generatedProjectDir.eachFileRecurse { file ->
    if (file.isFile() && file.name == "pom.xml") {
        generatedPoms << file
    }
}
assert generatedPoms.size() == 1: "Expected a single Maven module"
assert pomXml.modules.module.isEmpty(): "Expected no Maven child modules"
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
assert pom.contains("<egon-cola.version>5.2.2</egon-cola.version>")
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

def localConfig = assertFile("src/main/resources/application-local.yml").text
def testConfig = assertFile("src/main/resources/application-test.yml").text
[localConfig, testConfig].each { profileConfig ->
    assert profileConfig.contains("rabbitmq:\n      enabled: false")
    assert profileConfig.contains("redis:\n      enabled: false")
    assert profileConfig.contains("external-http:\n      enabled: false")
}
assert testConfig.contains("export: false")
assert testConfig.contains("rabbit:\n      enabled: false")
assert testConfig.contains("redis:\n      enabled: false")

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
assertFile("src/main/java/it/pkg/package-info.java")
def requiredPackagePaths = [
    "start",
    "start/config",
    "start/config/async",
    "start/config/encryption",
    "adapter",
    "adapter/user",
    "adapter/user/controller",
    "adapter/user/mq",
    "adapter/user/rpc",
    "adapter/user/graphql",
    "adapter/user/facade",
    "adapter/user/facade/impl",
    "adapter/user/dto",
    "adapter/user/vo",
    "adapter/user/convertor",
    "adapter/user/validators",
    "adapter/teaching",
    "adapter/teaching/controller",
    "adapter/teaching/mq",
    "adapter/teaching/rpc",
    "adapter/teaching/graphql",
    "adapter/teaching/facade",
    "adapter/teaching/facade/impl",
    "adapter/teaching/dto",
    "adapter/teaching/vo",
    "adapter/teaching/convertor",
    "adapter/teaching/validators",
    "adapter/handler",
    "adapter/filter",
    "facade",
    "facade/user",
    "facade/user/dto",
    "facade/user/enums",
    "facade/user/exceptions",
    "facade/user/utils",
    "facade/teaching",
    "facade/teaching/dto",
    "facade/teaching/enums",
    "facade/teaching/exceptions",
    "facade/teaching/utils",
    "application",
    "application/user",
    "application/user/manage",
    "application/user/manage/impl",
    "application/user/command",
    "application/user/query",
    "application/user/result",
    "application/user/convertor",
    "application/user/validators",
    "application/user/assemblers",
    "application/teaching",
    "application/teaching/manage",
    "application/teaching/manage/impl",
    "application/teaching/command",
    "application/teaching/query",
    "application/teaching/result",
    "application/teaching/convertor",
    "application/teaching/validators",
    "application/teaching/assemblers",
    "domain",
    "domain/user",
    "domain/user/entities",
    "domain/user/aggregates",
    "domain/user/vos",
    "domain/user/service",
    "domain/user/repos",
    "domain/user/validators",
    "domain/user/enums",
    "domain/user/exceptions",
    "domain/teaching",
    "domain/teaching/entities",
    "domain/teaching/aggregates",
    "domain/teaching/vos",
    "domain/teaching/service",
    "domain/teaching/repos",
    "domain/teaching/validators",
    "domain/teaching/enums",
    "domain/teaching/exceptions",
    "infrastructure",
    "infrastructure/user",
    "infrastructure/user/repo",
    "infrastructure/user/repo/impl",
    "infrastructure/user/repo/po",
    "infrastructure/user/repo/jpa",
    "infrastructure/user/repo/converter",
    "infrastructure/user/service",
    "infrastructure/user/service/impl",
    "infrastructure/user/validators",
    "infrastructure/user/client",
    "infrastructure/user/client/impl",
    "infrastructure/user/mq",
    "infrastructure/user/cache",
    "infrastructure/teaching",
    "infrastructure/teaching/repo",
    "infrastructure/teaching/repo/impl",
    "infrastructure/teaching/repo/po",
    "infrastructure/teaching/repo/jpa",
    "infrastructure/teaching/repo/converter",
    "infrastructure/teaching/service",
    "infrastructure/teaching/service/impl",
    "infrastructure/teaching/validators",
    "infrastructure/teaching/client",
    "infrastructure/teaching/client/impl",
    "infrastructure/teaching/mq",
    "infrastructure/teaching/cache",
    "infrastructure/aop",
    "infrastructure/config",
    "common",
    "common/constants",
    "common/utils",
    "common/enums",
    "common/exceptions"
]
requiredPackagePaths.each { packagePath ->
    assertFile("src/main/java/it/pkg/${packagePath}/package-info.java")
}
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
    "query/GetUserPermissionsQuery",
    "result/UserResult",
    "result/PermissionResult",
    "result/PermissionDetailResult",
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
    "query/GetSchoolClassQuery",
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
    "rpc/PermissionRpcProvider",
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
    "dto/PermissionDetailDTO",
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
    "rpc/SchoolClassRpcProvider",
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

assertNoInternalImports("src/main/java/it/pkg/start", ["application", "common", "domain", "facade"])
assertNoInternalImports("src/main/java/it/pkg/adapter", ["common", "domain", "infrastructure", "start"])
assertNoInternalImports("src/main/java/it/pkg/application", ["adapter", "common", "facade", "infrastructure", "start"])
assertNoInternalImports("src/main/java/it/pkg/domain", ["adapter", "application", "facade", "infrastructure", "start"])
assertNoInternalImports("src/main/java/it/pkg/infrastructure", ["adapter", "application", "common", "facade", "start"])
assertNoInternalImports("src/main/java/it/pkg/facade", ["adapter", "application", "common", "domain", "infrastructure", "start"])
assertNoInternalImports("src/main/java/it/pkg/common", ["adapter", "application", "domain", "facade", "infrastructure", "start"])

[
    "src/main/java/it/pkg/adapter/controller",
    "src/main/java/it/pkg/adapter/mq",
    "src/main/java/it/pkg/adapter/rpc",
    "src/main/java/it/pkg/adapter/graphql",
    "src/main/java/it/pkg/adapter/facade",
    "src/main/java/it/pkg/adapter/dto",
    "src/main/java/it/pkg/adapter/vo",
    "src/main/java/it/pkg/adapter/convertor",
    "src/main/java/it/pkg/adapter/validators",
    "src/main/java/it/pkg/adapter/validation",
    "src/main/java/it/pkg/application/manage",
    "src/main/java/it/pkg/application/command",
    "src/main/java/it/pkg/application/query",
    "src/main/java/it/pkg/application/result",
    "src/main/java/it/pkg/application/convertor",
    "src/main/java/it/pkg/application/validators",
    "src/main/java/it/pkg/application/assemblers",
    "src/main/java/it/pkg/application/config",
    "src/main/java/it/pkg/facade/api",
    "src/main/java/it/pkg/facade/dto",
    "src/main/java/it/pkg/facade/enums",
    "src/main/java/it/pkg/facade/exceptions",
    "src/main/java/it/pkg/facade/utils",
    "src/main/java/it/pkg/infrastructure/repo",
    "src/main/java/it/pkg/infrastructure/service",
    "src/main/java/it/pkg/infrastructure/validators",
    "src/main/java/it/pkg/infrastructure/client",
    "src/main/java/it/pkg/infrastructure/mq",
    "src/main/java/it/pkg/infrastructure/cache",
    "src/main/java/it/pkg/domain/common",
    "src/main/java/it/pkg/domain/student",
    "src/main/java/it/pkg/domain/teaching/model"
].each { path ->
    assert !new File(generatedProjectDir, path).exists(): "Unexpected reversed or stale path ${path}"
}
assert !new File(generatedProjectDir, "src/main/java/it/pkg/application/client").exists()
assert !new File(generatedProjectDir, "src/main/java/it/pkg/domain/user/service/impl").exists()
assert !new File(generatedProjectDir, "src/main/java/it/pkg/domain/teaching/service/impl").exists()

assert !new File(generatedProjectDir, "src/main/java/it/pkg/adapter/ChargeController.java").exists()
assert !new File(generatedProjectDir, "src/main/java/it/pkg/domain/charge").exists()
assert !new File(generatedProjectDir, "src/test/charge.http").exists()

def migrationDir = new File(generatedProjectDir, "src/main/resources/db/migration")
assert migrationDir.listFiles({ dir, name -> name.endsWith(".sql") } as FilenameFilter).size() == 2

assertFile("src/test/java/it/pkg/ArchitectureDependencyTest.java")
assertFile("src/test/java/it/pkg/adapter/user/rpc/PermissionRpcProviderTest.java")
assertFile("src/test/java/it/pkg/adapter/teaching/rpc/SchoolClassRpcProviderTest.java")

def readme = assertFile("README.md").text
assert readme.contains("Student Management")
assert readme.contains("single Maven module")
assert readme.contains("start          -> adapter, infrastructure")
assert readme.contains("adapter        -> application, facade")
assert readme.contains("infrastructure -> domain")
assert readme.contains("Domain-First Structure")
assert readme.contains("Primary Workflows")
assert readme.contains("RABBITMQ_ENABLED=true")
assert readme.contains("ConfigCipherCli")
assert readme.contains("docker build -t basic:local .")
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
