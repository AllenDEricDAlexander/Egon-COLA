package top.egon.cola.platform.rbac3.admin.authorization.domain.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Authorization-publication fence check bound to a user identity subject.
 */
public record AuthorizationFenceRequestDTO(@NotBlank String identitySub) {

    public AuthorizationFenceRequestDTO {
        if (identitySub == null || identitySub.isBlank()) {
            throw new IllegalArgumentException("identitySub is required");
        }
        identitySub = identitySub.trim();
    }
}
