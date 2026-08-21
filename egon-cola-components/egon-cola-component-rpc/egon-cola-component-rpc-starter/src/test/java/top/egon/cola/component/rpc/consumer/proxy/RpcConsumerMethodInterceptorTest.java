package top.egon.cola.component.rpc.consumer.proxy;

import com.google.protobuf.StringValue;
import org.junit.jupiter.api.Test;
import org.springframework.util.ClassUtils;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;
import top.egon.cola.component.rpc.annotation.FailStrategy;
import top.egon.cola.component.rpc.annotation.LoadBalance;
import top.egon.cola.component.rpc.consumer.channel.RpcConsumerChannelPool;
import top.egon.cola.component.rpc.consumer.channel.RpcEndpoint;
import top.egon.cola.component.rpc.consumer.invocation.RpcInvocationExecutor;
import top.egon.cola.component.rpc.consumer.invocation.RpcInvocationPlan;
import top.egon.cola.component.rpc.consumer.loadbalance.RpcLoadBalancers;
import top.egon.cola.component.rpc.consumer.reference.RpcReferenceDefinition;
import top.egon.cola.component.rpc.consumer.reference.RpcReferenceMode;
import top.egon.cola.component.rpc.consumer.reference.RpcReferencePolicy;
import top.egon.cola.component.rpc.consumer.reference.RpcReferenceStrategy;
import top.egon.cola.component.rpc.context.identity.RpcProcessIdentity;
import top.egon.cola.component.rpc.contract.descriptor.RpcContractDescriptor;
import top.egon.cola.component.rpc.contract.validation.RpcContractValidator;
import top.egon.cola.component.rpc.exception.EgonRpcException;
import top.egon.cola.component.rpc.exception.RpcStatusExceptionMapper;
import top.egon.cola.component.rpc.support.TestGrpcDescriptorFixtures.UnaryFixtureGrpc;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class RpcConsumerMethodInterceptorTest {

    @Test
    void createsCglibProxyAndDispatchesCompiledBlockingPlan() throws Exception {
        RpcContractValidator validator = new RpcContractValidator();
        RpcContractDescriptor contract = validator.validate(EchoContract.class);
        RpcInvocationExecutor executor = mock(RpcInvocationExecutor.class);
        when(executor.executeBlocking(
                any(RpcInvocationPlan.class),
                any()
        )).thenReturn(StringValue.of("compiled"));
        RpcConsumerProxyFactory factory = factory(executor);

        EchoContract proxy = factory.create(
                contract,
                definition(contract),
                strategy()
        );

        assertThat(ClassUtils.isCglibProxyClass(proxy.getClass())).isTrue();
        assertThat(proxy.echo(StringValue.of("request")))
                .isEqualTo(StringValue.of("compiled"));
        assertThat(proxy.toString()).contains(EchoContract.class.getName());
        assertThat(proxy.hashCode()).isEqualTo(System.identityHashCode(proxy));
        assertThat(proxy.equals(proxy)).isTrue();
        assertThat(proxy.equals(new Object())).isFalse();
        verify(executor).executeBlocking(any(RpcInvocationPlan.class), any());
        verifyNoMoreInteractions(executor);
    }

    @Test
    void dispatchesAsyncPlanWithoutBlockingTheProxyCallback() {
        RpcContractValidator validator = new RpcContractValidator();
        RpcContractDescriptor contract = validator.validate(AsyncContract.class);
        RpcInvocationExecutor executor = mock(RpcInvocationExecutor.class);
        CompletableFuture<Object> response = CompletableFuture.completedFuture(
                StringValue.of("async")
        );
        when(executor.executeAsync(
                any(RpcInvocationPlan.class),
                any()
        )).thenReturn(response);
        AsyncContract proxy = factory(executor).create(
                contract,
                definition(contract),
                strategy()
        );

        CompletionStage<StringValue> result = proxy.echo(
                StringValue.of("request")
        );

        assertThat(result.toCompletableFuture().join())
                .isEqualTo(StringValue.of("async"));
        verify(executor).executeAsync(any(RpcInvocationPlan.class), any());
    }

    @Test
    void rejectsNullRequestBeforeExecutorAndKeepsObjectMethodsLocal() {
        RpcContractValidator validator = new RpcContractValidator();
        RpcContractDescriptor contract = validator.validate(EchoContract.class);
        RpcInvocationExecutor executor = mock(RpcInvocationExecutor.class);
        EchoContract proxy = factory(executor).create(
                contract,
                definition(contract),
                strategy()
        );

        assertThatThrownBy(() -> proxy.echo(null))
                .isInstanceOf(EgonRpcException.class)
                .hasMessageContaining("one Protobuf Message");
        assertThat(proxy.equals(proxy)).isTrue();
        verifyNoMoreInteractions(executor);
    }

    private RpcConsumerProxyFactory factory(RpcInvocationExecutor executor) {
        return new RpcConsumerProxyFactory(
                new RpcContractValidator(),
                mock(RpcConsumerChannelPool.class),
                executor,
                new RpcProcessIdentity(
                        "proxy-test",
                        "test",
                        "127.0.0.1",
                        1,
                        "proxy-1"
                ),
                new RpcLoadBalancers(),
                new RpcStatusExceptionMapper(),
                1000,
                List.of()
        );
    }

    private RpcReferenceDefinition definition(RpcContractDescriptor contract) {
        Map<Method, RpcReferencePolicy> policies = contract.methods().stream()
                .collect(java.util.stream.Collectors.toMap(
                        method -> method.javaMethod(),
                        ignored -> new RpcReferencePolicy(
                                1000,
                                0,
                                LoadBalance.ROUND_ROBIN,
                                FailStrategy.FAIL_CLOSED,
                                "",
                                null
                        )
                ));
        return new RpcReferenceDefinition(
                RpcReferenceMode.GATEWAY,
                new top.egon.cola.component.rpc.contract.identity.RpcServiceIdentity(
                        contract.serviceName(),
                        contract.group(),
                        contract.version()
                ),
                null,
                policies
        );
    }

    private RpcReferenceStrategy strategy() {
        return new RpcReferenceStrategy() {
            @Override
            public RpcReferenceMode mode() {
                return RpcReferenceMode.GATEWAY;
            }

            @Override
            public String queryIdentity() {
                return "proxy-test";
            }

            @Override
            public long revision() {
                return 1;
            }

            @Override
            public List<? extends RpcEndpoint> candidates() {
                return List.of();
            }

            @Override
            public void close() {
            }
        };
    }

    @EgonRpcService(
            grpcClass = UnaryFixtureGrpc.class,
            group = "test",
            version = "1.0.0"
    )
    public interface EchoContract {

        @EgonRpcMethod(name = "Echo")
        StringValue echo(StringValue request);
    }

    @EgonRpcService(
            grpcClass = UnaryFixtureGrpc.class,
            group = "test",
            version = "1.0.0"
    )
    public interface AsyncContract {

        @EgonRpcMethod(name = "Echo")
        CompletionStage<StringValue> echo(StringValue request);
    }
}
