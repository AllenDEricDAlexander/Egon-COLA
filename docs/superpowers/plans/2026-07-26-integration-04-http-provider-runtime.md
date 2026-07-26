# Integration 04 HTTP Provider Runtime Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 Spring MVC 与 Spring WebFlux Provider 通过同一依赖和 YAML 自动完成接口上报、DDC 注册、心跳、恢复、健康检查与注销。

**Architecture:** Boot binding properties 负责解析并校验外部配置，AutoConfiguration 把 Reporting identity、DDC Registry 与实际 WebServer port 组合为现有 `HttpProviderLeaseRuntime`。Servlet/Reactive 共享运行时，框架差异只存在于 Starter 的 HandlerMapping contributor。

**Tech Stack:** Spring Boot AutoConfiguration、Servlet WebServer、Reactive WebServer、Actuator、Spring MVC、Spring WebFlux、Reactor Mono、JUnit 5。

## Global Constraints

- 依赖 Integration 01 状态和 Redis 基线。
- 保留现有 `HttpProviderRuntimeProperties` 作为不可变运行时值。
- 非 local/test 必须显式配置 advertised host。
- MVC 与 WebFlux 上报 artifact version、注册 service version 必须一致。
- 不实现 RouterFunction、SSE 或 Flux streaming。

---

### Task 1: 增加 HTTP Provider Boot 配置属性

**Files:**
- Create: `.../gateway-provider-runtime/src/main/java/top/egon/cola/component/gateway/provider/GatewayHttpProviderProperties.java`
- Modify: `.../gateway-provider-runtime/pom.xml`
- Test: `.../gateway-provider-runtime/src/test/java/top/egon/cola/component/gateway/provider/GatewayHttpProviderPropertiesTest.java`

**Interfaces:**
- Produces prefix `egon.cola.component.gateway.provider.http`.
- Produces `HttpProviderRuntimeProperties toRuntime(GatewayDefinitionIdentity, int actualPort)`.

- [ ] **Step 1: Write failing binding and validation tests**

```java
new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(BindingConfiguration.class))
        .withPropertyValues(
                "egon.cola.component.gateway.provider.http.enabled=true",
                "egon.cola.component.gateway.provider.http.service-name=orders",
                "egon.cola.component.gateway.provider.http.advertised-host=127.0.0.1",
                "egon.cola.component.gateway.provider.http.port=0",
                "egon.cola.component.gateway.provider.http.env=test"
        )
        .run(context -> assertThat(context.getBean(GatewayHttpProviderProperties.class)
                .getServiceName()).isEqualTo("orders"));
```

Add production loopback, heartbeat>=lease, invalid protocol and version/report identity conflict cases.

- [ ] **Step 2: Run provider-runtime tests**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-gateway/\
egon-cola-component-gateway-provider-runtime -am test \
  -Dtest=GatewayHttpProviderPropertiesTest,HttpProviderLeaseRuntimeTest
```

- [ ] **Step 3: Implement explicit Boot binding**

The bean exposes enabled/env/namespace/instanceId/serviceName/group/version/protocol/advertisedHost/port,
leaseSeconds, heartbeatIntervalSeconds, failFast and metadata. Defaults may come from Reporting/DDC only inside
AutoConfiguration, not by silently guessing in the value object.

Add explicit compile dependencies on `spring-boot-autoconfigure`, optional configuration processor and optional
Actuator; do not rely on accidental transitive Boot types.

- [ ] **Step 4: Run focused tests**

Expected: PASS and configuration metadata is generated in `target/classes/META-INF`.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-provider-runtime
git commit -m "feat: bind gateway http provider properties"
```

### Task 2: 自动绑定 WebServer 生命周期与健康状态

