package top.egon.cola.component.gateway.engine.balance;

import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

public final class ProviderLoadBalancers {

    private ProviderLoadBalancers() {
    }

    public static ProviderLoadBalancer create(LoadBalancerType type) {
        return switch (type) {
            case ROUND_ROBIN -> new RoundRobin();
            case SMOOTH_WEIGHTED_ROUND_ROBIN -> new SmoothWeightedRoundRobin();
            case RANDOM -> new Random();
            case LEAST_IN_FLIGHT -> new LeastInFlight();
        };
    }

    private static ProviderSelectionHandle handle(
            ProviderInstance selected) {
        return new ProviderSelectionHandle(selected, () -> {
        });
    }

    private static List<ProviderInstance> checked(
            List<ProviderInstance> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalStateException("GATEWAY_PROVIDER_UNAVAILABLE");
        }
        return candidates.stream()
                .sorted(Comparator.comparing(ProviderInstance::runtimeIdentity))
                .toList();
    }

    private static final class RoundRobin implements ProviderLoadBalancer {

        private final Map<ProviderServiceKey, AtomicInteger> sequences =
                new ConcurrentHashMap<>();

        @Override
        public ProviderSelectionHandle select(
                ProviderServiceKey serviceKey,
                List<ProviderInstance> candidates) {
            List<ProviderInstance> available = checked(candidates);
            int index = Math.floorMod(
                    sequences.computeIfAbsent(
                            serviceKey,
                            ignored -> new AtomicInteger()
                    ).getAndIncrement(),
                    available.size()
            );
            return handle(available.get(index));
        }
    }

    private static final class Random implements ProviderLoadBalancer {

        @Override
        public ProviderSelectionHandle select(
                ProviderServiceKey serviceKey,
                List<ProviderInstance> candidates) {
            List<ProviderInstance> available = checked(candidates);
            return handle(available.get(
                    ThreadLocalRandom.current().nextInt(available.size())
            ));
        }
    }

    private static final class SmoothWeightedRoundRobin
            implements ProviderLoadBalancer {

        private final Map<ProviderServiceKey, Map<String, Integer>> currents =
                new ConcurrentHashMap<>();

        @Override
        public synchronized ProviderSelectionHandle select(
                ProviderServiceKey serviceKey,
                List<ProviderInstance> candidates) {
            List<ProviderInstance> available = checked(candidates);
            Map<String, Integer> current = currents.computeIfAbsent(
                    serviceKey,
                    ignored -> new LinkedHashMap<>()
            );
            current.keySet().retainAll(
                    available.stream()
                            .map(ProviderInstance::runtimeIdentity)
                            .toList()
            );
            int totalWeight = 0;
            ProviderInstance selected = null;
            int best = Integer.MIN_VALUE;
            for (ProviderInstance candidate : available) {
                int next = current.getOrDefault(
                        candidate.runtimeIdentity(),
                        0
                ) + candidate.weight();
                current.put(candidate.runtimeIdentity(), next);
                totalWeight += candidate.weight();
                if (selected == null || next > best) {
                    selected = candidate;
                    best = next;
                }
            }
            String selectedIdentity = selected.runtimeIdentity();
            current.put(
                    selectedIdentity,
                    current.get(selectedIdentity) - totalWeight
            );
            return handle(selected);
        }
    }

    private static final class LeastInFlight implements ProviderLoadBalancer {

        private final Map<String, LongAdder> inFlight =
                new ConcurrentHashMap<>();

        @Override
        public ProviderSelectionHandle select(
                ProviderServiceKey serviceKey,
                List<ProviderInstance> candidates) {
            ProviderInstance selected = checked(candidates).stream()
                    .min(Comparator
                            .comparingLong((ProviderInstance candidate) ->
                                    inFlight.computeIfAbsent(
                                            candidate.runtimeIdentity(),
                                            ignored -> new LongAdder()
                                    ).sum())
                            .thenComparing(ProviderInstance::runtimeIdentity))
                    .orElseThrow();
            LongAdder counter = inFlight.computeIfAbsent(
                    selected.runtimeIdentity(),
                    ignored -> new LongAdder()
            );
            counter.increment();
            return new ProviderSelectionHandle(selected, counter::decrement);
        }
    }
}
