# Egon-COLA 架构审计与整改规格

状态：**已确认，实施中**——§9 七项决策点已评审通过（全部采纳建议方案，见 §9 表），整改已启动

审计完成度：**100%**（全部阶段已执行完毕，无未完成审计、无 B/C 级证据残留）

| 阶段                     | 状态 | 产出                                                                 |
|------------------------|----|--------------------------------------------------------------------|
| 类别地图（7 个面）             | 完成 | components / archetypes / gateway / DDC / RPC / gateway 测试与部署 / 治理 |
| 缺口裁决（16 项）             | 完成 | 每项独立审计者先尝试证伪；16/16 确认为真                                            |
| 计划套件复核                 | 完成 | 约 95 条路径核验，19 项缺陷                                                  |
| ShardingSphere 设计/计划复核 | 完成 | 20 条路径核验，13 项缺陷                                                    |
| 对抗性复核（47 条）            | 完成 | 25 条成立、22 条被推翻（其中 20 条系被本次合并修掉）                                    |
| 完整性复核（审计盲区）            | 完成 | 6 类盲区，含阻断级 B-1                                                     |
| 作者亲自复验                 | 完成 | §3 全部 17 项已在 `main@b12592ff` 上逐条 grep 确认                           |

审计规模：73 个只读 Agent、约 310 万 token、1313 次工具调用、两轮执行（第一轮因会话额度中断，
第二轮从缓存恢复并补齐全部失败阶段）。

审计日期：2026-07-26（初审基线 `main@bff002cd`，结论已按 `main@a58d7645` 重标）

当前基线：`main@b12592ff`（§3 全部 17 项已在此提交上复验仍然成立；相关代码事实自
`a58d7645`「Merge branch 'codex/gateway-ddc-rpc-integration'」以来未变；其后的提交为文档收口与 components 能力整改，不触及本规格所列位置）

被审设计：`2026-07-26-gateway-ddc-rpc-integration-remediation-design.md` + 其 7 份实施计划

姊妹设计：`2026-07-26-components-capability-hardening-design.md`（components 类别，**本规格不重复其范围**）

---

## 0. 结论摘要

**审计期间代码基线发生了变化。** 审计启动时 `main@bff002cd`，`codex/gateway-ddc-rpc-integration`
是一条未合入的分支；审计进行中该分支被合入（`a58d7645`），同时 ShardingSphere archetype 改造
的 4 个提交直接落在 main 上。本规格已按合并后的事实重标结论。

三个问题，三个回答。

**设计满足要求吗？** 满足。设计第 4 节列出的 **16 项缺口，逐条复核后 16 项全部被代码证据确认**
（1 项 P1-04 问题为真但陈述不准）。所有补救方案都被判定为合理、最小、可实施，无一项过度设计。
设计拒绝 Saga/2PC、Strategy/Chain、Gateway 专用 DDC Bundle API 的决策是正确的。

**实现合理吗？** 合理，且已完成合入。7 份计划全部执行完毕（compose.demo.yml、demo/、scripts/、
`docs/developer-integration*.md`、`Dockerfile.test-app` 均已存在）。抽查表明实现**独立命中了审计
提出的多数设计欠定义点**——`normalizedStatus()` 正确地加在了 starter 的 record 上、Provider
自动装配做了 server namespace 过滤、published 指针从 version 表取值、chunk 清理是选择性的。

**那还有什么问题？** 有，而且现在是本规格的主体。

> **更正（2026-07-26，实施阶段）：原 §3.0 的阻断级发现 B-1 已被实测推翻，本规格已撤销该条。**
> 详见 §3.0。教训记录在 §7.4。

三类：

1. **17 项在合并代码中仍未闭合**（§3；原阻断级 B-1 已撤销，同时新增 M-11）。其中两项是设计明确承诺、却**没有任何计划任务认领**
   的整章：
   §13.5 Compose readiness 和 §17 可观测性与脱敏。新建的 `compose.demo.yml` **零 healthcheck、
   7 处 `service_started`**——它恰好带着设计要消灭的那个竞态发布了。
2. **计划套件本身有 19 个缺陷**（§4），包括 3 处错误文件路径、20/25 条定向测试命令会因错误原因失败、
   以及覆盖矩阵对 3 个 P2 行的过度声明。这些计划已经被执行过了。
3. **同一个文档漂移模式出现了三次**（§6）。gateway 7 份计划 0/160 勾选、ShardingSphere 计划
   0/39 勾选且 spec 仍写"等待实施"、全仓 62 份计划仅 3 份有勾选——**而三处的代码都已经写完并合入**。

### 严重度分布

| 来源                       | 高 |  中 | 低 | 合计 |
|--------------------------|--:|---:|--:|---:|
| 合并后仍未闭合（§3）              | 4 | 11 | 2 | 17 |
| 审计自身盲区（§7.3）             | 1 |  4 | 1 |  6 |
| 计划套件缺陷（§4）               | 3 |  8 | 8 | 19 |
| ShardingSphere 设计/计划（§5） | 3 |  4 | 6 | 13 |
| 治理与 archetypes（§6）       | 3 |  7 | 5 | 15 |
| 范围外缺陷复核后仍成立（§7.1）        | 1 |  2 | 0 |  3 |

**另有 22 条类别映射结论在对抗性复核中被推翻**（§7.2）——其中 20 条正是被这次合并修掉的，
它们构成"合并闭合了什么"的独立证据。

components 类别（原 C-01..C-07）**已从本规格删除**，全部由姊妹设计
`2026-07-26-components-capability-hardening-design.md` 覆盖，且其覆盖更深（M1-M7 七类模式）。
两份文档不重叠。

---

## 1. 审计方法与证据强度

七个类别地图 + 16 个缺口审计者（每人被要求先尝试证伪）+ 计划套件复核 + ShardingSphere 复核，
合计 67 个只读 Agent、约 2.67M token、1130 次工具调用。全部结论带 `路径:行` 证据。

**未执行**：任何构建、测试、容器。所有"当前行为"来自代码阅读，不来自运行观察。因此以下为
**未验证边界**：真实 Redis Cluster 的 CROSSSLOT（按 CRC16 算术推导）、镜像内 curl/wget 是否存在、
`gateway-live` 套件实际通过率、ShardingSphere 生成项目在真实 PostgreSQL 上的行为。

| 级别           | 覆盖                                              | 复核方式                                     |
|--------------|-------------------------------------------------|------------------------------------------|
| **A｜双阶段复核**  | §2 全部 16 项缺口裁决                                  | 独立审计者先尝试证伪再判断补救                          |
| **A｜作者亲自核验** | §3 全部 17 项、§6 的三处勾选状态、当前 git 合并状态               | 我直接 grep/读文件确认，证据见各条                     |
| **A｜专项复核**   | §4 计划套件（约 95 条路径核验）、§5 ShardingSphere（20 条路径核验） | 专职复核者，带证据                                |
| **A｜对抗性复核**  | 47 条类别映射结论                                      | 每条由独立复核者**按当前 HEAD 尽力证伪**：25 条成立、22 条被推翻 |

**全部级别均为 A。** 对抗性复核已完成，无 B/C 级残留。

**22 条被推翻的结论极具信息量**：其中 20 条不是"审计判断错误"，而是**类别映射跑在合并前的
`bff002cd`、复核跑在合并后的 `a58d7645`——合并本身修掉了它们**。被推翻清单见 §7.2，它同时是
"这次合并到底闭合了什么"的独立证据。另 2 条（components 跨组件耦合、BOM 独立版本）是在事实成立
的前提下**结论不成立**：复核者认定它们不构成缺陷。

§3 的每一条我都在 `b12592ff` 上亲自 grep 验证过，且凡进入对抗性复核的都判定为成立，可直接执行。

---

## 2. 设计裁决（历史基线 `main@bff002cd`）

16 项缺口**全部确认为真**。完整证据见附录性质的逐条记录，此处只列裁决与要点。

