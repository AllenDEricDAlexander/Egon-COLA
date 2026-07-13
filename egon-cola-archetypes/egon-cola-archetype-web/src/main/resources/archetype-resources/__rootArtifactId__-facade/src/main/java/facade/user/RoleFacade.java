package ${package}.facade.user;

import ${package}.facade.user.dto.AssignRoleDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface RoleFacade {
    void assignRole(@Valid @NotNull AssignRoleDTO request);
}
