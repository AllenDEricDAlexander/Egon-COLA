# Archetype Runtime Profile, Shutdown, And Tomcat Design

## Goal

Align all generated Egon-COLA projects with one GitFlow-oriented runtime profile contract, complete Spring Boot graceful shutdown coverage, and expose safe Tomcat tuning controls where an embedded Tomcat server actually exists.

## Scope

The change covers the three generators under `egon-cola-archetypes`:

- `egon-cola-archetype-light`
- `egon-cola-archetype-service`
- `egon-cola-archetype-web`

It changes source templates and their generation-time `verify.groovy` contracts. Generated files under `target/` are validation artifacts and are not edited.

## Reference Findings

The graceful-shutdown reference recommends four cooperating layers:

1. `server.shutdown=graceful` for HTTP request draining.
2. `spring.lifecycle.timeout-per-shutdown-phase` for bounded lifecycle phases.
3. task-execution and task-scheduling shutdown waits for in-flight background work.
4. health probes and an external SIGTERM/grace-period contract for deployment platforms.

The archetypes already have items 1, 2, task-execution waiting, and health probes. They do not configure task-scheduling waiting.

The Tomcat reference groups the useful controls into:

1. worker capacity (`threads.max`, `threads.min-spare`);
2. connection admission (`max-connections`, `accept-count`);
3. connection lifetime (`connection-timeout`, `keep-alive-timeout`, `max-keep-alive-requests`);
4. request/resource bounds (`max-http-form-post-size`, `max-swallow-size`, `processor-cache`);
5. observability (`mbeanregistry.enabled` plus Actuator metrics).

Light and Web already have groups 1-3 and Actuator exposure. They lack group 4 and Tomcat MBean registration. Service is intentionally a non-Web RPC/MQ archetype, so Tomcat tuning does not belong there.

## Considered Approaches

### A. Keep `local` and make `dev` an alias

This preserves compatibility but leaves duplicate configuration files and contradicts the requirement to delete `local`.

### B. Replace `dev` with the current zero-dependency `local` configuration

This makes IDE startup easy, but it silently disables the real PostgreSQL, Redis, RabbitMQ, Nacos, HTTP, and Dubbo integration path already used by development Compose files and feature integration environments.

### C. Delete `local`, make `dev` the default, and retain the existing `dev` integration contract

This is the selected approach. It removes ambiguity without breaking the current development deployment surface. Tests continue to use the isolated `test` profile. The production surface remains `prod`.

## Runtime Profile Contract

| GitFlow context | Spring profile | Intended behavior |
| --- | --- | --- |
| developer workstation | `dev` | local development with environment-backed integrations |
| `feature/*` validation | `dev` | feature integration testing |
| `develop` integration pipeline | `test` | repeatable integration tests without required external infrastructure |
| `release/*` validation | `test` | release-candidate verification |
| `hotfix/*` validation | `test` | urgent-fix verification before merge |
| `main` runtime artifact | `prod` | production-only configuration |

`application-local.yml` and `bootstrap-local.yml` are removed from every archetype. Both `application.yml` and `bootstrap.yml` default to `dev`. Base Nacos namespace defaults also use `dev`.

Local fallback classes may keep their domain-meaningful `Local...` names, but their Spring profile becomes `test` only. Real Dubbo clients remain active in `dev` and `prod`.

## Graceful Shutdown Design

All three archetypes retain:

- graceful embedded-server shutdown;
- a 30-second, environment-overridable lifecycle phase timeout;
- task-execution termination waiting;
- liveness and readiness probes.

They add task-scheduling termination waiting with the same environment-overridable 30-second default. No placeholder `@PreDestroy` bean or custom `SmartLifecycle` abstraction is added because the templates do not own a custom resource whose shutdown order requires one.

## Tomcat Design

Light and Web add environment-overridable defaults:

- `TOMCAT_MAX_HTTP_FORM_POST_SIZE=2MB`
- `TOMCAT_MAX_SWALLOW_SIZE=2MB`
- `TOMCAT_PROCESSOR_CACHE=200`
- `TOMCAT_MBEAN_REGISTRY_ENABLED=true`

Existing thread, admission, timeout, and keep-alive defaults remain unchanged. The reference explicitly recommends measurement-driven tuning, so increasing capacities without workload evidence would be unsafe.

The Service archetype receives only Spring lifecycle/task shutdown improvements. It remains free of Web/Tomcat configuration.

## Generated-Project Contract

Each `verify.groovy` must prove:

- only `dev`, `test`, and `prod` profile files are generated;
- `local` profile files are absent;
- the default profile is `dev`;
- task execution and scheduling both await termination;
- local fallback adapters are test-only and real Dubbo clients remain `dev`/`prod`;
- Light and Web contain the full Tomcat bounds and MBean controls;
- Service contains no Tomcat block.

README pairs are updated together so English-default and Chinese documentation stay aligned.

## Design Pattern Decision

No design pattern is introduced. Strategy or Factory abstractions would add code without solving a runtime variation problem that Spring profiles and existing conditional configuration already express directly. The smallest safe design is configuration plus generated-contract assertions.

## Validation

Validation is complete only after:

1. each changed archetype passes its targeted `clean integration-test`;
2. the full `egon-cola-archetypes` reactor passes `clean integration-test`;
3. generated projects contain no `application-local.yml` or `bootstrap-local.yml`;
4. `git diff --check` succeeds;
5. the worktree contains only the intended commits and no uncommitted files.
