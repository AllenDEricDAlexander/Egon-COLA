# Archetype Container Runtime, Compose, and Jenkins Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the Light, Service, and Web archetypes generate one portable source-building Dockerfile, explicit Docker/Podman/nerdctl development and production Compose definitions, secure environment examples, and a standalone Jenkins image-publication pipeline.

**Architecture:** Keep image construction in one standards-only `deploy/container/Dockerfile`; express runtime orchestration in six standalone Compose files per generated project; and keep CI in a runtime-parameterized declarative `Jenkinsfile`. Drive each delivery slice by tightening all three generated-project contracts first, make only the files needed for that slice pass, validate the three archetypes, and commit once per task.

**Tech Stack:** Java 21, Maven Wrapper, Maven Archetype Plugin/Invoker, Groovy `verify.groovy`, Spring Boot layered JAR tools, OCI/Dockerfile syntax, Docker Compose Specification, Docker, Podman, nerdctl/containerd/BuildKit, PostgreSQL, Redis, RabbitMQ, Nacos, Jenkins Declarative Pipeline.

---

## Execution Constraints

- Execute in an isolated worktree created at implementation time with `superpowers:using-git-worktrees`.
- Record `IMPLEMENTATION_BASE=$(git rev-parse HEAD)` before Task 1. The final scope audit compares against this commit.
- Restrict implementation changes to:
  - `egon-cola-archetypes/egon-cola-archetype-light`
  - `egon-cola-archetypes/egon-cola-archetype-service`
  - `egon-cola-archetypes/egon-cola-archetype-web`
- Do not modify POM dependencies, generated Maven module graphs, Java business code, runtime profile YAML, database schemas, or existing Flyway migrations.
- Do not rewrite historical files under `docs/superpowers/specs` or `docs/superpowers/plans` during implementation.
- Do not add Kubernetes, Helm, Quadlet, systemd, Shared Library, deployment-from-Jenkins, or multi-architecture image support.
- Do not open a browser or use computer-control tools.
- Do not run `compose up`, `docker run`, `podman run`, `nerdctl run`, or start any generated application or infrastructure. Static Compose parsing and image builds are allowed because they do not start the project.
- Preserve the Service archetype's existing generated GitHub Actions workflow; only update its Docker build path and arguments.
- Use `apply_patch` for hand-authored files and `git mv`/`git rm` only for the Dockerfile relocation. Do not create files with shell redirection.
- Commit each numbered task exactly once with path-scoped staging. Do not create empty commits and do not fold later-task cleanup into an earlier commit.
- If implementation uses subagents, dispatch one fresh subagent per numbered task, wait for its final result, review the diff and verification evidence, then clean it up before dispatching the next task.

## Design Pattern Decision

The delivery variation is small and fixed. Do not introduce Strategy classes, Template Method inheritance, a generator, a Jenkins Shared Library, or engine-specific Dockerfiles. The selected pattern is an explicit configuration strategy: one portable Dockerfile is the invariant, while standalone Compose files and one `switch` in Jenkins isolate the three runtime command surfaces. Exact generated-contract checks control duplication without adding another abstraction layer.

## File Responsibility Map

For each archetype, the generated resource root is:

```text
egon-cola-archetypes/egon-cola-archetype-<kind>/src/main/resources/archetype-resources
```

| Relative path | Responsibility | Archetype filtering |
| --- | --- | --- |
| `deploy/container/Dockerfile` | Source build, Spring Boot layer extraction, non-root runtime, health check | Filtered |
| `deploy/container/README.md` | Build-engine and Compose operator guide | Unfiltered |
| `deploy/compose/compose.docker.yaml` | Docker development full stack | Unfiltered |
| `deploy/compose/compose.podman.yaml` | Podman development full stack | Unfiltered |
| `deploy/compose/compose.nerdctl.yaml` | nerdctl development full stack | Unfiltered |
| `deploy/compose/compose.docker.prod.yaml` | Docker single-host production template | Unfiltered |
| `deploy/compose/compose.podman.prod.yaml` | Podman single-host production template | Unfiltered |
| `deploy/compose/compose.nerdctl.prod.yaml` | nerdctl single-host production template | Unfiltered |
| `deploy/env/.env.example` | Filtered development image name and safe local defaults | Filtered |
| `deploy/env/.env.prod.example` | Filtered image name and blank production secrets | Filtered |
| `Jenkinsfile` | Standalone test/build/publish pipeline | Unfiltered |
| `.dockerignore` | Source-build context and secret exclusions | Existing root file |
| `__gitignore__` | Generated `.gitignore` including env/secret exclusions | Existing root file |
| `README.md` | Concise entry commands and Jenkins parameter summary | Existing filtered file |
| `META-INF/maven/archetype-metadata.xml` | Generation file-set contract | Archetype source metadata |
| `src/test/resources/projects/basic/verify.groovy` | Generated delivery contract | Archetype integration test |

## Canonical Runtime Values

| Archetype | Generated project in integration test | JAR path | App/management port | Dubbo port | Development remote-client override |
| --- | --- | --- | --- | --- | --- |
| Light | `basic` | `target/*.jar` | `8080` | `50051` | `EXTERNAL_HTTP_ENABLED=false` |
| Service | `student-management-evaluation` | `student-management-evaluation-starter/target/*.jar` | `8081` | `50051` | `ORGANIZATION_FACADE_ENABLED=false` |
| Web | `student-management-organization` | `student-management-organization-starter/target/*.jar` | `8080` | `50051` | `EVALUATION_FACADE_ENABLED=false` |

Infrastructure image defaults are explicit non-`latest` tags:

```text
postgres:17-alpine
redis:7.4-alpine
rabbitmq:4-management
nacos/nacos-server:v2.5.1
```

These defaults live in env example files so operators can update them without editing Compose YAML.

### Task 1: Replace Root Runtime-Only Dockerfiles with One Portable Source-Build Dockerfile

**Files:**
- Move/modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/Dockerfile`
- Move/modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/Dockerfile`
- Move/modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/Dockerfile`
- Modify: all three `src/main/resources/META-INF/maven/archetype-metadata.xml`
- Modify: all three `src/main/resources/archetype-resources/.dockerignore`
- Modify: all three `src/main/resources/archetype-resources/__gitignore__`
- Modify: all three `src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Change the generated Dockerfile contracts first**

Add `assertMissing` to Light immediately after `assertFile`:

```groovy
def assertMissing = { path ->
    assert !new File(generatedProjectDir, path).exists(): "Unexpected path ${path}"
}
```

In Web, replace the `"Dockerfile"` entry in `requiredFiles` with:

```groovy
"deploy/container/Dockerfile",
```

In all three verification scripts, replace the existing root-Dockerfile assertion block with this helper, using the script's existing generated project directory variable:

```groovy
def assertPortableDockerfile = { jarFile, exposedPorts, readinessPort ->
    assertMissing("Dockerfile")
    [
        "Dockerfile.containerd",
        "Dockerfile.nerdctl",
        "Dockerfile.podman",
        "Containerfile",
        "Containerfile.podman",
        "deploy/container/Dockerfile.containerd",
        "deploy/container/Dockerfile.nerdctl",
        "deploy/container/Dockerfile.podman",
        "deploy/container/Containerfile",
        "deploy/container/Containerfile.podman"
    ].each { assertMissing(it) }

    def text = assertFile("deploy/container/Dockerfile").text
    assert text.contains("ARG BUILD_IMAGE=eclipse-temurin:21-jdk-jammy")
    assert text.contains('FROM ${BUILD_IMAGE} AS builder')
    assert text.contains("chmod +x mvnw")
    assert text.contains("./mvnw -B -ntp -DskipTests package")
    assert text.contains("ARG RUNTIME_IMAGE=eclipse-temurin:21-jre-jammy")
    assert text.contains('FROM ${RUNTIME_IMAGE} AS extractor')
    assert text.contains("ARG JAR_FILE=${jarFile}")
    assert text.contains('COPY --from=builder /workspace/${JAR_FILE} app.jar')
    assert text.contains("java -Djarmode=tools -jar app.jar extract --layers --destination extracted")
    assert text.contains('FROM ${RUNTIME_IMAGE} AS runtime')
    assert text.contains("ARG CONTAINER_ENGINE=oci")
    assert text.contains("ARG APP_UID=10001")
    assert text.contains("ARG APP_GID=10001")
    assert text.contains("org.opencontainers.image.build.engine")
    assert text.contains("USER app")
    assert text.contains("EXPOSE ${exposedPorts}")
    assert text.contains("http://127.0.0.1:${readinessPort}/actuator/health/readiness")
    assert text.contains("JarLauncher")
    assert !text.contains("--mount=type=cache")
}
```

Call it with these exact values:

```groovy
// Light
assertPortableDockerfile("target/*.jar", "8080 50051", "8080")

// Service
assertPortableDockerfile(
        "student-management-evaluation-starter/target/*.jar", "8081 50051", "8081")

// Web
assertPortableDockerfile(
        "student-management-organization-starter/target/*.jar", "8080 50051", "8080")
```

Remove the old assertions that require a root Dockerfile, forbid a builder stage, forbid `./mvnw`, or require `COPY ${JAR_FILE}` from the host context.

- [ ] **Step 2: Run the three archetype integration tests and verify the new contract fails**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-light -am clean integration-test
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-service -am clean integration-test
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-web -am clean integration-test
```

Expected: each command fails in its generated `verify.groovy` because `deploy/container/Dockerfile` does not exist.

- [ ] **Step 3: Move the three Dockerfiles and update archetype metadata**

Run only these structural moves:

```bash
for kind in light service web; do
  root="egon-cola-archetypes/egon-cola-archetype-${kind}/src/main/resources/archetype-resources"
  mkdir -p "$root/deploy/container"
  git mv "$root/Dockerfile" "$root/deploy/container/Dockerfile"
done
```

In each metadata file, remove `<include>Dockerfile</include>` from the root file set and add this filtered file set before the closing root `</fileSets>` or before `<modules>` for the multi-module archetypes:

```xml
<fileSet filtered="true" encoding="UTF-8">
    <directory>deploy/container</directory>
    <includes>
        <include>Dockerfile</include>
    </includes>
</fileSet>
```

Preserve the surrounding indentation style: two spaces in Light and four spaces in Service/Web.

- [ ] **Step 4: Replace the Light Dockerfile with the complete portable implementation**

Write this exact content to the Light template Dockerfile:

```dockerfile
# syntax=docker/dockerfile:1
#set( $symbol_dollar = '$' )

ARG BUILD_IMAGE=eclipse-temurin:21-jdk-jammy
ARG RUNTIME_IMAGE=eclipse-temurin:21-jre-jammy

FROM ${symbol_dollar}{BUILD_IMAGE} AS builder
WORKDIR /workspace
COPY . .
RUN chmod +x mvnw && ./mvnw -B -ntp -DskipTests package

