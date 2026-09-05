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
def modules = ["common", "domain", "application", "infrastructure", "adapter", "starter"]
def moduleNames = modules.collect { "${prefix}-${it}" }
def rootPom = new XmlSlurper(false, false).parse(file("pom.xml"))
assert rootPom.modules.module*.text() == moduleNames
assert rootPom.properties.'java.version'.text() == "21"
assert rootPom.properties.'egon-cola.version'.text()
assert rootPom.properties.'lombok.version'.text() == "1.18.46"
assert rootPom.properties.'shardingsphere.version'.text() == "5.5.3"
def rootPomText = file("pom.xml").getText("UTF-8")
assert rootPomText.contains("egon-cola-components-bom")
assert !rootPomText.contains("spring-boot-starter-data-jpa")
assert !rootPomText.contains("mybatis-plus.version")
assert rootPomText.contains("<artifactId>lombok-mapstruct-binding</artifactId>")
moduleNames.each { file("${it}/pom.xml") }

modules.each { module ->
    ["src/main/java", "src/main/resources", "src/test/java", "src/test/resources"].each { source ->
        directory("${prefix}-${module}/${source}")
    }
}
missing("${prefix}-client")
missing("${prefix}-app")
missing("${prefix}-domain/src/main/java/domain")
missing("${prefix}-domain/src/main/java/${prefix}")
missing("${prefix}-infrastructure/src/main/java/infrastructure/repo")
missing("${prefix}-infrastructure/src/main/java/infrastructure/user/repo/jpa")
missing("${prefix}-infrastructure/src/main/java/infrastructure/teaching/repo/jpa")
missing("${prefix}-infrastructure/src/main/java/infrastructure/user/repo/mapper")
missing("${prefix}-infrastructure/src/main/java/infrastructure/teaching/repo/mapper")
missing("${prefix}-domain/src/main/java/domain/user/service/impl")
missing("${prefix}-domain/src/main/java/domain/teaching/service/impl")

def expectedFiles = [
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
    "${prefix}-infrastructure/src/main/resources/db/migration/sharding/shard/V20260825_004__migrate_organization_sharded_to_tenant_model.sql"
]
expectedFiles.each { file(it) }

def poms = [:]
modules.each { module -> poms[module] = new XmlSlurper(false, false).parse(file("${prefix}-${module}/pom.xml")) }
def dependencyIds = { pom -> pom.dependencies.dependency.artifactId*.text() as Set }
assert dependencyIds(poms.domain).contains("egon-cola-component-common-mybatis-plus-spring-boot-starter")
assert dependencyIds(poms.infrastructure).contains("${prefix}-domain".toString())
assert dependencyIds(poms.infrastructure).contains("flyway-core")
assert !dependencyIds(poms.infrastructure).contains("spring-boot-starter-data-jpa")
assert !dependencyIds(poms.infrastructure).contains("mybatis-plus-spring-boot3-starter")

def javaSources = []
modules.each { module -> javaSources.addAll(sourceFiles("${prefix}-${module}/src/main/java")) }
def runtimeText = javaSources.collect { it.getText("UTF-8") }.join("\n")
["spring-boot-starter-data-jpa", "jakarta.persistence", "JpaRepository", "@Entity", "@MappedSuperclass",
 "UuidV7Generator", "extends BaseMapper<", "repo.mapper"].each { token ->
    assert !runtimeText.contains(token): "Forbidden Web runtime token ${token}"
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

def mapperXmls = []
def mapperRoot = directory("${prefix}-infrastructure/src/main/resources/mybatis/mapper")
mapperRoot.traverse(type: FileType.FILES) { candidate -> if (candidate.name.endsWith("DAO.xml")) mapperXmls << candidate }
assert mapperXmls.size() == 8
mapperXmls.each { xml ->
    assert xml.text.contains("namespace=\"it.pkg.infrastructure.")
    assert xml.text.contains("DAO\"")
    assert xml.text.contains("tenant_id")
}

def application = file("${prefix}-starter/src/main/java/it/pkg/starter/OrganizationApplication.java").text
assert application.contains("@MapperScan")
assert application.contains("@EnableConfigurationProperties(EgonColaMybatisPlusProperties.class)")
assert application.contains("LongIdGenerator")
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
    file("${prefix}-starter/src/main/resources/sharding/shardingsphere-sharding-readwrite.yml"),
    file("${prefix}-starter/src/main/resources/datasource/sharding.yml"),
    file("${prefix}-starter/src/main/resources/datasource/sharding-readwrite.yml")
].collect { it.text }.join("\n")
["tenant_id", "tenant_long_database_bucket", "tenant_long_table_bucket", "LongTenantShardingAlgorithm"].each { token ->
    assert sharding.contains(token): "Expected sharding token ${token}"
}
assert !sharding.contains("UuidV7")
assert !sharding.contains("uuid_v7")

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
assert file("README.zh-CN.md").text.contains("MyBatis-Plus")

def reports = []
projectDir.traverse(type: FileType.FILES) { candidate ->
    if (candidate.path.replace('\\', '/').contains('/target/surefire-reports/') && candidate.name.endsWith('.xml')) reports << candidate
}
["WebArchitectureTest", "OrganizationApplicationTest", "OrganizationFlowTest", "OrganizationRollbackTest",
 "OrganizationFlywayMigrationTest", "LongTenantShardingAlgorithmTest", "ReadwriteRoutingIntegrationTest"].each { testName ->
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
    'egon-cola-source-web',
    '0.1.0-SNAPSHOT'
].each { forbiddenToken ->
    sourceBoundaryFiles.each { candidate ->
        assert !candidate.getText('UTF-8').contains(forbiddenToken):
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
