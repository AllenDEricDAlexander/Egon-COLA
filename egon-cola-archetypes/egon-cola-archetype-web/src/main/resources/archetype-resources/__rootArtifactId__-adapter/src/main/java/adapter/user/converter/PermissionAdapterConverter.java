package ${package}.adapter.user.converter;

import ${package}.adapter.user.dto.GrantPermissionRequest;
import ${package}.adapter.user.vo.PermissionTreeVO;
import ${package}.application.user.command.GrantPermissionCommand;
import ${package}.application.user.result.PermissionTreeResult;
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
