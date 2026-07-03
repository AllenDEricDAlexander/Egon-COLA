# Runtime Engineering Archetype Baseline Design

Date: 2026-07-03

## Goal

Upgrade `egon-cola-archetype-web`, `egon-cola-archetype-service`, and
`egon-cola-archetype-light` so each generated project has the same Spring Boot
runtime engineering baseline: environment-aware configuration, configuration
decryption, validation, virtual threads, async execution, graceful shutdown,
Actuator, Flyway, HikariCP, Jackson defaults, executable jar packaging, Docker
packaging, and CI verification.

This is an archetype engineering baseline change. It must preserve the current
Clean Architecture boundaries, the current generated project identities, and the
light archetype's single-module package-layer model.

## Confirmed Decisions

- Use one total design spec for all three archetypes, with archetype-specific
  differences called out in each section.
- Keep the existing generated project targets:
  - `egon-cola-archetype-light`: single-module `student-management`.
  - `egon-cola-archetype-web`: multi-module `student-management-organization`.
  - `egon-cola-archetype-service`: multi-module `student-management-evaluation`.
- Implement the first configuration encryption version directly inside the
  generated projects. Do not extract `egon-cola-component-config-starter` in this
  change.
- Add Nacos discovery/config dependencies by default where the generated project
  supports bootstrap configuration.
- `local` profile defaults to no external registry/config center so generated
  projects remain easy to start and test locally.
- `dev` profile defaults to connecting to Nacos through environment variables.
- `test` profile defaults to no external registry/config center so CI does not
  require Nacos.
- `prod` profile must use environment variables, mounted files, or platform
  secrets for registry, config center, database, and decryption secrets.
- Service archetype remains a pure service archetype. It must not gain business
  HTTP controllers, Web filters, or a Web application boundary.
- Service archetype may expose a dedicated management HTTP port for Actuator
  health and metrics.
- Dockerfiles must support GitHub Actions `docker build` verification, but the
  implementation workflow does not require starting containers locally.
- Jackson must keep `Long` values as JSON numbers. Do not add global Long-to-
  String serialization.
- Update GitHub Actions to verify archetype integration tests, generated project
  test/package commands, and generated Docker builds.
- Write and commit this design before implementation planning.

## Current Context

The three archetypes already generate Java 21 Spring Boot projects with the
intended broad architecture shapes. The current baseline is incomplete for
enterprise runtime delivery:

- Configuration is mostly a single `application.yml`, without consistent
  `bootstrap*.yml` and profile-specific runtime configuration.
- Bootstrap files would not be reliable unless bootstrap support is included.
- Validation support is inconsistent across web, service, light, facade,
  application, domain, and infrastructure boundaries.
- Service currently stays pure service, but needs an explicit management endpoint
  strategy for operations.
- Dockerfiles and `.dockerignore` are not consistently generated.
- Executable jar packaging is not expressed as a durable generated-project
  contract across all archetypes.
- Configuration encryption is not available in generated projects.
- CI does not yet prove that generated projects can test, package, and build
  Docker images.

The existing archetype designs and follow-up clean architecture decisions remain
authoritative unless this spec explicitly changes runtime engineering behavior.

## Scope

In scope:

- `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources`.
- `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/META-INF/maven/archetype-metadata.xml`.
- `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic`.
- `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources`.
- `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/META-INF/maven/archetype-metadata.xml`.
- `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic`.
- `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources`.
- `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/META-INF/maven/archetype-metadata.xml`.
- `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic`.
- Generated POMs, generated source templates, generated tests, generated
  resources, generated README text, generated Dockerfiles, and generated
  `.dockerignore` files.
- `.github/workflows/**` files required to verify the generated project runtime
  baseline.

Out of scope:

- Updating `cola-samples`.
- Redesigning the three archetype business samples.
- Replacing Clean Architecture with DDD command/query/usecase packages.
- Adding a business Web surface to the service archetype.
- Adding native grpc-java services or proto service modules.
- Extracting a reusable configuration starter in `egon-cola-components`.
- Starting generated applications or running containers locally as part of the
  implementation workflow.
- Editing already published migration files outside archetype templates.

## Generated Configuration Files

Each generated runnable project must contain these files in its runnable module:

