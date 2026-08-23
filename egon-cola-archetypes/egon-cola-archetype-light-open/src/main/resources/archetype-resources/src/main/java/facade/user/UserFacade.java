package ${package}.facade.user;

import ${package}.facade.user.dto.AssignRoleDTO;
import ${package}.facade.user.dto.CreateUserDTO;
import ${package}.facade.user.dto.UserDetailDTO;

public interface UserFacade {
    UserDetailDTO createUser(CreateUserDTO request);

    UserDetailDTO assignRole(AssignRoleDTO request);

    UserDetailDTO getUser(String userId);
}
