import groovy.io.FileType
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
def modules = ["common", "domain", "application", "facade", "infrastructure", "adapter", "starter"]
def moduleNames = modules.collect { "${prefix}-${it}" }
def rootPomFile = file("pom.xml")
def rootPom = new XmlSlurper(false, false).parse(rootPomFile)
assert rootPom.modules.module*.text() == moduleNames
assert rootPom.properties.'java.version'.text() == "21"
assert rootPom.properties.'lombok.version'.text() == "1.18.46"
def rootPomText = rootPomFile.getText("UTF-8")
assert !rootPomText.contains("spring-boot-starter-data-jpa")
assert !rootPomText.contains("mybatis-plus.version")
assert !rootPomText.contains("mybatis-plus-spring-boot3-starter")
assert rootPomText.contains("<artifactId>lombok-mapstruct-binding</artifactId>")
assert rootPom.properties.'evaluation-facade.group-id'.text() == "top.egon.internal.archetype.source"
assert rootPom.properties.'evaluation-facade.artifact-id'.text() == "egon-cola-source-service-open-facade"
assert rootPom.properties.'evaluation-facade.version'.text() == "0.1.0-SNAPSHOT"
assert rootPom.properties.'evaluation-facade.package'.text() == "top.egon.cola.archetype.source.serviceopen.facade"
moduleNames.each { file("${it}/pom.xml") }

def peerFacadeDependencies = { module ->
    def pom = new XmlSlurper(false, false)
            .parse(file("${prefix}-${module}/pom.xml"))
    pom.dependencies.dependency.findAll {
        it.artifactId.text() == '${evaluation-facade.artifact-id}'
    }.collect { [groupId: it.groupId.text(), artifactId: it.artifactId.text()] }
}
assert peerFacadeDependencies("infrastructure") == [[
        groupId: '${evaluation-facade.group-id}',
        artifactId: '${evaluation-facade.artifact-id}'
]]
modules.findAll { it != "infrastructure" }.each { module ->
    assert peerFacadeDependencies(module).isEmpty():
            "Only infrastructure may consume the peer Facade artifact: ${module}"
}

def javaModules = modules.findAll { it != "facade" }
javaModules.each { module ->
    ["src/main/java", "src/main/resources", "src/test/java", "src/test/resources"].each { source ->
        directory("${prefix}-${module}/${source}")
    }
}
directory("${prefix}-facade/src/main/proto")
def protoRoot = new File(projectDir, "${prefix}-facade/src/main/proto")
def protoFiles = []
protoRoot.eachFileRecurse { candidate ->
    if (candidate.isFile() && candidate.name.endsWith('.proto')) {
        protoFiles << candidate.path.substring(protoRoot.path.length() + 1).replace('\\', '/')
    }
}
assert protoFiles.sort() == [
        "google/protobuf/empty.proto",
        "organization/v1/teaching.proto",
        "organization/v1/user.proto"
]
missing("${prefix}-facade/src/main/proto/evaluation")
assert file("${prefix}-infrastructure/src/main/java/it/pkg/infrastructure/client/evaluation/impl/GrpcEvaluationQueryClientImpl.java")
        .text.contains("top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.Course")
assert file("${prefix}-infrastructure/src/main/java/it/pkg/infrastructure/client/evaluation/impl/LocalEvaluationQueryClientImpl.java").isFile()
assert file("${prefix}-infrastructure/src/main/java/it/pkg/infrastructure/client/evaluation/EvaluationQueryClient.java").isFile()
directory("${prefix}-facade/src/test/java")
directory("${prefix}-facade/src/test/resources")
missing("${prefix}-client")
missing("${prefix}-app")
missing("${prefix}-domain/src/main/java/domain")
missing("${prefix}-domain/src/main/java/${prefix}")
missing("${prefix}-infrastructure/src/main/java/infrastructure/repo")
missing("${prefix}-infrastructure/src/main/java/it/pkg/infrastructure/user/repo/jpa")
missing("${prefix}-infrastructure/src/main/java/it/pkg/infrastructure/teaching/repo/jpa")
missing("${prefix}-infrastructure/src/main/java/it/pkg/infrastructure/user/repo/mapper")
missing("${prefix}-infrastructure/src/main/java/it/pkg/infrastructure/teaching/repo/mapper")
missing("${prefix}-domain/src/main/java/it/pkg/domain/user/service/impl")
missing("${prefix}-domain/src/main/java/it/pkg/domain/teaching/service/impl")
missing("${prefix}-infrastructure/src/main/resources/db/migration")
missing("${prefix}-infrastructure/src/main/resources/mapper")

