# Evaluation-Organization Facade Integration Design

Date: 2026-07-10

Status: Approved on 2026-07-11 for bidirectional infrastructure integration only. This document authorizes implementation planning and archetype changes within the scope defined below. Wiring either client into an Application use case still requires a separately approved business design.

## Goal

Define how the independent `student-management-evaluation` and `student-management-organization` Projects generate compile-ready, configurable, and testable clients for each other's published Facade artifacts without violating their seven-module dependency boundaries.

The intended future relationship is bidirectional RPC:

```text
student-management-evaluation
    -> student-management-organization-facade
    -> Organization Dubbo provider

student-management-organization
    -> student-management-evaluation-facade
    -> Evaluation Dubbo provider
```

Both provider contracts are now stable enough for the archetypes to consume their existing read operations. This integration does not change either provider-owned Facade interface, DTO, validation rule, error code, or provider implementation. Generated consumers pin explicit Maven coordinates and an explicit Java contract package supplied as archetype parameters.

## Approved Infrastructure Scope

Both archetypes generate the cross-Project boundary as infrastructure that is ready for later Application use but is not called by any current business flow.

The approved implementation includes:

- Explicit external Facade Maven-coordinate and Java-package archetype parameters.
- Consumer-owned, read-only Domain ports and consumer-owned projection records.
- Infrastructure Dubbo client adapters, contract converters, and deterministic local/test stubs.
- Profile-controlled real-client or stub selection and finite timeout configuration.
- Isolated archetype-test Facade fixtures and generated-project boundary tests.
- Documentation of publication order, runtime configuration, and the intentionally unused Application boundary.

The approved implementation excludes:

- Calls from existing Application Manage classes or changes to existing business behavior.
- New HTTP, GraphQL, MQ, Facade, command, or query use cases.
- Provider Facade contract changes or new cross-Project write operations.
- Starting either generated application or connecting to an external registry or provider during validation.

## Confirmed Architectural Decisions

- The service archetype continues to generate only one independent `student-management-evaluation` Project.
- The web archetype continues to own the independent `student-management-organization` Project.
- Cross-Project synchronous calls are bidirectional and use Dubbo plus the provider's standalone Facade artifact.
- Neither Project may depend on the other Project's Common, Domain, Application, Infrastructure, Adapter, or Starter module.
- Each consumer defines outbound operations as interface methods in its own Domain module.
- Domain contains no outbound implementation and no Dubbo, Spring, Facade DTO, Maven-coordinate, timeout, registry, or serialization detail.
- Infrastructure depends on the external Facade artifact and supplies the Dubbo client implementation of the Domain port.
- Adapter remains the provider-side location for Facade implementations and Dubbo exposure.
- Local and test profiles use deterministic stubs and make no external runtime connection.
- Dev and prod profiles use the real Dubbo clients and fail explicitly when the required provider is unavailable.
- This RPC design does not add cross-Project MQ contracts. RabbitMQ publication and consumption remain governed by the owning archetype's separate design.

## Approval Boundary

Infrastructure implementation planning may proceed because both generated Facade modules and provider-side Dubbo exposure now exist. The following decisions are approved for this implementation:

1. Both directions generate read-only client foundations only.
2. Each provider continues to own and publish its Facade artifact independently.
3. Maven coordinates and Java base packages are explicit required generation inputs.
4. Local/test selects deterministic stubs; dev/prod selects real Dubbo clients.
5. Real clients have finite configurable deadlines and no automatic retries.
6. Ordinary archetype integration tests use isolated contract fixtures and require no external infrastructure.

Application wiring remains gated. Before a port is injected into an existing or new Application use case, that call direction still requires a named business purpose, approved failure semantics, an end-to-end timeout budget, and a call-graph review proving that the provider cannot synchronously call back into the origin Project.

## Project And Module Boundaries

The two generated Projects remain independently built, versioned, deployed, and operated. Bidirectional capability does not create a Maven reactor containing both Projects.

The dependency directions are symmetrical:

