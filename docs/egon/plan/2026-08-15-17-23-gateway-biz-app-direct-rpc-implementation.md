# Gateway BIZ/APP 范围鉴权与 DDC 直连 RPC 实施计划

| Field              | Value                                                                                                                                                                                                                                                |
|--------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Document           | `2026-08-15-17-23-gateway-biz-app-direct-rpc-implementation.md`                                                                                                                                                                                      |
| Status             | `Review`                                                                                                                                                                                                                                             |
| Created            | `2026-08-15 17:23 CST`                                                                                                                                                                                                                               |
| Updated            | `2026-08-15 17:23 CST`                                                                                                                                                                                                                               |
| Owner              | `Egon-COLA platform owner / User`                                                                                                                                                                                                                    |
| Repository         | `/Users/mario/SelfProject/Egon-COLA`                                                                                                                                                                                                                 |
| Scope              | `Gateway HTTP/RPC data plane, RBAC3 scope projection/adapter, RPC Starter/DDC direct discovery, IdP RPC credential relay`                                                                                                                            |
| Source Requirement | `2026-08-15 user request and confirmation: Gateway checks BIZ then APP only; downstream owns operation permission; @EgonRpcDirectReference is accepted; APP scope uses effective active roles; historical Gateway RPC Routes are explicitly retired` |
| Baseline Revision  | `main@8a64b586634d8d1fc94ffcc101e9add00c2e7730; only docs/egon is untracked at Plan creation`                                                                                                                                                        |
| Implements Spec    | [Gateway 流量层 BIZ/APP 范围校验与 DDC 直连 RPC 双链路规格](../spec/2026-08-15-16-57-gateway-biz-app-scope-direct-rpc-design.md)                                                                                                                                    |
| Spec Status        | `Review`                                                                                                                                                                                                                                             |
| Spec Revision      | `Updated 2026-08-15 16:57 CST; baseline main@8a64b586634d8d1fc94ffcc101e9add00c2e7730`                                                                                                                                                               |
| Effective Specs    | [Gateway 流量层 BIZ/APP 范围校验与 DDC 直连 RPC 双链路规格](../spec/2026-08-15-16-57-gateway-biz-app-scope-direct-rpc-design.md)                                                                                                                                    |
| Depends On Plans   | `None`                                                                                                                                                                                                                                               |
| Supersedes         | `None`                                                                                                                                                                                                                                               |
| Superseded By      | `None`                                                                                                                                                                                                                                               |
| Related Plans      | `None`                                                                                                                                                                                                                                               |

## 1. Summary

本计划实施唯一目标 Spec，把工作拆成 9 个按依赖顺序、可单独验证和提交的 Step：先建立 RBAC3 BIZ→APP 轻量投影和一致发布，再替换
Gateway RBAC3 数据面 Provider并接入 Engine；随后在中立 RPC Starter 中增加 Provider Directory/双注解，在 DDC Adapter 中实现
`RPC_PROVIDER` 发现；最后补齐 IdP RPC 凭证中继、Gateway→RPC 转发和文档/实例注册回归边界。

最终证据包括：每个 Step 的 RED/GREEN 聚焦测试、RPC Components 与 Platforms 受影响模块测试、Gateway Admin 未修改检查、
`git diff --check`、Plan/Spec 追踪检查，以及由用户启动本地栈后执行的 Gateway/Direct 双链路运行验收。本计划不修改生产代码、不运行数据库迁移、不启动服务。

## 2. Target Spec and Effective Design

### 2.1 Primary target

-
Path：[Gateway 流量层 BIZ/APP 范围校验与 DDC 直连 RPC 双链路规格](../spec/2026-08-15-16-57-gateway-biz-app-scope-direct-rpc-design.md)
- Status：文档元数据为 `Review`；用户已在本轮明确接受 `@EgonRpcDirectReference`、有效/已激活角色 APP 范围和历史 Route
  显式下线，并要求开始写 Plan。
- Revision：`Updated 2026-08-15 16:57 CST`，源码基线 `main@8a64b586634d8d1fc94ffcc101e9add00c2e7730`。
- Approval evidence：用户回复“1 可以 2 是 3 确认，开始写 plan 吧。”；该证据授权基于本 Spec 规划，但本 Plan 仍等待用户审核，因此状态为
  `Review`。

### 2.2 Effective Spec set

| Role    | Spec/link                                                                                                | Status/revision                                       | Effective sections | Why included                     |
|---------|----------------------------------------------------------------------------------------------------------|-------------------------------------------------------|--------------------|----------------------------------|
| Primary | [Gateway BIZ/APP + Direct RPC Spec](../spec/2026-08-15-16-57-gateway-biz-app-scope-direct-rpc-design.md) | `Review`, accepted for planning; 2026-08-15 16:57 CST | §1–§20 全部          | 唯一直接约束本次行为、接口、模型、测试、兼容和发布顺序的设计基线 |

### 2.3 Superseded or excluded content

- Primary Spec 已明确修订其 `Amends` 中旧设计的接口级 Gateway Permission 和 RPC 必经 Gateway 表述；本 Plan 只采用 Primary
  Spec §20.4 所列仍有效边界。
- Primary Spec `Related Specs` 中 RBAC3 IAM 文档仍为 Draft，仅作术语背景，不是 Effective Spec。
- Gateway Admin UI、MCP Tool 权限、RPC Streaming、新数据库模型、新 Client Governance 均按 Spec §3.2 排除。

## 3. Effective Requirements and Acceptance

