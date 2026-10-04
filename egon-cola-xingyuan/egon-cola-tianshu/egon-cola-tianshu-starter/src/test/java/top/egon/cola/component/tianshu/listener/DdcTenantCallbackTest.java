package top.egon.cola.component.tianshu.listener;

import io.netty.buffer.ByteBuf;
import jakarta.validation.Validation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.redisson.client.handler.State;
import org.redisson.codec.Kryo5Codec;
import org.slf4j.MDC;
import top.egon.cola.component.tianshu.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.tianshu.autoconfigure.properties.DdcTenantProperties;
import top.egon.cola.component.tianshu.format.DdcChecksum;
import top.egon.cola.component.tianshu.listener.config.DdcConfigChangeListener;
import top.egon.cola.component.tianshu.model.config.DdcPublishMessage;
import top.egon.cola.component.tianshu.model.config.DdcPublishTarget;
import top.egon.cola.component.tianshu.error.DdcException;
import top.egon.cola.component.tianshu.error.DdcErrorStatus;
import top.egon.cola.component.tianshu.model.lease.DdcLeaseRole;
import top.egon.cola.component.tianshu.model.lease.DdcLeaseSession;
import top.egon.cola.component.tianshu.model.registry.DdcServiceKey;
import top.egon.cola.component.tianshu.model.registry.DdcServiceKind;
import top.egon.cola.component.tianshu.model.tenant.DdcTenantMessageDTO;
import top.egon.cola.component.tianshu.model.event.DdcConfigDispatchPreparedEvent;
import top.egon.cola.component.tianshu.service.refresh.DdcRefreshService;
import top.egon.cola.component.tianshu.state.DdcActiveRegistrationIndex;
import top.egon.cola.component.tianshu.state.DdcLeaseSessionHolder;
import top.egon.cola.component.tianshu.state.DdcLocalConfigState;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class DdcTenantCallbackTest {

    private final ValidationUtils validationUtils = new ValidationUtils(
            Validation.buildDefaultValidatorFactory().getValidator());

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void rejectsWrongTenantMessageAndRestoresMdcWhenAcceptedCallbackFails() {
        DdcProperties properties = new DdcProperties();
        properties.setBizCode("retail");
        properties.setEnv("dev");
        properties.setAppCode("order");
        DdcTenantProperties tenantProperties = new DdcTenantProperties();
        tenantProperties.setTenantId(101L);
        DdcRefreshService refreshService = mock(DdcRefreshService.class);
        DdcConfigChangeListener listener = new DdcConfigChangeListener(
                properties, tenantProperties, refreshService, validationUtils);
        DdcPublishMessage message = publishMessage();

        listener.onMessage("tenant-topic", new DdcTenantMessageDTO<>(0L, message));
        listener.onMessage("tenant-topic", new DdcTenantMessageDTO<>(202L, message));
        verifyNoInteractions(refreshService);

        MDC.put("tenantId", "999");
        MDC.put("userId", "system:outer");
        doAnswer(invocation -> {
            assertThat(MDC.get("tenantId")).isEqualTo("101");
            throw new IllegalStateException("callback failure");
        }).when(refreshService).refresh(message);

        assertThatThrownBy(() -> listener.onMessage(
                "tenant-topic", new DdcTenantMessageDTO<>(101L, message)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("callback failure");
        assertThat(MDC.get("tenantId")).isEqualTo("999");
        assertThat(MDC.get("userId")).isEqualTo("system:outer");
        assertThat(new DdcTenantMessageDTO<>(101L, message).toString())
                .doesNotContain(message.getContent());
    }

    @Test
    void redissonDefaultCodecPreservesTypedPayloadInsideTenantEnvelope() throws Exception {
        Kryo5Codec codec = new Kryo5Codec();
        ByteBuf encoded = codec.getValueEncoder().encode(
                new DdcTenantMessageDTO<>(101L, preparedEvent(101L, publishMessage())));
        try {
            Object decoded = codec.getValueDecoder().decode(encoded, new State());

            assertThat(decoded).isInstanceOf(DdcTenantMessageDTO.class);
            DdcTenantMessageDTO<?> envelope = (DdcTenantMessageDTO<?>) decoded;
            assertThat(envelope.getTenantId()).isEqualTo(101L);
            assertThat(envelope.getPayload()).isInstanceOf(DdcConfigDispatchPreparedEvent.class);
            DdcConfigDispatchPreparedEvent event = (DdcConfigDispatchPreparedEvent) envelope.getPayload();
            assertThat(event.getEventId()).hasSize(64);
            assertThat(event.getTenantId()).isEqualTo(101L);
            assertThat(event.getMessage().getContent())
                    .isEqualTo("feature: true\n");
        } finally {
            encoded.release();
        }
    }

    @Test
    void rejectsPreparedEventWithInvalidEnvelopeIdentityBeforeRefresh() {
        DdcProperties properties = new DdcProperties();
        properties.setBizCode("retail");
        properties.setEnv("dev");
        properties.setAppCode("order");
        DdcTenantProperties tenantProperties = new DdcTenantProperties();
        tenantProperties.setTenantId(101L);
        DdcRefreshService refreshService = mock(DdcRefreshService.class);
        DdcConfigChangeListener listener = new DdcConfigChangeListener(
                properties, tenantProperties, refreshService, validationUtils);
        DdcConfigDispatchPreparedEvent event = preparedEvent(101L, publishMessage());
        event.setEventChecksum("0".repeat(64));

        listener.onMessage("tenant-topic", new DdcTenantMessageDTO<>(101L, event));

        verifyNoInteractions(refreshService);
    }

    @Test
    void publishMessageKeepsDefensiveTargetsAndDoesNotExposeContentInToString() {
        DdcPublishMessage message = publishMessage();
        List<DdcPublishTarget> targets = new java.util.ArrayList<>(message.getTargets());

        message.setTargets(targets);
        targets.clear();

        assertThat(message.getTargets()).containsExactly(new DdcPublishTarget("instance-1", "lease-1"));
        assertThatThrownBy(() -> message.getTargets().add(new DdcPublishTarget("instance-2", "lease-2")))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(message.toString()).doesNotContain(message.getContent());
    }

    @Test
    void tenantSeparatesLocalConfigLeaseAndRegistrationState() {
        DdcLocalConfigState tenantAState = new DdcLocalConfigState(101L);
        DdcLocalConfigState tenantBState = new DdcLocalConfigState(202L);
        tenantAState.updateVersion("application.yml", 7L);
        tenantBState.updateVersion("application.yml", 2L);

        assertThat(tenantAState.version("application.yml")).isEqualTo(7L);
        assertThat(tenantBState.version("application.yml")).isEqualTo(2L);

        Instant now = Instant.parse("2026-09-27T00:00:00Z");
        DdcLeaseSession sessionA = session("instance-a", "same-lease", now);
        DdcLeaseSession sessionB = session("instance-b", "same-lease", now);
        DdcLeaseSessionHolder tenantAHolder = new DdcLeaseSessionHolder(101L);
        DdcLeaseSessionHolder tenantBHolder = new DdcLeaseSessionHolder(202L);
        tenantAHolder.replace(sessionA);
        tenantBHolder.replace(sessionB);

        DdcServiceKey serviceKey = new DdcServiceKey(
                "retail", "dev", "order", DdcServiceKind.RPC_PROVIDER,
                "order.v1.Query", "default", "1.0.0", "grpc");
        DdcActiveRegistrationIndex tenantAIndex = new DdcActiveRegistrationIndex(101L);
        DdcActiveRegistrationIndex tenantBIndex = new DdcActiveRegistrationIndex(202L);
        tenantAIndex.put(serviceKey, sessionA);
        tenantBIndex.put(serviceKey, sessionB);

        assertThat(tenantAHolder.current()).contains(sessionA);
        assertThat(tenantBHolder.current()).contains(sessionB);
        assertThat(tenantAIndex.require("instance-a", "same-lease")).isEqualTo(serviceKey);
        assertThat(tenantBIndex.require("instance-b", "same-lease")).isEqualTo(serviceKey);
        assertThatThrownBy(() -> tenantAIndex.require("instance-b", "same-lease"))
                .isInstanceOfSatisfying(DdcException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo(DdcErrorStatus.LEASE_MISMATCH.getCode()));
    }

    private DdcPublishMessage publishMessage() {
        DdcPublishMessage message = new DdcPublishMessage();
        message.setChangeId("change-1");
        message.setBizCode("retail");
        message.setEnv("dev");
        message.setAppCode("order");
        message.setResourceName("application.yml");
        message.setFormat("yaml");
        message.setContent("feature: true\n");
        message.setTargetVersion(2L);
        message.setPublishMode("SYNC_ALL_ACK");
        message.setOperator("user:test");
        message.setTimestamp(1790000000000L);
        message.setResourceChecksum(DdcChecksum.resource(
                message.getResourceName(), message.getFormat(), message.getContent()));
        message.setTargets(List.of(new DdcPublishTarget("instance-1", "lease-1")));
        message.setChecksum(DdcChecksum.sha256(message));
        return message;
    }

    private DdcConfigDispatchPreparedEvent preparedEvent(Long tenantId, DdcPublishMessage message) {
        DdcConfigDispatchPreparedEvent event = new DdcConfigDispatchPreparedEvent();
        event.setEventId(DdcConfigDispatchPreparedEvent.eventId(tenantId, message.getChangeId(), 0));
        event.setTenantId(tenantId);
        event.setSchemaVersion("1");
        event.setAttempt(0);
        event.setConfigId("810001");
        event.setExpectedPublishedVersion(1L);
        event.setMessage(message);
        event.setEventChecksum(event.calculateEventChecksum());
        return event;
    }

    private DdcLeaseSession session(String instanceId, String leaseId, Instant now) {
        return new DdcLeaseSession(
                instanceId, leaseId, DdcLeaseRole.CONFIG_CLIENT,
                30, 10, now, now.plusSeconds(30));
    }
}
