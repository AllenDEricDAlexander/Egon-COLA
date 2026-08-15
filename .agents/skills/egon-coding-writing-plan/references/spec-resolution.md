# Resolving the effective Spec for a Plan

## Select the primary target

1. Use the exact user-provided path when present.
2. Otherwise search `docs/egon/spec` and relevant legacy design directories.
3. Select a document only when it unambiguously governs the requested coding scope.
4. Ask the user when several current candidates remain.

Do not require a numeric Spec ID. Identity comes from the repository-relative path, metadata, update/revision, and baseline commit.

## Resolve effective content

1. Read the primary document completely.
2. Follow its `Supersedes`/replacement relations and remove replaced content only for the named scope.
3. Apply accepted `Amends` documents in timestamp/order.
4. Include normative `Depends On` sections.
5. Preserve source requirement identifiers and exact acceptance criteria.
6. Record every effective document, status/revision, and exact sections in the Plan.
7. Treat undocumented conflicts as a major blocker.

When an eligible legacy Spec has no formal requirement IDs, create `PLAN-REQ-NNN` aliases that point to exact existing statements and sections. These aliases provide traceability only; they cannot alter, extend, or reinterpret the source.

## Eligibility gate

A target is eligible when:

- it contains a sufficiently complete coding design for the requested scope;
- the relevant design is current after resolving relationships;
- acceptance criteria and implementation boundaries are traceable;
- major decisions affecting implementation are closed or the user explicitly requested a non-Ready Draft Plan;
- the coding work is not already completed.

Stop and use `egon-coding-writing-spec` or ask the user when:

- there is no Spec or the target is ambiguous;
- accepted/effective documents conflict;
- open decisions alter behavior, contracts, data, security, architecture, migration, compatibility, or rollout;
- the Plan would introduce behavior or structure absent from effective design;
- repository drift materially invalidates paths, ownership, contracts, or acceptance;
- the target describes completed work and only verification remains.

## Drift classification

- **Minor drift**: a semantic-preserving rename, moved file with identical responsibility, or unambiguous internal placement change. Record `Plan Clarification` with path/symbol evidence.
- **Major drift**: changed behavior, ownership, contract, schema, dependency direction, security, migration, compatibility, or already-completed scope. Return to the Spec/user.

Never disguise major drift as an implementation detail.
