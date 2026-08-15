# RFC-style governance for EGON Coding Specs

## Filenames and identity

- File: `docs/egon/spec/YYYY-MM-DD-HH-MM-<lowercase-kebab-abstract>.md`.
- The timestamp is the local creation minute and is part of the document identity.
- The abstract is concise, stable, and specific enough to avoid collisions.
- Never reuse a path for a different design and never overwrite a predecessor. If a collision occurs, choose a more specific abstract.
- Header `Document`, `Created`, and `Updated` values must agree with the file and actual history.

## Status lifecycle

```text
Draft -> Review -> Accepted -> Implemented
  |        |          |
  +------> Rejected   +-> Superseded
```

- `Draft`: incomplete or blocked by a major decision.
- `Review`: internally complete and awaiting user/decision-owner review.
- `Accepted`: explicitly approved; never infer acceptance from silence.
- `Implemented`: the accepted design was delivered and verified by implementation evidence.
- `Superseded`: a later Spec completely replaces it for a named scope.
- `Rejected`: deliberately not adopted.

## Relationship semantics

- `Amends`: changes or fills only named sections of an earlier Spec. Both documents form the effective design.
- `Supersedes`: completely replaces an earlier Spec for an explicitly named scope.
- `Depends On`: the current design cannot be understood or implemented correctly without the referenced sections.
- `Related Specs`: useful context without normative dependency.
- `Related Plans`: implementation plans derived from this Spec.

Relationships use relative Markdown links and exact section references. Targets may be older design documents outside `docs/egon/spec` if they remain authoritative.

A later Spec may repair an earlier one. It must identify the exact old rules it changes and the replacement rules. Update only relationship/status metadata in an accepted predecessor when repository policy allows; never silently rewrite approved normative content.

## Effective-design resolution

When several documents govern a task:

1. Start with the base Spec or authoritative legacy design.
2. Exclude content superseded for the relevant scope.
3. Apply accepted amendments in timestamp order.
4. Include normative dependencies.
5. Treat unresolved conflicts as a major ambiguity and ask the user.

Cite exact locations such as `[Identity baseline](../../superpowers/specs/2026-08-01-unified-identity-platform-design.md) §15.1`, not “the old spec.”

## Backlinks and immutability

When creating a Plan, add it to the Spec's `Related Plans` metadata if repository policy permits metadata maintenance. When creating an amendment/supersession, add a metadata-only backlink to the predecessor if safe. Do not make backlink updates when the predecessor is immutable by repository rule; the forward link remains authoritative.
