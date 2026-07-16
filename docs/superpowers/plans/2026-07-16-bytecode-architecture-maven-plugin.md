# Bytecode Architecture Maven Plugin Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver the bytecode component foundation and a Maven Plugin that enforces exactly ARCH-001 through ARCH-010, then replace all generated-project ArchUnit checks in the light, web, and service archetypes.

**Architecture:** Add a new bytecode component aggregator containing a JDK-only public API, an internal ASM core, the Maven Plugin, integration-test fixtures, and benchmarks. The core parses class files into an immutable dependency graph without loading application classes; rule Specifications, baseline/cache services, and report writers consume that graph. Archetypes bind the plugin with explicit versions and explicit layer mappings.

**Tech Stack:** Java 21 production bytecode, Java 21/25 verification, Maven 3.9.14+, ASM 9.9.1, Maven Plugin API, JUnit 5, Maven Invoker Plugin, Jackson for plugin-side JSON only, JMH.

## Global Constraints

- Treat `docs/superpowers/specs/2026-07-15-bytecode-enhancement-design.md` as authoritative.
- Compile production sources with `--release 21`; parse and test real Java 25 class files.
- Use ASM 9.9.1 and expose no ASM type from a public Egon-COLA API.
- Use Java packages under `top.egon.cola.component.bytecode.*`.
- Implement exactly ARCH-001 through ARCH-010; do not preserve or recreate the five bespoke ArchUnit protections explicitly dropped by the design.
- Read class files only. Never load or initialize scanned application classes.
- Keep normal lifecycle execution read-only outside `target`; baseline replacement requires the explicit baseline goal and overwrite flag.
- Use content hashes, not timestamps or Git diffs, for incremental parsing.
- Publish API and core; manage only API in the BOM during this stage. The Maven Plugin uses an explicit version and is not placed in dependency management.
- Do not add a component-local `docs` directory. Update the component `README.md` and repository-level plan/spec only.
- Do not add database, Flyway, UI, browser, Docker, Attach API, Agent, or runtime code in this stage.
- At execution time create an isolated worktree with `superpowers:using-git-worktrees` and use one path-scoped commit per task.
- Do not start any generated application. Maven tests and short-lived compiler/Invoker processes are allowed.

## Pattern Boundary

Use Specification for the ten independent architecture rules, Strategy for layer resolution/report writers/failure policy, and a thin Maven Adapter around the Spring-free core. These are real configured variation points; do not add transformer factories, a generic rule-chain framework, or inheritance hierarchies because the explicit registry and ordered list are sufficient.

## Prerequisites And Stage Boundary

- Start from a branch containing commit `15db90f` or its equivalent approved design.
- This is Stage 1. Do not begin the Agent/Executor plan until this plan is implemented, reviewed, merged, and confirmed.
- Pin these build versions in the bytecode component root: `asm.version=9.9.1`, `maven.version=3.9.14`, `maven.plugin.annotations.version=3.15.2`, `maven.plugin.plugin.version=3.15.2`, `maven.invoker.plugin.version=3.9.1`, `jackson.version=2.19.4`, and `jmh.version=1.37`.

## File Structure

Create this stage-owned structure:

```text
egon-cola-components/egon-cola-component-bytecode/
├── pom.xml
├── README.md
├── egon-cola-component-bytecode-api/
│   ├── pom.xml
│   └── src/{main,test}/java/top/egon/cola/component/bytecode/api/architecture/
├── egon-cola-component-bytecode-core/
│   ├── pom.xml
│   └── src/{main,test}/java/top/egon/cola/component/bytecode/core/
├── egon-cola-component-bytecode-architecture-maven-plugin/
│   ├── pom.xml
│   └── src/{main,test}/java/top/egon/cola/component/bytecode/maven/
├── egon-cola-component-bytecode-test/
│   ├── pom.xml
│   └── src/it/architecture-*/
└── egon-cola-component-bytecode-benchmark/
    ├── pom.xml
    └── src/main/java/top/egon/cola/component/bytecode/benchmark/ArchitectureScanBenchmark.java
```

File ownership is fixed:

- `egon-cola-component-bytecode-api/src/main/java/top/egon/cola/component/bytecode/api/architecture`: stable rule, graph-view, layer, dependency, severity, and finding contracts.
- `egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/classfile`: ASM parsing and immutable metadata.
- `egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/architecture`: graph building, layer resolution, built-in rules, finding fingerprints.
- `egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/cache`: content-hash metadata cache codecs; no Maven types.
- `egon-cola-component-bytecode-architecture-maven-plugin/src/main/java/top/egon/cola/component/bytecode/maven/mojo`: Maven parameter and lifecycle adaptation only.
- `egon-cola-component-bytecode-architecture-maven-plugin/src/main/java/top/egon/cola/component/bytecode/maven/scan`: scan orchestration over class directories/JARs/reactor modules.
- `egon-cola-component-bytecode-architecture-maven-plugin/src/main/java/top/egon/cola/component/bytecode/maven/report`: Console/Text/JSON/HTML writers over one immutable result.
- `egon-cola-component-bytecode-architecture-maven-plugin/src/main/java/top/egon/cola/component/bytecode/maven/baseline`: baseline load/generate/compare.
- `src/it`: Maven Invoker projects proving goal behavior.

## Canonical Interfaces

All tasks use these names and signatures:

```java
package top.egon.cola.component.bytecode.api.architecture;

public interface ArchitectureRule {
    String id();
    ArchitectureSeverity severity();
    java.util.List<ArchitectureFinding> evaluate(ArchitectureRuleContext context);
}

public interface ArchitectureRuleContext {
    java.util.Collection<ArchitectureType> types();
    java.util.Collection<ArchitectureDependency> dependencies();
    java.util.Optional<ArchitectureType> findType(String className);
    ArchitectureFinding finding(
            ArchitectureRule rule,
            ArchitectureDependency dependency,
            String message,
            String suggestion
    );
}

public enum ArchitectureLayer {
    DOMAIN, APPLICATION, INFRASTRUCTURE, ADAPTER, FACADE, STARTER, COMMON, UNKNOWN
}

public enum ArchitectureSeverity { ERROR, WARNING, INFO }
```

`ArchitectureType`, `ArchitectureDependency`, and `ArchitectureFinding` are records with the exact fields below:

```java
public record ArchitectureType(
        String module,
        String className,
        ArchitectureLayer layer,
        java.util.Set<String> annotations,
        boolean interfaceType
) { }

public record ArchitectureDependency(
        String module,
        String sourceClass,
        String sourceMember,
        String sourceDescriptor,
        ArchitectureLayer sourceLayer,
        String targetClass,
        String targetMember,
        String targetDescriptor,
        ArchitectureLayer targetLayer,
        DependencyKind dependencyKind,
        LocationKind locationKind,
        Integer lineNumber
) { }

public record ArchitectureFinding(
        String ruleId,
        ArchitectureSeverity severity,
        String module,
        ArchitectureLayer sourceLayer,
        ArchitectureLayer targetLayer,
        String sourceClass,
        String sourceMember,
        String sourceDescriptor,
        String targetClass,
        String targetMember,
        String targetDescriptor,
        DependencyKind dependencyKind,
        LocationKind locationKind,
        Integer lineNumber,
        String message,
        String suggestion
) { }
```

## Task 1: Component Shell And Public Architecture Contracts

**Files:**
- Modify: `egon-cola-components/pom.xml`
- Modify: `egon-cola-components/egon-cola-components-bom/pom.xml`
- Create: `egon-cola-components/egon-cola-component-bytecode/pom.xml`
- Create: `egon-cola-components/egon-cola-component-bytecode/README.md`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-api/pom.xml`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-api/src/main/java/top/egon/cola/component/bytecode/api/architecture/ArchitectureLayer.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-api/src/main/java/top/egon/cola/component/bytecode/api/architecture/ArchitectureSeverity.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-api/src/main/java/top/egon/cola/component/bytecode/api/architecture/DependencyKind.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-api/src/main/java/top/egon/cola/component/bytecode/api/architecture/LocationKind.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-api/src/main/java/top/egon/cola/component/bytecode/api/architecture/ArchitectureType.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-api/src/main/java/top/egon/cola/component/bytecode/api/architecture/ArchitectureDependency.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-api/src/main/java/top/egon/cola/component/bytecode/api/architecture/ArchitectureFinding.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-api/src/main/java/top/egon/cola/component/bytecode/api/architecture/ArchitectureRule.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-api/src/main/java/top/egon/cola/component/bytecode/api/architecture/ArchitectureRuleContext.java`
- Test: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-api/src/test/java/top/egon/cola/component/bytecode/api/architecture/ArchitectureContractsTest.java`

**Interfaces:**
- Produces the canonical architecture API above.
- Produces Maven artifact `top.egon:egon-cola-component-bytecode-api:${project.version}`.
- Later tasks consume the API without importing `org.objectweb.asm` or Maven classes.

- [ ] **Step 1: Write the failing boundary test**

```java
package top.egon.cola.component.bytecode.api.architecture;

