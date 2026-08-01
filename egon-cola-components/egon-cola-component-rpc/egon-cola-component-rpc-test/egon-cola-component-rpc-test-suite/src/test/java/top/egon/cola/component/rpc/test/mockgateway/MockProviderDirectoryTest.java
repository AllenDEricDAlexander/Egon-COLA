package top.egon.cola.component.rpc.test.mockgateway;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.model.enums.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceInstance;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;
import top.egon.cola.component.ddc.registry.DdcRegistrySubscription;
import top.egon.cola.component.ddc.registry.DdcServiceRegistryClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class MockProviderDirectoryTest {

    @Test
    void shouldDiscoverReplaceAndRemoveProviderLeases() {
        DirectoryRegistry registry = new DirectoryRegistry();
        java.util.concurrent.atomic.AtomicReference<
                java.util.Collection<MockProviderEndpoint>> retained =
                new java.util.concurrent.atomic.AtomicReference<>(List.of());
        MockProviderDirectory directory = new MockProviderDirectory(
                registry,
                "test",
                retained::set
        );

        directory.start();

        assertThat(directory.cluster(registry.key).endpoints())
                .extracting(MockProviderEndpoint::instanceId)
                .containsExactly("provider-a", "provider-b");

        registry.publish(new DdcServiceSnapshot(
                registry.key,
                2,
                List.of(registry.instance("provider-b", "lease-b2", 19092)),
                Instant.now()
        ));

        assertThat(directory.cluster(registry.key).revision()).isEqualTo(2);
        assertThat(directory.cluster(registry.key).endpoints())
                .extracting(MockProviderEndpoint::channelKey)
                .containsExactly("provider-b:lease-b2:127.0.0.1:19092:false");
        assertThat(retained.get()).hasSize(1);
        directory.close();
        assertThat(retained.get()).isEmpty();
    }

    private static final class DirectoryRegistry
            implements DdcServiceRegistryClient {

        private final DdcServiceKey key = new DdcServiceKey(
                "test-biz",
                "test",
                "test-app",
                DdcServiceKind.RPC_PROVIDER,
                "egon.rpc.test.v1.EchoService",
                "default",
                "1.0.0",
                "grpc"
        );

        private Consumer<DdcServiceSnapshot> instanceListener;

        @Override
        public DdcServiceCatalogSnapshot getServiceKeys(
                DdcServiceQuery query) {
            return new DdcServiceCatalogSnapshot(
                    query,
                    1,
                    List.of(key),
                    Instant.now()
            );
        }

        @Override
        public DdcServiceSnapshot getInstances(DdcServiceKey serviceKey) {
            return new DdcServiceSnapshot(
                    key,
                    1,
                    List.of(
                            instance("provider-b", "lease-b", 19091),
                            instance("provider-a", "lease-a", 19090)
                    ),
                    Instant.now()
            );
        }

        @Override
        public DdcRegistrySubscription subscribeServices(
                DdcServiceQuery query,
                Consumer<DdcServiceCatalogSnapshot> listener) {
            return () -> {
            };
        }

        @Override
        public DdcRegistrySubscription subscribe(
                DdcServiceKey serviceKey,
                Consumer<DdcServiceSnapshot> listener) {
            instanceListener = listener;
            return () -> instanceListener = null;
        }

        void publish(DdcServiceSnapshot snapshot) {
            instanceListener.accept(snapshot);
        }

        DdcServiceInstance instance(
                String instanceId,
                String leaseId,
                int port) {
            Instant now = Instant.now();
            return new DdcServiceInstance(
                    instanceId,
                    leaseId,
                    key,
                    "127.0.0.1",
                    port,
                    false,
                    Map.of(),
                    30,
                    10,
                    now,
                    now,
                    now.plusSeconds(30),
                    "UP",
                    1
            );
        }

        @Override
        public DdcLeaseSession register(DdcServiceRegistration registration) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DdcLeaseOperationResult heartbeat(
                String instanceId,
                String leaseId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DdcLeaseOperationResult deregister(
                String instanceId,
                String leaseId) {
            throw new UnsupportedOperationException();
        }
    }
}