| 缺口                            | 裁决              | 要点                                                                              |
|-------------------------------|-----------------|---------------------------------------------------------------------------------|
| P0-01 Gateway→DDC 请求无效        | 确认              | 真实发布必然在 `requireUuidV7` 第一步失败                                                   |
| P0-02 服务状态不一致                 | 确认              | 真实 DDC 下**无任何 Provider 可路由、无任何 Gateway Slot 被连接**                               |
| P0-03 DDC 容器不可执行              | 确认              | thin JAR 无 `Main-Class`，实测确认                                                    |
| P0-04 联调进程连错 Redis            | 确认              | 两个 engine 开了 DDC 却无 Redis 环境变量，回退 `127.0.0.1:6379`                              |
| P0-05 HTTP Provider 不可消费      | 确认              | provider-runtime 无任何 Spring 类型                                                  |
| P1-01 只记录 activation changeId | 确认（**严重度被低估**）  | chunk 中途崩溃留下 `PUBLISHING + change_id NULL`，`recoverable()` 永远选不到                |
| P1-02 draft/published 混用      | 确认（**比设计更广**）   | 未发布的 draft 编辑本来就会在一个 reconcile 周期内静默扩散                                          |
| P1-03 Redis 发布非原子             | 确认              | 三次独立操作，失败只标 DB，Redis 半写无补偿                                                      |
| P1-04 Redisson Bean 串线        | **部分确认（陈述需修正）** | 实质为真；但 rate-limit Bean 是属性开关而非类型条件（它是压制者不是被压制者），registry 消费者按类型注入而非固定 qualifier |
| P1-05 Redis Cluster 跨槽        | 确认              | registry 六键实测槽位全不同                                                              |
| P1-06 ACK 无重试                 | 确认（**比设计更差**）   | 日志**不含异常本身**，且吞掉服务端业务拒绝                                                         |
| P1-07 chunk 顺序/生命周期           | 确认（**严重度被低估**）  | 默认 `failFast=true` 下不是"丢失激活机会"而是 **engine 启动失败**                                |
| P1-08 RPC 重试/错误分类             | 确认              | 字面量 `"provider"` 的唯一写入者是测试 mock                                                 |
| P2-01 DDC 管理面安全               | 确认（**比设计更差**）   | 签名默认 `false`、传输默认 plaintext——**默认配置下连 openapi 都不鉴权**                            |
| P2-02 Compose 与真实测试           | 确认              | 全 `service_started`，Java 服务无 healthcheck                                        |
| P2-03 live IT 覆盖面与文档          | 确认（10 条子断言全复现）  | 两 engine 共用 spec name 导致日志互相截断；`5.2.1` 硬编码在**两处**                               |

**结论**：设计的问题诊断准确，无一项虚构，且在 P1-01/P1-02/P1-06/P1-07/P2-01 五处**低估了自己
发现的严重度**。合并已闭合其中大部分——但不是全部。

---

## 3. 合并后仍未闭合的 17 项（行动清单）

**这是本规格现在最重要的一节。** 每条我都在 `main@b12592ff` 上亲自复验过。

### 3.0 【已撤销】原阻断级发现 B-1 —— 实测推翻

> **状态：撤销。原判断错误，DDC Admin 可以正常启动。**

原结论称：`DdcManifestController` 的 `@Value("${egon.cola.component.ddc.admin.manifest.version}")`
无默认值，而 `application.yml` 把它接到 `${sdk.version}`，且 `sdk.version` 无人提供，因此上下文
启动失败。

**实测结果（`DdcAdminContextSmokeTest`，本轮新增）：三个断言全部通过，
`manifest.version` 解析为 `5.2.3`，`sdk.version` 同样解析为 `5.2.3`。**

**我漏掉的证据**——`application.yml:17-18`：

```yaml
spring:
  config:
    import: classpath:META-INF/egon-cola-ddc.properties
```

admin 自己的 `application.yml` 用 `spring.config.import` 把 starter 模块那份 **Maven 过滤过的**
`META-INF/egon-cola-ddc.properties`（内容 `sdk.version=${project.version}`）导入成了正规 Spring
配置源。所以 `sdk.version` **确实是 Spring 属性**，链路完整：
Maven 过滤 → starter jar 内的 properties → `spring.config.import` → Environment → `@Value`。

原分析中"没有任何机制把该文件注入 Environment"的排除是错的：我检查了
`spring.factories`、`EnvironmentPostProcessor`、`@PropertySource`、`build-info`，**唯独没检查
`spring.config.import`**。

连带撤销：原"`GatewayLiveTopologyIT` 现在应当是红的"推论同样不成立——它不会因此失败。
G-1（live IT 不在 push/PR 上运行）本身仍然成立，但**不能**再用"已经造成一次实际漏网"来佐证。

**保留下来的产物**：`DdcAdminContextSmokeTest` 仍然有价值并已合入本次改动。它把上面那条链路
钉死——一旦有人删掉 `spring.config.import`、改键名、或让 starter 丢掉资源过滤，
`everyShippedPropertyResolves` 会立即失败。这条链路目前**只由这一个测试守护**。

**顺带确认的两个真实事实**（原分析中正确的部分）：

1. admin 测试树此前确实没有任何 `@SpringBootTest`；`DdcAdminSecurityIntegrationTest.java:59`
   确实覆盖了 `manifest.version`，`DdcManifestControllerTest` 确实是 `@WebMvcTest` 切片。
   只是这层"看不见"没有掩盖真实缺陷。
2. **`application-test.yml` 无法启动完整 admin 上下文**：它关掉 Redis
   （`admin.redis.enabled=false`），但 `DdcAdminRedisConfig` 里 `DdcRedisRepository`、
   `DdcConfigLeaseService`、`DdcServiceRegistryService`、`DdcLeaseExpiryScanner` 等全部
   `@ConditionalOnBean(name="ddcAdminRedissonClient")`，而 `DdcCacheService`/`DdcPublishService`
   强依赖它们。这就是新测试只加载配置装配、不加载完整 Bean 图的原因。这是一个**新的中级发现**，
   记为 M-11。

### 3.1 高

**H-1｜设计 §13.5（Compose readiness）无任何计划任务认领，且新建的 demo 拓扑带着原竞态发布**

设计承诺给 DDC、Gateway Admin、Engine 加 healthcheck 并依赖 readiness 而非 `service_started`。
七份计划中**没有任何一步**提及 Compose 的 healthcheck/readiness，而索引的覆盖矩阵却把它算在
`P2 Compose | 07/Task 1-4` 里。实测：

```text
compose.yml       healthcheck: 4 处（全是 postgres/redis/kafka 基础设施）  service_started: 4 处
compose.demo.yml  healthcheck: 0 处                                      service_started: 7 处
```

`compose.demo.yml` 是本轮**新建**的文件，它零 healthcheck。设计要消灭的启动竞态，在新交付物里
原样复现了。

**H-2｜设计 §17（可观测性与敏感信息）零任务、零矩阵行**

§17 列了 7 类必须输出的结构化日志/指标维度（releaseId/attempt/phase/changeId/configKey、
DDC expected/target version 与 dispatch/replay 次数、ACK 重试/队列饱和/最终耗尽、legacy/v2 回退
次数、chunk GC 数量与被保护 release、Provider/Gateway lease 重注册、HMAC credential id/scope
拒绝/重放拒绝）和 3 条禁令（禁止记录 configValue/chunk Base64/完整规则正文；禁止记录 credential
secret/JWT/HMAC secret/数据库密码；禁止记录完整敏感 cache diff）。

**没有任何计划任务拥有这一章，覆盖矩阵里也没有对应行。** 七份计划的关键词扫描只有三处顺带提及。
这一章带有安全义务，无人认领意味着无人验证。

关联风险：journal 的 V4 迁移里有 `content_value TEXT NOT NULL`（第 9 行），即**完整规则正文
落库存储**。设计 §7.1 的列清单里没有这一列（只有 `contentSha256`），所以这是一个未经设计评审的
数据静态存储决定，且正好落在 §17 禁令的相邻语义上（禁止记录 ≠ 禁止存储，但两者应一起裁定）。

**H-3｜`ddcAdminRedissonClient` 缺 `destroyMethod = "shutdown"`，Admin 关闭时泄漏 Redisson 客户端**

设计 §9 要求四个 Bean **全部**使用 `destroyMethod = "shutdown"`。计划 01 Task 2 的 Interfaces
只列了三个，Files 里完全没有 `DdcAdminRedisConfig.java`；唯一碰这个文件的计划 06 Task 2 只说
"消费"它。实测 `DdcAdminRedisConfig.java:32`：

