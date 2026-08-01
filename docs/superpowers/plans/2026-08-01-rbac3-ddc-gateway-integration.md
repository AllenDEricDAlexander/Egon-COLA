# RBAC3 DDC 配置中心与 Gateway 文档/路由中心一次性实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` in inline execution mode and execute every checkbox in order. Do not dispatch subagents; the user explicitly prohibited subagent mode.

**Goal:** 在一次完整交付中让 RBAC3 Admin 同时完成 DDC 配置客户端与服务注册接入，以类型化、可校验、可回退的运行时策略消费五项动态配置；仅在 DDC 配置客户端 READY 后发布 HTTP Provider；并以 Gateway 现有注解、Interface Catalog、显式 Release 和 DDC 实例发现形成可验证的文档与流量路由闭环。

**Architecture:** 业务层只依赖 `Rbac3RuntimePolicy` 端口及其不可变 Snapshot。`AtomicRbac3RuntimePolicy` 以无锁读、串行候选快照校验和单次原子替换维护最后一次合法配置；DDC Adapter 用五个 `@DdcValue(refreshable = false)` 声明默认值，并以 exact `DdcConfigApplier` 完成字符串解析、完整约束校验和版本记录。RBAC3 专用 Provider 启动 Gate 覆盖 Gateway Provider Runtime 默认 Listener，在 Web Server 事件只保存端口，在 `ApplicationReadyEvent` 且 `DdcRuntimeCoordinator` 为 `READY` 时才注册 `HTTP_PROVIDER`。Gateway Controller 继续使用现有注解，契约测试比较 Spring 实际 Mapping 与 Gateway Contributor 发现集合，Gateway Admin Interface Catalog 继续作为唯一文档中心。

**Tech Stack:** Java 21、Spring Boot 3.5、Spring MVC/Security、Maven、JUnit 5、AssertJ、Mockito、DDC Starter、Gateway Starter/Provider Runtime、PostgreSQL、Redis、React 19、TypeScript、Vitest、Playwright。

**Approved Spec:** `docs/superpowers/specs/2026-08-01-rbac3-ddc-gateway-integration-design.md`

## 全局执行约束

- 全程在当前 `/Users/mario/SelfProject/Egon-COLA` 的 `main` checkout 内 inline 执行；不得创建 subagent、额外 worktree 或实施分支。
- 这是一次性交付计划。任务编号表示依赖顺序和可回滚提交点，不表示分期上线；最终验收前必须全部完成。
- 每个行为变化先写一个能因缺少该行为而失败的测试，确认 RED 后再写最小实现，随后执行 GREEN 与相邻回归。
- 每个任务只提交该任务列出的文件；工作树出现用户已有或无关变更时必须保留并从暂存区排除。
- 每个任务使用独立提交；提交前执行 `git diff --check` 和该任务的验证命令，不得把失败测试提交为完成状态。
- 不新增依赖，不新增 RBAC3 Test 聚合模块，不拆出第二套 DDC/Gateway 客户端，不引入 Swagger/Springdoc。
- 不修改、重命名或删除 RBAC3 既有 `V1`、`V2` Flyway 文件；本次无数据库结构变化，不创建任何新迁移。
- 本地 Profile 继续关闭 DDC 配置客户端和 HTTP Provider；生产配置不得出现 `localhost`、默认 Secret 或凭据回退。
- DDC 配置作用域固定为 `bizCode + env + appCode + configKey`；`namespace` 只用于服务注册与 Gateway Definition/Provider 身份。
- DDC/Gateway Bootstrap、安全根和信任边界配置不得进入动态配置；五个允许动态调整的 Key 以外一律拒绝。
- 不启动项目，不打开浏览器。最终只运行构建、单元/集成测试、静态脚本和前端离线验证；真实多进程 DDC/Gateway 拓扑留给用户启动后验证。
- 设计模式只使用已批准且解决实际问题的 Ports and Adapters、Policy/Strategy、Immutable Snapshot、Adapter 和启动 Gate；不增加 Factory/Chain/Observer 等无必要层次。

## 任务依赖与一次性交付顺序

```text
Task 1 Runtime Policy 端口与原子快照
  ├─ Task 2 DDC 声明、exact Applier 与注册顺序
  ├─ Task 3 JWT/Session 动态消费
  └─ Task 4 激活根角色上限
Task 2
  ├─ Task 5 生产配置、Spring 装配与作用域
  └─ Task 6 Provider 发布 Gate、状态与 Readiness
Task 5 + Task 6
  └─ Task 7 Gateway 文档目录完备性
Task 1..7
  ├─ Task 8 故障、恢复与跨模块回归
  └─ Task 9 运维文档和静态校验
Task 1..9
  └─ Task 10 一次性 clean verification 与交付审计
```

---

### Task 1：建立与 DDC 解耦的 Runtime Policy 和原子快照

**Files:**

