---
name: egon-coding-writing-plan
description: Use when a coding task has a specific reviewed or accepted specification and needs a repository-grounded, implementation-ready, file-by-file plan with exact dependency order, Java/Spring/Egon-COLA standards, blocking Manual Checks, repository-language pseudocode, validation commands, and per-Step commit scopes before code changes begin.
---

# EGON Coding Plan Writing

## Purpose

Translate one specific coding Spec and the current repository state into an ordered, file-by-file implementation path. Write the Plan under `docs/egon/plan` after design review and before implementation.

The Plan defines **which file is handled first, what is written there, which file follows, and how each step is proven complete**. It implements the Spec; it must not redesign it or start coding.

## Scope and output contract

- Use this skill only for coding work backed by an identifiable Spec.
- Produce or revise Plan documents, Plan relationship metadata, and the target Spec's `Related Plans` metadata only. Do not modify production/test code, execute migrations, or start the project.
- Write the Plan in the language requested by the user. If unspecified, use the target Spec's natural language. Keep repository paths, symbols, code identifiers, schemas, and commands exact.
- Save every new Plan as `docs/egon/plan/YYYY-MM-DD-HH-MM-ABSTRACT.md`.
  - Use the user's/repository's local creation minute.
  - Replace `ABSTRACT` with a concise lowercase ASCII kebab-case summary, normally 3–8 words.
  - Example: `docs/egon/plan/2026-08-15-16-10-account-lockout-implementation.md`.
  - Never overwrite a same-minute/same-abstract document; choose a more specific abstract.
- Start from Plan Template Version 4 in `assets/plan-template.md`. Keep all numbered chapters. Use evidence-backed `N/A` when a chapter does not apply. The validator continues to accept Version 2, Version 3, and existing unversioned Plans under their original contracts.

## Resource-integrity preflight

Resolve the directory containing this `SKILL.md` as `<skill-root>`. Before reading any bundled reference, template, or helper script, run:

```bash
python3 <skill-root>/scripts/validate_skill_resources.py
```

`<skill-root>` is notation, not literal shell text: substitute the resolved absolute directory before executing the command. All bundled paths in this skill are relative to that directory and therefore start with `references/`, `assets/`, or `scripts/`; never resolve a bare filename relative to the repository root or the currently opened reference file. If the preflight reports a missing, escaping, ambiguous, or broken resource, stop before planning, report the exact diagnostic to the user, and repair/reinstall the skill. Do not continue with a partial skill or silently substitute an invented resource.

## User-mandated Java rules — verbatim normative source

The following source rules are preserved exactly. They are mandatory and must be compiled into ordered files, pseudocode, checks, and Step commits. Read `references/user-mandated-java-rules.md` for the non-weakened planning contract.

```text
1 类名规范，必须以 java的pojo规范命名。以dao po bo vo dto query command event等结尾
2 每层之间必须被 springboot-validation 校验，复用的对象 validation 要分组校验，ValidatorUtils使用 libphonenumber进行规范化校验或者validation原生注解，若非必要，不要自己写。
3 实体类规范：复杂对象使用java类并使用Lombok进行@Data\@NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor\@RequiredArgsConstructor\@Builder\@Accessors(chain = true)修饰。简单对象使用Java Record。使用MapStruct、MapStructPlus 进行转换，egon-cola-component-common-core有通用的convertor，必须继承实现这个。如果是不可变对象，使用@Value注释修饰。record场景Record 构造器很适合做数据规范化，推荐使用紧凑构造器，Record 可以作为局部类，在方法内部定义临时数据结构。
4业务类必须使用@Slf4j注解注入log对象。如果业务类被spring管理，必须指定名称，如果是单例的情况下，参考@Service("userService")。如果需要依赖注入，必须@RequiredArgsConstructor进行修饰，不要代码中写。且属性必须被@qualify修饰。
5 工具类只允许使用jdk原生、Apache Commons(commons-lang3、commons-collections4、commons-io、commons-text、commons-codec、commons-beanutils)、Guava。针对Tika按需引入。
6 json 使用SpringBoot-JackSon 对外交互层的实体类必须按需被jackson注解修饰。
7 springboot 多环境配置文件，必须保持配置一致，但值不一定一致。
9 复杂业务必须引入设计模式，不允许硬编码
10 日期相关的必须使用java.time下的实体类，不允许使用java.util下的
11 plan中必须确认代码结构，分层结构或者egon-cola-archetype，只允许这两种代码结构规范。&#x20;
```

