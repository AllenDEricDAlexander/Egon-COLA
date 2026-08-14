package top.egon.cola.platform.rbac3.admin.bootstrap.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import top.egon.cola.platform.rbac3.admin.bootstrap.repository.DevelopmentBootstrapPort;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Idempotently prepares local RBAC membership for an IdP subject.
 */
@Component
@Profile("local")
@ConditionalOnProperty(
        prefix = "egon.rbac3.development-bootstrap",
        name = "enabled",
        havingValue = "true")
public class Rbac3DevelopmentBootstrap implements ApplicationRunner {

    private final DevelopmentBootstrapPort bootstrap;
    private final List<String> tenantCodes;
    private final String identitySub;

    public Rbac3DevelopmentBootstrap(
            DevelopmentBootstrapPort bootstrap,
            @Value("${egon.rbac3.development-bootstrap.tenant-codes:${egon.rbac3.development-bootstrap.tenant-code:default}}")
            String tenantCodes,
            @Value("${egon.rbac3.development-bootstrap.identity-sub:}")
            String identitySub) {
        this.bootstrap = Objects.requireNonNull(bootstrap, "bootstrap");
        this.tenantCodes = Arrays.stream(required(tenantCodes, "tenantCodes").split(","))
                .map(value -> required(value, "tenantCode"))
                .distinct()
                .toList();
        this.identitySub = required(identitySub, "identitySub");
    }

    @Override
    public void run(ApplicationArguments args) {
        tenantCodes.forEach(tenantCode -> bootstrap.bootstrap(tenantCode, identitySub));
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }
}
