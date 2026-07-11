package ${package}.facade.user;

import ${package}.facade.dto.user.AssignRoleDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface RoleFacade {
    void assignRole(@Valid @NotNull AssignRoleDTO request);
}
