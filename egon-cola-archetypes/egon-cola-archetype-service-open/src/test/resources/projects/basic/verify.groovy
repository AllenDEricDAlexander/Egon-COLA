import groovy.io.FileType
import groovy.xml.XmlSlurper

def projectDir = [
        new File(basedir, "project/student-management-evaluation"),
        new File(basedir, "student-management-evaluation"),
        basedir
].find { candidate -> new File(candidate, "student-management-evaluation-starter").isDirectory() }
assert projectDir != null: "Expected generated Service Open project"

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

def moduleNames = [
        "student-management-evaluation-common",
        "student-management-evaluation-facade",
        "student-management-evaluation-domain",
        "student-management-evaluation-application",
        "student-management-evaluation-infrastructure",
        "student-management-evaluation-adapter",
        "student-management-evaluation-starter"
]
def rootPomFile = file("pom.xml")
def rootPom = new XmlSlurper(false, false).parse(rootPomFile)
assert rootPom.modules.module*.text() == moduleNames
assert rootPom.parent.version.text() == "3.5.16"
assert rootPom.properties.'java.version'.text() == "21"
assert rootPom.properties.'dubbo.version'.text() == "3.3.6"
assert rootPom.properties.'grpc.version'.text() == "1.73.0"
assert rootPom.properties.'spring-cloud.version'.text() == "2025.0.3"
assert rootPom.properties.'spring-cloud-alibaba.version'.text() == "2025.0.0.0"
assert rootPom.properties.'shardingsphere.version'.text() == "5.5.3"
assert !rootPom.properties.'mybatis-plus.version'.text()
assert rootPom.properties.'archunit.version'.text() == "1.4.2"
moduleNames.each { name -> file("${name}/pom.xml") }

def runtimeFiles = filesUnder(".") { candidate ->
    def path = candidate.path.replace('\\', '/')
    path.contains('/src/main/') || candidate.name == 'pom.xml'
}
def runtimeText = runtimeFiles.collect { it.getText("UTF-8") }.join("\n")
[
        "springdoc-openapi",
        "org.springdoc",
        "spring-boot-starter-gateway",
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
        ,"mybatis-plus-spring-boot3-starter"
        ,"BaseMapper"
        ,"repo.mapper"
].each { token ->
    assert !runtimeText.contains(token): "Forbidden Service Open runtime token ${token}"
}
assert !runtimeText.contains("@RestController")
assert !runtimeText.contains("@Controller")

def protoRoot = new File(projectDir, "student-management-evaluation-facade/src/main/proto")
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
        "student-management-evaluation-facade/src/test/java/it/pkg/facade/contract/ProtoDescriptorContractTest.java",
        "student-management-evaluation-adapter/src/main/java/it/pkg/adapter/course/facade/impl/CourseFacadeImpl.java",
        "student-management-evaluation-adapter/src/main/java/it/pkg/adapter/exam/facade/impl/ExamFacadeImpl.java",
        "student-management-evaluation-adapter/src/main/java/it/pkg/adapter/exam/facade/impl/ScoreFacadeImpl.java",
        "student-management-evaluation-adapter/src/test/java/it/pkg/adapter/rpc/EvaluationDubboTripleIntegrationTest.java",
        "student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/client/organization/DubboOrganizationDirectoryClient.java",
        "student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/client/organization/LocalOrganizationDirectoryStub.java",
        "student-management-evaluation-starter/src/test/java/it/pkg/starter/EvaluationServiceApplicationTest.java",
        "student-management-evaluation-starter/src/test/java/it/pkg/starter/EvaluationExternalFreeContextTest.java",
        "student-management-evaluation-starter/src/test/java/it/pkg/architecture/OpenArchitectureTest.java"
].each { path -> file(path) }

def mapperFiles = filesUnder("student-management-evaluation-infrastructure/src/main/resources/mybatis/mapper") {
    it.name.endsWith("DAO.xml")
}
assert mapperFiles.size() == 5
[
        "student-management-evaluation-infrastructure/src/main/resources/db/manual/postgresql/README.md",
        "student-management-evaluation-infrastructure/src/main/resources/db/manual/postgresql/master-data/001__create_evaluation_master_data_schema.sql",
        "student-management-evaluation-infrastructure/src/main/resources/db/manual/postgresql/shard/002__create_evaluation_sharded_schema.sql",
        "student-management-evaluation-infrastructure/src/main/resources/db/manual/postgresql/master-data/003__migrate_evaluation_master_data_to_egon_model.sql",
        "student-management-evaluation-infrastructure/src/main/resources/db/manual/postgresql/shard/004__migrate_evaluation_sharded_to_tenant_model.sql"
].each { path -> file(path) }
assert file("student-management-evaluation-infrastructure/src/main/resources/db/manual/postgresql/master-data/001__create_evaluation_master_data_schema.sql").text.contains("BIGINT")
assert file("student-management-evaluation-infrastructure/src/main/resources/db/manual/postgresql/shard/002__create_evaluation_sharded_schema.sql").text.contains("BIGINT")
assert file("student-management-evaluation-infrastructure/src/main/resources/db/manual/postgresql/master-data/003__migrate_evaluation_master_data_to_egon_model.sql").text
        .contains("evaluation_course")
assert file("student-management-evaluation-infrastructure/src/main/resources/db/manual/postgresql/shard/004__migrate_evaluation_sharded_to_tenant_model.sql").text
        .contains("tenant_id")
