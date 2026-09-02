package top.egon.cola.archetype.source.light.facade.user.dto;

import java.io.Serializable;

public record PermissionDTO(String roleCode, String permissionCode, String status) implements Serializable {
}
