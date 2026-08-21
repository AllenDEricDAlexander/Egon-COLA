package top.egon.cola.component.rpc.provider.server;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.accessguard.api.AccessGuardRejectedException;
import top.egon.cola.component.accessguard.core.GuardDecision;
import top.egon.cola.component.accessguard.core.GuardOutcome;
import top.egon.cola.component.rpc.context.invocation.RpcFailureStage;
import top.egon.cola.component.rpc.context.invocation.RpcMetadataKeys;

import static org.assertj.core.api.Assertions.assertThat;

class RpcAccessGuardExceptionMapperTest {

    private final RpcAccessGuardExceptionMapper mapper =
            new RpcAccessGuardExceptionMapper();

    @Test
    void mapsOnlyRateLimitRejectionToProviderUnavailable() {
        StatusRuntimeException mapped = mapper.map(new AccessGuardRejectedException(
                GuardOutcome.rejected(
                        "rpc.order.create",
                        GuardDecision.RATE_LIMITED,
                        "rate-limit",
                        1L)))
                .orElseThrow();

        assertThat(mapped.getStatus().getCode()).isEqualTo(Status.Code.UNAVAILABLE);
        assertThat(mapped.getStatus().getDescription())
                .isEqualTo("RPC provider rate limited");
        assertThat(mapped.getTrailers()).isNotNull();
        assertThat(RpcFailureStage.from(mapped.getTrailers()))
                .contains(RpcFailureStage.PROVIDER);
        assertThat(mapped.getTrailers().get(RpcMetadataKeys.ERROR_TYPE))
                .isEqualTo("rate-limit");
    }

    @Test
    void delegatesOtherGuardDecisionsAndUnknownFailures() {
        assertThat(mapper.map(new AccessGuardRejectedException(GuardOutcome.rejected(
                "rpc.order.create",
                GuardDecision.DENY_LIST_HIT,
                "deny-list",
                1L)))).isEmpty();
        assertThat(mapper.map(new IllegalStateException("business failure")))
                .isEmpty();
    }
}
