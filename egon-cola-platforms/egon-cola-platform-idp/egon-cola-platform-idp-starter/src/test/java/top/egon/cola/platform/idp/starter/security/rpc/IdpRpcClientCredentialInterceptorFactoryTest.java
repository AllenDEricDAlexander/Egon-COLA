package top.egon.cola.platform.idp.starter.security.rpc;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import top.egon.cola.component.rpc.consumer.interceptor.RpcClientInvocation;
import top.egon.cola.component.rpc.context.invocation.RpcMetadataKeys;
import top.egon.cola.platform.idp.contract.AuthenticationContext;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.idp.starter.security.VerifiedUserTokenCarrier;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class IdpRpcClientCredentialInterceptorFactoryTest {

    private final IdpRpcClientCredentialInterceptorFactory factory =
            new IdpRpcClientCredentialInterceptorFactory();

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void forwardsOnlyTheVerifiedServletUserToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(request)
        );
        VerifiedUserTokenCarrier.set(request, "servlet-token");

        Metadata headers = start(factory.create(invocation()));

        assertThat(headers.getAll(RpcMetadataKeys.AUTHORIZATION))
                .containsExactly("Bearer servlet-token");
    }

    @Test
    void forwardsTheVerifiedRpcUserTokenAcrossAnotherHop() {
        Metadata[] observed = new Metadata[1];

        IdpRpcSecurityContext.with(principal(), "rpc-token").run(() ->
                observed[0] = start(factory.create(invocation()))
        );

        assertThat(observed[0].getAll(RpcMetadataKeys.AUTHORIZATION))
                .containsExactly("Bearer rpc-token");
    }

    @Test
    void leavesAuthorizationAbsentForAnonymousCalls() {
        Metadata headers = start(factory.create(invocation()));

        assertThat(headers.getAll(RpcMetadataKeys.AUTHORIZATION)).isNull();
    }

    @Test
    void rejectsPreexistingAuthorizationInsteadOfMergingCredentials() {
        Metadata headers = new Metadata();
        headers.put(RpcMetadataKeys.AUTHORIZATION, "Bearer untrusted-token");
        CapturingChannel channel = new CapturingChannel();
        AtomicReference<ClientCall<Object, Object>> call =
                new AtomicReference<>();

        IdpRpcSecurityContext.with(principal(), "verified-token").run(() -> {
            call.set(factory.create(invocation())
                    .interceptCall(method(), CallOptions.DEFAULT, channel));
        });
        assertThatThrownBy(() -> call.get().start(
                        new ClientCall.Listener<>() {
                        },
                        headers
                )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("authorization");

        assertThat(channel.call.started).isFalse();
        assertThat(headers.getAll(RpcMetadataKeys.AUTHORIZATION))
                .containsExactly("Bearer untrusted-token");
    }

    private Metadata start(ClientInterceptor interceptor) {
        CapturingChannel channel = new CapturingChannel();
        ClientCall<Object, Object> call = interceptor.interceptCall(
                method(),
                CallOptions.DEFAULT,
                channel
        );
        Metadata headers = new Metadata();
        call.start(new ClientCall.Listener<>() {
        }, headers);
        assertThat(channel.call.started).isTrue();
        return headers;
    }

    @SuppressWarnings("unchecked")
    private MethodDescriptor<Object, Object> method() {
        return mock(MethodDescriptor.class);
    }

    private RpcClientInvocation invocation() {
        return mock(RpcClientInvocation.class);
    }

    private IdentityPrincipal principal() {
        Instant issuedAt = Instant.parse("2026-08-15T00:00:00Z");
        return new IdentityPrincipal(
                "user-1",
                "tenant-1",
                "token-1",
                Set.of("resource-1"),
                issuedAt,
                issuedAt.plusSeconds(300),
                AuthenticationContext.of("PASSWORD", issuedAt)
        );
    }

    private static final class CapturingChannel extends Channel {

        private final CapturingClientCall call = new CapturingClientCall();

        @Override
        @SuppressWarnings("unchecked")
        public <RequestT, ResponseT> ClientCall<RequestT, ResponseT> newCall(
                MethodDescriptor<RequestT, ResponseT> methodDescriptor,
                CallOptions callOptions
        ) {
            return (ClientCall<RequestT, ResponseT>) call;
        }

        @Override
        public String authority() {
            return "test";
        }
    }

    private static final class CapturingClientCall
            extends ClientCall<Object, Object> {

        private boolean started;

        @Override
        public void start(Listener<Object> listener, Metadata headers) {
            started = true;
        }

        @Override
        public void request(int numMessages) {
        }

        @Override
        public void cancel(String message, Throwable cause) {
        }

        @Override
        public void halfClose() {
        }

        @Override
        public void sendMessage(Object message) {
        }
    }
}
