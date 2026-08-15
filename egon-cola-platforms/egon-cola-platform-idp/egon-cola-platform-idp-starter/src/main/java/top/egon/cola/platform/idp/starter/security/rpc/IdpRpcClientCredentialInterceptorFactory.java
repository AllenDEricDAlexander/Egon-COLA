package top.egon.cola.platform.idp.starter.security.rpc;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import top.egon.cola.component.rpc.consumer.interceptor.RpcClientInterceptorFactory;
import top.egon.cola.component.rpc.consumer.interceptor.RpcClientInvocation;
import top.egon.cola.component.rpc.context.invocation.RpcMetadataKeys;
import top.egon.cola.platform.idp.starter.security.VerifiedUserTokenCarrier;

import java.util.Objects;

/**
 * Relays only an already verified USER token from the trusted HTTP or gRPC request context.
 * Existing authorization metadata is rejected so independent credentials can never be merged.
 */
public final class IdpRpcClientCredentialInterceptorFactory
        implements RpcClientInterceptorFactory {

    private static final int MAX_CREDENTIAL_LENGTH = 8192;

    /**
     * Captures the trusted request-scoped USER token for the current RPC invocation.
     *
     * @param invocation current RPC invocation
     * @return client interceptor adding at most one Bearer credential
     */
    @Override
    public ClientInterceptor create(RpcClientInvocation invocation) {
        Objects.requireNonNull(invocation, "invocation");
        String token = VerifiedUserTokenCarrier.currentOrNull();
        String authorization = token == null ? null : bearer(token);
        return new ClientInterceptor() {
            @Override
            public <RequestT, ResponseT> ClientCall<RequestT, ResponseT>
                    interceptCall(
                            MethodDescriptor<RequestT, ResponseT> method,
                            CallOptions callOptions,
                            Channel next
                    ) {
                ClientCall<RequestT, ResponseT> delegate =
                        next.newCall(method, callOptions);
                return new ForwardingClientCall.SimpleForwardingClientCall<>(
                        delegate
                ) {
                    @Override
                    public void start(
                            Listener<ResponseT> responseListener,
                            Metadata headers
                    ) {
                        if (authorization != null) {
                            Iterable<String> existing = headers.getAll(
                                    RpcMetadataKeys.AUTHORIZATION
                            );
                            if (existing != null && existing.iterator().hasNext()) {
                                throw new IllegalStateException(
                                        "authorization metadata already exists"
                                );
                            }
                            headers.put(
                                    RpcMetadataKeys.AUTHORIZATION,
                                    authorization
                            );
                        }
                        super.start(responseListener, headers);
                    }
                };
            }
        };
    }

    private String bearer(String token) {
        if (token.isBlank() || token.length() > MAX_CREDENTIAL_LENGTH) {
            throw new IllegalArgumentException("verified USER token is invalid");
        }
        for (int index = 0; index < token.length(); index++) {
            char value = token.charAt(index);
            if (value < 0x21 || value > 0x7e) {
                throw new IllegalArgumentException(
                        "verified USER token is invalid"
                );
            }
        }
        return "Bearer " + token;
    }
}
