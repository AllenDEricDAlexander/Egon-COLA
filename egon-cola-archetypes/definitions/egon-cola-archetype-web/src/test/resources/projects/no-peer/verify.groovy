import groovy.xml.XmlSlurper

def artifact = 'student-management-organization-standalone'
def project = [new File(basedir, "project/${artifact}"), new File(basedir, artifact)].find { it.isDirectory() }
assert project != null
def root = new XmlSlurper(false, false).parse(new File(project, 'pom.xml'))
assert root.modules.module*.text().size() == 7
assert !root.properties.'evaluation-facade.group-id'.text()
assert !root.properties.'evaluation-facade.artifact-id'.text()
assert !root.properties.'evaluation-facade.version'.text()
assert !root.properties.'evaluation-facade.package'.text()
def infra = new File(project, "${artifact}-infrastructure")
assert !new File(infra, 'pom.xml').text.contains('evaluation-facade')
assert !new File(project, 'pom.xml').text.contains('__NO_PEER_FACADE__')
def java = new File(infra, 'src/main/java')
def peerSources = []
java.eachFileRecurse { file -> if (file.isFile() && (file.path.contains('/client/evaluation/') || file.name == 'EvaluationQueryServiceImpl.java')) peerSources << file }
assert peerSources.isEmpty()
def domainSources = new File(project, "${artifact}-domain/src/main/java")
domainSources.eachFileRecurse { file ->
    if (file.isFile()) assert !file.name.startsWith('Evaluation')
}
['application.yml', 'application-dev.yml', 'application-test.yml', 'application-prod.yml'].each { name ->
    assert !new File(project, "${artifact}-starter/src/main/resources/${name}").text.contains('EVALUATION_FACADE_')
}
['env', 'compose'].each { directory ->
    new File(project, "deploy/${directory}").eachFileRecurse { file ->
        if (file.isFile()) assert !(file.text =~ /EVALUATION_(?:FACADE_|(?:COURSE|EXAM|SCORE)_FACADE_GROUP)/)
    }
}

return true
