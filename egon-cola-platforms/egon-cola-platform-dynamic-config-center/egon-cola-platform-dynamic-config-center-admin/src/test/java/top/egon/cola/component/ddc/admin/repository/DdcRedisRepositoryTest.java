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
                "default",
                "dev",
                "demo",
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
    void writesOnlyV3ConfigProjection() {
        RedissonClient redisson = mock(RedissonClient.class);
        RBucket<String> value = bucket();
        RBucket<Long> version = bucket();
        when(redisson.<String>getBucket(DdcKeys.v3Config(
                "default", "dev", "demo", "switch"
        ))).thenReturn(value);
        when(redisson.<Long>getBucket(DdcKeys.v3Version(
                "default", "dev", "demo", "switch"
        ))).thenReturn(version);

        new DdcRedisRepository(redisson)
                .writeConfig("default", "dev", "demo", "switch", "on", 2L);

        verify(value).set("on");
        verify(version).set(2L);
    }

    @Test
    void publishesOnlyV3ChangeTopic() {
        RedissonClient redisson = mock(RedissonClient.class);
        RTopic topic = mock(RTopic.class);
        DdcPublishMessage message = mock(DdcPublishMessage.class);
        when(message.getBizCode()).thenReturn("default");
        when(message.getAppCode()).thenReturn("demo");
        when(message.getEnv()).thenReturn("dev");
        when(redisson.getTopic(DdcKeys.v3Topic("default", "dev", "demo")))
                .thenReturn(topic);

        new DdcRedisRepository(redisson).publish(message);

        verify(topic).publish(message);
    }

    @SuppressWarnings("unchecked")
    private <T> RBucket<T> bucket() {
        return mock(RBucket.class);
    }
}
