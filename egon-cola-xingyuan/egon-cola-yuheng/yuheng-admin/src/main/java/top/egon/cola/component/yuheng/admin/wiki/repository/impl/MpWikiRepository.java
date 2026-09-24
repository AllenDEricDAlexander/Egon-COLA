package top.egon.cola.component.yuheng.admin.wiki.repository.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.mybatis.business.EgonColaUserIdProvider;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminNotFoundException;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminRevisionConflictException;
import top.egon.cola.component.yuheng.admin.wiki.converter.WikiPagePersistenceConverter;
import top.egon.cola.component.yuheng.admin.wiki.converter.WikiRevisionPersistenceConverter;
import top.egon.cola.component.yuheng.admin.wiki.dao.WikiPageDAO;
import top.egon.cola.component.yuheng.admin.wiki.dao.WikiRevisionDAO;
import top.egon.cola.component.yuheng.admin.wiki.domain.bo.WikiPageBO;
import top.egon.cola.component.yuheng.admin.wiki.domain.bo.WikiRevisionBO;
import top.egon.cola.component.yuheng.admin.wiki.domain.dto.WikiPageQueryDTO;
import top.egon.cola.component.yuheng.admin.wiki.domain.enums.WikiPublicationStatusEnum;
import top.egon.cola.component.yuheng.admin.wiki.domain.enums.WikiReviewStatusEnum;
import top.egon.cola.component.yuheng.admin.wiki.domain.po.WikiPagePO;
import top.egon.cola.component.yuheng.admin.wiki.domain.po.WikiRevisionPO;
import top.egon.cola.component.yuheng.admin.wiki.repository.WikiRepository;
import top.egon.cola.component.yuheng.admin.wiki.repository.mp.WikiPagePersistenceRepository;
import top.egon.cola.component.yuheng.admin.wiki.repository.mp.WikiRevisionPersistenceRepository;

/**
 * 中文说明：{@code MpWikiRepository} 是 {@link WikiRepository} 唯一实现，把 Wiki 两面的十进制字符串 id 与
 * 生命周期枚举换算成受守卫的行模型：读取一律按主键或索引谓词取活跃行并复核跨库归属，写入只走
 * 「载入活跃行 → 复核期望值 → 具名 CAS → 以影响行数裁决」这一条路径，行模型与 MyBatis-Plus 的
 * {@code LambdaQueryWrapper} 永不越过本类。两个父级定位键（{@code kb_id} 与 {@code page_id}）在所有语句里
 * 都是谓词而不是入参，因此跨库引用只能读到空、也永远改不到别人的行。
 * 目录分页与总数刻意共用同一段 SQL 片段（{@code catalogFrom}/{@code catalogScope}），两者由构造而非约定保持一致；
 * 草稿可见性在这里只影响读哪一版，权限判断属于业务层，仓储绝不代替 {@code EDITOR} 复核。
 * English summary: {@code MpWikiRepository} is the only implementation of {@link WikiRepository}, converting the decimal-string
 * ids and lifecycle enums of both Wiki sides into guarded row models: reads fetch active rows by key or indexed predicate and
 * re-check base ownership, and every write follows the single path 「load the active row, re-verify the expectations, run the
 * named compare-and-set, judge by affected rows」, so no row model and no {@code LambdaQueryWrapper} ever leaves this class.
 * Both parent locators, {@code kb_id} and {@code page_id}, are predicates rather than arguments in every statement, so a
 * cross-base reference can only read empty and can never mutate another tenant's or base's row. The catalog page and its
 * total deliberately share one SQL fragment pair ({@code catalogFrom}/{@code catalogScope}), making them consistent by
 * construction rather than by convention; draft visibility here only chooses which revision is read, authorization belongs to
 * the business layer, and the repository never substitutes for the {@code EDITOR} check.
 *
 * 用法 / Usage: 由业务侧按 {@code @Qualifier("wikiRepository")} 注入；写方法必须在调用方的
 * {@code gatewayTransactionManager} 事务内执行，本类不自开写事务，读方法自带只读可重复读事务。
 * Injected by the business side; write methods must run inside the caller's {@code gatewayTransactionManager} transaction
 * since this class opens no write transaction, while the read methods carry their own read-only repeatable-read one.
 */
@Slf4j
@Repository("wikiRepository")
@RequiredArgsConstructor
@Validated
public class MpWikiRepository implements WikiRepository {

