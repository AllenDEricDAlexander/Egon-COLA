package top.egon.cola.component.gateway.engine.discovery;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.core.provider.ProviderHealthState;
import top.egon.cola.component.gateway.core.provider.ProviderCatalogSnapshot;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.provider.ProviderRegistryState;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.core.provider.ProviderServiceRegistry;
import top.egon.cola.component.gateway.core.provider.ProviderServiceSnapshot;
import top.egon.cola.component.gateway.core.provider.ProviderSubscription;
import top.egon.cola.component.gateway.core.provider.ProviderQuery;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProviderActiveHealthMonitorTest {

    @Test
    void probesEveryRegisteredInstanceAndRecordsProtocolResult() {
        ProviderServiceKey httpKey = key(
                "orders",
                ProviderProtocolType.HTTP
        );
        ProviderServiceKey rpcKey = key(
                "inventory",
                ProviderProtocolType.RPC
        );
        ProviderDirectory directory = new ProviderDirectory(
                registry(Map.of(
                        httpKey,
                        snapshot(httpKey, provider(httpKey, "http")),
                        rpcKey,
                        snapshot(rpcKey, provider(rpcKey, "rpc"))
                )),
                Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)
        );
        directory.activate(Set.of(httpKey, rpcKey));
        ActiveHealthTracker tracker = new ActiveHealthTracker(1, 1);
        AtomicInteger httpCalls = new AtomicInteger();
        AtomicInteger rpcCalls = new AtomicInteger();
        ProviderActiveHealthMonitor monitor =
                new ProviderActiveHealthMonitor(
                        directory,
                        Map.of(
                                ProviderProtocolType.HTTP,
                                (instance, policy) -> {
                                    httpCalls.incrementAndGet();
                                    return Mono.just(true);
                                },
                                ProviderProtocolType.RPC,
                                (instance, policy) -> {
                                    rpcCalls.incrementAndGet();
                                    return Mono.just(false);
                                }
                        ),
                        tracker,
                        ActiveHealthProbePolicy.defaults()
                );

        monitor.probeOnce().block(Duration.ofSeconds(1));

        assertEquals(1, httpCalls.get());
        assertEquals(1, rpcCalls.get());
        assertEquals(
                ProviderHealthState.HEALTHY,
                tracker.snapshot("http:lease").state()
        );
        assertEquals(
                ProviderHealthState.UNHEALTHY,
                tracker.snapshot("rpc:lease").state()
        );
        monitor.close();
        directory.close();
    }

    private ProviderServiceRegistry registry(
            Map<ProviderServiceKey, ProviderServiceSnapshot> snapshots) {
        return new ProviderServiceRegistry() {
            @Override
            public ProviderCatalogSnapshot getServiceKeys(
                    ProviderQuery query) {
                throw new UnsupportedOperationException();
            }

            @Override
            public ProviderServiceSnapshot getInstances(
                    ProviderServiceKey serviceKey) {
                return snapshots.get(serviceKey);
            }

            @Override
            public ProviderSubscription subscribeServices(
                    ProviderQuery query,
                    ProviderCatalogListener listener) {
                throw new UnsupportedOperationException();
            }

            @Override
            public ProviderSubscription subscribe(
                    ProviderServiceKey serviceKey,
                    ProviderSnapshotListener listener) {
                return new ProviderSubscription() {
                    @Override
                    public boolean active() {
                        return true;
                    }

                    @Override
                    public void close() {
                    }
                };
            }
        };
    }

    private ProviderServiceSnapshot snapshot(
            ProviderServiceKey key,
            ProviderInstance instance) {
        return new ProviderServiceSnapshot(
                key,
                1,
                Instant.EPOCH,
                List.of(instance)
        );
    }

    private ProviderInstance provider(
            ProviderServiceKey key,
            String id) {
        return new ProviderInstance(
                key,
                id,
                "lease",
                "127.0.0.1",
                8080,
                false,
                Map.of(),
                Instant.EPOCH.plusSeconds(60),
                ProviderRegistryState.REGISTERED,
                ProviderHealthState.UNKNOWN,
                ProviderHealthState.UNKNOWN
        );
    }

    private ProviderServiceKey key(
            String service,
            ProviderProtocolType protocol) {
        return new ProviderServiceKey(
                "test-biz",
                "test-app",
                "local",
                "default",
                protocol,
                service,
                "default",
                "v1",
                protocol == ProviderProtocolType.HTTP ? "http" : "grpc"
        );
    }
}
