# Archetype Container Runtime, Compose, and Jenkins Design

Date: 2026-07-13

Status: User-approved design. Implementation planning remains gated until the
user reviews this written spec.

## Goal

Extend all three Egon-COLA archetypes so each generated project provides:

- one portable OCI-compatible Dockerfile usable by Docker, Podman, and
  nerdctl/BuildKit;
- convenient full-stack Compose definitions for development;
- explicit single-host production Compose templates;
- rootless- and rootful-compatible container defaults;
- a standalone declarative Jenkins Pipeline for test, image build, and image
  publication;
- generated documentation and archetype verification for the complete delivery
  contract.

The target archetypes are:

```text
egon-cola-archetypes/egon-cola-archetype-light
egon-cola-archetypes/egon-cola-archetype-service
egon-cola-archetypes/egon-cola-archetype-web
```

The generated applications and their existing runtime behavior remain the source
of truth. This work adds deployment and CI/CD delivery surfaces without changing
business behavior, module dependency direction, database schemas, or protocol
contracts.

## Confirmed Decisions

The user approved these decisions on 2026-07-13:

1. All three archetypes are in scope.
2. Docker, Podman, and nerdctl/containerd receive explicit Compose files rather
   than relying on one shared Compose file plus runtime-specific overrides.
3. Image construction uses exactly one Dockerfile. The design must not create
   `Dockerfile.containerd`, `Dockerfile.nerdctl`, `Containerfile.podman`, or other
   duplicate build definitions.
4. The Dockerfile lives at `deploy/container/Dockerfile`. The generated root
   Dockerfile is removed and no compatibility copy remains.
5. The Dockerfile builds from source through a Maven builder stage. A clean
   checkout can therefore use Compose `up --build` without first packaging on the
   host.
6. Development Compose starts the application, PostgreSQL, Redis, RabbitMQ, and
   Nacos by default.
7. Both development and production Compose variants are generated.
8. Container definitions must avoid unnecessary privileged features and support
   both rootless and rootful execution.
9. Jenkins provides CI and image publication, but does not deploy or run Compose.
10. Jenkins can select `docker`, `podman`, or `nerdctl` for image build, login, and
    push.
11. Registry address, namespace, credentials ID, image name, and image tag remain
    parameterized. The generated pipeline does not bind users to Harbor, Docker
    Hub, or another registry product.
12. Each generated project contains a complete declarative `Jenkinsfile` and does
    not depend on a Jenkins Shared Library.
13. Validation must not start the generated application or its infrastructure.
    The user will perform runtime testing.

## Design Authority and Precedence

Implementation decisions follow this order:

1. The confirmed decisions in this spec.
2. The existing runtime profiles, ports, integrations, and artifact layout of the
   target archetype.
3. Existing archetype style and generation-time verification conventions.
4. Runtime-specific compatibility requirements proven by Docker, Podman, or
   nerdctl behavior.

Where a runtime supports the same standard Dockerfile or Compose behavior, the
implementation must keep the behavior the same. Runtime-specific divergence is
allowed only when the tools demonstrably require it.

## Scope

Implementation may change only deployment-contract files owned by the three
archetypes:

- `src/main/resources/archetype-resources/deploy/**`;
- the generated root `Jenkinsfile`;
- the generated root README;
- generated `.gitignore`, `.dockerignore`, and related delivery metadata;
- `META-INF/maven/archetype-metadata.xml` file-set declarations;
- `src/test/resources/projects/basic/verify.groovy`;
- the service archetype's existing generated GitHub Actions workflow, only where
  its Dockerfile path must change;
- archetype-local test fixtures required to prove the generated contract.

The existing generated root Dockerfile is moved into the new deployment layout;
it is not retained as a second file.

## Out of Scope

- Changing generated Java business behavior.
- Changing generated Maven module boundaries or dependency graphs.
- Adding Kubernetes, Helm, Nomad, Swarm, or OpenShift resources.
- Adding Podman Quadlet or systemd units.
- Adding a Jenkins Shared Library.
- Deploying from Jenkins.
- Binding the pipeline to a specific registry vendor.
- Building multi-architecture manifest lists.
- Providing multi-host high availability for PostgreSQL, Redis, RabbitMQ, or
  Nacos.
- Automating database backup, restore, TLS certificate issuance, or disaster
  recovery.
- Editing an existing Flyway migration or adding a database migration.
- Changing repository components or root business code.
- Starting containers, generated applications, browsers, or external services as
  part of implementation validation.