    /**
     * 中文说明：目录分页的硬上限，即 API-023 的合同上界。§9.2 的通用规则是「size 默认 20、范围 1–100」，
     * 但 §9.2.23 因为目录项携带完整正文而把本接口收紧到 20，所以这里的夹取上界与 {@code WikiPageQueryDTO}
     * 的 {@code @Max} 必须同值，超出即被夹回而非报错。
     * English summary: The catalog page ceiling, which is API-023's contractual bound. §9.2's generic rule is
     * 「size defaulting to 20 within 1–100」, but §9.2.23 tightens this endpoint to 20 because a row carries its full body, so
     * the clamp must equal {@code WikiPageQueryDTO}'s {@code @Max} and a larger request is clamped rather than rejected.
     */
    private static final int CATALOG_PAGE_SIZE = 20;

    /**
     * 中文说明：新插入行的业务 revision 初值，与 DDL 的 {@code revision bigint NOT NULL DEFAULT 1} 同值；
     * 只在插入路径上作为缺省守护使用，指针切换的 revision 永远来自载体本身。
     * English summary: The initial business revision of a fresh row, equal to the DDL's {@code revision bigint NOT NULL DEFAULT 1};
     * used only as an insert-path default guard, since a pointer swap takes its revision from the carrier itself.
     */
    private static final long FIRST_REVISION = 1L;

    /**
     * 中文说明：还没有权威 revision 可报告时的冲突载荷，与 knowledge 侧同一约定。
     * English summary: The conflict payload when no authoritative revision exists to report, the same convention as on the
     * knowledge side.
     */
    private static final long CREATE_REVISION = 0L;

    /**
     * 中文说明：已发布修订建图扫描的行上限，端口自身按合同裁剪，这里只兜住一个语句级上界。
     * English summary: The row ceiling of the published-revision graph scan; the port bounds what it asks for and this only
     * backstops a statement-level ceiling.
     */
    private static final int SCAN_PAGE_SIZE = 100;

    /**
     * 中文说明：页面行的受守卫通用仓储，提供带租户与软删谓词的主键读取与插入。
     * English summary: The guarded generic repository for page rows, supplying tenant- and soft-delete-scoped key reads and inserts.
     *
     * 用法 / Usage: 仅由本类的读写路径调用。/ Reached only by this class's read and write paths.
     */
    @Qualifier("wikiPagePersistenceRepository")
    private final WikiPagePersistenceRepository pagePersistenceRepository;

    /**
     * 中文说明：修订行的受守卫通用仓储，与页面仓储同样只承担生成语句。
     * English summary: The guarded generic repository for revision rows, carrying generated statements only like the page one.
     *
     * 用法 / Usage: 仅由本类的读写路径调用。/ Reached only by this class's read and write paths.
     */
    @Qualifier("wikiRevisionPersistenceRepository")
    private final WikiRevisionPersistenceRepository revisionPersistenceRepository;

    /**
     * 中文说明：页面 DAO，承载指针 CAS 与目录分页两条生成语句表达不了的原语。
     * English summary: The page DAO, carrying the two primitives generic statements cannot express: the pointer CAS and the
     * catalog page.
     *
     * 用法 / Usage: 仅由本类调用，参数形态见 {@code WikiPageDAO.xml}。/ Called only here; see {@code WikiPageDAO.xml} for shapes.
     */
    @Qualifier("wikiPageDAO")
    private final WikiPageDAO pageDAO;

    /**
     * 中文说明：修订 DAO，承载状态迁移、旧化与建图扫描三条原语。
     * English summary: The revision DAO, carrying the three primitives of status migration, supersession and the graph scan.
     *
     * 用法 / Usage: 仅由本类调用，参数形态见 {@code WikiRevisionDAO.xml}。/ Called only here; see {@code WikiRevisionDAO.xml}.
     */
    @Qualifier("wikiRevisionDAO")
    private final WikiRevisionDAO revisionDAO;

    /**
     * 中文说明：页面行模型与业务载体的双向转换器，也是两个指针的列级白名单。
     * English summary: The page row-model to carrier converter, which is also the column whitelist for the two pointers.
     *
     * 用法 / Usage: {@code pagePersistenceConverter.toBusiness(row)} 与 {@code applyPointers(carrier, row)}。
     * @see WikiPagePersistenceConverter
     */
    @Qualifier("wikiPagePersistenceConverter")
    private final WikiPagePersistenceConverter pagePersistenceConverter;

