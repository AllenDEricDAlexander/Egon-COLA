# MyBatis-Plus Sharding-JDBC 整合 Starter 实施计划

| Field              | Value                                                                                                                        |
|--------------------|------------------------------------------------------------------------------------------------------------------------------|
| Document           | [`2026-09-14-22-28-mp-sharding-jdbc-starter-implementation.md`](2026-09-14-22-28-mp-sharding-jdbc-starter-implementation.md) |
| Template Version   | `4`                                                                                                                          |
| Status             | `Review`                                                                                                                     |
| Created            | `2026-09-14 22:28 CST`                                                                                                       |
| Updated            | `2026-09-14 22:28 CST`                                                                                                       |
| Owner              | Mario / Egon-COLA maintainers                                                                                                |
| Repository         | `Egon-COLA`                                                                                                                  |
| Scope              | 重命名并扩展 Common MP Starter 为强制 PG+MP+ShardingSphere-JDBC+ext；六套 archetype 脚手架与 Agent 改为只消费该 Starter                            |
| Source Requirement | 已接受 Spec：外部只引该 Starter；必须同时使用 PG、MP、Sharding-JDBC 与 ext；排除 SS 必须编译或启动失败                                                      |
| Baseline Revision  | `main @ f94a8894435efe8ef53264549e42469f6ca420b5`；工作树仅含本 Spec/Plan 文档                                                        |
| Implements Spec    | [MP + Sharding-JDBC Starter 统一](../spec/2026-09-14-22-05-mp-sharding-jdbc-starter-unification.md)                            |
| Spec Status        | `Accepted`                                                                                                                   |
| Spec Revision      | Updated `2026-09-14 22:24 CST`；用户审核通过并关闭强制 SS 决策                                                                             |
| Effective Specs    | [MP + Sharding-JDBC Starter 统一](../spec/2026-09-14-22-05-mp-sharding-jdbc-starter-unification.md)                            |
| Depends On Plans   | [已完成的 Repository/SS 实施](2026-09-13-07-42-mybatis-repository-sharding-implementation.md)                                      |
| Supersedes         | `None`                                                                                                                       |
| Superseded By      | `None`                                                                                                                       |
| Related Plans      | `None`                                                                                                                       |

## 1. Summary

本 Plan 实施已接受的 [2026-09-14 Spec](../spec/2026-09-14-22-05-mp-sharding-jdbc-starter-unification.md)。把
`egon-cola-component-common-mybatis-plus-spring-boot-starter` 重命名为
`egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter`，硬依赖 ShardingSphere-JDBC
5.5.3，上收物理池/策略/逻辑 DataSource/TableInfo 维护，删除六套复制 bootstrap，Agent 提供合法 SS YAML。共 8 个顺序
Step，每步一次语义提交。完成证据是模块测试、脚手架架构测试与文档键齐；真实 PG 主从仍为人工门禁。

## 2. Target Spec and Effective Design

### 2.1 Primary target

-
Path: [docs/egon/spec/2026-09-14-22-05-mp-sharding-jdbc-starter-unification.md](../spec/2026-09-14-22-05-mp-sharding-jdbc-starter-unification.md)
- Status: `Accepted`
- Revision: Updated `2026-09-14 22:24 CST`；基线 `f94a8894435efe8ef53264549e42469f6ca420b5`
- Approval evidence: 用户 2026-09-14 “spec审核过了，还可以”，并追加强制 PG+MP+SS+ext 一体、排除 SS 必须失败

### 2.2 Effective Spec set

| Role                       | Spec/link                                                                       | Status/revision               | Effective sections                            | Why included      |
|----------------------------|---------------------------------------------------------------------------------|-------------------------------|-----------------------------------------------|-------------------|
| Primary                    | [统一 Spec](../spec/2026-09-14-22-05-mp-sharding-jdbc-starter-unification.md)     | Accepted，2026-09-14 22:24 CST | 全文，尤其 `REQ-001`–`REQ-022`、`DEC-009`/`DEC-010` | 本 Plan 唯一需求编号来源   |
| Context, already delivered | [主规格](../spec/2026-09-12-21-11-mybatis-repository-cqrs-postgresql-evolution.md) | Accepted                      | Repository/`EgonModel`/LOCAL 守卫/两级纯函数         | 不重实施；本 Plan 复用现代码 |
| Context, already delivered | [重建修订](../spec/2026-09-13-07-42-mybatis-test-database-rebuild-amendment.md)     | Accepted                      | 空 schema 与旧 SQL 不可变                           | 不改 B/V/manual 字节  |

Header `Effective Specs` 只链主键，避免把已完成主规格的 `REQ-001`–`REQ-042` 再次纳入本 Plan 覆盖集。其合同通过 Depends On
Plans 的已完成实施保留。

### 2.3 Superseded or excluded content

不实施在线重分片、Seata、Hibernate ddl-auto、MP-only 剖面、默认 `DdlApplicationRunner`、修改旧迁移文件、改 `vector_store` 所有权。

## 3. Effective Requirements and Acceptance

| Requirement | Source Spec section | Effective statement        | Observable acceptance                      | Implementation impact |
|-------------|---------------------|----------------------------|--------------------------------------------|-----------------------|
| `REQ-001`   | Spec §4             | 模块更名为整合 Starter            | 新坐标可解析；旧坐标 relocation                      | Step 1                |
| `REQ-002`   | Spec §4             | 编译期整合 MP 3.5.16 与 SS 5.5.3 | 脚手架 POM 不再直连 SS artifacts                  | Step 1, 7, 8          |
| `REQ-003`   | Spec §4             | 只引 Starter 即得逻辑 DataSource | `@Primary` SS DataSource；缺 YAML 不得出现该 Bean | Step 6                |
| `REQ-004`   | Spec §4             | 六套删除本地 SS 适配               | 无 `ShardingDataSourceBootstrapper` 复制类型    | Step 7, 8             |
| `REQ-005`   | Spec §4             | 一份 YAML                    | 删除双文件组合；profile 键齐                         | Step 2, 7, 8          |
| `REQ-006`   | Spec §4             | YAML 策略模式                  | 未知 type 启动失败                               | Step 3                |
| `REQ-007`   | Spec §4             | 兼容原生 SS rules              | NATIVE 与 STRATEGY 互斥                       | Step 3, 4             |
| `REQ-008`   | Spec §4             | README 推荐用法                | 中英含订单两级与 XA                                | Step 8                |
| `REQ-009`   | Spec §4             | 默认 LOCAL，不排除 XA            | POM 无 xa-core exclusion                    | Step 1, 6             |
| `REQ-010`   | Spec §4             | PG 读写分离                    | READWRITE 事务读主                             | Step 4, 6             |
| `REQ-011`   | Spec §4             | 逻辑/广播/单表互斥                 | 策略校验                                       | Step 3                |
| `REQ-012`   | Spec §4             | tenant 一层、业务 ID 二层         | mix64 + COMPLEX 策略                         | Step 3, 4             |
| `REQ-013`   | Spec §4             | 缺键/跨组写失败                   | LOCAL 守卫消费 profiles                        | Step 4, 6             |
| `REQ-014`   | Spec §4             | 均匀性与指纹拒绝静默改图               | fingerprint mismatch 失败                    | Step 4                |
| `REQ-015`   | Spec §4             | 动态表名默认关                    | 既有 interceptor 保留                          | Step 6                |
| `REQ-016`   | Spec §4             | TableInfo 分片感知维护           | CREATE/ADD，禁止 DROP                         | Step 5                |
| `REQ-017`   | Spec §4             | checksum 脚本保留              | 无 IDdl Bean                                | Step 5, 6             |
| `REQ-018`   | Spec §4             | 全部应用表 tenant_id            | 扫描缺列失败                                     | Step 5                |
| `REQ-019`   | Spec §4             | Agent 消费强制 SS              | 合法 YAML + SS DataSource                    | Step 8                |
| `REQ-020`   | Spec §4             | 多环境键结构一致                   | yml/dev/test/prod 键齐                       | Step 7, 8             |
| `REQ-021`   | Spec §4             | 本阶段不启动外部基础设施               | Plan 不连库、不 start                           | 全过程约束                 |
| `REQ-022`   | Spec §4             | 强制 PG+MP+SS+ext            | exclusion SS 或缺 YAML 失败                    | Step 1, 6             |

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

先让新模块坐标与 SS 硬依赖可编译，再落 YAML 合同与策略，再搬 bootstrap 与 schema，再强制 AutoConfig，最后改消费者。这样每步都能在
Starter 模块内 RED/GREEN，脚手架删除发生在 Starter API 已存在之后。

### 4.2 Test-first strategy

每个行为 Step 先写失败测试：缺 SS 依赖/缺 YAML、未知 type、缺分片键、缺表维护、脚手架仍含复制类。GREEN 只补最小生产代码。Maven
模块测试不启动 Docker；PG IT 保持既有手工标签。

### 4.3 Sequential and parallel boundaries

| Step   | Depends on | May run in parallel with | Must not overlap with | Reason                      |
|--------|------------|--------------------------|-----------------------|-----------------------------|
| Step 1 | None       | None                     | 全部后续                  | 坐标未定则后续路径无效                 |
| Step 2 | Step 1     | None                     | Step 3–6              | Properties 是策略输入            |
| Step 3 | Step 2     | None                     | Step 4                | Factory 才能生成 rules          |
| Step 4 | Step 3     | None                     | Step 5–6              | Bootstrap 需要策略与算法           |
| Step 5 | Step 4     | None                     | Step 6                | 维护器在逻辑 DS 之前被 bootstrap 调用  |
| Step 6 | Step 5     | None                     | Step 7–8              | AutoConfig 是消费者合同           |
| Step 7 | Step 6     | None                     | Step 8                | Light 先验证消费形状               |
| Step 8 | Step 7     | None                     | None                  | 其余脚手架、Agent、README、verifier |

### 4.4 Commit boundaries

