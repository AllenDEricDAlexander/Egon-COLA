package top.egon.cola.component.gateway.engine.rpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.DescriptorProtos;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.ServerCalls;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.rule.GatewayRpcDescriptor;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleContent;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleSnapshot;
import top.egon.cola.component.gateway.core.http.NormalizedHttpRequest;
import top.egon.cola.component.gateway.core.provider.ProviderHealthState;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.provider.ProviderRegistryState;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.core.route.GatewayResponseMode;
import top.egon.cola.component.gateway.core.route.HttpRouteCompiler;
import top.egon.cola.component.gateway.core.route.HttpRouteMatch;
import top.egon.cola.component.gateway.core.route.RuntimeHttpRoute;
import top.egon.cola.component.gateway.engine.rule.CompiledGatewayRules;
import top.egon.cola.component.gateway.engine.rule.GatewayRuleJsonCodec;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpRpcUpstreamAdapterTest {

    @Test
    void invokesRawRpcUsingOnlyRuleDescriptor() throws Exception {
        var method = RawByteMarshaller.INSTANCE.descriptor(
                "test.EchoService/Echo"
        );
        Server server = ServerBuilder.forPort(0)
                .addService(ServerServiceDefinition
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
                        .build())
                .build()
                .start();
        RpcProviderChannelCache channels =
                new RpcProviderChannelCache(Duration.ofSeconds(1));
        try {
            byte[] descriptor = descriptorSet();
            String sha = GatewayRuleJsonCodec.sha256(descriptor);
            CompiledGatewayRules rules = rules(descriptor, sha);
            HttpRpcUpstreamAdapter adapter = new HttpRpcUpstreamAdapter(
                    () -> rules,
                    channels,
                    new ObjectMapper()
            );
            ProviderServiceKey service = new ProviderServiceKey(
                    "test",
                    "default",
                    ProviderProtocolType.RPC,
                    "echo",
                    "default",
                    "1.0.0",
                    "grpc"
            );
            RuntimeHttpRoute route = new RuntimeHttpRoute(
                    "echo-route",
                    "echo-operation",
                    "group",
                    Set.of(AccessZone.PUBLIC),
                    "api.test.local",
                    Set.of("POST"),
                    "/api/echo",
                    true,
                    service,
                    Set.of(),
                    0,
                    GatewayResponseMode.TRANSPARENT,
                    Map.of(
                            "methodIdentity",
                            "test.EchoService/Echo",
                            "requestSchema",
                            "test.EchoMessage",
                            "responseSchema",
                            "test.EchoMessage",
                            "descriptorSha256",
                            sha
                    )
            );
            String response = adapter.invoke(
                            new HttpRouteMatch(route, Map.of()),
                            provider(service, server.getPort()),
                            new NormalizedHttpRequest(
                                    "POST",
                                    "api.test.local",
                                    "/api/echo",
                                    "/api/echo",
                                    "",
                                    Map.of()
                            ),
                            "{\"value\":\"hello\"}".getBytes(
                                    StandardCharsets.UTF_8
                            ),
                            Map.of(),
                            Duration.ofSeconds(2)
                    )
                    .flatMapMany(value -> value.body())
                    .reduce(new byte[0], (left, right) -> {
                        byte[] joined = java.util.Arrays.copyOf(
                                left,
                                left.length + right.length
                        );
                        System.arraycopy(
                                right,
                                0,
                                joined,
                                left.length,
                                right.length
                        );
                        return joined;
                    })
                    .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                    .block();

            assertTrue(response.contains("\"value\": \"hello\""));
        } finally {
            channels.close();
            server.shutdownNow();
            server.awaitTermination();
        }
    }

    private CompiledGatewayRules rules(byte[] descriptor, String sha) {
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
                new HttpRouteCompiler().compile(List.of()),
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
            int port) {
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
