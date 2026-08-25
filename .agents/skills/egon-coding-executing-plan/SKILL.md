---
name: egon-coding-executing-plan
description: Use when an approved coding Plan must be implemented one Step at a time with Java/Spring/Egon-COLA standards, blocking Manual Checks, validation and a separate commit after every completed Step, followed by a final conformance audit against the effective Specs.
---

# EGON Coding Plan Execution

## Purpose

Execute one approved coding Plan sequentially. Complete, verify, and commit exactly one Plan Step before starting the next. After all Steps, audit the delivered repository against the effective Specs and report every unmet, partial, or runtime-unverified requirement.

The Plan controls implementation order. The effective Specs control correctness. A completed Plan is not sufficient when the implementation still violates a Spec.

## Resource-integrity preflight

Resolve the directory containing this `SKILL.md` as `<skill-root>`. Before reading a bundled reference or editing code, run:

```bash
python3 <skill-root>/scripts/validate_skill_resources.py
```

Replace `<skill-root>` with the resolved absolute directory. Bundled resources must use skill-root-relative `references/` or `scripts/` paths. If the preflight reports a missing, escaping, ambiguous, or broken resource, stop execution, report the exact diagnostic, and repair/reinstall the skill. Never continue with an invented or partial checklist.

## User-mandated Java rules — verbatim normative source

The following rules are preserved exactly and are mandatory at coding, Step, commit, and final-audit gates. Read `references/user-mandated-java-rules.md` for the concrete inspection contract.

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

Do not translate, renumber, correct, shorten, summarize, or weaken this block. Literal spellings are mapped to exact code symbols by `references/user-mandated-java-rules.md` without changing the source.

## Entry conditions and execution authorization

Before modifying code:

1. Identify one exact Plan path and read it completely.
2. Resolve and read the primary `Implements Spec` plus every `Effective Specs` document and governing amendment, supersession, or dependency.
3. Read all applicable `AGENTS.md` files and repository instructions.
4. Confirm the Plan revision and repository baseline. Reinspect every current path, symbol, migration sequence, consumer, and validation command used by the next Step.
5. Confirm execution authorization:
   - normally the Plan is `Ready` and the primary Spec is `Accepted` or `Implemented`; or
   - the user explicitly authorizes execution of the exact Plan and Spec revisions in the current conversation.
6. Run the structural validator supplied by the installed `egon-coding-writing-plan` skill against the exact Plan with strict mode enabled.
7. Inspect `git status`, the current branch/HEAD, staged changes, and untracked files. Preserve all unrelated work.
8. For Java work, read `references/user-mandated-java-rules.md` and `references/java-spring-egon-coding-standards.md` completely. Confirm the actual project tree is either the established traditional layered profile or the exact selected Egon-COLA Archetype profile. Revalidate the Plan's Spring/Starter/Egon/module capability reuse ledger before adding code or dependencies.

Stop before implementation when the Plan target is ambiguous, authorization is absent, an effective Spec conflicts, a major decision is open, the Plan is structurally invalid, or repository drift changes architecture, behavior, contracts, data, security, migration, compatibility, or Step ownership.

Do not start services, browsers, databases, local stacks, or long-running runtime tests unless the user explicitly requests them. Source, compile, static, and focused/module test execution remains allowed when specified by the Plan.

## Step state machine

Treat each Plan Step as a strict state transition:

```text
Pending -> In Progress -> Verified -> Committed
              |              |
              +-> Blocked <--+
```

- `Pending`: no Step-owned edits have begun.
- `In Progress`: only the current Step's files may be changed.
- `Verified`: every required Step validation passed with fresh evidence and the diff review passed.
- `Committed`: one path-limited semantic commit exists and its hash/scope were verified.
- `Blocked`: the Step cannot safely reach `Verified` or `Committed` within the approved Plan.

**Never begin Step N+1 until Step N is `Committed`.** A test pass, code completion, or staged diff is not a completed Step without its commit.

## Non-negotiable execution rules

