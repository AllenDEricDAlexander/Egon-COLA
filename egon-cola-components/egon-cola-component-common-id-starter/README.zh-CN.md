# egon-cola-component-common-id-starter

[English](README.md) | 中文

## 简要介绍

该 Starter 是 Egon COLA 有状态、纯 JDK Snowflake ID 生成器的 Spring Boot 3 接入层。算法仍位于不依赖 Spring 的 `egon-cola-component-common-id`；本模块只负责配置绑定和默认 Bean 装配。

数据库 `BIGINT` 主键应调用 `LongIdGenerator.nextLongId()`。为降低升级破坏性，继承的 `IdGenerator.nextId()` 会返回同一个 long ID 的十进制字符串。

## Maven 依赖

先 import `egon-cola-components-bom`，再无版本引入 Starter：

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-common-id-starter</artifactId>
</dependency>
```

非 Spring 应用直接依赖纯 Java 算法模块：

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-common-id</artifactId>
</dependency>
```

## 配置

Starter 启用时必须显式配置 `machine-id`。实现不会根据 IP、MAC、hostname、端口、进程号、随机值或哈希值自动推导机器 ID。

```yaml
egon:
  cola:
    component:
      id:
        enabled: true
        machine-id: 17
        max-clock-backward: 5ms
```

| 配置项 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `egon.cola.component.id.enabled` | `boolean` | `true` | 是否装配默认 Snowflake Bean。 |
| `egon.cola.component.id.machine-id` | `long` | 无 | 必填，节点 ID 范围为 `0..1023`。 |
| `egon.cola.component.id.max-clock-backward` | `Duration` | `5ms` | 允许短暂等待恢复的最大时钟回拨量。 |

`machine-id` 缺失或越界会在 Spring 上下文启动阶段失败。`enabled=false` 时不创建生成器。业务自定义 `IdGenerator` 或 `LongIdGenerator` Bean 后，默认自动配置会退让。

## Spring 使用

```java
import org.springframework.stereotype.Service;
import top.egon.cola.component.common.id.generator.LongIdGenerator;

@Service
public class OrderService {

    private final LongIdGenerator idGenerator;

    public OrderService(LongIdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    public long createOrder() {
        long orderId = idGenerator.nextLongId();
        // 将 orderId 写入 BIGINT 列。
        return orderId;
    }
}
```

同一个 Bean 也可以按 `IdGenerator` 注入；`nextId()` 返回 `nextLongId()` 的十进制字符串。

## 非 Spring 使用

每个进程创建一个长期存活的生成器实例，并传入部署系统分配的机器 ID：

```java
import java.time.Duration;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;

LongIdGenerator idGenerator = new SnowflakeIdGenerator(17, Duration.ofMillis(5));
long id = idGenerator.nextLongId();
String text = idGenerator.nextId();
```

不要按请求重复创建生成器。实例内保存的时间戳和序列状态是保证该实例严格递增的基础。

## 数据库 `BIGINT`

数据库使用有符号 64 位列，Java 侧按 `long` 绑定：

```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    created_at TIMESTAMP NOT NULL
);
```

```java
preparedStatement.setLong(1, idGenerator.nextLongId());
```

生成值为正数。如果直接暴露给 JavaScript 客户端，建议序列化为字符串，因为 JavaScript number 无法精确表示全部 64 位整数。

## 固定位布局

位布局和 Epoch 是协议常量，不允许业务配置：

```text
0 | 41 位毫秒时间差 | 10 位机器 ID | 12 位序列号
```

- 符号位固定为 `0`，生成正数 `long`。
- Epoch 固定为 `2026-01-01T00:00:00Z`。
- 时间戳为 41 位毫秒差，约可使用 69.7 年。
- 机器 ID 为 10 位，最多 1,024 个节点（`0..1023`）。
- 序列号为 12 位，单节点每毫秒最多 4,096 个 ID（`0..4095`）。

同一个生成器实例线程安全、不重复，并在 CAS 成功的线性化点严格递增。机器 ID 正确分配且系统时间正常时，不同节点的 ID 全局唯一并按时间趋势有序；无中心协调条件下，不保证跨节点按照真实业务发生顺序全局严格递增。

## 解析

```java
import top.egon.cola.component.common.id.snowflake.SnowflakeId;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdParser;

SnowflakeId decoded = SnowflakeIdParser.parse(id);
decoded.generatedAt();
decoded.machineId();
decoded.sequence();
```

解析器拒绝负数，并始终使用与生成器相同的固定 Epoch 和位布局。

## 时钟回拨策略

- 回拨量不超过 `max-clock-backward` 时，通过短暂 park 和单调时钟截止时间等待系统时间追平。
- 等待过程可中断；中断会停止生成并保留线程中断标记。
- 大幅回拨，或小幅回拨未在截止时间内恢复时，立即抛出 `ClockMovedBackwardException`，异常包含当前时间、最后使用时间、回拨毫秒数和机器 ID。
- 严重回拨时不会无条件使用虚构的逻辑时间继续生成；进程重启会丢失内存水位，这样做不安全。
- 41 位时间戳耗尽时抛出 `SnowflakeTimestampOutOfRangeException`，不会让字段回绕。

所有节点都应使用可靠 NTP 并监控时间同步。纯内存生成器无法在严重时钟回拨并重启后无条件保证绝对不重复。

## 机器 ID 分配

所有同时运行的生成器都必须使用唯一 `machine-id`。两个节点复用同一个 ID 可能产生冲突。

Kubernetes StatefulSet 可以在副本数不超过 1,024 时，把稳定 ordinal 映射为机器 ID：

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: order-service
spec:
  serviceName: order-service
  replicas: 3
  template:
    spec:
      containers:
        - name: app
          env:
            - name: EGON_COLA_COMPONENT_ID_MACHINE_ID
              valueFrom:
                fieldRef:
                  fieldPath: metadata.labels['apps.kubernetes.io/pod-index']
```

部署前应确认集群提供 pod-index label、滚动发布期间不会有两个存活 Pod 复用同一 ordinal，并保证其他工作负载不占用同一分配区间。普通 Deployment 的随机 Pod 名称不是稳定机器 ID，不应直接解析或哈希成机器 ID。

## UUIDv7 兼容和能力边界

`UuidV7` 与 `UuidV7Generator` 暂时以纯 JDK RFC 9562 兼容 API 保留在 `egon-cola-component-common-id`，已标记待删除，并且不再依赖 `uuid-creator`。Starter 不会自动装配 UUIDv7，默认数据库主键方案是 Snowflake。

对于 UUIDv7 线协议、`VARCHAR(36)` 字段或 UUID 专用分片/校验规则，不要机械替换为 long；只有在明确修改契约和迁移数据后才能迁移。

本组件不提供机器 ID 自动发现、Redis 租约、数据库号段、批量预取、持久化水位或网络协调。

## 验证

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-common-id-starter -am clean test
```
