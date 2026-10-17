# `scripts/` 目录说明

> 适用路径：`scripts/README.md`
> 目的：区分「长期工具」「仓库不变量守卫」「工具回归套件」「本地栈脚本」「agent 运行时脚本」，避免一次性 harness 堆积在顶层。每个被跟踪的脚本都必须在本文登记；新增、移动或删除脚本时同步更新对应表格。

---

## 1. 分层规则

```text
scripts/
├── <长期工具>                    # 人和 CI 直接调用的入口，路径即契约
├── checks/                       # 仓库不变量守卫：纯静态、秒级、可反复跑
├── regression/                   # 长期工具与守卫的回归套件
├── work/                         # agent 运行时脚本区（默认 gitignore，见第 6 节）
├── unified-identity-local.sh     # 本地四平台栈（第一代单体）
└── unified-xingyuan/             # 本地四平台栈（第二代拆分）与隔离联调入口
```

判断一个新脚本该放哪：

| 问题 | 放哪 |
|---|---|
| 人或 CI 会直接敲它吗？ | `scripts/` 顶层 |
| 它校验的是「仓库当前必须一直成立」的约束，且不需要 fixture？ | `checks/` |
| 它保护的是某个已存在长期工具或守卫的行为？ | `regression/` |
| 它是本次 Plan 临时写出来、只为验收某一步？ | `work/` |
| 它跟本地栈生命周期绑（要起进程、连库）？ | `unified-xingyuan/` |

---

## 2. 顶层长期工具

| 脚本 | 作用 | 前置条件 | 谁在引用 |
|---|---|---|---|
| `generate_archetypes.sh` | 两段式 archetype 生成：`source-projects` → `.generated` → `definitions`；子命令 `generate` / `check` | `./mvnw`、JDK 21 | CI 写死：`.github/workflows/publish-maven-central.yml` 的 `generate` + `check` 门；`egon-cola-archetypes/pom.xml` 的缺失提示；skill `egon-coding-create-new-module` |
| `bump_cola_version.sh` | 全仓库版本号递增（pom、`.generated`、派生工件同步） | `./mvnw` | 根 README；`maven-deploy.md`；`regression/test-bump-cola-version.sh` |
| `maven-deploy.sh` / `maven-deploy.md` | Maven Central 发布编排（Sonatype Central Portal）；`.md` 是它的操作说明 | 发布凭据；内部调用 `generate_archetypes.sh` | 根 README；`regression/test-archetype-release.sh` |
| `egon-codegen.sh` | 离线代码生成器 launcher（`templates`/`plan`/`check`/`apply`/`recover`） | 必须显式提供 `EGON_CODEGEN_CLASSPATH`，缺失返回 `BLOCKED_TOOLING` 且退出码 7 | **4 个 `egon-coding-*` skill 逐字写死 `scripts/egon-codegen.sh`**，作为 native light/web/service 上 GENERATED 文件的唯一允许入口；`regression/test-egon-codegen.sh` |
| `link-skills.sh` | 把本仓库 `.agents/skills/` 下的每个 skill 各建一条软链接进**其他**项目的 `.agents/skills/`，替代跨仓库复制副本；`--prune` 回收上游改名/删除后失效的旧链 | 目标只接受绝对路径、且必须是自身 git 仓库根（worktree 与别人仓库的子目录一律拒绝）；同名真实副本仅在「已被目标仓库跟踪且干净」时替换，未跟踪/有改动的一律 `blocked` 退出码 2，需 `--force` 才丢弃；`--dry-run` 全量预览 | `regression/test-link-skills.sh`；CI 与其他脚本不引用 |

> 顶层禁止再新增一次性脚本。`egon-codegen.sh` 与 `generate_archetypes.sh` 的路径已被 skill 与 CI 写死，**重命名或移动前必须先改这些引用方**。

---

## 3. `checks/` — 仓库不变量守卫

纯静态解析 POM/源码，无 fixture、无网络，秒级完成。Python 守卫的 `--root` 默认取仓库根（脚本自身上两级）。

