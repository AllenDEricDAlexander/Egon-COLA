package ${package}.facade.user;

import ${package}.facade.dto.user.CreateUserRequest;
import ${package}.facade.dto.user.UserDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public interface UserFacade {
    UserDTO createUser(@Valid @NotNull CreateUserRequest request);

    UserDTO getUser(@NotBlank String userId);
}
