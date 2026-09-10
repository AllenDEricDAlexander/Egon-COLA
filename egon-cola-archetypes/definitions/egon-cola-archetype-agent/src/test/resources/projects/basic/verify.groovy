import groovy.io.FileType
import groovy.xml.XmlSlurper

def projectDir = new File(basedir, "pom.xml").isFile() ? basedir : context.projectDir
assert projectDir != null: "Expected generated Deep Research project"

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
def textFiles = {
    def result = []
    projectDir.traverse(type: FileType.FILES) { candidate ->
        if (!candidate.path.replace('\\', '/').contains('/target/') &&
                ['.java', '.xml', '.yml', '.md', '.properties'].any { candidate.name.endsWith(it) }) {
            result << candidate
        }
    }
    result
}
def dependencyIds = { pom -> pom.dependencies.dependency.artifactId*.text() as Set }

def prefix = "deep-research-agent"
def modules = ["common", "domain", "application", "infrastructure", "adapter", "starter"]
def moduleNames = modules.collect { "${prefix}-${it}" }
def rootPom = new XmlSlurper(false, false).parse(file("pom.xml"))
assert rootPom.modules.module*.text() == moduleNames
assert rootPom.properties.'java.version'.text() == "21"
assert rootPom.properties.'spring-ai.version'.text() == "1.1.8"
assert rootPom.properties.'egon-cola.version'.text()
assert rootPom.dependencies.dependency.size() == 0

def rootPomText = file("pom.xml").getText("UTF-8")
assert rootPomText.contains("spring-ai-bom")
assert !rootPomText.contains("top.egon.internal.archetype.source")
assert !rootPomText.contains("egon-cola-source-agent")

moduleNames.each { module -> file("${module}/pom.xml") }
modules.each { module ->
    def moduleRoot = "${prefix}-${module}"
    directory("${moduleRoot}/src/main/java")
    directory("${moduleRoot}/src/test/java")
}

def poms = [:]
modules.each { module -> poms[module] = new XmlSlurper(false, false).parse(file("${prefix}-${module}/pom.xml")) }
assert dependencyIds(poms.common).contains("egon-cola-component-common-core")
assert dependencyIds(poms.domain).contains("${prefix}-common".toString())
assert dependencyIds(poms.application).contains("${prefix}-domain".toString())
assert dependencyIds(poms.infrastructure).contains("${prefix}-domain".toString())
assert dependencyIds(poms.infrastructure).contains("egon-cola-component-agent-flow-starter")
assert dependencyIds(poms.infrastructure).contains("spring-ai-mcp")
assert dependencyIds(poms.adapter).contains("${prefix}-application".toString())
assert !dependencyIds(poms.adapter).contains("${prefix}-infrastructure".toString())
assert dependencyIds(poms.starter).contains("${prefix}-adapter".toString())
assert dependencyIds(poms.starter).contains("${prefix}-infrastructure".toString())
assert dependencyIds(poms.starter).contains("spring-ai-openai")

// knowledge 域的持久化、RAG、outbox 与向量依赖必须落到对应模块；starter 不直接声明 JDBC starter（`EVD-003`）。
assert dependencyIds(poms.domain).contains("egon-cola-component-common-mybatis-plus-spring-boot-starter")
assert dependencyIds(poms.infrastructure).contains("egon-cola-component-rag-starter")
assert dependencyIds(poms.infrastructure).contains("egon-cola-component-transactional-outbox-starter")
assert dependencyIds(poms.infrastructure).contains("spring-ai-pgvector-store")
assert dependencyIds(poms.infrastructure).contains("flyway-core")
assert dependencyIds(poms.infrastructure).contains("postgresql")
modules.each { module ->
    assert !dependencyIds(poms[module]).contains("spring-boot-starter-jdbc"):
            "spring-boot-starter-jdbc must stay forbidden in ${module} (EVD-003)"
}

// flyway-core 与 mybatis-plus-spring-boot3-starter 是 knowledge 域的持久化依赖，
// 已由 Spec 2026-09-10-11-51 的 Amends 放开；其余禁项保持不变。
def forbiddenDependencies = [
    "spring-boot-starter-data-jpa",
    "spring-boot-starter-data-redis", "spring-boot-starter-amqp", "dubbo-spring-boot-starter",
    "spring-boot-starter-graphql", "egon-cola-organization-facade", "egon-cola-evaluation-facade"
]
modules.each { module ->
    def ids = dependencyIds(poms[module])
    forbiddenDependencies.each { forbidden -> assert !ids.contains(forbidden): "Forbidden dependency ${forbidden} in ${module}" }
}

