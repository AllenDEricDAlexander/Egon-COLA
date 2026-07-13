# Method Extension Component Design

## 1. Context

The source requirement describes a Spring Boot middleware that intercepts an
annotated method, runs a business-defined precondition, and either proceeds with
the original invocation or returns a configured rejection response.

The original proposal locates the precondition as another method on the same
Spring Bean and invokes it reflectively by name. Egon-COLA will keep the
pre-invocation decision semantics but replace string-based reflective lookup with
a typed Spring Bean extension contract.

This capability is independent of Access Guard. Applications must be able to use
it without bringing in access-control policies, Redisson, Spring Web, a database,
or another Egon-COLA runtime component. It also does not belong in
`egon-cola-component-common`, whose modules are Spring-independent foundational
Jars rather than auto-configured runtime middleware.

## 2. Confirmed Decisions

1. Create a new `egon-cola-component-method-extension` component.
2. Use the repository-standard `root + starter + test` structure.
3. Export only the starter through `egon-cola-components-bom`.
4. Do not add the feature to Access Guard or common.
5. Use `@MethodExtension` as the only public annotation; do not provide a
   `@DoMethodExt` compatibility alias.
6. Select a typed `MethodExtensionHandler` Spring Bean from the annotation.
7. Have handlers return `MethodExtensionDecision`, not a boolean or an exception
   to represent ordinary rejection.
8. Propagate handler failures and do not execute the original method after a
   handler failure.
9. Follow the repository baseline of Java 21 and Spring Boot 3.5.x; Spring Boot
   2.x compatibility is out of scope.
10. Do not start an application or open a browser for validation.

## 3. Goals

The component will provide:

1. method-level Spring AOP interception through `@MethodExtension`;
2. one reusable Spring-managed handler per annotated invocation;
3. an invocation context containing the target, resolved method, and arguments;
4. an explicit allow/reject decision model;
5. direct rejection responses supplied by a handler;
6. Jackson conversion of an annotation-level `returnJson` fallback;
7. clear configuration, handler, and response errors;
8. configurable enablement and AOP order;
9. focused logs without recording method arguments or response bodies;
10. auto-configuration, documentation, examples, and regression tests.

## 4. Non-Goals

V1 does not include:

1. multiple handlers or an ordered handler chain;
2. asynchronous or reactive handlers;
3. string-based methods on the intercepted Bean;
4. dynamic handler selection through SpEL or configuration-center data;
5. automatic construction of `ResponseEntity`, `CompletableFuture`, Reactor, or
   other framework wrappers from JSON;
6. a Web management service or UI;
7. Redis, database, Flyway, MQ, or remote calls;
8. Access Guard annotations or compatibility adapters;
9. a global exception-to-HTTP/RPC response translator;
10. Spring Boot 2.x support;
11. interception of Spring AOP self-invocation, private methods, static methods,
    or non-proxyable final methods.

## 5. Component Structure

```text
egon-cola-components/
`-- egon-cola-component-method-extension/
    |-- pom.xml
    |-- README.md
    |-- egon-cola-component-method-extension-starter/
    |   |-- pom.xml
    |   `-- src/
    `-- egon-cola-component-method-extension-test/
        |-- pom.xml
        `-- src/
```

The components parent aggregates the component root. The component root
aggregates `starter` and `test`. The BOM exports only:

```text
top.egon:egon-cola-component-method-extension-starter
```

The package root is:

```text
top.egon.cola.component.methodextension
```

The starter package layout is:

```text
top.egon.cola.component.methodextension
|-- annotation
|   `-- MethodExtension
|-- aop
|   `-- MethodExtensionAop
|-- autoconfigure
|   |-- MethodExtensionAutoConfiguration
|   `-- MethodExtensionProperties
|-- context
|   `-- MethodExtensionContext
|-- handler
|   |-- MethodExtensionHandler
|   |-- MethodExtensionDecision
|   `-- MethodExtensionHandlerResolver
|-- response
|   `-- MethodExtensionResponseResolver
|-- support
|   `-- MethodExtensionMethodResolver
`-- exception
    |-- MethodExtensionException
    |-- MethodExtensionConfigurationException
    `-- MethodExtensionResponseException
