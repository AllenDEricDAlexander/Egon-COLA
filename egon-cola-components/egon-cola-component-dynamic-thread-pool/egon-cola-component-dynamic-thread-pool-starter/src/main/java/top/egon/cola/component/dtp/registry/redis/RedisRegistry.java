package top.egon.cola.component.dtp.registry.redis;

import org.redisson.api.RBucket;
import org.redisson.api.RKeys;
import org.redisson.api.RList;
import org.redisson.api.RSet;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorSnapshot;
import top.egon.cola.component.dtp.domain.model.entity.ThreadPoolConfigEntity;
import top.egon.cola.component.dtp.domain.model.valobj.RegistryEnumVO;
import top.egon.cola.component.dtp.registry.IRegistry;
import top.egon.cola.component.dtp.registry.model.DtpAuditEvent;
import top.egon.cola.component.dtp.registry.model.DtpConfigChangeMessage;
import top.egon.cola.component.dtp.registry.model.DtpRedisKeys;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: RedisRegistry
 * @description: Redis 注册中心
 * @author: 有罗敷的马同学
 * @datetime: 2025Year-04Month-14Day-下午8:15
 * @Version: 1.0
 */
public class RedisRegistry implements IRegistry {

    private static final Duration SNAPSHOT_TTL = Duration.ofSeconds(90);

    private static final Duration AUDIT_EVENT_TTL = Duration.ofDays(30);

    private static final DateTimeFormatter EVENT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    private final RedissonClient redissonClient;

    public RedisRegistry(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public void reportSnapshot(ExecutorSnapshot snapshot) {
        RSet<String> apps = redissonClient.getSet(DtpRedisKeys.apps());
        apps.add(snapshot.getAppName());

        RSet<String> instances = redissonClient.getSet(DtpRedisKeys.instances(snapshot.getAppName()));
        instances.add(snapshot.getInstanceId());

        RBucket<ExecutorSnapshot> bucket = redissonClient.getBucket(
                DtpRedisKeys.snapshot(snapshot.getAppName(), snapshot.getInstanceId(), snapshot.getExecutorName()));
        bucket.set(snapshot, SNAPSHOT_TTL);
    }

    @Override
    public void reportSnapshots(List<ExecutorSnapshot> snapshots) {
        for (ExecutorSnapshot snapshot : snapshots) {
            reportSnapshot(snapshot);
        }
    }

    @Override
    public List<ExecutorSnapshot> querySnapshots(String appName, String instanceId) {
        List<ExecutorSnapshot> snapshots = new ArrayList<>();
        RKeys keys = redissonClient.getKeys();
        for (String key : keys.getKeysByPattern(DtpRedisKeys.snapshot(appName, instanceId, "*"))) {
            RBucket<ExecutorSnapshot> bucket = redissonClient.getBucket(key);
            ExecutorSnapshot snapshot = bucket.get();
            if (snapshot != null) {
                snapshots.add(snapshot);
            }
        }
        return snapshots;
    }

    @Override
    public ExecutorSnapshot querySnapshot(String appName, String instanceId, String executorName) {
        RBucket<ExecutorSnapshot> bucket = redissonClient.getBucket(DtpRedisKeys.snapshot(appName, instanceId, executorName));
        return bucket.get();
    }

    @Override
    public void publishConfigChange(DtpConfigChangeMessage message) {
        RTopic topic = redissonClient.getTopic(DtpRedisKeys.changeTopic(message.getAppName()));
        topic.publish(message);
    }

    @Override
    public void recordAuditEvent(DtpAuditEvent event) {
        RList<DtpAuditEvent> list = redissonClient.getList(DtpRedisKeys.event(event.getAppName(), formatEventDate(event.getCreatedAt())));
        list.add(event);
        list.expire(AUDIT_EVENT_TTL);
    }

    @Override
    public List<DtpAuditEvent> queryAuditEvents(String appName, String date) {
        RList<DtpAuditEvent> list = redissonClient.getList(DtpRedisKeys.event(appName, date));
        return list.readAll();
    }

    private String formatEventDate(Instant createdAt) {
        Instant eventTime = createdAt == null ? Instant.now() : createdAt;
        return EVENT_DATE_FORMATTER.format(eventTime);
    }

    @Deprecated
    @Override
    public void reportThreadPool(List<ThreadPoolConfigEntity> threadPoolEntities) {
        RList<ThreadPoolConfigEntity> list = redissonClient.getList(RegistryEnumVO.THREAD_POOL_CONFIG_LIST_KEY.getKey());
        list.delete();
        list.addAll(threadPoolEntities);
    }

    @Deprecated
    @Override
    public void reportThreadPoolConfigParameter(ThreadPoolConfigEntity threadPoolConfigEntity) {
        String cacheKey = RegistryEnumVO.THREAD_POOL_CONFIG_PARAMETER_LIST_KEY.getKey() + "_" + threadPoolConfigEntity.getAppName() + "_" + threadPoolConfigEntity.getThreadPoolName();
        RBucket<ThreadPoolConfigEntity> bucket = redissonClient.getBucket(cacheKey);
        bucket.set(threadPoolConfigEntity, Duration.ofDays(30));
    }

}
