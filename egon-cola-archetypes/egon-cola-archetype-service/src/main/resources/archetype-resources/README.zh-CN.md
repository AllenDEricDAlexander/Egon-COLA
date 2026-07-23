#set( $symbol_pound = '#' )
${symbol_pound} ${rootArtifactId}

[English](README.md) | 中文

`${rootArtifactId}` 是一个只面向 service 的 COLA 示例，覆盖 Course、Schedule、Exam、Paper 和 Score 流程。业务流量通过 Dubbo Triple RPC 或 RabbitMQ 进入；HTTP 仅用于 Spring Boot Actuator 管理端点。

${symbol_pound}${symbol_pound} 模块职责

- `${rootArtifactId}-common`：稳定的错误、常量、枚举和标识符工具。
- `${rootArtifactId}-domain`：实体、聚合、值对象、领域服务、仓储/事件端口，以及由消费者拥有的 Organization 目录端口。不包含持久化、MQ、Facade 或 Dubbo 实现。
- `${rootArtifactId}-application`：命令、查询、用例管理器、应用校验和结果模型。
- `${rootArtifactId}-infrastructure`：Spring Data JPA 仓储、Flyway migration、RabbitMQ/本地发布器实现，以及 `top.egon:egon-cola-organization-facade` 防腐适配器。
- `${rootArtifactId}-adapter`：`top.egon:egon-cola-evaluation-facade` 的 Dubbo provider、facade 转换、校验、异常转换和 score-command MQ consumer。
- `${rootArtifactId}-starter`：Spring Boot 组装、profile、管理配置以及架构/上下文测试。

${symbol_pound}${symbol_pound} 领域优先包布局

业务代码先按领域组织，再按技术职责组织：

```text
domain/exam/entities
application/course/manage
infrastructure/exam/repo
adapter/course/facade/impl
adapter/exam/mq
```

该项目保持 service-only：业务流量通过 Dubbo Triple 或 RabbitMQ 进入，不包含业务 Controller、Web Filter、GraphQL 或 VO 包。外部 Organization 边界位于 `domain/client/organization` 和 `infrastructure/client/organization`。

允许的内部依赖图为：

```text
Common <- Domain <- Application <- Adapter <- Canonical Evaluation Facade
          Domain <- Infrastructure <- Canonical Organization Facade
          Adapter <- Starter -> Infrastructure
```

更精确地说：Domain 只依赖 Common；Application 和 Infrastructure 只依赖 Domain；Adapter 只依赖 Application。Adapter 实现外部 Evaluation Facade 契约，Infrastructure 消费外部 Organization Facade 契约，两个已发布 Facade 都不依赖当前生成项目。Starter 是组合根，因此不存在 Web/Service Maven 循环依赖。

${symbol_pound}${symbol_pound} 示例流程

- Course RPC 创建具有唯一规范化 code 的课程，读取课程、分页查询课程，并在时间范围不重叠的情况下安排课程。
- Exam RPC 为课程创建考试，关联一张试卷，并且只在试卷就绪后发布考试。
- Score RPC 记录并查询经过校验的分数。RabbitMQ score command 通过 `RecordScoreConsumer` 进入，然后委托给同一个 Application 用例。
- Domain publisher port 描述课程安排、考试发布和分数记录；Infrastructure 提供本地或 RabbitMQ 实现。

RabbitMQ 支持有意保持为基础传输能力。示例不承诺重试、死信队列、幂等 inbox、事务 outbox，或超出 broker 配置行为之外的投递保证。

${symbol_pound}${symbol_pound} Profile 与集成

`local` 是默认 profile。`local` 和 `test` 都使用 PostgreSQL 兼容模式的 H2，不需要 Nacos、RabbitMQ 或 PostgreSQL 服务。`test` 中 RabbitMQ publisher 和 listener 已关闭；`local` 使用本地 publisher，除非显式启用 RabbitMQ。

