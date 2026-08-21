package top.egon.cola.component.rpc.consumer.loadbalance;

import top.egon.cola.component.rpc.annotation.LoadBalance;
import top.egon.cola.component.rpc.consumer.channel.RpcEndpoint;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

/** Factory and bounded, transport-neutral implementations of RPC LB policies. */
public final class RpcLoadBalancers {

    public static final int DEFAULT_VIRTUAL_NODES = 160;

    private static final int DEFAULT_WEIGHT = 100;

    private final int virtualNodes;
    private final RandomGenerator random;

    public RpcLoadBalancers() {
        this(DEFAULT_VIRTUAL_NODES, RandomGenerator.getDefault());
    }

    public RpcLoadBalancers(int virtualNodes, RandomGenerator random) {
        if (virtualNodes < 16 || virtualNodes > 4096) {
            throw invalid("consistent-hash virtual nodes must be between 16 and 4096");
        }
        this.virtualNodes = virtualNodes;
        this.random = random == null ? RandomGenerator.getDefault() : random;
    }

    public static RpcLoadBalancer create(LoadBalance algorithm) {
        return new RpcLoadBalancers().loadBalancer(algorithm);
    }

    public static RpcLoadBalancer create(LoadBalance algorithm, int virtualNodes) {
        return new RpcLoadBalancers(virtualNodes, RandomGenerator.getDefault())
                .loadBalancer(algorithm);
    }

    public RpcLoadBalancer loadBalancer(LoadBalance algorithm) {
        if (algorithm == null || algorithm == LoadBalance.INHERIT) {
            throw invalid("RPC load-balance algorithm must be resolved before selection");
        }
        return switch (algorithm) {
            case ROUND_ROBIN -> new RoundRobinBalancer();
            case SMOOTH_WEIGHTED_ROUND_ROBIN -> new SmoothWeightedRoundRobinBalancer();
            case RANDOM -> new RandomBalancer();
            case WEIGHTED_RANDOM -> new WeightedRandomBalancer();
            case CONSISTENT_HASH -> new ConsistentHashBalancer();
            case LEAST_IN_FLIGHT -> new LeastInFlightBalancer();
            case INHERIT -> throw invalid("INHERIT is not a runtime load-balance algorithm");
        };
    }

    /** Stable endpoint identity used in exclusion sets and local LB state only. */
    public static String endpointKey(RpcEndpoint endpoint) {
        if (endpoint == null || endpoint.host() == null || endpoint.host().isBlank()
                || endpoint.port() <= 0 || endpoint.port() > 65535) {
            throw invalid("RPC endpoint identity is invalid");
        }
        return endpoint.host().trim() + ':' + endpoint.port() + ':' + endpoint.secure();
    }

    private abstract static class AbstractBalancer implements RpcLoadBalancer {

        protected List<Candidate> candidates(RpcLoadBalanceContext context) {
            if (context == null) {
                throw invalid("RPC load-balance context is required");
            }
            Map<String, Candidate> unique = new LinkedHashMap<>();
            for (RpcEndpoint endpoint : context.candidates()) {
                if (endpoint == null) {
                    continue;
                }
                String key = endpointKey(endpoint);
                unique.putIfAbsent(key, new Candidate(endpoint, key, weight(endpoint)));
            }
            List<Candidate> values = new ArrayList<>(unique.values());
            values.sort(Comparator.comparing(Candidate::key));
            Set<String> excluded = context.excluded();
            values.removeIf(candidate -> excluded.contains(candidate.key()));
            if (values.isEmpty()) {
                throw unavailable(context);
            }
            return List.copyOf(values);
        }

        protected List<Candidate> allCandidates(RpcLoadBalanceContext context) {
            if (context == null) {
                throw invalid("RPC load-balance context is required");
            }
            Map<String, Candidate> unique = new LinkedHashMap<>();
            for (RpcEndpoint endpoint : context.candidates()) {
                if (endpoint == null) {
                    continue;
                }
                String key = endpointKey(endpoint);
                unique.putIfAbsent(key, new Candidate(endpoint, key, weight(endpoint)));
            }
            List<Candidate> values = new ArrayList<>(unique.values());
            values.sort(Comparator.comparing(Candidate::key));
            if (values.isEmpty()) {
                throw unavailable(context);
            }
            return List.copyOf(values);
        }

