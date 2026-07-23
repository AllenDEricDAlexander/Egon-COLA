# Archetype 接入 ShardingSphere JDBC、Flyway 与 UUIDv7 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 Light、Web、Service 三个 archetype 生成的项目同时支持默认单数据源、数据分片、读写分离、物理节点 Flyway 初始化和应用侧 UUIDv7 主键，并以 2N（2n 法）稳定槽位契约支持后续 `N → 2N` 平滑迁移。

**Architecture:** 默认 profile 继续由 Spring Boot 管理单数据源和 Flyway；启用 `sharding` 后，由 Infrastructure 内的 Facade 按“创建物理 Hikari 数据源 → 校验拓扑 → 逐个 primary 执行 Flyway → 用公开 YAML Factory 创建 ShardingSphere 逻辑数据源”的顺序启动。分片路由使用 `CLASS_BASED` Strategy，将 UUIDv7 稳定散列到追加式 `nodeMap`；读写分离使用独立规则 YAML，事务内读取固定走 primary，非事务查询走 replica。

**Tech Stack:** Java 21、Spring Boot 3.5.16、Spring Data JPA、Apache ShardingSphere JDBC 5.5.3、Flyway、HikariCP、PostgreSQL、H2、JUnit 5、AssertJ、Mockito、Maven Invoker。

## Global Constraints

1. 设计基线为 `docs/superpowers/specs/2026-07-23-archetype-shardingsphere-flyway-design.md`，实现不得自行更换分片键、表分类、事务策略或 migration 命名规则。
2. 只修改 `src/main/resources/archetype-resources`、共享 facade 源码及三个 `verify.groovy`；不直接修改或提交任何 `target/` 生成产物。
3. 三个模板都必须保留默认单数据源模式；`sharding`、`readwrite` 是叠加 profile，`readwrite` 单独启用必须失败。
4. 不引入 XA、BASE、Seata、Saga 框架或 ShardingSphere 分布式事务模块。
5. 不引入 ShardingSphere 内部 YAML 类型，也不新增 SnakeYAML 直接依赖；逻辑数据源只通过公开 API `YamlShardingSphereDataSourceFactory.createDataSource(Map<String, DataSource>, byte[])` 创建。
6. ShardingSphere JDBC 固定为 `5.5.3`；业务主键生成复用现有 `top.egon:egon-cola-component-common-id`、`IdGenerator` 和 `UuidV7Generator`。
7. 数据库数、每库表数和总节点数必须为 `2` 的幂；初始拓扑固定为 `2 库 × 2 表 = 4 节点`。
8. 新旧路由映射兼容性必须满足：`newNodeCount = oldNodeCount × 2`、`newMappingVersion > oldMappingVersion`、旧槽位映射前缀完全不变。
9. Flyway 只迁移 primary；replica 只能是 primary 的数据库复制节点，不允许作为独立 migration target。
10. 每个 SQL 文件在首条 SQL 前写明“变更内容、影响范围、兼容性说明”，文件名使用 `VyyyyMMdd_NNN__lower_snake_case_description.sql`。
11. 本仓库中的六个原始 `V1/V2` 属于未执行的 archetype 模板；按已确认需求直接替换为最终初始化 schema，不保留旧模型和搬数 SQL。
12. 每个任务只提交一次；提交前先执行该任务列出的最小验证。全量验证只在最后一个任务执行。
13. archetype YAML 中的运行时 Spring placeholder 必须遵循现有 Velocity 写法：文件首部声明 `#set( $symbol_dollar = '$' )`，正文写 `${symbol_dollar}{ENV_NAME:default}`；不能把 `${ENV_NAME}` 直接写入模板源码。

---

### Task 1: 增加 ShardingSphere 依赖与 2N 路由核心

**Files:**

- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-common/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-common/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/pom.xml`
- Create in each archetype's `infrastructure/config/datasource` main package: `ShardingNodeMap.java`
- Create in each archetype's `infrastructure/config/datasource` main package: `ShardingNodeMapCompatibilityValidator.java`
- Create in each archetype's `infrastructure/config/datasource` main package: `UuidV7BucketShardingAlgorithm.java`
- Create matching tests in each archetype's `infrastructure/config/datasource` test package: `ShardingNodeMapTest.java`
- Create matching tests in each archetype's `infrastructure/config/datasource` test package: `ShardingNodeMapCompatibilityValidatorTest.java`
- Create matching tests in each archetype's `infrastructure/config/datasource` test package: `UuidV7BucketShardingAlgorithmTest.java`

The concrete roots are:

```text
egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/config/datasource
egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/infrastructure/config/datasource
egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/config/datasource
egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource
egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/config/datasource
egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource
```

- [ ] **Step 1: 先写 2N 路由失败测试**

固定以下断言：

1. 初始映射必须精确为：

   ```text
   0=shard_0:0,1=shard_0:1,2=shard_1:0,3=shard_1:1
   ```

2. `nodeCount` 不是 `2` 的幂、槽位不连续、完整物理节点重复、数据库数或每库表数不是 `2` 的幂时构造失败。
3. `4 → 8` 时每个键的 `newSlot` 只能是 `oldSlot` 或 `oldSlot + 4`。
4. 新版本未递增、不是翻倍扩容、覆盖旧槽位时 `assertCanExpandTo` 失败。
5. UUID 不是 v7、空值、范围路由时失败。
6. database 策略和 table 策略对同一 UUIDv7 选中同一完整物理节点。

- [ ] **Step 2: 增加依赖**

Light 生成 POM 直接增加：

```xml
<shardingsphere.version>5.5.3</shardingsphere.version>
```

```xml
<dependency>
    <groupId>org.apache.shardingsphere</groupId>
    <artifactId>shardingsphere-jdbc</artifactId>
    <version>${shardingsphere.version}</version>
