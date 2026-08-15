package top.egon.cola.component.gateway.engine.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.DescriptorProtos;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.ServerCalls;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.rule.GatewayRequestBodyMode;
import top.egon.cola.component.gateway.contract.rule.GatewayRouteProfile;
import top.egon.cola.component.gateway.contract.rule.GatewayRpcDescriptor;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleContent;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleSnapshot;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportResponseMode;
import top.egon.cola.component.gateway.core.http.HttpRequestNormalizer;
import top.egon.cola.component.gateway.core.provider.ProviderHealthState;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.provider.ProviderRegistryState;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.core.route.GatewayResponseMode;
import top.egon.cola.component.gateway.core.route.HttpRouteCompiler;
import top.egon.cola.component.gateway.core.route.RuntimeHttpRoute;
import top.egon.cola.component.gateway.core.security.GatewayCredential;
import top.egon.cola.component.gateway.core.security.TrustedIdentity;
import top.egon.cola.component.gateway.core.transport.EffectiveGatewayTransportPolicy;
import top.egon.cola.component.gateway.engine.balance.ProviderSelectionHandle;
import top.egon.cola.component.gateway.engine.discovery.ProviderCallOutcomeRecorder;
import top.egon.cola.component.gateway.engine.observability.GatewayCallCompletionListener;
import top.egon.cola.component.gateway.engine.rpc.HttpRpcUpstreamAdapter;
import top.egon.cola.component.gateway.engine.rpc.RawByteMarshaller;
import top.egon.cola.component.gateway.engine.rpc.RpcMethodIndex;
import top.egon.cola.component.gateway.engine.rpc.RpcProviderChannelCache;
import top.egon.cola.component.gateway.engine.rule.CompiledGatewayRules;
import top.egon.cola.component.gateway.engine.rule.GatewayRuleJsonCodec;
import top.egon.cola.component.gateway.engine.traffic.GatewayTrafficGovernance;
import top.egon.cola.component.rpc.context.invocation.RpcMetadataKeys;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DefaultGatewayHttpDataPlaneHandlerCredentialForwardingTest {

    private static final String TOKEN = "exact.header.payload.signature";

    @Test
    void restoresVerifiedBearerForRpcUpstreamWhenPolicyAllows()
            throws Exception {
        String authorization = invoke(
                new GatewayHttpSecurityProcessor.Outcome(
                        TrustedIdentity.empty(),
                        Set.of("authorization"),
                        new GatewayCredential("bearer", TOKEN, Map.of())
                ),
                true
        );

        assertEquals("Bearer " + TOKEN, authorization);
    }

    @Test
    void neverRestoresUnverifiedInboundBearerForRpc() throws Exception {
        String authorization = invoke(
                new GatewayHttpSecurityProcessor.Outcome(
                        TrustedIdentity.empty(),
                        Set.of("authorization"),
                        null
                ),
                true
        );

        assertNull(authorization);
    }

    @Test
    void policyCanForbidVerifiedBearerForwardingToRpc() throws Exception {
        String authorization = invoke(
                new GatewayHttpSecurityProcessor.Outcome(
                        TrustedIdentity.empty(),
                        Set.of("authorization"),
                        new GatewayCredential("bearer", TOKEN, Map.of())
                ),
                false
        );

        assertNull(authorization);
    }

    private String invoke(
            GatewayHttpSecurityProcessor.Outcome security,
            boolean authorizationForwardingAllowed
    ) throws Exception {
        var method = RawByteMarshaller.INSTANCE.descriptor(
                "test.EchoService/Echo"
        );
        AtomicReference<String> authorization = new AtomicReference<>();
        Server server = io.grpc.ServerBuilder.forPort(0)
                .addService(ServerInterceptors.intercept(
                        ServerServiceDefinition
                                .builder("test.EchoService")
                                .addMethod(
                                        method,
                                        ServerCalls.asyncUnaryCall(
                                                (request, observer) -> {
                                                    observer.onNext(request);
                                                    observer.onCompleted();
                                                }
                                        )
                                )
                                .build(),
                        new ServerInterceptor() {
                            @Override
                            public <RequestT, ResponseT>
                                    ServerCall.Listener<RequestT> interceptCall(
                                            ServerCall<RequestT, ResponseT> call,
                                            Metadata headers,
                                            ServerCallHandler<RequestT, ResponseT> next
                                    ) {
                                authorization.set(headers.get(
                                        RpcMetadataKeys.AUTHORIZATION
                                ));
                                return next.startCall(call, headers);
                            }
                        }
                ))
                .build()
                .start();
        RpcProviderChannelCache channels =
                new RpcProviderChannelCache(Duration.ofSeconds(1));
        try {
            byte[] descriptor = descriptorSet();
            String sha = GatewayRuleJsonCodec.sha256(descriptor);
            ProviderServiceKey service = serviceKey();
            RuntimeHttpRoute route = route(
                    service,
                    sha,
                    authorizationForwardingAllowed
            );
            CompiledGatewayRules rules = rules(route, descriptor, sha);
            HttpRpcUpstreamAdapter rpc = new HttpRpcUpstreamAdapter(
                    () -> rules,
                    channels,
                    new ObjectMapper()
            );
            DefaultGatewayHttpDataPlaneHandler handler =
                    new DefaultGatewayHttpDataPlaneHandler(
                            new HttpRequestNormalizer(32, 8192),
                            rules::httpRoutes,
                            ignored -> new ProviderSelectionHandle(
                                    provider(service, server.getPort()),
                                    () -> {
                                    }
                            ),
                            request -> Mono.error(new AssertionError(
                                    "HTTP adapter must not be used"
                            )),
                            1024,
                            Duration.ofSeconds(2),
                            (zone, request, normalized, match, traceId) ->
                                    Mono.just(security),
                            GatewayCallCompletionListener.noop(),
                            "engine-1",
                            GatewayTrafficGovernance.noop(),
                            rpc,
                            ProviderCallOutcomeRecorder.noop()
                    );
            GatewayOutboundHttpResponse response = handler.handle(
                    AccessZone.INTERNAL,
                    new GatewayInboundHttpRequest(
                            "POST",
                            "api.test.local",
                            "/api/echo",
                            Map.of(
                                    "content-type",
                                    List.of("application/json"),
                                    "authorization",
                                    List.of("Bearer forged-inbound-token")
                            ),
                            new InetSocketAddress("127.0.0.1", 12345),
                            Flux.just(DefaultDataBufferFactory.sharedInstance
                                    .wrap("{\"value\":\"hello\"}".getBytes(
                                            StandardCharsets.UTF_8
                                    )))
                    )
            ).block();
            GatewayDataBufferTestSupport.joinUtf8(response.body(), 1024);
            return authorization.get();
        } finally {
            channels.close();
            server.shutdownNow().awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    private RuntimeHttpRoute route(
            ProviderServiceKey service,
            String sha,
            boolean authorizationForwardingAllowed
    ) {
        return new RuntimeHttpRoute(
                "echo-route",
                "echo-operation",
                "group",
                Set.of(AccessZone.INTERNAL),
                "api.test.local",
                Set.of("POST"),
                "/api/echo",
                true,
                service,
                Set.of(),
                0,
                GatewayResponseMode.TRANSPARENT,
                Map.of(
                        "methodIdentity", "test.EchoService/Echo",
                        "requestSchema", "test.EchoMessage",
                        "responseSchema", "test.EchoMessage",
                        "descriptorSha256", sha
                ),
                new EffectiveGatewayTransportPolicy(
                        GatewayRouteProfile.DEFAULT,
                        GatewayTransportProtocol.HTTP,
                        GatewayRequestBodyMode.AGGREGATED,
                        GatewayTransportResponseMode.STANDARD,
                        1024,
                        OptionalLong.of(1024),
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(1),
                        Optional.of(Duration.ofSeconds(2)),
                        Optional.empty(),
                        OptionalLong.empty(),
                        false,
                        false,
                        authorizationForwardingAllowed
                )
        );
    }

    private CompiledGatewayRules rules(
            RuntimeHttpRoute route,
            byte[] descriptor,
            String sha
    ) {
        GatewayRuleContent content = new GatewayRuleContent(
                "group",
                "group",
                "test",
                "default",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new GatewayRpcDescriptor(
                        "echo",
                        sha,
                        Base64.getEncoder().encodeToString(descriptor)
                ))
        );
        return new CompiledGatewayRules(
                new GatewayRuleSnapshot(
                        "v1",
                        "release",
                        Instant.EPOCH,
                        "content",
                        "artifact",
                        content
                ),
                new HttpRouteCompiler().compile(List.of(route)),
                RpcMethodIndex.empty(),
                Set.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of()
        );
    }

    private ProviderInstance provider(
            ProviderServiceKey service,
            int port
    ) {
        return new ProviderInstance(
                service,
                "provider-1",
                "lease-1",
                "127.0.0.1",
                port,
                false,
                Map.of(),
                Instant.now().plusSeconds(60),
                ProviderRegistryState.REGISTERED,
                ProviderHealthState.HEALTHY,
                ProviderHealthState.HEALTHY
        );
    }

    private ProviderServiceKey serviceKey() {
        return new ProviderServiceKey(
                "test-biz",
                "test-app",
                "test",
                "default",
                ProviderProtocolType.RPC,
                "echo",
                "default",
                "1.0.0",
                "grpc"
        );
    }

    private byte[] descriptorSet() {
        DescriptorProtos.DescriptorProto message =
                DescriptorProtos.DescriptorProto.newBuilder()
                        .setName("EchoMessage")
                        .addField(
                                DescriptorProtos.FieldDescriptorProto
                                        .newBuilder()
                                        .setName("value")
                                        .setNumber(1)
                                        .setType(
                                                DescriptorProtos
                                                        .FieldDescriptorProto
                                                        .Type.TYPE_STRING
                                        )
                        )
                        .build();
        DescriptorProtos.FileDescriptorProto file =
                DescriptorProtos.FileDescriptorProto.newBuilder()
                        .setName("echo.proto")
                        .setPackage("test")
                        .setSyntax("proto3")
                        .addMessageType(message)
                        .build();
        return DescriptorProtos.FileDescriptorSet.newBuilder()
                .addFile(file)
                .build()
                .toByteArray();
    }
}
