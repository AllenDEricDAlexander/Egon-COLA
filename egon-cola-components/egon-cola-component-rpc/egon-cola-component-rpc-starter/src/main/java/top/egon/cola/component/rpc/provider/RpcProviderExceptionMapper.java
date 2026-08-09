package top.egon.cola.component.rpc.provider;

import io.grpc.StatusRuntimeException;

import java.util.Optional;

/**
 * 将 Provider 领域异常映射为带状态和 Trailer 的 gRPC 异常。
 *
 * <p>Maps a Provider domain failure to a gRPC status and trailers.
 */
@FunctionalInterface
public interface RpcProviderExceptionMapper {

    /**
     * 尝试映射异常；空结果表示交给后续 Mapper 或默认处理。
     *
     * <p>Attempts to map a failure; empty delegates to the next mapper or the
     * default fallback.
     *
     * @param throwable Provider 抛出的异常 / Provider failure
     * @return 可选映射结果 / optional mapped failure
     */
    Optional<StatusRuntimeException> map(Throwable throwable);
}
