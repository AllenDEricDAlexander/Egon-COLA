package top.egon.cola.component.yuheng.admin.wiki.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
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
import top.egon.cola.component.yuheng.admin.wiki.domain.dto.WikiSourceDTO;
import top.egon.cola.component.yuheng.admin.wiki.domain.enums.WikiPublicationPolicyEnum;
import top.egon.cola.component.yuheng.admin.wiki.domain.enums.WikiPublicationStatusEnum;
import top.egon.cola.component.yuheng.admin.wiki.domain.enums.WikiReviewStatusEnum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 中文说明：{@code WikiPageVO} 是原 API-023–027 共享的完整页面投影；publicationStatus、reviewStatus、
 * publicationPolicySnapshot、publicationVersion、publicationErrorCode 描述的都是 {@code contentRevisionId}
 * 所指版本，没有正文 revision 时五者全为 null，绝不用 page 状态顶替。
 * English summary: {@code WikiPageVO} is the shared API-023–027 page projection whose five publication fields
 * describe the contentRevisionId version and stay null when no content revision exists.
 *
 * 用法 / Usage: reviewInstanceId、审核人、审核意见以及 publishedAt/By、archivedAt/By 等内部流程资料不外露；
 * stale 只表示来源有效性，不替代生命周期；集合缺失时输出 {@code []} 而非 null，可空指针保持 null。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class WikiPageVO {

    /** 页面稳定 ID / page id. */
    @NotBlank
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String id;

    /** 所属知识库 ID / owning KB id. */
    @NotBlank
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String kbId;

    /** 逻辑地址，不是磁盘路径 / logical slug, never a disk path. */
    @NotBlank
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,63}$")
    private String slug;

    /** 页面标题 1–128 / page title. */
    @NotBlank
    @Size(max = 128)
    private String title;

    /** 草稿版本 ID，可 null / draft revision id, nullable. */
    @Schema(nullable = true)
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String draftRevisionId;

    /** 已发布版本 ID，null 表示未发布 / published revision id, null when unpublished. */
    @Schema(nullable = true)
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String publishedRevisionId;

    /** 所返回 contentRevision 的发布状态；无正文 revision 时 null / publication status of the returned content revision. */
    @Schema(nullable = true)
    private WikiPublicationStatusEnum publicationStatus;

    /** 所返回 contentRevision 的审核状态；无正文 revision 时 null / review status of the returned content revision. */
    @Schema(nullable = true)
    private WikiReviewStatusEnum reviewStatus;

    /** 此 revision 创建时冻结的策略快照 / policy snapshot frozen when this revision was created. */
    @Schema(nullable = true)
    private WikiPublicationPolicyEnum publicationPolicySnapshot;

    /** 状态 CAS 版本，与 page.revision 不同；无正文 revision 时 null / publication CAS version, distinct from page revision. */
    @Schema(nullable = true)
    @Min(1)
    private Long publicationVersion;

    /** 最近安全发布失败码，无错误为 null，不含上游正文 / latest safe publication failure code, null when none. */
    @Schema(nullable = true)
    @Size(max = 128)
    private String publicationErrorCode;

    /** 当前返回正文对应版本 ID，非 page 乐观计数 / content revision id the returned body belongs to. */
    @NotBlank
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String contentRevisionId;

    /** 原始 Markdown，合同上限 131072 字节 / raw markdown bounded by 131072 UTF-8 bytes. */
    @NotBlank
    @Size(max = 131_072)
    private String markdown;

    /** 最多 20 个唯一标签 / at most 20 unique tags. */
    @NotNull
    @Size(max = 20)
    private List<@NotBlank @Size(min = 1, max = 32) String> tags;

    /** 同 KB 页面稳定 ID，最多 100 / at most 100 same-KB page ids. */
    @NotNull
    @Size(max = 100)
    private List<@Pattern(regexp = "^[1-9][0-9]{0,19}$") String> links;

    /** 证据列表 1–100 / 1–100 evidence sources. */
    @NotNull
    @Size(min = 1, max = 100)
    @Valid
    private List<WikiSourceDTO> sources;

    /** 任一来源非当前 active 或被删为 true / true when any source is stale or removed. */
    @NotNull
    private Boolean stale;

    /** 已提交乐观版本，正整数 / committed optimistic revision, positive. */
    @NotNull
    @Min(1)
    private Long revision;

    /**
     * 中文说明：返回 tags 的不可变快照副本，未设置时输出 {@code []}。
     * English summary: Returns an unmodifiable copy of the tags, or an empty list so the wire value is never null.
     */
    public List<String> getTags() {
        return tags == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(tags));
    }

    /**
     * 中文说明：写入时复制 tags。
     * English summary: Defensively copies the incoming tags.
     */
    public WikiPageVO setTags(List<String> tags) {
        this.tags = tags == null ? null : new ArrayList<>(tags);
        return this;
    }

    /**
     * 中文说明：返回 links 的不可变快照副本，未设置时输出 {@code []}。
     * English summary: Returns an unmodifiable copy of the link ids, or an empty list.
     */
    public List<String> getLinks() {
        return links == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(links));
    }

    /**
     * 中文说明：写入时复制 links。
     * English summary: Defensively copies the incoming link ids.
     */
    public WikiPageVO setLinks(List<String> links) {
        this.links = links == null ? null : new ArrayList<>(links);
        return this;
    }

    /**
     * 中文说明：返回 sources 的不可变快照副本，未设置时输出 {@code []}。
     * English summary: Returns an unmodifiable copy of the sources, or an empty list.
     */
    public List<WikiSourceDTO> getSources() {
        return sources == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(sources));
    }

    /**
     * 中文说明：写入时复制 sources；元素仍由 {@code @Valid} 逐个校验。
     * English summary: Defensively copies the incoming sources; elements stay validated by {@code @Valid}.
     */
    public WikiPageVO setSources(List<WikiSourceDTO> sources) {
        this.sources = sources == null ? null : new ArrayList<>(sources);
        return this;
    }
}
