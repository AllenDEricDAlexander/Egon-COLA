# `scripts/` 目录说明

> 适用路径：`scripts/README.md`
> 目的：区分「长期工具」「仓库不变量守卫」「工具回归套件」「agent 运行时脚本」，避免一次性 harness 堆积在顶层。

---

## 1. 分层规则

```text
scripts/
├── <长期工具>                    # 人和 CI 直接调用的入口，路径即契约
├── checks/                       # 仓库不变量守卫：纯静态、秒级、可反复跑
├── regression/                   # 上面那些长期工具的回归套件
├── work/                         # agent 运行时脚本区（默认 gitignore，见第 4 节）
├── unified-identity-local.sh     # 本地四平台栈（第一代单体）
└── unified-xingyuan/             # 本地四平台栈（第二代拆分）
```

判断一个新脚本该放哪：

| 问题 | 放哪 |
|---|---|
| 人或 CI 会直接敲它吗？ | `scripts/` 顶层 |
| 它校验的是「仓库当前必须一直成立」的约束，且不需要 fixture？ | `checks/` |
| 它保护的是某个已存在长期工具的行为？ | `regression/` |
| 它是本次 Plan 临时写出来、只为验收某一步？ | `work/` |
| 它跟本地栈生命周期绑（要起进程、连库）？ | `unified-xingyuan/` |

---

## 2. 顶层长期工具

| 脚本 | 作用 | 前置条件 | 谁在引用 |
|---|---|---|---|
| `generate_archetypes.sh` | 两段式 archetype 生成：`source-projects` → `.generated` → `definitions`；子命令 `generate` / `check` | `./mvnw`、JDK 21 | CI 写死：`.github/workflows/publish-maven-central.yml` 的 `generate` + `check` 门；skill `egon-coding-create-new-module` |
| `bump_cola_version.sh` | 全仓库版本号递增（pom、`.generated`、派生工件同步） | `./mvnw` | `regression/test-bump-cola-version.sh` |
| `maven-deploy.sh` / `maven-deploy.md` | Maven Central 发布编排（Sonatype Central Portal） | 发布凭据；内部调用 `generate_archetypes.sh` | 见 `maven-deploy.md` |
| `egon-codegen.sh` | 离线代码生成器 launcher（`templates`/`plan`/`check`/`apply`/`recover`） | 必须显式提供 `EGON_CODEGEN_CLASSPATH`，缺失返回 `BLOCKED_TOOLING` 且退出码 7 | **4 个 `egon-coding-*` skill 逐字写死 `scripts/egon-codegen.sh`**，作为 native light/web/service 上 GENERATED 文件的唯一允许入口 |
| `link-skills.sh` | 把本仓库 `.agents/skills/` 下的每个 skill 各建一条软链接进**其他**项目的 `.agents/skills/`，替代跨仓库复制副本；`--prune` 回收上游改名/删除后失效的旧链 | 目标只接受绝对路径、且必须是自身 git 仓库根（worktree 与别人仓库的子目录一律拒绝）；同名真实副本仅在「已被目标仓库跟踪且干净」时替换，未跟踪/有改动的一律 `blocked` 退出码 2，需 `--force` 才丢弃；`--dry-run` 全量预览 | `regression/test-link-skills.sh`；CI 与其他脚本不引用 |

> 顶层禁止再新增一次性脚本。`egon-codegen.sh` 与 `generate_archetypes.sh` 的路径已被 skill 与 CI 写死，**重命名或移动前必须先改这些引用方**。

---

## 3. `checks/` 与 `regression/`

### `checks/` — 仓库不变量守卫

纯静态解析 POM/源码，无 fixture、无网络，秒级完成。`--root` 默认取仓库根（脚本自身上两级）。

