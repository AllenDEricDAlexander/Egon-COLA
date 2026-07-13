# Web and Service Archetype Domain-First Package Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate the Web and Service archetype templates, generated tests, verification contracts, cross-Project Facade fixtures, and living documentation from technical-first packages to business-domain-first packages without changing behavior or Maven module boundaries.

**Architecture:** Keep the seven existing layer modules and move only domain-owned Java packages so the package order is `<layer>.<domain>.<technical responsibility>`. Preserve genuinely cross-domain roots and the four approved external-client exceptions. Drive every slice by changing the generated-project contract first, then moving sources and tests, updating all package references, and running the smallest archetype integration test before committing.

**Tech Stack:** Java 21, Spring Boot 3.5.x, Maven Wrapper, Maven Archetype Plugin/Invoker, Groovy generated-project verification, JUnit 5, ArchUnit, Spring MVC, Spring GraphQL, Dubbo, RabbitMQ, Spring Data JPA, Flyway.

---

## Execution Constraints

- Execute from an isolated worktree created with `superpowers:using-git-worktrees`.
- Record `IMPLEMENTATION_BASE=$(git rev-parse HEAD)` before Task 1. Final scope checks compare against this commit.
- Restrict implementation changes to:
  - `egon-cola-archetypes/egon-cola-archetype-web`
  - `egon-cola-archetypes/egon-cola-archetype-service`
- Do not modify either archetype's POM, Maven module graph, dependency set, `META-INF/maven/archetype-metadata.xml`, `META-INF/archetype-post-generate.groovy`, or repository CI. Their current wildcard Java file sets already generate moved packages, and repository CI has no old package-path coupling.
- Do not change public routes, GraphQL fields, Dubbo methods, Facade DTO fields, MQ semantics, profiles, error mapping, database schema, or runtime behavior.
- Do not edit or add Flyway migrations. Preserve these SHA-256 digests:
  - Web V1: `c5481736a3ffefc45197a767aec26c1462bb338dfccc1d11751a782ac3de6df1`
  - Web V2: `ebd03c0c4ec3ca5bb3be7095c40ff6e5afc85cca92a4bb5722ca1706456bd3ec`
  - Service V1: `ed5d26a47aef8337b204ab3e77b8d4583fcfc22c3f30cb46fc2055a4429b5df0`
  - Service V2: `c895b9ac63523646214e6932f4e4646f8af7d35bbfd9e14d8152713f26b083c3`
- Do not rewrite historical files under `docs/superpowers/specs` or `docs/superpowers/plans` during implementation.
- Do not start either generated application, Docker, a browser, or an external service.
- Use `apply_patch` for hand-authored content and new files, `git mv`/`git rm` for structural changes, and the explicitly listed bounded Perl loops only for mechanical Java package/import rewrites.
- Commit each task once with path-scoped staging. Do not mix cleanup from later tasks into an earlier commit.

## Design Pattern Decision

The existing Ports and Adapters architecture and the external-Facade Anti-Corruption Layers remain the correct patterns. No new Strategy, Factory, Template Method, inheritance hierarchy, or handler abstraction is introduced: the only variation is deterministic package ownership, so a direct package migration plus exact generated-contract verification is simpler and safer.

## Canonical Package Maps

### Web production map

| Module | Old domain-owned prefix | New prefix |
| --- | --- | --- |
| Facade | `facade/dto/user` | `facade/user/dto` |
| Facade | `facade/dto/teaching` | `facade/teaching/dto` |
| Domain | `domain/{aggregates,entities,enums,events,repos,service,validators,vos}/user` | `domain/user/{aggregates,entities,enums,events,repos,service,validators,vos}` |
| Domain | `domain/{aggregates,entities,enums,events,repos,service,validators,vos}/teaching` | `domain/teaching/{aggregates,entities,enums,events,repos,service,validators,vos}` |
| Domain | `domain/client/user` | `domain/user/client` |
| Domain | `domain/client/teaching` | `domain/teaching/client` |
| Application | `application/{assemblers,command,converter,manage,query,result,validators}/user` | `application/user/{assemblers,command,converter,manage,query,result,validators}` |
| Application | `application/{assemblers,command,converter,manage,query,result,validators}/teaching` | `application/teaching/{assemblers,command,converter,manage,query,result,validators}` |
| Infrastructure | `infrastructure/repo/user` | `infrastructure/user/repo` |
| Infrastructure | `infrastructure/repo/teaching` | `infrastructure/teaching/repo` |
| Infrastructure | User cache implementations in `infrastructure/cache` | `infrastructure/user/cache` |
| Infrastructure | Teaching cache implementations in `infrastructure/cache` | `infrastructure/teaching/cache` |
| Adapter | `adapter/{controller,dto,vo}/user` | `adapter/user/{controller,dto,vo}` |
| Adapter | `adapter/{controller,dto,vo}/teaching` | `adapter/teaching/{controller,dto,vo}` |
| Adapter | `adapter/facade/impl/user` | `adapter/user/facade/impl` |
| Adapter | `adapter/facade/impl/teaching` | `adapter/teaching/facade/impl` |
| Adapter | `adapter/mq/user` | `adapter/user/mq` |
| Adapter | `adapter/mq/teaching` | `adapter/teaching/mq` |
| Adapter | Domain-owned root converter, RPC, and GraphQL classes | `adapter/{user,teaching}/{converter,rpc,graphql}` |

Web shared roots remain `domain/client`, `domain/client/evaluation`, `domain/events`, `domain/exceptions`, `domain/validators`, `application/config`, `application/context`, `application/exceptions`, `application/support`, `infrastructure/client/evaluation`, `infrastructure/cache`, `infrastructure/mq`, `infrastructure/aop`, `infrastructure/config`, `adapter/facade/impl`, `adapter/mq`, `adapter/graphql`, `adapter/filter`, and `adapter/handler`.

### Service production map

| Module | Old domain-owned prefix | New prefix |
| --- | --- | --- |
| Facade | `facade/api/CourseFacade` | `facade/course/CourseFacade` |
| Facade | `facade/api/{ExamFacade,ScoreFacade}` | `facade/exam/{ExamFacade,ScoreFacade}` |
| Facade | `facade/dto/course` | `facade/course/dto` |
| Facade | `facade/dto/exam` | `facade/exam/dto` |
| Domain | `domain/{aggregates,entities,enums,event,repos,service,validators,vos}/course` | `domain/course/{aggregates,entities,enums,event,repos,service,validators,vos}` |
| Domain | `domain/{aggregates,entities,enums,event,repos,service,validators,vos}/exam` | `domain/exam/{aggregates,entities,enums,event,repos,service,validators,vos}` |
| Application | `application/{command,converter,manage,query,result,validators}/course` | `application/course/{command,converter,manage,query,result,validators}` |
| Application | `application/{command,converter,manage,query,result,validators}/exam` | `application/exam/{command,converter,manage,query,result,validators}` |
| Infrastructure | `infrastructure/repo/course` | `infrastructure/course/repo` |
| Infrastructure | `infrastructure/repo/exam` | `infrastructure/exam/repo` |
| Infrastructure | `infrastructure/mq/course` and Course message | `infrastructure/course/mq` and `infrastructure/course/mq/message` |
| Infrastructure | `infrastructure/mq/exam` and Exam/Score messages | `infrastructure/exam/mq` and `infrastructure/exam/mq/message` |
| Adapter | `adapter/facade/impl/course` | `adapter/course/facade/impl` |
| Adapter | `adapter/facade/impl/exam` | `adapter/exam/facade/impl` |
| Adapter | `adapter/{converter,validators}/course` | `adapter/course/{converter,validators}` |
| Adapter | `adapter/{converter,validators}/exam` | `adapter/exam/{converter,validators}` |
| Adapter | `adapter/dto/exam` | `adapter/exam/dto` |
| Adapter | `adapter/mq/exam` | `adapter/exam/mq` |

Service shared roots remain `facade/dto` for `Response`, `SingleResponse`, and `PageResponse`; `facade/enums`, `facade/exceptions`, `facade/utils`; `domain/client`, `domain/client/organization`, `domain/common`; `application/config`, `application/exceptions`, `application/result` for `PageResult`; `infrastructure/client/organization`, `infrastructure/config`, `infrastructure/aop`, `infrastructure/validators`; and `adapter/handler`. No Controller, Web, Filter, GraphQL, or VO package is created.

## Cross-Project Contract Ripple

The Facade package moves are public generated-source contract changes, so the opposite archetype's compile-time fixture must move in the same commit:

- Web Facade DTO moves require Service fixture DTO moves under `egon-cola-archetype-service/src/test/java/fixture/organization/facade/{user,teaching}/dto` and Service Infrastructure imports to follow them.
- Service Facade interface/DTO moves require Web fixture moves under `egon-cola-archetype-web/src/test/java/fixture/evaluation/facade/{course,exam}` and Web Infrastructure imports to follow them.
- External Facade imports remain confined to `infrastructure/client/evaluation` or `infrastructure/client/organization`; only their provider-side package names change.

### Task 1: Move the Web Facade DTO Contract and Organization Fixture