**Files:**
- Create: `.../gateway-provider-runtime/src/main/java/top/egon/cola/component/gateway/provider/GatewayHttpProviderAutoConfiguration.java`
- Create: `.../gateway-provider-runtime/src/main/java/top/egon/cola/component/gateway/provider/GatewayHttpProviderHealthIndicator.java`
- Create: `.../gateway-provider-runtime/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Modify: `.../gateway-provider-runtime/src/main/java/top/egon/cola/component/gateway/provider/HttpProviderLeaseRuntime.java`
- Test: `.../gateway-provider-runtime/src/test/java/top/egon/cola/component/gateway/provider/GatewayHttpProviderAutoConfigurationTest.java`

**Interfaces:**
- Produces one `HttpProviderLeaseRuntime` when Registry and enabled properties exist.
- Consumes `WebServerInitializedEvent` for both Servlet and Reactive contexts.
- Produces health details `state`, `instanceId`, `leaseId`, `leaseExpireAt` without secrets.

- [ ] **Step 1: Write failing auto-configuration tests**

```java
runner.withBean(DdcServiceRegistryClient.class, () -> registry)
        .withBean(GatewayDefinitionIdentity.class,
                () -> new GatewayDefinitionIdentity("set-1", "1.0.0", "build-1"))
        .withPropertyValues(requiredProperties())
        .run(context -> {
            assertThat(context).hasSingleBean(HttpProviderLeaseRuntime.class);
            context.publishEvent(webServerEvent(18101));
            assertThat(registry.registration().port()).isEqualTo(18101);
            assertThat(context.getBean(GatewayHttpProviderHealthIndicator.class)
                    .health().getStatus()).isEqualTo(Status.UP);
        });
```

Add disabled, missing Registry, fail-fast failure, non-fail-fast RECOVERING→REGISTERED and context close cases.

- [ ] **Step 2: Run auto-configuration tests**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-gateway/\
egon-cola-component-gateway-provider-runtime -am test \
  -Dtest=GatewayHttpProviderAutoConfigurationTest,HttpProviderLeaseRuntimeTest
```

- [ ] **Step 3: Implement one framework-neutral AutoConfiguration**

```java
@AutoConfiguration(afterName = {
        "top.egon.cola.component.ddc.config.DdcRegistryAutoConfig",
        "top.egon.cola.component.gateway.starter.GatewayReportingAutoConfiguration"
})
@EnableConfigurationProperties(GatewayHttpProviderProperties.class)
@ConditionalOnProperty(prefix = PREFIX, name = "enabled", havingValue = "true")
public class GatewayHttpProviderAutoConfiguration {
    // runtime, event listener and conditional HealthIndicator beans
}
```

Use `ApplicationListener<WebServerInitializedEvent>` and a close callback owned by Spring. Avoid separate MVC
and WebFlux configurations because the event contract is common.

- [ ] **Step 4: Run provider-runtime module and auto-config import contract**

Expected: PASS; `AutoConfiguration.imports` contains exactly the new class.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-provider-runtime
git commit -m "feat: auto-register gateway http providers"
```

### Task 3: 让现有 MVC 应用只使用依赖和 YAML

**Files:**
- Delete: `.../gateway-test-http-provider/src/main/java/top/egon/cola/component/gateway/test/http/HttpProviderRuntimeConfiguration.java`
- Modify: `.../gateway-test-http-provider/src/main/resources/application.yml`
- Modify: `.../gateway-test-http-provider/src/test/java/top/egon/cola/component/gateway/test/http/HttpProviderContractTest.java`

**Interfaces:**
- Consumes `GatewayHttpProviderAutoConfiguration` with no application-private lease beans.
- Produces service identity `gateway-test-http-provider/default/1.0.0-live` consistently in report and DDC.

- [ ] **Step 1: Add a context assertion that no private Configuration is imported**

```java
assertThat(context.getBean(HttpProviderLeaseRuntime.class)).isNotNull();
assertThat(context.containsBean("httpProviderRuntimeConfiguration")).isFalse();
assertThat(runtimeRegistration.serviceKey().version())
        .isEqualTo(report.identity().artifactVersion());