Do not translate, renumber, correct, shorten, or paraphrase this block. `references/user-mandated-java-rules.md` resolves literal spellings to exact Java/Spring symbols without relaxing them.

## Non-negotiable rules

1. A Plan must name exactly one primary target in `Implements Spec` using a repository-relative Markdown link. It must also list the complete `Effective Specs` set: the primary Spec plus applicable accepted amendments, normative dependencies, and replacements.
2. Read the primary and effective Specs completely, including metadata relationships, requirements, acceptance criteria, target file tree, interfaces, models, database, frontend, tests, compatibility, rollout, risks, and approval state.
3. Read all applicable `AGENTS.md` files and inspect the current repository at the target baseline. Verify paths, symbols, consumers, build/test commands, migrations, generated sources, and relevant worktree changes rather than copying stale Spec claims.
4. Do not generate a finalized Plan when no target Spec exists, the target is ambiguous, the effective Specs conflict, or an open major decision changes implementation. Use `egon-coding-writing-spec` or ask the user to resolve the target/decision.
5. Do not introduce behavior, public contracts, schema fields, pages, dependencies, architectural layers, or refactors absent from the effective Spec. A major Spec defect or material repository drift must return to the user and the Spec before planning continues.
6. Infer only small, local, reversible implementation details that preserve the effective design and follow one clear repository convention. Record consequential inferences as `Plan Clarification` entries with evidence and impact if wrong.
7. Every implementation Step must be small enough to verify independently and normally map to one semantic commit. Respect repository instructions requiring one commit per task.
8. Every Step must name covered source requirement IDs, dependencies, observable outcome, a strict ordered file list, file operation, symbols, repository-language pseudocode, intermediate result, exact validation, completion criteria, rollback point, and proposed commit message.
9. Plan behavior changes test-first: first write or modify a focused failing unit/contract test, state its expected RED reason, then plan the minimum production change, then refactor/wire/verify. If test-first is technically impossible, treat the exception as a major decision unless the user or repository explicitly authorizes it.
10. Pseudocode must be implementation-bearing but not production code. Use actual class/function/component/table names, signatures, field mappings, branches, calls, state changes, error paths, transactions, and assertions in the repository's language and framework style.
11. Determine file order from real dependencies. Do not mechanically apply a layer list when migration, generated code, contract publication, cross-module compilation, or frontend/backend compatibility requires a different order.
12. Include all applicable migrations, configuration, permissions, observability, documentation, compatibility, rollout, rollback, and release verification files in the ordered steps.
13. Never modify an existing immutable Flyway migration. If the Spec requires one database change, plan exactly one new next-version migration unless the user explicitly approved a different migration decomposition.
14. Review the finished Plan against both the original user requirements recorded by the Spec and the effective Spec design. Fix omissions and inconsistencies before delivery.
15. Do not mark a Plan `Ready` without an explicitly accepted primary Spec and explicit user/decision-owner approval of the Plan. A complete Plan awaiting review is `Review`; a Spec or decision blocker requires `Draft` or `Blocked`.
16. Read `references/file-by-file-planning.md` completely. Every Step must state baseline/end state, test-first applicability, exact ordered files, validation working directory, commit paths, and one semantic outcome. Every file must include current repository evidence, dependencies/consumers, input/output/state mapping, error/edge behavior, implementation-bearing pseudocode, verification contribution, and after-file state.
17. Before planning files, perform a Spec simplicity and implementation-necessity audit. Do not silently compile an unjustified API, parameter-preflight flow, class, layer, table, cache, job, dependency, or page into implementation. When the direct/reuse alternative satisfies the same requirements or the Spec lacks a necessity decision, return `REVISE`/`BLOCKED` with evidence and exact Spec sections; do not redesign inside the Plan.
18. For every Java Plan, read `references/user-mandated-java-rules.md` and `references/java-spring-egon-coding-standards.md` completely. Before assigning files, prove exactly one allowed architecture profile from the current tree and selected Archetype, then build a reuse ledger covering Spring, Spring Boot Starters, Egon-COLA Components/common infrastructure, and module-local candidates. Do not plan a new dependency or custom replacement without a proven capability gap and approved impact.
19. Every Step must list the applicable blocking `MC-*` IDs, and every affected Java file must state its standards impact. The pseudocode and validation must make semantic naming, every layer-handoff Validation/groups/normalization, exact Record/`@Value`/complete complex Lombok modeling, MapStruct/MapStructPlus plus mandatory Egon `BaseConverter`, Bean names/injection/Qualifier propagation, `@Slf4j`, utilities, Jackson, `java.time`, configuration parity, and the mandatory approved pattern for each Complex business flow executable rather than aspirational.
20. Complete every Manual Check row one by one in Chapter 12. An applicable row requires `PASS` and concrete repository/Plan evidence; an inapplicable row requires evidence-backed `N/A`. Any missing ID/evidence, `FAIL`, `BLOCKED`, `UNKNOWN`, or unresolved exception prohibits a PASS verdict.
21. Apply `references/user-mandated-java-rules.md` literally. Chapter 4 must preserve ten independent rows using original numbering `1, 2, 3, 4, 5, 6, 7, 9, 10, 11`; every Step must contain `Literal Rules:` and every affected file `Literal rule enforcement:`. Mandatory wording may not be converted into “prefer/consider/when compatible.” A conflict returns to the Spec/user and blocks PASS.