| 脚本 | 守的约束 |
|---|---|
| `check-archetype-dependency-ownership.py` | 各 source archetype 的 parent 必须是 `top.egon:egon-cola-archetypes-parent:<version>`、`relativePath` 留空、版本用 `${egon-cola.version}`、BOM 归属与跨家族 facade 消费关系 |
| `check-archetype-family-boundaries.py` | 七族的声明与模块边界：Open / Agent 依赖白名单与 Agent 技术 SDK 所在模块；共享 facade 已退休；facade 族自发布 Protobuf 契约；各族模块拓扑；持久化边界——禁用旧 MP API、domain 不依赖 MP、具体 Repository 数（service 族 5、其余 8）、每族恰好一份 MP-SDJ `repository-manifest.json`，脚本数（light 为初始化 + 已批准的 forward ALTER 共 2 个，其余 1 个）及逐个 checksum；Agent 要求恰好一条 forward model correction |
| `check-native-archetype-boundaries.py` | native 家族禁止出现 dubbo / nacos / springdoc 传输栈与注解、必须使用 curated native rpc 工件、禁止引用退休的共享 facade 包（冻结的 Protobuf wire 包除外） |
| `check-admin-web-controller-coverage.sh` | 四个管理平台（授柄、鉴神、玉衡、天枢）的 `Exposure.EXTERNAL` Controller 与各自 Admin Web 的 `/api/` 引用做静态盘点，并断言若干关键修复仍在源码中；调用同目录的 `controller_ui_coverage.py` 判定是否有未解释的管理方法。需要 `rg`。只是静态源码盘点，不证明运行中的进程已加载新构建 |

`controller_ui_coverage.py` 是上一行的 Python 辅助模块，不单独作为入口；它的单元测试在 `regression/test-controller-ui-coverage.py`。

---

## 4. `regression/` — 长期工具与守卫的回归套件

全部在 `mktemp` 目录里自建 fixture，不污染工作树。除特别说明外不需要真实 Maven 构建，耗时 2 秒到 1 分钟。

| 脚本 | 保护对象 | 运行方式 |
|---|---|---|
| `test-generate-archetypes.sh` | `generate_archetypes.sh`（确定性、遗留归档哈希、发布接线） | 默认只跑 `unit`；另有 `generation`、`package reactor`、`release` 三种模式。workflow 接线断言只在 `release` 模式下执行，所以改 CI 后它不会自动变红 |
| `test-archetype-release.sh` | `maven-deploy.sh` 的发布形态（假仓库、fake stage） | 无参数全跑；注入假 `mvnw` |
| `test-bump-cola-version.sh` | `bump_cola_version.sh`（fixture 仓库树哈希、失败与非法版本路径） | 无参数全跑；注入假 `mvnw` |
| `test-egon-codegen.sh` | `egon-codegen.sh` 背后生成器（light / web / service plan→apply→compile） | 需要 `EGON_CODEGEN_CLASSPATH`，缺失时按契约返回 `BLOCKED_TOOLING`（退出码 7） |
| `test-link-skills.sh` | `link-skills.sh` 的软链语义（dry-run 不写盘、可逆副本替换、`.git/info/exclude` 记账与回收、不可逆副本需 `--force`、失效链 `--prune` 清理、自指与嵌套仓库拒绝） | 无参数全跑；只造目录与 git fixture |
| `test-controller-ui-coverage.py` | `checks/controller_ui_coverage.py` 的 Java/Web 盘点与分类 | `python3 scripts/regression/test-controller-ui-coverage.py`，任意 cwd 均可 |
| `test-agent-archetype-verifier.py` | Agent archetype 的 `verify.groovy` 策略（对已生成工程的源码/报告做变异，库策略用仅含条目的归档） | 需要 `--project <已验证/打包的生成 Agent 工程>` 与 `--groovy-classpath <groovy、groovy-xml jar>`；不启动应用、数据库或模型 |
| `test-service-open-archetype-verifier.py` | Service Open archetype 的 `verify.groovy` 测试报告策略 | 需要 `--project <已构建的生成 Service Open 工程>` 与 `--groovy-classpath <groovy、groovy-xml、groovy-json jar>`；只变异报告元数据，不启动 Triple 监听 |
| `test-isolated-integration-contract.sh` | `unified-xingyuan/isolated-integration.sh` 九个公开命令的参数、opt-in、owner guard 与受控准备/恢复契约 | 无参数全跑；以 stub 替换 `psql`/`createdb`/`redis-cli`/`nginx`/`java`/`node`/`npm`/`mvnw`，零外部设施。其内部模拟的 case 不计作真实联调 |