```

- [ ] **Step 2: Delete the private configuration and run the test to see missing YAML**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-gateway/\
egon-cola-component-gateway-test/egon-cola-component-gateway-test-http-provider \
  -am test
```

- [ ] **Step 3: Add complete provider YAML**

```yaml
egon:
  cola:
    component:
      gateway:
        provider:
          http:
            enabled: true
            service-name: gateway-test-http-provider
            group: default
            version: ${gateway.reporting.artifact-version}
            advertised-host: ${gateway.test.advertised-host:127.0.0.1}
            port: ${gateway.test.advertised-port:0}
```

Read `egon.cola.component.gateway.reporting.artifact-version` through
`GatewayReportingProperties.getArtifactVersion()` and use it as the registration version; do not introduce a
second version variable.

- [ ] **Step 4: Run MVC module tests**

Expected: PASS and one DDC registration after server start.

- [ ] **Step 5: Commit**

```bash
git add -A egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-http-provider
git commit -m "test: consume http provider auto-configuration"
```

### Task 4: 新增真实 WebFlux Provider 模块

**Files:**
- Create: `.../gateway-test/egon-cola-component-gateway-test-webflux-http-provider/pom.xml`
- Create: `.../src/main/java/top/egon/cola/component/gateway/test/webflux/GatewayWebFluxHttpTestProviderApplication.java`
- Create: `.../src/main/java/top/egon/cola/component/gateway/test/webflux/ReactiveInventoryController.java`
- Create: `.../src/main/resources/application.yml`
- Create: `.../src/test/java/top/egon/cola/component/gateway/test/webflux/WebFluxHttpProviderContractTest.java`
- Modify: `.../gateway-test/pom.xml`
- Modify: `.../gateway/pom.xml`
- Modify: `.../gateway-test-suite/pom.xml`

**Interfaces:**
- Produces executable `gateway-test-webflux-http-provider.jar`.
- Produces annotated `Mono<InventoryResponse>` endpoints under `/test/items/{id}`.
- Registers the same service identity as MVC with a distinct instanceId/port/zone.

- [ ] **Step 1: Add module and failing WebFlux contract test**

```java
@Test
void reportsAnnotatedMonoEndpointAndAutoRegisters() {
    assertThat(report.operations())
            .anySatisfy(operation -> {
                assertThat(operation.httpMethod()).isEqualTo("GET");
                assertThat(operation.path()).isEqualTo("/test/items/{id}");
                assertThat(operation.responseMode()).isEqualTo("OBJECT");
            });
    assertThat(registry.registration().serviceKey().serviceName())
            .isEqualTo("gateway-test-http-provider");
}
```

- [ ] **Step 2: Run the new module test**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-gateway/\
egon-cola-component-gateway-test/\
egon-cola-component-gateway-test-webflux-http-provider -am test
```

Expected: module or application classes missing until implemented.

- [ ] **Step 3: Implement the minimal annotated Mono application**

```java
@RestController
@RequestMapping("/test/items")
final class ReactiveInventoryController {
    @GetMapping("/{id}")
    Mono<InventoryResponse> item(@PathVariable String id) {
        return Mono.just(new InventoryResponse(id, "webflux"));
    }
}
```

Use `spring-boot-starter-webflux`, Gateway Starter, Provider Runtime and Actuator. Do not add Servlet starter.

- [ ] **Step 4: Package MVC and WebFlux applications**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-gateway/\
egon-cola-component-gateway-test/egon-cola-component-gateway-test-http-provider,\
egon-cola-components/egon-cola-component-gateway/\
egon-cola-component-gateway-test/\
egon-cola-component-gateway-test-webflux-http-provider -am clean package
```

Expected: both executable JARs and tests PASS.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-gateway
git commit -m "feat: add webflux gateway provider sample"
```
