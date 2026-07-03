# Archetype Clean Architecture Optimization Design

Date: 2026-07-03

## Goal

Optimize the current `egon-cola-archetype-light`, `egon-cola-archetype-web`, and `egon-cola-archetype-service` templates so their generated projects consistently follow the same Clean Architecture boundary rules without restructuring the archetypes.

This is a detail-level optimization. It must preserve the current business samples, current module shapes, current package trunks, and the existing `application.manage` naming style. The work must not redesign the archetypes, introduce DDD aggregate patterns, add `command` / `query` / `usecase` packages, or start any generated application.

## Confirmed Decisions

- Apply the same rules to all three archetypes: light, web, and service.
- Keep existing archetype structure and business samples:
  - `egon-cola-archetype-light`: single-module `student-management`.
  - `egon-cola-archetype-web`: multi-module `student-management-organization`.
  - `egon-cola-archetype-service`: multi-module `student-management-evaluation`.
- `application` layer returns domain models only. It must not return View, DTO, facade response, adapter response, or common response objects.
- `adapter` layer converts domain models into external HTTP, RPC, or MQ objects.
- Spring Beans use ordinary classes with Lombok `@RequiredArgsConstructor`.
- Spring Beans must be explicitly named, and injection points must use `@Qualifier`.
- All cross-model converters use MapStruct Plus, including adapter converters and infrastructure PO converters.
- All three archetypes expose facade examples through Dubbo3 RPC with Triple as the default protocol.
- Do not generate native `grpc-java` service implementations, proto service files, or a separate gRPC module.
- Do not keep "technology-neutral RPC" wording where the generated template now intentionally demonstrates Dubbo3 Triple.
- `light` remains single-module, but it still needs a Dubbo3 Triple facade example.
- Write and commit this design before implementation planning.

## Current Context

The current templates already have the intended broad layer shapes, but several details are inconsistent with the requested boundary:

- `application` methods currently return `*View` records in the light, web, and service samples.
- Adapter implementations currently convert from application views instead of domain models.
- Some converters are static helper classes or hand-written Spring components instead of MapStruct Plus based converters.
- Bean annotations and injection style are not uniform; generated beans are not consistently named, and constructor injection does not consistently use `@Qualifier`.
- The service archetype currently documents RPC/MQ boundaries in a technology-neutral way, while the requested target is explicit Dubbo3 Triple.
- The earlier light spec excluded MapStruct Plus, but this optimization now explicitly requires MapStruct Plus across all three archetypes.

The optimization should adjust these details in place instead of replacing the archetype designs.

## Scope

In scope:

- `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources`.
- `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/META-INF/maven/archetype-metadata.xml`.
- `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic`.
- `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources`.
- `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/META-INF/maven/archetype-metadata.xml`.
- `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic`.
- `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources`.
- `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/META-INF/maven/archetype-metadata.xml`.
- `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic`.
- Generated POMs, generated source templates, generated tests, generated README text, and generated `application.yml` files where needed for Dubbo3 Triple or MapStruct Plus.

Out of scope:

- Replacing the three archetype business samples.
- Renaming `application.manage`.
- Adding `command`, `query`, `usecase`, aggregate, factory, or strategy packages.
- Redrawing the architecture documents except for narrow wording fixes that prevent contradiction with generated templates.
- Editing existing generated Flyway migration files outside the archetype templates.
- Adding native grpc-java service implementations.
- Adding a separate gRPC module.
- Starting generated applications.

## Layer Boundary Rules

### Application Returns Domain Models

Each `application.manage.*` interface and implementation must return domain models or simple values:

Allowed examples:

```text
Student
Course
User
SchoolClass
ExamResult
List<Course>
Optional<User>
boolean / long / int
```

Forbidden examples:

```text
StudentView
CourseView
UserView
SchoolClassView
ExamResultView
StudentDTO
CourseDTO
SingleResponse<CourseDTO>
Response
HTTP response objects
MQ message DTOs
```

Existing `*View` records should be removed if they only exist to shape external responses. If a value is a facade contract, it belongs under `facade/dto`. If it is an adapter-only message object, it belongs under `adapter/dto`. If it is part of business behavior, it should be represented as a domain model or a small domain value object using existing domain package conventions.

### Adapter Converts External Objects

Adapter classes own the conversion between inner models and external shapes:

```text
HTTP request/response <-> adapter convertor <-> domain model / simple parameters
Facade request/response <-> adapter convertor <-> domain model / simple parameters
MQ message <-> adapter convertor <-> domain model / simple parameters
Infrastructure PO <-> infrastructure converter <-> domain model
```

