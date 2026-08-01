package top.egon.cola.platform.rbac3.admin;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Rbac3AdminApplicationModeTest {

    @Test
    void selectsTheNonWebBootstrapModeOnlyForTheExplicitCommand() {
        assertThat(Rbac3AdminApplication.isBootstrapCommand(new String[]{
                "bootstrap-platform-admin", "--tenant-code", "platform"
        })).isTrue();
        assertThat(Rbac3AdminApplication.isBootstrapCommand(new String[]{
                "--spring.profiles.active=local"
        })).isFalse();
        assertThat(Rbac3AdminApplication.isBootstrapCommand(new String[0])).isFalse();
    }
}
