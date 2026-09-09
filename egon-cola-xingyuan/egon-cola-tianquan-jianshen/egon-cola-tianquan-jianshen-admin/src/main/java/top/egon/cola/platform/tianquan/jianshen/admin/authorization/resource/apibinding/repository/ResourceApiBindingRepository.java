package top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.apibinding.repository;

import java.time.Instant;
import java.util.Set;

/** Persistence port for the global CI-owned resource-to-API relation. */
public interface ResourceApiBindingRepository {

    void replaceForApplication(
            Long applicationId,
            String sourceBuildId,
            String sourceChecksum,
            Set<BindingPair> pairs,
            String actorId,
            Instant now);

    Set<Long> apiIdsForSources(
            Long applicationId,
            Set<Long> sourceResourceIds,
            Instant now);

    long countDistinctActiveRolesDerivingApi(
            Long applicationId,
            Long apiResourceId,
            Instant now);

    record BindingPair(Long sourceResourceId, Long apiResourceId) {
        public BindingPair {
            sourceResourceId = positive(sourceResourceId, "sourceResourceId");
            apiResourceId = positive(apiResourceId, "apiResourceId");
            if (sourceResourceId.equals(apiResourceId)) {
                throw new IllegalArgumentException(
                        "sourceResourceId and apiResourceId must differ");
            }
        }

        private static Long positive(Long value, String fieldName) {
            if (value == null || value <= 0L) {
                throw new IllegalArgumentException(fieldName + " must be positive");
            }
            return value;
        }
    }
}