def expectedFiles = [
        "${prefix}-domain/pom.xml",
        "${prefix}-domain/src/main/java/it/pkg/domain/user/service/UserDomainService.java",
        "${prefix}-domain/src/main/java/it/pkg/domain/user/service/PermissionDomainService.java",
        "${prefix}-domain/src/main/java/it/pkg/domain/teaching/service/GradeDomainService.java",
        "${prefix}-domain/src/main/java/it/pkg/domain/teaching/service/SchoolClassDomainService.java",
        "${prefix}-application/src/main/java/it/pkg/application/config/ApplicationConfiguration.java",
        "${prefix}-infrastructure/src/main/java/it/pkg/infrastructure/user/service/impl/UserDomainServiceImpl.java",
        "${prefix}-infrastructure/src/main/java/it/pkg/infrastructure/user/service/impl/PermissionDomainServiceImpl.java",
        "${prefix}-infrastructure/src/main/java/it/pkg/infrastructure/teaching/service/impl/GradeDomainServiceImpl.java",
        "${prefix}-infrastructure/src/main/java/it/pkg/infrastructure/teaching/service/impl/SchoolClassDomainServiceImpl.java",
        "${prefix}-starter/src/main/java/it/pkg/starter/OrganizationApplication.java",
        "${prefix}-starter/src/test/java/it/pkg/architecture/WebOpenPersistenceArchitectureTest.java",
        "${prefix}-adapter/src/main/java/it/pkg/adapter/user/facade/impl/UserFacadeImpl.java",
        "${prefix}-adapter/src/main/java/it/pkg/adapter/teaching/facade/impl/SchoolClassFacadeImpl.java",
        "${prefix}-facade/src/test/java/it/pkg/facade/ProtoContractTest.java",
        "${prefix}-infrastructure/src/main/resources/mybatis/mapper/user/UserDAO.xml",
        "${prefix}-infrastructure/src/main/resources/mybatis/mapper/teaching/GradeDAO.xml",
        "${prefix}-infrastructure/src/main/resources/db/manual/postgresql/master-data/003__migrate_organization_master_data_to_egon_model.sql",
        "${prefix}-infrastructure/src/main/resources/db/manual/postgresql/shard/004__migrate_organization_sharded_to_tenant_model.sql"
]
expectedFiles.each { file(it) }
// Sharding topology, algorithms and bootstrap come from the component starter, never from a local copy.
[
        "DataSourceModeProperties",
        "ShardingNodeMap",
        "LongTenantShardingAlgorithm",
        "ShardingWriteTargetResolver",
        "ShardingDataSourceBootstrapper",
        "ShardingDataSourcePropertiesLoader",
        "ShardingTopologyValidator",
        "ShardingSphereDataSourceConfiguration",
        "LogicalDataSourceFlywayMigrationStrategy",
        "ShardingDataSourceModeCondition",
        "ShardingNodeMapCompatibilityValidator"
].each { typeName ->
    missing("${prefix}-infrastructure/src/main/java/it/pkg/infrastructure/config/datasource/${typeName}.java")
}

def poms = [:]
modules.each { module -> poms[module] = new XmlSlurper(false, false).parse(file("${prefix}-${module}/pom.xml")) }
def dependencyIds = { pom -> pom.dependencies.dependency.artifactId*.text() as Set }
assert !dependencyIds(poms.domain).contains("egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter")
assert dependencyIds(poms.infrastructure).contains("egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter")
assert dependencyIds(poms.infrastructure).contains("${prefix}-domain".toString())
assert !dependencyIds(poms.infrastructure).contains("spring-boot-starter-data-jpa")
assert !dependencyIds(poms.infrastructure).contains("mybatis-plus-spring-boot3-starter")

