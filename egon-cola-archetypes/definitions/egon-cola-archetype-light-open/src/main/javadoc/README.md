# Egon-COLA Light Open Archetype

This classifier documents the Egon-COLA Light Open Maven Archetype distribution. It is documentation for the archetype, not a Java API reference.

Use the public `top.egon:egon-cola-archetype-light-open` coordinate with Maven's `archetype:generate` goal to create a project. Maintainers edit the corresponding normal source project under `source-projects`, then run `scripts/generate_archetypes.sh generate` before packaging.

## Dependency and runtime ownership

Generated projects inherit the released `top.egon:egon-cola-archetypes-parent` at a concrete version with an empty `relativePath`. The parent imports the Components BOM, manages Common dependencies and ShardingSphere 5.5.3, and keeps Commons Lang at 3.20.0. Consumer modules inherit their own project root. Install the matching parent/BOM and required artifacts locally before validating an unpublished release; a local install does not publish artifacts.

Open retains its existing Spring Cloud/Nacos stack and consumed Common Core/ID/MyBatis Plus/Dynamic Thread Pool components. Light Open retains Springdoc and has no Dubbo business contract.
