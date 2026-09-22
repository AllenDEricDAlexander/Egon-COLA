import groovy.io.FileType
import groovy.xml.XmlSlurper

def projectDir = new File(basedir, "pom.xml").isFile() ? basedir : context.projectDir
assert projectDir != null: "Expected generated Web project"

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
def relativePath = { File candidate ->
    projectDir.toPath().relativize(candidate.toPath()).toString().replace(File.separator, "/")
}
def sourceFiles = { String root ->
    def result = []
    directory(root).traverse(type: FileType.FILES) { candidate ->
        if (candidate.name.endsWith(".java")) result << candidate
    }
    result
}

def prefix = "student-management-organization"
def modules = ["common", "facade", "domain", "application", "infrastructure", "adapter", "starter"]
def moduleNames = modules.collect { "${prefix}-${it}" }
def rootPom = new XmlSlurper(false, false).parse(file("pom.xml"))
assert rootPom.modules.module*.text() == moduleNames
assert rootPom.properties.'java.version'.text() == "21"
assert rootPom.properties.'egon-cola.version'.text()
assert rootPom.properties.'lombok.version'.text() == "1.18.46"
assert rootPom.properties.'evaluation-facade.group-id'.text() == "top.egon.internal.archetype.source"
assert rootPom.properties.'evaluation-facade.artifact-id'.text() == "egon-cola-source-service-facade"
assert rootPom.properties.'evaluation-facade.version'.text() == "0.1.0-SNAPSHOT"
assert rootPom.properties.'evaluation-facade.package'.text() == "top.egon.cola.archetype.source.service.facade"
["group-id", "artifact-id", "version", "package"].each { part ->
    assert !rootPom.properties."organization-facade.${part}".text():
            "The own contract is a reactor module and must not stay an external Facade artifact: organization-facade.${part}"
}
def rootPomText = file("pom.xml").getText("UTF-8")
assert !rootPomText.contains("spring-boot-starter-data-jpa")
assert !rootPomText.contains("mybatis-plus.version")
assert rootPomText.contains("<artifactId>lombok-mapstruct-binding</artifactId>")
moduleNames.each { file("${it}/pom.xml") }

modules.each { module ->
    def moduleSources = module == "facade"
            ? ["src/main/java", "src/main/proto"]
            : ["src/main/java", "src/main/resources", "src/test/java", "src/test/resources"]
    moduleSources.each { source ->
        directory("${prefix}-${module}/${source}")
    }
}
missing("${prefix}-client")
missing("${prefix}-app")
missing("${prefix}-facade/src/main/resources")
missing("${prefix}-facade/src/test/java")
missing("${prefix}-facade/src/test/resources")
missing("${prefix}-domain/src/main/java/domain")
missing("${prefix}-domain/src/main/java/${prefix}")
missing("${prefix}-infrastructure/src/main/java/infrastructure/repo")
missing("${prefix}-infrastructure/src/main/java/infrastructure/user/repo/jpa")
missing("${prefix}-infrastructure/src/main/java/infrastructure/teaching/repo/jpa")
missing("${prefix}-infrastructure/src/main/java/infrastructure/user/repo/mapper")
missing("${prefix}-infrastructure/src/main/java/infrastructure/teaching/repo/mapper")
missing("${prefix}-domain/src/main/java/domain/user/service/impl")
missing("${prefix}-domain/src/main/java/domain/teaching/service/impl")

// The converged tree is domain-first: carriers live under pojo/, the persistence roles sit beside
// the Repository, and the retired technical-first paths must not come back through generation.
def requiredPackagePaths = [
    "common/constants", "common/enums", "common/exception", "common/utils",
    "facade/user", "facade/teaching",
    "domain/events", "domain/service", "domain/validators",
    "application/config", "application/context", "application/support",
    "infrastructure/aop", "infrastructure/cache", "infrastructure/client/evaluation",
    "infrastructure/client/evaluation/impl", "infrastructure/config", "infrastructure/mq",
    "infrastructure/mq/impl", "infrastructure/service/impl",
    "adapter/config", "adapter/facade/impl", "adapter/filter", "adapter/graphql",
    "adapter/handler", "adapter/mq", "adapter/pojo/convertor", "adapter/pojo/dto",
    "starter", "starter/config", "starter/config/async", "starter/config/encryption"
]
["teaching", "user"].each { businessDomain ->
    ["aggregates", "entities", "enums", "events", "service", "validators", "vos"].each { role ->
        requiredPackagePaths << "domain/${businessDomain}/${role}"
    }
    ["manage", "manage/impl", "pojo/command", "pojo/convertor", "pojo/query", "pojo/result",
     "validators"].each { role ->
        requiredPackagePaths << "application/${businessDomain}/${role}"
    }
    ["converter", "dao", "po", "repo", "service/impl"].each { role ->
        requiredPackagePaths << "infrastructure/${businessDomain}/${role}"
    }
    ["controller", "facade/impl", "graphql", "mq", "pojo/convertor", "pojo/dto", "pojo/vo",
     "rpc"].each { role ->
        requiredPackagePaths << "adapter/${businessDomain}/${role}"
    }
}
requiredPackagePaths.each { packagePath ->
    def module = packagePath.contains('/') ? packagePath.substring(0, packagePath.indexOf('/')) : packagePath
    file("${prefix}-${module}/src/main/java/it/pkg/${packagePath}/package-info.java")
}
[
    "common/exceptions",
    "domain/client", "domain/gateway", "domain/exceptions", "domain/teaching/client",
    "domain/teaching/cache", "domain/user/client", "domain/user/cache",
    "domain/teaching/service/impl", "domain/user/service/impl",
    "application/exceptions", "application/teaching/assemblers", "application/user/assemblers",
    "application/teaching/command", "application/teaching/query", "application/teaching/result",
    "application/teaching/converter", "application/user/command", "application/user/query",
    "application/user/result", "application/user/converter",
    "adapter/exceptions", "adapter/teaching/dto", "adapter/teaching/vo", "adapter/teaching/converter",
    "adapter/user/dto", "adapter/user/vo", "adapter/user/converter",
    "infrastructure/exceptions", "infrastructure/repo",
    "infrastructure/teaching/cache", "infrastructure/user/cache",
    "infrastructure/teaching/repo/dao", "infrastructure/teaching/repo/po",
    "infrastructure/teaching/repo/converter", "infrastructure/user/repo/dao",
    "infrastructure/user/repo/po", "infrastructure/user/repo/converter",
    "infrastructure/cache/RedisCommandIdempotencyAdapter.java",
    "infrastructure/cache/InMemoryCommandIdempotencyAdapter.java",
    "infrastructure/mq/RabbitOrganizationEventPublisher.java",
    "infrastructure/mq/LocalOrganizationEventPublisher.java",
    "infrastructure/client/evaluation/LocalEvaluationQueryStub.java",
    "infrastructure/client/evaluation/NativeEvaluationQueryClient.java",
    "starter/config/encryption/ConfigDecryptException.java"
].collect { "it/pkg/" + it }.each { path ->
    def module = path.substring("it/pkg/".length()).split('/')[0]
    missing("${prefix}-${module}/src/main/java/${path}")
}

