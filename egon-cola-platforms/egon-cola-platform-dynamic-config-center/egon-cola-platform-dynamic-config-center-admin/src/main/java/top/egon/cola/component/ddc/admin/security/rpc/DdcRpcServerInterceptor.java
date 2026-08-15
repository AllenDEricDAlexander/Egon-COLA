package top.egon.cola.component.ddc.admin.security.rpc;

import com.google.protobuf.Message;
import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import top.egon.cola.component.ddc.admin.config.DdcAdminProperties;
import top.egon.cola.component.ddc.error.DdcErrorStatus;
import top.egon.cola.component.rpc.ddc.security.DdcRpcCanonicalRequest;
import top.egon.cola.component.rpc.ddc.security.DdcRpcMetadataKeys;
import top.egon.cola.component.rpc.ddc.security.DdcRpcOperation;
import top.egon.cola.component.rpc.ddc.security.DdcRpcOperationResolver;
import top.egon.cola.component.rpc.ddc.security.DdcRpcRequestSigner;

import java.time.Clock;
import java.time.Duration;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 在 Provider 执行前认证单条 DDC unary RPC 请求。
 * / Authenticates one DDC unary RPC request before provider execution.
 */
public final class DdcRpcServerInterceptor implements ServerInterceptor {

    private static final Pattern HEX_64 = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern NONCE = Pattern.compile("[\\x21-\\x7e]{1,128}");

    private final DdcAdminProperties.Rpc properties;
    private final DdcHmacCredentialRegistry credentialRegistry;
    private final DdcNonceStore nonceStore;
    private final Clock clock;
    private final DdcRpcRequestSigner signer = new DdcRpcRequestSigner();
    private final DdcRpcOperationResolver operationResolver =
            new DdcRpcOperationResolver();
    private final DdcRpcScopeExtractor scopeExtractor =
            new DdcRpcScopeExtractor();

    /** 创建生产时钟的服务端认证拦截器。 / Creates the server authenticator with the production clock. */
    public DdcRpcServerInterceptor(
            DdcAdminProperties properties,
            DdcHmacCredentialRegistry credentialRegistry,
            DdcNonceStore nonceStore) {
        this(properties, credentialRegistry, nonceStore, Clock.systemUTC());
    }

    DdcRpcServerInterceptor(
            DdcAdminProperties properties,
            DdcHmacCredentialRegistry credentialRegistry,
            DdcNonceStore nonceStore,
            Clock clock) {
        if (properties == null || credentialRegistry == null || clock == null) {
            throw new IllegalArgumentException(
                    "properties, credentialRegistry and clock are required");
        }
        this.properties = properties.getRpc();
        this.credentialRegistry = credentialRegistry;
        this.nonceStore = nonceStore;
        this.clock = clock;
        validateConfiguration();
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        ServerCall.Listener<ReqT> delegate = next.startCall(call, headers);
        return new AuthenticatingListener<>(call, headers, delegate);
    }

    private Authentication authenticate(
            String fullMethodName,
            Metadata headers,
            Message request) {
        Headers signed = null;
        DdcHmacCredential credential = null;
        long timestamp = 0L;
        long now = clock.millis();
        long skew = properties.getAllowedClockSkewSeconds() * 1000L;
        if (properties.isSignatureEnabled()) {
            signed = headers(headers);
            timestamp = timestamp(signed.timestamp());
            if (timestamp < now - skew || timestamp > now + skew) {
                throw authentication(DdcErrorStatus.SIGNATURE_EXPIRED);
            }
            credential = credentialRegistry.resolve(
                    signed.accessKey()).orElseThrow(() ->
                    authentication(DdcErrorStatus.SIGNATURE_INVALID));
            DdcRpcCanonicalRequest canonical = new DdcRpcCanonicalRequest(
                    fullMethodName, timestamp, signed.nonce(), request);
            if (!signer.matches(
                    canonical.contentSha256(), signed.contentSha256())
                    || !signer.matches(
                    signer.sign(canonical, credential.secret()),
                    signed.signature())) {
                throw authentication(DdcErrorStatus.SIGNATURE_INVALID);
            }
        }
        DdcRpcOperation operation;
        try {
            operation = operationResolver.resolve(fullMethodName);
        } catch (IllegalArgumentException unknownMethod) {
            throw denied();
        }
        DdcRpcScopeExtractor.Scope scope;
        try {
            scope = scopeExtractor.extract(fullMethodName, request);
        } catch (IllegalArgumentException invalidScope) {
            throw DdcRpcProviderExceptionMapper.status(
                    Status.INVALID_ARGUMENT,
                    DdcErrorStatus.INVALID_REQUEST.getStatus(),
                    DdcErrorStatus.INVALID_REQUEST.getMessage(),
                    false
            );
        }
        if (!properties.isSignatureEnabled()) {
            return new Authentication(localPrincipal(scope));
        }
        if (!credential.permits(
                scope.clientType(),
                operation.name(),
                scope.appCode(),
                scope.env(),
                scope.bizCode())) {
            throw denied();
        }
        consumeNonce(credential, signed.nonce(), timestamp, now, skew, operation);
        return new Authentication(
                credential.principal(
                        scope.appCode(), scope.env(), scope.bizCode())
        );
    }