## Target Spec and effective-design resolution

Read `references/spec-resolution.md` before writing.

If the user provides a Spec path, use it. Otherwise search `docs/egon/spec` and relevant legacy design directories; select a target only when exactly one current document unambiguously governs the request. Ask the user when multiple candidates remain.

Use relative links rather than requiring a special numeric ID. A legacy Spec is eligible only when it provides a sufficiently complete and still-current coding design. If it lacks traceable requirements, contains unresolved decisions, or describes already completed work, stop and explain whether it needs an amending Spec, a residual-work Spec, or verification rather than another Plan.

Resolve the effective design in this order:

1. Start with the primary target Spec.
2. Replace content superseded for the current scope.
3. Apply accepted amendments in chronological order.
4. Include normative dependencies.
5. Preserve the source requirement identifiers (`REQ-*` or an established predecessor scheme). If a source has no identifiers, create Plan-local trace aliases only for existing statements; never invent new requirements.
6. Treat ungoverned conflicts as major blockers.

Record the exact Spec status, update/revision, baseline commit, and links in the Plan header. An explicit user request may authorize a Draft/Review Plan against a non-accepted Spec, but it remains non-Ready and must expose that risk.

## Spec defect, drift, and inference boundary

Return to the Spec/user when a finding changes business behavior, scope, acceptance, public API/RPC/event contracts, model/schema, ownership, dependency direction, permissions/security/tenancy, transactions/consistency/idempotency, technology selection, compatibility, migration, rollout, destructive behavior, or operational cost.

Record a `Plan Clarification` only when the detail is local, reversible, not externally observable beyond an already-decided contract, and directly supported by current code—for example an internal helper name, test fixture placement, or a class rename with identical semantics.

Do not disguise a redesign as a clarification.

Treat overdesign as a material Spec defect when it changes public interaction count, client/server state, contract surface, failure points, operational cost, or implementation scope. In particular, reject a Plan that makes a caller fetch values only to forward them unchanged to a command that can derive or validate them.

## Required planning workflow

1. **Lock the target Spec**
   - Record the primary path, status, revision, relations, approval evidence, and original source request.
2. **Build the effective requirement set**
   - Extract every effective requirement, acceptance criterion, interface, field, state rule, table/page/test requirement, non-functional constraint, migration, and rollout condition.
   - Map each approved design element to its Spec necessity verdict. For legacy Specs, perform the concise audit in `references/file-by-file-planning.md`; escalate material gaps rather than inventing justification.
3. **Inspect the current repository baseline**
   - Verify actual files/symbols and identify already-complete, missing, moved, generated, or conflicting work.
   - Preserve unrelated dirty-worktree changes and plan path-limited commits.
   - For Java work, apply `references/user-mandated-java-rules.md`, identify the exact traditional-layer or selected Archetype COLA profile, and build the capability-reuse ledger required by `references/java-spring-egon-coding-standards.md` before proposing new files or dependencies.
4. **Resolve blockers and clarifications**
   - Ask about major defects/ambiguities; infer and record only small implementation gaps.
