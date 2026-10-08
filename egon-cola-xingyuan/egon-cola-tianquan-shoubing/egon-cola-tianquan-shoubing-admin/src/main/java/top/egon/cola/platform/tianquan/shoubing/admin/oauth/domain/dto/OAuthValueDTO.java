package top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/** OAuth 客户端集合字段的单值变更输入。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class OAuthValueDTO {

    @NotBlank
    private String value;
}