</dependency>
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-common-id</artifactId>
</dependency>
```

Web、Service 的生成父 POM 只管理 `shardingsphere-jdbc:5.5.3`；各自 infrastructure POM 引入 ShardingSphere JDBC，各自 common POM 引入 `egon-cola-component-common-id`。这样 Infrastructure 可经 Domain → Common 依赖链注册 `UuidV7Generator`，Application 也可经 Domain → Common 注入 `IdGenerator`，Domain 源码本身不导入主键组件。

- [ ] **Step 3: 实现不可变节点映射**

核心模型固定为：

```java
public record ShardingNodeMap(
        int mappingVersion,
        int nodeCount,
        Map<Integer, PhysicalNode> nodes) {

    public record PhysicalNode(String database, int tableSuffix) {
    }

    public static ShardingNodeMap parse(Properties properties) {
        return parse(
                properties.getProperty("mapping-version"),
                properties.getProperty("node-count"),
                properties.getProperty("node-map"));
    }

    public PhysicalNode route(String shardingKey) {
        UUID uuid = UUID.fromString(shardingKey);
        if (uuid.version() != 7) {
            throw new IllegalArgumentException("sharding key must be UUIDv7");
        }
        int hash = shardingKey.hashCode();
        int spreadHash = hash ^ (hash >>> 16);
        return nodes.get(spreadHash & (nodeCount - 1));
    }
}
```

实现时补齐构造校验、不可变拷贝和不含敏感信息的异常消息。禁止使用 `Math.abs(hash)`，避免 `Integer.MIN_VALUE` 边界；必须使用位掩码。

- [ ] **Step 4: 实现 2N 兼容性校验**

```java
public final class ShardingNodeMapCompatibilityValidator {

    public void assertCanExpandTo(ShardingNodeMap current, ShardingNodeMap candidate) {
        if (candidate.mappingVersion() <= current.mappingVersion()) {
            throw new IllegalArgumentException("mapping version must increase");
        }
        if (candidate.nodeCount() != current.nodeCount() * 2) {
            throw new IllegalArgumentException("node count must expand from N to 2N");
        }
        current.nodes().forEach((slot, node) -> {
            if (!node.equals(candidate.nodes().get(slot))) {
                throw new IllegalArgumentException("existing slot mapping must not change: " + slot);
            }
        });
    }
}
```

- [ ] **Step 5: 实现 ShardingSphere Strategy**

`UuidV7BucketShardingAlgorithm` 实现 `StandardShardingAlgorithm<String>`：

```java
@Override
public String doSharding(
        Collection<String> availableTargetNames,
        PreciseShardingValue<String> shardingValue) {
    ShardingNodeMap.PhysicalNode node = nodeMap.route(shardingValue.getValue());
    return switch (target) {
        case DATABASE -> selectDatabase(availableTargetNames, node.database());
        case TABLE -> selectTable(availableTargetNames, node.tableSuffix());
    };
}

@Override
public Collection<String> doSharding(
        Collection<String> availableTargetNames,
        RangeShardingValue<String> shardingValue) {
    throw new UnsupportedOperationException("range sharding is not supported");
}
```

table target 必须从 `availableTargetNames` 中选择后缀为 `_<tableSuffix>` 的真实表名；不得自行拼接逻辑表名。database target 必须精确匹配 `shard_N`。

- [ ] **Step 6: 运行三个 archetype 的最小生成测试**

Run:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl :egon-cola-archetype-light,:egon-cola-archetype-web,:egon-cola-archetype-service -am integration-test
```

Expected: 三个生成项目编译，2N 路由单元测试通过。

- [ ] **Step 7: 提交**

```bash
git add egon-cola-archetypes/egon-cola-archetype-light egon-cola-archetypes/egon-cola-archetype-web egon-cola-archetypes/egon-cola-archetype-service
git commit -m "feat(archetype): 增加2n稳定分片路由"
```

---

### Task 2: 实现物理数据源、Flyway 与逻辑数据源启动编排

**Files:**

- Create in each Task 1 main datasource root: `ShardingDataSourceProperties.java`
- Create in each Task 1 main datasource root: `PhysicalDataSourceFactory.java`
- Create in each Task 1 main datasource root: `ShardingYamlLoader.java`
- Create in each Task 1 main datasource root: `ShardingTopologyValidator.java`
- Create in each Task 1 main datasource root: `PhysicalDataSourceFlywayMigrator.java`
- Create in each Task 1 main datasource root: `ShardingDataSourceBootstrapper.java`
- Create in each Task 1 main datasource root: `ShardingSphereDataSourceConfiguration.java`
- Create in each Task 1 main datasource root: `ReadwriteProfileGuard.java`
- Create `ShardingDataSourcePropertiesTest.java`, `PhysicalDataSourceFactoryTest.java`, `ShardingYamlLoaderTest.java`, `ShardingTopologyValidatorTest.java`, `PhysicalDataSourceFlywayMigratorTest.java`, `ShardingDataSourceBootstrapperTest.java` and `ReadwriteProfileGuardTest.java` in each Task 1 test datasource root.

- [ ] **Step 1: 写启动顺序和失败关闭测试**

测试必须证明：

1. Spring 列表配置创建具名 `Map<String, DataSource>`，名称重复或连接属性缺失时失败。
2. topology validator 要求每个 primary 恰好一个 Flyway target，replica 零个 target。
3. `logicalName` 覆盖 single 和 nodeMap 中全部 `shard_N`，每组只有一个 primary。
4. target 按 `data-source-name` 排序，Flyway 串行执行。
5. 第二个 target 失败时不调用逻辑数据源创建，且关闭全部临时 Hikari pool。
6. 所有 migration 成功后，同一份 `Map<String, DataSource>` 传入 ShardingSphere 公开 Factory。
7. 两份规则 YAML 从 `- !SHARDING` 开始的 UTF-8 文本逐字一致。
8. `readwrite` 未与 `sharding` 同时启用时，启动失败。
9. 错误消息包含 target 和 location，不包含 JDBC 密码。