```java
@Bean("ddcAdminRedissonClient")                              // ← 无 destroyMethod
@ConditionalOnMissingBean(name = "ddcAdminRedissonClient")
```

另外三个 Bean 都有。设计 §15.1 还要求一个"四个 Redisson Bean 与用户自定义 Bean 共存"的测试，
按三 Bean 口径写的计划无法满足它。

**H-4｜计划 06 打开 fail-closed 鉴权，但无任务认领部署侧凭据传播**

计划 06 Task 3 引入 `DdcHmacCredential`（accessKey + clientType + appCode/env/namespace 模式 +
allowedOperations），Task 4 让所有未匹配路由默认拒绝。但 06 的 Files 全部在 ddc-admin 模块内，
不含任何部署文件或 Gateway Admin 的 DDC 客户端配置；计划 07 Task 4 的 Files 只有 `.env.example`
和新建的 `compose.demo.yml`，不含既有的 `deployment/compose.yml`。

后果：计划 06 **不是独立可提交的**——按它提交后既有 Compose 拓扑处于鉴权断裂状态，实际是四个
提交之后才重新自洽。

### 3.2 中

**M-1｜journal 缺 `timeout_ms` 列（原 D-01）**

实测 V4 迁移的列：`release_id / attempt_no / phase_order / phase_type / config_key /
content_value / expected_version / change_id / ddc_target_version / ddc_status / error_code /
error_message / created_at / updated_at`——**没有 timeout**。全模块 grep 也没有任何
publication/journal/phase 相关代码引用 `timeoutMs`。

而 DDC 的 changeId 幂等重放校验会比较 timeoutMs，不匹配即抛 `CHANGE_ID_CONFLICT`
（`DdcPublishService.java:638`），设计 §7.1 的约束也明写"重试复用原超时"。当前潜伏是因为
Gateway 超时是固定 10s 常量（`GatewayAdminConfiguration.java:88`）——**一旦这个值改成可配置，
所有跨配置变更的重试都会被 DDC 拒绝。**

**M-2｜N-01 的入口崩溃窗口只收窄了，没有关闭**

好消息：`recoverable()` 已改为 JOIN `gateway_release_publication`，不再要求 `change_id IS NOT NULL`。
坏消息：`beginAttempt` 仍在 `GatewayReleaseService.java:303` 先把状态置为 PUBLISHING，journal 行
由 coordinator 随后写入；而 `hasReleaseInProgress` 仍然阻塞 `CREATED/VALIDATING/READY/PUBLISHING`：

```sql
SELECT count(*) FROM gateway_release
 WHERE gateway_group_id = ?
   AND status IN ('CREATED','VALIDATING','READY','PUBLISHING')
```

于是：崩溃发生在 `beginAttempt` 与第一条 journal 行之间 → 该 release 状态为 PUBLISHING 但零条
publication 行 → `recoverable()` 的 JOIN 排除它 → **该 group 永久无法发布**。停在 READY 的
release（崩在 beginAttempt 之前）同理，且 READY 根本不在 `recoverable()` 的状态列表里。

窗口比修复前窄得多，但后果完全相同且无上限。

**M-3｜engine 默认 DDC app-code 仍与 publisher 约定不匹配**

`gateway-engine/src/main/resources/application.yml:31` 仍是 `app-code: egon-cola-gateway-engine`，
而 publisher 写入 `gateway-engine-{group}`。只有 compose 覆盖了它——**用默认配置起的 engine
静默订阅错误 scope，永远收不到规则**（会停在 LKG 或空规则）。

**M-4｜版本字面量漂移仍有两处**

`GatewayEngineConfiguration.java:527-528` 仍是两行 `"5.2.3"`，进入注册元数据
（`gateway.engine-version` / `egon.rpc.runtime-version`）；`DtpManifestController.java:15-16` 仍有
两处 `5.2.1`（`@Value` 默认值 + 字段初始值）。RPC starter 已经用 Maven 过滤资源做对了，DDC manifest
本轮也修好了（现在是无默认值的属性注入）——**只有这两处漏了**。两者都经对抗性复核确认成立。

**M-5｜ACK 重试的错误分类缺服务端配套（原 D-21）**

未见服务端改动。DDC admin 仍把业务失败返回为 HTTP 200 + failure `ResultDto`；ACK 拒绝抛的是
`DdcAdminException`，而全局 handler 只捕 `DdcException`，于是 lease mismatch / checksum mismatch
被兜底压成通用 `INTERNAL_FAILURE(56999)`——**与真正可重试的瞬时失败不可区分**。"重试 5xx 不重试
4xx"在这个 wire 合同下不可实现。

**M-6｜v2 键迁移的 revision 计数器连续性未处理（原 D-17）**

未见 seeding 逻辑。`serviceRevision`/`catalogRevision` 是 INCR 计数器，换到 v2 键会从 0 重新开始；
跨迁移比较 revision 的客户端会错序或忽略更新。设计的双写只覆盖 config value/version。

**M-7｜部署侧发的是"全通配"HMAC 凭据，把 fail-closed 抵消了**

P2-01 的 scope 化凭据确实实现了（`DdcHmacCredential` / `DdcHmacCredentialRegistry` 存在）。但
`deployment/compose.yml:80-89` 给 DDC Admin 配的那把凭据，**每一个 scope 维度都是 `*`**：
`DDC_OPENAPI_CLIENT_TYPE`、`DDC_OPENAPI_APP_CODE_PATTERNS`、`DDC_OPENAPI_ENV_PATTERNS`、
`DDC_OPENAPI_NAMESPACE_PATTERNS`、`DDC_OPENAPI_ALLOWED_OPERATIONS` 全是 `*`，且复用紧邻上方那对
legacy 静态密钥。默认拒绝的机制在，但被这把凭据在部署层面还原成了"全放行"。这比 H-4 更进一步：
不是"没人认领部署传播"，而是**传播了，传播成了空**。

**M-8｜8 个 `@Scheduled` 作业、零 leader election，而 HA 拓扑刻意跑双副本**

全仓 grep `shedlock`/leader election/`LeaderLatch` **零命中**，但两个 admin 都带无保护的定时任务：
Gateway Admin 有 `GatewayReleaseReconciler:59`、`GatewayRuleChunkGarbageCollector:62`、
`GatewayDefinitionLifecycleReconciler:75`、`GatewayObservabilityRetentionReaper:15`、
`GatewayHmacNonceReaper:20`；DDC Admin 有 `PublishTimeoutScanner:50`、`DdcLeaseExpiryScanner:52`、
`PublishStartupRecovery:68`。而 `compose.ha.yml` 同时定义 `ddc-admin`+`ddc-admin-2`、
`gateway-admin`+`gateway-admin-2`。§7.1 的发布互斥只是这个风险类里的一个实例——**整类没有机制**。
决策点 §9.6 应扩大到覆盖全部 8 个作业，而不只是发布互斥。

**M-9｜journal 每次 attempt 重存一份完整规则正文，且无回收**

`content_value TEXT NOT NULL` 按 `(release_id, attempt_no, phase_order)` 存储完整 chunk/activation
载荷，而 `gateway_release_content` 已经存了 `chunk_manifest`/`activation_content`——**同一份正文
被复制两处**，且每次重试再存一遍。`gateway_release_publication` 没有任何 reaper。设计 §7.1 的列
清单里只有 `contentSha256`，审计对 P1-01 的分析也明确假设"正文从 `gateway_release_content` 取"。
与决策点 §9.1 一并裁定。

**M-10｜DDC V4 回填是无保护的全表 UPDATE，且全仓没有回滚方向**

两个方言的 `V4__add_published_config_pointer.sql` 都是
`alter table ... add column published_version ...` + 无条件
`update ddc_config_item set published_version = current_version;`——无 `NOT NULL`、无 `DEFAULT`、
无 `where deleted = false`。同时全仓**没有任何 undo/down 迁移**，runbook 也没有"schema 前进、
代码回退"的步骤。发布回滚场景未被设计覆盖。

**M-11｜`application-test.yml` 无法启动完整 admin 上下文（新增，实施阶段实测）**