**Files:**
- Modify/move: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/main/java/facade/dto/{user,teaching}`
- Modify: Web template Java files importing `facade.dto.user` or `facade.dto.teaching`
- Modify/move: `egon-cola-archetypes/egon-cola-archetype-service/src/test/java/fixture/organization/facade/dto/{user,teaching}`
- Modify: Service template and fixture Java files importing Organization Facade DTOs
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Change the Web generated-file contract before moving sources**

In `verify.groovy`, replace every required path segment `/facade/dto/user/` with `/facade/user/dto/`, replace `/facade/dto/teaching/` with `/facade/teaching/dto/`, remove the obsolete root `facade/dto/package-info.java` requirement, and add these exact absence checks:

```groovy
[
    "student-management-organization-facade/src/main/java/it/pkg/facade/dto/user",
    "student-management-organization-facade/src/main/java/it/pkg/facade/dto/teaching",
    "student-management-organization-facade/src/main/java/it/pkg/facade/dto/package-info.java"
].each { assertMissing(it) }
assertFile("student-management-organization-facade/src/main/java/it/pkg/facade/user/dto/CreateUserDTO.java")
assertFile("student-management-organization-facade/src/main/java/it/pkg/facade/teaching/dto/SchoolClassDetailDTO.java")
```

- [ ] **Step 2: Run the Web archetype integration test and verify the contract fails**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-web -am clean integration-test
```

Expected: FAIL in `verify.groovy` because `facade/user/dto/CreateUserDTO.java` does not yet exist.

- [ ] **Step 3: Move both Web DTO trees and the matching Service fixture DTOs**

Run these structural moves from the repository root:

```bash
WEB_FACADE=egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/main/java/facade
SERVICE_FIXTURE=egon-cola-archetypes/egon-cola-archetype-service/src/test/java/fixture/organization/facade
mkdir -p "$WEB_FACADE/user" "$WEB_FACADE/teaching" "$SERVICE_FIXTURE/user" "$SERVICE_FIXTURE/teaching"
git mv "$WEB_FACADE/dto/user" "$WEB_FACADE/user/dto"
git mv "$WEB_FACADE/dto/teaching" "$WEB_FACADE/teaching/dto"
git rm "$WEB_FACADE/dto/package-info.java"
git mv "$SERVICE_FIXTURE/dto/user" "$SERVICE_FIXTURE/user/dto"
git mv "$SERVICE_FIXTURE/dto/teaching" "$SERVICE_FIXTURE/teaching/dto"
```

- [ ] **Step 4: Rewrite the exact Java package and import prefixes**

Apply this mechanical rewrite only to Java files under the two archetypes:

```bash
find egon-cola-archetypes/egon-cola-archetype-web egon-cola-archetypes/egon-cola-archetype-service -type f -name '*.java' -print0 |
while IFS= read -r -d '' file; do
  perl -pi -e 's/\.facade\.dto\.user(?=[.;])/.facade.user.dto/g; s/\.facade\.dto\.teaching(?=[.;])/.facade.teaching.dto/g' "$file"
done
```

Inspect the diff and confirm Facade interface locations stay `facade.user` and `facade.teaching`, and the Service external client remains under `infrastructure.client.organization`.

- [ ] **Step 5: Run both archetype integration tests**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-web -am clean integration-test
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-service -am clean integration-test
```

Expected: both commands exit 0; the Web manifest accepts `facade/{user,teaching}/dto`, and the Service fixture-backed Organization client compiles against the moved DTO packages.

- [ ] **Step 6: Commit the cross-Project Facade DTO contract**

```bash
git add egon-cola-archetypes/egon-cola-archetype-web egon-cola-archetypes/egon-cola-archetype-service
git commit -m "refactor: organize web facade DTOs by domain"
```

### Task 2: Move the Web Domain Module by Business Domain

**Files:**
- Modify/move: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain`
- Modify: Web template tests and Java imports referencing moved Domain packages
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Change the Domain paths in the fixed Web manifest and add old-root checks**

Mechanically transform every `requiredFiles` and representative assertion using this complete mapping:

```text
domain/aggregates/{user,teaching} -> domain/{user,teaching}/aggregates
domain/client/{user,teaching} -> domain/{user,teaching}/client
domain/entities/{user,teaching} -> domain/{user,teaching}/entities
domain/enums/{user,teaching} -> domain/{user,teaching}/enums
domain/events/{user,teaching} -> domain/{user,teaching}/events
domain/repos/{user,teaching} -> domain/{user,teaching}/repos
domain/service/{user,teaching} -> domain/{user,teaching}/service
domain/validators/{user,teaching} -> domain/{user,teaching}/validators
domain/vos/{user,teaching} -> domain/{user,teaching}/vos
```

Add this exact contract after `assertMissing` is defined:

```groovy
def webDomainModule = "student-management-organization-domain/src/main/java/it/pkg/domain"
["user", "teaching"].each { businessDomain ->
    ["aggregates", "client", "entities", "enums", "events", "repos", "service", "validators", "vos"].each { role ->
        assertFile("${webDomainModule}/${businessDomain}/${role}/package-info.java")
        assertMissing("${webDomainModule}/${role}/${businessDomain}")
    }
}
assertFile("${webDomainModule}/client/evaluation/package-info.java")
assertFile("${webDomainModule}/events/OrganizationDomainEvent.java")
```

Remove the old `domain/enums/package-info.java` entry because no shared enum remains there. Keep shared `domain/client/package-info.java`, `domain/events/package-info.java`, `domain/exceptions/package-info.java`, and `domain/validators/package-info.java`.

- [ ] **Step 2: Run the Web archetype integration test and verify it fails**

Run the Web integration command from Task 1.

Expected: FAIL with `Expected file student-management-organization-domain/src/main/java/it/pkg/domain/user/aggregates/package-info.java`.

- [ ] **Step 3: Move all local Domain branches and remove the obsolete enum root**

```bash
DOMAIN=egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain
mkdir -p "$DOMAIN/user" "$DOMAIN/teaching"
for role in aggregates entities enums events repos service validators vos; do
  git mv "$DOMAIN/$role/user" "$DOMAIN/user/$role"
  git mv "$DOMAIN/$role/teaching" "$DOMAIN/teaching/$role"
done
git mv "$DOMAIN/client/user" "$DOMAIN/user/client"
git mv "$DOMAIN/client/teaching" "$DOMAIN/teaching/client"
git rm "$DOMAIN/enums/package-info.java"
```

- [ ] **Step 4: Rewrite all Web Domain package declarations and imports**

Apply these exact dotted-prefix replacements to every Web archetype Java file, including generated tests:

```text
.domain.aggregates.user -> .domain.user.aggregates
.domain.aggregates.teaching -> .domain.teaching.aggregates
.domain.client.user -> .domain.user.client
.domain.client.teaching -> .domain.teaching.client
.domain.entities.user -> .domain.user.entities
.domain.entities.teaching -> .domain.teaching.entities
.domain.enums.user -> .domain.user.enums
.domain.enums.teaching -> .domain.teaching.enums
.domain.events.user -> .domain.user.events
.domain.events.teaching -> .domain.teaching.events
.domain.repos.user -> .domain.user.repos
.domain.repos.teaching -> .domain.teaching.repos
.domain.service.user -> .domain.user.service
.domain.service.teaching -> .domain.teaching.service
.domain.validators.user -> .domain.user.validators
.domain.validators.teaching -> .domain.teaching.validators
.domain.vos.user -> .domain.user.vos
.domain.vos.teaching -> .domain.teaching.vos
```

Execute the replacements with this bounded loop, which uses lookahead `(?=[.;])` so package declarations and imports change without altering unrelated prose:

```bash
WEB=egon-cola-archetypes/egon-cola-archetype-web
find "$WEB" -type f -name '*.java' -print0 |
while IFS= read -r -d '' file; do
  for role in aggregates entities enums events repos service validators vos; do
    perl -pi -e "s/\\.domain\\.${role}\\.user(?=[.;])/.domain.user.${role}/g; s/\\.domain\\.${role}\\.teaching(?=[.;])/.domain.teaching.${role}/g" "$file"
  done
  perl -pi -e 's/\.domain\.client\.user(?=[.;])/.domain.user.client/g; s/\.domain\.client\.teaching(?=[.;])/.domain.teaching.client/g' "$file"
done
```

Do not rewrite `.domain.client.evaluation`, `.domain.client`, `.domain.events`, `.domain.exceptions`, or `.domain.validators` shared types.

- [ ] **Step 5: Run the Web integration test and commit**

Run the Web integration command. Expected: exit 0, including Domain unit tests and package-document checks.

```bash
git add egon-cola-archetypes/egon-cola-archetype-web
git commit -m "refactor: organize web domain packages by business domain"
```

### Task 3: Move the Web Application Module by Business Domain

**Files:**
- Modify/move: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application`
- Modify: Web Java imports and Application tests
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Change the Application contract first**

Transform the fixed manifest and representative assertions with this complete map:

```text
application/{assemblers,command,converter,manage,query,result,validators}/user
-> application/user/{assemblers,command,converter,manage,query,result,validators}