| Requirement | Source Spec section                                                                                              | Effective statement                                    | Observable acceptance                                                | Implementation impact                             |
|-------------|------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------|----------------------------------------------------------------------|---------------------------------------------------|
| `REQ-001`   | [§4](../spec/2026-08-15-16-57-gateway-biz-app-scope-direct-rpc-design.md#4-requirements-and-acceptance-criteria) | `BUSINESS_PROTECTED` USER 先校验 Route BIZ，再校验该 BIZ 下 APP | BIZ 拒绝时 APP lookup 为 0；BIZ/APP 都允许才转发                                | RBAC3 contract/admin/gateway adapter、Engine tests |
| `REQ-002`   | §4                                                                                                               | Gateway HTTP/RPC 不判断 Operation Permission              | 数据面不读取 mapping metadata/key 或 `permissions`                          | 删除旧 reader/provider；HTTP attrs 收敛                 |
| `REQ-003`   | §4                                                                                                               | 保留认证、暴露规则、范围 Fail Closed                               | 401/403/503 与 `externalAccessible` 语义保持                              | 复用 `GatewaySecurityChain`，替换 Provider             |
| `REQ-004`   | §4                                                                                                               | Gateway Admin 本地接口权限保持                                 | Admin 路径零修改，既有 Admin 模块测试通过                                          | 无 Admin 生产文件；最终 diff gate                         |
| `REQ-005`   | §4                                                                                                               | `@EgonRpcReference` 继续走 `INTERNAL_GATEWAY`             | 旧源码兼容；Gateway 缺失仍报 `RPC_GATEWAY_UNAVAILABLE`                         | RPC Starter Gateway demand/wiring                 |
| `REQ-006`   | §4, §9                                                                                                           | 新增 `@EgonRpcDirectReference` 精确发现 `RPC_PROVIDER`       | DDC key 含 BIZ/APP/env/service/group/version/grpc；Channel 不连接 Gateway | RPC Provider Directory + DDC adapter              |
| `REQ-007`   | §4, §9                                                                                                           | 同应用/同契约可双字段双路径；同字段双注解失败                                | 两类 Proxy 同时注入；冲突错误含 bean/field                                       | BPP、双 Proxy Factory、context tests                 |
| `REQ-008`   | §4, §7.5                                                                                                         | 两种路径不自动 fallback                                       | Gateway/Provider 不可用错误彼此独立                                           | 两套 Manager/Channel Strategy                       |
| `REQ-009`   | §4                                                                                                               | 所有导出 RPC Provider 注册 DDC，不受 Gateway 文档影响               | 无 Gateway Group 时 `RPC_PROVIDER` lease 仍注册                           | DDC registry regression                           |
| `REQ-010`   | §4                                                                                                               | Direct-only RPC 可不配置 Gateway 文档                        | 无 `@GatewayInterfaceGroup` 就无报告/Route                                | Gateway Starter contract test                     |
| `REQ-011`   | §4                                                                                                               | Consumer 不主动判断接口权限                                     | RPC Starter/Adapter 无 RBAC3 permission 依赖/读取                         | 仅发现、调用、Trace、凭证中继                                 |
| `REQ-012`   | §4, §9                                                                                                           | Provider 可取得并验证受控中继 USER AT                            | 有效 token 建身份；非法/重复 token 为 `UNAUTHENTICATED`；无 token 匿名              | IdP RPC interceptors + Gateway forwarders         |
| `REQ-013`   | §4, §10                                                                                                          | Scope 与全量快照同授权版本/Fence                                 | tenant/sub/user/version/expiry 一致；不完整发布不可见                           | 双投影、Redis key/order、reader                        |
| `REQ-014`   | §4, §16                                                                                                          | Direct-only 切换显式撤销历史 Gateway Route                     | Active Release 不再含旧 Operation；改注解不能代替下线                              | 发布清单与人工/运行 Gate                                   |

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

1. 先发布可独立序列化的 RBAC3 scope contract，再让 Admin 一次投影并在 Pointer 可见前双写 Redis；这为读侧提供向后兼容的生产事实。
2. 在 Gateway Adapter 中用新 reader/provider替换旧 Permission reader/provider；所有失败仍进入现有 `GatewaySecurityChain`
   的 Fail Closed 映射。
3. Engine 增加 Adapter runtime依赖、配置和新 Provider ID；先具备 Capability，再迁移 Policy，避免规则引用不存在的 Provider。
4. RPC Starter 先落中立 `RpcProviderDirectory`/Manager/Channel Strategy，再落双注解和 Spring wiring，保持 Starter 不依赖
   DDC。
5. DDC Adapter 实现精确 `RPC_PROVIDER` 查询并装配端口；Provider 注册写链不改变。
6. IdP Starter 提供 RPC client/server credential adapter；Consumer 只中继 `VerifiedUserTokenCarrier` 中的可信
   token，Provider 验证并在 callback scope 建立 Spring Security Context。
7. Gateway RPC 与 HTTP→RPC 恢复 Security Chain 已验证的 `forwardingCredential`，不复制未经验证的原始 Header。
8. 固化 Gateway Definition opt-in 和 DDC lease 解耦，并把历史 Route 下线列为发布 Gate。
9. 运行跨模块回归、静态依赖/秘密扫描和用户控制的本地栈验收；不在实现过程中自动启动服务。

### 4.2 Test-first strategy

| Behavior                | RED test and expected RED reason                                    | Minimum GREEN                                                 | Refactor/wiring                              |
|-------------------------|---------------------------------------------------------------------|---------------------------------------------------------------|----------------------------------------------|
| 双快照投影/发布                | Contract/projector/repository tests 因新类型、字段、key 不存在或 scope 未写失败     | records + effective scope + projection + ordered dual publish | 保持 full snapshot JSON 不变，复用 Fence            |
| Gateway BIZ→APP         | 新 reader/Fail Closed tests 因 scope reader/provider 不存在失败            | scope reader按层级判断并返回稳定 reason                                 | 删除 Permission reader/provider，更新 auto-config |
| Engine scope capability | classpath/HTTP attrs tests 因 Adapter 缺失、旧 attrs仍存在失败                | POM/config/attrs/script切换                                     | 无第二套 security chain                          |
| Provider Directory      | Manager tests 因 Directory records/manager 不存在失败                     | snapshot reconcile + round robin + drain                      | `RpcEndpoint` 仅复用建链逻辑                        |
| 双注解                     | BPP/context tests 因注解/factory不存在、direct-only仍要求 Gateway失败           | annotation + two factories + mode-specific demand             | 保持 `@EgonRpcReference` 源兼容                   |
| DDC Direct discovery    | directory/autoconfig tests 因 `RpcProviderDirectory` Bean/key映射不存在失败 | `DdcRpcProviderDirectory` + Bean                              | 不修改 Provider Registry key                    |
| RPC credential relay    | IdP interceptor tests 因 Authorization key/context adapter不存在失败      | trusted carrier + client/server interceptors                  | token 不写 Authentication credentials/log      |
| Gateway→RPC relay       | Gateway tests 因 Outcome不携带 credential或 outbound metadata缺失失败        | Outcome + `authorization` metadata                            | HTTP→HTTP现状不变                                |
| 文档/实例解耦                 | Contributor/Registry tests 因缺少明确无 Group组合断言失败                       | 测试锁定现有行为                                                      | 不让 Consumer 注解写 Admin                        |

### 4.3 Sequential and parallel boundaries

| Step   | Depends on    | May run in parallel with | Must not overlap with                          | Reason                               |
|--------|---------------|--------------------------|------------------------------------------------|--------------------------------------|
| Step 1 | None          | Step 4                   | RBAC3 contract/admin files                     | 先建立写侧事实；与 RPC Core 无写冲突              |
| Step 2 | Step 1        | Step 4                   | RBAC3 gateway adapter files                    | reader依赖新 contract/key               |
| Step 3 | Step 2        | Step 5                   | Gateway Engine POM/config/HTTP security/script | Engine必须先能加载新 Provider               |
| Step 4 | None          | Step 1                   | RPC Starter provider/channel packages          | 中立 SPI 不依赖 DDC                       |
| Step 5 | Step 4        | Step 3                   | RPC Starter annotation/proxy/config files      | 双注解依赖 Provider Manager               |
| Step 6 | Step 4        | Step 3                   | RPC DDC Adapter files                          | Adapter只依赖已发布中立 SPI                  |
| Step 7 | Step 5        | None                     | IdP Starter + shared RPC metadata key          | AutoConfig要把 interceptor注入 RPC proxy |
| Step 8 | Steps 2, 3, 7 | None                     | Gateway RPC/HTTP forwarding files              | Gateway使用新 credential relay contract |
| Step 9 | Steps 1–8     | None                     | Gateway Starter/DDC registry tests only        | 最终锁定独立事实源与发布边界                       |

### 4.4 Commit boundaries

每个 Step 对应一个语义 commit，并使用 `git commit --only <该 Step 文件>` 保护其他工作。Step 1/2 和 Step 4/5
分开提交是为了分别保持“发布端→读取端”与“中立发现→注解装配”的可编译依赖。Step 9 是测试/发布边界 commit，不夹带生产重构。

## 5. Change File Tree

```text
egon-cola-components/egon-cola-component-rpc/
├── egon-cola-component-rpc-starter/
│   ├── src/main/java/top/egon/cola/component/rpc/
│   │   ├── annotation/EgonRpcDirectReference.java                                      CREATE [S5]
│   │   ├── config/EgonRpcAutoConfig.java                                               MODIFY [S5]
│   │   ├── config/EgonRpcProperties.java                                               MODIFY [S5]
│   │   ├── consumer/channel/RpcEndpoint.java                                           CREATE [S4]
│   │   ├── consumer/channel/RpcConsumerChannelFactory.java                             MODIFY [S4]
│   │   ├── consumer/gateway/RpcConsumerGatewayManager.java                             MODIFY [S5]
│   │   ├── consumer/gateway/RpcGatewayEndpoint.java                                    MODIFY [S4]
│   │   ├── consumer/provider/ProviderRpcInvocationChannelProvider.java                 CREATE [S4]
│   │   ├── consumer/provider/RpcConsumerProviderManager.java                           CREATE [S4]
│   │   ├── consumer/provider/RpcProviderDirectory.java                                 CREATE [S4]
│   │   ├── consumer/provider/RpcProviderEndpoint.java                                  CREATE [S4]
│   │   ├── consumer/provider/RpcProviderQuery.java                                     CREATE [S4]
│   │   ├── consumer/provider/RpcProviderSnapshot.java                                  CREATE [S4]
│   │   ├── consumer/provider/RpcProviderSubscription.java                              CREATE [S4]
│   │   ├── consumer/proxy/EgonRpcReferenceBeanPostProcessor.java                      MODIFY [S5]
│   │   ├── consumer/proxy/RpcDirectReferenceProxyFactory.java                          CREATE [S5]
│   │   └── context/invocation/RpcMetadataKeys.java                                     MODIFY [S7]
│   └── src/test/java/top/egon/cola/component/rpc/
│       ├── config/EgonRpcAutoConfigTest.java                                           CREATE [S5]
│       ├── config/EgonRpcPropertiesTest.java                                           MODIFY [S5]
│       ├── consumer/gateway/RpcConsumerGatewayManagerTest.java                         MODIFY [S5]
│       ├── consumer/provider/RpcConsumerProviderManagerTest.java                       CREATE [S4]
│       └── consumer/proxy/EgonRpcReferenceBeanPostProcessorTest.java                   CREATE [S5]
└── egon-cola-component-rpc-ddc-adapter/
    ├── src/main/java/top/egon/cola/component/rpc/ddc/
    │   ├── autoconfigure/DdcRpcAutoConfiguration.java                                  MODIFY [S6]
    │   └── registry/DdcRpcProviderDirectory.java                                       CREATE [S6]
    └── src/test/java/top/egon/cola/component/rpc/ddc/
        ├── autoconfigure/DdcRpcAutoConfigurationTest.java                              MODIFY [S6]
        └── registry/
            ├── DdcRpcProviderDirectoryTest.java                                        CREATE [S6]
            └── DdcRpcProviderRegistryTest.java                                         MODIFY [S9]

egon-cola-platforms/egon-cola-platform-rbac3/
├── egon-cola-platform-rbac3-contract/
│   ├── src/main/java/top/egon/cola/platform/rbac3/contract/authorization/
│   │   ├── ApplicationAccessScope.java                                                 CREATE [S1]
│   │   ├── BusinessAccessScope.java                                                    CREATE [S1]
│   │   └── GatewayBizAppScopeSnapshot.java                                             CREATE [S1]
│   └── src/test/java/top/egon/cola/platform/rbac3/contract/ContractSerializationTest.java MODIFY [S1]
├── egon-cola-platform-rbac3-core/src/main/java/top/egon/cola/platform/rbac3/core/runtime/
│   └── Rbac3RuntimeKeyFactory.java                                                     MODIFY [S1]
├── egon-cola-platform-rbac3-admin/
│   ├── src/main/java/top/egon/cola/platform/rbac3/admin/
│   │   ├── iam/role/service/EffectiveApplicationScope.java                             CREATE [S1]
│   │   ├── iam/role/service/RoleEligibilityService.java                               MODIFY [S1]
│   │   └── runtime/
│   │       ├── domain/vo/UserSnapshotProjectionVO.java                                 MODIFY [S1]
│   │       ├── repository/redis/RedisAuthorizationRuntimeRepository.java               MODIFY [S1]
│   │       └── service/UserAuthorizationSnapshotProjector.java                         MODIFY [S1]
│   └── src/test/java/top/egon/cola/platform/rbac3/admin/
│       ├── iam/role/service/RoleEligibilityServiceTest.java                            MODIFY [S1]
│       └── runtime/
│           ├── RedisAuthorizationRuntimeRepositoryTest.java                            CREATE [S1]
│           └── UserAuthorizationSnapshotProjectorTest.java                             MODIFY [S1]
└── egon-cola-platform-rbac3-gateway-adapter/
    ├── src/main/java/top/egon/cola/platform/rbac3/gateway/
    │   ├── autoconfigure/Rbac3GatewayAdapterAutoConfiguration.java                     MODIFY [S2]
    │   ├── runtime/Rbac3GatewayRuntimeSnapshotReader.java                              DELETE [S2]
    │   ├── runtime/Rbac3GatewayScopeSnapshotReader.java                                CREATE [S2]
    │   ├── security/Rbac3PermissionAuthorizationProvider.java                          DELETE [S2]
    │   └── security/Rbac3BizAppScopeAuthorizationProvider.java                         CREATE [S2]
    └── src/test/java/top/egon/cola/platform/rbac3/gateway/
        ├── autoconfigure/Rbac3GatewayAdapterAutoConfigurationTest.java                 MODIFY [S2]
        ├── performance/GatewayHotPathBudgetTest.java                                   MODIFY [S2]
        ├── runtime/Rbac3GatewayRuntimeSnapshotReaderTest.java                          DELETE [S2]
        ├── runtime/Rbac3GatewayScopeSnapshotReaderTest.java                            CREATE [S2]
        └── security/GatewayFailClosedSecurityMatrixTest.java                           MODIFY [S2]

egon-cola-platforms/egon-cola-platform-gateway/
├── egon-cola-platform-gateway-engine/
│   ├── pom.xml                                                                         MODIFY [S3]
│   ├── src/main/resources/application.yml                                              MODIFY [S3]
│   ├── src/main/java/top/egon/cola/component/gateway/engine/
│   │   ├── http/RuleBackedHttpGatewaySecurityProcessor.java                            MODIFY [S3]
│   │   ├── http/DefaultGatewayHttpDataPlaneHandler.java                                MODIFY [S8]
│   │   └── rpc/
│   │       ├── GatewayRpcSecurityProcessor.java                                        MODIFY [S8]
│   │       ├── RpcGatewayForwarder.java                                                MODIFY [S8]
│   │       └── RuleBackedRpcGatewaySecurityProcessor.java                              MODIFY [S8]
│   └── src/test/java/top/egon/cola/component/gateway/engine/
│       ├── IdpAdapterRuntimeClasspathTest.java                                         MODIFY [S3]
│       ├── http/
│       │   ├── RuleBackedHttpGatewaySecurityProcessorTest.java                         MODIFY [S3]
│       │   └── DefaultGatewayHttpDataPlaneHandlerCredentialForwardingTest.java         MODIFY [S8]
│       └── rpc/
│           ├── HttpRpcUpstreamAdapterTest.java                                         MODIFY [S8]
│           ├── RpcGatewayCredentialForwardingTest.java                                 CREATE [S8]
│           └── RuleBackedRpcGatewaySecurityProcessorTest.java                          MODIFY [S8]
└── egon-cola-platform-gateway-starter/src/test/java/top/egon/cola/component/gateway/starter/discovery/
    └── RpcGatewayDefinitionContributorTest.java                                        MODIFY [S9]

egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-starter/
├── src/main/java/top/egon/cola/platform/idp/starter/
│   ├── autoconfigure/IdpStarterAutoConfiguration.java                                  MODIFY [S7]
│   └── security/
│       ├── VerifiedUserTokenCarrier.java                                               MODIFY [S7]
│       └── rpc/
│           ├── IdpRpcBearerServerInterceptor.java                                      CREATE [S7]
│           ├── IdpRpcClientCredentialInterceptorFactory.java                           CREATE [S7]
│           └── IdpRpcSecurityContext.java                                              CREATE [S7]
└── src/test/java/top/egon/cola/platform/idp/starter/
    ├── autoconfigure/IdpStarterAutoConfigurationTest.java                              MODIFY [S7]
    └── security/rpc/
        ├── IdpRpcBearerServerInterceptorTest.java                                      CREATE [S7]
        └── IdpRpcClientCredentialInterceptorFactoryTest.java                           CREATE [S7]

scripts/unified-identity-local.sh                                                       MODIFY [S3]
```

每个路径只在上述 inventory 出现一次；Step 章节会按依赖顺序引用它们。

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

- 分支/提交：`main@8a64b586634d8d1fc94ffcc101e9add00c2e7730`。
- 仓库内 `rg --files -g AGENTS.md` 无结果；适用的是用户消息中的 Main Agent Rules。
- Plan 创建时 `git status --short` 只有 `?? docs/egon/`；实施时必须先重新检查状态，保留所有无关并发修改。
- 每步使用 path-limited add/commit；不得批量暂存整个仓库，不得修改既有 Flyway 文件或生成的 Protobuf Java。
- 本计划不新增依赖版本；Engine 只引入平台 BOM 已管理的 `egon-cola-platform-rbac3-gateway-adapter`。

### 6.2 Build, test, and environment prerequisites

| Concern            | Exact command/source                                                                                                                                                                                                                                                                                                                                                                                                    | Required state            | Validation boundary            |
|--------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------|--------------------------------|
| Java/Maven         | `java -version`; `mvn -version`；父 POM要求 Java 21                                                                                                                                                                                                                                                                                                                                                                         | JDK 21、Maven可用            | 构建工具                           |
| Components reactor | `mvn -f egon-cola-components/pom.xml -pl egon-cola-component-rpc/egon-cola-component-rpc-starter,egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter -am test`                                                                                                                                                                                                                                                  | exit 0                    | Components模块，不证明真实DDC          |
| Platforms reactor  | `mvn -f egon-cola-platforms/pom.xml -pl egon-cola-platform-rbac3/egon-cola-platform-rbac3-contract,egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin,egon-cola-platform-rbac3/egon-cola-platform-rbac3-gateway-adapter,egon-cola-platform-gateway/egon-cola-platform-gateway-engine,egon-cola-platform-gateway/egon-cola-platform-gateway-starter,egon-cola-platform-idp/egon-cola-platform-idp-starter -am test` | exit 0                    | 平台模块，不证明Redis/JWK/DDC运行态       |
| Static scope       | `git diff --check`；`git diff --name-only -- egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin`                                                                                                                                                                                                                                                                                            | 无 whitespace错误；Admin路径无变化 | 静态边界                           |
| Runtime            | 用户自行执行 `scripts/unified-identity-local.sh` 的既有工作流                                                                                                                                                                                                                                                                                                                                                                       | 双路径观察满足§9                 | 外部Redis/PostgreSQL/DDC/JWK真实闭环 |

### 6.3 Immutable constraints and approved decisions

- 注解名确定为 `@EgonRpcDirectReference`；现有 `@EgonRpcReference` 不重命名。
- APP 范围继续采用当前 effective/active role口径；不得新增独立 User-APP grant 模型。
- 历史 Gateway RPC Route 必须通过现有 Draft/Release 流程显式撤销；Consumer 注解不写 Gateway Admin。
- Gateway Admin权限链、MCP权限、HTTP/RPC Wire Descriptor、DDC Provider key、全量 `UserAuthorizationSnapshot` JSON、PostgreSQL
  Schema 均不可改变。
- 两条 RPC 路径无隐式 fallback；Direct 首期单次调用、确定性 Round Robin。
- 设计模式沿用 Spec：Strategy、Adapter、现有 Chain of Responsibility、Immutable Snapshot、Ports and Adapters；不新增统一
  LoadBalancer/第二条安全链。

### 6.4 Plan Clarifications

| ID              | Small implementation inference                                                                                               | Repository evidence                                                                            | Why semantics are unchanged                                   | Impact if wrong                                    |
|-----------------|------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------|---------------------------------------------------------------|----------------------------------------------------|
| `PLAN-CLAR-001` | `RpcConsumerProviderManager` 复用现有 `channelDrainTimeoutMs`，Channel建连复用 `RpcConsumerChannelFactory`；不新增 direct discovery 外部配置键 | `EgonRpcProperties.Consumer` 已有 default deadline、Gateway discovery和channel drain；Spec 未批准新配置字段 | 只选择内部已有超时，不改变注解或DDC查询契约；`EgonRpcProperties` 仅集中校验共享正值         | 若必须独立调优 Direct discovery，需要回到 Spec 增加公共配置契约        |
| `PLAN-CLAR-002` | 保留现有 `consumer/direct/RpcDirectClientFactory` 静态 target API；新注解使用 `consumer/provider` 包，不重命名或复用其单 target 生命周期                | 已有 `RpcDirectClientFactory` 服务 IdP/DDC基础设施静态直连；Spec target tree已为发现式直连定义独立 provider manager    | 避免破坏既有基础设施调用；两者都不经过Gateway但发现来源不同                             | 若要求统一两套 API，将扩大兼容与生命周期范围，需另行设计                     |
| `PLAN-CLAR-003` | Gateway Manager 的 demand 由双注解 BPP 在生命周期启动前登记；没有 Gateway 字段时 Manager 不订阅 `INTERNAL_GATEWAY`                                   | 当前 BPP 在 Bean initialization 阶段扫描字段，`RpcConsumerGatewayManager` 是 `SmartLifecycle`             | 实现 Spec 的“Direct-only 不要求 Gateway”，且保留Gateway字段的启动期 fail-fast | 若第三方在容器启动后动态创建带注解 Bean，需要补运行时 demand 生命周期，当前不支持此模式 |

## 7. Ordered File-by-file Implementation Steps

> 每一步先落 RED 测试，再落最小 GREEN 实现；实施时每步完成验证并做一个 path-limited commit，不能跨步提前修改后续文件。

### Step 1 — 发布与全量快照同版本的 BIZ→APP 轻量投影

- Requirements:`REQ-001`, `REQ-013`
- Dependencies:None
- Observable outcome:RBAC3 Admin 对一次 USER projection同时产生 full snapshot和 `GatewayBizAppScopeSnapshot`，在同一
  Fence 下按 scope/full→versions→pointer顺序写入，二者 identity/version/expiry相同且 scope 不含 permission。
- Ordered files:

#### File 1 —
`MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-contract/src/test/java/top/egon/cola/platform/rbac3/contract/ContractSerializationTest.java`

- Purpose:先定义三个新 record 的 JSON round-trip与“不含 permission字段”契约。
- Symbols:新增 `roundTripsGatewayBizAppScopeSnapshot()`、`gatewayScopeContainsNoPermissionPayload()`。
- Why now:这是公共 contract 的 RED 入口。
- Contract/signature changes:测试预期 `GatewayBizAppScopeSnapshot` 的
  tenant/sub/user/version/business/checksum/time字段及嵌套 BIZ→APP。
- Implementation pseudocode:

```java
snapshot = new GatewayBizAppScopeSnapshot(... one BusinessAccessScope(... one ApplicationAccessScope ...));
json = objectMapper.writeValueAsString(snapshot);
assertThat(read(json, GatewayBizAppScopeSnapshot.class)).isEqualTo(snapshot);
assertThat(json).doesNotContain("permissions", "dataScopes", "fieldPolicies", "resources");
```

- After this file:测试因三个 record 尚不存在而 RED，不因 fixture/Jackson 配置失败。

#### File 2 —
`MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/iam/role/service/RoleEligibilityServiceTest.java`

- Purpose:固定有效 APP scope 必须同时满足本地 ACTIVE、User Business grant、DDC APP/Business enabled和父子一致。
- Symbols:新增 `resolvesEffectiveApplicationScopeWithDdcBizAndAppIdentity()`、扩展各 fail-closed 参数化场景。
- Why now:projector需要结构化 scope，而不是当前 boolean。
- Contract/signature changes:预期 `resolveEffectiveScope(tenantId,userId,applicationId,at)` 返回
  `Optional<EffectiveApplicationScope>`；现有 `isEffective*` 结果保持。
- Implementation pseudocode:

```java
given active local app(ddcAppId, ddcBizId), effective business grant,
      catalog app(bizCode, appCode, enabled, parent matches);
assert scope == (ddcBizId,bizCode,ddcAppId,appCode);
for each missing/disabled/mismatched fact assert Optional.empty and isEffective == false;
```

- After this file:因新 VO/方法缺失 RED。

#### File 3 —
`MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/runtime/UserAuthorizationSnapshotProjectorTest.java`

- Purpose:定义一次 projection输出 full + scope，APP仅来自有效/已激活角色上下文并按 BIZ/APP稳定排序。
- Symbols:扩展现有 `project*` tests；新增 `projectsGatewayScopeFromEffectiveActiveApplicationContexts()`、
  `keepsFullSnapshotPermissionsUnchanged()`。
- Why now:锁定用户已确认的 APP 口径。
- Contract/signature changes:`UserSnapshotProjectionVO.gatewayScope()` 必须存在；两个快照共享 identity/version/time，scope
  checksum独立。
- Implementation pseudocode:

```java
projection = projector.project(commandWithActiveRolesAcrossBizApps());
assert projection.snapshot().permissions remain existing expected values;
assert projection.gatewayScope().businesses == sorted biz scopes with sorted active apps;
assert same tenant/sub/user/authVersion/policyVersion/generatedAt/expiresAt;
assert gatewayScope.checksum != blank and is deterministic under input ordering;
```

- After this file:因 `gatewayScope()` 和结构化 eligibility 缺失 RED。

#### File 4 —
`CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/runtime/RedisAuthorizationRuntimeRepositoryTest.java`

- Purpose:固定双快照发布顺序、TTL、Fence和失败原子可见性边界。
- Symbols:`publishesBothSnapshotsBeforePointerAndRemovesFenceLast()`、
  `scopeWriteFailureKeepsFenceAndDoesNotAdvancePointer()`、`rejectsScopeIdentityOrVersionMismatch()`。
- Why now:Redis发布顺序是 `REQ-013` 的核心 RED 契约。
- Contract/signature changes:捕获 `RBucket.set/delete` 次序；新 key由 `gatewayScope(...)` 产生。
- Implementation pseudocode:

```java
repo.publish(command(projection(full, scope)));
assert ordered calls: full snapshot set, gateway scope set,
       auth version set, policy version set, user pointer set, guard delete;
given scope bucket.set throws -> assert publish throws, pointer not set, guard not deleted;
given scope version differs -> assert IllegalArgumentException before writes;
```

- After this file:因 scope key/字段/写入不存在 RED。

#### File 5 —
`CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-contract/src/main/java/top/egon/cola/platform/rbac3/contract/authorization/ApplicationAccessScope.java`

- Purpose:表达一个 DDC APP 最小访问事实。
- Symbols:`record ApplicationAccessScope(String applicationId, String applicationCode)`。
- Why now:先建立叶节点，供 Business/snapshot编译。
- Contract/signature changes:两个字段 required/trimmed，不携带权限。
- Implementation pseudocode:

```java
compact constructor:
  applicationId = required(applicationId);
  applicationCode = required(applicationCode);
```

- After this file:APP scope 是可序列化不可变叶节点。

#### File 6 —
`CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-contract/src/main/java/top/egon/cola/platform/rbac3/contract/authorization/BusinessAccessScope.java`

- Purpose:表达 BIZ及其 APP children。
- Symbols:
  `record BusinessAccessScope(String businessId, String businessCode, List<ApplicationAccessScope> applications)`。
- Why now:组合 File 5，保持层级而非全局 appCode集合。
- Contract/signature changes:BIZ字段 required；applications immutable、按 code唯一性由构造器校验。
- Implementation pseudocode:

```java
copy applications;
reject blank business fields, null app, duplicate applicationCode;
require list already sorted by applicationCode or normalize to sorted immutable list;
```

- After this file:BIZ→APP父子不变量在 contract边界固定。

#### File 7 —
`CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-contract/src/main/java/top/egon/cola/platform/rbac3/contract/authorization/GatewayBizAppScopeSnapshot.java`

- Purpose:定义 Gateway 唯一可读取的最小授权投影。
- Symbols:Spec §10.2 所列十个 record components。
- Why now:组合前两个 record，并为 Publisher/Reader提供稳定类型。
- Contract/signature changes:identity/version/checksum/time required；businessCode唯一；expiry必须晚于generatedAt。
- Implementation pseudocode:

```java
validate tenantId, identitySub, rbacUserId, checksum;
validate authVersion >= 0 && policyVersion >= 0;
copy/sort businesses; reject duplicate businessCode;
require expiresAt.isAfter(generatedAt);
```

- After this file:Contract serialization tests可进入 GREEN。

#### File 8 —
`MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-core/src/main/java/top/egon/cola/platform/rbac3/core/runtime/Rbac3RuntimeKeyFactory.java`

- Purpose:提供 versioned Gateway scope Redis key。
- Symbols:`gatewayScope(String tenantId, String identitySub, long authVersion)`。
- Why now:Publisher与Reader必须共享唯一 key factory。
- Contract/signature changes:输出 `rbac3:{tenant}:gateway-scope:{identitySub}:{authVersion}`，复用 safe
  segment/nonnegative规则。
- Implementation pseudocode:

```java
if authVersion < 0 throw;
return prefix(tenantId) + "gateway-scope:" + segment(identitySub) + ':' + authVersion;
```

- After this file:读写端可引用同一 key契约。

#### File 9 —
`CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/role/service/EffectiveApplicationScope.java`

- Purpose:在 Admin内部传递已同时验证的 DDC BIZ/APP身份。
- Symbols:
  `record EffectiveApplicationScope(String businessId, String businessCode, String applicationId, String applicationCode)`。
- Why now:避免 Projector 再次查询 Catalog或丢失父 BIZ。
- Contract/signature changes:四字段 required/trimmed；不对外持久化。
- Implementation pseudocode:

```java
compact constructor validates all four nonblank identifiers/codes;
```

- After this file:Role eligibility可返回结构化结果。

#### File 10 —
`MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/role/service/RoleEligibilityService.java`

- Purpose:一次性解析有效 APP及其父 BIZ，现有 boolean API委托该结果。
- Symbols:新增 `Optional<EffectiveApplicationScope> resolveEffectiveScope(...)`；修改 `isEffective`、
  `isEffectiveApplicationCode`。
- Why now:为投影提供用户已批准的 effective/active APP口径。
- Contract/signature changes:异常、缺记录、disabled、parent mismatch均 `Optional.empty()`；`requireEffectiveRole` 行为不变。
- Implementation pseudocode:

```java
resolveEffectiveScope(tenant,user,localApp,at):
  load ACTIVE local app;
  require user's effectiveBusinessIds contains local.ddcBusinessId;
  load catalog.findApplication(local.ddcApplicationId);
  require appEnabled && businessEnabled && parent id matches;
  return EffectiveApplicationScope(ddcBusinessId,bizCode,ddcApplicationId,appCode);
catch runtime -> Optional.empty;
isEffective(...) = resolveEffectiveScope(...).isPresent();
```

- After this file:eligibility tests GREEN，旧调用者语义保持。

#### File 11 —
`MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/runtime/domain/vo/UserSnapshotProjectionVO.java`

- Purpose:让一次 projection携带 full和gateway scope。
- Symbols:record新增 `GatewayBizAppScopeSnapshot gatewayScope`。
- Why now:Repository只接收一个已有 projection边界，避免第二次投影调用。
- Contract/signature changes:三个 components均非 null；所有构造调用同步更新。
- Implementation pseudocode:

```java
record UserSnapshotProjectionVO(user, snapshot, gatewayScope) {
  compact constructor requireNonNull(all);
}
```

- After this file:写侧可在单命令中接收两个快照。

#### File 12 —
`MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/runtime/service/UserAuthorizationSnapshotProjector.java`

- Purpose:从被保留的有效 active contexts构造 BIZ→APP层级与独立 checksum。
- Symbols:`project`、`appContext`；新增内部 scope聚合/checksum helper。
- Why now:依赖 Files 5–11 的稳定 contract和 eligibility。
- Contract/signature changes:`project` 返回 VO(full,scope)；full snapshot permission内容完全不变。
- Implementation pseudocode:

```java
for activeRole roots by local applicationId:
  effective = roleEligibility.resolveEffectiveScope(...);
  if empty skip both appContext and scope entry;
  build existing AppAuthorizationContext unchanged;
  group effective by businessCode, add unique ApplicationAccessScope;
sort contexts, businesses, applications;
canonical = tenant|sub|user|versions|times|each bizId/code|each appId/code;
scope = new GatewayBizAppScopeSnapshot(..., sha256(canonical), same times);
return new UserSnapshotProjectionVO(user, fullSnapshot, scope);
```

- After this file:Projector tests GREEN；Gateway scope无 permission数据。

#### File 13 —
`MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/runtime/repository/redis/RedisAuthorizationRuntimeRepository.java`

- Purpose:在一个 publication Fence内双写 scope/full并最后推进 Pointer。
- Symbols:`publish(PublishCommandDTO)` identity/version校验和写序。
- Why now:最后连接 Step 1 写链。
- Contract/signature changes:验证 scope与 user/full完全同 identity/version/expiry；返回 checksum仍为既有 full snapshot
  checksum以保持调用兼容。
- Implementation pseudocode:

```java
validate command == user == full == scope identity and versions;
reject current newer version;
ttl = ttl(user.expiresAt);
set full snapshot(versioned), then gateway scope(versioned);
set authVersion, policyVersion;
set user pointer last;
delete publication guard only after every write succeeds;
return existing PublishResultVO(created, full.checksum);
```

- After this file:聚焦测试全部 GREEN；写失败不会删除 Fence或推进 Pointer。

- Verification command:

```bash
mvn -f egon-cola-platforms/pom.xml \
  -pl egon-cola-platform-rbac3/egon-cola-platform-rbac3-contract,egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin \
  -am -Dtest=ContractSerializationTest,RoleEligibilityServiceTest,UserAuthorizationSnapshotProjectorTest,RedisAuthorizationRuntimeRepositoryTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

- Expected result:exit 0；四个命名 test class通过；full snapshot序列化断言无变化；无数据库连接要求。
- Completion criteria:`REQ-001/013` 写侧字段、顺序、Fence、TTL、排序和 fail-closed测试全部通过。
- Rollback:仅回退 Step 1文件；尚无 Reader/Policy消费新 key，遗留 Redis key可按 TTL自然过期。
- Commit:`feat(rbac3): publish gateway biz app scope snapshots`

### Step 2 — 用 BIZ→APP Scope Provider 替换 Gateway 接口权限 Provider

- Requirements:`REQ-001`, `REQ-002`, `REQ-003`, `REQ-013`
- Dependencies:Step 1
- Observable outcome:RBAC3 Gateway Adapter只读取 user pointer/version/fence和 `gateway-scope`，先 BIZ后 APP；不再包含
  Operation Mapping/Permission reader/provider。
- Ordered files:

#### File 1 —
`CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-gateway-adapter/src/test/java/top/egon/cola/platform/rbac3/gateway/runtime/Rbac3GatewayScopeSnapshotReaderTest.java`

- Purpose:建立 scope reader 的完整 RED矩阵。
- Symbols:`deniesBusinessWithoutLookingUpApplication()`、`deniesApplicationWithinMatchedBusiness()`、`allowsNestedScope()`
  、Fence/missing/expiry/version/tenant tests。
- Why now:先证明 BIZ→APP顺序和运行态失败分类。
- Contract/signature changes:`authorize(GatewayAuthContext)` 只消费 `idp.biz-code/app-code`；严格 fake Redisson对
  operation mapping key访问直接失败。
- Implementation pseudocode:

```java
given valid pointer/versions/scope without biz -> decision DENY business reason;
assert application predicate/read counter == 0;
given biz but no nested app -> DENY application reason;
given nested match -> ALLOW;
for fence/missing/stale/mismatch/redisson error -> throw RuntimeUnavailableException;
verify no key starts with operation-mapping and no permissions are deserialized;
```

- After this file:因新 reader不存在 RED。

#### File 2 —
`MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-gateway-adapter/src/test/java/top/egon/cola/platform/rbac3/gateway/autoconfigure/Rbac3GatewayAdapterAutoConfigurationTest.java`

- Purpose:要求 AutoConfiguration暴露新 reader/provider ID且不暴露旧类型。
- Symbols:更新 context assertions。
- Why now:锁定生产 wiring再改配置。
- Contract/signature changes:预期 `Rbac3GatewayScopeSnapshotReader`、`Rbac3BizAppScopeAuthorizationProvider` 单
  Bean；providerId=`rbac3-biz-app-scope`。
- Implementation pseudocode:

```java
run enabled context with named redisson;
assert single new reader/provider;
assert provider.providerId == "rbac3-biz-app-scope";
assert no bean assignable to old permission types;
```

- After this file:因旧 AutoConfig仍装 Permission provider RED。

#### File 3 —
`MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-gateway-adapter/src/test/java/top/egon/cola/platform/rbac3/gateway/security/GatewayFailClosedSecurityMatrixTest.java`

- Purpose:固定新 Provider将 reader异常映射为 `ERROR`，DENY原因保持区分。
- Symbols:替换旧 provider fixture；增加 BIZ/APP deny与runtime error assertions。
- Why now:保持 `GatewaySecurityChain` 的 403/503输入语义。
- Contract/signature changes:异常 reason=`RBAC3_SCOPE_RUNTIME_UNAVAILABLE`；业务/应用拒绝不被转换为 error。
- Implementation pseudocode:

```java
provider(source returns deny business/application) -> DENY same reason;
provider(source throws) -> ERROR "RBAC3_SCOPE_RUNTIME_UNAVAILABLE";
never return ALLOW on exception;
```

- After this file:因新 provider不存在 RED。

#### File 4 —
`MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-gateway-adapter/src/test/java/top/egon/cola/platform/rbac3/gateway/performance/GatewayHotPathBudgetTest.java`

- Purpose:保持一次 scope decision/request和热路径无 Admin/DB/HTTP client。
- Symbols:provider类型替换；增加源码扫描禁止 `operationMapping`、`permissions()`。
- Why now:证明粒度收敛没有引入控制面查询。
- Contract/signature changes:1000次 decision只调用 source 1000次。
- Implementation pseudocode:

```java
invoke new provider DECISION_COUNT times; assert source calls same count;
scan adapter main sources; reject EntityManager/Jdbc/WebClient/rbac3-admin/operationMapping/permissions();
```

- After this file:旧类型/源码引用导致 RED。

#### File 5 —
`CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-gateway-adapter/src/main/java/top/egon/cola/platform/rbac3/gateway/runtime/Rbac3GatewayScopeSnapshotReader.java`

- Purpose:读取一致 scope并按 BIZ→APP顺序作决定。
- Symbols:constructor、`authorize`、`runtime`、`RuntimeUserAuthorization`、`RuntimeUnavailableException`。
- Why now:测试已确定读键、顺序和错误。
- Contract/signature changes:不接受 definitionSet/mappingVersion/operationId作为授权输入。
- Implementation pseudocode:

```java
authorize(context):
  require authenticated USER with tenant/sub;
  biz = required route["idp.biz-code"]; app = required route["idp.app-code"];
  scope = runtime(tenant, sub);
  business = scope.businesses filter exact businessCode;
  if exactly zero -> deny BUSINESS_SCOPE_DENIED; if duplicate -> unavailable;
  application = business.applications filter exact applicationCode;
  if exactly zero -> deny APPLICATION_SCOPE_DENIED; if duplicate -> unavailable;
  return allow;
runtime:
  read pointer; validate active/not expired; reject Fence;
  read auth/policy versions; compare pointer;
  read keyFactory.gatewayScope(...); validate identity/version/expiry;
  wrap malformed/Redis errors as RuntimeUnavailableException;
```

- After this file:reader tests可 GREEN，完全没有 Operation Mapping类型。

#### File 6 —
`CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-gateway-adapter/src/main/java/top/egon/cola/platform/rbac3/gateway/security/Rbac3BizAppScopeAuthorizationProvider.java`

- Purpose:把阻塞 Redis scope source适配为 Gateway reactive SPI。
- Symbols:`PROVIDER_ID="rbac3-biz-app-scope"`、`DecisionSource`、`authorize`。
- Why now:复用现有 Adapter模式和 boundedElastic边界。
- Contract/signature changes:source异常统一 `AuthorizationDecision.error("RBAC3_SCOPE_RUNTIME_UNAVAILABLE")`。
- Implementation pseudocode:

```java
authorize(context) = Mono.fromCallable(() -> source.authorize(context))
  .subscribeOn(boundedElastic())
  .onErrorReturn(error("RBAC3_SCOPE_RUNTIME_UNAVAILABLE"));
```

- After this file:Fail Closed/provider budget tests可进入 GREEN。

#### File 7 —
`MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-gateway-adapter/src/main/java/top/egon/cola/platform/rbac3/gateway/autoconfigure/Rbac3GatewayAdapterAutoConfiguration.java`

- Purpose:只装配新 scope reader/provider。
- Symbols:Bean methods改为 `rbac3GatewayScopeSnapshotReader`、`rbac3BizAppScopeAuthorizationProvider`。
- Why now:新类型已实现，可安全切换 wiring。
- Contract/signature changes:条件仍依赖 named `rbac3RuntimeRedissonClient`，Clock/key/sanitizer保持。
- Implementation pseudocode:

```java
@Bean reader(redisson, mapper, keyFactory, clock) -> new scope reader;
@Bean @ConditionalOnBean(scope reader) provider -> new scope provider(reader::authorize);
remove old bean imports/methods;
```

- After this file:AutoConfig test GREEN；旧 Bean不再可见。

#### File 8 —
`DELETE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-gateway-adapter/src/main/java/top/egon/cola/platform/rbac3/gateway/runtime/Rbac3GatewayRuntimeSnapshotReader.java`

- Purpose:物理移除 Operation Mapping/full Permission Snapshot热路径。
- Symbols:删除全部类型。
- Why now:所有生产 consumer已切到新 reader。
- Contract/signature changes:旧 Java内部类型不提供兼容别名。
- Implementation pseudocode:

```text
delete file; verify rg finds no production reference to Rbac3GatewayRuntimeSnapshotReader,
OperationPermissionMapping, keyFactory.operationMapping in gateway adapter.
```

- After this file:Adapter生产代码不再具备接口权限读取能力。

#### File 9 —
`DELETE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-gateway-adapter/src/main/java/top/egon/cola/platform/rbac3/gateway/security/Rbac3PermissionAuthorizationProvider.java`

- Purpose:移除旧 `rbac3-permission` Capability实现。
- Symbols:删除类型和旧 `PROVIDER_ID`。
- Why now:Policy迁移将在 Step 3完成，模块内部先只保留目标能力。
- Contract/signature changes:不把旧 ID伪装成新语义。
- Implementation pseudocode:

```text
delete file; no compatibility bean under "rbac3-permission".
```

- After this file:模块 capability只有 `rbac3-biz-app-scope`。

#### File 10 —
`DELETE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-gateway-adapter/src/test/java/top/egon/cola/platform/rbac3/gateway/runtime/Rbac3GatewayRuntimeSnapshotReaderTest.java`

- Purpose:删除只验证旧接口 Permission模型的测试。
- Symbols:删除旧 test class。
- Why now:新 reader测试已覆盖仍有效的版本/Fence/fail-closed行为。
- Contract/signature changes:N/A。
- Implementation pseudocode:

```text
delete obsolete mapping/permission fixtures and test class.
```

- After this file:测试集不再把旧行为当成需求。

- Verification command:

```bash
mvn -f egon-cola-platforms/pom.xml \
  -pl egon-cola-platform-rbac3/egon-cola-platform-rbac3-gateway-adapter \
  -am -Dtest=Rbac3GatewayScopeSnapshotReaderTest,Rbac3GatewayAdapterAutoConfigurationTest,GatewayFailClosedSecurityMatrixTest,GatewayHotPathBudgetTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

- Expected result:exit 0；命名测试全部通过；
  `rg -n "Rbac3PermissionAuthorizationProvider|Rbac3GatewayRuntimeSnapshotReader|operationMapping\(|permissions\(\)" <adapter-main>`
  无输出。
- Completion criteria:BIZ先于APP、运行态失败 ERROR、无 mapping/permission读取、旧实现删除。
- Rollback:连同 Step 2全部回退可恢复旧 Adapter；尚未切 Engine Policy时不影响运行规则。
- Commit:`refactor(rbac3): authorize gateway by biz app scope`

### Step 3 — 在 Gateway Engine 启用 Scope Capability并迁移 Policy ID

- Requirements:`REQ-001`, `REQ-002`, `REQ-003`, `REQ-004`
- Dependencies:Step 2
- Observable outcome:Engine executable classpath包含 IdP + RBAC3 scope adapters；HTTP Route只向安全链提供服务端
  BIZ/APP/env；本地 `BUSINESS_PROTECTED` Policy引用 `rbac3-biz-app-scope`；Admin模块无修改。
- Ordered files:

#### File 1 —
`MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/test/java/top/egon/cola/component/gateway/engine/IdpAdapterRuntimeClasspathTest.java`

- Purpose:把 Engine runtime capability预期从“只有 IdP Adapter”改为“IdP + scope Adapter”。
- Symbols:重命名测试为 `executableEngineCarriesIdentityAndBizAppScopeAdapters()`。
- Why now:先让缺失 runtime依赖产生明确 RED。
- Contract/signature changes:两个 AutoConfiguration class均 `Class.forName`成功；不再期待 RBAC3 class缺失。
- Implementation pseudocode:

```java
assertDoesNotThrow(load IdpGatewayAdapterAutoConfiguration);
assertDoesNotThrow(load Rbac3GatewayAdapterAutoConfiguration);
```

- After this file:因 Engine POM未依赖 Adapter而 RED。

#### File 2 —
`MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/test/java/top/egon/cola/component/gateway/engine/http/RuleBackedHttpGatewaySecurityProcessorTest.java`

- Purpose:固定 HTTP security attributes只含 target BIZ/APP/env。
- Symbols:更新/新增 `usesOnlyServerRouteBizAppIdentityForAuthorization()`。
- Why now:先证明 mapping metadata必须消失。
- Contract/signature changes:即使 Route metadata含 definitionSet/mappingVersion/applicationCode，结果也精确等于三个
  `idp.*`字段。
- Implementation pseudocode:

```java
attrs = securityAttributes(routeWithUpstreamAndLegacyRbacMetadata());
assertThat(attrs).containsExactlyEntriesOf(Map.of(
  "idp.biz-code", biz, "idp.app-code", app, "idp.env", env));
```

- After this file:当前实现仍复制三个 `rbac3.*`字段而 RED。

#### File 3 —
`MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/http/RuleBackedHttpGatewaySecurityProcessor.java`

- Purpose:从 HTTP数据面删除接口映射授权输入。
- Symbols:`securityAttributes(HttpRouteMatch)`；删除仅服务旧 RBAC metadata的 `copy`调用/辅助逻辑。
- Why now:新 Scope Provider只需要 Route upstream identity。
- Contract/signature changes:返回 immutable map仅含 `idp.biz-code/app-code/env`。
- Implementation pseudocode:

```java
upstream = route.route().upstream();
return Map.of("idp.biz-code", upstream.bizCode(),
              "idp.app-code", upstream.appCode(),
              "idp.env", upstream.env());
```

- After this file:HTTP attrs测试 GREEN；HTTP/RPC scope输入一致。

#### File 4 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/pom.xml`

- Purpose:把 RBAC3 Gateway Adapter加入 executable Engine runtime。
- Symbols:新增 managed dependency `top.egon:egon-cola-platform-rbac3-gateway-adapter`，scope=`runtime`。
- Why now:代码/测试已确认需要 capability，BOM已有版本管理。
- Contract/signature changes:不删除现有 `rbac3-starter`（下游/MCP能力仍需）；不添加新版本。
- Implementation pseudocode:

```xml
<dependency>
  <groupId>top.egon</groupId>
  <artifactId>egon-cola-platform-rbac3-gateway-adapter</artifactId>
  <scope>runtime</scope>
</dependency>
```

- After this file:classpath test GREEN。

#### File 5 —
`MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/resources/application.yml`

- Purpose:显式启用 Scope Adapter并指向 RBAC3 runtime Redis。
- Symbols:`egon.cola.platform.rbac3.gateway.enabled/runtime.*`。
- Why now:类在 runtime后还需满足 AutoConfiguration条件。
- Contract/signature changes:新增 Engine环境变量
  `GATEWAY_RBAC3_SCOPE_ENABLED/REDIS_ADDRESS/REDIS_DATABASE/REDIS_PASSWORD_FILE/TIMEOUT`；不复用请求 Header或MCP
  endpoint。
- Implementation pseudocode:

```yaml
egon.cola.platform.rbac3.gateway:
  enabled: ${GATEWAY_RBAC3_SCOPE_ENABLED:true}
  runtime:
    redis-enabled: ${GATEWAY_RBAC3_SCOPE_ENABLED:true}
    redis-address: ${GATEWAY_RBAC3_SCOPE_REDIS_ADDRESS:redis://127.0.0.1:6379}
    database: ${GATEWAY_RBAC3_SCOPE_REDIS_DATABASE:8}
    password-file: ${GATEWAY_RBAC3_SCOPE_REDIS_PASSWORD_FILE:}
    timeout: ${GATEWAY_RBAC3_SCOPE_REDIS_TIMEOUT:2s}
```

- After this file:Engine配置可装配新 Provider；MCP的 `rbac3.enabled`边界不变。

#### File 6 — `MODIFY scripts/unified-identity-local.sh`

- Purpose:本地环境先配置 scope runtime，再把 `BUSINESS_PROTECTED` Policy迁移到新 Provider ID。
- Symbols:Gateway Engine env生成；policy case中的 `authz_providers`。
- Why now:Engine capability/config已具备，最后切规则。
- Contract/signature changes:写入 scope Redis env；`authz_providers='["rbac3-biz-app-scope"]'`；保留 PUBLIC/IDENTITY和
  `ORIGINAL_BEARER`。
- Implementation pseudocode:

```bash
write_env gateway-engine.env GATEWAY_RBAC3_SCOPE_ENABLED true
write_env ...REDIS_ADDRESS "redis://${redis_host}:${redis_port}"
write_env ...REDIS_DATABASE 8
write_env ...PASSWORD_FILE "${secret_dir}/redis.password"
BUSINESS_PROTECTED -> authz_providers='["rbac3-biz-app-scope"]'
before deleting old adapter in deployed environment:
  assert no desired/active policy still references rbac3-permission
```

- After this file:本地发布脚本不再生成接口级 Gateway授权策略。

- Verification command:

```bash
mvn -f egon-cola-platforms/pom.xml \
  -pl egon-cola-platform-gateway/egon-cola-platform-gateway-engine \
  -am -Dtest=IdpAdapterRuntimeClasspathTest,RuleBackedHttpGatewaySecurityProcessorTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
bash -n scripts/unified-identity-local.sh
git diff --name-only -- egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin
```

- Expected result:Maven与`bash -n` exit 0；最后一条命令无输出；Engine主源码不再出现 `rbac3.definition-set-id`/
  `rbac3.mapping-version`。
- Completion criteria:Engine capability、配置、HTTP attrs和策略 ID全部匹配新范围模型，Gateway Admin零修改。
- Rollback:先把 Policy回滚到上一 Release，再回退 Engine/config/script；禁止通过清空 authorization providers回滚。
- Commit:`feat(gateway): enable biz app scope authorization`

### Step 4 — 建立中立 RPC Provider Directory 与发现式 Channel Manager

- Requirements:`REQ-006`, `REQ-007`, `REQ-008`, `REQ-011`
- Dependencies:None
- Observable outcome:RPC Starter具备与 DDC无关的精确 Provider query/snapshot SPI、租约 reconcile、稳定 Round Robin、Channel
  drain和单次调用 Strategy；尚未暴露字段注解。
- Ordered files:

#### File 1 —
`CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/consumer/provider/RpcConsumerProviderManagerTest.java`

- Purpose:定义多个 query、租约替换/过期、Round Robin、drain、无候选和停止语义。
- Symbols:`subscribesEachExactQueryOnce()`、`roundRobinsOnlyActiveProviderLeases()`、`replacesLeaseAndDrainsOldChannel()`、
  `neverFallsBackToGateway()`。
- Why now:这是 Provider discovery core的 RED边界。
- Contract/signature changes:Manager注册 query后返回 mode-bound channel provider；无实例抛 `RPC_PROVIDER_UNAVAILABLE`
  ；maxAttempts=1。
- Implementation pseudocode:

```java
provider = manager.register(query);
manager.start(); fakeDirectory.publish(query, two active endpoints + expired);
assert currentChannel calls alternate deterministically;
replace lease -> assert old.shutdown then scheduled shutdownNow;
empty snapshot -> assert EgonRpcException(RPC_PROVIDER_UNAVAILABLE);
verify fake Gateway directory is not a dependency/call;
manager.stop -> subscriptions close and all channels shutdownNow;
```

- After this file:因 provider package contracts/manager不存在 RED。

#### File 2 —
`CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/channel/RpcEndpoint.java`

- Purpose:为 Gateway/Provider endpoint共享建链字段，不共享发现语义。
- Symbols:interface accessors `host()`, `port()`, `secure()`。
- Why now:Channel Factory不应依赖 gateway package。
- Contract/signature changes:纯内部 transport port，无 DDC类型。
- Implementation pseudocode:

```java
public interface RpcEndpoint {
  String host(); int port(); boolean secure();
}
```

- After this file:两类 endpoint可复用 TLS建链。

#### File 3 —
`MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/gateway/RpcGatewayEndpoint.java`

- Purpose:让现有 Gateway endpoint实现中立 transport port。
- Symbols:`implements RpcEndpoint`。
- Why now:保持现有 Gateway Manager源码兼容，同时为 factory泛化做准备。
- Contract/signature changes:record fields/validation/activeAt不变。
- Implementation pseudocode:

```java
public record RpcGatewayEndpoint(...) implements RpcEndpoint { existing validation }
```

- After this file:Gateway endpoint行为无变化。

#### File 4 —
`MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/channel/RpcConsumerChannelFactory.java`

- Purpose:从任意 `RpcEndpoint` 建立相同 mTLS/plaintext channel。
- Symbols:新增/改为 `create(RpcEndpoint)`；保留 `create(RpcGatewayEndpoint)`兼容 overload并委托。
- Why now:Provider Manager需要复用现有 TLS安全规则。
- Contract/signature changes:secure/plaintext mismatch错误保持；retry仍 disabled。
- Implementation pseudocode:

```java
ManagedChannel create(RpcEndpoint endpoint) { existing builder/security branches }
ManagedChannel create(RpcGatewayEndpoint endpoint) { return create((RpcEndpoint) endpoint); }
```

- After this file:现有 Gateway tests仍编译，Provider endpoint可建链。

#### File 5 —
`CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/provider/RpcProviderQuery.java`

- Purpose:表达精确 Direct service identity。
- Symbols:record fields `bizCode,appCode,env,serviceName,group,version,protocol`。
- Why now:Directory订阅与Manager map的稳定 key。
- Contract/signature changes:全部 required/safe segment；protocol固定 `grpc`，禁止 wildcard。
- Implementation pseudocode:

```java
compact constructor trim/validate every field;
if protocol != "grpc" throw RPC_INVALID_CONTRACT;
```

- After this file:query可作为 immutable map key。

#### File 6 —
`CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/provider/RpcProviderEndpoint.java`

- Purpose:表达一个 DDC Provider lease endpoint。
- Symbols:record同 Gateway endpoint字段并 `implements RpcEndpoint`。
- Why now:Snapshot需要与 DDC解耦的 lease模型。
- Contract/signature changes:routable host、1..65535 port、instance/lease required、expiry required；`activeAt(Instant)`。
- Implementation pseudocode:

```java
validate identity/host/port/expiry exactly as RpcGatewayEndpoint;
activeAt(now) = leaseExpireAt.isAfter(now);
```

- After this file:Provider endpoint可被通用 factory消费。

#### File 7 —
`CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/provider/RpcProviderSnapshot.java`

- Purpose:传递 Directory revision和 immutable endpoints。
- Symbols:`record RpcProviderSnapshot(long revision, Instant observedAt, List<RpcProviderEndpoint> endpoints)`。
- Why now:对齐既有 `RpcGatewaySnapshot` 模式。
- Contract/signature changes:revision非负、observedAt非 null、list copy。
- Implementation pseudocode:

```java
validate revision >= 0; require observedAt; endpoints = List.copyOf(endpoints);
```

- After this file:snapshot可安全异步发布。

#### File 8 —
`CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/provider/RpcProviderSubscription.java`

- Purpose:中立关闭 Directory订阅。
- Symbols:`@FunctionalInterface close()`，`extends AutoCloseable`且不抛 checked exception。
- Why now:Manager停止时必须先关订阅。
- Contract/signature changes:与 Gateway subscription风格一致。
- Implementation pseudocode:

```java
interface RpcProviderSubscription extends AutoCloseable { void close(); }
```

- After this file:订阅生命周期可由 Manager拥有。

#### File 9 —
`CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/provider/RpcProviderDirectory.java`

- Purpose:定义 Starter到注册中心 Adapter的读端口。
- Symbols:`subscribe(RpcProviderQuery, Consumer<RpcProviderSnapshot>)`。
- Why now:依赖倒置，Starter不能构造 `DdcServiceKey`。
- Contract/signature changes:返回 closeable subscription；不暴露 Gateway或DDC类型。
- Implementation pseudocode:

```java
@FunctionalInterface
interface RpcProviderDirectory {
  RpcProviderSubscription subscribe(query, listener);
}
```

- After this file:DDC Adapter可在 Step 6实现。

#### File 10 —
`CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/provider/RpcConsumerProviderManager.java`

- Purpose:按唯一 query共享订阅和Channel Set。
- Symbols:`register`, `start`, `stop`, `currentChannel`, `recordFailure`，内部
  `Registration/ActiveProvider/ProviderIdentity`。
- Why now:Directory contract和transport port已齐备。
- Contract/signature changes:注册不阻塞；无有效实例在调用点报 `RPC_PROVIDER_UNAVAILABLE`
  ；snapshot按instanceId/leaseId稳定排序；单 monitor reconcile。
- Implementation pseudocode:

```java
register(query): registrations.computeIfAbsent(query); return provider bound to query;
start(): for each registration subscribe(query, snapshot -> accept(query,snapshot)); schedule expiry;
accept: discard older revision; filter active; retain same instanceId+leaseId+endpoint channel;
        create channels for new leases; drain removed/replaced; store immutable sorted list;
currentChannel(query, excluded): expire; choose floorMod(sequence++, candidates.size); else throw PROVIDER_UNAVAILABLE;
recordFailure(query, channel): remove+drain only that provider; no gateway call;
stop(): close subscriptions, shutdown channels/executor;
```

- After this file:Manager核心行为存在，但尚无调用点注解。

#### File 11 —
`CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/provider/ProviderRpcInvocationChannelProvider.java`

- Purpose:把一个精确 query绑定为 typed proxy的 Channel Strategy。
- Symbols:implements `RpcInvocationChannelProvider`；`currentChannel`, `recordFailure`, `maxAttempts`。
- Why now:复用现有 `RpcConsumerInvocationHandler`，不在 handler判断模式。
- Contract/signature changes:`maxAttempts()`固定1；所有调用委托 Manager/query；无 fallback。
- Implementation pseudocode:

```java
currentChannel(excluded) = manager.currentChannel(query, excluded);
recordFailure(channel) = manager.recordFailure(query, channel);
maxAttempts() = 1;
```

- After this file:Provider Manager test GREEN，Strategy边界完成。

- Verification command:

```bash
mvn -f egon-cola-components/pom.xml \
  -pl egon-cola-component-rpc/egon-cola-component-rpc-starter \
  -am -Dtest=RpcConsumerProviderManagerTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

- Expected result:exit 0；命名测试通过；`rg -n "DdcService|RpcGatewayDirectory" .../consumer/provider` 无输出。
- Completion criteria:精确 query、active lease、RR、drain、停止和 `RPC_PROVIDER_UNAVAILABLE`语义具备，Provider strategy
  `maxAttempts=1`。
- Rollback:仅回退新增中立 SPI/Manager与 transport泛化；尚无 Spring bean或DDC consumer。
- Commit:`feat(rpc): add provider directory channel management`

### Step 5 — 增加 Gateway/Direct 双注解注入与 mode-specific demand

- Requirements:`REQ-005`, `REQ-006`, `REQ-007`, `REQ-008`, `REQ-011`
- Dependencies:Step 4
- Observable outcome:应用可分别或同时注入 `@EgonRpcReference`/`@EgonRpcDirectReference`；direct-only不订阅/等待
  Gateway；同字段冲突启动失败；两者均使用已有 interceptor factory链且无隐式 fallback。
- Ordered files:

#### File 1 —
`CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/consumer/proxy/EgonRpcReferenceBeanPostProcessorTest.java`

- Purpose:定义 Gateway、Direct、双字段、同契约双字段、同字段双注解和缺 mode factory错误。
- Symbols:六个对应 test methods和 fixture beans/contracts。
- Why now:公开注解行为的主要 RED契约。
- Contract/signature changes:冲突/缺工厂异常必须含 bean name、field name、annotation mode。
- Implementation pseudocode:

```java
process bean(gatewayField,directField); assert both fields set to factory-specific proxies;
process bean(two fields same contract different annotations); assert allowed;
process doubleAnnotatedField -> IllegalStateException contains bean/field/both annotations;
direct annotation + no direct factory -> stable startup error; gateway equivalent likewise;
assert non-interface and missing @EgonRpcService remain RPC_INVALID_CONTRACT paths;
```

- After this file:因新 annotation/factory/BPP contract不存在 RED。

#### File 2 —
`CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/config/EgonRpcAutoConfigTest.java`

- Purpose:用 `ApplicationContextRunner` 固定 gateway-only/direct-only/both和 interceptor注入 wiring。
- Symbols:`directOnlyDoesNotRequireGatewayDirectory()`、`gatewayOnlyKeepsExistingPath()`、`bothModesCoexist()`、
  `missingSelectedDirectoryFailsAtInjection()`。
- Why now:防止 Bean条件仍强制 Gateway。
- Contract/signature changes:Manager/Proxy beans按相应 Directory条件创建；BPP总能给出稳定配置错误。
- Implementation pseudocode:

```java
with RpcProviderDirectory only + direct fixture -> context starts, no gateway subscription;
with RpcGatewayDirectory only + gateway fixture -> existing manager starts;
with both -> both managers/proxies and ordered RpcClientInterceptorFactory list;
with annotation but missing matching directory -> startupFailure root message identifies mode;
```

- After this file:当前 AutoConfig要求 `RpcGatewayDirectory`而 RED。

#### File 3 —
`MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/consumer/gateway/RpcConsumerGatewayManagerTest.java`

- Purpose:增加 demand为零不订阅、demand大于零保持现有 fail-fast的回归。
- Symbols:`doesNotSubscribeWithoutGatewayReferences()`、修改 manager helper登记 demand。
- Why now:Direct-only不要求 Gateway的 RED证据。
- Contract/signature changes:既有 gateway tests在 `manager.registerDemand()` 后保持；零 demand start不访问 Directory。
- Implementation pseudocode:

```java
manager.start without demand; verify directory.subscribe never called; state STOPPED;
manager.registerDemand(); manager.start(); execute existing ready/failure tests unchanged;
```

- After this file:当前 Manager无 demand API而 RED。

#### File 4 —
`MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/config/EgonRpcPropertiesTest.java`

- Purpose:固定 Direct复用的共享 timeout/drain设置必须为正且不新增自动 fallback开关。
- Symbols:新增 `consumerSharedTransportSettingsArePositive()`、`consumerHasNoAutomaticGatewayDirectFallbackProperty()`。
- Why now:落实 `PLAN-CLAR-001`。
- Contract/signature changes:通过 `Consumer.validateSharedSettings()` 或 AutoConfig验证；不新增 direct mode配置枚举。
- Implementation pseudocode:

```java
props.consumer.validateSharedSettings succeeds for defaults;
set defaultTimeout/channelDrain <=0 -> RPC_INVALID_CONTRACT;
reflect Consumer properties; assert no mode/autoFallback field;
```

- After this file:因集中校验方法不存在 RED。

#### File 5 —
`CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/annotation/EgonRpcDirectReference.java`

- Purpose:实现用户已确认的 Direct字段注解。
- Symbols:Spec §9准确签名 `bizCode/appCode/env/group/version/timeoutMs`。
- Why now:测试已锁定公开 API。
- Contract/signature changes:`@Target(FIELD)`, runtime retention；无 permission/gatewayOperation/fallback字段。
- Implementation pseudocode:

```java
String bizCode(); String appCode(); String env() default "";
String group() default ""; String version() default "";
long timeoutMs() default -1;
```

- After this file:调用方可编译声明 Direct引用。

#### File 6 —
`CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/proxy/RpcDirectReferenceProxyFactory.java`

- Purpose:把注解+Contract解析为精确 query和 Provider-bound typed proxy。
- Symbols:constructor；`<T> T create(Class<T>, EgonRpcDirectReference)`。
- Why now:Strategy选择留在 factory，不污染 invocation handler。
- Contract/signature changes:env blank→`RpcProcessIdentity.env`；group/version blank→validated
  contract；timeout只缩短default。
- Implementation pseudocode:

```java
contract = validator.validate(type);
query = new RpcProviderQuery(required(annotation.bizCode/appCode),
  blankTo(annotation.env, process.env), contract.serviceName,
  blankTo(annotation.group, contract.group), blankTo(annotation.version, contract.version), "grpc");
channelProvider = providerManager.register(query);
factory = new RpcConsumerProxyFactory(validator, channelProvider, processIdentity,
  statusMapper, defaultTimeout, interceptorFactories);
return factory.create(type, annotation.timeoutMs);
```

- After this file:Direct proxy构造不依赖 DDC或 Gateway。

#### File 7 —
`MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/gateway/RpcConsumerGatewayManager.java`

- Purpose:仅在存在 Gateway引用 demand时订阅 `INTERNAL_GATEWAY`。
- Symbols:新增 `registerDemand()`/demand counter；调整 `start/isRunning/stop`。
- Why now:BPP将在 lifecycle启动前登记。
- Contract/signature changes:零 demand start为 no-op；有 demand沿用当前 discovery timeout/fail-fast/RR/retry。
- Implementation pseudocode:

```java
registerDemand(): synchronized require state STOPPED then demandCount++;
start(): if demandCount == 0 return without directory.subscribe;
otherwise execute existing start unchanged;
stop(): existing cleanup; retain demandCount for immutable bean wiring/restart;
```

- After this file:Direct-only不触发 Gateway discovery；既有 Gateway行为保持。

#### File 8 —
`MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/proxy/EgonRpcReferenceBeanPostProcessor.java`

- Purpose:一次扫描安全处理两种注解并登记对应 demand/query。
- Symbols:constructor接收 optional Gateway proxy/manager和Direct factory；`postProcessBeforeInitialization`。
- Why now:两种 factory/manager已存在。
- Contract/signature changes:同字段双注解优先报冲突；每字段只注入一次；错误含 bean/field。
- Implementation pseudocode:

```java
for each field annotated gateway or direct:
  if both -> throw conflict(beanName, fieldName);
  require field.type interface;
  if gateway:
    require gatewayFactory+manager; manager.registerDemand(); proxy=gatewayFactory.create(...);
  else:
    require directFactory; proxy=directFactory.create(type, annotation);
  makeAccessible and set once;
```

- After this file:BPP tests除 wiring条件外 GREEN。

#### File 9 —
`MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/config/EgonRpcProperties.java`

- Purpose:集中校验两类 Consumer共享的 default deadline和channel drain，不引入新外部键。
- Symbols:`Consumer.validateSharedSettings()`；现有 Gateway-specific校验仍由 Gateway Manager完成。
- Why now:AutoConfig/Provider Manager可复用一致校验。
- Contract/signature changes:默认字段和值不变。
- Implementation pseudocode:

```java
void validateSharedSettings() {
  if defaultTimeoutMs <= 0 || channelDrainTimeoutMs <= 0 -> EgonRpcException(RPC_INVALID_CONTRACT);
}
```

- After this file:Properties tests GREEN；配置兼容。

#### File 10 —
`MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/config/EgonRpcAutoConfig.java`

- Purpose:条件装配两套 Directory/Manager/Proxy并把有序 client interceptors同时注入。
- Symbols:新增 `RpcConsumerChannelFactory` Bean、Provider Manager/Direct Factory Beans；调整 Gateway beans/BPP。
- Why now:所有中立类型和注解行为已完成。
- Contract/signature changes:Gateway beans `@ConditionalOnBean(RpcGatewayDirectory)`；Direct beans
  `@ConditionalOnBean(RpcProviderDirectory)`；BPP使用 `ObjectProvider`给缺失模式稳定错误。
- Implementation pseudocode:

```java
@Bean shared channelFactory(transportSecurity);
@Bean if gatewayDirectory -> gatewayManager(shared factory,...);
@Bean if providerDirectory -> providerManager(shared factory, properties);
@Bean gatewayProxy(..., interceptors.orderedStream().toList());
@Bean directProxy(... providerManager ..., same interceptors);
@Bean BPP(optional gatewayProxy/manager, optional directProxy);
provider lifecycle keeps all ServerInterceptor beans;
```

- After this file:四个 RED test class GREEN；两种模式可同容器存在。

- Verification command:

```bash
mvn -f egon-cola-components/pom.xml \
  -pl egon-cola-component-rpc/egon-cola-component-rpc-starter \
  -am -Dtest=EgonRpcReferenceBeanPostProcessorTest,EgonRpcAutoConfigTest,RpcConsumerGatewayManagerTest,EgonRpcPropertiesTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

- Expected result:exit 0；direct-only context无 Gateway订阅；gateway-only和both通过；冲突信息稳定；现有
  `RpcDirectClientFactoryTest`在模块回归仍通过。
- Completion criteria:`REQ-005`–`REQ-008/011` 的注解、兼容、并存、fail-fast和no-fallback可观察。
- Rollback:回退 Step 5恢复单注解；Step 4未被使用但可单独保留或随后回退。
- Commit:`feat(rpc): add ddc direct reference injection`

### Step 6 — 用 DDC Adapter 实现精确 `RPC_PROVIDER` 发现

- Requirements:`REQ-006`, `REQ-008`, `REQ-009`
- Dependencies:Step 4（实施顺序上建议在 Step 5后验证完整 context）
- Observable outcome:`RpcProviderDirectory`由 DDC Adapter提供，严格按 Direct query构造 `DdcServiceKind.RPC_PROVIDER`
  key并映射活动 leases；绝不查询 `INTERNAL_GATEWAY`。
- Ordered files:

#### File 1 —
`CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter/src/test/java/top/egon/cola/component/rpc/ddc/registry/DdcRpcProviderDirectoryTest.java`

- Purpose:锁定完整 DDC key、revision/lease映射和订阅关闭。
- Symbols:`queriesExactRpcProviderServiceKey()`、`mapsLeaseSnapshotAndPreservesRevision()`。
- Why now:DDC Adapter的 RED契约。
- Contract/signature changes:captor断言 BIZ/APP/env/service/group/version/protocol和 kind。
- Implementation pseudocode:

```java
subscription = directory.subscribe(query, observed::set);
assert captured key == DdcServiceKey(biz,env,app,RPC_PROVIDER,service,group,version,"grpc");
publish Ddc snapshot -> assert provider endpoints preserve instance/lease/host/port/secure/expiry and revision;
subscription.close -> verify DDC subscription close;
```

- After this file:因实现不存在 RED。

#### File 2 —
`MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter/src/test/java/top/egon/cola/component/rpc/ddc/autoconfigure/DdcRpcAutoConfigurationTest.java`

- Purpose:要求 registry-enabled context同时暴露 Provider Registry、Gateway Directory和Provider Directory。
- Symbols:扩展 `registrySwitchCreatesRegistryPortsWithOnlyRegistryCredential()`。
- Why now:锁定 Adapter Bean wiring。
- Contract/signature changes:`hasSingleBean(RpcProviderDirectory.class)`；不要求 Gateway consumer配置。
- Implementation pseudocode:

```java
run registry-enabled context;
assert beans: DdcServiceRegistryClient, RpcProviderRegistry,
              RpcGatewayDirectory, RpcProviderDirectory;
```

- After this file:因新 Bean缺失 RED。

#### File 3 —
`CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter/src/main/java/top/egon/cola/component/rpc/ddc/registry/DdcRpcProviderDirectory.java`

- Purpose:把 DDC registry snapshots适配为中立 Provider snapshots。
- Symbols:implements `RpcProviderDirectory`；constructor/client；`subscribe`。
- Why now:查询/映射已由测试固定。
- Contract/signature changes:不使用 default biz/app fallback，Direct query所有字段精确；调用
  `ServiceInstanceMetaCodec.decode`验证保留 metadata但不暴露它。
- Implementation pseudocode:

```java
key = new DdcServiceKey(query.bizCode, query.env, query.appCode,
  RPC_PROVIDER, query.serviceName, query.group, query.version, query.protocol);
ddcSub = client.subscribe(key, snap -> listener.accept(new RpcProviderSnapshot(
  snap.revision, snap.observedAt,
  snap.instances.map(validateMeta then new RpcProviderEndpoint(...)))));
return ddcSub::close;
```

- After this file:Directory tests GREEN；没有 Gateway slot访问。

#### File 4 —
`MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter/src/main/java/top/egon/cola/component/rpc/ddc/autoconfigure/DdcRpcAutoConfiguration.java`

- Purpose:在已有 `DdcServiceRegistryClient`上暴露中立 Provider Directory Bean。
- Symbols:`ddcRpcProviderDirectory(...)`。
- Why now:实现已存在，最后接入 Spring。
- Contract/signature changes:`@ConditionalOnBean(DdcServiceRegistryClient.class)` +
  `@ConditionalOnMissingBean(RpcProviderDirectory.class)`。
- Implementation pseudocode:

```java
@Bean RpcProviderDirectory ddcRpcProviderDirectory(client) {
  return new DdcRpcProviderDirectory(client);
}
```

- After this file:Direct annotation通过 DDC实现完整 discovery call path。

- Verification command:

```bash
mvn -f egon-cola-components/pom.xml \
  -pl egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter \
  -am -Dtest=DdcRpcProviderDirectoryTest,DdcRpcAutoConfigurationTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

- Expected result:exit 0；captured key kind严格为 `RPC_PROVIDER`；AutoConfig同时提供三个 registry-related RPC ports。
- Completion criteria:DDC读侧与现有写侧使用同一物理服务键；Direct path没有 Gateway Directory调用。
- Rollback:回退 Adapter Bean/实现/测试；Provider registration不受影响。
- Commit:`feat(rpc-ddc): discover direct rpc providers`

### Step 7 — 在 IdP Starter 中建立可信 RPC USER Credential Relay

- Requirements:`REQ-011`, `REQ-012`
- Dependencies:Step 5
- Observable outcome:HTTP或上游 RPC 已验证的 USER access token可由 RPC client interceptor附加为单一 `authorization`
  metadata；Provider server interceptor重新验证并在每个 gRPC callback作用域建立 Spring Security Context；Consumer不读取
  RBAC3权限。
- Ordered files:

#### File 1 —
`CREATE egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-starter/src/test/java/top/egon/cola/platform/idp/starter/security/rpc/IdpRpcClientCredentialInterceptorFactoryTest.java`

- Purpose:定义可信 token存在/不存在、Servlet→RPC和RPC→RPC relay行为。
- Symbols:`relaysVerifiedServletUserToken()`、`relaysVerifiedRpcContextToken()`、`addsNothingWithoutVerifiedToken()`、
  `neverDuplicatesAuthorization()`。
- Why now:Client侧 RED契约先行。
- Contract/signature changes:只从 `VerifiedUserTokenCarrier.currentOrNull()`取值；输出恰好一个 `Bearer <token>`。
- Implementation pseudocode:

```java
with carrier token -> create interceptor, start call, assert one AUTHORIZATION value;
with IdpRpcSecurityContext token -> same assertion;
without trusted context -> metadata has no authorization;
with preexisting authorization -> reject/replace per single-value invariant, never merge duplicate;
```

- After this file:因 metadata key/context/factory缺失 RED。

#### File 2 —
`CREATE egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-starter/src/test/java/top/egon/cola/platform/idp/starter/security/rpc/IdpRpcBearerServerInterceptorTest.java`

- Purpose:定义 Provider侧 USER token验证、匿名、非法/重复值、callback context和清理。
- Symbols:`establishesAuthenticationForValidUserBearer()`、`allowsAnonymousWhenMissing()`、
  `rejectsMalformedInvalidExpiredOrDuplicateBearer()`、`clearsContextAfterEveryCallback()`。
- Why now:证明“下游自行鉴权”可执行且没有身份泄漏。
- Contract/signature changes:有效 USER→`IdpAuthenticationToken`；非法→gRPC `UNAUTHENTICATED`且 handler不调用；缺失→匿名继续。
- Implementation pseudocode:

```java
valid verifier + one bearer:
  next handler returns listener whose onHalfClose captures SecurityContext principal and carrier token;
  assert IdentityPrincipal and token visible only during callback;
missing -> next invoked with no authentication;
duplicate/malformed/invalid/expired -> call.close(UNAUTHENTICATED), next never called;
after callback assert SecurityContextHolder empty and rpc context token absent outside call;
```

- After this file:因 server interceptor/context不存在 RED。

#### File 3 —
`MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-starter/src/test/java/top/egon/cola/platform/idp/starter/autoconfigure/IdpStarterAutoConfigurationTest.java`

- Purpose:要求启用 IdP时把 client factory和server interceptor暴露给 RPC Starter。
- Symbols:新增/扩展 context assertion；校验 user verifier缺失时不创建。
- Why now:先锁定 Spring Bean条件和无重复 Bean。
- Contract/signature changes:`RpcClientInterceptorFactory`中包含 IdP factory；`ServerInterceptor`中包含 IdP server
  interceptor。
- Implementation pseudocode:

```java
enabled context with UserAccessTokenVerifier -> hasSingleBean(IdpRpc...Factory/Interceptor);
assert beans assignable to RpcClientInterceptorFactory and ServerInterceptor;
user override beans still win through ConditionalOnMissingBean;
```

- After this file:当前 AutoConfig无 RPC安全 Bean而 RED。

#### File 4 —
`MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/context/invocation/RpcMetadataKeys.java`

- Purpose:为 Gateway、IdP、RPC共享唯一 Authorization metadata key。
- Symbols:`public static final Metadata.Key<String> AUTHORIZATION = ascii("authorization")`。
- Why now:避免各模块重复拼写/Marshaller。
- Contract/signature changes:additive internal constant；不改变既有 keys。
- Implementation pseudocode:

```java
public static final Metadata.Key<String> AUTHORIZATION = ascii("authorization");
```

- After this file:两侧 interceptor可共享 key。

#### File 5 —
`CREATE egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-starter/src/main/java/top/egon/cola/platform/idp/starter/security/rpc/IdpRpcSecurityContext.java`

- Purpose:保存当前已验证 USER principal/token并提供 callback作用域桥接。
- Symbols:gRPC `Context.Key<IdentityPrincipal>`/token key；`with(...)`、`currentTokenOrNull()`、
  `runWithSpringSecurity(...)`。
- Why now:普通 ThreadLocal无法跨 gRPC executor；Servlet carrier需要可查询的可信 fallback。
- Contract/signature changes:token只在 gRPC Context短暂存在，不进入 `Authentication.credentials`。
- Implementation pseudocode:

```java
Context with(principal, token) = Context.current().withValues(PRINCIPAL, principal, TOKEN, validatedToken);
currentTokenOrNull = TOKEN.get();
runWithSpringSecurity(principal, callback):
  previous = SecurityContextHolder.getContext();
  set empty context with new IdpAuthenticationToken(principal);
  try callback.run(); finally restore/clear previous exactly;
```

- After this file:RPC Context与Spring Security的责任边界明确。

#### File 6 —
`MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-starter/src/main/java/top/egon/cola/platform/idp/starter/security/VerifiedUserTokenCarrier.java`

- Purpose:在保持 Servlet API兼容下增加 non-throwing/gRPC fallback读取。
- Symbols:新增 `currentOrNull()`；`current()`委托并在 null时抛现有错误。
- Why now:RPC client必须允许无 USER token的匿名/服务调用继续。
- Contract/signature changes:优先 Servlet request attribute；没有 Servlet时读取 `IdpRpcSecurityContext`；set/clear(
  Servlet)不变。
- Implementation pseudocode:

```java
currentOrNull():
  if ServletRequestAttributes and valid ATTRIBUTE -> return token;
  return IdpRpcSecurityContext.currentTokenOrNull();
current(): token=currentOrNull; if null throw existing IllegalStateException;
```

- After this file:Servlet→RPC和RPC→RPC均可读取可信 token，无任意 ThreadLocal。

#### File 7 —
`CREATE egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-starter/src/main/java/top/egon/cola/platform/idp/starter/security/rpc/IdpRpcClientCredentialInterceptorFactory.java`

- Purpose:按 invocation创建只负责 credential relay的 gRPC interceptor。
- Symbols:implements `RpcClientInterceptorFactory`；`create`。
- Why now:carrier/context已可用。
- Contract/signature changes:无 token不加 metadata；有 token时验证长度并禁止覆盖已有 Authorization。
- Implementation pseudocode:

```java
create(invocation):
  token = VerifiedUserTokenCarrier.currentOrNull();
  return interceptor whose call.start:
    if token != null:
      if headers.getAll(AUTHORIZATION) not empty -> throw invalid relay state;
      headers.put(AUTHORIZATION, "Bearer " + token);
    delegate.start;
```

- After this file:Client tests GREEN；没有 RBAC3依赖或权限判断。

#### File 8 —
`CREATE egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-starter/src/main/java/top/egon/cola/platform/idp/starter/security/rpc/IdpRpcBearerServerInterceptor.java`

- Purpose:Provider重新验证一个 USER Bearer并建立安全上下文。
- Symbols:implements `ServerInterceptor`；Bearer parse/reason/noop listener/callback wrapper。
- Why now:client relay协议已固定。
- Contract/signature changes:最大长度沿用 IdP 8192；缺 token匿名；非法/expired/duplicate为 `Status.UNAUTHENTICATED`。
- Implementation pseudocode:

```java
values = headers.getAll(AUTHORIZATION);
if none -> next.startCall;
if count != 1 or malformed/tooLong -> close UNAUTHENTICATED; return noop listener;
verification = userVerifier.verify(rawToken);
if not Valid<IdentityPrincipal> -> close UNAUTHENTICATED(reason); no handler;
grpcContext = IdpRpcSecurityContext.with(principal, rawToken);
listener = Contexts.interceptCall(grpcContext, call, headers, next);
return wrapper: each onMessage/onHalfClose/onCancel/onComplete/onReady executes
  grpcContext.call(() -> runWithSpringSecurity(principal, delegate callback));
```

- After this file:Provider本地 `@RequiresPermission`可读取已验证 principal。

#### File 9 —
`MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-starter/src/main/java/top/egon/cola/platform/idp/starter/autoconfigure/IdpStarterAutoConfiguration.java`

- Purpose:条件注册 IdP RPC client/server安全 adapters。
- Symbols:两个 `@Bean @ConditionalOnBean(UserAccessTokenVerifier.class) @ConditionalOnMissingBean` methods。
- Why now:实现已通过单测，最后交给 RPC Starter的已有 ObjectProvider机制。
- Contract/signature changes:不增加新 Maven dependency（IdP Starter已依赖 RPC Starter）；HTTP filter beans不变。
- Implementation pseudocode:

```java
@Bean IdpRpcClientCredentialInterceptorFactory idpRpcClient...() = new ...;
@Bean IdpRpcBearerServerInterceptor idpRpcServer...(UserAccessTokenVerifier verifier) = new ...(verifier);
```

- After this file:AutoConfig test GREEN；所有 RPC proxies/providers自动接收 adapters。

- Verification command:

```bash
mvn -f egon-cola-platforms/pom.xml \
  -pl egon-cola-platform-idp/egon-cola-platform-idp-starter \
  -am -Dtest=IdpRpcClientCredentialInterceptorFactoryTest,IdpRpcBearerServerInterceptorTest,IdpStarterAutoConfigurationTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

- Expected result:exit 0；有效/匿名/非法/重复/清理场景通过；日志断言不包含原始 token。
- Completion criteria:凭证中继与Provider身份建立可测试；Consumer无 permission读取或判定。
- Rollback:回退 IdP RPC beans/classes与 shared metadata key；HTTP IdP filter不受影响。
- Commit:`feat(idp): relay verified user credentials over rpc`

### Step 8 — 将 Gateway 已验证 Bearer受控转发到 RPC Provider

- Requirements:`REQ-002`, `REQ-003`, `REQ-011`, `REQ-012`
- Dependencies:Steps 2, 3, 7
- Observable outcome:RPC→RPC和HTTP→RPC仅转发 `GatewaySecurityChain.Result.forwardingCredential`；原始未经验证
  Authorization仍被清洗；Provider可按原始 USER身份执行本地权限。
- Ordered files:

#### File 1 —
`MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/test/java/top/egon/cola/component/gateway/engine/rpc/RuleBackedRpcGatewaySecurityProcessorTest.java`

- Purpose:要求 RPC security outcome保留 forwarding credential且 attributes仍只有 BIZ/APP/env。
- Symbols:新增/扩展 `returnsOnlyVerifiedForwardingCredential()`、`usesOnlyTargetBizAppAttributes()`。
- Why now:RPC安全结果的 RED契约。
- Contract/signature changes:匿名/no-forward Policy的 credential为 null；允许 `ORIGINAL_BEARER`且验证成功时等于 chain
  result。
- Implementation pseudocode:

```java
authorize route through stub chain(result with credential);
assert outcome.forwardingCredential == verified credential;
assert captured authContext.attributes == idp biz/app/env only;
assert raw inbound metadata alone cannot populate outcome credential;
```

- After this file:当前 Outcome无 credential而 RED。

#### File 2 —
`CREATE egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/test/java/top/egon/cola/component/gateway/engine/rpc/RpcGatewayCredentialForwardingTest.java`

- Purpose:固定 RPC Gateway outbound metadata的 credential规则。
- Symbols:`forwardsVerifiedBearerWhenPresent()`、`doesNotForwardRawInboundBearer()`、`doesNotLogBearerOnFailure()`。
- Why now:防止从 inbound metadata直接复制形成信任绕过。
- Contract/signature changes:使用 `RpcMetadataKeys.AUTHORIZATION`；仅 Outcome credential可产生 header。
- Implementation pseudocode:

```java
inbound contains forged Authorization; security outcome has null -> outbound absent;
security outcome has GatewayCredential verified-token -> outbound == "Bearer verified-token";
capture error/telemetry/log arguments -> never contains token;
```

- After this file:当前 forwarder从不附加 credential，首个允许场景 RED。

#### File 3 —
`MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/test/java/top/egon/cola/component/gateway/engine/http/DefaultGatewayHttpDataPlaneHandlerCredentialForwardingTest.java`

- Purpose:把已有 HTTP→HTTP credential测试扩展到 RPC Provider上游。
- Symbols:新增 `restoresVerifiedBearerForRpcUpstreamWhenPolicyAllows()`、`neverRestoresUnverifiedInboundBearerForRpc()`。
- Why now:当前 `forwardHttpCredential`仅在 Provider protocol HTTP时为 true。
- Contract/signature changes:RPC bridge收到的 headers只在 forwardingCredential存在且transport policy允许时含
  Authorization。
- Implementation pseudocode:

```java
provider protocol RPC + allowed + verified credential -> captured bridge headers authorization present;
provider protocol RPC + raw source only/null credential -> absent;
authorizationForwardingAllowed false -> absent;
```

- After this file:当前 handler对 RPC传 false而 RED。

#### File 4 —
`MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/test/java/top/egon/cola/component/gateway/engine/rpc/HttpRpcUpstreamAdapterTest.java`

- Purpose:回归 Bridge只把已清洗 headers中的单一 Authorization映射为共享 RPC metadata key。
- Symbols:扩展现有 `authorization`断言，增加 absent/duplicate-sanitized case。
- Why now:证明 Step 8不需修改 Bridge生产实现。
- Contract/signature changes:保持当前 body/schema/trace行为。
- Implementation pseudocode:

```java
invoke bridge with sanitized headers containing one bearer -> outbound AUTHORIZATION exact;
invoke without authorization -> metadata absent;
```

- After this file:当前 Bridge行为应立即 GREEN，作为非回归基线。

#### File 5 —
`MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/rpc/GatewayRpcSecurityProcessor.java`

- Purpose:让 RPC安全边界显式携带已验证 forwarding credential。
- Symbols:`Outcome`新增 `GatewayCredential forwardingCredential`。
- Why now:不允许 Forwarder回读原始 inbound Authorization。
- Contract/signature changes:canonical constructor允许 credential null；`anonymous()`为 null。
- Implementation pseudocode:

```java
record Outcome(TrustedIdentity trustedIdentity,
               Set<String> fieldsToRemove,
               GatewayCredential forwardingCredential) { copy/require non-sensitive fields }
anonymous() = new Outcome(empty, Set.of(), null);
```

- After this file:Forwarder可从可信安全结果取 credential。

#### File 6 —
`MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/rpc/RuleBackedRpcGatewaySecurityProcessor.java`

- Purpose:把 `GatewaySecurityChain.Result.forwardingCredential()`映射到 RPC Outcome。
- Symbols:`authorize(...).map(result -> new Outcome(...))`。
- Why now:Outcome contract已扩展。
- Contract/signature changes:不从 `inboundMetadata`构造 credential；BIZ/APP attributes不变。
- Implementation pseudocode:

```java
return chain.execute(...).map(result -> new Outcome(
  result.trustedIdentity(), result.fieldsToRemove(), result.forwardingCredential()));
```

- After this file:RPC processor tests GREEN。

#### File 7 —
`MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/rpc/RpcGatewayForwarder.java`

- Purpose:在 RPC outbound metadata中恢复已验证 Bearer。
- Symbols:`outboundHeaders(...)`。
- Why now:只依赖可信 Outcome和共享 key。
- Contract/signature changes:credential非 null时 `put(AUTHORIZATION,"Bearer "+tokenReference)`；不调用
  `copy(inbound,...AUTHORIZATION)`。
- Implementation pseudocode:

```java
build existing service/trace/trusted identity metadata;
if security.forwardingCredential != null:
  result.put(RpcMetadataKeys.AUTHORIZATION,
             "Bearer " + security.forwardingCredential.tokenReference());
return result;
```

- After this file:RPC Gateway credential tests GREEN；伪造 inbound不会透传。

#### File 8 —
`MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/http/DefaultGatewayHttpDataPlaneHandler.java`

- Purpose:允许 HTTP→RPC Bridge复用同一 verified credential恢复逻辑。
- Symbols:Provider attempt调用 `forwardedHeaders`时的最后两个 flags；`restoreOriginalBearer`保持唯一写点。
- Why now:Bridge已证明可安全映射清洗后的 Authorization。
- Contract/signature changes:`forwardHttpCredential`语义调整为“当前 upstream transport可接收credential”，HTTP与RPC都可；仍受
  `authorizationForwardingAllowed`和非 null credential约束。
- Implementation pseudocode:

```java
forwardedHeaders(...,
  route.transportPolicy.authorizationForwardingAllowed(),
  provider.protocolType in {HTTP,RPC},
  publicProtocol);
restoreOriginalBearer only if transportAllowed && forwardingCredential != null;
```

- After this file:HTTP→HTTP、HTTP→RPC均只转发经安全链验证的 token。

- Verification command:

```bash
mvn -f egon-cola-platforms/pom.xml \
  -pl egon-cola-platform-gateway/egon-cola-platform-gateway-engine \
  -am -Dtest=RuleBackedRpcGatewaySecurityProcessorTest,RpcGatewayCredentialForwardingTest,DefaultGatewayHttpDataPlaneHandlerCredentialForwardingTest,HttpRpcUpstreamAdapterTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

- Expected result:exit 0；两条 Gateway→RPC路径的有效/伪造/禁止转发场景通过；test output无 token。
- Completion criteria:Provider收到原始已验证 USER AT，Gateway不做 Permission，未经验证 header无法越过 sanitizer/security
  result。
- Rollback:整体回退 Step 8会停止 Gateway→RPC USER credential relay；不得在已下沉下游权限后单独回退，除非同步回滚
  Policy/业务发布。
- Commit:`feat(gateway): relay verified bearer to rpc providers`

### Step 9 — 固化 DDC注册、Gateway文档与历史 Route 的独立边界

- Requirements:`REQ-004`, `REQ-009`, `REQ-010`, `REQ-014`
- Dependencies:Steps 1–8
- Observable outcome:测试证明无 Gateway Group的 RPC不报告 Definition但 Provider lease仍注册 `RPC_PROVIDER`；发布清单要求
  Direct-only前显式删除/撤销历史 Operation并发布新 Release；Gateway Admin权限代码零变化。
- Ordered files:

#### File 1 —
`MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-starter/src/test/java/top/egon/cola/component/gateway/starter/discovery/RpcGatewayDefinitionContributorTest.java`

- Purpose:显式固定 Contract无 `@GatewayInterfaceGroup`就完全跳过报告。
- Symbols:新增无 Group fixture contract/provider和 `skipsRpcContractWithoutGatewayInterfaceGroup()`。
- Why now:避免 Direct annotation被误接成文档开关。
- Contract/signature changes:有 Group时现有全部 Unary方法报告行为不变；Consumer注解不参与 contributor。
- Implementation pseudocode:

```java
catalog contains groupedContract and ungroupedContract;
definitions = contributor.discover();
assert grouped present with existing methods;
assert ungrouped service/method absent;
assert contributor source has no EgonRpcDirectReference dependency;
```

- After this file:若当前 skip边界正确应直接 GREEN；否则只修正测试fixture/现有 contributor最小判断，不新增 Consumer反向写入。

#### File 2 —
`MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter/src/test/java/top/egon/cola/component/rpc/ddc/registry/DdcRpcProviderRegistryTest.java`

- Purpose:证明注册请求只取 Provider registration/service identity，不依赖任何 Gateway annotation/report bean。
- Symbols:新增 `registersRpcProviderWithoutGatewayDefinitionMetadata()`。
- Why now:与 File 1共同锁定三条事实链分离。
- Contract/signature changes:captured `DdcServiceRegistration.serviceKey.serviceKind=RPC_PROVIDER`，完整
  group/version/grpc不变。
- Implementation pseudocode:

```java
registration = RpcProviderRegistration for contract identity with ordinary metadata only;
registry.register(registration);
capture DdcServiceRegistration;
assert RPC_PROVIDER exact key and lease fields;
assert metadata need not contain gateway group/operation/definition;
```

- After this file:Provider lease独立性有明确回归证据。

- Verification command:

```bash
mvn -f egon-cola-platforms/pom.xml \
  -pl egon-cola-platform-gateway/egon-cola-platform-gateway-starter \
  -am -Dtest=RpcGatewayDefinitionContributorTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
mvn -f egon-cola-components/pom.xml \
  -pl egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter \
  -am -Dtest=DdcRpcProviderRegistryTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
git diff --name-only -- egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin
```

- Expected result:两个 Maven命令 exit 0；Admin路径 diff无输出；无 Group Definition为空但 DDC captured registration存在。
- Completion criteria:文档、实例、Consumer调用三个事实源互不反向控制；`REQ-014`进入发布 Gate而非注解副作用。
- Rollback:测试文件可单独回退但不建议；没有生产行为或数据回滚。
- Commit:`test(gateway): lock rpc reporting and registration boundaries`

## 8. Test, Validation, and Quality Gates

| Gate/order                   | Command or method                                                                                                                                                                                                                                                                                                                                                                                                                                                                   | Scope                                | Expected result                                                                                                                                                                                                                                                                              | Failure returns to       | Requirements                                                                                                                                                                                                                                        |
|------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| RED S1                       | Step 1聚焦命令，在生产文件前执行                                                                                                                                                                                                                                                                                                                                                                                                                                                                 | RBAC contract/admin                  | 仅因新 records/method/key缺失失败                                                                                                                                                                                                                                                                   | S1 Files 5–13            | `REQ-001`,`REQ-013`                                                                                                                                                                                                                                 |
| GREEN S1                     | Step 1聚焦命令                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | RBAC写侧                               | exit 0；双快照/Fence tests通过                                                                                                                                                                                                                                                                     | S1 owning file           | `REQ-001`,`REQ-013`                                                                                                                                                                                                                                 |
| RED/GREEN S2                 | Step 2聚焦命令                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | RBAC Gateway adapter                 | RED因新 reader/provider缺失；GREEN exit 0                                                                                                                                                                                                                                                         | S2                       | `REQ-001`–`REQ-003`,`REQ-013`                                                                                                                                                                                                                       |
| RED/GREEN S3                 | Step 3 Maven + `bash -n`                                                                                                                                                                                                                                                                                                                                                                                                                                                            | Engine capability/policy             | Adapter class存在、attrs精确、shell合法                                                                                                                                                                                                                                                              | S3                       | `REQ-001`–`REQ-004`                                                                                                                                                                                                                                 |
| RED/GREEN S4                 | Step 4聚焦命令                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | RPC Provider Manager                 | RED因SPI/manager缺失；GREEN RR/drain/no fallback                                                                                                                                                                                                                                                 | S4                       | `REQ-006`–`REQ-008`,`REQ-011`                                                                                                                                                                                                                       |
| RED/GREEN S5                 | Step 5聚焦命令                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | RPC annotation/wiring                | 四种 context和冲突测试通过                                                                                                                                                                                                                                                                            | S5                       | `REQ-005`–`REQ-008`,`REQ-011`                                                                                                                                                                                                                       |
| RED/GREEN S6                 | Step 6聚焦命令                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | RPC DDC adapter                      | key=`RPC_PROVIDER`且精确，Bean存在                                                                                                                                                                                                                                                                 | S6                       | `REQ-006`,`REQ-008`,`REQ-009`                                                                                                                                                                                                                       |
| RED/GREEN S7                 | Step 7聚焦命令                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | IdP/RPC security                     | token有效/匿名/非法/清理通过                                                                                                                                                                                                                                                                           | S7                       | `REQ-011`,`REQ-012`                                                                                                                                                                                                                                 |
| RED/GREEN S8                 | Step 8聚焦命令                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | Gateway→RPC                          | 只转发verified credential                                                                                                                                                                                                                                                                       | S8                       | `REQ-002`,`REQ-003`,`REQ-012`                                                                                                                                                                                                                       |
| GREEN S9                     | Step 9两个命令                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | Reporting/registration               | 无 Group无报告，仍有lease                                                                                                                                                                                                                                                                           | S9                       | `REQ-009`,`REQ-010`,`REQ-014`                                                                                                                                                                                                                       |
| Components regression        | `mvn -f egon-cola-components/pom.xml -pl egon-cola-component-rpc/egon-cola-component-rpc-starter,egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter -am test`                                                                                                                                                                                                                                                                                                              | RPC reactor                          | exit 0，既有 static direct client tests也通过                                                                                                                                                                                                                                                      | S4–S6/S9                 | `REQ-005`–`REQ-011`                                                                                                                                                                                                                                 |
| Platforms regression         | `mvn -f egon-cola-platforms/pom.xml -pl egon-cola-platform-rbac3/egon-cola-platform-rbac3-contract,egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin,egon-cola-platform-rbac3/egon-cola-platform-rbac3-gateway-adapter,egon-cola-platform-gateway/egon-cola-platform-gateway-admin,egon-cola-platform-gateway/egon-cola-platform-gateway-engine,egon-cola-platform-gateway/egon-cola-platform-gateway-starter,egon-cola-platform-idp/egon-cola-platform-idp-starter -am test` | 受影响平台+Admin回归                        | exit 0                                                                                                                                                                                                                                                                                       | S1–S3/S7–S9              | `REQ-001`–`REQ-004`,`REQ-010`,`REQ-012`,`REQ-013`                                                                                                                                                                                                   |
| Static removal               | `rg -n "rbac3-permission                                                                                                                                                                                                                                                                                                                                                                                                                                                            | Rbac3PermissionAuthorizationProvider | Rbac3GatewayRuntimeSnapshotReader                                                                                                                                                                                                                                                            | rbac3\.definition-set-id | rbac3\.mapping-version" egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-gateway-adapter/src/main scripts/unified-identity-local.sh` | 数据面/脚本 | 无输出 | S2/S3 | `REQ-002` |
| Consumer permission boundary | `rg -n "rbac3                                                                                                                                                                                                                                                                                                                                                                                                                                                                       | permission                           | RequiresPermission" egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter/src/main/java/top/egon/cola/component/rpc/ddc/registry` | Consumer/Directory       | 无新增 permission读取；仅可能命中文档否定语句                                                                                                                                                                                                                        | S4–S7 | `REQ-011` |
| Secret/log scan              | `rg -n "logger.*token                                                                                                                                                                                                                                                                                                                                                                                                                                                               | log.*authorization                   | metadata.*token" <changed-main-paths>` 并人工审查 diff                                                                                                                                                                                                                                            | 安全                       | 无原始 token日志/指标/DDC metadata                                                                                                                                                                                                                         | S7/S8 | `REQ-012` |
| Formatting/scope             | `git diff --check`; `git status --short`; `git diff --name-only -- .../gateway-admin`                                                                                                                                                                                                                                                                                                                                                                                               | 全仓                                   | 无 whitespace错误；仅计划文件树；Admin生产路径无变化                                                                                                                                                                                                                                                           | owning Step              | All                                                                                                                                                                                                                                                 |
| Runtime E2E                  | 用户启动本地栈后按 §9.3执行                                                                                                                                                                                                                                                                                                                                                                                                                                                                    | Redis/DDC/JWK/Provider/Engine        | 两条路径地址、身份、授权、无fallback均符合                                                                                                                                                                                                                                                                    | 相应 Step                  | All                                                                                                                                                                                                                                                 |

聚焦 Maven命令使用 `-Dsurefire.failIfNoSpecifiedTests=false`，因为 `-am`带入的上游模块不一定包含同名测试；这不忽略目标模块测试失败。任何丢失进程句柄或未取得
exit code的命令都不算通过，必须重跑聚焦命令。

## 9. Migration, Compatibility, Rollout, and Rollback

### 9.1 Data and schema migration

N/A。依据 Spec §11，不新增/修改 PostgreSQL表、JPA PO、DDC Proto或 Flyway migration。新增的是 TTL控制的 Redis versioned key；历史
Flyway文件保持不可变。

### 9.2 Source/runtime compatibility

- `@EgonRpcReference` signature和 Gateway默认行为保持；新 `@EgonRpcDirectReference`是 additive API。
- 现有 `RpcDirectClientFactory`静态 target API不改，保证 IdP/DDC基础设施客户端兼容。
- DDC `RPC_PROVIDER`和`INTERNAL_GATEWAY` key schema不改；只新增读 Adapter。
- 全量 `UserAuthorizationSnapshot/AppAuthorizationContext` JSON不改；新 scope独立发布。
- RPC Starter和 RPC DDC Adapter必须同一平台版本发布；旧 Consumer不使用新注解时不受影响。

### 9.3 Deployment order and post-deploy gates

1. 部署 Step 1 RBAC3 Admin写侧，保持旧 Gateway Policy；触发活跃 USER授权重投影。
2. 通过 Redis只读检查确认每个 active pointer都有同 `authVersion` scope key，identity/version/expiry匹配且
   Fence为空。缺失用户不得进入下一步。
3. 部署 Step 2 Adapter artifact和 Step 3 Engine artifact/config，但保持旧 Engine实例/Policy受控滚动；确认新 Engine
   capability列出 `rbac3-biz-app-scope`。
4. 发布 `BUSINESS_PROTECTED.authorizationProviderIds=["rbac3-biz-app-scope"]`，观察 BIZ deny、APP deny、runtime
   unavailable；确认没有 active policy引用 `rbac3-permission`后才完成旧代码清理部署。
5. 部署 Steps 4–7 RPC Starter/DDC Adapter/IdP Starter到一个试点 Provider+Consumer；Provider必须已有本地 IdP/RBAC3接口权限链。
6. 在不同字段分别调用 Gateway和Direct：Gateway连接地址为 `INTERNAL_GATEWAY` lease；Direct连接地址为目标 `RPC_PROVIDER`
   lease；Trace连续，Provider principal相同。
7. 切换某契约为 direct-only前，先通过现有 Admin Draft删除其 RPC Operation/Route并发布新 Release；查询 release/diff/runtime
   consistency确认 active release不含旧 Operation。之后才移除 Provider的 `@GatewayInterfaceGroup`或 Gateway Starter依赖。
8. 观察 endpoint count、channel create/drain、scope denied/runtime unavailable、Provider本地 permission denied；扫描日志确保无
   Authorization值。

### 9.4 Runtime acceptance matrix

| Scenario                                                   | Expected Gateway/DDC path                     | Expected auth result                          |
|------------------------------------------------------------|-----------------------------------------------|-----------------------------------------------|
| Gateway RPC + BIZ denied                                   | Consumer→`INTERNAL_GATEWAY`; no Provider call | Gateway 403/`PERMISSION_DENIED`, APP未查        |
| Gateway RPC + APP denied                                   | Consumer→Gateway; no Provider call            | Gateway 403/`PERMISSION_DENIED`               |
| Gateway RPC + scope allowed + downstream permission denied | Consumer→Gateway→Provider                     | Gateway allow；Provider本地 deny                 |
| Direct RPC + downstream permission allowed                 | Consumer→exact `RPC_PROVIDER`                 | 无 Gateway span/address；Provider验证 USER并 allow |
| Direct RPC + no Provider                                   | Provider Directory empty                      | `RPC_PROVIDER_UNAVAILABLE`，不连 Gateway         |
| Gateway RPC + no Gateway                                   | Gateway Directory empty                       | `RPC_GATEWAY_UNAVAILABLE`，不直连 Provider        |
| Direct-only contract无 Group                                | Provider仍有 DDC lease                          | Gateway report/active route不存在                |

### 9.5 Rollback

- Scope写侧可先部署/后回滚；旧 Gateway忽略新 key，key由 TTL自然清理。
- Policy已切新 Provider后，回滚顺序必须是先回滚 Policy/Release，再回滚 Engine/Adapter；禁止清空 authorization providers。
- Direct调用点可源码改回 `@EgonRpcReference`，但仅当对应 Gateway Definition/Active Release仍存在；没有运行时 AUTO
  fallback。
- 已显式撤销的 Gateway Route必须走正常 Draft/Release恢复，Consumer注解回滚不会自动恢复。
- IdP/Gateway credential relay不可在下游权限已成为唯一接口授权后单独回滚；需要与调用点/Policy整体回退。
- Redis scope keys无需破坏性删除；数据库 rollback N/A。

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Effective Spec section | Steps        | Files                                                           | Tests/gates                          | Completion evidence       |
|-------------|------------------------|--------------|-----------------------------------------------------------------|--------------------------------------|---------------------------|
| `REQ-001`   | §4, §7                 | S1–S3        | scope records/projector/repository/reader/provider/Engine attrs | TEST-001/002；S1–S3 gates             | BIZ miss时APP不查；双允许才转发     |
| `REQ-002`   | §4, §8                 | S2, S3, S8   | old reader/provider DELETE；HTTP/RPC security                    | TEST-003/004；static removal          | 数据面无 mapping/permissions读 |
| `REQ-003`   | §4, §7.5               | S2, S3, S8   | scope provider、Engine config、existing chain outcomes            | fail-closed matrix/Engine tests      | 401/403/503与暴露规则保持        |
| `REQ-004`   | §4, §12                | S3, S9       | 无 Admin生产文件                                                     | Platforms regression；Admin diff gate | Admin接口权限链零修改且测试通过        |
| `REQ-005`   | §4, §9                 | S5           | Gateway manager/BPP/AutoConfig                                  | Gateway manager/context tests        | 旧注解仍查 Gateway且源兼容         |
| `REQ-006`   | §4, §9                 | S4–S6        | provider SPI/manager/direct factory/DDC directory               | Provider manager/DDC key tests       | exact `RPC_PROVIDER`直连    |
| `REQ-007`   | §4, §9                 | S4, S5       | annotation/BPP/two factories                                    | BPP/context tests                    | 双字段双路径；同字段拒绝              |
| `REQ-008`   | §4, §7.5               | S4–S6        | separate managers/providers                                     | no-candidate/no-gateway tests        | 错误码分离且无fallback           |
| `REQ-009`   | §4                     | S6, S9       | DDC directory + Registry test                                   | TEST-010/S9                          | 无文档仍注册lease               |
| `REQ-010`   | §4                     | S9           | Contributor test                                                | TEST-011/S9                          | 无 Group无report/Route      |
| `REQ-011`   | §4                     | S4, S5, S7   | RPC consumer/IdP client factory                                 | consumer static boundary             | Consumer只中继不授权            |
| `REQ-012`   | §4, §9                 | S7, S8       | IdP interceptors/carrier、Gateway forwarders                     | TEST-013/014；S7/S8                   | Provider重验token并建身份       |
| `REQ-013`   | §4, §10                | S1, S2       | dual projection/key/publish/reader                              | TEST-015；S1/S2                       | 同版本、Fence、TTL、identity    |
| `REQ-014`   | §4, §16                | S9 + rollout | Contributor/Registry tests；现有 Admin Draft/Release流程             | release diff/runtime gate            | 历史 Operation显式撤销          |

## 11. Risks, Blockers, and User Decisions

| ID         | Risk or decision                                                   | Impacted Steps/files   | Evidence                                              | Owner         | Status/action                                                        |
|------------|--------------------------------------------------------------------|------------------------|-------------------------------------------------------|---------------|----------------------------------------------------------------------|
| `DEC-001`  | 注解名 `@EgonRpcDirectReference`                                      | S5                     | 用户本轮确认“1 可以”                                          | User          | Closed                                                               |
| `DEC-002`  | APP范围使用 effective/active role                                      | S1                     | 用户本轮确认“2 是”                                           | User          | Closed                                                               |
| `DEC-003`  | 历史 Gateway RPC Route显式下线                                           | S9/rollout             | 用户本轮确认“3 确认”                                          | User/Operator | Closed；部署前逐契约执行                                                      |
| `RISK-001` | scope双写顺序/Fence错误导致503窗口                                           | S1/S2                  | 当前 authVersion key非版本化且Reader fail closed             | Implementer   | 用 ordered interaction/failure tests与覆盖率Gate                          |
| `RISK-002` | Engine同时包含 rbac3-starter和gateway-adapter，named Redis bean条件装配顺序不一致 | S3 application.yml/POM | 两者均可能声明 `rbac3RuntimeRedissonClient`且有 missing-bean条件 | Implementer   | Context/classpath test；两组配置指向同 runtime Redis；若不能保证则退回Spec，不擅自拆新Bean名 |
| `RISK-003` | Direct绕过 Gateway集中限流/熔断                                            | S4–S6                  | Spec §15已接受                                           | User          | Accepted；需治理的调用继续Gateway                                             |
| `RISK-004` | Provider未启用本地IdP/RBAC3                                             | S7/rollout             | Gateway不再兜底接口Permission                               | Service owner | Required rollout gate；未通过不得迁移该Provider                               |
| `RISK-005` | gRPC异步 callback泄漏/丢失 SecurityContext                               | S7                     | Servlet carrier当前只支持request scope                     | Implementer   | 每callback scope/cleanup tests；不使用普通ThreadLocal token                 |
| `RISK-006` | Active Release仍含 direct-only RPC                                   | S9/rollout             | Definition与Consumer不同事实源                              | Operator      | Pre-deploy release diff必须为零                                          |
| `RISK-007` | Plan基线后出现并发源码修改                                                    | All                    | 当前仅 docs untracked                                    | Main agent    | 每Step前检查status；重叠时停止并协调                                              |

当前无阻塞实施计划的重大决定。`RISK-002` 是实施期必须用 ApplicationContext验证的装配风险；如果真实
AutoConfiguration顺序无法稳定共享同一 Redis client，这将改变依赖/配置边界，必须停止并回到 Spec，而不能作为局部重构处理。

## 12. Review and Acceptance

### 12.1 Original requirement fidelity

Plan逐项覆盖 Gateway只校验 BIZ→APP、移除数据面接口Permission、Admin权限不变、RPC双注解/并存/Direct DDC发现、Direct可无
Gateway文档、所有 Provider注册 DDC、Consumer不授权、Provider下游授权和历史 Route显式下线。没有把范围鉴权删成完全不鉴权，也没有引入隐式
fallback。

### 12.2 Spec consistency

计划未新增数据库、页面、Streaming、LoadBalance框架、MCP变更、Admin业务能力或 Gateway/Direct AUTO模式。三个 `PLAN-CLAR-*`
只处理已有配置/程序化静态直连/生命周期装配的局部实现位置，不改变已批准外部行为。

### 12.3 Repository executability

所有现有路径、类和测试入口按 `main@8a64b586` 复核；CREATE/DELETE路径来自 Spec target tree；Maven命令使用真实 reactor/module
artifact路径。每个文件在 Change File Tree只出现一次，Step写域不重叠，commit可使用 path-limited staging保护并发工作。

### 12.4 Test and release completeness

每个行为变化均先安排可解释的 RED，再安排最小 GREEN；覆盖 contract、projection、Redis publication、Gateway scope reader、Engine
wiring、RPC Manager/BPP/DDC adapter、IdP interceptors、Gateway forwarding、文档/注册边界和 Admin零修改。数据库迁移为 N/A；真实
Redis/DDC/JWK/Provider/Engine闭环明确留给用户启动本地栈后验证，Plan不冒充运行态证据。

### 12.5 Final verdict

PASS — Ready for user review