`application` must not import `facade.dto`, `adapter.dto`, `common.response`, or HTTP response types. `adapter` may import `application`, `facade`, `common`, and Spring Web or Dubbo annotations as appropriate for the archetype.

## Bean and Injection Rules

Generated Spring Beans must use ordinary classes and constructor injection generated by Lombok:

```java
@Service("studentManage")
@RequiredArgsConstructor
public class StudentManageImpl implements StudentManage {

    @Qualifier("studentDomainService")
    private final StudentDomainService studentDomainService;
}
```

Rules:

- Bean annotations must include explicit names, such as `studentManage`, `courseFacadeImpl`, `userRpcConvertor`, or `courseRepositoryImpl`.
- Dependency fields must be `private final`.
- Dependency fields must use `@Qualifier`.
- Bean classes must use `@RequiredArgsConstructor`.
- Do not use field `@Autowired`.
- Do not use setter injection.
- Do not convert Spring Beans into records.
- Add generated `lombok.config` where needed so Lombok copies `@Qualifier` to generated constructor parameters:

```properties
config.stopBubbling = true
lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier
lombok.addLombokGeneratedAnnotation = true
```

The exact bean names should follow the current class names and layer conventions instead of introducing a new naming system.

## MapStruct Plus Rules

MapStruct Plus is the standard converter mechanism for generated templates.

Use it in:

- `adapter/convertor` for HTTP, RPC, and MQ DTO conversion.
- `infrastructure/repo/**/converter` for PO/domain conversion.
- Single-module light archetype converter packages using the same responsibilities.

Do not use it to hide business logic. Straight field mapping belongs in MapStruct Plus; semantic conversion remains explicit Java code near the converter method.

Examples of straightforward mapping:

```text
id -> id
name -> name
email -> email
credit -> credit
courseIds -> courseIds
createdAt -> createdAt
```

Examples that should stay explicit:

```text
enum-to-string display rules
status transition decisions
score pass/fail decisions
cross-field derived values
external code fallback behavior
```

Generated POMs should include a single MapStruct Plus version property and configure the annotation processor alongside Lombok. Adapter and infrastructure modules should receive the dependencies they need; the light single-module POM should include both runtime starter and annotation processor configuration.

## Dubbo3 Triple Facade Rules

All three archetypes must demonstrate facade exposure through Dubbo3 RPC with Triple as the default protocol.

Rules:

- `facade` defines RPC interfaces and request/response DTOs.
- `adapter/facade` or `adapter/facade/impl` implements facade interfaces.
- Facade implementations use `@DubboService`.
- Default protocol is Triple, configured as `tri`.
- Do not generate native grpc-java services.
- Do not generate proto service definitions for native grpc-java.
- Do not add a standalone gRPC module.
- Do not enable both `tri` and legacy `dubbo` protocols by default.

Default generated configuration should include:

```yaml
dubbo:
  application:
    name: ${spring.application.name}
  registry:
    address: ${DUBBO_REGISTRY_ADDRESS:N/A}
  protocol:
    name: tri
    port: ${DUBBO_PORT:50051}
  provider:
    timeout: 3000
    retries: 0
```

Using `N/A` keeps the generated sample testable without requiring a registry by default. Teams can switch it to Nacos or another registry in their real project.

## Archetype-Specific Adjustments

### Light

Keep the generated project as a single Maven module and preserve the current student/course sample.

Required optimizations:

- Keep HTTP controller examples.
- Add or keep facade API and facade DTO examples.
- Add a Dubbo3 Triple facade implementation under the existing adapter facade implementation package.
- Change `StudentManage` and `CourseManage` to return `Student` and `Course` domain models.
- Remove `StudentView` and `CourseView` if they are no longer used.
- Convert `Student` and `Course` domain models to facade DTOs or HTTP response bodies in adapter converters.
- Replace static converter helpers with Spring Beans using MapStruct Plus.
- Apply named Bean and `@Qualifier` rules to controllers, facade implementation, manage implementations, domain services, repositories, handlers, and converters.
- Update generated tests to assert the same behavior through domain-returning application services and adapter-converted external responses.

The light archetype remains a single-module teaching sample; adding the facade example must not turn it into a multi-module project.

### Web

Keep the generated project as the existing seven-module organization sample.

Required optimizations:

- Preserve modules: `common`, `facade`, `domain`, `application`, `infrastructure`, `adapter`, and `starter`.
- Preserve user and teaching domains.
- Keep HTTP controller examples.
- Expose facade examples through Dubbo3 Triple.
- Change `UserManage` and `SchoolClassManage` to return `User` and `SchoolClass` domain models.
- Remove `UserView` and `SchoolClassView` if they are no longer used.
- Let adapter converters transform domain models into HTTP/facade DTOs.
- Let infrastructure converters transform PO objects and domain models through MapStruct Plus.
- Keep facade independent from `common`, `application`, `domain`, `infrastructure`, `adapter`, and `starter`.
- Apply named Bean and `@Qualifier` rules across generated Spring Beans.

### Service

Keep the generated project as the existing seven-module pure service sample.

Required optimizations:

- Preserve modules: `common`, `facade`, `domain`, `application`, `infrastructure`, `adapter`, and `starter`.
- Preserve course and examing domains.
- Preserve the pure service constraint: no HTTP controller, no Web filter, no WebFlux, no GraphQL, no Web VO.
- Replace technology-neutral RPC wording and implementation with Dubbo3 Triple facade providers.
- Keep MQ message consumer examples, but keep concrete MQ broker dependencies out unless already required by existing template scope.
- Change `CourseManage` and `ExamManage` to return `Course` and `ExamResult` domain models.
- Remove `CourseView` and `ExamResultView` if they are no longer used.
- Let adapter converters transform domain models into facade DTOs and MQ response DTOs.
- Let infrastructure converters transform PO objects and domain models through MapStruct Plus.
- Apply named Bean and `@Qualifier` rules across generated Spring Beans.

## Dependency and Build Rules

Each generated project should keep the current Java 21 and Spring Boot 3.5.x baseline.

Add only dependencies directly required for the confirmed rules:

- Lombok for `@RequiredArgsConstructor`.
- MapStruct Plus starter and processor.
- Dubbo Spring Boot starter.
- Annotation processor configuration for Lombok and MapStruct Plus.

Do not introduce unrelated dependencies, frameworks, build plugins, or new module layers.

The generated templates must continue to compile through the archetype integration validation path. If Dubbo dependencies require a version property or dependency-management entry, define it once in the generated root POM for each archetype shape.

## Validation Design

Validation should not start generated applications.

Required validation after implementation:

- Run the relevant archetype integration tests for all three archetypes.
- Run generated project compilation/tests through each archetype's basic project verification.
- Run architecture dependency tests in generated projects.
- Search generated sources to verify no `application.manage` method returns `View`, `DTO`, or `Response`.
- Search generated sources to verify no `application` package imports `facade.dto`, `adapter.dto`, `common.response`, or HTTP response types.
- Search generated sources to verify no Spring Bean is missing an explicit bean name where one is required.
- Search generated sources to verify Spring Bean dependency fields use `@Qualifier`.
- Search generated sources to verify no field `@Autowired` or setter injection is introduced.
- Search generated service archetype output to verify no forbidden Web packages or dependencies appear.
- Search all generated outputs to verify no native grpc-java service implementation or standalone gRPC module is generated.
- Verify `dubbo.protocol.name=tri` or equivalent YAML exists in each generated project.
- Verify MapStruct Plus annotation processing succeeds.

## Design Pattern Consideration

No additional design pattern should be introduced for this optimization.

The existing ports-and-adapters shape is enough:

- Application services orchestrate use cases.
- Domain models and domain services keep business behavior.
- Repository ports remain in domain.
- Infrastructure implements repository ports.
- Adapter classes implement external entry points and conversion.

Strategy, Factory Method, Abstract Factory, Builder, Command, Chain of Responsibility, State, Observer, Decorator, and Specification would add structure that the requested detail optimization does not need. The right implementation is direct, explicit, and consistent with the current archetype style.

## Risks

- Adding Dubbo3 Triple to all three archetypes may require dependency management updates in generated POMs. Keep this focused to generated templates.
- Lombok `@Qualifier` constructor propagation depends on `lombok.config`; missing it can make generated applications fail at runtime when multiple beans share a type.
- MapStruct Plus generated mappings can accidentally hide enum/string semantics. Keep semantic conversions explicit.
- The light archetype must not drift into a multi-module shape while adding the facade example.
- The service archetype must not regain Web dependencies while adding Dubbo.

## Completion Criteria

The implementation is complete when:

- All three archetypes preserve their current structure and business sample.
- All generated application services return domain models or simple values.
- External response conversion is performed in adapter converters.
- All generated Spring Beans follow named Bean, `@RequiredArgsConstructor`, final field, and `@Qualifier` injection rules.
- MapStruct Plus is used for adapter and infrastructure conversions.
- All three generated projects include Dubbo3 Triple facade examples.
- No native grpc-java service implementation is generated.
- No generated application is started during validation.
- Relevant archetype and generated-project validation commands pass, or any blocker is reported with the exact command and failure.