每 Step 一次 path-limited 提交。模块 rename 必须在 Step 1 单独提交，避免后续路径漂移。

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element            | Spec necessity verdict/section | Current repository evidence              | Direct/reuse alternative | Interaction/implementation cost | Plan decision       |
|-------------------------|--------------------------------|------------------------------------------|--------------------------|---------------------------------|---------------------|
| 整合 Starter 重命名          | Add/Keep，§7.0                  | 现 `...-mybatis-plus-spring-boot-starter` | 保持旧名让脚手架继续直连 SS          | relocation 一次                   | Implement           |
| Strategy 族              | Add，§13                        | 脚手架复制 CLASS_BASED                        | 每表 if/else               | 未知 type 失败                      | Implement           |
| TableInfo 维护器           | Add，§11                        | 仅脚本 runner                               | 只跑 SQL                   | 新管理表                            | Implement           |
| 强制 SS                   | `DEC-010`                      | 当前 `sharding` 可关、plain resolver          | MP-only                  | 用户禁止                            | Implement fail-fast |
| SchemaObject Repository | Remove，§7.0                    | DDL runner 已用 JDBC                       | 再包 Repository            | 多余                              | 不实施                 |
| 外部 API                  | Unchanged                      | 现有 adapter                               | 无                        | 无                               | 不实施                 |

### 4.6 Change-unit Dependency Matrix

| Change unit       | Requirements                                          | Proof/RED point         | Compile/runtime prerequisites | Produces                     | Consumers/unblocks | Owning Step |
|-------------------|-------------------------------------------------------|-------------------------|-------------------------------|------------------------------|--------------------|-------------|
| Maven 坐标与 SS 硬依赖  | `REQ-001`, `REQ-002`, `REQ-009`, `REQ-022`            | POM/exclusion 测试        | 现 common reactor              | 新模块路径                        | 全部后续               | Step 1      |
| 单 YAML Properties | `REQ-005`, `REQ-020`, `REQ-022`                       | 缺 URL/enabled=false 失败  | 新模块                           | `EgonColaShardingProperties` | Step 3–6           | Step 2      |
| 策略 Factory        | `REQ-006`, `REQ-007`, `REQ-011`, `REQ-012`            | 未知 type                 | Properties                    | rules 片段 + profiles          | Step 4             | Step 3      |
| Bootstrap/算法      | `REQ-003`, `REQ-010`, `REQ-013`, `REQ-014`            | 缺键/关池                   | Factory                       | 逻辑 DS 工厂                     | Step 5–6           | Step 4      |
| Schema 维护         | `REQ-016`, `REQ-017`, `REQ-018`                       | 缺 tenant 失败             | Bootstrap 物理池                 | `egon_schema_object`         | Step 6             | Step 5      |
| 强制 AutoConfig     | `REQ-003`, `REQ-015`, `REQ-022`                       | 缺 YAML 无 DataSource 且失败 | 维护器                           | `@Primary DataSource`        | Step 7–8           | Step 6      |
| 脚手架消费             | `REQ-004`, `REQ-005`, `REQ-008`, `REQ-019`, `REQ-020` | 复制类存在则失败                | AutoConfig                    | 单 YAML 消费者                   | 生成 verifier        | Step 7–8    |

### 4.7 Java, Spring, and Egon-COLA Implementation Standards

| Concern                    | Current repository evidence                                                                      | Effective Spec decision | Planned implementation consequence         | Owning Steps/checks                                    |
|----------------------------|--------------------------------------------------------------------------------------------------|-------------------------|--------------------------------------------|--------------------------------------------------------|
| Architecture profile       | Starter 包 `top.egon.cola.component.common.mybatis`；精确 COLA archetype 脚手架                         | Spec §6.1               | 不引入 `biz.*`；脚手架只删/改 infrastructure 配置      | 每 Step；`MC-ARCH-001`                                   |
| Reuse/capability           | `YamlShardingSphereDataSourceFactory`、`EgonColaTwoLevelRouteStrategy`、`EgonColaPostgreDdlRunner` | 上收而非重写                  | 搬 Light 实现进 Starter 并改包名                   | Step 4–5；`MC-REUSE-001`                                |
| Naming/model/validation    | 现 Properties 不完整 Lombok；新类必须合规                                                                   | Spec §10                | 新 Properties 完整基线；简单值为 record；无新 Converter | Step 2–5；`MC-NAME-001`/`MC-MODEL-001`/`MC-CONVERT-001` |
| Bean/logging/config        | 现 Starter `lombok.config` 复制 Qualifier                                                           | Spec §6.2               | 显式 Bean 名、`@Qualifier`、profile 键齐          | Step 2, 6–8；`MC-BEAN-001`/`MC-CONFIG-001`              |
| Business variation/pattern | 表 type 变化轴                                                                                       | Strategy + Factory      | 禁止脚手架包名硬编码                                 | Step 3；`MC-PATTERN-001`                                |

#### Capability reuse ledger

| Need          | Candidates inspected                                     | Exact evidence           | Fit/gap              | Decision                | Added dependency/custom code | Owning Step/check     |
|---------------|----------------------------------------------------------|--------------------------|----------------------|-------------------------|------------------------------|-----------------------|
| 逻辑 DataSource | SS `YamlShardingSphereDataSourceFactory`；六套 Bootstrapper | Light bootstrap          | 能力在脚手架               | 上收 Starter              | 不新增第三方                       | Step 4；`MC-REUSE-001` |
| 两级哈希          | `EgonColaTwoLevelRouteStrategy`                          | 现 routing 包              | 足够                   | 复用                      | 无                            | Step 3–4              |
| 脚本 DDL        | `EgonColaPostgreDdlRunner`                               | 现 ddl 包                  | 足够                   | 复用                      | 无                            | Step 5                |
| SS 版本         | archetypes 父 POM 5.5.3                                   | `shardingsphere.version` | components 父 POM 未管理 | 上收 components 父 POM/BOM | 不升级版本                        | Step 1；`MC-DEP-001`   |
| TableInfo DDL | MP TableInfoHelper；PgVectorStore 模式                      | Spec `EVD-011`           | 脚本不够                 | 新增 Maintainer           | 无新库                          | Step 5                |

### 4.8 User-mandated Java Rule Implementation Matrix

| Literal rule | Spec source   | Repository evidence                | Exact files and order              | Pseudocode obligations                                  | Validation gate | Steps  | Status/blocker |
|--------------|---------------|------------------------------------|------------------------------------|---------------------------------------------------------|-----------------|--------|----------------|
| Rule 1       | Spec §6.2/§10 | 新策略/维护类型                           | Step 2–5 新类型                       | `*Strategy`/`*Factory`/`*Query`/`*Result`/`*Properties` | 命名扫描            | 2–5    | PASS           |
| Rule 2       | Spec §6.2/§9  | YAML→Properties→Factory→Maintainer | Properties、Query records           | `@Validated`/`ValidationUtils`                          | 正反测             | 2–5    | PASS           |
| Rule 3       | Spec §10      | 复杂 Properties                      | `EgonColaShardingProperties`       | 完整 Lombok；record 简单值；无 Converter                        | delombok/编译     | 2      | PASS           |
| Rule 4       | Spec §6.2     | 现 lombok.config                    | Bootstrapper/Maintainer/AutoConfig | `@Slf4j`、Bean 名、`@Qualifier`                            | 注入测试            | 4–6    | PASS           |
| Rule 5       | Spec §6.2     | 现依赖                                | 新类 import                          | 仅 JDK/Commons/Guava                                     | import 扫描       | 全部     | PASS           |
| Rule 6       | Spec §6.2     | 内部 Jackson manifest                | 既有 ObjectMapper                    | 无新外部 JSON                                               | 无新 wire         | 4–5    | PASS           |
| Rule 7       | Spec §6.2     | 脚手架四份 profile                      | YAML                               | 键齐                                                      | `TEST-011`      | 7–8    | PASS           |
| Rule 9       | Spec §13      | 表 type                             | Strategy 族                         | Factory 选择，禁 if/else                                    | 未知 type 测试      | 3      | PASS           |
| Rule 10      | Spec §10/§11  | `applied_at`                       | schema 表/维护器                       | `Instant`/`Duration`                                    | 时间字段            | 5      | PASS           |
| Rule 11      | Spec §6.1/§8  | starter + 精确 archetype 脚手架         | 每个目标文件                             | 不发明层                                                    | verifier        | 每 Step | PASS           |

## 5. Change File Tree

```text
egon-cola-components/egon-cola-component-common/
├── pom.xml                                                                 # MODIFY module 列表
├── egon-cola-component-common-mybatis-plus-spring-boot-starter/            # RENAME→relocation POM
└── egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/  # RENAME 目标
    ├── pom.xml                                                             # MODIFY SS 硬依赖
    ├── README.md / README.zh-CN.md                                         # MODIFY
    └── src/main/java/top/egon/cola/component/common/mybatis/
        ├── autoconfigure/EgonColaMybatisPlusAutoConfiguration.java         # MODIFY 删除空 profiles
        ├── autoconfigure/EgonColaShardingAutoConfiguration.java            # CREATE
        ├── sharding/EgonColaShardingProperties.java                        # CREATE
        ├── sharding/strategy/*                                             # CREATE
        ├── sharding/algorithm/*                                            # CREATE
        ├── sharding/bootstrap/*                                            # CREATE
        ├── sharding/resolver/EgonColaShardingWriteTargetResolver.java      # CREATE
        └── schema/EgonColaTableInfoSchemaMaintainer.java                   # CREATE
            EgonColaSchemaMaintainQuery.java / EgonColaSchemaMaintainResult.java

egon-cola-components/pom.xml / egon-cola-components-bom/pom.xml             # MODIFY 版本与坐标
六套 archetype 脚手架 infrastructure/config/datasource/*                              # DELETE 复制类
六套 datasource/sharding*.yml 与 sharding/shardingsphere-*.yml              # DELETE
六套 + Agent 一份 egon-mybatis-plus-sharding.yml                            # CREATE
AgentArchitectureTest                                                       # MODIFY 允许 SS 合法引用
```