import org.junit.jupiter.api.Test;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ArchitectureContractsTest {

    @Test
    void findingContractKeepsStableFieldOrder() {
        assertEquals(
                java.util.List.of("ruleId", "severity", "module", "sourceLayer", "targetLayer",
                        "sourceClass", "sourceMember", "sourceDescriptor", "targetClass",
                        "targetMember", "targetDescriptor", "dependencyKind", "locationKind",
                        "lineNumber", "message", "suggestion"),
                Arrays.stream(ArchitectureFinding.class.getRecordComponents())
                        .map(RecordComponent::getName)
                        .toList()
        );
    }

    @Test
    void publicApiDoesNotExposeAsmOrMavenTypes() {
        String signatures = Arrays.stream(ArchitectureRule.class.getMethods())
                .map(Object::toString)
                .reduce("", String::concat);
        assertFalse(signatures.contains("org.objectweb.asm"));
        assertFalse(signatures.contains("org.apache.maven"));
    }
}
```

- [ ] **Step 2: Run the test and verify the module is absent**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-api -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=ArchitectureContractsTest test
```

Expected: FAIL because the bytecode component/module does not exist.

- [ ] **Step 3: Add the component reactor and contracts**

Add the component to `egon-cola-components/pom.xml` after Method Extension. The component root initially contains only:

```xml
<modules>
    <module>egon-cola-component-bytecode-api</module>
    <module>egon-cola-component-bytecode-core</module>
    <module>egon-cola-component-bytecode-architecture-maven-plugin</module>
    <module>egon-cola-component-bytecode-test</module>
    <module>egon-cola-component-bytecode-benchmark</module>
</modules>
```

Add only this Stage-1 BOM entry:

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-bytecode-api</artifactId>
    <version>${project.version}</version>
</dependency>
```

Implement the canonical records/interfaces and these enums:

```java
public enum DependencyKind {
    EXTENDS, IMPLEMENTS, FIELD, PARAMETER, RETURN, THROWS, SIGNATURE,
    ANNOTATION, NEW, ARRAY, CAST, INSTANCEOF, FIELD_READ, FIELD_WRITE,
    METHOD_CALL, CONSTRUCTOR_CALL, METHOD_HANDLE, INVOKEDYNAMIC,
    LAMBDA_TARGET, CONSTANT_DYNAMIC, CONSTANT_POOL
}

public enum LocationKind { CLASS, FIELD, METHOD, INSTRUCTION }
```

The component root manages the pinned versions in Prerequisites and sets deployment skip only on test/benchmark modules, not API/core/plugin.

- [ ] **Step 4: Run the focused test**

Run the command from Step 2.

Expected: PASS; dependency tree for API contains JUnit only in test scope and no ASM/Maven/Spring dependency.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/pom.xml \
  egon-cola-components/egon-cola-components-bom/pom.xml \
  egon-cola-components/egon-cola-component-bytecode
git commit -m "feat(bytecode): add architecture component contracts"
```

## Task 2: ASM Metadata Reader And Dependency Graph

**Files:**
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/pom.xml`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/classfile/ClassMetadata.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/classfile/ClassDependency.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/classfile/ClassMetadataReader.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/classfile/AsmClassMetadataReader.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/architecture/ArchitectureGraph.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/architecture/ArchitectureGraphBuilder.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/architecture/DefaultArchitectureRuleContext.java`
- Test: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/test/java/top/egon/cola/component/bytecode/core/classfile/AsmClassMetadataReaderTest.java`
- Test fixture: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/test/java/top/egon/cola/component/bytecode/core/classfile/fixture/DependencyFixture.java`

**Interfaces:**
- Consumes the Stage-1 API records.
- Produces `ClassMetadataReader.read(String module, byte[] classBytes)` and `ArchitectureGraphBuilder.build(Collection<ClassMetadata>, LayerResolver)`.

- [ ] **Step 1: Write a failing metadata coverage test**

The fixture must contain superclass/interface, field, generic signature, annotation, exception, allocation, cast, method call, lambda, method reference, and string-free class literal dependencies. Assert the parser emits at least these kinds:

