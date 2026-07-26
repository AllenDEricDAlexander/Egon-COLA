# Archetype ShardingSphere None 策略实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 删除三个 archetype 的应用级 SINGLE mode 和 `!SINGLE` 规则，使应用始终通过 ShardingSphereDataSource 运行，并用单节点 `actualDataNodes + NoneShardingStrategy` 管理主数据表。

**Architecture:** `app.datasource.mode` 只允许 `SHARDING`、`SHARDING_READWRITE`，默认 SHARDING。主数据表和真正的分片表统一声明在 `!SHARDING.tables` 中：主数据表固定到 `master_data` 且分库/分表策略都是 `none`，分片表继续使用 UUIDv7 标准路由、binding tables 和 DML 审计。FlywayAutoConfiguration 被排除，现有 Bootstrapper 在创建逻辑 DataSource 前初始化所有物理 primary。

**Tech Stack:** Java 21、Spring Boot、Apache ShardingSphere JDBC 5.5.3、Flyway、HikariCP、PostgreSQL、H2、JUnit 5、AssertJ、Maven Archetype、Groovy `verify.groovy`、Docker Compose。

## Global Constraints

- 适用模块只有 `egon-cola-archetype-light`、`egon-cola-archetype-service`、`egon-cola-archetype-web`。
- Spring Profile 只允许 dev、test、prod；不得增加 sharding、readwrite 或 local Profile。
- 合法 mode 只有 `SHARDING`、`SHARDING_READWRITE`；默认值必须是 SHARDING。
- 应用 Bean 图中始终只有 ShardingSphereDataSource，不得恢复普通单数据源兜底。
- 主数据表必须显式配置一个 `master_data.public.<table>` actual data node，并同时配置 `databaseStrategy.none`、`tableStrategy.none`。
- active ShardingSphere YAML 中禁止 `!SINGLE` 和 `defaultDataSource`。
- 分片表继续使用应用层 UUIDv7，不启用 Snowflake 或数据库自增分片键。
- 分片表必须配置 `DML_SHARDING_CONDITIONS`，`allowHintDisable=false`。
- 不增加 XA、Seata 或其他分布式事务依赖；同分片键关联表只使用单物理库本地事务。
- 不实现历史业务数据、在线数据搬迁、双写、切流、回滚或真实主从环境。
- 只保留初始 2 的幂拓扑校验；删除 `mapping-version` 和旧/新 node map 兼容比较。
- Flyway 只初始化物理 primary；replica 和逻辑 DataSource 不执行 Flyway。
- SQL 文件名使用 `VyyyyMMdd_NNN__lower_snake_case_description.sql`，并包含“变更内容、影响范围、兼容性说明”三行头注释。
- 修改 archetype 源模板和 `verify.groovy`，不得修改或提交 `target/` 生成产物。
- 保持现有 Facade：`ShardingDataSourceBootstrapper#createDataSource(ShardingDataSourceProperties, FlywayProperties)`；不引入新的业务设计模式层级。
- 每个任务先写失败测试，再做最小实现，再运行对应 archetype integration-test，最后独立提交。
- 实施开始时使用 `superpowers:using-git-worktrees` 创建隔离 worktree；不要在当前 main 上直接改模板代码。

## Target Interfaces

三个 archetype 的 datasource 包最终必须具有相同接口：

```java
@ConfigurationProperties("app.datasource")
public record DataSourceModeProperties(DataSourceMode mode) {

    public DataSourceModeProperties {
        mode = mode == null ? DataSourceMode.SHARDING : mode;
    }

    public enum DataSourceMode {
        SHARDING("sharding"),
        SHARDING_READWRITE("sharding-readwrite");

        private final String topologyName;

        DataSourceMode(String topologyName) {
            this.topologyName = topologyName;
        }

        public String topologyName() {
            return topologyName;
        }
    }
}
```

```java
public record ShardingNodeMap(
        int nodeCount,
        Map<Integer, PhysicalNode> nodes) {

    public static ShardingNodeMap parse(Properties properties);

    static ShardingNodeMap parse(String nodeCount, String nodeMap);

    public PhysicalNode route(String shardingKey);

    public int routeSlot(String shardingKey);
}
```

`ShardingDataSourceProperties.ShardingRoutingProperties` 最终只保留：

```java
public record ShardingRoutingProperties(
        int nodeCount,
        String nodeMap) {
}
```

`ShardingTopologyValidator` 保持公开入口：

```java
public void validate(ShardingDataSourceProperties properties, byte[] yaml)
```

内部表规则模型固定为：

```java
private record TableRule(
        String actualDataNodes,
        Strategy databaseStrategy,
        Strategy tableStrategy,
        boolean shardingAuditRequired,
        boolean hintDisableAllowed) {
}

private enum Strategy {
    NONE,
    STANDARD
}
```

## File Responsibility Map

