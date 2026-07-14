# Archetype Verify Groovy Contract Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove release-version coupling from all archetype verifier scripts, make their POM assertions semantic, and expose the verifier Groovy runtime to IDEA without changing generated-project behavior.

**Architecture:** Each Maven Invoker `verify.groovy` remains standalone and uses small local XML assertion helpers. The archetypes parent supplies test-scoped Groovy modules aligned with Maven Archetype Plugin 3.4.1, while generated-project Facade fixtures remain isolated test artifacts.

**Tech Stack:** Maven 3, Maven Archetype Plugin 3.4.1, Groovy 4.0.28, Groovy `XmlSlurper`, Maven Invoker archetype integration tests.

## Global Constraints

- Do not hardcode the current Egon-COLA release value in a verifier.
- Keep Java 21 as the generated-project architecture baseline.
- Keep exact `1.0.0-fixture` assertions because they validate explicit integration-test input substitution.
- Keep `src/test/java/fixture` sources and their test-jar installation lifecycle.
- Do not introduce a shared verifier base script or a new build plugin.
- Do not start generated applications or open a browser.

---

### Task 1: Harden All Archetype Verifier Contracts

**Files:**
- Modify: `egon-cola-archetypes/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/pom.xml`
- Create: `docs/superpowers/plans/2026-07-14-archetype-verify-groovy-contract.md`

**Interfaces:**
- Consumes: Maven Invoker variables `basedir` and `context`, generated root POMs, and fixture coordinates from `archetype.properties`.
- Produces: standalone verifier scripts with `XmlSlurper`/`FileType` imports and an `assertEgonColaBom` closure that validates property-based BOM version wiring.

- [x] **Step 1: Record the failing baseline**

Run:

```bash
rg -n '<egon-cola.version>5\.2\.2</egon-cola.version>' \
  egon-cola-archetypes/*/src/test/resources/projects/basic/verify.groovy

bash ./mvnw -B -ntp -f egon-cola-archetypes/pom.xml \
  dependency:tree -Dscope=test -Dincludes=org.apache.groovy:groovy,org.apache.groovy:groovy-xml
```

Expected: the first command finds stale assertions in Light and Web; the dependency tree contains neither required Groovy artifact.

- [x] **Step 2: Expose the plugin-compatible Groovy API to IDEA**

Add `archetype.groovy.version` with value `4.0.28` to the archetypes parent and inherit test-scoped `org.apache.groovy:groovy` plus `org.apache.groovy:groovy-xml` dependencies in all three archetype modules.

- [x] **Step 3: Replace release-value assertions with a semantic BOM contract**

Each verifier must explicitly import:

```groovy
import groovy.io.FileType
import groovy.xml.XmlSlurper
```

Each verifier must parse the generated root POM and enforce this invariant:

```groovy
def assertEgonColaBom = { pom ->
    def egonColaVersion = pom.properties.'egon-cola.version'.text().trim()
    assert egonColaVersion: "Expected non-empty egon-cola.version"

    def bom = pom.dependencyManagement.dependencies.dependency.find {
        it.groupId.text() == "top.egon" &&
                it.artifactId.text() == "egon-cola-components-bom"
    }
    assert bom: "Expected top.egon:egon-cola-components-bom"
    assert bom.version.text() == '${egon-cola.version}':
            "Expected Egon-COLA BOM version to reference egon-cola.version"
    assert bom.type.text() == "pom"
    assert bom.scope.text() == "import"
}
```

Remove mutable third-party numeric version mirrors from Light and Web while retaining structural property/dependency checks and the exact Java 21 baseline. Keep Service fixture-value assertions unchanged.

- [x] **Step 4: Clarify Facade fixture lifecycle**

Add one concise XML comment before the existing fixture build plugin chain in Web and Service POMs explaining that it builds a minimal external Facade artifact required to compile the generated consumer during archetype integration tests. Do not change plugin executions or fixture sources.

- [x] **Step 5: Run focused and full validation**

Run:

```bash
bash ./mvnw -B -ntp -f egon-cola-archetypes/pom.xml \
  dependency:tree -Dscope=test -Dincludes=org.apache.groovy:groovy,org.apache.groovy:groovy-xml

bash ./mvnw -B -ntp -f egon-cola-archetypes/pom.xml clean integration-test

git diff --check

test -z "$(rg -l '<egon-cola.version>[0-9]' \
  egon-cola-archetypes/*/src/test/resources/projects/basic/verify.groovy)"
```

Expected: Groovy and Groovy XML appear in the test dependency tree; the full archetype reactor reports `BUILD SUCCESS`; static checks exit zero.

- [x] **Step 6: Commit the completed task once**

```bash
git add docs/superpowers/plans/2026-07-14-archetype-verify-groovy-contract.md \
  egon-cola-archetypes/pom.xml \
  egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy \
  egon-cola-archetypes/egon-cola-archetype-web/pom.xml \
  egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy \
  egon-cola-archetypes/egon-cola-archetype-service/pom.xml \
  egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy
git commit -m "test(archetype): harden generated project verification"
```
