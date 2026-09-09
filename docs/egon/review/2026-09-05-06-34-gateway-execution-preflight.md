# Yuheng 拆分执行预检与最小修正案

| Field | Value |
| --- | --- |
| Status | `Accepted — user approved DEP-001, MODEL-001 and SCOPE-001 on 2026-09-05` |
| Checked | `2026-09-05 06:34 CST` |
| Repository baseline | `main@d73c77d4fa14421194b28fd7dea51a001d2aa630` |
| Plan | [双 Engine 实施计划](../plan/2026-09-02-21-03-yuheng-dual-engine-separation-implementation.md) |
| Primary Spec | [单 Admin、双 Engine 规格](../spec/2026-09-02-19-52-yuheng-dual-engine-separation.md) |

本文件保留执行预检发现。用户已明确同意本修正案；修正后的顺序和版本权威已回写主 Spec/Plan，生产实现按修正后的文件执行。

## 已确认的执行授权

用户于 2026-09-05 确认按 `egon-coding-executing-plan` 执行本计划，并明确要求拆分完成后启动全量 xingyuan、使用浏览器测试及修复测试问题，重点包含 Tianquan-Jianshen 角色分配权限接口、Yuheng OpenAPI 3 接口清单页面。

因此后续启动本地 xingyuan、浏览器验证和复现问题后的范围内修复已经获得授权，不需要再次确认运行权限。每 Step 单独验证、路径限定提交的要求继续生效。推送、远程部署和发布制品未包含在本次授权内。

## 预检发现

### FMT-001：格式化导致严格结构校验失效，已修复

原 Plan 经仓库提交 `9566c9679` 格式化后，表格单元格被填充空格，File 标题与路径拆成两行，Manual Checks 和 Commit paths 换行。现有严格校验器报告 48 项结构错误。

本次只规范化了表格空格、File 标题及字段续行，未调整其执行顺序和设计。格式恢复后 `validate_plan.py --strict` 已通过。结构校验通过不能证明下列依赖和数据模型问题已解决。

### DEP-001：Step 3 使用 Step 4 才创建的契约

当前源码证据（均位于 `egon-cola-xingyuan/egon-cola-yuheng/yuheng-biz-gateway/src/main/java/top/egon/cola/component/yuheng/engine/`）：

| 源文件与符号 | 当前依赖 | 原 Plan 安排 | 问题 |
| --- | --- | --- | --- |
| `operation/service/EngineGatewayOperationInvoker.java:19,66,206` | `CompiledGatewayRules`、`Supplier<CompiledGatewayRules>`、`active.snapshot()` | Step 3 移入 runtime-core | 依赖仍由可执行 Engine 所有；Step 3 无共享 DTO 可用 |
| `operation/adapter/HttpRpcUpstreamAdapter.java:22,61,372` | 相同具体编译结果及 RPC Descriptor Snapshot | Step 3 移入 runtime-core | 同一反向依赖 |
| `operation/adapter/DefaultGatewayOperationTransport.java:5,24,43` | Invoker 内部 `OperationTransport` 与 `HttpRpcUpstreamAdapter` | Step 3 同步迁移 | 不能把仅两个有问题的类留下而直接移动此类 |
| `rule/domain/CompiledGatewayRules.java:3,8,11` | API `RpcMethodIndex`、CORS、`CompiledMcpRules` | 继续留在 Engine 至 Step 6 | 整体下沉具体 Record 会把 API/MCP 依赖带回 runtime-core |

按原顺序执行，Step 3 无法同时满足“编译通过”和“runtime-core 不依赖 executable”。引入临时 executable 反向依赖、复制具体 Record 或提交不可编译代码都不是合格处理方式。

**建议修正：交换原 Step 3 与 Step 4。** 保持十一 Step 和每 Step 一次语义提交：

