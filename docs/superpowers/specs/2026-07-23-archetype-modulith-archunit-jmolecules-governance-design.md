# 2026-07-23 Egon-COLA Archetype 模块化架构治理改造 Spec

状态：草案，等待用户逐节审核。本文档未授权进入实现阶段。

## 1. 审核方式

建议按下面的顺序逐节确认。任何一节被否决时，只回改该节及其直接受影响章节，不提前写实现计划，也不修改 archetype 代码。

1. 第 2～5 节：目标、现状判断、方案选择和总体决策。
2. 第 6～8 节：总体结构、模块识别和依赖版本。
3. 第 9～11 节：light、web、service 三类 archetype 的目标结构。
4. 第 12 节：jMolecules 的领域语义标注规则。
5. 第 13 节：现有字节码插件、Spring Modulith、ArchUnit 的规则归属。
6. 第 14～16 节：模块 API、事件、测试和文档。
7. 第 17～21 节：变更范围、迁移步骤、验证、风险和验收标准。

## 2. 目标

结合以下四篇实践文章，把当前三个 Egon-COLA archetype 从“按约定组织的 COLA 分层工程”升级为“业务模块边界可识别、领域角色可表达、架构规则可执行、模块文档可生成”的工程模板：

- `spring-modulith-deep-practice-blog.md`
- `archunit-blog.md`
- `jmolecules-practice-blog.md`
- `springboot3-ecommerce-archunit-modulith-jmolecules-blog.md`

本次覆盖：

```text
egon-cola-archetype-light
egon-cola-archetype-web
egon-cola-archetype-service
```

目标不是简单加入三个依赖，而是建立职责清晰的四层治理：

```text
Spring Modulith
  -> 识别业务模块、验证模块依赖、保护模块内部实现、模块级测试、生成模块文档

jMolecules
  -> 在 Java 代码中表达聚合根、实体、值对象、领域事件、仓储和架构层语义

ArchUnit
  -> 执行 jMolecules 语义规则和 Egon-COLA/archetype 特有的架构规则

Egon 字节码架构 Maven Plugin
  -> 继续执行已经稳定的十条通用 COLA 分层规则
```

最终生成项目必须满足：

1. Spring Modulith 识别出的一级模块是业务模块，而不是 `adapter`、`application`、`domain`、`infrastructure` 技术层。
2. 业务模块只能访问其他模块明确暴露的 Named Interface。
3. 模块内部实现不能被其他模块直接依赖。
4. 领域模型具备可检查的 DDD 语义，而不是只靠类名和目录猜测。
5. 通用 COLA 分层规则、模块规则、领域语义规则之间没有重复权威和冲突。
6. `mvn verify` 同时完成业务测试、字节码架构检查、Modulith 结构检查、jMolecules/ArchUnit 语义检查和模块文档生成。
7. 不改变现有 REST、GraphQL、Dubbo、RabbitMQ、数据库表、配置 profile 和业务结果语义。

## 3. 当前仓库基线

### 3.1 Archetype 形态

当前三个 archetype 的物理构建结构不同：

| Archetype | 物理 Maven 结构 | 当前业务域 |
|---|---|---|
| light | 单 Maven 模块 | `user`、`teaching` |
| web | `common/domain/application/infrastructure/adapter/starter` 六模块 | `user`、`teaching` |
| service | `common/domain/application/infrastructure/adapter/starter` 六模块 | `course`、`exam` |

web 和 service 已使用稳定的 canonical Facade artifacts：

```text
top.egon:egon-cola-organization-facade
top.egon:egon-cola-evaluation-facade
```

这两个 Facade artifact 是跨项目协议边界，不属于本次生成项目内部的 Spring Modulith 模块。

### 3.2 当前 Java 包仍不能表达 Spring Modulith 业务模块

当前代码虽然已经在每个技术层下按业务域组织，例如：

```text
adapter/user
application/user
domain/user
infrastructure/user
```

但 Spring Modulith 默认从启动类根包下识别模块。按当前结构，它看到的仍然是：

```text
adapter
application
common
domain
infrastructure
starter/start
```

也就是说，当前包结构满足“层内业务域优先”，但不满足“业务模块成为应用模块根包”。如果只添加依赖和 `modules.verify()`，得到的是技术层模块图，不是业务模块图。

### 3.3 已存在的架构门禁

当前三个 archetype 已删除原 ArchUnit 测试，改用：

```text
top.egon:egon-cola-component-bytecode-architecture-maven-plugin
```

插件已经执行 `ARCH-001`～`ARCH-010` 十条通用规则，包括：

- Domain 依赖方向和技术框架隔离；
- Application 不依赖 Infrastructure/Adapter 和持久化实现；
- Facade 自包含；
- Starter/Common 的职责限制；
- Adapter 不直连 Infrastructure 实现；
- Domain 不包含 JPA/Mapper/SQL Session；
- Facade 实现位于 Adapter。

因此，本次不能把博客中的所有 ArchUnit 示例原样再写一遍。重复规则会造成两套失败信息、两套配置和两套维护入口。

### 3.4 当前缺口

现有字节码插件不负责以下内容：

1. 业务模块识别、`allowedDependencies`、Named Interface 和模块循环依赖。
2. 模块级 Spring 集成测试和模块文档生成。
3. 聚合根、实体、值对象、领域事件、领域仓储等 DDD 角色表达。
4. 聚合间对象引用、`@Identity` 完整性等 jMolecules 语义规则。
5. Controller 直接访问 Domain Repository 这类项目特有规则。
6. light 的业务域优先路径、web 的 canonical Facade 隔离、service 的纯 Service 边界等 archetype 特有规则。
7. Java 模块 API 泄漏内部 Aggregate、Entity、PO、Mapper 的检查。

### 3.5 已确认的真实跨业务域依赖

当前 web 存在：

```text
teaching -> user
```

具体包括 teaching 直接访问 `User`、`UserStatus`、`UserRepository`、`UserId`。其中 Repository、Entity、内部状态枚举不应成为跨模块接口。

当前 service 存在：

```text
exam -> course
```

具体包括 exam 直接访问 `Course`、`CourseRepository`、`CourseId`。其中 Repository 和 Course Entity 不应被 exam 直接使用。

当前 light 的 `user` 和 `teaching` 没有必须保留的直接跨域依赖。

## 4. 方案比较

### 4.1 方案 A：保留当前包结构，只增加三个工具

做法：

- 保留 `adapter/user`、`application/user`、`domain/user`；
- 添加 Spring Modulith、jMolecules、ArchUnit；
- 用自定义检测或普通架构规则补救。

问题：

- 一个完整业务域分散在四个互不从属的包树中；
- Spring Modulith 无法自然把这些不连续包组成一个 application module；
- `internal` 保护和 Named Interface 都无法覆盖完整业务域；
- 模块文档仍以技术层为中心；
- 后续每增加业务域都要维护复杂的自定义检测逻辑。

结论：不采用。改动表面小，但不能完成用户要求的模块化治理。

### 4.2 方案 B：保留 Maven 物理分层，Java 包改为业务模块优先

做法：

- light 保持单 Maven 模块；
- web/service 保持现有六个 Maven 分层模块；
- Java 包统一改为：

```text
<business-module>/internal/<layer>/<responsibility>
```

例如：

```text
user/internal/adapter/controller
user/internal/application/manage
user/internal/domain/aggregates
user/internal/infrastructure/repo
```

同一个逻辑业务模块的不同 layer 仍由现有 Maven module 编译，但在 Java 包空间中统一位于 `user` 或 `teaching` 下。

优点：

- Spring Modulith 能识别真实业务模块；
- 保留现有 Maven artifact、构建顺序和 COLA 物理分层；
- 不重写 web/service 的发布结构；
- `internal`、Named Interface、模块测试和文档都能直接使用；
- 未来若要改成垂直 Maven 模块，Java 包已经完成准备。