```java
assertEquals(
        Set.of(DependencyKind.EXTENDS, DependencyKind.IMPLEMENTS, DependencyKind.FIELD,
                DependencyKind.PARAMETER, DependencyKind.RETURN, DependencyKind.THROWS,
                DependencyKind.SIGNATURE, DependencyKind.ANNOTATION, DependencyKind.NEW,
                DependencyKind.CAST, DependencyKind.METHOD_CALL, DependencyKind.LAMBDA_TARGET,
                DependencyKind.METHOD_HANDLE, DependencyKind.CONSTANT_POOL),
        metadata.dependencies().stream().map(ClassDependency::kind).collect(Collectors.toSet())
);
```

Also load a fixture class whose static initializer throws and assert parsing its bytes does not initialize it.

- [ ] **Step 2: Run the focused test and observe failure**

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=AsmClassMetadataReaderTest test
```

Expected: FAIL because the core reader is missing.

- [ ] **Step 3: Implement the immutable parser**

Use this boundary:

```java
public interface ClassMetadataReader {
    ClassMetadata read(String module, byte[] classBytes);
}

public record ClassMetadata(
        String module,
        String className,
        String superName,
        Set<String> interfaces,
        Set<String> annotations,
        boolean interfaceType,
        List<ClassDependency> dependencies
) { }
```

`AsmClassMetadataReader` must use `ClassReader.SKIP_FRAMES`, keep debug information when present, visit constant-dynamic/bootstrap arguments recursively, convert internal names to dotted names once, and never call `Class.forName`.

- [ ] **Step 4: Build the immutable graph and rerun tests**

`ArchitectureGraphBuilder` deduplicates identical edges but retains different source member/location provenance. `DefaultArchitectureRuleContext.finding(...)` copies dependency provenance and supplies the rule ID/severity.

Run Step 2 and:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core -am test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core
git commit -m "feat(bytecode): parse architecture dependency metadata"
```

## Task 3: Layer Resolution And Ten Built-In Rule Specifications

**Files:**
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/architecture/LayerMapping.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/architecture/LayerResolver.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/architecture/DefaultLayerResolver.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/architecture/rule/AbstractDependencyRule.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/architecture/rule/Arch001DomainDirectionRule.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/architecture/rule/Arch002DomainTechnicalFrameworkRule.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/architecture/rule/Arch003ApplicationDirectionRule.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/architecture/rule/Arch004ApplicationPersistenceRule.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/architecture/rule/Arch005FacadeContractRule.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/architecture/rule/Arch006StarterBusinessRule.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/architecture/rule/Arch007CommonBusinessRule.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/architecture/rule/Arch008AdapterInfrastructureImplementationRule.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/architecture/rule/Arch009DomainPersistenceRule.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/architecture/rule/Arch010FacadeImplementationRule.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/architecture/rule/BuiltInArchitectureRules.java`
- Test: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/test/java/top/egon/cola/component/bytecode/core/architecture/DefaultLayerResolverTest.java`
- Test: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/test/java/top/egon/cola/component/bytecode/core/architecture/BuiltInArchitectureRulesTest.java`

**Interfaces:**
- Consumes `ArchitectureGraph`.
- Produces `BuiltInArchitectureRules.all()` in strict ARCH-001..010 order.

- [ ] **Step 1: Write one isolated failing fixture per rule**

Use a parameterized test with ten graph fixtures:

```java
@ParameterizedTest
@MethodSource("violations")
void eachRuleReportsOnlyItsOwnFixture(String ruleId, ArchitectureRuleContext context) {
    ArchitectureRule rule = BuiltInArchitectureRules.all().stream()
            .filter(candidate -> candidate.id().equals(ruleId))
            .findFirst().orElseThrow();
    List<ArchitectureFinding> findings = rule.evaluate(context);
    assertEquals(1, findings.size());
    assertEquals(ruleId, findings.getFirst().ruleId());
}
```

Add explicit assertions for ARCH-002's default denylist, ARCH-004 persistence types/package patterns, ARCH-005 Facade allowlist, ARCH-008 implementation-name/package matching, ARCH-009 domain repository interfaces, and ARCH-010 configured Adapter packages.

- [ ] **Step 2: Run the rule tests and observe failure**

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=DefaultLayerResolverTest,BuiltInArchitectureRulesTest test
```

Expected: FAIL because resolvers and rules are missing.

- [ ] **Step 3: Implement resolution precedence**

