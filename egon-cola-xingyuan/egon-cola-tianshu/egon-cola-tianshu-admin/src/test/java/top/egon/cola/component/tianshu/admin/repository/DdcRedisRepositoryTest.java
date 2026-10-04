package top.egon.cola.component.tianshu.admin.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import top.egon.cola.component.tianshu.admin.common.DdcAdminException;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAtomicPublishCommand;
import top.egon.cola.component.tianshu.redis.DdcRedisKeys;
import top.egon.cola.component.tianshu.model.config.DdcPublishMessage;
import top.egon.cola.component.tianshu.model.config.DdcPublishTarget;
import top.egon.cola.component.tianshu.model.event.DdcConfigDispatchPreparedEvent;
import top.egon.cola.component.tianshu.model.tenant.DdcTenantMessageDTO;
import top.egon.cola.component.tianshu.format.DdcChecksum;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcRedisRepositoryTest {

    @BeforeEach
    void bindTenant() {
        MDC.put("tenantId", "101");
    }

    @AfterEach
    void clearTenant() {
        MDC.clear();
    }

    @Test
    void rejectsReusedChangeIdWithDifferentEventFingerprint() {
        RedissonClient redisson = mock(RedissonClient.class);
        RLock lock = lock(redisson);
        RBucket<String> idempotency = bucket();
        when(redisson.<String>getBucket(DdcRedisKeys.publishIdempotency(101L,
                "default", "dev", "demo", "change-1"
        ))).thenReturn(idempotency);
        when(idempotency.get()).thenReturn("change-1:old-checksum");
        DdcAtomicPublishCommand command = command("different-checksum");

        assertThatThrownBy(() -> repository(redisson).dispatch(command))
                .isInstanceOf(DdcAdminException.class)
                .hasMessageContaining("change id conflict");
        verify(lock).unlock();
        verify(redisson, never()).getTopic(anyString());
    }

    @Test
    void dispatchesThroughRedissonObjectsAndKeepsIdempotentRetriesQuiet() {
        RedissonClient redisson = mock(RedissonClient.class);
        RLock lock = lock(redisson);
        RBucket<String> value = bucket();
        RBucket<Long> version = bucket();
        RBucket<String> idempotency = bucket();
        RTopic topic = mock(RTopic.class);
        String configKey = DdcRedisKeys.config(101L, "default", "dev", "demo", "switch");
        String versionKey = DdcRedisKeys.version(101L, "default", "dev", "demo", "switch");
        String idempotencyKey = DdcRedisKeys.publishIdempotency(101L,
                "default", "dev", "demo", "change-1");
        when(redisson.<String>getBucket(configKey)).thenReturn(value);
        when(redisson.<Long>getBucket(versionKey)).thenReturn(version);
        when(redisson.<String>getBucket(idempotencyKey)).thenReturn(idempotency);
        when(redisson.getTopic(DdcRedisKeys.topic(101L, "default", "dev", "demo")))
                .thenReturn(topic);
        when(version.get()).thenReturn(1L);
        when(idempotency.get()).thenReturn(null, "change-1:checksum");
        DdcAtomicPublishCommand command = command("checksum");
        DdcRedisRepository repository = repository(redisson);

        repository.dispatch(command);
        repository.dispatch(command);

        verify(value).set("on");
        verify(version).set(2L);
        verify(idempotency).set("change-1:checksum");
        verify(topic).publish(new DdcTenantMessageDTO<>(101L, command.message()));
        verify(lock, org.mockito.Mockito.times(2)).unlock();
    }

    @Test
    void preparedEventRetryReplaysTopicWithoutRepeatingRedisStateWrites() {
        RedissonClient redisson = mock(RedissonClient.class);
        RLock lock = lock(redisson);
        RBucket<String> value = bucket();
        RBucket<Long> version = bucket();
        RBucket<String> idempotency = bucket();
        RTopic topic = mock(RTopic.class);
        DdcPublishMessage message = publishMessage();
        DdcConfigDispatchPreparedEvent event = new DdcConfigDispatchPreparedEvent();
        event.setEventId(DdcConfigDispatchPreparedEvent.eventId(101L, message.getChangeId(), 0));
        event.setTenantId(101L);
        event.setSchemaVersion("1");
        event.setAttempt(0);
        event.setConfigId("config-1");
        event.setExpectedPublishedVersion(1L);
        event.setMessage(message);
        event.setEventChecksum(event.calculateEventChecksum());
        DdcAtomicPublishCommand command = DdcAtomicPublishCommand.builder()
                .configId("config-1").changeId(message.getChangeId())
                .bizCode(message.getBizCode()).env(message.getEnv()).appCode(message.getAppCode())
                .resourceName(message.getResourceName()).expectedPublishedVersion(1L)
                .targetVersion(2L).content(message.getContent()).eventChecksum(event.getEventChecksum())
                .message(message).tenantId(101L).attempt(0).eventId(event.getEventId()).build();
        String key = DdcRedisKeys.publishIdempotency(101L,
                "default", "dev", "demo", event.getEventId());
        when(redisson.<String>getBucket(DdcRedisKeys.config(101L, "default", "dev", "demo", "switch")))
                .thenReturn(value);
        when(redisson.<Long>getBucket(DdcRedisKeys.version(101L, "default", "dev", "demo", "switch")))
                .thenReturn(version);
        when(redisson.<String>getBucket(key)).thenReturn(idempotency);
        when(redisson.getTopic(DdcRedisKeys.topic(101L, "default", "dev", "demo"))).thenReturn(topic);
        when(version.get()).thenReturn(1L);
        when(idempotency.get()).thenReturn(null, event.getEventId() + ":" + event.getEventChecksum());

        DdcRedisRepository repository = repository(redisson);
        repository.dispatch(command, event);
        repository.dispatch(command, event);

        verify(value).set("on");
        verify(version).set(2L);
        verify(idempotency).set(event.getEventId() + ":" + event.getEventChecksum());
        verify(topic, org.mockito.Mockito.times(2)).publish(new DdcTenantMessageDTO<>(101L, event));
        verify(lock, org.mockito.Mockito.times(2)).unlock();
    }

    @Test
    void writesOnlyV4TenantConfigProjection() {
        RedissonClient redisson = mock(RedissonClient.class);
        RLock lock = lock(redisson);
        RBucket<String> value = bucket();
        RBucket<Long> version = bucket();
        when(redisson.<String>getBucket(DdcRedisKeys.config(101L,
                "default", "dev", "demo", "switch"
        ))).thenReturn(value);
        when(redisson.<Long>getBucket(DdcRedisKeys.version(101L,
                "default", "dev", "demo", "switch"
        ))).thenReturn(version);

        repository(redisson)
                .writeConfig("default", "dev", "demo", "switch", "on", 2L);

        verify(value).set("on");
        verify(version).set(2L);
        verify(lock).unlock();
    }

    @Test
    void publishesTenantEnvelopeOnV4ChangeTopic() {
        RedissonClient redisson = mock(RedissonClient.class);
        RTopic topic = mock(RTopic.class);
        DdcPublishMessage message = mock(DdcPublishMessage.class);
        when(message.getBizCode()).thenReturn("default");
        when(message.getAppCode()).thenReturn("demo");
        when(message.getEnv()).thenReturn("dev");
        when(redisson.getTopic(DdcRedisKeys.topic(101L, "default", "dev", "demo")))
                .thenReturn(topic);

        repository(redisson).publish(message);

        verify(topic).publish(new DdcTenantMessageDTO<>(101L, message));
    }

    private DdcAtomicPublishCommand command(String checksum) {
        return new DdcAtomicPublishCommand(
                "config-1", "change-1", "default", "dev", "demo", "switch",
                1L, 2L, "on", checksum, new DdcPublishMessage()
        );
    }

    private DdcRedisRepository repository(RedissonClient redisson) {
        return new DdcRedisRepository(redisson, new ObjectMapper());
    }

    private DdcPublishMessage publishMessage() {
        DdcPublishMessage message = new DdcPublishMessage();
        message.setChangeId("change-1");
        message.setBizCode("default");
        message.setAppCode("demo");
        message.setEnv("dev");
        message.setResourceName("switch");
        message.setFormat("YAML");
        message.setContent("on");
        message.setTargetVersion(2L);
        message.setPublishMode("SYNC_ALL_ACK");
        message.setOperator("user:test");
        message.setTimestamp(1790899200000L);
        message.setResourceChecksum(DdcChecksum.resource("switch", "YAML", "on"));
        message.setTargets(List.of(new DdcPublishTarget("instance-1", "lease-1")));
        message.setChecksum(DdcChecksum.sha256(message));
        return message;
    }

    private RLock lock(RedissonClient redisson) {
        RLock lock = mock(RLock.class);
        when(redisson.getLock(anyString())).thenReturn(lock);
        return lock;
    }

    @SuppressWarnings("unchecked")
    private <T> RBucket<T> bucket() {
        return mock(RBucket.class);
    }
}
