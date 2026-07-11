package ${package}.adapter.converter;

import ${package}.adapter.dto.user.CreateUserRequest;
import ${package}.adapter.vo.user.UserDetailVO;
import ${package}.application.command.user.CreateUserCommand;
import ${package}.application.result.user.UserDetailResult;
import ${package}.facade.dto.user.CreateUserDTO;
import ${package}.facade.dto.user.UserDetailDTO;
import org.springframework.stereotype.Component;

@Component("userAdapterConverter")
public final class UserAdapterConverter {

    public CreateUserCommand toCommand(String requestId, CreateUserRequest request) {
        return new CreateUserCommand(requestId, request.name(), request.email());
    }

    public CreateUserCommand toCommand(String requestId, CreateUserDTO request) {
        return new CreateUserCommand(requestId, request.name(), request.email());
    }

    public UserDetailVO toVO(UserDetailResult result) {
        return new UserDetailVO(
            result.id(), result.name(), result.email(), result.status(), result.roleCodes());
    }

    public UserDetailDTO toDTO(UserDetailResult result) {
        return new UserDetailDTO(
            result.id(), result.name(), result.email(), result.status(), result.roleCodes());
    }
}