- `DataSourceModeProperties.java`：合法 mode、默认 mode 和 topology name。
- `ShardingDataSourcePropertiesLoader.java`：只绑定被选中的 topology，未选中的密钥占位符不能解析。
- `ShardingNodeMap.java`：新项目静态 node-count/node-map 校验和 UUIDv7 槽位路由。
- `UuidV7BucketShardingAlgorithm.java`：将同一个 node map 投影为 database 或 table target。
- `ShardingTopologyValidator.java`：物理角色、Flyway target、None 表、分片表、read/write group、audit 和 actual node 一致性。
- `PhysicalDataSourceFlywayMigrator.java`：按名称顺序初始化物理 primary。
- `ShardingDataSourceBootstrapper.java`：连接池、规则、Flyway、逻辑 DataSource 的启动 Facade。
- `ShardingSphereDataSourceConfiguration.java`：始终装配上述组件并发布唯一 Primary DataSource。
- `datasource/*.yml`：物理连接池、逻辑组和 Flyway target。
- `sharding/*.yml`：None 表、分片表、binding、audit 和 read/write 规则。
- `db/migration/sharding/master-data`：主数据物理库结构。
- `db/migration/sharding/shard`：每个分片库的物理分表结构。
- `verify.groovy`：生成项目的不可回退契约。

## Design Pattern Decision

- 保留现有 `ShardingDataSourceBootstrapper` Facade，集中编排“物理连接池 → Flyway → ShardingSphereDataSource”；该入口已经隔离启动复杂度，不新增第二层 Facade。
- 表路由差异直接使用 ShardingSphere 已提供的 `NoneShardingStrategy` 与 Standard Strategy 配置；不再创建应用自定义 Strategy/Factory，因为变化点已经由框架规则模型承载，额外接口只会增加三套模板的维护面。
- 保留 `PhysicalDataSourceFlywayMigrator` 作为单一迁移协作者，不引入 Template Method 或 Chain of Responsibility；当前步骤固定且失败即终止，直接顺序编排更清晰。

---

### Task 1: Light archetype 完整切换到 ShardingSphere-only

**Files:**

- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/config/datasource/DataSourceModeProperties.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/config/datasource/ShardingDataSourceProperties.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/config/datasource/ShardingDataSourcePropertiesLoader.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/config/datasource/ShardingNodeMap.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/config/datasource/UuidV7BucketShardingAlgorithm.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/config/datasource/ShardingTopologyValidator.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/config/datasource/ShardingSphereDataSourceConfiguration.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/start/StudentManagementApplication.java`
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/config/datasource/ShardingDataSourceModeCondition.java`
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/config/datasource/LogicalDataSourceFlywayMigrationStrategy.java`
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/config/datasource/ShardingNodeMapCompatibilityValidator.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/application.yml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/application-test.yml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/datasource/sharding.yml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/datasource/sharding-readwrite.yml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/sharding/shardingsphere-sharding.yml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/sharding/shardingsphere-sharding-readwrite.yml`
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/db/migration/default/V20260724_001__init_light_default_schema.sql`
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/db/migration/sharding/single/V20260724_002__init_light_single_schema.sql`
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/db/migration/sharding/shard/V20260724_003__init_light_sharding_schema.sql`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/db/migration/sharding/master-data/V20260726_001__init_light_master_data_schema.sql`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/db/migration/sharding/shard/V20260726_002__init_light_sharded_schema.sql`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/infrastructure/config/datasource/DataSourceModePropertiesTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/infrastructure/config/datasource/ShardingDataSourcePropertiesTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/infrastructure/config/datasource/ShardingDataSourcePropertiesLoaderTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/infrastructure/config/datasource/ShardingNodeMapTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/infrastructure/config/datasource/UuidV7BucketShardingAlgorithmTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/infrastructure/config/datasource/ShardingTopologyValidatorTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/infrastructure/config/datasource/ShardingYamlLoaderTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/infrastructure/config/datasource/PhysicalDataSourceFactoryTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/infrastructure/config/datasource/PhysicalDataSourceFlywayMigratorTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/infrastructure/config/datasource/ShardingDataSourceBootstrapperTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/infrastructure/config/datasource/ReadwriteRoutingIntegrationTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/infrastructure/config/datasource/LightDataSourceModeTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/infrastructure/migration/FlywayMigrationConventionTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/infrastructure/migration/LogicalSchemaParityTest.java`
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/infrastructure/config/datasource/LogicalDataSourceFlywayMigrationStrategyTest.java`
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/infrastructure/config/datasource/ShardingNodeMapCompatibilityValidatorTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/README.md`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/README.zh-CN.md`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/deploy/container/README.md`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/deploy/container/README.zh-CN.md`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/deploy/container/Dockerfile`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/deploy/env/.env.example`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/deploy/env/.env.prod.example`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/deploy/compose/compose.docker.yaml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/deploy/compose/compose.docker.prod.yaml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/deploy/compose/compose.podman.yaml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/deploy/compose/compose.podman.prod.yaml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/deploy/compose/compose.nerdctl.yaml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/deploy/compose/compose.nerdctl.prod.yaml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`

**Interfaces:**

- Consumes: `DataSourceModeProperties`, `ShardingNodeMap` and `ShardingTopologyValidator` signatures from Target Interfaces.
- Produces: a generated Light application whose default/test DataSource is ShardingSphereDataSource; `users/roles/permissions/user_roles/role_permissions/courses` route to `master_data`; `school_classes/class_course_schedules` route by UUIDv7.

- [ ] **Step 1: Write failing mode, node-map, topology and startup tests**

Update `DataSourceModePropertiesTest` to assert exactly two values and reject SINGLE:

```java
@Test
void defaultsToShardingAndRejectsSingle() {
    assertThat(new DataSourceModeProperties(null).mode())
            .isEqualTo(DataSourceModeProperties.DataSourceMode.SHARDING);
    assertThat(DataSourceModeProperties.DataSourceMode.values())
            .containsExactly(
                    DataSourceModeProperties.DataSourceMode.SHARDING,
                    DataSourceModeProperties.DataSourceMode.SHARDING_READWRITE);
    assertThatThrownBy(() -> bind("SINGLE"))
            .isInstanceOf(BindException.class)
            .hasMessageContaining("app.datasource.mode");
}
```

Update `ShardingNodeMapTest` to call `parse("4", NODE_MAP)` and assert that `mappingVersion()` no longer exists by compiling only against the new two-field record contract.

Add `ShardingTopologyValidatorTest` cases with this exact master-data table rule:

```yaml
users:
  actualDataNodes: master_data.public.users
  databaseStrategy:
    none:
  tableStrategy:
    none:
```

The tests must reject `!SINGLE`, reject a master-data table without either `none`, reject a shard table without `DML_SHARDING_CONDITIONS`, and reject `allowHintDisable: true`.

Update `LightDataSourceModeTest`:

```java
assertThat(context.getBean(DataSource.class).getClass().getName())
        .contains("ShardingSphereDataSource");
assertThat(context.getBeansOfType(Flyway.class)).isEmpty();
assertThat(context.getEnvironment().getProperty("app.datasource.mode"))
        .isEqualTo("SHARDING");
```

Remove the former Hikari/SINGLE startup test.

- [ ] **Step 2: Run the Light archetype contract and observe RED**

Run:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml \
  -pl egon-cola-archetype-light -am clean integration-test
```

Expected: FAIL because `DataSourceMode.SINGLE`, `mapping-version`, `!SINGLE`, default migration and Boot Flyway are still present.

- [ ] **Step 3: Implement the ShardingSphere-only runtime contract**

Apply the Target Interfaces exactly. In `StudentManagementApplication` use:

```java
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;

@SpringBootApplication(
        scanBasePackages = "${package}",
        exclude = FlywayAutoConfiguration.class)
```

Remove `@Conditional(ShardingDataSourceModeCondition.class)` and the `FlywayMigrationStrategy` Bean from `ShardingSphereDataSourceConfiguration`. Keep:

```java
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({
    DataSourceModeProperties.class,
    FlywayProperties.class
})
public class ShardingSphereDataSourceConfiguration {
```

Change `application.yml` to:

```yaml
app:
  datasource:
    mode: ${symbol_dollar}{APP_DATASOURCE_MODE:SHARDING}
```

Remove ordinary `spring.datasource` and default Flyway locations from `application-test.yml`; define a complete H2 SHARDING topology for `master_data`, `shard_0`, `shard_1` and the three physical Flyway targets.

- [ ] **Step 4: Replace Light SINGLE rules with explicit NoneShardingStrategy rules**

Both Light ShardingSphere YAML files must declare these master-data rules inside `!SHARDING.tables`:

```yaml
users:
  actualDataNodes: master_data.public.users
  databaseStrategy:
    none:
  tableStrategy:
    none:
roles:
  actualDataNodes: master_data.public.roles
  databaseStrategy:
    none:
  tableStrategy:
    none:
permissions:
  actualDataNodes: master_data.public.permissions
  databaseStrategy:
    none:
  tableStrategy:
    none:
user_roles:
  actualDataNodes: master_data.public.user_roles
  databaseStrategy:
    none:
  tableStrategy:
    none:
role_permissions:
  actualDataNodes: master_data.public.role_permissions
  databaseStrategy:
    none:
  tableStrategy:
    none:
courses:
  actualDataNodes: master_data.public.courses
  databaseStrategy:
    none:
  tableStrategy:
    none:
```

Keep `school_classes` and `class_course_schedules` standard strategies, add to each:

