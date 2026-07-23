package ${package}.adapter.user.converter;

import ${package}.adapter.user.dto.GrantPermissionRequest;
import ${package}.adapter.user.vo.PermissionTreeVO;
import ${package}.application.user.command.GrantPermissionCommand;
import ${package}.application.user.result.PermissionTreeResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PermissionAdapterConverter {

    @Mapping(target = "requestId", source = "requestId")
    @Mapping(target = "roleCode", source = "roleCode")
    @Mapping(target = "permissionCode", source = "request.permissionCode")
    GrantPermissionCommand toCommand(
            String requestId,
            String roleCode,
            GrantPermissionRequest request);

    PermissionTreeVO toVO(PermissionTreeResult result);
}