FROM ${symbol_dollar}{RUNTIME_IMAGE} AS extractor
WORKDIR /workspace
ARG JAR_FILE=target/*.jar
COPY --from=builder /workspace/${symbol_dollar}{JAR_FILE} app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted

FROM ${symbol_dollar}{RUNTIME_IMAGE} AS runtime
WORKDIR /app

ARG CONTAINER_ENGINE=oci
ARG APP_UID=10001
ARG APP_GID=10001

LABEL org.opencontainers.image.build.engine="${symbol_dollar}{CONTAINER_ENGINE}"

ENV TZ=Asia/Shanghai \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

RUN case "${symbol_dollar}{CONTAINER_ENGINE}" in \
        oci|docker|podman|nerdctl) ;; \
        *) echo "Unsupported CONTAINER_ENGINE=${symbol_dollar}{CONTAINER_ENGINE}" >&2; exit 64 ;; \
    esac \
    && groupadd --gid "${symbol_dollar}{APP_GID}" app \
    && useradd --uid "${symbol_dollar}{APP_UID}" --gid "${symbol_dollar}{APP_GID}" --no-create-home --shell /usr/sbin/nologin app \
    && apt-get update \
    && apt-get install --yes --no-install-recommends ca-certificates curl \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /app/logs \
    && chown -R app:app /app

COPY --from=extractor /workspace/extracted/dependencies/ ./
COPY --from=extractor /workspace/extracted/spring-boot-loader/ ./
COPY --from=extractor /workspace/extracted/snapshot-dependencies/ ./
COPY --from=extractor /workspace/extracted/application/ ./

USER app

EXPOSE 8080 50051

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
    CMD curl --fail --silent --show-error http://127.0.0.1:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["sh", "-c", "exec java ${symbol_dollar}JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
```

- [ ] **Step 5: Replace the Service Dockerfile with the complete portable implementation**

Write this exact content to the Service template Dockerfile:

```dockerfile
# syntax=docker/dockerfile:1
#set( $symbol_dollar = '$' )

ARG BUILD_IMAGE=eclipse-temurin:21-jdk-jammy
ARG RUNTIME_IMAGE=eclipse-temurin:21-jre-jammy

FROM ${symbol_dollar}{BUILD_IMAGE} AS builder
WORKDIR /workspace
COPY . .
RUN chmod +x mvnw && ./mvnw -B -ntp -DskipTests package

FROM ${symbol_dollar}{RUNTIME_IMAGE} AS extractor
WORKDIR /workspace
ARG JAR_FILE=${rootArtifactId}-starter/target/*.jar
COPY --from=builder /workspace/${symbol_dollar}{JAR_FILE} app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted

FROM ${symbol_dollar}{RUNTIME_IMAGE} AS runtime
WORKDIR /app

ARG CONTAINER_ENGINE=oci
ARG APP_UID=10001
ARG APP_GID=10001

LABEL org.opencontainers.image.build.engine="${symbol_dollar}{CONTAINER_ENGINE}"

ENV TZ=Asia/Shanghai \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

RUN case "${symbol_dollar}{CONTAINER_ENGINE}" in \
        oci|docker|podman|nerdctl) ;; \
        *) echo "Unsupported CONTAINER_ENGINE=${symbol_dollar}{CONTAINER_ENGINE}" >&2; exit 64 ;; \
    esac \
    && groupadd --gid "${symbol_dollar}{APP_GID}" app \
    && useradd --uid "${symbol_dollar}{APP_UID}" --gid "${symbol_dollar}{APP_GID}" --no-create-home --shell /usr/sbin/nologin app \
    && apt-get update \
    && apt-get install --yes --no-install-recommends ca-certificates curl \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /app/logs \
    && chown -R app:app /app

COPY --from=extractor /workspace/extracted/dependencies/ ./
COPY --from=extractor /workspace/extracted/spring-boot-loader/ ./
COPY --from=extractor /workspace/extracted/snapshot-dependencies/ ./
COPY --from=extractor /workspace/extracted/application/ ./

USER app

EXPOSE 8081 50051

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
    CMD curl --fail --silent --show-error http://127.0.0.1:8081/actuator/health/readiness || exit 1

ENTRYPOINT ["sh", "-c", "exec java ${symbol_dollar}JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
```

- [ ] **Step 6: Replace the Web Dockerfile with the complete portable implementation**

Write this exact content to the Web template Dockerfile:

```dockerfile
# syntax=docker/dockerfile:1
#set( $symbol_dollar = '$' )

ARG BUILD_IMAGE=eclipse-temurin:21-jdk-jammy
ARG RUNTIME_IMAGE=eclipse-temurin:21-jre-jammy

FROM ${symbol_dollar}{BUILD_IMAGE} AS builder
WORKDIR /workspace
COPY . .
RUN chmod +x mvnw && ./mvnw -B -ntp -DskipTests package

FROM ${symbol_dollar}{RUNTIME_IMAGE} AS extractor
WORKDIR /workspace
ARG JAR_FILE=${rootArtifactId}-starter/target/*.jar
COPY --from=builder /workspace/${symbol_dollar}{JAR_FILE} app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted

FROM ${symbol_dollar}{RUNTIME_IMAGE} AS runtime
WORKDIR /app

ARG CONTAINER_ENGINE=oci
ARG APP_UID=10001
ARG APP_GID=10001

LABEL org.opencontainers.image.build.engine="${symbol_dollar}{CONTAINER_ENGINE}"

ENV TZ=Asia/Shanghai \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

RUN case "${symbol_dollar}{CONTAINER_ENGINE}" in \
        oci|docker|podman|nerdctl) ;; \
        *) echo "Unsupported CONTAINER_ENGINE=${symbol_dollar}{CONTAINER_ENGINE}" >&2; exit 64 ;; \
    esac \
    && groupadd --gid "${symbol_dollar}{APP_GID}" app \
    && useradd --uid "${symbol_dollar}{APP_UID}" --gid "${symbol_dollar}{APP_GID}" --no-create-home --shell /usr/sbin/nologin app \
    && apt-get update \
    && apt-get install --yes --no-install-recommends ca-certificates curl \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /app/logs \
    && chown -R app:app /app

COPY --from=extractor /workspace/extracted/dependencies/ ./
COPY --from=extractor /workspace/extracted/spring-boot-loader/ ./
COPY --from=extractor /workspace/extracted/snapshot-dependencies/ ./
COPY --from=extractor /workspace/extracted/application/ ./

USER app

EXPOSE 8080 50051

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
    CMD curl --fail --silent --show-error http://127.0.0.1:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["sh", "-c", "exec java ${symbol_dollar}JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
```

- [ ] **Step 7: Tighten the source-build context and generated secret ignores**

Replace all three `.dockerignore` files with:

```text
.git
.gitignore
.github
.idea
.vscode
*.iml
.DS_Store

**/target
**/build
**/.mvn/wrapper/maven-wrapper.jar

logs
*.log

.env
.env.*
config/*secret*
secrets
*.pem
*.key
```

In all three `__gitignore__` files, keep the existing local-env section and add these two negations immediately after `.env.*`:

```text
!deploy/env/.env.example
!deploy/env/.env.prod.example
```

Update the existing `.dockerignore` assertions to require `**/target` and remove
the old JAR re-inclusion assertions. Update the `.gitignore` assertions to require
the two generated env-example exceptions.

- [ ] **Step 8: Run the three focused integration tests**

Run the three commands from Step 2.

Expected: all three commands exit 0; generated projects contain only `deploy/container/Dockerfile`; the source-builder and archetype-specific JAR/port assertions pass.

- [ ] **Step 9: Commit the portable image contract**

```bash
git add -- \
  egon-cola-archetypes/egon-cola-archetype-light \
  egon-cola-archetypes/egon-cola-archetype-service \
  egon-cola-archetypes/egon-cola-archetype-web
git diff --cached --check
git commit -m "feat(archetype): add portable source-build images"
```

### Task 2: Add Safe Development and Production Environment Contracts

**Files:**
- Create: all three `src/main/resources/archetype-resources/deploy/env/.env.example`
- Create: all three `src/main/resources/archetype-resources/deploy/env/.env.prod.example`
- Modify: all three `src/main/resources/META-INF/maven/archetype-metadata.xml`
- Modify: all three `src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Add failing env-file assertions to all three generated contracts**

Add this block after the portable Dockerfile assertion in each `verify.groovy`:

```groovy
def developmentEnv = assertFile("deploy/env/.env.example").text
def productionEnv = assertFile("deploy/env/.env.prod.example").text

[
    "POSTGRES_IMAGE=postgres:17-alpine",
    "REDIS_IMAGE=redis:7.4-alpine",
    "RABBITMQ_IMAGE=rabbitmq:4-management",
    "NACOS_IMAGE=nacos/nacos-server:v2.5.1",
    "POSTGRES_PASSWORD=",
    "REDIS_PASSWORD=",
    "RABBITMQ_PASSWORD=",
    "NACOS_PASSWORD=",
    "NACOS_AUTH_TOKEN="
].each { expected ->
    assert productionEnv.readLines().contains(expected):
            "Expected production env example line ${expected}"
}

[
    "POSTGRES_PASSWORD=local-postgres",
    "REDIS_PASSWORD=local-redis",
    "RABBITMQ_PASSWORD=local-rabbitmq",
    "NACOS_PASSWORD=nacos"
].each { forbidden ->
    assert !productionEnv.contains(forbidden):
            "Production env example must not contain development credential ${forbidden}"
}

assert developmentEnv.contains("IMAGE_TAG=local")
assert developmentEnv.contains("NACOS_AUTH_ENABLE=true")
assert productionEnv.contains("REGISTRY=")
assert productionEnv.contains("REGISTRY_NAMESPACE=")
assert productionEnv.contains("IMAGE_TAG=")
```

Add the archetype-specific checks:

```groovy
// Light
assert developmentEnv.contains("IMAGE_NAME=basic")
assert developmentEnv.contains("POSTGRES_DB=student_management")
assert developmentEnv.contains("EXTERNAL_HTTP_ENABLED=false")

// Service
assert developmentEnv.contains("IMAGE_NAME=student-management-evaluation")
assert developmentEnv.contains("POSTGRES_DB=student_management_evaluation")
assert developmentEnv.contains("APPLICATION_PORT=8081")
assert developmentEnv.contains("ORGANIZATION_FACADE_ENABLED=false")
assert productionEnv.readLines().contains("ORGANIZATION_FACADE_ENABLED=")

// Web
assert developmentEnv.contains("IMAGE_NAME=student-management-organization")
assert developmentEnv.contains("POSTGRES_DB=student_management_organization")
assert developmentEnv.contains("APPLICATION_PORT=8080")
assert developmentEnv.contains("EVALUATION_FACADE_ENABLED=false")
assert productionEnv.readLines().contains("EVALUATION_FACADE_ENABLED=")
```

- [ ] **Step 2: Run the three focused integration tests and verify they fail**

Run the three Task 1 integration-test commands.

Expected: each command fails with `Expected file deploy/env/.env.example`.

- [ ] **Step 3: Add the filtered env file set to all three metadata files**

Add this block alongside the Dockerfile file set, preserving each XML file's indentation:

```xml
<fileSet filtered="true" encoding="UTF-8">
    <directory>deploy/env</directory>
    <includes>
        <include>.env.example</include>
        <include>.env.prod.example</include>
    </includes>
</fileSet>
```

- [ ] **Step 4: Create the Light development environment example**

Write:

```dotenv
COMPOSE_PROJECT_NAME=${artifactId}
IMAGE_NAME=${artifactId}
IMAGE_TAG=local
APPLICATION_PORT=8080
DUBBO_PORT=50051

POSTGRES_IMAGE=postgres:17-alpine
POSTGRES_DB=student_management
POSTGRES_USER=student_management
POSTGRES_PASSWORD=local-postgres
POSTGRES_PORT=5432

REDIS_IMAGE=redis:7.4-alpine
REDIS_PASSWORD=local-redis
REDIS_PORT=6379

RABBITMQ_IMAGE=rabbitmq:4-management
RABBITMQ_USERNAME=student_management
RABBITMQ_PASSWORD=local-rabbitmq
RABBITMQ_PORT=5672
RABBITMQ_MANAGEMENT_PORT=15672

NACOS_IMAGE=nacos/nacos-server:v2.5.1
NACOS_USERNAME=nacos
NACOS_PASSWORD=nacos
NACOS_PORT=8848
NACOS_NAMESPACE=dev
NACOS_AUTH_ENABLE=true
NACOS_AUTH_TOKEN=VGhpc0lzQVNlY3VyZU5hY29zVG9rZW5Gb3JEZXY=
NACOS_AUTH_IDENTITY_KEY=serverIdentity
NACOS_AUTH_IDENTITY_VALUE=security

EXTERNAL_HTTP_ENABLED=false
USER_SERVICE_BASE_URL=http://host.containers.internal:18081
TEACHING_SERVICE_BASE_URL=http://host.containers.internal:18082
```

