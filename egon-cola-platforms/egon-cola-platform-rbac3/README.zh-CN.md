# Egon COLA RBAC3 权限平台

RBAC3 是一个租户隔离的权限控制面与运行时鉴权系统，统一管理应用、资源清单、
角色 DAG、角色分配、约束、Session 激活角色集合、授权快照、审计证据，以及
Gateway/DDC 发布和服务发现。全部能力位于 `egon-cola-platforms` 下，按一个完整
系统交付，不拆成多个上线阶段。

## 一、不可变业务语义

1. **不需要审批。** 权限变更是受身份、授权、幂等、并发和审计约束的直接命令，
   RBAC3 不建立审批单、审批人或审批状态机。
2. **轮岗属于业务定义。** 排班、轮岗和当班规则由业务系统决定；权限平台只提供
   “激活角色集合”的语义接口，不维护轮岗流程。
3. **登录不选择角色。** 登录只确认身份并创建空激活角色的 Session；Bootstrap
   返回候选角色，随后由激活接口一次性替换整个激活集合。
4. **支持同时激活多个角色。** 对每个被选角色先解析唯一最顶级角色，再纳入该根
   角色的全部后代角色和所有权限；多个根族权限按确定性代数合并。
5. **同一 APP 下互斥根角色不能同时激活。** 这会造成 APP 授权上下文歧义，系统
   在变更前校验 DSD/互斥约束，并原子拒绝整个集合，不做部分成功。
6. **无独立测试模块。** `contract/core/starter/gateway-adapter/admin` 各自在
   `src/test` 下维护单元或模块集成测试，不创建 `rbac3-test`。

## 二、模块与依赖边界

| 模块 | 职责 | 强制边界 |
| --- | --- | --- |
| `egon-cola-platform-rbac3-contract` | 稳定 DTO、枚举、Manifest、Decision 合同 | 不依赖 Spring 运行时和持久化 |
| `egon-cola-platform-rbac3-core` | 角色图、激活算法、权限代数、约束规则 | 纯 Java，不访问 I/O/HTTP/Redis/JPA |
| `egon-cola-platform-rbac3-starter` | 业务服务侧 PEP、JWT 校验、Session 快照读取 | 不依赖 Admin，只消费合同与 Core |
| `egon-cola-platform-rbac3-gateway-adapter` | Gateway 热路径认证与授权 | 不通过 HTTP 调 Admin，不访问 SQL |
| `egon-cola-platform-rbac3-admin` | 控制面、认证、持久化、Worker、DDC/Gateway 集成 | 仅服务端使用，不被 Starter 引入 |
| `egon-cola-platform-rbac3-react-sdk` | 类型化认证状态和 UI 接入能力 | Access Token 仅进程内存保存 |
| `egon-cola-platform-rbac3-admin-web` | 权限过滤后的管理台 | 静态 Vite SPA，仅使用本地组件注册表 |

## 三、关键执行链路

```text
不可变 Resource Manifest
  -> 校验/影响分析/幂等激活
  -> 角色、权限、约束、分配变更
  -> Mutation Journal + Fail-Closed Fence
  -> PostgreSQL Worker 领取任务
  -> 生成不可变授权快照并原子发布到专用 Redis
  -> Fence 开放
  -> Starter / Gateway 校验 JWT 与精确版本
  -> Function + Data + Field + Participation 决策
```

```text
RBAC3 Admin Definition Report（DDC v3 bizCode + appCode）-> Gateway Admin
RBAC3 Admin HTTP_PROVIDER Lease（同一 bizCode + appCode）-> DDC
Gateway Release -> DDC 配置投影 -> Gateway Engine
Gateway Engine -> 从 DDC 获取 RBAC3 实例 -> 路由请求
```

Definition、Lease、Release 是三项独立状态，任何一项未知或不一致都不能被合并解释
为“可路由”。进程存活也不等于 Gateway 已可路由。

## 四、DDC 配置 scope、服务 scope 与 Gateway 文档中心

配置 scope 与服务 scope 是两个不同的身份空间：

- 配置 scope 为 `bizCode + appCode + env + namespace + configKey`。RBAC3 以
  `CONFIG_CLIENT` Lease 拉取五个运行策略值，只接受通过校验且版本单调递增的快照。
- 服务 scope 为 `bizCode + appCode + env + namespace + serviceKind + protocol +
  serviceName + group + version`。RBAC3 以独立的 `HTTP_PROVIDER` Lease 注册服务，
  Gateway 从该 scope 获取未过期实例并路由到 advertised host/port。

两类 Lease 可以使用同一个 Instance ID，但 Lease 凭据和状态互相独立。启动时必须先
取得 `CONFIG_CLIENT` Session 并达到 `READY`，再由 RBAC3 发布门闩把根 HTTP Server
发布为 `HTTP_PROVIDER`。Definition 上报与两类 Lease 也相互独立。Spring MVC Mapping
和现有 Gateway 注解共同生成 Gateway Interface Catalog；它是唯一接口文档中心。
Gateway Release 必须由操作者显式发布，RBAC3 不自动发布 Release。

| DDC Key | 默认值 | 合法范围 |
| --- | ---: | ---: |
| `rbac3.access-token-ttl-seconds` | 900 | 300..1800 |
| `rbac3.refresh-token-ttl-seconds` | 604800 | 86400..2592000 |
| `rbac3.session-idle-timeout-seconds` | 1800 | 300..28800 |
| `rbac3.session-absolute-timeout-seconds` | 43200 | 3600..86400 |
| `rbac3.maximum-active-roots` | 16 | 1..32 |

