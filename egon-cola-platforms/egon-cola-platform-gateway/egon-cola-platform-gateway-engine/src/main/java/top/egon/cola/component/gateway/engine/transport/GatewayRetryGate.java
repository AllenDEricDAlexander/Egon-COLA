package top.egon.cola.component.gateway.engine.transport;

import top.egon.cola.component.gateway.contract.rule.GatewayRequestBodyMode;
import top.egon.cola.component.gateway.contract.rule.GatewayRouteProfile;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportResponseMode;
import top.egon.cola.component.gateway.core.transport.EffectiveGatewayTransportPolicy;

import java.util.Objects;

/**
 * Pure retry policy that keeps streaming commit facts out of retry libraries.
 */
public final class GatewayRetryGate {

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