- [ ] **Step 5: Create the Light production environment example**

Blank values below are deliberate fail-fast operator inputs. The implementer must
leave them blank so operators are forced to supply production values:

```dotenv
COMPOSE_PROJECT_NAME=${artifactId}
REGISTRY=
REGISTRY_NAMESPACE=
IMAGE_NAME=${artifactId}
IMAGE_TAG=
APPLICATION_PORT=8080
DUBBO_PORT=50051
APP_MEMORY_LIMIT=1g
APP_CPUS=1.0

POSTGRES_IMAGE=postgres:17-alpine
POSTGRES_DB=student_management
POSTGRES_USER=
POSTGRES_PASSWORD=

REDIS_IMAGE=redis:7.4-alpine
REDIS_PASSWORD=

RABBITMQ_IMAGE=rabbitmq:4-management
RABBITMQ_USERNAME=
RABBITMQ_PASSWORD=

NACOS_IMAGE=nacos/nacos-server:v2.5.1
NACOS_USERNAME=
NACOS_PASSWORD=
NACOS_NAMESPACE=prod
NACOS_AUTH_ENABLE=true
NACOS_AUTH_TOKEN=
NACOS_AUTH_IDENTITY_KEY=
NACOS_AUTH_IDENTITY_VALUE=

EXTERNAL_HTTP_ENABLED=false
USER_SERVICE_BASE_URL=
TEACHING_SERVICE_BASE_URL=
```

- [ ] **Step 6: Create the Service development environment example**

Write:

```dotenv
COMPOSE_PROJECT_NAME=${rootArtifactId}
IMAGE_NAME=${rootArtifactId}
IMAGE_TAG=local
APPLICATION_PORT=8081
DUBBO_PORT=50051

POSTGRES_IMAGE=postgres:17-alpine
POSTGRES_DB=student_management_evaluation
POSTGRES_USER=student_management_evaluation
POSTGRES_PASSWORD=local-postgres
POSTGRES_PORT=5432

REDIS_IMAGE=redis:7.4-alpine
REDIS_PASSWORD=local-redis
REDIS_PORT=6379

RABBITMQ_IMAGE=rabbitmq:4-management
RABBITMQ_USERNAME=student_management_evaluation
RABBITMQ_PASSWORD=local-rabbitmq
RABBITMQ_PORT=5672
RABBITMQ_MANAGEMENT_PORT=15672

NACOS_IMAGE=nacos/nacos-server:v2.5.1
NACOS_USERNAME=nacos
NACOS_PASSWORD=nacos
NACOS_PORT=8848
NACOS_NAMESPACE=dev
NACOS_AUTH_ENABLE=true
NACOS_AUTH_TOKEN=VGhpc0lzQVNlY3VyZU5hY29zVG9rZW5Gb3JEZXY=
NACOS_AUTH_IDENTITY_KEY=serverIdentity
NACOS_AUTH_IDENTITY_VALUE=security

ORGANIZATION_FACADE_ENABLED=false
```

- [ ] **Step 7: Create the Service production environment example**

Write:

```dotenv
COMPOSE_PROJECT_NAME=${rootArtifactId}
REGISTRY=
REGISTRY_NAMESPACE=
IMAGE_NAME=${rootArtifactId}
IMAGE_TAG=
APPLICATION_PORT=8081
DUBBO_PORT=50051
APP_MEMORY_LIMIT=1g
APP_CPUS=1.0

POSTGRES_IMAGE=postgres:17-alpine
POSTGRES_DB=student_management_evaluation
POSTGRES_USER=
POSTGRES_PASSWORD=

REDIS_IMAGE=redis:7.4-alpine
REDIS_PASSWORD=

RABBITMQ_IMAGE=rabbitmq:4-management
RABBITMQ_USERNAME=
RABBITMQ_PASSWORD=

NACOS_IMAGE=nacos/nacos-server:v2.5.1
NACOS_USERNAME=
NACOS_PASSWORD=
NACOS_NAMESPACE=prod
NACOS_AUTH_ENABLE=true
NACOS_AUTH_TOKEN=
NACOS_AUTH_IDENTITY_KEY=
NACOS_AUTH_IDENTITY_VALUE=

ORGANIZATION_FACADE_ENABLED=
```

- [ ] **Step 8: Create the Web development environment example**

Write:

```dotenv
COMPOSE_PROJECT_NAME=${rootArtifactId}
IMAGE_NAME=${rootArtifactId}
IMAGE_TAG=local
APPLICATION_PORT=8080
DUBBO_PORT=50051

POSTGRES_IMAGE=postgres:17-alpine
POSTGRES_DB=student_management_organization
POSTGRES_USER=student_management_organization
POSTGRES_PASSWORD=local-postgres
POSTGRES_PORT=5432

REDIS_IMAGE=redis:7.4-alpine
REDIS_PASSWORD=local-redis
REDIS_PORT=6379

RABBITMQ_IMAGE=rabbitmq:4-management
RABBITMQ_USERNAME=student_management_organization
RABBITMQ_PASSWORD=local-rabbitmq
RABBITMQ_PORT=5672
RABBITMQ_MANAGEMENT_PORT=15672

NACOS_IMAGE=nacos/nacos-server:v2.5.1
NACOS_USERNAME=nacos
NACOS_PASSWORD=nacos
NACOS_PORT=8848
NACOS_NAMESPACE=dev
NACOS_AUTH_ENABLE=true
NACOS_AUTH_TOKEN=VGhpc0lzQVNlY3VyZU5hY29zVG9rZW5Gb3JEZXY=
NACOS_AUTH_IDENTITY_KEY=serverIdentity
NACOS_AUTH_IDENTITY_VALUE=security

EVALUATION_FACADE_ENABLED=false
```

- [ ] **Step 9: Create the Web production environment example**

Write:

```dotenv
COMPOSE_PROJECT_NAME=${rootArtifactId}
REGISTRY=
REGISTRY_NAMESPACE=
IMAGE_NAME=${rootArtifactId}
IMAGE_TAG=
APPLICATION_PORT=8080
DUBBO_PORT=50051
APP_MEMORY_LIMIT=1g
APP_CPUS=1.0

POSTGRES_IMAGE=postgres:17-alpine
POSTGRES_DB=student_management_organization
POSTGRES_USER=
POSTGRES_PASSWORD=

REDIS_IMAGE=redis:7.4-alpine
REDIS_PASSWORD=

RABBITMQ_IMAGE=rabbitmq:4-management
RABBITMQ_USERNAME=
RABBITMQ_PASSWORD=

NACOS_IMAGE=nacos/nacos-server:v2.5.1
NACOS_USERNAME=
NACOS_PASSWORD=
NACOS_NAMESPACE=prod
NACOS_AUTH_ENABLE=true
NACOS_AUTH_TOKEN=
NACOS_AUTH_IDENTITY_KEY=
NACOS_AUTH_IDENTITY_VALUE=

EVALUATION_FACADE_ENABLED=
```

- [ ] **Step 10: Run the three focused integration tests**

Run the three Task 1 integration-test commands.

Expected: all exit 0; the two env files are generated for each project, development values are usable local examples, and production secrets remain blank.

- [ ] **Step 11: Commit the environment contract**

```bash
git add -- \
  egon-cola-archetypes/egon-cola-archetype-light \
  egon-cola-archetypes/egon-cola-archetype-service \
  egon-cola-archetypes/egon-cola-archetype-web
git diff --cached --check
git commit -m "feat(archetype): add container environment templates"
```

### Task 3: Add Explicit Full-Stack Development Compose Files

**Files:**
- Create: all three `deploy/compose/compose.docker.yaml`
- Create: all three `deploy/compose/compose.podman.yaml`
- Create: all three `deploy/compose/compose.nerdctl.yaml`
- Modify: all three `src/main/resources/META-INF/maven/archetype-metadata.xml`
- Modify: all three `src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Add the failing development-Compose contract**

Add this helper to every `verify.groovy` after the env assertions:

```groovy
def assertDevelopmentCompose = { fileName, engine, requiredApplicationLines ->
    def text = assertFile("deploy/compose/${fileName}").text
    ["application:", "postgres:", "redis:", "rabbitmq:", "nacos:",
     "healthcheck:", "networks:", "volumes:", "application_logs:"].each { token ->
        assert text.contains(token): "Expected ${fileName} to contain ${token}"
    }
    assert text.contains("CONTAINER_ENGINE: ${engine}")
    assert text.contains("dockerfile: deploy/container/Dockerfile")
    assert text.contains("context: ../..")
    assert text.contains('SPRING_PROFILES_ACTIVE: dev')
    assert text.contains('jdbc:postgresql://postgres:5432/${POSTGRES_DB}')
    assert text.contains("NACOS_SERVER_ADDR: nacos:8848")
    assert text.contains("DUBBO_REGISTRY_ADDRESS: nacos://nacos:8848")
    assert text.contains('pg_isready -U "$${POSTGRES_USER}" -d "$${POSTGRES_DB}"')
    assert text.contains('redis-cli --no-auth-warning -a "$${REDIS_PASSWORD}" ping')
    assert text.contains("rabbitmq-diagnostics -q ping")
    requiredApplicationLines.each { required ->
        assert text.contains(required): "Expected ${fileName} to contain ${required}"
    }
}

def developmentComposeFiles = [
    "compose.docker.yaml" : "docker",
    "compose.podman.yaml" : "podman",
    "compose.nerdctl.yaml": "nerdctl"
]
```

Call the helper with these exact archetype requirements:

```groovy
// Light
developmentComposeFiles.each { fileName, engine ->
    assertDevelopmentCompose(fileName, engine, [
        'SERVER_PORT: ${APPLICATION_PORT:-8080}',
        'EXTERNAL_HTTP_ENABLED: "false"',
        '"${APPLICATION_PORT:-8080}:${APPLICATION_PORT:-8080}"'
    ])
}

// Service
developmentComposeFiles.each { fileName, engine ->
    assertDevelopmentCompose(fileName, engine, [
        'MANAGEMENT_SERVER_PORT: ${APPLICATION_PORT:-8081}',
        'ORGANIZATION_FACADE_ENABLED: "false"',
        '"${APPLICATION_PORT:-8081}:${APPLICATION_PORT:-8081}"'
    ])
}

// Web
developmentComposeFiles.each { fileName, engine ->
    assertDevelopmentCompose(fileName, engine, [
        'SERVER_PORT: ${APPLICATION_PORT:-8080}',
        'EVALUATION_FACADE_ENABLED: "false"',
        '"${APPLICATION_PORT:-8080}:${APPLICATION_PORT:-8080}"'
    ])
}
```

The `${...}` sequences above are Groovy string content in the generated verification script. Escape them using single-quoted Groovy strings where interpolation would otherwise occur.

- [ ] **Step 2: Run the three focused integration tests and verify they fail**

Run the three Task 1 integration-test commands.

Expected: each fails with `Expected file deploy/compose/compose.docker.yaml`.

- [ ] **Step 3: Add the three unfiltered development Compose files to metadata**

Add this file set to every metadata file:

```xml
<fileSet filtered="false" encoding="UTF-8">
    <directory>deploy/compose</directory>
    <includes>
        <include>compose.docker.yaml</include>
        <include>compose.podman.yaml</include>
        <include>compose.nerdctl.yaml</include>
    </includes>
