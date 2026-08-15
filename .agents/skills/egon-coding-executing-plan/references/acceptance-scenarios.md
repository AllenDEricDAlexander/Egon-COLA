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