def expectedFiles = [
    "${prefix}-facade/src/main/java/it/pkg/facade/user/UserFacade.java",
    "${prefix}-facade/src/main/java/it/pkg/facade/user/RoleFacade.java",
    "${prefix}-facade/src/main/java/it/pkg/facade/user/PermissionFacade.java",
    "${prefix}-facade/src/main/java/it/pkg/facade/teaching/GradeFacade.java",
    "${prefix}-facade/src/main/java/it/pkg/facade/teaching/SchoolClassFacade.java",
    "${prefix}-facade/src/main/proto/organization_facade.proto",
    "${prefix}-adapter/src/main/java/it/pkg/adapter/user/facade/impl/UserFacadeImpl.java",
    "${prefix}-adapter/src/main/java/it/pkg/adapter/user/facade/impl/RoleFacadeImpl.java",
    "${prefix}-adapter/src/main/java/it/pkg/adapter/user/facade/impl/PermissionFacadeImpl.java",
    "${prefix}-adapter/src/main/java/it/pkg/adapter/teaching/facade/impl/GradeFacadeImpl.java",
    "${prefix}-adapter/src/main/java/it/pkg/adapter/teaching/facade/impl/SchoolClassFacadeImpl.java",
    "${prefix}-adapter/src/main/java/it/pkg/adapter/pojo/convertor/OrganizationFacadeConverter.java",
    "${prefix}-starter/src/test/java/it/pkg/contract/OwnedFacadeContractTest.java",
    "${prefix}-domain/pom.xml",
    "${prefix}-domain/src/main/java/it/pkg/domain/user/service/UserDomainService.java",
    "${prefix}-domain/src/main/java/it/pkg/domain/user/service/PermissionDomainService.java",
    "${prefix}-domain/src/main/java/it/pkg/domain/teaching/service/GradeDomainService.java",
    "${prefix}-domain/src/main/java/it/pkg/domain/teaching/service/SchoolClassDomainService.java",
    "${prefix}-infrastructure/src/main/java/it/pkg/infrastructure/user/service/impl/UserDomainServiceImpl.java",
    "${prefix}-infrastructure/src/main/java/it/pkg/infrastructure/user/service/impl/PermissionDomainServiceImpl.java",
    "${prefix}-infrastructure/src/main/java/it/pkg/infrastructure/teaching/service/impl/GradeDomainServiceImpl.java",
    "${prefix}-infrastructure/src/main/java/it/pkg/infrastructure/teaching/service/impl/SchoolClassDomainServiceImpl.java",
    "${prefix}-starter/src/main/java/it/pkg/starter/OrganizationApplication.java",
    "${prefix}-starter/src/test/java/it/pkg/architecture/WebArchitectureTest.java",
    "${prefix}-adapter/src/main/java/it/pkg/adapter/user/controller/UserController.java",
    "${prefix}-adapter/src/main/java/it/pkg/adapter/teaching/controller/GradeController.java",
    "${prefix}-adapter/src/main/resources/graphql/schema.graphqls",
    "${prefix}-infrastructure/src/main/resources/mybatis/mapper/user/UserDAO.xml",
    "${prefix}-infrastructure/src/main/resources/mybatis/mapper/teaching/GradeDAO.xml",
    "${prefix}-infrastructure/src/main/resources/db/migration/sharding/master-data/V20260825_003__migrate_organization_master_data_to_egon_model.sql",
    "${prefix}-infrastructure/src/main/resources/db/migration/sharding/shard/V20260825_004__migrate_organization_sharded_to_tenant_model.sql",
    "${prefix}-common/src/main/java/it/pkg/common/exception/BizException.java",
    "${prefix}-common/src/main/java/it/pkg/common/enums/OrganizationFailureType.java",
    "${prefix}-domain/src/main/java/it/pkg/domain/service/CommandIdempotencyService.java",
    "${prefix}-domain/src/main/java/it/pkg/domain/service/OrganizationEventService.java",
    "${prefix}-domain/src/main/java/it/pkg/domain/teaching/service/EvaluationQueryService.java",
    "${prefix}-application/src/main/java/it/pkg/application/support/IdempotentCommand.java",
    "${prefix}-application/src/main/java/it/pkg/application/teaching/pojo/command/CreateGradeCommand.java",
    "${prefix}-application/src/main/java/it/pkg/application/user/pojo/result/UserDetailResult.java",
    "${prefix}-adapter/src/main/java/it/pkg/adapter/user/pojo/dto/CreateUserRequest.java",
    "${prefix}-adapter/src/main/java/it/pkg/adapter/teaching/pojo/vo/GradeDetailVO.java",
    "${prefix}-infrastructure/src/main/java/it/pkg/infrastructure/teaching/dao/GradeDAO.java",
    "${prefix}-infrastructure/src/main/java/it/pkg/infrastructure/teaching/po/GradePO.java",
    "${prefix}-infrastructure/src/main/java/it/pkg/infrastructure/teaching/repo/GradeRepository.java",
    "${prefix}-infrastructure/src/main/java/it/pkg/infrastructure/mq/MqRouteEnum.java",
    "${prefix}-infrastructure/src/main/java/it/pkg/infrastructure/mq/impl/RabbitMqMessageServiceImpl.java",
    "${prefix}-infrastructure/src/main/java/it/pkg/infrastructure/service/impl/CommandIdempotencyServiceImpl.java",
    "${prefix}-infrastructure/src/main/java/it/pkg/infrastructure/client/evaluation/impl/NativeEvaluationQueryClientImpl.java"
]
expectedFiles.each { file(it) }

