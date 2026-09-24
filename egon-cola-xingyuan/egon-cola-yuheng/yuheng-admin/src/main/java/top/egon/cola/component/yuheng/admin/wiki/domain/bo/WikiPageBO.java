package top.egon.cola.component.yuheng.admin.wiki.domain.bo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 中文说明：{@code WikiPageBO} 是 {@code gateway_wiki_page} 在业务侧唯一的页面载体，只携带仓储与 worker
 * 真正需要的业务列：主键、所属知识库、slug、草稿与已发布两个 revision 指针、业务 revision 与两个审计时刻。
 * 页面本身不含正文——正文永远在 revision 上，因此本类既无标题也无 markdown；租户、操作者、软删与技术乐观锁
 * {@code version} 只在持久边界出现，绝不进入端口或方法签名。指针为十进制字符串，未发布或未起草即 {@code null}，
 * 绝不伪造 0。
 * English summary: {@code WikiPageBO} is the only business carrier of {@code gateway_wiki_page}, holding exactly the
 * columns the repository and the worker need: the key, the owning knowledge base, the slug, the two revision pointers,
 * the business revision and the two audit instants. A page carries no body — content always lives on a revision — so this
 * class has neither title nor markdown; tenant, operator, soft delete and the technical {@code version} stay at the
 * persistence boundary and never reach a port or a signature. Pointers are decimal strings, {@code null} while nothing is
 * drafted or published and never a fabricated zero.
 *
 * 用法 / Usage: 由 {@code WikiRepository} 与 {@code WikiLifecycleService} 传递，页面可见性判定与指针切换都读这里；
 * {@code contentRevisionId(includeDraft)} 是「本页当前展示哪个 revision」的唯一口径，SQL 侧的 CASE 必须与之一致。
 * Carried by {@code WikiRepository} and {@code WikiLifecycleService}; {@code contentRevisionId(includeDraft)} is the single
 * rule for which revision a page shows and the SQL CASE mirrors it.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class WikiPageBO {

    /** 页面十进制字符串主键，插入后由持久边界回填 / the decimal-string page key, filled in by the persistence boundary after insert. */
    private String id;

    /** 所属知识库十进制字符串 id，跨库读取的第一道复核 / the owning knowledge base id, re-checked on every cross-base read. */
    private String kbId;

    /** 库内唯一的短链名，发布后不可变 / the base-unique short name, immutable once published. */
    private String slug;

    /** 当前草稿 revision 十进制字符串 id，无草稿为 {@code null} / the current draft revision id, null when none. */
    private String draftRevisionId;

    /** 当前已发布 revision 十进制字符串 id，未发布或已下线为 {@code null} / the published revision id, null before publication and after unpublishing. */
    private String publishedRevisionId;

    /** 页面级业务 revision，只在指针切换时 +1，与技术乐观锁互不替代 / the page-level business revision, bumped only when a pointer moves and never the technical lock. */
    private long revision;

    /** 创建时刻，只读投影自 MP 审计列 / the creation instant, projected read-only from the MP audit column. */
    private Instant createdAt;

    /** 更新时刻，只读投影自 MP 审计列 / the update instant, projected read-only from the MP audit column. */
    private Instant updatedAt;

    /**
     * 中文说明：给出本页当前应当展示的 revision——{@code includeDraft} 且确有草稿时取草稿，否则取已发布版本；
     * 两者都没有时返回 {@code null}，由调用方按不可见处理。检索目录、图与列表共用这一口径，SQL 里的
     * {@code CASE} 与本方法必须一致，避免同一个页面在不同入口显示不同正文。
     * English summary: Names the revision this page should show: the draft when {@code includeDraft} and one exists,
     * otherwise the published one, and {@code null} when there is neither, which callers treat as invisible. The catalog,
     * the graph and the list share this single rule so one page never shows two bodies, and the SQL {@code CASE} mirrors it.
     * @param includeDraft 参数 是否允许展示草稿（EDITOR 及以上）；parameter whether a draft may be shown (EDITOR or above).
     * @return 返回 应展示的 revision 十进制字符串 id 或 {@code null}；returns the revision id to show or {@code null}.
     */
    @JsonIgnore
    public String contentRevisionId(boolean includeDraft) {
        return includeDraft && draftRevisionId != null ? draftRevisionId : publishedRevisionId;
    }

    /**
     * 中文说明：把当前可见的 revision 指针按「草稿优先于已发布」列出，供批量装载正文时一次取齐。
     * English summary: Lists the visible revision pointers draft-before-published so a batch body load needs one read.
     * @return 返回 至多两个 revision id；returns at most two revision ids.
     */
    @JsonIgnore
    public List<String> revisionPointerIds() {
        return java.util.Arrays.stream(new String[]{draftRevisionId, publishedRevisionId})
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
    }
}