    private Headers headers(Metadata metadata) {
        String accessKey = metadata.get(DdcRpcMetadataKeys.ACCESS_KEY);
        String timestamp = metadata.get(DdcRpcMetadataKeys.TIMESTAMP);
        String nonce = metadata.get(DdcRpcMetadataKeys.NONCE);
        String contentSha256 = metadata.get(DdcRpcMetadataKeys.CONTENT_SHA256);
        String signature = metadata.get(DdcRpcMetadataKeys.SIGNATURE);
        String contractVersion = metadata.get(
                DdcRpcMetadataKeys.CONTRACT_VERSION);
        if (accessKey == null || timestamp == null || nonce == null
                || contentSha256 == null || signature == null
                || contractVersion == null) {
            throw authentication(DdcErrorStatus.SIGNATURE_REQUIRED);
        }
        if (accessKey.isBlank() || accessKey.length() > 256
                || !NONCE.matcher(nonce).matches()
                || !HEX_64.matcher(contentSha256).matches()
                || !HEX_64.matcher(signature).matches()
                || !DdcRpcCanonicalRequest.CONTRACT_VERSION.equals(
                contractVersion)) {
            throw authentication(DdcErrorStatus.SIGNATURE_INVALID);
        }
        return new Headers(
                accessKey, timestamp, nonce, contentSha256, signature);
    }

