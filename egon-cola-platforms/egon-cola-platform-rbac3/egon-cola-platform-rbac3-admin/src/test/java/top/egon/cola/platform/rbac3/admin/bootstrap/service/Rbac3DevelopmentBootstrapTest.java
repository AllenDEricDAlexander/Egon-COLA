package top.egon.cola.platform.rbac3.admin.bootstrap.service;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Rbac3DevelopmentBootstrapTest {

    @Test
    void bootstrapsEveryConfiguredTenantAgainstTheSameIdpSubject() {
        List<String> calls = new ArrayList<>();
        var runner = new Rbac3DevelopmentBootstrap(
                (tenantCode, identitySub) -> calls.add(
                        tenantCode + ":" + identitySub),
                "default, tenant-b",
                "idp-subject");

        runner.run(null);

        assertThat(calls).containsExactly(
                "default:idp-subject",
                "tenant-b:idp-subject");
    }
}
