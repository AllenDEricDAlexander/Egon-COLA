# Maintainer acceptance scenarios

Use these scenarios after changing this skill.

1. **No Spec**: A user asks for a Plan from one sentence. Expected: require/write a Spec first; do not turn the request into architecture during planning.
2. **Multiple candidates**: Two current Specs govern the same module and neither is clearly primary. Expected: ask the user for the target path.
3. **Dated or legacy Spec**: The governing document has no `EGON-SPEC-NNNN` ID but has a complete, current coding design. Expected: link the exact relative path and sections; do not reject it solely for naming.
4. **Amended design**: A base Spec plus an accepted amendment changes one contract field. Expected: include both under `Effective Specs`; all file pseudocode uses the amended field.
5. **Requirement omission**: Effective Specs contain five requirements but Step coverage contains four. Expected: strict validation failure.
6. **Plan redesign**: The planner decides a new table/API/page would be cleaner than the Spec. Expected: stop and amend the Spec with user approval.
7. **Small repository rename**: The Spec names `OldService`; current `NewService` has identical responsibility and consumers. Expected: evidence-backed `Plan Clarification`, not a new architecture decision.
8. **Major repository drift**: The requested behavior is already implemented or the target architecture no longer exists. Expected: stop; request a residual-work/amending Spec or route to verification rather than duplicate implementation.
9. **Generic pseudocode**: A Step says “implement validation and update service.” Expected: review failure until signatures, symbols, fields, branches, collaborators, errors, and assertions are explicit.
10. **Tests delayed**: All production files precede behavior tests. Expected: review failure; put focused RED tests before implementation unless explicitly authorized otherwise.
11. **Migration pressure**: One schema change exists and historical Flyway migrations are immutable. Expected: exactly one new next-version migration in the file order; no edit to old migrations.
12. **Dirty worktree**: Unrelated changes are present. Expected: explicit preservation and path-limited Step/commit scopes.
13. **Fetch-then-forward Spec defect**: The accepted Spec adds a query whose only consumer copies tenant/application values into a command that can derive them. Expected: Plan returns `REVISE`/`BLOCKED` with repository evidence and exact Spec sections; it does not produce files for the query.
14. **Missing Spec necessity decision**: A new cache/factory/endpoint appears in a legacy Spec without a direct-alternative analysis. Expected: inspect current repository, perform the concise sanity audit, and escalate if the simpler option satisfies the same requirements.
15. **Phase-only Step**: A Step says “implement backend” and lists controller/service/DAO without exact order or intermediate states. Expected: review failure; split by semantic outcome and expand every file.
16. **Shallow file block**: A file has path and pseudocode but no repository evidence, callers, mappings, edge behavior, verification contribution, or after-file state. Expected: Template v2 strict validation/review failure.
17. **Generic short pseudocode**: Pseudocode contains two lines, `validate(); save();`. Expected: strict validation warning/error; name exact conditions, collaborators, mapping, error/transaction behavior, and assertions.
18. **Compile prerequisite before RED**: A generated/proto type must exist before the focused test compiles. Expected: plan the source-of-truth contract/generation first, state it is a compile prerequisite rather than RED proof, then place the focused RED test before provider/consumer behavior.
19. **Frontend detailed Step**: Component test, API type, client/hook, page states, route wiring, query invalidation, exact typecheck/test commands, and manual boundary are ordered from evidence. Expected: pass when no architecture decision remains.
20. **Validation without cwd**: A Step says `mvn test` with no repository/module working directory, selector, expected tests, or failure interpretation. Expected: strict validation/review failure.
21. **Commit scope ambiguity**: A Step says “commit changed files.” Expected: review failure; list exact repository-relative commit paths matching the Step write scope.
22. **Same file in multiple Steps**: Two semantic outcomes modify different methods in one file. Expected: either combine into one atomic Step or explicitly assign symbols/sections, intermediate compile behavior, and separate non-overlapping commit intent.
