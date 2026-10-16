# Time semantics, UTC, and conversion boundaries

Read this reference when an affected field, query, event, calculation, schedule, persistence mapping or UI depends on time. Apply it within the authorized change surface; reading it does not authorize database, historical-data, configuration or business-code changes.

## Classify the meaning first

| Semantics | Java type | PostgreSQL representation | Wire policy |
| --- | --- | --- | --- |
| Occurred instant: creation, payment, completion, expiry, event occurrence | `Instant`; boundary `OffsetDateTime` converted to an instant | `timestamptz(6)` | ISO 8601 UTC with `Z`, e.g. `2026-10-13T02:00:00Z` |
| Pure date: birthday, business date, accounting date | `LocalDate` | `date` | `YYYY-MM-DD`; never shift by time zone |
| Local time: opening time, a wall-clock value | `LocalTime` | `time` | Local time; identify the owning business zone when resolving an occurrence |
| Zoned schedule: local appointment or recurring execution rule | `LocalDateTime` / `LocalTime` plus IANA `ZoneId`; `ZonedDateTime` for calculation | Local date/time plus zone ID; recurrence rule if needed | Local values plus an explicit zone ID |
| Duration: elapsed time, timeout, TTL | `Duration` | Numeric value with explicit unit, or the existing compatible representation | Explicit unit or agreed ISO duration; no zone conversion |

A resolved appointment instant is a separate occurred-instant value. Keep local intent and zone when future or recurring behavior must follow regional rules. An offset such as `+08:00` identifies an instant with a date/time but does not encode a region's future daylight-saving rules. Use IANA names such as `Asia/Shanghai` and `America/New_York` for business zones. For non-Java work, document the equivalent runtime type without inventing Java files.

## Storage and transmission

- New occurred-instant persistence uses UTC instant semantics; PostgreSQL `timestamptz` normalizes to UTC but retains neither the original offset nor the IANA zone. Store a separate zone only when a real requirement needs it. Changing DB session display time does not change the stored instant.
- Emit occurred instants as ISO 8601 UTC `Z` in new/changed REST, GraphQL, event and RPC contracts. Normalize input with an explicit offset/`Z`; if a local-input workflow is required, require a known business zone and resolve it at the boundary. Never guess from the server default zone or emit a bare local date/time as an instant. Pure dates, local schedules and durations retain their own formats.
- Check the actual MyBatis/JDBC/JSON mapping. pgJDBC's native `timestamptz` counterpart is `OffsetDateTime`; using `Instant` in business models requires verified existing mapping or an approved boundary conversion. Do not assume `setObject(Instant)` is supported or add another handler when existing capability is sufficient.
- Align persisted precision with the actual column/handler (normally PostgreSQL microseconds); document wire precision and any rounding/truncation. Preserve the same instant across the round trip. Do not silently lose sub-millisecond precision through a frontend `Date` and then submit it as an exact value/version.

## Business computation and frontend display

- Each time-dependent business rule names its zone source: store, tenant, explicit application configuration or a request zone authorized by the contract, with precedence/fallback and invalid/missing-zone behavior defined by that rule. Do not use OS/JVM/database defaults or the viewer's browser zone as implicit business authority. A fixed global zone is acceptable when explicitly required and configured.
- Run JVM and DB sessions in UTC where deployment scope permits, but still use explicit `ZoneId` for calendar operations. Obtain current instants through the existing injected `Clock` (normally `Clock.systemUTC()`); distinguish elapsed `Duration` from calendar arithmetic. Do not mutate global default zones per request.
- For date filters and day/week/month reports, compute local start and next local boundary in the business zone, convert both to instants, and query `[start, end)`. Do not add a fixed 24 hours to find the next calendar day; DST days can differ. SQL grouping must use the same explicit business zone. Do not store extra display-time copies.
- Resolve daylight-saving gaps and overlaps explicitly for local appointment/schedule input: reject or use a documented business policy; do not silently accept Java's default adjustment. Specify recurrence behavior and whether zone-rule changes recompute future occurrences when relevant.
- Frontend display converts the UTC instant exactly once using an explicit user preference or a documented browser fallback. Reports/accounting views use and label the authoritative business zone. Formatting never changes stored values or server-side business dates. Submit pure dates as date strings, not midnight timestamps; local schedule submissions carry/resolve the business zone. Never manually add/subtract fixed hours after parsing an offset-bearing value.

## Existing contracts and scope

Inspect current types, physical columns, serialization and data provenance before choosing a change. Existing UTC-by-convention `LocalDateTime`/`timestamp without time zone` (for example logical-deletion fields) is `Legacy UTC`, not a new-field default. Record the enforced UTC interpretation, read/write conversion, source-zone evidence and compatibility strategy. Do not claim the current template/model is fully compliant merely from this reference.

Unchanged legacy contracts remain unchanged within the current task. When an authorized change must correct a column or historical data, add the repository-approved next migration; never edit an existing migration, infer unknown historical zones, automatically shift all rows, or treat a skill update as permission to migrate. Unknown provenance or incompatible effective Specs is a blocker requiring an explicit decision.

## Contract table and blocking checks

New Specs (Version 9) retain §6.3; new Plans (Version 5) retain §4.9. Set `Time contract: Required` for affected time semantics/boundaries, otherwise `Time contract: Not applicable` with exact scope evidence. Plans consume the effective Spec's time decisions; missing material decisions go back to the Spec rather than being invented by the Plan.

Use one `TIME-NNN` row per semantic group with identical boundaries; name exact fields/symbols and split any group with different policies. Canonical table tokens remain English in documents of either language:

- Semantics: `Occurred instant`, `Pure date`, `Local time`, `Zoned schedule`, `Duration`.
- Storage policy: `UTC instant`, `Pure date`, `Local time`, `Local + zone`, `Duration`, `Legacy UTC`, `Not persisted`.
- Wire policy: `UTC Z`, `Pure date`, `Local`, `Local + zone`, `Duration`, `Internal` (no affected wire boundary).
- `Not applicable: <reason>` is allowed for an irrelevant business/display zone, precision/clock or compatibility cell; bare `N/A` is insufficient. Zoned schedules must name a region-zone source. `Legacy UTC` must explain the existing contract and conversion in Compatibility.

`MC-TIME-001` must evidence semantic classification, actual persistence/driver/JSON round trips, explicit business-zone computation, frontend display/submission, precision/Clock and legacy compatibility for affected boundaries. Spec verification names concrete `TEST-*` cases in §14; Plan verification names owning `Step N`, tests/commands in §8 and per-file obligations. Execution records actual results and required runtime gaps; no automatic application/database/browser startup.

Use only applicable checks: same instant across UTC/Shanghai/New York environments, `Z` output and offset input, missing-zone rejection, pure-date stability, business midnight/month boundaries, DST gaps/overlaps and calendar-day range, precision round trips, and legacy UTC mapping. Mock/H2 checks do not prove PostgreSQL mappings. Structural validators check rows and policy conflicts, not whether prose, migrations or runtime behavior is correct; manual evidence remains blocking.

Sources: [PostgreSQL date/time semantics](https://www.postgresql.org/docs/current/datatype-datetime.html), [pgJDBC Java time mapping](https://jdbc.postgresql.org/documentation/query/#using-java-8-date-and-time-classes).
