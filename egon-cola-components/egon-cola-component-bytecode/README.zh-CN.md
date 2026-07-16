# Egon COLA 字节码组件

[English](README.md) | 中文

字节码组件无需加载或初始化应用类，即可依据标准 Egon COLA 架构规则检查已编译的类。其公共 API 仅依赖 JDK；ASM、Maven 和 JSON 序列化均属于实现细节。

## 运行时 Agent 安装

运行时增强由两个需要独立安装的部分组成。请将 Spring starter 添加到应用，并在应用主类之前将单独发布的 shaded Agent JAR 传给 JVM：

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-bytecode-starter</artifactId>
    <version>${egon-cola.version}</version>
</dependency>
```

```bash
java -Xverify:all \
  "-javaagent:/opt/egon/egon-cola-component-bytecode-agent-5.2.3.jar=enabled=true,features=executor;observation;method-extension;access-guard,include=com.example.*,observation-include=com.example.*" \
  -jar application.jar
```

发布的 Agent JAR 是主产物 `egon-cola-component-bytecode-agent-${version}.jar`；其中已包含重定位后的 ASM 和 SnakeYAML 类。不要在命令行中使用未 shaded 的 Agent classifier。Agent 仅支持 `premain`。

Agent 默认禁用；启用 Agent 时必须至少显式配置一个 `include` 模式。支持的键包括 `enabled`、`features`、`include`、`exclude`、`observation-include`、`observation-method`、`observation-exclude`、`observe-constructors`、`observation-slow-threshold-millis`、`failure-policy`、`failure-capacity` 和 `config`。配置优先级从低到高依次为默认值、环境变量、JVM 系统属性、YAML 和 `-javaagent` 参数。`config` 路径本身依次从环境变量、系统属性和 Agent 参数中选择。

```yaml
enabled: true
features:
  - executor
  - observation
  - access-guard
include:
  - com.example.*
exclude:
  - com.example.generated.*
observation-include:
  - com.example.application.*
observation-method:
  - handle*
observation-exclude:
  - com.example.application.internal.*
observe-constructors: false
observation-slow-threshold-millis: 500
failure-policy: skip-class
failure-capacity: 32
```

环境变量键使用 `EGON_COLA_BYTECODE_` 前缀，例如 `EGON_COLA_BYTECODE_INCLUDE`；系统属性使用 `egon.cola.bytecode.` 前缀，例如 `-Degon.cola.bytecode.config=/opt/egon/bytecode.yaml`。列表值接受逗号或分号分隔。故障策略包括 `skip-class`、`disable-feature` 和 `mark-fatal`。

包含模式永远不能覆盖针对 bootstrap 类以及 `java`、`javax`、`jakarta`、`jdk`、`sun`、`com.sun`、ASM、Spring、logging、Micrometer 和 `top.egon.cola.component.bytecode` 包的不可变排除规则。只有 class 文件版本 65 至 69（Java 21 至 Java 25）符合条件。

生成的 JDK 代理类（`$ProxyN`）以及 Spring CGLIB/FastClass 类也会被排除。Agent 会增强具体目标类，因此代理调用只评估一次策略，不会在代理和目标对象上各评估一次。

## Executor 增强语义

Agent 只会重写已包含应用类中的以下接口调用点：

- `Executor.execute(Runnable)`
- `ExecutorService.submit(Runnable)`
- `ExecutorService.submit(Runnable, Object)`
- `ExecutorService.submit(Callable)`

对调度器 API、具体 executor owner 方法、JDK 类和其他重载的调用保持不变。每个重写点都有一个稳定 ID，该 ID 由其 owner、所在方法、目标签名和指令位置派生。ID 冲突会导致明确的注册失败，而不会产生含义不清的指标。

底层 executor API 恰好调用一次。`submit` 返回该 executor 创建的原始 `Future`，而业务异常、`RejectedExecutionException`、中断和取消行为均保留原有身份与时序。包装器在任务执行期间恢复捕获的上下文，并在 `finally` 中清理工作线程状态；如果 carrier 已经执行了捕获，取消操作无法阻止这次捕获工作。

存在 SLF4J 时会启用 MDC 传播。其他 carrier 实现仅依赖 JDK 的 `ContextCarrier` API。Egon 已包装的任务以及类型精确为 `DtpRunnable` 和 `DtpCallable` 的 DTP 包装器不会被再次包装；既不会修改动态线程池注册表，也不会使用它进行发现。

Spring 运行时设置使用 `egon.cola.component.bytecode` 前缀：

```yaml
egon:
  cola:
    component:
      bytecode:
        enabled: true
        executor:
          enabled: true
          propagate-mdc: true
          metrics: true
          sampling-rate: 1.0
          names:
            applicationTaskExecutor: application
        runtime:
          failure-capacity: 32
        endpoint:
          enabled: true