- Create: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/application/port/Rbac3RuntimePolicy.java`
- Create: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/ddc/AtomicRbac3RuntimePolicy.java`
- Create: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/ddc/AtomicRbac3RuntimePolicyTest.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3ApplicationConfiguration.java`

**Produces:**

- 稳定的应用端口 `Rbac3RuntimePolicy.current()`。
- 完整不可变的 `Snapshot`，包含四个 Duration、最大激活根数和五个 Key 的版本 Map。
- 单例 `AtomicRbac3RuntimePolicy`；所有 Profile 均从 `Rbac3AdminProperties` 初始化同一个 Bean。
- 白名单 Key 常量、严格整数解析、范围约束、关系约束、失败元数据和原子更新入口。

**Consumes:** 仅消费 `Rbac3AdminProperties` 的既有安全默认值；本任务不依赖 DDC 类型。

- [ ] **Step 1：先写 Snapshot 和原子更新的 RED 测试**

  在 `AtomicRbac3RuntimePolicyTest` 精确覆盖：

  1. 默认值为 900、604800、1800、43200、16，版本 Map 初始均为 0；
  2. `configVersions()` 无法修改；
  3. 合法单 Key 更新产生新 Snapshot，旧 Snapshot 内容不变；
  4. 读到的 Snapshot 永远包含全部字段，不暴露半更新状态；
  5. 拒绝空串、前后空白、小数、符号附加字符、溢出和未知 Key；
  6. 拒绝越界值：Access 300–1800、Refresh 86400–2592000、Idle 300–28800、Absolute 3600–86400、Roots 1–32；
  7. 拒绝 `idle > absolute` 与 `refresh < absolute`；
  8. 失败后 Snapshot、该 Key 版本均不变，`lastApplyFailure` 只保存 Key、目标版本和非敏感错误码；
  9. 后续合法更新清除相同 Key 的旧失败记录；
  10. 直接构造非法 `Rbac3RuntimePolicy.Snapshot` 同样失败，不能绕过 Atomic Adapter 获得非法业务快照。

  关键断言采用具体值，不复刻实现分支：

  ```java
  AtomicRbac3RuntimePolicy policy = policyWithDefaults();
  Rbac3RuntimePolicy.Snapshot before = policy.current();

  policy.apply(AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY, "1200", 7L);

  assertThat(policy.current().accessTokenTtl()).isEqualTo(Duration.ofSeconds(1200));
  assertThat(policy.current().configVersions())
          .containsEntry(AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY, 7L);
  assertThat(before.accessTokenTtl()).isEqualTo(Duration.ofSeconds(900));
  ```

- [ ] **Step 2：运行 RED**

  ```bash
  ./mvnw -B -ntp \
    -pl :egon-cola-platform-rbac3-admin -am \
    -Dtest=AtomicRbac3RuntimePolicyTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
  ```

  Expected: 测试编译失败，因为端口与实现尚不存在。记录这是预期 RED，不处理无关模块。

- [ ] **Step 3：实现最小端口与不可变 Snapshot**

  `Rbac3RuntimePolicy` 只声明业务读模型：

  ```java
  public interface Rbac3RuntimePolicy {

      Snapshot current();

      record Snapshot(
              Duration accessTokenTtl,
              Duration refreshTokenTtl,
              Duration sessionIdleTimeout,
              Duration sessionAbsoluteTimeout,
              int maximumActiveRoots,
          Map<String, Long> configVersions) {

          public Snapshot {
              Objects.requireNonNull(accessTokenTtl, "accessTokenTtl");
              Objects.requireNonNull(refreshTokenTtl, "refreshTokenTtl");
              Objects.requireNonNull(sessionIdleTimeout, "sessionIdleTimeout");
              Objects.requireNonNull(sessionAbsoluteTimeout, "sessionAbsoluteTimeout");
              configVersions = Map.copyOf(configVersions);
              validateRangesAndRelationships(
                      accessTokenTtl, refreshTokenTtl,
                      sessionIdleTimeout, sessionAbsoluteTimeout,
                      maximumActiveRoots);
          }
      }
  }
  ```

  `Snapshot` 的 canonical constructor 是范围与关系约束的唯一入口，保证测试桩、静态默认值和 DDC Candidate 都不能构造非法快照。`AtomicRbac3RuntimePolicy` 使用 `AtomicReference<Snapshot>` 提供无锁读取；`apply` 使用 `synchronized` 串行解析并构造 Candidate，Snapshot 校验通过后一次 `reference.set(candidate)`。解析必须使用能拒绝空白和附加字符的严格十进制规则，异常消息只能包含 Key、Version 和规则，不包含 `rawValue`。

  将范围与关系校验集中在 `Snapshot` 的一个私有静态校验入口：

  ```java
  requireRange(access, 300, 1800, ACCESS_TOKEN_TTL_KEY);
  requireRange(refresh, 86_400, 2_592_000, REFRESH_TOKEN_TTL_KEY);
  requireRange(idle, 300, 28_800, SESSION_IDLE_TIMEOUT_KEY);
  requireRange(absolute, 3_600, 86_400, SESSION_ABSOLUTE_TIMEOUT_KEY);
  requireRange(maximumRoots, 1, 32, MAXIMUM_ACTIVE_ROOTS_KEY);
  if (idle > absolute) {
      throw invalid(SESSION_IDLE_TIMEOUT_KEY, "IDLE_EXCEEDS_ABSOLUTE");
  }
  if (refresh < absolute) {
      throw invalid(REFRESH_TOKEN_TTL_KEY, "REFRESH_BELOW_ABSOLUTE");
  }
  ```

  `Rbac3ApplicationConfiguration` 始终创建一个 `AtomicRbac3RuntimePolicy`，并以 `Rbac3RuntimePolicy` 端口暴露；不得为 local/test 另建静态实现。

- [ ] **Step 4：运行 GREEN 与应用层边界回归**

  ```bash
  ./mvnw -B -ntp \
    -pl :egon-cola-platform-rbac3-admin -am \
    -Dtest=AtomicRbac3RuntimePolicyTest,AdminLayerBoundaryTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
  git diff --check
  ```

  Expected: 测试通过；`admin.application.port` 不出现任何 `top.egon.cola.component.ddc` import。

- [ ] **Step 5：提交 Task 1**

  ```bash
  git add \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/application/port/Rbac3RuntimePolicy.java \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/ddc/AtomicRbac3RuntimePolicy.java \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3ApplicationConfiguration.java \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/ddc/AtomicRbac3RuntimePolicyTest.java
  git commit -m "feat(rbac3): add atomic runtime policy"
  ```

---

### Task 2：声明五项 DDC 配置并注册 exact Applier

**Files:**

- Create: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/ddc/Rbac3DdcValueDeclarations.java`
- Create: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/ddc/Rbac3DdcPolicyApplier.java`
- Create: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/ddc/Rbac3DdcPolicyConfiguration.java`
- Create: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/ddc/Rbac3DdcPolicyConfigurationTest.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/ddc/AtomicRbac3RuntimePolicyTest.java`

**Produces:** 五个可上报默认值的 `@DdcValue` 声明；五个 exact Applier；固定全量 Pull 优先级；在 DDC Registry freeze 前完成的确定性注册。

**Consumes:** `AtomicRbac3RuntimePolicy`、`DdcConfigApplier`、`DdcConfigApplierRegistry`、`@DdcValue`；不得复制 DDC 的版本、Checksum、锁或 ACK 算法。

- [ ] **Step 1：先写注解、注册和优先级 RED 测试**

  `Rbac3DdcPolicyConfigurationTest` 通过反射和真实 `DefaultDdcConfigApplierRegistry` 断言：

  - 字段 Key 与默认值精确为 Spec 中五项；类型为 `Long.class` 或 `Integer.class`；`required=true`、`refreshable=false`；
  - exact Registry 能按 Key 返回 RBAC3 Applier，不会落到字段反射 fallback；
  - Access/Roots priority 0，Refresh 10，Absolute 20，Idle 30；
  - 重复注册同一 Key 启动失败；Registry freeze 后注册失败；
  - Applier 把 `key/value/version` 原样传给 Policy，不记录 rawValue；
  - 依次应用 `refresh → absolute → idle` 可从默认值进入一个新的整体合法组合。

  反射断言必须验证注解契约本身：

  ```java
  DdcValue annotation = field.getAnnotation(DdcValue.class);
  assertThat(annotation.key()).isEqualTo(AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY);
  assertThat(annotation.defaultValue()).isEqualTo("900");
  assertThat(annotation.refreshable()).isFalse();
  ```

- [ ] **Step 2：运行 RED**

  ```bash
  ./mvnw -B -ntp \
    -pl :egon-cola-platform-rbac3-admin -am \
    -Dtest=Rbac3DdcPolicyConfigurationTest,AtomicRbac3RuntimePolicyTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
  ```

  Expected: 新类缺失导致编译失败。

- [ ] **Step 3：实现声明、Adapter 和 freeze 前注册**

  五个声明字段采用以下同一形式，默认值分别是 `900/604800/1800/43200/16`：

  ```java
  @DdcValue(
          value = "rbac3.access-token-ttl-seconds:900",
          key = AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY,
          defaultValue = "900",
          type = Long.class,
          required = true,
          refreshable = false)
  private Long accessTokenTtlSeconds = 900L;
  ```

  每个 `Rbac3DdcPolicyApplier` 只绑定一个 Key 和 Priority：

  ```java
  @Override
  public void apply(String actualKey, String value, long version) {
      if (!key.equals(actualKey)) {
          throw new IllegalArgumentException("unexpected RBAC3 config key: " + actualKey);
      }
      policy.apply(actualKey, value, version);
  }

  @Override
  public int priority() {
      return priority;
  }
  ```

  `Rbac3DdcPolicyConfiguration` 只在 `egon.cola.component.ddc.enabled=true` 时创建声明和注册器。注册器实现 `InitializingBean`，构造时持有 Registry，`afterPropertiesSet()` 对五个 Key 调用 `registerExact`；这会发生在 DDC 的 `SmartInitializingSingleton` freezer 之前。不得在 `ApplicationReadyEvent` 或首次 Refresh 时延迟注册。

- [ ] **Step 4：运行 GREEN 和 DDC Starter 邻接回归**

  ```bash
  ./mvnw -B -ntp \
    -pl :egon-cola-platform-rbac3-admin,:egon-cola-platform-dynamic-config-center-starter -am \
    -Dtest=Rbac3DdcPolicyConfigurationTest,AtomicRbac3RuntimePolicyTest,DefaultDdcConfigApplierRegistryTest,DdcRefreshServiceTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
  git diff --check
  ```

  Expected: RBAC3 测试与 DDC 既有版本/ACK 测试全部通过。

- [ ] **Step 5：提交 Task 2**

  ```bash
  git add \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/ddc/Rbac3DdcValueDeclarations.java \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/ddc/Rbac3DdcPolicyApplier.java \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/ddc/Rbac3DdcPolicyConfiguration.java \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/ddc/Rbac3DdcPolicyConfigurationTest.java \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/ddc/AtomicRbac3RuntimePolicyTest.java
  git commit -m "feat(rbac3): consume validated DDC configuration"
  ```

---

### Task 3：让新 Token 和新 Session 每次读取一个 Policy Snapshot

**Files:**

- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/auth/application/JwtTokenService.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/session/application/SessionFacade.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/security/Rbac3JwtConfiguration.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3ApplicationConfiguration.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/auth/JwtTokenServiceTest.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/session/SessionFacadeTest.java`

