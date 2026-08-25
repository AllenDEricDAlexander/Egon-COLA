# Acceptance Scenarios

Use these scenarios to review future changes to this skill.

1. **Unrelated dirty and staged work exists.** The executor records it, stages only explicit current-Step paths, creates a path-limited commit, and proves the unrelated work remains untouched.
2. **Step tests pass but no commit exists.** The Step remains `Verified`; Step N+1 cannot start until the current Step is committed and the hash/scope are checked.
3. **The Step is already implemented and produces no diff.** The executor does not create an empty commit; it reports Plan/baseline drift and waits for direction.
4. **Implementation requires an undeclared file or public contract.** The executor stops and requests an approved Plan or Spec correction before modifying that area.
5. **A validation fails inside the approved scope.** The executor diagnoses and fixes it within the current Step, reruns the gate, and commits only after success; otherwise it reports `Blocked`.
6. **Another person's changes overlap a Step-owned file.** The executor stops and reports the exact overlap instead of overwriting, reverting, or committing it.
7. **A later Step exposes an earlier Step defect.** The executor pauses later work, creates a dedicated minimal corrective commit attributed to the earlier Step, reruns affected gates, and does not amend or reset history.
8. **The final audit finds an omitted Spec requirement.** The executor marks it `Partial` or `Not satisfied`, reports evidence and impact, and waits for a corrective Plan/Step instead of silently fixing it.
9. **A Spec acceptance criterion requires a live runtime that was not authorized.** The executor records available source/module evidence and marks the criterion `Runtime unverified`; it does not claim PASS.
10. **The Plan or Spec header still says Review, but the user explicitly authorizes exact revisions in the current conversation.** Execution may proceed for those revisions, while all Step gates and the final independent Spec audit remain mandatory.
11. **A bundled standards/checklist reference is missing.** Resource-integrity preflight fails before editing and reports the exact missing path; execution does not invent a replacement rule.
12. **The current project is traditional layered.** The Step preserves `biz.controller -> biz.service -> biz.service.impl -> biz.dao`; it does not add generic COLA/DDD packages.
13. **The project uses an exact Egon-COLA Archetype.** The executor rechecks the selected archetype tree/verifier and preserves its modules/dependency direction instead of forcing `biz.*` or a hybrid.
14. **A Step proposes a helper or dependency already covered by Spring/Egon-COLA.** `MC-REUSE-001`/`MC-DEP-001` fail; the Step reuses the existing Starter/Component/abstraction or stops for an approved gap decision.
15. **A new carrier is named `UserInfo`, `OrderParam`, or `XxxData`.** `MC-NAME-001` fails until the touched type has its real PO/BO/DTO/VO/Query/Command/Event/boundary role; unrelated legacy names remain untouched.
16. **A reused Command crosses create/update boundaries.** The Step cannot PASS until Jakarta constraints, Validation Groups, group invocation, normalization/error behavior, and focused tests are proven.
17. **A telephone rule is handwritten despite available mature validation.** The executor rejects duplicate parsing/Validator code, uses native Validation/`ValidationUtils`/approved libphonenumber integration, or stops for a proven gap.
18. **A simple carrier receives every Lombok annotation.** `MC-MODEL-001` fails; the executor selects a record/compact constructor, `@Value`, or only the necessary mutable-class annotations based on actual framework semantics.
19. **A Service maps objects through setters, BeanUtils, reflection, or JSON.** `MC-CONVERT-001` fails until MapStruct/MapStructPlus and mandatory Egon `BaseConverter` own the mapping; no one-way exception is inferred.
20. **A Spring Bean has no explicit name, field injection, or an unqualified dependency.** `MC-BEAN-001` fails until stable naming, final-field `@RequiredArgsConstructor`, `@Qualifier`, and Lombok propagation are verified.
21. **A configuration key is changed in only one environment.** `MC-CONFIG-001` fails until all profiles are compared and key parity plus typed properties/tests are satisfied.
22. **Complex rules remain a hard-coded switch, or simple logic grows ceremonial patterns.** `MC-PATTERN-001` fails until the approved present variation is isolated with the right pattern or direct logic is retained without overdesign.
23. **Tests pass but one Manual Check is missing or lacks evidence.** The Step stays `In Progress`; no completion commit and no next Step are allowed.
24. **All Steps passed their local checks, but the final tree violates a standard.** The final audit reruns all 17 IDs, returns `PARTIAL`, reports the exact gap, and waits for a corrective Plan/Step.
25. **Verbatim rules were summarized or corrected.** The literal preservation test fails when either entrypoint or either phase reference changes the exact source, numbering, escapes, spelling, or trailing `&#x20;` text.
26. **Tests pass but Literal Rule evidence is missing.** The Step remains `In Progress`; passing tests cannot replace the ten independent rule rows or permit a commit.
27. **Controller-only Validation.** Controller tests pass, but the Service and DAO/Repository handoffs lack group/invocation checks. Rule 2 and `MC-VALID-001` fail; no commit occurs until every affected crossing is proven.
28. **Reduced complex-object annotations.** The code compiles with only `@Data @Builder`, contrary to the approved complete baseline. Rule 3 and `MC-MODEL-001` fail even though tests pass; the executor blocks for the exact conflict/decision.
29. **Converter bypass.** A new MapStruct mapper does not implement Egon `BaseConverter`, or a Service manually copies fields. Rule 3 and `MC-CONVERT-001` fail; the executor does not treat one-way mapping as an implicit exception.
30. **Bean injection almost compliant.** A Bean has `@RequiredArgsConstructor` but one dependency lacks `@Qualifier`, or the Bean name is implicit. Rule 4 and `MC-BEAN-001` fail until the exact field/name/lombok propagation is proven.
31. **One-profile configuration change.** Tests use only dev configuration and pass, but another profile lacks the new key. Rule 7 and `MC-CONFIG-001` fail; the Step cannot commit as complete.
32. **Complex logic uses direct switch.** The Plan requires a State/Strategy pattern but implementation collapses it into a switch. Rule 9 and `MC-PATTERN-001` fail; shorter code is not a waiver.
33. **Final audit repeats only MC IDs.** The final report has 17 Manual Checks but omits the ten literal rules. Final PASS is prohibited until both matrices are independently rerun against final commits.