> **CI 目前不跑 `checks/` 与 `regression/`。** 这两类是 agent Manual Check 与人工门。凡是「必须长期成立」的约束，只写成脚本迟早会静默失效——要么接进 `.github/workflows/`，要么就接受它会烂。

> 2026-09-23 起仓库只剩 `ci-backend.yml` 与 `ci-frontend.yml` 两条验证流水线，二者都不接 archetype 生成。`publish-maven-central.yml` 是唯一仍运行 `generate_archetypes.sh generate` + `check` 的 workflow，因此 **archetype 源码到生成物的漂移只在发布时才被兜住**。

---

## 5. `unified-*` — 本地四平台栈

两代并存。所有会访问设施或启动进程的命令只由操作者执行，操作说明见[本机联调手册](../docs/runbooks/unified-identity-local.md)和[MCP 本机手册](../docs/operations/unified-identity-mcp-local-runbook.md)。

### 5.1 第一代：`unified-identity-local.sh`

2735 行单体。第二代仍回调它：`start-local-stack.sh` 调用 `issue-user-token`、`oauth-context`、`write-service-registration`、`start`、`publish-yuheng-routes`，`verify-local-stack.sh` 与 `stop-local-stack.sh` 也各自调用它，所以单体不能直接删。

### 5.2 第二代：`unified-xingyuan/`

| 脚本 | 作用 | 备注 |
|---|---|---|
| `prepare-local-stack.sh` | 运行用户授权的本地编排准备并停止受管进程 | 会实际访问设施，不是静态检查 |
| `start-local-stack.sh` | 启动完整本地栈 | 回调第一代单体 |
| `status-local-stack.sh` | 只读查看本地栈状态 | |
| `stop-local-stack.sh` | 停止本地栈 | 回调第一代单体 |
| `verify-local-stack.sh` | 对运行中的本地栈做验收 | 需要栈已启动 |
| `lib/common.sh` | 上述脚本共享的函数库 | 只被 `source`，不单独执行 |
| `fixtures/` | `tianquan-jianshen-bootstrap.json`、`unified-xingyuan-release.json` | 被 `start-` / `verify-local-stack.sh` 与 `test-direct-run-contract.sh` 读取 |
| `test-direct-run-contract.sh` | 对本地栈脚本做静态契约断言（必须包含 / 不得包含的调用与参数） | 不起进程 |
| `test-live-frontend-login.sh` | 对运行中的前端做真实登录验收 | 需要栈已启动 |
| `isolated-integration.sh` + `lib/isolated-integration.sh` + `lib/isolated_integration.py` | 隔离联调入口，见 5.3 | |

**一次性迁移工具**（完成历史迁移用，保留以备其他环境复用；没有新的迁移需求时不要调用）：

| 脚本 | 作用 | 备注 |
|---|---|---|
| `cleanup-legacy-identity-keys.sh` | 清理无状态 JWT 改造前遗留的身份运行时 Redis 键 | 默认 dry-run；`--execute` 具破坏性且必须显式 `--endpoint`；从不扫描或清空整个 Redis 库。被 `test-direct-run-contract.sh` 引用 |
| `migrate-tenant-authority.sh` | 鉴神 → 授柄的 tenant-authority 导出 / 导入 / 校验 / 报告 | 只在受控切流流程中使用，启动脚本不会自动执行它 |
| `test-tenant-authority-migration.sh` | `migrate-tenant-authority.sh` 的契约回归 | 用 `psql` shim，不连真实数据库 |

