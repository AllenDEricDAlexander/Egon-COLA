package top.egon.cola.component.rpc.provider.server;

import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.ClientCalls;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;
import top.egon.cola.component.rpc.contract.descriptor.RpcContractDescriptor;
import top.egon.cola.component.rpc.contract.validation.RpcContractValidator;
import top.egon.cola.component.rpc.provider.binding.RpcProviderBinding;
import top.egon.cola.component.rpc.provider.binding.RpcProviderMethodRegistry;
import top.egon.cola.component.rpc.provider.lifecycle.RpcProviderAvailabilityRegistry;
import top.egon.cola.component.rpc.support.RpcProviderTestFixtures;
import top.egon.cola.component.rpc.support.TestGrpcDescriptorFixtures.UnaryFixtureGrpc;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RpcServerServiceDefinitionFactoryTest {

    private final RpcContractValidator validator = new RpcContractValidator();

    @Test
    void shouldKeepBlockingProviderPath() throws Exception {
        try (RunningServer running = start(
                RpcProviderTestFixtures.EchoContract.class,
                new RpcProviderTestFixtures.EchoProvider(),
                List.of())) {
            Message response = ClientCalls.blockingUnaryCall(
                    running.channel,
                    running.method,
                    CallOptions.DEFAULT,
                    StringValue.of("request")
            );

            assertThat(response).isEqualTo(StringValue.of("provider:request"));
        }
    }

    @Test
    void shouldBridgeAsyncCompletionToGrpcObserver() throws Exception {
        AsyncProvider provider = new AsyncProvider();
        provider.response = CompletableFuture.completedFuture(
                StringValue.of("async-response")
        );
        try (RunningServer running = start(AsyncContract.class, provider, List.of())) {
            Message response = ClientCalls.blockingUnaryCall(
                    running.channel,
                    running.method,
                    CallOptions.DEFAULT,
                    StringValue.of("request")
            );

            assertThat(response).isEqualTo(StringValue.of("async-response"));
        }
    }

    @Test
    void shouldMapAsyncCompletionFailureThroughExistingMapperChain()
            throws Exception {
        AsyncProvider provider = new AsyncProvider();
        provider.response = CompletableFuture.failedFuture(
                new IllegalStateException("provider-secret")
        );
        RpcProviderExceptionMapper mapper = throwable -> Optional.of(
                Status.INVALID_ARGUMENT
                        .withDescription("mapped provider failure")
                        .asRuntimeException()
        );

        try (RunningServer running = start(
                AsyncContract.class,
                provider,
                List.of(mapper))) {
            assertThatThrownBy(() -> ClientCalls.blockingUnaryCall(
                    running.channel,
                    running.method,
                    CallOptions.DEFAULT,
                    StringValue.of("request")
            ))
                    .isInstanceOf(StatusRuntimeException.class)
                    .satisfies(throwable -> assertThat(
                            ((StatusRuntimeException) throwable)
                                    .getStatus().getCode())
                            .isEqualTo(Status.Code.INVALID_ARGUMENT))
                    .satisfies(throwable -> assertThat(
                            ((StatusRuntimeException) throwable)
                                    .getStatus().getDescription())
                            .isEqualTo("mapped provider failure"));
        }
    }

    @Test
    void shouldRejectNullAsyncStageAndNullAsyncValue() throws Exception {
        AsyncProvider nullStage = new AsyncProvider();
        nullStage.response = null;
        try (RunningServer running = start(
                AsyncContract.class,
                nullStage,
                List.of())) {
            assertInternalFailure(running);
        }

        AsyncProvider nullValue = new AsyncProvider();
        nullValue.response = CompletableFuture.completedFuture(null);
        try (RunningServer running = start(
                AsyncContract.class,
                nullValue,
                List.of())) {
            assertInternalFailure(running);
        }
    }

    @Test
    void shouldCancelFutureWhenGrpcContextIsCancelled() throws Exception {
        CancellableProvider provider = new CancellableProvider();
        try (RunningServer running = start(
                AsyncContract.class,
                provider,
                List.of())) {
            ListenableFuture<Message> call = ClientCalls.futureUnaryCall(
                    running.channel.newCall(running.method, CallOptions.DEFAULT),
                    StringValue.of("request")
            );

            assertThat(provider.started.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(call.cancel(true)).isTrue();
            assertThat(provider.cancelled.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(call.isCancelled()).isTrue();
        }
    }

    private void assertInternalFailure(RunningServer running) {
        assertThatThrownBy(() -> ClientCalls.blockingUnaryCall(
                running.channel,
                running.method,
                CallOptions.DEFAULT,
                StringValue.of("request")
        ))
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(throwable -> assertThat(
                        ((StatusRuntimeException) throwable).getStatus().getCode())
                        .isEqualTo(Status.Code.INTERNAL));
    }

    private RunningServer start(
            Class<?> contractType,
            Object provider,
            List<RpcProviderExceptionMapper> mappers) throws Exception {
        RpcContractDescriptor contract = validator.validate(contractType);
        RpcProviderBinding binding = new RpcProviderBinding(provider, contract);
        RpcProviderMethodRegistry registry =
                new RpcProviderMethodRegistry(List.of(binding));
        RpcProviderAvailabilityRegistry availability =
                new RpcProviderAvailabilityRegistry();
        availability.available(binding.serviceIdentity());
        Server server = NettyServerBuilder.forPort(0)
                .addService(new RpcServerServiceDefinitionFactory(
                        availability,
                        mappers
                ).create(registry).getFirst())
                .build()
                .start();
        ManagedChannel channel = ManagedChannelBuilder.forAddress(
                        "127.0.0.1",
                        server.getPort()
                )
                .usePlaintext()
                .build();
        @SuppressWarnings("unchecked")
        MethodDescriptor<Message, Message> method =
                (MethodDescriptor<Message, Message>) (MethodDescriptor<?, ?>)
                        contract.methods().getFirst().grpcMethod();
        return new RunningServer(server, channel, method);
    }

    @EgonRpcService(
            grpcClass = UnaryFixtureGrpc.class,
            group = "test",
            version = "1.0.0"
    )
    interface AsyncContract {

        @EgonRpcMethod(name = "Echo")
        CompletionStage<StringValue> echo(StringValue request);
    }

    private static class AsyncProvider implements AsyncContract {

        private CompletionStage<StringValue> response;

        @Override
        public CompletionStage<StringValue> echo(StringValue request) {
            return response;
        }
    }

    private static final class CancellableProvider extends AsyncProvider {

        private final CountDownLatch started = new CountDownLatch(1);

        private final CountDownLatch cancelled = new CountDownLatch(1);

        @Override
        public CompletionStage<StringValue> echo(StringValue request) {
            CancellableFuture future = new CancellableFuture(cancelled);
            started.countDown();
            return future;
        }
    }

    private static final class CancellableFuture
            extends CompletableFuture<StringValue> {

        private final CountDownLatch cancelled;

        private CancellableFuture(CountDownLatch cancelled) {
            this.cancelled = cancelled;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            boolean result = super.cancel(mayInterruptIfRunning);
            if (result) {
                cancelled.countDown();
            }
            return result;
        }
    }

    private static final class RunningServer implements AutoCloseable {

        private final Server server;

        private final ManagedChannel channel;

        private final MethodDescriptor<Message, Message> method;

        private RunningServer(
                Server server,
                ManagedChannel channel,
                MethodDescriptor<Message, Message> method) {
            this.server = server;
            this.channel = channel;
            this.method = method;
        }

        @Override
        public void close() throws Exception {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
            server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
