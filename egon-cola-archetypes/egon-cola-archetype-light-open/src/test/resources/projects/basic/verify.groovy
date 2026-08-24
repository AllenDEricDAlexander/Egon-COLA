import groovy.xml.XmlSlurper

def projectDir = new File(basedir, "project/basic")
assert projectDir.isDirectory(): "Expected generated Light Open project"

def file = { String path ->
    def result = new File(projectDir, path)
    assert result.isFile(): "Expected generated file ${path}"
    result
}

def directory = { String path ->
    def result = new File(projectDir, path)
    assert result.isDirectory(): "Expected generated directory ${path}"
    result
}

def missing = { String path ->
    assert !new File(projectDir, path).exists(): "Unexpected generated path ${path}"
}

def filesUnder = { String path, Closure<Boolean> predicate ->
    def root = new File(projectDir, path)
    assert root.isDirectory(): "Expected generated directory ${path}"
    def result = []
    root.eachFileRecurse { candidate ->
        if (candidate.isFile() && predicate(candidate)) {
            result << candidate
        }
    }
    result
}

def sourceText = filesUnder("src/main") { it.name.endsWith(".java") || it.name.endsWith(".xml") ||
        it.name.endsWith(".yml") || it.name.endsWith(".yaml") || it.name.endsWith(".sql") }
        .collect { it.getText("UTF-8") }.join("\n")
def pomFiles = [file("pom.xml")] + filesUnder(".") { it.name == "pom.xml" && it.parentFile != projectDir }
def pomText = pomFiles.collect { it.getText("UTF-8") }.join("\n")

def forbidden = [
        "spring-boot-starter-data-jpa",
        "jakarta.persistence",
        "org.flywaydb",
        "flyway-database",
        "liquibase",
        "spring-cloud-starter-gateway",
        "spring.cloud.gateway",
        "UuidV7Generator",
        "UUID.randomUUID",
        "egon-cola-component-bytecode-architecture"
]
forbidden.each { token ->
    assert !sourceText.contains(token): "Forbidden runtime token ${token}"
    assert !pomText.contains(token): "Forbidden dependency/plugin token ${token}"
}

def rootPom = new XmlSlurper(false, false).parse(file("pom.xml"))
assert rootPom.modules.module.isEmpty(): "Light Open must remain one generated Maven project"
assert rootPom.parent.artifactId.text() == "spring-boot-starter-parent"
assert rootPom.parent.version.text() == "3.5.16"
assert rootPom.properties.'java.version'.text() == "21"
assert rootPom.properties.'spring-cloud.version'.text() == "2025.0.3"
assert rootPom.properties.'spring-cloud-alibaba.version'.text() == "2025.0.0.0"
assert rootPom.properties.'springdoc.version'.text() == "2.8.17"
assert rootPom.properties.'shardingsphere.version'.text() == "5.5.3"
assert rootPom.properties.'mybatis-plus.version'.text() == "3.5.17"
[
        "egon-cola-component-common-core",
        "egon-cola-component-common-id-starter",
        "egon-cola-component-dynamic-thread-pool-starter",
        "mybatis-plus-spring-boot3-starter",
        "springdoc-openapi-starter-webmvc-ui",
        "archunit-junit5"
].each { artifact ->
    assert pomText.contains("<artifactId>${artifact}</artifactId>"):
            "Expected Light Open dependency ${artifact}"
}
assert !pomText.contains("dubbo-spring-boot-starter")