```

Agent 的包含范围在 Spring 启动前确定。运行时的 `executor.include` 和 `executor.exclude` 值是预留的请求设置，不能扩大 Agent 的有效转换范围。

## 方法观测语义

方法观测按以下顺序匹配：不可变/硬编码的包排除规则和显式 `observation-exclude` 模式优先；随后 `@EgonObserved` 可启用其他方面符合条件的方法；最后由 `observation-include` 加 `observation-method` 启用配置的方法。该注解最多接受八个静态 `key=value` 标签，并可配置慢调用阈值。静态标签会经过校验并受到边界限制；动态 `${...}` 和 `#{...}` 值会被拒绝。

支持的目标包括任意可见性的具体实例方法、static 方法、final 方法、synchronized 方法、同类调用、递归调用和非 Spring 对象。abstract、native、synthetic、bridge、生成的 lambda-body 方法和 `<clinit>` 会被排除。只有带注解或配置 `observe-constructors=true` 时才观测构造器；计时从第一次成功的 `this(...)` 或直接 `super(...)` 调用之后开始。该初始化调用的失败不会归因于子构造器；在成功的 `this(...)` 调用链中，每个被观测的构造器会分别记录自身剩余的方法体。

观测包装器保留原始返回值，并重新抛出完全相同的原始 `Throwable`。它记录方法身份元数据、推断出的层、耗时、结果、异常类分组、虚拟线程状态、事件 Sink 可用时的 trace ID，以及校验后的静态标签。它绝不捕获参数、返回载荷、异常消息、任意对象文本、凭据、cookie、authorization header 或其他请求载荷。计时只覆盖同步方法执行：返回 `Future`、`CompletionStage`、响应式 publisher 或其他异步值时，观测在返回时结束，不会跟踪后续完成状态。

Spring 运行时控制与转换时匹配相互独立：

```yaml
egon:
  cola:
    component:
      bytecode:
        observation:
          enabled: true
          sampling-rate: 1.0
          slow-threshold-millis: 500
          metrics-enabled: true
```

采样和运行时禁用无需重新转换即可成为空操作。运行时 Sink 故障会隔离到有界诊断信息中，深度保护会抑制由事件发布递归触发的观测。Agent 还会硬排除 bridge、runtime、logging 和 metrics 包。

## Method Extension Agent 语义

Method Extension Agent 模式复用现有 Method Extension 注解、Handler、响应转换、事件和异常。由于 Method Extension 集成是可选的，且 bytecode starter 不会传递导出该集成，应用必须显式添加两个 starter：

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-method-extension-starter</artifactId>
    <version>${egon-cola.version}</version>
</dependency>
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-bytecode-starter</artifactId>
    <version>${egon-cola.version}</version>
</dependency>
```

在 Spring 中选择 Agent 模式，并在 JVM 启动时启用对应的 Agent 功能：

```yaml
egon:
  cola:
    component:
      method-extension:
        engine: AGENT
        not-ready-policy: PROCEED
```

```bash
java "-javaagent:/opt/egon/egon-cola-component-bytecode-agent-5.2.3.jar=enabled=true,features=method-extension,include=com.example.*" \
  -jar application.jar
