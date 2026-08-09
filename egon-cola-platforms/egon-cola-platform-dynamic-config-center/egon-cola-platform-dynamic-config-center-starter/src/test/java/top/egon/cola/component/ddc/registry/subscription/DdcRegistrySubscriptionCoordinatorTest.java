package top.egon.cola.component.ddc.registry.subscription;

import top.egon.cola.component.ddc.service.registry.DdcRegistrySnapshotLoader;

import org.junit.jupiter.api.Test;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.api.listener.MessageListener;
import top.egon.cola.component.ddc.transport.redis.DdcRedisKeys;
import top.egon.cola.component.ddc.model.registry.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcRegistryEvent;
import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceInstance;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.ddc.api.registry.DdcRegistrySubscription;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcRegistrySubscriptionCoordinatorTest {

    private static final Instant NOW = Instant.parse("2026-07-24T12:00:00Z");

    @Test
    void subscribesBeforeInitialPullAndRefreshesFromRelevantEvent() throws Exception {
        List<String> order = new ArrayList<>();
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
        assertThat(order.subList(0, 2)).containsExactly("subscribe", "pull");
        verify(topic.redisson()).getTopic(
                DdcRedisKeys.registryTopic(
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
        return """
                {
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
                """.formatted(serviceRevision);
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
                revision
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
