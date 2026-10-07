package top.egon.cola.component.tianshu.admin.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.tianshu.listener.registry.DdcRegistrySubscriptionCoordinator;
import top.egon.cola.component.tianshu.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.tianshu.model.registry.DdcServiceKey;
import top.egon.cola.component.tianshu.model.registry.DdcServiceKind;
import top.egon.cola.component.tianshu.model.registry.DdcServiceQuery;
import top.egon.cola.component.tianshu.redis.DdcRedisClientFactory;
import top.egon.cola.component.tianshu.redis.DdcRedisKeys;
import top.egon.cola.component.tianshu.service.registry.DdcRegistrySnapshotLoader;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcTenantRedisIT {

    @Test
    void tenantAclAllowsOwnKeysAndTopicsButRejectsAnotherTenant() {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("TIANSHU_AI_PERSISTENCE_IT_REDIS_ENABLED")),
                "requires an explicitly configured isolated Redis ACL target");
        long tenantId = positiveEnvironment("TIANSHU_AI_PERSISTENCE_IT_TENANT_ID");
        long foreignTenantId = positiveEnvironment("TIANSHU_AI_PERSISTENCE_IT_FOREIGN_TENANT_ID");
        assertThat(foreignTenantId).isNotEqualTo(tenantId);

        String address = requiredEnvironment("TIANSHU_AI_PERSISTENCE_IT_REDIS_ADDRESS");
        String username = requiredEnvironment("TIANSHU_AI_PERSISTENCE_IT_REDIS_USERNAME");
        String password = System.getenv("TIANSHU_AI_PERSISTENCE_IT_REDIS_PASSWORD");
        RedissonClient client = DdcRedisClientFactory.create("SINGLE", List.of(address), null, null, 0,
                password, username, 0);
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String ownKey = DdcRedisKeys.config(tenantId, "it-" + suffix, "test", "redis-it", "own");
        String foreignKey = DdcRedisKeys.config(foreignTenantId, "it-" + suffix, "test", "redis-it", "foreign");
        String ownTopic = DdcRedisKeys.topic(tenantId, "it-" + suffix, "test", "redis-it");
        String foreignTopic = DdcRedisKeys.topic(foreignTenantId, "it-" + suffix, "test", "redis-it");
        try {
            client.<String>getBucket(ownKey, StringCodec.INSTANCE).set("own-tenant");
            assertThat(client.<String>getBucket(ownKey, StringCodec.INSTANCE).get()).isEqualTo("own-tenant");
            assertThatThrownBy(() -> client.<String>getBucket(foreignKey, StringCodec.INSTANCE).set("forbidden"))
                    .isInstanceOf(RuntimeException.class);
            client.getTopic(ownTopic, StringCodec.INSTANCE).publish("own-tenant-event");
            assertThatThrownBy(() -> client.getTopic(foreignTopic, StringCodec.INSTANCE).publish("forbidden"))
                    .isInstanceOf(RuntimeException.class);
        } finally {
            client.<String>getBucket(ownKey, StringCodec.INSTANCE).delete();
            client.shutdown();
        }
    }

    @Test
    void periodicSnapshotReconciliationConvergesWithoutARedisNotification() throws Exception {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("TIANSHU_AI_PERSISTENCE_IT_REDIS_ENABLED")),
                "requires an explicitly configured isolated Redis ACL target");
        long tenantId = positiveEnvironment("TIANSHU_AI_PERSISTENCE_IT_TENANT_ID");
        String address = requiredEnvironment("TIANSHU_AI_PERSISTENCE_IT_REDIS_ADDRESS");
        RedissonClient client = DdcRedisClientFactory.create("SINGLE", List.of(address), null, null, 0,
                System.getenv("TIANSHU_AI_PERSISTENCE_IT_REDIS_PASSWORD"),
                requiredEnvironment("TIANSHU_AI_PERSISTENCE_IT_REDIS_USERNAME"), 0);
        DdcServiceQuery query = new DdcServiceQuery("step24-it", "test", "redis-it",
                DdcServiceKind.HTTP_PROVIDER, "http", null, null, null);
        DdcServiceKey serviceKey = new DdcServiceKey("step24-it", "test", "redis-it",
                DdcServiceKind.HTTP_PROVIDER, "reconcile-fixture", "default", "1.0.0", "http");
        AtomicInteger loads = new AtomicInteger();
        DdcRegistrySnapshotLoader loader = new DdcRegistrySnapshotLoader() {
            @Override
            public top.egon.cola.component.tianshu.model.registry.DdcServiceSnapshot getInstances(
                    DdcServiceKey ignored) {
                throw new UnsupportedOperationException("catalog reconciliation does not load instances");
            }

            @Override
            public DdcServiceCatalogSnapshot getServiceKeys(DdcServiceQuery requested) {
                int revision = loads.incrementAndGet();
                return new DdcServiceCatalogSnapshot(requested, revision,
                        revision == 1 ? List.of() : List.of(serviceKey), Instant.now());
            }
        };
        CountDownLatch reconciled = new CountDownLatch(2);
        List<Long> revisions = new CopyOnWriteArrayList<>();
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            DdcRegistrySubscriptionCoordinator coordinator = new DdcRegistrySubscriptionCoordinator(
                    loader, client, tenantId, new ObjectMapper(),
                    new ValidationUtils(validatorFactory.getValidator()), 1);
            try {
                coordinator.subscribeServices(query, snapshot -> {
                    revisions.add(snapshot.revision());
                    reconciled.countDown();
                });
                assertThat(reconciled.await(8, TimeUnit.SECONDS)).isTrue();
                assertThat(revisions).contains(1L, 2L);
            } finally {
                coordinator.close();
            }
        } finally {
            client.shutdown();
        }
    }

    private static long positiveEnvironment(String name) {
        long value = Long.parseLong(requiredEnvironment(name));
        if (value <= 0) throw new IllegalStateException(name + " must be positive");
        return value;
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " is required for this IT");
        return value;
    }
}
