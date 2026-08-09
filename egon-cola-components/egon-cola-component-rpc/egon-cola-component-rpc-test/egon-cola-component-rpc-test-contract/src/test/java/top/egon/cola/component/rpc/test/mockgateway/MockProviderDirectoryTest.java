package top.egon.cola.component.rpc.test.mockgateway;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.context.RpcProcessIdentity;
import top.egon.cola.component.rpc.provider.RpcProviderLease;
import top.egon.cola.component.rpc.provider.RpcProviderLeaseIdentity;
import top.egon.cola.component.rpc.provider.RpcProviderRegistration;
import top.egon.cola.component.rpc.provider.RpcServiceIdentity;
import top.egon.cola.component.rpc.test.support.InMemoryRpcRegistryBackend;
import top.egon.cola.component.rpc.test.support.InMemoryRpcRegistryClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MockProviderDirectoryTest {

    private static final RpcServiceIdentity SERVICE =
            new RpcServiceIdentity(
                    "egon.rpc.test.v1.EchoService",
                    "default",
                    "1.0.0"
            );

    @Test
    void discoversReplacesAndRemovesProviderLeases() {
        InMemoryRpcRegistryClient registry = new InMemoryRpcRegistryClient(
                new InMemoryRpcRegistryBackend()
        );
        RpcProviderLease first = registry.register(registration(
                "provider-a",
                19090
        ));
        registry.register(registration("provider-b", 19091));
        java.util.concurrent.atomic.AtomicReference<
                java.util.Collection<MockProviderEndpoint>> retained =
                new java.util.concurrent.atomic.AtomicReference<>(List.of());
        MockProviderDirectory directory = new MockProviderDirectory(
                registry,
                "test",
                retained::set
        );

        directory.start();

        assertThat(directory.cluster(SERVICE).endpoints())
                .extracting(MockProviderEndpoint::instanceId)
                .containsExactly("provider-a", "provider-b");

        RpcProviderLease replacement = registry.register(registration(
                "provider-b",
                19092
        ));
        registry.deregister(new RpcProviderLeaseIdentity(
                SERVICE,
                first.instanceId(),
                first.leaseId()
        ));

        assertThat(directory.cluster(SERVICE).revision()).isPositive();
        assertThat(directory.cluster(SERVICE).endpoints())
                .extracting(MockProviderEndpoint::channelKey)
                .containsExactly(
                        "provider-b:" + replacement.leaseId()
                                + ":127.0.0.1:19092:false"
                );
        assertThat(retained.get()).hasSize(1);
        directory.close();
        assertThat(retained.get()).isEmpty();
    }

    private RpcProviderRegistration registration(
            String instanceId,
            int port) {
        return new RpcProviderRegistration(
                SERVICE,
                new RpcProcessIdentity(
                        "provider-test",
                        "test",
                        "127.0.0.1",
                        1,
                        instanceId
                ),
                "127.0.0.1",
                port,
                false,
                Map.of(),
                30,
                10
        );
    }
}
