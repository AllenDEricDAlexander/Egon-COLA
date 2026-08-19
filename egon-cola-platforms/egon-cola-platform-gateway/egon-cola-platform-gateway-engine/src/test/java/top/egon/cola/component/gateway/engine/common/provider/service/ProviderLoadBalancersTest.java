package top.egon.cola.component.gateway.engine.common.provider.service;

import top.egon.cola.component.gateway.engine.common.provider.domain.LoadBalancerType;
import top.egon.cola.component.gateway.engine.common.provider.domain.ProviderSelectionHandle;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.core.provider.ProviderHealthState;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.provider.ProviderRegistryState;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProviderLoadBalancersTest {

    @Test
    void roundRobinIsStableAndEmptyCandidatesFailFast() {
        ProviderLoadBalancer balancer = ProviderLoadBalancers.create(
                LoadBalancerType.ROUND_ROBIN
        );
        List<ProviderInstance> providers = List.of(
                provider("b", 100),
                provider("a", 100)
        );

        assertEquals("a", balancer.select(key(), providers)
                .instance().instanceId());
        assertEquals("b", balancer.select(key(), providers)
                .instance().instanceId());
        assertEquals("a", balancer.select(key(), providers)
                .instance().instanceId());
        assertThrows(
                IllegalStateException.class,
                () -> balancer.select(key(), List.of())
        );
    }

    @Test
    void smoothWeightedRoundRobinHonorsProviderWeights() {
        ProviderLoadBalancer balancer = ProviderLoadBalancers.create(
                LoadBalancerType.SMOOTH_WEIGHTED_ROUND_ROBIN
        );
        List<String> selected = new ArrayList<>();
        for (int index = 0; index < 4; index++) {
            selected.add(balancer.select(
                    key(),
                    List.of(provider("a", 3), provider("b", 1))
            ).instance().instanceId());
        }

        assertEquals(3, selected.stream().filter("a"::equals).count());
        assertEquals(1, selected.stream().filter("b"::equals).count());
    }

    @Test
    void leastInFlightReleasesCounterExactlyOnce() {
        ProviderLoadBalancer balancer = ProviderLoadBalancers.create(
                LoadBalancerType.LEAST_IN_FLIGHT
        );
        ProviderSelectionHandle first = balancer.select(
                key(),
                List.of(provider("a", 1), provider("b", 1))
        );
        ProviderSelectionHandle second = balancer.select(
                key(),
                List.of(provider("a", 1), provider("b", 1))
        );

        assertEquals("a", first.instance().instanceId());
        assertEquals("b", second.instance().instanceId());
        first.close();
        first.close();
        second.close();
        assertEquals("a", balancer.select(
                key(),
                List.of(provider("a", 1), provider("b", 1))
        ).instance().instanceId());
    }

    private ProviderInstance provider(String instanceId, int weight) {
        return new ProviderInstance(
                key(),
                instanceId,
                "lease",
                "127.0.0.1",
                8080,
                false,
                Map.of("gateway.weight", Integer.toString(weight)),
                Instant.now().plusSeconds(60),
                ProviderRegistryState.REGISTERED,
                ProviderHealthState.HEALTHY,
                ProviderHealthState.HEALTHY
        );
    }

    private ProviderServiceKey key() {
        return new ProviderServiceKey(
                "test-biz",
                "test-app",
                "local",
                "default",
                ProviderProtocolType.HTTP,
                "orders",
                "default",
                "v1",
                "http"
        );
    }
}
