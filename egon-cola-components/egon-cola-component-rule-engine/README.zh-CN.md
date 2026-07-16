# Egon COLA Rule Engine Component

[English](README.md) | 中文

## 简要介绍

`egon-cola-component-rule-engine` 是 Egon COLA 的轻量级 Java 规则编排 starter。它不提供规则后台、表达式语言或远程拓扑配置，而是让业务在 Java 代码中组装规则链、单例责任链和规则树，用统一执行器获得步骤控制、超时控制、执行轨迹、监听器和异步加载能力。

组件适合订单预校验、登录校验、会员权益路由、风控预判、灰度入口前置判断等规则数量有限、变化点明确、希望保留 Java 类型安全和可测试性的业务场景。

## 模块结构

| Module | 说明 |
|---|---|
| `egon-cola-component-rule-engine-starter` | Spring Boot starter，提供规则链、规则树、执行器、上下文、结果模型、轨迹、监听器、异步加载和自动配置 |
| `egon-cola-component-rule-engine-test` | 链式规则、单例责任链、规则树和自动配置样例验证 |

## 功能说明

### 规则链

`RuleChain<T, R>` 是线性编排模型，适合一组顺序执行的校验或转换。每个 `ChainHandler<T, R>` 返回 `RuleResult<R>`：

| 返回结果 | 行为 |
|---|---|
| `RuleResult.success(data)` | 当前节点成功，继续执行下一个 handler |
| `RuleResult.stop(code, message, data)` | 当前节点主动停止，返回停止结果 |
| `RuleResult.fail(code, message, exception)` | 当前节点失败，返回失败结果 |

### 单例责任链

`AbstractSingletonRuleLink<T, R>` 支持通过 `appendNext` 组装单例链路，适合把每个规则节点作为独立类长期复用。它采用责任链模式：当前节点成功且上下文未停止时才进入下一个节点。

### 规则树

`RuleTree<T, R>` 是基于 `RuleNode<T, R>` 和 `RouteDecision` 的路由模型，适合根据当前节点结果动态跳转到不同节点。节点可以返回：

| RouteDecision | 行为 |
|---|---|
| `RouteDecision.toCode("nodeCode")` | 跳转到树中指定 code 的节点 |
| `RouteDecision.toNode(node, reason)` | 跳转到直接指定的节点 |
| `RouteDecision.end(data)` | 结束规则树并返回数据 |
| `RouteDecision.noRoute(reason)` | 没有可用路由 |

### 上下文、轨迹和监听器

`RuleContext` 保存 requestId、traceId、执行路径、错误列表、自定义属性、最大步数和超时时间。开启 trace 后，执行结果会携带 `RuleTrace` 和 `NodeTrace`。业务可以注册 `RuleExecutionListener` Bean 监听引擎、节点、路由、停止、超时和异常事件。

### 异步加载

`RuleAsyncExecutor` 用于在规则执行期间加载外部数据，并把结果写入 `RuleContext`。默认实现为 `DefaultRuleAsyncExecutor`，线程池大小由配置控制。

## 依赖方式

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
        <artifactId>egon-cola-component-rule-engine-starter</artifactId>
    </dependency>
</dependencies>
```

## 配置说明

配置前缀为 `egon.cola.component.rule-engine`：

```yaml
egon:
  cola:
    component:
      rule-engine:
        enabled: true
        default-max-steps: 100
        default-timeout-millis: 3000
        async-core-pool-size: 4
        async-max-pool-size: 16
        trace-enabled: true
        listener-error-ignore: true
        throw-exception: false
```

| 配置 | 默认值 | 说明 |
|---|---:|---|
| `enabled` | `true` | 是否启用自动配置 |
| `default-max-steps` | `100` | 默认最大执行步数 |
| `default-timeout-millis` | `3000` | 默认超时时间 |
| `async-core-pool-size` | `4` | 异步加载核心线程数 |
| `async-max-pool-size` | `16` | 异步加载最大线程数 |
| `trace-enabled` | `true` | 是否记录执行轨迹 |
| `listener-error-ignore` | `true` | 监听器异常是否忽略 |
| `throw-exception` | `false` | 执行失败时是否直接抛异常 |

## 完整的使用示例

### 1. 在 Spring Boot 中注入 RuleEngine

```java
package demo.order;