- [ ] **Step 2: 实现 Spring 配置模型**

`ShardingDataSourceProperties` 绑定：

```yaml
app:
  sharding:
    config: classpath:sharding/shardingsphere-sharding.yml
    routing:
      mapping-version: 1
      node-count: 4
      node-map: 0=shard_0:0,1=shard_0:1,2=shard_1:0,3=shard_1:1
    physical-data-sources:
      - name: single
        logical-name: single
        role: PRIMARY
        driver-class-name: org.postgresql.Driver
        jdbc-url: jdbc:postgresql://localhost:5432/app_single
        username: app
        password: app
    flyway:
      targets:
        - data-source-name: single
          locations:
            - classpath:db/migration/sharding/single
```

Java 模型包含：

```java
public enum DataSourceRole {
    PRIMARY,
    REPLICA
}

public record PhysicalDataSourceProperties(
        String name,
        String logicalName,
        DataSourceRole role,
        String driverClassName,
        String jdbcUrl,
        String username,
        String password) {
}

public record ShardingRoutingProperties(
        int mappingVersion,
        int nodeCount,
        String nodeMap) {
}

public record FlywayTargetProperties(
        String dataSourceName,
        List<String> locations) {
}
```

- [ ] **Step 3: 实现物理数据源创建与关闭**

`PhysicalDataSourceFactory#create` 返回按配置顺序构造、按名称索引的 HikariDataSource Map。构造中途失败时立即关闭已创建实例。密码不能进入 `toString`、异常或日志。

- [ ] **Step 4: 实现规则资源加载**

`ShardingYamlLoader` 使用 Spring `ResourceLoader` 和 `Environment#resolveRequiredPlaceholders` 读取 UTF-8 bytes。测试锁定 `$->{0..1}` 原样保留，`${app.sharding.routing.node-map}` 与 `${SHARDING_SQL_SHOW:false}` 被解析。另加规则后缀比较方法，不引入 YAML parser。

- [ ] **Step 5: 实现 Flyway 编排**

```java
public void migrate(
        Map<String, DataSource> physicalDataSources,
        List<FlywayTargetProperties> targets,
        FlywayProperties springFlywayProperties) {
    targets.stream()
            .sorted(comparing(FlywayTargetProperties::dataSourceName))
            .forEach(target -> migrateOne(
                    physicalDataSources.get(target.dataSourceName()),
                    target,
                    springFlywayProperties));
}
```

每个 target 都执行 `migrate()`，再执行 `validate()`；继承 `baselineOnMigrate`、`validateOnMigrate`、`validateMigrationNaming`、`cleanDisabled`、encoding 和 placeholders。

- [ ] **Step 6: 实现 Bootstrapper Facade**

启动顺序固定为：

```java
Map<String, DataSource> physical = physicalDataSourceFactory.create(properties);
try {
    byte[] yaml = shardingYamlLoader.load(properties.config());
    topologyValidator.validate(properties, yaml);
    physicalDataSourceFlywayMigrator.migrate(
            physical,
            properties.flyway().targets(),
            flywayProperties);
    return YamlShardingSphereDataSourceFactory.createDataSource(physical, yaml);
} catch (RuntimeException failure) {
    physicalDataSourceFactory.close(physical.values());
    throw failure;
}
```

实际实现同时处理公开 Factory 抛出的 checked exception，包装为不泄漏凭证的启动异常。逻辑数据源成功创建后，物理连接池所有权转移给 ShardingSphere，不得提前关闭。

- [ ] **Step 7: 实现条件装配**

`ShardingSphereDataSourceConfiguration` 只在 `sharding` profile 激活，注册：

1. `@ConfigurationProperties("app.sharding")`；
2. `UuidV7Generator`；
3. 由 Bootstrapper 返回且标记 `@Primary` 的逻辑 `DataSource`；
4. 沿用 Spring Boot 基于该 DataSource 创建的事务管理器、JdbcTemplate 和 JPA。

不得创建第二套 EntityManagerFactory。

- [ ] **Step 8: 运行生成测试**

