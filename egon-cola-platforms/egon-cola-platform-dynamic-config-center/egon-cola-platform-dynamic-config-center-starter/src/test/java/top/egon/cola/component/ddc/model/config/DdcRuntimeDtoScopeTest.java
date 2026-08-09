package top.egon.cola.component.ddc.model.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DdcRuntimeDtoScopeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void runtimePayloadsUsePhysicalBizEnvAppScope() {
        DdcInstanceRegisterRequest register = new DdcInstanceRegisterRequest();
        register.setBizCode("retail");
        register.setEnv("dev");
        register.setAppCode("order");
        register.setNamespace("namespace-a");

        DdcHeartbeatRequest heartbeat = new DdcHeartbeatRequest();
        heartbeat.setBizCode("retail");
        heartbeat.setEnv("dev");
        heartbeat.setAppCode("order");
        heartbeat.setNamespace("namespace-a");

        DdcAckRequest ack = new DdcAckRequest();
        ack.setBizCode("retail");
        ack.setEnv("dev");
        ack.setAppCode("order");
        ack.setNamespace("namespace-a");

        DdcPublishMessage publish = new DdcPublishMessage();
        publish.setBizCode("retail");
        publish.setEnv("dev");
        publish.setAppCode("order");
        publish.setNamespace("namespace-a");

        for (Object payload : List.of(register, heartbeat, ack, publish)) {
            JsonNode json = objectMapper.valueToTree(payload);
            assertThat(json.path("bizCode").asText()).isEqualTo("retail");
            assertThat(json.path("env").asText()).isEqualTo("dev");
            assertThat(json.path("appCode").asText()).isEqualTo("order");
            assertThat(json.has("namespace")).isFalse();
        }
    }
}