// Inspect packaged runtime libraries; imported BOM entries never enter this set.
def agentArchive = new File(projectDir, "${prefix}-starter/target").listFiles()?.find {
    it.name.endsWith(".jar") && !it.name.endsWith("-sources.jar") && !it.name.endsWith("-javadoc.jar")
}
assert agentArchive: "Expected packaged Agent runtime"
def agentLibraries = [] as Set
new java.util.jar.JarFile(agentArchive).withCloseable { archive ->
    archive.entries().each { entry ->
        if (entry.name.startsWith("BOOT-INF/lib/")) agentLibraries << entry.name.substring("BOOT-INF/lib/".length())
    }
}
["egon-cola-component-agent-flow-starter-", "spring-ai-openai-", "spring-ai-mcp-", "google-adk-",
 "egon-cola-component-common-mybatis-plus-spring-boot-starter-", "mybatis-plus-spring-boot3-starter-",
 "egon-cola-component-rag-starter-", "egon-cola-component-transactional-outbox-starter-",
 "spring-ai-pgvector-store-", "flyway-core-", "flyway-database-postgresql-", "postgresql-"].each { required ->
    assert agentLibraries.any { it.startsWith(required) }: "Missing Agent runtime library ${required}"
}
["egon-cola-component-rpc-", "egon-cola-tianshu", "nacos-", "dubbo-",
 "shardingsphere-", "spring-cloud-starter-alibaba-nacos-"].each { forbidden ->
    assert !agentLibraries.any { it.startsWith(forbidden) }: "Forbidden Agent runtime library ${forbidden}"
}
// Google ADK's gRPC/Protobuf dependencies remain allowed.

def requiredFiles = [
    "${prefix}-domain/src/main/java/it/pkg/domain/research/model/DeepResearchTaskBO.java",
    "${prefix}-domain/src/main/java/it/pkg/domain/research/service/DeepResearchRunService.java",
    "${prefix}-application/src/main/java/it/pkg/application/research/manage/DeepResearchManage.java",
    "${prefix}-application/src/main/java/it/pkg/application/research/manage/impl/DeepResearchManageImpl.java",
    "${prefix}-infrastructure/src/main/java/it/pkg/infrastructure/research/gateway/AgentFlowDeepResearchAgentGateway.java",
    "${prefix}-infrastructure/src/main/java/it/pkg/infrastructure/research/tool/McpResearchToolFactory.java",
    "${prefix}-adapter/src/main/java/it/pkg/adapter/research/controller/DeepResearchController.java",
    "${prefix}-adapter/src/main/java/it/pkg/adapter/research/dto/StartDeepResearchRequest.java",
    "${prefix}-adapter/src/main/java/it/pkg/adapter/research/vo/DeepResearchEventVO.java",
    "${prefix}-starter/src/main/java/it/pkg/starter/DeepResearchApplication.java",
    "${prefix}-starter/src/main/resources/agent/deep-research-flow.yml",
    "${prefix}-starter/src/main/resources/application.yml",
    "${prefix}-starter/src/main/resources/application-dev.yml",
    "${prefix}-starter/src/main/resources/application-test.yml",
    "${prefix}-starter/src/main/resources/application-prod.yml"
]
requiredFiles.each { file(it) }
missing("${prefix}-client")
missing("${prefix}-app")
missing("${prefix}-facade")
missing("${prefix}-adapter/src/main/resources/graphql")

// 迁移必须随生成产物交付；向量表由 PgVectorStore 以 initializeSchema 自建，不进入 Flyway 版本管理。
def knowledgeMigrations = [
    "${prefix}-infrastructure/src/main/resources/db/migration/V20260910_001__create_knowledge_schema.sql",
    "${prefix}-infrastructure/src/main/resources/db/migration/V20260910_002__create_transactional_outbox_schema.sql"
]
knowledgeMigrations.each { file(it) }
assert file(knowledgeMigrations[0]).getText("UTF-8").contains("create extension if not exists vector")
knowledgeMigrations.each { migration ->
    assert !file(migration).getText("UTF-8").toLowerCase().contains("vector_store"):
            "vector_store must stay outside Flyway version control (${migration})"
}

def runtimeSources = modules.collectMany { sourceFiles("${prefix}-${it}/src/main/java") }
def runtimeText = runtimeSources.collect { it.getText("UTF-8") }.join("\n")
assert runtimeText.contains("PostMapping")
assert runtimeText.contains("TEXT_EVENT_STREAM")
assert runtimeText.contains("deep-research")
assert runtimeText.contains("researchApiKey")
assert !runtimeText.contains("org.apache.commons.lang3.time.DateUtils")
assert !(runtimeText =~ /import\s+java\.util\.(Date|Calendar)/)
assert !runtimeText.contains("com.alibaba.fastjson")

