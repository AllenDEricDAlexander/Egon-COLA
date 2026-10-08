package top.egon.cola.platform.tianquan.shoubing.starter.security.rpc;

import io.grpc.Attributes;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import top.egon.cola.component.rpc.context.invocation.RpcMetadataKeys;
import top.egon.cola.platform.tianquan.shoubing.contract.AuthenticationContext;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.starter.security.AccessTokenVerification;
import top.egon.cola.platform.tianquan.shoubing.starter.security.UserAccessTokenVerifier;
import top.egon.cola.platform.tianquan.shoubing.starter.security.VerifiedUserTokenCarrier;

import java.time.Instant;
import java.lang.reflect.Constructor;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class IdpRpcBearerServerInterceptorTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void passesAnonymousCallsThroughWithoutCreatingAuthentication() {
        UserAccessTokenVerifier verifier = mock(UserAccessTokenVerifier.class);
        RecordingHandler next = new RecordingHandler();

        ServerCall.Listener<String> listener =
                new IdpRpcBearerServerInterceptor(verifier).interceptCall(
                        new RecordingServerCall(),
                        new Metadata(),
                        next
                );

        assertThat(listener).isSameAs(next.listener);
        assertThat(next.calls).isOne();
        verifyNoInteractions(verifier);
    }

    @Test
    void verifiesUserAndRestoresSecurityContextForEveryCallback() {
        UserAccessTokenVerifier verifier = mock(UserAccessTokenVerifier.class);
        IdentityPrincipal principal = principal();
        when(verifier.verify("verified-token"))
                .thenReturn(new AccessTokenVerification.Valid<>(principal));
        Metadata headers = bearer("verified-token");
        Authentication[] observed = new Authentication[1];
        String[] relayedToken = new String[1];
        ServerCallHandler<String, String> next = (call, metadata) ->
                new ServerCall.Listener<>() {
                    @Override
                    public void onHalfClose() {
                        observed[0] = SecurityContextHolder.getContext()
                                .getAuthentication();
                        relayedToken[0] = VerifiedUserTokenCarrier.currentOrNull();
                    }
                };

        ServerCall.Listener<String> listener =
                new IdpRpcBearerServerInterceptor(verifier).interceptCall(
                        new RecordingServerCall(),
                        headers,
                        next
                );
        listener.onHalfClose();

        assertThat(observed[0].getPrincipal()).isEqualTo(principal);
        assertThat(observed[0].getCredentials()).isEqualTo("");
        assertThat(relayedToken[0]).isEqualTo("verified-token");
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNull();
        assertThat(VerifiedUserTokenCarrier.currentOrNull()).isNull();
    }

    @Test
    void rejectsMalformedAndDuplicateAuthorizationBeforeTheHandler() {
        UserAccessTokenVerifier verifier = mock(UserAccessTokenVerifier.class);
        assertRejected(verifier, headers("Basic credential"));
        assertRejected(verifier, bearer("a".repeat(8193)));
        Metadata duplicate = bearer("first");
        duplicate.put(RpcMetadataKeys.AUTHORIZATION, "Bearer second");
        assertRejected(verifier, duplicate);

        verifyNoInteractions(verifier);
    }

    @Test
    void rejectsExpiredAndInvalidTokensBeforeTheHandler() {
        UserAccessTokenVerifier verifier = mock(UserAccessTokenVerifier.class);
        when(verifier.verify("expired"))
                .thenReturn(new AccessTokenVerification.Expired<>());
        when(verifier.verify("invalid"))
                .thenReturn(new AccessTokenVerification.Invalid<>("bad_signature"));

        assertRejected(verifier, bearer("expired"));
        assertRejected(verifier, bearer("invalid"));
    }

    @Test
    void sharedRevocationUnavailableUsesGrpcUnavailableInsteadOfUnauthenticated() {
        UserAccessTokenVerifier verifier = mock(UserAccessTokenVerifier.class);
        when(verifier.verify("state-unavailable"))
                .thenReturn(unavailableOrCurrentInvalidResult());
        RecordingServerCall call = new RecordingServerCall();
        RecordingHandler next = new RecordingHandler();

        new IdpRpcBearerServerInterceptor(verifier).interceptCall(
                call,
                bearer("state-unavailable"),
                next);

        assertThat(call.status.getCode()).isEqualTo(Status.Code.UNAVAILABLE);
        assertThat(call.status.getDescription())
                .isEqualTo("oauth_security_state_unavailable");
        assertThat(next.calls).isZero();
    }

    @SuppressWarnings("unchecked")
    private AccessTokenVerification<IdentityPrincipal> unavailableOrCurrentInvalidResult() {
        try {
            Class<?> unavailableType = Class.forName(
                    "top.egon.cola.platform.tianquan.shoubing.starter.security.AccessTokenVerification$Unavailable");
            Constructor<?> constructor = unavailableType.getConstructor(String.class);
            return (AccessTokenVerification<IdentityPrincipal>) constructor
                    .newInstance("OAUTH_SECURITY_STATE_UNAVAILABLE");
        } catch (ClassNotFoundException | NoSuchMethodException compatibilityBaseline) {
            return new AccessTokenVerification.Invalid<>("OAUTH_SECURITY_STATE_UNAVAILABLE");
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("UNAVAILABLE result must be constructible", failure);
        }
    }

    private void assertRejected(
            UserAccessTokenVerifier verifier,
            Metadata headers
    ) {
        RecordingServerCall call = new RecordingServerCall();
        RecordingHandler next = new RecordingHandler();

        ServerCall.Listener<String> listener =
                new IdpRpcBearerServerInterceptor(verifier).interceptCall(
                        call,
                        headers,
                        next
                );

        assertThat(call.status.getCode())
                .isEqualTo(Status.Code.UNAUTHENTICATED);
        assertThat(next.calls).isZero();
        assertThat(listener).isNotNull();
    }

    private Metadata bearer(String token) {
        return headers("Bearer " + token);
    }

    private Metadata headers(String authorization) {
        Metadata headers = new Metadata();
        headers.put(RpcMetadataKeys.AUTHORIZATION, authorization);
        return headers;
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

    private static final class RecordingHandler
            implements ServerCallHandler<String, String> {

        private final ServerCall.Listener<String> listener =
                new ServerCall.Listener<>() {
                };
        private int calls;

        @Override
        public ServerCall.Listener<String> startCall(
                ServerCall<String, String> call,
                Metadata headers
        ) {
            calls++;
            return listener;
        }
    }

    private static final class RecordingServerCall
            extends ServerCall<String, String> {

        private Status status;

        @Override
        public void request(int numMessages) {
        }

        @Override
        public void sendHeaders(Metadata headers) {
        }

        @Override
        public void sendMessage(String message) {
        }

        @Override
        public void close(Status status, Metadata trailers) {
            this.status = status;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public MethodDescriptor<String, String> getMethodDescriptor() {
            return null;
        }

        @Override
        public Attributes getAttributes() {
            return Attributes.EMPTY;
        }
    }
}
