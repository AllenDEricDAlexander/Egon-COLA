package top.egon.cola.component.tianshu.admin.model.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Objects;

/**
 * 一次非流式模型补全请求（INTERNAL-103）。
 * / One non-streaming model completion request (INTERNAL-103).
 */
public record DdcAiModelRequestDTO(
        @NotBlank @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,63}$") String modelAlias,
        @NotBlank String systemPrompt,
        @NotBlank String userPrompt,
        @Min(1) @Max(32768) int maxOutputTokens,
        @DecimalMin("0") @DecimalMax("2") double temperature,
        @NotBlank @Pattern(regexp = "^(JUDGE|OPTIMIZE)$") String purpose) {

    public DdcAiModelRequestDTO {
        Objects.requireNonNull(modelAlias, "modelAlias");
        Objects.requireNonNull(systemPrompt, "systemPrompt");
        Objects.requireNonNull(userPrompt, "userPrompt");
        Objects.requireNonNull(purpose, "purpose");
    }
}
