import groovy.xml.XmlSlurper

// Called by the generated-project verifier after packaging; no application startup.
def verifyOpenDependencyBoundary = { File projectDir ->
    def pom = new XmlSlurper(false, false).parse(new File(projectDir, 'pom.xml'))
    assert pom.parent.groupId.text() == 'top.egon'
    assert pom.parent.artifactId.text() == 'egon-cola-archetypes-parent'
    assert pom.parent.version.text() == pom.properties.'egon-cola.version'.text()
    assert pom.parent.relativePath.size() == 1 && !pom.parent.relativePath.text()
    def dependencies = pom.dependencies.dependency.findAll { !(it.scope.text() in ['test', 'provided']) }
    def ids = dependencies.artifactId*.text() as Set
    ['egon-cola-component-common-core', 'egon-cola-component-common-id-starter',
     'egon-cola-component-common-mybatis-plus-spring-boot-starter',
     'egon-cola-component-dynamic-thread-pool-starter', 'spring-cloud-starter-bootstrap',
     'spring-cloud-starter-alibaba-nacos-config', 'spring-cloud-starter-alibaba-nacos-discovery',
     'springdoc-openapi-starter-webmvc-ui'].each { required ->
        assert ids.contains(required): "Missing Light Open dependency ${required}"
    }
    def forbidden = ['egon-cola-component-rpc-', 'egon-cola-tianshu', 'dubbo-']
    assert !ids.any { id -> forbidden.any { id.startsWith(it) } }
    def packaged = new File(projectDir, 'target').listFiles()?.find {
        it.name.endsWith('.jar') && !it.name.endsWith('-sources.jar') && !it.name.endsWith('-javadoc.jar')
    }
    assert packaged: 'Expected packaged Light Open runtime'
    def libraries = [] as Set
    new java.util.jar.JarFile(packaged).withCloseable { archive ->
        archive.entries().each { entry ->
            if (entry.name.startsWith('BOOT-INF/lib/')) libraries << entry.name.substring('BOOT-INF/lib/'.length())
        }
    }
    assert libraries.any { it.startsWith('egon-cola-component-common-core-') }
    assert libraries.any { it.startsWith('nacos-client-') }
    assert libraries.any { it.startsWith('springdoc-openapi-starter-webmvc-ui-') }
    assert !libraries.any { library -> forbidden.any { library.startsWith(it) } }
    true
}
verifyOpenDependencyBoundary