- Migrating projects generated by older archetype releases.

## Generated File Layout

Each generated project has this delivery layout:

```text
deploy/
├── container/
│   ├── Dockerfile
│   └── README.md
├── compose/
│   ├── compose.docker.yaml
│   ├── compose.docker.prod.yaml
│   ├── compose.podman.yaml
│   ├── compose.podman.prod.yaml
│   ├── compose.nerdctl.yaml
│   └── compose.nerdctl.prod.yaml
└── env/
    ├── .env.example
    └── .env.prod.example
Jenkinsfile
```

The top-level generated README introduces the commands. The deployment README
owns detailed container-engine prerequisites, environment variables, production
boundaries, and troubleshooting.

Production Compose files are standalone definitions. They are not override files
and do not require merging with the development files. This avoids relying on
Compose merge semantics that may differ among Docker Compose, Podman Compose
providers, and nerdctl Compose.

## Single Portable Dockerfile

### Naming and Tool Semantics

The only build definition is:

```text
deploy/container/Dockerfile
```

Docker, Podman, and nerdctl are treated as container build and management entry
points. The design does not call them CRI implementations, and it does not treat
containerd itself as a Dockerfile builder.

Representative commands are:

```bash
docker build --build-arg CONTAINER_ENGINE=docker \
  -f deploy/container/Dockerfile .

podman build --build-arg CONTAINER_ENGINE=podman \
  -f deploy/container/Dockerfile .

nerdctl build --build-arg CONTAINER_ENGINE=nerdctl \
  -f deploy/container/Dockerfile .
```

### Build Stages

The Dockerfile contains three stages:

```text
builder -> extractor -> runtime
```

#### Builder

The builder stage:

- uses a pinned JDK 21-capable build image;
- copies Maven Wrapper files, Maven POMs, and project sources;
- runs the generated project's Maven Wrapper;
- executes a batch, no-transfer-progress package with tests skipped;
- produces the Spring Boot executable JAR from the correct module.

The JAR location differs by archetype:

- light uses the single root module's `target` directory;
- service uses the generated `${rootArtifactId}-starter` module;
- web uses the generated `${rootArtifactId}-starter` module.

The builder does not use Docker-only daemon features or BuildKit-only cache mount
syntax. Native engine layer caches may still be used by the selected tool.

#### Extractor

The extractor uses Spring Boot's `jarmode=tools` to extract:

```text
dependencies
spring-boot-loader
snapshot-dependencies
application
```

The four layers are copied independently into the runtime image to preserve the
existing layered-image behavior.

#### Runtime

The runtime stage:

- uses a pinned JRE 21 image;
- runs with a fixed configurable non-root UID and GID;
- contains only the extracted application and the minimum health-check tooling;
- keeps `TZ`, `SPRING_PROFILES_ACTIVE`, and `JAVA_OPTS` configurable;
- launches `JarLauncher` through an `exec` entrypoint so the JVM receives signals;
- exposes only the ports owned by the generated archetype.

The light and web applications expose their HTTP and Dubbo ports. The service
archetype exposes its Actuator management port and Dubbo port; it does not gain a
business HTTP endpoint.

### Build Arguments

The Dockerfile supports these arguments:

```text
CONTAINER_ENGINE
BUILD_IMAGE
RUNTIME_IMAGE
APP_UID
APP_GID
JAR_FILE
```

`CONTAINER_ENGINE` accepts `oci`, `docker`, `podman`, or `nerdctl`. It records the
selected build entry point in OCI image metadata and reserves one controlled
variation point. The initial implementation must not create conditional behavior
without a proven runtime incompatibility.

The other arguments permit controlled image, user, group, and artifact overrides.
Each archetype supplies defaults matching its generated project.

### Pattern Decision

No Strategy classes, Template Method hierarchy, code generator, or build wrapper
framework is introduced. The runtime choices form a small, fixed variation point.
One standard Dockerfile plus explicit command selection is clearer than a new
abstraction and prevents three build files from evolving independently.

## Compose Design

### Responsibility

The Dockerfile builds one application image. Compose definitions orchestrate the
application and its infrastructure:

```text
application
├── PostgreSQL
├── Redis
├── RabbitMQ
└── Nacos
```

Every Compose file defines an explicit project network, named data volumes, image
versions through environment variables, and health checks appropriate to its
runtime.