def javaSources = []
javaModules.each { module -> javaSources.addAll(sourceFiles("${prefix}-${module}/src/main/java")) }
def runtimeText = javaSources.collect { it.getText("UTF-8") }.join("\n") +
        moduleNames.collect { file("${it}/pom.xml").getText("UTF-8") }.join("\n")
["spring-boot-starter-data-jpa", "jakarta.persistence", "JpaRepository", "@Entity", "@MappedSuperclass",
 "UuidV7Generator", "extends BaseMapper<", "repo.mapper", "org.flywaydb", "flyway-database",
 "liquibase", "egon-cola-organization-facade", "egon-cola-evaluation-facade", "spring-cloud-starter-gateway",
 "spring.cloud.gateway"].each { token ->
    assert !runtimeText.contains(token): "Forbidden Web Open runtime token ${token}"
}
// Data identity stays on the static Snowflake generator. UUIDv4 is only allowed where the inbound
// adapter mints trace, idempotency-key and message correlation ids; no other layer may use it.
assert runtimeText.contains("SnowflakeIdGenerator.nextLongId()")
assert javaSources.findAll { !relativePath(it).contains("-adapter/src/main/java") }
        .every { !it.getText("UTF-8").contains("UUID") }:
        "Only the adapter layer may mint UUID correlation ids"

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

def mapperFiles = []
def mapperRoot = directory("${prefix}-infrastructure/src/main/resources/mybatis/mapper")
mapperRoot.traverse(type: FileType.FILES) { candidate ->
    if (candidate.name.endsWith("DAO.xml")) mapperFiles << candidate
}
assert mapperFiles.size() == 8
mapperFiles.each { xml ->
    assert xml.text.contains("namespace=\"it.pkg.infrastructure.")
    assert xml.text.contains("DAO\"")
    assert xml.text.contains("tenant_id")
}

def application = file("${prefix}-starter/src/main/java/it/pkg/starter/OrganizationApplication.java").text
assert application.contains("@MapperScan")
assert application.contains("@EnableConfigurationProperties(EgonColaMybatisPlusProperties.class)")
// Step 2 staticizes ID issuance: the starter configuration initializes the static generator, so the
// Application class must not hand-wire an ID bean or its clock-rollback state.
assert !application.contains("LongIdGenerator")
assert !application.contains("AtomicLong")
assert !application.contains("@Bean")
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
def openRepositoryManifest = file("${prefix}-infrastructure/src/main/resources/db/egon-mp/repository-manifest.json").text
assert openRepositoryManifest.contains('"family": "web-open"')
assert openRepositoryManifest.contains("V20260913_001__initialize_repository_schema.sql")
assert openRepositoryManifest.contains('"sha256"')
assert !sharding.contains('mapping-version')
assert !sharding.contains('node-count')
assert !sharding.contains('node-map')
assert !sharding.contains('LongTenantShardingAlgorithm')
assert !sharding.contains('grade_id INLINE')
assert !sharding.contains('SnowflakeLongShardingAlgorithm')
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

def manualRoot = directory("${prefix}-infrastructure/src/main/resources/db/manual/postgresql")
def manualPaths = []
manualRoot.traverse(type: FileType.FILES) { candidate ->
    if (candidate.name.endsWith(".sql")) {
        manualPaths << manualRoot.toPath().relativize(candidate.toPath()).toString().replace(File.separator, "/")
    }
}
assert manualPaths.sort() == [
        "master-data/001__create_organization_master_data_schema.sql",
        "master-data/003__migrate_organization_master_data_to_egon_model.sql",
        "shard/002__create_organization_sharded_schema.sql",
        "shard/004__migrate_organization_sharded_to_tenant_model.sql"
]
assert file("${prefix}-infrastructure/src/main/resources/db/manual/postgresql/master-data/001__create_organization_master_data_schema.sql").text.contains("BIGINT")
assert file("${prefix}-infrastructure/src/main/resources/db/manual/postgresql/shard/002__create_organization_sharded_schema.sql").text.contains("BIGINT")
assert file("${prefix}-infrastructure/src/main/resources/db/manual/postgresql/master-data/003__migrate_organization_master_data_to_egon_model.sql").text.contains("tenant_id BIGINT")
assert file("${prefix}-infrastructure/src/main/resources/db/manual/postgresql/shard/004__migrate_organization_sharded_to_tenant_model.sql").text.contains("tenant_id BIGINT")
assert file("${prefix}-infrastructure/src/main/resources/db/manual/postgresql/README.md").text.contains("003__migrate")
assert file("${prefix}-infrastructure/src/main/resources/db/manual/postgresql/README.md").text.contains("004__migrate")

