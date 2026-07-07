package top.egon.cola.component.ddc.test;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.client.DdcAdminClient;
import top.egon.cola.component.ddc.common.DdcValueConverter;
import top.egon.cola.component.ddc.model.dto.DdcAckRequest;
import top.egon.cola.component.ddc.model.dto.DdcDefaultReportRequest;
import top.egon.cola.component.ddc.model.dto.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.dto.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.dto.DdcPublishMessage;
import top.egon.cola.component.ddc.model.enums.DdcAckStatus;
import top.egon.cola.component.ddc.model.vo.DdcConfigValue;
import top.egon.cola.component.ddc.repository.DdcLocalConfigRepository;
import top.egon.cola.component.ddc.service.DdcFieldBindingService;
import top.egon.cola.component.ddc.service.DdcRefreshService;
import top.egon.cola.component.ddc.test.service.SampleConfigService;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DdcSampleRefreshFlowTest {

    @Test
    void refreshUpdatesBoundFieldAndReportsSuccessAck() {
        RecordingAdminClient adminClient = new RecordingAdminClient();
        DdcLocalConfigRepository repository = new DdcLocalConfigRepository();
        DdcFieldBindingService bindingService = new DdcFieldBindingService(repository, new DdcValueConverter());
        SampleConfigService sample = new SampleConfigService();
        bindingService.bind(sample, SampleConfigService.class);
        DdcRefreshService refreshService = new DdcRefreshService(repository, bindingService::apply, adminClient);

        refreshService.refresh(message("rateLimit", "200", 2L));

        assertThat(sample.getRateLimit()).isEqualTo(200);
        assertThat(adminClient.lastAck().getStatus()).isEqualTo(DdcAckStatus.SUCCESS);
    }

    private DdcPublishMessage message(String key, String value, long version) {
        DdcPublishMessage message = new DdcPublishMessage();
        message.setChangeId("c1");
        message.setAppCode("demo-app");
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
