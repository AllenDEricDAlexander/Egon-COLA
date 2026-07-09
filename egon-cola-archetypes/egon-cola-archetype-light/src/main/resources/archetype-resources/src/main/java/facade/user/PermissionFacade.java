package ${package}.facade.user;

import ${package}.facade.user.dto.GrantPermissionDTO;
import ${package}.facade.user.dto.PermissionDTO;
import ${package}.facade.user.dto.PermissionDetailDTO;

import java.util.List;

public interface PermissionFacade {
    PermissionDTO grantPermission(GrantPermissionDTO request);

    List<PermissionDetailDTO> getUserPermissions(String userId);
}