| Consumer | Consumer Domain | Consumer Infrastructure | Provider Adapter |
| --- | --- | --- | --- |
| Evaluation | Declares organization-facing port methods in consumer-owned Domain terms | Depends on the Organization Facade artifact, converts models, and invokes Dubbo | Implements and exposes the Organization Facade |
| Organization | Declares evaluation-facing port methods in consumer-owned Domain terms | Depends on the Evaluation Facade artifact, converts models, and invokes Dubbo | Implements and exposes the Evaluation Facade |

The only cross-Project compile dependencies are:

```text
student-management-evaluation-infrastructure
    -> student-management-organization-facade

student-management-organization-infrastructure
    -> student-management-evaluation-facade
```

Because each Facade is self-contained and has no dependency on the provider's internal modules, this arrangement does not create a Maven dependency cycle. Both Projects still require the consumed Facade artifact to have been published or installed before the consumer is compiled.

## External Facade Parameters

The consuming archetype accepts explicit Maven coordinates instead of inferring them from the consumer's own coordinates.

Evaluation consumes Organization with:

```text
organizationFacadeGroupId
organizationFacadeArtifactId
organizationFacadeVersion
organizationFacadePackage
```

Organization consumes Evaluation with:

```text
evaluationFacadeGroupId
evaluationFacadeArtifactId
evaluationFacadeVersion
evaluationFacadePackage
```

Parameter rules:

- All four values are required because both cross-Project clients are always generated.
- Versions are exact values. Maven ranges, `LATEST`, and `RELEASE` are not allowed.
- Snapshot coordinates may be used by isolated archetype tests and development builds; release and production builds pin released versions.
- The generated root POM may centralize these values as properties and dependency management, but only Infrastructure declares the actual external Facade dependency.
- No other internal module directly declares or source-imports the external
  Facade. Starter may receive and package it transitively through Infrastructure
  because Starter assembles the runnable application, but Starter must not use
  Facade types directly.
- Maven coordinates do not identify Java types. The package parameter is the provider's generated base package without the `.facade` suffix. Generated imports append the provider-owned Facade paths to that value; interface names and method signatures are not archetype parameters.

Runtime Dubbo group and service version are configuration concerns, not substitutes for Maven artifact versions. The generated defaults match the current providers: Organization group `student-management-organization`; Evaluation groups `course`, `exam`, and `score`; service version `1.0.0` in both directions. Environment-backed properties may override these values without changing the pinned Maven contract version.

## Contract Ownership And Versioning

The provider owns its Facade artifact:

- Organization owns the Organization Facade interfaces, request/response DTOs, Facade enums, validation annotations, and Facade error contract.
- Evaluation owns the Evaluation Facade interfaces, request/response DTOs, Facade enums, validation annotations, and Facade error contract.
- A consumer may adapt a provider contract but must not copy, fork, or redefine provider DTOs under a local package.
- Facade remains self-contained and must not depend on Common or any other generated internal module.
- Provider implementation remains in Adapter and delegates to provider Application use cases.

Contract evolution follows these rules:

- Additive, backward-compatible changes may retain the current major contract line.
- Removing or renaming methods or fields, changing field meaning or type, tightening previously accepted validation, or changing established error semantics is breaking and requires a new major contract line.
- A provider must keep an older Dubbo contract version available for the agreed migration window before removing it.
- Consumers pin one approved artifact version and one approved Dubbo service identity; they do not float to an untested contract.
- Unknown provider error codes are mapped to a stable consumer-owned external-service failure rather than leaking raw provider details.
- Artifact publication, compatibility evidence, and consumer upgrade order are recorded together for each release.

The first generated versions are the exact parameter values supplied by the user. This archetype work does not declare a multi-version compatibility window; a provider release must add coordinated compatibility evidence before claiming support for more than the pinned version.

## Approved Read-Only Client Surface

Evaluation consumes these existing Organization operations:

- `UserFacade#getUser(String userId)` through `OrganizationDirectoryPort#getUser`.
- `SchoolClassFacade#getSchoolClass(String schoolClassId)` through `OrganizationDirectoryPort#getSchoolClass`.

