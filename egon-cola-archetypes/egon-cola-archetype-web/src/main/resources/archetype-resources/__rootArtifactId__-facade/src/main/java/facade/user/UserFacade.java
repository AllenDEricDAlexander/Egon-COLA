package ${package}.facade.user;

import ${package}.facade.dto.user.CreateUserRequest;
import ${package}.facade.dto.user.UserDTO;

public interface UserFacade {
    UserDTO createUser(CreateUserRequest request);

    UserDTO getUser(String userId);
}