| Operation | Path                                                                                                            | Current evidence/symbol    | Final symbols/state    | Responsibility | Step     | Requirements         | Validation owner |
|-----------|-----------------------------------------------------------------------------------------------------------------|----------------------------|------------------------|----------------|----------|----------------------|------------------|
| RENAME    | `.../egon-cola-component-common-mybatis-plus-spring-boot-starter` → `...-sharding-jdbc-ext-spring-boot-starter` | 现模块目录                      | 新目录 + 旧 relocation POM | 坐标             | Step 1   | `REQ-001`            | POM 测试           |
| MODIFY    | `egon-cola-components/pom.xml`                                                                                  | 无 `shardingsphere.version` | 管理 5.5.3               | 版本             | Step 1   | `REQ-002`            | 父 POM            |
| MODIFY    | `egon-cola-components-bom/pom.xml`                                                                              | 旧 artifact                 | 新旧坐标                   | BOM            | Step 1   | `REQ-001`            | BOM              |
| CREATE    | `.../sharding/EgonColaShardingProperties.java`                                                                  | 无                          | 单 YAML 绑定              | 配置             | Step 2   | `REQ-005`, `REQ-022` | Binder 测试        |
| CREATE    | `.../sharding/strategy/*`                                                                                       | 脚手架算法类                     | 五策略 + Factory          | 变化轴            | Step 3   | `REQ-006`–`REQ-012`  | Factory 测试       |
| CREATE    | `.../sharding/bootstrap/*`                                                                                      | Light bootstrap            | Starter bootstrap      | 启动             | Step 4   | `REQ-003`, `REQ-010` | 关池测试             |
| CREATE    | `.../schema/EgonColaTableInfoSchemaMaintainer.java`                                                             | 仅脚本 runner                 | TableInfo 增量           | schema         | Step 5   | `REQ-016`–`REQ-018`  | 维护测试             |
| CREATE    | `.../autoconfigure/EgonColaShardingAutoConfiguration.java`                                                      | 条件空 profiles               | 强制 SS DS               | 装配             | Step 6   | `REQ-003`, `REQ-022` | ContextRunner    |
| DELETE    | 六套 `config/datasource` 复制类与双 YAML                                                                               | `EVD-003`                  | 目录不存在                  | 去适配            | Step 7–8 | `REQ-004`            | 架构测试             |
| CREATE    | 七份 `egon-mybatis-plus-sharding.yml`                                                                             | 两组旧 YAML                   | 单文件                    | 消费             | Step 7–8 | `REQ-005`, `REQ-019` | 键齐               |
| MODIFY    | README 双文                                                                                                       | 写明无 SS                     | 推荐 STRATEGY + 强制 SS    | 文档             | Step 8   | `REQ-008`            | 文档测试             |

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

- 基线 `f94a8894435efe8ef53264549e42469f6ca420b5`，`main`
- 工作树仅允许本 Spec/Plan 文档；实施时 path-limited commit
- 不改旧 B/V/manual SQL 字节
- 不启动 PostgreSQL/Docker

### 6.2 Build, test, and environment prerequisites

| Concern    | Exact command/source                                                                                                                                                                                      | Required state | Validation boundary |
|------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------|---------------------|
| Build      | `./mvnw` Java 21                                                                                                                                                                                          | 本地可解析 BOM      | 静态/模块               |
| Starter 测试 | `./mvnw -pl egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=<Name> test` | Step 1 后路径存在   | 模块                  |
| Light 测试   | `./mvnw -pl egon-cola-archetypes/source-projects/egon-cola-source-light -am -Dtest=LightPersistenceArchitectureTest test`                                                                                 | Step 7 后       | 模块                  |
| PG/SS 真库   | 既有 `egon.pg.routing=true`                                                                                                                                                                                 | 不在本 Plan 执行    | 人工                  |

### 6.3 Immutable constraints and approved decisions

`DEC-009` 坐标惯例；`DEC-010` 强制 SS；LOCAL 默认；旧迁移不可变；`PgVectorStore` 拥有向量表。

### 6.4 Plan Clarifications

| ID              | Small implementation inference            | Repository evidence                          | Why semantics are unchanged | Impact if wrong                                       |
|-----------------|-------------------------------------------|----------------------------------------------|-----------------------------|-------------------------------------------------------|
| `PLAN-CLAR-001` | 模块目录用 `git mv` 再把旧路径改成 relocation POM     | 现单模块目录                                       | 只改坐标                        | 需再改 reactor 路径                                        |
| `PLAN-CLAR-002` | 其余五套脚手架与 Light 同构：同一 14 类删除、同一单 YAML 形状   | `EVD-003`                                    | Spec 要求消灭复制                 | 某脚手架包名不同时按实际包替换                                       |
| `PLAN-CLAR-003` | Agent 使用单 PRIMARY + 知识/outbox `SINGLE`    | Agent 现单库 Flyway                             | 仍用 SS，不是关 SS                | 需补物理 URL 环境变量名                                        |
| `PLAN-CLAR-004` | 既有 H2 拦截器单测可保留，但 AutoConfig IT 必须带分片 YAML | 现 `EgonColaMybatisPlusAutoConfigurationTest` | 插件链测试不创建业务 DS               | 若 ContextRunner 加载 Sharding AutoConfig，必须给 YAML 或期望失败 |

## 7. Ordered File-by-file Implementation Steps

### Step 1 — 重命名模块并硬依赖 ShardingSphere-JDBC

- Requirements: `REQ-001`, `REQ-002`, `REQ-009`, `REQ-021`, `REQ-022`
- Dependencies: None
- Baseline state: 模块名为 `egon-cola-component-common-mybatis-plus-spring-boot-starter`，无 SS 依赖；Light POM 直连 SS 并
  exclusion xa-core
- Observable outcome: 新坐标可编译；旧坐标 relocation；Starter POM 含 SS 且不 exclusion xa-core
- End state: 后续文件写在新目录；消费者尚未切换
- Test-first gate: `Required` — 当前 BOM 不包含新 artifactId，exclusion 测试尚不存在
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 5`, `Rule 11`
- Ordered files:

#### File 1 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/contract/EgonColaShardingDependencyContractTest.java`

- Purpose: 锁定新 artifact、SS 硬依赖、无 xa exclusion。
- Symbols: `artifact_id_contains_sharding_jdbc_ext_spring_boot_starter`,
  `pom_declares_shardingsphere_jdbc_without_xa_exclusion`
- Repository evidence: 现模块测试在 starter `src/test/java/.../contract/`
- Dependencies and consumers: 读取本模块 `pom.xml`
- Why now: RED 证明坐标与依赖缺口。
- Contract/signature changes: 无生产 API
- Input/output and state mapping: POM XML → 断言 artifactId 与 dependency
- Error and edge behavior: 缺 SS 依赖或存在 xa-core exclusion 则失败
- Standards impact: `MC-DEP-001`, `MC-TEST-001` — 只断言 Spec 批准的 SS 5.5.3 坐标
- Literal rule enforcement: `Rule 5` 不引入额外工具库；`Rule 11` 测试留在 starter 模块
- Implementation pseudocode:

```java
@Test
void artifact_id_is_sharding_jdbc_ext_spring_boot_starter() throws Exception {
    String pom = Files.readString(Path.of("pom.xml"));
    assertThat(pom).contains("<artifactId>egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter</artifactId>");
    assertThat(pom).contains("<artifactId>shardingsphere-jdbc</artifactId>");
    assertThat(pom).doesNotContain("<artifactId>shardingsphere-transaction-xa-core</artifactId>");
}
```

- Verification contribution: Step 1 RED/GREEN
- After this file: 测试因旧 POM 失败

#### File 2 —
`RENAME egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter -> egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter`

- Purpose: 目录与 artifact 对齐 `DEC-009`
- Symbols: Maven `artifactId`
- Repository evidence: 现唯一 MP starter 目录
- Dependencies and consumers: common reactor、BOM、六套+Agent POM
- Why now: 测试要求新 artifactId
- Contract/signature changes: 坐标字符串
- Input/output and state mapping: git mv 保留历史；旧路径下一步变 relocation
- Error and edge behavior: reactor 漏模块则无法解析
- Standards impact: `MC-ARCH-001`, `MC-DEP-001` — 仍是 common 子模块
- Literal rule enforcement: `Rule 11` 不把模块改成 COLA 应用树
- Implementation pseudocode:

```text
git mv <old-dir> <new-dir>
set artifactId/name/description to the Spec name
add shardingsphere-jdbc and the current 脚手架 SS modules without xa-core exclusion
keep mybatis-plus-spring-boot3-starter
```

- Verification contribution: File 1 开始可 GREEN
- After this file: 新路径可编译

#### File 3 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/pom.xml`

- Purpose: 旧坐标 relocation 到新 artifact
- Symbols: Maven `relocation`
- Repository evidence: 旧模块曾是消费者坐标
- Dependencies and consumers: 尚未切换的外部 POM
- Why now: `REQ-001` 兼容
- Contract/signature changes: 旧 artifact 不再含代码
- Input/output and state mapping: 依赖旧坐标 → 解析到新坐标
- Error and edge behavior: 缺少 relocation 则旧消费者断
- Standards impact: `MC-DEP-001`, `MC-SCOPE-001`
- Literal rule enforcement: `Rule 11` relocation POM 仍在 common 聚合下
- Implementation pseudocode:

```xml
<artifactId>egon-cola-component-common-mybatis-plus-spring-boot-starter</artifactId>
<distributionManagement>
  <relocation>
    <artifactId>egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter</artifactId>
  </relocation>
</distributionManagement>
<dependencies>
  <dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter</artifactId>
    <version>${project.version}</version>
  </dependency>
</dependencies>
```

- Verification contribution: BOM 同时导出两坐标
- After this file: 旧坐标可解析

#### File 4 — `MODIFY egon-cola-components/egon-cola-component-common/pom.xml`

- Purpose: reactor 同时包含 relocation 模块与新模块
- Symbols: Maven reactor module list
- Repository evidence: 现只列旧模块
- Dependencies and consumers: components 构建
- Why now: File 2/3 必须被聚合
- Contract/signature changes: 模块列表
- Input/output and state mapping: 两个 starter 子模块条目
- Error and edge behavior: 漏新模块则 `-pl` 失败
- Standards impact: `MC-ARCH-001`
- Literal rule enforcement: `Rule 11` 保持 common 聚合
- Implementation pseudocode:

```text
reactor.modules.add("egon-cola-component-common-core")
reactor.modules.add("egon-cola-component-common-mybatis-plus-spring-boot-starter")
reactor.modules.add("egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter")
```

- Verification contribution: reactor 可定位两模块
- After this file: Step 1 可跑 Maven

#### File 5 — `MODIFY egon-cola-components/pom.xml`

- Purpose: 管理 `shardingsphere.version=5.5.3`
- Symbols: `dependencyManagement` 中 SS artifacts
- Repository evidence: 现仅 archetypes 父 POM 管理 SS
- Dependencies and consumers: 新 starter POM
- Why now: Starter 不能硬编码版本
- Contract/signature changes: 版本源上收
- Input/output and state mapping: 与 archetypes 相同 5.5.3 坐标列表
- Error and edge behavior: 漏模块则 starter 无法解析
- Standards impact: `MC-DEP-001` — 不升级版本
- Literal rule enforcement: `Rule 5`/`Rule 11` 只上收已用依赖
- Implementation pseudocode:

```xml
<properties>
  <shardingsphere.version>5.5.3</shardingsphere.version>
</properties>
<dependencyManagement>
  <dependency>
    <groupId>org.apache.shardingsphere</groupId>
    <artifactId>shardingsphere-jdbc</artifactId>
    <version>${shardingsphere.version}</version>
  </dependency>
</dependencyManagement>
```

- Verification contribution: starter POM 无版本号
- After this file: 依赖可解析

#### File 6 — `MODIFY egon-cola-components/egon-cola-components-bom/pom.xml`

- Purpose: BOM 导出新旧 artifact
- Symbols: `dependencyManagement` 条目
- Repository evidence: 现只导出旧 artifact
- Dependencies and consumers: 全仓消费者
- Why now: 外部通过 BOM 解析
- Contract/signature changes: 新坐标
- Input/output and state mapping: 两 artifact 同 version
- Error and edge behavior: 只改一处会导致消费者漂移
- Standards impact: `MC-DEP-001`
- Literal rule enforcement: `Rule 11`
- Implementation pseudocode:

```xml
<dependency>
  <groupId>top.egon</groupId>
  <artifactId>egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter</artifactId>
  <version>${project.version}</version>
</dependency>
<dependency>
  <groupId>top.egon</groupId>
  <artifactId>egon-cola-component-common-mybatis-plus-spring-boot-starter</artifactId>
  <version>${project.version}</version>
</dependency>
```

- Verification contribution: File 1 与 BOM 一致
- After this file: Step 1 完成

- Validation working directory: 仓库根
- Verification command:
  `./mvnw -pl egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter -am -Dtest=EgonColaShardingDependencyContractTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: 该测试 exit 0
- Failure returns to: File 2–6 POM
- Completion criteria: 新坐标可编译；无 xa exclusion
- Rollback: 还原本 Step 路径
- Commit paths:
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/contract/EgonColaShardingDependencyContractTest.java`,
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter -> egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter`,
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/pom.xml`,
  `egon-cola-components/egon-cola-component-common/pom.xml`, `egon-cola-components/pom.xml`,
  `egon-cola-components/egon-cola-components-bom/pom.xml`
- Commit: `build(mybatis): rename starter and hard-depend on shardingsphere-jdbc`

### Step 2 — 绑定单文件分片配置并拒绝关闭开关

- Requirements: `REQ-005`, `REQ-020`, `REQ-022`
- Dependencies: Step 1
- Baseline state: 无 `EgonColaShardingProperties`
- Observable outcome: 合法 YAML 绑定成功；`enabled=false`、缺物理源、非 PG 驱动失败
- End state: Properties 可供 Factory 使用
- Test-first gate: `Required` — Binder 测试因缺类失败
- Manual Checks: `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 7`, `Rule 11`
- Ordered files:

#### File 1 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/sharding/EgonColaShardingPropertiesTest.java`

- Purpose: 定义绑定与 fail-fast
- Symbols: `binds_valid_yaml`, `rejects_enabled_false`, `rejects_non_postgresql_driver`
- Repository evidence: Light `ShardingDataSourcePropertiesTest`
- Dependencies and consumers: 未来 Properties 类
- Why now: RED 配置合同
- Contract/signature changes: prefix `egon.cola.component.mybatis-plus.sharding`
- Input/output and state mapping: YAML → record 列表
- Error and edge behavior: enabled=false、空 dataSources、driver≠PG 抛校验异常
- Standards impact: `MC-VALID-001`, `MC-CONFIG-001`, `MC-TEST-001`
- Literal rule enforcement: `Rule 2` 绑定即校验；`Rule 7` 测试键集合
- Implementation pseudocode:

```java
@Test
void rejects_enabled_false() {
    assertThatThrownBy(() -> bind("enabled: false\nmode: SHARDING\nconfig-style: STRATEGY\ndata-sources: []"))
        .hasMessageContaining("SHARDING_REQUIRED");
}
```

- Verification contribution: Step 2 RED
- After this file: 编译失败或缺行为失败

#### File 2 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/sharding/EgonColaShardingProperties.java`

- Purpose: 单 YAML 根对象
- Symbols: `EgonColaShardingProperties`, nested records `PhysicalDataSourceProperties`
- Repository evidence: 现 `EgonColaMybatisPlusProperties`；Light `ShardingDataSourceProperties`
- Dependencies and consumers: Step 3–6
- Why now: GREEN 绑定
- Contract/signature changes: prefix 见 Spec
- Input/output and state mapping: mode/configStyle/transactionDefaultType/dataSources/tables/nativeRulesResource
- Error and edge behavior: `enabled` 必须 true；NATIVE 与 tables 互斥在下一步 Factory 再查也可在此 `@AssertTrue`
- Standards impact: `MC-NAME-001`, `MC-MODEL-001`, `MC-VALID-001`, `MC-BEAN-001`
- Literal rule enforcement: `Rule 1` Properties 后缀；`Rule 3` 完整 Lombok 基线；简单条目 record；`Rule 2` `@Validated`
- Implementation pseudocode:

```java
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
@Accessors(chain = true)
@Validated
@ConfigurationProperties(prefix = "egon.cola.component.mybatis-plus.sharding")
public class EgonColaShardingProperties {
    @AssertTrue(message = "SHARDING_REQUIRED")
    private boolean enabled = true;
    @NotNull private ModeEnum mode;
    @NotNull private ConfigStyleEnum configStyle;
    @NotBlank private String transactionDefaultType = "LOCAL";
    @NotEmpty @Valid private List<PhysicalDataSourceProperties> dataSources;
}
```

- Verification contribution: File 1 GREEN
- After this file: 配置合同可用

- Validation working directory: 仓库根
- Verification command:
  `./mvnw -pl egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter -am -Dtest=EgonColaShardingPropertiesTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0
- Failure returns to: File 2 约束
- Completion criteria: enabled=false 无法绑定
- Rollback: 还原本 Step 两文件
- Commit paths:
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/sharding/EgonColaShardingPropertiesTest.java`,
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/sharding/EgonColaShardingProperties.java`
- Commit: `feat(mybatis): bind mandatory sharding yaml properties`

### Step 3 — 实现分片策略 Factory

- Requirements: `REQ-006`, `REQ-007`, `REQ-011`, `REQ-012`
- Dependencies: Step 2
- Baseline state: 仅有 Properties
- Observable outcome: 五 type 可创建；未知 type 失败；NATIVE 与 STRATEGY 互斥
- End state: 可产出 SS rules 片段与 `EgonColaRoutingProfileBO`
- Test-first gate: `Required` — Factory 不存在
- Manual Checks: `MC-PATTERN-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-BEAN-001`, `MC-LOG-001`, `MC-SCOPE-001`,
  `MC-TEST-001`
- Literal Rules: `Rule 1`, `Rule 2`, `Rule 4`, `Rule 9`, `Rule 11`
- Ordered files:

#### File 1 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/sharding/EgonColaShardingStrategyFactoryTest.java`

- Purpose: 未知 type 与互斥模式
- Symbols: `rejects_unknown_type`, `rejects_native_and_strategy_together`
- Repository evidence: Light `TwoLevelShardingAlgorithmTest`
- Dependencies and consumers: Factory
- Why now: RED 策略选择
- Contract/signature changes: `create(String type, ...)`
- Input/output and state mapping: type → 策略实例
- Error and edge behavior: 未知 type → `UNKNOWN_SHARDING_STRATEGY`
- Standards impact: `MC-PATTERN-001`, `MC-TEST-001`
- Literal rule enforcement: `Rule 9` 禁止字符串 if/else 测试期望走注册表
- Implementation pseudocode:

```java
@Test
void rejects_unknown_type() {
    assertThatThrownBy(() -> factory.create("HASH_MOD", tableConfig))
        .hasMessageContaining("UNKNOWN_SHARDING_STRATEGY");
}
```

- Verification contribution: Step 3 RED
- After this file: 缺类失败

#### File 2 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/sharding/strategy/EgonColaShardingStrategy.java`

- Purpose: 策略接口
- Symbols: `toRulesFragment()`, `toRoutingProfile()`
- Repository evidence: Spec §13 Strategy
- Dependencies and consumers: 五个实现、Factory
- Why now: 编译 Factory
- Contract/signature changes: 新接口
- Input/output and state mapping: 表配置 → YAML 节点 + profile
- Error and edge behavior: 非法 actualDataNodes 抛校验
- Standards impact: `MC-PATTERN-001`, `MC-NAME-001`
- Literal rule enforcement: `Rule 1` Strategy 后缀；`Rule 9`
- Implementation pseudocode:

```java
public interface EgonColaShardingStrategy {
    String type();
    String toRulesFragment();
    EgonColaRoutingProfileBO toRoutingProfile();
}
```

- Verification contribution: 编译测试
- After this file: 可写实现

#### File 3 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/sharding/strategy/EgonColaShardingStrategyFactory.java`

