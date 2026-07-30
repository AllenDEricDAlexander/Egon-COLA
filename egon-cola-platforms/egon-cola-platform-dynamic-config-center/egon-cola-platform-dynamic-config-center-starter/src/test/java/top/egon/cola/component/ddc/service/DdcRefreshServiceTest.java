package top.egon.cola.component.ddc.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import top.egon.cola.component.ddc.client.DdcAdminClient;
import top.egon.cola.component.ddc.common.DdcChecksum;
import top.egon.cola.component.ddc.common.DdcException;
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

import java.time.Instant;
import top.egon.cola.component.ddc.repository.DdcLocalConfigRepository;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcRefreshServiceTest {

    @Test
    void targetMessageSubmitsAckToLifecycleDelivery() {
        DdcAckDelivery delivery = mock(DdcAckDelivery.class);
        when(delivery.submit(any())).thenReturn(true);
        DdcLocalConfigRepository repository = new DdcLocalConfigRepository();
        DdcConfigApplier applier = (key, value, version) -> {
        };
        DdcRefreshService service = new DdcRefreshService(
                repository,
                new DefaultDdcConfigApplierRegistry(applier),
                delivery,
                sessionHolder()
        );

        service.refresh(message("switch", "on", 2L));

        ArgumentCaptor<DdcAckRequest> ack =
                ArgumentCaptor.forClass(DdcAckRequest.class);
        verify(delivery).submit(ack.capture());
        assertThat(ack.getValue()).satisfies(request -> {
            assertThat(request.getChangeId()).isEqualTo("c1");
            assertThat(request.getStatus()).isEqualTo(DdcAckStatus.SUCCESS);
            assertThat(request.getInstanceId()).isEqualTo("instance-1");
            assertThat(request.getLeaseId()).isEqualTo("lease-1");
        });
    }

    @Test
    void batchSnapshotAppliesByPriorityThenConfigKey() {
        RecordingAdminClient client = new RecordingAdminClient();
        DdcLocalConfigRepository repository = new DdcLocalConfigRepository();
        List<String> applied = new ArrayList<>();
        DdcConfigApplier fallback = (key, value, version) -> applied.add(key);
        DefaultDdcConfigApplierRegistry registry =
                new DefaultDdcConfigApplierRegistry(fallback);
        registry.registerPrefix(
                "gateway.rules.chunk.",
                (key, value, version) -> applied.add(key)
        );
        registry.registerExact(
                "gateway.rules.active",
                new DdcConfigApplier() {
                    @Override
                    public void apply(String key, String value, long version) {
                        applied.add(key);
                    }

                    @Override
                    public int priority() {
                        return 100;
                    }
                }
        );
        registry.freeze();
        DdcLeaseSessionHolder holder = new DdcLeaseSessionHolder();
        DdcRefreshService service =
                new DdcRefreshService(repository, registry, client, holder);

        service.applySnapshots(List.of(
                config("gateway.rules.active", "release-1", 1L),
                config("gateway.rules.chunk.002", "chunk-2", 1L),
                config("gateway.rules.chunk.001", "chunk-1", 1L)
        ));

        assertThat(applied).containsExactly(
                "gateway.rules.chunk.001",
                "gateway.rules.chunk.002",
                "gateway.rules.active"
        );
    }

    @Test
    void snapshotAppliesWithoutAckAndOlderSnapshotCannotOverwriteTopicValue() {
        RecordingAdminClient client = new RecordingAdminClient();
        DdcLocalConfigRepository repository = new DdcLocalConfigRepository();
        AtomicReference<String> value = new AtomicReference<>();
        DdcRefreshService service = service(repository, (key, next, version) -> {
            value.set(next);
            repository.updateVersion(key, version);
        }, client);

        service.refresh(message("switch", "new", 2L));
        service.applySnapshot(config("switch", "old", 1L));

        assertThat(value).hasValue("new");
        assertThat(client.ackCount).isEqualTo(1);
    }

    @Test
    void targetMessageSendsLeaseAwareSuccessAndNonTargetDoesNothing() {
        RecordingAdminClient client = new RecordingAdminClient();
        DdcLocalConfigRepository repository = new DdcLocalConfigRepository();
        DdcRefreshService service = service(repository, (key, value, version) ->
                repository.updateVersion(key, version), client);

        service.refresh(message("switch", "on", 2L));

        assertThat(client.lastAck()).satisfies(ack -> {
            assertThat(ack.getStatus()).isEqualTo(DdcAckStatus.SUCCESS);
            assertThat(ack.getInstanceId()).isEqualTo("instance-1");
            assertThat(ack.getLeaseId()).isEqualTo("lease-1");
            assertThat(ack.getContentChecksum()).isNotBlank();
        });

        DdcPublishMessage nonTarget = message("switch", "off", 3L);
        nonTarget.setTargets(List.of(new DdcPublishTarget("other", "other-lease")));
        service.refresh(nonTarget);
        assertThat(client.ackCount).isEqualTo(1);
    }

    @Test
    void sameVersionAndChecksumReportsSuccessWithoutApplyingAgain() {
        RecordingAdminClient client = new RecordingAdminClient();
        DdcLocalConfigRepository repository = new DdcLocalConfigRepository();
        AtomicInteger applyCount = new AtomicInteger();
        DdcRefreshService service = service(repository, (key, value, version) -> {
            applyCount.incrementAndGet();
            repository.updateVersion(key, version);
        }, client);
        DdcPublishMessage message = message("switch", "on", 2L);

        service.refresh(message);
        service.refresh(message);

        assertThat(applyCount).hasValue(1);
        assertThat(client.lastAck().getStatus()).isEqualTo(DdcAckStatus.SUCCESS);
    }

    @Test
    void sameVersionWithDifferentChecksumReportsFailedAndKeepsLastKnownGoodValue() {
        RecordingAdminClient client = new RecordingAdminClient();
        DdcLocalConfigRepository repository = new DdcLocalConfigRepository();
        repository.updateVersion("switch", 2L);
        repository.updateChecksum("switch", DdcChecksum.content("old"));
        AtomicInteger applyCount = new AtomicInteger();
        DdcRefreshService service = service(repository, (key, value, version) ->
                applyCount.incrementAndGet(), client);

        service.refresh(message("switch", "new", 2L));

        assertThat(applyCount).hasValue(0);
        assertThat(repository.version("switch")).isEqualTo(2L);
        assertThat(repository.checksum("switch")).isEqualTo(DdcChecksum.content("old"));
        assertThat(client.lastAck().getStatus()).isEqualTo(DdcAckStatus.FAILED);
        assertThat(client.lastAck().getCurrentVersion()).isEqualTo(2L);
    }

    @Test
    void olderTopicMessageReportsIgnoredWithoutApplying() {
        RecordingAdminClient client = new RecordingAdminClient();
        DdcLocalConfigRepository repository = new DdcLocalConfigRepository();
        repository.updateVersion("switch", 3L);
        AtomicInteger applyCount = new AtomicInteger();
        DdcRefreshService service = service(repository, (key, value, version) ->
                applyCount.incrementAndGet(), client);

        service.refresh(message("switch", "old", 2L));

        assertThat(applyCount).hasValue(0);
        assertThat(client.lastAck().getStatus()).isEqualTo(DdcAckStatus.IGNORED);
        assertThat(client.lastAck().getCurrentVersion()).isEqualTo(3L);
    }

    @Test
    void malformedTargetMessageDoesNotApplyOrAck() {
        RecordingAdminClient client = new RecordingAdminClient();
        AtomicInteger applyCount = new AtomicInteger();
        DdcRefreshService service = service(new DdcLocalConfigRepository(),
                (key, value, version) -> applyCount.incrementAndGet(), client);
        DdcPublishMessage message = message("switch", "on", 2L);
        message.setContentChecksum(null);

        service.refresh(message);

        assertThat(applyCount).hasValue(0);
        assertThat(client.ackCount).isZero();
    }

    @Test
    void reportsFailedAckWhenApplyFails() {
        RecordingAdminClient client = new RecordingAdminClient();
        DdcRefreshService service = service(new DdcLocalConfigRepository(), (key, value, version) -> {
            throw new DdcException("convert config value failed");
        }, client);

        service.refresh(message("switch", "bad", 4L));

        assertThat(client.lastAck().getStatus()).isEqualTo(DdcAckStatus.FAILED);
        assertThat(client.lastAck().getErrorMessage()).contains("convert config value failed");
    }

    @Test
    void applyFailureRestoresMetadataAndReturnsBoundedSingleLineError() {
        RecordingAdminClient client = new RecordingAdminClient();
        DdcLocalConfigRepository repository = new DdcLocalConfigRepository();
        String oldChecksum = DdcChecksum.content("old");
        repository.updateVersion("switch", 2L);
        repository.updateChecksum("switch", oldChecksum);
        String detail = "unsafe\n" + "x".repeat(400);
        DdcRefreshService service = service(repository, (key, value, version) -> {
            repository.updateVersion(key, version);
            repository.updateChecksum(key, DdcChecksum.content(value));
            throw new IllegalStateException(detail);
        }, client);

        service.refresh(message("switch", "bad", 4L));

        assertThat(repository.version("switch")).isEqualTo(2L);
        assertThat(repository.checksum("switch")).isEqualTo(oldChecksum);
        assertThat(client.lastAck().getStatus()).isEqualTo(DdcAckStatus.FAILED);
        assertThat(client.lastAck().getErrorMessage())
                .startsWith("DDC config apply failed")
                .doesNotContain("\n", "\r", "IllegalStateException")
                .hasSizeLessThanOrEqualTo(256);
    }

    @Test
    void sameVersionSnapshotWithDifferentChecksumKeepsLastKnownGoodValue() {
        RecordingAdminClient client = new RecordingAdminClient();
        DdcLocalConfigRepository repository = new DdcLocalConfigRepository();
        repository.updateVersion("switch", 2L);
        String oldChecksum = DdcChecksum.content("old");
        repository.updateChecksum("switch", oldChecksum);
        AtomicInteger applyCount = new AtomicInteger();
        DdcRefreshService service = service(repository, (key, value, version) ->
                applyCount.incrementAndGet(), client);

        service.applySnapshot(config("switch", "new", 2L));

        assertThat(applyCount).hasValue(0);
        assertThat(repository.version("switch")).isEqualTo(2L);
        assertThat(repository.checksum("switch")).isEqualTo(oldChecksum);
        assertThat(client.ackCount).isZero();
    }

    @Test
    void ackTransportFailureDoesNotRollbackAppliedValueOrVersion() {
        RecordingAdminClient client = new RecordingAdminClient();
        client.failAck = true;
        DdcLocalConfigRepository repository = new DdcLocalConfigRepository();
        AtomicReference<String> value = new AtomicReference<>();
        DdcRefreshService service = service(repository, (key, next, version) -> {
            value.set(next);
            repository.updateVersion(key, version);
        }, client);

        service.refresh(message("switch", "on", 2L));

        assertThat(value).hasValue("on");
        assertThat(repository.version("switch")).isEqualTo(2L);
    }

    private DdcRefreshService service(DdcLocalConfigRepository repository,
                                      DdcConfigApplier applier,
                                      RecordingAdminClient client) {
        return new DdcRefreshService(repository, applier, client, sessionHolder());
    }

    private DdcLeaseSessionHolder sessionHolder() {
        DdcLeaseSessionHolder holder = new DdcLeaseSessionHolder();
        Instant registeredAt = Instant.parse("2026-07-24T12:00:00Z");
        holder.replace(new DdcLeaseSession(
                "instance-1",
                "lease-1",
                DdcLeaseRole.CONFIG_CLIENT,
                30,
                10,
                registeredAt,
                registeredAt.plusSeconds(30)
        ));
        return holder;
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
        message.setContentChecksum(DdcChecksum.content(value));
        message.setTargets(List.of(new DdcPublishTarget("instance-1", "lease-1")));
        return message;
    }

    private DdcConfigValue config(String key, String value, long version) {
        DdcConfigValue config = new DdcConfigValue();
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setVersion(version);
        return config;
    }

    static class RecordingAdminClient implements DdcAdminClient {

        private DdcAckRequest lastAck;

        private int ackCount;

        private boolean failAck;

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
            ackCount++;
            if (failAck) {
                throw new IllegalStateException("ack unavailable");
            }
        }

        DdcAckRequest lastAck() {
            return lastAck;
        }
    }
}
