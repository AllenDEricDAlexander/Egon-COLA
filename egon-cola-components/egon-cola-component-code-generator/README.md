# egon-cola-component-code-generator

离线开发工具。它读取 PostgreSQL DDL 或 MP-SDJ manifest，为 native Light、Web、Service 生成后端 CRUD。它不启动 Spring，不连接数据库，也不修改目标项目的 POM。

已批准的模板引擎只有 `org.freemarker:freemarker:2.3.35`，并且只存在于本工具。生成出的业务工程不依赖 FreeMarker。

## 构建与 classpath

在仓库根目录执行：

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-code-generator -am package -DskipTests
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-code-generator -am dependency:build-classpath -DincludeScope=runtime -Dmdep.outputFile=/tmp/egon-codegen.cp
```

把本模块 `target/classes` 加到该 classpath 前面，再导出：

```bash
export EGON_CODEGEN_CLASSPATH="egon-cola-components/egon-cola-component-code-generator/target/classes:$(cat /tmp/egon-codegen.cp)"
```

`scripts/egon-codegen.sh` 只使用已经给出的 `EGON_CODEGEN_CLASSPATH`。classpath 缺失时它打印 `BLOCKED_TOOLING` 后退出，不会执行 Maven、curl 或启动应用。

## 命令

```text
scripts/egon-codegen.sh templates
scripts/egon-codegen.sh plan --config codegen.json
scripts/egon-codegen.sh check --config codegen.json
scripts/egon-codegen.sh apply --plan <plan-id> --config codegen.json
scripts/egon-codegen.sh recover --config codegen.json
```

标准输出是一个 JSON 对象。日志和错误在标准错误。退出码：0 成功或无变化，2 配置或语法，3 缺类型或不规范，4 人工冲突，5 破坏性变更未确认，6 过期计划，7 恢复或 IO，8 检查发现待同步差异。

`check` 不改项目文件。`apply` 只替换磁盘内容仍等于上次生成内容的文件。人工改过的文件保持原样。

当前验收覆盖源码编译和隔离测试。它不代表 PostgreSQL、ShardingSphere 或 MQ 已经在真实环境运行。
