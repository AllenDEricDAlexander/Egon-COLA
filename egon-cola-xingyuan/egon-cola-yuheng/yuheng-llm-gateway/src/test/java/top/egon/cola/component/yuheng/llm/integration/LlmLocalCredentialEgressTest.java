package top.egon.cola.component.yuheng.llm.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.yuheng.llm.config.LlmGatewayProperties;
import top.egon.cola.component.yuheng.llm.proxy.domain.bo.LlmModelSnapshotBO;
import top.egon.cola.component.yuheng.llm.proxy.domain.dto.LlmInvocationCommandDTO;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmCapabilityEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmDeploymentEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmProtocolEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.exception.LlmInvocationException;
import top.egon.cola.component.yuheng.llm.proxy.domain.vo.LlmInvocationResultVO;
import top.egon.cola.component.yuheng.llm.proxy.service.AnthropicMessagesProtocolStrategy;
import top.egon.cola.component.yuheng.llm.proxy.service.LlmProtocolStrategy;
import top.egon.cola.component.yuheng.llm.proxy.service.OpenAiChatProtocolStrategy;
import top.egon.cola.component.yuheng.llm.proxy.service.OpenAiEmbeddingProtocolStrategy;
import top.egon.cola.component.yuheng.llm.proxy.service.OpenAiResponsesProtocolStrategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmLocalCredentialEgressTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    @DisplayName("All four local protocol adapters send unauthenticated LOCAL requests without synthesizing credential headers")
    void localChannelsWithoutSecretReferencesOmitCredentialHeaders() throws Exception {
        Map<String, Headers> received = new ConcurrentHashMap<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", request -> {
            received.put(request.getRequestURI().getPath(), request.getRequestHeaders());
            byte[] response = "{\"error\":{\"message\":\"test upstream auth response\"}}"
                    .getBytes(StandardCharsets.UTF_8);
            request.sendResponseHeaders(401, response.length);
            try (var body = request.getResponseBody()) {
                body.write(response);
            }
        });
        server.start();
        try {
            String origin = "http://127.0.0.1:" + server.getAddress().getPort();
            LlmGatewayProperties properties = new LlmGatewayProperties()
                    .setEnabled(true)
                    .setAllowedLocalCidrs(List.of("127.0.0.1/32"))
                    .setAllowedCloudHosts(List.of())
                    .setSecretsRoot(null)
                    .setMaxRequestBytes(1_048_576L)
                    .setMaxFrameBytes(65_536)
                    .setMaximumAttempts(2)
                    .setStreamingThreads(4);
            List<LlmProtocolStrategy> strategies = List.of(
                    new OpenAiChatProtocolStrategy(properties, JSON),
                    new OpenAiEmbeddingProtocolStrategy(properties, JSON),
                    new OpenAiResponsesProtocolStrategy(properties, JSON),
                    new AnthropicMessagesProtocolStrategy(properties, JSON));
            for (LlmProtocolStrategy strategy : strategies) {
                LlmProtocolEnum protocol = strategy.protocol();
                String alias = alias(protocol);
                String baseUrl = protocol == LlmProtocolEnum.ANTHROPIC_MESSAGES ? origin : origin + "/v1";
                LlmModelSnapshotBO.RouteBO route = route(protocol, alias, baseUrl);
                LlmInvocationCommandDTO command = command(protocol, alias, payload(protocol, alias));
                strategy.exchange(command, route, new LlmInvocationResultVO()
                        .setStatus(200)
                        .setHeaders(Map.of("Cache-Control", "no-store"))
                        .setCandidates(List.of(route)));
            }

            for (LlmProtocolStrategy strategy : strategies) {
                LlmProtocolEnum protocol = strategy.protocol();
                String alias = alias(protocol);
                LlmModelSnapshotBO.RouteBO cloudRoute = route(protocol, alias, "https://cloud.invalid/v1");
                cloudRoute.getChannel().setDeployment(LlmDeploymentEnum.CLOUD);
                LlmInvocationCommandDTO command = command(protocol, alias, payload(protocol, alias));
                assertThatThrownBy(() -> strategy.exchange(command, cloudRoute,
                        new LlmInvocationResultVO().setStatus(200)
                                .setHeaders(Map.of("Cache-Control", "no-store"))
                                .setCandidates(List.of(cloudRoute))))
                        .as("a cloud channel without a configured credential fails before network egress")
                        .isInstanceOf(LlmInvocationException.class);
            }

            assertThat(received.keySet()).containsExactlyInAnyOrder(
                    "/v1/chat/completions", "/v1/embeddings", "/v1/responses", "/v1/messages");
            for (Headers headers : received.values()) {
                assertThat(headers.getFirst("Authorization")).isNull();
                assertThat(headers.getFirst("x-api-key")).isNull();
            }
        } finally {
            server.stop(0);
        }
    }

    private static String alias(LlmProtocolEnum protocol) {
        return switch (protocol) {
            case OPENAI_CHAT -> "company-chat";
            case OPENAI_EMBEDDING -> "company-embed-v1";
            case OPENAI_RESPONSES -> "company-responses";
            case ANTHROPIC_MESSAGES -> "company-messages";
        };
    }

    private static ObjectNode payload(LlmProtocolEnum protocol, String alias) throws Exception {
        return switch (protocol) {
            case OPENAI_CHAT -> (ObjectNode) JSON.readTree("""
                    {"model":"%s","messages":[{"role":"user","content":"hello"}],"max_tokens":16}
                    """.formatted(alias));
            case OPENAI_EMBEDDING -> (ObjectNode) JSON.readTree("""
                    {"model":"%s","input":["hello"]}
                    """.formatted(alias));
            case OPENAI_RESPONSES -> (ObjectNode) JSON.readTree("""
                    {"model":"%s","input":"hello","max_output_tokens":16}
                    """.formatted(alias));
            case ANTHROPIC_MESSAGES -> (ObjectNode) JSON.readTree("""
                    {"model":"%s","max_tokens":16,"messages":[{"role":"user","content":"hello"}]}
                    """.formatted(alias));
        };
    }

    private static LlmInvocationCommandDTO command(
            LlmProtocolEnum protocol, String alias, ObjectNode payload) {
        return new LlmInvocationCommandDTO()
                .setProtocol(protocol)
                .setModel(alias)
                .setStream(Boolean.FALSE)
                .setPayload(payload)
                .setCallerSubject("svc:local-test")
                .setAllowedDeployments(Set.of(LlmDeploymentEnum.LOCAL))
                .setRequiredCapabilities(Set.of(LlmCapabilityEnum.TEXT));
    }

    private static LlmModelSnapshotBO.RouteBO route(
            LlmProtocolEnum protocol, String alias, String baseUrl) {
        LlmModelSnapshotBO.ChannelBO channel = new LlmModelSnapshotBO.ChannelBO()
                .setChannelKey("local-no-auth")
                .setDeployment(LlmDeploymentEnum.LOCAL)
                .setProtocol(protocol)
                .setBaseUrl(baseUrl)
                .setSecretRef(null)
                .setEnabled(Boolean.TRUE)
                .setConnectTimeoutMs(2_000)
                .setHeaderTimeoutMs(5_000)
                .setIdleTimeoutMs(5_000)
                .setTotalTimeoutMs(5_000)
                .setMaxConcurrent(4);
        return new LlmModelSnapshotBO.RouteBO()
                .setChannelKey(channel.getChannelKey())
                .setUpstreamModel(alias + "-upstream")
                .setPriority(1)
                .setWeight(100)
                .setCapabilities(Set.of(LlmCapabilityEnum.TEXT))
                .setChannel(channel);
    }
}