```text
bootstrap.yml
bootstrap-local.yml
bootstrap-dev.yml
bootstrap-test.yml
bootstrap-prod.yml
application.yml
application-local.yml
application-dev.yml
application-test.yml
application-prod.yml
```

Placement:

- Web: `${rootArtifactId}-starter/src/main/resources`.
- Service: `${rootArtifactId}-starter/src/main/resources`.
- Light: `src/main/resources`.

`bootstrap*.yml` owns startup-early configuration:

- `spring.application.name`.
- Spring Cloud bootstrap/discovery/config flags.
- Nacos config/discovery server, namespace, group, username, and password
  placeholders.
- Dubbo registry address.
- Other config-center or service-discovery values that must exist before the
  main application context binds.

`application*.yml` owns main runtime configuration:

- HTTP server and management server.
- Tomcat, where applicable.
- virtual threads and async execution.
- Jackson.
- datasource, HikariCP, JPA, and Flyway.
- Actuator.
- logging.
- application defaults.
- optional imports for local external config and configtree secrets.

The same logical setting should not be defined in both bootstrap and application
files unless the generated README explains the override reason.

## Profile Rules

`application.yml` and `bootstrap.yml` must set `local` as the default profile:

```yaml
spring:
  profiles:
    default: local
```

Profile behavior:

- `local` defaults to external registry/config center off. Generated projects
  should be easy to start and test without Nacos.
- `local` may be switched to Nacos by environment variables:
  `NACOS_CONFIG_ENABLED=true`, `NACOS_DISCOVERY_ENABLED=true`,
  `NACOS_SERVER_ADDR=...`, and `DUBBO_REGISTRY_ADDRESS=nacos://...`.
- `dev` defaults to external registry/config center on and reads all connection
  details from environment variables with development-safe defaults where that
  does not create secret leakage.
- `test` defaults to external registry/config center off and is the profile used
  by CI-generated project verification.
- `prod` defaults to external registry/config center on, but has no local server
  fallback for sensitive or environment-specific values.

`prod` must not include real usernames, passwords, tokens, database credentials,
or decryption keys. Missing required production values should fail fast.

## Bootstrap Support And Nacos

Generated projects that include `bootstrap*.yml` must also include bootstrap
runtime support. The preferred baseline is `spring-cloud-starter-bootstrap` so
teams do not need to remember `spring.cloud.bootstrap.enabled=true`.

Nacos is the default reserved implementation for discovery and config:

- Add Nacos discovery/config dependencies where the generated project needs the
  bootstrap Nacos settings to work.
- Keep Nacos disabled by default in `local` and `test`.
- Keep Nacos enabled by default in `dev` and `prod`.
- All Nacos server addresses, namespaces, groups, usernames, and passwords must
  be environment-variable driven.

Dubbo registry defaults:

- `local`: `DUBBO_REGISTRY_ADDRESS:N/A`.
- `test`: `DUBBO_REGISTRY_ADDRESS:N/A`.
- `dev`: `DUBBO_REGISTRY_ADDRESS:nacos://${NACOS_SERVER_ADDR}`.
- `prod`: `DUBBO_REGISTRY_ADDRESS:nacos://${NACOS_SERVER_ADDR}` with required
  production variables.

Service archetype must keep business RPC/MQ boundaries. Bootstrap support must
not imply adding a business Web module or controller.

## Configuration Encryption

The first encryption version is generated-project local code. It is not a shared
Egon component yet.

Supported encrypted value format:

```text
ENC(v1:base64(iv):base64(ciphertext):base64(tag))
```

Rules:

- Algorithm: AES-256-GCM.
- `v1` reserves future algorithm evolution.
- IV must be random for each encryption operation.
- The same plaintext encrypted twice must produce different ciphertext.
- Plaintext, ciphertext, and keys must not be logged.
- Plain properties must keep working when no `ENC(...)` values are present.
- If any `ENC(...)` value is present and decryption fails, startup must fail.
- In non-`local` profiles, an encrypted value without an available key must fail
  startup.
- In `local`, a missing key is allowed only when no encrypted values are used.

Allowed key sources, in priority order:

```text
EGON_CONFIG_DECRYPT_KEY
EGON_CONFIG_DECRYPT_KEY_FILE
/run/secrets/egon_config_decrypt_key
```

