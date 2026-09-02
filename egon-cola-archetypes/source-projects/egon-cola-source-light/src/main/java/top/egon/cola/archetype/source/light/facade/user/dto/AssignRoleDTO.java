package top.egon.cola.archetype.source.light.facade.user.dto;

import java.io.Serializable;

public record AssignRoleDTO(
        Long userId,
        String roleCode,
        String operatorId,
        String requestId) implements Serializable {
}
