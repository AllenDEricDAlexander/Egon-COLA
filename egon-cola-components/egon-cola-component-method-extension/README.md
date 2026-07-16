# Egon-COLA Method Extension

`egon-cola-component-method-extension` is a lightweight Spring Boot Starter for
running one business-defined decision handler before an annotated method.

## Modules

| Module | Purpose |
|---|---|
| `egon-cola-component-method-extension-starter` | Annotation, handler contract, AOP, response conversion, and auto-configuration |
| `egon-cola-component-method-extension-test` | Black-list, gray-release, and temporary-validation samples |

## Dependency

Import `top.egon:egon-cola-components-bom:5.2.3`, then add:

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-method-extension-starter</artifactId>
</dependency>
```

## Configuration

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

`engine` accepts `AOP`, `AGENT`, or `DISABLED`. A missing value preserves the
existing AOP behavior. `enabled=false` always disables both engines.

| Engine | Supported methods | Runtime boundary |
|---|---|---|
| `AOP` | Public methods reachable through Spring AOP proxies | Existing default; no Java Agent required |
| `AGENT` | Concrete instance methods of every visibility, including private self-calls, final, synchronized, interface-declared annotations, and non-Spring objects | Requires the bytecode starter and a premain Agent with `features=method-extension` |
| `DISABLED` | None | Registers neither AOP nor Agent integration |

Agent mode excludes static methods, constructors, abstract methods, native methods,
synthetic methods, and bridge methods. Generated JDK and Spring CGLIB proxy classes
are excluded so only the concrete target is enhanced and each Handler runs once.

## Handler

Register every handler as a Spring Bean. Return `allow()` to invoke the original
method, `reject(response)` to return a direct object, or `reject()` to use the
annotation's `returnJson`.

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

## Response Rules

1. A direct handler response has first priority and must match the method return type.
2. A non-blank `returnJson` is second priority.
3. `String` methods receive `returnJson` unchanged.
4. Object and generic JSON conversion requires exactly one application `ObjectMapper` Bean.
5. `void` methods reject with `null` and must not configure `returnJson`.
6. Missing responses and incompatible types produce explicit component exceptions.

## Error Behavior

Missing or ambiguous handlers, null decisions, and invalid response configuration
raise `MethodExtensionConfigurationException`. JSON and response type failures
raise `MethodExtensionResponseException`. Exceptions from a handler are logged
and propagated unchanged; the original method is never executed after a handler
failure.

## Spring AOP Limits

Only public methods on Spring-managed proxy Beans are intercepted. Self-invocation
through `this`, private methods, static methods, and non-proxyable final methods
are not supported. V1 executes one synchronous handler and does not provide a
handler chain, reactive adapter, Web layer, Redis integration, or database state.

## Agent Installation

The Method Extension starter remains standalone and does not depend on the bytecode
component. To use `engine: AGENT`, add the bytecode starter separately and pass its
published Agent JAR before the application main class:

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

The bytecode starter declares Method Extension support as optional, so consumers
must still declare this Method Extension starter explicitly. Spring startup fails
when `engine: AGENT` is selected but the active Agent does not advertise the
`METHOD_EXTENSION` capability. `not-ready-policy` applies only before Spring finishes
initializing the runtime adapter: `PROCEED` allows the original body, `REJECT` returns
`null` without invoking a Handler, and `FAIL` throws a startup-readiness error.

For rejections, direct synchronous values preserve the existing response rules.
`Future`, `CompletionStage`, and `CompletableFuture` return types receive a completed
rejection value; allowed calls retain the original asynchronous object identity.
Arbitrary concrete `Future` implementations and reactive return types are not
adapted. Agent events expose bounded method/Handler identities and outcomes only;
arguments, return payloads, credentials, and exception messages are never published.