def poms = [:]
modules.each { module -> poms[module] = new XmlSlurper(false, false).parse(file("${prefix}-${module}/pom.xml")) }
def dependencyIds = { pom -> pom.dependencies.dependency.artifactId*.text() as Set }
assert !dependencyIds(poms.domain).contains("egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter")
assert dependencyIds(poms.infrastructure).contains("${prefix}-domain".toString())
assert dependencyIds(poms.infrastructure).contains("egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter")
assert !dependencyIds(poms.infrastructure).contains("flyway-core")
assert !dependencyIds(poms.infrastructure).contains("spring-boot-starter-data-jpa")
assert !dependencyIds(poms.infrastructure).contains("mybatis-plus-spring-boot3-starter")
// The own contract is protocol-only: no internal module, no published peer artifact.
assert ["egon-cola-component-rpc-starter", "protobuf-java", "grpc-protobuf", "grpc-stub",
        "jakarta.validation-api"].every { dependencyIds(poms.facade).contains(it) }
assert !dependencyIds(poms.facade).any { it.startsWith(prefix) }:
        "The own Facade module must stay free of internal-module dependencies"
assert dependencyIds(poms.adapter).containsAll(["${prefix}-application".toString(), "${prefix}-facade".toString()])
assert dependencyIds(poms.infrastructure).contains('${evaluation-facade.artifact-id}'):
        "Infrastructure must consume the published peer Evaluation Facade"
assert !dependencyIds(poms.infrastructure).contains("${prefix}-facade".toString()):
        "Infrastructure must not depend on the project's own contract"
modules.findAll { it != "infrastructure" }.each { module ->
    assert !dependencyIds(poms[module]).contains('${evaluation-facade.artifact-id}'):
            "Unexpected peer Facade dependency in ${module}"
}

def javaSources = []
modules.each { module -> javaSources.addAll(sourceFiles("${prefix}-${module}/src/main/java")) }
def runtimeText = javaSources.collect { it.getText("UTF-8") }.join("\n")
["spring-boot-starter-data-jpa", "jakarta.persistence", "JpaRepository", "@Entity", "@MappedSuperclass",
 "UuidV7Generator", "extends BaseMapper<", "repo.mapper"].each { token ->
    assert !runtimeText.contains(token): "Forbidden Web runtime token ${token}"
}

// The published contract is interfaces plus Protobuf only; implementations stay in the Adapter.
def facadeJavaFiles = sourceFiles("${prefix}-facade/src/main/java")
assert facadeJavaFiles.every { it.name.endsWith("Facade.java") || it.name == "package-info.java" }:
        "The contract module holds only contract interfaces: ${facadeJavaFiles*.name}"
assert facadeJavaFiles.findAll { it.name.endsWith("Facade.java") }*.name.toSet() == [
        "UserFacade.java", "RoleFacade.java", "PermissionFacade.java",
        "GradeFacade.java", "SchoolClassFacade.java"
].toSet(): "The generated own contract changed: ${facadeJavaFiles*.name}"
facadeJavaFiles.findAll { it.name.endsWith("Facade.java") }.each { candidate ->
    def source = candidate.getText("UTF-8")
    assert source.contains("public interface "): "Contract must be an interface: ${relativePath(candidate)}"
    assert !source.contains("@Component") && !source.contains("@EgonRpcProvider")
    assert !source.contains("import it.pkg.adapter.") && !source.contains("import it.pkg.infrastructure.")
}

