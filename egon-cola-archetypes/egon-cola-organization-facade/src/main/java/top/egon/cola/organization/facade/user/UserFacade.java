package top.egon.cola.organization.facade.user;

import top.egon.cola.organization.facade.user.dto.CreateUserDTO;
import top.egon.cola.organization.facade.user.dto.UserDetailDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public interface UserFacade {

    UserDetailDTO createUser(@Valid @NotNull CreateUserDTO request);

    UserDetailDTO getUser(@NotBlank String userId);
}
