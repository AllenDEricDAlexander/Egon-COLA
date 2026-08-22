package top.egon.cola.platform.rbac3.admin.iam.user.repository;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.idp.rpc.contract.IdentityDirectoryRpc;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.GetTenantMembershipRequest;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.GetTenantMembershipResponse;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.IdentityStatus;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.MembershipStatus;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.TenantMembershipProfile;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.TenantStatus;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdentityTenantMembershipDirectoryTest {

    @Test
    void returnsTypedVerificationOnlyForAnActiveTenantIdentityMembership() {
        IdentityDirectoryRpc rpc = mock(IdentityDirectoryRpc.class);
        when(rpc.getTenantMembership(any(GetTenantMembershipRequest.class)))
                .thenReturn(response(
                        TenantStatus.TENANT_STATUS_ACTIVE,
                        IdentityStatus.IDENTITY_STATUS_ACTIVE,
                        MembershipStatus.MEMBERSHIP_STATUS_ACTIVE,
                        7L));

        IdentityTenantMembershipDirectory directory =
                new IdentityTenantMembershipDirectory(rpc);

        IdentityTenantMembershipDirectory.MembershipVerification result =
                directory.requireActive("00017", "subject-17");

        assertThat(result.tenantId()).isEqualTo("17");
        assertThat(result.identitySub()).isEqualTo("subject-17");
        assertThat(result.membershipVersion()).isEqualTo(7L);
        verify(rpc).getTenantMembership(any(GetTenantMembershipRequest.class));
    }

    @Test
    void rejectsInactiveMembershipWithoutReturningAnAllowMarker() {
        IdentityDirectoryRpc rpc = mock(IdentityDirectoryRpc.class);
        when(rpc.getTenantMembership(any(GetTenantMembershipRequest.class)))
                .thenReturn(response(
                        TenantStatus.TENANT_STATUS_ACTIVE,
                        IdentityStatus.IDENTITY_STATUS_ACTIVE,
                        MembershipStatus.MEMBERSHIP_STATUS_DISABLED,
                        7L));

        assertThatThrownBy(() -> new IdentityTenantMembershipDirectory(rpc)
                .requireActive("17", "subject-17"))
                .isInstanceOf(Rbac3RuleViolation.class)
                .extracting(exception -> ((Rbac3RuleViolation) exception).reasonCode())
                .isEqualTo("IDENTITY_TENANT_MEMBERSHIP_NOT_ACTIVE");
    }

    @Test
    void mapsMissingMalformedAndUnavailableResponsesToFailClosedViolation() {
        IdentityDirectoryRpc rpc = mock(IdentityDirectoryRpc.class);
        when(rpc.getTenantMembership(any(GetTenantMembershipRequest.class)))
                .thenReturn(GetTenantMembershipResponse.getDefaultInstance());

        assertThatThrownBy(() -> new IdentityTenantMembershipDirectory(rpc)
                .requireActive("17", "subject-17"))
                .isInstanceOf(Rbac3RuleViolation.class)
                .extracting(exception -> ((Rbac3RuleViolation) exception).reasonCode())
                .isEqualTo("IDENTITY_TENANT_MEMBERSHIP_UNAVAILABLE");

        when(rpc.getTenantMembership(any(GetTenantMembershipRequest.class)))
                .thenThrow(new IllegalStateException("timeout"));
        assertThatThrownBy(() -> new IdentityTenantMembershipDirectory(rpc)
                .requireActive("17", "subject-17"))
                .isInstanceOf(Rbac3RuleViolation.class)
                .extracting(exception -> ((Rbac3RuleViolation) exception).reasonCode())
                .isEqualTo("IDENTITY_TENANT_MEMBERSHIP_UNAVAILABLE");
    }

    @Test
    void rejectsInvalidExternalIdentifiersBeforeCallingIdp() {
        IdentityDirectoryRpc rpc = mock(IdentityDirectoryRpc.class);
        IdentityTenantMembershipDirectory directory =
                new IdentityTenantMembershipDirectory(rpc);

        assertThatThrownBy(() -> directory.requireActive("0", "subject-17"))
                .isInstanceOf(Rbac3RuleViolation.class)
                .extracting(exception -> ((Rbac3RuleViolation) exception).reasonCode())
                .isEqualTo("IDENTITY_TENANT_MEMBERSHIP_INVALID");
        assertThatThrownBy(() -> directory.requireActive("17", " subject-17"))
                .isInstanceOf(Rbac3RuleViolation.class)
                .extracting(exception -> ((Rbac3RuleViolation) exception).reasonCode())
                .isEqualTo("IDENTITY_TENANT_MEMBERSHIP_INVALID");
    }

    private static GetTenantMembershipResponse response(
            TenantStatus tenantStatus,
            IdentityStatus identityStatus,
            MembershipStatus membershipStatus,
            long version) {
        return GetTenantMembershipResponse.newBuilder()
                .setProfile(TenantMembershipProfile.newBuilder()
                        .setTenantId("17")
                        .setIdentitySub("subject-17")
                        .setTenantStatus(tenantStatus)
                        .setIdentityStatus(identityStatus)
                        .setMembershipStatus(membershipStatus)
                        .setMembershipVersion(version)
                        .build())
                .build();
    }
}
