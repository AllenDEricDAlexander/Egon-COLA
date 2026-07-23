# Archetype Runtime Profile, Shutdown, And Tomcat Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Generate Light, Service, and Web projects with a `dev`/`test`/`prod` GitFlow profile contract, complete graceful shutdown settings, and bounded observable Tomcat defaults.

**Architecture:** Keep cross-cutting Spring lifecycle settings in each generated `application.yml`, keep environment-specific integration values in profile files, and enforce the output with each archetype's existing `verify.groovy`. Apply Tomcat-only settings to Light and Web while preserving Service as a pure RPC/MQ archetype.

**Tech Stack:** Java 21, Spring Boot 3.5.16, Maven Archetype Plugin 3.4.1, Groovy verifier scripts, YAML, Markdown.

## Global Constraints

- Delete `application-local.yml` and `bootstrap-local.yml`; do not retain aliases.
- Default local and feature work to `dev`, Dev/Release/Hotfix validation to `test`, and main runtime to `prod`.
- Preserve existing `dev`, `test`, and `prod` integration behavior except where a `local` profile binding must be removed.
- Do not add Tomcat settings to the pure Service archetype.
- Do not edit generated `target/` files.
- Keep English and Chinese README files synchronized.
- Commit each implementation task once.

---

### Task 1: Complete Spring Graceful Shutdown

