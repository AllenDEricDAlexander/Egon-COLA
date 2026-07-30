# Snowflake ID 完整测试与容量基准设计

## 1. 目标

在现有 `egon-cola-component-common-id-starter` 内补齐 Snowflake 生成器的确定性边界测试、
平台线程与 JDK 21 虚拟线程并发测试，以及独立 JMH 容量基准。测试只验证现有生产实现，
不修改 `SnowflakeIdGenerator` 的算法、公共 API 或位布局，也不启动应用服务。

本次需要给出两类证据：

- 正确性证据：固定毫秒内序列边界、等待与中断、时钟回拨、时间范围、machine ID 隔离和
  同 machine ID 独立实例的冲突风险。
- 性能观察：共享同一生成器时，平台线程和虚拟线程并发规模增加后的吞吐、平均延迟、
  CAS 竞争和 GC 分配情况。

## 2. 已有约束

- JDK 21。
- 位布局固定为 `1/41/10/12`，单个 machine ID 每毫秒有 `4096` 个序列值。
- 单实例成功 CAS 是线性化点；正确性测试不得把任务完成顺序误当成 CAS 顺序。
- 测试继续使用模块已有的 `spring-boot-starter-test`，不增加另一套测试框架。
- JMH 使用 `1.37`，仅在显式启用 `jmh` Maven Profile 时参与编译和打包。
- 普通 JUnit 测试不设置吞吐量、耗时或最大线程数等固定性能阈值。
- 所有可能等待时间推进、Future、线程结束或执行器退出的测试均有超时保护。

## 3. 文件边界

```text
egon-cola-component-common-id-starter/
├── pom.xml
├── src/test/java/top/egon/cola/component/common/id/snowflake/
│   ├── ControllableTimeSource.java
│   ├── SnowflakeIdGeneratorTest.java
│   └── SnowflakeIdGeneratorConcurrencyTest.java
├── src/jmh/java/top/egon/cola/component/common/id/benchmark/
│   └── SnowflakeIdGeneratorBenchmark.java
└── src/test/README.md
```

`src/test` 保存确定性功能测试、并发正确性测试和运行说明；`src/jmh` 是同一 Starter
模块内的独立基准源集，不新增 Maven 子模块，也不让普通 Surefire 执行 JMH。

## 4. 可控测试时间源

`ControllableTimeSource` 是 package-private 测试工具，实现生产 `TimeSource`：

- `AtomicLong` 保存当前毫秒，确保并发读取、设置和推进可见。
- `AtomicLong` 统计 `currentTimeMillis()` 的读取次数。
- `setCurrentTimeMillis(long)` 人工设置绝对时间。
- `advanceMillis(long)` 原子推进指定的非负毫秒数并返回新时间。
- `readCount()` 返回累计读取次数。

等待测试通过“读取次数已经增加且调用线程仍存活”证明生成器已进入时间等待路径，再推进
时间或发送中断。测试不依赖固定 sleep 推测线程状态。

## 5. 确定性与边界测试

### 5.1 同毫秒序列容量

在固定 `TEST_TIME` 下调用同一实例 4096 次，逐项验证：

- ID 全部为正数。
- ID 无重复并按调用顺序严格递增。
- 解析后的毫秒不变，machine ID 不变。
- 序列逐项等于字面量期望 `0..4095`。

第 4097 次调用放入虚拟线程。观察时间源继续被读取且线程尚未结束后，将时间推进 1ms；
调用应在超时内成功返回，时间戳进入下一毫秒且序列重置为 0。

### 5.2 等待中断

耗尽固定毫秒的 4096 个序列后，用虚拟线程执行第 4097 次调用。确认其进入等待后中断该
虚拟线程，验证抛出 `IdGenerationInterruptedException`，并验证异常捕获点仍保留中断标记。

### 5.3 时钟与 machine ID 边界

- 小回拨：先生成一个 ID，将时间回拨 3ms；虚拟线程等待后人工恢复到最后使用毫秒，
  调用成功且序列继续为 1。
- 大回拨：回拨量超过 5ms，立即抛出 `ClockMovedBackwardException`，并校验诊断字段。
- 时间范围：Epoch 前 1ms 和 41 位最大时间后 1ms 均抛出
  `SnowflakeTimestampOutOfRangeException`。
