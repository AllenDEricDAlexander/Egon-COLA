package top.egon.cola.organization.facade.user;

import top.egon.cola.organization.facade.user.dto.AssignRoleDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface RoleFacade {
    void assignRole(@Valid @NotNull AssignRoleDTO request);
}
