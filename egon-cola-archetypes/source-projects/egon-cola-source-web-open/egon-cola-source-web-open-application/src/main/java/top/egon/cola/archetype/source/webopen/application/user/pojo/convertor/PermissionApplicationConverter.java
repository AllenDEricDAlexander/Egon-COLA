package top.egon.cola.archetype.source.webopen.application.user.pojo.convertor;

import org.mapstruct.AnnotateWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.webopen.application.user.pojo.command.GrantPermissionCommand;

/** Command assembly for the permission grant use case. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
@AnnotateWith(value = Component.class,
        elements = @AnnotateWith.Element(strings = "permissionApplicationConverterImpl"))
public interface PermissionApplicationConverter {

    @Mapping(target = "requestId", source = "requestId")
    @Mapping(target = "roleCode", source = "roleCode")
    @Mapping(target = "permissionCode", source = "permissionCode")
    GrantPermissionCommand toCommand(String requestId, String roleCode, String permissionCode);
}
