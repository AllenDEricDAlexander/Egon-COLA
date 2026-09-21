package top.egon.cola.archetype.source.webopen.adapter.user.pojo.convertor;

import top.egon.cola.archetype.source.webopen.adapter.user.pojo.dto.CreateUserRequest;
import top.egon.cola.archetype.source.webopen.adapter.user.pojo.vo.UserDetailVO;
import top.egon.cola.archetype.source.webopen.application.user.pojo.command.CreateUserCommand;
import top.egon.cola.archetype.source.webopen.application.user.pojo.result.UserDetailResult;
import org.mapstruct.BeforeMapping;
import org.mapstruct.AnnotateWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.Objects;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
@AnnotateWith(value = Component.class, elements = @AnnotateWith.Element(strings = "userAdapterConverterImpl"))
public interface UserAdapterConverter extends BaseForwardConverter<UserDetailResult, UserDetailVO> {

    @Mapping(target = "requestId", source = "requestId")
    @Mapping(target = "name", source = "request.name")
    @Mapping(target = "email", source = "request.email")
    CreateUserCommand toCommand(String requestId, CreateUserRequest request);

    @Override
    UserDetailVO toTarget(UserDetailResult source);

    /** Callable contract kept from the previous hand-written projection. */
    default UserDetailVO toVO(UserDetailResult result) {
        return toTarget(result);
    }

    @BeforeMapping
    default void requireRequest(CreateUserRequest request) {
        Objects.requireNonNull(request, "request");
    }

    @BeforeMapping
    default void requireResult(UserDetailResult result) {
        Objects.requireNonNull(result, "result");
    }
}
