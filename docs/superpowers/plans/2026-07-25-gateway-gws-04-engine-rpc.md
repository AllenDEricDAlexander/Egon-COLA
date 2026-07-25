# Gateway GWS-04 Egon RPC 数据面实现计划

状态：已执行

> **For Codex:** REQUIRED SUB-SKILL: Use superpowers:executing-plans and
> superpowers:test-driven-development to implement this plan task-by-task.

**Goal:** 提供 INTERNAL gRPC Listener、动态 Unary 方法路由、原始 Protobuf 转发和
HTTP→RPC 动态消息转换，生产代码不依赖 RPC Test Mock。

**Architecture:** Rule Compiler 产出完整不可变 `RpcMethodIndex`；gRPC
`HandlerRegistry` 在每次调用开始时捕获当前索引。透明 RPC 路径用 byte[] Marshaller，
Channel Cache 以 leaseId 为键；需要映射时才使用 `DynamicMessage`。

## 设计模式判断

- Registry + Atomic Snapshot 保证 Method 集合原子切换。
- Adapter 封装 gRPC ServerCall/ClientCall。
- Cache 管理 Provider Channel 生命周期。
- 不使用业务接口反射或生成代码作为运行时依赖。

## Task 1: RPC Route 与原子 Method Index

- 定义 Runtime RPC Route、Descriptor 引用和编译校验。
- 重复 Full Method、非 Unary、Descriptor 不一致发布失败。
- 测试旧索引捕获与未知方法。

**Commit:** `feat(gateway): compile rpc method index`

## Task 2: Raw Byte Handler 与 Unary Forwarder

- 实现 Raw Byte Marshaller、动态 MethodDescriptor 和 ServerCallHandler。
- 透传允许的 Metadata、Status、Trailer；限制消息和 Metadata 大小。
- Deadline 取入站、Route 和系统上限最小值；Cancellation 传播。

**Commit:** `feat(gateway): forward raw unary rpc calls`

## Task 3: Provider Channel Cache

- Key 包含 service、instanceId、leaseId、地址与 secure。
- 租约替换停止新借用并有界 Drain。
- 测试旧 lease Channel 不复用、并发 acquire/release 和关闭。

**Commit:** `feat(gateway): manage rpc provider channels`

## Task 4: RPC Listener 与 INTERNAL_GATEWAY Slot

- 启动独立 INTERNAL gRPC Server。
- Engine Ready 后注册单个 DDC `INTERNAL_GATEWAY` 租约，Drain 前注销。
- Engine 强制关闭 RPC Consumer Gateway Manager。
- 真实 gRPC 测试覆盖调用、deadline、cancel、未知方法和多 Slot 拒绝。

**Commit:** `feat(gateway): expose internal rpc listener`

## Task 5: HTTP 到 RPC 动态消息桥

- 从标准 FileDescriptorSet 构建受控 Descriptor Registry。
- 按显式映射创建/读取 `DynamicMessage`，拒绝未知字段和类型错误。
- 测试不依赖业务接口 JAR。

**Commit:** `feat(gateway): bridge http requests to rpc`

## Task 6: GWS-04 验收

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl :egon-cola-component-gateway-engine -am clean test
test -z "$(rg 'test\\.mockgateway|MockRpcGateway' \
  egon-cola-components/egon-cola-component-gateway/*/src/main || true)"
```
