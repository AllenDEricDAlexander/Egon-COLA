# Egon COLA Dynamic Config Center Component

## 简要介绍

`egon-cola-component-dynamic-config-center` 是 Egon COLA 的动态配置中心组件，提供业务应用 starter SDK 和独立 admin 后端。业务应用通过 `@DdcValue` 绑定配置字段，starter 负责默认值注入、配置拉取、Redis 变更监听、运行时刷新、实例注册、心跳和 ACK 上报；admin 负责配置项管理、版本管理、发布任务、实例状态、Redis 缓存和一致性策略。

组件边界是后端闭环能力，不包含 UI、账号登录、RBAC/权限管理，也不包含 MySQL、Spring Boot 2.7 或 JDK 17 兼容目标。

## 模块结构

| Module | 说明 |
|---|---|
| `egon-cola-component-dynamic-config-center-starter` | 业务应用 SDK，提供 `@DdcValue`、字段绑定、配置刷新、Redis 订阅、Admin OpenAPI 客户端、注册/心跳/ACK |
| `egon-cola-component-dynamic-config-center-admin` | 独立管理后端，提供 REST API、JPA 持久化、Flyway 初始化脚本、Redis 缓存、发布任务和实例管理 |
| `egon-cola-component-dynamic-config-center-test` | starter 样例应用和刷新流程验证 |

## 功能说明

### Starter SDK

业务应用引入 starter 后，`DdcAutoConfig` 会自动装配：

| Bean / 能力 | 说明 |
|---|---|
| `DdcValueConverter` | 将字符串配置转换为 `String`、`Integer`、`Long`、`Boolean`、`Double`、`BigDecimal`、`Enum`、`List<String>` 或 JSON 对象 |
| `DdcLocalConfigRepository` | 保存本地配置值、版本号和字段绑定关系 |
| `DdcFieldBindingService` | 扫描 `@DdcValue` 字段，注入默认值，并在刷新时反射更新字段 |
| `DdcAdminClient` | 通过 HTTP 调用 admin OpenAPI |
| `DdcRefreshService` | 接收发布消息，比较本地版本，更新字段并上报 SUCCESS / FAILED / IGNORED ACK |
| `DdcRedisChangeListener` | 订阅 Redis Topic，接收 admin 发布的配置变更消息 |
| `DdcInstanceService` | 注册实例、发送心跳、下线实例 |

`@DdcValue` 支持两种写法：

```java
@DdcValue("rateLimit:100")
private volatile Integer rateLimit;

@DdcValue(value = "", key = "downgradeSwitch", defaultValue = "false", type = Boolean.class)
private volatile Boolean downgradeSwitch;
```

### Admin 后端

Admin API 基础路径为 `/api/v1/ddc`：

| API | 说明 |
|---|---|
| `GET /api/v1/ddc/manifest` | 管理 UI 发现用 manifest |
| `GET /api/v1/ddc/apps` / `POST /api/v1/ddc/apps` | 应用查询和创建 |
| `GET /api/v1/ddc/namespaces` / `POST /api/v1/ddc/namespaces` | 命名空间查询和创建 |
| `GET /api/v1/ddc/configs` | 查询配置项 |
| `POST /api/v1/ddc/configs` | 创建配置项 |
| `PUT /api/v1/ddc/configs/{id}` | 更新配置项 |
| `DELETE /api/v1/ddc/configs/{id}` | 软删除配置项 |
| `POST /api/v1/ddc/configs/{id}/publish` | 发布配置 |
| `GET /api/v1/ddc/configs/{id}/versions` | 查询配置版本 |
| `POST /api/v1/ddc/configs/{id}/rollback` | 回滚配置版本 |
| `GET /api/v1/ddc/publish-tasks` | 查询发布任务 |
| `GET /api/v1/ddc/publish-tasks/{changeId}` | 查询单个发布任务 |
| `GET /api/v1/ddc/instances` | 查询实例 |
| `POST /api/v1/ddc/cache/rebuild` | 重建 Redis 缓存 |
| `GET /api/v1/ddc/cache/check` | 校验数据库和缓存一致性 |

