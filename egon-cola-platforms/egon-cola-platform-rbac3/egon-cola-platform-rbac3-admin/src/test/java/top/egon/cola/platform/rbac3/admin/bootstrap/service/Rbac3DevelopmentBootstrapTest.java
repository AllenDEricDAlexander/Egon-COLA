package top.egon.cola.platform.rbac3.admin.bootstrap.service;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.iam.user.repository.IdentityTenantMembershipDirectory;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
                "17, 18,17",
                "idp-subject");

        runner.run(null);

        assertThat(calls).containsExactly(
                "17:idp-subject",
                "18:idp-subject");
        verify(memberships).requireActive("17", "idp-subject");
        verify(memberships).requireActive("18", "idp-subject");
    }
}