5. **Derive the dependency path**
   - Establish compilation, contract, data, migration, runtime, and consumer order.
   - Explicitly identify steps that may run in parallel and steps that must remain sequential, without creating overlapping write scopes.
6. **Write the target file tree**
   - List every Create/Modify/Delete path once with symbols, responsibility, requirement mapping, and owning Step.
7. **Write ordered implementation Steps**
   - For each behavior, place the focused failing test before its production implementation.
   - For each file, write language/framework-specific pseudocode and the state after that file is completed.
   - End each Step with targeted verification, objective completion evidence, rollback, and one proposed commit.
   - Record baseline/end state, test-first gate, validation working directory, and exact commit paths. Use every per-file field required by `references/file-by-file-planning.md`.
   - List the applicable `MC-*` IDs for the Step and the standards impact for every Java file. The Step validation must prove those checks before its proposed commit.
   - List exact original `Rule N` values for the Step and explain each file's literal-rule enforcement. A generic standards paragraph is not sufficient.
8. **Write quality and release gates**
   - Use exact repository commands for focused tests, module tests, static checks, builds, integration/E2E/manual checks, migration validation, and final regression.
9. **Review and repair**
   - Apply `references/review-checklist.md` and reconcile every mismatch with the effective Spec.
   - Execute all Chapter 12 Manual Checks individually; close failures/blockers or change the verdict. Never collapse them into one unsupported assertion.
10. **Validate and deliver**
    - Run `scripts/validate_plan.py <plan-path> --strict`.
    - Report the Plan path/status, target/effective Specs, Step count, clarification entries, blockers, and validation boundary.
    - Stop for user review. Do not begin implementation as a side effect.

## File-order and pseudocode contract

Each Step must use this sequence contract:

1. State requirements, dependencies, baseline state, one observable outcome, exact end state, and whether test-first is required or evidence-backed not applicable.
   State every applicable blocking Manual Check ID for the Step.
   State every applicable original `Rule N`; Rule 11 applies to every Step containing repository files.
2. List files in the exact order an implementer should handle them.
3. For each file, provide:
   - `CREATE`, `MODIFY`, `DELETE`, `RENAME`, or `GENERATED` operation;
   - exact repository-relative path and affected symbols;
   - current repository evidence for placement, symbol, style, and operation;
   - callers, callees, imports/modules, generated sources, and runtime/compile consumers;
   - why this file occurs at this point in the sequence;
   - signatures/contracts/fields that must be added or changed;
   - input/output/field/state mapping, including null/default/enum/time/precision and persistence/side effects;
   - validation, permission, missing, duplicate, concurrent, dependency, rollback, and compatibility behavior as applicable;
   - language-appropriate pseudocode for control flow, mapping, persistence, errors, and tests;
   - applicable Java/Spring/Egon-COLA standards, exact annotations/contracts, and Manual Check IDs;
   - exact original Rule numbers and the concrete non-weakened enforcement for this file;
   - the exact test/gate to which this file contributes;
   - the expected intermediate repository state after this file.
4. State the validation working directory, exact focused verification command, exact success result, and failure return point.
5. State completion evidence, rollback point, exact commit paths, and one semantic commit message.

Good Java pseudocode names annotations, method signatures, collaborators, transaction boundaries, domain calls, mapper/repository operations, exceptions, and assertions. Good TypeScript/React pseudocode names props/types, hooks/state, API calls, render branches, events, and component tests. Good SQL pseudocode names the new migration, DDL/DML, constraints, indexes, backfill, guards, and rollback/forward-fix limits.

Avoid placeholders such as “implement service,” “handle errors,” “update frontend,” or “run tests.” The implementer must not need to invent architecture or file order.

## Required chapters

