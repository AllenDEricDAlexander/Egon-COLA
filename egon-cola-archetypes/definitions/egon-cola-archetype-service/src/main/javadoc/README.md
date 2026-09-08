# Egon-COLA Service Archetype

This classifier documents the Egon-COLA Service Maven Archetype distribution. It is documentation for the archetype, not a Java API reference.

Use the public `top.egon:egon-cola-archetype-service` coordinate with Maven's `archetype:generate` goal to create a six-module project. Maintainers edit the corresponding normal source project under `source-projects`, then run `scripts/generate_archetypes.sh generate` before packaging.

## Dependency and runtime ownership

Generated projects inherit the released `top.egon:egon-cola-archetypes-parent` at a concrete version with an empty `relativePath`. The parent imports the Components BOM, manages Common dependencies and ShardingSphere 5.5.3, and keeps Commons Lang at 3.20.0. Consumer modules inherit their own project root. Install the matching parent/BOM and required artifacts locally before validating an unpublished release; a local install does not publish artifacts.

This native family uses Egon RPC unary Protobuf contracts (gRPC 1.75.0 / Protobuf 4.32.0), the RPC DDC adapter, DDC configuration and HTTP registration, and the platform OpenAPI MVC starter. Runtime configuration lives in `application.yml` plus the dev/test/prod files; imported configuration uses Spring Boot Config Data. Supply the DDC endpoints, HMAC credentials, TLS material and IdP SERVICE client settings described in the generated README. Test profiles disable external integration lifecycles.

Service exposes eleven Evaluation operations through `top.egon:egon-cola-evaluation-facade` and consumes Organization through `top.egon:egon-cola-organization-facade`. Existing business facade DTOs, HTTP/GraphQL/MQ behavior and database contracts are retained.

Platform API document governance is opt-in. Controllers need explicit, unique `@Operation(operationId = ...)` values before enabling that catalog; existing business endpoints remain accessible with the default configuration. Live DDC/IdP/TLS discovery, cross-process RPC and production rollout require operator acceptance.