application/{assemblers,command,converter,manage,query,result,validators}/teaching
-> application/teaching/{assemblers,command,converter,manage,query,result,validators}
```

Add:

```groovy
def webApplicationModule = "student-management-organization-application/src/main/java/it/pkg/application"
["user", "teaching"].each { businessDomain ->
    ["assemblers", "command", "converter", "manage", "query", "result", "validators"].each { role ->
        assertFile("${webApplicationModule}/${businessDomain}/${role}/package-info.java")
        assertMissing("${webApplicationModule}/${role}/${businessDomain}")
    }
    assertFile("${webApplicationModule}/${businessDomain}/manage/impl/package-info.java")
}
```

- [ ] **Step 2: Run the Web integration test and verify it fails**

Expected: FAIL because `application/user/assemblers/package-info.java` is absent.

- [ ] **Step 3: Move every Application responsibility below its owning domain**

```bash
APPLICATION=egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application
mkdir -p "$APPLICATION/user" "$APPLICATION/teaching"
for role in assemblers command converter manage query result validators; do
  git mv "$APPLICATION/$role/user" "$APPLICATION/user/$role"
  git mv "$APPLICATION/$role/teaching" "$APPLICATION/teaching/$role"
done
```

- [ ] **Step 4: Rewrite all corresponding dotted package prefixes**

Run:

```bash
WEB=egon-cola-archetypes/egon-cola-archetype-web
find "$WEB" -type f -name '*.java' -print0 |
while IFS= read -r -d '' file; do
  for role in assemblers command converter manage query result validators; do
    perl -pi -e "s/\\.application\\.${role}\\.user(?=[.;])/.application.user.${role}/g; s/\\.application\\.${role}\\.teaching(?=[.;])/.application.teaching.${role}/g" "$file"
  done
done
```

Keep `.application.config`, `.application.context`, `.application.exceptions`, and `.application.support` unchanged.

Also update `verify.groovy`'s Application use-case scan from `/application/manage/` to these two accepted path fragments:

```groovy
path.contains("/application/user/manage/") || path.contains("/application/teaching/manage/")
```

- [ ] **Step 5: Run the Web integration test and commit**

Expected: exit 0; existing Application tests remain under `src/test/java/application/{user,teaching}` and compile with new imports.

```bash
git add egon-cola-archetypes/egon-cola-archetype-web
git commit -m "refactor: organize web application packages by domain"
```

### Task 4: Move the Web Infrastructure Repositories, Caches, and Tests

**Files:**
- Modify/move: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/{repo,cache}`
- Modify/move: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/{user,teaching}`
- Modify: Web Java imports and `verify.groovy`

- [ ] **Step 1: Require domain-first repositories, caches, and mirrored tests**

Transform every fixed-manifest entry from `infrastructure/repo/{user,teaching}` to `infrastructure/{user,teaching}/repo`, and from test `infrastructure/{user,teaching}` to `infrastructure/{user,teaching}/repo`. Replace the old root cache assertions with:

```groovy
def webInfrastructureModule = "student-management-organization-infrastructure"
["user", "teaching"].each { businessDomain ->
    assertFile("${webInfrastructureModule}/src/main/java/it/pkg/infrastructure/${businessDomain}/repo/package-info.java")
    assertMissing("${webInfrastructureModule}/src/main/java/it/pkg/infrastructure/repo/${businessDomain}")
    assertFile("${webInfrastructureModule}/src/main/java/it/pkg/infrastructure/${businessDomain}/cache/package-info.java")
    assertFile("${webInfrastructureModule}/src/test/java/it/pkg/infrastructure/${businessDomain}/repo/package-info.java")
}
assertFile("${webInfrastructureModule}/src/main/java/it/pkg/infrastructure/cache/InMemoryCommandIdempotencyAdapter.java")
assertFile("${webInfrastructureModule}/src/main/java/it/pkg/infrastructure/cache/RedisCommandIdempotencyAdapter.java")
assertFile("${webInfrastructureModule}/src/main/java/it/pkg/infrastructure/cache/OrganizationCacheKey.java")
assertMissing("${webInfrastructureModule}/src/main/java/it/pkg/infrastructure/cache/InMemoryUserCache.java")
assertMissing("${webInfrastructureModule}/src/main/java/it/pkg/infrastructure/cache/InMemoryGradeCache.java")
```

- [ ] **Step 2: Run the Web integration test and verify it fails**

Expected: FAIL on `infrastructure/user/repo/package-info.java`.

- [ ] **Step 3: Move repositories, domain caches, and repository tests**

```bash
INFRA=egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src
mkdir -p "$INFRA/main/java/infrastructure/user/cache" "$INFRA/main/java/infrastructure/teaching/cache"
git mv "$INFRA/main/java/infrastructure/repo/user" "$INFRA/main/java/infrastructure/user/repo"
git mv "$INFRA/main/java/infrastructure/repo/teaching" "$INFRA/main/java/infrastructure/teaching/repo"
git mv "$INFRA/main/java/infrastructure/cache/InMemoryUserCache.java" "$INFRA/main/java/infrastructure/user/cache/"
git mv "$INFRA/main/java/infrastructure/cache/RedisUserCache.java" "$INFRA/main/java/infrastructure/user/cache/"
git mv "$INFRA/main/java/infrastructure/cache/InMemoryGradeCache.java" "$INFRA/main/java/infrastructure/teaching/cache/"
git mv "$INFRA/main/java/infrastructure/cache/RedisGradeCache.java" "$INFRA/main/java/infrastructure/teaching/cache/"
git mv "$INFRA/main/java/infrastructure/cache/InMemorySchoolClassCache.java" "$INFRA/main/java/infrastructure/teaching/cache/"
git mv "$INFRA/main/java/infrastructure/cache/RedisSchoolClassCache.java" "$INFRA/main/java/infrastructure/teaching/cache/"
git mv "$INFRA/test/java/infrastructure/user" "$INFRA/test/java/infrastructure/user-repo-tmp"
mkdir -p "$INFRA/test/java/infrastructure/user"
git mv "$INFRA/test/java/infrastructure/user-repo-tmp" "$INFRA/test/java/infrastructure/user/repo"
git mv "$INFRA/test/java/infrastructure/teaching" "$INFRA/test/java/infrastructure/teaching-repo-tmp"
mkdir -p "$INFRA/test/java/infrastructure/teaching"
git mv "$INFRA/test/java/infrastructure/teaching-repo-tmp" "$INFRA/test/java/infrastructure/teaching/repo"
```

Create `package-info.java` in both new cache directories using the existing package documentation style:

```java
/** Domain-owned cache adapters for the user domain. */
package ${package}.infrastructure.user.cache;
```

```java
/** Domain-owned cache adapters for the teaching domain. */
package ${package}.infrastructure.teaching.cache;
```

- [ ] **Step 4: Rewrite exact Infrastructure package prefixes**

Apply these replacements to all Web Java files:

```text
.infrastructure.repo.user -> .infrastructure.user.repo
.infrastructure.repo.teaching -> .infrastructure.teaching.repo
.infrastructure.cache.InMemoryUserCache -> .infrastructure.user.cache.InMemoryUserCache
.infrastructure.cache.RedisUserCache -> .infrastructure.user.cache.RedisUserCache
.infrastructure.cache.InMemoryGradeCache -> .infrastructure.teaching.cache.InMemoryGradeCache
.infrastructure.cache.RedisGradeCache -> .infrastructure.teaching.cache.RedisGradeCache
.infrastructure.cache.InMemorySchoolClassCache -> .infrastructure.teaching.cache.InMemorySchoolClassCache
.infrastructure.cache.RedisSchoolClassCache -> .infrastructure.teaching.cache.RedisSchoolClassCache
```

Change the moved repository test package declarations from `.infrastructure.user`/`.infrastructure.teaching` to `.infrastructure.user.repo`/`.infrastructure.teaching.repo`. Keep the external Evaluation client paths and shared cache/MQ/AOP/config packages unchanged.

- [ ] **Step 5: Run the Web integration test and commit**

Expected: exit 0, including repository, cache, profile, and Flyway tests.

```bash
git add egon-cola-archetypes/egon-cola-archetype-web
git commit -m "refactor: organize web infrastructure packages by domain"
```

### Task 5: Move the Web Adapter Module and Domain-Specific Adapter Tests

**Files:**
- Modify/move: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter`
- Modify/move/create: Web Adapter tests under `__rootArtifactId__-adapter/src/test/java/adapter`
- Modify: `__rootArtifactId__-starter/src/test/java/starter/ArchitectureDependencyTest.java`
- Modify: Web Java imports and `verify.groovy`

- [ ] **Step 1: Change the Adapter generated-file contract**

Apply these exact path transformations to the fixed manifest:

```text
adapter/{controller,dto,vo}/{user,teaching} -> adapter/{user,teaching}/{controller,dto,vo}
adapter/facade/impl/{user,teaching} -> adapter/{user,teaching}/facade/impl
adapter/mq/{user,teaching} -> adapter/{user,teaching}/mq
adapter/converter/{User,Role,Permission}* -> adapter/user/converter/{same file}
adapter/converter/{Grade,SchoolClass}* -> adapter/teaching/converter/{same file}
adapter/rpc/UserRpcProvider -> adapter/user/rpc/UserRpcProvider
adapter/rpc/SchoolClassRpcProvider -> adapter/teaching/rpc/SchoolClassRpcProvider
adapter/graphql/UserResolver -> adapter/user/graphql/UserResolver
adapter/graphql/SchoolClassResolver -> adapter/teaching/graphql/SchoolClassResolver
```