- Purpose: 按 type 选择实现
- Symbols: `egonColaShardingStrategyFactory#create`
- Repository evidence: Spec Factory；现有显式 Bean
- Dependencies and consumers: Bootstrapper
- Why now: GREEN 选择
- Contract/signature changes: 注册 Map `<String, EgonColaShardingStrategy>`
- Input/output and state mapping: YAML type → Bean
- Error and edge behavior: 未知 type / NATIVE+STRATEGY 共存失败
- Standards impact: `MC-BEAN-001`, `MC-LOG-001`, `MC-PATTERN-001`, `MC-VALID-001`
- Literal rule enforcement: `Rule 4` `@Slf4j` `@Component("egonColaShardingStrategyFactory")` `@RequiredArgsConstructor`
  `@Qualifier`；`Rule 2` `ValidationUtils`；`Rule 9`
- Implementation pseudocode:

```java
@Slf4j
@Component("egonColaShardingStrategyFactory")
@RequiredArgsConstructor
public class EgonColaShardingStrategyFactory {
    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;
    @Qualifier("egonColaShardingStrategies")
    private final Map<String, EgonColaShardingStrategy> strategies;
    public EgonColaShardingStrategy create(String type) {
        EgonColaShardingStrategy strategy = strategies.get(type);
        if (strategy == null) throw new EgonColaMybatisPlusConfigurationException("UNKNOWN_SHARDING_STRATEGY");
        return strategy;
    }
}
```

- Verification contribution: File 1 GREEN
- After this file: 五实现可在同 Step 补齐或下一步；本 Step 至少注册接口与未知 type。五实现若本 Step 未写完，File 1 只测未知
  type。

#### File 4 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/sharding/strategy/EgonColaSingleTableShardingStrategy.java`

- Purpose: SINGLE/BROADCAST/STANDARD/COMPLEX/NATIVE 五个实现放在本文件块代表的同一提交内完成（其余四个同包同规则）
- Symbols: `EgonColaSingleTableShardingStrategy`, `EgonColaBroadcastReadOnlyShardingStrategy`,
  `EgonColaStandardTenantIdShardingStrategy`, `EgonColaComplexTenantThenBusinessShardingStrategy`,
  `EgonColaNativeYamlShardingStrategy`
- Repository evidence: Light `LongTenantShardingAlgorithm`、`TenantBusinessTableShardingAlgorithm`
- Dependencies and consumers: Factory Map
- Why now: 有可选择的实现
- Contract/signature changes: Bean 名等于 type
- Input/output and state mapping: COMPLEX 使用既有 `EgonColaTwoLevelRouteStrategy`
- Error and edge behavior: BROADCAST 写由 profile kind 拒绝；COMPLEX 缺根键失败
- Standards impact: `MC-PATTERN-001`, `MC-LOG-001`, `MC-BEAN-001`
- Literal rule enforcement: `Rule 4` 每个实现 `@Component("TYPE")` `@Slf4j` `@RequiredArgsConstructor`；`Rule 9`
- Implementation pseudocode:

```java
@Component("COMPLEX_TENANT_THEN_BUSINESS")
@RequiredArgsConstructor
@Slf4j
public class EgonColaComplexTenantThenBusinessShardingStrategy implements EgonColaShardingStrategy {
    @Qualifier("egonColaTwoLevelRouteStrategy")
    private final EgonColaTwoLevelRouteStrategy routeStrategy;
    public EgonColaRoutingProfileBO toRoutingProfile() { return /* Spec TENANT_ID_TWO_LEVEL profile */; }
}
```

- Verification contribution: Factory 能取出五 type
- After this file: Step 3 完成

- Validation working directory: 仓库根
- Verification command:
  `./mvnw -pl egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter -am -Dtest=EgonColaShardingStrategyFactoryTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0
- Failure returns to: File 3–4 注册
- Completion criteria: 未知 type 失败；五 type 可创建
- Rollback: 还原本 Step 路径
- Commit paths:
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/sharding/EgonColaShardingStrategyFactoryTest.java`,
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/sharding/strategy/EgonColaShardingStrategy.java`,
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/sharding/strategy/EgonColaShardingStrategyFactory.java`,
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/sharding/strategy/EgonColaSingleTableShardingStrategy.java`
- Commit: `feat(mybatis): add sharding strategy factory`

### Step 4 — 上收 Bootstrapper 与 CLASS_BASED 算法

- Requirements: `REQ-003`, `REQ-010`, `REQ-013`, `REQ-014`
- Dependencies: Step 3
- Baseline state: Light 仍有 bootstrap；Starter 无物理池
- Observable outcome: Starter 可按 Properties 建池、校验、失败关池；算法 FQCN 在 Starter 包
- End state: 可供 AutoConfig 调用；尚未强制装配
- Test-first gate: `Required` — 关池测试缺 Bootstrapper
- Manual Checks: `MC-REUSE-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 4`, `Rule 5`, `Rule 10`, `Rule 11`
- Ordered files:

#### File 1 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/sharding/EgonColaShardingDataSourceBootstrapperTest.java`

- Purpose: 失败关闭已开池
- Symbols: `closes_pools_when_topology_invalid`
- Repository evidence: Light `ShardingDataSourceBootstrapperTest`
- Dependencies and consumers: Bootstrapper
- Why now: RED 启动失败合同
- Contract/signature changes: `createDataSource(EgonColaShardingProperties)`
- Input/output and state mapping: 非法拓扑 → 无泄漏池
- Error and edge behavior: 校验失败 close
- Standards impact: `MC-TEST-001`, `MC-SCOPE-001`
- Literal rule enforcement: `Rule 11` 测试在 starter
- Implementation pseudocode:

```java
@Test
void closes_pools_when_topology_invalid() {
    when(factory.create(any())).thenReturn(Map.of("shard_0", ds));
    assertThatThrownBy(() -> bootstrapper.createDataSource(invalidProps));
    verify(factory).close(any());
}
```

- Verification contribution: Step 4 RED
- After this file: 缺类失败

#### File 2 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/sharding/bootstrap/EgonColaShardingDataSourceBootstrapper.java`

- Purpose: 物理池→校验→（下一步维护）→逻辑 DS
- Symbols: `EgonColaShardingDataSourceBootstrapper#createDataSource`
- Repository evidence: Light `ShardingDataSourceBootstrapper`
- Dependencies and consumers: AutoConfig
- Why now: GREEN 启动链
- Contract/signature changes: 入参改为 Starter Properties
- Input/output and state mapping: 同现顺序；本 Step 维护器可空调用点，Step 5 接入
- Error and edge behavior: 任意失败 close pools
- Standards impact: `MC-LOG-001`, `MC-BEAN-001`, `MC-REUSE-001`
- Literal rule enforcement: `Rule 4` `@Slf4j` `@RequiredArgsConstructor` 全字段 `@Qualifier`
- Implementation pseudocode:

```java
@Slf4j
@RequiredArgsConstructor
public final class EgonColaShardingDataSourceBootstrapper {
    public DataSource createDataSource(EgonColaShardingProperties properties) {
        Map<String, DataSource> physical = physicalDataSourceFactory.create(properties);
        try {
            var topology = topologyValidator.validate(properties);
            DataSource logical = logicalDataSourceFactory.create(physical, topology.yaml());
            readyTopology = topology;
            return logical;
        } catch (RuntimeException failure) {
            physicalDataSourceFactory.close(physical.values());
            throw failure;
        }
    }
}
```

- Verification contribution: File 1 GREEN
- After this file: 需要 factory/validator/algorithm 同提交

#### File 3 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/sharding/algorithm/EgonColaLongTenantShardingAlgorithm.java`

- Purpose: 把 Light CLASS_BASED 算法迁到 Starter 包（含 Complex 算法与
  PhysicalDataSourceFactory、TopologyValidator、YamlLoader、WriteTargetResolver）
- Symbols: `EgonColaLongTenantShardingAlgorithm`, `EgonColaTenantBusinessComplexShardingAlgorithm`
- Repository evidence: Light 同名算法
- Dependencies and consumers: SS YAML `algorithmClassName`
- Why now: YAML 不再引用脚手架包
- Contract/signature changes: FQCN 变更
- Input/output and state mapping: 委托 `EgonColaTwoLevelRouteStrategy`
- Error and edge behavior: 非正 tenant 失败
- Standards impact: `MC-REUSE-001`, `MC-LOG-001`
- Literal rule enforcement: `Rule 4` `@Slf4j`；`Rule 5` 无新工具
- Implementation pseudocode:

```java
@Slf4j
public final class EgonColaLongTenantShardingAlgorithm implements StandardShardingAlgorithm<Long> {
    public String doSharding(Collection<String> available, PreciseShardingValue<Long> value) {
        if (value.getValue() == null || value.getValue() <= 0) throw new IllegalArgumentException("SHARDING_KEY");
        return /* mix64 slot mapped node */;
    }
}
```

- Verification contribution: 与 two-level fixture 对齐的后续测试
- After this file: Step 4 完成可编译启动链

- Validation working directory: 仓库根
- Verification command:
  `./mvnw -pl egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter -am -Dtest=EgonColaShardingDataSourceBootstrapperTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0
- Failure returns to: File 2 关池
- Completion criteria: 非法拓扑关池
- Rollback: 还原本 Step
- Commit paths:
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/sharding/EgonColaShardingDataSourceBootstrapperTest.java`,
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/sharding/bootstrap/EgonColaShardingDataSourceBootstrapper.java`,
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/sharding/algorithm/EgonColaLongTenantShardingAlgorithm.java`
- Commit: `feat(mybatis): move sharding bootstrap and algorithms into starter`

### Step 5 — TableInfo 分片感知表结构维护

- Requirements: `REQ-016`, `REQ-017`, `REQ-018`
- Dependencies: Step 4
- Baseline state: 仅脚本 runner
- Observable outcome: 缺表 CREATE；缺可空列 ADD；缺 tenant_id 失败；不 DROP
- End state: Bootstrapper 在脚本前调用维护器
- Test-first gate: `Required` — 维护器不存在
- Manual Checks: `MC-NAME-001`, `MC-VALID-001`, `MC-TIME-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-SCOPE-001`,
  `MC-TEST-001`
