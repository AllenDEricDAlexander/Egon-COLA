# Snowflake ID 与 Common ID Starter 改造设计

## 1. 背景与目标

原 ID 实现只提供 `IdGenerator`、`UuidV7` 和 `UuidV7Generator`，并通过
`com.github.f4b6a3:uuid-creator` 生成 UUIDv7。本次改造将默认数据库主键方案切换为
纯 JDK 的 64 位 Snowflake 变体，同时保持现有 UUIDv7 公共 API 的兼容周期，并将全部
ID 能力收敛到 common 下唯一的 Starter 模块。

目标如下：

- common 下只保留 `egon-cola-component-common-id-starter` 一个 ID 模块，不保留独立
  `common-id` 或 test 模块。
- 核心接口、算法、解析器和异常保持纯 JDK 实现，不导入 Spring API，也不引入第三方
  ID 算法依赖；Spring Boot 自动配置与它们发布在同一个 Starter Artifact 中。
- 新增 `LongIdGenerator` 和默认 `SnowflakeIdGenerator`，面向数据库 `BIGINT`
  主键。
- 保持 `IdGenerator.nextId()` 的字符串兼容契约。
- 通过 CAS 状态机保证单实例线程安全、不重复和严格递增。
- 显式处理序列耗尽、时钟回拨、线程中断和时间戳位耗尽。
- 保留 `UuidV7`、`UuidV7Generator` 的原方法与结果格式，改为纯 JDK 实现并进入
  废弃周期。
- 提供确定性单元测试、Starter 自动配置测试、依赖边界检查和双语文档。

## 2. 非目标

本次不实现：

- Redis 机器号租约、数据库号段、网络协调或持久化水位。
- 自动推导 machine ID，包括 IP、MAC、hostname、端口、进程号、随机值和哈希。
- 可配置 Epoch、位数分配、算法结构或批量预取。
- 跨节点真实业务发生顺序的全局严格递增保证。
- 将现有 UUID 字符串协议、trace、nonce、lease、event 或 UUIDv7 分片键机械替换为
  long。
- 修改任何已经存在的 Flyway migration。

## 3. 模块边界

最终结构：

```text
egon-cola-components/
└── egon-cola-component-common/
    └── egon-cola-component-common-id-starter/
        ├── 纯 JDK 接口、算法、解析器、异常
        ├── Spring Boot 3 自动配置与配置属性
        ├── AutoConfiguration.imports
        ├── src/test 中的算法、兼容与 ApplicationContextRunner 单元测试
        ├── README.md
        └── README.zh-CN.md
```

`egon-cola-component-common-id-starter` 继承 `egon-cola-component-common`，由 common
聚合 POM 管理。BOM 只管理该 Starter；Spring 和非 Spring 消费者都使用同一 Artifact，
非 Spring 场景直接实例化核心类。

发布继续继承 components parent 的 source、Javadoc、GPG 和 Central Publishing
配置，不复制发布插件，也不修改发布工作流目标列表。

## 4. 公共 API

### 4.1 字符串兼容接口

```java
public interface IdGenerator {
    String nextId();
}
```

签名保持不变，避免破坏现有 Bean、lambda、源码和二进制调用方。

### 4.2 Long ID 接口

```java
public interface LongIdGenerator extends IdGenerator {
    long nextLongId();

    @Override
    default String nextId() {
        return Long.toString(nextLongId());
    }
}
```

`LongIdGenerator` 继续属于现有 Strategy 扩展点；默认方法是 long 到字符串契约的轻量
Adapter。无需额外 Factory、Builder、Template Method 或 State 模式。

### 4.3 时间源

```java
@FunctionalInterface
public interface TimeSource {
    long currentTimeMillis();
}
```

生产构造器默认使用 `System::currentTimeMillis`。可注入构造器用于确定性测试和非
Spring 场景，但不得通过时间源改变固定 Epoch 或位布局。

### 4.4 默认实现

`SnowflakeIdGenerator` 是有状态、线程安全实例，实现 `LongIdGenerator`。公开构造器
支持：

- `SnowflakeIdGenerator(long machineId)`：使用系统时间和 `5ms` 回拨阈值。
- `SnowflakeIdGenerator(long machineId, Duration maxClockBackward)`：非 Spring
  自定义回拨阈值。
