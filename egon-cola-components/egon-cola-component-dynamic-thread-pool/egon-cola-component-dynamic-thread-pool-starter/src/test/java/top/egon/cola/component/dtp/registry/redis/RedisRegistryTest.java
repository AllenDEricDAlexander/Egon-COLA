package top.egon.cola.component.dtp.registry.redis;

import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RList;
import org.redisson.api.RSet;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorSnapshot;
import top.egon.cola.component.dtp.registry.model.DtpAuditEvent;
import top.egon.cola.component.dtp.registry.model.DtpConfigChangeMessage;
import top.egon.cola.component.dtp.registry.model.DtpRedisKeys;

import java.time.Duration;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * @description RedisRegistry 单元测试
 */
public class RedisRegistryTest {

    /**
     * 测试：上报执行器快照
     */
    @Test
    public void test_reportSnapshot() {
        RedissonClient mockRedissonClient = mock(RedissonClient.class);
        @SuppressWarnings("rawtypes")
        RSet mockApps = mock(RSet.class);
        @SuppressWarnings("rawtypes")
        RSet mockInstances = mock(RSet.class);
        @SuppressWarnings("rawtypes")
        RBucket mockSnapshotBucket = mock(RBucket.class);
        RedisRegistry redisRegistry = new RedisRegistry(mockRedissonClient);

        ExecutorSnapshot snapshot = new ExecutorSnapshot();
        snapshot.setAppName("test-app");
        snapshot.setInstanceId("i-001");
        snapshot.setExecutorName("executor01");

        when(mockRedissonClient.getSet(DtpRedisKeys.apps())).thenReturn(mockApps);
        when(mockRedissonClient.getSet(DtpRedisKeys.instances("test-app"))).thenReturn(mockInstances);
        when(mockRedissonClient.getBucket(DtpRedisKeys.snapshot("test-app", "i-001", "executor01"))).thenReturn(mockSnapshotBucket);

        redisRegistry.reportSnapshot(snapshot);

        verify(mockRedissonClient, times(1)).getSet(DtpRedisKeys.apps());
        verify(mockApps, times(1)).add("test-app");
        verify(mockRedissonClient, times(1)).getSet(DtpRedisKeys.instances("test-app"));
        verify(mockInstances, times(1)).add("i-001");
        verify(mockRedissonClient, times(1)).getBucket(DtpRedisKeys.snapshot("test-app", "i-001", "executor01"));
        verify(mockSnapshotBucket, times(1)).set(eq(snapshot), eq(Duration.ofSeconds(90)));
    }

    /**
     * 测试：发布配置变更消息
     */
    @Test
    public void test_publishConfigChange() {
        RedissonClient mockRedissonClient = mock(RedissonClient.class);
        RTopic mockTopic = mock(RTopic.class);
        RedisRegistry redisRegistry = new RedisRegistry(mockRedissonClient);

        DtpConfigChangeMessage message = new DtpConfigChangeMessage();
        message.setAppName("test-app");

        when(mockRedissonClient.getTopic(DtpRedisKeys.changeTopic("test-app"))).thenReturn(mockTopic);

        redisRegistry.publishConfigChange(message);

        verify(mockRedissonClient, times(1)).getTopic(DtpRedisKeys.changeTopic("test-app"));
        verify(mockTopic, times(1)).publish(message);
    }

    /**
     * 测试：记录审计事件
     */
    @Test
    public void test_recordAuditEvent() {
        RedissonClient mockRedissonClient = mock(RedissonClient.class);
        @SuppressWarnings("rawtypes")
        RList mockList = mock(RList.class);
        RedisRegistry redisRegistry = new RedisRegistry(mockRedissonClient);

        DtpAuditEvent event = new DtpAuditEvent();
        event.setAppName("test-app");
        event.setCreatedAt(Instant.parse("2026-06-29T10:15:30Z"));

        when(mockRedissonClient.getList(DtpRedisKeys.event("test-app", "20260629"))).thenReturn(mockList);

        redisRegistry.recordAuditEvent(event);

        verify(mockRedissonClient, times(1)).getList(DtpRedisKeys.event("test-app", "20260629"));
        verify(mockList, times(1)).add(event);
        verify(mockList, times(1)).expire(Duration.ofDays(30));
    }
}
