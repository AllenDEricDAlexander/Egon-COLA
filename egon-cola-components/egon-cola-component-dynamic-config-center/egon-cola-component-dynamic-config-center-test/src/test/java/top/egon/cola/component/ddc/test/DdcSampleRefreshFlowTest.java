package top.egon.cola.component.ddc.test;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.client.DdcAdminClient;
import top.egon.cola.component.ddc.common.DdcChecksum;
import top.egon.cola.component.ddc.common.DdcValueConverter;
import top.egon.cola.component.ddc.model.dto.DdcAckRequest;
import top.egon.cola.component.ddc.model.dto.DdcDefaultReportRequest;
import top.egon.cola.component.ddc.model.dto.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.dto.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.dto.DdcPublishMessage;
import top.egon.cola.component.ddc.model.dto.DdcPublishTarget;
import top.egon.cola.component.ddc.model.enums.DdcAckStatus;
import top.egon.cola.component.ddc.model.enums.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.enums.DdcLeaseRole;
import top.egon.cola.component.ddc.model.vo.DdcConfigValue;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;
import top.egon.cola.component.ddc.repository.DdcLocalConfigRepository;
import top.egon.cola.component.ddc.service.DdcFieldBindingService;
import top.egon.cola.component.ddc.service.DdcLeaseSessionHolder;
import top.egon.cola.component.ddc.service.DdcRefreshService;
import top.egon.cola.component.ddc.test.service.SampleConfigService;

import java.time.Instant;
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
        DdcLeaseSessionHolder sessionHolder = new DdcLeaseSessionHolder();
        sessionHolder.replace(adminClient.session());
        DdcRefreshService refreshService =
                new DdcRefreshService(repository, bindingService::apply, adminClient, sessionHolder);

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
        message.setContentChecksum(DdcChecksum.content(value));
        message.setTargets(List.of(new DdcPublishTarget("instance-1", "lease-1")));
        return message;
    }

    static class RecordingAdminClient implements DdcAdminClient {

        private DdcAckRequest lastAck;

        @Override
        public DdcLeaseSession register(DdcInstanceRegisterRequest request) {
            Instant registeredAt = Instant.parse("2026-07-24T12:00:00Z");
            return new DdcLeaseSession(
                    request.getInstanceId(),
                    "lease-1",
                    DdcLeaseRole.CONFIG_CLIENT,
                    30,
                    10,
                    registeredAt,
                    registeredAt.plusSeconds(30)
            );
        }

        @Override
        public DdcLeaseOperationResult heartbeat(DdcHeartbeatRequest request) {
            return new DdcLeaseOperationResult(
                    DdcLeaseOperationStatus.RENEWED,
                    Instant.parse("2026-07-24T12:00:30Z")
            );
        }

        @Override
        public DdcLeaseOperationResult offline(DdcHeartbeatRequest request) {
            return new DdcLeaseOperationResult(DdcLeaseOperationStatus.DELETED, null);
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

        DdcLeaseSession session() {
            Instant registeredAt = Instant.parse("2026-07-24T12:00:00Z");
            return new DdcLeaseSession(
                    "instance-1",
                    "lease-1",
                    DdcLeaseRole.CONFIG_CLIENT,
                    30,
                    10,
                    registeredAt,
                    registeredAt.plusSeconds(30)
            );
        }
    }
}
