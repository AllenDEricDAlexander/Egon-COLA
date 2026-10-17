package top.egon.cola.component.tianshu.admin.service.ai.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import top.egon.cola.component.tianshu.admin.config.DdcAiOptimizationProperties;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiModelRequestDTO;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiModelResultDTO;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiRunErrorCodeEnum;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext;
import top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceOAuth2Client;
import top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceTokenRequest;

import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class YuhengLlmModelAdapterTest {

    private static final String URL = "http://yuheng.test/v1/chat/completions";

    private final IdpServiceOAuth2Client oauth2Client = mock(IdpServiceOAuth2Client.class);
    private MockRestServiceServer server;
    private YuhengLlmModelAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://yuheng.test");
        server = MockRestServiceServer.bindTo(builder).build();
        DdcAiOptimizationProperties.Model model = new DdcAiOptimizationProperties.Model()
                .setRegistrationId("tianshu-ai").setAppId("tianshu-admin")
                .setResourceUri("https://yuheng.test/llm").setScopes(List.of("yuheng:llm:invoke"));
        adapter = new YuhengLlmModelAdapter(builder.build(), oauth2Client, new ObjectMapper(), model);
        when(oauth2Client.authorize(any())).thenReturn(new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                "tkn", Instant.parse("2026-10-17T00:00:00Z"), Instant.parse("2026-10-17T01:00:00Z")));
    }

    @Test
    void completesAndParsesTheChatCompletion() {
        server.expect(requestTo(URL)).andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer tkn"))
                .andExpect(jsonPath("$.model").value("qwen-plus"))
                .andExpect(jsonPath("$.stream").value(false))
                .andExpect(jsonPath("$.max_tokens").value(512))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].content").value("user prompt"))
                .andRespond(withSuccess("""
                        {"model":"qwen-plus","choices":[{"message":{"content":"{\\"score\\":77}"},"finish_reason":"stop"}],
                         "usage":{"prompt_tokens":10,"completion_tokens":5}}""", MediaType.APPLICATION_JSON));

        DdcAiModelResultDTO result = adapter.complete(request());

        assertThat(result.text()).isEqualTo("{\"score\":77}");
        assertThat(result.promptTokens()).isEqualTo(10);
        assertThat(result.completionTokens()).isEqualTo(5);
        assertThat(result.truncated()).isFalse();
        server.verify();
    }

    @Test
    void tokenRequestUsesThePlatformContextAndConfiguredScopes() {
        server.expect(requestTo(URL)).andRespond(withSuccess(
                "{\"choices\":[{\"message\":{\"content\":\"ok\"},\"finish_reason\":\"length\"}]}", MediaType.APPLICATION_JSON));

        assertThat(adapter.complete(request()).truncated()).isTrue();

        ArgumentCaptor<IdpServiceTokenRequest> token = ArgumentCaptor.forClass(IdpServiceTokenRequest.class);
        verify(oauth2Client).authorize(token.capture());
        assertThat(token.getValue().registrationId()).isEqualTo("tianshu-ai");
        assertThat(token.getValue().appId()).isEqualTo("tianshu-admin");
        assertThat(token.getValue().audience()).isEqualTo(URI.create("https://yuheng.test/llm"));
        assertThat(token.getValue().context()).isEqualTo(ServiceTokenContext.PLATFORM);
        assertThat(token.getValue().scopes()).containsExactly("yuheng:llm:invoke");
    }

    @Test
    void unauthorizedInvalidatesTheCachedTokenAndMapsToModelAuth() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> adapter.complete(request()))
                .isInstanceOfSatisfying(DdcAiModelException.class,
                        failure -> assertThat(failure.getCode()).isEqualTo(DdcAiRunErrorCodeEnum.MODEL_AUTH));
        verify(oauth2Client).invalidate(any(IdpServiceTokenRequest.class));
    }

    @Test
    void tokenFailureMapsToModelAuthWithoutCallingTheGateway() {
        when(oauth2Client.authorize(any())).thenThrow(new IllegalStateException("idp down"));

        assertThatThrownBy(() -> adapter.complete(request()))
                .isInstanceOfSatisfying(DdcAiModelException.class,
                        failure -> assertThat(failure.getCode()).isEqualTo(DdcAiRunErrorCodeEnum.MODEL_AUTH));
        server.verify();
        verify(oauth2Client, never()).invalidate(any());
    }

    @Test
    void rateLimitTimeoutServerErrorAndNonJsonAreClassified() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        assertCode(DdcAiRunErrorCodeEnum.MODEL_RATE_LIMITED);
        server.reset();

        server.expect(requestTo(URL)).andRespond(withException(new HttpTimeoutException("read timed out")));
        assertCode(DdcAiRunErrorCodeEnum.MODEL_TIMEOUT);
        server.reset();

        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        assertCode(DdcAiRunErrorCodeEnum.MODEL_ERROR);
        server.reset();

        server.expect(requestTo(URL)).andRespond(withSuccess("<html>oops</html>", MediaType.TEXT_HTML));
        assertCode(DdcAiRunErrorCodeEnum.MODEL_ERROR);
        server.reset();

        server.expect(requestTo(URL)).andRespond(withSuccess("{\"choices\":[]}", MediaType.APPLICATION_JSON));
        assertCode(DdcAiRunErrorCodeEnum.MODEL_ERROR);
    }

    @Test
    void failureMessagesNeverCarryTheTokenOrPrompt() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> adapter.complete(request()))
                .hasMessageNotContaining("tkn").hasMessageNotContaining("user prompt");
    }

    private void assertCode(DdcAiRunErrorCodeEnum expected) {
        assertThatThrownBy(() -> adapter.complete(request()))
                .isInstanceOfSatisfying(DdcAiModelException.class,
                        failure -> assertThat(failure.getCode()).isEqualTo(expected));
    }

    private static DdcAiModelRequestDTO request() {
        return new DdcAiModelRequestDTO("qwen-plus", "system prompt", "user prompt", 512, 0.0d, "JUDGE");
    }
}
