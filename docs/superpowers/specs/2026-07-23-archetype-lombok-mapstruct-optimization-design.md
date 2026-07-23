# Archetype Lombok and Mapping Optimization Design

Date: 2026-07-23

## Goal

Increase the effective use of Lombok, MapStruct, and MapStruct Plus in the
`light`, `service`, and `web` archetype templates without weakening domain
semantics or changing the generated applications' behavior.

The optimization follows the role boundaries from
`/Users/mario/SelfProject/blog/source/_posts/java/utils/lombok.md`:

- Lombok removes ordinary constructor, accessor, and logging boilerplate.
- MapStruct owns explicit, compile-time checked mapping.
- MapStruct Plus removes repetitive mapper plumbing for flat model pairs.
- Record compact constructors and explicit Java retain validation,
  normalization, restoration, exception, and other semantic behavior.

## Current State

The generated POMs already declare Lombok and MapStruct Plus, but current use is
uneven:

- `light` has 68 explicit constructors and 25
  `@RequiredArgsConstructor` classes.
- `service` has 37 explicit constructors, 12
  `@RequiredArgsConstructor` classes, and two concrete MapStruct Plus
  `BaseMapper` interfaces.
- `web` has 80 explicit constructors, no `@RequiredArgsConstructor` classes,
  and no generated mapper interfaces.
- The three archetypes contain no `@AutoMapper` model declarations.
- Many adapter converters still construct records manually even when the
  mapping is a flat field-for-field copy.
- The compiler processor path includes Lombok and MapStruct Plus, but does not
  include `lombok-mapstruct-binding`.

The existing contract that converter wrappers must not inject the generic
`io.github.linpeilie.Converter` remains in force. Concrete mapper types keep
mapping boundaries inspectable and avoid runtime lookup through a generic
converter bean.

## Considered Approaches

### Blanket annotation conversion

Replace every constructor and converter with Lombok or automatic mapping.

This is rejected because exception constructors must call `super(...)`, Record
compact constructors enforce invariants, domain restore paths contain semantic
decisions, and framework bootstrap constructors sometimes require a specific
shape.

### Role-based incremental conversion

Use Lombok only for structural constructors, MapStruct for explicit mapping,
and MapStruct Plus `BaseMapper` for flat pairs while retaining wrappers around
semantic mapping.

This is selected because it removes meaningful boilerplate, preserves the
existing architecture, and lets generated-project compilation prove annotation
processing end to end.

### Configuration-only optimization

Add processor configuration and documentation without changing source
templates.

This is rejected because it would leave the dependencies mostly demonstrative
rather than useful.

## Lombok Design

### Constructor injection

Spring-managed classes whose constructors only assign injected fields use
`@RequiredArgsConstructor` and `final` fields. This includes controllers,
facade implementations, consumers, application services, repositories, caches,
publishers, aspects, and adapters.

When constructor parameters currently carry `@Qualifier` or Spring `@Value`,
the annotation moves to the corresponding final field. The generated
`lombok.config` copies both annotations to Lombok-generated constructor
parameters.

Constructors remain explicit when they:

- call a superclass constructor;
- validate or normalize input;
- select defaults or restore domain state;
- create defensive copies;
- are required entry points for framework bootstrap behavior;
- provide an intentional overload beyond dependency injection.

### Persistence objects

JPA persistence objects with a boilerplate protected no-argument constructor
and a field-for-field full constructor use:

```java
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
```

`@Data` is not used for persistence or domain objects. The Lombok configuration
flags future `@Data` and `val` use with warnings.

### Logging

Classes that only declare `LoggerFactory.getLogger(CurrentClass.class)` use
`@Slf4j`. Logging statements and their levels remain unchanged.

## Mapping Design

### MapStruct

Flat request/command, result/response, and result/view conversions become
concrete Spring mapper interfaces. Each mapper uses compile-time error reporting
for unmapped target fields.

Mappings remain explicit when names or types differ. Business decisions do not
move into MapStruct expressions.

### MapStruct Plus

Concrete mapper interfaces extend
`io.github.linpeilie.BaseMapper<Source, Target>` only for flat model pairs where
the `BaseMapper` contract is meaningful. Wrappers may perform explicit enum,
value-object, timestamp, relationship, or restoration work around the mapper.

The generic `io.github.linpeilie.Converter` is not injected. `@AutoMapper` is
not added to domain models merely to increase annotation counts, because that
would introduce outward layer dependencies from domain types to adapter or
persistence types.

### Compiler configuration

All three generated root POMs add
`org.projectlombok:lombok-mapstruct-binding:0.2.0` to
`maven-compiler-plugin.annotationProcessorPaths` after Lombok and before
MapStruct Plus. This makes the Lombok/MapStruct processing relationship
explicit.

## Contract and Validation

The archetype `verify.groovy` scripts are extended before template changes so
the first focused run fails for the missing contract. The checks cover:

- the binding version property and processor path;
- Lombok configuration for `@Qualifier`, `@Value`, generated annotations,
  constructor properties, and restricted broad annotations;
- representative Lombok-generated dependency and JPA constructors;
- concrete MapStruct and MapStruct Plus mapper templates;
- continued absence of generic `Converter` injection;
- continued presence of explicit semantic mapping where required.

Validation then runs:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml clean integration-test
git diff --check
```

The Maven integration test generates all three projects, compiles annotation
processors and generated mappers, and runs their test suites. The project is
not started manually.

## Compatibility and Scope

- Generated public APIs, endpoints, facade contracts, database schemas, package
  boundaries, and runtime behavior remain unchanged.
- Existing Flyway migrations are not modified.
- No new runtime dependency is introduced; the binding artifact is
  compile-time annotation-processor support.
- The work changes archetype source templates and generated-project contract
  checks, never checked-in `target/` output.
- Unrelated repository files and the user's modified root `README.md` remain
  untouched.

## Completion Criteria

- All three archetypes demonstrate actual Lombok constructor generation.
- All three archetypes compile concrete mapping code rather than relying on
  manual record construction for every flat mapping.
- MapStruct Plus remains concrete and layer-safe.
- Semantic constructors and conversions remain explicit.
- Full archetype integration tests and whitespace validation pass.