**在跑完本地栈相关 Plan 之前不要重命名第一代单体与 `unified-xingyuan/` 下的栈脚本**：`docs/egon/plan/2026-09-23-06-16-tianshu-multitenant-ai-implementation.md`（`In Progress`）已把它们列进未完成 Step 的 commit paths。

### 5.3 隔离联调入口

`isolated-integration.sh` 只接受九个公开命令：`prepare`、`provision-identity`、`start-control-plane`、`provision-authorization`、`start-consumers`、`authorize-cli`、`verify`、`status`、`stop`。Bash 负责严格参数和 opt-in，`lib/isolated-integration.sh` 转交同目录标准库 `isolated_integration.py`；后者不是额外的公开 CLI。`status` 只读，其余命令要求 `XINGYUAN_ISOLATED_INTEGRATION=true`；入口限定任务专属的九个新库、Redis DB1–15、HTTPS 28443 和冻结进程表，禁止 Docker，不代替已有的旧栈脚本。

完整命令、两个 authorization phase、保护文件、退出码和恢复规则见[本机联调手册第 7 节](../docs/runbooks/unified-identity-local.md#7-新隔离接入-harnessstep17-实施中)及[Runtime Spec](../docs/egon/spec/2026-10-11-10-26-xingyuan-native-full-integration-runtime.md)。本入口还带有三项私有能力，均不对外暴露：

- `native-build-provenance`：只执行显式离线源码导出与全七族生成 / 检查 / 构建，把四种 native 真实制品、安全测试与三个 Open 族的保留事实写入私有证明；`start-consumers` 在任何启动前要求这份 SHA 冻结的证明。
- file-only profile 盘点：读取 SHA 绑定的 12 个实际 Boot JAR，只报告配置键 / 引用名与未解析标记；不生成完整运行配置，也不启动应用。
- `formal-revoke` 桥：只服务既有测试的精确 AT 摘要请求，通过固定 SA owner selector 和实际 JUnit 证明确认撤销；不得作为任意目标的撤销工具。

所有 task manifest、secret、token、PID、运行配置与证据都保留在被忽略的任务运行目录里，不提交仓库。真实联调尚未执行过，见[实施 Plan §12.128](../docs/egon/plan/2026-10-11-12-03-xingyuan-native-full-integration-plan.md)。

---

## 6. `work/` — agent 运行时脚本区（生命周期）

`.gitignore` 已忽略本目录内容（只保留 `.gitkeep`）。Plan 期间 agent 新写的验收 harness 一律落在 `work/`，命名 `<plan-id>-<verb>.sh`，默认不入版本库。

Plan 收尾（final audit）时，`work/` 里每个文件必须被显式三选一：

1. **升格**：确实会反复用 → 脚本移进 `checks/` 或 `regression/` 并在第 3、4 节补一行；长期有效的知识整理成正式文档。然后才 `git add`。
2. **保留为本地栈契约**：需要起进程/连库 → 移进 `unified-xingyuan/`，并在第 5 节补一行。
3. **删除**：不会复用 → 直接删掉。git 历史不需要为一次性 harness 留档。

不允许的收尾状态：`work/` 里还留着文件却没人判断过。

> 例：原先放在这里的 `archetype-readme-frozen-tokens.md` 已升格为 [`egon-cola-archetypes/docs/generated-readme-contract.md`](../egon-cola-archetypes/docs/generated-readme-contract.md)。

---

## 7. 命名约定

新增脚本用 kebab-case（`maven-deploy.sh`、`check-*.py`、`test-*.sh`）。例外只有两类：

- `bump_cola_version.sh` 与 `generate_archetypes.sh` 是历史 snake_case，因 CI 与 skill 已写死，不改名。
- 会被其他 Python 文件 `import` 的辅助模块用 snake_case（`checks/controller_ui_coverage.py`、`unified-xingyuan/lib/isolated_integration.py`），因为带连字符的文件名无法作为模块导入。
