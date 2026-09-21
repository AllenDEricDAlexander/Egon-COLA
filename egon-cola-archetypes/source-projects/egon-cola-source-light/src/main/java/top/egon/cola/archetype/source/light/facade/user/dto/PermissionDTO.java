package top.egon.cola.archetype.source.light.facade.user.dto;

import top.egon.cola.component.common.core.pojo.BasePojo;

public record PermissionDTO(String roleCode, String permissionCode, String status) implements BasePojo {
}