**Behavior:** 一次 `issue()` 或 `create()` 只读取一次 Snapshot；更新后新对象使用新 TTL，旧 JWT claim 和已持久化 Session/Refresh 到期时间不变。

- [ ] **Step 1：写动态消费的 RED 测试**

  `JwtTokenServiceTest` 使用可替换的 Policy，先签发默认 Token，再改为 1200 秒并签发第二个 Token：

  ```java
  assertThat(first.expiresAt()).isEqualTo(now.plusSeconds(900));
  policy.apply(AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY, "1200", 1L);
  assertThat(second.expiresAt()).isEqualTo(now.plusSeconds(1200));
  assertThat(first.claims().exp()).isEqualTo(now.plusSeconds(900));
  ```

  `SessionFacadeTest` 创建第一条 Session，更新 Refresh/Absolute/Idle，再创建第二条，断言：

  - 第一条仍为创建时的三个到期时间；
  - 第二条三个到期时间均来自同一个更新后 Snapshot；
  - Policy 测试桩统计 `current()` 在一次 `create()` 中只调用一次；
  - 配置在 Store callback 前后变化不会混合三个期限。

- [ ] **Step 2：运行 RED**

  ```bash
  ./mvnw -B -ntp \
    -pl :egon-cola-platform-rbac3-admin -am \
    -Dtest=JwtTokenServiceTest,SessionFacadeTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
  ```

  Expected: 当前构造器仍永久保存 Duration，第二次调用继续使用旧值或新构造器测试无法编译。

- [ ] **Step 3：改为按命令读取端口，并更新全部直接构造点**

  先定位全部构造点，逐一改为注入同一 Policy，不保留两套可能漂移的 TTL 校验：

  ```bash
  rg -n 'new JwtTokenService\(|new SessionFacade\(' \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src
  ```

  `JwtTokenService.issue()` 开头读取一次：

  ```java
  Rbac3RuntimePolicy.Snapshot policySnapshot = runtimePolicy.current();
  Instant expiresAt = now.plus(policySnapshot.accessTokenTtl());
  ```

  `SessionFacade.create()` 在生成记录和调用 Store 前读取一次，并从同一局部变量计算 Idle、Absolute、Refresh。构造器只验证端口非空；范围与关系由 Snapshot 所有者统一保证。

  `Rbac3JwtConfiguration` 和 `Rbac3ApplicationConfiguration` 注入 `Rbac3RuntimePolicy`，不再把 `Rbac3AdminProperties` 的 Duration 复制到业务服务。

- [ ] **Step 4：运行 GREEN 和认证/Refresh 发布回归**

  ```bash
  ./mvnw -B -ntp \
    -pl :egon-cola-platform-rbac3-admin -am \
    -Dtest=JwtTokenServiceTest,SessionFacadeTest,AuthenticationRuntimePublicationTest,RefreshRuntimePublicationTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
  git diff --check
  ```

- [ ] **Step 5：提交 Task 3**

  ```bash
  git add \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/auth/application/JwtTokenService.java \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/session/application/SessionFacade.java \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/security/Rbac3JwtConfiguration.java \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3ApplicationConfiguration.java \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/auth/JwtTokenServiceTest.java \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/session/SessionFacadeTest.java
  git diff --cached --name-only
  git commit -m "feat(rbac3): apply runtime policy to new credentials"
  ```

---

### Task 4：在规范化后限制激活的顶级根角色总数

**Files:**

- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-contract/src/main/java/top/egon/cola/platform/rbac3/contract/error/Rbac3ErrorCode.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/activation/application/RoleActivationFacade.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3ApplicationConfiguration.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-contract/src/test/java/top/egon/cola/platform/rbac3/contract/Rbac3ErrorCodeTest.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/activation/RoleActivationFacadeIT.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/activation/RoleActivationConcurrencyIT.java`

**Rule:** 限制 `resolution.activeRoleSet().rootIds().size()`，不是请求 Role ID 数量，也不是继承展开后的有效角色数量；同一事务内在任何持久化、Fence、投影或发 Token 之前失败。

- [ ] **Step 1：写错误契约和规范根计数 RED 测试**

  新增以下场景：

  - Error Code `ACTIVE_ROLE_ROOT_LIMIT_EXCEEDED` 的 HTTP status 为 422，`retryable=false`；
  - 请求包含父角色与其子角色，规范化后只有一个根，在上限 1 下成功；
  - 两个 APP 各一个规范根，在上限 1 下失败；
  - Policy 从 2 动态改为 1 后，下一次激活失败，既有激活状态不追溯撤销；
  - 失败时 Transaction 不写 roots，RuntimeStore 不建 Fence、不 publish，TokenIssuer 不调用；
  - APP 内互斥仍先由 Resolver 返回原有错误，不被数量错误覆盖。

  核心失败断言：

  ```java
  assertThatThrownBy(() -> facade.replace(command))
          .isInstanceOf(Rbac3RuleViolation.class)
          .extracting("code")
          .isEqualTo("ACTIVE_ROLE_ROOT_LIMIT_EXCEEDED");
  verifyNoInteractions(runtimeStore, accessTokenIssuer);
  ```

- [ ] **Step 2：运行 RED**

  ```bash
  ./mvnw -B -ntp \
    -pl :egon-cola-platform-rbac3-contract,:egon-cola-platform-rbac3-admin -am \
    -Dtest=Rbac3ErrorCodeTest,RoleActivationFacadeIT \
    -Dsurefire.failIfNoSpecifiedTests=false test
  ```

- [ ] **Step 3：实现最小数量规则并更新构造点**

  `RoleActivationFacade` 注入 `Rbac3RuntimePolicy`，并在 Resolver 返回后、`requireAuthenticationStrength` 前执行：

  ```java
  int actual = resolution.activeRoleSet().rootIds().size();
  int maximum = runtimePolicy.current().maximumActiveRoots();
  if (actual > maximum) {
      throw new Rbac3RuleViolation(
              "ACTIVE_ROLE_ROOT_LIMIT_EXCEEDED",
              List.of(Integer.toString(actual), Integer.toString(maximum)));
  }
  ```

  错误参数只包含实际数与上限，不包含角色 ID。更新生产配置与两类 IT 的构造器；不得把规则移进 Resolver，因为上限是运行时平台策略而非角色规范化算法本身。

- [ ] **Step 4：运行 GREEN、并发与 HTTP 错误映射回归**

  ```bash
  ./mvnw -B -ntp \
    -pl :egon-cola-platform-rbac3-contract,:egon-cola-platform-rbac3-admin -am \
    -Dtest=Rbac3ErrorCodeTest,RoleActivationFacadeIT,RoleActivationConcurrencyIT,Rbac3ApiExceptionHandlerTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
  git diff --check
  ```

- [ ] **Step 5：提交 Task 4**

  ```bash
  git add \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-contract/src/main/java/top/egon/cola/platform/rbac3/contract/error/Rbac3ErrorCode.java \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-contract/src/test/java/top/egon/cola/platform/rbac3/contract/Rbac3ErrorCodeTest.java \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/activation/application/RoleActivationFacade.java \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3ApplicationConfiguration.java \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/activation/RoleActivationFacadeIT.java \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/activation/RoleActivationConcurrencyIT.java
  git commit -m "feat(rbac3): enforce active root policy"
  ```

---

### Task 5：启用生产 DDC 配置客户端并验证配置/服务作用域分离

**Files:**

- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/resources/application.yml`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/resources/application-local.yml`
- Verify unchanged or minimally align: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/resources/application-local-it.yml`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/GatewayDdcConfigurationTest.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/Rbac3AdminApplicationContextTest.java`

**Production contract:** `ddc.enabled=true` 与 `ddc.registry.enabled=true` 同时存在；CONFIG_CLIENT 和 HTTP_PROVIDER 使用同一进程 Instance ID 但独立 Lease；DDC 配置 scope 不含 namespace，Provider/Definition identity 保留 namespace。

- [ ] **Step 1：写生产 YAML 和条件装配 RED 测试**

  `GatewayDdcConfigurationTest` 使用 `YamlPropertySourceLoader` 或项目既有读取方式断言：

  - `egon.cola.component.ddc.enabled=true`；
  - `biz-code=${DDC_BIZ_CODE:rbac3}`、`app-code=rbac3-admin`、env 无默认、namespace 仅用于服务身份；
  - Instance ID 来自 `${RBAC3_INSTANCE_ID}`，租约 30 秒、心跳 10 秒；
  - `consistency.fail-fast=true`、`reconcile-enabled=true`、周期 30 秒；
  - Registry 与 Gateway reporting/provider 都启用；
  - Admin/HMAC/Redis 没有 localhost 或 Secret 默认值；
  - local profile 显式设置 `ddc.enabled=false`、`ddc.registry.enabled=false` 和 Provider disabled，不因生产配置打开而继承为 true。

  `Rbac3AdminApplicationContextTest` 用隔离 `ApplicationContextRunner` 验证：DDC disabled 时 Policy 仍存在但没有声明/Applier 注册器；DDC enabled 且提供假的 DDC 基础 Bean 时五个 exact Applier 均存在。

- [ ] **Step 2：运行 RED**

  ```bash
  ./mvnw -B -ntp \
    -pl :egon-cola-platform-rbac3-admin -am \
    -Dtest=GatewayDdcConfigurationTest,Rbac3AdminApplicationContextTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
  ```

  Expected: 生产 YAML 的 `ddc.enabled` 仍为 false，配置断言失败。

- [ ] **Step 3：最小修改生产配置**

  目标片段必须使用项目现有 kebab-case 属性名：

  ```yaml
  egon:
    cola:
      component:
        ddc:
          enabled: true
          biz-code: ${DDC_BIZ_CODE:rbac3}
          app-code: rbac3-admin
          env: ${DEPLOYMENT_ENV}
          namespace: ${DEPLOYMENT_NAMESPACE}
          instance:
            id: ${RBAC3_INSTANCE_ID}
            lease-seconds: 30
            heartbeat-interval-seconds: 10
          consistency:
            fail-fast: true
            reconcile-enabled: true
            reconcile-interval-seconds: 30
          registry:
            enabled: true
  ```

  保留现有 Admin Endpoint/HMAC/Redis/Gateway 配置及 Secret 来源，不新建第二组 Redisson 或静态 RBAC3 provider URL。由于 Spring Profile 会继承基础 `application.yml`，必须同时在 `application-local.yml` 的 `egon.cola.component.ddc` 下显式增加 `enabled: false`；既有 `registry.enabled: false` 和 `gateway.provider.http.enabled: false` 保持不变。

- [ ] **Step 4：运行 GREEN 与资源扫描**

  ```bash
  ./mvnw -B -ntp \
    -pl :egon-cola-platform-rbac3-admin -am \
    -Dtest=GatewayDdcConfigurationTest,Rbac3AdminApplicationContextTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
  rg -n 'localhost|127\.0\.0\.1|secret-key: [^$]|password: [^$]' \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/resources/application.yml
  git diff --check
  ```

  Expected: 测试通过；`rg` 不产生生产回退匹配。

- [ ] **Step 5：提交 Task 5**

  ```bash
  git add \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/resources/application.yml \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/resources/application-local.yml \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/GatewayDdcConfigurationTest.java \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/Rbac3AdminApplicationContextTest.java
  git commit -m "feat(rbac3): enable production DDC configuration client"
  ```

---

### Task 6：以 DDC READY Gate 控制 Provider 发布，并扩展独立状态与 Readiness

**Files:**

- Create: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/ddc/DdcConfigClientStatusService.java`
- Create: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/ddc/Rbac3IntegrationMetrics.java`
- Create: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3HttpProviderPublicationGate.java`
- Create: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/Rbac3HttpProviderPublicationGateTest.java`
- Create: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/DdcConfigClientStatusServiceTest.java`
- Create: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/Rbac3IntegrationMetricsTest.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/ddc/Rbac3DdcPolicyApplier.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/ddc/Rbac3DdcPolicyConfiguration.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3PlatformIntegrationConfiguration.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/runtime/application/ControlPlaneRuntimeStatusPort.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/runtime/RuntimeQueryServiceTest.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/Rbac3AdminApplicationContextTest.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/runtime/runtime.api.ts`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/runtime/ControlPlaneStatusCards.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/runtime/RuntimeStatusPage.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/runtime/RuntimeStatusPage.test.tsx`

**Produces:** 名为 `gatewayHttpProviderServerReadyListener` 的 RBAC3 Bean；Gateway Provider Runtime 默认 Bean 因 `@ConditionalOnMissingBean(name=...)` 退让。Runtime Status 新增独立 `ddcConfigClient`，不合并 Definition、Provider、Release。Micrometer 暴露四个低基数指标：`rbac3_ddc_config_apply_total`、`rbac3_ddc_config_snapshot_version`、`rbac3_ddc_config_ready`、`rbac3_gateway_definition_operation_count`。

