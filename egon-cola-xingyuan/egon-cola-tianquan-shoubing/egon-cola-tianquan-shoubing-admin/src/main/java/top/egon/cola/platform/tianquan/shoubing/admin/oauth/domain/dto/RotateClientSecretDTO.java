package top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/** Request to rotate one Confidential Client Secret. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class RotateClientSecretDTO {

    @PositiveOrZero
    private long expectedVersion;
}