def flow = file("${prefix}-starter/src/main/resources/agent/deep-research-flow.yml").text
["Planner", "ParallelResearch", "EvidenceResearcher", "CounterpointResearcher", "FreshnessResearcher", "Writer",
 "research_plan", "evidence_findings", "counterpoint_findings", "freshness_findings", "final_report"].each { token ->
    assert flow.contains(token): "Missing fixed flow token ${token}"
}
assert flow.contains("sub-agent-names: [Planner, ParallelResearch, Writer]")
assert flow.contains("sub-agent-names: [EvidenceResearcher, CounterpointResearcher, FreshnessResearcher]")

def profiles = ["application.yml", "application-dev.yml", "application-test.yml", "application-prod.yml"]
profiles.each { profile ->
    def text = file("${prefix}-starter/src/main/resources/${profile}").text
    ["agent:", "deep-research:", "model:", "search:"].each { token ->
        assert text.contains(token): "Missing ${token} in ${profile}"
    }
    if (profile == "application-test.yml") {
        assert text.contains("api-key:")
    } else {
        assert text.contains("DEEP_RESEARCH_API_KEY")
    }
}

def readme = file("README.md").text
assert readme.contains("Deep Research")
assert readme.contains("POST /api/v1/deep-research/runs")
assert readme.contains("clean verify")
assert readme.contains("no database")
assert file("README.zh-CN.md").text.contains("Deep Research")

def reports = []
projectDir.traverse(type: FileType.FILES) { candidate ->
    if (candidate.path.replace('\\', '/').contains('/target/surefire-reports/') && candidate.name.endsWith('.xml')) {
        reports << candidate
    }
}
["AgentArchitectureTest", "DeepResearchDomainTest", "DeepResearchManageImplTest", "AgentFlowDeepResearchAgentGatewayTest",
 "AgentFlowEventConverterTest", "McpResearchToolFactoryTest", "DeepResearchControllerTest", "DeepResearchConverterTest",
 "ResearchApiKeyFilterTest", "DeepResearchApplicationTest", "DeepResearchFlowTest", "DeepResearchOpenApiTest",
 "AgentSourceContractTest"].each { testName ->
    def report = reports.find { it.name.contains(testName) }
    assert report && report.text.contains('failures="0"') && report.text.contains('errors="0"'):
            "Expected generated Agent test report ${testName} to pass"
}

def sourceBoundaryFiles = textFiles().findAll { candidate ->
    def path = candidate.path.replace('\\', '/')
    path.contains('/src/main/') || candidate.name == 'pom.xml'
}
def generatedSourceText = sourceBoundaryFiles.collect { it.text }.join("\n").toLowerCase()
["top.egon.internal.archetype.source", "egon-cola-source-agent", "0.1.0-snapshot"].each { forbiddenToken ->
    assert !generatedSourceText.contains(forbiddenToken): "Generated project leaked source sentinel ${forbiddenToken}"
}
["spring-boot-starter-data-jpa", "jakarta.persistence", "jparepository", "redis", "graphql", "dubbo", "fastjson"].each { forbiddenToken ->
    assert !generatedSourceText.contains(forbiddenToken): "Generated project leaked forbidden token ${forbiddenToken}"
}

def assertPackageDocs = { String sourceRoot ->
    def javaDirs = [] as Set
    directory(sourceRoot).traverse(type: FileType.FILES) { candidate ->
        if (candidate.name.endsWith(".java") && candidate.name != "package-info.java") javaDirs << candidate.parentFile
    }
    javaDirs.each { dir ->
        assert new File(dir, "package-info.java").isFile(): "Missing package-info.java in ${relativePath(dir)}"
    }
}
modules.each { module ->
    assertPackageDocs("${prefix}-${module}/src/main/java")
    assertPackageDocs("${prefix}-${module}/src/test/java")
}

def architectureDocs = null
def architectureCursor = projectDir
while (architectureCursor != null && architectureDocs == null) {
    def candidate = new File(architectureCursor, "architecture-docs")
    if (candidate.isDirectory()) {
        architectureDocs = candidate
    }
    architectureCursor = architectureCursor.parentFile
}
assert architectureDocs && architectureDocs.listFiles()?.any { doc ->
    doc.file && doc.text.contains("Deep Research")
}

println "Agent archetype verifier: six-module Deep Research contract passed"

// Verify Maven filtering and Java argument parsing without starting the application.
def launchModule = new File(projectDir, "deep-research-agent-starter")
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
def releasedArchive = new File(projectDir, 'deep-research-agent-starter/target').listFiles()?.find {
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

println 'Published parent and agent runtime boundaries passed'
true