该 profile 关掉 Redis（`admin.redis.enabled=false`），但 `DdcAdminRedisConfig` 中
`DdcRedisRepository`、`DdcConfigLeaseRedisRepository`/`DdcConfigLeaseService`、
`DdcServiceRegistryRedisRepository`/`DdcServiceRegistryService`、`DdcLeaseExpiryScanner`
全部 `@ConditionalOnBean(name = "ddcAdminRedissonClient")`，而 `DdcCacheService` 与
`DdcPublishService` 是强依赖。实测启动依次报
`No qualifying bean of type 'DdcRedisRepository'` → `'DdcConfigLeaseService'`。

后果：仓库里**没有任何可用的"启动完整 admin"测试路径**，也就无法用测试覆盖跨 Bean 的装配回归。
建议二选一：(a) 给 `test` profile 提供内嵌/Testcontainers Redis；
(b) 把这些 Redis 依赖改成可缺省（`@ConditionalOnMissingBean` 提供 no-op 实现），
让 admin 在无 Redis 时能以降级模式启动。

### 3.3 低

**L-1｜legacy failure-stage 值处理未定义（原 D-39）** — 现网 gateway 把错误码写进
`x-egon-rpc-failure-stage`，新 consumer 若严格要求 `stage==GATEWAY` 对旧 engine 永不 failover。
未见前缀/大小写兼容规则。

**L-2｜历史遗留 release 行无终结策略（原 D-03）** — V4 迁移与 reconciler 中未见 legacy 处置分支。
修复前的 `gateway-release-*` changeId 行不会有 journal，新恢复逻辑处理不了。实际风险低（DDC 本来
就拒绝非 UUIDv7，不可能存在成功的历史发布），但会永久占用 `hasReleaseInProgress`。

### 3.4 已确认吸收（无需行动）

抽查确认实现独立命中了这些审计点，**优于计划书面口径**：

| 项                                                                                   | 证据                                                                         |
|-------------------------------------------------------------------------------------|----------------------------------------------------------------------------|
| `normalizedStatus()` 加在 **starter 的** `DdcServiceInstance` 上（不只是 management-client） | `DdcServiceInstance.java:38`                                               |
| Provider 自动装配按 server namespace 过滤（避免 management 端口二次触发）                            | `GatewayHttpProviderAutoConfiguration.java:73-75`                          |
| published 值从 version 表取（决策 §8.2 已由实现选定方案 a）                                         | `DdcConfigService.java:267,276,366`                                        |
| chunk 清理是选择性的，非全量 `clear()`                                                         | `GatewayRuleChunkStore.java`                                               |
| Demo 的构建上下文已解决                                                                      | `deployment/Dockerfile.test-app` 已存在                                       |
| WebFlux Provider 模块已建                                                               | `egon-cola-component-gateway-test-webflux-http-provider`                   |
| 七份计划全部执行完毕                                                                          | `compose.demo.yml`、`demo/`、`scripts/`、`docs/developer-integration*.md` 均存在 |

---

## 4. 计划套件复核结果（19 项）

复核范围：1 份设计 + 1 份索引 + 7 份计划（31 个任务），核验约 95 条路径。

**结构性结论**：四条最终验证的 `-pl` reactor 路径全部正确，`-Pgateway-live` 真实存在且被
test-suite 子模块继承，failsafe 版本在 `egon-cola-components/pom.xml:375-376` 受管——**门禁命令
是可执行的**。迁移文件名与设计完全一致。执行顺序（01 → {02,04,05,06} → 03 在 02 之后 → 07）
无环，各计划的"依赖 Integration 0X"声明自洽（计划 06 除外）。

### 4.1 高

- **P-1｜§13.5 无覆盖**（同 H-1）。索引矩阵却声称 `07/Task 1-4` 覆盖。
- **P-2｜§17 无覆盖**（同 H-2）。零任务、零矩阵行。
- **P-3｜计划 06 缺部署传播**（同 H-4）。

### 4.2 中

- **P-4｜25 条定向测试命令里 20 条会因错误原因失败。** 每个 Step 2 都是
  `./mvnw -pl <module> -am test -Dtest=<X>`，而 Surefire 3.x 的 `failIfNoSpecifiedTests` 默认为
  true，全仓无任何 POM 覆盖它。配合 `-am`，上游有测试类但无匹配的模块会以
  `No tests were executed!` 中止 reactor，**在预期的断言失败被观察到之前**。受影响的上游包括
  common-core（3 个测试类）、management-client（6）、gateway-core（10）、gateway-contract（4）。
  计划作者显然知道这件事——01/T3、01/T4、02/T1、02/T2、02/T3 这 5 条带了
  `-Dsurefire.failIfNoSpecifiedTests=false`，其余 20 条没带，包括计划 03 之后的每一个 Step 2。
- **P-5｜计划 07 Task 1 的四个 live fixture 文件路径错了包。** 列为
  `.../gateway/test/live/` 下的 `GatewayProcessSpec/GatewayProcessHarness/GatewayTestInfrastructure/
  GatewayProcessHarnessTest`，在基线和 HEAD 上都位于 `.../gateway/test/process/`。照做会在 `live/`
  下新建四个重复类而不是重构既有 harness。（同任务的两个 `Create` 条目路径正确。）
- **P-6｜计划 03 Task 3 引用了不存在的 `admin/config/` 子包。** 实际类在
  `.../gateway/admin/GatewayAdminConfiguration.java`；`admin/config/` 下只有
  `GatewayAdminProperties.java`。照做会新建一个竞争的 `@Configuration`。
- **P-7｜§9 四 Bean 合同只覆盖 3/4**（同 H-3），矩阵行 P1-04 不准确。
- **P-8｜计划 01 与 05 同时拥有 `egon-gateway-rpc` 默认值。** 两者都改 `EgonRpcProperties.java`、
  都建/改 `EgonRpcPropertiesTest.java`。索引要求 01 先于 05，所以 05/T4 的那半边在开始时**已经是
  绿的**，"先观察失败"的证据不可获得。
- **P-9｜计划 04 Task 3 的 YAML 片段用了无法解析的占位符。** 写的是
  `${gateway.reporting.artifact-version}`，但该属性不存在；同任务下两行的散文说的是
  `egon.cola.component.gateway.reporting.artifact-version`。照抄会在启动时占位符解析失败——正好
  打破计划 04 顶部"上报与注册版本必须一致"的约束。
- **P-10｜计划 03 Task 1 自相矛盾三处。** (a) Step 1 调 `store.insert(List.of(...))`，同任务
  Step 3 的 Store API 声明是 `insertAll(...)`；(b) Step 1 的 `PublicationRecord` 传 14 个位置参数
  （对应设计 §7.1 的 14 字段），但 Task 3 Step 3 要求持久化 `configKey/内容哈希/内容/UUIDv7`
  即第 15 个 `contentValue`——实际 record 就是 15 个，V4 也确有 `content_value TEXT NOT NULL`；
  (c) Step 3 对 V4 的描述只提"非空内容哈希/状态"，从不提内容列。设计 §7.1 同样漏了它，**于是
  "把完整规则正文存进 gateway_release_publication" 这个决定从未被评审过**。
- **P-11｜§4.3 P2 第 8 条（外部 Redis 精确清理）无任务、无矩阵行。** 唯一相关文字是计划 07 的
  全局约束，作用域是 gateway live 套件，而缺口报告的位置是 DDC 测试模块。

### 4.3 低

计划 06 是唯一无上游依赖声明的计划，却与 01/02 共享文件；计划 01 Task 1 的 Interfaces 写
`isOnline` 而测试/实现/散文都是 `isAvailable`；**设计 §12.3 写的 `REGISTERED` 状态在代码里不存在**
（枚举是 `REGISTERED_READY`，计划 05 用对了，是设计错）；设计写基线 `3690c5f1` 而索引写
`d5d53762`；4 个任务的 Files 列表未枚举具体路径，`git add` 无法界定提交边界；计划 07 T5/T6 无
失败测试，与索引全局约束冲突（对文档/审计任务可辩护，但约束写的是无例外）；01/T3 Step 4 硬编码
`...-starter-5.2.3.jar` 文件名；03/T4 Step 4 的 `rg` 扫了 `src/test` 却断言"生产代码无输出"。

---

## 5. ShardingSphere 设计/计划复核（13 项）

