# Integration 05 RPC Idempotency and Recovery Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 RPC Consumer 在多 Gateway 下只对显式幂等方法进行安全故障转移，并准确分类 Gateway/Provider 失败；Gateway Slot 在租约故障后自动恢复。

**Architecture:** 幂等性属于 Java Contract method descriptor；failure-stage 属于公共 RPC wire metadata；Consumer 将二者与 Deadline、Status 共同决策。Gateway Slot 使用显式恢复状态和有界退避重新注册。

**Tech Stack:** grpc-java、Protobuf Descriptor、JDK Dynamic Proxy、DDC Registry、ScheduledExecutorService、JUnit 5。

## Global Constraints

- 依赖 Integration 01 状态合同。
- `@EgonRpcMethod.idempotent` 默认 false，保持旧业务方法安全。
- 只处理 unary RPC。
- Provider 阶段失败不得跨 Gateway 重试。
- wire metadata key 保持 `x-egon-rpc-failure-stage`。

---

### Task 1: 把幂等性加入 RPC Contract

**Files:**
- Modify: `.../rpc-starter/src/main/java/top/egon/cola/component/rpc/annotation/EgonRpcMethod.java`
- Modify: `.../rpc-starter/src/main/java/top/egon/cola/component/rpc/contract/RpcMethodDescriptor.java`
- Modify: `.../rpc-starter/src/main/java/top/egon/cola/component/rpc/contract/RpcContractValidator.java`
- Modify: `.../gateway-test-rpc-contract/src/main/java/top/egon/cola/component/gateway/test/rpc/contract/EchoRpc.java`
- Modify: `.../gateway-test-rpc-contract/src/main/java/top/egon/cola/component/gateway/test/rpc/contract/OrderRpc.java`
- Test: `.../rpc-starter/src/test/java/top/egon/cola/component/rpc/contract/RpcContractValidatorTest.java`

**Interfaces:**
- Produces `boolean EgonRpcMethod.idempotent() default false`.
- Produces `RpcMethodDescriptor.idempotent()`.

- [ ] **Step 1: Write failing descriptor tests**

```java
@EgonRpcMethod(name = "Echo", idempotent = true)
EchoResponse echo(EchoRequest request);

assertThat(validator.validate(IdempotentContract.class)
        .methods().get(method).idempotent()).isTrue();
assertThat(validator.validate(LegacyContract.class)
        .methods().get(method).idempotent()).isFalse();
```

- [ ] **Step 2: Run contract tests**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-rpc/\
egon-cola-component-rpc-starter -am test \
  -Dtest=RpcContractValidatorTest,RpcContractSnapshotBuilderTest
```

- [ ] **Step 3: Add the annotation member and descriptor component**

```java
boolean idempotent() default false;
```

Pass `rpcMethod.idempotent()` from validator to the descriptor. Mark test Echo/GetOrder as idempotent and
CreateOrder as non-idempotent.

- [ ] **Step 4: Run RPC starter and contract module tests**

Expected: PASS without changing wire Proto descriptors.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-rpc \
        egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-rpc-contract
git commit -m "feat: declare rpc method idempotency"
```

### Task 2: 统一 typed failure-stage 和错误映射

**Files:**
- Create: `.../rpc-starter/src/main/java/top/egon/cola/component/rpc/context/RpcFailureStage.java`
- Modify: `.../rpc-starter/src/main/java/top/egon/cola/component/rpc/context/RpcMetadataKeys.java`
- Modify: `.../rpc-starter/src/main/java/top/egon/cola/component/rpc/exception/RpcStatusExceptionMapper.java`
- Modify: `.../gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/rpc/RpcGatewayForwarder.java`
- Test: `.../rpc-starter/src/test/java/top/egon/cola/component/rpc/exception/RpcStatusExceptionMapperTest.java`
- Test: `.../gateway-engine/src/test/java/top/egon/cola/component/gateway/engine/rpc/RpcGatewayForwarderTest.java`

**Interfaces:**
- Produces `RpcFailureStage.GATEWAY/PROVIDER`, `put(Metadata)` and `from(Metadata)`.
- Preserves existing wire values `gateway` and `provider`.

- [ ] **Step 1: Write failure classification tests**

```java
assertThat(mapper.map(unavailable(GATEWAY)).getCode())
        .isEqualTo(RPC_GATEWAY_UNAVAILABLE);
assertThat(mapper.map(unavailable(PROVIDER)).getCode())
        .isEqualTo(RPC_PROVIDER_UNAVAILABLE);
assertThat(mapper.map(Status.UNIMPLEMENTED.asRuntimeException()).getCode())
        .isEqualTo(RPC_METHOD_NOT_FOUND);
```

Gateway test asserts `gatewayTrailers("GATEWAY_RPC_METADATA_MISMATCH", trace)` writes stage `gateway`, not the
error code; proxied Provider trailers preserve/force stage `provider`.

- [ ] **Step 2: Run mapper/forwarder tests**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter,\
egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-engine \
  -am test -Dtest=RpcStatusExceptionMapperTest,RpcGatewayForwarderTest