def poSources = javaSources.findAll {
    def path = relativePath(it)
    path.contains("/infrastructure/") && path.contains("/po/") && it.name.endsWith("PO.java")
}
assert poSources.size() == 8: "Expected eight infrastructure PO classes, got ${poSources.size()}"
poSources.each { po ->
    def source = po.getText("UTF-8")
    ["@Data", "@NoArgsConstructor", "@AllArgsConstructor", "@Builder", "@Accessors(chain = true)",
     "@TableName", "extends EgonModel<"].each { token ->
        assert source.contains(token): "${relativePath(po)} must contain ${token}"
    }
    assert !source.contains("@RequiredArgsConstructor")
    assert !source.contains("@SuperBuilder")
}

def daoSources = javaSources.findAll {
    def path = relativePath(it)
    path.contains("/infrastructure/") && path.contains("/dao/") && it.name.endsWith("DAO.java")
}
assert daoSources.size() == 8: "Expected eight infrastructure DAO classes, got ${daoSources.size()}"
daoSources.each { dao ->
    assert dao.text.contains("@Mapper")
    assert dao.text.contains("extends EgonColaMapper<")
}

def domainServices = javaSources.findAll {
    def path = relativePath(it)
    path.contains("/domain/") && path.contains("/service/") && it.name.endsWith("DomainService.java")
}
assert domainServices.size() == 4
domainServices.each { service ->
    assert !service.text.contains("EgonColaIService"):
            "Domain service must expose a domain-only port: ${relativePath(service)}"
}
def serviceImpls = javaSources.findAll {
    def path = relativePath(it)
    path.contains("/infrastructure/") && path.contains("/service/impl/") && it.name.endsWith("DomainServiceImpl.java")
}
assert serviceImpls.size() == 4
serviceImpls.each { implementation ->
    assert !implementation.text.contains("extends EgonColaServiceImpl<"):
            "Infrastructure service must compose repositories: ${relativePath(implementation)}"
    assert implementation.text.contains("@RequiredArgsConstructor")
}

def mapperXmls = []
def mapperRoot = directory("${prefix}-infrastructure/src/main/resources/mybatis/mapper")
mapperRoot.traverse(type: FileType.FILES) { candidate -> if (candidate.name.endsWith("DAO.xml")) mapperXmls << candidate }
assert mapperXmls.size() == 8
mapperXmls.each { xml ->
    assert xml.text.contains("namespace=\"it.pkg.infrastructure.")
    assert xml.text.contains("DAO\"")
    assert xml.text.contains("tenant_id")
}

// Contract traffic boundaries: the published peer Evaluation Facade is an Infrastructure-client
// detail, while the project's own contract may only be consumed by its Adapter providers.
def runtimeJava = []
projectDir.traverse(type: FileType.FILES) { candidate ->
    if (candidate.isFile() && candidate.name.endsWith(".java")) {
        def path = relativePath(candidate)
        if (path.contains("/src/main/java/") && !path.contains("/target/")) runtimeJava << candidate
    }
}
def peerFacadeImports = runtimeJava.findAll {
    it.getText("UTF-8").contains("import top.egon.cola.archetype.source.service.facade.")
}
assert peerFacadeImports: "Infrastructure must consume the published peer Evaluation Facade"
assert peerFacadeImports.every { relativePath(it).contains("/infrastructure/client/evaluation/") }:
        "Peer Evaluation Facade imports escaped Infrastructure client: ${peerFacadeImports.collect { relativePath(it) }}"
def ownFacadeImports = runtimeJava.findAll { it.getText("UTF-8").contains("import it.pkg.facade.") }
assert ownFacadeImports: "The generated Adapter must consume the project's own Facade module"
assert ownFacadeImports.every {
    def path = relativePath(it)
    path.startsWith("${prefix}-facade/src/") || path.startsWith("${prefix}-adapter/src/")
}: "Own Facade contract escaped its module and the Adapter: ${ownFacadeImports.collect { relativePath(it) }}"
def localEvaluationClient = file("${prefix}-infrastructure/src/main/java/it/pkg/infrastructure/client/evaluation/impl/LocalEvaluationQueryClientImpl.java").text
assert !localEvaluationClient.contains("top.egon.cola.archetype.source.service.facade"):
        "The test-profile client must stay free of the published peer contract"
assert localEvaluationClient.contains('@Profile("test")')
assert localEvaluationClient.contains('@Component("evaluationQueryClient")')
[
    "top.egon.cola.evaluation.facade", "top.egon.cola.organization.facade",
    "EvaluationRpcConverter", "OrganizationRpcConverter",
    "UserRpcService", "GradeRpcService", "RoleRpcService", "PermissionRpcService",
    "SchoolClassRpcService", "CourseRpcService", "ExamRpcService", "ScoreRpcService",
    "RpcSchoolClassQuery",
    "UserCachePort", "GradeCachePort", "SchoolClassCachePort", "CachePort",
    "RedisUserCache", "InMemoryUserCache", "RedisGradeCache", "InMemoryGradeCache",
    "RedisSchoolClassCache", "InMemorySchoolClassCache", "RedisCommandIdempotencyAdapter",
    "InMemoryCommandIdempotencyAdapter", "RabbitOrganizationEventPublisher",
    "LocalOrganizationEventPublisher", "LocalEvaluationQueryStub", "QueryGateway",
    "repo.dao", "repo.po", "repo.converter", "it.pkg.domain.client", "it.pkg.domain.gateway",
    "it.pkg.application.exceptions", "it.pkg.common.exceptions", "Assemblers", "Assembler"
].each { token ->
    runtimeJava.each { candidate ->
        assert !candidate.getText("UTF-8").contains(token):
                "Retired shared Facade reference ${token} in ${relativePath(candidate)}"
    }
}
def frozenWire = file("${prefix}-facade/src/main/proto/organization_facade.proto").text
assert frozenWire.contains("package top.egon.cola.organization.facade.rpc.v1;")
assert frozenWire.contains('option java_package = "it.pkg.facade.proto";')
def facadeConverter = file("${prefix}-adapter/src/main/java/it/pkg/adapter/pojo/convertor/OrganizationFacadeConverter.java").text
assert facadeConverter.contains("@Mapper(")
assert facadeConverter.contains("extends BaseConverter<")
assert facadeConverter.contains("@BeanMapping(ignoreByDefault = true)")
assert facadeConverter.contains("@AfterMapping")
assert facadeConverter.contains("import it.pkg.facade.proto.")

