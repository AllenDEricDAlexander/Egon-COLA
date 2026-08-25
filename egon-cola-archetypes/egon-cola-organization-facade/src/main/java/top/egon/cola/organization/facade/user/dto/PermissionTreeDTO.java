package top.egon.cola.organization.facade.user.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record PermissionTreeDTO(@NotNull @Positive Long userId, List<String> permissionCodes) {
    public PermissionTreeDTO {
        permissionCodes = List.copyOf(permissionCodes);
    }
}
