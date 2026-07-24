# Egon COLA 事务消息组件

[English](README.md) | [中文](README.zh-CN.md)

## 解决什么问题

`egon-cola-component-transactional-outbox` 把业务变更和待发送消息原子地写入同一个
PostgreSQL 本地事务，再通过 HTTP、RabbitMQ 或自定义通道异步投递。如果应用在数据库
提交后停止，轮询任务会恢复这条已落库消息。

组件只包含两个 Maven 模块：

| 模块 | 用途 |
|---|---|
| `egon-cola-component-transactional-outbox-starter` | 公开 API、PostgreSQL/JDBC 存储、轮询、重试、清理、HTTP/RabbitMQ 适配、自动配置和指标 |
| `egon-cola-component-transactional-outbox-test` | 组件集成测试，以及恰好两个可执行的消费端示例 |

实现采用 Transactional Outbox 模式。`TransactionalOutbox` 是业务侧门面，
`DeliveryHandler` 是不同投递通道的策略/适配器扩展点。

## 投递保证与重复窗口

本组件保证的是 **at-least-once（至少一次）**，不是 exactly-once：

1. `enqueue` 在调用方当前业务事务中写入 outbox 记录。
2. 提交后事件只作为降低延迟的唤醒提示。
3. PostgreSQL 轮询通过 `FOR UPDATE SKIP LOCKED` 抢占到期或租约过期的记录。
4. 对外投递在数据库事务之外执行。
5. 重试、租约恢复和带 owner 条件的更新使记录依次处于
   `PENDING`、`PROCESSING`、`RETRY_WAIT`、`SUCCEEDED` 或 `DEAD`。

远端处理成功后、本应用记录本地成功前如果崩溃，同一个远端副作用可能再次执行。因此，
所有下游都必须使用稳定的 `messageId` 去重。HTTP 会把它放在 `Idempotency-Key` 和
`X-Egon-Cola-Message-Id`，RabbitMQ 会把它设置为 AMQP `messageId`。

可选的 `idempotencyKey` 会在对应记录仍保留时阻止重复 outbox 记录。同一个 key
如果对应不同的消息内容会直接报冲突，不会静默接受；`availableAt` 不参与内容指纹。

## 不适用的场景

以下需求不适合使用本组件：

- 不允许依赖下游去重、但要求远端副作用 exactly-once；
- 要求跨数据库或外部服务的分布式事务；
- 要求参与 Reactive/R2DBC 事务；
- 业务数据和 outbox 表位于不同数据库；
- 需要 Inbox、管理后台、重放 API 或自动声明 Broker 拓扑。

兼容边界是 Java 21、Spring Boot 3.5.x、PostgreSQL 和命令式 Spring JDBC。
组件不依赖 Redis。

## Maven 依赖

引入组件 BOM，业务应用只依赖 starter：

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
        <artifactId>egon-cola-component-transactional-outbox-starter</artifactId>
    </dependency>
</dependencies>
```

应用还需要提供唯一的 `ObjectMapper`、JDBC `DataSource` 以及与之匹配的
`PlatformTransactionManager`。只在使用 HTTP 投递时增加 `spring-web`，只在使用
RabbitMQ 投递时增加 `spring-rabbit`。

## PostgreSQL 迁移

引入 starter **不会自动建表**，也不会替业务应用执行 Flyway。请复制：

```text
egon-cola-component-transactional-outbox-starter/src/main/resources/
db/transactional-outbox/postgresql/V1__create_transactional_outbox_schema.sql
```

到消费应用自己的 `classpath:db` 迁移序列，并改成该应用的下一个 Flyway 版本，例如
`V42__create_transactional_outbox_schema.sql`。不要直接把组件内文件放入自动扫描目录而
不分配消费端自己的版本号。

启动时默认校验表结构；缺少预期表结构会快速失败。只有在其他地方完成结构校验时才建议关闭：

```yaml
egon:
  cola:
    component:
      transactional-outbox:
        storage:
          validate-schema: false
```

## 直接 API 示例

推荐显式调用 API，让业务写入和 enqueue 处于同一个命令式 Spring 事务：

```java
@Service
public class OrderApplicationService {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionalOutbox transactionalOutbox;

    public OrderApplicationService(
            JdbcTemplate jdbcTemplate,
            TransactionalOutbox transactionalOutbox
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionalOutbox = transactionalOutbox;
    }

