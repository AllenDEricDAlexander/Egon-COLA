# Evaluation-Organization Facade Integration Design

Date: 2026-07-10

Status: Deferred. This document records a future cross-Project design. It does not authorize implementation, implementation planning, or changes to either archetype in the current evaluation-alignment work.

## Goal

Define how the independent `student-management-evaluation` and `student-management-organization` Projects will eventually call each other through published Facade artifacts and Dubbo without violating their seven-module dependency boundaries.

The intended future relationship is bidirectional RPC:

```text
student-management-evaluation
    -> student-management-organization-facade
    -> Organization Dubbo provider

student-management-organization
    -> student-management-evaluation-facade
    -> Evaluation Dubbo provider
```

This document deliberately does not select Facade interface names, method signatures, DTO fields, error-code values, or initial artifact versions. The organization web archetype and its Facade contract are still being redesigned. Those details must come from separately approved provider contracts rather than being invented by the consumer.

## Deferred Status And Current Boundary

The current service-archetype alignment remains evaluation-only. It may improve the evaluation Project's internal architecture, business examples, Dubbo provider, RabbitMQ integration, runtime baseline, CI, and tests, but it must not implement the cross-Project relationship described here.

The current evaluation-alignment spec and its future implementation plan must exclude:

- Dependencies on `student-management-organization-facade`.
- Organization-facing Domain ports, Infrastructure clients, converters, stubs, or configuration.
- Organization contract parameters in archetype metadata.
- Cross-Project Dubbo calls or compatibility tests.
- Organization Facade fixtures in the service archetype integration test.
- Changes to the web archetype or its generated Facade contract.

This deferred design requires its own later approval before it can move to an implementation plan.

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

## Prerequisites And Approval Gates

Implementation planning may begin only after all of these gates are satisfied:

1. The organization web-archetype alignment is approved and its Facade output is stable enough to publish as a contract artifact.
2. The evaluation service-archetype alignment is approved and its Facade output is stable enough to publish as a contract artifact.
3. Each call direction has named business use cases, a clear provider owner, and an explicit reason that synchronous RPC is preferable to an existing local rule or asynchronous event.
4. The provider-owned Facade interfaces, DTOs, validation rules, error contract, and Java package are separately reviewed and approved.
5. Maven coordinates, supported exact contract versions, and compatibility windows are selected for both Facade artifacts.
6. Dubbo service identity, registry behavior, timeout budgets, and provider availability expectations are agreed for both directions.
7. The call graph is reviewed to prove that a request cannot synchronously call back into its origin Project.
8. Local/test stub semantics and dev/prod failure behavior are approved for every outbound operation.
9. Both archetypes can validate the integration against contract fixtures without starting either generated application or requiring external infrastructure.

Failure of any gate keeps this design deferred. It is not permission to fill the missing contract details with provisional guesses.

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

When this deferred design is implemented, the consuming archetype must accept explicit Maven coordinates instead of inferring them from the consumer's own coordinates.

Evaluation consumes Organization with:

```text
organizationFacadeGroupId
organizationFacadeArtifactId
organizationFacadeVersion
```

Organization consumes Evaluation with:

```text
evaluationFacadeGroupId
evaluationFacadeArtifactId
evaluationFacadeVersion
```

Parameter rules:

- All three values are required when the corresponding cross-Project client is generated.
- Versions are exact values. Maven ranges, `LATEST`, and `RELEASE` are not allowed.
- Snapshot coordinates may be used by isolated archetype tests and development builds; release and production builds pin released versions.
- The generated root POM may centralize these values as properties and dependency management, but only Infrastructure declares the actual external Facade dependency.
- No other internal module directly declares or source-imports the external
  Facade. Starter may receive and package it transitively through Infrastructure
  because Starter assembles the runnable application, but Starter must not use
  Facade types directly.
- Maven coordinates do not identify Java types. If the provider's generated Java base package remains configurable, the later design review must also approve an explicit contract-package parameter for each direction. Interface names and method signatures still come from the approved provider contract rather than from archetype parameters.

Runtime Dubbo group and service version are configuration concerns, not substitutes for Maven artifact versions. Their final property names and values are selected at re-entry after the provider contracts are stable.

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

The first supported versions and the exact compatibility window are re-entry decisions because neither Facade contract is stable today.

## Consumer Domain Port Boundary