- [ ] **Step 1：写事件时序、状态脱敏与 Readiness RED 测试**

  `Rbac3HttpProviderPublicationGateTest` 直接驱动事件，覆盖：

  1. root WebServer event 只记录端口，不调用 `onHttpServerReady`；
  2. management server namespace event 被忽略；
  3. ApplicationReady + DDC READY + 端口一致时仅调用一次；
  4. 事件重复或顺序反转也最多发布一次；
  5. DDC NEW/STARTING/RECOVERING/FAILED 或无 CONFIG_CLIENT session 时不发布并抛出启动失败；
  6. 实际端口与显式 Provider 端口冲突时失败；配置端口为 0 时接受 WebServer 实际端口；
  7. local/DDC disabled 场景不创建该 Gate Bean，保留现有 disabled 行为。

  `DdcConfigClientStatusServiceTest` 与 `RuntimeQueryServiceTest` 断言：

  - state、instanceId、leaseExpireAt、五个版本和最后失败非敏感信息均返回；
  - 完整 leaseId 不返回，状态使用短 Hash；
  - readiness 仅在生产要求 DDC READY + session present；
  - 一次非法动态配置保留 LKG 时，状态记录失败但 readiness 不立即 DOWN；
  - Definition、Config Client、Provider Lease、Release 字段各自保留。

  `Rbac3IntegrationMetricsTest` 使用 `SimpleMeterRegistry` 断言：

  - 成功/失败 Apply 分别增加 `status=success/failed` Counter；
  - `key` 标签只允许五个白名单 Key，不能使用 rawValue、版本、实例、租户或异常文本；
  - 五个 Snapshot Version Gauge 跟随当前 Policy 版本；
  - Ready Gauge 仅在 Coordinator READY 且存在 Session 时为 1；
  - Gateway Operation Count 从 `GatewayReportingState.snapshot().result().counts().operations()` 读取，未成功上报时为 0；
  - Meter/Tag 名称与 Spec 完全一致且总组合数有固定上界。

  `RuntimeStatusPage.test.tsx` 的 fixture 增加 `ddcConfigClient`，断言页面同时显示 “DDC Config Client”、`READY`、脱敏 Lease 信息和原有 Provider 的 `RECOVERING`；并断言完整 leaseId、配置原值和 Secret 不出现在 DOM。

- [ ] **Step 2：运行 RED**

  ```bash
  ./mvnw -B -ntp \
    -pl :egon-cola-platform-rbac3-admin -am \
    -Dtest=Rbac3HttpProviderPublicationGateTest,DdcConfigClientStatusServiceTest,Rbac3IntegrationMetricsTest,RuntimeQueryServiceTest,Rbac3AdminApplicationContextTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
  npm --prefix egon-cola-platforms/egon-cola-platform-rbac3 ci
  npm --prefix egon-cola-platforms/egon-cola-platform-rbac3 test \
    --workspace @egon-cola/rbac3-admin-web -- \
    --run src/features/runtime/RuntimeStatusPage.test.tsx
  ```

  Expected: 后端新类缺失导致编译失败；前端尚无 Config Client 字段和卡片，新增断言失败。

- [ ] **Step 3：实现确定性启动 Gate**

  `Rbac3HttpProviderPublicationGate` 实现 `ApplicationListener<ApplicationEvent>`，保存 root server port、`ApplicationReadyEvent` 标志和 `AtomicBoolean published`。只有以下条件全部满足才调用 Provider Runtime：

  ```java
  coordinator.state() == DdcRuntimeState.READY
          && coordinator.currentSession().isPresent()
          && serverPort > 0
          && configuredPortMatches(serverPort)
          && published.compareAndSet(false, true)
  ```

  Gate 不注册 DDC、不拉配置、不创建自己的 Lease；它只编排现有 `DdcRuntimeCoordinator` 和 `HttpProviderLeaseRuntime`。生产 fail-fast 下 DDC 未 READY 时抛出不含凭据的 `IllegalStateException`，确保进程不声称 Ready。

  在 `Rbac3PlatformIntegrationConfiguration` 以同名 Bean 覆盖默认 Listener：

  ```java
  @Bean(name = "gatewayHttpProviderServerReadyListener")
  @ConditionalOnProperty(prefix = "egon.cola.component.ddc", name = "enabled", havingValue = "true")
  ApplicationListener<ApplicationEvent> gatewayHttpProviderServerReadyListener(
          DdcRuntimeCoordinator coordinator,
          HttpProviderLeaseRuntime providerRuntime,
          GatewayHttpProviderProperties providerProperties) {
      return new Rbac3HttpProviderPublicationGate(
              coordinator, providerRuntime, providerProperties);
  }
  ```

- [ ] **Step 4：实现独立 Config Client 状态和 Readiness 检查**

  `ControlPlaneRuntimeStatusPort.RuntimeStatus` 新增 `DdcConfigClientStatus ddcConfigClient`。该 Record 只包含：

  ```text
  state, instanceId, leaseIdFingerprint, leaseExpireAt,
  configVersions, lastApplyFailureKey, lastApplyFailureVersion,
  lastApplyFailureCode
  ```

  Map 必须 `Map.copyOf`；指纹使用固定 SHA-256 截断而不是完整 leaseId。`DdcConfigClientStatusService` 只读取 Coordinator 和 Policy，不缓存第二份状态。

  `Rbac3ReadinessIndicator` 增加名为 `ddcConfigClient` 的检查：生产环境要求 `READY` 且当前 session 存在；local/test 不要求。非法新配置因 Snapshot 保持 LKG 而不单独把 readiness 置 DOWN。

  保留 `RuntimeStatus` 现有四参数便利构造器供 `GatewayDdcRuntimeStatusService` 使用，并让它填入一个显式 `UNKNOWN` 的 `DdcConfigClientStatus`；最终聚合端口再用真实 Config Client 状态替换该占位。这样不要求 Gateway 状态服务伪造 DDC 配置事实。

  Admin Web 的 `ControlPlaneRuntimeStatus` 增加同名只读字段并单独显示 “DDC Config Client” 卡片，展示 state、instanceId、lease expiry 和 last failure code；不显示完整 leaseId 或配置原值。第一行改为四个等宽卡片，页面提示更新为五事实语义；既有 Definition、HTTP Provider Lease、Release 和运维卡片保持独立。

- [ ] **Step 5：实现固定白名单的 Micrometer 观测**

  `Rbac3IntegrationMetrics` 接受 Policy，并以 `ObjectProvider<DdcRuntimeCoordinator>`、`ObjectProvider<GatewayReportingState>` 作为惰性状态 Supplier，在构造时注册固定 Gauge；不得为了指标提前创建 Coordinator，也不得让 Applier Registry → Metrics → Coordinator → Refresh/Registry 形成循环依赖。配置类通过 `ObjectProvider<Rbac3IntegrationMetrics>` 可选获得指标；无 MeterRegistry/Metric Bean 时使用 Applier 的 no-op observer，动态配置本身绝不能依赖观测系统可用。`Rbac3DdcPolicyApplier` 在 `policy.apply` 成功后记录 success，在 catch 中记录 failed 后原样抛出；Counter 标签只使用构造时绑定的五个 Key 和两个固定 Status。

  ```java
  try {
      policy.apply(actualKey, value, version);
      metrics.recordApply(key, "success");
  } catch (RuntimeException failure) {
      metrics.recordApply(key, "failed");
      throw failure;
  }
  ```

  不增加 `lastReconcileAt` 的复制调度器；DDC Reconcile 继续复用 Starter 的日志/生命周期，Config Ready Gauge 作为 Spec 允许的替代观测。

