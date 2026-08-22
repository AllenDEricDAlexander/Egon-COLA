package top.egon.cola.platform.rbac3.admin.iam.user.repository;

import org.springframework.stereotype.Component;
import top.egon.cola.component.rpc.annotation.EgonRpcReference;
import top.egon.cola.component.rpc.consumer.reference.RpcReferenceMode;
import top.egon.cola.platform.idp.rpc.contract.IdentityDirectoryRpc;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.GetTenantMembershipRequest;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.GetTenantMembershipResponse;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.IdentityStatus;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.MembershipStatus;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.TenantMembershipProfile;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.TenantStatus;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.util.Objects;

/**
 * Fail-closed RBAC adapter for the authoritative IdP tenant membership fact.
 *
 * <p>The adapter returns only a small verification marker. It never caches or
 * persists the IdP profile and it maps transport, missing and malformed
 * responses to stable RBAC violations.</p>
 */
@Component
public final class IdentityTenantMembershipDirectory {

    private static final String NOT_ACTIVE =
            "IDENTITY_TENANT_MEMBERSHIP_NOT_ACTIVE";
    private static final String UNAVAILABLE =
            "IDENTITY_TENANT_MEMBERSHIP_UNAVAILABLE";
    private static final String INVALID =
            "IDENTITY_TENANT_MEMBERSHIP_INVALID";

    @EgonRpcReference(mode = RpcReferenceMode.GATEWAY, timeoutMs = 1500)
    private IdentityDirectoryRpc rpc;

    public IdentityTenantMembershipDirectory() {
    }

    IdentityTenantMembershipDirectory(IdentityDirectoryRpc rpc) {
        this.rpc = Objects.requireNonNull(rpc, "rpc");
    }

    /**
     * Requires the current IdP tenant, identity and membership statuses to be
     * ACTIVE before an RBAC write may continue.
     */
    public MembershipVerification requireActive(
            String tenantId,
            String identitySub) {
        String normalizedTenantId = tenantId(tenantId);
        String normalizedIdentitySub = identitySub(identitySub);
        IdentityDirectoryRpc client = rpc;
        if (client == null) {
            throw unavailable();
        }

        GetTenantMembershipResponse response;
        try {
            response = client.getTenantMembership(
                    GetTenantMembershipRequest.newBuilder()
                            .setTenantId(normalizedTenantId)
                            .setIdentitySub(normalizedIdentitySub)
                            .build());
        } catch (RuntimeException unavailable) {
            throw unavailable();
        }
        if (response == null || !response.hasProfile()) {
            throw unavailable();
        }

        TenantMembershipProfile profile = response.getProfile();
        if (!normalizedTenantId.equals(profile.getTenantId())
                || !normalizedIdentitySub.equals(profile.getIdentitySub())
                || profile.getMembershipVersion() < 0L) {
            throw unavailable();
        }
        if (profile.getTenantStatus() != TenantStatus.TENANT_STATUS_ACTIVE
                || profile.getIdentityStatus() != IdentityStatus.IDENTITY_STATUS_ACTIVE
                || profile.getMembershipStatus()
                != MembershipStatus.MEMBERSHIP_STATUS_ACTIVE) {
            throw notActive();
        }
        return new MembershipVerification(
                normalizedTenantId,
                normalizedIdentitySub,
                profile.getMembershipVersion());
    }

    /** Marker proving the immediately preceding membership gate succeeded. */
    public record MembershipVerification(
            String tenantId,
            String identitySub,
            long membershipVersion) {

        public MembershipVerification {
            if (tenantId == null || tenantId.isBlank()
                    || identitySub == null || identitySub.isBlank()
                    || membershipVersion < 0L) {
                throw new IllegalArgumentException(
                        "membership verification is invalid");
            }
        }
    }

    private static String tenantId(String value) {
        String normalized = required(value, "tenantId");
        if (!normalized.chars().allMatch(Character::isDigit)) {
            throw invalid();
        }
        try {
            long parsed = Long.parseLong(normalized);
            if (parsed <= 0L) {
                throw invalid();
            }
            return Long.toString(parsed);
        } catch (NumberFormatException overflow) {
            throw invalid();
        }
    }

    private static String identitySub(String value) {
        String normalized = required(value, "identitySub");
        if (normalized.length() > 64) {
            throw invalid();
        }
        return normalized;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw invalid();
        }
        return value;
    }

    private static Rbac3RuleViolation invalid() {
        return new Rbac3RuleViolation(INVALID);
    }

    private static Rbac3RuleViolation notActive() {
        return new Rbac3RuleViolation(NOT_ACTIVE);
    }

    private static Rbac3RuleViolation unavailable() {
        return new Rbac3RuleViolation(UNAVAILABLE);
    }
}
