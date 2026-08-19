package top.egon.cola.component.gateway.engine.common.traffic.service;

import top.egon.cola.component.gateway.engine.common.traffic.domain.GatewayRetryPolicy;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 中文说明：{@code GatewayAttemptExecutor} 是类型，位于当前 Gateway 模块的相关包中，负责网关AttemptExecutor相关的职责与边界。
 * English summary: {@code GatewayAttemptExecutor} is a type in the current Gateway module; it owns the gateway attempt executor-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayAttemptExecutor {

    /**
     * 中文说明：执行 execute 操作；该方法是 {@code GatewayAttemptExecutor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the execute operation; this method is the invocation entry point on {@code GatewayAttemptExecutor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAttemptExecutor.execute(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policy 参数 策略；parameter policy。
     * @param idempotent 参数 idempotent；parameter idempotent。
     * @param replayableBody 参数 replayableBody；parameter replayable body。
     * @param totalBudget 参数 totalBudget；parameter total budget。
     * @param attempt 参数 attempt；parameter attempt。
     * @param retryable 参数 retryable；parameter retryable。
     * @return 返回 execute 的处理结果；returns the result of the operation.
     */
    public <T> Mono<T> execute(
            GatewayRetryPolicy policy,
            boolean idempotent,
            boolean replayableBody,
            Duration totalBudget,
            Supplier<Mono<T>> attempt,
            Predicate<Throwable> retryable) {
        Objects.requireNonNull(policy, "policy");
        Objects.requireNonNull(totalBudget, "totalBudget");
        Objects.requireNonNull(attempt, "attempt");
        Objects.requireNonNull(retryable, "retryable");
        if (totalBudget.isZero() || totalBudget.isNegative()) {
            return Mono.error(new TimeoutException("GATEWAY_TIMEOUT"));
        }
        if (policy.enabled() && (!idempotent || !replayableBody)) {
            return Mono.error(new IllegalArgumentException(
                    "retry requires an idempotent operation and replayable body"
            ));
        }
        long deadline = System.nanoTime() + totalBudget.toNanos();
        return executeAttempt(
                policy,
                attempt,
                retryable,
                deadline,
                1
        ).timeout(totalBudget);
    }

    /**
     * 中文说明：执行 executeAttempt 操作；该方法是 {@code GatewayAttemptExecutor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the execute attempt operation; this method is the invocation entry point on {@code GatewayAttemptExecutor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAttemptExecutor.executeAttempt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policy 参数 策略；parameter policy。
     * @param attempt 参数 attempt；parameter attempt。
     * @param retryable 参数 retryable；parameter retryable。
     * @param deadline 参数 deadline；parameter deadline。
     * @param attemptNumber 参数 attemptNumber；parameter attempt number。
     * @return 返回 executeAttempt 的处理结果；returns the result of the operation.
     */
    private <T> Mono<T> executeAttempt(
            GatewayRetryPolicy policy,
            Supplier<Mono<T>> attempt,
            Predicate<Throwable> retryable,
            long deadline,
            int attemptNumber) {
        return Mono.defer(attempt).onErrorResume(failure -> {
            if (!policy.enabled()
                    || attemptNumber >= policy.maxAttempts()
                    || !retryable.test(failure)) {
                return Mono.error(failure);
            }
            Duration backoff = policy.backoff(attemptNumber);
            long remaining = deadline - System.nanoTime();
            long required = backoff.toNanos()
                    + policy.minimumAttemptBudget().toNanos();
            if (remaining < required) {
                return Mono.error(new TimeoutException("GATEWAY_TIMEOUT"));
            }
            return Mono.delay(backoff).then(executeAttempt(
                    policy,
                    attempt,
                    retryable,
                    deadline,
                    attemptNumber + 1
            ));
        });
    }
}