    private long timestamp(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException invalid) {
            throw authentication(DdcErrorStatus.SIGNATURE_INVALID);
        }
    }

    private void consumeNonce(
            DdcHmacCredential credential,
            String nonce,
            long timestamp,
            long now,
            long skew,
            DdcRpcOperation operation) {
        try {
            Duration ttl = Duration.ofMillis(Math.max(
                    1L, timestamp + skew - now));
            if (!nonceStore.markIfAbsent(
                    credential.credentialId(), nonce, ttl)) {
                throw authentication(DdcErrorStatus.SIGNATURE_REPLAY);
            }
        } catch (StatusRuntimeException rejected) {
            throw rejected;
        } catch (RuntimeException unavailable) {
            if (write(operation)) {
                throw DdcRpcProviderExceptionMapper.status(
                        Status.UNAVAILABLE,
                        "DDC_NONCE_STORE_UNAVAILABLE",
                        "DDC nonce store is unavailable",
                        true
                );
            }
        }
    }

    private boolean write(DdcRpcOperation operation) {
        return switch (operation) {
            case CONFIG_PULL,
                 REGISTRY_READ,
                 MANAGEMENT_CONFIG_READ,
                 MANAGEMENT_TASK_READ,
                 MANAGEMENT_INSTANCE_READ,
                 MANAGEMENT_SCOPE_READ,
                 MANAGEMENT_REGISTRY_READ,
                 MANAGEMENT_CATALOG_READ -> false;
            default -> true;
        };
    }

    private DdcServicePrincipal localPrincipal(
            DdcRpcScopeExtractor.Scope scope) {
        return new DdcServicePrincipal(
                "local-dev",
                scope.clientType(),
                Set.of("*"),
                Set.of("*"),
                Set.of("*"),
                Set.of("*"),
                scope.appCode(),
                scope.env(),
                scope.bizCode()
        );
    }

    private StatusRuntimeException authentication(DdcErrorStatus error) {
        return DdcRpcProviderExceptionMapper.status(
                Status.UNAUTHENTICATED,
                error.getStatus(),
                error.getMessage(),
                false
        );
    }

    private StatusRuntimeException denied() {
        return DdcRpcProviderExceptionMapper.status(
                Status.PERMISSION_DENIED,
                "DDC_HMAC_SCOPE_DENIED",
                "DDC HMAC credential scope denied",
                false
        );
    }

    private void validateConfiguration() {
        if (properties.getAllowedClockSkewSeconds() <= 0) {
            throw new IllegalStateException(
                    "DDC RPC allowed clock skew must be positive");
        }
        if (properties.isSignatureEnabled() && credentialRegistry.isEmpty()) {
            throw new IllegalStateException(
                    "DDC RPC credentials are required when signatures are enabled");
        }
        if (properties.isSignatureEnabled() && nonceStore == null) {
            throw new IllegalStateException(
                    "Redis DdcNonceStore is required when DDC RPC signatures are enabled");
        }
    }

    private final class AuthenticatingListener<ReqT, RespT>
            extends ServerCall.Listener<ReqT> {

        private final ServerCall<ReqT, RespT> call;
        private final Metadata headers;
        private final ServerCall.Listener<ReqT> delegate;
        private Context authenticatedContext;
        private boolean closed;
        private boolean ready;

        private AuthenticatingListener(
                ServerCall<ReqT, RespT> call,
                Metadata headers,
                ServerCall.Listener<ReqT> delegate) {
            this.call = call;
            this.headers = headers;
            this.delegate = delegate;
        }

        @Override
        public void onMessage(ReqT request) {
            if (closed) {
                return;
            }
            if (!(request instanceof Message message)) {
                reject(DdcRpcProviderExceptionMapper.status(
                        Status.INVALID_ARGUMENT,
                        DdcErrorStatus.INVALID_REQUEST.getStatus(),
                        DdcErrorStatus.INVALID_REQUEST.getMessage(),
                        false
                ));
                return;
            }
            try {
                Authentication authentication = authenticate(
                        call.getMethodDescriptor().getFullMethodName(),
                        headers,
                        message
                );
                authenticatedContext = authentication.principal()
                        .bind(Context.current());
                authenticatedContext.run(() -> delegate.onMessage(request));
                if (ready) {
                    authenticatedContext.run(delegate::onReady);
                }
            } catch (StatusRuntimeException failure) {
                reject(failure);
            }
        }

        @Override
        public void onHalfClose() {
            if (closed) {
                return;
            }
            if (authenticatedContext == null) {
                reject(DdcRpcProviderExceptionMapper.status(
                        Status.INVALID_ARGUMENT,
                        DdcErrorStatus.INVALID_REQUEST.getStatus(),
                        DdcErrorStatus.INVALID_REQUEST.getMessage(),
                        false
                ));
                return;
            }
            authenticatedContext.run(delegate::onHalfClose);
        }

        @Override
        public void onCancel() {
            callback(delegate::onCancel);
        }

        @Override
        public void onComplete() {
            callback(delegate::onComplete);
        }

        @Override
        public void onReady() {
            ready = true;
            if (authenticatedContext != null) {
                authenticatedContext.run(delegate::onReady);
            }
        }

        private void callback(Runnable action) {
            if (authenticatedContext == null) {
                action.run();
            } else {
                authenticatedContext.run(action);
            }
        }

        private void reject(StatusRuntimeException failure) {
            closed = true;
            call.close(
                    failure.getStatus(),
                    failure.getTrailers() == null
                            ? new Metadata()
                            : failure.getTrailers()
            );
        }
    }

    private record Headers(
            String accessKey,
            String timestamp,
            String nonce,
            String contentSha256,
            String signature) {
    }

    private record Authentication(DdcServicePrincipal principal) {
    }
}