```java
public final class DefaultLayerResolver implements LayerResolver {
    @Override
    public ArchitectureLayer resolve(String module, String className, LayerMapping mapping) {
        return mapping.explicitModules().entrySet().stream()
                .filter(entry -> entry.getValue().contains(module))
                .map(Map.Entry::getKey)
                .findFirst()
                .or(() -> mapping.packagePatterns().entrySet().stream()
                        .filter(entry -> entry.getValue().stream().anyMatch(pattern -> matches(pattern, className)))
                        .map(Map.Entry::getKey).findFirst())
                .or(() -> layerFromModuleSuffix(module))
                .orElse(ArchitectureLayer.UNKNOWN);
    }
}
```

Reject duplicate explicit mappings and ambiguous package-pattern matches during configuration validation.

- [ ] **Step 4: Implement exactly ten rules and rerun**

`BuiltInArchitectureRules.all()` returns the ten named rule instances above in numeric order. Each rule uses graph data only, returns deterministic messages/suggestions, and never creates an eleventh rule for configuration denylists or package mappings.

Run Step 2. Expected: PASS and exactly ten rule IDs.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core
git commit -m "feat(bytecode): enforce ten COLA architecture rules"
```

## Task 4: Deterministic Results And Four Report Writers

**Files:**
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-architecture-maven-plugin/pom.xml`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-architecture-maven-plugin/src/main/java/top/egon/cola/component/bytecode/maven/result/ArchitectureCheckResult.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-architecture-maven-plugin/src/main/java/top/egon/cola/component/bytecode/maven/result/FindingOrder.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-architecture-maven-plugin/src/main/java/top/egon/cola/component/bytecode/maven/report/ArchitectureReportWriter.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-architecture-maven-plugin/src/main/java/top/egon/cola/component/bytecode/maven/report/ConsoleReportWriter.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-architecture-maven-plugin/src/main/java/top/egon/cola/component/bytecode/maven/report/TextReportWriter.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-architecture-maven-plugin/src/main/java/top/egon/cola/component/bytecode/maven/report/JsonReportWriter.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-architecture-maven-plugin/src/main/java/top/egon/cola/component/bytecode/maven/report/HtmlReportWriter.java`
- Test: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-architecture-maven-plugin/src/test/java/top/egon/cola/component/bytecode/maven/report/ArchitectureReportWriterTest.java`

**Interfaces:**
- Produces one immutable sorted `ArchitectureCheckResult` consumed by all writers.
- Writers output under `${project.build.directory}/egon-cola-architecture`.

- [ ] **Step 1: Write a failing cross-format parity test**

Build a result with two rules, mixed severities, missing member/line data, and characters requiring JSON/HTML escaping. Assert all formats expose the same rule/severity/count totals, JSON parses, HTML escapes values, and Text uses `<class>`/`-1` placeholders.

- [ ] **Step 2: Run the report test**

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-architecture-maven-plugin -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=ArchitectureReportWriterTest test
```

Expected: FAIL because plugin/result writers are absent.

- [ ] **Step 3: Implement deterministic result ordering**

```java
public record ArchitectureCheckResult(
        List<ArchitectureFinding> findings,
        Map<String, Long> countsByRule,
        Map<ArchitectureSeverity, Long> countsBySeverity
) {
    public ArchitectureCheckResult {
        findings = findings.stream().sorted(FindingOrder.COMPARATOR).toList();
        countsByRule = Map.copyOf(countsByRule);
        countsBySeverity = Map.copyOf(countsBySeverity);
    }
}
```

Sort by rule, module, source class/member, dependency kind, target class/member.

- [ ] **Step 4: Implement all writers and rerun**

Use Jackson only inside the Maven Plugin for JSON. HTML is a self-contained UTF-8 document with no script or external asset. Console writes through Maven `Log`; the other writers use atomic temp-file replacement.

Expected: focused test PASS.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-architecture-maven-plugin
git commit -m "feat(bytecode): add architecture reports"
```

## Task 5: Baseline Fingerprints And Content-Hash Cache

