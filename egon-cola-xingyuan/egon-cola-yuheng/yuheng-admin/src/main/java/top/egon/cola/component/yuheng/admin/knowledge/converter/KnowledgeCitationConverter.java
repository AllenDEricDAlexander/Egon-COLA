package top.egon.cola.component.yuheng.admin.knowledge.converter;

import lombok.extern.slf4j.Slf4j;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeRetrievalHitBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgeCitationVO;

/**
 * 中文说明：{@code KnowledgeCitationConverter} 是 API-022 引用条目的唯一出站投影器，把检索 SQL 已经授权的
 * {@link KnowledgeRetrievalHitBO} 单向投影为 {@link KnowledgeCitationVO}：八个字段全部有来源——
 * 冻结资料版本标识 {@code documentRevisionId}、稳定分块标识 {@code chunkId}（都是十进制文本，绝不使用查询列表下标）、
 * 冻结来源 hash {@code sourceHash}（用于 stale 检查）、授权证据节选 {@code excerpt}（按声明上限做代理对安全截断）、
 * 资料标识 {@code documentId} 与显示名 {@code fileName}（取自冻结版本行，因此是上传当时的口径而不是当前改名）、
 * Wiki 页面标识 {@code pageId}（无页面血缘时为 {@code null}）以及检索排序分 {@code score}。
 * 本投影刻意不含 {@code rawBytes}、{@code embedding} 向量、chunk 内部 {@code metadata}、租户、审计、软删与技术版本列：
 * 候选载体本身就没有这些列，因此反向不可重建源行，本类实现 {@code BaseForwardConverter} 而不提供 {@code toSource}，
 * 也不用 {@code UnsupportedOperationException} 占位。
 * English summary: {@code KnowledgeCitationConverter} is the only outbound projector of an API-022 citation, projecting the
 * already-authorized {@link KnowledgeRetrievalHitBO} of retrieval SQL one way onto {@link KnowledgeCitationVO}. All eight
 * fields have a source: the frozen document-revision identifier and the stable chunk identifier (decimal text, never a
 * result-list index), the frozen source hash used for staleness checks, the authorized excerpt truncated in a
 * surrogate-pair-safe way to its declared bound, the document identifier and display name (read from the frozen revision row,
 * hence the name as uploaded rather than any later rename), the wiki page identifier ({@code null} without page lineage) and
 * the retrieval ranking score. The projection deliberately excludes {@code rawBytes}, the {@code embedding} vector, the
 * chunk's internal {@code metadata}, and the tenant, audit, soft-delete and technical version columns: the candidate carrier
 * has no such columns at all, so the source row cannot be rebuilt, which is why this class implements
 * {@code BaseForwardConverter} with no {@code toSource} and no {@code UnsupportedOperationException} placeholder.
 *
 * 用法 / Usage: 由 {@code KnowledgeRetrievalServiceImpl}（Step 13）在装配答案引用时调用（bean 名
 * {@code knowledgeCitationConverter}），单条走 {@code toTarget}、整批走继承来的 {@code toTargetList}，
 * 传入 {@code null} 如实得到 {@code null} 而不伪造引用。投影前调用方必须已完成两件事：
 * 一是按输出顺序定好 {@code score}（向量路取 SQL 的 {@code 1 - cosine_distance}，关键词与混合路取 RRF 名次分），
 * 二是对每一条候选重新验证来源仍然可见——本投影不做任何授权判断，它只负责出站形态。
 * Injected under the bean name {@code knowledgeCitationConverter} in Step 13's authorized retrieval service when assembling
 * answer citations, one row at a time through {@code toTarget} or a whole batch through the inherited {@code toTargetList},
 * with {@code null} yielding {@code null} instead of a fabricated citation. Two things must already hold before the
 * projection: the caller has fixed {@code score} in output order (the SQL {@code 1 - cosine_distance} for the vector leg and
 * the reciprocal-rank score for the keyword and hybrid legs), and every candidate has been re-verified as still visible —
 * this projector makes no authorization decision, only the outbound shape.
 */
