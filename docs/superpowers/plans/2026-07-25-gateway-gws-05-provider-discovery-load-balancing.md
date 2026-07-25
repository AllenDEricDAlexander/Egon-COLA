# Gateway GWS-05 Provider 发现、健康与负载均衡实现计划

状态：已执行

> **For Codex:** REQUIRED SUB-SKILL: Use superpowers:executing-plans and
> superpowers:test-driven-development to implement this plan task-by-task.

**Goal:** 统一 HTTP/RPC Provider 身份和本地目录，通过 DDC 动态发现，提供确定性候选
过滤、健康摘除和四种负载均衡策略。

**Architecture:** Core 只暴露 `ProviderServiceRegistry` 与不可变模型；Engine DDC
Adapter 转换 DDC 类型。`ProviderDirectory` 以 Service 引用计数管理订阅并原子替换
Snapshot；选择器依次应用 Metadata 覆盖、候选过滤、健康状态和 LoadBalancer Strategy。

## 设计模式判断

- Adapter 防止 DDC 类型泄漏到 Core。
- Repository/Directory 提供本地只读快照。
- Strategy 承载 Round Robin、SWRR、Random、Least In-Flight。
- Observer 用于订阅，不在请求线程获取 DDC。

## Task 1: Provider Core 模型与 Registry SPI

- 定义 Service Key、Instance、Registry/Health 状态、Snapshot、Query、Subscription。
- 所有集合不可变，实例身份包含 instanceId + leaseId。
- 测试协议映射、过期租约和 Secret Metadata 拒绝。

**Commit:** `feat(gateway): define provider discovery contracts`

## Task 2: HTTP Provider Runtime

- 解析 Server Ready 后的真实地址并注册 DDC HTTP_PROVIDER。
- 心跳、租约丢失重注册、停止注销、fail-fast。
- 生产环境拒绝 wildcard/loopback；port=0 仅 local/test。

**Commit:** `feat(gateway): register http provider leases`

## Task 3: DDC Adapter 与 Provider Directory

- 映射 DDC Service/Instance，订阅 Service Catalog 和活动 Route 引用。
- 引用计数共享订阅；新增 Service 初始查询成功后 Rule 才可激活。
- DDC 中断只使用未过期内存租约，冷启动不可达不 Ready。

**Commit:** `feat(gateway): maintain provider directory`

## Task 4: Metadata、健康与候选过滤

- 实现系统默认、Provider、Service Rule、Instance Override 优先级。
- 校验 zone/region/tags/weight，区分 Registry/Active/Passive 状态。
- 连续可重试失败和失败率触发有界 Ejection，业务拒绝不摘除。

**Commit:** `feat(gateway): filter healthy provider candidates`

## Task 5: Load Balancer Strategy

- 实现 Round Robin、Smooth Weighted Round Robin、Random、Least In-Flight。
- Selection Handle 负责准确增减在途计数。
- 空候选稳定失败；相同 Snapshot 行为可重复测试。

**Commit:** `feat(gateway): balance provider traffic`

## Task 6: GWS-05 验收

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl :egon-cola-component-gateway-provider-runtime,\
:egon-cola-component-gateway-engine -am clean test
```

检查 Engine 代码不存在静态 Provider 地址和请求线程 DDC 查询。
