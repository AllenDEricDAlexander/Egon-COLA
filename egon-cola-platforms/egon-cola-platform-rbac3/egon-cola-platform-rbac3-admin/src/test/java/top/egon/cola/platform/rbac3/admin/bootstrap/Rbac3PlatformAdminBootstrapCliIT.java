package top.egon.cola.platform.rbac3.admin.bootstrap;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.bootstrap.controller.cli.Rbac3PlatformAdminBootstrapCli;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import top.egon.cola.platform.rbac3.admin.bootstrap.service.PlatformAdminBootstrapService;

class Rbac3PlatformAdminBootstrapCliIT {

    @Test
    void readsPasswordFromInputAndRejectsPasswordArgumentsAndSecondBootstrap() {
        var port = new SingleUseBootstrapPort();
        var cli = new Rbac3PlatformAdminBootstrapCli(port);
        int result = cli.run(new String[]{
                        "bootstrap-platform-admin", "--tenant-code", "platform",
                        "--username", "root"
                }, new ByteArrayInputStream("secret-password\n".getBytes(StandardCharsets.UTF_8)));
        assertEquals(0, result);
        assertEquals("root", port.username);
        assertEquals("secret-password", port.password);

        assertThrows(IllegalStateException.class, () -> cli.run(new String[]{
                        "bootstrap-platform-admin", "--tenant-code", "platform",
                        "--username", "other"
                }, new ByteArrayInputStream("other-password\n".getBytes(StandardCharsets.UTF_8))));
        assertThrows(IllegalArgumentException.class, () -> cli.run(new String[]{
                        "bootstrap-platform-admin", "--tenant-code", "platform",
                        "--username", "root", "--password", "leak"
                }, new ByteArrayInputStream(new byte[0])));
        assertThrows(IllegalArgumentException.class, () -> cli.run(new String[]{
                        "bootstrap-platform-admin", "--tenant-code", "platform",
                        "--username", "root"
                }, new ByteArrayInputStream("short\n".getBytes(StandardCharsets.UTF_8))));
    }

    private static final class SingleUseBootstrapPort
            implements PlatformAdminBootstrapService {
        private boolean initialized;
        private String username;
        private String password;

        @Override
        public synchronized void bootstrap(String tenantCode, String username, char[] password) {
            if (initialized) {
                throw new IllegalStateException("platform administrator already exists");
            }
            initialized = true;
            this.username = username;
            this.password = new String(password);
        }
    }
}
