# Backend code generation and dependency gate

Effective: 2026-09-22. Read this for Java dependency decisions and DDL-based CRUD/template work. It supplements `references/egon-java-cqe-contract.md` and applies in Spec, Plan, execution and final review.

## Dependency approval is mandatory

Do not autonomously add, upgrade, replace or download dependencies, build plugins, annotation processors, template engines or generator tooling. A BOM-managed version or availability in another module is not authorization to add it here. This applies to internal Egon module dependencies as well as third-party ones; reuse already available module capabilities first.

Generated code uses the selected Egon-COLA components, archetype and platform contracts and the JDK. Existing framework types supplied by those approved dependencies (Spring, Jakarta Validation, Lombok, Jackson, MapStruct and MP) remain valid; this rule does not require eliminating the existing framework stack. Do not add MyBatis-Plus Generator, RuoYi, Velocity, a new SQL parser or an external runtime merely to generate code. The user's final correction on 2026-09-22 selects Apache FreeMarker instead of Velocity: org.freemarker:freemarker is approved for the generator tool only, with the plan pinning 2.3.35. No engine dependency belongs in generated applications. This does not authorize other dependencies; do not ask again for this approved FreeMarker introduction.

When current dependencies cannot satisfy a requirement:

1. Mark the affected work `BLOCKED_DEPENDENCY` under `MC-DEP-001` and stop dependent edits/execution. Continue independent authorized work.
2. Show the required capability and current module/POM/API evidence, existing Egon alternatives and exact gap.
3. Present concrete options: extend an existing component, add a named internal component dependency, or propose a named external dependency as an exception. State coordinates/version owner, target modules, transitive/build/runtime impact and validation.
4. Ask the user to choose/approve before modifying a manifest or fetching/installing the proposed tool. A self-written Spec/Plan is not approval. A previously explicit approval of those exact dependencies remains sufficient; do not ask again.
5. Until approval, do not weaken standards, silently vendor a library, invent a large replacement infrastructure or claim generated output is compliant.

## Generator availability and scope

The verified offline generator is `scripts/egon-codegen.sh`. It supports native profiles `light`, `web` and `service`, FreeMarker 2.3.35, and the commands `templates`, `plan`, `check`, `apply` and `recover`. Operating notes are in the generator module README next to that script. The launcher requires an explicit local `EGON_CODEGEN_CLASSPATH`. A missing Java executable or classpath entry returns `BLOCKED_TOOLING` and does not download dependencies or start an application. Agent, open and traditional profiles remain unsupported.

The CLI now expands `backend-crud` through PO/DAO/Mapper XML/Repository, Domain model and Service/implementation, Command/Query/Result/converters, Manage/implementation, and a Controller for native Light/Web. Native Service has no HTTP Controller. `logicalTables` must be explicit; selecting a narrower artifact list never widens the write scope. Confirm the target project already provides every required referenced type and API/error boundary before applying. The generated Domain Service implementation calls named Repository methods for queries and writes; it must not call `getBaseMapper()` or a DAO directly. The Repository owns Mapper/XML access.

The DDL must contain the inherited `EgonModel` id, tenant, audit, nullable `deleted_at` and version columns. Verify their types, but generate only business columns in the PO class. Mapper XML includes the inherited fields. Do not add those shared declarations to a subclass merely because they appear in SQL.

Plans use formatVersion 2. Review the file inventory before apply: an existing path with no generator state is `CONFLICT`, as is a user-edited or intentionally removed generated file. The CLI re-reads config and DDL and independently checks template, generator and state fingerprints before writing; a v1 plan or stale input must be replanned. Do not use a handwritten patch to bypass these protections.

For an approved template task, call `plan`, inspect the JSON summary, selected files and conflicts, then `apply` only the authorized actions. Read the machine summary and diff. Do not retype generated boilerplate. A missing dependency or absent classpath still blocks; do not invent a replacement generator.

Generate supported repetitive backend scaffolding through that command. The model supplies generation configuration and business decisions, reviews its diff, and writes only business logic/custom queries not covered by templates. Do not add empty adapters, base classes or services merely to exercise a template.

The intended initial profiles are native Light (single-module monolith), Web and Service. Agent is excluded. Existing traditional three-layer packages are preserved; never reinterpret “monolith” as permission to move a traditional project into Light DDD. A selected profile must match current POMs, packages and architecture checks. Open variants need explicit support and dependency verification, not silent fallback to native templates.

