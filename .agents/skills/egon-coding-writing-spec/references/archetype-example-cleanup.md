# Archetype examples when business implementation begins

Apply this contract to a generated business project when the user starts real business design or implementation, including an existing project created in another task. A separate cleanup-skill invocation is not required. Creating only a scaffold leaves examples available for inspection; the first business Spec must resolve them before implementation is accepted.

## Trigger and boundary

- Inspect the target generated project, across all of its modules. Discovery covers the whole project even when the first requested business feature touches one domain. Do not leave another unused sample domain simply because its files are outside that feature.
- Use the creation handoff's archetype GAV/version and sample paths when available. Otherwise derive provenance from current POMs, README, packages, registrations and comparison with the matching archetype. Missing creation history is not permission to skip discovery.
- Maintaining the Egon-COLA archetype source/definitions themselves does not trigger deletion: those repositories intentionally own examples. Non-archetype projects, scaffold-only work and unrelated infrastructure maintenance may use evidence-backed `Not applicable`.
- Treat detected remnants in an already partly implemented business project the same as first-business cleanup. If an explicit user scope restriction conflicts with cleanup, expose that conflict in the Spec; do not silently broaden an already approved Plan.
- The Spec designs cleanup and stops at its normal review gate. Actual deletion is part of authorized implementation, after callers/configuration/data consequences are resolved.

## Discover and classify from evidence

Inspect production and test code plus configuration, XML/SQL, dependencies, README/API documentation, HTTP/GraphQL/RPC registrations, message/job/agent wiring and seed data. Follow imports, Bean references, routes, schema/type references and consumers into the sample chain. A Spring scan or auto-registration counts as an incoming reference.

Names such as `User`, `Role`, `Order`, `Course`, `sample` or `demo` are search hints, not proof of sample ownership. A type already adapted to an accepted real-business use case is real business. Startup/wiring, error handling, tenant/security context, Egon integration, managed-DDL support and justified infrastructure tests remain when the real application uses them. Preserve unrelated user work.

Generated sample-specific tests and architecture assertions belong to the sample chain too. Replace their fixed example class counts, routes or business assumptions with the real project's tests while retaining the Egon architecture/component invariants. Do not keep unused sample classes just to satisfy an old scaffold verifier, and do not re-copy or regenerate archetype examples after cleanup.

Record every confirmed sample or disputed candidate with an `EXAMPLE-NNN` ID, exact owned paths/symbols and origin evidence. Directory deletion is valid only when every contained file is classified and its incoming references are covered; a wildcard match alone never authorizes deletion.

| Action | Meaning and required evidence |
| --- | --- |
| Delete | Remove unused example code and its registrations, sample resources/tests/docs; name the cleanup requirement and verification. |
| Replace | Replace an example chain with real-business behavior; name the real requirement/use case, target files and removal of old registrations. |
| Real business | Retain code that implements an accepted real requirement and `UC-*`; identify concrete callers/contracts/tests. Keeping a sample merely as future reference is invalid. |
| Infrastructure | Retain technical code required by the actual application; identify its runtime/build purpose and consumers. |
| Applied history | Preserve only immutable applied SQL/history. Cite the SQL and evidence of application; design any correction as a new version. This action cannot retain active example code. |
| Blocked | Ownership, external compatibility, applied data or authorization is unresolved; identify the decision needed. A PASS verdict is prohibited. |

Do not delete/edit an applied migration, a user-owned business implementation or a published contract by inference. Remove unapplied sample initialization/seed resources only when evidenced unused. If applied state or external consumers are unknown and material, resolve the affected decision before approving deletion; source-only scans are not live database proof.

## Spec Template Version 8 contract

The header field `Archetype Cleanup` and §2.5 `Archetype example cleanup` use the same disposition:

- `Required`: unused examples/remnants need Delete or Replace, or a candidate is Blocked.
- `Complete`: the target business project was freshly scanned and no unused example remains; provide residual evidence. No Delete/Replace/Blocked action is allowed under Complete.
- `Not applicable`: the trigger is absent; provide the concrete task/project reason. Do not use this merely because a small feature was requested.

For Required/Complete, give `Target project:`, `Origin evidence:`, `Scan scope: All target modules` and `Scan evidence:` with current paths/symbols/commands and their limits. Complete additionally gives `Residual evidence:`. Not applicable needs `Reason:`.

Required uses the canonical inventory:

| Example ID | Exact paths/symbols | Origin evidence | Action | Real-business requirement / infrastructure purpose | Incoming references/config/data handling | Acceptance evidence |
| --- | --- | --- | --- | --- | --- | --- |

Delete/Replace/Real business rows reference requirements defined in §4 and traced in §19. Real business also names an accepted `UC-*`. Applied history rows identify `.sql` paths and application evidence. Every cell carries a decision or evidence; bare `N/A`, `None`, placeholders and “keep for reference/later” do not close a candidate.

Make cleanup an explicit `Affected` row in §3.3, with §8 and §14 affected. Carry each Delete/Replace `EXAMPLE-*` into §8's exact file/registration changes and §14's acceptance design. Map the cleanup `REQ-*` through §19. Scope counts as implementation work even if the task is otherwise a simple feature.

## Plan, execution and acceptance handoff

The Plan consumes every inventory ID. Place Delete/Replace work before the first real-business feature is accepted, either in an initial cleanup Step or ordered, bounded Steps that preserve compilation. Do not limit cleanup to the sample files touched by the new feature. An existing approved Plan missing this scope needs amendment; execution must not improvise deletion.

Before the owning Step's commit and again at final acceptance:

1. Reconcile all inventory IDs to removed/replaced paths, real-business evidence, retained infrastructure or immutable history. Resolve every Blocked row.
2. Search source/resources/tests/docs/configuration for evidenced old packages, routes, Bean names, provider/listener/job/agent registrations, mapper/schema identifiers and sample seed/config keys. Broad common nouns are not an absence oracle.
3. Confirm no unused example executable or reference chain remains in production modules and no new business code depends on the retired sample. Regenerate derived output through its owning tool when required.
4. Compile/test affected modules and verify route/Bean/mapper/configuration contracts statically or with focused tests. Do not start applications, infrastructure or databases without authorization.

Missing inventory, deferred cleanup, unexplained retention or unresolved consumers/data state fails `MC-SCOPE-001`, `MC-ARCH-001` and/or `MC-TEST-001` as applicable. A well-formed inventory proves document coverage; it never proves files have actually been deleted.

## 中文执行要点

创建阶段可保留样例供查看；一旦用户开始真实业务，write-spec 必须检查整个目标生成项目的全部模块。已有项目也触发，不能依赖同一聊天里是否调用过创建 skill。维护 archetype 源项目本身不删参考代码。

每个样例或待辨别候选都有准确路径、来源证据和 `EXAMPLE-*` 决策：删除、替换、已是实际业务、必要基础设施、已应用 SQL 历史或阻断。不能以“参考”“以后可能用”保留样例，也不能仅凭类名/目录名删除已变成真实业务的代码。基础设施、用户改动、已应用历史及外部契约要按真实证据处理。

Template Version 8 的 Header 和 §2.5 都写 `Required`、`Complete` 或 `Not applicable`。Required 必须有清单、REQ/UC 证据、§3.3 变更面、§8 文件/注册变动与 §14 验收；Complete 要重新扫描并证明无遗留；N/A 要说明触发条件不适用。设计阶段不删除源码，获批 Plan 在首个业务功能验收前完成所有删除/替换，提交前和最终验收都逐项复核。
