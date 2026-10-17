package top.egon.cola.component.tianshu.admin.model.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 模型补全结果；finishReason=length 表示输出被截断。
 * / Model completion result; finishReason=length means the output was truncated.
 */
public record DdcAiModelResultDTO(
        @NotBlank String text,
        Integer promptTokens,
        Integer completionTokens,
        String model,
        String finishReason) {

    /** 输出是否因长度上限被截断。 / Whether the output was cut by the length limit. */
    public boolean truncated() {
        return "length".equals(finishReason);
    }
}