```yaml
auditStrategy:
  auditorNames:
    - sharding_key_required_auditor
  allowHintDisable: false
```

Add rule-level:

```yaml
auditors:
  sharding_key_required_auditor:
    type: DML_SHARDING_CONDITIONS
```

Delete the complete `!SINGLE` section. Rename logical group `single` to `master_data` in both datasource topology files and in the read/write groups.

Rename `LIGHT_SHARDING_SINGLE_URL` to `LIGHT_SHARDING_MASTER_DATA_URL`; rename the read/write trio `LIGHT_SINGLE_PRIMARY_*` / `LIGHT_SINGLE_REPLICA_0_*` to `LIGHT_MASTER_DATA_PRIMARY_*` / `LIGHT_MASTER_DATA_REPLICA_0_*`. Keep shard variable names unchanged.

- [ ] **Step 5: Rewrite Light Flyway initialization resources**

Create `V20260726_001__init_light_master_data_schema.sql` containing only `users`, `roles`, `permissions`, `user_roles`, `role_permissions`, `courses` and their local indexes/constraints/seeds.

Create `V20260726_002__init_light_sharded_schema.sql` containing only `school_classes_0`, `school_classes_1`, `class_course_schedules_0`, `class_course_schedules_1` and their local indexes/constraints.

Both files must start with concrete Chinese header comments. Configure Flyway targets exactly as:

```yaml
flyway:
  targets:
    - data-source-name: master_data
      locations:
        - classpath:db/migration/sharding/master-data
    - data-source-name: shard_0
      locations:
        - classpath:db/migration/sharding/shard
    - data-source-name: shard_1
      locations:
        - classpath:db/migration/sharding/shard
```

The read/write file uses `master_data_primary`, `shard_0_primary`, `shard_1_primary` with the same respective locations.

- [ ] **Step 6: Complete Light routing, audit, deployment and verifier coverage**

Extend `ReadwriteRoutingIntegrationTest` with a `users` table present only on `master_data`; verify CRUD never touches `shard_0` or `shard_1`. Add a dangerous `UPDATE school_classes SET ...` without `id` and assert ShardingSphere rejects it through DML audit.

Change Dockerfile default to `APP_DATASOURCE_MODE=SHARDING`. In all six Compose files replace one `postgres` service with `postgres-master-data`, `postgres-shard-0`, `postgres-shard-1`, three volumes and application environment variables for the three URLs. Do not add replica services.

The Light application environment must be exactly:

```yaml
APP_DATASOURCE_MODE: "SHARDING"
LIGHT_SHARDING_MASTER_DATA_URL: jdbc:postgresql://postgres-master-data:5432/${POSTGRES_DB}
LIGHT_SHARDING_SHARD_0_URL: jdbc:postgresql://postgres-shard-0:5432/${POSTGRES_DB}
LIGHT_SHARDING_SHARD_1_URL: jdbc:postgresql://postgres-shard-1:5432/${POSTGRES_DB}
LIGHT_SHARDING_USERNAME: ${POSTGRES_USER}
LIGHT_SHARDING_PASSWORD: ${POSTGRES_PASSWORD}
```

In both env examples replace `POSTGRES_PORT` with collision-free host ports `POSTGRES_MASTER_DATA_PORT=5432`, `POSTGRES_SHARD_0_PORT=5433`, `POSTGRES_SHARD_1_PORT=5434`. Compose maps each to container port 5432 and uses volumes `postgres_master_data_data`, `postgres_shard_0_data`, `postgres_shard_1_data`.

Update four README files to describe NONE versus SHARDING tables, the default three-primary topology and external read/write variables.

Update `verify.groovy` to assert:

```groovy
assert lightApplication.contains('mode: ${APP_DATASOURCE_MODE:SHARDING}')
assertMissing('src/main/resources/db/migration/default')
assertMissing('src/main/resources/db/migration/sharding/single')

[
    'src/main/resources/sharding/shardingsphere-sharding.yml',
    'src/main/resources/sharding/shardingsphere-sharding-readwrite.yml'
].each { path ->
    assert !assertFile(path).text.contains('!SINGLE')
}
[
    'deploy/compose/compose.docker.yaml',
    'deploy/compose/compose.docker.prod.yaml',
    'deploy/compose/compose.podman.yaml',
    'deploy/compose/compose.podman.prod.yaml',
    'deploy/compose/compose.nerdctl.yaml',
    'deploy/compose/compose.nerdctl.prod.yaml'
].each { path ->
    assert !assertFile(path).text.contains('APP_DATASOURCE_MODE: "SINGLE"')
}
```

Also assert six `databaseStrategy:\n          none:` and six `tableStrategy:\n          none:` occurrences, both migration filenames, three PostgreSQL services, and absence of `LogicalDataSourceFlywayMigrationStrategy`.

- [ ] **Step 7: Run Light verification and commit**

