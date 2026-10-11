package top.egon.cola.platform.tianquan.shoubing.starter.security.rpc;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Validator;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.rpc.context.invocation.RpcMetadataKeys;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.contract.IdpPrincipal;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.platform.tianquan.shoubing.starter.security.AccessTokenVerification;
import top.egon.cola.platform.tianquan.shoubing.starter.security.UserAccessTokenVerifier;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpJwtVerifier;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties.RpcPrincipalKindEnum;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties.RpcProviderMethodProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.IdpAuthorizationObservationContextBO;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Applies a fixed provider-method policy before any callback. The original optional USER adapter
 * stays available when RPC policies are disabled; configured calls verify USER or SERVICE once.
 */
@Slf4j
@RequiredArgsConstructor(staticName = "createConfigured")
public final class IdpRpcBearerServerInterceptor
        implements ServerInterceptor {

    private static final int MAX_CREDENTIAL_LENGTH = 8192;
    private static final String BEARER_PREFIX = "Bearer ";

    @Qualifier("userAccessTokenVerifier")
    private final UserAccessTokenVerifier verifier;
    @Qualifier("idpJwtVerifier") private final IdpJwtVerifier combinedVerifier;
    @Qualifier("idpStarterProperties") private final IdpStarterProperties properties;
    @Qualifier("idpObservedVerificationComponent") private final IdpObservedVerificationComponent observations;
    @Qualifier("defaultValidator") private final Validator validator;

    /** Preserves the existing optional USER-only adapter. */
    public IdpRpcBearerServerInterceptor(UserAccessTokenVerifier verifier) {
        this(verifier, null, new IdpStarterProperties(), null, null);
    }

    @PostConstruct
    public void validateConfiguration() {
        if (configured()) {
            new ValidationUtils(Objects.requireNonNull(validator, "defaultValidator")).validate(properties.getRpc());
            Objects.requireNonNull(combinedVerifier, "idpJwtVerifier");
        }
    }

    /**
     * Verifies the method-bound credential and scopes identity state to every callback.
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
        RpcProviderMethodProperties policy = null;
        if (configured()) {
            new ValidationUtils(Objects.requireNonNull(validator, "defaultValidator")).validate(properties.getRpc());
            String method = call.getMethodDescriptor() == null ? null : call.getMethodDescriptor().getFullMethodName();
            policy = properties.getRpc().getProvider().getMethods().get(method);
            if (policy == null) { return beforeVerificationReject(call, Status.PERMISSION_DENIED, "rpc_method_policy_missing"); }
        }
        List<String> authorization = values(
                headers.getAll(RpcMetadataKeys.AUTHORIZATION)
        );
        if (authorization.isEmpty()) {
            if (policy != null && policy.isRequired()) { return beforeVerificationReject(call, Status.UNAUTHENTICATED, "authorization_required"); }
            return next.startCall(call, headers);
        }
        if (authorization.size() != 1) {
            return beforeVerificationReject(call, Status.UNAUTHENTICATED, "authorization_header_invalid");
        }
        String token = token(authorization.getFirst());
        if (token == null) {
            return beforeVerificationReject(call, Status.UNAUTHENTICATED, "authorization_header_invalid");
        }
        AccessTokenVerification<?> verification;
        if (policy == null) { verification = verifier.verify(token); }
        else if (combinedVerifier == null) { return reject(call, Status.UNAVAILABLE, "oauth_security_state_unavailable"); }
        else {
            RpcProviderMethodProperties target = policy;
            verification = observations == null ? combinedVerifier.verifyAny(token)
                    : observations.observe(capture -> combinedVerifier.verifyAny(token, capture), target.getExpectedResourceUri(), MDC.get("traceId"));
        }
        if (verification instanceof AccessTokenVerification.Unavailable<?>) {
            return reject(call, Status.UNAVAILABLE, "oauth_security_state_unavailable");
        }
        if (!(verification instanceof AccessTokenVerification.Valid<?> valid)) {
            String reason = verification instanceof AccessTokenVerification.Expired<?>
                    ? "jwt_expired"
                    : "jwt_invalid";
            return reject(call, reason);
        }
        IdpPrincipal principal = valid.principal();
        IdpAuthorizationObservationContextBO marker = new IdpAuthorizationObservationContextBO();
        if (observations != null && observations.enabled() && valid.proof() == null) {
            marker.close();
            return reject(call, Status.UNAVAILABLE, "oauth_security_state_unavailable");
        }
        if (valid.proof() != null && validator != null) { new ValidationUtils(validator).validate(valid.proof()); }
        if (policy != null && !allows(policy, principal, valid.scopes())) {
            try {
                boolean confirmed = marker.denyOnce(() -> observations == null || observations.authorizationDenied(valid.proof()));
                return reject(call, confirmed ? Status.PERMISSION_DENIED : Status.UNAVAILABLE,
                        confirmed ? "rpc_authorization_denied" : "oauth_security_state_unavailable");
            } finally { marker.close(); }
        }
        Context context = IdpRpcSecurityContext.with(principal, token, valid.proof(), marker, valid.scopes());
        ServerCall.Listener<RequestT> delegate;
        try {
            delegate = Contexts.interceptCall(context, call, headers, (scopedCall, scopedHeaders) -> {
                var accepted = new AtomicReference<ServerCall.Listener<RequestT>>();
                IdpRpcSecurityContext.runWithSpringSecurity(principal, valid.scopes(), valid.proof(),
                        () -> accepted.set(next.startCall(scopedCall, scopedHeaders)));
                return Objects.requireNonNull(accepted.get(), "provider listener");
            });
        } catch (RuntimeException | Error providerFailure) { marker.close(); throw providerFailure; }
        boolean policyEnabled = policy != null;
        AtomicBoolean terminated = new AtomicBoolean();
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
                finish(super::onCancel);
            }

            @Override
            public void onComplete() {
                finish(super::onComplete);
            }

            @Override
            public void onReady() {
                run(super::onReady);
            }

            private void run(Runnable callback) {
                if (terminated.get() && policyEnabled) { return; }
                try {
                    context.run(() -> IdpRpcSecurityContext.runWithSpringSecurity(principal, valid.scopes(), valid.proof(), callback));
                } catch (RuntimeException | Error providerFailure) {
                    terminated.set(true);
                    marker.close();
                    throw providerFailure;
                }
            }

            private void finish(Runnable callback) {
                // Keep the former optional USER adapter's callback contract; its marker stays closed.
                if (!terminated.compareAndSet(false, true) && policyEnabled) { return; }
                try { context.run(() -> IdpRpcSecurityContext.runWithSpringSecurity(principal, valid.scopes(), valid.proof(), callback)); }
                finally { marker.close(); }
            }
        };
    }

    private boolean configured() {
        return properties != null && properties.getRpc().isEnabled() && properties.getRpc().getProvider().isEnabled();
    }

    private boolean allows(RpcProviderMethodProperties policy, IdpPrincipal principal, Set<String> scopes) {
        boolean kind = policy.getPrincipalKind() == RpcPrincipalKindEnum.USER_OR_SERVICE
                || policy.getPrincipalKind() == RpcPrincipalKindEnum.USER && principal instanceof IdentityPrincipal
                || policy.getPrincipalKind() == RpcPrincipalKindEnum.SERVICE && principal instanceof ServiceIdentityPrincipal;
        if (!kind || !scopes.containsAll(policy.getRequiredScopes())) { return false; }
        if (principal instanceof ServiceIdentityPrincipal service) {
            return service.scopeContext() == policy.getScopeContext() && service.resourceUri().equals(policy.getExpectedResourceUri())
                    && (service.scopeContext() == ServiceTokenContext.PLATFORM ? service.tenantId() == null : service.tenantId() != null);
        }
        return principal instanceof IdentityPrincipal user && policy.getScopeContext() == ServiceTokenContext.TENANT
                && user.tenantId() != null && user.audience().equals(Set.of(policy.getExpectedResourceUri().toASCIIString()));
    }

    private <RequestT, ResponseT> ServerCall.Listener<RequestT> beforeVerificationReject(
            ServerCall<RequestT, ResponseT> call, Status status, String reason) {
        if (observations != null && !observations.authenticationFailure()) {
            return reject(call, Status.UNAVAILABLE, "oauth_security_state_unavailable");
        }
        return reject(call, status, reason);
    }

    private List<String> values(Iterable<String> values) {
        if (values == null) return List.of();
        List<String> result = new ArrayList<>();
        for (String value : values) { result.add(value); if (result.size() == 2) { break; } }
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
        return reject(call, Status.UNAUTHENTICATED, reason);
    }

    private <RequestT, ResponseT> ServerCall.Listener<RequestT> reject(
            ServerCall<RequestT, ResponseT> call,
            Status status,
            String reason
    ) {
        call.close(
                status.withDescription(reason),
                new Metadata()
        );
        return new ServerCall.Listener<>() {
        };
    }
}
