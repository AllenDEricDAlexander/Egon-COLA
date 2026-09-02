package top.egon.cola.archetype.source.lightopen.facade.user;

import top.egon.cola.archetype.source.lightopen.facade.user.dto.GrantPermissionDTO;
import top.egon.cola.archetype.source.lightopen.facade.user.dto.PermissionDTO;
import top.egon.cola.archetype.source.lightopen.facade.user.dto.PermissionDetailDTO;

import java.util.List;

public interface PermissionFacade {
    PermissionDTO grantPermission(GrantPermissionDTO request);

    List<PermissionDetailDTO> getUserPermissions(Long userId);

}