- [ ] **Step 6：运行 GREEN 与 Provider Runtime 自动配置回归**

  ```bash
  ./mvnw -B -ntp \
    -pl :egon-cola-platform-rbac3-admin,:egon-cola-platform-gateway-provider-runtime -am \
    -Dtest=Rbac3HttpProviderPublicationGateTest,DdcConfigClientStatusServiceTest,Rbac3IntegrationMetricsTest,RuntimeQueryServiceTest,Rbac3AdminApplicationContextTest,GatewayHttpProviderAutoConfigurationTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
  npm --prefix egon-cola-platforms/egon-cola-platform-rbac3 run typecheck \
    --workspace @egon-cola/rbac3-admin-web
  npm --prefix egon-cola-platforms/egon-cola-platform-rbac3 test \
    --workspace @egon-cola/rbac3-admin-web -- \
    --run src/features/runtime/RuntimeStatusPage.test.tsx
  git diff --check
  ```

- [ ] **Step 7：提交 Task 6**

  ```bash
  git add \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/ddc/DdcConfigClientStatusService.java \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/ddc/Rbac3IntegrationMetrics.java \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/ddc/Rbac3DdcPolicyApplier.java \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/ddc/Rbac3DdcPolicyConfiguration.java \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3HttpProviderPublicationGate.java \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3PlatformIntegrationConfiguration.java \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/runtime/application/ControlPlaneRuntimeStatusPort.java \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/Rbac3HttpProviderPublicationGateTest.java \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/DdcConfigClientStatusServiceTest.java \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/Rbac3IntegrationMetricsTest.java \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/Rbac3AdminApplicationContextTest.java \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/runtime/RuntimeQueryServiceTest.java \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/runtime/runtime.api.ts \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/runtime/ControlPlaneStatusCards.tsx \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/runtime/RuntimeStatusPage.tsx \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/runtime/RuntimeStatusPage.test.tsx
  git commit -m "feat(rbac3): gate provider publication on DDC readiness"
  ```

---

### Task 7：把 Gateway 现有注解和 Interface Catalog 固化为完整文档契约

**Files:**

- Create: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/Rbac3GatewayDocumentCatalogContractTest.java`
- Modify only when a test proves a real gap: RBAC3 Controller/DTO files under `egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/Rbac3GatewayDefinitionDiscoveryTest.java`

**Contract:** Spring MVC 是 Method/Path/Consumes/Produces 机械事实来源；Gateway 注解提供业务目录和 Schema 补充；实际 Mapping 集合必须与 `MvcGatewayDefinitionContributor` 发现集合完全相等，不能长期硬编码 78。

- [ ] **Step 1：先写 Mapping/Definition 集合等价 RED 测试**

  启动最小 MVC Context，过滤 Spring 自带 `/error` 与 Actuator mapping，把一个 Mapping 的多 Path、多 Method 展开为笛卡尔积，并构造可由 Contributor 同样表达的规范键：

  ```text
  HTTP_METHOD + normalized_path
  ```

  Contributor 侧使用 `Operation.methodIdentity()` 生成同一规范键，对实际 Handler 集合和 Contributor Operation 集合做 `containsExactlyInAnyOrderElementsOf`；Controller class 和 handler method 不在 Definition 中重复编码，注解覆盖则直接遍历 Spring Handler 检查。同时逐项断言：

  - Controller 同时有 `@RestController`、`@EgonHttpService`、`@GatewayInterfaceGroup`；
  - Handler 有 `@GatewayOperation`；
  - Operation name 非空、全局唯一，包含 `rbac3` 且具有 `-v数字` 版本后缀；不把 RBAC3 平台代际 `3` 错当成接口版本；
  - summary 非空，tags 同时含 `rbac3` 和能力域；
  - `externalAccessible` 可从 annotation 明确读取；
  - Contributor 保留 Method、Path、参数、Request/Response Schema、summary、description、tags、Provider identity；
  - Schema 可以保留契约要求的敏感字段名，但敏感字段节点不得携带 `example`、`default` 或真实样例值；描述不得嵌入 refresh token、password、credential、private key、secret 或 hash 原文；
  - 输出当前 Operation 数作为诊断信息，但不使用 `assertThat(count).isEqualTo(78)`。

- [ ] **Step 2：运行 RED/基线测试**

  ```bash
  ./mvnw -B -ntp \
    -pl :egon-cola-platform-rbac3-admin -am \
    -Dtest=Rbac3GatewayDocumentCatalogContractTest,Rbac3GatewayDefinitionDiscoveryTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
  ```

  Expected: 新测试缺失导致编译失败；添加测试后若当前 78 个 Mapping 已完整，测试可以直接 GREEN，此时不为制造 RED 而修改生产 Controller。TDD 的 RED 证据是测试类不存在/编译失败，生产代码只在测试发现真实缺口时改变。

- [ ] **Step 3：只修复测试发现的具体注解缺口**

  若集合不等，按失败报告定位单个 Controller/Handler，补充现有 Gateway 注解或缺失 Schema 字段说明。不得：

  - 新建 RBAC3 私有文档注解；
  - 重复在 Gateway 注解内维护 Method/Path；
  - 添加 Swagger/Springdoc；
  - 自动发布 Gateway Release；
  - 把敏感请求样例加入 Catalog。

- [ ] **Step 4：运行全部 Gateway Discovery 测试**

  ```bash
  ./mvnw -B -ntp \
    -pl :egon-cola-platform-rbac3-admin -am \
    -Dtest='*GatewayDiscoveryTest,Rbac3GatewayDocumentCatalogContractTest' \
    -Dsurefire.failIfNoSpecifiedTests=false test
  git diff --check
  ```

- [ ] **Step 5：提交 Task 7**

  ```bash
  git add \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/Rbac3GatewayDocumentCatalogContractTest.java \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/Rbac3GatewayDefinitionDiscoveryTest.java
  git diff --name-only -- \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http
  git commit -m "test(rbac3): enforce gateway document catalog coverage"
  ```

  Expected: 当前审计显示 78 个 Mapping 已有注解，因此生产目录列表应为空。只有 Step 3 的失败证明确实要求修改时，才把失败报告点名的 Controller/DTO 以完整文件路径追加到 `git add`，并先审阅 `git diff --cached --name-only`；不得暂存整个目录。

---

### Task 8：覆盖 DDC 非法更新、LKG、恢复和五事实独立性

**Files:**

- Create: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/Rbac3DdcRefreshIntegrationTest.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/GatewayDdcConfigurationTest.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/runtime/RuntimeQueryServiceTest.java`
- Modify only if required by failing test: DDC integration classes created in Tasks 1, 2 and 6.

**Boundary:** 本任务在进程内使用真实 DDC Refresh/Registry 算法和 fake Admin/Ack 边界；不把 Testcontainers 或 mock 成功描述为真实 Redis/PostgreSQL/Gateway 多进程证明。

