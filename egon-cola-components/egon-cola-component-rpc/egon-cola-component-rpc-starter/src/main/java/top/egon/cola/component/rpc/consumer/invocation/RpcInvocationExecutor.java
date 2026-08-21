package top.egon.cola.component.rpc.consumer.invocation;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import top.egon.cola.component.rpc.annotation.FailStrategy;
import top.egon.cola.component.rpc.context.invocation.RpcMetadataKeys;
import top.egon.cola.component.rpc.consumer.channel.RpcChannelLease;
import top.egon.cola.component.rpc.consumer.channel.RpcEndpoint;
import top.egon.cola.component.rpc.consumer.loadbalance.RpcLoadBalanceContext;
import top.egon.cola.component.rpc.consumer.loadbalance.RpcLoadBalancers;
import top.egon.cola.component.rpc.consumer.lifecycle.RpcConsumerLifecycleCoordinator;
import top.egon.cola.component.rpc.consumer.reference.RpcReferenceMode;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;
import top.egon.cola.component.rpc.exception.RpcStatusExceptionMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Single blocking/async state machine for bounded same-mode unary attempts. */
public final class RpcInvocationExecutor {

    private final RpcConsumerLifecycleCoordinator lifecycle;
    private final RpcStatusExceptionMapper statusMapper;

    public RpcInvocationExecutor(RpcStatusExceptionMapper statusMapper) {
        this(null, statusMapper);
    }

    public RpcInvocationExecutor(
            RpcConsumerLifecycleCoordinator lifecycle,
            RpcStatusExceptionMapper statusMapper) {
        this.lifecycle = lifecycle;
        this.statusMapper = statusMapper == null
                ? new RpcStatusExceptionMapper() : statusMapper;
    }

    public Object executeBlocking(RpcInvocationPlan plan, Object request) {
        CompletableFuture<Object> future = executeAsync(plan, request).toCompletableFuture();
        try {
            return future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            throw new EgonRpcException(
                    EgonRpcErrorCode.RPC_CANCELLED,
                    "RPC call was cancelled", exception);
        } catch (CancellationException exception) {
            throw new EgonRpcException(
                    EgonRpcErrorCode.RPC_CANCELLED,
                    "RPC call was cancelled", exception);
        } catch (ExecutionException exception) {
            Throwable cause = unwrap(exception.getCause());
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new EgonRpcException(
                    EgonRpcErrorCode.RPC_INTERNAL,
                    "RPC invocation failed", cause);
        }
    }

    public CompletionStage<Object> executeAsync(
            RpcInvocationPlan plan,
            Object request) {
        Invocation execution = new Invocation(plan, request);
        execution.start();
        return execution.result;
    }

    private final class Invocation {

        private final RpcInvocationPlan plan;
        private final Object request;
        private final RpcInvocationContext context;
        private final ExecutionFuture result = new ExecutionFuture();
        private final AtomicReference<AttemptState> active = new AtomicReference<>();
        private volatile byte[] affinityDigest;

        private Invocation(RpcInvocationPlan plan, Object request) {
            this.plan = java.util.Objects.requireNonNull(plan, "plan");
            this.request = request;
            this.context = new RpcInvocationContext(
                    System.nanoTime(),
                    this.plan.policy().timeoutMs(),
                    this.plan.policy().retries());
        }

        private void start() {
            try {
                if (lifecycle != null) {
                    lifecycle.requireAccepting();
                }
                affinityDigest = resolveAffinity();
                startNext();
            } catch (RuntimeException exception) {
                completeException(exception);
            }
        }