- 不同 machine ID：同一毫秒分别生成完整序列，两个集合无交集。
- 相同 machine ID：两个独立实例在相同时间从相同序列起步，明确验证会产生相同 ID，
  作为部署配置风险证据。

## 6. 并发正确性测试

平台线程测试使用固定线程池，多个线程共享一个 `SnowflakeIdGenerator`，总量显著超过
单毫秒 4096 容量并使用系统时钟自然推进。虚拟线程测试使用
`Executors.newVirtualThreadPerTaskExecutor()` 提交大量独立任务。两类测试均验证：

- 返回数量等于计划数量。
- 所有 ID 为正数。
- 并发集合大小等于返回数量，即没有重复。

并发测试不按 Future 完成顺序断言严格递增，因为完成顺序不是 CAS 线性化顺序；单线程
固定毫秒测试负责严格递增契约。

## 7. JMH 设计

### 7.1 共享生成器平台线程基准

`SnowflakeIdGeneratorBenchmark` 使用 `@State(Scope.Benchmark)`，所有 JMH worker 共享
同一生成器。提供 1、2、4、8、16、32 个平台线程的独立 benchmark 方法，每个方法直接
调用 `nextLongId()`：

- `@BenchmarkMode({Mode.Throughput, Mode.AverageTime})` 同时报告吞吐和平均延迟。
- 32 线程档位作为基础 CAS 高竞争观察点。
- `-prof gc` 报告 `gc.alloc.rate`、`gc.alloc.rate.norm` 和 GC 次数/时间。

### 7.2 虚拟线程扩展基准

虚拟线程端到端基准使用一个 trial 级
`Executors.newVirtualThreadPerTaskExecutor()`，每次 JMH invocation 提交一批任务并等待
全部完成。批次规模为：

```text
32, 128, 512, 2048, 8192, 32768, 65536
```

每个任务调用同一个生成器一次。通过 `@OperationsPerInvocation` 或等价的结果归一化，
报告 ID/秒，而不是“批次/秒”。最高档位只是本机实测上界，不声明 JVM 或操作系统的绝对
最大虚拟线程数；若某档位在限定 benchmark 时间内无法完成，报告该档位及资源表现，不把
它变成 JUnit 失败阈值。

### 7.3 4096 容量判断

12 位序列的算法上限是单 machine ID 每毫秒 4096 个，即理想上限约
`4,096,000 IDs/s`。实测重点观察：

- 吞吐是否接近或达到该上限。
- 并发增加后平均延迟何时明显上升。
- 32 线程以上 CAS 竞争是否使吞吐停止增长。
- 虚拟线程批次扩大时调度和 Future 分配带来的 GC 成本。

基准结果只能代表运行时机器与 JVM 参数。若真实业务持续需求超过单 machine ID 容量，
优先通过正确分配不同 machine ID 水平扩展；本次不改变 12 位序列布局。

## 8. Maven 与运行方式

默认 `mvn test` 只执行 JUnit/Spring Test。`jmh` Profile 增加 `src/jmh/java`、JMH 注解处理
器和带 `org.openjdk.jmh.Main` 的 `benchmarks` classifier 可执行 jar。

验证顺序：

1. 运行 Snowflake 定向测试。
2. 运行 Starter 全量测试。
3. 启用 `jmh` Profile 打包 benchmark jar。
4. 使用缩短 warmup/measurement 的参数做 JMH 冒烟。
5. 使用正式参数和 `-prof gc` 采集平台线程、虚拟线程扩展与容量数据。

## 9. 设计模式取舍

本次不修改非平凡业务逻辑，不引入 Strategy、Factory、State 或其他生产设计模式。
`TimeSource` 已经是生产实现预留的时间抽象；测试侧只增加线程安全 Fake Clock。
进一步引入时钟工厂、等待策略或 benchmark facade 只会扩大测试改动范围，直接测试设施更
符合现有项目风格。

## 10. 完成条件

- 用户要求的确定性、并发、等待、中断、回拨、范围和 machine ID 测试全部可重复通过。
- 所有可能阻塞的 JUnit 测试都有超时边界。
- 默认构建不执行 JMH，`-Pjmh package` 能生成可运行 benchmark jar。
- JMH 冒烟通过，并完成当前机器上的虚拟线程扩展实测。
- 测试不包含固定性能阈值断言，生产源码不发生变化。
