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
        "egon-cola-component-bytecode-architecture"
]
forbidden.each { token ->
    assert !sourceText.contains(token): "Forbidden runtime token ${token}"
    assert !pomText.contains(token): "Forbidden dependency/plugin token ${token}"
}

def rootPom = new XmlSlurper(false, false).parse(file("pom.xml"))
assert rootPom.modules.module.isEmpty(): "Light Open must remain one generated Maven project"
assert rootPom.properties.'java.version'.text() == "21"
assert rootPom.properties.'mybatis-plus.version'.isEmpty()
[
        "egon-cola-component-common-core",
        "egon-cola-component-common-mybatis-plus-spring-boot-starter",
        "egon-cola-component-common-id-starter",
        "egon-cola-component-dynamic-thread-pool-starter",
        "springdoc-openapi-starter-webmvc-ui",
        "archunit-junit5"
].each { artifact ->
    assert pomText.contains("<artifactId>${artifact}</artifactId>"):
            "Expected Light Open dependency ${artifact}"
}
assert !pomText.contains("dubbo-spring-boot-starter")

[
        "src/main/java/it/pkg/infrastructure/config/datasource/LongTenantShardingAlgorithm.java",
        "src/main/resources/mybatis/mapper/user/UserDAO.xml",
        "src/main/resources/mybatis/mapper/user/RoleDAO.xml",
        "src/main/resources/mybatis/mapper/user/PermissionDAO.xml",
        "src/main/resources/mybatis/mapper/user/UserRoleDAO.xml",
        "src/main/resources/mybatis/mapper/user/RolePermissionDAO.xml",
        "src/main/resources/mybatis/mapper/teaching/CourseDAO.xml",
        "src/main/resources/mybatis/mapper/teaching/ClassCourseScheduleDAO.xml",
        "src/main/resources/mybatis/mapper/teaching/SchoolClassDAO.xml",
        "src/main/resources/db/manual/postgresql/README.md",
        "src/main/resources/db/manual/postgresql/master-data/001__create_light_master_data_schema.sql",
        "src/main/resources/db/manual/postgresql/shard/002__create_light_sharded_schema.sql",
        "src/main/resources/db/manual/postgresql/master-data/003__migrate_light_master_data_to_egon_model.sql",
        "src/main/resources/db/manual/postgresql/shard/004__migrate_light_sharded_to_tenant_model.sql",
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

assert filesUnder("src/main/resources/mybatis/mapper") { it.name.endsWith("DAO.xml") }.size() == 8
assert filesUnder("src/main/java/it/pkg/infrastructure") { it.name.endsWith("PO.java") }.size() == 8
assert filesUnder("src/main/java/it/pkg/infrastructure") { it.name.endsWith("DAO.java") }.size() == 8
assert file("src/main/resources/db/manual/postgresql/master-data/001__create_light_master_data_schema.sql")
        .text.contains("BIGINT")
assert file("src/main/resources/db/manual/postgresql/shard/002__create_light_sharded_schema.sql")
        .text.contains("BIGINT")
assert file("src/main/resources/db/manual/postgresql/master-data/003__migrate_light_master_data_to_egon_model.sql")
assert file("src/main/resources/db/manual/postgresql/shard/004__migrate_light_sharded_to_tenant_model.sql")
missing("src/main/resources/db/migration")
missing("src/main/java/it/pkg/infrastructure/user/repo/mapper")
missing("src/main/java/it/pkg/infrastructure/user/repo/impl")
missing("src/main/java/it/pkg/infrastructure/teaching/repo/mapper")
missing("src/main/java/it/pkg/infrastructure/teaching/repo/impl")
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
        "LongTenantShardingAlgorithmTest",
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

def sourceBoundaryFiles = []
projectDir.eachFileRecurse { candidate ->
    def candidatePath = projectDir.toPath().relativize(candidate.toPath()).toString().replace('\\', '/')
    if (candidate.isFile() && !candidatePath.startsWith('target/')) {
        sourceBoundaryFiles << candidate
    }
}
assert sourceBoundaryFiles.every { candidate ->
    def relativePath = projectDir.toPath().relativize(candidate.toPath()).toString().replace('\\', '/')
    !relativePath.contains('.generated')
}
[
        'top.egon.internal.archetype.source',
        'egon-cola-source-light-open',
        '0.1.0-SNAPSHOT'
].each { forbiddenToken ->
    sourceBoundaryFiles.each { candidate ->
        assert !candidate.getText('UTF-8').contains(forbiddenToken):
                "Generated project leaked source sentinel ${forbiddenToken} in ${candidate}"
    }
}
true

// Verify Maven filtering and Java argument parsing without starting the application.
def launchModule = new File(projectDir, ".")
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
def releasedArchive = new File(projectDir, 'target').listFiles()?.find {
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
assert !releasedLibraries.any { it.startsWith('egon-cola-component-rpc-') || it.startsWith('egon-cola-platform-dynamic-config-center') }
assert releasedLibraries.contains('springdoc-openapi-starter-webmvc-ui-2.8.17.jar')
def openBoundaryFile = [new File(basedir, 'open-dependency-boundary.groovy'),
        new File(basedir, '../../open-dependency-boundary.groovy')].find { it.isFile() }
assert openBoundaryFile: 'Expected curated Open dependency verifier'
def openBoundary = new GroovyShell(this.class.classLoader).evaluate(openBoundaryFile)
assert openBoundary(projectDir)

println 'Published parent and light-open runtime boundaries passed'
true