```

Agent 模式支持任意可见性的具体实例方法，包括 private 同类调用、final 方法、synchronized 方法、通过接口声明注解的方法和非 Spring 对象。static 方法、构造器、abstract、native、synthetic 和 bridge 方法会被排除。`AOP` 和 `AGENT` 是互斥引擎；Agent 模式不会注册 Method Extension AOP advisor。

Handler 在方法观测之前运行。因此，被拒绝的调用会产生 Method Extension 事件，但不会进入被观测的业务方法。允许执行的同步调用保留返回值身份。拒绝处理支持直接值和 `returnJson`，并使用现有响应规则适配 `Future`、`CompletionStage` 和 `CompletableFuture` 载荷。任意具体 `Future` 实现和响应式类型明确不受支持。

Spring 只会在单例初始化完成后将运行时标记为就绪。在此之前，`not-ready-policy` 控制 `PROCEED`、`REJECT` 或 `FAIL`；默认值是 `PROCEED`。方法元数据按应用 `ClassLoader` 缓存，且不会全局持有应用 ClassLoader。事件包含有界的方法和 Handler 标识及结果，但绝不包含参数、返回载荷、凭据或异常消息。

## Access Guard Agent 语义

Access Guard Agent 模式复用现有注解、规则解析器、白名单/黑名单、限流器、故障策略、拒绝处理和事件。请添加两个可选 starter，选择互斥的 `AGENT` 引擎，并启用对应的 Agent 功能：

```yaml
egon:
  cola:
    component:
      access-guard:
        engine: AGENT
```

```bash
java "-javaagent:/opt/egon/egon-cola-component-bytecode-agent-5.2.3.jar=enabled=true,features=access-guard,include=com.example.*" \
  -jar application.jar
```

支持的方法目标包括 public/private 实例方法和 static 方法，也包括同类、递归、final、synchronized、代理和非 Spring 调用。protected、package-private、abstract、native、synthetic、bridge 方法和类初始化器会明确失败。static 受保护方法只能使用 static fallback。synchronized 方法保留原有 monitor 边界；由于将方法体移到另一个线程会破坏该边界，因此会拒绝超时执行。

public/private 构造器仅支持聚合注解 `@AccessGuard`。Guard 在第一次 `this(...)` 或直接 `super(...)` 调用之前运行，因此此时不存在已初始化的 receiver。构造器规则支持从参数、Header、IP 或 `all` 中解析 key，也支持白名单/黑名单、限流、失败策略和事件。它们拒绝 timeout、fallback、`returnJson`、`LOCAL_FALLBACK`、返回值替换和实例状态访问。成功的 `this(...)` 调用链中，每个带注解的构造器都会评估一次。构造器使用的自定义 key 解析器还必须实现 `ExecutableAccessKeyResolver`。

当多个功能重叠时，稳定的调用顺序依次是 Method Extension、Access Guard、方法观测，最后是业务方法体。Method Extension 拒绝或 Access Guard 拒绝永远不会进入方法观测。Agent 的 `failure-policy=mark-fatal` 会记录 `FAILED` 状态并阻止 Spring 上下文完成，但不会终止 JVM 或结束类加载；`skip-class` 和运行时 `FAIL_OPEN` 保留其文档约定的 fail-open 边界。请避免对 Agent 运行时就绪之前创建的框架/bootstrap 基础设施使用构造器 Guard。

Access Guard 诊断遵循与其他运行时功能相同的隐私契约：不会输出参数、返回载荷、凭据、cookie、authorization header、异常消息、原始 include 模式或对象文本。

## 指标、状态与隐私

存在 `MeterRegistry` 时，starter 只会发出以下有界指标：

- `egon.cola.bytecode.executor.tasks.submitted`
- `egon.cola.bytecode.executor.tasks.started`
- `egon.cola.bytecode.executor.tasks.finished`
- `egon.cola.bytecode.executor.queue.wait`
- `egon.cola.bytecode.executor.execution`
- `egon.bytecode.method.duration`
- `egon.bytecode.method.errors`
- `egon.bytecode.method.slow`

Executor 标签为 `executor`、`executor_type`、`result`、`exception_group` 和 `virtual_thread`。观测标签包括有界的 class、method、layer、virtual-thread、适用时的 exception-group，以及校验后的静态注解标签。Trace ID、request ID、线程名、原始 descriptor、参数和返回值绝不会成为指标标签。未知或由身份派生的 executor 名称会归并为 `unmanaged`，值会经过清理并限制长度，每个 `sampling-rate` 必须介于 `0.0` 和 `1.0` 之间。

Actuator 是可选的，starter 不会传递引入它。如果应用已安装 Actuator 并公开了该端点，`GET /actuator/egonbytecode` 会报告 Agent/运行时版本、协议、状态、请求/生效功能、有界计数与近期故障、dispatcher 注册、元数据计数和聚合观测计数。它绝不会报告原始 include/exclude 模式、Agent 参数、方法 descriptor、类 owner、参数、返回值、异常消息、任务、`Future` 对象、request ID、trace ID 或捕获的上下文。Agent 启动输出同样只打印模式数量和 SHA-256 摘要。

状态包括 `DISABLED`、`STARTING`、`ACTIVE`、`DEGRADED` 和 `FAILED`；starter 会将缺少 Agent 报告为 `AGENT_UNAVAILABLE`。运行中的 Agent 如果协议主版本不同，会导致 Spring 启动失败。Agent 不支持 Attach、`agentmain`、重定义、重新转换、bootstrap/JDK 转换或转换后类转储。

## Maven 插件

始终显式声明插件版本。在单模块项目中绑定 `check`，或在多模块 reactor 的终端模块中绑定 `check-reactor`：

```xml
<plugin>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-bytecode-architecture-maven-plugin</artifactId>
    <version>${egon-cola.version}</version>
    <configuration>
        <packageMappings>
            <com.example.domain..>DOMAIN</com.example.domain..>
            <com.example.application..>APPLICATION</com.example.application..>
            <com.example.infrastructure..>INFRASTRUCTURE</com.example.infrastructure..>
            <com.example.adapter..>ADAPTER</com.example.adapter..>
            <com.example.facade..>FACADE</com.example.facade..>
            <com.example.starter..>STARTER</com.example.starter..>
            <com.example.common..>COMMON</com.example.common..>
        </packageMappings>
    </configuration>
    <executions>
        <execution>
            <id>cola-architecture-check</id>
            <phase>verify</phase>
            <goals>
                <goal>check-reactor</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