```

This keeps the AOP orchestration direct. V1 does not introduce an executor,
factory hierarchy, registry abstraction, or private-method call chain that would
only redistribute simple control flow.

## 6. Dependency Boundary

The starter may depend on:

1. `spring-boot-starter`;
2. `spring-boot-autoconfigure`;
3. `spring-boot-starter-aop`;
4. Jackson Databind from the Spring Boot dependency set;
5. `spring-boot-configuration-processor` as optional;
6. SLF4J through the Spring Boot dependency set.

The starter must not depend on:

1. any Egon-COLA common module;
2. Access Guard or another runtime component;
3. Spring Web;
4. Redisson or another Redis client;
5. JPA, JDBC, Flyway, or database drivers;
6. messaging, RPC, or configuration-center clients;
7. the component test module.

The test module depends on the starter and Spring Boot test support only.

## 7. Public API

### 7.1 Annotation

`@MethodExtension` is method-scoped, runtime-retained, and documented.

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MethodExtension {

    Class<? extends MethodExtensionHandler> handler();

    String returnJson() default "";
}
```

The handler is selected by a class literal rather than a Bean name. This gives
compile-time type checking and survives ordinary Bean renaming.

### 7.2 Handler

```java
public interface MethodExtensionHandler {

    MethodExtensionDecision evaluate(MethodExtensionContext context) throws Exception;
}
```

Handlers are ordinary Spring Beans registered with `@Component` or `@Bean`. The
configured handler type must resolve to exactly one Bean. A handler is expected
to be stateless and side-effect-free where practical.

### 7.3 Context

`MethodExtensionContext` is an immutable value object that exposes:

1. the target Bean;
2. the most-specific user method behind the Spring proxy;
3. the original invocation arguments.

The context defensively copies the argument array when it is created and when it
is read. This prevents replacing the invocation's argument array. It cannot and
does not deep-copy mutable argument objects.

### 7.4 Decision

`MethodExtensionDecision` is an immutable value object created through factories:

```java
MethodExtensionDecision.allow()
MethodExtensionDecision.reject()
MethodExtensionDecision.rejectWithReason(String reason)
MethodExtensionDecision.reject(Object response)
MethodExtensionDecision.reject(Object response, String reason)
```

The decision records whether invocation is allowed, whether a direct response is
present, that response, and an optional diagnostic reason. `reject(response)`
rejects `null` so that a direct-null response cannot be confused with the
`returnJson` fallback path.

Example usage:

```java
@Component
public class BlacklistHandler implements MethodExtensionHandler {

    @Override
    public MethodExtensionDecision evaluate(MethodExtensionContext context) {
        String userId = (String) context.arguments()[0];
        return "blocked-user".equals(userId)
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
public UserInfo queryUserInfo(String userId) {
    return userService.query(userId);
}
```

## 8. Design Pattern Choice

The component uses Strategy for the real variation point: each business rule is
a `MethodExtensionHandler`, and the annotation selects one strategy. The AOP
aspect implements the interception seam around the target method.

V1 does not use Chain of Responsibility because exactly one handler runs. It does
not use Template Method because handlers do not share a required algorithm
skeleton. It does not add Factory Method or Abstract Factory; the Spring container
already owns handler construction, while the resolver only enforces unique Bean
selection.

## 9. Auto-Configuration

The starter registers through:

```text
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

Configuration prefix:

```text
egon.cola.component.method-extension
```

Properties:

```yaml
egon:
  cola:
    component:
      method-extension:
        enabled: true
        order: -2147483548
