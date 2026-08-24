import groovy.xml.XmlSlurper

def projectDir = [
        new File(basedir, "project/student-management-organization"),
        new File(basedir, "student-management-organization"),
        basedir
].find { candidate -> new File(candidate, "student-management-organization-starter").isDirectory() }
assert projectDir != null: "Expected generated Web Open project"

def file = { String path ->
    def result = new File(projectDir, path)
    assert result.isFile(): "Expected generated file ${path}"
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

def modules = [
        "student-management-organization-common",
        "student-management-organization-domain",
        "student-management-organization-application",
        "student-management-organization-facade",
        "student-management-organization-infrastructure",
        "student-management-organization-adapter",
        "student-management-organization-starter"
]
def rootPomFile = file("pom.xml")
def rootPom = new XmlSlurper(false, false).parse(rootPomFile)
assert rootPom.modules.module*.text() == modules
assert rootPom.parent.version.text() == "3.5.16"
assert rootPom.properties.'java.version'.text() == "21"
assert rootPom.properties.'spring-cloud.version'.text() == "2025.0.3"
assert rootPom.properties.'spring-cloud-alibaba.version'.text() == "2025.0.0.0"
assert rootPom.properties.'dubbo.version'.text() == "3.3.6"
assert rootPom.properties.'grpc.version'.text() == "1.73.0"
assert rootPom.properties.'springdoc.version'.text() == "2.8.17"
assert rootPom.properties.'shardingsphere.version'.text() == "5.5.3"
assert rootPom.properties.'mybatis-plus.version'.text() == "3.5.17"
modules.each { module -> file("${module}/pom.xml") }

def runtimeFiles = filesUnder(".") { candidate ->
    def path = candidate.path.replace('\\', '/')
    path.contains('/src/main/') || candidate.name == 'pom.xml'
}
def runtimeText = runtimeFiles.collect { it.getText("UTF-8") }.join("\n")
[
        "spring-cloud-starter-gateway",
        "spring.cloud.gateway",
        "spring-boot-starter-data-jpa",
        "jakarta.persistence",
        "org.flywaydb",
        "flyway-database",
        "liquibase",
        "UuidV7Generator",
        "UUID.randomUUID",
        "egon-cola-organization-facade",
        "egon-cola-evaluation-facade",
        "top.egon.cola.organization.facade",
        "top.egon.cola.evaluation.facade",
        "egon-cola-component-bytecode-architecture"
].each { token ->
    assert !runtimeText.contains(token): "Forbidden Web Open runtime token ${token}"
}

def protoRoot = new File(projectDir, "student-management-organization-facade/src/main/proto")
assert protoRoot.isDirectory()
def protoFiles = []
protoRoot.eachFileRecurse { candidate ->
    if (candidate.isFile() && candidate.name.endsWith('.proto')) {
        protoFiles << candidate.path.substring(protoRoot.path.length() + 1)
                .replace('\\', '/')
    }
}
assert protoFiles.sort() == [
        "evaluation/v1/course.proto",
        "evaluation/v1/exam.proto",
        "evaluation/v1/score.proto",
        "google/protobuf/empty.proto",
        "organization/v1/teaching.proto",
        "organization/v1/user.proto"
]

[
        "student-management-organization-facade/src/test/java/it/pkg/facade/ProtoContractTest.java",
        "student-management-organization-adapter/src/main/java/it/pkg/adapter/user/facade/impl/UserFacadeImpl.java",
        "student-management-organization-adapter/src/main/java/it/pkg/adapter/user/facade/impl/RoleFacadeImpl.java",
        "student-management-organization-adapter/src/main/java/it/pkg/adapter/user/facade/impl/PermissionFacadeImpl.java",
        "student-management-organization-adapter/src/main/java/it/pkg/adapter/teaching/facade/impl/GradeFacadeImpl.java",
        "student-management-organization-adapter/src/main/java/it/pkg/adapter/teaching/facade/impl/SchoolClassFacadeImpl.java",
        "student-management-organization-adapter/src/main/java/it/pkg/adapter/user/rpc/UserRpcProvider.java",
        "student-management-organization-adapter/src/main/java/it/pkg/adapter/teaching/rpc/SchoolClassRpcProvider.java",
        "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/client/evaluation/GrpcEvaluationQueryClient.java",
        "student-management-organization-infrastructure/src/test/java/it/pkg/infrastructure/client/evaluation/GrpcEvaluationQueryClientTest.java",
        "student-management-organization-starter/src/test/java/it/pkg/starter/OrganizationApplicationTest.java",
        "student-management-organization-starter/src/test/java/it/pkg/starter/OrganizationExternalFreeContextTest.java",
        "student-management-organization-starter/src/test/java/it/pkg/starter/OpenApiContractTest.java",
        "student-management-organization-starter/src/test/java/it/pkg/architecture/OpenArchitectureTest.java"
].each { path -> file(path) }

def mapperFiles = filesUnder("student-management-organization-infrastructure/src/main/resources/mapper") {
    it.name.endsWith("Mapper.xml")
}
assert mapperFiles.size() == 8
[
        "deploy/sql/README.md",
        "deploy/sql/mysql-master.sql",
        "deploy/sql/mysql-shard.sql"
].each { path -> file(path) }
assert file("deploy/sql/mysql-master.sql").text.contains("BIGINT")
assert file("deploy/sql/mysql-shard.sql").text.contains("BIGINT")
missing("student-management-organization-infrastructure/src/main/resources/db/migration")
missing("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/user/repo/jpa")
missing("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/teaching/repo/jpa")

def starterPom = file("student-management-organization-starter/pom.xml").text
assert starterPom.contains("egon-cola-component-dynamic-thread-pool-starter")
assert starterPom.contains("archunit-junit5")
def application = file("student-management-organization-starter/src/main/resources/application.yml").text
assert application.contains("machine-id: \${EGON_ID_MACHINE_ID}")
assert application.contains("queue-capacity: \${ASYNC_QUEUE_CAPACITY:1000}")
assert application.contains("keep-alive: \${ASYNC_KEEP_ALIVE:60s}")
assert application.contains("springdoc")
assert application.contains("/v3/api-docs")
assert file("student-management-organization-starter/src/main/resources/application-dev.yml").text
        .contains("name: tri")
assert file("student-management-organization-starter/src/main/resources/application-prod.yml").text
        .contains("name: tri")
def testApplication = file("student-management-organization-starter/src/main/resources/application-test.yml").text
assert testApplication.contains("machine-id: 0")
assert testApplication.contains("enabled: false")
assert testApplication.contains("name: injvm")
assert file("deploy/env/.env.example").text.contains("NACOS_IMAGE=nacos/nacos-server:v3.0.3")
assert file("deploy/env/.env.example").text.contains("EGON_ID_MACHINE_ID=2")
[
        "compose.docker.yaml", "compose.podman.yaml", "compose.nerdctl.yaml",
        "compose.docker.prod.yaml", "compose.podman.prod.yaml", "compose.nerdctl.prod.yaml"
].each { name ->
    def compose = file("deploy/compose/${name}").text
    assert compose.contains("EGON_ID_MACHINE_ID")
    assert compose.contains("DTP_ENABLED")
    assert compose.contains("NACOS_SERVER_ADDR: nacos:8848")
    assert compose.contains("DUBBO_REGISTRY_ADDRESS: nacos://nacos:8848")
}

def reports = filesUnder(".") { candidate ->
    candidate.path.replace('\\', '/').contains('/target/surefire-reports/') && candidate.name.endsWith('.xml')
}
[
        "ProtoContractTest",
        "GrpcEvaluationQueryClientTest",
        "OrganizationDubboProviderConfigurationTest",
        "OrganizationApplicationTest",
        "OrganizationExternalFreeContextTest",
        "OpenApiContractTest",
        "OpenArchitectureTest"
].each { testName ->
    def report = reports.find { it.name.contains(testName) }
    assert report && report.text.contains('failures="0"') && report.text.contains('errors="0"'):
            "Expected generated Web test report ${testName} to pass"
}

assert file("README.md").text.contains("Springdoc")
assert file("README.md").text.contains("MyBatis-Plus")
assert file("README.md").text.contains("Gateway")