| 修正后顺序 | 内容 | 前置 |
| --- | --- | --- |
| Step 1 | 固定角色 Enum | 无 |
| Step 2 | common 能力与 runtime-core 基础 | Step 1 |
| Step 3（原 Step 4） | 最小共享 DTO、Compiler Strategy、Generic Activation；现有 mixed Record/Compiler 接入接口 | Step 2 |
| Step 4（原 Step 3） | HTTP/RPC 出站、Operation 迁移；改为消费已存在的共享 DTO | Step 3 |
| Step 5–11 | MCP compiler、双 executable、Admin/UI、suite、部署、脚本 | 依次执行，既定目标不变 |

原 Step 4 要迁移的通用 Rule 服务仅依赖 contract/core 和 Step 2 的 common 能力；其具体 mixed compiler 保留在 Engine，仍可引用原 HTTP/RPC 类型，因此可先于 transport/operation 提取。需同步修正依赖矩阵、文件块、RED/GREEN 命令、追踪矩阵；删除原 Step 4 中与“保留 mixed bridge”矛盾的“no mixed compiled type remains”预期。

### MODEL-001：Spec 将 Tianshu Version 错误归为 Snapshot 字段

当前 `yuheng-contract/.../rule/GatewayRuleSnapshot.java:12` 的全部字段为：

```java
GatewayRuleSnapshot(
    String ruleSchemaVersion,
    String releaseId,
    Instant generatedAt,
    String ruleContentSha256,
    String artifactSha256,
    GatewayRuleContent content
)
```

它不存在数字 `ruleVersion`。`ruleSchemaVersion` 是字符串 Schema 版本，不能作为 Tianshu 发布版本。

当前 `GatewayRuleActivationApplier.apply(String key, String value, long version)` 接收真实 Tianshu Version；`compiler.compile(snapshot)` 只接收 Snapshot；`successStatus(prepared, version)` 将 Tianshu Version 写入 `GatewayRuleRuntimeStatus.activeDdcVersion`。`restoreLkg()` 当前明确使用 `successStatus(prepared, 0)`，表示脱离 Tianshu 的恢复状态。`GatewayEngineConfiguration.gatewayRuntimeMetadata()` 从 status 读取版本，且 `activeRuleChecksum` 使用 `artifactSha256`。

主 Spec §9.2.3 要求编译结果的 release/version/checksum 全部等于输入 Snapshot；§10.3 又要求 `GatewayCompiledRulesDTO.ruleVersion: long` 来自 Snapshot/compiler。这两项无法按当前 `compile(snapshot)` 契约同时成立。

**建议修正：保留现有版本权威和 compiler 签名，数字 Tianshu Version 只属于 Activation Runtime Status。**

```java
public interface GatewayCompiledRulesDTO {
    String releaseId();
    String ruleChecksum();
    GatewayRuleSnapshot snapshot();
    Set<ProviderServiceKey> providerServices();
    Map<String, RuntimeProviderPolicy> providerPolicies();
    Map<String, RuntimeTrafficPolicy> trafficPolicies();
}

public interface GatewayRuleCompilerStrategy<T extends GatewayCompiledRulesDTO> {
    T compile(GatewayRuleSnapshot snapshot);
}
```

- 删除 compiled DTO 的 `ruleVersion` 要求；releaseId 对齐 `snapshot.releaseId()`，ruleChecksum 对齐 `snapshot.artifactSha256()`。
- Admin/Tianshu 仍校验各在线角色的 releaseId、`status.activeDdcVersion()`、Artifact SHA 和 ACK。
- Snapshot/Artifact JSON、Tianshu active key、LKG 文件格式、公开 Admin/MCP/API/RPC 契约不变。
- LKG 恢复继续保留 `version=0`、degraded 语义，不伪造正数版本；Tianshu 正常应用后才更新真实版本与 ACK。
- 编译行为以相同 Snapshot 为确定性输入，不额外创建版本参数、上下文 DTO 或双版本持久化。
- 同时修正 Plan 伪代码的 `Map<String, RuntimeProviderService>`：现有真实类型是 `Set<ProviderServiceKey>`，无需创建不存在的 Provider DTO。

必须增加/保留的验证：同一 Snapshot 编译确定性、Tianshu Apply 版本传递、LKG 恢复为 0/degraded、相同 release 不同 Tianshu version 的一致性、Artifact SHA 映射、无 Snapshot wire 变化。

