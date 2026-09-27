package top.egon.cola.component.tianshu.admin.security.tenancy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Immutable trusted tenant and actor snapshot for one synchronous or callback boundary. */
public record DdcTenantContextDTO(
        @NotNull @Positive Long tenantId,
        @NotBlank String identityTenantId,
        @NotBlank @Size(max = 512) String actor) {

    public DdcTenantContextDTO {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("tenantId must be positive");
        }
        identityTenantId = required(identityTenantId, "identityTenantId");
        actor = required(actor, "actor");
        if (actor.length() > 512) {
            throw new IllegalArgumentException("actor must not exceed 512 characters");
        }
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }
}