Each consumer places one or more outbound interfaces under its Domain client boundary. The interfaces contain method declarations only. They express what the consumer's business logic needs, not a mirror of every provider Facade method.

Evaluation's organization-facing Domain boundary must obey these rules:

- Methods use Evaluation-owned Domain value objects, identifiers, and result concepts.
- The boundary contains no implementation class, Spring annotation, Dubbo annotation, Facade DTO, transport exception, timeout value, registry address, or configuration property.
- Application and Domain code depend only on this boundary and do not import Organization Facade types.
- The port is kept as narrow as the approved evaluation use cases require.

Organization follows the same rules for its evaluation-facing Domain boundary.

No concrete interface or method name is specified here. Freezing those names before the provider Facades stabilize would turn an interim web design into a permanent cross-Project contract.

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

This is an Anti-Corruption Layer implemented through Ports and Adapters. A Strategy or Factory hierarchy is not introduced: one real implementation and one deterministic local/test stub are sufficient until a genuine provider-selection variation appears.

## Provider Adapter Boundary

The provider's Adapter module implements its own Facade artifact and exposes that implementation through Dubbo. It converts Facade DTOs into provider Application Commands or Queries and converts Application Results back into Facade responses.

Provider Adapter must not:

- Accept the consumer's Domain or Application types.
- Call provider repositories or Infrastructure implementations directly.
- Expose provider Domain entities or persistence objects.
- Add consumer-specific behavior to a general provider operation.
- Synchronize back to the originating Project during the same request chain.

## Runtime Data Flow

Evaluation calling Organization follows this path:

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

Organization calling Evaluation uses the exact mirrored path.

Bidirectional RPC means either Project may originate a call for a separately approved use case. It does not permit synchronous ping-pong. A provider handling a cross-Project request must not call back into the originating Project as part of the same logical operation. Work that requires a callback must be redesigned as local data ownership, an asynchronous event, or a separate user-initiated operation.

## Runtime Profiles

### Local And Test

- Domain ports resolve to deterministic Infrastructure stubs.
- Real Dubbo reference beans, registry discovery, and outbound network calls are disabled.
- Tests do not require Organization, Evaluation, Nacos, a Dubbo registry, RabbitMQ, Redis, or PostgreSQL to run.
- Stub responses are defined per approved use case and cover success, not-found, business rejection, timeout, and unavailable outcomes.
- The external Facade artifact is still a compile-time dependency of the real Infrastructure client. "No external runtime dependency" does not mean that Maven may compile without resolving the contract artifact.

### Dev And Prod

- Domain ports resolve only to the real Infrastructure Dubbo clients.
- Required registry, service identity, timeout, and contract configuration is validated at startup.
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

Every real RPC operation has a finite, externally configurable timeout. Numeric defaults are intentionally not frozen in this deferred document; provider latency objectives and the caller's end-to-end budget must be approved at re-entry. An unbounded or framework-implicit timeout is not acceptable.

Automatic retries are disabled by default. Mutation calls are not retried automatically. A read-only call may gain a bounded retry only after its idempotence, retry budget, and duplicate-load impact are explicitly approved and tested.

Fallback rules are strict:

- Local/test uses deterministic stubs for isolation.
- Dev/prod never falls back to fabricated business data or a test stub.
- A production business fallback is allowed only when the owning use case defines correct degraded semantics and that behavior receives separate approval.
- Without an approved business fallback, timeout and unavailability fail explicitly through the mapped categories above.

## Contract And Compatibility Tests

After re-entry, validation covers both provider ownership and consumer adaptation:

- Each Facade module compiles and tests independently of internal modules.
- Provider Facade contract tests cover validation, serialization, error compatibility, and additive evolution rules.
- Provider Adapter tests invoke Facade implementations with mocked Application interfaces and verify protocol conversion.
- Consumer Infrastructure tests use mocked Facade references and verify Domain-to-Facade conversion, response mapping, timeout handling, and every failure category.
- Consumer Domain and Application tests mock only the Domain port and remain unaware of Facade and Dubbo types.
- Profile assembly tests prove local/test selects the stub and dev/prod selects the real client without duplicate beans.
- POM assertions prove only Infrastructure directly declares the external Facade,
  and source/ArchUnit assertions prove only Infrastructure imports provider or
  Dubbo types. Starter's unavoidable transitive runtime classpath is allowed but
  must not become a source dependency.
