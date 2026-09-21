package top.egon.cola.archetype.source.webopen.adapter.user.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import top.egon.cola.component.common.core.pojo.BasePojo;

public record AssignRoleRequest(@NotBlank String roleCode) implements BasePojo {
}