</fileSet>
```

- [ ] **Step 4: Create the Light Docker development Compose header**

Start `compose.docker.yaml` with this exact Light block:

```yaml
name: ${COMPOSE_PROJECT_NAME:?Set COMPOSE_PROJECT_NAME}

services:
  application:
    image: ${IMAGE_NAME:?Set IMAGE_NAME}:${IMAGE_TAG:-local}
    build:
      context: ../..
      dockerfile: deploy/container/Dockerfile
      args:
        CONTAINER_ENGINE: docker
    environment:
      APPLICATION_PORT: ${APPLICATION_PORT:-8080}
      SPRING_PROFILES_ACTIVE: dev
      SERVER_PORT: ${APPLICATION_PORT:-8080}
      DUBBO_PORT: ${DUBBO_PORT:-50051}
      DB_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB}
      DB_USERNAME: ${POSTGRES_USER}
      DB_PASSWORD: ${POSTGRES_PASSWORD}
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATA_REDIS_PORT: 6379
      SPRING_DATA_REDIS_PASSWORD: ${REDIS_PASSWORD}
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_RABBITMQ_PORT: 5672
      SPRING_RABBITMQ_USERNAME: ${RABBITMQ_USERNAME}
      SPRING_RABBITMQ_PASSWORD: ${RABBITMQ_PASSWORD}
      REDIS_ENABLED: "true"
      RABBITMQ_ENABLED: "true"
      EXTERNAL_HTTP_ENABLED: "false"
      USER_SERVICE_BASE_URL: ${USER_SERVICE_BASE_URL}
      TEACHING_SERVICE_BASE_URL: ${TEACHING_SERVICE_BASE_URL}
      DISCOVERY_ENABLED: "true"
      NACOS_CONFIG_ENABLED: "true"
      NACOS_DISCOVERY_ENABLED: "true"
      NACOS_SERVER_ADDR: nacos:8848
      NACOS_NAMESPACE: ${NACOS_NAMESPACE:-dev}
      NACOS_USERNAME: ${NACOS_USERNAME}
      NACOS_PASSWORD: ${NACOS_PASSWORD}
      DUBBO_REGISTRY_ADDRESS: nacos://nacos:8848
    ports:
      - "${APPLICATION_PORT:-8080}:${APPLICATION_PORT:-8080}"
      - "${DUBBO_PORT:-50051}:${DUBBO_PORT:-50051}"
    volumes:
      - application_logs:/app/logs
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
      nacos:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "curl -fsS http://127.0.0.1:$${APPLICATION_PORT}/actuator/health/readiness"]
      interval: 30s
      timeout: 5s
      start_period: 60s
      retries: 5
    restart: unless-stopped
    networks: [application]
```

- [ ] **Step 5: Create the Service Docker development Compose header**

Start its `compose.docker.yaml` with:

```yaml
name: ${COMPOSE_PROJECT_NAME:?Set COMPOSE_PROJECT_NAME}

services:
  application:
    image: ${IMAGE_NAME:?Set IMAGE_NAME}:${IMAGE_TAG:-local}
    build:
      context: ../..
      dockerfile: deploy/container/Dockerfile
      args:
        CONTAINER_ENGINE: docker
    environment:
      APPLICATION_PORT: ${APPLICATION_PORT:-8081}
      SPRING_PROFILES_ACTIVE: dev
      MANAGEMENT_SERVER_PORT: ${APPLICATION_PORT:-8081}
      DUBBO_PORT: ${DUBBO_PORT:-50051}
      DB_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB}
      DB_USERNAME: ${POSTGRES_USER}
      DB_PASSWORD: ${POSTGRES_PASSWORD}
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_RABBITMQ_PORT: 5672
      SPRING_RABBITMQ_USERNAME: ${RABBITMQ_USERNAME}
      SPRING_RABBITMQ_PASSWORD: ${RABBITMQ_PASSWORD}
      RABBITMQ_ENABLED: "true"
      RABBITMQ_LISTENER_AUTO_STARTUP: "true"
      ORGANIZATION_FACADE_ENABLED: "false"
      DISCOVERY_ENABLED: "true"
      NACOS_CONFIG_ENABLED: "true"
      NACOS_DISCOVERY_ENABLED: "true"
      NACOS_SERVER_ADDR: nacos:8848
      NACOS_NAMESPACE: ${NACOS_NAMESPACE:-dev}
      NACOS_USERNAME: ${NACOS_USERNAME}
      NACOS_PASSWORD: ${NACOS_PASSWORD}
      DUBBO_REGISTRY_ADDRESS: nacos://nacos:8848
    ports:
      - "${APPLICATION_PORT:-8081}:${APPLICATION_PORT:-8081}"
      - "${DUBBO_PORT:-50051}:${DUBBO_PORT:-50051}"
    volumes:
      - application_logs:/app/logs
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
      nacos:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "curl -fsS http://127.0.0.1:$${APPLICATION_PORT}/actuator/health/readiness"]
      interval: 30s
      timeout: 5s
      start_period: 60s
      retries: 5
    restart: unless-stopped
    networks: [application]
```

- [ ] **Step 6: Create the Web Docker development Compose header**

Start its `compose.docker.yaml` with:

```yaml
name: ${COMPOSE_PROJECT_NAME:?Set COMPOSE_PROJECT_NAME}

services:
  application:
    image: ${IMAGE_NAME:?Set IMAGE_NAME}:${IMAGE_TAG:-local}
    build:
      context: ../..
      dockerfile: deploy/container/Dockerfile
      args:
        CONTAINER_ENGINE: docker
    environment:
      APPLICATION_PORT: ${APPLICATION_PORT:-8080}
      SPRING_PROFILES_ACTIVE: dev
      SERVER_PORT: ${APPLICATION_PORT:-8080}
      DUBBO_PORT: ${DUBBO_PORT:-50051}
      DUBBO_PROTOCOL_PORT: ${DUBBO_PORT:-50051}
      DB_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB}
      DB_USERNAME: ${POSTGRES_USER}
      DB_PASSWORD: ${POSTGRES_PASSWORD}
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: ${REDIS_PASSWORD}
      RABBITMQ_HOST: rabbitmq
      RABBITMQ_PORT: 5672
      RABBITMQ_USERNAME: ${RABBITMQ_USERNAME}
      RABBITMQ_PASSWORD: ${RABBITMQ_PASSWORD}
      ORGANIZATION_REDIS_ENABLED: "true"
      ORGANIZATION_RABBIT_ENABLED: "true"
      EVALUATION_FACADE_ENABLED: "false"
      DISCOVERY_ENABLED: "true"
      NACOS_CONFIG_ENABLED: "true"
      NACOS_DISCOVERY_ENABLED: "true"
      NACOS_SERVER_ADDR: nacos:8848
      NACOS_NAMESPACE: ${NACOS_NAMESPACE:-dev}
      NACOS_USERNAME: ${NACOS_USERNAME}
      NACOS_PASSWORD: ${NACOS_PASSWORD}
      DUBBO_REGISTRY_ADDRESS: nacos://nacos:8848
    ports:
      - "${APPLICATION_PORT:-8080}:${APPLICATION_PORT:-8080}"
      - "${DUBBO_PORT:-50051}:${DUBBO_PORT:-50051}"
    volumes:
      - application_logs:/app/logs
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
      nacos:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "curl -fsS http://127.0.0.1:$${APPLICATION_PORT}/actuator/health/readiness"]
      interval: 30s
      timeout: 5s
      start_period: 60s
      retries: 5
    restart: unless-stopped
    networks: [application]
```

- [ ] **Step 7: Append the complete shared development infrastructure tail to each Docker file**

Append this exact block after the archetype-specific `application` service in all three Docker files:

```yaml
  postgres:
    image: ${POSTGRES_IMAGE:?Set POSTGRES_IMAGE}
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    ports:
      - "${POSTGRES_PORT:-5432}:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U \"$${POSTGRES_USER}\" -d \"$${POSTGRES_DB}\""]
      interval: 10s
      timeout: 5s
      retries: 10
    restart: unless-stopped
    networks: [application]

  redis:
    image: ${REDIS_IMAGE:?Set REDIS_IMAGE}
    command: ["sh", "-c", "exec redis-server --appendonly yes --requirepass \"$${REDIS_PASSWORD}\""]
    environment:
      REDIS_PASSWORD: ${REDIS_PASSWORD}
    ports:
      - "${REDIS_PORT:-6379}:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD-SHELL", "redis-cli --no-auth-warning -a \"$${REDIS_PASSWORD}\" ping | grep PONG"]
      interval: 10s
      timeout: 5s
      retries: 10
    restart: unless-stopped
    networks: [application]

  rabbitmq:
    image: ${RABBITMQ_IMAGE:?Set RABBITMQ_IMAGE}
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USERNAME}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD}
    ports:
      - "${RABBITMQ_PORT:-5672}:5672"
      - "${RABBITMQ_MANAGEMENT_PORT:-15672}:15672"
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "-q", "ping"]
      interval: 15s
      timeout: 10s
      retries: 10
    restart: unless-stopped
    networks: [application]

  nacos:
    image: ${NACOS_IMAGE:?Set NACOS_IMAGE}
    environment:
      MODE: standalone
      PREFER_HOST_MODE: hostname
      NACOS_AUTH_ENABLE: ${NACOS_AUTH_ENABLE:-true}
      NACOS_AUTH_TOKEN: ${NACOS_AUTH_TOKEN}
      NACOS_AUTH_IDENTITY_KEY: ${NACOS_AUTH_IDENTITY_KEY}
      NACOS_AUTH_IDENTITY_VALUE: ${NACOS_AUTH_IDENTITY_VALUE}
    ports:
      - "${NACOS_PORT:-8848}:8848"
    volumes:
      - nacos_data:/home/nacos/data
      - nacos_logs:/home/nacos/logs
    healthcheck:
      test: ["CMD-SHELL", "curl -fsS http://127.0.0.1:8848/nacos/v1/console/health/readiness"]
      interval: 15s
      timeout: 10s
      start_period: 45s
      retries: 20
    restart: unless-stopped
    networks: [application]

networks:
  application:
    driver: bridge

volumes:
  application_logs:
  postgres_data:
  redis_data:
  rabbitmq_data:
  nacos_data:
  nacos_logs:
```

- [ ] **Step 8: Create the Podman and nerdctl files as independent definitions**

For each archetype, use `apply_patch` to add two independent files containing the
complete Docker development YAML assembled in Steps 4-7, with these exact
single-line changes:

```diff
-        CONTAINER_ENGINE: docker
+        CONTAINER_ENGINE: podman
```

in `compose.podman.yaml`, and:

```diff
-        CONTAINER_ENGINE: docker
+        CONTAINER_ENGINE: nerdctl
```

in `compose.nerdctl.yaml`. Do not add includes, anchors that cross files, or engine-specific Dockerfiles.

- [ ] **Step 9: Run the three focused integration tests**

Run the three Task 1 integration-test commands.

Expected: all exit 0; each generated project contains three standalone development Compose files, the full five-service topology, the selected build-engine argument, and the archetype-specific remote-client override.

- [ ] **Step 10: Commit the development orchestration**

```bash
git add -- \
  egon-cola-archetypes/egon-cola-archetype-light \
  egon-cola-archetypes/egon-cola-archetype-service \
  egon-cola-archetypes/egon-cola-archetype-web
