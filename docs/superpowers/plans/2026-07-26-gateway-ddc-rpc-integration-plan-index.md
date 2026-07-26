# Gateway、DDC、RPC 与 HTTP 联调闭环 Implementation Plan Index

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 按已确认设计修复 Gateway、DDC、RPC 的全部 P0/P1/P2，并交付 MVC/WebFlux/RPC 真实联调与指导文档。

**Architecture:** 工作拆成七个有序子计划，每个计划产生可独立测试和提交的能力。公共 wire/Bean 合同先稳定，随后分别完成 DDC 与 Gateway 发布状态机，再接入 HTTP、RPC、安全和真实拓扑。

**Tech Stack:** Java 21、Spring Boot 3、Spring MVC、Spring WebFlux、gRPC/Protobuf、Redisson、PostgreSQL、SQLite、Flyway、Kafka、Testcontainers、Docker Compose、Maven Failsafe。

## Global Constraints

- 代码基线为 `main@d5d53762`；实施时以工作区实际 HEAD 为准。
- 不修改任何既有 Flyway V1-V3 文件。
- Gateway 只新增一份 V4；DDC 的 PostgreSQL、SQLite 各新增一份同逻辑 V4。
- 公共 JSON `status` 字段继续保持 String，使用 typed accessor 归一化旧值。
- HTTP 首版只覆盖注解式 MVC 与 WebFlux `Mono<DTO>`/JSON。
- RPC 只覆盖 unary；非幂等方法不得跨 Gateway 自动重试。
- 不引入 Saga/2PC、Strategy、Chain 或 Gateway 专用 DDC bundle API。
- 每个任务先写失败测试，定向测试通过后单独提交。
- 不自动遗留运行中的业务项目、容器或后台进程。

---

## 执行顺序

1. [01 公共合同与启动基线](2026-07-26-integration-01-foundation-contracts.md)
2. [02 DDC 发布一致性](2026-07-26-integration-02-ddc-publish-consistency.md)
3. [03 Gateway 分阶段发布](2026-07-26-integration-03-gateway-publication-journal.md)
4. [04 HTTP Provider Runtime](2026-07-26-integration-04-http-provider-runtime.md)
5. [05 RPC 幂等与恢复](2026-07-26-integration-05-rpc-resilience.md)
6. [06 DDC 管理面安全](2026-07-26-integration-06-ddc-security.md)
7. [07 真实联调、Compose 与 Runbook](2026-07-26-integration-07-live-demo-runbook.md)

计划 01 是所有后续计划的前置。计划 02 完成后执行计划 03；计划 04、05 可以在计划 01
之后分别实施，但在计划 07 汇合。计划 06 在公开 Demo 前必须完成。

## Spec 覆盖矩阵

| Spec 缺口 | 实施任务 |
|---|---|
| P0-01 Gateway→DDC 请求无效 | 02/Task 1，03/Task 1-4 |
| P0-02 服务状态不一致 | 01/Task 1 |
| P0-03 DDC 容器不可执行 | 01/Task 3 |
| P0-04 联调进程连接错误 Redis | 01/Task 4，07/Task 1-4 |
| P0-05 HTTP Provider 不可直接消费 | 04/Task 1-4 |
| P1-01 Gateway 无 chunk 恢复日志 | 03/Task 1-4 |
| P1-02 draft/published 混用 | 02/Task 2、4 |
| P1-03 Redis 发布非原子 | 02/Task 3-4 |
| P1-04 Redisson Bean 串线 | 01/Task 2 |
| P1-05 Redis Cluster CROSSSLOT | 02/Task 3 |
| P1-06 ACK 无重试 | 02/Task 5 |
| P1-07 chunk 顺序/生命周期 | 02/Task 5，03/Task 5 |
| P1-08 RPC 重试/错误分类 | 05/Task 1-4 |
| P2 DDC 管理安全 | 06/Task 1-4 |
| P2 Compose、真实测试和可测试性 | 07/Task 1-4 |
| P2 中英文联调文档 | 07/Task 5-6 |

## 最终验证

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-dynamic-config-center \
  -am clean verify

./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-rpc \
  -am clean verify

./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-gateway \
  -am clean verify

./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-gateway/\
egon-cola-component-gateway-test/egon-cola-component-gateway-test-suite \
  -am -Pgateway-live clean verify
```

以上命令必须按实际 POM reactor 路径校准；不能用窄测试代替最终三组件闭环证据。
