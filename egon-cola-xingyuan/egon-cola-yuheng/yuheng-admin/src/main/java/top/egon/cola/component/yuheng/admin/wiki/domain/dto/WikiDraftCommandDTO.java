package top.egon.cola.component.yuheng.admin.wiki.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
 * 中文说明：{@code WikiDraftCommandDTO} 是 CQE Command 载体，承载原 API-026 保存人工修订的完整替换体；
 * 页面身份仍由路径 {@code kbId/pageId} 决定，正文合同为 title/markdown/tags/links/sources 加乐观 revision。
 * English summary: {@code WikiDraftCommandDTO} is the CQE command carrier of the API-026 full draft replacement;
 * the page identity stays in the kbId/pageId path.
 *
 * 用法 / Usage: markdown 保真，不执行 HTML/脚本；tags 最多 20 个唯一值，links 最多 100 个同 KB 页面 ID，
 * sources 为 1–100 个 {@link WikiSourceDTO} 并逐个 {@code @Valid}；集合在 setter/getter 上各复制一次；
 * 发布/审核状态一律服务端派生，caller 不能借本载体覆写。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class WikiDraftCommandDTO {

    /** 页面标题 1–128 / page title, 1–128 characters. */
    @NotBlank
    @Size(max = 128)
    private String title;

    /** 原始 Markdown，合同上限 131072 字节；注解只约束字符数，字节上限由 Service 复核 / raw markdown bounded by 131072 UTF-8 bytes. */
    @NotBlank
    @Size(max = 131_072)
    private String markdown;

    /** 最多 20 个唯一标签，每个 1–32 字符 / at most 20 unique tags of 1–32 characters. */
    @NotNull
    @Size(max = 20)
    private List<@NotBlank @Size(min = 1, max = 32) String> tags;

    /** 同 KB 页面稳定 ID，最多 100 / at most 100 page ids inside the same KB. */
    @NotNull
    @Size(max = 100)
    private List<@Pattern(regexp = "^[1-9][0-9]{0,19}$") String> links;

    /** 证据列表 1–100，仅来自任务冻结且可读来源 / 1–100 frozen readable sources. */
    @NotNull
    @Size(min = 1, max = 100)
    @Valid
    private List<WikiSourceDTO> sources;

    /** 须等于页面当前 revision / must equal the current page revision. */
    @NotNull
    @Min(0)
    private Long expectedRevision;

    /**
     * 中文说明：返回 tags 的不可变快照副本。
     * English summary: Returns an unmodifiable copy of the tags.
     */
    public List<String> getTags() {
        return tags == null ? null : Collections.unmodifiableList(new ArrayList<>(tags));
    }

    /**
     * 中文说明：写入时复制 tags，避免异步任务期间被外部修改。
     * English summary: Defensively copies the incoming tags.
     */
    public WikiDraftCommandDTO setTags(List<String> tags) {
        this.tags = tags == null ? null : new ArrayList<>(tags);
        return this;
    }

    /**
     * 中文说明：返回 links 的不可变快照副本。
     * English summary: Returns an unmodifiable copy of the link ids.
     */
    public List<String> getLinks() {
        return links == null ? null : Collections.unmodifiableList(new ArrayList<>(links));
    }

    /**
     * 中文说明：写入时复制 links。
     * English summary: Defensively copies the incoming link ids.
     */
    public WikiDraftCommandDTO setLinks(List<String> links) {
        this.links = links == null ? null : new ArrayList<>(links);
        return this;
    }

    /**
     * 中文说明：返回 sources 的不可变快照副本。
     * English summary: Returns an unmodifiable copy of the source list.
     */
    public List<WikiSourceDTO> getSources() {
        return sources == null ? null : Collections.unmodifiableList(new ArrayList<>(sources));
    }

    /**
     * 中文说明：写入时复制 sources；元素仍由 {@code @Valid} 逐个校验。
     * English summary: Defensively copies the incoming sources; elements stay validated by {@code @Valid}.
     */
    public WikiDraftCommandDTO setSources(List<WikiSourceDTO> sources) {
        this.sources = sources == null ? null : new ArrayList<>(sources);
        return this;
    }
}