- [ ] **Step 1：写完整配置 Refresh/LKG RED 测试**

  用真实 `DefaultDdcConfigApplierRegistry`、`DdcRefreshService`、内存 `DdcLocalConfigRepository`、已启动的真实 `DdcAckDelivery`、捕获 `ack()` 的 fake `DdcAdminClient` 和测试 `DdcLeaseSessionHolder` 覆盖：

  - 启动全量 Pull 的五个合法 `applySnapshots` 按 priority 应用，版本与 Policy 全部更新；该路径不伪造运行期 Publish ACK；
  - 运行期 `refresh(DdcPublishMessage)` 的合法更新产生 SUCCESS ACK；非法 `idle > absolute` 产生 FAILED ACK，Policy、Repository currentVersion 和 Checksum 仍为旧值；
  - 同版本同 Checksum为 ignored，低版本为 ignored，同版本不同 Checksum 按 DDC 现有冲突语义执行；
  - 后续更高合法版本能从 LKG 恢复并产生 SUCCESS ACK；
  - ACK、异常、Runtime Status 均不包含 rawValue；
  - CONFIG_CLIENT session 与 fake HTTP_PROVIDER lease ID 不相等、状态字段不互相推导；
  - Definition ACCEPTED + Provider REGISTERED + Release 非 SUCCESS 时汇总仍为 NOT_ROUTABLE；
  - Release SUCCESS 但 Provider 过期时仍为 NOT_ROUTABLE。

- [ ] **Step 2：运行 RED**

  ```bash
  ./mvnw -B -ntp \
    -pl :egon-cola-platform-rbac3-admin -am \
    -Dtest=Rbac3DdcRefreshIntegrationTest,GatewayDdcConfigurationTest,RuntimeQueryServiceTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
  ```

- [ ] **Step 3：只修复跨边界失败，不复制 DDC 算法**

  如果测试失败，优先修正 RBAC3 exact Applier 注册、优先级、状态映射或 Gate；不得在 RBAC3 重写 `DdcRefreshService` 的版本/Checksum/ACK 流程，也不得把跨 Key 更新包装成不存在的事务。

- [ ] **Step 4：运行 GREEN、模块测试和本机依赖 IT**

  ```bash
  ./mvnw -B -ntp \
    -pl :egon-cola-platform-rbac3-admin -am \
    -Dtest=Rbac3DdcRefreshIntegrationTest,GatewayDdcConfigurationTest,RuntimeQueryServiceTest \
    -Dsurefire.failIfNoSpecifiedTests=false test

  ./mvnw -B -ntp \
    -pl :egon-cola-platform-rbac3-admin -am \
    -Prbac3-local-it verify
  git diff --check
  ```

  本机 Redis/PostgreSQL 不可用时，第二条命令必须保留失败证据并在最终结果明确标为环境阻塞；不得改用容器掩盖。

- [ ] **Step 5：提交 Task 8**

  ```bash
  git add \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/Rbac3DdcRefreshIntegrationTest.java \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/GatewayDdcConfigurationTest.java \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/runtime/RuntimeQueryServiceTest.java
  git diff --name-only -- \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/ddc \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime
  git commit -m "test(rbac3): verify DDC fail-safe integration"
  ```

  Expected: 若 Step 3 没有暴露生产缺陷，两个 main 目录列表为空。若有修复，只把测试证明所需的具体文件以完整路径追加到 `git add`，不得使用目录级 `git add -u`。

---

### Task 9：更新运维文档、状态说明和静态验收脚本

**Files:**

- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/README.zh-CN.md`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/README.md`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/docs/architecture.md`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/docs/operations-runbook.md`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/docs/verification-evidence-template.md`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/scripts/verification/verify-static.sh`

**Documentation must state:**

- 配置 scope 与服务 scope 的区别；
- 五个 Key、默认值、范围、关系和安全发布顺序；
- 动态配置只影响新 Token/Session/激活命令；
- CONFIG_CLIENT 与 HTTP_PROVIDER 是独立 Lease；
- DDC READY → Provider 发布的启动顺序；
- Gateway Interface Catalog 是唯一文档中心，Release 必须显式发布；
- 五事实状态模型、常见故障、LKG 和恢复步骤；
- Secret/Bootstrap 禁入项；
- Maven/静态验证不等于真实外部拓扑证明。

- [ ] **Step 1：先扩展静态脚本并确认 RED**

  `verify-static.sh` 增加确定性检查：

  - 生产 `ddc.enabled: true` 与 `registry.enabled: true`；
  - 五个 Key 各出现一次声明且为 `refreshable = false`；
  - `gatewayHttpProviderServerReadyListener` 由 RBAC3 提供；
  - 四个固定低基数 Metric 名称与 `key/status` 白名单实现存在；
  - Controller 的 Gateway 文档契约测试存在；
  - RBAC3 仍只有两个既有 Flyway 文件；
  - 不存在独立 `egon-cola-platform-rbac3-test` 模块；
  - 不存在 Swagger/Springdoc 依赖；
  - 生产 YAML 不含 localhost/默认 Secret。

  在文档和实现尚未全部满足前执行：

  ```bash
  bash egon-cola-platforms/egon-cola-platform-rbac3/scripts/verification/verify-static.sh
  ```

  Expected: 新增的文档/配置断言至少一项失败；失败信息要指出文件和缺少的契约。

- [ ] **Step 2：补齐中英文 README、架构、Runbook 和证据模板**

  Runbook 必须给出不泄密的排查顺序：

  ```text
  DDC Config Client state/session
  → current five config versions / last apply error code
  → Gateway Definition status
  → DDC HTTP_PROVIDER lease
  → Gateway Release/Consistency
  → routed request evidence
  ```

  配置发布示例必须明确跨 Key 非事务及顺序：扩大时先 Refresh、再 Absolute、最后 Idle；收缩时先 Idle、再 Absolute、最后按关系调整 Refresh。不得给出带真实 Access Key/Secret 的命令。

- [ ] **Step 3：运行 GREEN、链接和禁止项检查**

  ```bash
  bash egon-cola-platforms/egon-cola-platform-rbac3/scripts/verification/verify-static.sh
  rg -n 'swagger|springdoc|localhost|127\.0\.0\.1' \
    egon-cola-platforms/egon-cola-platform-rbac3/README.md \
    egon-cola-platforms/egon-cola-platform-rbac3/README.zh-CN.md \
    egon-cola-platforms/egon-cola-platform-rbac3/docs \
    egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/resources/application.yml
  git diff --check
  ```

  `rg` 发现 runbook 中明确标注的本地验证地址可以保留；生产配置、依赖或推荐拓扑中不得出现旁路回退。

- [ ] **Step 4：提交 Task 9**

  ```bash
  git add \
    egon-cola-platforms/egon-cola-platform-rbac3/README.md \
    egon-cola-platforms/egon-cola-platform-rbac3/README.zh-CN.md \
    egon-cola-platforms/egon-cola-platform-rbac3/docs/architecture.md \
    egon-cola-platforms/egon-cola-platform-rbac3/docs/operations-runbook.md \
    egon-cola-platforms/egon-cola-platform-rbac3/docs/verification-evidence-template.md \
    egon-cola-platforms/egon-cola-platform-rbac3/scripts/verification/verify-static.sh
  git commit -m "docs(rbac3): document DDC and gateway operations"
  ```

---

### Task 10：一次性 clean verification、提交审计与交付门槛

**Files:**

