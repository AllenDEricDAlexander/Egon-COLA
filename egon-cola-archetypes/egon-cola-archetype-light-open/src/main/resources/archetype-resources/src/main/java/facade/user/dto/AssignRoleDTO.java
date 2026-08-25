package ${package}.facade.user.dto;

import java.io.Serializable;

public record AssignRoleDTO(
        Long userId,
        String roleCode,
        String operatorId,
        String requestId) implements Serializable {
}