代价：

- 三个 archetype 都需要一次较大的路径、package、import、扫描配置和文档迁移；
- web/service 的一个逻辑业务模块会跨多个 Maven artifact，需要在 Starter 测试 classpath 上做整体验证；
- Spring Modulith 元数据需放在 Starter/Start 侧，避免 Domain 编译依赖 Spring。

结论：采用。

### 4.3 方案 C：Maven 模块也改成业务垂直切片

做法示例：

```text
organization-user
organization-teaching
organization-shared
organization-starter
```

优点：

- Java 模块边界与 Maven 编译边界完全一致；
- 编译期隔离最强。

问题：

- 重写当前六模块契约、依赖图、构建顺序、Docker layer、发布说明和大量测试；
- 失去现有统一的 COLA layer artifacts；
- 与本次“结合四篇文章补齐架构治理”的目标相比，改动明显过大。

结论：本次不采用。可作为以后独立架构版本讨论。

## 5. 本 Spec 的总体决策

1. 三个 archetype 一起改造，不能只改 light 或只改 web/service。
2. 采用方案 B：Maven 结构不变，Java 包按业务模块优先重排。
3. 业务模块统一使用 closed module，不使用 Open Module 过渡。
4. 模块内部实现统一位于 `internal` 下。
5. 模块间只允许依赖 `api`、`events` 等显式 Named Interface。
6. `shared` 是受控共享基础模块，不是业务代码垃圾桶。
7. jMolecules 首期采用 annotation-based model；不强制全部领域类型实现 jMolecules 泛型接口。
8. 保留现有 Egon 字节码架构 Maven Plugin。
9. 重新引入 ArchUnit，但只承担 jMolecules 语义和 archetype 特有规则。
10. Spring Boot 3.5.16 使用 Spring Modulith 1.4.x 兼容线，锁定 `1.4.12`，不使用面向 Spring Boot 4 的 2.x。
11. 第一阶段不启用事件发布注册表、事件表、事件重试、事件外部化或运行时 Modulith Actuator。
12. 第一阶段不修改任何现有 Flyway migration。
13. canonical Facade artifact 的包名、接口、DTO、异常和发布方式保持不变。
14. 不迁移由旧版本 archetype 已生成的外部项目。

## 6. 总体目标结构

每个生成应用都使用下面的逻辑结构：

```text
${package}
├── start 或 starter
│   ├── XxxApplication.java
│   ├── config
│   └── modulith
│       └── 业务模块元数据
├── shared
│   ├── api
│   │   ├── constants
│   │   ├── exceptions
│   │   └── types
│   └── internal
│       ├── infrastructure
│       └── utils
├── <business-module-a>
│   ├── api
│   ├── events
│   └── internal
│       ├── adapter
│       ├── application
│       ├── domain
│       └── infrastructure
└── <business-module-b>
    ├── api
    ├── events
    └── internal
        ├── adapter
        ├── application
        ├── domain
        └── infrastructure
```

`api` 和 `events` 不是强制每个模块都必须有内容。没有跨模块消费者时不创建空 API 类，只保留必要的元数据。

### 6.1 包排序不变量

业务代码必须遵守：

```text
<business-module>/internal/<layer>/<technical-responsibility>
```

以下旧形式必须从生产代码、测试、文档、反射字符串和 verifier 中消失：

```text
adapter/<business-module>
application/<business-module>
domain/<business-module>
infrastructure/<business-module>
facade/<business-module>
```

### 6.2 模块公开面

默认规则：

- 模块根包只放模块元数据，不放实现类。
- `api` 是同步模块调用契约。
- `events` 是允许其他模块监听的领域事实。
- 其他所有子包均为 internal。
- Spring Modulith 1.4 的 Named Interface 不会自动递归公开子包；`api`、`events` 下每个实际承载公开类型的子包都必须显式声明同名 `@NamedInterface`，同名声明合并为一个逻辑公开面。
- Controller 是 HTTP 对外入口，但属于 Java 模块内部实现，必须位于 `internal/adapter`。
- Repository、PO、JPA Repository、Cache、MQ producer、Dubbo client 均为 internal。
- Aggregate、Entity 不能直接出现在模块 API 或 canonical Facade DTO 中。

### 6.3 共享模块

顶层 `common` Java 包改为：

```text
shared
```

web/service 的 Maven artifact 仍叫 `${rootArtifactId}-common`，只改变 Java 包，不改变 artifactId。

`shared.api` 允许放：

- 稳定、业务中立的异常基类和错误契约；
- 通用标识、分页、时间、追踪等基础类型；
- 被两个业务模块共同使用且不属于其中任何一方的技术无关值对象；
- 跨域通用且业务中立的幂等、时钟、ID 生成等基础端口，但接口不能出现某个具体业务域的 Entity/Aggregate。

`shared` 禁止放：

- `UserUtils`、`CourseHelper` 等业务归属明确的类型；
- 同时罗列多个业务模块错误码的组合枚举；
- Repository、Mapper、PO、Controller、Application Service；
- canonical Facade 的复制品；
- 为了绕过 `allowedDependencies` 而搬入的 DTO 或 Service。

共享异常基类如需承载模块错误码，只能依赖 `shared.api.exceptions` 下的最小 error-code 接口或稳定字符串；user/teaching、course/exam 各自的错误码枚举留在所属模块，并保持现有对外 code 值不变。

### 6.4 模块外装配区

业务模块和 `shared` 之外，只允许保留一个装配根：

```text
light:       ${package}.start
web/service: ${package}.starter
```

装配根用于承载启动类、全局 Spring 配置、Filter、全局异常映射、AOP、序列化配置以及尚未被任何业务用例消费的外部集成示例。它不是业务模块，也不是可供业务代码复用的公共库。

依赖方向必须是：

```text
start/starter -> shared、业务模块
业务模块      -X-> start/starter
shared        -X-> start/starter
```

即使某个装配类型物理上仍编译在 Adapter/Infrastructure Maven artifact 中，它的 Java package 也必须归入 `start`/`starter`。Maven layer 表示物理编译职责，Java 根包表示逻辑模块归属，两者不能混为一谈。

改造后不得保留无人归属的顶层：

```text
adapter
application
common
domain
facade
infrastructure
```

每个旧顶层类型都必须在实施前落入“业务模块、shared、装配根”三者之一；不能通过让 Spring Modulith 忽略旧包来制造表面通过。

## 7. Spring Modulith 模块识别设计

### 7.1 启动类

保留当前启动类所在的 `start`/`starter` Maven 和 Java package，不为 Spring Modulith 强行移动启动类。

启动类增加：

```java
@Modulithic(
        systemName = "<generated-artifact-id>",
        sharedModules = "shared",
        additionalPackages = "${package}"
)
```

`systemName` 取生成项目标识，不硬编码 basic IT 的示例名称：

```text
light:       ${artifactId}
web/service: ${rootArtifactId}
```

原有：

```java
@SpringBootApplication(scanBasePackages = "${package}")
@EnableDubbo(...)
@EnableJpaRepositories(...)
@EntityScan(...)
```

继续保留，并按新业务根包更新扫描路径。

### 7.2 检测策略

生成配置和架构测试统一使用：

```properties
spring.modulith.detection-strategy=explicitly-annotated
```

生成项目的 YAML 形式：

```yaml
spring:
  modulith:
    detection-strategy: explicitly-annotated
```

Surefire 在保留现有配置的基础上追加：

```xml
<systemPropertyVariables>
    <spring.modulith.detection-strategy>
        explicitly-annotated
    </spring.modulith.detection-strategy>
</systemPropertyVariables>
```

原因：

- 只识别明确标注的业务模块；
- `start`、`starter`、全局配置等装配包不会被误识别成业务模块；
- 后续新增模块必须显式声明，避免包一创建就自动进入架构图。

