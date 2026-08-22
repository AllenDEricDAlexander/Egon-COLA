package top.egon.cola.platform.rbac3.admin.bootstrap;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.bootstrap.controller.cli.Rbac3PlatformAdminBootstrapCli;
import top.egon.cola.platform.rbac3.admin.bootstrap.service.PlatformAdminBootstrapService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Rbac3PlatformAdminBootstrapCliIT {

    @Test
    void bootstrapsIdentityMembershipAndRejectsCredentialArgumentsAndSecondBootstrap() {
        var port = new SingleUseBootstrapPort();
        var cli = new Rbac3PlatformAdminBootstrapCli(port);
        int result = cli.run(new String[]{
                        "bootstrap-platform-admin", "--tenant-id", "17",
                "--identity-sub", "idp-subject"
        });
        assertEquals(0, result);
        assertEquals("idp-subject", port.identitySub);
        assertEquals("17", port.tenantId);

        assertThrows(IllegalStateException.class, () -> cli.run(new String[]{
                        "bootstrap-platform-admin", "--tenant-id", "17",
                "--identity-sub", "other"
        }));
        assertThrows(IllegalArgumentException.class, () -> cli.run(new String[]{
                        "bootstrap-platform-admin", "--tenant-id", "17",
                "--identity-sub", "idp-subject", "--password", "leak"
        }));
    }

    private static final class SingleUseBootstrapPort
            implements PlatformAdminBootstrapService {
        private boolean initialized;
        private String tenantId;
        private String identitySub;

        @Override
        public synchronized void bootstrap(String tenantId, String identitySub) {
            if (initialized) {
                throw new IllegalStateException("platform administrator already exists");
            }
            initialized = true;
            this.tenantId = tenantId;
            this.identitySub = identitySub;
        }
    }
}