Organization consumes these existing Evaluation operations:

- `CourseFacade#getCourse(GetCourseRequest request)` through `EvaluationQueryPort#getCourse`.
- `ExamFacade#getExam(GetExamRequest request)` through `EvaluationQueryPort#getExam`.
- `ScoreFacade#getScore(GetScoreRequest request)` through `EvaluationQueryPort#getScore`.

The Domain ports return consumer-owned projection records containing only the fields required to demonstrate stable conversion. They do not expose provider DTOs, response wrappers, error enums, or transport exceptions. No provider write operation is wrapped because no approved consumer business flow currently requires one.

## Consumer Domain Port Boundary

Each consumer places its approved outbound interface and projection records under its Domain client boundary. The interfaces contain method declarations only and cover only the approved read surface rather than mirroring every provider Facade method.

Evaluation generates `OrganizationDirectoryPort`; Organization generates `EvaluationQueryPort`. Both boundaries obey these rules:

- Methods use consumer-owned Domain value objects, identifiers, and result concepts.
- The boundary contains no implementation class, Spring annotation, Dubbo annotation, Facade DTO, transport exception, timeout value, registry address, or configuration property.
- Application and Domain code depend only on this boundary and do not import provider Facade types.
- The ports remain unused by Application until a separately approved business use case requires them.

The concrete port names are consumer-owned sample boundaries, not public contracts. Changing a provider Facade later affects only the corresponding Infrastructure adapter unless the consumer-owned projection semantics also change.

## Infrastructure Dubbo Client

Each consumer's Infrastructure module supplies the technology implementation of its Domain port.

The implementation owns:

- The external Facade artifact dependency.
- Dubbo reference configuration.
- Conversion between consumer Domain models and provider Facade DTOs.
- Provider response and error interpretation.
- Timeout and availability handling.
- Trace and correlation metadata propagation.
- Profile-controlled selection between the real client and the local/test stub.

The implementation does not import the consumer's Application module and does not return Facade DTOs to Domain. Provider-specific conversion stays beside the Infrastructure client so an external contract revision does not spread across the consumer.

This is an Anti-Corruption Layer implemented through Ports and Adapters. A Strategy or Factory hierarchy is not introduced: Spring conditional bean selection between one real implementation and one deterministic local/test stub is sufficient until a genuine provider-selection variation appears.

## Provider Adapter Boundary

The provider's Adapter module implements its own Facade artifact and exposes that implementation through Dubbo. It converts Facade DTOs into provider Application Commands or Queries and converts Application Results back into Facade responses.

Provider Adapter must not:

- Accept the consumer's Domain or Application types.
- Call provider repositories or Infrastructure implementations directly.
- Expose provider Domain entities or persistence objects.
- Add consumer-specific behavior to a general provider operation.
- Synchronize back to the originating Project during the same request chain.

## Runtime Data Flow

After a future business use case is approved, Evaluation calling Organization will follow this path:

```text
Evaluation inbound Adapter
    -> Evaluation Application use case
    -> Evaluation Domain port
    -> Evaluation Infrastructure Dubbo client
    -> Organization Facade contract
    -> Organization Adapter Facade implementation
    -> Organization Application use case
    -> Organization Domain
```

Organization calling Evaluation will use the exact mirrored path. In this implementation no current Application class injects either port, so validation stops at direct Infrastructure adapter and profile-assembly tests.

Bidirectional RPC means either Project may originate a call for a separately approved use case. It does not permit synchronous ping-pong. A provider handling a cross-Project request must not call back into the originating Project as part of the same logical operation. Work that requires a callback must be redesigned as local data ownership, an asynchronous event, or a separate user-initiated operation.

## Runtime Profiles

### Local And Test