```

Defaults:

1. `enabled=true`;
2. `order=Ordered.HIGHEST_PRECEDENCE + 100`.

When disabled, auto-configuration does not register the aspect, so annotated
methods execute normally without resolving or invoking a handler. The order is
configurable for applications that combine method extension with transactions,
Access Guard, method security, or custom aspects.

The auto-configuration discovers the application's Spring-managed
`ObjectMapper` lazily. A unique application Bean remains authoritative, and the
component does not publish a second mapper or create private JSON settings.

An application that only uses direct handler responses does not need an
`ObjectMapper` Bean. When a rejection actually takes the `returnJson` path,
missing or ambiguous mapper Beans produce a configuration exception. This keeps
the starter independent of Spring Web while making the JSON dependency explicit.

## 10. Method and Handler Resolution

For every intercepted invocation, the component:

1. resolves the most-specific method from the proxy and target class;
2. resolves bridge methods produced for generics;
3. searches the implementation method first and the invoked interface method
   second for `@MethodExtension`;
4. obtains exactly one Spring Bean matching the annotation's handler type;
5. creates `MethodExtensionContext` from the target, method, and arguments.

An implementation-method annotation takes precedence over an interface-method
annotation. Missing and ambiguous handlers are configuration errors. The resolver
must remain compatible with proxied handler Beans rather than assuming that the
runtime Bean class equals the annotation's class literal.

The original arguments are used when the target method proceeds. Handler code
cannot replace that argument array through the context.

## 11. Execution Flow

```text
annotated Spring proxy method
        |
        v
resolve target method and annotation
        |
        v
resolve exactly one handler Bean
        |
        v
handler.evaluate(context)
        |
        +-- throws ----------------> log and propagate; target does not run
        |
        +-- returns null ----------> configuration error; target does not run
        |
        +-- allow -----------------> invoke target once and return its result
        |
        `-- reject ----------------> skip target and resolve rejection response
```

Handlers run synchronously and exactly once per intercepted call. V1 does not
cache handler instances outside the Spring container, so normal Spring Bean scope
semantics remain under container control.

## 12. Rejection Response Rules

Response priority is:

1. the direct object carried by `MethodExtensionDecision.reject(response)`;
2. the annotation's non-blank `returnJson`;
3. `null` only for a `void` or `Void` target method;
4. otherwise, a configuration exception.

### 12.1 Direct Responses

A direct response must be assignable to the target method's declared return type.
Primitive declarations accept their matching boxed values. Runtime validation can
verify only the raw class of parameterized types; Java erasure prevents checking
the generic contents of a handler-supplied object.

For a `void` or `Void` target, the handler must use `reject()` and the annotation
must leave `returnJson` blank.

### 12.2 JSON Responses

JSON conversion uses the application's `ObjectMapper` and the method's generic
return type. This supports resolvable shapes such as `List<UserInfo>` and
`ResultDto<UserInfo>`, subject to the application's Jackson configuration.

Special rules:

1. a `String` target receives the `returnJson` text unchanged;
2. a `void` or `Void` target may not configure `returnJson`;
3. unresolved type variables or failed Jackson conversion produce a response
   exception;
4. V1 does not construct framework wrappers from JSON;
5. a handler may still directly provide a compatible wrapper instance through
   `reject(response)`.

## 13. Error Handling

The exception hierarchy is intentionally small:

```text
MethodExtensionException
|-- MethodExtensionConfigurationException
`-- MethodExtensionResponseException
```

`MethodExtensionConfigurationException` covers:

1. missing or ambiguous handler Beans;
2. a `null` decision;
3. a missing or ambiguous `ObjectMapper` when `returnJson` must be converted;
4. rejection without a usable response for a non-void target;
5. `returnJson` configured for a void target;
6. other invalid component contract states.

`MethodExtensionResponseException` covers:

1. a handler response incompatible with the target return type;
2. malformed JSON;
3. JSON incompatible with the target generic return type.

Exceptions thrown by business handlers are logged and propagated unchanged. The
component does not wrap them, translate them into rejection decisions, or proceed
with the original invocation.

## 14. Logging

Logging levels:

| Event | Level |
|---|---|
| annotation matched | DEBUG |
| handler about to execute | DEBUG |
| handler allowed invocation | DEBUG |
| handler rejected invocation | INFO |
| invalid configuration or response | ERROR |
| handler failure | ERROR |

Logs include the target class, method signature, handler type, and optional
diagnostic reason. They do not include argument values, direct response objects,
or the complete `returnJson` text. Handler authors must keep diagnostic reasons
free of sensitive business data.

## 15. Spring AOP Boundaries

The component intercepts annotated public methods on Spring-managed Beans. It
supports JDK and CGLIB proxies, interface annotations, implementation annotations,
and inherited methods through most-specific method resolution.

Standard Spring Proxy AOP limitations remain explicit:

1. `this.someAnnotatedMethod()` self-invocation is not intercepted;
2. private and static methods are not intercepted;
3. final methods that cannot be proxied are not supported;
4. calls on objects created outside the Spring container are not intercepted.

The README and tests must state these boundaries rather than implying that the
starter performs bytecode weaving.

## 16. Testing Strategy

### 16.1 Starter Unit Tests

Cover:

1. decision factories and illegal `null` direct responses;
2. context argument-array defensive copies;
3. unique, missing, ambiguous, and proxied handler resolution;
4. direct response compatibility, including primitive boxing;
5. missing, ambiguous, custom, and unique `ObjectMapper` behavior;
6. JSON objects, resolvable generic types, raw strings, and void behavior;
7. malformed JSON and incompatible responses;
8. unchanged propagation of handler exceptions.

### 16.2 AOP Integration Tests

Cover:

1. `allow()` invokes the target exactly once and returns its result;
2. `reject(response)` skips the target and returns the direct response;
3. `reject()` skips the target and converts `returnJson`;
4. `enabled=false` bypasses handler resolution and invocation;
5. configured AOP order is applied;
6. JDK and CGLIB proxy method resolution;
7. interface, implementation, bridge, and inherited method annotations;
8. self-invocation remains un-intercepted as a documented limitation;
9. handler `null` decisions and failures never execute the target.

### 16.3 Test Module Samples

The test module provides black-list, gray-release, and temporary-validation
handlers. Samples demonstrate direct object rejection, annotation JSON fallback,
normal allow flow, and handler failure. Tests use Spring test contexts only; they
do not start a service or require external infrastructure.

## 17. Documentation

The component root `README.md` will document:

1. Maven dependency and BOM usage;
2. configuration properties;
3. the annotation and handler contract;
4. allow and both rejection paths;
5. handler Bean requirements;
6. return-type rules;
7. error behavior and logging;
8. Spring Proxy AOP limitations;
9. runnable test examples.

No component-local `docs/` directory is added. This repository-level design file
remains the architecture source for V1.

## 18. Acceptance Criteria

The design is complete when implementation proves:

1. annotated public Spring Bean methods are intercepted;
2. unannotated methods are unaffected;
3. exactly one configured handler Bean is executed;
4. allow executes the original method exactly once;
5. reject never executes the original method;
6. direct and JSON rejection responses follow the defined priority;
7. generic JSON conversion uses the method's generic return type;
8. invalid handlers, decisions, JSON, and response types fail clearly;
9. handler exceptions propagate and do not result in fail-open behavior;
10. disabling the component bypasses interception;
11. proxy behavior and limitations match the documentation;
12. the parent reactor and BOM expose only the intended starter surface;
13. the starter has no Access Guard, Redis, Web, database, Flyway, MQ, or remote
    configuration dependency.

## 19. Validation Plan

Focused starter and test modules:

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter,egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-test \
  -am test
```

Components reactor:

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml test
```

Dependency boundary:

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter \
  -am dependency:tree
```

The dependency output must be checked for unintended Access Guard, Redisson,
Spring Web, Flyway, database, MQ, and configuration-center artifacts.

Repository hygiene:

```bash
git diff --check
```