Run:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl :egon-cola-archetype-light,:egon-cola-archetype-web,:egon-cola-archetype-service -am integration-test
```

Expected: datasource 和 bootstrapper 单元测试全部通过。

- [ ] **Step 9: 提交**

```bash
git add egon-cola-archetypes/egon-cola-archetype-light egon-cola-archetypes/egon-cola-archetype-web egon-cola-archetypes/egon-cola-archetype-service
git commit -m "feat(archetype): 编排物理节点flyway启动"
```

---

### Task 3: 完成 Light 的 UUIDv7、最终 schema 和三种 profile

**Files:**

- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/db/migration/V1__init_student_management.sql`
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/db/migration/V2__align_large_monolith_domain.sql`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/db/migration/default/V20260723_001__init_light_default_schema.sql`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/db/migration/sharding/single/V20260723_002__init_light_single_schema.sql`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/db/migration/sharding/shard/V20260723_003__init_light_sharding_schema.sql`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/application-sharding.yml`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/application-readwrite.yml`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/sharding/shardingsphere-sharding.yml`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/sharding/shardingsphere-sharding-readwrite.yml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/application-dev.yml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/application-prod.yml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/application-test.yml`
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/common/utils/IdUtils.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/user/service/impl/UserDomainServiceImpl.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/teaching/service/impl/CourseDomainServiceImpl.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/teaching/service/impl/SchoolClassDomainServiceImpl.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/teaching/repo/po/ClassCourseSchedulePO.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/teaching/repo/impl/SchoolClassRepositoryImpl.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/start/StudentManagementApplication.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/application/user/manage/UserManageTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/application/teaching/manage/CourseManageTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/application/teaching/manage/SchoolClassManageTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/infrastructure/user/repo/UserRepositoryImplTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/infrastructure/teaching/repo/SchoolClassRepositoryImplTest.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/infrastructure/config/datasource/LightShardingProfileTest.java`

- [ ] **Step 1: 写 Light 失败测试**

覆盖：

1. 用户、课程、班级、排课代理主键均为 36 位 UUIDv7。
2. 同一 `schoolClassId` 的 `school_classes` 和 `class_course_schedules` 路由到同一库和表后缀。
3. default profile 只有无后缀逻辑表；sharding profile 的 single 库只有单表，两个 shard 库都有 `_0`、`_1`。
4. migration 文件名、三项文件头注释和同日序列号符合规范。
5. 三种 profile 的 Hibernate validate 通过。

- [ ] **Step 2: 注入现有 IdGenerator**

三个 domain service 实现通过构造器注入：

```java
private final IdGenerator idGenerator;
```

业务实体创建使用：

```java
String id = idGenerator.nextId();
```

`ClassCourseSchedulePO.id` 从 `Long + IDENTITY` 改为 `String`，在 repository 保存排课前生成 UUIDv7。traceId、requestId、事件 ID 保持现状。

- [ ] **Step 3: 重写最终初始化 SQL**

每份 SQL 首部使用具体注释。Light default 文件创建：

```text
users
roles
permissions
user_roles
role_permissions
courses
school_classes
class_course_schedules
```

single 文件只创建：

```text
users
roles
permissions
user_roles
role_permissions
courses
```

shard 文件在每个 shard primary 创建：

```text
school_classes_0
school_classes_1
class_course_schedules_0
class_course_schedules_1
```

所有代理主键使用 `VARCHAR(36)`。`class_course_schedules_N.school_class_id` 只引用同后缀 `school_classes_N`；`course_id` 不建立跨库外键。索引和唯一约束沿用当前最终模型语义。

- [ ] **Step 4: 增加 primary-only 和 read/write 配置**

primary-only 使用物理名：

```text
single
shard_0
shard_1
```

read/write 使用：

```text
single_primary
single_replica_0
shard_0_primary
shard_0_replica_0
shard_1_primary
shard_1_replica_0
```

`application-sharding.yml` 只配置一份 `mapping-version=1`、`node-count=4`、`node-map`；两份规则 YAML 配置 `school_classes` 与 `class_course_schedules` 为 binding tables，database/table 算法都通过 `${app.sharding.routing.*}` 引用同一映射。

- [ ] **Step 5: 调整 Flyway 与 Repository 事务配置**

1. default profile 的 `spring.flyway.locations` 改为 `classpath:db/migration/default`。
2. sharding profile 设置 `spring.flyway.enabled=false`。
3. `StudentManagementApplication` 的 `@EnableJpaRepositories` 设置 `enableDefaultTransactions=false`。
4. 审计 Light Application 写用例，缺失的显式 `@Transactional` 必须在关闭默认事务前补齐。
5. 纯查询移除无必要的 `readOnly=true` 事务。

- [ ] **Step 6: 运行 Light archetype 集成测试**

Run:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl :egon-cola-archetype-light -am clean integration-test
```

Expected: 默认、`test,sharding`、`test,sharding,readwrite` 相关测试通过。

- [ ] **Step 7: 提交**

```bash
git add egon-cola-archetypes/egon-cola-archetype-light
git commit -m "feat(archetype): 完成light分片模板"
```

---

### Task 4: 让 Web 班级聚合全链路携带 gradeId

**Files:**

- Modify: `egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/teaching/SchoolClassFacade.java`
- Modify: `egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/teaching/dto/AssignUserToClassDTO.java`
- Modify: `egon-cola-archetypes/egon-cola-organization-facade/src/test/java/top/egon/cola/organization/facade/OrganizationFacadeContractTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/teaching/query/SchoolClassDetailQuery.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/teaching/command/AssignUserToClassCommand.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/teaching/manage/impl/SchoolClassManageImpl.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/teaching/repos/SchoolClassRepository.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/teaching/client/SchoolClassCachePort.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/teaching/cache/InMemorySchoolClassCache.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/teaching/cache/RedisSchoolClassCache.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/teaching/repo/impl/SchoolClassRepositoryImpl.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/teaching/repo/jpa/SchoolClassJpaRepository.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/teaching/repo/jpa/SchoolClassUserJpaRepository.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/teaching/repo/po/SchoolClassUserPO.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/teaching/controller/SchoolClassController.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/teaching/graphql/SchoolClassResolver.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/resources/graphql/schema.graphqls`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/teaching/facade/impl/SchoolClassFacadeImpl.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/teaching/converter/SchoolClassAdapterConverter.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-application/src/test/java/application/teaching/AssignUserToClassUseCaseTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-application/src/test/java/application/teaching/SchoolClassManageImplTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/teaching/repo/SchoolClassRepositoryImplTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/test/java/adapter/teaching/controller/TeachingControllerTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/test/java/adapter/OrganizationGraphQlContractTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/test/java/adapter/OrganizationDubboProviderConfigurationTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/test/java/starter/OrganizationRollbackTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/client/organization/OrganizationDirectoryPort.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/client/organization/DubboOrganizationDirectoryClient.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/client/organization/LocalOrganizationDirectoryStub.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/client/organization/DubboOrganizationDirectoryClientTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/client/organization/LocalOrganizationDirectoryStubTest.java`

- [ ] **Step 1: 先写缺少 gradeId 的失败测试**

共享 Facade 必须先断言：

```java
SchoolClassDetailDTO getSchoolClass(
        @NotBlank String gradeId,
        @NotBlank String schoolClassId);
```

```java
public record AssignUserToClassDTO(
        @NotBlank String gradeId,
        @NotBlank String userId,
        @NotBlank String schoolClassId) {
}
```

锁定以下调用链：

```text
REST/GraphQL/Facade
  -> SchoolClassDetailQuery(gradeId, schoolClassId)
  -> SchoolClassRepository.findByGradeIdAndId(gradeId, schoolClassId)
