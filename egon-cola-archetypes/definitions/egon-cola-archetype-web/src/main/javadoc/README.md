# Egon-COLA Web Archetype

This classifier documents the Egon-COLA Web Maven Archetype distribution. It is documentation for the archetype, not a Java API reference.

Use the public `top.egon:egon-cola-archetype-web` coordinate with Maven's `archetype:generate` goal to create a six-module web project with HTTP, GraphQL and OpenAPI support. Maintainers edit the corresponding normal source project, then regenerate the archetype before packaging.

## Dependency and runtime ownership

Generated projects inherit the released `top.egon:egon-cola-archetypes-parent` at a concrete version with an empty `relativePath`. The parent imports the Components BOM, manages Common dependencies and ShardingSphere 5.5.3, and keeps Commons Lang at 3.20.0. Consumer modules inherit their own project root. Install the matching parent/BOM and required artifacts locally before validating an unpublished release; a local install does not publish artifacts.

This native family uses Egon RPC unary Protobuf contracts (gRPC 1.75.0 / Protobuf 4.32.0), the RPC Tianshu adapter, Tianshu configuration and HTTP registration, and the platform OpenAPI MVC starter. Runtime configuration lives in `application.yml` plus the dev/test/prod files; imported configuration uses Spring Boot Config Data. Supply the Tianshu endpoints, HMAC credentials, TLS material and Tianquan-Shoubing SERVICE client settings described in the generated README. Test profiles disable external integration lifecycles.

Web exposes ten Organization operations through `top.egon:egon-cola-organization-facade` and consumes Evaluation through `top.egon:egon-cola-evaluation-facade`. Existing business facade DTOs, HTTP/GraphQL/MQ behavior and database contracts are retained.

Platform API document governance is opt-in. Controllers need explicit, unique `@Operation(operationId = ...)` values before enabling that catalog; existing business endpoints remain accessible with the default configuration. Live Tianshu/Tianquan-Shoubing/TLS discovery, cross-process RPC and production rollout require operator acceptance.