- Compatibility tests compile the consumer against every provider artifact version still declared supported.
- A cross-Project build test installs the approved provider Facade artifact before compiling the consumer; it does not start either application.

Both directions require the same coverage. Success in one direction is not evidence that the reverse dependency is compatible.

## Archetype Integration-Test Fixture Strategy

Each archetype must remain testable without generating, building, or running the other Project during its ordinary integration test.

The service-archetype IT uses an Organization Facade fixture. The web-archetype IT uses an Evaluation Facade fixture. Each fixture is a minimal, test-only Maven contract artifact that:

- Uses the exact parameterized coordinates supplied to that IT run.
- Contains only the approved Facade types needed by the generated client.
- Contains no provider implementation, Spring context, database, broker, registry, or application startup code.
- Is installed into an isolated Maven test repository before the generated consumer Project is compiled.
- Is derived from the approved provider contract after the corresponding re-entry gate passes, not from the unstable Facade drafts that exist today.

The generated consumer is then built with coordinate parameters pointing to the fixture. `verify.groovy` and generated tests assert the module dependency boundary, generated client/stub selection, contract conversion, and external-free local/test assembly.

The fixture is not the authority for the public API. Provider Facade source remains authoritative. A coordinated compatibility check must fail when the fixture and the supported provider contract diverge. Cross-project CI additionally installs the actual generated provider Facade artifact and compiles the consumer against it, without starting either application.

No Facade fixture is added as part of the current evaluation-alignment work.

## Out Of Scope

- Any implementation or implementation plan under the current evaluation-archetype alignment task.
- Freezing current Organization or Evaluation Facade interface names, methods, DTO fields, enum values, validation annotations, or error-code values.
- Modifying either current Facade contract while its owning archetype is being redesigned.
- Generating Organization from the service archetype or Evaluation from the web archetype.
- A shared parent reactor, shared database, shared Domain model, or shared Common module.
- Direct dependencies on another Project's Domain, Application, Infrastructure, Adapter, or Starter module.
- Cross-Project MQ events, Outbox, distributed transactions, two-phase commit, or workflow orchestration.
- Production circuit-breaker, cache, or fabricated-data fallback behavior without a separately approved business requirement.
- Starting generated applications, registries, brokers, databases, or containers as part of archetype validation.

## Risks And Mitigations

- **Unstable web Facade leaks into Evaluation.** Mitigation: no signatures or DTOs are recorded here; re-entry requires an approved, published Organization contract.
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
- **Maven coordinates do not reveal Java packages.** Mitigation: re-entry must approve the provider contract namespace and add an explicit package parameter if generated provider packages remain configurable.
- **Remote failures escape as vendor exceptions.** Mitigation: Infrastructure maps all transport and provider outcomes to stable consumer-owned categories.

## Acceptance Criteria For Re-Entry

This deferred design is ready for a separate implementation plan only when:

- Both provider Facade contracts have been separately approved and are available as buildable artifacts.
- The exact bidirectional use cases are approved without a synchronous callback cycle.
- Both coordinate parameter sets and any required contract-package parameters are approved.
- Artifact compatibility, Dubbo service identity, timeout budgets, and supported-version windows are explicit.
- Evaluation Domain ports contain methods only and Infrastructure owns all Organization Facade and Dubbo implementation details.
- Organization applies the same Domain-port and Infrastructure-client boundary for Evaluation.
- Local/test stub outcomes and dev/prod explicit-failure behavior are defined for each operation.
- Fixture sources can be derived from approved contracts and installed in isolated archetype IT repositories.
- The separate cross-Project integration spec is approved after the moving web-archetype work has settled.

## Final Completion Criteria

When a later, separately approved implementation is complete:

- Both independent Projects compile without depending on the other's internal modules.
- Evaluation calls Organization only through the Organization Facade artifact from Evaluation Infrastructure.
- Organization calls Evaluation only through the Evaluation Facade artifact from Organization Infrastructure.
- Domain and Application in both Projects contain no Facade or Dubbo types.
- Provider implementations remain in Adapter and consumer implementations remain in Infrastructure.
- Exact Maven coordinates and runtime Dubbo identities are configurable and version-pinned.
- Local/test runs without external services and dev/prod cannot select test stubs.
- Error, timeout, and fallback behavior matches this design.
- Provider, consumer, compatibility, profile, ArchUnit, archetype IT, and cross-project build tests pass without starting either application.
- The then-current implementation plan contains this work only after this deferred spec receives separate user approval.
