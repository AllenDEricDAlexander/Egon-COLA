package top.egon.cola.component.yaoguang.admin.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.yaoguang.admin.config.properties.KnowledgeModelClientProperties;
import top.egon.cola.component.yaoguang.admin.knowledge.domain.bo.KnowledgeBaseBO;
import top.egon.cola.component.yaoguang.admin.knowledge.domain.enums.KnowledgeEgressPolicyEnum;
import top.egon.cola.component.yaoguang.admin.knowledge.repository.KnowledgeRepository;
import top.egon.cola.component.yaoguang.admin.knowledge.service.impl.KnowledgeModelClientServiceImpl;

import java.io.ByteArrayInputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeModelClientContractTest {
    @TempDir
    Path directory;

    @Test
    void modelPoliciesUseOnlyTheConfiguredGatewayAndRejectExtraUpstreamFields() throws Exception {
        var http = mock(HttpClient.class);
        var client = client(http, mock(KnowledgeRepository.class));
        var response = response("[{\"modelKey\":\"chat-main\",\"kind\":\"CHAT\",\"protocols\":[\"OPENAI_CHAT\"],"
                + "\"revision\":2,\"dimensions\":null,\"embeddingSpaceId\":null,\"allRoutesEnabled\":true,\"deployments\":[\"LOCAL\"]}]");
        doReturn(response).when(http).send(any(), any());
        assertThat(client.describeModels(List.of("chat-main"))).singleElement()
                .satisfies(policy -> assertThat(policy.getRevision()).isEqualTo(2));
        var request = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(request.capture(), any());
        assertThat(request.getValue().uri().toString()).isEqualTo("https://gateway.invalid/v1/model-policies?model=chat-main");
        assertThat(request.getValue().method()).isEqualTo("GET");
        assertThat(request.getValue().headers().firstValue("Authorization")).isPresent();

        doReturn(response("[{\"modelKey\":\"chat-main\",\"baseUrl\":\"https://provider.invalid\"}]"))
                .when(http).send(any(), any());
        assertThatThrownBy(() -> client.describeModels(List.of("chat-main")))
                .isInstanceOfSatisfying(CommonException.class, error -> assertThat(error.getCode()).isEqualTo(503));
    }

    @Test
    void chatRequestsCarryTheStoredEgressLimitWithoutChangingTheNativeBody() throws Exception {
        var http = mock(HttpClient.class);
        var repository = mock(KnowledgeRepository.class);
        when(repository.findBase("7001")).thenReturn(Optional.of(new KnowledgeBaseBO().setId("7001")
                .setChatModel("chat-main").setEgressPolicy(KnowledgeEgressPolicyEnum.LOCAL_ONLY)));
        var client = client(http, repository);
        doReturn(response("{\"choices\":[{\"message\":{\"content\":\"answer\"}}]}"))
                .when(http).send(any(), any());
        assertThat(client.generate("7001", "question", 32)).isEqualTo("answer");
        var request = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(request.capture(), any());
        assertThat(request.getValue().uri().getPath()).isEqualTo("/v1/chat/completions");
        assertThat(request.getValue().headers().firstValue("X-Egon-Llm-Egress-Policy")).contains("LOCAL_ONLY");
    }

    private KnowledgeModelClientServiceImpl client(HttpClient http, KnowledgeRepository repository) throws Exception {
        Path token = directory.resolve("model-service-token");
        Files.writeString(token, "unit-test-token");
        var properties = new KnowledgeModelClientProperties().setBaseUrl("https://gateway.invalid")
                .setServiceTokenRef(token.toString());
        var client = new KnowledgeModelClientServiceImpl(properties, repository, new ObjectMapper());
        ReflectionTestUtils.setField(client, "httpClient", http);
        return client;
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<java.io.InputStream> response(String body) {
        HttpResponse<java.io.InputStream> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
        return response;
    }
}
