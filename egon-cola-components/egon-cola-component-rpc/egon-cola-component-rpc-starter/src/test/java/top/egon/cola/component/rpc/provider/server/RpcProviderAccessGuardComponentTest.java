package top.egon.cola.component.rpc.provider.server;

import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ClientCalls;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.accessguard.api.RateLimitGuard;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcProvider;
import top.egon.cola.component.rpc.annotation.EgonRpcService;
import top.egon.cola.component.rpc.config.RpcAccessGuardAutoConfiguration;
import top.egon.cola.component.rpc.context.invocation.RpcFailureStage;
import top.egon.cola.component.rpc.context.invocation.RpcMetadataKeys;
import top.egon.cola.component.rpc.contract.validation.RpcContractValidator;
import top.egon.cola.component.rpc.provider.binding.RpcProviderBeanScanner;
import top.egon.cola.component.rpc.provider.binding.RpcProviderMethodRegistry;
import top.egon.cola.component.rpc.provider.lifecycle.RpcProviderAvailabilityRegistry;
import top.egon.cola.component.rpc.support.TestGrpcDescriptorFixtures.UnaryFixtureGrpc;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RpcProviderAccessGuardComponentTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            JacksonAutoConfiguration.class,
                            top.egon.cola.component.accessguard.autoconfigure
                                    .AccessGuardCoreAutoConfiguration.class,
                            top.egon.cola.component.accessguard.autoconfigure
                                    .AccessGuardLocalStoreAutoConfiguration.class,
                            top.egon.cola.component.accessguard.autoconfigure
                                    .AccessGuardTimeLimitAutoConfiguration.class,
                            top.egon.cola.component.accessguard.autoconfigure
                                    .AccessGuardAopAutoConfiguration.class,
                            RpcAccessGuardAutoConfiguration.class))
                    .withUserConfiguration(ProviderConfiguration.class)
                    .withPropertyValues(
                            "egon.cola.component.rpc.provider.enabled=true",
                            "egon.cola.component.access-guard.enabled=true",
                            "egon.cola.component.access-guard.key.hmac-secret=test-secret",
                            "egon.cola.component.access-guard.key.contributors[0]=GLOBAL",
                            "egon.cola.component.access-guard.rules.rpc-create.rate-limit.enabled=true",
                            "egon.cola.component.access-guard.rules.rpc-create.rate-limit.algorithm=TOKEN_BUCKET",
                            "egon.cola.component.access-guard.rules.rpc-create.rate-limit.capacity=1",
                            "egon.cola.component.access-guard.rules.rpc-create.rate-limit.refill-tokens=1",
                            "egon.cola.component.access-guard.rules.rpc-create.rate-limit.refill-period=PT1H",
                            "egon.cola.component.access-guard.rules.rpc-create.rate-limit.requested-tokens=1");

    @Test
    void rateLimitGuardRejectsBeforeBusinessAndMapsProviderUnavailable()
            throws Exception {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            GuardedProvider provider = context.getBean(GuardedProvider.class);
            RpcProviderMethodRegistry registry = new RpcProviderBeanScanner(
                    context,
                    new RpcContractValidator()).scan();
            RpcProviderAvailabilityRegistry availability =
                    new RpcProviderAvailabilityRegistry();
            registry.providers().forEach(binding ->
                    availability.available(binding.serviceIdentity()));
            RpcServerServiceDefinitionFactory definitions =
                    new RpcServerServiceDefinitionFactory(
                            availability,
                            List.of(context.getBean(
                                    RpcAccessGuardExceptionMapper.class)));
            Server server = null;
            ManagedChannel channel = null;
            try {
                var service = definitions.create(registry).getFirst();
                server = NettyServerBuilder.forPort(0)
                        .addService(service)
                        .build()
                        .start();
                channel = ManagedChannelBuilder.forAddress(
                                "127.0.0.1", server.getPort())
                        .usePlaintext()
                        .build();
                var method = new RpcContractValidator()
                        .validate(EchoContract.class)
                        .methods()
                        .getFirst()
                        .grpcMethod();

                @SuppressWarnings("unchecked")
                Message first = ClientCalls.blockingUnaryCall(
                        channel,
                        (io.grpc.MethodDescriptor<Message, Message>)
                                (io.grpc.MethodDescriptor<?, ?>) method,
                        CallOptions.DEFAULT,
                        StringValue.of("first"));
                assertThat(first).isEqualTo(StringValue.of("provider:first"));
                assertThat(provider.calls()).isOne();

                ManagedChannel activeChannel = channel;
                assertThatThrownBy(() -> ClientCalls.blockingUnaryCall(
                                activeChannel,
                                (io.grpc.MethodDescriptor<Message, Message>)
                                        (io.grpc.MethodDescriptor<?, ?>) method,
                                CallOptions.DEFAULT,
                                StringValue.of("second")))
                        .isInstanceOfSatisfying(
                                StatusRuntimeException.class,
                                failure -> assertRateLimitFailure(failure));
                assertThat(provider.calls()).isOne();
            } finally {
                if (channel != null) {
                    channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
                }
                if (server != null) {
                    server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
                }
            }
        });
    }

    private void assertRateLimitFailure(StatusRuntimeException failure) {
        assertThat(failure.getStatus().getCode())
                .isEqualTo(Status.Code.UNAVAILABLE);
        assertThat(failure.getTrailers()).isNotNull();
        assertThat(RpcFailureStage.from(failure.getTrailers()))
                .contains(RpcFailureStage.PROVIDER);
        assertThat(failure.getTrailers().get(RpcMetadataKeys.ERROR_TYPE))
                .isEqualTo("rate-limit");
    }

    @Configuration(proxyBeanMethods = false)
    static class ProviderConfiguration {

        @Bean
        GuardedProvider guardedProvider() {
            return new GuardedProvider();
        }
    }

    @EgonRpcService(
            grpcClass = UnaryFixtureGrpc.class,
            group = "test",
            version = "1.0.0")
    interface EchoContract {

        @EgonRpcMethod(name = "Echo")
        StringValue echo(StringValue request);
    }

    @EgonRpcProvider
    static class GuardedProvider implements EchoContract {

        private final AtomicInteger calls = new AtomicInteger();

        @Override
        @RateLimitGuard("rpc-create")
        public StringValue echo(StringValue request) {
            calls.incrementAndGet();
            return StringValue.of("provider:" + request.getValue());
        }

        int calls() {
            return calls.get();
        }
    }
}
