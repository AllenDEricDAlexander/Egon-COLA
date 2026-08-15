# EGON Coding Spec / Plan Skills

本目录包含两个仅面向 coding 场景的仓库级 skill：

- `$egon-coding-writing-spec`：基于当前仓库编写系统架构、概要设计或详细设计 Spec，输出到 `docs/egon/spec`。
- `$egon-coding-writing-plan`：基于一份明确 Spec 编写有严格文件顺序和语言化伪代码的实施 Plan，输出到 `docs/egon/plan`。

每个 skill 都提供两份入口内容：

- `SKILL.md`：全英文运行版，供 Codex 在开发中实际加载。
- `SKILL.zh-CN.md`：全中文审核镜像，不作为自动发现入口；修改规则时必须与英文版同步。

## 典型工作流

```text
$egon-coding-writing-spec 为当前编码需求编写技术 Spec
用户审核并明确接受 Spec
$egon-coding-writing-plan 基于 docs/egon/spec/2026-08-15-14-30-example-design.md 编写实施 Plan
用户审核 Plan 后再进入编码
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
- skill 只写设计文档，不会顺带开始编码或启动项目。