- Literal Rules: `Rule 1`, `Rule 2`, `Rule 4`, `Rule 10`, `Rule 11`
- Ordered files:

#### File 1 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/schema/EgonColaTableInfoSchemaMaintainerTest.java`

- Purpose: 缺 tenant 与禁止 DROP
- Symbols: `rejects_model_without_tenant_id`, `does_not_emit_drop_column`
- Repository evidence: 现 `EgonColaPostgreDdlRunnerTest`
- Dependencies and consumers: Maintainer
- Why now: RED schema 合同
- Contract/signature changes: `maintain(EgonColaSchemaMaintainQuery)`
- Input/output and state mapping: TableInfo → SQL 动作
- Error and edge behavior: 缺 tenant 失败；dropExtraColumns true 失败
- Standards impact: `MC-VALID-001`, `MC-TEST-001`
- Literal rule enforcement: `Rule 2` 校验 Query
- Implementation pseudocode:

```java
@Test
void rejects_model_without_tenant_id() {
    assertThatThrownBy(() -> maintainer.maintain(queryWith(PoMissingTenant.class)))
        .hasMessageContaining("TENANT_ID_REQUIRED");
}
```

- Verification contribution: Step 5 RED
- After this file: 缺类失败

#### File 2 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/schema/EgonColaSchemaMaintainQuery.java`

- Purpose: 维护输入/输出 record
- Symbols: `EgonColaSchemaMaintainQuery`, `EgonColaSchemaMaintainResult`
- Repository evidence: 现 ddl BO records
- Dependencies and consumers: Maintainer
- Why now: 编译维护器
- Contract/signature changes: 新 record
- Input/output and state mapping: packages/profiles/dataSources
- Error and edge behavior: 空 packages 非法
- Standards impact: `MC-NAME-001`, `MC-MODEL-001`, `MC-VALID-001`
- Literal rule enforcement: `Rule 1` Query/Result；`Rule 3` record
- Implementation pseudocode:

```java
public record EgonColaSchemaMaintainQuery(
        @NotEmpty List<String> modelPackages,
        @NotEmpty Map<String, EgonColaRoutingProfileBO> profiles,
        @NotEmpty Map<String, DataSource> primaryDataSources) {}
```

- Verification contribution: 校验测试
- After this file: 可写维护器

#### File 3 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/schema/EgonColaTableInfoSchemaMaintainer.java`

- Purpose: 实际节点增量 DDL
- Symbols: `maintain`
- Repository evidence: `EgonColaPostgreDdlRunner` 锁与 PG 检查；PgVectorStore IF NOT EXISTS
- Dependencies and consumers: Bootstrapper
- Why now: GREEN
- Contract/signature changes: JDBC 写 `egon_schema_object`
- Input/output and state mapping: CREATE/ADD/SKIPPED；禁止 DROP
- Error and edge behavior: 类型冲突失败；非 PG 失败
- Standards impact: `MC-LOG-001`, `MC-BEAN-001`, `MC-TIME-001`, `MC-VALID-001`
- Literal rule enforcement: `Rule 4` `@Slf4j` `@Component("egonColaTableInfoSchemaMaintainer")`
  `@RequiredArgsConstructor` `@Qualifier`；`Rule 10` `Instant`/`Clock`
- Implementation pseudocode:

```java
@Slf4j
@Component("egonColaTableInfoSchemaMaintainer")
@RequiredArgsConstructor
public class EgonColaTableInfoSchemaMaintainer {
    public List<EgonColaSchemaMaintainResult> maintain(EgonColaSchemaMaintainQuery query) {
        validationUtils.validate(query);
        // lock, ensure egon_schema_object, diff information_schema vs TableInfo, CREATE/ADD, never DROP
        return results;
    }
}
```

- Verification contribution: File 1 GREEN
- After this file: Bootstrapper 下一步接线

- Validation working directory: 仓库根
- Verification command:
  `./mvnw -pl egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter -am -Dtest=EgonColaTableInfoSchemaMaintainerTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0
- Failure returns to: File 3 SQL 形状
- Completion criteria: 缺 tenant 失败；无 DROP
- Rollback: 还原本 Step
- Commit paths:
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/schema/EgonColaTableInfoSchemaMaintainerTest.java`,
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/schema/EgonColaSchemaMaintainQuery.java`,
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/schema/EgonColaTableInfoSchemaMaintainer.java`
- Commit: `feat(mybatis): maintain postgres tables from tableinfo on shard nodes`

### Step 6 — 强制装配 Sharding DataSource

- Requirements: `REQ-003`, `REQ-009`, `REQ-015`, `REQ-017`, `REQ-022`
- Dependencies: Step 5
- Baseline state: 空 `egonColaRoutingProfiles`；无 SS AutoConfig
- Observable outcome: 合法 YAML 产生 `@Primary` SS DataSource；缺 YAML/`enabled=false` 启动失败；无 IDdl Bean
- End state: 消费者只需依赖 Starter
- Test-first gate: `Required` — ContextRunner 当前仍能在无分片 YAML 下起来
- Manual Checks: `MC-BEAN-001`, `MC-CONFIG-001`, `MC-DEP-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 4`, `Rule 7`, `Rule 11`
- Ordered files:

#### File 1 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaShardingAutoConfigurationTest.java`

- Purpose: 缺 YAML 失败；有 YAML 才有 DataSource
- Symbols: `fails_without_sharding_yaml`, `creates_primary_datasource_with_valid_yaml`
- Repository evidence: `EgonColaMybatisPlusAutoConfigurationTest` 的 `ApplicationContextRunner`
- Dependencies and consumers: 新 AutoConfig
- Why now: RED 强制耦合
- Contract/signature changes: 加载即要求分片配置
- Input/output and state mapping: 无 YAML → 失败；有 YAML → DataSource Bean
- Error and edge behavior: 不降级 plain DS
- Standards impact: `MC-TEST-001`, `MC-CONFIG-001`
- Literal rule enforcement: `Rule 7` 测试键；`Rule 11`
- Implementation pseudocode:

```java
@Test
void fails_without_sharding_yaml() {
    ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EgonColaMybatisPlusAutoConfiguration.class, EgonColaShardingAutoConfiguration.class));
    runner.run(context -> assertThat(context.getStartupFailure())
            .hasMessageContaining("SHARDING_REQUIRED"));
}
```

- Verification contribution: Step 6 RED
- After this file: 现 AutoConfig 仍成功则测试失败

#### File 2 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaShardingAutoConfiguration.java`

- Purpose: 强制发布逻辑 DS 与 profiles
- Symbols: `dataSource`, `egonColaRoutingProfiles`, `egonColaWriteTargetResolver`
- Repository evidence: Light `ShardingSphereDataSourceConfiguration`
- Dependencies and consumers: Spring、MP
- Why now: GREEN
- Contract/signature changes: `@Primary DataSource`
- Input/output and state mapping: Properties → Bootstrapper → Bean
- Error and edge behavior: 缺配置不创建 Bean 且失败
- Standards impact: `MC-BEAN-001`, `MC-LOG-001`
- Literal rule enforcement: `Rule 4` 显式 `@Bean("dataSource")` 等
- Implementation pseudocode:

```java
@AutoConfiguration(after = EgonColaMybatisPlusAutoConfiguration.class)
@EnableConfigurationProperties(EgonColaShardingProperties.class)
@Slf4j
public class EgonColaShardingAutoConfiguration {
    @Bean("dataSource")
    @Primary
    DataSource dataSource(@Qualifier("egonColaShardingDataSourceBootstrapper")
            EgonColaShardingDataSourceBootstrapper bootstrapper,
            EgonColaShardingProperties properties) {
        return bootstrapper.createDataSource(properties);
    }
}
```

- Verification contribution: File 1 缺 YAML 仍失败；补测试 YAML 后可成功路径
- After this file: 需 imports 注册

#### File 3 —
`MODIFY egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

- Purpose: 注册 Sharding AutoConfig；删除空 profiles 默认
- Symbols: import 行
- Repository evidence: 现仅 MP AutoConfig
- Dependencies and consumers: Boot 加载
- Why now: 接线
- Contract/signature changes: 新增一行
- Input/output and state mapping: 启动加载两个 AutoConfig
- Error and edge behavior: 漏注册则强制失败测试误通过
- Standards impact: `MC-BEAN-001`
- Literal rule enforcement: `Rule 11` 仍用 Boot 3 imports
- Implementation pseudocode:

```text
# Boot 3.5 imports file, one FQCN per line, loaded for every consumer of this starter.
top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusAutoConfiguration
top.egon.cola.component.common.mybatis.autoconfigure.EgonColaShardingAutoConfiguration
```

- Verification contribution: ContextRunner 加载新配置
- After this file: 同时修改 MP AutoConfig 删除空 Map 默认 resolver

#### File 4 —
`MODIFY egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusAutoConfiguration.java`

- Purpose: 删除 plain `egonColaRoutingProfiles` 空 Map 与 plain WriteTargetResolver
- Symbols: `egonColaRoutingProfiles`, `egonColaWriteTargetResolver`
- Repository evidence: 现返回 `Map.of()` 与 plain fingerprint
- Dependencies and consumers: LOCAL 守卫
- Why now: 无 SS 剖面必须消失
- Contract/signature changes: 两 Bean 改由 Sharding AutoConfig 提供
- Input/output and state mapping: 无默认
- Error and edge behavior: 缺 Sharding AutoConfig 则守卫缺 Bean 失败
- Standards impact: `MC-SCOPE-001`, `MC-BEAN-001`
- Literal rule enforcement: `Rule 11` 不把守卫改到脚手架
- Implementation pseudocode:

```java
public class EgonColaMybatisPlusAutoConfiguration {
    // delete Bean methods egonColaRoutingProfiles() and egonColaWriteTargetResolver()
    // keep interceptor, DDL runner, and dynamic-table-name beans
    // LocalWriteGuard then requires Sharding AutoConfig to supply profiles or context fails
}
```

- Verification contribution: 无 YAML 启动失败
- After this file: Step 6 完成；动态表名 Bean 保留默认关

- Validation working directory: 仓库根
- Verification command:
  `./mvnw -pl egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter -am -Dtest=EgonColaShardingAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0