Generated projects should support encrypted values from:

- `bootstrap*.yml` property sources before registry/config-center client binding.
- `application*.yml`.
- `config/application-secrets.yml`.
- `configtree:/run/secrets/`.
- External config center properties once they are visible to Spring Boot
  property binding.

Generated `bootstrap*.yml` templates should still avoid `ENC(...)` by default.
Registry and config-center credentials should prefer environment variables,
mounted files, or platform secrets. If a team chooses to use encrypted bootstrap
values later, generated code must decrypt them before Nacos or Dubbo registry
properties are bound.

Design pattern decision:

- Use a thin Strategy boundary for decryption, such as `ConfigDecryptor`.
- Provide `AesGcmConfigDecryptor` as the default implementation.
- Use an early Spring Boot extension point, such as `EnvironmentPostProcessor`,
  to detect and replace `ENC(...)` values before regular property binding.
- Do not add a heavy factory hierarchy. One interface plus one implementation is
  enough for this generated baseline and leaves a later KMS/Vault implementation
  possible.

Each generated project should include a small test or command-line helper that
can generate and verify an encrypted value without leaking secrets to logs.

## Validation Baseline

Validation is allowed across generated layers, but the dependency and usage rules
must preserve module boundaries.

Dependency rules:

- Facade contract modules use `jakarta.validation-api` only. They may place
  Bean Validation annotations on DTOs and RPC request objects.
- Adapter modules use `spring-boot-starter-validation`.
- Application modules may use Spring-managed `jakarta.validation.Validator` or
  narrow validation components for command and workflow object validation.
- Domain modules may use Bean Validation to express domain object invariants and
  may depend on validation support when the generated project needs executable
  constraint validation.
- Infrastructure modules may use validation for persistence config, external
  resource input, import rows, and integration-facing objects.
- Light is single-module and may depend on `spring-boot-starter-validation`, but
  package-level responsibilities still apply.

Boundary rules:

- Facade must not depend on Spring Boot starter dependencies.
- Domain must not depend on Spring Web, Servlet, adapter exceptions, HTTP error
  models, or controller types.
- Application must not format HTTP or RPC error responses.
- Infrastructure must not leak persistence-specific validation concerns into
  domain contracts.
- Static scattered `XxxValidator.validate(...)` utility classes are not the
  target style.

Validation component placement:

- Web/Service adapter may contain
  `${package}.adapter.validation.ValidatorUtils` for boundary and DTO validation.
- Light may contain `${package}.adapter.validation.ValidatorUtils`.
- Application/domain/infrastructure may contain named validation components when
  the responsibility belongs there, for example `CommandValidator`,
  `DomainInvariantValidator`, or `ImportRowValidator`.
- Generated validation components should be Spring Beans when they need injected
  `jakarta.validation.Validator`.

Design pattern decision:

- Use Bean Validation annotations for field, nested object, group, and cross-
  field request validation.
- Use custom `ConstraintValidator` implementations for cross-field validation.
- Use Specification, Policy, or domain methods for business rules only when the
  sample actually has a changing rule worth isolating.
- Do not introduce strategy/specification packages merely to make the template
  look more advanced.

## Virtual Threads And Async Execution

All generated projects must support Java 21 virtual threads:

```yaml
spring:
  threads:
    virtual:
      enabled: ${SPRING_THREADS_VIRTUAL_ENABLED:true}
```

Async baseline:

- Enable `@Async`.
- Provide an application async executor.
- If virtual threads are enabled, async execution may use virtual threads.
- If virtual threads are disabled, async execution must use a bounded thread
  pool with configurable core size, max size, and queue capacity.
- Async thread names must have a recognizable configurable prefix.
- Shutdown must wait for async work up to a configurable timeout.
- Async exceptions must be logged through a unified uncaught exception handler.

Placement:

- Web/Service: starter boot configuration.
- Light: `start` or equivalent boot configuration package.

Virtual threads do not remove the need for connection limits, database pool
limits, queue capacity, timeouts, or careful handling of synchronized/native/CPU-
bound code.

## Tomcat And Management Ports

Web and Light:

- Configure the business HTTP port with `SERVER_PORT`, defaulting to `8080`.
- Configure Tomcat thread, accept queue, connection, keep-alive, and timeout
  limits through environment variables.
