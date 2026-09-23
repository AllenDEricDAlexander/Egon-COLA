# egon-cola-component-code-generator

[English](README.md) | [中文](README.zh-CN.md)

An offline development tool. It reads PostgreSQL DDL or an MP-SDJ manifest and generates the backend
CRUD for the native Light, Web and Service project types. It does not start Spring, does not connect
to a database, and does not modify the target project's POM.

`org.freemarker:freemarker:2.3.35` is the only approved template engine, and it exists only inside
this tool. Generated business projects do not depend on FreeMarker.

## Build and classpath

Run from the repository root:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-code-generator -am install -DskipTests
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-code-generator dependency:build-classpath -DincludeScope=runtime -Dmdep.outputFile=/tmp/egon-codegen.cp
```

The first command uses `install` instead of `package` so the upstream modules land in the local
repository; the second one omits `-am`, otherwise every module in the reactor overwrites the same
`/tmp/egon-codegen.cp` and the result depends on build order.

Prepend this module's `target/classes` to that classpath, then export it:

```bash
export EGON_CODEGEN_CLASSPATH="egon-cola-components/egon-cola-component-code-generator/target/classes:$(cat /tmp/egon-codegen.cp)"
```

`scripts/egon-codegen.sh` only consumes an `EGON_CODEGEN_CLASSPATH` that has already been provided:
it runs no Maven, no curl, and starts no application. The Java executable is selected in the order
`JAVA`, then `JAVA_HOME/bin/java`, then `java` on `PATH`. Missing Java, an empty
`EGON_CODEGEN_CLASSPATH`, or any entry of that classpath that does not exist on disk all print
`{"code":"BLOCKED_TOOLING",...}` to standard error and exit with code 7.

## Commands

```text
scripts/egon-codegen.sh templates
scripts/egon-codegen.sh plan --config codegen.json
scripts/egon-codegen.sh check --config codegen.json
scripts/egon-codegen.sh apply --plan <plan-id> --config codegen.json
scripts/egon-codegen.sh recover --config codegen.json
```

On success the JSON object is the last line of standard output. This module ships no logging
configuration, so a Logback default console appender on the classpath also writes log lines (for
example the Hibernate Validator version line) to standard output; discard everything before the JSON
when parsing. Errors and diagnostics go only to standard error, as `{"code":...,"message":...}`.
Exit codes: 0 success or no change, 2 configuration or syntax, 3 missing type or non-canonical
input, 4 manual conflict, 5 unconfirmed destructive change, 6 stale plan, 7 recovery or IO,
8 check found drift awaiting synchronization.

`check` never touches project files; it compares each rendered result with the content on disk.
`apply` is all-or-nothing: when the preflight finds a `CONFLICT` it stops before writing any file
and returns 4. Hand-edited files stay as they are, and only files whose disk content still equals
the previously generated content are replaced. Exit code 5, `DESTRUCTIVE_ACTION`, covers `DELETE`
and `RENAME` actions that need explicit acceptance, but `plan` only emits `CONFLICT`, `NO_CHANGE`,
`ADD` and `UPDATE`, and the command line passes no acceptance set, so that branch is unreachable in
the current CLI.

`backend-crud` generates the complete base chain: PO, DAO, Mapper XML, Repository, domain model and
Domain Service/implementation, Command/Query/Result, converters, Manage/implementation; Light and
Web additionally generate Controllers, and Service generates no HTTP entry point.
`persistence-crud` expands to only the first four items. `artifacts` can be selected one by one, and
the aliases `pojo`→`po` and `mapper`→`mapper-xml` are normalized first. When a lower-level artifact
is omitted, `existingTypeMappings` must declare the type that already exists: a missing `po` or
`dao` returns `MISSING_TYPE` (exit code 3). The generator enforces only those two; the
`requiredTypes` of the remaining catalog entries take no part in validation today, and the
generation scope never widens on its own. Both queries and writes keep Mapper/XML access inside the
Repository, and a Domain Service implementation must not call a DAO directly. `logicalTables` must
be stated explicitly and matched one by one against the logical tables in the DDL; a table name that
does not exist fails immediately.

In the configuration JSON, `configVersion` must be `1`, `projectType` is one of
`light`/`web`/`service`, and unknown keys are always rejected (deserialization enables
`FAIL_ON_UNKNOWN_PROPERTIES`). `input.mode` has exactly two values: `schema` replays `schemaFiles`
in order, `manifest` reads the `manifest` path below `resourceRoot`. A manifest looks like
`{"family":...,"scripts":[{"version":...,"path":...,"sha256":...}]}`; script bytes that do not match
the recorded `sha256`, or a changed prefix among the already recorded versions, return
`CHECKSUM_DRIFT`.

The DDL must define the id, tenant, audit, `deleted_at` and version columns that `EgonModel`
requires, and the generator validates their types and null semantics. A PO extends `EgonModel` and
declares only business fields; the Mapper XML still maps the inherited fields. The generator never
re-declares those common fields on the PO.

The plan format is currently v2. A same-named file with no generation record, a hand-edited file,
and files added or changed after the plan was cut all return conflicts; nothing is adopted or
overwritten automatically. `apply` re-checks the fingerprints of the configuration, DDL, templates,
tool code and generation state, and preflights every target file under a lock on the same output
directory. A v1 plan needs a fresh `plan` run.

`plan`, `apply` and `recover` maintain only `.egon/codegen/` below the output root:
`plans/<plan-id>.json` is written by `plan`, `state.json` is updated by `apply`, the journal inside
it records the interrupted phase so `recover` can roll back, and `apply.lock` is the write lock.
Deleting that directory strips the generator of every file-ownership record, after which all
same-named files are judged as conflicts.

Current acceptance covers compiling from source and the isolated tests:
`./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-code-generator -am test`. It does not
mean PostgreSQL, ShardingSphere or MQ have run in a real environment.