SDK OpenAPI 位于 `/api/v1/ddc/openapi`，由 starter 调用：

| API | 说明 |
|---|---|
| `POST /instances/register` | 实例注册 |
| `POST /instances/heartbeat` | 实例心跳 |
| `POST /instances/offline` | 实例下线 |
| `GET /configs/pull` | 拉取全量配置 |
| `GET /configs/{key}` | 拉取单个配置 |
| `POST /publish/ack` | 上报发布 ACK |
| `POST /defaults/report` | 上报注解默认值 |

### 发布一致性

Admin 支持三种发布模式：

| PublishMode | 行为 |
|---|---|
| `ASYNC` | 消息写入 Redis 并发布后立即完成 |
| `STRONG_ALL_ACK` | 所有目标实例成功 ACK 才成功；全部目标结束且存在失败/超时则失败 |
| `STRONG_QUORUM_ACK` | 多数实例成功 ACK 即成功；失败/超时数量导致无法达成多数时失败 |

### Redis Key

starter 和 admin 共享 `DdcKeys`：

| Key / Topic | 说明 |
|---|---|
| `ddc:config:{appCode}:{env}:{namespace}:{key}` | 配置值 |
| `ddc:version:{appCode}:{env}:{namespace}:{key}` | 配置版本 |
| `ddc:instance:{appCode}:{env}:{namespace}:{instanceId}` | 实例详情 |
| `ddc:instances:{appCode}:{env}:{namespace}` | 实例集合 |
| `ddc:publish:{changeId}` | 发布任务 |
| `ddc:publish:ack:{changeId}` | 发布 ACK |
| `ddc:topic:{appCode}:{env}:{namespace}` | 配置发布 Topic |

## 依赖方式