**Files:**
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/application.yml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/application.yml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/application.yml`
- Modify: all `deploy/compose/*.yaml` templates under the three archetypes

**Interfaces:**
- Consumes: Spring Boot `spring.task.execution.shutdown` and `spring.task.scheduling.shutdown` properties.
- Consumes: Compose `stop_grace_period`.
- Produces: generated applications whose executor and scheduler wait up to `${SCHEDULING_AWAIT_TERMINATION:30s}` during shutdown, with a `${STOP_GRACE_PERIOD:-40s}` container stop window.

- [ ] **Step 1: Add failing generated-contract assertions**

Add assertions for:

```groovy
assert applicationYaml.contains("scheduling:")
assert applicationYaml.contains('${SCHEDULING_AWAIT_TERMINATION:30s}')
assert applicationYaml.contains("shutdown: graceful")
assert composeYaml.contains('stop_grace_period: ${STOP_GRACE_PERIOD:-40s}')
```

- [ ] **Step 2: Run the Light archetype contract and verify RED**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-light -am clean integration-test
```

Expected: failure in `verify.groovy` because scheduling shutdown or the Compose stop grace period is not present.

- [ ] **Step 3: Add scheduler termination waiting**

Under `spring.task`, add:

```yaml
scheduling:
  shutdown:
    await-termination: true
    await-termination-period: ${symbol_dollar}{SCHEDULING_AWAIT_TERMINATION:30s}
```

Apply the same structure to all three archetypes.

- [ ] **Step 4: Align the external container stop window**

Add the following to the application service in every development and production Compose template:

```yaml
stop_grace_period: ${STOP_GRACE_PERIOD:-40s}
```

The 40-second default is deliberately longer than Spring's 30-second lifecycle phase timeout.

- [ ] **Step 5: Run targeted contracts and verify GREEN**

Run:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml clean integration-test
```

Expected: all six reactor modules succeed.

- [ ] **Step 6: Commit**

```bash
git add -- egon-cola-archetypes
git commit -m "feat(archetype): complete graceful shutdown config"
```

### Task 2: Align GitFlow Profiles

**Files:**
- Delete: all three archetypes' `application-local.yml`
- Delete: all three archetypes' `bootstrap-local.yml`
- Modify: all three archetypes' `application.yml`, `bootstrap.yml`, and `verify.groovy`
- Modify: Light and Web generated `__gitignore__`
- Modify: Service `LocalOrganizationDirectoryStub.java`
- Modify: Web `LocalEvaluationQueryStub.java`
- Modify: all three generated `README.md` and `README.zh-CN.md` pairs

**Interfaces:**
- Consumes: GitFlow branch contexts `feature`, `develop`, `release`, `hotfix`, and `main`.
- Produces: exactly three runtime profiles: `dev`, `test`, and `prod`.

- [ ] **Step 1: Add failing profile-contract assertions**

Assert that runtime file lists contain only:

```groovy
[
    "bootstrap.yml", "bootstrap-dev.yml", "bootstrap-test.yml", "bootstrap-prod.yml",
    "application.yml", "application-dev.yml", "application-test.yml", "application-prod.yml"
]
```

Also assert:

```groovy
assert applicationYaml.contains("default: dev")
assert !new File(resourcesDir, "application-local.yml").exists()
assert !new File(resourcesDir, "bootstrap-local.yml").exists()
```

- [ ] **Step 2: Run the Light archetype contract and verify RED**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-light -am clean integration-test
```

Expected: failure because `local` files still exist and the default remains `local`.

- [ ] **Step 3: Remove local profile files and bindings**

Delete the six template files, set both default-profile declarations to:

```yaml
spring:
  profiles:
    default: dev
```

Set base Nacos namespaces to `${NACOS_NAMESPACE:dev}`, change local fallback annotations to `@Profile("test")`, and remove `application-local.yml` from generated ignore files.

- [ ] **Step 4: Update profile documentation**

Document:

```text
workstation + feature/* -> dev
develop + release/* + hotfix/* validation -> test
main runtime -> prod
```

Use `SPRING_PROFILES_ACTIVE=dev` in generated local-run commands.

- [ ] **Step 5: Run targeted contracts and verify GREEN**

Run:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml clean integration-test
```

Expected: all generated projects build and all profile assertions pass.

- [ ] **Step 6: Commit**

```bash
git add -- egon-cola-archetypes
git commit -m "refactor(archetype): align GitFlow runtime profiles"
```

### Task 3: Complete Tomcat Bounds And Observability

**Files:**
- Modify: Light and Web `application.yml`
- Modify: Light and Web `verify.groovy`

**Interfaces:**
- Consumes: embedded Tomcat configuration in Spring Boot 3.5.16.
- Produces: environment-overridable request bounds, processor cache, and MBean registration.

- [ ] **Step 1: Add failing Tomcat assertions**

Add:

```groovy
assert applicationYaml.contains('${TOMCAT_MAX_HTTP_FORM_POST_SIZE:2MB}')
assert applicationYaml.contains('${TOMCAT_MAX_SWALLOW_SIZE:2MB}')
assert applicationYaml.contains('${TOMCAT_PROCESSOR_CACHE:200}')
assert applicationYaml.contains('${TOMCAT_MBEAN_REGISTRY_ENABLED:true}')
```

Service must assert:

```groovy
assert !applicationYaml.contains("tomcat:")
```

- [ ] **Step 2: Run the Light archetype contract and verify RED**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-light -am clean integration-test
```

Expected: failure because the four new Tomcat properties are absent.

- [ ] **Step 3: Add bounded Tomcat configuration**

Under `server.tomcat` in Light and Web add:

```yaml
max-http-form-post-size: ${symbol_dollar}{TOMCAT_MAX_HTTP_FORM_POST_SIZE:2MB}
max-swallow-size: ${symbol_dollar}{TOMCAT_MAX_SWALLOW_SIZE:2MB}
processor-cache: ${symbol_dollar}{TOMCAT_PROCESSOR_CACHE:200}
mbeanregistry:
  enabled: ${symbol_dollar}{TOMCAT_MBEAN_REGISTRY_ENABLED:true}
```

- [ ] **Step 4: Run Light and Web contracts and verify GREEN**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-light,egon-cola-archetypes/egon-cola-archetype-web -am clean integration-test
```

Expected: both archetypes and their generated projects succeed.

- [ ] **Step 5: Commit**

```bash
git add -- egon-cola-archetypes
git commit -m "perf(archetype): bound and observe Tomcat runtime"
```

### Task 4: Final Generated-Project Verification

**Files:**
- Verify only; no planned source changes.

**Interfaces:**
- Consumes: all commits from Tasks 1-3.
- Produces: fresh evidence that all archetypes generate buildable projects with the intended contract.

- [ ] **Step 1: Run the full archetype reactor**

Run:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml clean integration-test
```

Expected: six reactor modules succeed with zero test failures.

- [ ] **Step 2: Audit removed profile files**

Run:

```bash
find egon-cola-archetypes -path '*/src/main/resources/archetype-resources/*' \( -name 'application-local.yml' -o -name 'bootstrap-local.yml' \) -print
```

Expected: no output.

- [ ] **Step 3: Check patch hygiene**

Run:

```bash
git diff --check
git status --short
```

Expected: no whitespace errors and no uncommitted files.