**Files:**
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/architecture/FindingFingerprint.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/cache/ClassMetadataCacheKey.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/cache/ClassMetadataCacheEntry.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-architecture-maven-plugin/src/main/java/top/egon/cola/component/bytecode/maven/baseline/ArchitectureBaseline.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-architecture-maven-plugin/src/main/java/top/egon/cola/component/bytecode/maven/baseline/ArchitectureBaselineRepository.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-architecture-maven-plugin/src/main/java/top/egon/cola/component/bytecode/maven/baseline/BaselineComparator.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-architecture-maven-plugin/src/main/java/top/egon/cola/component/bytecode/maven/cache/ArchitectureCacheRepository.java`
- Test: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/test/java/top/egon/cola/component/bytecode/core/architecture/FindingFingerprintTest.java`
- Test: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-architecture-maven-plugin/src/test/java/top/egon/cola/component/bytecode/maven/baseline/BaselineComparatorTest.java`
- Test: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-architecture-maven-plugin/src/test/java/top/egon/cola/component/bytecode/maven/cache/ArchitectureCacheRepositoryTest.java`

**Interfaces:**
- Fingerprint input excludes line, message, suggestion, and report order.
- Cache key is SHA-256 class content + parser schema + ASM baseline + effective scan-config digest.

- [ ] **Step 1: Write failing stability and invalidation tests**

Assert line/message changes keep the same fingerprint, target/member changes alter it, stale baseline entries are returned separately, normal comparison never writes the baseline, same bytes hit cache, and one-byte/config/schema changes miss cache.

- [ ] **Step 2: Run the focused tests**

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-architecture-maven-plugin -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=FindingFingerprintTest,BaselineComparatorTest,ArchitectureCacheRepositoryTest test
```

Expected: FAIL because baseline/cache types are absent.

- [ ] **Step 3: Implement fingerprint and baseline comparison**

```java
public record BaselineComparison(
        List<ArchitectureFinding> accepted,
        List<ArchitectureFinding> newFindings,
        Set<String> staleFingerprints
) { }
```

Default path is `${maven.multiModuleProjectDirectory}/.egon-cola/architecture-baseline.json`. `generate-baseline` is the only writer; existing content requires `overwrite=true`.

- [ ] **Step 4: Implement cache beneath target and rerun**

Cache parsed metadata, not rule results. Every invocation reconstructs the complete graph and reruns every rule. Corrupt cache entries are deleted and reparsed; report the recovery at debug level.

Expected: all focused tests PASS.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-bytecode
git commit -m "feat(bytecode): add architecture baseline and cache"
```

## Task 6: Maven Goals, Failure Policy, And Invoker Fixtures

**Files:**
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-architecture-maven-plugin/src/main/java/top/egon/cola/component/bytecode/maven/mojo/AbstractArchitectureMojo.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-architecture-maven-plugin/src/main/java/top/egon/cola/component/bytecode/maven/mojo/ArchitectureCheckMojo.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-architecture-maven-plugin/src/main/java/top/egon/cola/component/bytecode/maven/mojo/ArchitectureCheckReactorMojo.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-architecture-maven-plugin/src/main/java/top/egon/cola/component/bytecode/maven/mojo/GenerateBaselineMojo.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-architecture-maven-plugin/src/main/java/top/egon/cola/component/bytecode/maven/config/ArchitecturePluginConfiguration.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-architecture-maven-plugin/src/main/java/top/egon/cola/component/bytecode/maven/scan/ArchitectureScanner.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-architecture-maven-plugin/src/main/java/top/egon/cola/component/bytecode/maven/scan/MavenScanInputResolver.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-architecture-maven-plugin/src/main/java/top/egon/cola/component/bytecode/maven/rule/ArchitectureRuleLoader.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/pom.xml`
- Create fixture tree: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/it/architecture-clean/`
- Create fixture tree: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/it/architecture-arch-001/`
- Create fixture tree: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/it/architecture-arch-002/`
- Create fixture tree: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/it/architecture-arch-003/`
- Create fixture tree: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/it/architecture-arch-004/`
- Create fixture tree: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/it/architecture-arch-005/`
- Create fixture tree: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/it/architecture-arch-006/`
- Create fixture tree: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/it/architecture-arch-007/`
- Create fixture tree: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/it/architecture-arch-008/`
- Create fixture tree: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/it/architecture-arch-009/`
- Create fixture tree: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/it/architecture-arch-010/`
- Create fixture tree: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/it/architecture-baseline/`
- Create fixture tree: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/it/architecture-reactor/`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/it/settings.xml`

**Interfaces:**
- Goals: `bytecode-architecture:check`, `check-reactor`, `generate-baseline`.
- Failure policies: `FAIL`, `WARN`, `REPORT_ONLY`; default `FAIL`.

- [ ] **Step 1: Create failing Invoker projects**

Each project has `pom.xml`, Java fixture, `invoker.properties`, and `verify.groovy`. The illegal fixtures expect build failure and verify the matching `ARCH-xxx` in JSON/Text. The clean project verifies all four reports and absence of class-initializer side effects. Add scan-input fixtures for dependency JARs, missing debug information, empty modules, unknown layers, and a multi-module reactor.

- [ ] **Step 2: Run the Invoker suite and observe missing goals**

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test -am verify
```