git diff --cached --check
git commit -m "feat(archetype): add full-stack development compose"
```

### Task 4: Add Standalone Single-Host Production Compose Templates

**Files:**
- Create: all three `deploy/compose/compose.docker.prod.yaml`
- Create: all three `deploy/compose/compose.podman.prod.yaml`
- Create: all three `deploy/compose/compose.nerdctl.prod.yaml`
- Modify: all three `src/main/resources/META-INF/maven/archetype-metadata.xml`
- Modify: all three `src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Add the failing production-Compose contract**

Add this helper to every `verify.groovy`:

```groovy
def assertProductionCompose = { fileName, requiredApplicationLines ->
    def text = assertFile("deploy/compose/${fileName}").text
    ["application:", "postgres:", "redis:", "rabbitmq:", "nacos:",
     "healthcheck:", "networks:", "volumes:", "application_logs:",
     "read_only: true", "tmpfs:", "mem_limit:", "cpus:",
     "restart: unless-stopped"].each { token ->
        assert text.contains(token): "Expected ${fileName} to contain ${token}"
    }
    assert text.contains('${REGISTRY:?Set REGISTRY}/${REGISTRY_NAMESPACE:?Set REGISTRY_NAMESPACE}/${IMAGE_NAME:?Set IMAGE_NAME}:${IMAGE_TAG:?Set IMAGE_TAG}')
    assert text.contains("SPRING_PROFILES_ACTIVE: prod")
    assert text.contains('${POSTGRES_USER:?Set POSTGRES_USER}')
    assert text.contains('${POSTGRES_PASSWORD:?Set POSTGRES_PASSWORD}')
    assert text.contains('${REDIS_PASSWORD:?Set REDIS_PASSWORD}')
    assert text.contains('${RABBITMQ_PASSWORD:?Set RABBITMQ_PASSWORD}')
    assert text.contains('${NACOS_AUTH_TOKEN:?Set NACOS_AUTH_TOKEN}')
    assert !text.contains("build:")
    assert !text.contains("local-postgres")
    assert !text.contains("local-redis")
    assert !text.contains("local-rabbitmq")
    assert !text.contains('${POSTGRES_PORT:-5432}:5432')
    assert !text.contains('${RABBITMQ_MANAGEMENT_PORT:-15672}:15672')
    requiredApplicationLines.each { required ->
        assert text.contains(required): "Expected ${fileName} to contain ${required}"
    }
}

def productionComposeFiles = [
    "compose.docker.prod.yaml",
    "compose.podman.prod.yaml",
    "compose.nerdctl.prod.yaml"
]
```

Call it with:

```groovy
// Light
productionComposeFiles.each { fileName ->
    assertProductionCompose(fileName, [
        'SERVER_PORT: ${APPLICATION_PORT:-8080}',
        'EXTERNAL_HTTP_ENABLED: "${EXTERNAL_HTTP_ENABLED:-false}"'
    ])
}

// Service
productionComposeFiles.each { fileName ->
    assertProductionCompose(fileName, [
        'MANAGEMENT_SERVER_PORT: ${APPLICATION_PORT:-8081}',
        'ORGANIZATION_FACADE_ENABLED: "${ORGANIZATION_FACADE_ENABLED:?Set ORGANIZATION_FACADE_ENABLED to true or false}"'
    ])
}

// Web
productionComposeFiles.each { fileName ->
    assertProductionCompose(fileName, [
        'SERVER_PORT: ${APPLICATION_PORT:-8080}',
        'EVALUATION_FACADE_ENABLED: "${EVALUATION_FACADE_ENABLED:?Set EVALUATION_FACADE_ENABLED to true or false}"'
    ])
}
```

- [ ] **Step 2: Run the three focused integration tests and verify they fail**

Run the three Task 1 integration-test commands.

Expected: each fails with `Expected file deploy/compose/compose.docker.prod.yaml`.

- [ ] **Step 3: Extend the Compose metadata file set with production files**

Add these includes to the existing unfiltered `deploy/compose` file set in every metadata file:

```xml
<include>compose.docker.prod.yaml</include>
<include>compose.podman.prod.yaml</include>
<include>compose.nerdctl.prod.yaml</include>
```

- [ ] **Step 4: Create the Light production application header**

Start `compose.docker.prod.yaml` with:

```yaml
name: ${COMPOSE_PROJECT_NAME:?Set COMPOSE_PROJECT_NAME}

x-logging: &default-logging
  driver: json-file
  options:
    max-size: "10m"
    max-file: "5"

services:
  application:
    image: ${REGISTRY:?Set REGISTRY}/${REGISTRY_NAMESPACE:?Set REGISTRY_NAMESPACE}/${IMAGE_NAME:?Set IMAGE_NAME}:${IMAGE_TAG:?Set IMAGE_TAG}
    environment:
      APPLICATION_PORT: ${APPLICATION_PORT:-8080}
      SPRING_PROFILES_ACTIVE: prod
      SERVER_PORT: ${APPLICATION_PORT:-8080}
      DUBBO_PORT: ${DUBBO_PORT:-50051}
      DB_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB}
      DB_USERNAME: ${POSTGRES_USER:?Set POSTGRES_USER}
      DB_PASSWORD: ${POSTGRES_PASSWORD:?Set POSTGRES_PASSWORD}
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATA_REDIS_PORT: 6379
      SPRING_DATA_REDIS_PASSWORD: ${REDIS_PASSWORD:?Set REDIS_PASSWORD}
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_RABBITMQ_PORT: 5672
      SPRING_RABBITMQ_USERNAME: ${RABBITMQ_USERNAME:?Set RABBITMQ_USERNAME}
      SPRING_RABBITMQ_PASSWORD: ${RABBITMQ_PASSWORD:?Set RABBITMQ_PASSWORD}
      REDIS_ENABLED: "true"
      RABBITMQ_ENABLED: "true"
      EXTERNAL_HTTP_ENABLED: "${EXTERNAL_HTTP_ENABLED:-false}"
      USER_SERVICE_BASE_URL: ${USER_SERVICE_BASE_URL:-}
      TEACHING_SERVICE_BASE_URL: ${TEACHING_SERVICE_BASE_URL:-}
      DISCOVERY_ENABLED: "true"
      NACOS_CONFIG_ENABLED: "true"
      NACOS_DISCOVERY_ENABLED: "true"
      NACOS_SERVER_ADDR: nacos:8848
      NACOS_NAMESPACE: ${NACOS_NAMESPACE:-prod}
      NACOS_USERNAME: ${NACOS_USERNAME:?Set NACOS_USERNAME}
      NACOS_PASSWORD: ${NACOS_PASSWORD:?Set NACOS_PASSWORD}
      DUBBO_REGISTRY_ADDRESS: nacos://nacos:8848
    ports:
      - "${APPLICATION_PORT:-8080}:${APPLICATION_PORT:-8080}"
      - "${DUBBO_PORT:-50051}:${DUBBO_PORT:-50051}"
    read_only: true
    tmpfs:
      - /tmp:size=128m,mode=1777
    volumes:
      - application_logs:/app/logs
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
      nacos:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "curl -fsS http://127.0.0.1:$${APPLICATION_PORT}/actuator/health/readiness"]
      interval: 30s
      timeout: 5s
      start_period: 60s
      retries: 5
    mem_limit: ${APP_MEMORY_LIMIT:-1g}
    cpus: "${APP_CPUS:-1.0}"
    restart: unless-stopped
    logging: *default-logging
    networks: [application]
```

- [ ] **Step 5: Create the Service production application header**

Start its `compose.docker.prod.yaml` with:

```yaml
name: ${COMPOSE_PROJECT_NAME:?Set COMPOSE_PROJECT_NAME}

x-logging: &default-logging
  driver: json-file
  options:
    max-size: "10m"
    max-file: "5"

services:
  application:
    image: ${REGISTRY:?Set REGISTRY}/${REGISTRY_NAMESPACE:?Set REGISTRY_NAMESPACE}/${IMAGE_NAME:?Set IMAGE_NAME}:${IMAGE_TAG:?Set IMAGE_TAG}
    environment:
      APPLICATION_PORT: ${APPLICATION_PORT:-8081}
      SPRING_PROFILES_ACTIVE: prod
      MANAGEMENT_SERVER_PORT: ${APPLICATION_PORT:-8081}
      DUBBO_PORT: ${DUBBO_PORT:-50051}
      DB_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB}
      DB_USERNAME: ${POSTGRES_USER:?Set POSTGRES_USER}
      DB_PASSWORD: ${POSTGRES_PASSWORD:?Set POSTGRES_PASSWORD}
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_RABBITMQ_PORT: 5672
      SPRING_RABBITMQ_USERNAME: ${RABBITMQ_USERNAME:?Set RABBITMQ_USERNAME}
      SPRING_RABBITMQ_PASSWORD: ${RABBITMQ_PASSWORD:?Set RABBITMQ_PASSWORD}
      RABBITMQ_ENABLED: "true"
      RABBITMQ_LISTENER_AUTO_STARTUP: "true"
      ORGANIZATION_FACADE_ENABLED: "${ORGANIZATION_FACADE_ENABLED:?Set ORGANIZATION_FACADE_ENABLED to true or false}"
      DISCOVERY_ENABLED: "true"
      NACOS_CONFIG_ENABLED: "true"
      NACOS_DISCOVERY_ENABLED: "true"
      NACOS_SERVER_ADDR: nacos:8848
      NACOS_NAMESPACE: ${NACOS_NAMESPACE:-prod}
      NACOS_USERNAME: ${NACOS_USERNAME:?Set NACOS_USERNAME}
      NACOS_PASSWORD: ${NACOS_PASSWORD:?Set NACOS_PASSWORD}
      DUBBO_REGISTRY_ADDRESS: nacos://nacos:8848
    ports:
      - "${APPLICATION_PORT:-8081}:${APPLICATION_PORT:-8081}"
      - "${DUBBO_PORT:-50051}:${DUBBO_PORT:-50051}"
    read_only: true
    tmpfs:
      - /tmp:size=128m,mode=1777
    volumes:
      - application_logs:/app/logs
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
      nacos:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "curl -fsS http://127.0.0.1:$${APPLICATION_PORT}/actuator/health/readiness"]
      interval: 30s
      timeout: 5s
      start_period: 60s
      retries: 5
    mem_limit: ${APP_MEMORY_LIMIT:-1g}
    cpus: "${APP_CPUS:-1.0}"
    restart: unless-stopped
    logging: *default-logging
    networks: [application]
```

- [ ] **Step 6: Create the Web production application header**

Start its `compose.docker.prod.yaml` with:

