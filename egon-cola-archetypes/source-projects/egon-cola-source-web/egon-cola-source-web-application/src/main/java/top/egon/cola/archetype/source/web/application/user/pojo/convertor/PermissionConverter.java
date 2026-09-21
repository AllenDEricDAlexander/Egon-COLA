package top.egon.cola.archetype.source.web.application.user.pojo.convertor;

import org.mapstruct.AnnotateWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.web.application.user.pojo.result.PermissionTreeResult;
import top.egon.cola.archetype.source.web.domain.user.entities.Permission;

import java.util.List;

/** Granted-permission projection; the ordered, de-duplicated code set is the original wire shape. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
@AnnotateWith(value = Component.class, elements = @AnnotateWith.Element(strings = "permissionConverterImpl"))
public interface PermissionConverter {

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "permissionCodes", source = "permissions", qualifiedByName = "permissionCodes")
    PermissionTreeResult toResult(Long userId, List<Permission> permissions);

    @Named("permissionCodes")
    default List<String> toPermissionCodes(List<Permission> permissions) {
        if (permissions == null) {
            return List.of();
        }
        return permissions.stream()
                .map(permission -> permission.code().value())
                .distinct()
                .sorted()
                .toList();
    }
}
