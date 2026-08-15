package top.egon.cola.platform.rbac3.admin.iam.user.service;

import top.egon.cola.platform.rbac3.admin.iam.user.domain.vo.TenantMembershipVO;
import top.egon.cola.platform.rbac3.admin.iam.user.repository.IdentityMembershipRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Exposes tenant membership without maintaining a second identity-mapping table.
 */
public final class IdentityMembershipFacade {

    private final IdentityMembershipRepository repository;

    public IdentityMembershipFacade(IdentityMembershipRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public Optional<TenantMembershipVO> resolve(String identitySub, String tenantId) {
        return repository.resolve(required(tenantId, "tenantId"),
                required(identitySub, "identitySub"));
    }

    public List<TenantMembershipVO> tenants(String identitySub) {
        return List.copyOf(repository.tenants(required(identitySub, "identitySub")));
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }
}