1. Execute Steps in Plan order. Do not batch several Steps into one working-tree change or one commit.
2. Keep at most one Step `In Progress`.
3. Follow the Step's ordered file sequence, operations, symbols, pseudocode, requirements, and validation gates. Do not redesign while implementing.
4. Use test-first execution for every behavior covered by a planned RED/GREEN cycle: write the focused test, observe the expected failure, implement the minimum behavior, then refactor/wire while tests remain green.
5. Modify only the current Step's declared files and generated outputs. A required undeclared file, public contract, migration, dependency, or behavior is Plan drift; stop and ask for a Plan/Spec correction.
6. Preserve unrelated dirty or staged work. Never use broad staging such as `git add -A` or `git add .`. Stage and commit only explicit Step paths.
7. Do not overwrite, revert, reformat, stage, or commit another person's unrelated changes. If unrelated edits overlap a Step-owned file, stop and report the overlap.
8. Run the exact focused validation required by the Step, plus the smallest relevant compile/static/regression gate needed to prove the Step is internally complete.
9. Read the full command output and exit status. A lost process handle, partial log, timeout, skipped test, or warning treated as failure by repository policy is not a pass.
10. Review the Step diff against its requirements and the effective Spec before committing. Remove accidental files, debug output, secrets, generated noise, and unrelated refactors.
11. Commit every verified Step immediately using a path-limited semantic commit. Prefer the Plan's proposed message when it matches the final diff and repository convention.
12. Never create an empty commit to simulate Step completion. If a Step is already implemented or produces no semantic diff, classify it as repository/Plan drift and stop for direction.
13. After committing, verify the commit hash, file list, diff summary, validation evidence, and remaining worktree state. Record the commit against the Step before advancing.
14. Do not amend, squash, reset, or rewrite committed history automatically. If a later Step exposes a defect in an earlier commit, stop advancing, make the smallest dedicated corrective commit attributed to the originating Step, rerun affected gates, and report the deviation.
15. Never modify an existing immutable Flyway migration. Execute only the new migration file named by the approved Plan and Spec.
16. Do not silently skip, reorder, merge, split, or expand Steps. Obtain user approval for a material execution-sequence change.
17. Every coding Step has a blocking Manual Check. At Step lock, enumerate all applicable stable `MC-*` IDs from `references/java-spring-egon-coding-standards.md`; before commit, evaluate each row manually with concrete diff/path/symbol/command evidence. `MC-SCOPE-001` and `MC-TEST-001` always apply.
18. A Step cannot become `Verified` or be committed as complete while any applicable Manual Check is `FAIL`, `BLOCKED`, `UNKNOWN`, missing, lacks evidence, or has an unresolved exception. Evidence-backed `N/A` is allowed only when the concern is truly outside the Step.
19. For touched Java code, enforce semantic type suffixes, Jakarta/Spring Validation and groups at every affected handoff, approved normalization, the exact record/`@Value`/complete complex-class Lombok classification, MapStruct/MapStructPlus plus mandatory Egon `BaseConverter`, `@Slf4j`, explicit Bean names, qualified Lombok constructor injection, approved utilities, Jackson, `java.time`, configuration-profile parity, and a mandatory approved pattern for every Complex business flow. Do not perform unrelated broad cleanup.
20. Apply `references/user-mandated-java-rules.md` literally. At Step lock and before commit, execute separate Rule 1, 2, 3, 4, 5, 6, 7, 9, 10, and 11 rows. Rule 11 always applies. No rule may be collapsed into a generic Manual Check assertion, changed to preference language, or waived because tests pass.
21. Complex objects must retain the complete mandated Lombok baseline and every new affected Converter must use MapStruct/MapStructPlus plus Egon `BaseConverter`; any constructor/framework/converter conflict blocks the Step. Every affected layer handoff must be validated, and every Complex business flow must implement the approved pattern rather than direct branching.

## Per-Step execution workflow

Read `references/step-gate-checklist.md` at the start and end of every Step.

### 1. Lock the Step

- Record Step number/title, requirements, dependencies, declared paths, expected RED/GREEN behavior, validation commands, rollback point, and proposed commit.
- Record `git rev-parse HEAD` as the Step baseline.
- Verify all dependencies are represented by earlier committed hashes.
- Confirm the Step's paths do not overlap unrelated work.
- Copy the Plan's applicable `MC-*` IDs into the Step Manual Check table, add any concern newly revealed by current repository evidence, and record architecture/reuse baselines before editing.
- Copy all ten original `Rule N` rows into the Literal Rule Gate. Record current applicability/evidence before editing; Rule 11 is always `Applicable`.

### 2. Revalidate the current repository

- Reopen the actual files and symbols before editing; do not rely only on Plan pseudocode.
- Confirm the Plan's implementation direction still matches current APIs, consumers, language/framework style, and migration sequence.
- Treat semantic drift as a blocker. Resolve only mechanical, local details already permitted by a `Plan Clarification` or an unambiguous repository convention.
- For Java, recheck the exact architecture profile, existing Spring/Egon/module candidates, dependencies, `lombok.config`, converters/validators, environment profiles, and nearby naming/model/Bean conventions relevant to this Step.

