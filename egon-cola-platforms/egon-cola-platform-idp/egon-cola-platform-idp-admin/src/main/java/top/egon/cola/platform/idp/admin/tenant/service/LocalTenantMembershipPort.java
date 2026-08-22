package top.egon.cola.platform.idp.admin.tenant.service;

import top.egon.cola.platform.idp.core.port.TenantMembershipPort;

import java.util.List;
import java.util.Objects;

/** Adapts the IdP-local membership read model to the core login port. */
public final class LocalTenantMembershipPort implements TenantMembershipPort {

    private final TenantMembershipService memberships;

    public LocalTenantMembershipPort(TenantMembershipService memberships) {
        this.memberships = Objects.requireNonNull(memberships, "memberships");
    }

    @Override
    public TenantMembership resolve(String identitySub, String tenantId) {
        try {
            TenantMembershipService.TenantMembershipProfile profile =
                    memberships.resolve(identitySub, tenantId);
            return toDomain(profile);
        } catch (IllegalStateException exception) {
            if ("membership not found".equals(exception.getMessage())) {
                return null;
            }
            throw unavailable(exception);
        } catch (RuntimeException exception) {
            throw unavailable(exception);
        }
    }

    @Override
    public List<TenantMembership> list(String identitySub) {
        try {
            return memberships.listByIdentity(identitySub).stream()
                    .filter(profile -> profile.effectiveStatus()
                            == MembershipStatus.ACTIVE)
                    .map(this::toDomain)
                    .toList();
        } catch (RuntimeException exception) {
            throw unavailable(exception);
        }
    }

    private TenantMembership toDomain(
            TenantMembershipService.TenantMembershipProfile profile
    ) {
        if (profile == null
                || profile.identitySub() == null
                || profile.tenantId() == null
                || profile.tenantDisplayName() == null
                || profile.effectiveStatus() == null) {
            throw unavailable(null);
        }
        return new TenantMembership(
                profile.identitySub(),
                profile.tenantId(),
                profile.tenantDisplayName(),
                profile.effectiveStatus()
        );
    }

    private static TenantMembershipException unavailable(Throwable cause) {
        TenantMembershipException exception = new TenantMembershipException(
                "tenant membership is unavailable"
        );
        if (cause != null) {
            exception.initCause(cause);
        }
        return exception;
    }
}
