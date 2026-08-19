package top.egon.cola.component.gateway.engine.http.proxy.service;

import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.core.transport.EffectiveGatewayTransportPolicy;
import top.egon.cola.component.gateway.engine.common.traffic.service.GatewayAttemptExecutor;
import top.egon.cola.component.gateway.engine.common.traffic.domain.GatewayRetryPolicy;
import top.egon.cola.component.gateway.engine.common.transport.service.GatewayCommitGuard;
import top.egon.cola.component.gateway.engine.common.transport.service.GatewayRetryGate;
import top.egon.cola.component.gateway.engine.common.transport.service.GatewayTransportTimeouts;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Existing attempt orchestration constrained by transport commit facts.
 * 补充说明 / Supplementary summary: {@code GatewayHttpAttemptCoordinator} 是类型，位于当前 Gateway 模块的相关包中，负责网关HttpAttemptCoordinator相关的职责与边界。
 * English supplement: {@code GatewayHttpAttemptCoordinator} is a type in the current Gateway module; it owns the gateway http attempt coordinator-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayHttpAttemptCoordinator {

    /**
     * 中文说明：保存 executor 对应的状态、依赖或配置值；字段类型为 {@code GatewayAttemptExecutor}，由 {@code GatewayHttpAttemptCoordinator} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by executor; its type is {@code GatewayAttemptExecutor}, and {@code GatewayHttpAttemptCoordinator} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayHttpAttemptCoordinator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpAttemptCoordinator}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayAttemptExecutor executor = new GatewayAttemptExecutor();

    /**
     * 中文说明：保存 重试Gate 对应的状态、依赖或配置值；字段类型为 {@code GatewayRetryGate}，由 {@code GatewayHttpAttemptCoordinator} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by retry gate; its type is {@code GatewayRetryGate}, and {@code GatewayHttpAttemptCoordinator} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayHttpAttemptCoordinator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpAttemptCoordinator}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayRetryGate retryGate = new GatewayRetryGate();

    /**
     * 中文说明：执行 can重试LegacyStatus 操作；该方法是 {@code GatewayHttpAttemptCoordinator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the can retry legacy status operation; this method is the invocation entry point on {@code GatewayHttpAttemptCoordinator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpAttemptCoordinator.canRetryLegacyStatus(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param transportPolicy 参数 传输策略；parameter transport policy。
     * @param retryPolicy 参数 重试策略；parameter retry policy。
     * @param commitGuard 参数 commitGuard；parameter commit guard。
     * @param idempotent 参数 idempotent；parameter idempotent。
     * @param replayable 参数 replayable；parameter replayable。
     * @param attempt 参数 attempt；parameter attempt。
     * @return 返回 can重试LegacyStatus 的处理结果；returns the result of the operation.
     */
    public boolean canRetryLegacyStatus(
            EffectiveGatewayTransportPolicy transportPolicy,
            GatewayRetryPolicy retryPolicy,
            GatewayCommitGuard commitGuard,
            boolean idempotent,
            boolean replayable,
            int attempt) {
        Objects.requireNonNull(transportPolicy, "transportPolicy");
        Objects.requireNonNull(retryPolicy, "retryPolicy");
        Objects.requireNonNull(commitGuard, "commitGuard");
        return retryGate.canRetryLegacyStatus(
                transportPolicy,
                commitGuard,
                retryPolicy.enabled(),
                idempotent,
                replayable,
                attempt,
                retryPolicy.maxAttempts()
        );
    }

    /**
     * 中文说明：执行 execute 操作；该方法是 {@code GatewayHttpAttemptCoordinator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the execute operation; this method is the invocation entry point on {@code GatewayHttpAttemptCoordinator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpAttemptCoordinator.execute(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param transportPolicy 参数 传输策略；parameter transport policy。
     * @param retryPolicy 参数 重试策略；parameter retry policy。
     * @param commitGuard 参数 commitGuard；parameter commit guard。
     * @param idempotent 参数 idempotent；parameter idempotent。
     * @param replayable 参数 replayable；parameter replayable。
     * @param totalBudget 参数 totalBudget；parameter total budget。
     * @param attempt 参数 attempt；parameter attempt。
     * @param retryableTransportFailure 参数 retryable传输Failure；parameter retryable transport failure。
     * @param retryableLegacyStatus 参数 retryableLegacyStatus；parameter retryable legacy status。
     * @return 返回 execute 的处理结果；returns the result of the operation.
     */
    public <T> Mono<T> execute(
            EffectiveGatewayTransportPolicy transportPolicy,
            GatewayRetryPolicy retryPolicy,
            GatewayCommitGuard commitGuard,
            boolean idempotent,
            boolean replayable,
            Duration totalBudget,
            Supplier<Mono<T>> attempt,
            Predicate<Throwable> retryableTransportFailure,
            Predicate<Throwable> retryableLegacyStatus) {
        Objects.requireNonNull(transportPolicy, "transportPolicy");
        Objects.requireNonNull(retryPolicy, "retryPolicy");
        Objects.requireNonNull(commitGuard, "commitGuard");
        Objects.requireNonNull(attempt, "attempt");
        Objects.requireNonNull(
                retryableTransportFailure,
                "retryableTransportFailure"
        );
        Objects.requireNonNull(retryableLegacyStatus, "retryableLegacyStatus");
        boolean eligible = retryGate.canRetryTransportFailure(
                transportPolicy,
                commitGuard,
                retryPolicy.enabled(),
                idempotent,
                replayable,
                1,
                retryPolicy.maxAttempts()
        ) || canRetryLegacyStatus(
                transportPolicy,
                retryPolicy,
                commitGuard,
                idempotent,
                replayable,
                1
        );
        AtomicInteger attempts = new AtomicInteger();
        if (!eligible) {
            return GatewayTransportTimeouts.total(
                    Mono.defer(() -> {
                        attempts.incrementAndGet();
                        return attempt.get();
                    }),
                    transportPolicy.totalTimeout()
            );
        }
        return executor.execute(
                retryPolicy,
                idempotent,
                replayable,
                totalBudget,
                () -> {
                    attempts.incrementAndGet();
                    return attempt.get();
                },
                failure -> retryableLegacyStatus.test(failure)
                        ? canRetryLegacyStatus(
                                transportPolicy,
                                retryPolicy,
                                commitGuard,
                                idempotent,
                                replayable,
                                attempts.get()
                        )
                        : retryableTransportFailure.test(failure)
                        && retryGate.canRetryTransportFailure(
                                transportPolicy,
                                commitGuard,
                                retryPolicy.enabled(),
                                idempotent,
                                replayable,
                                attempts.get(),
                                retryPolicy.maxAttempts()
                        )
        );
    }
}
