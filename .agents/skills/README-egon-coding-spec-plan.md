# EGON Coding Spec / Plan / Execution Skills

本目录包含四个仅面向 coding 场景的仓库级 skill：

- `$egon-coding-create-new-module`：用一个非 open 的 `egon-cola-archetype-{light,service,web,agent}` 生成真实业务项目。不选手写骨架，不选 `-open`。
- `$egon-coding-writing-spec`：基于当前仓库编写系统架构、概要设计或详细设计 Spec，输出到 `docs/egon/spec`。
- `$egon-coding-writing-plan`：基于一份明确 Spec 编写有严格文件顺序和语言化伪代码的实施 Plan，输出到 `docs/egon/plan`。
- `$egon-coding-executing-plan`：基于一份已获准执行的 Plan 逐 Step 修改、验证并提交代码；每个 Step 必须形成独立提交，最后独立审核实现是否满足全部有效 Spec。

每个 skill 都提供两份入口内容：

- `SKILL.md`：全英文运行版，供 Codex 在开发中实际加载。
- `SKILL.zh-CN.md`：全中文审核镜像，不作为自动发现入口；修改规则时必须与英文版同步。

## 典型工作流

```text
新业务项目：$egon-coding-create-new-module 选择唯一非 open archetype 并 archetype:generate
仅创建脚手架时交接 archetype 来源与待清理示例；同一请求已有业务工作则继续进入 Spec
$egon-coding-writing-spec 为当前编码需求编写技术 Spec，并在首次业务工作时盘点整个目标项目示例
用户审核并明确接受 Spec
$egon-coding-writing-plan 基于 docs/egon/spec/2026-08-15-14-30-example-design.md 编写实施 Plan
用户审核并授权执行 Plan
$egon-coding-executing-plan 基于 docs/egon/plan/2026-08-15-15-10-example-implementation.md 分步实现
在首次真实业务验收前落实 Spec §2.5 全部示例删除/替换，保留已证实的真实业务、基础设施及已应用 SQL 历史
每个 Step：修改 -> 验证 -> 立即路径受限提交；核验提交哈希后才能开始下一 Step，禁止最后再拆分提交
全部 Step 提交后，独立对照有效 Spec 审核并汇报未满足项
```

## 文档路径与命名

```text
docs/egon/spec/YYYY-MM-DD-HH-MM-ABSTRACT.md
docs/egon/plan/YYYY-MM-DD-HH-MM-ABSTRACT.md
```

`ABSTRACT` 使用简洁的小写 ASCII kebab-case 摘要。同一分钟不能用相同摘要覆盖已有文档。

## 核心治理规则

- Spec 使用统一 RFC 风格 Header，通过 `Amends`、`Supersedes`、`Depends On` 和 `Related Specs` 引用当前或旧目录中的既有设计。
- Plan 必须用相对路径明确引用 `Implements Spec`，并列出完整 `Effective Specs`。
- 重大需求、契约、数据、安全、架构、迁移或兼容歧义必须找用户确认；小型、本地、可逆缺口依据仓库证据最小推断。
- Spec 必须覆盖架构、分包与文件树、接口、实体、数据库、前端、设计模式/架构理念、测试和追踪矩阵。
- Plan 必须逐 Step 写明先处理哪个文件、真实符号、针对当前语言/框架的伪代码、完成该文件后的状态、验证、回滚和提交边界。
- 两份文档都必须在交付前复核：Spec 对照原始需求和仓库；Plan 对照原始需求、有效 Spec 和当前仓库。
- Spec 和 Plan skill 只写设计文档，不会顺带开始编码。
- 无需另建示例清理 skill：创建 skill 交接来源，write-spec Version 8 必填 §2.5 与 `Archetype Cleanup`，Plan/Execute 消费整份 `EXAMPLE-*` 清单；已生成或部分实施项目也触发。仅按当前功能局部清理或“留作参考”都不能验收通过。
- Execute Plan skill 同一时间只允许一个 Step 处于执行中；Step 只有达到 `Verified` 并形成已核验的 `Committed` 提交后才算完成。
- Execute Plan skill 使用路径受限暂存与提交，保留所有无关工作；禁止空提交、跨 Step 合并提交和自动改写历史。
- 最终 Spec 审核必须逐项标记 `Satisfied`、`Partial`、`Not satisfied` 或 `Runtime unverified`；发现缺口只汇报，不静默追加未计划实现。
- 除非用户明确要求，任何 skill 都不会自动启动项目、数据库、浏览器、部署或外部运行环境。

## 后端模板生成与依赖审批

三个设计/实施 skill 各自带一份必须保持相同的 `references/backend-code-generation.md`：不允许自行引入依赖，缺口必须阻断并提交用户审批方案。后端生成器入口是 `scripts/egon-codegen.sh`，只负责 native Light/Web/Service 的初始 `CREATE TABLE` 建表模板；独立 `CREATE INDEX` 由 agent 审核，不参与生成器建模；`ALTER` 等后续结构变更由 agent 按获批 Spec/Plan 修改代码，不再用生成器刷新。FreeMarker 2.3.35 只属于生成器工具。Agent、Open 和传统三层不在 CLI 范围内，保持各自现有架构。Repository → DAO 只校验数据库表结构约束，业务规则在上层。脚手架自带代码仅供参考，真实业务实施 Step 必须删除或替换相应样例。每次 skill 驱动的初始生成还要把用到的 `src/main/resources/db/` SQL 追加到 `docs/egon/codegen/ddl-consumption-log.md`；该文件不是数据库执行记录。

- 时间规范同步中英文：Spec Version 9 §6.3、Plan Version 5 §4.9 记录 TIME 契约，MC-TIME-001 验证发生时刻 UTC 存储/传输、纯日期语义、显式业务时区计算与前端展示转换；创建 skill 只交接真实模板证据。既有 UTC LocalDateTime 单列兼容，skill 更新不授权修改业务/数据库/历史迁移。旧版本文档按原结构契约校验。
