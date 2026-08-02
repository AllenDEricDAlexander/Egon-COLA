package top.egon.cola.platform.rbac3.admin.bootstrap.application;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Idempotently completes the local unified-identity tenant after platform bootstrap. */
@Component
@Profile("local")
@ConditionalOnProperty(
        prefix = "egon.rbac3.development-bootstrap",
        name = "enabled",
        havingValue = "true")
public class Rbac3DevelopmentBootstrap implements ApplicationRunner {

    private final BootstrapPort bootstrap;
    private final List<String> tenantCodes;
    private final String username;
    private final String identitySub;

    public Rbac3DevelopmentBootstrap(
            BootstrapPort bootstrap,
            @Value("${egon.rbac3.development-bootstrap.tenant-codes:${egon.rbac3.development-bootstrap.tenant-code:default}}")
            String tenantCodes,
            @Value("${egon.rbac3.development-bootstrap.username:alice}")
            String username,
            @Value("${egon.rbac3.development-bootstrap.identity-sub:}")
            String identitySub) {
        this.bootstrap = Objects.requireNonNull(bootstrap, "bootstrap");
        this.tenantCodes = Arrays.stream(requireText(tenantCodes, "tenantCodes").split(","))
                .map(value -> requireText(value, "tenantCode"))
                .distinct()
                .toList();
        this.username = requireText(username, "username");
        this.identitySub = requireText(identitySub, "identitySub");
    }

    @Override
    public void run(ApplicationArguments args) {
        tenantCodes.forEach(tenantCode ->
                bootstrap.bootstrap(tenantCode, username, identitySub));
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }

    public interface BootstrapPort {

        void bootstrap(String tenantCode, String username, String identitySub);
    }
}