    /**
     * 中文说明：修订行模型与业务载体的双向转换器，其 {@code applyLifecycle} 就是「内容列冻结、状态列可动」这条
     * 不变量的列级表达。
     * English summary: The revision row-model to carrier converter, whose {@code applyLifecycle} is the column-level statement of
     * the 「content frozen, status movable」 invariant.
     *
     * 用法 / Usage: {@code revisionPersistenceConverter.toBusiness(row)} 与 {@code applyLifecycle(carrier, row)}。
     * @see WikiRevisionPersistenceConverter
     */
    @Qualifier("wikiRevisionPersistenceConverter")
    private final WikiRevisionPersistenceConverter revisionPersistenceConverter;

    /**
     * 中文说明：守卫边界的当前用户提供者；CAS 语句写审计列时绑定它，而不是行上残留的上一位写者。
     * English summary: The guarded boundary's current-user provider; a CAS statement binds it for the audit column instead of the
     * previous writer still recorded on the row.
     *
     * 用法 / Usage: {@code userIdProvider.currentUserId()}，与 observability 侧同一注入形态。
     */
    @Qualifier("egonColaMdcUserIdProvider")
    private final EgonColaUserIdProvider userIdProvider;

    /**
     * 中文说明：按主键读取活跃页面，跨租户与软删由守卫边界挡掉，读不到即空。
     * English summary: Reads an active page by key, a foreign tenant or a soft-deleted row being excluded by the guarded
     * boundary and an unreadable one returning empty.
     *
     * 用法 / Usage: {@code wikiRepository.findPage(pageId)}。
     * @param pageId 参数 页面十进制字符串 id；parameter decimal-string page id.
     * @return 返回 页面载体或空；returns the page carrier or empty.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<WikiPageBO> findPage(String pageId) {
        return Optional.ofNullable(pagePersistenceRepository.getById(idOf(pageId)))
                .map(pagePersistenceConverter::toBusiness);
    }

    /**
     * 中文说明：按库内唯一 slug 读取活跃页面；唯一性由部分唯一索引 {@code uq_wiki_page_active_1} 保证，
     * 因此这里取单行而不必处理多行歧义。
     * English summary: Reads the active page behind a base-unique slug; the partial unique index {@code uq_wiki_page_active_1}
     * establishes uniqueness, so a single row is read without any ambiguity to resolve.
     *
     * 用法 / Usage: {@code wikiRepository.findPageBySlug(kbId, slug)}。
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param slug 参数 库内唯一短链名；parameter the base-unique slug.
     * @return 返回 页面载体或空；returns the page carrier or empty.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<WikiPageBO> findPageBySlug(String kbId, String slug) {
        return Optional.ofNullable(pagePersistenceRepository.getOne(
                        Wrappers.<WikiPagePO>lambdaQuery()
                                .eq(WikiPagePO::getKbId, idOf(kbId))
                                .eq(WikiPagePO::getSlug, slug)))
                .map(pagePersistenceConverter::toBusiness);
    }

    /**
     * 中文说明：读取目录当页，谓词、可见版本选择与次序全部落在 {@code selectCatalog} 一条语句里，
     * 与 {@link #countPages} 共用同一段片段。
     * English summary: Reads one catalog page with its predicate, visible-revision choice and ordering all inside the single
     * {@code selectCatalog} statement, sharing one fragment with {@link #countPages}.
     *
     * 用法 / Usage: {@code wikiRepository.listPages(kbId, query)}；空页返回 {@code []}。
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param query 参数 查询载体；parameter the query carrier.
     * @return 返回 当页页面载体；returns the page carriers of that page.
     */
    @Override
    @Transactional(readOnly = true)
    public List<WikiPageBO> listPages(String kbId, WikiPageQueryDTO query) {
        int size = catalogSize(query.getSize());
        int offset = (int) Math.min((long) (Math.max(query.getPage(), 1) - 1) * size, Integer.MAX_VALUE);
        return pagePersistenceConverter.toBusinessList(pageDAO.selectCatalog(idOf(kbId), query.isIncludeDraft(),
                searchPattern(query.getSearch()), StringUtils.trimToNull(query.getTag()), offset, size));
    }

    /**
     * 中文说明：与当页同谓词地统计总数；两者共用 SQL 片段，因此不可能各自放宽一半。
     * English summary: Counts the pages under the same predicate as the page itself, both sharing the SQL fragments so neither
     * can relax half of it on its own.
     *
     * 用法 / Usage: {@code wikiRepository.countPages(kbId, query)}。
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param query 参数 查询载体；parameter the query carrier.
     * @return 返回 总数；returns the total.
     */
    @Override
    @Transactional(readOnly = true)
    public long countPages(String kbId, WikiPageQueryDTO query) {
        return pageDAO.countCatalog(idOf(kbId), query.isIncludeDraft(),
                searchPattern(query.getSearch()), StringUtils.trimToNull(query.getTag()));
    }

