package top.egon.cola.component.tianshu.admin.service.ai.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import top.egon.cola.component.tianshu.admin.config.DdcAiOptimizationProperties;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiModelRequestDTO;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiModelResultDTO;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiRunErrorCodeEnum;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiModelPort;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext;
import top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceOAuth2Client;
import top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceTokenRequest;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.util.LinkedHashSet;
import java.util.Objects;

/**
 * 玉衡 LLM 网关的 OpenAI 兼容补全适配器；以 SERVICE 令牌认证，不重试。
 * / OpenAI-compatible completion adapter for the Yuheng LLM gateway; authenticates with a SERVICE token and never
 * retries.
 */
@Slf4j
public class YuhengLlmModelAdapter implements DdcAiModelPort {

    static final String COMPLETIONS_PATH = "/v1/chat/completions";

    private final RestClient client;
    private final IdpServiceOAuth2Client oauth2Client;
    private final ObjectMapper objectMapper;
    private final DdcAiOptimizationProperties.Model model;

    public YuhengLlmModelAdapter(RestClient client, IdpServiceOAuth2Client oauth2Client, ObjectMapper objectMapper,
            DdcAiOptimizationProperties.Model model) {
        this.client = Objects.requireNonNull(client, "client");
        this.oauth2Client = Objects.requireNonNull(oauth2Client, "oauth2Client");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.model = Objects.requireNonNull(model, "model");
    }

    @Override
    public DdcAiModelResultDTO complete(DdcAiModelRequestDTO request) {
        IdpServiceTokenRequest tokenRequest = tokenRequest();
        String token;
        try {
            token = oauth2Client.authorize(tokenRequest).getTokenValue();
        } catch (RuntimeException failure) {
            throw new DdcAiModelException(DdcAiRunErrorCodeEnum.MODEL_AUTH, 0, "service token unavailable", failure);
        }
        long started = System.nanoTime();
        int status = 0;
        try {
            String raw = client.post()
                    .uri(COMPLETIONS_PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body(request))
                    .retrieve()
                    .body(String.class);
            status = 200;
            return parse(raw);
        } catch (RestClientResponseException http) {
            status = http.getStatusCode().value();
            if (status == 401 || status == 403) oauth2Client.invalidate(tokenRequest);
            throw new DdcAiModelException(classify(status), status, "model gateway returned HTTP " + status, http);
        } catch (ResourceAccessException io) {
            throw new DdcAiModelException(timeout(io) ? DdcAiRunErrorCodeEnum.MODEL_TIMEOUT
                    : DdcAiRunErrorCodeEnum.MODEL_ERROR, 0, "model gateway unreachable", io);
        } finally {
            log.info("AI_MODEL_CALL alias={} purpose={} status={} latencyMs={} promptChars={}",
                    request.modelAlias(), request.purpose(), status, (System.nanoTime() - started) / 1_000_000L,
                    request.userPrompt().length());
        }
    }

    private String body(DdcAiModelRequestDTO request) {
        ObjectNode body = objectMapper.createObjectNode()
                .put("model", request.modelAlias())
                .put("temperature", request.temperature())
                .put("max_tokens", request.maxOutputTokens())
                .put("stream", false);
        body.putArray("messages")
                .add(objectMapper.createObjectNode().put("role", "system").put("content", request.systemPrompt()))
                .add(objectMapper.createObjectNode().put("role", "user").put("content", request.userPrompt()));
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException failure) {
            throw new DdcAiModelException(DdcAiRunErrorCodeEnum.MODEL_ERROR, 0, "model request not serializable", failure);
        }
    }

    private DdcAiModelResultDTO parse(String raw) {
        JsonNode root;
        try {
            root = raw == null ? null : objectMapper.readTree(raw);
        } catch (JsonProcessingException failure) {
            throw new DdcAiModelException(DdcAiRunErrorCodeEnum.MODEL_ERROR, 200, "model response is not JSON", failure);
        }
        JsonNode choice = root == null ? null : root.path("choices").path(0);
        String text = choice == null || choice.isMissingNode() ? null : choice.path("message").path("content").asText(null);
        if (text == null || text.isBlank()) {
            throw new DdcAiModelException(DdcAiRunErrorCodeEnum.MODEL_ERROR, 200, "model response has no content", null);
        }
        return new DdcAiModelResultDTO(text, intOrNull(root.at("/usage/prompt_tokens")),
                intOrNull(root.at("/usage/completion_tokens")), root.path("model").asText(null),
                choice.path("finish_reason").asText(null));
    }

    private IdpServiceTokenRequest tokenRequest() {
        try {
            return new IdpServiceTokenRequest(model.getRegistrationId(), model.getAppId(),
                    URI.create(model.getResourceUri()), ServiceTokenContext.PLATFORM, null,
                    new LinkedHashSet<>(model.getScopes()));
        } catch (RuntimeException invalid) {
            throw new DdcAiModelException(DdcAiRunErrorCodeEnum.MODEL_AUTH, 0, "model token request is misconfigured",
                    invalid);
        }
    }

    static DdcAiRunErrorCodeEnum classify(int status) {
        return switch (status) {
            case 401, 403 -> DdcAiRunErrorCodeEnum.MODEL_AUTH;
            case 408, 504 -> DdcAiRunErrorCodeEnum.MODEL_TIMEOUT;
            case 429 -> DdcAiRunErrorCodeEnum.MODEL_RATE_LIMITED;
            default -> DdcAiRunErrorCodeEnum.MODEL_ERROR;
        };
    }

    private static boolean timeout(Throwable failure) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current instanceof HttpTimeoutException || current instanceof SocketTimeoutException) return true;
        }
        return false;
    }

    private static Integer intOrNull(JsonNode node) {
        return node == null || node.isMissingNode() || !node.canConvertToInt() ? null : node.intValue();
    }
}
