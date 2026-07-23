package ${package}.adapter.user.converter;

import ${package}.adapter.user.dto.CreateUserRequest;
import ${package}.adapter.user.vo.UserDetailVO;
import ${package}.application.user.command.CreateUserCommand;
import ${package}.application.user.result.UserDetailResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.organization.facade.user.dto.CreateUserDTO;
import top.egon.cola.organization.facade.user.dto.UserDetailDTO;

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
}
