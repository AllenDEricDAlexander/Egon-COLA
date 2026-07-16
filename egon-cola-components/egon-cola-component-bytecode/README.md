# Egon COLA Bytecode Component

The bytecode component checks compiled classes against the standard Egon COLA architecture rules without loading or initializing application classes. Its public API is JDK-only; ASM, Maven, and JSON serialization remain implementation details.

## Maven Plugin

Always declare the plugin version explicitly. Bind `check` in a single-module project or bind `check-reactor` in the terminal module of a multi-module reactor:

```xml
<plugin>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-bytecode-architecture-maven-plugin</artifactId>
    <version>${egon-cola.version}</version>
    <configuration>
        <packageMappings>
            <com.example.domain..>DOMAIN</com.example.domain..>
            <com.example.application..>APPLICATION</com.example.application..>
            <com.example.infrastructure..>INFRASTRUCTURE</com.example.infrastructure..>
            <com.example.adapter..>ADAPTER</com.example.adapter..>
            <com.example.facade..>FACADE</com.example.facade..>
            <com.example.starter..>STARTER</com.example.starter..>
            <com.example.common..>COMMON</com.example.common..>
        </packageMappings>
    </configuration>
    <executions>
        <execution>
            <id>cola-architecture-check</id>
            <phase>verify</phase>
            <goals>
                <goal>check-reactor</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

The goals are:

- `bytecode-architecture:check`: scan the current module.
- `bytecode-architecture:check-reactor`: scan compiled classes in the current Maven reactor.
- `bytecode-architecture:generate-baseline`: explicitly write the current finding fingerprints to the baseline.

Normal checks only write beneath `target` and never add findings to the baseline.

## Configuration

Maven user properties override matching XML configuration, XML overrides plugin defaults, and rule-specific lists replace their defaults when supplied. Layer resolution then uses this independent precedence:

```text
explicit module mapping > package mapping > module-name suffix > UNKNOWN
```

Supported layers are `DOMAIN`, `APPLICATION`, `INFRASTRUCTURE`, `ADAPTER`, `FACADE`, `STARTER`, `COMMON`, and `UNKNOWN`. Important options are:

| Option | Default | Purpose |
| --- | --- | --- |
| `moduleMappings` | empty | Map an exact Maven artifact ID to a layer. |
| `packageMappings` | empty | Map package patterns such as `com.example.domain..` to a layer. |
| `scanTests` / `egonArchitecture.scanTests` | `false` | Include `target/test-classes`. |
| `scanDependencies` / `egonArchitecture.scanDependencies` | `false` | Include dependency JARs. |
| `additionalClassDirectories` | empty | Include additional compiled-class directories. |
| `frameworkDenylist` | built-in Spring/Jakarta technical prefixes | Replace the Domain technical-framework denylist. |
| `frameworkAllowlist` | empty | Allow specific technical prefixes before applying the denylist. |
| `facadeImplementationPackages` | `..adapter..` | Define allowed packages for Facade implementations. |
| `failurePolicy` / `egonArchitecture.failurePolicy` | `FAIL` | Choose `FAIL`, `WARN`, or `REPORT_ONLY`. |
| `unknownLayerPolicy` / `egonArchitecture.unknownLayerPolicy` | `WARN` | Choose `FAIL`, `WARN`, or `IGNORE`. |
| `cacheEnabled` / `egonArchitecture.cache.enabled` | `true` | Enable parsed-class metadata caching. |

## Standard Rules

The built-in registry contains exactly these ten Specifications:

1. `ARCH-001`: Domain must not depend on Application, Infrastructure, Adapter, Facade, or Starter.
2. `ARCH-002`: Domain must remain free from configured technical frameworks.
3. `ARCH-003`: Application must not depend on Infrastructure or Adapter.
4. `ARCH-004`: Application must not directly access persistence frameworks or infrastructure mapper/repository implementations.
5. `ARCH-005`: Facade must remain a self-contained contract module.
6. `ARCH-006`: Starter must not contain or directly reference Domain or Application business implementations.
7. `ARCH-007`: Common must not depend on business modules.
8. `ARCH-008`: Adapter must not directly call Infrastructure implementations.
9. `ARCH-009`: Domain may define repository interfaces but must not contain JPA entities, mapper implementations, SQL sessions, or infrastructure repository implementations.
10. `ARCH-010`: Facade implementations must reside in configured Adapter packages.

The scanner covers inheritance, fields, parameters, return and exception types, generic signatures, annotations and values, local types, allocations, arrays, casts, `instanceof`, field access, method and constructor calls, method handles, `invokedynamic`, Lambda targets, `ConstantDynamic`, and constant-pool class references.

## Reports And Failure Policies

Every check writes deterministically ordered Text, JSON, and HTML reports from the same immutable result model and prints the same totals to the console. The default directory is:

```text
${project.build.directory}/egon-cola-architecture
```

Files are `architecture-report.txt`, `architecture-report.json`, and `architecture-report.html`.

`FAIL` fails the build for new error findings, `WARN` logs them without failing, and `REPORT_ONLY` only emits reports. Existing baseline findings do not count as new. `unknownLayerPolicy` is evaluated separately so unmapped classes are never silently accepted.

## Baseline Workflow

The default baseline is `${maven.multiModuleProjectDirectory}/.egon-cola/architecture-baseline.json`.

1. Review the current findings.
2. Run `./mvnw bytecode-architecture:generate-baseline` to create a baseline.
3. Commit the reviewed baseline if it represents accepted debt.
4. Keep running `check` or `check-reactor`; only new findings are subject to the failure policy and fixed entries are reported as stale.
5. Pass `-DegonArchitecture.overwrite=true` only when intentionally replacing an existing baseline.

The stable fingerprint includes rule ID, source class/member/descriptor, dependency kind, and target class/member/descriptor. It excludes line numbers and presentation text.

## Content-Hash Cache

Parsed metadata is cached below `target/egon-cola-architecture/cache`. The cache key contains the class SHA-256, parser schema version, ASM baseline version, and effective scan-configuration digest. Every run still rebuilds the complete graph and evaluates every rule. CI disables the cache with:

```bash
./mvnw -DegonArchitecture.cache.enabled=false verify
```

## ArchUnit Migration Boundary

The light, web, and service archetypes replace their generated ArchUnit test with this plugin. The approved standard-rule scope intentionally does not preserve five bespoke checks: light domain-first/reversed outbound-port package naming, web external evaluation-facade isolation, service forbidden inbound package segments, service project-wide native gRPC prohibition, and service provider-facade isolation.

## Compatibility And Benchmark

Production artifacts compile with `--release 21`. Maven Invoker verifies real Java 21 classes locally and compiles a real `--release 25` record fixture on JDK 25 before the plugin scans it.

Build and list the JMH benchmark with:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-benchmark -am -DskipTests package
java -jar egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-benchmark/target/egon-cola-component-bytecode-benchmark-benchmarks.jar -l
```

`ArchitectureScanBenchmark.scanOneThousandClasses` generates its 1,000 deterministic class byte arrays before measurement, then measures parsing, graph construction, all ten rules, and result creation. The controlled target is at or below two seconds; shared CI records performance evidence without applying a noisy absolute threshold.
