package ${package}.adapter.converter;

import ${package}.adapter.dto.user.GrantPermissionRequest;
import ${package}.adapter.vo.user.PermissionTreeVO;
import ${package}.application.command.user.GrantPermissionCommand;
import ${package}.application.result.user.PermissionTreeResult;
import org.springframework.stereotype.Component;

@Component("permissionAdapterConverter")
public final class PermissionAdapterConverter {
    public GrantPermissionCommand toCommand(String requestId, String roleCode, GrantPermissionRequest request) {
        return new GrantPermissionCommand(requestId, roleCode, request.permissionCode());
    }

    public PermissionTreeVO toVO(PermissionTreeResult result) {
        return new PermissionTreeVO(result.userId(), result.permissionCodes());
    }
}
