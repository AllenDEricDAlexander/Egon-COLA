---
name: egon-coding-create-new-module
description: 用唯一的非 open Egon-COLA Maven archetype 生成一个真实业务项目，或为多个生成项目设计/创建明确要求的外层聚合 POM，并说明新项目如何使用 Egon-COLA 组件与平台依赖。用户要求新建模块、询问父 POM 或初始依赖关系，或运行 /egon-coding-create-new-module 时使用。不选 -open，不复制 source-projects，不手写业务骨架。不替代 write-spec、write-plan 或 exec-plan。
---

# 用非 open archetype 生成业务项目

> 本文件是 `SKILL.md` 的全中文审核镜像，不是自动发现入口。修改任一版本时必须同步另一版本。选型表只在 `references/archetype-selection.md`。

用户要求业务项目时，用 `archetype:generate` 生成一个 Maven 项目。仅创建脚手架的请求以创建结果和样例清理交接结束；同一请求已包含真实业务设计/实施时，在已有授权内把项目交给 `egon-coding-writing-spec`，遵守它的审核门禁。仅询问/要求外层父 POM 时，先区分聚合与继承，不顺带生成业务项目。创建阶段本身不写 Spec、Plan、业务代码、SQL 或 codegen 产物。

## 何时使用

用户要一个还不存在的业务项目/模块，或询问聚合多个生成项目的外层 POM 时使用。

修改已有业务代码时改用 `egon-coding-writing-spec`。用户明确要求的外层聚合 POM 可按 `references/multi-project-parent.md` 创建或更新，不在已有业务项目旁重复生成一棵树。

## 步骤

1. 生成业务项目时完整读取 `references/archetype-selection.md`。涉及外层父 POM、多项目 reactor、Maven 继承或生成项目引入 Egon Component/Platform 依赖时，还要完整读取 `references/multi-project-parent.md`。
2. 先判断是独立生成项目、纯聚合外层 reactor，还是明确需要共享父 POM 的继承。独立生成项目已自带根 POM；默认外层 reactor 不设 `<parent>`，生成项目继续直接继承 `egon-cola-archetypes-parent`。
3. 要生成时从选型表选择且只选择一个 archetype；两个都合适才询问。只要求父 POM 时跳过 archetype 生成。
4. 一次问齐实际创建所必需且无法从现有文件确定的信息：生成项目的 GAV、包名、输出父目录；外层 reactor 的业务 GAV 与准确子项目目录。Web/Service 的对端 Facade 四项坐标可以全不传；若要传入，必须四项齐全。不得编造对端 Facade。
5. 从 `egon-cola-archetypes/pom.xml` 读取版本。生成业务项目时在 Egon-COLA 仓库根使用 `./mvnw`；只回答父 POM 时不运行生成命令。
6. archetype 无法解析时给出 Maven 错误；本地安装前询问用户。不要发布或运行 `scripts/generate_archetypes.sh`。
7. 完成适用的生成检查及外层 reactor 检查；失败时报告证据，不靠改写生成项目的父 POM 强行通过。
8. 报告聚合/继承关系；发生生成时再报告 archetype GAV/版本、输出路径、拓扑、codegen 适用范围以及下述样例清理交接。仅创建请求到此完成；已经要求业务工作时继续进入 Spec skill，不要求用户再调用一个清理 skill。

## 新业务项目如何使用 Egon 模块

按 `references/multi-project-parent.md` 的位置表处理：外层 reactor 只列业务项目根目录，不把 `egon-cola-components`、`egon-cola-xingyuan` 源码列入 `<modules>`；生成项目根 POM 继承 archetypes parent 的依赖管理，包括 Components BOM 和部分指定的平台工件；实际使用某能力的内部模块才声明对应 `<dependency>`。被管理版本不等于已加入 classpath，纯聚合 POM 的依赖管理也不会传给未继承它的生成项目。已有项目新增或升级依赖应走 Spec/Plan/Execute 的批准流程，本建项目 skill 不代替它修改业务模块 POM。

## 模型不写这些

- 不复制 `egon-cola-archetypes/source-projects`。
- 不改 `definitions` 或 `.generated`。
- generate 失败时不手写生成项目的 `pom.xml`、包或模块骨架。用户明确要求的外层纯聚合 `pom.xml` 按 `references/multi-project-parent.md` 单独处理。
- 不选择、也不改名 `-open` archetype。
- 用户没要求时，不把项目加进外层 reactor POM。单纯聚合不修改生成项目的 `<parent>`。
- 不启动进程、数据库或浏览器。

native `light`、`web`、`service` 仅在初始 CREATE 建表时用 `scripts/egon-codegen.sh` 生成模板；之后的 ALTER 等结构变更由 agent 按获批 Plan 修改代码。`agent` 是合法的非 open archetype，但不在生成器范围内；不要为它编造持久化模板。

## 示例与首次业务请求

生成后检查实际携带的样例链路，报告准确路径/入口注册、来源 GAV/版本和样例是否仍存在。仍有样例时标记 `Example cleanup: Pending business implementation`；脚手架生成完成不代表业务就绪。不能按固定业务名词编造样例清单。

首次真实业务 Spec 必须检查整个目标生成项目的全部模块，包括与首个功能无关的未使用样例域。`egon-coding-writing-spec` 在 §2.5 清单逐项写 Delete、Replace、Real business、Infrastructure、Applied history 或 Blocked。获批 Plan 必须在首个真实业务功能验收前删除/替换全部未使用的可执行样例，不能以“参考”理由保留。已部分实施但仍有遗留的项目同样适用。

保留真实应用需要的启动/技术接线和已有真实需求证据的业务代码。样例 SQL 仅在未应用时移除，已应用历史只能新增纠正版本。范围、数据和契约有疑问时在 Spec 中解决，不按名称猜测删除。本规则只处理用户生成的业务项目，不删除 Egon-COLA 维护的 `source-projects`、`definitions` 或 `.generated` 中的参考代码。

## 交接

生成成功后，下一步是对生成项目使用 `egon-coding-writing-spec`，把来源和待清理样例证据带入它的清理评估。用户已在本次请求中要求业务工作时继续进入该 skill；否则只交接并结束创建请求。无需单独的清理 skill。

## 时间契约交接

检查生成项目的真实时间类型、JDBC/JSON 映射、基础及环境配置时区、SQL 列声明，报告已观察路径和差距；不得假定所有 archetype 已符合 UTC，也不得在仅创建脚手架阶段修复业务代码/配置/SQL。向 write-spec 交接：发生时刻 UTC 存储并用 ISO 8601 UTC `Z` 传输，纯日期/本地时间/时长保留自身语义，计算/展示使用明确业务/用户时区。既有约定 UTC 的逻辑删除字段作为有证据的兼容决策保留；首份业务 Spec 定义受影响转换和 MC-TIME-001 检查。例外或历史纠正必须进入获批业务范围，不能自动迁移或编辑旧脚本。
