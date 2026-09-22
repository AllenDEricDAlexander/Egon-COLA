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

The DDL backend generator is currently a proposal, not an installed command. First discover its actual executable, supported project profiles, template inventory and component compatibility. Never execute a command copied from a proposal as though it exists. If tooling is absent, report `BLOCKED_TOOLING` for template generation and request its implementation or an explicit alternative; do not silently spend tokens recreating the covered boilerplate. Design and dependency review can continue.

Once the generator is implemented and verified, generate supported repetitive backend scaffolding through it. The model supplies generation configuration and business decisions, reviews its diff, and writes only business logic/custom queries not covered by templates. Do not add empty adapters, base classes or services merely to exercise a template.

The intended initial profiles are native Light (single-module monolith), Web and Service. Agent is excluded. Existing traditional three-layer packages are preserved; never reinterpret “monolith” as permission to move a traditional project into Light DDD. A selected profile must match current POMs, packages and architecture checks. Open variants need explicit support and dependency verification, not silent fallback to native templates.

## DDL input, selective output and change safety

- DDL input is read-only. Distinguish a full schema snapshot from an ordered MP-SDJ SQL/manifest history. Preserve applied version/checksum prefixes and do not run DDL or connect to a database without separate authorization.
- Resolve logical tables and physical shard mappings from actual MP-SDJ topology; never turn each physical suffix into a business class or guess a shard key from a column name.
- Record exact output root, module/package mapping, table selection and requested artifacts. DAO means Java Mapper interface; mapper XML is a separate artifact. Normalize ambiguous CLI names using the actual tool's documented contract.
- Generate only selected artifacts. Missing referenced PO/DAO/contract dependencies cause a scoped diagnostic; never silently widen a dao-only request to repo/service/controller or alter the POM.
- Plan first: schema differences, exact output files, component/type dependencies, baseline hashes, custom-file conflicts and destructive changes. Apply only within the authorized scope, verifying the plan still matches input and disk state.
- Repeat generation is deterministic and has no diff. Update only files owned by the generator whose contents match the last generated hash. Unknown files, user-edited files, changed custom XML and manual business methods must not be overwritten, deleted or auto-merged.
- Renames, drops, type/nullability changes and contract changes require an explicit impact decision. Use explicit rename mappings, never infer rename from similar names. A file outside the selected output range remains untouched; report affected consumers as pending rather than claiming project-wide convergence.
- Inherited MP Repository CRUD is reused, not copied or overridden. Generate the explicit Mapper XML required by the actual EgonColaMapper contract and typed, named query/page methods when needed. Keep optimistic version, tenant, soft deletion and routing semantics intact.

## Phase evidence

| Phase | Required result |
| --- | --- |
| Spec | DDL source/type, supported profile, templates, generated/custom ownership, CRUD/API semantics, dependency availability and change/conflict policy |
| Plan | Exact configuration, executable discovery, artifact scope, output paths, required existing types, expected diff, generator/component/template versions, validations and approvals |
| Execute | Run the verified generator for approved templates, inspect scoped diff, preserve custom code, compile/test the affected output; block missing tooling/dependencies rather than inventing them |
| Final audit | Reconcile DDL diff, selected/generated files, unresolved consumers/conflicts, manifest/dependency changes, validation results and runtime limits |

Map these concerns to `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-MODEL-001`, `MC-SCOPE-001` and `MC-TEST-001`. An unresolved applicable concern cannot PASS.

## 中文审核要点

- 禁止自行引入、升级或下载依赖、插件、注解处理器和生成器工具；BOM 已管理不等于授权。内部依赖新增也须有明确授权，已有明确批准不重复询问。
- 现有依赖不足时阻断受影响工作，提交能力缺口、内部复用方案、引入坐标/版本/模块/传递影响和验证方案，由用户决定；不能先改 POM 再说明。
- 未来已支持的后端模板必须通过已验证生成器产生，模型负责配置、业务规则与差异审核。当前生成器尚是方案，不得虚构可用命令或宣称已接入。
- 明确 Light/Web/Service、DDL 来源、逻辑表映射、输出根目录、精确产物范围；只生成 DAO 不得自动扩展到其他层。Agent 暂不支持，传统三层结构不迁移。
- 重生成按基线/hash 管理，只更新未被人工修改的自有文件；不覆写自定义代码，不执行数据库 DDL，不自行添加依赖。

用户于 2026-09-22 最终明确改为 FreeMarker，替代前一条 Velocity 选型。仅授权工具依赖 org.freemarker:freemarker（方案固定2.3.35），生成业务工程不增加引擎依赖，其他依赖仍须逐项批准。
