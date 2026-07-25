# GWS-13 Gateway Test and Deployment Implementation Plan

状态：已执行

**Goal:** Turn the existing test-module skeleton into independently runnable
HTTP and Egon RPC applications, add deterministic process/Testcontainers
harnesses and deployment assets, and make fast versus live validation
boundaries explicit.

**Architecture:** Test applications are normal Spring Boot jars and use the
production Gateway Starter, Provider Runtime, RPC Starter, Admin, and Engine
artifacts. A Process Harness owns random ports, readiness probes, bounded
startup/shutdown, redacted diagnostics, and unique test scope. A
`gateway-live-test` Maven profile gates infrastructure-dependent Failsafe tests;
the default reactor remains fast and hermetic.

**Patterns considered:** Builder fits immutable process specifications;
Adapter isolates process and container controls; Scenario Object keeps E2E
orchestration readable. A single in-process mega-test is rejected because it
would hide port, lifecycle, registration, and configuration failures. Mock
Gateway is allowed only for unit seams and is forbidden in live scenarios.

---

## Task 1: Real HTTP provider

Create a Spring Boot MVC application with Gateway Starter and Provider Runtime,
controllers for public/internal, path/query/header/body, slow, failure, cancel,
and echo cases, plus deterministic instance metadata and readiness.

## Task 2: Real RPC contract, provider, and consumer

Define and generate unary Protobuf services. Create a real RPC provider using
the existing Egon RPC annotations/runtime and Gateway Starter reporting. Create
a real RPC consumer that discovers only `INTERNAL_GATEWAY`, exposes bounded
HTTP driver endpoints, and propagates trace/deadline/cancellation metadata.

## Task 3: Process and infrastructure harness

Add immutable process specifications, random-port allocation, readiness
polling without fixed sleeps, graceful stop followed by bounded force stop,
per-process logs, unique env/namespace/topic/database scope, and redacted
diagnostic manifests. Add Testcontainers definitions for PostgreSQL, two Redis
key spaces, and Kafka behind the live profile.

## Task 4: Scenario suites

Add fast topology/contract tests and live `*IT` scenario entry points for
Starter report visibility, DDC registration, HTTP and RPC forwarding, rule
publication/ACK/LKG, traffic/security behavior, trace/Kafka projection, failure
recovery, and the 0/1/2 `INTERNAL_GATEWAY` invariant. Live tests must skip with
a precise reason unless the profile and container runtime are available.

## Task 5: Build and CI wiring

Configure protobuf generation, Spring Boot executable jars, Surefire/Failsafe,
and the `gateway-live-test` profile. Add documented commands for fast reactor,
live process tests, and Admin Web lint/typecheck/unit/build/E2E.

## Task 6: Deployment assets

Add non-root Java 21 Engine/Admin container definitions, an Admin Web static
image that does not manage Nginx gateway nodes, Compose examples for local
dependencies, configuration examples without secrets, health/readiness
contracts, persistent Engine LKG mount, and ordered graceful shutdown docs.

## Task 7: Verification and acceptance audit

Run the full Gateway Maven reactor without live profile, frontend lint,
typecheck, unit tests, and build. Validate POM/profile wiring, executable jar
contents, Docker/config syntax where tooling exists, migration immutability,
forbidden Nacos/Dubbo/Nginx-management dependencies, and every GWS-01 through
GWS-13 acceptance item. Do not start product processes in this session.
