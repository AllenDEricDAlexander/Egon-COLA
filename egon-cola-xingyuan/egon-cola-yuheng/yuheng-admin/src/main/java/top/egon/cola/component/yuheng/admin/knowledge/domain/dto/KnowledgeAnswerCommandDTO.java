package top.egon.cola.component.yuheng.admin.knowledge.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 中文说明：{@code KnowledgeAnswerCommandDTO} 是原 API-022 问答的 CQE Command 载体，
 * 承载 {@code question/sourceMode/topK/searchMode}；问题只作为证据约束下的用户输入，不作为系统指令。
 * English summary: {@code KnowledgeAnswerCommandDTO} is the API-022 answer command carrier; the question stays
 * user input and is never a system instruction.
 *
 * 用法 / Usage: {@code sourceMode}/{@code topK}/{@code searchMode} 为可选，缺省语义（合同给出的
 * topK=8 与 VECTOR 等默认）由服务端决定，因此本载体不写任何 Java 默认值；
 * 集合权限、模型可用性与出域策略仍在 Service 内复核。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class KnowledgeAnswerCommandDTO {

    /** 用户问题 1–8000 字符 / user question, 1–8000 characters. */
    @NotBlank
    @Size(max = 8_000)
    private String question;

    /** DOCUMENTS/WIKI/BOTH，仍仅在当前 KB 内取证 / optional source scope. */
    @Schema(nullable = true)
    @Pattern(regexp = "^(DOCUMENTS|WIKI|BOTH)$")
    private String sourceMode;

    /** 返回证据数 1–20，缺省由服务端决定 / optional evidence count. */
    @Schema(nullable = true)
    @Min(1)
    @Max(20)
    private Integer topK;

    /** VECTOR/KEYWORD/HYBRID，缺省由服务端决定 / optional search mode. */
    @Schema(nullable = true)
    @Pattern(regexp = "^(VECTOR|KEYWORD|HYBRID)$")
    private String searchMode;
}