该属性写入生成项目的 `application.yml`，同时由 Maven Surefire 作为测试 JVM system property 传入。这样直接调用 `ApplicationModules.of(...)` 的静态架构测试也使用显式检测策略，不依赖 Spring Test 上下文是否已经启动。若现有 Surefire 已有 `systemPropertyVariables`，必须合并键值，不能覆盖日志、编码或 profile 等已有属性。

### 7.3 元数据隔离

web/service 的业务模块横跨多个 Maven layer artifact。为避免 Domain module 编译依赖 Spring Modulith：

- `@ApplicationModule` 元数据类型放在 Starter module 的生产源码中；
- 元数据类型的 package 必须是目标业务模块根包；
- 元数据类型为 package-private、无状态、无业务方法；
- `@NamedInterface` 的 package metadata 同样由 Starter module 提供；
- Domain/Application/Infrastructure/Adapter module 不直接依赖 Spring Modulith API。

示例：

```text
starter/src/main/java/user/ModuleMetadata.java
starter/src/main/java/user/api/package-info.java
starter/src/main/java/user/api/dto/package-info.java
starter/src/main/java/user/events/package-info.java
```

这些文件生成后的 package 分别是：

```text
${package}.user
${package}.user.api
${package}.user.api.dto
${package}.user.events
```

`user.api` 与 `user.api.dto` 都声明 `@NamedInterface("api")`，共同组成 `user :: api`。其他公开子包采用相同规则；不能因为父包已声明就假定子包自动公开。

light 是单 Maven 模块，使用相同元数据布局，保证三个 archetype 的开发体验一致。

### 7.4 Closed module

所有业务模块都使用默认 closed 类型：

```java
@ApplicationModule(...)
```

禁止：

```java
@ApplicationModule(type = ApplicationModule.Type.OPEN)
```

这些 archetype 是新项目模板，不存在需要冻结的历史非法访问，不能把迁移用的 Open Module 变成默认架构。

### 7.5 精确的模块依赖声明

每个模块都必须显式设置 `allowedDependencies`，禁止保留 Spring Modulith 表示“不限制依赖”的默认开放 token。

元数据形式：

```java
@ApplicationModule(
        displayName = "User",
        allowedDependencies = {"shared::api"}
)
final class ModuleMetadata {
}
```

没有任何业务模块依赖的 `shared` 使用：

```java
@ApplicationModule(
        displayName = "Shared",
        allowedDependencies = {}
)
final class ModuleMetadata {
}
```

三个 archetype 的精确值如下：

| Archetype | 模块 | `allowedDependencies` |
|---|---|---|
| light | `shared` | `{}` |
| light | `user` | `{"shared::api"}` |
| light | `teaching` | `{"shared::api"}` |
| web | `shared` | `{}` |
| web | `user` | `{"shared::api"}` |
| web | `teaching` | `{"shared::api", "user::api"}` |
| service | `shared` | `{}` |
| service | `course` | `{"shared::api"}` |
| service | `exam` | `{"shared::api", "course::api"}` |

`sharedModules = "shared"` 只表示所有业务模块均可使用该共享模块；`shared` 仍然是 closed module，消费者只能访问其根包或显式 Named Interface。本 spec 要求根包只放元数据，因此共享类型必须通过 `shared::api` 暴露。

## 8. 版本与依赖设计

### 8.1 版本

生成 root POM 增加：

```xml
<spring-modulith.version>1.4.12</spring-modulith.version>
<jmolecules.version>2025.0.2</jmolecules.version>
<archunit.version>1.4.2</archunit.version>
```

依赖管理增加：

```xml
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-bom</artifactId>
    <version>${spring-modulith.version}</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>

<dependency>
    <groupId>org.jmolecules</groupId>
    <artifactId>jmolecules-bom</artifactId>
    <version>${jmolecules.version}</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

版本理由：

- 当前 archetype 固定 Spring Boot `3.5.16`；
- Spring Modulith 官方兼容矩阵规定 1.4 对应 Spring Boot 3.5；
- Spring Modulith 2.x 对应 Spring Boot 4，不进入本次范围；
- ArchUnit `1.4.2` 与文章及 Spring Modulith 当前兼容线一致；
- jMolecules `2025.0.2` 与综合实践和 Spring Modulith 兼容信息一致。

### 8.2 生产依赖

light 单模块增加：

```text
spring-modulith-api
jmolecules-ddd
jmolecules-events
jmolecules-layered-architecture
```

web/service：

- Starter module 增加 `spring-modulith-api`；
- Domain module 增加 `jmolecules-ddd`、`jmolecules-events`、`jmolecules-layered-architecture`；
- Application/Infrastructure/Adapter module 只在对应 package-info 需要时增加 `jmolecules-layered-architecture`；
- Common module 只有实际共享值对象使用 DDD 注解时才增加 `jmolecules-ddd`。

`spring-modulith-api` 足以承载 `@Modulithic`、`@ApplicationModule` 和 `@NamedInterface` 元数据。模块模型、验证、模块测试和文档 API 均由测试依赖提供；第一阶段不为了架构检查把 Spring Modulith 运行时能力带入应用。

不增加：

```text
jmolecules-spring
jmolecules-jackson
jmolecules-jpa
jmolecules-bytebuddy
jmolecules-cqrs-architecture
```

原因：

- 本阶段只需要架构语义，不改变 JSON 绑定、Spring Converter、JPA 映射或字节码增强；
- 当前 Commands/Queries 已经由包和命名表达，暂不为 CQRS 增加另一套注解；
- annotation-based model 的侵入和运行风险最低。

### 8.3 测试依赖

light 的测试作用域、web/service 的 Starter 测试作用域增加：

```text
spring-modulith-starter-test
spring-modulith-docs
archunit-junit5
jmolecules-archunit
```

`jmolecules-archunit` 由 Spring Modulith 的 `ApplicationModules.verify()` 自动触发其 DDD/架构规则；自定义 ArchUnit 测试只补充 Egon-COLA 特有规则。

### 8.4 首期不增加的运行时依赖

本阶段不增加：

```text
spring-modulith-starter-core
spring-modulith-starter-jdbc
spring-modulith-starter-jpa
spring-modulith-starter-insight
spring-modulith-events-kafka
spring-modulith-events-amqp
```

因此本阶段：

- 不创建 `event_publication` 表；
- 不增加 Flyway migration；
- 不改变事件重试和补偿语义；
- 不新增 `/actuator/modulith` 暴露面；
- 不改变现有 RabbitMQ producer/consumer 行为。

## 9. Light Archetype 目标设计

### 9.1 模块

```text
shared
user
teaching
```

依赖：

```text
user -> shared :: api
teaching -> shared :: api
```

`user` 和 `teaching` 首期不直接依赖彼此。

### 9.2 目标包树

```text
${package}
├── start
├── shared
├── user
│   ├── api
│   │   ├── facade
│   │   ├── dto
│   │   ├── enums
│   │   └── exceptions
│   ├── events
│   └── internal
│       ├── adapter
│       │   ├── controller
│       │   ├── graphql
│       │   ├── mq
│       │   ├── rpc
│       │   ├── converter
│       │   ├── validators
│       │   └── vo
│       ├── application
│       │   ├── manage
│       │   ├── command
│       │   ├── query
│       │   ├── result
│       │   ├── converter
│       │   ├── validators
│       │   └── assemblers
│       ├── domain
│       │   ├── aggregates
│       │   ├── entities
│       │   ├── vos
│       │   ├── repos
│       │   ├── service
│       │   ├── validators
│       │   ├── enums
│       │   └── exceptions
│       └── infrastructure
│           ├── repo
│           ├── cache
│           ├── client
│           ├── mq
│           ├── service
│           └── validators
└── teaching
    └── 与 user 对称
