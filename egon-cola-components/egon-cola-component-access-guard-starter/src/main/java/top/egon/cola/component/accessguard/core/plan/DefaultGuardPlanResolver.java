package top.egon.cola.component.accessguard.core.plan;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class DefaultGuardPlanResolver implements GuardPlanResolver, AutoCloseable {

    private static final int MAX_FAILURES = 256;

    private final List<GuardPlanSource> sources;
    private final GuardPlanValidator validator;
    private final Consumer<GuardPlanChangedEvent> eventConsumer;
    private final Map<SnapshotKey, GuardPlanSnapshot> snapshots = new ConcurrentHashMap<>();
    private final Map<String, GuardPlanLoadFailure> failures = new ConcurrentHashMap<>();
    private final List<AutoCloseable> subscriptions = new ArrayList<>();

    public DefaultGuardPlanResolver(List<GuardPlanSource> sources, GuardPlanValidator validator) {
        this(sources, validator, ignored -> {
        });
    }

    public DefaultGuardPlanResolver(
            List<GuardPlanSource> sources,
            GuardPlanValidator validator,
            Consumer<GuardPlanChangedEvent> eventConsumer
    ) {
        if (sources == null || sources.isEmpty()) {
            throw new IllegalArgumentException("At least one GuardPlanSource is required");
        }
        this.validator = java.util.Objects.requireNonNull(validator, "validator");
        this.eventConsumer = java.util.Objects.requireNonNull(eventConsumer, "eventConsumer");
        validateSources(sources);
        this.sources = sources.stream()
                .sorted(Comparator.comparingInt(GuardPlanSource::priority).reversed())
                .toList();
        this.sources.forEach(source -> subscriptions.add(
                source.subscribe(snapshot -> accept(source, snapshot))));
    }

    @Override
    public GuardPlanSnapshot resolve(String ruleId) {
        if (ruleId == null || ruleId.isBlank()) {
            throw new IllegalArgumentException("ruleId must not be blank");
        }
        String normalized = ruleId.trim();
        sources.forEach(source -> loadCurrent(source, normalized));
        return sources.stream()
                .map(source -> snapshots.get(new SnapshotKey(source.name(), normalized)))
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown Access Guard rule: " + normalized));
    }

    @Override
    public Optional<GuardPlanLoadFailure> lastFailure(String ruleId) {
        return Optional.ofNullable(failures.get(ruleId));
    }

    @Override
    public void close() {
        RuntimeException failure = null;
        for (AutoCloseable subscription : subscriptions) {
            try {
                subscription.close();
            } catch (Exception exception) {
                if (failure == null) {
                    failure = new IllegalStateException("Failed to close GuardPlan subscription", exception);
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }
        subscriptions.clear();
        if (failure != null) {
            throw failure;
        }
    }

    private void loadCurrent(GuardPlanSource source, String ruleId) {
        source.current(ruleId).ifPresent(snapshot -> {
            SnapshotKey key = new SnapshotKey(source.name(), ruleId);
            GuardPlanSnapshot current = snapshots.get(key);
            if (current == null || snapshot.version() > current.version()) {
                accept(source, snapshot);
            }
        });
    }

    private synchronized void accept(GuardPlanSource source, GuardPlanSnapshot candidate) {
        GuardPlanSnapshot normalized = normalize(source, candidate);
        SnapshotKey key = new SnapshotKey(source.name(), normalized.ruleId());
        GuardPlanSnapshot current = snapshots.get(key);
        if (current != null && normalized.version() <= current.version()) {
            throw new IllegalArgumentException(
                    "GuardPlan versions must be monotonic for rule " + normalized.ruleId());
        }
        try {
            validator.validate(normalized);
        } catch (RuntimeException exception) {
            recordFailure(normalized, exception);
            return;
        }
        snapshots.put(key, normalized);
        failures.remove(normalized.ruleId());
        eventConsumer.accept(new GuardPlanChangedEvent(
                normalized.ruleId(),
                current == null ? -1L : current.version(),
                normalized.version(),
                source.name(),
                Instant.now()));
    }

    private GuardPlanSnapshot normalize(GuardPlanSource source, GuardPlanSnapshot snapshot) {
        return new GuardPlanSnapshot(
                snapshot.ruleId(),
                snapshot.version(),
                snapshot.loadedAt(),
                source.name(),
                snapshot.plan(),
                snapshot.configurationFingerprint());
    }

    private void recordFailure(GuardPlanSnapshot snapshot, RuntimeException exception) {
        if (failures.size() >= MAX_FAILURES && !failures.containsKey(snapshot.ruleId())) {
            failures.keySet().stream().findFirst().ifPresent(failures::remove);
        }
        failures.put(snapshot.ruleId(), new GuardPlanLoadFailure(
                snapshot.ruleId(),
                snapshot.source(),
                snapshot.version(),
                exception.getClass().getSimpleName(),
                Instant.now()));
    }

    private static void validateSources(List<GuardPlanSource> sources) {
        Set<Integer> priorities = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (GuardPlanSource source : sources) {
            if (source == null) {
                throw new IllegalArgumentException("GuardPlanSource must not be null");
            }
            if (source.name() == null || source.name().isBlank() || !names.add(source.name())) {
                throw new IllegalArgumentException("GuardPlanSource names must be unique and nonblank");
            }
            if (!priorities.add(source.priority())) {
                throw new IllegalArgumentException("GuardPlanSource priority must be unique");
            }
        }
    }

    private record SnapshotKey(String source, String ruleId) {
    }

}
