package top.egon.cola.archetype.source.web.adapter.user.converter;

import top.egon.cola.archetype.source.web.adapter.user.dto.CreateUserRequest;
import top.egon.cola.archetype.source.web.adapter.user.vo.UserDetailVO;
import top.egon.cola.archetype.source.web.application.user.command.CreateUserCommand;
import top.egon.cola.archetype.source.web.application.user.result.UserDetailResult;
import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.Objects;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserAdapterConverter {

    @Mapping(target = "requestId", source = "requestId")
    @Mapping(target = "name", source = "request.name")
    @Mapping(target = "email", source = "request.email")
    CreateUserCommand toCommand(String requestId, CreateUserRequest request);

    UserDetailVO toVO(UserDetailResult result);

    @BeforeMapping
    default void requireRequest(CreateUserRequest request) {
        Objects.requireNonNull(request, "request");
    }

    @BeforeMapping
    default void requireResult(UserDetailResult result) {
        Objects.requireNonNull(result, "result");
    }
}
