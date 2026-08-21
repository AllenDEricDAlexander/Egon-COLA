# Gateway Engine 与 MCP Core 功能域分包实施计划

| Field              | Value                                                                                                |
|--------------------|------------------------------------------------------------------------------------------------------|
| Document           | `2026-08-19-14-28-gateway-engine-mcp-package-refactor.md`                                            |
| Template Version   | `2`                                                                                                  |
| Status             | `Review`                                                                                             |
| Created            | `2026-08-19 14:28 CST`                                                                               |
| Updated            | `2026-08-19 14:28 CST`                                                                               |
| Owner              | `Egon-COLA platform owner / User`                                                                    |
| Repository         | `/Users/mario/SelfProject/Egon-COLA`                                                                 |
| Scope              | `egon-cola-platform-gateway-engine`、`egon-cola-platform-gateway-mcp-core` 及仓库内直接 FQCN 消费者            |
| Source Requirement | 用户确认：按功能/service domain 和 common/* 深度梳理 Gateway Engine 与 MCP Core，分离实体、业务类和适配器，改善可读性                 |
| Baseline Revision  | `main@26ba1413`；工作区保留用户已暂存的 GatewayContractVersions.java 删除；本 Plan/Spec 不处理该变更                       |
| Implements Spec    | [Gateway Engine 与 MCP Core 功能域分包设计](../spec/2026-08-19-13-51-gateway-engine-mcp-package-refactor.md) |
| Spec Status        | `Accepted`                                                                                           |
| Spec Revision      | `Updated 2026-08-19 14:28 CST`；用户于 2026-08-19 回复“确认”；DEC-005 已关闭并选择直接迁移                              |
| Effective Specs    | [Gateway Engine 与 MCP Core 功能域分包设计](../spec/2026-08-19-13-51-gateway-engine-mcp-package-refactor.md) |
| Depends On Plans   | `None`                                                                                               |
| Supersedes         | `None`                                                                                               |
| Superseded By      | `None`                                                                                               |
| Related Plans      | `None`                                                                                               |

## 1. Summary

本 Plan 实施唯一有效的 Accepted Spec，采用两个按模块边界组织的实现 Step：Step 1 完成 MCP Core 的
common/capability/service/domain/adapter 迁移及全部直接消费者更新；Step 2 完成 Engine 的 bootstrap、common
capability、HTTP、RPC、Operation、Rule、Engine MCP、WebSocket 迁移及全部直接消费者更新。

两个 Step 都是源码级 package/import/package-info 重构，不新增 Maven module、业务行为、协议字段、配置 key、数据库变更或运行时调用层。每个
Step 先建立包边界 RED gate，再完成该模块的原子包迁移并运行模块回归；最终通过全仓库 FQCN 扫描、根包/common 依赖扫描、Gateway
reactor 测试和 `git diff --check` 证明完成。真实 DDC、Redis、Provider、MCP 拓扑仍由用户后续启动服务验证，本 Plan 不自动启动服务。

## 2. Target Spec and Effective Design

### 2.1 Primary target

- Path: [Gateway Engine 与 MCP Core 功能域分包设计](../spec/2026-08-19-13-51-gateway-engine-mcp-package-refactor.md)
- Status: `Accepted`
- Revision: `Updated 2026-08-19 14:28 CST`; baseline `main@26ba1413`。
- Approval evidence: 用户在 Spec Review 后明确回复“确认”；该回复批准按 Spec 进入 Plan 阶段，不代表本 Plan 已获执行批准。

### 2.2 Effective Spec set

| Role    | Spec/link                                                                                            | Status/revision                          | Effective sections | Why included                             |
|---------|------------------------------------------------------------------------------------------------------|------------------------------------------|--------------------|------------------------------------------|
| Primary | [Gateway Engine 与 MCP Core 功能域分包设计](../spec/2026-08-19-13-51-gateway-engine-mcp-package-refactor.md) | `Accepted`; Updated 2026-08-19 14:28 CST | §1–§20 全部          | 唯一规定目标包树、角色语义、保持不变的运行时边界、测试和兼容策略的有效 Spec |

### 2.3 Superseded or excluded content

- Spec 的 Related Specs 只提供现有 HTTP/RPC/MCP 语义背景；它们不是本 Plan 的额外变更来源，不覆盖或重定义本次包树。
- `gateway-contract`、`gateway-core`、POM、数据库 migration、Admin Web、协议 payload、配置 key、Bean 名称、方法签名和运行时状态均保持不变。
- 不把 `GatewayEngineConfiguration` 拆成多个配置类，不拆大方法，不引入 DDD/COLA、Facade、Factory、Repository Port 或旧包
  wrapper。

## 3. Effective Requirements and Acceptance

| Requirement | Source Spec section | Effective statement            | Observable acceptance                                                                                        | Implementation impact                                       |
|-------------|---------------------|--------------------------------|--------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------|
| `REQ-001`   | Spec §4             | Engine 和 MCP Core 以功能域作为第一包层级  | HTTP、RPC、MCP、Rule、Operation、Provider 和 MCP capability 不再以技术平铺包混在同一根包                                         | 两个模块的 production/test package declaration、路径和 import        |
| `REQ-002`   | Spec §4             | 功能域内部区分 service/domain/adapter | 服务编排、状态/策略/上下文模型、外部技术实现可由包名直接识别                                                                              | 按类职责移动到 role package；不改类字段、方法和嵌套类型                          |
| `REQ-003`   | Spec §4             | common/* 仅承载真实跨域公共能力并单向依赖      | `engine.common` 不 import Engine feature；`mcp.common` 不 import capability；Provider/Security/Traffic 等只有一个公共归属 | common capability 迁移、边界测试和静态 import gate                    |
| `REQ-004`   | Spec §4、§7.3.5、§16  | 保持 Engine 启动和运行边界兼容            | `GatewayEngineApplication` 根包和 POM mainClass 不变；配置 Bean、测试、协议和状态断言通过                                         | bootstrap relocation、全量 import 更新、模块/跨模块验证                  |
| `REQ-005`   | Spec §4、§7.0、§17    | 不借分包引入过度抽象或新模块                 | 无新 Maven module、DTO、mapper、全局 service/domain、运行时 facade、行为修复                                                 | 只做 MOVE/RENAME/package/import/package-info 和 boundary tests |
| `REQ-006`   | Spec §4、§14         | 包边界可持续验证                       | Engine/MCP Core 根包和 common 规则由 JUnit 测试与全仓库静态扫描持续锁定                                                          | 新增两个 boundary test 及最终静态门禁                                  |

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

1. 先在 MCP Core 写入包边界测试，使当前平铺的 `protocol`、`transport`、`security`、`telemetry` 和 capability 根包状态形成可观察
   RED；随后一次性移动 MCP Core 全部 production/test 文件并更新所有直接消费者。
2. 在 Engine 写入根包/common 规则测试；保持 `GatewayEngineApplication` 的现有 FQCN 和 POM `mainClass`，将其余 Engine 类按
   Spec 目标树一次性迁移，避免 HTTP/RPC/Rule 之间出现半迁移 FQCN。
3. 每个模块完成后先跑自身 Maven reactor test，再进行全仓库 FQCN 扫描；只允许保留根应用入口、测试中用于禁止依赖的前缀字符串以及不属于
   Java import 的历史文档内容。
4. 最后运行 Gateway module focused test 和静态 diff gate。该 Plan 不启动服务，因此不把 Maven/module 证据表述成真实
   Spring、DDC、Redis、Provider 或远程 MCP 拓扑证据。

### 4.2 Test-first strategy

| Behavior/constraint                 | RED test and expected RED reason                                                                                    | Minimum GREEN                                                                             | Refactor/wiring                                   |
|-------------------------------------|---------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------|---------------------------------------------------|
| MCP root/capability/common boundary | `McpCorePackageBoundaryTest` 在当前树上因 `common/*` 和 role 子包不存在、旧平铺目录仍存在而失败                                             | 创建 `common.protocol/transport/security/telemetry` 与 capability role 目录，根包只保留 package-info | 只重写 package-info 和 imports，不改变 Handler/Driver 调用链 |
| Engine root/common boundary         | `GatewayEnginePackageBoundaryTest` 在当前树上因 root 仍有 Configuration/Runtime/Properties 而失败，并检查目标 common/feature role 目录 | Application/package-info 保留在 root；bootstrap、common、feature role 目录完成                      | 不改变 Spring annotation、Bean 名、POM mainClass 或构造顺序  |
| Existing MCP behavior               | 迁移后的原有 MCP tests 在 package/import 变更阶段编译失败或找不到目标 FQCN                                                               | 保留原断言、fixture、Reactor 流和错误语义，仅更新 package/import                                           | 不拆 Handler/Driver，不增加 wrapper                     |
| Existing Engine behavior            | Engine reactor test 在旧 import 存在时编译失败                                                                               | 全量 package/import/package-info 迁移后原有 Engine tests 和配置测试通过                                 | 不改变 HTTP/RPC/MCP/Rule/Traffic/Security 行为         |

这次没有新的业务行为需要先写行为 RED；测试先行约束落在可执行的包边界断言和现有行为测试的编译/回归 gate
上。边界测试本身在生产移动前必须能编译，并应只因预期的目录/包状态失败，不能因 fixture、外部服务或凭证失败。

### 4.3 Sequential and parallel boundaries

| Step              | Depends on | May run in parallel with | Must not overlap with                                                      | Reason                                                            |
|-------------------|------------|--------------------------|----------------------------------------------------------------------------|-------------------------------------------------------------------|
| Step 1 — MCP Core | None       | None                     | `gateway-engine-mcp-core` source/test and its direct consumer files        | Engine imports MCP Core FQCN；先完成共享运行时包，避免 Engine 同时处理两套旧包名        |
| Step 2 — Engine   | Step 1     | None                     | Engine source/test、Gateway test suite direct consumers、RPC starter JavaDoc | Engine 同时引用 MCP Core、core/contract 和全部 common/feature；必须以一个编译闭环完成 |

### 4.4 Commit boundaries

每个 Step 只产生一个 path-limited semantic commit。Step 1 的提交范围是 MCP Core 生产/测试树和仓库中对 MCP Core FQCN
的直接消费者；Step 2 的提交范围是 Engine 生产/测试树和对 Engine FQCN 的直接消费者。不得使用 `git add .`，不得触碰用户已暂存的
`gateway-contract/.../GatewayContractVersions.java` 删除。

两个 Step 不按 HTTP/RPC 子域拆成多个 commit，是因为当前 Engine 的 `GatewayEngineConfiguration`
、Runtime、Rule、Operation、HTTP、RPC 和 common capability
互相引用；按子域拆分会制造暂时无法编译的半迁移状态、重复修改同一个消费者并扩大回滚面。模块级原子迁移仍保留清晰的功能域清单和逐文件目标映射。

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element                 | Spec necessity verdict/section        | Current repository evidence                                                         | Direct/reuse alternative            | Interaction/implementation cost             | Plan decision          |
|------------------------------|---------------------------------------|-------------------------------------------------------------------------------------|-------------------------------------|---------------------------------------------|------------------------|
| Feature-first package tree   | Spec §7.0 Verdict `Add`；§7.3.2/§7.3.3 | Engine 有 HTTP/RPC/Rule/MCP/Provider 等现有域；MCP Core 有 capability 包但角色混杂               | 保留平铺包不能从路径识别业务域和角色                  | package declaration/import/file move，无运行时交互 | Implement in Steps 1–2 |
| `engine.common.<capability>` | Spec §7.0 Verdict `Add`；§7.3.2        | Provider、Security、Traffic、Transport、Observability 被 HTTP/RPC/Rule/Operation 共同使用    | 在各 feature 复制会重复策略；全局 common 会继续混杂  | import migration，boundary scan              | Implement in Step 2    |
| `mcp.common.*`               | Spec §7.0 Verdict `Add`；§7.3.3        | protocol、transport、security、telemetry 被多个 MCP capability 引用                         | 留在 capability 会产生重复公共位置和交叉检索        | import migration，MCP module tests           | Implement in Step 1    |
| Bootstrap relocation         | Spec §7.0 Verdict `Modify`；§7.3.5     | `GatewayEngineConfiguration` 装配全模块；`GatewayEngineRuntime` 协调 HTTP/RPC/Rule/Provider | 留在 root 会继续堆积；拆方法会改变装配边界            | only package/import move                    | Implement in Step 2    |
| Boundary tests               | Spec §14 TEST-001/002                 | 当前已有模块 boundary test 风格，且本次目标要求可持续验证                                                | 只依赖 code review 不能阻止 root/common 回流 | 两个 JUnit static scans                       | Implement in Steps 1–2 |

审计未发现 fetch-then-forward API、可由可信上下文推导却新增的 caller-supplied 参数、重复 DTO/mapper、 speculative
cache/layer、无当前消费者的 compatibility facade 或必须引入的设计模式。选择的是 package-by-feature、curated common 和
adapter boundary；不新增 Strategy/Factory/Facade/DDD service，因为现有 Handler/Driver/Compiler/Registry/Strategy 已覆盖实际变化点。

### 4.6 Change-unit Dependency Matrix

| Change unit                                      | Requirements    | Proof/RED point                                   | Compile/runtime prerequisites             | Produces                                                                                                            | Consumers/unblocks                                                 | Owning Step |
|--------------------------------------------------|-----------------|---------------------------------------------------|-------------------------------------------|---------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------|-------------|
| MCP common protocol/transport/security/telemetry | REQ-001–003     | `McpCorePackageBoundaryTest`                      | contract/core dependency unchanged        | `mcp.common.*` FQCN                                                                                                 | all MCP capability classes, Engine MCP integration, Admin MCP code | Step 1      |
| MCP capability role packages                     | REQ-001–003     | existing 10 MCP Core tests                        | MCP common FQCN available                 | `app`, `completion`, `prompt`, `remote`, `resource`, `rule`, `server`, `subscription`, `task`, `tool` role packages | MCP Core tests and Admin/test-suite consumers                      | Step 1      |
| Engine bootstrap/root                            | REQ-004         | `GatewayEnginePackageBoundaryTest` root assertion | unchanged POM mainClass                   | `bootstrap.config`, `bootstrap.lifecycle`, `common.config`                                                          | Engine component scan and Runtime tests                            | Step 2      |
| Engine common capabilities                       | REQ-001–003     | boundary common reverse-import assertion          | MCP Core Step 1 and gateway core/contract | `engine.common.provider/security/traffic/transport/observability`                                                   | HTTP/RPC/Rule/Operation/MCP                                        | Step 2      |
| Engine feature domains                           | REQ-001/002/004 | existing Engine tests                             | all Engine common FQCN available          | `engine.http`, `engine.rpc`, `engine.operation`, `engine.rule`, `engine.mcp` role packages                          | Engine config, Gateway test suite and runtime entry                | Step 2      |
| Repository-wide direct consumer rewrite          | REQ-004/006     | old FQCN scan                                     | both module package moves complete        | no stale Java import/Javadoc consumer                                                                               | Admin, Gateway integration tests, RPC starter docs                 | Step 2      |

## 5. Change File Tree

### 5.1 Target tree

```text
top.egon.cola.component.gateway.engine
├── GatewayEngineApplication.java                         KEEP: Spring Boot/mainClass anchor
├── bootstrap
│   ├── config/GatewayEngineConfiguration.java             MOVE
│   └── lifecycle/GatewayEngineRuntime.java                MOVE
├── common
│   ├── config/GatewayEngineRuntimeProperties.java         MOVE
│   ├── provider/{domain,service,adapter}
│   ├── security/{domain,service,adapter}
│   ├── traffic/{domain,service,adapter}
│   ├── transport/{domain,service}
│   └── observability/{domain,service,adapter}
├── http/{domain,service,adapter,security,cors,proxy,common}
│   └── websocket/{domain,service,adapter}
├── rpc/{domain,service,adapter,security}
├── operation/{service,adapter}
├── rule/{domain,service,repository,adapter/json}
└── mcp/{domain,service,adapter/{remote,security}}

top.egon.cola.component.gateway.mcp
├── common/{protocol,transport,security,telemetry}
├── app/{domain,service}
├── completion/service
├── prompt/{domain,service}
├── remote/{domain,service}
├── resource/{domain,service,adapter}
├── rule/{domain,service}
├── server/{domain,service/handler}
├── subscription/service
├── task/{domain,service}
└── tool/service
```

### 5.2 Exact path roots and migration manifests

The following roots are literal repository-relative prefixes; every listed source filename is moved with the same
basename to the stated target package, and every unlisted package is outside this change:

| Alias         | Literal repository-relative root                                                                                                         | Baseline evidence         |
|---------------|------------------------------------------------------------------------------------------------------------------------------------------|---------------------------|
| `MCP_MAIN`    | `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-core/src/main/java/top/egon/cola/component/gateway/mcp/`  | 89 production Java files  |
| `MCP_TEST`    | `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-core/src/test/java/top/egon/cola/component/gateway/mcp/`  | 10 test Java files        |
| `ENGINE_MAIN` | `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/` | 195 production Java files |
| `ENGINE_TEST` | `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/test/java/top/egon/cola/component/gateway/engine/` | 98 test Java files        |

#### MCP Core production manifest

| Current relative path set    | Target package role                                                      | Exact symbols/file names                                                                                                                                                                                                                                                                                                                                                                    |
|------------------------------|--------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `MCP_MAIN/app/`              | `mcp.app.domain` or `mcp.app.service`                                    | `AppUiResourceDriver.java`, `McpAppRuntime.java`, `McpAppSecurityValidator.java`, `package-info.java`                                                                                                                                                                                                                                                                                       |
| `MCP_MAIN/completion/`       | `mcp.completion.service`                                                 | `DictionaryCompletionProvider.java`, `McpCompletionHandler.java`, `McpCompletionProvider.java`, `OperationCompletionProvider.java`, `package-info.java`                                                                                                                                                                                                                                     |
| `MCP_MAIN/prompt/`           | `mcp.prompt.domain` or `mcp.prompt.service`                              | `McpPromptDriver.java`, `McpPromptsGetHandler.java`, `McpPromptsListHandler.java`, `OperationPromptDriver.java`, `StaticPromptDriver.java`, `StrictPromptTemplate.java`, `package-info.java`                                                                                                                                                                                                |
| `MCP_MAIN/protocol/`         | `mcp.common.protocol`                                                    | `AbstractMcpDialectAdapter.java`, `HttpMcpRequest.java`, `LegacySseMcpAdapter.java`, `McpDialectAdapter.java`, `McpJsonRpcCodec.java`, `McpProtocolException.java`, `RcMcpDialectAdapter.java`, `StableMcpDialectAdapter.java`, `package-info.java`                                                                                                                                         |
| `MCP_MAIN/remote/`           | `mcp.remote.domain` or `mcp.remote.service`                              | `McpCapabilitySynchronizer.java`, `McpDialectTranslator.java`, `McpNamespaceRouter.java`, `McpRemoteClientPool.java`, `McpRemoteEndpointValidator.java`, `RemoteMcpCompletionProvider.java`, `RemoteMcpPromptDriver.java`, `RemoteMcpResourceDriver.java`, `RemoteMcpToolDriver.java`, `package-info.java`                                                                                  |
| `MCP_MAIN/resource/`         | `mcp.resource.domain`, `mcp.resource.service`, or `mcp.resource.adapter` | `DatabaseSchemaResourceDriver.java`, `McpResourceCatalog.java`, `McpResourceDriver.java`, `McpResourceTemplatesListHandler.java`, `McpResourceUriValidator.java`, `McpResourcesListHandler.java`, `McpResourcesReadHandler.java`, `ObjectStorageResourceDriver.java`, `OperationResourceDriver.java`, `StaticBlobResourceDriver.java`, `StaticTextResourceDriver.java`, `package-info.java` |
| `MCP_MAIN/rule/`             | `mcp.rule.domain` or `mcp.rule.service`                                  | `CompiledMcpRules.java`, `McpRuleCompiler.java`, `package-info.java`                                                                                                                                                                                                                                                                                                                        |
| `MCP_MAIN/security/`         | `mcp.common.security`                                                    | `McpSecurityDigests.java`, `McpSecurityGate.java`, `package-info.java`                                                                                                                                                                                                                                                                                                                      |
| `MCP_MAIN/server/`           | `mcp.server.domain` or `mcp.server.service`                              | `McpMethodDispatcher.java`, `McpMethodHandler.java`, `McpRequestContext.java`, `package-info.java`                                                                                                                                                                                                                                                                                          |
| `MCP_MAIN/server/handler/`   | `mcp.server.service.handler`                                             | `McpDiscoverHandler.java`, `McpInitializeHandler.java`, `McpInitializedHandler.java`, `McpPingHandler.java`, `McpServerDescription.java`, `package-info.java`                                                                                                                                                                                                                               |
| `MCP_MAIN/subscription/`     | `mcp.subscription.service`                                               | `McpResourceSubscribeHandler.java`, `McpSubscriptionService.java`, `McpSubscriptionsListenHandler.java`, `package-info.java`                                                                                                                                                                                                                                                                |
| `MCP_MAIN/task/`             | `mcp.task.domain` or `mcp.task.service`                                  | `McpTask.java`, `McpTaskExecutor.java`, `McpTaskService.java`, `McpTaskStateMachine.java`, `McpTaskStore.java`, `McpTasksCancelHandler.java`, `McpTasksGetHandler.java`, `McpTasksUpdateHandler.java`, `package-info.java`                                                                                                                                                                  |
| `MCP_MAIN/telemetry/`        | `mcp.common.telemetry`                                                   | `McpTelemetry.java`, `package-info.java`                                                                                                                                                                                                                                                                                                                                                    |
| `MCP_MAIN/tool/`             | `mcp.tool.service`                                                       | `McpResultBinder.java`, `McpToolCatalog.java`, `McpToolsCallHandler.java`, `McpToolsListHandler.java`, `package-info.java`                                                                                                                                                                                                                                                                  |
| `MCP_MAIN/transport/`        | `mcp.common.transport`                                                   | `McpHttpRequest.java`, `McpHttpResponse.java`, `McpSessionStore.java`, `McpSubscriptionEventStore.java`, `package-info.java`                                                                                                                                                                                                                                                                |
| `MCP_MAIN/package-info.java` | `mcp` root                                                               | Keep root module documentation; root has no runtime class after migration                                                                                                                                                                                                                                                                                                                   |

`HttpMcpRequest` in `mcp.common.protocol` and `McpHttpRequest` in `mcp.common.transport` remain two different records
with their current fields and semantics; the similar names are not merged.

#### MCP Core test manifest

| Current relative test path                           | Target test package               |
|------------------------------------------------------|-----------------------------------|
| `MCP_TEST/app/McpAppSecurityTest.java`               | `mcp.app.domain`                  |
| `MCP_TEST/completion/McpCompletionTest.java`         | `mcp.completion.service`          |
| `MCP_TEST/prompt/McpPromptFlowTest.java`             | `mcp.prompt.service`              |
| `MCP_TEST/protocol/McpDialectCompatibilityTest.java` | `mcp.common.protocol`             |
| `MCP_TEST/remote/McpFederationTest.java`             | `mcp.remote.service`              |
| `MCP_TEST/resource/McpResourceFlowTest.java`         | `mcp.resource.service`            |
| `MCP_TEST/security/McpSecurityGateTest.java`         | `mcp.common.security`             |
| `MCP_TEST/server/McpMethodDispatcherTest.java`       | `mcp.server.service`              |
| `MCP_TEST/task/McpTaskStateMachineTest.java`         | `mcp.task.domain`                 |
| `MCP_TEST/tool/McpLocalToolFlowTest.java`            | `mcp.tool.service`                |
| `MCP_TEST/McpCorePackageBoundaryTest.java`           | New file; root test package `mcp` |

#### Engine production manifest

| Current relative path set                                  | Target package role                                                                                 | Exact symbols/file names                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
|------------------------------------------------------------|-----------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ENGINE_MAIN/GatewayEngineConfiguration.java`              | `engine.bootstrap.config`                                                                           | `GatewayEngineConfiguration.java`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| `ENGINE_MAIN/GatewayEngineRuntime.java`                    | `engine.bootstrap.lifecycle`                                                                        | `GatewayEngineRuntime.java`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| `ENGINE_MAIN/GatewayEngineRuntimeProperties.java`          | `engine.common.config`                                                                              | `GatewayEngineRuntimeProperties.java`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| `ENGINE_MAIN/package-info.java`                            | `engine` root                                                                                       | Keep root documentation and state that only Application is a runtime root class                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| `ENGINE_MAIN/balance/`                                     | `engine.common.provider.domain/service`                                                             | `LoadBalancerType.java`, `ProviderLoadBalancer.java`, `ProviderLoadBalancers.java`, `ProviderSelectionHandle.java`, `package-info.java`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| `ENGINE_MAIN/discovery/`                                   | `engine.common.provider.domain/service/adapter`                                                     | `ActiveHealthProbePolicy.java`, `ActiveHealthTracker.java`, `DdcProviderServiceRegistryAdapter.java`, `DirectoryProviderSelector.java`, `GatewayProviderPolicyCompiler.java`, `HttpProviderActiveHealthProbe.java`, `PassiveHealthPolicy.java`, `PassiveHealthTracker.java`, `ProviderActiveHealthMonitor.java`, `ProviderActiveHealthProbe.java`, `ProviderCallOutcome.java`, `ProviderCallOutcomeRecorder.java`, `ProviderCandidateFilter.java`, `ProviderCandidateFilterResult.java`, `ProviderCandidateStage.java`, `ProviderDirectory.java`, `ProviderPolicyOverride.java`, `ProviderSelectionPolicy.java`, `RpcProviderActiveHealthProbe.java`, `RuntimeProviderPolicy.java`, `package-info.java`                                                                                                                                               |
| `ENGINE_MAIN/security/`                                    | `engine.common.security.domain/service/adapter`                                                     | `GatewaySecurityCapabilityRegistry.java`, `GatewaySecurityChain.java`, `GatewaySecurityException.java`, `GatewaySecurityPolicyCompiler.java`, `GatewaySecurityResult.java`, `GatewayTransportSecurity.java`, `GatewayTransportSecurityEndpoint.java`, `GatewayTransportSecurityEndpointConfiguration.java`, `TrustedClientAddressResolver.java`, `TrustedIdentitySanitizer.java`, `package-info.java`                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| `ENGINE_MAIN/traffic/`                                     | `engine.common.traffic.domain/service/adapter`                                                      | `DistributedTokenBucketRateLimiter.java`, `GatewayAttemptExecutor.java`, `GatewayBulkheadRegistry.java`, `GatewayCircuitBreakerRegistry.java`, `GatewayPolicyKeyCompiler.java`, `GatewayRequestResourceGuard.java`, `GatewayResourceLimits.java`, `GatewayRetryPolicy.java`, `GatewayTrafficContext.java`, `GatewayTrafficGovernance.java`, `GatewayTrafficPolicyCompiler.java`, `GatewayTrafficRejectedException.java`, `LocalTokenBucketPolicy.java`, `LocalTokenBucketRateLimiter.java`, `ProviderCallClassification.java`, `RateLimitDecision.java`, `RateLimitFailureMode.java`, `RedisTokenBucketExecutor.java`, `RedissonRedisTokenBucketExecutor.java`, `RuntimeTrafficPolicy.java`, `TrafficPolicyScope.java`, `TrafficPolicyType.java`, `package-info.java`                                                                                 |
| `ENGINE_MAIN/transport/`                                   | `engine.common.transport.domain/service`                                                            | `GatewayCancellation.java`, `GatewayCommitGuard.java`, `GatewayCommitPoint.java`, `GatewayConnectTimeoutException.java`, `GatewayResponseHeaderTimeoutException.java`, `GatewayRetryGate.java`, `GatewayStreamDirection.java`, `GatewayStreamIdleTimeoutException.java`, `GatewayTotalTimeoutException.java`, `GatewayTransportDispatcher.java`, `GatewayTransportTimeoutException.java`, `GatewayTransportTimeouts.java`, `GatewayWebSocketIdleTimeoutException.java`, `package-info.java`                                                                                                                                                                                                                                                                                                                                                           |
| `ENGINE_MAIN/observability/`                               | `engine.common.observability.domain/service/adapter`                                                | `GatewayCallAccessLogger.java`, `GatewayCallCompletionListener.java`, `GatewayCallEventDispatcher.java`, `GatewayCallEventSerializer.java`, `GatewayCallEventSink.java`, `GatewayCallMetricsListener.java`, `GatewayCallObservation.java`, `GatewayTelemetry.java`, `KafkaGatewayCallEventSink.java`, `package-info.java`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| `ENGINE_MAIN/http/`                                        | `engine.http.domain/service/adapter/security/cors/proxy/common`                                     | `AbstractGatewayHttpStageExchange.java`, `DefaultGatewayHttpDataPlaneHandler.java`, `GatewayBodySizeLimiter.java`, `GatewayCompositeHttpDataPlaneHandler.java`, `GatewayCorsException.java`, `GatewayCorsProcessor.java`, `GatewayHeaderFilter.java`, `GatewayHttpDataPlaneHandler.java`, `GatewayHttpEngineProperties.java`, `GatewayHttpExecutionPipeline.java`, `GatewayHttpFlushMode.java`, `GatewayHttpListener.java`, `GatewayHttpSecurityProcessor.java`, `GatewayHttpServer.java`, `GatewayInboundHttpRequest.java`, `GatewayOutboundHttpResponse.java`, `GatewayRequestBodyTooLargeException.java`, `GatewayResponseBodyTooLargeException.java`, `HttpUpstreamAdapter.java`, `HttpUpstreamRequest.java`, `ProviderSelector.java`, `ReactorNettyHttpUpstreamAdapter.java`, `RuleBackedHttpGatewaySecurityProcessor.java`, `package-info.java` |
| `ENGINE_MAIN/http/buffer/` and `ENGINE_MAIN/http/logging/` | `engine.http.common.buffer` and `engine.http.common.logging`                                        | `GatewayDataBufferOwnership.java`, `GatewayDataBufferPipeline.java`, `GatewayBodyLogDirection.java`, `GatewayBodyLogEvent.java`, `GatewayBodyLogTap.java`, both package-info files                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| `ENGINE_MAIN/http/proxy/`                                  | `engine.http.proxy.domain/service`                                                                  | `AggregatedHttpProxyStrategy.java`, `GatewayHttpAttemptCoordinator.java`, `GatewayHttpProxyContext.java`, `GatewayHttpProxyStrategy.java`, `GatewayHttpProxyStrategySelector.java`, `GatewayHttpResponseSemantics.java`, `StreamingHttpProxyStrategy.java`, `package-info.java`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| `ENGINE_MAIN/websocket/`                                   | `engine.http.websocket.domain/service/adapter`                                                      | `GatewayPreparedWebSocketSession.java`, `GatewayWebSocketCloseStatus.java`, `GatewayWebSocketFrame.java`, `GatewayWebSocketFrameType.java`, `GatewayWebSocketHandshakeResult.java`, `GatewayWebSocketObserver.java`, `GatewayWebSocketPeer.java`, `GatewayWebSocketProxy.java`, `GatewayWebSocketProxyContext.java`, `ReactorNettyWebSocketPeer.java`, `ReactorNettyWebSocketUpstreamAdapter.java`, `WebSocketUpstreamAdapter.java`, `package-info.java`                                                                                                                                                                                                                                                                                                                                                                                              |
| `ENGINE_MAIN/rpc/`                                         | `engine.rpc.domain/service/adapter/security` plus `engine.operation.adapter` for HTTP-to-RPC bridge | `GatewayRpcSecurityProcessor.java`, `HttpRpcDynamicMessageBridge.java`, `HttpRpcUpstreamAdapter.java`, `ProtobufDescriptorRegistry.java`, `RawByteMarshaller.java`, `RpcGatewayForwarder.java`, `RpcGatewayHandlerRegistry.java`, `RpcGatewayServer.java`, `RpcGatewaySlotProperties.java`, `RpcGatewaySlotRuntime.java`, `RpcGatewaySubsystemState.java`, `RpcMethodIndex.java`, `RpcMethodIndexCompiler.java`, `RpcProviderChannelCache.java`, `RpcProviderChannelKey.java`, `RuleBackedRpcGatewaySecurityProcessor.java`, `RuntimeRpcRoute.java`, `package-info.java`                                                                                                                                                                                                                                                                              |
| `ENGINE_MAIN/operation/`                                   | `engine.operation.service/adapter`                                                                  | `DefaultGatewayOperationTransport.java`, `EngineGatewayOperationInvoker.java`, `package-info.java`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| `ENGINE_MAIN/rule/`                                        | `engine.rule.domain/service/repository/adapter/json`                                                | `CompiledGatewayRules.java`, `EngineGatewayRuleCompiler.java`, `GatewayRuleActivationApplier.java`, `GatewayRuleApplierRegistrar.java`, `GatewayRuleApplyStage.java`, `GatewayRuleChunkStore.java`, `GatewayRuleJsonCodec.java`, `GatewayRuleLkgRepository.java`, `GatewayRuleRuntimeStatus.java`, `package-info.java`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| `ENGINE_MAIN/mcp/`                                         | `engine.mcp.domain/service/adapter`                                                                 | `FileSystemMcpAppArtifactReader.java`, `HttpMcpTaskServiceTokenSupplier.java`, `JdbcMcpRuntimeTaskStore.java`, `McpAuditPublisher.java`, `McpEngineHttpHandler.java`, `McpGatewayIdentityAuthenticator.java`, `McpRuntimeHealthIndicator.java`, `McpRuntimeProperties.java`, `McpTaskOperationExecutor.java`, `McpTaskServiceTokenSupplier.java`, `McpTaskWorker.java`, `MicrometerMcpTelemetry.java`, `RedisMcpSessionStore.java`, `package-info.java`                                                                                                                                                                                                                                                                                                                                                                                               |
| `ENGINE_MAIN/mcp/remote/` and `ENGINE_MAIN/mcp/security/`  | `engine.mcp.adapter.remote` and `engine.mcp.adapter.security`                                       | `ReactorNettyRemoteMcpClient.java`, `ReferenceRemoteAuthProvider.java`, `JdbcMcpApprovalAdapter.java`, `Rbac3McpAuthorizationAdapter.java`, both package-info files                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |

#### Engine test manifest

All existing Engine tests move with their production responsibility. The exact names are:

- Root tests: `GatewayEngineConfigurationTest.java` → `bootstrap.config`; `GatewayEngineRuntimeTest.java` →
  `bootstrap.lifecycle`; `GatewayEngineRuntimePropertiesTest.java` → `common.config`;
  `GatewayEngineApplicationConfigurationTest.java`, `GatewayEngineClockQualificationTest.java`,
  `GatewayEngineTaskStoreConfigurationTest.java`, and `IdpAdapterRuntimeClasspathTest.java` remain under the root test
  package with updated imports where required.
- Provider tests: `balance/ProviderLoadBalancersTest.java`, `discovery/ActiveHealthTrackerTest.java`,
  `discovery/GatewayProviderPolicyCompilerTest.java`, `discovery/MutableClock.java`,
  `discovery/PassiveHealthTrackerTest.java`, `discovery/ProviderActiveHealthMonitorTest.java`,
  `discovery/ProviderCandidateFilterTest.java`, `discovery/ProviderDirectoryTest.java` → `common.provider` role
  packages.
- Security tests: `GatewayIdentityOnlySecurityTest.java`, `GatewayOriginalBearerForwardingTest.java`,
  `GatewaySecurityCapabilityRegistryTest.java`, `GatewaySecurityChainTest.java`,
  `GatewaySecurityPolicyCompilerTest.java`, `GatewayTransportSecurityEndpointConfigurationTest.java`,
  `GatewayTransportSecurityTest.java`, `IdpTrustedIdentitySanitizerTest.java`, `TrustedClientAddressResolverTest.java`,
  `TrustedIdentitySanitizerTest.java` → `common.security` role packages.
- Traffic tests: `GatewayPolicyKeyCompilerTestSupport.java`, `GatewayRateLimitTest.java`, `GatewayResilienceTest.java`,
  `GatewayTrafficGovernanceTest.java`, `RedissonRedisTokenBucketExecutorTest.java` → `common.traffic` role packages.
- Transport/observability tests: all files under current `transport/` including `fixture/StreamingHttpTestUpstream.java`
  and `fixture/WebSocketTestUpstream.java`, and all five current `observability/` tests → `common.transport` and
  `common.observability` role packages.
- HTTP/WebSocket tests: all current `http/`, `http/buffer/`, `http/logging/`, `http/proxy/`, and `websocket/` test
  files → the corresponding target HTTP role packages.
- RPC/Operation tests: all current `rpc/` tests, including `HttpRpcDynamicMessageBridgeTest.java` and
  `HttpRpcUpstreamAdapterTest.java`, plus `operation/EngineGatewayOperationInvokerTest.java` → target RPC/Operation
  roles.
- Rule/MCP tests: all six current `rule/` tests and all current `mcp/`, `mcp/remote/`, and `mcp/security/` tests →
  target Rule/Engine MCP roles.

#### Direct consumer manifest outside the two module source trees

The following exact files must be part of the corresponding module commit when their imported FQCN moves:

- MCP Core consumers:
  `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/bootstrap/GatewayAdminConfiguration.java`,
  `.../admin/mcp/controller/McpApprovalController.java`, `.../admin/mcp/service/McpControlPlaneService.java`,
  `.../admin/mcp/service/McpValidationService.java`, `.../admin/rule/service/GatewayRuleCompiler.java`,
  `.../admin/src/test/java/top/egon/cola/component/gateway/admin/mcp/interfaces/McpApprovalControllerTest.java`,
  `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/mcp/McpCompleteReleaseIT.java`,
  `McpHaRecoveryIT.java`, and `McpSecurityIT.java`.
- Engine consumers:
  `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/annotation/LoadBalance.java`
  for the `LoadBalancerType` JavaDoc FQCN; Gateway test-suite `deployment/GatewayComposeConfigurationTest.java`,
  `live/GatewayRuleWireCompatibilityTest.java`, `mcp/McpHaRecoveryIT.java`, and `mcp/McpSecurityIT.java`; plus the
  existing Engine module files identified by the exact command in §8.3.
- Intentional non-consumers: `GatewayContractBoundaryTest.java`, `GatewayCoreBoundaryTest.java`, and
  `GatewayLiveTopologyIT.java` contain broad forbidden-prefix or unchanged root application-name assertions; they are
  reviewed by the static gate and only modified if the exact assertion contains a stale moved FQCN.

### 5.3 File operation inventory

| Operation       | Path/root                                                                                                                                                                     | Current evidence/symbol                                                  | Final symbols/state                                                       | Responsibility                    | Step   | Requirements        | Validation owner                 |
|-----------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------|---------------------------------------------------------------------------|-----------------------------------|--------|---------------------|----------------------------------|
| CREATE          | `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-core/src/test/java/top/egon/cola/component/gateway/mcp/McpCorePackageBoundaryTest.java`        | No current file                                                          | Static root/common/capability boundary assertions                         | MCP package architecture gate     | Step 1 | REQ-003/006         | MCP Core test                    |
| RENAME + MODIFY | `MCP_MAIN` manifest in §5.2                                                                                                                                                   | Current 15 top-level package groups and 89 production classes            | Target common/capability role packages; same symbols/fields/methods       | MCP Core runtime ownership        | Step 1 | REQ-001/002/003/005 | MCP Core compile/test            |
| RENAME + MODIFY | `MCP_TEST` manifest in §5.2                                                                                                                                                   | Current 10 tests                                                         | Target production-mirror test packages; same assertions/fixtures          | MCP Core regression coverage      | Step 1 | REQ-004/006         | MCP Core test                    |
| MODIFY          | Direct MCP consumer manifest in §5.2                                                                                                                                          | Existing Admin, Engine integration and Gateway test-suite imports        | New MCP Core FQCNs; no logic/endpoint change                              | Cross-module source compatibility | Step 1 | REQ-004/005         | Gateway reactor compile          |
| CREATE          | `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/test/java/top/egon/cola/component/gateway/engine/GatewayEnginePackageBoundaryTest.java` | No current file                                                          | Static Engine root/common/feature boundary assertions                     | Engine package architecture gate  | Step 2 | REQ-003/006         | Engine test                      |
| RENAME + MODIFY | `ENGINE_MAIN` manifest in §5.2                                                                                                                                                | Current 195 production classes and root Configuration/Runtime/Properties | Target bootstrap/common/feature role packages; Application FQCN unchanged | Engine runtime ownership          | Step 2 | REQ-001/002/004/005 | Engine compile/test              |
| RENAME + MODIFY | `ENGINE_TEST` manifest in §5.2                                                                                                                                                | Current 98 tests                                                         | Target production-mirror test packages; same assertions/fixtures          | Engine regression coverage        | Step 2 | REQ-004/006         | Engine test                      |
| MODIFY          | Direct Engine consumer manifest in §5.2                                                                                                                                       | Existing RPC JavaDoc and Gateway integration tests                       | New Engine FQCNs; unchanged root Application string and behavior          | Cross-module source compatibility | Step 2 | REQ-004/005         | Full Gateway reactor/static scan |

No Maven POM, contract class, database migration, frontend source, generated file, runtime configuration key, or
deployment descriptor is an affected file in this Plan.

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

- Read and obey repository `AGENTS.md` instructions; implementation must use path-limited edits and commits.
- Work from `/Users/mario/SelfProject/Egon-COLA`, branch `main`, baseline `26ba1413`.
- Preserve the pre-existing staged deletion at
  `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-contract/src/main/java/top/egon/cola/component/gateway/contract/version/GatewayContractVersions.java`.
- Do not stage or modify unrelated files; use `git diff --name-only`, `git diff --check`, and `git commit --only` with
  the Step-owned paths.
- Use the existing Maven Wrapper at repository root. Do not start the Engine, containers, databases, DDC, Redis, Kafka,
  Provider, or remote MCP services.

### 6.2 Build, test, and environment prerequisites

| Concern                 | Exact command/source                                                                                             | Required state                                                              | Validation boundary                                |
|-------------------------|------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------|----------------------------------------------------|
| Maven wrapper           | `./mvnw -B -ntp` from repository root                                                                            | Java 21/Maven Wrapper available as required by root README                  | static/module only                                 |
| MCP module              | `./mvnw -B -ntp -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-core -am test` | Step 1 package/import migration complete                                    | MCP Core and dependencies; no live services        |
| Engine module           | `./mvnw -B -ntp -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine -am test`   | Step 1 and Step 2 migration complete                                        | Engine and dependencies; Spring context tests only |
| Gateway focused reactor | `./mvnw -B -ntp -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test -am test`     | both Steps committed or working tree intentionally contains only Plan scope | Gateway test reactor; no gateway-live profile      |
| Static FQCN scan        | `rg -n -F` commands in §8.3                                                                                      | no stale old Java imports/package declarations                              | source/static only                                 |
| Diff hygiene            | `git diff --check`                                                                                               | no whitespace errors; unrelated staged deletion unchanged                   | source/diff only                                   |

### 6.3 Immutable constraints and approved decisions

- `GatewayEngineApplication` remains `top.egon.cola.component.gateway.engine.GatewayEngineApplication`; the POM
  `mainClass` remains unchanged.
- `GatewayEngineConfiguration` moves to `engine.bootstrap.config`; `GatewayEngineRuntime` moves to
  `engine.bootstrap.lifecycle`; neither class is decomposed.
- `service` means behavior/orchestration/Handler/Driver/SPI/state transitions; `domain` means existing
  state/policy/context/result/enum models and does not introduce DDD semantics; `adapter` means existing
  Spring/Netty/gRPC/JDBC/Redis/Kafka/filesystem/remote implementations.
- `common` is reserved for the concrete cross-feature capabilities selected in Spec §7.3; it must not import
  HTTP/RPC/MCP/Rule/Operation feature packages.
- No old-package compatibility facade is created because accepted DEC-005 selected direct migration. If an
  implementation-time external binary consumer is discovered outside the repository, stop that Step and return to the
  Spec decision; do not silently add a wrapper.

### 6.4 Plan Clarifications

| ID              | Small implementation inference                                                                                                                                                                                       | Repository evidence                                                                                                    | Why semantics are unchanged                                                                        | Impact if wrong                                                 |
|-----------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------|-----------------------------------------------------------------|
| `PLAN-CLAR-001` | Where two old package-info files would collide after `balance + discovery` merge, retain their documentation as role/root package-info files rather than creating duplicate `package-info` declarations for one FQCN | `ENGINE_MAIN/balance/package-info.java`, `ENGINE_MAIN/discovery/package-info.java`; target has one provider capability | Documentation placement changes only; class contracts and runtime packages follow the target table | only documentation location needs correction                    |
| `PLAN-CLAR-002` | Update all repository-visible direct consumers found by `rg`, including Admin production code, Gateway test-suite files and RPC starter JavaDoc, not only the Spec's first consumer example                          | §5.2 direct consumer scan found exact paths in Admin, Gateway test suite and `LoadBalance.java`                        | FQCN update is required for compilation/documentation and changes no behavior                      | a stale import/Javadoc would fail the final scan or compilation |
| `PLAN-CLAR-003` | Keep protocol `HttpMcpRequest` and transport `McpHttpRequest` as distinct target types                                                                                                                               | both records exist under different current packages and have different fields                                          | no merge, mapper or field change is introduced                                                     | an accidental merge would be a Spec violation                   |
| `PLAN-CLAR-004` | Place `HttpRpcDynamicMessageBridge` and `HttpRpcUpstreamAdapter` under `engine.operation.adapter`, while keeping RPC channel/marshaller/descriptor classes under `engine.rpc.adapter`                                | Spec §7.3.2 names HTTP-to-RPC bridge as Operation adapter; current classes are in `engine.rpc`                         | package ownership makes the existing composition visible without changing invocation signatures    | only target path/imports would need review                      |

## 7. Ordered File-by-file Implementation Steps

### Step 1 — Migrate MCP Core into common and capability role packages

- Requirements: `REQ-001`, `REQ-002`, `REQ-003`, `REQ-004`, `REQ-005`, `REQ-006`
- Dependencies: `None`
- Baseline state: MCP Core has 89 production classes in flat capability/technical packages and 10 tests; Admin, Engine,
  Gateway test-suite and RPC starter contain direct MCP FQCN consumers.
- Observable outcome: MCP Core exposes the target `mcp.common.*` and capability role packages, all direct consumers
  compile against the new FQCNs, and original MCP Core tests pass unchanged in behavior.
- End state: `mcp` root has no runtime class beyond package-info; `mcp.common` has protocol/transport/security/telemetry
  only; each capability has the Spec-defined domain/service/adapter ownership; no `mcp.common -> capability` import
  exists.
- Test-first gate:
  `Required — McpCorePackageBoundaryTest must compile first and fail only because current flat directories/common role directories do not satisfy the target assertions; after the move it must be GREEN before the Step commit.`
- Ordered files:

#### File 1 —
`CREATE egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-core/src/test/java/top/egon/cola/component/gateway/mcp/McpCorePackageBoundaryTest.java`

- Purpose: Define the MCP root/common/capability package contract before moving production classes.
- Symbols: `McpCorePackageBoundaryTest`, `rootContainsNoRuntimeTypesAfterMigration`,
  `commonPackagesDoNotDependOnCapabilities`, `capabilitiesExposeRolePackages`.
- Repository evidence: `mcp-core/src/test/java/top/egon/cola/component/gateway/mcp/*` uses JUnit 5; `gateway-contract`
  and `gateway-core` already use source-tree boundary assertions without a new architecture library.
- Dependencies and consumers: Reads `mcp-core/src/main/java`; the Step 1 production move must make its path assertions
  GREEN.
- Why now: This is the static RED contract for REQ-003/REQ-006 and does not depend on a live MCP server.
- Contract/signature changes: No production signature; assert that root has only `package-info.java`, required
  common/role directories exist, and files below `mcp.common` contain no import prefix for capability packages.
- Input/output and state mapping: `Path.of("src/main/java/top/egon/cola/component/gateway/mcp")` -> directory names and
  Java text -> JUnit assertions; a missing target directory or forbidden import produces an assertion failure with the
  exact path.
- Error and edge behavior: Skip no directory silently; if a target subtree is absent, fail with the expected target
  path; tolerate `package-info.java` as documentation and do not treat it as a runtime type.
- Implementation pseudocode:

```java
Path root = Path.of("src/main/java/top/egon/cola/component/gateway/mcp");
assertOnlyPackageInfoAtRoot(root);
for (String required : List.of("common/protocol", "common/transport", "common/security", "common/telemetry"))
    assertTrue(Files.isDirectory(root.resolve(required)), () -> "missing MCP target package " + required);
scanJavaFiles(root.resolve("common"));
assertNoImportPrefix(scanResults, "import top.egon.cola.component.gateway.mcp.app.", "tool.", "resource.", "task.");
```

- Verification contribution: `./mvnw -B -ntp -pl ...mcp-core -am -Dtest=McpCorePackageBoundaryTest test` must produce
  the expected RED before the production move and GREEN after it; the full module test is the final Step gate.
- After this file: The test compiles independently and fails for package-tree reasons only; no production logic or
  fixture is involved.

#### File 2 —
`RENAME egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-core/src/main/java/top/egon/cola/component/gateway/mcp/`

- Purpose: Move all 89 MCP Core production files in the §5.2 MCP production manifest into the target common/capability
  role packages.
- Symbols: Existing `Mcp*` Handler/Driver/Provider/Store/Runtime/Rule/Task/Protocol symbols; no new public type.
- Repository evidence: Current directories `app`, `completion`, `prompt`, `protocol`, `remote`, `resource`, `rule`,
  `security`, `server`, `subscription`, `task`, `telemetry`, `tool`, `transport`; target role map is defined in §5.2.
- Dependencies and consumers: `gateway-contract` and `gateway-core` remain unchanged; all MCP Core internal imports and
  the direct consumer files in File 4 must use the final target FQCNs.
- Why now: Production ownership must move after the boundary RED and before tests/consumers can become GREEN.
- Contract/signature changes: Change only filesystem path, `package` declaration, package-info prose and imports.
  Preserve record fields, method signatures, nested types, validation, error codes, Reactor publishers, Handler/Driver
  registration and state machine transitions.
- Input/output and state mapping: Current package + class basename -> one target role package from §5.2;
  `protocol.HttpMcpRequest` maps to `common.protocol`, while `transport.McpHttpRequest` maps to `common.transport`; no
  record field or mapper is added.
- Error and edge behavior: Keep `McpProtocolException`, security rejection, remote endpoint validation, resource URI
  validation, task state transitions and subscription/session behavior unchanged; compilation errors from missed imports
  are treated as migration failures, not fallback behavior.
- Implementation pseudocode:

```text
for each current MCP_MAIN file in the manifest:
    derive target directory from the class responsibility table;
    move the file and rewrite exactly one package declaration;
    replace old top.egon.cola.component.gateway.mcp FQCNs with target FQCNs;
    preserve annotations, method bodies, nested records/enums, exception mapping and package-info bilingual responsibility text;
reject any target path that would merge the two distinct HttpMcpRequest records.
```

- Verification contribution: The MCP module compiler proves every moved type and import is resolved;
  `McpCorePackageBoundaryTest` proves root/common direction; existing MCP tests prove protocol, resource, task, tool,
  prompt, remote, server and security semantics remain asserted.
- After this file: All MCP Core production files are at the target paths and the only expected compilation failures are
  direct consumers not yet processed by File 4.

#### File 3 —
`RENAME egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-core/src/test/java/top/egon/cola/component/gateway/mcp/`

- Purpose: Move the 10 existing MCP Core tests to mirror the final production role packages and update only
  package/import references.
- Symbols: `McpAppSecurityTest`, `McpCompletionTest`, `McpPromptFlowTest`, `McpDialectCompatibilityTest`,
  `McpFederationTest`, `McpResourceFlowTest`, `McpSecurityGateTest`, `McpMethodDispatcherTest`,
  `McpTaskStateMachineTest`, `McpLocalToolFlowTest`.
- Repository evidence: Exact baseline test filenames are listed in §5.2; POM supplies JUnit 5 and Reactor Core without a
  new test framework.
- Dependencies and consumers: Tests consume the moved MCP production classes and gateway contract/core types; their
  fixtures and assertions are the behavior preservation evidence.
- Why now: The tests must compile against final FQCNs in the same module commit and keep package-local test assumptions
  aligned.
- Contract/signature changes: Change test package/import lines only; do not change assertion values, mock behavior,
  fixture data, error code, response shape, task state or protocol method.
- Input/output and state mapping: Each current test path -> corresponding target role path from §5.2; test input/output
  mappings stay byte-for-byte and state-for-state equivalent.
- Error and edge behavior: Preserve negative security, invalid dialect, remote isolation, resource validation, task
  state and tool result assertions; a failure must identify a moved symbol, not be weakened by deleting an assertion.
- Implementation pseudocode:

```java
for each test in MCP_TEST manifest:
    move file to the package that owns the primary symbol under test;
    rewrite package declaration and all mcp-core imports to the target FQCN;
    leave JUnit annotations, assertions, fixture builders and expected errors unchanged;
    compile the selected test class before running the complete MCP Core suite.
```

- Verification contribution: The 10 existing tests remain the regression gate for MCP protocol compatibility, security,
  capability dispatch, remote federation, resource flow, task state and tool flow.
- After this file: MCP Core test sources are role-aligned and retain the same test count and behavioral assertions.

#### File 4 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/`

- Purpose: Update repository-visible direct MCP Core consumers so the reactor has one FQCN source of truth.
- Symbols: `GatewayAdminConfiguration`, `McpApprovalController`, `McpControlPlaneService`, `McpValidationService`,
  `GatewayRuleCompiler`, `McpApprovalControllerTest`, `McpCompleteReleaseIT`, `McpHaRecoveryIT`, `McpSecurityIT`.
- Repository evidence: `rg -l -F 'top.egon.cola.component.gateway.mcp.' --glob '*.java'` identified these files outside
  MCP Core; exact list is recorded in §5.2.
- Dependencies and consumers: Admin uses app/protocol/remote/rule/security MCP types; Gateway integration tests use
  transport/security/rule types; no controller route or service behavior changes.
- Why now: These are compile-time and test-time consumers of the moved MCP Core types and must be updated in the same
  module migration commit.
- Contract/signature changes: Replace only old MCP Core FQCNs with their target package names; keep Spring annotations,
  endpoints, constructor parameters, persistence keys, request/response assertions and test topology unchanged.
- Input/output and state mapping: Existing Admin/Gateway request or rule data -> same class simple name at target
  FQCN -> same result/error; no DTO or adapter is introduced.
- Error and edge behavior: Preserve MCP approval digest, endpoint validation, rule compiler and security gate behavior;
  if an old FQCN remains, fail compilation/static scan rather than adding a compatibility facade.
- Implementation pseudocode:

```text
scan the exact consumer files for old gateway.mcp imports and JavaDoc references;
replace each import/reference according to the MCP manifest, including common.security and rule.domain/service;
re-run the scan and assert no stale Java import remains outside intentional boundary strings;
compile Admin and Gateway test-suite consumers through the Gateway reactor without changing their call sites.
```

- Verification contribution: Gateway module compile confirms the direct consumers resolve; Admin MCP tests and Gateway
  test-suite compile prove no source compatibility gap was missed.
- After this file: MCP Core migration is source-complete across the repository, with no wrapper, duplicate model or
  behavior change.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command:
  `./mvnw -B -ntp -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-core -am test`
- Expected result: MCP Core compiles; all existing 10 MCP Core tests plus `McpCorePackageBoundaryTest` pass; no expected
  stale MCP import remains in the direct consumer scan.
- Failure returns to: File 2 for target package/import errors; File 3 for test package/assertion errors; File 4 for
  Admin/Gateway consumer errors; Spec decision DEC-005 if an external binary consumer is found.
- Completion criteria: Target common/capability tree exists; root/common boundary test is GREEN; all MCP Core tests
  pass; direct consumer FQCN scan is clean; `git diff --check` is clean.
- Rollback: Revert only the Step 1 commit with `git revert COMMIT` after verifying its path list; do not touch the
  unrelated staged contract deletion; if uncommitted, restore only the Step 1 path set through the same move map.
- Commit paths:
  `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-core/src/test/java/top/egon/cola/component/gateway/mcp/McpCorePackageBoundaryTest.java`,
  `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-core/src/main/java/top/egon/cola/component/gateway/mcp/`,
  `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-core/src/test/java/top/egon/cola/component/gateway/mcp/`,
  `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/`,
  `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/`
- Commit: `refactor(gateway-mcp-core): partition MCP runtime packages`

### Step 2 — Migrate Gateway Engine into bootstrap, common and feature role packages

- Requirements: `REQ-001`, `REQ-002`, `REQ-003`, `REQ-004`, `REQ-005`, `REQ-006`
- Dependencies: `Step 1`
- Baseline state: Engine has 195 production classes, 98 tests, root Configuration/Runtime/Properties, flat shared
  capability packages, HTTP/RPC/WebSocket/Rule/MCP packages and direct external Engine FQCN consumers.
- Observable outcome: Engine production/test sources compile at the target tree, `GatewayEngineApplication` and POM
  mainClass remain unchanged, existing Engine behavior tests pass, and all direct Engine FQCN consumers use the target
  names.
- End state: Engine root contains only the unchanged Application and package-info; common capabilities are curated and
  one-way; feature packages expose service/domain/adapter ownership; no stale old Engine import remains outside
  intentional broad boundary strings.
- Test-first gate:
  `Required — GatewayEnginePackageBoundaryTest must compile first and fail only because current root still contains GatewayEngineConfiguration/GatewayEngineRuntime/GatewayEngineRuntimeProperties and target role directories are absent; after the full atomic move it must be GREEN before the Step commit.`
- Ordered files:

#### File 1 —
`CREATE egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/test/java/top/egon/cola/component/gateway/engine/GatewayEnginePackageBoundaryTest.java`

- Purpose: Define the Engine root/common/feature dependency direction before moving the Engine source tree.
- Symbols: `GatewayEnginePackageBoundaryTest`, `rootContainsOnlyApplicationAndPackageInfo`,
  `commonPackagesDoNotDependOnConcreteFeatures`, `targetFeatureRolesExist`.
- Repository evidence: Existing `GatewayContractBoundaryTest` and `GatewayCoreBoundaryTest` scan Java source text with
  JUnit; Engine POM already includes JUnit 5 and Spring Boot Test.
- Dependencies and consumers: Reads Engine `src/main/java`; it must not load Spring or external services and must work
  from the module reactor test phase.
- Why now: This is the RED contract for REQ-003/REQ-006 and locks the intentional root Application exception.
- Contract/signature changes: No production API; assert root Java files are exactly `GatewayEngineApplication.java` and
  `package-info.java`, required `common`/feature role paths exist, and `engine.common` files do not import
  `engine.http`, `engine.rpc`, `engine.mcp`, `engine.rule`, `engine.operation` or other concrete features.
- Input/output and state mapping: Engine source root -> filenames/package declarations/import lines -> assertion result;
  any root business class or reverse common import produces a path-bearing failure.
- Error and edge behavior: The unchanged Application is allowed; package-info is documentation-only; stale old package
  directories and missing target role directories fail explicitly; no live Bean lookup is performed.
- Implementation pseudocode:

```java
Path root = Path.of("src/main/java/top/egon/cola/component/gateway/engine");
assertOnlyNamesAtRoot(root, Set.of("GatewayEngineApplication.java", "package-info.java"));
for (String target : List.of("bootstrap/config", "bootstrap/lifecycle", "common/provider", "http/service", "rpc/service", "rule/service", "mcp/service"))
    assertTrue(Files.isDirectory(root.resolve(target)), () -> "missing Engine target package " + target);
assertNoImportPrefix(readJava(root.resolve("common")), "import top.egon.cola.component.gateway.engine.http.", "rpc.", "mcp.", "rule.", "operation.");
```

- Verification contribution: The targeted boundary test supplies the expected RED before production movement and GREEN
  after the Engine tree move; it does not claim Spring startup proof.
- After this file: The Engine architecture test compiles and fails only for the known current root/tree state.

#### File 2 —
`RENAME egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/`

- Purpose: Move all Engine production files in the §5.2 manifest into bootstrap/common/feature role packages in one
  compilation-safe operation.
- Symbols: Existing `GatewayEngineConfiguration`, `GatewayEngineRuntime`, `GatewayEngineRuntimeProperties`, Provider,
  Security, Traffic, Transport, Observability, HTTP, WebSocket, RPC, Operation, Rule and Engine MCP symbols.
- Repository evidence: Current 195-file tree and target ownership map are recorded in §5.2; POM `mainClass` points only
  to the unchanged Application.
- Dependencies and consumers: Step 1 MCP FQCNs are already available; all moved Engine classes and external consumers in
  File 4 must be rewritten to the final target names in this same Step.
- Why now: Engine classes are tightly cross-referenced; moving the whole source tree avoids repeated edits and transient
  missing FQCNs between commits.
- Contract/signature changes: Only `git mv`-equivalent path changes, package declarations, imports, package-info prose
  and JavaDoc FQCN references. Preserve annotations, Bean names, conditions, constructor order, method signatures,
  fields, nested records/enums, error mapping, Reactor flows, timeout/retry/cancel state and external calls.
- Input/output and state mapping: Each current class in the manifest maps to one target role from the explicit table;
  `GatewayEngineApplication` stays at its original FQCN; `GatewayEngineConfiguration` is the sole bootstrap assembly
  class and `GatewayEngineRuntimeProperties` is the common config model.
- Error and edge behavior: Preserve fail-closed security, Provider health/balance, Traffic limits, Transport
  commit/retry/timeout, HTTP/RPC/MCP protocol errors, Rule LKG/recovery and observability event semantics; stale imports
  fail the compile gate and must be corrected rather than suppressed.
- Implementation pseudocode:

```text
move root Configuration -> bootstrap.config, Runtime -> bootstrap.lifecycle, RuntimeProperties -> common.config;
classify every ENGINE_MAIN file by the §5.2 manifest into common capability or feature role package;
rewrite all package declarations, imports, package-info descriptions and direct nested-type references in one pass;
keep GatewayEngineApplication and its POM mainClass unchanged, then assert no root business class and no common-to-feature import remain.
```

- Verification contribution: Engine compilation proves all 195 production symbols resolve across the new tree; the
  boundary test proves root/common constraints; configuration tests prove Bean/method semantics are not changed by path
  relocation.
- After this file: Engine production files are at final target paths; only test package/import and repository consumer
  edits from Files 3–4 may remain before the Step is GREEN.

#### File 3 —
`RENAME egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/test/java/top/egon/cola/component/gateway/engine/`

- Purpose: Move all 98 Engine tests and fixtures to mirror the final production ownership and update their imports to
  final target FQCNs.
- Symbols: Existing root, Provider, Security, Traffic, Transport, Observability, HTTP, WebSocket, RPC, Operation, Rule
  and MCP test classes listed in §5.2.
- Repository evidence: Current test tree is enumerated by `rg --files` and grouped in §5.2; tests use JUnit 5, Reactor
  Test, Spring Boot Test and existing fixtures.
- Dependencies and consumers: Tests consume Engine production types and Step 1 MCP Core targets; no test should add a
  new service, fixture, external dependency or weaker assertion.
- Why now: All test package declarations/imports must align with final production packages before the Engine reactor can
  become GREEN.
- Contract/signature changes: Change only test path/package/import/Javadoc references; keep test method names, test
  inputs, expected outputs, exception codes, Reactor schedules, mocks, fixture lifecycle and IT annotations unchanged.
- Input/output and state mapping: Existing test scenario -> same class under target role -> same observable result;
  `transport/fixture` remains under `common.transport.fixture`, HTTP buffer/logging/proxy tests mirror their new local
  subdomains.
- Error and edge behavior: Preserve credential forwarding, retry/no-retry-after-commit, timeout, WebSocket close/frame,
  RPC fullMethodName/marshaller, Rule compatibility, MCP session/task/telemetry and security negative paths.
- Implementation pseudocode:

```java
for each file in ENGINE_TEST manifest:
    move it to the package owning the primary production symbol;
    replace old engine and mcp-core imports with final target imports;
    keep assertions, fixtures, mocks, timeouts, expected failures and test profile annotations unchanged;
    compile the complete Engine test source set before selecting any narrower test subset.
```

- Verification contribution: The existing 98-test Engine suite remains the behavioral and configuration regression gate;
  package boundary test adds architecture evidence without replacing behavior tests.
- After this file: Every Engine test/fixture has a final package and no test assertion was removed or broadened.

#### File 4 —
`MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/annotation/LoadBalance.java`

- Purpose: Update all direct Engine FQCN consumers outside the Engine module and preserve the unchanged root Application
  reference where applicable.
- Symbols: `LoadBalance` JavaDoc `LoadBalancerType`; `GatewayComposeConfigurationTest`;
  `GatewayRuleWireCompatibilityTest`; `McpHaRecoveryIT`; `McpSecurityIT`; related existing Engine package imports
  discovered by the exact scan below.
- Repository evidence: `rg -l -F 'top.egon.cola.component.gateway.engine.' --glob '*.java'` identifies module-internal
  and external references; current live topology code uses the root Application string, which remains unchanged.
- Dependencies and consumers: RPC starter has a documentation-only type reference; Gateway test-suite consumes Engine
  properties, Rule codec, Provider Directory, Engine MCP runtime/remote classes; these files do not own the moved
  runtime types.
- Why now: A source-only package migration is incomplete until all repository-visible imports and JavaDoc references
  resolve.
- Contract/signature changes: Replace only FQCNs: `LoadBalancerType` ->
  `engine.common.provider.domain.LoadBalancerType`; `GatewayEngineRuntimeProperties` -> `engine.common.config`; Rule
  codec -> `engine.rule.adapter.json`; Provider/Engine MCP references -> their §5.2 targets. Leave
  `GatewayEngineApplication` root string unchanged.
- Input/output and state mapping: Existing test/deployment configuration -> same target class -> same test assertion and
  process main class; no test topology, port, environment variable or deployment behavior changes.
- Error and edge behavior: Do not mask a stale import with a wrapper; preserve live-test skip/profile behavior and all
  JavaDoc semantics except the FQCN spelling.
- Implementation pseudocode:

```text
collect exact Java consumers with rg and exclude only the two intentionally broad boundary-test prefix strings;
apply the final target FQCN map to imports, qualified references and JavaDoc code references;
verify GatewayEngineApplication remains top.egon.cola.component.gateway.engine.GatewayEngineApplication;
fail the Step if any old engine subpackage FQCN remains in Java source outside the explicit allowlist.
```

- Verification contribution: Full Gateway reactor compilation and static scan prove the moved Engine classes remain
  consumable; live topology code remains a user-controlled runtime gate.
- After this file: Repository-wide Engine FQCN consumers are updated and no compatibility facade or new dependency has
  been introduced.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command:
  `./mvnw -B -ntp -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine -am test`
- Expected result: Engine compiles; all existing 98 Engine tests plus `GatewayEnginePackageBoundaryTest` pass; POM
  mainClass remains `top.egon.cola.component.gateway.engine.GatewayEngineApplication`; common reverse-import and root
  package assertions are GREEN.
- Failure returns to: File 2 for production target/import/package-info errors; File 3 for test/fixture errors; File 4
  for external consumer errors; `GatewayEngineApplication`/POM boundary if scan or configuration tests show a changed
  entry point.
- Completion criteria: All Engine manifest files are at target packages; root/common boundary test passes; Engine
  behavior/configuration tests pass; direct Engine FQCN scan is clean; no POM/module/contract/database/frontend change
  appears in the Step diff.
- Rollback: Revert only the Step 2 commit with `git revert COMMIT` after checking the exact Step 2 path list; preserve
  Step 1 unless the user explicitly requests a combined rollback; never reset or checkout the worktree globally.
- Commit paths:
  `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/test/java/top/egon/cola/component/gateway/engine/GatewayEnginePackageBoundaryTest.java`,
  `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/`,
  `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/test/java/top/egon/cola/component/gateway/engine/`,
  `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/annotation/LoadBalance.java`,
  `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/`
- Commit: `refactor(gateway-engine): organize runtime packages by feature`

## 8. Test, Validation, and Quality Gates

| Gate/order                 | Working directory                    | Command or method                                                                                                                                                                     | Scope                                       | Expected result                                                                                     | Failure returns to           | Requirements/runtime boundary                                                                       |
|----------------------------|--------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------|-----------------------------------------------------------------------------------------------------|------------------------------|-----------------------------------------------------------------------------------------------------|
| Step 1 RED                 | `/Users/mario/SelfProject/Egon-COLA` | `./mvnw -B -ntp -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-core -am -Dtest=McpCorePackageBoundaryTest test` before File 2                      | New MCP boundary test                       | Fails because current flat tree lacks target common/role directories; no fixture/environment error  | Step 1 File 1                | REQ-003/006; static/module                                                                          |
| Step 1 GREEN               | `/Users/mario/SelfProject/Egon-COLA` | `./mvnw -B -ntp -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-core -am test`                                                                      | MCP Core and dependencies                   | All existing 10 tests plus boundary test pass                                                       | Step 1 File 2/3/4            | REQ-001–006; module                                                                                 |
| Step 2 RED                 | `/Users/mario/SelfProject/Egon-COLA` | `./mvnw -B -ntp -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine -am -Dtest=GatewayEnginePackageBoundaryTest test` before File 2                  | New Engine boundary test                    | Fails because root contains Configuration/Runtime/Properties and target role directories are absent | Step 2 File 1                | REQ-003/006; static/module                                                                          |
| Step 2 GREEN               | `/Users/mario/SelfProject/Egon-COLA` | `./mvnw -B -ntp -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine -am test`                                                                        | Engine, MCP Core and dependencies           | All existing 98 Engine tests plus boundary test pass; mainClass unchanged                           | Step 2 File 2/3/4            | REQ-001–006; module                                                                                 |
| Static old MCP FQCN        | `/Users/mario/SelfProject/Egon-COLA` | `rg -n -F 'top.egon.cola.component.gateway.mcp.protocol.' --glob '*.java'` and equivalent old MCP capability prefixes                                                                 | Repository Java sources                     | No stale moved MCP FQCN except explicitly reviewed boundary strings                                 | Step 1 File 4                | REQ-004/006; static                                                                                 |
| Static old Engine FQCN     | `/Users/mario/SelfProject/Egon-COLA` | `rg -n 'top\.egon\.cola\.component\.gateway\.engine\.(balance                                                                                                                         | discovery                                   | security                                                                                            | traffic                      | transport                                                                                           |observability|http|websocket|rpc|operation|rule|mcp)(\.|"|\x27)' --glob '*.java'` | Repository Java sources | No old Engine subpackage FQCN; root Application reference is retained | Step 2 File 4 | REQ-004/006; static |
| Common reverse dependency  | `/Users/mario/SelfProject/Egon-COLA` | `rg -n 'import top\.egon\.cola\.component\.gateway\.engine\.common\.[^;]+engine\.(http                                                                                                | rpc                                         | mcp                                                                                                 | rule                         | operation)\.' egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src` | Engine common Java sources | No common-to-concrete-feature import | Step 2 File 2 | REQ-003/006; static |
| Root package scan          | `/Users/mario/SelfProject/Egon-COLA` | `./mvnw -B -ntp -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine -am -Dtest=GatewayEnginePackageBoundaryTest test` and inspect the root assertion | Engine root                                 | Only `GatewayEngineApplication.java` and `package-info.java` at root                                | Step 2 File 1/2              | REQ-001/006; static                                                                                 |
| Diff hygiene               | `/Users/mario/SelfProject/Egon-COLA` | `git diff --check` and `git diff --name-only` against the Step path list                                                                                                              | Working tree                                | No whitespace errors; no unrelated paths; staged contract deletion remains untouched                | Owning Step                  | REQ-005; static                                                                                     |
| Gateway reactor regression | `/Users/mario/SelfProject/Egon-COLA` | `./mvnw -B -ntp -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test -am test`                                                                          | Gateway module and test suite               | Compile and non-live tests pass; no claim about external topology                                   | Step 2 File 4 or owning test | REQ-004/006; cross-module                                                                           |
| User-controlled runtime    | User-selected Gateway deployment CWD | User starts Engine/live profile and checks Spring Bean discovery, HTTP/RPC/MCP/Provider/Rule paths                                                                                    | Live DDC/Redis/Provider/remote MCP topology | User observes startup and protocol behavior; not executed by this Plan                              | User/runtime follow-up       | REQ-004; runtime, explicitly not proven here                                                        |

The `-Dtest` RED commands are planning gates. They must be executed before the corresponding production moves during
implementation; the final reports must record the expected RED reason and subsequent GREEN exit code rather than
treating a lost Maven process as success.

## 9. Migration, Compatibility, Rollout, and Rollback

Database/schema migration: `N/A` — no `classpath:db` file is changed, created or regenerated. Historical Flyway files
remain immutable.

Maven/module migration: `N/A` — the two existing Maven modules remain unchanged and no dependency/module boundary is
added.

Source compatibility: This is an internal source FQCN migration. Existing methods, fields, annotations, Bean names,
contracts, payloads, error codes, configuration keys, retry/cancel/LKG/task semantics and test assertions remain
unchanged. DEC-005 accepted direct migration, so no old-package wrapper is created. Any external binary consumer
discovered during implementation blocks the affected Step and requires a new compatibility Spec.

Rollout: No service rollout, data backfill, feature flag, dual-read/write or deployment ordering is required for the
source-only change. After both commits, the user may run the existing Gateway deployment/live profile manually.

Rollback: Revert Step 2 independently to restore Engine paths, or Step 1 independently to restore MCP Core paths. Do not
use `git reset --hard`, `git checkout --`, broad deletion, or worktree-wide restore; preserve the unrelated staged
contract deletion. If a consumer outside the repository is discovered after merge, use a forward compatibility design
rather than silently restoring only selected packages.

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Effective Spec section       | Steps     | Files                                                            | Tests/gates                                           | Completion evidence                                   |
|-------------|------------------------------|-----------|------------------------------------------------------------------|-------------------------------------------------------|-------------------------------------------------------|
| `REQ-001`   | Spec §3.1, §4, §7.3.2–§7.3.3 | Steps 1–2 | MCP_MAIN/ENGINE_MAIN manifests; package-info and tests           | MCP/Engine boundary tests; target tree review         | first package layer identifies capability/feature     |
| `REQ-002`   | Spec §3.1, §7.3.1, §8.3      | Steps 1–2 | domain/service/adapter target paths in both manifests            | module compile and package declaration scan           | role is visible without reading class body            |
| `REQ-003`   | Spec §3.1, §7.1, §7.3.4, §13 | Steps 1–2 | `mcp.common/*`, `engine.common/*`, boundary tests                | common reverse-import scans                           | no common-to-concrete-feature dependency              |
| `REQ-004`   | Spec §3.1, §7.3.5, §9, §16   | Steps 1–2 | bootstrap files, config/runtime tests, direct consumers          | module tests, Gateway reactor, mainClass/static check | existing Bean/config/protocol assertions remain GREEN |
| `REQ-005`   | Spec §3.2, §7.0, §13.2, §17  | Steps 1–2 | only listed package/import/package-info/test files               | `git diff --name-only`, no POM/schema scan            | no new module, API, model, facade or behavior change  |
| `REQ-006`   | Spec §3.1, §8, §14           | Steps 1–2 | `McpCorePackageBoundaryTest`, `GatewayEnginePackageBoundaryTest` | RED/GREEN boundary gates and final static scan        | future root/common regressions fail automatically     |

## 11. Risks, Blockers, and User Decisions

| ID         | Risk or decision                                                   | Impacted Steps/files            | Evidence                                                                                                          | Owner            | Status/action                                                                         |
|------------|--------------------------------------------------------------------|---------------------------------|-------------------------------------------------------------------------------------------------------------------|------------------|---------------------------------------------------------------------------------------|
| `RISK-001` | A binary consumer outside this repository may use an old FQCN      | Steps 1–2 direct consumers      | Accepted DEC-005 selected direct migration; current repository `rg` scan found only listed source/docs references | User/implementer | Mitigated for source repository; stop and return to Spec if a binary consumer appears |
| `RISK-002` | Moving configuration/runtime classes could change Spring discovery | Step 2 bootstrap files          | Application root package and POM mainClass are unchanged; configuration test exists                               | Implementer      | Validation required; module tests and user startup are separate evidence              |
| `RISK-003` | `common` could become a new miscellaneous bucket                   | Steps 1–2 common manifests      | Spec requires named capability common packages and boundary tests                                                 | Implementer      | Guarded by per-capability mapping and reverse-import test                             |
| `RISK-004` | Two `HttpMcpRequest` records could be accidentally merged          | Step 1 protocol/transport files | Current records have different packages/fields and separate consumers                                             | Implementer      | Guarded by explicit PLAN-CLAR-003 and compile/test gate                               |
| `RISK-005` | Unrelated staged contract deletion could enter a commit            | Both commits                    | Current `git status --short` shows the deletion before Plan work                                                  | Implementer      | Guarded by exact commit paths and pre/post status check                               |
| `DEC-005`  | Direct migration versus old-package compatibility facade           | Steps 1–2                       | User “确认” on 2026-08-19; Spec status Accepted                                                                     | User             | Closed — direct migration selected; no wrapper                                        |

## 12. Review and Acceptance

### 12.1 Original requirement fidelity

The Plan covers both named modules, uses feature-first plus local service/domain/adapter and curated common packages,
explicitly separates current model/service/adapter responsibilities, updates direct FQCN consumers, preserves the root
Engine application entry and does not expand into behavior, module, schema, frontend or runtime changes.

### 12.2 Spec consistency

The Plan implements the Accepted target tree and all six requirements without adding a new public API, field, model,
mapper, cache, persistence object, dependency, Maven module, protocol behavior or compatibility wrapper. The only
clarifications are evidence-backed package-info collision handling, direct consumer completeness, distinct MCP request
records and the Spec-defined Operation adapter placement for HTTP-to-RPC bridge classes.

The Spec simplicity audit found no fetch-then-forward interface, caller-supplied value derivation issue, speculative
pattern or duplicate model that would require returning to the Spec.

### 12.3 Repository executability

The Plan records the current branch/commit and dirty-worktree exception, exact module/source roots, target package
ownership, direct consumer paths, test-first RED/GREEN order, Maven working directory/commands, static scans,
path-limited commit scopes and rollback boundaries. The two module-level commits are intentionally atomic because their
imports are cross-linked; their internal manifest remains file/class specific.

### 12.4 Test and release completeness

RED/GREEN boundary tests precede each production move; existing MCP and Engine tests remain regression gates; Gateway
reactor and static scans run after both moves; no migration/rollout is claimed; live topology is explicitly deferred to
user-controlled runtime verification; rollback preserves unrelated worktree state.

### 12.5 Final verdict

PASS — Ready for user review

This document is an implementation Plan only. No production code, test code, migration, service start or runtime
verification was performed while writing it.