**先说最重要的**：这份计划**也已经在 main 上执行完毕**了——`a6394278` / `128ed881` / `7b101ec2` /
`f3e48330`（均 2026-07-26），提交信息与计划 Task 1/2/3 Step 6-7 规定的字面一致。而 spec 头部
仍写 `状态：方案与 Spec 已确认，等待实施`，39 个勾选框 0 个勾选。

路径核验：计划里每个 `Modify:`/`Create:` 目标都存在，每个 `Delete:` 目标都已消失。实现与设计
在结构上是一致的。

### 5.1 高

- **S-1｜状态陈旧**（见上）。Task 4 Step 5（文档收口）从未执行。
- **S-2｜`actualDataNodes` 字面量矛盾，会直接破坏重实现。** 设计的主数据规则和三份表清单都用
  三段式 `master_data.public.users`，计划在 Task 1 Step 1、Task 1 Step 4、Task 2 Step 1 三处重复
  这个字面量并称"就是这个精确规则"。**而所有已发布模板用的是两段式 `master_data.users`**，
  `ShardingTopologyValidator.splitActualDataNode` 按两段比较，三份 `verify.groovy` 也断言两段式。
  照设计重做会直接校验失败。
- **S-3｜被取代的 2026-07-23 计划没有任何取代声明。** 新计划 Task 4 Step 5 要求在旧计划标题下
  插入取代说明，从未做。旧计划的 Architecture 段仍在描述 `app.datasource.mode=SINGLE|...`、
  Spring Boot Hikari+Flyway 管理 SINGLE、以及条件化的 no-op `FlywayMigrationStrategy`，全局约束 3
  还写着"必须保留默认 SINGLE"——**这些描述的代码已经被删除了**
  （`ShardingDataSourceModeCondition.java`、`LogicalDataSourceFlywayMigrationStrategy.java` 均已不存在）。
  两份计划现在互相矛盾，且没有任何标记告诉读者该信哪份。

### 5.2 中

- **S-4｜设计要求 test profile `ddl-auto=validate`，实现用 `none`。** dev/prod 都保留了
  `validate`（针对 ShardingSphere 逻辑 DataSource），而唯一能验证它的 test profile 关掉了——
  **dev/prod 会走的 Hibernate 校验路径零自动化测试覆盖**。
- **S-5｜`test` profile 静默降级为内存 H2**，与设计 §3.1.3"profile 只表示环境"和 §3.2.1
  "配置错误必须在启动时暴露、不得静默回退"直接冲突：三个物理 URL 都有 `jdbc:h2:mem:` 默认值，
  `driver-class-name: org.h2.Driver` 硬编码无 env 覆盖，且 `clean-disabled: false`（在一个以真实
  环境命名的 profile 里开启 `flyway clean`）。
- **S-6｜设计 §17 的 18 条校验规则中，第 11 条（binding tables）未实现。** `ShardingTopologyValidator`
  （644 行，三个 archetype 字节相同）从不解析 `bindingTables`，全包 grep 无 `binding`。模板用户
  删掉 binding 块或配置分片键不同的 binding 对，会静默把单节点 join 变成跨分片笛卡尔路由。
- **S-7｜计划自己的残留审计门禁（Task 4 Step 1，"Expected: no output"）不可能通过**，且与设计
  §19 直接矛盾——设计说"辅助扫描允许 README 的历史说明出现必要文字"，而六份生成项目 README 都
  故意写了 `!SINGLE`。执行者要么削弱 README，要么把失败的步骤标绿。

### 5.3 低

计划的隔离 worktree 约束被违反（三个模板提交直接落在 main 的 first-parent 链上，且与 gateway 分支
时间交错）；`app.sharding.routing` 只在 SHARDING 拓扑文件里声明却被无条件绑定，删掉看似无用的
import 会让 readwrite 模式启动失败；archetype IT 零 PostgreSQL 覆盖（唯一真 PG 路径是 opt-in
profile，且 Flyway 指向未拆分的 location）；三份生成项目都带了没用到的 MySQL SQL 解析器依赖；
`shardingsphere.version` 在三份 pom 里各硬编码一次，无跨 archetype 漂移检查；test profile 重复
声明 `mode` 属性，使 Java 层默认值分支在启动时永不被执行。

---

## 6. 治理：同一模式的三次出现

这是本规格里唯一一个"不是技术缺陷"的发现，但它是复发率最高的。

| 实例                   | 代码状态                         | 文档状态                                |
|----------------------|------------------------------|-------------------------------------|
| gateway/DDC/RPC 七份计划 | 全部实现并合入 `a58d7645`           | **0/160 勾选**                        |
| ShardingSphere 计划    | 全部实现并合入 main                 | **0/39 勾选**，spec 仍写"等待实施"           |
| 全仓 62 份计划            | 含已完成的 transactional-outbox 等 | 仅 3 份有任何勾选；outbox 计划 110 未勾选 / 0 勾选 |

**后果不是记账洁癖。** 任何接手者（人或 agent）读 main 上的计划会认为一件事都没做，从而重做或
冲突修改；被取代的旧计划（S-3）还在主动指导错误方向。112 份 superpowers 文档没有任何顶层
实现-vs-计划索引，plan↔spec 配对也无法机械推导。

其余治理项：

- **G-1（中）** gateway/ddc/rpc 唯一的跨组件集成测试 `GatewayLiveTopologyIT` 被双重门控
  （`gateway-live` profile + `gateway.live.test=true`），启用它的 job 只在 nightly cron 和手动派发
  下运行——**push 和 PR 永远不跑**，包括修改这三个组件的 PR。
- **G-2（中）** `cola-samples/` 为空且未被 git 跟踪（样例在 `c99c5f78` 删除），两份 README 的
  仓库布局仍列出 `light/`、`fable/`、`fable-web/`，且在更晚的 `3690c5f1` 刷新 README 时也没改。
- **G-3（中）** README 版本三重漂移：远程生成示例 5.1.2、本地示例 5.2.3、中文 README 本地示例
  5.2.1。根因是 `bump_cola_version.sh` 只改 POM 和 archetype 模板，而 `maven-deploy.md:78` 声称
  它会同步 README 版本。
- **G-4（中）** `.gitmodules` 声明 `scripts/bash-buddy` 但索引无 gitlink、目录不存在；两个 CI
  仍 `submodules: recursive`，`maven-deploy.md` 的排障表还教用户重新拉取它。
- **G-5（中）** `publish-maven-central.yml` 的 `skip_tests` 默认 **true**，可以不跑任何测试就发布
  到不可变的 Maven Central；无"必须有绿色 CI"的门禁。
- **G-6（低）** 三个父 POM 的 license URL 指向 `blob/master/LICENSE-GPL-2.1`——文件名错
  （实际 `LICENSE-LGPL-2.1`）、分支错（实际 `main`），发布到 Maven Central 的元数据里是死链。
- **G-7（低）** `maven-deploy.md` 的脚本名错（缺 `.sh`）、验证用的 artifact
  `egon-cola-component-dto` 在 reactor 里不存在（上游 COLA 命名残留）。

### 6.1 archetypes 类别（未被姊妹设计覆盖）

- **A-1（高）｜light archetype 的架构检查对任何真实生成的项目都是空转。** 生成项目的 pom 模板把
  `packageMappings` 的键硬编码成集成测试的包名（`archetype-resources/pom.xml:290-296`，
  `<it.pkg.domain..>` 等 7 行），而这个 pom 是**被 Velocity 过滤的**，本应使用 `${package}`。
  任何按文档流程生成的项目（`-Dpackage=top.egon.light`，CI 用 `top.egon.cola.ci.*`）**零个类命中
  任何映射**，插件把所有类解析成 UNKNOWN，默认只 WARN。**已在 `a58d7645` 上复验仍然成立。**
- **A-2（中）｜A-1 长期不可见的原因**：兼容性 CI 对 light/service 只跑 `clean test` +
  `-DskipTests package`，只有 web 跑 `clean verify`（`ci_java_compatibility.yaml:234,237,240`），
  而架构检查插件绑定在 `verify` 阶段——**它从未在真实包名下执行过**。