```

### 9.3 Facade

light 当前生成的本地 Facade contract 分别归入：

```text
user.api
teaching.api
```

并作为 Named Interface 暴露。

Facade implementation 继续位于：

```text
user.internal.adapter.facade.impl
teaching.internal.adapter.facade.impl
```

不改变 Dubbo 接口方法、DTO 字段和异常语义。

### 9.4 全局技术代码

以下类型保留在 `start` 下或迁入 `start` 的技术装配子包：

- 全局配置；
- async 和 encryption；
- 全局异常响应包装；
- Trace/RequestContext Filter；
- 只依赖 shared contract 的全局技术处理。

这些类型不得依赖某个业务模块的 Aggregate、Entity、Repository 或 Application implementation。

### 9.5 旧全局包归属

light 的非业务路径按以下方式处理：

| 当前路径 | 目标 |
|---|---|
| `common/constants`、`common/exceptions` | `shared.api.constants`、`shared.api.exceptions` |
| `common/utils` | 对外稳定类型进入 `shared.api`，纯内部工具进入 `shared.internal.utils` |
| `adapter/filter`、`adapter/handler` | `start.adapter.filter`、`start.adapter.handler` |
| `infrastructure/aop`、`infrastructure/config` | `start.config.aop`、`start.config` |
| `facade/user`、`facade/teaching` | `user.api.facade`、`teaching.api.facade` |
| 各技术层下的 `user`、`teaching` | 对应业务模块的 `internal/<layer>` |
| 只剩 package-info 的旧顶层包 | 删除，不保留空壳 |

业务模块不得反向调用 `start.adapter` 或 `start.config`；全局入口只负责建立上下文、统一错误响应和调用模块 API。

### 9.6 现有字节码插件映射

light 继续使用 `check` goal，但 package mappings 改为匹配：

```text
${package}.user.internal.domain..            -> DOMAIN
${package}.teaching.internal.domain..        -> DOMAIN
${package}.user.internal.application..       -> APPLICATION
${package}.teaching.internal.application..   -> APPLICATION
${package}.user.internal.infrastructure..    -> INFRASTRUCTURE
${package}.teaching.internal.infrastructure..-> INFRASTRUCTURE
${package}.user.internal.adapter..           -> ADAPTER
${package}.teaching.internal.adapter..       -> ADAPTER
${package}.user.api..                        -> FACADE
${package}.user.events..                     -> FACADE
${package}.teaching.api..                    -> FACADE
${package}.teaching.events..                 -> FACADE
${package}.start..                           -> STARTER
${package}.shared.api..                      -> COMMON
${package}.shared.internal.utils..           -> COMMON
${package}.shared.internal.infrastructure..  -> INFRASTRUCTURE
```

Maven Map 配置使用上面的完整可生成 package pattern，不能使用 XML 元素名无法表达的前置通配符。业务模块根包下的 metadata class 可以是 `UNKNOWN`，但只能是 Modulith 元数据；任何其他业务实现落入 `UNKNOWN` 都必须由 verifier 判定失败。

## 10. Web Archetype 目标设计

### 10.1 模块

```text
shared
user
teaching
```

依赖：

```text
user -> shared :: api
teaching -> shared :: api
teaching -> user :: api
```

禁止：

```text
user -> teaching
teaching -> user.internal
```

### 10.2 Maven 结构

以下六个 Maven artifactId 保持不变：

```text
${rootArtifactId}-common
${rootArtifactId}-domain
${rootArtifactId}-application
${rootArtifactId}-infrastructure
${rootArtifactId}-adapter
${rootArtifactId}-starter
```

每个 artifact 内部按业务模块重新放置源码：

```text
domain module:
  user/internal/domain
  teaching/internal/domain
  user/api

application module:
  user/internal/application
  teaching/internal/application

infrastructure module:
  user/internal/infrastructure
  teaching/internal/infrastructure

adapter module:
  user/internal/adapter
  teaching/internal/adapter
```

### 10.3 Teaching 到 User 的同步模块 API

当前 teaching 直接依赖 `UserRepository` 和 `User Entity`。改造后由 user 模块提供最小同步查询契约：

```text
user.api.UserDirectory
user.api.UserId
user.api.UserMembership
```

最小契约形态：

```java
interface UserDirectory {
    Optional<UserMembership> findMembership(UserId userId);
}

record UserMembership(UserId userId, boolean assignable) {
}
```

契约语义：

- `Optional.empty()` 对应当前“user not found”分支；
- `UserDirectory` 只提供 teaching 当前确实需要的用户存在性和可加入班级状态查询；
- `UserMembership` 是不可变摘要，只暴露 `userId` 和是否允许加入，不暴露 `UserStatus`、User Aggregate/Entity；
- `UserId` 是强类型业务标识；
- 实现位于 `user.internal.application`；
- teaching 的 Application 使用 `UserDirectory`；
- teaching Domain 只接收 `UserId` 和必要的状态值，不接收 `User` Entity；
- teaching 不再访问 `UserRepository`。

该调用必须保持同步，因为“用户是否存在且允许加入班级”是当前命令成功前必须确认的前置条件。这里不为了事件驱动而改成最终一致。

### 10.4 Web 的外部 Evaluation 集成

canonical Evaluation Facade 保持外部契约：

```text
top.egon.cola.evaluation.facade..
```

现有尚未接入业务用例的 Evaluation client/stub 归入 Starter 技术装配区：

```text
${package}.starter.integration.evaluation
```

相关 port、client、stub、failure mapper 物理移动到 Starter Maven artifact。在它真正被某个业务模块消费前，不把它声明为 user 或 teaching 的模块依赖。

ArchUnit 继续保证：

- 只有允许的 integration/adapter 包可以依赖 canonical Evaluation Facade；
- Domain/Application 不得依赖 canonical Facade DTO；
- canonical Facade 类型不能被复制到生成项目。

### 10.5 共享幂等能力与模块事件端口

当前被 user 和 teaching 同时使用的幂等能力迁入：

```text
shared.api.idempotency.CommandIdempotencyPort
shared.api.idempotency.IdempotentCommand
```

它们必须保持业务中立，方法签名不能引用 user/teaching 的 Aggregate、Entity、Command、DTO 或应用异常；Infrastructure adapter 位于 `shared.internal.infrastructure`。

当前 `IdempotentCommand` 直接创建 `OrganizationApplicationException`，迁移时改为由调用方传入重复请求异常工厂：

```java
static <T> T execute(
        CommandIdempotencyPort port,
        String operation,
        String requestId,
        Supplier<? extends RuntimeException> duplicateFailure,
        Supplier<T> action)
