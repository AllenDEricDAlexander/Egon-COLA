package top.egon.cola.archetype.source.web.adapter.user.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import top.egon.cola.component.common.core.pojo.BasePojo;

public record GrantPermissionRequest(@NotBlank String permissionCode) implements BasePojo {
}