```

```text
REST/GraphQL/Facade
  -> AssignUserToClassCommand(requestId, gradeId, schoolClassId, userId)
  -> addUser/hasUser(gradeId, schoolClassId, userId)
```

- [ ] **Step 2: 收紧 Domain Repository 和缓存契约**

方法固定为：

```java
Optional<SchoolClass> findByGradeIdAndId(String gradeId, SchoolClassId schoolClassId);
void addUser(String gradeId, SchoolClassId schoolClassId, UserId userId);
boolean hasUser(String gradeId, SchoolClassId schoolClassId, UserId userId);
```

删除仅按 `schoolClassId` 的查找重载和 `findByUserId` 全路由方法。缓存键使用 `gradeId + ":" + schoolClassId`，避免不同路由上下文共用不完整 key。

- [ ] **Step 3: 调整 JPA 查询与关系 PO**

`SchoolClassJpaRepository` 使用 `findByGradeIdAndId`。`SchoolClassUserJpaRepository` 的点查、存在性判断都以 `gradeId` 开头。

`SchoolClassUserPO` 增加：

```java
@Id
private String id;

@Column(name = "grade_id", nullable = false, length = 36)
private String gradeId;
```

删除 `GenerationType.IDENTITY`，唯一约束改为 `(grade_id, school_class_id, user_id)`。

- [ ] **Step 4: 修改 Adapter 契约**

REST 路由固定为：

```text
GET  /api/v1/grades/{gradeId}/school-classes/{schoolClassId}
POST /api/v1/grades/{gradeId}/school-classes/{schoolClassId}/users
```

GraphQL 的班级详情参数和成员分配 input 都增加 `gradeId: ID!`。共享 `SchoolClassFacade` 及本任务中的实现按新签名转成 Query/Command。

Service 侧的组织目录 Port、Dubbo client 和本地 stub 同步改为：

```java
OrganizationSchoolClass getSchoolClass(String gradeId, String schoolClassId);
```

Dubbo client 调用 `schoolClassFacade.getSchoolClass(gradeId, schoolClassId)`；不保留只按 `schoolClassId` 的重载。

- [ ] **Step 5: 运行 Web 分层测试**

Run:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl :egon-cola-organization-facade,:egon-cola-archetype-web,:egon-cola-archetype-service -am integration-test
```

Expected: Web 生成项目编译，班级 API、GraphQL、Facade、Application、Repository 测试通过。

- [ ] **Step 6: 提交**

```bash
git add egon-cola-archetypes/egon-cola-organization-facade egon-cola-archetypes/egon-cola-archetype-web egon-cola-archetypes/egon-cola-archetype-service
git commit -m "refactor(archetype): 补全web班级分片键"
```

---

### Task 5: 完成 Web 的 UUIDv7、最终 schema 和三种 profile

**Files:**

- Delete: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-common/src/main/java/common/utils/IdGenerator.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/user/manage/impl/UserManageImpl.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/teaching/manage/impl/GradeManageImpl.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/teaching/manage/impl/SchoolClassManageImpl.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/user/repo/po/UserRolePO.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/user/repo/po/RolePermissionPO.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/user/repo/impl/UserRepositoryImpl.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/user/repo/impl/RoleRepositoryImpl.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/teaching/repo/impl/SchoolClassRepositoryImpl.java`
- Delete the two existing Web migration files under `__rootArtifactId__-infrastructure/src/main/resources/db/migration`.
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/migration/default/V20260723_001__init_organization_default_schema.sql`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/migration/sharding/single/V20260723_002__init_organization_single_schema.sql`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/migration/sharding/shard/V20260723_003__init_organization_sharding_schema.sql`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/application-sharding.yml`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/application-readwrite.yml`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/sharding/shardingsphere-sharding.yml`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/sharding/shardingsphere-sharding-readwrite.yml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/java/starter/OrganizationApplication.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-application/src/test/java/application/user/UserManageImplTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-application/src/test/java/application/teaching/SchoolClassManageImplTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/user/repo/UserRepositoryImplTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/user/repo/RolePermissionRepositoryImplTest.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource/OrganizationShardingProfileTest.java`

- [ ] **Step 1: 先写 Web UUIDv7 与 schema 失败测试**

所有数据库代理主键必须通过 `top.egon.cola.component.common.id.generator.IdGenerator` 创建并满足：

```java
UUID uuid = UUID.fromString(id);
assertThat(uuid.version()).isEqualTo(7);
assertThat(id).hasSize(36);
```

事件 ID、requestId、traceId 不纳入此断言。

- [ ] **Step 2: 注入 IdGenerator 并移除前缀 ID**

`UserManageImpl`、`GradeManageImpl`、`SchoolClassManageImpl` 使用 raw UUIDv7，不再生成 `user-`、`grade-`、`class-` 前缀。关系 repository 在构造三个关系 PO 时也使用 UUIDv7。

- [ ] **Step 3: 重写 Web 最终初始化 SQL**

default 文件创建：

```text
users
roles
permissions
user_roles
role_permissions
grades
school_classes
school_class_users
```

single 文件创建前六张表；shard 文件创建：

```text
school_classes_0
school_classes_1
school_class_users_0
school_class_users_1
```

`school_classes_N` 的唯一约束包含 `(grade_id, name)`；`school_class_users_N` 必须包含 `grade_id`，唯一约束为 `(grade_id, school_class_id, user_id)`。不建立指向 single 库 users/grades 的跨库外键。

- [ ] **Step 4: 增加 Web 两套规则**

`school_classes(grade_id)` 与 `school_class_users(grade_id)` 为 binding tables。SINGLE 列表包含 users、roles、permissions、user_roles、role_permissions、grades。2N 映射只写在 `application-sharding.yml` 的 `app.sharding.routing`，两类算法都引用该配置。

- [ ] **Step 5: 关闭 Repository 默认事务并审计写用例**

