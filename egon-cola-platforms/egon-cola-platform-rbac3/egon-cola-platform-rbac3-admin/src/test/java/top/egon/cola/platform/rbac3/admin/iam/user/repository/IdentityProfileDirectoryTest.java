package top.egon.cola.platform.rbac3.admin.iam.user.repository;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.idp.rpc.contract.IdentityDirectoryRpc;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.BatchGetIdentityProfilesRequest;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.BatchGetIdentityProfilesResponse;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.IdentityProfile;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdentityProfileDirectoryTest {

    @Test
    void returnsEnrichedProfilesAndMissingMarker() {
        IdentityDirectoryRpc rpc = mock(IdentityDirectoryRpc.class);
        when(rpc.batchGetIdentityProfiles(any(BatchGetIdentityProfilesRequest.class)))
                .thenReturn(BatchGetIdentityProfilesResponse.newBuilder()
                        .addProfiles(IdentityProfile.newBuilder()
                                .setSubject("subject-a")
                                .setUsername("alice")
                                .setDisplayName("Alice")
                                .setStatus("ACTIVE")
                                .setVersion(3L))
                        .addMissingSubjects("subject-missing")
                        .build());

        IdentityProfileDirectory directory = new IdentityProfileDirectory(rpc);
        IdentityProfileDirectory.BatchResult result = directory.batchGet(
                List.of("subject-a", "subject-missing"));

        assertThat(result.available()).isTrue();
        assertThat(result.profiles()).containsKey("subject-a");
        assertThat(result.profiles().get("subject-a").displayName())
                .isEqualTo("Alice");
        assertThat(result.missingSubjects())
                .containsExactly("subject-missing");
    }

    @Test
    void keepsAuthorizationUsableWhenIdentityDirectoryIsUnavailable() {
        IdentityDirectoryRpc rpc = mock(IdentityDirectoryRpc.class);
        when(rpc.batchGetIdentityProfiles(any(BatchGetIdentityProfilesRequest.class)))
                .thenThrow(new IllegalStateException("idp unavailable"));

        IdentityProfileDirectory directory = new IdentityProfileDirectory(rpc);
        IdentityProfileDirectory.BatchResult result = directory.batchGet(
                Set.of("subject-a"));

        assertThat(result.available()).isFalse();
        assertThat(result.profiles()).isEmpty();
        assertThat(result.missingSubjects()).isEmpty();
    }

    @Test
    void treatsMissingRpcBindingAsUnavailable() {
        IdentityProfileDirectory directory = new IdentityProfileDirectory();

        assertThat(directory.batchGet(List.of("subject-a")).available())
                .isFalse();
    }
}
