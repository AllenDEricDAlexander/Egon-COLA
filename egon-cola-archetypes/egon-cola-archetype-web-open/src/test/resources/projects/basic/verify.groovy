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
assert rootPom.parent.version.text() == "3.5.16"
assert rootPom.properties.'java.version'.text() == "21"
assert rootPom.properties.'spring-cloud.version'.text() == "2025.0.3"
assert rootPom.properties.'spring-cloud-alibaba.version'.text() == "2025.0.0.0"
assert rootPom.properties.'dubbo.version'.text() == "3.3.6"
assert rootPom.properties.'grpc.version'.text() == "1.73.0"
assert rootPom.properties.'springdoc.version'.text() == "2.8.17"
assert rootPom.properties.'shardingsphere.version'.text() == "5.5.3"
assert rootPom.properties.'lombok.version'.text() == "1.18.46"
def rootPomText = rootPomFile.getText("UTF-8")
assert rootPomText.contains("egon-cola-components-bom")
assert !rootPomText.contains("spring-boot-starter-data-jpa")
assert !rootPomText.contains("mybatis-plus.version")
assert !rootPomText.contains("mybatis-plus-spring-boot3-starter")
assert rootPomText.contains("<artifactId>lombok-mapstruct-binding</artifactId>")
moduleNames.each { file("${it}/pom.xml") }

def javaModules = modules.findAll { it != "facade" }
javaModules.each { module ->
    ["src/main/java", "src/main/resources", "src/test/java", "src/test/resources"].each { source ->
        directory("${prefix}-${module}/${source}")
    }
}
directory("${prefix}-facade/src/main/proto")
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
        "${prefix}-infrastructure/src/main/java/it/pkg/infrastructure/config/datasource/LongTenantShardingAlgorithm.java",
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

def poms = [:]
modules.each { module -> poms[module] = new XmlSlurper(false, false).parse(file("${prefix}-${module}/pom.xml")) }
def dependencyIds = { pom -> pom.dependencies.dependency.artifactId*.text() as Set }
assert dependencyIds(poms.domain).contains("egon-cola-component-common-mybatis-plus-spring-boot-starter")
assert dependencyIds(poms.infrastructure).contains("${prefix}-domain".toString())
assert !dependencyIds(poms.infrastructure).contains("spring-boot-starter-data-jpa")
assert !dependencyIds(poms.infrastructure).contains("mybatis-plus-spring-boot3-starter")

def javaSources = []
javaModules.each { module -> javaSources.addAll(sourceFiles("${prefix}-${module}/src/main/java")) }
def runtimeText = javaSources.collect { it.getText("UTF-8") }.join("\n") +
        moduleNames.collect { file("${it}/pom.xml").getText("UTF-8") }.join("\n")
["spring-boot-starter-data-jpa", "jakarta.persistence", "JpaRepository", "@Entity", "@MappedSuperclass",
 "UuidV7Generator", "UUID.randomUUID", "extends BaseMapper<", "repo.mapper", "org.flywaydb", "flyway-database",
 "liquibase", "egon-cola-organization-facade", "egon-cola-evaluation-facade", "spring-cloud-starter-gateway",
 "spring.cloud.gateway"].each { token ->
    assert !runtimeText.contains(token): "Forbidden Web Open runtime token ${token}"
}

def poSources = javaSources.findAll {
    def path = relativePath(it)
    path.contains("/infrastructure/") && path.contains("/repo/po/") && it.name.endsWith("PO.java")
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
    path.contains("/infrastructure/") && path.contains("/repo/dao/") && it.name.endsWith("DAO.java")
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
    assert service.text.contains("extends EgonColaIService<"):
            "Domain service must extend EgonColaIService: ${relativePath(service)}"
}
def serviceImpls = javaSources.findAll {
    def path = relativePath(it)
    path.contains("/infrastructure/") && path.contains("/service/impl/") && it.name.endsWith("DomainServiceImpl.java")
}
assert serviceImpls.size() == 4
serviceImpls.each { implementation ->
    assert implementation.text.contains("extends EgonColaServiceImpl<"):
            "Infrastructure service must extend EgonColaServiceImpl: ${relativePath(implementation)}"
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
assert application.contains("LongIdGenerator")
assert application.contains("AtomicLong")
assert application.contains("@Bean")
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

def sharding = [
        file("${prefix}-starter/src/main/resources/sharding/shardingsphere-sharding.yml"),
        file("${prefix}-starter/src/main/resources/sharding/shardingsphere-sharding-readwrite.yml")
].collect { it.text }.join("\n")
["tenant_id", "tenant_long_database_bucket", "tenant_long_table_bucket", "LongTenantShardingAlgorithm"].each { token ->
    assert sharding.contains(token): "Expected sharding token ${token}"
}
assert !sharding.contains("grade_id INLINE")
assert !sharding.contains("SnowflakeLongShardingAlgorithm")
assert !sharding.contains("UuidV7")
assert !sharding.contains("uuid_v7")

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
assert readme.contains("db/manual/postgresql")
assert !readme.contains("spring-boot-starter-data-jpa")
assert !readme.contains("UUIDv7")
assert file("README.zh-CN.md").text.contains("MyBatis-Plus")

def reports = []
projectDir.traverse(type: FileType.FILES) { candidate ->
    if (candidate.path.replace('\\', '/').contains('/target/surefire-reports/') && candidate.name.endsWith('.xml')) reports << candidate
}
["ProtoContractTest", "GrpcEvaluationQueryClientTest", "OrganizationDubboProviderConfigurationTest",
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

def livingArchitectureDoc = new File(projectDir.parentFile ?: projectDir, "multi-project-multi-module-architecture.md")
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
assert !livingText.contains("BaseMapper")
assert !livingText.contains("grade_id` 路由")
assert !livingText.contains("JpaRepository")
assert !livingText.contains("UuidV7")

def sourceBoundaryFiles = []
projectDir.eachFileRecurse { candidate ->
    def candidatePath = projectDir.toPath().relativize(candidate.toPath()).toString().replace(File.separator, '/')
    if (candidate.isFile() && !candidatePath.startsWith('target/')) {
        sourceBoundaryFiles << candidate
    }
}
assert sourceBoundaryFiles.every { candidate ->
    def candidatePath = projectDir.toPath().relativize(candidate.toPath()).toString().replace(File.separator, '/')
    !candidatePath.contains('.generated')
}
[
    'top.egon.internal.archetype.source',
    'egon-cola-source-web-open',
    '0.1.0-SNAPSHOT'
].each { forbiddenToken ->
    sourceBoundaryFiles.each { candidate ->
        assert !candidate.getText('UTF-8').contains(forbiddenToken):
                "Generated project leaked source sentinel ${forbiddenToken} in ${candidate}"
    }
}

println "Web Open archetype verifier: Common MyBatis-Plus/Long/tenant contract passed"
