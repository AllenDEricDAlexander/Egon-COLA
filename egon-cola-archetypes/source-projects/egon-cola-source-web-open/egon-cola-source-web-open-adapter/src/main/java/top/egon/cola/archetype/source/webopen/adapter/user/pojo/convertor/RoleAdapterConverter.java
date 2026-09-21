package top.egon.cola.archetype.source.webopen.adapter.user.pojo.convertor;

import top.egon.cola.archetype.source.webopen.adapter.user.pojo.dto.AssignRoleRequest;
import top.egon.cola.archetype.source.webopen.application.user.pojo.command.AssignRoleCommand;
import org.mapstruct.BeforeMapping;
import org.mapstruct.AnnotateWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.Objects;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
@AnnotateWith(value = Component.class, elements = @AnnotateWith.Element(strings = "roleAdapterConverterImpl"))
public interface RoleAdapterConverter {

    @Mapping(target = "requestId", source = "requestId")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "roleCode", source = "request.roleCode")
    AssignRoleCommand toCommand(String requestId, Long userId, AssignRoleRequest request);

    @BeforeMapping
    default void requireRequest(AssignRoleRequest request) {
        Objects.requireNonNull(request, "request");
    }
}
