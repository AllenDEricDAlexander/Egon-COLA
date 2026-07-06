# egon-cola-component-dynamic-thread-pool

Dynamic thread pool component for Egon COLA.

## Modules

| Module | Description |
| --- | --- |
| `egon-cola-component-dynamic-thread-pool-starter` | Spring Boot starter for dynamic thread pool registration, configuration adjustment, reporting, and metrics binding. |

## Starter

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-dynamic-thread-pool-starter</artifactId>
    <version>5.2.0-SNAPSHOT</version>
</dependency>
```

Configuration prefix:

```properties
egon.cola.component.dtp.enabled=true
egon.cola.component.dtp.app-name=student-management
egon.cola.component.dtp.registry.redis.host=127.0.0.1
```