关系约束为 `Idle <= Absolute <= Refresh`。跨 Key 发布不是事务：扩大窗口时先发布
Refresh，再发布 Absolute，最后发布 Idle；收缩窗口时先发布 Idle，再发布 Absolute，
最后在关系允许时调整 Refresh。Access TTL 与最大根数互相独立，但仍应一次只变更
一个 Key。

合法更新通过不可变 Snapshot 原子替换，只影响新签发的 Access/Refresh Token、
新创建或刷新的 Session，以及之后执行的角色激活命令；不会追溯改写已签发 Token、
已固化 Session 到期时间或已提交的激活集合。非法值产生 FAILED ACK，但继续使用
Last-Known-Good Policy 和旧版本/Checksum；恢复必须发布更高的合法版本。

运维必须分别观察五项事实：DDC Config Client、Gateway Definition、未过期的 DDC
HTTP Provider Lease、显式 Gateway Release/Engine Consistency、真实 Routed Request。
状态与指标只暴露版本、状态、指纹和错误码，不暴露配置原值、Lease 凭据、密码、
Token、私钥、Hash 或首个管理员 Bootstrap Secret。

## 五、角色激活规则摘要

- 输入是 Session 的**完整目标角色集合**，不是增量追加；必须携带预期 Session
  版本，服务端在 Session 行锁内完成 CAS。
- 子角色输入先映射到唯一顶级根；一个角色存在多个根、出现环或超过深度上限时
  直接拒绝。
- 每个根展开完整后代族；同一族重复输入被归一化，结果与输入顺序无关。
- 一个 Session 可以激活多个 APP 的多个根，也可以激活同 APP 中没有互斥关系的
  多个根；同 APP 的互斥组合被拒绝。
- 激活前验证 Assignment 证据、DSD、先决角色、禁用/过期状态和最大激活根数。
- 激活成功后生成新的不可变快照并重签 Access Token；响应不确定时客户端用
  `GET /auth/role-activations` 恢复，而不是盲目重放。

## 六、权限决策摘要

最终决策按固定顺序执行：身份与 Tenant/APP 边界、Session/User/Tenant/Policy
精确版本、Fence、Function Permission、Data Scope、Field Rule、Participation 与
Operation SOD。任何必需数据缺失、版本不一致、Redis/密钥不可用或规则无法解析，
均 Fail Closed。

权限合并满足交换律、结合律和幂等性；字段规则按稳定键排序，敏感字段默认
`NONE`，不能因规则缺失自动放宽。Gateway 热路径只做一次决策，不访问 PostgreSQL，
也不回调 Admin HTTP 接口。

## 七、首次管理员初始化

首次部署只允许使用 Admin 制品内的 one-shot CLI，不提供创建首个管理员的 HTTP
接口。命令自动选择 non-web Spring Context，成功或失败后都会退出，不启动 HTTP
Server：

```bash
java -jar egon-cola-platform-rbac3-admin.jar \
  bootstrap-platform-admin \
  --tenant-code platform \
  --username <username>
```

密码必须为 12～64 个字符，只从标准输入读取。部署脚本可以把受控 Secret FD
重定向到标准输入；禁止把密码写入 argv、环境变量、普通配置或日志。CLI 在一个
事务中取得 PostgreSQL Advisory Lock，创建平台 Tenant、`rbac3-system` APP、内置
权限、`ROLE_PLATFORM_ADMIN`、User、Credential、Assignment、Audit 与 Outbox。
已有有效平台管理员或同名平台 Tenant 时命令会拒绝；管理员遗失必须使用独立恢复
runbook，不能重跑初始化命令静默创建第二个 root 账号。

## 八、构建与验证

要求 Java 21、Maven Wrapper，以及模块 `.node-version` 指定的 Node 24。

```bash
./mvnw -B -ntp \
  -pl :egon-cola-platform-rbac3-contract,:egon-cola-platform-rbac3-core,:egon-cola-platform-rbac3-starter,:egon-cola-platform-rbac3-gateway-adapter,:egon-cola-platform-rbac3-admin,:egon-cola-platform-gateway-engine \
  -am clean verify

cd egon-cola-platforms/egon-cola-platform-rbac3
npm ci
npm run typecheck
npm test -- --run
npm run lint
npm run build
npm run e2e --workspace @egon-cola/rbac3-admin-web -- --list
```

上述 E2E 命令只列出场景，不打开浏览器。仓库不会自动启动项目、Gateway、DDC、
PostgreSQL、Redis 或前端服务。

验证脚本默认不执行外部访问：

```bash
scripts/verification/verify-static.sh --verify
scripts/verification/verify-local-dependencies.sh --check-config
scripts/verification/prepare-rbac3-fixture.sh --check-config
scripts/verification/verify-gateway-ddc-topology.sh --check-config
scripts/verification/cleanup-rbac3-fixture.sh --check-config
```

真实拓扑验证要求操作者预先启动两个 Admin 实例，并显式提供不同端口、Instance ID、
Build ID、Snowflake machine-id、DDC/Gateway 地址、Release ID 和专用 Tenant。脚本在
故障切换点暂停，由操作者改变外部状态；脚本本身不停止进程。

## 九、文档入口

- [架构、算法与设计模式](docs/architecture.md)
- [API 与 Manifest 合同](docs/api-and-manifest.md)
- [运维手册](docs/operations-runbook.md)
- [安全与信任边界](docs/security-boundaries.md)
- [验证证据模板](docs/verification-evidence-template.md)

CI/单测/静态扫描只能证明源码和隔离环境内的行为，不能冒充用户实际部署中的
PostgreSQL、Redis、DDC、Gateway 或多进程拓扑证据。真实验证结果必须按证据模板
记录环境、命令、退出码、Release、Schema、Redis 前缀和清理结果。