| 脚本 | 守的约束 |
|---|---|
| `check-archetype-dependency-ownership.py` | 各 source archetype 的 parent 必须是 `top.egon:egon-cola-archetypes-parent:<version>`、`relativePath` 留空、版本用 `${egon-cola.version}`、BOM 归属与跨家族 facade 消费关系 |
| `check-archetype-family-boundaries.py` | open / agent 家族的依赖声明边界（公共 id/mybatis-plus/thread-pool starter 白名单、外部 nacos/spring-cloud 白名单、AI 依赖集合、共享 facade 已退休） |
| `check-native-archetype-boundaries.py` | native 家族禁止出现 dubbo / nacos / springdoc 传输栈与注解、必须使用 curated native rpc 工件、禁止引用退休的共享 facade 包（冻结的 Protobuf wire 包除外） |

### `regression/` — 长期工具的回归套件

第 2 节那 5 个顶层工具各配一个。全部在 `mktemp` 目录里自建 fixture 仓库，不污染工作树、不需要真实 Maven 构建，耗时 2 秒到 1 分钟；除 `test-link-skills.sh` 只造目录与 git fixture 外，其余都注入**假 `mvnw`**。`test-egon-codegen.sh` 额外要求 `EGON_CODEGEN_CLASSPATH`，缺失时按契约返回 `BLOCKED_TOOLING`（退出码 7）。

| 脚本 | 保护对象 |
|---|---|
| `test-generate-archetypes.sh` | `generate_archetypes.sh`（确定性、遗留归档哈希、发布接线） |
| `test-archetype-release.sh` | `maven-deploy.sh` 的发布形态（假仓库、fake stage） |
| `test-bump-cola-version.sh` | `bump_cola_version.sh`（fixture 仓库树哈希、失败与非法版本路径） |
| `test-egon-codegen.sh` | `egon-codegen.sh` 背后生成器（light / web / service plan→apply→compile） |
| `test-link-skills.sh` | `link-skills.sh` 的软链语义（dry-run 不写盘、可逆副本替换、`.git/info/exclude` 记账与回收、不可逆副本需 `--force`、失效链 `--prune` 清理、自指与嵌套仓库拒绝） |

运行模式：`test-generate-archetypes.sh` 默认只跑 `unit`，另有 `generation`、`package reactor`、`release` 三种；workflow 接线断言只在 `release` 模式下执行，所以改 CI 后它不会自动变红。其余四个套件无参数即全跑。

> **CI 目前不跑 `checks/` 与 `regression/`。** 这两类是 agent Manual Check 与人工门。凡是「必须长期成立」的约束，只写成脚本迟早会静默失效——要么接进 `.github/workflows/`，要么就接受它会烂。

> 2026-09-23 起仓库只剩 `ci-backend.yml` 与 `ci-frontend.yml` 两条验证流水线，二者都不接 archetype 生成。`publish-maven-central.yml` 是唯一仍运行 `generate_archetypes.sh generate` + `check` 的 workflow，因此 **archetype 源码到生成物的漂移只在发布时才被兜住**；上面两个 archetype 套件的 CI 接线断言也只针对该 workflow。

---

## 4. `work/` — agent 运行时脚本区（生命周期）

`.gitignore` 已忽略本目录内容（只保留 `.gitkeep`）。Plan 期间 agent 新写的验收 harness 一律落在 `work/`，命名 `<plan-id>-<verb>.sh`，默认不入版本库。

Plan 收尾（final audit）时，`work/` 里每个脚本必须被显式三选一：

1. **升格**：确实会反复用 → 移进 `checks/` 或 `regression/`，按第 3 节补一行表格，然后才 `git add`。
2. **保留为本地栈契约**：需要起进程/连库 → 移进 `unified-xingyuan/`。
3. **删除**：不会复用 → 直接删掉。git 历史不需要为一次性 harness 留档。

不允许的收尾状态：`work/` 里还留着脚本却没人判断过。

---

## 5. `unified-*` — 本地四平台栈有两代并存

- `unified-identity-local.sh`：2582 行单体，操作说明在 `docs/runbooks/unified-identity-local.md`。
- `unified-xingyuan/`：第二代拆分（`start-` / `stop-` / `status-` / `prepare-` / `verify-local-stack.sh` + `lib/common.sh` + `fixtures/`）。

