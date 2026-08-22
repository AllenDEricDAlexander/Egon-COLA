package top.egon.cola.platform.idp.admin.tenant.service;

import top.egon.cola.platform.idp.admin.tenant.domain.pojo.IdentityTenantEntity;
import top.egon.cola.platform.idp.admin.tenant.domain.pojo.IdentityTenantMembershipEntity;
import top.egon.cola.platform.idp.core.identity.IdentityUserStatus;
import top.egon.cola.platform.idp.core.port.TenantMembershipPort;

import java.time.Instant;
import java.util.List;

/** Application service for IdP-owned tenant membership facts and effective reads. */
public interface TenantMembershipService {

    List<MembershipView> listByTenant(String tenantId);

    List<TenantMembershipProfile> listByIdentity(String identitySub);

    MembershipView upsert(UpsertMembershipCommand command);

    TenantMembershipProfile resolve(String identitySub, String tenantId);

    /** Versioned membership create/replace command. */
    record UpsertMembershipCommand(
            String tenantId,
            String identitySub,
            IdentityTenantMembershipEntity.Status status,
            Long expectedVersion,
            String operatorSub
    ) {
    }

    /** Safe membership view for administrative transport. */
    record MembershipView(
            String tenantId,
            String identitySub,
            String displayName,
            IdentityTenantMembershipEntity.Status status,
            long version,
            Instant updatedAt
    ) {
    }

    /** Complete IdP fact set consumed by local login, HTTP and RPC adapters. */
    record TenantMembershipProfile(
            String identitySub,
            String tenantId,
            String tenantDisplayName,
            String identityDisplayName,
            IdentityTenantEntity.Status tenantStatus,
            IdentityUserStatus identityStatus,
            IdentityTenantMembershipEntity.Status membershipStatus,
            TenantMembershipPort.MembershipStatus effectiveStatus,
            long membershipVersion,
            Instant membershipUpdatedAt
    ) {
    }
}