    /**
     * 中文说明：插入一个两个指针皆空的页面行，主键与审计列由受守卫边界回填进返回载体；
     * slug 库内唯一由部分唯一索引兜底，重复键如实换算成 409 而不是静默覆盖既有页面。
     * English summary: Inserts a page row with both pointers empty, the key and audit columns written back into the returned
     * carrier by the guarded boundary; the base-unique slug is enforced by the partial unique index and a duplicate key
     * honestly becomes a 409 rather than silently overwriting the existing page.
     *
     * 用法 / Usage: {@code wikiRepository.insertPage(page)}，在调用方写事务内执行。
     * @param page 参数 待插入页面载体；parameter the page carrier to insert.
     * @return 返回 带权威主键与 revision 的同一载体；returns that carrier with its authoritative key and revision.
     */
    @Override
    public WikiPageBO insertPage(WikiPageBO page) {
        WikiPagePO row = pagePersistenceConverter.newRow(page);
        row.setId(null);
        if (row.getRevision() == null) {
            row.setRevision(FIRST_REVISION);
        }
        try {
            requireSaved(pagePersistenceRepository.save(row), "gateway_wiki_page",
                    "YUHENG_ADMIN_WIKI_PAGE_CREATE_FAILED");
        } catch (DuplicateKeyException raced) {
            log.warn("YUHENG_ADMIN_WIKI_PAGE_CREATE_RACED kbId={} slug={}", page.getKbId(), page.getSlug(), raced);
            throw new GatewayAdminRevisionConflictException(CREATE_REVISION);
        }
        return pagePersistenceConverter.toBusiness(row);
    }

    /**
     * 中文说明：以「页面业务 revision 与两个指针现值」为条件切换指针：先按主键载入活跃行并复核其
     * 知识库归属与 revision 及两个指针确实等于调用方观察到的值，任一条不成立即如实返回 {@code false}；
     * 成立时把目标指针与切换后的 revision 覆盖进行模型，再交给单语句 CAS，影响 0 行同样返回 {@code false}。
     * English summary: Swaps the pointers under 「the page's business revision plus both current pointer values」: the active row
     * is loaded by key and its base ownership, revision and both pointers re-verified against what the caller observed, any
     * unmet condition honestly answering {@code false}; on a match the target pointers and the post-swap revision are applied
     * onto the row model and handed to the single-statement compare-and-set, which again answers {@code false} on zero rows.
     *
     * 用法 / Usage: {@code wikiRepository.movePagePointers(page, expectedPageRevision, expectedPublished, expectedDraft)}。
     * @param page 参数 目标指针已就位的页面载体；parameter carrier holding the target pointers.
     * @param expectedPageRevision 参数 调用方观察到的页面 revision；parameter the caller-observed page revision.
     * @param expectedPublishedRevisionId 参数 期望的已发布指针现值；parameter the expected published pointer.
     * @param expectedDraftRevisionId 参数 期望的草稿指针现值；parameter the expected draft pointer.
     * @return 返回 CAS 是否命中；whether the compare-and-set hit.
     */
    @Override
    public boolean movePagePointers(WikiPageBO page,
                                    long expectedPageRevision,
                                    String expectedPublishedRevisionId,
                                    String expectedDraftRevisionId) {
        WikiPagePO row = pagePersistenceRepository.getById(idOf(page.getId()));
        if (row == null
                || !Objects.equals(row.getKbId(), idOf(page.getKbId()))
                || revisionOf(row.getRevision()) != expectedPageRevision
                || !Objects.equals(row.getPublishedRevisionId(), keyOf(expectedPublishedRevisionId))
                || !Objects.equals(row.getDraftRevisionId(), keyOf(expectedDraftRevisionId))) {
            log.info("YUHENG_ADMIN_WIKI_POINTER_CAS_MISSED pageId={} expected={}", page.getId(), expectedPageRevision);
            return false;
        }
        pagePersistenceConverter.applyPointers(page, row);
        if (pageDAO.movePointers(row, expectedPageRevision, keyOf(expectedPublishedRevisionId),
                keyOf(expectedDraftRevisionId), userIdProvider.currentUserId(), Instant.now()) != 1) {
            log.info("YUHENG_ADMIN_WIKI_POINTER_CAS_LOST pageId={}", page.getId());
            return false;
        }
        log.info("YUHENG_ADMIN_WIKI_POINTERS_MOVED pageId={} revision={}->{}",
                page.getId(), expectedPageRevision, page.getRevision());
        return true;
    }