Image tags must be pinned to explicit configurable versions. The generated
examples must not use floating `latest` tags for infrastructure.

### Runtime-Specific Files

The Docker files are invoked with `docker compose`. The Podman files are invoked
through `podman compose` and its configured Compose provider. The nerdctl files
are invoked with `nerdctl compose` against the configured containerd namespace
and BuildKit service.

The six files remain independently readable. Shared variable names and service
names form the cross-runtime contract, while tool-specific capabilities may be
expressed in their respective files. A change to one runtime must be reviewed
against the other two to prevent behavioral drift.

### Development Definitions

The development files:

- build the application from `deploy/container/Dockerfile`;
- pass the corresponding `CONTAINER_ENGINE` value;
- start application, PostgreSQL, Redis, RabbitMQ, and Nacos by default;
- use values from `deploy/env/.env.example`;
- expose application, Dubbo, RabbitMQ management, and Nacos ports needed for
  local integration;
- persist infrastructure data through named volumes;
- enable Nacos configuration/discovery and the real database, cache, and broker
  adapters;
- wait for infrastructure health before attempting application startup;
- keep external cross-project Facade clients disabled so each generated project
  can start independently.

The service development definition sets
`ORGANIZATION_FACADE_ENABLED=false`. The web development definition sets
`EVALUATION_FACADE_ENABLED=false`. Light keeps any unrelated external HTTP client
disabled unless that provider is explicitly configured.

Representative Docker usage is:

```bash
docker compose \
  --env-file deploy/env/.env.example \
  -f deploy/compose/compose.docker.yaml \
  up -d --build
```

Equivalent Podman and nerdctl commands are documented using their own files and
supported CLI syntax.

### Production Definitions

The production definitions:

- use the `prod` Spring profile;
- pull the application image from the configured registry instead of compiling
  source on the server;
- refer to `${REGISTRY}/${REGISTRY_NAMESPACE}/${IMAGE_NAME}:${IMAGE_TAG}`;
- require explicit production credentials and connection variables;
- expose only required application-facing ports by default;
- do not publish infrastructure management ports to the host by default;
- retain named data volumes across ordinary stop and down operations;
- define health checks, restart behavior, bounded logs, and runtime-appropriate
  resource controls;
- avoid privileged mode, host networking, runtime sockets, and fixed host data
  paths.

Production must explicitly select whether cross-project Facade clients are
enabled. If a Facade is enabled but no provider is registered, the application
retains its current fail-fast behavior. The Compose template does not silently
fall back to a local stub.

The production definitions provide a single-host deployment baseline. They do not
claim high availability. Users may replace included infrastructure endpoints with
external managed or clustered services through environment variables.

## Environment Files and Secret Policy

### Development Example

`deploy/env/.env.example` contains non-sensitive sample values suitable only for
local development. It documents:

- application and management ports;
- PostgreSQL database, username, password, and image version;
- Redis password and image version;
- RabbitMQ username, password, management port, and image version;
- Nacos credentials, ports, namespace, and image version;
- application image name and build arguments.

### Production Example

`deploy/env/.env.prod.example` lists production variable names and descriptive
placeholders. It does not contain usable production passwords. Required Compose
values use fail-fast variable expressions so missing configuration fails before
containers start.

The generated `.gitignore` and `.dockerignore` exclude:

```text
.env
.env.*
!.env.example
!.env.prod.example
secrets/
*.pem
*.key
```

The exact patterns must preserve the two committed examples while excluding
operator-created environment and secret files.

Compose, Dockerfile, Jenkinsfile, and generated documentation must not embed real
credentials.

## Health, Startup, and Data Safety

The startup dependency flow is:

```text
PostgreSQL --\
Redis -------+--> application --> Actuator readiness
RabbitMQ ----+
Nacos -------/
```

Infrastructure uses native health commands where provided by its image. The
application checks its existing Spring Boot Actuator readiness endpoint. The
runtime image may add only the minimal HTTP client required for that check and
must remove package-manager caches in the same image layer.

Runtime-specific Compose files may express readiness ordering differently when a
Compose implementation lacks equivalent `depends_on` behavior. The observable
contract remains that an unhealthy dependency does not produce a false healthy
application state.

Ordinary stop or down operations retain named volumes. Documentation must warn
that an explicit volume-removal command deletes local data. No generated helper
automatically performs destructive cleanup.

## Rootless and Rootful Compatibility