        private void startNext() {
            if (result.isDone() || context.terminal()) {
                return;
            }
            long remaining = context.remainingNanos(System.nanoTime());
            if (remaining == 0) {
                completeException(deadlineExceeded());
                return;
            }
            List<? extends RpcEndpoint> candidates;
            try {
                candidates = plan.strategy().candidates();
            } catch (RuntimeException exception) {
                context.recordAvailability(false);
                finalizeAvailability();
                return;
            }
            if (candidates == null || candidates.isEmpty() || !context.hasAttemptBudget()) {
                finalizeAvailability();
                return;
            }
            RpcLoadBalanceContext balanceContext = new RpcLoadBalanceContext(
                    plan.strategy().queryIdentity(),
                    plan.serviceName(),
                    plan.fullMethodName(),
                    request,
                    candidates,
                    context.excluded(),
                    affinityDigest,
                    Math.max(0, plan.strategy().revision()));
            RpcEndpoint endpoint;
            String endpointKey;
            try {
                endpoint = plan.loadBalancer().select(balanceContext);
                endpointKey = RpcLoadBalancers.endpointKey(endpoint);
            } catch (RuntimeException exception) {
                finalizeAvailability();
                return;
            }
            if (!context.beginAttempt(endpointKey)) {
                finalizeAvailability();
                return;
            }
            RpcChannelLease lease = null;
            try {
                lease = plan.channelPool().acquire(endpoint);
                lease.beginCall();
            } catch (RuntimeException exception) {
                if (lease != null) {
                    lease.close();
                }
                context.recordAvailability(false);
                if (context.hasAttemptBudget() && !context.expired(System.nanoTime())) {
                    startNext();
                } else {
                    finalizeAvailability();
                }
                return;
            }
            RpcInvocationPlan.Attempt attempt;
            try {
                attempt = plan.invoker().invoke(
                        request,
                        lease,
                        Duration.ofNanos(context.remainingNanos(System.nanoTime())),
                        context.invocationId());
            } catch (Exception exception) {
                lease.endCall();
                lease.close();
                handleFailure(exception);
                return;
            }
            if (attempt == null) {
                lease.endCall();
                lease.close();
                handleFailure(new EgonRpcException(
                        EgonRpcErrorCode.RPC_INTERNAL,
                        "RPC invoker returned no attempt"));
                return;
            }
            AttemptState state = new AttemptState(lease, attempt);
            active.set(state);
            long timeoutNanos = Math.max(1, context.remainingNanos(System.nanoTime()));
            attempt.completion().toCompletableFuture()
                    .orTimeout(timeoutNanos, TimeUnit.NANOSECONDS)
                    .whenComplete((value, error) -> {
                        if (error != null && unwrap(error) instanceof TimeoutException) {
                            state.cancelTransport();
                        }
                        if (!state.release()) {
                            return;
                        }
                        active.compareAndSet(state, null);
                        if (error == null) {
                            completeSuccess(value);
                        } else {
                            handleFailure(error);
                        }
                    });
        }

        private void handleFailure(Throwable failure) {
            Throwable cause = unwrap(failure);
            if (cause instanceof TimeoutException) {
                completeException(deadlineExceeded());
                return;
            }
            if (!isAvailability(cause)) {
                completeException(map(cause));
                return;
            }
            boolean rateLimited = isRateLimited(cause);
            context.recordAvailability(rateLimited);
            if (context.hasAttemptBudget() && !context.expired(System.nanoTime())) {
                startNext();
            } else {
                finalizeAvailability();
            }
        }

        private void completeSuccess(Object value) {
            if (value == null) {
                completeException(new EgonRpcException(
                        EgonRpcErrorCode.RPC_INTERNAL,
                        "RPC Provider returned a null response"));
                return;
            }
            if (!plan.responseType().isInstance(value)) {
                completeException(new EgonRpcException(
                        EgonRpcErrorCode.RPC_INTERNAL,
                        "RPC Provider returned an invalid response type"));
                return;
            }
            if (context.terminate()) {
                result.complete(value);
            }
        }

        private void finalizeAvailability() {
            if (!context.terminate()) {
                return;
            }
            EgonRpcErrorCode code = unavailableCode();
            try {
                if (plan.policy().failStrategy() == FailStrategy.FAIL_OPEN) {
                    result.complete(null);
                } else if (plan.policy().failStrategy() == FailStrategy.LOCAL_FALLBACK
                        && plan.fallback() != null) {
                    result.complete(plan.fallback().apply(request));
                } else {
                    result.completeExceptionally(new EgonRpcException(
                            code, code == EgonRpcErrorCode.RPC_RATE_LIMITED
                                    ? "RPC request was rate limited"
                                    : "RPC endpoint is unavailable"));
                }
            } catch (RuntimeException exception) {
                result.completeExceptionally(exception);
            }
        }

        private void completeException(RuntimeException exception) {
            if (context.terminate()) {
                result.completeExceptionally(exception);
            }
        }

