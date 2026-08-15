# Per-Step Gate Checklist

Use this checklist for every Plan Step. Record objective evidence; do not replace failed or missing evidence with a narrative assertion.

## Step lock

- [ ] Exact Plan path and revision recorded.
- [ ] Step number, title, requirement IDs, dependencies, and proposed commit recorded.
- [ ] Effective Spec sections for this Step reread.
- [ ] `git rev-parse HEAD` recorded as the Step baseline.
- [ ] Current branch, `git status --short`, staged paths, and untracked paths inspected.
- [ ] Every earlier dependency is represented by an already verified commit.
- [ ] Declared Step paths do not overlap unrelated or concurrent edits.
- [ ] Planned files, symbols, consumers, migration sequence, and commands still exist.
- [ ] No material repository, Plan, or Spec drift is present.

## RED gate when the Step changes behavior

- [ ] The focused test is created or modified before production behavior.
- [ ] The exact focused command is run.
- [ ] The test fails for the expected missing behavior.
- [ ] Failure is not caused by compilation, fixture, dependency, environment, or unrelated errors.
- [ ] RED evidence is recorded. If RED is legitimately impossible, the approved reason is recorded.

## Implementation gate

- [ ] Only one Step is `In Progress`.
- [ ] Files are handled in the Plan's declared order.
- [ ] Every operation is limited to declared `CREATE`, `MODIFY`, `DELETE`, `RENAME`, or `GENERATED` paths.
- [ ] Implementation follows real repository APIs and conventions while preserving the Plan's semantics.
- [ ] No undeclared public contract, dependency, migration, schema, permission, page, or architecture change was introduced.
- [ ] No unrelated refactor, formatting sweep, debug output, secret, or generated noise was added.

## Verification gate

- [ ] Exact focused validation passed with complete output and exit status.
- [ ] Required compile, typecheck, lint/format, XML/schema, module, integration, or regression gates passed.
- [ ] Expected GREEN behavior and relevant error paths are covered.
- [ ] Current Step requirements and effective Spec sections were reread against the implementation.
- [ ] `git diff --check -- <Step paths>` passed.
- [ ] Failures, skips, warnings, timeouts, and unavailable runtime evidence are classified honestly.

## Pre-commit gate

- [ ] `git diff -- <Step paths>` was reviewed line by line.
- [ ] `git status --short` was reviewed again.
- [ ] Only explicit Step paths were staged; broad staging was not used.
- [ ] `git diff --cached --check` passed for the Step paths.
- [ ] Cached `--stat` and `--name-only` match the declared Step scope.
- [ ] The commit message is semantic and matches the Plan proposal or repository convention.
- [ ] The commit is non-empty.

## Post-commit gate

- [ ] Commit succeeded and its hash was recorded.
- [ ] `git show --stat --oneline <hash>` was inspected.
- [ ] The committed file list contains only Step-owned paths.
- [ ] Validation commands and observed results are recorded against the hash.
- [ ] Deviations are recorded as `None`, approved clarification, or corrective-commit explanation.
- [ ] Unrelated staged, unstaged, and untracked work remains preserved.
- [ ] The Step is marked `Committed` before any later Step begins.

If any required box cannot be checked, keep the Step `In Progress` or mark it `Blocked`; do not advance.
