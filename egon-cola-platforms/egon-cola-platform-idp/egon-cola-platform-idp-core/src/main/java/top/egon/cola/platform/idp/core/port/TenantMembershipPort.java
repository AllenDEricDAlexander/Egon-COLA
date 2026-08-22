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
