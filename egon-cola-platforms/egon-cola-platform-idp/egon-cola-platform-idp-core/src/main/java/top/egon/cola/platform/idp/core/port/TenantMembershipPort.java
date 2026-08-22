package top.egon.cola.platform.idp.core.port;

import java.util.List;

public interface TenantMembershipPort {

    TenantMembership resolve(
            String identitySub,
            String tenantId
    );

    List<TenantMembership> list(String identitySub);

    record TenantMembership(
            String identitySub,
            String tenantId,
            String tenantDisplayName,
            MembershipStatus status
    ) {

        /**
         * 兼容尚未切换的旧 adapter；legacy internal identifier 被有意丢弃且不进入 record state。
         *
         * <p>Compatibility constructor for the old adapter; the legacy internal identifier is
         * intentionally discarded and never enters the record state.</p>
         */
        @Deprecated(forRemoval = true)
        public TenantMembership(
                String identitySub,
                String tenantId,
                String ignoredLegacyIdentifier,
                String tenantDisplayName,
                MembershipStatus status
        ) {
            this(identitySub, tenantId, tenantDisplayName, status);
        }
    }

    enum MembershipStatus {
        ACTIVE,
        DISABLED
    }

    final class TenantMembershipException extends RuntimeException {
        public TenantMembershipException(String message) {
            super(message);
        }
    }
}
