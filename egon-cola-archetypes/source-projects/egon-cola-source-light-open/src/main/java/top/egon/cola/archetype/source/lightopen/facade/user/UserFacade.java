package top.egon.cola.archetype.source.lightopen.facade.user;

import top.egon.cola.archetype.source.lightopen.facade.user.dto.AssignRoleDTO;
import top.egon.cola.archetype.source.lightopen.facade.user.dto.CreateUserDTO;
import top.egon.cola.archetype.source.lightopen.facade.user.dto.UserDetailDTO;

public interface UserFacade {
    UserDetailDTO createUser(CreateUserDTO request);

    UserDetailDTO assignRole(AssignRoleDTO request);

    UserDetailDTO getUser(Long userId);

}