        protected static int weight(RpcEndpoint endpoint) {
            try {
                Method method = endpoint.getClass().getMethod("weight");
                if (method.getReturnType() == int.class
                        || method.getReturnType() == Integer.class) {
                    method.trySetAccessible();
                    Object value = method.invoke(endpoint);
                    if (value instanceof Number number
                            && number.intValue() > 0
                            && number.intValue() <= 10_000) {
                        return number.intValue();
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // RpcEndpoint gains a default weight contract in the DDC step;
                // older/custom endpoint implementations remain weight 100 here.
            }
            return DEFAULT_WEIGHT;
        }

        protected static long totalWeight(List<Candidate> candidates) {
            long total = 0;
            for (Candidate candidate : candidates) {
                total = safeAdd(total, candidate.weight());
            }
            return Math.max(1, total);
        }

        protected static long safeAdd(long left, long right) {
            return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
        }

        protected static EgonRpcException unavailable(RpcLoadBalanceContext context) {
            return new EgonRpcException(
                    EgonRpcErrorCode.RPC_PROVIDER_UNAVAILABLE,
                    "RPC endpoint candidates are unavailable for " + context.queryIdentity());
        }
    }

    private record Candidate(RpcEndpoint endpoint, String key, int weight) {
    }

    private final class RoundRobinBalancer extends AbstractBalancer {

        private final Map<String, AtomicLong> sequences = new ConcurrentHashMap<>();

        @Override
        public RpcEndpoint select(RpcLoadBalanceContext context) {
            List<Candidate> candidates = candidates(context);
            AtomicLong sequence = sequences.computeIfAbsent(
                    context.queryIdentity(), ignored -> new AtomicLong());
            int index = Math.floorMod(sequence.getAndIncrement(), candidates.size());
            return candidates.get(index).endpoint();
        }

        @Override
        public void remove(String queryIdentity) {
            if (queryIdentity != null) {
                sequences.remove(queryIdentity);
            }
        }

        @Override
        public void close() {
            sequences.clear();
        }
    }

    private final class RandomBalancer extends AbstractBalancer {

        @Override
        public RpcEndpoint select(RpcLoadBalanceContext context) {
            List<Candidate> candidates = candidates(context);
            int index;
            synchronized (random) {
                index = random.nextInt(candidates.size());
            }
            return candidates.get(index).endpoint();
        }
    }

    private final class WeightedRandomBalancer extends AbstractBalancer {

        @Override
        public RpcEndpoint select(RpcLoadBalanceContext context) {
            List<Candidate> candidates = candidates(context);
            long total = totalWeight(candidates);
            long offset;
            synchronized (random) {
                offset = random.nextLong(total);
            }
            for (Candidate candidate : candidates) {
                if (offset < candidate.weight()) {
                    return candidate.endpoint();
                }
                offset -= candidate.weight();
            }
            return candidates.getLast().endpoint();
        }
    }

    private final class SmoothWeightedRoundRobinBalancer extends AbstractBalancer {

        private final Map<String, SwrrState> states = new ConcurrentHashMap<>();

        @Override
        public RpcEndpoint select(RpcLoadBalanceContext context) {
            List<Candidate> candidates = candidates(context);
            SwrrState state = states.computeIfAbsent(
                    context.queryIdentity(), ignored -> new SwrrState());
            synchronized (state) {
                state.reconcile(candidates);
                long total = totalWeight(candidates);
                Candidate selected = null;
                long selectedCurrent = Long.MIN_VALUE;
                for (Candidate candidate : candidates) {
                    long current = safeAdd(
                            state.current.getOrDefault(candidate.key(), 0L),
                            candidate.weight());
                    state.current.put(candidate.key(), current);
                    if (selected == null || current > selectedCurrent) {
                        selected = candidate;
                        selectedCurrent = current;
                    }
                }
                state.current.computeIfPresent(
                        selected.key(), (ignored, value) -> value - total);
                return selected.endpoint();
            }
        }

        @Override
        public void remove(String queryIdentity) {
            if (queryIdentity != null) {
                states.remove(queryIdentity);
            }
        }

        @Override
        public void close() {
            states.clear();
        }
    }

    private static final class SwrrState {

        private final Map<String, Long> current = new HashMap<>();

        private void reconcile(List<Candidate> candidates) {
            Set<String> active = candidates.stream()
                    .map(Candidate::key)
                    .collect(Collectors.toSet());
            current.keySet().removeIf(key -> !active.contains(key));
            candidates.forEach(candidate -> current.putIfAbsent(candidate.key(), 0L));
        }
    }

    private final class LeastInFlightBalancer extends AbstractBalancer {

        private final Map<String, Map<String, AtomicLong>> inFlight =
                new ConcurrentHashMap<>();

        @Override
        public RpcEndpoint select(RpcLoadBalanceContext context) {
            List<Candidate> candidates = candidates(context);
            Map<String, AtomicLong> counts = inFlight.computeIfAbsent(
                    context.queryIdentity(), ignored -> new ConcurrentHashMap<>());
            Set<String> active = candidates.stream()
                    .map(Candidate::key)
                    .collect(Collectors.toSet());
            counts.keySet().removeIf(key -> !active.contains(key));
            Candidate selected = candidates.getFirst();
            long selectedCount = count(counts, selected.key());
            for (Candidate candidate : candidates) {
                long current = count(counts, candidate.key());
                if (current < selectedCount) {
                    selected = candidate;
                    selectedCount = current;
                }
            }
            counts.computeIfAbsent(selected.key(), ignored -> new AtomicLong())
                    .incrementAndGet();
            return selected.endpoint();
        }

        @Override
        public void release(RpcLoadBalanceContext context, RpcEndpoint endpoint) {
            if (context == null || endpoint == null) {
                return;
            }
            Map<String, AtomicLong> counts = inFlight.get(context.queryIdentity());
            if (counts == null) {
                return;
            }
            AtomicLong count = counts.get(endpointKey(endpoint));
            if (count != null) {
                count.updateAndGet(value -> Math.max(0, value - 1));
            }
        }

        @Override
        public void remove(String queryIdentity) {
            if (queryIdentity != null) {
                inFlight.remove(queryIdentity);
            }
        }

        @Override
        public void close() {
            inFlight.clear();
        }

        private long count(Map<String, AtomicLong> counts, String key) {
            AtomicLong value = counts.get(key);
            return value == null ? 0 : value.get();
        }
    }

    private final class ConsistentHashBalancer extends AbstractBalancer {

        private final Map<String, RingState> rings = new ConcurrentHashMap<>();

        @Override
        public RpcEndpoint select(RpcLoadBalanceContext context) {
            byte[] digest = context == null ? null : context.affinityDigest();
            if (digest == null || digest.length == 0) {
                throw new EgonRpcException(
                        EgonRpcErrorCode.RPC_INVALID_REQUEST,
                        "CONSISTENT_HASH requires a non-empty affinity digest");
            }
            List<Candidate> all = allCandidates(context);
            String signature = signature(context.revision(), all);
            RingState state = rings.compute(context.queryIdentity(),
                    (ignored, current) -> current == null || !current.signature.equals(signature)
                            ? new RingState(signature, buildRing(all)) : current);
            if (state.ring.isEmpty()) {
                throw unavailable(context);
            }
            long point = hashPoint(digest);
            for (Map.Entry<Long, Candidate> entry : state.ring.tailMap(point, true).entrySet()) {
                if (!context.excluded().contains(entry.getValue().key())) {
                    return entry.getValue().endpoint();
                }
            }
            for (Candidate candidate : state.ring.values()) {
                if (!context.excluded().contains(candidate.key())) {
                    return candidate.endpoint();
                }
            }
            throw unavailable(context);
        }

        @Override
        public void remove(String queryIdentity) {
            if (queryIdentity != null) {
                rings.remove(queryIdentity);
            }
        }

        @Override
        public void close() {
            rings.clear();
        }

        private TreeMap<Long, Candidate> buildRing(List<Candidate> candidates) {
            TreeMap<Long, Candidate> ring = new TreeMap<>();
            for (Candidate candidate : candidates) {
                long requested = (long) virtualNodes * candidate.weight();
                int count = (int) Math.max(1, Math.min(100_000, (requested + 99) / 100));
                for (int index = 0; index < count; index++) {
                    long point = hashPoint((candidate.key() + '#' + index)
                            .getBytes(StandardCharsets.UTF_8));
                    while (ring.containsKey(point)) {
                        point = point == Long.MAX_VALUE ? 0 : point + 1;
                    }
                    ring.put(point, candidate);
                }
            }
            return ring;
        }

        private String signature(long revision, List<Candidate> candidates) {
            StringBuilder value = new StringBuilder().append(revision);
            for (Candidate candidate : candidates) {
                value.append('|').append(candidate.key()).append(':').append(candidate.weight());
            }
            return value.toString();
        }
    }

    private record RingState(String signature, TreeMap<Long, Candidate> ring) {
    }

    private static long hashPoint(byte[] value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value);
            return ByteBuffer.wrap(digest).getLong() & Long.MAX_VALUE;
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required", exception);
        }
    }

    private static EgonRpcException invalid(String message) {
        return new EgonRpcException(EgonRpcErrorCode.RPC_INVALID_CONTRACT, message);
    }
}