def application = file("${prefix}-starter/src/main/java/it/pkg/starter/OrganizationApplication.java").text
assert application.contains("@MapperScan")
assert application.contains("@EnableConfigurationProperties(EgonColaMybatisPlusProperties.class)")
// Step 2 staticizes ID issuance: the starter configuration initializes the static generator and
// the runtime calls it, so the Application class must not hand-wire an ID bean.
assert file("${prefix}-starter/src/main/resources/application.yml").text
        .contains('machine-id: ${EGON_ID_MACHINE_ID}')
assert !application.contains("LongIdGenerator")
assert runtimeText.contains("SnowflakeIdGenerator.nextLongId()")
assert !application.contains("@EntityScan")
assert !application.contains("@EnableJpaRepositories")
def applicationYaml = file("${prefix}-starter/src/main/resources/application.yml").text
assert applicationYaml.contains("egon:")
assert applicationYaml.contains("mybatis-plus:")
assert applicationYaml.contains("mapper-locations: classpath*:mybatis/mapper/**/*.xml")
assert applicationYaml.contains("tenant-id:")
assert applicationYaml.contains("mdc-key: tenantId")
assert applicationYaml.contains("user-id-mdc-key: userId")
assert applicationYaml.contains("sql:") && applicationYaml.contains("mode: never")

assert applicationYaml.contains('classpath:egon-mybatis-plus-sharding.yml')
// The MP starter owns the topology: one STRATEGY document, no node-map bootstrap and no raw ShardingSphere YAML.
def sharding = file("${prefix}-starter/src/main/resources/egon-mybatis-plus-sharding.yml").text
assert sharding.contains('mode: ${APP_DATASOURCE_MODE:SHARDING}')
assert sharding.contains('config-style: STRATEGY')
assert sharding.contains('transaction-default-type: LOCAL')
[
    'jdbc-url: ${ORGANIZATION_SHARDING_MASTER_DATA_URL}',
    'jdbc-url: ${ORGANIZATION_SHARDING_SHARD_0_URL}',
    'jdbc-url: ${ORGANIZATION_SHARDING_SHARD_1_URL}'
].each { assert sharding.contains(it) }
assert sharding.count('role: PRIMARY') == 3
assert sharding.contains('data-source: master_data')
assert sharding.contains('type: STANDARD_TENANT_ID')
assert sharding.contains('sharding-column: tenant_id')
def webRepositoryManifest = file("${prefix}-infrastructure/src/main/resources/db/egon-mp/repository-manifest.json").text
assert webRepositoryManifest.contains('"family": "web"')
assert webRepositoryManifest.contains("V20260913_001__initialize_repository_schema.sql")
assert webRepositoryManifest.contains('"sha256"')
assert !sharding.contains('mapping-version')
assert !sharding.contains('node-count')
assert !sharding.contains('node-map')
assert !sharding.contains('LongTenantShardingAlgorithm')
assert !sharding.contains('UuidV7')
assert !sharding.contains('uuid_v7')
assert !sharding.contains('.public.')
assert !sharding.contains('proxy-frontend-database-protocol-type')
assert !sharding.contains('defaultDataSource')
assert !sharding.contains('transactionalReadQueryStrategy')
missing("${prefix}-starter/src/main/resources/sharding/shardingsphere-sharding.yml")
missing("${prefix}-starter/src/main/resources/sharding/shardingsphere-sharding-readwrite.yml")
missing("${prefix}-starter/src/main/resources/datasource/sharding.yml")
missing("${prefix}-starter/src/main/resources/datasource/sharding-readwrite.yml")

