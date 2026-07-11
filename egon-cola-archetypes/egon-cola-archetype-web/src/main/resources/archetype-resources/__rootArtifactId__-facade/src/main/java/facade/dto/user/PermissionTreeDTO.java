package ${package}.facade.dto.user;

import java.util.List;

public record PermissionTreeDTO(String userId, List<String> permissionCodes) {
    public PermissionTreeDTO {
        permissionCodes = List.copyOf(permissionCodes);
    }
}