第二代仍回调第一代三次（`start-local-stack.sh` 的 `issue-user-token`、`start`、`publish-yuheng-routes`），所以单体不能直接删。**在跑完本地栈相关 Plan 之前不要重命名这两处路径**：`docs/egon/plan/2026-09-23-06-16-tianshu-multitenant-ai-implementation.md` 已把它们列进未完成 Step 的 commit paths。

`unified-xingyuan/` 下的 `test-direct-run-contract.sh`、`test-live-frontend-login.sh`、`test-tenant-authority-migration.sh` 虽然叫 `test-`，但与本地栈生命周期绑定、由该 Plan 引用，属于第 1 节表格最后一行，**不是** `work/` 类一次性 harness，保持原位。

---

### 当前隔离接入入口（Step17 实施中）

`unified-xingyuan/isolated-integration.sh` 只接受九个公开命令：`prepare`、`provision-identity`、`start-control-plane`、`provision-authorization`、`start-consumers`、`authorize-cli`、`verify`、`status`、`stop`。Bash 负责严格参数和 opt-in，`lib/isolated-integration.sh` 转交同目录标准库 `isolated_integration.py`；后者不是额外的公开 CLI。

完整命令、两个 authorization phase、保护文件、退出码和恢复规则见[联调手册第7节](../docs/runbooks/unified-identity-local.md#7-新隔离接入-harnessstep17-实施中)及[Runtime Spec](../docs/egon/spec/2026-10-11-10-26-xingyuan-native-full-integration-runtime.md)。入口限定本任务新九库、Redis DB1–15、HTTPS28443 和冻结进程表，禁止 Docker；status 只读，其余命令要求 `XINGYUAN_ISOLATED_INTEGRATION=true`。它不代替已有旧栈脚本。

`bash scripts/regression/test-isolated-integration-contract.sh` 运行默认无外部设施的合同回归；受控传输/临时流水线输出不能当成24项 LIVE 已通过。原生授权phase及native/resource两个完整组合runner已有真实owner链源码，路由/MCP细权限/协议审计/异步上下文与最后保留门禁已接线；Step17源码审查与实际构建见Plan最新执行记录，真实联调仍未执行。原生目录、鉴神准入/注册/审核及角色分配按既定CLI004native顺序调用；独立子阶段COMMITTED不能当作完整授权通过。私有 `formal-revoke` 桥只服务既有测试的精确 AT 摘要请求，通过固定 SA owner selector 和实际 JUnit 证明确认撤销；不得作为任意目标撤销工具。

所有 task manifest、secret、token、PID、运行配置与证据都保留在忽略的任务运行目录，不提交仓库；停止仅 TERM 经归属核验的任务进程，保留数据。UNKNOWN_COMMIT 应先只读核实，不盲重跑，不 DROP/FLUSHDB/repair。

prepare会在首次任务修改前保存原DB身份、nginx进程/监听和非任务Redis库键摘要基线，恢复及最终owned stop后严格复核。Redis只读采集需要7+和现有读取权限，原键仅允许已到原定时间的自然过期；不可读、超限、原值/TTL/服务身份漂移都失败。before缺失不能补造；三种局部proof不代表完整LIVE-023或旧服务健康。默认回归只使用受控CLI，不访问实际PG/Redis/nginx。 另有显式file-only profile盘点，读取SHA绑定的12个实际Boot JAR，报告仅配置键/引用名与未解析标记；它不生成完整运行配置或启动应用。

---

## 6. 命名约定

新增脚本用 kebab-case（`maven-deploy.sh`、`check-*.py`、`test-*.sh`）。`bump_cola_version.sh` 与 `generate_archetypes.sh` 是历史 snake_case 例外，因 CI 与 skill 已写死，不改名。

私有 `native-build-provenance` 只执行显式离线源码导出、原全七族生成/检查/构建，四种native真实制品与安全测试、三Open保留事实写入私有证明；`start-consumers`在任何启动前要求该SHA冻结证明。命令、保护文件和当前源码变化处理见[本机手册](../docs/runbooks/unified-identity-local.md#7-新隔离接入-harnessstep17-实施中)。它不启动设施或补造LIVE运行证据。