可用目标包括：

- `bytecode-architecture:check`：扫描当前模块。
- `bytecode-architecture:check-reactor`：扫描当前 Maven reactor 中已编译的类。
- `bytecode-architecture:generate-baseline`：将当前发现项的指纹显式写入 baseline。

常规检查只会写入 `target` 目录，不会向 baseline 添加发现项。

## 配置

Maven 用户属性会覆盖匹配的 XML 配置，XML 会覆盖插件默认值；提供规则专用列表时，该列表会替换对应默认值。随后，层解析使用以下独立优先级：

```text
explicit module mapping > package mapping > module-name suffix > UNKNOWN
```

支持的层包括 `DOMAIN`、`APPLICATION`、`INFRASTRUCTURE`、`ADAPTER`、`FACADE`、`STARTER`、`COMMON` 和 `UNKNOWN`。重要选项如下：

| 选项 | 默认值 | 用途 |
| --- | --- | --- |
| `moduleMappings` | 空 | 将精确的 Maven artifact ID 映射到某一层。 |
| `packageMappings` | 空 | 将 `com.example.domain..` 等包模式映射到某一层。 |
| `scanTests` / `egonArchitecture.scanTests` | `false` | 包含 `target/test-classes`。 |
| `scanDependencies` / `egonArchitecture.scanDependencies` | `false` | 包含依赖 JAR。 |
| `additionalClassDirectories` | 空 | 包含其他已编译类目录。 |
| `frameworkDenylist` | 内置 Spring/Jakarta 技术前缀 | 替换 Domain 技术框架拒绝列表。 |
| `frameworkAllowlist` | 空 | 在应用拒绝列表前允许指定技术前缀。 |
| `facadeImplementationPackages` | `..adapter..` | 定义 Facade 实现允许所在的包。 |
| `failurePolicy` / `egonArchitecture.failurePolicy` | `FAIL` | 选择 `FAIL`、`WARN` 或 `REPORT_ONLY`。 |
| `unknownLayerPolicy` / `egonArchitecture.unknownLayerPolicy` | `WARN` | 选择 `FAIL`、`WARN` 或 `IGNORE`。 |
| `cacheEnabled` / `egonArchitecture.cache.enabled` | `true` | 启用已解析类元数据缓存。 |

## 标准规则

内置注册表恰好包含以下十条 Specification：

1. `ARCH-001`：Domain 不得依赖 Application、Infrastructure、Adapter、Facade 或 Starter。
2. `ARCH-002`：Domain 必须与配置的技术框架保持隔离。
3. `ARCH-003`：Application 不得依赖 Infrastructure 或 Adapter。
4. `ARCH-004`：Application 不得直接访问持久化框架或基础设施 mapper/repository 实现。
5. `ARCH-005`：Facade 必须保持为自包含的契约模块。
6. `ARCH-006`：Starter 不得包含或直接引用 Domain 或 Application 业务实现。
7. `ARCH-007`：Common 不得依赖业务模块。
8. `ARCH-008`：Adapter 不得直接调用 Infrastructure 实现。
9. `ARCH-009`：Domain 可以定义 repository 接口，但不得包含 JPA entity、mapper 实现、SQL session 或基础设施 repository 实现。
10. `ARCH-010`：Facade 实现必须位于配置的 Adapter 包中。

