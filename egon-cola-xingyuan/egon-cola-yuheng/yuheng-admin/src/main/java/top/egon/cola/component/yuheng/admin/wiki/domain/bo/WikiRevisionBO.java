package top.egon.cola.component.yuheng.admin.wiki.domain.bo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.codec.digest.DigestUtils;
import top.egon.cola.component.yuheng.admin.wiki.domain.dto.WikiSourceDTO;
import top.egon.cola.component.yuheng.admin.wiki.domain.enums.WikiPublicationPolicyEnum;
import top.egon.cola.component.yuheng.admin.wiki.domain.enums.WikiPublicationStatusEnum;
import top.egon.cola.component.yuheng.admin.wiki.domain.enums.WikiReviewStatusEnum;

/**
 * 中文说明：{@code WikiRevisionBO} 是 {@code gateway_wiki_revision} 的业务载体，也是 §7.3.7 状态机的唯一客体：
 * 内容列（标题、markdown、标签、内链、来源三元组与其摘要）一经创建就不可改写，可迁移的只有发布与评审两组状态列
 * （{@code publicationStatus}/{@code reviewStatus}/{@code publicationVersion} 与 reviewer、published、archived、
 * {@code publicationErrorCode}）。发布策略在创建时以 {@code publicationPolicySnapshot} 冻结，之后的配置变化不改写历史。
 * {@code publicationVersion} 是 revision 行的状态 CAS 版本，{@code revision} 是业务行版本，MP 技术 {@code version}
 * 只在持久边界出现；三个版本互不替代。{@code pageId}/{@code kbId}/{@code generationJobId} 等指针为十进制字符串。
 * English summary: {@code WikiRevisionBO} is the business carrier of {@code gateway_wiki_revision} and the only object of the
 * §7.3.7 state machine: the content columns (title, markdown, tags, links, the source triples and their digest) never change
 * once created while only the publication and review columns are migratable ({@code publicationStatus}, {@code reviewStatus},
 * {@code publicationVersion} plus the reviewer, published, archived and {@code publicationErrorCode} fields). The publication
 * policy freezes into {@code publicationPolicySnapshot} at creation so later configuration never rewrites history.
 * {@code publicationVersion} is the status compare-and-set version of the revision row, {@code revision} the business row
 * version and the MP technical {@code version} stays at the persistence boundary; none substitutes for another.
 * {@code pageId}, {@code kbId} and {@code generationJobId} are decimal-string pointers.
 *
 * 用法 / Usage: 新建正文一律走 {@link #newDraft}，它把状态钉在 DRAFT + NOT_REQUIRED、两个版本钉在 1、
 * 审核与发布痕迹全部留空，并按内容算出 {@code contentHash}；状态迁移由 {@code WikiLifecycleService} 在本载体上
 * 完成后交给 {@code WikiRepository} 做双版本 CAS。列表与图只读投影本载体的安全列。
 * Build every new body through {@link #newDraft}, which pins DRAFT plus NOT_REQUIRED, both versions at one and every
 * review and publication trace empty while deriving {@code contentHash}; the lifecycle service migrates this carrier and the
 * repository performs the dual-version compare-and-set.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class WikiRevisionBO {

    /** 新建 revision 的业务起始 revision / the business revision a freshly created revision row carries. */
    private static final long FIRST_REVISION = 1L;

    /** 新建 revision 的状态 CAS 起始版本 / the status compare-and-set version a freshly created revision row carries. */
    private static final long FIRST_PUBLICATION_VERSION = 1L;

    /** revision 十进制字符串主键，插入后由持久边界回填 / the decimal-string revision key, filled in by the persistence boundary. */
    private String id;

    /** 所属知识库十进制字符串 id，跨库判定的第二道复核 / the owning knowledge base id, the second cross-base guard. */
    private String kbId;

    /** 所属页面十进制字符串 id / the owning page id. */
    private String pageId;

    /** 正文标题，至多 128 字符 / the body title, at most 128 characters. */
    private String title;

    /** 正文 markdown，创建后不可改写 / the markdown body, immutable after creation. */
    private String markdown;

    /** 标签，至多 20 项 / the tags, at most twenty entries. */
    private List<String> tags;

    /** 内链指向的页面 id 列表，至多 100 项 / the linked page ids, at most one hundred entries. */
    private List<String> links;

    /** 来源三元组（资料 revision、切片、摘要），至少一条 / the source triples, at least one entry. */
    private List<WikiSourceDTO> sources;

    /** 内容摘要，由标题、正文、标签、内链与来源共同算出的 SHA-256 / the content digest over title, body, tags, links and sources. */
    private String contentHash;

    /** 作者主体标识 / the authoring actor. */
    private String authorActorId;

    /** 生成来源作业 id，人工起草为 {@code null} / the generating job id, null for a hand-written draft. */
    private String generationJobId;

    /** 发布状态 / the publication status. */
    private WikiPublicationStatusEnum publicationStatus;

    /** 评审状态 / the review status. */
    private WikiReviewStatusEnum reviewStatus;

    /** 创建时冻结的发布策略快照 / the publication policy snapshot frozen at creation. */
    private WikiPublicationPolicyEnum publicationPolicySnapshot;

    /** revision 行的状态 CAS 版本 / the status compare-and-set version of this revision row. */
    private long publicationVersion;

    /** 评审实例外部标识，本期始终为 {@code null} / the external review instance, always null in this release. */
    private String reviewInstanceId;

    /** 评审人主体标识，DIRECT 路径始终为 {@code null} / the reviewer, always null on the DIRECT path. */
    private String reviewerActorId;

    /** 评审时刻，DIRECT 路径始终为 {@code null} / the review instant, always null on the DIRECT path. */
    private Instant reviewedAt;

    /** 评审决定码，DIRECT 路径固定 {@code NOT_REQUIRED} / the review decision code, {@code NOT_REQUIRED} on the DIRECT path. */
    private String reviewDecisionCode;

    /** 发布时刻 / the publication instant. */
    private Instant publishedAt;

    /** 发布人主体标识 / the publishing actor. */
    private String publishedByActorId;

    /** 归档时刻 / the archival instant. */
    private Instant archivedAt;

    /** 归档操作者主体标识 / the archiving actor. */
    private String archivedByActorId;

    /** 发布失败的安全错误码，只允许稳定码表 / the safe publication error code, a stable code only. */
    private String publicationErrorCode;

    /** 是否曾经发布过，一旦为真不再回落 / whether the revision was ever published, never falling back once true. */
    private Boolean everPublished;

    /** 业务行 revision，随业务列覆盖 +1 / the business row revision, bumped with a business-column replacement. */
    private long revision;

    /** 创建时刻，只读投影自 MP 审计列 / the creation instant, projected read-only from the MP audit column. */
    private Instant createdAt;

    /** 更新时刻，只读投影自 MP 审计列 / the update instant, projected read-only from the MP audit column. */
    private Instant updatedAt;

    /**
     * 中文说明：创建一个 DRAFT 修订：内容列由调用方给全，状态钉为 DRAFT + NOT_REQUIRED，策略快照与内容摘要在此
     * 一次性冻结，两个版本都从 1 开始，审核痕迹（评审人、评审时刻、评审实例）与发布、归档痕迹全部留空，
     * {@code everPublished=false}——本方法绝不伪造一次系统评审，也不预判未来接入的评审适配器。
     * English summary: Creates a DRAFT revision: the caller supplies the content columns, the state is pinned to DRAFT plus
     * NOT_REQUIRED, the policy snapshot and the content digest freeze here once, both versions start at one and every review,
     * publication and archival trace stays empty with {@code everPublished = false} — this method never fabricates a system
     * review nor anticipates an adapter that is not wired yet.
     * @param kbId 参数 所属知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param pageId 参数 所属页面十进制字符串 id；parameter decimal-string page id.
     * @param title 参数 标题；parameter the title.
     * @param markdown 参数 正文；parameter the markdown body.
     * @param tags 参数 标签列表；parameter the tags.
     * @param links 参数 内链页面 id 列表；parameter the linked page ids.
     * @param sources 参数 来源三元组列表；parameter the source triples.
     * @param authorActorId 参数 起草主体；parameter the authoring actor.
     * @param generationJobId 参数 生成作业 id，人工起草传 {@code null}；parameter the generating job id, null when hand-written.
     * @param policy 参数 创建时冻结的发布策略；parameter the publication policy to freeze.
     * @return 返回 处于 DRAFT 的新修订载体；returns the new draft carrier.
     */
    public static WikiRevisionBO newDraft(String kbId,
                                          String pageId,
                                          String title,
                                          String markdown,
                                          List<String> tags,
                                          List<String> links,
                                          List<WikiSourceDTO> sources,
                                          String authorActorId,
                                          String generationJobId,
                                          WikiPublicationPolicyEnum policy) {
        List<String> frozenTags = tags == null ? List.of() : List.copyOf(tags);
        List<String> frozenLinks = links == null ? List.of() : List.copyOf(links);
        List<WikiSourceDTO> frozenSources = sources == null ? List.of() : List.copyOf(sources);
        return WikiRevisionBO.builder()
                .kbId(kbId)
                .pageId(pageId)
                .title(title)
                .markdown(markdown)
                .tags(frozenTags)
                .links(frozenLinks)
                .sources(frozenSources)
                .contentHash(digest(title, markdown, frozenTags, frozenLinks, frozenSources))
                .authorActorId(authorActorId)
                .generationJobId(generationJobId)
                .publicationStatus(WikiPublicationStatusEnum.DRAFT)
                .reviewStatus(initialReviewStatus(policy))
                .publicationPolicySnapshot(policy)
                .publicationVersion(FIRST_PUBLICATION_VERSION)
                .everPublished(Boolean.FALSE)
                .revision(FIRST_REVISION)
                .build();
    }

    /**
     * 中文说明：给出策略快照对应的初始评审状态：DIRECT 直接 {@code NOT_REQUIRED}，未接入的 REVIEW_REQUIRED
     * 只能落在 {@code NOT_SUBMITTED}（等待一个尚不存在的适配器），本方法是 {@code DirectWikiPublicationPolicyStrategy}
     * 同一取值的静态形态，两处都不做状态迁移判断。
     * English summary: Names the initial review status of the frozen policy: DIRECT yields {@code NOT_REQUIRED} while the
     * unwired REVIEW_REQUIRED can only start at {@code NOT_SUBMITTED}, waiting for an adapter that does not exist yet. This
     * is the static form of the same value {@code DirectWikiPublicationPolicyStrategy} reports and neither one migrates a state.
     * @param policy 参数 冻结的策略；parameter the frozen policy.
     * @return 返回 评审状态初值；returns the initial review status.
     */
    private static WikiReviewStatusEnum initialReviewStatus(WikiPublicationPolicyEnum policy) {
        return WikiPublicationPolicyEnum.DIRECT.equals(policy)
                ? WikiReviewStatusEnum.NOT_REQUIRED
                : WikiReviewStatusEnum.NOT_SUBMITTED;
    }

    /**
     * 中文说明：判断给定 revision id 是否就是本页当前已发布版本。
     * English summary: Decides whether the given revision id is this page's currently published revision.
     * @return 返回 是否处于 PUBLISHED；whether the row is published.
     */
    @JsonIgnore
    public boolean isPublished() {
        return WikiPublicationStatusEnum.PUBLISHED.equals(publicationStatus);
    }

    /**
     * 中文说明：计算内容摘要：标题、正文、标签、内链与来源三元组按固定顺序拼接后取 SHA-256，
     * 顺序与分隔符都是合同的一部分，改动会让历史 revision 的摘要无法复核。
     * English summary: Computes the content digest by joining title, body, tags, links and the source triples in a fixed order
     * and hashing it with SHA-256; that order and those separators are contractual, changing them would make stored digests
     * unverifiable.
     * @param title 参数 标题；parameter the title.
     * @param markdown 参数 正文；parameter the body.
     * @param tags 参数 标签；parameter the tags.
     * @param links 参数 内链；parameter the links.
     * @param sources 参数 来源；parameter the sources.
     * @return 返回 六十四位小写十六进制摘要；returns the sixty-four lowercase hex digest.
     */
    public static String digest(String title,
                                String markdown,
                                List<String> tags,
                                List<String> links,
                                List<WikiSourceDTO> sources) {
        StringBuilder canonical = new StringBuilder()
                .append("title=").append(title).append('\n')
                .append("body=").append(markdown).append('\n')
                .append("tags=").append(String.join(",", tags == null ? List.of() : tags)).append('\n')
                .append("links=").append(String.join(",", links == null ? List.of() : links)).append('\n');
        for (WikiSourceDTO source : sources == null ? List.<WikiSourceDTO>of() : sources) {
            canonical.append("source=").append(source.getDocumentRevisionId())
                    .append(':').append(source.getChunkId())
                    .append(':').append(source.getSourceHash())
                    .append('\n');
        }
        return DigestUtils.sha256Hex(canonical.toString().getBytes(StandardCharsets.UTF_8));
    }

    public List<String> getTags() {
        return tags == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(tags));
    }

    public WikiRevisionBO setTags(List<String> tags) {
        this.tags = tags == null ? null : new ArrayList<>(tags);
        return this;
    }

    public List<String> getLinks() {
        return links == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(links));
    }

    public WikiRevisionBO setLinks(List<String> links) {
        this.links = links == null ? null : new ArrayList<>(links);
        return this;
    }

    public List<WikiSourceDTO> getSources() {
        return sources == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(sources));
    }

    public WikiRevisionBO setSources(List<WikiSourceDTO> sources) {
        this.sources = sources == null ? null : new ArrayList<>(sources);
        return this;
    }
}
