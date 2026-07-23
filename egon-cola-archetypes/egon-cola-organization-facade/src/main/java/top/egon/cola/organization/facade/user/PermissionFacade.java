package top.egon.cola.organization.facade.user;

import top.egon.cola.organization.facade.user.dto.GrantPermissionDTO;
import top.egon.cola.organization.facade.user.dto.PermissionTreeDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public interface PermissionFacade {
    void grantPermission(@Valid @NotNull GrantPermissionDTO request);
    PermissionTreeDTO getPermissionTree(@NotBlank String userId);
}
