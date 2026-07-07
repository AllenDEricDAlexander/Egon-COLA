package top.egon.cola.component.ddc.service;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.client.DdcAdminClient;
import top.egon.cola.component.ddc.common.DdcException;
import top.egon.cola.component.ddc.model.dto.DdcAckRequest;
import top.egon.cola.component.ddc.model.dto.DdcDefaultReportRequest;
import top.egon.cola.component.ddc.model.dto.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.dto.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.dto.DdcPublishMessage;
import top.egon.cola.component.ddc.model.enums.DdcAckStatus;
import top.egon.cola.component.ddc.model.vo.DdcConfigValue;
import top.egon.cola.component.ddc.repository.DdcLocalConfigRepository;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DdcRefreshServiceTest {

    @Test
    void ignoresLowerVersionAndReportsIgnoredAck() {
        RecordingAdminClient client = new RecordingAdminClient();
        DdcLocalConfigRepository repository = new DdcLocalConfigRepository();
        repository.updateVersion("switch", 3L);
        DdcRefreshService service = new DdcRefreshService(repository, (key, value, version) -> {
        }, client);

        service.refresh(message("switch", "1", 2L));

        assertThat(client.lastAck().getStatus()).isEqualTo(DdcAckStatus.IGNORED);
    }

    @Test
    void reportsFailedAckWhenApplyFails() {
        RecordingAdminClient client = new RecordingAdminClient();
        DdcRefreshService service = new DdcRefreshService(new DdcLocalConfigRepository(), (key, value, version) -> {
            throw new DdcException("convert config value failed");
        }, client);

        service.refresh(message("switch", "bad", 4L));

        assertThat(client.lastAck().getStatus()).isEqualTo(DdcAckStatus.FAILED);
        assertThat(client.lastAck().getErrorMessage()).contains("convert config value failed");
    }

    private DdcPublishMessage message(String key, String value, long version) {
        DdcPublishMessage message = new DdcPublishMessage();
        message.setChangeId("c1");
        message.setAppCode("demo");
        message.setEnv("dev");
        message.setNamespace("default");
        message.setConfigKey(key);
        message.setConfigValue(value);
        message.setTargetVersion(version);
        return message;
    }

    static class RecordingAdminClient implements DdcAdminClient {

        private DdcAckRequest lastAck;

        @Override
        public void register(DdcInstanceRegisterRequest request) {
        }

        @Override
        public void heartbeat(DdcHeartbeatRequest request) {
        }

        @Override
        public void offline(DdcHeartbeatRequest request) {
        }

        @Override
        public List<DdcConfigValue> pull() {
            return Collections.emptyList();
        }

        @Override
        public void reportDefaults(DdcDefaultReportRequest request) {
        }

        @Override
        public void ack(DdcAckRequest request) {
            this.lastAck = request;
        }

        DdcAckRequest lastAck() {
            return lastAck;
        }
    }
}