def migrationRoot = directory("${prefix}-infrastructure/src/main/resources/db/migration")
def migrationPaths = []
migrationRoot.traverse(type: FileType.FILES) { candidate ->
    if (candidate.name.endsWith(".sql")) migrationPaths << migrationRoot.toPath().relativize(candidate.toPath()).toString().replace(File.separator, "/")
}
assert migrationPaths.sort() == [
    "sharding/master-data/B20260825_003__baseline_organization_master_data_schema.sql",
    "sharding/master-data/V20260726_001__init_organization_master_data_schema.sql",
    "sharding/master-data/V20260825_003__migrate_organization_master_data_to_egon_model.sql",
    "sharding/shard/B20260825_004__baseline_organization_sharded_schema.sql",
    "sharding/shard/V20260726_002__init_organization_sharded_schema.sql",
    "sharding/shard/V20260825_004__migrate_organization_sharded_to_tenant_model.sql"
]
migrationPaths.each { path ->
    def migration = file("${prefix}-infrastructure/src/main/resources/db/migration/${path}").text
    assert migration.startsWith("-- 变更内容：")
    assert migration.contains("\n-- 影响范围：")
    assert migration.contains("\n-- 兼容性说明：")
}
assert file("${prefix}-infrastructure/src/main/resources/db/migration/sharding/master-data/V20260825_003__migrate_organization_master_data_to_egon_model.sql").text.contains("tenant_id BIGINT")
assert file("${prefix}-infrastructure/src/main/resources/db/migration/sharding/shard/V20260825_004__migrate_organization_sharded_to_tenant_model.sql").text.contains("tenant_id BIGINT")

def readme = file("README.md").text
assert readme.contains("MyBatis-Plus")
assert readme.contains("tenant_id")
assert readme.contains("Long")
assert readme.contains("clean verify")
assert !readme.contains("Spring Data JPA")
assert !readme.contains("UUIDv7")
assert readme.contains("The Organization contract is published by this project itself")
["evaluation-facade.group-id", "evaluation-facade.artifact-id",
 "evaluation-facade.version", "evaluation-facade.package"].each {
    assert readme.contains(it): "The generated Web README must document the peer Facade property ${it}"
}
def webReadmeZh = file("README.zh-CN.md").text
assert webReadmeZh.contains("MyBatis-Plus")
["evaluation-facade.group-id", "evaluation-facade.artifact-id",
 "evaluation-facade.version", "evaluation-facade.package"].each {
    assert webReadmeZh.contains(it): "The Chinese Web README must document the peer Facade property ${it}"
}

def reports = []
projectDir.traverse(type: FileType.FILES) { candidate ->
    if (candidate.path.replace('\\', '/').contains('/target/surefire-reports/') && candidate.name.endsWith('.xml')) reports << candidate
}
["WebArchitectureTest", "OrganizationApplicationTest",
 "OrganizationFlowTest", "OrganizationRollbackTest",
 "OrganizationFlywayMigrationTest", "RepositoryPersistenceContractTest", "LocalTransactionBoundaryTest"].each { testName ->
    def report = reports.find { it.name.contains(testName) }
    assert report && report.text.contains('failures="0"') && report.text.contains('errors="0"'):
            "Expected generated Web test report ${testName} to pass"
}

def assertPackageDocs = { String sourceRoot ->
    def javaDirs = [] as Set
    directory(sourceRoot).traverse(type: FileType.FILES) { candidate ->
        if (candidate.name.endsWith(".java") && candidate.name != "package-info.java") javaDirs << candidate.parentFile
    }
    javaDirs.each { dir ->
        assert new File(dir, "package-info.java").isFile():
                "Missing package-info.java in ${relativePath(dir)}"
    }
}
modules.each { module ->
    assertPackageDocs("${prefix}-${module}/src/main/java")
    if (module != "facade") {
        assertPackageDocs("${prefix}-${module}/src/test/java")
    }
}

def livingArchitectureDoc = null
def architectureCursor = projectDir
while (architectureCursor != null && livingArchitectureDoc == null) {
    def definitionDoc = new File(architectureCursor, "architecture-docs/multi-project-multi-module-architecture.md")
    def legacyDoc = new File(architectureCursor, "multi-project-multi-module-architecture.md")
    if (definitionDoc.isFile()) {
        livingArchitectureDoc = definitionDoc
    } else if (legacyDoc.isFile()) {
        livingArchitectureDoc = legacyDoc
    }
    architectureCursor = architectureCursor.parentFile
}
if (!livingArchitectureDoc.isFile()) {
    def cursor = basedir
    while (cursor != null && !livingArchitectureDoc.isFile()) {
        livingArchitectureDoc = new File(cursor, "multi-project-multi-module-architecture.md")
        cursor = cursor.parentFile
    }
}
assert livingArchitectureDoc.isFile(): "Expected Web living architecture document"
def livingText = livingArchitectureDoc.text
assert livingText.contains("EgonColaMapper")
assert livingText.contains("EgonModel")
assert livingText.contains("tenant_id")
assert !livingText.contains("JpaRepository")
assert !livingText.contains("UuidV7")

def sourceBoundaryFiles = []
projectDir.eachFileRecurse { candidate ->
    def candidatePath = projectDir.toPath().relativize(candidate.toPath()).toString().replace(File.separator, '/')
    // Authored sources carry the boundary; a packaged build output embeds the resolved peer facade
    // dependency GAV, which is a resolution input rather than a source leak.
    if (candidate.isFile() && !candidatePath.startsWith('target/') && !candidatePath.contains('/target/')) {
        sourceBoundaryFiles << candidate
    }
}
assert sourceBoundaryFiles.every { candidate ->
    def candidatePath = projectDir.toPath().relativize(candidate.toPath()).toString().replace(File.separator, '/')
    !candidatePath.contains('.generated')
}
// The IT supplies the peer project's real coordinates, so the root POM's explicit
// `evaluation-facade.*` property lines carry them by design instead of leaking a template sentinel.
def boundaryText = { candidate ->
    def text = candidate.getText('UTF-8')
    def candidatePath = projectDir.toPath().relativize(candidate.toPath()).toString().replace(File.separator, '/')
    candidatePath == 'pom.xml'
            ? text.readLines().findAll { !it.contains('evaluation-facade.') }.join('\n')
            : text
}
[
    'top.egon.internal.archetype.source',
    'egon-cola-source-web',
    '0.1.0-SNAPSHOT'
].each { forbiddenToken ->
    sourceBoundaryFiles.each { candidate ->
        assert !boundaryText(candidate).contains(forbiddenToken):
                "Generated project leaked source sentinel ${forbiddenToken} in ${candidate}"
    }
}