扫描器覆盖继承、字段、参数、返回类型与异常类型、泛型签名、注解及其值、局部类型、对象分配、数组、类型转换、`instanceof`、字段访问、方法与构造器调用、method handle、`invokedynamic`、Lambda 目标、`ConstantDynamic` 和常量池类引用。

## 报告与故障策略

每次检查都基于同一个不可变结果模型，写出确定性排序的 Text、JSON 和 HTML 报告，并向控制台打印相同的总数。默认目录为：

```text
${project.build.directory}/egon-cola-architecture
```

文件分别为 `architecture-report.txt`、`architecture-report.json` 和 `architecture-report.html`。

`FAIL` 会因新的错误发现项使构建失败，`WARN` 会记录这些发现项但不使构建失败，`REPORT_ONLY` 只生成报告。baseline 中已有的发现项不计为新发现项。`unknownLayerPolicy` 单独评估，因此未映射的类绝不会被静默接受。

## Baseline 工作流

默认 baseline 为 `${maven.multiModuleProjectDirectory}/.egon-cola/architecture-baseline.json`。

1. 审查当前发现项。
2. 运行 `./mvnw bytecode-architecture:generate-baseline` 创建 baseline。
3. 如果 baseline 代表已接受的技术债，请提交审查后的 baseline。
4. 持续运行 `check` 或 `check-reactor`；只有新发现项会受故障策略约束，已修复项会报告为 stale。
5. 只有明确要替换现有 baseline 时，才传入 `-DegonArchitecture.overwrite=true`。

稳定指纹包括规则 ID、源 class/member/descriptor、依赖种类，以及目标 class/member/descriptor。它不包含行号和展示文本。

## 内容哈希缓存

解析后的元数据缓存在 `target/egon-cola-architecture/cache` 下。缓存键包含 class SHA-256、解析器 schema 版本、ASM baseline 版本和有效扫描配置摘要。每次运行仍会重新构建完整图并评估所有规则。CI 使用以下命令禁用缓存：

```bash
./mvnw -DegonArchitecture.cache.enabled=false verify
```

## ArchUnit 迁移边界

light、web 和 service archetype 使用此插件替换其生成的 ArchUnit 测试。已批准的标准规则范围特意不保留五项定制检查：light 的 domain-first/reversed outbound-port 包命名、web 的 external evaluation-facade 隔离、service 禁止的 inbound 包段、service 项目级原生 gRPC 禁令，以及 service provider-facade 隔离。

## 兼容性与基准测试

生产产物使用 `--release 21` 编译。Maven Invoker 会在本地验证真实 Java 21 类，并在 JDK 25 上编译真实的 `--release 25` record fixture，再交由插件扫描。Fork 测试还会使用 `-Xverify:all` 和发布的 `-javaagent` JAR 启动真实 Java 21/25 进程，其中包括 Surefire 和 Failsafe fixture。

使用以下命令构建并列出 JMH benchmark：

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-benchmark -am -DskipTests package
java -jar egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-benchmark/target/egon-cola-component-bytecode-benchmark-benchmarks.jar -l
```

`ArchitectureScanBenchmark.scanOneThousandClasses` 会在测量前生成 1,000 个确定性的 class 字节数组，随后测量解析、图构建、全部十条规则和结果创建。受控目标为不超过两秒；共享 CI 会记录性能证据，但不会应用易受噪声影响的绝对阈值。

`ExecutorEnhancementBenchmark` 会分别记录未匹配过滤、1,000 次转换、直接提交、仅上下文提交，以及上下文加 Micrometer 提交。受控目标为 1,000 次转换不超过一秒，提交开销低于五微秒；共享 CI 会列出并记录这些 benchmark，但不强制执行对硬件敏感的绝对数值。

`MethodObservationBenchmark` 会在不捕获参数的情况下记录直接 baseline、禁用 bridge、启用成功、启用异常和慢事件路径。受控的启用成功目标低于两微秒；共享 CI 会记录结果，但不应用对硬件敏感的绝对阈值。
