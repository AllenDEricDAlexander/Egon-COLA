# Archetype Mapper and Pagination Follow-up Design

Date: 2026-07-03

## Goal

Apply a focused follow-up optimization to `egon-cola-archetype-light`, `egon-cola-archetype-web`, and `egon-cola-archetype-service`.

This work keeps the previous clean-architecture optimization intact and fixes two details:

- Converter wrapper classes must not inject the generic MapStruct Plus `Converter`.
- Application services must show a `getPage` example that returns `Page<DomainModel>`, and adapters must expose that page through external HTTP or Dubbo contracts.

The work is still a detail-level optimization. It must not redesign the archetypes, rename the business samples, change the light archetype into a multi-module project, or add unrelated architecture packages.

## Confirmed Decisions

- Use approach 1 from brainstorming: keep existing `*Converter` / `*Convertor` wrapper classes, but have them depend only on concrete MapStruct Plus mapper components.
- Do not keep fields like `private final Converter converter;` in generated converter wrappers.
- Prefer standalone mapper interfaces such as `CourseAdapterMapper`, `StudentPoMapper`, `ExamResultAdapterMapper`, or `UserPoMapper`.
- Mapper interfaces use MapStruct Plus-compatible mapper contracts, for example `@Mapper(componentModel = "spring") public interface CourseAdapterMapper extends BaseMapper<Course, CourseDTO>`.
- Wrapper classes stay named Spring Beans and keep `@RequiredArgsConstructor` plus `@Qualifier` injection.
- Application `getPage` methods return `Page<DomainModel>`, not DTOs, responses, or facade objects.
- Adapter layer converts `Page<DomainModel>` to external page DTOs or responses.
- The page example must be present in all three archetypes.
- Empty generated source folders must be avoided. If a directory would otherwise be empty, add a `.gitkeep` file or a package-level `package-info.java`.

## Scope

In scope:

- Converter and mapper templates in adapter and infrastructure packages for all three archetypes.
- Application manage interfaces and implementations for all three archetypes.
- Repository ports and implementations only where needed to support a simple page query.
- External HTTP and Dubbo facade contracts where needed to expose the page example.
- Generated DTOs for external page responses.
- Generated tests and `verify.groovy` guardrails.
- README wording where it must describe the converter and pagination examples accurately.
- Archetype metadata updates when new files are added.

Out of scope:

- Replacing business samples.
- Introducing command/query/usecase packages.
- Adding a full pagination framework.
- Adding sorting, filtering, cursor pagination, search conditions, or database-specific paging abstractions.
- Changing existing module dependency rules.
- Starting generated applications manually.

## Converter Design

Generated converter wrappers remain in their current layer-specific packages:

- `adapter/convertor` converts domain models to HTTP, Dubbo, or MQ-facing DTOs.
- `infrastructure/repo/**/converter` converts persistence objects and domain models.

The wrapper classes keep explicit bean names and constructor injection:

```java
@Component("courseAdapterConvertor")
@RequiredArgsConstructor
public class CourseAdapterConvertor {

    @Qualifier("courseAdapterMapperImpl")
    private final CourseAdapterMapper courseAdapterMapper;

    public CourseDTO toDto(Course course) {
        CourseDTO dto = courseAdapterMapper.convert(course);
        dto.setStatus(course.getStatus().name());
        return dto;
    }
}
```

The mapper is a standalone MapStruct Plus mapper:

```java
@Mapper(componentModel = "spring")
public interface CourseAdapterMapper extends BaseMapper<Course, CourseDTO> {

    @Override
    @Mapping(target = "status", ignore = true)
    CourseDTO convert(Course course);
}
```

Rules:

- No generated converter wrapper may import `io.github.linpeilie.Converter`.
- No generated converter wrapper may declare `private final Converter converter`.
- Mapper interfaces may import MapStruct Plus mapper contracts such as `BaseMapper`.
- The wrapper may keep explicit Java logic for enum/string values, child-id lists, timestamps, and object restoration.
- The wrapper must not reimplement straight field mapping that belongs in the mapper.
- Mapper interfaces should be top-level files so generated Spring mapper beans have predictable names and the templates remain easy to inspect.

## Pagination Model

Add a small internal page model that application services can return:

```java
public record Page<T>(
        List<T> records,
        int currentPage,
        int totalPages,
        int pageSize,
        long totalCount
) {
}
```