        private byte[] resolveAffinity() {
            if (plan.policy().keyResolver() == null) {
                return null;
            }
            RpcLoadBalanceContext context = new RpcLoadBalanceContext(
                    plan.strategy().queryIdentity(), plan.serviceName(),
                    plan.fullMethodName(), request, plan.strategy().candidates(),
                    Set.of(), null, Math.max(0, plan.strategy().revision()));
            String key = plan.policy().keyResolver().resolve(context);
            if (key == null || key.isBlank()
                    || key.getBytes(StandardCharsets.UTF_8).length > 512) {
                throw new EgonRpcException(
                        EgonRpcErrorCode.RPC_INVALID_REQUEST,
                        "RPC consistent-hash key is invalid");
            }
            try {
                return MessageDigest.getInstance("SHA-256")
                        .digest(key.getBytes(StandardCharsets.UTF_8));
            } catch (NoSuchAlgorithmException exception) {
                throw new IllegalStateException("SHA-256 is required", exception);
            }
        }

        private EgonRpcErrorCode unavailableCode() {
            if (context.allRateLimited()
                    && plan.referenceMode() == RpcReferenceMode.DIRECT) {
                return EgonRpcErrorCode.RPC_RATE_LIMITED;
            }
            return plan.referenceMode() == RpcReferenceMode.GATEWAY
                    ? EgonRpcErrorCode.RPC_GATEWAY_UNAVAILABLE
                    : EgonRpcErrorCode.RPC_PROVIDER_UNAVAILABLE;
        }

        private EgonRpcException map(Throwable cause) {
            if (cause instanceof StatusRuntimeException status) {
                return statusMapper.map(status);
            }
            if (cause instanceof EgonRpcException rpc) {
                return rpc;
            }
            return new EgonRpcException(
                    EgonRpcErrorCode.RPC_INTERNAL,
                    "RPC invocation failed", cause);
        }

        private boolean isAvailability(Throwable cause) {
            if (cause instanceof StatusRuntimeException status) {
                return status.getStatus().getCode() == Status.Code.UNAVAILABLE;
            }
            if (cause instanceof EgonRpcException exception) {
                return exception.getCode() == EgonRpcErrorCode.RPC_PROVIDER_UNAVAILABLE
                        || exception.getCode() == EgonRpcErrorCode.RPC_GATEWAY_UNAVAILABLE
                        || exception.getCode() == EgonRpcErrorCode.RPC_RATE_LIMITED;
            }
            return false;
        }

        private boolean isRateLimited(Throwable cause) {
            if (!(cause instanceof StatusRuntimeException status)) {
                return cause instanceof EgonRpcException exception
                        && exception.getCode() == EgonRpcErrorCode.RPC_RATE_LIMITED;
            }
            Metadata trailers = status.getTrailers();
            return status.getStatus().getCode() == Status.Code.UNAVAILABLE
                    && trailers != null
                    && "rate-limit".equalsIgnoreCase(trailers.get(RpcMetadataKeys.ERROR_TYPE));
        }

        private EgonRpcException deadlineExceeded() {
            return new EgonRpcException(
                    EgonRpcErrorCode.RPC_DEADLINE_EXCEEDED,
                    "RPC deadline exceeded");
        }

        private final class ExecutionFuture extends CompletableFuture<Object> {

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                if (!context.terminate()) {
                    return false;
                }
                AttemptState state = active.getAndSet(null);
                if (state != null) {
                    state.cancel();
                }
                return super.cancel(mayInterruptIfRunning);
            }
        }

        private final class AttemptState {

            private final RpcChannelLease lease;
            private final RpcInvocationPlan.Attempt attempt;
            private final AtomicBoolean released = new AtomicBoolean();

            private AttemptState(
                    RpcChannelLease lease,
                    RpcInvocationPlan.Attempt attempt) {
                this.lease = lease;
                this.attempt = attempt;
            }

            private boolean release() {
                if (!released.compareAndSet(false, true)) {
                    return false;
                }
                lease.endCall();
                lease.close();
                return true;
            }

            private void cancel() {
                try {
                    cancelTransport();
                } finally {
                    release();
                }
            }

            private void cancelTransport() {
                attempt.cancel().run();
            }
        }
    }

    private static Throwable unwrap(Throwable value) {
        Throwable cause = value;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException)
                && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }
}
