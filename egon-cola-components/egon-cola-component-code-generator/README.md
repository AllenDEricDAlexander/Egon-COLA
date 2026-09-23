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

`backend-crud` 会生成完整基础链路：PO、DAO、Mapper XML、Repository、领域模型与
Domain Service/实现、Command/Query/Result、转换器、Manage/实现；Light 和 Web 还会
生成 Controller，Service 不生成 HTTP 入口。可以只选其中的产物；引用的其他类型必须已在
目标项目中存在，生成器不会擅自扩大范围。查询和写入均由 Repository 持有 Mapper/XML 访问，
Domain Service 实现不能直接调用 DAO。`logicalTables` 必须明确指定。

DDL 必须定义 `EgonModel` 所需的 id、tenant、审计、deleted_at 和 version 列，
生成器校验其类型和空值语义。PO 继承 `EgonModel`，只声明业务字段；Mapper XML 仍映射
继承字段。生成器不会把这些通用字段再声明到 PO 中。

计划格式当前为 v2。无生成记录的同名文件、人工编辑的文件，以及计划后新增或改变的文件
均返回冲突，不自动接管或覆盖。`apply` 重新核对配置、DDL、模板、工具代码和生成状态的
指纹，并在同一输出目录锁下预检所有目标文件；旧 v1 计划需要重新运行 `plan`。

当前验收覆盖源码编译和隔离测试。它不代表 PostgreSQL、ShardingSphere 或 MQ 已经在真实环境运行。
