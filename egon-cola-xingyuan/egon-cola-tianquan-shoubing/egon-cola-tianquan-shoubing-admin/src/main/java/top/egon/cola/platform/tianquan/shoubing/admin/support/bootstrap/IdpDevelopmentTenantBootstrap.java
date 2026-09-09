package top.egon.cola.platform.tianquan.shoubing.admin.support.bootstrap;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.pojo.IdentityTenantEntity;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.pojo.IdentityTenantMembershipEntity;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.service.TenantMembershipService;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.service.TenantService;
import top.egon.cola.platform.tianquan.shoubing.core.port.IdentityUserStore;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 通过 Tianquan-Shoubing 应用服务初始化显式启用的本地测试租户与成员关系。
 * Initializes opt-in local tenant fixtures through the Tianquan-Shoubing application services.
 */
@Component
@Profile("local")
@ConditionalOnProperty(prefix = "egon.tianquan-shoubing.development-bootstrap", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class IdpDevelopmentTenantBootstrap {

    private static final String ACTOR = "local-development-bootstrap";
    private static final List<String> TENANT_CODES = List.of("default", "tenant-b");

    private final TenantService tenants;
    private final TenantMembershipService memberships;
    private final IdentityUserStore users;

    /** Creates missing local tenants before resolving configured codes to exact grant IDs. */
    public Set<String> resolveTenantIds(Set<String> configuredTenants) {
        List<TenantService.TenantView> existing = tenants.list();
        for (String code : TENANT_CODES) {
            if (existing.stream().anyMatch(tenant -> code.equals(tenant.tenantCode()))) {
                continue;
            }
            TenantService.TenantView tenant = tenants.create(new TenantService.CreateTenantCommand(
                    code, "default".equals(code) ? "默认测试租户" : "测试租户 B", "{}", ACTOR));
            tenants.update(tenant.tenantId(), new TenantService.UpdateTenantCommand(
                    tenant.version(), null, null, IdentityTenantEntity.Status.ACTIVE, ACTOR));
        }
        List<TenantService.TenantView> available = tenants.list();
        Set<String> resolved = new LinkedHashSet<>();
        for (String configured : configuredTenants) {
            TenantService.TenantView tenant = available.stream()
                    .filter(item -> configured.equals(item.tenantId()) || configured.equals(item.tenantCode()))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException(
                            "local service grant tenant does not exist: " + configured));
            if (!tenant.tenantId().matches("^[1-9][0-9]{0,18}$")) {
                throw new IllegalStateException("Tianquan-Shoubing tenant ID must be a positive numeric identifier");
            }
            resolved.add(tenant.tenantId());
        }
        return java.util.Collections.unmodifiableSet(resolved);
    }

    /** Adds only missing memberships after the explicitly requested local administrator exists. */
    public void initializeAdministratorMemberships() {
        users.findByNormalizedUsername("alice").ifPresent(user -> {
            Set<String> existing = memberships.listByIdentity(user.id()).stream()
                    .map(TenantMembershipService.TenantMembershipProfile::tenantId)
                    .collect(java.util.stream.Collectors.toSet());
            for (TenantService.TenantView tenant : tenants.list()) {
                if (TENANT_CODES.contains(tenant.tenantCode()) && !existing.contains(tenant.tenantId())) {
                    memberships.upsert(new TenantMembershipService.UpsertMembershipCommand(
                            tenant.tenantId(), user.id(), IdentityTenantMembershipEntity.Status.ACTIVE,
                            null, ACTOR));
                }
            }
        });
    }
}
