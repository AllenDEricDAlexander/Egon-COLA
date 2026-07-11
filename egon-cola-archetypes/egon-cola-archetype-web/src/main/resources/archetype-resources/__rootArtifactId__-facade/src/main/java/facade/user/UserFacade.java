package ${package}.facade.user;

import ${package}.facade.dto.user.CreateUserDTO;
import ${package}.facade.dto.user.UserDetailDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public interface UserFacade {

    UserDetailDTO createUser(@Valid @NotNull CreateUserDTO request);

    UserDetailDTO getUser(@NotBlank String userId);
}
