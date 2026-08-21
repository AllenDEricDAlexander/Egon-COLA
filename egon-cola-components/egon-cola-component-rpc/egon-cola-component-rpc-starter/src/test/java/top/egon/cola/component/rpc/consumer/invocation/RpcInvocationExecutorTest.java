package top.egon.cola.component.rpc.consumer.invocation;

import com.google.protobuf.StringValue;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.annotation.FailStrategy;
import top.egon.cola.component.rpc.annotation.LoadBalance;
import top.egon.cola.component.rpc.consumer.channel.RpcChannelLease;
import top.egon.cola.component.rpc.consumer.channel.RpcConsumerChannelPool;
import top.egon.cola.component.rpc.consumer.channel.RpcEndpoint;
import top.egon.cola.component.rpc.consumer.loadbalance.RpcLoadBalancer;
import top.egon.cola.component.rpc.consumer.loadbalance.RpcLoadBalancers;
import top.egon.cola.component.rpc.consumer.reference.RpcReferenceMode;
import top.egon.cola.component.rpc.consumer.reference.RpcReferencePolicy;
import top.egon.cola.component.rpc.consumer.reference.RpcReferenceStrategy;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;
import top.egon.cola.component.rpc.exception.RpcStatusExceptionMapper;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RpcInvocationExecutorTest {

    @Test
    void retriesConfiguredUnavailableEvenWhenContractIsNotIdempotent() {
        RpcConsumerChannelPool pool = mock(RpcConsumerChannelPool.class);
        RpcChannelLease firstLease = mock(RpcChannelLease.class);
        RpcChannelLease secondLease = mock(RpcChannelLease.class);
        when(pool.acquire(any(RpcEndpoint.class))).thenReturn(firstLease, secondLease);
        RpcReferenceStrategy strategy = strategy(RpcReferenceMode.DIRECT,
                endpoint("node-a"), endpoint("node-b"));
        RpcLoadBalancer loadBalancer = new RpcLoadBalancers().loadBalancer(LoadBalance.ROUND_ROBIN);
        AtomicInteger attempts = new AtomicInteger();
        RpcInvocationPlan plan = plan(
                RpcReferenceMode.DIRECT, strategy, loadBalancer, pool,
                new RpcReferencePolicy(1000, 1, LoadBalance.ROUND_ROBIN,
                        FailStrategy.FAIL_CLOSED, "", null),
                (request, lease, timeout, invocationId) -> {
                    if (attempts.getAndIncrement() == 0) {
                        return failed(Status.UNAVAILABLE);
                    }
                    return successful(StringValue.of("ok"));
                });

        Object value = new RpcInvocationExecutor(new RpcStatusExceptionMapper())
                .executeBlocking(plan, StringValue.of("request"));

        assertThat(value).isEqualTo(StringValue.of("ok"));
        assertThat(attempts).hasValue(2);
        verify(firstLease).close();
        verify(secondLease).close();
    }

    @Test
    void businessErrorIsTerminalAndNeverRunsFailOpen() {
        RpcConsumerChannelPool pool = mock(RpcConsumerChannelPool.class);
        RpcChannelLease lease = mock(RpcChannelLease.class);
        when(pool.acquire(any(RpcEndpoint.class))).thenReturn(lease);
        RpcInvocationPlan plan = plan(
                RpcReferenceMode.DIRECT,
                strategy(RpcReferenceMode.DIRECT, endpoint("node-a")),
                RpcLoadBalancers.create(LoadBalance.ROUND_ROBIN), pool,
                new RpcReferencePolicy(1000, 3, LoadBalance.ROUND_ROBIN,
                        FailStrategy.FAIL_OPEN, "", null),
                (request, ignored, timeout, invocationId) -> failed(Status.ALREADY_EXISTS));

        assertThatThrownBy(() -> new RpcInvocationExecutor(new RpcStatusExceptionMapper())
                .executeBlocking(plan, StringValue.of("request")))
                .isInstanceOf(EgonRpcException.class)
                .satisfies(error -> assertThat(((EgonRpcException) error).getCode())
                        .isEqualTo(EgonRpcErrorCode.RPC_INTERNAL));
        verify(lease).close();
    }

    @Test
    void failOpenReturnsNullOnlyAfterAvailabilityExhaustion() {
        RpcConsumerChannelPool pool = mock(RpcConsumerChannelPool.class);
        RpcChannelLease lease = mock(RpcChannelLease.class);
        when(pool.acquire(any(RpcEndpoint.class))).thenReturn(lease);
        RpcInvocationPlan plan = plan(
                RpcReferenceMode.GATEWAY,
                strategy(RpcReferenceMode.GATEWAY, endpoint("node-a")),
                RpcLoadBalancers.create(LoadBalance.ROUND_ROBIN), pool,
                new RpcReferencePolicy(1000, 0, LoadBalance.ROUND_ROBIN,
                        FailStrategy.FAIL_OPEN, "", null),
                (request, ignored, timeout, invocationId) -> failed(Status.UNAVAILABLE));

        assertThat(new RpcInvocationExecutor(new RpcStatusExceptionMapper())
                .executeBlocking(plan, StringValue.of("request"))).isNull();
        verify(lease).close();
    }

    @Test
    void asyncCancellationCancelsCurrentAttemptAndReleasesLeaseOnce() {
        RpcConsumerChannelPool pool = mock(RpcConsumerChannelPool.class);
        RpcChannelLease lease = mock(RpcChannelLease.class);
        when(pool.acquire(any(RpcEndpoint.class))).thenReturn(lease);
        CompletableFuture<Object> pending = new CompletableFuture<>();
        AtomicInteger cancels = new AtomicInteger();
        RpcInvocationPlan plan = plan(
                RpcReferenceMode.DIRECT,
                strategy(RpcReferenceMode.DIRECT, endpoint("node-a")),
                RpcLoadBalancers.create(LoadBalance.ROUND_ROBIN), pool,
                new RpcReferencePolicy(1000, 1, LoadBalance.ROUND_ROBIN,
                        FailStrategy.FAIL_CLOSED, "", null),
                (request, ignored, timeout, invocationId) ->
                        new RpcInvocationPlan.Attempt(pending, cancels::incrementAndGet));

        CompletionStage<Object> stage = new RpcInvocationExecutor(new RpcStatusExceptionMapper())
                .executeAsync(plan, StringValue.of("request"));
        assertThat(stage.toCompletableFuture().cancel(true)).isTrue();
        assertThat(cancels).hasValue(1);
        verify(lease).close();
    }

    @Test
    void directAllRateLimitedFailuresExposeRateLimitedCode() {
        RpcConsumerChannelPool pool = mock(RpcConsumerChannelPool.class);
        RpcChannelLease lease = mock(RpcChannelLease.class);
        when(pool.acquire(any(RpcEndpoint.class))).thenReturn(lease);
        Metadata trailers = new Metadata();
        trailers.put(Metadata.Key.of("x-egon-rpc-failure-stage",
                Metadata.ASCII_STRING_MARSHALLER), "provider");
        trailers.put(Metadata.Key.of("x-egon-rpc-error-type",
                Metadata.ASCII_STRING_MARSHALLER), "rate-limit");
        RpcInvocationPlan plan = plan(
                RpcReferenceMode.DIRECT,
                strategy(RpcReferenceMode.DIRECT, endpoint("node-a")),
                RpcLoadBalancers.create(LoadBalance.ROUND_ROBIN), pool,
                new RpcReferencePolicy(1000, 0, LoadBalance.ROUND_ROBIN,
                        FailStrategy.FAIL_CLOSED, "", null),
                (request, ignored, timeout, invocationId) ->
                        new RpcInvocationPlan.Attempt(
                                CompletableFuture.failedFuture(
                                        new StatusRuntimeException(Status.UNAVAILABLE, trailers)),
                                () -> { }));

        assertThatThrownBy(() -> new RpcInvocationExecutor(new RpcStatusExceptionMapper())
                .executeBlocking(plan, StringValue.of("request")))
                .isInstanceOfSatisfying(EgonRpcException.class, error ->
                        assertThat(error.getCode()).isEqualTo(EgonRpcErrorCode.RPC_RATE_LIMITED));
    }

    private static RpcInvocationPlan plan(
            RpcReferenceMode mode,
            RpcReferenceStrategy strategy,
            RpcLoadBalancer loadBalancer,
            RpcConsumerChannelPool pool,
            RpcReferencePolicy policy,
            RpcInvocationPlan.UnaryInvoker invoker) {
        return new RpcInvocationPlan(
                "sample.Service", "sample.Service/Echo", mode, policy,
                strategy, loadBalancer, pool, StringValue.class, invoker);
    }

    private static RpcReferenceStrategy strategy(
            RpcReferenceMode mode,
            RpcEndpoint... endpoints) {
        List<RpcEndpoint> candidates = List.of(endpoints);
        return new RpcReferenceStrategy() {
            @Override
            public RpcReferenceMode mode() {
                return mode;
            }

            @Override
            public String queryIdentity() {
                return "sample.Service/Echo";
            }

            @Override
            public long revision() {
                return 1;
            }

            @Override
            public List<? extends RpcEndpoint> candidates() {
                return candidates;
            }

            @Override
            public void close() {
            }
        };
    }

    private static RpcEndpoint endpoint(String host) {
        return new RpcEndpoint() {
            @Override
            public String host() {
                return host;
            }

            @Override
            public int port() {
                return 19090;
            }

            @Override
            public boolean secure() {
                return false;
            }
        };
    }

    private static RpcInvocationPlan.Attempt successful(Object value) {
        return new RpcInvocationPlan.Attempt(
                CompletableFuture.completedFuture(value), () -> { });
    }

    private static RpcInvocationPlan.Attempt failed(Status status) {
        return new RpcInvocationPlan.Attempt(
                CompletableFuture.failedFuture(new StatusRuntimeException(status)), () -> { });
    }
}
