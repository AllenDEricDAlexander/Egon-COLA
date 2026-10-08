package top.egon.cola.platform.tianquan.shoubing.admin.token.domain.command;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class OAuthSubjectRevokeCommand {

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @NotBlank
    @Size(max = 64)
    @Pattern(regexp = "[A-Za-z0-9._~-]{1,64}")
    private String subject;

    @NotBlank
    @Size(max = 128)
    @Pattern(regexp = "[A-Z0-9_]{1,128}")
    private String reasonCode;
}
