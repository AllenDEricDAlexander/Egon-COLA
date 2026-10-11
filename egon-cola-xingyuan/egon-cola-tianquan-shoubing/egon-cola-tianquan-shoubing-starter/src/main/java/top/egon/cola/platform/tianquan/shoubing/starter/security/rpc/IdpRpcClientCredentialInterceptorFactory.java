package top.egon.cola.platform.tianquan.shoubing.starter.security.rpc;

import io.grpc.CallOptions;
import io.grpc.CallCredentials;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import jakarta.validation.Validator;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceOAuth2Client;
import top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceTokenRequest;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties.RpcConsumerBindingProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties.RpcConsumerModeEnum;
import top.egon.cola.platform.tianquan.shoubing.contract.IdpPrincipal;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpAuthenticationToken;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.TokenAuditProofBO;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import top.egon.cola.component.rpc.consumer.interceptor.RpcClientInterceptorFactory;
import top.egon.cola.component.rpc.consumer.interceptor.RpcClientInvocation;
import top.egon.cola.component.rpc.context.invocation.RpcMetadataKeys;
import top.egon.cola.platform.tianquan.shoubing.starter.security.VerifiedUserTokenCarrier;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Selects a fixed local RPC credential strategy. USER relay remains request scoped; SERVICE
 * acquisition runs on the gRPC application executor and reuses the existing OAuth2 client cache.
 */