Organization Facade client 是一个暂未使用的 infrastructure 基础能力。`local` 和 `test` 选择确定性的 `OrganizationDirectoryPort` stub。`dev` 和 `prod` 选择真实 Dubbo client，通过生成的 POM 固定 `top.egon:egon-cola-organization-facade`，并在 provider 不可用时显式失败。Adapter 实现 `top.egon:egon-cola-evaluation-facade`。当前没有 Application 用例调用 Organization port。

`dev` 和 `prod` 是外部集成 profile。请通过环境变量配置，不要提交敏感信息：

- 数据库：`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`DB_DRIVER_CLASS_NAME`。
- Nacos：`NACOS_SERVER_ADDR`、`NACOS_NAMESPACE`、`NACOS_GROUP`、`NACOS_USERNAME`、`NACOS_PASSWORD`。
- Dubbo：`DUBBO_REGISTRY_ADDRESS`、`DUBBO_PORT`、`DUBBO_CONSUMER_TIMEOUT`。
- Organization Facade：`ORGANIZATION_FACADE_ENABLED`、`ORGANIZATION_FACADE_GROUP`、`ORGANIZATION_FACADE_SERVICE_VERSION`。
- RabbitMQ：`RABBITMQ_HOST`、`RABBITMQ_PORT`、`RABBITMQ_USERNAME`、`RABBITMQ_PASSWORD`、`RABBITMQ_ENABLED`、`RABBITMQ_LISTENER_AUTO_STARTUP`。
- 配置解密：`CONFIG_DECRYPT_KEY` 或文档化的 config-tree secret source。

${symbol_pound}${symbol_pound} 数据库策略

Flyway 负责 schema 演进。`V1__init_student_management_evaluation.sql` 不可修改。`V2__align_evaluation_course_exam_domain.sql` 是唯一的对齐 migration，用于在保留有效 V1 数据的同时增加 Course/Schedule/Exam/Paper/Score 模型。绝不要编辑已应用的 migration；应新增下一个版本。

${symbol_pound}${symbol_pound} 验证与打包

```bash
SPRING_PROFILES_ACTIVE=test bash ./mvnw -B -ntp clean test
SPRING_PROFILES_ACTIVE=test bash ./mvnw -B -ntp -DskipTests package
```

测试套件包括 Domain 规则、Application 编排、JPA adapter、V1 到 V2 的 migration 行为、无 broker 的 MQ adapter、实际 Dubbo Triple proxy 调用、无外部依赖的 Spring context 组装和 ArchUnit 依赖检查。构建镜像不会启动服务。

${symbol_pound}${symbol_pound} 容器交付

生成的项目使用一个从源码构建的 `deploy/container/Dockerfile`：

```bash
docker build --build-arg CONTAINER_ENGINE=docker -f deploy/container/Dockerfile -t ${rootArtifactId}:local .
podman build --build-arg CONTAINER_ENGINE=podman -f deploy/container/Dockerfile -t ${rootArtifactId}:local .
nerdctl build --build-arg CONTAINER_ENGINE=nerdctl -f deploy/container/Dockerfile -t ${rootArtifactId}:local .
```

使用以下命令启动完整的 Docker 开发栈：

```bash
docker compose --env-file deploy/env/.env.example -f deploy/compose/compose.docker.yaml up -d --build
```

Podman 和 nerdctl 分别使用 `compose.podman.yaml` 和 `compose.nerdctl.yaml`。生产环境使用匹配的 `.prod.yaml` 文件和由运维方持有的 `.env.prod`。关于 rootless 前置条件、持久化、生产边界和数据删除警告，请参见 `deploy/container/README.md`。

根目录 `Jenkinsfile` 会运行测试，也可以发布不可变镜像。设置 `PUBLISH_IMAGE=true` 并提供 registry 参数即可发布；它不会执行部署。

${symbol_pound}${symbol_pound} 范围边界

该生成的 service 项目不包含业务 Controller、Web Filter、GraphQL endpoint、native grpc-java 模块或启用的 H2 console。Organization Facade client 有意未接入当前 Application 行为。