- `SnowflakeIdGenerator(long machineId, Duration maxClockBackward,
  TimeSource timeSource)`：确定性测试或受控时间源。

所有构造器验证 machine ID 和非负回拨阈值。Epoch 与位数没有构造参数。

## 5. ID 位布局与容量

固定 Epoch：`2026-01-01T00:00:00Z`。

```text
0 | 41-bit elapsed milliseconds | 10-bit machine ID | 12-bit sequence
63                                                  0
```

| 字段 | 位数 | 范围 |
|---|---:|---:|
| 符号位 | 1 | 固定为 0 |
| Epoch 后毫秒差 | 41 | `0 .. 2^41 - 1` |
| machine ID | 10 | `0 .. 1023` |
| 毫秒内序列 | 12 | `0 .. 4095` |

组合公式：

```text
id = (elapsedMillis << 22) | (machineId << 12) | sequence
```

结果不使用符号位，因此不为负。生产当前时间已经晚于 Epoch；为使公共契约在 Epoch
精确起点也满足“正数”，当组合结果为 `0` 时跳过序列 `0`，从 `1` 开始。解析器仍允许
解析非负 ID `0`，因为其位布局本身合法。

容量边界：最多 1024 个正确分配的节点，单节点单毫秒最多 4096 个 ID，41 位时间范围
约 69.7 年。最后一个可表示毫秒仍可生成 ID；超过后立即失败。

## 6. CAS 并发状态机

生成器内部只有一个关键共享状态：

```text
state = (elapsedMillis << 12) | sequence
```

使用 `AtomicLong` 保存，初值为专用未初始化哨兵。machine ID 是实例不可变字段，不放入
状态。每次生成过程：

1. 读取时间源。
2. 校验时间没有早于 Epoch且没有超过 41 位范围。
3. 读取原子状态并解码最后毫秒与序列。
4. 当前时间晚于最后毫秒：候选序列为 0。
5. 当前时间等于最后毫秒：候选序列加 1。
6. 序列已经为 4095：等待时间进入下一毫秒后重试。
7. 当前时间早于最后毫秒：进入时钟回拨策略。
8. CAS 更新状态；成功的 CAS 是生成操作的线性化点。
9. 使用成功写入的时间戳、machine ID 和序列组合最终 ID。

CAS 失败表示另一个线程先完成生成，当前线程重新读取状态和时间。时间判断与序列分配
都以同一个 CAS 状态为依据，不使用 `synchronized`、全局静态锁或分离的时间戳/序列
原子变量。

因此同一实例内按成功 CAS 次序严格递增且不重复。不同实例在 machine ID 唯一且系统
时间正常的前提下全局唯一并按毫秒趋势有序；同一毫秒内不同 machine ID 的位排序不代表
真实业务发生顺序。

## 7. 等待、回拨与中断

### 7.1 小幅回拨

当 `currentTime < lastTime` 且差值不超过 `maxClockBackward`：

- 使用 `System.nanoTime()` 建立单调、有界的等待截止时间。
- 周期性重新读取 `TimeSource`。
- 使用短时 `LockSupport.parkNanos`，避免无限忙自旋。
- 时间追平后重新进入 CAS 循环，绝不在回拨期间继续使用逻辑时间生成。
- 如果等待超过边界仍未恢复，按回拨失败处理。

### 7.2 大幅回拨

差值超过阈值时不等待，立即抛出 `ClockMovedBackwardException`。异常公开以下字段：

- 当前时间毫秒值。
- 最后使用时间毫秒值。
- 回拨毫秒数。
- machine ID。

异常信息包含同样内容。不能无条件使用内存逻辑时间，因为进程重启会丢失水位并可能与
旧 ID 冲突。

### 7.3 序列耗尽

同一毫秒已经使用序列 4095 时，生成线程短暂 park，并等待 `TimeSource` 进入下一毫秒，
然后重新竞争 CAS。等待响应线程中断，不用粗粒度锁阻塞其他线程。

### 7.4 中断

任一等待路径发现线程中断时：

- 保留或恢复线程中断标记。
- 抛出专用非受检 `IdGenerationInterruptedException`。
- 不更新原子状态，不产生 ID。

## 8. 时间戳溢出和解析

当前时间早于 Epoch 或 elapsed milliseconds 超过 `2^41 - 1` 时抛出
`SnowflakeTimestampOutOfRangeException`，不进行截断或位回绕。