`OrganizationApplication` 使用 `enableDefaultTransactions=false`。先确保 User、Role、Permission、Grade、SchoolClass 的写 Application service 都有显式 `@Transactional`；查询移除 `@Transactional(readOnly = true)`，repository 内完成映射。

- [ ] **Step 6: 运行 Web archetype 集成测试**

Run:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl :egon-cola-archetype-web -am clean integration-test
```

Expected: default、sharding、readwrite 三种测试拓扑均通过。

- [ ] **Step 7: 提交**

```bash
git add egon-cola-archetypes/egon-cola-archetype-web
git commit -m "feat(archetype): 完成web分片模板"
```

---

### Task 6: 让 Service 成绩点查全链路携带 examId

**Files:**

- Modify: `egon-cola-archetypes/egon-cola-evaluation-facade/src/main/java/top/egon/cola/evaluation/facade/exam/dto/GetScoreRequest.java`
- Modify: `egon-cola-archetypes/egon-cola-evaluation-facade/src/test/java/top/egon/cola/evaluation/facade/EvaluationFacadeContractTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/exam/query/GetScoreQuery.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/exam/manage/impl/ScoreManageImpl.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/exam/repos/ScoreRepository.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/exam/repo/impl/ScoreRepositoryImpl.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/exam/repo/jpa/ScoreJpaRepository.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/exam/facade/impl/ScoreFacadeImpl.java`
- Modify Service tests: `ScoreManageTest.java`, `ScoreRepositoryTest.java`, `ScoreFacadeImplTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/client/evaluation/EvaluationQueryPort.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/client/evaluation/DubboEvaluationQueryClient.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/client/evaluation/LocalEvaluationQueryStub.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/client/evaluation/DubboEvaluationQueryClientTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/client/evaluation/LocalEvaluationQueryStubTest.java`

- [ ] **Step 1: 写 examId 路由契约失败测试**

共享 Facade 先断言：

```java
public record GetScoreRequest(
        @NotBlank String examId,
        @NotBlank String scoreId) implements Serializable {
}
```

固定签名：

```java
public record GetScoreQuery(String examId, String scoreId) {
}
```

```java
Optional<Score> findByExamIdAndId(ExamId examId, String scoreId);
```

JPA 使用：

```java
Optional<ScorePo> findByExamIdAndId(String examId, String id);
```

- [ ] **Step 2: 修改 Service 全链路**

`ScoreFacadeImpl` 将 `GetScoreRequest(examId, scoreId)` 转为 Query。`ScoreManageImpl#get` 同时校验两个字段，repository 的新增和查询都不得调用只按 id 的 JPA 方法。

- [ ] **Step 3: 修改 Web 消费端**

```java
EvaluationScore getScore(String examId, String scoreId);
```

Dubbo client 构造新的 `GetScoreRequest(examId, scoreId)`；删除旧重载，更新所有编译调用点。

- [ ] **Step 4: 运行 Service 与 Web 生成测试**

Run:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl :egon-cola-evaluation-facade,:egon-cola-archetype-service,:egon-cola-archetype-web -am integration-test
```

Expected: 成绩点查契约和所有消费端编译、测试通过。

- [ ] **Step 5: 提交**

```bash
git add egon-cola-archetypes/egon-cola-evaluation-facade egon-cola-archetypes/egon-cola-archetype-service egon-cola-archetypes/egon-cola-archetype-web
git commit -m "refactor(archetype): 补全成绩分片键"
```

---

### Task 7: 完成 Service 的 UUIDv7、最终 schema 和三种 profile

**Files:**

- Delete: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-common/src/main/java/common/utils/EvaluationIdUtils.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/course/vos/CourseId.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/exam/vos/ExamId.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/course/manage/impl/CourseManageImpl.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/exam/manage/impl/ExamManageImpl.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/exam/manage/impl/ScoreManageImpl.java`
- Delete the two existing Service migration files under `__rootArtifactId__-infrastructure/src/main/resources/db/migration`.
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/migration/default/V20260723_001__init_evaluation_default_schema.sql`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/migration/sharding/single/V20260723_002__init_evaluation_single_schema.sql`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/migration/sharding/shard/V20260723_003__init_evaluation_sharding_schema.sql`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/application-sharding.yml`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/application-readwrite.yml`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/sharding/shardingsphere-sharding.yml`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/sharding/shardingsphere-sharding-readwrite.yml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/java/starter/EvaluationServiceApplication.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/test/java/application/course/CourseManageTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/test/java/application/exam/ExamManageTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/test/java/application/exam/ScoreManageTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/course/repo/CourseRepositoryTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/course/repo/CourseScheduleRepositoryTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/exam/repo/ExamRepositoryTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/exam/repo/ExamPaperRepositoryTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/exam/repo/ScoreRepositoryTest.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource/EvaluationShardingProfileTest.java`

- [ ] **Step 1: 写 Service UUIDv7 与共置失败测试**

验证 course、course_schedule、exam、exam_paper、score 的代理主键都是 36 位 UUIDv7；同一 examId 的三张考试表共置，同一 courseId 的排课稳定路由。

- [ ] **Step 2: 注入 IdGenerator**

`CourseId.newId()`、`ExamId.newId()` 和 `EvaluationIdUtils` 全部删除。Application service 构造实体前注入 `IdGenerator`：

```java
private final IdGenerator idGenerator;
```

```java
String id = idGenerator.nextId();
```

- [ ] **Step 3: 重写 Service 最终初始化 SQL**

default 文件创建：

```text
course
course_schedule
exam
exam_paper
score
```

single 文件只创建 course。shard 文件创建：

```text
course_schedule_0
course_schedule_1
exam_0
exam_1
exam_paper_0
exam_paper_1
score_0
score_1
```

`exam_paper_N.exam_id` 和 `score_N.exam_id` 可建立到同后缀 `exam_N.id` 的本地外键；`course_id` 不建立跨 single 库外键。`score_N` 唯一约束为 `(exam_id, student_id)`。

- [ ] **Step 4: 增加 Service 两套规则**

1. `course_schedule` 按 `course_id` 分片。
2. `exam` 按 `id`，`exam_paper` 和 `score` 按 `exam_id`。
3. `exam,exam_paper,score` 为 binding tables。
4. course 为 SINGLE。
5. 2N 映射只写在 `application-sharding.yml` 的 `app.sharding.routing`，database/table 算法都引用该配置。

- [ ] **Step 5: 关闭默认 Repository 事务并补齐写事务**

`EvaluationServiceApplication` 设置 `enableDefaultTransactions=false`。Course、Exam、Score 的命令用例保留显式本地事务；get/page 查询移除 read-only 事务。

- [ ] **Step 6: 运行 Service archetype 集成测试**

Run:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl :egon-cola-archetype-service -am clean integration-test
```

