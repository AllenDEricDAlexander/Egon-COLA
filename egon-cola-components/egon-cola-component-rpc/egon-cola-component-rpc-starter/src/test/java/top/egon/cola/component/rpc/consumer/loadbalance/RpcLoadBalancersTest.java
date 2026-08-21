package top.egon.cola.component.rpc.consumer.loadbalance;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.annotation.LoadBalance;
import top.egon.cola.component.rpc.consumer.channel.RpcEndpoint;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RpcLoadBalancersTest {

    @Test
    void factoryCoversEverySelectableAlgorithmAndRejectsInherit() {
        RpcLoadBalancers factory = new RpcLoadBalancers();
        for (LoadBalance algorithm : LoadBalance.values()) {
            if (algorithm == LoadBalance.INHERIT) {
                assertThatThrownBy(() -> factory.loadBalancer(algorithm))
                        .isInstanceOf(EgonRpcException.class)
                        .satisfies(error -> assertThat(((EgonRpcException) error).getCode())
                                .isEqualTo(EgonRpcErrorCode.RPC_INVALID_CONTRACT));
            } else {
                assertThat(factory.loadBalancer(algorithm)).isNotNull();
            }
        }
    }

    @Test
    void roundRobinUsesStableEndpointOrderAndExclusion() {
        List<TestEndpoint> endpoints = List.of(
                endpoint("node-c", 8083, 100),
                endpoint("node-a", 8081, 100),
                endpoint("node-b", 8082, 100));
        RpcLoadBalancer loadBalancer = new RpcLoadBalancers().loadBalancer(LoadBalance.ROUND_ROBIN);
        RpcLoadBalanceContext context = context("service.Echo", endpoints, Set.of(), null, 1);

        assertThat(List.of(
                loadBalancer.select(context).host(),
                loadBalancer.select(context).host(),
                loadBalancer.select(context).host()))
                .containsExactly("node-a", "node-b", "node-c");
        assertThat(loadBalancer.select(context("service.Echo", endpoints,
                Set.of(key(endpoints.get(0))), null, 1)).host())
                .isEqualTo("node-b");
    }

    @Test
    void weightedRandomHonorsRelativeWeightsWithSeededRandom() {
        List<TestEndpoint> endpoints = List.of(
                endpoint("node-a", 8081, 1),
                endpoint("node-b", 8082, 3),
                endpoint("node-c", 8083, 6));
        RpcLoadBalancer loadBalancer = new RpcLoadBalancers(160, new Random(7))
                .loadBalancer(LoadBalance.WEIGHTED_RANDOM);
        Map<String, Long> counts = new HashMap<>();
        RpcLoadBalanceContext context = context("weighted", endpoints, Set.of(), null, 1);

        for (int index = 0; index < 10_000; index++) {
            String host = loadBalancer.select(context).host();
            counts.merge(host, 1L, Long::sum);
        }

        assertThat(counts.get("node-a") / 10_000.0).isBetween(0.07, 0.13);
        assertThat(counts.get("node-b") / 10_000.0).isBetween(0.25, 0.35);
        assertThat(counts.get("node-c") / 10_000.0).isBetween(0.55, 0.65);
    }

    @Test
    void smoothWeightedRoundRobinDistributesOneCycleByWeight() {
        List<TestEndpoint> endpoints = List.of(
                endpoint("node-a", 8081, 1),
                endpoint("node-b", 8082, 3),
                endpoint("node-c", 8083, 6));
        RpcLoadBalancer loadBalancer = new RpcLoadBalancers().loadBalancer(
                LoadBalance.SMOOTH_WEIGHTED_ROUND_ROBIN);
        RpcLoadBalanceContext context = context("swrr", endpoints, Set.of(), null, 1);
        Map<String, Long> counts = selectCounts(loadBalancer, context, 10);

        assertThat(counts).containsEntry("node-a", 1L)
                .containsEntry("node-b", 3L)
                .containsEntry("node-c", 6L);
    }

    @Test
    void consistentHashKeepsMostKeysWhenOneMemberIsAdded() {
        List<TestEndpoint> before = List.of(
                endpoint("node-a", 8081, 100),
                endpoint("node-b", 8082, 100),
                endpoint("node-c", 8083, 100));
        List<TestEndpoint> after = new ArrayList<>(before);
        after.add(endpoint("node-d", 8084, 100));
        RpcLoadBalancer loadBalancer = new RpcLoadBalancers(160, new Random(7))
                .loadBalancer(LoadBalance.CONSISTENT_HASH);
        Map<String, String> first = new HashMap<>();
        Map<String, String> second = new HashMap<>();
        for (int index = 0; index < 10_000; index++) {
            String key = "affinity-" + index;
            first.put(key, loadBalancer.select(context(
                    "hash", before, Set.of(), digest(key), 1)).host());
            second.put(key, loadBalancer.select(context(
                    "hash", after, Set.of(), digest(key), 2)).host());
        }

        long changed = first.keySet().stream()
                .filter(key -> !first.get(key).equals(second.get(key)))
                .count();
        assertThat(changed).isLessThan(4_500L);
    }

    @Test
    void leastInFlightTracksReleaseAndRejectsMissingAffinityForHash() {
        List<TestEndpoint> endpoints = List.of(
                endpoint("node-a", 8081, 100),
                endpoint("node-b", 8082, 100));
        RpcLoadBalancer loadBalancer = new RpcLoadBalancers().loadBalancer(
                LoadBalance.LEAST_IN_FLIGHT);
        RpcLoadBalanceContext context = context("lif", endpoints, Set.of(), null, 1);
        RpcEndpoint first = loadBalancer.select(context);
        RpcEndpoint second = loadBalancer.select(context);
        assertThat(first).isNotEqualTo(second);
        loadBalancer.release(context, first);
        assertThat(loadBalancer.select(context)).isEqualTo(first);

        RpcLoadBalancer hash = new RpcLoadBalancers().loadBalancer(LoadBalance.CONSISTENT_HASH);
        assertThatThrownBy(() -> hash.select(context))
                .isInstanceOf(EgonRpcException.class)
                .satisfies(error -> assertThat(((EgonRpcException) error).getCode())
                        .isEqualTo(EgonRpcErrorCode.RPC_INVALID_REQUEST));
    }

    @Test
    void stateCanBeRemovedByQueryIdentity() {
        List<TestEndpoint> endpoints = List.of(
                endpoint("node-a", 8081, 100),
                endpoint("node-b", 8082, 100));
        RpcLoadBalancer loadBalancer = new RpcLoadBalancers().loadBalancer(LoadBalance.ROUND_ROBIN);
        RpcLoadBalanceContext context = context("cleanup", endpoints, Set.of(), null, 1);
        assertThat(loadBalancer.select(context).host()).isEqualTo("node-a");
        assertThat(loadBalancer.select(context).host()).isEqualTo("node-b");
        loadBalancer.remove("cleanup");
        assertThat(loadBalancer.select(context).host()).isEqualTo("node-a");
    }

    private static Map<String, Long> selectCounts(
            RpcLoadBalancer loadBalancer,
            RpcLoadBalanceContext context,
            int count) {
        Map<String, Long> result = new HashMap<>();
        for (int index = 0; index < count; index++) {
            result.merge(loadBalancer.select(context).host(), 1L, Long::sum);
        }
        return result;
    }

    private static TestEndpoint endpoint(String host, int port, int weight) {
        return new TestEndpoint(host, port, weight);
    }

    private static String key(TestEndpoint endpoint) {
        return endpoint.host() + ':' + endpoint.port() + ':' + endpoint.secure();
    }

    private static RpcLoadBalanceContext context(
            String queryIdentity,
            List<TestEndpoint> endpoints,
            Set<String> excluded,
            byte[] affinityDigest,
            long revision) {
        return new RpcLoadBalanceContext(
                queryIdentity,
                "sample.Service",
                "sample.Service/Echo",
                "request",
                endpoints,
                excluded,
                affinityDigest,
                revision);
    }

    private static byte[] digest(String value) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
    }

    private record TestEndpoint(String host, int port, int weight)
            implements RpcEndpoint {

        @Override
        public boolean secure() {
            return false;
        }
    }
}