## SQL changes refresh catalog output only through the generator

On native `light`, `web`, and `service`, a classpath SQL change under `src/main/resources/db/` does not authorize the model to write or patch FreeMarker catalog output. The model writes the next SQL script, its MP-SDJ manifest entry, and the generator config. `scripts/egon-codegen.sh` writes the catalog files.

Catalog artifacts, excluding the test-only `probe` and `unsafe` entries in `egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/catalog.json`:

`po`, `dao`, `mapper-xml`, `repo`, `domain-model`, `domain-query`, `command`, `query`, `result`, `converter`, `domain-service`, `domain-impl`, `manage`, `manage-impl`, and `controller` (`controller` only for `light` and `web`).

`persistence-crud` stays `po,dao,mapper-xml,repo`. A wider set is allowed only when the Spec selected it. A narrower request must not be widened.

Order:

1. Add the next SQL version and manifest. Do not edit an applied script.
2. Point the generator config at that schema or manifest, the logical tables, and the selected artifacts.
3. Run `plan`. Read the JSON summary, conflicts, and destructive changes.
4. `apply` only the authorized file actions.
5. Append the classpath SQL journal in this reference.
6. The model may then edit only business behavior and custom queries the catalog does not own, and only in files the generator left untouched.

Do not hand-write those catalog types so the SQL will compile. Do not copy `.ftl` into the business project. Do not edit a `.ftl` to fit one schema. Do not treat Plan Java pseudocode as permission to retype generator output. `CONFLICT` or `BLOCKED_TOOLING` stops the work; it is not a cue to finish the file by hand.

`agent`, every `-open` archetype, and an existing traditional three-layer tree are outside this generator. Do not imitate the catalog by hand. Keep an existing traditional tree (Rule 11). Ask before moving that work onto native `light`, `web`, or `service`. A new business project is created by `egon-coding-create-new-module` from exactly one non-open archetype (`light`, `service`, `web`, or `agent`). Do not copy `source-projects` and do not write a module skeleton.

## DDL input, selective output and change safety

- DDL input is read-only. Distinguish a full schema snapshot from an ordered MP-SDJ SQL/manifest history. Preserve applied version/checksum prefixes and do not run DDL or connect to a database without separate authorization.
- Resolve logical tables and physical shard mappings from actual MP-SDJ topology; never turn each physical suffix into a business class or guess a shard key from a column name.
- Record exact output root, module/package mapping, table selection and requested artifacts. DAO means Java Mapper interface; mapper XML is a separate artifact. Normalize ambiguous CLI names using the actual tool's documented contract.
- Generate only selected artifacts. Missing referenced PO/DAO/contract dependencies cause a scoped diagnostic; never silently widen a dao-only request to repo/service/controller or alter the POM.
- Plan first: schema differences, exact output files, component/type dependencies, baseline hashes, custom-file conflicts and destructive changes. Apply only within the authorized scope, verifying the plan still matches input and disk state.
- Repeat generation is deterministic and has no diff. Update only files owned by the generator whose contents match the last generated hash. Unknown files, user-edited files, changed custom XML and manual business methods must not be overwritten, deleted or auto-merged.
- Renames, drops, type/nullability changes and contract changes require an explicit impact decision. Use explicit rename mappings, never infer rename from similar names. A file outside the selected output range remains untouched; report affected consumers as pending rather than claiming project-wide convergence.
- Inherited MP Repository CRUD is reused, not copied or overridden. Generate the explicit Mapper XML required by the actual EgonColaMapper contract and typed, named query/page methods when needed. Keep optimistic version, tenant, soft deletion and routing semantics intact.

## Classpath SQL journal

After every skill-driven generator `apply` that succeeds, and after a `plan` when that plan is the last authorized generator action, append one entry to `docs/egon/codegen/ddl-consumption-log.md`. This records classpath SQL under `src/main/resources/db/` that the run used as generator input. It is not `ddl_history` and does not mean the SQL was executed on PostgreSQL. Do not connect to a database to fill it.

For that run record, in order: the Asia/Shanghai time, profile, repository-relative output root, and the highest script version consumed (`throughVersion`). Then one block per script in manifest or filename order: version token from the filename (`V20260913_001` or the same token without the leading `V`), repository-relative path, SHA-256 of the file bytes, and the full SQL text. If the run used no `resources/db` script, record `throughVersion: none` and say so; do not invent SQL.

