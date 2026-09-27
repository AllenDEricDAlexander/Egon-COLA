import groovy.xml.XmlSlurper

def artifact = 'student-management-evaluation-standalone'
def project = [new File(basedir, "project/${artifact}"), new File(basedir, artifact)].find { it.isDirectory() }
assert project != null
def root = new XmlSlurper(false, false).parse(new File(project, 'pom.xml'))
assert root.modules.module*.text().size() == 7
assert !root.properties.'organization-facade.group-id'.text()
assert !root.properties.'organization-facade.artifact-id'.text()
assert !root.properties.'organization-facade.version'.text()
assert !root.properties.'organization-facade.package'.text()
def infra = new File(project, "${artifact}-infrastructure")
assert !new File(infra, 'pom.xml').text.contains('organization-facade')
assert !new File(project, 'pom.xml').text.contains('__NO_PEER_FACADE__')
def peerSources = []
new File(infra, 'src/main/java').eachFileRecurse { file ->
    if (file.isFile() && (file.path.contains('/client/organization/') || file.name == 'OrganizationDirectoryServiceImpl.java')) peerSources << file
}
assert peerSources.isEmpty()
new File(project, "${artifact}-domain/src/main/java").eachFileRecurse { file ->
    if (file.isFile()) assert !file.name.startsWith('Organization')
}
['application.yml', 'application-dev.yml', 'application-test.yml', 'application-prod.yml'].each { name ->
    assert !new File(project, "${artifact}-starter/src/main/resources/${name}").text.contains('ORGANIZATION_FACADE_')
}
['env', 'compose'].each { directory ->
    new File(project, "deploy/${directory}").eachFileRecurse { file ->
        if (file.isFile()) assert !file.text.contains('ORGANIZATION_FACADE_')
    }
}

return true
