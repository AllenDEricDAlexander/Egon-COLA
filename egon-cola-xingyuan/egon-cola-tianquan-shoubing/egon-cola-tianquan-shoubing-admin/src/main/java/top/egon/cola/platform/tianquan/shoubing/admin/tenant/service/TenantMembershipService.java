package top.egon.cola.platform.tianquan.shoubing.admin.tenant.service;

import top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.bo.IdentityTenantBO;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.bo.IdentityTenantMembershipBO;
import top.egon.cola.platform.tianquan.shoubing.core.identity.IdentityUserStatus;
import top.egon.cola.platform.tianquan.shoubing.core.port.TenantMembershipPort;

import java.time.Instant;
import java.util.List;

/** Application service for Tianquan-Shoubing-owned tenant membership facts and effective reads. */
public interface TenantMembershipService {

    List<MembershipView> listByTenant(String tenantId);

    List<TenantMembershipProfile> listByIdentity(String identitySub);

    MembershipView upsert(UpsertMembershipCommand command);

    TenantMembershipProfile resolve(String identitySub, String tenantId);

    /** Versioned membership create/replace command. */
    record UpsertMembershipCommand(
            String tenantId,
            String identitySub,
            IdentityTenantMembershipBO.Status status,
            Long expectedVersion,
            String operatorSub
    ) {
    }

    /** Safe membership view for administrative transport. */
    record MembershipView(
            String tenantId,
            String identitySub,
            String displayName,
            IdentityTenantMembershipBO.Status status,
            long version,
            Instant updatedAt
    ) {
    }

    /** Complete Tianquan-Shoubing fact set consumed by local login, HTTP and RPC adapters. */
    record TenantMembershipProfile(
            String identitySub,
            String tenantId,
            String tenantDisplayName,
            String identityDisplayName,
            IdentityTenantBO.Status tenantStatus,
            IdentityUserStatus identityStatus,
            IdentityTenantMembershipBO.Status membershipStatus,
            TenantMembershipPort.MembershipStatus effectiveStatus,
            long membershipVersion,
            Instant membershipUpdatedAt
    ) {
    }
}
