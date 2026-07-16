# Egon-COLA Method Extension

[English](README.md) | 中文

`egon-cola-component-method-extension` 是一个轻量级 Spring Boot Starter，用于在执行带注解的方法之前运行一个由业务定义的决策 Handler。

## 模块

| 模块 | 用途 |
|---|---|
| `egon-cola-component-method-extension-starter` | 提供注解、Handler 契约、AOP、响应转换和自动配置 |
| `egon-cola-component-method-extension-test` | 提供黑名单、灰度发布和临时校验示例 |

## 依赖

导入 `top.egon:egon-cola-components-bom:5.2.3`，然后添加：

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-method-extension-starter</artifactId>
</dependency>
```

## 配置

```yaml
egon:
  cola:
    component:
      method-extension:
        enabled: true
        engine: AOP
        not-ready-policy: PROCEED
        order: -2147483548
```

`engine` 接受 `AOP`、`AGENT` 或 `DISABLED`。未配置该值时保留现有 AOP 行为。`enabled=false` 始终会禁用这两种引擎。

| 引擎 | 支持的方法 | 运行时边界 |
|---|---|---|
| `AOP` | 可通过 Spring AOP 代理访问的 public 方法 | 现有默认模式；不需要 Java Agent |
| `AGENT` | 任意可见性的具体实例方法，包括 private 自调用、final 方法、synchronized 方法、通过接口声明注解的方法和非 Spring 对象 | 需要 bytecode starter，以及通过 `features=method-extension` 启用功能的 premain Agent |
| `DISABLED` | 无 | 不注册 AOP 或 Agent 集成 |

Agent 模式排除 static 方法、构造器、abstract 方法、native 方法、synthetic 方法和 bridge 方法。生成的 JDK 和 Spring CGLIB 代理类也会被排除，因此只增强具体目标类，每个 Handler 仅运行一次。

## Handler

将每个 Handler 注册为 Spring Bean。返回 `allow()` 会调用原方法，返回 `reject(response)` 会直接返回对象，返回 `reject()` 则使用注解中的 `returnJson`。

```java
@Component
public class BlacklistHandler implements MethodExtensionHandler {

    @Override
    public MethodExtensionDecision evaluate(MethodExtensionContext context) {
        String userId = (String) context.arguments()[0];
        return Set.of("bbb", "222").contains(userId)
                ? MethodExtensionDecision.rejectWithReason("blacklist hit")
                : MethodExtensionDecision.allow();
    }
}
```

```java
@MethodExtension(
        handler = BlacklistHandler.class,
        returnJson = "{\"code\":1111,\"message\":\"access rejected\"}"
)
public UserResponse query(String userId) {
    return new UserResponse(0, "success");
}
```

## 响应规则

1. Handler 直接返回的响应优先级最高，并且必须与方法返回类型匹配。
2. 非空白的 `returnJson` 优先级次之。
3. `String` 方法会原样接收 `returnJson`。
4. 对象和泛型 JSON 转换要求应用中恰好存在一个 `ObjectMapper` Bean。
5. `void` 方法拒绝时返回 `null`，并且不得配置 `returnJson`。
6. 缺少响应或类型不兼容会产生明确的组件异常。

## 错误行为

Handler 缺失或存在歧义、决策为 null，以及响应配置无效时，会抛出 `MethodExtensionConfigurationException`。JSON 和响应类型错误会抛出 `MethodExtensionResponseException`。Handler 抛出的异常会被记录并原样向外传播；Handler 失败后绝不会执行原方法。

## Spring AOP 限制

只有 Spring 管理的代理 Bean 上的 public 方法会被拦截。不支持通过 `this` 的自调用、private 方法、static 方法和不可代理的 final 方法。V1 执行一个同步 Handler，不提供 Handler 链、响应式适配器、Web 层、Redis 集成或数据库状态。

## Agent 安装

Method Extension starter 保持独立，不依赖 bytecode 组件。若要使用 `engine: AGENT`，请单独添加 bytecode starter，并在应用主类之前传入其发布的 Agent JAR：

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-bytecode-starter</artifactId>
</dependency>
```

```bash
java "-javaagent:/opt/egon/egon-cola-component-bytecode-agent-5.2.3.jar=enabled=true,features=method-extension,include=com.example.*" \
  -jar application.jar
```

bytecode starter 将 Method Extension 支持声明为可选依赖，因此使用方仍必须显式声明本 Method Extension starter。当选择 `engine: AGENT`，但活动 Agent 未声明 `METHOD_EXTENSION` 能力时，Spring 启动会失败。`not-ready-policy` 仅在 Spring 完成运行时适配器初始化之前生效：`PROCEED` 允许执行原方法体，`REJECT` 在不调用 Handler 的情况下返回 `null`，`FAIL` 抛出启动就绪状态错误。

对于拒绝结果，直接同步值沿用现有响应规则。返回类型为 `Future`、`CompletionStage` 和 `CompletableFuture` 时会收到一个已完成的拒绝值；允许执行的调用保留原异步对象的身份。不适配任意具体 `Future` 实现和响应式返回类型。Agent 事件只公开有界的方法/Handler 标识和结果；绝不会发布参数、返回载荷、凭据或异常消息。
