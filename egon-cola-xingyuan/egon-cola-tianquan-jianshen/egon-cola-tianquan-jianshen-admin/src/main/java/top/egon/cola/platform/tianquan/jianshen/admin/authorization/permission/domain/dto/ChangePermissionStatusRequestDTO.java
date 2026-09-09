package top.egon.cola.platform.tianquan.jianshen.admin.authorization.permission.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/** Global permission review/status command. */
public record ChangePermissionStatusRequestDTO(
        @NotBlank String status,
        @PositiveOrZero long expectedVersion) {
}