业务应用引入 starter：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>top.egon</groupId>
            <artifactId>egon-cola-components-bom</artifactId>
            <version>${egon-cola.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-dynamic-config-center-starter</artifactId>
    </dependency>
</dependencies>
```

Admin 服务作为组件内独立应用构建和部署：

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin -am package -DskipTests
```

## 配置说明

业务应用配置前缀为 `egon.cola.component.ddc`：

```yaml
spring:
  application:
    name: order-service

egon:
  cola:
    component:
      ddc:
        enabled: true
        app-code: order-service
        env: dev
        namespace: default
        admin:
          endpoint: http://localhost:18080
          access-key:
          secret-key:
          signature-enabled: false
        redis:
          enabled: true
          host: 127.0.0.1
          port: 6379
          password:
          database: 0
        instance:
          heartbeat-interval-seconds: 10
          heartbeat-timeout-seconds: 30
        consistency:
          ack-enabled: true
          fail-fast: true
```

Admin 默认端口和 Flyway 配置：

```yaml
server:
  port: 18080

spring:
  application:
    name: egon-cola-ddc-admin
  flyway:
    locations: classpath:db/postgresql

egon:
  cola:
    component:
      ddc:
        enabled: false
        admin:
          redis:
            host: 127.0.0.1
            port: 6379
            database: 0
```

Flyway 脚本位置：

```text
classpath:db/postgresql/V1__create_ddc_schema.sql
classpath:db/sqlite/V1__create_ddc_schema.sql
```

## 完整的使用示例

### 1. 业务应用绑定动态配置

配置字段建议使用 `volatile`，确保刷新后其他线程可以及时读取到新值。

```java
package demo.order;

import org.springframework.stereotype.Service;
import top.egon.cola.component.ddc.annotation.DdcValue;

@Service
public class OrderSwitchService {

    @DdcValue("downgradeSwitch:false")
    private volatile Boolean downgradeSwitch;

    @DdcValue(value = "rateLimit:100", type = Integer.class)
    private volatile Integer rateLimit;

    public boolean shouldDowngrade() {
        return Boolean.TRUE.equals(downgradeSwitch);
    }

    public int rateLimit() {
        return rateLimit == null ? 100 : rateLimit;
    }
}
```

### 2. 创建配置项

```bash
curl -X POST 'http://localhost:18080/api/v1/ddc/configs?operator=admin' \
  -H 'Content-Type: application/json' \
  -d '{
    "appCode": "order-service",
    "env": "dev",
    "namespace": "default",
    "configKey": "rateLimit",
    "configValue": "100",
    "defaultValue": "100",
    "valueType": "INTEGER",
    "description": "订单接口限流阈值"
  }'
```

### 3. 发布配置

```bash
curl -X POST 'http://localhost:18080/api/v1/ddc/configs/{configId}/publish?operator=admin' \
  -H 'Content-Type: application/json' \
  -d '{
    "configValue": "200",
    "publishMode": "STRONG_QUORUM_ACK",
    "expectedVersion": 1,
    "timeoutMs": 30000
  }'
```

发布后 admin 会更新数据库版本、写入 Redis 配置值、发布 `DdcPublishMessage`。业务应用收到消息后，`DdcRefreshService` 会更新本地字段并上报 ACK。

### 4. 查询发布任务和实例

```bash
curl 'http://localhost:18080/api/v1/ddc/publish-tasks'
curl 'http://localhost:18080/api/v1/ddc/publish-tasks/{changeId}'
curl 'http://localhost:18080/api/v1/ddc/instances?appCode=order-service&env=dev&namespace=default'
```

### 5. 本地无 admin 时使用注解默认值

如果本地开发不启动 admin，可以关闭 Redis 并关闭 fail-fast，字段会保持 `@DdcValue` 中的默认值：

```yaml
egon:
  cola:
    component:
      ddc:
        enabled: true
        app-code: demo-app
        env: dev
        namespace: default
        redis:
          enabled: false
        consistency:
          fail-fast: false
```

## 设计思想和实现细节

### 设计思想

1. starter 和 admin 分离。业务应用只依赖轻量 SDK，管理端独立部署。
2. 配置定位使用 `appCode + env + namespace + configKey`，避免不同应用、环境、命名空间互相污染。
3. 默认值随业务代码声明，admin 不可用时仍能保持本地可启动。
4. 发布链路先写数据库和版本，再写 Redis 并发布消息，业务侧通过版本号判断是否需要刷新。
5. 一致性策略独立封装，异步发布、全量 ACK、多数 ACK 三种模式共享发布任务和 ACK 模型。

### 实现细节

- `DdcBeanPostProcessor` 扫描 Spring Bean 字段上的 `@DdcValue`，并委托 `DdcFieldBindingService` 绑定。
- `DdcValueParser` 支持 `key:defaultValue` 表达式，也支持通过注解的 `key`、`defaultValue`、`type` 显式声明。
- `DdcValueConverter` 将字符串配置转换为目标字段类型；复杂对象走 Jackson JSON 反序列化。
- `DdcLocalConfigRepository` 保存配置版本，收到旧版本消息时会 ACK 为 `IGNORED`。
- `DdcPublishService.publish` 使用 `TransactionTemplate` 准备发布任务、目标 ACK 记录和版本记录，然后写 Redis 并发布消息。
- `PublishFailureRecorder` 在发布异常时记录失败任务，避免异常链路没有可追踪的发布记录。
- `DdcTraceIdFilter` 和全局异常处理器在 admin 侧提供统一 trace 和错误响应。
- admin 的 PostgreSQL 和 SQLite 脚本各自位于 `classpath:db/postgresql`、`classpath:db/sqlite`，新增数据库变更必须新增 Flyway 版本文件，不能改旧脚本。

## 边界和注意事项

- 不包含 UI、账号、登录、角色、权限、RBAC。
- 当前脚本覆盖 PostgreSQL 和 SQLite，不包含 MySQL。
- `@DdcValue` 字段通过反射更新，字段不应是 `final`。
- 默认值来自代码注解，运行时发布值来自 admin/Redis。
- 开启强一致发布时，业务实例必须能正常 ACK，否则发布任务会停留在运行中或最终失败。

## 验证命令

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter -am test
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin -am test
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test -am -Dsurefire.failIfNoSpecifiedTests=false test
```