```yaml
name: ${COMPOSE_PROJECT_NAME:?Set COMPOSE_PROJECT_NAME}

x-logging: &default-logging
  driver: json-file
  options:
    max-size: "10m"
    max-file: "5"

services:
  application:
    image: ${REGISTRY:?Set REGISTRY}/${REGISTRY_NAMESPACE:?Set REGISTRY_NAMESPACE}/${IMAGE_NAME:?Set IMAGE_NAME}:${IMAGE_TAG:?Set IMAGE_TAG}
    environment:
      APPLICATION_PORT: ${APPLICATION_PORT:-8080}
      SPRING_PROFILES_ACTIVE: prod
      SERVER_PORT: ${APPLICATION_PORT:-8080}
      DUBBO_PORT: ${DUBBO_PORT:-50051}
      DUBBO_PROTOCOL_PORT: ${DUBBO_PORT:-50051}
      DB_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB}
      DB_USERNAME: ${POSTGRES_USER:?Set POSTGRES_USER}
      DB_PASSWORD: ${POSTGRES_PASSWORD:?Set POSTGRES_PASSWORD}
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: ${REDIS_PASSWORD:?Set REDIS_PASSWORD}
      RABBITMQ_HOST: rabbitmq
      RABBITMQ_PORT: 5672
      RABBITMQ_USERNAME: ${RABBITMQ_USERNAME:?Set RABBITMQ_USERNAME}
      RABBITMQ_PASSWORD: ${RABBITMQ_PASSWORD:?Set RABBITMQ_PASSWORD}
      ORGANIZATION_REDIS_ENABLED: "true"
      ORGANIZATION_RABBIT_ENABLED: "true"
      EVALUATION_FACADE_ENABLED: "${EVALUATION_FACADE_ENABLED:?Set EVALUATION_FACADE_ENABLED to true or false}"
      DISCOVERY_ENABLED: "true"
      NACOS_CONFIG_ENABLED: "true"
      NACOS_DISCOVERY_ENABLED: "true"
      NACOS_SERVER_ADDR: nacos:8848
      NACOS_NAMESPACE: ${NACOS_NAMESPACE:-prod}
      NACOS_USERNAME: ${NACOS_USERNAME:?Set NACOS_USERNAME}
      NACOS_PASSWORD: ${NACOS_PASSWORD:?Set NACOS_PASSWORD}
      DUBBO_REGISTRY_ADDRESS: nacos://nacos:8848
    ports:
      - "${APPLICATION_PORT:-8080}:${APPLICATION_PORT:-8080}"
      - "${DUBBO_PORT:-50051}:${DUBBO_PORT:-50051}"
    read_only: true
    tmpfs:
      - /tmp:size=128m,mode=1777
    volumes:
      - application_logs:/app/logs
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
      nacos:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "curl -fsS http://127.0.0.1:$${APPLICATION_PORT}/actuator/health/readiness"]
      interval: 30s
      timeout: 5s
      start_period: 60s
      retries: 5
    mem_limit: ${APP_MEMORY_LIMIT:-1g}
    cpus: "${APP_CPUS:-1.0}"
    restart: unless-stopped
    logging: *default-logging
    networks: [application]
```

- [ ] **Step 7: Append the complete production infrastructure tail to each Docker production file**

Append:

```yaml
  postgres:
    image: ${POSTGRES_IMAGE:?Set POSTGRES_IMAGE}
    environment:
      POSTGRES_DB: ${POSTGRES_DB:?Set POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER:?Set POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:?Set POSTGRES_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U \"$${POSTGRES_USER}\" -d \"$${POSTGRES_DB}\""]
      interval: 10s
      timeout: 5s
      retries: 10
    restart: unless-stopped
    logging: *default-logging
    networks: [application]

  redis:
    image: ${REDIS_IMAGE:?Set REDIS_IMAGE}
    command: ["sh", "-c", "exec redis-server --appendonly yes --requirepass \"$${REDIS_PASSWORD}\""]
    environment:
      REDIS_PASSWORD: ${REDIS_PASSWORD:?Set REDIS_PASSWORD}
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD-SHELL", "redis-cli --no-auth-warning -a \"$${REDIS_PASSWORD}\" ping | grep PONG"]
      interval: 10s
      timeout: 5s
      retries: 10
    restart: unless-stopped
    logging: *default-logging
    networks: [application]

  rabbitmq:
    image: ${RABBITMQ_IMAGE:?Set RABBITMQ_IMAGE}
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USERNAME:?Set RABBITMQ_USERNAME}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD:?Set RABBITMQ_PASSWORD}
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "-q", "ping"]
      interval: 15s
      timeout: 10s
      retries: 10
    restart: unless-stopped
    logging: *default-logging
    networks: [application]

  nacos:
    image: ${NACOS_IMAGE:?Set NACOS_IMAGE}
    environment:
      MODE: standalone
      PREFER_HOST_MODE: hostname
      NACOS_AUTH_ENABLE: ${NACOS_AUTH_ENABLE:-true}
      NACOS_AUTH_TOKEN: ${NACOS_AUTH_TOKEN:?Set NACOS_AUTH_TOKEN}
      NACOS_AUTH_IDENTITY_KEY: ${NACOS_AUTH_IDENTITY_KEY:?Set NACOS_AUTH_IDENTITY_KEY}
      NACOS_AUTH_IDENTITY_VALUE: ${NACOS_AUTH_IDENTITY_VALUE:?Set NACOS_AUTH_IDENTITY_VALUE}
    volumes:
      - nacos_data:/home/nacos/data
      - nacos_logs:/home/nacos/logs
    healthcheck:
      test: ["CMD-SHELL", "curl -fsS http://127.0.0.1:8848/nacos/v1/console/health/readiness"]
      interval: 15s
      timeout: 10s
      start_period: 45s
      retries: 20
    restart: unless-stopped
    logging: *default-logging
    networks: [application]

networks:
  application:
    driver: bridge

volumes:
  application_logs:
  postgres_data:
  redis_data:
  rabbitmq_data:
  nacos_data:
  nacos_logs:
```

- [ ] **Step 8: Create independent Podman and nerdctl production files**

For each archetype, use `apply_patch` to add the complete Docker production
definition byte-for-byte at both paths:

```text
deploy/compose/compose.podman.prod.yaml
deploy/compose/compose.nerdctl.prod.yaml
```

No `build` section or engine argument belongs in production because these files pull the immutable image published by Jenkins. The files remain separate so future proven runtime differences can be isolated without changing file names or commands.

- [ ] **Step 9: Run the three focused integration tests**

Run the three Task 1 integration-test commands.

Expected: all exit 0; all production files are standalone, have no build section or development credential, require production secrets, hide infrastructure host ports, and require an explicit Service/Web Facade choice.

- [ ] **Step 10: Commit the production templates**

```bash
git add -- \
  egon-cola-archetypes/egon-cola-archetype-light \
  egon-cola-archetypes/egon-cola-archetype-service \
  egon-cola-archetypes/egon-cola-archetype-web
git diff --cached --check
git commit -m "feat(archetype): add production compose templates"
```

### Task 5: Add the Standalone Runtime-Parameterized Jenkins Pipeline

**Files:**
- Create: all three `src/main/resources/archetype-resources/Jenkinsfile`
- Modify: all three `src/main/resources/META-INF/maven/archetype-metadata.xml`
- Modify: all three `src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Add the failing Jenkinsfile contract**

Add this exact assertion block to all three `verify.groovy` files:

```groovy
def jenkinsfile = assertFile("Jenkinsfile").text
[
    "pipeline {",
    "choice(name: 'CONTAINER_ENGINE', choices: ['docker', 'podman', 'nerdctl']",
    "string(name: 'CONTAINERD_NAMESPACE', defaultValue: 'default'",
    "string(name: 'REGISTRY', defaultValue: ''",
    "string(name: 'REGISTRY_NAMESPACE', defaultValue: ''",
    "string(name: 'REGISTRY_CREDENTIALS_ID', defaultValue: ''",
    "string(name: 'IMAGE_NAME', defaultValue: ''",
    "string(name: 'IMAGE_TAG', defaultValue: ''",
    "booleanParam(name: 'PUBLISH_IMAGE', defaultValue: false",
    "booleanParam(name: 'PUBLISH_LATEST', defaultValue: false",
    "stage('Preflight')",
    "stage('Test')",
    "stage('Build Image')",
    "stage('Publish Image')",
    "deploy/container/Dockerfile",
    "CONTAINER_ENGINE=${CONTAINER_ENGINE}",
    "credentialsId: params.REGISTRY_CREDENTIALS_ID",
    "--password-stdin",
    "SPRING_PROFILES_ACTIVE=test bash ./mvnw -B -ntp clean verify",
    "allowEmptyResults: true"
].each { token ->
    assert jenkinsfile.contains(token): "Expected Jenkinsfile to contain ${token}"
}
assert !jenkinsfile.contains("docker compose")
assert !jenkinsfile.contains("podman compose")
assert !jenkinsfile.contains("nerdctl compose")
assert !jenkinsfile.contains("withRegistry")
```

- [ ] **Step 2: Run the three focused integration tests and verify they fail**

Run the three Task 1 integration-test commands.

Expected: each fails with `Expected file Jenkinsfile`.

- [ ] **Step 3: Add an unfiltered root Jenkinsfile to metadata**

Add this file set to every metadata file:

```xml
<fileSet filtered="false" encoding="UTF-8">
    <directory></directory>
    <includes>
        <include>Jenkinsfile</include>
    </includes>