The application image runs as a non-root numeric user. Compose definitions:

- use ports above 1024;
- use named volumes instead of fixed root-owned host directories;
- do not require `privileged`;
- do not use host networking;
- do not mount Docker, Podman, or containerd sockets;
- do not require a fixed Linux security-label configuration;
- respect the supported user model of each pinned infrastructure image.

The deployment README documents engine prerequisites and any host preparation that
cannot be encoded portably. Rootless support does not imply that an unavailable or
misconfigured containerd, BuildKit, Podman socket, or Compose provider will be
installed or repaired by the generated project.

## Jenkins Pipeline

### Form and Prerequisites

Each generated project contains a standalone declarative `Jenkinsfile`. It assumes
a Linux Jenkins agent with JDK 21, the executable Maven Wrapper, and the selected
container tool. Only the selected tool must be installed:

- Docker requires an accessible Docker daemon;
- Podman may use a rootless or rootful connection;
- nerdctl requires accessible containerd and BuildKit services.

The pipeline does not install engines, alter daemon configuration, start Compose,
or deploy the application.

### Parameters

The Jenkinsfile defines:

```text
CONTAINER_ENGINE          docker | podman | nerdctl
REGISTRY                  registry host
REGISTRY_NAMESPACE        registry project or namespace
REGISTRY_CREDENTIALS_ID   Jenkins username/password credentials ID
IMAGE_NAME                defaults to the generated rootArtifactId
IMAGE_TAG                 optional explicit tag
PUBLISH_IMAGE             enable registry login and push
PUBLISH_LATEST            publish the latest alias under guarded conditions
```

`PUBLISH_IMAGE` and `PUBLISH_LATEST` default to `false`. A newly generated
project therefore runs CI without requiring registry credentials, while image
publication is an explicit job choice. Enabling publication requires non-empty
registry, namespace, and credentials parameters.

When `IMAGE_TAG` is blank, the pipeline creates an immutable tag:

```text
<project-version>-<build-number>-<short-git-commit>
```

Tag components are normalized to registry-safe characters. The immutable tag is
the primary published reference.

### Stages

The pipeline contains these stages:

1. `Checkout`
   - checks out source;
   - records the full and short commit;
   - reads the Maven project version;
   - computes the image reference.
2. `Preflight`
   - checks JDK 21 and the Maven Wrapper;
   - checks only the selected container command;
   - verifies the selected daemon, Podman connection, or containerd/BuildKit
     endpoint before image work.
3. `Test`
   - sets `SPRING_PROFILES_ACTIVE=test`;
   - runs Maven `clean verify`;
   - uses the archetype's existing external-service-free test behavior.
4. `Build Image`
   - builds through `deploy/container/Dockerfile`;
   - passes the selected `CONTAINER_ENGINE`;
   - applies the immutable image tag.
5. `Publish Image`
   - runs only when `PUBLISH_IMAGE=true`;
   - validates required registry parameters;
   - logs in with credentials bound by Jenkins;
   - pushes through the selected container command;
   - optionally pushes `latest` only for `main` or `master`, never for a change
     request.
6. `Post`
   - publishes available test reports;
   - logs out of the registry after a login attempt;
   - reports the immutable image coordinate;
   - does not remove shared agent caches or images destructively.

### Credential Handling

Registry username and password come from Jenkins Credentials Binding. The
password is sent to the selected login command through standard input. Shell
tracing is disabled around credential operations. The Jenkinsfile must not print
the password, interpolate it into the image reference, or persist it in generated
files.

Login, build, or push failure fails the stage and the pipeline. A failed push must
not be reported as a published image.

### Existing GitHub Actions

The service archetype's existing generated GitHub Actions workflow remains. Its
Docker build command is updated to point at `deploy/container/Dockerfile` and to
remain consistent with the source-building image contract. This work does not add
GitHub Actions to the light or web archetypes and does not replace GitHub Actions
with Jenkins.

## Error Handling

- Missing required production variables fail Compose configuration before
  container startup.
- Unhealthy infrastructure prevents a false successful application startup.
- Failed application readiness marks the application unhealthy.
- Missing production images fail rather than falling back to a local source build.
- Enabled but unavailable remote Facades retain fail-fast behavior.
- Missing container commands or unavailable daemons fail Jenkins preflight.
- Missing registry parameters fail before login.
- Registry login and push errors fail the Jenkins publication stage.
- Ordinary shutdown retains named volumes and user data.
- No validation or generated helper automatically starts or destroys services.

