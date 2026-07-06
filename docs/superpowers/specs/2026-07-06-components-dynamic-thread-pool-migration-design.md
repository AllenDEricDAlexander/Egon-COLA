# Components Dynamic Thread Pool Migration Design

## Context

Egon-COLA `egon-cola-components` is being rebuilt from a historical collection of legacy components into a small, explicit component system. The provided architecture document defines the new target shape:

- old components are removed instead of compatibility-migrated;
- `egon-cola-component-common` is the only foundational pure Jar component;
- starter-type components use Spring Boot auto-configuration;
- admin modules are independently deployable backend services;
- UI does not live in this repository;
- admin modules expose a manifest contract for dynamic frontend discovery;
- the version line stays on 5.x.

The first starter-type component is dynamic thread pool. Its source implementation should be migrated from the existing `atluofu-dynamic-thread-pool` project by copying the working starter/admin/test code into the new component module and then renaming, pruning, and aligning it with Egon-COLA conventions. The goal is not to rewrite the dynamic thread pool from scratch.

## Goals

This work must deliver:

- a cleaned `egon-cola-components` module set;
- a new `egon-cola-component-common` pure Jar;
- a new `egon-cola-component-dynamic-thread-pool` component root;
- a copied-and-adapted `egon-cola-component-dynamic-thread-pool-starter`;
- a copied-and-adapted `egon-cola-component-dynamic-thread-pool-admin`;
- a single `egon-cola-component-dynamic-thread-pool-test` validation/sample module;
- a BOM that exports only business-consumable components;
- a manifest endpoint from the dynamic thread pool admin;
- updated documentation and archetype dependency guidance;
- build and test validation without starting long-running services.

## Non-Goals

This work will not include:

- compatibility modules for old components;
- deprecated bridge packages for old `com.alibaba.cola` components;
- rewriting the dynamic thread pool implementation from scratch;
- embedding or migrating the Vue UI into Egon-COLA;
- new dynamic-thread-pool capabilities beyond the existing migrated capability set;
- automatic dynamic thread pool starter injection into generated archetype projects;
- complete RBAC, gateway aggregation, or frontend implementation for manifest consumption;
- Flyway/database schema work unless a later requirement explicitly introduces persistent admin storage.

## Target Components Structure

`egon-cola-components` should become:

```text
egon-cola-components/
├── pom.xml
├── egon-cola-components-bom/
│   └── pom.xml
├── egon-cola-component-common/
│   ├── pom.xml
│   └── src/main/java/top/egon/cola/component/common/
├── egon-cola-component-dynamic-thread-pool/
│   ├── pom.xml
│   ├── README.md
│   ├── docs/
│   ├── egon-cola-component-dynamic-thread-pool-starter/
│   ├── egon-cola-component-dynamic-thread-pool-admin/
│   └── egon-cola-component-dynamic-thread-pool-test/
└── egon-cola-components-architecture.md
```

The root `egon-cola-components/pom.xml` should aggregate only:

- `egon-cola-components-bom`;
- `egon-cola-component-common`;
- `egon-cola-component-dynamic-thread-pool`.

## Common Component

`egon-cola-component-common` is a framework-free pure Jar. It may contain stable base models and utilities that are safe for every module to depend on:

- response/result models;
- pagination models;
- base error-code abstractions;
- base exceptions;
- assertion helpers;
- light validation/result helpers.

It must not depend on Spring Boot, Spring Web, Spring Context, Redis, database libraries, MQ, Nacos, Dubbo, dynamic thread pool, admin modules, test modules, or business modules.

Legacy DTO and exception ideas may be copied and renamed into the new package family, but old packages are not retained as public compatibility APIs.

## Dynamic Thread Pool Component

The dynamic thread pool component uses the approved Scheme A:

- `admin` may depend on `starter` to reuse shared models, Redis contracts, and executor governance types;
- `starter` must not depend on `admin`, `test`, or UI;
- `test` may depend on `starter` and, where needed for integration validation, `admin`;
- only `starter` is exported by BOM;
- admin remains independently buildable and deployable.

### Starter

The starter is copied from `atluofu-dynamic-thread-pool-spring-boot-starter` and adapted rather than rewritten.

Responsibilities:

- Spring Boot 3.5 `AutoConfiguration.imports`;
- configuration property binding;
- discovery of supported executor beans;
- `ManagedExecutor` abstraction;
- `ThreadPoolExecutor`, `ThreadPoolTaskExecutor`, and bounded virtual thread adapters;
- safe traditional thread pool resize;
- bounded virtual thread concurrency update;
- Redis registry interaction;
- snapshot reporting;
- change listener;
- audit event writing;
- MDC/trace context propagation;
- Micrometer meter binding when available.

Package root:

```text
top.egon.cola.component.dtp
```

Recommended package layout:

```text
autoconfigure
properties
core
executor
registry
monitor
listener
event
model
spi
support
```

The copied implementation can keep internal class names such as `ManagedExecutor`, `ExecutorSnapshot`, `BoundedVirtualThreadExecutor`, `RedisRegistry`, and `DtpRedisKeys` where they remain accurate. The package and configuration namespace must be aligned to Egon-COLA.

