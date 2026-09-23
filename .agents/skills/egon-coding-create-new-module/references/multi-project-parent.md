# Aggregating Egon-COLA business projects

Read this when the user asks for an outer parent POM, a reactor containing several archetype-generated projects, or the inheritance relationship between them. Do not apply this layout to a single generated project without a requirement for an outer reactor.

## Identify the two different POM relationships

- One archetype invocation creates a **complete project**. Native `light` has its root POM. Native `web` and `service` have a root `pom` that already parents their own seven internal modules. The generated root directly inherits the published `top.egon:egon-cola-archetypes-parent` at the selected Egon release, with `<relativePath/>`. Its internal modules inherit the generated project root.
- An additional business root is needed only to build several independent generated projects in one Maven reactor. `<modules>` aggregates them; it does **not** make that root the Maven `<parent>` of the child project POMs.

## Default: a pure outer aggregator

For a new multi-project business repository, recommend a plain `packaging=pom` aggregator with its own business GAV and **no `<parent>`**. Each generated project retains its archetype-produced direct parent. This avoids changing generated POMs or inheriting release/build settings into the outer root that its children will not inherit.

Example of an outer `pom.xml`; use the user's actual groupId, artifactId, version and exact generated directory names in real work:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example.shop</groupId>
    <artifactId>shop-reactor</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    <modules>
        <module>order-service</module>
        <module>catalog-service</module>
    </modules>
</project>
```

Layout:

```text
shop/pom.xml                 aggregation only
shop/order-service/pom.xml   parent: top.egon:egon-cola-archetypes-parent
shop/catalog-service/pom.xml parent: top.egon:egon-cola-archetypes-parent
```

List **project roots**, not their internal `domain`/`application`/`starter` modules again. Let Maven calculate reactor order from exact GAV dependencies. A peer Facade must use the actual sibling's published groupId/artifactId/version; merely placing both projects in `<modules>` does not supply or correct that dependency.

## Adding Egon-COLA capabilities to generated projects

Keep three Maven concepts separate:

| POM location | What belongs there | What it does not do |
| --- | --- | --- |
| Outer business reactor | Relative `<module>` paths to generated **project roots** | Does not add Egon libraries to a generated project's classpath or manage versions for roots that do not inherit it |
| Generated project root | Inherits `top.egon:egon-cola-archetypes-parent`; owns its internal `<modules>`, current `egon-cola.version`, and project-wide dependency management | A managed dependency does not become an actual dependency of every inner module |
| Generated inner module | Declares the Egon Component, platform starter/contract, or sibling Facade it actually consumes in `<dependencies>` | Does not aggregate the source repositories of Egon-COLA |

The published archetypes parent imports `top.egon:egon-cola-components-bom` at `${egon-cola.version}` and explicitly manages some platform artifacts, including `egon-cola-tianshu-starter`, `egon-cola-tianshu-http-registration-starter`, `yuheng-starter-openapi`, and `yuheng-starter-openapi-webmvc`. The Components BOM manages many Component coordinates, including Common Core, the MP-SDJ starter, cache, ID, RPC and transactional outbox. Check the **current** parent/BOM for the exact requested artifact; they do not manage every Platform artifact. Do not treat `egon-cola-xingyuan/pom.xml` as an importable BOM merely because it has `pom` packaging.

An isolated `help:effective-pom` check against the current published parent resolved the MP-SDJ starter, transactional outbox starter and Yuheng WebMVC starter to `5.4.1` in dependency management; none became a direct dependency of that otherwise empty generated-project root. This is the practical difference between version management and adding a capability.

First inspect the selected archetype's generated project/module POMs and the resolved dependency tree. They already declare the capabilities needed by that product. Add a dependency only to the module that needs a missing capability, with the user's explicit approval under the coding skills' dependency gate. When an artifact is managed by the inherited parent/BOM, the module dependency normally omits `<version>`; when not managed, record its exact released coordinate and version decision rather than relying on a sibling source directory or an invented BOM entry. Do not automatically add every Egon component or platform starter to the outer root.

For example, if a Web project's infrastructure module is explicitly approved to use an outbox that it does not already depend on, that module's POM may declare the managed artifact:

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-transactional-outbox-starter</artifactId>
</dependency>
```

That is a **dependency**, not a `<module>` entry in the business reactor. A Service/Web peer Facade is a different case: use the actual business project's GAV in the consuming module and its generated root's dependency management. If both business projects are in the outer reactor, Maven can resolve a matching sibling artifact, but the consumer still needs the explicit dependency and correct version; check generation-time `organizationFacade*` or `evaluationFacade*` properties against the sibling contract.

The outer reactor's own `<dependencies>` and `<dependencyManagement>` are not inherited by generated project roots in the default layout. To centrally govern extra business dependencies across those roots, first obtain a separate design decision for a business-parent inheritance chain. Maven permits only one direct `<parent>` per POM, and the current archetype verifier expects the released Egon parent directly, so this skill must not rewrite that chain or claim outer aggregation provides inherited policy.

## Optional: the outer aggregator also inherits Egon

If the user explicitly needs Egon settings on the outer POM itself, it may inherit the published `top.egon:egon-cola-archetypes-parent` at the selected release with `<relativePath/>`. When the aggregator's own version is a business version, it **must also set** `<egon-cola.version>` to that Egon release in its `<properties>`. The archetypes parent defines `egon-cola.version` from `${project.version}`; without the override, an outer `1.0.0-SNAPSHOT` project tries to import `egon-cola-components-bom:1.0.0-SNAPSHOT`. This failure was confirmed with an isolated offline Maven model check. In the current checkout the verified Egon release is `5.4.1`; read the current `egon-cola-archetypes/pom.xml` again for future work.

This optional outer inheritance does not make generated project roots inherit the outer POM. Shared business dependency management or plugin configuration added to that outer POM therefore will not reach the generated projects. If inheritance by each generated project is a real requirement, design it separately: replacing their direct Egon parent with a business parent changes the archetype's verified POM contract and must not be done silently by this skill.

Never use the internal repository `egon-cola-aggregation-parent` as the consumer business parent, copy `source-projects` POMs, or insert an unpublished version as a default.

## Skill workflow when aggregation is requested

1. Inspect the target outer directory and any existing `pom.xml`. Confirm the user wants aggregation only or explicitly wants shared parent inheritance; default to pure aggregation when no shared inherited settings are required.
2. Resolve the outer business GAV and exact child project directory names from the request or existing files. Do not invent peer Facade coordinates. If these essential identities are missing for an actual creation, ask once.
3. Generate at most the one requested business project with `references/archetype-selection.md`. For a parent-only request, skip archetype generation. Preserve an existing outer POM and unrelated work.
4. Create or path-limit the outer `pom.xml` only when the user asks to create/update that reactor. Add only exact child project root paths; keep each child's generated `<parent>` and `<relativePath/>` unchanged. Do not edit other reactor POMs.
5. Verify POM syntax, that each module path exists, that each generated root still inherits the published archetypes parent, and that each inner module still inherits its generated root. Inspect the inherited effective dependency management and each selected module's actual dependencies when the request mentions Egon capabilities. Run the smallest local Maven `validate` against the outer POM when the required released artifacts are resolvable. Report a missing artifact without installing or publishing it automatically.

An isolated two-child pure aggregator with direct Egon child parents passed offline `mvn validate` on 2026-09-23. An outer POM inheriting Egon with its own business version failed BOM resolution without an explicit `egon-cola.version` and passed when that property was set to the selected Egon release. These model checks prove POM relationships only, not generated business builds or runtime integrations.