Run:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml \
  -pl egon-cola-archetype-light -am clean integration-test
git diff --check
```

Expected: Light archetype and generated Light project BUILD SUCCESS; all Light tests pass; `git diff --check` has no output.

Commit:

```bash
git add egon-cola-archetypes/egon-cola-archetype-light
git commit -m "fix(archetype-light): require ShardingSphere datasource"
```

---

### Task 2: Service archetype 完整切换到 ShardingSphere-only

**Files:**

- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/config/datasource/DataSourceModeProperties.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/config/datasource/ShardingDataSourceProperties.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/config/datasource/ShardingDataSourcePropertiesLoader.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/config/datasource/ShardingNodeMap.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/config/datasource/UuidV7BucketShardingAlgorithm.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/config/datasource/ShardingTopologyValidator.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/config/datasource/ShardingSphereDataSourceConfiguration.java`
- Delete: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/config/datasource/ShardingDataSourceModeCondition.java`
- Delete: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/config/datasource/LogicalDataSourceFlywayMigrationStrategy.java`
- Delete: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/config/datasource/ShardingNodeMapCompatibilityValidator.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/java/starter/EvaluationServiceApplication.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/application.yml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/application-test.yml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/datasource/sharding.yml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/datasource/sharding-readwrite.yml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/sharding/shardingsphere-sharding.yml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/sharding/shardingsphere-sharding-readwrite.yml`
- Delete: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/migration/default/V20260724_001__init_evaluation_default_schema.sql`
- Delete: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/migration/sharding/single/V20260724_002__init_evaluation_single_schema.sql`
- Delete: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/migration/sharding/shard/V20260724_003__init_evaluation_sharding_schema.sql`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/migration/sharding/master-data/V20260726_001__init_evaluation_master_data_schema.sql`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/migration/sharding/shard/V20260726_002__init_evaluation_sharded_schema.sql`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource/DataSourceModePropertiesTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource/ShardingDataSourcePropertiesTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource/ShardingDataSourcePropertiesLoaderTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource/ShardingNodeMapTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource/UuidV7BucketShardingAlgorithmTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource/ShardingTopologyValidatorTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource/ShardingYamlLoaderTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource/PhysicalDataSourceFactoryTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource/PhysicalDataSourceFlywayMigratorTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource/ShardingDataSourceBootstrapperTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource/ReadwriteRoutingIntegrationTest.java`
- Delete: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource/LogicalDataSourceFlywayMigrationStrategyTest.java`
- Delete: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource/ShardingNodeMapCompatibilityValidatorTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/migration/FlywayMigrationConventionTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/migration/LogicalSchemaParityTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/migration/EvaluationMigrationTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/test/java/starter/EvaluationDataSourceModeTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/README.md`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/README.zh-CN.md`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/deploy/container/README.md`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/deploy/container/README.zh-CN.md`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/deploy/container/Dockerfile`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/deploy/env/.env.example`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/deploy/env/.env.prod.example`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/deploy/compose/compose.docker.yaml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/deploy/compose/compose.docker.prod.yaml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/deploy/compose/compose.podman.yaml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/deploy/compose/compose.podman.prod.yaml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/deploy/compose/compose.nerdctl.yaml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/deploy/compose/compose.nerdctl.prod.yaml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`

**Interfaces:**

- Consumes: Target Interfaces; no Java dependency on Light sources.
- Produces: `course` on `master_data` with none/none; `course_schedule(course_id)` and `exam(id)/exam_paper(exam_id)/score(exam_id)` on shards; generated Service application defaults to ShardingSphereDataSource.

- [ ] **Step 1: Write failing Service tests**

Update Service `DataSourceModePropertiesTest`, `ShardingNodeMapTest`, `ShardingTopologyValidatorTest` and `EvaluationDataSourceModeTest` with the same public contracts as Task 1, but use this master-data rule:

```yaml
course:
  actualDataNodes: master_data.public.course
  databaseStrategy:
    none:
  tableStrategy:
    none:
```

The startup assertion must require ShardingSphereDataSource, no Flyway Bean and default SHARDING. Delete no-op Flyway and compatibility-validator tests.

- [ ] **Step 2: Run the Service archetype contract and observe RED**

Run:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml \
  -pl egon-cola-archetype-service -am clean integration-test