### SCOPE-001：Step 2 提交范围漏掉实际生产引用方

Step 2 File 3 要求更新所有 API Engine imports，但 Commit paths 只列 common 生产目录、Engine 测试和 POM，未列实际引用 common 的其他生产文件。例如 `bootstrap/config/GatewayEngineConfiguration.java`、`bootstrap/lifecycle/GatewayEngineRuntime.java`、HTTP/RPC/Operation/Rule 的消费者均需要同步改 import。`yuheng-test-suite/.../mcp/McpHaRecoveryIT.java:22` 也直接导入旧 `ProviderDirectory`。

建议在原 Step 2 明确加入当前直接消费者的 **import/FQCN-only** 修改清单和路径限定提交范围；未来 Step 继续拥有其业务变更。不得借此提前修改后续 Step 行为。执行锁定时用以下搜索重新生成精确文件清单，逐项加入该 Step 记录：

```bash
rg -l '^import top\.egon\.cola\.component\.yuheng\.engine\.common\.(provider|security|traffic|transport|observability)\.' \
  egon-cola-xingyuan/egon-cola-yuheng --glob '*.java'
```

## 确认后的实施与运行验收

按上述修正更新主 Spec 和 Plan，严格校验后逐 Step 编码、验证、提交。拆分完成后继续执行用户已授权的第二阶段：

1. 通过现有 `scripts/unified-xingyuan/` 编排启动本地完整 xingyuan；保留数据库、已有凭据和业务数据，验证各 Admin、Engine、Portal/Web 的健康与依赖。
2. 浏览器登录并覆盖已注册的平台入口及主要只读页面，记录失败请求的 URL、状态码、响应与服务端 trace/log。
3. Tianquan-Jianshen：使用专用测试角色完成分配权限、保存、重新读取、页面刷新和撤销测试权限，验证前后端契约与授权、版本冲突分支；不修改现有管理角色的权限。
4. Yuheng：验证 OpenAPI 3 来源、分组、接口清单、接口详情与错误状态，核对页面请求到 Controller/Service/Repository 的真实契约。
5. 每个复现问题修复后补最小必要回归、单独提交，并重复相同浏览器动作直到验证通过；最终记录全部问题、修复提交、仍未覆盖的页面/环境边界。

## 本次实际验证与状态

| 检查 | 结果 |
| --- | --- |
| Execution skill resource-integrity | PASS：9 Markdown、12 resources |
| Plan strict validation（修复格式前） | FAIL：48 项格式识别错误 |
| Plan strict validation（修复格式后） | PASS：仅证明文档结构、链接、步骤标记和 REQ 覆盖 |
| 源码依赖/版本权威核查 | BLOCKED：DEP-001、MODEL-001、SCOPE-001 |
| Step 1–11 | 均未开始，零实施提交 |
| Maven 编译/业务测试 | 未运行；尚未修改生产/测试代码 |
| xingyuan 启动/浏览器测试/问题修复 | 未开始；按用户要求在拆分完成后执行 |
| 当前改动 | 原 Plan 的纯格式修复；本预检修正案 |
| 其他工作区 | Archetype 暂存删除/文档、共享 Web buildinfo、其他未跟踪 Spec/Plan 均保留；未暂存/提交 |

## 暂停依据

[egon-coding-executing-plan](../../../.agents/skills/egon-coding-executing-plan/SKILL.md) 的 Non-negotiable rules 16 明确要求：

> Do not silently skip, reorder, merge, split, or expand Steps. Obtain user approval for a material execution-sequence change.

同一技能 Entry conditions 要求遇到 effective Spec conflict 停在实施之前。本次不是缺少启动服务或浏览器权限；需要确认的是交换两个 Step，以及将错误的 compiled DTO version 要求修正为已有 Tianshu Runtime Status 权威。

**预检结论：BLOCKED。建议一次确认 DEP-001、MODEL-001、SCOPE-001 的上述最小修正，然后持续完成拆分和用户已经授权的全量平台浏览器测试修复。**