- Failure returns to: File 2–4
- Completion criteria: 缺 YAML 失败；无 IDdl Bean
- Rollback: 还原本 Step
- Commit paths:
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaShardingAutoConfigurationTest.java`,
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaShardingAutoConfiguration.java`,
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`,
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusAutoConfiguration.java`
- Commit: `feat(mybatis): require shardingsphere datasource autoconfig`

### Step 7 — Light 脚手架只消费 Starter

- Requirements: `REQ-004`, `REQ-005`, `REQ-020`
- Dependencies: Step 6
- Baseline state: Light 有 14 个 datasource 类与双 YAML
- Observable outcome: Light 无本地 bootstrap；一份 YAML；POM 只引新 Starter
- End state: 其余脚手架尚未改
- Test-first gate: `Required` — 架构测试仍允许本地 `ShardingDataSourceBootstrapper`
- Manual Checks: `MC-ARCH-001`, `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 7`, `Rule 11`
- Ordered files:

#### File 1 —
`MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/architecture/LightPersistenceArchitectureTest.java`

- Purpose: 禁止本地 SS bootstrap 与双 YAML
- Symbols: `generatedProjectUsesCommonPersistenceOwnership`
- Repository evidence: 现测试已禁 JPA
- Dependencies and consumers: 生成/源码树
- Why now: RED 复制类仍在
- Contract/signature changes: 断言不含 `ShardingDataSourceBootstrapper`，含 Starter 算法 FQCN
- Input/output and state mapping: 源码扫描
- Error and edge behavior: 残留复制类失败
- Standards impact: `MC-ARCH-001`, `MC-TEST-001`
- Literal rule enforcement: `Rule 11` 保持 Light 包
- Implementation pseudocode:

```java
assertFalse(source.contains("class ShardingDataSourceBootstrapper"));
assertTrue(source.contains("egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter")
        || Files.readString(Path.of("pom.xml")).contains("egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter"));
```

- Verification contribution: Step 7 RED
- After this file: 因复制类仍在而失败

#### File 2 —
`CREATE egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/egon-mybatis-plus-sharding.yml`

- Purpose: 合并物理源与规则为一份 STRATEGY YAML
- Symbols: `egon.cola.component.mybatis-plus.sharding`
- Repository evidence: `datasource/sharding.yml` + `sharding/shardingsphere-sharding.yml`
- Dependencies and consumers: `spring.config.import`
- Why now: 单文件
- Contract/signature changes: 删除 `app.sharding`
- Input/output and state mapping: 现物理 URL 环境变量迁到新前缀
- Error and edge behavior: 缺键启动失败
- Standards impact: `MC-CONFIG-001`
- Literal rule enforcement: `Rule 7` 随后同步 dev/test/prod
- Implementation pseudocode:

```yaml
egon.cola.component.mybatis-plus.sharding:
  enabled: true
  mode: ${APP_DATASOURCE_MODE:SHARDING}
  config-style: STRATEGY
  transaction:
    default-type: LOCAL
  data-sources: [/* current physical entries */]
  tables:
    users: { type: SINGLE, data-source: master_data }
    school_classes: { type: STANDARD_TENANT_ID, sharding-column: tenant_id }
```

- Verification contribution: 启动配置
- After this file: 旧双文件仍在，下一步删除

#### File 3 —
`DELETE egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/infrastructure/config/datasource/ShardingDataSourceBootstrapper.java`

- Purpose: 删除 Light 复制启动链（同目录其余 13 个 SS 类与双 YAML、直连 SS POM 在本提交一并删除/修改）
- Symbols: 原 Light datasource 包
- Repository evidence: 14 个 Java 类型
- Dependencies and consumers: 无，改由 Starter AutoConfig
- Why now: File 1 要求缺类
- Contract/signature changes: 本地类消失
- Input/output and state mapping: 无
- Error and edge behavior: 漏删则架构测试失败
- Standards impact: `MC-ARCH-001`, `MC-SCOPE-001`
- Literal rule enforcement: `Rule 11` infrastructure 不再实现 SS
- Implementation pseudocode:

```text
delete Light datasource SS types and tests
remove shardingsphere direct dependencies from Light pom
import classpath:egon-mybatis-plus-sharding.yml once
align application.yml/dev/test/prod keys
```

- Verification contribution: File 1 GREEN
- After this file: Light 消费 Starter

- Validation working directory: 仓库根
- Verification command:
  `./mvnw -pl egon-cola-archetypes/source-projects/egon-cola-source-light -am -Dtest=LightPersistenceArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0
- Failure returns to: File 2–3 残留类或键
- Completion criteria: Light 无 bootstrap 复制；单 YAML
- Rollback: 还原 Light 路径
- Commit paths:
  `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/architecture/LightPersistenceArchitectureTest.java`,
  `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/egon-mybatis-plus-sharding.yml`,
  `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/infrastructure/config/datasource/ShardingDataSourceBootstrapper.java`
- Commit: `refactor(light): consume sharding jdbc starter and drop local adapter`

### Step 8 — 其余脚手架、Agent、README 与 verifier

- Requirements: `REQ-004`, `REQ-008`, `REQ-019`, `REQ-020`
- Dependencies: Step 7
- Baseline state: Light 已切；其余五套与 Agent 未切
- Observable outcome: 全部消费者无本地 SS 适配；Agent 有合法 SS YAML；README 含推荐配置
- End state: 实施完成，待人工 PG
- Test-first gate: `Required` — Agent 架构测试仍禁止 `shardingsphere` 文本
- Manual Checks: `MC-ARCH-001`, `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 7`, `Rule 11`
- Ordered files:

#### File 1 —
`MODIFY egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/test/java/top/egon/cola/archetype/source/agent/architecture/AgentArchitectureTest.java`

- Purpose: 允许 Starter/SS 合法引用，仍禁止 Agent 自建 bootstrap
- Symbols: `has_no_forbidden_integrations_or_source_web_business`
- Repository evidence: 现禁止任意 `shardingsphere` 子串
- Dependencies and consumers: Agent 源码/POM
- Why now: RED 与强制 SS 冲突
- Contract/signature changes: 禁词改为本地 `ShardingDataSourceBootstrapper`/`YamlShardingSphereDataSourceFactory`
  自建，不再禁坐标名
- Input/output and state mapping: 扫描 xml/java
- Error and edge behavior: 自建 SS 仍失败
- Standards impact: `MC-ARCH-001`, `MC-TEST-001`
- Literal rule enforcement: `Rule 11` 保持 Agent 六模块
- Implementation pseudocode:

```java
@Test
void has_no_forbidden_integrations_or_source_web_business() throws IOException {
    String content = read(path);
    assertFalse(content.contains("ShardingDataSourceBootstrapper"), path + " rebuilt SS bootstrap");
    assertFalse(content.contains("YamlShardingSphereDataSourceFactory"), path + " created SS datasource locally");
}
```

- Verification contribution: Step 8 RED（POM 仍旧坐标时也可能失败，随后切换）
- After this file: 需加 YAML 与 POM

#### File 2 —
`CREATE egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-infrastructure/src/main/resources/egon-mybatis-plus-sharding.yml`

- Purpose: Agent 单 PRIMARY + 知识表 SINGLE
- Symbols: sharding YAML
- Repository evidence: Agent 现 Flyway 单库
- Dependencies and consumers: Agent starter import
- Why now: `REQ-019`
- Contract/signature changes: 新增强制分片配置
- Input/output and state mapping: 现 knowledge JDBC URL → data-sources
- Error and edge behavior: 缺文件启动失败
- Standards impact: `MC-CONFIG-001`
- Literal rule enforcement: `Rule 7` 同步 Agent profiles
- Implementation pseudocode:

```yaml
egon.cola.component.mybatis-plus.sharding:
  enabled: true
  mode: SHARDING
  config-style: STRATEGY
  transaction: { default-type: LOCAL }
  tables:
    knowledge_base: { type: SINGLE, data-source: primary }
    knowledge_document: { type: SINGLE, data-source: primary }
```

- Verification contribution: Agent 可启动合同
- After this file: 需改 POM 坐标

#### File 3 —
`MODIFY egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/README.md`

- Purpose: 推荐 STRATEGY、强制 SS、XA 可选、订单两级示例（中文 README 同步）
- Symbols: 文档示例
- Repository evidence: 现 README 写 Common 无 SS
- Dependencies and consumers: 外部开发者
- Why now: `REQ-008`
- Contract/signature changes: 文档合同
- Input/output and state mapping: 与 Properties 键一致
- Error and edge behavior: 写明 exclusion SS 会失败
- Standards impact: `MC-CONFIG-001`, `MC-SCOPE-001`
- Literal rule enforcement: `Rule 7` 示例含全部核心键
- Implementation pseudocode:

```markdown
Consuming this starter requires PostgreSQL + MyBatis-Plus + ShardingSphere-JDBC.
Excluding shardingsphere-jdbc is unsupported and must fail at compile or startup.
Recommended STRATEGY yaml: tenant_id then order_id for orders/order_items.
```

- Verification contribution: `TEST-013` 文本断言可在同提交测试中做
- After this file: 其余脚手架按 `PLAN-CLAR-002` 删除复制类并添加同构 YAML（本提交包含那些路径，见 commit paths
  目录范围说明：实施时将五套 `config/datasource` 与双 YAML 一并纳入本 Step 工作树）

- Validation working directory: 仓库根
- Verification command:
  `./mvnw -pl egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter,egon-cola-archetypes/source-projects/egon-cola-source-light -am -Dtest=AgentArchitectureTest,LightPersistenceArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: 两测试 exit 0