    /**
     * 中文说明：按主键读取活跃修订并复核所属知识库，跨库读不到按空返回，不用存在性泄漏内容。
     * English summary: Reads an active revision by key and re-checks its base, a cross-base read being empty so existence never
     * leaks content.
     *
     * 用法 / Usage: {@code wikiRepository.findRevision(kbId, revisionId)}。
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param revisionId 参数 修订十进制字符串 id；parameter decimal-string revision id.
     * @return 返回 修订载体或空；returns the revision carrier or empty.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<WikiRevisionBO> findRevision(String kbId, String revisionId) {
        WikiRevisionPO row = revisionPersistenceRepository.getById(idOf(revisionId));
        if (row == null || !Objects.equals(row.getKbId(), idOf(kbId))) {
            return Optional.empty();
        }
        return Optional.of(revisionPersistenceConverter.toBusiness(row));
    }

    /**
     * 中文说明：批量读取本库的活跃修订，一次取齐供目录、图与来源复核使用；跨库的 id 与缺失的 id 都如实少返回，
     * 不补位也不报错。
     * English summary: Batch-reads this base's active revisions so the catalog, the graph and the source re-check settle in one
     * round; foreign-base and absent ids honestly shorten the result instead of being padded or rejected.
     *
     * 用法 / Usage: {@code wikiRepository.listRevisions(kbId, revisionIds)}。
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param revisionIds 参数 待读修订 id 列表；parameter the revision ids to read.
     * @return 返回 读到的修订载体；returns the revision carriers found.
     */
    @Override
    @Transactional(readOnly = true)
    public List<WikiRevisionBO> listRevisions(String kbId, List<String> revisionIds) {
        long baseKey = idOf(kbId);
        List<Long> keys = revisionIds.stream().map(MpWikiRepository::idOf).distinct().toList();
        return revisionPersistenceConverter.toBusinessList(revisionPersistenceRepository.list(
                Wrappers.<WikiRevisionPO>lambdaQuery()
                        .eq(WikiRevisionPO::getKbId, baseKey)
                        .in(WikiRevisionPO::getId, keys)
                        .orderByDesc(WikiRevisionPO::getCreateTime)
                        .orderByDesc(WikiRevisionPO::getId)));
    }

    /**
     * 中文说明：插入一个 DRAFT 修订行；全部内容列（正文、标签、内链、来源、内容 hash、策略快照）在此一次落定，
     * 之后只能由状态 CAS 改写生命周期列；缺失的页面归属按 404 抛出，绝不把未建立的归属伪装成成功。
     * English summary: Inserts a DRAFT revision row; every content column (body, tags, links, sources, content hash, policy
     * snapshot) settles exactly here and only the lifecycle columns move afterwards under a status compare-and-set, while a
     * missing page ownership raises a 404 instead of letting an unestablished relation masquerade as success.
     *
     * 用法 / Usage: {@code wikiRepository.insertRevision(revision)}，与页面插入同在调用方写事务内。
     * @param revision 参数 待插入修订载体；parameter the revision carrier to insert.
     * @return 返回 带权威主键与 revision 的同一载体；returns that carrier with its authoritative key and revision.
     */
    @Override
    public WikiRevisionBO insertRevision(WikiRevisionBO revision) {
        WikiPagePO host = pagePersistenceRepository.getById(idOf(revision.getPageId()));
        if (host == null || !Objects.equals(host.getKbId(), idOf(revision.getKbId()))) {
            throw new GatewayAdminNotFoundException("gateway_wiki_page#" + revision.getPageId());
        }
        WikiRevisionPO row = revisionPersistenceConverter.newRow(revision);
        row.setId(null);
        if (row.getRevision() == null) {
            row.setRevision(FIRST_REVISION);
        }
        requireSaved(revisionPersistenceRepository.save(row), "gateway_wiki_revision",
                "YUHENG_ADMIN_WIKI_REVISION_CREATE_FAILED");
        return revisionPersistenceConverter.toBusiness(row);
    }

