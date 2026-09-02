# Egon-COLA Web Open Archetype

This classifier documents the Egon-COLA Web Open Maven Archetype distribution. It is documentation for the archetype, not a Java API reference.

Use the public `top.egon:egon-cola-archetype-web-open` coordinate with Maven's `archetype:generate` goal to create a seven-module web project with HTTP, GraphQL and OpenAPI support. The Gateway remains an external integration boundary rather than a generated module. Maintainers edit the corresponding normal source project, then regenerate the archetype before packaging.
