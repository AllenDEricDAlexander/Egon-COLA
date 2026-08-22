package top.egon.cola.platform.idp.admin.identity.support.rpc;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.idp.admin.identity.repo.IdentityUserDirectory;
import top.egon.cola.platform.idp.admin.tenant.service.TenantMembershipService;
import top.egon.cola.platform.idp.core.identity.IdentityUser;
import top.egon.cola.platform.idp.core.identity.IdentityUserStatus;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.BatchGetIdentityProfilesRequest;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.BatchGetIdentityProfilesResponse;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.GetTenantMembershipRequest;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.GetTenantMembershipResponse;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.TenantMembershipProfile;
import top.egon.cola.platform.idp.admin.tenant.domain.pojo.IdentityTenantEntity;
import top.egon.cola.platform.idp.admin.tenant.domain.pojo.IdentityTenantMembershipEntity;
import top.egon.cola.platform.idp.core.port.TenantMembershipPort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdentityDirectoryRpcProviderTest {

    @Test
    void returnsMinimalProfilesAndMissingSubjects() {
        IdentityUserDirectory directory = mock(IdentityUserDirectory.class);
        when(directory.list()).thenReturn(List.of(
                user("subject-a", "alice", "Alice", 4L),
                user("subject-b", "bob", "Bob", 7L)));

        IdentityDirectoryRpcProvider provider =
                new IdentityDirectoryRpcProvider(
                        directory,
                        mock(TenantMembershipService.class)
                );
        BatchGetIdentityProfilesResponse response = provider
                .batchGetIdentityProfiles(BatchGetIdentityProfilesRequest.newBuilder()
                        .addSubjects("subject-a")
                        .addSubjects("subject-missing")
                        .build());

        assertThat(response.getProfilesList()).hasSize(1);
        assertThat(response.getProfiles(0).getSubject()).isEqualTo("subject-a");
        assertThat(response.getProfiles(0).getUsername()).isEqualTo("alice");
        assertThat(response.getProfiles(0).getDisplayName()).isEqualTo("Alice");
        assertThat(response.getProfiles(0).getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getProfiles(0).getVersion()).isEqualTo(4L);
        assertThat(response.getMissingSubjectsList())
                .containsExactly("subject-missing");
    }

    @Test
    void rejectsEmptyDuplicateAndOversizedBatches() {
        IdentityDirectoryRpcProvider provider =
                new IdentityDirectoryRpcProvider(
                        mock(IdentityUserDirectory.class),
                        mock(TenantMembershipService.class)
                );

        assertThatThrownBy(() -> provider.batchGetIdentityProfiles(
                BatchGetIdentityProfilesRequest.getDefaultInstance()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> provider.batchGetIdentityProfiles(
                BatchGetIdentityProfilesRequest.newBuilder()
                        .addSubjects("subject-a")
                        .addSubjects("subject-a")
                        .build()))
                .isInstanceOf(IllegalArgumentException.class);

        BatchGetIdentityProfilesRequest.Builder oversized =
                BatchGetIdentityProfilesRequest.newBuilder();
        for (int index = 0; index < 101; index++) {
            oversized.addSubjects("subject-" + index);
        }
        assertThatThrownBy(() -> provider.batchGetIdentityProfiles(
                oversized.build()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void returnsCompleteTenantMembershipFactsWithoutRbacIdentifiers() {
        IdentityUserDirectory directory = mock(IdentityUserDirectory.class);
        TenantMembershipService memberships = mock(TenantMembershipService.class);
        when(memberships.resolve("user-1", "10001")).thenReturn(
                new TenantMembershipService.TenantMembershipProfile(
                        "user-1",
                        "10001",
                        "Acme",
                        "Mario",
                        IdentityTenantEntity.Status.ACTIVE,
                        IdentityUserStatus.ACTIVE,
                        IdentityTenantMembershipEntity.Status.ACTIVE,
                        TenantMembershipPort.MembershipStatus.ACTIVE,
                        4L,
                        java.time.Instant.parse("2026-08-22T02:00:00Z")
                )
        );

        IdentityDirectoryRpcProvider provider =
                new IdentityDirectoryRpcProvider(directory, memberships);
        GetTenantMembershipResponse response = provider.getTenantMembership(
                GetTenantMembershipRequest.newBuilder()
                        .setTenantId("10001")
                        .setIdentitySub("user-1")
                        .build()
        );

        TenantMembershipProfile profile = response.getProfile();
        assertThat(profile.getTenantId()).isEqualTo("10001");
        assertThat(profile.getIdentitySub()).isEqualTo("user-1");
        assertThat(profile.getTenantStatus().name()).endsWith("ACTIVE");
        assertThat(profile.getIdentityStatus().name()).endsWith("ACTIVE");
        assertThat(profile.getMembershipStatus().name()).endsWith("ACTIVE");
        assertThat(profile.getMembershipVersion()).isEqualTo(4L);
        assertThat(profile.getSerializedSize()).isGreaterThan(0);
    }

    @Test
    void rejectsWildcardAndMapsMissingAuthorityToNotFound() {
        TenantMembershipService memberships = mock(TenantMembershipService.class);
        when(memberships.resolve("user-1", "10001"))
                .thenThrow(new IllegalStateException("membership not found"));
        IdentityDirectoryRpcProvider provider =
                new IdentityDirectoryRpcProvider(
                        mock(IdentityUserDirectory.class),
                        memberships
                );

        assertThatThrownBy(() -> provider.getTenantMembership(
                GetTenantMembershipRequest.newBuilder()
                        .setTenantId("*")
                        .setIdentitySub("user-1")
                        .build()
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> provider.getTenantMembership(
                GetTenantMembershipRequest.newBuilder()
                        .setTenantId("10001")
                        .setIdentitySub("user-1")
                        .build()
        )).isInstanceOf(java.util.NoSuchElementException.class);
    }

    private IdentityUser user(
            String subject,
            String username,
            String displayName,
            long version) {
        return new IdentityUser(
                subject,
                username,
                username,
                displayName,
                IdentityUserStatus.ACTIVE,
                0,
                null,
                null,
                version);
    }
}
