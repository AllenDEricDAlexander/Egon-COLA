package top.egon.cola.component.tianshu.admin.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.pojo.PageResultRecord;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.tianshu.admin.common.DdcAdminException;
import top.egon.cola.component.tianshu.admin.model.bo.DdcPublishTaskBO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcInstanceResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPublishTaskResult;
import top.egon.cola.component.tianshu.error.DdcErrorStatus;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DdcLegacyBehaviorContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void taskAndInstanceWireIdsStayStringsAndHideInternalTenantKeys() throws Exception {
        DdcPublishTaskBO task = DdcPublishTaskBO.builder()
                .id(810001L).tenantId(202L).publicId("810001").changeId("810002")
                .status("PUBLISHING").build();
        JsonNode taskJson = objectMapper.valueToTree(task);
        DdcPublishTaskResult taskResult = DdcPublishTaskResult.builder()
                .publicId("810001").changeId("810002").status("PUBLISHING").build();
        DdcInstanceResult instanceResult = DdcInstanceResult.builder()
                .publicId("910001").instanceId("instance-1").status("ONLINE").build();

        assertThat(taskJson.path("id").isTextual()).isTrue();
        assertThat(taskJson.path("id").asText()).isEqualTo("810001");
        assertThat(taskJson.has("tenantId")).isFalse();
        assertThat(taskJson.has("publicId")).isFalse();
        assertThat(objectMapper.valueToTree(taskResult).path("id").asText()).isEqualTo("810001");
        assertThat(objectMapper.valueToTree(instanceResult).path("id").asText()).isEqualTo("910001");
    }

    @Test
    void pagesKeepTheLegacyEnvelopeAndDateRepresentation() throws Exception {
        DdcPublishTaskResult result = DdcPublishTaskResult.builder()
                .publicId("810001").changeId("810002").status("SUCCESS")
                .createdAt(LocalDateTime.parse("2026-07-25T10:00:00")).build();
        PageResultRecord<DdcPublishTaskResult> page = PageResultRecord.success(List.of(result), 12, 2, 10);
        JsonNode json = objectMapper.valueToTree(page);

        assertThat(json.path("success").asBoolean()).isTrue();
        assertThat(json.path("records").get(0).path("id").asText()).isEqualTo("810001");
        assertThat(json.path("records").get(0).path("createdAt").asText()).isEqualTo("2026-07-25T10:00:00");
        assertThat(json.path("page").path("total").asLong()).isEqualTo(12);
        assertThat(json.path("page").path("pageNo").asInt()).isEqualTo(2);
        assertThat(json.path("page").path("pageSize").asInt()).isEqualTo(10);
        assertThat(json.has("data")).isFalse();
    }

    @Test
    void legacyInvalidRequestErrorCodeAndMessageRemainStable() throws Exception {
        JsonNode json = objectMapper.valueToTree(ResultRecord.failure(new DdcAdminException("config version changed")));

        assertThat(json.path("success").asBoolean()).isFalse();
        assertThat(json.path("code").asInt()).isEqualTo(DdcErrorStatus.INVALID_REQUEST.getCode());
        assertThat(json.path("status").asText()).isEqualTo(DdcErrorStatus.INVALID_REQUEST.getStatus());
        assertThat(json.path("message").asText()).isEqualTo("config version changed");
        assertThat(json.has("data")).isTrue();
        assertThat(json.get("data").isNull()).isTrue();
    }
}
