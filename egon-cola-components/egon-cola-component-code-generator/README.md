# egon-cola-component-code-generator

离线开发工具。它读取 PostgreSQL DDL 或 MP-SDJ manifest，为 native Light、Web、Service 生成后端 CRUD。它不启动 Spring，不连接数据库，也不修改目标项目的 POM。

已批准的模板引擎只有 `org.freemarker:freemarker:2.3.35`，并且只存在于本工具。生成出的业务工程不依赖 FreeMarker。

## 构建与 classpath

在仓库根目录执行：

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-code-generator -am install -DskipTests
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-code-generator dependency:build-classpath -DincludeScope=runtime -Dmdep.outputFile=/tmp/egon-codegen.cp
```

第一条命令用 `install` 而不是 `package`，把上游模块写入本地仓库；第二条命令不带 `-am`，
否则 reactor 中每个模块都会覆写同一个 `/tmp/egon-codegen.cp`，结果只取决于构建顺序。

把本模块 `target/classes` 加到该 classpath 前面，再导出：

```bash
export EGON_CODEGEN_CLASSPATH="egon-cola-components/egon-cola-component-code-generator/target/classes:$(cat /tmp/egon-codegen.cp)"
```

`scripts/egon-codegen.sh` 只使用已经给出的 `EGON_CODEGEN_CLASSPATH`，不执行 Maven、curl，也不启动应用。Java 可执行文件按 `JAVA`、`JAVA_HOME/bin/java`、`PATH` 中的 `java` 顺序选取。缺少 Java、`EGON_CODEGEN_CLASSPATH` 为空、或该 classpath 中任一条目在磁盘上不存在，都会向标准错误打印 `{"code":"BLOCKED_TOOLING",...}` 并以退出码 7 结束。

## 命令

```text
scripts/egon-codegen.sh templates
scripts/egon-codegen.sh plan --config codegen.json
scripts/egon-codegen.sh check --config codegen.json
scripts/egon-codegen.sh apply --plan <plan-id> --config codegen.json
scripts/egon-codegen.sh recover --config codegen.json
```

命令成功时，JSON 对象是标准输出的最后一行。本模块不内置日志配置，classpath 上的 Logback 默认控制台 appender 会把日志行（例如 Hibernate Validator 版本行）也写到标准输出，解析前需要丢弃 JSON 之前的内容。错误与诊断以 `{"code":...,"message":...}` 形式只写到标准错误。退出码：0 成功或无变化，2 配置或语法，3 缺类型或不规范，4 人工冲突，5 破坏性变更未确认，6 过期计划，7 恢复或 IO，8 检查发现待同步差异。

`check` 不改项目文件，只逐项比较渲染结果与磁盘内容。`apply` 是全有或全无：预检一旦发现 `CONFLICT`，就在写入任何文件之前中止并返回 4；人工改过的文件保持原样，只有磁盘内容仍等于上次生成内容的文件会被替换。退出码 5 `DESTRUCTIVE_ACTION` 对应需要显式受理的 `DELETE`/`RENAME` 动作，但 `plan` 只产出 `CONFLICT`、`NO_CHANGE`、`ADD`、`UPDATE`，命令行也不传受理集合，因此该分支在当前 CLI 下不可达。

`backend-crud` 会生成完整基础链路：PO、DAO、Mapper XML、Repository、领域模型与
Domain Service/实现、Command/Query/Result、转换器、Manage/实现；Light 和 Web 还会
生成 Controller，Service 不生成 HTTP 入口；`persistence-crud` 只展开为前四项。
`artifacts` 可以逐个选择，别名 `pojo`→`po`、`mapper`→`mapper-xml` 会先归一化。
省略下层产物时必须用 `existingTypeMappings` 声明已有类型：缺 `po` 或缺 `dao` 返回
`MISSING_TYPE`（退出码 3）。生成器只强制这两项，catalog 里其余条目的 `requiredTypes`
当前不参与校验；生成范围也不会自动扩大。查询和写入均由 Repository 持有 Mapper/XML 访问，
Domain Service 实现不能直接调用 DAO。`logicalTables` 必须明确指定，且逐个匹配 DDL 中的逻辑表，
不存在的表名直接报错。

配置 JSON 的 `configVersion` 必须是 `1`，`projectType` 取 `light`/`web`/`service`，
未知键一律拒绝（反序列化开启 `FAIL_ON_UNKNOWN_PROPERTIES`）。`input.mode` 只有两种：
`schema` 按顺序重放 `schemaFiles`，`manifest` 读取 `resourceRoot` 下的 `manifest` 路径。
manifest 形如 `{"family":...,"scripts":[{"version":...,"path":...,"sha256":...}]}`；
脚本字节与记录的 `sha256` 不符，或已记录的版本前缀发生变化，都返回 `CHECKSUM_DRIFT`。

DDL 必须定义 `EgonModel` 所需的 id、tenant、审计、deleted_at 和 version 列，
生成器校验其类型和空值语义。PO 继承 `EgonModel`，只声明业务字段；Mapper XML 仍映射
继承字段。生成器不会把这些通用字段再声明到 PO 中。

计划格式当前为 v2。无生成记录的同名文件、人工编辑的文件，以及计划后新增或改变的文件
均返回冲突，不自动接管或覆盖。`apply` 重新核对配置、DDL、模板、工具代码和生成状态的
指纹，并在同一输出目录锁下预检所有目标文件；旧 v1 计划需要重新运行 `plan`。

`plan`、`apply` 与 `recover` 只在输出根目录下维护 `.egon/codegen/`：
`plans/<plan-id>.json` 由 `plan` 写出，`state.json` 由 `apply` 更新，其中的 journal 记录
中断阶段并供 `recover` 回滚，`apply.lock` 是写入锁。删除该目录等于让生成器失去所有文件的
归属记录，此后同名文件都会判为冲突。

当前验收覆盖源码编译和隔离测试：`./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-code-generator -am test`。它不代表 PostgreSQL、ShardingSphere 或 MQ 已经在真实环境运行。
