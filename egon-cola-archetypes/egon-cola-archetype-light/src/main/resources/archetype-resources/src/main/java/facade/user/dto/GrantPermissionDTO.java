package ${package}.facade.user.dto;

import java.io.Serializable;

public record GrantPermissionDTO(
        String roleCode,
        String permissionCode,
        String operatorId,
        String requestId) implements Serializable {
}