def starterPom = file("${prefix}-starter/pom.xml").text
assert starterPom.contains("egon-cola-component-dynamic-thread-pool-starter")
assert starterPom.contains("archunit-junit5")
def readme = file("README.md").text
assert readme.contains("Springdoc")
assert readme.contains("MyBatis-Plus")
assert readme.contains("Gateway")
assert readme.contains("tenant_id")
assert readme.contains("Long")
assert readme.contains("db/egon-mp")
assert !readme.contains("spring-boot-starter-data-jpa")
assert !readme.contains("UUIDv7")
assert file("README.zh-CN.md").text.contains("MyBatis-Plus")
["evaluation-facade.group-id", "evaluation-facade.artifact-id",
 "evaluation-facade.version", "evaluation-facade.package"].each { token ->
    assert readme.contains(token): "The README must document the peer Facade property ${token}"
}

def reports = []
projectDir.traverse(type: FileType.FILES) { candidate ->
    if (candidate.path.replace('\\', '/').contains('/target/surefire-reports/') && candidate.name.endsWith('.xml')) reports << candidate
}
["ProtoContractTest", "OpenPeerFacadeContractTest", "GrpcEvaluationQueryClientTest", "OrganizationDubboProviderConfigurationTest",
 "OrganizationApplicationTest", "OrganizationExternalFreeContextTest", "OpenApiContractTest",
 "OpenArchitectureTest", "WebOpenPersistenceArchitectureTest"].each { testName ->
    def report = reports.find { it.name.contains(testName) }
    assert report && report.text.contains('failures="0"') && report.text.contains('errors="0"'):
            "Expected generated Web Open test report ${testName} to pass"
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
javaModules.each { module ->
    assertPackageDocs("${prefix}-${module}/src/main/java")
}
modules.each { module ->
    assertPackageDocs("${prefix}-${module}/src/test/java")
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
assert livingArchitectureDoc.isFile(): "Expected Web Open living architecture document"
def livingText = livingArchitectureDoc.text
assert livingText.contains("EgonColaMapper")
assert livingText.contains("EgonModel")
assert livingText.contains("tenant_id")
assert !livingText.contains("com.baomidou.mybatisplus.core.mapper.BaseMapper")
assert !livingText.contains("grade_id` 路由")
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
    def scannedPath = projectDir.toPath().relativize(candidate.toPath()).toString().replace(File.separator, '/')
    def text = candidate.getText('UTF-8')
    scannedPath == 'pom.xml'
            ? text.readLines().findAll { !it.contains('evaluation-facade.') }.join('\n')
            : text
}
[
    'top.egon.internal.archetype.source',
    'egon-cola-source-web-open',
    '0.1.0-SNAPSHOT'
].each { forbiddenToken ->
    sourceBoundaryFiles.each { candidate ->
        assert !boundaryText(candidate).contains(forbiddenToken):
                "Generated project leaked source sentinel ${forbiddenToken} in ${candidate}"
    }
}

println "Web Open archetype verifier: Common MyBatis-Plus/Long/tenant contract passed"

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
assert !releasedLibraries.any { it.startsWith('egon-cola-component-rpc-') || it.startsWith('egon-cola-tianshu') }
assert releasedLibraries.contains('grpc-api-1.73.0.jar')
assert releasedLibraries.contains('protobuf-java-3.25.8.jar')
assert releasedLibraries.contains('dubbo-3.3.6.jar')
assert releasedLibraries.contains('springdoc-openapi-starter-webmvc-ui-2.8.17.jar')

println 'Published parent and web-open runtime boundaries passed'
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
assert repositoryManifest.family == 'web-open'
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
