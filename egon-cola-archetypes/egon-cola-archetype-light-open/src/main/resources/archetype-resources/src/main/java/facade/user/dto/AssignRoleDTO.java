package ${package}.facade.user.dto;

import java.io.Serializable;

public record AssignRoleDTO(
        String userId,
        String roleCode,
        String operatorId,
        String requestId) implements Serializable {
}