The configuration prefix should be:

```text
egon.cola.component.dtp
```

The old `atluofu.dynamic.thread-pool` prefix is not preserved as a compatibility alias.

### Admin

The admin is copied from `dynamic-thread-pool-admin` and adapted into:

```text
top.egon.cola.component.dtp.admin
```

Responsibilities:

- independent Spring Boot application entry;
- REST APIs for app, instance, executor, resize, virtual limit, and audit query;
- trace filter;
- Redis client wiring;
- response wrapper;
- dynamic thread pool manifest endpoint;
- Dockerfile for jar-based container packaging.

The existing API base path should remain:

```text
/api/v1/dtp
```

The admin must not include UI assets, Vite/Vue files, or frontend build steps.

### Manifest

Dynamic thread pool admin exposes a manifest endpoint under the admin API surface. The manifest should describe:

- component identifier: `dynamic-thread-pool`;
- display name;
- component version;
- enabled flag;
- base API path: `/api/v1/dtp`;
- frontend module key;
- frontend route base;
- menu definitions;
- permission definitions.

The first implementation only needs to expose a deterministic backend contract. Gateway aggregation and frontend dynamic route consumption are outside this implementation scope.

### Test Module

The test module consolidates the old sample/test intent from `atluofu-dynamic-thread-pool-test` and `atluofu-dynamic-thread-pool-test2` into one component-local validation module.

Responsibilities:

- sample Spring Boot application;
- traditional executor registration validation;
- virtual executor registration validation;
- starter auto-configuration validation;
- Redis key contract validation;
- admin controller validation;
- non-running integration-style tests that can execute in Maven.

The test module must not enter BOM and must not be used by business projects.

## BOM Contract

`egon-cola-components-bom` should export only:

- `top.egon:egon-cola-component-common`;
- `top.egon:egon-cola-component-dynamic-thread-pool-starter`.

It must not export:

- dynamic-thread-pool admin;
- dynamic-thread-pool test;
- component root POMs;
- docs;
- deleted legacy modules;
- UI artifacts.

## Archetype Integration

Generated projects should default to `egon-cola-component-common` only.

Dynamic thread pool starter is optional:

- service and web archetypes may document how to add it;
- no archetype should inject it by default;
- no generated domain or application module should couple to dynamic thread pool internals;
- runtime assembly belongs in the generated starter module when users choose to add the component.

## Design Patterns

The migration keeps the useful patterns already present in the dynamic thread pool project:

- Adapter: executor implementations adapt `ThreadPoolExecutor`, `ThreadPoolTaskExecutor`, and bounded virtual-thread executors to the shared `ManagedExecutor` contract.
- Decorator: `DtpRunnable`, `DtpCallable`, and related wrappers centralize MDC/trace propagation across async boundaries.
- Registry: local and Redis registries isolate executor lookup, snapshot storage, change messages, and audit events.

No new Strategy, Factory, or extra core module is introduced in this design. The copied implementation already has the required variation points, and additional abstraction would add migration risk without making the first component simpler.

## Migration Strategy

The implementation should copy first, then adapt:

1. remove legacy component modules from the components aggregator;
2. create `common`, `dynamic-thread-pool`, `starter`, `admin`, and `test` module shells;
3. copy starter Java/resource/test files from the old project into the new starter;
4. copy admin Java/resource/test files into the new admin;
5. copy only one sample/test application path into the new test module;
6. rename Maven coordinates, package names, configuration prefix, README examples, and resource paths;
7. remove UI, old start scripts, old docs that are not part of the component contract, stale absolute local paths, and duplicate sample modules;
8. add manifest endpoint and admin Dockerfile;
9. update BOM, root README, component architecture docs, and archetype references;
10. validate with targeted Maven commands and full components package.

## Validation

The implementation should not start application servers automatically.

Expected validation commands:

```bash
bash ./mvnw -B -ntp -f egon-cola-components/pom.xml -DskipTests validate
bash ./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-common -am test
bash ./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter -am test
bash ./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin -am test
bash ./mvnw -B -ntp -f egon-cola-components/pom.xml clean package
docker build -f egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/Dockerfile egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin
```

If Redis-backed tests require a live Redis instance, they should be isolated behind an explicit profile or environment flag so the default Maven test path remains deterministic.

## Acceptance Criteria

The migration is complete when:

- old component modules are no longer aggregated or exported;
- `common` builds as a pure Jar without Spring/runtime framework dependencies;
- dynamic thread pool starter auto-configuration works under Spring Boot 3.5;
- starter exposes the approved Egon-COLA configuration prefix;
- admin builds as an independent executable jar;
- admin Dockerfile can build an image from the jar;
- admin exposes REST APIs and manifest;
- UI files are absent from `egon-cola-components`;
- BOM exports only common and dynamic-thread-pool starter;
- archetype default dependency guidance is updated to common-only;
- targeted tests and full components package pass, or any environment-specific limitation is explicitly documented.
