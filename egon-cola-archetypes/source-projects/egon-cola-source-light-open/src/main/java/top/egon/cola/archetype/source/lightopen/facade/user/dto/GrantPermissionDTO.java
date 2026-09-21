package top.egon.cola.archetype.source.lightopen.facade.user.dto;

import jakarta.validation.constraints.NotBlank;
import top.egon.cola.component.common.core.pojo.BasePojo;

public record GrantPermissionDTO(
        @NotBlank String roleCode,
        @NotBlank String permissionCode,
        @NotBlank String operatorId,
        @NotBlank String requestId) implements BasePojo {
}