Require domain-specific controller tests at:

```groovy
[
    "student-management-organization-adapter/src/test/java/it/pkg/adapter/user/controller/UserControllerTest.java",
    "student-management-organization-adapter/src/test/java/it/pkg/adapter/user/controller/RolePermissionControllerTest.java",
    "student-management-organization-adapter/src/test/java/it/pkg/adapter/user/controller/package-info.java",
    "student-management-organization-adapter/src/test/java/it/pkg/adapter/teaching/controller/TeachingControllerTest.java",
    "student-management-organization-adapter/src/test/java/it/pkg/adapter/teaching/controller/package-info.java"
].each { assertFile(it) }
```

Add absence checks for every old domain-owned Adapter root, including `adapter/controller/{user,teaching}`, `adapter/dto/{user,teaching}`, `adapter/vo/{user,teaching}`, `adapter/facade/impl/{user,teaching}`, `adapter/mq/{user,teaching}`, the five domain converter classes under `adapter/converter`, and both providers/resolvers under the shared RPC/GraphQL roots.

- [ ] **Step 2: Run the Web integration test and verify it fails**

Expected: FAIL on `adapter/user/controller/UserController.java`.

- [ ] **Step 3: Move directory-owned Adapter branches**

```bash
ADAPTER=egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src
mkdir -p "$ADAPTER/main/java/adapter/user/facade" "$ADAPTER/main/java/adapter/teaching/facade"
for role in controller dto vo; do
  git mv "$ADAPTER/main/java/adapter/$role/user" "$ADAPTER/main/java/adapter/user/$role"
  git mv "$ADAPTER/main/java/adapter/$role/teaching" "$ADAPTER/main/java/adapter/teaching/$role"
done
git mv "$ADAPTER/main/java/adapter/facade/impl/user" "$ADAPTER/main/java/adapter/user/facade/impl"
git mv "$ADAPTER/main/java/adapter/facade/impl/teaching" "$ADAPTER/main/java/adapter/teaching/facade/impl"
git mv "$ADAPTER/main/java/adapter/mq/user" "$ADAPTER/main/java/adapter/user/mq"
git mv "$ADAPTER/main/java/adapter/mq/teaching" "$ADAPTER/main/java/adapter/teaching/mq"
```

- [ ] **Step 4: Move class-owned Adapter responsibilities and remove obsolete package docs**

```bash
ADAPTER_MAIN=egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter
mkdir -p "$ADAPTER_MAIN/user/converter" "$ADAPTER_MAIN/teaching/converter" "$ADAPTER_MAIN/user/rpc" "$ADAPTER_MAIN/teaching/rpc" "$ADAPTER_MAIN/user/graphql" "$ADAPTER_MAIN/teaching/graphql"
git mv "$ADAPTER_MAIN/converter/UserAdapterConverter.java" "$ADAPTER_MAIN/converter/RoleAdapterConverter.java" "$ADAPTER_MAIN/converter/PermissionAdapterConverter.java" "$ADAPTER_MAIN/user/converter/"
git mv "$ADAPTER_MAIN/converter/GradeAdapterConverter.java" "$ADAPTER_MAIN/converter/SchoolClassAdapterConverter.java" "$ADAPTER_MAIN/teaching/converter/"
git mv "$ADAPTER_MAIN/rpc/UserRpcProvider.java" "$ADAPTER_MAIN/user/rpc/"
git mv "$ADAPTER_MAIN/rpc/SchoolClassRpcProvider.java" "$ADAPTER_MAIN/teaching/rpc/"
git mv "$ADAPTER_MAIN/graphql/UserResolver.java" "$ADAPTER_MAIN/user/graphql/"
git mv "$ADAPTER_MAIN/graphql/SchoolClassResolver.java" "$ADAPTER_MAIN/teaching/graphql/"
git rm "$ADAPTER_MAIN/converter/package-info.java" "$ADAPTER_MAIN/rpc/package-info.java"
```

Create the six package-doc files with exactly these Javadocs and declarations:

```java
// adapter/user/converter/package-info.java
/** Inbound and boundary converters owned by the user domain. */
package ${package}.adapter.user.converter;

// adapter/user/rpc/package-info.java
/** RPC providers owned by the user domain. */
package ${package}.adapter.user.rpc;

// adapter/user/graphql/package-info.java
/** GraphQL resolvers owned by the user domain. */
package ${package}.adapter.user.graphql;

// adapter/teaching/converter/package-info.java
/** Inbound and boundary converters owned by the teaching domain. */
package ${package}.adapter.teaching.converter;

// adapter/teaching/rpc/package-info.java
/** RPC providers owned by the teaching domain. */
package ${package}.adapter.teaching.rpc;

// adapter/teaching/graphql/package-info.java
/** GraphQL resolvers owned by the teaching domain. */
package ${package}.adapter.teaching.graphql;
```

Keep `adapter.graphql` because it owns `OrganizationGraphQlContextInterceptor`; keep `adapter.mq` because it owns `OrganizationMessageSupport` and `RetryableOrganizationMessageException`; keep `adapter.facade.impl` because it owns `OrganizationFacadeSupport`.

- [ ] **Step 5: Move only domain-specific Adapter tests and add test package docs**

```bash
ADAPTER_TEST=egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/test/java/adapter
mkdir -p "$ADAPTER_TEST/user/controller" "$ADAPTER_TEST/teaching/controller"
git mv "$ADAPTER_TEST/UserControllerTest.java" "$ADAPTER_TEST/RolePermissionControllerTest.java" "$ADAPTER_TEST/user/controller/"
git mv "$ADAPTER_TEST/TeachingControllerTest.java" "$ADAPTER_TEST/teaching/controller/"
```

Create:

```java
/** Tests for user HTTP adapter behavior. */
package ${package}.adapter.user.controller;
```

and:

```java
/** Tests for teaching HTTP adapter behavior. */
package ${package}.adapter.teaching.controller;
```

Keep cross-domain Dubbo, GraphQL, filter, MQ, and error-handling tests in `src/test/java/adapter`, which retains its existing `package-info.java`.

- [ ] **Step 6: Rewrite packages/imports and the two hard-coded ArchUnit package scopes**

Apply these exact package/import replacements across all Web Java files:

```text
.adapter.controller.user -> .adapter.user.controller
.adapter.controller.teaching -> .adapter.teaching.controller
.adapter.dto.user -> .adapter.user.dto
.adapter.dto.teaching -> .adapter.teaching.dto
.adapter.vo.user -> .adapter.user.vo
.adapter.vo.teaching -> .adapter.teaching.vo
.adapter.facade.impl.user -> .adapter.user.facade.impl
.adapter.facade.impl.teaching -> .adapter.teaching.facade.impl
.adapter.mq.user -> .adapter.user.mq
.adapter.mq.teaching -> .adapter.teaching.mq
.adapter.converter.UserAdapterConverter -> .adapter.user.converter.UserAdapterConverter
.adapter.converter.RoleAdapterConverter -> .adapter.user.converter.RoleAdapterConverter
.adapter.converter.PermissionAdapterConverter -> .adapter.user.converter.PermissionAdapterConverter
.adapter.converter.GradeAdapterConverter -> .adapter.teaching.converter.GradeAdapterConverter
.adapter.converter.SchoolClassAdapterConverter -> .adapter.teaching.converter.SchoolClassAdapterConverter
.adapter.rpc.UserRpcProvider -> .adapter.user.rpc.UserRpcProvider
.adapter.rpc.SchoolClassRpcProvider -> .adapter.teaching.rpc.SchoolClassRpcProvider
.adapter.graphql.UserResolver -> .adapter.user.graphql.UserResolver
.adapter.graphql.SchoolClassResolver -> .adapter.teaching.graphql.SchoolClassResolver
```

In `ArchitectureDependencyTest.java`, replace:

```java
"${package}.adapter.facade.impl.user..",
"${package}.adapter.facade.impl.teaching.."
```

with:

```java
"${package}.adapter.user.facade.impl..",
"${package}.adapter.teaching.facade.impl.."
```

Change moved test package declarations to `.adapter.user.controller` and `.adapter.teaching.controller`.

- [ ] **Step 7: Run the Web integration test and commit**

Expected: exit 0, including controller, GraphQL, Dubbo, MQ, and ArchUnit tests.

```bash
git add egon-cola-archetypes/egon-cola-archetype-web
git commit -m "refactor: organize web adapter packages by domain"
```

### Task 6: Harden the Web Contract and Update Living Documentation

**Files:**
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/README.md`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/multi-project-multi-module-architecture.md`

- [ ] **Step 1: Add failing README and stale-layout assertions**

Add these README assertions before editing the README:

```groovy
[
    "facade/user/dto",
    "domain/user/entities",
    "application/teaching/manage",
    "infrastructure/user/repo",
    "adapter/teaching/controller"
].each { assert readme.contains(it) }
assert !readme.contains("adapter/controller/user")
assert !readme.contains("application/manage/teaching")
```

Add an exact stale-path scan over generated Java paths and contents:

```groovy
def forbiddenWebPathFragments = [
    "/facade/dto/user/", "/facade/dto/teaching/",
    "/domain/aggregates/user/", "/domain/aggregates/teaching/",
    "/domain/client/user/", "/domain/client/teaching/",
    "/domain/entities/user/", "/domain/entities/teaching/",
    "/domain/enums/user/", "/domain/enums/teaching/",
    "/domain/events/user/", "/domain/events/teaching/",
    "/domain/repos/user/", "/domain/repos/teaching/",
    "/domain/service/user/", "/domain/service/teaching/",
    "/domain/validators/user/", "/domain/validators/teaching/",
    "/domain/vos/user/", "/domain/vos/teaching/",
    "/application/command/user/", "/application/command/teaching/",
    "/application/converter/user/", "/application/converter/teaching/",
    "/application/manage/user/", "/application/manage/teaching/",
    "/application/query/user/", "/application/query/teaching/",
    "/application/result/user/", "/application/result/teaching/",
    "/application/validators/user/", "/application/validators/teaching/",
    "/application/assemblers/user/", "/application/assemblers/teaching/",
    "/infrastructure/repo/user/", "/infrastructure/repo/teaching/",
    "/adapter/controller/user/", "/adapter/controller/teaching/",
    "/adapter/dto/user/", "/adapter/dto/teaching/",
    "/adapter/vo/user/", "/adapter/vo/teaching/",
    "/adapter/facade/impl/user/", "/adapter/facade/impl/teaching/",
    "/adapter/mq/user/", "/adapter/mq/teaching/"
]
def staleWebPaths = generatedJavaFiles.collect(relativePath).findAll { path ->
    forbiddenWebPathFragments.any { path.contains(it) }
}
assert staleWebPaths.isEmpty(): "Unexpected technical-first Web paths: ${staleWebPaths.join(', ')}"
```

The list deliberately does not reject `domain/client/evaluation` or `infrastructure/client/evaluation`.

- [ ] **Step 2: Run the Web integration test and verify the README contract fails**

Expected: FAIL because the generated README does not yet contain `facade/user/dto`.

- [ ] **Step 3: Add the canonical Web package-layout section to the generated README**

Add this content after the seven-module list:

````markdown
## Domain-first package layout

Business-owned code puts the domain before the technical responsibility:

```text
facade/user/dto
domain/user/entities
application/teaching/manage
infrastructure/user/repo
adapter/teaching/controller
```

Shared runtime concerns stay at their layer root. The external Evaluation boundary is the deliberate exception at `domain/client/evaluation` and `infrastructure/client/evaluation`.
````

- [ ] **Step 4: Align all package-layout sections in the Web living architecture document**

Update the generic module structures in sections 3.2 through 3.6 and both concrete Project trees in sections 4.2 and 4.3. Section 4.2 must show the Web `user`/`teaching` map from Tasks 1–5. Section 4.3 must show the Service `course`/`exam` map from Tasks 7–11 and must not show a Controller, Web Filter, GraphQL, or VO package. In particular:

```text
organization: user/teaching before controller, dto, converter, manage, entities, repo, cache, rpc, graphql, and mq
evaluation: course/exam before facade DTO/API, manage, entities, repo, mq, converter, validators, and facade impl
```

Spell every converter package `converter`. Describe `domain/client/evaluation`, `infrastructure/client/evaluation`, `domain/client/organization`, and `infrastructure/client/organization` as the only technical-first external-client exceptions. Remove examples that place local domains after technical responsibilities; do not change module dependency or runtime-flow prose.

- [ ] **Step 5: Run the Web integration test and a focused documentation audit**

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-web -am clean integration-test
rg -n 'adapter/controller/(user|teaching)|application/(manage|command|query|result|converter)/(user|teaching)|domain/(entities|aggregates|repos|service)/(user|teaching)|infrastructure/repo/(user|teaching)|facade/dto/(user|teaching)|convertor' \
  egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/README.md \
  egon-cola-archetypes/egon-cola-archetype-web/multi-project-multi-module-architecture.md
```

Expected: Maven exits 0; `rg` exits 1 with no forbidden local layout or `convertor` matches.

- [ ] **Step 6: Commit the Web contract and living docs**

```bash
git add egon-cola-archetypes/egon-cola-archetype-web
git commit -m "docs: align web archetype domain-first package contract"
```

### Task 7: Move the Service Facade Contract and Evaluation Fixture

**Files:**
- Modify/move: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/main/java/facade/{api,dto}`
- Modify: Service Java files importing local Facade types
- Modify/move: `egon-cola-archetypes/egon-cola-archetype-web/src/test/java/fixture/evaluation/facade/{api,dto}`
- Modify: Web template and fixture Java imports for Evaluation Facade types
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Change the Service Facade verifier contract first**

Replace `facade/api` and domain DTO package requirements with:

```groovy
[
    "facade/course",
    "facade/course/dto",
    "facade/exam",
    "facade/exam/dto"
]
```

Keep the shared `facade/dto` requirement. Update representative required files to these exact paths and add old-path absence checks:

```groovy
[
    "student-management-evaluation-facade/src/main/java/it/pkg/facade/course/CourseFacade.java",
    "student-management-evaluation-facade/src/main/java/it/pkg/facade/course/dto/CourseResponse.java",
    "student-management-evaluation-facade/src/main/java/it/pkg/facade/exam/ExamFacade.java",
    "student-management-evaluation-facade/src/main/java/it/pkg/facade/exam/ScoreFacade.java",
    "student-management-evaluation-facade/src/main/java/it/pkg/facade/exam/dto/ScoreResponse.java"
].each { assertFile(it) }
[
    "student-management-evaluation-facade/src/main/java/it/pkg/facade/api",
    "student-management-evaluation-facade/src/main/java/it/pkg/facade/dto/course",
    "student-management-evaluation-facade/src/main/java/it/pkg/facade/dto/exam"
].each { assertMissing(it) }
```

- [ ] **Step 2: Run the Service integration test and verify it fails**

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-service -am clean integration-test
```

Expected: FAIL because `facade/course/CourseFacade.java` is absent.

- [ ] **Step 3: Move Service Facade interfaces/DTOs and Web fixture equivalents**

```bash
SERVICE_FACADE=egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/main/java/facade
WEB_FIXTURE=egon-cola-archetypes/egon-cola-archetype-web/src/test/java/fixture/evaluation/facade
mkdir -p "$SERVICE_FACADE/course" "$SERVICE_FACADE/exam" "$WEB_FIXTURE/course" "$WEB_FIXTURE/exam"
git mv "$SERVICE_FACADE/api/CourseFacade.java" "$SERVICE_FACADE/course/"
git mv "$SERVICE_FACADE/api/ExamFacade.java" "$SERVICE_FACADE/api/ScoreFacade.java" "$SERVICE_FACADE/exam/"
git rm "$SERVICE_FACADE/api/package-info.java"
git mv "$SERVICE_FACADE/dto/course" "$SERVICE_FACADE/course/dto"
git mv "$SERVICE_FACADE/dto/exam" "$SERVICE_FACADE/exam/dto"
git mv "$WEB_FIXTURE/api/CourseFacade.java" "$WEB_FIXTURE/course/"
git mv "$WEB_FIXTURE/api/ExamFacade.java" "$WEB_FIXTURE/api/ScoreFacade.java" "$WEB_FIXTURE/exam/"
rmdir "$WEB_FIXTURE/api"
git mv "$WEB_FIXTURE/dto/course" "$WEB_FIXTURE/course/dto"
git mv "$WEB_FIXTURE/dto/exam" "$WEB_FIXTURE/exam/dto"
```

Keep shared Service wrappers and the fixture `SingleResponse` in `facade/dto`.

- [ ] **Step 4: Rewrite the exact Service/Evaluation Facade package names**

Across Java files in both archetypes, replace:

```text
.facade.api.CourseFacade -> .facade.course.CourseFacade
.facade.api.ExamFacade -> .facade.exam.ExamFacade
.facade.api.ScoreFacade -> .facade.exam.ScoreFacade
.facade.dto.course -> .facade.course.dto
.facade.dto.exam -> .facade.exam.dto
```

The Web provider-import confinement assertion continues to match `fixture.evaluation.facade.` and must still prove that only `infrastructure/client/evaluation` imports provider types.

- [ ] **Step 5: Run both integration tests and commit**

Run Service first, then Web. Expected: both exit 0; Service exports domain-owned Facades directly under `course`/`exam`, and Web's Evaluation client compiles against the moved fixture.

```bash
git add egon-cola-archetypes/egon-cola-archetype-service egon-cola-archetypes/egon-cola-archetype-web
git commit -m "refactor: organize service facade contracts by domain"
```

### Task 8: Move the Service Domain Module by Business Domain

**Files:**
- Modify/move: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain`
- Modify/create: Service Domain tests under `__rootArtifactId__-domain/src/test/java/domain/{course,exam}`
- Modify: Service Java imports and `verify.groovy`

- [ ] **Step 1: Replace the Service Domain package manifest**

Replace the old Domain entries with this exact set:

```groovy
["course", "exam"].each { businessDomain ->
    ["aggregates", "entities", "enums", "event", "repos", "service", "validators", "vos"].each { role ->
        requiredPackagePaths << "domain/${businessDomain}/${role}"
    }
}
```