import org.springframework.stereotype.Service;
import top.egon.cola.component.ruleengine.chain.RuleChain;
import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.engine.RuleEngine;
import top.egon.cola.component.ruleengine.result.RuleResult;

@Service
public class OrderRuleService {

    private final RuleEngine ruleEngine;

    public OrderRuleService(RuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
    }

    public RuleResult<String> preCheck(OrderRequest request) {
        RuleChain<OrderRequest, String> chain = RuleChain.<OrderRequest, String>builder("order-pre-check")
                .name("订单提交前置校验")
                .handler((order, context) -> {
                    context.set("paramChecked", order.orderId() != null);
                    return order.orderId() == null
                            ? RuleResult.stop(600101, "orderId required", "blocked")
                            : RuleResult.success(null);
                })
                .handler((order, context) -> order.stock() > 0
                        ? RuleResult.success("allowed")
                        : RuleResult.stop(600201, "stock unavailable", "blocked"))
                .maxSteps(10)
                .timeoutMillis(1000)
                .build();

        RuleContext context = RuleContext.create("req-001", "trace-001");
        return ruleEngine.executeChain(chain, request, context);
    }

    public record OrderRequest(String orderId, int stock) {
    }
}
```

### 2. 单例责任链示例

```java
package demo.login;

import top.egon.cola.component.ruleengine.chain.AbstractSingletonRuleLink;
import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.result.RuleResult;

public class LoginRuleSample {

    public RuleResult<String> check(LoginRequest request) {
        AccountCheck accountCheck = new AccountCheck();
        PasswordCheck passwordCheck = new PasswordCheck();
        StatusCheck statusCheck = new StatusCheck();
        accountCheck.appendNext(passwordCheck).appendNext(statusCheck);
        return accountCheck.handle(request, RuleContext.create());
    }

    static final class AccountCheck extends AbstractSingletonRuleLink<LoginRequest, String> {
        @Override
        protected RuleResult<String> apply(LoginRequest request, RuleContext context) {
            return request.account() == null || request.account().isBlank()
                    ? RuleResult.stop(600301, "account required", "login-blocked")
                    : RuleResult.success(null);
        }
    }

    static final class PasswordCheck extends AbstractSingletonRuleLink<LoginRequest, String> {
        @Override
        protected RuleResult<String> apply(LoginRequest request, RuleContext context) {
            return request.password() == null || request.password().isBlank()
                    ? RuleResult.stop(600302, "password required", "login-blocked")
                    : RuleResult.success(null);
        }
    }

    static final class StatusCheck extends AbstractSingletonRuleLink<LoginRequest, String> {
        @Override
        protected RuleResult<String> apply(LoginRequest request, RuleContext context) {
            return request.active()
                    ? RuleResult.success("login-allowed")
                    : RuleResult.stop(600303, "account disabled", "login-blocked");
        }
    }

    public record LoginRequest(String account, String password, boolean active) {
    }
}
```

### 3. 规则树示例

```java
package demo.member;

import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.engine.RuleEngine;
import top.egon.cola.component.ruleengine.result.RuleResult;
import top.egon.cola.component.ruleengine.tree.NodeType;
import top.egon.cola.component.ruleengine.tree.RouteDecision;
import top.egon.cola.component.ruleengine.tree.RuleNode;
import top.egon.cola.component.ruleengine.tree.RuleTree;

public class MemberBenefitRuleService {

    private final RuleEngine ruleEngine;

    public MemberBenefitRuleService(RuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
    }

