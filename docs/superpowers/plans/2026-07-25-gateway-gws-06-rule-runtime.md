# Gateway GWS-06 Rule Snapshot、DDC 发布与运行态实现计划

> **For Codex:** REQUIRED SUB-SKILL: Use superpowers:executing-plans and
> superpowers:test-driven-development to implement this plan task-by-task.

**Goal:** 实现 Admin Draft 到 DDC `gateway.rules.active`，再到 Engine 校验、持久化、
原子激活、精确 ACK 和 LKG 恢复的完整闭环。

**Architecture:** Contract 模块定义稳定 Activation/Snapshot DTO；Admin Compiler
生成 canonical content 和双 SHA-256，Publisher 仅调用 DDC Management Client。
Engine Applier 使用 Prepare/Commit：校验、编译、准备 Provider 订阅、写临时 LKG，
随后原子切换并替换最终 LKG；失败保留旧快照。

## 设计模式判断

- Compiler/Builder 分离编辑态和运行态。
- Two-phase Prepare/Commit 保证资源与规则原子生效。
- Repository 保存 LKG，Strategy 支持 INLINE/CHUNKED。
- Observer 通过 DDC Applier 接收配置，不直接订阅 Redis。

## Task 1: Snapshot/Activation 稳定契约

- 定义 v1 Envelope、Content、Operation、Route、Policy Ref、Chunk Ref。
- Canonical JSON 排序、不输出 null，计算 content/artifact SHA-256。
- 测试语义相同 hash 相同、release metadata 只改变 artifact hash。

**Commit:** `feat(gateway): define rule snapshot contracts`

## Task 2: Admin Rule Compiler 与发布校验

- 校验唯一 ID、引用、Route 歧义、Exposure、Protocol、Policy 和 Schema。
- 禁止静态 Provider URL、未知枚举和不支持 RPC Streaming。
- 输出 INLINE 或不超过 256 KiB 的不可变 CHUNK。

**Commit:** `feat(gateway): compile gateway rule releases`

## Task 3: DDC Publisher

- 固定 appCode/env/namespace 与唯一 `gateway.rules.active` Key。
- Chunk 全部 SYNC_ALL_ACK 成功后才发布 Activation。
- 保存 changeId/expectedVersion/精确 Target 结果；失败不推进 Release 成功状态。

**Commit:** `feat(gateway): publish rules through ddc`

## Task 4: Engine Rule Applier 与 LKG

- 支持 INLINE 和乱序 CHUNKED staging。
- 校验双 Hash、Schema 和递增 DDC Version。
- 编译 Route/RPC/Policy，准备 Provider 订阅，fsync 临时文件后原子激活。
- 失败 ACK 且保留旧版本；启动从 LKG 恢复并做兼容/校验。

**Commit:** `feat(gateway): activate rule snapshots atomically`

## Task 5: Runtime Status

- 暴露管理网络只读状态模型：active/pending release、version/hash、节点状态、错误。
- 不经 PUBLIC/INTERNAL 数据面，也不返回 Secret 或完整规则。
- 测试发布失败、重复、旧版本、回滚和节点分歧投影。

**Commit:** `feat(gateway): report rule runtime status`

## Task 6: GWS-06 验收

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl :egon-cola-component-gateway-admin,\
:egon-cola-component-gateway-engine -am clean test
git diff --check
```

检查 Gateway 不直接访问 DDC DB/Redis，只有 `gateway.rules.active` 推进活动版本，任何
失败不会破坏当前 LKG。