@Slf4j
@Component("knowledgeCitationConverter")
public class KnowledgeCitationConverter implements BaseForwardConverter<
        KnowledgeRetrievalHitBO,
        KnowledgeCitationVO> {

    /**
     * 中文说明：表示 MapStruct 生成的同包结构映射器实例，本类只做空值守护与日志边界。
     * English summary: The MapStruct generated same-package structural mapper; this class only adds null guarding and logging.
     *
     * 用法 / Usage: 仅由本转换器的公开方法调用。/ Invoked only by the public methods of this converter.
     */
    private static final KnowledgeCitationProjection MAPPING =
            Mappers.getMapper(KnowledgeCitationProjection.class);

    /**
     * 中文说明：执行 toTarget 操作，把一条已授权候选单向投影为引用条目；
     * 传入 {@code null} 时如实返回 {@code null} 并留下调试日志，不伪造引用。
     * English summary: Executes the toTarget operation, projecting one authorized candidate one way onto a citation;
     * {@code null} input yields {@code null} with a debug log instead of a fabricated citation.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeCitationConverter.toTarget(hit)}。
     * 整批投影用继承的 {@code toTargetList(List)}。/ Use the inherited {@code toTargetList(List)} for a whole batch.
     * @param source 参数 已授权候选载体；parameter the authorized candidate carrier.
     * @return 返回 引用投影；returns the citation projection.
     */
    @Override
    public KnowledgeCitationVO toTarget(KnowledgeRetrievalHitBO source) {
        if (source == null) {
            log.debug("a retrieval candidate is absent; no citation projection rendered");
            return null;
        }
        return MAPPING.toView(source);
    }
}

