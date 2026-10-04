package top.egon.cola.component.tianshu.listener.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import top.egon.cola.component.tianshu.service.registry.DdcRegistrySnapshotLoader;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import org.junit.jupiter.api.Test;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.api.listener.MessageListener;
import org.slf4j.MDC;
import top.egon.cola.component.tianshu.redis.DdcRedisKeys;
import top.egon.cola.component.tianshu.model.registry.DdcServiceKind;
import top.egon.cola.component.tianshu.model.registry.DdcRegistryEvent;
import top.egon.cola.component.tianshu.model.event.DdcRegistryRevisionEvent;
import top.egon.cola.component.tianshu.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.tianshu.model.registry.DdcServiceInstance;
import top.egon.cola.component.tianshu.model.registry.DdcServiceKey;
import top.egon.cola.component.tianshu.model.registry.DdcServiceQuery;
import top.egon.cola.component.tianshu.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.tianshu.model.tenant.DdcTenantMessageDTO;
import top.egon.cola.component.tianshu.api.registry.DdcRegistrySubscription;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcRegistrySubscriptionCoordinatorTest {

    private static final long TENANT_ID = 101L;

    private static final ValidationUtils VALIDATION_UTILS = new ValidationUtils(
            Validation.buildDefaultValidatorFactory().getValidator());

    private static final Instant NOW = Instant.parse("2026-07-24T12:00:00Z");

    @Test
    void subscribesBeforeInitialPullAndRefreshesFromRelevantEvent() throws Exception {
        List<String> order = Collections.synchronizedList(new ArrayList<>());
        AtomicReference<DdcServiceSnapshot> snapshot =
                new AtomicReference<>(snapshot(1L, "instance-2"));
        DdcRegistrySnapshotLoader loader = loader(snapshot, order);
        TopicFixture topic = topic(order);
        CountDownLatch refreshed = new CountDownLatch(1);
        List<DdcServiceSnapshot> observed = new ArrayList<>();
        DdcRegistrySubscriptionCoordinator manager = manager(loader, topic.redisson());

        DdcRegistrySubscription subscription = manager.subscribe(SERVICE_KEY, value -> {
            observed.add(value);
            if (value.revision() == 2L) {
                refreshed.countDown();
            }
        });
        snapshot.set(snapshot(2L, "instance-1"));
        topic.listener().get().onMessage("topic", eventJson(2L));

        assertThat(refreshed.await(2, TimeUnit.SECONDS)).isTrue();
        List<String> orderSnapshot;
        synchronized (order) {
            orderSnapshot = List.copyOf(order);
        }
        assertThat(orderSnapshot.subList(0, 2)).containsExactly("subscribe", "pull");
        verify(topic.redisson()).getTopic(
                DdcRedisKeys.registryTopic(101L,
                        "pay-biz", "dev", "orders-app", DdcServiceKind.RPC_PROVIDER, "grpc"
                ),
                StringCodec.INSTANCE
        );
        assertThat(observed.get(1).instances())
                .extracting(DdcServiceInstance::instanceId)
                .containsExactly("instance-1");
        assertThatThrownByMutation(observed.get(1));

        subscription.close();
        subscription.close();
        manager.close();
    }

    @Test
    void listenerFailureDoesNotStopLaterRefresh() throws Exception {
        AtomicReference<DdcServiceSnapshot> snapshot =
                new AtomicReference<>(snapshot(1L, "instance-1"));
        DdcRegistrySnapshotLoader loader = loader(snapshot, new ArrayList<>());
        TopicFixture topic = topic(new ArrayList<>());
        CountDownLatch calls = new CountDownLatch(2);
        DdcRegistrySubscriptionCoordinator manager = manager(loader, topic.redisson());
        manager.subscribe(SERVICE_KEY, value -> {
            calls.countDown();
            throw new IllegalStateException("listener failed");
        });
        snapshot.set(snapshot(2L, "instance-2"));

        topic.listener().get().onMessage("topic", eventJson(2L));

        assertThat(calls.await(2, TimeUnit.SECONDS)).isTrue();
        manager.close();
    }

    @Test
    void rejectsWrongTenantEnvelopeRestoresCallbackContextAndIgnoresOlderSnapshots() {
        AtomicReference<DdcServiceSnapshot> snapshot =
                new AtomicReference<>(snapshot(2L, "instance-2"));
        AtomicInteger loads = new AtomicInteger();
        DdcRegistrySnapshotLoader loader = new DelegatingSnapshotLoader() {
            @Override
            public DdcServiceSnapshot getInstances(DdcServiceKey serviceKey) {
                loads.incrementAndGet();
                return snapshot.get();
            }
        };
        TopicFixture topic = topic(new ArrayList<>());
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        ScheduledFuture<?> reconcile = mock(ScheduledFuture.class);
        List<Runnable> queued = new java.util.concurrent.CopyOnWriteArrayList<>();
        doReturn(reconcile).when(scheduler).scheduleWithFixedDelay(
                any(Runnable.class), anyLong(), anyLong(), eq(TimeUnit.MILLISECONDS));
        doAnswer(invocation -> {
            queued.add(invocation.getArgument(0));
            return null;
        }).when(scheduler).execute(any(Runnable.class));

        List<Long> revisions = new java.util.concurrent.CopyOnWriteArrayList<>();
        List<String> callbackTenants = new java.util.concurrent.CopyOnWriteArrayList<>();
        DdcRegistrySubscriptionCoordinator manager = new DdcRegistrySubscriptionCoordinator(
                loader, topic.redisson(), TENANT_ID, new ObjectMapper(),
                VALIDATION_UTILS,
                Clock.fixed(NOW, ZoneOffset.UTC), scheduler, 60_000L);
        MDC.put("tenantId", "999");
        MDC.put("userId", "system:outer");
        manager.subscribe(SERVICE_KEY, value -> {
            revisions.add(value.revision());
            callbackTenants.add(MDC.get("tenantId"));
        });

        assertThat(revisions).containsExactly(2L);
        assertThat(callbackTenants).containsExactly("101");
        assertThat(MDC.get("tenantId")).isEqualTo("999");
        assertThat(MDC.get("userId")).isEqualTo("system:outer");

        snapshot.set(snapshot(3L, "instance-3"));
        topic.listener().get().onMessage("topic", eventJson(TENANT_ID, 3L));
        assertThat(queued).hasSize(1);
        queued.removeFirst().run();
        assertThat(revisions).containsExactly(2L, 3L);
        assertThat(callbackTenants).containsExactly("101", "101");
        assertThat(MDC.get("tenantId")).isEqualTo("999");

        topic.listener().get().onMessage("topic", eventJson(0L, 4L));
        assertThat(queued).isEmpty();

        snapshot.set(snapshot(1L, "stale-instance"));
        topic.listener().get().onMessage("topic", eventJson(TENANT_ID, 1L));
        queued.removeFirst().run();
        assertThat(revisions).containsExactly(2L, 3L);

        snapshot.set(snapshot(4L, "other-tenant-instance"));
        topic.listener().get().onMessage("topic", eventJson(202L, 4L));
        assertThat(queued).isEmpty();
        assertThat(loads).hasValue(3);
        assertThat(revisions).containsExactly(2L, 3L);

        manager.close();
        assertThat(MDC.get("tenantId")).isEqualTo("999");
        MDC.clear();
    }

    @Test
    void reconciliationRecoversDroppedEvents() throws Exception {
        AtomicReference<DdcServiceSnapshot> snapshot =
                new AtomicReference<>(snapshot(1L, "instance-1"));
        DdcRegistrySnapshotLoader loader = loader(snapshot, new ArrayList<>());
        TopicFixture topic = topic(new ArrayList<>());
        CountDownLatch reconciled = new CountDownLatch(1);
        DdcRegistrySubscriptionCoordinator manager = manager(loader, topic.redisson());
        manager.subscribe(SERVICE_KEY, value -> {
            if (value.revision() == 2L) {
                reconciled.countDown();
            }
        });

        snapshot.set(snapshot(2L, "instance-2"));

        assertThat(reconciled.await(2, TimeUnit.SECONDS)).isTrue();
        manager.close();
    }

    @Test
    void typedRevisionEnvelopeRefreshesWithoutRegressingAndLegacyEnvelopeRemainsSupported()
            throws Exception {
        AtomicReference<DdcServiceSnapshot> snapshot =
                new AtomicReference<>(snapshot(2L, "instance-2"));
        AtomicInteger loads = new AtomicInteger();
        DdcRegistrySnapshotLoader loader = new DelegatingSnapshotLoader() {
            @Override
            public DdcServiceSnapshot getInstances(DdcServiceKey serviceKey) {
                loads.incrementAndGet();
                return snapshot.get();
            }
        };
        TopicFixture topic = topic(new ArrayList<>());
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        ScheduledFuture<?> reconcile = mock(ScheduledFuture.class);
        List<Runnable> queued = new java.util.concurrent.CopyOnWriteArrayList<>();
        doReturn(reconcile).when(scheduler).scheduleWithFixedDelay(
                any(Runnable.class), anyLong(), anyLong(), eq(TimeUnit.MILLISECONDS));
        doAnswer(invocation -> {
            queued.add(invocation.getArgument(0));
            return null;
        }).when(scheduler).execute(any(Runnable.class));
        List<Long> revisions = new java.util.concurrent.CopyOnWriteArrayList<>();
        DdcRegistrySubscriptionCoordinator manager = new DdcRegistrySubscriptionCoordinator(
                loader, topic.redisson(), TENANT_ID, new ObjectMapper(), VALIDATION_UTILS,
                Clock.fixed(NOW, ZoneOffset.UTC), scheduler, 60_000L);
        manager.subscribe(SERVICE_KEY, value -> revisions.add(value.revision()));

        snapshot.set(snapshot(3L, "instance-3"));
        topic.listener().get().onMessage("topic", revisionEventJson(TENANT_ID, TENANT_ID, 3L, 2L));
        assertThat(queued).hasSize(1);
        queued.removeFirst().run();
        assertThat(revisions).containsExactly(2L, 3L);

        topic.listener().get().onMessage("topic", revisionEventJson(TENANT_ID, 202L, 4L, 3L));
        assertThat(queued).isEmpty();

        snapshot.set(snapshot(2L, "stale-instance"));
        topic.listener().get().onMessage("topic", revisionEventJson(TENANT_ID, TENANT_ID, 2L, 1L));
        assertThat(queued).hasSize(1);
        queued.removeFirst().run();
        assertThat(revisions).containsExactly(2L, 3L);

        snapshot.set(snapshot(4L, "instance-4"));
        topic.listener().get().onMessage("topic", eventJson(TENANT_ID, 4L));
        assertThat(queued).hasSize(1);
        queued.removeFirst().run();
        assertThat(revisions).containsExactly(2L, 3L, 4L);
        assertThat(loads).hasValue(4);

        manager.close();
    }

    @Test
    void expiresCachedInstancesWhenAdminIsUnavailable() throws Exception {
        MutableClock clock = new MutableClock(NOW);
        AtomicBoolean unavailable = new AtomicBoolean();
        DdcServiceSnapshot initial = snapshot(1L, "instance-1");
        DdcRegistrySnapshotLoader loader = new DelegatingSnapshotLoader() {
            @Override
            public DdcServiceSnapshot getInstances(DdcServiceKey serviceKey) {
                if (unavailable.get()) {
                    throw new IllegalStateException("admin unavailable");
                }
                return initial;
            }
        };
        TopicFixture topic = topic(new ArrayList<>());
        CountDownLatch expired = new CountDownLatch(1);
        DdcRegistrySubscriptionCoordinator manager = new DdcRegistrySubscriptionCoordinator(
                loader,
                topic.redisson(),
                TENANT_ID,
                new ObjectMapper(),
                VALIDATION_UTILS,
                clock,
                Executors.newSingleThreadScheduledExecutor(),
                20
        );
        manager.subscribe(SERVICE_KEY, value -> {
            if (value.instances().isEmpty()) {
                expired.countDown();
            }
        });

        unavailable.set(true);
        clock.advance(Duration.ofSeconds(31));

        assertThat(expired.await(2, TimeUnit.SECONDS)).isTrue();
        manager.close();
    }

    private DdcRegistrySubscriptionCoordinator manager(DdcRegistrySnapshotLoader loader,
                                                       RedissonClient redisson) {
        return new DdcRegistrySubscriptionCoordinator(
                loader,
                redisson,
                TENANT_ID,
                new ObjectMapper(),
                VALIDATION_UTILS,
                Clock.fixed(NOW, ZoneOffset.UTC),
                Executors.newSingleThreadScheduledExecutor(),
                20
        );
    }

    private TopicFixture topic(List<String> order) {
        RedissonClient redisson = mock(RedissonClient.class);
        RTopic topic = mock(RTopic.class);
        AtomicReference<MessageListener<String>> listener = new AtomicReference<>();
        when(redisson.getTopic(anyString(), eq(StringCodec.INSTANCE))).thenReturn(topic);
        doAnswer(invocation -> {
            order.add("subscribe");
            listener.set(invocation.getArgument(1));
            return 1;
        }).when(topic).addListener(eq(String.class), any(MessageListener.class));
        return new TopicFixture(redisson, listener);
    }

    private String eventJson(long serviceRevision) {
        return eventJson(TENANT_ID, serviceRevision);
    }

    private String eventJson(long tenantId, long serviceRevision) {
        return """
                {
                  "tenantId":%d,
                  "payload":{
                    "serviceKey":{
                      "bizCode":"pay-biz",
                      "env":"dev",
                      "appCode":"orders-app",
                      "serviceKind":"RPC_PROVIDER",
                      "serviceName":"order.v1.OrderQueryService",
                      "group":"default",
                      "version":"1.0.0",
                      "protocol":"grpc"
                    },
                    "serviceRevision":%d,
                    "catalogRevision":1
                  }
                }
                """.formatted(tenantId, serviceRevision);
    }

    private String revisionEventJson(
            long envelopeTenantId,
            long eventTenantId,
            long serviceRevision,
            long catalogRevision) throws Exception {
        DdcRegistryEvent registryEvent = new DdcRegistryEvent(
                SERVICE_KEY, serviceRevision, catalogRevision);
        DdcRegistryRevisionEvent revisionEvent = new DdcRegistryRevisionEvent(
                DdcRegistryRevisionEvent.eventId(eventTenantId, registryEvent),
                eventTenantId,
                "1",
                registryEvent);
        return new ObjectMapper().writeValueAsString(
                new DdcTenantMessageDTO<>(envelopeTenantId, revisionEvent));
    }

    private DdcRegistrySnapshotLoader loader(AtomicReference<DdcServiceSnapshot> snapshot,
                                             List<String> order) {
        return new DelegatingSnapshotLoader() {
            @Override
            public DdcServiceSnapshot getInstances(DdcServiceKey serviceKey) {
                order.add("pull");
                return snapshot.get();
            }
        };
    }

    private abstract static class DelegatingSnapshotLoader
            implements DdcRegistrySnapshotLoader {

        @Override
        public DdcServiceCatalogSnapshot getServiceKeys(DdcServiceQuery query) {
            throw new UnsupportedOperationException();
        }

    }

    private DdcServiceSnapshot snapshot(long revision, String instanceId) {
        DdcServiceInstance instance = new DdcServiceInstance(
                instanceId,
                "lease-1",
                SERVICE_KEY,
                "127.0.0.1",
                19090,
                false,
                java.util.Map.of(),
                30,
                10,
                NOW,
                NOW,
                NOW.plusSeconds(30),
                "ONLINE",
                revision,
                "resource-order",
                1L,
                "kid-test",
                NOW.plusSeconds(20)
        );
        return new DdcServiceSnapshot(SERVICE_KEY, revision, List.of(instance), NOW);
    }

    private void assertThatThrownByMutation(DdcServiceSnapshot snapshot) {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> snapshot.instances().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private record TopicFixture(
            RedissonClient redisson,
            AtomicReference<MessageListener<String>> listener
    ) {
    }

    private static final class MutableClock extends Clock {

        private final AtomicReference<Instant> instant;

        private MutableClock(Instant instant) {
            this.instant = new AtomicReference<>(instant);
        }

        private void advance(Duration duration) {
            instant.updateAndGet(value -> value.plus(duration));
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant.get();
        }
    }

    private static final DdcServiceKey SERVICE_KEY = new DdcServiceKey(
            "pay-biz",
            "dev",
            "orders-app",
            DdcServiceKind.RPC_PROVIDER,
            "order.v1.OrderQueryService",
            "default",
            "1.0.0",
            "grpc"
    );
}