    /**
     * 中文说明：以「状态版本 + 起止状态」为条件迁移修订状态并整列覆盖生命周期列；载入活跃行后先复核知识库与
     * 页面归属、状态版本与两个起始状态，任一条不成立即返回 {@code false}，成立时把目标生命周期列覆盖进行模型
     * 交给单语句 CAS，内容列与两个父级定位键由受守卫谓词固定在 WHERE 里、从不进入 SET。
     * English summary: Migrates the revision status and rewrites the lifecycle columns under 「the status version plus the
     * from-statuses」: the active row is loaded and its base and page ownership, status version and both from-statuses
     * re-verified first, any unmet condition answering {@code false}, and on a match the target lifecycle columns are applied
     * onto the row model before the single-statement compare-and-set, the content columns and both parent locators being
     * pinned in the WHERE and never entering the SET.
     *
     * 用法 / Usage: {@code wikiRepository.transitionRevision(revision, expectedPublicationVersion, fromPublication, fromReview)}。
     * @param revision 参数 目标状态已就位的修订载体；parameter carrier holding the target status.
     * @param expectedPublicationVersion 参数 调用方观察到的状态版本；parameter the caller-observed status version.
     * @param fromPublicationStatus 参数 期望的发布状态起点；parameter the expected publication from-state.
     * @param fromReviewStatus 参数 期望的评审状态起点；parameter the expected review from-state.
     * @return 返回 CAS 是否命中；whether the compare-and-set hit.
     */
    @Override
    public boolean transitionRevision(WikiRevisionBO revision,
                                      long expectedPublicationVersion,
                                      WikiPublicationStatusEnum fromPublicationStatus,
                                      WikiReviewStatusEnum fromReviewStatus) {
        WikiRevisionPO row = revisionPersistenceRepository.getById(idOf(revision.getId()));
        if (row == null
                || !Objects.equals(row.getKbId(), idOf(revision.getKbId()))
                || !Objects.equals(row.getPageId(), idOf(revision.getPageId()))
                || positive(row.getPublicationVersion()) != expectedPublicationVersion
                || !fromPublicationStatus.wireValue().equals(row.getPublicationStatus())
                || !fromReviewStatus.wireValue().equals(row.getReviewStatus())) {
            log.info("YUHENG_ADMIN_WIKI_TRANSITION_CAS_MISSED revisionId={} expected={}",
                    revision.getId(), expectedPublicationVersion);
            return false;
        }
        revisionPersistenceConverter.applyLifecycle(revision, row);
        if (revisionDAO.transitionRevision(row, expectedPublicationVersion, fromPublicationStatus.wireValue(),
                fromReviewStatus.wireValue(), userIdProvider.currentUserId(), Instant.now()) != 1) {
            log.info("YUHENG_ADMIN_WIKI_TRANSITION_CAS_LOST revisionId={}", revision.getId());
            return false;
        }
        log.info("YUHENG_ADMIN_WIKI_REVISION_TRANSITIONED revisionId={} status={} publicationVersion={}->{}",
                revision.getId(), revision.getPublicationStatus().wireValue(), expectedPublicationVersion,
                revision.getPublicationVersion());
        return true;
    }

    /**
     * 中文说明：把页面当前的已发布修订置 SUPERSEDED 并保留其发布与评审痕迹；同一 CAS 复核它仍是
     * PUBLISHED、状态版本未被并发改动，且不是接替它的那一行（重复发布不该先把自己旧化）。
     * English summary: Marks the page's current published revision SUPERSEDED while keeping its publication and review trace,
     * the same compare-and-set re-checking it is still PUBLISHED at the expected status version and is not the very row
     * replacing it, since a repeat publication must not supersede itself first.
     *
     * 用法 / Usage: {@code wikiRepository.supersedePrevious(kbId, publishedRevisionId, newRevisionId, expected)}；
     * 返回 {@code false} 由业务侧换算成 409。
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param publishedRevisionId 参数 待旧化的已发布修订 id；parameter the published revision to supersede.
     * @param newRevisionId 参数 接替它的新修订 id；parameter the replacing revision id.
     * @param expectedPublicationVersion 参数 旧行的状态版本现值；parameter the old row's status version.
     * @return 返回 CAS 是否命中；whether the compare-and-set hit.
     */
    @Override
    public boolean supersedePrevious(String kbId,
                                     String publishedRevisionId,
                                     String newRevisionId,
                                     long expectedPublicationVersion) {
        WikiRevisionPO row = revisionPersistenceRepository.getById(idOf(publishedRevisionId));
        if (row == null
                || !Objects.equals(row.getKbId(), idOf(kbId))
                || positive(row.getPublicationVersion()) != expectedPublicationVersion
                || !WikiPublicationStatusEnum.PUBLISHED.wireValue().equals(row.getPublicationStatus())) {
            log.info("YUHENG_ADMIN_WIKI_SUPERSEDE_CAS_MISSED revisionId={} expected={}",
                    publishedRevisionId, expectedPublicationVersion);
            return false;
        }
        if (revisionDAO.supersedePrevious(row, idOf(newRevisionId), expectedPublicationVersion,
                userIdProvider.currentUserId(), Instant.now()) != 1) {
            log.info("YUHENG_ADMIN_WIKI_SUPERSEDE_CAS_LOST revisionId={}", publishedRevisionId);
            return false;
        }
        log.info("YUHENG_ADMIN_WIKI_REVISION_SUPERSEDED revisionId={} replacedBy={} publicationVersion={}->{}",
                publishedRevisionId, newRevisionId, expectedPublicationVersion, expectedPublicationVersion + 1);
        return true;
    }

