package top.egon.cola.component.yuheng.admin.wiki.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
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
 * 中文说明：{@code WikiGenerationCommandDTO} 是 CQE Command 载体，承载原 API-024 创建 Wiki 生成任务的请求体；
 * 只接受同 KB、active 且可读的来源版本 ID，不接受任意文件路径。
 * English summary: {@code WikiGenerationCommandDTO} is the CQE command carrier of the API-024 wiki generation
 * request and accepts only same-KB active readable revision ids.
 *
 * 用法 / Usage: {@code pageId} 为空表示新建页面，否则在既有页面上追加草稿版本；
 * 生成模型、出域策略与 idempotency 由服务端与可信上下文决定，不在本载体中出现。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class WikiGenerationCommandDTO {

    /** 同 KB、active 且可读的来源版本 ID，1–20 / 1–20 same-KB active readable source revision ids. */
    @NotNull
    @Size(min = 1, max = 20)
    private List<@Pattern(regexp = "^[1-9][0-9]{0,19}$") String> sourceRevisionIds;

    /** 目标页面稳定 ID，为空表示新建页面 / target page id, null creates a new page. */
    @Schema(nullable = true)
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String pageId;

    /** 新建页面为 0，追加时在既有页面上等于当前 revision / 0 for a new page, current revision otherwise. */
    @NotNull
    @Min(0)
    private Long expectedRevision;

    /**
     * 中文说明：返回 sourceRevisionIds 的不可变快照副本，冻结前不受外部修改影响。
     * English summary: Returns an unmodifiable copy of the frozen source revision ids.
     */
    public List<String> getSourceRevisionIds() {
        return sourceRevisionIds == null
                ? null : Collections.unmodifiableList(new ArrayList<>(sourceRevisionIds));
    }

    /**
     * 中文说明：写入时复制 sourceRevisionIds。
     * English summary: Defensively copies the incoming source revision ids.
     */
    public WikiGenerationCommandDTO setSourceRevisionIds(List<String> sourceRevisionIds) {
        this.sourceRevisionIds = sourceRevisionIds == null ? null : new ArrayList<>(sourceRevisionIds);
        return this;
    }
}
