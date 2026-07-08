# Egon COLA Rule Engine Component

This component provides a lightweight Spring Boot Starter for Java-assembled business rule orchestration.

## Modules

| Module | Purpose |
|---|---|
| `egon-cola-component-rule-engine-starter` | Business application starter, rule engine API, chain executor, tree executor, trace, listener, and async loading. |
| `egon-cola-component-rule-engine-test` | Sample and validation module for chain, singleton chain, rule tree, and auto-configuration flows. |

## Starter Dependency

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-rule-engine-starter</artifactId>
    <version>5.2.0-SNAPSHOT</version>
</dependency>
```

## Configuration

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

## Boundaries

Rules are assembled with Java code. YAML, JSON, database topology, remote configuration, UI management, hot update, tenant binding, permission binding, grayscale binding, and expression engines are outside V1.0.

## Validation

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter,egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-test -am test
```
