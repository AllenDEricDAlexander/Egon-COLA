# Gateway GWS-03 Engine Core 与 HTTP 数据面实现计划

> **For Codex:** REQUIRED SUB-SKILL: Use superpowers:executing-plans and
> superpowers:test-driven-development to implement this plan task-by-task.

**Goal:** 在不依赖 Spring Cloud Gateway 的前提下，建立可由 PUBLIC/INTERNAL 独立
Listener 驱动的自研 HTTP Gateway 热路径。

**Architecture:** Core 保持框架无关，以不可变 Route Index、阶段化 Filter Chain 和
`GatewayExecutor` 组织请求。Engine 使用 Reactor Netty 适配网络输入输出和上游调用，
Listener 只负责把可信 `AccessZone` 注入 Exchange，地址只能来自已选择 Provider。

**Tech Stack:** Java 21、Reactor Core、Reactor Netty、JUnit 5、AssertJ。

## 设计模式判断

- 使用 Chain of Responsibility 表达固定安全顺序和可扩展 Filter。
- 使用 Compiler + Immutable Snapshot 生成无全表扫描的 Route Index。
- 使用 Adapter 隔离 Reactor Netty 与框架无关 Core。
- HTTP 上游仅承担协议适配，不承担发现和负载均衡。

## Task 1: 请求规范化与 Body 所有权

- 新增 Path/Host/Header 规范化器及稳定异常。
- 新增聚合 Body 单消费、上限、释放和可选 Cache 语义。
- 测试非法 UTF-8、重复编码、穿越、编码斜杠和二次消费。

**Commit:** `feat(gateway): normalize inbound http requests`

## Task 2: 编译不可变 Route Index

- 新增 Runtime HTTP Route、Predicate、Path Pattern 和 Match Result。
- 编译 Host → Method → Path Trie，激活后只读。
- 检测重复/歧义 Route；测试精确、变量、尾部通配和优先级。

**Commit:** `feat(gateway): compile immutable http routes`

## Task 3: Filter Chain 与 Executor

- 新增固定 `GatewayFilterStage`、Filter、Chain、Executor。
- 同 Order 冲突失败；Exposure 之前和 Observation 之后禁止扩展。
- 同步/异步异常统一映射；取消和资源释放使用终结回调。

**Commit:** `feat(gateway): execute staged filter chains`

## Task 4: Reactor Netty Listener 与 HTTP Upstream

- Engine 增加独立 PUBLIC/INTERNAL Listener 配置和生命周期。
- Listener 端口互斥，AccessZone 不读取客户端 Header。
- 复用连接池的上游 Adapter 过滤 Hop-by-Hop/伪造身份 Header，重建转发 Header。
- 上游只能接收 `ProviderInstance`，不接受绝对 URL。
- 真实端口测试覆盖转发、外部不可访问、Body 超限和关停。

**Commit:** `feat(gateway): add reactor netty http data plane`

## Task 5: GWS-03 验收

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl :egon-cola-component-gateway-engine -am clean test
git diff --check
```

检查 Engine 不出现 Spring Cloud Gateway、客户端 Access Zone Header 不影响 Listener
来源、Route 热路径无全量扫描，连接目标无静态管理地址。
