package top.egon.cola.component.rpc.test.fixture.provider;

import org.springframework.context.annotation.Profile;
import top.egon.cola.component.rpc.annotation.EgonRpcProvider;
import top.egon.cola.component.rpc.context.invocation.RpcInvocationMetadata;
import top.egon.cola.component.rpc.test.contract.AsyncEchoRpc;
import top.egon.cola.component.rpc.test.contract.proto.EchoRequest;
import top.egon.cola.component.rpc.test.contract.proto.EchoResponse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

/** Isolated controllable async Provider fixture; not part of the blocking app. */
@EgonRpcProvider
@Profile("rpc-async-fixture")
public class AsyncEchoRpcTestProvider implements AsyncEchoRpc {

    private final AtomicInteger calls = new AtomicInteger();

    private volatile CompletableFuture<EchoResponse> active;

    @Override
    public CompletionStage<EchoResponse> echoAsync(EchoRequest request) {
        calls.incrementAndGet();
        RpcInvocationMetadata invocation = RpcInvocationMetadata.current();
        String invocationId = invocation == null || invocation.invocationId() == null
                ? ""
                : invocation.invocationId();
        CompletableFuture<EchoResponse> future = new CompletableFuture<>();
        active = future;
        return future.thenApply(ignored -> EchoResponse.newBuilder()
                .setProviderId("async-provider")
                .setMessage(request.getMessage())
                .setInvocationId(invocationId)
                .setTraceId(invocation == null || invocation.traceId() == null
                        ? ""
                        : invocation.traceId())
                .build());
    }

    public void complete() {
        CompletableFuture<EchoResponse> future = active;
        if (future == null) {
            throw new IllegalStateException("no async RPC is pending");
        }
        future.complete(EchoResponse.getDefaultInstance());
    }

    public boolean cancelPending() {
        CompletableFuture<EchoResponse> future = active;
        return future != null && future.cancel(true);
    }

    public int calls() {
        return calls.get();
    }

    public boolean pendingCancelled() {
        CompletableFuture<EchoResponse> future = active;
        return future != null && future.isCancelled();
    }
}