## Archetype Generation Contract

Each archetype metadata file must include the new deployment files with the
correct filtering mode:

- Velocity placeholders such as `${rootArtifactId}` and the generated package
  coordinates are filtered where required;
- shell, Compose, and Jenkins dollar expressions are protected from accidental
  Velocity expansion;
- environment example files are generated under their dotfile names;
- the Jenkinsfile is generated at the project root;
- obsolete root Dockerfile metadata is removed.

`verify.groovy` for each archetype asserts:

- `deploy/container/Dockerfile` exists;
- no generated root Dockerfile exists;
- no duplicate engine-specific Dockerfile or Containerfile exists;
- builder, extractor, and runtime stages exist;
- `ARG CONTAINER_ENGINE`, non-root execution, layered extraction, and the correct
  artifact path exist;
- all six Compose definitions exist;
- both environment example files and deployment README exist;
- all Compose files define application, PostgreSQL, Redis, RabbitMQ, Nacos,
  health checks, networks, and named volumes;
- production files do not contain known development passwords;
- the Jenkinsfile contains engine selection, test, build, credential-bound login,
  push, and guarded latest behavior;
- archetype-specific ports, JAR paths, and Facade flags match the generated
  application;
- generated documentation references only current paths and commands.

The checks should assert durable behavior and exact critical paths without
snapshotting every line of YAML or Jenkins syntax.

## Validation Strategy

Validation proceeds from generation contract to optional local tooling:

1. Run the archetype reactor's `clean integration-test` so all three generated
   projects and their `verify.groovy` contracts are exercised.
2. Run Maven `clean verify` inside each generated project.
3. Run the available Docker, Podman, and nerdctl Compose `config` commands against
   their development and production files without starting services.
4. Build the generated application image with each container engine available in
   the environment.
5. Validate the Jenkinsfile with an installed Declarative Pipeline linter when
   available; otherwise perform structural checks and report the missing linter.
6. Run `git diff --check` and a path-scope review.

No `compose up`, container run, generated application start, browser, or external
service start is permitted during validation. If an engine, daemon, containerd,
BuildKit, Compose provider, or Jenkins linter is unavailable, that validation is
reported as environment-blocked rather than passed.

## Implementation Boundaries by Archetype

### Light

- Single-module JAR path.
- HTTP port 8080 and Dubbo port 50051 by current default.
- Full infrastructure enabled in development Compose.
- Unconfigured unrelated external HTTP clients remain disabled.

### Service

- Starter-module JAR path.
- Actuator management port 8081 and Dubbo port 50051 by current default.
- No business HTTP port or Controller is added.
- `ORGANIZATION_FACADE_ENABLED=false` in development Compose.
- Existing generated GitHub Actions remains and uses the new Dockerfile path.

### Web

- Starter-module JAR path.
- HTTP port 8080 and Dubbo port 50051 by current default.
- `EVALUATION_FACADE_ENABLED=false` in development Compose.

## Documentation Requirements

The generated top-level README provides concise commands for:

- Docker, Podman, and nerdctl image builds;
- development Compose startup and shutdown;
- production environment preparation and Compose startup;
- Jenkins job prerequisites and parameter meanings.

`deploy/container/README.md` documents:

- why one Dockerfile is shared;
- the meaning of `CONTAINER_ENGINE`;
- Docker, Podman, and nerdctl prerequisites;
- rootless and rootful considerations;
- development versus production Compose boundaries;
- registry image naming;
- health checks and persistent volumes;
- explicit data-deletion warnings;
- known environment-dependent validation limits.

Documentation must not describe the single-host production template as a highly
available production platform.

## Completion Criteria

The implementation is complete when:

- all three archetypes generate the confirmed deployment layout;
- one Dockerfile builds each generated project from source;
- Docker, Podman, and nerdctl have explicit development and production Compose
  files;
- development files define the full required infrastructure stack;
- production files fail fast on missing required configuration and do not embed
  development credentials;
- rootless-safe constraints are present;
- each generated project has a standalone parameterized Jenkinsfile;
- Jenkins tests, builds, and can publish with the selected engine without
  deploying;
- archetype integration tests and generated-project Maven verification pass;
- every locally available static Compose, image build, and Jenkins lint check is
  reported accurately;
- no application or infrastructure is automatically started;
- no files outside the approved archetype and design/plan scope are modified.