Keep `domain/client`, `domain/client/organization`, and `domain/common`. Add generated path absence checks for every old `domain/{aggregates,entities,enums,event,repos,service,validators,vos}/{course,exam}` directory.

- [ ] **Step 2: Run the Service integration test and verify it fails**

Expected: FAIL on the first `domain/course/.../package-info.java` requirement.

- [ ] **Step 3: Move all Service Domain branches and remove obsolete technical-root docs**

```bash
DOMAIN=egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain
mkdir -p "$DOMAIN/course" "$DOMAIN/exam"
for role in aggregates entities enums event repos service validators vos; do
  git mv "$DOMAIN/$role/course" "$DOMAIN/course/$role"
  git mv "$DOMAIN/$role/exam" "$DOMAIN/exam/$role"
  git rm "$DOMAIN/$role/package-info.java"
done
```

- [ ] **Step 4: Rewrite Domain package/import prefixes and document Domain tests**

Run:

```bash
SERVICE=egon-cola-archetypes/egon-cola-archetype-service
find "$SERVICE" -type f -name '*.java' -print0 |
while IFS= read -r -d '' file; do
  for role in aggregates entities enums event repos service validators vos; do
    perl -pi -e "s/\\.domain\\.${role}\\.course(?=[.;])/.domain.course.${role}/g; s/\\.domain\\.${role}\\.exam(?=[.;])/.domain.exam.${role}/g" "$file"
  done
done
```

Keep `.domain.client.organization`, `.domain.client`, and `.domain.common` unchanged.

Create these test package docs:

```java
/** Tests for course domain behavior and invariants. */
package ${package}.domain.course;
```

```java
/** Tests for exam and score domain behavior and invariants. */
package ${package}.domain.exam;
```

- [ ] **Step 5: Run the Service integration test and commit**

Expected: exit 0, including Course, Exam, and Score Domain tests.

```bash
git add egon-cola-archetypes/egon-cola-archetype-service
git commit -m "refactor: organize service domain packages by business domain"
```

### Task 9: Move the Service Application Module and Remove Empty Assemblers

**Files:**
- Modify/move: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application`
- Modify/create: Service Application tests under `__rootArtifactId__-application/src/test/java/application/{course,exam}`
- Modify: Service Java imports and `verify.groovy`

- [ ] **Step 1: Change the Service Application package contract**

Replace old domain-owned paths with:

```groovy
["course", "exam"].each { businessDomain ->
    ["command", "converter", "manage", "query", "result", "validators"].each { role ->
        requiredPackagePaths << "application/${businessDomain}/${role}"
    }
    requiredPackagePaths << "application/${businessDomain}/manage/impl"
}
```

Keep shared `application/config`, `application/exceptions`, and `application/result` because `PageResult` remains there. Require `application/assemblers` to be absent; it contains only empty package documentation and has no Service implementation.

- [ ] **Step 2: Run the Service integration test and verify it fails**

Expected: FAIL on `application/course/command/package-info.java`.

- [ ] **Step 3: Move all implemented Application responsibilities and delete empty assemblers**

```bash
APPLICATION=egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application
mkdir -p "$APPLICATION/course" "$APPLICATION/exam"
for role in command converter manage query result validators; do
  git mv "$APPLICATION/$role/course" "$APPLICATION/course/$role"
  git mv "$APPLICATION/$role/exam" "$APPLICATION/exam/$role"
  if [ "$role" != result ]; then git rm "$APPLICATION/$role/package-info.java"; fi
done
git rm -r "$APPLICATION/assemblers"
```

- [ ] **Step 4: Rewrite Application prefixes and add test package docs**

Run:

```bash
SERVICE=egon-cola-archetypes/egon-cola-archetype-service
find "$SERVICE" -type f -name '*.java' -print0 |
while IFS= read -r -d '' file; do
  for role in command converter manage query result validators; do
    perl -pi -e "s/\\.application\\.${role}\\.course(?=[.;])/.application.course.${role}/g; s/\\.application\\.${role}\\.exam(?=[.;])/.application.exam.${role}/g" "$file"
  done
done
```

Change the verifier's Application manager scan from `/application/manage/` to `/application/course/manage/` or `/application/exam/manage/`.

Create:

```java
/** Tests for course application use cases. */
package ${package}.application.course;
```

```java
/** Tests for exam and score application use cases. */
package ${package}.application.exam;
```

- [ ] **Step 5: Run the Service integration test and commit**

Expected: exit 0; Course, Exam, and Score managers compile and their tests pass without an empty assemblers package.

```bash
git add egon-cola-archetypes/egon-cola-archetype-service
git commit -m "refactor: organize service application packages by domain"
```

### Task 10: Move the Service Infrastructure Repositories, MQ Types, and Tests

**Files:**
- Modify/move: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/{repo,mq}`
- Modify/move/create: Service Infrastructure tests
- Modify: Service Java imports and `verify.groovy`

- [ ] **Step 1: Require domain-first persistence and MQ paths**

Replace old requirements with:

```groovy
["course", "exam"].each { businessDomain ->
    requiredPackagePaths.addAll([
        "infrastructure/${businessDomain}/repo",
        "infrastructure/${businessDomain}/repo/impl",
        "infrastructure/${businessDomain}/repo/po",
        "infrastructure/${businessDomain}/repo/jpa",
        "infrastructure/${businessDomain}/repo/converter",
        "infrastructure/${businessDomain}/mq",
        "infrastructure/${businessDomain}/mq/message"
    ])
}
```

Require representative publishers/messages and mirrored tests at:

```text
infrastructure/course/mq/RabbitCourseEventPublisher.java
infrastructure/course/mq/message/CourseScheduledMessage.java
infrastructure/exam/mq/RabbitExamEventPublisher.java
infrastructure/exam/mq/message/ExamPublishedMessage.java
infrastructure/exam/mq/message/ScoreRecordedMessage.java
src/test/java/it/pkg/infrastructure/course/{repo,mq}
src/test/java/it/pkg/infrastructure/exam/{repo,mq}
```

Require old `infrastructure/repo/{course,exam}`, `infrastructure/mq/{course,exam}`, and `infrastructure/mq/message` to be absent.

- [ ] **Step 2: Run the Service integration test and verify it fails**

Expected: FAIL on `infrastructure/course/repo/package-info.java`.

- [ ] **Step 3: Move production repositories, publishers, and messages**

```bash
INFRA=egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src
mkdir -p "$INFRA/main/java/infrastructure/course" "$INFRA/main/java/infrastructure/exam"
git mv "$INFRA/main/java/infrastructure/repo/course" "$INFRA/main/java/infrastructure/course/repo"
git mv "$INFRA/main/java/infrastructure/repo/exam" "$INFRA/main/java/infrastructure/exam/repo"
git mv "$INFRA/main/java/infrastructure/mq/course" "$INFRA/main/java/infrastructure/course/mq"
git mv "$INFRA/main/java/infrastructure/mq/exam" "$INFRA/main/java/infrastructure/exam/mq"
mkdir -p "$INFRA/main/java/infrastructure/course/mq/message" "$INFRA/main/java/infrastructure/exam/mq/message"
git mv "$INFRA/main/java/infrastructure/mq/message/CourseScheduledMessage.java" "$INFRA/main/java/infrastructure/course/mq/message/"
git mv "$INFRA/main/java/infrastructure/mq/message/ExamPublishedMessage.java" "$INFRA/main/java/infrastructure/mq/message/ScoreRecordedMessage.java" "$INFRA/main/java/infrastructure/exam/mq/message/"
git rm "$INFRA/main/java/infrastructure/repo/package-info.java" "$INFRA/main/java/infrastructure/mq/package-info.java" "$INFRA/main/java/infrastructure/mq/message/package-info.java"
```

Create package docs in both new message packages:

```java
/** RabbitMQ transport messages owned by the course domain. */
package ${package}.infrastructure.course.mq.message;
```

```java
/** RabbitMQ transport messages owned by the exam domain. */
package ${package}.infrastructure.exam.mq.message;
```

- [ ] **Step 4: Move domain-specific Infrastructure tests**

```bash
INFRA_TEST=egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure
mkdir -p "$INFRA_TEST/course/repo" "$INFRA_TEST/exam/repo" "$INFRA_TEST/course/mq" "$INFRA_TEST/exam/mq"
git mv "$INFRA_TEST/repo/course/"*.java "$INFRA_TEST/course/repo/"
git mv "$INFRA_TEST/repo/exam/"*.java "$INFRA_TEST/exam/repo/"
rmdir "$INFRA_TEST/repo/course" "$INFRA_TEST/repo/exam" "$INFRA_TEST/repo"
git mv "$INFRA_TEST/mq/RabbitCourseEventPublisherTest.java" "$INFRA_TEST/course/mq/"
git mv "$INFRA_TEST/mq/RabbitExamEventPublisherTest.java" "$INFRA_TEST/exam/mq/"
```

Keep `RabbitMqConfigurationTest` in shared `infrastructure.mq`. Add these exact package docs:

```java
// infrastructure/course/repo/package-info.java
/** Tests for course persistence adapters. */
package ${package}.infrastructure.course.repo;

// infrastructure/exam/repo/package-info.java
/** Tests for exam and score persistence adapters. */
package ${package}.infrastructure.exam.repo;

// infrastructure/course/mq/package-info.java
/** Tests for course event publisher adapters. */
package ${package}.infrastructure.course.mq;

// infrastructure/exam/mq/package-info.java
/** Tests for exam event publisher adapters. */
package ${package}.infrastructure.exam.mq;

// infrastructure/mq/package-info.java
/** Tests for shared RabbitMQ configuration. */
package ${package}.infrastructure.mq;
```

- [ ] **Step 5: Rewrite Infrastructure package names**

Apply:

```text
.infrastructure.repo.course -> .infrastructure.course.repo
.infrastructure.repo.exam -> .infrastructure.exam.repo
.infrastructure.mq.course -> .infrastructure.course.mq
.infrastructure.mq.exam -> .infrastructure.exam.mq
.infrastructure.mq.message.CourseScheduledMessage -> .infrastructure.course.mq.message.CourseScheduledMessage
.infrastructure.mq.message.ExamPublishedMessage -> .infrastructure.exam.mq.message.ExamPublishedMessage
.infrastructure.mq.message.ScoreRecordedMessage -> .infrastructure.exam.mq.message.ScoreRecordedMessage
```

Change the moved test packages to `.infrastructure.course.repo`, `.infrastructure.exam.repo`, `.infrastructure.course.mq`, and `.infrastructure.exam.mq`. Keep `infrastructure.client.organization`, `infrastructure.config`, `infrastructure.aop`, and `infrastructure.validators` unchanged.

- [ ] **Step 6: Run the Service integration test and commit**

Expected: exit 0, including repository, RabbitMQ, configuration, Organization client, and Flyway tests.

```bash
git add egon-cola-archetypes/egon-cola-archetype-service
git commit -m "refactor: organize service infrastructure packages by domain"
```

### Task 11: Move the Service Adapter Module and Tests

**Files:**
- Modify/move: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter`
- Modify/move/create: Service Adapter tests
- Modify: Service Java imports and `verify.groovy`

- [ ] **Step 1: Change the Service Adapter contract and reinforce the service-only boundary**

Replace Adapter required paths with:

```groovy
[
    "adapter/course/facade/impl",
    "adapter/course/converter",
    "adapter/course/validators",
    "adapter/exam/facade/impl",
    "adapter/exam/dto",
    "adapter/exam/converter",
    "adapter/exam/mq",
    "adapter/exam/validators",
    "adapter/handler"
]
```

Require domain-specific tests under `adapter/course/facade/impl`, `adapter/exam/facade/impl`, and `adapter/exam/mq`; keep the cross-domain Dubbo Triple integration test under `adapter/rpc`. Extend the existing forbidden segment list unchanged:

```groovy
def forbiddenSegments = ["controller", "web", "filter", "graphql", "vo"]
```

Add absence checks for old `adapter/facade/impl/{course,exam}`, `adapter/converter/{course,exam}`, `adapter/validators/{course,exam}`, `adapter/dto/{course,exam}`, and `adapter/mq/{course,exam}` paths.

- [ ] **Step 2: Run the Service integration test and verify it fails**

Expected: FAIL on `adapter/course/facade/impl/package-info.java`.

- [ ] **Step 3: Move implemented Adapter branches and delete empty symmetric packages**

```bash
ADAPTER=egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter
mkdir -p "$ADAPTER/course/facade" "$ADAPTER/exam/facade"
git mv "$ADAPTER/facade/impl/course" "$ADAPTER/course/facade/impl"
git mv "$ADAPTER/facade/impl/exam" "$ADAPTER/exam/facade/impl"
git mv "$ADAPTER/converter/course" "$ADAPTER/course/converter"
git mv "$ADAPTER/converter/exam" "$ADAPTER/exam/converter"
git mv "$ADAPTER/validators/course" "$ADAPTER/course/validators"
git mv "$ADAPTER/validators/exam" "$ADAPTER/exam/validators"
git mv "$ADAPTER/dto/exam" "$ADAPTER/exam/dto"
git mv "$ADAPTER/mq/exam" "$ADAPTER/exam/mq"
git rm -r "$ADAPTER/dto/course" "$ADAPTER/mq/course"
git rm "$ADAPTER/converter/package-info.java" "$ADAPTER/dto/package-info.java" "$ADAPTER/facade/package-info.java" "$ADAPTER/facade/impl/package-info.java" "$ADAPTER/mq/package-info.java" "$ADAPTER/validators/package-info.java"
```

- [ ] **Step 4: Move Adapter tests and add package documentation**

```bash
ADAPTER_TEST=egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/test/java/adapter
mkdir -p "$ADAPTER_TEST/course/facade/impl" "$ADAPTER_TEST/exam/facade/impl" "$ADAPTER_TEST/exam/mq"
git mv "$ADAPTER_TEST/facade/impl/CourseFacadeImplTest.java" "$ADAPTER_TEST/course/facade/impl/"
git mv "$ADAPTER_TEST/facade/impl/ExamFacadeImplTest.java" "$ADAPTER_TEST/facade/impl/ScoreFacadeImplTest.java" "$ADAPTER_TEST/exam/facade/impl/"
rmdir "$ADAPTER_TEST/facade/impl" "$ADAPTER_TEST/facade"
git mv "$ADAPTER_TEST/mq/exam/RecordScoreConsumerTest.java" "$ADAPTER_TEST/exam/mq/"
rmdir "$ADAPTER_TEST/mq/exam" "$ADAPTER_TEST/mq"
```

Create package docs for the three new test packages and shared `adapter.rpc`:

```java
/** Tests for course Facade adapter implementations. */
package ${package}.adapter.course.facade.impl;

// adapter/exam/facade/impl/package-info.java
/** Tests for exam and score Facade adapter implementations. */
package ${package}.adapter.exam.facade.impl;

// adapter/exam/mq/package-info.java
/** Tests for exam RabbitMQ consumers. */
package ${package}.adapter.exam.mq;

// adapter/rpc/package-info.java
/** Cross-domain Dubbo Triple integration tests. */
package ${package}.adapter.rpc;
```

- [ ] **Step 5: Rewrite Adapter package declarations and imports**

Apply:

```text
.adapter.facade.impl.course -> .adapter.course.facade.impl
.adapter.facade.impl.exam -> .adapter.exam.facade.impl
.adapter.converter.course -> .adapter.course.converter
.adapter.converter.exam -> .adapter.exam.converter
.adapter.validators.course -> .adapter.course.validators
.adapter.validators.exam -> .adapter.exam.validators
.adapter.dto.exam -> .adapter.exam.dto
.adapter.mq.exam -> .adapter.exam.mq
```

Change moved test package declarations accordingly. Do not create an Adapter Course DTO/MQ package, any Controller, Web, Filter, GraphQL, or VO package.

- [ ] **Step 6: Run the Service integration test and commit**

Expected: exit 0, including Facade implementation, consumer, Dubbo Triple, and architecture tests.

```bash
git add egon-cola-archetypes/egon-cola-archetype-service
git commit -m "refactor: organize service adapter packages by domain"
```

### Task 12: Complete Service Package Documentation, Harden Contracts, and Validate Both Archetypes

**Files:**
- Modify/create: remaining Service test `package-info.java` files
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/README.md`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/student-management-service-only-rpc-mq-architecture.md`

- [ ] **Step 1: Add complete Service package-doc and README contracts before the remaining docs**

Port the Web verifier's `assertPackageDocs` helper into the Service verifier and run it for every generated module's main and test Java roots:

```groovy
def assertPackageDocs = { String sourceRoot ->
    def root = new File(projectDir, sourceRoot)
    assert root.isDirectory(): "Expected directory ${sourceRoot}"
    def javaDirs = [] as Set
    root.traverse(type: FileType.FILES) { file ->
        if (file.name.endsWith(".java") && file.name != "package-info.java") {
            javaDirs << file.parentFile
        }
    }
    javaDirs.each { dir ->
        assert new File(dir, "package-info.java").isFile():
                "Missing package-info.java in ${projectDir.toPath().relativize(dir.toPath())}"
    }
}
modules.each { module ->
    assertPackageDocs("student-management-evaluation-${module}/src/main/java")
    assertPackageDocs("student-management-evaluation-${module}/src/test/java")
}
```

Add README assertions:

```groovy
[
    "facade/course/dto",
    "domain/exam/entities",
    "application/course/manage",
    "infrastructure/exam/repo",
    "adapter/exam/mq"
].each { assert readme.contains(it) }
assert readme.contains("service-only")
assert !readme.contains("facade/api")
assert !readme.contains("application/manage/course")
```

- [ ] **Step 2: Run the Service integration test and verify package docs or README fail**

Expected: FAIL on the first remaining shared test package without `package-info.java`, or on the first missing README token.

- [ ] **Step 3: Add package docs to every unchanged Service test package that still contains Java**

Create `package-info.java` under these exact test paths when not already created in Tasks 8–11:

```text
__rootArtifactId__-facade/src/test/java/facade
__rootArtifactId__-infrastructure/src/test/java/infrastructure/client/organization
__rootArtifactId__-infrastructure/src/test/java/infrastructure/migration
__rootArtifactId__-starter/src/test/java/starter
__rootArtifactId__-starter/src/test/java/starter/config/encryption
```

Create these exact files:

```java
/** Contract tests for the generated Evaluation Facade. */
package ${package}.facade;