Expected: default、sharding、readwrite 三种测试拓扑均通过。

- [ ] **Step 7: 提交**

```bash
git add egon-cola-archetypes/egon-cola-archetype-service
git commit -m "feat(archetype): 完成service分片模板"
```

---

### Task 8: 强制 migration 命名、注释与逻辑 schema 一致性

**Files:**

- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/infrastructure/migration/FlywayMigrationConventionTest.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/infrastructure/migration/LogicalSchemaParityTest.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/migration/FlywayMigrationConventionTest.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/migration/LogicalSchemaParityTest.java`
- Replace: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/OrganizationFlywayMigrationTest.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/migration/FlywayMigrationConventionTest.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/migration/LogicalSchemaParityTest.java`
- Replace: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/migration/EvaluationMigrationTest.java`

- [ ] **Step 1: 实现 migration 规范扫描测试**

固定正则：

```java
private static final Pattern VERSIONED_MIGRATION = Pattern.compile(
        "^V\\d{8}_\\d{3}__[a-z0-9]+(?:_[a-z0-9]+)*\\.sql$");
```

扫描 `db/migration` 全部子目录，验证：

1. 文件名匹配；
2. 同一 archetype 同一天的 `NNN` 不重复；
3. 首条 SQL 前包含非空“变更内容、影响范围、兼容性说明”；
4. 不含 `TODO`、`TBD`、“待补充”；
5. 不存在 `V1__`、`V2__` 文件。

- [ ] **Step 2: 实现 schema parity 测试**

分别迁移：

1. default H2；
2. single H2；
3. shard_0 H2；
4. shard_1 H2。

将 `_0`、`_1` 后缀归一化后，合并 single + shard 的逻辑表/列，与 default schema 比较。允许差异仅为：

```text
物理表后缀
物理数据库名
只在同一物理节点可成立的本地外键
```

JPA `ddl-auto=validate` 必须分别对 default 和 ShardingSphere 逻辑 DataSource 执行。

- [ ] **Step 3: 删除旧数据搬迁语义测试**

原先断言 legacy 表、旧数据回填或固定 version `1` 的测试，改为断言最终干净 schema 和成功 migration 状态，不保留兼容分支。

- [ ] **Step 4: 运行三个 archetype 集成测试**

Run:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl :egon-cola-archetype-light,:egon-cola-archetype-web,:egon-cola-archetype-service -am clean integration-test
```

Expected: 命名、注释、schema parity 和 JPA validate 全部通过。

- [ ] **Step 5: 提交**

```bash
git add egon-cola-archetypes/egon-cola-archetype-light egon-cola-archetypes/egon-cola-archetype-web egon-cola-archetypes/egon-cola-archetype-service
git commit -m "test(archetype): 强制flyway与schema契约"
```

---

### Task 9: 验证读写分离和本地事务边界

**Files:**

- Create or modify focused routing integration tests under each archetype's generated `src/test/java/infrastructure/config/datasource`.
- Modify Application service tests for every current write use case in Light、Web、Service.
- Modify query tests that previously expected `@Transactional(readOnly = true)`.
- Modify the three generated starter/application context tests to cover `test,sharding,readwrite`.

- [ ] **Step 1: 写路由探针测试**

使用可记录 JDBC URL 或 datasource name 的测试 DataSource，断言：

```text
非事务 SELECT -> replica
写 SQL -> primary
事务内 SELECT -> primary
Flyway -> primary only
```

H2 测试允许 primary/replica 指向相同底层数据库，但必须使用不同 datasource name 和探针，避免“数据能读到”掩盖路由错误。

- [ ] **Step 2: 写单写节点事务测试**

逐个覆盖当前写用例：

1. Light：用户/角色单表写、班级/排课同一 schoolClassId 写。
2. Web：用户授权单表写、同一 gradeId 的班级/成员写。
3. Service：同一 courseId 的排课写、同一 examId 的考试/试卷/成绩写。

每个测试记录实际写 datasource 集合，并断言 size 为 `1`。不得实现通用 SQL 拦截器来承诺未来任意事务安全。

- [ ] **Step 3: 验证禁止场景没有伪事务**

代码扫描和测试共同确认：

1. 不存在 ShardingSphere transaction、Seata、Atomikos、Narayana 依赖；
2. 没有一个 Application 事务同时写 SINGLE 与 SHARDING；
3. 跨分片流程只通过已有事件、幂等、重试或补偿边界表达。

- [ ] **Step 4: 运行三个 archetype 集成测试**