The user phrase "当前页，总页数，页大小，页数量" is interpreted as:

- `currentPage`: current page number.
- `totalPages`: total page count.
- `pageSize`: requested page size.
- `totalCount`: total record count.

`records` is required because the return type is `Page<DomainModel>`.

Placement:

- In the light archetype, place it under the generated internal model area, for example `${package}.domain.common.Page`.
- In the web and service archetypes, place it in the `domain` module, for example `${package}.domain.common.Page`, so the existing `application -> domain` dependency remains unchanged.

This page type is an internal application/domain-facing model. It is not a facade DTO and not an HTTP response object.

## Application Page Examples

Each archetype adds at least one page example in the application layer:

- Light: `StudentManage#getPage(int currentPage, int pageSize)` returns `Page<Student>`.
- Web: `UserManage#getPage(int currentPage, int pageSize)` returns `Page<User>`.
- Service: `CourseManage#getPage(int currentPage, int pageSize)` returns `Page<Course>`.

Implementations delegate to repository ports. Repository ports may expose matching simple page methods, returning the same internal `Page<DomainModel>`.

The sample should stay intentionally small:

- Page number and size are simple integer parameters.
- Use ordinary offset/pageable support available in the current persistence stack.
- Validate only basic boundaries needed by the existing sample style.
- Do not add filters, sort options, cursor pagination, or query DTOs.

## Adapter Page Exposure

The page example must be externally visible.

Light archetype:

- HTTP controller exposes a page endpoint for students.
- Dubbo facade exposes a page method for students.
- Adapter converts `Page<Student>` to a page response containing `StudentDTO` records and page metadata.

Web archetype:

- HTTP controller exposes a page endpoint for users.
- Dubbo facade exposes a page method for users.
- Adapter converts `Page<User>` to a page response containing `UserDTO` records and page metadata.

Service archetype:

- Dubbo facade exposes a page method for courses.
- No HTTP controller is added.
- Adapter converts `Page<Course>` to a page response containing `CourseDTO` records and page metadata.

External page DTOs should live in facade or adapter DTO packages, not in application. The facade module should stay independent from application/domain modules; facade page DTOs contain DTO records and primitive metadata only.

## Empty Folder Handling

The implementation must check generated template directories after removing old converter internals, old view folders, or unused packages.

Rules:

- If a directory is no longer needed, remove it from archetype resources and metadata.
- If the directory is intentionally kept as a package placeholder, add `package-info.java`.
- If it is a non-Java placeholder directory, add `.gitkeep`.
- Do not leave empty source folders that disappear from archetype generation or confuse generated project structure.

## Validation Design

Required validation after implementation:

- Run `./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-light,egon-cola-archetypes/egon-cola-archetype-web,egon-cola-archetypes/egon-cola-archetype-service -am clean integration-test`.
- Search generated template sources and generated projects to verify no converter wrapper contains `private final Converter converter`.
- Search generated template sources to verify converter wrappers do not import `io.github.linpeilie.Converter`.
- Verify concrete `@Mapper(componentModel = "spring")` mapper files exist for adapter and infrastructure conversion points.
- Verify application manage interfaces include the required `getPage` methods returning `Page<DomainModel>`.
- Verify external HTTP/Dubbo contracts expose page examples where required.
- Verify adapter conversion owns `Page<DomainModel>` to external page DTO conversion.
- Verify service archetype remains pure service: no controller, web, filter, graphql, vo, `spring-boot-starter-web`, or `spring-boot-starter-webflux`.
- Verify archetype metadata includes all new mapper, page, page DTO, and placeholder files.
- Verify there are no empty generated template directories that should contain `.gitkeep` or `package-info.java`.

## Completion Criteria

The follow-up is complete when:

- All three archetypes no longer generate converter wrappers that inject MapStruct Plus `Converter`.
- All adapter and infrastructure conversions use concrete MapStruct Plus mapper components.
- All three archetypes generate at least one application `getPage` method returning `Page<DomainModel>`.
- The page example is exposed through adapter-facing HTTP and/or Dubbo contracts according to each archetype shape.
- Page metadata includes current page, total pages, page size, and total count.
- Empty-folder handling is covered by removed directories or explicit placeholders.
- The full archetype integration validation passes.