[
        "src/main/java/it/pkg/infrastructure/config/datasource/SnowflakeLongShardingAlgorithm.java",
        "src/main/resources/mybatis/mapper/user/UserMapper.xml",
        "src/main/resources/mybatis/mapper/user/RoleMapper.xml",
        "src/main/resources/mybatis/mapper/user/PermissionMapper.xml",
        "src/main/resources/mybatis/mapper/user/UserRoleMapper.xml",
        "src/main/resources/mybatis/mapper/user/RolePermissionMapper.xml",
        "src/main/resources/mybatis/mapper/teaching/CourseMapper.xml",
        "src/main/resources/mybatis/mapper/teaching/ClassCourseScheduleMapper.xml",
        "src/main/resources/mybatis/mapper/teaching/SchoolClassMapper.xml",
        "src/main/resources/db/manual/postgresql/README.md",
        "src/main/resources/db/manual/postgresql/master-data/001__create_light_master_data_schema.sql",
        "src/main/resources/db/manual/postgresql/shard/002__create_light_sharded_schema.sql",
        "src/test/java/it/pkg/architecture/OpenArchitectureTest.java",
        "src/test/java/it/pkg/start/StudentManagementApplicationTest.java",
        "src/test/java/it/pkg/start/config/RuntimeConfigurationTest.java",
        "src/test/java/it/pkg/start/config/async/AsyncConfigurationTest.java"
].each { path -> file(path) }
[
        "src/main/java/it/pkg/common",
        "src/main/java/it/pkg/domain",
        "src/main/java/it/pkg/application",
        "src/main/java/it/pkg/infrastructure",
        "src/main/java/it/pkg/adapter",
        "src/main/java/it/pkg/facade",
        "src/main/java/it/pkg/start"
].each { path -> directory(path) }

assert filesUnder("src/main/resources/mybatis/mapper") { it.name.endsWith("Mapper.xml") }.size() == 8
assert file("src/main/resources/db/manual/postgresql/master-data/001__create_light_master_data_schema.sql")
        .text.contains("BIGINT")
assert file("src/main/resources/db/manual/postgresql/shard/002__create_light_sharded_schema.sql")
        .text.contains("BIGINT")
missing("src/main/resources/db/migration")
missing("src/main/java/it/pkg/infrastructure/user/repo/jpa")
missing("src/main/java/it/pkg/infrastructure/teaching/repo/jpa")
missing("target/egon-cola-architecture")

def application = file("src/main/resources/application.yml").text
assert application.contains("machine-id: \${EGON_ID_MACHINE_ID}")
assert application.contains("queue-capacity: \${ASYNC_QUEUE_CAPACITY:1000}")
assert application.contains("keep-alive: \${ASYNC_KEEP_ALIVE:60s}")
assert application.contains("egon-cola-component") || pomText.contains("dynamic-thread-pool-starter")
assert application.contains("enabled: \${DTP_ENABLED:true}")
def testApplication = file("src/main/resources/application-test.yml").text
assert testApplication.contains("machine-id: 0")
assert testApplication.contains("enabled: false")
assert testApplication.contains("report:")
assert file("deploy/env/.env.example").text.contains("NACOS_IMAGE=nacos/nacos-server:v3.0.3")
assert file("deploy/env/.env.example").text.contains("EGON_ID_MACHINE_ID=0")
assert file("deploy/env/.env.prod.example").text.contains("EGON_ID_MACHINE_ID=")
[
        "compose.docker.yaml", "compose.podman.yaml", "compose.nerdctl.yaml",
        "compose.docker.prod.yaml", "compose.podman.prod.yaml", "compose.nerdctl.prod.yaml"
].each { name ->
    def compose = file("deploy/compose/${name}").text
    assert compose.contains("EGON_ID_MACHINE_ID")
    assert compose.contains("DTP_ENABLED")
    assert compose.contains("NACOS_SERVER_ADDR: nacos:8848")
    assert compose.contains("nacos:")
}

def reportFiles = filesUnder("target") { it.path.replace('\\', '/').contains('/surefire-reports/') && it.name.endsWith('.xml') }
[
        "OpenArchitectureTest",
        "StudentManagementApplicationTest",
        "RuntimeConfigurationTest",
        "AsyncConfigurationTest",
        "ManualSqlConventionTest",
        "ManualSchemaIntegrationTest"
].each { testName ->
    def report = reportFiles.find { it.name.contains(testName) }
    assert report && report.text.contains('failures="0"') && report.text.contains('errors="0"'):
            "Expected generated test report ${testName} to pass"
}

assert file("README.md").text.contains("Dynamic Thread Pool")
assert file("README.md").text.contains("MyBatis-Plus")
assert file("README.md").text.contains("manual")
