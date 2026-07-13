package ${package}.facade.user.dto;

import java.util.List;

public record PermissionTreeDTO(String userId, List<String> permissionCodes) {
    public PermissionTreeDTO {
        permissionCodes = List.copyOf(permissionCodes);
    }
}
