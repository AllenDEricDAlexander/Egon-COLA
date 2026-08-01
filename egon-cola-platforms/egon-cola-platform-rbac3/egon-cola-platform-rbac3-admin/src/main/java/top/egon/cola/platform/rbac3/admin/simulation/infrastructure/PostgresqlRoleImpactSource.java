package top.egon.cola.platform.rbac3.admin.simulation.infrastructure;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.identity.domain.TenantEntity;
import top.egon.cola.platform.rbac3.admin.role.application.RoleFacade;
import top.egon.cola.platform.rbac3.admin.simulation.application.AuthorizationSimulationService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * Loads role impact and the tenant policy version in one read-only transaction.
 */
@Repository
public class PostgresqlRoleImpactSource
        implements AuthorizationSimulationService.RoleImpactSource {

    private final EntityManager entityManager;
    private final RoleFacade roleFacade;

    public PostgresqlRoleImpactSource(
            EntityManager entityManager,
            RoleFacade roleFacade) {
        this.entityManager = entityManager;
        this.roleFacade = roleFacade;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthorizationSimulationService.RoleImpactSnapshot load(
            String tenantId,
            String roleId) {
        TenantEntity tenant = entityManager.find(
                TenantEntity.class, Long.valueOf(tenantId));
        if (tenant == null) {
            throw new IllegalArgumentException("tenant is missing");
        }
        RoleFacade.RoleImpactView impact = roleFacade.impact(tenantId, roleId);
        long policyVersion = tenant.getPolicyVersion();
        return new AuthorizationSimulationService.RoleImpactSnapshot(
                impact, policyVersion,
                checksum(tenantId, policyVersion, impact));
    }

    private static String checksum(
            String tenantId,
            long policyVersion,
            RoleFacade.RoleImpactView impact) {
        String canonical = String.join("\n",
                tenantId,
                Long.toString(policyVersion),
                impact.roleId(),
                sorted(impact.activationRoots()),
                sorted(impact.roleFamily()),
                impact.effectiveFamilyRisk(),
                Long.toString(impact.permissionCount()),
                sorted(impact.conflicts()));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private static String sorted(List<String> values) {
        return values.stream().sorted().toList().toString();
    }
}
