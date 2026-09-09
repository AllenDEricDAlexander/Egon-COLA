package top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto;

import jakarta.validation.constraints.PositiveOrZero;

/** Request to rotate one Confidential Client Secret. */
public record RotateClientSecretDTO(
        @PositiveOrZero long expectedVersion
) {
}
