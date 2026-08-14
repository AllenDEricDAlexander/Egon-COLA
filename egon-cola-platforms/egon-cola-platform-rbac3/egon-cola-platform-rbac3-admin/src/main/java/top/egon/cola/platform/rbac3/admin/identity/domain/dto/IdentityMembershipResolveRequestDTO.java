package top.egon.cola.platform.rbac3.admin.identity.domain.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Service request for resolving one IdP subject's membership in one tenant.
 */
public record IdentityMembershipResolveRequestDTO(
        @NotBlank String identitySub,
        @NotBlank String tenantId) {
}
