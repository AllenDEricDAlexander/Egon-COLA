package top.egon.cola.component.ddc.admin.repository;

import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import top.egon.cola.component.ddc.admin.common.DdcAdminException;
import top.egon.cola.component.ddc.admin.model.vo.DdcAtomicPublishCommand;
import top.egon.cola.component.ddc.redis.DdcRedisKeys;
import top.egon.cola.component.ddc.model.config.DdcPublishMessage;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcRedisRepositoryTest {

    @Test
    void rejectsReusedChangeIdWithDifferentEventFingerprint() {
        RedissonClient redisson = mock(RedissonClient.class);
        RLock lock = lock(redisson);
        RBucket<String> idempotency = bucket();
        when(redisson.<String>getBucket(DdcRedisKeys.publishIdempotency(
                "default", "dev", "demo", "change-1"
        ))).thenReturn(idempotency);
        when(idempotency.get()).thenReturn("change-1:old-checksum");
        DdcAtomicPublishCommand command = command("different-checksum");

        assertThatThrownBy(() -> new DdcRedisRepository(redisson).dispatch(command))
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
        String configKey = DdcRedisKeys.config("default", "dev", "demo", "switch");
        String versionKey = DdcRedisKeys.version("default", "dev", "demo", "switch");
        String idempotencyKey = DdcRedisKeys.publishIdempotency(
                "default", "dev", "demo", "change-1");
        when(redisson.<String>getBucket(configKey)).thenReturn(value);
        when(redisson.<Long>getBucket(versionKey)).thenReturn(version);
        when(redisson.<String>getBucket(idempotencyKey)).thenReturn(idempotency);
        when(redisson.getTopic(DdcRedisKeys.topic("default", "dev", "demo")))
                .thenReturn(topic);
        when(version.get()).thenReturn(1L);
        when(idempotency.get()).thenReturn(null, "change-1:checksum");
        DdcAtomicPublishCommand command = command("checksum");
        DdcRedisRepository repository = new DdcRedisRepository(redisson);

        repository.dispatch(command);
        repository.dispatch(command);

        verify(value).set("on");
        verify(version).set(2L);
        verify(idempotency).set("change-1:checksum");
        verify(topic).publish(command.message());
        verify(lock, org.mockito.Mockito.times(2)).unlock();
    }

    @Test
    void writesOnlyV3ConfigProjection() {
        RedissonClient redisson = mock(RedissonClient.class);
        RLock lock = lock(redisson);
        RBucket<String> value = bucket();
        RBucket<Long> version = bucket();
        when(redisson.<String>getBucket(DdcRedisKeys.config(
                "default", "dev", "demo", "switch"
        ))).thenReturn(value);
        when(redisson.<Long>getBucket(DdcRedisKeys.version(
                "default", "dev", "demo", "switch"
        ))).thenReturn(version);

        new DdcRedisRepository(redisson)
                .writeConfig("default", "dev", "demo", "switch", "on", 2L);

        verify(value).set("on");
        verify(version).set(2L);
        verify(lock).unlock();
    }

    @Test
    void publishesOnlyV3ChangeTopic() {
        RedissonClient redisson = mock(RedissonClient.class);
        RTopic topic = mock(RTopic.class);
        DdcPublishMessage message = mock(DdcPublishMessage.class);
        when(message.getBizCode()).thenReturn("default");
        when(message.getAppCode()).thenReturn("demo");
        when(message.getEnv()).thenReturn("dev");
        when(redisson.getTopic(DdcRedisKeys.topic("default", "dev", "demo")))
                .thenReturn(topic);

        new DdcRedisRepository(redisson).publish(message);

        verify(topic).publish(message);
    }

    private DdcAtomicPublishCommand command(String checksum) {
        return new DdcAtomicPublishCommand(
                "config-1", "change-1", "default", "dev", "demo", "switch",
                1L, 2L, "on", checksum, new DdcPublishMessage()
        );
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