`SnowflakeIdParser` 提供 `parse(long id)`，返回不可变 `SnowflakeId` record：

- `long id`
- `Instant generatedAt`
- `long elapsedMillis`
- `int machineId`
- `int sequence`

解析器拒绝负数，使用与生成器共享的 package-level 位布局常量，避免复制魔法数字。

## 9. UUIDv7 兼容策略

仓库仍存在以下必须保持 UUID/String 语义的调用：

- DDC `changeId` 明确解析并校验 UUID version 7。
- DDC nonce、lease、trace 和管理协议需要字符串或高熵标识。
- Gateway/RPC 的 request、trace、event、release 和跨进程 ID 是字符串契约。
- Light/Web/Service archetype 使用 `VARCHAR(36)`、UUIDv7 分片算法和版本校验。

因此：

- 保留 `UuidV7.generate()`、`string()`、`simpleString()` 的签名、36/32 位格式和
  version 7 语义。
- 保留 `UuidV7Generator implements IdGenerator`。
- 两个类型均标记 `@Deprecated(forRemoval = true)`，Javadoc 指向
  `SnowflakeIdGenerator` / `LongIdGenerator`，并说明删除安排属于下一大版本。
- 使用 JDK `UUID`、`SecureRandom` 和 `System.currentTimeMillis()` 组装 RFC 9562
  UUIDv7 的 48 位 Unix 毫秒时间戳、version 7、variant 2 和随机位。
- 默认 Starter 只创建 Snowflake，不创建 UUIDv7 Bean。
- 删除 `uuid-creator` 依赖，并通过 clean build 与 dependency tree 防止旧 class 或传递
  依赖残留。

不把 Snowflake 数字伪装进 UUID 外壳，也不改变现有 UUID 调用的返回格式。

## 10. Starter 自动配置

配置前缀：`egon.cola.component.id`。

```yaml
egon:
  cola:
    component:
      id:
        enabled: true
        machine-id: 17
        max-clock-backward: 5ms
```

属性：

| 属性 | 类型 | 默认值 | 规则 |
|---|---|---|---|
| `enabled` | `boolean` | `true` | `false` 时完全不创建生成器 |
| `machine-id` | `Long` | 无 | 启用时必须显式设置，范围 0..1023 |
| `max-clock-backward` | `Duration` | `5ms` | 必须非负 |

自动配置使用：

- `@AutoConfiguration`
- `@EnableConfigurationProperties(IdGeneratorProperties.class)`
- `@ConditionalOnProperty(prefix = "egon.cola.component.id", name = "enabled",
  havingValue = "true", matchIfMissing = true)`
- `@ConditionalOnMissingBean(IdGenerator.class)`
- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

配置校验使用仓库既有的显式 validator 风格，不为此新增 validation starter。缺失
machine ID、越界、负回拨阈值都在创建生成器前 fail-fast，并在异常中指出完整属性名和
合法范围。

默认 Bean 的具体类型为 `SnowflakeIdGenerator`，同一个实例可按
`SnowflakeIdGenerator`、`LongIdGenerator` 和 `IdGenerator` 注入。业务应用只要提供
任意自定义 `IdGenerator`（包括 `LongIdGenerator` 子类型），默认 Bean 即退让，避免
同一注入点歧义。

Starter 不使用 `@ComponentScan`，不推导 machine ID，不注册静态全局生成器。

## 11. 测试设计

### 11.1 核心算法与兼容 API

测试全部使用可控 `TimeSource`，不依赖 `Thread.sleep`：

- 单线程连续生成严格递增。
- 大批量生成无重复。
- 多线程高并发生成无重复，排序后严格递增。
- machine ID 0 和 1023。
- machine ID -1 和 1024 构造失败。
- 不同 machine ID 在同一毫秒不冲突。
- 同毫秒序列从 0 递增至 4095。
- 第 4097 个请求等待下一毫秒。
- 小幅回拨等待时间源恢复后继续生成。
- 大幅回拨立即抛出带完整字段的异常。
- 小幅回拨未在有界时间内恢复时失败。
- 等待线程被中断时恢复中断状态且不生成 ID。
- Epoch 起点仍生成正数。
- 41 位最后时间点可生成，下一毫秒失败。
- `SnowflakeIdParser` 正确解析时间、machine ID 和序列并拒绝负数。
- `LongIdGenerator.nextId()` 等于 `Long.toString(nextLongId())`。
- JDK UUIDv7 兼容 API仍产生 version 7、variant 2、36/32 位字符串。

