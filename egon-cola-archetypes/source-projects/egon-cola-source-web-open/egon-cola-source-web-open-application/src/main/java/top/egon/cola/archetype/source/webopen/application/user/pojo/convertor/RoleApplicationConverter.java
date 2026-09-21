package top.egon.cola.archetype.source.webopen.application.user.pojo.convertor;

import org.mapstruct.AnnotateWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.webopen.application.user.pojo.command.AssignRoleCommand;

/** Command assembly for the role assignment use case. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
@AnnotateWith(value = Component.class, elements = @AnnotateWith.Element(strings = "roleApplicationConverterImpl"))
public interface RoleApplicationConverter {

    @Mapping(target = "requestId", source = "requestId")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "roleCode", source = "roleCode")
    AssignRoleCommand toCommand(String requestId, Long userId, String roleCode);
}
