# Egon-COLA Web Open Archetype

This classifier documents the Egon-COLA Web Open Maven Archetype distribution. It is documentation for the archetype, not a Java API reference.

Use the public `top.egon:egon-cola-archetype-web-open` coordinate with Maven's `archetype:generate` goal to create a seven-module web project with HTTP, GraphQL and OpenAPI support. The Gateway remains an external integration boundary rather than a generated module. Maintainers edit the corresponding normal source project, then regenerate the archetype before packaging.

## Dependency and runtime ownership

Generated projects inherit the released `top.egon:egon-cola-archetypes-parent` at a concrete version with an empty `relativePath`. The parent imports the Components BOM, manages Common dependencies and ShardingSphere 5.5.3, and keeps Commons Lang at 3.20.0. Consumer modules inherit their own project root. Install the matching parent/BOM and required artifacts locally before validating an unpublished release; a local install does not publish artifacts.

Open retains its existing Spring Cloud/Nacos stack and consumed Common Core/ID/MyBatis Plus/Dynamic Thread Pool components. Service/Web Open retain their local facade Protobuf contracts and the 21-operation external interoperability surface, using gRPC 1.73.0 / Protobuf 3.25.8 and Dubbo 3.3.6. The Open Gateway boundary remains external.