    @Transactional
    public OutboxReceipt createOrder(String orderId) {
        jdbcTemplate.update(
                "insert into business_order(id) values (?) on conflict do nothing",
                orderId
        );
        return transactionalOutbox.enqueue(OutboxMessage.builder()
                .idempotencyKey("order:created:" + orderId)
                .channel("order-events")
                .destination("order-created-v1")
                .payload(new OrderCreatedEvent(orderId))
                .schemaVersion("1")
                .build());
    }

    public record OrderCreatedEvent(String orderId) {
    }
}
```

`enqueue` 要求当前存在已开启事务同步、且绑定到组件配置 `DataSource` 的 Spring
事务。缺少兼容事务时，会在写入前直接失败。

## 注解示例

`@TransactionalMessage` 可以从同步方法的返回值中解析一条 `OutboxMessage`。它会使用
指定事务管理器创建或加入 `REQUIRED` 事务：

```java
@Service
public class AnnotatedOrderApplicationService {

    private final JdbcTemplate jdbcTemplate;

    public AnnotatedOrderApplicationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @TransactionalMessage(message = "#result.outboxMessage()")
    public CreateOrderResult createOrder(String orderId) {
        jdbcTemplate.update("insert into business_order(id) values (?)", orderId);
        return new CreateOrderResult(
                orderId,
                OutboxMessage.builder()
                        .idempotencyKey("order:annotated-created:" + orderId)
                        .channel("order-events")
                        .destination("order-created-v1")
                        .payload(new OrderCreatedEvent(orderId))
                        .build()
        );
    }

    public record CreateOrderResult(String orderId, OutboxMessage outboxMessage) {
    }

    public record OrderCreatedEvent(String orderId) {
    }
}
```

表达式必须解析出非空 `OutboxMessage`，否则整个事务回滚。方法必须是经过代理的 Spring
Bean 方法，并且为 public、非 static、非 final、同步返回；`Future`、
`CompletionStage` 和响应式返回类型会被拒绝。如果方法同时声明 `@Transactional`，
必须使用 `REQUIRED`、非只读并选择同一个事务管理器。Spring 代理的常规限制（例如
self-invocation）同样适用。

## DataSource 选择

只有一个 `DataSource` 和一个事务管理器时无需额外配置。存在多个候选时，组件会选择唯一
的 `@Primary`；否则必须同时显式指定两个 Bean：

```yaml
egon:
  cola:
    component:
      transactional-outbox:
        storage:
          data-source-bean-name: orderDataSource
          transaction-manager-bean-name: orderTransactionManager
```

对于 `DataSourceTransactionManager`，启动校验还会检查事务管理器持有的就是选定
`DataSource`。业务写入和 outbox 写入必须使用同一个数据库事务。

## 核心运行配置

默认值均为有限、有界配置：

```yaml
egon:
  cola:
    component:
      transactional-outbox:
        enabled: true
        polling:
          enabled: true
          fixed-delay: 1s
          batch-size: 100
          concurrency: 4
        delivery:
          timeout: 10s
          lease-duration: 60s
          queue-capacity: 1000
        retry:
          max-attempts: 10
          initial-delay: 1s
          multiplier: 2.0
          max-delay: 5m
          jitter: 0.2
        payload:
          max-bytes: 1MB
          max-header-count: 64
          max-header-bytes: 16KB
        shutdown:
          grace-period: 30s
```

到期时间和租约过期均以数据库时间为准。每次 claim 都使用包含节点标识和 claim UUID 的
唯一 owner token，成功、重试、死亡状态都只允许当前 owner 以 CAS 方式更新。

## HTTP 投递

HTTP 默认关闭。消息只保存逻辑 destination，不允许 outbox 记录携带任意 URL：

```yaml
egon:
  cola:
    component:
      transactional-outbox:
        http:
          enabled: true
          destinations:
            order-api:
              uri: https://service.example.internal/events/orders
              method: POST
              connect-timeout: 2s
              read-timeout: 10s
              fixed-headers:
                X-Event-Source: order-service
