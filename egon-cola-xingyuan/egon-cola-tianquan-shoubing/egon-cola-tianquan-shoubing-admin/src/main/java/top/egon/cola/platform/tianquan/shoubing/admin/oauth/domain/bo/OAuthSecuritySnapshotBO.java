package top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class OAuthSecuritySnapshotBO {

    @PositiveOrZero
    private long realmGeneration;

    @PositiveOrZero
    private long clientGeneration;

    @PositiveOrZero
    private Long subjectGeneration;

    @NotBlank
    @Size(max = 128)
    private String readyEpoch;

    @PositiveOrZero
    private long revision;
}
