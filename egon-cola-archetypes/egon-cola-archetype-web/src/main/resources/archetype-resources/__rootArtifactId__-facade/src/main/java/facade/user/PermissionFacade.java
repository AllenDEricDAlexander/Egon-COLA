package ${package}.facade.user;

import ${package}.facade.user.dto.GrantPermissionDTO;
import ${package}.facade.user.dto.PermissionTreeDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public interface PermissionFacade {
    void grantPermission(@Valid @NotNull GrantPermissionDTO request);
    PermissionTreeDTO getPermissionTree(@NotBlank String userId);
}
