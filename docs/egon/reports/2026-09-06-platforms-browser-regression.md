# Platforms 本地浏览器回归记录（2026-09-06）

## 结论与范围

本轮完成本地 `platforms` 启动、浏览器主流程检查、已复现缺陷修复及对应回归。最终检查时，6 个后端进程和 5 个前端进程均运行，健康检查均为 HTTP 200；服务保留运行，便于继续人工测试。

这不是生产部署、全部功能穷举测试或 MCP 全协议兼容认证。按照用户本轮确认，保留现有 Egon Tasks 扩展并明确标注，不升级为完整标准 Tasks。

| 入口 | 地址 |
| --- | --- |
| 统一入口 | <http://127.0.0.1:18125> |
| IdP | <http://127.0.0.1:18121> |
| RBAC3 | <http://127.0.0.1:18131> |
| Gateway Admin | <http://127.0.0.1:18141> |
| DDC | <http://127.0.0.1:18152> |
| Gateway HTTP/RPC | <http://127.0.0.1:18180> |
| MCP 数据面 | <http://127.0.0.1:18185/mcp/qa-browser-0906> |

健康检查记录：`target/local-unified-platform/evidence/final-platform-health.log`。

## 已验证的关键流程

| 模块 | 实际验证 | 结果 |
| --- | --- | --- |
| 统一入口 | 刷新嵌入式 DDC 页面并读取最新服务目录 | 正常加载，包含独立 MCP 引擎 |
| Gateway / DDC | 浏览器编辑 QA Prompt、校验、统一发布到 API_RPC 与 MCP 两个角色 | Release `01a075968d017ff38069db34361881df` 为 SUCCESS；运行一致性为 CONSISTENT |
| RBAC3 | 新用户绑定、限时角色任职、候选角色、主动选择角色 | 独立用户只有指定 QA 角色，无平台管理员角色 |
| RBAC3 | 初次激活前及仅激活业务角色后的自身信息、候选角色、当前角色接口 | 均为 200，仅提供自身上下文与角色选择能力 |
| RBAC3 | 独立用户访问用户管理、委托管理 | 均为 403，未因自助入口修复获得管理能力 |
| RBAC3 | 委托策略编辑、管理对象查询、保存后的能力刷新 | 只返回指定 QA 用户和角色；页面计数自动刷新 |
| MCP | initialize、initialized、resources/list/read、prompts/list/get、tools/list、ping | Resource 和 Prompt 各一项；工具列表为空；Prompt 参数实际完成替换 |
| MCP 撤权 | 同一个 MCP 会话内撤销 Prompt 权限，再恢复 | 撤销后列表隐藏，直接调用返回 MCP_FORBIDDEN；Resource 仍可读；恢复后 Prompt 可用，未重新激活角色 |
| DDC | 独立配置创建、编辑、历史查询、回滚 | v1 `first` → v2 `second` → v3 回滚为 `first`，历史保留 |
| DDC | 向无在线客户端的测试应用发布配置 | 正确返回 `success=false / DDC_NO_LIVE_INSTANCE`，不是发布成功 |
| IdP | 浏览器按测试用户筛选审计 | 全部 44 条收敛为该用户 2 条 |
| IdP | 实际 PostgreSQL 上组合筛选、分页总数、时间边界、Trace ID、无效时间 | 组合筛选 1 条，排除条件 0 条，分页总数正确，无效时间返回 400 |

撤权测试曾遇到测试 Access Token 到期的 401；该响应没有被计作撤权成功。完成正常 Token 刷新后，仍沿用原 MCP 会话，观察到包含 `RBAC3_PERMISSION_DENIED`、`authVersion=3`、`policyVersion=10` 的明确拒绝。恢复后策略版本为 11，授权版本仍为 3。

主要证据位于 `target/local-unified-platform/evidence/`：

- `qa-mcp-same-session-revoked-*.json`、`qa-mcp-same-session-restored-*.json`
- `mcp-isolated-qa-final.log`、`mcp-isolated-qa-initialize.json`
- `qa-user-about.json`、`qa-user-role-activations.json`
- `qa-manageable-users.json`、`qa-manageable-roles.json`
- `qa-ddc-config.json`、`qa-ddc-no-live-target.json`
- `qa-idp-audit-*.json`

## 本轮主要修复

