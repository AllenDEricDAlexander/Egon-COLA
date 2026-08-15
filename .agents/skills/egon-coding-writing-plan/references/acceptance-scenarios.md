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
