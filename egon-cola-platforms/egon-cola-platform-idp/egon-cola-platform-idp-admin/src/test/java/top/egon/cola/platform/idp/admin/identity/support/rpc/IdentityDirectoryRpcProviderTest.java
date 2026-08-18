package top.egon.cola.platform.idp.admin.identity.support.rpc;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.idp.admin.identity.repo.IdentityUserDirectory;
import top.egon.cola.platform.idp.core.identity.IdentityUser;
import top.egon.cola.platform.idp.core.identity.IdentityUserStatus;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.BatchGetIdentityProfilesRequest;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.BatchGetIdentityProfilesResponse;

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
                new IdentityDirectoryRpcProvider(directory);
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
                new IdentityDirectoryRpcProvider(mock(IdentityUserDirectory.class));

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
