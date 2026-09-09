package top.egon.cola.platform.tianquan.jianshen.admin.bootstrap.service;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.domain.dto.MutationWorkDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.service.RuntimeProjectionExecutor;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.user.repository.IdentityTenantMembershipDirectory;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class Rbac3DevelopmentBootstrapTest {

    @Test
    void bootstrapsEveryConfiguredTenantAgainstTheSameIdpSubject() {
        List<String> calls = new ArrayList<>();
        IdentityTenantMembershipDirectory memberships = mock(
                IdentityTenantMembershipDirectory.class);
        var runner = new Rbac3DevelopmentBootstrap(
                (tenantId, identitySub) -> calls.add(
                        tenantId + ":" + identitySub),
                memberships,
                mutation -> calls.add("project:" + mutation.tenantId()),
                "17, 18,17",
                "tianquan-shoubing-subject");

        runner.run(null);

        assertThat(calls).containsExactly(
                "17:tianquan-shoubing-subject",
                "project:17",
                "18:tianquan-shoubing-subject",
                "project:18");
        verify(memberships).requireActive("17", "tianquan-shoubing-subject");
        verify(memberships).requireActive("18", "tianquan-shoubing-subject");
    }

    @Test
    void anUnchangedBootstrapStillRepairsThePreviouslyCommittedRuntime() {
        RuntimeProjectionExecutor projection = mock(RuntimeProjectionExecutor.class);
        var runner = new Rbac3DevelopmentBootstrap((tenantId, identitySub) -> {},
                mock(IdentityTenantMembershipDirectory.class), projection, "17", "tianquan-shoubing-subject");

        runner.run(null);

        verify(projection).project(new MutationWorkDTO(
                "local-bootstrap-17", "17", "TENANT", "17", "COMMITTED"));
    }

    @Test
    void failedDatabaseBootstrapNeverPublishesUncommittedPermissions() {
        RuntimeProjectionExecutor projection = mock(RuntimeProjectionExecutor.class);
        var runner = new Rbac3DevelopmentBootstrap((tenantId, identitySub) -> {
            throw new IllegalStateException("bootstrap failed");
        }, mock(IdentityTenantMembershipDirectory.class), projection, "17", "tianquan-shoubing-subject");

        assertThatThrownBy(() -> runner.run(null)).hasMessage("bootstrap failed");
        verifyNoInteractions(projection);
    }

    @Test
    void failedProjectionPreventsSuccessfulStartupAndCanBeRetried() {
        var runner = new Rbac3DevelopmentBootstrap((tenantId, identitySub) -> {},
                mock(IdentityTenantMembershipDirectory.class), mutation -> {
                    throw new IllegalStateException("Redis unavailable");
                }, "17", "tianquan-shoubing-subject");

        assertThatThrownBy(() -> runner.run(null)).hasMessage("Redis unavailable");
    }
}
