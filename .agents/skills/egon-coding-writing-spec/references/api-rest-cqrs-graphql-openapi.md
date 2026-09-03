# REST, CQRS, GraphQL, and OpenAPI 3 API Design

Read this reference completely whenever an external HTTP API is `Affected`. It specializes `references/interface-contract-design.md`; both references apply. Its purpose is to make Chapter 9 an implementation-ready API contract, not a list of routes or annotations.

## 1. Authority and adaptation policy

Use sources in this order:

1. the explicit user requirement and accepted effective Specs;
2. the current repository's controllers, Spring configuration, security, DTOs, error/result types, Jackson rules, GraphQL SDL, clients, tests, and dependency management;
3. official protocol and framework documentation listed in §18;
4. external example repositories only as non-normative design input.

The referenced [Mshuyan/swagger](https://github.com/Mshuyan/swagger) repository is useful for these ideas:

- surface Bean Validation constraints in API documentation;
- document success and error wrappers, security schemes, and authorization requirements;
- keep reusable API contracts reviewable and support documentation aggregation where a real gateway requires it;
- keep documentation exposure configurable.

It is not the implementation baseline for new designs. The repository contains Springfox, `Docket`, Swagger 2 annotations, old Spring Boot examples, mixed annotation generations, and manual aggregation patterns. For new or modernized Spring Boot APIs:

- use the Spring Boot-compatible `springdoc-openapi-starter-*` artifact already managed by the repository or its BOM;
- use `io.swagger.v3.oas.annotations.*` only;
- prohibit new `io.swagger.annotations.*`, Springfox `Docket`, `@EnableSwagger2`, `@EnableOpenApi`, `ApiImplicitParam`, and fake endpoints created only to make Swagger UI show documentation;
- do not hand-build gateway aggregation when an existing gateway/component/catalog already publishes service documents;
- do not introduce an annotation-only Java interface merely to separate documentation from a Controller. Reuse an existing Facade/API contract interface only when it is already an architectural boundary and repository tests prove that Spring mappings, validation, and OpenAPI annotations are inherited as intended.

If an affected legacy module still uses Springfox, record `Legacy/Compatibility` rather than silently mixing generations. A migration to springdoc is a separate affected change unless the user approved it.

## 2. Protocol selection before endpoint design

Classify every affected external operation as exactly one of:

| API style | CQRS role | Use when | Do not use merely because |
| --- | --- | --- | --- |
| REST resource query | Query | A stable resource/collection representation fits HTTP resource semantics | GET looks familiar |
| REST resource command | Command | Create, replace, patch, delete, or a resource lifecycle action has clear HTTP semantics | every Service method needs a URL |
| REST task command | Command | A business action cannot be represented honestly as ordinary resource state replacement | verbs are convenient |
| GraphQL query | Query | A client needs a selectable graph/projection across related data and the project already supports or explicitly adopts GraphQL | the response has nested JSON |
| GraphQL mutation | Command | A task-based write belongs to an approved GraphQL schema | one `/graphql` URL seems simpler |
| GraphQL subscription | Subscription/read stream | The requirement proves a live stream and transport/lifecycle support | polling feels inelegant |

The Spec must state why the chosen style fits the consumer goal and why the direct existing alternative is insufficient. REST and GraphQL may coexist only with explicit ownership: name which consumer/use case uses each, which business capability is authoritative, how validation/security/errors remain consistent, and whether one protocol delegates to the same application Service. Do not create duplicate business logic or independent write semantics per protocol.

OpenAPI describes REST HTTP operations. GraphQL SDL and operation documents describe GraphQL fields. Do not pretend that Swagger annotations on GraphQL resolver methods replace SDL, and do not expand the generic `POST /graphql` transport operation into fake REST resources.

## 3. CQRS semantic gate without ceremonial architecture

Every API operation must declare a CQRS role even in a traditional three-layer project:

- **Query** retrieves data and must not create durable business state, publish business events, or perform hidden commands. Incidental observability such as metrics is not a business mutation.
- **Command** expresses a business task or resource state change. Prefer task language such as `approveInvoice` over field-setting language such as `setStatusApproved` when the business transition has guards or side effects.
- **Subscription** delivers an approved stream and must document source, authorization, ordering, resume/reconnect, backpressure, and termination.

CQRS classification does not by itself justify separate databases, read replicas, event sourcing, buses, handler hierarchies, or new modules. Use the smallest level that satisfies current evidence:

| Level | Design | Adoption requirement |
| --- | --- | --- |
| L0 | Operation is explicitly classified Query or Command; existing Service/data model remains shared | Default for simple CRUD and focused changes |
| L1 | Separate `Query`/`Command` boundary objects and application methods, shared database | Distinct validation, permission, projection, or transactional semantics |
| L2 | Separate read projection/model, shared or replicated persistence | Measured query shape/performance or security need |
| L3 | Separate stores, messaging, eventual consistency, possibly event sourcing | Explicit scale/availability/history requirement plus synchronization, lag, recovery, and operations design |

Any choice above L0 must record evidence, consistency model, failure modes, additional state/calls, migrations, observability, tests, and why the lower level fails. Never add a Query endpoint whose only purpose is to fetch parameters copied into a Command; the Command must derive and authoritatively validate server-owned context.

## 4. REST resource and URI rules

For each REST operation:

- model the URI as a stable resource identifier, normally lowercase plural nouns: `/api/v1/orders/{orderId}`;
- use nesting only when the child is meaningfully scoped by the parent and the relationship is stable; avoid deep paths;
- keep tenant, actor, permission, server time, derived state, and server configuration out of caller-controlled paths/bodies when infrastructure owns them;
- use query parameters for filtering, searching, sorting, pagination, field projection, and optional representation controls, not for hidden writes;
- avoid RPC verbs for ordinary CRUD. A task command may use a subordinate action/resource such as `POST /orders/{orderId}/cancellations` when cancellation has independent business semantics, audit, idempotency, or lifecycle;
- never expose package/class names as public resource vocabulary;
- verify the full application route from class and method mappings, context path, gateway prefix, and API version; do not invent environment hosts.

### 4.1 HTTP method semantics

| Method | Required semantics | Typical success | Design prohibitions |
| --- | --- | --- | --- |
| `GET` | Safe, read-only, cache semantics stated | `200`; `304` with validators; `404` for absent singleton | No request body, hidden command, or server state transition |
| `POST` | Create subordinate resource or execute non-idempotent/task command | `201` + `Location` for creation; `200`; `202` for truly async work | Do not claim idempotency without a key/deduplication contract |
| `PUT` | Complete replacement at a known URI; idempotent | `200` or `204`; `201` only when create-by-URI is supported | Do not describe partial update as PUT |
| `PATCH` | Explicit partial-update media type and field semantics | `200` or `204` | No ambiguous absent/null/reset behavior |
| `DELETE` | Remove/deactivate according to documented resource semantics; idempotent requested effect | `204` or repository convention | Do not return success while an undocumented async deletion is pending |
| `HEAD` | Same metadata as GET without content | `200`/`304` | No distinct business logic |

HTTP method, status, headers, caching, and conditional-request behavior must agree. Avoid bodies on `GET`, `HEAD`, and normally `DELETE`, because interoperable semantics are not well-defined.

### 4.2 Status and header contract

Document every applicable outcome, not a generic `default` response:

- `200 OK`: successful representation or synchronous command result;
- `201 Created`: created resource; document `Location` and returned representation;
- `202 Accepted`: work is not complete; define status resource/field, polling or callback, expiry, terminal failures, and retry behavior;
- `204 No Content`: no response body; generated OpenAPI content must also be empty;
- `400 Bad Request`: malformed syntax/type or repository-standard validation mapping;
- `401 Unauthorized`: authentication missing/invalid and relevant `WWW-Authenticate` behavior;
- `403 Forbidden`: authenticated but not authorized; do not leak cross-tenant existence;
- `404 Not Found`: resource absence under the repository's disclosure policy;
- `409 Conflict`: current state, uniqueness, idempotency-key reuse, or business transition conflict;
- `412 Precondition Failed`: failed `If-Match`/precondition when optimistic HTTP concurrency is used;
- `415 Unsupported Media Type`, `422 Unprocessable Content`, `429 Too Many Requests`, and `5xx` dependency/internal outcomes when actually applicable.

Define `Location`, `ETag`, `If-Match`, `Idempotency-Key`, correlation/trace IDs, pagination links/cursors, `Retry-After`, cache headers, and deprecation headers only when supported by implementation and consumers. Header name, ownership, format, requiredness, propagation, logging/masking, and retry effect belong in the request/response tables.

### 4.3 Query, pagination, and compatibility

List/search operations must define:

- filter operators, combinations, blank/null/unknown handling, permission and tenant scope;
- allowed sort fields, direction, default, and deterministic tie-breaker;
- offset page base/default/max/count semantics or cursor encoding/expiry/stable-order semantics;
- empty results as a non-null empty collection unless the established contract differs;
- concurrency effects between pages and whether snapshot consistency is promised;
- maximum page size, projected fields, query/index evidence, and cost controls;
- additive/removal/rename compatibility, unknown-field behavior, version/deprecation policy, and named consumers.

Do not add a new API version automatically. Use the repository's existing compatibility strategy and treat breaking public changes as a major user decision.

## 5. Request, validation, and serialization contract

For every REST request or GraphQL input, trace each value to Path, Query, Header, Cookie, Body, Multipart, GraphQL variable, authenticated principal, tenant context, configuration, server clock, or derived data.

The Spec must define:

- JSON/property name and Java semantic type (`Command`, `Query`, `Request`, DTO, VO, etc.);
- wire type/format, requiredness, missing/null/blank/empty behavior, default, min/max, length, precision/scale, pattern, enum values, collection bounds/uniqueness, trimming/case, timezone, and unknown-field behavior;
- nested `@Valid`, method/class `@Validated`, and Validation Group selection for reused inputs;
- normalization timing and authoritative revalidation, using existing `ValidatorUtils`, Jakarta Validation, and libphonenumber where applicable;
- Jackson field names, inclusion, formats, enum/date behavior, sensitive-field exposure, and compatibility;
- exact validation error mapping with field paths safe for the consumer.

OpenAPI annotations describe the contract; they do not replace runtime Bean Validation. Generated constraints must be checked against the validator annotations and custom cross-field rules. Never document a constraint that code will not enforce, and never rely on generated documentation to secure or validate an operation.

## 6. Response and error contract

Use the actual repository response/error infrastructure. Do not create a second wrapper only for Swagger.

- If the repository has a stable Egon/common `Result` or error envelope, document its exact HTTP mapping, fields, nullability, codes, and Jackson shape.
- For a new external REST API without an established application envelope, prefer Spring's RFC 9457 `ProblemDetail`/`ErrorResponse` support rather than inventing another error format.
- If adopting RFC 9457 would break existing consumers, preserve the current envelope and record the compatibility decision; do not mix formats per Controller without a defined negotiation/version policy.
- Never expose stack traces, exception class names, SQL, secrets, tokens, tenant existence, or internal dependency details.

Every response requires a complete `jsonc` example with a line-end meaning comment on every key, plus a field table where nullability/source/precision needs more detail. Every error row requires condition, HTTP status, stable code or GraphQL `extensions.code`, shape, retryability, frontend handling, logging/trace behavior, and test.

## 7. OpenAPI 3 document contract

For every affected REST operation, Chapter 9 must design the generated OpenAPI operation as carefully as the runtime route.

### 7.1 Required operation properties

| OpenAPI element | Rule |
| --- | --- |
| `tags` | Stable resource/capability grouping; do not use package names |
| `summary` | Short consumer-facing action; one sentence |
| `description` | Business behavior, material guards/side effects, idempotency/consistency, and consumer guidance; do not duplicate implementation code |
| `operationId` | Explicit, globally unique, stable lowerCamelCase identifier; never depend on generated method-name collisions |
| `parameters` | Exact name + `in`, schema/format, required/default/example, description; every path parameter is required and matches the URI template |
| `requestBody` | Requiredness, media type, schema, examples; absent for bodyless operations |
| `responses` | Every material success and error status with description, media type, headers, and exact schema; no unexplained `default` |
| `security` | Exact registered scheme names and OAuth scopes/permissions; public operations explicitly override global security |
| `deprecated` | True only with replacement and removal policy documented |

Schemas must define stable names, descriptions, examples, required/null semantics, formats, enum values, ranges, lengths, pattern where meaningful, read-only/write-only access, and discriminators/composition only when the wire contract actually needs them. Avoid exposing persistence objects or internal polymorphism.

OpenAPI 3.1 versus 3.0 must follow the installed springdoc/swagger-core compatibility and current consumers. Do not request 3.1 features while the generator or downstream tooling emits/accepts only 3.0. Do not hardcode a library version in the Spec without checking Spring Boot, dependency management, and repository compatibility.

### 7.2 Source of truth and generated artifact

State one source-of-truth model:

- **Code-first**: Spring mappings, Java boundary types, Bean Validation, Jackson, and OpenAPI annotations are authoritative; `/v3/api-docs` is generated and diff-checked.
- **Contract-first**: an approved checked-in OpenAPI document is authoritative; generated server/client integration and implementation conformance are validated. Do not independently maintain contradictory annotations.

If the repository is code-first, do not check in a copied JSON/YAML artifact unless current build/release policy requires it. If it is checked in, define regeneration ownership and a drift gate.

## 8. springdoc dependency and configuration rules

Inspect the current Spring Boot generation, Web MVC versus WebFlux stack, dependency management, security, actuator/gateway topology, and existing documentation setup before proposing any dependency.

Use the matching starter family:

- MVC with UI: `org.springdoc:springdoc-openapi-starter-webmvc-ui`;
- MVC API only: `org.springdoc:springdoc-openapi-starter-webmvc-api`;
- WebFlux with UI/API: the corresponding `springdoc-openapi-starter-webflux-*` artifact.

Prefer the repository/BOM-managed version. Never add both Springfox and springdoc to a new module. Never add Swagger Core annotations separately when the selected starter already supplies the compatible annotation dependency unless repository dependency policy requires an explicit managed declaration.

Design configuration for:

- API document/UI enablement by environment;
- exact `/v3/api-docs`, YAML, Swagger UI, management-port, and grouped-document paths when changed from defaults;
- `GroupedOpenApi` only for real public/internal/module audiences with non-overlapping inclusion rules;
- packages/paths to scan, hidden internal endpoints, and management endpoints;
- server URL handling behind proxies/gateways without hardcoded environment hosts;
- Spring Security access to docs/UI and OAuth redirect paths;
- production exposure, authentication, network restriction, or complete disablement;
- identical key sets across all Spring Boot environment configuration files, with values allowed to differ.

Documentation aggregation is an architectural feature. For a gateway/catalog, define discovery source, service identity, document URL, auth propagation, timeouts, partial service failure, stale cache, version compatibility, collision handling for component names/operation IDs, and production access. Do not copy a static resource-provider example without proving it fits the current gateway.

## 9. OpenAPI 3 annotation standard

Only use `io.swagger.v3.oas.annotations.*`. Import collisions must be explicit, especially between Spring's `org.springframework.web.bind.annotation.RequestBody` and OpenAPI's `io.swagger.v3.oas.annotations.parameters.RequestBody`.

### 9.1 Annotation placement matrix

| Target | Annotation | Mandatory design content |
| --- | --- | --- |
| API metadata configuration | `@OpenAPIDefinition(info = @Info(...), tags = ...)` | title, version, description, contact/license only when repository-owned; no secrets or environment host |
| Security configuration | `@SecurityScheme` | exact scheme name/type, bearer format or OAuth/OpenID flows and scopes from real security config |
| Controller or existing API contract type | `@Tag` | stable capability name and consumer-facing description |
| REST operation | `@Operation` | `summary`, `description`, explicit unique `operationId`, tags/security/deprecated as applicable |
| Path/query/header/cookie parameter | `@Parameter` | description, required, example, schema constraints; prefer inference from Spring annotations when exact and add annotation for missing semantics |
| Existing query-parameter aggregate | springdoc `@ParameterObject` | expand a real flat query/form object into parameters; do not use for JSON request bodies or create a carrier only for documentation |
| Request body | OpenAPI `@RequestBody` | description, required, `@Content`, schema/example only where inference is incomplete |
| Each outcome | `@ApiResponse` / `@ApiResponses` | exact response code, description, headers, `@Content`, schema/example |
| Media/schema | `@Content`, `@Schema`, `@ArraySchema` | media type and actual wrapper/payload schema; use `@ArraySchema` for arrays rather than conflicting array/schema declarations |
| Example | `@ExampleObject` | valid protocol JSON, named purpose, no secrets/real personal data; keep examples consistent with validation and schema |
| Response header | `io.swagger.v3.oas.annotations.headers.Header` | exact name, description, requiredness/schema/example for `Location`, `ETag`, rate/retry, correlation, or deprecation headers |
| Boundary model/field | `@Schema` | name/description/example/format/access/allowable values only as required to express the wire contract |
| Secured operation | `@SecurityRequirement` | exact registered scheme name and scopes; never a descriptive permission string masquerading as a scheme |
| OAuth/OpenID scheme | `@OAuthFlows`, `@OAuthFlow`, `@OAuthScope` inside `@SecurityScheme` | exact real endpoints, flow, scope name/meaning, and client audience; never credentials |
| Operation/server relationship | `@Link`, `@Callback`, `@Server` | only for a real follow-up/callback/server contract; avoid environment-specific hosts and decorative links |
| Existing functional Web route | springdoc `@RouterOperation` / `@RouterOperations` | exact path, method, operation bean method, consumes/produces, parameters, responses, and security; use only when the repository actually uses RouterFunction |
| Hidden internal item | `@Hidden` | only when the endpoint must exist but must not be in this published audience |

Avoid annotation noise. Spring MVC mappings, Java types, Bean Validation, and Jackson already provide structural facts; annotations should add or correct consumer semantics, not repeat every obvious type. Conversely, explicit `operationId`, business descriptions, material responses/errors, security, wrapper schemas, examples, and non-obvious serialization cannot be omitted merely because springdoc can infer something.

Apply these placement rules:

- put `@Operation` and operation-specific `@ApiResponse`/`@SecurityRequirement` on the mapped operation, not on an unrelated helper;
- use `@Parameter` for non-body parameters and OpenAPI `@RequestBody` for the body; never model a body as an implicit parameter;
- use springdoc `@ParameterObject` only for an existing parameter aggregate whose fields really arrive as query/form parameters; confirm nested-object and naming behavior in the generated document;
- keep Spring mapping/validation annotations as runtime authority and OpenAPI annotations as documentation enrichment; explicitly qualify colliding `RequestBody` imports;
- represent `204` with no `@Content`; represent arrays with `@ArraySchema`; represent concrete generic wrappers so the generated component contains the real payload;
- express field requiredness/nullability through the installed annotation version plus Bean Validation and Jackson. Prefer the current `requiredMode` API when available; do not copy deprecated attributes from an older example;
- put reusable metadata, security schemes, shared problem responses, headers, or schemas in central components only when values and behavior are genuinely identical;
- keep `summary` short, place detailed Markdown behavior in `description`, and keep `operationId` stable across Java refactors;
- use `@Hidden` to exclude an actual internal operation from one published audience, not as a replacement for security or package-scan ownership.
- for functional endpoints, keep the `RouterFunction`, handler method, and `@RouterOperation` identity aligned and generated-contract tested; do not add a parallel annotated Controller.

### 9.2 REST Controller example

This is a shape example, not repository evidence. Replace every name, wrapper, status, error, permission, and path with verified project values.

```java
@Tag(name = "Orders", description = "Order query and command operations")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final @Qualifier("orderService") OrderService orderService;

    @Operation(
            summary = "Create an order",
            description = "Validates authoritative references and creates one order idempotently.",
            operationId = "createOrder",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Order created",
                    content = @Content(schema = @Schema(implementation = CreateOrderResultResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Request validation failed",
                    content = @Content(schema = @Schema(implementation = ValidationProblemResponse.class))),
            @ApiResponse(
                    responseCode = "409",
                    description = "Idempotency or state conflict",
                    content = @Content(schema = @Schema(implementation = ConflictProblemResponse.class)))
    })
    @PostMapping
    public ResponseEntity<CreateOrderResultResponse> createOrder(
            @Parameter(description = "Stable key for retries of the same command", required = true)
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @org.springframework.web.bind.annotation.RequestBody CreateOrderCommand command) {
        // Shape only; production logic belongs in the existing application/service boundary.
        throw new UnsupportedOperationException("example only");
    }
}
```

The real Spec must explain whether an existing API/Facade interface owns the annotations. Do not create `OrderApi` only to move annotation lines away from the Controller, and do not split runtime mappings and documentation so they can drift.

### 9.3 Schema example

```java
public record CreateOrderCommand(
        @Schema(description = "Tenant-visible customer identifier", example = "12001")
        @Positive Long customerId,
        @Schema(description = "Unique order lines; one through one hundred entries")
        @NotEmpty @Size(max = 100) List<@Valid CreateOrderItemCommand> items) {
}
```

The generated required list, numeric/collection constraints, JSON names, examples, enum/date formats, and actual runtime validation must be compared. For generic response wrappers, confirm springdoc resolves the concrete payload; use explicit `@Content(schema = @Schema(implementation = ...))` or a repository-standard customizer only when generation is otherwise wrong. Do not create one fake wrapper subtype per endpoint without a proven tooling limitation and approved convention.

## 10. GraphQL schema and operation standard

GraphQL design is schema-first even when resolvers are implemented with annotations. The Spec must name the checked-in SDL path, affected schema types/fields, resolver symbols, and consumer operation documents.

### 10.1 Schema rules

- Use nouns for object/interface types and task-oriented verbs for mutations.
- A Query field is read-only; a Mutation field is a Command and owns business validation/transaction/side effects.
- Use dedicated input types for mutations; do not expose persistence entities or reuse output types as input by convenience.
- Decide nullability deliberately. `!` is a compatibility promise; explain nullable fields and partial-data behavior.
- Use enums for closed protocol values, custom scalars only with a proven requirement and exact coercion/serialization rules, and IDs with stable opaque semantics.
- Define deprecation with `@deprecated(reason: ...)`, replacement, consumer migration, and removal policy.
- Avoid unbounded nested collections and recursive graph expansion.
- Prefer cursor/connection pagination for evolving ordered graphs when repository conventions support it; define edge/node/cursor/pageInfo semantics, stable ordering, limits, and concurrent-change behavior. Do not invent Relay wrappers for a simple bounded list without a requirement.

### 10.2 Operation contract

For every affected field, provide:

1. the SDL fragment, including descriptions, input/output types, nullability, enums/scalars, and deprecation;
2. the named consumer operation document with variables and a complete representative selection set;
3. a variable table with type, required/null/default, validation, meaning, example, and source;
4. complete success and error/partial-data `jsonc` transport examples;
5. exact `Query.field`, `Mutation.field`, or `Subscription.field` identity and Spring resolver symbol;
6. authentication at the HTTP endpoint plus field/business authorization at the Service/resolver boundary;
7. transaction, idempotency, concurrency, side effects, and consistency for mutations;
8. fetch plan, batching, pagination, depth/complexity/cost limits, and N+1 prevention for queries;
9. compatibility, consumer documents, schema checks, resolver tests, and transport tests.

Use top-level GraphQL `errors` for parse, validation, coercion, authorization/execution, and unexpected failures according to the project policy. Define safe `extensions.code`, field/argument paths, retryability, partial `data`, masking, and frontend behavior. Do not put every domain validation failure into HTTP `400`; GraphQL execution can return a protocol response containing errors and partial data. Do not hide business outcomes inside a generic success payload without stable typed/error semantics.

### 10.3 Spring for GraphQL mapping

Use the Spring Boot GraphQL starter and current project configuration when GraphQL is approved. The standard mapping is:

| GraphQL contract | Spring mapping | Required design evidence |
| --- | --- | --- |
| `Query.field` | `@QueryMapping` or exact `@SchemaMapping(typeName = "Query", field = "...")` | SDL field, resolver, Query object/arguments, read-only Service call |
| `Mutation.field` | `@MutationMapping` | SDL input/output, `@Argument @Valid`, `@Validated` groups when reused, Command Service, transaction/idempotency/errors |
| Nested field | `@SchemaMapping` | parent/source type, loading ownership, auth, null/error behavior |
| Batched nested field | `@BatchMapping` or registered DataLoader | keys, stable result association, batch size, missing items, ordering, cache scope, N+1 test |
| Subscription | `@SubscriptionMapping` | publisher/source, auth, transport, backpressure, reconnect/resume, cleanup |

The `/graphql` path-level security gate is not sufficient for field authorization because all operations share one URL. Reuse Spring Security at Service methods (for example the project's established `@PreAuthorize` policy) and document field-level disclosure rules. Never place authorization only in descriptions.

### 10.4 GraphQL transport

Spring for GraphQL and the GraphQL-over-HTTP draft guide the transport. Record the exact supported method/media types from the installed version. POST support and JSON request envelopes are baseline; `application/graphql-response+json` should be designed/tested when supported. GET, persisted queries, WebSocket, RSocket, multipart upload, subscriptions, and incremental delivery are not implied and require explicit support and security/cache analysis.

The GraphQL-over-HTTP document is still a draft. Treat its version as a compatibility input, record the installed Spring behavior, and do not claim draft-only behavior without a test.

### 10.5 GraphQL example

```graphql
"Order operations available to the authenticated tenant."
type Query {
  order(id: ID!): Order
}

type Mutation {
  createOrder(input: CreateOrderInput!): CreateOrderPayload!
}

input CreateOrderInput {
  customerId: ID!
  items: [CreateOrderItemInput!]!
  idempotencyKey: String!
}

type CreateOrderPayload {
  order: Order!
  traceId: String!
}
```

```graphql
mutation CreateOrder($input: CreateOrderInput!) {
  createOrder(input: $input) {
    order {
      id
      number
      status
    }
    traceId
  }
}
```

The real Spec must expand all referenced affected types and variables; this abbreviated example only demonstrates separation of SDL and consumer operation documents.

## 11. REST and GraphQL coexistence rules

When both protocols expose the same capability:

- name one application Service/Command/Query behavior as the business source of truth;
- prohibit protocol adapters from implementing different validation, permission, transaction, state, or event rules;
- map REST status/error envelopes and GraphQL errors from the same typed business outcomes without leaking transport concerns into domain logic;
- define idempotency scope across protocols so the same command cannot bypass duplicate protection;
- define shared rate/cost policy, audit identity, tenant source, observability correlation, and sensitive-field rules;
- test equivalent business outcomes while separately testing protocol serialization and errors;
- document why two protocols are necessary for named consumers. If no independent value exists, select one rather than maintaining two façades.

## 12. Security and documentation exposure

For every operation or field, document authentication, authorization, tenant isolation, ownership checks, sensitive fields, rate/cost limits, audit, and error disclosure. Then map them to runtime and documentation:

- `@SecurityScheme` defines a real mechanism; `@SecurityRequirement` references its exact registered name and scopes;
- HTTP security protects REST/docs/GraphQL paths; method/Service security protects business operations and GraphQL fields;
- examples use synthetic, non-secret values;
- Swagger UI OAuth configuration contains no client secret in source or public output;
- production docs/UI exposure is an explicit configuration/security decision, not an accidental default;
- GraphQL introspection/GraphiQL exposure is environment- and audience-specific; disabling it is not a substitute for authorization;
- schema descriptions and error details must not reveal internal topology or cross-tenant existence.

## 13. API documentation and code organization

Choose annotation ownership from repository evidence:

1. Existing Controller ownership: keep mappings and OpenAPI annotations together unless local style says otherwise.
2. Existing Facade/API interface ownership: use it only if it is already the public application contract and inheritance behavior is covered by tests.
3. Central reusable components: use `@OpenAPIDefinition`, `@SecurityScheme`, reusable schemas/responses, or customizers only for truly shared facts.

Do not create parallel endpoint interfaces, response subclasses, annotation constants, or customizers solely to reduce annotation length. Reuse can be selected when it prevents real drift across many operations and springdoc can represent the generic/wrapper contract correctly. Any custom `OpenApiCustomizer`, model converter, or plugin must state the generator gap, scope, ordering, compatibility, tests, and maintenance owner.

## 14. Generated-contract verification

A Spec must identify exact feasible commands from the repository rather than assume a tool. The Plan should later order implementation and these checks.

At minimum, design verification for affected REST APIs:

- compile and focused Controller/validation/security/error tests;
- generate/read the actual `/v3/api-docs` document without claiming a live run unless performed;
- parse and validate OpenAPI syntax with an existing repository plugin/tool or a justified addition;
- assert paths, methods, unique `operationId`, parameters, required fields, schemas/formats, status codes, headers, error models, security, and deprecated flags;
- compare generated contract with a checked-in baseline or consumer contract when repository policy has one;
- verify Swagger UI/document endpoints are exposed or disabled as intended in every environment profile;
- verify no internal endpoints/schemas or secrets leak.

For GraphQL:

- validate SDL and application startup schema wiring through existing tests;
- use `GraphQlTester` and named `.graphql`/`.gql` documents for Query, Mutation, validation, authorization, errors, partial data, pagination, batching, and N+1-sensitive paths;
- test HTTP media types/status handling supported by the installed Spring version;
- diff/check schema or consumer documents according to repository policy;
- verify complexity/depth/cost limits and field security when applicable.

Static source inspection is not live runtime proof. A Spec may prescribe a later runtime check, but its final verdict must distinguish design completeness from implementation/runtime validation.

## 15. Chapter 9 blocking API gate

When any external API is affected, Chapter 9.4 must contain every row below exactly once. Each row is manually evaluated from repository evidence and the completed design:

| Gate ID | Blocking question |
| --- | --- |
| `API-GATE-001` | Is every API operation necessary, atomic, consumer-owned, and free of fetch-then-forward behavior? |
| `API-GATE-002` | Is the REST/GraphQL choice and Query/Command/Subscription role explicit and minimally sufficient? |
| `API-GATE-003` | Do all affected REST operations obey resource, method, status, header, idempotency, pagination, and compatibility semantics? |
| `API-GATE-004` | Do all affected GraphQL fields have complete SDL, operation, nullability, resolver, batching/cost, security, and error semantics? |
| `API-GATE-005` | Do runtime validation, Jackson/GraphQL coercion, schemas, examples, and public field names agree? |
| `API-GATE-006` | Are auth, permission, tenant, sensitive-data, error-disclosure, rate/cost, and documentation-exposure rules explicit? |
| `API-GATE-007` | Are springdoc compatibility, annotation ownership, OpenAPI elements, and legacy Springfox boundaries explicit for REST? |
| `API-GATE-008` | Are generated OpenAPI or GraphQL schema/operation verification, contract tests, and drift checks exact and feasible? |
| `API-GATE-009` | Do Chapter 9 contracts agree field-by-field and outcome-by-outcome with requirements, models, database, frontend, tests, compatibility, and rollout? |

Allowed status is `PASS` or evidence-backed `N/A`. `N/A` is valid for a protocol-specific row only when that protocol is absent. `FAIL`, `BLOCKED`, `UNKNOWN`, a missing row, empty evidence, or an unresolved exception prohibits `PASS — Ready for user review` and must also be reflected in Chapter 20's relevant `MC-*` rows and `MC-BLOCKER-001`.

## 16. Required Chapter 20 mappings

For an affected API, the general Manual Checks must include API evidence:

- `MC-REUSE-001`: existing Spring/springdoc/Spring GraphQL/Egon result, validation, security, converter, and gateway capabilities inspected;
- `MC-DEP-001`: no duplicate Springfox/springdoc stack or unjustified GraphQL/tooling dependency;
- `MC-VALID-001`: every REST and GraphQL input boundary, group, nested validation, normalization, and error path;
- `MC-JSON-001`: Jackson/wire/OpenAPI/GraphQL response agreement and no competing JSON library;
- `MC-CONFIG-001`: documentation/GraphQL keys consistent across environments;
- `MC-SCOPE-001`: only affected operations/protocols are fully redesigned;
- `MC-TEST-001`: generated contract/schema, validation, security, error, compatibility, and consumer tests;
- `MC-BLOCKER-001`: all `API-GATE-*` rows and major protocol/compatibility decisions closed.

## 17. Review failures

Return `REVISE` or `BLOCKED` as appropriate when any of these occurs:

- a route table exists without per-operation REST/CQRS/GraphQL and documentation design;
- a Query mutates business state or a Command is named as a generic field update despite task semantics;
- CQRS is used to justify an unrequired bus, event store, read database, handler layer, or package structure;
- REST uses verbs for ordinary resources, wrong method semantics, generic `200` for every outcome, undocumented async behavior, or request bodies on GET;
- OpenAPI depends on inferred `operationId`, omits material errors/security, shows a DTO class instead of the actual wrapper, or disagrees with runtime validation/Jackson;
- new code uses Springfox/Swagger 2 annotations or mixes them with OpenAPI 3 annotations;
- documentation is separated into an interface/customizer with no architectural reason or drift test;
- a fake endpoint exists only to document authentication or another non-operation concern;
- GraphQL is documented only as `POST /graphql`, or Swagger annotations are treated as the GraphQL schema;
- a GraphQL Query performs a write, Mutation lacks command/idempotency/transaction semantics, nested fields create N+1 queries, or null/error/authorization behavior is undefined;
- REST and GraphQL duplicate business logic or produce conflicting validation, permissions, state transitions, or idempotency;
- docs/UI/introspection exposure and security are accidental or contain secrets;
- generated document/schema verification is described vaguely as “check Swagger” or “test GraphQL.”

## 18. Normative and research sources

Checked on 2026-09-03. Protocol/framework versions in a real Spec must still be verified against the current project.

- [OpenAPI Specification 3.1](https://spec.openapis.org/oas/v3.1.0)
- [RFC 9110: HTTP Semantics](https://www.rfc-editor.org/rfc/rfc9110.html)
- [RFC 9457: Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457.html)
- [springdoc-openapi official documentation](https://springdoc.org/)
- [springdoc-openapi official repository](https://github.com/springdoc/springdoc-openapi)
- [Swagger Core OpenAPI 3 annotation guide](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations)
- [OpenAPI 3 bearer authentication](https://swagger.io/docs/specification/v3_0/authentication/bearer-authentication/)
- [Microsoft CQRS pattern guidance](https://learn.microsoft.com/en-us/azure/architecture/patterns/cqrs)
- [GraphQL Specification](https://spec.graphql.org/October2021/)
- [GraphQL over HTTP draft](https://graphql.github.io/graphql-over-http/draft/)
- [Spring for GraphQL annotated controllers](https://docs.spring.io/spring-graphql/reference/controllers.html)
- [Spring for GraphQL transports](https://docs.spring.io/spring-graphql/reference/transports.html)
- [Spring for GraphQL security](https://docs.spring.io/spring-graphql/reference/security.html)
- [Spring for GraphQL testing](https://docs.spring.io/spring-graphql/reference/testing.html)
- [Spring Framework RFC 9457 error responses](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)
- [Mshuyan/swagger example repository](https://github.com/Mshuyan/swagger) — non-normative legacy/example input only