并发压力测试使用确定性分段时间源：每个模拟毫秒允许一批生成请求后前进，既制造 CAS
竞争和同毫秒序列竞争，又避免真实时钟导致偶发失败。

### 11.2 Spring Boot 自动配置

使用 `ApplicationContextRunner`：

- 合法 machine ID 创建一个 Snowflake 实例，能按三个类型获取。
- `enabled=false` 不创建 `IdGenerator` 或 `LongIdGenerator`。
- 自定义 `IdGenerator` 时默认实现退让。
- 自定义 `LongIdGenerator` 时默认实现退让。
- 缺失 machine ID 时上下文启动失败，错误包含完整属性名。
- machine ID 越界时启动失败。
- 负 `max-clock-backward` 时启动失败。
- `Duration` 绑定和 `5ms` 默认值正确。
- `AutoConfiguration.imports` 能由 Boot 加载，无 `@ComponentScan`。

算法、兼容 API 和自动配置测试全部位于 Starter 自己的 `src/test`，不创建独立 test
Artifact。

## 12. 文档与迁移说明

更新范围：

- 唯一 Starter 的 POM 描述和中英文 README。
- common 中英文 README。
- components 聚合 POM、BOM POM和 BOM 中英文 README。
- 仓库中英文 README 中的组件入口清单。
- 任何当前架构清单中直接枚举可消费模块的位置。

Starter README 必须包含：

- Maven 引入方式。
- YAML 配置。
- `LongIdGenerator` / `IdGenerator` Bean 注入。
- 非 Spring 直接创建 `SnowflakeIdGenerator`。
- `BIGINT` 表结构和参数绑定示例。
- 位布局、Epoch、容量和严格递增语义。
- 时钟回拨与异常策略。
- machine ID 分配责任。
- Kubernetes StatefulSet ordinal 到 machine ID 的显式映射示例。
- 普通 Deployment 随机 Pod 名不等于稳定 machine ID。
- 重复 machine ID 必然可能冲突。
- 可靠 NTP 要求。
- 纯内存算法在严重回拨并重启后不能无条件保证绝对不重复。
- UUIDv7 兼容 API 的废弃和迁移说明。

文档不得承诺：

- 无中心协调条件下跨机器严格递增。
- 自动避免 machine ID 冲突。
- 严重回拨和进程重启后的绝对不重复。
- Maven 测试等同于真实多节点部署证明。

## 13. 构建与发布验证

分层执行：

1. Starter 内的核心算法与兼容 API 单测。
2. Starter 自动配置单测。
3. common 全组件测试。
4. components reactor 测试。
5. Starter dependency tree，确认不存在 `uuid-creator` 或已删除的 `common-id` Artifact。
6. `rg` 检查所有 POM/源码不再引用 `uuid-creator`。
7. BOM effective POM和依赖解析。
8. `-Prelease -Dgpg.skip=true -DskipTests verify` 检查 source/Javadoc/Central 发布形状。
9. 根 `clean integration-test` 检查共享 reactor 接缝。
10. `git diff --check`、工作树和提交范围检查。

不启动 Spring Boot 应用，不连接真实数据库、Redis、Kubernetes 或 NTP。最终报告明确
区分确定性单测/Maven 构建证明与真实多 JVM 部署验证。

## 14. 兼容性与风险

- `IdGenerator` ABI 保持；新默认 Starter 输出十进制 Snowflake 字符串。
- 直接使用 `UuidV7` / `UuidV7Generator` 的消费者继续得到 UUIDv7，但会收到废弃
  警告。
- `LongIdGenerator` 适用于 Java `long` / SQL `BIGINT`；通过 JSON 传给 JavaScript
  时应优先使用字符串，避免超过安全整数范围。
- machine ID 是部署责任。不同节点复用同一个值可在相同毫秒和序列上冲突。
- 单实例严格递增不等于跨节点业务顺序严格递增。
- 允许的小回拨只在进程存活期间有内存水位；大回拨直接失败是为了避免重启后冲突风险。
- UUIDv7 兼容实现替换了底层库，需保留 version、variant、字符串格式和时间字段测试。
