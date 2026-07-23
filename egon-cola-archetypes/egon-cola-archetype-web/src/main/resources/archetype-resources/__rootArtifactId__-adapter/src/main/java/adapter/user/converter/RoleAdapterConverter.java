package ${package}.adapter.user.converter;

import ${package}.adapter.user.dto.AssignRoleRequest;
import ${package}.application.user.command.AssignRoleCommand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RoleAdapterConverter {

    @Mapping(target = "requestId", source = "requestId")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "roleCode", source = "request.roleCode")
    AssignRoleCommand toCommand(String requestId, String userId, AssignRoleRequest request);
}
