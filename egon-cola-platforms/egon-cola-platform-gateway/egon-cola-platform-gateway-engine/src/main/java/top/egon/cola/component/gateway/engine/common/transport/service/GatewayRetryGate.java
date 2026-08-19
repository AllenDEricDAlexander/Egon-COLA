package top.egon.cola.component.gateway.engine.common.transport.service;

import top.egon.cola.component.gateway.contract.rule.GatewayRequestBodyMode;
import top.egon.cola.component.gateway.contract.rule.GatewayRouteProfile;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportResponseMode;
import top.egon.cola.component.gateway.core.transport.EffectiveGatewayTransportPolicy;

import java.util.Objects;

/**
 * Pure retry policy that keeps streaming commit facts out of retry libraries.
 * 补充说明 / Supplementary summary: {@code GatewayRetryGate} 是类型，位于当前 Gateway 模块的相关包中，负责网关重试Gate相关的职责与边界。
 * English supplement: {@code GatewayRetryGate} is a type in the current Gateway module; it owns the gateway retry gate-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayRetryGate {

    /**
     * 中文说明：执行 can重试传输Failure 操作；该方法是 {@code GatewayRetryGate} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the can retry transport failure operation; this method is the invocation entry point on {@code GatewayRetryGate} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRetryGate.canRetryTransportFailure(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policy 参数 策略；parameter policy。
     * @param commitGuard 参数 commitGuard；parameter commit guard。
     * @param retryPolicyEnabled 参数 重试策略Enabled；parameter retry policy enabled。
     * @param idempotent 参数 idempotent；parameter idempotent。
     * @param replayable 参数 replayable；parameter replayable。
     * @param attempt 参数 attempt；parameter attempt。
     * @param maxAttempts 参数 maxAttempts；parameter max attempts。
     * @return 返回 can重试传输Failure 的处理结果；returns the result of the operation.
     */
    public boolean canRetryTransportFailure(
            EffectiveGatewayTransportPolicy policy,
            GatewayCommitGuard commitGuard,
            boolean retryPolicyEnabled,
            boolean idempotent,
            boolean replayable,
            int attempt,
            int maxAttempts) {
        return eligible(
                policy,
                commitGuard,
                retryPolicyEnabled,
                idempotent,
                replayable,
                attempt,
                maxAttempts
        ) && !commitGuard.upstreamAccepted();
    }

    /**
     * 中文说明：执行 can重试LegacyStatus 操作；该方法是 {@code GatewayRetryGate} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the can retry legacy status operation; this method is the invocation entry point on {@code GatewayRetryGate} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRetryGate.canRetryLegacyStatus(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policy 参数 策略；parameter policy。
     * @param commitGuard 参数 commitGuard；parameter commit guard。
     * @param retryPolicyEnabled 参数 重试策略Enabled；parameter retry policy enabled。
     * @param idempotent 参数 idempotent；parameter idempotent。
     * @param replayable 参数 replayable；parameter replayable。
     * @param attempt 参数 attempt；parameter attempt。
     * @param maxAttempts 参数 maxAttempts；parameter max attempts。
     * @return 返回 can重试LegacyStatus 的处理结果；returns the result of the operation.
     */
    public boolean canRetryLegacyStatus(
            EffectiveGatewayTransportPolicy policy,
            GatewayCommitGuard commitGuard,
            boolean retryPolicyEnabled,
            boolean idempotent,
            boolean replayable,
            int attempt,
            int maxAttempts) {
        return eligible(
                policy,
                commitGuard,
                retryPolicyEnabled,
                idempotent,
                replayable,
                attempt,
                maxAttempts
        ) && !commitGuard.downstreamCommitted();
    }

    /**
     * 中文说明：执行 eligible 操作；该方法是 {@code GatewayRetryGate} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the eligible operation; this method is the invocation entry point on {@code GatewayRetryGate} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRetryGate.eligible(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policy 参数 策略；parameter policy。
     * @param commitGuard 参数 commitGuard；parameter commit guard。
     * @param retryPolicyEnabled 参数 重试策略Enabled；parameter retry policy enabled。
     * @param idempotent 参数 idempotent；parameter idempotent。
     * @param replayable 参数 replayable；parameter replayable。
     * @param attempt 参数 attempt；parameter attempt。
     * @param maxAttempts 参数 maxAttempts；parameter max attempts。
     * @return 返回 eligible 的处理结果；returns the result of the operation.
     */
    private boolean eligible(
            EffectiveGatewayTransportPolicy policy,
            GatewayCommitGuard commitGuard,
            boolean retryPolicyEnabled,
            boolean idempotent,
            boolean replayable,
            int attempt,
            int maxAttempts) {
        Objects.requireNonNull(policy, "policy");
        Objects.requireNonNull(commitGuard, "commitGuard");
        return isLegacyAggregatedHttp(policy)
                && policy.retryAllowed()
                && retryPolicyEnabled
                && idempotent
                && replayable
                && attempt > 0
                && attempt < maxAttempts
                && !commitGuard.terminated();
    }

    /**
     * 中文说明：执行 isLegacyAggregatedHttp 操作；该方法是 {@code GatewayRetryGate} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the is legacy aggregated http operation; this method is the invocation entry point on {@code GatewayRetryGate} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRetryGate.isLegacyAggregatedHttp(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policy 参数 策略；parameter policy。
     * @return 返回 isLegacyAggregatedHttp 的处理结果；returns the result of the operation.
     */
    private boolean isLegacyAggregatedHttp(
            EffectiveGatewayTransportPolicy policy) {
        return policy.profile() == GatewayRouteProfile.DEFAULT
                && policy.transportProtocol()
                == GatewayTransportProtocol.HTTP
                && policy.requestBodyMode()
                == GatewayRequestBodyMode.AGGREGATED
                && policy.responseMode()
                == GatewayTransportResponseMode.STANDARD;
    }
}