    /**
     * 中文说明：读取本库当前处于 PUBLISHED 的活跃修订，供图与内链闭合；被替换与归档的发布由状态谓词自动出局。
     * English summary: Reads this base's active revisions currently PUBLISHED for the graph and the inline-link closure,
     * superseded and archived publications dropping out by the status predicate alone.
     *
     * 用法 / Usage: {@code wikiRepository.listPublishedRevisions(kbId, limit)}。
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param limit 参数 返回上限，正整数；parameter the positive ceiling.
     * @return 返回 已发布修订载体；returns the published revision carriers.
     */
    @Override
    @Transactional(readOnly = true)
    public List<WikiRevisionBO> listPublishedRevisions(String kbId, int limit) {
        return revisionPersistenceConverter.toBusinessList(
                revisionDAO.selectPublishedRevisions(idOf(kbId), scanLimit(limit)));
    }

    /**
     * 中文说明：把端口上的十进制字符串 id 换算为 {@code long} 主键；端口注解已把形态限定在 1–20 位十进制，
     * 但 20 位仍可能超出 {@code long} 上限，所以此处如实按 {@code 422 YUHENG_ADMIN_VALIDATION_FAILED} 拒绝，
     * 绝不静默降级为 0，也不按前缀猜测。
     * English summary: Converts a port-side decimal-string id into the {@code long} primary key; the port's annotations already
     * confine the shape to 1–20 decimal digits, yet 20 of them can still exceed the {@code long} ceiling, so this refuses
     * honestly with {@code 422 YUHENG_ADMIN_VALIDATION_FAILED} instead of degrading to zero or guessing from a prefix.
     *
     * 用法 / Usage: {@code idOf(pageId)}，本类唯一的 id 换算入口。
     * @param value 参数 十进制字符串 id；parameter decimal-string identifier.
     * @return 返回 主键；returns the primary key.
     */
    private static long idOf(String value) {
        if (StringUtils.isBlank(value)) {
            throw validation("an identifier must be a decimal string of 1 to 20 digits");
        }
        try {
            long parsed = Long.parseLong(value.trim());
            if (parsed <= 0) {
                throw validation("an identifier must be a positive decimal string");
            }
            return parsed;
        } catch (NumberFormatException malformed) {
            throw validation("an identifier must be a decimal string of 1 to 20 digits");
        }
    }

    /**
     * 中文说明：把可空的外键列值换算成十进制字符串可比较的 {@code Long}，空值保持为空，
     * 使「未发布」与「发布到某一行」在指针谓词里可区分。
     * English summary: Normalizes a nullable foreign-key column value into a comparable {@code Long}, an absent value staying
     * absent so 「unpublished」 and 「published at some row」 remain distinguishable in the pointer predicate.
     *
     * 用法 / Usage: {@code keyOf(page.getDraftRevisionId())}。
     * @param value 参数 主键或十进制字符串 id；parameter a key or a decimal-string id.
     * @return 返回 可比较的键或空；returns the comparable key or null.
     */
    private static Long keyOf(String value) {
        return StringUtils.isBlank(value) ? null : idOf(value);
    }

    /**
     * 中文说明：读取可空 bigint 业务 revision，缺失按初值 1 处理（DDL 的 {@code DEFAULT 1}）。
     * English summary: Reads a nullable bigint business revision, an absent value being the DDL default of one.
     * @param revision 参数 列值；parameter column value.
     * @return 返回 业务 revision；returns the business revision.
     */
    private static long revisionOf(Long revision) {
        return revision == null ? FIRST_REVISION : revision;
    }

