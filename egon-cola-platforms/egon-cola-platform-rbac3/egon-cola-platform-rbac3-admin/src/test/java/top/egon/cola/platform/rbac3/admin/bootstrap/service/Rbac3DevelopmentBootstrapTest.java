package top.egon.cola.platform.rbac3.admin.bootstrap.service;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import top.egon.cola.platform.rbac3.admin.bootstrap.service.Rbac3DevelopmentBootstrap;

class Rbac3DevelopmentBootstrapTest {

    @Test
    void bootstrapsEveryConfiguredTenantAgainstTheSameIdpSubject() {
        List<String> calls = new ArrayList<>();
        var runner = new Rbac3DevelopmentBootstrap(
                (tenantCode, username, identitySub) -> calls.add(
                        tenantCode + ":" + username + ":" + identitySub),
                "default, tenant-b",
                "alice",
                "idp-subject");

        runner.run(null);

        assertThat(calls).containsExactly(
                "default:alice:idp-subject",
                "tenant-b:alice:idp-subject");
    }
}
