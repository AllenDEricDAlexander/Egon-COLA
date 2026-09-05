# egon-cola-source-agent

[English](README.md) | [中文](README.zh-CN.md)

这是 `egon-cola-archetype-agent` 的内部 Deep Research source reactor。它采用 Web non-open 六模块结构，但只保留一个 `research` 业务域。source project 只是构建输入，不是发布到 Central 的公共 artifact。

```text
common -> domain -> application -> adapter -> starter
             \-> infrastructure -> starter
```

项目通过 `egon-cola-component-agent-flow-starter` 消费通用 Agent Flow 能力。provider 配置、MCP 凭据和 API secret 由 Starter 的环境配置提供。项目明确不包含 database、Flyway、Redis、MQ、RPC、GraphQL、UI、历史、恢复 API。

source reactor 使用 Java 21、Spring Boot 3.5.16、Spring AI 1.1.8、Springdoc 2.8.17，并通过 Components starter 使用 Google ADK 0.7.0。只有生成后的 `egon-cola-archetype-agent` definition 会成为公共 Archetype family。

## Maven Profiles 与外部启动参数

每次选择 `dev`、`test`、`prod` 中一个环境 profile，未指定时使用 `dev` 默认值。
这些 profile 控制应用启动，Surefire 仍使用原有 `test` 配置。堆内存参数是示例值，部署时按资源调整。

在项目根目录执行，多模块项目先安装兄弟模块依赖：

```bash
mvn install
mvn -pl egon-cola-source-agent-starter -Pdev spring-boot:run
mvn -Pprod -Drun.jvm-args="-Xms1g -Xmx2g" \
  -Drun.server-port=8080 \
  -Drun.config-location=file:/etc/myapp/override.yml package
```

Maven 在 `process-resources` 阶段生成 `egon-cola-source-agent-starter/target/launch.args`，打包时也会生成。
切换 profile 或 `-Drun.*` 参数后，无须 `clean` 即可更新文件。此执行仅使用 `@...@`
过滤 `src/main/launch/launch.args`，现有 YAML 占位符继续在运行时解析，参数文件不进入 JAR。

将可执行 JAR 与 `launch.args` 一起部署，JAR 可重命名为 `app.jar`，然后在部署目录执行：

```bash
java @launch.args -jar app.jar
java @launch.args -Xmx3g -jar app.jar --server.port=9080
```

文件记录 JVM 参数、Spring profile、端口及额外配置位置。这些显式系统属性优先于对应环境变量；
`-Drun.*` 覆盖 Maven 默认值，JVM 覆盖参数放在 `-jar` 前，Spring 命令行覆盖参数放在 JAR 后。
单独 `java -jar` 或 IDE 直接运行 main 不会自动读取该文件。相对配置路径以启动工作目录为准，
并非参数文件所在目录；部署建议使用绝对 `file:` 路径，Windows 路径使用正斜杠。
额外文件补充 `application.yml` 和选中的 `application-{profile}.yml`，不替换默认配置位置；
要求文件必须存在时去掉 `optional:`。密码和密钥继续通过现有环境变量/secrets 注入，不写入 `run.*`。

## 运行合同

唯一公共操作是 `POST /api/v1/deep-research/runs`，请求为 JSON，响应为 `text/event-stream`。请求体包含 `topic`、可选 `reportLanguage`（`ZH_CN` 或 `EN_US`，默认 `ZH_CN`）以及可选 `maxSources`（默认 `8`，范围 `3..20`）。必须提供 `X-Research-Api-Key`；`X-Trace-Id` 可选，缺失时由服务端生成。

每个通过校验的请求创建一个进程内 Agent Flow Session 和一次非幂等 run。事件按 `runId:sequence` 排序，名称为 `research.started`、`research.progress`、`research.completed` 或 `research.failed`；completed/failed 是唯一公共终态。重连会创建新的 run，不提供 status、history、resume 或持久化恢复 API。客户端断开时会取消 run，并释放 Session 与容量许可。

固定工作流为 `Planner -> ParallelResearch -> Writer`；并行节点包含 `EvidenceResearcher`、`CounterpointResearcher`、`FreshnessResearcher`。搜索回调来自一个配置的 MCP SSE 服务。请求不能选择模型、MCP 地址、密钥、工具或 prompt。

## 环境配置合同

开发和生产环境必须从外部提供 `DEEP_RESEARCH_MODEL_BASE_URL`、`DEEP_RESEARCH_MODEL_API_KEY`、`DEEP_RESEARCH_MODEL_NAME`、`DEEP_RESEARCH_API_KEY`、`DEEP_RESEARCH_MCP_BASE_URI`、`DEEP_RESEARCH_MCP_SSE_ENDPOINT` 与 `DEEP_RESEARCH_MCP_API_KEY`。运行限制使用 `DEEP_RESEARCH_MAX_*` 与 `DEEP_RESEARCH_HEARTBEAT_INTERVAL`。source 中不提交凭据、供应商 URL 或可用 secret；test profile 使用 fake `ChatModel` 和 fake `ToolCallback`，不会连接网络。

请求示例（shell 从外部 secret 环境展开 key）：

```bash
curl --fail-with-body --no-buffer \
  -H "X-Research-Api-Key: ${DEEP_RESEARCH_API_KEY}" \
  -H "Accept: text/event-stream" \
  -H "Content-Type: application/json" \
  -d '{"topic":"agent architecture","reportLanguage":"EN_US","maxSources":8}' \
  http://localhost:8080/api/v1/deep-research/runs
```

`reportMarkdown` 是不可信的模型输出，渲染前必须清理。服务端不会重试付费模型或 MCP 调用；重试会产生新的计费 run。进程重启后进程内 Session 与报告都会消失。生产环境关闭 API 文档；TLS、代理头与 secret 投递由部署负责。

## 校验

不启动服务即可运行 source 校验：

```bash
./mvnw -B -ntp -f egon-cola-source-agent/pom.xml clean verify
```

生成的 family 由仓库 archetype 脚本独立验证。该 source project 不包含 database、Flyway migration、缓存、消息中间件、RPC、GraphQL endpoint 或 UI。