@Slf4j
@RequiredArgsConstructor(staticName = "createConfigured")
public final class IdpRpcClientCredentialInterceptorFactory
        implements RpcClientInterceptorFactory {

    private static final int MAX_CREDENTIAL_LENGTH = 8192;

    @Qualifier("idpServiceOAuth2Client") private final IdpServiceOAuth2Client serviceClient;
    @Qualifier("idpStarterProperties") private final IdpStarterProperties properties;
    @Qualifier("defaultValidator") private final Validator validator;

    /** Preserves the existing USER-relay-only entry point. */
    public IdpRpcClientCredentialInterceptorFactory() { this(null, new IdpStarterProperties(), null); }

    @PostConstruct
    public void validateConfiguration() {
        if (!configured()) { return; }
        new ValidationUtils(Objects.requireNonNull(validator, "defaultValidator")).validate(properties.getRpc());
        if (properties.getRpc().getConsumer().getBindings().values().stream().anyMatch(binding -> binding.getMode() == RpcConsumerModeEnum.SERVICE)
                && serviceClient == null) {
            throw new IllegalStateException("SERVICE RPC requires idpServiceOAuth2Client");
        }
    }

    /**
     * Captures the trusted request-scoped USER token for the current RPC invocation.
     *
     * @param invocation current RPC invocation
     * @return client interceptor adding at most one Bearer credential
     */
    @Override
    public ClientInterceptor create(RpcClientInvocation invocation) {
        Objects.requireNonNull(invocation, "invocation");
        if (!configured()) { return userRelay(VerifiedUserTokenCarrier.currentOrNull(), false, null); }
        validateConfiguration();
        RpcConsumerBindingProperties binding = properties.getRpc().getConsumer().getBindings().values().stream()
                .filter(candidate -> matches(candidate, invocation)).findFirst()
                .orElseThrow(() -> new IllegalStateException("RPC target binding is unavailable"));
        if (binding.getMode() == RpcConsumerModeEnum.USER_RELAY) {
            // A SERVICE callback must never borrow a servlet token left on a reused worker.
            String token = IdpRpcSecurityContext.currentPrincipalOrNull() == null
                    ? VerifiedUserTokenCarrier.currentOrNull() : IdpRpcSecurityContext.currentTokenOrNull();
            if (token == null) { throw new IllegalStateException("verified USER RPC credential is unavailable"); }
            return userRelay(token, true, binding.getFullMethodName());
        }
        IdpPrincipal principal = IdpRpcSecurityContext.currentPrincipalOrNull();
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (principal == null && authentication != null && authentication.isAuthenticated()
                && (authentication instanceof IdpAuthenticationToken || authentication.getDetails() instanceof TokenAuditProofBO)
                && authentication.getPrincipal() instanceof IdpPrincipal verified) {
            principal = verified;
        }
        String tenant = binding.getScopeContext() == ServiceTokenContext.TENANT && principal != null ? principal.tenantId() : null;
        String registration = binding.getScopeContext() == ServiceTokenContext.PLATFORM ? binding.getRegistrationId()
                : tenant == null ? null : binding.getTenantRegistrationIds().get(tenant);
        if (registration == null) { throw new IllegalStateException("RPC tenant registration is unavailable"); }
        final IdpServiceTokenRequest request = new IdpServiceTokenRequest(registration, binding.getAppId(), binding.getResourceUri(),
                binding.getScopeContext(), tenant, binding.getScopes());
        final Context caller = Context.current();
        final String fullMethodName = binding.getFullMethodName();
        return new ClientInterceptor() {
            @Override public <RequestT, ResponseT> ClientCall<RequestT, ResponseT> interceptCall(
                    MethodDescriptor<RequestT, ResponseT> method, CallOptions options, Channel next) {
                if (options.getCredentials() != null) { throw new IllegalStateException("RPC call credentials already exist"); }
                if (!fullMethodName.equals(method.getFullMethodName())) { throw new IllegalStateException("RPC method binding does not match"); }
                Context.CancellableContext credentialsContext = caller.withCancellation();
                CallCredentials credentials = serviceCredentials(request, credentialsContext);
                ClientCall<RequestT, ResponseT> delegate;
                try { delegate = next.newCall(method, options.withCallCredentials(credentials)); }
                catch (RuntimeException createFailure) { credentialsContext.cancel(createFailure); throw createFailure; }
                return new ForwardingClientCall.SimpleForwardingClientCall<>(delegate) {
                    @Override public void start(Listener<ResponseT> listener, Metadata headers) {
                        try {
                            rejectExistingAuthorization(headers);
                            super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<>(listener) {
                                @Override public void onClose(Status status, Metadata trailers) {
                                    try { super.onClose(status, trailers); } finally { credentialsContext.cancel(null); }
                                }
                            }, headers);
                        } catch (RuntimeException startFailure) { credentialsContext.cancel(startFailure); throw startFailure; }
                    }
                    @Override public void cancel(String message, Throwable cause) {
                        credentialsContext.cancel(cause);
                        super.cancel(message, cause);
                    }
                };
            }
        };
    }

    private CallCredentials serviceCredentials(IdpServiceTokenRequest request, Context.CancellableContext context) {
        return new CallCredentials() {
            @Override public void applyRequestMetadata(RequestInfo info, Executor appExecutor, MetadataApplier applier) {
                AtomicBoolean completed = new AtomicBoolean();
                Context.CancellationListener cancellation = new Context.CancellationListener() {
                    @Override public void cancelled(Context ignored) {
                        try {
                            if (completed.compareAndSet(false, true)) {
                                applier.fail(Status.CANCELLED.withDescription("RPC credential acquisition cancelled"));
                            }
                        } finally { context.removeListener(this); }
                    }
                };
                context.addListener(cancellation, Runnable::run);
                if (completed.get()) { context.removeListener(cancellation); return; }
                try {
                    appExecutor.execute(context.wrap(() -> {
                        try {
                            if (context.isCancelled()) { cancellation.cancelled(context); return; }
                            if (completed.get()) { return; }
                            var token = serviceClient.authorize(request);
                            Metadata metadata = new Metadata();
                            metadata.put(RpcMetadataKeys.AUTHORIZATION, bearer(token.getTokenValue()));
                            if (context.isCancelled()) { cancellation.cancelled(context); return; }
                            if (completed.compareAndSet(false, true)) { applier.apply(metadata); }
                        } catch (RuntimeException acquisitionFailure) {
                            if (completed.compareAndSet(false, true)) {
                                applier.fail(Status.UNAVAILABLE.withDescription("RPC credential acquisition unavailable"));
                            }
                        } finally { context.removeListener(cancellation); }
                    }));
                } catch (RuntimeException schedulingFailure) {
                    try {
                        if (completed.compareAndSet(false, true)) {
                            applier.fail(Status.UNAVAILABLE.withDescription("RPC credential acquisition unavailable"));
                        }
                    } finally { context.removeListener(cancellation); }
                }
            }
            @Override public void thisUsesUnstableApi() { }
        };
    }

    private boolean configured() {
        return properties != null && properties.getRpc().isEnabled() && properties.getRpc().getConsumer().isEnabled();
    }

    private boolean matches(RpcConsumerBindingProperties binding, RpcClientInvocation invocation) {
        var process = invocation.processIdentity();
        return process != null && binding.getServiceName().equals(invocation.serviceName()) && binding.getGroup().equals(invocation.group())
                && binding.getVersion().equals(invocation.version()) && binding.getFullMethodName().equals(invocation.fullMethodName())
                && binding.getProcessIdentity().get("applicationName").equals(process.applicationName())
                && binding.getProcessIdentity().get("env").equals(process.env());
    }

    private ClientInterceptor userRelay(String token, boolean strict, String fullMethodName) {
        String authorization = token == null ? null : bearer(token);
        return new ClientInterceptor() {
            @Override
            public <RequestT, ResponseT> ClientCall<RequestT, ResponseT>
                    interceptCall(
                            MethodDescriptor<RequestT, ResponseT> method,
                            CallOptions callOptions,
                            Channel next
                    ) {
                if (strict && callOptions.getCredentials() != null) { throw new IllegalStateException("RPC call credentials already exist"); }
                if (strict && !fullMethodName.equals(method.getFullMethodName())) { throw new IllegalStateException("RPC method binding does not match"); }
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
                            rejectExistingAuthorization(headers);
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

    private void rejectExistingAuthorization(Metadata headers) {
        Iterable<String> existing = headers.getAll(RpcMetadataKeys.AUTHORIZATION);
        if (existing != null && existing.iterator().hasNext()) { throw new IllegalStateException("authorization metadata already exists"); }
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