    public RuleResult<String> route(MemberRequest request) {
        RuleNode<MemberRequest, String> root = new StaticNode("root", NodeType.ROOT, RouteDecision.toCode("account"));
        RuleNode<MemberRequest, String> account = new StaticNode("account", NodeType.BIZ, RouteDecision.toCode("level"));
        RuleNode<MemberRequest, String> level = new StaticNode("level", NodeType.SWITCH, RouteDecision.toCode("coupon"));
        RuleNode<MemberRequest, String> coupon = new StaticNode("coupon", NodeType.BIZ, RouteDecision.end("coupon-granted"));

        RuleTree<MemberRequest, String> tree = RuleTree.<MemberRequest, String>builder("member-benefit", root)
                .node(account)
                .node(level)
                .node(coupon)
                .maxSteps(10)
                .timeoutMillis(1000)
                .build();

        return ruleEngine.executeTree(tree, request, RuleContext.create().maxSteps(10));
    }

    private record StaticNode(String code, NodeType type, RouteDecision decision)
            implements RuleNode<MemberRequest, String> {

        @Override
        public String name() {
            return code;
        }

        @Override
        public RuleResult<String> execute(MemberRequest request, RuleContext context) {
            return RuleResult.success(null);
        }

        @Override
        public RouteDecision route(MemberRequest request, RuleContext context) {
            return decision;
        }
    }

    public record MemberRequest(String userId) {
    }
}
```

### 4. 注册监听器

```java
package demo.rule;

import org.springframework.stereotype.Component;
import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.listener.RuleExecutionListener;
import top.egon.cola.component.ruleengine.result.RuleResult;

@Component
public class RuleAuditListener implements RuleExecutionListener {

    @Override
    public void afterEngineExecute(String modelType, String ruleCode, RuleContext context, RuleResult<?> result) {
        context.set("lastRuleStatus", result.getStatus().name());
    }
}
```

## 设计思想和实现细节

### 设计思想

1. 规则使用 Java 代码组装，保留类型安全、IDE 重构能力和单元测试可见性。
2. 线性规则使用规则链，动态跳转使用规则树，单例规则类使用责任链，按复杂度选择最直接的模型。
3. 执行器只负责编排、轨迹、超时、步数和监听，不侵入具体业务判断。
4. `RuleContext` 是规则之间共享数据的唯一上下文，避免使用全局状态。
5. 监听器和异步加载是扩展点，不要求每个规则都继承框架基类。

### 实现细节

- `RuleEngineAutoConfiguration` 通过 `AutoConfiguration.imports` 注册，`enabled` 缺省为 `true`。
- `DefaultRuleEngine` 组合 `RuleChainExecutor` 和 `RuleTreeExecutor`，分别处理链式和树式规则。
- `DefaultRuleChainExecutor` 按 handler 顺序执行，遇到 stop/fail、超时或超过最大步数时结束。
- `DefaultRuleTreeExecutor` 从 root 开始执行节点，再根据 `RouteDecision` 选择下一个节点，支持无路由、结束节点、最大步数和超时保护。
- `RuleResult` 是统一结果模型，包含 `success`、`status`、`code`、`message`、`data`、`trace`、`exception`、`stoppedNode`、`hitNode` 和 `costMillis`。
- `RuleExecutionListenerComposite` 会按 Spring order 排序所有监听器，并根据 `listener-error-ignore` 决定监听器异常是否影响主流程。
- `DefaultRuleAsyncExecutor` 作为独立线程池 Bean 注册，销毁方法为 `shutdown`。

## 边界和注意事项

- V1 规则拓扑由 Java 代码组装，不支持 YAML、JSON、数据库拓扑、远程配置和热更新。
- 不包含 UI 管理、租户绑定、权限绑定、灰度绑定和表达式引擎。
- `maxSteps` 是防止规则树环路和规则链异常扩张的保护，复杂规则树应显式设置合理值。
- `throw-exception=false` 时执行异常会转成失败结果；需要上抛异常时再开启。
- 规则节点和 handler 应保持无共享可变状态，跨节点共享数据放入 `RuleContext`。

## 验证命令

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter,egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-test -am test
```
