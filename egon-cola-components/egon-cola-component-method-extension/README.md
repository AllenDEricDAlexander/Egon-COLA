# Egon-COLA Method Extension

`egon-cola-component-method-extension` is a lightweight Spring Boot Starter for
running one business-defined decision handler before an annotated method.

## Modules

| Module | Purpose |
|---|---|
| `egon-cola-component-method-extension-starter` | Annotation, handler contract, AOP, response conversion, and auto-configuration |
| `egon-cola-component-method-extension-test` | Black-list, gray-release, and temporary-validation samples |

## Dependency

Import `top.egon:egon-cola-components-bom:5.2.2`, then add:

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
        order: -2147483548
```

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