</fileSet>
```

Keep it separate from filtered root resources so Jenkins `${...}` and Groovy interpolation are never processed by Velocity.

- [ ] **Step 4: Create the complete Jenkinsfile in all three archetype resource roots**

Write the following identical unfiltered file to each archetype:

```groovy
pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    parameters {
        choice(name: 'CONTAINER_ENGINE', choices: ['docker', 'podman', 'nerdctl'],
                description: 'Container command used to build, log in, and push')
        string(name: 'CONTAINERD_NAMESPACE', defaultValue: 'default', trim: true,
                description: 'nerdctl/containerd namespace; ignored by Docker and Podman')
        string(name: 'REGISTRY', defaultValue: '', trim: true,
                description: 'Registry host without http:// or https://')
        string(name: 'REGISTRY_NAMESPACE', defaultValue: '', trim: true,
                description: 'Registry project or namespace')
        string(name: 'REGISTRY_CREDENTIALS_ID', defaultValue: '', trim: true,
                description: 'Jenkins username/password credentials ID')
        string(name: 'IMAGE_NAME', defaultValue: '', trim: true,
                description: 'Blank uses the Maven root artifactId')
        string(name: 'IMAGE_TAG', defaultValue: '', trim: true,
                description: 'Blank creates version-build-shortCommit')
        booleanParam(name: 'PUBLISH_IMAGE', defaultValue: false,
                description: 'Log in and push the immutable image tag')
        booleanParam(name: 'PUBLISH_LATEST', defaultValue: false,
                description: 'Also publish latest on main/master, never on change requests')
    }

    environment {
        SPRING_PROFILES_ACTIVE = 'test'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT_FULL = sh(
                            returnStdout: true,
                            script: 'git rev-parse HEAD').trim()
                    env.GIT_COMMIT_SHORT = sh(
                            returnStdout: true,
                            script: 'git rev-parse --short=8 HEAD').trim()
                    env.PROJECT_VERSION = sh(
                            returnStdout: true,
                            script: "bash ./mvnw -q -DforceStdout help:evaluate -Dexpression=project.version | tail -n 1").trim()
                    env.PROJECT_ARTIFACT_ID = sh(
                            returnStdout: true,
                            script: "bash ./mvnw -q -DforceStdout help:evaluate -Dexpression=project.artifactId | tail -n 1").trim()

                    def imageName = params.IMAGE_NAME.trim() ?: env.PROJECT_ARTIFACT_ID
                    if (!(imageName ==~ /[a-z0-9]+(?:[._-][a-z0-9]+)*/)) {
                        error("Invalid IMAGE_NAME: ${imageName}")
                    }

                    def rawTag = params.IMAGE_TAG.trim() ?:
                            "${env.PROJECT_VERSION}-${env.BUILD_NUMBER}-${env.GIT_COMMIT_SHORT}"
                    def normalizedTag = rawTag.toLowerCase(Locale.ROOT)
                            .replaceAll(/[^a-z0-9_.-]/, '-')
                            .replaceAll(/^[.-]+/, '')
                            .take(128)
                    if (!normalizedTag) {
                        error('Resolved image tag is empty')
                    }

                    def registry = params.REGISTRY.trim().replaceAll('/+$', '')
                    def namespace = params.REGISTRY_NAMESPACE.trim()
                            .replaceAll('^/+', '')
                            .replaceAll('/+$', '')
                    if (namespace && !(namespace ==~ /[a-z0-9]+(?:[._\/-][a-z0-9]+)*/)) {
                        error("Invalid REGISTRY_NAMESPACE: ${namespace}")
                    }
                    env.RESOLVED_IMAGE_NAME = imageName
                    env.RESOLVED_IMAGE_TAG = normalizedTag
                    env.IMAGE_REPOSITORY = registry && namespace ?
                            "${registry}/${namespace}/${imageName}" : imageName
                    env.FULL_IMAGE = "${env.IMAGE_REPOSITORY}:${normalizedTag}"
                }
            }
        }

        stage('Preflight') {
            steps {
                sh '''
                    set -eu
                    test -x ./mvnw

                    java_major="$(java -XshowSettings:properties -version 2>&1 |
                      awk -F= '/java.specification.version/ {gsub(/[[:space:]]/, "", $2); print $2; exit}')"
                    test "$java_major" = "21" || {
                      echo "JDK 21 is required; found ${java_major:-unknown}" >&2
                      exit 1
                    }

                    command -v "$CONTAINER_ENGINE" >/dev/null 2>&1
                    case "$CONTAINER_ENGINE" in
                      docker)
                        docker info >/dev/null
                        ;;
                      podman)
                        podman info >/dev/null
                        ;;
                      nerdctl)
                        nerdctl --namespace "$CONTAINERD_NAMESPACE" info >/dev/null
                        nerdctl --namespace "$CONTAINERD_NAMESPACE" build --help >/dev/null
                        ;;
                      *)
                        echo "Unsupported CONTAINER_ENGINE=$CONTAINER_ENGINE" >&2
                        exit 64
                        ;;
                    esac
                '''
            }
        }

        stage('Test') {
            steps {
                sh 'SPRING_PROFILES_ACTIVE=test bash ./mvnw -B -ntp clean verify'
            }
        }

        stage('Build Image') {
            steps {
                sh '''
                    set -eu
                    engine() {
                      if [ "$CONTAINER_ENGINE" = "nerdctl" ]; then
                        nerdctl --namespace "$CONTAINERD_NAMESPACE" "$@"
                      else
                        "$CONTAINER_ENGINE" "$@"
                      fi
                    }

                    engine build \
                      --build-arg "CONTAINER_ENGINE=${CONTAINER_ENGINE}" \
                      --file deploy/container/Dockerfile \
                      --tag "$FULL_IMAGE" \
                      .
                '''
            }
        }

        stage('Publish Image') {
            when {
                expression { params.PUBLISH_IMAGE }
            }
            steps {
                script {
                    if (!params.REGISTRY.trim()) {
                        error('REGISTRY is required when PUBLISH_IMAGE=true')
                    }
                    if (params.REGISTRY.contains('://')) {
                        error('REGISTRY must not include a URL scheme')
                    }
                    if (!params.REGISTRY_NAMESPACE.trim()) {
                        error('REGISTRY_NAMESPACE is required when PUBLISH_IMAGE=true')
                    }
                    if (!params.REGISTRY_CREDENTIALS_ID.trim()) {
                        error('REGISTRY_CREDENTIALS_ID is required when PUBLISH_IMAGE=true')
                    }

                    withCredentials([usernamePassword(
                            credentialsId: params.REGISTRY_CREDENTIALS_ID,
                            usernameVariable: 'REGISTRY_USERNAME',
                            passwordVariable: 'REGISTRY_PASSWORD')]) {
                        sh '''
                            set +x
                            set -eu
                            engine() {
                              if [ "$CONTAINER_ENGINE" = "nerdctl" ]; then
                                nerdctl --namespace "$CONTAINERD_NAMESPACE" "$@"
                              else
                                "$CONTAINER_ENGINE" "$@"
                              fi
                            }

                            printf '%s' "$REGISTRY_PASSWORD" |
                              engine login "$REGISTRY" \
                                --username "$REGISTRY_USERNAME" \
                                --password-stdin
                            engine push "$FULL_IMAGE"

                            if [ "$PUBLISH_LATEST" = "true" ] &&
                               [ -z "${CHANGE_ID:-}" ] &&
                               { [ "${BRANCH_NAME:-}" = "main" ] ||
                                 [ "${BRANCH_NAME:-}" = "master" ]; }; then
                              latest_image="${IMAGE_REPOSITORY}:latest"
                              engine tag "$FULL_IMAGE" "$latest_image"
                              engine push "$latest_image"
                            fi
                        '''
                    }
                }
            }
        }
    }

    post {
        always {
            junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
            script {
                if (params.PUBLISH_IMAGE && params.REGISTRY.trim()) {
                    sh returnStatus: true, script: '''
                        set +x
                        if command -v "$CONTAINER_ENGINE" >/dev/null 2>&1; then
                          if [ "$CONTAINER_ENGINE" = "nerdctl" ]; then
                            nerdctl --namespace "$CONTAINERD_NAMESPACE" logout "$REGISTRY"
                          else
                            "$CONTAINER_ENGINE" logout "$REGISTRY"
                          fi
                        fi
                    '''
                }
            }
        }
        success {
            echo "Built immutable image ${env.FULL_IMAGE}"
        }
    }
}
```

- [ ] **Step 5: Run the three focused integration tests**

Run the three Task 1 integration-test commands.

Expected: all exit 0; the unfiltered Jenkinsfile survives archetype generation with its `${...}` expressions intact and contains no Compose deployment command.

- [ ] **Step 6: Commit the Jenkins CI/image-publication contract**

```bash
git add -- \
  egon-cola-archetypes/egon-cola-archetype-light \
  egon-cola-archetypes/egon-cola-archetype-service \
  egon-cola-archetypes/egon-cola-archetype-web
git diff --cached --check
git commit -m "feat(archetype): add Jenkins image pipeline"
```

### Task 6: Document the Delivery Contract and Align Existing GitHub Actions

**Files:**
- Create: all three `src/main/resources/archetype-resources/deploy/container/README.md`
- Modify: all three `src/main/resources/archetype-resources/README.md`
- Modify: all three `src/main/resources/META-INF/maven/archetype-metadata.xml`
- Modify: all three `src/test/resources/projects/basic/verify.groovy`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/.github/workflows/ci.yml`

- [ ] **Step 1: Add failing documentation and GitHub Actions assertions**

Add this block to all three verification scripts:

```groovy
def deliveryReadme = assertFile("deploy/container/README.md").text
[
    "One Portable Dockerfile",
    "Docker",
    "Podman",
    "nerdctl",
    "Rootless And Rootful",
    "Development Compose",
    "Production Compose",
    "Persistent Data",
    "Jenkins",
    "does not provide high availability"
].each { token ->
    assert deliveryReadme.contains(token): "Expected deployment README to contain ${token}"
}

def generatedReadme = assertFile("README.md").text
[
    "deploy/container/Dockerfile",
    "compose.docker.yaml",
    "compose.podman.yaml",
    "compose.nerdctl.yaml",
    "Jenkinsfile",
    "PUBLISH_IMAGE"
].each { token ->
    assert generatedReadme.contains(token): "Expected generated README to contain ${token}"
}
assert !generatedReadme.contains("docker build -t")
```

Replace the Service workflow assertion with:

```groovy
def generatedWorkflow = assertFile(".github/workflows/ci.yml").text
assert generatedWorkflow.contains("SPRING_PROFILES_ACTIVE=test bash ./mvnw -B -ntp clean test")
assert generatedWorkflow.contains("SPRING_PROFILES_ACTIVE=test bash ./mvnw -B -ntp -DskipTests package")
assert generatedWorkflow.contains("docker build --build-arg CONTAINER_ENGINE=docker")
assert generatedWorkflow.contains("--file deploy/container/Dockerfile")
assert generatedWorkflow.contains("--tag student-management-evaluation:ci")
assert !generatedWorkflow.contains("docker build -t student-management-evaluation:ci .")
```

- [ ] **Step 2: Run the three focused integration tests and verify they fail**

Run the three Task 1 integration-test commands.

Expected: each fails with `Expected file deploy/container/README.md`.

- [ ] **Step 3: Add the unfiltered deployment README to metadata**

Add this separate file set to every metadata file:

```xml
<fileSet filtered="false" encoding="UTF-8">
    <directory>deploy/container</directory>
    <includes>
        <include>README.md</include>
    </includes>
</fileSet>
```

- [ ] **Step 4: Create the complete deployment README in all three archetypes**

Write this identical unfiltered content to each `deploy/container/README.md`:

````markdown
# Container Delivery

## One Portable Dockerfile

`deploy/container/Dockerfile` is the only image build definition. Docker, Podman,
and nerdctl/BuildKit consume the same standards-oriented multi-stage file. The
`CONTAINER_ENGINE` build argument records which command performed the build; it
does not select a second Dockerfile.

```bash
IMAGE_NAME="$(bash ./mvnw -q -DforceStdout help:evaluate -Dexpression=project.artifactId | tail -n 1)"
docker build --build-arg CONTAINER_ENGINE=docker \
  --file deploy/container/Dockerfile --tag "$IMAGE_NAME:local" .
podman build --build-arg CONTAINER_ENGINE=podman \
  --file deploy/container/Dockerfile --tag "$IMAGE_NAME:local" .
nerdctl build --build-arg CONTAINER_ENGINE=nerdctl \
  --file deploy/container/Dockerfile --tag "$IMAGE_NAME:local" .
```

The Dockerfile packages source with the Maven Wrapper. All Maven dependencies,
including organization-specific Facade artifacts, must be resolvable from the
build environment. Private-repository credential transport is an operator concern
and must not be encoded as a Docker build argument because build arguments are not
secret storage.

## Docker

Docker requires a reachable daemon and the Compose v2 plugin.

## Podman

Podman may run rootless or rootful. `podman compose` also requires a configured
Compose provider. Inspect `podman compose version` before using the generated
commands.

## nerdctl

nerdctl requires reachable containerd and BuildKit services. Select the intended
containerd namespace in the command or Jenkins parameter; `default` is the
generated Jenkins default.

## Rootless And Rootful

The application runs as a numeric non-root user. Compose files avoid privileged
mode, host networking, runtime sockets, fixed host data paths, and ports below
1024. Infrastructure images still require host user-namespace and volume support
appropriate to the selected engine.

## Development Compose

Development definitions build source and start application, PostgreSQL, Redis,
RabbitMQ, and Nacos. Use the file matching the selected runtime:

```bash
docker compose --env-file deploy/env/.env.example \
  --file deploy/compose/compose.docker.yaml up -d --build
podman compose --env-file deploy/env/.env.example \
  --file deploy/compose/compose.podman.yaml up -d --build
nerdctl compose --env-file deploy/env/.env.example \
  --file deploy/compose/compose.nerdctl.yaml up -d --build
```

The example credentials are development-only.

## Production Compose

Copy `deploy/env/.env.prod.example` to an ignored operator-owned `.env.prod`, set
every blank required value, and use the production file for the selected runtime.
Production files pull `${REGISTRY}/${REGISTRY_NAMESPACE}/${IMAGE_NAME}:${IMAGE_TAG}`
and never build source on the server.

The generated production topology is a single-host baseline. It does not provide
high availability, backup, certificate issuance, cross-host scheduling, or
disaster recovery. Replace included infrastructure endpoints with managed or
clustered services when those properties are required.

## Persistent Data

Ordinary `stop` and `down` retain named volumes. A command that explicitly removes
volumes permanently deletes local database, broker, cache, Nacos, and application
log data. No generated helper performs that deletion automatically.

## Health And Failure Behavior

