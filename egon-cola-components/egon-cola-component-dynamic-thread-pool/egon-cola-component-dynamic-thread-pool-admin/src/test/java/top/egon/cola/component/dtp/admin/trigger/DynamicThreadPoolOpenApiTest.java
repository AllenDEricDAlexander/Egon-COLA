package top.egon.cola.component.dtp.admin.trigger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.component.dtp.admin.trigger.converter.ResizeExecutorConverterImpl;
import top.egon.cola.component.dtp.config.DynamicThreadPoolAutoConfig;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = DynamicThreadPoolOpenApiTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {"springdoc.api-docs.enabled=true", "springdoc.api-docs.version=openapi_3_0",
                "spring.main.banner-mode=off", "logging.config=classpath:logback-dtp-test.xml"})
@AutoConfigureMockMvc
class DynamicThreadPoolOpenApiTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;

    @Test
    void generatedContractMatchesTheExistingEnvelopeAndOptionalQueueCapacity() throws Exception {
        JsonNode document = mapper.readTree(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        JsonNode operation = document.path("paths")
                .path("/api/v1/dtp/apps/{appName}/instances/{instanceId}/executors/{executorName}/resize").path("post");
        assertThat(operation.path("operationId").asText()).isEqualTo("resizeDtpExecutor");
        assertThat(operation.path("parameters")).hasSize(3);
        assertThat(operation.path("responses").has("200")).isTrue();
        assertThat(operation.path("responses").has("400")).isTrue();
        JsonNode input = document.path("components").path("schemas").path("ResizeExecutorRequest");
        assertThat(input.path("properties").path("queueCapacity").path("nullable").asBoolean()).isTrue();
        assertThat(input.path("properties").path("queueCapacity").path("minimum").asInt()).isEqualTo(1);
        assertThat(input.path("required").toString()).doesNotContain("queueCapacity");
        String responseRef = operation.path("responses").path("200").path("content").path("*/*").path("schema").path("$ref").asText();
        assertThat(responseRef).startsWith("#/components/schemas/");
        JsonNode envelope = document.at(responseRef.substring(1));
        assertThat(envelope.path("properties").has("code")).isTrue();
        assertThat(envelope.path("properties").has("info")).isTrue();
        assertThat(envelope.path("properties").has("traceId")).isTrue();
        assertThat(envelope.path("properties").path("data").path("type").asText()).isEqualTo("boolean");
    }

    @Test
    void oldRequestKeepsItsWireShapeAndNewInvalidCapacityUsesTheOriginalErrorEnvelope() throws Exception {
        String route = "/api/v1/dtp/apps/app/instances/one/executors/pool/resize";
        String oldBody = "{\"executorKind\":\"PLATFORM_THREAD_POOL\",\"corePoolSize\":2,\"maximumPoolSize\":4}";
        mvc.perform(post(route).contentType("application/json").content(oldBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data").value(true)).andExpect(jsonPath("$.info").value("调用成功"));
        String invalid = oldBody.substring(0, oldBody.length() - 1) + ",\"queueCapacity\":0}";
        mvc.perform(post(route).contentType("application/json").content(invalid))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("0002"))
                .andExpect(jsonPath("$.data").value(false));
        mvc.perform(post(route).contentType("application/json").content("{")) .andExpect(status().isBadRequest());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = DynamicThreadPoolAutoConfig.class)
    @Import({DynamicThreadPoolController.class, ResizeExecutorConverterImpl.class})
    static class TestApplication {
        @Bean("redissonClient")
        RedissonClient redis() {
            var redis = mock(RedissonClient.class);
            when(redis.getTopic(anyString())).thenReturn(mock(RTopic.class));
            return redis;
        }
    }
}