- **A-3（中）｜类别约定文档与实际强制的分层相反。** `code-style-abstract.md` 与
  `architecture-mermaid-diagrams.md` 规定 `infrastructure → application`、application 持有
  client/port、MyBatis-Plus 仓储链、`facade.api` 包；三个 archetype 及其 IT 强制的是 infrastructure
  只依赖 domain、port 在 domain、MyBatis-Plus 被明确禁止。这两份文档被 README 当作类别规范引用。
- **A-4（低）** 跨 archetype 命名与元数据不一致（light 用 `start` 包 vs service/web 用 `starter`；
  descriptor schema 1.0.0 vs 1.1.0；service/web 用 XML 实体 `&#112;`/`&#97;` 隐藏 application 目录名
  中的字母且无注释说明；只有 service 生成 CI workflow）。
- **A-5（低）** 两个 facade 契约模块不对称：organization-facade 带着零使用的 lombok/jackson 依赖；
  两者返回约定不同（Response 信封 vs 裸 DTO + 方法级约束），而两者都是生成项目模仿的"典范"。

---

## 7. 范围外缺陷：对抗性复核结果

### 7.1 复核后仍成立（3 项，需排期）

- **（高）DDC 发布互斥与完成通知是进程内状态，但 HA 已被设想。** `PublishResourceLockRegistry`
  与 `PublishCompletionWaiterRegistry` 是内存 Map，而 `compose.ha.yml` 已经跑两个 admin，
  `PublishStartupRecovery` 的消息里甚至写了 "HA stale timeout"。2 副本下：两节点都能通过
  tryAcquire 与非串行化的活跃任务检查并在 expectedVersion 上竞争；资源锁只在执行终态迁移的那个
  节点释放，另一节点的锁泄漏。**决策点 §9.6 直接决定这一项做不做。**
- **（中）`GatewayReleaseStateMachine` 是死代码。** 状态迁移已定义并有测试，但服务与 Store 从不
  执行它；状态由裸 SQL UPDATE 改写，非法迁移无守卫，`SUPERSEDED` 从不写入，release 直接以 READY
  插入、跳过 CREATED/VALIDATING。
- **（中）DTP 仍硬编码 `5.2.1`。** 本轮修好了 DDC（`DdcManifestController.java:15` 现在是无默认值
  的属性注入），但 `DtpManifestController.java:15-16` 仍有两处 `5.2.1`（`@Value` 默认值 + 字段
  初始值），项目版本是 5.2.3。与 §3 的 M-4（engine 硬编码 `"5.2.3"`）属同一漂移类，一并修。

### 7.2 复核后被推翻（22 项）

**这一节的价值不在"审计错了"，而在于它独立证明了合并闭合了什么。** 20 项是因为类别映射跑在
合并前基线、复核跑在 `a58d7645`；复核者逐条给出了修复它的提交。

| 被推翻的结论                                       | 推翻理由                                                     |
|----------------------------------------------|----------------------------------------------------------|
| Provider 状态 ONLINE/REGISTERED 不匹配            | `9f7b273d` 已统一状态语义                                       |
| provider-runtime 无自动装配                       | `c79522fa` + `45e4eac4` 已补齐                              |
| DDC Redis value/version/event 非原子            | HEAD 上已走 Lua 原子发布                                        |
| DDC starter Redisson 装配被压制                   | 51 个提交前已修，且有回归测试守护                                       |
| 多键 Lua 无 hash tag（CROSSSLOT）                 | HEAD 已有专门的同槽键实现                                          |
| DDC 管理端点完全无鉴权                                | admin 已引入完整 Spring Security + OAuth2 JWT resource server |
| HMAC nonce 为单进程内存态                           | `b7bb9869` 已改为共享状态                                       |
| Draft 编辑绕过发布协议                               | `56966430`（draft/published 分离）已闭合                        |
| Admin Docker 打包 thin JAR                     | 证据在 HEAD 上已不存在                                           |
| RPC 无幂等契约 / 网关不发 provider stage              | `cc0efb3d` 已分类 gateway 与 provider 失败                     |
| RPC 默认网关服务名不一致                               | 引用行在 HEAD 上已不是该内容                                        |
| RPC slot 心跳失败进终态 FAILED                      | 引用源码在 HEAD 上已不存在                                         |
| RPC NOT_FOUND/UNIMPLEMENTED 分类只能靠 mock       | `UNIMPLEMENTED` 已不再走默认分支                                 |
| 两个 live engine 共用日志/manifest                 | 前提事实不成立，行号也对不上                                           |
| `expectedVersion` CAS 从不使用                   | 已被当前实现推翻                                                 |
| Reconciler 无法处理 chunk 阶段 UNKNOWN + chunk 无清理 | `78ae899a`（chunk 生命周期收敛）之后已不成立                           |
| Release 编排无单元测试                              | 四项断言全部与 HEAD 代码矛盾                                        |
| live 覆盖缺 rollback/LKG/WebFlux                | 计划 07 已交付                                                |
| DISTRIBUTED 限流零覆盖                            | 已有覆盖                                                     |
| components 跨组件强制编译耦合                         | **事实成立但不构成缺陷**（复核者判定）                                    |
| BOM 独立于父继承链需人工同步版本                           | **事实成立但结论不成立**：已有文档化且经验证的同步机制                            |

未进入复核队列的若干低级观察（`@EgonRpcReference` timeoutMs 只能缩短、`restoreLkg` 失败无降级、
子进程密钥在宿主 `ps` 可见、`GatewayTestScope` 死代码、engine 临时目录不清理）保留为提示，
**未经复核，建议动工前各自确认**。

---

### 7.3 审计自身的盲区（完整性复核结论）

一个专职复核者检查了"这次审计漏了什么"。除已并入 §3 的 B-1、M-7..M-10 外，还有四类：

- **（高）七个组件零实现级覆盖。** 本次七个类别只深挖了 gateway/DDC/RPC。完全未做实现级审计的
  有：bytecode（31 个 pom、264 个 java）、transactional-outbox（115）、access-guard（86）、
  dynamic-thread-pool（75）、common（63，9 模块，**所有其它组件的依赖基座**）、rule-engine（52）、
  method-extension（36）——约 690 个 java 文件。**bytecode 是最高杠杆的遗漏**：它同时是运行时
  Java agent 和构建期强制的 Maven 插件（也就是 §6.1 A-1 里失效的那个检查）。姊妹设计
  `components-capability-hardening` 覆盖了能力面，但两者都没做这些组件的深度实现审计。
- **（中）供应链/CVE 风险类完全无工具。** 全仓 pom 对 `cyclonedx`/`dependency-check`/`spotbugs`/
  `checkstyle`/`banned`/`dependencyConvergence` **零命中**：无 SBOM、无 OWASP 扫描、无禁用依赖
  规则、无依赖收敛规则；`.github/workflows` 无 CodeQL/Trivy/secret scanning。`dependabot.yml` 只
  覆盖 maven + github-actions，**不覆盖已纳入 git 的 npm 依赖树**（admin-web，React 19/antd 6/vite 8）。
- **（中）版本治理止步于 Maven。** `bump_cola_version.sh` 只遍历 `pom.xml`，零 `package.json`/npm
  引用，而 `admin-web/package.json:3` 硬编码 `"version": "5.2.3"`。下次 bump 会静默让已发布的 UI
  版本与组件版本脱钩。（这也是 §6.1 与 C-01 的同一根因：admin-web 在所有治理机制之外。）
- **（中）许可与法务从未被检查。** 根 pom 同时声明 MIT 与 LGPL-2.1 且都是 `distribution=repo`，
  **未说明哪个 artifact 适用哪个许可**，对下游消费者法律上是歧义的；LGPL 条目的 URL 双重错误
  （`blob/master/LICENSE-GPL-2.1`，分支应为 `main`、文件名应为 `LICENSE-LGPL-2.1`）；无 NOTICE 文件。
- **（低）测试质量与发布门禁未评估。** jacoco 只配了 `prepare-agent` + `report`，**没有 `check`
  goal，因此全仓无覆盖率下限**——本次合并的 19,407 行新增代码没有任何强制覆盖门槛。CI 的根步骤
  跑默认 profile 的 `clean install`，只有 bytecode-test 用 `-Prelease verify`，**release profile 的
  javadoc/source/签名/产物形状门禁从未在全 reactor 上被执行过**。