### 3. Execute the Step in file order

- Apply each declared `CREATE`, `MODIFY`, `DELETE`, `RENAME`, or `GENERATED` action in order.
- For behavior changes, run the focused test at the RED point and confirm the failure is caused by missing behavior rather than syntax, fixture, dependency, or environment errors.
- Implement only the minimum Spec-compliant behavior needed for GREEN.
- Preserve current project style, comments/annotations, module boundaries, public compatibility, and unrelated behavior.

### 4. Verify the Step

- Run the Step's exact focused command and confirm the objective expected result.
- Run applicable compile, lint/format, mapper/XML/schema, module, or cross-module checks required by the Step and repository.
- Run `git diff --check` on the Step paths.
- Re-read the Step requirements and relevant Spec sections. Confirm every stated behavior, error path, field, state, permission, migration, UI state, and test obligation represented by this Step is implemented.
- Execute each applicable Manual Check row in `references/step-gate-checklist.md`. Record independent evidence and findings; close failures or mark the Step blocked.
- Re-execute every Literal Rule Gate row against the final diff. Tests/static searches support but never replace the manual per-rule inspection.

Any failed gate keeps the Step `In Progress` or `Blocked`; it cannot be committed as complete.

### 5. Review and commit the Step

- Inspect `git diff -- <Step paths>` and `git status --short`.
- Confirm only Step-owned paths will be committed.
- Stage explicit paths, then inspect `git diff --cached --check`, `--stat`, and `--name-only`.
- Commit only those paths. If other work is already staged, use a path-limited commit that leaves it untouched.
- Capture the resulting hash and inspect `git show --stat --oneline <hash>` plus the committed file list.
- Confirm unrelated staged/unstaged/untracked work remains preserved.

Only now mark the Step `Committed` and begin the next one.

## Failure and blocker handling

Stop the current Step and report evidence when:

- the Plan or Spec is ambiguous, contradictory, unapproved, or materially stale;
- a required file/symbol/consumer is missing or already changed with different semantics;
- the Step needs an undeclared contract, table/column, migration, dependency, page, permission, or architecture change;
- validation fails for a reason that cannot be repaired inside the Step's approved scope;
- credentials, permissions, external services, or runtime state are required but unavailable;
- another change overlaps a Step-owned file;
- a safe path-limited commit cannot be produced.

Do not mark a blocker as complete, skip to a later Step, or create a misleading commit. Report the affected Step, evidence, Spec/Plan impact, safe options, and recommended next action.

## Commit contract

Every implementation Step must produce at least one non-empty semantic commit before the next Step begins.

For each Step record:

| Evidence | Required value |
| --- | --- |
| Step | Number and exact Plan title |
| Requirements | Source requirement IDs |
| Baseline | Commit before Step edits |
| Commit | Resulting full or short hash |
| Paths | Exact committed file list |
| Validation | Commands and observed results |
| Manual Check | All stable IDs with `PASS` or evidence-backed `N/A`; no unresolved row |
| Literal Rules | Rules 1, 2, 3, 4, 5, 6, 7, 9, 10, 11 each `PASS` or evidence-backed `N/A`; Rule 11 `PASS` |
| Deviations | `None`, approved clarification, or corrective-commit explanation |

Use path-limited staging/commits. Never include unrelated work merely because it was already staged. Do not push, open a PR, merge, or release unless the user separately authorizes it.

## Final Spec conformance audit

After every Plan Step is committed, read `references/final-spec-audit.md` and perform a fresh audit. Do not rely on the Plan's traceability matrix alone.

1. Re-resolve the final effective Spec set and exact revisions.
2. Extract every effective requirement, acceptance criterion, non-goal, interface, model/schema rule, UI behavior, test obligation, non-functional constraint, migration, compatibility, rollout, and rollback requirement.
3. Map each requirement to concrete implementation evidence: commit(s), paths/symbols, and validation/test output.
4. Run the Plan's final source/static/module/full regression commands that are safe and authorized. Do not automatically start runtime systems.
5. Assign one status to every requirement:
   - `Satisfied`: implementation and required non-runtime evidence prove it.
   - `Partial`: only part of the requirement is implemented or proven.
   - `Not satisfied`: implementation conflicts with or omits the requirement.
   - `Runtime unverified`: source/module evidence exists, but the Spec requires user-controlled live-system proof that was not run.