Append only. Do not rewrite or delete earlier entries. Consuming the same version again adds a new timed entry. A missing journal file is created from the header already stored there before the first entry.

## Phase evidence

| Phase | Required result |
| --- | --- |
| Spec | DDL source/type, supported profile, templates, generated/custom ownership, CRUD/API semantics, dependency availability and change/conflict policy. Catalog types are generator-owned, not handwritten classes |
| Plan | Exact configuration, executable discovery, artifact scope, output paths, required existing types, expected diff, generator/component/template versions, validations and approvals. Catalog paths are `GENERATED`; their pseudocode is the config and `plan`/`apply`, not a Java body |
| Execute | Run the verified generator for approved templates, inspect scoped diff, preserve custom code, compile/test the affected output; append the classpath SQL journal; block missing tooling/dependencies rather than inventing them. Do not type catalog files after a SQL change |
| Final audit | Reconcile DDL diff, selected/generated files, unresolved consumers/conflicts, manifest/dependency changes, validation results and runtime limits |

Map these concerns to `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-MODEL-001`, `MC-SCOPE-001` and `MC-TEST-001`. An unresolved applicable concern cannot PASS.

## 中文审核要点

- 禁止自行引入、升级或下载依赖、插件、注解处理器和生成器工具；BOM 已管理不等于授权。内部依赖新增也须有明确授权，已有明确批准不重复询问。
- 现有依赖不足时阻断受影响工作，提交能力缺口、内部复用方案、引入坐标/版本/模块/传递影响和验证方案，由用户决定；不能先改 POM 再说明。
- 已验证命令是 `scripts/egon-codegen.sh`，支持 `templates`、`plan`、`check`、`apply`、`recover`。只对获准的 native Light/Web/Service 范围调用；先看计划摘要和冲突，再 apply。缺 classpath 返回 `BLOCKED_TOOLING`，不得下载依赖或手写替代模板。Agent、Open 和传统三层仍不支持。
- native light/web/service 上，`src/main/resources/db/` 的 SQL 变动不能由模型改目录产物。模型只写下一版 SQL、Manifest 和生成器配置；`po`、`dao`、`mapper-xml`、`repo`、`domain-model`、`domain-query`、`command`、`query`、`result`、`converter`、`domain-service`、`domain-impl`、`manage`、`manage-impl`，以及 light/web 的 `controller`，只由生成器刷新。`CONFLICT` 或 `BLOCKED_TOOLING` 时停止，不能手写补完。不要把 `.ftl` 抄进业务工程，也不要为单份 Schema 改 `.ftl`。
- 新业务项目由 `egon-coding-create-new-module` 从唯一的非 open archetype（`light`、`service`、`web`、`agent`）生成。不要复制 `source-projects`，不要手写模块骨架。已有传统三层保持现状；agent、open、传统三层不能靠手写 native 目录产物绕过生成器。
- 明确 Light/Web/Service、DDL 来源、逻辑表映射、输出根目录、精确产物范围；只生成 DAO 不得自动扩展到其他层。Agent 暂不支持，传统三层结构不迁移。
- 重生成按基线/hash 管理，只更新未被人工修改的自有文件；不覆写自定义代码，不执行数据库 DDL，不自行添加依赖。
- skill 驱动的生成在 `apply` 成功后，或本次只授权到 `plan` 时，向 `docs/egon/codegen/ddl-consumption-log.md` 追加一条。记下 Asia/Shanghai 时间、profile、输出根、用到的 `src/main/resources/db/` 脚本版本（执行到的最高版本）、路径、SHA-256 和完整 SQL。这不是数据库执行记录，不连接数据库。只追加，不改旧条目；没有 db 脚本就记 `throughVersion: none`。

用户于 2026-09-22 最终明确改为 FreeMarker，替代前一条 Velocity 选型。仅授权工具依赖 org.freemarker:freemarker（方案固定2.3.35），生成业务工程不增加引擎依赖，其他依赖仍须逐项批准。

当前 CLI 的 `backend-crud` 会连通 Repo → Domain Service/实现 → Manage/实现 → Light/Web Controller；Service 不生成 HTTP Controller。查询和写入都经 Repository，Domain Service 实现不得直接调用 Mapper/DAO。`logicalTables` 明确选择。DDL 中的 EgonModel 通用列仅校验并映射，PO 继承基类且只生成业务列。v2 计划保护无归属文件、人工更改和旧输入；v1 计划须重新生成，不能手改目录文件绕过冲突。