/**
 * 中文说明：{@code KnowledgeCitationProjection} 是检索候选到引用条目的 MapStruct 结构映射契约，
 * {@code unmappedTargetPolicy=ERROR} 强制 {@link KnowledgeCitationVO} 的每个字段都显式表态：
 * 七个字段按同名投影，只有 {@code excerpt} 需要一次有语义的裁剪，因此用显式表达式调用
 * {@link KnowledgeCitationProjection#excerpt(String)}。候选载体上唯一多出的 {@code kbId} 不进目标类型，
 * 向量、{@code rawBytes}、{@code metadata}、租户/审计/软删/版本与任务租约列更是从不在候选载体上出现，
 * 因命名巧合被带出的可能性由逐字段 {@code @Mapping} 与 ERROR 策略共同封堵。
 * English summary: {@code KnowledgeCitationProjection} is the MapStruct structural contract from a retrieval candidate to the
 * citation, where {@code unmappedTargetPolicy=ERROR} forces every field of {@link KnowledgeCitationVO} to state itself: seven
 * fields map by identical name and only {@code excerpt} needs a meaningful cut, which is why it calls
 * {@link KnowledgeCitationProjection#excerpt(String)} through an explicit expression. The one extra field on the candidate,
 * {@code kbId}, never reaches the target type, and the vector, {@code rawBytes}, {@code metadata}, tenant, audit,
 * soft-delete, version and job-lease columns never appear on the candidate at all — the risk of one leaking through a naming
 * coincidence is closed by the per-field {@code @Mapping} declarations plus the ERROR policy.
 *
 * 用法 / Usage: 由 {@link KnowledgeCitationConverter} 通过 {@code Mappers} 持有，不对外暴露，也不登记为 Spring bean。
 * Held by {@link KnowledgeCitationConverter} through {@code Mappers}; neither published to callers nor registered as a Spring
 * bean.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface KnowledgeCitationProjection {

    /**
     * 中文说明：{@link KnowledgeCitationVO#getExcerpt()} 声明的节选上限，投影时按该上限做代理对安全截断。
     * English summary: The excerpt bound declared by {@link KnowledgeCitationVO#getExcerpt()}, enforced during projection with a
     * surrogate-pair-safe cut.
     */
    int EXCERPT_MAX_LENGTH = 1_000;

    /**
     * 中文说明：执行 toView 操作，按 {@link KnowledgeCitationVO} 的字段顺序投影一条已授权候选；
     * 除节选需要显式裁剪外全部同名映射，标识一律是候选载体上已有的十进制文本，不做任何数值换算。
     * English summary: Executes the toView operation, projecting one authorized candidate onto
     * {@link KnowledgeCitationVO}; everything maps by identical name except the excerpt, which is cut explicitly, and every
     * identifier is already the decimal text the candidate carries, so no numeric conversion happens here.
     *
     * 用法 / Usage: 仅由 {@link KnowledgeCitationConverter#toTarget(KnowledgeRetrievalHitBO)} 调用。
     * @param source 参数 已授权候选载体；parameter the authorized candidate carrier.
     * @return 返回 引用投影；returns the citation projection.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "documentRevisionId", source = "documentRevisionId")
    @Mapping(target = "chunkId", source = "chunkId")
    @Mapping(target = "sourceHash", source = "sourceHash")
    @Mapping(target = "documentId", source = "documentId")
    @Mapping(target = "fileName", source = "fileName")
    // pageId 只在 WIKI 来源下由已发布页面血缘给出，纯资料候选保持 null，绝不从 chunk metadata 猜测。
    @Mapping(target = "pageId", source = "pageId")
    // score 由服务侧在定序后折算（向量路 1-距离，关键词/混合路 RRF 名次分），此处只搬运不改口径。
    @Mapping(target = "score", source = "score")
    @Mapping(target = "excerpt", expression = "java( excerpt( source.getContent() ) )")
    KnowledgeCitationVO toView(KnowledgeRetrievalHitBO source);

    /**
     * 中文说明：执行 excerpt 操作，把候选正文裁剪为不超过 {@code EXCERPT_MAX_LENGTH} 的授权节选，
     * 结尾若落在代理对高半区则回退一格，避免产出孤立码元；缺失如实保持缺失，不追加省略号也不伪造文本。
     * 检索 SQL 已经把正文截到同一上限，因此这里通常是恒等操作，保留它是为了让出站约束不依赖某一条 SQL 的写法。
     * English summary: Executes the excerpt operation, cutting the candidate body down to at most
     * {@code EXCERPT_MAX_LENGTH} of authorized excerpt and stepping back one char when the cut would land on a high surrogate so
     * no orphan code unit is emitted; an absent value stays absent and neither an ellipsis nor fabricated text is added.
     * Retrieval SQL already cuts the body to the same bound, so this is usually the identity, and it stays because the outbound
     * constraint must not depend on how one SQL statement happens to be written.
     *
     * {@code @Named} 保证本方法只由 excerpt 的显式表达式调用，不被 MapStruct 当成通用 {@code String} 换算挑中，
     * 否则 {@code sourceHash} 这类同型字符串列会被悄悄截断。
     * English summary: {@code @Named} keeps this method reachable only from the explicit excerpt expression so MapStruct never
     * picks it up as a generic {@code String} converter, which would silently truncate a same-shaped column such as
     * {@code sourceHash}.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param content 参数 候选正文；parameter the candidate body.
     * @return 返回 授权节选或 {@code null}；returns the authorized excerpt or {@code null}.
     */
    @Named("boundedExcerpt")
    default String excerpt(String content) {
        if (content == null || content.length() <= EXCERPT_MAX_LENGTH) {
            return content;
        }
        String head = content.substring(0, EXCERPT_MAX_LENGTH);
        if (Character.isHighSurrogate(head.charAt(head.length() - 1))) {
            return head.substring(0, head.length() - 1);
        }
        return head;
    }
}