- 授权资源变更进入已有持久化事件队列；恢复器根据数据库当前事实重建用户已选角色，不自动激活其他候选角色。
- Redis 快照原子发布、版本水位单调校验、数据库版本复核及授权缓存失效；失效资格要求重新选择角色。
- 补齐首激活自助入口的后端权限、Gateway 路由和 React SDK 状态处理；仅激活业务角色后仍可管理自己的角色选择。
- 可管理用户查询不再引用已删除的用户名、显示名列；返回 RBAC 用户 ID 与 IdP 身份标识。
- 修复委托策略时间编辑及保存、禁用后的能力和管理对象缓存刷新；补齐本地初始化缺失的管理权限，限制初始化只授权明确配置的资源。
- Prompt 编辑器、预览与运行时统一使用 `${参数名}`。
- DDC 配置列表和变更确认明确展示业务域、环境、应用，消除多个同名 `application.yml` 的目标歧义。
- IdP 审计六项条件进入数据库筛选；时间参数转为绝对 ISO 时间，无效参数返回 400。
- 恢复器通过已有发布端口访问存储；补齐包文档及过时的合同测试，未降低架构或鉴权断言。

代表性提交：`85bf78e56`、`3dc6996ac`、`ecb0047b8`、`7c1f22d0d`、`06a73d973`、`03f6a38e0`、`d7b1633ae`、`41b9721cc`、`ed7c74faa`、`6e4fce501`。各独立修复已分别提交，未打包提交原有无关改动。

## 自动化验证

| 验证项 | 最终结果 | 日志 |
| --- | --- | --- |
| RBAC3 前端 | 19 文件 / 57 测试通过 | `rbac-full-web-tests.log` |
| Gateway 前端 | 30 文件 / 137 测试通过 | `gateway-full-web-final.log` |
| IdP 前端 | 10 文件 / 37 测试通过 | `idp-full-web-final.log` |
| DDC 前端 | 21 文件 / 57 测试通过 | `ddc-full-web-final.log` |
| 四个前端 TypeScript | 全部通过 | `*-final-typecheck.log` |
| RBAC3 Admin 普通 `*Test` 测试集 | 216 项中 211 通过，5 项 Redis 集成测试按默认开关跳过；无失败 | `rbac-full-admin-final.log` |
| 显式连接本地 Redis 的集成测试 | 上述 5 项另行全部通过；只使用独立测试键，不清库 | `rbac-final-real-redis-tests.log` |
| MCP Core + Engine | 34 + 42 测试通过，打包成功 | `mcp-experimental-tasks-green.log` |
| IdP 审计服务与 MVC 合同 | 9 测试通过，打包成功 | `idp-audit-filter-green.log` |
| 直接运行脚本合同 | 通过 | `final-direct-run-contract.log` |
| Git 差异检查 | `git diff --check` 通过 | — |

前端合计 288 项。日志中的 jsdom `getComputedStyle` 提示是测试环境限制，不是被忽略的失败；新增断言最初的失败记录保留，最终结果以上表为准。

## 扩展边界与保留数据

Stable 初始化将现有 Tasks 声明在 `capabilities.experimental["top.egon/tasks"]`，并明确 `standardTaskAugmentation=false`；没有声明标准 Tasks 能力。`tasks/get`、`tasks/update`、`tasks/cancel` 及既有持久化策略保持不变，未实现 `tasks/list`、`tasks/result`。RC 发现保持既有方言描述。该区分依据 [MCP Tasks 契约](https://modelcontextprotocol.io/specification/2025-11-25/basic/utilities/tasks) 和 [初始化能力协商](https://modelcontextprotocol.io/specification/2025-11-25/basic/lifecycle)。

本轮没有启动可选的完整测试拓扑（mock Provider、Remote MCP fixture、额外 API 副本），未穷举远端工具调用、Tasks 创建全生命周期、Apps 宿主互操作、生产 HA 或性能压测。Resource/Prompt 测试成功不应外推为这些能力全部通过。

收尾验证没有重跑 RBAC3 全部 PostgreSQL、迁移及并发 `*IT`。上表中的普通测试、明确列出的 Redis 集成测试、实际浏览器/API 检查是各自独立的证据，不能替代未执行的测试集。

保留了独立 QA 身份、成员、角色、委托策略及 DDC 标记配置以便复查。QA 任职与 Business 授权的有效期配置到当天 18:00（Asia/Shanghai）；本轮验证的是显式撤权传播，未进行时间推进的过期回收测试。QA 角色现已恢复为原来的两项只读资源。凭据仅保存在本地受限文件中，不包含在本报告或 Git 提交中。

后续可直接通过统一入口进行人工验收；无需再次初始化或清空本轮数据。无关的 Archetype、Spec/Plan 和共享前端构建状态改动均保留原状。本轮由主代理独立完成，未使用子代理。