- Failure returns to: File 1–3 或未列出但同 Step 的其余脚手架删除
- Completion criteria: 无本地 bootstrap；Agent 有 SS YAML；README 不再声称无 SS
- Rollback: 还原本 Step 与同构脚手架路径
- Commit paths:
  `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/test/java/top/egon/cola/archetype/source/agent/architecture/AgentArchitectureTest.java`,
  `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-infrastructure/src/main/resources/egon-mybatis-plus-sharding.yml`,
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/README.md`
- Commit: `refactor(archetypes): require sharding jdbc starter for all archetypes and agent`

## 8. Test, Validation, and Quality Gates

| Gate/order        | Working directory | Command or method                                                                                                                                   | Scope   | Expected result              | Failure returns to | Requirements/runtime boundary |
|-------------------|-------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|---------|------------------------------|--------------------|-------------------------------|
| RED/GREEN Step 1  | 仓库根               | `./mvnw -pl ...-sharding-jdbc-ext-spring-boot-starter -am -Dtest=EgonColaShardingDependencyContractTest test`                                       | POM     | exit 0                       | Step 1             | `REQ-001`; 模块                 |
| RED/GREEN Step 2  | 仓库根               | `... -Dtest=EgonColaShardingPropertiesTest test`                                                                                                    | 绑定      | exit 0                       | Step 2             | `REQ-022`                     |
| RED/GREEN Step 3  | 仓库根               | `... -Dtest=EgonColaShardingStrategyFactoryTest test`                                                                                               | 策略      | exit 0                       | Step 3             | `REQ-006`                     |
| RED/GREEN Step 4  | 仓库根               | `... -Dtest=EgonColaShardingDataSourceBootstrapperTest test`                                                                                        | 启动      | exit 0                       | Step 4             | `REQ-003`                     |
| RED/GREEN Step 5  | 仓库根               | `... -Dtest=EgonColaTableInfoSchemaMaintainerTest test`                                                                                             | schema  | exit 0                       | Step 5             | `REQ-016`                     |
| RED/GREEN Step 6  | 仓库根               | `... -Dtest=EgonColaShardingAutoConfigurationTest test`                                                                                             | 装配      | exit 0                       | Step 6             | `REQ-022`                     |
| RED/GREEN Step 7  | 仓库根               | `./mvnw -pl egon-cola-archetypes/source-projects/egon-cola-source-light -am -Dtest=LightPersistenceArchitectureTest test`                           | Light   | exit 0                       | Step 7             | `REQ-004`                     |
| RED/GREEN Step 8  | 仓库根               | Agent+Light 架构测试命令见 Step 8                                                                                                                          | 消费者     | exit 0                       | Step 8             | `REQ-019`                     |
| Module regression | 仓库根               | `./mvnw -pl egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter -am test` | Starter | 既有+新测试通过；不把 skip 的 PG IT 当证明 | 拥有 Step            | 模块                            |
| Manual PG/SS      | 人工                | 既有 `egon.pg.routing=true`                                                                                                                           | 真库      | 不在本 Plan 执行                  | 运行后                | 运行时未验证                        |

## 9. Migration, Compatibility, Rollout, and Rollback

旧 B/V/manual 文件不改。`egon_schema_object` 由维护器 `CREATE TABLE IF NOT EXISTS`，不新增 Flyway 版本。Maven
先发新坐标再切消费者。回滚：逐 Step revert；已加列保留。应用 rollback 不能撤销物理节点建表。`REQ-021`：本 Plan 不连库、不 start
服务。

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Effective Spec section | Steps        | Files                   | Tests/gates            | Completion evidence |
|-------------|------------------------|--------------|-------------------------|------------------------|---------------------|
| `REQ-001`   | Spec §4                | Step 1       | rename/BOM              | DependencyContractTest | 新坐标                 |
| `REQ-002`   | Spec §4                | Step 1, 7, 8 | POM                     | 无直连 SS                 | 脚手架 POM             |
| `REQ-003`   | Spec §4                | Step 4, 6    | Bootstrapper/AutoConfig | AutoConfigurationTest  | Primary DS          |
| `REQ-004`   | Spec §4                | Step 7, 8    | DELETE 复制类              | Light/Agent 架构测试       | 无 bootstrap         |
| `REQ-005`   | Spec §4                | Step 2, 7, 8 | 单 YAML                  | 键齐                     | 一份文件                |
| `REQ-006`   | Spec §4                | Step 3       | Factory                 | FactoryTest            | 未知 type             |
| `REQ-007`   | Spec §4                | Step 3       | NATIVE 策略               | FactoryTest            | 互斥                  |
| `REQ-008`   | Spec §4                | Step 8       | README                  | 文档                     | 示例                  |
| `REQ-009`   | Spec §4                | Step 1, 6    | POM                     | 无 xa exclusion         | LOCAL 默认            |
| `REQ-010`   | Spec §4                | Step 4       | mode READWRITE          | Bootstrapper           | 事务读主合同              |
| `REQ-011`   | Spec §4                | Step 3       | 五策略                     | FactoryTest            | 互斥归属                |
| `REQ-012`   | Spec §4                | Step 3, 4    | COMPLEX/算法              | 策略测试                   | 两级                  |
| `REQ-013`   | Spec §4                | Step 4, 6    | resolver                | 守卫既有+profiles          | 跨组失败                |
| `REQ-014`   | Spec §4                | Step 4       | fingerprint             | 拓扑校验                   | 变更拒绝                |
| `REQ-015`   | Spec §4                | Step 6       | 动态表名 Bean 保留            | 既有测试                   | 默认关                 |
| `REQ-016`   | Spec §4                | Step 5       | Maintainer              | MaintainerTest         | CREATE/ADD          |
| `REQ-017`   | Spec §4                | Step 5, 6    | runner 保留               | 无 IDdl                 | 脚本                  |
| `REQ-018`   | Spec §4                | Step 5       | tenant 扫描               | MaintainerTest         | 缺列失败                |
| `REQ-019`   | Spec §4                | Step 8       | Agent YAML              | AgentArchitectureTest  | SS DS               |
| `REQ-020`   | Spec §4                | Step 7, 8    | profiles                | 键齐                     | Rule 7              |
| `REQ-021`   | Spec §4                | 全部           | 无运行时命令                  | 本 Plan 不连库             | 文档                  |
| `REQ-022`   | Spec §4                | Step 1, 6    | 硬依赖+fail-fast           | AutoConfig/POM 测试      | 排除 SS 失败            |

## 11. Risks, Blockers, and User Decisions

| ID          | Risk or decision        | Impacted Steps/files | Evidence                 | Owner | Status/action         |
|-------------|-------------------------|----------------------|--------------------------|-------|-----------------------|
| `RISK-001`  | TableInfo 与手写 SQL 类型不一致 | Step 5               | Spec `RISK-001`          | 实施者   | Closed in design：冲突失败 |
| `RISK-002`  | 其余脚手架与 Light 不完全同构      | Step 8               | `PLAN-CLAR-002`          | 实施者   | Closed：按实际包名替换        |
| `BLOCK-001` | 无未关闭用户决策                | 无                    | Spec `DEC-009`/`DEC-010` | User  | Closed                |

## 12. Review and Acceptance

### 12.1 Original requirement fidelity

覆盖重命名、强制 PG+MP+SS+ext、单 YAML、策略、表维护、六套+Agent、排除 SS 失败。未规划 MP-only。

### 12.2 Spec consistency

未新增外部 API 或业务表列。`PLAN-CLAR-001`–`004` 仅路径/同构/Agent SINGLE/H2 插件单测边界。

### 12.3 Repository executability

路径来自当前 Light bootstrap 与 common starter。命令使用仓库 `./mvnw -pl`。真实 PG 不在本 Plan 执行。

### 12.4 Test and release completeness

每 Step RED/GREEN；模块回归在 Starter；PG 人工。旧 SQL 不可变。

### 12.5 Blocking Manual Check

| Check ID         | Applicability  | Status | Evidence                         | Finding          | Required action/exception |
|------------------|----------------|--------|----------------------------------|------------------|---------------------------|
| `MC-ARCH-001`    | Applicable     | PASS   | §4.7 starter + 精确 archetype 脚手架  | 无混合层             | None                      |
| `MC-REUSE-001`   | Applicable     | PASS   | §4.7 ledger                      | 上收 Light/SS/MP   | None                      |
| `MC-DEP-001`     | Applicable     | PASS   | SS 5.5.3 上收；无 xa exclusion       | 批准缺口             | None                      |
| `MC-NAME-001`    | Applicable     | PASS   | Strategy/Query/Result/Properties | 无 Data/Info      | None                      |
| `MC-VALID-001`   | Applicable     | PASS   | Properties/Query/Factory         | 层边界校验            | None                      |
| `MC-MODEL-001`   | Applicable     | PASS   | Properties 完整 Lombok；record      | 无静默删注解           | None                      |
| `MC-CONVERT-001` | Not applicable | N/A    | 无新 Converter                     | YAML 绑定          | None                      |
| `MC-LOG-001`     | Applicable     | PASS   | Bootstrapper/Maintainer/Factory  | `@Slf4j`         | None                      |
| `MC-BEAN-001`    | Applicable     | PASS   | 显式 Bean/`@Qualifier`             | lombok.config 已有 | None                      |
| `MC-UTIL-001`    | Applicable     | PASS   | JDK SHA-256                      | 无新工具库            | None                      |
| `MC-JSON-001`    | Applicable     | PASS   | 无新外部 JSON                        | 内部 manifest      | None                      |
| `MC-TIME-001`    | Applicable     | PASS   | Instant/Clock                    | 维护表              | None                      |
| `MC-CONFIG-001`  | Applicable     | PASS   | 单 prefix；Step 7–8 键齐             | Rule 7           | None                      |
| `MC-PATTERN-001` | Applicable     | PASS   | Strategy+Factory                 | Step 3           | None                      |
| `MC-SCOPE-001`   | Applicable     | PASS   | 不改业务 API/旧 SQL                   | 范围               | None                      |
| `MC-TEST-001`    | Applicable     | PASS   | 每 Step 精确 `-Dtest`               | 不执行 PG           | None                      |
| `MC-BLOCKER-001` | Applicable     | PASS   | §11 Closed                       | 无 open           | None                      |

### 12.6 Final verdict

PASS — Ready for user review