6. Check non-goals and scope boundaries for accidental behavior, dependency, migration, or refactor expansion.
7. Check every Plan Step has a verified commit and no planned file/validation gate was silently omitted.
8. Re-execute all ten original Literal Rules and all 17 Manual Checks against the final tree and delivery commits. Every applicable row must be `PASS`; every `N/A` needs evidence and reason; Rule 11 and `MC-BLOCKER-001` must pass and reconcile all findings.
9. Report every `Partial`, `Not satisfied`, `Runtime unverified`, failed/blocked Literal Rule or Manual Check, and silent-exception attempt with evidence, impact, and recommended next action.

Do not silently add unplanned fixes during the final audit. If the audit finds a gap, report it and wait for the user to approve a corrective Plan/Step.

## Final report contract

The completion report must contain:

- Plan path/revision and effective Spec paths/revisions;
- a Step table with status, commit hash, committed paths, and validation evidence;
- final validation commands and actual results;
- a Spec conformance matrix with every requirement status;
- a final Manual Check matrix containing every stable `MC-*` ID, applicability, status, evidence, finding, and required action/exception;
- a final Literal Rule matrix containing Rules 1, 2, 3, 4, 5, 6, 7, 9, 10, and 11 separately with applicability, status, diff evidence, validation evidence, finding, and action;
- explicit unmet, partial, and runtime-unverified requirements;
- approved deviations and corrective commits;
- remaining worktree state and confirmation that unrelated work was preserved;
- whether runtime, database, browser, deployment, push, PR, or release actions were not performed;
- one final verdict:
  - `PASS — Implementation conforms to the effective Specs`
  - `PARTIAL — Spec requirements are unmet or unverified`
  - `BLOCKED — Final verification could not be completed`

Never claim full completion when any effective requirement is `Partial`, `Not satisfied`, required runtime evidence is missing, or any Literal Rule/Manual Check is missing, non-passing, weakened, or unsupported. Final PASS requires every Spec requirement satisfied, every applicable literal rule passed, Rule 11 passed, and every applicable Manual Check passed.

## Common failures

| Failure | Required correction |
| --- | --- |
| Editing several Steps before committing | Revert only the unapproved later-Step edits safely; finish and commit the current Step first |
| Starting Step N+1 after tests but before commit | Stop; verify and commit Step N |
| Committing unrelated staged files | Use path-limited commit and verify the committed file list |
| Creating an empty Step commit | Stop and report Plan/baseline drift |
| Treating Plan completion as Spec compliance | Run the independent final Spec audit |
| Fixing an unplanned Spec gap during final audit | Report it and request a corrective Plan/Step |
| Claiming runtime acceptance from unit/module tests | Mark the requirement `Runtime unverified` |
| Rewriting earlier Step commits after later work | Preserve history; use an attributed corrective commit and report it |
| Committing a Step with a failed/missing/unknown Manual Check | Keep it `In Progress`/`Blocked`; close each row with evidence before commit |
| Adding a dependency/helper without current Spring/Egon/module reuse proof | Stop, rebuild the reuse ledger, and use existing capability or return to the Spec/Plan for gap approval |
| Creating a hybrid package tree during execution | Stop; preserve the existing traditional or exact Archetype profile and request a structural Plan/Spec correction |
| Passing final audit by summarizing Manual Checks in one sentence | Re-run and record all stable IDs individually against the final commits/tree |
| Replacing the exact numbered rules with a general coding-standard summary | Restore the verbatim source and re-run every original Rule row independently |
| Tests pass while a complex class lacks the full Lombok baseline or a Converter bypasses `BaseConverter` | Keep the Step blocked; compiler/test success cannot waive Rule 3 |
| Only Controller input is validated | Add and verify Validation/groups at every affected layer handoff before commit |
| Complex business logic remains `if/else` or `switch` | Implement the approved design-pattern participants and tests before commit |
| Starting the project automatically | Leave runtime testing to the user unless explicitly requested |

## Skill maintenance

When changing this skill, run `scripts/test_user_mandated_java_rules.py`, `scripts/test_validate_skill_resources.py`, and `scripts/validate_skill_resources.py`, review `references/acceptance-scenarios.md`, and confirm the verbatim source, English operational file, Chinese review mirror, standards, checklists, and metadata still express the same execution contract.
