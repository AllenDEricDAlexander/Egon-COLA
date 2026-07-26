package top.egon.cola.component.ddc.admin.repository;

import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import top.egon.cola.component.ddc.admin.common.DdcAdminException;
import top.egon.cola.component.ddc.admin.model.vo.DdcAtomicPublishCommand;
import top.egon.cola.component.ddc.common.DdcKeys;
import top.egon.cola.component.ddc.model.dto.DdcPublishMessage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcRedisRepositoryTest {

    @Test
    void rejectsReusedChangeIdWithDifferentEventFingerprint() {
        RedissonClient redisson = mock(RedissonClient.class);
        RScript script = mock(RScript.class);
        when(redisson.getScript()).thenReturn(script);
        when(script.eval(
                eq(RScript.Mode.READ_WRITE),
                anyString(),
                eq(RScript.ReturnType.MULTI),
                anyList(),
                any(Object[].class)
        )).thenReturn(List.of(3L));
        DdcPublishMessage message = new DdcPublishMessage();
        DdcAtomicPublishCommand command = new DdcAtomicPublishCommand(
                "config-1",
                "change-1",
                "demo",
                "dev",
                "default",
                "switch",
                1L,
                2L,
                "on",
                "different-checksum",
                message
        );

        assertThatThrownBy(() -> new DdcRedisRepository(redisson).dispatch(command))
                .isInstanceOf(DdcAdminException.class)
                .hasMessageContaining("change id conflict");
    }

    @Test
    void writesBothV2AndLegacyConfigProjections() {
        RedissonClient redisson = mock(RedissonClient.class);
        RBucket<String> v2Value = bucket();
        RBucket<Long> v2Version = bucket();
        RBucket<String> legacyValue = bucket();
        RBucket<Long> legacyVersion = bucket();
        when(redisson.<String>getBucket(DdcKeys.v2Config(
                "demo", "dev", "default", "switch"
        ))).thenReturn(v2Value);
        when(redisson.<Long>getBucket(DdcKeys.v2Version(
                "demo", "dev", "default", "switch"
        ))).thenReturn(v2Version);
        when(redisson.<String>getBucket(DdcKeys.config(
                "demo", "dev", "default", "switch"
        ))).thenReturn(legacyValue);
        when(redisson.<Long>getBucket(DdcKeys.version(
                "demo", "dev", "default", "switch"
        ))).thenReturn(legacyVersion);

        new DdcRedisRepository(redisson)
                .writeConfig("demo", "dev", "default", "switch", "on", 2L);

        verify(v2Value).set("on");
        verify(v2Version).set(2L);
        verify(legacyValue).set("on");
        verify(legacyVersion).set(2L);
    }

    @Test
    void publishesBothV2AndLegacyChangeTopics() {
        RedissonClient redisson = mock(RedissonClient.class);
        RTopic v2 = mock(RTopic.class);
        RTopic legacy = mock(RTopic.class);
        DdcPublishMessage message = mock(DdcPublishMessage.class);
        when(message.getAppCode()).thenReturn("demo");
        when(message.getEnv()).thenReturn("dev");
        when(message.getNamespace()).thenReturn("default");
        when(redisson.getTopic(DdcKeys.v2Topic("demo", "dev", "default")))
                .thenReturn(v2);
        when(redisson.getTopic(DdcKeys.topic("demo", "dev", "default")))
                .thenReturn(legacy);

        new DdcRedisRepository(redisson).publish(message);

        verify(v2).publish(message);
        verify(legacy).publish(message);
    }

    @SuppressWarnings("unchecked")
    private <T> RBucket<T> bucket() {
        return mock(RBucket.class);
    }
}