```

Expected: FAIL on the old SINGLE/default migration contract.

- [ ] **Step 3: Implement Service runtime, None and sharding rules**

Apply the exact Target Interfaces and FlywayAutoConfiguration exclusion to `EvaluationServiceApplication`.

Declare `course` with none/none. Preserve these standard routes:

```text
course_schedule -> course_id
exam            -> id
exam_paper      -> exam_id
score           -> exam_id
```

Keep binding group `exam,exam_paper,score`. Add `sharding_key_required_auditor` to all four sharded tables with `allowHintDisable: false`. Remove `!SINGLE`; rename `single` to `master_data` in physical and read/write topology.

- [ ] **Step 4: Rewrite Service Flyway resources**

Create exactly:

```text
db/migration/sharding/master-data/V20260726_001__init_evaluation_master_data_schema.sql
db/migration/sharding/shard/V20260726_002__init_evaluation_sharded_schema.sql
```

The first file creates only `course`. The second creates `course_schedule_0/1`, `exam_0/1`, `exam_paper_0/1`, `score_0/1`. Add the required three Chinese header comments. Set physical Flyway targets to `master_data`, `shard_0`, `shard_1`; read/write targets to their three primary names.

- [ ] **Step 5: Complete Service routing, deployment, docs and verifier**

Extend Service `ReadwriteRoutingIntegrationTest` to prove `course` CRUD uses only `master_data` and exam binding writes share one physical shard. Add a sharded DML audit rejection test.

Make the Service Dockerfile, six Compose files and four README files follow the exact three-primary/no-replica contract. Update `verify.groovy` with Service-specific counts: one none/none master table, four audited sharded tables, two final migration files, no default/single paths, default SHARDING and three PostgreSQL services.

Use `EVALUATION_SHARDING_MASTER_DATA_URL`, `EVALUATION_SHARDING_SHARD_0_URL`, `EVALUATION_SHARDING_SHARD_1_URL`, `EVALUATION_SHARDING_USERNAME`, and `EVALUATION_SHARDING_PASSWORD` in Compose; use the same three host-port keys and volume names defined in Task 1. Remove every `EVALUATION_SHARDING_SINGLE_*` and `EVALUATION_SINGLE_PRIMARY/REPLICA_*` reference, replacing read/write master-data variables with `EVALUATION_MASTER_DATA_PRIMARY/REPLICA_0_*`.

- [ ] **Step 6: Run Service verification and commit**

Run:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml \
  -pl egon-cola-archetype-service -am clean integration-test
git diff --check
```

Expected: Service archetype and generated multi-module Service project BUILD SUCCESS.

Commit:

```bash
git add egon-cola-archetypes/egon-cola-archetype-service
git commit -m "fix(archetype-service): require ShardingSphere datasource"
```

---

### Task 3: Web archetype 完整切换到 ShardingSphere-only

**Files:**

- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/config/datasource/DataSourceModeProperties.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/config/datasource/ShardingDataSourceProperties.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/config/datasource/ShardingDataSourcePropertiesLoader.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/config/datasource/ShardingNodeMap.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/config/datasource/UuidV7BucketShardingAlgorithm.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/config/datasource/ShardingTopologyValidator.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/config/datasource/ShardingSphereDataSourceConfiguration.java`
- Delete: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/config/datasource/ShardingDataSourceModeCondition.java`
- Delete: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/config/datasource/LogicalDataSourceFlywayMigrationStrategy.java`
- Delete: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/config/datasource/ShardingNodeMapCompatibilityValidator.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/java/starter/OrganizationApplication.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/application.yml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/application-test.yml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/datasource/sharding.yml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/datasource/sharding-readwrite.yml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/sharding/shardingsphere-sharding.yml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/sharding/shardingsphere-sharding-readwrite.yml`
- Delete: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/migration/default/V20260724_001__init_organization_default_schema.sql`
- Delete: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/migration/sharding/single/V20260724_002__init_organization_single_schema.sql`
- Delete: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/migration/sharding/shard/V20260724_003__init_organization_sharding_schema.sql`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/migration/sharding/master-data/V20260726_001__init_organization_master_data_schema.sql`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/migration/sharding/shard/V20260726_002__init_organization_sharded_schema.sql`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource/DataSourceModePropertiesTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource/ShardingDataSourcePropertiesTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource/ShardingDataSourcePropertiesLoaderTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource/ShardingNodeMapTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource/UuidV7BucketShardingAlgorithmTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource/ShardingTopologyValidatorTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource/ShardingYamlLoaderTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource/PhysicalDataSourceFactoryTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource/PhysicalDataSourceFlywayMigratorTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource/ShardingDataSourceBootstrapperTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource/ReadwriteRoutingIntegrationTest.java`
- Delete: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource/LogicalDataSourceFlywayMigrationStrategyTest.java`
- Delete: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource/ShardingNodeMapCompatibilityValidatorTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/migration/FlywayMigrationConventionTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/migration/LogicalSchemaParityTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/test/java/starter/OrganizationDataSourceModeTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/README.md`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/README.zh-CN.md`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/deploy/container/README.md`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/deploy/container/README.zh-CN.md`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/deploy/container/Dockerfile`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/deploy/env/.env.example`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/deploy/env/.env.prod.example`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/deploy/compose/compose.docker.yaml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/deploy/compose/compose.docker.prod.yaml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/deploy/compose/compose.podman.yaml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/deploy/compose/compose.podman.prod.yaml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/deploy/compose/compose.nerdctl.yaml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/deploy/compose/compose.nerdctl.prod.yaml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy`

**Interfaces:**

- Consumes: Target Interfaces; no Java dependency on Light or Service sources.
- Produces: six organization master-data tables on `master_data`; `school_classes` and `school_class_users` bind by `grade_id`; generated Web application defaults to ShardingSphereDataSource.

- [ ] **Step 1: Write failing Web tests**

Update Web mode/node-map/topology tests and `OrganizationDataSourceModeTest`. Add these exact None tables:

```text
users
roles
permissions
user_roles
role_permissions
grades
```

Each YAML rule must use `master_data.public.<table>` plus none/none. Tests must reject old `!SINGLE`, old mapping-version, missing audit and logical Flyway Bean creation.

- [ ] **Step 2: Run the Web archetype contract and observe RED**

Run:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml \
  -pl egon-cola-archetype-web -am clean integration-test
```

