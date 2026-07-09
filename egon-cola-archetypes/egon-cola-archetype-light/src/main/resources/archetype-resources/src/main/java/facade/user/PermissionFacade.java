package ${package}.facade.user;

import ${package}.facade.user.dto.GrantPermissionDTO;
import ${package}.facade.user.dto.PermissionDTO;

public interface PermissionFacade {
    PermissionDTO grantPermission(GrantPermissionDTO request);
}