println "Web archetype verifier: MyBatis-Plus/Long/tenant contract passed"

// Verify Maven filtering and Java argument parsing without starting the application.
def launchModule = new File(projectDir, "student-management-organization-starter")
def launchTemplate = new File(launchModule, "src/main/launch/launch.args")
assert launchTemplate.isFile(): "Expected external launch argument template"
assert launchTemplate.text.contains('@run.jvm-args@')
assert launchTemplate.text.contains('@run.config-location@')
def launchArgs = new File(launchModule, "target/launch.args")
assert launchArgs.isFile(): "Maven must generate target/launch.args"
assert !launchArgs.text.contains('@run.')
assert !new File(launchModule, "target/classes/launch.args").exists()
def launchJava = new File(System.getProperty("java.home"),
        System.getProperty("os.name").toLowerCase().contains("windows") ? "bin/java.exe" : "bin/java")
def launchCheck = new ProcessBuilder(launchJava.absolutePath, "@${launchArgs.absolutePath}",
        "-XshowSettings:properties", "-version").redirectErrorStream(true).start()
def launchOutput = launchCheck.inputStream.getText("UTF-8")
assert launchCheck.waitFor() == 0: launchOutput
assert launchOutput.contains("spring.profiles.active = dev"): launchOutput
assert launchOutput.contains("server.port = 8080"): launchOutput
assert launchOutput.contains("spring.config.additional-location = optional:file:./config/override.yml"): launchOutput
true

// Verify the released parent and the actual packaged runtime, separately from BOM management.
def releasedParent = new XmlSlurper(false, false).parse(new File(projectDir, 'pom.xml'))
assert releasedParent.parent.groupId.text() == 'top.egon'
assert releasedParent.parent.artifactId.text() == 'egon-cola-archetypes-parent'
assert releasedParent.parent.version.text() == releasedParent.properties.'egon-cola.version'.text()
assert releasedParent.parent.version.text() ==~ /[0-9]+(?:\.[0-9]+)+(?:[-.][A-Za-z0-9]+)*/
assert releasedParent.parent.relativePath.size() == 1 && !releasedParent.parent.relativePath.text()
['commons-lang3.version', 'commons.lang3.version', 'shardingsphere.version', 'dubbo.version',
 'grpc.version', 'protobuf.version', 'spring-cloud.version', 'spring-cloud-alibaba.version', 'springdoc.version'].each { name ->
    assert !releasedParent.properties."${name}".text(): "Version must be inherited: ${name}"
}
def releasedArchive = new File(projectDir, 'student-management-organization-starter/target').listFiles()?.find {
    it.name.endsWith('.jar') && !it.name.endsWith('-sources.jar') && !it.name.endsWith('-javadoc.jar')
}
assert releasedArchive: 'Expected packaged consumer runtime'
def releasedLibraries = [] as Set
new java.util.jar.JarFile(releasedArchive).withCloseable { archive ->
    archive.entries().each { entry ->
        if (entry.name.startsWith('BOOT-INF/lib/')) releasedLibraries << entry.name.substring('BOOT-INF/lib/'.length())
    }
}
assert releasedLibraries.contains('spring-boot-3.5.16.jar')
assert releasedLibraries.contains('commons-lang3-3.20.0.jar')
assert releasedLibraries.any { it.startsWith('egon-cola-component-common-core-') }
assert releasedLibraries.contains('shardingsphere-jdbc-5.5.3.jar')
assert releasedLibraries.contains('grpc-core-1.75.0.jar')
assert releasedLibraries.contains('protobuf-java-4.32.0.jar')
['egon-cola-component-rpc-starter-', 'egon-cola-component-rpc-tianshu-adapter-',
 'egon-cola-tianshu-starter-',
 'egon-cola-tianshu-http-registration-starter-',
 'yuheng-starter-openapi-webmvc-'].each { required ->
    assert releasedLibraries.any { it.startsWith(required) }: "Missing native runtime ${required}"
}
assert !releasedLibraries.any { it.startsWith('dubbo-') || it.startsWith('nacos-') || it.startsWith('spring-cloud-starter-alibaba-nacos-') }
['bootstrap.yml', 'bootstrap-dev.yml', 'bootstrap-test.yml', 'bootstrap-prod.yml'].each {
    assert !new File(projectDir, 'student-management-organization-starter/src/main/resources/' + it).exists()
}
['application.yml', 'application-dev.yml', 'application-test.yml', 'application-prod.yml'].each { profile ->
    def config = new File(projectDir, 'student-management-organization-starter/src/main/resources/' + profile).text
    ['rpc:', 'tianshu:', 'provider:', 'consumer:', 'registry:', 'yuheng:', 'openapi:', 'tianquan:', 'shoubing:'].each { token ->
        assert config.contains(token): "Missing native configuration ${token} in ${profile}"
    }
    assert !config.contains('DUBBO_') && !config.contains('NACOS_')
    // The component prefix is egon.cola.platform.tianquan.shoubing; '-' is not a hierarchy separator.
    assert !config.contains('tianquan-shoubing:'): "tianquan-shoubing must nest as tianquan.shoubing in ${profile}"
}
def nativeJava = []
projectDir.traverse(type: FileType.FILES) { candidate ->
    def path = '/' + projectDir.toPath().relativize(candidate.toPath()).toString().replace('\\', '/')
    if (path.contains('/src/main/java/') && !path.contains('/target/') && candidate.name.endsWith('.java')) nativeJava << candidate
}
assert nativeJava.every { !it.text.contains('org.apache.dubbo') }
def nativeProviders = nativeJava.findAll {
    it.name.endsWith('FacadeImpl.java') && it.text.contains('@EgonRpcProvider')
}
assert nativeProviders*.name.toSet() == ['UserFacadeImpl.java', 'RoleFacadeImpl.java', 'PermissionFacadeImpl.java',
                                         'GradeFacadeImpl.java', 'SchoolClassFacadeImpl.java'].toSet():
        "Every native contract must have exactly one merged provider: ${nativeProviders*.name}"