missing("student-management-evaluation-infrastructure/src/main/resources/db/migration")
missing("student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/course/repo/jpa")
missing("student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/exam/repo/jpa")
missing("student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/course/repo/mapper")
missing("student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/exam/repo/mapper")
missing("student-management-evaluation-domain/src/main/java/it/pkg/domain/course/service/impl")
missing("student-management-evaluation-domain/src/main/java/it/pkg/domain/exam/service/impl")

def starterPom = file("student-management-evaluation-starter/pom.xml").text
assert starterPom.contains("egon-cola-component-dynamic-thread-pool-starter")
assert starterPom.contains("archunit-junit5")
assert file("student-management-evaluation-domain/pom.xml").text
        .contains("egon-cola-component-common-mybatis-plus-spring-boot-starter")
assert file("student-management-evaluation-starter/src/main/resources/application.yml").text
        .contains("queue-capacity: \${ASYNC_QUEUE_CAPACITY:1000}")
assert file("student-management-evaluation-starter/src/main/resources/application.yml").text
        .contains("machine-id: \${EGON_ID_MACHINE_ID}")
assert file("student-management-evaluation-starter/src/main/resources/application-test.yml").text
        .contains("machine-id: 0")
assert file("student-management-evaluation-starter/src/main/resources/application-test.yml").text
        .contains("enabled: false")
assert file("deploy/env/.env.example").text.contains("NACOS_IMAGE=nacos/nacos-server:v3.0.3")
[
        "compose.docker.yaml", "compose.podman.yaml", "compose.nerdctl.yaml",
        "compose.docker.prod.yaml", "compose.podman.prod.yaml", "compose.nerdctl.prod.yaml"
].each { name ->
    def compose = file("deploy/compose/${name}").text
    assert compose.contains("EGON_ID_MACHINE_ID")
    assert compose.contains("DTP_ENABLED")
    assert compose.contains("DUBBO_REGISTRY_ADDRESS")
}

def reports = filesUnder(".") { candidate ->
    candidate.path.replace('\\', '/').contains('/target/surefire-reports/') && candidate.name.endsWith('.xml')
}
[
        "ProtoDescriptorContractTest",
        "EvaluationDubboTripleIntegrationTest",
        "EvaluationServiceApplicationTest",
        "EvaluationExternalFreeContextTest",
        "OpenArchitectureTest"
].each { testName ->
    def report = reports.find { it.name.contains(testName) }
    assert report && report.text.contains('failures="0"') && report.text.contains('errors="0"'):
            "Expected generated Service test report ${testName} to pass"
}

def javaFiles = []
projectDir.traverse(type: FileType.FILES) { candidate ->
    def relativePath = projectDir.toPath().relativize(candidate.toPath()).toString()
            .replace(File.separator, "/")
    if (relativePath.contains("/src/main/java/") && candidate.name.endsWith(".java")) {
        javaFiles << candidate
    }
}
def javaPath = { File candidate ->
    projectDir.toPath().relativize(candidate.toPath()).toString().replace(File.separator, "/")
}
def persistencePoSources = javaFiles.findAll { candidate ->
    def path = javaPath(candidate)
    path.contains("/infrastructure/") && path.contains("/repo/po/")
            && candidate.name.endsWith("PO.java")
}
assert persistencePoSources.size() == 5
persistencePoSources.each { candidate ->
    def source = candidate.text
    ["@Data", "@NoArgsConstructor", "@AllArgsConstructor", "@Builder",
     "@Accessors(chain = true)", "@TableName", "extends EgonModel<"].each { token ->
        assert source.contains(token): "Expected ${candidate.name} to contain ${token}"
    }
    assert !source.contains("@RequiredArgsConstructor")
    assert !source.contains("@SuperBuilder")
}
def daoSources = javaFiles.findAll { candidate ->
    def path = javaPath(candidate)
    path.contains("/infrastructure/") && path.contains("/repo/dao/")
            && candidate.name.endsWith("DAO.java")
}
assert daoSources.size() == 5
daoSources.each { candidate ->
    assert candidate.text.contains("extends EgonColaMapper<"):
            "DAO must extend EgonColaMapper: ${candidate.name}"
}
def domainServiceSources = javaFiles.findAll { candidate ->
    def path = javaPath(candidate)
    path.contains("/domain/") && path.contains("/service/")
            && candidate.name.endsWith("DomainService.java")
}
assert domainServiceSources.size() == 3
domainServiceSources.each { candidate ->
    assert candidate.text.contains("extends EgonColaIService<"):
            "Domain service must extend EgonColaIService: ${candidate.name}"
}
def infrastructureServiceSources = javaFiles.findAll { candidate ->
    def path = javaPath(candidate)
    path.contains("/infrastructure/") && path.contains("/service/impl/")
            && candidate.name.endsWith("DomainServiceImpl.java")
}
assert infrastructureServiceSources.size() == 3
infrastructureServiceSources.each { candidate ->
    assert candidate.text.contains("extends EgonColaServiceImpl<"):
            "Infrastructure service must extend EgonColaServiceImpl: ${candidate.name}"
}
javaFiles.each { candidate ->
    assert !candidate.text.contains("extends BaseMapper<")
    assert !candidate.text.contains("repo.mapper")
}

assert file("README.md").text.contains("MyBatis-Plus")
assert file("README.md").text.contains("Proto")
assert file("README.md").text.contains("Springdoc")
