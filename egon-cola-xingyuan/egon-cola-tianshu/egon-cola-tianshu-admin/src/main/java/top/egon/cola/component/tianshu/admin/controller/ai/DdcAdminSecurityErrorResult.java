package top.egon.cola.component.tianshu.admin.controller.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "DdcAdminSecurityErrorResult", description = "Authentication and coarse authorization error envelope")
public record DdcAdminSecurityErrorResult(
        @NotBlank @Schema(description = "Stable security error code", requiredMode = Schema.RequiredMode.REQUIRED)
        String code,
        @NotBlank @Schema(description = "Safe security error message", requiredMode = Schema.RequiredMode.REQUIRED)
        String message) {
}
