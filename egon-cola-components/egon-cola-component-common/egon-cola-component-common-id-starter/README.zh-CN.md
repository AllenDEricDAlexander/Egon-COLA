# egon-cola-component-common-id-starter

[English](README.md) | 中文

## 简要介绍

这是 Egon COLA 唯一的 ID 模块，在同一个 Starter 中同时提供有状态、纯 JDK 的 Snowflake 接口与算法，以及 Spring Boot 3 配置绑定。核心算法包不导入 Spring API，应用统一依赖该 Starter Artifact。

数据库 `BIGINT` 主键应调用 `SnowflakeIdGenerator.nextLongId()`。为降低升级破坏性，继承的 `IdGenerator.nextId()` 会返回同一个 long ID 的十进制字符串。两者都是同一个进程级引擎上的静态入口；`SnowflakeIdGenerator` 不可构造，也没有重置入口。`LongIdGenerator` 仍是具名策略类型，并把两个操作都声明为抽象方法。

## Maven 依赖

先 import `egon-cola-components-bom`，再无版本引入 Starter：

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-common-id-starter</artifactId>
</dependency>
```

非 Spring 应用同样依赖该 Artifact，并在启动时调用一次 `SnowflakeIdGenerator.initialize(machineId, maxClockBackward)`；Spring Boot 自动配置代业务完成这次绑定，且只在 Spring Boot 应用上下文中激活。

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
| `egon.cola.component.id.enabled` | `boolean` | `true` | 是否绑定默认 Snowflake 引擎。 |
| `egon.cola.component.id.machine-id` | `long` | 无 | 必填，节点 ID 范围为 `0..1023`。 |
| `egon.cola.component.id.max-clock-backward` | `Duration` | `5ms` | 允许短暂等待恢复的最大时钟回拨量。 |

`machine-id` 缺失或越界会在 Spring 上下文启动阶段失败。`enabled=false` 时不绑定引擎。业务自定义 `IdGenerator` Bean（或 `LongIdGenerator` Bean，它是前者的子类型）后，默认自动配置会退让。

## Spring 使用

```java
import org.springframework.stereotype.Service;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;

@Service
public class OrderService {

    public long createOrder() {
        long orderId = SnowflakeIdGenerator.nextLongId();
        // 将 orderId 写入 BIGINT 列。
        return orderId;
    }
}
```

Starter 不发布任何生成器 Bean，因此业务代码无需注入：自动配置在配置阶段绑定引擎，业务代码调用静态入口。`nextId()` 返回 `nextLongId()` 的十进制字符串。

## 非 Spring 使用

每个进程用部署系统分配的机器 ID 绑定一次，然后调用静态入口：

```java
import java.time.Duration;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;

SnowflakeIdGenerator.initialize(17L, Duration.ofMillis(5));
long id = SnowflakeIdGenerator.nextLongId();
String text = SnowflakeIdGenerator.nextId();
```

配置完全相同的重复 `initialize` 调用会复用已绑定的引擎；配置不同则被拒绝，而不是给存活的序列重新播种。`SnowflakeLongIdGenerator` 是该门面背后的具名引擎，只有需要隔离时钟的测试夹具与基准才直接构造它。

不要按请求重新绑定。已绑定引擎在内存中保存的时间戳和序列状态，是进程内严格递增的基础。

## 数据库 `BIGINT`

数据库使用有符号 64 位列，Java 侧按 `long` 绑定：

```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    created_at TIMESTAMP NOT NULL
);
```

```java
preparedStatement.setLong(1, SnowflakeIdGenerator.nextLongId());
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

为保证生成器永不返回 `0`，全零编码被保留。因此只有机器 `0` 在时间恰好等于
Epoch 的那一毫秒从序列 `1` 开始；正常运行期间的每个毫秒仍保留完整的 4,096 个序列容量。

已绑定的引擎线程安全、不重复，并在 CAS 成功的线性化点严格递增。机器 ID 正确分配且系统时间正常时，不同节点的 ID 全局唯一并按时间趋势有序；无中心协调条件下，不保证跨节点按照真实业务发生顺序全局严格递增。

## 时钟回拨策略

- 回拨量不超过 `max-clock-backward` 时，通过短暂 park 和单调时钟截止时间等待系统时间追平。
- 等待过程可中断：进入等待前已被中断、或等待期间被中断的线程，会以 `IdGenerationInterruptedException` 停止生成，中断标记保持不变。
- 大幅回拨，或小幅回拨未在截止时间内恢复时，立即抛出 `ClockMovedBackwardException`，异常包含当前时间、最后使用时间、回拨毫秒数和机器 ID。
- 严重回拨时不会无条件使用虚构的逻辑时间继续生成；进程重启会丢失内存水位，这样做不安全。
- 系统时间落在可表示窗口之外时抛出 `SnowflakeTimestampOutOfRangeException`，不会让 41 位字段回绕。该窗口在 41 位毫秒差耗尽时结束，也排除早于 Epoch 的任何时刻。

所有节点都应使用可靠 NTP 并监控时间同步。纯内存生成器无法在严重时钟回拨并重启后无条件保证绝对不重复。

上述配置失败与生成失败统一抛出 common-core 的 `CommonException`，携带 `ResultCode.SYSTEM_ERROR` 的整型 `getCode()`、字符串 `getStatus()`，以及包含生成诊断的异常消息。

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

## 能力边界

本 Starter 只通过 `SnowflakeIdGenerator` 的静态入口生成 Snowflake ID，不提供 UUIDv7、机器 ID 自动发现、Redis 租约、数据库号段、批量预取、持久化水位或网络协调。

## 验证

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl egon-cola-component-common/egon-cola-component-common-id-starter -am clean test
```