Expected: FAIL because Maven goal descriptors and scan orchestration are missing.

- [ ] **Step 3: Implement Maven parameters and goals**

Use these Mojo annotations:

```java
@Mojo(name = "check", defaultPhase = LifecyclePhase.VERIFY,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public final class ArchitectureCheckMojo extends AbstractArchitectureMojo { }

@Mojo(name = "check-reactor", defaultPhase = LifecyclePhase.VERIFY,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        aggregator = true, threadSafe = true)
public final class ArchitectureCheckReactorMojo extends AbstractArchitectureMojo { }

@Mojo(name = "generate-baseline", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        aggregator = true, threadSafe = true)
public final class GenerateBaselineMojo extends AbstractArchitectureMojo { }
```

Parameters include output directory, baseline path, overwrite, scanTests, scanDependencies, additional class directories, module mappings, package mappings, framework denylist/allowlist, Facade implementation packages, failure policy, unknown-layer policy, and cache enabled.

`ArchitectureRuleLoader` returns built-ins first, then custom rules discovered with `ServiceLoader<ArchitectureRule>` from the Maven plugin realm. Reject duplicate IDs, blank IDs, unknown severities, and custom rules that throw during initialization. Custom plugin dependencies are the only external rule loading mechanism; there is no arbitrary bytecode Transformer SPI.

- [ ] **Step 4: Complete scanner error semantics and rerun**

Unreadable class, rule exception, or report-write failure throws `MojoExecutionException`. Empty class directories and unknown layers follow explicit policies. `REPORT_ONLY` always writes reports and never fails. Normal goals never mutate the baseline.

Expected: all Invoker fixtures PASS.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-bytecode
git commit -m "feat(bytecode): add architecture Maven goals"
```

## Task 7: Replace ArchUnit In All Three Archetypes

**Files:**
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/pom.xml`
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/ArchitectureDependencyTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/pom.xml`
- Delete: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/test/java/starter/ArchitectureDependencyTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter/pom.xml`
- Delete: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/test/java/starter/ServiceArchitectureDependencyTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`
- No archetype metadata file changes: all three descriptors already include Java tests by wildcard, so deleting the concrete ArchUnit files is sufficient.

**Interfaces:**
- Light binds `check` in its single module.
- Web/service bind `check-reactor` in terminal Starter modules.
- All plugin declarations use `<version>${egon-cola.version}</version>`.

- [ ] **Step 1: Change verifiers to require plugin and forbid ArchUnit**

For each `verify.groovy`, assert:

```groovy
assertMissing("src/test/java/it/pkg/ArchitectureDependencyTest.java")
assertMissing("student-management-organization-starter/src/test/java/it/pkg/starter/ArchitectureDependencyTest.java")
assertMissing("student-management-evaluation-starter/src/test/java/it/pkg/starter/ServiceArchitectureDependencyTest.java")
assert !allPomText.contains("archunit-junit5")
assert !allPomText.contains("archunit.version")
assert plugin.groupId.text() == "top.egon"
assert plugin.artifactId.text() == "egon-cola-component-bytecode-architecture-maven-plugin"
assert plugin.version.text() == '${egon-cola.version}'
assertFile("target/egon-cola-architecture/architecture-report.json")
assertFile("student-management-organization-starter/target/egon-cola-architecture/architecture-report.json")
assertFile("student-management-evaluation-starter/target/egon-cola-architecture/architecture-report.json")
```

Each archetype verifier includes only the assertion pair for its own generated layout; the combined block above documents all three exact paths.

Add an illegal-dependency fixture to each generated project test tree and assert a direct plugin invocation fails with the intended standard rule.

- [ ] **Step 2: Install the plugin and run archetype IT to observe old templates fail**

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-architecture-maven-plugin -am install
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml clean integration-test
```

Expected: FAIL because templates still contain ArchUnit and lack plugin configuration.

