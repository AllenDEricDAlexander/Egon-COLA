package top.egon.cola.component.rpc.provider.server;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import top.egon.cola.component.accessguard.common.exception.AccessGuardRejectedException;
import top.egon.cola.component.accessguard.core.GuardDecision;
import top.egon.cola.component.rpc.context.invocation.RpcFailureStage;
import top.egon.cola.component.rpc.context.invocation.RpcMetadataKeys;

import java.util.Optional;

/** Maps only Access Guard rate-limit rejections to Provider UNAVAILABLE. */
public final class RpcAccessGuardExceptionMapper
        implements RpcProviderExceptionMapper {

    @Override
    public Optional<StatusRuntimeException> map(Throwable throwable) {
        if (!(throwable instanceof AccessGuardRejectedException rejected)
                || rejected.outcome().decision() != GuardDecision.RATE_LIMITED) {
            return Optional.empty();
        }
        Metadata trailers = new Metadata();
        RpcFailureStage.PROVIDER.put(trailers);
        trailers.put(RpcMetadataKeys.ERROR_TYPE, "rate-limit");
        return Optional.of(Status.UNAVAILABLE
                .withDescription("RPC provider rate limited")
                .asRuntimeException(trailers));
    }
}
