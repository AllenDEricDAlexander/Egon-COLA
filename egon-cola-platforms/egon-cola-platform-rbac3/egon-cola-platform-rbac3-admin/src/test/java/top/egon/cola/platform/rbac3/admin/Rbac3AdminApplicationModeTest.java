package top.egon.cola.platform.rbac3.admin;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import top.egon.cola.platform.rbac3.admin.config.security.Rbac3AdminSecurityConfiguration;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class Rbac3AdminApplicationModeTest {

    @Test
    void createsTheSecurityFilterChainOnlyInServletWebMode() {
        var filterChain = Arrays.stream(
                        Rbac3AdminSecurityConfiguration.class.getDeclaredMethods())
                .filter(method -> method.getName().equals(
                        "rbac3SecurityFilterChain"))
                .findFirst()
                .orElseThrow();

        ConditionalOnWebApplication condition = filterChain.getAnnotation(
                ConditionalOnWebApplication.class);
        assertThat(condition).isNotNull();
        assertThat(condition.type()).isEqualTo(
                ConditionalOnWebApplication.Type.SERVLET);
    }

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

    @Test
    void disablesLongRunningInfrastructureForTheOneShotBootstrapCommand() {
        assertThat(Rbac3AdminApplication.bootstrapRuntimeProperties())
                .containsEntry("egon.cola.component.ddc.enabled", false)
                .containsEntry("egon.cola.component.gateway.reporting.enabled", false)
                .containsEntry("egon.cola.component.ddc.registry.http.enabled", false)
                .containsEntry(
                        "egon.cola.component.transactional-outbox.polling.enabled",
                        false)
                .containsEntry(
                        "management.endpoint.health.validate-group-membership",
                        false);
    }
}