// infrastructure/client/organization/package-info.java
/** Tests for the external Organization client anti-corruption layer. */
package ${package}.infrastructure.client.organization;

// infrastructure/migration/package-info.java
/** Tests for Evaluation Flyway migration compatibility. */
package ${package}.infrastructure.migration;

// starter/package-info.java
/** Generated Evaluation application assembly and architecture tests. */
package ${package}.starter;

// starter/config/encryption/package-info.java
/** Tests for generated configuration decryption support. */
package ${package}.starter.config.encryption;
```

- [ ] **Step 4: Add a complete Service stale-layout audit**

Add:

```groovy
def forbiddenServicePathFragments = [
    "/facade/api/", "/facade/dto/course/", "/facade/dto/exam/",
    "/domain/aggregates/course/", "/domain/aggregates/exam/",
    "/domain/entities/course/", "/domain/entities/exam/",
    "/domain/enums/course/", "/domain/enums/exam/",
    "/domain/event/course/", "/domain/event/exam/",
    "/domain/repos/course/", "/domain/repos/exam/",
    "/domain/service/course/", "/domain/service/exam/",
    "/domain/validators/course/", "/domain/validators/exam/",
    "/domain/vos/course/", "/domain/vos/exam/",
    "/application/command/course/", "/application/command/exam/",
    "/application/converter/course/", "/application/converter/exam/",
    "/application/manage/course/", "/application/manage/exam/",
    "/application/query/course/", "/application/query/exam/",
    "/application/result/course/", "/application/result/exam/",
    "/application/validators/course/", "/application/validators/exam/",
    "/infrastructure/repo/course/", "/infrastructure/repo/exam/",
    "/infrastructure/mq/course/", "/infrastructure/mq/exam/",
    "/adapter/facade/impl/course/", "/adapter/facade/impl/exam/",
    "/adapter/converter/course/", "/adapter/converter/exam/",
    "/adapter/validators/course/", "/adapter/validators/exam/",
    "/adapter/dto/exam/", "/adapter/mq/exam/"
]
def staleServicePaths = javaFiles.collect(javaPath).findAll { path ->
    forbiddenServicePathFragments.any { path.contains(it) }
}
assert staleServicePaths.isEmpty(): "Unexpected technical-first Service paths: ${staleServicePaths.join(', ')}"
```

Keep the existing external-import confinement checks and `forbiddenSegments = ["controller", "web", "filter", "graphql", "vo"]`. The stale list deliberately permits `domain/client/organization` and `infrastructure/client/organization`.

- [ ] **Step 5: Add the canonical Service package-layout section to the generated README**

Add after the module summary:

````markdown
## Domain-first package layout

Business-owned code puts the domain before the technical responsibility:

```text
facade/course/dto
domain/exam/entities
application/course/manage
infrastructure/exam/repo
adapter/exam/mq
```

This remains service-only: business traffic enters through Dubbo Triple or RabbitMQ, with no business Controller, Web Filter, GraphQL, or VO package. The external Organization boundary remains at `domain/client/organization` and `infrastructure/client/organization`.
````

- [ ] **Step 6: Align the Service living architecture document**

Update sections 3.2 through 3.6 and concrete structures in sections 4.2 and 4.3. Section 4.2 keeps its `user`/`teaching` example but places each domain before Service responsibilities; section 4.3 uses the `course`/`exam` canonical Service map from Tasks 7–11. Replace `convertor` with `converter`; show Facade interfaces directly under each business domain; and preserve the Service-only rule prohibiting Controller, Web, Filter, GraphQL, and VO packages. Keep dependency directions, RPC/MQ flow, validation responsibilities, and the Organization client Anti-Corruption Layer unchanged.

- [ ] **Step 7: Run both full archetype integration tests**

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-web -am clean integration-test
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-service -am clean integration-test
```

Expected: both commands exit 0. The Invoker-generated Web project runs `verify`; the Invoker-generated Service project runs `test`; both Groovy contracts pass.

- [ ] **Step 8: Run explicit generated-project wrapper verification**

After the integration tests create the generated projects, run:

```bash
WEB_GENERATED=egon-cola-archetypes/egon-cola-archetype-web/target/test-classes/projects/basic/project/student-management-organization
SERVICE_GENERATED=egon-cola-archetypes/egon-cola-archetype-service/target/test-classes/projects/basic/project/student-management-evaluation
bash "$WEB_GENERATED/mvnw" -B -ntp -f "$WEB_GENERATED/pom.xml" clean verify
bash "$SERVICE_GENERATED/mvnw" -B -ntp -f "$SERVICE_GENERATED/pom.xml" clean verify
```

Expected: both commands exit 0 without starting either application or requiring Nacos, Redis, RabbitMQ, PostgreSQL, or another Project at runtime.

- [ ] **Step 9: Audit stale package forms in templates, tests, verifiers, and living docs**

```bash
rg -n 'adapter/(controller|dto|vo)/(user|teaching)|adapter/facade/impl/(user|teaching)|application/(assemblers|command|converter|manage|query|result|validators)/(user|teaching)|domain/(aggregates|client|entities|enums|events|repos|service|validators|vos)/(user|teaching)|infrastructure/(repo|cache)/(user|teaching)|facade/dto/(user|teaching)' egon-cola-archetypes/egon-cola-archetype-web
rg -n 'facade/api|facade/dto/(course|exam)|application/(command|converter|manage|query|result|validators)/(course|exam)|domain/(aggregates|entities|enums|event|repos|service|validators|vos)/(course|exam)|infrastructure/(repo|mq)/(course|exam)|adapter/facade/impl/(course|exam)|adapter/(converter|validators)/(course|exam)|adapter/(dto|mq)/exam|convertor' egon-cola-archetypes/egon-cola-archetype-service
```

Expected: both commands exit 1 with no matches. The patterns do not include the approved external-client exception paths.

- [ ] **Step 10: Verify migrations, metadata, build files, CI, and historical docs are unchanged**

```bash
shasum -a 256 \
  egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/migration/*.sql \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/migration/*.sql
git diff --exit-code "$IMPLEMENTATION_BASE" -- \
  pom.xml egon-cola-archetypes/pom.xml \
  egon-cola-archetypes/egon-cola-archetype-web/pom.xml \
  egon-cola-archetypes/egon-cola-archetype-service/pom.xml \
  egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/META-INF \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/META-INF \
  .github docs/superpowers/specs docs/superpowers/plans
```

Expected: four hashes exactly match the values in Execution Constraints; `git diff --exit-code` exits 0.

- [ ] **Step 11: Review scope and commit the Service contract/docs**

```bash
git status --short
git diff --check
git diff --stat "$IMPLEMENTATION_BASE"
git add egon-cola-archetypes/egon-cola-archetype-service
git commit -m "docs: align service archetype domain-first package contract"
```

Expected: only the two approved archetypes changed since `IMPLEMENTATION_BASE`, the final commit contains the Service verifier/test package docs/README/living architecture document, and the worktree is clean after commit.

## Spec Coverage Map

| Approved requirement | Implemented by |
| --- | --- |
| Web uses `user`/`teaching` before technical responsibility in every layer | Tasks 1–6 |
| Service uses `course`/`exam` before technical responsibility in every implemented layer | Tasks 7–12 |
| Service remains Service-only with no business Controller/Web surface | Task 11 contract and Task 12 final audit |
| Facade interfaces directly under business domains | Existing Web interface layout preserved in Task 1; Service changed in Task 7 |
| `converter` spelling only | Canonical maps, Tasks 3/5/9/11, Tasks 6/12 audits |
| Production, tests, package docs, verifier, and living docs agree | Every module task plus Tasks 6 and 12 |
| Shared code stays only at justified cross-domain roots | Canonical maps and module-specific move tasks |
| Four external-client technical-first exceptions remain | Tasks 1/7 cross-project checks and Tasks 6/12 whitelisted audits |
| Module dependencies and runtime behavior remain unchanged | Execution Constraints, existing verifier dependency guards, Task 12 scope/verification |
| Existing Flyway files unchanged; no new migration | Execution Constraints and Task 12 digest/scope check |
| Historical specs/plans unchanged | Task 12 scope check |
| No application, browser, container, or external service starts | Execution Constraints and wrapper-only verification |

## Completion Checklist

- [ ] All twelve task commits exist and each contains only its declared slice.
- [ ] Web and Service `clean integration-test` commands pass from a clean worktree.
- [ ] Both generated-project `clean verify` commands pass.
- [ ] Exact Maven module dependency guards still pass in both `verify.groovy` files.
- [ ] External Facade imports remain confined to the two Infrastructure client packages.
- [ ] Old local technical-first paths are absent; the four external-client exceptions remain present.
- [ ] All generated Java source and test packages containing classes have useful `package-info.java` files.
- [ ] Service has no Controller, Web, Filter, GraphQL, or VO business package.
- [ ] The four migration hashes match and no migration was created.
- [ ] POMs, metadata, CI, and historical design artifacts are unchanged during implementation.
- [ ] No project, browser, container, or external service was started.
