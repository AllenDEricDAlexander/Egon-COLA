import java.util.regex.Pattern

def project = new File(request.outputDirectory, request.artifactId)
def wrapper = new File(project, 'mvnw')
if (wrapper.isFile()) wrapper.setExecutable(true, false)

def rootPom = new File(project, 'pom.xml')
def rootText = rootPom.getText('UTF-8')
def web = rootText.contains('<evaluation-facade.group-id>')
def peer = web ? 'evaluation' : 'organization'
def peerClass = web ? 'Evaluation' : 'Organization'
def parts = ['group-id', 'artifact-id', 'version', 'package']
def values = parts.collect { part ->
    def matcher = Pattern.compile('<' + peer + '-facade\\.' + part + '>([^<]*)</' + peer + '-facade\\.' + part + '>').matcher(rootText)
    if (!matcher.find()) throw new IllegalStateException("Generated POM lacks ${peer}-facade.${part}")
    matcher.group(1).trim()
}
def omitted = values.count { it == '__NO_PEER_FACADE__' }
if (omitted != 0 && omitted != parts.size()) {
    throw new IllegalArgumentException("Supply all four ${peer}Facade* properties or omit all four")
}
if (omitted == 0) {
    if (values.any { !it || it.contains('__NO_PEER_FACADE__') }) {
        throw new IllegalArgumentException("${peerClass} Facade coordinates must be nonblank and cannot use the reserved sentinel")
    }
    return
}

def moduleMatcher = Pattern.compile('<module>([^<]+)-common</module>').matcher(rootText)
if (!moduleMatcher.find()) throw new IllegalStateException('Generated root POM has no common module')
def rootArtifactId = moduleMatcher.group(1)
def module = { suffix -> new File(project, "${rootArtifactId}-${suffix}") }
def replaceExactly = { file, pattern, replacement, expected ->
    def source = file.getText('UTF-8')
    def matcher = Pattern.compile(pattern).matcher(source)
    def count = 0
    while (matcher.find()) count++
    if (count != expected) throw new IllegalStateException("Expected ${expected} peer sections in ${file}; found ${count}")
    file.setText(source.replaceAll(pattern, replacement), 'UTF-8')
}
def removeNamed = { suffix, names ->
    def base = new File(module(suffix), 'src/main/java')
    names.each { name ->
        def matches = []
        base.eachFileRecurse { file -> if (file.isFile() && file.name == name) matches << file }
        if (matches.size() != 1 || !matches[0].delete()) {
            throw new IllegalStateException("Expected one ${name} in ${base}")
        }
    }
}
def removeClient = { scope ->
    def base = new File(module('infrastructure'), scope)
    def matches = []
    base.eachDirRecurse { dir ->
        if (dir.name == peer && dir.parentFile.name == 'client') matches << dir
    }
    if (matches.size() != 1 || !matches[0].deleteDir()) {
        throw new IllegalStateException("Expected one ${peerClass} client tree in ${base}")
    }
}
def findTest = { name ->
    def base = new File(module('starter'), 'src/test/java')
    def matches = []
    base.eachFileRecurse { file -> if (file.isFile() && file.name == name) matches << file }
    if (matches.size() != 1) throw new IllegalStateException("Expected one ${name} in ${base}")
    matches[0]
}

