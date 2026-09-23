package top.egon.cola.component.yuheng.admin.knowledge.domain.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 中文说明：{@code KnowledgeAnswerVO} 是原 API-022 的问答投影；无证据时 outcome 为 NO_EVIDENCE 并给出
 * 固定说明，绝不由模型编造；依赖错误走非 2xx 而非空成功。
 * English summary: {@code KnowledgeAnswerVO} is the API-022 answer projection; NO_EVIDENCE returns the fixed
 * statement while dependency failures stay non-2xx instead of an empty success.
 *
 * 用法 / Usage: citations 无证据时为 {@code []} 而非 null，每条引用都已复核来源版本；
 * model 回显客户端稳定 alias，不泄漏 upstreamModel；集合在 setter/getter 上各复制一次。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class KnowledgeAnswerVO {

    /** ANSWERED 或 NO_EVIDENCE / ANSWERED or NO_EVIDENCE. */
    @NotBlank
    @Pattern(regexp = "^(ANSWERED|NO_EVIDENCE)$")
    private String outcome;

    /** 回答文本，无证据时为固定说明 / answer text, fixed statement without evidence. */
    @NotBlank
    private String answer;

    /** 已验证引用，无证据时为空数组 / verified citations, empty array when none. */
    @NotNull
    @Size(max = 20)
    @Valid
    private List<KnowledgeCitationVO> citations;

    /** 客户端稳定 alias / client-facing stable model alias. */
    @NotBlank
    @Size(max = 64)
    private String model;

    /**
     * 中文说明：返回 citations 的不可变快照副本；未设置时投影为 {@code []} 而非 null。
     * English summary: Returns an unmodifiable copy of the citations, or an empty list so the wire value is never null.
     */
    public List<KnowledgeCitationVO> getCitations() {
        return citations == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(citations));
    }

    /**
     * 中文说明：写入时复制 citations，避免响应对象被下游修改。
     * English summary: Defensively copies the incoming citations.
     */
    public KnowledgeAnswerVO setCitations(List<KnowledgeCitationVO> citations) {
        this.citations = citations == null ? null : new ArrayList<>(citations);
        return this;
    }
}