    /**
     * 中文说明：读取非空 bigint 状态版本，缺失按初值 1 处理，与 {@link #revisionOf(Long)} 同理但不表达「未盖章」。
     * English summary: Reads a not-null bigint status version, an absent value being the initial one, same reasoning as
     * {@link #revisionOf(Long)} but never expressing an unstamped row.
     * @param version 参数 列值；parameter column value.
     * @return 返回 状态版本；returns the status version.
     */
    private static long positive(Long version) {
        return version == null ? FIRST_REVISION : version;
    }

    /**
     * 中文说明：把目录页大小收敛到 1–100，端口注解已给出同一值域，此处只是防御性再夹一次；
     * 目录与建图扫描的上界刻意分开，因为放宽其中一个不可能顺带放宽另一个。
     * English summary: Clamps a catalog page size into 1–100, the port's annotations already enforcing the same range so this is a
     * defensive second clamp; the catalog and the graph-scan bounds stay separate on purpose, since relaxing one can never
     * relax the other along with it.
     * @param size 参数 请求页大小；parameter requested page size.
     * @return 返回 生效页大小；returns the effective page size.
     */
    private static int catalogSize(int size) {
        return Math.min(Math.max(size, 1), CATALOG_PAGE_SIZE);
    }

    /**
     * 中文说明：把建图扫描的行数收敛到正上界。
     * English summary: Clamps a graph-scan row count into a positive ceiling.
     * @param limit 参数 请求上限；parameter requested ceiling.
     * @return 返回 生效上限；returns the effective ceiling.
     */
    private static int scanLimit(int limit) {
        return Math.min(Math.max(limit, 1), SCAN_PAGE_SIZE);
    }

    /**
     * 中文说明：把检索词转成 {@code ILIKE} 的字面子串模式：先 trim，空串如实变为无检索条件，再转义
     * {@code \}、{@code %}、{@code _} 三个元字符并加两端通配。这与 knowledge 关键词腿同一约定，
     * 因为 PostgreSQL 的 simple 分词不能被宣称为中文全文检索，这里就不假装是。
     * English summary: Turns a search term into a literal {@code ILIKE} substring pattern: trimmed first, a blank term honestly
     * becoming no search condition at all, then the three metacharacters {@code \}, {@code %} and {@code _} escaped and wildcards
     * wrapped around it. This is the same convention as the knowledge keyword leg, because PostgreSQL's simple
     * tokenization is never presented as Chinese full-text search and no such claim is made here either.
     * @param search 参数 原始检索词，可为空；parameter the raw search term, nullable.
     * @return 返回 已转义的模式或空；returns the escaped pattern or null.
     */
    private static String searchPattern(String search) {
        String trimmed = StringUtils.trimToNull(search);
        if (trimmed == null) {
            return null;
        }
        String escaped = trimmed.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }

    /**
     * 中文说明：受守卫写入的行数裁决：{@code false}（影响 0 行）按内部错误抛出并回滚调用方事务，
     * 绝不把「什么都没写」当作创建成功。
     * English summary: The row-count verdict of a guarded write: {@code false} (zero affected rows) raises an internal error that
     * rolls back the caller's transaction instead of treating "nothing written" as a successful create.
     * @param written 参数 受守卫边界的写入结论；parameter the boundary's write verdict.
     * @param table 参数 逻辑表名（仅日志与错误细节）；parameter logical table (log and detail only).
     * @param code 参数 稳定机读码；parameter stable machine code.
     */
    private static void requireSaved(boolean written, String table, String code) {
        if (!written) {
            log.error("{} table={}", code, table);
            throw new IllegalStateException(code);
        }
    }

    /**
     * 中文说明：构造 {@code 422 YUHENG_ADMIN_VALIDATION_FAILED}，只用于 id 形态越界（例如 20 位超出 {@code long}）。
     * English summary: Builds the {@code 422 YUHENG_ADMIN_VALIDATION_FAILED} raised for an out-of-range identifier shape alone,
     * such as a 20-digit value beyond {@code long}.
     * @param detail 参数 稳定可读说明（不含正文）；parameter a stable readable detail (no content).
     * @return 返回 待抛出的异常；returns the exception to throw.
     */
    private static CommonException validation(String detail) {
        return new CommonException(422, "YUHENG_ADMIN_VALIDATION_FAILED", detail);
    }
}