PostgreSQL, Redis, RabbitMQ, Nacos, and the Spring Boot readiness endpoint have
health checks. Missing production variables fail Compose configuration. An enabled
but unavailable remote Facade retains the generated application's fail-fast
behavior.

## Jenkins

The root `Jenkinsfile` tests, builds, and optionally publishes the image with
Docker, Podman, or nerdctl. It never runs Compose or deploys the project.
`PUBLISH_IMAGE` and `PUBLISH_LATEST` default to `false`; registry publication must
be explicitly enabled with Jenkins credentials.
````

- [ ] **Step 5: Replace each top-level README's old Docker paragraph with the confirmed command section**

Use the existing `${symbol_pound}` heading style. For Light, insert:

````markdown
${symbol_pound}${symbol_pound} Container Delivery

The generated project uses one source-building `deploy/container/Dockerfile`:

```bash
docker build --build-arg CONTAINER_ENGINE=docker -f deploy/container/Dockerfile -t ${artifactId}:local .
podman build --build-arg CONTAINER_ENGINE=podman -f deploy/container/Dockerfile -t ${artifactId}:local .
nerdctl build --build-arg CONTAINER_ENGINE=nerdctl -f deploy/container/Dockerfile -t ${artifactId}:local .
```

Start the complete Docker development stack with:

```bash
docker compose --env-file deploy/env/.env.example -f deploy/compose/compose.docker.yaml up -d --build
```

Podman and nerdctl use `compose.podman.yaml` and `compose.nerdctl.yaml`. Production
uses the matching `.prod.yaml` file and an operator-owned `.env.prod`. See
`deploy/container/README.md` for rootless prerequisites, persistence, production
boundaries, and data-deletion warnings.

The root `Jenkinsfile` runs tests and can publish immutable images. Set
`PUBLISH_IMAGE=true` plus registry parameters to publish; it never deploys.
````

For Service, insert this complete section while preserving the existing module,
domain, profile, and integration documentation around it:

````markdown
${symbol_pound}${symbol_pound} Container Delivery

The generated project uses one source-building `deploy/container/Dockerfile`:

```bash
docker build --build-arg CONTAINER_ENGINE=docker -f deploy/container/Dockerfile -t ${rootArtifactId}:local .
podman build --build-arg CONTAINER_ENGINE=podman -f deploy/container/Dockerfile -t ${rootArtifactId}:local .
nerdctl build --build-arg CONTAINER_ENGINE=nerdctl -f deploy/container/Dockerfile -t ${rootArtifactId}:local .
```

Start the complete Docker development stack with:

```bash
docker compose --env-file deploy/env/.env.example -f deploy/compose/compose.docker.yaml up -d --build
```

Podman and nerdctl use `compose.podman.yaml` and `compose.nerdctl.yaml`. Production
uses the matching `.prod.yaml` file and an operator-owned `.env.prod`. See
`deploy/container/README.md` for rootless prerequisites, persistence, production
boundaries, and data-deletion warnings.

The root `Jenkinsfile` runs tests and can publish immutable images. Set
`PUBLISH_IMAGE=true` plus registry parameters to publish; it never deploys.
````

For Web, insert this complete section while preserving its existing module,
domain, profile, and integration documentation around it:

````markdown
${symbol_pound}${symbol_pound} Container Delivery

The generated project uses one source-building `deploy/container/Dockerfile`:

```bash
docker build --build-arg CONTAINER_ENGINE=docker -f deploy/container/Dockerfile -t ${rootArtifactId}:local .
podman build --build-arg CONTAINER_ENGINE=podman -f deploy/container/Dockerfile -t ${rootArtifactId}:local .
nerdctl build --build-arg CONTAINER_ENGINE=nerdctl -f deploy/container/Dockerfile -t ${rootArtifactId}:local .
```

Start the complete Docker development stack with:

```bash
docker compose --env-file deploy/env/.env.example -f deploy/compose/compose.docker.yaml up -d --build
```

Podman and nerdctl use `compose.podman.yaml` and `compose.nerdctl.yaml`. Production
uses the matching `.prod.yaml` file and an operator-owned `.env.prod`. See
`deploy/container/README.md` for rootless prerequisites, persistence, production
boundaries, and data-deletion warnings.

The root `Jenkinsfile` runs tests and can publish immutable images. Set
`PUBLISH_IMAGE=true` plus registry parameters to publish; it never deploys.
````

- [ ] **Step 6: Update the Service GitHub Actions image build**

Replace its final build step with:

```yaml
      - run: >-
          docker build
          --build-arg CONTAINER_ENGINE=docker
          --file deploy/container/Dockerfile
          --tag ${rootArtifactId}:ci
          .
```

Keep checkout, JDK 21 setup, Maven cache, test, and package steps unchanged.

- [ ] **Step 7: Run the three focused integration tests**

Run the three Task 1 integration-test commands.

Expected: all exit 0; generated READMEs reference only the new paths, the operator guide states production limits and data safety, and Service GitHub Actions builds through the portable Dockerfile.

- [ ] **Step 8: Commit documentation and existing-CI alignment**

```bash
git add -- \
  egon-cola-archetypes/egon-cola-archetype-light \
  egon-cola-archetypes/egon-cola-archetype-service \
  egon-cola-archetypes/egon-cola-archetype-web
git diff --cached --check
git commit -m "docs(archetype): document container delivery"
```

## Final Verification Checkpoint (No Empty Commit)

- [ ] **Step 1: Validate archetype metadata XML**

Run:

```bash
for file in \
  egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/META-INF/maven/archetype-metadata.xml \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/META-INF/maven/archetype-metadata.xml \
  egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/META-INF/maven/archetype-metadata.xml; do
  xmllint --noout "$file"
done
```

Expected: no output and exit 0.

- [ ] **Step 2: Run the authoritative archetype reactor integration test**

```bash
bash ./mvnw -B -ntp -f egon-cola-archetypes/pom.xml clean integration-test
```

Expected: `BUILD SUCCESS`; all three `verify.groovy` contracts pass.

- [ ] **Step 3: Run generated-project Maven verification**

Run:

```bash
bash egon-cola-archetypes/egon-cola-archetype-light/target/test-classes/projects/basic/project/basic/mvnw \
  -B -ntp -f egon-cola-archetypes/egon-cola-archetype-light/target/test-classes/projects/basic/project/basic/pom.xml clean verify

bash egon-cola-archetypes/egon-cola-archetype-service/target/test-classes/projects/basic/project/student-management-evaluation/mvnw \
  -B -ntp -f egon-cola-archetypes/egon-cola-archetype-service/target/test-classes/projects/basic/project/student-management-evaluation/pom.xml clean verify

bash egon-cola-archetypes/egon-cola-archetype-web/target/test-classes/projects/basic/project/student-management-organization/mvnw \
  -B -ntp -f egon-cola-archetypes/egon-cola-archetype-web/target/test-classes/projects/basic/project/student-management-organization/pom.xml clean verify
```

Expected: all three exit 0. If the Maven Invoker uses a different generated directory in the installed Maven version, use the exact `projectDir` printed by the already-passing `verify.groovy`; do not alter the archetype to fit a stale target path.

- [ ] **Step 4: Parse every Compose file with each locally available engine without starting services**

Set validation-only production values in the shell:

```bash
export REGISTRY=registry.example.test
export REGISTRY_NAMESPACE=validation
export IMAGE_TAG=validation
export POSTGRES_USER=validation
export POSTGRES_PASSWORD=validation-postgres
export REDIS_PASSWORD=validation-redis
export RABBITMQ_USERNAME=validation
export RABBITMQ_PASSWORD=validation-rabbitmq
export NACOS_USERNAME=validation
export NACOS_PASSWORD=validation-nacos
export NACOS_AUTH_TOKEN=VGhpc0lzQVNlY3VyZU5hY29zVG9rZW5Gb3JWYWxpZGF0aW9u
export NACOS_AUTH_IDENTITY_KEY=serverIdentity
export NACOS_AUTH_IDENTITY_VALUE=validation
export ORGANIZATION_FACADE_ENABLED=false
export EVALUATION_FACADE_ENABLED=false
```

For each of these generated project roots:

```text
egon-cola-archetypes/egon-cola-archetype-light/target/test-classes/projects/basic/project/basic
egon-cola-archetypes/egon-cola-archetype-service/target/test-classes/projects/basic/project/student-management-evaluation
egon-cola-archetypes/egon-cola-archetype-web/target/test-classes/projects/basic/project/student-management-organization
```

run the available parser commands from inside that root:

```bash
docker compose --env-file deploy/env/.env.example -f deploy/compose/compose.docker.yaml config >/dev/null
docker compose --env-file deploy/env/.env.prod.example -f deploy/compose/compose.docker.prod.yaml config >/dev/null

podman compose --env-file deploy/env/.env.example -f deploy/compose/compose.podman.yaml config >/dev/null
podman compose --env-file deploy/env/.env.prod.example -f deploy/compose/compose.podman.prod.yaml config >/dev/null

nerdctl --namespace default compose --env-file deploy/env/.env.example -f deploy/compose/compose.nerdctl.yaml config >/dev/null
nerdctl --namespace default compose --env-file deploy/env/.env.prod.example -f deploy/compose/compose.nerdctl.prod.yaml config >/dev/null
```

Expected: every command for an installed/configured engine exits 0 and does not create or start a container. Record a missing CLI, Compose provider, daemon, containerd endpoint, or BuildKit service as environment-blocked.

- [ ] **Step 5: Build generated images with each available engine, without running them**

From each generated project root, run the applicable command:

```bash
docker build --build-arg CONTAINER_ENGINE=docker -f deploy/container/Dockerfile -t archetype-container-verify:docker .
podman build --build-arg CONTAINER_ENGINE=podman -f deploy/container/Dockerfile -t archetype-container-verify:podman .
nerdctl --namespace default build --build-arg CONTAINER_ENGINE=nerdctl -f deploy/container/Dockerfile -t archetype-container-verify:nerdctl .
```

Expected: available engines build the image and do not start it. The Service/Web integration fixtures use Maven coordinates installed for the host-side archetype test; a clean image builder may not see those fixture artifacts. If that exact fixture-resolution limitation occurs, record Service/Web image build as environment-blocked, keep Light image-build evidence, and do not weaken the source-build Dockerfile or commit fixture repositories into generated projects.

- [ ] **Step 6: Validate the Jenkinsfile when a Declarative Pipeline linter is available**

If `JENKINS_URL`, `JENKINS_USER`, and `JENKINS_TOKEN` point to an authorized Jenkins instance, run for each generated project:

```bash
curl --fail --silent --show-error \
  --user "$JENKINS_USER:$JENKINS_TOKEN" \
  --request POST \
  --form "jenkinsfile=<Jenkinsfile" \
  "${JENKINS_URL%/}/pipeline-model-converter/validate"
```

Expected: response contains `Jenkinsfile successfully validated`. If no authorized linter is available, report the linter check as environment-blocked; the generated Groovy structural contract remains required.

- [ ] **Step 7: Perform final scope, migration, and whitespace checks**

```bash
git diff --check "$IMPLEMENTATION_BASE"..HEAD
git diff --name-only "$IMPLEMENTATION_BASE"..HEAD
git diff --exit-code "$IMPLEMENTATION_BASE"..HEAD -- '**/db/migration/**'
git status --short
```

Expected:

- whitespace check exits 0;
- changed paths are limited to the three archetypes;
- Flyway diff is empty;
- worktree is clean;
- exactly six task commits exist after `IMPLEMENTATION_BASE`.

Do not create an empty commit for this checkpoint. If validation reveals a defect, fix it in the task that owns the file, rerun that task's focused checks and the full checkpoint, then create one narrowly scoped corrective commit with the reason recorded in the final handoff.
