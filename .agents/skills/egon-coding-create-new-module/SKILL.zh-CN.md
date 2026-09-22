---
name: egon-coding-create-new-module
description: 用唯一的非 open Egon-COLA Maven archetype 生成一个真实业务项目。用户要求新建模块、从 egon-cola-archetypes 生成项目，或运行 /egon-coding-create-new-module 时使用。不选 -open，不复制 source-projects，不手写骨架。不替代 write-spec、write-plan 或 exec-plan。
---

# 用非 open archetype 生成业务项目

> 本文件是 `SKILL.md` 的全中文审核镜像，不是自动发现入口。修改任一版本时必须同步另一版本。选型表只在 `references/archetype-selection.md`。

用 `archetype:generate` 生成一个 Maven 项目，然后停下来给用户审核。本 skill 不写 Spec、Plan、业务代码、SQL 或 codegen 产物。

## 何时使用

只在用户要一个还不存在的业务项目或模块时使用。

如果改的是已经存在的项目，停止并改用 `egon-coding-writing-spec`。不要在旁边再生成一棵树。

## 步骤

1. 完整读取 `references/archetype-selection.md`。允许的 artifactId、禁止的 `-open`、必填属性、版本来源、输出位置、命令形态和生成后检查都在那里。
2. 从该表选择且只选择一个 archetype。两个都合适时，生成前询问。用户已经点名一个允许的 archetype 时，直接用它。
3. `groupId`、`artifactId`、`version`、`package`、输出父目录，或必填的对端 facade 坐标缺任何一项时，一次问齐。不要编造对端 facade，也不要把对端默认成 `source-projects` 里的示例契约。
4. 从 `egon-cola-archetypes/pom.xml` 读取 archetype 版本。在 Egon-COLA 仓库根用 `./mvnw` 执行 `references/archetype-selection.md` 里的命令。
5. archetype 无法解析时停止并出示 Maven 错误。任何本地安装之前先问用户。不要发布。不要运行 `scripts/generate_archetypes.sh`。
6. 执行 `references/archetype-selection.md` 的生成后检查。失败时报告差异，不要用手改树。
7. 停止。报告 archetype、版本、输出路径、拓扑，以及后续 codegen 适用（`light`、`web`、`service`）或不适用（`agent`）。

## 模型不写这些

- 不复制 `egon-cola-archetypes/source-projects`。
- 不改 `definitions` 或 `.generated`。
- generate 失败时不手写 `pom.xml`、包或模块骨架。
- 不选择、也不改名 `-open` archetype。
- 用户没要求时，不把项目加进 reactor POM。
- 不启动进程、数据库或浏览器。

native `light`、`web`、`service` 上，之后的 SQL 变动只通过 `scripts/egon-codegen.sh` 刷新目录内 Java 和 Mapper XML，规则在 `egon-coding-writing-spec`。本 skill 不重写那份目录。`agent` 是合法的非 open archetype，但不在生成器范围内；不要为它编造持久化模板。

## 交接

生成成功后，下一步是对生成项目使用 `egon-coding-writing-spec`。用户没要求就不要开始。