[rootPom, new File(module('infrastructure'), 'pom.xml')].each { pom ->
    replaceExactly(pom, '(?s)\\s*<!-- optional-peer-facade:start -->.*?<!-- optional-peer-facade:end -->', '',
            pom == rootPom ? 2 : 1)
}
removeClient('src/main/java')
removeClient('src/test/java')
if (web) {
    removeNamed('domain', ['EvaluationQueryService.java', 'EvaluationCourseBO.java',
            'EvaluationExamBO.java', 'EvaluationScoreBO.java'])
    removeNamed('infrastructure', ['EvaluationQueryServiceImpl.java'])
    def context = findTest('OrganizationApplicationTest.java')
    replaceExactly(context, '(?m)^import .*?(?:EvaluationQueryService|EvaluationQueryClientImpl|EvaluationQueryServiceImpl);\\n', '', 4)
    replaceExactly(context, '(?m)^    @Autowired\\n    private EvaluationQueryService evaluationQueryPort;\\n\\n', '', 1)
    replaceExactly(context, '(?m)^        (?:assertThat\\((?:environment.*evaluation|evaluationQueryPort|context.*evaluationQueryClient|context.*NativeEvaluation).*|// The Domain Service always answers.*)\\n', '', 5)
    def config = findTest('NativeWebConfigurationTest.java')
    replaceExactly(config, '(?m)^        assertThat\\(properties\\("application-test.yml"\\).getProperty\\("organization.integrations.evaluation.version"\\)\\)\\n                \\.endsWith\\(":1.0}"\\);\\n', '', 1)
    replaceExactly(config, ' \\|\\| key.startsWith\\("organization.integrations.evaluation."\\)', '', 1)
} else {
    removeNamed('domain', ['OrganizationDirectoryService.java', 'OrganizationUserBO.java',
            'OrganizationSchoolClassBO.java'])
    removeNamed('infrastructure', ['OrganizationDirectoryServiceImpl.java'])
    def context = findTest('EvaluationExternalFreeContextTest.java')
    replaceExactly(context, '(?m)^import .*?(?:OrganizationDirectoryService|OrganizationDirectoryClient|OrganizationDirectoryClientImpl|OrganizationDirectoryServiceImpl);\\n', '', 4)
    replaceExactly(context, '(?m)^    @Autowired private OrganizationDirectoryService organizationDirectory;\\n', '', 1)
    replaceExactly(context, '(?s)        assertThat\\(environment.getProperty\\("app.integrations.organization.enabled", Boolean.class\\)\\)\\n                \\.isFalse\\(\\);\\n        assertThat\\(organizationDirectory\\).*?\\n                \\.containsExactly\\("organizationDirectoryClient"\\);\\n', '', 1)
    def config = findTest('NativeServiceConfigurationTest.java')
    replaceExactly(config, '(?m)^        assertThat\\(properties\\("application-test.yml"\\).getProperty\\("app.integrations.organization.version"\\)\\)\\n                \\.endsWith\\(":1.0}"\\);\\n', '', 1)
    replaceExactly(config, ' \\|\\| key.startsWith\\("app.integrations.organization."\\)', '', 1)
}

def architecture = findTest('ArchetypeContractConvergenceTest.java')
def peerMethod = web ? 'externalContractStaysBehindTheNamedClientInInfrastructure'
        : 'externalClientsLiveInInfrastructureClientAndImpl'
replaceExactly(architecture, '(?s)    @Test\\n    void ' + peerMethod + '\\(\\).*?(?=    @Test\\n)', '', 1)
def peerDomain = web
        ? '(?:teaching/service/EvaluationQueryService|teaching/vos/Evaluation(?:Course|Exam|Score)BO)'
        : '(?:course/service/OrganizationDirectoryService|course/vos/Organization(?:User|SchoolClass)BO)'
replaceExactly(architecture, '(?m)^        assertExists\\("domain", "' + peerDomain + '\\.java"\\);\\n', '', web ? 4 : 3)

def resources = new File(module('starter'), 'src/main/resources')
['application.yml', 'application-dev.yml', 'application-test.yml', 'application-prod.yml'].each { name ->
    def yaml = new File(resources, name)
    replaceExactly(yaml, '(?m)^    ' + peer + ':\\n(?:^      .*\\n)*', '', 1)
    if (web && name == 'application.yml') {
        replaceExactly(yaml, '(?m)\\norganization:\\n  integrations:\\n?\\z', '\n', 1)
    }
}

def deploy = new File(project, 'deploy')
def peerEnvPattern = web
        ? ~/EVALUATION_(?:FACADE_|(?:COURSE|EXAM|SCORE)_FACADE_GROUP)/
        : ~/ORGANIZATION_FACADE_/
['env', 'compose'].each { directory ->
    new File(deploy, directory).eachFileRecurse { file ->
        if (!file.isFile()) return
        def original = file.getText('UTF-8')
        def lines = original.readLines().findAll { line ->
            !peerEnvPattern.matcher(line).find() &&
                    !line.startsWith("# Exact native ${peer} provider scope")
        }
        file.setText(lines.join('\n') + '\n', 'UTF-8')
    }
}
