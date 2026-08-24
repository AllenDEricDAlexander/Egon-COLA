package top.egon.cola.platform.rbac3.admin.runtime.repository;

import java.util.Optional;

/** Resolves the minimum PostgreSQL context required before the first runtime snapshot exists. */
public interface InitialAuthorizationContextRepository {

    Optional<InitialAuthorizationContext> find(String tenantId, String identitySub);

    record InitialAuthorizationContext(
            String userId,
            long authVersion,
            long policyVersion
    ) {
        public InitialAuthorizationContext {
            if (userId == null || userId.isBlank()
                    || !userId.equals(userId.trim())) {
                throw new IllegalArgumentException("userId is required");
            }
            if (authVersion < 0L || policyVersion < 0L) {
                throw new IllegalArgumentException(
                        "authorization versions must be non-negative");
            }
        }
    }
}