1. **Summary** — target Spec, scope, implementation direction, and final evidence.
2. **Target Spec and effective design** — exact relative links, status/revisions, relationships, approval, and source requirements.
3. **Effective requirements and acceptance** — all source IDs/statements, exact Spec sections, acceptance, and implementation impact.
4. **Implementation strategy and dependency order** — Spec simplicity/necessity audit, why the sequence works, change-unit dependencies, test strategy, migration/compatibility constraints, parallelism, and commit boundaries.
5. **Change file tree** — complete Create/Modify/Delete/Rename/Generated tree mapped to current evidence, exact symbols, final state, Steps, requirements, and validation owners.
6. **Prerequisites, constraints, and Plan Clarifications** — commands, environments, immutable files/contracts, decisions, dirty-worktree precautions, and small evidence-backed inferences.
7. **Ordered file-by-file implementation Steps** — exact path order and pseudocode contract above.
8. **Test, validation, and quality gates** — RED/GREEN points, focused/module/full checks, expected results, and failure return points.
9. **Migration, compatibility, rollout, and rollback**.
10. **Requirement-to-Step traceability matrix**.
11. **Risks, blockers, and user decisions**.
12. **Review and acceptance** — original requirement fidelity, Spec consistency, repository executability, coverage, release safety, the complete blocking Manual Check table, and final verdict.

## Completion verdicts

Use exactly one:

- `PASS — Ready for user review`
- `BLOCKED — Spec or user decision required`
- `REVISE — Plan and Spec are inconsistent`

`PASS` means internally complete and every blocking Manual Check is `PASS` or evidence-backed `N/A`; it does not mean user-approved or ready to implement. Never claim code, database, service, browser, or runtime verification from a Plan-only task.

## Common failures

| Failure | Required correction |
| --- | --- |
| Writing a Plan from a one-line request | Write/approve a Spec first |
| Requiring a numeric Spec ID instead of an exact path | Link the actual governing Spec and its effective relations |
| Copying a stale target tree | Re-inspect current paths, symbols, consumers, and worktree state |
| Treating a new API/table/page as a Plan clarification | Stop and amend the Spec with user approval |
| Faithfully planning a fetch-then-forward or otherwise unjustified Spec element | Record repository/direct-alternative evidence and return `REVISE`/`BLOCKED` to the Spec; planning detail cannot substitute for necessity |
| Listing phases without exact files | Expand each Step into strict file order and symbols |
| Generic pseudocode such as “implement validation” | Name signatures, fields, branches, collaborators, errors, and assertions |
| File blocks omit repository evidence, consumers, mapping, edge behavior, or after-file state | Complete every required field so execution needs no architecture or behavior decision |
| Validation says only “run tests/build” | Add working directory, exact command/selectors/environment, expected exit/result, and failure return point |
| Putting all tests after production code | Plan focused RED tests before each behavior implementation |
| Guessing validation commands | Read repository scripts/build files and state objective pass criteria |
| Omitting migration/config/docs/permission/observability files | Add every applicable file to the tree and ordered Steps |
| Plan file tree differs from Spec without escalation | Return to the Spec unless it is a proven semantic-preserving rename |
| Planning a hybrid/new package structure without inspecting the current tree | Select the existing traditional profile or exact Archetype profile; otherwise block for a user decision |
| Adding a helper/dependency before Spring/Egon/module reuse discovery | Complete the reuse ledger and prove the capability gap, or remove the addition |
| Java pseudocode omits validation groups, mapping, Bean names/Qualifiers, time/JSON/config semantics, or applicable annotations | Add the exact repository-consistent implementation and validation consequences |
| Replacing the user's numbered rules with a summary or “best practices” paragraph | Restore the exact block and map every original rule to files, pseudocode, validation, and Steps |
| Planning only Controller validation | Add each affected layer handoff, its group/invocation/error behavior, and tests |
| Reducing the complex-class Lombok baseline or bypassing `BaseConverter` | Block and return to the Spec/user; do not plan a silent exception |
| Leaving Complex business variation as direct branching | Add the effective-Spec-selected pattern participants, wiring, orchestration, and tests |
| Marking PASS with a missing/failed/unknown Manual Check | Close every blocker with evidence or use `BLOCKED`/`REVISE` |
| Writing code or starting runtime after the Plan | Stop and deliver for user review |

## Skill maintenance

When changing this skill, first run the literal-rule preservation test (`scripts/test_user_mandated_java_rules.py`), resource-integrity tests (`scripts/test_validate_skill_resources.py`), Manual Check tests (`scripts/test_validate_manual_checks.py`), and preflight (`scripts/validate_skill_resources.py`), then run `references/acceptance-scenarios.md` as review cases and the applicable output validator. Keep `SKILL.zh-CN.md` plus all `*.zh-CN.md` review mirrors synchronized with the English operational contract. A change is not complete if the verbatim source changes, any bundled resource is missing, a path is ambiguous/escaping, or a local Markdown link is broken.
