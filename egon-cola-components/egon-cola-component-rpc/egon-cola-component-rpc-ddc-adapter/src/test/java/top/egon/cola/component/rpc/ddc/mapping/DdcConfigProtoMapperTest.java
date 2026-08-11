package top.egon.cola.component.rpc.ddc.mapping;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.model.config.DdcAckRequest;
import top.egon.cola.component.ddc.model.config.DdcAckStatus;
import top.egon.cola.component.ddc.model.config.DdcConfigValue;
import top.egon.cola.component.ddc.model.config.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.config.DdcInstanceRegisterRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcConfig;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcConfigFormat;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcScope;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcConfigProtoMapperTest {

    private final DdcCommonProtoMapper common =
            new DdcCommonProtoMapper(4 * 1024 * 1024);
    private final DdcConfigProtoMapper mapper =
            new DdcConfigProtoMapper(common, 1024 * 1024);

    @Test
    void roundTripsCompleteRuntimeRequestsAndOptionalFields() {
        DdcInstanceRegisterRequest registration = new DdcInstanceRegisterRequest();
        registration.setInstanceId("instance-1");
        registration.setBizCode("retail");
        registration.setEnv("prod");
        registration.setAppCode("order");
        registration.setHost("10.0.0.1");
        registration.setPort(8080);
        registration.setPid("21");
        registration.setSdkVersion("5.3.3");
        registration.setLeaseSeconds(30);
        registration.setHeartbeatIntervalSeconds(10);
        registration.setMetadata(Map.of("zone", "east"));
        registration.setAdmissionTicket("config-register-ticket");

        DdcInstanceRegisterRequest restored = mapper.fromRegisterRequest(
                mapper.toRegisterRequest(registration));

        assertThat(restored).usingRecursiveComparison()
                .isEqualTo(registration);

        DdcHeartbeatRequest heartbeat = new DdcHeartbeatRequest();
        heartbeat.setInstanceId("instance-1");
        heartbeat.setLeaseId("lease-1");
        heartbeat.setBizCode("retail");
        heartbeat.setEnv("prod");
        heartbeat.setAppCode("order");
        heartbeat.setHost("10.0.0.1");
        heartbeat.setPort(8080);
        heartbeat.setPid("21");
        heartbeat.setSdkVersion("5.3.3");
        heartbeat.setMetadata(Map.of("zone", "east"));
        heartbeat.setAdmissionTicket("config-heartbeat-ticket");

        assertThat(mapper.fromHeartbeatRequest(
                mapper.toHeartbeatRequest(heartbeat)))
                .usingRecursiveComparison()
                .isEqualTo(heartbeat);
        assertThat(mapper.fromOfflineRequest(
                mapper.toOfflineRequest(heartbeat)))
                .extracting(
                        DdcHeartbeatRequest::getInstanceId,
                        DdcHeartbeatRequest::getLeaseId,
                        DdcHeartbeatRequest::getBizCode,
                        DdcHeartbeatRequest::getEnv,
                        DdcHeartbeatRequest::getAppCode
                ).containsExactly(
                        "instance-1", "lease-1", "retail", "prod", "order");

        assertThat(mapper.toRegisterRequest(registration).getAdmissionTicket())
                .isEqualTo("config-register-ticket");
        assertThat(mapper.toHeartbeatRequest(heartbeat).getAdmissionTicket())
                .isEqualTo("config-heartbeat-ticket");
        assertThat(mapper.toOfflineRequest(heartbeat).getAllFields().keySet())
                .noneMatch(field -> field.getName().equals("admission_ticket"));
    }

    @Test
    void roundTripsConfigAndEveryAcknowledgementStatus() {
        DdcConfigValue value = new DdcConfigValue();
        value.setResourceName("application.yml");
        value.setContent("feature:\n  enabled: true\n");
        value.setFormat("YAML");
        value.setVersion(7L);

        assertThat(mapper.fromConfig(mapper.toConfig(value)))
                .usingRecursiveComparison()
                .isEqualTo(value);

        for (DdcAckStatus status : DdcAckStatus.values()) {
            DdcAckRequest ack = new DdcAckRequest();
            ack.setChangeId("change-1");
            ack.setInstanceId("instance-1");
            ack.setLeaseId("lease-1");
            ack.setBizCode("retail");
            ack.setEnv("prod");
            ack.setAppCode("order");
            ack.setResourceName("application.yml");
            ack.setTargetVersion(7L);
            ack.setCurrentVersion(6L);
            ack.setResourceChecksum("abc");
            ack.setStatus(status);
            ack.setErrorMessage("detail");
            ack.setAckTime(1700000000000L);

            assertThat(mapper.fromAcknowledgeRequest(
                    mapper.toAcknowledgeRequest(ack)))
                    .usingRecursiveComparison()
                    .isEqualTo(ack);
        }
    }

    @Test
    void rejectsUnknownEnumsOversizedConfigurationAndMetadata() {
        DdcConfig unknown = DdcConfig.newBuilder()
                .setScope(DdcScope.newBuilder()
                        .setBizCode("retail")
                        .setEnv("prod")
                        .setAppCode("order"))
                .setResourceName("application.yml")
                .setContent("a: b")
                .setFormat(DdcConfigFormat.DDC_CONFIG_FORMAT_UNSPECIFIED)
                .build();
        assertThatThrownBy(() -> mapper.fromConfig(unknown))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("config format");

        DdcConfigValue oversized = new DdcConfigValue();
        oversized.setResourceName("application.yml");
        oversized.setContent("x".repeat(1024 * 1024 + 1));
        oversized.setFormat("YAML");
        assertThatThrownBy(() -> mapper.toConfig(oversized))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("config content");

        DdcInstanceRegisterRequest excessiveMetadata =
                new DdcInstanceRegisterRequest();
        excessiveMetadata.setInstanceId("instance-1");
        excessiveMetadata.setBizCode("retail");
        excessiveMetadata.setEnv("prod");
        excessiveMetadata.setAppCode("order");
        excessiveMetadata.setHost("127.0.0.1");
        excessiveMetadata.setPid("1");
        excessiveMetadata.setSdkVersion("1.0");
        excessiveMetadata.setLeaseSeconds(30);
        excessiveMetadata.setHeartbeatIntervalSeconds(10);
        excessiveMetadata.setAdmissionTicket("config-register-ticket");
        java.util.LinkedHashMap<String, String> metadata = new java.util.LinkedHashMap<>();
        for (int index = 0; index < 33; index++) {
            metadata.put("key-" + index, "value");
        }
        excessiveMetadata.setMetadata(metadata);
        assertThatThrownBy(() -> mapper.toRegisterRequest(excessiveMetadata))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("metadata");

        DdcCommonProtoMapper tinyMessageMapper = new DdcCommonProtoMapper(8);
        assertThatThrownBy(() -> tinyMessageMapper.checked(unknown))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("message exceeds");
    }
}
