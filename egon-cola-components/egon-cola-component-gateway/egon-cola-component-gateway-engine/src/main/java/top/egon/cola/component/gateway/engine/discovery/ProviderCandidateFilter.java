package top.egon.cola.component.gateway.engine.discovery;

import top.egon.cola.component.gateway.core.provider.ProviderHealthState;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.provider.ProviderRegistryState;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

public final class ProviderCandidateFilter {

    private final Clock clock;

    private final Predicate<String> admissionAvailable;

    public ProviderCandidateFilter(
            Clock clock,
            Predicate<String> admissionAvailable) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.admissionAvailable = Objects.requireNonNull(
                admissionAvailable,
                "admissionAvailable"
        );
    }

    public ProviderCandidateFilterResult filter(
            ProviderServiceKey expectedService,
            List<ProviderInstance> instances,
            ProviderSelectionPolicy policy) {
        Objects.requireNonNull(expectedService, "expectedService");
        Objects.requireNonNull(instances, "instances");
        Objects.requireNonNull(policy, "policy");
        EnumMap<ProviderCandidateStage, Integer> counts =
                new EnumMap<>(ProviderCandidateStage.class);
        Map<String, String> rejected = new LinkedHashMap<>();
        List<Candidate> candidates = instances.stream()
                .map(Candidate::new)
                .toList();

        candidates = apply(
                candidates,
                ProviderCandidateStage.EXACT_SERVICE,
                candidate -> expectedService.equals(
                        candidate.instance.serviceKey()
                ),
                "SERVICE_KEY_MISMATCH",
                counts,
                rejected
        );
        Instant now = clock.instant();
        candidates = apply(
                candidates,
                ProviderCandidateStage.VALID_LEASE,
                candidate -> candidate.instance.registryState()
                        == ProviderRegistryState.REGISTERED
                        && candidate.instance.leaseExpireAt().isAfter(now),
                "LEASE_EXPIRED",
                counts,
                rejected
        );
        candidates = apply(
                candidates,
                ProviderCandidateStage.PROTOCOL_MATCH,
                candidate -> policy.secureRequired() == null
                        || candidate.instance.secure()
                        == policy.secureRequired(),
                "PROTOCOL_MISMATCH",
                counts,
                rejected
        );
        candidates = candidates.stream()
                .peek(candidate -> candidate.resolve(policy))
                .toList();
        candidates = apply(
                candidates,
                ProviderCandidateStage.ADMIN_ENABLED,
                candidate -> candidate.enabled,
                "ADMIN_DISABLED",
                counts,
                rejected
        );
        candidates = apply(
                candidates,
                ProviderCandidateStage.LOCATION_AND_TAGS,
                candidate -> matchesLocationAndTags(candidate, policy),
                "LOCATION_OR_TAG_MISMATCH",
                counts,
                rejected
        );
        candidates = apply(
                candidates,
                ProviderCandidateStage.HEALTHY,
                candidate -> healthy(candidate.instance)
                        && admissionAvailable.test(
                        candidate.instance.runtimeIdentity()
                ),
                "UNHEALTHY_OR_EJECTED",
                counts,
                rejected
        );
        counts.put(
                ProviderCandidateStage.ADMISSION_AVAILABLE,
                candidates.size()
        );

        List<Candidate> weighted = new ArrayList<>();
        for (Candidate candidate : candidates) {
            try {
                if (candidate.weight() > 0) {
                    weighted.add(candidate);
                } else {
                    rejected.putIfAbsent(
                            candidate.identity(),
                            "WEIGHT_DISABLED"
                    );
                }
            } catch (IllegalArgumentException invalidMetadata) {
                rejected.putIfAbsent(
                        candidate.identity(),
                        "INVALID_METADATA"
                );
            }
        }
        counts.put(ProviderCandidateStage.POSITIVE_WEIGHT, weighted.size());
        return new ProviderCandidateFilterResult(
                weighted.stream().map(Candidate::effectiveInstance).toList(),
                counts,
                rejected
        );
    }

    private List<Candidate> apply(
            List<Candidate> source,
            ProviderCandidateStage stage,
            Predicate<Candidate> predicate,
            String rejection,
            Map<ProviderCandidateStage, Integer> counts,
            Map<String, String> rejected) {
        List<Candidate> accepted = new ArrayList<>();
        for (Candidate candidate : source) {
            if (predicate.test(candidate)) {
                accepted.add(candidate);
            } else {
                rejected.putIfAbsent(candidate.identity(), rejection);
            }
        }
        counts.put(stage, accepted.size());
        return List.copyOf(accepted);
    }

    private boolean matchesLocationAndTags(
            Candidate candidate,
            ProviderSelectionPolicy policy) {
        return matches(policy.requiredZone(), candidate.zone)
                && matches(policy.requiredRegion(), candidate.region)
                && candidate.tags.containsAll(policy.requiredTags());
    }

    private boolean matches(String required, String actual) {
        return required == null || required.equals(actual);
    }

    private boolean healthy(ProviderInstance instance) {
        return allowed(instance.activeHealth())
                && allowed(instance.passiveHealth());
    }

    private boolean allowed(ProviderHealthState state) {
        return state != ProviderHealthState.UNHEALTHY
                && state != ProviderHealthState.EJECTED;
    }

    private static final class Candidate {

        private final ProviderInstance instance;

        private Map<String, String> metadata;

        private boolean enabled;

        private String zone;

        private String region;

        private Set<String> tags;

        private Candidate(ProviderInstance instance) {
            this.instance = Objects.requireNonNull(instance, "instance");
        }

        private void resolve(ProviderSelectionPolicy policy) {
            metadata = new LinkedHashMap<>(instance.metadata());
            enabled = policy.serviceEnabled();
            apply(policy.serviceOverride());
            apply(policy.instanceOverrides().get(instance.instanceId()));
            zone = metadata.get("gateway.zone");
            region = metadata.get("gateway.region");
            tags = parseTags(metadata.get("gateway.tags"));
        }

        private void apply(ProviderPolicyOverride override) {
            if (override == null) {
                return;
            }
            if (override.enabled() != null) {
                enabled = override.enabled();
            }
            put("gateway.weight", override.weight());
            put("gateway.zone", override.zone());
            put("gateway.region", override.region());
            if (override.tags() != null) {
                metadata.put(
                        "gateway.tags",
                        String.join(",", override.tags().stream().sorted()
                                .toList())
                );
            }
        }

        private void put(String key, Object value) {
            if (value != null) {
                metadata.put(key, value.toString());
            }
        }

        private int weight() {
            String raw = metadata.getOrDefault("gateway.weight", "100");
            try {
                int result = Integer.parseInt(raw);
                if (result < 0 || result > 10000) {
                    throw new IllegalArgumentException(
                            "gateway.weight must be between 0 and 10000"
                    );
                }
                return result;
            } catch (NumberFormatException invalid) {
                throw new IllegalArgumentException(
                        "gateway.weight must be an integer",
                        invalid
                );
            }
        }

        private ProviderInstance effectiveInstance() {
            return new ProviderInstance(
                    instance.serviceKey(),
                    instance.instanceId(),
                    instance.leaseId(),
                    instance.host(),
                    instance.port(),
                    instance.secure(),
                    metadata,
                    instance.leaseExpireAt(),
                    instance.registryState(),
                    instance.activeHealth(),
                    instance.passiveHealth()
            );
        }

        private String identity() {
            return instance.runtimeIdentity();
        }

        private static Set<String> parseTags(String raw) {
            if (raw == null || raw.isBlank()) {
                return Set.of();
            }
            Set<String> result = new LinkedHashSet<>();
            for (String value : raw.split(",")) {
                String tag = value.trim();
                if (tag.isEmpty()) {
                    throw new IllegalArgumentException(
                            "gateway.tags contains an empty tag"
                    );
                }
                result.add(tag);
            }
            return Set.copyOf(result);
        }
    }
}