- Keep the defaults conservative. Do not encode aggressive production tuning.
- Keep Tomcat protection settings even when virtual threads are enabled.

Service:

- Do not add Web dependencies to the service adapter or business modules.
- Do not generate business controllers, Web filters, or Web packages.
- Add Actuator and a dedicated management HTTP port, defaulting to a separate
  value such as `MANAGEMENT_SERVER_PORT=8081`.
- If Spring Boot requires a servlet or reactive web runtime to expose Actuator
  over HTTP, that runtime may be isolated in the service starter only and must be
  documented as management-only infrastructure.
- The management endpoint is for health, probes, info, metrics, and Prometheus
  only. It is not a business API surface.

## Graceful Shutdown

All generated projects must support graceful shutdown:

```yaml
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: ${SHUTDOWN_TIMEOUT:30s}
```

Rules:

- Web/Light stop accepting new HTTP requests and wait for accepted requests.
- Service waits for RPC/MQ resources, async execution, and connection pools to
  shut down.
- Async shutdown must set `await-termination=true`.
- Generated code must not use `System.exit(...)` for normal shutdown.
- Docker ENTRYPOINT must allow the Java process to receive SIGTERM.

## Actuator And Observability

Generated runnable modules must include Actuator. The baseline should also
include Prometheus registry so the configured endpoint exists consistently:

- `spring-boot-starter-actuator`.
- `micrometer-registry-prometheus`.

Default endpoint exposure:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: ${MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE:health,info,metrics,prometheus}
  endpoint:
    health:
      probes:
        enabled: true
      show-details: ${MANAGEMENT_HEALTH_SHOW_DETAILS:never}
  health:
    readinessstate:
      enabled: true
    livenessstate:
      enabled: true