- Verify only: all RBAC3 modules and directly affected DDC/Gateway modules.
- Modify only when a verification failure proves a regression caused by Tasks 1–9; fixes go into the owning task's file scope and require a focused regression test.
- Do not create a generic “cleanup” refactor commit.

- [ ] **Step 1：逐条审计批准 Spec 的强制决策和验收标准**

  使用本计划末尾的追踪矩阵，逐项核对 IG-01..IG-18 与 AC-IG-01..AC-IG-10。执行：

  ```bash
  git log --oneline 63571e7b..HEAD
  git status --short
  git diff --check 63571e7b..HEAD
  git diff --name-only 63571e7b..HEAD
  ```

  Expected: 只有本计划列出的 RBAC3 文件；无迁移、无 Test 模块、无依赖变更、无无关重构。

- [ ] **Step 2：执行一次完整后端 clean verify**

  ```bash
  ./mvnw -B -ntp \
    -pl :egon-cola-platform-rbac3-contract,:egon-cola-platform-rbac3-core,:egon-cola-platform-rbac3-starter,:egon-cola-platform-rbac3-gateway-adapter,:egon-cola-platform-rbac3-admin,:egon-cola-platform-gateway-engine \
    -am clean verify
  ```

  必须读取最终 reactor summary 和失败测试报告；只有命令 exit 0 才能写“通过”。Maven/static 证据不能描述为真实 DDC/Gateway 多 JVM 路由成功。

- [ ] **Step 3：执行本机 Redis/PostgreSQL IT**

  ```bash
  ./mvnw -B -ntp \
    -pl :egon-cola-platform-rbac3-admin -am \
    -Prbac3-local-it verify
  ```

  不启动服务，不自动准备/清理用户数据库。若本机依赖不可用，记录准确命令、失败测试和根因边界；其余已通过证据仍分别报告。

- [ ] **Step 4：执行 RBAC3 Admin Web 离线回归**

  ```bash
  npm --prefix egon-cola-platforms/egon-cola-platform-rbac3 ci
  npm --prefix egon-cola-platforms/egon-cola-platform-rbac3 run typecheck
  npm --prefix egon-cola-platforms/egon-cola-platform-rbac3 test -- --run
  npm --prefix egon-cola-platforms/egon-cola-platform-rbac3 run lint
  npm --prefix egon-cola-platforms/egon-cola-platform-rbac3 run build
  npm --prefix egon-cola-platforms/egon-cola-platform-rbac3 run e2e \
    --workspace @egon-cola/rbac3-admin-web -- --list
  ```

  该组验证用于证明新增 DDC Config Client 卡片、Runtime Status 类型和既有页面均未回归。不得打开浏览器或运行需要外部服务的 Playwright 场景。

- [ ] **Step 5：重复静态验收并审计最终工作树**

  ```bash
  bash egon-cola-platforms/egon-cola-platform-rbac3/scripts/verification/verify-static.sh
  git diff --check
  git status --short --branch
  git log --oneline -10
  ```

  如果验证修复产生改动，回到其所属 Task：补 RED/GREEN 证据、只暂存对应文件、使用 `fix(rbac3): ...` 的独立提交，再重跑 Step 2–5。不得把多个失败用一个未说明的最终提交掩盖。

- [ ] **Step 6：停止在交付边界**

  最终答复必须报告：

  - 完成的提交与主要文件；
  - IG/AC 完成情况；
  - 每条实际执行的验证命令与结果；
  - 本机依赖或真实拓扑未验证的精确边界；
  - 没有启动 RBAC3/DDC/Gateway 服务；
  - 建议用户下一步按 Runbook 启动真实拓扑并审核五事实状态。

  不 push、不自动合并、不启动服务、不发布 Gateway Release。

## Spec 追踪矩阵

| Spec 条目 | 实施任务 | 主要证据 |
|---|---:|---|
| IG-01/02 Gateway 唯一文档中心与现有注解 | 7、9 | Mapping/Contributor 集合等价测试、README/Architecture |
| IG-03 配置客户端与注册客户端同时启用 | 5、6 | 生产 YAML、Context Test、独立状态 |
| IG-04/05 配置 scope 与服务 scope 分离 | 5、8、9 | YAML/作用域测试、Runbook |
| IG-06/07/08 类型化 Policy、声明与 exact Applier | 1、2 | 原子策略与配置装配测试 |
| IG-09/10 单 Key 原子、非法 ACK/LKG | 1、2、8 | Candidate Snapshot 与真实 Refresh 测试 |
| IG-11 只影响新 Token/Session | 3 | 两次签发/创建行为测试 |
| IG-12 规范根数量限制 | 4 | Activation IT 与 422 Error Code |
| IG-13 安全根不进 DDC | 5、9 | YAML/静态禁止项和文档 |
| IG-14 DDC READY 后发布 Provider | 6 | 事件顺序与最多一次发布测试 |
| IG-15 五事实独立 | 6、8 | Runtime Status 与路由组合测试 |
| IG-16 不自动发布 Release | 7、9、10 | 无发布代码、文档和 diff 审计 |
| IG-17 无数据库变化 | 9、10 | Flyway 数量静态检查、最终 diff |
| IG-18 无独立 Test 模块 | 9、10 | 静态脚本、最终模块审计 |
| AC-IG-01 配置客户端闭环 | 2、5、8 | DDC declaration/refresh/context 测试 |
| AC-IG-02 动态配置生效 | 3、4 | Token/Session/Activation 测试 |
| AC-IG-03 非法配置 Fail Safe | 1、2、8 | Snapshot/LKG/FAILED ACK 测试 |
| AC-IG-04 独立服务租约 | 5、6、8 | Config session 与 Provider lease 状态 |
| AC-IG-05 Provider 发布顺序 | 6 | Publication Gate 测试 |
| AC-IG-06 Gateway 文档中心 | 7 | 全 Mapping 覆盖与敏感字段测试 |
| AC-IG-07 显式 Release | 7、9、10 | 文档、无自动发布实现、diff 审计 |
| AC-IG-08 DDC 服务发现 | 5、8、10 | 既有 Gateway/DDC 回归与配置测试 |
| AC-IG-09 安全配置边界 | 5、9 | Secret/Bootstrap 禁止项 |
| AC-IG-10 全量回归 | 8、10 | clean verify、本机 IT、前端离线验证 |

## 完成定义

只有以下条件同时满足，实施任务才可声明完成：

1. Tasks 1–9 每个独立提交均存在且任务验证通过；
2. Task 10 后端 clean verify、静态验证和前端离线回归均有实际 exit code 证据；
3. 本机 IT 通过，或以准确环境阻塞单独报告而不冒充通过；
4. 生产配置同时启用 CONFIG_CLIENT 与 Registry，local/test 行为保持关闭；
5. 五个动态 Key 通过 exact Applier 更新同一不可变 Policy，非法值保留 LKG；
6. JWT、Session 和激活命令使用新策略，既有凭证/Session 不追溯修改；
7. Provider 只在 DDC READY 后发布，状态 API 与 Readiness 保留独立事实；
8. Spring Mapping 与 Gateway Catalog Definition 集合完全一致；
9. 没有新增/修改 Flyway、Test 模块、Swagger/Springdoc、静态 Provider URL 或默认 Secret；
10. 没有启动服务、打开浏览器、push 或发布 Gateway Release。
