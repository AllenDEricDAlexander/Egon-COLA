package top.egon.cola.component.rpc.ddc.security;

import io.grpc.ClientInterceptor;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import top.egon.cola.component.rpc.context.RpcClientInterceptorFactory;
import top.egon.cola.component.rpc.context.RpcClientInvocation;

import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 基于实际 protobuf 请求创建 DDC HMAC Metadata 的客户端拦截器工厂。
 * / Client interceptor factory that creates DDC HMAC Metadata from the actual
 * protobuf request.
 */
public final class DdcRpcClientInterceptorFactory
        implements RpcClientInterceptorFactory {

    private final DdcRpcCredential credential;
    private final Clock clock;
    private final Supplier<String> nonceSupplier;
    private final DdcRpcRequestSigner signer;
    private final DdcRpcOperationResolver operationResolver;

    public DdcRpcClientInterceptorFactory(DdcRpcCredential credential) {
        this(credential, Clock.systemUTC(), () -> UUID.randomUUID().toString());
    }

    public DdcRpcClientInterceptorFactory(
            DdcRpcCredential credential,
            Clock clock,
            Supplier<String> nonceSupplier) {
        if (credential == null || clock == null || nonceSupplier == null) {
            throw new IllegalArgumentException(
                    "credential, clock and nonceSupplier are required");
        }
        this.credential = credential;
        this.clock = clock;
        this.nonceSupplier = nonceSupplier;
        this.signer = new DdcRpcRequestSigner();
        this.operationResolver = new DdcRpcOperationResolver();
    }

    @Override
    public ClientInterceptor create(RpcClientInvocation invocation) {
        return MetadataUtils.newAttachHeadersInterceptor(headers(invocation));
    }

    Metadata headers(RpcClientInvocation invocation) {
        if (invocation == null) {
            throw new IllegalArgumentException("invocation is required");
        }
        String fullMethodName = invocation.method().fullMethodName();
        operationResolver.resolve(fullMethodName);
        long timestamp = clock.millis();
        String nonce = nonceSupplier.get();
        DdcRpcCanonicalRequest canonical = new DdcRpcCanonicalRequest(
                fullMethodName,
                timestamp,
                nonce,
                invocation.request()
        );
        Metadata metadata = new Metadata();
        metadata.put(DdcRpcMetadataKeys.ACCESS_KEY, credential.accessKey());
        metadata.put(DdcRpcMetadataKeys.TIMESTAMP, Long.toString(timestamp));
        metadata.put(DdcRpcMetadataKeys.NONCE, nonce);
        metadata.put(
                DdcRpcMetadataKeys.CONTENT_SHA256,
                canonical.contentSha256()
        );
        metadata.put(
                DdcRpcMetadataKeys.SIGNATURE,
                signer.sign(canonical, credential.secretKey())
        );
        metadata.put(
                DdcRpcMetadataKeys.CONTRACT_VERSION,
                DdcRpcCanonicalRequest.CONTRACT_VERSION
        );
        return metadata;
    }

    DdcRpcOperationResolver operationResolver() {
        return operationResolver;
    }
}