Run:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl :egon-cola-archetype-light,:egon-cola-archetype-web,:egon-cola-archetype-service -am clean integration-test
```

Expected: 三类路由和单写节点断言通过。

- [ ] **Step 5: 提交**

```bash
git add egon-cola-archetypes/egon-cola-archetype-light egon-cola-archetypes/egon-cola-archetype-web egon-cola-archetypes/egon-cola-archetype-service
git commit -m "test(archetype): 验证读写与本地事务路由"
```

---

### Task 10: 更新生成契约和中英文使用文档

**Files:**

- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/README.md`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/README.zh-CN.md`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/README.md`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/README.zh-CN.md`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/README.md`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/README.zh-CN.md`

- [ ] **Step 1: 扩展三个 verify.groovy**

每个 verifier 必须断言生成项目包含：

1. `shardingsphere-jdbc` 版本 `5.5.3`；
2. `egon-cola-component-common-id`；
3. `application-sharding.yml`、`application-readwrite.yml`；
4. 两份 ShardingSphere 规则 YAML；
5. 三个 migration locations 和三份规范文件；
6. 原 `V1/V2` 不存在；
7. `node-count: 4`、`mapping-version: 1` 和完整初始 `node-map`；
8. 2N 算法、兼容性校验、Flyway 编排和测试类；
9. Web 的 `gradeId`、Service 的 `examId` 契约；
10. README 中的 profile、分片键、事务和 SQL 规范。

- [ ] **Step 2: 更新 README.md 与 README.zh-CN.md**

两种语言都必须说明：

1. 默认、`sharding`、`sharding,readwrite` 三种启动方式；
2. 每个 archetype 的 SINGLE/SHARDING 表清单和分片键；
3. 物理数据源环境变量；
4. Flyway 只迁移 primary；
5. UUIDv7 为 36 位 RFC 字符串；
6. SQL 命名和三项文件头注释；
7. 2N 数量约束、追加式 nodeMap、`N → 2N` 切换步骤；
8. 只允许单库本地事务，跨分片由业务幂等、状态、事件和补偿解决；
9. 2N “平滑”不代表脚手架内置在线双写、CDC 或自动搬数。

- [ ] **Step 3: 运行生成契约**

Run:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml clean integration-test
```

Expected: 三个 `verify.groovy` 与生成项目测试全部通过。

- [ ] **Step 4: 提交**

```bash
git add egon-cola-archetypes
git commit -m "docs(archetype): 补充分片与flyway指南"
```

---

### Task 11: 执行全量验证并收口

**Files:**

- Modify only if validation exposes a defect: files owned by Tasks 1–11.
- Do not create or modify files under any `target/` directory.

- [ ] **Step 1: 检查依赖树**

Run:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl :egon-cola-archetype-light,:egon-cola-archetype-web,:egon-cola-archetype-service -am dependency:tree -Dincludes=org.apache.shardingsphere,org.flywaydb,top.egon:egon-cola-component-common-id
```

Expected:

```text
ShardingSphere JDBC only resolves to 5.5.3
No distributed transaction implementation is present
common-id is present only in the intended generated modules
```

- [ ] **Step 2: 执行 archetype 全量集成测试**

Run:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml clean integration-test
```

Expected: `BUILD SUCCESS`，三个 archetype 都完成生成、构建、测试和 verifier 校验。

- [ ] **Step 3: 在三个生成项目中验证 profile**

从 Maven Invoker 实际生成目录中定位每个项目，分别执行：

```bash
./mvnw -B -ntp clean verify
./mvnw -B -ntp -Dspring.profiles.active=test,sharding clean verify
./mvnw -B -ntp -Dspring.profiles.active=test,sharding,readwrite clean verify
```

Expected: 三个命令在三个生成项目中均为 `BUILD SUCCESS`。只运行生成目录中的命令，不修改生成文件。

- [ ] **Step 4: 做静态收口检查**

Run:

```bash
rg -n 'V1__|V2__|GenerationType\\.IDENTITY|EvaluationIdUtils|common\\.utils\\.IdGenerator|common\\.utils\\.IdUtils' egon-cola-archetypes/egon-cola-archetype-{light,web,service}/src/main/resources/archetype-resources
```

Expected: 没有旧 migration、数据库自增业务主键和旧随机 ID 工具残留。

Run:

```bash
rg -n 'seata|atomikos|narayana|xa-transaction|shardingsphere-transaction' egon-cola-archetypes/egon-cola-archetype-{light,web,service}/src/main/resources/archetype-resources
```

Expected: 无分布式事务依赖或配置。

Run:

```bash
git diff --check
git status --short
```

Expected: `git diff --check` 无输出；`git status --short` 只包含本计划范围内尚未提交的修正，且没有 `target/` 文件。

- [ ] **Step 5: 对验证修正提交一次**

仅当 Steps 1–4 暴露问题并完成修正时执行：

```bash
git add egon-cola-archetypes
git commit -m "fix(archetype): 收口分片模板验证"
```

若没有修正，不创建空提交。

## Completion Checklist

- [ ] Light、Web、Service 三个 archetype 都生成三种数据源模式。
- [ ] 分片表、SINGLE 表和 binding tables 与 spec 一致。
- [ ] Web 所有班级访问携带 `gradeId`，Service 成绩点查携带 `examId`。
- [ ] 业务代理主键全部为应用侧 UUIDv7，非业务追踪 ID 不被误改。
- [ ] 初始 `2 × 2 = 4` 节点和 2N 追加式映射由测试锁定。
- [ ] `assertCanExpandTo` 拒绝非翻倍、版本倒退和旧槽位改写。
- [ ] Flyway 逐个迁移 primary，replica 从不执行 migration。
- [ ] SQL 文件名、序列号和三项注释由自动化测试强制执行。
- [ ] 原六个 `V1/V2` 模板迁移已被最终初始化 schema 替换。
- [ ] 非事务查询走 replica，写事务和事务内读取走 primary。
- [ ] 每个现有写事务只触达一个物理主库。
- [ ] 不存在分布式事务依赖、配置或伪装成单事务的跨分片写。
- [ ] 三个 `verify.groovy`、README.md 和 README.zh-CN.md 同步完成。
- [ ] `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml clean integration-test` 通过。
- [ ] 三个生成项目的 default、sharding、sharding+readwrite 验证全部通过。
