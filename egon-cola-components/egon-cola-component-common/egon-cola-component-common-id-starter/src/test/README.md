# Snowflake ID 测试与 JMH 基准

## 正确性测试

在仓库根目录执行：

```bash
mvn -f egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/pom.xml clean test
```

定向执行生成器与并发测试：

```bash
mvn -f egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/pom.xml \
  -Dtest=SnowflakeIdGeneratorTest,SnowflakeIdGeneratorConcurrencyTest test
```

阻塞场景有 JUnit 超时、Future 超时或线程 join 超时保护。普通测试只验证正确性，不使用
吞吐量、平均延迟或最大线程数等固定性能阈值。

## 构建 JMH

JMH 位于同一 Starter 的 `src/jmh/java`，只在显式启用 `jmh` Profile 时编译：

```bash
mvn -f egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/pom.xml \
  -Pjmh -DskipTests package
```

生成的可执行文件：

```text
egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/
target/egon-cola-component-common-id-starter-5.3.2-benchmarks.jar
```

## 平台线程吞吐与平均延迟

正式执行 1、2、4、8、16、32 个平台线程，并采集 GC 分配数据：

```bash
java -jar egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/target/egon-cola-component-common-id-starter-5.3.2-benchmarks.jar \
  '.*(throughput|averageTime)Threads(1|2|4|8|16|32)$' -prof gc
```

- `throughputThreadsN` 的主结果单位是 `ops/s`。
- `averageTimeThreadsN` 的主结果单位是 `ns/op`。
- 32 线程结果用于观察共享 `AtomicLong` CAS 高竞争下的吞吐、延迟和 GC。
- `gc.alloc.rate.norm` 接近零表示直接生成路径基本没有每次调用分配；以实际报告为准。

## 虚拟线程整体吞吐

虚拟线程 benchmark 每个任务调用共享生成器一次，包含任务提交、虚拟线程调度、Future
完成和结果收集的端到端成本。每个固定批次方法使用 `@OperationsPerInvocation` 将原始结果
归一化为 ID/s 和 B/ID。常用批次为 `32, 128, 512, 2048, 8192, 32768, 65536`：

```bash
java -jar egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/target/egon-cola-component-common-id-starter-5.3.2-benchmarks.jar \
  '.*virtualThreadBatch(32|128|512|2048|8192|32768|65536)$' -prof gc
```

`virtualThreadBatchN` 的吞吐主结果直接是 `IDs/s`，`gc.alloc.rate.norm` 直接是 `B/ID`，
可与平台线程结果和单 machine ID 的 409.6 万 ID/s 算法上限直接比较。

继续向上探测 131,072、262,144、524,288 和 1,048,576 个虚拟线程任务：

```bash
java -jar egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/target/egon-cola-component-common-id-starter-5.3.2-benchmarks.jar \
  '.*virtualThreadBatch(131072|262144|524288|1048576)$' -prof gc
```

`Executors.newVirtualThreadPerTaskExecutor()` 没有配置固定最大线程池大小。成功运行的最高档位
仅表示当前机器、当前 JVM 参数和当前负载下已验证的规模，不是 JDK 的绝对上限。大批次会
同时保留大量 Future 和结果对象，应结合 `gc.alloc.rate.norm` 判断是否值得继续上探。

## 快速冒烟

冒烟只验证 benchmark 可启动和完成，不用于正式容量结论：

```bash
java -jar egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/target/egon-cola-component-common-id-starter-5.3.2-benchmarks.jar \
  '.*(throughputThreads(1|32)|averageTimeThreads(1|32))$' \
  -wi 0 -i 1 -r 200ms -f 1 -prof gc

java -jar egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/target/egon-cola-component-common-id-starter-5.3.2-benchmarks.jar \
  '.*virtualThreadBatch(32|512|2048|8192|32768|65536)$' \
  -wi 0 -i 1 -r 200ms -f 1 -prof gc
```

## 如何判断 4096 是否够用

12 位序列允许单个 machine ID 每毫秒生成 4096 个 ID，算法上限为：

```text
4096 IDs/ms = 4,096,000 IDs/s
```

如果实测吞吐接近该值后不再随线程数增长，并且平均延迟明显增加，说明调用已经频繁等待
下一毫秒。持续业务需求超过该容量时，应优先正确分配多个不同 machine ID 做水平扩展；
两个独立生成器使用相同 machine ID 会产生重复风险。
