package top.egon.fable-web.facade.user;

import top.egon.fable-web.facade.dto.user.CreateUserRequest;
import top.egon.fable-web.facade.dto.user.UserDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public interface UserFacade {
    UserDTO createUser(@Valid @NotNull CreateUserRequest request);

    UserDTO getUser(@NotBlank String userId);
}