```

Rules:

- `prod` default exposure must stay small.
- Do not expose `env`, `beans`, `configprops`, `heapdump`, or `threaddump` by
  default.
- Health details default to `never` in production.
- If Actuator is exposed outside the container network, the generated README must
  tell users to protect it through gateway, network, or security controls.

Expected endpoints:

- `/actuator/health`.
- `/actuator/health/liveness`.
- `/actuator/health/readiness`.
- `/actuator/metrics`.
- `/actuator/prometheus`.

## Flyway Migration

All three generated projects must support Flyway by default.

Directory:

```text
src/main/resources/db/migration/
```

Naming:

- Versioned scripts: `V{number}__description.sql`.
- Repeatable scripts: `R__description.sql`.

Rules:

- Already merged migration files must not be modified.
- Published schema changes must be added as new versioned migration files.
- `prod` must have `clean-disabled=true`.
- `validate-on-migrate=true` by default.
- JPA `ddl-auto` must be `validate` or `none`; do not use production `update`.
- Local/test defaults should keep generated projects testable with H2 or a
  PostgreSQL-compatible setup.

This spec does not require editing repository-level existing migrations outside
the archetype templates.

## HikariCP

All generated projects must include HikariCP configuration for datasource-backed
profiles:

- `pool-name`.
- `maximum-pool-size`.
- `minimum-idle`.
- `connection-timeout`.
- `idle-timeout`.
- `max-lifetime`.
- `keepalive-time`.
- `validation-timeout`.

Rules:

- Core values must be environment-variable overrideable.
- Production datasource URL, username, and password must come from environment
  variables, mounted secrets, configtree, or external config.
- Do not increase database pool defaults merely because virtual threads are
  enabled.
- Pool metrics must be visible through Micrometer/Actuator where available.

## Jackson JSON Baseline

Web and Light must configure JSON consistently:

- Human-readable date/time format.
- Time zone default from `APP_TIME_ZONE`, with `Asia/Shanghai` as the generated
  default.
- Do not write dates as timestamps.
- Do not output null fields by default.
- Do not fail on unknown request fields by default.
- Do not fail on empty beans by default.

Service facade DTOs may use Jackson annotations where a contract needs them, but
the JSON formatter itself belongs to runnable boot configuration, not domain.

Rules:

- Keep `Long` values as JSON numbers.
- Do not add global Long-to-String serialization.
- Amount examples should use `BigDecimal`, not `double`.
- Domain models should not add Jackson annotations merely for HTTP presentation.

## Maven Dependency Baseline

Root generated POMs:

- Keep Java 21.
- Keep Spring Boot parent inheritance.
- Continue managing Spring Boot, Spring Cloud, Spring Cloud Alibaba, Dubbo,
  Lombok, MapStruct Plus, and other project-wide versions where appropriate.
- Do not directly add runtime starter dependencies to root aggregator POMs.

Web starter:

- `spring-boot-starter`.
- `spring-boot-starter-actuator`.
- `spring-cloud-starter-bootstrap`.
- Nacos discovery/config dependencies.
- Prometheus registry.
- Internal dependencies on adapter and infrastructure.
- Spring Boot Maven plugin for repackage and layered jar.

Web adapter:

- `spring-boot-starter-web`.
- `spring-boot-starter-validation`.
- Dubbo starter.
- MapStruct Plus where converters are generated.

Service starter:

- `spring-boot-starter`.
- `spring-boot-starter-actuator`.
- `spring-cloud-starter-bootstrap`.
- Nacos discovery/config dependencies.
- Prometheus registry.
- A minimal management-only web runtime dependency if required to expose
  Actuator over HTTP on the dedicated management port.
- Internal dependencies on adapter and infrastructure.
- Spring Boot Maven plugin for repackage and layered jar.

Service adapter:

- No `spring-boot-starter-web`.
- Use `spring-boot-starter-validation` instead of only
  `jakarta.validation-api` for executable validation.
- Dubbo starter.
- MapStruct Plus where converters are generated.

Facade modules:

- `jakarta.validation-api`.
- No `spring-boot-starter-validation`.
- No Spring Web.
- No adapter/application/infrastructure/starter dependency.

Infrastructure modules:

- `spring-boot-starter-data-jpa`.
- `flyway-core`.
- H2 runtime.
- PostgreSQL runtime.
- MapStruct Plus where PO/domain converters are generated.

Light:

- `spring-boot-starter-web`.
- `spring-boot-starter-validation`.
- `spring-boot-starter-data-jpa`.
- `spring-boot-starter-actuator`.
- `spring-cloud-starter-bootstrap`.
- Nacos discovery/config dependencies.
- Prometheus registry.
- `flyway-core`.
- H2 runtime.
- PostgreSQL runtime.
- Dubbo starter.
- MapStruct Plus.
- Spring Boot Maven plugin for repackage and layered jar.

## Executable Jar Packaging

Web and Service generated projects:

- The `${rootArtifactId}-starter` module is the only runnable jar module.
- `./mvnw -DskipTests package` must produce:

```text
${rootArtifactId}-starter/target/${rootArtifactId}-starter-${version}.jar
```

- The jar must include adapter, application, domain, infrastructure, facade, and
  common dependencies through normal Maven dependency resolution.
- Non-starter modules must not be repackaged as independent boot applications.

Light generated project:

- The single module is the runnable jar.
- `./mvnw -DskipTests package` must produce:

```text
target/${artifactId}-${version}.jar
```

All archetypes:

- Enable Spring Boot layered jar.
- Exclude Lombok from runtime jars.
- Keep dependencies, snapshot dependencies, Spring Boot loader, and application
  content in separate layers for Docker cache behavior.

## Docker Packaging

Each generated project must include:

```text
Dockerfile
.dockerignore
```

Dockerfile baseline:

- Multi-stage build.
- Builder stage uses JDK and Maven Wrapper.
- Extractor stage extracts layered jar.
- Runtime stage uses a JRE image, not a full JDK image.
- Runtime stage runs as a non-root user.
- Runtime stage does not copy source, Maven local repository, build cache, test
  resources, `.git`, IDE files, logs, or secrets.
- ENTRYPOINT uses shell only to expand `JAVA_OPTS` and then `exec`s Java:

```dockerfile
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
```

The first implementation should use `eclipse-temurin:21-jre-jammy` for runtime
compatibility and debuggability. Distroless, Alpine, and jlink images are future
optimizations, not part of this implementation.

`.dockerignore` must exclude at least:

```text
.git
.github
.idea
.vscode
*.iml
.DS_Store
**/target
**/build
logs
*.log
.env
.env.*
config/*secret*
secrets
*.pem
*.key
```

Docker build must work in GitHub Actions. This spec does not require local
container startup or `docker run` during implementation.

## Archetype Verification

Each archetype's integration test must verify that generated projects contain
the runtime engineering baseline.

Required generated files:

- The 10 bootstrap/application profile files.
- Dockerfile.
- `.dockerignore`.
- Maven Wrapper files.
- Flyway migration directory and initial migration.
- Relevant validation components.
- Relevant async/config/encryption components.

Required generated POM checks:

- Bootstrap dependency exists where bootstrap files are generated.
- Actuator and Prometheus dependencies exist in runnable modules.
- Validation dependencies follow facade/adapter/application/domain/
  infrastructure rules.
- Service adapter does not contain `spring-boot-starter-web` or WebFlux starter.
- Any Web runtime dependency in service starter is documented and guarded as
  management-only.
- Layered Spring Boot Maven plugin configuration exists in runnable modules.
- Lombok is excluded from runtime boot jars.

Required service checks:

- No business Web controllers.
- No Web filters.
- No generated `web` package.
- No Web starter in the service adapter.
- Management port configuration exists.

Required web/light checks:

- Web starter dependency exists.
- Tomcat configuration exists.
- Actuator endpoint configuration exists.

The reliable archetype proof remains `clean integration-test`, not a plain
module `test`.

## GitHub Actions Verification

CI must prove generated projects are usable, not only that templates compile.

Required CI stages:

- Run the three archetype integration tests.
- Generate a temporary web project.
- Generate a temporary service project.
- Generate a temporary light project.
- For each generated project, run:

```bash
./mvnw -V --no-transfer-progress clean test
./mvnw -V --no-transfer-progress -DskipTests package
docker build -t egon-generated-${kind}:ci .
```

CI should use `SPRING_PROFILES_ACTIVE=test` or equivalent environment variables
that keep external registry/config center disabled. CI must not require a live
Nacos instance.

Docker verification is build-only. CI does not need to run containers for this
change.

## Implementation Task Shape

The implementation plan should split work into focused commits:

1. Configuration and dependency baseline for all three archetypes.
2. Generated-project configuration encryption support and tests.
3. Validation examples and validation boundary cleanup.
4. Virtual threads, async, graceful shutdown, Tomcat, and management port
   configuration.
5. Layered jar packaging, Dockerfile, and `.dockerignore`.
6. Archetype `verify.groovy` and generated-project integration-test hardening.
7. GitHub Actions generated-project test/package/docker-build verification.

Each task should keep changes scoped and commit once after its own validation.

## Acceptance Criteria

- Web, Service, and Light archetypes all pass `clean integration-test`.
- Each archetype can generate its target project.
- Each generated project contains the 10 required bootstrap/application config
  files.
- `local` and `test` defaults do not require external Nacos.
- `dev` and `prod` support Nacos and Dubbo registry through environment
  variables.
- `prod` templates contain no real secrets and no default local production
  registry/config center addresses.
- Configuration `ENC(...)` values decrypt with AES-256-GCM and fail fast on
  missing or invalid keys when encrypted values are present.
- Facade modules depend only on `jakarta.validation-api` for validation
  annotations.
- Adapter/application/domain/infrastructure layers can use validation according
  to their responsibilities.
- Service remains free of business Web dependencies and business Web packages.
- Service exposes only management HTTP for Actuator.
- Web and Light include Tomcat runtime tuning configuration.
- All three generated projects support virtual threads, async execution,
  graceful shutdown, Actuator, Flyway, HikariCP, Jackson defaults, and validation.
- Web/Service package exactly one runnable starter jar.
- Light packages exactly one runnable jar.
- All three generated projects include Dockerfile and `.dockerignore`.
- GitHub Actions verifies generated project test, package, and Docker build.

## Risks And Constraints

- Nacos dependencies can make local startup brittle if enabled by default. This
  design keeps `local` and `test` external dependencies off by default.
- Encryption must run early enough for property binding. The implementation must
  verify the chosen Spring Boot extension point with generated project tests.
- Service management HTTP must not accidentally introduce business Web support.
  POM and package guards must explicitly check this.
- Docker layered extraction depends on the Spring Boot version's supported jar
  tools mode. Generated Dockerfiles and CI must verify the exact command.
- The generated project verification cost will increase. CI should keep the
  matrix focused on the three archetypes and use Maven/Docker caches where
  practical.