- **（中）设计 §5/§6/§14/§16/§18/§20 无任何裁决覆盖。** 其中 **§20 是完成定义**——那 9 条退出契约
  （每个缺口都有先红后绿的定向测试、四个中间件都要有行为断言而非仅容器启动、无法验证的边界逐项
  报告、`git diff --check` 通过……）如果被执行过，本规格 §3 的多数条目和 B-1 都会在合入前被拦下。

### 7.4 审计误判记录：B-1

本规格在实施阶段自己推翻了自己的头号发现，记录在此，因为它暴露的是**方法问题**而不是运气问题。

**误判内容**：断言 DDC Admin 因 `${sdk.version}` 无法解析而启动失败，并把它列为唯一的阻断级项、
写进摘要、写进 Wave 1 的第一刀。

**真相**：`application.yml:17-18` 的 `spring.config.import: classpath:META-INF/egon-cola-ddc.properties`
把 starter 里那份 Maven 过滤过的属性文件导入成了正规配置源，链路完整、功能正常。

**根因**：我用"排除法"证明一个否定命题——检查了 `spring.factories`、`EnvironmentPostProcessor`、
`@PropertySource`、`build-info`、部署环境变量，然后宣布"确定不是 Spring 属性"。**穷举式排除
本质上无法证明否定命题**，只要漏掉一种机制（这里是 `spring.config.import`）结论就翻转。而我在
§3.0 里还专门写了一段"补充排除"来加强这个错误结论，等于给错误加了信心。

**教训（对后续所有审计有效）**：

1. **能跑就不要推理。** 这个误判用一个 5 秒的测试就能证伪，而我写了三段静态分析去支撑它。
   凡是断言"运行时会失败"的结论，落笔前必须实际运行一次。
2. **否定命题要正面验证。** 与其枚举"没有哪种机制提供它"，不如直接问"它到底解析成什么"——
   一次 `environment.getProperty()` 打印胜过五轮 grep。
3. **本规格其余结论的可信度分层没有变**：§3 其余各项、§4、§5 都是**正面证据**
   （某文件某行确实是某内容 / 某计数为 0），不是否定命题，不受本次误判影响；
   §7.2 那 22 条被推翻的结论也恰恰说明对抗性复核在起作用。但读者有权因此对本规格中
   任何"因此会失败"型推断降低置信度，除非它带有实跑证据。

本次误判的成本被限制在文档层面：没有据此改动任何生产代码。

---

## 8. 整改计划

### Wave 0｜文档状态收口（阻塞其余，建议 1 天内）

| 任务   | 产出                                                                               |
|------|----------------------------------------------------------------------------------|
| W0-1 | 按代码事实回填 gateway 七份计划（0/160）与 ShardingSphere 计划（0/39）的勾选框                         |
| W0-2 | 翻转 ShardingSphere spec 状态；给 2026-07-23 旧计划加取代声明（S-3）                             |
| W0-3 | 建 `docs/superpowers/INDEX.md`，112 份文档全部归类（已实现/进行中/计划中/已废弃）                       |
| W0-4 | 修正设计与计划中的事实错误：S-2 三段式表名、P-5/P-6 错误路径、P-9 占位符、低级项里的 `isOnline`/`REGISTERED`/基线不一致 |

### Wave 1｜合并代码的未闭合项（§3 全部 + §7.1 的 3 项）

~~先做 B-1~~ —— **B-1 已撤销**（§3.0），其配套的 `DdcAdminContextSmokeTest` 已作为配置链路
回归网保留并合入。

四项高：H-1 Compose readiness（含 `compose.demo.yml`）、H-2 §17 可观测性与脱敏、
H-3 `ddcAdminRedissonClient` destroyMethod、H-4 计划 06 的部署凭据传播（与 M-7 通配凭据一并做）。

再十一项中：M-1 timeout_ms 列、M-2 入口崩溃窗口、M-3 app-code 默认值、M-4 版本字面量（engine 的
`"5.2.3"` 与 DTP 的 `5.2.1`）、M-5 ACK 分类服务端配套、M-6 revision seeding、M-7 通配凭据、
M-8 定时任务 leader election、M-9 journal 正文重复与回收、M-10 迁移回滚方向、
M-11 admin 无可用的完整上下文测试路径。最后两项低。

**门禁**：设计 §20 的原 9 条（**本轮必须真正执行它——§7.3 指出它从未被执行**），加上
"§17 的 7 类维度与 3 条禁令各有测试或显式豁免"，以及"每个 admin 至少有一个用真实
`application.yml` 启动上下文的冒烟测试"。

### Wave 2｜计划套件质量（§4）

P-4 定向测试命令统一（建议在 `egon-cola-components/pom.xml` 设
`<failIfNoSpecifiedTests>false</failIfNoSpecifiedTests>` 一次性解决 20 处）、P-7/P-8/P-10 的内部
矛盾、P-11 外部 Redis 清理、以及低级项。**建议同时引入一个机械校验：发布计划前用脚本核对
Files 列表里的每条路径在基线树上存在**——本次三处路径错误都能被它拦下。

### Wave 3｜archetypes（§6.1）

**A-1 + A-2 优先**（模板改用 `${package}`，CI 把 light/service 也改成 `clean verify`）——修复很小，
但它是全仓"治理声明与现实差距"最大的一处。然后 S-4/S-5/S-6/S-7（ShardingSphere 的中级项）、
A-3 约定文档收口、A-4/A-5。

### Wave 4｜components

**不在本规格范围**，执行姊妹设计 `2026-07-26-components-capability-hardening-design.md`。

### Wave 5｜治理（§6 的 G-1..G-7）

G-1 把 gateway-live 至少纳入 PR 路径过滤触发；G-2/G-3 README 与版本一致性（并让 bump 脚本覆盖
markdown，或删除 `maven-deploy.md` 里的失效声明）；G-4 清理 `.gitmodules`；G-5 发布门禁；
G-6/G-7 修正。

### 分支策略

Wave 0 为纯文档，可直接在 main 上做。Wave 1/2/3/5 各起独立分支。**Wave 3 的 archetype 部分注意：
ShardingSphere 改造刚落在 main 上且未走 worktree（S-3 低级项），同区域再改需先确认无未提交状态。**

---

## 9. 决策点（已评审确认）

**评审已完成（2026-07-26）：七项全部采纳建议方案。** 以下为已确认的实施口径。

| #   | 决策点                              | 已确认口径                                                                  | 对整改的直接影响                                                                                   |
|-----|----------------------------------|------------------------------------------------------------------------|--------------------------------------------------------------------------------------------|
| 9.1 | `content_value` 是否落库             | **(b)** 只存 `contentSha256`，正文从 `gateway_release_content` 按 configKey 取 | V4 需去掉 `content_value` 列或改为可空并停写；H-2 的脱敏边界随之简化                                             |
| 9.2 | published 指针推进判据                 | **(b)** 任务终态 SUCCESS 才推进指针                                             | 兑现"pull 不读未发布内容"；发布延迟 = ACK 收敛时间，需定义部分成功语义                                                 |
| 9.3 | Provider 版本来源统一方向                | **(a)** 契约版本赢，`artifactVersion` 只进 metadata                            | 兼容既有 `1.0.0` catalog，无需数据迁移                                                                |
| 9.4 | UNKNOWN 状态实例的处置                  | **(a)** UNKNOWN 一律不可用                                                  | 维持现状；需确认无老生产者依赖"不写 status 即在线"                                                             |
| 9.5 | LKG 恢复失败时 engine 行为              | **(a)** 维持 fail closed                                                 | 现状即目标，只需在设计中显式记录该取舍                                                                        |
| 9.6 | DDC Admin 是否支持多副本                | **(b)** 本轮不做，显式声明单副本                                                   | **必须从 `compose.ha.yml` 移除 `ddc-admin-2` 与 `gateway-admin-2`**，否则 M-8 的 8 个无锁定时任务会在双副本下真实并发 |
| 9.7 | ShardingSphere `test` profile 定位 | **(b)** `test` 快速失败、不回退 H2，H2 拓扑另起名字                                   | 与设计 §3.1.3/§3.2.1 一致；需同步改三个 archetype 的 `application-test.yml` 与 `verify.groovy`           |

