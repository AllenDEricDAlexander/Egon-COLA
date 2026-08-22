package top.egon.cola.platform.idp.admin.oauth.domain.dto;

import jakarta.validation.constraints.PositiveOrZero;

/** Request to rotate one Confidential Client Secret. */
public record RotateClientSecretDTO(
        @PositiveOrZero long expectedVersion
) {
}