assert nativeProviders.every { it.text.contains('@RequiredArgsConstructor') }
assert nativeJava.every { !it.name.endsWith('RpcProvider.java') }:
        "The split RpcProvider classes must be gone after the provider merge"
def nativeOperations = nativeProviders.collectMany { provider ->
    (provider.text =~ /public\s+\w*Response\s+(\w+)\(/).collect { it[1] }
}.sort()
assert nativeOperations == ["createUser", "getUser", "assignRole", "grantPermission", "getPermissionTree", "createGrade", "getGrade", "createSchoolClass", "getSchoolClass", "assignUser"].sort(): "Native operation inventory changed: ${nativeOperations}"
['docker', 'podman', 'nerdctl'].each { engine ->
    ['', '.prod'].each { profile ->
        def compose = new File(projectDir, "deploy/compose/compose.${engine}${profile}.yaml").text
        assert compose.contains('TIANSHU_APP_CODE: ${TIANSHU_APP_CODE:?Set TIANSHU_APP_CODE}')
        ['${artifactId}', '${rootArtifactId}', '${parentArtifactId}'].each { marker ->
            assert !compose.contains(marker): "Unexpanded archetype variable in Compose: ${marker}"
        }
    }
}
def nativeReports = []
projectDir.traverse(type: FileType.FILES) { candidate ->
    if (candidate.path.replace('\\', '/').contains('/target/surefire-reports/') && candidate.name.endsWith('.xml')) nativeReports << candidate
}
["NativeOrganizationRpcProviderTest", "NativeOrganizationRpcContextTest", "NativeEvaluationQueryClientTest", "NativeWebConfigurationTest", "NativeHttpCompatibilityTest"].each { name ->
    def reportsForTest = nativeReports.findAll {
        it.name.endsWith('.' + name + '.xml') || it.name.contains('.' + name + '$')
    }
    assert reportsForTest: "Missing generated native test ${name}"
    def results = reportsForTest.collect { new XmlSlurper(false, false).parse(it) }
    assert results.sum { it.@tests.text().toInteger() } > 0: "No tests ran for ${name}"
    assert results.every { it.@failures.text() == '0' && it.@errors.text() == '0' }:
            "Generated native test failed: ${name}"
}

println 'Published parent and web runtime boundaries passed'
true

// Repository/CQRS and managed PostgreSQL initialization must survive project generation.
def repositoryContractFiles = []
projectDir.traverse(type: FileType.FILES) { candidate ->
    def resourcePath = projectDir.canonicalFile.toPath().relativize(candidate.canonicalFile.toPath()).toString().replace(File.separator, "/")
    if (!resourcePath.startsWith("target/") && !resourcePath.contains("/target/")) { repositoryContractFiles << candidate }
}
def repositoryImplementations = repositoryContractFiles.findAll {
    it.path.replace('\\', '/').contains('/src/main/java/') && it.name.endsWith('Repository.java')
}
assert repositoryImplementations.size() == 8: 'Expected concrete persistence repositories'
repositoryImplementations.each { assert it.text.contains('extends EgonColaRepository<') }
def repositoryManifests = repositoryContractFiles.findAll { it.name == 'repository-manifest.json' }
assert repositoryManifests.size() == 1
def repositoryManifest = new groovy.json.JsonSlurper().parse(repositoryManifests.first())
assert repositoryManifest.family == 'web'
assert repositoryManifest.scripts.size() == 1
def initializationSql = new File(repositoryManifests.first().parentFile, 'V20260913_001__initialize_repository_schema.sql')
assert initializationSql.isFile()
assert repositoryManifest.scripts.first().sha256 == java.security.MessageDigest.getInstance('SHA-256').digest(initializationSql.bytes).encodeHex().toString()
assert initializationSql.text.contains('deleted_at') && initializationSql.text.contains('version BIGINT NOT NULL DEFAULT 0')
repositoryContractFiles.findAll { it.path.replace('\\', '/').contains('/src/main/resources/mybatis/mapper/') && it.name.endsWith('DAO.xml') }.each {
    assert it.text.contains('selectActiveById') && it.text.contains('selectActiveByIds')
    assert it.text.contains('deleteVersionedById') && it.text.contains('MP_OPTLOCK_VERSION_ORIGINAL')
    assert !it.text.contains('is_deleted')
}

return true
