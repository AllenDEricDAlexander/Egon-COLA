package top.egon.cola.platform.rbac3.admin.bootstrap.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import top.egon.cola.platform.rbac3.admin.bootstrap.repository.DevelopmentBootstrapPort;
import top.egon.cola.platform.rbac3.admin.iam.user.repository.IdentityTenantMembershipDirectory;

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
    private final IdentityTenantMembershipDirectory memberships;
    private final List<String> tenantIds;
    private final String identitySub;

    public Rbac3DevelopmentBootstrap(
            DevelopmentBootstrapPort bootstrap,
            IdentityTenantMembershipDirectory memberships,
            @Value("${egon.rbac3.development-bootstrap.tenant-ids:}")
            String tenantIds,
            @Value("${egon.rbac3.development-bootstrap.identity-sub:}")
            String identitySub) {
        this.bootstrap = Objects.requireNonNull(bootstrap, "bootstrap");
        this.memberships = Objects.requireNonNull(memberships, "memberships");
        this.tenantIds = Arrays.stream(required(tenantIds, "tenantIds").split(","))
                .map(Rbac3DevelopmentBootstrap::tenantId)
                .distinct()
                .toList();
        this.identitySub = required(identitySub, "identitySub");
    }

    @Override
    public void run(ApplicationArguments args) {
        tenantIds.forEach(tenantId -> {
            memberships.requireActive(tenantId, identitySub);
            bootstrap.bootstrap(tenantId, identitySub);
        });
    }

    private static String tenantId(String value) {
        String normalized = required(value, "tenantId");
        try {
            long parsed = Long.parseLong(normalized);
            if (parsed <= 0L) {
                throw new NumberFormatException("tenant id must be positive");
            }
            return Long.toString(parsed);
        } catch (NumberFormatException invalid) {
            throw new IllegalArgumentException("tenantId is invalid", invalid);
        }
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }
}