Expected: FAIL on the old SINGLE/default migration contract.

- [ ] **Step 3: Implement Web runtime, None and sharding rules**

Apply Target Interfaces and exclude FlywayAutoConfiguration in `OrganizationApplication`.

Keep the standard routes:

```text
school_classes    -> grade_id
school_class_users -> grade_id
```

Keep binding group `school_classes,school_class_users`. Add audit to both. Move all six master-data tables into `!SHARDING.tables` with none/none. Remove `!SINGLE`; rename logical group `single` to `master_data` throughout both topology modes.

- [ ] **Step 4: Rewrite Web Flyway resources**

Create exactly:

```text
db/migration/sharding/master-data/V20260726_001__init_organization_master_data_schema.sql
db/migration/sharding/shard/V20260726_002__init_organization_sharded_schema.sql
```

The first creates only users/RBAC/grades tables. The second creates `school_classes_0/1` and `school_class_users_0/1`. Add the required Chinese comments and map Flyway targets exactly to master-data versus shard locations.

- [ ] **Step 5: Complete Web routing, deployment, docs and verifier**

Extend Web `ReadwriteRoutingIntegrationTest` to prove `grades` CRUD uses only `master_data`, while one `gradeId` places school classes and members on one physical shard. Add a sharded DML audit rejection test.

Update Web Dockerfile, six Compose files, four README files and `verify.groovy`. Assert six None tables, two audited sharded tables, two final migrations, no default/single paths, default SHARDING, no replica containers and three primary PostgreSQL services.

Use `ORGANIZATION_SHARDING_MASTER_DATA_URL`, `ORGANIZATION_SHARDING_SHARD_0_URL`, `ORGANIZATION_SHARDING_SHARD_1_URL`, `ORGANIZATION_SHARDING_USERNAME`, and `ORGANIZATION_SHARDING_PASSWORD` in Compose; use the same three host-port keys and volume names defined in Task 1. Remove every `ORGANIZATION_SHARDING_SINGLE_*` and `ORGANIZATION_SINGLE_PRIMARY/REPLICA_*` reference, replacing read/write master-data variables with `ORGANIZATION_MASTER_DATA_PRIMARY/REPLICA_0_*`.

- [ ] **Step 6: Run Web verification and commit**

Run:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml \
  -pl egon-cola-archetype-web -am clean integration-test
git diff --check
```

Expected: Web archetype and generated multi-module Web project BUILD SUCCESS.

Commit:

```bash
git add egon-cola-archetypes/egon-cola-archetype-web
git commit -m "fix(archetype-web): require ShardingSphere datasource"
```

---

### Task 4: Repository-wide drift audit, full verification and documentation closure

**Files:**

- Modify: `docs/superpowers/specs/2026-07-23-archetype-shardingsphere-flyway-design.md`
- Modify: `docs/superpowers/plans/2026-07-23-archetype-shardingsphere-flyway-implementation.md`
- Modify: `docs/superpowers/plans/2026-07-26-archetype-shardingsphere-none-strategy.md`

**Interfaces:**

- Consumes: three individually green archetype commits from Tasks 1-3.
- Produces: one verified reactor result, no active-template SINGLE residue, an implemented Spec status and unambiguous current-plan pointer.

- [ ] **Step 1: Run the active-template residue audit**

Run:

```bash
rg -n --glob '!**/target/**' \
  'APP_DATASOURCE_MODE.*SINGLE|mode:.*SINGLE|!SINGLE|defaultDataSource|mapping-version|db/migration/default|db/migration/sharding/single|LogicalDataSourceFlywayMigrationStrategy|ShardingDataSourceModeCondition|ShardingNodeMapCompatibilityValidator' \
  egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources \
  egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources
```

Expected: no output. If a README needs to explain the rejected SINGLE mode, phrase it without copying an active configuration literal so the contract remains searchable.

- [ ] **Step 2: Verify cross-mode and cross-archetype contracts**

Compare the SHARDING rule suffix in each pair of mode files:

```bash
for rule_dir in \
  egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/sharding \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/sharding \
  egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/sharding
do
  diff -u \
    <(sed -n '/^  - !SHARDING/,$p' "${rule_dir}/shardingsphere-sharding.yml") \
    <(sed -n '/^  - !SHARDING/,$p' "${rule_dir}/shardingsphere-sharding-readwrite.yml")
done
```

Compare the two shared Java contracts across all three archetypes:

```bash
light_ds_dir=egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/config/datasource
service_ds_dir=egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/config/datasource
web_ds_dir=egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/config/datasource
for contract_file in DataSourceModeProperties.java ShardingNodeMap.java
do
  diff -u "${light_ds_dir}/${contract_file}" "${service_ds_dir}/${contract_file}"
  diff -u "${light_ds_dir}/${contract_file}" "${web_ds_dir}/${contract_file}"
done
```

Assert all 18 Compose files and all six final migration filenames:

```bash
compose_files=$(rg --files \
  egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/deploy/compose \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/deploy/compose \
  egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/deploy/compose)
test "$(printf '%s\n' "${compose_files}" | wc -l | tr -d ' ')" -eq 18
for compose_file in ${compose_files}
do
  rg -q 'APP_DATASOURCE_MODE: "SHARDING"' "${compose_file}"
  rg -q '^  postgres-master-data:' "${compose_file}"
  rg -q '^  postgres-shard-0:' "${compose_file}"
  rg -q '^  postgres-shard-1:' "${compose_file}"
  ! rg -q '^  .*replica.*:' "${compose_file}"
done

migration_files=$(rg --files egon-cola-archetypes \
  | rg '/db/migration/sharding/(master-data|shard)/V20260726_00[12]__[a-z0-9_]+\.sql$')
test "$(printf '%s\n' "${migration_files}" | wc -l | tr -d ' ')" -eq 6
test "$(printf '%s\n' "${migration_files}" | rg -c '/master-data/V20260726_001__')" -eq 3
test "$(printf '%s\n' "${migration_files}" | rg -c '/shard/V20260726_002__')" -eq 3
```

Expected: every `diff`, `rg` and `test` exits 0 without contract drift.

- [ ] **Step 3: Run the authoritative full archetype reactor**

Run:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml clean integration-test
```

Expected: all six reactor modules BUILD SUCCESS; Light, Service and Web generated projects pass tests, architecture checks and `verify.groovy`.

- [ ] **Step 4: Run final static checks**

Run:

```bash
git diff --check
git status --short
git log --oneline -6
```

Expected: no whitespace errors; before documentation closure, only the three approved documentation files may be modified.

- [ ] **Step 5: Close the documentation state**

Change the Spec status to:

```text
状态：已实现并通过 archetype 完整验证。
```

Add this notice immediately below the title of the old implementation plan:

```markdown
> 本计划已被 `2026-07-26-archetype-shardingsphere-none-strategy.md` 取代；不得再按应用级 SINGLE mode 或 `!SINGLE` 规则实施。
```

Mark every completed checkbox in this plan as `[x]` only after its command has actually passed.

- [ ] **Step 6: Commit documentation closure**

Run:

```bash
git add \
  docs/superpowers/specs/2026-07-23-archetype-shardingsphere-flyway-design.md \
  docs/superpowers/plans/2026-07-23-archetype-shardingsphere-flyway-implementation.md \
  docs/superpowers/plans/2026-07-26-archetype-shardingsphere-none-strategy.md
git commit -m "docs: close archetype sharding redesign"
```

- [ ] **Step 7: Apply completion verification skill**

Before claiming completion, invoke `superpowers:verification-before-completion`, inspect the latest full-reactor output and re-run `git status -sb`. Do not start any generated application or Compose stack.

## Completion Checklist

- [ ] `SINGLE` application mode is absent from active templates.
- [ ] `!SINGLE` is absent from active rules.
- [ ] Every master-data table uses one actual node plus none/none.
- [ ] Every sharded table uses UUIDv7 standard routing plus DML audit.
- [ ] `mapping-version` and node-map compatibility code are absent.
- [ ] FlywayAutoConfiguration is excluded and no logical Flyway Bean exists.
- [ ] Physical Flyway targets cover exactly all primary data sources.
- [ ] Default and sharding/single migration paths are absent.
- [ ] All 18 Compose files provide three primary PostgreSQL services and no replicas.
- [ ] dev/test/prod remain the only environment Profiles.
- [ ] Light, Service and Web module integration-tests pass independently.
- [ ] Full archetype reactor passes after all three commits.
- [ ] Worktree remains available until the user chooses the final branch integration option.
