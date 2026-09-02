package top.egon.cola.archetype.source.web.adapter.user.converter;

import top.egon.cola.archetype.source.web.adapter.user.dto.AssignRoleRequest;
import top.egon.cola.archetype.source.web.application.user.command.AssignRoleCommand;
import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.Objects;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
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
