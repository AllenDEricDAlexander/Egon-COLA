package ${package}.facade.user.dto;

import java.io.Serializable;

public record PermissionDTO(String roleCode, String permissionCode, String status) implements Serializable {
}