```

user/teaching 调用方继续提供当前的冲突异常，claim、失败 release 和异常传播语义不变。这里使用函数参数隔离模块错误策略，不再引入额外 Strategy 类。

事件发布不进入 `shared`。现有 `OrganizationEventPublisher` 和 `OrganizationDomainEvent` 拆分为各模块自己拥有的端口和事件：

```text
user.internal.domain.events.UserEventPublisher
teaching.internal.domain.events.TeachingEventPublisher
```

具体业务事件继续归所属模块；RabbitMQ/Local publisher adapter 分别位于所属模块的 `internal.infrastructure`。此次只调整内部类型归属，不改变 routing key、payload、发送时机和 local fallback 语义。

### 10.6 Canonical Organization Facade

web Adapter 继续实现：

```text
top.egon.cola.organization.facade..
```

实现移动到：

```text
user.internal.adapter.facade.impl
teaching.internal.adapter.facade.impl
```

canonical Facade artifact 本身不改。

当前跨 user/teaching 复用的 `OrganizationFacadeSupport` 不得留在无人归属的 `adapter.facade`，也不得放入 `shared` 使共享模块依赖 canonical Facade。它拆为：

```text
user.internal.adapter.facade.support.UserFacadeSupport
teaching.internal.adapter.facade.support.TeachingFacadeSupport
```

两份 support 各自完成相同的 request context 建立和异常映射；公共的 `OrganizationRequestContext`/Holder 进入 `shared.api.context`。这里接受少量边界代码重复，以换取模块独立和 canonical Facade 依赖不泄漏。

### 10.7 旧全局包归属

web 的所有旧顶层类型必须按下表迁移：

| 当前路径/类型 | 目标 |
|---|---|
| `common/**` | 稳定公开类型进入 `shared.api`，内部工具进入 `shared.internal` |
| `application/context/**` | `shared.api.context` |
| 跨模块 Application 异常基类/失败类型 | `shared.api.exceptions` |
| `application/support/IdempotentCommand` | `shared.api.idempotency`，按 10.5 去除业务异常依赖 |
| `application/support/OrganizationTransactionHooks` | `shared.api.transaction.AfterCommitExecutor` 端口和 `shared.internal.infrastructure.transaction.SpringAfterCommitExecutor` |
| `application/config/DomainServiceConfiguration` | 拆为 `user.internal.application.config` 与 `teaching.internal.application.config` |
| `domain/client/CommandIdempotencyPort` | `shared.api.idempotency` |
| `domain/client/evaluation`、`infrastructure/client/evaluation` | 物理移动到 Starter artifact 的 `${package}.starter.integration.evaluation` |
| `domain/events/OrganizationDomainEvent`、`domain/client/OrganizationEventPublisher` | 按 10.5 拆入 user/teaching，不保留全局 marker |
| `domain/exceptions` | 通用异常基类进入 `shared.api.exceptions`；user/teaching 错误码分别归模块 internal Domain，错误码字符串不变 |
| `domain/validators/OrganizationCodeValidator` | `shared.api.validation`，保持纯 Java、无模块 Entity 依赖 |
| 全局 Filter、GraphQL interceptor、异常 handler、AOP、Jackson/Swagger/Redis/Rabbit 配置 | `${package}.starter` 下对应技术子包，物理 Maven artifact 可保持不变 |
| 全局 Facade/MQ support | 分别拆为 user/teaching 的 module-local Adapter support |
| idempotency Cache adapter | `shared.internal.infrastructure.idempotency` |
| 全局 Organization event publisher | 按 10.5 拆为 user/teaching 的 module-local Infrastructure publisher |

`AfterCommitExecutor` 是现有事务提交后回调语义的 Port，Spring 实现仍使用 `TransactionSynchronizationManager`。这是真实的框架变化点，采用 Port and Adapter 隔离；不新增 Strategy 层级，提交后执行、无事务时立即执行的行为保持不变。

## 11. Service Archetype 目标设计

### 11.1 模块

```text
shared
course
exam
```

依赖：

```text
course -> shared :: api
exam -> shared :: api
exam -> course :: api
```

禁止：

```text
course -> exam
exam -> course.internal
```

### 11.2 Maven 结构

现有六个 Maven artifactId 保持不变。Java 源码按下列方式重排：

```text
domain module:
  course/internal/domain
  exam/internal/domain
  course/api

application module:
  course/internal/application
  exam/internal/application

infrastructure module:
  course/internal/infrastructure
  exam/internal/infrastructure

adapter module:
  course/internal/adapter
  exam/internal/adapter
```

### 11.3 Exam 到 Course 的同步模块 API

course 提供：

```text
course.api.CourseCatalog
course.api.CourseId
course.api.CourseSnapshot
```

最小契约形态：

```java
interface CourseCatalog {
    Optional<CourseSnapshot> findCourse(CourseId courseId);
}

record CourseSnapshot(CourseId courseId, boolean active) {
}
```

契约语义：

- `CourseCatalog` 提供 exam 当前确实需要的课程查询；
- `Optional.empty()` 对应当前“course not found”分支；
- `CourseSnapshot` 是不可变摘要，只包含课程 ID 和是否有效，不暴露 `CourseStatus` 或 Course Entity；
- `CourseId` 是跨模块公开的强类型标识；
- 实现位于 `course.internal.application`；
- exam Application 不再访问 `CourseRepository`；
- exam Domain 不再接收 `Course Entity`，只接收 `CourseId` 或 `CourseSnapshot`。

该调用保持同步，因为考试创建时必须即时确认课程有效性。

### 11.4 外部 Organization 集成

canonical Organization Facade 保持：

```text
top.egon.cola.organization.facade..
```

当前未被 course/exam 业务用例消费的 Organization directory client/stub 归入：

```text
${package}.starter.integration.organization
```

相关 port、client、stub、failure mapper 物理移动到 Starter Maven artifact。只有将来明确接入某个业务用例时，才把端口和实现迁入对应业务模块。

### 11.5 Pure Service 边界

service 继续是 RPC/MQ Service，不新增业务 HTTP 入口。

service 业务模块 internal 下禁止出现：

```text
controller
web
filter
graphql
vo
```

启动所需的 Spring Web 依赖只服务于运行容器、Actuator 或框架要求，不能成为业务 API。

### 11.6 Canonical Evaluation Facade

service Adapter 继续实现：

```text
top.egon.cola.evaluation.facade..
```

实现分别位于：

```text
course.internal.adapter.facade.impl
exam.internal.adapter.facade.impl
```

canonical Evaluation Facade artifact 本身不改。

### 11.7 旧全局包归属

service 的所有旧顶层类型必须按下表迁移：

| 当前路径/类型 | 目标 |
|---|---|
| `common/**` | 稳定公开类型进入 `shared.api`，内部工具进入 `shared.internal` |
| `application/exceptions/ApplicationException`、通用 `application/result` | `shared.api.exceptions`、`shared.api.types` |
| `application/exceptions/ApplicationErrorCode` | 拆为 course/exam module-local Application 错误码；现有 code 字符串不变 |
| `domain/common/Page` 和通用异常基类 | `shared.api.types`、`shared.api.exceptions` |
| `EvaluationDomainErrorCode` | 拆为 course/exam 模块 internal Domain 错误码；现有 code 字符串不变 |
| `application/config/DomainServiceConfiguration` | 拆为 `course.internal.application.config` 与 `exam.internal.application.config` |
| `domain/client/organization`、`infrastructure/client/organization` | 物理移动到 Starter artifact 的 `${package}.starter.integration.organization` |
| 全局 `adapter.handler.GlobalFacadeExceptionHandler` | 拆为 course/exam module-local Facade error mapper |
| `infrastructure/validators/EvaluationPersistenceValidator` | 拆为 course/exam module-local persistence failure translator，各自只识别本模块约束 |
| 全局 Rabbit/AOP/启动配置 | `${package}.starter` 下对应技术子包，物理 Maven artifact 可保持不变 |
| 各技术层下的 `course`、`exam` | 对应业务模块的 `internal/<layer>` |
| 只剩 package-info 的旧顶层包 | 删除，不保留空壳 |

Facade error mapper 的少量重复与 web 的 Facade support 采用同一原则：canonical Facade 依赖留在拥有该 provider 的模块 Adapter 内，不通过 `shared` 横向传播。

## 12. jMolecules 领域语义设计

### 12.1 首期采用注解模型

首期使用：

```text
@AggregateRoot
@Entity
@ValueObject
@Identity
@Repository
@DomainEvent
@DomainLayer
@ApplicationLayer
@InfrastructureLayer
@InterfaceLayer
```

不要求所有类立即实现：

```text
AggregateRoot<T, ID>
Entity<T, ID>
Identifier
Association
```

原因：

- 当前模型已经有大量构造器、record、MapStruct/JPA 转换和测试；
- 注解模型足以表达并校验架构语义；
- 类型模型可在以后针对稳定核心聚合单独增强，不应与包结构大迁移同时强推。

### 12.2 聚合角色

现有 `*Aggregate` 类型继续作为聚合根，首期不删除这些包装类：

```text
light:
  UserAggregate
  RolePermissionAggregate
  SchoolClassAggregate
  CourseAggregate

web:
  UserAggregate
  RolePermissionAggregate
  SchoolClassAggregate

service:
  CourseAggregate
  ExamAggregate
  ScoreAggregate
```

每个聚合根必须：

- 标注 `@AggregateRoot`；
- 暴露并标注稳定的 `@Identity`；
- 维护本聚合内部一致性；
- 不直接持有另一个聚合根对象；
- 不暴露可变内部集合；
- 不依赖 Spring、JPA、MyBatis、Redis、Dubbo、HTTP、MQ。

若当前 wrapper 没有直接 identity accessor，实现时增加只读 identity accessor，不改变业务行为。

### 12.3 Entity

`internal/domain/entities` 下真正具有生命周期和标识的类型标注 jMolecules `@Entity`，其 identity 标注 `@Identity`。

不得把以下类型误标成 DDD Entity：

- Infrastructure 的 JPA PO；
- Facade DTO；
- Application Command/Query/Result；
- Adapter Request/Response/VO。

JPA `@Entity` 只允许出现在：

```text
internal/infrastructure/repo/po
```

同一个类不得同时作为 jMolecules AggregateRoot 和 JPA Entity。

### 12.4 Value Object

满足下列条件的 `vos` 类型标注 `@ValueObject`：

- 不可变；
- 使用值相等语义；
- 构造时完成合法性校验；
- 不具有独立生命周期；
- 不持有 Repository、Service 或框架对象。

事件包装、外部系统响应和可变快照不能因为位于 `vos` 就自动标成 Value Object；不符合语义的类型要移动到更准确的 package。

### 12.5 Repository

Domain Repository interface 标注：

```java
org.jmolecules.ddd.annotation.Repository
```

Infrastructure Repository implementation 继续使用：

```java
org.springframework.stereotype.Repository
```

架构检查必须区分两种注解：

- `jmolecules-archunit` 负责 jMolecules Repository 的 DDD 语义，自定义 ArchUnit 只补充其必须位于 internal Domain 的项目路径约束；
- Spring Repository 必须位于 internal Infrastructure；
- 模块 API、Controller、GraphQL、RPC provider 不能直接依赖 Domain Repository。

### 12.6 Domain Event

现有业务事实事件标注：

```java
org.jmolecules.event.annotation.DomainEvent
```

事件要求：

- 使用过去时、业务事实命名；
- 不携带 Aggregate、Entity、PO、JPA Entity；
- 只携带必要的 ID、值和发生时间；
- 业务模块拥有自己的事件；
- 只有真正供其他模块监听的事件放入顶层 `events` Named Interface；
- 仅供模块内部或外部 Rabbit adapter 使用的事件留在 internal Domain。

本阶段不把所有现有 RabbitMQ 事件自动改成 Spring ApplicationEvent，也不自动外部化事件。

### 12.7 分层包语义

各业务模块的 package-info 增加：

```text
internal.domain          -> @DomainLayer
internal.application     -> @ApplicationLayer
internal.infrastructure -> @InfrastructureLayer
internal.adapter         -> @InterfaceLayer
```

jMolecules 注解表达“它是什么”，现有字节码插件和 ArchUnit 负责检查“它是否违规”。

## 13. 架构规则归属

### 13.1 单一权威原则

同一条规则只由一个主要执行器负责。其他工具可以提供补充证据，但不复制同一条失败规则。

| 规则 | 主要执行器 |
|---|---|
| Domain/Application/Infrastructure/Adapter/Facade/Starter/Common 通用方向 | Egon 字节码插件 |
| Domain 技术框架隔离 | Egon 字节码插件 |
| Application 禁止持久化实现 | Egon 字节码插件 |
| 业务模块循环依赖 | Spring Modulith |
| 跨模块 internal 访问 | Spring Modulith |
| `allowedDependencies`/Named Interface | Spring Modulith |
| jMolecules DDD 角色一致性 | `jmolecules-archunit`，由 `modules.verify()` 触发 |
| Controller/GraphQL/RPC/MQ 入口不得直连 Domain Repository | 自定义 ArchUnit |
| API 不得泄漏 Aggregate/Entity/PO/Mapper | 自定义 ArchUnit |
| 禁止字段注入 | 自定义 ArchUnit |
| 业务模块/shared 反向依赖 `start`/`starter` | 自定义 ArchUnit |
| light 业务域优先路径 | 自定义 ArchUnit + verifier |
| web canonical Facade 隔离 | 自定义 ArchUnit |
| service pure Service、native gRPC/Facade 隔离 | 自定义 ArchUnit |
| Maven 文件、生成文件、文档和配置契约 | `verify.groovy` |

### 13.2 保留现有字节码插件

禁止：

- 删除 `egon-cola-component-bytecode-architecture-maven-plugin`；
- 把 `ARCH-001`～`ARCH-010` 重写成 ArchUnit；
- 修改插件组件规则来适配本次 spec；
- 降低 `failurePolicy`；
- 用 baseline 接受新 archetype 违规。

web/service 继续使用 `check-reactor`，light 继续使用 `check`。

### 13.3 自定义 ArchUnit 规则集

三个 archetype 生成：

```text
ArchitectureSemanticsTest
```

规则至少包括：

1. Spring `@Autowired` 字段注入禁止。
2. Controller、GraphQL Resolver、Dubbo provider、MQ consumer 不得依赖 Domain Repository 或 Infrastructure persistence。
3. 顶层 `api`、`events` 不得引用 internal Aggregate、Entity、Repository、PO、Mapper、JPA Repository。
4. jMolecules `@AggregateRoot` 必须位于 `..internal.domain.aggregates..`。
5. jMolecules `@Entity` 必须位于 `..internal.domain.entities..`。
6. jMolecules `@ValueObject` 必须位于 Domain `vos`、模块公开 API value types 或受控 shared types。
7. jMolecules Domain Repository 必须位于 `..internal.domain..`；“必须是接口”等通用语义交给 `jmolecules-archunit`。
8. Spring Repository/JPA Entity 必须位于 internal Infrastructure。
9. `shared` 不得包含 AggregateRoot、Controller、Application Service、Repository implementation。
10. 业务模块和 `shared` 不得依赖 `start`/`starter`。
11. 业务实现不得落入未识别的顶层 package。

archetype 特有规则：

```text
light:
  禁止恢复 adapter.user / domain.user 等技术层优先根路径

web:
  Domain/Application 不得依赖 canonical Facade
  只有指定 Adapter/Starter integration 包可依赖 canonical Facade

service:
  禁止业务 controller/web/filter/graphql/vo
  禁止原生 gRPC API
  只有指定 Adapter/Starter integration 包可依赖 canonical Facade
```

### 13.4 多 Maven module 的 ArchUnit 导入

light：

- 导入生成项目根 package；
- 排除 tests 和 dependency JAR。

web/service：

- 测试放在 Starter；
- 导入生成项目根 package；
- 排除 tests，但不能全局排除 JAR，因为其他五个 reactor module 通过 JAR 出现在 Starter test classpath；
- canonical Facade 隔离规则显式导入两个稳定 Facade package；
- 不扫描整个第三方 classpath。

### 13.5 设计模式考虑

本次不新增 Strategy、Factory、Template Method 或 Handler chain。

原因：

- 模块变化点已经由 `@ApplicationModule`、Named Interface 和 `allowedDependencies` 声明式表达；
- 现有字节码插件的 `ARCH-001`～`ARCH-010` 已使用 Specification 风格规则模型，本次直接复用；
- 跨模块同步查询使用现有 Ports and Adapters 思路，由 `UserDirectory`、`CourseCatalog` 形成明确 Port；
- 外部 canonical Facade 继续承担 Facade/Anti-Corruption Layer，不引入第二套抽象；
- 为简单元数据再建立自定义工厂或检测框架只会增加维护成本。

## 14. 模块协作与事件策略

### 14.1 同步 API

以下场景使用同步模块 API：

- 命令完成前必须获得结果；
- 必须立即校验另一个模块的业务状态；
- 调用失败必须使当前用例失败；
- 查询语义稳定且返回最小摘要。

本次明确使用同步 API：

```text
web: teaching -> user :: api
service: exam -> course :: api
```

### 14.2 Domain Event

以下场景才使用模块事件：

- 上游事务已经完成；
- 下游是对既成业务事实的响应；
- 不要求上游同步等待结果；
- 允许通过重试、补偿或最终一致处理。

本阶段仅整理和标注已有 Domain Event，不新增会改变业务结果的跨模块 listener。

### 14.3 内部事件与外部集成事件

必须区分：

```text
Domain Event
  -> 应用内部业务事实

Integration Event
  -> RabbitMQ 或未来外部系统契约
```

禁止把 internal Aggregate 直接序列化成外部消息。现有 RabbitMQ message adapter 继续负责从 Domain Event 转成外部消息模型。

### 14.4 后续可选阶段

只有用户另行批准后，才讨论：

- `@ApplicationModuleListener`；
- Spring Modulith JDBC/JPA event publication registry；
- 失败重试、清理和 completion mode；
- Kafka/Rabbit event externalization；
- runtime insight 和 `/actuator/modulith`；
- 按业务模块拆分 Flyway location。

该后续阶段如涉及数据库，每个生成应用必须新增一个新的 Flyway migration，绝不修改现有 migration。

## 15. 测试设计

### 15.1 结构验证

每个生成项目增加：

```text
ModulithStructureTest
```

职责：

```java
ApplicationModules.of(XxxApplication.class).verify();
```

测试必须确认识别结果精确等于：

```text
light:   shared, user, teaching
web:     shared, user, teaching
service: shared, course, exam
```

不能多出：

```text
adapter
application
domain
infrastructure
start
starter
```

### 15.2 模块级集成测试

每个业务模块至少增加一个：

```text
@ApplicationModuleTest
```

最小集合：

```text
light:
  UserModuleTest
  TeachingModuleTest

web:
  UserModuleTest
  TeachingModuleTest

service:
  CourseModuleTest
  ExamModuleTest
```

测试原则：

- 默认 `STANDALONE`；
- 对同步依赖优先 `@MockitoBean` mock 其他模块公开 API；
- 只有确实验证组合行为时使用 `DIRECT_DEPENDENCIES`；
- 不为了让测试通过而使用 `ALL_DEPENDENCIES`；
- test profile 继续无外部 Redis、RabbitMQ、Nacos、Dubbo registry 要求；
- 不启动真实外部服务。

### 15.3 架构语义测试

`ArchitectureSemanticsTest` 执行第 13 节规则。

它不复制 `ARCH-001`～`ARCH-010`。

### 15.4 文档生成测试

每个生成项目增加：

```text
ModulithDocumentationTest
```

执行：

```java
new Documenter(modules)
        .writeModulesAsPlantUml()
        .writeIndividualModulesAsPlantUml()
        .writeModuleCanvases();
```

输出位于：

```text
target/spring-modulith-docs
```

生成文件属于构建产物，不提交进 archetype source。

### 15.5 现有测试

以下测试不得因本次改造被删除或弱化：

- Domain 规则和聚合测试；
- Application 编排测试；
- JPA/Flyway 测试；
- REST/GraphQL/Dubbo/RabbitMQ contract 测试；
- 外部依赖关闭的 Spring context 测试；
- 配置解密、profile、容器打包和日志测试；
- canonical Facade provider/consumer contract 测试；
- Egon 字节码架构 Maven Plugin 检查。

## 16. 文档设计

### 16.1 生成项目 README

三个 archetype 的：

```text
README.md
README.zh-CN.md
```

必须同步说明：

- Maven 物理层模块与 Spring Modulith 逻辑业务模块的区别；
- 新包结构；
- 模块依赖图；
- `api`、`events`、`internal` 规则；
- jMolecules 标注规范；
- 四类架构检查如何执行；
- 模块文档输出位置；
- 如何新增业务模块；
- 为什么首期未启用 event publication registry。

英文 README 仍为默认文档，中文 README 与其同目录，内容语义一致。

### 16.2 Living architecture documents

更新：

```text
egon-cola-archetype-light/large-monolith-light-domain-architecture.md
egon-cola-archetype-web/multi-project-multi-module-architecture.md
egon-cola-archetype-service/student-management-service-only-rpc-mq-architecture.md
```

文档必须同时展示：

```text
Maven layer graph
Spring Modulith business module graph
模块内 COLA layer graph
```

不能再把 `adapter/user` 等旧路径当成目标架构。

### 16.3 Verifier 契约

三个 `verify.groovy` 必须验证：

- BOM、版本属性和依赖作用域；
- Spring Modulith 元数据和 explicit detection；
- 精确的模块 ID 和 `allowedDependencies`；
- jMolecules 注解/依赖存在；
- ArchUnit 只包含特有规则，不复制通用插件规则；
- Egon 字节码插件仍存在且 goal 不变；
- 文档生成测试及输出存在；
- 旧技术层优先业务路径不存在；
- light 的字节码插件 package mapping 使用 archetype `${package}` 生成值，不再硬编码 `it.pkg`；
- canonical Facade 包未复制；
- 所有 `${package}`、`${rootArtifactId}` 等 archetype placeholder 在生成后被正确替换。

## 17. 允许修改的范围

允许修改：

- 三个 archetype 的 `src/main/resources/archetype-resources`；
- 三个 archetype 的 `META-INF/maven/archetype-metadata.xml`；
- 三个 archetype 的 `src/test/resources/projects/basic`；
- 三个 archetype module POM；
- `egon-cola-archetypes/pom.xml`，仅当 archetype IT 依赖管理确实需要；
- 三个生成项目 README；
- light/web/service living architecture documents；
- 与旧路径、旧测试名或文档输出直接耦合的 CI/verification assertion；
- 本 spec 以及用户批准后的实现计划。

## 18. 明确不在范围内

- 修改 `ARCH-001`～`ARCH-010` 的组件实现。
- 删除或替换 Egon 字节码架构 Maven Plugin。
- 改 canonical Facade 接口、DTO、异常、groupId、artifactId 或 package。
- 改 REST path、GraphQL schema、Dubbo method、Rabbit routing key 或消息字段语义。
- 改数据库 schema、表、索引、种子数据或 Flyway migration。
- 启用 event publication registry。
- 启用运行时 Modulith actuator/observability。
- 引入 Kafka。
- 引入 jMolecules Spring/Jackson/JPA/ByteBuddy integration。
- 改 web/service 的六 Maven 模块。
- 把 service 改成 Web API 项目。
- 自动迁移历史生成项目。
- 启动应用、容器、数据库、broker、浏览器或外部服务进行验证。

## 19. 实施顺序

用户批准本 spec 后，实施计划必须按以下依赖顺序拆分，每项单独提交：

1. 建立公共版本/依赖基线和失败的 verifier 契约。
2. 改 light 的 Java 包、Modulith 元数据和扫描路径。
3. 为 light 增加 jMolecules 语义、ArchUnit 特有规则、模块测试和文档测试。
4. 改 web 的 Java 包、Modulith 元数据和扫描路径。
5. 用 `user.api` 消除 web 的 teaching -> user internal 依赖。
6. 为 web 增加 jMolecules、ArchUnit、模块测试和文档测试。
7. 改 service 的 Java 包、Modulith 元数据和扫描路径。
8. 用 `course.api` 消除 service 的 exam -> course internal 依赖。
9. 为 service 增加 jMolecules、ArchUnit、模块测试和文档测试。
10. 更新双语 README、living docs 和三套 verifier。
11. 执行三 archetype 及生成项目的全量验证。

不得同时让多个任务修改同一 archetype 的同一源码树。

## 20. 验证方案

### 20.1 静态检查

```bash
git diff --check
```

搜索并拒绝旧业务路径：

```text
adapter.user
application.user
domain.user
infrastructure.user
adapter.teaching
application.teaching
domain.teaching
infrastructure.teaching
adapter.course
application.course
domain.course
infrastructure.course
adapter.exam
application.exam
domain.exam
infrastructure.exam
```

历史 specs/plans 不参与旧路径拒绝。

### 20.2 Archetype reactor

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml clean integration-test
```

### 20.3 单 archetype 定位验证

```bash
./mvnw -B -ntp \
  -pl egon-cola-archetypes/egon-cola-archetype-light \
  -am clean integration-test

./mvnw -B -ntp \
  -pl egon-cola-archetypes/egon-cola-archetype-web \
  -am clean integration-test

./mvnw -B -ntp \
  -pl egon-cola-archetypes/egon-cola-archetype-service \
  -am clean integration-test
```

### 20.4 生成项目

三个 basic generated project 分别执行：

```bash
./mvnw -B -ntp clean verify
```

验证结果必须同时包含：

- 原有单元/集成/contract tests 通过；
- `ModulithStructureTest` 通过；
- 六个业务 `ApplicationModuleTest` 通过；
- `ArchitectureSemanticsTest` 通过；
- jMolecules rules 通过；
- Egon architecture report 无新 finding；
- `target/spring-modulith-docs` 生成；
- generated project `verify` 不要求外部服务。

### 20.5 生成契约

`verify.groovy` 还必须确认：

- light 精确识别 `shared/user/teaching`；
- web 精确识别 `shared/user/teaching`；
- service 精确识别 `shared/course/exam`；
- web 只存在 `teaching -> user::api` 的业务跨模块依赖；
- service 只存在 `exam -> course::api` 的业务跨模块依赖；
- 没有业务模块访问另一个模块的 `internal`；
- 没有旧 package 或 unresolved Velocity placeholder；
- canonical Facade contract 没有被复制。

## 21. 风险与控制

### 21.1 Spring Modulith 把 Maven dependency JAR 当成外部库

风险：

web/service 业务模块横跨 reactor JAR，测试如果排除所有 JAR，会只看到 Starter。

控制：

- Spring Modulith/ArchUnit 测试放 Starter；
- Starter 显式依赖现有 Adapter/Infrastructure，沿传递依赖获得完整 reactor classpath；
- web/service 的 ArchUnit 不使用 `DoNotIncludeJars`；
- 模块识别测试断言精确模块集合，防止“测试通过但只扫描一小部分”。

### 21.2 Domain 被迫依赖 Spring Modulith

风险：

`@ApplicationModule` 和 `@NamedInterface` 若直接放 Domain module，会破坏 Domain 技术纯净性。

控制：

- Modulith 元数据由 Starter/Start 生产源码提供；
- Domain 只依赖 jMolecules 架构语义；
- Egon `ARCH-002` 继续阻断 Domain 的 Spring 依赖。

### 21.3 业务模块根包跨多个 Maven artifact

风险：

形成同一逻辑 package tree 分布在多个 JAR 的情况。

控制：

- 不在多个 artifact 中重复定义同一个业务类或 package-info；
- Modulith/Named Interface metadata 只由 Starter 提供；
- 各 layer 使用不同子包，不产生 class 重名；
- generated project `clean verify` 和模块文档共同证明扫描完整。

### 21.4 规则重复

风险：

同一违规同时被 Plugin、Modulith、jMolecules、ArchUnit 报告。

控制：

- 按第 13 节分配唯一主要执行器；
- 自定义 ArchUnit 不实现 `ARCH-001`～`ARCH-010`；
- test name 和 failure message 标明规则归属。

### 21.5 jMolecules 只贴注解、不改变语义

风险：

代码看起来具备 DDD 标签，但 Aggregate 仍直接互相引用或没有 identity。

控制：

- `jmolecules-archunit` 进入 blocking test；
- AggregateRoot 必须具备 identity；
- 跨聚合只传 ID/摘要；
- Value Object 必须不可变；
- API 泄漏规则进入 blocking test。

### 21.6 Shared 变成绕过模块边界的入口

控制：

- `shared` 无条件禁止 AggregateRoot、Controller、Application Service 和 Repository implementation；
- 业务名明确的类型必须归回业务模块；
- `shared` 只能依赖 JDK、受控基础库和现有 common core；
- Modulith canvas 和 ArchUnit 每次构建检查 shared 内容。

### 21.7 包迁移造成配置字符串漂移

风险位置：

- `@EnableDubbo`；
- `@EnableJpaRepositories`；
- `@EntityScan`；
- MapStruct；
- Spring component scan；
- tests；
- YAML/properties；
- reflection/string package checks；
- Maven plugin package mappings；
- README/living docs；
- `verify.groovy`。

控制：

- 同时搜索 slash path 和 dotted package；
- 生成项目 `clean verify` 作为最终契约，不以源码 compile 代替。

## 22. 完成验收标准

只有以下全部满足，实施任务才算完成：

1. 三个 archetype 都完成业务模块优先的 Java 包迁移。
2. light/web/service 的 Maven artifact 结构未改变。
3. 三个 generated project 的 Modulith 模块集合精确符合 spec。
4. `allowedDependencies` 与本 spec 一致且无循环。
5. web 不再存在 teaching 对 user internal Entity/Repository 的依赖。
6. service 不再存在 exam 对 course internal Entity/Repository 的依赖。
7. 关键 Aggregate、Entity、Value Object、Repository、Domain Event 具备正确 jMolecules 语义。
8. `jmolecules-archunit` 与自定义 `ArchitectureSemanticsTest` 通过。
9. Egon `ARCH-001`～`ARCH-010` 插件仍启用且通过。
10. 六个业务模块级集成测试通过。
11. 三个 generated project 都生成 `target/spring-modulith-docs`。
12. canonical Facade contract 和 provider/consumer 行为未改变。
13. service 未增加业务 Web surface。
14. 现有 Flyway migration 的内容和校验和不变。
15. 三 archetype reactor `clean integration-test` 通过。
16. 三个 generated project `clean verify` 通过。
17. 双语 README、living docs、源码、测试和 verifier 对同一结构没有矛盾。
18. 未启动应用、容器、数据库、broker、浏览器或外部服务。
19. light 使用非 `it.pkg` 的额外生成样例证明字节码 package mapping 随 `${package}` 正确生成。

## 23. 后续阶段入口

本 spec 审核通过后，下一步只允许：

1. 使用 `writing-plans` 生成逐任务实施计划；
2. 在计划中为每个 archetype 明确文件清单、失败测试、提交边界和验证命令；
3. 用户再次确认计划后才开始代码实现。

在用户确认本 spec 前，不创建实现分支、不修改 archetype 源码、不增加依赖、不运行会改变项目状态的生成或迁移操作。

## 24. 参考依据

- Spring Modulith 1.4 官方兼容矩阵：
  `https://docs.spring.io/spring-modulith/reference/1.4/appendix.html`
- Spring Modulith 模块识别：
  `https://docs.spring.io/spring-modulith/reference/1.4/fundamentals.html`
- Spring Modulith 模块测试：
  `https://docs.spring.io/spring-modulith/reference/1.4/testing.html`
- Spring Modulith 模块文档：
  `https://docs.spring.io/spring-modulith/reference/1.4/documentation.html`
- ArchUnit 官方用户指南：
  `https://www.archunit.org/userguide/html/000_Index.html`
- jMolecules：
  `https://github.com/xmolecules/jmolecules`
- jMolecules Integrations：
  `https://github.com/xmolecules/jmolecules-integrations`
