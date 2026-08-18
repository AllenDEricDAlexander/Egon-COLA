package top.egon.cola.platform.rbac3.admin.iam.user.repository;

import org.springframework.stereotype.Component;
import top.egon.cola.component.rpc.annotation.EgonRpcReference;
import top.egon.cola.platform.idp.rpc.contract.IdentityDirectoryRpc;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.BatchGetIdentityProfilesRequest;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.BatchGetIdentityProfilesResponse;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.IdentityProfile;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Read-only RBAC adapter for optional IdP identity display enrichment.
 *
 * <p>Authorization rows remain usable when IdP is unavailable; callers receive an explicit
 * partial marker instead of persisting a profile copy in RBAC.</p>
 */
@Component
public final class IdentityProfileDirectory {

    private static final int MAX_SUBJECTS = 100;

    @EgonRpcReference(timeoutMs = 1500)
    private IdentityDirectoryRpc rpc;

    public IdentityProfileDirectory() {
    }

    IdentityProfileDirectory(IdentityDirectoryRpc rpc) {
        this.rpc = Objects.requireNonNull(rpc, "rpc");
    }

    /**
     * Resolves a bounded batch of IdP profiles and converts transport failures to an unavailable
     * result. The returned data is never written to an RBAC entity.
     */
    public BatchResult batchGet(Collection<String> subjects) {
        List<String> normalized = subjects(subjects);
        if (normalized.isEmpty()) {
            return BatchResult.available(Map.of(), Set.of());
        }
        IdentityDirectoryRpc client = rpc;
        if (client == null) {
            return BatchResult.unavailable();
        }
        try {
            BatchGetIdentityProfilesResponse response = client
                    .batchGetIdentityProfiles(BatchGetIdentityProfilesRequest.newBuilder()
                            .addAllSubjects(normalized)
                            .build());
            return map(response, normalized);
        } catch (RuntimeException exception) {
            return BatchResult.unavailable();
        }
    }

    private BatchResult map(
            BatchGetIdentityProfilesResponse response,
            List<String> requested) {
        if (response == null) {
            return BatchResult.unavailable();
        }
        Map<String, Profile> profiles = new LinkedHashMap<>();
        for (IdentityProfile profile : response.getProfilesList()) {
            if (profile.getSubject().isBlank()
                    || profiles.putIfAbsent(
                    profile.getSubject(),
                    new Profile(
                            profile.getSubject(),
                            profile.getUsername(),
                            profile.getDisplayName(),
                            profile.getStatus(),
                            profile.getVersion())) != null) {
                return BatchResult.unavailable();
            }
        }
        Set<String> missing = new LinkedHashSet<>(
                response.getMissingSubjectsList());
        if (missing.stream().anyMatch(value -> value == null || value.isBlank())
                || !requested.containsAll(profiles.keySet())
                || !requested.containsAll(missing)
                || profiles.keySet().stream().anyMatch(missing::contains)) {
            return BatchResult.unavailable();
        }
        return BatchResult.available(profiles, missing);
    }

    private List<String> subjects(Collection<String> values) {
        Objects.requireNonNull(values, "subjects");
        if (values.size() > MAX_SUBJECTS) {
            throw new IllegalArgumentException("subjects must not exceed 100");
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank()
                    || !value.equals(value.trim())) {
                throw new IllegalArgumentException(
                        "subjects must contain non-blank values");
            }
            if (!normalized.add(value)) {
                throw new IllegalArgumentException(
                        "subjects must not contain duplicates");
            }
        }
        return List.copyOf(normalized);
    }

    public record Profile(
            String subject,
            String username,
            String displayName,
            String status,
            long version) {
    }

    public record BatchResult(
            Map<String, Profile> profiles,
            Set<String> missingSubjects,
            boolean available) {

        public BatchResult {
            profiles = Map.copyOf(Objects.requireNonNull(profiles, "profiles"));
            missingSubjects = Set.copyOf(
                    Objects.requireNonNull(missingSubjects, "missingSubjects"));
        }

        public static BatchResult available(
                Map<String, Profile> profiles,
                Set<String> missingSubjects) {
            return new BatchResult(profiles, missingSubjects, true);
        }

        public static BatchResult unavailable() {
            return new BatchResult(Map.of(), Set.of(), false);
        }
    }
}
