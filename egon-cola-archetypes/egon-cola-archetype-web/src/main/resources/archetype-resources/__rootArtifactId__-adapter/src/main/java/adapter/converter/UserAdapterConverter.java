package ${package}.adapter.converter;

import ${package}.adapter.dto.user.CreateUserRequest;
import ${package}.adapter.vo.user.UserDetailVO;
import ${package}.application.user.command.CreateUserCommand;
import ${package}.application.user.result.UserDetailResult;
import ${package}.facade.user.dto.CreateUserDTO;
import ${package}.facade.user.dto.UserDetailDTO;
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
