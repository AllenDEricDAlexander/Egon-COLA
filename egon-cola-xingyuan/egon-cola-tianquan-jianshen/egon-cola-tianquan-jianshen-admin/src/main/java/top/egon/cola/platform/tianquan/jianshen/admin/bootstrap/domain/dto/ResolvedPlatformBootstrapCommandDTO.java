package top.egon.cola.platform.tianquan.jianshen.admin.bootstrap.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/** Explicit local initialization using Tianshu-owned public identifiers. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class ResolvedPlatformBootstrapCommandDTO {
    @NotBlank @Pattern(regexp = "[1-9][0-9]{0,18}")
    private String tenantId;
    @NotBlank @Size(max = 200)
    private String identitySub;
    @NotNull @Size(min = 4, max = 4) @Valid
    private List<@NotNull PlatformCatalogBindingDTO> applications;

    /** The codes are selectors; the catalog owner must verify both public IDs. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    public static class PlatformCatalogBindingDTO {
        @NotBlank @Size(max = 128)
        private String applicationCode;
        @NotBlank @Size(max = 128)
        private String businessCode;
        @NotBlank @Pattern(regexp = "[1-9][0-9]{0,18}")
        private String ddcApplicationId;
        @NotBlank @Pattern(regexp = "[1-9][0-9]{0,18}")
        private String ddcBusinessId;
    }
}