- [ ] **Step 3: Replace template dependencies and tests**

Remove every `archunit.version`, `archunit-junit5`, and architecture test file. Configure explicit mappings for the real layouts, including light's `start` package and web/service module suffixes. Do not add mappings for the five approved-lost bespoke rules.

Web/service Starter plugin execution:

```xml
<plugin>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-bytecode-architecture-maven-plugin</artifactId>
    <version>${egon-cola.version}</version>
    <executions>
        <execution>
            <id>cola-architecture-check</id>
            <phase>verify</phase>
            <goals><goal>check-reactor</goal></goals>
        </execution>
    </executions>
</plugin>
```

- [ ] **Step 4: Run generated-project verification**

Run Step 2 again.

Expected: PASS; generated projects contain no ArchUnit references and produce architecture reports.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-archetypes
git commit -m "feat(archetypes): replace ArchUnit with bytecode rules"
```

## Task 8: Java 25 Fixture, Benchmark, CI, README, And Final Gate

**Files:**
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-benchmark/pom.xml`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-benchmark/src/main/java/top/egon/cola/component/bytecode/benchmark/ArchitectureScanBenchmark.java`
- Add Java-25 fixture under: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/it/architecture-java25/`
- Modify: `.github/workflows/ci.yaml`
- Modify: `.github/workflows/ci_java_compatibility.yaml`
- Complete: `egon-cola-components/egon-cola-component-bytecode/README.md`

**Interfaces:**
- Benchmark reports full scanning of 1,000 representative classes; controlled target is at or below two seconds.
- CI clean scans disable cache and compile a real `--release 25` fixture on JDK 25.

- [ ] **Step 1: Add failing compatibility and benchmark smoke assertions**

The Java-25 Invoker fixture compiles with `<maven.compiler.release>25</maven.compiler.release>` and runs `bytecode-architecture:check`. The JMH smoke invocation must discover `ArchitectureScanBenchmark.scanOneThousandClasses`.

- [ ] **Step 2: Run Java-21 local-compatible checks**

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode -am test
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-benchmark -am -DskipTests package
```

Expected before implementation: FAIL due to missing benchmark/fixture.

- [ ] **Step 3: Implement benchmark and CI matrix wiring**

JMH generates 1,000 deterministic class byte arrays before measurement; the benchmark method measures parse, graph build, ten-rule evaluation, and result creation. It must not include fixture generation in measured time.

CI adds a focused plugin/Invoker step on Java 21 and 25 and uses `-DegonArchitecture.cache.enabled=false`. The Java-25 job compiles the release-25 fixture before scanning.

- [ ] **Step 4: Document usage and run final validation**

README must document goals, configuration precedence, layer mappings, all ten rules, report paths, failure policies, baseline workflow, cache key, explicit plugin version, and the approved ArchUnit coverage loss.

Run in order:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode -am test
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test -am verify
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-architecture-maven-plugin -am install
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml clean integration-test
./mvnw -B -ntp -f egon-cola-components/pom.xml test
./mvnw -B -ntp clean integration-test
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-api -am dependency:tree -Dincludes=org.ow2.asm,org.apache.maven,org.springframework
git diff --check
```

Expected: all commands PASS; API dependency filter prints no compile/runtime ASM/Maven/Spring dependency. Do not start an application.

- [ ] **Step 5: Commit**

```bash
git add .github/workflows \
  egon-cola-components/egon-cola-component-bytecode
git commit -m "test(bytecode): verify architecture plugin compatibility"
```

## Stage Completion Checklist

- [ ] Component reactor contains API/core/plugin/test/benchmark only for this stage.
- [ ] BOM manages API only; core and Maven Plugin are not BOM entries.
- [ ] ASM is pinned to 9.9.1 and absent from public signatures.
- [ ] Scanner covers all dependency kinds from the design and never initializes target classes.
- [ ] Built-in rule registry contains exactly ARCH-001..010.
- [ ] Console/Text/JSON/HTML totals are identical and deterministically ordered.
- [ ] Baseline and content-hash cache semantics pass corruption/invalidation tests.
- [ ] Light/web/service generated projects use the explicit-version plugin and contain no ArchUnit.
- [ ] The five bespoke ArchUnit protections are absent by approved design, not accidentally reintroduced.
- [ ] Java 21/25, Invoker, archetype integration, reactor, dependency-boundary, hygiene, and performance evidence are recorded.
- [ ] Stop and request review before Stage 2.
