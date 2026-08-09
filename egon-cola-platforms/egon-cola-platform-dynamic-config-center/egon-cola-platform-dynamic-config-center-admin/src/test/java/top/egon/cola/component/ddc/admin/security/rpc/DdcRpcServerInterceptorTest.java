package top.egon.cola.component.ddc.admin.security.rpc;

import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.grpc.Attributes;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import io.grpc.protobuf.ProtoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.admin.config.DdcAdminProperties;
import top.egon.cola.component.ddc.admin.service.config.DdcConfigFacade;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcConfigRuntimeServiceGrpc;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcScope;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.PullConfigRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.PullConfigResponse;
import top.egon.cola.component.rpc.ddc.mapping.DdcRpcStatusExceptionMapper;
import top.egon.cola.component.rpc.ddc.security.DdcRpcCanonicalRequest;
import top.egon.cola.component.rpc.ddc.security.DdcRpcMetadataKeys;
import top.egon.cola.component.rpc.ddc.security.DdcRpcRequestSigner;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class DdcRpcServerInterceptorTest {

    private static final Instant NOW = Instant.parse("2026-08-09T08:00:00Z");
    private static final String ACCESS_KEY = "sdk-access";
    private static final String SECRET = "sdk-secret";

    private final DdcRpcRequestSigner signer = new DdcRpcRequestSigner();
    private final DdcConfigFacade facade = mock(DdcConfigFacade.class);
    private final AtomicReference<DdcServicePrincipal> principal =
            new AtomicReference<>();

    private DdcRpcServerInterceptor interceptor;

    @BeforeEach
    void setUp() {
        DdcAdminProperties properties = properties(List.of(
                credential(List.of("CONFIG_PULL"), List.of("app-a"))));
        interceptor = new DdcRpcServerInterceptor(
                properties,
                new DdcHmacCredentialRegistry(properties),
                new InMemoryDdcNonceStore(100),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void authenticatesDeterministicRequestAndExposesPrincipalContext() {
        PullConfigRequest request = request("biz-a", "dev", "app-a");

        RecordingServerCall<PullConfigRequest, PullConfigResponse> call =
                invoke(request, signed(request, NOW.toEpochMilli(), "nonce-1"));

        assertThat(call.status()).isNull();
        verify(facade).pull("biz-a", "dev", "app-a");
        assertThat(principal.get().credentialId()).isEqualTo("sdk-a");
        assertThat(principal.get().auditOperator("claimed-user"))
                .isEqualTo("service:sdk-a [requested=claimed-user]");
    }

    @Test
    void rejectsMissingMalformedAndExpiredAuthenticationBeforeFacade() {
        PullConfigRequest request = request("biz-a", "dev", "app-a");

        assertRejected(request, new Metadata(), Status.Code.UNAUTHENTICATED,
                "DDC_SIGNATURE_REQUIRED");

        Metadata malformed = signed(request, NOW.toEpochMilli(), "nonce-bad-ts");
        malformed.removeAll(DdcRpcMetadataKeys.TIMESTAMP);
        malformed.put(DdcRpcMetadataKeys.TIMESTAMP, "not-a-number");
        assertRejected(request, malformed, Status.Code.UNAUTHENTICATED,
                "DDC_SIGNATURE_INVALID");

        Metadata unknownAccess = signed(
                request, NOW.toEpochMilli(), "nonce-unknown-access");
        unknownAccess.removeAll(DdcRpcMetadataKeys.ACCESS_KEY);
        unknownAccess.put(DdcRpcMetadataKeys.ACCESS_KEY, "unknown");
        assertRejected(request, unknownAccess, Status.Code.UNAUTHENTICATED,
                "DDC_SIGNATURE_INVALID");

        Metadata wrongContract = signed(
                request, NOW.toEpochMilli(), "nonce-contract");
        wrongContract.removeAll(DdcRpcMetadataKeys.CONTRACT_VERSION);
        wrongContract.put(DdcRpcMetadataKeys.CONTRACT_VERSION, "v2");
        assertRejected(request, wrongContract, Status.Code.UNAUTHENTICATED,
                "DDC_SIGNATURE_INVALID");

        assertRejected(
                request,
                signed(request, NOW.minusSeconds(301).toEpochMilli(),
                        "nonce-expired"),
                Status.Code.UNAUTHENTICATED,
                "DDC_SIGNATURE_EXPIRED"
        );
        verifyNoInteractions(facade);
    }

    @Test
    void rejectsBadHashBadSignatureAndReplayBeforeFacade() {
        PullConfigRequest request = request("biz-a", "dev", "app-a");
        Metadata badHash = signed(request, NOW.toEpochMilli(), "nonce-hash");
        badHash.removeAll(DdcRpcMetadataKeys.CONTENT_SHA256);
        badHash.put(DdcRpcMetadataKeys.CONTENT_SHA256, "0".repeat(64));
        assertRejected(request, badHash, Status.Code.UNAUTHENTICATED,
                "DDC_SIGNATURE_INVALID");

        Metadata badSignature = signed(
                request, NOW.toEpochMilli(), "nonce-signature");
        badSignature.removeAll(DdcRpcMetadataKeys.SIGNATURE);
        badSignature.put(DdcRpcMetadataKeys.SIGNATURE, "0".repeat(64));
        assertRejected(request, badSignature, Status.Code.UNAUTHENTICATED,
                "DDC_SIGNATURE_INVALID");

        Metadata replay = signed(request, NOW.toEpochMilli(), "nonce-replay");
        RecordingServerCall<PullConfigRequest, PullConfigResponse> first =
                invoke(request, replay);
        assertThat(first.status()).isNull();
        RecordingServerCall<PullConfigRequest, PullConfigResponse> second =
                invoke(request, replay);
        assertThat(second.status().getCode())
                .isEqualTo(Status.Code.UNAUTHENTICATED);
        assertThat(errorCode(second)).isEqualTo("DDC_SIGNATURE_REPLAY");
    }

    @Test
    void rejectsWrongOperationScopeAndUnknownMethodBeforeFacade() {
        DdcAdminProperties wrongOperationProperties = properties(List.of(
                credential(List.of("PUBLISH_ACK"), List.of("app-a"))));
        interceptor = new DdcRpcServerInterceptor(
                wrongOperationProperties,
                new DdcHmacCredentialRegistry(wrongOperationProperties),
                new InMemoryDdcNonceStore(100),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        PullConfigRequest allowedScope = request("biz-a", "dev", "app-a");
        assertRejected(
                allowedScope,
                signed(allowedScope, NOW.toEpochMilli(), "wrong-operation"),
                Status.Code.PERMISSION_DENIED,
                "DDC_HMAC_SCOPE_DENIED"
        );

        DdcAdminProperties scopedProperties = properties(List.of(
                credential(List.of("CONFIG_PULL"), List.of("app-a"))));
        interceptor = new DdcRpcServerInterceptor(
                scopedProperties,
                new DdcHmacCredentialRegistry(scopedProperties),
                new InMemoryDdcNonceStore(100),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        PullConfigRequest wrongScope = request("biz-a", "dev", "app-b");
        assertRejected(
                wrongScope,
                signed(wrongScope, NOW.toEpochMilli(), "wrong-scope"),
                Status.Code.PERMISSION_DENIED,
                "DDC_HMAC_SCOPE_DENIED"
        );

        MethodDescriptor<StringValue, StringValue> unknown =
                MethodDescriptor.<StringValue, StringValue>newBuilder()
                        .setType(MethodDescriptor.MethodType.UNARY)
                        .setFullMethodName("example.UnknownService/Echo")
                        .setRequestMarshaller(ProtoUtils.marshaller(
                                StringValue.getDefaultInstance()))
                        .setResponseMarshaller(ProtoUtils.marshaller(
                                StringValue.getDefaultInstance()))
                        .build();
        StringValue value = StringValue.of("request");
        RecordingServerCall<StringValue, StringValue> unknownCall = invoke(
                unknown,
                value,
                signed(unknown.getFullMethodName(), value,
                        NOW.toEpochMilli(), "unknown-method"),
                ignored -> facade.pull("must", "not", "run")
        );
        assertThat(unknownCall.status().getCode())
                .isEqualTo(Status.Code.PERMISSION_DENIED);
        assertThat(errorCode(unknownCall)).isEqualTo("DDC_HMAC_SCOPE_DENIED");
        verifyNoInteractions(facade);
    }

    @Test
    void failsClosedForNonceStoreOutageOnWriteRequests() {
        DdcAdminProperties properties = properties(List.of(
                credential(List.of("SDK_REGISTER"), List.of("app-a"))));
        interceptor = new DdcRpcServerInterceptor(
                properties,
                new DdcHmacCredentialRegistry(properties),
                (credentialId, nonce, ttl) -> {
                    throw new IllegalStateException("redis-secret");
                },
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        var method = DdcConfigRuntimeServiceGrpc.getRegisterConfigClientMethod();
        var request = top.egon.cola.component.rpc.ddc.contract.proto.v1
                .RegisterConfigClientRequest.newBuilder()
                .setScope(scope("biz-a", "dev", "app-a"))
                .setInstanceId("instance-1")
                .setHost("127.0.0.1")
                .setPid("1")
                .setSdkVersion("1.0.0")
                .setLeaseSeconds(30)
                .setHeartbeatIntervalSeconds(10)
                .build();

        RecordingServerCall<?, ?> call = invoke(
                method,
                request,
                signed(method.getFullMethodName(), request,
                        NOW.toEpochMilli(), "nonce-store-down"),
                ignored -> facade.pull("must", "not", "run")
        );

        assertThat(call.status().getCode()).isEqualTo(Status.Code.UNAVAILABLE);
        assertThat(errorCode(call)).isEqualTo("DDC_NONCE_STORE_UNAVAILABLE");
        assertThat(call.status().getDescription()).doesNotContain("redis-secret");
        verifyNoInteractions(facade);
    }

    @Test
    void preservesReadAvailabilityWhenNonceStoreIsTemporarilyUnavailable() {
        DdcAdminProperties properties = properties(List.of(
                credential(List.of("CONFIG_PULL"), List.of("app-a"))));
        interceptor = new DdcRpcServerInterceptor(
                properties,
                new DdcHmacCredentialRegistry(properties),
                (credentialId, nonce, ttl) -> {
                    throw new IllegalStateException("redis unavailable");
                },
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        PullConfigRequest request = request("biz-a", "dev", "app-a");

        RecordingServerCall<PullConfigRequest, PullConfigResponse> call =
                invoke(request, signed(
                        request, NOW.toEpochMilli(), "read-store-down"));

        assertThat(call.status()).isNull();
        verify(facade).pull("biz-a", "dev", "app-a");
    }

    private RecordingServerCall<PullConfigRequest, PullConfigResponse> invoke(
            PullConfigRequest request,
            Metadata headers) {
        return invoke(
                DdcConfigRuntimeServiceGrpc.getPullConfigMethod(),
                request,
                headers,
                message -> {
                    principal.set(DdcServicePrincipal.current());
                    facade.pull(
                            message.getScope().getBizCode(),
                            message.getScope().getEnv(),
                            message.getScope().getAppCode()
                    );
                }
        );
    }

    private <ReqT extends Message, RespT extends Message>
            RecordingServerCall<ReqT, RespT> invoke(
                    MethodDescriptor<ReqT, RespT> method,
                    ReqT request,
                    Metadata headers,
                    java.util.function.Consumer<ReqT> action) {
        RecordingServerCall<ReqT, RespT> call =
                new RecordingServerCall<>(method);
        AtomicReference<ReqT> received = new AtomicReference<>();
        ServerCallHandler<ReqT, RespT> handler = (ignored, ignoredHeaders) ->
                new ServerCall.Listener<>() {
                    @Override
                    public void onMessage(ReqT message) {
                        received.set(message);
                    }

                    @Override
                    public void onHalfClose() {
                        action.accept(received.get());
                    }
                };
        ServerCall.Listener<ReqT> listener = interceptor.interceptCall(
                call, headers, handler);
        listener.onMessage(request);
        listener.onHalfClose();
        return call;
    }

    private void assertRejected(
            PullConfigRequest request,
            Metadata headers,
            Status.Code status,
            String errorCode) {
        RecordingServerCall<PullConfigRequest, PullConfigResponse> call =
                invoke(request, headers);
        assertThat(call.status().getCode()).isEqualTo(status);
        assertThat(errorCode(call)).isEqualTo(errorCode);
    }

    private String errorCode(RecordingServerCall<?, ?> call) {
        return call.trailers()
                .get(DdcRpcStatusExceptionMapper.ERROR_DETAIL)
                .getCode();
    }

    private Metadata signed(
            PullConfigRequest request,
            long timestamp,
            String nonce) {
        return signed(
                DdcConfigRuntimeServiceGrpc.getPullConfigMethod()
                        .getFullMethodName(),
                request,
                timestamp,
                nonce
        );
    }

    private Metadata signed(
            String fullMethodName,
            Message request,
            long timestamp,
            String nonce) {
        DdcRpcCanonicalRequest canonical = new DdcRpcCanonicalRequest(
                fullMethodName, timestamp, nonce, request);
        Metadata headers = new Metadata();
        headers.put(DdcRpcMetadataKeys.ACCESS_KEY, ACCESS_KEY);
        headers.put(DdcRpcMetadataKeys.TIMESTAMP, Long.toString(timestamp));
        headers.put(DdcRpcMetadataKeys.NONCE, nonce);
        headers.put(DdcRpcMetadataKeys.CONTENT_SHA256,
                canonical.contentSha256());
        headers.put(DdcRpcMetadataKeys.SIGNATURE,
                signer.sign(canonical, SECRET));
        headers.put(DdcRpcMetadataKeys.CONTRACT_VERSION,
                DdcRpcCanonicalRequest.CONTRACT_VERSION);
        return headers;
    }

    private DdcAdminProperties properties(
            List<DdcAdminProperties.Credential> credentials) {
        DdcAdminProperties properties = new DdcAdminProperties();
        properties.getRpc().setSignatureEnabled(true);
        properties.getRpc().setAllowedClockSkewSeconds(300);
        properties.getRpc().setCredentials(credentials);
        return properties;
    }

    private DdcAdminProperties.Credential credential(
            List<String> operations,
            List<String> apps) {
        DdcAdminProperties.Credential credential =
                new DdcAdminProperties.Credential();
        credential.setCredentialId("sdk-a");
        credential.setAccessKey(ACCESS_KEY);
        credential.setSecret(SECRET);
        credential.setClientType("SDK");
        credential.setBizCodePatterns(List.of("biz-a"));
        credential.setEnvPatterns(List.of("dev"));
        credential.setAppCodePatterns(apps);
        credential.setAllowedOperations(operations);
        return credential;
    }

    private PullConfigRequest request(
            String bizCode,
            String env,
            String appCode) {
        return PullConfigRequest.newBuilder()
                .setScope(scope(bizCode, env, appCode))
                .build();
    }

    private DdcScope scope(String bizCode, String env, String appCode) {
        return DdcScope.newBuilder()
                .setBizCode(bizCode)
                .setEnv(env)
                .setAppCode(appCode)
                .build();
    }

    private static final class RecordingServerCall<ReqT, RespT>
            extends ServerCall<ReqT, RespT> {

        private final MethodDescriptor<ReqT, RespT> method;
        private Status status;
        private Metadata trailers;

        private RecordingServerCall(MethodDescriptor<ReqT, RespT> method) {
            this.method = method;
        }

        @Override
        public void request(int numMessages) {
        }

        @Override
        public void sendHeaders(Metadata headers) {
        }

        @Override
        public void sendMessage(RespT message) {
        }

        @Override
        public void close(Status status, Metadata trailers) {
            this.status = status;
            this.trailers = trailers;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public MethodDescriptor<ReqT, RespT> getMethodDescriptor() {
            return method;
        }

        @Override
        public Attributes getAttributes() {
            return Attributes.EMPTY;
        }

        private Status status() {
            return status;
        }

        private Metadata trailers() {
            return trailers;
        }
    }
}
