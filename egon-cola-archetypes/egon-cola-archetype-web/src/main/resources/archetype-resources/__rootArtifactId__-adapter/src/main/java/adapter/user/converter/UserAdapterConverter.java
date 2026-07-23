package ${package}.adapter.user.converter;

import ${package}.adapter.user.dto.CreateUserRequest;
import ${package}.adapter.user.vo.UserDetailVO;
import ${package}.application.user.command.CreateUserCommand;
import ${package}.application.user.result.UserDetailResult;
import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.organization.facade.user.dto.CreateUserDTO;
import top.egon.cola.organization.facade.user.dto.UserDetailDTO;

import java.util.Objects;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserAdapterConverter {

    @Mapping(target = "requestId", source = "requestId")
    @Mapping(target = "name", source = "request.name")
    @Mapping(target = "email", source = "request.email")
    CreateUserCommand toCommand(String requestId, CreateUserRequest request);

    @Mapping(target = "requestId", source = "requestId")
    @Mapping(target = "name", source = "request.name")
    @Mapping(target = "email", source = "request.email")
    CreateUserCommand toCommand(String requestId, CreateUserDTO request);

    UserDetailVO toVO(UserDetailResult result);

    UserDetailDTO toDTO(UserDetailResult result);

    @BeforeMapping
    default void requireRequest(CreateUserRequest request) {
        Objects.requireNonNull(request, "request");
    }

    @BeforeMapping
    default void requireRequest(CreateUserDTO request) {
        Objects.requireNonNull(request, "request");
    }

    @BeforeMapping
    default void requireResult(UserDetailResult result) {
        Objects.requireNonNull(result, "result");
    }
}
