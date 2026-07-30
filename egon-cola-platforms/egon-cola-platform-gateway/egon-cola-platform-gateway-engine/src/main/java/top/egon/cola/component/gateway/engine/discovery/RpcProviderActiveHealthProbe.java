package top.egon.cola.component.gateway.engine.discovery;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.engine.rpc.RpcProviderChannelCache;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public final class RpcProviderActiveHealthProbe
        implements ProviderActiveHealthProbe {

    private final RpcProviderChannelCache channels;

    public RpcProviderActiveHealthProbe(
            RpcProviderChannelCache channels) {
        this.channels = channels;
    }

    @Override
    public Mono<Boolean> probe(
            ProviderInstance instance,
            ActiveHealthProbePolicy policy) {
        return Mono.fromCallable(() -> check(instance, policy));
    }

    private boolean check(
            ProviderInstance instance,
            ActiveHealthProbePolicy policy) {
        try (RpcProviderChannelCache.ChannelHandle handle =
                     channels.acquire(instance)) {
            HealthCheckResponse response = HealthGrpc.newBlockingStub(
                            handle.channel()
                    )
                    .withDeadlineAfter(
                            policy.timeout().toMillis(),
                            TimeUnit.MILLISECONDS
                    )
                    .check(HealthCheckRequest.newBuilder()
                            .setService(policy.rpcServiceName(instance))
                            .build());
            return response.getStatus()
                    == HealthCheckResponse.ServingStatus.SERVING;
        } catch (StatusRuntimeException unavailable) {
            if (policy.rpcConnectFallback()
                    && unavailable.getStatus().getCode()
                    == Status.Code.UNIMPLEMENTED) {
                return connect(instance, policy);
            }
            return false;
        }
    }

    private boolean connect(
            ProviderInstance instance,
            ActiveHealthProbePolicy policy) {
        try (Socket socket = new Socket()) {
            socket.connect(
                    new InetSocketAddress(
                            instance.host(),
                            instance.port()
                    ),
                    Math.toIntExact(Math.min(
                            Integer.MAX_VALUE,
                            policy.timeout().toMillis()
                    ))
            );
            return true;
        } catch (java.io.IOException failure) {
            return false;
        }
    }
}