```

enqueue 时使用 `channel("http")` 和 `destination("order-api")`。客户端不会跟随重定向。
凭证应由 `HttpCredentialProvider` Bean 在投递时提供，不要把 `Authorization` 或
`Cookie` 持久化到 `OutboxMessage`：

```java
@Bean
HttpCredentialProvider outboxHttpCredentials(TokenSupplier tokenSupplier) {
    return destination -> Map.of(
            HttpHeaders.AUTHORIZATION,
            "Bearer " + tokenSupplier.currentToken()
    );
}
```

组件日志不会记录 payload、凭证、完整 URL 或响应体。

## RabbitMQ 投递

RabbitMQ 默认关闭，也不会创建 exchange、queue 或 binding。业务应用必须单独维护拓扑，
并开启 correlated publisher confirm、publisher return 和 mandatory：

```yaml
spring:
  rabbitmq:
    publisher-confirm-type: correlated
    publisher-returns: true
    template:
      mandatory: true

egon:
  cola:
    component:
      transactional-outbox:
        rabbitmq:
          enabled: true
          confirm-timeout: 5s
          destinations:
            order-events:
              exchange: business.events
              routing-key: order.created.v1
              mandatory: true
```

enqueue 时使用 `channel("rabbitmq")` 和 `destination("order-events")`。只有收到关联 ACK
且没有 mandatory return 才算投递成功。NACK、return、超时或连接异常会按规则重试或
进入 `DEAD`；每次尝试都保留同一个稳定 `messageId`。

## 自定义 DeliveryHandler

每个自定义通道注册一个 handler。destination 会在 enqueue 阶段、插入记录前完成校验：

```java
@Bean
DeliveryHandler orderEventsDeliveryHandler(OrderEventClient client) {
    return new DeliveryHandler() {
        @Override
        public String channel() {
            return "order-events";
        }

        @Override
        public void validateDestination(String destination) {
            if (!"order-created-v1".equals(destination)) {
                throw new OutboxValidationException("Unknown order destination");
            }
        }

        @Override
        public DeliveryResult deliver(DeliveryContext context) {
            return client.send(context.messageId(), context.payload())
                    ? DeliveryResult.success()
                    : DeliveryResult.retryableFailure(
                            "ORDER_DELIVERY_UNAVAILABLE",
                            "ORDER_DELIVERY_UNAVAILABLE"
                    );
        }
    };
}
```

handler 在业务/数据库事务之外执行，必须遵守 `context.deadline()`，不能无限等待；对明确
不可重试的数据应返回 permanent failure。

## 重试、租约、DEAD 与崩溃恢复

重试采用带 jitter 的有界指数退避。可重试结果进入 `RETRY_WAIT`；耗尽
`max-attempts`、永久失败或未知 channel 会进入 `DEAD`。可以注册
`OutboxDeadLetterListener` 观察死亡状态，但监听器异常不会撤销已保存的状态。

worker 在 `PROCESSING` 时崩溃，其他 worker 可在 `locked_until` 后重新 claim。旧 worker
不能覆盖新 owner，因为所有终态更新都比较 owner token。单条毒消息也不会阻断同批次的
其他记录。

## 清理与幂等窗口

清理默认关闭：

```yaml
egon:
  cola:
    component:
      transactional-outbox:
        cleanup:
          enabled: true
          success-retention: 7d
          fixed-delay: 1h
          batch-size: 500
```

清理只删除过期的 `SUCCEEDED`，始终保留 `DEAD`。成功记录删除后，对应
`idempotencyKey` 在 outbox 一侧的去重窗口也随之结束；下游去重保留期必须覆盖业务真正
需要的时长。

## 指标与安全日志

存在唯一 Micrometer `MeterRegistry` 时，组件记录：

- `egon.cola.outbox.backlog`
- `egon.cola.outbox.enqueue`
- `egon.cola.outbox.claim`
- `egon.cola.outbox.delivery`
- `egon.cola.outbox.delivery.duration`
- `egon.cola.outbox.retry`
- `egon.cola.outbox.dead`
- `egon.cola.outbox.lease_lost`

指标标签只使用 channel、result 等有界值，不使用 messageId、destination 或错误文本。
日志和持久化错误摘要不会记录 payload、凭证、`Authorization`、`Cookie`、响应体或
完整 URL。

## 明确的支持边界

支持：Java 21、Spring Boot 3.5.x、PostgreSQL、命令式 JDBC 事务、HTTP 投递、
RabbitMQ 投递和自定义同步 handler。

不支持：Reactive/R2DBC 事务、跨数据库事务、分布式事务、exactly-once、自动执行
Flyway、Admin/UI、重放 API、Inbox 处理和自动声明 Broker 拓扑。
