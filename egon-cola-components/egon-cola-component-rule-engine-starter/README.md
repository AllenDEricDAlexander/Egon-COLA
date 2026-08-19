# Egon COLA Rule Engine Starter

[English](README.md) | [中文](README.zh-CN.md)

## Overview

`egon-cola-component-rule-engine-starter` is a lightweight Java rule orchestration starter for Egon COLA. It does not provide a rule-management backend, an expression language, or remote topology configuration. Instead, it lets business code assemble rule chains, singleton chains of responsibility, and rule trees in Java, while unified executors provide step limits, timeout control, execution traces, listeners, and asynchronous loading.

The component suits scenarios with a limited number of rules and explicit variation points where Java type safety and testability should be preserved, such as order pre-validation, login checks, member-benefit routing, preliminary risk control, and checks before canary entry points.

## Module and Test Layout

| Module | Description |
|---|---|
| `egon-cola-component-rule-engine-starter` | Spring Boot starter and its sample/unit tests for rule chains, rule trees, executors, context, result models, traces, listeners, asynchronous loading, and auto-configuration |

## Features

### Rule Chains

`RuleChain<T, R>` is a linear orchestration model for validations or transformations that run in sequence. Each `ChainHandler<T, R>` returns a `RuleResult<R>`:

| Return Value | Behavior |
|---|---|
| `RuleResult.success(data)` | The current node succeeds and execution continues to the next handler |
| `RuleResult.stop(code, message, data)` | The current node stops execution intentionally and returns the stop result |
| `RuleResult.fail(code, message, exception)` | The current node fails and returns the failure result |

### Singleton Chain of Responsibility

`AbstractSingletonRuleLink<T, R>` supports assembling singleton links through `appendNext`, making each reusable rule node a long-lived independent class. It follows the Chain of Responsibility pattern: the next node runs only when the current node succeeds and the context has not stopped.

### Rule Trees

`RuleTree<T, R>` is a routing model based on `RuleNode<T, R>` and `RouteDecision`. It is suitable when the result of the current node dynamically selects the next node. A node can return:

| RouteDecision | Behavior |
|---|---|
| `RouteDecision.toCode("nodeCode")` | Route to the node with the specified code in the tree |
| `RouteDecision.toNode(node, reason)` | Route directly to the specified node |
| `RouteDecision.end(data)` | End the rule tree and return data |
| `RouteDecision.noRoute(reason)` | Indicate that no route is available |

### Context, Traces, and Listeners

`RuleContext` stores the requestId, traceId, execution path, error list, custom attributes, maximum step count, and timeout. When tracing is enabled, the execution result includes `RuleTrace` and `NodeTrace`. Applications can register `RuleExecutionListener` Beans to observe engine, node, routing, stop, timeout, and exception events.

### Asynchronous Loading

`RuleAsyncExecutor` loads external data during rule execution and writes the result into `RuleContext`. Its default implementation is `DefaultRuleAsyncExecutor`, and configuration controls the thread-pool size.

## Dependency Setup

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

## Configuration

The configuration prefix is `egon.cola.component.rule-engine`:

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

| Property | Default | Description |
|---|---:|---|
| `enabled` | `true` | Whether auto-configuration is enabled |
| `default-max-steps` | `100` | Default maximum execution steps |
| `default-timeout-millis` | `3000` | Default timeout |
| `async-core-pool-size` | `4` | Core thread count for asynchronous loading |
| `async-max-pool-size` | `16` | Maximum thread count for asynchronous loading |
| `trace-enabled` | `true` | Whether to record execution traces |
| `listener-error-ignore` | `true` | Whether listener exceptions are ignored |
| `throw-exception` | `false` | Whether execution failures are thrown directly |

## Complete Usage Examples

### 1. Inject RuleEngine in Spring Boot

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
                .name("Order submission pre-check")
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

### 2. Singleton Chain of Responsibility Example

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

### 3. Rule Tree Example

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

### 4. Register a Listener

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

## Design Principles and Implementation Details

### Design Principles

1. Assemble rules in Java to retain type safety, IDE refactoring support, and unit-test visibility.
2. Use rule chains for linear rules, rule trees for dynamic routing, and a chain of responsibility for singleton rule classes, selecting the most direct model for the complexity involved.
3. Keep executors responsible only for orchestration, tracing, timeout, step limits, and listeners without intruding into business decisions.
4. Use `RuleContext` as the sole shared context across rules and avoid global state.
5. Treat listeners and asynchronous loading as extension points without requiring every rule to inherit a framework base class.

### Implementation Details

- `RuleEngineAutoConfiguration` is registered through `AutoConfiguration.imports`, and `enabled` defaults to `true`.
- `DefaultRuleEngine` combines `RuleChainExecutor` and `RuleTreeExecutor` for chained and tree-based rules respectively.
- `DefaultRuleChainExecutor` runs handlers in order and stops on a stop/fail result, timeout, or maximum-step violation.
- `DefaultRuleTreeExecutor` starts at the root and selects the next node from `RouteDecision`, with support for no-route and end nodes plus maximum-step and timeout protection.
- `RuleResult` is the unified result model. It includes `success`, `status`, `code`, `message`, `data`, `trace`, `exception`, `stoppedNode`, `hitNode`, and `costMillis`.
- `RuleExecutionListenerComposite` orders all listeners by Spring order and uses `listener-error-ignore` to determine whether a listener exception affects the primary flow.
- `DefaultRuleAsyncExecutor` is registered as a dedicated thread-pool Bean with `shutdown` as its destroy method.

## Boundaries and Operational Notes

- V1 rule topologies are assembled in Java. YAML, JSON, database topologies, remote configuration, and hot updates are not supported.
- The component does not include UI management, tenant binding, permission binding, canary binding, or an expression engine.
- `maxSteps` protects against rule-tree loops and abnormal rule-chain growth. Complex rule trees should set an explicit, reasonable value.
- When `throw-exception=false`, execution exceptions are converted into failure results. Enable it only when exceptions must propagate.
- Rule nodes and handlers should have no shared mutable state. Place data shared across nodes in `RuleContext`.

## Validation Command

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-rule-engine-starter -am test
```