```

- [ ] **Step 3: Implement typed stage helpers and repair Gateway trailers**

```java
public enum RpcFailureStage {
    GATEWAY("gateway"), PROVIDER("provider");
    // wireValue(), put(Metadata), static from(Metadata)
}
```

`gatewayTrailers` receives stage separately from error code. Map gRPC `UNIMPLEMENTED` to method-not-found;
do not use absence of stage as proof of a retryable Gateway failure.

- [ ] **Step 4: Run RPC + Gateway Engine tests**

Expected: PASS with all production `FAILURE_STAGE` writes using the enum.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-rpc \
        egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-engine
git commit -m "fix: classify rpc gateway and provider failures"
```

### Task 3: 只为幂等方法执行有界 Gateway 故障转移

**Files:**
- Modify: `.../rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/RpcConsumerInvocationHandler.java`
- Test: `.../rpc-starter/src/test/java/top/egon/cola/component/rpc/consumer/RpcConsumerInvocationHandlerTest.java`

**Interfaces:**
- Consumes `RpcMethodDescriptor.idempotent()` and `RpcFailureStage`.
- Produces at most `min(gatewayMaxAttempts, distinctGateways)` attempts inside original Deadline.

- [ ] **Step 1: Add retry matrix tests**

```text
idempotent + GATEWAY UNAVAILABLE + second gateway -> retries and succeeds
non-idempotent + GATEWAY UNAVAILABLE             -> one attempt
idempotent + PROVIDER UNAVAILABLE                -> one attempt
idempotent + missing stage                       -> one attempt
idempotent + DEADLINE_EXCEEDED                   -> one attempt
```

Assert the same request and invocation metadata are used and total elapsed time never resets Deadline.

- [ ] **Step 2: Run invocation tests**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-rpc/\
egon-cola-component-rpc-starter -am test \
  -Dtest=RpcConsumerInvocationHandlerTest
```

- [ ] **Step 3: Replace permissive retry decision**

```java
private boolean retryableGatewayFailure(
        RpcMethodDescriptor method, StatusRuntimeException failure) {
    return method.idempotent()
            && failure.getStatus().getCode() == Status.Code.UNAVAILABLE
            && RpcFailureStage.from(failure.getTrailers())
                    .filter(stage -> stage == GATEWAY)
                    .isPresent();
}
```

Do not call `recordFailure` for Provider failures.

- [ ] **Step 4: Run full RPC starter tests**

Expected: PASS and non-idempotent methods have one transport call.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter
git commit -m "fix: restrict rpc gateway retries to idempotent calls"
```

### Task 4: 恢复 Gateway Slot 租约并统一默认 identity

**Files:**
- Modify: `.../gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/rpc/RpcGatewaySubsystemState.java`
- Modify: `.../gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/rpc/RpcGatewaySlotRuntime.java`
- Modify: `.../rpc-starter/src/main/java/top/egon/cola/component/rpc/config/EgonRpcProperties.java`
- Modify: `.../gateway-engine/src/main/resources/application.yml`
- Modify: `.../gateway-test-rpc-consumer/src/main/resources/application.yml`
- Modify: `.../gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/live/GatewayLiveTopologyIT.java`
- Test: `.../gateway-engine/src/test/java/top/egon/cola/component/gateway/engine/rpc/RpcGatewaySlotRuntimeTest.java`
- Test: `.../rpc-starter/src/test/java/top/egon/cola/component/rpc/config/EgonRpcPropertiesTest.java`

**Interfaces:**
- Produces `RECOVERING` state and re-registration with a new leaseId.
- Produces default service name `egon-gateway-rpc` across Engine/Consumer/tests/deployment.

- [ ] **Step 1: Write heartbeat exception/recovery tests**

```java
runtime.engineReady();
registry.failHeartbeatOnce();
runtime.heartbeatAndRecover();
assertThat(runtime.state()).isEqualTo(RECOVERING);
runtime.heartbeatAndRecover();
assertThat(runtime.state()).isEqualTo(REGISTERED_READY);
assertThat(runtime.lease().orElseThrow().leaseId()).isNotEqualTo(firstLease);
```

Add close-during-recovery and repeated register failure cases; close must prevent later scheduled registration.

- [ ] **Step 2: Run slot/default tests**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-engine,\
egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter \
  -am test -Dtest=RpcGatewaySlotRuntimeTest,EgonRpcPropertiesTest
```

- [ ] **Step 3: Implement bounded recovery state**

On heartbeat exception/NOT_FOUND clear the lease, enter RECOVERING, and let the existing fixed-delay task retry
registration. Only explicit drain/close shuts down the scheduler. Keep last failure observable; do not move to a
terminal FAILED state for transient DDC errors.

- [ ] **Step 4: Run Gateway Engine and RPC suites**

Expected: PASS with multi-Slot Consumer discovery.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-gateway \
        egon-cola-components/egon-cola-component-rpc
git commit -m "fix: recover rpc gateway slot leases"
```