> **9.6 的落地要点**：选 (b) 不是"什么都不做"。当前 `compose.ha.yml` 实际在跑双 admin，而
> §3 M-8 的 8 个 `@Scheduled` 作业没有任何 leader election——**不移除双副本，等于把一个已知的
> 并发缺陷留在已发布的拓扑里**。这一项应与 H-4/M-7（凭据传播与通配 scope）一起改，它们同文件。

各项详细取舍如下（保留原始 a/b 论证，供实施时回溯）。

**9.1｜`content_value` 落库（新增，源自 P-10 + H-2）**
journal 表把完整规则正文以 `TEXT NOT NULL` 存在 `gateway_release_publication` 里，而设计 §7.1 的
列清单只有 `contentSha256`，§17 又禁止记录完整规则正文。
(a) 保留落库，在设计 §7.1 补列并明确"禁止记录 ≠ 禁止存储"，同时定义该列的保留期与访问控制。
(b) 改为只存 sha256，正文从 `gateway_release_content` 按 configKey 取（原设计的隐含口径）。

**9.2｜published 指针的推进判据**
实现已选定"从 version 表取已发布值"（原决策 7.2 已解决）。仍未定的是推进时机：
(a) Redis 投递成功即推进——现状，但全 NACK 的 FAILED 任务版本仍会被 pull 分发。
(b) 任务终态 SUCCESS 才推进——真正实现"pull 不读未发布内容"，代价是发布延迟等于 ACK 收敛时间。

**9.3｜Provider 版本来源统一方向**
(a) 契约版本赢（registry 与 catalog 都用服务契约 version，artifactVersion 只进 metadata）——兼容
现有 `1.0.0` catalog。(b) 构建版本赢——需同步改 catalog 默认值与既有数据。

**9.4｜UNKNOWN 状态的既有实例**
(a) UNKNOWN 一律不可用——现状，安全，但会摘除不写 status 的老生产者。
(b) 迁移窗口内 UNKNOWN 视同 ONLINE 并打告警指标。

**9.5｜LKG 恢复失败时 engine 的行为**
(a) fail closed（现状）。(b) 空载启动 + 等待 DDC 推送。

**9.6｜DDC Admin 是否支持多副本**
(a) 本轮做（发布互斥与完成通知改分布式）——范围显著扩大。
(b) 本轮不做，显式声明单副本并从 `compose.ha.yml` 移除双 admin——**推荐**。

**9.7｜ShardingSphere `test` profile 的定位（S-5）**
(a) 保持 H2 内存默认，但把 profile 更名/改为非环境语义的配置源。
(b) 让 `test` 快速失败、不回退 H2，H2 拓扑另起名字——与设计 §3.1.3/§3.2.1 一致。

---

## 10. 完成定义

本规格自身的完成条件（审计侧）已全部满足，见文首完成度表。以下是**整改侧**的完成定义。

0. §9 的七项决策点全部有评审意见，且已据此更新受影响的设计条目。
1. **M-11 已处置**：DDC Admin 与 Gateway Admin 各有一个可用的完整上下文启动测试路径
   （当前 admin 因 Redis 强依赖无法启动完整上下文）。配置链路回归网
   `DdcAdminContextSmokeTest` 保持绿色。
2. §3 的 17 项各有修复 + 先失败后通过的定向测试，或明确排期。
3. §7.1 的 3 项各有修复或明确排期；§7.2 末尾未复核的低级观察各自确认过；§7.3 的六类盲区各有
   处置结论（修复 / 排期 / 显式接受）。
4. §4 的 19 项各有修正，或在 INDEX 中登记为已知偏差。
5. §5 的 13 项各有修正；旧计划取代声明已加。
6. §6 的三处勾选状态与代码事实一致；`docs/superpowers/INDEX.md` 覆盖全部 112 份文档。
7. §6.1 的 A-1/A-2 已修复，且 CI 能证明架构检查在真实包名下真的执行了。
8. 设计 §20 的原有 9 条完成定义全部满足（**§7.3 指出它从未被真正执行过**）。
9. 无法运行的环境（Redis Cluster/Sentinel、TLS/HA、Docker healthcheck 探针、真实 PostgreSQL）
   逐项报告为未验证边界，不以 mock 冒充通过。
10. `git diff --check` 通过；不遗留运行中的业务进程、容器或后台任务。

---

## 11. 整改进度台账

起始快照：`main@55614029`（2026-07-26）。此时已合入的是**姊妹设计**的 components 波次
（access-guard / dtp / bytecode / rule-engine / method-extension），**本规格所列条目尚未开始**。

下表每行都给了机械可验的判据，可直接重跑确认，不依赖人工记忆。

| 项           | 判据（在仓库根执行）                                                               | 期望                                          | 起始                             |
|-------------|--------------------------------------------------------------------------|---------------------------------------------|--------------------------------|
| ~~B-1~~     | **已撤销**（§3.0）——`spring.config.import` 使该链路成立                             | —                                           | 不适用                            |
| **配置链路回归网** | `./mvnw -pl …-admin test -Dtest=DdcAdminContextSmokeTest`                | 绿                                           | ✅ 已加入，2 断言通过；admin 全量 111 测试通过 |
| **M-11**    | 存在可启动完整 admin 上下文的测试路径                                                   | 存在                                          | 无（Redis 强依赖）✗                  |
| **H-1**     | `grep -c 'healthcheck:' …/gateway/deployment/compose.demo.yml`           | `>0`                                        | 0 ✗                            |
| **H-1**     | `grep -c 'service_started' …/deployment/compose.demo.yml`                | `0`                                         | 7 ✗                            |
| **H-3**     | `grep -c 'destroyMethod' …/ddc…-admin/…/config/DdcAdminRedisConfig.java` | `>0`                                        | 0 ✗                            |
| **M-1**     | `grep -c 'timeout' …/gateway-admin/…/db/migration/V4__*.sql`             | `>0`                                        | 0 ✗                            |
| **M-3**     | `grep 'app-code' …/gateway-engine/src/main/resources/application.yml`    | 与 publisher 的 `gateway-engine-{group}` 约定一致 | `egon-cola-gateway-engine` ✗   |
| **M-4**     | `grep -c '"5\.2\.3"' …/gateway/engine/GatewayEngineConfiguration.java`   | `0`                                         | 2 ✗                            |
| **M-4b**    | `grep -c '5\.2\.1' …/dtp/admin/manifest/DtpManifestController.java`      | `0`                                         | 2 ✗                            |
| **9.1/M-9** | `grep -c 'content_value' …/V4__add_release_publication_journal.sql`      | `0`（决策 b）                                   | 1 ✗                            |
| **9.6/M-8** | `grep -cE 'ddc-admin-2\|gateway-admin-2' …/deployment/compose.ha.yml`    | `0`（决策 b）                                   | >0 ✗                           |
| **A-1**     | `grep -c 'it.pkg' …/archetype-light/…/archetype-resources/pom.xml`       | `0`                                         | 7 ✗                            |
| **A-2**     | `grep -c 'clean test' .github/workflows/ci_java_compatibility.yaml`      | light/service 改为 `clean verify`             | 未改 ✗                           |
| **W0-1**    | 七份 `2026-07-26-integration-0*.md` 的 `- [x]` 计数                           | `160`                                       | 0 ✗                            |
| **S-1**     | `2026-07-26-archetype-shardingsphere-none-strategy.md` 的 `- [x]` 计数      | `39`                                        | 0 ✗                            |
| **G-2**     | `ls cola-samples \| wc -l` 与 README 布局一致                                 | 一致                                          | 目录空、README 仍列 3 个样例 ✗          |

**建议的最短首刀**（三处同文件、一次提交即可闭合四个条目）：
`deployment/compose.demo.yml` 加 healthcheck + readiness（H-1），`deployment/compose.ha.yml`
移除双 admin（9.6/M-8），`deployment/compose.yml` 收敛通配 HMAC 凭据（M-7）并补上凭据传播（H-4）。

**已完成**：`DdcAdminContextSmokeTest` 已新增并通过（3 个断言），把
`Maven 过滤 → starter properties → spring.config.import → Environment → @Value` 这条链路钉死。
它原本是为验证 B-1 而写，结果反而证伪了 B-1——但作为回归网仍然必要，因为这条链路此前
**没有任何测试覆盖**。