- Domain ports resolve to deterministic Infrastructure stubs.
- Real Dubbo reference beans, registry discovery, and outbound network calls are disabled.
- Tests do not require Organization, Evaluation, Nacos, a Dubbo registry, RabbitMQ, Redis, or PostgreSQL to run.
- Stub responses are deterministic by identifier and cover success and not-found without contacting a provider. Timeout, unavailable, malformed-response, and unknown-provider failures are tested against mocked real Facade references rather than fabricated by the normal stub.
- The external Facade artifact is still a compile-time dependency of the real Infrastructure client. "No external runtime dependency" does not mean that Maven may compile without resolving the contract artifact.

### Dev And Prod

- Domain ports resolve only to the real Infrastructure Dubbo clients.
- Required registry, service identity, timeout, and contract configuration is validated when real-client integration is enabled.
- A missing provider does not silently activate the local/test stub.
- Environment-backed values supply endpoints and credentials; generated source does not embed them.
- Contract artifact and runtime service versions must match an approved compatibility entry.

## Error, Timeout, And Fallback Mapping

Infrastructure translates provider and transport behavior into stable consumer-owned failure categories:

| External outcome | Consumer-facing category |
| --- | --- |
| Provider reports missing business data | Dependency data not found, when the use case distinguishes absence |
| Provider rejects a valid request by business rule | External business rejection with a mapped stable reason |
| Provider reports malformed or unsupported input | Cross-contract validation failure |
| Dubbo cannot discover or reach a provider | External dependency unavailable |
| A finite Dubbo deadline expires | External dependency timeout |
| Response cannot be decoded or violates the approved contract | External contract incompatibility |
| Unknown provider or transport failure | External service failure |

Dubbo and provider exception types never cross the Infrastructure boundary. Logs may retain provider diagnostics and trace IDs, but consumer-facing errors must not expose remote stack traces or credentials.

Every real RPC operation has a finite, externally configurable timeout. The generated infrastructure default is `3000` milliseconds, matching the current Dubbo consumer baseline. A later Application use case must confirm that this fits its end-to-end budget before it starts calling the port. An unbounded or framework-implicit timeout is not acceptable.

Automatic retries are disabled by default. Mutation calls are not retried automatically. A read-only call may gain a bounded retry only after its idempotence, retry budget, and duplicate-load impact are explicitly approved and tested.

Fallback rules are strict:

- Local/test uses deterministic stubs for isolation.
- Dev/prod never falls back to fabricated business data or a test stub.
- A production business fallback is allowed only when the owning use case defines correct degraded semantics and that behavior receives separate approval.
- Without an approved business fallback, timeout and unavailability fail explicitly through the mapped categories above.

## Contract And Compatibility Tests

Validation covers both provider ownership and consumer adaptation:

- Each Facade module compiles and tests independently of internal modules.
- Provider Facade contract tests cover validation, serialization, error compatibility, and additive evolution rules.
- Provider Adapter tests invoke Facade implementations with mocked Application interfaces and verify protocol conversion.
- Consumer Infrastructure tests use mocked Facade references and verify Domain-to-Facade conversion, response mapping, timeout handling, and every failure category.
- Domain tests prove the ports and projections contain no Facade or Dubbo types. No Application test is added because the ports are intentionally not wired into Application.
- Profile assembly tests prove local/test selects the stub and dev/prod selects the real client without duplicate beans.
- POM assertions prove only Infrastructure directly declares the external Facade,
  and source/ArchUnit assertions prove only Infrastructure imports provider or
  Dubbo types. Starter's unavoidable transitive runtime classpath is allowed but
  must not become a source dependency.
- Compatibility tests compile the consumer against every provider artifact version still declared supported.
- A cross-Project build test installs the approved provider Facade artifact before compiling the consumer; it does not start either application.

Both directions require the same coverage. Success in one direction is not evidence that the reverse dependency is compatible.

## Archetype Integration-Test Fixture Strategy

Each archetype remains testable without generating, building, or running the other Project during its ordinary integration test.

The service-archetype IT uses an Organization Facade fixture. The web-archetype IT uses an Evaluation Facade fixture. Each fixture is a minimal, test-only Maven contract artifact that:

- Uses the exact parameterized coordinates supplied to that IT run.
- Contains only the approved Facade types needed by the generated client.
- Contains no provider implementation, Spring context, database, broker, registry, or application startup code.
- Is rebuilt on every run and installed under a dedicated `fixture.*` coordinate in the build's active Maven local repository before the generated consumer Project is compiled, avoiding collisions with provider-owned release coordinates.
- Matches the approved read-only subset of the current provider contract.

The generated consumer is then built with coordinate parameters pointing to the fixture. `verify.groovy` and generated tests assert the module dependency boundary, generated client/stub selection, contract conversion, and external-free local/test assembly.

The fixture is not the authority for the public API. Provider Facade source remains authoritative. A coordinated compatibility check must fail when the fixture and the supported provider contract diverge. Cross-project CI additionally installs the actual generated provider Facade artifact and compiles the consumer against it, without starting either application.

## Out Of Scope

- Wiring either generated port into a current Application use case.
- Changing Organization or Evaluation Facade interface names, methods, DTO fields, enum values, validation annotations, or error-code values.
- Generating Organization from the service archetype or Evaluation from the web archetype.
- A shared parent reactor, shared database, shared Domain model, or shared Common module.
- Direct dependencies on another Project's Domain, Application, Infrastructure, Adapter, or Starter module.
- Cross-Project MQ events, Outbox, distributed transactions, two-phase commit, or workflow orchestration.
- Production circuit-breaker, cache, or fabricated-data fallback behavior without a separately approved business requirement.
- Starting generated applications, registries, brokers, databases, or containers as part of archetype validation.

## Risks And Mitigations

- **Provider Facade revisions leak into the consumer.** Mitigation: provider DTOs remain inside Infrastructure and consumer-owned Domain projections isolate the rest of the project.
- **Bidirectional RPC creates request cycles.** Mitigation: review the call graph per use case and prohibit synchronous callbacks to the origin Project.
- **Independent Projects create release-order coupling.** Mitigation: publish Facade artifacts first, pin exact versions, retain compatible provider versions during migration, and compile consumers against all supported versions.
- **Facade DTOs leak into Domain.** Mitigation: Domain-owned ports and Infrastructure conversion form an explicit Anti-Corruption Layer; ArchUnit enforces it.
- **External Facade dependencies become available transitively at runtime.**
  Mitigation: only Infrastructure directly declares and source-imports the
  dependency; Starter may package it transitively but verifier and ArchUnit rules
  prevent Starter and other layers from importing its types.
- **Local/test accidentally connects to a registry.** Mitigation: property-gate real Dubbo references, select deterministic stubs, and run external-free assembly tests.
- **Dev/prod silently uses test data.** Mitigation: stubs are unavailable outside local/test and startup validates real-client configuration.
- **Fixture contracts drift from providers.** Mitigation: provider source is authoritative, coordinated compatibility checks detect drift, and cross-project CI compiles against the actual generated artifact.
- **Maven coordinates do not reveal Java packages.** Mitigation: both archetypes require an explicit provider base-package parameter and verify the generated imports against a fixture contract.
- **Remote failures escape as vendor exceptions.** Mitigation: Infrastructure maps all transport and provider outcomes to stable consumer-owned categories.

## Final Completion Criteria

The infrastructure integration is complete when:

- Both independent Projects compile without depending on the other's internal modules.
- Evaluation's generated real adapter accesses Organization only through the Organization Facade artifact from Evaluation Infrastructure.
- Organization's generated real adapter accesses Evaluation only through the Evaluation Facade artifact from Organization Infrastructure.
- Domain and Application in both Projects contain no Facade or Dubbo types.
- Provider implementations remain in Adapter and consumer implementations remain in Infrastructure.
- Exact Maven coordinates and runtime Dubbo identities are configurable and version-pinned.
- Local/test runs without external services and dev/prod cannot select test stubs.
- Error, timeout, and fallback behavior matches this design.
- Provider, consumer, profile, ArchUnit, archetype IT, and fixture-backed generated-project tests pass without starting either application.
- Existing Application classes do not inject or call either new Domain port.
