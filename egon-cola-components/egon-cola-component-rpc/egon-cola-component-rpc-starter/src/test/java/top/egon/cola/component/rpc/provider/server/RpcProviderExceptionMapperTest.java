package top.egon.cola.component.rpc.provider.server;

import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.ClientCalls;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.contract.descriptor.RpcContractDescriptor;
import top.egon.cola.component.rpc.contract.validation.RpcContractValidator;
import top.egon.cola.component.rpc.provider.binding.RpcProviderBinding;
import top.egon.cola.component.rpc.provider.binding.RpcProviderMethodRegistry;
import top.egon.cola.component.rpc.provider.lifecycle.RpcProviderAvailabilityRegistry;
import top.egon.cola.component.rpc.support.RpcProviderTestFixtures;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RpcProviderExceptionMapperTest {

    private static final Metadata.Key<String> DETAIL =
            Metadata.Key.of("x-test-detail", Metadata.ASCII_STRING_MARSHALLER);

    @Test
    void evaluatesOrderedMappersAndPreservesMappedTrailers() throws Exception {
        List<String> order = new ArrayList<>();
        RpcProviderExceptionMapper first = throwable -> {
            order.add("first");
            return Optional.empty();
        };
        RpcProviderExceptionMapper second = throwable -> {
            order.add("second");
            Metadata trailers = new Metadata();
            trailers.put(DETAIL, "typed-detail");
            return Optional.of(Status.INVALID_ARGUMENT
                    .withDescription("typed failure")
                    .asRuntimeException(trailers));
        };

        StatusRuntimeException failure = invoke(List.of(first, second));

        assertThat(order).containsExactly("first", "second");
        assertThat(failure.getStatus().getCode())
                .isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(failure.getTrailers().get(DETAIL))
                .isEqualTo("typed-detail");
    }

    @Test
    void sanitizesUnknownProviderFailures() throws Exception {
        StatusRuntimeException failure = invoke(List.of());

        assertThat(failure.getStatus().getCode())
                .isEqualTo(Status.Code.INTERNAL);
        assertThat(failure.getStatus().getDescription())
                .isEqualTo("RPC provider invocation failed")
                .doesNotContain("provider-secret");
    }

    private StatusRuntimeException invoke(
            List<RpcProviderExceptionMapper> mappers) throws Exception {
        RpcContractDescriptor contract = new RpcContractValidator().validate(
                RpcProviderTestFixtures.EchoContract.class
        );
        RpcProviderBinding binding = new RpcProviderBinding(
                new ThrowingProvider(),
                contract
        );
        RpcProviderMethodRegistry registry =
                new RpcProviderMethodRegistry(List.of(binding));
        RpcProviderAvailabilityRegistry availability =
                new RpcProviderAvailabilityRegistry();
        availability.available(binding.serviceIdentity());
        List<io.grpc.ServerServiceDefinition> services =
                new RpcServerServiceDefinitionFactory(
                        availability,
                        mappers
                ).create(registry);
        Server server = NettyServerBuilder.forPort(0)
                .addService(services.getFirst())
                .build()
                .start();
        ManagedChannel channel = ManagedChannelBuilder.forAddress(
                        "127.0.0.1",
                        server.getPort()
                )
                .usePlaintext()
                .build();
        try {
            @SuppressWarnings("unchecked")
            io.grpc.MethodDescriptor<Message, Message> method =
                    contract.methods().getFirst().grpcMethod();
            return org.assertj.core.api.Assertions.catchThrowableOfType(
                    () -> ClientCalls.blockingUnaryCall(
                            channel,
                            method,
                            io.grpc.CallOptions.DEFAULT,
                            StringValue.of("request")
                    ),
                    StatusRuntimeException.class
            );
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
            server.shutdownNow().awaitTermination();
        }
    }

    private static final class ThrowingProvider
            implements RpcProviderTestFixtures.EchoContract {

        @Override
        public StringValue echo(StringValue request) {
            throw new IllegalStateException("provider-secret");
        }
    }
}
