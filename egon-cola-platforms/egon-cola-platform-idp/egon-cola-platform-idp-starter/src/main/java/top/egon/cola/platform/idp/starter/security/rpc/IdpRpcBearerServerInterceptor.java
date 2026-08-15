package top.egon.cola.platform.idp.starter.security.rpc;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import top.egon.cola.component.rpc.context.invocation.RpcMetadataKeys;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.idp.starter.security.AccessTokenVerification;
import top.egon.cola.platform.idp.starter.security.UserAccessTokenVerifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Verifies an optional RPC USER Bearer token before invoking provider callbacks. Anonymous calls
 * remain anonymous; malformed, duplicate, expired, or invalid credentials fail closed.
 */
public final class IdpRpcBearerServerInterceptor
        implements ServerInterceptor {

    private static final int MAX_CREDENTIAL_LENGTH = 8192;
    private static final String BEARER_PREFIX = "Bearer ";

    private final UserAccessTokenVerifier verifier;

    /**
     * Creates the interceptor with the shared USER-token verifier.
     *
     * @param verifier USER access-token verifier
     */
    public IdpRpcBearerServerInterceptor(UserAccessTokenVerifier verifier) {
        this.verifier = Objects.requireNonNull(verifier, "verifier");
    }

    /**
     * Verifies one optional USER Bearer credential and scopes identity state to every callback.
     *
     * @param call current server call
     * @param headers incoming metadata
     * @param next downstream handler
     * @return listener for the accepted call, or a no-op listener after rejection
     */
    @Override
    public <RequestT, ResponseT> ServerCall.Listener<RequestT> interceptCall(
            ServerCall<RequestT, ResponseT> call,
            Metadata headers,
            ServerCallHandler<RequestT, ResponseT> next
    ) {
        List<String> authorization = values(
                headers.getAll(RpcMetadataKeys.AUTHORIZATION)
        );
        if (authorization.isEmpty()) {
            return next.startCall(call, headers);
        }
        if (authorization.size() != 1) {
            return reject(call, "authorization_header_invalid");
        }
        String token = token(authorization.getFirst());
        if (token == null) {
            return reject(call, "authorization_header_invalid");
        }
        AccessTokenVerification<IdentityPrincipal> verification =
                verifier.verify(token);
        if (!(verification instanceof AccessTokenVerification.Valid<?> valid)) {
            String reason = verification instanceof AccessTokenVerification.Expired<?>
                    ? "jwt_expired"
                    : "jwt_invalid";
            return reject(call, reason);
        }
        IdentityPrincipal principal = (IdentityPrincipal) valid.principal();
        Context context = IdpRpcSecurityContext.with(principal, token);
        ServerCall.Listener<RequestT> delegate = Contexts.interceptCall(
                context,
                call,
                headers,
                next
        );
        return new ForwardingServerCallListener
                .SimpleForwardingServerCallListener<>(delegate) {
            @Override
            public void onMessage(RequestT message) {
                run(() -> super.onMessage(message));
            }

            @Override
            public void onHalfClose() {
                run(super::onHalfClose);
            }

            @Override
            public void onCancel() {
                run(super::onCancel);
            }

            @Override
            public void onComplete() {
                run(super::onComplete);
            }

            @Override
            public void onReady() {
                run(super::onReady);
            }

            private void run(Runnable callback) {
                context.run(() -> IdpRpcSecurityContext
                        .runWithSpringSecurity(principal, callback));
            }
        };
    }

    private List<String> values(Iterable<String> values) {
        if (values == null) return List.of();
        List<String> result = new ArrayList<>();
        values.forEach(result::add);
        return List.copyOf(result);
    }

    private String token(String authorization) {
        if (authorization == null
                || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = authorization.substring(BEARER_PREFIX.length());
        if (token.isBlank()
                || token.length() > MAX_CREDENTIAL_LENGTH
                || !token.equals(token.trim())) {
            return null;
        }
        for (int index = 0; index < token.length(); index++) {
            char value = token.charAt(index);
            if (value < 0x21 || value > 0x7e) return null;
        }
        return token;
    }

    private <RequestT, ResponseT> ServerCall.Listener<RequestT> reject(
            ServerCall<RequestT, ResponseT> call,
            String reason
    ) {
        call.close(
                Status.UNAUTHENTICATED.withDescription(reason),
                new Metadata()
        );
        return new ServerCall.Listener<>() {
        };
    }
}
